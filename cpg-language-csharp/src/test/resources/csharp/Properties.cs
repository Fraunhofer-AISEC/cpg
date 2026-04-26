namespace HelloWorld
{
    class Foo
    {
        private int reader;

        // Auto-property with getter and setter
        public int AutoProp { get; set; }

        // Auto-property getter-only with initializer
        public int GetOnly { get; } = 42;

        // Property with a full getter body
        public int WithBody
        {
            get
            {
                return reader;
            }
        }

        // Property-level expression body (get-only)
        public int ExprBody => reader;

        // Property with an expression-bodied getter accessor
        public int AccessorExpr
        {
            get => reader;
        }

        // Mixed accessor visibility: public getter, private setter
        public int Restricted { get; private set; }
    }
}