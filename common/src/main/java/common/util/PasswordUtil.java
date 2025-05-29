package common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Утилиты для работы с паролями
 */
public class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * Генерирует соль для хеширования
     * @return соль в Base64
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Хеширует пароль с солью
     * @param password пароль
     * @param salt соль
     * @return хеш пароля
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка хеширования пароля", e);
        }
    }
    
    /**
     * Проверяет соответствие пароля хешу
     * @param password пароль
     * @param salt соль
     * @param expectedHash ожидаемый хеш
     * @return true, если пароль соответствует хешу
     */
    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        String actualHash = hashPassword(password, salt);
        return actualHash.equals(expectedHash);
    }
} 