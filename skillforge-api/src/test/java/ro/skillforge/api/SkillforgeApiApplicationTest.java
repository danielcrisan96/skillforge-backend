package ro.skillforge.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.skillforge.api.config.TestcontainersConfiguration;

/**
 * Pornește aplicația întreagă pe un MongoDB real: dacă un bean lipsește sau o setare e greșită, pică aici.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SkillforgeApiApplicationTest {

    @Test
    void contextLoads() {
    }
}
