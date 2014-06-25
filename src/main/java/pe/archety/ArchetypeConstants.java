package pe.archety;

import com.google.common.base.Charsets;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.util.encoders.Base64;

public class ArchetypeConstants {
    public static final String ACTION = "action";
    public static final String DATA = "data";


    public static String calculateHash(String input) {
        SHA3Digest digest = new SHA3Digest(512);
        byte[] inputAsBytes = input.getBytes(Charsets.UTF_8);
        byte[] retValue = new byte[digest.getDigestSize()];
        digest.update(inputAsBytes, 0, inputAsBytes.length);
        digest.doFinal(retValue, 0);
        return Base64.toBase64String(retValue);
    }

}
