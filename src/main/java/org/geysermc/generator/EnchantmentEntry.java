package org.geysermc.generator;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class EnchantmentEntry {
    private String rarity;

    @SerializedName("max_level")
    private int maxLevel;

    @SerializedName("incompatible_enchants")
    private List<String> incompatibleEnchantments;

    @SerializedName("valid_items")
    private List<String> validItems;
}
