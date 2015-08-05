package dan.dit.whatsthat.testsubject;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;

import dan.dit.whatsthat.util.image.ExternalStorage;

/**
 * Non high security cryptography helper class to asymmetrically
 * encrypt and decrypt strings. Offers a public key to encrypt messages
 * that can only be read by the developers or people that can break 1024 bit codes.
 * Created by daniel on 04.08.15.
 */
public class SimpleCrypto {

    private static final String PUBLIC_KEY_FILE = "dev_key_public.txt";
    private static final String PRIVATE_KEY_FILE = "dev_key_private.txt";
    private static final String DEVELOPER_PUBLIC_KEY_ENCODED = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCYT5vZ5Wof4Hh3hgNjVVAd13bUrPnqyiHXqCRT\n" +
            "zvEUVPAnokpr+Uw2Ft2YFPSw9J4USHrqWqVdumiABJameWx6MuvNPUU4yNd/xWd3UYpCMwJHaJm3\n" +
            "WP481XbUk5qU5JZWAPPZGHYBEm5FXA1kC5L8jfT41+F1ca2R0dA7S3GXEQIDAQAB";
    private static Key DEVELOPER_PUBLIC_KEY;

    private SimpleCrypto() {}

    public static String encodeToString(byte[] data) {
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    public static byte[] encodedToBytes(String encoded) {
        return Base64.decode(encoded, Base64.DEFAULT);
    }

    public static void saveKeyPair(KeyPair pair) {
        DEVELOPER_PUBLIC_KEY = pair.getPublic();
        FileWriter writer = null;
        try {
            writer = new FileWriter(new File(ExternalStorage.getExternalStoragePathIfMounted(null) + "/" + PUBLIC_KEY_FILE));
            writer.write(encodeToString(pair.getPublic().getEncoded()));
        } catch (IOException e) {
            Log.e("HomeStuff", "Error during writing public key." + e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    Log.e("HomeStuff", "Error closing file writer while writing public key. " + ioe);
                }
            }
        }
        try {
            writer = new FileWriter(new File(ExternalStorage.getExternalStoragePathIfMounted(null) + "/" + PRIVATE_KEY_FILE));
            writer.write(encodeToString(pair.getPrivate().getEncoded()));
        } catch (IOException e) {
            Log.e("HomeStuff", "Error during writing private key." + e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    Log.e("HomeStuff", "Error closing file writer while writing private key. " + ioe);
                }
            }
        }
    }

    public static synchronized Key getDeveloperPublicKey() {
        if (DEVELOPER_PUBLIC_KEY != null) {
            return DEVELOPER_PUBLIC_KEY;
        }
        String keyEncoded = DEVELOPER_PUBLIC_KEY_ENCODED;
        if (TextUtils.isEmpty(keyEncoded)) {
            // attempt to read key from file
            StringBuilder builder = new StringBuilder();
            FileReader reader = null;
            try {
                reader = new FileReader(new File(ExternalStorage.getExternalStoragePathIfMounted(null) + "/" + PUBLIC_KEY_FILE));
                char[] buffer = new char[64];
                int read;
                while ((read = reader.read(buffer)) > 0) {
                    builder.append(buffer, 0, read);
                }
            } catch (Exception e) {
                Log.e("HomeStuff", "Error trying to read public key file: " + e);
                return null;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ioe) {
                        Log.e("HomeStuff", "Error closing file reader when reading public key file.");
                    }
                }
            }
            keyEncoded = builder.toString();
        }
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(SimpleCrypto.encodedToBytes(keyEncoded));
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            DEVELOPER_PUBLIC_KEY = keyFactory.generatePublic(x509KeySpec);
            return DEVELOPER_PUBLIC_KEY;
        } catch (Exception e) {
            Log.e("HomeStuff", "Error with keyfactory when decoding public key"  + e);
            return null;
        }
    }

    public static Key getDeveloperPrivateKey() {
        StringBuilder builder = new StringBuilder();
        FileReader reader = null;
        try {
            reader = new FileReader(new File(ExternalStorage.getExternalStoragePathIfMounted(null) + "/" + PRIVATE_KEY_FILE));
            char[] buffer = new char[64];
            int read;
            while ((read = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, read);
            }
        } catch (Exception e) {
            Log.e("HomeStuff", "Error trying to read private key file: " + e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    Log.e("HomeStuff", "Error closing file reader when reading private key file.");
                }
            }
        }
        byte[] data = SimpleCrypto.encodedToBytes(builder.toString());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(data);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            Key privateKey = keyFactory.generatePrivate(keySpec);
            Arrays.fill(data, (byte) 0);
            return privateKey;
        } catch (Exception e) {
            Log.e("HomeStuff", "Error with keyfactory when decoding private key"  + e);
            return null;
        }
    }

    public static KeyPair generateKeyPair() {
        // Generate key pair for 1024-bit RSA encryption and decryption
        try {
            SecureRandom random = new SecureRandom();
            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(spec, random);
            return generator.genKeyPair();
        } catch (Exception e) {
            Log.e("HomeStuff", "RSA key pair error" + e);
            return null;
        }
    }

    public static String encrypt(Key publicKey, String data) {
        if (publicKey == null || TextUtils.isEmpty(data)) {
            return null;
        }
        // Encode the original data with RSA public key
        byte[] encodedBytes;
        try {
            Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            c.init(Cipher.ENCRYPT_MODE, publicKey);
            encodedBytes = c.doFinal(data.getBytes());
            return encodeToString(encodedBytes);
        } catch (Exception e) {
            Log.e("HomeStuff", "RSA encryption error");
            return null;
        }
    }

    public static String decrypt(Key privateKey, String encrypted) {
        if (privateKey == null || TextUtils.isEmpty(encrypted)) {
            return null;
        }
        // Decode the encoded data with RSA private key
        byte[] decodedBytes;
        try {
            Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            c.init(Cipher.DECRYPT_MODE, privateKey);
            decodedBytes = c.doFinal(encodedToBytes(encrypted));
            return new String(decodedBytes);
        } catch (Exception e) {
            Log.e("HomeStuff", "RSA decryption error");
            return null;
        }
    }

}
