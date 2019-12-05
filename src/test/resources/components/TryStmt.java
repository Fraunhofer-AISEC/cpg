import java.text.ParseException;

class TryStmt {

	public static void main(String[] args){
		try {
			System.out.println(Integer.parseInt(args[0]));
			throw new RuntimeException("Reached");
		}catch(NumberFormatException p){
			System.out.println("NumberFormatException");
		}catch(RuntimeException r){
			System.out.println("RuntimeException");
		}finally{System.out.println("Finished");}
	}
}