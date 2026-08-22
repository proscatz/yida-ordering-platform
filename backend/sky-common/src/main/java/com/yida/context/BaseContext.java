package com.yida.context;

/** 兼容既有业务代码的入口，实际上下文统一由 AuthenticationContext 保存。 */
public final class BaseContext {
    private BaseContext() { }

    public static void setCurrentId(Long id) {
        AuthenticationContext.set(new AuthenticatedPrincipal(id, AuthenticatedPrincipal.LEGACY, null));
    }

    public static Long getCurrentId() { return AuthenticationContext.getCurrentId(); }
    public static void removeCurrentId() { AuthenticationContext.clear(); }
}