package com.yida.websocket;
import com.yida.context.AuthenticatedPrincipal;
import com.yida.entity.Employee;
import com.yida.mapper.EmployeeMapper;
import com.yida.security.TokenService;
import org.junit.jupiter.api.Test;
import javax.websocket.Session;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
class WebSocketServerSecurityTest {
    @Test void unauthenticatedHandshakeIsClosed() throws Exception {TokenService tokens=mock(TokenService.class);EmployeeMapper employees=mock(EmployeeMapper.class);Session session=mock(Session.class);when(session.getUserProperties()).thenReturn(new HashMap<>());when(session.isOpen()).thenReturn(true);new WebSocketServer(tokens,employees).onOpen(session,"1");verify(session).close(any());}
    @Test void pathIdentityMustMatchAuthenticatedEmployee() throws Exception {TokenService tokens=mock(TokenService.class);EmployeeMapper employees=mock(EmployeeMapper.class);Session session=mock(Session.class);Map<String,Object> props=new HashMap<>();props.put(WebSocketHandshakeConfigurator.TOKEN_PROPERTY,"token");when(session.getUserProperties()).thenReturn(props);when(session.isOpen()).thenReturn(true);when(tokens.authenticateAdmin("token")).thenReturn(new AuthenticatedPrincipal(2L,"ADMIN","jti"));new WebSocketServer(tokens,employees).onOpen(session,"1");verify(session).close(any());}
    @Test void authenticatedMatchingEmployeeIsAccepted() throws Exception {TokenService tokens=mock(TokenService.class);EmployeeMapper employees=mock(EmployeeMapper.class);Session session=mock(Session.class);Map<String,Object> props=new HashMap<>();props.put(WebSocketHandshakeConfigurator.TOKEN_PROPERTY,"token");when(session.getUserProperties()).thenReturn(props);when(session.isOpen()).thenReturn(true);when(tokens.authenticateAdmin("token")).thenReturn(new AuthenticatedPrincipal(1L,"ADMIN","jti"));when(employees.getById(1L)).thenReturn(Employee.builder().id(1L).status(1).build());new WebSocketServer(tokens,employees).onOpen(session,"1");verify(session,never()).close(any());}
}
