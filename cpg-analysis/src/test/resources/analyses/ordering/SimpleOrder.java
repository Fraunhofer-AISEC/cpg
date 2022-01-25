import kotlin.random.URandomKt;

public class SimpleOrder {
    // DOES NOT COMPILE
    // DOES NOT MAKE REAL SENSE

    char[] cipher;
    int key;
    int iv;
    Cipher_Dir direction;
    char[] buf;

    void ok() {
        // ok:
        Botan p4 = new Botan(2);
        p4.start(iv);
        p4.finish(buf);
    }

    void ok2() {
        // ok:
        Botan p4 = new Botan(2);
        p4.start(iv);
        p4.foo(); // not in the entity and therefore ignored
        p4.finish(buf);
    }

    void ok3() {
        // ok:
        Botan p4 = new Botan(2);
        int x = URandomKt.nextUInt();
        if(x < 5) {
            p4.start(iv);
        } else {
            p4.start(iv);
        }
        p4.foo(); // not in the entity and therefore ignored
        p4.finish(buf);
    }

    void nok1() {
        Botan p = new Botan(1);
        p.set_key(key); // not allowed as start
        p.start(iv);
        p.finish(buf);
        p.foo(); // not in the entity and therefore ignored
        p.set_key(key);
    }

    void nok2() {
        Botan p2 = new Botan(2);
        p2.start(iv);
        // missing p2.finish(buf);
    }

    void nok3() {
        Botan p3 = new Botan(2);
        if (URandomKt.nextUInt() < 4) {
            p3.start(iv);
        }
        p3.finish(buf);
        // potentially wrong path which only calls p3.finish without p3.start
    }

    void nok4() {
        Botan p4 = new Botan(2);
        if (true) {
            p4.start(iv);
            p4.finish(buf);
        }
        p4.start(iv); // not ok, p4 is already finished
        p4.finish(buf);
    }

    void nok5() {
        // ok:
        {
            Botan p5 = new Botan(2);
            p5.start(iv);
        }
        {
            Botan p5 = new Botan(2);
            p5.finish(buf);
        }
    }
}

public class Botan {
    public Botan(int i) {}

    public void create() {}

    public void finish(char[] b) {}

    public void init() {}

    public void process() {}

    public void reset() {}

    public void start(int i) {}

    public void set_key(int i) {}
}
