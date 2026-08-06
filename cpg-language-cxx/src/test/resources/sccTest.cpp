#include<stdio.h>

int nested() {
  if ( 1 == 2 ) {
    printf("huh?");
  } else {
    for ( int i = 0; i < 5; i++) {
      for (int j = 0; j < 10; j++) {
        for (int k = 0; k < 15; k++) {
          printf("didoo");
        }
      }
      printf("woop");
    }
  }
  printf("test");
  if ( 2 == 3) 
    printf("bla");
  else
    printf("blubb");
  return 2;
}

int breakloop() {
  for (int i = 0; i< 5; i++) {
    printf("a");
    if ( 1 == 2) break;
  }
  printf("done");
}

int gotoloop() {
  for (int i = 0; i< 5; i++) {
    loop:
    printf("a");
    if ( 1 == 2) goto exit;
  }
  exit:
  printf("done");
  if ( 2 == 3 ) goto loop;
}
