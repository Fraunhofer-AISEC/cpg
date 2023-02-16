import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class Lambda {
    private static void testFunction(BiFunction<String, Integer, String> lambda) {
        int a = 5;
        String b = "test";
        String result = lambda.apply(b, a);
    }

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(List.of(0, 1, 2, 5));
        list.forEach( (n) -> System.out.println("tada " + n));
        list.replaceAll((n) -> n+1);
        String outerVar = "outerVar";
        var result = list.stream().map((i) -> outerVar+i).toList();

        BiFunction<String, Integer, String> lambda = (strV, intV) -> outerVar + strV + intV;
        Lambda.testFunction(lambda);

        BiFunction<String, Integer, String> lambdaVar = new BiFunction<String, Integer, String>() {
            @Override
            public String apply(String s, Integer integer) {
                return outerVar + s + integer;
            }
        };
        Lambda.testFunction(lambdaVar);
    }
}