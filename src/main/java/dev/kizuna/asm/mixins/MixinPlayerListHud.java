package dev.kizuna.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.kizuna.mod.modules.impl.render.BetterTab;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerListHud.class)
public abstract class MixinPlayerListHud {

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Inject(method = "collectPlayerEntries", at = @At("RETURN"), cancellable = true)
    private void modifyPlayerCount(CallbackInfoReturnable<List<PlayerListEntry>> info) {
        BetterTab module = BetterTab.INSTANCE;
        if (module.isOn()) {
            List<PlayerListEntry> entries = info.getReturnValue();
            double maxSize = module.tabSize.getValue();
            if (entries.size() > maxSize) {
                info.setReturnValue(entries.subList(0, (int) maxSize));
            }
        }
    }

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    public void getPlayerName(PlayerListEntry playerListEntry, CallbackInfoReturnable<Text> info) {
        if (BetterTab.INSTANCE.isOn()) {
            info.setReturnValue(BetterTab.INSTANCE.getPlayerName(playerListEntry));
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BEFORE))
    private void modifyHeight(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
        BetterTab module = BetterTab.INSTANCE;
        if (!module.isOn()) return;

        int newO;
        int newP = 1;
        int totalPlayers = newO = this.collectPlayerEntries().size();
        while (newO > module.tabHeight.getValue()) {
            newO = (totalPlayers + ++newP - 1) / newP;
        }

        o.set(newO);
        p.set(newP);
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