public class Victim {
  int array1_size = 16;
  byte[] array1 = new byte[16];
  byte[] array2 = new byte[256 * 512];
  byte temp = 0;

  static void _mm_lfence() {}

  void victim_function(int x) {
    if (x < array1_size) {
      temp &= array2[array1[x] * 512];
    }
  }

  void victim_function_split(int x) {
    if (x < array1_size) {
      byte v = array1[x];
      temp &= array2[v * 512];
    }
  }

  void victim_function_fenced(int x) {
    if (x < array1_size) {
      _mm_lfence();
      temp &= array2[array1[x] * 512];
    }
  }

  byte safe_function(int x) {
    if (x < array1_size) {
      return array1[x];
    }
    return 0;
  }
}
