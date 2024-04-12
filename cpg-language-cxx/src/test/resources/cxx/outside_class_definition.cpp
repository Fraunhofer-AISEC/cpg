namespace my {
    class MainClass {
        class SubClass {
            void doSomething();

            int field;
        };
    };

    void MainClass::SubClass::doSomething() {
        // do something
        field = 1;
    }
}
