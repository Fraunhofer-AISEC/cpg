public class BasicSlice {

    public static void main(String[] args) {     // samples/BasicSlice.java:a:12:13 samples/BasicSlice.java
        int a = 0;
        int b = 1, c = 0, d = 0;
        boolean sunShines = true; // and i am still inside :(

        /*
        if (b > 2) {
            a +=2; // 3
        } else {
            a -=2; // 2
        }
        b = a; // 1
        a -= 4; // 4

         */

        if (a > 0) {
            d = 5;
            c = 2;
            if (b > 0) {
                d = a * 2;
                a = a + d * 2;
            } else if (b < -2) {
                a = a - 10;
            }
        } else {
            b = -2;
            d = -2;
            a--;
        }

        a = a + b;

        switch (sunShines) {
            case True:
                a = a * 2;
                c = -2;
                break;
            case False:
                a = 290;
                d = -2;
                b = -2;
                break;
        }
    }


}
