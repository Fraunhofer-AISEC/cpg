#include <stdio.h>

int main(void){
		int i = 10;
		printf("\n");
		{
			printf("Compound");
			printf("Compound");
		}
		while(i>0)
		{
			i--;
		}
		printf("\n");
		do
		{
			i++;
		}
		while(i < 10);
		printf("\n");
}