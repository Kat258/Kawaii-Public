package dev.kizuna.api.events.impl;

import dev.kizuna.api.events.Event;

public class GameLeftEvent extends Event {
    public GameLeftEvent() {
        super(Stage.Post);
    }
}
