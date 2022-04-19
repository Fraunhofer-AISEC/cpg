public class Issue285 {
    void doSomething() {
        System.out.println("test");

        var request = target.path("/").get();

        Issue285.doSomethingStatic();
    }

    static void doSomethingStatic() {

    }
}