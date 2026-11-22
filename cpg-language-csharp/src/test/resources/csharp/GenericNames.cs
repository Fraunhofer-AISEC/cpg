namespace Test
{
    public class Holder<T>
    {
        public static int Count;

        public int Size;
    }

    class GenericNames
    {
        Holder<int> holder;

        System.Collections.Generic.List<int> list;

        int Create<T>(int x)
        {
            return x;
        }

        int CallGenericMethod(int x)
        {
            return Create<int>(x);
        }

        int AccessStaticOfGenericType()
        {
            return Holder<int>.Count;
        }

        int AccessThroughGenericType(Holder<int> h)
        {
            return h.Size;
        }
    }
}
