package org.geysermc.mappings;

import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.geysermc.mappings.resources.BedrockSamples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public record ProviderGroup(List<ProviderFactory<? extends DataProvider>> factories,
                            boolean requiresRegistries, boolean requiresSamples) {

    public ProviderGroup merge(ProviderGroup other) {
        ArrayList<ProviderFactory<? extends DataProvider>> combined = new ArrayList<>();
        combined.addAll(factories);
        combined.addAll(other.factories);
        return new ProviderGroup(Collections.unmodifiableList(combined),
                requiresRegistries || other.requiresRegistries,
                requiresSamples || other.requiresSamples);
    }

    public static ProviderGroup of(UnaryOperator<Builder> builder) {
        return builder.apply(new Builder()).build();
    }

    public static final class Builder {
        private final List<ProviderFactory<? extends DataProvider>> factories = new ArrayList<>();
        private boolean requiresRegistries = false;
        private boolean requiresSamples = false;

        public Builder with(UnaryOperator<Builder> operator) {
            return operator.apply(this);
        }

        public Builder withFactory(Function<PackOutput, ? extends DataProvider> factory) {
            return withFactoryInternal((output, _, _) -> factory.apply(output));
        }

        public Builder withRegistryFactory(BiFunction<PackOutput, CompletableFuture<RegistryAccess>, ? extends DataProvider> factory) {
            requiresRegistries = true;
            return withFactoryInternal((output, registries, _) -> factory.apply(output, registries));
        }

        public Builder withSamplesFactory(BiFunction<PackOutput, CompletableFuture<BedrockSamples>, ? extends DataProvider> factory) {
            requiresSamples = true;
            return withFactoryInternal((output, _, samples) -> factory.apply(output, samples));
        }

        public Builder withRegistryAndSamplesFactory(ProviderFactory<? extends DataProvider> factory) {
            requiresRegistries = true;
            requiresSamples = true;
            return withFactoryInternal(factory);
        }

        private Builder withFactoryInternal(ProviderFactory<? extends DataProvider> factory) {
            this.factories.add(factory);
            return this;
        }

        public ProviderGroup build() {
            return new ProviderGroup(List.copyOf(factories), requiresRegistries, requiresSamples);
        }
    }

    public interface ProviderFactory<T extends DataProvider> {

        T create(PackOutput output, CompletableFuture<RegistryAccess> registries, CompletableFuture<BedrockSamples> samples);
    }
}
