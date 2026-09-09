enum Expr {
    // Tuple-field variant
    Call(String, Vec<Expr>),

    // Record-field variant
    Record {
        name: String,
        pars: Vec<Expr>,
    },

    Number(i64),
}

fn main() {
    let mut tuple_pars = Vec::new();
    tuple_pars.push(Expr::Number(1));
    tuple_pars.push(Expr::Number(2));

    let tuple_expr = Expr::Call(
        String::from("add"),
        tuple_pars,
    );

    let mut record_pars = Vec::new();
    record_pars.push(Expr::Number(10));
    record_pars.push(Expr::Number(20));

    let record_expr = Expr::Record {
        name: String::from("point"),
        pars: record_pars,
    };

    // Use the values without printing macros.
    match tuple_expr {
        Expr::Call(name, pars) => {
            let _name = name;
            let _pars = pars;
        }
        _ => {}
    }

    match record_expr {
        Expr::Record { name, pars } => {
            let _name = name;
            let _pars = pars;
        }
        _ => {}
    }
}