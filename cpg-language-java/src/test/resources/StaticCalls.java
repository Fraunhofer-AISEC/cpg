import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import com.example.Util;

public class StaticCalls {
    enum Color {
        RED,
        GREEN
    }

    static int helper() {
        return 1;
    }

    void test(String s) {
        // a static call to the JDK, which JavaParser can resolve
        List<String> list = Arrays.asList(s);
        // a static call to a class that we only know from its import
        Util.doSomething(s);
        // a static call to our own class
        StaticCalls.helper();
        // a regular call on a variable
        list.size();

        // a static field of a class that we only know from its import
        int constant = Util.CONSTANT;
        // a static field of a class in java.lang (which is imported implicitly)
        int max = Integer.MAX_VALUE;
        // a method reference to a static method of a class in java.lang
        Function<String, Boolean> parse = Boolean::parseBoolean;
        // a call on an enum constant
        String color = Color.RED.name();
    }
}
