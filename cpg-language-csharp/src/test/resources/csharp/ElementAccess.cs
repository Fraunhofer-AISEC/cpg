namespace Test
{
    class ElementAccess
    {
        int ArrayAccess(int[] values, int i)
        {
            return values[i];
        }

        void ArrayWrite(int[] values)
        {
            values[0] = 42;
        }

        string IndexerAccess(Dictionary<string, string> map)
        {
            return map["key"];
        }

        int MultiDimensional(int[,] matrix, int i, int j)
        {
            return matrix[i, j];
        }

        int Multiple(int[][] values, int i, int j)
        {
            return values[i][j];
        }

        int NestedIndex(int[] values, int[] indices, int i)
        {
            return values[indices[i]];
        }

        int MemberAccessBase(Container container, int i)
        {
            return container.Values[i];
        }
    }
}