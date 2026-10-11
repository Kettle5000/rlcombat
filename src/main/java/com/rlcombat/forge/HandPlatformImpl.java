package com.rlcombat.forge;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class HandPlatformImpl {

    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
