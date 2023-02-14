import java.util.ArrayList;
import java.util.List;

public class Lambda {
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(List.of(0, 1, 2, 5));
        list.forEach( (n) -> System.out.println("tada " + n));
        list.replaceAll((n) -> n+1);
        var result = list.stream().map((i) -> ""+i).toList();
    }
}