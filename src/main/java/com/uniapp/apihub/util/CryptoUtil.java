package com.uniapp.apihub.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-CBC 加密工具 — 用于保护下游系统认证凭据
 *
 * 密钥从 application.yml 的 crypto.secret-key 读取
 * IV 随机生成并前缀存储于密文中（IV(16字节)+密文）
 */
@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    private final byte[] secretKeyBytes;

    public CryptoUtil(@Value("${crypto.secret-key}") String secretKey) {
        // 校验密钥长度
        if (secretKey == null || secretKey.length() < 16) {
            throw new IllegalStateException("crypto.secret-key 必须至少16个字符，请在 application.yml 中配置");
        }
        // 截取或填充至32字节（AES-256）
        byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
        this.secretKeyBytes = new byte[32];
        System.arraycopy(raw, 0, this.secretKeyBytes, 0, Math.min(raw.length, 32));
    }

    /**
     * 加密：明文 → Base64(IV + 密文)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 密文 → Base64
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * 解密：Base64(IV + 密文) → 明文
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            if (combined.length < IV_LENGTH) {
                return cipherText; // 太短，可能是未加密的旧数据
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败可能是旧数据未加密，直接返回原文
            return cipherText;
        }
    }

    /**
     * 判断是否已加密（格式检测：Base64 且长度 > 100）
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) return false;
        try {
            byte[] data = Base64.getDecoder().decode(text);
            return data.length > IV_LENGTH + 10;
        } catch (Exception e) {
            return false;
        }
    }
}
