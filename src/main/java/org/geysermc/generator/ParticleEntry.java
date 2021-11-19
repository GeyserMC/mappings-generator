package org.geysermc.generator;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class ParticleEntry {
    @SerializedName("eventType")
    public String cloudburstLevelEventType;

    public String bedrockId;
}
