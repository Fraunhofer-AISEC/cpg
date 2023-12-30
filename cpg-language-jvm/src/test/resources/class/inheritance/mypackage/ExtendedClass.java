package mypackage;

public class ExtendedClass extends BaseClass implements MyInterface {

    public void setMyProperty(int myProperty) {
        informSomebody(this.myProperty, myProperty);
        super.setMyProperty(myProperty);
    }
    private void informSomebody(int oldValue, int newValue) {
        System.out.println("We changed the value from " + oldValue + " to " + newValue);
    }

    @Override
    public void doSomething() {

    }

}