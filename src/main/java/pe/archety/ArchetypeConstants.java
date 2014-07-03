package pe.archety;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.google.common.base.Charsets;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.util.encoders.Base64;

public class ArchetypeConstants {
    public static final String ACTION = "action";
    public static final String DATA = "data";
    public static final String URLPREFIX = "http://en.wikipedia.org/wiki/";

    public static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();
    public static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    public static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    public static final BatchWriterService BATCH_WRITER_SERVICE = BatchWriterService.INSTANCE;

    public static String calculateHash(String input) {
        SHA3Digest digest = new SHA3Digest(512);
        byte[] inputAsBytes = input.getBytes(Charsets.UTF_8);
        byte[] retValue = new byte[digest.getDigestSize()];
        digest.update(inputAsBytes, 0, inputAsBytes.length);
        digest.doFinal(retValue, 0);
        return Base64.toBase64String(retValue);
    }

    private static final int ITERATIONS = 1000;
    private static final int KEY_LENGTH = 256; // bits

    public static String calculateHashVerySlowly(String input) throws NoSuchAlgorithmException, InvalidKeySpecException {
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
