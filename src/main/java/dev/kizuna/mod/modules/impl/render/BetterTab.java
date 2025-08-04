package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.Objects;

import static dev.kizuna.api.utils.Wrapper.mc;

public class BetterTab extends Module {
    public static BetterTab INSTANCE;

    private final BooleanSetting self = add(new BooleanSetting("HighlightSelf", true));
    private final ColorSetting selfColor = add(new ColorSetting("SelfColor", new Color(250, 130, 30)).injectBoolean(true));
    private final BooleanSetting friends = add(new BooleanSetting("HighlightFriends", true));
    private final ColorSetting friendColor = add(new ColorSetting("FriendColor", new Color(0, 255, 127)).injectBoolean(true));
    public final BooleanSetting accurateLatency = add(new BooleanSetting("AccurateLatency", true));
    private final BooleanSetting gamemode = add(new BooleanSetting("Gamemode", false));
    public final SliderSetting tabSize = add(new SliderSetting("TabSize", 100, 1, 1000));
    public final SliderSetting tabHeight = add(new SliderSetting("ColumnHeight", 20, 1, 100));

    public BetterTab() {
        super("BetterTab", Category.Render);
        setChinese("增强标签列表");
        INSTANCE = this;
    }

    public Text getPlayerName(PlayerListEntry playerListEntry) {
        Text name;
        Color color = null;

        name = playerListEntry.getDisplayName();
        if (name == null) name = Text.literal(playerListEntry.getProfile().getName());

        // 检查是否是自己
        if (playerListEntry.getProfile().getId().toString().equals(mc.player.getGameProfile().getId().toString()) && self.getValue()) {
            color = selfColor.getValue();
        }
        // 检查是否是好友
        else if (friends.getValue() && Kawaii.FRIEND.isFriend(playerListEntry.getProfile().getName())) {
            color = friendColor.getValue();
        }

        if (color != null) {
            String nameString = name.getString();

            // 移除所有颜色格式
            for (Formatting format : Formatting.values()) {
                if (format.isColor()) nameString = nameString.replace(format.toString(), "");
            }

            // 移除alpha通道，因为TextColor只接受RGB值
int rgb = color.getRGB() & 0xFFFFFF;
name = Text.literal(nameString).setStyle(name.getStyle().withColor(TextColor.fromRgb(rgb)));
        }

        // 添加游戏模式
        if (gamemode.getValue()) {
            GameMode gm = playerListEntry.getGameMode();
            String gmText = "?";
            if (gm != null) {
                gmText = switch (gm) {
                    case SPECTATOR -> "Sp";
                    case SURVIVAL -> "S";
                    case CREATIVE -> "C";
                    case ADVENTURE -> "A";
                    default -> gmText;
                };
            }
            MutableText text = Text.literal("");
            text.append(name);
            text.append(" [" + gmText + "]");
            name = text;
        }

        return name;
    }
}