; ModuleID = 'compiler_builtins.0ea671ee-cgu.0'
source_filename = "compiler_builtins.0ea671ee-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

module asm ""
module asm "            .pushsection .text.__rust_probestack"
module asm "            .globl __rust_probestack"
module asm "            .type  __rust_probestack, @function"
module asm "            .hidden __rust_probestack"
module asm "        __rust_probestack:"
module asm "            "
module asm "    .cfi_startproc"
module asm "    pushq  %rbp"
module asm "    .cfi_adjust_cfa_offset 8"
module asm "    .cfi_offset %rbp, -16"
module asm "    movq   %rsp, %rbp"
module asm "    .cfi_def_cfa_register %rbp"
module asm ""
module asm "    mov    %rax,%r11        // duplicate %rax as we're clobbering %r11"
module asm ""
module asm "    // Main loop, taken in one page increments. We're decrementing rsp by"
module asm "    // a page each time until there's less than a page remaining. We're"
module asm "    // guaranteed that this function isn't called unless there's more than a"
module asm "    // page needed."
module asm "    //"
module asm "    // Note that we're also testing against `8(%rsp)` to account for the 8"
module asm "    // bytes pushed on the stack orginally with our return address. Using"
module asm "    // `8(%rsp)` simulates us testing the stack pointer in the caller's"
module asm "    // context."
module asm ""
module asm "    // It's usually called when %rax >= 0x1000, but that's not always true."
module asm "    // Dynamic stack allocation, which is needed to implement unsized"
module asm "    // rvalues, triggers stackprobe even if %rax < 0x1000."
module asm "    // Thus we have to check %r11 first to avoid segfault."
module asm "    cmp    $0x1000,%r11"
module asm "    jna    3f"
module asm "2:"
module asm "    sub    $0x1000,%rsp"
module asm "    test   %rsp,8(%rsp)"
module asm "    sub    $0x1000,%r11"
module asm "    cmp    $0x1000,%r11"
module asm "    ja     2b"
module asm ""
module asm "3:"
module asm "    // Finish up the last remaining stack space requested, getting the last"
module asm "    // bits out of r11"
module asm "    sub    %r11,%rsp"
module asm "    test   %rsp,8(%rsp)"
module asm ""
module asm "    // Restore the stack pointer to what it previously was when entering"
module asm "    // this function. The caller will readjust the stack pointer after we"
module asm "    // return."
module asm "    add    %rax,%rsp"
module asm ""
module asm "    leave"
module asm "    .cfi_def_cfa_register %rsp"
module asm "    .cfi_adjust_cfa_offset -8"
module asm "    ret"
module asm "    .cfi_endproc"
module asm "    "
module asm "            .size __rust_probestack, . - __rust_probestack"
module asm "            .popsection"
module asm "            "

@__ashlsi3 = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @_ZN17compiler_builtins3int5shift9__ashlsi317hf7e8091f323e46d5E
@__ashlti3 = hidden unnamed_addr alias i128 (i128, i32), i128 (i128, i32)* @_ZN17compiler_builtins3int5shift9__ashlti317hdb78eb5232153150E
@__divti3 = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int4sdiv8__divti317h9864093343baf4a4E
@__powisf2 = hidden unnamed_addr alias float (float, i32), float (float, i32)* @_ZN17compiler_builtins5float3pow9__powisf217hc443ebb069659ce1E
@__powidf2 = hidden unnamed_addr alias double (double, i32), double (double, i32)* @_ZN17compiler_builtins5float3pow9__powidf217hb44d86f49244a064E
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h64f19e3e23fa8277E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h424be8bc3d4f5a33E"
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h05e32b74e13533faE" = hidden unnamed_addr alias i8 (i8), i8 (i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h389ce3b426ebe905E"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h8b76fe7bc962b712E" = hidden unnamed_addr alias i16 (i16), i16 (i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17hfd0b17ad78f498f6E"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h1ec76b03190bc142E" = hidden unnamed_addr alias i32 (i32), i32 (i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17hb35a7240cb2a1241E"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17hc1b530f79b44a338E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h424be8bc3d4f5a33E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17hac1f9841cc5408cfE" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h424be8bc3d4f5a33E"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17hbe65457ecd41b005E" = hidden unnamed_addr alias i128 (i128), i128 (i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h7a6c34ae93988e2bE"
@__floatunsidf = hidden unnamed_addr alias double (i32), double (i32)* @_ZN17compiler_builtins5float4conv13__floatunsidf17h52495b14b26fcd57E
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h7c5f63da43358962E" = hidden unnamed_addr alias i8 (i8, i32), i8 (i8, i32)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h43f0be6c2bed5926E"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h1771828752785387E" = hidden unnamed_addr alias i16 (i16, i32), i16 (i16, i32)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h9e7e89dfb69559bfE"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17ha6a7fc39687ab66bE" = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h2d9cda3a5d0bb116E"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h9bc3863fd5057d33E" = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h2d9cda3a5d0bb116E"
@__udivmoddi4 = hidden unnamed_addr alias i64 (i64, i64, i64*), i64 (i64, i64, i64*)* @_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E
@__fixunsdfsi = hidden unnamed_addr alias i32 (double), i32 (double)* @_ZN17compiler_builtins5float4conv12__fixunsdfsi17h1ea0fb892fac3f9bE
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h9813d3b527a4b145E" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h71ab96f3f7dd29deE"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h0edad2731a0bc365E" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h71ab96f3f7dd29deE"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h3d00714ff50f89d1E" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h71ab96f3f7dd29deE"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h65311a90eb629634E" = hidden unnamed_addr alias i32 (i128), i32 (i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17hfc5e21c2edd9d8a2E"
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h6b7f13e4d71ce32bE" = hidden unnamed_addr alias i32 (i8), i32 (i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17hef75fe0301096b7fE"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h95cb121bfa946d2cE" = hidden unnamed_addr alias i32 (i16), i32 (i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h159cdaece7d9d029E"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h7d87bcff7bbfb3a9E" = hidden unnamed_addr alias i32 (i32), i32 (i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17hf4e50181e271378eE"
@__rust_i128_add = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int6addsub15__rust_i128_add17he0810486ef38d614E
@_ZN17compiler_builtins3int6addsub15__rust_u128_add17hf52950fe288ae249E = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int6addsub15__rust_i128_add17he0810486ef38d614E
@__rust_u128_add = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int6addsub15__rust_i128_add17he0810486ef38d614E
@__ashrdi3 = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @_ZN17compiler_builtins3int5shift9__ashrdi317h001d1e9df7d0826bE
@"_ZN51_$LT$u8$u20$as$u20$compiler_builtins..int..HInt$GT$9widen_mul17h16bf34fcbfda1ff0E" = hidden unnamed_addr alias i16 (i8, i8), i16 (i8, i8)* @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17hbb96828275347aa3E"
@"_ZN52_$LT$u16$u20$as$u20$compiler_builtins..int..HInt$GT$9widen_mul17hcee62c45788d57b2E" = hidden unnamed_addr alias i32 (i16, i16), i32 (i16, i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17hf9d4c4b0930d8528E"
@"_ZN52_$LT$u32$u20$as$u20$compiler_builtins..int..HInt$GT$9widen_mul17h0a0c46e9b421f14bE" = hidden unnamed_addr alias i64 (i32, i32), i64 (i32, i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17h031c10a41ee194f4E"
@"_ZN52_$LT$u64$u20$as$u20$compiler_builtins..int..HInt$GT$9widen_mul17h38b575f3640f6a26E" = hidden unnamed_addr alias i128 (i64, i64), i128 (i64, i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17h27d545639dd77e61E"
@"_ZN51_$LT$u8$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17h738f264172efa813E" = hidden unnamed_addr alias i16 (i8, i8), i16 (i8, i8)* @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17hbb96828275347aa3E"
@"_ZN52_$LT$u16$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17h7c3e1d8be8ce27cdE" = hidden unnamed_addr alias i32 (i16, i16), i32 (i16, i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17hf9d4c4b0930d8528E"
@"_ZN52_$LT$u32$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17h01f28cf7e13ad45aE" = hidden unnamed_addr alias i64 (i32, i32), i64 (i32, i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17h031c10a41ee194f4E"
@"_ZN52_$LT$u64$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17h654f9b9205acb432E" = hidden unnamed_addr alias i128 (i64, i64), i128 (i64, i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17h27d545639dd77e61E"
@__floatuntidf = hidden unnamed_addr alias double (i128), double (i128)* @_ZN17compiler_builtins5float4conv13__floatuntidf17h5b8fc0de12a730cfE
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17h0f4c31dcd4d99f4bE" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hdabd916a07a07807E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17h0e5a94f105e31598E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17h7c0ee07b2fb80d2eE" = hidden unnamed_addr alias i8 (i8), i8 (i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h15b7d9f78947b234E"
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hcefbc71124b357e8E" = hidden unnamed_addr alias i8 (i8), i8 (i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h15b7d9f78947b234E"
@"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17h7343a8df1bff4eb2E" = hidden unnamed_addr alias i8 (i8), i8 (i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h15b7d9f78947b234E"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17ha4db9ba693c867d6E" = hidden unnamed_addr alias i16 (i16), i16 (i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hf1550914b234f0eeE"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hf5e885ac9ceff2a1E" = hidden unnamed_addr alias i16 (i16), i16 (i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hf1550914b234f0eeE"
@"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17hc8e40498d1f6c4ceE" = hidden unnamed_addr alias i16 (i16), i16 (i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hf1550914b234f0eeE"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17ha6316251d6db5e25E" = hidden unnamed_addr alias i32 (i32), i32 (i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h4ccaa21b4a01268eE"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hde584f1b1f65d1f7E" = hidden unnamed_addr alias i32 (i32), i32 (i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h4ccaa21b4a01268eE"
@"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17hc44f145fcc27fb93E" = hidden unnamed_addr alias i32 (i32), i32 (i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h4ccaa21b4a01268eE"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hafdfb52564120e2dE" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17h651e6afa822acaabE" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h606d7c0fe9761072E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17h49e1664db4a56e7bE" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17h837c9a0c87bf462eE" = hidden unnamed_addr alias i128 (i128), i128 (i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h609d4be81cbeae6cE"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17he970dae628dc1de9E" = hidden unnamed_addr alias i128 (i128), i128 (i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h609d4be81cbeae6cE"
@"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$8unsigned17h8bf12880e50228beE" = hidden unnamed_addr alias i128 (i128), i128 (i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h609d4be81cbeae6cE"
@"_ZN71_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17hecb1fba4c2694525E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN71_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17h153c6ecaf928b4d2E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN69_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17he3f22b5ae761544cE" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN69_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17h15b1e96a2aa84207E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN71_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17h3af010ed8c700300E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN71_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17h5fcd98bcb2d048d9E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN69_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17h602ea2db4e845045E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN69_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hbea175bad099da71E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN65_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17h645ff78a59de1a56E" = hidden unnamed_addr alias i8 (i8), i8 (i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h15b7d9f78947b234E"
@"_ZN65_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17hac22a0ee222d98b8E" = hidden unnamed_addr alias i8 (i8), i8 (i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h15b7d9f78947b234E"
@"_ZN65_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17hf233a54e3bd32e37E" = hidden unnamed_addr alias i8 (i8), i8 (i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h15b7d9f78947b234E"
@"_ZN65_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h2287177aec8eda53E" = hidden unnamed_addr alias i8 (i8), i8 (i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h15b7d9f78947b234E"
@"_ZN67_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17h3e5ecc6dbd155810E" = hidden unnamed_addr alias i16 (i16), i16 (i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hf1550914b234f0eeE"
@"_ZN67_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17hd55706bde7e69c3fE" = hidden unnamed_addr alias i16 (i16), i16 (i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hf1550914b234f0eeE"
@"_ZN67_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17hd1a80967b324495dE" = hidden unnamed_addr alias i16 (i16), i16 (i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hf1550914b234f0eeE"
@"_ZN67_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h4359c641f1a20f3eE" = hidden unnamed_addr alias i16 (i16), i16 (i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hf1550914b234f0eeE"
@"_ZN67_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17hbcd1255f79e38b03E" = hidden unnamed_addr alias i32 (i32), i32 (i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h4ccaa21b4a01268eE"
@"_ZN67_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17hf1ff782d96c046a3E" = hidden unnamed_addr alias i32 (i32), i32 (i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h4ccaa21b4a01268eE"
@"_ZN67_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17h33b1134867bb5d29E" = hidden unnamed_addr alias i32 (i32), i32 (i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h4ccaa21b4a01268eE"
@"_ZN67_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17hcc4197d9b48eead9E" = hidden unnamed_addr alias i32 (i32), i32 (i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h4ccaa21b4a01268eE"
@"_ZN69_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17hc5cdb0c6af06df11E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN69_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17h379519567fc4f0fdE" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN67_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17h1b269a7076cecd2dE" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN67_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hb11d65a3548e113bE" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN69_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17ha9ae02165eaab943E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN69_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17h00fafe492cf2ad63E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17hce7ece64cef416e1E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hcbf60bb94131eec1E" = hidden unnamed_addr alias i64 (i64), i64 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"
@"_ZN69_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17h9592c803a6d61e1eE" = hidden unnamed_addr alias i128 (i128), i128 (i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h609d4be81cbeae6cE"
@"_ZN69_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17hf818f00787564a23E" = hidden unnamed_addr alias i128 (i128), i128 (i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h609d4be81cbeae6cE"
@"_ZN69_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17haa8ca80e0a68e85bE" = hidden unnamed_addr alias i128 (i128), i128 (i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h609d4be81cbeae6cE"
@"_ZN69_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h121304682fe724b1E" = hidden unnamed_addr alias i128 (i128), i128 (i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h609d4be81cbeae6cE"
@"_ZN52_$LT$u16$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17h6c03d90c8b34f0f9E" = hidden unnamed_addr alias { i8, i8 } (i16), { i8, i8 } (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17hab48faaeafa389a5E"
@"_ZN52_$LT$u32$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17h885cf395dacd38e8E" = hidden unnamed_addr alias { i16, i16 } (i32), { i16, i16 } (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17hd7577bd073daf64fE"
@"_ZN52_$LT$u64$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17h508c943bccc751c0E" = hidden unnamed_addr alias { i32, i32 } (i64), { i32, i32 } (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17h538bfc8f40787228E"
@"_ZN53_$LT$u128$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17h77df40e8f2582cf1E" = hidden unnamed_addr alias { i64, i64 } (i128), { i64, i64 } (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17h3e0a66c71be1dee7E"
@__truncdfsf2 = hidden unnamed_addr alias float (double), float (double)* @_ZN17compiler_builtins5float5trunc12__truncdfsf217h482318a1628cd545E
@__udivsi3 = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @_ZN17compiler_builtins3int4udiv9__udivsi317h50584612c9e69bf1E
@__udivdi3 = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @_ZN17compiler_builtins3int4udiv9__udivdi317hc08bf3f90625fde0E
@"_ZN52_$LT$u16$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17hb64a8c20f7839d32E" = hidden unnamed_addr alias i8 (i16), i8 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17h1463329f897e96d7E"
@"_ZN52_$LT$u32$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17h5ba53f1eb8393923E" = hidden unnamed_addr alias i16 (i32), i16 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17h402e54b1d608b742E"
@"_ZN52_$LT$u64$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17hd08bcbe41494ecdfE" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17h7d8e3bea6fc6af1fE"
@"_ZN53_$LT$u128$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17h5b1257f3a2b9a74fE" = hidden unnamed_addr alias i64 (i128), i64 (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17hbc6a3496555ac319E"
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17hf9784278f083e179E" = hidden unnamed_addr alias i64 (i1), i64 (i1)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h91b89361e4f85204E"
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17hddaa79f026bf5a64E" = hidden unnamed_addr alias i8 (i1), i8 (i1)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17ha461a8f8164d4e5fE"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17hc0dbe3f833218753E" = hidden unnamed_addr alias i16 (i1), i16 (i1)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h3093b7bd05625bafE"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h2bcabf22c490dbf1E" = hidden unnamed_addr alias i32 (i1), i32 (i1)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17hff506f32b556ab97E"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h8f6dd6f7fd753697E" = hidden unnamed_addr alias i64 (i1), i64 (i1)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h91b89361e4f85204E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h446fe5e43dd86343E" = hidden unnamed_addr alias i64 (i1), i64 (i1)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h91b89361e4f85204E"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17ha9f282f20997e09fE" = hidden unnamed_addr alias i128 (i1), i128 (i1)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h0ea615cb30d55a7bE"
@"_ZN51_$LT$u8$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h33c51167a0cd6fe9E" = hidden unnamed_addr alias i16 (i8), i16 (i8)* @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hd581a7bb2f7c254cE"
@"_ZN52_$LT$u16$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17hd95d2b62c80d54a0E" = hidden unnamed_addr alias i32 (i16), i32 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17h152d0a820037c71fE"
@"_ZN52_$LT$u32$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h7cba2201e84daca1E" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17he456ea74c9916a66E"
@"_ZN52_$LT$u64$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h77a46c3a1f56aa66E" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hab7aa2541e64881cE"
@"_ZN51_$LT$u8$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17haec57facc06c52b2E" = hidden unnamed_addr alias i16 (i8), i16 (i8)* @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hd581a7bb2f7c254cE"
@"_ZN52_$LT$u16$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hd7945e363f5b4018E" = hidden unnamed_addr alias i32 (i16), i32 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17h152d0a820037c71fE"
@"_ZN52_$LT$u32$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17h9e959ba678a8768dE" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17he456ea74c9916a66E"
@"_ZN52_$LT$u64$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17h77b15005a2dcf4acE" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hab7aa2541e64881cE"
@"_ZN70_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17h09eab255299ee61eE" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hab7aa2541e64881cE"
@"_ZN70_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h2d16672f68f0ac9fE" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hab7aa2541e64881cE"
@"_ZN68_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17h210c2a89869b49b4E" = hidden unnamed_addr alias i64 (i8), i64 (i8)* @"_ZN66_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hef58ba17cd498d0eE"
@"_ZN66_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17h95e14745732ff58aE" = hidden unnamed_addr alias i16 (i8), i16 (i8)* @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hd581a7bb2f7c254cE"
@"_ZN66_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17hfe1e243c5bf36774E" = hidden unnamed_addr alias i16 (i8), i16 (i8)* @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hd581a7bb2f7c254cE"
@"_ZN66_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17h688d858f04c2dd7fE" = hidden unnamed_addr alias i32 (i8), i32 (i8)* @"_ZN66_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17hecc048f26c9a4f63E"
@"_ZN68_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17hbd8b9da4ae8704eaE" = hidden unnamed_addr alias i64 (i8), i64 (i8)* @"_ZN66_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hef58ba17cd498d0eE"
@"_ZN66_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17h0daefb7346f4615eE" = hidden unnamed_addr alias i64 (i8), i64 (i8)* @"_ZN66_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hef58ba17cd498d0eE"
@"_ZN67_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17ha2304ec25b31857cE" = hidden unnamed_addr alias i128 (i8), i128 (i8)* @"_ZN67_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17hee76aae7d86d0d2fE"
@"_ZN69_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17h52c1976055ee5001E" = hidden unnamed_addr alias i64 (i16), i64 (i16)* @"_ZN67_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17h8cb48dfdfc621d2cE"
@"_ZN67_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17hfef6cf94cc6db581E" = hidden unnamed_addr alias i32 (i16), i32 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17h152d0a820037c71fE"
@"_ZN67_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h5b340c8977ae90b1E" = hidden unnamed_addr alias i32 (i16), i32 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17h152d0a820037c71fE"
@"_ZN69_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17h45912fb837afe858E" = hidden unnamed_addr alias i64 (i16), i64 (i16)* @"_ZN67_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17h8cb48dfdfc621d2cE"
@"_ZN67_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17hc4101458b4efcaddE" = hidden unnamed_addr alias i64 (i16), i64 (i16)* @"_ZN67_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17h8cb48dfdfc621d2cE"
@"_ZN68_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17he551a835dfff5767E" = hidden unnamed_addr alias i128 (i16), i128 (i16)* @"_ZN68_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h4caef293bb753cc8E"
@"_ZN69_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17h063d29f1eda9ce05E" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17he456ea74c9916a66E"
@"_ZN69_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17h555e99062d485946E" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17he456ea74c9916a66E"
@"_ZN67_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17h759a1e7fbdc6931eE" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17he456ea74c9916a66E"
@"_ZN67_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17h9a47d4dcf131c75fE" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17he456ea74c9916a66E"
@"_ZN68_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17h40b8016407abc729E" = hidden unnamed_addr alias i128 (i32), i128 (i32)* @"_ZN68_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h6a3f09413d52631bE"
@"_ZN68_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17hbd6e180b423674e8E" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hab7aa2541e64881cE"
@"_ZN68_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h280ffd8b63fd0e80E" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hab7aa2541e64881cE"
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17he13d1f66969dfbf0E" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hbca044964faa5ae0E"
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h93ea9522de7bccf9E" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hbca044964faa5ae0E"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h1b9f50519792defaE" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hbca044964faa5ae0E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17he94733e7849ea27eE" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hbca044964faa5ae0E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h62671d1f0346cceeE" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hbca044964faa5ae0E"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17h22394eb5fbd7af6eE" = hidden unnamed_addr alias i128 (i128, i32), i128 (i128, i32)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h86ad79a0618b7570E"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h6da3fc94b8621f59E" = hidden unnamed_addr alias i128 (i128, i32), i128 (i128, i32)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h86ad79a0618b7570E"
@__extendsfdf2 = hidden unnamed_addr alias double (float), double (float)* @_ZN17compiler_builtins5float6extend13__extendsfdf217h943d2d697ea1a5e8E
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17hc869aacc5dca2ad6E" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17h7666acffbb2a576aE"
@"_ZN70_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17h2e4f4843c50619c1E" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h759088ab849a8fa7E"
@"_ZN70_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17hcf8cc7b6ccba597fE" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h759088ab849a8fa7E"
@"_ZN68_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17hbc5522d238c8639eE" = hidden unnamed_addr alias i64 (i8), i64 (i8)* @"_ZN66_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hbe3fb013ddfdd8caE"
@"_ZN66_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17h00fbd2eeb2c878c6E" = hidden unnamed_addr alias i16 (i8), i16 (i8)* @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h9389d382586f2123E"
@"_ZN66_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h768d00af55f8abecE" = hidden unnamed_addr alias i16 (i8), i16 (i8)* @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h9389d382586f2123E"
@"_ZN66_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17h8ce935a619e5a12fE" = hidden unnamed_addr alias i32 (i8), i32 (i8)* @"_ZN66_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h876343148483e526E"
@"_ZN68_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17h81f98fd75680603dE" = hidden unnamed_addr alias i64 (i8), i64 (i8)* @"_ZN66_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hbe3fb013ddfdd8caE"
@"_ZN66_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17he5e064c42f0f30b4E" = hidden unnamed_addr alias i64 (i8), i64 (i8)* @"_ZN66_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hbe3fb013ddfdd8caE"
@"_ZN67_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17hed89bb0b36f2ed9fE" = hidden unnamed_addr alias i128 (i8), i128 (i8)* @"_ZN67_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h5ccdd81da1cde183E"
@"_ZN69_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17hb209376aa8b141c8E" = hidden unnamed_addr alias i64 (i16), i64 (i16)* @"_ZN67_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hb094f236667a92a9E"
@"_ZN67_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17hb281447ddc4feec4E" = hidden unnamed_addr alias i32 (i16), i32 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17hdf95dcca3e132be5E"
@"_ZN67_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h67f628d0af4dc7b4E" = hidden unnamed_addr alias i32 (i16), i32 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17hdf95dcca3e132be5E"
@"_ZN69_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17ha62cc6069e36e20cE" = hidden unnamed_addr alias i64 (i16), i64 (i16)* @"_ZN67_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hb094f236667a92a9E"
@"_ZN67_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17h2afe476dfb55c938E" = hidden unnamed_addr alias i64 (i16), i64 (i16)* @"_ZN67_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hb094f236667a92a9E"
@"_ZN68_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17h08e2cfcc3073991fE" = hidden unnamed_addr alias i128 (i16), i128 (i16)* @"_ZN68_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17hf56dcc0126695dcaE"
@"_ZN69_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17h65a15d212dfa5d74E" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h8e95efb8625550c0E"
@"_ZN69_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17h5843e7434347947bE" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h8e95efb8625550c0E"
@"_ZN67_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17hf5f5f5a4da92d701E" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h8e95efb8625550c0E"
@"_ZN67_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17h363894b26997dd46E" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h8e95efb8625550c0E"
@"_ZN68_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17he25568f3aa018be9E" = hidden unnamed_addr alias i128 (i32), i128 (i32)* @"_ZN68_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h8d02e2a8dc9e3ebdE"
@"_ZN68_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$u128$GT$$GT$4cast17hdc5c583fc198db33E" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h759088ab849a8fa7E"
@"_ZN68_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h64715fa6cc6f0c54E" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h759088ab849a8fa7E"
@__unordsf2 = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp10__unordsf217h398a701a49d7de0bE
@__unorddf2 = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp10__unorddf217hd63f8d61cd349cf9E
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17h70583e31fbd48d0cE" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17he57e36ff7eb8f488E"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17h463d95678ebba142E" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17he57e36ff7eb8f488E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17hc635daffdea2e411E" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17he57e36ff7eb8f488E"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17hff5ae98e10e1a932E" = hidden unnamed_addr alias i128 (i128, i32), i128 (i128, i32)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17hb1e633d126db72ddE"
@__muldi3 = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @_ZN17compiler_builtins3int3mul8__muldi317h3d08cf1a53d1f1f7E
@"_ZN52_$LT$u16$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h6e1b8842ecc6f0a2E" = hidden unnamed_addr alias i8 (i16), i8 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h3334d26e0c9c4bfbE"
@"_ZN52_$LT$u32$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h288ad241f3032fa8E" = hidden unnamed_addr alias i16 (i32), i16 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h14ba82835a8a932fE"
@"_ZN52_$LT$u64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h79c65252c14cac97E" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17haba78631228d2ab6E"
@"_ZN53_$LT$u128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h8daee7bf172f7b98E" = hidden unnamed_addr alias i64 (i128), i64 (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h972d40a580ed2081E"
@"_ZN68_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17h91fba668669b84e0E" = hidden unnamed_addr alias i8 (i64), i8 (i64)* @"_ZN66_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17hcfcef1dacc7028bdE"
@"_ZN69_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17hf58a344966e920faE" = hidden unnamed_addr alias i16 (i64), i16 (i64)* @"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h809442d170a31f22E"
@"_ZN69_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17h0634e1502630df08E" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17haba78631228d2ab6E"
@"_ZN69_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h7654372496d6b919E" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17haba78631228d2ab6E"
@"_ZN68_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17hb0d0e101a3cebe5cE" = hidden unnamed_addr alias i8 (i64), i8 (i64)* @"_ZN66_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17hcfcef1dacc7028bdE"
@"_ZN68_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17h43fe84b9e763ac3fE" = hidden unnamed_addr alias i8 (i64), i8 (i64)* @"_ZN66_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17hcfcef1dacc7028bdE"
@"_ZN69_$LT$usize$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h560c708ec86630a7E" = hidden unnamed_addr alias i16 (i64), i16 (i64)* @"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h809442d170a31f22E"
@"_ZN69_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17hbbf4c11ea2556797E" = hidden unnamed_addr alias i16 (i64), i16 (i64)* @"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h809442d170a31f22E"
@"_ZN69_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17h18623dc768cacd31E" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17haba78631228d2ab6E"
@"_ZN69_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17ha1deb4170f8b232dE" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17haba78631228d2ab6E"
@"_ZN66_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17h62e4fe6f8387bec5E" = hidden unnamed_addr alias i8 (i16), i8 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h3334d26e0c9c4bfbE"
@"_ZN66_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h6a03a39d40aa28cdE" = hidden unnamed_addr alias i8 (i16), i8 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h3334d26e0c9c4bfbE"
@"_ZN66_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17hc97a885fa08dafe9E" = hidden unnamed_addr alias i8 (i16), i8 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h3334d26e0c9c4bfbE"
@"_ZN66_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h3c3aa7e96bc43164E" = hidden unnamed_addr alias i8 (i16), i8 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h3334d26e0c9c4bfbE"
@"_ZN66_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17h05fc76bdf216cecfE" = hidden unnamed_addr alias i8 (i32), i8 (i32)* @"_ZN66_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h863dd52dfb398b3bE"
@"_ZN67_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17h55e40470f0dcb79eE" = hidden unnamed_addr alias i16 (i32), i16 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h14ba82835a8a932fE"
@"_ZN67_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h3eb755e44eb81431E" = hidden unnamed_addr alias i16 (i32), i16 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h14ba82835a8a932fE"
@"_ZN66_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h8adc6ea763a97300E" = hidden unnamed_addr alias i8 (i32), i8 (i32)* @"_ZN66_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h863dd52dfb398b3bE"
@"_ZN66_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17ha2687b98547a536cE" = hidden unnamed_addr alias i8 (i32), i8 (i32)* @"_ZN66_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h863dd52dfb398b3bE"
@"_ZN67_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17h358ec5e3d2062fd0E" = hidden unnamed_addr alias i16 (i32), i16 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h14ba82835a8a932fE"
@"_ZN67_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h8be7a72a26f432bbE" = hidden unnamed_addr alias i16 (i32), i16 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h14ba82835a8a932fE"
@"_ZN68_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17ha1d262ed523c91fdE" = hidden unnamed_addr alias i8 (i64), i8 (i64)* @"_ZN66_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17hcfcef1dacc7028bdE"
@"_ZN66_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17h189b0724a01f4404E" = hidden unnamed_addr alias i8 (i64), i8 (i64)* @"_ZN66_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17hcfcef1dacc7028bdE"
@"_ZN69_$LT$isize$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h020555c2648cbf97E" = hidden unnamed_addr alias i16 (i64), i16 (i64)* @"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h809442d170a31f22E"
@"_ZN67_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17h9650f6f32ebd8513E" = hidden unnamed_addr alias i16 (i64), i16 (i64)* @"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h809442d170a31f22E"
@"_ZN67_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17h2541ce144864bc7cE" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17haba78631228d2ab6E"
@"_ZN67_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h49b426b2d11e6e2dE" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17haba78631228d2ab6E"
@"_ZN66_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h809ec021cf215855E" = hidden unnamed_addr alias i8 (i64), i8 (i64)* @"_ZN66_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17hcfcef1dacc7028bdE"
@"_ZN66_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17h119b9f8e48803d9fE" = hidden unnamed_addr alias i8 (i64), i8 (i64)* @"_ZN66_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17hcfcef1dacc7028bdE"
@"_ZN67_$LT$u64$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17hf1b24666cea1853dE" = hidden unnamed_addr alias i16 (i64), i16 (i64)* @"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h809442d170a31f22E"
@"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17h4f9418f1b0ab671aE" = hidden unnamed_addr alias i16 (i64), i16 (i64)* @"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h809442d170a31f22E"
@"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17hd4920170979f0b63E" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17haba78631228d2ab6E"
@"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h743958f65691d47fE" = hidden unnamed_addr alias i32 (i64), i32 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17haba78631228d2ab6E"
@"_ZN70_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17h792f73b52982c7b4E" = hidden unnamed_addr alias i64 (i128), i64 (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h972d40a580ed2081E"
@"_ZN70_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17h40f571addaeb7deeE" = hidden unnamed_addr alias i64 (i128), i64 (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h972d40a580ed2081E"
@"_ZN67_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17h7cbd1ea50095b9b6E" = hidden unnamed_addr alias i8 (i128), i8 (i128)* @"_ZN67_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h8d2703c035b9f847E"
@"_ZN68_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17hfc921ecbc854b6bcE" = hidden unnamed_addr alias i16 (i128), i16 (i128)* @"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17hc4f7c240ff061307E"
@"_ZN68_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17hf9f8073659f8cbd9E" = hidden unnamed_addr alias i32 (i128), i32 (i128)* @"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h81cef005f31fdf2eE"
@"_ZN68_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17hc806629fd7bf61f1E" = hidden unnamed_addr alias i64 (i128), i64 (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h972d40a580ed2081E"
@"_ZN68_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17he561b6defd295e3bE" = hidden unnamed_addr alias i64 (i128), i64 (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h972d40a580ed2081E"
@"_ZN70_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$usize$GT$$GT$4cast17h83fc1aa104c9eba1E" = hidden unnamed_addr alias i64 (i128), i64 (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h972d40a580ed2081E"
@"_ZN70_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$isize$GT$$GT$4cast17h748db8e5f4da7ee8E" = hidden unnamed_addr alias i64 (i128), i64 (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h972d40a580ed2081E"
@"_ZN67_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h1ccad6d68b5b6a01E" = hidden unnamed_addr alias i8 (i128), i8 (i128)* @"_ZN67_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h8d2703c035b9f847E"
@"_ZN67_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$u8$GT$$GT$4cast17h911262abad9c2d46E" = hidden unnamed_addr alias i8 (i128), i8 (i128)* @"_ZN67_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h8d2703c035b9f847E"
@"_ZN68_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17hcdd10ec192c600b3E" = hidden unnamed_addr alias i16 (i128), i16 (i128)* @"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17hc4f7c240ff061307E"
@"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$u16$GT$$GT$4cast17h7ca40ab9df331b5eE" = hidden unnamed_addr alias i16 (i128), i16 (i128)* @"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17hc4f7c240ff061307E"
@"_ZN68_$LT$u128$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h40a98086db5acb07E" = hidden unnamed_addr alias i32 (i128), i32 (i128)* @"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h81cef005f31fdf2eE"
@"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$u32$GT$$GT$4cast17h0625ad2a7e979fc8E" = hidden unnamed_addr alias i32 (i128), i32 (i128)* @"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h81cef005f31fdf2eE"
@"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$u64$GT$$GT$4cast17h34b3d7925826ef1cE" = hidden unnamed_addr alias i64 (i128), i64 (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h972d40a580ed2081E"
@"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hedc859a477a11949E" = hidden unnamed_addr alias i64 (i128), i64 (i128)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h972d40a580ed2081E"
@"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$4repr17h4a79a49ea40365f0E" = hidden unnamed_addr alias i32 (float), i32 (float)* @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$11signed_repr17hc2ac4eb884d62273E"
@"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$4repr17hb9717090c8e3e8c4E" = hidden unnamed_addr alias i64 (double), i64 (double)* @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$11signed_repr17h268d1dae8750bed5E"
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h7c22c85afe527f8fE" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h6b61fc2dad9437cdE"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h2a62cf7d9bb517ddE" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h6b61fc2dad9437cdE"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h7fd8a5b69c98351fE" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h6b61fc2dad9437cdE"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17hc49ac5351dad878fE" = hidden unnamed_addr alias i128 (i128, i32), i128 (i128, i32)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17hbfbc7d878fa5353eE"
@__ashrsi3 = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @_ZN17compiler_builtins3int5shift9__ashrsi317h06d87a31e097d3c0E
@__udivmodti4 = hidden unnamed_addr alias i128 (i128, i128, i64*), i128 (i128, i128, i64*)* @_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E
@__llvm_memmove_element_unordered_atomic_1 = hidden unnamed_addr alias void (i8*, i8*, i64), void (i8*, i8*, i64)* @_ZN17compiler_builtins3mem41__llvm_memmove_element_unordered_atomic_117hab562ca7a704838eE
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17hbf8a3d81ef1747caE" = hidden unnamed_addr alias { i64, i8 } (i64, i64), { i64, i8 } (i64, i64)* @"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17h0b6adedcabc3950bE"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17he2995d9027b40274E" = hidden unnamed_addr alias { i64, i8 } (i64, i64), { i64, i8 } (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17hbd8f5f993a8952eeE"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17hc95557dc07c6fb0bE" = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h304fa0338895d35cE"
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h5c14f7eec6e265c2E" = hidden unnamed_addr alias i1 (i64), i1 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h432204bf1c46b739E"
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h8893e365e937507eE" = hidden unnamed_addr alias i1 (i8), i1 (i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h83d2988bb1725b22E"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17ha3950c7776ad1d3aE" = hidden unnamed_addr alias i1 (i16), i1 (i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h35160746e237e679E"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17hfaba2aa873486d94E" = hidden unnamed_addr alias i1 (i32), i1 (i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h721ff5c67e32c6f7E"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17ha1f0aa9d44f750cdE" = hidden unnamed_addr alias i1 (i64), i1 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h432204bf1c46b739E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h3cf967c3e5421fbfE" = hidden unnamed_addr alias i1 (i64), i1 (i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h432204bf1c46b739E"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17ha7261353b001d393E" = hidden unnamed_addr alias i1 (i128), i1 (i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h22e4c998b7682872E"
@__lshrdi3 = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @_ZN17compiler_builtins3int5shift9__lshrdi317h3d3fe01537cf1e08E
@__floattisf = hidden unnamed_addr alias float (i128), float (i128)* @_ZN17compiler_builtins5float4conv11__floattisf17hd3998757712d07f4E
@__lshrsi3 = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @_ZN17compiler_builtins3int5shift9__lshrsi317h05d06ae4526dab3aE
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17he98e6485033d5bafE" = hidden unnamed_addr alias i8 (i8, i32), i8 (i8, i32)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hd7414b7af3f3076fE"
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hde569ef77d489fd2E" = hidden unnamed_addr alias i8 (i8, i32), i8 (i8, i32)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hd7414b7af3f3076fE"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17h66baad13c603d19bE" = hidden unnamed_addr alias i16 (i16, i32), i16 (i16, i32)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h44c46ce08e7aae8bE"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hb532a7c8cb54a3faE" = hidden unnamed_addr alias i16 (i16, i32), i16 (i16, i32)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h44c46ce08e7aae8bE"
@__fixsfdi = hidden unnamed_addr alias i64 (float), i64 (float)* @_ZN17compiler_builtins5float4conv9__fixsfdi17h0e6639760129671bE
@__fixsfti = hidden unnamed_addr alias i128 (float), i128 (float)* @_ZN17compiler_builtins5float4conv9__fixsfti17hc0d01c578e77be4aE
@__fixdfti = hidden unnamed_addr alias i128 (double), i128 (double)* @_ZN17compiler_builtins5float4conv9__fixdfti17h603ce156de3eba31E
@__llvm_memmove_element_unordered_atomic_2 = hidden unnamed_addr alias void (i16*, i16*, i64), void (i16*, i16*, i64)* @_ZN17compiler_builtins3mem41__llvm_memmove_element_unordered_atomic_217h7953b6cedf6a9513E
@__llvm_memmove_element_unordered_atomic_4 = hidden unnamed_addr alias void (i32*, i32*, i64), void (i32*, i32*, i64)* @_ZN17compiler_builtins3mem41__llvm_memmove_element_unordered_atomic_417h87333737abf52cfbE
@__llvm_memmove_element_unordered_atomic_8 = hidden unnamed_addr alias void (i64*, i64*, i64), void (i64*, i64*, i64)* @_ZN17compiler_builtins3mem41__llvm_memmove_element_unordered_atomic_817h63cf96f3c0205be0E
@__modti3 = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int4sdiv8__modti317he17e37567c477bb3E
@__floatsidf = hidden unnamed_addr alias double (i32), double (i32)* @_ZN17compiler_builtins5float4conv11__floatsidf17hd467774adecb1e68E
@__divsi3 = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @_ZN17compiler_builtins3int4sdiv8__divsi317hadf668fbdbc4a68aE
@__ashrti3 = hidden unnamed_addr alias i128 (i128, i32), i128 (i128, i32)* @_ZN17compiler_builtins3int5shift9__ashrti317h87ef6c4a04a8fa1dE
@__llvm_memset_element_unordered_atomic_1 = hidden unnamed_addr alias void (i8*, i8, i64), void (i8*, i8, i64)* @_ZN17compiler_builtins3mem40__llvm_memset_element_unordered_atomic_117h39777d350d5ffff0E
@__fixunssfsi = hidden unnamed_addr alias i32 (float), i32 (float)* @_ZN17compiler_builtins5float4conv12__fixunssfsi17hf54458efcdfb9f48E
@__fixunsdfdi = hidden unnamed_addr alias i64 (double), i64 (double)* @_ZN17compiler_builtins5float4conv12__fixunsdfdi17h8239f57048b408fbE
@__floattidf = hidden unnamed_addr alias double (i128), double (i128)* @_ZN17compiler_builtins5float4conv11__floattidf17h7ca077a44af40196E
@__umoddi3 = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @_ZN17compiler_builtins3int4udiv9__umoddi317h7ef2251ed05e25a3E
@__clzsi2 = hidden unnamed_addr alias i64 (i64), i64 (i64)* @_ZN17compiler_builtins3int13leading_zeros8__clzsi217h44420f7804efd156E
@__divdi3 = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @_ZN17compiler_builtins3int4sdiv8__divdi317he7b7963c951daec7E
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17hfda1ff117b275f18E" = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17he346b9962a61cb6dE"
@__fixunssfdi = hidden unnamed_addr alias i64 (float), i64 (float)* @_ZN17compiler_builtins5float4conv12__fixunssfdi17h8f8d685f960e4b87E
@__fixunssfti = hidden unnamed_addr alias i128 (float), i128 (float)* @_ZN17compiler_builtins5float4conv12__fixunssfti17h08da700ef7403245E
@__fixunsdfti = hidden unnamed_addr alias i128 (double), i128 (double)* @_ZN17compiler_builtins5float4conv12__fixunsdfti17h935cad9de4c1436cE
@__lshrti3 = hidden unnamed_addr alias i128 (i128, i32), i128 (i128, i32)* @_ZN17compiler_builtins3int5shift9__lshrti317h9e5af592047cb74eE
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h6ee4cdb47593a768E" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h6dd5f14560f65fe3E"
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17he667accb1c58bf0fE" = hidden unnamed_addr alias i8 (i8, i8), i8 (i8, i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h658c67e96fe841faE"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h72c4d78f22be2272E" = hidden unnamed_addr alias i16 (i16, i16), i16 (i16, i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17hb6fa2e1e6c90a81aE"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17hc7e73da9b87a5df9E" = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17hf1df666bbff9d8b7E"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h75d8aae81311d8fbE" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h6dd5f14560f65fe3E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17he9d4801cf8d5c083E" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h6dd5f14560f65fe3E"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h298eba80d4d20b79E" = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h25ec688c41a1696eE"
@__umodti3 = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int4udiv9__umodti317hab94b75c62073ef9E
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17h4db1e26a4d45198eE" = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17h363554f4d11e168bE"
@__udivti3 = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int4udiv9__udivti317ha55a25e1f34ecba7E
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17h5bb8438ce8932691E" = hidden unnamed_addr alias i8 (i8, i32), i8 (i8, i32)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17hbf03e0e749dd9523E"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17h68dd4b5d93d02b25E" = hidden unnamed_addr alias i16 (i16, i32), i16 (i16, i32)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17h894c2583a47a65c6E"
@__llvm_memset_element_unordered_atomic_2 = hidden unnamed_addr alias void (i16*, i8, i64), void (i16*, i8, i64)* @_ZN17compiler_builtins3mem40__llvm_memset_element_unordered_atomic_217hdf7be15bf572b0d2E
@__llvm_memset_element_unordered_atomic_4 = hidden unnamed_addr alias void (i32*, i8, i64), void (i32*, i8, i64)* @_ZN17compiler_builtins3mem40__llvm_memset_element_unordered_atomic_417h57132921a27349d7E
@__llvm_memset_element_unordered_atomic_8 = hidden unnamed_addr alias void (i64*, i8, i64), void (i64*, i8, i64)* @_ZN17compiler_builtins3mem40__llvm_memset_element_unordered_atomic_817h2ffc9b593d29c4adE
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17h2c351f0d378e025fE" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17hcc8f7e4d9054e984E"
@__rust_i128_sub = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int6addsub15__rust_i128_sub17hfcc2aa6867c91785E
@_ZN17compiler_builtins3int6addsub15__rust_u128_sub17h5ad0556efbd7feb0E = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int6addsub15__rust_i128_sub17hfcc2aa6867c91785E
@__rust_u128_sub = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int6addsub15__rust_i128_sub17hfcc2aa6867c91785E
@__floatsisf = hidden unnamed_addr alias float (i32), float (i32)* @_ZN17compiler_builtins5float4conv11__floatsisf17hc81f19c8035af6c6E
@__fixdfsi = hidden unnamed_addr alias i32 (double), i32 (double)* @_ZN17compiler_builtins5float4conv9__fixdfsi17h7b2120fc884aa80eE
@__floatuntisf = hidden unnamed_addr alias float (i128), float (i128)* @_ZN17compiler_builtins5float4conv13__floatuntisf17h3644c78b08fa2f34E
@__floatundisf = hidden unnamed_addr alias float (i64), float (i64)* @_ZN17compiler_builtins5float4conv13__floatundisf17h8304f71c44b2b091E
@__lesf2 = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp7__eqsf217he7a3f748735ac2a3E
@__gesf2 = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp7__gesf217h1eb6d709339225f7E
@_ZN17compiler_builtins5float3cmp7__lesf217h8fac853fe0b14ea1E = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp7__eqsf217he7a3f748735ac2a3E
@__eqsf2 = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp7__eqsf217he7a3f748735ac2a3E
@_ZN17compiler_builtins5float3cmp7__ltsf217h067e509dc1965f8aE = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp7__eqsf217he7a3f748735ac2a3E
@__ltsf2 = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp7__eqsf217he7a3f748735ac2a3E
@_ZN17compiler_builtins5float3cmp7__nesf217h1632c929344f882bE = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp7__eqsf217he7a3f748735ac2a3E
@__nesf2 = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp7__eqsf217he7a3f748735ac2a3E
@_ZN17compiler_builtins5float3cmp7__gtsf217hb61ba97a71f9fd44E = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp7__gesf217h1eb6d709339225f7E
@__gtsf2 = hidden unnamed_addr alias i32 (float, float), i32 (float, float)* @_ZN17compiler_builtins5float3cmp7__gesf217h1eb6d709339225f7E
@__ledf2 = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp7__eqdf217hfe55385038c8e487E
@__gedf2 = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp7__gedf217h2c6f81c0073f269cE
@_ZN17compiler_builtins5float3cmp7__ledf217h76f69d3879c99c51E = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp7__eqdf217hfe55385038c8e487E
@__eqdf2 = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp7__eqdf217hfe55385038c8e487E
@_ZN17compiler_builtins5float3cmp7__ltdf217h3f536cd05594cbd3E = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp7__eqdf217hfe55385038c8e487E
@__ltdf2 = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp7__eqdf217hfe55385038c8e487E
@_ZN17compiler_builtins5float3cmp7__nedf217h91d9e2ae07dccffdE = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp7__eqdf217hfe55385038c8e487E
@__nedf2 = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp7__eqdf217hfe55385038c8e487E
@_ZN17compiler_builtins5float3cmp7__gtdf217h21a42976975e0476E = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp7__gedf217h2c6f81c0073f269cE
@__gtdf2 = hidden unnamed_addr alias i32 (double, double), i32 (double, double)* @_ZN17compiler_builtins5float3cmp7__gedf217h2c6f81c0073f269cE
@__floatunsisf = hidden unnamed_addr alias float (i32), float (i32)* @_ZN17compiler_builtins5float4conv13__floatunsisf17hd59c5bbeec77e049E
@__floatdisf = hidden unnamed_addr alias float (i64), float (i64)* @_ZN17compiler_builtins5float4conv11__floatdisf17hb3723a08e7423bd0E
@__floatdidf = hidden unnamed_addr alias double (i64), double (i64)* @_ZN17compiler_builtins5float4conv11__floatdidf17hc017db6e67256b31E
@"_ZN51_$LT$u8$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17hc5392c40f66407d1E" = hidden unnamed_addr alias i16 (i8), i16 (i8)* @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17h40e91ceb18778addE"
@"_ZN52_$LT$u16$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17h9a746ed2533ffb7dE" = hidden unnamed_addr alias i32 (i16), i32 (i16)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17hcddfc0cb6da81b4dE"
@"_ZN52_$LT$u32$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17h298802c66b2e60deE" = hidden unnamed_addr alias i64 (i32), i64 (i32)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17h43e737d59a8c5312E"
@"_ZN52_$LT$u64$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17h26987935e2a45eecE" = hidden unnamed_addr alias i128 (i64), i128 (i64)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17h25b5d792c52a426bE"
@__llvm_memcpy_element_unordered_atomic_1 = hidden unnamed_addr alias void (i8*, i8*, i64), void (i8*, i8*, i64)* @_ZN17compiler_builtins3mem40__llvm_memcpy_element_unordered_atomic_117h4ecc9812d3e3ab8dE
@__llvm_memcpy_element_unordered_atomic_2 = hidden unnamed_addr alias void (i16*, i16*, i64), void (i16*, i16*, i64)* @_ZN17compiler_builtins3mem40__llvm_memcpy_element_unordered_atomic_217h7320826cace4b95dE
@__llvm_memcpy_element_unordered_atomic_4 = hidden unnamed_addr alias void (i32*, i32*, i64), void (i32*, i32*, i64)* @_ZN17compiler_builtins3mem40__llvm_memcpy_element_unordered_atomic_417h57e9c294c739a7c5E
@__llvm_memcpy_element_unordered_atomic_8 = hidden unnamed_addr alias void (i64*, i64*, i64), void (i64*, i64*, i64)* @_ZN17compiler_builtins3mem40__llvm_memcpy_element_unordered_atomic_817h60f1af3251af768aE
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17h4e40f22bc5bfa21eE" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17hab26316137090cd4E"
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17h5fb98acc3dc41854E" = hidden unnamed_addr alias i8 (i8, i8), i8 (i8, i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17hc7084ba9d645fe3dE"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17hc20721ed2b4ac440E" = hidden unnamed_addr alias i16 (i16, i16), i16 (i16, i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17hb7b809c573b727cfE"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17ha63879c037a0da0eE" = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17h050221ccec7c8c3dE"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17h705332ba2c56fa90E" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17hab26316137090cd4E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17h3eff06dcc0da3acbE" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17hab26316137090cd4E"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17h8d78cb553101b838E" = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17h33e4d9671042a9e0E"
@__udivmodsi4 = hidden unnamed_addr alias i32 (i32, i32, i32*), i32 (i32, i32, i32*)* @_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE
@"_ZN53_$LT$usize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h7efad1f9f7cd483bE" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17hf21f0fa7ad9607f5E"
@"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h09d04f9ebcf963f5E" = hidden unnamed_addr alias i8 (i8, i8), i8 (i8, i8)* @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h9bc8dab6ebf2d4b8E"
@"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h7611b46738bd15e7E" = hidden unnamed_addr alias i16 (i16, i16), i16 (i16, i16)* @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17he2b0fbd3e4d74233E"
@"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17hc329cfc91c78d5f4E" = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h1deec60c34110673E"
@"_ZN53_$LT$isize$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h3b63b370c7bab36dE" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17hf21f0fa7ad9607f5E"
@"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h9779c3d158a072d7E" = hidden unnamed_addr alias i64 (i64, i64), i64 (i64, i64)* @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17hf21f0fa7ad9607f5E"
@"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17haa7196462eab890cE" = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h821b94ff07894d5eE"
@__floatundidf = hidden unnamed_addr alias double (i64), double (i64)* @_ZN17compiler_builtins5float4conv13__floatundidf17h88a3f11573bed3beE
@"_ZN52_$LT$u16$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17hc13b4a8ef31e8da1E" = hidden unnamed_addr alias i16 (i8, i8), i16 (i8, i8)* @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17h8765f91e8aa5dee9E"
@"_ZN52_$LT$u32$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17h43d69fc8d68c2740E" = hidden unnamed_addr alias i32 (i16, i16), i32 (i16, i16)* @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17h3cb2943c34635f8bE"
@"_ZN52_$LT$u64$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17h131510c999354b4cE" = hidden unnamed_addr alias i64 (i32, i32), i64 (i32, i32)* @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17h7cf7596ce05f1e02E"
@"_ZN53_$LT$u128$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17hd62bee44602b5c06E" = hidden unnamed_addr alias i128 (i64, i64), i128 (i64, i64)* @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17ha95ab27448b8e8e4E"
@__ashldi3 = hidden unnamed_addr alias i64 (i64, i32), i64 (i64, i32)* @_ZN17compiler_builtins3int5shift9__ashldi317h49ef5dcd9b8206ceE
@__fixsfsi = hidden unnamed_addr alias i32 (float), i32 (float)* @_ZN17compiler_builtins5float4conv9__fixsfsi17h57e50c15835c9c75E
@__fixdfdi = hidden unnamed_addr alias i64 (double), i64 (double)* @_ZN17compiler_builtins5float4conv9__fixdfdi17h06e50da023d9b27eE
@__divmodsi4 = hidden unnamed_addr alias i32 (i32, i32, i32*), i32 (i32, i32, i32*)* @_ZN17compiler_builtins3int4sdiv11__divmodsi417h80e0e1a8a998731dE
@__umodsi3 = hidden unnamed_addr alias i32 (i32, i32), i32 (i32, i32)* @_ZN17compiler_builtins3int4udiv9__umodsi317hcf330f4b294364f6E
@__multi3 = hidden unnamed_addr alias i128 (i128, i128), i128 (i128, i128)* @_ZN17compiler_builtins3int3mul8__multi317h610a2e13318c07e9E

; compiler_builtins::mem::memcpy
; Function Attrs: nonlazybind uwtable
define weak hidden i8* @_ZN17compiler_builtins3mem6memcpy17hef14bddb2144d719E(i8* %dest, i8* %src, i64 %n) unnamed_addr #0 {
start:
  %qword_count.i = lshr i64 %n, 3
  %byte_count.i = and i64 %n, 7
  %0 = tail call { i64, i8*, i8* } asm sideeffect "repe movsq (%rsi), (%rdi)\0Amov ${3:k}, %ecx\0Arepe movsb (%rsi), (%rdi)", "=&{cx},=&{di},=&{si},r,0,1,2,~{memory}"(i64 %byte_count.i, i64 %qword_count.i, i8* %dest, i8* %src) #14, !srcloc !2
  ret i8* %dest
}

; compiler_builtins::mem::memmove
; Function Attrs: nonlazybind uwtable
define weak hidden i8* @_ZN17compiler_builtins3mem7memmove17h161fe3a89cacf0f9E(i8* %dest, i8* %src, i64 %n) unnamed_addr #0 {
start:
  %_5 = ptrtoint i8* %dest to i64
  %_7 = ptrtoint i8* %src to i64
  %0 = sub i64 %_5, %_7
  %_9.not = icmp ult i64 %0, %n
  %qword_count.i = lshr i64 %n, 3
  %byte_count.i = and i64 %n, 7
  br i1 %_9.not, label %bb4, label %bb2

bb4:                                              ; preds = %start
  %.idx.i = add i64 %n, -8
  %1 = getelementptr i8, i8* %dest, i64 %.idx.i
  %2 = getelementptr i8, i8* %src, i64 %.idx.i
  %3 = tail call { i64, i8*, i8* } asm sideeffect "std\0Arepe movsq (%rsi), (%rdi)\0Amovl ${3:k}, %ecx\0Aaddq $$7, %rdi\0Aaddq $$7, %rsi\0Arepe movsb (%rsi), (%rdi)\0Acld", "=&{cx},=&{di},=&{si},r,0,1,2,~{dirflag},~{fpsr},~{flags},~{memory}"(i64 %byte_count.i, i64 %qword_count.i, i8* %1, i8* %2) #14, !srcloc !3
  br label %bb6

bb2:                                              ; preds = %start
  %4 = tail call { i64, i8*, i8* } asm sideeffect "repe movsq (%rsi), (%rdi)\0Amov ${3:k}, %ecx\0Arepe movsb (%rsi), (%rdi)", "=&{cx},=&{di},=&{si},r,0,1,2,~{memory}"(i64 %byte_count.i, i64 %qword_count.i, i8* %dest, i8* %src) #14, !srcloc !2
  br label %bb6

bb6:                                              ; preds = %bb4, %bb2
  ret i8* %dest
}

; compiler_builtins::mem::memset
; Function Attrs: nonlazybind uwtable
define weak hidden i8* @_ZN17compiler_builtins3mem6memset17hb15b25c9009ef1e4E(i8* %s, i32 %c, i64 %n) unnamed_addr #0 {
start:
  %qword_count.i = lshr i64 %n, 3
  %byte_count.i = and i64 %n, 7
  %0 = and i32 %c, 255
  %_12.i = zext i32 %0 to i64
  %_11.i = mul nuw i64 %_12.i, 72340172838076673
  %1 = tail call { i64, i8* } asm sideeffect "repe stosq %rax, (%rdi)\0Amov ${2:k}, %ecx\0Arepe stosb %al, (%rdi)", "=&{cx},=&{di},r,0,1,{ax},~{memory}"(i64 %byte_count.i, i64 %qword_count.i, i8* %s, i64 %_11.i) #14, !srcloc !4
  ret i8* %s
}

; compiler_builtins::mem::memcmp
; Function Attrs: nonlazybind uwtable
define weak hidden i32 @_ZN17compiler_builtins3mem6memcmp17heb8eb7e4b78fd5fbE(i8* %s1, i8* %s2, i64 %n) unnamed_addr #0 {
start:
  %_56.not = icmp eq i64 %n, 0
  br i1 %_56.not, label %bb8, label %bb2

bb1:                                              ; preds = %bb2
  %exitcond.not = icmp eq i64 %2, %n
  br i1 %exitcond.not, label %bb8, label %bb2

bb2:                                              ; preds = %start, %bb1
  %i.07 = phi i64 [ %2, %bb1 ], [ 0, %start ]
  %0 = getelementptr inbounds i8, i8* %s1, i64 %i.07
  %a = load i8, i8* %0, align 1
  %1 = getelementptr inbounds i8, i8* %s2, i64 %i.07
  %b = load i8, i8* %1, align 1
  %_16.not = icmp eq i8 %a, %b
  %2 = add nuw i64 %i.07, 1
  br i1 %_16.not, label %bb1, label %bb5

bb5:                                              ; preds = %bb2
  %_19 = zext i8 %a to i32
  %_21 = zext i8 %b to i32
  %3 = sub nsw i32 %_19, %_21
  br label %bb8

bb8:                                              ; preds = %bb1, %start, %bb5
  %.0 = phi i32 [ %3, %bb5 ], [ 0, %start ], [ 0, %bb1 ]
  ret i32 %.0
}

; compiler_builtins::mem::bcmp
; Function Attrs: nonlazybind uwtable
define weak hidden i32 @_ZN17compiler_builtins3mem4bcmp17h7db3d50cac4f2c7aE(i8* %s1, i8* %s2, i64 %n) unnamed_addr #0 {
start:
; call compiler_builtins::mem::memcmp
  %0 = tail call i32 @_ZN17compiler_builtins3mem6memcmp17heb8eb7e4b78fd5fbE(i8* %s1, i8* %s2, i64 %n)
  ret i32 %0
}

; compiler_builtins::float::add::__addsf3
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float3add8__addsf317hb95467ebe387ff0cE(float %a, float %b) unnamed_addr #1 {
start:
  %0 = bitcast float %a to i32
  %1 = bitcast float %b to i32
  %2 = and i32 %0, 2147483647
  %3 = and i32 %1, 2147483647
  %4 = add nsw i32 %2, -1
  %5 = icmp ugt i32 %4, 2139095038
  %6 = add nsw i32 %3, -1
  %7 = icmp ugt i32 %6, 2139095038
  %or.cond.i = select i1 %5, i1 true, i1 %7
  br i1 %or.cond.i, label %bb18.i, label %bb55.i

bb55.i:                                           ; preds = %bb51.i, %start
  %8 = icmp ugt i32 %3, %2
  %spec.select.i = select i1 %8, i32 %0, i32 %1
  %spec.select38.i = select i1 %8, i32 %1, i32 %0
  %9 = lshr i32 %spec.select38.i, 23
  %_5.0.i52.i = and i32 %9, 255
  %10 = lshr i32 %spec.select.i, 23
  %_5.0.i53.i = and i32 %10, 255
  %11 = and i32 %spec.select38.i, 8388607
  %12 = and i32 %spec.select.i, 8388607
  %13 = icmp eq i32 %_5.0.i52.i, 0
  br i1 %13, label %bb67.i, label %bb70.i

bb18.i:                                           ; preds = %start
  %14 = icmp ugt i32 %2, 2139095040
  br i1 %14, label %bb20.i, label %bb23.i

bb23.i:                                           ; preds = %bb18.i
  %15 = icmp ugt i32 %3, 2139095040
  br i1 %15, label %bb25.i, label %bb28.i

bb20.i:                                           ; preds = %bb18.i
  %16 = or i32 %2, 4194304
  %17 = bitcast i32 %16 to float
  br label %_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit

bb28.i:                                           ; preds = %bb23.i
  %18 = icmp eq i32 %2, 2139095040
  br i1 %18, label %bb30.i, label %bb38.i

bb25.i:                                           ; preds = %bb23.i
  %19 = or i32 %3, 4194304
  %20 = bitcast i32 %19 to float
  br label %_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit

bb38.i:                                           ; preds = %bb28.i
  %21 = icmp eq i32 %3, 2139095040
  br i1 %21, label %_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit, label %bb41.i

bb30.i:                                           ; preds = %bb28.i
  %22 = xor i32 %1, %0
  %23 = icmp eq i32 %22, -2147483648
  %spec.select151.i = select i1 %23, float 0x7FF8000000000000, float %a
  br label %_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit

bb41.i:                                           ; preds = %bb38.i
  %24 = icmp eq i32 %2, 0
  %25 = icmp eq i32 %3, 0
  br i1 %24, label %bb43.i, label %bb51.i

bb51.i:                                           ; preds = %bb41.i
  br i1 %25, label %_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit, label %bb55.i

bb43.i:                                           ; preds = %bb41.i
  br i1 %25, label %bb45.i, label %_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit

bb45.i:                                           ; preds = %bb43.i
  %26 = and i32 %1, %0
  %27 = bitcast i32 %26 to float
  br label %_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit

bb67.i:                                           ; preds = %bb55.i
  %28 = tail call i32 @llvm.ctlz.i32(i32 %11, i1 false) #14, !range !5
  %29 = add nuw nsw i32 %28, 24
  %30 = sub nsw i32 9, %28
  %31 = and i32 %29, 31
  %_10.i.i = shl i32 %11, %31
  br label %bb70.i

bb70.i:                                           ; preds = %bb67.i, %bb55.i
  %a_significand.0.i = phi i32 [ %_10.i.i, %bb67.i ], [ %11, %bb55.i ]
  %a_exponent.0.i = phi i32 [ %30, %bb67.i ], [ %_5.0.i52.i, %bb55.i ]
  %32 = icmp eq i32 %_5.0.i53.i, 0
  br i1 %32, label %bb71.i, label %bb74.i

bb71.i:                                           ; preds = %bb70.i
  %33 = tail call i32 @llvm.ctlz.i32(i32 %12, i1 false) #14, !range !5
  %34 = add nuw nsw i32 %33, 24
  %35 = sub nsw i32 9, %33
  %36 = and i32 %34, 31
  %_10.i54.i = shl i32 %12, %36
  br label %bb74.i

bb74.i:                                           ; preds = %bb71.i, %bb70.i
  %b_significand.0.i = phi i32 [ %_10.i54.i, %bb71.i ], [ %12, %bb70.i ]
  %b_exponent.0.i = phi i32 [ %35, %bb71.i ], [ %_5.0.i53.i, %bb70.i ]
  %37 = and i32 %spec.select38.i, -2147483648
  %38 = xor i32 %spec.select.i, %spec.select38.i
  %.not.i = icmp sgt i32 %38, -1
  %39 = shl i32 %a_significand.0.i, 3
  %_5.0.i55.i = or i32 %39, 67108864
  %40 = shl i32 %b_significand.0.i, 3
  %_5.0.i56.i = or i32 %40, 67108864
  %41 = sub nsw i32 %a_exponent.0.i, %b_exponent.0.i
  %.not152.i = icmp eq i32 %41, 0
  br i1 %.not152.i, label %bb99.i, label %bb86.i

bb99.i:                                           ; preds = %bb88.i, %bb86.i, %bb74.i
  %b_significand.1.i = phi i32 [ %47, %bb88.i ], [ %_5.0.i56.i, %bb74.i ], [ 1, %bb86.i ]
  br i1 %.not.i, label %bb113.i, label %bb100.i

bb86.i:                                           ; preds = %bb74.i
  %42 = icmp ult i32 %41, 32
  br i1 %42, label %bb88.i, label %bb99.i

bb88.i:                                           ; preds = %bb86.i
  %43 = sub nsw i32 0, %41
  %44 = and i32 %43, 31
  %_5.0.i57.i = shl i32 %_5.0.i56.i, %44
  %45 = icmp ne i32 %_5.0.i57.i, 0
  %46 = zext i1 %45 to i32
  %_5.0.i58.i = lshr i32 %_5.0.i56.i, %41
  %47 = or i32 %_5.0.i58.i, %46
  br label %bb99.i

bb113.i:                                          ; preds = %bb99.i
  %_4.0.i.i = add i32 %b_significand.1.i, %_5.0.i55.i
  %48 = and i32 %_4.0.i.i, 134217728
  %.not153.i = icmp eq i32 %48, 0
  br i1 %.not153.i, label %bb125.i, label %bb118.i

bb100.i:                                          ; preds = %bb99.i
  %49 = sub i32 %_5.0.i55.i, %b_significand.1.i
  %50 = icmp eq i32 %49, 0
  br i1 %50, label %_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit, label %bb104.i

bb104.i:                                          ; preds = %bb100.i
  %51 = icmp ult i32 %49, 67108864
  br i1 %51, label %bb107.i, label %bb125.i

bb107.i:                                          ; preds = %bb104.i
  %52 = tail call i32 @llvm.ctlz.i32(i32 %49, i1 true) #14, !range !5
  %shift.i = add nsw i32 %52, -5
  %53 = and i32 %shift.i, 31
  %_4.0.i59.i = shl i32 %49, %53
  %54 = sub nsw i32 %a_exponent.0.i, %shift.i
  br label %bb125.i

bb125.i:                                          ; preds = %bb118.i, %bb107.i, %bb104.i, %bb113.i
  %a_significand.1.i = phi i32 [ %_4.0.i59.i, %bb107.i ], [ %49, %bb104.i ], [ %56, %bb118.i ], [ %_4.0.i.i, %bb113.i ]
  %a_exponent.1.i = phi i32 [ %54, %bb107.i ], [ %a_exponent.0.i, %bb104.i ], [ %57, %bb118.i ], [ %a_exponent.0.i, %bb113.i ]
  %_229.i = icmp sgt i32 %a_exponent.1.i, 254
  br i1 %_229.i, label %bb126.i, label %bb129.i

bb118.i:                                          ; preds = %bb113.i
  %55 = and i32 %_4.0.i.i, 1
  %_5.0.i60.i = lshr i32 %_4.0.i.i, 1
  %56 = or i32 %55, %_5.0.i60.i
  %57 = add nsw i32 %a_exponent.0.i, 1
  br label %bb125.i

bb129.i:                                          ; preds = %bb125.i
  %_236.i = icmp slt i32 %a_exponent.1.i, 1
  br i1 %_236.i, label %bb130.i, label %bb140.i

bb126.i:                                          ; preds = %bb125.i
  %58 = or i32 %37, 2139095040
  %59 = bitcast i32 %58 to float
  br label %_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit

bb140.i:                                          ; preds = %bb130.i, %bb129.i
  %a_significand.2.i = phi i32 [ %68, %bb130.i ], [ %a_significand.1.i, %bb129.i ]
  %a_exponent.2.i = phi i32 [ 0, %bb130.i ], [ %a_exponent.1.i, %bb129.i ]
  %round_guard_sticky.i = and i32 %a_significand.2.i, 7
  %_5.0.i61.i = lshr i32 %a_significand.2.i, 3
  %60 = and i32 %_5.0.i61.i, 8388607
  %_5.0.i62.i = shl nuw nsw i32 %a_exponent.2.i, 23
  %61 = or i32 %60, %_5.0.i62.i
  %62 = or i32 %61, %37
  %_275.i = icmp ugt i32 %round_guard_sticky.i, 4
  br i1 %_275.i, label %bb150.thread.i, label %bb150.i

bb130.i:                                          ; preds = %bb129.i
  %_239.i = sub nsw i32 1, %a_exponent.1.i
  %63 = add nsw i32 %a_exponent.1.i, 31
  %64 = and i32 %63, 31
  %_5.0.i63.i = shl i32 %a_significand.1.i, %64
  %65 = icmp ne i32 %_5.0.i63.i, 0
  %66 = zext i1 %65 to i32
  %67 = and i32 %_239.i, 31
  %_5.0.i64.i = lshr i32 %a_significand.1.i, %67
  %68 = or i32 %_5.0.i64.i, %66
  br label %bb140.i

bb150.i:                                          ; preds = %bb140.i
  %69 = icmp eq i32 %round_guard_sticky.i, 4
  br i1 %69, label %bb151.i, label %bb155.i

bb150.thread.i:                                   ; preds = %bb140.i
  %_4.0.i65.i = add i32 %62, 1
  br label %bb155.i

bb151.i:                                          ; preds = %bb150.i
  %70 = and i32 %_5.0.i61.i, 1
  %_4.0.i66.i = add i32 %62, %70
  br label %bb155.i

bb155.i:                                          ; preds = %bb151.i, %bb150.thread.i, %bb150.i
  %result.1.i = phi i32 [ %_4.0.i66.i, %bb151.i ], [ %62, %bb150.i ], [ %_4.0.i65.i, %bb150.thread.i ]
  %71 = bitcast i32 %result.1.i to float
  br label %_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit

_ZN17compiler_builtins5float3add3add17h1813addaf62eae1eE.exit: ; preds = %bb20.i, %bb25.i, %bb38.i, %bb30.i, %bb51.i, %bb43.i, %bb45.i, %bb100.i, %bb126.i, %bb155.i
  %.3.i = phi float [ %71, %bb155.i ], [ %17, %bb20.i ], [ %20, %bb25.i ], [ %27, %bb45.i ], [ %b, %bb38.i ], [ %b, %bb43.i ], [ %a, %bb51.i ], [ %59, %bb126.i ], [ 0.000000e+00, %bb100.i ], [ %spec.select151.i, %bb30.i ]
  ret float %.3.i
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @__addsf3(float %a, float %b) unnamed_addr #1 {
start:
; call compiler_builtins::float::add::__addsf3
  %0 = tail call float @_ZN17compiler_builtins5float3add8__addsf317hb95467ebe387ff0cE(float %a, float %b)
  ret float %0
}

; compiler_builtins::float::add::__adddf3
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float3add8__adddf317h9c1cc12e52bbedbeE(double %a, double %b) unnamed_addr #1 {
start:
  %0 = bitcast double %a to i64
  %1 = bitcast double %b to i64
  %2 = and i64 %0, 9223372036854775807
  %3 = and i64 %1, 9223372036854775807
  %4 = add nsw i64 %2, -1
  %5 = icmp ugt i64 %4, 9218868437227405310
  %6 = add nsw i64 %3, -1
  %7 = icmp ugt i64 %6, 9218868437227405310
  %or.cond.i = select i1 %5, i1 true, i1 %7
  br i1 %or.cond.i, label %bb18.i, label %bb55.i

bb55.i:                                           ; preds = %bb51.i, %start
  %8 = icmp ugt i64 %3, %2
  %spec.select.i = select i1 %8, i64 %0, i64 %1
  %spec.select38.i = select i1 %8, i64 %1, i64 %0
  %9 = lshr i64 %spec.select38.i, 52
  %10 = trunc i64 %9 to i32
  %11 = and i32 %10, 2047
  %12 = lshr i64 %spec.select.i, 52
  %13 = trunc i64 %12 to i32
  %14 = and i32 %13, 2047
  %15 = and i64 %spec.select38.i, 4503599627370495
  %16 = and i64 %spec.select.i, 4503599627370495
  %17 = icmp eq i32 %11, 0
  br i1 %17, label %bb67.i, label %bb70.i

bb18.i:                                           ; preds = %start
  %18 = icmp ugt i64 %2, 9218868437227405312
  br i1 %18, label %bb20.i, label %bb23.i

bb23.i:                                           ; preds = %bb18.i
  %19 = icmp ugt i64 %3, 9218868437227405312
  br i1 %19, label %bb25.i, label %bb28.i

bb20.i:                                           ; preds = %bb18.i
  %20 = or i64 %2, 2251799813685248
  %21 = bitcast i64 %20 to double
  br label %_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit

bb28.i:                                           ; preds = %bb23.i
  %22 = icmp eq i64 %2, 9218868437227405312
  br i1 %22, label %bb30.i, label %bb38.i

bb25.i:                                           ; preds = %bb23.i
  %23 = or i64 %3, 2251799813685248
  %24 = bitcast i64 %23 to double
  br label %_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit

bb38.i:                                           ; preds = %bb28.i
  %25 = icmp eq i64 %3, 9218868437227405312
  br i1 %25, label %_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit, label %bb41.i

bb30.i:                                           ; preds = %bb28.i
  %26 = xor i64 %1, %0
  %27 = icmp eq i64 %26, -9223372036854775808
  %spec.select151.i = select i1 %27, double 0x7FF8000000000000, double %a
  br label %_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit

bb41.i:                                           ; preds = %bb38.i
  %28 = icmp eq i64 %2, 0
  %29 = icmp eq i64 %3, 0
  br i1 %28, label %bb43.i, label %bb51.i

bb51.i:                                           ; preds = %bb41.i
  br i1 %29, label %_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit, label %bb55.i

bb43.i:                                           ; preds = %bb41.i
  br i1 %29, label %bb45.i, label %_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit

bb45.i:                                           ; preds = %bb43.i
  %30 = and i64 %1, %0
  %31 = bitcast i64 %30 to double
  br label %_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit

bb67.i:                                           ; preds = %bb55.i
  %32 = tail call i64 @llvm.ctlz.i64(i64 %15, i1 false) #14, !range !6
  %33 = trunc i64 %32 to i32
  %34 = add nuw nsw i64 %32, 53
  %35 = sub nsw i32 12, %33
  %36 = and i64 %34, 63
  %_10.i.i = shl i64 %15, %36
  br label %bb70.i

bb70.i:                                           ; preds = %bb67.i, %bb55.i
  %a_significand.0.i = phi i64 [ %_10.i.i, %bb67.i ], [ %15, %bb55.i ]
  %a_exponent.0.i = phi i32 [ %35, %bb67.i ], [ %11, %bb55.i ]
  %37 = icmp eq i32 %14, 0
  br i1 %37, label %bb71.i, label %bb74.i

bb71.i:                                           ; preds = %bb70.i
  %38 = tail call i64 @llvm.ctlz.i64(i64 %16, i1 false) #14, !range !6
  %39 = trunc i64 %38 to i32
  %40 = add nuw nsw i64 %38, 53
  %41 = sub nsw i32 12, %39
  %42 = and i64 %40, 63
  %_10.i54.i = shl i64 %16, %42
  br label %bb74.i

bb74.i:                                           ; preds = %bb71.i, %bb70.i
  %b_significand.0.i = phi i64 [ %_10.i54.i, %bb71.i ], [ %16, %bb70.i ]
  %b_exponent.0.i = phi i32 [ %41, %bb71.i ], [ %14, %bb70.i ]
  %43 = and i64 %spec.select38.i, -9223372036854775808
  %44 = xor i64 %spec.select.i, %spec.select38.i
  %.not.i = icmp sgt i64 %44, -1
  %45 = shl i64 %a_significand.0.i, 3
  %_5.0.i55.i = or i64 %45, 36028797018963968
  %46 = shl i64 %b_significand.0.i, 3
  %_5.0.i56.i = or i64 %46, 36028797018963968
  %47 = sub nsw i32 %a_exponent.0.i, %b_exponent.0.i
  %.not152.i = icmp eq i32 %47, 0
  br i1 %.not152.i, label %bb99.i, label %bb86.i

bb99.i:                                           ; preds = %bb88.i, %bb86.i, %bb74.i
  %b_significand.1.i = phi i64 [ %55, %bb88.i ], [ %_5.0.i56.i, %bb74.i ], [ 1, %bb86.i ]
  br i1 %.not.i, label %bb113.i, label %bb100.i

bb86.i:                                           ; preds = %bb74.i
  %48 = icmp ult i32 %47, 64
  br i1 %48, label %bb88.i, label %bb99.i

bb88.i:                                           ; preds = %bb86.i
  %49 = sub nsw i32 0, %47
  %50 = and i32 %49, 63
  %51 = zext i32 %50 to i64
  %_5.0.i57.i = shl i64 %_5.0.i56.i, %51
  %52 = icmp ne i64 %_5.0.i57.i, 0
  %53 = zext i1 %52 to i64
  %54 = zext i32 %47 to i64
  %_5.0.i58.i = lshr i64 %_5.0.i56.i, %54
  %55 = or i64 %_5.0.i58.i, %53
  br label %bb99.i

bb113.i:                                          ; preds = %bb99.i
  %_4.0.i.i = add i64 %b_significand.1.i, %_5.0.i55.i
  %56 = and i64 %_4.0.i.i, 72057594037927936
  %.not153.i = icmp eq i64 %56, 0
  br i1 %.not153.i, label %bb125.i, label %bb118.i

bb100.i:                                          ; preds = %bb99.i
  %57 = sub i64 %_5.0.i55.i, %b_significand.1.i
  %58 = icmp eq i64 %57, 0
  br i1 %58, label %_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit, label %bb104.i

bb104.i:                                          ; preds = %bb100.i
  %59 = icmp ult i64 %57, 36028797018963968
  br i1 %59, label %bb107.i, label %bb125.i

bb107.i:                                          ; preds = %bb104.i
  %60 = tail call i64 @llvm.ctlz.i64(i64 %57, i1 true) #14, !range !6
  %61 = trunc i64 %60 to i32
  %shift.i = add nsw i32 %61, -8
  %62 = and i32 %shift.i, 63
  %63 = zext i32 %62 to i64
  %_4.0.i59.i = shl i64 %57, %63
  %64 = sub nsw i32 %a_exponent.0.i, %shift.i
  br label %bb125.i

bb125.i:                                          ; preds = %bb118.i, %bb107.i, %bb104.i, %bb113.i
  %a_significand.1.i = phi i64 [ %_4.0.i59.i, %bb107.i ], [ %57, %bb104.i ], [ %66, %bb118.i ], [ %_4.0.i.i, %bb113.i ]
  %a_exponent.1.i = phi i32 [ %64, %bb107.i ], [ %a_exponent.0.i, %bb104.i ], [ %67, %bb118.i ], [ %a_exponent.0.i, %bb113.i ]
  %_229.i = icmp sgt i32 %a_exponent.1.i, 2046
  br i1 %_229.i, label %bb126.i, label %bb129.i

bb118.i:                                          ; preds = %bb113.i
  %65 = and i64 %_4.0.i.i, 1
  %_5.0.i60.i = lshr i64 %_4.0.i.i, 1
  %66 = or i64 %65, %_5.0.i60.i
  %67 = add nsw i32 %a_exponent.0.i, 1
  br label %bb125.i

bb129.i:                                          ; preds = %bb125.i
  %_236.i = icmp slt i32 %a_exponent.1.i, 1
  br i1 %_236.i, label %bb130.i, label %bb140.i

bb126.i:                                          ; preds = %bb125.i
  %68 = or i64 %43, 9218868437227405312
  %69 = bitcast i64 %68 to double
  br label %_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit

bb140.i:                                          ; preds = %bb130.i, %bb129.i
  %a_significand.2.i = phi i64 [ %82, %bb130.i ], [ %a_significand.1.i, %bb129.i ]
  %a_exponent.2.i = phi i32 [ 0, %bb130.i ], [ %a_exponent.1.i, %bb129.i ]
  %70 = trunc i64 %a_significand.2.i to i32
  %round_guard_sticky.i = and i32 %70, 7
  %_5.0.i61.i = lshr i64 %a_significand.2.i, 3
  %71 = and i64 %_5.0.i61.i, 4503599627370495
  %72 = zext i32 %a_exponent.2.i to i64
  %_5.0.i62.i = shl nuw nsw i64 %72, 52
  %73 = or i64 %_5.0.i62.i, %71
  %74 = or i64 %73, %43
  %_275.i = icmp ugt i32 %round_guard_sticky.i, 4
  br i1 %_275.i, label %bb150.thread.i, label %bb150.i

bb130.i:                                          ; preds = %bb129.i
  %_239.i = sub nsw i32 1, %a_exponent.1.i
  %75 = add nsw i32 %a_exponent.1.i, 63
  %76 = and i32 %75, 63
  %77 = zext i32 %76 to i64
  %_5.0.i63.i = shl i64 %a_significand.1.i, %77
  %78 = icmp ne i64 %_5.0.i63.i, 0
  %79 = zext i1 %78 to i64
  %80 = and i32 %_239.i, 63
  %81 = zext i32 %80 to i64
  %_5.0.i64.i = lshr i64 %a_significand.1.i, %81
  %82 = or i64 %_5.0.i64.i, %79
  br label %bb140.i

bb150.i:                                          ; preds = %bb140.i
  %83 = icmp eq i32 %round_guard_sticky.i, 4
  br i1 %83, label %bb151.i, label %bb155.i

bb150.thread.i:                                   ; preds = %bb140.i
  %_4.0.i65.i = add i64 %74, 1
  br label %bb155.i

bb151.i:                                          ; preds = %bb150.i
  %84 = and i64 %_5.0.i61.i, 1
  %_4.0.i66.i = add i64 %74, %84
  br label %bb155.i

bb155.i:                                          ; preds = %bb151.i, %bb150.thread.i, %bb150.i
  %result.1.i = phi i64 [ %_4.0.i66.i, %bb151.i ], [ %74, %bb150.i ], [ %_4.0.i65.i, %bb150.thread.i ]
  %85 = bitcast i64 %result.1.i to double
  br label %_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit

_ZN17compiler_builtins5float3add3add17h46ad5aedaaa58a34E.exit: ; preds = %bb20.i, %bb25.i, %bb38.i, %bb30.i, %bb51.i, %bb43.i, %bb45.i, %bb100.i, %bb126.i, %bb155.i
  %.3.i = phi double [ %85, %bb155.i ], [ %21, %bb20.i ], [ %24, %bb25.i ], [ %31, %bb45.i ], [ %b, %bb38.i ], [ %b, %bb43.i ], [ %a, %bb51.i ], [ %69, %bb126.i ], [ 0.000000e+00, %bb100.i ], [ %spec.select151.i, %bb30.i ]
  ret double %.3.i
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @__adddf3(double %a, double %b) unnamed_addr #1 {
start:
; call compiler_builtins::float::add::__adddf3
  %0 = tail call double @_ZN17compiler_builtins5float3add8__adddf317h9c1cc12e52bbedbeE(double %a, double %b)
  ret double %0
}

; compiler_builtins::float::cmp::__gesf2
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins5float3cmp7__gesf217h1eb6d709339225f7E(float %a, float %b) unnamed_addr #2 {
start:
  %0 = bitcast float %a to i32
  %1 = bitcast float %b to i32
  %2 = and i32 %0, 2147483647
  %3 = and i32 %1, 2147483647
  %4 = icmp ugt i32 %2, 2139095040
  %5 = icmp ugt i32 %3, 2139095040
  %or.cond.i = select i1 %4, i1 true, i1 %5
  br i1 %or.cond.i, label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %6 = or i32 %3, %2
  %7 = icmp eq i32 %6, 0
  br i1 %7, label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit, label %bb16.i

bb16.i:                                           ; preds = %bb12.i
  %8 = and i32 %1, %0
  %9 = icmp sgt i32 %8, -1
  br i1 %9, label %bb21.i, label %bb30.i

bb30.i:                                           ; preds = %bb16.i
  %10 = icmp sgt i32 %0, %1
  br i1 %10, label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit, label %bb33.i

bb21.i:                                           ; preds = %bb16.i
  %11 = icmp slt i32 %0, %1
  br i1 %11, label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit, label %bb24.i

bb24.i:                                           ; preds = %bb21.i
  %12 = icmp ne i32 %0, %1
  %spec.select = zext i1 %12 to i32
  br label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit

bb33.i:                                           ; preds = %bb30.i
  %13 = icmp ne i32 %0, %1
  %spec.select6 = zext i1 %13 to i32
  br label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit

_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit: ; preds = %bb33.i, %bb24.i, %bb30.i, %bb21.i, %start, %bb12.i
  %.0.i = phi i32 [ 0, %bb12.i ], [ -1, %start ], [ -1, %bb21.i ], [ -1, %bb30.i ], [ %spec.select, %bb24.i ], [ %spec.select6, %bb33.i ]
  ret i32 %.0.i
}

; compiler_builtins::float::cmp::__unordsf2
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins5float3cmp10__unordsf217h398a701a49d7de0bE(float %a, float %b) unnamed_addr #2 {
start:
  %0 = bitcast float %a to i32
  %1 = bitcast float %b to i32
  %2 = and i32 %0, 2147483647
  %3 = and i32 %1, 2147483647
  %4 = icmp ugt i32 %2, 2139095040
  %5 = icmp ugt i32 %3, 2139095040
  %.0.i = select i1 %4, i1 true, i1 %5
  %6 = zext i1 %.0.i to i32
  ret i32 %6
}

; compiler_builtins::float::cmp::__eqsf2
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins5float3cmp7__eqsf217he7a3f748735ac2a3E(float %a, float %b) unnamed_addr #2 {
start:
  %0 = bitcast float %a to i32
  %1 = bitcast float %b to i32
  %2 = and i32 %0, 2147483647
  %3 = and i32 %1, 2147483647
  %4 = icmp ugt i32 %2, 2139095040
  %5 = icmp ugt i32 %3, 2139095040
  %or.cond.i = select i1 %4, i1 true, i1 %5
  br i1 %or.cond.i, label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %6 = or i32 %3, %2
  %7 = icmp eq i32 %6, 0
  br i1 %7, label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit, label %bb16.i

bb16.i:                                           ; preds = %bb12.i
  %8 = and i32 %1, %0
  %9 = icmp sgt i32 %8, -1
  br i1 %9, label %bb21.i, label %bb30.i

bb30.i:                                           ; preds = %bb16.i
  %10 = icmp sgt i32 %0, %1
  br i1 %10, label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit, label %bb33.i

bb21.i:                                           ; preds = %bb16.i
  %11 = icmp slt i32 %0, %1
  br i1 %11, label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit, label %bb24.i

bb24.i:                                           ; preds = %bb21.i
  %12 = icmp ne i32 %0, %1
  %spec.select = zext i1 %12 to i32
  br label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit

bb33.i:                                           ; preds = %bb30.i
  %13 = icmp ne i32 %0, %1
  %spec.select9 = zext i1 %13 to i32
  br label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit

_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit: ; preds = %bb33.i, %bb24.i, %bb30.i, %bb21.i, %start, %bb12.i
  %.0.i = phi i32 [ 0, %bb12.i ], [ 1, %start ], [ -1, %bb21.i ], [ -1, %bb30.i ], [ %spec.select, %bb24.i ], [ %spec.select9, %bb33.i ]
  ret i32 %.0.i
}

; compiler_builtins::float::cmp::__gedf2
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins5float3cmp7__gedf217h2c6f81c0073f269cE(double %a, double %b) unnamed_addr #2 {
start:
  %0 = bitcast double %a to i64
  %1 = bitcast double %b to i64
  %2 = and i64 %0, 9223372036854775807
  %3 = and i64 %1, 9223372036854775807
  %4 = icmp ugt i64 %2, 9218868437227405312
  %5 = icmp ugt i64 %3, 9218868437227405312
  %or.cond.i = select i1 %4, i1 true, i1 %5
  br i1 %or.cond.i, label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %6 = or i64 %3, %2
  %7 = icmp eq i64 %6, 0
  br i1 %7, label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit, label %bb16.i

bb16.i:                                           ; preds = %bb12.i
  %8 = and i64 %1, %0
  %9 = icmp sgt i64 %8, -1
  br i1 %9, label %bb21.i, label %bb30.i

bb30.i:                                           ; preds = %bb16.i
  %10 = icmp sgt i64 %0, %1
  br i1 %10, label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit, label %bb33.i

bb21.i:                                           ; preds = %bb16.i
  %11 = icmp slt i64 %0, %1
  br i1 %11, label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit, label %bb24.i

bb24.i:                                           ; preds = %bb21.i
  %12 = icmp ne i64 %0, %1
  %spec.select = zext i1 %12 to i32
  br label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit

bb33.i:                                           ; preds = %bb30.i
  %13 = icmp ne i64 %0, %1
  %spec.select6 = zext i1 %13 to i32
  br label %_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit

_ZN17compiler_builtins5float3cmp6Result9to_ge_abi17h60e0b44633e51fb8E.exit: ; preds = %bb33.i, %bb24.i, %bb30.i, %bb21.i, %start, %bb12.i
  %.0.i = phi i32 [ 0, %bb12.i ], [ -1, %start ], [ -1, %bb21.i ], [ -1, %bb30.i ], [ %spec.select, %bb24.i ], [ %spec.select6, %bb33.i ]
  ret i32 %.0.i
}

; compiler_builtins::float::cmp::__unorddf2
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins5float3cmp10__unorddf217hd63f8d61cd349cf9E(double %a, double %b) unnamed_addr #2 {
start:
  %0 = bitcast double %a to i64
  %1 = bitcast double %b to i64
  %2 = and i64 %0, 9223372036854775807
  %3 = and i64 %1, 9223372036854775807
  %4 = icmp ugt i64 %2, 9218868437227405312
  %5 = icmp ugt i64 %3, 9218868437227405312
  %.0.i = select i1 %4, i1 true, i1 %5
  %6 = zext i1 %.0.i to i32
  ret i32 %6
}

; compiler_builtins::float::cmp::__eqdf2
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins5float3cmp7__eqdf217hfe55385038c8e487E(double %a, double %b) unnamed_addr #2 {
start:
  %0 = bitcast double %a to i64
  %1 = bitcast double %b to i64
  %2 = and i64 %0, 9223372036854775807
  %3 = and i64 %1, 9223372036854775807
  %4 = icmp ugt i64 %2, 9218868437227405312
  %5 = icmp ugt i64 %3, 9218868437227405312
  %or.cond.i = select i1 %4, i1 true, i1 %5
  br i1 %or.cond.i, label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %6 = or i64 %3, %2
  %7 = icmp eq i64 %6, 0
  br i1 %7, label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit, label %bb16.i

bb16.i:                                           ; preds = %bb12.i
  %8 = and i64 %1, %0
  %9 = icmp sgt i64 %8, -1
  br i1 %9, label %bb21.i, label %bb30.i

bb30.i:                                           ; preds = %bb16.i
  %10 = icmp sgt i64 %0, %1
  br i1 %10, label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit, label %bb33.i

bb21.i:                                           ; preds = %bb16.i
  %11 = icmp slt i64 %0, %1
  br i1 %11, label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit, label %bb24.i

bb24.i:                                           ; preds = %bb21.i
  %12 = icmp ne i64 %0, %1
  %spec.select = zext i1 %12 to i32
  br label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit

bb33.i:                                           ; preds = %bb30.i
  %13 = icmp ne i64 %0, %1
  %spec.select9 = zext i1 %13 to i32
  br label %_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit

_ZN17compiler_builtins5float3cmp6Result9to_le_abi17h823b94fab369a37dE.exit: ; preds = %bb33.i, %bb24.i, %bb30.i, %bb21.i, %start, %bb12.i
  %.0.i = phi i32 [ 0, %bb12.i ], [ 1, %start ], [ -1, %bb21.i ], [ -1, %bb30.i ], [ %spec.select, %bb24.i ], [ %spec.select9, %bb33.i ]
  ret i32 %.0.i
}

; compiler_builtins::float::conv::__floatsisf
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float4conv11__floatsisf17hc81f19c8035af6c6E(i32 %i) unnamed_addr #1 {
start:
  %0 = icmp eq i32 %i, 0
  br i1 %0, label %_ZN17compiler_builtins5float4conv12int_to_float17hb300739ef829a256E.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = tail call i32 @llvm.abs.i32(i32 %i, i1 false) #14
  %2 = tail call i32 @llvm.ctlz.i32(i32 %1, i1 false) #14, !range !5
  %i_sd.i = sub nuw nsw i32 32, %2
  %3 = sub nsw i32 31, %2
  %_36.i = icmp ugt i32 %1, 16777215
  br i1 %_36.i, label %bb15.i, label %bb16.i

bb16.i:                                           ; preds = %bb3.i
  %_92.i = add nuw nsw i32 %2, 24
  %_4.i.i.i = and i32 %_92.i, 31
  %4 = shl i32 %1, %_4.i.i.i
  br label %bb43.i

bb15.i:                                           ; preds = %bb3.i
  switch i32 %i_sd.i, label %bb21.i [
    i32 25, label %bb17.i
    i32 26, label %bb29.i
  ]

bb17.i:                                           ; preds = %bb15.i
  %_5.0.i.i = shl i32 %1, 1
  br label %bb29.i

bb29.i:                                           ; preds = %bb21.i, %bb17.i, %bb15.i
  %_41.0.i = phi i32 [ %12, %bb21.i ], [ %_5.0.i.i, %bb17.i ], [ %1, %bb15.i ]
  %5 = lshr i32 %_41.0.i, 2
  %.lobit.i = and i32 %5, 1
  %6 = or i32 %.lobit.i, %_41.0.i
  %_4.0.i.i = add i32 %6, 1
  %7 = and i32 %_4.0.i.i, 67108864
  %.not.i = icmp eq i32 %7, 0
  %spec.select.v.i = select i1 %.not.i, i32 2, i32 3
  %spec.select.i = lshr i32 %_4.0.i.i, %spec.select.v.i
  %spec.select32.i = select i1 %.not.i, i32 %3, i32 %i_sd.i
  br label %bb43.i

bb21.i:                                           ; preds = %bb15.i
  %_52.i = sub nsw i32 6, %2
  %8 = and i32 %_52.i, 31
  %_5.0.i7.i = lshr i32 %1, %8
  %_61.i = add nuw nsw i32 %2, 26
  %_4.i.i8.i = and i32 %_61.i, 31
  %9 = shl i32 %1, %_4.i.i8.i
  %10 = icmp ne i32 %9, 0
  %11 = zext i1 %10 to i32
  %12 = or i32 %_5.0.i7.i, %11
  br label %bb29.i

bb43.i:                                           ; preds = %bb29.i, %bb16.i
  %_35.0.i = phi i32 [ %4, %bb16.i ], [ %spec.select.i, %bb29.i ]
  %exp.1.i = phi i32 [ %3, %bb16.i ], [ %spec.select32.i, %bb29.i ]
  %13 = and i32 %i, -2147483648
  %_97.i = shl nsw i32 %exp.1.i, 23
  %_11.i.i = add nsw i32 %_97.i, 1065353216
  %_10.i.i = and i32 %_11.i.i, 2139095040
  %_13.i.i = and i32 %_35.0.i, 8388607
  %_5.i.i = or i32 %_13.i.i, %13
  %_4.i.i = or i32 %_5.i.i, %_10.i.i
  %14 = bitcast i32 %_4.i.i to float
  br label %_ZN17compiler_builtins5float4conv12int_to_float17hb300739ef829a256E.exit

_ZN17compiler_builtins5float4conv12int_to_float17hb300739ef829a256E.exit: ; preds = %start, %bb43.i
  %.0.i = phi float [ %14, %bb43.i ], [ 0.000000e+00, %start ]
  ret float %.0.i
}

; compiler_builtins::float::conv::__floatsidf
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float4conv11__floatsidf17hd467774adecb1e68E(i32 %i) unnamed_addr #1 {
start:
  %0 = icmp eq i32 %i, 0
  br i1 %0, label %_ZN17compiler_builtins5float4conv12int_to_float17h87967d5baddeeb78E.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = tail call i32 @llvm.abs.i32(i32 %i, i1 false) #14
  %2 = tail call i32 @llvm.ctlz.i32(i32 %1, i1 false) #14, !range !5
  %_26.i = sub nuw nsw i32 1054, %2
  %3 = zext i32 %_26.i to i64
  %4 = zext i32 %1 to i64
  %_31.i = add nuw nsw i32 %2, 21
  %5 = zext i32 %_31.i to i64
  %_5.0.i.i = shl i64 %4, %5
  %6 = and i32 %i, -2147483648
  %7 = zext i32 %6 to i64
  %8 = shl nuw i64 %7, 32
  %_11.i.i = shl nuw nsw i64 %3, 52
  %_5.i.i = or i64 %_11.i.i, %8
  %_13.i.i = and i64 %_5.0.i.i, 4503599627370495
  %_4.i.i = or i64 %_5.i.i, %_13.i.i
  %9 = bitcast i64 %_4.i.i to double
  br label %_ZN17compiler_builtins5float4conv12int_to_float17h87967d5baddeeb78E.exit

_ZN17compiler_builtins5float4conv12int_to_float17h87967d5baddeeb78E.exit: ; preds = %start, %bb3.i
  %.0.i = phi double [ %9, %bb3.i ], [ 0.000000e+00, %start ]
  ret double %.0.i
}

; compiler_builtins::float::conv::__floatdisf
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float4conv11__floatdisf17hb3723a08e7423bd0E(i64 %i) unnamed_addr #2 {
start:
  %0 = sitofp i64 %i to float
  ret float %0
}

; compiler_builtins::float::conv::__floatdidf
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float4conv11__floatdidf17hc017db6e67256b31E(i64 %i) unnamed_addr #2 {
start:
  %0 = sitofp i64 %i to double
  ret double %0
}

; compiler_builtins::float::conv::__floattisf
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float4conv11__floattisf17hd3998757712d07f4E(i128 %i) unnamed_addr #1 {
start:
  %0 = icmp eq i128 %i, 0
  br i1 %0, label %_ZN17compiler_builtins5float4conv12int_to_float17hdf5d98e23a36fcebE.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = tail call i128 @llvm.abs.i128(i128 %i, i1 false) #14
  %2 = tail call i128 @llvm.ctlz.i128(i128 %1, i1 false) #14, !range !7
  %3 = trunc i128 %2 to i32
  %i_sd.i = sub nuw nsw i32 128, %3
  %4 = sub nsw i32 127, %3
  %_36.i = icmp ult i32 %3, 104
  br i1 %_36.i, label %bb15.i, label %bb16.i

bb16.i:                                           ; preds = %bb3.i
  %_92.i = add nuw nsw i32 %3, 24
  %_5.i.i.i = and i32 %_92.i, 127
  %_4.i.i.i = zext i32 %_5.i.i.i to i128
  %5 = shl i128 %1, %_4.i.i.i
  %extract.t.i = trunc i128 %5 to i32
  br label %bb43.i

bb15.i:                                           ; preds = %bb3.i
  switch i32 %i_sd.i, label %bb21.i [
    i32 25, label %bb17.i
    i32 26, label %bb29.i
  ]

bb17.i:                                           ; preds = %bb15.i
  %_5.0.i.i = shl i128 %1, 1
  br label %bb29.i

bb29.i:                                           ; preds = %bb21.i, %bb17.i, %bb15.i
  %_41.0.i = phi i128 [ %14, %bb21.i ], [ %_5.0.i.i, %bb17.i ], [ %1, %bb15.i ]
  %6 = lshr i128 %_41.0.i, 2
  %.lobit.i = and i128 %6, 1
  %7 = or i128 %.lobit.i, %_41.0.i
  %_4.0.i.i = add i128 %7, 1
  %_4.0.i6.i = lshr i128 %_4.0.i.i, 2
  %8 = and i128 %_4.0.i.i, 67108864
  %.not.i = icmp eq i128 %8, 0
  %extract.t34.i = trunc i128 %_4.0.i6.i to i32
  br i1 %.not.i, label %bb43.i, label %bb39.i

bb21.i:                                           ; preds = %bb15.i
  %_52.i = sub nsw i32 102, %3
  %9 = and i32 %_52.i, 127
  %10 = zext i32 %9 to i128
  %_5.0.i7.i = lshr i128 %1, %10
  %_61.i = add nuw nsw i32 %3, 26
  %_5.i.i8.i = and i32 %_61.i, 127
  %_4.i.i9.i = zext i32 %_5.i.i8.i to i128
  %11 = shl i128 %1, %_4.i.i9.i
  %12 = icmp ne i128 %11, 0
  %13 = zext i1 %12 to i128
  %14 = or i128 %_5.0.i7.i, %13
  br label %bb29.i

bb39.i:                                           ; preds = %bb29.i
  %_4.0.i10.i = lshr i128 %_4.0.i.i, 3
  %extract.t33.i = trunc i128 %_4.0.i10.i to i32
  br label %bb43.i

bb43.i:                                           ; preds = %bb39.i, %bb29.i, %bb16.i
  %_35.0.off0.i = phi i32 [ %extract.t.i, %bb16.i ], [ %extract.t33.i, %bb39.i ], [ %extract.t34.i, %bb29.i ]
  %exp.1.i = phi i32 [ %4, %bb16.i ], [ %i_sd.i, %bb39.i ], [ %4, %bb29.i ]
  %15 = lshr i128 %i, 96
  %16 = trunc i128 %15 to i32
  %17 = and i32 %16, -2147483648
  %_97.i = shl nsw i32 %exp.1.i, 23
  %_11.i.i = add i32 %_97.i, 1065353216
  %_10.i.i = and i32 %_11.i.i, 2139095040
  %_13.i.i = and i32 %_35.0.off0.i, 8388607
  %_5.i.i = or i32 %_13.i.i, %17
  %_4.i.i = or i32 %_5.i.i, %_10.i.i
  %18 = bitcast i32 %_4.i.i to float
  br label %_ZN17compiler_builtins5float4conv12int_to_float17hdf5d98e23a36fcebE.exit

_ZN17compiler_builtins5float4conv12int_to_float17hdf5d98e23a36fcebE.exit: ; preds = %start, %bb43.i
  %.0.i = phi float [ %18, %bb43.i ], [ 0.000000e+00, %start ]
  ret float %.0.i
}

; compiler_builtins::float::conv::__floattidf
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float4conv11__floattidf17h7ca077a44af40196E(i128 %i) unnamed_addr #1 {
start:
  %0 = icmp eq i128 %i, 0
  br i1 %0, label %_ZN17compiler_builtins5float4conv12int_to_float17hdea66a48f31232ccE.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = tail call i128 @llvm.abs.i128(i128 %i, i1 false) #14
  %2 = tail call i128 @llvm.ctlz.i128(i128 %1, i1 false) #14, !range !7
  %3 = trunc i128 %2 to i32
  %i_sd.i = sub nuw nsw i32 128, %3
  %4 = sub nsw i32 127, %3
  %_36.i = icmp ult i32 %3, 75
  br i1 %_36.i, label %bb15.i, label %bb16.i

bb16.i:                                           ; preds = %bb3.i
  %_92.i = add nuw nsw i32 %3, 53
  %_5.i.i.i = and i32 %_92.i, 127
  %_4.i.i.i = zext i32 %_5.i.i.i to i128
  %5 = shl i128 %1, %_4.i.i.i
  %extract.t.i = trunc i128 %5 to i64
  br label %bb43.i

bb15.i:                                           ; preds = %bb3.i
  switch i32 %i_sd.i, label %bb21.i [
    i32 54, label %bb17.i
    i32 55, label %bb29.i
  ]

bb17.i:                                           ; preds = %bb15.i
  %_5.0.i.i = shl i128 %1, 1
  br label %bb29.i

bb29.i:                                           ; preds = %bb21.i, %bb17.i, %bb15.i
  %_41.0.i = phi i128 [ %14, %bb21.i ], [ %_5.0.i.i, %bb17.i ], [ %1, %bb15.i ]
  %6 = lshr i128 %_41.0.i, 2
  %.lobit.i = and i128 %6, 1
  %7 = or i128 %.lobit.i, %_41.0.i
  %_4.0.i.i = add i128 %7, 1
  %_4.0.i6.i = lshr i128 %_4.0.i.i, 2
  %8 = and i128 %_4.0.i.i, 36028797018963968
  %.not.i = icmp eq i128 %8, 0
  %extract.t34.i = trunc i128 %_4.0.i6.i to i64
  br i1 %.not.i, label %bb43.i, label %bb39.i

bb21.i:                                           ; preds = %bb15.i
  %_52.i = sub nsw i32 73, %3
  %9 = and i32 %_52.i, 127
  %10 = zext i32 %9 to i128
  %_5.0.i7.i = lshr i128 %1, %10
  %_61.i = add nuw nsw i32 %3, 55
  %_5.i.i8.i = and i32 %_61.i, 127
  %_4.i.i9.i = zext i32 %_5.i.i8.i to i128
  %11 = shl i128 %1, %_4.i.i9.i
  %12 = icmp ne i128 %11, 0
  %13 = zext i1 %12 to i128
  %14 = or i128 %_5.0.i7.i, %13
  br label %bb29.i

bb39.i:                                           ; preds = %bb29.i
  %_4.0.i10.i = lshr i128 %_4.0.i.i, 3
  %extract.t33.i = trunc i128 %_4.0.i10.i to i64
  br label %bb43.i

bb43.i:                                           ; preds = %bb39.i, %bb29.i, %bb16.i
  %_35.0.off0.i = phi i64 [ %extract.t.i, %bb16.i ], [ %extract.t33.i, %bb39.i ], [ %extract.t34.i, %bb29.i ]
  %exp.1.i = phi i32 [ %4, %bb16.i ], [ %i_sd.i, %bb39.i ], [ %4, %bb29.i ]
  %_97.i = add nsw i32 %exp.1.i, 1023
  %15 = zext i32 %_97.i to i64
  %16 = lshr i128 %i, 64
  %17 = trunc i128 %16 to i64
  %18 = and i64 %17, -9223372036854775808
  %_11.i.i = shl nuw nsw i64 %15, 52
  %_5.i.i = or i64 %_11.i.i, %18
  %_13.i.i = and i64 %_35.0.off0.i, 4503599627370495
  %_4.i.i = or i64 %_5.i.i, %_13.i.i
  %19 = bitcast i64 %_4.i.i to double
  br label %_ZN17compiler_builtins5float4conv12int_to_float17hdea66a48f31232ccE.exit

_ZN17compiler_builtins5float4conv12int_to_float17hdea66a48f31232ccE.exit: ; preds = %start, %bb43.i
  %.0.i = phi double [ %19, %bb43.i ], [ 0.000000e+00, %start ]
  ret double %.0.i
}

; compiler_builtins::float::conv::__floatunsisf
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float4conv13__floatunsisf17hd59c5bbeec77e049E(i32 %i) unnamed_addr #1 {
start:
  %0 = icmp eq i32 %i, 0
  br i1 %0, label %_ZN17compiler_builtins5float4conv12int_to_float17h4d213dff49257ff8E.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = tail call i32 @llvm.ctlz.i32(i32 %i, i1 true) #14, !range !5
  %i_sd.i = sub nuw nsw i32 32, %1
  %2 = xor i32 %1, 31
  %_36.i = icmp ugt i32 %i, 16777215
  br i1 %_36.i, label %bb15.i, label %bb16.i

bb16.i:                                           ; preds = %bb3.i
  %_92.i = add nuw nsw i32 %1, 24
  %_4.i.i.i = and i32 %_92.i, 31
  %3 = shl i32 %i, %_4.i.i.i
  br label %bb43.i

bb15.i:                                           ; preds = %bb3.i
  switch i32 %i_sd.i, label %bb21.i [
    i32 25, label %bb17.i
    i32 26, label %bb29.i
  ]

bb17.i:                                           ; preds = %bb15.i
  %_5.0.i.i = shl i32 %i, 1
  br label %bb29.i

bb29.i:                                           ; preds = %bb21.i, %bb17.i, %bb15.i
  %_41.0.i = phi i32 [ %11, %bb21.i ], [ %_5.0.i.i, %bb17.i ], [ %i, %bb15.i ]
  %4 = lshr i32 %_41.0.i, 2
  %.lobit.i = and i32 %4, 1
  %5 = or i32 %.lobit.i, %_41.0.i
  %_4.0.i.i = add i32 %5, 1
  %6 = and i32 %_4.0.i.i, 67108864
  %.not.i = icmp eq i32 %6, 0
  %spec.select.v.i = select i1 %.not.i, i32 2, i32 3
  %spec.select.i = lshr i32 %_4.0.i.i, %spec.select.v.i
  %spec.select32.i = select i1 %.not.i, i32 %2, i32 %i_sd.i
  br label %bb43.i

bb21.i:                                           ; preds = %bb15.i
  %_52.i = sub nsw i32 6, %1
  %7 = and i32 %_52.i, 31
  %_5.0.i7.i = lshr i32 %i, %7
  %_61.i = add nuw nsw i32 %1, 26
  %_4.i.i8.i = and i32 %_61.i, 31
  %8 = shl i32 %i, %_4.i.i8.i
  %9 = icmp ne i32 %8, 0
  %10 = zext i1 %9 to i32
  %11 = or i32 %_5.0.i7.i, %10
  br label %bb29.i

bb43.i:                                           ; preds = %bb29.i, %bb16.i
  %_35.0.i = phi i32 [ %3, %bb16.i ], [ %spec.select.i, %bb29.i ]
  %exp.1.i = phi i32 [ %2, %bb16.i ], [ %spec.select32.i, %bb29.i ]
  %_97.i = shl nsw i32 %exp.1.i, 23
  %_11.i.i = add nsw i32 %_97.i, 1065353216
  %_10.i.i = and i32 %_11.i.i, 2139095040
  %_13.i.i = and i32 %_35.0.i, 8388607
  %_4.i.i = or i32 %_10.i.i, %_13.i.i
  %12 = bitcast i32 %_4.i.i to float
  br label %_ZN17compiler_builtins5float4conv12int_to_float17h4d213dff49257ff8E.exit

_ZN17compiler_builtins5float4conv12int_to_float17h4d213dff49257ff8E.exit: ; preds = %start, %bb43.i
  %.0.i = phi float [ %12, %bb43.i ], [ 0.000000e+00, %start ]
  ret float %.0.i
}

; compiler_builtins::float::conv::__floatunsidf
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float4conv13__floatunsidf17h52495b14b26fcd57E(i32 %i) unnamed_addr #1 {
start:
  %0 = icmp eq i32 %i, 0
  br i1 %0, label %_ZN17compiler_builtins5float4conv12int_to_float17h546d4abc3c9517d9E.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = tail call i32 @llvm.ctlz.i32(i32 %i, i1 true) #14, !range !5
  %_26.i = sub nuw nsw i32 1054, %1
  %2 = zext i32 %_26.i to i64
  %3 = zext i32 %i to i64
  %_31.i = add nuw nsw i32 %1, 21
  %4 = zext i32 %_31.i to i64
  %_5.0.i.i = shl i64 %3, %4
  %_11.i.i = shl nuw nsw i64 %2, 52
  %_13.i.i = and i64 %_5.0.i.i, 4503599627370494
  %_4.i.i = or i64 %_13.i.i, %_11.i.i
  %5 = bitcast i64 %_4.i.i to double
  br label %_ZN17compiler_builtins5float4conv12int_to_float17h546d4abc3c9517d9E.exit

_ZN17compiler_builtins5float4conv12int_to_float17h546d4abc3c9517d9E.exit: ; preds = %start, %bb3.i
  %.0.i = phi double [ %5, %bb3.i ], [ 0.000000e+00, %start ]
  ret double %.0.i
}

; compiler_builtins::float::conv::__floatundisf
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float4conv13__floatundisf17h8304f71c44b2b091E(i64 %i) unnamed_addr #1 {
start:
  %0 = icmp eq i64 %i, 0
  br i1 %0, label %_ZN17compiler_builtins5float4conv12int_to_float17hffc57fc44d1764aaE.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = tail call i64 @llvm.ctlz.i64(i64 %i, i1 true) #14, !range !6
  %2 = trunc i64 %1 to i32
  %i_sd.i = sub nuw nsw i32 64, %2
  %3 = xor i32 %2, 63
  %_36.i = icmp ult i32 %2, 40
  br i1 %_36.i, label %bb15.i, label %bb16.i

bb16.i:                                           ; preds = %bb3.i
  %_92.i = add nuw nsw i64 %1, 24
  %_5.i.i.i = and i64 %_92.i, 63
  %4 = shl i64 %i, %_5.i.i.i
  br label %bb43.i

bb15.i:                                           ; preds = %bb3.i
  switch i32 %i_sd.i, label %bb21.i [
    i32 25, label %bb17.i
    i32 26, label %bb29.i
  ]

bb17.i:                                           ; preds = %bb15.i
  %_5.0.i.i = shl i64 %i, 1
  br label %bb29.i

bb29.i:                                           ; preds = %bb21.i, %bb17.i, %bb15.i
  %_41.0.i = phi i64 [ %12, %bb21.i ], [ %_5.0.i.i, %bb17.i ], [ %i, %bb15.i ]
  %5 = lshr i64 %_41.0.i, 2
  %.lobit.i = and i64 %5, 1
  %6 = or i64 %.lobit.i, %_41.0.i
  %_4.0.i.i = add i64 %6, 1
  %7 = and i64 %_4.0.i.i, 67108864
  %.not.i = icmp eq i64 %7, 0
  %spec.select.v.i = select i1 %.not.i, i64 2, i64 3
  %spec.select.i = lshr i64 %_4.0.i.i, %spec.select.v.i
  %spec.select33.i = select i1 %.not.i, i32 %3, i32 %i_sd.i
  br label %bb43.i

bb21.i:                                           ; preds = %bb15.i
  %_52.i = sub nsw i64 38, %1
  %8 = and i64 %_52.i, 63
  %_5.0.i7.i = lshr i64 %i, %8
  %_61.i = add nuw nsw i64 %1, 26
  %_5.i.i8.i = and i64 %_61.i, 63
  %9 = shl i64 %i, %_5.i.i8.i
  %10 = icmp ne i64 %9, 0
  %11 = zext i1 %10 to i64
  %12 = or i64 %_5.0.i7.i, %11
  br label %bb29.i

bb43.i:                                           ; preds = %bb29.i, %bb16.i
  %_35.0.i = phi i64 [ %4, %bb16.i ], [ %spec.select.i, %bb29.i ]
  %exp.1.i = phi i32 [ %3, %bb16.i ], [ %spec.select33.i, %bb29.i ]
  %13 = trunc i64 %_35.0.i to i32
  %_97.i = shl nsw i32 %exp.1.i, 23
  %_11.i.i = add i32 %_97.i, 1065353216
  %_10.i.i = and i32 %_11.i.i, 2139095040
  %_13.i.i = and i32 %13, 8388607
  %_4.i.i = or i32 %_10.i.i, %_13.i.i
  %14 = bitcast i32 %_4.i.i to float
  br label %_ZN17compiler_builtins5float4conv12int_to_float17hffc57fc44d1764aaE.exit

_ZN17compiler_builtins5float4conv12int_to_float17hffc57fc44d1764aaE.exit: ; preds = %start, %bb43.i
  %.0.i = phi float [ %14, %bb43.i ], [ 0.000000e+00, %start ]
  ret float %.0.i
}

; compiler_builtins::float::conv::__floatundidf
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float4conv13__floatundidf17h88a3f11573bed3beE(i64 %i) unnamed_addr #1 {
start:
  %0 = icmp eq i64 %i, 0
  br i1 %0, label %_ZN17compiler_builtins5float4conv12int_to_float17h3679f3f05d9c668dE.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = tail call i64 @llvm.ctlz.i64(i64 %i, i1 true) #14, !range !6
  %2 = trunc i64 %1 to i32
  %i_sd.i = sub nuw nsw i32 64, %2
  %3 = xor i32 %2, 63
  %_36.i = icmp ult i32 %2, 11
  br i1 %_36.i, label %bb15.i, label %bb16.i

bb16.i:                                           ; preds = %bb3.i
  %_92.i = add nuw nsw i64 %1, 53
  %_5.i.i.i = and i64 %_92.i, 63
  %4 = shl i64 %i, %_5.i.i.i
  br label %bb43.i

bb15.i:                                           ; preds = %bb3.i
  switch i32 %i_sd.i, label %bb21.i [
    i32 54, label %bb17.i
    i32 55, label %bb29.i
  ]

bb17.i:                                           ; preds = %bb15.i
  %_5.0.i.i = shl i64 %i, 1
  br label %bb29.i

bb29.i:                                           ; preds = %bb21.i, %bb17.i, %bb15.i
  %_41.0.i = phi i64 [ %12, %bb21.i ], [ %_5.0.i.i, %bb17.i ], [ %i, %bb15.i ]
  %5 = lshr i64 %_41.0.i, 2
  %.lobit.i = and i64 %5, 1
  %6 = or i64 %.lobit.i, %_41.0.i
  %_4.0.i.i = add i64 %6, 1
  %7 = and i64 %_4.0.i.i, 36028797018963968
  %.not.i = icmp eq i64 %7, 0
  %spec.select.v.i = select i1 %.not.i, i64 2, i64 3
  %spec.select.i = lshr i64 %_4.0.i.i, %spec.select.v.i
  %spec.select33.i = select i1 %.not.i, i32 %3, i32 %i_sd.i
  br label %bb43.i

bb21.i:                                           ; preds = %bb15.i
  %_52.i = sub nsw i64 9, %1
  %8 = and i64 %_52.i, 63
  %_5.0.i7.i = lshr i64 %i, %8
  %_61.i = add nuw nsw i64 %1, 55
  %_5.i.i8.i = and i64 %_61.i, 63
  %9 = shl i64 %i, %_5.i.i8.i
  %10 = icmp ne i64 %9, 0
  %11 = zext i1 %10 to i64
  %12 = or i64 %_5.0.i7.i, %11
  br label %bb29.i

bb43.i:                                           ; preds = %bb29.i, %bb16.i
  %_35.0.i = phi i64 [ %4, %bb16.i ], [ %spec.select.i, %bb29.i ]
  %exp.1.i = phi i32 [ %3, %bb16.i ], [ %spec.select33.i, %bb29.i ]
  %_97.i = add nsw i32 %exp.1.i, 1023
  %13 = zext i32 %_97.i to i64
  %_11.i.i = shl nuw nsw i64 %13, 52
  %_13.i.i = and i64 %_35.0.i, 4503599627370495
  %_4.i.i = or i64 %_11.i.i, %_13.i.i
  %14 = bitcast i64 %_4.i.i to double
  br label %_ZN17compiler_builtins5float4conv12int_to_float17h3679f3f05d9c668dE.exit

_ZN17compiler_builtins5float4conv12int_to_float17h3679f3f05d9c668dE.exit: ; preds = %start, %bb43.i
  %.0.i = phi double [ %14, %bb43.i ], [ 0.000000e+00, %start ]
  ret double %.0.i
}

; compiler_builtins::float::conv::__floatuntisf
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float4conv13__floatuntisf17h3644c78b08fa2f34E(i128 %i) unnamed_addr #1 {
start:
  %0 = icmp eq i128 %i, 0
  br i1 %0, label %_ZN17compiler_builtins5float4conv12int_to_float17hb82f5b090e03b9f9E.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = tail call i128 @llvm.ctlz.i128(i128 %i, i1 true) #14, !range !7
  %2 = trunc i128 %1 to i32
  %i_sd.i = sub nuw nsw i32 128, %2
  %3 = xor i32 %2, 127
  %_36.i = icmp ult i32 %2, 104
  br i1 %_36.i, label %bb15.i, label %bb16.i

bb16.i:                                           ; preds = %bb3.i
  %_92.i = add nuw nsw i32 %2, 24
  %_5.i.i.i = and i32 %_92.i, 127
  %_4.i.i.i = zext i32 %_5.i.i.i to i128
  %4 = shl i128 %i, %_4.i.i.i
  %extract.t.i = trunc i128 %4 to i32
  br label %bb43.i

bb15.i:                                           ; preds = %bb3.i
  %trunc.i = trunc i32 %i_sd.i to i8
  switch i8 %trunc.i, label %bb21.i [
    i8 25, label %bb17.i
    i8 26, label %bb29.i
  ]

bb17.i:                                           ; preds = %bb15.i
  %_5.0.i.i = shl i128 %i, 1
  br label %bb29.i

bb29.i:                                           ; preds = %bb21.i, %bb17.i, %bb15.i
  %_41.0.i = phi i128 [ %13, %bb21.i ], [ %_5.0.i.i, %bb17.i ], [ %i, %bb15.i ]
  %5 = lshr i128 %_41.0.i, 2
  %.lobit.i = and i128 %5, 1
  %6 = or i128 %.lobit.i, %_41.0.i
  %_4.0.i.i = add i128 %6, 1
  %_4.0.i6.i = lshr i128 %_4.0.i.i, 2
  %7 = and i128 %_4.0.i.i, 67108864
  %.not.i = icmp eq i128 %7, 0
  %extract.t34.i = trunc i128 %_4.0.i6.i to i32
  br i1 %.not.i, label %bb43.i, label %bb39.i

bb21.i:                                           ; preds = %bb15.i
  %_52.i = sub nsw i32 102, %2
  %8 = and i32 %_52.i, 127
  %9 = zext i32 %8 to i128
  %_5.0.i7.i = lshr i128 %i, %9
  %_61.i = add nuw nsw i32 %2, 26
  %_5.i.i8.i = and i32 %_61.i, 127
  %_4.i.i9.i = zext i32 %_5.i.i8.i to i128
  %10 = shl i128 %i, %_4.i.i9.i
  %11 = icmp ne i128 %10, 0
  %12 = zext i1 %11 to i128
  %13 = or i128 %_5.0.i7.i, %12
  br label %bb29.i

bb39.i:                                           ; preds = %bb29.i
  %_4.0.i10.i = lshr i128 %_4.0.i.i, 3
  %extract.t33.i = trunc i128 %_4.0.i10.i to i32
  br label %bb43.i

bb43.i:                                           ; preds = %bb39.i, %bb29.i, %bb16.i
  %_35.0.off0.i = phi i32 [ %extract.t.i, %bb16.i ], [ %extract.t33.i, %bb39.i ], [ %extract.t34.i, %bb29.i ]
  %exp.1.i = phi i32 [ %3, %bb16.i ], [ %i_sd.i, %bb39.i ], [ %3, %bb29.i ]
  %_97.i = shl i32 %exp.1.i, 23
  %_11.i.i = add i32 %_97.i, 1065353216
  %_10.i.i = and i32 %_11.i.i, 2139095040
  %_13.i.i = and i32 %_35.0.off0.i, 8388607
  %_4.i.i = or i32 %_10.i.i, %_13.i.i
  %14 = bitcast i32 %_4.i.i to float
  br label %_ZN17compiler_builtins5float4conv12int_to_float17hb82f5b090e03b9f9E.exit

_ZN17compiler_builtins5float4conv12int_to_float17hb82f5b090e03b9f9E.exit: ; preds = %start, %bb43.i
  %.0.i = phi float [ %14, %bb43.i ], [ 0.000000e+00, %start ]
  ret float %.0.i
}

; compiler_builtins::float::conv::__floatuntidf
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float4conv13__floatuntidf17h5b8fc0de12a730cfE(i128 %i) unnamed_addr #1 {
start:
  %0 = icmp eq i128 %i, 0
  br i1 %0, label %_ZN17compiler_builtins5float4conv12int_to_float17hc347f84a82b17320E.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = tail call i128 @llvm.ctlz.i128(i128 %i, i1 true) #14, !range !7
  %2 = trunc i128 %1 to i32
  %i_sd.i = sub nuw nsw i32 128, %2
  %3 = xor i32 %2, 127
  %_36.i = icmp ult i32 %2, 75
  br i1 %_36.i, label %bb15.i, label %bb16.i

bb16.i:                                           ; preds = %bb3.i
  %_92.i = add nuw nsw i32 %2, 53
  %_5.i.i.i = and i32 %_92.i, 127
  %_4.i.i.i = zext i32 %_5.i.i.i to i128
  %4 = shl i128 %i, %_4.i.i.i
  %extract.t.i = trunc i128 %4 to i64
  br label %bb43.i

bb15.i:                                           ; preds = %bb3.i
  %trunc.i = trunc i32 %i_sd.i to i8
  switch i8 %trunc.i, label %bb21.i [
    i8 54, label %bb17.i
    i8 55, label %bb29.i
  ]

bb17.i:                                           ; preds = %bb15.i
  %_5.0.i.i = shl i128 %i, 1
  br label %bb29.i

bb29.i:                                           ; preds = %bb21.i, %bb17.i, %bb15.i
  %_41.0.i = phi i128 [ %13, %bb21.i ], [ %_5.0.i.i, %bb17.i ], [ %i, %bb15.i ]
  %5 = lshr i128 %_41.0.i, 2
  %.lobit.i = and i128 %5, 1
  %6 = or i128 %.lobit.i, %_41.0.i
  %_4.0.i.i = add i128 %6, 1
  %_4.0.i6.i = lshr i128 %_4.0.i.i, 2
  %7 = and i128 %_4.0.i.i, 36028797018963968
  %.not.i = icmp eq i128 %7, 0
  %extract.t34.i = trunc i128 %_4.0.i6.i to i64
  br i1 %.not.i, label %bb43.i, label %bb39.i

bb21.i:                                           ; preds = %bb15.i
  %_52.i = sub nsw i32 73, %2
  %8 = and i32 %_52.i, 127
  %9 = zext i32 %8 to i128
  %_5.0.i7.i = lshr i128 %i, %9
  %_61.i = add nuw nsw i32 %2, 55
  %_5.i.i8.i = and i32 %_61.i, 127
  %_4.i.i9.i = zext i32 %_5.i.i8.i to i128
  %10 = shl i128 %i, %_4.i.i9.i
  %11 = icmp ne i128 %10, 0
  %12 = zext i1 %11 to i128
  %13 = or i128 %_5.0.i7.i, %12
  br label %bb29.i

bb39.i:                                           ; preds = %bb29.i
  %_4.0.i10.i = lshr i128 %_4.0.i.i, 3
  %extract.t33.i = trunc i128 %_4.0.i10.i to i64
  br label %bb43.i

bb43.i:                                           ; preds = %bb39.i, %bb29.i, %bb16.i
  %_35.0.off0.i = phi i64 [ %extract.t.i, %bb16.i ], [ %extract.t33.i, %bb39.i ], [ %extract.t34.i, %bb29.i ]
  %exp.1.i = phi i32 [ %3, %bb16.i ], [ %i_sd.i, %bb39.i ], [ %3, %bb29.i ]
  %_97.i = add nsw i32 %exp.1.i, 1023
  %14 = zext i32 %_97.i to i64
  %_11.i.i = shl nuw nsw i64 %14, 52
  %_13.i.i = and i64 %_35.0.off0.i, 4503599627370495
  %_4.i.i = or i64 %_11.i.i, %_13.i.i
  %15 = bitcast i64 %_4.i.i to double
  br label %_ZN17compiler_builtins5float4conv12int_to_float17hc347f84a82b17320E.exit

_ZN17compiler_builtins5float4conv12int_to_float17hc347f84a82b17320E.exit: ; preds = %start, %bb43.i
  %.0.i = phi double [ %15, %bb43.i ], [ 0.000000e+00, %start ]
  ret double %.0.i
}

; compiler_builtins::float::conv::__fixsfsi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins5float4conv9__fixsfsi17h57e50c15835c9c75E(float %f) unnamed_addr #2 {
start:
  %0 = bitcast float %f to i32
  %1 = icmp slt i32 %0, 0
  %_3.i.i = lshr i32 %0, 23
  %2 = trunc i32 %_3.i.i to i16
  %3 = and i16 %2, 255
  %4 = icmp ult i16 %3, 127
  br i1 %4, label %_ZN17compiler_builtins5float4conv12float_to_int17hcff2a44e87c62a7fE.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 157
  br i1 %5, label %bb21.i, label %bb25.i

bb25.i:                                           ; preds = %bb12.i
  %6 = icmp ult i16 %3, 150
  %7 = and i32 %0, 8388607
  %8 = or i32 %7, 8388608
  br i1 %6, label %bb44.i, label %bb49.i

bb21.i:                                           ; preds = %bb12.i
  %..i = select i1 %1, i32 -2147483648, i32 2147483647
  br label %_ZN17compiler_builtins5float4conv12float_to_int17hcff2a44e87c62a7fE.exit

bb49.i:                                           ; preds = %bb25.i
  %_5.0.i.i = add nsw i16 %3, -150
  %9 = zext i16 %_5.0.i.i to i32
  %_5.0.i7.i = shl i32 %8, %9
  br label %bb54.i

bb44.i:                                           ; preds = %bb25.i
  %_5.0.i8.i = sub nuw nsw i16 150, %3
  %10 = zext i16 %_5.0.i8.i to i32
  %_5.0.i9.i = lshr i32 %8, %10
  br label %bb54.i

bb54.i:                                           ; preds = %bb44.i, %bb49.i
  %tmp1.0.i = phi i32 [ %_5.0.i9.i, %bb44.i ], [ %_5.0.i7.i, %bb49.i ]
  %11 = sub i32 0, %tmp1.0.i
  %spec.select.i = select i1 %1, i32 %11, i32 %tmp1.0.i
  br label %_ZN17compiler_builtins5float4conv12float_to_int17hcff2a44e87c62a7fE.exit

_ZN17compiler_builtins5float4conv12float_to_int17hcff2a44e87c62a7fE.exit: ; preds = %start, %bb21.i, %bb54.i
  %.2.i = phi i32 [ %..i, %bb21.i ], [ 0, %start ], [ %spec.select.i, %bb54.i ]
  ret i32 %.2.i
}

; compiler_builtins::float::conv::__fixsfdi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @_ZN17compiler_builtins5float4conv9__fixsfdi17h0e6639760129671bE(float %f) unnamed_addr #2 {
start:
  %0 = bitcast float %f to i32
  %1 = icmp slt i32 %0, 0
  %_3.i.i = lshr i32 %0, 23
  %2 = trunc i32 %_3.i.i to i16
  %3 = and i16 %2, 255
  %4 = icmp ult i16 %3, 127
  br i1 %4, label %_ZN17compiler_builtins5float4conv12float_to_int17h6050e3a570d04af2E.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 189
  br i1 %5, label %bb21.i, label %bb25.i

bb25.i:                                           ; preds = %bb12.i
  %6 = icmp ult i16 %3, 150
  %7 = and i32 %0, 8388607
  %8 = or i32 %7, 8388608
  %9 = zext i32 %8 to i64
  br i1 %6, label %bb29.i, label %bb35.i

bb21.i:                                           ; preds = %bb12.i
  %..i = select i1 %1, i64 -9223372036854775808, i64 9223372036854775807
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h6050e3a570d04af2E.exit

bb35.i:                                           ; preds = %bb25.i
  %_5.0.i.i = add nsw i16 %3, -150
  %10 = zext i16 %_5.0.i.i to i64
  %_5.0.i7.i = shl i64 %9, %10
  br label %bb41.i

bb29.i:                                           ; preds = %bb25.i
  %_5.0.i8.i = sub nuw nsw i16 150, %3
  %11 = zext i16 %_5.0.i8.i to i64
  %_5.0.i923.i = lshr i64 %9, %11
  br label %bb41.i

bb41.i:                                           ; preds = %bb29.i, %bb35.i
  %tmp.0.i = phi i64 [ %_5.0.i923.i, %bb29.i ], [ %_5.0.i7.i, %bb35.i ]
  %12 = sub i64 0, %tmp.0.i
  %spec.select.i = select i1 %1, i64 %12, i64 %tmp.0.i
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h6050e3a570d04af2E.exit

_ZN17compiler_builtins5float4conv12float_to_int17h6050e3a570d04af2E.exit: ; preds = %start, %bb21.i, %bb41.i
  %.2.i = phi i64 [ %..i, %bb21.i ], [ 0, %start ], [ %spec.select.i, %bb41.i ]
  ret i64 %.2.i
}

; compiler_builtins::float::conv::__fixsfti
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @_ZN17compiler_builtins5float4conv9__fixsfti17hc0d01c578e77be4aE(float %f) unnamed_addr #2 {
start:
  %0 = bitcast float %f to i32
  %1 = icmp slt i32 %0, 0
  %_3.i.i = lshr i32 %0, 23
  %2 = trunc i32 %_3.i.i to i16
  %3 = and i16 %2, 255
  %4 = icmp ult i16 %3, 127
  br i1 %4, label %_ZN17compiler_builtins5float4conv12float_to_int17h14fe9ff343a105daE.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 253
  br i1 %5, label %bb21.i, label %bb25.i

bb25.i:                                           ; preds = %bb12.i
  %6 = icmp ult i16 %3, 150
  %7 = and i32 %0, 8388607
  %8 = or i32 %7, 8388608
  %9 = zext i32 %8 to i128
  br i1 %6, label %bb29.i, label %bb35.i

bb21.i:                                           ; preds = %bb12.i
  %..i = select i1 %1, i128 -170141183460469231731687303715884105728, i128 170141183460469231731687303715884105727
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h14fe9ff343a105daE.exit

bb35.i:                                           ; preds = %bb25.i
  %_5.0.i.i = add nsw i16 %3, -150
  %10 = zext i16 %_5.0.i.i to i128
  %_5.0.i7.i = shl i128 %9, %10
  br label %bb41.i

bb29.i:                                           ; preds = %bb25.i
  %_5.0.i8.i = sub nuw nsw i16 150, %3
  %11 = zext i16 %_5.0.i8.i to i128
  %_5.0.i923.i = lshr i128 %9, %11
  br label %bb41.i

bb41.i:                                           ; preds = %bb29.i, %bb35.i
  %tmp.0.i = phi i128 [ %_5.0.i923.i, %bb29.i ], [ %_5.0.i7.i, %bb35.i ]
  %12 = sub i128 0, %tmp.0.i
  %spec.select.i = select i1 %1, i128 %12, i128 %tmp.0.i
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h14fe9ff343a105daE.exit

_ZN17compiler_builtins5float4conv12float_to_int17h14fe9ff343a105daE.exit: ; preds = %start, %bb21.i, %bb41.i
  %.2.i = phi i128 [ %..i, %bb21.i ], [ 0, %start ], [ %spec.select.i, %bb41.i ]
  ret i128 %.2.i
}

; compiler_builtins::float::conv::__fixdfsi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins5float4conv9__fixdfsi17h7b2120fc884aa80eE(double %f) unnamed_addr #2 {
start:
  %0 = bitcast double %f to i64
  %1 = icmp slt i64 %0, 0
  %_3.i.i = lshr i64 %0, 52
  %2 = trunc i64 %_3.i.i to i16
  %3 = and i16 %2, 2047
  %4 = icmp ult i16 %3, 1023
  br i1 %4, label %_ZN17compiler_builtins5float4conv12float_to_int17h138158fe85a9b172E.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 1053
  br i1 %5, label %bb21.i, label %bb54.i

bb21.i:                                           ; preds = %bb12.i
  %..i = select i1 %1, i32 -2147483648, i32 2147483647
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h138158fe85a9b172E.exit

bb54.i:                                           ; preds = %bb12.i
  %6 = and i64 %0, 4503599627370495
  %7 = or i64 %6, 4503599627370496
  %_5.0.i8.i = sub nuw nsw i16 1075, %3
  %8 = zext i16 %_5.0.i8.i to i64
  %_5.0.i9.i = lshr i64 %7, %8
  %9 = trunc i64 %_5.0.i9.i to i32
  %10 = sub i32 0, %9
  %spec.select.i = select i1 %1, i32 %10, i32 %9
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h138158fe85a9b172E.exit

_ZN17compiler_builtins5float4conv12float_to_int17h138158fe85a9b172E.exit: ; preds = %start, %bb21.i, %bb54.i
  %.2.i = phi i32 [ %..i, %bb21.i ], [ 0, %start ], [ %spec.select.i, %bb54.i ]
  ret i32 %.2.i
}

; compiler_builtins::float::conv::__fixdfdi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @_ZN17compiler_builtins5float4conv9__fixdfdi17h06e50da023d9b27eE(double %f) unnamed_addr #2 {
start:
  %0 = bitcast double %f to i64
  %1 = icmp slt i64 %0, 0
  %_3.i.i = lshr i64 %0, 52
  %2 = trunc i64 %_3.i.i to i16
  %3 = and i16 %2, 2047
  %4 = icmp ult i16 %3, 1023
  br i1 %4, label %_ZN17compiler_builtins5float4conv12float_to_int17h2ecd39e19bd182bbE.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 1085
  br i1 %5, label %bb21.i, label %bb25.i

bb25.i:                                           ; preds = %bb12.i
  %6 = icmp ult i16 %3, 1075
  %7 = and i64 %0, 4503599627370495
  %8 = or i64 %7, 4503599627370496
  br i1 %6, label %bb44.i, label %bb49.i

bb21.i:                                           ; preds = %bb12.i
  %..i = select i1 %1, i64 -9223372036854775808, i64 9223372036854775807
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h2ecd39e19bd182bbE.exit

bb49.i:                                           ; preds = %bb25.i
  %_5.0.i.i = add nsw i16 %3, -1075
  %9 = zext i16 %_5.0.i.i to i64
  %_5.0.i7.i = shl i64 %8, %9
  br label %bb54.i

bb44.i:                                           ; preds = %bb25.i
  %_5.0.i8.i = sub nuw nsw i16 1075, %3
  %10 = zext i16 %_5.0.i8.i to i64
  %_5.0.i9.i = lshr i64 %8, %10
  br label %bb54.i

bb54.i:                                           ; preds = %bb44.i, %bb49.i
  %tmp1.0.i = phi i64 [ %_5.0.i9.i, %bb44.i ], [ %_5.0.i7.i, %bb49.i ]
  %11 = sub i64 0, %tmp1.0.i
  %spec.select.i = select i1 %1, i64 %11, i64 %tmp1.0.i
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h2ecd39e19bd182bbE.exit

_ZN17compiler_builtins5float4conv12float_to_int17h2ecd39e19bd182bbE.exit: ; preds = %start, %bb21.i, %bb54.i
  %.2.i = phi i64 [ %..i, %bb21.i ], [ 0, %start ], [ %spec.select.i, %bb54.i ]
  ret i64 %.2.i
}

; compiler_builtins::float::conv::__fixdfti
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @_ZN17compiler_builtins5float4conv9__fixdfti17h603ce156de3eba31E(double %f) unnamed_addr #2 {
start:
  %0 = bitcast double %f to i64
  %1 = icmp slt i64 %0, 0
  %_3.i.i = lshr i64 %0, 52
  %2 = trunc i64 %_3.i.i to i16
  %3 = and i16 %2, 2047
  %4 = icmp ult i16 %3, 1023
  br i1 %4, label %_ZN17compiler_builtins5float4conv12float_to_int17h1d3ea694518d1006E.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 1149
  br i1 %5, label %bb21.i, label %bb25.i

bb25.i:                                           ; preds = %bb12.i
  %6 = icmp ult i16 %3, 1075
  %7 = and i64 %0, 4503599627370495
  %8 = or i64 %7, 4503599627370496
  %9 = zext i64 %8 to i128
  br i1 %6, label %bb29.i, label %bb35.i

bb21.i:                                           ; preds = %bb12.i
  %..i = select i1 %1, i128 -170141183460469231731687303715884105728, i128 170141183460469231731687303715884105727
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h1d3ea694518d1006E.exit

bb35.i:                                           ; preds = %bb25.i
  %_5.0.i.i = add nsw i16 %3, -1075
  %10 = zext i16 %_5.0.i.i to i128
  %_5.0.i7.i = shl i128 %9, %10
  br label %bb41.i

bb29.i:                                           ; preds = %bb25.i
  %_5.0.i8.i = sub nuw nsw i16 1075, %3
  %11 = zext i16 %_5.0.i8.i to i128
  %_5.0.i923.i = lshr i128 %9, %11
  br label %bb41.i

bb41.i:                                           ; preds = %bb29.i, %bb35.i
  %tmp.0.i = phi i128 [ %_5.0.i923.i, %bb29.i ], [ %_5.0.i7.i, %bb35.i ]
  %12 = sub i128 0, %tmp.0.i
  %spec.select.i = select i1 %1, i128 %12, i128 %tmp.0.i
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h1d3ea694518d1006E.exit

_ZN17compiler_builtins5float4conv12float_to_int17h1d3ea694518d1006E.exit: ; preds = %start, %bb21.i, %bb41.i
  %.2.i = phi i128 [ %..i, %bb21.i ], [ 0, %start ], [ %spec.select.i, %bb41.i ]
  ret i128 %.2.i
}

; compiler_builtins::float::conv::__fixunssfsi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins5float4conv12__fixunssfsi17hf54458efcdfb9f48E(float %f) unnamed_addr #2 {
start:
  %0 = bitcast float %f to i32
  %1 = icmp slt i32 %0, 0
  %_3.i.i = lshr i32 %0, 23
  %2 = trunc i32 %_3.i.i to i16
  %3 = and i16 %2, 255
  %4 = icmp ult i16 %3, 127
  %.sign.i = select i1 %4, i1 true, i1 %1
  br i1 %.sign.i, label %_ZN17compiler_builtins5float4conv12float_to_int17h5aa1b8bf2326b043E.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 158
  br i1 %5, label %_ZN17compiler_builtins5float4conv12float_to_int17h5aa1b8bf2326b043E.exit, label %bb25.i

bb25.i:                                           ; preds = %bb12.i
  %6 = icmp ult i16 %3, 150
  %7 = and i32 %0, 8388607
  %8 = or i32 %7, 8388608
  br i1 %6, label %bb44.i, label %bb49.i

bb49.i:                                           ; preds = %bb25.i
  %_5.0.i.i = add nsw i16 %3, -150
  %9 = zext i16 %_5.0.i.i to i32
  %_5.0.i7.i = shl i32 %8, %9
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h5aa1b8bf2326b043E.exit

bb44.i:                                           ; preds = %bb25.i
  %_5.0.i8.i = sub nuw nsw i16 150, %3
  %10 = zext i16 %_5.0.i8.i to i32
  %_5.0.i9.i = lshr i32 %8, %10
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h5aa1b8bf2326b043E.exit

_ZN17compiler_builtins5float4conv12float_to_int17h5aa1b8bf2326b043E.exit: ; preds = %start, %bb12.i, %bb49.i, %bb44.i
  %.2.i = phi i32 [ 0, %start ], [ -1, %bb12.i ], [ %_5.0.i9.i, %bb44.i ], [ %_5.0.i7.i, %bb49.i ]
  ret i32 %.2.i
}

; compiler_builtins::float::conv::__fixunssfdi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @_ZN17compiler_builtins5float4conv12__fixunssfdi17h8f8d685f960e4b87E(float %f) unnamed_addr #2 {
start:
  %0 = bitcast float %f to i32
  %1 = icmp slt i32 %0, 0
  %_3.i.i = lshr i32 %0, 23
  %2 = trunc i32 %_3.i.i to i16
  %3 = and i16 %2, 255
  %4 = icmp ult i16 %3, 127
  %.sign.i = select i1 %4, i1 true, i1 %1
  br i1 %.sign.i, label %_ZN17compiler_builtins5float4conv12float_to_int17hbf6562cb4b15c08aE.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 190
  br i1 %5, label %_ZN17compiler_builtins5float4conv12float_to_int17hbf6562cb4b15c08aE.exit, label %bb25.i

bb25.i:                                           ; preds = %bb12.i
  %6 = icmp ult i16 %3, 150
  %7 = and i32 %0, 8388607
  %8 = or i32 %7, 8388608
  %9 = zext i32 %8 to i64
  br i1 %6, label %bb29.i, label %bb35.i

bb35.i:                                           ; preds = %bb25.i
  %_5.0.i.i = add nsw i16 %3, -150
  %10 = zext i16 %_5.0.i.i to i64
  %_5.0.i7.i = shl i64 %9, %10
  br label %_ZN17compiler_builtins5float4conv12float_to_int17hbf6562cb4b15c08aE.exit

bb29.i:                                           ; preds = %bb25.i
  %_5.0.i8.i = sub nuw nsw i16 150, %3
  %11 = zext i16 %_5.0.i8.i to i64
  %_5.0.i9.i = lshr i64 %9, %11
  br label %_ZN17compiler_builtins5float4conv12float_to_int17hbf6562cb4b15c08aE.exit

_ZN17compiler_builtins5float4conv12float_to_int17hbf6562cb4b15c08aE.exit: ; preds = %start, %bb12.i, %bb35.i, %bb29.i
  %.2.i = phi i64 [ 0, %start ], [ -1, %bb12.i ], [ %_5.0.i9.i, %bb29.i ], [ %_5.0.i7.i, %bb35.i ]
  ret i64 %.2.i
}

; compiler_builtins::float::conv::__fixunssfti
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @_ZN17compiler_builtins5float4conv12__fixunssfti17h08da700ef7403245E(float %f) unnamed_addr #2 {
start:
  %0 = bitcast float %f to i32
  %1 = icmp slt i32 %0, 0
  %_3.i.i = lshr i32 %0, 23
  %2 = trunc i32 %_3.i.i to i16
  %3 = and i16 %2, 255
  %4 = icmp ult i16 %3, 127
  %.sign.i = select i1 %4, i1 true, i1 %1
  br i1 %.sign.i, label %_ZN17compiler_builtins5float4conv12float_to_int17h91907fd1431c106bE.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp eq i16 %3, 255
  br i1 %5, label %_ZN17compiler_builtins5float4conv12float_to_int17h91907fd1431c106bE.exit, label %bb25.i

bb25.i:                                           ; preds = %bb12.i
  %6 = icmp ult i16 %3, 150
  %7 = and i32 %0, 8388607
  %8 = or i32 %7, 8388608
  %9 = zext i32 %8 to i128
  br i1 %6, label %bb29.i, label %bb35.i

bb35.i:                                           ; preds = %bb25.i
  %_5.0.i.i = add nsw i16 %3, -150
  %10 = zext i16 %_5.0.i.i to i128
  %_5.0.i7.i = shl i128 %9, %10
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h91907fd1431c106bE.exit

bb29.i:                                           ; preds = %bb25.i
  %_5.0.i8.i = sub nuw nsw i16 150, %3
  %11 = zext i16 %_5.0.i8.i to i128
  %_5.0.i9.i = lshr i128 %9, %11
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h91907fd1431c106bE.exit

_ZN17compiler_builtins5float4conv12float_to_int17h91907fd1431c106bE.exit: ; preds = %start, %bb12.i, %bb35.i, %bb29.i
  %.2.i = phi i128 [ 0, %start ], [ -1, %bb12.i ], [ %_5.0.i9.i, %bb29.i ], [ %_5.0.i7.i, %bb35.i ]
  ret i128 %.2.i
}

; compiler_builtins::float::conv::__fixunsdfsi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins5float4conv12__fixunsdfsi17h1ea0fb892fac3f9bE(double %f) unnamed_addr #2 {
start:
  %0 = bitcast double %f to i64
  %1 = icmp slt i64 %0, 0
  %_3.i.i = lshr i64 %0, 52
  %2 = trunc i64 %_3.i.i to i16
  %3 = and i16 %2, 2047
  %4 = icmp ult i16 %3, 1023
  %.sign.i = select i1 %4, i1 true, i1 %1
  br i1 %.sign.i, label %_ZN17compiler_builtins5float4conv12float_to_int17hba8ff44f142d0460E.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 1054
  br i1 %5, label %_ZN17compiler_builtins5float4conv12float_to_int17hba8ff44f142d0460E.exit, label %bb60.i

bb60.i:                                           ; preds = %bb12.i
  %6 = and i64 %0, 4503599627370495
  %7 = or i64 %6, 4503599627370496
  %_5.0.i8.i = sub nuw nsw i16 1075, %3
  %8 = zext i16 %_5.0.i8.i to i64
  %_5.0.i9.i = lshr i64 %7, %8
  %9 = trunc i64 %_5.0.i9.i to i32
  br label %_ZN17compiler_builtins5float4conv12float_to_int17hba8ff44f142d0460E.exit

_ZN17compiler_builtins5float4conv12float_to_int17hba8ff44f142d0460E.exit: ; preds = %start, %bb12.i, %bb60.i
  %.2.i = phi i32 [ %9, %bb60.i ], [ 0, %start ], [ -1, %bb12.i ]
  ret i32 %.2.i
}

; compiler_builtins::float::conv::__fixunsdfdi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @_ZN17compiler_builtins5float4conv12__fixunsdfdi17h8239f57048b408fbE(double %f) unnamed_addr #2 {
start:
  %0 = bitcast double %f to i64
  %1 = icmp slt i64 %0, 0
  %_3.i.i = lshr i64 %0, 52
  %2 = trunc i64 %_3.i.i to i16
  %3 = and i16 %2, 2047
  %4 = icmp ult i16 %3, 1023
  %.sign.i = select i1 %4, i1 true, i1 %1
  br i1 %.sign.i, label %_ZN17compiler_builtins5float4conv12float_to_int17hd2c9c0492197700eE.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 1086
  br i1 %5, label %_ZN17compiler_builtins5float4conv12float_to_int17hd2c9c0492197700eE.exit, label %bb25.i

bb25.i:                                           ; preds = %bb12.i
  %6 = icmp ult i16 %3, 1075
  %7 = and i64 %0, 4503599627370495
  %8 = or i64 %7, 4503599627370496
  br i1 %6, label %bb44.i, label %bb49.i

bb49.i:                                           ; preds = %bb25.i
  %_5.0.i.i = add nsw i16 %3, -1075
  %9 = zext i16 %_5.0.i.i to i64
  %_5.0.i7.i = shl i64 %8, %9
  br label %_ZN17compiler_builtins5float4conv12float_to_int17hd2c9c0492197700eE.exit

bb44.i:                                           ; preds = %bb25.i
  %_5.0.i8.i = sub nuw nsw i16 1075, %3
  %10 = zext i16 %_5.0.i8.i to i64
  %_5.0.i9.i = lshr i64 %8, %10
  br label %_ZN17compiler_builtins5float4conv12float_to_int17hd2c9c0492197700eE.exit

_ZN17compiler_builtins5float4conv12float_to_int17hd2c9c0492197700eE.exit: ; preds = %start, %bb12.i, %bb49.i, %bb44.i
  %.2.i = phi i64 [ 0, %start ], [ -1, %bb12.i ], [ %_5.0.i9.i, %bb44.i ], [ %_5.0.i7.i, %bb49.i ]
  ret i64 %.2.i
}

; compiler_builtins::float::conv::__fixunsdfti
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @_ZN17compiler_builtins5float4conv12__fixunsdfti17h935cad9de4c1436cE(double %f) unnamed_addr #2 {
start:
  %0 = bitcast double %f to i64
  %1 = icmp slt i64 %0, 0
  %_3.i.i = lshr i64 %0, 52
  %2 = trunc i64 %_3.i.i to i16
  %3 = and i16 %2, 2047
  %4 = icmp ult i16 %3, 1023
  %.sign.i = select i1 %4, i1 true, i1 %1
  br i1 %.sign.i, label %_ZN17compiler_builtins5float4conv12float_to_int17h80d8f5149eaec6d9E.exit, label %bb12.i

bb12.i:                                           ; preds = %start
  %5 = icmp ugt i16 %3, 1150
  br i1 %5, label %_ZN17compiler_builtins5float4conv12float_to_int17h80d8f5149eaec6d9E.exit, label %bb25.i

bb25.i:                                           ; preds = %bb12.i
  %6 = icmp ult i16 %3, 1075
  %7 = and i64 %0, 4503599627370495
  %8 = or i64 %7, 4503599627370496
  %9 = zext i64 %8 to i128
  br i1 %6, label %bb29.i, label %bb35.i

bb35.i:                                           ; preds = %bb25.i
  %_5.0.i.i = add nsw i16 %3, -1075
  %10 = zext i16 %_5.0.i.i to i128
  %_5.0.i7.i = shl i128 %9, %10
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h80d8f5149eaec6d9E.exit

bb29.i:                                           ; preds = %bb25.i
  %_5.0.i8.i = sub nuw nsw i16 1075, %3
  %11 = zext i16 %_5.0.i8.i to i128
  %_5.0.i9.i = lshr i128 %9, %11
  br label %_ZN17compiler_builtins5float4conv12float_to_int17h80d8f5149eaec6d9E.exit

_ZN17compiler_builtins5float4conv12float_to_int17h80d8f5149eaec6d9E.exit: ; preds = %start, %bb12.i, %bb35.i, %bb29.i
  %.2.i = phi i128 [ 0, %start ], [ -1, %bb12.i ], [ %_5.0.i9.i, %bb29.i ], [ %_5.0.i7.i, %bb35.i ]
  ret i128 %.2.i
}

; compiler_builtins::float::div::__divsf3
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float3div8__divsf317h5b98ce21bcb2e1cdE(float %a, float %b) unnamed_addr #1 {
start:
  %0 = bitcast float %a to i32
  %1 = bitcast float %b to i32
  %_5.0.i33.i = lshr i32 %0, 23
  %2 = and i32 %_5.0.i33.i, 255
  %_5.0.i34.i = lshr i32 %1, 23
  %3 = and i32 %_5.0.i34.i, 255
  %4 = xor i32 %1, %0
  %5 = and i32 %4, -2147483648
  %6 = and i32 %0, 8388607
  %7 = and i32 %1, 8388607
  %8 = add nsw i32 %2, -1
  %9 = icmp ugt i32 %8, 253
  %10 = add nsw i32 %3, -1
  %11 = icmp ugt i32 %10, 253
  %or.cond.i = select i1 %9, i1 true, i1 %11
  br i1 %or.cond.i, label %bb25.i, label %bb72.i

bb72.i:                                           ; preds = %bb69.i, %bb67.i, %start
  %a_significand.0.i = phi i32 [ %a_significand.1.i, %bb69.i ], [ %a_significand.1.i, %bb67.i ], [ %6, %start ]
  %b_significand.0.i = phi i32 [ %_10.i38.i, %bb69.i ], [ %7, %bb67.i ], [ %7, %start ]
  %scale.0.i = phi i32 [ %63, %bb69.i ], [ %scale.1.i, %bb67.i ], [ 0, %start ]
  %12 = or i32 %b_significand.0.i, 8388608
  %13 = sub nsw i32 %2, %3
  %14 = add nsw i32 %13, %scale.0.i
  %_5.0.i35.i = shl i32 %12, 8
  %15 = sub i32 1963258675, %_5.0.i35.i
  %_156.i = zext i32 %15 to i64
  %_158.i = zext i32 %_5.0.i35.i to i64
  %16 = mul nuw i64 %_156.i, %_158.i
  %_154.i = lshr i64 %16, 32
  %17 = sub nsw i64 0, %_154.i
  %_164.i = and i64 %17, 4294967295
  %18 = mul nuw i64 %_164.i, %_156.i
  %_160.i = lshr i64 %18, 31
  %_170.i = and i64 %_160.i, 4294967295
  %19 = mul nuw i64 %_170.i, %_158.i
  %_168.i = lshr i64 %19, 32
  %20 = sub nsw i64 0, %_168.i
  %_178.i = and i64 %20, 4294967295
  %21 = mul nuw i64 %_178.i, %_170.i
  %_174.i = lshr i64 %21, 31
  %_184.i = and i64 %_174.i, 4294967295
  %22 = mul nuw i64 %_184.i, %_158.i
  %_182.i = lshr i64 %22, 32
  %23 = sub nsw i64 0, %_182.i
  %_192.i = and i64 %23, 4294967295
  %24 = mul nuw i64 %_192.i, %_184.i
  %_188.i = lshr i64 %24, 31
  %25 = add nuw nsw i64 %_188.i, 4294967294
  %26 = shl i32 %a_significand.0.i, 1
  %_5.0.i36.i = or i32 %26, 16777216
  %27 = zext i32 %_5.0.i36.i to i64
  %28 = and i64 %25, 4294967295
  %29 = mul nuw i64 %28, %27
  %_2.i.i = lshr i64 %29, 32
  %30 = trunc i64 %_2.i.i to i32
  %31 = icmp ult i32 %30, 16777216
  %_4.0.i99.i = lshr i64 %29, 33
  %_4.0.i.i = trunc i64 %_4.0.i99.i to i32
  %.sink.i = select i1 %31, i32 24, i32 23
  %_4.0.i.sink.i = select i1 %31, i32 %30, i32 %_4.0.i.i
  %32 = sext i1 %31 to i32
  %quotient_exponent.0.i = add nsw i32 %14, %32
  %_5.0.i39.i = shl i32 %a_significand.0.i, %.sink.i
  %33 = mul i32 %_4.0.i.sink.i, %12
  %34 = sub i32 %_5.0.i39.i, %33
  %_229.i = icmp sgt i32 %quotient_exponent.0.i, 127
  br i1 %_229.i, label %bb111.i, label %bb114.i

bb25.i:                                           ; preds = %start
  %35 = and i32 %0, 2147483647
  %36 = and i32 %1, 2147483647
  %37 = icmp ugt i32 %35, 2139095040
  br i1 %37, label %bb29.i, label %bb32.i

bb32.i:                                           ; preds = %bb25.i
  %38 = icmp ugt i32 %36, 2139095040
  br i1 %38, label %bb34.i, label %bb37.i

bb29.i:                                           ; preds = %bb25.i
  %39 = or i32 %0, 4194304
  %40 = bitcast i32 %39 to float
  br label %_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit

bb37.i:                                           ; preds = %bb32.i
  %41 = icmp eq i32 %35, 2139095040
  %42 = icmp eq i32 %36, 2139095040
  br i1 %41, label %bb39.i, label %bb46.i

bb34.i:                                           ; preds = %bb32.i
  %43 = or i32 %1, 4194304
  %44 = bitcast i32 %43 to float
  br label %_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit

bb46.i:                                           ; preds = %bb37.i
  br i1 %42, label %bb48.i, label %bb50.i

bb39.i:                                           ; preds = %bb37.i
  br i1 %42, label %_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit, label %bb43.i

bb43.i:                                           ; preds = %bb39.i
  %45 = and i32 %1, -2147483648
  %46 = xor i32 %45, %0
  %47 = bitcast i32 %46 to float
  br label %_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit

bb50.i:                                           ; preds = %bb46.i
  %48 = icmp eq i32 %35, 0
  %49 = icmp eq i32 %36, 0
  br i1 %48, label %bb52.i, label %bb58.i

bb48.i:                                           ; preds = %bb46.i
  %50 = bitcast i32 %5 to float
  br label %_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit

bb58.i:                                           ; preds = %bb50.i
  br i1 %49, label %bb60.i, label %bb63.i

bb52.i:                                           ; preds = %bb50.i
  %51 = bitcast i32 %5 to float
  %spec.select.i = select i1 %49, float 0x7FF8000000000000, float %51
  br label %_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit

bb63.i:                                           ; preds = %bb58.i
  %52 = icmp ult i32 %35, 8388608
  br i1 %52, label %bb65.i, label %bb67.i

bb60.i:                                           ; preds = %bb58.i
  %53 = or i32 %5, 2139095040
  %54 = bitcast i32 %53 to float
  br label %_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit

bb67.i:                                           ; preds = %bb65.i, %bb63.i
  %a_significand.1.i = phi i32 [ %_10.i.i, %bb65.i ], [ %6, %bb63.i ]
  %scale.1.i = phi i32 [ %58, %bb65.i ], [ 0, %bb63.i ]
  %55 = icmp ult i32 %36, 8388608
  br i1 %55, label %bb69.i, label %bb72.i

bb65.i:                                           ; preds = %bb63.i
  %56 = tail call i32 @llvm.ctlz.i32(i32 %6, i1 false) #14, !range !5
  %57 = add nuw nsw i32 %56, 24
  %58 = sub nsw i32 9, %56
  %59 = and i32 %57, 31
  %_10.i.i = shl i32 %6, %59
  br label %bb67.i

bb69.i:                                           ; preds = %bb67.i
  %60 = tail call i32 @llvm.ctlz.i32(i32 %7, i1 false) #14, !range !5
  %61 = add nuw nsw i32 %60, 24
  %.neg.i = add nsw i32 %60, -9
  %62 = and i32 %61, 31
  %_10.i38.i = shl i32 %7, %62
  %63 = add nsw i32 %.neg.i, %scale.1.i
  br label %bb72.i

bb114.i:                                          ; preds = %bb72.i
  %_236.i = icmp slt i32 %quotient_exponent.0.i, -126
  br i1 %_236.i, label %bb115.i, label %bb117.i

bb111.i:                                          ; preds = %bb72.i
  %64 = or i32 %5, 2139095040
  %65 = bitcast i32 %64 to float
  br label %_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit

bb117.i:                                          ; preds = %bb114.i
  %_5.0.i41.i = shl i32 %34, 1
  %66 = icmp ugt i32 %_5.0.i41.i, %12
  %round.i = zext i1 %66 to i32
  %67 = and i32 %_4.0.i.sink.i, 8388607
  %68 = shl nsw i32 %quotient_exponent.0.i, 23
  %_5.0.i42.i = or i32 %67, 1065353216
  %69 = add i32 %_5.0.i42.i, %68
  %70 = add i32 %69, %round.i
  %71 = or i32 %70, %5
  %72 = bitcast i32 %71 to float
  br label %_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit

bb115.i:                                          ; preds = %bb114.i
  %73 = bitcast i32 %5 to float
  br label %_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit

_ZN17compiler_builtins5float3div5div3217he572054e270a4b07E.exit: ; preds = %bb29.i, %bb34.i, %bb39.i, %bb43.i, %bb48.i, %bb52.i, %bb60.i, %bb111.i, %bb117.i, %bb115.i
  %.1.i = phi float [ %40, %bb29.i ], [ %44, %bb34.i ], [ %47, %bb43.i ], [ %50, %bb48.i ], [ %54, %bb60.i ], [ 0x7FF8000000000000, %bb39.i ], [ %65, %bb111.i ], [ %73, %bb115.i ], [ %72, %bb117.i ], [ %spec.select.i, %bb52.i ]
  ret float %.1.i
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @__divsf3(float %a, float %b) unnamed_addr #1 {
start:
; call compiler_builtins::float::div::__divsf3
  %0 = tail call float @_ZN17compiler_builtins5float3div8__divsf317h5b98ce21bcb2e1cdE(float %a, float %b)
  ret float %0
}

; compiler_builtins::float::div::__divdf3
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float3div8__divdf317h9a15b37725ddfcf7E(double %a, double %b) unnamed_addr #1 {
start:
  %0 = bitcast double %a to i64
  %1 = bitcast double %b to i64
  %_5.0.i38.i = lshr i64 %0, 52
  %2 = and i64 %_5.0.i38.i, 2047
  %_5.0.i39.i = lshr i64 %1, 52
  %3 = and i64 %_5.0.i39.i, 2047
  %4 = xor i64 %1, %0
  %5 = and i64 %4, -9223372036854775808
  %6 = and i64 %0, 4503599627370495
  %7 = and i64 %1, 4503599627370495
  %8 = add nsw i64 %2, -1
  %9 = icmp ugt i64 %8, 2045
  %10 = add nsw i64 %3, -1
  %11 = icmp ugt i64 %10, 2045
  %or.cond.i = select i1 %9, i1 true, i1 %11
  br i1 %or.cond.i, label %bb25.i, label %bb72.i

bb72.i:                                           ; preds = %bb69.i, %bb67.i, %start
  %a_significand.0.i = phi i64 [ %a_significand.1.i, %bb69.i ], [ %a_significand.1.i, %bb67.i ], [ %6, %start ]
  %b_significand.0.i = phi i64 [ %_10.i44.i, %bb69.i ], [ %7, %bb67.i ], [ %7, %start ]
  %scale.0.i = phi i32 [ %77, %bb69.i ], [ %scale.1.i, %bb67.i ], [ 0, %start ]
  %12 = or i64 %b_significand.0.i, 4503599627370496
  %13 = trunc i64 %2 to i32
  %14 = trunc i64 %3 to i32
  %15 = sub nsw i32 %13, %14
  %16 = add nsw i32 %15, %scale.0.i
  %_5.0.i40.i = lshr i64 %12, 21
  %17 = sub nsw i64 1963258675, %_5.0.i40.i
  %_156.i = and i64 %17, 4294967295
  %_158.i = and i64 %_5.0.i40.i, 4294967295
  %18 = mul nuw i64 %_156.i, %_158.i
  %_154.i = lshr i64 %18, 32
  %19 = sub nsw i64 0, %_154.i
  %_164.i = and i64 %19, 4294967295
  %20 = mul nuw i64 %_164.i, %_156.i
  %_160.i = lshr i64 %20, 31
  %_170.i = and i64 %_160.i, 4294967295
  %21 = mul nuw i64 %_170.i, %_158.i
  %_168.i = lshr i64 %21, 32
  %22 = sub nsw i64 0, %_168.i
  %_178.i = and i64 %22, 4294967295
  %23 = mul nuw i64 %_178.i, %_170.i
  %_174.i = lshr i64 %23, 31
  %_184.i = and i64 %_174.i, 4294967295
  %24 = mul nuw i64 %_184.i, %_158.i
  %_182.i = lshr i64 %24, 32
  %25 = sub nsw i64 0, %_182.i
  %_192.i = and i64 %25, 4294967295
  %26 = mul nuw i64 %_192.i, %_184.i
  %_188.i = lshr i64 %26, 31
  %27 = add nuw nsw i64 %_188.i, 4294967295
  %28 = shl i64 %b_significand.0.i, 11
  %_203.i = and i64 %27, 4294967295
  %29 = mul nuw i64 %_203.i, %_158.i
  %_211.i = and i64 %28, 4294965248
  %30 = mul nuw i64 %_203.i, %_211.i
  %_207.i = lshr i64 %30, 32
  %31 = add nuw i64 %29, %_207.i
  %32 = sub i64 0, %31
  %_214.i = lshr i64 %32, 32
  %33 = mul nuw i64 %_214.i, %_203.i
  %_228.i = and i64 %32, 4294967295
  %34 = mul nuw i64 %_228.i, %_203.i
  %_224.i = lshr i64 %34, 32
  %35 = add i64 %33, -2
  %36 = add i64 %35, %_224.i
  %37 = shl i64 %a_significand.0.i, 2
  %_5.0.i42.i = or i64 %37, 18014398509481984
  %38 = zext i64 %_5.0.i42.i to i128
  %39 = zext i64 %36 to i128
  %40 = mul nuw i128 %39, %38
  %_2.i.i = lshr i128 %40, 64
  %41 = trunc i128 %_2.i.i to i64
  %42 = icmp ult i64 %41, 9007199254740992
  %_4.0.i106.i = lshr i128 %40, 65
  %_4.0.i.i = trunc i128 %_4.0.i106.i to i64
  %.sink.i = select i1 %42, i64 53, i64 52
  %_4.0.i.sink.i = select i1 %42, i64 %41, i64 %_4.0.i.i
  %43 = sext i1 %42 to i32
  %quotient_exponent.0.i = add nsw i32 %16, %43
  %_5.0.i45.i = shl i64 %a_significand.0.i, %.sink.i
  %44 = mul i64 %_4.0.i.sink.i, %12
  %45 = sub i64 %_5.0.i45.i, %44
  %46 = add nsw i32 %quotient_exponent.0.i, 1023
  %_265.i = icmp sgt i32 %quotient_exponent.0.i, 1023
  br i1 %_265.i, label %bb122.i, label %bb125.i

bb25.i:                                           ; preds = %start
  %47 = and i64 %0, 9223372036854775807
  %48 = and i64 %1, 9223372036854775807
  %49 = icmp ugt i64 %47, 9218868437227405312
  br i1 %49, label %bb29.i, label %bb32.i

bb32.i:                                           ; preds = %bb25.i
  %50 = icmp ugt i64 %48, 9218868437227405312
  br i1 %50, label %bb34.i, label %bb37.i

bb29.i:                                           ; preds = %bb25.i
  %51 = or i64 %0, 2251799813685248
  %52 = bitcast i64 %51 to double
  br label %_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit

bb37.i:                                           ; preds = %bb32.i
  %53 = icmp eq i64 %47, 9218868437227405312
  %54 = icmp eq i64 %48, 9218868437227405312
  br i1 %53, label %bb39.i, label %bb46.i

bb34.i:                                           ; preds = %bb32.i
  %55 = or i64 %1, 2251799813685248
  %56 = bitcast i64 %55 to double
  br label %_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit

bb46.i:                                           ; preds = %bb37.i
  br i1 %54, label %bb48.i, label %bb50.i

bb39.i:                                           ; preds = %bb37.i
  br i1 %54, label %_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit, label %bb43.i

bb43.i:                                           ; preds = %bb39.i
  %57 = and i64 %1, -9223372036854775808
  %58 = xor i64 %57, %0
  %59 = bitcast i64 %58 to double
  br label %_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit

bb50.i:                                           ; preds = %bb46.i
  %60 = icmp eq i64 %47, 0
  %61 = icmp eq i64 %48, 0
  br i1 %60, label %bb52.i, label %bb58.i

bb48.i:                                           ; preds = %bb46.i
  %62 = bitcast i64 %5 to double
  br label %_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit

bb58.i:                                           ; preds = %bb50.i
  br i1 %61, label %bb60.i, label %bb63.i

bb52.i:                                           ; preds = %bb50.i
  %63 = bitcast i64 %5 to double
  %spec.select.i = select i1 %61, double 0x7FF8000000000000, double %63
  br label %_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit

bb63.i:                                           ; preds = %bb58.i
  %64 = icmp ult i64 %47, 4503599627370496
  br i1 %64, label %bb65.i, label %bb67.i

bb60.i:                                           ; preds = %bb58.i
  %65 = or i64 %5, 9218868437227405312
  %66 = bitcast i64 %65 to double
  br label %_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit

bb67.i:                                           ; preds = %bb65.i, %bb63.i
  %a_significand.1.i = phi i64 [ %_10.i.i, %bb65.i ], [ %6, %bb63.i ]
  %scale.1.i = phi i32 [ %71, %bb65.i ], [ 0, %bb63.i ]
  %67 = icmp ult i64 %48, 4503599627370496
  br i1 %67, label %bb69.i, label %bb72.i

bb65.i:                                           ; preds = %bb63.i
  %68 = tail call i64 @llvm.ctlz.i64(i64 %6, i1 false) #14, !range !6
  %69 = trunc i64 %68 to i32
  %70 = add nuw nsw i64 %68, 53
  %71 = sub nsw i32 12, %69
  %72 = and i64 %70, 63
  %_10.i.i = shl i64 %6, %72
  br label %bb67.i

bb69.i:                                           ; preds = %bb67.i
  %73 = tail call i64 @llvm.ctlz.i64(i64 %7, i1 false) #14, !range !6
  %74 = trunc i64 %73 to i32
  %75 = add nuw nsw i64 %73, 53
  %.neg.i = add nsw i32 %74, -12
  %76 = and i64 %75, 63
  %_10.i44.i = shl i64 %7, %76
  %77 = add nsw i32 %.neg.i, %scale.1.i
  br label %bb72.i

bb125.i:                                          ; preds = %bb72.i
  %_272.i = icmp slt i32 %quotient_exponent.0.i, -1022
  br i1 %_272.i, label %bb126.i, label %bb128.i

bb122.i:                                          ; preds = %bb72.i
  %78 = or i64 %5, 9218868437227405312
  %79 = bitcast i64 %78 to double
  br label %_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit

bb128.i:                                          ; preds = %bb125.i
  %_5.0.i47.i = shl i64 %45, 1
  %80 = icmp ugt i64 %_5.0.i47.i, %12
  %81 = and i64 %_4.0.i.sink.i, 4503599627370495
  %82 = zext i32 %46 to i64
  %_5.0.i48.i = shl nuw nsw i64 %82, 52
  %83 = zext i1 %80 to i64
  %84 = or i64 %_5.0.i48.i, %81
  %85 = add nuw i64 %84, %83
  %86 = or i64 %85, %5
  %87 = bitcast i64 %86 to double
  br label %_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit

bb126.i:                                          ; preds = %bb125.i
  %88 = bitcast i64 %5 to double
  br label %_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit

_ZN17compiler_builtins5float3div5div6417hf9de202c258b0f03E.exit: ; preds = %bb29.i, %bb34.i, %bb39.i, %bb43.i, %bb48.i, %bb52.i, %bb60.i, %bb122.i, %bb128.i, %bb126.i
  %.1.i = phi double [ %52, %bb29.i ], [ %56, %bb34.i ], [ %59, %bb43.i ], [ %62, %bb48.i ], [ %66, %bb60.i ], [ 0x7FF8000000000000, %bb39.i ], [ %79, %bb122.i ], [ %88, %bb126.i ], [ %87, %bb128.i ], [ %spec.select.i, %bb52.i ]
  ret double %.1.i
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @__divdf3(double %a, double %b) unnamed_addr #1 {
start:
; call compiler_builtins::float::div::__divdf3
  %0 = tail call double @_ZN17compiler_builtins5float3div8__divdf317h9a15b37725ddfcf7E(double %a, double %b)
  ret double %0
}

; compiler_builtins::float::extend::__extendsfdf2
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float6extend13__extendsfdf217h943d2d697ea1a5e8E(float %a) unnamed_addr #1 {
start:
  %0 = bitcast float %a to i32
  %1 = and i32 %0, 2147483647
  %2 = add nsw i32 %1, -8388608
  %3 = icmp ult i32 %2, 2130706432
  br i1 %3, label %bb8.i, label %bb14.i

bb14.i:                                           ; preds = %start
  %4 = icmp ugt i32 %1, 2139095039
  br i1 %4, label %bb16.i, label %bb27.i

bb8.i:                                            ; preds = %start
  %5 = zext i32 %1 to i64
  %6 = shl nuw nsw i64 %5, 29
  %_4.0.i.i = add nuw nsw i64 %6, 4035225266123964416
  br label %_ZN17compiler_builtins5float6extend6extend17hfb6289bebb750eceE.exit

bb27.i:                                           ; preds = %bb14.i
  %.not.i = icmp eq i32 %1, 0
  br i1 %.not.i, label %_ZN17compiler_builtins5float6extend6extend17hfb6289bebb750eceE.exit, label %bb29.i

bb16.i:                                           ; preds = %bb14.i
  %7 = zext i32 %0 to i64
  %8 = shl nuw nsw i64 %7, 29
  %9 = or i64 %8, 9218868437227405312
  br label %_ZN17compiler_builtins5float6extend6extend17hfb6289bebb750eceE.exit

bb29.i:                                           ; preds = %bb27.i
  %10 = tail call i32 @llvm.ctlz.i32(i32 %1, i1 true) #14, !range !5
  %11 = zext i32 %1 to i64
  %_92.i = sub nuw nsw i32 905, %10
  %12 = zext i32 %_92.i to i64
  %_98.i = add nuw nsw i32 %10, 21
  %_4.i.i.i = zext i32 %_98.i to i64
  %13 = shl i64 %11, %_4.i.i.i
  %14 = xor i64 %13, 4503599627370496
  %15 = shl nuw nsw i64 %12, 52
  %16 = or i64 %14, %15
  br label %_ZN17compiler_builtins5float6extend6extend17hfb6289bebb750eceE.exit

_ZN17compiler_builtins5float6extend6extend17hfb6289bebb750eceE.exit: ; preds = %bb8.i, %bb27.i, %bb16.i, %bb29.i
  %abs_result.0.i = phi i64 [ %_4.0.i.i, %bb8.i ], [ %9, %bb16.i ], [ %16, %bb29.i ], [ 0, %bb27.i ]
  %17 = and i32 %0, -2147483648
  %18 = zext i32 %17 to i64
  %19 = shl nuw i64 %18, 32
  %20 = or i64 %abs_result.0.i, %19
  %21 = bitcast i64 %20 to double
  ret double %21
}

; compiler_builtins::float::mul::__mulsf3
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float3mul8__mulsf317ha0a3ca24c075e3c5E(float %a, float %b) unnamed_addr #1 {
start:
  %0 = bitcast float %a to i32
  %1 = bitcast float %b to i32
  %_5.0.i30.i = lshr i32 %0, 23
  %2 = and i32 %_5.0.i30.i, 255
  %_5.0.i31.i = lshr i32 %1, 23
  %3 = and i32 %_5.0.i31.i, 255
  %4 = xor i32 %1, %0
  %5 = and i32 %4, -2147483648
  %6 = and i32 %0, 8388607
  %7 = and i32 %1, 8388607
  %8 = add nsw i32 %2, -1
  %9 = icmp ugt i32 %8, 253
  %10 = add nsw i32 %3, -1
  %11 = icmp ugt i32 %10, 253
  %or.cond.i = select i1 %9, i1 true, i1 %11
  br i1 %or.cond.i, label %bb25.i, label %bb72.i

bb72.i:                                           ; preds = %bb69.i, %bb67.i, %start
  %a_significand.0.i = phi i32 [ %a_significand.1.i, %bb69.i ], [ %a_significand.1.i, %bb67.i ], [ %6, %start ]
  %b_significand.0.i = phi i32 [ %_10.i33.i, %bb69.i ], [ %7, %bb67.i ], [ %7, %start ]
  %scale.0.i = phi i32 [ %51, %bb69.i ], [ %scale.1.i, %bb67.i ], [ 0, %start ]
  %12 = or i32 %a_significand.0.i, 8388608
  %13 = shl i32 %b_significand.0.i, 8
  %_5.0.i32.i = or i32 %13, -2147483648
  %14 = zext i32 %12 to i64
  %15 = zext i32 %_5.0.i32.i to i64
  %16 = mul nuw i64 %15, %14
  %17 = trunc i64 %16 to i32
  %_2.i.i.i = lshr i64 %16, 32
  %18 = trunc i64 %_2.i.i.i to i32
  %19 = add nuw nsw i32 %3, %2
  %20 = add nsw i32 %19, %scale.0.i
  %21 = and i32 %18, 8388608
  %.not.i = icmp eq i32 %21, 0
  br i1 %.not.i, label %bb87.i, label %bb85.i

bb25.i:                                           ; preds = %start
  %22 = and i32 %0, 2147483647
  %23 = and i32 %1, 2147483647
  %24 = icmp ugt i32 %22, 2139095040
  br i1 %24, label %bb29.i, label %bb32.i

bb32.i:                                           ; preds = %bb25.i
  %25 = icmp ugt i32 %23, 2139095040
  br i1 %25, label %bb34.i, label %bb37.i

bb29.i:                                           ; preds = %bb25.i
  %26 = or i32 %0, 4194304
  %27 = bitcast i32 %26 to float
  br label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit

bb37.i:                                           ; preds = %bb32.i
  %28 = icmp eq i32 %22, 2139095040
  br i1 %28, label %bb39.i, label %bb46.i

bb34.i:                                           ; preds = %bb32.i
  %29 = or i32 %1, 4194304
  %30 = bitcast i32 %29 to float
  br label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit

bb46.i:                                           ; preds = %bb37.i
  %31 = icmp eq i32 %23, 2139095040
  %.not117.i = icmp eq i32 %22, 0
  br i1 %31, label %bb48.i, label %bb55.i

bb39.i:                                           ; preds = %bb37.i
  %.not118.i = icmp eq i32 %23, 0
  br i1 %.not118.i, label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit, label %bb41.i

bb41.i:                                           ; preds = %bb39.i
  %32 = and i32 %1, -2147483648
  %33 = xor i32 %32, %0
  %34 = bitcast i32 %33 to float
  br label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit

bb55.i:                                           ; preds = %bb46.i
  br i1 %.not117.i, label %bb57.i, label %bb59.i

bb48.i:                                           ; preds = %bb46.i
  br i1 %.not117.i, label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit, label %bb50.i

bb50.i:                                           ; preds = %bb48.i
  %35 = and i32 %0, -2147483648
  %36 = xor i32 %35, %1
  %37 = bitcast i32 %36 to float
  br label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit

bb59.i:                                           ; preds = %bb55.i
  %38 = icmp eq i32 %23, 0
  br i1 %38, label %bb61.i, label %bb63.i

bb57.i:                                           ; preds = %bb55.i
  %39 = bitcast i32 %5 to float
  br label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit

bb63.i:                                           ; preds = %bb59.i
  %40 = icmp ult i32 %22, 8388608
  br i1 %40, label %bb65.i, label %bb67.i

bb61.i:                                           ; preds = %bb59.i
  %41 = bitcast i32 %5 to float
  br label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit

bb67.i:                                           ; preds = %bb65.i, %bb63.i
  %a_significand.1.i = phi i32 [ %_10.i.i, %bb65.i ], [ %6, %bb63.i ]
  %scale.1.i = phi i32 [ %45, %bb65.i ], [ 0, %bb63.i ]
  %42 = icmp ult i32 %23, 8388608
  br i1 %42, label %bb69.i, label %bb72.i

bb65.i:                                           ; preds = %bb63.i
  %43 = tail call i32 @llvm.ctlz.i32(i32 %6, i1 false) #14, !range !5
  %44 = add nuw nsw i32 %43, 24
  %45 = sub nsw i32 9, %43
  %46 = and i32 %44, 31
  %_10.i.i = shl i32 %6, %46
  br label %bb67.i

bb69.i:                                           ; preds = %bb67.i
  %47 = tail call i32 @llvm.ctlz.i32(i32 %7, i1 false) #14, !range !5
  %48 = add nuw nsw i32 %47, 24
  %49 = sub nsw i32 9, %47
  %50 = and i32 %48, 31
  %_10.i33.i = shl i32 %7, %50
  %51 = add nsw i32 %49, %scale.1.i
  br label %bb72.i

bb87.i:                                           ; preds = %bb72.i
  %52 = add nsw i32 %20, -127
  %_5.0.i34116.i = shl nuw nsw i64 %_2.i.i.i, 1
  %_5.0.i34.i = trunc i64 %_5.0.i34116.i to i32
  %_5.0.i35.i = lshr i32 %17, 31
  %53 = or i32 %_5.0.i35.i, %_5.0.i34.i
  %_4.0.i.i = shl i32 %17, 1
  br label %bb92.i

bb85.i:                                           ; preds = %bb72.i
  %54 = add nsw i32 %20, -126
  br label %bb92.i

bb92.i:                                           ; preds = %bb85.i, %bb87.i
  %product_low.0.i = phi i32 [ %17, %bb85.i ], [ %_4.0.i.i, %bb87.i ]
  %product_high.0.i = phi i32 [ %18, %bb85.i ], [ %53, %bb87.i ]
  %product_exponent.0.i = phi i32 [ %54, %bb85.i ], [ %52, %bb87.i ]
  %_178.i = icmp sgt i32 %product_exponent.0.i, 254
  br i1 %_178.i, label %bb93.i, label %bb96.i

bb96.i:                                           ; preds = %bb92.i
  %_185.i = icmp slt i32 %product_exponent.0.i, 1
  br i1 %_185.i, label %bb97.i, label %bb98.i

bb93.i:                                           ; preds = %bb92.i
  %55 = or i32 %5, 2139095040
  %56 = bitcast i32 %55 to float
  br label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit

bb98.i:                                           ; preds = %bb96.i
  %57 = and i32 %product_high.0.i, 8388607
  %_5.0.i36.i = shl nuw nsw i32 %product_exponent.0.i, 23
  %58 = or i32 %_5.0.i36.i, %57
  br label %bb125.i

bb97.i:                                           ; preds = %bb96.i
  %59 = sub nsw i32 1, %product_exponent.0.i
  %_192.i = icmp ugt i32 %59, 31
  br i1 %_192.i, label %bb102.i, label %bb105.i

bb102.i:                                          ; preds = %bb97.i
  %60 = bitcast i32 %5 to float
  br label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit

bb105.i:                                          ; preds = %bb97.i
  %_201.i = add nsw i32 %product_exponent.0.i, 31
  %_5.0.i37.i = shl i32 %product_low.0.i, %_201.i
  %_5.0.i38.i = shl i32 %product_high.0.i, %_201.i
  %_5.0.i39.i = lshr i32 %product_low.0.i, %59
  %61 = or i32 %_5.0.i39.i, %_5.0.i38.i
  %62 = or i32 %61, %_5.0.i37.i
  %_4.0.i40.i = lshr i32 %product_high.0.i, %59
  br label %bb125.i

bb125.i:                                          ; preds = %bb105.i, %bb98.i
  %product_low.1.i = phi i32 [ %62, %bb105.i ], [ %product_low.0.i, %bb98.i ]
  %product_high.1.i = phi i32 [ %_4.0.i40.i, %bb105.i ], [ %58, %bb98.i ]
  %63 = or i32 %product_high.1.i, %5
  %64 = icmp ugt i32 %product_low.1.i, -2147483648
  br i1 %64, label %bb130.thread.i, label %bb130.i

bb130.i:                                          ; preds = %bb125.i
  %65 = icmp eq i32 %product_low.1.i, -2147483648
  br i1 %65, label %bb132.i, label %bb135.i

bb130.thread.i:                                   ; preds = %bb125.i
  %_4.0.i41.i = add i32 %63, 1
  br label %bb135.i

bb135.i:                                          ; preds = %bb132.i, %bb130.thread.i, %bb130.i
  %product_high.3.i = phi i32 [ %_4.0.i42.i, %bb132.i ], [ %63, %bb130.i ], [ %_4.0.i41.i, %bb130.thread.i ]
  %66 = bitcast i32 %product_high.3.i to float
  br label %_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit

bb132.i:                                          ; preds = %bb130.i
  %67 = and i32 %product_high.1.i, 1
  %_4.0.i42.i = add i32 %63, %67
  br label %bb135.i

_ZN17compiler_builtins5float3mul3mul17h88d6793952e2a007E.exit: ; preds = %bb29.i, %bb34.i, %bb39.i, %bb41.i, %bb48.i, %bb50.i, %bb57.i, %bb61.i, %bb93.i, %bb102.i, %bb135.i
  %.3.i = phi float [ %66, %bb135.i ], [ %27, %bb29.i ], [ %30, %bb34.i ], [ %34, %bb41.i ], [ %37, %bb50.i ], [ %39, %bb57.i ], [ %41, %bb61.i ], [ 0x7FF8000000000000, %bb39.i ], [ 0x7FF8000000000000, %bb48.i ], [ %56, %bb93.i ], [ %60, %bb102.i ]
  ret float %.3.i
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @__mulsf3(float %a, float %b) unnamed_addr #1 {
start:
; call compiler_builtins::float::mul::__mulsf3
  %0 = tail call float @_ZN17compiler_builtins5float3mul8__mulsf317ha0a3ca24c075e3c5E(float %a, float %b)
  ret float %0
}

; compiler_builtins::float::mul::__muldf3
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float3mul8__muldf317ha331fc5a60651d3eE(double %a, double %b) unnamed_addr #1 {
start:
  %0 = bitcast double %a to i64
  %1 = bitcast double %b to i64
  %_5.0.i30.i = lshr i64 %0, 52
  %2 = and i64 %_5.0.i30.i, 2047
  %_5.0.i31.i = lshr i64 %1, 52
  %3 = and i64 %_5.0.i31.i, 2047
  %4 = xor i64 %1, %0
  %5 = and i64 %4, -9223372036854775808
  %6 = and i64 %0, 4503599627370495
  %7 = and i64 %1, 4503599627370495
  %8 = add nsw i64 %2, -1
  %9 = icmp ugt i64 %8, 2045
  %10 = add nsw i64 %3, -1
  %11 = icmp ugt i64 %10, 2045
  %or.cond.i = select i1 %9, i1 true, i1 %11
  br i1 %or.cond.i, label %bb25.i, label %bb72.i

bb72.i:                                           ; preds = %bb69.i, %bb67.i, %start
  %a_significand.0.i = phi i64 [ %a_significand.1.i, %bb69.i ], [ %a_significand.1.i, %bb67.i ], [ %6, %start ]
  %b_significand.0.i = phi i64 [ %_10.i33.i, %bb69.i ], [ %7, %bb67.i ], [ %7, %start ]
  %scale.0.i = phi i32 [ %55, %bb69.i ], [ %scale.1.i, %bb67.i ], [ 0, %start ]
  %12 = or i64 %a_significand.0.i, 4503599627370496
  %13 = shl i64 %b_significand.0.i, 11
  %_5.0.i32.i = or i64 %13, -9223372036854775808
  %14 = zext i64 %12 to i128
  %15 = zext i64 %_5.0.i32.i to i128
  %16 = mul nuw i128 %15, %14
  %17 = trunc i128 %16 to i64
  %_2.i.i.i = lshr i128 %16, 64
  %18 = trunc i128 %_2.i.i.i to i64
  %19 = trunc i64 %2 to i32
  %20 = trunc i64 %3 to i32
  %21 = add nuw nsw i32 %20, %19
  %22 = add nsw i32 %21, %scale.0.i
  %23 = and i64 %18, 4503599627370496
  %.not.i = icmp eq i64 %23, 0
  br i1 %.not.i, label %bb87.i, label %bb85.i

bb25.i:                                           ; preds = %start
  %24 = and i64 %0, 9223372036854775807
  %25 = and i64 %1, 9223372036854775807
  %26 = icmp ugt i64 %24, 9218868437227405312
  br i1 %26, label %bb29.i, label %bb32.i

bb32.i:                                           ; preds = %bb25.i
  %27 = icmp ugt i64 %25, 9218868437227405312
  br i1 %27, label %bb34.i, label %bb37.i

bb29.i:                                           ; preds = %bb25.i
  %28 = or i64 %0, 2251799813685248
  %29 = bitcast i64 %28 to double
  br label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit

bb37.i:                                           ; preds = %bb32.i
  %30 = icmp eq i64 %24, 9218868437227405312
  br i1 %30, label %bb39.i, label %bb46.i

bb34.i:                                           ; preds = %bb32.i
  %31 = or i64 %1, 2251799813685248
  %32 = bitcast i64 %31 to double
  br label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit

bb46.i:                                           ; preds = %bb37.i
  %33 = icmp eq i64 %25, 9218868437227405312
  %.not117.i = icmp eq i64 %24, 0
  br i1 %33, label %bb48.i, label %bb55.i

bb39.i:                                           ; preds = %bb37.i
  %.not118.i = icmp eq i64 %25, 0
  br i1 %.not118.i, label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit, label %bb41.i

bb41.i:                                           ; preds = %bb39.i
  %34 = and i64 %1, -9223372036854775808
  %35 = xor i64 %34, %0
  %36 = bitcast i64 %35 to double
  br label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit

bb55.i:                                           ; preds = %bb46.i
  br i1 %.not117.i, label %bb57.i, label %bb59.i

bb48.i:                                           ; preds = %bb46.i
  br i1 %.not117.i, label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit, label %bb50.i

bb50.i:                                           ; preds = %bb48.i
  %37 = and i64 %0, -9223372036854775808
  %38 = xor i64 %37, %1
  %39 = bitcast i64 %38 to double
  br label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit

bb59.i:                                           ; preds = %bb55.i
  %40 = icmp eq i64 %25, 0
  br i1 %40, label %bb61.i, label %bb63.i

bb57.i:                                           ; preds = %bb55.i
  %41 = bitcast i64 %5 to double
  br label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit

bb63.i:                                           ; preds = %bb59.i
  %42 = icmp ult i64 %24, 4503599627370496
  br i1 %42, label %bb65.i, label %bb67.i

bb61.i:                                           ; preds = %bb59.i
  %43 = bitcast i64 %5 to double
  br label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit

bb67.i:                                           ; preds = %bb65.i, %bb63.i
  %a_significand.1.i = phi i64 [ %_10.i.i, %bb65.i ], [ %6, %bb63.i ]
  %scale.1.i = phi i32 [ %48, %bb65.i ], [ 0, %bb63.i ]
  %44 = icmp ult i64 %25, 4503599627370496
  br i1 %44, label %bb69.i, label %bb72.i

bb65.i:                                           ; preds = %bb63.i
  %45 = tail call i64 @llvm.ctlz.i64(i64 %6, i1 false) #14, !range !6
  %46 = trunc i64 %45 to i32
  %47 = add nuw nsw i64 %45, 53
  %48 = sub nsw i32 12, %46
  %49 = and i64 %47, 63
  %_10.i.i = shl i64 %6, %49
  br label %bb67.i

bb69.i:                                           ; preds = %bb67.i
  %50 = tail call i64 @llvm.ctlz.i64(i64 %7, i1 false) #14, !range !6
  %51 = trunc i64 %50 to i32
  %52 = add nuw nsw i64 %50, 53
  %53 = sub nsw i32 12, %51
  %54 = and i64 %52, 63
  %_10.i33.i = shl i64 %7, %54
  %55 = add nsw i32 %53, %scale.1.i
  br label %bb72.i

bb87.i:                                           ; preds = %bb72.i
  %56 = add nsw i32 %22, -1023
  %_5.0.i34116.i = shl nuw nsw i128 %_2.i.i.i, 1
  %_5.0.i34.i = trunc i128 %_5.0.i34116.i to i64
  %_5.0.i35.i = lshr i64 %17, 63
  %57 = or i64 %_5.0.i35.i, %_5.0.i34.i
  %_4.0.i.i = shl i64 %17, 1
  br label %bb92.i

bb85.i:                                           ; preds = %bb72.i
  %58 = add nsw i32 %22, -1022
  br label %bb92.i

bb92.i:                                           ; preds = %bb85.i, %bb87.i
  %product_low.0.i = phi i64 [ %17, %bb85.i ], [ %_4.0.i.i, %bb87.i ]
  %product_high.0.i = phi i64 [ %18, %bb85.i ], [ %57, %bb87.i ]
  %product_exponent.0.i = phi i32 [ %58, %bb85.i ], [ %56, %bb87.i ]
  %_178.i = icmp sgt i32 %product_exponent.0.i, 2046
  br i1 %_178.i, label %bb93.i, label %bb96.i

bb96.i:                                           ; preds = %bb92.i
  %_185.i = icmp slt i32 %product_exponent.0.i, 1
  br i1 %_185.i, label %bb97.i, label %bb98.i

bb93.i:                                           ; preds = %bb92.i
  %59 = or i64 %5, 9218868437227405312
  %60 = bitcast i64 %59 to double
  br label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit

bb98.i:                                           ; preds = %bb96.i
  %61 = and i64 %product_high.0.i, 4503599627370495
  %62 = zext i32 %product_exponent.0.i to i64
  %_5.0.i36.i = shl nuw nsw i64 %62, 52
  %63 = or i64 %_5.0.i36.i, %61
  br label %bb125.i

bb97.i:                                           ; preds = %bb96.i
  %64 = sub nsw i32 1, %product_exponent.0.i
  %_192.i = icmp ugt i32 %64, 63
  br i1 %_192.i, label %bb102.i, label %bb105.i

bb102.i:                                          ; preds = %bb97.i
  %65 = bitcast i64 %5 to double
  br label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit

bb105.i:                                          ; preds = %bb97.i
  %_201.i = add nsw i32 %product_exponent.0.i, 63
  %66 = zext i32 %_201.i to i64
  %_5.0.i37.i = shl i64 %product_low.0.i, %66
  %_5.0.i38.i = shl i64 %product_high.0.i, %66
  %67 = zext i32 %64 to i64
  %_5.0.i39.i = lshr i64 %product_low.0.i, %67
  %68 = or i64 %_5.0.i39.i, %_5.0.i38.i
  %69 = or i64 %68, %_5.0.i37.i
  %_4.0.i40.i = lshr i64 %product_high.0.i, %67
  br label %bb125.i

bb125.i:                                          ; preds = %bb105.i, %bb98.i
  %product_low.1.i = phi i64 [ %69, %bb105.i ], [ %product_low.0.i, %bb98.i ]
  %product_high.1.i = phi i64 [ %_4.0.i40.i, %bb105.i ], [ %63, %bb98.i ]
  %70 = or i64 %product_high.1.i, %5
  %71 = icmp ugt i64 %product_low.1.i, -9223372036854775808
  br i1 %71, label %bb130.thread.i, label %bb130.i

bb130.i:                                          ; preds = %bb125.i
  %72 = icmp eq i64 %product_low.1.i, -9223372036854775808
  br i1 %72, label %bb132.i, label %bb135.i

bb130.thread.i:                                   ; preds = %bb125.i
  %_4.0.i41.i = add i64 %70, 1
  br label %bb135.i

bb135.i:                                          ; preds = %bb132.i, %bb130.thread.i, %bb130.i
  %product_high.3.i = phi i64 [ %_4.0.i42.i, %bb132.i ], [ %70, %bb130.i ], [ %_4.0.i41.i, %bb130.thread.i ]
  %73 = bitcast i64 %product_high.3.i to double
  br label %_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit

bb132.i:                                          ; preds = %bb130.i
  %74 = and i64 %product_high.1.i, 1
  %_4.0.i42.i = add i64 %70, %74
  br label %bb135.i

_ZN17compiler_builtins5float3mul3mul17h6fad9fc71b0d13d8E.exit: ; preds = %bb29.i, %bb34.i, %bb39.i, %bb41.i, %bb48.i, %bb50.i, %bb57.i, %bb61.i, %bb93.i, %bb102.i, %bb135.i
  %.3.i = phi double [ %73, %bb135.i ], [ %29, %bb29.i ], [ %32, %bb34.i ], [ %36, %bb41.i ], [ %39, %bb50.i ], [ %41, %bb57.i ], [ %43, %bb61.i ], [ 0x7FF8000000000000, %bb39.i ], [ 0x7FF8000000000000, %bb48.i ], [ %60, %bb93.i ], [ %65, %bb102.i ]
  ret double %.3.i
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @__muldf3(double %a, double %b) unnamed_addr #1 {
start:
; call compiler_builtins::float::mul::__muldf3
  %0 = tail call double @_ZN17compiler_builtins5float3mul8__muldf317ha331fc5a60651d3eE(double %a, double %b)
  ret double %0
}

; compiler_builtins::float::pow::__powisf2
; Function Attrs: nofree nosync nounwind nonlazybind readnone uwtable
define hidden float @_ZN17compiler_builtins5float3pow9__powisf217hc443ebb069659ce1E(float %a, i32 %b) unnamed_addr #3 {
start:
  %0 = tail call i32 @llvm.abs.i32(i32 %b, i1 false) #14
  %_99.i = and i32 %0, 1
  %1 = icmp eq i32 %_99.i, 0
  %spec.select10.i = select i1 %1, float 1.000000e+00, float %a
  %2 = icmp ult i32 %0, 2
  br i1 %2, label %_ZN17compiler_builtins5float3pow3pow17hfe2ef3b581ccf326E.exit, label %bb8.i

bb8.i:                                            ; preds = %start, %bb8.i
  %spec.select13.i = phi float [ %spec.select.i, %bb8.i ], [ %spec.select10.i, %start ]
  %pow.012.i = phi i32 [ %3, %bb8.i ], [ %0, %start ]
  %a1.011.i = phi float [ %4, %bb8.i ], [ %a, %start ]
  %3 = lshr i32 %pow.012.i, 1
  %4 = fmul float %a1.011.i, %a1.011.i
  %5 = and i32 %pow.012.i, 2
  %6 = icmp eq i32 %5, 0
  %7 = fmul float %spec.select13.i, %4
  %spec.select.i = select i1 %6, float %spec.select13.i, float %7
  %8 = icmp ult i32 %pow.012.i, 4
  br i1 %8, label %_ZN17compiler_builtins5float3pow3pow17hfe2ef3b581ccf326E.exit, label %bb8.i

_ZN17compiler_builtins5float3pow3pow17hfe2ef3b581ccf326E.exit: ; preds = %bb8.i, %start
  %spec.select.lcssa.i = phi float [ %spec.select10.i, %start ], [ %spec.select.i, %bb8.i ]
  %recip.i = icmp slt i32 %b, 0
  %9 = fdiv float 1.000000e+00, %spec.select.lcssa.i
  %.0.i = select i1 %recip.i, float %9, float %spec.select.lcssa.i
  ret float %.0.i
}

; compiler_builtins::float::pow::__powidf2
; Function Attrs: nofree nosync nounwind nonlazybind readnone uwtable
define hidden double @_ZN17compiler_builtins5float3pow9__powidf217hb44d86f49244a064E(double %a, i32 %b) unnamed_addr #3 {
start:
  %0 = tail call i32 @llvm.abs.i32(i32 %b, i1 false) #14
  %_99.i = and i32 %0, 1
  %1 = icmp eq i32 %_99.i, 0
  %spec.select10.i = select i1 %1, double 1.000000e+00, double %a
  %2 = icmp ult i32 %0, 2
  br i1 %2, label %_ZN17compiler_builtins5float3pow3pow17h33927b5f3ec32e28E.exit, label %bb8.i

bb8.i:                                            ; preds = %start, %bb8.i
  %spec.select13.i = phi double [ %spec.select.i, %bb8.i ], [ %spec.select10.i, %start ]
  %pow.012.i = phi i32 [ %3, %bb8.i ], [ %0, %start ]
  %a1.011.i = phi double [ %4, %bb8.i ], [ %a, %start ]
  %3 = lshr i32 %pow.012.i, 1
  %4 = fmul double %a1.011.i, %a1.011.i
  %5 = and i32 %pow.012.i, 2
  %6 = icmp eq i32 %5, 0
  %7 = fmul double %spec.select13.i, %4
  %spec.select.i = select i1 %6, double %spec.select13.i, double %7
  %8 = icmp ult i32 %pow.012.i, 4
  br i1 %8, label %_ZN17compiler_builtins5float3pow3pow17h33927b5f3ec32e28E.exit, label %bb8.i

_ZN17compiler_builtins5float3pow3pow17h33927b5f3ec32e28E.exit: ; preds = %bb8.i, %start
  %spec.select.lcssa.i = phi double [ %spec.select10.i, %start ], [ %spec.select.i, %bb8.i ]
  %recip.i = icmp slt i32 %b, 0
  %9 = fdiv double 1.000000e+00, %spec.select.lcssa.i
  %.0.i = select i1 %recip.i, double %9, double %spec.select.lcssa.i
  ret double %.0.i
}

; compiler_builtins::float::sub::__subsf3
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float3sub8__subsf317h6c8c1b3577b4cce9E(float %a, float %b) unnamed_addr #1 {
start:
  %0 = bitcast float %b to i32
  %_5 = xor i32 %0, -2147483648
  %1 = bitcast i32 %_5 to float
; call compiler_builtins::float::add::__addsf3
  %2 = tail call float @_ZN17compiler_builtins5float3add8__addsf317hb95467ebe387ff0cE(float %a, float %1)
  ret float %2
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @__subsf3(float %a, float %b) unnamed_addr #1 {
start:
  %0 = bitcast float %b to i32
  %_5.i = xor i32 %0, -2147483648
  %1 = bitcast i32 %_5.i to float
; call compiler_builtins::float::add::__addsf3
  %2 = tail call float @_ZN17compiler_builtins5float3add8__addsf317hb95467ebe387ff0cE(float %a, float %1) #14
  ret float %2
}

; compiler_builtins::float::sub::__subdf3
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @_ZN17compiler_builtins5float3sub8__subdf317hc55e745fab6244d9E(double %a, double %b) unnamed_addr #1 {
start:
  %0 = bitcast double %b to i64
  %_5 = xor i64 %0, -9223372036854775808
  %1 = bitcast i64 %_5 to double
; call compiler_builtins::float::add::__adddf3
  %2 = tail call double @_ZN17compiler_builtins5float3add8__adddf317h9c1cc12e52bbedbeE(double %a, double %1)
  ret double %2
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @__subdf3(double %a, double %b) unnamed_addr #1 {
start:
  %0 = bitcast double %b to i64
  %_5.i = xor i64 %0, -9223372036854775808
  %1 = bitcast i64 %_5.i to double
; call compiler_builtins::float::add::__adddf3
  %2 = tail call double @_ZN17compiler_builtins5float3add8__adddf317h9c1cc12e52bbedbeE(double %a, double %1) #14
  ret double %2
}

; compiler_builtins::float::trunc::__truncdfsf2
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @_ZN17compiler_builtins5float5trunc12__truncdfsf217h482318a1628cd545E(double %a) unnamed_addr #2 {
start:
  %0 = bitcast double %a to i64
  %1 = and i64 %0, 9223372036854775807
  %2 = add nsw i64 %1, -4039728865751334912
  %3 = add nsw i64 %1, -5183643171103440896
  %4 = icmp ult i64 %2, %3
  br i1 %4, label %bb20.i, label %bb37.i

bb37.i:                                           ; preds = %start
  %5 = icmp ugt i64 %1, 9218868437227405312
  br i1 %5, label %bb39.i, label %bb47.i

bb20.i:                                           ; preds = %start
  %_5.0.i.i = lshr i64 %0, 29
  %6 = trunc i64 %_5.0.i.i to i32
  %7 = add i32 %6, 1073741824
  %8 = and i64 %0, 536870911
  %9 = icmp ugt i64 %8, 268435456
  br i1 %9, label %bb28.i, label %bb30.i

bb30.i:                                           ; preds = %bb20.i
  %10 = icmp eq i64 %8, 268435456
  br i1 %10, label %bb32.i, label %_ZN17compiler_builtins5float5trunc5trunc17h7e166f301a536819E.exit

bb28.i:                                           ; preds = %bb20.i
  %_4.0.i.i = add i32 %6, 1073741825
  br label %_ZN17compiler_builtins5float5trunc5trunc17h7e166f301a536819E.exit

bb32.i:                                           ; preds = %bb30.i
  %11 = and i32 %6, 1
  %_4.0.i9.i = add i32 %7, %11
  br label %_ZN17compiler_builtins5float5trunc5trunc17h7e166f301a536819E.exit

bb47.i:                                           ; preds = %bb37.i
  %12 = icmp ugt i64 %1, 5183643171103440895
  br i1 %12, label %_ZN17compiler_builtins5float5trunc5trunc17h7e166f301a536819E.exit, label %bb51.i

bb39.i:                                           ; preds = %bb37.i
  %13 = lshr i64 %0, 29
  %14 = trunc i64 %13 to i32
  %15 = and i32 %14, 4194303
  %16 = or i32 %15, 2143289344
  br label %_ZN17compiler_builtins5float5trunc5trunc17h7e166f301a536819E.exit

bb51.i:                                           ; preds = %bb47.i
  %_5.0.i11.i = lshr i64 %1, 52
  %17 = trunc i64 %_5.0.i11.i to i32
  %shift.i = sub nsw i32 897, %17
  %18 = and i64 %0, 4503599627370495
  %19 = or i64 %18, 4503599627370496
  %_141.i = icmp ugt i32 %shift.i, 52
  br i1 %_141.i, label %_ZN17compiler_builtins5float5trunc5trunc17h7e166f301a536819E.exit, label %bb58.i

bb58.i:                                           ; preds = %bb51.i
  %_149.i = add nuw nsw i64 %_5.0.i11.i, 63
  %20 = and i64 %_149.i, 63
  %_5.0.i12.i = shl i64 %19, %20
  %.not.i = icmp ne i64 %_5.0.i12.i, 0
  %sticky.0.i = zext i1 %.not.i to i64
  %21 = zext i32 %shift.i to i64
  %_5.0.i13.i = lshr i64 %19, %21
  %_5.0.i14.i = lshr i64 %_5.0.i13.i, 29
  %22 = trunc i64 %_5.0.i14.i to i32
  %_5.0.i13.masked.i = and i64 %_5.0.i13.i, 536870911
  %23 = or i64 %_5.0.i13.masked.i, %sticky.0.i
  %24 = icmp ugt i64 %23, 268435456
  br i1 %24, label %bb70.i, label %bb72.i

bb72.i:                                           ; preds = %bb58.i
  %25 = icmp eq i64 %23, 268435456
  br i1 %25, label %bb74.i, label %_ZN17compiler_builtins5float5trunc5trunc17h7e166f301a536819E.exit

bb70.i:                                           ; preds = %bb58.i
  %_4.0.i15.i = add i32 %22, 1
  br label %_ZN17compiler_builtins5float5trunc5trunc17h7e166f301a536819E.exit

bb74.i:                                           ; preds = %bb72.i
  %26 = and i32 %22, 1
  %_4.0.i16.i = add i32 %26, %22
  br label %_ZN17compiler_builtins5float5trunc5trunc17h7e166f301a536819E.exit

_ZN17compiler_builtins5float5trunc5trunc17h7e166f301a536819E.exit: ; preds = %bb30.i, %bb28.i, %bb32.i, %bb47.i, %bb39.i, %bb51.i, %bb72.i, %bb70.i, %bb74.i
  %abs_result.1.i = phi i32 [ %16, %bb39.i ], [ %_4.0.i.i, %bb28.i ], [ %_4.0.i9.i, %bb32.i ], [ %7, %bb30.i ], [ 2139095040, %bb47.i ], [ 0, %bb51.i ], [ %_4.0.i15.i, %bb70.i ], [ %_4.0.i16.i, %bb74.i ], [ %22, %bb72.i ]
  %27 = lshr i64 %0, 32
  %28 = trunc i64 %27 to i32
  %29 = and i32 %28, -2147483648
  %30 = or i32 %abs_result.1.i, %29
  %31 = bitcast i32 %30 to float
  ret float %31
}

; <f32 as compiler_builtins::float::Float>::signed_repr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$11signed_repr17hc2ac4eb884d62273E"(float %self) unnamed_addr #2 {
start:
  %0 = bitcast float %self to i32
  ret i32 %0
}

; <f32 as compiler_builtins::float::Float>::eq_repr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$7eq_repr17h74e767c7acddd9feE"(float %self, float %rhs) unnamed_addr #2 {
start:
  %0 = fcmp uno float %self, 0.000000e+00
  %1 = fcmp uno float %rhs, 0.000000e+00
  %or.cond = select i1 %0, i1 %1, i1 false
  br i1 %or.cond, label %bb10, label %bb7

bb7:                                              ; preds = %start
  %2 = bitcast float %self to i32
  %3 = bitcast float %rhs to i32
  %4 = icmp eq i32 %2, %3
  br label %bb10

bb10:                                             ; preds = %start, %bb7
  %.0 = phi i1 [ %4, %bb7 ], [ true, %start ]
  ret i1 %.0
}

; <f32 as compiler_builtins::float::Float>::sign
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$4sign17hde24a636da3c0fdeE"(float %self) unnamed_addr #2 {
start:
  %0 = bitcast float %self to i32
  %1 = icmp slt i32 %0, 0
  ret i1 %1
}

; <f32 as compiler_builtins::float::Float>::exp
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$3exp17he4fdc4fbb7f928f5E"(float %self) unnamed_addr #2 {
start:
  %0 = bitcast float %self to i32
  %_3 = lshr i32 %0, 23
  %1 = trunc i32 %_3 to i16
  %2 = and i16 %1, 255
  ret i16 %2
}

; <f32 as compiler_builtins::float::Float>::frac
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$4frac17h20cdc83a93c11dd5E"(float %self) unnamed_addr #2 {
start:
  %0 = bitcast float %self to i32
  %1 = and i32 %0, 8388607
  ret i32 %1
}

; <f32 as compiler_builtins::float::Float>::imp_frac
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$8imp_frac17h055a13976df59fa4E"(float %self) unnamed_addr #2 {
start:
  %0 = bitcast float %self to i32
  %1 = and i32 %0, 8388607
  %2 = or i32 %1, 8388608
  ret i32 %2
}

; <f32 as compiler_builtins::float::Float>::from_repr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$9from_repr17h877e5067b71e48daE"(i32 %a) unnamed_addr #2 {
start:
  %0 = bitcast i32 %a to float
  ret float %0
}

; <f32 as compiler_builtins::float::Float>::from_parts
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden float @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$10from_parts17h42e6f262af31af93E"(i1 zeroext %sign, i32 %exponent, i32 %significand) unnamed_addr #2 {
start:
  %_6 = select i1 %sign, i32 -2147483648, i32 0
  %_11 = shl i32 %exponent, 23
  %_10 = and i32 %_11, 2139095040
  %_5 = or i32 %_10, %_6
  %_13 = and i32 %significand, 8388607
  %_4 = or i32 %_5, %_13
  %0 = bitcast i32 %_4 to float
  ret float %0
}

; <f32 as compiler_builtins::float::Float>::normalize
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i32, i32 } @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$9normalize17h4016e8cc858a05d1E"(i32 %significand) unnamed_addr #1 {
start:
  %0 = tail call i32 @llvm.ctlz.i32(i32 %significand, i1 false) #14, !range !5
  %1 = add nuw nsw i32 %0, 24
  %2 = sub nsw i32 9, %0
  %3 = and i32 %1, 31
  %_10 = shl i32 %significand, %3
  %4 = insertvalue { i32, i32 } undef, i32 %2, 0
  %5 = insertvalue { i32, i32 } %4, i32 %_10, 1
  ret { i32, i32 } %5
}

; <f32 as compiler_builtins::float::Float>::is_subnormal
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN55_$LT$f32$u20$as$u20$compiler_builtins..float..Float$GT$12is_subnormal17h576984067d197a06E"(float %self) unnamed_addr #2 {
start:
  %0 = bitcast float %self to i32
  %_2 = and i32 %0, 2139095040
  %1 = icmp eq i32 %_2, 0
  ret i1 %1
}

; <f64 as compiler_builtins::float::Float>::signed_repr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$11signed_repr17h268d1dae8750bed5E"(double %self) unnamed_addr #2 {
start:
  %0 = bitcast double %self to i64
  ret i64 %0
}

; <f64 as compiler_builtins::float::Float>::eq_repr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$7eq_repr17hdadad2e08211786fE"(double %self, double %rhs) unnamed_addr #2 {
start:
  %0 = fcmp uno double %self, 0.000000e+00
  %1 = fcmp uno double %rhs, 0.000000e+00
  %or.cond = select i1 %0, i1 %1, i1 false
  br i1 %or.cond, label %bb10, label %bb7

bb7:                                              ; preds = %start
  %2 = bitcast double %self to i64
  %3 = bitcast double %rhs to i64
  %4 = icmp eq i64 %2, %3
  br label %bb10

bb10:                                             ; preds = %start, %bb7
  %.0 = phi i1 [ %4, %bb7 ], [ true, %start ]
  ret i1 %.0
}

; <f64 as compiler_builtins::float::Float>::sign
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$4sign17h31ee4d05e7fb51f9E"(double %self) unnamed_addr #2 {
start:
  %0 = bitcast double %self to i64
  %1 = icmp slt i64 %0, 0
  ret i1 %1
}

; <f64 as compiler_builtins::float::Float>::exp
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$3exp17h97fdbb3874ab72c0E"(double %self) unnamed_addr #2 {
start:
  %0 = bitcast double %self to i64
  %_3 = lshr i64 %0, 52
  %1 = trunc i64 %_3 to i16
  %2 = and i16 %1, 2047
  ret i16 %2
}

; <f64 as compiler_builtins::float::Float>::frac
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$4frac17h953882f41759f642E"(double %self) unnamed_addr #2 {
start:
  %0 = bitcast double %self to i64
  %1 = and i64 %0, 4503599627370495
  ret i64 %1
}

; <f64 as compiler_builtins::float::Float>::imp_frac
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$8imp_frac17hb45b7d4bd6a01f18E"(double %self) unnamed_addr #2 {
start:
  %0 = bitcast double %self to i64
  %1 = and i64 %0, 4503599627370495
  %2 = or i64 %1, 4503599627370496
  ret i64 %2
}

; <f64 as compiler_builtins::float::Float>::from_repr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$9from_repr17h629b5ff075670a59E"(i64 %a) unnamed_addr #2 {
start:
  %0 = bitcast i64 %a to double
  ret double %0
}

; <f64 as compiler_builtins::float::Float>::from_parts
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden double @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$10from_parts17hc6594e02855b54edE"(i1 zeroext %sign, i64 %exponent, i64 %significand) unnamed_addr #2 {
start:
  %_6 = select i1 %sign, i64 -9223372036854775808, i64 0
  %_11 = shl i64 %exponent, 52
  %_10 = and i64 %_11, 9218868437227405312
  %_5 = or i64 %_10, %_6
  %_13 = and i64 %significand, 4503599627370495
  %_4 = or i64 %_5, %_13
  %0 = bitcast i64 %_4 to double
  ret double %0
}

; <f64 as compiler_builtins::float::Float>::normalize
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i32, i64 } @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$9normalize17hcb2c44e2f883ebe0E"(i64 %significand) unnamed_addr #1 {
start:
  %0 = tail call i64 @llvm.ctlz.i64(i64 %significand, i1 false) #14, !range !6
  %1 = trunc i64 %0 to i32
  %2 = add nuw nsw i64 %0, 53
  %3 = sub nsw i32 12, %1
  %4 = and i64 %2, 63
  %_10 = shl i64 %significand, %4
  %5 = insertvalue { i32, i64 } undef, i32 %3, 0
  %6 = insertvalue { i32, i64 } %5, i64 %_10, 1
  ret { i32, i64 } %6
}

; <f64 as compiler_builtins::float::Float>::is_subnormal
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN55_$LT$f64$u20$as$u20$compiler_builtins..float..Float$GT$12is_subnormal17h54b28cca08a4e3e9E"(double %self) unnamed_addr #2 {
start:
  %0 = bitcast double %self to i64
  %_2 = and i64 %0, 9218868437227405312
  %1 = icmp eq i64 %_2, 0
  ret i1 %1
}

; compiler_builtins::int::addsub::__rust_i128_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int6addsub15__rust_i128_add17he0810486ef38d614E(i128 %a, i128 %b) unnamed_addr #1 {
start:
  %0 = trunc i128 %a to i64
  %1 = trunc i128 %b to i64
  %2 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %0, i64 %1) #14
  %3 = extractvalue { i64, i1 } %2, 0
  %4 = extractvalue { i64, i1 } %2, 1
  %_2.i.i.i = lshr i128 %a, 64
  %5 = trunc i128 %_2.i.i.i to i64
  %_2.i4.i.i = lshr i128 %b, 64
  %6 = trunc i128 %_2.i4.i.i to i64
  %7 = add i64 %6, %5
  %8 = zext i1 %4 to i64
  %spec.select.i.i = add i64 %7, %8
  %9 = zext i64 %3 to i128
  %_2.i.i.i.i = zext i64 %spec.select.i.i to i128
  %10 = shl nuw i128 %_2.i.i.i.i, 64
  %11 = or i128 %10, %9
  ret i128 %11
}

; compiler_builtins::int::addsub::__rust_i128_addo
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn writeonly
define hidden void @_ZN17compiler_builtins3int6addsub16__rust_i128_addo17h75ee4e7337c6fca1E({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #4 {
start:
  %1 = trunc i128 %a to i64
  %2 = trunc i128 %b to i64
  %3 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %1, i64 %2) #14
  %4 = extractvalue { i64, i1 } %3, 0
  %5 = extractvalue { i64, i1 } %3, 1
  %_2.i.i.i.i = lshr i128 %a, 64
  %6 = trunc i128 %_2.i.i.i.i to i64
  %_2.i4.i.i.i = lshr i128 %b, 64
  %7 = trunc i128 %_2.i4.i.i.i to i64
  %8 = add i64 %7, %6
  %9 = zext i1 %5 to i64
  %spec.select.i.i.i = add i64 %8, %9
  %10 = zext i64 %4 to i128
  %_2.i.i.i.i.i = zext i64 %spec.select.i.i.i to i128
  %11 = shl nuw i128 %_2.i.i.i.i.i, 64
  %12 = or i128 %11, %10
  %13 = icmp slt i128 %b, 0
  %14 = icmp slt i128 %12, %a
  %_7.i = xor i1 %13, %14
  %15 = zext i1 %_7.i to i8
  %16 = insertvalue { i128, i8 } undef, i128 %12, 0
  %17 = insertvalue { i128, i8 } %16, i8 %15, 1
  store { i128, i8 } %17, { i128, i8 }* %0, align 8
  ret void
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define hidden void @__rust_i128_addo({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #5 {
start:
  %1 = trunc i128 %a to i64
  %2 = trunc i128 %b to i64
  %3 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %1, i64 %2) #14
  %4 = extractvalue { i64, i1 } %3, 0
  %5 = extractvalue { i64, i1 } %3, 1
  %_2.i.i.i.i.i = lshr i128 %a, 64
  %6 = trunc i128 %_2.i.i.i.i.i to i64
  %_2.i4.i.i.i.i = lshr i128 %b, 64
  %7 = trunc i128 %_2.i4.i.i.i.i to i64
  %8 = add i64 %7, %6
  %9 = zext i1 %5 to i64
  %spec.select.i.i.i.i = add i64 %8, %9
  %10 = zext i64 %4 to i128
  %_2.i.i.i.i.i.i = zext i64 %spec.select.i.i.i.i to i128
  %11 = shl nuw i128 %_2.i.i.i.i.i.i, 64
  %12 = or i128 %11, %10
  %13 = icmp slt i128 %b, 0
  %14 = icmp slt i128 %12, %a
  %_7.i.i = xor i1 %13, %14
  %15 = zext i1 %_7.i.i to i8
  %16 = insertvalue { i128, i8 } undef, i128 %12, 0
  %17 = insertvalue { i128, i8 } %16, i8 %15, 1
  store { i128, i8 } %17, { i128, i8 }* %0, align 8, !alias.scope !8
  ret void
}

; compiler_builtins::int::addsub::__rust_u128_addo
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn writeonly
define hidden void @_ZN17compiler_builtins3int6addsub16__rust_u128_addo17hf70a1bb418b353caE({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #4 {
start:
  %1 = trunc i128 %a to i64
  %2 = trunc i128 %b to i64
  %3 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %1, i64 %2) #14
  %4 = extractvalue { i64, i1 } %3, 0
  %5 = extractvalue { i64, i1 } %3, 1
  %_2.i.i.i.i = lshr i128 %a, 64
  %6 = trunc i128 %_2.i.i.i.i to i64
  %_2.i4.i.i.i = lshr i128 %b, 64
  %7 = trunc i128 %_2.i4.i.i.i to i64
  %8 = add i64 %7, %6
  %9 = zext i1 %5 to i64
  %spec.select.i.i.i = add i64 %8, %9
  %10 = zext i64 %4 to i128
  %_2.i.i.i.i.i = zext i64 %spec.select.i.i.i to i128
  %11 = shl nuw i128 %_2.i.i.i.i.i, 64
  %12 = or i128 %11, %10
  %13 = icmp ult i128 %12, %a
  %14 = zext i1 %13 to i8
  %15 = insertvalue { i128, i8 } undef, i128 %12, 0
  %16 = insertvalue { i128, i8 } %15, i8 %14, 1
  store { i128, i8 } %16, { i128, i8 }* %0, align 8
  ret void
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define hidden void @__rust_u128_addo({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #5 {
start:
  %1 = trunc i128 %a to i64
  %2 = trunc i128 %b to i64
  %3 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %1, i64 %2) #14
  %4 = extractvalue { i64, i1 } %3, 0
  %5 = extractvalue { i64, i1 } %3, 1
  %_2.i.i.i.i.i = lshr i128 %a, 64
  %6 = trunc i128 %_2.i.i.i.i.i to i64
  %_2.i4.i.i.i.i = lshr i128 %b, 64
  %7 = trunc i128 %_2.i4.i.i.i.i to i64
  %8 = add i64 %7, %6
  %9 = zext i1 %5 to i64
  %spec.select.i.i.i.i = add i64 %8, %9
  %10 = zext i64 %4 to i128
  %_2.i.i.i.i.i.i = zext i64 %spec.select.i.i.i.i to i128
  %11 = shl nuw i128 %_2.i.i.i.i.i.i, 64
  %12 = or i128 %11, %10
  %13 = icmp ult i128 %12, %a
  %14 = zext i1 %13 to i8
  %15 = insertvalue { i128, i8 } undef, i128 %12, 0
  %16 = insertvalue { i128, i8 } %15, i8 %14, 1
  store { i128, i8 } %16, { i128, i8 }* %0, align 8, !alias.scope !11
  ret void
}

; compiler_builtins::int::addsub::__rust_i128_sub
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int6addsub15__rust_i128_sub17hfcc2aa6867c91785E(i128 %a, i128 %b) unnamed_addr #1 {
start:
  %0 = xor i128 %b, -1
  %1 = trunc i128 %0 to i64
  %2 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %1, i64 1) #14
  %3 = extractvalue { i64, i1 } %2, 0
  %4 = extractvalue { i64, i1 } %2, 1
  %..i.i.i = zext i1 %4 to i64
  %_2.i.i.i.i = lshr i128 %0, 64
  %5 = trunc i128 %_2.i.i.i.i to i64
  %6 = trunc i128 %a to i64
  %7 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %6, i64 %3) #14
  %8 = extractvalue { i64, i1 } %7, 0
  %9 = extractvalue { i64, i1 } %7, 1
  %_2.i.i1.i.i = lshr i128 %a, 64
  %10 = trunc i128 %_2.i.i1.i.i to i64
  %11 = zext i1 %9 to i64
  %12 = add i64 %5, %10
  %13 = add i64 %12, %..i.i.i
  %spec.select.i.i.i = add i64 %13, %11
  %14 = zext i64 %8 to i128
  %_2.i.i.i2.i.i = zext i64 %spec.select.i.i.i to i128
  %15 = shl nuw i128 %_2.i.i.i2.i.i, 64
  %16 = or i128 %15, %14
  ret i128 %16
}

; compiler_builtins::int::addsub::__rust_i128_subo
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn writeonly
define hidden void @_ZN17compiler_builtins3int6addsub16__rust_i128_subo17h3cedbf7b2f801579E({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #4 {
start:
  %1 = xor i128 %b, -1
  %2 = trunc i128 %1 to i64
  %3 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %2, i64 1) #14
  %4 = extractvalue { i64, i1 } %3, 0
  %5 = extractvalue { i64, i1 } %3, 1
  %..i.i.i.i = zext i1 %5 to i64
  %_2.i.i.i.i.i = lshr i128 %1, 64
  %6 = trunc i128 %_2.i.i.i.i.i to i64
  %7 = trunc i128 %a to i64
  %8 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %7, i64 %4) #14
  %9 = extractvalue { i64, i1 } %8, 0
  %10 = extractvalue { i64, i1 } %8, 1
  %_2.i.i1.i.i.i = lshr i128 %a, 64
  %11 = trunc i128 %_2.i.i1.i.i.i to i64
  %12 = zext i1 %10 to i64
  %13 = add i64 %6, %11
  %14 = add i64 %13, %..i.i.i.i
  %spec.select.i.i.i.i = add i64 %14, %12
  %15 = zext i64 %9 to i128
  %_2.i.i.i2.i.i.i = zext i64 %spec.select.i.i.i.i to i128
  %16 = shl nuw i128 %_2.i.i.i2.i.i.i, 64
  %17 = or i128 %16, %15
  %18 = icmp slt i128 %b, 0
  %19 = icmp sgt i128 %17, %a
  %_7.i = xor i1 %18, %19
  %20 = zext i1 %_7.i to i8
  %21 = insertvalue { i128, i8 } undef, i128 %17, 0
  %22 = insertvalue { i128, i8 } %21, i8 %20, 1
  store { i128, i8 } %22, { i128, i8 }* %0, align 8
  ret void
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define hidden void @__rust_i128_subo({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #5 {
start:
  %1 = xor i128 %b, -1
  %2 = trunc i128 %1 to i64
  %3 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %2, i64 1) #14
  %4 = extractvalue { i64, i1 } %3, 0
  %5 = extractvalue { i64, i1 } %3, 1
  %..i.i.i.i.i = zext i1 %5 to i64
  %_2.i.i.i.i.i.i = lshr i128 %1, 64
  %6 = trunc i128 %_2.i.i.i.i.i.i to i64
  %7 = trunc i128 %a to i64
  %8 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %7, i64 %4) #14
  %9 = extractvalue { i64, i1 } %8, 0
  %10 = extractvalue { i64, i1 } %8, 1
  %_2.i.i1.i.i.i.i = lshr i128 %a, 64
  %11 = trunc i128 %_2.i.i1.i.i.i.i to i64
  %12 = zext i1 %10 to i64
  %13 = add i64 %6, %11
  %14 = add i64 %13, %..i.i.i.i.i
  %spec.select.i.i.i.i.i = add i64 %14, %12
  %15 = zext i64 %9 to i128
  %_2.i.i.i2.i.i.i.i = zext i64 %spec.select.i.i.i.i.i to i128
  %16 = shl nuw i128 %_2.i.i.i2.i.i.i.i, 64
  %17 = or i128 %16, %15
  %18 = icmp slt i128 %b, 0
  %19 = icmp sgt i128 %17, %a
  %_7.i.i = xor i1 %18, %19
  %20 = zext i1 %_7.i.i to i8
  %21 = insertvalue { i128, i8 } undef, i128 %17, 0
  %22 = insertvalue { i128, i8 } %21, i8 %20, 1
  store { i128, i8 } %22, { i128, i8 }* %0, align 8, !alias.scope !14
  ret void
}

; compiler_builtins::int::addsub::__rust_u128_subo
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn writeonly
define hidden void @_ZN17compiler_builtins3int6addsub16__rust_u128_subo17h358927d3f5806756E({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #4 {
start:
  %1 = xor i128 %b, -1
  %2 = trunc i128 %1 to i64
  %3 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %2, i64 1) #14
  %4 = extractvalue { i64, i1 } %3, 0
  %5 = extractvalue { i64, i1 } %3, 1
  %..i.i.i.i = zext i1 %5 to i64
  %_2.i.i.i.i.i = lshr i128 %1, 64
  %6 = trunc i128 %_2.i.i.i.i.i to i64
  %7 = trunc i128 %a to i64
  %8 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %7, i64 %4) #14
  %9 = extractvalue { i64, i1 } %8, 0
  %10 = extractvalue { i64, i1 } %8, 1
  %_2.i.i1.i.i.i = lshr i128 %a, 64
  %11 = trunc i128 %_2.i.i1.i.i.i to i64
  %12 = zext i1 %10 to i64
  %13 = add i64 %6, %11
  %14 = add i64 %13, %..i.i.i.i
  %spec.select.i.i.i.i = add i64 %14, %12
  %15 = zext i64 %9 to i128
  %_2.i.i.i2.i.i.i = zext i64 %spec.select.i.i.i.i to i128
  %16 = shl nuw i128 %_2.i.i.i2.i.i.i, 64
  %17 = or i128 %16, %15
  %18 = icmp ugt i128 %17, %a
  %19 = zext i1 %18 to i8
  %20 = insertvalue { i128, i8 } undef, i128 %17, 0
  %21 = insertvalue { i128, i8 } %20, i8 %19, 1
  store { i128, i8 } %21, { i128, i8 }* %0, align 8
  ret void
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define hidden void @__rust_u128_subo({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #5 {
start:
  %1 = xor i128 %b, -1
  %2 = trunc i128 %1 to i64
  %3 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %2, i64 1) #14
  %4 = extractvalue { i64, i1 } %3, 0
  %5 = extractvalue { i64, i1 } %3, 1
  %..i.i.i.i.i = zext i1 %5 to i64
  %_2.i.i.i.i.i.i = lshr i128 %1, 64
  %6 = trunc i128 %_2.i.i.i.i.i.i to i64
  %7 = trunc i128 %a to i64
  %8 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %7, i64 %4) #14
  %9 = extractvalue { i64, i1 } %8, 0
  %10 = extractvalue { i64, i1 } %8, 1
  %_2.i.i1.i.i.i.i = lshr i128 %a, 64
  %11 = trunc i128 %_2.i.i1.i.i.i.i to i64
  %12 = zext i1 %10 to i64
  %13 = add i64 %6, %11
  %14 = add i64 %13, %..i.i.i.i.i
  %spec.select.i.i.i.i.i = add i64 %14, %12
  %15 = zext i64 %9 to i128
  %_2.i.i.i2.i.i.i.i = zext i64 %spec.select.i.i.i.i.i to i128
  %16 = shl nuw i128 %_2.i.i.i2.i.i.i.i, 64
  %17 = or i128 %16, %15
  %18 = icmp ugt i128 %17, %a
  %19 = zext i1 %18 to i8
  %20 = insertvalue { i128, i8 } undef, i128 %17, 0
  %21 = insertvalue { i128, i8 } %20, i8 %19, 1
  store { i128, i8 } %21, { i128, i8 }* %0, align 8, !alias.scope !17
  ret void
}

; compiler_builtins::int::leading_zeros::__clzsi2
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @_ZN17compiler_builtins3int13leading_zeros8__clzsi217h44420f7804efd156E(i64 %x) unnamed_addr #2 {
start:
  %0 = icmp ult i64 %x, 4294967296
  %1 = lshr i64 %x, 32
  %spec.select.i = select i1 %0, i64 64, i64 32
  %spec.select32.i = select i1 %0, i64 %x, i64 %1
  %2 = icmp ult i64 %spec.select32.i, 65536
  %3 = lshr i64 %spec.select32.i, 16
  %4 = add nsw i64 %spec.select.i, -16
  %z.1.i = select i1 %2, i64 %spec.select.i, i64 %4
  %x1.1.i = select i1 %2, i64 %spec.select32.i, i64 %3
  %5 = icmp ult i64 %x1.1.i, 256
  %6 = lshr i64 %x1.1.i, 8
  %7 = add nsw i64 %z.1.i, -8
  %z.2.i = select i1 %5, i64 %z.1.i, i64 %7
  %x1.2.i = select i1 %5, i64 %x1.1.i, i64 %6
  %8 = icmp ult i64 %x1.2.i, 16
  %9 = lshr i64 %x1.2.i, 4
  %10 = add nsw i64 %z.2.i, -4
  %z.3.i = select i1 %8, i64 %z.2.i, i64 %10
  %x1.3.i = select i1 %8, i64 %x1.2.i, i64 %9
  %11 = icmp ult i64 %x1.3.i, 4
  %12 = lshr i64 %x1.3.i, 2
  %13 = add nsw i64 %z.3.i, -2
  %z.4.i = select i1 %11, i64 %z.3.i, i64 %13
  %x1.4.i = select i1 %11, i64 %x1.3.i, i64 %12
  %14 = icmp ult i64 %x1.4.i, 2
  %15 = sub nsw i64 0, %x1.4.i
  %.0.p.i = select i1 %14, i64 %15, i64 -2
  %.0.i = add nsw i64 %.0.p.i, %z.4.i
  ret i64 %.0.i
}

; compiler_builtins::int::mul::__muldi3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @_ZN17compiler_builtins3int3mul8__muldi317h3d08cf1a53d1f1f7E(i64 %a, i64 %b) unnamed_addr #2 {
start:
  %0 = trunc i64 %a to i32
  %1 = trunc i64 %b to i32
  %2 = and i32 %0, 65535
  %3 = and i32 %1, 65535
  %4 = mul nuw i32 %3, %2
  %_2.i9.i = lshr i64 %b, 16
  %5 = trunc i64 %_2.i9.i to i32
  %6 = and i32 %5, 65535
  %7 = mul nuw i32 %6, %2
  %_2.i110.i = lshr i64 %a, 16
  %8 = trunc i64 %_2.i110.i to i32
  %9 = and i32 %8, 65535
  %10 = mul nuw i32 %3, %9
  %11 = mul nuw i32 %6, %9
  %12 = zext i32 %4 to i64
  %_2.i.i.i = zext i32 %11 to i64
  %13 = shl nuw i64 %_2.i.i.i, 32
  %14 = zext i32 %7 to i64
  %15 = zext i32 %10 to i64
  %_2.i5.i = lshr i64 %b, 32
  %_2.i6.i = mul i64 %_2.i5.i, %a
  %_2.i7.i = lshr i64 %a, 32
  %_2.i8.i = mul i64 %_2.i7.i, %b
  %reass.add.i = add nuw nsw i64 %14, %15
  %reass.mul.i = shl nuw nsw i64 %reass.add.i, 16
  %reass.add11.i = add i64 %_2.i6.i, %_2.i8.i
  %reass.mul12.i = shl i64 %reass.add11.i, 32
  %16 = or i64 %reass.mul12.i, %12
  %17 = add i64 %16, %13
  %18 = add i64 %17, %reass.mul.i
  ret i64 %18
}

; compiler_builtins::int::mul::__multi3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int3mul8__multi317h610a2e13318c07e9E(i128 %a, i128 %b) unnamed_addr #2 {
start:
  %0 = trunc i128 %a to i64
  %1 = trunc i128 %b to i64
  %2 = and i64 %0, 4294967295
  %3 = and i64 %1, 4294967295
  %4 = mul nuw i64 %3, %2
  %5 = lshr i128 %b, 32
  %6 = trunc i128 %5 to i64
  %7 = and i64 %6, 4294967295
  %8 = mul nuw i64 %7, %2
  %9 = lshr i128 %a, 32
  %10 = trunc i128 %9 to i64
  %11 = and i64 %10, 4294967295
  %12 = mul nuw i64 %3, %11
  %13 = mul nuw i64 %7, %11
  %14 = zext i64 %4 to i128
  %_21.i.i.i = zext i64 %13 to i128
  %15 = shl nuw i128 %_21.i.i.i, 64
  %16 = zext i64 %8 to i128
  %17 = zext i64 %12 to i128
  %18 = lshr i128 %b, 64
  %19 = trunc i128 %18 to i64
  %20 = mul i64 %19, %0
  %_21.i.i = zext i64 %20 to i128
  %21 = lshr i128 %a, 64
  %22 = trunc i128 %21 to i64
  %23 = mul i64 %1, %22
  %_21.i2.i = zext i64 %23 to i128
  %reass.add.i = add nuw nsw i128 %16, %17
  %reass.mul.i = shl nuw nsw i128 %reass.add.i, 32
  %reass.add3.i = add nuw nsw i128 %_21.i.i, %_21.i2.i
  %reass.mul4.i = shl i128 %reass.add3.i, 64
  %24 = or i128 %15, %14
  %25 = add i128 %24, %reass.mul4.i
  %26 = add i128 %25, %reass.mul.i
  ret i128 %26
}

; compiler_builtins::int::mul::__mulosi4
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn writeonly
define hidden i32 @_ZN17compiler_builtins3int3mul9__mulosi417headb65407c5eaf4dE(i32 %a, i32 %b, i32* noalias nocapture align 4 dereferenceable(4) %oflow) unnamed_addr #4 {
start:
  %0 = icmp eq i32 %a, 0
  %_7.i = icmp eq i32 %b, 0
  %_5.0.i = select i1 %0, i1 true, i1 %_7.i
  br i1 %_5.0.i, label %_ZN17compiler_builtins3int3mul19i32_overflowing_mul17h4b283f3679793c99E.exit, label %bb5.i

bb5.i:                                            ; preds = %start
  %lhs_neg.i = icmp slt i32 %a, 0
  %rhs_neg.i = icmp slt i32 %b, 0
  %1 = sub i32 0, %a
  %spec.select.i = select i1 %lhs_neg.i, i32 %1, i32 %a
  %2 = sub i32 0, %b
  %spec.select17.i = select i1 %rhs_neg.i, i32 %2, i32 %b
  %mul_neg.i = xor i1 %lhs_neg.i, %rhs_neg.i
  %_2.i.i.i = lshr i32 %spec.select.i, 16
  %3 = icmp ult i32 %spec.select.i, 65536
  %_2.i4.i.i = lshr i32 %spec.select17.i, 16
  %4 = icmp ult i32 %spec.select17.i, 65536
  br i1 %3, label %bb6.i.i, label %bb5.i.i

bb5.i.i:                                          ; preds = %bb5.i
  br i1 %4, label %bb25.i.i, label %bb8.i.i

bb6.i.i:                                          ; preds = %bb5.i
  br i1 %4, label %bb7.i.i, label %bb10.i.i

bb10.i.i:                                         ; preds = %bb6.i.i
  %5 = and i32 %spec.select17.i, 65535
  %6 = mul nuw i32 %5, %spec.select.i
  %7 = mul nuw nsw i32 %_2.i4.i.i, %spec.select.i
  %_2.i6.i.i = shl i32 %7, 16
  %8 = tail call { i32, i1 } @llvm.uadd.with.overflow.i32(i32 %6, i32 %_2.i6.i.i) #14
  %9 = extractvalue { i32, i1 } %8, 0
  %10 = extractvalue { i32, i1 } %8, 1
  %11 = icmp ugt i32 %7, 65535
  %spec.select.i.i = select i1 %10, i1 true, i1 %11
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i

bb7.i.i:                                          ; preds = %bb6.i.i
  %12 = mul nuw i32 %spec.select17.i, %spec.select.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i

bb8.i.i:                                          ; preds = %bb5.i.i
  %13 = mul i32 %spec.select17.i, %spec.select.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i

bb25.i.i:                                         ; preds = %bb5.i.i
  %14 = and i32 %spec.select.i, 65535
  %15 = mul nuw i32 %14, %spec.select17.i
  %16 = mul nuw nsw i32 %_2.i.i.i, %spec.select17.i
  %_2.i9.i.i = shl i32 %16, 16
  %17 = tail call { i32, i1 } @llvm.uadd.with.overflow.i32(i32 %15, i32 %_2.i9.i.i) #14
  %18 = extractvalue { i32, i1 } %17, 0
  %19 = extractvalue { i32, i1 } %17, 1
  %20 = icmp ugt i32 %16, 65535
  %spec.select11.i.i = select i1 %19, i1 true, i1 %20
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i

_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i: ; preds = %bb25.i.i, %bb8.i.i, %bb7.i.i, %bb10.i.i
  %.sroa.5.0.shrunk.shrunk.i.i = phi i1 [ false, %bb7.i.i ], [ true, %bb8.i.i ], [ %spec.select.i.i, %bb10.i.i ], [ %spec.select11.i.i, %bb25.i.i ]
  %.sroa.0.0.i.i = phi i32 [ %12, %bb7.i.i ], [ %13, %bb8.i.i ], [ %9, %bb10.i.i ], [ %18, %bb25.i.i ]
  %21 = sub i32 0, %.sroa.0.0.i.i
  %spec.select18.i = select i1 %mul_neg.i, i32 %21, i32 %.sroa.0.0.i.i
  %_35.i = icmp slt i32 %spec.select18.i, 0
  %_34.i = xor i1 %mul_neg.i, %_35.i
  %narrow.i = select i1 %_34.i, i1 true, i1 %.sroa.5.0.shrunk.shrunk.i.i
  %phi.cast = zext i1 %narrow.i to i32
  br label %_ZN17compiler_builtins3int3mul19i32_overflowing_mul17h4b283f3679793c99E.exit

_ZN17compiler_builtins3int3mul19i32_overflowing_mul17h4b283f3679793c99E.exit: ; preds = %start, %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i
  %.sroa.4.0.i = phi i32 [ %phi.cast, %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i ], [ 0, %start ]
  %.sroa.0.0.i = phi i32 [ %spec.select18.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i ], [ 0, %start ]
  store i32 %.sroa.4.0.i, i32* %oflow, align 4
  ret i32 %.sroa.0.0.i
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define hidden i32 @__mulosi4(i32 %a, i32 %b, i32* noalias nocapture align 4 dereferenceable(4) %oflow) unnamed_addr #5 {
start:
  %0 = icmp eq i32 %a, 0
  %_7.i.i = icmp eq i32 %b, 0
  %_5.0.i.i = select i1 %0, i1 true, i1 %_7.i.i
  br i1 %_5.0.i.i, label %_ZN17compiler_builtins3int3mul9__mulosi417headb65407c5eaf4dE.exit, label %bb5.i.i

bb5.i.i:                                          ; preds = %start
  %lhs_neg.i.i = icmp slt i32 %a, 0
  %rhs_neg.i.i = icmp slt i32 %b, 0
  %1 = sub i32 0, %a
  %spec.select.i.i = select i1 %lhs_neg.i.i, i32 %1, i32 %a
  %2 = sub i32 0, %b
  %spec.select17.i.i = select i1 %rhs_neg.i.i, i32 %2, i32 %b
  %mul_neg.i.i = xor i1 %lhs_neg.i.i, %rhs_neg.i.i
  %_2.i.i.i.i = lshr i32 %spec.select.i.i, 16
  %3 = icmp ult i32 %spec.select.i.i, 65536
  %_2.i4.i.i.i = lshr i32 %spec.select17.i.i, 16
  %4 = icmp ult i32 %spec.select17.i.i, 65536
  br i1 %3, label %bb6.i.i.i, label %bb5.i.i.i

bb5.i.i.i:                                        ; preds = %bb5.i.i
  br i1 %4, label %bb25.i.i.i, label %bb8.i.i.i

bb6.i.i.i:                                        ; preds = %bb5.i.i
  br i1 %4, label %bb7.i.i.i, label %bb10.i.i.i

bb10.i.i.i:                                       ; preds = %bb6.i.i.i
  %5 = and i32 %spec.select17.i.i, 65535
  %6 = mul nuw i32 %5, %spec.select.i.i
  %7 = mul nuw nsw i32 %_2.i4.i.i.i, %spec.select.i.i
  %_2.i6.i.i.i = shl i32 %7, 16
  %8 = tail call { i32, i1 } @llvm.uadd.with.overflow.i32(i32 %6, i32 %_2.i6.i.i.i) #14
  %9 = extractvalue { i32, i1 } %8, 0
  %10 = extractvalue { i32, i1 } %8, 1
  %11 = icmp ugt i32 %7, 65535
  %spec.select.i.i.i = select i1 %10, i1 true, i1 %11
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i.i

bb7.i.i.i:                                        ; preds = %bb6.i.i.i
  %12 = mul nuw i32 %spec.select17.i.i, %spec.select.i.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i.i

bb8.i.i.i:                                        ; preds = %bb5.i.i.i
  %13 = mul i32 %spec.select17.i.i, %spec.select.i.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i.i

bb25.i.i.i:                                       ; preds = %bb5.i.i.i
  %14 = and i32 %spec.select.i.i, 65535
  %15 = mul nuw i32 %14, %spec.select17.i.i
  %16 = mul nuw nsw i32 %_2.i.i.i.i, %spec.select17.i.i
  %_2.i9.i.i.i = shl i32 %16, 16
  %17 = tail call { i32, i1 } @llvm.uadd.with.overflow.i32(i32 %15, i32 %_2.i9.i.i.i) #14
  %18 = extractvalue { i32, i1 } %17, 0
  %19 = extractvalue { i32, i1 } %17, 1
  %20 = icmp ugt i32 %16, 65535
  %spec.select11.i.i.i = select i1 %19, i1 true, i1 %20
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i.i

_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i.i: ; preds = %bb25.i.i.i, %bb8.i.i.i, %bb7.i.i.i, %bb10.i.i.i
  %.sroa.5.0.shrunk.shrunk.i.i.i = phi i1 [ false, %bb7.i.i.i ], [ true, %bb8.i.i.i ], [ %spec.select.i.i.i, %bb10.i.i.i ], [ %spec.select11.i.i.i, %bb25.i.i.i ]
  %.sroa.0.0.i.i.i = phi i32 [ %12, %bb7.i.i.i ], [ %13, %bb8.i.i.i ], [ %9, %bb10.i.i.i ], [ %18, %bb25.i.i.i ]
  %21 = sub i32 0, %.sroa.0.0.i.i.i
  %spec.select18.i.i = select i1 %mul_neg.i.i, i32 %21, i32 %.sroa.0.0.i.i.i
  %_35.i.i = icmp slt i32 %spec.select18.i.i, 0
  %_34.i.i = xor i1 %mul_neg.i.i, %_35.i.i
  %narrow.i.i = select i1 %_34.i.i, i1 true, i1 %.sroa.5.0.shrunk.shrunk.i.i.i
  %phi.cast.i = zext i1 %narrow.i.i to i32
  br label %_ZN17compiler_builtins3int3mul9__mulosi417headb65407c5eaf4dE.exit

_ZN17compiler_builtins3int3mul9__mulosi417headb65407c5eaf4dE.exit: ; preds = %start, %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i.i
  %.sroa.4.0.i.i = phi i32 [ %phi.cast.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i.i ], [ 0, %start ]
  %.sroa.0.0.i.i = phi i32 [ %spec.select18.i.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17h81ce11cd3ae904dcE.exit.i.i ], [ 0, %start ]
  store i32 %.sroa.4.0.i.i, i32* %oflow, align 4, !alias.scope !20
  ret i32 %.sroa.0.0.i.i
}

; compiler_builtins::int::mul::__mulodi4
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn writeonly
define hidden i64 @_ZN17compiler_builtins3int3mul9__mulodi417h270d5205ef1acb48E(i64 %a, i64 %b, i32* noalias nocapture align 4 dereferenceable(4) %oflow) unnamed_addr #4 {
start:
  %0 = icmp eq i64 %a, 0
  %_7.i = icmp eq i64 %b, 0
  %_5.0.i = select i1 %0, i1 true, i1 %_7.i
  br i1 %_5.0.i, label %_ZN17compiler_builtins3int3mul19i64_overflowing_mul17h0e2e810fcad7cfa4E.exit, label %bb5.i

bb5.i:                                            ; preds = %start
  %lhs_neg.i = icmp slt i64 %a, 0
  %rhs_neg.i = icmp slt i64 %b, 0
  %1 = sub i64 0, %a
  %spec.select.i = select i1 %lhs_neg.i, i64 %1, i64 %a
  %2 = sub i64 0, %b
  %spec.select17.i = select i1 %rhs_neg.i, i64 %2, i64 %b
  %mul_neg.i = xor i1 %lhs_neg.i, %rhs_neg.i
  %_2.i.i.i = lshr i64 %spec.select.i, 32
  %3 = icmp ult i64 %spec.select.i, 4294967296
  %_2.i4.i.i = lshr i64 %spec.select17.i, 32
  %4 = icmp ult i64 %spec.select17.i, 4294967296
  br i1 %3, label %bb6.i.i, label %bb5.i.i

bb5.i.i:                                          ; preds = %bb5.i
  br i1 %4, label %bb25.i.i, label %bb8.i.i

bb6.i.i:                                          ; preds = %bb5.i
  br i1 %4, label %bb7.i.i, label %bb10.i.i

bb10.i.i:                                         ; preds = %bb6.i.i
  %5 = and i64 %spec.select17.i, 4294967295
  %6 = mul nuw i64 %5, %spec.select.i
  %7 = mul nuw nsw i64 %_2.i4.i.i, %spec.select.i
  %_2.i6.i.i = shl i64 %7, 32
  %8 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %6, i64 %_2.i6.i.i) #14
  %9 = extractvalue { i64, i1 } %8, 0
  %10 = extractvalue { i64, i1 } %8, 1
  %11 = icmp ugt i64 %7, 4294967295
  %spec.select.i.i = select i1 %10, i1 true, i1 %11
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i

bb7.i.i:                                          ; preds = %bb6.i.i
  %12 = mul nuw i64 %spec.select17.i, %spec.select.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i

bb8.i.i:                                          ; preds = %bb5.i.i
  %13 = mul i64 %spec.select17.i, %spec.select.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i

bb25.i.i:                                         ; preds = %bb5.i.i
  %14 = and i64 %spec.select.i, 4294967295
  %15 = mul nuw i64 %14, %spec.select17.i
  %16 = mul nuw nsw i64 %_2.i.i.i, %spec.select17.i
  %_2.i9.i.i = shl i64 %16, 32
  %17 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %15, i64 %_2.i9.i.i) #14
  %18 = extractvalue { i64, i1 } %17, 0
  %19 = extractvalue { i64, i1 } %17, 1
  %20 = icmp ugt i64 %16, 4294967295
  %spec.select11.i.i = select i1 %19, i1 true, i1 %20
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i

_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i: ; preds = %bb25.i.i, %bb8.i.i, %bb7.i.i, %bb10.i.i
  %.sroa.5.0.shrunk.shrunk.i.i = phi i1 [ false, %bb7.i.i ], [ true, %bb8.i.i ], [ %spec.select.i.i, %bb10.i.i ], [ %spec.select11.i.i, %bb25.i.i ]
  %.sroa.0.0.i.i = phi i64 [ %12, %bb7.i.i ], [ %13, %bb8.i.i ], [ %9, %bb10.i.i ], [ %18, %bb25.i.i ]
  %21 = sub i64 0, %.sroa.0.0.i.i
  %spec.select18.i = select i1 %mul_neg.i, i64 %21, i64 %.sroa.0.0.i.i
  %_35.i = icmp slt i64 %spec.select18.i, 0
  %_34.i = xor i1 %mul_neg.i, %_35.i
  %narrow.i = select i1 %_34.i, i1 true, i1 %.sroa.5.0.shrunk.shrunk.i.i
  %phi.cast = zext i1 %narrow.i to i32
  br label %_ZN17compiler_builtins3int3mul19i64_overflowing_mul17h0e2e810fcad7cfa4E.exit

_ZN17compiler_builtins3int3mul19i64_overflowing_mul17h0e2e810fcad7cfa4E.exit: ; preds = %start, %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i
  %.sroa.4.0.i = phi i32 [ %phi.cast, %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i ], [ 0, %start ]
  %.sroa.0.0.i = phi i64 [ %spec.select18.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i ], [ 0, %start ]
  store i32 %.sroa.4.0.i, i32* %oflow, align 4
  ret i64 %.sroa.0.0.i
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define hidden i64 @__mulodi4(i64 %a, i64 %b, i32* noalias nocapture align 4 dereferenceable(4) %oflow) unnamed_addr #5 {
start:
  %0 = icmp eq i64 %a, 0
  %_7.i.i = icmp eq i64 %b, 0
  %_5.0.i.i = select i1 %0, i1 true, i1 %_7.i.i
  br i1 %_5.0.i.i, label %_ZN17compiler_builtins3int3mul9__mulodi417h270d5205ef1acb48E.exit, label %bb5.i.i

bb5.i.i:                                          ; preds = %start
  %lhs_neg.i.i = icmp slt i64 %a, 0
  %rhs_neg.i.i = icmp slt i64 %b, 0
  %1 = sub i64 0, %a
  %spec.select.i.i = select i1 %lhs_neg.i.i, i64 %1, i64 %a
  %2 = sub i64 0, %b
  %spec.select17.i.i = select i1 %rhs_neg.i.i, i64 %2, i64 %b
  %mul_neg.i.i = xor i1 %lhs_neg.i.i, %rhs_neg.i.i
  %_2.i.i.i.i = lshr i64 %spec.select.i.i, 32
  %3 = icmp ult i64 %spec.select.i.i, 4294967296
  %_2.i4.i.i.i = lshr i64 %spec.select17.i.i, 32
  %4 = icmp ult i64 %spec.select17.i.i, 4294967296
  br i1 %3, label %bb6.i.i.i, label %bb5.i.i.i

bb5.i.i.i:                                        ; preds = %bb5.i.i
  br i1 %4, label %bb25.i.i.i, label %bb8.i.i.i

bb6.i.i.i:                                        ; preds = %bb5.i.i
  br i1 %4, label %bb7.i.i.i, label %bb10.i.i.i

bb10.i.i.i:                                       ; preds = %bb6.i.i.i
  %5 = and i64 %spec.select17.i.i, 4294967295
  %6 = mul nuw i64 %5, %spec.select.i.i
  %7 = mul nuw nsw i64 %_2.i4.i.i.i, %spec.select.i.i
  %_2.i6.i.i.i = shl i64 %7, 32
  %8 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %6, i64 %_2.i6.i.i.i) #14
  %9 = extractvalue { i64, i1 } %8, 0
  %10 = extractvalue { i64, i1 } %8, 1
  %11 = icmp ugt i64 %7, 4294967295
  %spec.select.i.i.i = select i1 %10, i1 true, i1 %11
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i.i

bb7.i.i.i:                                        ; preds = %bb6.i.i.i
  %12 = mul nuw i64 %spec.select17.i.i, %spec.select.i.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i.i

bb8.i.i.i:                                        ; preds = %bb5.i.i.i
  %13 = mul i64 %spec.select17.i.i, %spec.select.i.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i.i

bb25.i.i.i:                                       ; preds = %bb5.i.i.i
  %14 = and i64 %spec.select.i.i, 4294967295
  %15 = mul nuw i64 %14, %spec.select17.i.i
  %16 = mul nuw nsw i64 %_2.i.i.i.i, %spec.select17.i.i
  %_2.i9.i.i.i = shl i64 %16, 32
  %17 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %15, i64 %_2.i9.i.i.i) #14
  %18 = extractvalue { i64, i1 } %17, 0
  %19 = extractvalue { i64, i1 } %17, 1
  %20 = icmp ugt i64 %16, 4294967295
  %spec.select11.i.i.i = select i1 %19, i1 true, i1 %20
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i.i

_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i.i: ; preds = %bb25.i.i.i, %bb8.i.i.i, %bb7.i.i.i, %bb10.i.i.i
  %.sroa.5.0.shrunk.shrunk.i.i.i = phi i1 [ false, %bb7.i.i.i ], [ true, %bb8.i.i.i ], [ %spec.select.i.i.i, %bb10.i.i.i ], [ %spec.select11.i.i.i, %bb25.i.i.i ]
  %.sroa.0.0.i.i.i = phi i64 [ %12, %bb7.i.i.i ], [ %13, %bb8.i.i.i ], [ %9, %bb10.i.i.i ], [ %18, %bb25.i.i.i ]
  %21 = sub i64 0, %.sroa.0.0.i.i.i
  %spec.select18.i.i = select i1 %mul_neg.i.i, i64 %21, i64 %.sroa.0.0.i.i.i
  %_35.i.i = icmp slt i64 %spec.select18.i.i, 0
  %_34.i.i = xor i1 %mul_neg.i.i, %_35.i.i
  %narrow.i.i = select i1 %_34.i.i, i1 true, i1 %.sroa.5.0.shrunk.shrunk.i.i.i
  %phi.cast.i = zext i1 %narrow.i.i to i32
  br label %_ZN17compiler_builtins3int3mul9__mulodi417h270d5205ef1acb48E.exit

_ZN17compiler_builtins3int3mul9__mulodi417h270d5205ef1acb48E.exit: ; preds = %start, %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i.i
  %.sroa.4.0.i.i = phi i32 [ %phi.cast.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i.i ], [ 0, %start ]
  %.sroa.0.0.i.i = phi i64 [ %spec.select18.i.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17he71b0c0b47eca029E.exit.i.i ], [ 0, %start ]
  store i32 %.sroa.4.0.i.i, i32* %oflow, align 4, !alias.scope !23
  ret i64 %.sroa.0.0.i.i
}

; compiler_builtins::int::mul::__muloti4
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn writeonly
define hidden i128 @_ZN17compiler_builtins3int3mul9__muloti417h9010b4dc2a8b3d17E(i128 %a, i128 %b, i32* noalias nocapture align 4 dereferenceable(4) %oflow) unnamed_addr #4 {
start:
  %0 = icmp eq i128 %a, 0
  %_7.i = icmp eq i128 %b, 0
  %_5.0.i = select i1 %0, i1 true, i1 %_7.i
  br i1 %_5.0.i, label %_ZN17compiler_builtins3int3mul20i128_overflowing_mul17hc5174033bee7bb3dE.exit, label %bb5.i

bb5.i:                                            ; preds = %start
  %lhs_neg.i = icmp slt i128 %a, 0
  %rhs_neg.i = icmp slt i128 %b, 0
  %1 = sub i128 0, %a
  %spec.select.i = select i1 %lhs_neg.i, i128 %1, i128 %a
  %2 = sub i128 0, %b
  %spec.select17.i = select i1 %rhs_neg.i, i128 %2, i128 %b
  %mul_neg.i = xor i1 %lhs_neg.i, %rhs_neg.i
  %_2.i.i.i = lshr i128 %spec.select.i, 64
  %3 = icmp ult i128 %spec.select.i, 18446744073709551616
  %_2.i4.i.i = lshr i128 %spec.select17.i, 64
  %4 = icmp ult i128 %spec.select17.i, 18446744073709551616
  br i1 %3, label %bb6.i.i, label %bb5.i.i

bb5.i.i:                                          ; preds = %bb5.i
  br i1 %4, label %bb25.i.i, label %bb8.i.i

bb6.i.i:                                          ; preds = %bb5.i
  br i1 %4, label %bb7.i.i, label %bb10.i.i

bb10.i.i:                                         ; preds = %bb6.i.i
  %5 = and i128 %spec.select17.i, 18446744073709551615
  %6 = mul nuw i128 %5, %spec.select.i
  %7 = mul nuw nsw i128 %_2.i4.i.i, %spec.select.i
  %_2.i6.i.i = shl i128 %7, 64
  %8 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %6, i128 %_2.i6.i.i) #14
  %9 = extractvalue { i128, i1 } %8, 0
  %10 = extractvalue { i128, i1 } %8, 1
  %11 = icmp ugt i128 %7, 18446744073709551615
  %spec.select.i.i = select i1 %10, i1 true, i1 %11
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i

bb7.i.i:                                          ; preds = %bb6.i.i
  %12 = mul nuw i128 %spec.select17.i, %spec.select.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i

bb8.i.i:                                          ; preds = %bb5.i.i
  %13 = mul i128 %spec.select17.i, %spec.select.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i

bb25.i.i:                                         ; preds = %bb5.i.i
  %14 = and i128 %spec.select.i, 18446744073709551615
  %15 = mul nuw i128 %14, %spec.select17.i
  %16 = mul nuw nsw i128 %_2.i.i.i, %spec.select17.i
  %_2.i9.i.i = shl i128 %16, 64
  %17 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %15, i128 %_2.i9.i.i) #14
  %18 = extractvalue { i128, i1 } %17, 0
  %19 = extractvalue { i128, i1 } %17, 1
  %20 = icmp ugt i128 %16, 18446744073709551615
  %spec.select11.i.i = select i1 %19, i1 true, i1 %20
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i

_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i: ; preds = %bb25.i.i, %bb8.i.i, %bb7.i.i, %bb10.i.i
  %.sroa.5.0.shrunk.shrunk.i.i = phi i1 [ false, %bb7.i.i ], [ true, %bb8.i.i ], [ %spec.select.i.i, %bb10.i.i ], [ %spec.select11.i.i, %bb25.i.i ]
  %.sroa.0.0.i.i = phi i128 [ %12, %bb7.i.i ], [ %13, %bb8.i.i ], [ %9, %bb10.i.i ], [ %18, %bb25.i.i ]
  %21 = sub i128 0, %.sroa.0.0.i.i
  %spec.select18.i = select i1 %mul_neg.i, i128 %21, i128 %.sroa.0.0.i.i
  %_35.i = icmp slt i128 %spec.select18.i, 0
  %_34.i = xor i1 %mul_neg.i, %_35.i
  %narrow.i = select i1 %_34.i, i1 true, i1 %.sroa.5.0.shrunk.shrunk.i.i
  %phi.cast = zext i1 %narrow.i to i32
  br label %_ZN17compiler_builtins3int3mul20i128_overflowing_mul17hc5174033bee7bb3dE.exit

_ZN17compiler_builtins3int3mul20i128_overflowing_mul17hc5174033bee7bb3dE.exit: ; preds = %start, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i
  %.sroa.4.0.i = phi i32 [ %phi.cast, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i ], [ 0, %start ]
  %.sroa.0.0.i = phi i128 [ %spec.select18.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i ], [ 0, %start ]
  store i32 %.sroa.4.0.i, i32* %oflow, align 4
  ret i128 %.sroa.0.0.i
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define hidden i128 @__muloti4(i128 %a, i128 %b, i32* noalias nocapture align 4 dereferenceable(4) %oflow) unnamed_addr #5 {
start:
  %0 = icmp eq i128 %a, 0
  %_7.i.i = icmp eq i128 %b, 0
  %_5.0.i.i = select i1 %0, i1 true, i1 %_7.i.i
  br i1 %_5.0.i.i, label %_ZN17compiler_builtins3int3mul9__muloti417h9010b4dc2a8b3d17E.exit, label %bb5.i.i

bb5.i.i:                                          ; preds = %start
  %lhs_neg.i.i = icmp slt i128 %a, 0
  %rhs_neg.i.i = icmp slt i128 %b, 0
  %1 = sub i128 0, %a
  %spec.select.i.i = select i1 %lhs_neg.i.i, i128 %1, i128 %a
  %2 = sub i128 0, %b
  %spec.select17.i.i = select i1 %rhs_neg.i.i, i128 %2, i128 %b
  %mul_neg.i.i = xor i1 %lhs_neg.i.i, %rhs_neg.i.i
  %_2.i.i.i.i = lshr i128 %spec.select.i.i, 64
  %3 = icmp ult i128 %spec.select.i.i, 18446744073709551616
  %_2.i4.i.i.i = lshr i128 %spec.select17.i.i, 64
  %4 = icmp ult i128 %spec.select17.i.i, 18446744073709551616
  br i1 %3, label %bb6.i.i.i, label %bb5.i.i.i

bb5.i.i.i:                                        ; preds = %bb5.i.i
  br i1 %4, label %bb25.i.i.i, label %bb8.i.i.i

bb6.i.i.i:                                        ; preds = %bb5.i.i
  br i1 %4, label %bb7.i.i.i, label %bb10.i.i.i

bb10.i.i.i:                                       ; preds = %bb6.i.i.i
  %5 = and i128 %spec.select17.i.i, 18446744073709551615
  %6 = mul nuw i128 %5, %spec.select.i.i
  %7 = mul nuw nsw i128 %_2.i4.i.i.i, %spec.select.i.i
  %_2.i6.i.i.i = shl i128 %7, 64
  %8 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %6, i128 %_2.i6.i.i.i) #14
  %9 = extractvalue { i128, i1 } %8, 0
  %10 = extractvalue { i128, i1 } %8, 1
  %11 = icmp ugt i128 %7, 18446744073709551615
  %spec.select.i.i.i = select i1 %10, i1 true, i1 %11
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i

bb7.i.i.i:                                        ; preds = %bb6.i.i.i
  %12 = mul nuw i128 %spec.select17.i.i, %spec.select.i.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i

bb8.i.i.i:                                        ; preds = %bb5.i.i.i
  %13 = mul i128 %spec.select17.i.i, %spec.select.i.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i

bb25.i.i.i:                                       ; preds = %bb5.i.i.i
  %14 = and i128 %spec.select.i.i, 18446744073709551615
  %15 = mul nuw i128 %14, %spec.select17.i.i
  %16 = mul nuw nsw i128 %_2.i.i.i.i, %spec.select17.i.i
  %_2.i9.i.i.i = shl i128 %16, 64
  %17 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %15, i128 %_2.i9.i.i.i) #14
  %18 = extractvalue { i128, i1 } %17, 0
  %19 = extractvalue { i128, i1 } %17, 1
  %20 = icmp ugt i128 %16, 18446744073709551615
  %spec.select11.i.i.i = select i1 %19, i1 true, i1 %20
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i

_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i: ; preds = %bb25.i.i.i, %bb8.i.i.i, %bb7.i.i.i, %bb10.i.i.i
  %.sroa.5.0.shrunk.shrunk.i.i.i = phi i1 [ false, %bb7.i.i.i ], [ true, %bb8.i.i.i ], [ %spec.select.i.i.i, %bb10.i.i.i ], [ %spec.select11.i.i.i, %bb25.i.i.i ]
  %.sroa.0.0.i.i.i = phi i128 [ %12, %bb7.i.i.i ], [ %13, %bb8.i.i.i ], [ %9, %bb10.i.i.i ], [ %18, %bb25.i.i.i ]
  %21 = sub i128 0, %.sroa.0.0.i.i.i
  %spec.select18.i.i = select i1 %mul_neg.i.i, i128 %21, i128 %.sroa.0.0.i.i.i
  %_35.i.i = icmp slt i128 %spec.select18.i.i, 0
  %_34.i.i = xor i1 %mul_neg.i.i, %_35.i.i
  %narrow.i.i = select i1 %_34.i.i, i1 true, i1 %.sroa.5.0.shrunk.shrunk.i.i.i
  %phi.cast.i = zext i1 %narrow.i.i to i32
  br label %_ZN17compiler_builtins3int3mul9__muloti417h9010b4dc2a8b3d17E.exit

_ZN17compiler_builtins3int3mul9__muloti417h9010b4dc2a8b3d17E.exit: ; preds = %start, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i
  %.sroa.4.0.i.i = phi i32 [ %phi.cast.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i ], [ 0, %start ]
  %.sroa.0.0.i.i = phi i128 [ %spec.select18.i.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i ], [ 0, %start ]
  store i32 %.sroa.4.0.i.i, i32* %oflow, align 4, !alias.scope !26
  ret i128 %.sroa.0.0.i.i
}

; compiler_builtins::int::mul::__rust_i128_mulo
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn writeonly
define hidden void @_ZN17compiler_builtins3int3mul16__rust_i128_mulo17heeb8c85e3487da40E({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #4 {
start:
  %1 = icmp eq i128 %a, 0
  %_7.i = icmp eq i128 %b, 0
  %_5.0.i = select i1 %1, i1 true, i1 %_7.i
  br i1 %_5.0.i, label %_ZN17compiler_builtins3int3mul20i128_overflowing_mul17hc5174033bee7bb3dE.exit, label %bb5.i

bb5.i:                                            ; preds = %start
  %lhs_neg.i = icmp slt i128 %a, 0
  %rhs_neg.i = icmp slt i128 %b, 0
  %2 = sub i128 0, %a
  %spec.select.i = select i1 %lhs_neg.i, i128 %2, i128 %a
  %3 = sub i128 0, %b
  %spec.select17.i = select i1 %rhs_neg.i, i128 %3, i128 %b
  %mul_neg.i = xor i1 %lhs_neg.i, %rhs_neg.i
  %_2.i.i.i = lshr i128 %spec.select.i, 64
  %4 = icmp ult i128 %spec.select.i, 18446744073709551616
  %_2.i4.i.i = lshr i128 %spec.select17.i, 64
  %5 = icmp ult i128 %spec.select17.i, 18446744073709551616
  br i1 %4, label %bb6.i.i, label %bb5.i.i

bb5.i.i:                                          ; preds = %bb5.i
  br i1 %5, label %bb25.i.i, label %bb8.i.i

bb6.i.i:                                          ; preds = %bb5.i
  br i1 %5, label %bb7.i.i, label %bb10.i.i

bb10.i.i:                                         ; preds = %bb6.i.i
  %6 = and i128 %spec.select17.i, 18446744073709551615
  %7 = mul nuw i128 %6, %spec.select.i
  %8 = mul nuw nsw i128 %_2.i4.i.i, %spec.select.i
  %_2.i6.i.i = shl i128 %8, 64
  %9 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %7, i128 %_2.i6.i.i) #14
  %10 = extractvalue { i128, i1 } %9, 0
  %11 = extractvalue { i128, i1 } %9, 1
  %12 = icmp ugt i128 %8, 18446744073709551615
  %spec.select.i.i = select i1 %11, i1 true, i1 %12
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i

bb7.i.i:                                          ; preds = %bb6.i.i
  %13 = mul nuw i128 %spec.select17.i, %spec.select.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i

bb8.i.i:                                          ; preds = %bb5.i.i
  %14 = mul i128 %spec.select17.i, %spec.select.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i

bb25.i.i:                                         ; preds = %bb5.i.i
  %15 = and i128 %spec.select.i, 18446744073709551615
  %16 = mul nuw i128 %15, %spec.select17.i
  %17 = mul nuw nsw i128 %_2.i.i.i, %spec.select17.i
  %_2.i9.i.i = shl i128 %17, 64
  %18 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %16, i128 %_2.i9.i.i) #14
  %19 = extractvalue { i128, i1 } %18, 0
  %20 = extractvalue { i128, i1 } %18, 1
  %21 = icmp ugt i128 %17, 18446744073709551615
  %spec.select11.i.i = select i1 %20, i1 true, i1 %21
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i

_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i: ; preds = %bb25.i.i, %bb8.i.i, %bb7.i.i, %bb10.i.i
  %.sroa.5.0.shrunk.shrunk.i.i = phi i1 [ false, %bb7.i.i ], [ true, %bb8.i.i ], [ %spec.select.i.i, %bb10.i.i ], [ %spec.select11.i.i, %bb25.i.i ]
  %.sroa.0.0.i.i = phi i128 [ %13, %bb7.i.i ], [ %14, %bb8.i.i ], [ %10, %bb10.i.i ], [ %19, %bb25.i.i ]
  %22 = sub i128 0, %.sroa.0.0.i.i
  %spec.select18.i = select i1 %mul_neg.i, i128 %22, i128 %.sroa.0.0.i.i
  %_35.i = icmp slt i128 %spec.select18.i, 0
  %_34.i = xor i1 %mul_neg.i, %_35.i
  %narrow.i = select i1 %_34.i, i1 true, i1 %.sroa.5.0.shrunk.shrunk.i.i
  %..i = zext i1 %narrow.i to i8
  br label %_ZN17compiler_builtins3int3mul20i128_overflowing_mul17hc5174033bee7bb3dE.exit

_ZN17compiler_builtins3int3mul20i128_overflowing_mul17hc5174033bee7bb3dE.exit: ; preds = %start, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i
  %.sroa.4.0.i = phi i8 [ %..i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i ], [ 0, %start ]
  %.sroa.0.0.i = phi i128 [ %spec.select18.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i ], [ 0, %start ]
  %23 = insertvalue { i128, i8 } undef, i128 %.sroa.0.0.i, 0
  %24 = insertvalue { i128, i8 } %23, i8 %.sroa.4.0.i, 1
  store { i128, i8 } %24, { i128, i8 }* %0, align 8
  ret void
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define hidden void @__rust_i128_mulo({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #5 {
start:
  %1 = icmp eq i128 %a, 0
  %_7.i.i = icmp eq i128 %b, 0
  %_5.0.i.i = select i1 %1, i1 true, i1 %_7.i.i
  br i1 %_5.0.i.i, label %_ZN17compiler_builtins3int3mul16__rust_i128_mulo17heeb8c85e3487da40E.exit, label %bb5.i.i

bb5.i.i:                                          ; preds = %start
  %lhs_neg.i.i = icmp slt i128 %a, 0
  %rhs_neg.i.i = icmp slt i128 %b, 0
  %2 = sub i128 0, %a
  %spec.select.i.i = select i1 %lhs_neg.i.i, i128 %2, i128 %a
  %3 = sub i128 0, %b
  %spec.select17.i.i = select i1 %rhs_neg.i.i, i128 %3, i128 %b
  %mul_neg.i.i = xor i1 %lhs_neg.i.i, %rhs_neg.i.i
  %_2.i.i.i.i = lshr i128 %spec.select.i.i, 64
  %4 = icmp ult i128 %spec.select.i.i, 18446744073709551616
  %_2.i4.i.i.i = lshr i128 %spec.select17.i.i, 64
  %5 = icmp ult i128 %spec.select17.i.i, 18446744073709551616
  br i1 %4, label %bb6.i.i.i, label %bb5.i.i.i

bb5.i.i.i:                                        ; preds = %bb5.i.i
  br i1 %5, label %bb25.i.i.i, label %bb8.i.i.i

bb6.i.i.i:                                        ; preds = %bb5.i.i
  br i1 %5, label %bb7.i.i.i, label %bb10.i.i.i

bb10.i.i.i:                                       ; preds = %bb6.i.i.i
  %6 = and i128 %spec.select17.i.i, 18446744073709551615
  %7 = mul nuw i128 %6, %spec.select.i.i
  %8 = mul nuw nsw i128 %_2.i4.i.i.i, %spec.select.i.i
  %_2.i6.i.i.i = shl i128 %8, 64
  %9 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %7, i128 %_2.i6.i.i.i) #14
  %10 = extractvalue { i128, i1 } %9, 0
  %11 = extractvalue { i128, i1 } %9, 1
  %12 = icmp ugt i128 %8, 18446744073709551615
  %spec.select.i.i.i = select i1 %11, i1 true, i1 %12
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i

bb7.i.i.i:                                        ; preds = %bb6.i.i.i
  %13 = mul nuw i128 %spec.select17.i.i, %spec.select.i.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i

bb8.i.i.i:                                        ; preds = %bb5.i.i.i
  %14 = mul i128 %spec.select17.i.i, %spec.select.i.i
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i

bb25.i.i.i:                                       ; preds = %bb5.i.i.i
  %15 = and i128 %spec.select.i.i, 18446744073709551615
  %16 = mul nuw i128 %15, %spec.select17.i.i
  %17 = mul nuw nsw i128 %_2.i.i.i.i, %spec.select17.i.i
  %_2.i9.i.i.i = shl i128 %17, 64
  %18 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %16, i128 %_2.i9.i.i.i) #14
  %19 = extractvalue { i128, i1 } %18, 0
  %20 = extractvalue { i128, i1 } %18, 1
  %21 = icmp ugt i128 %17, 18446744073709551615
  %spec.select11.i.i.i = select i1 %20, i1 true, i1 %21
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i

_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i: ; preds = %bb25.i.i.i, %bb8.i.i.i, %bb7.i.i.i, %bb10.i.i.i
  %.sroa.5.0.shrunk.shrunk.i.i.i = phi i1 [ false, %bb7.i.i.i ], [ true, %bb8.i.i.i ], [ %spec.select.i.i.i, %bb10.i.i.i ], [ %spec.select11.i.i.i, %bb25.i.i.i ]
  %.sroa.0.0.i.i.i = phi i128 [ %13, %bb7.i.i.i ], [ %14, %bb8.i.i.i ], [ %10, %bb10.i.i.i ], [ %19, %bb25.i.i.i ]
  %22 = sub i128 0, %.sroa.0.0.i.i.i
  %spec.select18.i.i = select i1 %mul_neg.i.i, i128 %22, i128 %.sroa.0.0.i.i.i
  %_35.i.i = icmp slt i128 %spec.select18.i.i, 0
  %_34.i.i = xor i1 %mul_neg.i.i, %_35.i.i
  %narrow.i.i = select i1 %_34.i.i, i1 true, i1 %.sroa.5.0.shrunk.shrunk.i.i.i
  %..i.i = zext i1 %narrow.i.i to i8
  br label %_ZN17compiler_builtins3int3mul16__rust_i128_mulo17heeb8c85e3487da40E.exit

_ZN17compiler_builtins3int3mul16__rust_i128_mulo17heeb8c85e3487da40E.exit: ; preds = %start, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i
  %.sroa.4.0.i.i = phi i8 [ %..i.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i ], [ 0, %start ]
  %.sroa.0.0.i.i = phi i128 [ %spec.select18.i.i, %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit.i.i ], [ 0, %start ]
  %23 = insertvalue { i128, i8 } undef, i128 %.sroa.0.0.i.i, 0
  %24 = insertvalue { i128, i8 } %23, i8 %.sroa.4.0.i.i, 1
  store { i128, i8 } %24, { i128, i8 }* %0, align 8, !alias.scope !29
  ret void
}

; compiler_builtins::int::mul::__rust_u128_mulo
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn writeonly
define hidden void @_ZN17compiler_builtins3int3mul16__rust_u128_mulo17hb5426c52cc38e8b6E({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #4 {
start:
  %_2.i.i = lshr i128 %a, 64
  %1 = icmp ult i128 %a, 18446744073709551616
  %_2.i4.i = lshr i128 %b, 64
  %2 = icmp ult i128 %b, 18446744073709551616
  br i1 %1, label %bb6.i, label %bb5.i

bb5.i:                                            ; preds = %start
  br i1 %2, label %bb25.i, label %bb8.i

bb6.i:                                            ; preds = %start
  br i1 %2, label %bb7.i, label %bb10.i

bb10.i:                                           ; preds = %bb6.i
  %3 = and i128 %b, 18446744073709551615
  %4 = mul nuw i128 %3, %a
  %5 = mul nuw i128 %_2.i4.i, %a
  %_2.i6.i = shl i128 %5, 64
  %6 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %4, i128 %_2.i6.i) #14
  %7 = extractvalue { i128, i1 } %6, 0
  %8 = extractvalue { i128, i1 } %6, 1
  %9 = icmp ugt i128 %5, 18446744073709551615
  %spec.select.i = select i1 %8, i1 true, i1 %9
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit

bb7.i:                                            ; preds = %bb6.i
  %10 = mul nuw i128 %b, %a
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit

bb8.i:                                            ; preds = %bb5.i
  %11 = mul i128 %b, %a
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit

bb25.i:                                           ; preds = %bb5.i
  %12 = and i128 %a, 18446744073709551615
  %13 = mul nuw i128 %12, %b
  %14 = mul nuw i128 %_2.i.i, %b
  %_2.i9.i = shl i128 %14, 64
  %15 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %13, i128 %_2.i9.i) #14
  %16 = extractvalue { i128, i1 } %15, 0
  %17 = extractvalue { i128, i1 } %15, 1
  %18 = icmp ugt i128 %14, 18446744073709551615
  %spec.select11.i = select i1 %17, i1 true, i1 %18
  br label %_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit

_ZN17compiler_builtins3int3mul5UMulo4mulo17ha985b76b339d38f2E.exit: ; preds = %bb10.i, %bb7.i, %bb8.i, %bb25.i
  %.sroa.5.0.shrunk.shrunk.i = phi i1 [ false, %bb7.i ], [ true, %bb8.i ], [ %spec.select.i, %bb10.i ], [ %spec.select11.i, %bb25.i ]
  %.sroa.0.0.i = phi i128 [ %10, %bb7.i ], [ %11, %bb8.i ], [ %7, %bb10.i ], [ %16, %bb25.i ]
  %.sroa.5.0.shrunk.i = zext i1 %.sroa.5.0.shrunk.shrunk.i to i8
  %19 = insertvalue { i128, i8 } undef, i128 %.sroa.0.0.i, 0
  %20 = insertvalue { i128, i8 } %19, i8 %.sroa.5.0.shrunk.i, 1
  store { i128, i8 } %20, { i128, i8 }* %0, align 8
  ret void
}

; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define hidden void @__rust_u128_mulo({ i128, i8 }* noalias nocapture sret({ i128, i8 }) dereferenceable(24) %0, i128 %a, i128 %b) unnamed_addr #5 {
start:
  %_2.i.i.i = lshr i128 %a, 64
  %1 = icmp ult i128 %a, 18446744073709551616
  %_2.i4.i.i = lshr i128 %b, 64
  %2 = icmp ult i128 %b, 18446744073709551616
  br i1 %1, label %bb6.i.i, label %bb5.i.i

bb5.i.i:                                          ; preds = %start
  br i1 %2, label %bb25.i.i, label %bb8.i.i

bb6.i.i:                                          ; preds = %start
  br i1 %2, label %bb7.i.i, label %bb10.i.i

bb10.i.i:                                         ; preds = %bb6.i.i
  %3 = and i128 %b, 18446744073709551615
  %4 = mul nuw i128 %3, %a
  %5 = mul nuw i128 %_2.i4.i.i, %a
  %_2.i6.i.i = shl i128 %5, 64
  %6 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %4, i128 %_2.i6.i.i) #14
  %7 = extractvalue { i128, i1 } %6, 0
  %8 = extractvalue { i128, i1 } %6, 1
  %9 = icmp ugt i128 %5, 18446744073709551615
  %spec.select.i.i = select i1 %8, i1 true, i1 %9
  br label %_ZN17compiler_builtins3int3mul16__rust_u128_mulo17hb5426c52cc38e8b6E.exit

bb7.i.i:                                          ; preds = %bb6.i.i
  %10 = mul nuw i128 %b, %a
  br label %_ZN17compiler_builtins3int3mul16__rust_u128_mulo17hb5426c52cc38e8b6E.exit

bb8.i.i:                                          ; preds = %bb5.i.i
  %11 = mul i128 %b, %a
  br label %_ZN17compiler_builtins3int3mul16__rust_u128_mulo17hb5426c52cc38e8b6E.exit

bb25.i.i:                                         ; preds = %bb5.i.i
  %12 = and i128 %a, 18446744073709551615
  %13 = mul nuw i128 %12, %b
  %14 = mul nuw i128 %_2.i.i.i, %b
  %_2.i9.i.i = shl i128 %14, 64
  %15 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %13, i128 %_2.i9.i.i) #14
  %16 = extractvalue { i128, i1 } %15, 0
  %17 = extractvalue { i128, i1 } %15, 1
  %18 = icmp ugt i128 %14, 18446744073709551615
  %spec.select11.i.i = select i1 %17, i1 true, i1 %18
  br label %_ZN17compiler_builtins3int3mul16__rust_u128_mulo17hb5426c52cc38e8b6E.exit

_ZN17compiler_builtins3int3mul16__rust_u128_mulo17hb5426c52cc38e8b6E.exit: ; preds = %bb10.i.i, %bb7.i.i, %bb8.i.i, %bb25.i.i
  %.sroa.5.0.shrunk.shrunk.i.i = phi i1 [ false, %bb7.i.i ], [ true, %bb8.i.i ], [ %spec.select.i.i, %bb10.i.i ], [ %spec.select11.i.i, %bb25.i.i ]
  %.sroa.0.0.i.i = phi i128 [ %10, %bb7.i.i ], [ %11, %bb8.i.i ], [ %7, %bb10.i.i ], [ %16, %bb25.i.i ]
  %.sroa.5.0.shrunk.i.i = zext i1 %.sroa.5.0.shrunk.shrunk.i.i to i8
  %19 = insertvalue { i128, i8 } undef, i128 %.sroa.0.0.i.i, 0
  %20 = insertvalue { i128, i8 } %19, i8 %.sroa.5.0.shrunk.i.i, 1
  store { i128, i8 } %20, { i128, i8 }* %0, align 8, !alias.scope !32
  ret void
}

; compiler_builtins::int::sdiv::__divmodsi4
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i32 @_ZN17compiler_builtins3int4sdiv11__divmodsi417h80e0e1a8a998731dE(i32 %a, i32 %b, i32* noalias nocapture align 4 dereferenceable(4) %rem) unnamed_addr #6 {
start:
  %a_neg = icmp slt i32 %a, 0
  %b_neg = icmp slt i32 %b, 0
  %0 = sub i32 0, %a
  %spec.select = select i1 %a_neg, i32 %0, i32 %a
  %1 = sub i32 0, %b
  %spec.select13 = select i1 %b_neg, i32 %1, i32 %b
  %2 = icmp ne i32 %b, 0
  tail call void @llvm.assume(i1 %2) #14
  %_5.i.i = icmp ult i32 %spec.select, %spec.select13
  br i1 %_5.i.i, label %_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit, label %bb4.i.i

bb4.i.i:                                          ; preds = %start
  %3 = tail call i32 @llvm.ctlz.i32(i32 %spec.select13, i1 true) #14, !range !5
  %4 = tail call i32 @llvm.ctlz.i32(i32 %spec.select, i1 false) #14, !range !5
  %_4.i.i.i = sub nsw i32 %3, %4
  %5 = zext i32 %_4.i.i.i to i64
  %6 = and i32 %_4.i.i.i, 31
  %_12.i.i.i = shl i32 %spec.select13, %6
  %_10.i.i.i = icmp ugt i32 %_12.i.i.i, %spec.select
  %7 = sext i1 %_10.i.i.i to i64
  %shl.0.i.i.i = add nsw i64 %7, %5
  %8 = trunc i64 %shl.0.i.i.i to i32
  %9 = and i32 %8, 31
  %10 = shl i32 %spec.select13, %9
  %11 = sub i32 %spec.select, %10
  %12 = shl nuw i32 1, %9
  %_21.i.i = icmp ult i32 %11, %spec.select13
  br i1 %_21.i.i, label %_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit, label %bb8.i.i

bb8.i.i:                                          ; preds = %bb4.i.i
  %_27.i.i = icmp slt i32 %10, 0
  br i1 %_27.i.i, label %bb9.i.i, label %bb16.i.i

bb9.i.i:                                          ; preds = %bb8.i.i
  %13 = lshr i32 %10, 1
  %14 = add nsw i64 %shl.0.i.i.i, -1
  %15 = trunc i64 %14 to i32
  %16 = and i32 %15, 31
  %tmp.i.i = shl nuw i32 1, %16
  %17 = sub i32 %11, %13
  %_36.i.i = icmp sgt i32 %17, -1
  %18 = select i1 %_36.i.i, i32 %tmp.i.i, i32 0
  %spec.select.i.i = or i32 %18, %12
  %spec.select38.i.i = select i1 %_36.i.i, i32 %17, i32 %11
  %_41.i.i = icmp ult i32 %spec.select38.i.i, %spec.select13
  br i1 %_41.i.i, label %_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit, label %bb16.i.i

bb16.i.i:                                         ; preds = %bb9.i.i, %bb8.i.i
  %mask.0.in.i.i = phi i32 [ %tmp.i.i, %bb9.i.i ], [ %12, %bb8.i.i ]
  %quo.1.i.i = phi i32 [ %spec.select.i.i, %bb9.i.i ], [ %12, %bb8.i.i ]
  %div2.0.i.i = phi i32 [ %13, %bb9.i.i ], [ %10, %bb8.i.i ]
  %shl.0.i.i = phi i64 [ %14, %bb9.i.i ], [ %shl.0.i.i.i, %bb8.i.i ]
  %duo1.1.i.i = phi i32 [ %spec.select38.i.i, %bb9.i.i ], [ %11, %bb8.i.i ]
  %mask.0.i.i = add i32 %mask.0.in.i.i, -1
  %19 = add nsw i32 %div2.0.i.i, -1
  %20 = icmp eq i64 %shl.0.i.i, 0
  br i1 %20, label %bb19.i.i, label %bb20.i.i.preheader

bb20.i.i.preheader:                               ; preds = %bb16.i.i
  %21 = add nsw i64 %shl.0.i.i, -1
  %xtraiter = and i64 %shl.0.i.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol

bb20.i.i.prol:                                    ; preds = %bb20.i.i.preheader, %bb20.i.i.prol
  %duo1.240.i.i.prol = phi i32 [ %26, %bb20.i.i.prol ], [ %duo1.1.i.i, %bb20.i.i.preheader ]
  %i.039.i.i.prol = phi i64 [ %22, %bb20.i.i.prol ], [ %shl.0.i.i, %bb20.i.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.i.prol ], [ %xtraiter, %bb20.i.i.preheader ]
  %22 = add i64 %i.039.i.i.prol, -1
  %23 = shl i32 %duo1.240.i.i.prol, 1
  %24 = sub i32 %23, %19
  %25 = ashr i32 %24, 31
  %_62.i.i.prol = and i32 %25, %19
  %26 = add i32 %_62.i.i.prol, %24
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol, !llvm.loop !35

bb20.i.i.prol.loopexit:                           ; preds = %bb20.i.i.prol, %bb20.i.i.preheader
  %.lcssa.unr = phi i32 [ undef, %bb20.i.i.preheader ], [ %26, %bb20.i.i.prol ]
  %duo1.240.i.i.unr = phi i32 [ %duo1.1.i.i, %bb20.i.i.preheader ], [ %26, %bb20.i.i.prol ]
  %i.039.i.i.unr = phi i64 [ %shl.0.i.i, %bb20.i.i.preheader ], [ %22, %bb20.i.i.prol ]
  %27 = icmp ult i64 %21, 3
  br i1 %27, label %bb19.i.i, label %bb20.i.i

bb19.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i, %bb16.i.i
  %duo1.2.lcssa.i.i = phi i32 [ %duo1.1.i.i, %bb16.i.i ], [ %.lcssa.unr, %bb20.i.i.prol.loopexit ], [ %46, %bb20.i.i ]
  %_66.i.i = and i32 %duo1.2.lcssa.i.i, %mask.0.i.i
  %_65.i.i = or i32 %_66.i.i, %quo.1.i.i
  %28 = trunc i64 %shl.0.i.i to i32
  %29 = and i32 %28, 31
  %_70.i.i = lshr i32 %duo1.2.lcssa.i.i, %29
  br label %_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit

bb20.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i
  %duo1.240.i.i = phi i32 [ %46, %bb20.i.i ], [ %duo1.240.i.i.unr, %bb20.i.i.prol.loopexit ]
  %i.039.i.i = phi i64 [ %42, %bb20.i.i ], [ %i.039.i.i.unr, %bb20.i.i.prol.loopexit ]
  %30 = shl i32 %duo1.240.i.i, 1
  %31 = sub i32 %30, %19
  %32 = ashr i32 %31, 31
  %_62.i.i = and i32 %32, %19
  %33 = add i32 %_62.i.i, %31
  %34 = shl i32 %33, 1
  %35 = sub i32 %34, %19
  %36 = ashr i32 %35, 31
  %_62.i.i.1 = and i32 %36, %19
  %37 = add i32 %_62.i.i.1, %35
  %38 = shl i32 %37, 1
  %39 = sub i32 %38, %19
  %40 = ashr i32 %39, 31
  %_62.i.i.2 = and i32 %40, %19
  %41 = add i32 %_62.i.i.2, %39
  %42 = add i64 %i.039.i.i, -4
  %43 = shl i32 %41, 1
  %44 = sub i32 %43, %19
  %45 = ashr i32 %44, 31
  %_62.i.i.3 = and i32 %45, %19
  %46 = add i32 %_62.i.i.3, %44
  %47 = icmp eq i64 %42, 0
  br i1 %47, label %bb19.i.i, label %bb20.i.i

_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit: ; preds = %start, %bb4.i.i, %bb9.i.i, %bb19.i.i
  %.sroa.5.0.i.i = phi i32 [ %spec.select, %start ], [ %11, %bb4.i.i ], [ %_70.i.i, %bb19.i.i ], [ %spec.select38.i.i, %bb9.i.i ]
  %.sroa.0.0.i.i = phi i32 [ 0, %start ], [ %12, %bb4.i.i ], [ %_65.i.i, %bb19.i.i ], [ %spec.select.i.i, %bb9.i.i ]
  br i1 %a_neg, label %bb10, label %_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit.bb10_crit_edge

_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit.bb10_crit_edge: ; preds = %_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit
  store i32 %.sroa.5.0.i.i, i32* %rem, align 4
  br i1 %b_neg, label %bb11, label %bb14

bb10:                                             ; preds = %_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit
  %48 = sub i32 0, %.sroa.5.0.i.i
  store i32 %48, i32* %rem, align 4
  br i1 %b_neg, label %bb14, label %bb11

bb11:                                             ; preds = %_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit.bb10_crit_edge, %bb10
  %49 = sub i32 0, %.sroa.0.0.i.i
  br label %bb14

bb14:                                             ; preds = %_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit.bb10_crit_edge, %bb10, %bb11
  %.0 = phi i32 [ %49, %bb11 ], [ %.sroa.0.0.i.i, %bb10 ], [ %.sroa.0.0.i.i, %_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE.exit.bb10_crit_edge ]
  ret i32 %.0
}

; compiler_builtins::int::sdiv::__divsi3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i32 @_ZN17compiler_builtins3int4sdiv8__divsi317hadf668fbdbc4a68aE(i32 %a, i32 %b) unnamed_addr #6 {
start:
  %a_neg = icmp slt i32 %a, 0
  %b_neg = icmp slt i32 %b, 0
  %0 = sub i32 0, %a
  %spec.select = select i1 %a_neg, i32 %0, i32 %a
  %1 = sub i32 0, %b
  %spec.select7 = select i1 %b_neg, i32 %1, i32 %b
  %2 = icmp ne i32 %b, 0
  tail call void @llvm.assume(i1 %2) #14
  %_5.i.i = icmp ult i32 %spec.select, %spec.select7
  br i1 %_5.i.i, label %_ZN17compiler_builtins3int4udiv9__udivsi317h50584612c9e69bf1E.exit, label %bb4.i.i

bb4.i.i:                                          ; preds = %start
  %3 = tail call i32 @llvm.ctlz.i32(i32 %spec.select7, i1 true) #14, !range !5
  %4 = tail call i32 @llvm.ctlz.i32(i32 %spec.select, i1 false) #14, !range !5
  %_4.i.i.i = sub nsw i32 %3, %4
  %5 = zext i32 %_4.i.i.i to i64
  %6 = and i32 %_4.i.i.i, 31
  %_12.i.i.i = shl i32 %spec.select7, %6
  %_10.i.i.i = icmp ugt i32 %_12.i.i.i, %spec.select
  %7 = sext i1 %_10.i.i.i to i64
  %shl.0.i.i.i = add nsw i64 %7, %5
  %8 = trunc i64 %shl.0.i.i.i to i32
  %9 = and i32 %8, 31
  %10 = shl i32 %spec.select7, %9
  %11 = sub i32 %spec.select, %10
  %12 = shl nuw i32 1, %9
  %_21.i.i = icmp ult i32 %11, %spec.select7
  br i1 %_21.i.i, label %_ZN17compiler_builtins3int4udiv9__udivsi317h50584612c9e69bf1E.exit, label %bb8.i.i

bb8.i.i:                                          ; preds = %bb4.i.i
  %_27.i.i = icmp slt i32 %10, 0
  br i1 %_27.i.i, label %bb9.i.i, label %bb16.i.i

bb9.i.i:                                          ; preds = %bb8.i.i
  %13 = lshr i32 %10, 1
  %14 = add nsw i64 %shl.0.i.i.i, -1
  %15 = trunc i64 %14 to i32
  %16 = and i32 %15, 31
  %tmp.i.i = shl nuw i32 1, %16
  %17 = sub i32 %11, %13
  %_36.i.i = icmp sgt i32 %17, -1
  %18 = select i1 %_36.i.i, i32 %tmp.i.i, i32 0
  %spec.select.i.i = or i32 %18, %12
  %spec.select38.i.i = select i1 %_36.i.i, i32 %17, i32 %11
  %_41.i.i = icmp ult i32 %spec.select38.i.i, %spec.select7
  br i1 %_41.i.i, label %_ZN17compiler_builtins3int4udiv9__udivsi317h50584612c9e69bf1E.exit, label %bb16.i.i

bb16.i.i:                                         ; preds = %bb9.i.i, %bb8.i.i
  %mask.0.in.i.i = phi i32 [ %tmp.i.i, %bb9.i.i ], [ %12, %bb8.i.i ]
  %quo.1.i.i = phi i32 [ %spec.select.i.i, %bb9.i.i ], [ %12, %bb8.i.i ]
  %div2.0.i.i = phi i32 [ %13, %bb9.i.i ], [ %10, %bb8.i.i ]
  %shl.0.i.i = phi i64 [ %14, %bb9.i.i ], [ %shl.0.i.i.i, %bb8.i.i ]
  %duo1.1.i.i = phi i32 [ %spec.select38.i.i, %bb9.i.i ], [ %11, %bb8.i.i ]
  %mask.0.i.i = add i32 %mask.0.in.i.i, -1
  %19 = add nsw i32 %div2.0.i.i, -1
  %20 = icmp eq i64 %shl.0.i.i, 0
  br i1 %20, label %bb19.i.i, label %bb20.i.i.preheader

bb20.i.i.preheader:                               ; preds = %bb16.i.i
  %21 = add nsw i64 %shl.0.i.i, -1
  %xtraiter = and i64 %shl.0.i.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol

bb20.i.i.prol:                                    ; preds = %bb20.i.i.preheader, %bb20.i.i.prol
  %duo1.240.i.i.prol = phi i32 [ %26, %bb20.i.i.prol ], [ %duo1.1.i.i, %bb20.i.i.preheader ]
  %i.039.i.i.prol = phi i64 [ %22, %bb20.i.i.prol ], [ %shl.0.i.i, %bb20.i.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.i.prol ], [ %xtraiter, %bb20.i.i.preheader ]
  %22 = add i64 %i.039.i.i.prol, -1
  %23 = shl i32 %duo1.240.i.i.prol, 1
  %24 = sub i32 %23, %19
  %25 = ashr i32 %24, 31
  %_62.i.i.prol = and i32 %25, %19
  %26 = add i32 %_62.i.i.prol, %24
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol, !llvm.loop !37

bb20.i.i.prol.loopexit:                           ; preds = %bb20.i.i.prol, %bb20.i.i.preheader
  %.lcssa.unr = phi i32 [ undef, %bb20.i.i.preheader ], [ %26, %bb20.i.i.prol ]
  %duo1.240.i.i.unr = phi i32 [ %duo1.1.i.i, %bb20.i.i.preheader ], [ %26, %bb20.i.i.prol ]
  %i.039.i.i.unr = phi i64 [ %shl.0.i.i, %bb20.i.i.preheader ], [ %22, %bb20.i.i.prol ]
  %27 = icmp ult i64 %21, 3
  br i1 %27, label %bb19.i.i, label %bb20.i.i

bb19.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i, %bb16.i.i
  %duo1.2.lcssa.i.i = phi i32 [ %duo1.1.i.i, %bb16.i.i ], [ %.lcssa.unr, %bb20.i.i.prol.loopexit ], [ %44, %bb20.i.i ]
  %_66.i.i = and i32 %duo1.2.lcssa.i.i, %mask.0.i.i
  %_65.i.i = or i32 %_66.i.i, %quo.1.i.i
  br label %_ZN17compiler_builtins3int4udiv9__udivsi317h50584612c9e69bf1E.exit

bb20.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i
  %duo1.240.i.i = phi i32 [ %44, %bb20.i.i ], [ %duo1.240.i.i.unr, %bb20.i.i.prol.loopexit ]
  %i.039.i.i = phi i64 [ %40, %bb20.i.i ], [ %i.039.i.i.unr, %bb20.i.i.prol.loopexit ]
  %28 = shl i32 %duo1.240.i.i, 1
  %29 = sub i32 %28, %19
  %30 = ashr i32 %29, 31
  %_62.i.i = and i32 %30, %19
  %31 = add i32 %_62.i.i, %29
  %32 = shl i32 %31, 1
  %33 = sub i32 %32, %19
  %34 = ashr i32 %33, 31
  %_62.i.i.1 = and i32 %34, %19
  %35 = add i32 %_62.i.i.1, %33
  %36 = shl i32 %35, 1
  %37 = sub i32 %36, %19
  %38 = ashr i32 %37, 31
  %_62.i.i.2 = and i32 %38, %19
  %39 = add i32 %_62.i.i.2, %37
  %40 = add i64 %i.039.i.i, -4
  %41 = shl i32 %39, 1
  %42 = sub i32 %41, %19
  %43 = ashr i32 %42, 31
  %_62.i.i.3 = and i32 %43, %19
  %44 = add i32 %_62.i.i.3, %42
  %45 = icmp eq i64 %40, 0
  br i1 %45, label %bb19.i.i, label %bb20.i.i

_ZN17compiler_builtins3int4udiv9__udivsi317h50584612c9e69bf1E.exit: ; preds = %start, %bb4.i.i, %bb9.i.i, %bb19.i.i
  %.sroa.0.0.i.i = phi i32 [ 0, %start ], [ %12, %bb4.i.i ], [ %_65.i.i, %bb19.i.i ], [ %spec.select.i.i, %bb9.i.i ]
  %_21 = xor i1 %a_neg, %b_neg
  %46 = sub i32 0, %.sroa.0.0.i.i
  %spec.select8 = select i1 %_21, i32 %46, i32 %.sroa.0.0.i.i
  ret i32 %spec.select8
}

; compiler_builtins::int::sdiv::__modsi3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i32 @_ZN17compiler_builtins3int4sdiv8__modsi317hce16a5cedea8ae25E(i32 %a, i32 %b) unnamed_addr #6 {
start:
  %a_neg = icmp slt i32 %a, 0
  %0 = sub i32 0, %a
  %spec.select = select i1 %a_neg, i32 %0, i32 %a
  %1 = tail call i32 @llvm.abs.i32(i32 %b, i1 false)
  %2 = icmp ne i32 %b, 0
  tail call void @llvm.assume(i1 %2) #14
  %_5.i.i = icmp ult i32 %spec.select, %1
  br i1 %_5.i.i, label %_ZN17compiler_builtins3int4udiv9__umodsi317hcf330f4b294364f6E.exit, label %bb4.i.i

bb4.i.i:                                          ; preds = %start
  %3 = tail call i32 @llvm.ctlz.i32(i32 %1, i1 true) #14, !range !5
  %4 = tail call i32 @llvm.ctlz.i32(i32 %spec.select, i1 false) #14, !range !5
  %_4.i.i.i = sub nsw i32 %3, %4
  %5 = zext i32 %_4.i.i.i to i64
  %6 = and i32 %_4.i.i.i, 31
  %_12.i.i.i = shl i32 %1, %6
  %_10.i.i.i = icmp ugt i32 %_12.i.i.i, %spec.select
  %7 = sext i1 %_10.i.i.i to i64
  %shl.0.i.i.i = add nsw i64 %7, %5
  %8 = trunc i64 %shl.0.i.i.i to i32
  %9 = and i32 %8, 31
  %10 = shl i32 %1, %9
  %11 = sub i32 %spec.select, %10
  %_21.i.i = icmp ult i32 %11, %1
  br i1 %_21.i.i, label %_ZN17compiler_builtins3int4udiv9__umodsi317hcf330f4b294364f6E.exit, label %bb8.i.i

bb8.i.i:                                          ; preds = %bb4.i.i
  %_27.i.i = icmp slt i32 %10, 0
  br i1 %_27.i.i, label %bb9.i.i, label %bb16.i.i

bb9.i.i:                                          ; preds = %bb8.i.i
  %12 = lshr i32 %10, 1
  %13 = add nsw i64 %shl.0.i.i.i, -1
  %14 = sub i32 %11, %12
  %_36.i.i = icmp sgt i32 %14, -1
  %spec.select38.i.i = select i1 %_36.i.i, i32 %14, i32 %11
  %_41.i.i = icmp ult i32 %spec.select38.i.i, %1
  br i1 %_41.i.i, label %_ZN17compiler_builtins3int4udiv9__umodsi317hcf330f4b294364f6E.exit, label %bb16.i.i

bb16.i.i:                                         ; preds = %bb9.i.i, %bb8.i.i
  %div2.0.i.i = phi i32 [ %12, %bb9.i.i ], [ %10, %bb8.i.i ]
  %shl.0.i.i = phi i64 [ %13, %bb9.i.i ], [ %shl.0.i.i.i, %bb8.i.i ]
  %duo1.1.i.i = phi i32 [ %spec.select38.i.i, %bb9.i.i ], [ %11, %bb8.i.i ]
  %15 = add nsw i32 %div2.0.i.i, -1
  %16 = icmp eq i64 %shl.0.i.i, 0
  br i1 %16, label %bb19.i.i, label %bb20.i.i.preheader

bb20.i.i.preheader:                               ; preds = %bb16.i.i
  %17 = add nsw i64 %shl.0.i.i, -1
  %xtraiter = and i64 %shl.0.i.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol

bb20.i.i.prol:                                    ; preds = %bb20.i.i.preheader, %bb20.i.i.prol
  %duo1.240.i.i.prol = phi i32 [ %22, %bb20.i.i.prol ], [ %duo1.1.i.i, %bb20.i.i.preheader ]
  %i.039.i.i.prol = phi i64 [ %18, %bb20.i.i.prol ], [ %shl.0.i.i, %bb20.i.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.i.prol ], [ %xtraiter, %bb20.i.i.preheader ]
  %18 = add i64 %i.039.i.i.prol, -1
  %19 = shl i32 %duo1.240.i.i.prol, 1
  %20 = sub i32 %19, %15
  %21 = ashr i32 %20, 31
  %_62.i.i.prol = and i32 %21, %15
  %22 = add i32 %_62.i.i.prol, %20
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol, !llvm.loop !38

bb20.i.i.prol.loopexit:                           ; preds = %bb20.i.i.prol, %bb20.i.i.preheader
  %.lcssa.unr = phi i32 [ undef, %bb20.i.i.preheader ], [ %22, %bb20.i.i.prol ]
  %duo1.240.i.i.unr = phi i32 [ %duo1.1.i.i, %bb20.i.i.preheader ], [ %22, %bb20.i.i.prol ]
  %i.039.i.i.unr = phi i64 [ %shl.0.i.i, %bb20.i.i.preheader ], [ %18, %bb20.i.i.prol ]
  %23 = icmp ult i64 %17, 3
  br i1 %23, label %bb19.i.i, label %bb20.i.i

bb19.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i, %bb16.i.i
  %duo1.2.lcssa.i.i = phi i32 [ %duo1.1.i.i, %bb16.i.i ], [ %.lcssa.unr, %bb20.i.i.prol.loopexit ], [ %42, %bb20.i.i ]
  %24 = trunc i64 %shl.0.i.i to i32
  %25 = and i32 %24, 31
  %_70.i.i = lshr i32 %duo1.2.lcssa.i.i, %25
  br label %_ZN17compiler_builtins3int4udiv9__umodsi317hcf330f4b294364f6E.exit

bb20.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i
  %duo1.240.i.i = phi i32 [ %42, %bb20.i.i ], [ %duo1.240.i.i.unr, %bb20.i.i.prol.loopexit ]
  %i.039.i.i = phi i64 [ %38, %bb20.i.i ], [ %i.039.i.i.unr, %bb20.i.i.prol.loopexit ]
  %26 = shl i32 %duo1.240.i.i, 1
  %27 = sub i32 %26, %15
  %28 = ashr i32 %27, 31
  %_62.i.i = and i32 %28, %15
  %29 = add i32 %_62.i.i, %27
  %30 = shl i32 %29, 1
  %31 = sub i32 %30, %15
  %32 = ashr i32 %31, 31
  %_62.i.i.1 = and i32 %32, %15
  %33 = add i32 %_62.i.i.1, %31
  %34 = shl i32 %33, 1
  %35 = sub i32 %34, %15
  %36 = ashr i32 %35, 31
  %_62.i.i.2 = and i32 %36, %15
  %37 = add i32 %_62.i.i.2, %35
  %38 = add i64 %i.039.i.i, -4
  %39 = shl i32 %37, 1
  %40 = sub i32 %39, %15
  %41 = ashr i32 %40, 31
  %_62.i.i.3 = and i32 %41, %15
  %42 = add i32 %_62.i.i.3, %40
  %43 = icmp eq i64 %38, 0
  br i1 %43, label %bb19.i.i, label %bb20.i.i

_ZN17compiler_builtins3int4udiv9__umodsi317hcf330f4b294364f6E.exit: ; preds = %start, %bb4.i.i, %bb9.i.i, %bb19.i.i
  %.sroa.5.0.i.i = phi i32 [ %spec.select, %start ], [ %11, %bb4.i.i ], [ %_70.i.i, %bb19.i.i ], [ %spec.select38.i.i, %bb9.i.i ]
  %44 = sub i32 0, %.sroa.5.0.i.i
  %spec.select8 = select i1 %a_neg, i32 %44, i32 %.sroa.5.0.i.i
  ret i32 %spec.select8
}

; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i32 @__modsi3(i32 %a, i32 %b) unnamed_addr #6 {
start:
  %a_neg.i = icmp slt i32 %a, 0
  %0 = sub i32 0, %a
  %spec.select.i = select i1 %a_neg.i, i32 %0, i32 %a
  %1 = tail call i32 @llvm.abs.i32(i32 %b, i1 false) #14
  %2 = icmp ne i32 %b, 0
  tail call void @llvm.assume(i1 %2) #14
  %_5.i.i.i = icmp ult i32 %spec.select.i, %1
  br i1 %_5.i.i.i, label %_ZN17compiler_builtins3int4sdiv8__modsi317hce16a5cedea8ae25E.exit, label %bb4.i.i.i

bb4.i.i.i:                                        ; preds = %start
  %3 = tail call i32 @llvm.ctlz.i32(i32 %1, i1 true) #14, !range !5
  %4 = tail call i32 @llvm.ctlz.i32(i32 %spec.select.i, i1 false) #14, !range !5
  %_4.i.i.i.i = sub nsw i32 %3, %4
  %5 = zext i32 %_4.i.i.i.i to i64
  %6 = and i32 %_4.i.i.i.i, 31
  %_12.i.i.i.i = shl i32 %1, %6
  %_10.i.i.i.i = icmp ugt i32 %_12.i.i.i.i, %spec.select.i
  %7 = sext i1 %_10.i.i.i.i to i64
  %shl.0.i.i.i.i = add nsw i64 %7, %5
  %8 = trunc i64 %shl.0.i.i.i.i to i32
  %9 = and i32 %8, 31
  %10 = shl i32 %1, %9
  %11 = sub i32 %spec.select.i, %10
  %_21.i.i.i = icmp ult i32 %11, %1
  br i1 %_21.i.i.i, label %_ZN17compiler_builtins3int4sdiv8__modsi317hce16a5cedea8ae25E.exit, label %bb8.i.i.i

bb8.i.i.i:                                        ; preds = %bb4.i.i.i
  %_27.i.i.i = icmp slt i32 %10, 0
  br i1 %_27.i.i.i, label %bb9.i.i.i, label %bb16.i.i.i

bb9.i.i.i:                                        ; preds = %bb8.i.i.i
  %12 = lshr i32 %10, 1
  %13 = add nsw i64 %shl.0.i.i.i.i, -1
  %14 = sub i32 %11, %12
  %_36.i.i.i = icmp sgt i32 %14, -1
  %spec.select38.i.i.i = select i1 %_36.i.i.i, i32 %14, i32 %11
  %_41.i.i.i = icmp ult i32 %spec.select38.i.i.i, %1
  br i1 %_41.i.i.i, label %_ZN17compiler_builtins3int4sdiv8__modsi317hce16a5cedea8ae25E.exit, label %bb16.i.i.i

bb16.i.i.i:                                       ; preds = %bb9.i.i.i, %bb8.i.i.i
  %div2.0.i.i.i = phi i32 [ %12, %bb9.i.i.i ], [ %10, %bb8.i.i.i ]
  %shl.0.i.i.i = phi i64 [ %13, %bb9.i.i.i ], [ %shl.0.i.i.i.i, %bb8.i.i.i ]
  %duo1.1.i.i.i = phi i32 [ %spec.select38.i.i.i, %bb9.i.i.i ], [ %11, %bb8.i.i.i ]
  %15 = add nsw i32 %div2.0.i.i.i, -1
  %16 = icmp eq i64 %shl.0.i.i.i, 0
  br i1 %16, label %bb19.i.i.i, label %bb20.i.i.i.preheader

bb20.i.i.i.preheader:                             ; preds = %bb16.i.i.i
  %17 = add nsw i64 %shl.0.i.i.i, -1
  %xtraiter = and i64 %shl.0.i.i.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.i.i.prol.loopexit, label %bb20.i.i.i.prol

bb20.i.i.i.prol:                                  ; preds = %bb20.i.i.i.preheader, %bb20.i.i.i.prol
  %duo1.240.i.i.i.prol = phi i32 [ %22, %bb20.i.i.i.prol ], [ %duo1.1.i.i.i, %bb20.i.i.i.preheader ]
  %i.039.i.i.i.prol = phi i64 [ %18, %bb20.i.i.i.prol ], [ %shl.0.i.i.i, %bb20.i.i.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.i.i.prol ], [ %xtraiter, %bb20.i.i.i.preheader ]
  %18 = add i64 %i.039.i.i.i.prol, -1
  %19 = shl i32 %duo1.240.i.i.i.prol, 1
  %20 = sub i32 %19, %15
  %21 = ashr i32 %20, 31
  %_62.i.i.i.prol = and i32 %21, %15
  %22 = add i32 %_62.i.i.i.prol, %20
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.i.i.prol.loopexit, label %bb20.i.i.i.prol, !llvm.loop !39

bb20.i.i.i.prol.loopexit:                         ; preds = %bb20.i.i.i.prol, %bb20.i.i.i.preheader
  %.lcssa.unr = phi i32 [ undef, %bb20.i.i.i.preheader ], [ %22, %bb20.i.i.i.prol ]
  %duo1.240.i.i.i.unr = phi i32 [ %duo1.1.i.i.i, %bb20.i.i.i.preheader ], [ %22, %bb20.i.i.i.prol ]
  %i.039.i.i.i.unr = phi i64 [ %shl.0.i.i.i, %bb20.i.i.i.preheader ], [ %18, %bb20.i.i.i.prol ]
  %23 = icmp ult i64 %17, 3
  br i1 %23, label %bb19.i.i.i, label %bb20.i.i.i

bb19.i.i.i:                                       ; preds = %bb20.i.i.i.prol.loopexit, %bb20.i.i.i, %bb16.i.i.i
  %duo1.2.lcssa.i.i.i = phi i32 [ %duo1.1.i.i.i, %bb16.i.i.i ], [ %.lcssa.unr, %bb20.i.i.i.prol.loopexit ], [ %42, %bb20.i.i.i ]
  %24 = trunc i64 %shl.0.i.i.i to i32
  %25 = and i32 %24, 31
  %_70.i.i.i = lshr i32 %duo1.2.lcssa.i.i.i, %25
  br label %_ZN17compiler_builtins3int4sdiv8__modsi317hce16a5cedea8ae25E.exit

bb20.i.i.i:                                       ; preds = %bb20.i.i.i.prol.loopexit, %bb20.i.i.i
  %duo1.240.i.i.i = phi i32 [ %42, %bb20.i.i.i ], [ %duo1.240.i.i.i.unr, %bb20.i.i.i.prol.loopexit ]
  %i.039.i.i.i = phi i64 [ %38, %bb20.i.i.i ], [ %i.039.i.i.i.unr, %bb20.i.i.i.prol.loopexit ]
  %26 = shl i32 %duo1.240.i.i.i, 1
  %27 = sub i32 %26, %15
  %28 = ashr i32 %27, 31
  %_62.i.i.i = and i32 %28, %15
  %29 = add i32 %_62.i.i.i, %27
  %30 = shl i32 %29, 1
  %31 = sub i32 %30, %15
  %32 = ashr i32 %31, 31
  %_62.i.i.i.1 = and i32 %32, %15
  %33 = add i32 %_62.i.i.i.1, %31
  %34 = shl i32 %33, 1
  %35 = sub i32 %34, %15
  %36 = ashr i32 %35, 31
  %_62.i.i.i.2 = and i32 %36, %15
  %37 = add i32 %_62.i.i.i.2, %35
  %38 = add i64 %i.039.i.i.i, -4
  %39 = shl i32 %37, 1
  %40 = sub i32 %39, %15
  %41 = ashr i32 %40, 31
  %_62.i.i.i.3 = and i32 %41, %15
  %42 = add i32 %_62.i.i.i.3, %40
  %43 = icmp eq i64 %38, 0
  br i1 %43, label %bb19.i.i.i, label %bb20.i.i.i

_ZN17compiler_builtins3int4sdiv8__modsi317hce16a5cedea8ae25E.exit: ; preds = %start, %bb4.i.i.i, %bb9.i.i.i, %bb19.i.i.i
  %.sroa.5.0.i.i.i = phi i32 [ %spec.select.i, %start ], [ %11, %bb4.i.i.i ], [ %_70.i.i.i, %bb19.i.i.i ], [ %spec.select38.i.i.i, %bb9.i.i.i ]
  %44 = sub i32 0, %.sroa.5.0.i.i.i
  %spec.select8.i = select i1 %a_neg.i, i32 %44, i32 %.sroa.5.0.i.i.i
  ret i32 %spec.select8.i
}

; compiler_builtins::int::sdiv::__divmoddi4
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i64 @_ZN17compiler_builtins3int4sdiv11__divmoddi417h188d6fb699b7a37dE(i64 %a, i64 %b, i64* noalias nocapture align 8 dereferenceable(8) %rem) unnamed_addr #6 {
start:
  %a_neg = icmp slt i64 %a, 0
  %b_neg = icmp slt i64 %b, 0
  %0 = sub i64 0, %a
  %spec.select = select i1 %a_neg, i64 %0, i64 %a
  %1 = sub i64 0, %b
  %spec.select13 = select i1 %b_neg, i64 %1, i64 %b
  %2 = icmp ne i64 %b, 0
  tail call void @llvm.assume(i1 %2) #14
  %_5.i.i = icmp ult i64 %spec.select, %spec.select13
  br i1 %_5.i.i, label %_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit, label %bb4.i.i

bb4.i.i:                                          ; preds = %start
  %3 = tail call i64 @llvm.ctlz.i64(i64 %spec.select13, i1 true) #14, !range !6
  %4 = tail call i64 @llvm.ctlz.i64(i64 %spec.select, i1 false) #14, !range !6
  %_4.i.i.i = sub nsw i64 %3, %4
  %5 = and i64 %_4.i.i.i, 4294967295
  %6 = and i64 %_4.i.i.i, 63
  %_12.i.i.i = shl i64 %spec.select13, %6
  %_10.i.i.i = icmp ugt i64 %_12.i.i.i, %spec.select
  %7 = sext i1 %_10.i.i.i to i64
  %shl.0.i.i.i = add nsw i64 %5, %7
  %8 = and i64 %shl.0.i.i.i, 63
  %9 = shl i64 %spec.select13, %8
  %10 = sub i64 %spec.select, %9
  %11 = shl nuw i64 1, %8
  %_21.i.i = icmp ult i64 %10, %spec.select13
  br i1 %_21.i.i, label %_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit, label %bb8.i.i

bb8.i.i:                                          ; preds = %bb4.i.i
  %_27.i.i = icmp slt i64 %9, 0
  br i1 %_27.i.i, label %bb9.i.i, label %bb16.i.i

bb9.i.i:                                          ; preds = %bb8.i.i
  %12 = lshr i64 %9, 1
  %13 = add nsw i64 %shl.0.i.i.i, -1
  %14 = and i64 %13, 63
  %tmp.i.i = shl nuw i64 1, %14
  %15 = sub i64 %10, %12
  %_36.i.i = icmp sgt i64 %15, -1
  %16 = select i1 %_36.i.i, i64 %tmp.i.i, i64 0
  %spec.select.i.i = or i64 %16, %11
  %spec.select38.i.i = select i1 %_36.i.i, i64 %15, i64 %10
  %_41.i.i = icmp ult i64 %spec.select38.i.i, %spec.select13
  br i1 %_41.i.i, label %_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit, label %bb16.i.i

bb16.i.i:                                         ; preds = %bb9.i.i, %bb8.i.i
  %mask.0.in.i.i = phi i64 [ %tmp.i.i, %bb9.i.i ], [ %11, %bb8.i.i ]
  %quo.1.i.i = phi i64 [ %spec.select.i.i, %bb9.i.i ], [ %11, %bb8.i.i ]
  %div2.0.i.i = phi i64 [ %12, %bb9.i.i ], [ %9, %bb8.i.i ]
  %shl.0.i.i = phi i64 [ %13, %bb9.i.i ], [ %shl.0.i.i.i, %bb8.i.i ]
  %duo1.1.i.i = phi i64 [ %spec.select38.i.i, %bb9.i.i ], [ %10, %bb8.i.i ]
  %mask.0.i.i = add i64 %mask.0.in.i.i, -1
  %17 = add nsw i64 %div2.0.i.i, -1
  %18 = icmp eq i64 %shl.0.i.i, 0
  br i1 %18, label %bb19.i.i, label %bb20.i.i.preheader

bb20.i.i.preheader:                               ; preds = %bb16.i.i
  %19 = add nsw i64 %shl.0.i.i, -1
  %xtraiter = and i64 %shl.0.i.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol

bb20.i.i.prol:                                    ; preds = %bb20.i.i.preheader, %bb20.i.i.prol
  %duo1.240.i.i.prol = phi i64 [ %24, %bb20.i.i.prol ], [ %duo1.1.i.i, %bb20.i.i.preheader ]
  %i.039.i.i.prol = phi i64 [ %20, %bb20.i.i.prol ], [ %shl.0.i.i, %bb20.i.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.i.prol ], [ %xtraiter, %bb20.i.i.preheader ]
  %20 = add i64 %i.039.i.i.prol, -1
  %21 = shl i64 %duo1.240.i.i.prol, 1
  %22 = sub i64 %21, %17
  %23 = ashr i64 %22, 63
  %_62.i.i.prol = and i64 %23, %17
  %24 = add i64 %_62.i.i.prol, %22
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol, !llvm.loop !40

bb20.i.i.prol.loopexit:                           ; preds = %bb20.i.i.prol, %bb20.i.i.preheader
  %.lcssa.unr = phi i64 [ undef, %bb20.i.i.preheader ], [ %24, %bb20.i.i.prol ]
  %duo1.240.i.i.unr = phi i64 [ %duo1.1.i.i, %bb20.i.i.preheader ], [ %24, %bb20.i.i.prol ]
  %i.039.i.i.unr = phi i64 [ %shl.0.i.i, %bb20.i.i.preheader ], [ %20, %bb20.i.i.prol ]
  %25 = icmp ult i64 %19, 3
  br i1 %25, label %bb19.i.i, label %bb20.i.i

bb19.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i, %bb16.i.i
  %duo1.2.lcssa.i.i = phi i64 [ %duo1.1.i.i, %bb16.i.i ], [ %.lcssa.unr, %bb20.i.i.prol.loopexit ], [ %43, %bb20.i.i ]
  %_66.i.i = and i64 %duo1.2.lcssa.i.i, %mask.0.i.i
  %_65.i.i = or i64 %_66.i.i, %quo.1.i.i
  %26 = and i64 %shl.0.i.i, 63
  %_70.i.i = lshr i64 %duo1.2.lcssa.i.i, %26
  br label %_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit

bb20.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i
  %duo1.240.i.i = phi i64 [ %43, %bb20.i.i ], [ %duo1.240.i.i.unr, %bb20.i.i.prol.loopexit ]
  %i.039.i.i = phi i64 [ %39, %bb20.i.i ], [ %i.039.i.i.unr, %bb20.i.i.prol.loopexit ]
  %27 = shl i64 %duo1.240.i.i, 1
  %28 = sub i64 %27, %17
  %29 = ashr i64 %28, 63
  %_62.i.i = and i64 %29, %17
  %30 = add i64 %_62.i.i, %28
  %31 = shl i64 %30, 1
  %32 = sub i64 %31, %17
  %33 = ashr i64 %32, 63
  %_62.i.i.1 = and i64 %33, %17
  %34 = add i64 %_62.i.i.1, %32
  %35 = shl i64 %34, 1
  %36 = sub i64 %35, %17
  %37 = ashr i64 %36, 63
  %_62.i.i.2 = and i64 %37, %17
  %38 = add i64 %_62.i.i.2, %36
  %39 = add i64 %i.039.i.i, -4
  %40 = shl i64 %38, 1
  %41 = sub i64 %40, %17
  %42 = ashr i64 %41, 63
  %_62.i.i.3 = and i64 %42, %17
  %43 = add i64 %_62.i.i.3, %41
  %44 = icmp eq i64 %39, 0
  br i1 %44, label %bb19.i.i, label %bb20.i.i

_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit: ; preds = %start, %bb4.i.i, %bb9.i.i, %bb19.i.i
  %.sroa.5.0.i.i = phi i64 [ %spec.select, %start ], [ %10, %bb4.i.i ], [ %_70.i.i, %bb19.i.i ], [ %spec.select38.i.i, %bb9.i.i ]
  %.sroa.0.0.i.i = phi i64 [ 0, %start ], [ %11, %bb4.i.i ], [ %_65.i.i, %bb19.i.i ], [ %spec.select.i.i, %bb9.i.i ]
  br i1 %a_neg, label %bb10, label %_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit.bb10_crit_edge

_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit.bb10_crit_edge: ; preds = %_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit
  store i64 %.sroa.5.0.i.i, i64* %rem, align 8
  br i1 %b_neg, label %bb11, label %bb14

bb10:                                             ; preds = %_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit
  %45 = sub i64 0, %.sroa.5.0.i.i
  store i64 %45, i64* %rem, align 8
  br i1 %b_neg, label %bb14, label %bb11

bb11:                                             ; preds = %_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit.bb10_crit_edge, %bb10
  %46 = sub i64 0, %.sroa.0.0.i.i
  br label %bb14

bb14:                                             ; preds = %_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit.bb10_crit_edge, %bb10, %bb11
  %.0 = phi i64 [ %46, %bb11 ], [ %.sroa.0.0.i.i, %bb10 ], [ %.sroa.0.0.i.i, %_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E.exit.bb10_crit_edge ]
  ret i64 %.0
}

; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i64 @__divmoddi4(i64 %a, i64 %b, i64* noalias nocapture align 8 dereferenceable(8) %rem) unnamed_addr #6 {
start:
; call compiler_builtins::int::sdiv::__divmoddi4
  %0 = tail call i64 @_ZN17compiler_builtins3int4sdiv11__divmoddi417h188d6fb699b7a37dE(i64 %a, i64 %b, i64* noalias nonnull align 8 dereferenceable(8) %rem)
  ret i64 %0
}

; compiler_builtins::int::sdiv::__divdi3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i64 @_ZN17compiler_builtins3int4sdiv8__divdi317he7b7963c951daec7E(i64 %a, i64 %b) unnamed_addr #6 {
start:
  %a_neg = icmp slt i64 %a, 0
  %b_neg = icmp slt i64 %b, 0
  %0 = sub i64 0, %a
  %spec.select = select i1 %a_neg, i64 %0, i64 %a
  %1 = sub i64 0, %b
  %spec.select7 = select i1 %b_neg, i64 %1, i64 %b
  %2 = icmp ne i64 %b, 0
  tail call void @llvm.assume(i1 %2) #14
  %_5.i.i = icmp ult i64 %spec.select, %spec.select7
  br i1 %_5.i.i, label %_ZN17compiler_builtins3int4udiv9__udivdi317hc08bf3f90625fde0E.exit, label %bb4.i.i

bb4.i.i:                                          ; preds = %start
  %3 = tail call i64 @llvm.ctlz.i64(i64 %spec.select7, i1 true) #14, !range !6
  %4 = tail call i64 @llvm.ctlz.i64(i64 %spec.select, i1 false) #14, !range !6
  %_4.i.i.i = sub nsw i64 %3, %4
  %5 = and i64 %_4.i.i.i, 4294967295
  %6 = and i64 %_4.i.i.i, 63
  %_12.i.i.i = shl i64 %spec.select7, %6
  %_10.i.i.i = icmp ugt i64 %_12.i.i.i, %spec.select
  %7 = sext i1 %_10.i.i.i to i64
  %shl.0.i.i.i = add nsw i64 %5, %7
  %8 = and i64 %shl.0.i.i.i, 63
  %9 = shl i64 %spec.select7, %8
  %10 = sub i64 %spec.select, %9
  %11 = shl nuw i64 1, %8
  %_21.i.i = icmp ult i64 %10, %spec.select7
  br i1 %_21.i.i, label %_ZN17compiler_builtins3int4udiv9__udivdi317hc08bf3f90625fde0E.exit, label %bb8.i.i

bb8.i.i:                                          ; preds = %bb4.i.i
  %_27.i.i = icmp slt i64 %9, 0
  br i1 %_27.i.i, label %bb9.i.i, label %bb16.i.i

bb9.i.i:                                          ; preds = %bb8.i.i
  %12 = lshr i64 %9, 1
  %13 = add nsw i64 %shl.0.i.i.i, -1
  %14 = and i64 %13, 63
  %tmp.i.i = shl nuw i64 1, %14
  %15 = sub i64 %10, %12
  %_36.i.i = icmp sgt i64 %15, -1
  %16 = select i1 %_36.i.i, i64 %tmp.i.i, i64 0
  %spec.select.i.i = or i64 %16, %11
  %spec.select38.i.i = select i1 %_36.i.i, i64 %15, i64 %10
  %_41.i.i = icmp ult i64 %spec.select38.i.i, %spec.select7
  br i1 %_41.i.i, label %_ZN17compiler_builtins3int4udiv9__udivdi317hc08bf3f90625fde0E.exit, label %bb16.i.i

bb16.i.i:                                         ; preds = %bb9.i.i, %bb8.i.i
  %mask.0.in.i.i = phi i64 [ %tmp.i.i, %bb9.i.i ], [ %11, %bb8.i.i ]
  %quo.1.i.i = phi i64 [ %spec.select.i.i, %bb9.i.i ], [ %11, %bb8.i.i ]
  %div2.0.i.i = phi i64 [ %12, %bb9.i.i ], [ %9, %bb8.i.i ]
  %shl.0.i.i = phi i64 [ %13, %bb9.i.i ], [ %shl.0.i.i.i, %bb8.i.i ]
  %duo1.1.i.i = phi i64 [ %spec.select38.i.i, %bb9.i.i ], [ %10, %bb8.i.i ]
  %mask.0.i.i = add i64 %mask.0.in.i.i, -1
  %17 = add nsw i64 %div2.0.i.i, -1
  %18 = icmp eq i64 %shl.0.i.i, 0
  br i1 %18, label %bb19.i.i, label %bb20.i.i.preheader

bb20.i.i.preheader:                               ; preds = %bb16.i.i
  %19 = add nsw i64 %shl.0.i.i, -1
  %xtraiter = and i64 %shl.0.i.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol

bb20.i.i.prol:                                    ; preds = %bb20.i.i.preheader, %bb20.i.i.prol
  %duo1.240.i.i.prol = phi i64 [ %24, %bb20.i.i.prol ], [ %duo1.1.i.i, %bb20.i.i.preheader ]
  %i.039.i.i.prol = phi i64 [ %20, %bb20.i.i.prol ], [ %shl.0.i.i, %bb20.i.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.i.prol ], [ %xtraiter, %bb20.i.i.preheader ]
  %20 = add i64 %i.039.i.i.prol, -1
  %21 = shl i64 %duo1.240.i.i.prol, 1
  %22 = sub i64 %21, %17
  %23 = ashr i64 %22, 63
  %_62.i.i.prol = and i64 %23, %17
  %24 = add i64 %_62.i.i.prol, %22
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol, !llvm.loop !41

bb20.i.i.prol.loopexit:                           ; preds = %bb20.i.i.prol, %bb20.i.i.preheader
  %.lcssa.unr = phi i64 [ undef, %bb20.i.i.preheader ], [ %24, %bb20.i.i.prol ]
  %duo1.240.i.i.unr = phi i64 [ %duo1.1.i.i, %bb20.i.i.preheader ], [ %24, %bb20.i.i.prol ]
  %i.039.i.i.unr = phi i64 [ %shl.0.i.i, %bb20.i.i.preheader ], [ %20, %bb20.i.i.prol ]
  %25 = icmp ult i64 %19, 3
  br i1 %25, label %bb19.i.i, label %bb20.i.i

bb19.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i, %bb16.i.i
  %duo1.2.lcssa.i.i = phi i64 [ %duo1.1.i.i, %bb16.i.i ], [ %.lcssa.unr, %bb20.i.i.prol.loopexit ], [ %42, %bb20.i.i ]
  %_66.i.i = and i64 %duo1.2.lcssa.i.i, %mask.0.i.i
  %_65.i.i = or i64 %_66.i.i, %quo.1.i.i
  br label %_ZN17compiler_builtins3int4udiv9__udivdi317hc08bf3f90625fde0E.exit

bb20.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i
  %duo1.240.i.i = phi i64 [ %42, %bb20.i.i ], [ %duo1.240.i.i.unr, %bb20.i.i.prol.loopexit ]
  %i.039.i.i = phi i64 [ %38, %bb20.i.i ], [ %i.039.i.i.unr, %bb20.i.i.prol.loopexit ]
  %26 = shl i64 %duo1.240.i.i, 1
  %27 = sub i64 %26, %17
  %28 = ashr i64 %27, 63
  %_62.i.i = and i64 %28, %17
  %29 = add i64 %_62.i.i, %27
  %30 = shl i64 %29, 1
  %31 = sub i64 %30, %17
  %32 = ashr i64 %31, 63
  %_62.i.i.1 = and i64 %32, %17
  %33 = add i64 %_62.i.i.1, %31
  %34 = shl i64 %33, 1
  %35 = sub i64 %34, %17
  %36 = ashr i64 %35, 63
  %_62.i.i.2 = and i64 %36, %17
  %37 = add i64 %_62.i.i.2, %35
  %38 = add i64 %i.039.i.i, -4
  %39 = shl i64 %37, 1
  %40 = sub i64 %39, %17
  %41 = ashr i64 %40, 63
  %_62.i.i.3 = and i64 %41, %17
  %42 = add i64 %_62.i.i.3, %40
  %43 = icmp eq i64 %38, 0
  br i1 %43, label %bb19.i.i, label %bb20.i.i

_ZN17compiler_builtins3int4udiv9__udivdi317hc08bf3f90625fde0E.exit: ; preds = %start, %bb4.i.i, %bb9.i.i, %bb19.i.i
  %.sroa.0.0.i.i = phi i64 [ 0, %start ], [ %11, %bb4.i.i ], [ %_65.i.i, %bb19.i.i ], [ %spec.select.i.i, %bb9.i.i ]
  %_21 = xor i1 %a_neg, %b_neg
  %44 = sub i64 0, %.sroa.0.0.i.i
  %spec.select8 = select i1 %_21, i64 %44, i64 %.sroa.0.0.i.i
  ret i64 %spec.select8
}

; compiler_builtins::int::sdiv::__moddi3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i64 @_ZN17compiler_builtins3int4sdiv8__moddi317hdee1c7b6ac8975d2E(i64 %a, i64 %b) unnamed_addr #6 {
start:
  %a_neg = icmp slt i64 %a, 0
  %0 = sub i64 0, %a
  %spec.select = select i1 %a_neg, i64 %0, i64 %a
  %1 = tail call i64 @llvm.abs.i64(i64 %b, i1 false)
  %2 = icmp ne i64 %b, 0
  tail call void @llvm.assume(i1 %2) #14
  %_5.i.i = icmp ult i64 %spec.select, %1
  br i1 %_5.i.i, label %_ZN17compiler_builtins3int4udiv9__umoddi317h7ef2251ed05e25a3E.exit, label %bb4.i.i

bb4.i.i:                                          ; preds = %start
  %3 = tail call i64 @llvm.ctlz.i64(i64 %1, i1 true) #14, !range !6
  %4 = tail call i64 @llvm.ctlz.i64(i64 %spec.select, i1 false) #14, !range !6
  %_4.i.i.i = sub nsw i64 %3, %4
  %5 = and i64 %_4.i.i.i, 4294967295
  %6 = and i64 %_4.i.i.i, 63
  %_12.i.i.i = shl i64 %1, %6
  %_10.i.i.i = icmp ugt i64 %_12.i.i.i, %spec.select
  %7 = sext i1 %_10.i.i.i to i64
  %shl.0.i.i.i = add nsw i64 %5, %7
  %8 = and i64 %shl.0.i.i.i, 63
  %9 = shl i64 %1, %8
  %10 = sub i64 %spec.select, %9
  %_21.i.i = icmp ult i64 %10, %1
  br i1 %_21.i.i, label %_ZN17compiler_builtins3int4udiv9__umoddi317h7ef2251ed05e25a3E.exit, label %bb8.i.i

bb8.i.i:                                          ; preds = %bb4.i.i
  %_27.i.i = icmp slt i64 %9, 0
  br i1 %_27.i.i, label %bb9.i.i, label %bb16.i.i

bb9.i.i:                                          ; preds = %bb8.i.i
  %11 = lshr i64 %9, 1
  %12 = add nsw i64 %shl.0.i.i.i, -1
  %13 = sub i64 %10, %11
  %_36.i.i = icmp sgt i64 %13, -1
  %spec.select38.i.i = select i1 %_36.i.i, i64 %13, i64 %10
  %_41.i.i = icmp ult i64 %spec.select38.i.i, %1
  br i1 %_41.i.i, label %_ZN17compiler_builtins3int4udiv9__umoddi317h7ef2251ed05e25a3E.exit, label %bb16.i.i

bb16.i.i:                                         ; preds = %bb9.i.i, %bb8.i.i
  %div2.0.i.i = phi i64 [ %11, %bb9.i.i ], [ %9, %bb8.i.i ]
  %shl.0.i.i = phi i64 [ %12, %bb9.i.i ], [ %shl.0.i.i.i, %bb8.i.i ]
  %duo1.1.i.i = phi i64 [ %spec.select38.i.i, %bb9.i.i ], [ %10, %bb8.i.i ]
  %14 = add nsw i64 %div2.0.i.i, -1
  %15 = icmp eq i64 %shl.0.i.i, 0
  br i1 %15, label %bb19.i.i, label %bb20.i.i.preheader

bb20.i.i.preheader:                               ; preds = %bb16.i.i
  %16 = add nsw i64 %shl.0.i.i, -1
  %xtraiter = and i64 %shl.0.i.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol

bb20.i.i.prol:                                    ; preds = %bb20.i.i.preheader, %bb20.i.i.prol
  %duo1.240.i.i.prol = phi i64 [ %21, %bb20.i.i.prol ], [ %duo1.1.i.i, %bb20.i.i.preheader ]
  %i.039.i.i.prol = phi i64 [ %17, %bb20.i.i.prol ], [ %shl.0.i.i, %bb20.i.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.i.prol ], [ %xtraiter, %bb20.i.i.preheader ]
  %17 = add i64 %i.039.i.i.prol, -1
  %18 = shl i64 %duo1.240.i.i.prol, 1
  %19 = sub i64 %18, %14
  %20 = ashr i64 %19, 63
  %_62.i.i.prol = and i64 %20, %14
  %21 = add i64 %_62.i.i.prol, %19
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.i.prol.loopexit, label %bb20.i.i.prol, !llvm.loop !42

bb20.i.i.prol.loopexit:                           ; preds = %bb20.i.i.prol, %bb20.i.i.preheader
  %.lcssa.unr = phi i64 [ undef, %bb20.i.i.preheader ], [ %21, %bb20.i.i.prol ]
  %duo1.240.i.i.unr = phi i64 [ %duo1.1.i.i, %bb20.i.i.preheader ], [ %21, %bb20.i.i.prol ]
  %i.039.i.i.unr = phi i64 [ %shl.0.i.i, %bb20.i.i.preheader ], [ %17, %bb20.i.i.prol ]
  %22 = icmp ult i64 %16, 3
  br i1 %22, label %bb19.i.i, label %bb20.i.i

bb19.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i, %bb16.i.i
  %duo1.2.lcssa.i.i = phi i64 [ %duo1.1.i.i, %bb16.i.i ], [ %.lcssa.unr, %bb20.i.i.prol.loopexit ], [ %40, %bb20.i.i ]
  %23 = and i64 %shl.0.i.i, 63
  %_70.i.i = lshr i64 %duo1.2.lcssa.i.i, %23
  br label %_ZN17compiler_builtins3int4udiv9__umoddi317h7ef2251ed05e25a3E.exit

bb20.i.i:                                         ; preds = %bb20.i.i.prol.loopexit, %bb20.i.i
  %duo1.240.i.i = phi i64 [ %40, %bb20.i.i ], [ %duo1.240.i.i.unr, %bb20.i.i.prol.loopexit ]
  %i.039.i.i = phi i64 [ %36, %bb20.i.i ], [ %i.039.i.i.unr, %bb20.i.i.prol.loopexit ]
  %24 = shl i64 %duo1.240.i.i, 1
  %25 = sub i64 %24, %14
  %26 = ashr i64 %25, 63
  %_62.i.i = and i64 %26, %14
  %27 = add i64 %_62.i.i, %25
  %28 = shl i64 %27, 1
  %29 = sub i64 %28, %14
  %30 = ashr i64 %29, 63
  %_62.i.i.1 = and i64 %30, %14
  %31 = add i64 %_62.i.i.1, %29
  %32 = shl i64 %31, 1
  %33 = sub i64 %32, %14
  %34 = ashr i64 %33, 63
  %_62.i.i.2 = and i64 %34, %14
  %35 = add i64 %_62.i.i.2, %33
  %36 = add i64 %i.039.i.i, -4
  %37 = shl i64 %35, 1
  %38 = sub i64 %37, %14
  %39 = ashr i64 %38, 63
  %_62.i.i.3 = and i64 %39, %14
  %40 = add i64 %_62.i.i.3, %38
  %41 = icmp eq i64 %36, 0
  br i1 %41, label %bb19.i.i, label %bb20.i.i

_ZN17compiler_builtins3int4udiv9__umoddi317h7ef2251ed05e25a3E.exit: ; preds = %start, %bb4.i.i, %bb9.i.i, %bb19.i.i
  %.sroa.5.0.i.i = phi i64 [ %spec.select, %start ], [ %10, %bb4.i.i ], [ %_70.i.i, %bb19.i.i ], [ %spec.select38.i.i, %bb9.i.i ]
  %42 = sub i64 0, %.sroa.5.0.i.i
  %spec.select8 = select i1 %a_neg, i64 %42, i64 %.sroa.5.0.i.i
  ret i64 %spec.select8
}

; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i64 @__moddi3(i64 %a, i64 %b) unnamed_addr #6 {
start:
  %a_neg.i = icmp slt i64 %a, 0
  %0 = sub i64 0, %a
  %spec.select.i = select i1 %a_neg.i, i64 %0, i64 %a
  %1 = tail call i64 @llvm.abs.i64(i64 %b, i1 false) #14
  %2 = icmp ne i64 %b, 0
  tail call void @llvm.assume(i1 %2) #14
  %_5.i.i.i = icmp ult i64 %spec.select.i, %1
  br i1 %_5.i.i.i, label %_ZN17compiler_builtins3int4sdiv8__moddi317hdee1c7b6ac8975d2E.exit, label %bb4.i.i.i

bb4.i.i.i:                                        ; preds = %start
  %3 = tail call i64 @llvm.ctlz.i64(i64 %1, i1 true) #14, !range !6
  %4 = tail call i64 @llvm.ctlz.i64(i64 %spec.select.i, i1 false) #14, !range !6
  %_4.i.i.i.i = sub nsw i64 %3, %4
  %5 = and i64 %_4.i.i.i.i, 4294967295
  %6 = and i64 %_4.i.i.i.i, 63
  %_12.i.i.i.i = shl i64 %1, %6
  %_10.i.i.i.i = icmp ugt i64 %_12.i.i.i.i, %spec.select.i
  %7 = sext i1 %_10.i.i.i.i to i64
  %shl.0.i.i.i.i = add nsw i64 %5, %7
  %8 = and i64 %shl.0.i.i.i.i, 63
  %9 = shl i64 %1, %8
  %10 = sub i64 %spec.select.i, %9
  %_21.i.i.i = icmp ult i64 %10, %1
  br i1 %_21.i.i.i, label %_ZN17compiler_builtins3int4sdiv8__moddi317hdee1c7b6ac8975d2E.exit, label %bb8.i.i.i

bb8.i.i.i:                                        ; preds = %bb4.i.i.i
  %_27.i.i.i = icmp slt i64 %9, 0
  br i1 %_27.i.i.i, label %bb9.i.i.i, label %bb16.i.i.i

bb9.i.i.i:                                        ; preds = %bb8.i.i.i
  %11 = lshr i64 %9, 1
  %12 = add nsw i64 %shl.0.i.i.i.i, -1
  %13 = sub i64 %10, %11
  %_36.i.i.i = icmp sgt i64 %13, -1
  %spec.select38.i.i.i = select i1 %_36.i.i.i, i64 %13, i64 %10
  %_41.i.i.i = icmp ult i64 %spec.select38.i.i.i, %1
  br i1 %_41.i.i.i, label %_ZN17compiler_builtins3int4sdiv8__moddi317hdee1c7b6ac8975d2E.exit, label %bb16.i.i.i

bb16.i.i.i:                                       ; preds = %bb9.i.i.i, %bb8.i.i.i
  %div2.0.i.i.i = phi i64 [ %11, %bb9.i.i.i ], [ %9, %bb8.i.i.i ]
  %shl.0.i.i.i = phi i64 [ %12, %bb9.i.i.i ], [ %shl.0.i.i.i.i, %bb8.i.i.i ]
  %duo1.1.i.i.i = phi i64 [ %spec.select38.i.i.i, %bb9.i.i.i ], [ %10, %bb8.i.i.i ]
  %14 = add nsw i64 %div2.0.i.i.i, -1
  %15 = icmp eq i64 %shl.0.i.i.i, 0
  br i1 %15, label %bb19.i.i.i, label %bb20.i.i.i.preheader

bb20.i.i.i.preheader:                             ; preds = %bb16.i.i.i
  %16 = add nsw i64 %shl.0.i.i.i, -1
  %xtraiter = and i64 %shl.0.i.i.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.i.i.prol.loopexit, label %bb20.i.i.i.prol

bb20.i.i.i.prol:                                  ; preds = %bb20.i.i.i.preheader, %bb20.i.i.i.prol
  %duo1.240.i.i.i.prol = phi i64 [ %21, %bb20.i.i.i.prol ], [ %duo1.1.i.i.i, %bb20.i.i.i.preheader ]
  %i.039.i.i.i.prol = phi i64 [ %17, %bb20.i.i.i.prol ], [ %shl.0.i.i.i, %bb20.i.i.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.i.i.prol ], [ %xtraiter, %bb20.i.i.i.preheader ]
  %17 = add i64 %i.039.i.i.i.prol, -1
  %18 = shl i64 %duo1.240.i.i.i.prol, 1
  %19 = sub i64 %18, %14
  %20 = ashr i64 %19, 63
  %_62.i.i.i.prol = and i64 %20, %14
  %21 = add i64 %_62.i.i.i.prol, %19
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.i.i.prol.loopexit, label %bb20.i.i.i.prol, !llvm.loop !43

bb20.i.i.i.prol.loopexit:                         ; preds = %bb20.i.i.i.prol, %bb20.i.i.i.preheader
  %.lcssa.unr = phi i64 [ undef, %bb20.i.i.i.preheader ], [ %21, %bb20.i.i.i.prol ]
  %duo1.240.i.i.i.unr = phi i64 [ %duo1.1.i.i.i, %bb20.i.i.i.preheader ], [ %21, %bb20.i.i.i.prol ]
  %i.039.i.i.i.unr = phi i64 [ %shl.0.i.i.i, %bb20.i.i.i.preheader ], [ %17, %bb20.i.i.i.prol ]
  %22 = icmp ult i64 %16, 3
  br i1 %22, label %bb19.i.i.i, label %bb20.i.i.i

bb19.i.i.i:                                       ; preds = %bb20.i.i.i.prol.loopexit, %bb20.i.i.i, %bb16.i.i.i
  %duo1.2.lcssa.i.i.i = phi i64 [ %duo1.1.i.i.i, %bb16.i.i.i ], [ %.lcssa.unr, %bb20.i.i.i.prol.loopexit ], [ %40, %bb20.i.i.i ]
  %23 = and i64 %shl.0.i.i.i, 63
  %_70.i.i.i = lshr i64 %duo1.2.lcssa.i.i.i, %23
  br label %_ZN17compiler_builtins3int4sdiv8__moddi317hdee1c7b6ac8975d2E.exit

bb20.i.i.i:                                       ; preds = %bb20.i.i.i.prol.loopexit, %bb20.i.i.i
  %duo1.240.i.i.i = phi i64 [ %40, %bb20.i.i.i ], [ %duo1.240.i.i.i.unr, %bb20.i.i.i.prol.loopexit ]
  %i.039.i.i.i = phi i64 [ %36, %bb20.i.i.i ], [ %i.039.i.i.i.unr, %bb20.i.i.i.prol.loopexit ]
  %24 = shl i64 %duo1.240.i.i.i, 1
  %25 = sub i64 %24, %14
  %26 = ashr i64 %25, 63
  %_62.i.i.i = and i64 %26, %14
  %27 = add i64 %_62.i.i.i, %25
  %28 = shl i64 %27, 1
  %29 = sub i64 %28, %14
  %30 = ashr i64 %29, 63
  %_62.i.i.i.1 = and i64 %30, %14
  %31 = add i64 %_62.i.i.i.1, %29
  %32 = shl i64 %31, 1
  %33 = sub i64 %32, %14
  %34 = ashr i64 %33, 63
  %_62.i.i.i.2 = and i64 %34, %14
  %35 = add i64 %_62.i.i.i.2, %33
  %36 = add i64 %i.039.i.i.i, -4
  %37 = shl i64 %35, 1
  %38 = sub i64 %37, %14
  %39 = ashr i64 %38, 63
  %_62.i.i.i.3 = and i64 %39, %14
  %40 = add i64 %_62.i.i.i.3, %38
  %41 = icmp eq i64 %36, 0
  br i1 %41, label %bb19.i.i.i, label %bb20.i.i.i

_ZN17compiler_builtins3int4sdiv8__moddi317hdee1c7b6ac8975d2E.exit: ; preds = %start, %bb4.i.i.i, %bb9.i.i.i, %bb19.i.i.i
  %.sroa.5.0.i.i.i = phi i64 [ %spec.select.i, %start ], [ %10, %bb4.i.i.i ], [ %_70.i.i.i, %bb19.i.i.i ], [ %spec.select38.i.i.i, %bb9.i.i.i ]
  %42 = sub i64 0, %.sroa.5.0.i.i.i
  %spec.select8.i = select i1 %a_neg.i, i64 %42, i64 %.sroa.5.0.i.i.i
  ret i64 %spec.select8.i
}

; compiler_builtins::int::sdiv::__divmodti4
; Function Attrs: mustprogress nonlazybind uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int4sdiv11__divmodti417h36c9df28cf7f76ceE(i128 %a, i128 %b, i128* noalias nocapture align 8 dereferenceable(16) %rem) unnamed_addr #7 {
start:
  %a_neg = icmp slt i128 %a, 0
  %b_neg = icmp slt i128 %b, 0
  %0 = sub i128 0, %a
  %spec.select = select i1 %a_neg, i128 %0, i128 %a
  %1 = sub i128 0, %b
  %spec.select13 = select i1 %b_neg, i128 %1, i128 %b
  %_7.i.i = lshr i128 %spec.select, 64
  %duo_hi.i.i = trunc i128 %_7.i.i to i64
  %div_lo.i.i = trunc i128 %spec.select13 to i64
  %_13.i.i = lshr i128 %spec.select13, 64
  %div_hi.i.i = trunc i128 %_13.i.i to i64
  %2 = icmp eq i64 %div_hi.i.i, 0
  br i1 %2, label %bb1.i.i, label %bb9.i.i

bb1.i.i:                                          ; preds = %start
  %3 = icmp ne i64 %div_lo.i.i, 0
  tail call void @llvm.assume(i1 %3)
  %_18.i.i = icmp ult i64 %duo_hi.i.i, %div_lo.i.i
  br i1 %_18.i.i, label %bb4.i.i, label %bb5.i.i

bb9.i.i:                                          ; preds = %start
  %4 = tail call i64 @llvm.ctlz.i64(i64 %div_hi.i.i, i1 true) #14, !range !6
  %5 = trunc i64 %4 to i32
  %div_extra.i.i = sub nuw nsw i32 64, %5
  %6 = zext i32 %div_extra.i.i to i128
  %_59.i.i = lshr i128 %spec.select13, %6
  %div_sig_n.i.i = trunc i128 %_59.i.i to i64
  %_63.i.i = lshr i128 %spec.select, 1
  %duo_lo.i.i.i = trunc i128 %_63.i.i to i64
  %_6.i.i.i = lshr i128 %spec.select, 65
  %duo_hi.i.i.i = trunc i128 %_6.i.i.i to i64
  %7 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_sig_n.i.i, i64 %duo_lo.i.i.i, i64 %duo_hi.i.i.i) #15, !srcloc !44
  %tmp.0.i.i = extractvalue { i64, i64 } %7, 0
  %8 = xor i64 %4, 63
  %9 = lshr i64 %tmp.0.i.i, %8
  %10 = icmp eq i64 %9, 0
  %11 = add i64 %9, -1
  %spec.select.i.i = select i1 %10, i64 0, i64 %11
  %_76.i.i = zext i64 %spec.select.i.i to i128
  %12 = mul i128 %spec.select13, %_76.i.i
  %13 = sub i128 %spec.select, %12
  %_79.not.i.i = icmp ult i128 %13, %spec.select13
  %14 = select i1 %_79.not.i.i, i128 0, i128 %spec.select13
  %rem.0.i.i = sub i128 %13, %14
  %not._79.not.i.i = xor i1 %_79.not.i.i, true
  %15 = zext i1 %not._79.not.i.i to i64
  %quo.1.i.i = add nuw i64 %spec.select.i.i, %15
  %_83.i.i = zext i64 %quo.1.i.i to i128
  br label %_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit

bb5.i.i:                                          ; preds = %bb1.i.i
  %16 = udiv i64 %duo_hi.i.i, %div_lo.i.i
  %17 = urem i64 %duo_hi.i.i, %div_lo.i.i
  %duo_lo.i13.i.i = trunc i128 %spec.select to i64
  %18 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i.i, i64 %duo_lo.i13.i.i, i64 %17) #15, !srcloc !44
  %tmp.01.i.i = extractvalue { i64, i64 } %18, 0
  %tmp.12.i.i = extractvalue { i64, i64 } %18, 1
  %_45.i.i = zext i64 %tmp.01.i.i to i128
  %_48.i.i = zext i64 %16 to i128
  %_47.i.i = shl nuw i128 %_48.i.i, 64
  %_44.i.i = or i128 %_47.i.i, %_45.i.i
  %_51.i.i = zext i64 %tmp.12.i.i to i128
  br label %_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit

bb4.i.i:                                          ; preds = %bb1.i.i
  %duo_lo.i14.i.i = trunc i128 %spec.select to i64
  %19 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i.i, i64 %duo_lo.i14.i.i, i64 %duo_hi.i.i) #15, !srcloc !44
  %_23.0.i.i = extractvalue { i64, i64 } %19, 0
  %_23.1.i.i = extractvalue { i64, i64 } %19, 1
  %_26.i.i = zext i64 %_23.0.i.i to i128
  %_28.i.i = zext i64 %_23.1.i.i to i128
  br label %_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit

_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit: ; preds = %bb9.i.i, %bb5.i.i, %bb4.i.i
  %.sroa.4.0.i.i = phi i128 [ %_28.i.i, %bb4.i.i ], [ %_51.i.i, %bb5.i.i ], [ %rem.0.i.i, %bb9.i.i ]
  %.sroa.0.0.i.i = phi i128 [ %_26.i.i, %bb4.i.i ], [ %_44.i.i, %bb5.i.i ], [ %_83.i.i, %bb9.i.i ]
  br i1 %a_neg, label %bb10, label %_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit.bb10_crit_edge

_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit.bb10_crit_edge: ; preds = %_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit
  store i128 %.sroa.4.0.i.i, i128* %rem, align 8
  br i1 %b_neg, label %bb11, label %bb14

bb10:                                             ; preds = %_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit
  %20 = sub i128 0, %.sroa.4.0.i.i
  store i128 %20, i128* %rem, align 8
  br i1 %b_neg, label %bb14, label %bb11

bb11:                                             ; preds = %_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit.bb10_crit_edge, %bb10
  %21 = sub i128 0, %.sroa.0.0.i.i
  br label %bb14

bb14:                                             ; preds = %_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit.bb10_crit_edge, %bb10, %bb11
  %.0 = phi i128 [ %21, %bb11 ], [ %.sroa.0.0.i.i, %bb10 ], [ %.sroa.0.0.i.i, %_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E.exit.bb10_crit_edge ]
  ret i128 %.0
}

; Function Attrs: mustprogress nonlazybind uwtable willreturn
define hidden i128 @__divmodti4(i128 %a, i128 %b, i128* noalias nocapture align 8 dereferenceable(16) %rem) unnamed_addr #7 {
start:
; call compiler_builtins::int::sdiv::__divmodti4
  %0 = tail call i128 @_ZN17compiler_builtins3int4sdiv11__divmodti417h36c9df28cf7f76ceE(i128 %a, i128 %b, i128* noalias nonnull align 8 dereferenceable(16) %rem)
  ret i128 %0
}

; compiler_builtins::int::sdiv::__divti3
; Function Attrs: mustprogress nonlazybind uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int4sdiv8__divti317h9864093343baf4a4E(i128 %a, i128 %b) unnamed_addr #7 {
start:
  %a_neg = icmp slt i128 %a, 0
  %b_neg = icmp slt i128 %b, 0
  %0 = sub i128 0, %a
  %spec.select = select i1 %a_neg, i128 %0, i128 %a
  %1 = sub i128 0, %b
  %spec.select7 = select i1 %b_neg, i128 %1, i128 %b
  %_7.i.i = lshr i128 %spec.select, 64
  %duo_hi.i.i = trunc i128 %_7.i.i to i64
  %div_lo.i.i = trunc i128 %spec.select7 to i64
  %_13.i.i = lshr i128 %spec.select7, 64
  %div_hi.i.i = trunc i128 %_13.i.i to i64
  %2 = icmp eq i64 %div_hi.i.i, 0
  br i1 %2, label %bb1.i.i, label %bb9.i.i

bb1.i.i:                                          ; preds = %start
  %3 = icmp ne i64 %div_lo.i.i, 0
  tail call void @llvm.assume(i1 %3)
  %_18.i.i = icmp ult i64 %duo_hi.i.i, %div_lo.i.i
  br i1 %_18.i.i, label %bb4.i.i, label %bb5.i.i

bb9.i.i:                                          ; preds = %start
  %4 = tail call i64 @llvm.ctlz.i64(i64 %div_hi.i.i, i1 true) #14, !range !6
  %5 = trunc i64 %4 to i32
  %div_extra.i.i = sub nuw nsw i32 64, %5
  %6 = zext i32 %div_extra.i.i to i128
  %_59.i.i = lshr i128 %spec.select7, %6
  %div_sig_n.i.i = trunc i128 %_59.i.i to i64
  %_63.i.i = lshr i128 %spec.select, 1
  %duo_lo.i.i.i = trunc i128 %_63.i.i to i64
  %_6.i.i.i = lshr i128 %spec.select, 65
  %duo_hi.i.i.i = trunc i128 %_6.i.i.i to i64
  %7 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_sig_n.i.i, i64 %duo_lo.i.i.i, i64 %duo_hi.i.i.i) #15, !srcloc !44
  %tmp.0.i.i = extractvalue { i64, i64 } %7, 0
  %8 = xor i64 %4, 63
  %9 = lshr i64 %tmp.0.i.i, %8
  %10 = icmp eq i64 %9, 0
  %11 = add i64 %9, -1
  %spec.select.i.i = select i1 %10, i64 0, i64 %11
  %_76.i.i = zext i64 %spec.select.i.i to i128
  %12 = mul i128 %spec.select7, %_76.i.i
  %13 = sub i128 %spec.select, %12
  %_79.not.i.i = icmp uge i128 %13, %spec.select7
  %14 = zext i1 %_79.not.i.i to i64
  %quo.1.i.i = add nuw i64 %spec.select.i.i, %14
  %_83.i.i = zext i64 %quo.1.i.i to i128
  br label %_ZN17compiler_builtins3int4udiv9__udivti317ha55a25e1f34ecba7E.exit

bb5.i.i:                                          ; preds = %bb1.i.i
  %15 = udiv i64 %duo_hi.i.i, %div_lo.i.i
  %16 = urem i64 %duo_hi.i.i, %div_lo.i.i
  %duo_lo.i13.i.i = trunc i128 %spec.select to i64
  %17 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i.i, i64 %duo_lo.i13.i.i, i64 %16) #15, !srcloc !44
  %tmp.01.i.i = extractvalue { i64, i64 } %17, 0
  %_45.i.i = zext i64 %tmp.01.i.i to i128
  %_48.i.i = zext i64 %15 to i128
  %_47.i.i = shl nuw i128 %_48.i.i, 64
  %_44.i.i = or i128 %_47.i.i, %_45.i.i
  br label %_ZN17compiler_builtins3int4udiv9__udivti317ha55a25e1f34ecba7E.exit

bb4.i.i:                                          ; preds = %bb1.i.i
  %duo_lo.i14.i.i = trunc i128 %spec.select to i64
  %18 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i.i, i64 %duo_lo.i14.i.i, i64 %duo_hi.i.i) #15, !srcloc !44
  %_23.0.i.i = extractvalue { i64, i64 } %18, 0
  %_26.i.i = zext i64 %_23.0.i.i to i128
  br label %_ZN17compiler_builtins3int4udiv9__udivti317ha55a25e1f34ecba7E.exit

_ZN17compiler_builtins3int4udiv9__udivti317ha55a25e1f34ecba7E.exit: ; preds = %bb9.i.i, %bb5.i.i, %bb4.i.i
  %.sroa.0.0.i.i = phi i128 [ %_26.i.i, %bb4.i.i ], [ %_44.i.i, %bb5.i.i ], [ %_83.i.i, %bb9.i.i ]
  %_21 = xor i1 %a_neg, %b_neg
  %19 = sub i128 0, %.sroa.0.0.i.i
  %spec.select8 = select i1 %_21, i128 %19, i128 %.sroa.0.0.i.i
  ret i128 %spec.select8
}

; compiler_builtins::int::sdiv::__modti3
; Function Attrs: mustprogress nonlazybind uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int4sdiv8__modti317he17e37567c477bb3E(i128 %a, i128 %b) unnamed_addr #7 {
start:
  %a_neg = icmp slt i128 %a, 0
  %0 = sub i128 0, %a
  %spec.select = select i1 %a_neg, i128 %0, i128 %a
  %1 = tail call i128 @llvm.abs.i128(i128 %b, i1 false)
  %_7.i.i = lshr i128 %spec.select, 64
  %duo_hi.i.i = trunc i128 %_7.i.i to i64
  %div_lo.i.i = trunc i128 %1 to i64
  %_13.i.i = lshr i128 %1, 64
  %div_hi.i.i = trunc i128 %_13.i.i to i64
  %2 = icmp eq i64 %div_hi.i.i, 0
  br i1 %2, label %bb1.i.i, label %bb9.i.i

bb1.i.i:                                          ; preds = %start
  %3 = icmp ne i64 %div_lo.i.i, 0
  tail call void @llvm.assume(i1 %3)
  %_18.i.i = icmp ult i64 %duo_hi.i.i, %div_lo.i.i
  br i1 %_18.i.i, label %bb4.i.i, label %bb5.i.i

bb9.i.i:                                          ; preds = %start
  %4 = tail call i64 @llvm.ctlz.i64(i64 %div_hi.i.i, i1 true) #14, !range !6
  %5 = trunc i64 %4 to i32
  %div_extra.i.i = sub nuw nsw i32 64, %5
  %6 = zext i32 %div_extra.i.i to i128
  %_59.i.i = lshr i128 %1, %6
  %div_sig_n.i.i = trunc i128 %_59.i.i to i64
  %_63.i.i = lshr i128 %spec.select, 1
  %duo_lo.i.i.i = trunc i128 %_63.i.i to i64
  %_6.i.i.i = lshr i128 %spec.select, 65
  %duo_hi.i.i.i = trunc i128 %_6.i.i.i to i64
  %7 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_sig_n.i.i, i64 %duo_lo.i.i.i, i64 %duo_hi.i.i.i) #15, !srcloc !44
  %tmp.0.i.i = extractvalue { i64, i64 } %7, 0
  %8 = xor i64 %4, 63
  %9 = lshr i64 %tmp.0.i.i, %8
  %10 = icmp eq i64 %9, 0
  %11 = add i64 %9, -1
  %spec.select.i.i = select i1 %10, i64 0, i64 %11
  %_76.i.i = zext i64 %spec.select.i.i to i128
  %12 = mul i128 %1, %_76.i.i
  %13 = sub i128 %spec.select, %12
  %_79.not.i.i = icmp ult i128 %13, %1
  %14 = select i1 %_79.not.i.i, i128 0, i128 %1
  %rem.0.i.i = sub i128 %13, %14
  br label %_ZN17compiler_builtins3int4udiv9__umodti317hab94b75c62073ef9E.exit

bb5.i.i:                                          ; preds = %bb1.i.i
  %15 = urem i64 %duo_hi.i.i, %div_lo.i.i
  %duo_lo.i13.i.i = trunc i128 %spec.select to i64
  %16 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i.i, i64 %duo_lo.i13.i.i, i64 %15) #15, !srcloc !44
  %tmp.12.i.i = extractvalue { i64, i64 } %16, 1
  %_51.i.i = zext i64 %tmp.12.i.i to i128
  br label %_ZN17compiler_builtins3int4udiv9__umodti317hab94b75c62073ef9E.exit

bb4.i.i:                                          ; preds = %bb1.i.i
  %duo_lo.i14.i.i = trunc i128 %spec.select to i64
  %17 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i.i, i64 %duo_lo.i14.i.i, i64 %duo_hi.i.i) #15, !srcloc !44
  %_23.1.i.i = extractvalue { i64, i64 } %17, 1
  %_28.i.i = zext i64 %_23.1.i.i to i128
  br label %_ZN17compiler_builtins3int4udiv9__umodti317hab94b75c62073ef9E.exit

_ZN17compiler_builtins3int4udiv9__umodti317hab94b75c62073ef9E.exit: ; preds = %bb9.i.i, %bb5.i.i, %bb4.i.i
  %.sroa.4.0.i.i = phi i128 [ %_28.i.i, %bb4.i.i ], [ %_51.i.i, %bb5.i.i ], [ %rem.0.i.i, %bb9.i.i ]
  %18 = sub i128 0, %.sroa.4.0.i.i
  %spec.select8 = select i1 %a_neg, i128 %18, i128 %.sroa.4.0.i.i
  ret i128 %spec.select8
}

; compiler_builtins::int::shift::__ashlsi3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins3int5shift9__ashlsi317hf7e8091f323e46d5E(i32 %a, i32 %b) unnamed_addr #2 {
start:
  %_4.i = and i32 %b, 16
  %0 = icmp eq i32 %_4.i, 0
  br i1 %0, label %bb5.i, label %bb1.i

bb5.i:                                            ; preds = %start
  %1 = icmp eq i32 %b, 0
  br i1 %1, label %_ZN17compiler_builtins3int5shift4Ashl4ashl17h3d4085e1007681a7E.exit, label %bb7.i

bb1.i:                                            ; preds = %start
  %2 = trunc i32 %a to i16
  %3 = trunc i32 %b to i16
  %_4.i.i.i = and i16 %3, 15
  %4 = shl i16 %2, %_4.i.i.i
  %_2.i.i = zext i16 %4 to i32
  %5 = shl nuw i32 %_2.i.i, 16
  br label %_ZN17compiler_builtins3int5shift4Ashl4ashl17h3d4085e1007681a7E.exit

bb7.i:                                            ; preds = %bb5.i
  %6 = trunc i32 %a to i16
  %7 = trunc i32 %b to i16
  %_4.i.i1.i = and i16 %7, 15
  %8 = shl i16 %6, %_4.i.i1.i
  %9 = sub i16 0, %7
  %_4.i.i2.i = and i16 %9, 15
  %10 = lshr i16 %6, %_4.i.i2.i
  %_2.i3.i = lshr i32 %a, 16
  %11 = trunc i32 %_2.i3.i to i16
  %12 = shl i16 %11, %_4.i.i1.i
  %13 = or i16 %12, %10
  %14 = zext i16 %8 to i32
  %_2.i.i.i = zext i16 %13 to i32
  %15 = shl nuw i32 %_2.i.i.i, 16
  %16 = or i32 %15, %14
  br label %_ZN17compiler_builtins3int5shift4Ashl4ashl17h3d4085e1007681a7E.exit

_ZN17compiler_builtins3int5shift4Ashl4ashl17h3d4085e1007681a7E.exit: ; preds = %bb5.i, %bb1.i, %bb7.i
  %.0.i = phi i32 [ %16, %bb7.i ], [ %5, %bb1.i ], [ %a, %bb5.i ]
  ret i32 %.0.i
}

; compiler_builtins::int::shift::__ashldi3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @_ZN17compiler_builtins3int5shift9__ashldi317h49ef5dcd9b8206ceE(i64 %a, i32 %b) unnamed_addr #2 {
start:
  %_4.i = and i32 %b, 32
  %0 = icmp eq i32 %_4.i, 0
  br i1 %0, label %bb5.i, label %bb1.i

bb5.i:                                            ; preds = %start
  %1 = icmp eq i32 %b, 0
  br i1 %1, label %_ZN17compiler_builtins3int5shift4Ashl4ashl17hc43ad5ba4ea0f6cfE.exit, label %bb7.i

bb1.i:                                            ; preds = %start
  %2 = trunc i64 %a to i32
  %_4.i.i.i = and i32 %b, 31
  %3 = shl i32 %2, %_4.i.i.i
  %_2.i.i = zext i32 %3 to i64
  %4 = shl nuw i64 %_2.i.i, 32
  br label %_ZN17compiler_builtins3int5shift4Ashl4ashl17hc43ad5ba4ea0f6cfE.exit

bb7.i:                                            ; preds = %bb5.i
  %5 = trunc i64 %a to i32
  %_4.i.i1.i = and i32 %b, 31
  %6 = shl i32 %5, %_4.i.i1.i
  %_22.i = sub i32 0, %b
  %_4.i.i2.i = and i32 %_22.i, 31
  %7 = lshr i32 %5, %_4.i.i2.i
  %_2.i3.i = lshr i64 %a, 32
  %8 = trunc i64 %_2.i3.i to i32
  %9 = shl i32 %8, %_4.i.i1.i
  %10 = or i32 %9, %7
  %11 = zext i32 %6 to i64
  %_2.i.i.i = zext i32 %10 to i64
  %12 = shl nuw i64 %_2.i.i.i, 32
  %13 = or i64 %12, %11
  br label %_ZN17compiler_builtins3int5shift4Ashl4ashl17hc43ad5ba4ea0f6cfE.exit

_ZN17compiler_builtins3int5shift4Ashl4ashl17hc43ad5ba4ea0f6cfE.exit: ; preds = %bb5.i, %bb1.i, %bb7.i
  %.0.i = phi i64 [ %13, %bb7.i ], [ %4, %bb1.i ], [ %a, %bb5.i ]
  ret i64 %.0.i
}

; compiler_builtins::int::shift::__ashlti3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int5shift9__ashlti317hdb78eb5232153150E(i128 %a, i32 %b) unnamed_addr #2 {
start:
  %_4.i = and i32 %b, 64
  %0 = icmp eq i32 %_4.i, 0
  br i1 %0, label %bb5.i, label %bb1.i

bb5.i:                                            ; preds = %start
  %1 = icmp eq i32 %b, 0
  br i1 %1, label %_ZN17compiler_builtins3int5shift4Ashl4ashl17h60692a27b6428dfdE.exit, label %bb7.i

bb1.i:                                            ; preds = %start
  %2 = trunc i128 %a to i64
  %_5.i.i.i = and i32 %b, 63
  %_4.i.i.i = zext i32 %_5.i.i.i to i64
  %3 = shl i64 %2, %_4.i.i.i
  %_2.i.i = zext i64 %3 to i128
  %4 = shl nuw i128 %_2.i.i, 64
  br label %_ZN17compiler_builtins3int5shift4Ashl4ashl17h60692a27b6428dfdE.exit

bb7.i:                                            ; preds = %bb5.i
  %5 = trunc i128 %a to i64
  %_5.i.i1.i = and i32 %b, 63
  %_4.i.i2.i = zext i32 %_5.i.i1.i to i64
  %6 = shl i64 %5, %_4.i.i2.i
  %_22.i = sub i32 0, %b
  %_5.i.i3.i = and i32 %_22.i, 63
  %_4.i.i4.i = zext i32 %_5.i.i3.i to i64
  %7 = lshr i64 %5, %_4.i.i4.i
  %_2.i5.i = lshr i128 %a, 64
  %8 = trunc i128 %_2.i5.i to i64
  %9 = shl i64 %8, %_4.i.i2.i
  %10 = or i64 %9, %7
  %11 = zext i64 %6 to i128
  %_2.i.i.i = zext i64 %10 to i128
  %12 = shl nuw i128 %_2.i.i.i, 64
  %13 = or i128 %12, %11
  br label %_ZN17compiler_builtins3int5shift4Ashl4ashl17h60692a27b6428dfdE.exit

_ZN17compiler_builtins3int5shift4Ashl4ashl17h60692a27b6428dfdE.exit: ; preds = %bb5.i, %bb1.i, %bb7.i
  %.0.i = phi i128 [ %13, %bb7.i ], [ %4, %bb1.i ], [ %a, %bb5.i ]
  ret i128 %.0.i
}

; compiler_builtins::int::shift::__ashrsi3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins3int5shift9__ashrsi317h06d87a31e097d3c0E(i32 %a, i32 %b) unnamed_addr #2 {
start:
  %_4.i = and i32 %b, 16
  %0 = icmp eq i32 %_4.i, 0
  br i1 %0, label %bb7.i, label %bb1.i

bb7.i:                                            ; preds = %start
  %1 = icmp eq i32 %b, 0
  br i1 %1, label %_ZN17compiler_builtins3int5shift4Ashr4ashr17h2f0ff7f57adfc168E.exit, label %bb9.i

bb1.i:                                            ; preds = %start
  %2 = lshr i32 %a, 16
  %3 = trunc i32 %2 to i16
  %4 = trunc i32 %b to i16
  %_4.i.i.i = and i16 %4, 15
  %5 = ashr i16 %3, %_4.i.i.i
  %6 = ashr i16 %3, 15
  %7 = zext i16 %5 to i32
  %_21.i.i.i = zext i16 %6 to i32
  %8 = shl nuw i32 %_21.i.i.i, 16
  %9 = or i32 %8, %7
  br label %_ZN17compiler_builtins3int5shift4Ashr4ashr17h2f0ff7f57adfc168E.exit

bb9.i:                                            ; preds = %bb7.i
  %10 = trunc i32 %a to i16
  %11 = trunc i32 %b to i16
  %_4.i.i1.i = and i16 %11, 15
  %12 = lshr i16 %10, %_4.i.i1.i
  %13 = lshr i32 %a, 16
  %14 = trunc i32 %13 to i16
  %15 = sub i16 0, %11
  %_4.i.i2.i = and i16 %15, 15
  %16 = shl i16 %14, %_4.i.i2.i
  %17 = or i16 %16, %12
  %18 = ashr i16 %14, %_4.i.i1.i
  %19 = zext i16 %17 to i32
  %_21.i.i4.i = zext i16 %18 to i32
  %20 = shl nuw i32 %_21.i.i4.i, 16
  %21 = or i32 %20, %19
  br label %_ZN17compiler_builtins3int5shift4Ashr4ashr17h2f0ff7f57adfc168E.exit

_ZN17compiler_builtins3int5shift4Ashr4ashr17h2f0ff7f57adfc168E.exit: ; preds = %bb7.i, %bb1.i, %bb9.i
  %.0.i = phi i32 [ %21, %bb9.i ], [ %9, %bb1.i ], [ %a, %bb7.i ]
  ret i32 %.0.i
}

; compiler_builtins::int::shift::__ashrdi3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @_ZN17compiler_builtins3int5shift9__ashrdi317h001d1e9df7d0826bE(i64 %a, i32 %b) unnamed_addr #2 {
start:
  %_4.i = and i32 %b, 32
  %0 = icmp eq i32 %_4.i, 0
  br i1 %0, label %bb7.i, label %bb1.i

bb7.i:                                            ; preds = %start
  %1 = icmp eq i32 %b, 0
  br i1 %1, label %_ZN17compiler_builtins3int5shift4Ashr4ashr17h30719abfc5b64c98E.exit, label %bb9.i

bb1.i:                                            ; preds = %start
  %2 = lshr i64 %a, 32
  %3 = trunc i64 %2 to i32
  %_5.i.i.i = and i32 %b, 31
  %4 = ashr i32 %3, %_5.i.i.i
  %5 = ashr i32 %3, 31
  %6 = zext i32 %4 to i64
  %_21.i.i.i = zext i32 %5 to i64
  %7 = shl nuw i64 %_21.i.i.i, 32
  %8 = or i64 %7, %6
  br label %_ZN17compiler_builtins3int5shift4Ashr4ashr17h30719abfc5b64c98E.exit

bb9.i:                                            ; preds = %bb7.i
  %9 = trunc i64 %a to i32
  %_4.i.i.i = and i32 %b, 31
  %10 = lshr i32 %9, %_4.i.i.i
  %11 = lshr i64 %a, 32
  %12 = trunc i64 %11 to i32
  %_27.i = sub i32 0, %b
  %_5.i.i1.i = and i32 %_27.i, 31
  %13 = shl i32 %12, %_5.i.i1.i
  %14 = or i32 %13, %10
  %15 = ashr i32 %12, %_4.i.i.i
  %16 = zext i32 %14 to i64
  %_21.i.i3.i = zext i32 %15 to i64
  %17 = shl nuw i64 %_21.i.i3.i, 32
  %18 = or i64 %17, %16
  br label %_ZN17compiler_builtins3int5shift4Ashr4ashr17h30719abfc5b64c98E.exit

_ZN17compiler_builtins3int5shift4Ashr4ashr17h30719abfc5b64c98E.exit: ; preds = %bb7.i, %bb1.i, %bb9.i
  %.0.i = phi i64 [ %18, %bb9.i ], [ %8, %bb1.i ], [ %a, %bb7.i ]
  ret i64 %.0.i
}

; compiler_builtins::int::shift::__ashrti3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int5shift9__ashrti317h87ef6c4a04a8fa1dE(i128 %a, i32 %b) unnamed_addr #2 {
start:
  %_4.i = and i32 %b, 64
  %0 = icmp eq i32 %_4.i, 0
  br i1 %0, label %bb7.i, label %bb1.i

bb7.i:                                            ; preds = %start
  %1 = icmp eq i32 %b, 0
  br i1 %1, label %_ZN17compiler_builtins3int5shift4Ashr4ashr17h98dd210843dd5e95E.exit, label %bb9.i

bb1.i:                                            ; preds = %start
  %2 = lshr i128 %a, 64
  %3 = trunc i128 %2 to i64
  %_5.i.i.i = and i32 %b, 63
  %_4.i.i.i = zext i32 %_5.i.i.i to i64
  %4 = ashr i64 %3, %_4.i.i.i
  %5 = ashr i64 %3, 63
  %6 = zext i64 %4 to i128
  %_21.i.i.i = zext i64 %5 to i128
  %7 = shl nuw i128 %_21.i.i.i, 64
  %8 = or i128 %7, %6
  br label %_ZN17compiler_builtins3int5shift4Ashr4ashr17h98dd210843dd5e95E.exit

bb9.i:                                            ; preds = %bb7.i
  %9 = trunc i128 %a to i64
  %_5.i.i1.i = and i32 %b, 63
  %_4.i.i2.i = zext i32 %_5.i.i1.i to i64
  %10 = lshr i64 %9, %_4.i.i2.i
  %11 = lshr i128 %a, 64
  %12 = trunc i128 %11 to i64
  %_27.i = sub i32 0, %b
  %_5.i.i3.i = and i32 %_27.i, 63
  %_4.i.i4.i = zext i32 %_5.i.i3.i to i64
  %13 = shl i64 %12, %_4.i.i4.i
  %14 = or i64 %13, %10
  %15 = ashr i64 %12, %_4.i.i2.i
  %16 = zext i64 %14 to i128
  %_21.i.i7.i = zext i64 %15 to i128
  %17 = shl nuw i128 %_21.i.i7.i, 64
  %18 = or i128 %17, %16
  br label %_ZN17compiler_builtins3int5shift4Ashr4ashr17h98dd210843dd5e95E.exit

_ZN17compiler_builtins3int5shift4Ashr4ashr17h98dd210843dd5e95E.exit: ; preds = %bb7.i, %bb1.i, %bb9.i
  %.0.i = phi i128 [ %18, %bb9.i ], [ %8, %bb1.i ], [ %a, %bb7.i ]
  ret i128 %.0.i
}

; compiler_builtins::int::shift::__lshrsi3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @_ZN17compiler_builtins3int5shift9__lshrsi317h05d06ae4526dab3aE(i32 %a, i32 %b) unnamed_addr #2 {
start:
  %_4.i = and i32 %b, 16
  %0 = icmp eq i32 %_4.i, 0
  br i1 %0, label %bb5.i, label %bb1.i

bb5.i:                                            ; preds = %start
  %1 = icmp eq i32 %b, 0
  br i1 %1, label %_ZN17compiler_builtins3int5shift4Lshr4lshr17he037f6b7b9ebbe65E.exit, label %bb7.i

bb1.i:                                            ; preds = %start
  %_2.i.i = lshr i32 %a, 16
  %2 = trunc i32 %_2.i.i to i16
  %3 = trunc i32 %b to i16
  %_4.i.i.i = and i16 %3, 15
  %4 = lshr i16 %2, %_4.i.i.i
  %5 = zext i16 %4 to i32
  br label %_ZN17compiler_builtins3int5shift4Lshr4lshr17he037f6b7b9ebbe65E.exit

bb7.i:                                            ; preds = %bb5.i
  %6 = trunc i32 %a to i16
  %7 = trunc i32 %b to i16
  %_4.i.i1.i = and i16 %7, 15
  %8 = lshr i16 %6, %_4.i.i1.i
  %_2.i2.i = lshr i32 %a, 16
  %9 = trunc i32 %_2.i2.i to i16
  %10 = sub i16 0, %7
  %_4.i.i3.i = and i16 %10, 15
  %11 = shl i16 %9, %_4.i.i3.i
  %12 = or i16 %11, %8
  %13 = lshr i16 %9, %_4.i.i1.i
  %14 = zext i16 %12 to i32
  %_2.i.i.i = zext i16 %13 to i32
  %15 = shl nuw i32 %_2.i.i.i, 16
  %16 = or i32 %15, %14
  br label %_ZN17compiler_builtins3int5shift4Lshr4lshr17he037f6b7b9ebbe65E.exit

_ZN17compiler_builtins3int5shift4Lshr4lshr17he037f6b7b9ebbe65E.exit: ; preds = %bb5.i, %bb1.i, %bb7.i
  %.0.i = phi i32 [ %16, %bb7.i ], [ %5, %bb1.i ], [ %a, %bb5.i ]
  ret i32 %.0.i
}

; compiler_builtins::int::shift::__lshrdi3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @_ZN17compiler_builtins3int5shift9__lshrdi317h3d3fe01537cf1e08E(i64 %a, i32 %b) unnamed_addr #2 {
start:
  %_4.i = and i32 %b, 32
  %0 = icmp eq i32 %_4.i, 0
  br i1 %0, label %bb5.i, label %bb1.i

bb5.i:                                            ; preds = %start
  %1 = icmp eq i32 %b, 0
  br i1 %1, label %_ZN17compiler_builtins3int5shift4Lshr4lshr17h028002b6cf5ce959E.exit, label %bb7.i

bb1.i:                                            ; preds = %start
  %_2.i.i = lshr i64 %a, 32
  %2 = trunc i64 %_2.i.i to i32
  %_4.i.i.i = and i32 %b, 31
  %3 = lshr i32 %2, %_4.i.i.i
  %4 = zext i32 %3 to i64
  br label %_ZN17compiler_builtins3int5shift4Lshr4lshr17h028002b6cf5ce959E.exit

bb7.i:                                            ; preds = %bb5.i
  %5 = trunc i64 %a to i32
  %_4.i.i1.i = and i32 %b, 31
  %6 = lshr i32 %5, %_4.i.i1.i
  %_2.i2.i = lshr i64 %a, 32
  %7 = trunc i64 %_2.i2.i to i32
  %_22.i = sub i32 0, %b
  %_4.i.i3.i = and i32 %_22.i, 31
  %8 = shl i32 %7, %_4.i.i3.i
  %9 = or i32 %8, %6
  %10 = lshr i32 %7, %_4.i.i1.i
  %11 = zext i32 %9 to i64
  %_2.i.i.i = zext i32 %10 to i64
  %12 = shl nuw i64 %_2.i.i.i, 32
  %13 = or i64 %12, %11
  br label %_ZN17compiler_builtins3int5shift4Lshr4lshr17h028002b6cf5ce959E.exit

_ZN17compiler_builtins3int5shift4Lshr4lshr17h028002b6cf5ce959E.exit: ; preds = %bb5.i, %bb1.i, %bb7.i
  %.0.i = phi i64 [ %13, %bb7.i ], [ %4, %bb1.i ], [ %a, %bb5.i ]
  ret i64 %.0.i
}

; compiler_builtins::int::shift::__lshrti3
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int5shift9__lshrti317h9e5af592047cb74eE(i128 %a, i32 %b) unnamed_addr #2 {
start:
  %_4.i = and i32 %b, 64
  %0 = icmp eq i32 %_4.i, 0
  br i1 %0, label %bb5.i, label %bb1.i

bb5.i:                                            ; preds = %start
  %1 = icmp eq i32 %b, 0
  br i1 %1, label %_ZN17compiler_builtins3int5shift4Lshr4lshr17hb9b429dd33795356E.exit, label %bb7.i

bb1.i:                                            ; preds = %start
  %_2.i.i = lshr i128 %a, 64
  %2 = trunc i128 %_2.i.i to i64
  %_5.i.i.i = and i32 %b, 63
  %_4.i.i.i = zext i32 %_5.i.i.i to i64
  %3 = lshr i64 %2, %_4.i.i.i
  %4 = zext i64 %3 to i128
  br label %_ZN17compiler_builtins3int5shift4Lshr4lshr17hb9b429dd33795356E.exit

bb7.i:                                            ; preds = %bb5.i
  %5 = trunc i128 %a to i64
  %_5.i.i1.i = and i32 %b, 63
  %_4.i.i2.i = zext i32 %_5.i.i1.i to i64
  %6 = lshr i64 %5, %_4.i.i2.i
  %_2.i3.i = lshr i128 %a, 64
  %7 = trunc i128 %_2.i3.i to i64
  %_22.i = sub i32 0, %b
  %_5.i.i4.i = and i32 %_22.i, 63
  %_4.i.i5.i = zext i32 %_5.i.i4.i to i64
  %8 = shl i64 %7, %_4.i.i5.i
  %9 = or i64 %8, %6
  %10 = lshr i64 %7, %_4.i.i2.i
  %11 = zext i64 %9 to i128
  %_2.i.i.i = zext i64 %10 to i128
  %12 = shl nuw i128 %_2.i.i.i, 64
  %13 = or i128 %12, %11
  br label %_ZN17compiler_builtins3int5shift4Lshr4lshr17hb9b429dd33795356E.exit

_ZN17compiler_builtins3int5shift4Lshr4lshr17hb9b429dd33795356E.exit: ; preds = %bb5.i, %bb1.i, %bb7.i
  %.0.i = phi i128 [ %13, %bb7.i ], [ %4, %bb1.i ], [ %a, %bb5.i ]
  ret i128 %.0.i
}

; compiler_builtins::int::udiv::__udivsi3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i32 @_ZN17compiler_builtins3int4udiv9__udivsi317h50584612c9e69bf1E(i32 %n, i32 %d) unnamed_addr #6 {
start:
  %0 = icmp ne i32 %d, 0
  tail call void @llvm.assume(i1 %0) #14
  %_5.i = icmp ult i32 %n, %d
  br i1 %_5.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit, label %bb4.i

bb4.i:                                            ; preds = %start
  %1 = tail call i32 @llvm.ctlz.i32(i32 %d, i1 true) #14, !range !5
  %2 = tail call i32 @llvm.ctlz.i32(i32 %n, i1 false) #14, !range !5
  %_4.i.i = sub nsw i32 %1, %2
  %3 = zext i32 %_4.i.i to i64
  %4 = and i32 %_4.i.i, 31
  %_12.i.i = shl i32 %d, %4
  %_10.i.i = icmp ugt i32 %_12.i.i, %n
  %5 = sext i1 %_10.i.i to i64
  %shl.0.i.i = add nsw i64 %5, %3
  %6 = trunc i64 %shl.0.i.i to i32
  %7 = and i32 %6, 31
  %8 = shl i32 %d, %7
  %9 = sub i32 %n, %8
  %10 = shl nuw i32 1, %7
  %_21.i = icmp ult i32 %9, %d
  br i1 %_21.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit, label %bb8.i

bb8.i:                                            ; preds = %bb4.i
  %_27.i = icmp slt i32 %8, 0
  br i1 %_27.i, label %bb9.i, label %bb16.i

bb9.i:                                            ; preds = %bb8.i
  %11 = lshr i32 %8, 1
  %12 = add nsw i64 %shl.0.i.i, -1
  %13 = trunc i64 %12 to i32
  %14 = and i32 %13, 31
  %tmp.i = shl nuw i32 1, %14
  %15 = sub i32 %9, %11
  %_36.i = icmp sgt i32 %15, -1
  %16 = select i1 %_36.i, i32 %tmp.i, i32 0
  %spec.select.i = or i32 %16, %10
  %spec.select38.i = select i1 %_36.i, i32 %15, i32 %9
  %_41.i = icmp ult i32 %spec.select38.i, %d
  br i1 %_41.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit, label %bb16.i

bb16.i:                                           ; preds = %bb9.i, %bb8.i
  %mask.0.in.i = phi i32 [ %tmp.i, %bb9.i ], [ %10, %bb8.i ]
  %quo.1.i = phi i32 [ %spec.select.i, %bb9.i ], [ %10, %bb8.i ]
  %div2.0.i = phi i32 [ %11, %bb9.i ], [ %8, %bb8.i ]
  %shl.0.i = phi i64 [ %12, %bb9.i ], [ %shl.0.i.i, %bb8.i ]
  %duo1.1.i = phi i32 [ %spec.select38.i, %bb9.i ], [ %9, %bb8.i ]
  %mask.0.i = add i32 %mask.0.in.i, -1
  %17 = add nsw i32 %div2.0.i, -1
  %18 = icmp eq i64 %shl.0.i, 0
  br i1 %18, label %bb19.i, label %bb20.i.preheader

bb20.i.preheader:                                 ; preds = %bb16.i
  %19 = add nsw i64 %shl.0.i, -1
  %xtraiter = and i64 %shl.0.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.prol.loopexit, label %bb20.i.prol

bb20.i.prol:                                      ; preds = %bb20.i.preheader, %bb20.i.prol
  %duo1.240.i.prol = phi i32 [ %24, %bb20.i.prol ], [ %duo1.1.i, %bb20.i.preheader ]
  %i.039.i.prol = phi i64 [ %20, %bb20.i.prol ], [ %shl.0.i, %bb20.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.prol ], [ %xtraiter, %bb20.i.preheader ]
  %20 = add i64 %i.039.i.prol, -1
  %21 = shl i32 %duo1.240.i.prol, 1
  %22 = sub i32 %21, %17
  %23 = ashr i32 %22, 31
  %_62.i.prol = and i32 %23, %17
  %24 = add i32 %_62.i.prol, %22
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.prol.loopexit, label %bb20.i.prol, !llvm.loop !45

bb20.i.prol.loopexit:                             ; preds = %bb20.i.prol, %bb20.i.preheader
  %.lcssa.unr = phi i32 [ undef, %bb20.i.preheader ], [ %24, %bb20.i.prol ]
  %duo1.240.i.unr = phi i32 [ %duo1.1.i, %bb20.i.preheader ], [ %24, %bb20.i.prol ]
  %i.039.i.unr = phi i64 [ %shl.0.i, %bb20.i.preheader ], [ %20, %bb20.i.prol ]
  %25 = icmp ult i64 %19, 3
  br i1 %25, label %bb19.i, label %bb20.i

bb19.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i, %bb16.i
  %duo1.2.lcssa.i = phi i32 [ %duo1.1.i, %bb16.i ], [ %.lcssa.unr, %bb20.i.prol.loopexit ], [ %42, %bb20.i ]
  %_66.i = and i32 %duo1.2.lcssa.i, %mask.0.i
  %_65.i = or i32 %_66.i, %quo.1.i
  br label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit

bb20.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i
  %duo1.240.i = phi i32 [ %42, %bb20.i ], [ %duo1.240.i.unr, %bb20.i.prol.loopexit ]
  %i.039.i = phi i64 [ %38, %bb20.i ], [ %i.039.i.unr, %bb20.i.prol.loopexit ]
  %26 = shl i32 %duo1.240.i, 1
  %27 = sub i32 %26, %17
  %28 = ashr i32 %27, 31
  %_62.i = and i32 %28, %17
  %29 = add i32 %_62.i, %27
  %30 = shl i32 %29, 1
  %31 = sub i32 %30, %17
  %32 = ashr i32 %31, 31
  %_62.i.1 = and i32 %32, %17
  %33 = add i32 %_62.i.1, %31
  %34 = shl i32 %33, 1
  %35 = sub i32 %34, %17
  %36 = ashr i32 %35, 31
  %_62.i.2 = and i32 %36, %17
  %37 = add i32 %_62.i.2, %35
  %38 = add i64 %i.039.i, -4
  %39 = shl i32 %37, 1
  %40 = sub i32 %39, %17
  %41 = ashr i32 %40, 31
  %_62.i.3 = and i32 %41, %17
  %42 = add i32 %_62.i.3, %40
  %43 = icmp eq i64 %38, 0
  br i1 %43, label %bb19.i, label %bb20.i

_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit: ; preds = %start, %bb4.i, %bb9.i, %bb19.i
  %.sroa.0.0.i = phi i32 [ 0, %start ], [ %10, %bb4.i ], [ %_65.i, %bb19.i ], [ %spec.select.i, %bb9.i ]
  ret i32 %.sroa.0.0.i
}

; compiler_builtins::int::udiv::__umodsi3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i32 @_ZN17compiler_builtins3int4udiv9__umodsi317hcf330f4b294364f6E(i32 %n, i32 %d) unnamed_addr #6 {
start:
  %0 = icmp ne i32 %d, 0
  tail call void @llvm.assume(i1 %0) #14
  %_5.i = icmp ult i32 %n, %d
  br i1 %_5.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit, label %bb4.i

bb4.i:                                            ; preds = %start
  %1 = tail call i32 @llvm.ctlz.i32(i32 %d, i1 true) #14, !range !5
  %2 = tail call i32 @llvm.ctlz.i32(i32 %n, i1 false) #14, !range !5
  %_4.i.i = sub nsw i32 %1, %2
  %3 = zext i32 %_4.i.i to i64
  %4 = and i32 %_4.i.i, 31
  %_12.i.i = shl i32 %d, %4
  %_10.i.i = icmp ugt i32 %_12.i.i, %n
  %5 = sext i1 %_10.i.i to i64
  %shl.0.i.i = add nsw i64 %5, %3
  %6 = trunc i64 %shl.0.i.i to i32
  %7 = and i32 %6, 31
  %8 = shl i32 %d, %7
  %9 = sub i32 %n, %8
  %_21.i = icmp ult i32 %9, %d
  br i1 %_21.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit, label %bb8.i

bb8.i:                                            ; preds = %bb4.i
  %_27.i = icmp slt i32 %8, 0
  br i1 %_27.i, label %bb9.i, label %bb16.i

bb9.i:                                            ; preds = %bb8.i
  %10 = lshr i32 %8, 1
  %11 = add nsw i64 %shl.0.i.i, -1
  %12 = sub i32 %9, %10
  %_36.i = icmp sgt i32 %12, -1
  %spec.select38.i = select i1 %_36.i, i32 %12, i32 %9
  %_41.i = icmp ult i32 %spec.select38.i, %d
  br i1 %_41.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit, label %bb16.i

bb16.i:                                           ; preds = %bb9.i, %bb8.i
  %div2.0.i = phi i32 [ %10, %bb9.i ], [ %8, %bb8.i ]
  %shl.0.i = phi i64 [ %11, %bb9.i ], [ %shl.0.i.i, %bb8.i ]
  %duo1.1.i = phi i32 [ %spec.select38.i, %bb9.i ], [ %9, %bb8.i ]
  %13 = add nsw i32 %div2.0.i, -1
  %14 = icmp eq i64 %shl.0.i, 0
  br i1 %14, label %bb19.i, label %bb20.i.preheader

bb20.i.preheader:                                 ; preds = %bb16.i
  %15 = add nsw i64 %shl.0.i, -1
  %xtraiter = and i64 %shl.0.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.prol.loopexit, label %bb20.i.prol

bb20.i.prol:                                      ; preds = %bb20.i.preheader, %bb20.i.prol
  %duo1.240.i.prol = phi i32 [ %20, %bb20.i.prol ], [ %duo1.1.i, %bb20.i.preheader ]
  %i.039.i.prol = phi i64 [ %16, %bb20.i.prol ], [ %shl.0.i, %bb20.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.prol ], [ %xtraiter, %bb20.i.preheader ]
  %16 = add i64 %i.039.i.prol, -1
  %17 = shl i32 %duo1.240.i.prol, 1
  %18 = sub i32 %17, %13
  %19 = ashr i32 %18, 31
  %_62.i.prol = and i32 %19, %13
  %20 = add i32 %_62.i.prol, %18
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.prol.loopexit, label %bb20.i.prol, !llvm.loop !46

bb20.i.prol.loopexit:                             ; preds = %bb20.i.prol, %bb20.i.preheader
  %.lcssa.unr = phi i32 [ undef, %bb20.i.preheader ], [ %20, %bb20.i.prol ]
  %duo1.240.i.unr = phi i32 [ %duo1.1.i, %bb20.i.preheader ], [ %20, %bb20.i.prol ]
  %i.039.i.unr = phi i64 [ %shl.0.i, %bb20.i.preheader ], [ %16, %bb20.i.prol ]
  %21 = icmp ult i64 %15, 3
  br i1 %21, label %bb19.i, label %bb20.i

bb19.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i, %bb16.i
  %duo1.2.lcssa.i = phi i32 [ %duo1.1.i, %bb16.i ], [ %.lcssa.unr, %bb20.i.prol.loopexit ], [ %40, %bb20.i ]
  %22 = trunc i64 %shl.0.i to i32
  %23 = and i32 %22, 31
  %_70.i = lshr i32 %duo1.2.lcssa.i, %23
  br label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit

bb20.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i
  %duo1.240.i = phi i32 [ %40, %bb20.i ], [ %duo1.240.i.unr, %bb20.i.prol.loopexit ]
  %i.039.i = phi i64 [ %36, %bb20.i ], [ %i.039.i.unr, %bb20.i.prol.loopexit ]
  %24 = shl i32 %duo1.240.i, 1
  %25 = sub i32 %24, %13
  %26 = ashr i32 %25, 31
  %_62.i = and i32 %26, %13
  %27 = add i32 %_62.i, %25
  %28 = shl i32 %27, 1
  %29 = sub i32 %28, %13
  %30 = ashr i32 %29, 31
  %_62.i.1 = and i32 %30, %13
  %31 = add i32 %_62.i.1, %29
  %32 = shl i32 %31, 1
  %33 = sub i32 %32, %13
  %34 = ashr i32 %33, 31
  %_62.i.2 = and i32 %34, %13
  %35 = add i32 %_62.i.2, %33
  %36 = add i64 %i.039.i, -4
  %37 = shl i32 %35, 1
  %38 = sub i32 %37, %13
  %39 = ashr i32 %38, 31
  %_62.i.3 = and i32 %39, %13
  %40 = add i32 %_62.i.3, %38
  %41 = icmp eq i64 %36, 0
  br i1 %41, label %bb19.i, label %bb20.i

_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit: ; preds = %start, %bb4.i, %bb9.i, %bb19.i
  %.sroa.5.0.i = phi i32 [ %n, %start ], [ %9, %bb4.i ], [ %_70.i, %bb19.i ], [ %spec.select38.i, %bb9.i ]
  ret i32 %.sroa.5.0.i
}

; compiler_builtins::int::udiv::__udivmodsi4
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i32 @_ZN17compiler_builtins3int4udiv12__udivmodsi417h80b127e2d7d7968fE(i32 %n, i32 %d, i32* noalias nocapture align 4 dereferenceable_or_null(4) %0) unnamed_addr #6 {
start:
  %1 = icmp ne i32 %d, 0
  tail call void @llvm.assume(i1 %1) #14
  %_5.i = icmp ult i32 %n, %d
  br i1 %_5.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit, label %bb4.i

bb4.i:                                            ; preds = %start
  %2 = tail call i32 @llvm.ctlz.i32(i32 %d, i1 true) #14, !range !5
  %3 = tail call i32 @llvm.ctlz.i32(i32 %n, i1 false) #14, !range !5
  %_4.i.i = sub nsw i32 %2, %3
  %4 = zext i32 %_4.i.i to i64
  %5 = and i32 %_4.i.i, 31
  %_12.i.i = shl i32 %d, %5
  %_10.i.i = icmp ugt i32 %_12.i.i, %n
  %6 = sext i1 %_10.i.i to i64
  %shl.0.i.i = add nsw i64 %6, %4
  %7 = trunc i64 %shl.0.i.i to i32
  %8 = and i32 %7, 31
  %9 = shl i32 %d, %8
  %10 = sub i32 %n, %9
  %11 = shl nuw i32 1, %8
  %_21.i = icmp ult i32 %10, %d
  br i1 %_21.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit, label %bb8.i

bb8.i:                                            ; preds = %bb4.i
  %_27.i = icmp slt i32 %9, 0
  br i1 %_27.i, label %bb9.i, label %bb16.i

bb9.i:                                            ; preds = %bb8.i
  %12 = lshr i32 %9, 1
  %13 = add nsw i64 %shl.0.i.i, -1
  %14 = trunc i64 %13 to i32
  %15 = and i32 %14, 31
  %tmp.i = shl nuw i32 1, %15
  %16 = sub i32 %10, %12
  %_36.i = icmp sgt i32 %16, -1
  %17 = select i1 %_36.i, i32 %tmp.i, i32 0
  %spec.select.i = or i32 %17, %11
  %spec.select38.i = select i1 %_36.i, i32 %16, i32 %10
  %_41.i = icmp ult i32 %spec.select38.i, %d
  br i1 %_41.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit, label %bb16.i

bb16.i:                                           ; preds = %bb9.i, %bb8.i
  %mask.0.in.i = phi i32 [ %tmp.i, %bb9.i ], [ %11, %bb8.i ]
  %quo.1.i = phi i32 [ %spec.select.i, %bb9.i ], [ %11, %bb8.i ]
  %div2.0.i = phi i32 [ %12, %bb9.i ], [ %9, %bb8.i ]
  %shl.0.i = phi i64 [ %13, %bb9.i ], [ %shl.0.i.i, %bb8.i ]
  %duo1.1.i = phi i32 [ %spec.select38.i, %bb9.i ], [ %10, %bb8.i ]
  %mask.0.i = add i32 %mask.0.in.i, -1
  %18 = add nsw i32 %div2.0.i, -1
  %19 = icmp eq i64 %shl.0.i, 0
  br i1 %19, label %bb19.i, label %bb20.i.preheader

bb20.i.preheader:                                 ; preds = %bb16.i
  %20 = add nsw i64 %shl.0.i, -1
  %xtraiter = and i64 %shl.0.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.prol.loopexit, label %bb20.i.prol

bb20.i.prol:                                      ; preds = %bb20.i.preheader, %bb20.i.prol
  %duo1.240.i.prol = phi i32 [ %25, %bb20.i.prol ], [ %duo1.1.i, %bb20.i.preheader ]
  %i.039.i.prol = phi i64 [ %21, %bb20.i.prol ], [ %shl.0.i, %bb20.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.prol ], [ %xtraiter, %bb20.i.preheader ]
  %21 = add i64 %i.039.i.prol, -1
  %22 = shl i32 %duo1.240.i.prol, 1
  %23 = sub i32 %22, %18
  %24 = ashr i32 %23, 31
  %_62.i.prol = and i32 %24, %18
  %25 = add i32 %_62.i.prol, %23
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.prol.loopexit, label %bb20.i.prol, !llvm.loop !47

bb20.i.prol.loopexit:                             ; preds = %bb20.i.prol, %bb20.i.preheader
  %.lcssa.unr = phi i32 [ undef, %bb20.i.preheader ], [ %25, %bb20.i.prol ]
  %duo1.240.i.unr = phi i32 [ %duo1.1.i, %bb20.i.preheader ], [ %25, %bb20.i.prol ]
  %i.039.i.unr = phi i64 [ %shl.0.i, %bb20.i.preheader ], [ %21, %bb20.i.prol ]
  %26 = icmp ult i64 %20, 3
  br i1 %26, label %bb19.i, label %bb20.i

bb19.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i, %bb16.i
  %duo1.2.lcssa.i = phi i32 [ %duo1.1.i, %bb16.i ], [ %.lcssa.unr, %bb20.i.prol.loopexit ], [ %45, %bb20.i ]
  %_66.i = and i32 %duo1.2.lcssa.i, %mask.0.i
  %_65.i = or i32 %_66.i, %quo.1.i
  %27 = trunc i64 %shl.0.i to i32
  %28 = and i32 %27, 31
  %_70.i = lshr i32 %duo1.2.lcssa.i, %28
  br label %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit

bb20.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i
  %duo1.240.i = phi i32 [ %45, %bb20.i ], [ %duo1.240.i.unr, %bb20.i.prol.loopexit ]
  %i.039.i = phi i64 [ %41, %bb20.i ], [ %i.039.i.unr, %bb20.i.prol.loopexit ]
  %29 = shl i32 %duo1.240.i, 1
  %30 = sub i32 %29, %18
  %31 = ashr i32 %30, 31
  %_62.i = and i32 %31, %18
  %32 = add i32 %_62.i, %30
  %33 = shl i32 %32, 1
  %34 = sub i32 %33, %18
  %35 = ashr i32 %34, 31
  %_62.i.1 = and i32 %35, %18
  %36 = add i32 %_62.i.1, %34
  %37 = shl i32 %36, 1
  %38 = sub i32 %37, %18
  %39 = ashr i32 %38, 31
  %_62.i.2 = and i32 %39, %18
  %40 = add i32 %_62.i.2, %38
  %41 = add i64 %i.039.i, -4
  %42 = shl i32 %40, 1
  %43 = sub i32 %42, %18
  %44 = ashr i32 %43, 31
  %_62.i.3 = and i32 %44, %18
  %45 = add i32 %_62.i.3, %43
  %46 = icmp eq i64 %41, 0
  br i1 %46, label %bb19.i, label %bb20.i

_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit: ; preds = %start, %bb4.i, %bb9.i, %bb19.i
  %.sroa.5.0.i = phi i32 [ %n, %start ], [ %10, %bb4.i ], [ %_70.i, %bb19.i ], [ %spec.select38.i, %bb9.i ]
  %.sroa.0.0.i = phi i32 [ 0, %start ], [ %11, %bb4.i ], [ %_65.i, %bb19.i ], [ %spec.select.i, %bb9.i ]
  %.not = icmp eq i32* %0, null
  br i1 %.not, label %bb3, label %bb2

bb2:                                              ; preds = %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit
  store i32 %.sroa.5.0.i, i32* %0, align 4
  br label %bb3

bb3:                                              ; preds = %bb2, %_ZN17compiler_builtins3int19specialized_div_rem11u32_div_rem17h8bea4eac7664366bE.exit
  ret i32 %.sroa.0.0.i
}

; compiler_builtins::int::udiv::__udivdi3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i64 @_ZN17compiler_builtins3int4udiv9__udivdi317hc08bf3f90625fde0E(i64 %n, i64 %d) unnamed_addr #6 {
start:
  %0 = icmp ne i64 %d, 0
  tail call void @llvm.assume(i1 %0) #14
  %_5.i = icmp ult i64 %n, %d
  br i1 %_5.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit, label %bb4.i

bb4.i:                                            ; preds = %start
  %1 = tail call i64 @llvm.ctlz.i64(i64 %d, i1 true) #14, !range !6
  %2 = tail call i64 @llvm.ctlz.i64(i64 %n, i1 false) #14, !range !6
  %_4.i.i = sub nsw i64 %1, %2
  %3 = and i64 %_4.i.i, 4294967295
  %4 = and i64 %_4.i.i, 63
  %_12.i.i = shl i64 %d, %4
  %_10.i.i = icmp ugt i64 %_12.i.i, %n
  %5 = sext i1 %_10.i.i to i64
  %shl.0.i.i = add nsw i64 %3, %5
  %6 = and i64 %shl.0.i.i, 63
  %7 = shl i64 %d, %6
  %8 = sub i64 %n, %7
  %9 = shl nuw i64 1, %6
  %_21.i = icmp ult i64 %8, %d
  br i1 %_21.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit, label %bb8.i

bb8.i:                                            ; preds = %bb4.i
  %_27.i = icmp slt i64 %7, 0
  br i1 %_27.i, label %bb9.i, label %bb16.i

bb9.i:                                            ; preds = %bb8.i
  %10 = lshr i64 %7, 1
  %11 = add nsw i64 %shl.0.i.i, -1
  %12 = and i64 %11, 63
  %tmp.i = shl nuw i64 1, %12
  %13 = sub i64 %8, %10
  %_36.i = icmp sgt i64 %13, -1
  %14 = select i1 %_36.i, i64 %tmp.i, i64 0
  %spec.select.i = or i64 %14, %9
  %spec.select38.i = select i1 %_36.i, i64 %13, i64 %8
  %_41.i = icmp ult i64 %spec.select38.i, %d
  br i1 %_41.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit, label %bb16.i

bb16.i:                                           ; preds = %bb9.i, %bb8.i
  %mask.0.in.i = phi i64 [ %tmp.i, %bb9.i ], [ %9, %bb8.i ]
  %quo.1.i = phi i64 [ %spec.select.i, %bb9.i ], [ %9, %bb8.i ]
  %div2.0.i = phi i64 [ %10, %bb9.i ], [ %7, %bb8.i ]
  %shl.0.i = phi i64 [ %11, %bb9.i ], [ %shl.0.i.i, %bb8.i ]
  %duo1.1.i = phi i64 [ %spec.select38.i, %bb9.i ], [ %8, %bb8.i ]
  %mask.0.i = add i64 %mask.0.in.i, -1
  %15 = add nsw i64 %div2.0.i, -1
  %16 = icmp eq i64 %shl.0.i, 0
  br i1 %16, label %bb19.i, label %bb20.i.preheader

bb20.i.preheader:                                 ; preds = %bb16.i
  %17 = add nsw i64 %shl.0.i, -1
  %xtraiter = and i64 %shl.0.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.prol.loopexit, label %bb20.i.prol

bb20.i.prol:                                      ; preds = %bb20.i.preheader, %bb20.i.prol
  %duo1.240.i.prol = phi i64 [ %22, %bb20.i.prol ], [ %duo1.1.i, %bb20.i.preheader ]
  %i.039.i.prol = phi i64 [ %18, %bb20.i.prol ], [ %shl.0.i, %bb20.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.prol ], [ %xtraiter, %bb20.i.preheader ]
  %18 = add i64 %i.039.i.prol, -1
  %19 = shl i64 %duo1.240.i.prol, 1
  %20 = sub i64 %19, %15
  %21 = ashr i64 %20, 63
  %_62.i.prol = and i64 %21, %15
  %22 = add i64 %_62.i.prol, %20
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.prol.loopexit, label %bb20.i.prol, !llvm.loop !48

bb20.i.prol.loopexit:                             ; preds = %bb20.i.prol, %bb20.i.preheader
  %.lcssa.unr = phi i64 [ undef, %bb20.i.preheader ], [ %22, %bb20.i.prol ]
  %duo1.240.i.unr = phi i64 [ %duo1.1.i, %bb20.i.preheader ], [ %22, %bb20.i.prol ]
  %i.039.i.unr = phi i64 [ %shl.0.i, %bb20.i.preheader ], [ %18, %bb20.i.prol ]
  %23 = icmp ult i64 %17, 3
  br i1 %23, label %bb19.i, label %bb20.i

bb19.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i, %bb16.i
  %duo1.2.lcssa.i = phi i64 [ %duo1.1.i, %bb16.i ], [ %.lcssa.unr, %bb20.i.prol.loopexit ], [ %40, %bb20.i ]
  %_66.i = and i64 %duo1.2.lcssa.i, %mask.0.i
  %_65.i = or i64 %_66.i, %quo.1.i
  br label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit

bb20.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i
  %duo1.240.i = phi i64 [ %40, %bb20.i ], [ %duo1.240.i.unr, %bb20.i.prol.loopexit ]
  %i.039.i = phi i64 [ %36, %bb20.i ], [ %i.039.i.unr, %bb20.i.prol.loopexit ]
  %24 = shl i64 %duo1.240.i, 1
  %25 = sub i64 %24, %15
  %26 = ashr i64 %25, 63
  %_62.i = and i64 %26, %15
  %27 = add i64 %_62.i, %25
  %28 = shl i64 %27, 1
  %29 = sub i64 %28, %15
  %30 = ashr i64 %29, 63
  %_62.i.1 = and i64 %30, %15
  %31 = add i64 %_62.i.1, %29
  %32 = shl i64 %31, 1
  %33 = sub i64 %32, %15
  %34 = ashr i64 %33, 63
  %_62.i.2 = and i64 %34, %15
  %35 = add i64 %_62.i.2, %33
  %36 = add i64 %i.039.i, -4
  %37 = shl i64 %35, 1
  %38 = sub i64 %37, %15
  %39 = ashr i64 %38, 63
  %_62.i.3 = and i64 %39, %15
  %40 = add i64 %_62.i.3, %38
  %41 = icmp eq i64 %36, 0
  br i1 %41, label %bb19.i, label %bb20.i

_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit: ; preds = %start, %bb4.i, %bb9.i, %bb19.i
  %.sroa.0.0.i = phi i64 [ 0, %start ], [ %9, %bb4.i ], [ %_65.i, %bb19.i ], [ %spec.select.i, %bb9.i ]
  ret i64 %.sroa.0.0.i
}

; compiler_builtins::int::udiv::__umoddi3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i64 @_ZN17compiler_builtins3int4udiv9__umoddi317h7ef2251ed05e25a3E(i64 %n, i64 %d) unnamed_addr #6 {
start:
  %0 = icmp ne i64 %d, 0
  tail call void @llvm.assume(i1 %0) #14
  %_5.i = icmp ult i64 %n, %d
  br i1 %_5.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit, label %bb4.i

bb4.i:                                            ; preds = %start
  %1 = tail call i64 @llvm.ctlz.i64(i64 %d, i1 true) #14, !range !6
  %2 = tail call i64 @llvm.ctlz.i64(i64 %n, i1 false) #14, !range !6
  %_4.i.i = sub nsw i64 %1, %2
  %3 = and i64 %_4.i.i, 4294967295
  %4 = and i64 %_4.i.i, 63
  %_12.i.i = shl i64 %d, %4
  %_10.i.i = icmp ugt i64 %_12.i.i, %n
  %5 = sext i1 %_10.i.i to i64
  %shl.0.i.i = add nsw i64 %3, %5
  %6 = and i64 %shl.0.i.i, 63
  %7 = shl i64 %d, %6
  %8 = sub i64 %n, %7
  %_21.i = icmp ult i64 %8, %d
  br i1 %_21.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit, label %bb8.i

bb8.i:                                            ; preds = %bb4.i
  %_27.i = icmp slt i64 %7, 0
  br i1 %_27.i, label %bb9.i, label %bb16.i

bb9.i:                                            ; preds = %bb8.i
  %9 = lshr i64 %7, 1
  %10 = add nsw i64 %shl.0.i.i, -1
  %11 = sub i64 %8, %9
  %_36.i = icmp sgt i64 %11, -1
  %spec.select38.i = select i1 %_36.i, i64 %11, i64 %8
  %_41.i = icmp ult i64 %spec.select38.i, %d
  br i1 %_41.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit, label %bb16.i

bb16.i:                                           ; preds = %bb9.i, %bb8.i
  %div2.0.i = phi i64 [ %9, %bb9.i ], [ %7, %bb8.i ]
  %shl.0.i = phi i64 [ %10, %bb9.i ], [ %shl.0.i.i, %bb8.i ]
  %duo1.1.i = phi i64 [ %spec.select38.i, %bb9.i ], [ %8, %bb8.i ]
  %12 = add nsw i64 %div2.0.i, -1
  %13 = icmp eq i64 %shl.0.i, 0
  br i1 %13, label %bb19.i, label %bb20.i.preheader

bb20.i.preheader:                                 ; preds = %bb16.i
  %14 = add nsw i64 %shl.0.i, -1
  %xtraiter = and i64 %shl.0.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.prol.loopexit, label %bb20.i.prol

bb20.i.prol:                                      ; preds = %bb20.i.preheader, %bb20.i.prol
  %duo1.240.i.prol = phi i64 [ %19, %bb20.i.prol ], [ %duo1.1.i, %bb20.i.preheader ]
  %i.039.i.prol = phi i64 [ %15, %bb20.i.prol ], [ %shl.0.i, %bb20.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.prol ], [ %xtraiter, %bb20.i.preheader ]
  %15 = add i64 %i.039.i.prol, -1
  %16 = shl i64 %duo1.240.i.prol, 1
  %17 = sub i64 %16, %12
  %18 = ashr i64 %17, 63
  %_62.i.prol = and i64 %18, %12
  %19 = add i64 %_62.i.prol, %17
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.prol.loopexit, label %bb20.i.prol, !llvm.loop !49

bb20.i.prol.loopexit:                             ; preds = %bb20.i.prol, %bb20.i.preheader
  %.lcssa.unr = phi i64 [ undef, %bb20.i.preheader ], [ %19, %bb20.i.prol ]
  %duo1.240.i.unr = phi i64 [ %duo1.1.i, %bb20.i.preheader ], [ %19, %bb20.i.prol ]
  %i.039.i.unr = phi i64 [ %shl.0.i, %bb20.i.preheader ], [ %15, %bb20.i.prol ]
  %20 = icmp ult i64 %14, 3
  br i1 %20, label %bb19.i, label %bb20.i

bb19.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i, %bb16.i
  %duo1.2.lcssa.i = phi i64 [ %duo1.1.i, %bb16.i ], [ %.lcssa.unr, %bb20.i.prol.loopexit ], [ %38, %bb20.i ]
  %21 = and i64 %shl.0.i, 63
  %_70.i = lshr i64 %duo1.2.lcssa.i, %21
  br label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit

bb20.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i
  %duo1.240.i = phi i64 [ %38, %bb20.i ], [ %duo1.240.i.unr, %bb20.i.prol.loopexit ]
  %i.039.i = phi i64 [ %34, %bb20.i ], [ %i.039.i.unr, %bb20.i.prol.loopexit ]
  %22 = shl i64 %duo1.240.i, 1
  %23 = sub i64 %22, %12
  %24 = ashr i64 %23, 63
  %_62.i = and i64 %24, %12
  %25 = add i64 %_62.i, %23
  %26 = shl i64 %25, 1
  %27 = sub i64 %26, %12
  %28 = ashr i64 %27, 63
  %_62.i.1 = and i64 %28, %12
  %29 = add i64 %_62.i.1, %27
  %30 = shl i64 %29, 1
  %31 = sub i64 %30, %12
  %32 = ashr i64 %31, 63
  %_62.i.2 = and i64 %32, %12
  %33 = add i64 %_62.i.2, %31
  %34 = add i64 %i.039.i, -4
  %35 = shl i64 %33, 1
  %36 = sub i64 %35, %12
  %37 = ashr i64 %36, 63
  %_62.i.3 = and i64 %37, %12
  %38 = add i64 %_62.i.3, %36
  %39 = icmp eq i64 %34, 0
  br i1 %39, label %bb19.i, label %bb20.i

_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit: ; preds = %start, %bb4.i, %bb9.i, %bb19.i
  %.sroa.5.0.i = phi i64 [ %n, %start ], [ %8, %bb4.i ], [ %_70.i, %bb19.i ], [ %spec.select38.i, %bb9.i ]
  ret i64 %.sroa.5.0.i
}

; compiler_builtins::int::udiv::__udivmoddi4
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden i64 @_ZN17compiler_builtins3int4udiv12__udivmoddi417h60d6a1f6147133c8E(i64 %n, i64 %d, i64* noalias nocapture align 8 dereferenceable_or_null(8) %0) unnamed_addr #6 {
start:
  %1 = icmp ne i64 %d, 0
  tail call void @llvm.assume(i1 %1) #14
  %_5.i = icmp ult i64 %n, %d
  br i1 %_5.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit, label %bb4.i

bb4.i:                                            ; preds = %start
  %2 = tail call i64 @llvm.ctlz.i64(i64 %d, i1 true) #14, !range !6
  %3 = tail call i64 @llvm.ctlz.i64(i64 %n, i1 false) #14, !range !6
  %_4.i.i = sub nsw i64 %2, %3
  %4 = and i64 %_4.i.i, 4294967295
  %5 = and i64 %_4.i.i, 63
  %_12.i.i = shl i64 %d, %5
  %_10.i.i = icmp ugt i64 %_12.i.i, %n
  %6 = sext i1 %_10.i.i to i64
  %shl.0.i.i = add nsw i64 %4, %6
  %7 = and i64 %shl.0.i.i, 63
  %8 = shl i64 %d, %7
  %9 = sub i64 %n, %8
  %10 = shl nuw i64 1, %7
  %_21.i = icmp ult i64 %9, %d
  br i1 %_21.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit, label %bb8.i

bb8.i:                                            ; preds = %bb4.i
  %_27.i = icmp slt i64 %8, 0
  br i1 %_27.i, label %bb9.i, label %bb16.i

bb9.i:                                            ; preds = %bb8.i
  %11 = lshr i64 %8, 1
  %12 = add nsw i64 %shl.0.i.i, -1
  %13 = and i64 %12, 63
  %tmp.i = shl nuw i64 1, %13
  %14 = sub i64 %9, %11
  %_36.i = icmp sgt i64 %14, -1
  %15 = select i1 %_36.i, i64 %tmp.i, i64 0
  %spec.select.i = or i64 %15, %10
  %spec.select38.i = select i1 %_36.i, i64 %14, i64 %9
  %_41.i = icmp ult i64 %spec.select38.i, %d
  br i1 %_41.i, label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit, label %bb16.i

bb16.i:                                           ; preds = %bb9.i, %bb8.i
  %mask.0.in.i = phi i64 [ %tmp.i, %bb9.i ], [ %10, %bb8.i ]
  %quo.1.i = phi i64 [ %spec.select.i, %bb9.i ], [ %10, %bb8.i ]
  %div2.0.i = phi i64 [ %11, %bb9.i ], [ %8, %bb8.i ]
  %shl.0.i = phi i64 [ %12, %bb9.i ], [ %shl.0.i.i, %bb8.i ]
  %duo1.1.i = phi i64 [ %spec.select38.i, %bb9.i ], [ %9, %bb8.i ]
  %mask.0.i = add i64 %mask.0.in.i, -1
  %16 = add nsw i64 %div2.0.i, -1
  %17 = icmp eq i64 %shl.0.i, 0
  br i1 %17, label %bb19.i, label %bb20.i.preheader

bb20.i.preheader:                                 ; preds = %bb16.i
  %18 = add nsw i64 %shl.0.i, -1
  %xtraiter = and i64 %shl.0.i, 3
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb20.i.prol.loopexit, label %bb20.i.prol

bb20.i.prol:                                      ; preds = %bb20.i.preheader, %bb20.i.prol
  %duo1.240.i.prol = phi i64 [ %23, %bb20.i.prol ], [ %duo1.1.i, %bb20.i.preheader ]
  %i.039.i.prol = phi i64 [ %19, %bb20.i.prol ], [ %shl.0.i, %bb20.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb20.i.prol ], [ %xtraiter, %bb20.i.preheader ]
  %19 = add i64 %i.039.i.prol, -1
  %20 = shl i64 %duo1.240.i.prol, 1
  %21 = sub i64 %20, %16
  %22 = ashr i64 %21, 63
  %_62.i.prol = and i64 %22, %16
  %23 = add i64 %_62.i.prol, %21
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb20.i.prol.loopexit, label %bb20.i.prol, !llvm.loop !50

bb20.i.prol.loopexit:                             ; preds = %bb20.i.prol, %bb20.i.preheader
  %.lcssa.unr = phi i64 [ undef, %bb20.i.preheader ], [ %23, %bb20.i.prol ]
  %duo1.240.i.unr = phi i64 [ %duo1.1.i, %bb20.i.preheader ], [ %23, %bb20.i.prol ]
  %i.039.i.unr = phi i64 [ %shl.0.i, %bb20.i.preheader ], [ %19, %bb20.i.prol ]
  %24 = icmp ult i64 %18, 3
  br i1 %24, label %bb19.i, label %bb20.i

bb19.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i, %bb16.i
  %duo1.2.lcssa.i = phi i64 [ %duo1.1.i, %bb16.i ], [ %.lcssa.unr, %bb20.i.prol.loopexit ], [ %42, %bb20.i ]
  %_66.i = and i64 %duo1.2.lcssa.i, %mask.0.i
  %_65.i = or i64 %_66.i, %quo.1.i
  %25 = and i64 %shl.0.i, 63
  %_70.i = lshr i64 %duo1.2.lcssa.i, %25
  br label %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit

bb20.i:                                           ; preds = %bb20.i.prol.loopexit, %bb20.i
  %duo1.240.i = phi i64 [ %42, %bb20.i ], [ %duo1.240.i.unr, %bb20.i.prol.loopexit ]
  %i.039.i = phi i64 [ %38, %bb20.i ], [ %i.039.i.unr, %bb20.i.prol.loopexit ]
  %26 = shl i64 %duo1.240.i, 1
  %27 = sub i64 %26, %16
  %28 = ashr i64 %27, 63
  %_62.i = and i64 %28, %16
  %29 = add i64 %_62.i, %27
  %30 = shl i64 %29, 1
  %31 = sub i64 %30, %16
  %32 = ashr i64 %31, 63
  %_62.i.1 = and i64 %32, %16
  %33 = add i64 %_62.i.1, %31
  %34 = shl i64 %33, 1
  %35 = sub i64 %34, %16
  %36 = ashr i64 %35, 63
  %_62.i.2 = and i64 %36, %16
  %37 = add i64 %_62.i.2, %35
  %38 = add i64 %i.039.i, -4
  %39 = shl i64 %37, 1
  %40 = sub i64 %39, %16
  %41 = ashr i64 %40, 63
  %_62.i.3 = and i64 %41, %16
  %42 = add i64 %_62.i.3, %40
  %43 = icmp eq i64 %38, 0
  br i1 %43, label %bb19.i, label %bb20.i

_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit: ; preds = %start, %bb4.i, %bb9.i, %bb19.i
  %.sroa.5.0.i = phi i64 [ %n, %start ], [ %9, %bb4.i ], [ %_70.i, %bb19.i ], [ %spec.select38.i, %bb9.i ]
  %.sroa.0.0.i = phi i64 [ 0, %start ], [ %10, %bb4.i ], [ %_65.i, %bb19.i ], [ %spec.select.i, %bb9.i ]
  %.not = icmp eq i64* %0, null
  br i1 %.not, label %bb3, label %bb2

bb2:                                              ; preds = %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit
  store i64 %.sroa.5.0.i, i64* %0, align 8
  br label %bb3

bb3:                                              ; preds = %bb2, %_ZN17compiler_builtins3int19specialized_div_rem11u64_div_rem17h3c4581056f9a4138E.exit
  ret i64 %.sroa.0.0.i
}

; compiler_builtins::int::udiv::__udivti3
; Function Attrs: mustprogress nonlazybind uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int4udiv9__udivti317ha55a25e1f34ecba7E(i128 %n, i128 %d) unnamed_addr #7 {
start:
  %_7.i = lshr i128 %n, 64
  %duo_hi.i = trunc i128 %_7.i to i64
  %div_lo.i = trunc i128 %d to i64
  %_13.i = lshr i128 %d, 64
  %div_hi.i = trunc i128 %_13.i to i64
  %0 = icmp eq i64 %div_hi.i, 0
  br i1 %0, label %bb1.i, label %bb9.i

bb1.i:                                            ; preds = %start
  %1 = icmp ne i64 %div_lo.i, 0
  tail call void @llvm.assume(i1 %1)
  %_18.i = icmp ult i64 %duo_hi.i, %div_lo.i
  br i1 %_18.i, label %bb4.i, label %bb5.i

bb9.i:                                            ; preds = %start
  %2 = tail call i64 @llvm.ctlz.i64(i64 %div_hi.i, i1 true) #14, !range !6
  %3 = trunc i64 %2 to i32
  %div_extra.i = sub nuw nsw i32 64, %3
  %4 = zext i32 %div_extra.i to i128
  %_59.i = lshr i128 %d, %4
  %div_sig_n.i = trunc i128 %_59.i to i64
  %_63.i = lshr i128 %n, 1
  %duo_lo.i.i = trunc i128 %_63.i to i64
  %_6.i.i = lshr i128 %n, 65
  %duo_hi.i.i = trunc i128 %_6.i.i to i64
  %5 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_sig_n.i, i64 %duo_lo.i.i, i64 %duo_hi.i.i) #15, !srcloc !44
  %tmp.0.i = extractvalue { i64, i64 } %5, 0
  %6 = xor i64 %2, 63
  %7 = lshr i64 %tmp.0.i, %6
  %8 = icmp eq i64 %7, 0
  %9 = add i64 %7, -1
  %spec.select.i = select i1 %8, i64 0, i64 %9
  %_76.i = zext i64 %spec.select.i to i128
  %10 = mul i128 %_76.i, %d
  %11 = sub i128 %n, %10
  %_79.not.i = icmp uge i128 %11, %d
  %12 = zext i1 %_79.not.i to i64
  %quo.1.i = add nuw i64 %spec.select.i, %12
  %_83.i = zext i64 %quo.1.i to i128
  br label %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit

bb5.i:                                            ; preds = %bb1.i
  %13 = udiv i64 %duo_hi.i, %div_lo.i
  %14 = urem i64 %duo_hi.i, %div_lo.i
  %duo_lo.i13.i = trunc i128 %n to i64
  %15 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i, i64 %duo_lo.i13.i, i64 %14) #15, !srcloc !44
  %tmp.01.i = extractvalue { i64, i64 } %15, 0
  %_45.i = zext i64 %tmp.01.i to i128
  %_48.i = zext i64 %13 to i128
  %_47.i = shl nuw i128 %_48.i, 64
  %_44.i = or i128 %_47.i, %_45.i
  br label %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit

bb4.i:                                            ; preds = %bb1.i
  %duo_lo.i14.i = trunc i128 %n to i64
  %16 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i, i64 %duo_lo.i14.i, i64 %duo_hi.i) #15, !srcloc !44
  %_23.0.i = extractvalue { i64, i64 } %16, 0
  %_26.i = zext i64 %_23.0.i to i128
  br label %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit

_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit: ; preds = %bb9.i, %bb5.i, %bb4.i
  %.sroa.0.0.i = phi i128 [ %_26.i, %bb4.i ], [ %_44.i, %bb5.i ], [ %_83.i, %bb9.i ]
  ret i128 %.sroa.0.0.i
}

; compiler_builtins::int::udiv::__umodti3
; Function Attrs: mustprogress nonlazybind uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int4udiv9__umodti317hab94b75c62073ef9E(i128 %n, i128 %d) unnamed_addr #7 {
start:
  %_7.i = lshr i128 %n, 64
  %duo_hi.i = trunc i128 %_7.i to i64
  %div_lo.i = trunc i128 %d to i64
  %_13.i = lshr i128 %d, 64
  %div_hi.i = trunc i128 %_13.i to i64
  %0 = icmp eq i64 %div_hi.i, 0
  br i1 %0, label %bb1.i, label %bb9.i

bb1.i:                                            ; preds = %start
  %1 = icmp ne i64 %div_lo.i, 0
  tail call void @llvm.assume(i1 %1)
  %_18.i = icmp ult i64 %duo_hi.i, %div_lo.i
  br i1 %_18.i, label %bb4.i, label %bb5.i

bb9.i:                                            ; preds = %start
  %2 = tail call i64 @llvm.ctlz.i64(i64 %div_hi.i, i1 true) #14, !range !6
  %3 = trunc i64 %2 to i32
  %div_extra.i = sub nuw nsw i32 64, %3
  %4 = zext i32 %div_extra.i to i128
  %_59.i = lshr i128 %d, %4
  %div_sig_n.i = trunc i128 %_59.i to i64
  %_63.i = lshr i128 %n, 1
  %duo_lo.i.i = trunc i128 %_63.i to i64
  %_6.i.i = lshr i128 %n, 65
  %duo_hi.i.i = trunc i128 %_6.i.i to i64
  %5 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_sig_n.i, i64 %duo_lo.i.i, i64 %duo_hi.i.i) #15, !srcloc !44
  %tmp.0.i = extractvalue { i64, i64 } %5, 0
  %6 = xor i64 %2, 63
  %7 = lshr i64 %tmp.0.i, %6
  %8 = icmp eq i64 %7, 0
  %9 = add i64 %7, -1
  %spec.select.i = select i1 %8, i64 0, i64 %9
  %_76.i = zext i64 %spec.select.i to i128
  %10 = mul i128 %_76.i, %d
  %11 = sub i128 %n, %10
  %_79.not.i = icmp ult i128 %11, %d
  %12 = select i1 %_79.not.i, i128 0, i128 %d
  %rem.0.i = sub i128 %11, %12
  br label %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit

bb5.i:                                            ; preds = %bb1.i
  %13 = urem i64 %duo_hi.i, %div_lo.i
  %duo_lo.i13.i = trunc i128 %n to i64
  %14 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i, i64 %duo_lo.i13.i, i64 %13) #15, !srcloc !44
  %tmp.12.i = extractvalue { i64, i64 } %14, 1
  %_51.i = zext i64 %tmp.12.i to i128
  br label %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit

bb4.i:                                            ; preds = %bb1.i
  %duo_lo.i14.i = trunc i128 %n to i64
  %15 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i, i64 %duo_lo.i14.i, i64 %duo_hi.i) #15, !srcloc !44
  %_23.1.i = extractvalue { i64, i64 } %15, 1
  %_28.i = zext i64 %_23.1.i to i128
  br label %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit

_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit: ; preds = %bb9.i, %bb5.i, %bb4.i
  %.sroa.4.0.i = phi i128 [ %_28.i, %bb4.i ], [ %_51.i, %bb5.i ], [ %rem.0.i, %bb9.i ]
  ret i128 %.sroa.4.0.i
}

; compiler_builtins::int::udiv::__udivmodti4
; Function Attrs: mustprogress nonlazybind uwtable willreturn
define hidden i128 @_ZN17compiler_builtins3int4udiv12__udivmodti417h47bf35181ca40443E(i128 %n, i128 %d, i64* noalias nocapture align 8 dereferenceable_or_null(16) %0) unnamed_addr #7 {
start:
  %_7.i = lshr i128 %n, 64
  %duo_hi.i = trunc i128 %_7.i to i64
  %div_lo.i = trunc i128 %d to i64
  %_13.i = lshr i128 %d, 64
  %div_hi.i = trunc i128 %_13.i to i64
  %1 = icmp eq i64 %div_hi.i, 0
  br i1 %1, label %bb1.i, label %bb9.i

bb1.i:                                            ; preds = %start
  %2 = icmp ne i64 %div_lo.i, 0
  tail call void @llvm.assume(i1 %2)
  %_18.i = icmp ult i64 %duo_hi.i, %div_lo.i
  br i1 %_18.i, label %bb4.i, label %bb5.i

bb9.i:                                            ; preds = %start
  %3 = tail call i64 @llvm.ctlz.i64(i64 %div_hi.i, i1 true) #14, !range !6
  %4 = trunc i64 %3 to i32
  %div_extra.i = sub nuw nsw i32 64, %4
  %5 = zext i32 %div_extra.i to i128
  %_59.i = lshr i128 %d, %5
  %div_sig_n.i = trunc i128 %_59.i to i64
  %_63.i = lshr i128 %n, 1
  %duo_lo.i.i = trunc i128 %_63.i to i64
  %_6.i.i = lshr i128 %n, 65
  %duo_hi.i.i = trunc i128 %_6.i.i to i64
  %6 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_sig_n.i, i64 %duo_lo.i.i, i64 %duo_hi.i.i) #15, !srcloc !44
  %tmp.0.i = extractvalue { i64, i64 } %6, 0
  %7 = xor i64 %3, 63
  %8 = lshr i64 %tmp.0.i, %7
  %9 = icmp eq i64 %8, 0
  %10 = add i64 %8, -1
  %spec.select.i = select i1 %9, i64 0, i64 %10
  %_76.i = zext i64 %spec.select.i to i128
  %11 = mul i128 %_76.i, %d
  %12 = sub i128 %n, %11
  %_79.not.i = icmp ult i128 %12, %d
  %13 = select i1 %_79.not.i, i128 0, i128 %d
  %rem.0.i = sub i128 %12, %13
  %not._79.not.i = xor i1 %_79.not.i, true
  %14 = zext i1 %not._79.not.i to i64
  %quo.1.i = add nuw i64 %spec.select.i, %14
  %_83.i = zext i64 %quo.1.i to i128
  br label %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit

bb5.i:                                            ; preds = %bb1.i
  %15 = udiv i64 %duo_hi.i, %div_lo.i
  %16 = urem i64 %duo_hi.i, %div_lo.i
  %duo_lo.i13.i = trunc i128 %n to i64
  %17 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i, i64 %duo_lo.i13.i, i64 %16) #15, !srcloc !44
  %tmp.01.i = extractvalue { i64, i64 } %17, 0
  %tmp.12.i = extractvalue { i64, i64 } %17, 1
  %_45.i = zext i64 %tmp.01.i to i128
  %_48.i = zext i64 %15 to i128
  %_47.i = shl nuw i128 %_48.i, 64
  %_44.i = or i128 %_47.i, %_45.i
  %_51.i = zext i64 %tmp.12.i to i128
  br label %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit

bb4.i:                                            ; preds = %bb1.i
  %duo_lo.i14.i = trunc i128 %n to i64
  %18 = tail call { i64, i64 } asm "div ${2:q}", "={ax},={dx},r,0,1,~{dirflag},~{fpsr},~{flags}"(i64 %div_lo.i, i64 %duo_lo.i14.i, i64 %duo_hi.i) #15, !srcloc !44
  %_23.0.i = extractvalue { i64, i64 } %18, 0
  %_23.1.i = extractvalue { i64, i64 } %18, 1
  %_26.i = zext i64 %_23.0.i to i128
  %_28.i = zext i64 %_23.1.i to i128
  br label %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit

_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit: ; preds = %bb9.i, %bb5.i, %bb4.i
  %.sroa.4.0.i = phi i128 [ %_28.i, %bb4.i ], [ %_51.i, %bb5.i ], [ %rem.0.i, %bb9.i ]
  %.sroa.0.0.i = phi i128 [ %_26.i, %bb4.i ], [ %_44.i, %bb5.i ], [ %_83.i, %bb9.i ]
  %.not = icmp eq i64* %0, null
  br i1 %.not, label %bb3, label %bb2

bb2:                                              ; preds = %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit
  %19 = bitcast i64* %0 to i128*
  store i128 %.sroa.4.0.i, i128* %19, align 8
  br label %bb3

bb3:                                              ; preds = %bb2, %_ZN17compiler_builtins3int19specialized_div_rem12u128_div_rem17h00df94990be1f2f0E.exit
  ret i128 %.sroa.0.0.i
}

; <u8 as compiler_builtins::int::Int>::abs_diff
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17hc05ba0b8e7949133E"(i8 %self, i8 %other) unnamed_addr #2 {
start:
  %_3 = icmp ult i8 %self, %other
  %0 = sub i8 %other, %self
  %1 = sub i8 %self, %other
  %.0 = select i1 %_3, i8 %0, i8 %1
  ret i8 %.0
}

; <i8 as compiler_builtins::int::Int>::from_unsigned
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h15b7d9f78947b234E"(i8 returned %me) unnamed_addr #2 {
start:
  ret i8 %me
}

; <i8 as compiler_builtins::int::Int>::abs_diff
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17h19e0b1069af6635aE"(i8 %self, i8 %other) unnamed_addr #1 {
start:
  %0 = sub i8 %self, %other
  %1 = tail call i8 @llvm.abs.i8(i8 %0, i1 false) #14
  ret i8 %1
}

; <u8 as compiler_builtins::int::Int>::overflowing_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i8, i8 } @"_ZN50_$LT$u8$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17h06296c8cb21cd0f7E"(i8 %self, i8 %other) unnamed_addr #1 {
start:
  %0 = tail call { i8, i1 } @llvm.uadd.with.overflow.i8(i8 %self, i8 %other) #14
  %1 = extractvalue { i8, i1 } %0, 0
  %2 = extractvalue { i8, i1 } %0, 1
  %3 = zext i1 %2 to i8
  %4 = insertvalue { i8, i8 } undef, i8 %1, 0
  %5 = insertvalue { i8, i8 } %4, i8 %3, 1
  ret { i8, i8 } %5
}

; <i8 as compiler_builtins::int::Int>::from_bool
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17ha461a8f8164d4e5fE"(i1 zeroext %b) unnamed_addr #2 {
start:
  %0 = zext i1 %b to i8
  ret i8 %0
}

; <i8 as compiler_builtins::int::Int>::logical_shr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hd7414b7af3f3076fE"(i8 %self, i32 %other) unnamed_addr #2 {
start:
  %0 = trunc i32 %other to i8
  %_4.i = and i8 %0, 7
  %1 = lshr i8 %self, %_4.i
  ret i8 %1
}

; <i8 as compiler_builtins::int::Int>::is_zero
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h83d2988bb1725b22E"(i8 %self) unnamed_addr #2 {
start:
  %0 = icmp eq i8 %self, 0
  ret i1 %0
}

; <i8 as compiler_builtins::int::Int>::wrapping_neg
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h389ce3b426ebe905E"(i8 %self) unnamed_addr #2 {
start:
  %0 = sub i8 0, %self
  ret i8 %0
}

; <i8 as compiler_builtins::int::Int>::wrapping_add
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h9bc8dab6ebf2d4b8E"(i8 %self, i8 %other) unnamed_addr #2 {
start:
  %0 = add i8 %other, %self
  ret i8 %0
}

; <i8 as compiler_builtins::int::Int>::wrapping_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17hc7084ba9d645fe3dE"(i8 %self, i8 %other) unnamed_addr #2 {
start:
  %0 = mul i8 %other, %self
  ret i8 %0
}

; <i8 as compiler_builtins::int::Int>::wrapping_sub
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h658c67e96fe841faE"(i8 %self, i8 %other) unnamed_addr #2 {
start:
  %0 = sub i8 %self, %other
  ret i8 %0
}

; <i8 as compiler_builtins::int::Int>::wrapping_shl
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17hbf03e0e749dd9523E"(i8 %self, i32 %other) unnamed_addr #2 {
start:
  %0 = trunc i32 %other to i8
  %_4.i = and i8 %0, 7
  %1 = shl i8 %self, %_4.i
  ret i8 %1
}

; <i8 as compiler_builtins::int::Int>::wrapping_shr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17h0d81727c5b62f71bE"(i8 %self, i32 %other) unnamed_addr #2 {
start:
  %0 = trunc i32 %other to i8
  %_4.i = and i8 %0, 7
  %1 = ashr i8 %self, %_4.i
  ret i8 %1
}

; <i8 as compiler_builtins::int::Int>::rotate_left
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h43f0be6c2bed5926E"(i8 %self, i32 %other) unnamed_addr #1 {
start:
  %_4.i.i = trunc i32 %other to i8
  %0 = tail call i8 @llvm.fshl.i8(i8 %self, i8 %self, i8 %_4.i.i) #14
  ret i8 %0
}

; <i8 as compiler_builtins::int::Int>::overflowing_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i8, i8 } @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17h3b29facdb673971aE"(i8 %self, i8 %other) unnamed_addr #1 {
start:
  %0 = tail call { i8, i1 } @llvm.sadd.with.overflow.i8(i8 %self, i8 %other) #14
  %1 = extractvalue { i8, i1 } %0, 0
  %2 = extractvalue { i8, i1 } %0, 1
  %3 = zext i1 %2 to i8
  %4 = insertvalue { i8, i8 } undef, i8 %1, 0
  %5 = insertvalue { i8, i8 } %4, i8 %3, 1
  ret { i8, i8 } %5
}

; <i8 as compiler_builtins::int::Int>::leading_zeros
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN50_$LT$i8$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17hef75fe0301096b7fE"(i8 %self) unnamed_addr #1 {
start:
  %0 = tail call i8 @llvm.ctlz.i8(i8 %self, i1 false) #14, !range !51
  %1 = zext i8 %0 to i32
  ret i32 %1
}

; <u16 as compiler_builtins::int::Int>::abs_diff
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17he9e1fb9dd61a2173E"(i16 %self, i16 %other) unnamed_addr #2 {
start:
  %_3 = icmp ult i16 %self, %other
  %0 = sub i16 %other, %self
  %1 = sub i16 %self, %other
  %.0 = select i1 %_3, i16 %0, i16 %1
  ret i16 %.0
}

; <i16 as compiler_builtins::int::Int>::from_unsigned
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17hf1550914b234f0eeE"(i16 returned %me) unnamed_addr #2 {
start:
  ret i16 %me
}

; <i16 as compiler_builtins::int::Int>::abs_diff
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17h6c806f40babfb99bE"(i16 %self, i16 %other) unnamed_addr #1 {
start:
  %0 = sub i16 %self, %other
  %1 = tail call i16 @llvm.abs.i16(i16 %0, i1 false) #14
  ret i16 %1
}

; <u16 as compiler_builtins::int::Int>::overflowing_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i16, i8 } @"_ZN51_$LT$u16$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17h031667e717796298E"(i16 %self, i16 %other) unnamed_addr #1 {
start:
  %0 = tail call { i16, i1 } @llvm.uadd.with.overflow.i16(i16 %self, i16 %other) #14
  %1 = extractvalue { i16, i1 } %0, 0
  %2 = extractvalue { i16, i1 } %0, 1
  %3 = zext i1 %2 to i8
  %4 = insertvalue { i16, i8 } undef, i16 %1, 0
  %5 = insertvalue { i16, i8 } %4, i8 %3, 1
  ret { i16, i8 } %5
}

; <i16 as compiler_builtins::int::Int>::from_bool
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h3093b7bd05625bafE"(i1 zeroext %b) unnamed_addr #2 {
start:
  %0 = zext i1 %b to i16
  ret i16 %0
}

; <i16 as compiler_builtins::int::Int>::logical_shr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h44c46ce08e7aae8bE"(i16 %self, i32 %other) unnamed_addr #2 {
start:
  %0 = trunc i32 %other to i16
  %_4.i = and i16 %0, 15
  %1 = lshr i16 %self, %_4.i
  ret i16 %1
}

; <i16 as compiler_builtins::int::Int>::is_zero
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h35160746e237e679E"(i16 %self) unnamed_addr #2 {
start:
  %0 = icmp eq i16 %self, 0
  ret i1 %0
}

; <i16 as compiler_builtins::int::Int>::wrapping_neg
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17hfd0b17ad78f498f6E"(i16 %self) unnamed_addr #2 {
start:
  %0 = sub i16 0, %self
  ret i16 %0
}

; <i16 as compiler_builtins::int::Int>::wrapping_add
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17he2b0fbd3e4d74233E"(i16 %self, i16 %other) unnamed_addr #2 {
start:
  %0 = add i16 %other, %self
  ret i16 %0
}

; <i16 as compiler_builtins::int::Int>::wrapping_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17hb7b809c573b727cfE"(i16 %self, i16 %other) unnamed_addr #2 {
start:
  %0 = mul i16 %other, %self
  ret i16 %0
}

; <i16 as compiler_builtins::int::Int>::wrapping_sub
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17hb6fa2e1e6c90a81aE"(i16 %self, i16 %other) unnamed_addr #2 {
start:
  %0 = sub i16 %self, %other
  ret i16 %0
}

; <i16 as compiler_builtins::int::Int>::wrapping_shl
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17h894c2583a47a65c6E"(i16 %self, i32 %other) unnamed_addr #2 {
start:
  %0 = trunc i32 %other to i16
  %_4.i = and i16 %0, 15
  %1 = shl i16 %self, %_4.i
  ret i16 %1
}

; <i16 as compiler_builtins::int::Int>::wrapping_shr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17h2d12ec142e86b447E"(i16 %self, i32 %other) unnamed_addr #2 {
start:
  %0 = trunc i32 %other to i16
  %_4.i = and i16 %0, 15
  %1 = ashr i16 %self, %_4.i
  ret i16 %1
}

; <i16 as compiler_builtins::int::Int>::rotate_left
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h9e7e89dfb69559bfE"(i16 %self, i32 %other) unnamed_addr #1 {
start:
  %_4.i.i = trunc i32 %other to i16
  %0 = tail call i16 @llvm.fshl.i16(i16 %self, i16 %self, i16 %_4.i.i) #14
  ret i16 %0
}

; <i16 as compiler_builtins::int::Int>::overflowing_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i16, i8 } @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17h06bbfd2775f38261E"(i16 %self, i16 %other) unnamed_addr #1 {
start:
  %0 = tail call { i16, i1 } @llvm.sadd.with.overflow.i16(i16 %self, i16 %other) #14
  %1 = extractvalue { i16, i1 } %0, 0
  %2 = extractvalue { i16, i1 } %0, 1
  %3 = zext i1 %2 to i8
  %4 = insertvalue { i16, i8 } undef, i16 %1, 0
  %5 = insertvalue { i16, i8 } %4, i8 %3, 1
  ret { i16, i8 } %5
}

; <i16 as compiler_builtins::int::Int>::leading_zeros
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i16$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h159cdaece7d9d029E"(i16 %self) unnamed_addr #1 {
start:
  %0 = tail call i16 @llvm.ctlz.i16(i16 %self, i1 false) #14, !range !52
  %1 = zext i16 %0 to i32
  ret i32 %1
}

; <u32 as compiler_builtins::int::Int>::abs_diff
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17hbf270a85f2fd01a9E"(i32 %self, i32 %other) unnamed_addr #2 {
start:
  %_3 = icmp ult i32 %self, %other
  %0 = sub i32 %other, %self
  %1 = sub i32 %self, %other
  %.0 = select i1 %_3, i32 %0, i32 %1
  ret i32 %.0
}

; <i32 as compiler_builtins::int::Int>::from_unsigned
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h4ccaa21b4a01268eE"(i32 returned %me) unnamed_addr #2 {
start:
  ret i32 %me
}

; <i32 as compiler_builtins::int::Int>::abs_diff
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17h29d11a1188227733E"(i32 %self, i32 %other) unnamed_addr #1 {
start:
  %0 = sub i32 %self, %other
  %1 = tail call i32 @llvm.abs.i32(i32 %0, i1 false) #14
  ret i32 %1
}

; <u32 as compiler_builtins::int::Int>::overflowing_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i32, i8 } @"_ZN51_$LT$u32$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17h3aa89cabb08d35c9E"(i32 %self, i32 %other) unnamed_addr #1 {
start:
  %0 = tail call { i32, i1 } @llvm.uadd.with.overflow.i32(i32 %self, i32 %other) #14
  %1 = extractvalue { i32, i1 } %0, 0
  %2 = extractvalue { i32, i1 } %0, 1
  %3 = zext i1 %2 to i8
  %4 = insertvalue { i32, i8 } undef, i32 %1, 0
  %5 = insertvalue { i32, i8 } %4, i8 %3, 1
  ret { i32, i8 } %5
}

; <i32 as compiler_builtins::int::Int>::from_bool
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17hff506f32b556ab97E"(i1 zeroext %b) unnamed_addr #2 {
start:
  %0 = zext i1 %b to i32
  ret i32 %0
}

; <i32 as compiler_builtins::int::Int>::logical_shr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h2d9cda3a5d0bb116E"(i32 %self, i32 %other) unnamed_addr #2 {
start:
  %_4.i = and i32 %other, 31
  %0 = lshr i32 %self, %_4.i
  ret i32 %0
}

; <i32 as compiler_builtins::int::Int>::is_zero
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h721ff5c67e32c6f7E"(i32 %self) unnamed_addr #2 {
start:
  %0 = icmp eq i32 %self, 0
  ret i1 %0
}

; <i32 as compiler_builtins::int::Int>::wrapping_neg
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17hb35a7240cb2a1241E"(i32 %self) unnamed_addr #2 {
start:
  %0 = sub i32 0, %self
  ret i32 %0
}

; <i32 as compiler_builtins::int::Int>::wrapping_add
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h1deec60c34110673E"(i32 %self, i32 %other) unnamed_addr #2 {
start:
  %0 = add i32 %other, %self
  ret i32 %0
}

; <i32 as compiler_builtins::int::Int>::wrapping_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17h050221ccec7c8c3dE"(i32 %self, i32 %other) unnamed_addr #2 {
start:
  %0 = mul i32 %other, %self
  ret i32 %0
}

; <i32 as compiler_builtins::int::Int>::wrapping_sub
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17hf1df666bbff9d8b7E"(i32 %self, i32 %other) unnamed_addr #2 {
start:
  %0 = sub i32 %self, %other
  ret i32 %0
}

; <i32 as compiler_builtins::int::Int>::wrapping_shl
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17h363554f4d11e168bE"(i32 %self, i32 %other) unnamed_addr #2 {
start:
  %_5.i = and i32 %other, 31
  %0 = shl i32 %self, %_5.i
  ret i32 %0
}

; <i32 as compiler_builtins::int::Int>::wrapping_shr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17h9808e3886dc2ed7cE"(i32 %self, i32 %other) unnamed_addr #2 {
start:
  %_5.i = and i32 %other, 31
  %0 = ashr i32 %self, %_5.i
  ret i32 %0
}

; <i32 as compiler_builtins::int::Int>::rotate_left
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h304fa0338895d35cE"(i32 %self, i32 %other) unnamed_addr #1 {
start:
  %0 = tail call i32 @llvm.fshl.i32(i32 %self, i32 %self, i32 %other) #14
  ret i32 %0
}

; <i32 as compiler_builtins::int::Int>::overflowing_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i32, i8 } @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17hf1638f48b55b9817E"(i32 %self, i32 %other) unnamed_addr #1 {
start:
  %0 = tail call { i32, i1 } @llvm.sadd.with.overflow.i32(i32 %self, i32 %other) #14
  %1 = extractvalue { i32, i1 } %0, 0
  %2 = extractvalue { i32, i1 } %0, 1
  %3 = zext i1 %2 to i8
  %4 = insertvalue { i32, i8 } undef, i32 %1, 0
  %5 = insertvalue { i32, i8 } %4, i8 %3, 1
  ret { i32, i8 } %5
}

; <i32 as compiler_builtins::int::Int>::leading_zeros
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i32$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17hf4e50181e271378eE"(i32 %self) unnamed_addr #1 {
start:
  %0 = tail call i32 @llvm.ctlz.i32(i32 %self, i1 false) #14, !range !5
  ret i32 %0
}

; <u64 as compiler_builtins::int::Int>::abs_diff
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17hcc8f7e4d9054e984E"(i64 %self, i64 %other) unnamed_addr #2 {
start:
  %_3 = icmp ult i64 %self, %other
  %0 = sub i64 %other, %self
  %1 = sub i64 %self, %other
  %.0 = select i1 %_3, i64 %0, i64 %1
  ret i64 %.0
}

; <i64 as compiler_builtins::int::Int>::from_unsigned
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h1c42220f70a2fd85E"(i64 returned %me) unnamed_addr #2 {
start:
  ret i64 %me
}

; <i64 as compiler_builtins::int::Int>::abs_diff
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17h7666acffbb2a576aE"(i64 %self, i64 %other) unnamed_addr #1 {
start:
  %0 = sub i64 %self, %other
  %1 = tail call i64 @llvm.abs.i64(i64 %0, i1 false) #14
  ret i64 %1
}

; <u64 as compiler_builtins::int::Int>::overflowing_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i64, i8 } @"_ZN51_$LT$u64$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17h0b6adedcabc3950bE"(i64 %self, i64 %other) unnamed_addr #1 {
start:
  %0 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %self, i64 %other) #14
  %1 = extractvalue { i64, i1 } %0, 0
  %2 = extractvalue { i64, i1 } %0, 1
  %3 = zext i1 %2 to i8
  %4 = insertvalue { i64, i8 } undef, i64 %1, 0
  %5 = insertvalue { i64, i8 } %4, i8 %3, 1
  ret { i64, i8 } %5
}

; <i64 as compiler_builtins::int::Int>::from_bool
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h91b89361e4f85204E"(i1 zeroext %b) unnamed_addr #2 {
start:
  %0 = zext i1 %b to i64
  ret i64 %0
}

; <i64 as compiler_builtins::int::Int>::logical_shr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17hbca044964faa5ae0E"(i64 %self, i32 %other) unnamed_addr #2 {
start:
  %_5.i = and i32 %other, 63
  %_4.i = zext i32 %_5.i to i64
  %0 = lshr i64 %self, %_4.i
  ret i64 %0
}

; <i64 as compiler_builtins::int::Int>::is_zero
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h432204bf1c46b739E"(i64 %self) unnamed_addr #2 {
start:
  %0 = icmp eq i64 %self, 0
  ret i1 %0
}

; <i64 as compiler_builtins::int::Int>::wrapping_neg
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h424be8bc3d4f5a33E"(i64 %self) unnamed_addr #2 {
start:
  %0 = sub i64 0, %self
  ret i64 %0
}

; <i64 as compiler_builtins::int::Int>::wrapping_add
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17hf21f0fa7ad9607f5E"(i64 %self, i64 %other) unnamed_addr #2 {
start:
  %0 = add i64 %other, %self
  ret i64 %0
}

; <i64 as compiler_builtins::int::Int>::wrapping_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17hab26316137090cd4E"(i64 %self, i64 %other) unnamed_addr #2 {
start:
  %0 = mul i64 %other, %self
  ret i64 %0
}

; <i64 as compiler_builtins::int::Int>::wrapping_sub
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h6dd5f14560f65fe3E"(i64 %self, i64 %other) unnamed_addr #2 {
start:
  %0 = sub i64 %self, %other
  ret i64 %0
}

; <i64 as compiler_builtins::int::Int>::wrapping_shl
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17he57e36ff7eb8f488E"(i64 %self, i32 %other) unnamed_addr #2 {
start:
  %_5.i = and i32 %other, 63
  %_4.i = zext i32 %_5.i to i64
  %0 = shl i64 %self, %_4.i
  ret i64 %0
}

; <i64 as compiler_builtins::int::Int>::wrapping_shr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17he346b9962a61cb6dE"(i64 %self, i32 %other) unnamed_addr #2 {
start:
  %_5.i = and i32 %other, 63
  %_4.i = zext i32 %_5.i to i64
  %0 = ashr i64 %self, %_4.i
  ret i64 %0
}

; <i64 as compiler_builtins::int::Int>::rotate_left
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17h6b61fc2dad9437cdE"(i64 %self, i32 %other) unnamed_addr #1 {
start:
  %_4.i.i = zext i32 %other to i64
  %0 = tail call i64 @llvm.fshl.i64(i64 %self, i64 %self, i64 %_4.i.i) #14
  ret i64 %0
}

; <i64 as compiler_builtins::int::Int>::overflowing_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i64, i8 } @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17hbd8f5f993a8952eeE"(i64 %self, i64 %other) unnamed_addr #1 {
start:
  %0 = tail call { i64, i1 } @llvm.sadd.with.overflow.i64(i64 %self, i64 %other) #14
  %1 = extractvalue { i64, i1 } %0, 0
  %2 = extractvalue { i64, i1 } %0, 1
  %3 = zext i1 %2 to i8
  %4 = insertvalue { i64, i8 } undef, i64 %1, 0
  %5 = insertvalue { i64, i8 } %4, i8 %3, 1
  ret { i64, i8 } %5
}

; <i64 as compiler_builtins::int::Int>::leading_zeros
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN51_$LT$i64$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17h71ab96f3f7dd29deE"(i64 %self) unnamed_addr #1 {
start:
  %0 = tail call i64 @llvm.ctlz.i64(i64 %self, i1 false) #14, !range !6
  %1 = trunc i64 %0 to i32
  ret i32 %1
}

; <u128 as compiler_builtins::int::Int>::abs_diff
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17hae5a03afece7618bE"(i128 %self, i128 %other) unnamed_addr #2 {
start:
  %_3 = icmp ult i128 %self, %other
  %0 = sub i128 %other, %self
  %1 = sub i128 %self, %other
  %.0 = select i1 %_3, i128 %0, i128 %1
  ret i128 %.0
}

; <i128 as compiler_builtins::int::Int>::from_unsigned
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$13from_unsigned17h609d4be81cbeae6cE"(i128 returned %me) unnamed_addr #2 {
start:
  ret i128 %me
}

; <i128 as compiler_builtins::int::Int>::abs_diff
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$8abs_diff17hbe4eeac58047ad78E"(i128 %self, i128 %other) unnamed_addr #1 {
start:
  %0 = sub i128 %self, %other
  %1 = tail call i128 @llvm.abs.i128(i128 %0, i1 false) #14
  ret i128 %1
}

; <u128 as compiler_builtins::int::Int>::overflowing_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i128, i8 } @"_ZN52_$LT$u128$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17h38086d0efbb7bd2aE"(i128 %self, i128 %other) unnamed_addr #1 {
start:
  %0 = tail call { i128, i1 } @llvm.uadd.with.overflow.i128(i128 %self, i128 %other) #14
  %1 = extractvalue { i128, i1 } %0, 0
  %2 = extractvalue { i128, i1 } %0, 1
  %3 = zext i1 %2 to i8
  %4 = insertvalue { i128, i8 } undef, i128 %1, 0
  %5 = insertvalue { i128, i8 } %4, i8 %3, 1
  ret { i128, i8 } %5
}

; <i128 as compiler_builtins::int::Int>::from_bool
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$9from_bool17h0ea615cb30d55a7bE"(i1 zeroext %b) unnamed_addr #2 {
start:
  %0 = zext i1 %b to i128
  ret i128 %0
}

; <i128 as compiler_builtins::int::Int>::logical_shr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$11logical_shr17h86ad79a0618b7570E"(i128 %self, i32 %other) unnamed_addr #2 {
start:
  %_5.i = and i32 %other, 127
  %_4.i = zext i32 %_5.i to i128
  %0 = lshr i128 %self, %_4.i
  ret i128 %0
}

; <i128 as compiler_builtins::int::Int>::is_zero
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden zeroext i1 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$7is_zero17h22e4c998b7682872E"(i128 %self) unnamed_addr #2 {
start:
  %0 = icmp eq i128 %self, 0
  ret i1 %0
}

; <i128 as compiler_builtins::int::Int>::wrapping_neg
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_neg17h7a6c34ae93988e2bE"(i128 %self) unnamed_addr #2 {
start:
  %0 = sub i128 0, %self
  ret i128 %0
}

; <i128 as compiler_builtins::int::Int>::wrapping_add
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_add17h821b94ff07894d5eE"(i128 %self, i128 %other) unnamed_addr #2 {
start:
  %0 = add i128 %other, %self
  ret i128 %0
}

; <i128 as compiler_builtins::int::Int>::wrapping_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_mul17h33e4d9671042a9e0E"(i128 %self, i128 %other) unnamed_addr #2 {
start:
  %0 = mul i128 %other, %self
  ret i128 %0
}

; <i128 as compiler_builtins::int::Int>::wrapping_sub
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_sub17h25ec688c41a1696eE"(i128 %self, i128 %other) unnamed_addr #2 {
start:
  %0 = sub i128 %self, %other
  ret i128 %0
}

; <i128 as compiler_builtins::int::Int>::wrapping_shl
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shl17hb1e633d126db72ddE"(i128 %self, i32 %other) unnamed_addr #2 {
start:
  %_5.i = and i32 %other, 127
  %_4.i = zext i32 %_5.i to i128
  %0 = shl i128 %self, %_4.i
  ret i128 %0
}

; <i128 as compiler_builtins::int::Int>::wrapping_shr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$12wrapping_shr17hf524af41001633fcE"(i128 %self, i32 %other) unnamed_addr #2 {
start:
  %_5.i = and i32 %other, 127
  %_4.i = zext i32 %_5.i to i128
  %0 = ashr i128 %self, %_4.i
  ret i128 %0
}

; <i128 as compiler_builtins::int::Int>::rotate_left
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$11rotate_left17hbfbc7d878fa5353eE"(i128 %self, i32 %other) unnamed_addr #1 {
start:
  %_4.i.i = zext i32 %other to i128
  %0 = tail call i128 @llvm.fshl.i128(i128 %self, i128 %self, i128 %_4.i.i) #14
  ret i128 %0
}

; <i128 as compiler_builtins::int::Int>::overflowing_add
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i128, i8 } @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$15overflowing_add17h0d048bdcd9cb4564E"(i128 %self, i128 %other) unnamed_addr #1 {
start:
  %0 = tail call { i128, i1 } @llvm.sadd.with.overflow.i128(i128 %self, i128 %other) #14
  %1 = extractvalue { i128, i1 } %0, 0
  %2 = extractvalue { i128, i1 } %0, 1
  %3 = zext i1 %2 to i8
  %4 = insertvalue { i128, i8 } undef, i128 %1, 0
  %5 = insertvalue { i128, i8 } %4, i8 %3, 1
  ret { i128, i8 } %5
}

; <i128 as compiler_builtins::int::Int>::leading_zeros
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN52_$LT$i128$u20$as$u20$compiler_builtins..int..Int$GT$13leading_zeros17hfc5e21c2edd9d8a2E"(i128 %self) unnamed_addr #1 {
start:
  %0 = tail call i128 @llvm.ctlz.i128(i128 %self, i1 false) #14, !range !7
  %1 = trunc i128 %0 to i32
  ret i32 %1
}

; <i16 as compiler_builtins::int::DInt>::lo
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h3334d26e0c9c4bfbE"(i16 %self) unnamed_addr #2 {
start:
  %0 = trunc i16 %self to i8
  ret i8 %0
}

; <i16 as compiler_builtins::int::DInt>::hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17h1463329f897e96d7E"(i16 %self) unnamed_addr #2 {
start:
  %0 = lshr i16 %self, 8
  %1 = trunc i16 %0 to i8
  ret i8 %1
}

; <i16 as compiler_builtins::int::DInt>::lo_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i8, i8 } @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17hab48faaeafa389a5E"(i16 %self) unnamed_addr #2 {
start:
  %0 = trunc i16 %self to i8
  %1 = lshr i16 %self, 8
  %2 = trunc i16 %1 to i8
  %3 = insertvalue { i8, i8 } undef, i8 %0, 0
  %4 = insertvalue { i8, i8 } %3, i8 %2, 1
  ret { i8, i8 } %4
}

; <i16 as compiler_builtins::int::DInt>::from_lo_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17h8765f91e8aa5dee9E"(i8 %lo, i8 %hi) unnamed_addr #2 {
start:
  %0 = zext i8 %lo to i16
  %_21.i = zext i8 %hi to i16
  %1 = shl nuw i16 %_21.i, 8
  %2 = or i16 %1, %0
  ret i16 %2
}

; <i32 as compiler_builtins::int::DInt>::lo
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h14ba82835a8a932fE"(i32 %self) unnamed_addr #2 {
start:
  %0 = trunc i32 %self to i16
  ret i16 %0
}

; <i32 as compiler_builtins::int::DInt>::hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17h402e54b1d608b742E"(i32 %self) unnamed_addr #2 {
start:
  %0 = lshr i32 %self, 16
  %1 = trunc i32 %0 to i16
  ret i16 %1
}

; <i32 as compiler_builtins::int::DInt>::lo_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i16, i16 } @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17hd7577bd073daf64fE"(i32 %self) unnamed_addr #2 {
start:
  %0 = trunc i32 %self to i16
  %1 = lshr i32 %self, 16
  %2 = trunc i32 %1 to i16
  %3 = insertvalue { i16, i16 } undef, i16 %0, 0
  %4 = insertvalue { i16, i16 } %3, i16 %2, 1
  ret { i16, i16 } %4
}

; <i32 as compiler_builtins::int::DInt>::from_lo_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17h3cb2943c34635f8bE"(i16 %lo, i16 %hi) unnamed_addr #2 {
start:
  %0 = zext i16 %lo to i32
  %_21.i = zext i16 %hi to i32
  %1 = shl nuw i32 %_21.i, 16
  %2 = or i32 %1, %0
  ret i32 %2
}

; <i64 as compiler_builtins::int::DInt>::lo
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17haba78631228d2ab6E"(i64 %self) unnamed_addr #2 {
start:
  %0 = trunc i64 %self to i32
  ret i32 %0
}

; <i64 as compiler_builtins::int::DInt>::hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17h7d8e3bea6fc6af1fE"(i64 %self) unnamed_addr #2 {
start:
  %0 = lshr i64 %self, 32
  %1 = trunc i64 %0 to i32
  ret i32 %1
}

; <i64 as compiler_builtins::int::DInt>::lo_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i32, i32 } @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17h538bfc8f40787228E"(i64 %self) unnamed_addr #2 {
start:
  %0 = trunc i64 %self to i32
  %1 = lshr i64 %self, 32
  %2 = trunc i64 %1 to i32
  %3 = insertvalue { i32, i32 } undef, i32 %0, 0
  %4 = insertvalue { i32, i32 } %3, i32 %2, 1
  ret { i32, i32 } %4
}

; <i64 as compiler_builtins::int::DInt>::from_lo_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17h7cf7596ce05f1e02E"(i32 %lo, i32 %hi) unnamed_addr #2 {
start:
  %0 = zext i32 %lo to i64
  %_21.i = zext i32 %hi to i64
  %1 = shl nuw i64 %_21.i, 32
  %2 = or i64 %1, %0
  ret i64 %2
}

; <i128 as compiler_builtins::int::DInt>::lo
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2lo17h972d40a580ed2081E"(i128 %self) unnamed_addr #2 {
start:
  %0 = trunc i128 %self to i64
  ret i64 %0
}

; <i128 as compiler_builtins::int::DInt>::hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$2hi17hbc6a3496555ac319E"(i128 %self) unnamed_addr #2 {
start:
  %0 = lshr i128 %self, 64
  %1 = trunc i128 %0 to i64
  ret i64 %1
}

; <i128 as compiler_builtins::int::DInt>::lo_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden { i64, i64 } @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$5lo_hi17h3e0a66c71be1dee7E"(i128 %self) unnamed_addr #2 {
start:
  %0 = trunc i128 %self to i64
  %1 = lshr i128 %self, 64
  %2 = trunc i128 %1 to i64
  %3 = insertvalue { i64, i64 } undef, i64 %0, 0
  %4 = insertvalue { i64, i64 } %3, i64 %2, 1
  ret { i64, i64 } %4
}

; <i128 as compiler_builtins::int::DInt>::from_lo_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN53_$LT$i128$u20$as$u20$compiler_builtins..int..DInt$GT$10from_lo_hi17ha95ab27448b8e8e4E"(i64 %lo, i64 %hi) unnamed_addr #2 {
start:
  %0 = zext i64 %lo to i128
  %_21.i = zext i64 %hi to i128
  %1 = shl nuw i128 %_21.i, 64
  %2 = or i128 %1, %0
  ret i128 %2
}

; <i8 as compiler_builtins::int::HInt>::widen
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h9389d382586f2123E"(i8 %self) unnamed_addr #2 {
start:
  %0 = sext i8 %self to i16
  ret i16 %0
}

; <i8 as compiler_builtins::int::HInt>::zero_widen
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hd581a7bb2f7c254cE"(i8 %self) unnamed_addr #2 {
start:
  %0 = zext i8 %self to i16
  ret i16 %0
}

; <i8 as compiler_builtins::int::HInt>::widen_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17h40e91ceb18778addE"(i8 %self) unnamed_addr #2 {
start:
  %_21 = zext i8 %self to i16
  %0 = shl nuw i16 %_21, 8
  ret i16 %0
}

; <i8 as compiler_builtins::int::HInt>::zero_widen_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17hbb96828275347aa3E"(i8 %self, i8 %rhs) unnamed_addr #2 {
start:
  %0 = zext i8 %self to i16
  %1 = zext i8 %rhs to i16
  %2 = mul nuw i16 %1, %0
  ret i16 %2
}

; <i8 as compiler_builtins::int::HInt>::widen_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN51_$LT$i8$u20$as$u20$compiler_builtins..int..HInt$GT$9widen_mul17h7c7a6fad1ea3612dE"(i8 %self, i8 %rhs) unnamed_addr #2 {
start:
  %0 = sext i8 %self to i16
  %1 = sext i8 %rhs to i16
  %2 = mul nsw i16 %1, %0
  ret i16 %2
}

; <i16 as compiler_builtins::int::HInt>::widen
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17hdf95dcca3e132be5E"(i16 %self) unnamed_addr #2 {
start:
  %0 = sext i16 %self to i32
  ret i32 %0
}

; <i16 as compiler_builtins::int::HInt>::zero_widen
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17h152d0a820037c71fE"(i16 %self) unnamed_addr #2 {
start:
  %0 = zext i16 %self to i32
  ret i32 %0
}

; <i16 as compiler_builtins::int::HInt>::widen_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17hcddfc0cb6da81b4dE"(i16 %self) unnamed_addr #2 {
start:
  %_21 = zext i16 %self to i32
  %0 = shl nuw i32 %_21, 16
  ret i32 %0
}

; <i16 as compiler_builtins::int::HInt>::zero_widen_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17hf9d4c4b0930d8528E"(i16 %self, i16 %rhs) unnamed_addr #2 {
start:
  %0 = zext i16 %self to i32
  %1 = zext i16 %rhs to i32
  %2 = mul nuw i32 %1, %0
  ret i32 %2
}

; <i16 as compiler_builtins::int::HInt>::widen_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN52_$LT$i16$u20$as$u20$compiler_builtins..int..HInt$GT$9widen_mul17h440968f508d19bbcE"(i16 %self, i16 %rhs) unnamed_addr #2 {
start:
  %0 = sext i16 %self to i32
  %1 = sext i16 %rhs to i32
  %2 = mul nsw i32 %1, %0
  ret i32 %2
}

; <i32 as compiler_builtins::int::HInt>::widen
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h8e95efb8625550c0E"(i32 %self) unnamed_addr #2 {
start:
  %0 = sext i32 %self to i64
  ret i64 %0
}

; <i32 as compiler_builtins::int::HInt>::zero_widen
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17he456ea74c9916a66E"(i32 %self) unnamed_addr #2 {
start:
  %0 = zext i32 %self to i64
  ret i64 %0
}

; <i32 as compiler_builtins::int::HInt>::widen_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17h43e737d59a8c5312E"(i32 %self) unnamed_addr #2 {
start:
  %_21 = zext i32 %self to i64
  %0 = shl nuw i64 %_21, 32
  ret i64 %0
}

; <i32 as compiler_builtins::int::HInt>::zero_widen_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17h031c10a41ee194f4E"(i32 %self, i32 %rhs) unnamed_addr #2 {
start:
  %0 = zext i32 %self to i64
  %1 = zext i32 %rhs to i64
  %2 = mul nuw i64 %1, %0
  ret i64 %2
}

; <i32 as compiler_builtins::int::HInt>::widen_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN52_$LT$i32$u20$as$u20$compiler_builtins..int..HInt$GT$9widen_mul17h5aa9dde94948e382E"(i32 %self, i32 %rhs) unnamed_addr #2 {
start:
  %0 = sext i32 %self to i64
  %1 = sext i32 %rhs to i64
  %2 = mul nsw i64 %1, %0
  ret i64 %2
}

; <i64 as compiler_builtins::int::HInt>::widen
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$5widen17h759088ab849a8fa7E"(i64 %self) unnamed_addr #2 {
start:
  %0 = sext i64 %self to i128
  ret i128 %0
}

; <i64 as compiler_builtins::int::HInt>::zero_widen
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$10zero_widen17hab7aa2541e64881cE"(i64 %self) unnamed_addr #2 {
start:
  %0 = zext i64 %self to i128
  ret i128 %0
}

; <i64 as compiler_builtins::int::HInt>::widen_hi
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$8widen_hi17h25b5d792c52a426bE"(i64 %self) unnamed_addr #2 {
start:
  %_21 = zext i64 %self to i128
  %0 = shl nuw i128 %_21, 64
  ret i128 %0
}

; <i64 as compiler_builtins::int::HInt>::zero_widen_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$14zero_widen_mul17h27d545639dd77e61E"(i64 %self, i64 %rhs) unnamed_addr #2 {
start:
  %0 = zext i64 %self to i128
  %1 = zext i64 %rhs to i128
  %2 = mul nuw i128 %1, %0
  ret i128 %2
}

; <i64 as compiler_builtins::int::HInt>::widen_mul
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN52_$LT$i64$u20$as$u20$compiler_builtins..int..HInt$GT$9widen_mul17h2d6d3b5b9cf659adE"(i64 %self, i64 %rhs) unnamed_addr #2 {
start:
  %0 = sext i64 %self to i128
  %1 = sext i64 %rhs to i128
  %2 = mul nsw i128 %1, %0
  ret i128 %2
}

; <u8 as compiler_builtins::int::CastInto<i32>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN66_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17hecc048f26c9a4f63E"(i8 %self) unnamed_addr #2 {
start:
  %0 = zext i8 %self to i32
  ret i32 %0
}

; <u8 as compiler_builtins::int::CastInto<i64>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN66_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hef58ba17cd498d0eE"(i8 %self) unnamed_addr #2 {
start:
  %0 = zext i8 %self to i64
  ret i64 %0
}

; <u8 as compiler_builtins::int::CastInto<i128>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN67_$LT$u8$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17hee76aae7d86d0d2fE"(i8 %self) unnamed_addr #2 {
start:
  %0 = zext i8 %self to i128
  ret i128 %0
}

; <i8 as compiler_builtins::int::CastInto<i32>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN66_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h876343148483e526E"(i8 %self) unnamed_addr #2 {
start:
  %0 = sext i8 %self to i32
  ret i32 %0
}

; <i8 as compiler_builtins::int::CastInto<i64>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN66_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hbe3fb013ddfdd8caE"(i8 %self) unnamed_addr #2 {
start:
  %0 = sext i8 %self to i64
  ret i64 %0
}

; <i8 as compiler_builtins::int::CastInto<i128>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN67_$LT$i8$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h5ccdd81da1cde183E"(i8 %self) unnamed_addr #2 {
start:
  %0 = sext i8 %self to i128
  ret i128 %0
}

; <u16 as compiler_builtins::int::CastInto<i64>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN67_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17h8cb48dfdfc621d2cE"(i16 %self) unnamed_addr #2 {
start:
  %0 = zext i16 %self to i64
  ret i64 %0
}

; <u16 as compiler_builtins::int::CastInto<i128>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN68_$LT$u16$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h4caef293bb753cc8E"(i16 %self) unnamed_addr #2 {
start:
  %0 = zext i16 %self to i128
  ret i128 %0
}

; <i16 as compiler_builtins::int::CastInto<i64>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i64 @"_ZN67_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$i64$GT$$GT$4cast17hb094f236667a92a9E"(i16 %self) unnamed_addr #2 {
start:
  %0 = sext i16 %self to i64
  ret i64 %0
}

; <i16 as compiler_builtins::int::CastInto<i128>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN68_$LT$i16$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17hf56dcc0126695dcaE"(i16 %self) unnamed_addr #2 {
start:
  %0 = sext i16 %self to i128
  ret i128 %0
}

; <u32 as compiler_builtins::int::CastInto<i128>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN68_$LT$u32$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h6a3f09413d52631bE"(i32 %self) unnamed_addr #2 {
start:
  %0 = zext i32 %self to i128
  ret i128 %0
}

; <i32 as compiler_builtins::int::CastInto<i8>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN66_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h863dd52dfb398b3bE"(i32 %self) unnamed_addr #2 {
start:
  %0 = trunc i32 %self to i8
  ret i8 %0
}

; <i32 as compiler_builtins::int::CastInto<i128>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i128 @"_ZN68_$LT$i32$u20$as$u20$compiler_builtins..int..CastInto$LT$i128$GT$$GT$4cast17h8d02e2a8dc9e3ebdE"(i32 %self) unnamed_addr #2 {
start:
  %0 = sext i32 %self to i128
  ret i128 %0
}

; <i64 as compiler_builtins::int::CastInto<i8>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN66_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17hcfcef1dacc7028bdE"(i64 %self) unnamed_addr #2 {
start:
  %0 = trunc i64 %self to i8
  ret i8 %0
}

; <i64 as compiler_builtins::int::CastInto<i16>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN67_$LT$i64$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17h809442d170a31f22E"(i64 %self) unnamed_addr #2 {
start:
  %0 = trunc i64 %self to i16
  ret i16 %0
}

; <i128 as compiler_builtins::int::CastInto<i8>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i8 @"_ZN67_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i8$GT$$GT$4cast17h8d2703c035b9f847E"(i128 %self) unnamed_addr #2 {
start:
  %0 = trunc i128 %self to i8
  ret i8 %0
}

; <i128 as compiler_builtins::int::CastInto<i16>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i16 @"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i16$GT$$GT$4cast17hc4f7c240ff061307E"(i128 %self) unnamed_addr #2 {
start:
  %0 = trunc i128 %self to i16
  ret i16 %0
}

; <i128 as compiler_builtins::int::CastInto<i32>>::cast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define hidden i32 @"_ZN68_$LT$i128$u20$as$u20$compiler_builtins..int..CastInto$LT$i32$GT$$GT$4cast17h81cef005f31fdf2eE"(i128 %self) unnamed_addr #2 {
start:
  %0 = trunc i128 %self to i32
  ret i32 %0
}

; compiler_builtins::mem::__llvm_memcpy_element_unordered_atomic_1
; Function Attrs: nofree norecurse nosync nounwind nonlazybind uwtable
define hidden void @_ZN17compiler_builtins3mem40__llvm_memcpy_element_unordered_atomic_117h4ecc9812d3e3ab8dE(i8* nocapture %dest, i8* nocapture readonly %src, i64 %bytes) unnamed_addr #8 {
start:
  %_87.not.i = icmp eq i64 %bytes, 0
  br i1 %_87.not.i, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit, label %bb4.i.preheader

bb4.i.preheader:                                  ; preds = %start
  %0 = add i64 %bytes, -1
  %xtraiter = and i64 %bytes, 3
  %1 = icmp ult i64 %0, 3
  br i1 %1, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit.loopexit.unr-lcssa, label %bb4.i.preheader.new

bb4.i.preheader.new:                              ; preds = %bb4.i.preheader
  %unroll_iter = and i64 %bytes, -4
  br label %bb4.i

bb4.i:                                            ; preds = %bb4.i, %bb4.i.preheader.new
  %i.08.i = phi i64 [ 0, %bb4.i.preheader.new ], [ %17, %bb4.i ]
  %niter = phi i64 [ %unroll_iter, %bb4.i.preheader.new ], [ %niter.nsub.3, %bb4.i ]
  %2 = getelementptr inbounds i8, i8* %dest, i64 %i.08.i
  %3 = getelementptr inbounds i8, i8* %src, i64 %i.08.i
  %4 = load atomic i8, i8* %3 unordered, align 1
  store atomic i8 %4, i8* %2 unordered, align 1
  %5 = or i64 %i.08.i, 1
  %6 = getelementptr inbounds i8, i8* %dest, i64 %5
  %7 = getelementptr inbounds i8, i8* %src, i64 %5
  %8 = load atomic i8, i8* %7 unordered, align 1
  store atomic i8 %8, i8* %6 unordered, align 1
  %9 = or i64 %i.08.i, 2
  %10 = getelementptr inbounds i8, i8* %dest, i64 %9
  %11 = getelementptr inbounds i8, i8* %src, i64 %9
  %12 = load atomic i8, i8* %11 unordered, align 1
  store atomic i8 %12, i8* %10 unordered, align 1
  %13 = or i64 %i.08.i, 3
  %14 = getelementptr inbounds i8, i8* %dest, i64 %13
  %15 = getelementptr inbounds i8, i8* %src, i64 %13
  %16 = load atomic i8, i8* %15 unordered, align 1
  store atomic i8 %16, i8* %14 unordered, align 1
  %17 = add nuw i64 %i.08.i, 4
  %niter.nsub.3 = add i64 %niter, -4
  %niter.ncmp.3 = icmp eq i64 %niter.nsub.3, 0
  br i1 %niter.ncmp.3, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit.loopexit.unr-lcssa, label %bb4.i

_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit.loopexit.unr-lcssa: ; preds = %bb4.i, %bb4.i.preheader
  %i.08.i.unr = phi i64 [ 0, %bb4.i.preheader ], [ %17, %bb4.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit, label %bb4.i.epil

bb4.i.epil:                                       ; preds = %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit.loopexit.unr-lcssa, %bb4.i.epil
  %i.08.i.epil = phi i64 [ %21, %bb4.i.epil ], [ %i.08.i.unr, %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb4.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit.loopexit.unr-lcssa ]
  %18 = getelementptr inbounds i8, i8* %dest, i64 %i.08.i.epil
  %19 = getelementptr inbounds i8, i8* %src, i64 %i.08.i.epil
  %20 = load atomic i8, i8* %19 unordered, align 1
  store atomic i8 %20, i8* %18 unordered, align 1
  %21 = add nuw i64 %i.08.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit, label %bb4.i.epil, !llvm.loop !53

_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit: ; preds = %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb2ac1fe42f59dcf1E.exit.loopexit.unr-lcssa, %bb4.i.epil, %start
  ret void
}

; compiler_builtins::mem::__llvm_memcpy_element_unordered_atomic_2
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden void @_ZN17compiler_builtins3mem40__llvm_memcpy_element_unordered_atomic_217h7320826cace4b95dE(i16* nocapture %dest, i16* nocapture readonly %src, i64 %bytes) unnamed_addr #6 {
start:
  %_89.not.i = icmp eq i64 %bytes, 0
  br i1 %_89.not.i, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit, label %bb4.preheader.i

bb4.preheader.i:                                  ; preds = %start
  %0 = lshr i64 %bytes, 1
  %umax.i = tail call i64 @llvm.umax.i64(i64 %0, i64 1) #14
  %1 = add nsw i64 %umax.i, -1
  %xtraiter = and i64 %umax.i, 3
  %2 = icmp ult i64 %1, 3
  br i1 %2, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit.loopexit.unr-lcssa, label %bb4.preheader.i.new

bb4.preheader.i.new:                              ; preds = %bb4.preheader.i
  %unroll_iter = and i64 %umax.i, 9223372036854775804
  br label %bb4.i

bb4.i:                                            ; preds = %bb4.i, %bb4.preheader.i.new
  %i.010.i = phi i64 [ 0, %bb4.preheader.i.new ], [ %18, %bb4.i ]
  %niter = phi i64 [ %unroll_iter, %bb4.preheader.i.new ], [ %niter.nsub.3, %bb4.i ]
  %3 = getelementptr inbounds i16, i16* %dest, i64 %i.010.i
  %4 = getelementptr inbounds i16, i16* %src, i64 %i.010.i
  %5 = load atomic i16, i16* %4 unordered, align 2
  store atomic i16 %5, i16* %3 unordered, align 2
  %6 = or i64 %i.010.i, 1
  %7 = getelementptr inbounds i16, i16* %dest, i64 %6
  %8 = getelementptr inbounds i16, i16* %src, i64 %6
  %9 = load atomic i16, i16* %8 unordered, align 2
  store atomic i16 %9, i16* %7 unordered, align 2
  %10 = or i64 %i.010.i, 2
  %11 = getelementptr inbounds i16, i16* %dest, i64 %10
  %12 = getelementptr inbounds i16, i16* %src, i64 %10
  %13 = load atomic i16, i16* %12 unordered, align 2
  store atomic i16 %13, i16* %11 unordered, align 2
  %14 = or i64 %i.010.i, 3
  %15 = getelementptr inbounds i16, i16* %dest, i64 %14
  %16 = getelementptr inbounds i16, i16* %src, i64 %14
  %17 = load atomic i16, i16* %16 unordered, align 2
  store atomic i16 %17, i16* %15 unordered, align 2
  %18 = add nuw nsw i64 %i.010.i, 4
  %niter.nsub.3 = add i64 %niter, -4
  %niter.ncmp.3 = icmp eq i64 %niter.nsub.3, 0
  br i1 %niter.ncmp.3, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit.loopexit.unr-lcssa, label %bb4.i

_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit.loopexit.unr-lcssa: ; preds = %bb4.i, %bb4.preheader.i
  %i.010.i.unr = phi i64 [ 0, %bb4.preheader.i ], [ %18, %bb4.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit, label %bb4.i.epil

bb4.i.epil:                                       ; preds = %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit.loopexit.unr-lcssa, %bb4.i.epil
  %i.010.i.epil = phi i64 [ %22, %bb4.i.epil ], [ %i.010.i.unr, %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb4.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit.loopexit.unr-lcssa ]
  %19 = getelementptr inbounds i16, i16* %dest, i64 %i.010.i.epil
  %20 = getelementptr inbounds i16, i16* %src, i64 %i.010.i.epil
  %21 = load atomic i16, i16* %20 unordered, align 2
  store atomic i16 %21, i16* %19 unordered, align 2
  %22 = add nuw nsw i64 %i.010.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit, label %bb4.i.epil, !llvm.loop !54

_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit: ; preds = %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hedf5a835051efb61E.exit.loopexit.unr-lcssa, %bb4.i.epil, %start
  ret void
}

; compiler_builtins::mem::__llvm_memcpy_element_unordered_atomic_4
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden void @_ZN17compiler_builtins3mem40__llvm_memcpy_element_unordered_atomic_417h57e9c294c739a7c5E(i32* nocapture %dest, i32* nocapture readonly %src, i64 %bytes) unnamed_addr #6 {
start:
  %_89.not.i = icmp eq i64 %bytes, 0
  br i1 %_89.not.i, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit, label %bb4.preheader.i

bb4.preheader.i:                                  ; preds = %start
  %0 = lshr i64 %bytes, 2
  %umax.i = tail call i64 @llvm.umax.i64(i64 %0, i64 1) #14
  %1 = add nsw i64 %umax.i, -1
  %xtraiter = and i64 %umax.i, 3
  %2 = icmp ult i64 %1, 3
  br i1 %2, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit.loopexit.unr-lcssa, label %bb4.preheader.i.new

bb4.preheader.i.new:                              ; preds = %bb4.preheader.i
  %unroll_iter = and i64 %umax.i, 4611686018427387900
  br label %bb4.i

bb4.i:                                            ; preds = %bb4.i, %bb4.preheader.i.new
  %i.010.i = phi i64 [ 0, %bb4.preheader.i.new ], [ %18, %bb4.i ]
  %niter = phi i64 [ %unroll_iter, %bb4.preheader.i.new ], [ %niter.nsub.3, %bb4.i ]
  %3 = getelementptr inbounds i32, i32* %dest, i64 %i.010.i
  %4 = getelementptr inbounds i32, i32* %src, i64 %i.010.i
  %5 = load atomic i32, i32* %4 unordered, align 4
  store atomic i32 %5, i32* %3 unordered, align 4
  %6 = or i64 %i.010.i, 1
  %7 = getelementptr inbounds i32, i32* %dest, i64 %6
  %8 = getelementptr inbounds i32, i32* %src, i64 %6
  %9 = load atomic i32, i32* %8 unordered, align 4
  store atomic i32 %9, i32* %7 unordered, align 4
  %10 = or i64 %i.010.i, 2
  %11 = getelementptr inbounds i32, i32* %dest, i64 %10
  %12 = getelementptr inbounds i32, i32* %src, i64 %10
  %13 = load atomic i32, i32* %12 unordered, align 4
  store atomic i32 %13, i32* %11 unordered, align 4
  %14 = or i64 %i.010.i, 3
  %15 = getelementptr inbounds i32, i32* %dest, i64 %14
  %16 = getelementptr inbounds i32, i32* %src, i64 %14
  %17 = load atomic i32, i32* %16 unordered, align 4
  store atomic i32 %17, i32* %15 unordered, align 4
  %18 = add nuw nsw i64 %i.010.i, 4
  %niter.nsub.3 = add i64 %niter, -4
  %niter.ncmp.3 = icmp eq i64 %niter.nsub.3, 0
  br i1 %niter.ncmp.3, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit.loopexit.unr-lcssa, label %bb4.i

_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit.loopexit.unr-lcssa: ; preds = %bb4.i, %bb4.preheader.i
  %i.010.i.unr = phi i64 [ 0, %bb4.preheader.i ], [ %18, %bb4.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit, label %bb4.i.epil

bb4.i.epil:                                       ; preds = %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit.loopexit.unr-lcssa, %bb4.i.epil
  %i.010.i.epil = phi i64 [ %22, %bb4.i.epil ], [ %i.010.i.unr, %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb4.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit.loopexit.unr-lcssa ]
  %19 = getelementptr inbounds i32, i32* %dest, i64 %i.010.i.epil
  %20 = getelementptr inbounds i32, i32* %src, i64 %i.010.i.epil
  %21 = load atomic i32, i32* %20 unordered, align 4
  store atomic i32 %21, i32* %19 unordered, align 4
  %22 = add nuw nsw i64 %i.010.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit, label %bb4.i.epil, !llvm.loop !55

_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit: ; preds = %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hb17672ecd24f3fe6E.exit.loopexit.unr-lcssa, %bb4.i.epil, %start
  ret void
}

; compiler_builtins::mem::__llvm_memcpy_element_unordered_atomic_8
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden void @_ZN17compiler_builtins3mem40__llvm_memcpy_element_unordered_atomic_817h60f1af3251af768aE(i64* nocapture %dest, i64* nocapture readonly %src, i64 %bytes) unnamed_addr #6 {
start:
  %_89.not.i = icmp eq i64 %bytes, 0
  br i1 %_89.not.i, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit, label %bb4.preheader.i

bb4.preheader.i:                                  ; preds = %start
  %0 = lshr i64 %bytes, 3
  %umax.i = tail call i64 @llvm.umax.i64(i64 %0, i64 1) #14
  %1 = add nsw i64 %umax.i, -1
  %xtraiter = and i64 %umax.i, 3
  %2 = icmp ult i64 %1, 3
  br i1 %2, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit.loopexit.unr-lcssa, label %bb4.preheader.i.new

bb4.preheader.i.new:                              ; preds = %bb4.preheader.i
  %unroll_iter = and i64 %umax.i, 2305843009213693948
  br label %bb4.i

bb4.i:                                            ; preds = %bb4.i, %bb4.preheader.i.new
  %i.010.i = phi i64 [ 0, %bb4.preheader.i.new ], [ %18, %bb4.i ]
  %niter = phi i64 [ %unroll_iter, %bb4.preheader.i.new ], [ %niter.nsub.3, %bb4.i ]
  %3 = getelementptr inbounds i64, i64* %dest, i64 %i.010.i
  %4 = getelementptr inbounds i64, i64* %src, i64 %i.010.i
  %5 = load atomic i64, i64* %4 unordered, align 8
  store atomic i64 %5, i64* %3 unordered, align 8
  %6 = or i64 %i.010.i, 1
  %7 = getelementptr inbounds i64, i64* %dest, i64 %6
  %8 = getelementptr inbounds i64, i64* %src, i64 %6
  %9 = load atomic i64, i64* %8 unordered, align 8
  store atomic i64 %9, i64* %7 unordered, align 8
  %10 = or i64 %i.010.i, 2
  %11 = getelementptr inbounds i64, i64* %dest, i64 %10
  %12 = getelementptr inbounds i64, i64* %src, i64 %10
  %13 = load atomic i64, i64* %12 unordered, align 8
  store atomic i64 %13, i64* %11 unordered, align 8
  %14 = or i64 %i.010.i, 3
  %15 = getelementptr inbounds i64, i64* %dest, i64 %14
  %16 = getelementptr inbounds i64, i64* %src, i64 %14
  %17 = load atomic i64, i64* %16 unordered, align 8
  store atomic i64 %17, i64* %15 unordered, align 8
  %18 = add nuw nsw i64 %i.010.i, 4
  %niter.nsub.3 = add i64 %niter, -4
  %niter.ncmp.3 = icmp eq i64 %niter.nsub.3, 0
  br i1 %niter.ncmp.3, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit.loopexit.unr-lcssa, label %bb4.i

_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit.loopexit.unr-lcssa: ; preds = %bb4.i, %bb4.preheader.i
  %i.010.i.unr = phi i64 [ 0, %bb4.preheader.i ], [ %18, %bb4.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit, label %bb4.i.epil

bb4.i.epil:                                       ; preds = %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit.loopexit.unr-lcssa, %bb4.i.epil
  %i.010.i.epil = phi i64 [ %22, %bb4.i.epil ], [ %i.010.i.unr, %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb4.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit.loopexit.unr-lcssa ]
  %19 = getelementptr inbounds i64, i64* %dest, i64 %i.010.i.epil
  %20 = getelementptr inbounds i64, i64* %src, i64 %i.010.i.epil
  %21 = load atomic i64, i64* %20 unordered, align 8
  store atomic i64 %21, i64* %19 unordered, align 8
  %22 = add nuw nsw i64 %i.010.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit, label %bb4.i.epil, !llvm.loop !56

_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit: ; preds = %_ZN17compiler_builtins3mem31memcpy_element_unordered_atomic17hca8cf0f85eff5594E.exit.loopexit.unr-lcssa, %bb4.i.epil, %start
  ret void
}

; compiler_builtins::mem::__llvm_memmove_element_unordered_atomic_1
; Function Attrs: nofree norecurse nosync nounwind nonlazybind uwtable
define hidden void @_ZN17compiler_builtins3mem41__llvm_memmove_element_unordered_atomic_117hab562ca7a704838eE(i8* %dest, i8* readonly %src, i64 %bytes) unnamed_addr #8 {
start:
  %_7.i = icmp ult i8* %src, %dest
  %0 = icmp eq i64 %bytes, 0
  br i1 %_7.i, label %bb4.preheader.i, label %bb12.preheader.i

bb12.preheader.i:                                 ; preds = %start
  br i1 %0, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit, label %bb13.i.preheader

bb13.i.preheader:                                 ; preds = %bb12.preheader.i
  %1 = add i64 %bytes, -1
  %xtraiter = and i64 %bytes, 3
  %2 = icmp ult i64 %1, 3
  br i1 %2, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit.loopexit5.unr-lcssa, label %bb13.i.preheader.new

bb13.i.preheader.new:                             ; preds = %bb13.i.preheader
  %unroll_iter = and i64 %bytes, -4
  br label %bb13.i

bb4.preheader.i:                                  ; preds = %start
  br i1 %0, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit, label %bb5.i.preheader

bb5.i.preheader:                                  ; preds = %bb4.preheader.i
  %3 = add i64 %bytes, -1
  %xtraiter6 = and i64 %bytes, 3
  %lcmp.mod7.not = icmp eq i64 %xtraiter6, 0
  br i1 %lcmp.mod7.not, label %bb5.i.prol.loopexit, label %bb5.i.prol

bb5.i.prol:                                       ; preds = %bb5.i.preheader, %bb5.i.prol
  %i.016.i.prol = phi i64 [ %4, %bb5.i.prol ], [ %bytes, %bb5.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb5.i.prol ], [ %xtraiter6, %bb5.i.preheader ]
  %4 = add i64 %i.016.i.prol, -1
  %5 = getelementptr inbounds i8, i8* %dest, i64 %4
  %6 = getelementptr inbounds i8, i8* %src, i64 %4
  %7 = load atomic i8, i8* %6 unordered, align 1
  store atomic i8 %7, i8* %5 unordered, align 1
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb5.i.prol.loopexit, label %bb5.i.prol, !llvm.loop !57

bb5.i.prol.loopexit:                              ; preds = %bb5.i.prol, %bb5.i.preheader
  %i.016.i.unr = phi i64 [ %bytes, %bb5.i.preheader ], [ %4, %bb5.i.prol ]
  %8 = icmp ult i64 %3, 3
  br i1 %8, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit, label %bb5.i

bb5.i:                                            ; preds = %bb5.i.prol.loopexit, %bb5.i
  %i.016.i = phi i64 [ %21, %bb5.i ], [ %i.016.i.unr, %bb5.i.prol.loopexit ]
  %9 = add i64 %i.016.i, -1
  %10 = getelementptr inbounds i8, i8* %dest, i64 %9
  %11 = getelementptr inbounds i8, i8* %src, i64 %9
  %12 = load atomic i8, i8* %11 unordered, align 1
  store atomic i8 %12, i8* %10 unordered, align 1
  %13 = add i64 %i.016.i, -2
  %14 = getelementptr inbounds i8, i8* %dest, i64 %13
  %15 = getelementptr inbounds i8, i8* %src, i64 %13
  %16 = load atomic i8, i8* %15 unordered, align 1
  store atomic i8 %16, i8* %14 unordered, align 1
  %17 = add i64 %i.016.i, -3
  %18 = getelementptr inbounds i8, i8* %dest, i64 %17
  %19 = getelementptr inbounds i8, i8* %src, i64 %17
  %20 = load atomic i8, i8* %19 unordered, align 1
  store atomic i8 %20, i8* %18 unordered, align 1
  %21 = add i64 %i.016.i, -4
  %22 = getelementptr inbounds i8, i8* %dest, i64 %21
  %23 = getelementptr inbounds i8, i8* %src, i64 %21
  %24 = load atomic i8, i8* %23 unordered, align 1
  store atomic i8 %24, i8* %22 unordered, align 1
  %25 = icmp eq i64 %21, 0
  br i1 %25, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit, label %bb5.i

bb13.i:                                           ; preds = %bb13.i, %bb13.i.preheader.new
  %i1.015.i = phi i64 [ 0, %bb13.i.preheader.new ], [ %41, %bb13.i ]
  %niter = phi i64 [ %unroll_iter, %bb13.i.preheader.new ], [ %niter.nsub.3, %bb13.i ]
  %26 = getelementptr inbounds i8, i8* %dest, i64 %i1.015.i
  %27 = getelementptr inbounds i8, i8* %src, i64 %i1.015.i
  %28 = load atomic i8, i8* %27 unordered, align 1
  store atomic i8 %28, i8* %26 unordered, align 1
  %29 = or i64 %i1.015.i, 1
  %30 = getelementptr inbounds i8, i8* %dest, i64 %29
  %31 = getelementptr inbounds i8, i8* %src, i64 %29
  %32 = load atomic i8, i8* %31 unordered, align 1
  store atomic i8 %32, i8* %30 unordered, align 1
  %33 = or i64 %i1.015.i, 2
  %34 = getelementptr inbounds i8, i8* %dest, i64 %33
  %35 = getelementptr inbounds i8, i8* %src, i64 %33
  %36 = load atomic i8, i8* %35 unordered, align 1
  store atomic i8 %36, i8* %34 unordered, align 1
  %37 = or i64 %i1.015.i, 3
  %38 = getelementptr inbounds i8, i8* %dest, i64 %37
  %39 = getelementptr inbounds i8, i8* %src, i64 %37
  %40 = load atomic i8, i8* %39 unordered, align 1
  store atomic i8 %40, i8* %38 unordered, align 1
  %41 = add nuw i64 %i1.015.i, 4
  %niter.nsub.3 = add i64 %niter, -4
  %niter.ncmp.3 = icmp eq i64 %niter.nsub.3, 0
  br i1 %niter.ncmp.3, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit.loopexit5.unr-lcssa, label %bb13.i

_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit.loopexit5.unr-lcssa: ; preds = %bb13.i, %bb13.i.preheader
  %i1.015.i.unr = phi i64 [ 0, %bb13.i.preheader ], [ %41, %bb13.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit, label %bb13.i.epil

bb13.i.epil:                                      ; preds = %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit.loopexit5.unr-lcssa, %bb13.i.epil
  %i1.015.i.epil = phi i64 [ %45, %bb13.i.epil ], [ %i1.015.i.unr, %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit.loopexit5.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb13.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit.loopexit5.unr-lcssa ]
  %42 = getelementptr inbounds i8, i8* %dest, i64 %i1.015.i.epil
  %43 = getelementptr inbounds i8, i8* %src, i64 %i1.015.i.epil
  %44 = load atomic i8, i8* %43 unordered, align 1
  store atomic i8 %44, i8* %42 unordered, align 1
  %45 = add nuw i64 %i1.015.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit, label %bb13.i.epil, !llvm.loop !58

_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit: ; preds = %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h60a3cfb4b0d3c105E.exit.loopexit5.unr-lcssa, %bb13.i.epil, %bb5.i.prol.loopexit, %bb5.i, %bb12.preheader.i, %bb4.preheader.i
  ret void
}

; compiler_builtins::mem::__llvm_memmove_element_unordered_atomic_2
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden void @_ZN17compiler_builtins3mem41__llvm_memmove_element_unordered_atomic_217h7953b6cedf6a9513E(i16* %dest, i16* readonly %src, i64 %bytes) unnamed_addr #6 {
start:
  %0 = lshr i64 %bytes, 1
  %_7.i = icmp ult i16* %src, %dest
  %1 = icmp eq i64 %bytes, 0
  br i1 %_7.i, label %bb4.preheader.i, label %bb12.preheader.i

bb12.preheader.i:                                 ; preds = %start
  br i1 %1, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit, label %bb13.preheader.i

bb13.preheader.i:                                 ; preds = %bb12.preheader.i
  %umax.i = tail call i64 @llvm.umax.i64(i64 %0, i64 1) #14
  %2 = add nsw i64 %umax.i, -1
  %xtraiter = and i64 %umax.i, 3
  %3 = icmp ult i64 %2, 3
  br i1 %3, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit.loopexit5.unr-lcssa, label %bb13.preheader.i.new

bb13.preheader.i.new:                             ; preds = %bb13.preheader.i
  %unroll_iter = and i64 %umax.i, 9223372036854775804
  br label %bb13.i

bb4.preheader.i:                                  ; preds = %start
  br i1 %1, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit, label %bb5.i.preheader

bb5.i.preheader:                                  ; preds = %bb4.preheader.i
  %4 = add nsw i64 %0, -1
  %xtraiter6 = and i64 %0, 3
  %lcmp.mod7.not = icmp eq i64 %xtraiter6, 0
  br i1 %lcmp.mod7.not, label %bb5.i.prol.loopexit, label %bb5.i.prol

bb5.i.prol:                                       ; preds = %bb5.i.preheader, %bb5.i.prol
  %i.020.i.prol = phi i64 [ %5, %bb5.i.prol ], [ %0, %bb5.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb5.i.prol ], [ %xtraiter6, %bb5.i.preheader ]
  %5 = add i64 %i.020.i.prol, -1
  %6 = getelementptr inbounds i16, i16* %dest, i64 %5
  %7 = getelementptr inbounds i16, i16* %src, i64 %5
  %8 = load atomic i16, i16* %7 unordered, align 2
  store atomic i16 %8, i16* %6 unordered, align 2
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb5.i.prol.loopexit, label %bb5.i.prol, !llvm.loop !59

bb5.i.prol.loopexit:                              ; preds = %bb5.i.prol, %bb5.i.preheader
  %i.020.i.unr = phi i64 [ %0, %bb5.i.preheader ], [ %5, %bb5.i.prol ]
  %9 = icmp ult i64 %4, 3
  br i1 %9, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit, label %bb5.i

bb5.i:                                            ; preds = %bb5.i.prol.loopexit, %bb5.i
  %i.020.i = phi i64 [ %22, %bb5.i ], [ %i.020.i.unr, %bb5.i.prol.loopexit ]
  %10 = add i64 %i.020.i, -1
  %11 = getelementptr inbounds i16, i16* %dest, i64 %10
  %12 = getelementptr inbounds i16, i16* %src, i64 %10
  %13 = load atomic i16, i16* %12 unordered, align 2
  store atomic i16 %13, i16* %11 unordered, align 2
  %14 = add i64 %i.020.i, -2
  %15 = getelementptr inbounds i16, i16* %dest, i64 %14
  %16 = getelementptr inbounds i16, i16* %src, i64 %14
  %17 = load atomic i16, i16* %16 unordered, align 2
  store atomic i16 %17, i16* %15 unordered, align 2
  %18 = add i64 %i.020.i, -3
  %19 = getelementptr inbounds i16, i16* %dest, i64 %18
  %20 = getelementptr inbounds i16, i16* %src, i64 %18
  %21 = load atomic i16, i16* %20 unordered, align 2
  store atomic i16 %21, i16* %19 unordered, align 2
  %22 = add i64 %i.020.i, -4
  %23 = getelementptr inbounds i16, i16* %dest, i64 %22
  %24 = getelementptr inbounds i16, i16* %src, i64 %22
  %25 = load atomic i16, i16* %24 unordered, align 2
  store atomic i16 %25, i16* %23 unordered, align 2
  %26 = icmp eq i64 %22, 0
  br i1 %26, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit, label %bb5.i

bb13.i:                                           ; preds = %bb13.i, %bb13.preheader.i.new
  %i1.019.i = phi i64 [ 0, %bb13.preheader.i.new ], [ %42, %bb13.i ]
  %niter = phi i64 [ %unroll_iter, %bb13.preheader.i.new ], [ %niter.nsub.3, %bb13.i ]
  %27 = getelementptr inbounds i16, i16* %dest, i64 %i1.019.i
  %28 = getelementptr inbounds i16, i16* %src, i64 %i1.019.i
  %29 = load atomic i16, i16* %28 unordered, align 2
  store atomic i16 %29, i16* %27 unordered, align 2
  %30 = or i64 %i1.019.i, 1
  %31 = getelementptr inbounds i16, i16* %dest, i64 %30
  %32 = getelementptr inbounds i16, i16* %src, i64 %30
  %33 = load atomic i16, i16* %32 unordered, align 2
  store atomic i16 %33, i16* %31 unordered, align 2
  %34 = or i64 %i1.019.i, 2
  %35 = getelementptr inbounds i16, i16* %dest, i64 %34
  %36 = getelementptr inbounds i16, i16* %src, i64 %34
  %37 = load atomic i16, i16* %36 unordered, align 2
  store atomic i16 %37, i16* %35 unordered, align 2
  %38 = or i64 %i1.019.i, 3
  %39 = getelementptr inbounds i16, i16* %dest, i64 %38
  %40 = getelementptr inbounds i16, i16* %src, i64 %38
  %41 = load atomic i16, i16* %40 unordered, align 2
  store atomic i16 %41, i16* %39 unordered, align 2
  %42 = add nuw nsw i64 %i1.019.i, 4
  %niter.nsub.3 = add i64 %niter, -4
  %niter.ncmp.3 = icmp eq i64 %niter.nsub.3, 0
  br i1 %niter.ncmp.3, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit.loopexit5.unr-lcssa, label %bb13.i

_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit.loopexit5.unr-lcssa: ; preds = %bb13.i, %bb13.preheader.i
  %i1.019.i.unr = phi i64 [ 0, %bb13.preheader.i ], [ %42, %bb13.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit, label %bb13.i.epil

bb13.i.epil:                                      ; preds = %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit.loopexit5.unr-lcssa, %bb13.i.epil
  %i1.019.i.epil = phi i64 [ %46, %bb13.i.epil ], [ %i1.019.i.unr, %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit.loopexit5.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb13.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit.loopexit5.unr-lcssa ]
  %43 = getelementptr inbounds i16, i16* %dest, i64 %i1.019.i.epil
  %44 = getelementptr inbounds i16, i16* %src, i64 %i1.019.i.epil
  %45 = load atomic i16, i16* %44 unordered, align 2
  store atomic i16 %45, i16* %43 unordered, align 2
  %46 = add nuw nsw i64 %i1.019.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit, label %bb13.i.epil, !llvm.loop !60

_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit: ; preds = %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17hac9a7bb918a046d9E.exit.loopexit5.unr-lcssa, %bb13.i.epil, %bb5.i.prol.loopexit, %bb5.i, %bb12.preheader.i, %bb4.preheader.i
  ret void
}

; compiler_builtins::mem::__llvm_memmove_element_unordered_atomic_4
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden void @_ZN17compiler_builtins3mem41__llvm_memmove_element_unordered_atomic_417h87333737abf52cfbE(i32* %dest, i32* readonly %src, i64 %bytes) unnamed_addr #6 {
start:
  %0 = lshr i64 %bytes, 2
  %_7.i = icmp ult i32* %src, %dest
  %1 = icmp eq i64 %bytes, 0
  br i1 %_7.i, label %bb4.preheader.i, label %bb12.preheader.i

bb12.preheader.i:                                 ; preds = %start
  br i1 %1, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit, label %bb13.preheader.i

bb13.preheader.i:                                 ; preds = %bb12.preheader.i
  %umax.i = tail call i64 @llvm.umax.i64(i64 %0, i64 1) #14
  %2 = add nsw i64 %umax.i, -1
  %xtraiter = and i64 %umax.i, 3
  %3 = icmp ult i64 %2, 3
  br i1 %3, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit.loopexit5.unr-lcssa, label %bb13.preheader.i.new

bb13.preheader.i.new:                             ; preds = %bb13.preheader.i
  %unroll_iter = and i64 %umax.i, 4611686018427387900
  br label %bb13.i

bb4.preheader.i:                                  ; preds = %start
  br i1 %1, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit, label %bb5.i.preheader

bb5.i.preheader:                                  ; preds = %bb4.preheader.i
  %4 = add nsw i64 %0, -1
  %xtraiter6 = and i64 %0, 3
  %lcmp.mod7.not = icmp eq i64 %xtraiter6, 0
  br i1 %lcmp.mod7.not, label %bb5.i.prol.loopexit, label %bb5.i.prol

bb5.i.prol:                                       ; preds = %bb5.i.preheader, %bb5.i.prol
  %i.020.i.prol = phi i64 [ %5, %bb5.i.prol ], [ %0, %bb5.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb5.i.prol ], [ %xtraiter6, %bb5.i.preheader ]
  %5 = add i64 %i.020.i.prol, -1
  %6 = getelementptr inbounds i32, i32* %dest, i64 %5
  %7 = getelementptr inbounds i32, i32* %src, i64 %5
  %8 = load atomic i32, i32* %7 unordered, align 4
  store atomic i32 %8, i32* %6 unordered, align 4
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb5.i.prol.loopexit, label %bb5.i.prol, !llvm.loop !61

bb5.i.prol.loopexit:                              ; preds = %bb5.i.prol, %bb5.i.preheader
  %i.020.i.unr = phi i64 [ %0, %bb5.i.preheader ], [ %5, %bb5.i.prol ]
  %9 = icmp ult i64 %4, 3
  br i1 %9, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit, label %bb5.i

bb5.i:                                            ; preds = %bb5.i.prol.loopexit, %bb5.i
  %i.020.i = phi i64 [ %22, %bb5.i ], [ %i.020.i.unr, %bb5.i.prol.loopexit ]
  %10 = add i64 %i.020.i, -1
  %11 = getelementptr inbounds i32, i32* %dest, i64 %10
  %12 = getelementptr inbounds i32, i32* %src, i64 %10
  %13 = load atomic i32, i32* %12 unordered, align 4
  store atomic i32 %13, i32* %11 unordered, align 4
  %14 = add i64 %i.020.i, -2
  %15 = getelementptr inbounds i32, i32* %dest, i64 %14
  %16 = getelementptr inbounds i32, i32* %src, i64 %14
  %17 = load atomic i32, i32* %16 unordered, align 4
  store atomic i32 %17, i32* %15 unordered, align 4
  %18 = add i64 %i.020.i, -3
  %19 = getelementptr inbounds i32, i32* %dest, i64 %18
  %20 = getelementptr inbounds i32, i32* %src, i64 %18
  %21 = load atomic i32, i32* %20 unordered, align 4
  store atomic i32 %21, i32* %19 unordered, align 4
  %22 = add i64 %i.020.i, -4
  %23 = getelementptr inbounds i32, i32* %dest, i64 %22
  %24 = getelementptr inbounds i32, i32* %src, i64 %22
  %25 = load atomic i32, i32* %24 unordered, align 4
  store atomic i32 %25, i32* %23 unordered, align 4
  %26 = icmp eq i64 %22, 0
  br i1 %26, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit, label %bb5.i

bb13.i:                                           ; preds = %bb13.i, %bb13.preheader.i.new
  %i1.019.i = phi i64 [ 0, %bb13.preheader.i.new ], [ %42, %bb13.i ]
  %niter = phi i64 [ %unroll_iter, %bb13.preheader.i.new ], [ %niter.nsub.3, %bb13.i ]
  %27 = getelementptr inbounds i32, i32* %dest, i64 %i1.019.i
  %28 = getelementptr inbounds i32, i32* %src, i64 %i1.019.i
  %29 = load atomic i32, i32* %28 unordered, align 4
  store atomic i32 %29, i32* %27 unordered, align 4
  %30 = or i64 %i1.019.i, 1
  %31 = getelementptr inbounds i32, i32* %dest, i64 %30
  %32 = getelementptr inbounds i32, i32* %src, i64 %30
  %33 = load atomic i32, i32* %32 unordered, align 4
  store atomic i32 %33, i32* %31 unordered, align 4
  %34 = or i64 %i1.019.i, 2
  %35 = getelementptr inbounds i32, i32* %dest, i64 %34
  %36 = getelementptr inbounds i32, i32* %src, i64 %34
  %37 = load atomic i32, i32* %36 unordered, align 4
  store atomic i32 %37, i32* %35 unordered, align 4
  %38 = or i64 %i1.019.i, 3
  %39 = getelementptr inbounds i32, i32* %dest, i64 %38
  %40 = getelementptr inbounds i32, i32* %src, i64 %38
  %41 = load atomic i32, i32* %40 unordered, align 4
  store atomic i32 %41, i32* %39 unordered, align 4
  %42 = add nuw nsw i64 %i1.019.i, 4
  %niter.nsub.3 = add i64 %niter, -4
  %niter.ncmp.3 = icmp eq i64 %niter.nsub.3, 0
  br i1 %niter.ncmp.3, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit.loopexit5.unr-lcssa, label %bb13.i

_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit.loopexit5.unr-lcssa: ; preds = %bb13.i, %bb13.preheader.i
  %i1.019.i.unr = phi i64 [ 0, %bb13.preheader.i ], [ %42, %bb13.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit, label %bb13.i.epil

bb13.i.epil:                                      ; preds = %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit.loopexit5.unr-lcssa, %bb13.i.epil
  %i1.019.i.epil = phi i64 [ %46, %bb13.i.epil ], [ %i1.019.i.unr, %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit.loopexit5.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb13.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit.loopexit5.unr-lcssa ]
  %43 = getelementptr inbounds i32, i32* %dest, i64 %i1.019.i.epil
  %44 = getelementptr inbounds i32, i32* %src, i64 %i1.019.i.epil
  %45 = load atomic i32, i32* %44 unordered, align 4
  store atomic i32 %45, i32* %43 unordered, align 4
  %46 = add nuw nsw i64 %i1.019.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit, label %bb13.i.epil, !llvm.loop !62

_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit: ; preds = %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h860e17a7d2c8be79E.exit.loopexit5.unr-lcssa, %bb13.i.epil, %bb5.i.prol.loopexit, %bb5.i, %bb12.preheader.i, %bb4.preheader.i
  ret void
}

; compiler_builtins::mem::__llvm_memmove_element_unordered_atomic_8
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define hidden void @_ZN17compiler_builtins3mem41__llvm_memmove_element_unordered_atomic_817h63cf96f3c0205be0E(i64* %dest, i64* readonly %src, i64 %bytes) unnamed_addr #6 {
start:
  %0 = lshr i64 %bytes, 3
  %_7.i = icmp ult i64* %src, %dest
  %1 = icmp eq i64 %bytes, 0
  br i1 %_7.i, label %bb4.preheader.i, label %bb12.preheader.i

bb12.preheader.i:                                 ; preds = %start
  br i1 %1, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit, label %bb13.preheader.i

bb13.preheader.i:                                 ; preds = %bb12.preheader.i
  %umax.i = tail call i64 @llvm.umax.i64(i64 %0, i64 1) #14
  %2 = add nsw i64 %umax.i, -1
  %xtraiter = and i64 %umax.i, 3
  %3 = icmp ult i64 %2, 3
  br i1 %3, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit.loopexit5.unr-lcssa, label %bb13.preheader.i.new

bb13.preheader.i.new:                             ; preds = %bb13.preheader.i
  %unroll_iter = and i64 %umax.i, 2305843009213693948
  br label %bb13.i

bb4.preheader.i:                                  ; preds = %start
  br i1 %1, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit, label %bb5.i.preheader

bb5.i.preheader:                                  ; preds = %bb4.preheader.i
  %4 = add nsw i64 %0, -1
  %xtraiter6 = and i64 %0, 3
  %lcmp.mod7.not = icmp eq i64 %xtraiter6, 0
  br i1 %lcmp.mod7.not, label %bb5.i.prol.loopexit, label %bb5.i.prol

bb5.i.prol:                                       ; preds = %bb5.i.preheader, %bb5.i.prol
  %i.020.i.prol = phi i64 [ %5, %bb5.i.prol ], [ %0, %bb5.i.preheader ]
  %prol.iter = phi i64 [ %prol.iter.sub, %bb5.i.prol ], [ %xtraiter6, %bb5.i.preheader ]
  %5 = add i64 %i.020.i.prol, -1
  %6 = getelementptr inbounds i64, i64* %dest, i64 %5
  %7 = getelementptr inbounds i64, i64* %src, i64 %5
  %8 = load atomic i64, i64* %7 unordered, align 8
  store atomic i64 %8, i64* %6 unordered, align 8
  %prol.iter.sub = add i64 %prol.iter, -1
  %prol.iter.cmp.not = icmp eq i64 %prol.iter.sub, 0
  br i1 %prol.iter.cmp.not, label %bb5.i.prol.loopexit, label %bb5.i.prol, !llvm.loop !63

bb5.i.prol.loopexit:                              ; preds = %bb5.i.prol, %bb5.i.preheader
  %i.020.i.unr = phi i64 [ %0, %bb5.i.preheader ], [ %5, %bb5.i.prol ]
  %9 = icmp ult i64 %4, 3
  br i1 %9, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit, label %bb5.i

bb5.i:                                            ; preds = %bb5.i.prol.loopexit, %bb5.i
  %i.020.i = phi i64 [ %22, %bb5.i ], [ %i.020.i.unr, %bb5.i.prol.loopexit ]
  %10 = add i64 %i.020.i, -1
  %11 = getelementptr inbounds i64, i64* %dest, i64 %10
  %12 = getelementptr inbounds i64, i64* %src, i64 %10
  %13 = load atomic i64, i64* %12 unordered, align 8
  store atomic i64 %13, i64* %11 unordered, align 8
  %14 = add i64 %i.020.i, -2
  %15 = getelementptr inbounds i64, i64* %dest, i64 %14
  %16 = getelementptr inbounds i64, i64* %src, i64 %14
  %17 = load atomic i64, i64* %16 unordered, align 8
  store atomic i64 %17, i64* %15 unordered, align 8
  %18 = add i64 %i.020.i, -3
  %19 = getelementptr inbounds i64, i64* %dest, i64 %18
  %20 = getelementptr inbounds i64, i64* %src, i64 %18
  %21 = load atomic i64, i64* %20 unordered, align 8
  store atomic i64 %21, i64* %19 unordered, align 8
  %22 = add i64 %i.020.i, -4
  %23 = getelementptr inbounds i64, i64* %dest, i64 %22
  %24 = getelementptr inbounds i64, i64* %src, i64 %22
  %25 = load atomic i64, i64* %24 unordered, align 8
  store atomic i64 %25, i64* %23 unordered, align 8
  %26 = icmp eq i64 %22, 0
  br i1 %26, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit, label %bb5.i

bb13.i:                                           ; preds = %bb13.i, %bb13.preheader.i.new
  %i1.019.i = phi i64 [ 0, %bb13.preheader.i.new ], [ %42, %bb13.i ]
  %niter = phi i64 [ %unroll_iter, %bb13.preheader.i.new ], [ %niter.nsub.3, %bb13.i ]
  %27 = getelementptr inbounds i64, i64* %dest, i64 %i1.019.i
  %28 = getelementptr inbounds i64, i64* %src, i64 %i1.019.i
  %29 = load atomic i64, i64* %28 unordered, align 8
  store atomic i64 %29, i64* %27 unordered, align 8
  %30 = or i64 %i1.019.i, 1
  %31 = getelementptr inbounds i64, i64* %dest, i64 %30
  %32 = getelementptr inbounds i64, i64* %src, i64 %30
  %33 = load atomic i64, i64* %32 unordered, align 8
  store atomic i64 %33, i64* %31 unordered, align 8
  %34 = or i64 %i1.019.i, 2
  %35 = getelementptr inbounds i64, i64* %dest, i64 %34
  %36 = getelementptr inbounds i64, i64* %src, i64 %34
  %37 = load atomic i64, i64* %36 unordered, align 8
  store atomic i64 %37, i64* %35 unordered, align 8
  %38 = or i64 %i1.019.i, 3
  %39 = getelementptr inbounds i64, i64* %dest, i64 %38
  %40 = getelementptr inbounds i64, i64* %src, i64 %38
  %41 = load atomic i64, i64* %40 unordered, align 8
  store atomic i64 %41, i64* %39 unordered, align 8
  %42 = add nuw nsw i64 %i1.019.i, 4
  %niter.nsub.3 = add i64 %niter, -4
  %niter.ncmp.3 = icmp eq i64 %niter.nsub.3, 0
  br i1 %niter.ncmp.3, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit.loopexit5.unr-lcssa, label %bb13.i

_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit.loopexit5.unr-lcssa: ; preds = %bb13.i, %bb13.preheader.i
  %i1.019.i.unr = phi i64 [ 0, %bb13.preheader.i ], [ %42, %bb13.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit, label %bb13.i.epil

bb13.i.epil:                                      ; preds = %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit.loopexit5.unr-lcssa, %bb13.i.epil
  %i1.019.i.epil = phi i64 [ %46, %bb13.i.epil ], [ %i1.019.i.unr, %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit.loopexit5.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb13.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit.loopexit5.unr-lcssa ]
  %43 = getelementptr inbounds i64, i64* %dest, i64 %i1.019.i.epil
  %44 = getelementptr inbounds i64, i64* %src, i64 %i1.019.i.epil
  %45 = load atomic i64, i64* %44 unordered, align 8
  store atomic i64 %45, i64* %43 unordered, align 8
  %46 = add nuw nsw i64 %i1.019.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit, label %bb13.i.epil, !llvm.loop !64

_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit: ; preds = %_ZN17compiler_builtins3mem32memmove_element_unordered_atomic17h4886d101bfc91c7eE.exit.loopexit5.unr-lcssa, %bb13.i.epil, %bb5.i.prol.loopexit, %bb5.i, %bb12.preheader.i, %bb4.preheader.i
  ret void
}

; compiler_builtins::mem::__llvm_memset_element_unordered_atomic_1
; Function Attrs: nofree norecurse nosync nounwind nonlazybind uwtable writeonly
define hidden void @_ZN17compiler_builtins3mem40__llvm_memset_element_unordered_atomic_117h39777d350d5ffff0E(i8* nocapture %s, i8 zeroext %c, i64 %bytes) unnamed_addr #9 {
start:
  %_199.not.i = icmp eq i64 %bytes, 0
  br i1 %_199.not.i, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit, label %bb12.i.preheader

bb12.i.preheader:                                 ; preds = %start
  %0 = add i64 %bytes, -1
  %xtraiter = and i64 %bytes, 7
  %1 = icmp ult i64 %0, 7
  br i1 %1, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit.loopexit.unr-lcssa, label %bb12.i.preheader.new

bb12.i.preheader.new:                             ; preds = %bb12.i.preheader
  %unroll_iter = and i64 %bytes, -8
  br label %bb12.i

bb12.i:                                           ; preds = %bb12.i, %bb12.i.preheader.new
  %i1.010.i = phi i64 [ 0, %bb12.i.preheader.new ], [ %17, %bb12.i ]
  %niter = phi i64 [ %unroll_iter, %bb12.i.preheader.new ], [ %niter.nsub.7, %bb12.i ]
  %2 = getelementptr inbounds i8, i8* %s, i64 %i1.010.i
  store atomic i8 %c, i8* %2 unordered, align 1
  %3 = or i64 %i1.010.i, 1
  %4 = getelementptr inbounds i8, i8* %s, i64 %3
  store atomic i8 %c, i8* %4 unordered, align 1
  %5 = or i64 %i1.010.i, 2
  %6 = getelementptr inbounds i8, i8* %s, i64 %5
  store atomic i8 %c, i8* %6 unordered, align 1
  %7 = or i64 %i1.010.i, 3
  %8 = getelementptr inbounds i8, i8* %s, i64 %7
  store atomic i8 %c, i8* %8 unordered, align 1
  %9 = or i64 %i1.010.i, 4
  %10 = getelementptr inbounds i8, i8* %s, i64 %9
  store atomic i8 %c, i8* %10 unordered, align 1
  %11 = or i64 %i1.010.i, 5
  %12 = getelementptr inbounds i8, i8* %s, i64 %11
  store atomic i8 %c, i8* %12 unordered, align 1
  %13 = or i64 %i1.010.i, 6
  %14 = getelementptr inbounds i8, i8* %s, i64 %13
  store atomic i8 %c, i8* %14 unordered, align 1
  %15 = or i64 %i1.010.i, 7
  %16 = getelementptr inbounds i8, i8* %s, i64 %15
  store atomic i8 %c, i8* %16 unordered, align 1
  %17 = add nuw i64 %i1.010.i, 8
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit.loopexit.unr-lcssa, label %bb12.i

_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit.loopexit.unr-lcssa: ; preds = %bb12.i, %bb12.i.preheader
  %i1.010.i.unr = phi i64 [ 0, %bb12.i.preheader ], [ %17, %bb12.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit, label %bb12.i.epil

bb12.i.epil:                                      ; preds = %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit.loopexit.unr-lcssa, %bb12.i.epil
  %i1.010.i.epil = phi i64 [ %19, %bb12.i.epil ], [ %i1.010.i.unr, %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb12.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit.loopexit.unr-lcssa ]
  %18 = getelementptr inbounds i8, i8* %s, i64 %i1.010.i.epil
  store atomic i8 %c, i8* %18 unordered, align 1
  %19 = add nuw i64 %i1.010.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit, label %bb12.i.epil, !llvm.loop !65

_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit: ; preds = %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17h39193fdf154b4950E.exit.loopexit.unr-lcssa, %bb12.i.epil, %start
  ret void
}

; compiler_builtins::mem::__llvm_memset_element_unordered_atomic_2
; Function Attrs: nofree nosync nounwind nonlazybind uwtable writeonly
define hidden void @_ZN17compiler_builtins3mem40__llvm_memset_element_unordered_atomic_217hdf7be15bf572b0d2E(i16* nocapture %s, i8 zeroext %c, i64 %bytes) unnamed_addr #10 {
start:
  %0 = zext i8 %c to i16
  %1 = mul nuw i16 %0, 257
  %_1911.not.i = icmp eq i64 %bytes, 0
  br i1 %_1911.not.i, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit, label %bb12.preheader.i

bb12.preheader.i:                                 ; preds = %start
  %2 = lshr i64 %bytes, 1
  %umax.i = tail call i64 @llvm.umax.i64(i64 %2, i64 1) #14
  %3 = add nsw i64 %umax.i, -1
  %xtraiter = and i64 %umax.i, 7
  %4 = icmp ult i64 %3, 7
  br i1 %4, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit.loopexit.unr-lcssa, label %bb12.preheader.i.new

bb12.preheader.i.new:                             ; preds = %bb12.preheader.i
  %unroll_iter = and i64 %umax.i, 9223372036854775800
  br label %bb12.i

bb12.i:                                           ; preds = %bb12.i, %bb12.preheader.i.new
  %i1.012.i = phi i64 [ 0, %bb12.preheader.i.new ], [ %20, %bb12.i ]
  %niter = phi i64 [ %unroll_iter, %bb12.preheader.i.new ], [ %niter.nsub.7, %bb12.i ]
  %5 = getelementptr inbounds i16, i16* %s, i64 %i1.012.i
  store atomic i16 %1, i16* %5 unordered, align 2
  %6 = or i64 %i1.012.i, 1
  %7 = getelementptr inbounds i16, i16* %s, i64 %6
  store atomic i16 %1, i16* %7 unordered, align 2
  %8 = or i64 %i1.012.i, 2
  %9 = getelementptr inbounds i16, i16* %s, i64 %8
  store atomic i16 %1, i16* %9 unordered, align 2
  %10 = or i64 %i1.012.i, 3
  %11 = getelementptr inbounds i16, i16* %s, i64 %10
  store atomic i16 %1, i16* %11 unordered, align 2
  %12 = or i64 %i1.012.i, 4
  %13 = getelementptr inbounds i16, i16* %s, i64 %12
  store atomic i16 %1, i16* %13 unordered, align 2
  %14 = or i64 %i1.012.i, 5
  %15 = getelementptr inbounds i16, i16* %s, i64 %14
  store atomic i16 %1, i16* %15 unordered, align 2
  %16 = or i64 %i1.012.i, 6
  %17 = getelementptr inbounds i16, i16* %s, i64 %16
  store atomic i16 %1, i16* %17 unordered, align 2
  %18 = or i64 %i1.012.i, 7
  %19 = getelementptr inbounds i16, i16* %s, i64 %18
  store atomic i16 %1, i16* %19 unordered, align 2
  %20 = add nuw nsw i64 %i1.012.i, 8
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit.loopexit.unr-lcssa, label %bb12.i

_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit.loopexit.unr-lcssa: ; preds = %bb12.i, %bb12.preheader.i
  %i1.012.i.unr = phi i64 [ 0, %bb12.preheader.i ], [ %20, %bb12.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit, label %bb12.i.epil

bb12.i.epil:                                      ; preds = %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit.loopexit.unr-lcssa, %bb12.i.epil
  %i1.012.i.epil = phi i64 [ %22, %bb12.i.epil ], [ %i1.012.i.unr, %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb12.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit.loopexit.unr-lcssa ]
  %21 = getelementptr inbounds i16, i16* %s, i64 %i1.012.i.epil
  store atomic i16 %1, i16* %21 unordered, align 2
  %22 = add nuw nsw i64 %i1.012.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit, label %bb12.i.epil, !llvm.loop !66

_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit: ; preds = %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd9008e3d8e808ba2E.exit.loopexit.unr-lcssa, %bb12.i.epil, %start
  ret void
}

; compiler_builtins::mem::__llvm_memset_element_unordered_atomic_4
; Function Attrs: nofree nosync nounwind nonlazybind uwtable writeonly
define hidden void @_ZN17compiler_builtins3mem40__llvm_memset_element_unordered_atomic_417h57132921a27349d7E(i32* nocapture %s, i8 zeroext %c, i64 %bytes) unnamed_addr #10 {
start:
  %0 = zext i8 %c to i32
  %1 = mul nuw i32 %0, 16843009
  %_1912.not.i = icmp eq i64 %bytes, 0
  br i1 %_1912.not.i, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit, label %bb12.preheader.i

bb12.preheader.i:                                 ; preds = %start
  %2 = lshr i64 %bytes, 2
  %umax.i = tail call i64 @llvm.umax.i64(i64 %2, i64 1) #14
  %3 = add nsw i64 %umax.i, -1
  %xtraiter = and i64 %umax.i, 7
  %4 = icmp ult i64 %3, 7
  br i1 %4, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit.loopexit.unr-lcssa, label %bb12.preheader.i.new

bb12.preheader.i.new:                             ; preds = %bb12.preheader.i
  %unroll_iter = and i64 %umax.i, 4611686018427387896
  br label %bb12.i

bb12.i:                                           ; preds = %bb12.i, %bb12.preheader.i.new
  %i1.013.i = phi i64 [ 0, %bb12.preheader.i.new ], [ %20, %bb12.i ]
  %niter = phi i64 [ %unroll_iter, %bb12.preheader.i.new ], [ %niter.nsub.7, %bb12.i ]
  %5 = getelementptr inbounds i32, i32* %s, i64 %i1.013.i
  store atomic i32 %1, i32* %5 unordered, align 4
  %6 = or i64 %i1.013.i, 1
  %7 = getelementptr inbounds i32, i32* %s, i64 %6
  store atomic i32 %1, i32* %7 unordered, align 4
  %8 = or i64 %i1.013.i, 2
  %9 = getelementptr inbounds i32, i32* %s, i64 %8
  store atomic i32 %1, i32* %9 unordered, align 4
  %10 = or i64 %i1.013.i, 3
  %11 = getelementptr inbounds i32, i32* %s, i64 %10
  store atomic i32 %1, i32* %11 unordered, align 4
  %12 = or i64 %i1.013.i, 4
  %13 = getelementptr inbounds i32, i32* %s, i64 %12
  store atomic i32 %1, i32* %13 unordered, align 4
  %14 = or i64 %i1.013.i, 5
  %15 = getelementptr inbounds i32, i32* %s, i64 %14
  store atomic i32 %1, i32* %15 unordered, align 4
  %16 = or i64 %i1.013.i, 6
  %17 = getelementptr inbounds i32, i32* %s, i64 %16
  store atomic i32 %1, i32* %17 unordered, align 4
  %18 = or i64 %i1.013.i, 7
  %19 = getelementptr inbounds i32, i32* %s, i64 %18
  store atomic i32 %1, i32* %19 unordered, align 4
  %20 = add nuw nsw i64 %i1.013.i, 8
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit.loopexit.unr-lcssa, label %bb12.i

_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit.loopexit.unr-lcssa: ; preds = %bb12.i, %bb12.preheader.i
  %i1.013.i.unr = phi i64 [ 0, %bb12.preheader.i ], [ %20, %bb12.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit, label %bb12.i.epil

bb12.i.epil:                                      ; preds = %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit.loopexit.unr-lcssa, %bb12.i.epil
  %i1.013.i.epil = phi i64 [ %22, %bb12.i.epil ], [ %i1.013.i.unr, %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb12.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit.loopexit.unr-lcssa ]
  %21 = getelementptr inbounds i32, i32* %s, i64 %i1.013.i.epil
  store atomic i32 %1, i32* %21 unordered, align 4
  %22 = add nuw nsw i64 %i1.013.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit, label %bb12.i.epil, !llvm.loop !67

_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit: ; preds = %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17hd0dc245f62516aabE.exit.loopexit.unr-lcssa, %bb12.i.epil, %start
  ret void
}

; compiler_builtins::mem::__llvm_memset_element_unordered_atomic_8
; Function Attrs: nofree nosync nounwind nonlazybind uwtable writeonly
define hidden void @_ZN17compiler_builtins3mem40__llvm_memset_element_unordered_atomic_817h2ffc9b593d29c4adE(i64* nocapture %s, i8 zeroext %c, i64 %bytes) unnamed_addr #10 {
start:
  %0 = zext i8 %c to i64
  %1 = mul nuw i64 %0, 72340172838076673
  %_1912.not.i = icmp eq i64 %bytes, 0
  br i1 %_1912.not.i, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit, label %bb12.preheader.i

bb12.preheader.i:                                 ; preds = %start
  %2 = lshr i64 %bytes, 3
  %umax.i = tail call i64 @llvm.umax.i64(i64 %2, i64 1) #14
  %3 = add nsw i64 %umax.i, -1
  %xtraiter = and i64 %umax.i, 7
  %4 = icmp ult i64 %3, 7
  br i1 %4, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit.loopexit.unr-lcssa, label %bb12.preheader.i.new

bb12.preheader.i.new:                             ; preds = %bb12.preheader.i
  %unroll_iter = and i64 %umax.i, 2305843009213693944
  br label %bb12.i

bb12.i:                                           ; preds = %bb12.i, %bb12.preheader.i.new
  %i1.013.i = phi i64 [ 0, %bb12.preheader.i.new ], [ %20, %bb12.i ]
  %niter = phi i64 [ %unroll_iter, %bb12.preheader.i.new ], [ %niter.nsub.7, %bb12.i ]
  %5 = getelementptr inbounds i64, i64* %s, i64 %i1.013.i
  store atomic i64 %1, i64* %5 unordered, align 8
  %6 = or i64 %i1.013.i, 1
  %7 = getelementptr inbounds i64, i64* %s, i64 %6
  store atomic i64 %1, i64* %7 unordered, align 8
  %8 = or i64 %i1.013.i, 2
  %9 = getelementptr inbounds i64, i64* %s, i64 %8
  store atomic i64 %1, i64* %9 unordered, align 8
  %10 = or i64 %i1.013.i, 3
  %11 = getelementptr inbounds i64, i64* %s, i64 %10
  store atomic i64 %1, i64* %11 unordered, align 8
  %12 = or i64 %i1.013.i, 4
  %13 = getelementptr inbounds i64, i64* %s, i64 %12
  store atomic i64 %1, i64* %13 unordered, align 8
  %14 = or i64 %i1.013.i, 5
  %15 = getelementptr inbounds i64, i64* %s, i64 %14
  store atomic i64 %1, i64* %15 unordered, align 8
  %16 = or i64 %i1.013.i, 6
  %17 = getelementptr inbounds i64, i64* %s, i64 %16
  store atomic i64 %1, i64* %17 unordered, align 8
  %18 = or i64 %i1.013.i, 7
  %19 = getelementptr inbounds i64, i64* %s, i64 %18
  store atomic i64 %1, i64* %19 unordered, align 8
  %20 = add nuw nsw i64 %i1.013.i, 8
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit.loopexit.unr-lcssa, label %bb12.i

_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit.loopexit.unr-lcssa: ; preds = %bb12.i, %bb12.preheader.i
  %i1.013.i.unr = phi i64 [ 0, %bb12.preheader.i ], [ %20, %bb12.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit, label %bb12.i.epil

bb12.i.epil:                                      ; preds = %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit.loopexit.unr-lcssa, %bb12.i.epil
  %i1.013.i.epil = phi i64 [ %22, %bb12.i.epil ], [ %i1.013.i.unr, %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb12.i.epil ], [ %xtraiter, %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit.loopexit.unr-lcssa ]
  %21 = getelementptr inbounds i64, i64* %s, i64 %i1.013.i.epil
  store atomic i64 %1, i64* %21 unordered, align 8
  %22 = add nuw nsw i64 %i1.013.i.epil, 1
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit, label %bb12.i.epil, !llvm.loop !68

_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit: ; preds = %_ZN17compiler_builtins3mem31memset_element_unordered_atomic17ha989166d3b0c959cE.exit.loopexit.unr-lcssa, %bb12.i.epil, %start
  ret void
}

; Function Attrs: inaccessiblememonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.assume(i1 noundef) #11

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i8, i1 } @llvm.sadd.with.overflow.i8(i8, i8) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i16, i1 } @llvm.sadd.with.overflow.i16(i16, i16) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i32, i1 } @llvm.sadd.with.overflow.i32(i32, i32) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i64, i1 } @llvm.sadd.with.overflow.i64(i64, i64) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i128, i1 } @llvm.sadd.with.overflow.i128(i128, i128) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i8 @llvm.ctlz.i8(i8, i1 immarg) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i8 @llvm.fshl.i8(i8, i8, i8) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i8, i1 } @llvm.uadd.with.overflow.i8(i8, i8) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i16 @llvm.ctlz.i16(i16, i1 immarg) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i16 @llvm.fshl.i16(i16, i16, i16) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i16, i1 } @llvm.uadd.with.overflow.i16(i16, i16) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i32 @llvm.ctlz.i32(i32, i1 immarg) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i32 @llvm.fshl.i32(i32, i32, i32) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i32, i1 } @llvm.uadd.with.overflow.i32(i32, i32) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i64 @llvm.ctlz.i64(i64, i1 immarg) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i64 @llvm.fshl.i64(i64, i64, i64) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i64, i1 } @llvm.uadd.with.overflow.i64(i64, i64) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i128 @llvm.ctlz.i128(i128, i1 immarg) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i128 @llvm.fshl.i128(i128, i128, i128) #12

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i128, i1 } @llvm.uadd.with.overflow.i128(i128, i128) #12

; Function Attrs: nofree nosync nounwind readnone speculatable willreturn
declare i32 @llvm.abs.i32(i32, i1 immarg) #13

; Function Attrs: nofree nosync nounwind readnone speculatable willreturn
declare i128 @llvm.abs.i128(i128, i1 immarg) #13

; Function Attrs: nofree nosync nounwind readnone speculatable willreturn
declare i64 @llvm.abs.i64(i64, i1 immarg) #13

; Function Attrs: nofree nosync nounwind readnone speculatable willreturn
declare i8 @llvm.abs.i8(i8, i1 immarg) #13

; Function Attrs: nofree nosync nounwind readnone speculatable willreturn
declare i16 @llvm.abs.i16(i16, i1 immarg) #13

; Function Attrs: nofree nosync nounwind readnone speculatable willreturn
declare i64 @llvm.umax.i64(i64, i64) #13

attributes #0 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { mustprogress nofree nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #3 = { nofree nosync nounwind nonlazybind readnone uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #4 = { mustprogress nofree nosync nounwind nonlazybind uwtable willreturn writeonly "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #5 = { mustprogress nofree nosync nounwind nonlazybind uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #6 = { nofree nosync nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #7 = { mustprogress nonlazybind uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #8 = { nofree norecurse nosync nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #9 = { nofree norecurse nosync nounwind nonlazybind uwtable writeonly "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #10 = { nofree nosync nounwind nonlazybind uwtable writeonly "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #11 = { inaccessiblememonly mustprogress nofree nosync nounwind willreturn }
attributes #12 = { mustprogress nofree nosync nounwind readnone speculatable willreturn }
attributes #13 = { nofree nosync nounwind readnone speculatable willreturn }
attributes #14 = { nounwind }
attributes #15 = { nounwind readnone willreturn }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
!2 = !{i32 199125, i32 199162, i32 199198}
!3 = !{i32 199709, i32 199724, i32 199761, i32 199798, i32 199823, i32 199848, i32 199885}
!4 = !{i32 200843, i32 200878, i32 200914}
!5 = !{i32 0, i32 33}
!6 = !{i64 0, i64 65}
!7 = !{i128 0, i128 129}
!8 = !{!9}
!9 = distinct !{!9, !10, !"_ZN17compiler_builtins3int6addsub16__rust_i128_addo17h75ee4e7337c6fca1E: argument 0"}
!10 = distinct !{!10, !"_ZN17compiler_builtins3int6addsub16__rust_i128_addo17h75ee4e7337c6fca1E"}
!11 = !{!12}
!12 = distinct !{!12, !13, !"_ZN17compiler_builtins3int6addsub16__rust_u128_addo17hf70a1bb418b353caE: argument 0"}
!13 = distinct !{!13, !"_ZN17compiler_builtins3int6addsub16__rust_u128_addo17hf70a1bb418b353caE"}
!14 = !{!15}
!15 = distinct !{!15, !16, !"_ZN17compiler_builtins3int6addsub16__rust_i128_subo17h3cedbf7b2f801579E: argument 0"}
!16 = distinct !{!16, !"_ZN17compiler_builtins3int6addsub16__rust_i128_subo17h3cedbf7b2f801579E"}
!17 = !{!18}
!18 = distinct !{!18, !19, !"_ZN17compiler_builtins3int6addsub16__rust_u128_subo17h358927d3f5806756E: argument 0"}
!19 = distinct !{!19, !"_ZN17compiler_builtins3int6addsub16__rust_u128_subo17h358927d3f5806756E"}
!20 = !{!21}
!21 = distinct !{!21, !22, !"_ZN17compiler_builtins3int3mul9__mulosi417headb65407c5eaf4dE: %oflow"}
!22 = distinct !{!22, !"_ZN17compiler_builtins3int3mul9__mulosi417headb65407c5eaf4dE"}
!23 = !{!24}
!24 = distinct !{!24, !25, !"_ZN17compiler_builtins3int3mul9__mulodi417h270d5205ef1acb48E: %oflow"}
!25 = distinct !{!25, !"_ZN17compiler_builtins3int3mul9__mulodi417h270d5205ef1acb48E"}
!26 = !{!27}
!27 = distinct !{!27, !28, !"_ZN17compiler_builtins3int3mul9__muloti417h9010b4dc2a8b3d17E: %oflow"}
!28 = distinct !{!28, !"_ZN17compiler_builtins3int3mul9__muloti417h9010b4dc2a8b3d17E"}
!29 = !{!30}
!30 = distinct !{!30, !31, !"_ZN17compiler_builtins3int3mul16__rust_i128_mulo17heeb8c85e3487da40E: argument 0"}
!31 = distinct !{!31, !"_ZN17compiler_builtins3int3mul16__rust_i128_mulo17heeb8c85e3487da40E"}
!32 = !{!33}
!33 = distinct !{!33, !34, !"_ZN17compiler_builtins3int3mul16__rust_u128_mulo17hb5426c52cc38e8b6E: argument 0"}
!34 = distinct !{!34, !"_ZN17compiler_builtins3int3mul16__rust_u128_mulo17hb5426c52cc38e8b6E"}
!35 = distinct !{!35, !36}
!36 = !{!"llvm.loop.unroll.disable"}
!37 = distinct !{!37, !36}
!38 = distinct !{!38, !36}
!39 = distinct !{!39, !36}
!40 = distinct !{!40, !36}
!41 = distinct !{!41, !36}
!42 = distinct !{!42, !36}
!43 = distinct !{!43, !36}
!44 = !{i32 91952}
!45 = distinct !{!45, !36}
!46 = distinct !{!46, !36}
!47 = distinct !{!47, !36}
!48 = distinct !{!48, !36}
!49 = distinct !{!49, !36}
!50 = distinct !{!50, !36}
!51 = !{i8 0, i8 9}
!52 = !{i16 0, i16 17}
!53 = distinct !{!53, !36}
!54 = distinct !{!54, !36}
!55 = distinct !{!55, !36}
!56 = distinct !{!56, !36}
!57 = distinct !{!57, !36}
!58 = distinct !{!58, !36}
!59 = distinct !{!59, !36}
!60 = distinct !{!60, !36}
!61 = distinct !{!61, !36}
!62 = distinct !{!62, !36}
!63 = distinct !{!63, !36}
!64 = distinct !{!64, !36}
!65 = distinct !{!65, !36}
!66 = distinct !{!66, !36}
!67 = distinct !{!67, !36}
!68 = distinct !{!68, !36}
