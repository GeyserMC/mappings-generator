package org.geysermc.resources;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter @Setter
public class BlockEntry {

    @SerializedName("bedrock_identifier")
    private String bedrockIdentifier;

    @SerializedName("hardness_value")
    private float hardnessValue;

    @SerializedName("bedrock_states")
    private JsonElement bedrockStates;
}
