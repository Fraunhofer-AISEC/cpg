package multistep;

public class Level2 extends Level1 {

    public int getField2() {
        return field;
    }

    public int calculate() {
        return getNumber() + 1;
    }
}