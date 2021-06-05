package org.geysermc.generator.state;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface StateRemapper {

    /**
     * Regex string to check for when remapping states for blocks. Leave
     * empty to check against all blocks.
     *
     * @return regex string to check against when remapping states for blocks
     */
    String[] blockRegex() default "";

    /**
     * Regex string to exclude blocks when remapping states for blocks. Leave empty to exclude no blocks.
     *
     * @return regex string to exclude blocks when remapping states for blocks
     */
    String[] excludingBlockRegex() default "";

    /**
     * The name of the Minecraft: Java Edition blockstate data.
     *
     * @return the name of the Minecraft: java edition blockstate data.
     */
    String value();
}
