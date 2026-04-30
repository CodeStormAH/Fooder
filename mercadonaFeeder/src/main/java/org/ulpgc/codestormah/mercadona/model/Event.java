package org.ulpgc.codestormah.mercadona.model;

public record Event(
        long ts,
        String ss,
        Object payload
) {}
