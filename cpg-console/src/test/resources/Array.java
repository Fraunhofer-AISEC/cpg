class Array {
    public static void main(String[] args) {
        char[] c = new char[4];

        int a = 4;
        int b = a + 1;

        char d = c[b];

        // obviously null
        AnotherObject obj = null;

        // lets make it a little bit tricky at least
        obj = something;

        if (something) {
            AnotherObject yetAnotherObject = null;

            // whoops, overriden with null again
            obj = yetAnotherObject;
        }

        obj.doSomething();

        String s = "some string";
    }
}