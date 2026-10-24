package com.milesofsmiles.fleet;

import java.util.UUID;

public record Vehicle(UUID id, String reference, int contractVersion) {
    public static final UUID FIXTURE_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
}
