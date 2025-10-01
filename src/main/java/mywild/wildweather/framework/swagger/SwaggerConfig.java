package mywild.wildweather.framework.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        final String langHeaderName = "Accept-Language";
        return new OpenAPI()
            .info(new Info()
                .title("WildWeather API")
                .description("Include <code>'Authorization: Bearer &lt;jwt&gt;'</code> (when authenticated) and <code>'" + langHeaderName + ": &lt;code&gt;'</code> in the <b>request header</b>.")
                .version("1.0.0"))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .addSecurityItem(new SecurityRequirement().addList(langHeaderName))
            .components(new Components()
                    .addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT Access token for authentication."))
                    .addSecuritySchemes(langHeaderName,
                        new SecurityScheme()
                            .name(langHeaderName)
                            .type(SecurityScheme.Type.APIKEY)
                            .in(SecurityScheme.In.HEADER)
                            .description("Language code for responses. (EN, etc.)")))
                .externalDocs(new ExternalDocumentation()
                    .description("Download OpenAPI YAML Specification")
                    .url("/v3/api-docs.yaml"));
    }

}
