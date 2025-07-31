package dev.kizuna.core.impl;

import dev.kizuna.api.utils.world.BlockUtil;
import dev.kizuna.Kawaii;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.eventbus.EventPriority;
import dev.kizuna.api.events.impl.TickEvent;
import dev.kizuna.mod.modules.impl.render.PlaceRender;

public class ThreadManager {
    public static ClientService clientService;

    public ThreadManager() {
        Kawaii.EVENT_BUS.subscribe(this);
        clientService = new ClientService();
        clientService.setName("KawaiiClientService");
        clientService.setDaemon(true);
        clientService.start();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEvent(TickEvent event) {
        if (event.isPre()) {
            if (!clientService.isAlive()) {
                clientService = new ClientService();
                clientService.setName("KawaiiClientService");
                clientService.setDaemon(true);
                clientService.start();
            }
            BlockUtil.placedPos.forEach(pos -> PlaceRender.renderMap.put(pos, PlaceRender.INSTANCE.create(pos)));
            BlockUtil.placedPos.clear();
            Kawaii.SERVER.onUpdate();
            Kawaii.PLAYER.onUpdate();
            Kawaii.MODULE.onUpdate();
            Kawaii.GUI.onUpdate();
            Kawaii.POP.onUpdate();
        }
    }

    public static class ClientService extends Thread {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (Kawaii.MODULE != null) {
                        Kawaii.MODULE.onThread();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
