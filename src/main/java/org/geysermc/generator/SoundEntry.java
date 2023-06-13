package org.geysermc.generator;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
@Getter
@Setter
public class SoundEntry {

    @Nullable
    @SerializedName("playsound_mapping")
    private String playSound;

    @Nullable
    @SerializedName("bedrock_mapping")
    private String eventSound;

    @SerializedName("extra_data")
    private int extraData;

    @Nullable
    @SerializedName("identifier")
    private String identifier;

    @SerializedName("level_event")
    private boolean levelEvent;
}
