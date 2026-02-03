package mypackage;

public class Main {

    public static void main(String[] args) {
        var adder = new Adder();
        var sum = adder.add(1, 2);

        System.out.println(sum);
    }

}