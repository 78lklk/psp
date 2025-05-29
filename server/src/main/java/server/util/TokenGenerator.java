package server.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Простой генератор токенов для аутентификации.
 */
public class TokenGenerator {
    private static final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Генерирует случайный токен для аутентификации.
     * @return строка токена
     */
    public static String generateToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
} 