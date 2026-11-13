import java.util.Arrays;
import java.util.List;
import com.example.Util;
import static com.example.Logging.LOG;

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
        // a call on a statically imported field, which is a value and not a type name
        LOG.info(s);
    }
}
