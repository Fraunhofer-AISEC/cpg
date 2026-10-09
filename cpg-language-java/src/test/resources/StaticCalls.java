import java.util.Arrays;
import java.util.List;
import com.example.Util;

public class StaticCalls {
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
    }
}
