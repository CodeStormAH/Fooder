package org.ulpgc.codestormah.mercadona.model;

import java.util.Set;

public class Categories {

    private final Set<String> allowed;

    public Categories(Set<String> allowed) {
        this.allowed = allowed;
    }

    public boolean isAllowed(String category) {
        return allowed.contains(category);
    }
}
