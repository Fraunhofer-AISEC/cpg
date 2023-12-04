[[function_attribute()]]
int main() {
}

[[record_attribute()]]
class SomeClass {
    [[property_attribute("a", main, 2)]]
    int a;

    // defined by a macro set by the test
    PROPERTY_ATTRIBUTE(SomeCategory, SomeOtherThing)
    int b;
};
