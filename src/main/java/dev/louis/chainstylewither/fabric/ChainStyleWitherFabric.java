package dev.louis.chainstylewither.fabric;

import net.fabricmc.api.ModInitializer;
import dev.louis.chainstylewither.ChainStyleWither;
import dev.louis.chainstylewither.config.Config;

public class ChainStyleWitherFabric implements ModInitializer {
    //private static BESConfigFabric CONFIG;

    @Override
    public void onInitialize() {
        ChainStyleWither.init();
    }

    public static Config getConfig() {
        //if (CONFIG == null && FabricLoader.getInstance().isModLoaded("cloth-config")) CONFIG = BESConfigFabric.createConfig();
        //if (CONFIG != null) return CONFIG;
        return Config.DEFAULT;
    }
}
