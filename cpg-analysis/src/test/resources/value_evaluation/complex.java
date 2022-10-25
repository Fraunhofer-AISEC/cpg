public class MainClass {
    public static void main(String[] args) {
        int i = 3;

        String s;

        if (i < 2) {
            s = "small";
        } else {
            s = "big";
        }

        s += "!";
        s = s + "?";
        System.out.println(s);
    }
}