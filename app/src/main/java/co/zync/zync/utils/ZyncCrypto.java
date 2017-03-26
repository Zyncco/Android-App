package co.zync.zync.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
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
