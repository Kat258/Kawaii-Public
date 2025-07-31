package dev.kizuna.api.events.impl;

import dev.kizuna.api.events.Event;
import net.minecraft.entity.player.PlayerEntity;

public class DeathEvent extends Event {
    private final PlayerEntity player;

    public DeathEvent(PlayerEntity player) {
        super(Stage.Post);
        this.player = player;
    }

    public PlayerEntity getPlayer() {
        return this.player;
    }
}