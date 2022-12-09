package org.geysermc.generator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class GenerateMCProtocolLibCustomStatisticEnum {
    public static void main(String[] args) {
        Util.initialize();

        StringBuilder finalOutput = new StringBuilder();
        for (int i = 0; i < BuiltInRegistries.CUSTOM_STAT.size(); i++) {
            ResourceLocation location = BuiltInRegistries.CUSTOM_STAT.byId(i);
            Stat<?> stat = Stats.CUSTOM.get(location);

            String format;
            if (stat.formatter == StatFormatter.DIVIDE_BY_TEN) {
                format = "TENTHS";
            } else if (stat.formatter == StatFormatter.DISTANCE) {
                format = "DISTANCE";
            } else if (stat.formatter == StatFormatter.TIME) {
                format = "TIME";
            } else {
                format = "INTEGER";
            }

            finalOutput.append(location.getPath().toUpperCase(Locale.ROOT));
            if (!format.equals("INTEGER")) {
                finalOutput.append("(StatisticFormat.").append(format).append(")");
            }
            if (i != (BuiltInRegistries.CUSTOM_STAT.size() - 1)) {
                finalOutput.append(",\n");
            } else {
                finalOutput.append(";");
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("./statistics.txt"));
            writer.write(finalOutput.toString());
            writer.close();
            System.out.println("Finished statistics writing process!");
        } catch (IOException e) {
            System.out.println("Failed to write statistics.txt!");
            e.printStackTrace();
        }
    }
}
