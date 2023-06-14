package org.geysermc.generator;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;

@AllArgsConstructor
@NoArgsConstructor
public class ParticleEntry {

    /**
     * The {@link LevelEvent} or {@link ParticleType}
     */
    @SerializedName("eventType")
    public String cloudburstLevelEventType;

    public String bedrockId;
}
