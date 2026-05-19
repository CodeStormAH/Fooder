package org.ulpgc.codestormah.alcampo.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.alcampo.model.Product;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AlcampoControllerTest {

    @Test
    void shouldExecuteFeederAndStore() {
        AlcampoController controller = new AlcampoController(createMockFeeder(), createMockStore());
        assertDoesNotThrow(controller::execute);
    }

    private AlcampoFeeder createMockFeeder() {
        return () -> List.of(new Product("1", "agua", "agua", "brand", "drink", 1.0, "l", 1.0, false));
    }

    private AlcampoStore createMockStore() {
        return p -> {
            assertEquals(1, p.size());
            assertEquals("agua", p.get(0).getName());
        };
    }

    @Test
    void shouldNotFailWhenNoProducts() {
        AlcampoController controller = new AlcampoController(List::of, p -> fail("No debería llamarse"));
        assertDoesNotThrow(controller::execute);
    }
}