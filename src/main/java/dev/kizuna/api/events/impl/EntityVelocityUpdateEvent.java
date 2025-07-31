package dev.kizuna.api.events.impl;

import dev.kizuna.api.events.Event;

public class EntityVelocityUpdateEvent extends Event {
    public EntityVelocityUpdateEvent() {
        super(Stage.Pre);
    }
}
