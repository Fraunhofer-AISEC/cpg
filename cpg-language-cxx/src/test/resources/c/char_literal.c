#include <stdio.h>

int main() {
  char a = 'a';
  char zero = '\0';
  char eight = '\10';
  char max_digits = '\377';
  int hex = '\xff';

  char newline = '\n';

  char multi = '\1\2'; // 258
  char multi2 = '\1234'; // 21300

  char invalid = '\90';
  char invalid2 = '\90\90';
}
