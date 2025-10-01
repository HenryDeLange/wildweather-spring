package mywild.wildweather.framework.security.jwt;

import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.AUD;
import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.SUB;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class JwtConfig {

    @Value("${mywild.jwt.issuer}")
    private String issuer;

    @Value("${mywild.jwt.subject}")
    private String subject;

    @Value("${mywild.jwt.audience}")
    private String audience;

    @Autowired
    private RSAPublicKey publicKey;

    /**
     * Build and configure the JwtDecoder that will be used when we receive a JWT Token. Here we take in a
     * {@see RSAPublicKey} but you can also supply a JWK uri, or a {@see SecretKey}.
     * 
     * By default, the decoder will always verify the signature with the given key 
     * and validate the timestamp to check if the JWT is still valid.
     * 
     * Per default a Public key will set the algorithm to RS256. If you want something different you can set this explicitly.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        decoder.setJwtValidator(tokenValidator());
        return decoder;
    }

    /**
     * We can write custom validators to validate different parts of the JWT. Per default, the framework will always
     * validate the timestamp, but we can add validators to enhance security. For instance you should always
     * validate the issuer to make sure that the JWT was issued from a known source. Remember that if we customise the
     * validation we need to re-add the timestamp validator.
     *
     * Here we crate a list of validators. The {@see JwtTimestampValidator} and the {@see JwtIssuerValidator} are
     * from the spring security framework, but we have also added a custom one. Remember if you add a custom list, you
     * must always remember to add timestamp validation or else this will be removed.
     *
     * We then place these in a {@see DelegatingOAuth2TokenValidator} that we can set to our {@see JwtDecoder}.
     */
    private OAuth2TokenValidator<Jwt> tokenValidator() {
        final List<OAuth2TokenValidator<Jwt>> validators =
                List.of(
                    new JwtIssuerValidator(issuer),
                    audienceValidator(),
                    subjectValidator()
                );
        return JwtValidators.createDefaultWithValidators(validators);
    }

    /**
     * You can write a custom validation by adding a {@see JwtClaimValidator} for instance below we add a custom
     * validator to the aud (audience) claim. And check that it contains a certain string.
     * {@see OAuth2TokenIntrospectionClaimNames} contains static string names of several default claims. Below we are
     * referencing the {@see OAuth2TokenIntrospectionClaimNames#AUD} string.
     */
    private OAuth2TokenValidator<Jwt> audienceValidator() {
        return new JwtClaimValidator<>(AUD, aud ->
            switch (aud) {
                case String audString -> audString.contains(audience);
                case List<?> audList -> audList.contains(audience);
                default -> false;
            }
        );
    }

    /**
     * Similar to the audience validator above, this validator checks the subject field of the JWT Token.
     */
    private OAuth2TokenValidator<Jwt> subjectValidator() {
        return new JwtClaimValidator<>(SUB, sub ->
            switch (sub) {
                case String subString -> subString.contains(subject);
                case List<?> subList -> subList.contains(subject);
                default -> false;
            }
        );
    }
    
}
