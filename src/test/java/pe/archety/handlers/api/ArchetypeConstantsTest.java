package pe.archety.handlers.api;

import org.junit.Test;
import pe.archety.ArchetypeConstants;
import static org.junit.Assert.assertEquals;


public class ArchetypeConstantsTest {

    @Test
    public void shouldEncryptAndDecryptUsingEmails() throws Exception {
        String plainText = "encrypt@me.com";
        String key = "maxdemarzi@gmail.com";

        String cypherText = ArchetypeConstants.encrypt( plainText, key );
        String decipheredText = ArchetypeConstants.decrypt( cypherText, key );

        assertEquals( plainText, decipheredText );
    }
}
