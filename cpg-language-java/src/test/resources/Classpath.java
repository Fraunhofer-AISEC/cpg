import com.github.javaparser.ParserConfiguration;

public class Classpath {
    record Point(int x, int y) {}

    void test(Object o) {
        var configuration = new ParserConfiguration();
        var level = configuration.getLanguageLevel();

        if (o instanceof Point p) {
            System.out.println(p.x());
        }
    }
}
