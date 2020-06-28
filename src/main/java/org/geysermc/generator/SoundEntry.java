package org.geysermc.generator;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class SoundEntry {

    @SerializedName("playsound_mapping")
    private String playsoundMapping;

    @SerializedName("bedrock_mapping")
    private String bedrockMapping;

    @SerializedName("extra_data")
    private int extraData;

    @SerializedName("identifier")
    private String identifier;

    @SerializedName("level_event")
    private boolean levelEvent;
}
