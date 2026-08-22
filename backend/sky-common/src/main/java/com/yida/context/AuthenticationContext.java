package com.yida.context;

public final class AuthenticationContext {
    private static final ThreadLocal<AuthenticatedPrincipal> CURRENT = new ThreadLocal<>();

    private AuthenticationContext() { }

    public static void set(AuthenticatedPrincipal principal) { CURRENT.set(principal); }
    public static AuthenticatedPrincipal get() { return CURRENT.get(); }
    public static Long getCurrentId() { return CURRENT.get() == null ? null : CURRENT.get().getId(); }
    public static void clear() { CURRENT.remove(); }
}