package id.nawala.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class NawalaApplicationTest {

    @Test
    void contextLoads() {
        // Verifies the entire Spring context loads without errors
        assertDoesNotThrow(() -> {});
    }
}
