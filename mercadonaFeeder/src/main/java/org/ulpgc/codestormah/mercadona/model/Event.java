package org.ulpgc.codestormah.mercadona.model;

import java.time.LocalDateTime;

public record Event(
        LocalDateTime ts,
        String ss,

        String id,
        String name,
        String normalizedName,
        String brand,
        String category,

        double unitPrice,
        String unit,
        double quantity,

        boolean isOnSale
) {}
