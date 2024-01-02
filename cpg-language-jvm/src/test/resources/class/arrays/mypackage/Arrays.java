package mypackage;

public class Arrays {

    public Element[] create() {
        var arrays = new Element[2];
        arrays[0] = new Element();
        arrays[1] = arrays[0];

        return arrays;
    }

}