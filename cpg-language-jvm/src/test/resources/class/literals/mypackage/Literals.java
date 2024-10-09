package mypackage;

import java.util.function.Supplier;

public class Literals {

    void haveFunWithLiterals() {
        float f = 2;
        double d = 4d;
        String str = "mystring";
        short s = 10;
        int i = 2000;
        long l = 2000L;
        boolean b = false;
        Literals obj;
        if(Math.random() == 10) {
            obj = null;
        } else {
            obj = this;
        }

        Integer i2 = 1000;
        Long l2 = 1000L;

        Class<?> clazz = Literals.class;
        test(this::mySupplier);
    }

    void test(Supplier<Integer> s) {
        s.get();
    }

    Integer mySupplier() {
        return 1;
    }

}