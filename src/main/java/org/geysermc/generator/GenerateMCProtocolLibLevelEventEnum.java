package org.geysermc.generator;

import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.world.level.block.LevelEvent;
import org.mockito.Mockito;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

public class GenerateMCProtocolLibLevelEventEnum {
    public static void main(String[] args) throws IllegalAccessException {
        StringBuilder output = new StringBuilder();
        Field[] events = LevelEvent.class.getDeclaredFields();
        LevelEventHandler mockLevelEventHandler = Mockito.mock(LevelEventHandler.class);
        Mockito.doCallRealMethod().when(mockLevelEventHandler).globalLevelEvent(anyInt(), any(), anyInt());

        int currentValue;
        int lastValue = -1;
        for (int i = 0; i <= events.length - 1; i++) {
            Field event = events[i];
            currentValue = event.getInt(null);
            if (lastValue != -1 && currentValue - lastValue > 10)
                output.append('\n');

            output.append(event.getName()).append('(').append(currentValue).append("),");

            try {
                mockLevelEventHandler.globalLevelEvent(currentValue, null, 0);
            } catch (NullPointerException e) {
                output.append(" // Global level event");
            }

            if (i != (events.length - 1)) {
                output.append("\n");
            } else {
                int index = output.lastIndexOf(",");
                output.replace(index, index + 1, ";");
            }

            lastValue = currentValue;
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("./level_events.txt"));
            writer.write(output.toString());
            writer.close();
            System.out.println("Finished level events writing process!");
        } catch (IOException e) {
            System.out.println("Failed to write level_events.txt!");
            e.printStackTrace();
        }
    }
}
