package compiling;

import compiling.subpackage.*;
/**
 * Tests for different ways of providing argument to method calls.
 *
 * Resolution is expected to cope with all of them.
 */
public class CT {

	public static final int CONSTANT = 3;

	enum Color
	{
		RED, GREEN, BLUE;
	}

	public static void main(String[] args){
		CT c = new CT();

		// Enum as argument
		c.foo(Color.RED);

		// Field as argument
		c.bar(CT.CONSTANT);

		// Constant as argument
		c.bar(3);

		// Expression as argument
		c.bar(2+1);

		// MethodCallExpression as argument
		c.bar(c.red());

		// Intentionally create a variable that has the same name as an imported package,
		// because we had an issue with confusion
		CT subpackage = new CT();

		// This is just a regular member call
		subpackage.bar(1);
	}

	private void bar(int constant) {
	}

	private void foo(Color red) {
	}

	private int red() {
		return 0;
	}
}
