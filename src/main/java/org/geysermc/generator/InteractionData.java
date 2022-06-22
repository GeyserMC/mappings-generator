package org.geysermc.generator;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class InteractionData {

    @SerializedName("always_consumes")
    private List<String> alwaysConsumes;

    @SerializedName("requires_may_build")
    private List<String> requiresMayBuild;
}