package de.fraunhofer.aisec.cpgtest;

public class ForLoop {

  public static void main(String[] args) {
  	int n, o = 1;
  	o = 9;
  	System.out.println();
    for(n = 0, o = 1; o < 3; o++){
    	;
    	n = 2*o;
    }
    System.out.println(); 
    for(int i = 0, j = 1;  i < 2; i++, j++);
    System.out.println();
  }
}
