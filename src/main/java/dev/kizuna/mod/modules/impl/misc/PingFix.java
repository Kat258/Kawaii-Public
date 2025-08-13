package dev.kizuna.mod.modules.impl.misc;

import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import dev.kizuna.api.events.impl.PacketEvent;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PingFix extends Module {
    private final SliderSetting targetPing = add(new SliderSetting("TargetPing", 520.0, 0.0, 1000.0));
    private final SliderSetting pingOffset = add(new SliderSetting("PingOffset", 0.0, -200.0, 200.0));
    private final SliderSetting measureInterval = add(new SliderSetting("MeasureInterval", 1.0, 0.5, 10.0));
    private double realRtt = 0;
    private ScheduledExecutorService scheduler;
    private final Map<Long, Long> receiveTimes = new ConcurrentHashMap<>();
    private String serverIp;

    public PingFix() {
        super("PingFix", Module.Category.Misc);
        setChinese("动态Ping补偿");
    }

    public void onEnable() {
        scheduler = Executors.newScheduledThreadPool(2);
        if (mc.getCurrentServerEntry() != null) {
            serverIp = mc.getCurrentServerEntry().address;
        } else {
            serverIp = null;
        }
        scheduler.scheduleAtFixedRate(this::measurePing, 0,
            (long) (measureInterval.getValue() * 1000), TimeUnit.MILLISECONDS);
    }

    private void measurePing() {
        if (serverIp == null) return;
        try {
            InetAddress addr = InetAddress.getByName(serverIp);
            long start = System.currentTimeMillis();
            addr.isReachable(1000);
            realRtt = System.currentTimeMillis() - start;
        } catch (Exception e) {
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof KeepAliveS2CPacket pkt) {
            receiveTimes.put(pkt.getId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof KeepAliveC2SPacket pkt) {
            Long recvTime = receiveTimes.remove(pkt.getId());
            if (recvTime != null) {
                double delay = targetPing.getValue() + pingOffset.getValue() - realRtt;
                long d = (long) Math.max(0, Math.round(delay));
                if (d > 0) {
                    event.cancel();
                    scheduler.schedule(() -> {
                        if (mc.player != null && mc.getNetworkHandler() != null) {
                            mc.getNetworkHandler().sendPacket(pkt);
                        }
                    }, d, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        scheduler.shutdownNow();
        receiveTimes.clear();
    }
}