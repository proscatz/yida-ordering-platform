package com.yida.security;

import com.yida.context.AuthenticatedPrincipal;

public interface TokenService {
    String issueAdmin(Long employeeId);
    String issueUser(Long userId);
    AuthenticatedPrincipal authenticateAdmin(String token);
    AuthenticatedPrincipal authenticateUser(String token);
    void revokeAdmin(String token);
    void revokeUser(String token);
}