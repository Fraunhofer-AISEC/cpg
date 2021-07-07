function someFunction(): Number {
    const i = someOtherFunction("hello");

    return i;
}

function someOtherFunction(s: String): Number {
    return s.length;
}