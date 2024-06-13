package mypackage;

public class BaseClass {

    public int getMyProperty() {
        return myProperty;
    }

    public void setMyProperty(int myProperty) {
        this.myProperty = myProperty;
    }

    protected int myProperty = 5;
}