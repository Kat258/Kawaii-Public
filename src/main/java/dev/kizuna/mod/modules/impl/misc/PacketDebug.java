package dev.kizuna.mod.modules.impl.misc;

import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.PacketEvent;
import dev.kizuna.core.impl.CommandManager;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.StringSetting;
import net.minecraft.network.packet.Packet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;

public class PacketDebug extends Module {

    private static final Logger LOGGER = Logger.getLogger("Kawaii-PacketDebug");
    private final Set<Class<?>> blacklistedPacketTypes = new HashSet<>();
    private boolean shouldLog = true;
    
    // 设置选项
    private final BooleanSetting chatOutput = add(new BooleanSetting("ChatOutput", false));
    private final BooleanSetting logOutput = add(new BooleanSetting("LogOutput", true));
    private final BooleanSetting detailedInfo = add(new BooleanSetting("DetailedInfo", true));
    private final StringSetting blacklistFilter = add(new StringSetting("BlacklistFilter", ""));
    private final BooleanSetting useBlacklist = add(new BooleanSetting("UseBlacklist", false));

    public PacketDebug() {
        super("PacketDebug", "Logs all packets received from the server with detailed information", Category.Misc);
        // 可以添加一些黑名单以忽略过于频繁的数据包类型
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (nullCheck() || !shouldLog) return;
        
        Packet<?> packet = event.getPacket();
        Class<?> packetClass = packet.getClass();
        String packetName = packetClass.getSimpleName();
        
        // 检查是否在黑名单中
        if (useBlacklist.getValue() && isInBlacklist(packetName)) {
            return;
        }
        
        try {
            StringBuilder info = new StringBuilder();
            info.append("Received packet: §b").append(packetClass.getName());
            
            // 根据设置输出详细信息
            if (this.detailedInfo.getValue()) {
                String details = getPacketDetails(packet);
                
                // 输出到日志
                if (logOutput.getValue()) {
                    LOGGER.info(info.toString());
                    LOGGER.info("Detailed info: " + details);
                }
                
                // 输出到聊天（简化版，避免刷屏）
                if (chatOutput.getValue()) {
                    CommandManager.sendChatMessage("§7[PacketDebug] §f" + packetName);
                    // 只发送前500个字符的详细信息
                    String truncatedDetails = details.length() > 500 ? details.substring(0, 497) + "..." : details;
                    CommandManager.sendChatMessage("§8" + truncatedDetails);
                }
            } else {
                // 只输出基本信息
                if (logOutput.getValue()) {
                    LOGGER.info(info.toString());
                }
                if (chatOutput.getValue()) {
                    CommandManager.sendChatMessage("§7[PacketDebug] §f" + packetName);
                }
            }
            
        } catch (Exception e) {
            if (logOutput.getValue()) {
                LOGGER.warning("Error logging packet: " + e.getMessage());
                LOGGER.warning(getStackTraceAsString(e));
            }
            if (chatOutput.getValue()) {
                CommandManager.sendChatMessage("§7[PacketDebug] §cError logging packet: " + e.getMessage());
            }
        }
    }
    
    private boolean isInBlacklist(String packetName) {
        String filter = blacklistFilter.getValue();
        if (filter.isEmpty()) return false;
        
        String[] filters = filter.split(",");
        for (String f : filters) {
            f = f.trim();
            if (!f.isEmpty() && packetName.toLowerCase().contains(f.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String getPacketDetails(Packet<?> packet) {
        StringBuilder details = new StringBuilder();
        Class<?> clazz = packet.getClass();
        
        details.append(clazz.getSimpleName()).append(" {");
        
        try {
            // 获取所有字段（包括父类的）
            for (Field field : getAllFields(clazz)) {
                field.setAccessible(true);
                try {
                    Object value = field.get(packet);
                    details.append("\n  ").append(field.getName()).append(": ").append(formatValue(value));
                } catch (Exception e) {
                    details.append("\n  ").append(field.getName()).append(": [ERROR_ACCESSING]");
                }
            }
        } catch (Exception e) {
            details.append("\n  [ERROR_GETTING_FIELDS]");
        }
        
        details.append("\n}");
        return details.toString();
    }

    private Field[] getAllFields(Class<?> clazz) {
        Set<Field> fields = new HashSet<>();
        Class<?> currentClass = clazz;
        
        while (currentClass != null && currentClass != Object.class) {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        
        return fields.toArray(new Field[0]);
    }

    private String formatValue(Object value) {
        if (value == null) return "null";
        if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        }
        return value.toString();
    }

    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        shouldLog = true;
        
        StringBuilder message = new StringBuilder("§7[PacketDebug] §aEnabled");
        message.append("\n§7 - §fLog Output: §").append(logOutput.getValue() ? "aON" : "cOFF");
        message.append("\n§7 - §fChat Output: §").append(chatOutput.getValue() ? "aON" : "cOFF");
        message.append("\n§7 - §fDetailed Info: §").append(detailedInfo.getValue() ? "aON" : "cOFF");
        message.append("\n§7 - §fBlacklist: §").append(useBlacklist.getValue() ? "aON" : "cOFF");
        
        if (useBlacklist.getValue() && !blacklistFilter.getValue().isEmpty()) {
            message.append("\n§7 - §fFilters: §b").append(blacklistFilter.getValue());
        }
        
        CommandManager.sendChatMessage(message.toString());
        LOGGER.info("PacketDebug module enabled with settings - Log: " + logOutput.getValue() + ", Chat: " + chatOutput.getValue() + ", Detailed: " + detailedInfo.getValue());
    }

    @Override
    public void onDisable() {
        shouldLog = false;
        CommandManager.sendChatMessage("§7[PacketDebug] §cDisabled");
        LOGGER.info("PacketDebug module disabled - Stopped logging incoming packets");
    }
    
    @Override
    public String getInfo() {
        // 显示当前的数据包过滤状态
        if (useBlacklist.getValue() && !blacklistFilter.getValue().isEmpty()) {
            return "Blacklisted"; 
        }
        return null;
    }
}