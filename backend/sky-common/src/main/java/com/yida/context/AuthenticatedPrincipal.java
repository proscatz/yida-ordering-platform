package com.yida.context;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticatedPrincipal {
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";
    public static final String LEGACY = "LEGACY";

    private Long id;
    private String type;
    private String tokenId;
}