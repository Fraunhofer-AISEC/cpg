#include <iostream>

int main(void){
	int i = 5;
	printf("\n");
	switch(i){
		case 0:
		case 1:
		i = 10;
		break;
		case 2:
		i = 20;
		case 3:
		i *= 2;
		break;
		default:
		i = 4;
	}
	printf("\n");
}

void whileswitch(int i){
	printf("\n");
	while(i < 10){
		switch(i){
			case 0:
			i += 2;
			case 9:
			break;
			default:
			i++;
		}
		printf("\n");
	}
	printf("\n");
}

void switchwhile(int i){
	printf("\n");
	switch(i){
		case 0:
		i += 2;
		case 1:
		while(true){
			if(i > 5)
				break;
			i++;
		}
		printf("\n");
		default:
		i++;
	}
	printf("\n");
}