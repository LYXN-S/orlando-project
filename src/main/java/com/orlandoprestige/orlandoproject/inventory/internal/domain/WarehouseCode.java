package com.orlandoprestige.orlandoproject.inventory.internal.domain;

import java.util.Arrays;

public enum WarehouseCode {
    LG("LG"),
    MCC("MCC"),
    MCM("MCM"),
    OFFICE("Office"),
    PILAR("Pilar");

    private final String displayName;

    WarehouseCode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static WarehouseCode from(String raw) {
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(raw))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown warehouse: " + raw));
    }
}
