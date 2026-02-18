public class Operators {

    public int testArithmetic(int a, int b) {
        int sum = a + b;
        int diff = a - b;
        int prod = a * b;
        int quot = a / b;
        int mod = a % b;
        return sum + diff + prod + quot + mod;
    }

    public boolean testComparison(int a, int b) {
        boolean eq = a == b;
        boolean ne = a != b;
        boolean gt = a > b;
        boolean lt = a < b;
        boolean ge = a >= b;
        boolean le = a <= b;
        return eq && ne && gt && lt && ge && le;
    }

    public int testBitwise(int a, int b) {
        int and = a & b;
        int or = a | b;
        int xor = a ^ b;
        int shl = a << b;
        int shr = a >> b;
        int ushr = a >>> b;
        return and + or + xor + shl + shr + ushr;
    }

    public int testUnary(int a) {
        int neg = -a;
        return neg;
    }

    public int testArrayLength(int[] arr) {
        return arr.length;
    }

    public double testCast(int a) {
        double d = (double) a;
        return d;
    }

    public boolean testInstanceOf(Object obj) {
        return obj instanceof String;
    }
}

