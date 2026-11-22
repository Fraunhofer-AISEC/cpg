namespace Test
{
    class Super
    {
        public int field;

        public virtual int Describe()
        {
            return 0;
        }
    }

    class Sub : Super
    {
        public override int Describe()
        {
            return 1;
        }

        int CallBaseMethod()
        {
            return base.Describe();
        }

        int AccessBaseField()
        {
            return base.field;
        }
    }
}
