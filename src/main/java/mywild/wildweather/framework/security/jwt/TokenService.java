package mywild.wildweather.framework.security.jwt;

import java.security.interfaces.RSAPrivateKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TokenService {

    @Value("${mywild.jwt.issuer}")
    private String issuer;

    @Value("${mywild.jwt.subject}")
    private String subject;

    @Value("${mywild.jwt.audience}")
    private String audience;

    @Value("${mywild.jwt.access-token-duration}")
    private int accessTokenDuration;

    @Value("${mywild.jwt.refresh-token-duration}")
    private int refreshTokenDuration;

    @Autowired
    private RSAPrivateKey privateKey;

    public String generateToken(TokenType tokenType, long userId) {
        try {
            JWSHeader jwtHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build();
            JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                    .jwtID(Long.toString(System.currentTimeMillis()))
                    .issuer(issuer)
                    .subject(subject)
                    .audience(audience)
                    .issueTime(new Date())
                    .expirationTime(tokenType == TokenType.ACCESS
                        ? Date.from(LocalDateTime.now().plusMinutes(accessTokenDuration).atZone(ZoneId.systemDefault()).toInstant()) // Access
                        : Date.from(LocalDateTime.now().plusMinutes(refreshTokenDuration).atZone(ZoneId.systemDefault()).toInstant()) // Refresh
                    )
                    .claim("scope", tokenType.toString().toLowerCase())
                    .claim(TokenConstants.JWT_USER_ID, userId)
                    .build();
            SignedJWT jws = new SignedJWT(jwtHeader, jwtClaims);
            jws.sign(new RSASSASigner(privateKey));
            return jws.serialize();
        }
        catch (JOSEException ex) {
            log.error(ex.toString());
        }
        return null;
    }

}
