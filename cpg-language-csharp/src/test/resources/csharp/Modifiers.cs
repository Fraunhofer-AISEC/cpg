using System;
using static System.Math;

namespace Test
{
    public class Modifiers
    {
        int implicitPrivate;
        public int pub;
        protected int prot;
        internal int intern;
        protected internal int protIntern;
        private protected int privProt;
        public static int stat;
        public const int Const = 1;
        public static int StaticProp { get; set; }
        public int Prop { get; private set; }

        public Modifiers(int x)
        {
            this.pub = x;
        }

        public static void Stat() { }

        void Inst() { }

        int Over(int a) { return a; }

        string Over(string a) { return a; }

        void Call()
        {
            var v = 1;
            Over(v);
        }

        long Add(int a, long b)
        {
            return a + b;
        }

        bool ShortCircuit(object o, string s)
        {
            return o != null && s == null || s != null;
        }

        string Coalesce(string s)
        {
            return s ?? "default";
        }
    }

    interface IShape
    {
        void Area();
    }

    class InternalByDefault { }
}
