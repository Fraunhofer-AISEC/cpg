function someFunction(): Number {
    const i = someOtherFunction();

    return i.length;
}

function someOtherFunction(): String {
    return "hello";
}