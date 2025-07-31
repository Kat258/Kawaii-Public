package dev.kizuna.mod;

import dev.kizuna.api.utils.Wrapper;

public class Mod implements Wrapper {
    private final String name;
    public Mod(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
