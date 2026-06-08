package com.galaxy.apihub.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码工具 — SHA-256 + 随机盐
 */
public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LENGTH = 32;

    /**
     * 生成随机盐
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 哈希密码
     */
    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码哈希失败", e);
        }
    }

    /**
     * 验证密码
     */
    public static boolean verify(String password, String salt, String storedHash) {
        return hash(password, salt).equals(storedHash);
    }
}
