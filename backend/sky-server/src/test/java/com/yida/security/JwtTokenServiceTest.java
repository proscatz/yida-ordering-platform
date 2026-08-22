package com.yida.security;
import com.yida.context.AuthenticatedPrincipal;
import com.yida.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class JwtTokenServiceTest {
    private StringRedisTemplate redis;private ValueOperations<String,String> values;private JwtTokenService service;
    @SuppressWarnings("unchecked") @BeforeEach void setUp(){
        JwtProperties p=new JwtProperties();p.setAdminSecretKey("admin-secret-key-for-tests");p.setAdminTtl(60000);p.setUserSecretKey("user-secret-key-for-tests");p.setUserTtl(60000);
        redis=mock(StringRedisTemplate.class);values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);service=new JwtTokenService(p,redis);
    }
    @Test void issuedTokenCarriesTypeAndIdentity(){String token=service.issueUser(9L);AuthenticatedPrincipal p=service.authenticateUser(token);assertEquals(9L,p.getId());assertEquals("USER",p.getType());assertNotNull(p.getTokenId());}
    @Test void adminTokenCannotBeUsedAsUserToken(){String token=service.issueAdmin(1L);assertThrows(Exception.class,()->service.authenticateUser(token));}
    @Test void logoutRevokesTokenUntilExpiration(){String token=service.issueUser(9L);service.revokeUser(token);verify(values).set(startsWith("auth:revoked:"),eq("1"),longThat(v->v>0),eq(java.util.concurrent.TimeUnit.MILLISECONDS));}
    @Test void revokedTokenIsRejected(){String token=service.issueUser(9L);when(redis.hasKey(anyString())).thenReturn(true);assertThrows(IllegalArgumentException.class,()->service.authenticateUser(token));}
}