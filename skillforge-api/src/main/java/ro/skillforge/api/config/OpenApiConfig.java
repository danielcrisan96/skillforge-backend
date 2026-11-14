package ro.skillforge.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.skillforge.api.shared.web.ApiKeyFilter;

/**
 * Spune documentației că fiecare cerere are nevoie de cheia dintre servicii, ca butonul „Authorize” din Swagger UI
 * să o poată trimite. Fără asta, pagina ar arăta endpoint-urile, dar orice apel de probă ar primi 401.
 */
@Configuration
public class OpenApiConfig {
    private static final String API_KEY_SCHEME = "apiKey";

    @Bean
    OpenAPI skillforgeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkillForge API")
                        .description("Profilul și conversațiile utilizatorilor SkillForge, păstrate în MongoDB.")
                        .version("1"))
                .components(new Components().addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(ApiKeyFilter.API_KEY_HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
    }
}
