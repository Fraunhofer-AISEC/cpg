public class Operators {

    public static void main(String[] args) {
        // results should be type IntegerType("int")
        List.of(
            1 + 2,
			3 - 4,
			5 * 6,
			7 / 8,
			9 % 10,
			11 << 12,
			13 >> 14,
			14 >>> 14,
			15 ^ 16,
			17 & 18,
			19 | 20,
			+21,
			-22,
			~23
        );

        // results should be type IntegerType("long")
        List.of(
            1L + 2,
			3 - 4L,
			5L * 6,
			7 / 8L,
			9L % 10,
			11L << 12,
			13L >> 14,
			14L >>> 14,
			15 ^ 16L,
			17L & 18,
			19 | 20L,
			+21L,
			-22L,
			~23L
        );

        // results should be type FloatingPointType("float")
        List.of(
            1.f + 2,
			3 - 4.f,
			5.f * 6,
			7 / 8.f,
			9.f % 10,
			+21.f,
			-22.f
        );

		// results should be type FloatingPointType("long")
        List.of(
            1.f + 2.d,
			3 - 4.d,
			5.d * 6.f,
			7.d / 8.f,
			9.f % 10.d,
			+21.d,
			-22.d
        );

        // results should be type BooleanType
        List.of(
            true && false,
			true & true,
			false || true,
			true | true,
			false ^ true,
			!false
        );

        // result should be type StringType
        List.of(
            "1" + 2,
			3 + "4" ,
			"5" + true,
			'7' + "8",
			"9" + null,
			new ArrayList<Object>() + "12"
        );
    }

}