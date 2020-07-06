package org.geysermc.generator;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter @Setter
public class ItemEntry {

    @SerializedName("bedrock_id")
    private int bedrockId;

    @SerializedName("bedrock_data")
    private int bedrockData;

    @SerializedName("is_block")
    private boolean isBlock;
}