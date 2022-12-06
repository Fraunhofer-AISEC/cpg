package compiling;

public class OuterClass {
    String a = "test";

    public class InnerClass {

        public class EvenMoreInnerClass {

            String b;

            void doSomething() {
                b = OuterClass.this.a;
            }
        }
    }
}