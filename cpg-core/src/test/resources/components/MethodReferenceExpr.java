import java.util.Arrays;

class MethodReferenceExpr {

	public static String something(String s){return s + s;}

	public static void main(String[] args){
		Arrays.stream(args).map(MethodReferenceExpr::something).forEach(s -> System.out.println(s));
	}
}