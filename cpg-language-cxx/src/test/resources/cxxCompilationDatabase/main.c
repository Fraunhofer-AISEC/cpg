#include <stdio.h>
#include <string.h>

#include "header_1.h"
#include "header_2.h"
#include <sys_header.h>

int main() {
#ifdef DEBUG
	printf("%s\n", INFO);
#endif
	func1();
	func2();
	sys_func();
	FOO;
	BAR;
	return 0;
}
