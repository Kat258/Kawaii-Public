package dev.kizuna.core;

import dev.kizuna.Kawaii;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public class Manager {
    public static MinecraftClient mc = MinecraftClient.getInstance();

    public static File getFile(String s) {
        File folder = new File(mc.runDirectory.getPath() + File.separator + Kawaii.NAME.toLowerCase());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, s);
    }
}
