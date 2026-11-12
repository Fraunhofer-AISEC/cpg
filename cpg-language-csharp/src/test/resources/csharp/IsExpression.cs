namespace Test
{
    class IsExpression
    {
        bool IsReferenceType(object o)
        {
            return o is Base;
        }

        bool IsPredefinedType(object o)
        {
            return o is string;
        }

        void IsInCondition(object o)
        {
            if (o is Base)
            {
                Handle(o);
            }
        }

        void Handle(object o) { }
    }
}