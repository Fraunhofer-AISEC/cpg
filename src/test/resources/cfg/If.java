package cfg;

public class If {

	public static void main(String[] args) {
		int i = 0; // To if
		System.out.println();
		if(i < 1) 
			i = 1;
		System.out.println(); // Reached by end of condition and else - to if
		if(i >= 0) // From condition to then and else branch
			i = 0;
		else // Comes from condition end
			i = 1;
		System.out.println(); // Reached by end of then and else
	}

}