class Person
{
    private int age;

    // Auto-property with getter and setter
    public string Name { get; set; }

    // Auto-property getter-only with initializer
    public int BirthYear { get; } = 1990;

    // Property with a full getter body
    public int Age
    {
        get
        {
            return age;
        }
    }

    // Property-level expression body (get-only)
    public string FullName => Name;

    // Property with an expression-bodied getter accessor
    public int AgeInMonths
    {
        get => age * 12;
    }

    // Mixed accessor visibility: public getter, private setter
    public string Email { get; private set; }
}