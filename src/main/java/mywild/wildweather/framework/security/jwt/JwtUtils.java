package mywild.wildweather.framework.security.jwt;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class JwtUtils {

    public static final long UNKNOWN_USER_ID = -1L;

    private JwtUtils() {
    }

    public static long getUserIdFromJwt(JwtAuthenticationToken jwtToken) {
        if (jwtToken == null) {
            return UNKNOWN_USER_ID;
        }
        return (Long) jwtToken.getTokenAttributes().get(TokenConstants.JWT_USER_ID);
    }

}
