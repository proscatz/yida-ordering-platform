package com.yida.interceptor;

import com.yida.constant.StatusConstant;
import com.yida.context.AuthenticatedPrincipal;
import com.yida.context.AuthenticationContext;
import com.yida.entity.Employee;
import com.yida.mapper.EmployeeMapper;
import com.yida.properties.JwtProperties;
import com.yida.security.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtTokenAdminInterceptorTest {
    private JwtProperties properties;
    private TokenService tokenService;
    private EmployeeMapper employeeMapper;
    private JwtTokenAdminInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HandlerMethod handler;

    @BeforeEach
    void setUp() {
        properties = mock(JwtProperties.class);
        tokenService = mock(TokenService.class);
        employeeMapper = mock(EmployeeMapper.class);
        interceptor = new JwtTokenAdminInterceptor(properties, tokenService, employeeMapper);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        handler = mock(HandlerMethod.class);
        when(properties.getAdminTokenName()).thenReturn("token");
        when(request.getHeader("token")).thenReturn("opaque-token");
        when(tokenService.authenticateAdmin("opaque-token"))
                .thenReturn(new AuthenticatedPrincipal(2L, AuthenticatedPrincipal.ADMIN, "token-id"));
    }

    @AfterEach
    void tearDown() {
        AuthenticationContext.clear();
    }

    @Test
    void enabledEmployeeTokenIsAccepted() {
        when(employeeMapper.getById(2L)).thenReturn(Employee.builder().id(2L).status(StatusConstant.ENABLE).build());
        assertTrue(interceptor.preHandle(request, response, handler));
        assertEquals(2L, AuthenticationContext.getCurrentId());
    }

    @Test
    void disabledEmployeeExistingTokenIsRejectedImmediately() {
        when(employeeMapper.getById(2L)).thenReturn(Employee.builder().id(2L).status(StatusConstant.DISABLE).build());
        assertFalse(interceptor.preHandle(request, response, handler));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertNull(AuthenticationContext.get());
    }

    @Test
    void deletedEmployeeExistingTokenIsRejectedImmediately() {
        when(employeeMapper.getById(2L)).thenReturn(null);
        assertFalse(interceptor.preHandle(request, response, handler));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
