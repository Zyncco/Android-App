package co.zync.zync.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ZyncCrypto {
    private static byte[] iv;

    static {
        try {
            iv = "0000000000000000".getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignored) {}
    }

    public static byte[] encrypt(byte[] data, String encryptionKey) throws Exception {
        byte[] pass = hash(encryptionKey.getBytes("UTF-8"));
        SecretKeySpec key = new SecretKeySpec(pass, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, String encryptionKey) throws Exception {
        SecretKey key = new SecretKeySpec(hash(encryptionKey.getBytes("UTF-8")), "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(data);
    }

    public static byte[] hash(byte[] data) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            return new byte[0];
        }

        return md.digest(data);
    }
}
