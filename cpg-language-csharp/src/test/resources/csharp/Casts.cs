namespace Test
{
    class Casts
    {
        int PrimitiveCast(double d)
        {
            return (int)d;
        }

        Base ReferenceCast(object o)
        {
            return (Base)o;
        }

        int NestedCast(object o)
        {
            return (int)(long)o;
        }

        int CastOfCall(object o)
        {
            return (int)Get(o);
        }

        double CastInBinaryOperator(int i, double d)
        {
            return (double)i + d;
        }

        Base SafeCast(object o)
        {
            return o as Base;
        }

        string SafeCastToPredefinedType(object o)
        {
            return o as string;
        }

        bool SafeCastInCondition(object o)
        {
            return o as Base != null;
        }

        object Get(object o)
        {
            return o;
        }
    }
}