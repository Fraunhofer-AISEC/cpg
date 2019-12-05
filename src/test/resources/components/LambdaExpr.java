class Test {
	interface A {void a(String s, String t);}

	interface B {int b(int c);}

	public static void main(String[] args){
		A a = (s,t) -> System.out.println(s + " "+ t);
		B b = (c)->{
			int ret = c*2;
			return ret + 3;
		};
	}
}