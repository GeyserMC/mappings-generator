package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "age", blockRegex = ".*_vines.?$")
public class VinesAgeMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int age = 0;
        switch (value) {
            case "1":
                age = 1;
                break;
            case "2":
                age = 2;
                break;
            case "3":
                age = 3;
                break;
            case "4":
                age = 4;
                break;
            case "5":
                age = 5;
                break;
            case "6":
                age = 6;
                break;
            case "7":
                age = 7;
                break;
            case "8":
                age = 8;
                break;
            case "9":
                age = 9;
                break;
            case "10":
                age = 10;
                break;
            case "11":
                age = 11;
                break;
            case "12":
                age = 12;
                break;
            case "13":
                age = 13;
                break;
            case "14":
                age = 14;
                break;
            case "15":
                age = 15;
                break;
            case "16":
                age = 16;
                break;
            case "17":
                age = 17;
                break;
            case "18":
                age = 18;
                break;
            case "19":
                age = 19;
                break;
            case "20":
                age = 20;
                break;
            case "21":
                age = 21;
                break;
            case "22":
                age = 22;
                break;
            case "23":
                age = 23;
                break;
            case "24":
                age = 24;
                break;
            case "25":
                age = 25;
                break;
        }
        if (fullIdentifier.contains("weeping")) {
            return new Pair<>("weeping_vines_age", age);
        }
        return new Pair<>("twisting_vines_age", age);
    }
}
