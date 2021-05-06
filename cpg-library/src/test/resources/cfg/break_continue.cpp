#include <stdio.h>

int main(void){
    int i = 10;
	printf("\n");
    while (i > 0) {
      if (i < 8) continue;
      else if (i > 9) break;
      i--;
    }
	printf("\n");
    do {
      if (i > 9) break;
      if (i < 5) {
        i += 2;
        continue;
      }
      i++;
    } while (i < 10);
	printf("\n");
}