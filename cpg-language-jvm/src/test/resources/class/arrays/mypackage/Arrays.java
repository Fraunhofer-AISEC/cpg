package mypackage;

public class Arrays {

    public Element[] create() {
        var arrays = new Element[2];
        arrays[0] = new Element();
        arrays[1] = arrays[0];

        int len = arrays.length;

        return arrays;
    }

    public Element[][] createMulti() {
        var multi = new Element[2][10];

        return multi;
    }

}