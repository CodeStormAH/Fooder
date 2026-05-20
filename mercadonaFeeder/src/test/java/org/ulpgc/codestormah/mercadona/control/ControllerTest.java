package org.ulpgc.codestormah.mercadona.control;

import org.junit.jupiter.api.Test;
import org.ulpgc.codestormah.mercadona.model.Product;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {

    @Test
    void shouldExecuteFeederAndStoreWithoutException() {
        Controller controller = new Controller(createFeeder(), p -> assertEquals(1, p.size()));
        assertDoesNotThrow(() -> scheduleAndWait(controller));
    }

    private ProductFeeder createFeeder() {
        return max -> List.of(new Product("1", "t", "t", "b", "c", 1.0, "l", 1.0, false));
    }

    private void scheduleAndWait(Controller c) throws Exception {
        c.startScheduler(1);
        Thread.sleep(100);
    }
}