package org.geysermc.mappings;

import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.geysermc.mappings.resources.BedrockSamples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class ProviderGroupBuilder {
    private final List<ProviderFactory<? extends DataProvider>> factories = new ArrayList<>();

    public ProviderGroupBuilder with(UnaryOperator<ProviderGroupBuilder> operator) {
        return operator.apply(this);
    }

    public ProviderGroupBuilder withFactory(Function<PackOutput, ? extends DataProvider> factory) {
        return withRegistryAndSamplesFactory((output, _, _) -> factory.apply(output));
    }

    public ProviderGroupBuilder withRegistryFactory(BiFunction<PackOutput, CompletableFuture<RegistryAccess>, ? extends DataProvider> factory) {
        return withRegistryAndSamplesFactory((output, registries, _) -> factory.apply(output, registries));
    }

    public ProviderGroupBuilder withSamplesFactory(BiFunction<PackOutput, CompletableFuture<BedrockSamples>, ? extends DataProvider> factory) {
        return withRegistryAndSamplesFactory((output, _, samples) -> factory.apply(output, samples));
    }

    public ProviderGroupBuilder withRegistryAndSamplesFactory(ProviderFactory<? extends DataProvider> factory) {
        this.factories.add(factory);
        return this;
    }

    public List<ProviderFactory<? extends DataProvider>> build() {
        return List.copyOf(factories);
    }

    public interface ProviderFactory<T extends DataProvider> {

        T create(PackOutput output, CompletableFuture<RegistryAccess> registries, CompletableFuture<BedrockSamples> samples);
    }
}
