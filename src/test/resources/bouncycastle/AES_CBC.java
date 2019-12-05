package bouncycastle;

import bouncycastle.Helper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

public class AES_CBC {
  private static final int IV_LEN = 16;
  private static String TRANSFORMATION = "AES/CBC/PKCS5Padding";
  private static String PROVIDER = "BC";

  public byte[] encrypt1(byte[] key, byte[] cleartext) throws Exception{
    SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    Cipher pbeCipher = Cipher.getInstance(Helper.getMode(), PROVIDER);

    pbeCipher.init(Cipher.ENCRYPT_MODE, keySpec);
    byte[] enc = pbeCipher.doFinal(cleartext);

    byte[] ret = new byte[IV_LEN + enc.length];
    // TODO unclear if getIV is unique/securely generated
    System.arraycopy(pbeCipher.getIV(), 0, ret, 0, IV_LEN);
    System.arraycopy(enc, 0, ret, IV_LEN, enc.length);
    return ret;
  }

  public byte[] encrypt2(byte[] key, byte[] cleartext) throws Exception{
    SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    SecureRandom sr = new SecureRandom();
    byte[] ivData = new byte[IV_LEN];
    sr.nextBytes(ivData);
    IvParameterSpec ivParamSpec = new IvParameterSpec(ivData);
    Cipher pbeCipher = Cipher.getInstance(TRANSFORMATION, PROVIDER);

    pbeCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParamSpec);
    byte[] enc = pbeCipher.doFinal(cleartext);

    byte[] ret = new byte[IV_LEN + enc.length];
    //
    System.arraycopy(pbeCipher.getIV(), 0, ret, 0, IV_LEN);
    System.arraycopy(enc, 0, ret, IV_LEN, enc.length);

    return ret;
  }

  public byte[] decrypt(byte[] key, byte[] ciphertext) throws Exception{

    byte[] ivData = Arrays.copyOfRange(ciphertext, 0, IV_LEN);
    byte[] encData = Arrays.copyOfRange(ciphertext, IV_LEN, ciphertext.length);

    SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    IvParameterSpec ivParamSpec = new IvParameterSpec(ivData);
    Cipher pbeCipher = Cipher.getInstance(TRANSFORMATION, PROVIDER);

    pbeCipher.init(Cipher.DECRYPT_MODE, keySpec, ivParamSpec);

    return pbeCipher.doFinal(encData);
  }

  public static void main(String... args) throws Exception {
    Security.addProvider(new BouncyCastleProvider());

    String test = "secret text";
    AES_CBC des = new AES_CBC();
    byte[] key = Helper.randomBytes(256/8);

    {
      System.out.print("Input: ");
      Helper.printHex(test.getBytes());
      byte[] enc = des.encrypt1(key, test.getBytes());
      System.out.print("Encrypted: ");
      Helper.printHex(enc);
      byte[] dec = des.decrypt(key, enc);
      System.out.println("Decrypted: " + new String(dec));
    }
    {
      System.out.print("Input: ");
      Helper.printHex(test.getBytes());
      byte[] enc = des.encrypt2(key, test.getBytes());
      System.out.print("Encrypted: ");
      Helper.printHex(enc);
      byte[] dec = des.decrypt(key, enc);
      System.out.println("Decrypted: " + new String(dec));
    }
  }
}