package cfg;

public class Loops {

    public static void main(String[] args) {
        boolean containsArg = args.length > 0 && args[0].equals("SomeArg");
        boolean empty = args == null || args.length == 0;
    }
}