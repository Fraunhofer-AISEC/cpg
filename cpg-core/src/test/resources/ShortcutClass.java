public class ShortcutClass {
    private int attr = 0;

    public String toString() {
        return "ShortcutClass: attr=" + attr;
    }

    public int print() {
        System.out.println(this.toString());
    }

    public void magic(int b) {
        if(b > 5) {
            if(attr == 2) {
                attr = 3;
            } else {
                attr = 2;
            }
        } else {
            attr = b;
        }
    }

    public static void main(String[] args) {
        ShortcutClass sc = new ShortcutClass();
        sc.print();
        sc.magic(3);
    }
}