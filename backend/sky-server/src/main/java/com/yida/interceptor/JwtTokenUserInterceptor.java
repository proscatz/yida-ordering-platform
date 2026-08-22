package com.yida.interceptor;

import com.yida.context.AuthenticationContext;
import com.yida.properties.JwtProperties;
import com.yida.security.TokenService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtTokenUserInterceptor implements HandlerInterceptor {
    private final JwtProperties properties;
    private final TokenService tokenService;

    public JwtTokenUserInterceptor(JwtProperties properties, TokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) { return true; }
        try {
            AuthenticationContext.set(tokenService.authenticateUser(request.getHeader(properties.getUserTokenName())));
            return true;
        } catch (Exception ex) {
            AuthenticationContext.clear();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthenticationContext.clear();
    }
}