package org.ulpgc.codestormah.mercadona.model;

import java.time.LocalDateTime;

public record Event(
        LocalDateTime ts,
        String ss,
        Product payload
) {}
