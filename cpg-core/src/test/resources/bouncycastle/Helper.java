package bouncycastle;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

public class Helper {

  public static String encode(byte[] b) {
    String ret = new String(Base64.getEncoder().encode(b));
    if (ret.length() > 64) {
      return ret.substring(0, 64) + "[...]";
    } else {
      return ret;
    }
  }

  public static String getMode() {
    return "AES/CBC/PKCS5Padding";
  }

  public static String randomString(int len) {
    byte[] array = new byte[len];
    new SecureRandom().nextBytes(array);
    return new String(array, Charset.forName("UTF-8"));
  }
  public static String randomPrintableString(int len) {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    StringBuilder sb = new StringBuilder();
    Random random = new Random();
    for (int i = 0; i < len; i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }
    return sb.toString();
  }

  public static byte[] randomBytes(int len) {
    byte[] array = new byte[len];
    new SecureRandom().nextBytes(array);
    return array;
  }

  public static void printHex(byte[] b) {
    if (b == null) {
      System.out.println("NULL");
      return;
    }
    for (int j = 0; j < b.length; j++) {
      System.out.format("%02X ", b[j]);
    }
    System.out.println();
  }

}
