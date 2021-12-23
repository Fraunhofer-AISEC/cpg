#include <iostream>

int main(void){
	label:
	goto label;
}

void func(int a){
	goto label;
	switch(a){
		label: case 0:
		default:
		goto label;
	}
}

