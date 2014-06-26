package pe.archety;


import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class ArchetypeConstants {
    public static final String ACTION = "action";
    public static final String DATA = "data";

    private static final int ITERATIONS = 1000;
    private static final int KEY_LENGTH = 256; // bits

    public static String calculateHash(String input) throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] passwordChars = input.toCharArray();
        String salt = new StringBuffer(input).reverse().toString();
        byte[] saltBytes = salt.getBytes();

        PBEKeySpec spec = new PBEKeySpec(
                passwordChars,
                saltBytes,
                ITERATIONS,
                KEY_LENGTH
        );
        SecretKeyFactory key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hashedPassword = key.generateSecret(spec).getEncoded();
        return String.format("%x", new BigInteger(hashedPassword));
    }

}
