package org.geysermc.mappings.resources;

import org.geysermc.mappings.FileSystemAccess;
import org.geysermc.mappings.util.MappingsUtil;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/// This record downloads and manages access to [Mojang/bedrock-samples](https://github.com/Mojang/bedrock-samples)
/// and [Mojang/bedrock-protocol-docs](https://github.com/Mojang/bedrock-protocol-docs) (JSON data only).
///
/// All repositories are accessible as {@link DownloadedSamples} in this record, which provides {@link DownloadedSamples#open(DownloadedSamples.FileSystemUser)} to interact
/// with the data through a {@link FileSystemAccess} instance. If this API does not cover your use case, you can interact with a {@link FileSystem}
/// directly through {@link DownloadedSamples#openRaw(DownloadedSamples.RawFileSystemUser)}.
public record BedrockSamples(DownloadedSamples samples, DownloadedSamples protocol) {
    private static final Path SAMPLES = Path.of("bedrock-samples.zip");
    private static final Path PROTOCOL = Path.of("bedrock-protocol.zip");

    public static CompletableFuture<BedrockSamples> load(Path root) {
        // Load bedrock version data from FMJ fields
        String bedrockVersion = MappingsUtil.getBedrockVersion();
        // Human version is bedrock version but truncated (1.26.60.29-preview turns into 1.26.60). This is what we need for protocol docs
        String bedrockProtocolVersion = MappingsUtil.getHumanBedrockVersion();

        // Start downloading the data/samples
        CompletableFuture<DownloadedSamples> samples = DownloadedSamples.githubRelease("bedrock-samples", root.resolve(SAMPLES), "Mojang/bedrock-samples", "v" + bedrockVersion,
                asset -> asset.endsWith("-min.zip")); // Get the minified assets
        CompletableFuture<DownloadedSamples> protocol = DownloadedSamples.githubRelease("bedrock-protocol", root.resolve(PROTOCOL), "Mojang/bedrock-protocol-docs", "v" + bedrockProtocolVersion,
                asset -> asset.equals("metadata.zip")); // Get the metadata
        return CompletableFuture.allOf(samples, protocol).thenApply(_ -> new BedrockSamples(samples.join(), protocol.join()));
    }
}
