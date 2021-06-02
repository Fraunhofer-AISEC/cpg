class Array {
    public static void main(String[] args) {
        char[] c = new char[4];

        int a = 4;
        int b = a + 1;

        char d = c[b];

        // obviously null
        AnotherObject obj = null;

        // lets make it a little bit tricky at least
        obj = AnotherObject();

        if(something) {
            // whoops, overriden with null again
            obj = null;
        }

        obj.doSomething();
    }
}