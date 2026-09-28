namespace HelloWorld
{
    ref struct Buffer
    {
        public int Length;
    }

    class Scoped
    {
        int LocalWithScopedType()
        {
            scoped Buffer buffer = default;
            return buffer.Length;
        }
    }
}
