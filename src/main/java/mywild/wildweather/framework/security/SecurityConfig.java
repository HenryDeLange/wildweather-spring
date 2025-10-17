package mywild.wildweather.framework.security;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Value("${mywild.app.dev-mode}")
    private boolean devMode;

    @Value("${mywild.cors}")
    private String cors;

    @Value("${mywild.api-path}")
    private String apiPath;

    @Autowired
    private HeaderInterceptor interceptor;

    /** 
     * Add the header interceptor.
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/" + apiPath + "/**")
                .excludePathPatterns(
                    "/v3/api-docs", 
                    "/v3/api-docs.*", 
                    "/v3/api-docs/**", 
                    "/swagger-ui/**"
                );
    }

    /**
     * Set up a security filter chain and configure the access levels for the different endpoints.
     * We then configure our application into a Resource Server that accepts the JWT Token.
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(logout -> logout.disable())
            // Authorization
            .authorizeHttpRequests(authorize -> {
                authorize
                    // Root UI
                    .requestMatchers("/").permitAll()
                    .requestMatchers("/index.html").permitAll();
                // if (devMode) {
                //     // Nothing
                // }
                authorize
                    // Actuator
                    // .requestMatchers("/actuator/**").permitAll()
                    // Swagger UI
                    // .requestMatchers("/favicon.ico").permitAll()
                    // .requestMatchers("/index.html").permitAll()
                    .requestMatchers("/swagger-ui.html").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/api-docs").permitAll()
                    .requestMatchers("/v3/api-docs.*").permitAll()
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    // Auth Endpoints
                    .requestMatchers(apiPath + "/users/register").permitAll()
                    .requestMatchers(apiPath + "/users/login").permitAll()
                    // .requestMatchers(apiPath + "/users/reset").permitAll()
                    .requestMatchers(apiPath + "/users/refresh").hasAuthority("SCOPE_refresh")
                    // Version Endpoint
                    .requestMatchers(apiPath + "/version/**").permitAll()
                    // Domain Endpoints
                    .requestMatchers(apiPath + "/weather/**").permitAll()
                    .anyRequest().hasAuthority("SCOPE_access"); // .anyRequest().authenticated()
            })
            // Indicate this is a Resource Server that accepts JWT tokens
            .oauth2ResourceServer(configure -> configure.jwt(Customizer.withDefaults()))
            // CORS
            .cors(Customizer.withDefaults())
            // CSRF
            .csrf(csrf -> csrf.disable());
            // TODO: Proper CSRF tokens handling.
            //       See https://stackoverflow.com/questions/51026694/spring-security-blocks-post-requests-despite-securityconfig
            //       .addFilterAfter(new CsrfTokenResponseHeaderBindingFilter(), CsrfFilter.class)
        if (devMode) {
            httpSecurity
                // H2
                .headers(headers -> headers.frameOptions(FrameOptionsConfig::disable));
        }
        // Build
        return httpSecurity.build();
    }

    /**
     * Perform CORS checks based on the provided allowed origins.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(cors));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
