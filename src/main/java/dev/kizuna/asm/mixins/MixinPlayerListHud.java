package dev.kizuna.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.kizuna.mod.modules.impl.render.BetterTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Mixin(PlayerListHud.class)
public abstract class MixinPlayerListHud {

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Inject(method = "collectPlayerEntries", at = @At("RETURN"), cancellable = true)
    private void modifyPlayerCount(CallbackInfoReturnable<List<PlayerListEntry>> info) {
        BetterTab module = BetterTab.INSTANCE;
        if (module == null || !module.isOn()) return;

        List<PlayerListEntry> fullList = Collections.emptyList();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getNetworkHandler() != null) {
            fullList = new ArrayList<>(mc.getNetworkHandler().getPlayerList());
        }

        final Collator collator = Collator.getInstance(Locale.CHINA);
        collator.setStrength(Collator.PRIMARY);

        Collections.sort(fullList, Comparator.comparing(
                entry -> Objects.toString(entry.getProfile().getName(), ""),
                (a, b) -> compareNames(a, b, collator)
        ));

        int maxSize = (int) module.tabSize.getValue();

        if (fullList.size() > maxSize) {
            info.setReturnValue(fullList.subList(0, maxSize));
        } else {
            info.setReturnValue(fullList);
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

    private static int charCategory(char c) {
        if (c == '_') return 0;
        if (c >= '0' && c <= '9') return 1;
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) return 2;
        if (isChinese(c)) return 3;
        return 4;
    }

    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B;
    }

    private static int compareNames(String a, String b, Collator collator) {
        if (a == null) a = "";
        if (b == null) b = "";
        int la = a.length(), lb = b.length();
        int n = Math.min(la, lb);

        for (int i = 0; i < n; i++) {
            char ca = a.charAt(i);
            char cb = b.charAt(i);
            int caCat = charCategory(ca);
            int cbCat = charCategory(cb);
            if (caCat != cbCat) return Integer.compare(caCat, cbCat);

            if (caCat == 2) {
                char laChr = Character.toLowerCase(ca);
                char lbChr = Character.toLowerCase(cb);
                if (laChr != lbChr) return Character.compare(laChr, lbChr);
                if (ca != cb) return Character.compare(ca, cb);
            } else if (caCat == 3) {
                int cmp = collator.compare(a, b);
                if (cmp != 0) return cmp;
            } else {
                if (ca != cb) return Character.compare(ca, cb);
            }
        }
        return Integer.compare(la, lb);
    }
}
