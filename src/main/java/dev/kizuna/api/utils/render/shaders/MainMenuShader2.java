package dev.kizuna.api.utils.render.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import org.ladysnake.satin.api.managed.ManagedCoreShader;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import org.ladysnake.satin.api.managed.uniform.Uniform1f;
import org.ladysnake.satin.api.managed.uniform.Uniform2f;
import org.ladysnake.satin.api.managed.uniform.Uniform4f;
import dev.kizuna.api.utils.Wrapper;
import dev.kizuna.api.utils.math.FrameRateCounter;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class MainMenuShader2
implements Wrapper {
    private Uniform1f Time;
    private Uniform2f uSize;
    private Uniform2f resolution;
    private Uniform2f mouse;
    private Uniform4f color;
    public static float time_ = 10000.0f;
    public static final ManagedCoreShader MAIN_MENU = ShaderEffectManager.getInstance().manageCoreShader(Identifier.of((String)"minecraft", (String)"menu2"), VertexFormats.POSITION);

    public MainMenuShader2() {
        this.setup();
    }

    public void setParameters(float x, float y, float width, float height) {
        float i = (float)mc.getWindow().getScaleFactor();
        this.uSize.set(width * i, height * i);
        this.resolution.set(width * i, height * i);
        if (MainMenuShader2.mc.mouse != null) {
            double mouseX = MainMenuShader2.mc.mouse.getX();
            double mouseY = MainMenuShader2.mc.mouse.getY();
            this.mouse.set((float)mouseX, (float)mouseY);
        } else {
            this.mouse.set(width / 2.0f, height / 2.0f);
        }
        this.Time.set(time_ += (float)(0.55 * (double)MainMenuShader2.deltaTime()));
    }

    public static float deltaTime() {
        return FrameRateCounter.INSTANCE.getFps() > 5 ? 1.0f / (float)FrameRateCounter.INSTANCE.getFps() : 0.016f;
    }

    public void use() {
        RenderSystem.setShader(() -> ((ManagedCoreShader)MAIN_MENU).getProgram());
    }

    protected void setup() {
        this.uSize = MAIN_MENU.findUniform2f("uSize");
        this.Time = MAIN_MENU.findUniform1f("Time");
        this.resolution = MAIN_MENU.findUniform2f("resolution");
        this.mouse = MAIN_MENU.findUniform2f("mouse");
        this.color = MAIN_MENU.findUniform4f("color");
    }
}
