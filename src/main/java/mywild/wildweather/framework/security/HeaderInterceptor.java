package mywild.wildweather.framework.security;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HeaderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest req, @NonNull HttpServletResponse rsp, @NonNull Object handler) throws Exception {
        String acceptLanguage = req.getHeader("Accept-Language");
        String encoding = req.getHeader("Accept-Encoding");
        log.debug("Request URL    : {}", req.getRequestURI());
        log.debug("Request Header : Accept-Language = {}", acceptLanguage);
        log.debug("Request Header : Accept-Encoding = {}", encoding);
        return true;
    }

}

