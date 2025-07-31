package dev.kizuna.api.events.impl;

import dev.kizuna.api.events.Event;

public class UpdateWalkingPlayerEvent extends Event {
    public UpdateWalkingPlayerEvent(Stage stage) {
        super(stage);
    }
    private boolean cancelRotate = false;

    public void cancelRotate() {
        this.cancelRotate = true;
    }
    public void setCancelRotate(boolean cancelRotate) {
        this.cancelRotate = cancelRotate;
    }

    public boolean isCancelRotate() {
        return cancelRotate;
    }
}
