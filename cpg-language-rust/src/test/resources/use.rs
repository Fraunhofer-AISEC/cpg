#![allow(dead_code)]
#![allow(unused_imports)]

/*
    ===== Module definitions =====
*/

mod math {
    pub mod basic {
        pub fn add() { println!("math::basic::add"); }        // LINE 9
        pub fn sub() { println!("math::basic::sub"); }        // LINE 10
    }

    pub mod advanced {
        pub fn mul() { println!("math::advanced::mul"); }     // LINE 14
        pub fn div() { println!("math::advanced::div"); }     // LINE 15
    }
}

mod utils {
    pub fn helper() { println!("utils::helper"); }            // LINE 21

    pub mod inner {
        pub fn helper() { println!("utils::inner::helper"); } // LINE 24
    }
}

mod extra {
    pub fn foo() { println!("extra::foo"); }                  // LINE 29
    pub fn bar() { println!("extra::bar"); }                  // LINE 30
}

/*
    ===== Top-level uses =====
*/

use crate::math::basic::add as add_fn;
use crate::math::basic::{sub as sub_fn};
use crate::math::{
    advanced::{mul as mul_fn, div as div_fn},
};
use crate::extra::*;
use crate::utils::{self as utils_mod};
use crate::utils::helper as root_helper;

/*
    ===== Nested module to test `super` =====
*/

mod nested {
    pub fn local() { println!("nested::local"); }             // LINE 46

    pub mod child {
        use super::local as parent_local;

        pub fn call() {
            parent_local(); // resolves to LINE 46 (nested::local)
        }
    }
}

/*
    ===== Main function =====
*/

fn main() {
    println!("--- top-level uses ---");

    add_fn();      // resolves to LINE 9  (math::basic::add)
    sub_fn();      // resolves to LINE 10 (math::basic::sub)
    mul_fn();      // resolves to LINE 14 (math::advanced::mul)
    div_fn();      // resolves to LINE 15 (math::advanced::div)

    foo();         // resolves to LINE 29 (extra::foo via glob import)
    bar();         // resolves to LINE 30 (extra::bar via glob import)

    root_helper(); // resolves to LINE 21 (utils::helper)

    println!("--- function-local use (shadowing + nesting) ---");

    {
        use crate::utils::inner::helper as inner_helper_fn;

        inner_helper_fn(); // resolves to LINE 24 (utils::inner::helper)

        use crate::math::{
            basic::{add as add_local, sub as sub_local},
            advanced::{mul as mul_local},
        };

        add_local(); // resolves to LINE 9  (math::basic::add)
        sub_local(); // resolves to LINE 10 (math::basic::sub)
        mul_local(); // resolves to LINE 14 (math::advanced::mul)
    }

    println!("--- shadowing test ---");

    {
        use crate::utils::helper as helper;

        helper(); // resolves to LINE 21 (utils::helper)

        {
            use crate::utils::inner::helper as helper;

            helper(); // resolves to LINE 24 (utils::inner::helper) [shadowed]
        }

        helper(); // resolves to LINE 21 (utils::helper) [shadow restored]
    }

    println!("--- self + module alias ---");

    {
        use crate::utils as u;

        u::helper(); // resolves to LINE 21 (utils::helper)

        use crate::utils::inner as inner_mod;

        inner_mod::helper(); // resolves to LINE 24 (utils::inner::helper)
    }

    println!("--- super test ---");

    nested::child::call(); // calls parent_local() → LINE 46 (nested::local)

    println!("--- done ---");
}