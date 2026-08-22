package com.yida.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component
public class PasswordHasher {
    private static final Pattern LEGACY_MD5 = Pattern.compile("^[a-fA-F0-9]{32}$");
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matchesBcrypt(String rawPassword, String encodedPassword) {
        return encodedPassword != null && encodedPassword.startsWith("$2")
                && encoder.matches(rawPassword, encodedPassword);
    }

    public boolean matchesLegacyMd5(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || !LEGACY_MD5.matcher(encodedPassword).matches()) {
            return false;
        }
        String digest = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
        return digest.equalsIgnoreCase(encodedPassword);
    }
}