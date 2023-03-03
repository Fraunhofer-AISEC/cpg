class CastConvert {

	private class A{}

	private class B extends A{}

	private class C extends A{}

	public static void main(String[] args){
		A a;
		if(args > 0) 
		a = new B();
		else
		a = new C();
		int i = 10.1;
		double j = 3;
	}
}