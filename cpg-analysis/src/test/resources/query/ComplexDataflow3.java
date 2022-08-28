public class Dataflow {
    static Logger logger = Logger.getLogger("DataflowLogger");

    public int a;

    public static void highlyCriticalOperation(String s) {
        System.out.println(s);
    }


    public static void main(String[] args) {
        Dataflow sc = new Dataflow();
        sc.a = 5;
        logger.log(Level.INFO, "put " + sc.a + " into highlyCriticalOperation()");
        sc.a = 3;
        Dataflow.highlyCriticalOperation(Integer.toString(sc.a));
    }
}