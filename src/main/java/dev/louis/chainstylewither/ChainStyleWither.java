package dev.louis.chainstylewither;

import dev.louis.chainstylewither.config.Config;
import dev.louis.chainstylewither.fabric.ChainStyleWitherFabric;

public class ChainStyleWither {
    public static final String MODID = "chainstylewither";

    public static void init() {

    }

    public static Config getConfig() {
        return ChainStyleWitherFabric.getConfig();
    }
}
