trait MyTrait {
    fn required_method(&self);
    fn default_method(&self) {
        let x = 1;
    }
}

struct MyStruct;

impl MyTrait for MyStruct {
    fn required_method(&self) {
        let y = 2;
    }
}
