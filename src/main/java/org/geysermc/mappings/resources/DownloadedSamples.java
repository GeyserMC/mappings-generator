package org.geysermc.mappings.resources;

import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Util;
import org.geysermc.mappings.FileSystemAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipFile;

public final class DownloadedSamples {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private static final String GITHUB_ZIPBALL_BASE_URL = "http://api.github.com/repos/%s/zipball/%s";
    private static final String GITHUB_RELEASE_INFO_BASE_URL = "https://api.github.com/repos/%s/releases/tags/%s";

    private final Path destination;
    private final Function<FileSystem, Path> rootGetter;

    private DownloadedSamples(Path destination, Function<FileSystem, Path> rootGetter) {
        this.destination = destination;
        this.rootGetter = rootGetter;
    }

    /// Prefer using {@link DownloadedSamples#open(FileSystemUser)} when possible.
    public <T> CompletableFuture<T> openRaw(RawFileSystemUser<T> user) {
        return CompletableFuture.supplyAsync(() -> {
            try (FileSystem fileSystem = FileSystems.newFileSystem(destination)) {
                return user.use(fileSystem);
            } catch (IOException exception) {
                throw new UncheckedIOException("Failed to open " + destination.getFileName(), exception);
            }
        }, Util.backgroundExecutor().forName("DownloadedSamples#open"));
    }

    public <T> CompletableFuture<T> open(FileSystemUser<T> user) {
        return openRaw(system -> {
            Path root = rootGetter.apply(system);
            return user.use(() -> root).join();
        });
    }

    @FunctionalInterface
    public interface RawFileSystemUser<T> {

        T use(FileSystem system) throws IOException;
    }

    @FunctionalInterface
    public interface FileSystemUser<T> {

        CompletableFuture<T> use(FileSystemAccess access) throws IOException;
    }

    private static CompletableFuture<DownloadedSamples> load(CompletableFuture<ReleaseInfo> releaseInfoFuture) {
        return releaseInfoFuture.thenCompose(releaseInfo -> {
            // If there's already data downloaded, check if it's accurate
            if (Files.exists(releaseInfo.destination)) {
                try {
                    if (releaseInfo.isExistingAccurate()) {
                        LOGGER.info("{} is still reported accurate, not re-downloading", releaseInfo);
                        return CompletableFuture.completedFuture(new DownloadedSamples(releaseInfo.destination, releaseInfo.rootGetter));
                    }
                    Files.delete(releaseInfo.destination);
                } catch (Exception exception) {
                    LOGGER.error("Failed to check accuracy of {}! Attempting to re-download...", releaseInfo, exception);
                }
            }

            return tryToDownloadSamples(releaseInfo).thenApply(_ -> {
                try {
                    releaseInfo.verifyDownload();
                } catch (Exception exception) {
                    throw new RuntimeException("Failed to verify download of " + releaseInfo, exception);
                }
                return new DownloadedSamples(releaseInfo.destination, releaseInfo.rootGetter);
            });
        });
    }

    public static CompletableFuture<DownloadedSamples> githubZipball(String type, Path destination, String repository, String sha) {
        return load(CompletableFuture.completedFuture(new ReleaseInfo(type, URI.create(GITHUB_ZIPBALL_BASE_URL.formatted(repository, sha)),
                sha, destination, ReleaseInfo.GH_ZIPBALL_ROOT) {
            @Override
            protected boolean isExistingAccurate() throws Exception {
                return sha.equals(zipComment(destination));
            }
        }));
    }

    public static CompletableFuture<DownloadedSamples> githubRelease(String type, Path destination, String repository, String tag,
                                                                     Predicate<String> assetPicker) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(GITHUB_RELEASE_INFO_BASE_URL.formatted(repository, tag))).build();
        LOGGER.info("Gathering release info for {} version {}", type, tag);
        return load(CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Received status code " + response.statusCode() + " when trying to gather " + type + " release info for version " + tag);
            }
            try {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray assets = json.getAsJsonArray("assets");

                for (JsonElement asset : assets) {
                    String name = asset.getAsJsonObject().get("name").getAsString();
                    if (assetPicker.test(name)) {
                        String digest = asset.getAsJsonObject().get("digest").getAsString().replaceFirst("^sha256:", "");
                        return new ReleaseInfo(type, URI.create(asset.getAsJsonObject().get("browser_download_url").getAsString()),
                                tag, destination, ReleaseInfo.GH_RELEASE_ROOT) {
                            @Override
                            protected boolean isExistingAccurate() throws Exception {
                                return sha256File(destination).equals(digest);
                            }

                            @Override
                            protected void verifyDownload() throws Exception {
                                String sha256 = sha256File(destination);
                                if (!sha256.equals(digest)) {
                                    throw new IllegalStateException("Hashes of downloaded " + type + " and GitHub don't line up!\n" +
                                            "GitHub: " + digest + "\n" +
                                            "Local: " + sha256);
                                }
                            }
                        };
                    }
                }
                throw new IllegalStateException("Unable to find asset that matches assetPicker for " + type);
            } catch (Exception exception) {
                throw new RuntimeException("Caught exception when trying to gather " + type + " release info for version " + tag, exception);
            }
        }));
    }

    private static CompletableFuture<Path> tryToDownloadSamples(ReleaseInfo release) {
        HttpRequest request = HttpRequest.newBuilder(release.uri).build();
        LOGGER.info("Downloading {} version {}", release, release.reportedVersion);
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofFile(release.destination)).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Received status code " + response.statusCode() + " when trying to download " + release + " for version " + release.reportedVersion);
            }
            return response.body();
        });
    }

    private static @Nullable String zipComment(Path file) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            return zip.getComment();
        }
    }

    private static String sha256File(Path file) throws IOException {
        return Hashing.sha256().hashBytes(Files.readAllBytes(file)).toString();
    }

    public abstract static class ReleaseInfo {
        private static final Function<FileSystem, Path> GH_ZIPBALL_ROOT = system -> {
            // Have to do this because GitHub doesn't put the files in the root of the ZIP, rather it creates a folder with the name of the repo and the commit SHA
            // and puts the files in there
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(system.getPath("/"))) {
                return stream.iterator().next();
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        };
        private static final Function<FileSystem, Path> GH_RELEASE_ROOT = system -> system.getPath("/");

        private final String type;
        private final URI uri;
        private final String reportedVersion;
        private final Path destination;
        private final Function<FileSystem, Path> rootGetter;

        protected ReleaseInfo(String type, URI uri, String reportedVersion, Path destination, Function<FileSystem, Path> rootGetter) {
            this.type = type;
            this.uri = uri;
            this.reportedVersion = reportedVersion;
            this.destination = destination;
            this.rootGetter = rootGetter;
        }

        protected abstract boolean isExistingAccurate() throws Exception;

        protected void verifyDownload() throws Exception {}

        @Override
        public String toString() {
            return type;
        }
    }
}
