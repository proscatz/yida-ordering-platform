package com.yida.interceptor;

import com.yida.context.AuthenticationContext;
import com.yida.constant.StatusConstant;
import com.yida.entity.Employee;
import com.yida.mapper.EmployeeMapper;
import com.yida.properties.JwtProperties;
import com.yida.security.TokenService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtTokenAdminInterceptor implements HandlerInterceptor {
    private final JwtProperties properties;
    private final TokenService tokenService;
    private final EmployeeMapper employeeMapper;

    public JwtTokenAdminInterceptor(JwtProperties properties, TokenService tokenService,
                                    EmployeeMapper employeeMapper) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) { return true; }
        try {
            com.yida.context.AuthenticatedPrincipal principal =
                    tokenService.authenticateAdmin(request.getHeader(properties.getAdminTokenName()));
            Employee employee = employeeMapper.getById(principal.getId());
            if (employee == null || !StatusConstant.ENABLE.equals(employee.getStatus())) {
                throw new IllegalArgumentException("employee disabled");
            }
            AuthenticationContext.set(principal);
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
