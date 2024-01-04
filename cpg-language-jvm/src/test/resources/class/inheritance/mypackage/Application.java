package mypackage;

public class Application {
    public Application() {
        var extended = new ExtendedClass();
        int old = extended.getMyProperty();
        extended.setMyProperty(10);
        doSomething(extended);

        BaseClass base;
        if(Math.random() == 1.0) {
            base = (BaseClass) extended;
        } else {
            base = new AnotherExtendedClass();
        }
        base.setMyProperty(10);
    }

    public void doSomething(MyInterface i) {
        i.doSomething();
    }

}