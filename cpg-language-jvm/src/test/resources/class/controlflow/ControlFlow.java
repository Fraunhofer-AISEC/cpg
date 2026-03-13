public class ControlFlow {

    public int testIf(int a) {
        if (a > 10) {
            return 1;
        } else {
            return 0;
        }
    }

    public int testGoto(int a) {
        int result = 0;
        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                break;
            }
            result += i;
        }
        return result;
    }

    public void testLoop() {
        int i = 0;
        while (i < 10) {
            i++;
        }
    }
}

