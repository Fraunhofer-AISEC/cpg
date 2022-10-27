import kotlin.random.URandomKt;

public class TestClass {
    public void ifBothPossible() {
        int y = 6;
        int x = URandomKt.nextUInt();
        if(x < y) {
            y++;
        } else {
            y--;
        }
        System.out.println(y);
    }

    public void ifTrue() {
        int y = 6;
        int x = URandomKt.nextUInt();
        if(true) {
            y++;
        } else {
            y--;
        }
        System.out.println(y);
    }

    public void ifFalse() {
        int y = 6;
        int x = URandomKt.nextUInt();
        if(false) {
            y++;
        } else {
            y--;
        }
        System.out.println(y);
    }

    public void ifTrueComputed() {
        int y = 6;
        int x = URandomKt.nextUInt();
        if(y < 10) {
            y++;
        } else {
            y--;
        }
        System.out.println(y);
    }

    public void ifTrueComputedHard() {
        int z = 2;
        int y = z;
        int x = URandomKt.nextUInt();
        if(y + z < 10) {
            y++;
        } else {
            y--;
        }
        z = 10;
        System.out.println(y);
    }

    public void ifFalseComputedHard() {
        int z = 5;
        int y = z;
        int x = URandomKt.nextUInt();
        if(y + z < 10) {
            y++;
        } else {
            y--;
        }
        z = 3;
        System.out.println(y);
    }

    public void ifFalseComputed() {
        int y = 6;
        int x = URandomKt.nextUInt();
        if(y < 0) {
            y++;
        } else {
            y--;
        }
        System.out.println(y);
    }

    public void whileTrueEndless() {
        boolean x = true;
        while(x) {
            System.out.println("Cool loop");
        }
        System.out.println("After cool loop");
    }

    public void whileTrue() {
        boolean x = true;
        while(x) {
            System.out.println("Cool loop");
            x = false;
        }
        System.out.println("After cool loop");
    }

    public void whileFalse() {
        while(false) {
            System.out.println("Cool loop");
        }
        System.out.println("After cool loop");
    }

    public void whileComputedTrue() {
        int y = 1;
        while(y < 3) {
            System.out.println("Cool loop");
        }
        System.out.println("After cool loop");
    }

    public void whileComputedFalse() {
        int y = 1;
        while(y > 3) {
            System.out.println("Cool loop");
        }
        System.out.println("After cool loop");
    }

    public void whileUnknown() {
        int y = URandomKt.nextUInt();
        while(y < 3) {
            System.out.println("Cool loop");
            y = URandomKt.nextUInt();
        }
        System.out.println("After cool loop");
    }
}