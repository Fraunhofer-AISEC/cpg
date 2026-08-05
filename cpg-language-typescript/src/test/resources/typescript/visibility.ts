class TsClass {
    public publicField: number;
    protected protectedField: number;
    private privateField: number;
    static staticField: number;
    private static privateStaticField: number;
    defaultField: number;
    #hardField: number;
    static #hardStaticField: number;

    private constructor() {}

    public publicMethod(): void {}
    protected protectedMethod(): void {}
    private privateMethod(): void {}
    static staticMethod(): void {}
    private static privateStaticMethod(): void {}
    #hardMethod(): void {}
}
