public class Dataflow {
    public String toString() {
        return "Dataflow: attr=" + attr;
    }

    public String test() { return "abcd"; }

    public int print(String s) {
        System.out.println(s);
    }


    public static void main(String[] args) {
        Dataflow sc = new Dataflow();
        String s = sc.toString();
        sc.print(s);

        sc.print(sc.test());
    }
}