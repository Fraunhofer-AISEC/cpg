fn foo(x: i32) {
    match x {
        1 => println!("one"),
        2 => {
            println!("two");
        }
        _ => println!("many"),
    }
}
