#nullable enable

namespace Test
{
    public class Holder
    {
        public int Size;
    }

    class NullableTypes
    {
        string? text;

        int? number;

        byte[]? bytes;

        System.Collections.Generic.List<int>? list;

        Holder? holder;

        int AccessThroughNullable(Holder? h)
        {
            return h.Size;
        }
    }
}
