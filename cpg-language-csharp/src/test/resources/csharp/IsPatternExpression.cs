namespace Test
{
    class IsPatternExpression
    {
        bool DeclarationPattern(object o)
        {
            return o is Foo f;
        }

        // The scrutinee is not always a simple reference: GetObject() must only be called once,
        // not once for the type check and once more for the value bound to f.
        bool DeclarationPatternWithSideEffect()
        {
            return GetObject() is Foo f;
        }

        object GetObject()
        {
            return null;
        }

        // Declaration patterns bind value types too, not just reference types.
        bool DeclarationPatternWithValueType(object o)
        {
            return o is int i;
        }

        void DeclarationPatternInCondition(object o)
        {
            if (o is Foo f)
            {
                Handle(f);
            }
        }

        bool ConstantPatternNull(object o)
        {
            return o is null;
        }

        bool VarPattern(object o)
        {
            return o is var y;
        }

        bool NotPattern(object o)
        {
            return o is not null;
        }

        bool RelationalPattern(int x)
        {
            return x is > 0;
        }

        bool AndPattern(int x)
        {
            return x is > 0 and < 100;
        }

        bool OrPattern(object o)
        {
            // Predefined types are unambiguously types, unlike an identifier such as `Foo`
            // (see ConstantPatternSyntax handling).
            return o is int or string;
        }

        // A bare identifier in a pattern is a ConstantPattern for Roslyn, even when it names a
        // type. It is modeled as a type check.
        bool NotTypeCheck(object e)
        {
            return e is not Foo;
        }

        bool OrTypeCheck(object e)
        {
            return e is Foo or Bar;
        }

        // Two patterns in one method must not share their implicit temporary.
        bool TwoPatterns(object a, object b)
        {
            return (a is Foo f) && (b is Bar h);
        }

        bool DiscardDesignation(object o)
        {
            return o is Foo _;
        }

        bool ParenthesizedPattern(object o)
        {
            return o is (Foo or Bar) and not null;
        }

        // Not supported yet, must be recorded as a problem.
        bool RecursivePattern(Foo o)
        {
            return o is { };
        }

        // Pattern variables of an if are scoped to the if, so the same name can be reused.
        void TwoIfsSameName(object a, object b)
        {
            if (a is Foo f) { Handle(f); }
            if (b is Foo f) { Handle(f); }
        }

        void Handle(Foo f) { }
    }

    class Foo { }

    class Bar { }
}
