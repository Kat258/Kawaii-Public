package dev.kizuna.api.utils.render;

import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;

import java.awt.*;


public class Render2DEngine {

    public static void endBuilding(BufferBuilder bb) {
        BufferBuilder.BuiltBuffer builtBuffer = bb.endNullable();
        if (builtBuffer != null)
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
    }
}
