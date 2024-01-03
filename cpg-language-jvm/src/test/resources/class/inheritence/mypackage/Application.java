package mypackage;

public class Application {

    public void init() {
        var extended = new ExtendedClass();
        extended.setMyProperty(10);

        BaseClass base;
        if(Math.random() == 1.0) {
            base = (BaseClass) extended;
        } else {
            base = new AnotherExtendedClass();
        }
        base.setMyProperty(10);
    }

}