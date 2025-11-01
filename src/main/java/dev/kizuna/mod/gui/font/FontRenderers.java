package dev.kizuna.mod.gui.font;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class FontRenderers {
    public static FontAdapter ui;
    public static FontAdapter troll;
    public static FontAdapter icon;
    public static FontAdapter icon2;
    public static FontAdapter icon3;
    public static FontAdapter Calibri;
    public static @NotNull RendererFontAdapter createDefault(float size, String name) throws IOException, FontFormatException {
        return new RendererFontAdapter(Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(FontRenderers.class.getClassLoader().getResourceAsStream("assets/minecraft/font/" + name + ".ttf"))).deriveFont(Font.PLAIN, size), size);
    }

    public static void createDefault(float size) {
        try {
            ui = createDefault(size, "LexendDeca-Regular");
            troll = createDefault(size + 49, "Jura-Light");
            icon = createDefault(size + 5, "icon");
            icon2 = createDefault(size + 4, "icon");
            icon3 = createDefault(size + 2, "icon");
        } catch (IOException | FontFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public static RendererFontAdapter create(String name, int style, float size) {
        return new RendererFontAdapter(new Font(name, style, (int) size), size);
    }
}
