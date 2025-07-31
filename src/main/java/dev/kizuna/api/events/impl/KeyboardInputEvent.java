package dev.kizuna.api.events.impl;

import dev.kizuna.api.events.Event;

public class KeyboardInputEvent extends Event {
    public KeyboardInputEvent() {
        super(Stage.Pre);
    }
}
