struct Foo {
    x: i32,
}

enum E {
    Foo(Foo), // variant name == struct name
}

fn main() {
    let foo = Foo { x: 42 };

    let e = E::Foo(foo);

    match e {
        E::Foo(inner) => {
            println!("{}", inner.x);
        }
    }
}