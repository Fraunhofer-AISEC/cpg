package compiling.enums;

import compiling.enums.EnumsImport;
import static compiling.enums.EnumsImport.CONSTANT;
import static compiling.enums.EnumsImport.getConstant;

public enum Enums {

    VALUE_ONE(1),
    VALUE_TWO(2);

    private int value;
    public static final String NAME = "Enums";

    Enums(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static void main(String[] args) {
        Enums e1 = Enums.VALUE_ONE;
        Enums e2 = Enums.VALUE_TWO;

        System.out.println(e1.value + e2.value);

        int c1 = getConstant();
        int c2 = CONSTANT;

        System.out.println(c1 + c2);
    }
}