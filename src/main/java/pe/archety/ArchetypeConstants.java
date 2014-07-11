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
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
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

    public static String encrypt(String value, String keyString)
            throws Exception {
        BlowfishEngine engine = new BlowfishEngine();
        PaddedBufferedBlockCipher cipher =
                new PaddedBufferedBlockCipher(engine);
        KeyParameter key = new KeyParameter(keyString.getBytes());
        cipher.init(true, key);
        byte in[] = value.getBytes(Charsets.UTF_8);
        byte out[] = new byte[cipher.getOutputSize(in.length)];
        int len1 = cipher.processBytes(in, 0, in.length, out, 0);
        try {
            cipher.doFinal(out, len1);
        } catch (CryptoException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
        return new String(Base64.encode(out));
    }

    public static String decrypt(String value, String keyString)
            throws Exception {
        BlowfishEngine engine = new BlowfishEngine();
        PaddedBufferedBlockCipher cipher =
                new PaddedBufferedBlockCipher(engine);
        StringBuilder result = new StringBuilder();
        KeyParameter key = new KeyParameter(keyString.getBytes(Charsets.UTF_8));
        cipher.init(false, key);
        byte out[] = Base64.decode(value);
        byte out2[] = new byte[cipher.getOutputSize(out.length)];
        int len2 = cipher.processBytes(out, 0, out.length, out2, 0);
        cipher.doFinal(out2, len2);
        String s2 = new String(out2);
        for (int i = 0; i < s2.length(); i++) {
            char c = s2.charAt(i);
            if (c != 0) {
                result.append(c);
            }
        }

        return result.toString();
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
