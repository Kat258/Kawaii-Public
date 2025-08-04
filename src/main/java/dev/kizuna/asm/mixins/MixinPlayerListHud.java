package dev.kizuna.asm.mixins;

import dev.kizuna.mod.modules.impl.render.BetterTab;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class MixinPlayerListHud {

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    public void getPlayerName(PlayerListEntry playerListEntry, CallbackInfoReturnable<Text> info) {
        if (BetterTab.INSTANCE.isOn()) {
            info.setReturnValue(BetterTab.INSTANCE.getPlayerName(playerListEntry));
        }
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void onRenderLatencyIcon(net.minecraft.client.gui.DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (BetterTab.INSTANCE.isOn() && BetterTab.INSTANCE.accurateLatency.getValue()) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            net.minecraft.client.font.TextRenderer textRenderer = mc.textRenderer;

            int latency = net.minecraft.util.math.MathHelper.clamp(entry.getLatency(), 0, 9999);
            int color = latency < 150 ? 0x00E970 : latency < 300 ? 0xE7D020 : 0xD74238;
            String text = latency + "ms";
            context.drawTextWithShadow(textRenderer, text, x + width - textRenderer.getWidth(text), y, color);
            ci.cancel();
        }
    }
}