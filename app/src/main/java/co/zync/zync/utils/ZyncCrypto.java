package co.zync.zync.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ZyncCrypto {
    public static byte[] generateSecureSalt() {
        SecureRandom rng = new SecureRandom();
        return rng.generateSeed(25);
    }

    public static byte[] generateSecureIv() {
        SecureRandom rng = new SecureRandom();
        return rng.generateSeed(16);
    }

    public static String hashSha(byte[] data) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            return "unsupported";
        }

        data = md.digest(data);
        StringBuilder sb = new StringBuilder();

        for (byte b : data) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    public static Cipher getCipher(int mode, String encryptionKey, byte[] salt, byte[] iv) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                encryptionKey.toCharArray(),
                salt,
                1000,
                256
        );
        SecretKey tmp = SecretKeyFactory.getInstance("PBKDF2withHmacSHA1").generateSecret(spec);
        SecretKey key = new SecretKeySpec(tmp.getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(mode, key, new GCMParameterSpec(128, iv));
        return cipher;
    }

    public static byte[] encrypt(byte[] data, String encryptionKey, byte[] salt, byte[] iv) throws Exception {
        return getCipher(Cipher.ENCRYPT_MODE, encryptionKey, salt, iv).doFinal(data);
    }

    public static byte[] decrypt(byte[] data, String encryptionKey, byte[] salt, byte[] iv) throws Exception {
        return getCipher(Cipher.DECRYPT_MODE, encryptionKey, salt, iv).doFinal(data);
    }
}
