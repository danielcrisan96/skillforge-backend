package ro.skillforge.api;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;
import ro.skillforge.api.config.MongoConfig;
import ro.skillforge.api.config.TestcontainersConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Un test de persistență: doar partea de MongoDB din Spring ({@code @DataMongoTest}), un MongoDB în Docker și aceeași
 * validare înainte de salvare ca în aplicație. Serviciile testate se adaugă cu {@code @Import}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataMongoTest
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
@Import({TestcontainersConfiguration.class, MongoConfig.class})
public @interface MongoPersistenceTest {
}
