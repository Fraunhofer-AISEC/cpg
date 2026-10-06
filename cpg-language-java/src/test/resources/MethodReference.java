import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class MethodReference {
    static int staticMethod(String s) {
        return s.length();
    }

    int instanceMethod(String s) {
        return s.length();
    }

    void test(List<String> list) {
        // static method
        list.stream().map(MethodReference::staticMethod);
        // instance method with an unbound receiver
        list.stream().map(String::length);
        // instance method with a bound receiver
        list.forEach(this::instanceMethod);
        String s = "test";
        Supplier<Integer> bound = s::length;
        // constructors
        Supplier<ArrayList<String>> constructor = ArrayList::new;
        Function<Integer, int[]> array = int[]::new;
    }
}
