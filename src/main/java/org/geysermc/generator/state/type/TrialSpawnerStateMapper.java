package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "trial_spawner_state", blockRegex = "minecraft:trial_spawner")
public class TrialSpawnerStateMapper extends StateMapper<Integer> {
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        switch(value) {
            case "inactive" -> { return Pair.of("trial_spawner_state", 0); }
            case "waiting_for_players" -> { return Pair.of("trial_spawner_state", 1); }
            case "active" -> { return Pair.of("trial_spawner_state", 2); }
            case "waiting_for_reward_ejection" -> { return Pair.of("trial_spawner_state", 3); }
            case "ejecting_reward" -> { return Pair.of("trial_spawner_state", 4); }
            case "cooldown" -> { return Pair.of("trial_spawner_state", 5); }
            default -> { throw new RuntimeException("Unknown trial spawner state: " + value); }
        }
    }
}
