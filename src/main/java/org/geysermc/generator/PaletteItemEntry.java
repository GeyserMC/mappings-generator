package org.geysermc.generator;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter @Setter
public class PaletteItemEntry {

    @SerializedName("name")
    private String identifier;

    @SerializedName("id")
    private int legacy_id;
}