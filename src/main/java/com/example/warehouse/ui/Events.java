package com.example.warehouse.ui;

import java.util.ArrayList;
import java.util.List;

/** Tiny change bus: tabs register a refresh callback, any tab can fire after changing data. */
public class Events {
    private final List<Runnable> listeners = new ArrayList<>();

    public void onChange(Runnable r) {
        listeners.add(r);
    }

    public void fire() {
        for (Runnable r : listeners) {
            r.run();
        }
    }
}
