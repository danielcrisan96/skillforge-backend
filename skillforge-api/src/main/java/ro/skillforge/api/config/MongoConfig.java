package ro.skillforge.api.config;

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.mapping.event.ValidatingEntityCallback;

/**
 * Verifică adnotările de validare ale unei entități înainte ca Spring Data să o salveze. O entitate invalidă aruncă
 * {@code ConstraintViolationException} și nu se scrie nimic: un bug al nostru nu ajunge în baza de date.
 */
@Configuration
public class MongoConfig {

    @Bean
    public ValidatingEntityCallback validatingEntityCallback(Validator validator) {
        return new ValidatingEntityCallback(validator);
    }
}
