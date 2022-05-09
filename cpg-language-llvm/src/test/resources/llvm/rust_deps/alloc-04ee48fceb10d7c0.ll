; ModuleID = 'alloc.8e9e94fc-cgu.0'
source_filename = "alloc.8e9e94fc-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"string::Drain" = type { %"string::String"*, i64, i64, { i8*, i8* } }
%"string::String" = type { %"vec::Vec<u8>" }
%"vec::Vec<u8>" = type { { i8*, i64 }, i64 }
%"borrow::Cow<str>" = type { i64, [3 x i64] }
%"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>" = type { {}*, [2 x i64] }
%"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>" = type { i64, [2 x i64] }
%"core::panic::location::Location" = type { { [0 x i8]*, i64 }, i32, i32 }
%"core::fmt::Arguments" = type { { [0 x { [0 x i8]*, i64 }]*, i64 }, { i64*, i64 }, { [0 x { i8*, i64* }]*, i64 } }
%"collections::btree::mem::replace::PanicGuard" = type {}
%"core::fmt::Formatter" = type { { i64, i64 }, { i64, i64 }, { {}*, [3 x i64]* }, i32, i32, i8, [7 x i8] }
%"core::fmt::Error" = type {}
%"core::option::Option<core::str::lossy::Utf8LossyChunk>" = type { {}*, [3 x i64] }
%"core::str::lossy::Utf8Lossy" = type { [0 x i8] }
%"core::result::Result<string::String, string::FromUtf16Error>" = type { {}*, [2 x i64] }
%"core::result::Result<(), collections::TryReserveError>" = type { i64, [2 x i64] }
%"string::String::retain::SetLenOnDrop" = type { %"string::String"*, i64, i64 }
%"string::FromUtf8Error" = type { %"vec::Vec<u8>", %"core::str::error::Utf8Error" }
%"core::str::error::Utf8Error" = type { i64, { i8, i8 }, [6 x i8] }
%"string::FromUtf16Error" = type { {} }
%"core::str::pattern::StrSearcher" = type { { [0 x i8]*, i64 }, { [0 x i8]*, i64 }, %"core::str::pattern::StrSearcherImpl" }
%"core::str::pattern::StrSearcherImpl" = type { i64, [8 x i64] }
%"core::fmt::builders::DebugTuple" = type { %"core::fmt::Formatter"*, i64, i8, i8, [6 x i8] }
%"alloc::Global" = type {}
%"core::fmt::builders::DebugStruct" = type { %"core::fmt::Formatter"*, i8, i8, [6 x i8] }
%"core::fmt::builders::DebugList" = type { %"core::fmt::builders::DebugInner" }
%"core::fmt::builders::DebugInner" = type { %"core::fmt::Formatter"*, i8, i8, [6 x i8] }

@alloc2983 = private unnamed_addr constant <{ [116 x i8] }> <{ [116 x i8] c"/home/alwagner/.rustup/toolchains/nightly-x86_64-unknown-linux-gnu/lib/rustlib/src/rust/library/alloc/src/raw_vec.rs" }>, align 1
@alloc3022 = private unnamed_addr constant <{ [17 x i8] }> <{ [17 x i8] c"capacity overflow" }>, align 1
@alloc2984 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [116 x i8] }>, <{ [116 x i8] }>* @alloc2983, i32 0, i32 0, i32 0), [16 x i8] c"t\00\00\00\00\00\00\00\06\02\00\00\05\00\00\00" }>, align 8
@alloc2998 = private unnamed_addr constant <{ [114 x i8] }> <{ [114 x i8] c"/home/alwagner/.rustup/toolchains/nightly-x86_64-unknown-linux-gnu/lib/rustlib/src/rust/library/alloc/src/alloc.rs" }>, align 1
@alloc2904 = private unnamed_addr constant <{ [0 x i8] }> zeroinitializer, align 1
@alloc2995 = private unnamed_addr constant <{ [17 x i8] }> <{ [17 x i8] c"allocation failed" }>, align 1
@alloc2997 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [114 x i8] }>, <{ [114 x i8] }>* @alloc2998, i32 0, i32 0, i32 0), [16 x i8] c"r\00\00\00\00\00\00\00v\01\00\00\09\00\00\00" }>, align 8
@alloc2797 = private unnamed_addr constant <{ [21 x i8] }> <{ [21 x i8] c"memory allocation of " }>, align 1
@alloc2799 = private unnamed_addr constant <{ [13 x i8] }> <{ [13 x i8] c" bytes failed" }>, align 1
@alloc2798 = private unnamed_addr constant <{ i8*, [8 x i8], i8*, [8 x i8] }> <{ i8* getelementptr inbounds (<{ [21 x i8] }>, <{ [21 x i8] }>* @alloc2797, i32 0, i32 0, i32 0), [8 x i8] c"\15\00\00\00\00\00\00\00", i8* getelementptr inbounds (<{ [13 x i8] }>, <{ [13 x i8] }>* @alloc2799, i32 0, i32 0, i32 0), [8 x i8] c"\0D\00\00\00\00\00\00\00" }>, align 8
@alloc2999 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [114 x i8] }>, <{ [114 x i8] }>* @alloc2998, i32 0, i32 0, i32 0), [16 x i8] c"r\00\00\00\00\00\00\00\92\01\00\00\09\00\00\00" }>, align 8
@alloc3008 = private unnamed_addr constant <{ [24 x i8] }> <{ [24 x i8] c"memory allocation failed" }>, align 1
@alloc3011 = private unnamed_addr constant <{ [46 x i8] }> <{ [46 x i8] c" because the memory allocator returned a error" }>, align 1
@alloc3012 = private unnamed_addr constant <{ [64 x i8] }> <{ [64 x i8] c" because the computed capacity exceeded the collection's maximum" }>, align 1
@alloc3013 = private unnamed_addr constant <{ [51 x i8] }> <{ [51 x i8] c"a formatting trait implementation returned an error" }>, align 1
@alloc3014 = private unnamed_addr constant <{ [112 x i8] }> <{ [112 x i8] c"/home/alwagner/.rustup/toolchains/nightly-x86_64-unknown-linux-gnu/lib/rustlib/src/rust/library/alloc/src/fmt.rs" }>, align 1
@alloc3015 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [112 x i8] }>, <{ [112 x i8] }>* @alloc3014, i32 0, i32 0, i32 0), [16 x i8] c"p\00\00\00\00\00\00\00U\02\00\00\1C\00\00\00" }>, align 8
@alloc3027 = private unnamed_addr constant <{ [114 x i8] }> <{ [114 x i8] c"/home/alwagner/.rustup/toolchains/nightly-x86_64-unknown-linux-gnu/lib/rustlib/src/rust/library/alloc/src/slice.rs" }>, align 1
@alloc3024 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [114 x i8] }>, <{ [114 x i8] }>* @alloc3027, i32 0, i32 0, i32 0), [16 x i8] c"r\00\00\00\00\00\00\00.\02\00\002\00\00\00" }>, align 8
@alloc3039 = private unnamed_addr constant <{ [112 x i8] }> <{ [112 x i8] c"/home/alwagner/.rustup/toolchains/nightly-x86_64-unknown-linux-gnu/lib/rustlib/src/rust/library/alloc/src/str.rs" }>, align 1
@alloc3034 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [112 x i8] }>, <{ [112 x i8] }>* @alloc3039, i32 0, i32 0, i32 0), [16 x i8] c"p\00\00\00\00\00\00\00\9D\01\00\00<\00\00\00" }>, align 8
@alloc3036 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [112 x i8] }>, <{ [112 x i8] }>* @alloc3039, i32 0, i32 0, i32 0), [16 x i8] c"p\00\00\00\00\00\00\00\9E\01\00\000\00\00\00" }>, align 8
@alloc3043 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"\EF\BF\BD" }>, align 1
@alloc3046 = private unnamed_addr constant <{ [36 x i8] }> <{ [36 x i8] c"invalid utf-16: lone surrogate found" }>, align 1
@alloc3049 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"Drain" }>, align 1
@vtable.0 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7142ffe4e73b9138E" to i8*), [16 x i8] c"\10\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ [0 x i8]*, i64 }*, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hf82658bc94a10742E" to i8*), [0 x i8] zeroinitializer }>, align 8
@0 = private unnamed_addr constant <{ [16 x i8] }> <{ [16 x i8] c"\01\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00" }>, align 8
@alloc2909 = private unnamed_addr constant <{ [22 x i8] }> <{ [22 x i8] c"swap_remove index (is " }>, align 1
@alloc2925 = private unnamed_addr constant <{ [22 x i8] }> <{ [22 x i8] c") should be < len (is " }>, align 1
@alloc2933 = private unnamed_addr constant <{ [1 x i8] }> <{ [1 x i8] c")" }>, align 1
@alloc2910 = private unnamed_addr constant <{ i8*, [8 x i8], i8*, [8 x i8], i8*, [8 x i8] }> <{ i8* getelementptr inbounds (<{ [22 x i8] }>, <{ [22 x i8] }>* @alloc2909, i32 0, i32 0, i32 0), [8 x i8] c"\16\00\00\00\00\00\00\00", i8* getelementptr inbounds (<{ [22 x i8] }>, <{ [22 x i8] }>* @alloc2925, i32 0, i32 0, i32 0), [8 x i8] c"\16\00\00\00\00\00\00\00", i8* getelementptr inbounds (<{ [1 x i8] }>, <{ [1 x i8] }>* @alloc2933, i32 0, i32 0, i32 0), [8 x i8] c"\01\00\00\00\00\00\00\00" }>, align 8
@alloc3059 = private unnamed_addr constant <{ [116 x i8] }> <{ [116 x i8] c"/home/alwagner/.rustup/toolchains/nightly-x86_64-unknown-linux-gnu/lib/rustlib/src/rust/library/alloc/src/vec/mod.rs" }>, align 1
@alloc3054 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [116 x i8] }>, <{ [116 x i8] }>* @alloc3059, i32 0, i32 0, i32 0), [16 x i8] c"t\00\00\00\00\00\00\00\14\05\00\00\0D\00\00\00" }>, align 8
@alloc2916 = private unnamed_addr constant <{ [20 x i8] }> <{ [20 x i8] c"insertion index (is " }>, align 1
@alloc2932 = private unnamed_addr constant <{ [23 x i8] }> <{ [23 x i8] c") should be <= len (is " }>, align 1
@alloc2917 = private unnamed_addr constant <{ i8*, [8 x i8], i8*, [8 x i8], i8*, [8 x i8] }> <{ i8* getelementptr inbounds (<{ [20 x i8] }>, <{ [20 x i8] }>* @alloc2916, i32 0, i32 0, i32 0), [8 x i8] c"\14\00\00\00\00\00\00\00", i8* getelementptr inbounds (<{ [23 x i8] }>, <{ [23 x i8] }>* @alloc2932, i32 0, i32 0, i32 0), [8 x i8] c"\17\00\00\00\00\00\00\00", i8* getelementptr inbounds (<{ [1 x i8] }>, <{ [1 x i8] }>* @alloc2933, i32 0, i32 0, i32 0), [8 x i8] c"\01\00\00\00\00\00\00\00" }>, align 8
@alloc3056 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [116 x i8] }>, <{ [116 x i8] }>* @alloc3059, i32 0, i32 0, i32 0), [16 x i8] c"t\00\00\00\00\00\00\00=\05\00\00\0D\00\00\00" }>, align 8
@alloc2923 = private unnamed_addr constant <{ [18 x i8] }> <{ [18 x i8] c"removal index (is " }>, align 1
@alloc2924 = private unnamed_addr constant <{ i8*, [8 x i8], i8*, [8 x i8], i8*, [8 x i8] }> <{ i8* getelementptr inbounds (<{ [18 x i8] }>, <{ [18 x i8] }>* @alloc2923, i32 0, i32 0, i32 0), [8 x i8] c"\12\00\00\00\00\00\00\00", i8* getelementptr inbounds (<{ [22 x i8] }>, <{ [22 x i8] }>* @alloc2925, i32 0, i32 0, i32 0), [8 x i8] c"\16\00\00\00\00\00\00\00", i8* getelementptr inbounds (<{ [1 x i8] }>, <{ [1 x i8] }>* @alloc2933, i32 0, i32 0, i32 0), [8 x i8] c"\01\00\00\00\00\00\00\00" }>, align 8
@alloc2930 = private unnamed_addr constant <{ [21 x i8] }> <{ [21 x i8] c"`at` split index (is " }>, align 1
@alloc2931 = private unnamed_addr constant <{ i8*, [8 x i8], i8*, [8 x i8], i8*, [8 x i8] }> <{ i8* getelementptr inbounds (<{ [21 x i8] }>, <{ [21 x i8] }>* @alloc2930, i32 0, i32 0, i32 0), [8 x i8] c"\15\00\00\00\00\00\00\00", i8* getelementptr inbounds (<{ [23 x i8] }>, <{ [23 x i8] }>* @alloc2932, i32 0, i32 0, i32 0), [8 x i8] c"\17\00\00\00\00\00\00\00", i8* getelementptr inbounds (<{ [1 x i8] }>, <{ [1 x i8] }>* @alloc2933, i32 0, i32 0, i32 0), [8 x i8] c"\01\00\00\00\00\00\00\00" }>, align 8
@alloc3060 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [116 x i8] }>, <{ [116 x i8] }>* @alloc3059, i32 0, i32 0, i32 0), [16 x i8] c"t\00\00\00\00\00\00\00\8C\07\00\00\0D\00\00\00" }>, align 8
@alloc3061 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"Global" }>, align 1
@alloc3062 = private unnamed_addr constant <{ [15 x i8] }> <{ [15 x i8] c"TryReserveError" }>, align 1
@alloc3063 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"kind" }>, align 1
@vtable.1 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7142ffe4e73b9138E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i64, i64 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h33d66f6602a37396E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc3067 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"AllocError" }>, align 1
@alloc3068 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"layout" }>, align 1
@vtable.2 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7142ffe4e73b9138E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i64, i64 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h9b27245c2844029aE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc3072 = private unnamed_addr constant <{ [14 x i8] }> <{ [14 x i8] c"non_exhaustive" }>, align 1
@vtable.3 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7142ffe4e73b9138E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({}**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h7075983059913f2aE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc3076 = private unnamed_addr constant <{ [16 x i8] }> <{ [16 x i8] c"CapacityOverflow" }>, align 1
@alloc3077 = private unnamed_addr constant <{ [13 x i8] }> <{ [13 x i8] c"FromUtf8Error" }>, align 1
@alloc3078 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"bytes" }>, align 1
@vtable.4 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7142ffe4e73b9138E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"vec::Vec<u8>"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h528391e318defd79E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc3082 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"error" }>, align 1
@vtable.5 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7142ffe4e73b9138E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"core::str::error::Utf8Error"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h1e750e3acfebf1ccE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc3086 = private unnamed_addr constant <{ [14 x i8] }> <{ [14 x i8] c"FromUtf16Error" }>, align 1
@vtable.6 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7142ffe4e73b9138E" to i8*), [16 x i8] c"\00\00\00\00\00\00\00\00\01\00\00\00\00\00\00\00", i8* bitcast (i1 (%"core::fmt::Error"*, %"core::fmt::Formatter"*)* @"_ZN53_$LT$core..fmt..Error$u20$as$u20$core..fmt..Debug$GT$3fmt17haa251479953ac187E" to i8*), [0 x i8] zeroinitializer }>, align 8
@vtable.7 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7142ffe4e73b9138E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i8**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h2e266a9c28fc8eaeE" to i8*), [0 x i8] zeroinitializer }>, align 8
@vtable.8 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, i8*, i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7142ffe4e73b9138E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"string::String"**, [0 x i8]*, i64)* @"_ZN50_$LT$$RF$mut$u20$W$u20$as$u20$core..fmt..Write$GT$9write_str17h36a7fc6f3a923613E" to i8*), i8* bitcast (i1 (%"string::String"**, i32)* @"_ZN50_$LT$$RF$mut$u20$W$u20$as$u20$core..fmt..Write$GT$10write_char17h4f4dd9a3c5cb9730E" to i8*), i8* bitcast (i1 (%"string::String"**, %"core::fmt::Arguments"*)* @"_ZN50_$LT$$RF$mut$u20$W$u20$as$u20$core..fmt..Write$GT$9write_fmt17hd2f7f4bf7299fe41E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc3103 = private unnamed_addr constant <{ [2 x i8] }> <{ [2 x i8] c"()" }>, align 1

@"_ZN81_$LT$alloc..string..Drain$u20$as$u20$core..convert..AsRef$LT$$u5b$u8$u5d$$GT$$GT$6as_ref17h4e7aa0167280231bE" = unnamed_addr alias { [0 x i8]*, i64 } (%"string::Drain"*), { [0 x i8]*, i64 } (%"string::Drain"*)* @"_ZN72_$LT$alloc..string..Drain$u20$as$u20$core..convert..AsRef$LT$str$GT$$GT$6as_ref17h3e9de094f2b2621bE"

; core::ops::function::FnOnce::call_once
; Function Attrs: inlinehint noreturn nounwind nonlazybind uwtable
define internal fastcc void @_ZN4core3ops8function6FnOnce9call_once17h5e9bfb1e9387b989E(i64 %0, i64 %1) unnamed_addr #0 {
start:
; call alloc::alloc::handle_alloc_error::rt_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error8rt_error17h18e04995076b8eccE(i64 %0, i64 %1) #25
  unreachable
}

; core::ptr::drop_in_place<&u8>
; Function Attrs: inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define internal void @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7142ffe4e73b9138E"(i8** nocapture readnone %_1) unnamed_addr #1 {
start:
  ret void
}

; core::ptr::drop_in_place<alloc::borrow::Cow<str>>
; Function Attrs: nounwind nonlazybind uwtable
define internal fastcc void @"_ZN4core3ptr50drop_in_place$LT$alloc..borrow..Cow$LT$str$GT$$GT$17hdbb082751bfa4a80E"(%"borrow::Cow<str>"* nocapture readonly %_1) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %0 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %_1, i64 0, i32 0
  %_2 = load i64, i64* %0, align 8, !range !2
  %1 = icmp eq i64 %_2, 0
  br i1 %1, label %bb1, label %bb2

bb1:                                              ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i", %bb2, %start
  ret void

bb2:                                              ; preds = %start
  %2 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %_1, i64 0, i32 1
  %.idx.i.i = bitcast [3 x i64]* %2 to i8**
  %.idx.val.i.i = load i8*, i8** %.idx.i.i, align 8
  %3 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %_1, i64 0, i32 1, i64 1
  %.idx4.val.i.i = load i64, i64* %3, align 8
  %_4.i.i.i.i.i = icmp eq i64 %.idx4.val.i.i, 0
  %.not.i.i.i.i = icmp eq i8* %.idx.val.i.i, null
  %or.cond.i.i.i.i = select i1 %_4.i.i.i.i.i, i1 true, i1 %.not.i.i.i.i
  br i1 %or.cond.i.i.i.i, label %bb1, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i": ; preds = %bb2
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i, i64 %.idx4.val.i.i, i64 1) #26
  br label %bb1
}

; alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
; Function Attrs: cold nonlazybind uwtable
define internal fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nocapture align 8 dereferenceable(16) %slf, i64 %len, i64 %additional) unnamed_addr #3 personality i32 (...)* @rust_eh_personality {
start:
  %_30.i = alloca %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", align 8
  %_28.i = alloca %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", align 8
  tail call void @llvm.experimental.noalias.scope.decl(metadata !3)
  %0 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %len, i64 %additional) #26
  %1 = extractvalue { i64, i1 } %0, 0
  %2 = extractvalue { i64, i1 } %0, 1
  br i1 %2, label %bb5.i, label %bb8.i

bb8.i:                                            ; preds = %start
  %3 = getelementptr { i8*, i64 }, { i8*, i64 }* %slf, i64 0, i32 1
  %_20.i = load i64, i64* %3, align 8, !alias.scope !3, !noalias !6
  %_19.i = shl i64 %_20.i, 1
  %4 = icmp ugt i64 %_19.i, %1
  %.0.sroa.speculated.i.i.i.i = select i1 %4, i64 %_19.i, i64 %1
  %5 = icmp ugt i64 %.0.sroa.speculated.i.i.i.i, 8
  %.0.sroa.speculated.i.i.i16.i = select i1 %5, i64 %.0.sroa.speculated.i.i.i.i, i64 8
  %6 = bitcast %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %6) #26, !noalias !8
  %7 = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %7) #26, !noalias !8
  %self.idx.i = getelementptr { i8*, i64 }, { i8*, i64 }* %slf, i64 0, i32 0
  %_4.i.i = icmp eq i64 %_20.i, 0
  br i1 %_4.i.i, label %bb5.i.i, label %bb6.i.i

bb6.i.i:                                          ; preds = %bb8.i
  %self.idx.val.i = load i8*, i8** %self.idx.i, align 8, !alias.scope !3, !noalias !6
  %_13.sroa.0.0..sroa_idx.i.i = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i to i8**
  store i8* %self.idx.val.i, i8** %_13.sroa.0.0..sroa_idx.i.i, align 8, !alias.scope !9, !noalias !8
  %8 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i, i64 0, i32 1, i64 0
  store i64 %_20.i, i64* %8, align 8, !alias.scope !9, !noalias !8
  %9 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i, i64 0, i32 1, i64 1
  store i64 1, i64* %9, align 8, !alias.scope !9, !noalias !8
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i"

bb5.i.i:                                          ; preds = %bb8.i
  %10 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i, i64 0, i32 0
  store {}* null, {}** %10, align 8, !alias.scope !9, !noalias !8
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i"

"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i": ; preds = %bb5.i.i, %bb6.i.i
; call alloc::raw_vec::finish_grow
  call fastcc void @_ZN5alloc7raw_vec11finish_grow17h290ed6d0c09e324dE(%"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* noalias nocapture nonnull dereferenceable(24) %_28.i, i64 %.0.sroa.speculated.i.i.i16.i, i64 1, %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* noalias nocapture nonnull dereferenceable(24) %_30.i) #26, !noalias !8
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %7) #26, !noalias !8
  %11 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i, i64 0, i32 0
  %_2.i.i = load i64, i64* %11, align 8, !range !2, !alias.scope !12, !noalias !15
  %switch.not.i17.i = icmp eq i64 %_2.i.i, 1
  br i1 %switch.not.i17.i, label %bb3.i, label %_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE.exit

bb3.i:                                            ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i"
  %12 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i, i64 0, i32 1, i64 0
  %e.0.i.i = load i64, i64* %12, align 8, !alias.scope !12, !noalias !15
  %13 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i, i64 0, i32 1, i64 1
  %e.1.i.i = load i64, i64* %13, align 8, !alias.scope !12, !noalias !15
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %6) #26, !noalias !8
  %14 = icmp eq i64 %e.1.i.i, 0
  br i1 %14, label %bb5.i, label %bb6.i

bb5.i:                                            ; preds = %start, %bb3.i
; call alloc::raw_vec::capacity_overflow
  tail call void @_ZN5alloc7raw_vec17capacity_overflow17hbdad83560505d524E() #25, !noalias !17
  unreachable

bb6.i:                                            ; preds = %bb3.i
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %e.0.i.i, i64 %e.1.i.i) #27, !noalias !17
  unreachable

_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE.exit: ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i"
  %15 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i, i64 0, i32 1
  %16 = bitcast [2 x i64]* %15 to i8**
  %v.0.i46.i = load i8*, i8** %16, align 8, !alias.scope !12, !noalias !15, !nonnull !20
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %6) #26, !noalias !8
  store i8* %v.0.i46.i, i8** %self.idx.i, align 8, !alias.scope !21, !noalias !6
  store i64 %.0.sroa.speculated.i.i.i16.i, i64* %3, align 8, !alias.scope !21, !noalias !6
  ret void
}

; alloc::raw_vec::RawVec<T,A>::reserve_for_push
; Function Attrs: noinline nonlazybind uwtable
define internal fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$16reserve_for_push17hb577e8edd788625cE"({ i8*, i64 }* noalias nocapture align 8 dereferenceable(16) %self, i64 %len) unnamed_addr #4 personality i32 (...)* @rust_eh_personality {
start:
  %_30.i = alloca %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", align 8
  %_28.i = alloca %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", align 8
  tail call void @llvm.experimental.noalias.scope.decl(metadata !24)
  %0 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %len, i64 1) #26
  %1 = extractvalue { i64, i1 } %0, 0
  %2 = extractvalue { i64, i1 } %0, 1
  br i1 %2, label %bb5.i, label %bb8.i

bb8.i:                                            ; preds = %start
  %3 = getelementptr { i8*, i64 }, { i8*, i64 }* %self, i64 0, i32 1
  %_20.i = load i64, i64* %3, align 8, !alias.scope !24, !noalias !27
  %_19.i = shl i64 %_20.i, 1
  %4 = icmp ugt i64 %_19.i, %1
  %.0.sroa.speculated.i.i.i.i = select i1 %4, i64 %_19.i, i64 %1
  %5 = icmp ugt i64 %.0.sroa.speculated.i.i.i.i, 8
  %.0.sroa.speculated.i.i.i16.i = select i1 %5, i64 %.0.sroa.speculated.i.i.i.i, i64 8
  %6 = bitcast %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %6) #26, !noalias !29
  %7 = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %7) #26, !noalias !29
  %self.idx.i = getelementptr { i8*, i64 }, { i8*, i64 }* %self, i64 0, i32 0
  %_4.i.i = icmp eq i64 %_20.i, 0
  br i1 %_4.i.i, label %bb5.i.i, label %bb6.i.i

bb6.i.i:                                          ; preds = %bb8.i
  %self.idx.val.i = load i8*, i8** %self.idx.i, align 8, !alias.scope !24, !noalias !27
  %_13.sroa.0.0..sroa_idx.i.i = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i to i8**
  store i8* %self.idx.val.i, i8** %_13.sroa.0.0..sroa_idx.i.i, align 8, !alias.scope !30, !noalias !29
  %8 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i, i64 0, i32 1, i64 0
  store i64 %_20.i, i64* %8, align 8, !alias.scope !30, !noalias !29
  %9 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i, i64 0, i32 1, i64 1
  store i64 1, i64* %9, align 8, !alias.scope !30, !noalias !29
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i"

bb5.i.i:                                          ; preds = %bb8.i
  %10 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i, i64 0, i32 0
  store {}* null, {}** %10, align 8, !alias.scope !30, !noalias !29
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i"

"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i": ; preds = %bb5.i.i, %bb6.i.i
; call alloc::raw_vec::finish_grow
  call fastcc void @_ZN5alloc7raw_vec11finish_grow17h290ed6d0c09e324dE(%"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* noalias nocapture nonnull dereferenceable(24) %_28.i, i64 %.0.sroa.speculated.i.i.i16.i, i64 1, %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* noalias nocapture nonnull dereferenceable(24) %_30.i) #26, !noalias !29
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %7) #26, !noalias !29
  %11 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i, i64 0, i32 0
  %_2.i.i = load i64, i64* %11, align 8, !range !2, !alias.scope !33, !noalias !36
  %switch.not.i17.i = icmp eq i64 %_2.i.i, 1
  br i1 %switch.not.i17.i, label %bb3.i, label %_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE.exit

bb3.i:                                            ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i"
  %12 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i, i64 0, i32 1, i64 0
  %e.0.i.i = load i64, i64* %12, align 8, !alias.scope !33, !noalias !36
  %13 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i, i64 0, i32 1, i64 1
  %e.1.i.i = load i64, i64* %13, align 8, !alias.scope !33, !noalias !36
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %6) #26, !noalias !29
  %14 = icmp eq i64 %e.1.i.i, 0
  br i1 %14, label %bb5.i, label %bb6.i

bb5.i:                                            ; preds = %start, %bb3.i
; call alloc::raw_vec::capacity_overflow
  tail call void @_ZN5alloc7raw_vec17capacity_overflow17hbdad83560505d524E() #25, !noalias !38
  unreachable

bb6.i:                                            ; preds = %bb3.i
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %e.0.i.i, i64 %e.1.i.i) #27, !noalias !38
  unreachable

_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE.exit: ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i"
  %15 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i, i64 0, i32 1
  %16 = bitcast [2 x i64]* %15 to i8**
  %v.0.i46.i = load i8*, i8** %16, align 8, !alias.scope !33, !noalias !36, !nonnull !20
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %6) #26, !noalias !29
  store i8* %v.0.i46.i, i8** %self.idx.i, align 8, !alias.scope !41, !noalias !27
  store i64 %.0.sroa.speculated.i.i.i16.i, i64* %3, align 8, !alias.scope !41, !noalias !27
  ret void
}

; alloc::raw_vec::finish_grow
; Function Attrs: noinline nounwind nonlazybind uwtable
define internal fastcc void @_ZN5alloc7raw_vec11finish_grow17h290ed6d0c09e324dE(%"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* noalias nocapture dereferenceable(24) %0, i64 %new_layout.0, i64 %new_layout.1, %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* noalias nocapture readonly dereferenceable(24) %current_memory) unnamed_addr #5 {
start:
  %1 = icmp eq i64 %new_layout.1, 0
  br i1 %1, label %bb5, label %bb10

bb5:                                              ; preds = %start
  %2 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %0, i64 0, i32 1, i64 0
  store i64 %new_layout.0, i64* %2, align 8, !alias.scope !44
  br label %bb24

bb10:                                             ; preds = %start
  %3 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %current_memory, i64 0, i32 0
  %4 = load {}*, {}** %3, align 8
  %.not = icmp eq {}* %4, null
  %5 = bitcast {}* %4 to i8*
  br i1 %.not, label %bb14, label %bb15

bb24:                                             ; preds = %bb1.i11, %bb3.i10, %bb5
  %.sink52 = phi i64 [ 0, %bb5 ], [ %new_layout.1, %bb1.i11 ], [ %new_layout.0, %bb3.i10 ]
  %.sink = phi i64 [ 1, %bb5 ], [ 1, %bb1.i11 ], [ 0, %bb3.i10 ]
  %6 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %0, i64 0, i32 1, i64 1
  store i64 %.sink52, i64* %6, align 8
  %7 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %0, i64 0, i32 0
  store i64 %.sink, i64* %7, align 8
  ret void

bb15:                                             ; preds = %bb10
  %8 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %current_memory, i64 0, i32 1, i64 0
  %9 = load i64, i64* %8, align 8
  %10 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %current_memory, i64 0, i32 1, i64 1
  %11 = load i64, i64* %10, align 8, !range !47
  %_29 = icmp eq i64 %11, %new_layout.1
  tail call void @llvm.assume(i1 %_29)
  %12 = icmp eq i64 %9, 0
  br i1 %12, label %bb2.i.i, label %bb7.i.i

bb2.i.i:                                          ; preds = %bb15
  %13 = icmp eq i64 %new_layout.0, 0
  br i1 %13, label %bb3.i.i.i, label %bb8.i.i.i

bb3.i.i.i:                                        ; preds = %bb2.i.i
  %_2.i.i.i.i = inttoptr i64 %new_layout.1 to i8*
  br label %bb21

bb8.i.i.i:                                        ; preds = %bb2.i.i
  %14 = tail call i8* @__rust_alloc(i64 %new_layout.0, i64 %new_layout.1) #26
  br label %bb21

bb7.i.i:                                          ; preds = %bb15
  %_21.i.i = icmp ule i64 %9, %new_layout.0
  tail call void @llvm.assume(i1 %_21.i.i) #26
  %15 = tail call i8* @__rust_realloc(i8* nonnull %5, i64 %9, i64 %new_layout.1, i64 %new_layout.0) #26
  %16 = icmp eq i8* %15, null
  br i1 %16, label %bb1.i11, label %bb3.i10

bb14:                                             ; preds = %bb10
  %17 = icmp eq i64 %new_layout.0, 0
  br i1 %17, label %bb3.i.i, label %bb8.i.i9

bb3.i.i:                                          ; preds = %bb14
  %_2.i.i.i = inttoptr i64 %new_layout.1 to i8*
  br label %bb21

bb8.i.i9:                                         ; preds = %bb14
  %18 = tail call i8* @__rust_alloc(i64 %new_layout.0, i64 %new_layout.1) #26
  br label %bb21

bb21:                                             ; preds = %bb3.i.i.i, %bb8.i.i.i, %bb8.i.i9, %bb3.i.i
  %.sroa.0.2.i.i.pn = phi i8* [ %_2.i.i.i, %bb3.i.i ], [ %18, %bb8.i.i9 ], [ %_2.i.i.i.i, %bb3.i.i.i ], [ %14, %bb8.i.i.i ]
  %19 = icmp eq i8* %.sroa.0.2.i.i.pn, null
  br i1 %19, label %bb1.i11, label %bb3.i10

bb3.i10:                                          ; preds = %bb7.i.i, %bb21
  %.sroa.0.2.i.i.pn50 = phi i8* [ %.sroa.0.2.i.i.pn, %bb21 ], [ %15, %bb7.i.i ]
  %20 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %0, i64 0, i32 1
  %21 = bitcast [2 x i64]* %20 to i8**
  store i8* %.sroa.0.2.i.i.pn50, i8** %21, align 8, !alias.scope !48, !noalias !51
  br label %bb24

bb1.i11:                                          ; preds = %bb7.i.i, %bb21
  %22 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %0, i64 0, i32 1, i64 0
  store i64 %new_layout.0, i64* %22, align 8, !alias.scope !48, !noalias !51
  br label %bb24
}

; alloc::raw_vec::capacity_overflow
; Function Attrs: noreturn nonlazybind uwtable
define void @_ZN5alloc7raw_vec17capacity_overflow17hbdad83560505d524E() unnamed_addr #6 {
start:
; call core::panicking::panic
  tail call void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [17 x i8] }>* @alloc3022 to [0 x i8]*), i64 17, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc2984 to %"core::panic::location::Location"*)) #25
  unreachable
}

; alloc::alloc::handle_alloc_error
; Function Attrs: cold noreturn nounwind nonlazybind uwtable
define void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %layout.0, i64 %layout.1) unnamed_addr #7 personality i32 (...)* @rust_eh_personality {
start:
; call core::intrinsics::const_eval_select
  tail call fastcc void @_ZN4core10intrinsics17const_eval_select17hee83e7c870cc171bE(i64 %layout.0, i64 %layout.1) #25
  unreachable
}

; alloc::alloc::handle_alloc_error::ct_error
; Function Attrs: noreturn nonlazybind uwtable
define void @_ZN5alloc5alloc18handle_alloc_error8ct_error17hdb564b5268bde118E(i64 %_1.0, i64 %_1.1) unnamed_addr #6 {
start:
; call core::panicking::panic
  tail call void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [17 x i8] }>* @alloc2995 to [0 x i8]*), i64 17, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc2997 to %"core::panic::location::Location"*)) #25
  unreachable
}

; alloc::alloc::handle_alloc_error::rt_error
; Function Attrs: noreturn nounwind nonlazybind uwtable
define void @_ZN5alloc5alloc18handle_alloc_error8rt_error17h18e04995076b8eccE(i64 %0, i64 %1) unnamed_addr #8 {
start:
  tail call void @__rust_alloc_error_handler(i64 %0, i64 %1) #27
  unreachable
}

; Function Attrs: noreturn nonlazybind uwtable
define void @__rdl_oom(i64 %0, i64 %_align) unnamed_addr #6 {
start:
  %_10 = alloca [1 x { i8*, i64* }], align 8
  %_3 = alloca %"core::fmt::Arguments", align 8
  %size = alloca i64, align 8
  store i64 %0, i64* %size, align 8
  %1 = bitcast %"core::fmt::Arguments"* %_3 to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %1)
  %2 = bitcast [1 x { i8*, i64* }]* %_10 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %2)
  %3 = bitcast [1 x { i8*, i64* }]* %_10 to i64**
  store i64* %size, i64** %3, align 8
  %4 = getelementptr inbounds [1 x { i8*, i64* }], [1 x { i8*, i64* }]* %_10, i64 0, i64 0, i32 1
  store i64* bitcast (i1 (i64*, %"core::fmt::Formatter"*)* @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E" to i64*), i64** %4, align 8
  %5 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 0, i32 0
  store [0 x { [0 x i8]*, i64 }]* bitcast (<{ i8*, [8 x i8], i8*, [8 x i8] }>* @alloc2798 to [0 x { [0 x i8]*, i64 }]*), [0 x { [0 x i8]*, i64 }]** %5, align 8, !alias.scope !53, !noalias !56
  %6 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 0, i32 1
  store i64 2, i64* %6, align 8, !alias.scope !53, !noalias !56
  %7 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 1, i32 0
  store i64* null, i64** %7, align 8, !alias.scope !53, !noalias !56
  %8 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 2, i32 0
  %9 = bitcast [0 x { i8*, i64* }]** %8 to [1 x { i8*, i64* }]**
  store [1 x { i8*, i64* }]* %_10, [1 x { i8*, i64* }]** %9, align 8, !alias.scope !53, !noalias !56
  %10 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 2, i32 1
  store i64 1, i64* %10, align 8, !alias.scope !53, !noalias !56
; call core::panicking::panic_fmt
  call void @_ZN4core9panicking9panic_fmt17he21f9dfe87a034c1E(%"core::fmt::Arguments"* noalias nocapture nonnull dereferenceable(48) %_3, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc2999 to %"core::panic::location::Location"*)) #25
  unreachable
}

; Function Attrs: noreturn nonlazybind uwtable
define void @__rg_oom(i64 %size, i64 %align) unnamed_addr #6 {
start:
  tail call void @rust_oom(i64 %size, i64 %align) #25
  unreachable
}

; <alloc::boxed::Box<str> as core::default::Default>::default
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define { [0 x i8]*, i64 } @"_ZN71_$LT$alloc..boxed..Box$LT$str$GT$$u20$as$u20$core..default..Default$GT$7default17h13d778046edf2034E"() unnamed_addr #9 personality i32 (...)* @rust_eh_personality {
start:
  ret { [0 x i8]*, i64 } { [0 x i8]* inttoptr (i64 1 to [0 x i8]*), i64 0 }
}

; <alloc::boxed::Box<str> as core::clone::Clone>::clone
; Function Attrs: nounwind nonlazybind uwtable
define { [0 x i8]*, i64 } @"_ZN67_$LT$alloc..boxed..Box$LT$str$GT$$u20$as$u20$core..clone..Clone$GT$5clone17hc28ad314f6ff08b8E"({ [0 x i8]*, i64 }* noalias nocapture readonly align 8 dereferenceable(16) %self) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %0 = getelementptr inbounds { [0 x i8]*, i64 }, { [0 x i8]*, i64 }* %self, i64 0, i32 0
  %_5.0 = load [0 x i8]*, [0 x i8]** %0, align 8, !nonnull !20
  %1 = getelementptr inbounds { [0 x i8]*, i64 }, { [0 x i8]*, i64 }* %self, i64 0, i32 1
  %_5.1 = load i64, i64* %1, align 8
  %2 = icmp eq i64 %_5.1, 0
  br i1 %2, label %"_ZN50_$LT$T$u20$as$u20$core..convert..Into$LT$U$GT$$GT$4into17hb8801d34722af82aE.exit", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i": ; preds = %start
  %3 = tail call i8* @__rust_alloc(i64 %_5.1, i64 1) #26, !noalias !59
  %4 = insertvalue { i8*, i64 } undef, i8* %3, 0
  %5 = icmp eq i8* %3, null
  br i1 %5, label %bb20.i.i.i.i.i, label %"_ZN50_$LT$T$u20$as$u20$core..convert..Into$LT$U$GT$$GT$4into17hb8801d34722af82aE.exit"

bb20.i.i.i.i.i:                                   ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %_5.1, i64 1) #27, !noalias !59
  unreachable

"_ZN50_$LT$T$u20$as$u20$core..convert..Into$LT$U$GT$$GT$4into17hb8801d34722af82aE.exit": ; preds = %start, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
  %.pn.i.i.i.i.i = phi { i8*, i64 } [ %4, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %start ]
  %.fca.0.extract.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i, 0
  %6 = getelementptr [0 x i8], [0 x i8]* %_5.0, i64 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %.fca.0.extract.i.i, i8* nonnull align 1 %6, i64 %_5.1, i1 false) #26
  %7 = icmp ne i8* %.fca.0.extract.i.i, null
  tail call void @llvm.assume(i1 %7) #26
  %8 = bitcast i8* %.fca.0.extract.i.i to [0 x i8]*
  %9 = insertvalue { [0 x i8]*, i64 } undef, [0 x i8]* %8, 0
  %10 = insertvalue { [0 x i8]*, i64 } %9, i64 %_5.1, 1
  ret { [0 x i8]*, i64 } %10
}

; <alloc::borrow::Cow<str> as core::ops::arith::AddAssign<&str>>::add_assign
; Function Attrs: nonlazybind uwtable
define void @"_ZN92_$LT$alloc..borrow..Cow$LT$str$GT$$u20$as$u20$core..ops..arith..AddAssign$LT$$RF$str$GT$$GT$10add_assign17h375c6f79c611ff34E"(%"borrow::Cow<str>"* noalias align 8 dereferenceable(32) %self, [0 x i8]* noalias nonnull readonly align 1 %rhs.0, i64 %rhs.1) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
start:
  %s = alloca %"string::String", align 8
  %0 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %self, i64 0, i32 0
  %_2.i = load i64, i64* %0, align 8, !range !2, !alias.scope !64
  %switch.not.i = icmp eq i64 %_2.i, 1
  %1 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %self, i64 0, i32 1, i64 1
  %2 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %self, i64 0, i32 1, i64 2
  %.sroa.0.0.in.in.i = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %self, i64 0, i32 1
  %.sroa.0.0.in.i = bitcast [3 x i64]* %.sroa.0.0.in.in.i to [0 x i8]**
  %.sroa.0.0.i = load [0 x i8]*, [0 x i8]** %.sroa.0.0.in.i, align 8, !alias.scope !64
  %.val.i = load i64, i64* %2, align 8, !alias.scope !64
  %.val2.i = load i64, i64* %1, align 8, !alias.scope !64
  %.sroa.3.0.i = select i1 %switch.not.i, i64 %.val.i, i64 %.val2.i
  %3 = icmp eq i64 %.sroa.3.0.i, 0
  %4 = getelementptr [0 x i8], [0 x i8]* %.sroa.0.0.i, i64 0, i64 0
  br i1 %3, label %bb3, label %bb4

bb4:                                              ; preds = %start
  %5 = icmp eq i64 %rhs.1, 0
  br i1 %5, label %bb16, label %bb6

bb3:                                              ; preds = %start
  %6 = icmp eq i64 %_2.i, 0
  br i1 %6, label %bb19, label %bb2.i

bb2.i:                                            ; preds = %bb3
  %_4.i.i.i.i.i.i = icmp eq i64 %.val2.i, 0
  %.not.i.i.i.i.i = icmp eq [0 x i8]* %.sroa.0.0.i, null
  %or.cond.i.i.i.i.i = select i1 %_4.i.i.i.i.i.i, i1 true, i1 %.not.i.i.i.i.i
  br i1 %or.cond.i.i.i.i.i, label %bb19, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i": ; preds = %bb2.i
  tail call void @__rust_dealloc(i8* nonnull %4, i64 %.val2.i, i64 1) #26
  br label %bb19

bb19:                                             ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i", %bb2.i, %bb3
  store i64 0, i64* %0, align 8
  store [0 x i8]* %rhs.0, [0 x i8]** %.sroa.0.0.in.i, align 8
  store i64 %rhs.1, i64* %1, align 8
  br label %bb16

bb17:                                             ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i", %bb22
  resume { i8*, i32 } %23

bb16:                                             ; preds = %bb4, %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit58, %bb19
  ret void

bb6:                                              ; preds = %bb4
  %7 = icmp eq i64 %_2.i, 0
  br i1 %7, label %bb7, label %"_ZN5alloc6borrow12Cow$LT$B$GT$6to_mut17hde4773405bc7d257E.exit"

bb7:                                              ; preds = %bb6
  %8 = bitcast %"string::String"* %s to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %8)
  %_15 = add i64 %.val2.i, %rhs.1
  tail call void @llvm.experimental.noalias.scope.decl(metadata !67)
  %9 = icmp eq i64 %_15, 0
  br i1 %9, label %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i": ; preds = %bb7
  %10 = tail call i8* @__rust_alloc(i64 %_15, i64 1) #26, !noalias !70
  %11 = insertvalue { i8*, i64 } undef, i8* %10, 0
  %12 = icmp eq i8* %10, null
  br i1 %12, label %bb20.i.i.i.i.i, label %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit

bb20.i.i.i.i.i:                                   ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %_15, i64 1) #27, !noalias !70
  unreachable

_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit: ; preds = %bb7, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
  %.pn.i.i.i.i.i = phi { i8*, i64 } [ %11, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %bb7 ]
  %_3.0.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i, 0
  %_2.sroa.0.0..sroa_idx.i = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0, i32 0
  store i8* %_3.0.i.i.i, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !67
  %_2.sroa.4.0..sroa_idx2.i = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0, i32 1
  store i64 %_15, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !67
  %_2.sroa.5.0..sroa_idx4.i = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 1
  store i64 0, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !67
  tail call void @llvm.experimental.noalias.scope.decl(metadata !75)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !78)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !81)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !84)
  %13 = icmp ult i64 %_15, %.val2.i
  br i1 %13, label %bb2.i.i.i.i.i.i, label %bb11

bb2.i.i.i.i.i.i:                                  ; preds = %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit
  %_4.i.i.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0
; invoke alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  invoke fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i, i64 0, i64 %.val2.i)
          to label %.noexc unwind label %bb22

.noexc:                                           ; preds = %bb2.i.i.i.i.i.i
  %self.idx.val.pre.i.i.i.i = load i64, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !87, !noalias !88
  %_2.idx.val.i.i.i.i.i.pre = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !91, !noalias !88
  br label %bb11

"_ZN5alloc6borrow12Cow$LT$B$GT$6to_mut17hde4773405bc7d257E.exit": ; preds = %bb6, %bb12.thread
  %_5.i.i.i.i.i49 = phi i64 [ %_5.i.i.i.i.i49.pre, %bb12.thread ], [ %.val.i, %bb6 ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !94)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !97)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !100)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !103)
  %self.idx.val.i.i.i.i.i.i51 = load i64, i64* %1, align 8, !alias.scope !106, !noalias !111
  %14 = sub i64 %self.idx.val.i.i.i.i.i.i51, %_5.i.i.i.i.i49
  %15 = icmp ult i64 %14, %rhs.1
  br i1 %15, label %bb2.i.i.i.i.i.i54, label %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit58

bb2.i.i.i.i.i.i54:                                ; preds = %"_ZN5alloc6borrow12Cow$LT$B$GT$6to_mut17hde4773405bc7d257E.exit"
  %_4.i.i.i.i.i52 = bitcast [3 x i64]* %.sroa.0.0.in.in.i to { i8*, i64 }*
; call alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  tail call fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i52, i64 %_5.i.i.i.i.i49, i64 %rhs.1), !noalias !111
  %self.idx.val.pre.i.i.i.i53 = load i64, i64* %2, align 8, !alias.scope !114, !noalias !111
  br label %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit58

_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit58: ; preds = %"_ZN5alloc6borrow12Cow$LT$B$GT$6to_mut17hde4773405bc7d257E.exit", %bb2.i.i.i.i.i.i54
  %self.idx.val.i.i.i.i55 = phi i64 [ %_5.i.i.i.i.i49, %"_ZN5alloc6borrow12Cow$LT$B$GT$6to_mut17hde4773405bc7d257E.exit" ], [ %self.idx.val.pre.i.i.i.i53, %bb2.i.i.i.i.i.i54 ]
  %16 = getelementptr [0 x i8], [0 x i8]* %rhs.0, i64 0, i64 0
  %_2.idx.i.i.i.i.i56 = bitcast [3 x i64]* %.sroa.0.0.in.in.i to i8**
  %_2.idx.val.i.i.i.i.i57 = load i8*, i8** %_2.idx.i.i.i.i.i56, align 8, !alias.scope !115, !noalias !111, !nonnull !20
  %17 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i57, i64 %self.idx.val.i.i.i.i55
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %17, i8* nonnull align 1 %16, i64 %rhs.1, i1 false) #26, !noalias !114
  %18 = add i64 %self.idx.val.i.i.i.i55, %rhs.1
  store i64 %18, i64* %2, align 8, !alias.scope !114, !noalias !111
  br label %bb16

bb11:                                             ; preds = %.noexc, %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit
  %_2.idx.val.i.i.i.i.i = phi i8* [ %_3.0.i.i.i, %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit ], [ %_2.idx.val.i.i.i.i.i.pre, %.noexc ]
  %self.idx.val.i.i.i.i = phi i64 [ 0, %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit ], [ %self.idx.val.pre.i.i.i.i, %.noexc ]
  %19 = getelementptr [0 x i8], [0 x i8]* %.sroa.0.0.i, i64 0, i64 0
  %20 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i, i64 %self.idx.val.i.i.i.i
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %20, i8* nonnull align 1 %19, i64 %.val2.i, i1 false) #26, !noalias !87
  %21 = add i64 %self.idx.val.i.i.i.i, %.val2.i
  store i64 %21, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !87, !noalias !88
  %_2.i59 = load i64, i64* %0, align 8, !range !2
  %22 = icmp eq i64 %_2.i59, 0
  br i1 %22, label %bb12.thread, label %bb2.i66

bb2.i66:                                          ; preds = %bb11
  %.idx.i.i.i60 = bitcast [3 x i64]* %.sroa.0.0.in.in.i to i8**
  %.idx.val.i.i.i61 = load i8*, i8** %.idx.i.i.i60, align 8
  %.idx4.val.i.i.i62 = load i64, i64* %1, align 8
  %_4.i.i.i.i.i.i63 = icmp eq i64 %.idx4.val.i.i.i62, 0
  %.not.i.i.i.i.i64 = icmp eq i8* %.idx.val.i.i.i61, null
  %or.cond.i.i.i.i.i65 = select i1 %_4.i.i.i.i.i.i63, i1 true, i1 %.not.i.i.i.i.i64
  br i1 %or.cond.i.i.i.i.i65, label %bb12.thread, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i67"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i67": ; preds = %bb2.i66
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i.i61, i64 %.idx4.val.i.i.i62, i64 1) #26
  br label %bb12.thread

bb12.thread:                                      ; preds = %bb11, %bb2.i66, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i67"
  store i64 1, i64* %0, align 8
  %_23.sroa.5.0..sroa_cast24 = bitcast [3 x i64]* %.sroa.0.0.in.in.i to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %_23.sroa.5.0..sroa_cast24, i8* noundef nonnull align 8 dereferenceable(24) %8, i64 24, i1 false)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %8)
  %_5.i.i.i.i.i49.pre = load i64, i64* %2, align 8, !alias.scope !118, !noalias !111
  br label %"_ZN5alloc6borrow12Cow$LT$B$GT$6to_mut17hde4773405bc7d257E.exit"

bb22:                                             ; preds = %bb2.i.i.i.i.i.i
  %23 = landingpad { i8*, i32 }
          cleanup
  %.idx.val.i.i = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8
  %.idx4.val.i.i = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8
  %_4.i.i.i.i.i69 = icmp eq i64 %.idx4.val.i.i, 0
  %.not.i.i.i.i = icmp eq i8* %.idx.val.i.i, null
  %or.cond.i.i.i.i = select i1 %_4.i.i.i.i.i69, i1 true, i1 %.not.i.i.i.i
  br i1 %or.cond.i.i.i.i, label %bb17, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i": ; preds = %bb22
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i, i64 %.idx4.val.i.i, i64 1) #26
  br label %bb17
}

; <alloc::borrow::Cow<str> as core::ops::arith::AddAssign>::add_assign
; Function Attrs: nonlazybind uwtable
define void @"_ZN77_$LT$alloc..borrow..Cow$LT$str$GT$$u20$as$u20$core..ops..arith..AddAssign$GT$10add_assign17ha6143d1699111674E"(%"borrow::Cow<str>"* noalias align 8 dereferenceable(32) %self, %"borrow::Cow<str>"* noalias nocapture readonly dereferenceable(32) %rhs) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
bb2:
  %s = alloca %"string::String", align 8
  %0 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %self, i64 0, i32 0
  %_2.i = load i64, i64* %0, align 8, !range !2, !alias.scope !119
  %switch.not.i = icmp eq i64 %_2.i, 1
  %1 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %self, i64 0, i32 1, i64 1
  %2 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %self, i64 0, i32 1, i64 2
  %.sroa.0.0.in.in.i = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %self, i64 0, i32 1
  %.sroa.0.0.in.i = bitcast [3 x i64]* %.sroa.0.0.in.in.i to [0 x i8]**
  %.sroa.0.0.i = load [0 x i8]*, [0 x i8]** %.sroa.0.0.in.i, align 8, !alias.scope !119
  %.val.i = load i64, i64* %2, align 8, !alias.scope !119
  %.val2.i = load i64, i64* %1, align 8, !alias.scope !119
  %.sroa.3.0.i = select i1 %switch.not.i, i64 %.val.i, i64 %.val2.i
  %3 = icmp eq i64 %.sroa.3.0.i, 0
  %4 = getelementptr [0 x i8], [0 x i8]* %.sroa.0.0.i, i64 0, i64 0
  br i1 %3, label %bb3, label %bb6

cleanup:                                          ; preds = %bb2.i.i.i.i.i.i79
  %5 = landingpad { i8*, i32 }
          cleanup
  br label %bb21

bb3:                                              ; preds = %bb2
  %6 = bitcast %"borrow::Cow<str>"* %rhs to i8*
  %7 = icmp eq i64 %_2.i, 0
  br i1 %7, label %bb23, label %bb2.i

bb2.i:                                            ; preds = %bb3
  %_4.i.i.i.i.i.i = icmp eq i64 %.val2.i, 0
  %.not.i.i.i.i.i = icmp eq [0 x i8]* %.sroa.0.0.i, null
  %or.cond.i.i.i.i.i = select i1 %_4.i.i.i.i.i.i, i1 true, i1 %.not.i.i.i.i.i
  br i1 %or.cond.i.i.i.i.i, label %bb23, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i": ; preds = %bb2.i
  tail call void @__rust_dealloc(i8* nonnull %4, i64 %.val2.i, i64 1) #26
  br label %bb23

bb23:                                             ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i", %bb2.i, %bb3
  %8 = bitcast %"borrow::Cow<str>"* %self to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(32) %8, i8* noundef nonnull align 8 dereferenceable(32) %6, i64 32, i1 false)
  br label %bb20

bb6:                                              ; preds = %bb2
  %9 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %rhs, i64 0, i32 0
  %_2.i37 = load i64, i64* %9, align 8, !range !2, !alias.scope !122
  %switch.not.i38 = icmp eq i64 %_2.i37, 1
  %10 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %rhs, i64 0, i32 1, i64 1
  %11 = getelementptr %"borrow::Cow<str>", %"borrow::Cow<str>"* %rhs, i64 0, i32 1, i64 2
  %.sroa.0.0.in.in.i39 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %rhs, i64 0, i32 1
  %.sroa.0.0.in.i40 = bitcast [3 x i64]* %.sroa.0.0.in.in.i39 to [0 x i8]**
  %.val.i42 = load i64, i64* %11, align 8, !alias.scope !122
  %.val2.i43 = load i64, i64* %10, align 8, !alias.scope !122
  %.sroa.3.0.i44 = select i1 %switch.not.i38, i64 %.val.i42, i64 %.val2.i43
  %12 = icmp eq i64 %.sroa.3.0.i44, 0
  br i1 %12, label %bb26, label %bb7

bb7:                                              ; preds = %bb6
  %13 = icmp eq i64 %_2.i, 0
  br i1 %13, label %bb11, label %bb16

bb11:                                             ; preds = %bb7
  %14 = bitcast %"string::String"* %s to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %14)
  %_16 = add i64 %.sroa.3.0.i44, %.val2.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !125)
  %15 = icmp eq i64 %_16, 0
  br i1 %15, label %bb12, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i": ; preds = %bb11
  %16 = tail call i8* @__rust_alloc(i64 %_16, i64 1) #26, !noalias !128
  %17 = insertvalue { i8*, i64 } undef, i8* %16, 0
  %18 = icmp eq i8* %16, null
  br i1 %18, label %bb20.i.i.i.i.i, label %bb12

bb20.i.i.i.i.i:                                   ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %_16, i64 1) #27, !noalias !128
  unreachable

bb12:                                             ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i", %bb11
  %.pn.i.i.i.i.i = phi { i8*, i64 } [ %17, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %bb11 ]
  %_3.0.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i, 0
  %_2.sroa.0.0..sroa_idx.i = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0, i32 0
  store i8* %_3.0.i.i.i, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !125
  %_2.sroa.4.0..sroa_idx2.i = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0, i32 1
  store i64 %_16, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !125
  %_2.sroa.5.0..sroa_idx4.i = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 1
  store i64 0, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !125
  tail call void @llvm.experimental.noalias.scope.decl(metadata !133)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !136)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !139)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !142)
  %19 = icmp ult i64 %_16, %.val2.i
  br i1 %19, label %bb2.i.i.i.i.i.i, label %bb13

bb2.i.i.i.i.i.i:                                  ; preds = %bb12
  %_4.i.i.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0
; invoke alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  invoke fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i, i64 0, i64 %.val2.i)
          to label %.noexc unwind label %bb27

.noexc:                                           ; preds = %bb2.i.i.i.i.i.i
  %self.idx.val.pre.i.i.i.i = load i64, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !145, !noalias !146
  %_2.idx.val.i.i.i.i.i.pre = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !149, !noalias !146
  br label %bb13

bb13:                                             ; preds = %.noexc, %bb12
  %_2.idx.val.i.i.i.i.i = phi i8* [ %_3.0.i.i.i, %bb12 ], [ %_2.idx.val.i.i.i.i.i.pre, %.noexc ]
  %self.idx.val.i.i.i.i = phi i64 [ 0, %bb12 ], [ %self.idx.val.pre.i.i.i.i, %.noexc ]
  %20 = getelementptr [0 x i8], [0 x i8]* %.sroa.0.0.i, i64 0, i64 0
  %21 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i, i64 %self.idx.val.i.i.i.i
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %21, i8* nonnull align 1 %20, i64 %.val2.i, i1 false) #26, !noalias !145
  %22 = add i64 %self.idx.val.i.i.i.i, %.val2.i
  store i64 %22, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !145, !noalias !146
  %_2.i55 = load i64, i64* %0, align 8, !range !2
  %23 = icmp eq i64 %_2.i55, 0
  br i1 %23, label %bb14.thread, label %bb2.i62

bb2.i62:                                          ; preds = %bb13
  %.idx.i.i.i56 = bitcast [3 x i64]* %.sroa.0.0.in.in.i to i8**
  %.idx.val.i.i.i57 = load i8*, i8** %.idx.i.i.i56, align 8
  %.idx4.val.i.i.i58 = load i64, i64* %1, align 8
  %_4.i.i.i.i.i.i59 = icmp eq i64 %.idx4.val.i.i.i58, 0
  %.not.i.i.i.i.i60 = icmp eq i8* %.idx.val.i.i.i57, null
  %or.cond.i.i.i.i.i61 = select i1 %_4.i.i.i.i.i.i59, i1 true, i1 %.not.i.i.i.i.i60
  br i1 %or.cond.i.i.i.i.i61, label %bb14.thread, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i63"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i63": ; preds = %bb2.i62
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i.i57, i64 %.idx4.val.i.i.i58, i64 1) #26
  br label %bb14.thread

bb14.thread:                                      ; preds = %bb13, %bb2.i62, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i63"
  store i64 1, i64* %0, align 8
  %_26.sroa.5.0..sroa_cast10 = bitcast [3 x i64]* %.sroa.0.0.in.in.i to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %_26.sroa.5.0..sroa_cast10, i8* noundef nonnull align 8 dereferenceable(24) %14, i64 24, i1 false)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %14)
  %_5.i.i.i.i.i74.pre = load i64, i64* %2, align 8, !alias.scope !152, !noalias !163
  br label %bb16

bb27:                                             ; preds = %bb2.i.i.i.i.i.i
  %24 = landingpad { i8*, i32 }
          cleanup
  %.idx.val.i.i = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8
  %.idx4.val.i.i = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8
  %_4.i.i.i.i.i65 = icmp eq i64 %.idx4.val.i.i, 0
  %.not.i.i.i.i = icmp eq i8* %.idx.val.i.i, null
  %or.cond.i.i.i.i = select i1 %_4.i.i.i.i.i65, i1 true, i1 %.not.i.i.i.i
  br i1 %or.cond.i.i.i.i, label %bb21, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i": ; preds = %bb27
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i, i64 %.idx4.val.i.i, i64 1) #26
  br label %bb21

bb16:                                             ; preds = %bb7, %bb14.thread
  %_5.i.i.i.i.i74 = phi i64 [ %_5.i.i.i.i.i74.pre, %bb14.thread ], [ %.val.i, %bb7 ]
  %_2.i66 = load i64, i64* %9, align 8, !range !2, !alias.scope !166
  %switch.not.i67 = icmp eq i64 %_2.i66, 1
  %.sroa.0.0.i70 = load [0 x i8]*, [0 x i8]** %.sroa.0.0.in.i40, align 8, !alias.scope !166
  %.val.i71 = load i64, i64* %11, align 8, !alias.scope !166
  %.val2.i72 = load i64, i64* %10, align 8, !alias.scope !166
  %.sroa.3.0.i73 = select i1 %switch.not.i67, i64 %.val.i71, i64 %.val2.i72
  tail call void @llvm.experimental.noalias.scope.decl(metadata !169)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !170)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !171)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !172)
  %self.idx.val.i.i.i.i.i.i76 = load i64, i64* %1, align 8, !alias.scope !173, !noalias !163
  %25 = sub i64 %self.idx.val.i.i.i.i.i.i76, %_5.i.i.i.i.i74
  %26 = icmp ult i64 %25, %.sroa.3.0.i73
  br i1 %26, label %bb2.i.i.i.i.i.i79, label %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit84

bb2.i.i.i.i.i.i79:                                ; preds = %bb16
  %_4.i.i.i.i.i77 = bitcast [3 x i64]* %.sroa.0.0.in.in.i to { i8*, i64 }*
; invoke alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  invoke fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i77, i64 %_5.i.i.i.i.i74, i64 %.sroa.3.0.i73)
          to label %.noexc83 unwind label %cleanup

.noexc83:                                         ; preds = %bb2.i.i.i.i.i.i79
  %self.idx.val.pre.i.i.i.i78 = load i64, i64* %2, align 8, !alias.scope !176, !noalias !163
  br label %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit84

_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit84: ; preds = %bb16, %.noexc83
  %self.idx.val.i.i.i.i80 = phi i64 [ %_5.i.i.i.i.i74, %bb16 ], [ %self.idx.val.pre.i.i.i.i78, %.noexc83 ]
  %27 = getelementptr [0 x i8], [0 x i8]* %.sroa.0.0.i70, i64 0, i64 0
  %_2.idx.i.i.i.i.i81 = bitcast [3 x i64]* %.sroa.0.0.in.in.i to i8**
  %_2.idx.val.i.i.i.i.i82 = load i8*, i8** %_2.idx.i.i.i.i.i81, align 8, !alias.scope !177, !noalias !163, !nonnull !20
  %28 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i82, i64 %self.idx.val.i.i.i.i80
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %28, i8* nonnull align 1 %27, i64 %.sroa.3.0.i73, i1 false) #26, !noalias !176
  %29 = add i64 %self.idx.val.i.i.i.i80, %.sroa.3.0.i73
  store i64 %29, i64* %2, align 8, !alias.scope !176, !noalias !163
  br label %bb26

bb21:                                             ; preds = %cleanup, %bb27, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"
  %.pn34.ph = phi { i8*, i32 } [ %5, %cleanup ], [ %24, %bb27 ], [ %24, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i" ]
; call core::ptr::drop_in_place<alloc::borrow::Cow<str>>
  tail call fastcc void @"_ZN4core3ptr50drop_in_place$LT$alloc..borrow..Cow$LT$str$GT$$GT$17hdbb082751bfa4a80E"(%"borrow::Cow<str>"* nonnull %rhs) #28
  resume { i8*, i32 } %.pn34.ph

bb20:                                             ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i93", %bb2.i92, %bb26, %bb23
  ret void

bb26:                                             ; preds = %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit84, %bb6
  %.idx4.val.i.i.i88 = phi i64 [ %.val2.i72, %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit84 ], [ %.val2.i43, %bb6 ]
  %_2.i85 = phi i64 [ %_2.i66, %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit84 ], [ %_2.i37, %bb6 ]
  %30 = icmp eq i64 %_2.i85, 0
  br i1 %30, label %bb20, label %bb2.i92

bb2.i92:                                          ; preds = %bb26
  %.idx.i.i.i86 = bitcast [3 x i64]* %.sroa.0.0.in.in.i39 to i8**
  %.idx.val.i.i.i87 = load i8*, i8** %.idx.i.i.i86, align 8
  %_4.i.i.i.i.i.i89 = icmp eq i64 %.idx4.val.i.i.i88, 0
  %.not.i.i.i.i.i90 = icmp eq i8* %.idx.val.i.i.i87, null
  %or.cond.i.i.i.i.i91 = select i1 %_4.i.i.i.i.i.i89, i1 true, i1 %.not.i.i.i.i.i90
  br i1 %or.cond.i.i.i.i.i91, label %bb20, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i93"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i93": ; preds = %bb2.i92
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i.i87, i64 %.idx4.val.i.i.i88, i64 1) #26
  br label %bb20
}

; core::intrinsics::const_eval_select
; Function Attrs: noreturn nounwind nonlazybind uwtable
define internal fastcc void @_ZN4core10intrinsics17const_eval_select17hee83e7c870cc171bE(i64 %arg.0, i64 %arg.1) unnamed_addr #8 personality i32 (...)* @rust_eh_personality {
start:
; call core::ops::function::FnOnce::call_once
  tail call fastcc void @_ZN4core3ops8function6FnOnce9call_once17h5e9bfb1e9387b989E(i64 %arg.0, i64 %arg.1) #25
  unreachable
}

; <alloc::collections::btree::mem::replace::PanicGuard as core::ops::drop::Drop>::drop
; Function Attrs: noreturn nounwind nonlazybind uwtable
define void @"_ZN93_$LT$alloc..collections..btree..mem..replace..PanicGuard$u20$as$u20$core..ops..drop..Drop$GT$4drop17hbc8fa2c7d3ef1565E"(%"collections::btree::mem::replace::PanicGuard"* noalias nocapture nonnull readnone align 1 %self) unnamed_addr #8 {
start:
  tail call void @llvm.trap()
  unreachable
}

; alloc::collections::btree::node::splitpoint
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind uwtable willreturn writeonly
define void @_ZN5alloc11collections5btree4node10splitpoint17h74c68204bfa441c2E({ i64, { i64, i64 } }* noalias nocapture sret({ i64, { i64, i64 } }) dereferenceable(24) %0, i64 %edge_idx) unnamed_addr #11 {
start:
  %_3 = icmp ult i64 %edge_idx, 5
  br i1 %_3, label %bb7, label %bb2

bb2:                                              ; preds = %start
  switch i64 %edge_idx, label %bb3 [
    i64 5, label %bb7
    i64 6, label %bb6
  ]

bb7:                                              ; preds = %start, %bb2, %bb6, %bb3
  %.sink12 = phi i64 [ 5, %bb6 ], [ 6, %bb3 ], [ %edge_idx, %bb2 ], [ 4, %start ]
  %.sink10 = phi i64 [ 1, %bb6 ], [ 1, %bb3 ], [ 0, %bb2 ], [ 0, %start ]
  %.sink = phi i64 [ 0, %bb6 ], [ %_10, %bb3 ], [ %edge_idx, %bb2 ], [ %edge_idx, %start ]
  %1 = getelementptr inbounds { i64, { i64, i64 } }, { i64, { i64, i64 } }* %0, i64 0, i32 0
  store i64 %.sink12, i64* %1, align 8
  %2 = getelementptr inbounds { i64, { i64, i64 } }, { i64, { i64, i64 } }* %0, i64 0, i32 1, i32 0
  store i64 %.sink10, i64* %2, align 8
  %3 = getelementptr inbounds { i64, { i64, i64 } }, { i64, { i64, i64 } }* %0, i64 0, i32 1, i32 1
  store i64 %.sink, i64* %3, align 8
  ret void

bb3:                                              ; preds = %bb2
  %_10 = add i64 %edge_idx, -7
  br label %bb7

bb6:                                              ; preds = %bb2
  br label %bb7
}

; alloc::collections::vec_deque::VecDeque<T,A>::wrap_copy::diff
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define i64 @"_ZN5alloc11collections9vec_deque21VecDeque$LT$T$C$A$GT$9wrap_copy4diff17h9ae06859755d683eE"(i64 %a, i64 %b) unnamed_addr #12 {
start:
  %_3.not = icmp ugt i64 %a, %b
  %0 = sub i64 %b, %a
  %1 = sub i64 %a, %b
  %.0 = select i1 %_3.not, i64 %1, i64 %0
  ret i64 %.0
}

; <alloc::collections::TryReserveError as core::fmt::Display>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN74_$LT$alloc..collections..TryReserveError$u20$as$u20$core..fmt..Display$GT$3fmt17h992320a8d5bef832E"({ i64, i64 }* noalias nocapture readonly align 8 dereferenceable(16) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %fmt) unnamed_addr #10 {
start:
; call core::fmt::Formatter::write_str
  %_4 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %fmt, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [24 x i8] }>* @alloc3008 to [0 x i8]*), i64 24)
  br i1 %_4, label %bb12, label %bb3

bb3:                                              ; preds = %start
  %0 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %self, i64 0, i32 1
  %1 = load i64, i64* %0, align 8
  %2 = icmp eq i64 %1, 0
  %. = select i1 %2, [0 x i8]* bitcast (<{ [64 x i8] }>* @alloc3012 to [0 x i8]*), [0 x i8]* bitcast (<{ [46 x i8] }>* @alloc3011 to [0 x i8]*)
  %.4 = select i1 %2, i64 64, i64 46
; call core::fmt::Formatter::write_str
  %3 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %fmt, [0 x i8]* noalias nonnull readonly align 1 %., i64 %.4)
  br label %bb12

bb12:                                             ; preds = %start, %bb3
  %.0 = phi i1 [ %3, %bb3 ], [ true, %start ]
  ret i1 %.0
}

; alloc::fmt::format
; Function Attrs: nonlazybind uwtable
define void @_ZN5alloc3fmt6format17h689072c96aecd7b7E(%"string::String"* noalias nocapture sret(%"string::String") dereferenceable(24) %output, %"core::fmt::Arguments"* noalias nocapture readonly dereferenceable(48) %args) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
start:
  %e.i = alloca %"core::fmt::Error", align 1
  %_6.i = alloca %"core::fmt::Arguments", align 8
  %self.i = alloca %"string::String"*, align 8
  %args.idx = getelementptr %"core::fmt::Arguments", %"core::fmt::Arguments"* %args, i64 0, i32 0, i32 0
  %args.idx.val = load [0 x { [0 x i8]*, i64 }]*, [0 x { [0 x i8]*, i64 }]** %args.idx, align 8, !nonnull !20
  %args.idx4 = getelementptr %"core::fmt::Arguments", %"core::fmt::Arguments"* %args, i64 0, i32 0, i32 1
  %args.idx4.val = load i64, i64* %args.idx4, align 8
  %args.idx5 = getelementptr %"core::fmt::Arguments", %"core::fmt::Arguments"* %args, i64 0, i32 2, i32 1
  %args.idx5.val = load i64, i64* %args.idx5, align 8
  %0 = getelementptr inbounds [0 x { [0 x i8]*, i64 }], [0 x { [0 x i8]*, i64 }]* %args.idx.val, i64 0, i64 %args.idx4.val
  %1 = bitcast { [0 x i8]*, i64 }* %0 to [0 x { [0 x i8]*, i64 }]*
  %_12.i5.i.i.i.i.i = icmp eq [0 x { [0 x i8]*, i64 }]* %args.idx.val, %1
  br i1 %_12.i5.i.i.i.i.i, label %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i, label %bb3.i.i.i.i.preheader.i

bb3.i.i.i.i.preheader.i:                          ; preds = %start
  %2 = bitcast [0 x { [0 x i8]*, i64 }]* %args.idx.val to i64*
  %3 = shl nsw i64 %args.idx4.val, 4
  %4 = add i64 %3, -16
  %5 = lshr exact i64 %4, 4
  %6 = add nuw nsw i64 %5, 1
  %xtraiter = and i64 %6, 7
  %7 = icmp ult i64 %4, 112
  br i1 %7, label %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i.loopexit.unr-lcssa, label %bb3.i.i.i.i.preheader.i.new

bb3.i.i.i.i.preheader.i.new:                      ; preds = %bb3.i.i.i.i.preheader.i
  %unroll_iter = and i64 %6, 2305843009213693944
  br label %bb3.i.i.i.i.i

bb3.i.i.i.i.i:                                    ; preds = %bb3.i.i.i.i.i, %bb3.i.i.i.i.preheader.i.new
  %accum.07.i.i.i.i.i = phi i64 [ 0, %bb3.i.i.i.i.preheader.i.new ], [ %_6.0.i.i.i.i.i.i.i.7, %bb3.i.i.i.i.i ]
  %self.sroa.0.06.i.i.i.i.i = phi i64* [ %2, %bb3.i.i.i.i.preheader.i.new ], [ %15, %bb3.i.i.i.i.i ]
  %niter = phi i64 [ %unroll_iter, %bb3.i.i.i.i.preheader.i.new ], [ %niter.nsub.7, %bb3.i.i.i.i.i ]
  %8 = getelementptr i64, i64* %self.sroa.0.06.i.i.i.i.i, i64 1
  %.idx1.val.i.i.i.i.i = load i64, i64* %8, align 8
  %_6.0.i.i.i.i.i.i.i = add i64 %.idx1.val.i.i.i.i.i, %accum.07.i.i.i.i.i
  %9 = getelementptr i64, i64* %self.sroa.0.06.i.i.i.i.i, i64 3
  %.idx1.val.i.i.i.i.i.1 = load i64, i64* %9, align 8
  %_6.0.i.i.i.i.i.i.i.1 = add i64 %.idx1.val.i.i.i.i.i.1, %_6.0.i.i.i.i.i.i.i
  %10 = getelementptr i64, i64* %self.sroa.0.06.i.i.i.i.i, i64 5
  %.idx1.val.i.i.i.i.i.2 = load i64, i64* %10, align 8
  %_6.0.i.i.i.i.i.i.i.2 = add i64 %.idx1.val.i.i.i.i.i.2, %_6.0.i.i.i.i.i.i.i.1
  %11 = getelementptr i64, i64* %self.sroa.0.06.i.i.i.i.i, i64 7
  %.idx1.val.i.i.i.i.i.3 = load i64, i64* %11, align 8
  %_6.0.i.i.i.i.i.i.i.3 = add i64 %.idx1.val.i.i.i.i.i.3, %_6.0.i.i.i.i.i.i.i.2
  %12 = getelementptr i64, i64* %self.sroa.0.06.i.i.i.i.i, i64 9
  %.idx1.val.i.i.i.i.i.4 = load i64, i64* %12, align 8
  %_6.0.i.i.i.i.i.i.i.4 = add i64 %.idx1.val.i.i.i.i.i.4, %_6.0.i.i.i.i.i.i.i.3
  %13 = getelementptr i64, i64* %self.sroa.0.06.i.i.i.i.i, i64 11
  %.idx1.val.i.i.i.i.i.5 = load i64, i64* %13, align 8
  %_6.0.i.i.i.i.i.i.i.5 = add i64 %.idx1.val.i.i.i.i.i.5, %_6.0.i.i.i.i.i.i.i.4
  %14 = getelementptr i64, i64* %self.sroa.0.06.i.i.i.i.i, i64 13
  %.idx1.val.i.i.i.i.i.6 = load i64, i64* %14, align 8
  %_6.0.i.i.i.i.i.i.i.6 = add i64 %.idx1.val.i.i.i.i.i.6, %_6.0.i.i.i.i.i.i.i.5
  %15 = getelementptr inbounds i64, i64* %self.sroa.0.06.i.i.i.i.i, i64 16
  %16 = getelementptr i64, i64* %self.sroa.0.06.i.i.i.i.i, i64 15
  %.idx1.val.i.i.i.i.i.7 = load i64, i64* %16, align 8
  %_6.0.i.i.i.i.i.i.i.7 = add i64 %.idx1.val.i.i.i.i.i.7, %_6.0.i.i.i.i.i.i.i.6
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i.loopexit.unr-lcssa, label %bb3.i.i.i.i.i

_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i.loopexit.unr-lcssa: ; preds = %bb3.i.i.i.i.i, %bb3.i.i.i.i.preheader.i
  %_6.0.i.i.i.i.i.i.i.lcssa.ph = phi i64 [ undef, %bb3.i.i.i.i.preheader.i ], [ %_6.0.i.i.i.i.i.i.i.7, %bb3.i.i.i.i.i ]
  %accum.07.i.i.i.i.i.unr = phi i64 [ 0, %bb3.i.i.i.i.preheader.i ], [ %_6.0.i.i.i.i.i.i.i.7, %bb3.i.i.i.i.i ]
  %self.sroa.0.06.i.i.i.i.i.unr = phi i64* [ %2, %bb3.i.i.i.i.preheader.i ], [ %15, %bb3.i.i.i.i.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i, label %bb3.i.i.i.i.i.epil

bb3.i.i.i.i.i.epil:                               ; preds = %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i.loopexit.unr-lcssa, %bb3.i.i.i.i.i.epil
  %accum.07.i.i.i.i.i.epil = phi i64 [ %_6.0.i.i.i.i.i.i.i.epil, %bb3.i.i.i.i.i.epil ], [ %accum.07.i.i.i.i.i.unr, %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i.loopexit.unr-lcssa ]
  %self.sroa.0.06.i.i.i.i.i.epil = phi i64* [ %17, %bb3.i.i.i.i.i.epil ], [ %self.sroa.0.06.i.i.i.i.i.unr, %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb3.i.i.i.i.i.epil ], [ %xtraiter, %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i.loopexit.unr-lcssa ]
  %17 = getelementptr inbounds i64, i64* %self.sroa.0.06.i.i.i.i.i.epil, i64 2
  %18 = getelementptr i64, i64* %self.sroa.0.06.i.i.i.i.i.epil, i64 1
  %.idx1.val.i.i.i.i.i.epil = load i64, i64* %18, align 8
  %_6.0.i.i.i.i.i.i.i.epil = add i64 %.idx1.val.i.i.i.i.i.epil, %accum.07.i.i.i.i.i.epil
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i, label %bb3.i.i.i.i.i.epil, !llvm.loop !180

_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i: ; preds = %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i.loopexit.unr-lcssa, %bb3.i.i.i.i.i.epil, %start
  %accum.0.lcssa.i.i.i.i.i = phi i64 [ 0, %start ], [ %_6.0.i.i.i.i.i.i.i.lcssa.ph, %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i.loopexit.unr-lcssa ], [ %_6.0.i.i.i.i.i.i.i.epil, %bb3.i.i.i.i.i.epil ]
  %19 = icmp eq i64 %args.idx5.val, 0
  br i1 %19, label %_ZN4core3fmt9Arguments18estimated_capacity17h04b7827fe1cf4fd8E.exit, label %bb6.i

bb6.i:                                            ; preds = %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i
  %20 = icmp eq i64 %args.idx4.val, 0
  br i1 %20, label %bb17.i, label %bb14.i

bb14.i:                                           ; preds = %bb6.i
  %21 = getelementptr inbounds [0 x { [0 x i8]*, i64 }], [0 x { [0 x i8]*, i64 }]* %args.idx.val, i64 0, i64 0, i32 1
  %_15.1.i = load i64, i64* %21, align 8
  %22 = icmp eq i64 %_15.1.i, 0
  %_19.i = icmp ult i64 %accum.0.lcssa.i.i.i.i.i, 16
  %or.cond.i = select i1 %22, i1 %_19.i, i1 false
  br i1 %or.cond.i, label %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit, label %bb17.i

bb17.i:                                           ; preds = %bb14.i, %bb6.i
  %23 = tail call { i64, i1 } @llvm.umul.with.overflow.i64(i64 %accum.0.lcssa.i.i.i.i.i, i64 2) #26
  %24 = extractvalue { i64, i1 } %23, 0
  %25 = extractvalue { i64, i1 } %23, 1
  br i1 %25, label %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit, label %_ZN4core3fmt9Arguments18estimated_capacity17h04b7827fe1cf4fd8E.exit

_ZN4core3fmt9Arguments18estimated_capacity17h04b7827fe1cf4fd8E.exit: ; preds = %bb17.i, %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i
  %.0.i = phi i64 [ %accum.0.lcssa.i.i.i.i.i, %_ZN4core4iter6traits8iterator8Iterator3sum17hd664872edcdb000dE.exit.i ], [ %24, %bb17.i ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !182)
  %26 = icmp eq i64 %.0.i, 0
  br i1 %26, label %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i": ; preds = %_ZN4core3fmt9Arguments18estimated_capacity17h04b7827fe1cf4fd8E.exit
  %27 = tail call i8* @__rust_alloc(i64 %.0.i, i64 1) #26, !noalias !185
  %28 = insertvalue { i8*, i64 } undef, i8* %27, 0
  %29 = icmp eq i8* %27, null
  br i1 %29, label %bb20.i.i.i.i.i, label %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit

bb20.i.i.i.i.i:                                   ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %.0.i, i64 1) #27, !noalias !185
  unreachable

_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit: ; preds = %bb17.i, %bb14.i, %_ZN4core3fmt9Arguments18estimated_capacity17h04b7827fe1cf4fd8E.exit, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
  %.0.i8 = phi i64 [ %.0.i, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i" ], [ 0, %_ZN4core3fmt9Arguments18estimated_capacity17h04b7827fe1cf4fd8E.exit ], [ 0, %bb14.i ], [ 0, %bb17.i ]
  %.pn.i.i.i.i.i = phi { i8*, i64 } [ %28, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %_ZN4core3fmt9Arguments18estimated_capacity17h04b7827fe1cf4fd8E.exit ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %bb14.i ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %bb17.i ]
  %_3.0.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i, 0
  %_2.sroa.0.0..sroa_idx.i = getelementptr %"string::String", %"string::String"* %output, i64 0, i32 0, i32 0, i32 0
  store i8* %_3.0.i.i.i, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !182
  %_2.sroa.4.0..sroa_idx2.i = getelementptr %"string::String", %"string::String"* %output, i64 0, i32 0, i32 0, i32 1
  store i64 %.0.i8, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !182
  %_2.sroa.5.0..sroa_idx4.i = getelementptr inbounds %"string::String", %"string::String"* %output, i64 0, i32 0, i32 1
  store i64 0, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !182
  %30 = bitcast %"core::fmt::Arguments"* %args to i8*
  %31 = bitcast %"string::String"** %self.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %31)
  store %"string::String"* %output, %"string::String"** %self.i, align 8, !noalias !190
  %_3.0.i = bitcast %"string::String"** %self.i to {}*
  %32 = bitcast %"core::fmt::Arguments"* %_6.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %32), !noalias !190
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(48) %32, i8* noundef nonnull align 8 dereferenceable(48) %30, i64 48, i1 false)
; invoke core::fmt::write
  %33 = invoke zeroext i1 @_ZN4core3fmt5write17hc4ca0ba042bde21dE({}* nonnull align 1 %_3.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, i8*, i8*, [0 x i8] }>* @vtable.8 to [3 x i64]*), %"core::fmt::Arguments"* noalias nocapture nonnull dereferenceable(48) %_6.i)
          to label %bb3 unwind label %cleanup

bb3:                                              ; preds = %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit
  call void @llvm.lifetime.end.p0i8(i64 48, i8* nonnull %32), !noalias !190
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %31)
  %34 = bitcast %"core::fmt::Error"* %e.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 0, i8* nonnull %34)
  br i1 %33, label %bb1.i, label %bb4

bb1.i:                                            ; preds = %bb3
  %_6.0.i = bitcast %"core::fmt::Error"* %e.i to {}*
; invoke core::result::unwrap_failed
  invoke void @_ZN4core6result13unwrap_failed17he6aaa65cb062b616E([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [51 x i8] }>* @alloc3013 to [0 x i8]*), i64 51, {}* nonnull align 1 %_6.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.6 to [3 x i64]*), %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc3015 to %"core::panic::location::Location"*)) #25
          to label %.noexc unwind label %cleanup

.noexc:                                           ; preds = %bb1.i
  unreachable

cleanup:                                          ; preds = %bb1.i, %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit
  %35 = landingpad { i8*, i32 }
          cleanup
  %.idx.val.i.i = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8
  %.idx4.val.i.i = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8
  %_4.i.i.i.i.i = icmp eq i64 %.idx4.val.i.i, 0
  %.not.i.i.i.i = icmp eq i8* %.idx.val.i.i, null
  %or.cond.i.i.i.i = select i1 %_4.i.i.i.i.i, i1 true, i1 %.not.i.i.i.i
  br i1 %or.cond.i.i.i.i, label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i": ; preds = %cleanup
  call void @__rust_dealloc(i8* nonnull %.idx.val.i.i, i64 %.idx4.val.i.i, i64 1) #26
  br label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit"

"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit": ; preds = %cleanup, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"
  resume { i8*, i32 } %35

bb4:                                              ; preds = %bb3
  call void @llvm.lifetime.end.p0i8(i64 0, i8* nonnull %34)
  ret void
}

; alloc::str::<impl alloc::borrow::ToOwned for str>::clone_into
; Function Attrs: nonlazybind uwtable
define void @"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$10clone_into17ha0fe031b9d51f2f4E"([0 x i8]* noalias nonnull readonly align 1 %self.0, i64 %self.1, %"string::String"* noalias nocapture align 8 dereferenceable(24) %target) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
bb3:
  %b = alloca %"vec::Vec<u8>", align 8
  %0 = bitcast %"vec::Vec<u8>"* %b to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !194)
  %1 = load i8*, i8** bitcast (<{ [16 x i8] }>* @0 to i8**), align 8, !noalias !197, !nonnull !20
  %_4.sroa.0.0.tmp.sroa.0.0..sroa_cast3.i.i.i.sroa_cast = bitcast %"string::String"* %target to i8*
  %b31 = bitcast %"vec::Vec<u8>"* %b to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %b31, i8* noundef nonnull align 8 dereferenceable(24) %_4.sroa.0.0.tmp.sroa.0.0..sroa_cast3.i.i.i.sroa_cast, i64 24, i1 false)
  %_3.sroa.0.0.dest2526.i.sroa_idx.i = getelementptr %"string::String", %"string::String"* %target, i64 0, i32 0, i32 0, i32 0
  store i8* %1, i8** %_3.sroa.0.0.dest2526.i.sroa_idx.i, align 8, !alias.scope !205, !noalias !209
  %_3.sroa.4.0.dest2526.i.sroa_idx.i = getelementptr %"string::String", %"string::String"* %target, i64 0, i32 0, i32 0, i32 1
  %_3.sroa.4.0.dest2526.i.sroa_cast.i = bitcast i64* %_3.sroa.4.0.dest2526.i.sroa_idx.i to i8*
  tail call void @llvm.memset.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(16) %_3.sroa.4.0.dest2526.i.sroa_cast.i, i8 0, i64 16, i1 false) #26, !alias.scope !194, !noalias !211
  tail call void @llvm.experimental.noalias.scope.decl(metadata !212)
  %2 = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %b, i64 0, i32 1
  %_5.i.i = load i64, i64* %2, align 8, !alias.scope !215, !noalias !218
  %_3.i.i = icmp ult i64 %_5.i.i, %self.1
  br i1 %_3.i.i, label %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$16clone_from_slice17h9acbe59e8713fc24E.exit.i", label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$8truncate17hb7f76b27637c74fbE.exit.thread.i"

"_ZN5alloc3vec16Vec$LT$T$C$A$GT$8truncate17hb7f76b27637c74fbE.exit.thread.i": ; preds = %bb3
  store i64 %self.1, i64* %2, align 8, !alias.scope !215, !noalias !218
  br label %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$16clone_from_slice17h9acbe59e8713fc24E.exit.i"

"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$16clone_from_slice17h9acbe59e8713fc24E.exit.i": ; preds = %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$8truncate17hb7f76b27637c74fbE.exit.thread.i", %bb3
  %_5.i.i.i.i13.i = phi i64 [ %self.1, %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$8truncate17hb7f76b27637c74fbE.exit.thread.i" ], [ %_5.i.i, %bb3 ]
  %_7.i.i.i.i.i.i = sub i64 %self.1, %_5.i.i.i.i13.i
  %3 = bitcast %"vec::Vec<u8>"* %b to [0 x i8]**
  %target.idx1.val8.i = load [0 x i8]*, [0 x i8]** %3, align 8, !alias.scope !220, !noalias !218
  %4 = getelementptr [0 x i8], [0 x i8]* %self.0, i64 0, i64 0
  %5 = getelementptr [0 x i8], [0 x i8]* %target.idx1.val8.i, i64 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %5, i8* nonnull align 1 %4, i64 %_5.i.i.i.i13.i, i1 false) #26, !alias.scope !223, !noalias !212
  tail call void @llvm.experimental.noalias.scope.decl(metadata !233)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !236)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !239)
  %self.idx.i.i.i.i.i.i = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %b, i64 0, i32 0, i32 1
  %self.idx.val.i.i.i.i.i.i = load i64, i64* %self.idx.i.i.i.i.i.i, align 8, !alias.scope !242, !noalias !247
  %6 = sub i64 %self.idx.val.i.i.i.i.i.i, %_5.i.i.i.i13.i
  %7 = icmp ult i64 %6, %_7.i.i.i.i.i.i
  br i1 %7, label %bb2.i.i.i.i.i.i, label %bb8

bb2.i.i.i.i.i.i:                                  ; preds = %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$16clone_from_slice17h9acbe59e8713fc24E.exit.i"
  %_4.i.i.i.i.i = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %b, i64 0, i32 0
; invoke alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  invoke fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i, i64 %_5.i.i.i.i13.i, i64 %_7.i.i.i.i.i.i)
          to label %.noexc unwind label %bb9

.noexc:                                           ; preds = %bb2.i.i.i.i.i.i
  %self.idx.val.pre.i.i.i.i = load i64, i64* %2, align 8, !alias.scope !249, !noalias !247
  %target.idx1.phi.trans.insert.i = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %b, i64 0, i32 0, i32 0
  %_2.idx.val.i.i.i.i.pre.i = load i8*, i8** %target.idx1.phi.trans.insert.i, align 8, !alias.scope !250, !noalias !247
  br label %bb8

bb8:                                              ; preds = %.noexc, %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$16clone_from_slice17h9acbe59e8713fc24E.exit.i"
  %_2.idx.val.i.i.i.i.i = phi i8* [ %5, %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$16clone_from_slice17h9acbe59e8713fc24E.exit.i" ], [ %_2.idx.val.i.i.i.i.pre.i, %.noexc ]
  %self.idx.val.i.i.i.i = phi i64 [ %_5.i.i.i.i13.i, %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$16clone_from_slice17h9acbe59e8713fc24E.exit.i" ], [ %self.idx.val.pre.i.i.i.i, %.noexc ]
  %8 = getelementptr inbounds [0 x i8], [0 x i8]* %self.0, i64 0, i64 %_5.i.i.i.i13.i
  %9 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i, i64 %self.idx.val.i.i.i.i
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %9, i8* nonnull align 1 %8, i64 %_7.i.i.i.i.i.i, i1 false) #26, !noalias !249
  %10 = add i64 %self.idx.val.i.i.i.i, %_7.i.i.i.i.i.i
  store i64 %10, i64* %2, align 8, !alias.scope !249, !noalias !247
  %target3334 = bitcast %"string::String"* %target to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %target3334, i8* noundef nonnull align 8 dereferenceable(24) %0, i64 24, i1 false)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret void

bb6:                                              ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i", %bb9
  resume { i8*, i32 } %11

bb9:                                              ; preds = %bb2.i.i.i.i.i.i
  %11 = landingpad { i8*, i32 }
          cleanup
  %.idx.i = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %b, i64 0, i32 0, i32 0
  %.idx.val.i = load i8*, i8** %.idx.i, align 8
  %.idx4.val.i = load i64, i64* %self.idx.i.i.i.i.i.i, align 8
  %_4.i.i.i.i = icmp eq i64 %.idx4.val.i, 0
  %.not.i.i.i = icmp eq i8* %.idx.val.i, null
  %or.cond.i.i.i = select i1 %_4.i.i.i.i, i1 true, i1 %.not.i.i.i
  br i1 %or.cond.i.i.i, label %bb6, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i": ; preds = %bb9
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i, i64 %.idx4.val.i, i64 1) #26
  br label %bb6
}

; alloc::str::<impl str>::to_lowercase
; Function Attrs: nonlazybind uwtable
define void @"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase17h357cc8a45c8797ccE"(%"string::String"* noalias nocapture sret(%"string::String") dereferenceable(24) %s, [0 x i8]* noalias nonnull readonly align 1 %self.0, i64 %self.1) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
start:
  tail call void @llvm.experimental.noalias.scope.decl(metadata !253)
  %0 = icmp eq i64 %self.1, 0
  br i1 %0, label %bb5, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i": ; preds = %start
  %1 = tail call i8* @__rust_alloc(i64 %self.1, i64 1) #26, !noalias !256
  %2 = insertvalue { i8*, i64 } undef, i8* %1, 0
  %3 = icmp eq i8* %1, null
  br i1 %3, label %bb20.i.i.i.i.i, label %bb5

bb20.i.i.i.i.i:                                   ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %self.1, i64 1) #27, !noalias !256
  unreachable

cleanup.loopexit:                                 ; preds = %bb3.i.i12.i.i.i.i.i
  %lpad.loopexit = landingpad { i8*, i32 }
          cleanup
  br label %cleanup

cleanup.loopexit.split-lp.loopexit:               ; preds = %bb3.us.i.i.i.i.i.i
  %lpad.loopexit47 = landingpad { i8*, i32 }
          cleanup
  br label %cleanup

cleanup.loopexit.split-lp.loopexit.split-lp.loopexit: ; preds = %bb13, %bb16, %bb22, %bb23, %bb19, %bb20, %bb17, %"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase26case_ignoreable_then_cased17h350a8b4157b435caE.exit.i", %"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase26case_ignoreable_then_cased17h82d833ce84322953E.exit.i", %bb13.sink.split.i
  %lpad.loopexit50 = landingpad { i8*, i32 }
          cleanup
  br label %cleanup

cleanup.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp: ; preds = %bb2.i.i.i, %bb3.i.i.i23
  %lpad.loopexit.split-lp = landingpad { i8*, i32 }
          cleanup
  br label %cleanup

cleanup:                                          ; preds = %cleanup.loopexit.split-lp.loopexit, %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp, %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit, %cleanup.loopexit
  %lpad.phi = phi { i8*, i32 } [ %lpad.loopexit, %cleanup.loopexit ], [ %lpad.loopexit47, %cleanup.loopexit.split-lp.loopexit ], [ %lpad.loopexit50, %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit ], [ %lpad.loopexit.split-lp, %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp ]
  %.idx.val.i.i = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8
  %.idx4.val.i.i = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8
  %_4.i.i.i.i.i = icmp eq i64 %.idx4.val.i.i, 0
  %.not.i.i.i.i = icmp eq i8* %.idx.val.i.i, null
  %or.cond.i.i.i.i = select i1 %_4.i.i.i.i.i, i1 true, i1 %.not.i.i.i.i
  br i1 %or.cond.i.i.i.i, label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i": ; preds = %cleanup
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i, i64 %.idx4.val.i.i, i64 1) #26
  br label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit"

"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit": ; preds = %cleanup, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"
  resume { i8*, i32 } %lpad.phi

bb5:                                              ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i", %start
  %.pn.i.i.i.i.i = phi { i8*, i64 } [ %2, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %start ]
  %_3.0.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i, 0
  %_2.sroa.0.0..sroa_idx.i = getelementptr %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0, i32 0
  store i8* %_3.0.i.i.i, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !253
  %_2.sroa.4.0..sroa_idx2.i = getelementptr %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0, i32 1
  store i64 %self.1, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !253
  %_2.sroa.5.0..sroa_idx4.i = getelementptr %"string::String", %"string::String"* %s, i64 0, i32 0, i32 1
  store i64 0, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !253
  %4 = getelementptr [0 x i8], [0 x i8]* %self.0, i64 0, i64 0
  %5 = getelementptr inbounds [0 x i8], [0 x i8]* %self.0, i64 0, i64 %self.1
  %_4.i.i.i.i.i15.i = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0
  br i1 %0, label %bb10, label %bb3.i.i.i.preheader

bb3.i.i.i.preheader:                              ; preds = %bb5
  %_16.i.i59 = ptrtoint [0 x i8]* %self.0 to i64
  br label %bb3.i.i.i

bb3.i.i.i:                                        ; preds = %bb3.i.i.i.preheader, %bb26
  %_16.i.i64 = phi i64 [ %_16.i10.i, %bb26 ], [ %_16.i.i59, %bb3.i.i.i.preheader ]
  %iter.sroa.5.063 = phi i8* [ %iter.sroa.5.1, %bb26 ], [ %4, %bb3.i.i.i.preheader ]
  %iter.sroa.0.061 = phi i64 [ %18, %bb26 ], [ 0, %bb3.i.i.i.preheader ]
  %6 = getelementptr inbounds i8, i8* %iter.sroa.5.063, i64 1
  %x.i.i.i = load i8, i8* %iter.sroa.5.063, align 1, !noalias !261
  %_11.i.i.i = icmp sgt i8 %x.i.i.i, -1
  br i1 %_11.i.i.i, label %bb7.i.i.i, label %bb8.i.i.i

bb8.i.i.i:                                        ; preds = %bb3.i.i.i
  %_3.i.i.i.i = and i8 %x.i.i.i, 31
  %7 = zext i8 %_3.i.i.i.i to i32
  %8 = getelementptr inbounds i8, i8* %iter.sroa.5.063, i64 2
  %y.i.i.i = load i8, i8* %6, align 1, !noalias !261
  %_3.i10.i.i.i = shl nuw nsw i32 %7, 6
  %_6.i.i.i.i = and i8 %y.i.i.i, 63
  %_5.i.i.i.i = zext i8 %_6.i.i.i.i to i32
  %9 = or i32 %_3.i10.i.i.i, %_5.i.i.i.i
  %_24.i.i.i = icmp ugt i8 %x.i.i.i, -33
  br i1 %_24.i.i.i, label %bb13.i.i.i, label %bb7

bb7.i.i.i:                                        ; preds = %bb3.i.i.i
  %_13.i.i.i = zext i8 %x.i.i.i to i32
  br label %bb7

bb13.i.i.i:                                       ; preds = %bb8.i.i.i
  %10 = getelementptr inbounds i8, i8* %iter.sroa.5.063, i64 3
  %z.i.i.i = load i8, i8* %8, align 1, !noalias !261
  %_3.i17.i.i.i = shl nuw nsw i32 %_5.i.i.i.i, 6
  %_6.i18.i.i.i = and i8 %z.i.i.i, 63
  %_5.i19.i.i.i = zext i8 %_6.i18.i.i.i to i32
  %11 = or i32 %_3.i17.i.i.i, %_5.i19.i.i.i
  %_35.i.i.i = shl nuw nsw i32 %7, 12
  %12 = or i32 %11, %_35.i.i.i
  %_38.i.i.i = icmp ugt i8 %x.i.i.i, -17
  br i1 %_38.i.i.i, label %"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i", label %bb7

"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i": ; preds = %bb13.i.i.i
  %13 = getelementptr inbounds i8, i8* %iter.sroa.5.063, i64 4
  %w.i.i.i = load i8, i8* %10, align 1, !noalias !261
  %_45.i.i.i = shl nuw nsw i32 %7, 18
  %_44.i.i.i = and i32 %_45.i.i.i, 1835008
  %_3.i26.i.i.i = shl nuw nsw i32 %11, 6
  %_6.i27.i.i.i = and i8 %w.i.i.i, 63
  %_5.i28.i.i.i = zext i8 %_6.i27.i.i.i to i32
  %14 = or i32 %_3.i26.i.i.i, %_5.i28.i.i.i
  %15 = or i32 %14, %_44.i.i.i
  %16 = icmp eq i32 %15, 1114112
  br i1 %16, label %bb10, label %bb7

bb7:                                              ; preds = %bb8.i.i.i, %bb7.i.i.i, %bb13.i.i.i, %"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i"
  %iter.sroa.5.1 = phi i8* [ %6, %bb7.i.i.i ], [ %13, %"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i" ], [ %10, %bb13.i.i.i ], [ %8, %bb8.i.i.i ]
  %17 = phi i32 [ %_13.i.i.i, %bb7.i.i.i ], [ %15, %"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i" ], [ %12, %bb13.i.i.i ], [ %9, %bb8.i.i.i ]
  %_16.i10.i = ptrtoint i8* %iter.sroa.5.1 to i64
  %_11.i = sub i64 %iter.sroa.0.061, %_16.i.i64
  %18 = add i64 %_11.i, %_16.i10.i
  switch i32 %17, label %bb13 [
    i32 1114112, label %bb10
    i32 931, label %bb11
  ]

bb10:                                             ; preds = %bb7, %"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i", %bb26, %bb5
  ret void

bb11:                                             ; preds = %bb7
  tail call void @llvm.experimental.noalias.scope.decl(metadata !268)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !271)
  %19 = icmp eq i64 %iter.sroa.0.061, 0
  br i1 %19, label %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h020f9c9bc42b5435E.exit.i", label %bb2.i.i.i.i.i

bb2.i.i.i.i.i:                                    ; preds = %bb11
  %_3.i.i.i.i.i.i.i = icmp ult i64 %iter.sroa.0.061, %self.1
  br i1 %_3.i.i.i.i.i.i.i, label %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i.i", label %bb7.i.i.i.i.i

bb7.i.i.i.i.i:                                    ; preds = %bb2.i.i.i.i.i
  %20 = icmp eq i64 %iter.sroa.0.061, %self.1
  br i1 %20, label %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h020f9c9bc42b5435E.exit.i", label %bb2.i.i.i

"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i.i": ; preds = %bb2.i.i.i.i.i
  %21 = getelementptr inbounds [0 x i8], [0 x i8]* %self.0, i64 0, i64 %iter.sroa.0.061
  %b.i.i.i.i.i = load i8, i8* %21, align 1, !alias.scope !273, !noalias !271
  %22 = icmp sgt i8 %b.i.i.i.i.i, -65
  br i1 %22, label %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h020f9c9bc42b5435E.exit.i", label %bb2.i.i.i

bb2.i.i.i:                                        ; preds = %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i.i", %bb7.i.i.i.i.i
; invoke core::str::slice_error_fail
  invoke void @_ZN4core3str16slice_error_fail17h0d3b5d2bc1b01573E([0 x i8]* noalias nonnull readonly align 1 %self.0, i64 %self.1, i64 0, i64 %iter.sroa.0.061, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc3034 to %"core::panic::location::Location"*)) #25
          to label %.noexc unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp

.noexc:                                           ; preds = %bb2.i.i.i
  unreachable

"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h020f9c9bc42b5435E.exit.i": ; preds = %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i.i", %bb7.i.i.i.i.i, %bb11
  %23 = getelementptr inbounds [0 x i8], [0 x i8]* %self.0, i64 0, i64 %iter.sroa.0.061
  br label %bb1.us.i.i.i.i.i.i

bb1.us.i.i.i.i.i.i:                               ; preds = %.noexc24, %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h020f9c9bc42b5435E.exit.i"
  %_11.i.i.i.us.i.i.i.i.i.i = phi i8* [ %23, %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h020f9c9bc42b5435E.exit.i" ], [ %_11.i.i.i.us17.i.i.i.i.i.i, %.noexc24 ]
  %_12.i.i.i.us.i.i.i.i.i.i = icmp eq i8* %_11.i.i.i.us.i.i.i.i.i.i, %4
  br i1 %_12.i.i.i.us.i.i.i.i.i.i, label %bb12.split.i, label %bb3.i.i.us.i.i.i.i.i.i

bb3.i.i.us.i.i.i.i.i.i:                           ; preds = %bb1.us.i.i.i.i.i.i
  %24 = getelementptr inbounds i8, i8* %_11.i.i.i.us.i.i.i.i.i.i, i64 -1
  %_14.i.i.us.i.i.i.i.i.i = load i8, i8* %24, align 1, !alias.scope !268, !noalias !282
  %_13.i.i.us.i.i.i.i.i.i = icmp sgt i8 %_14.i.i.us.i.i.i.i.i.i, -1
  br i1 %_13.i.i.us.i.i.i.i.i.i, label %"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.thread14.us.i.i.i.i.i.i", label %bb8.i.i.us.i.i.i.i.i.i

bb8.i.i.us.i.i.i.i.i.i:                           ; preds = %bb3.i.i.us.i.i.i.i.i.i
  %25 = getelementptr inbounds i8, i8* %_11.i.i.i.us.i.i.i.i.i.i, i64 -2
  %z.i.i.us.i.i.i.i.i.i = load i8, i8* %25, align 1, !alias.scope !268, !noalias !282
  %_3.i.i.i.us.i.i.i.i.i.i = and i8 %z.i.i.us.i.i.i.i.i.i, 31
  %26 = zext i8 %_3.i.i.i.us.i.i.i.i.i.i to i32
  %27 = icmp slt i8 %z.i.i.us.i.i.i.i.i.i, -64
  br i1 %27, label %bb13.i.i.us.i.i.i.i.i.i, label %"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.us.i.i.i.i.i.i"

bb13.i.i.us.i.i.i.i.i.i:                          ; preds = %bb8.i.i.us.i.i.i.i.i.i
  %28 = getelementptr inbounds i8, i8* %_11.i.i.i.us.i.i.i.i.i.i, i64 -3
  %y.i.i.us.i.i.i.i.i.i = load i8, i8* %28, align 1, !alias.scope !268, !noalias !282
  %_3.i20.i.i.us.i.i.i.i.i.i = and i8 %y.i.i.us.i.i.i.i.i.i, 15
  %29 = zext i8 %_3.i20.i.i.us.i.i.i.i.i.i to i32
  %30 = icmp slt i8 %y.i.i.us.i.i.i.i.i.i, -64
  br i1 %30, label %bb18.i.i.us.i.i.i.i.i.i, label %bb23.i.i.us.i.i.i.i.i.i

bb18.i.i.us.i.i.i.i.i.i:                          ; preds = %bb13.i.i.us.i.i.i.i.i.i
  %31 = getelementptr inbounds i8, i8* %_11.i.i.i.us.i.i.i.i.i.i, i64 -4
  %x.i.i.us.i.i.i.i.i.i = load i8, i8* %31, align 1, !alias.scope !268, !noalias !282
  %_3.i30.i.i.us.i.i.i.i.i.i = and i8 %x.i.i.us.i.i.i.i.i.i, 7
  %32 = zext i8 %_3.i30.i.i.us.i.i.i.i.i.i to i32
  %_3.i31.i.i.us.i.i.i.i.i.i = shl nuw nsw i32 %32, 6
  %_6.i32.i.i.us.i.i.i.i.i.i = and i8 %y.i.i.us.i.i.i.i.i.i, 63
  %_5.i33.i.i.us.i.i.i.i.i.i = zext i8 %_6.i32.i.i.us.i.i.i.i.i.i to i32
  %33 = or i32 %_3.i31.i.i.us.i.i.i.i.i.i, %_5.i33.i.i.us.i.i.i.i.i.i
  br label %bb23.i.i.us.i.i.i.i.i.i

bb23.i.i.us.i.i.i.i.i.i:                          ; preds = %bb18.i.i.us.i.i.i.i.i.i, %bb13.i.i.us.i.i.i.i.i.i
  %_11.i.i.i.us19.i.i.i.i.i.i = phi i8* [ %31, %bb18.i.i.us.i.i.i.i.i.i ], [ %28, %bb13.i.i.us.i.i.i.i.i.i ]
  %ch.1.i.i.us.i.i.i.i.i.i = phi i32 [ %33, %bb18.i.i.us.i.i.i.i.i.i ], [ %29, %bb13.i.i.us.i.i.i.i.i.i ]
  %_3.i21.i.i.us.i.i.i.i.i.i = shl nsw i32 %ch.1.i.i.us.i.i.i.i.i.i, 6
  %_6.i22.i.i.us.i.i.i.i.i.i = and i8 %z.i.i.us.i.i.i.i.i.i, 63
  %_5.i23.i.i.us.i.i.i.i.i.i = zext i8 %_6.i22.i.i.us.i.i.i.i.i.i to i32
  %34 = or i32 %_3.i21.i.i.us.i.i.i.i.i.i, %_5.i23.i.i.us.i.i.i.i.i.i
  br label %"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.us.i.i.i.i.i.i"

"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.us.i.i.i.i.i.i": ; preds = %bb23.i.i.us.i.i.i.i.i.i, %bb8.i.i.us.i.i.i.i.i.i
  %_11.i.i.i.us18.i.i.i.i.i.i = phi i8* [ %_11.i.i.i.us19.i.i.i.i.i.i, %bb23.i.i.us.i.i.i.i.i.i ], [ %25, %bb8.i.i.us.i.i.i.i.i.i ]
  %ch.0.i.i.us.i.i.i.i.i.i = phi i32 [ %34, %bb23.i.i.us.i.i.i.i.i.i ], [ %26, %bb8.i.i.us.i.i.i.i.i.i ]
  %_3.i13.i.i.us.i.i.i.i.i.i = shl i32 %ch.0.i.i.us.i.i.i.i.i.i, 6
  %_6.i.i.i.us.i.i.i.i.i.i = and i8 %_14.i.i.us.i.i.i.i.i.i, 63
  %_5.i.i.i.us.i.i.i.i.i.i = zext i8 %_6.i.i.i.us.i.i.i.i.i.i to i32
  %35 = or i32 %_3.i13.i.i.us.i.i.i.i.i.i, %_5.i.i.i.us.i.i.i.i.i.i
  %.not.us.i.i.i.i.i.i = icmp eq i32 %35, 1114112
  br i1 %.not.us.i.i.i.i.i.i, label %bb12.split.i, label %bb3.us.i.i.i.i.i.i

"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.thread14.us.i.i.i.i.i.i": ; preds = %bb3.i.i.us.i.i.i.i.i.i
  %_15.i.i.us.i.i.i.i.i.i = zext i8 %_14.i.i.us.i.i.i.i.i.i to i32
  br label %bb3.us.i.i.i.i.i.i

bb3.us.i.i.i.i.i.i:                               ; preds = %"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.thread14.us.i.i.i.i.i.i", %"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.us.i.i.i.i.i.i"
  %_11.i.i.i.us17.i.i.i.i.i.i = phi i8* [ %24, %"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.thread14.us.i.i.i.i.i.i" ], [ %_11.i.i.i.us18.i.i.i.i.i.i, %"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.us.i.i.i.i.i.i" ]
  %36 = phi i32 [ %_15.i.i.us.i.i.i.i.i.i, %"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.thread14.us.i.i.i.i.i.i" ], [ %35, %"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.us.i.i.i.i.i.i" ]
; invoke core::unicode::unicode_data::case_ignorable::lookup
  %37 = invoke zeroext i1 @_ZN4core7unicode12unicode_data14case_ignorable6lookup17h5a75bf47d320559cE(i32 %36)
          to label %.noexc24 unwind label %cleanup.loopexit.split-lp.loopexit

.noexc24:                                         ; preds = %bb3.us.i.i.i.i.i.i
  br i1 %37, label %bb1.us.i.i.i.i.i.i, label %"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase26case_ignoreable_then_cased17h350a8b4157b435caE.exit.i"

"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase26case_ignoreable_then_cased17h350a8b4157b435caE.exit.i": ; preds = %.noexc24
; invoke core::unicode::unicode_data::cased::lookup
  %38 = invoke zeroext i1 @_ZN4core7unicode12unicode_data5cased6lookup17h4e29d1a807414825E(i32 %36)
          to label %.noexc25 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit

.noexc25:                                         ; preds = %"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase26case_ignoreable_then_cased17h350a8b4157b435caE.exit.i"
  br i1 %38, label %bb2.i, label %bb12.split.i

bb2.i:                                            ; preds = %.noexc25
  %_20.i = add i64 %iter.sroa.0.061, 2
  %39 = icmp eq i64 %_20.i, 0
  br i1 %39, label %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h0c94ab3e9c025581E.exit.i", label %bb2.i.i.i.i6.i

bb2.i.i.i.i6.i:                                   ; preds = %bb2.i
  %_3.i.i.i.i.i.i5.i = icmp ult i64 %_20.i, %self.1
  br i1 %_3.i.i.i.i.i.i5.i, label %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i9.i", label %bb7.i.i.i.i7.i

bb7.i.i.i.i7.i:                                   ; preds = %bb2.i.i.i.i6.i
  %40 = icmp eq i64 %_20.i, %self.1
  br i1 %40, label %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h0c94ab3e9c025581E.exit.i", label %bb3.i.i.i23

"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i9.i": ; preds = %bb2.i.i.i.i6.i
  %41 = getelementptr inbounds [0 x i8], [0 x i8]* %self.0, i64 0, i64 %_20.i
  %b.i.i.i.i8.i = load i8, i8* %41, align 1, !alias.scope !298, !noalias !271
  %42 = icmp sgt i8 %b.i.i.i.i8.i, -65
  br i1 %42, label %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h0c94ab3e9c025581E.exit.i", label %bb3.i.i.i23

bb3.i.i.i23:                                      ; preds = %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i9.i", %bb7.i.i.i.i7.i
; invoke core::str::slice_error_fail
  invoke void @_ZN4core3str16slice_error_fail17h0d3b5d2bc1b01573E([0 x i8]* noalias nonnull readonly align 1 %self.0, i64 %self.1, i64 %_20.i, i64 %self.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc3036 to %"core::panic::location::Location"*)) #25
          to label %.noexc26 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp

.noexc26:                                         ; preds = %bb3.i.i.i23
  unreachable

"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h0c94ab3e9c025581E.exit.i": ; preds = %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i9.i", %bb7.i.i.i.i7.i, %bb2.i
  %43 = getelementptr inbounds [0 x i8], [0 x i8]* %self.0, i64 0, i64 %_20.i
  br label %bb1.i.i.i.i.i

bb1.i.i.i.i.i:                                    ; preds = %"_ZN4core4iter6traits8iterator8Iterator4find5check28_$u7b$$u7b$closure$u7d$$u7d$17he5d122f3bb4d2bcdE.exit.i.i.i.i.i", %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h0c94ab3e9c025581E.exit.i"
  %_4.sroa.10.0.i.i = phi i8 [ 0, %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h0c94ab3e9c025581E.exit.i" ], [ %_4.sroa.10.1.i.i, %"_ZN4core4iter6traits8iterator8Iterator4find5check28_$u7b$$u7b$closure$u7d$$u7d$17he5d122f3bb4d2bcdE.exit.i.i.i.i.i" ]
  %_15.i.i.i.i.i.i.i.i = phi i8* [ %43, %"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h0c94ab3e9c025581E.exit.i" ], [ %_15.i.i.i17.i.i.i.i.i, %"_ZN4core4iter6traits8iterator8Iterator4find5check28_$u7b$$u7b$closure$u7d$$u7d$17he5d122f3bb4d2bcdE.exit.i.i.i.i.i" ]
  %_12.i.i.i.i.i.i.i.i = icmp eq i8* %_15.i.i.i.i.i.i.i.i, %5
  br i1 %_12.i.i.i.i.i.i.i.i, label %bb11.split.i, label %bb3.i.i.i.i.i.i.i

bb3.i.i.i.i.i.i.i:                                ; preds = %bb1.i.i.i.i.i
  %44 = getelementptr inbounds i8, i8* %_15.i.i.i.i.i.i.i.i, i64 1
  %x.i.i.i.i.i.i.i = load i8, i8* %_15.i.i.i.i.i.i.i.i, align 1, !alias.scope !268, !noalias !307
  %_11.i.i.i.i.i.i.i = icmp sgt i8 %x.i.i.i.i.i.i.i, -1
  br i1 %_11.i.i.i.i.i.i.i, label %bb7.i.i.i.i.i.i.i, label %bb8.i.i.i.i.i.i.i

bb8.i.i.i.i.i.i.i:                                ; preds = %bb3.i.i.i.i.i.i.i
  %_3.i.i.i.i.i.i.i.i = and i8 %x.i.i.i.i.i.i.i, 31
  %45 = zext i8 %_3.i.i.i.i.i.i.i.i to i32
  %46 = getelementptr inbounds i8, i8* %_15.i.i.i.i.i.i.i.i, i64 2
  %y.i.i.i.i.i.i.i = load i8, i8* %44, align 1, !alias.scope !268, !noalias !307
  %_3.i10.i.i.i.i.i.i.i = shl nuw nsw i32 %45, 6
  %_6.i.i.i.i.i.i.i.i = and i8 %y.i.i.i.i.i.i.i, 63
  %_5.i.i.i.i.i.i.i.i = zext i8 %_6.i.i.i.i.i.i.i.i to i32
  %47 = or i32 %_3.i10.i.i.i.i.i.i.i, %_5.i.i.i.i.i.i.i.i
  %_24.i.i.i.i.i.i.i = icmp ugt i8 %x.i.i.i.i.i.i.i, -33
  br i1 %_24.i.i.i.i.i.i.i, label %bb13.i.i.i.i.i.i.i, label %bb3.i.i.i.i.i

bb7.i.i.i.i.i.i.i:                                ; preds = %bb3.i.i.i.i.i.i.i
  %_13.i.i.i.i.i.i.i = zext i8 %x.i.i.i.i.i.i.i to i32
  br label %bb3.i.i.i.i.i

bb13.i.i.i.i.i.i.i:                               ; preds = %bb8.i.i.i.i.i.i.i
  %48 = getelementptr inbounds i8, i8* %_15.i.i.i.i.i.i.i.i, i64 3
  %z.i.i.i.i.i.i.i = load i8, i8* %46, align 1, !alias.scope !268, !noalias !307
  %_3.i17.i.i.i.i.i.i.i = shl nuw nsw i32 %_5.i.i.i.i.i.i.i.i, 6
  %_6.i18.i.i.i.i.i.i.i = and i8 %z.i.i.i.i.i.i.i, 63
  %_5.i19.i.i.i.i.i.i.i = zext i8 %_6.i18.i.i.i.i.i.i.i to i32
  %49 = or i32 %_3.i17.i.i.i.i.i.i.i, %_5.i19.i.i.i.i.i.i.i
  %_35.i.i.i.i.i.i.i = shl nuw nsw i32 %45, 12
  %50 = or i32 %49, %_35.i.i.i.i.i.i.i
  %_38.i.i.i.i.i.i.i = icmp ugt i8 %x.i.i.i.i.i.i.i, -17
  br i1 %_38.i.i.i.i.i.i.i, label %"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i.i.i.i.i", label %bb3.i.i.i.i.i

"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i.i.i.i.i": ; preds = %bb13.i.i.i.i.i.i.i
  %51 = getelementptr inbounds i8, i8* %_15.i.i.i.i.i.i.i.i, i64 4
  %w.i.i.i.i.i.i.i = load i8, i8* %48, align 1, !alias.scope !268, !noalias !307
  %_45.i.i.i.i.i.i.i = shl nuw nsw i32 %45, 18
  %_44.i.i.i.i.i.i.i = and i32 %_45.i.i.i.i.i.i.i, 1835008
  %_3.i26.i.i.i.i.i.i.i = shl nuw nsw i32 %49, 6
  %_6.i27.i.i.i.i.i.i.i = and i8 %w.i.i.i.i.i.i.i, 63
  %_5.i28.i.i.i.i.i.i.i = zext i8 %_6.i27.i.i.i.i.i.i.i to i32
  %52 = or i32 %_3.i26.i.i.i.i.i.i.i, %_5.i28.i.i.i.i.i.i.i
  %53 = or i32 %52, %_44.i.i.i.i.i.i.i
  %.not.i.i.i.i.i = icmp eq i32 %53, 1114112
  br i1 %.not.i.i.i.i.i, label %bb11.split.i, label %bb3.i.i.i.i.i

bb3.i.i.i.i.i:                                    ; preds = %"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i.i.i.i.i", %bb13.i.i.i.i.i.i.i, %bb7.i.i.i.i.i.i.i, %bb8.i.i.i.i.i.i.i
  %_15.i.i.i17.i.i.i.i.i = phi i8* [ %51, %"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i.i.i.i.i" ], [ %44, %bb7.i.i.i.i.i.i.i ], [ %48, %bb13.i.i.i.i.i.i.i ], [ %46, %bb8.i.i.i.i.i.i.i ]
  %54 = phi i32 [ %53, %"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i.i.i.i.i" ], [ %_13.i.i.i.i.i.i.i, %bb7.i.i.i.i.i.i.i ], [ %50, %bb13.i.i.i.i.i.i.i ], [ %47, %bb8.i.i.i.i.i.i.i ]
  %_4.not.i.i.i.i.i.i.i = icmp eq i8 %_4.sroa.10.0.i.i, 0
  br i1 %_4.not.i.i.i.i.i.i.i, label %bb3.i.i12.i.i.i.i.i, label %56

bb3.i.i12.i.i.i.i.i:                              ; preds = %bb3.i.i.i.i.i
; invoke core::unicode::unicode_data::case_ignorable::lookup
  %55 = invoke zeroext i1 @_ZN4core7unicode12unicode_data14case_ignorable6lookup17h5a75bf47d320559cE(i32 %54)
          to label %.noexc27 unwind label %cleanup.loopexit

.noexc27:                                         ; preds = %bb3.i.i12.i.i.i.i.i
  br i1 %55, label %"_ZN4core4iter6traits8iterator8Iterator4find5check28_$u7b$$u7b$closure$u7d$$u7d$17he5d122f3bb4d2bcdE.exit.i.i.i.i.i", label %56

56:                                               ; preds = %.noexc27, %bb3.i.i.i.i.i
  br label %"_ZN4core4iter6traits8iterator8Iterator4find5check28_$u7b$$u7b$closure$u7d$$u7d$17he5d122f3bb4d2bcdE.exit.i.i.i.i.i"

"_ZN4core4iter6traits8iterator8Iterator4find5check28_$u7b$$u7b$closure$u7d$$u7d$17he5d122f3bb4d2bcdE.exit.i.i.i.i.i": ; preds = %56, %.noexc27
  %_4.sroa.10.1.i.i = phi i8 [ 0, %.noexc27 ], [ 1, %56 ]
  %57 = phi i32 [ 1114112, %.noexc27 ], [ %54, %56 ]
  %58 = icmp eq i32 %57, 1114112
  br i1 %58, label %bb1.i.i.i.i.i, label %"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase26case_ignoreable_then_cased17h82d833ce84322953E.exit.i"

"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase26case_ignoreable_then_cased17h82d833ce84322953E.exit.i": ; preds = %"_ZN4core4iter6traits8iterator8Iterator4find5check28_$u7b$$u7b$closure$u7d$$u7d$17he5d122f3bb4d2bcdE.exit.i.i.i.i.i"
; invoke core::unicode::unicode_data::cased::lookup
  %59 = invoke zeroext i1 @_ZN4core7unicode12unicode_data5cased6lookup17h4e29d1a807414825E(i32 %57)
          to label %.noexc28 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit

.noexc28:                                         ; preds = %"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase26case_ignoreable_then_cased17h82d833ce84322953E.exit.i"
  br i1 %59, label %bb12.split.i, label %bb11.split.i

bb12.split.i:                                     ; preds = %"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E.exit.us.i.i.i.i.i.i", %bb1.us.i.i.i.i.i.i, %.noexc28, %.noexc25
  %_5.i.i.i.i.i.i = load i64, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !320, !noalias !331
  %self.idx.val.i.i.i.i.i.i.i = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !334, !noalias !331
  %60 = sub i64 %self.idx.val.i.i.i.i.i.i.i, %_5.i.i.i.i.i.i
  %61 = icmp ult i64 %60, 2
  br i1 %61, label %bb13.sink.split.i, label %"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase19map_uppercase_sigma17ha03f9062f1a561cdE.exit"

bb11.split.i:                                     ; preds = %"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E.exit.i.i.i.i.i", %bb1.i.i.i.i.i, %.noexc28
  %_5.i.i.i.i.i12.i = load i64, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !337, !noalias !348
  %self.idx.val.i.i.i.i.i.i14.i = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !351, !noalias !348
  %62 = sub i64 %self.idx.val.i.i.i.i.i.i14.i, %_5.i.i.i.i.i12.i
  %63 = icmp ult i64 %62, 2
  br i1 %63, label %bb13.sink.split.i, label %"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase19map_uppercase_sigma17ha03f9062f1a561cdE.exit"

bb13.sink.split.i:                                ; preds = %bb11.split.i, %bb12.split.i
  %_5.i.i.i.i.i12.sink.i = phi i64 [ %_5.i.i.i.i.i.i, %bb12.split.i ], [ %_5.i.i.i.i.i12.i, %bb11.split.i ]
  %.sink.ph.i = phi i16 [ -31793, %bb12.split.i ], [ -32049, %bb11.split.i ]
; invoke alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  invoke fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i15.i, i64 %_5.i.i.i.i.i12.sink.i, i64 2)
          to label %.noexc29 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit

.noexc29:                                         ; preds = %bb13.sink.split.i
  %self.idx.val.pre.i.i.i.i16.i = load i64, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !271, !noalias !268
  br label %"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase19map_uppercase_sigma17ha03f9062f1a561cdE.exit"

"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase19map_uppercase_sigma17ha03f9062f1a561cdE.exit": ; preds = %bb12.split.i, %bb11.split.i, %.noexc29
  %self.idx.val.i.i.i.i.sink31.i = phi i64 [ %_5.i.i.i.i.i.i, %bb12.split.i ], [ %_5.i.i.i.i.i12.i, %bb11.split.i ], [ %self.idx.val.pre.i.i.i.i16.i, %.noexc29 ]
  %.sink.i = phi i16 [ -31793, %bb12.split.i ], [ -32049, %bb11.split.i ], [ %.sink.ph.i, %.noexc29 ]
  %_2.idx.val.i.i.i.i.i.i = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !271, !noalias !268, !nonnull !20
  %64 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i.i, i64 %self.idx.val.i.i.i.i.sink31.i
  %65 = bitcast i8* %64 to i16*
  store i16 %.sink.i, i16* %65, align 1, !noalias !271
  %66 = add i64 %self.idx.val.i.i.i.i.sink31.i, 2
  store i64 %66, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !271, !noalias !268
  br label %bb26

bb13:                                             ; preds = %bb7
; invoke core::unicode::unicode_data::conversions::to_lower
  %67 = invoke i96 @_ZN4core7unicode12unicode_data11conversions8to_lower17h4c5a99401b395956E(i32 %17)
          to label %bb14 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit

bb14:                                             ; preds = %bb13
  %.sroa.018.0.extract.trunc = trunc i96 %67 to i32
  %.sroa.420.0.extract.shift = lshr i96 %67, 32
  %.sroa.420.0.extract.trunc = trunc i96 %.sroa.420.0.extract.shift to i32
  %.sroa.5.0.extract.shift = lshr i96 %67, 64
  %.sroa.5.0.extract.trunc = trunc i96 %.sroa.5.0.extract.shift to i32
  %68 = icmp eq i32 %.sroa.420.0.extract.trunc, 0
  br i1 %68, label %bb17, label %bb15

bb17:                                             ; preds = %bb14
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.018.0.extract.trunc)
          to label %bb26 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit

bb15:                                             ; preds = %bb14
  %69 = icmp eq i32 %.sroa.5.0.extract.trunc, 0
  br i1 %69, label %bb19, label %bb16

bb19:                                             ; preds = %bb15
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.018.0.extract.trunc)
          to label %bb20 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit

bb16:                                             ; preds = %bb15
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.018.0.extract.trunc)
          to label %bb22 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit

bb22:                                             ; preds = %bb16
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.420.0.extract.trunc)
          to label %bb23 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit

bb23:                                             ; preds = %bb22
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.5.0.extract.trunc)
          to label %bb26 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit

bb20:                                             ; preds = %bb19
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.420.0.extract.trunc)
          to label %bb26 unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.loopexit

bb26:                                             ; preds = %"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase19map_uppercase_sigma17ha03f9062f1a561cdE.exit", %bb23, %bb20, %bb17
  %_12.i.i.i.i = icmp eq i8* %iter.sroa.5.1, %5
  br i1 %_12.i.i.i.i, label %bb10, label %bb3.i.i.i
}

; alloc::str::<impl str>::to_uppercase
; Function Attrs: nonlazybind uwtable
define void @"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_uppercase17ha2f0afa79571d0d0E"(%"string::String"* noalias nocapture sret(%"string::String") dereferenceable(24) %s, [0 x i8]* noalias nonnull readonly align 1 %self.0, i64 %self.1) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
start:
  tail call void @llvm.experimental.noalias.scope.decl(metadata !354)
  %0 = icmp eq i64 %self.1, 0
  br i1 %0, label %bb5, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i": ; preds = %start
  %1 = tail call i8* @__rust_alloc(i64 %self.1, i64 1) #26, !noalias !357
  %2 = insertvalue { i8*, i64 } undef, i8* %1, 0
  %3 = icmp eq i8* %1, null
  br i1 %3, label %bb20.i.i.i.i.i, label %bb5

bb20.i.i.i.i.i:                                   ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %self.1, i64 1) #27, !noalias !357
  unreachable

cleanup:                                          ; preds = %bb14, %bb17, %bb16, %bb20, %bb19, %bb13, %bb8
  %4 = landingpad { i8*, i32 }
          cleanup
  %.idx.val.i.i = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8
  %.idx4.val.i.i = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8
  %_4.i.i.i.i.i = icmp eq i64 %.idx4.val.i.i, 0
  %.not.i.i.i.i = icmp eq i8* %.idx.val.i.i, null
  %or.cond.i.i.i.i = select i1 %_4.i.i.i.i.i, i1 true, i1 %.not.i.i.i.i
  br i1 %or.cond.i.i.i.i, label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i": ; preds = %cleanup
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i, i64 %.idx4.val.i.i, i64 1) #26
  br label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit"

"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit": ; preds = %cleanup, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"
  resume { i8*, i32 } %4

bb5:                                              ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i", %start
  %.pn.i.i.i.i.i = phi { i8*, i64 } [ %2, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %start ]
  %_3.0.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i, 0
  %_2.sroa.0.0..sroa_idx.i = getelementptr %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0, i32 0
  store i8* %_3.0.i.i.i, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !354
  %_2.sroa.4.0..sroa_idx2.i = getelementptr %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0, i32 1
  store i64 %self.1, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !354
  %_2.sroa.5.0..sroa_idx4.i = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 1
  store i64 0, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !354
  %5 = getelementptr inbounds [0 x i8], [0 x i8]* %self.0, i64 0, i64 %self.1
  br i1 %0, label %bb10, label %bb3.i.i.preheader

bb3.i.i.preheader:                                ; preds = %bb5
  %6 = getelementptr [0 x i8], [0 x i8]* %self.0, i64 0, i64 0
  br label %bb3.i.i

bb3.i.i:                                          ; preds = %bb3.i.i.preheader, %bb22
  %iter.sroa.0.027 = phi i8* [ %iter.sroa.0.123, %bb22 ], [ %6, %bb3.i.i.preheader ]
  %7 = getelementptr inbounds i8, i8* %iter.sroa.0.027, i64 1
  %x.i.i = load i8, i8* %iter.sroa.0.027, align 1, !noalias !362
  %_11.i.i = icmp sgt i8 %x.i.i, -1
  br i1 %_11.i.i, label %bb7.i.i, label %bb8.i.i

bb8.i.i:                                          ; preds = %bb3.i.i
  %_3.i.i.i = and i8 %x.i.i, 31
  %8 = zext i8 %_3.i.i.i to i32
  %9 = getelementptr inbounds i8, i8* %iter.sroa.0.027, i64 2
  %y.i.i = load i8, i8* %7, align 1, !noalias !362
  %_3.i10.i.i = shl nuw nsw i32 %8, 6
  %_6.i.i.i = and i8 %y.i.i, 63
  %_5.i.i.i = zext i8 %_6.i.i.i to i32
  %10 = or i32 %_3.i10.i.i, %_5.i.i.i
  %_24.i.i = icmp ugt i8 %x.i.i, -33
  br i1 %_24.i.i, label %bb13.i.i, label %bb8

bb7.i.i:                                          ; preds = %bb3.i.i
  %_13.i.i = zext i8 %x.i.i to i32
  br label %bb8

bb13.i.i:                                         ; preds = %bb8.i.i
  %11 = getelementptr inbounds i8, i8* %iter.sroa.0.027, i64 3
  %z.i.i = load i8, i8* %9, align 1, !noalias !362
  %_3.i17.i.i = shl nuw nsw i32 %_5.i.i.i, 6
  %_6.i18.i.i = and i8 %z.i.i, 63
  %_5.i19.i.i = zext i8 %_6.i18.i.i to i32
  %12 = or i32 %_3.i17.i.i, %_5.i19.i.i
  %_35.i.i = shl nuw nsw i32 %8, 12
  %13 = or i32 %12, %_35.i.i
  %_38.i.i = icmp ugt i8 %x.i.i, -17
  br i1 %_38.i.i, label %bb7, label %bb8

bb7:                                              ; preds = %bb13.i.i
  %14 = getelementptr inbounds i8, i8* %iter.sroa.0.027, i64 4
  %w.i.i = load i8, i8* %11, align 1, !noalias !362
  %_45.i.i = shl nuw nsw i32 %8, 18
  %_44.i.i = and i32 %_45.i.i, 1835008
  %_3.i26.i.i = shl nuw nsw i32 %12, 6
  %_6.i27.i.i = and i8 %w.i.i, 63
  %_5.i28.i.i = zext i8 %_6.i27.i.i to i32
  %15 = or i32 %_3.i26.i.i, %_5.i28.i.i
  %16 = or i32 %15, %_44.i.i
  %17 = icmp eq i32 %16, 1114112
  br i1 %17, label %bb10, label %bb8

bb10:                                             ; preds = %bb7, %bb22, %bb5
  ret void

bb8:                                              ; preds = %bb7.i.i, %bb13.i.i, %bb8.i.i, %bb7
  %18 = phi i32 [ %16, %bb7 ], [ %_13.i.i, %bb7.i.i ], [ %13, %bb13.i.i ], [ %10, %bb8.i.i ]
  %iter.sroa.0.123 = phi i8* [ %14, %bb7 ], [ %7, %bb7.i.i ], [ %11, %bb13.i.i ], [ %9, %bb8.i.i ]
; invoke core::unicode::unicode_data::conversions::to_upper
  %19 = invoke i96 @_ZN4core7unicode12unicode_data11conversions8to_upper17he4910922e1cf292aE(i32 %18)
          to label %bb11 unwind label %cleanup

bb11:                                             ; preds = %bb8
  %.sroa.015.0.extract.trunc = trunc i96 %19 to i32
  %.sroa.417.0.extract.shift = lshr i96 %19, 32
  %.sroa.417.0.extract.trunc = trunc i96 %.sroa.417.0.extract.shift to i32
  %.sroa.5.0.extract.shift = lshr i96 %19, 64
  %.sroa.5.0.extract.trunc = trunc i96 %.sroa.5.0.extract.shift to i32
  %20 = icmp eq i32 %.sroa.417.0.extract.trunc, 0
  br i1 %20, label %bb14, label %bb12

bb14:                                             ; preds = %bb11
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.015.0.extract.trunc)
          to label %bb22 unwind label %cleanup

bb12:                                             ; preds = %bb11
  %21 = icmp eq i32 %.sroa.5.0.extract.trunc, 0
  br i1 %21, label %bb16, label %bb13

bb16:                                             ; preds = %bb12
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.015.0.extract.trunc)
          to label %bb17 unwind label %cleanup

bb13:                                             ; preds = %bb12
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.015.0.extract.trunc)
          to label %bb19 unwind label %cleanup

bb19:                                             ; preds = %bb13
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.417.0.extract.trunc)
          to label %bb20 unwind label %cleanup

bb20:                                             ; preds = %bb19
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.5.0.extract.trunc)
          to label %bb22 unwind label %cleanup

bb22:                                             ; preds = %bb14, %bb17, %bb20
  %_12.i.i.i = icmp eq i8* %iter.sroa.0.123, %5
  br i1 %_12.i.i.i, label %bb10, label %bb3.i.i

bb17:                                             ; preds = %bb16
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %s, i32 %.sroa.417.0.extract.trunc)
          to label %bb22 unwind label %cleanup
}

; alloc::str::<impl str>::repeat
; Function Attrs: nonlazybind uwtable
define void @"_ZN5alloc3str21_$LT$impl$u20$str$GT$6repeat17haaa4687d11a2e4b0E"(%"string::String"* noalias nocapture sret(%"string::String") dereferenceable(24) %0, [0 x i8]* noalias nonnull readonly align 1 %self.0, i64 %self.1, i64 %n) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
start:
  %buf.i = alloca %"vec::Vec<u8>", align 8
  %_3.sroa.5 = alloca [16 x i8], align 8
  %_3.sroa.5.0.sroa_idx9 = getelementptr inbounds [16 x i8], [16 x i8]* %_3.sroa.5, i64 0, i64 0
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %_3.sroa.5.0.sroa_idx9)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !367)
  %1 = icmp eq i64 %n, 0
  br i1 %1, label %bb1.i, label %bb2.i

bb1.i:                                            ; preds = %start
  tail call void @llvm.experimental.noalias.scope.decl(metadata !370)
  %2 = load i8*, i8** bitcast (<{ [16 x i8] }>* @0 to i8**), align 8, !noalias !373, !nonnull !20
  call void @llvm.memset.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(16) %_3.sroa.5.0.sroa_idx9, i8 0, i64 16, i1 false), !alias.scope !375, !noalias !376
  br label %"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6repeat17hfaa7390a5d98f425E.exit"

bb2.i:                                            ; preds = %start
  %3 = tail call { i64, i1 } @llvm.umul.with.overflow.i64(i64 %self.1, i64 %n) #26
  %4 = extractvalue { i64, i1 } %3, 0
  %5 = extractvalue { i64, i1 } %3, 1
  br i1 %5, label %bb1.i.i, label %"_ZN4core6option15Option$LT$T$GT$6expect17h41593067e281bca9E.exit.i"

bb1.i.i:                                          ; preds = %bb2.i
; call core::option::expect_failed
  tail call void @_ZN4core6option13expect_failed17had3e778ecbdbaeaeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [17 x i8] }>* @alloc3022 to [0 x i8]*), i64 17, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc3024 to %"core::panic::location::Location"*)) #25, !noalias !377
  unreachable

"_ZN4core6option15Option$LT$T$GT$6expect17h41593067e281bca9E.exit.i": ; preds = %bb2.i
  %6 = bitcast %"vec::Vec<u8>"* %buf.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %6), !noalias !377
  tail call void @llvm.experimental.noalias.scope.decl(metadata !378)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !381) #26
  %7 = icmp eq i64 %4, 0
  br i1 %7, label %"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E.exit.i", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i": ; preds = %"_ZN4core6option15Option$LT$T$GT$6expect17h41593067e281bca9E.exit.i"
  %8 = tail call i8* @__rust_alloc(i64 %4, i64 1) #26, !noalias !384
  %9 = insertvalue { i8*, i64 } undef, i8* %8, 0
  %10 = icmp eq i8* %8, null
  br i1 %10, label %bb20.i.i.i.i.i, label %"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E.exit.i"

bb20.i.i.i.i.i:                                   ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %4, i64 1) #27, !noalias !384
  unreachable

"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E.exit.i": ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i", %"_ZN4core6option15Option$LT$T$GT$6expect17h41593067e281bca9E.exit.i"
  %.pn.i.i.i.i.i = phi { i8*, i64 } [ %9, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %"_ZN4core6option15Option$LT$T$GT$6expect17h41593067e281bca9E.exit.i" ]
  %_3.0.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i, 0
  %11 = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %buf.i, i64 0, i32 0, i32 0
  store i8* %_3.0.i.i.i, i8** %11, align 8, !alias.scope !385, !noalias !377
  %12 = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %buf.i, i64 0, i32 0, i32 1
  store i64 %4, i64* %12, align 8, !alias.scope !385, !noalias !377
  %13 = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %buf.i, i64 0, i32 1
  store i64 0, i64* %13, align 8, !alias.scope !385, !noalias !377
  tail call void @llvm.experimental.noalias.scope.decl(metadata !386)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !389)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !392)
  %14 = icmp ult i64 %4, %self.1
  br i1 %14, label %bb2.i.i.i.i.i.i, label %"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E.exit.i"

bb2.i.i.i.i.i.i:                                  ; preds = %"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E.exit.i"
  %_4.i.i.i.i.i = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %buf.i, i64 0, i32 0
; invoke alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  invoke fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i, i64 0, i64 %self.1)
          to label %.noexc.i unwind label %cleanup.i, !noalias !377

.noexc.i:                                         ; preds = %bb2.i.i.i.i.i.i
  %self.idx.val.pre.i.i.i.i = load i64, i64* %13, align 8, !alias.scope !395, !noalias !396
  %_2.idx.val.i.i.i.i.pre.i = load i8*, i8** %11, align 8, !noalias !377
  br label %"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E.exit.i"

"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E.exit.i": ; preds = %.noexc.i, %"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E.exit.i"
  %_3.sroa.0.0.copyload = phi i8* [ %_3.0.i.i.i, %"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E.exit.i" ], [ %_2.idx.val.i.i.i.i.pre.i, %.noexc.i ]
  %self.idx.val.i.i.i.i = phi i64 [ 0, %"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E.exit.i" ], [ %self.idx.val.pre.i.i.i.i, %.noexc.i ]
  %15 = getelementptr [0 x i8], [0 x i8]* %self.0, i64 0, i64 0
  %16 = getelementptr inbounds i8, i8* %_3.sroa.0.0.copyload, i64 %self.idx.val.i.i.i.i
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %16, i8* nonnull align 1 %15, i64 %self.1, i1 false) #26, !noalias !398
  %17 = add i64 %self.idx.val.i.i.i.i, %self.1
  %_18.not15.i = icmp ult i64 %n, 2
  br i1 %_18.not15.i, label %bb18.i, label %bb15.i

cleanup.i:                                        ; preds = %bb2.i.i.i.i.i.i
  %18 = landingpad { i8*, i32 }
          cleanup
  %.idx.val.i.i = load i8*, i8** %11, align 8, !noalias !377
  %.idx4.val.i.i = load i64, i64* %12, align 8, !noalias !377
  %_4.i.i.i.i10.i = icmp eq i64 %.idx4.val.i.i, 0
  %.not.i.i.i.i = icmp eq i8* %.idx.val.i.i, null
  %or.cond.i.i.i.i = select i1 %_4.i.i.i.i10.i, i1 true, i1 %.not.i.i.i.i
  br i1 %or.cond.i.i.i.i, label %"_ZN4core3ptr46drop_in_place$LT$alloc..vec..Vec$LT$u8$GT$$GT$17hfc1325ebdb355577E.exit.i", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i": ; preds = %cleanup.i
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i, i64 %.idx4.val.i.i, i64 1) #26, !noalias !377
  br label %"_ZN4core3ptr46drop_in_place$LT$alloc..vec..Vec$LT$u8$GT$$GT$17hfc1325ebdb355577E.exit.i"

"_ZN4core3ptr46drop_in_place$LT$alloc..vec..Vec$LT$u8$GT$$GT$17hfc1325ebdb355577E.exit.i": ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i", %cleanup.i
  resume { i8*, i32 } %18

bb15.i:                                           ; preds = %"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E.exit.i", %bb15.i
  %m.0.in17.i = phi i64 [ %m.0.i, %bb15.i ], [ %n, %"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E.exit.i" ]
  %storemerge16.i = phi i64 [ %_34.i, %bb15.i ], [ %17, %"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E.exit.i" ]
  %m.0.i = lshr i64 %m.0.in17.i, 1
  %19 = getelementptr inbounds i8, i8* %_3.sroa.0.0.copyload, i64 %storemerge16.i
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %19, i8* nonnull align 1 %_3.sroa.0.0.copyload, i64 %storemerge16.i, i1 false) #26, !noalias !377
  %_34.i = shl i64 %storemerge16.i, 1
  %_18.not.i = icmp ult i64 %m.0.in17.i, 4
  br i1 %_18.not.i, label %bb18.i, label %bb15.i

bb18.i:                                           ; preds = %bb15.i, %"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E.exit.i"
  %storemerge.lcssa.i = phi i64 [ %17, %"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E.exit.i" ], [ %_34.i, %bb15.i ]
  store i64 %storemerge.lcssa.i, i64* %13, align 8, !noalias !377
  %rem_len.i = sub i64 %4, %storemerge.lcssa.i
  %_40.not.i = icmp eq i64 %rem_len.i, 0
  br i1 %_40.not.i, label %bb26.i, label %bb24.i

bb26.i:                                           ; preds = %bb24.i, %bb18.i
  %_3.sroa.5.0..sroa_cast = bitcast i64* %12 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(16) %_3.sroa.5.0.sroa_idx9, i8* noundef nonnull align 8 dereferenceable(16) %_3.sroa.5.0..sroa_cast, i64 16, i1 false), !noalias !376
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %6), !noalias !377
  br label %"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6repeat17hfaa7390a5d98f425E.exit"

bb24.i:                                           ; preds = %bb18.i
  %20 = getelementptr inbounds i8, i8* %_3.sroa.0.0.copyload, i64 %storemerge.lcssa.i
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %20, i8* nonnull align 1 %_3.sroa.0.0.copyload, i64 %rem_len.i, i1 false) #26, !noalias !377
  store i64 %4, i64* %13, align 8, !alias.scope !399, !noalias !377
  br label %bb26.i

"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6repeat17hfaa7390a5d98f425E.exit": ; preds = %bb1.i, %bb26.i
  %_3.sroa.0.0 = phi i8* [ %2, %bb1.i ], [ %_3.sroa.0.0.copyload, %bb26.i ]
  %_3.sroa.0.0..sroa_idx2 = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 0, i32 0
  store i8* %_3.sroa.0.0, i8** %_3.sroa.0.0..sroa_idx2, align 8, !alias.scope !402
  %_3.sroa.5.0..sroa_idx6 = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 0, i32 1
  %_3.sroa.5.0..sroa_cast7 = bitcast i64* %_3.sroa.5.0..sroa_idx6 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(16) %_3.sroa.5.0..sroa_cast7, i8* noundef nonnull align 8 dereferenceable(16) %_3.sroa.5.0.sroa_idx9, i64 16, i1 false), !alias.scope !402
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %_3.sroa.5.0.sroa_idx9)
  ret void
}

; alloc::string::String::from_utf8_lossy
; Function Attrs: nonlazybind uwtable
define void @_ZN5alloc6string6String15from_utf8_lossy17h69035c96a0da61efE(%"borrow::Cow<str>"* noalias nocapture sret(%"borrow::Cow<str>") dereferenceable(32) %0, [0 x i8]* noalias nonnull readonly align 1 %v.0, i64 %v.1) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
start:
  %_34 = alloca %"core::option::Option<core::str::lossy::Utf8LossyChunk>", align 8
  %iter1 = alloca { i8*, i64 }, align 8
  %res = alloca %"string::String", align 8
  %_7 = alloca %"core::option::Option<core::str::lossy::Utf8LossyChunk>", align 8
  %iter = alloca { i8*, i64 }, align 8
  %1 = bitcast { i8*, i64 }* %iter to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %1)
; call core::str::lossy::Utf8Lossy::from_bytes
  %2 = tail call { %"core::str::lossy::Utf8Lossy"*, i64 } @_ZN4core3str5lossy9Utf8Lossy10from_bytes17h05976e8c14cc0348E([0 x i8]* noalias nonnull readonly align 1 %v.0, i64 %v.1)
  %_4.0 = extractvalue { %"core::str::lossy::Utf8Lossy"*, i64 } %2, 0
  %_4.1 = extractvalue { %"core::str::lossy::Utf8Lossy"*, i64 } %2, 1
; call core::str::lossy::Utf8Lossy::chunks
  %3 = tail call { i8*, i64 } @_ZN4core3str5lossy9Utf8Lossy6chunks17h46b994a81ffc2754E(%"core::str::lossy::Utf8Lossy"* noalias nonnull readonly align 1 %_4.0, i64 %_4.1)
  %.fca.0.extract = extractvalue { i8*, i64 } %3, 0
  %.fca.0.gep = getelementptr inbounds { i8*, i64 }, { i8*, i64 }* %iter, i64 0, i32 0
  store i8* %.fca.0.extract, i8** %.fca.0.gep, align 8
  %.fca.1.extract = extractvalue { i8*, i64 } %3, 1
  %.fca.1.gep = getelementptr inbounds { i8*, i64 }, { i8*, i64 }* %iter, i64 0, i32 1
  store i64 %.fca.1.extract, i64* %.fca.1.gep, align 8
  %4 = bitcast %"core::option::Option<core::str::lossy::Utf8LossyChunk>"* %_7 to i8*
  call void @llvm.lifetime.start.p0i8(i64 32, i8* nonnull %4)
; call <core::str::lossy::Utf8LossyChunksIter as core::iter::traits::iterator::Iterator>::next
  call void @"_ZN96_$LT$core..str..lossy..Utf8LossyChunksIter$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17ha7777be30a234397E"(%"core::option::Option<core::str::lossy::Utf8LossyChunk>"* noalias nocapture nonnull sret(%"core::option::Option<core::str::lossy::Utf8LossyChunk>") dereferenceable(32) %_7, { i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %iter)
  %5 = getelementptr inbounds %"core::option::Option<core::str::lossy::Utf8LossyChunk>", %"core::option::Option<core::str::lossy::Utf8LossyChunk>"* %_7, i64 0, i32 0
  %6 = load {}*, {}** %5, align 8
  %.not = icmp eq {}* %6, null
  br i1 %.not, label %bb8, label %bb4

bb4:                                              ; preds = %start
  %7 = getelementptr inbounds %"core::option::Option<core::str::lossy::Utf8LossyChunk>", %"core::option::Option<core::str::lossy::Utf8LossyChunk>"* %_7, i64 0, i32 1, i64 0
  %chunk.sroa.5.0.copyload = load i64, i64* %7, align 8
  %chunk.sroa.6.0..sroa_idx11 = getelementptr inbounds %"core::option::Option<core::str::lossy::Utf8LossyChunk>", %"core::option::Option<core::str::lossy::Utf8LossyChunk>"* %_7, i64 0, i32 1, i64 1
  %8 = bitcast i64* %chunk.sroa.6.0..sroa_idx11 to [0 x i8]**
  %chunk.sroa.6.0.copyload = load [0 x i8]*, [0 x i8]** %8, align 8
  %9 = getelementptr inbounds %"core::option::Option<core::str::lossy::Utf8LossyChunk>", %"core::option::Option<core::str::lossy::Utf8LossyChunk>"* %_7, i64 0, i32 1, i64 2
  %chunk.sroa.7.0.copyload = load i64, i64* %9, align 8
  %10 = icmp ne [0 x i8]* %chunk.sroa.6.0.copyload, null
  call void @llvm.assume(i1 %10)
  %11 = icmp eq i64 %chunk.sroa.7.0.copyload, 0
  br i1 %11, label %bb6, label %bb7

bb8:                                              ; preds = %start
  %12 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %0, i64 0, i32 1
  %13 = bitcast [3 x i64]* %12 to [0 x i8]**
  store [0 x i8]* getelementptr inbounds (<{ [0 x i8] }>, <{ [0 x i8] }>* @alloc2904, i32 0, i32 0), [0 x i8]** %13, align 8
  br label %bb23

bb23:                                             ; preds = %bb6, %bb8
  %chunk.sroa.5.0.copyload.sink = phi i64 [ %chunk.sroa.5.0.copyload, %bb6 ], [ 0, %bb8 ]
  %14 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %0, i64 0, i32 1, i64 1
  store i64 %chunk.sroa.5.0.copyload.sink, i64* %14, align 8
  %15 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %0, i64 0, i32 0
  store i64 0, i64* %15, align 8
  call void @llvm.lifetime.end.p0i8(i64 32, i8* nonnull %4)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %1)
  br label %bb24

bb7:                                              ; preds = %bb4
  call void @llvm.lifetime.end.p0i8(i64 32, i8* nonnull %4)
  %16 = bitcast %"string::String"* %res to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %16)
  call void @llvm.experimental.noalias.scope.decl(metadata !406)
  %17 = icmp eq i64 %v.1, 0
  br i1 %17, label %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i": ; preds = %bb7
  %18 = call i8* @__rust_alloc(i64 %v.1, i64 1) #26, !noalias !409
  %19 = insertvalue { i8*, i64 } undef, i8* %18, 0
  %20 = icmp eq i8* %18, null
  br i1 %20, label %bb20.i.i.i.i.i, label %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit

bb20.i.i.i.i.i:                                   ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %v.1, i64 1) #27, !noalias !409
  unreachable

_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit: ; preds = %bb7, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
  %.pn.i.i.i.i.i = phi { i8*, i64 } [ %19, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %bb7 ]
  %_3.0.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i, 0
  %_2.sroa.0.0..sroa_idx.i = getelementptr inbounds %"string::String", %"string::String"* %res, i64 0, i32 0, i32 0, i32 0
  store i8* %_3.0.i.i.i, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !406
  %_2.sroa.4.0..sroa_idx2.i = getelementptr inbounds %"string::String", %"string::String"* %res, i64 0, i32 0, i32 0, i32 1
  store i64 %v.1, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !406
  %_2.sroa.5.0..sroa_idx4.i = getelementptr inbounds %"string::String", %"string::String"* %res, i64 0, i32 0, i32 1
  store i64 0, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !406
  call void @llvm.experimental.noalias.scope.decl(metadata !414)
  call void @llvm.experimental.noalias.scope.decl(metadata !417)
  call void @llvm.experimental.noalias.scope.decl(metadata !420)
  call void @llvm.experimental.noalias.scope.decl(metadata !423)
  %21 = icmp ugt i64 %chunk.sroa.5.0.copyload, %v.1
  br i1 %21, label %bb2.i.i.i.i.i.i, label %bb10

bb2.i.i.i.i.i.i:                                  ; preds = %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit
  %_4.i.i.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %res, i64 0, i32 0, i32 0
; invoke alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  invoke fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i, i64 0, i64 %chunk.sroa.5.0.copyload)
          to label %.noexc unwind label %cleanup.loopexit.split-lp

.noexc:                                           ; preds = %bb2.i.i.i.i.i.i
  %self.idx.val.pre.i.i.i.i = load i64, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !426, !noalias !427
  %_2.idx.val.i.i.i.i.i.pre = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !430, !noalias !427
  br label %bb10

bb6:                                              ; preds = %bb4
  %22 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %0, i64 0, i32 1
  %23 = bitcast [3 x i64]* %22 to {}**
  store {}* %6, {}** %23, align 8
  br label %bb23

bb24:                                             ; preds = %bb17, %bb23
  ret void

bb10:                                             ; preds = %.noexc, %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit
  %_2.idx.val.i.i.i.i.i = phi i8* [ %_3.0.i.i.i, %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit ], [ %_2.idx.val.i.i.i.i.i.pre, %.noexc ]
  %self.idx.val.i.i.i.i = phi i64 [ 0, %_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E.exit ], [ %self.idx.val.pre.i.i.i.i, %.noexc ]
  %24 = bitcast {}* %6 to i8*
  %25 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i, i64 %self.idx.val.i.i.i.i
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %25, i8* nonnull align 1 %24, i64 %chunk.sroa.5.0.copyload, i1 false) #26, !noalias !426
  %26 = add i64 %self.idx.val.i.i.i.i, %chunk.sroa.5.0.copyload
  store i64 %26, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !426, !noalias !427
  call void @llvm.experimental.noalias.scope.decl(metadata !433)
  call void @llvm.experimental.noalias.scope.decl(metadata !436)
  call void @llvm.experimental.noalias.scope.decl(metadata !439)
  call void @llvm.experimental.noalias.scope.decl(metadata !442)
  %self.idx.val.i.i.i.i.i.i29 = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !445, !noalias !450
  %27 = sub i64 %self.idx.val.i.i.i.i.i.i29, %26
  %28 = icmp ult i64 %27, 3
  br i1 %28, label %bb2.i.i.i.i.i.i32, label %bb12

bb2.i.i.i.i.i.i32:                                ; preds = %bb10
  %_4.i.i.i.i.i30 = getelementptr inbounds %"string::String", %"string::String"* %res, i64 0, i32 0, i32 0
; invoke alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  invoke fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i30, i64 %26, i64 3)
          to label %.noexc36 unwind label %cleanup.loopexit.split-lp

.noexc36:                                         ; preds = %bb2.i.i.i.i.i.i32
  %self.idx.val.pre.i.i.i.i31 = load i64, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !453, !noalias !450
  br label %bb12

cleanup.loopexit:                                 ; preds = %bb13, %bb2.i.i.i.i.i.i44, %bb2.i.i.i.i.i.i55
  %lpad.loopexit = landingpad { i8*, i32 }
          cleanup
  br label %cleanup

cleanup.loopexit.split-lp:                        ; preds = %bb2.i.i.i.i.i.i, %bb2.i.i.i.i.i.i32
  %lpad.loopexit.split-lp = landingpad { i8*, i32 }
          cleanup
  br label %cleanup

cleanup:                                          ; preds = %cleanup.loopexit.split-lp, %cleanup.loopexit
  %lpad.phi = phi { i8*, i32 } [ %lpad.loopexit, %cleanup.loopexit ], [ %lpad.loopexit.split-lp, %cleanup.loopexit.split-lp ]
  %.idx.val.i.i = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8
  %.idx4.val.i.i = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8
  %_4.i.i.i.i.i38 = icmp eq i64 %.idx4.val.i.i, 0
  %.not.i.i.i.i = icmp eq i8* %.idx.val.i.i, null
  %or.cond.i.i.i.i = select i1 %_4.i.i.i.i.i38, i1 true, i1 %.not.i.i.i.i
  br i1 %or.cond.i.i.i.i, label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i": ; preds = %cleanup
  call void @__rust_dealloc(i8* nonnull %.idx.val.i.i, i64 %.idx4.val.i.i, i64 1) #26
  br label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit"

"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit": ; preds = %cleanup, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"
  resume { i8*, i32 } %lpad.phi

bb12:                                             ; preds = %bb10, %.noexc36
  %self.idx.val.i.i.i.i33 = phi i64 [ %26, %bb10 ], [ %self.idx.val.pre.i.i.i.i31, %.noexc36 ]
  %_2.idx.val.i.i.i.i.i35 = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !454, !noalias !450, !nonnull !20
  %29 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i35, i64 %self.idx.val.i.i.i.i33
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 1 dereferenceable(3) %29, i8* noundef nonnull align 1 dereferenceable(3) getelementptr inbounds (<{ [3 x i8] }>, <{ [3 x i8] }>* @alloc3043, i64 0, i32 0, i64 0), i64 3, i1 false) #26, !noalias !453
  %30 = add i64 %self.idx.val.i.i.i.i33, 3
  store i64 %30, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !453, !noalias !450
  %_32.0 = load i8*, i8** %.fca.0.gep, align 8, !nonnull !20
  %_32.1 = load i64, i64* %.fca.1.gep, align 8
  %31 = bitcast { i8*, i64 }* %iter1 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %31)
  %32 = getelementptr inbounds { i8*, i64 }, { i8*, i64 }* %iter1, i64 0, i32 0
  store i8* %_32.0, i8** %32, align 8
  %33 = getelementptr inbounds { i8*, i64 }, { i8*, i64 }* %iter1, i64 0, i32 1
  store i64 %_32.1, i64* %33, align 8
  %34 = bitcast %"core::option::Option<core::str::lossy::Utf8LossyChunk>"* %_34 to i8*
  %35 = getelementptr inbounds %"core::option::Option<core::str::lossy::Utf8LossyChunk>", %"core::option::Option<core::str::lossy::Utf8LossyChunk>"* %_34, i64 0, i32 0
  %36 = getelementptr inbounds %"core::option::Option<core::str::lossy::Utf8LossyChunk>", %"core::option::Option<core::str::lossy::Utf8LossyChunk>"* %_34, i64 0, i32 1, i64 0
  %37 = getelementptr inbounds %"core::option::Option<core::str::lossy::Utf8LossyChunk>", %"core::option::Option<core::str::lossy::Utf8LossyChunk>"* %_34, i64 0, i32 1, i64 2
  %_4.i.i.i.i.i42 = getelementptr inbounds %"string::String", %"string::String"* %res, i64 0, i32 0, i32 0
  br label %bb13

bb13:                                             ; preds = %bb22, %bb12
  %_2.idx.val.i.i.i.i.i4767 = phi i8* [ %_2.idx.val.i.i.i.i.i4768, %bb22 ], [ %_2.idx.val.i.i.i.i.i35, %bb12 ]
  %_5.i.i.i.i.i39 = phi i64 [ %_5.i.i.i.i.i3966, %bb22 ], [ %30, %bb12 ]
  call void @llvm.lifetime.start.p0i8(i64 32, i8* nonnull %34)
; invoke <core::str::lossy::Utf8LossyChunksIter as core::iter::traits::iterator::Iterator>::next
  invoke void @"_ZN96_$LT$core..str..lossy..Utf8LossyChunksIter$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17ha7777be30a234397E"(%"core::option::Option<core::str::lossy::Utf8LossyChunk>"* noalias nocapture nonnull sret(%"core::option::Option<core::str::lossy::Utf8LossyChunk>") dereferenceable(32) %_34, { i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %iter1)
          to label %bb14 unwind label %cleanup.loopexit

bb14:                                             ; preds = %bb13
  %38 = load {}*, {}** %35, align 8
  %39 = icmp eq {}* %38, null
  br i1 %39, label %bb17, label %bb15

bb17:                                             ; preds = %bb14
  call void @llvm.lifetime.end.p0i8(i64 32, i8* nonnull %34)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %31)
  %_50.sroa.0.0..sroa_idx18 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %0, i64 0, i32 1
  %_50.sroa.0.0..sroa_idx187071 = bitcast [3 x i64]* %_50.sroa.0.0..sroa_idx18 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %_50.sroa.0.0..sroa_idx187071, i8* noundef nonnull align 8 dereferenceable(24) %16, i64 24, i1 false)
  %40 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %0, i64 0, i32 0
  store i64 1, i64* %40, align 8
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %16)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %1)
  br label %bb24

bb15:                                             ; preds = %bb14
  %valid.13 = load i64, i64* %36, align 8
  %broken.15 = load i64, i64* %37, align 8
  call void @llvm.experimental.noalias.scope.decl(metadata !457)
  call void @llvm.experimental.noalias.scope.decl(metadata !460)
  call void @llvm.experimental.noalias.scope.decl(metadata !463)
  call void @llvm.experimental.noalias.scope.decl(metadata !466)
  %self.idx.val.i.i.i.i.i.i41 = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !469, !noalias !474
  %41 = sub i64 %self.idx.val.i.i.i.i.i.i41, %_5.i.i.i.i.i39
  %42 = icmp ult i64 %41, %valid.13
  br i1 %42, label %bb2.i.i.i.i.i.i44, label %bb19

bb2.i.i.i.i.i.i44:                                ; preds = %bb15
; invoke alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  invoke fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i42, i64 %_5.i.i.i.i.i39, i64 %valid.13)
          to label %.noexc48 unwind label %cleanup.loopexit

.noexc48:                                         ; preds = %bb2.i.i.i.i.i.i44
  %self.idx.val.pre.i.i.i.i43 = load i64, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !477, !noalias !474
  %_2.idx.val.i.i.i.i.i47.pre = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !478, !noalias !474
  br label %bb19

bb19:                                             ; preds = %bb15, %.noexc48
  %_2.idx.val.i.i.i.i.i47 = phi i8* [ %_2.idx.val.i.i.i.i.i4767, %bb15 ], [ %_2.idx.val.i.i.i.i.i47.pre, %.noexc48 ]
  %self.idx.val.i.i.i.i45 = phi i64 [ %_5.i.i.i.i.i39, %bb15 ], [ %self.idx.val.pre.i.i.i.i43, %.noexc48 ]
  %43 = bitcast {}* %38 to i8*
  %44 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i47, i64 %self.idx.val.i.i.i.i45
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %44, i8* nonnull align 1 %43, i64 %valid.13, i1 false) #26, !noalias !477
  %45 = add i64 %self.idx.val.i.i.i.i45, %valid.13
  store i64 %45, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !477, !noalias !474
  %46 = icmp eq i64 %broken.15, 0
  br i1 %46, label %bb22, label %bb20

bb22:                                             ; preds = %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit60, %bb19
  %_2.idx.val.i.i.i.i.i4768 = phi i8* [ %_2.idx.val.i.i.i.i.i58, %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit60 ], [ %_2.idx.val.i.i.i.i.i47, %bb19 ]
  %_5.i.i.i.i.i3966 = phi i64 [ %50, %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit60 ], [ %45, %bb19 ]
  call void @llvm.lifetime.end.p0i8(i64 32, i8* nonnull %34)
  br label %bb13

bb20:                                             ; preds = %bb19
  call void @llvm.experimental.noalias.scope.decl(metadata !481)
  call void @llvm.experimental.noalias.scope.decl(metadata !484)
  call void @llvm.experimental.noalias.scope.decl(metadata !487)
  call void @llvm.experimental.noalias.scope.decl(metadata !490)
  %self.idx.val.i.i.i.i.i.i52 = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !493, !noalias !498
  %47 = sub i64 %self.idx.val.i.i.i.i.i.i52, %45
  %48 = icmp ult i64 %47, 3
  br i1 %48, label %bb2.i.i.i.i.i.i55, label %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit60

bb2.i.i.i.i.i.i55:                                ; preds = %bb20
; invoke alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  invoke fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i42, i64 %45, i64 3)
          to label %.noexc59 unwind label %cleanup.loopexit

.noexc59:                                         ; preds = %bb2.i.i.i.i.i.i55
  %self.idx.val.pre.i.i.i.i54 = load i64, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !501, !noalias !498
  br label %_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit60

_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E.exit60: ; preds = %bb20, %.noexc59
  %self.idx.val.i.i.i.i56 = phi i64 [ %45, %bb20 ], [ %self.idx.val.pre.i.i.i.i54, %.noexc59 ]
  %_2.idx.val.i.i.i.i.i58 = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !502, !noalias !498, !nonnull !20
  %49 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i58, i64 %self.idx.val.i.i.i.i56
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 1 dereferenceable(3) %49, i8* noundef nonnull align 1 dereferenceable(3) getelementptr inbounds (<{ [3 x i8] }>, <{ [3 x i8] }>* @alloc3043, i64 0, i32 0, i64 0), i64 3, i1 false) #26, !noalias !501
  %50 = add i64 %self.idx.val.i.i.i.i56, 3
  store i64 %50, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !501, !noalias !498
  br label %bb22
}

; alloc::string::String::from_utf16
; Function Attrs: nonlazybind uwtable
define void @_ZN5alloc6string6String10from_utf1617hee719b9cd654ec01E(%"core::result::Result<string::String, string::FromUtf16Error>"* noalias nocapture sret(%"core::result::Result<string::String, string::FromUtf16Error>") dereferenceable(24) %0, [0 x i16]* noalias nonnull readonly align 2 %v.0, i64 %v.1) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
start:
  %ret = alloca %"string::String", align 8
  %1 = bitcast %"string::String"* %ret to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %1)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !505)
  %2 = icmp eq i64 %v.1, 0
  br i1 %2, label %bb5, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i": ; preds = %start
  %3 = tail call i8* @__rust_alloc(i64 %v.1, i64 1) #26, !noalias !508
  %4 = insertvalue { i8*, i64 } undef, i8* %3, 0
  %5 = icmp eq i8* %3, null
  br i1 %5, label %bb20.i.i.i.i.i, label %bb5

bb20.i.i.i.i.i:                                   ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %v.1, i64 1) #27, !noalias !508
  unreachable

cleanup:                                          ; preds = %bb11
  %6 = landingpad { i8*, i32 }
          cleanup
  %.idx.val.i.i = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8
  %.idx4.val.i.i = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8
  %_4.i.i.i.i.i = icmp eq i64 %.idx4.val.i.i, 0
  %.not.i.i.i.i = icmp eq i8* %.idx.val.i.i, null
  %or.cond.i.i.i.i = select i1 %_4.i.i.i.i.i, i1 true, i1 %.not.i.i.i.i
  br i1 %or.cond.i.i.i.i, label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i": ; preds = %cleanup
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i, i64 %.idx4.val.i.i, i64 1) #26
  br label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit"

"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit": ; preds = %cleanup, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"
  resume { i8*, i32 } %6

bb5:                                              ; preds = %start, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i"
  %.pn.i.i.i.i.i = phi { i8*, i64 } [ %4, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %start ]
  %_3.0.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i, 0
  %_2.sroa.0.0..sroa_idx.i = getelementptr inbounds %"string::String", %"string::String"* %ret, i64 0, i32 0, i32 0, i32 0
  store i8* %_3.0.i.i.i, i8** %_2.sroa.0.0..sroa_idx.i, align 8, !alias.scope !505
  %_2.sroa.4.0..sroa_idx2.i = getelementptr inbounds %"string::String", %"string::String"* %ret, i64 0, i32 0, i32 0, i32 1
  store i64 %v.1, i64* %_2.sroa.4.0..sroa_idx2.i, align 8, !alias.scope !505
  %_2.sroa.5.0..sroa_idx4.i = getelementptr inbounds %"string::String", %"string::String"* %ret, i64 0, i32 0, i32 1
  store i64 0, i64* %_2.sroa.5.0..sroa_idx4.i, align 8, !alias.scope !505
  %7 = getelementptr [0 x i16], [0 x i16]* %v.0, i64 0, i64 0
  %8 = getelementptr inbounds [0 x i16], [0 x i16]* %v.0, i64 0, i64 %v.1
  br label %bb6

bb6:                                              ; preds = %bb11, %bb5
  %iter.sroa.0.0 = phi i16* [ %7, %bb5 ], [ %iter.sroa.0.3, %bb11 ]
  %_12.i.i.i = icmp eq i16* %iter.sroa.0.0, %8
  br i1 %_12.i.i.i, label %bb10, label %"_ZN104_$LT$core..iter..adapters..cloned..Cloned$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h2e9ba807445181abE.exit.i"

"_ZN104_$LT$core..iter..adapters..cloned..Cloned$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h2e9ba807445181abE.exit.i": ; preds = %bb6
  %9 = getelementptr inbounds i16, i16* %iter.sroa.0.0, i64 1
  %.val.i.i.i = load i16, i16* %iter.sroa.0.0, align 2, !alias.scope !513, !noalias !516
  %10 = and i16 %.val.i.i.i, -2048
  %.not.i = icmp eq i16 %10, -10240
  br i1 %.not.i, label %bb17.i, label %bb15.i

bb17.i:                                           ; preds = %"_ZN104_$LT$core..iter..adapters..cloned..Cloned$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h2e9ba807445181abE.exit.i"
  %_23.i = icmp ugt i16 %.val.i.i.i, -9217
  %_12.i.i69.i = icmp eq i16* %9, %8
  %or.cond = select i1 %_23.i, i1 true, i1 %_12.i.i69.i
  br i1 %or.cond, label %bb13, label %bb23.i

bb15.i:                                           ; preds = %"_ZN104_$LT$core..iter..adapters..cloned..Cloned$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h2e9ba807445181abE.exit.i"
  %_19.sroa.440.0.insert.ext.i = zext i16 %.val.i.i.i to i64
  %_19.sroa.440.0.insert.shift.i = shl nuw nsw i64 %_19.sroa.440.0.insert.ext.i, 32
  br label %bb7

bb23.i:                                           ; preds = %bb17.i
  %.val.i.i70.i = load i16, i16* %9, align 2, !alias.scope !521, !noalias !524
  %11 = and i16 %.val.i.i70.i, -1024
  %.not80.i = icmp eq i16 %11, -9216
  br i1 %.not80.i, label %bb28.i, label %bb13

bb28.i:                                           ; preds = %bb23.i
  %12 = getelementptr inbounds i16, i16* %iter.sroa.0.0, i64 2
  %_50.i = add nsw i16 %.val.i.i.i, 10240
  %_49.i = zext i16 %_50.i to i64
  %_53.i = add nsw i16 %.val.i.i70.i, 9216
  %_52.i = zext i16 %_53.i to i64
  %13 = shl nuw nsw i64 %_49.i, 42
  %14 = shl nuw nsw i64 %_52.i, 32
  %c.i = or i64 %14, %13
  %_55.sroa.464.0.insert.shift.i = add nuw nsw i64 %c.i, 281474976710656
  br label %bb7

bb7:                                              ; preds = %bb28.i, %bb15.i
  %iter.sroa.0.3 = phi i16* [ %12, %bb28.i ], [ %9, %bb15.i ]
  %.sroa.0.3.i = phi i64 [ %_55.sroa.464.0.insert.shift.i, %bb28.i ], [ %_19.sroa.440.0.insert.shift.i, %bb15.i ]
  %.sroa.037.0.extract.trunc = trunc i64 %.sroa.0.3.i to i16
  %.sroa.5.0.extract.shift = lshr i64 %.sroa.0.3.i, 32
  %.sroa.5.0.extract.trunc = trunc i64 %.sroa.5.0.extract.shift to i32
  %15 = icmp eq i16 %.sroa.037.0.extract.trunc, 2
  br i1 %15, label %bb10, label %bb8

bb10:                                             ; preds = %bb6, %bb7
  %16 = bitcast %"core::result::Result<string::String, string::FromUtf16Error>"* %0 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %16, i8* noundef nonnull align 8 dereferenceable(24) %1, i64 24, i1 false)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %1)
  br label %bb15

bb8:                                              ; preds = %bb7
  %_16 = and i64 %.sroa.0.3.i, 65535
  %17 = icmp eq i64 %_16, 0
  br i1 %17, label %bb11, label %bb13

bb11:                                             ; preds = %bb8
; invoke alloc::string::String::push
  invoke fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %ret, i32 %.sroa.5.0.extract.trunc)
          to label %bb6 unwind label %cleanup

bb13:                                             ; preds = %bb17.i, %bb23.i, %bb8
  %18 = getelementptr inbounds %"core::result::Result<string::String, string::FromUtf16Error>", %"core::result::Result<string::String, string::FromUtf16Error>"* %0, i64 0, i32 0
  store {}* null, {}** %18, align 8
  %.idx.val.i.i43 = load i8*, i8** %_2.sroa.0.0..sroa_idx.i, align 8
  %.idx4.val.i.i45 = load i64, i64* %_2.sroa.4.0..sroa_idx2.i, align 8
  %_4.i.i.i.i.i46 = icmp eq i64 %.idx4.val.i.i45, 0
  %.not.i.i.i.i47 = icmp eq i8* %.idx.val.i.i43, null
  %or.cond.i.i.i.i48 = select i1 %_4.i.i.i.i.i46, i1 true, i1 %.not.i.i.i.i47
  br i1 %or.cond.i.i.i.i48, label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit50", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i49"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i49": ; preds = %bb13
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i43, i64 %.idx4.val.i.i45, i64 1) #26
  br label %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit50"

"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit50": ; preds = %bb13, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i49"
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %1)
  br label %bb15

bb15:                                             ; preds = %bb10, %"_ZN4core3ptr42drop_in_place$LT$alloc..string..String$GT$17h2c65d590f74a8d04E.exit50"
  ret void
}

; alloc::string::String::into_raw_parts
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @_ZN5alloc6string6String14into_raw_parts17h9d26432c93da74ceE({ i8*, i64, i64 }* noalias nocapture sret({ i8*, i64, i64 }) dereferenceable(24) %0, %"string::String"* noalias nocapture readonly dereferenceable(24) %self) unnamed_addr #9 {
start:
  %_2.sroa.0.0..sroa_idx = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 0
  %_2.sroa.0.0.copyload = load i8*, i8** %_2.sroa.0.0..sroa_idx, align 8, !nonnull !20
  %_2.sroa.4.0..sroa_idx2 = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 1
  %1 = bitcast i64* %_2.sroa.4.0..sroa_idx2 to <2 x i64>*
  %2 = load <2 x i64>, <2 x i64>* %1, align 8
  %shuffle = shufflevector <2 x i64> %2, <2 x i64> poison, <2 x i32> <i32 1, i32 0>
  %3 = getelementptr inbounds { i8*, i64, i64 }, { i8*, i64, i64 }* %0, i64 0, i32 0
  store i8* %_2.sroa.0.0.copyload, i8** %3, align 8, !alias.scope !527, !noalias !530
  %4 = getelementptr inbounds { i8*, i64, i64 }, { i8*, i64, i64 }* %0, i64 0, i32 1
  %5 = bitcast i64* %4 to <2 x i64>*
  store <2 x i64> %shuffle, <2 x i64>* %5, align 8, !alias.scope !527, !noalias !530
  ret void
}

; alloc::string::String::try_reserve
; Function Attrs: nounwind nonlazybind uwtable
define void @_ZN5alloc6string6String11try_reserve17h58233e2aa8372864E(%"core::result::Result<(), collections::TryReserveError>"* noalias nocapture sret(%"core::result::Result<(), collections::TryReserveError>") dereferenceable(24) %0, %"string::String"* noalias nocapture align 8 dereferenceable(24) %self, i64 %additional) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %_30.i.i.i = alloca %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", align 8
  %_28.i.i.i = alloca %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", align 8
  tail call void @llvm.experimental.noalias.scope.decl(metadata !532)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !535)
  %1 = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 1
  %_4.i = load i64, i64* %1, align 8, !alias.scope !535, !noalias !532
  tail call void @llvm.experimental.noalias.scope.decl(metadata !537) #26
  tail call void @llvm.experimental.noalias.scope.decl(metadata !540) #26
  %self.idx.i.i = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 1
  %self.idx.val.i.i = load i64, i64* %self.idx.i.i, align 8, !alias.scope !542, !noalias !543
  %2 = sub i64 %self.idx.val.i.i, %_4.i
  %3 = icmp ult i64 %2, %additional
  br i1 %3, label %bb2.i.i, label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$11try_reserve17haa990c326ba46a91E.exit"

bb2.i.i:                                          ; preds = %start
  tail call void @llvm.experimental.noalias.scope.decl(metadata !544) #26
  tail call void @llvm.experimental.noalias.scope.decl(metadata !547) #26
  %4 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %_4.i, i64 %additional) #26
  %5 = extractvalue { i64, i1 } %4, 0
  %6 = extractvalue { i64, i1 } %4, 1
  br i1 %6, label %bb10.i.i.i, label %bb8.i.i.i

bb8.i.i.i:                                        ; preds = %bb2.i.i
  %_19.i.i.i = shl i64 %self.idx.val.i.i, 1
  %7 = icmp ugt i64 %_19.i.i.i, %5
  %.0.sroa.speculated.i.i.i.i.i.i = select i1 %7, i64 %_19.i.i.i, i64 %5
  %8 = icmp ugt i64 %.0.sroa.speculated.i.i.i.i.i.i, 8
  %.0.sroa.speculated.i.i.i16.i.i.i = select i1 %8, i64 %.0.sroa.speculated.i.i.i.i.i.i, i64 8
  %9 = bitcast %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %9) #26, !noalias !549
  %10 = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %10) #26, !noalias !549
  %self.idx.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 0
  %_4.i.i.i.i = icmp eq i64 %self.idx.val.i.i, 0
  br i1 %_4.i.i.i.i, label %bb5.i.i.i.i, label %bb6.i.i.i.i

bb6.i.i.i.i:                                      ; preds = %bb8.i.i.i
  %self.idx.val.i.i.i = load i8*, i8** %self.idx.i.i.i, align 8, !alias.scope !550, !noalias !551
  %_13.sroa.0.0..sroa_idx.i.i.i.i = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i.i.i to i8**
  store i8* %self.idx.val.i.i.i, i8** %_13.sroa.0.0..sroa_idx.i.i.i.i, align 8, !alias.scope !552, !noalias !549
  %11 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i.i.i, i64 0, i32 1, i64 0
  store i64 %self.idx.val.i.i, i64* %11, align 8, !alias.scope !552, !noalias !549
  %12 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i.i.i, i64 0, i32 1, i64 1
  store i64 1, i64* %12, align 8, !alias.scope !552, !noalias !549
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i.i.i"

bb5.i.i.i.i:                                      ; preds = %bb8.i.i.i
  %13 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i.i.i, i64 0, i32 0
  store {}* null, {}** %13, align 8, !alias.scope !552, !noalias !549
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i.i.i"

"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i.i.i": ; preds = %bb5.i.i.i.i, %bb6.i.i.i.i
; call alloc::raw_vec::finish_grow
  call fastcc void @_ZN5alloc7raw_vec11finish_grow17h290ed6d0c09e324dE(%"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* noalias nocapture nonnull dereferenceable(24) %_28.i.i.i, i64 %.0.sroa.speculated.i.i.i16.i.i.i, i64 1, %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* noalias nocapture nonnull dereferenceable(24) %_30.i.i.i) #26, !noalias !549
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %10) #26, !noalias !549
  %14 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i.i.i, i64 0, i32 0
  %_2.i.i.i.i = load i64, i64* %14, align 8, !range !2, !alias.scope !555, !noalias !558
  %switch.not.i17.i.i.i = icmp eq i64 %_2.i.i.i.i, 1
  br i1 %switch.not.i17.i.i.i, label %bb20.i.i.i, label %bb18.i.i.i

bb10.i.i.i:                                       ; preds = %bb2.i.i
  %15 = getelementptr inbounds %"core::result::Result<(), collections::TryReserveError>", %"core::result::Result<(), collections::TryReserveError>"* %0, i64 0, i32 1, i64 0
  store i64 %5, i64* %15, align 8, !alias.scope !560, !noalias !550
  %16 = getelementptr inbounds %"core::result::Result<(), collections::TryReserveError>", %"core::result::Result<(), collections::TryReserveError>"* %0, i64 0, i32 1, i64 1
  store i64 0, i64* %16, align 8, !alias.scope !560, !noalias !550
  br label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$11try_reserve17haa990c326ba46a91E.exit"

bb18.i.i.i:                                       ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i.i.i"
  %17 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i.i.i, i64 0, i32 1
  %18 = bitcast [2 x i64]* %17 to i8**
  %v.0.i46.i.i.i = load i8*, i8** %18, align 8, !alias.scope !555, !noalias !558, !nonnull !20
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %9) #26, !noalias !549
  store i8* %v.0.i46.i.i.i, i8** %self.idx.i.i.i, align 8, !alias.scope !563, !noalias !551
  store i64 %.0.sroa.speculated.i.i.i16.i.i.i, i64* %self.idx.i.i, align 8, !alias.scope !563, !noalias !551
  br label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$11try_reserve17haa990c326ba46a91E.exit"

bb20.i.i.i:                                       ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i.i.i"
  %19 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_28.i.i.i, i64 0, i32 1, i64 0
  %20 = bitcast i64* %19 to <2 x i64>*
  %21 = load <2 x i64>, <2 x i64>* %20, align 8, !alias.scope !555, !noalias !558
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %9) #26, !noalias !549
  %22 = getelementptr inbounds %"core::result::Result<(), collections::TryReserveError>", %"core::result::Result<(), collections::TryReserveError>"* %0, i64 0, i32 1, i64 0
  %23 = bitcast i64* %22 to <2 x i64>*
  store <2 x i64> %21, <2 x i64>* %23, align 8, !alias.scope !566, !noalias !550
  br label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$11try_reserve17haa990c326ba46a91E.exit"

"_ZN5alloc3vec16Vec$LT$T$C$A$GT$11try_reserve17haa990c326ba46a91E.exit": ; preds = %start, %bb10.i.i.i, %bb18.i.i.i, %bb20.i.i.i
  %.sink.i.i = phi i64 [ 0, %start ], [ 1, %bb10.i.i.i ], [ 1, %bb20.i.i.i ], [ 0, %bb18.i.i.i ]
  %24 = getelementptr inbounds %"core::result::Result<(), collections::TryReserveError>", %"core::result::Result<(), collections::TryReserveError>"* %0, i64 0, i32 0
  store i64 %.sink.i.i, i64* %24, align 8, !alias.scope !543, !noalias !542
  ret void
}

; alloc::string::String::try_reserve_exact
; Function Attrs: nounwind nonlazybind uwtable
define void @_ZN5alloc6string6String17try_reserve_exact17h9988ae5376be4255E(%"core::result::Result<(), collections::TryReserveError>"* noalias nocapture sret(%"core::result::Result<(), collections::TryReserveError>") dereferenceable(24) %0, %"string::String"* noalias nocapture align 8 dereferenceable(24) %self, i64 %additional) unnamed_addr #2 {
start:
  %_24.i.i.i = alloca %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", align 8
  %_22.i.i.i = alloca %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", align 8
  tail call void @llvm.experimental.noalias.scope.decl(metadata !569)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !572)
  %1 = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 1
  %_4.i = load i64, i64* %1, align 8, !alias.scope !572, !noalias !569
  tail call void @llvm.experimental.noalias.scope.decl(metadata !574) #26
  tail call void @llvm.experimental.noalias.scope.decl(metadata !577) #26
  %self.idx.i.i = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 1
  %self.idx.val.i.i = load i64, i64* %self.idx.i.i, align 8, !alias.scope !579, !noalias !580
  %2 = sub i64 %self.idx.val.i.i, %_4.i
  %3 = icmp ult i64 %2, %additional
  br i1 %3, label %bb2.i.i, label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17try_reserve_exact17h0654327a5aa1fc1eE.exit"

bb2.i.i:                                          ; preds = %start
  tail call void @llvm.experimental.noalias.scope.decl(metadata !581) #26
  tail call void @llvm.experimental.noalias.scope.decl(metadata !584) #26
  %4 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %_4.i, i64 %additional) #26
  %5 = extractvalue { i64, i1 } %4, 0
  %6 = extractvalue { i64, i1 } %4, 1
  br i1 %6, label %bb10.i.i.i, label %bb8.i.i.i

bb8.i.i.i:                                        ; preds = %bb2.i.i
  %7 = bitcast %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_22.i.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %7) #26, !noalias !586
  %8 = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_24.i.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %8) #26, !noalias !586
  %self.idx.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 0
  %_4.i.i.i.i = icmp eq i64 %self.idx.val.i.i, 0
  br i1 %_4.i.i.i.i, label %bb5.i.i.i.i, label %bb6.i.i.i.i

bb6.i.i.i.i:                                      ; preds = %bb8.i.i.i
  %self.idx.val.i.i.i = load i8*, i8** %self.idx.i.i.i, align 8, !alias.scope !587, !noalias !588
  %_13.sroa.0.0..sroa_idx.i.i.i.i = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_24.i.i.i to i8**
  store i8* %self.idx.val.i.i.i, i8** %_13.sroa.0.0..sroa_idx.i.i.i.i, align 8, !alias.scope !589, !noalias !586
  %9 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_24.i.i.i, i64 0, i32 1, i64 0
  store i64 %self.idx.val.i.i, i64* %9, align 8, !alias.scope !589, !noalias !586
  %10 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_24.i.i.i, i64 0, i32 1, i64 1
  store i64 1, i64* %10, align 8, !alias.scope !589, !noalias !586
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i.i.i"

bb5.i.i.i.i:                                      ; preds = %bb8.i.i.i
  %11 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_24.i.i.i, i64 0, i32 0
  store {}* null, {}** %11, align 8, !alias.scope !589, !noalias !586
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i.i.i"

"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i.i.i": ; preds = %bb5.i.i.i.i, %bb6.i.i.i.i
; call alloc::raw_vec::finish_grow
  call fastcc void @_ZN5alloc7raw_vec11finish_grow17h290ed6d0c09e324dE(%"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* noalias nocapture nonnull dereferenceable(24) %_22.i.i.i, i64 %5, i64 1, %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* noalias nocapture nonnull dereferenceable(24) %_24.i.i.i) #26, !noalias !586
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %8) #26, !noalias !586
  %12 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_22.i.i.i, i64 0, i32 0
  %_2.i.i.i.i = load i64, i64* %12, align 8, !range !2, !alias.scope !592, !noalias !595
  %switch.not.i15.i.i.i = icmp eq i64 %_2.i.i.i.i, 1
  br i1 %switch.not.i15.i.i.i, label %bb18.i.i.i, label %bb16.i.i.i

bb10.i.i.i:                                       ; preds = %bb2.i.i
  %13 = getelementptr inbounds %"core::result::Result<(), collections::TryReserveError>", %"core::result::Result<(), collections::TryReserveError>"* %0, i64 0, i32 1, i64 0
  store i64 %5, i64* %13, align 8, !alias.scope !597, !noalias !587
  %14 = getelementptr inbounds %"core::result::Result<(), collections::TryReserveError>", %"core::result::Result<(), collections::TryReserveError>"* %0, i64 0, i32 1, i64 1
  store i64 0, i64* %14, align 8, !alias.scope !597, !noalias !587
  br label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17try_reserve_exact17h0654327a5aa1fc1eE.exit"

bb16.i.i.i:                                       ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i.i.i"
  %15 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_22.i.i.i, i64 0, i32 1
  %16 = bitcast [2 x i64]* %15 to i8**
  %v.0.i44.i.i.i = load i8*, i8** %16, align 8, !alias.scope !592, !noalias !595, !nonnull !20
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %7) #26, !noalias !586
  store i8* %v.0.i44.i.i.i, i8** %self.idx.i.i.i, align 8, !alias.scope !600, !noalias !588
  store i64 %5, i64* %self.idx.i.i, align 8, !alias.scope !600, !noalias !588
  br label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17try_reserve_exact17h0654327a5aa1fc1eE.exit"

bb18.i.i.i:                                       ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E.exit.i.i.i"
  %17 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, collections::TryReserveError>"* %_22.i.i.i, i64 0, i32 1, i64 0
  %18 = bitcast i64* %17 to <2 x i64>*
  %19 = load <2 x i64>, <2 x i64>* %18, align 8, !alias.scope !592, !noalias !595
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %7) #26, !noalias !586
  %20 = getelementptr inbounds %"core::result::Result<(), collections::TryReserveError>", %"core::result::Result<(), collections::TryReserveError>"* %0, i64 0, i32 1, i64 0
  %21 = bitcast i64* %20 to <2 x i64>*
  store <2 x i64> %19, <2 x i64>* %21, align 8, !alias.scope !603, !noalias !587
  br label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17try_reserve_exact17h0654327a5aa1fc1eE.exit"

"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17try_reserve_exact17h0654327a5aa1fc1eE.exit": ; preds = %start, %bb10.i.i.i, %bb16.i.i.i, %bb18.i.i.i
  %.sink.i.i = phi i64 [ 0, %start ], [ 1, %bb10.i.i.i ], [ 1, %bb18.i.i.i ], [ 0, %bb16.i.i.i ]
  %22 = getelementptr inbounds %"core::result::Result<(), collections::TryReserveError>", %"core::result::Result<(), collections::TryReserveError>"* %0, i64 0, i32 0
  store i64 %.sink.i.i, i64* %22, align 8, !alias.scope !580, !noalias !579
  ret void
}

; alloc::string::String::push
; Function Attrs: inlinehint nonlazybind uwtable
define internal fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nocapture align 8 dereferenceable(24) %self, i32 %ch) unnamed_addr #13 personality i32 (...)* @rust_eh_personality {
start:
  %_17 = alloca i32, align 4
  %0 = icmp ult i32 %ch, 1114112
  tail call void @llvm.assume(i1 %0) #26
  %_2.i.i = icmp ult i32 %ch, 128
  br i1 %_2.i.i, label %bb3, label %bb2.i.i.i

bb3:                                              ; preds = %start
  %_6 = trunc i32 %ch to i8
  tail call void @llvm.experimental.noalias.scope.decl(metadata !606)
  %1 = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 1
  %_4.i = load i64, i64* %1, align 8, !alias.scope !606
  %_6.idx.i = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 1
  %_6.idx.val.i = load i64, i64* %_6.idx.i, align 8, !alias.scope !606
  %_3.i = icmp eq i64 %_4.i, %_6.idx.val.i
  br i1 %_3.i, label %bb2.i, label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$4push17h009fa766e4e00c1aE.exit"

bb2.i:                                            ; preds = %bb3
  %_6.i = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0
; call alloc::raw_vec::RawVec<T,A>::reserve_for_push
  tail call fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$16reserve_for_push17hb577e8edd788625cE"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_6.i, i64 %_4.i)
  %_13.pre.i = load i64, i64* %1, align 8, !alias.scope !606
  br label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$4push17h009fa766e4e00c1aE.exit"

"_ZN5alloc3vec16Vec$LT$T$C$A$GT$4push17h009fa766e4e00c1aE.exit": ; preds = %bb3, %bb2.i
  %_13.i = phi i64 [ %_13.pre.i, %bb2.i ], [ %_4.i, %bb3 ]
  %_2.idx.i.i = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 0
  %_2.idx.val.i.i = load i8*, i8** %_2.idx.i.i, align 8, !alias.scope !609, !nonnull !20
  %2 = getelementptr inbounds i8, i8* %_2.idx.val.i.i, i64 %_13.i
  store i8 %_6, i8* %2, align 1, !noalias !606
  %3 = add i64 %_13.i, 1
  store i64 %3, i64* %1, align 8, !alias.scope !606
  br label %bb8

bb2.i.i.i:                                        ; preds = %start
  %_17.0.sroa_cast5 = bitcast i32* %_17 to i8*
  call void @llvm.lifetime.start.p0i8(i64 4, i8* nonnull %_17.0.sroa_cast5)
  store i32 0, i32* %_17, align 4
  %_4.i.i.i = icmp ult i32 %ch, 2048
  br i1 %_4.i.i.i, label %bb5.i.i, label %bb4.i.i.i

bb4.i.i.i:                                        ; preds = %bb2.i.i.i
  %_6.i.i.i = icmp ult i32 %ch, 65536
  br i1 %_6.i.i.i, label %bb6.i.i, label %bb7.i.i

bb5.i.i:                                          ; preds = %bb2.i.i.i
  %_30.i.i = lshr i32 %ch, 6
  %4 = trunc i32 %_30.i.i to i8
  %5 = or i8 %4, -64
  store i8 %5, i8* %_17.0.sroa_cast5, align 4, !alias.scope !612
  %6 = trunc i32 %ch to i8
  %_32.i.i = and i8 %6, 63
  %7 = or i8 %_32.i.i, -128
  %_17.1.sroa_raw_idx10 = getelementptr inbounds i8, i8* %_17.0.sroa_cast5, i64 1
  store i8 %7, i8* %_17.1.sroa_raw_idx10, align 1, !alias.scope !612
  br label %"_ZN4core4char7methods22_$LT$impl$u20$char$GT$11encode_utf817h03909e731d588b87E.exit"

bb6.i.i:                                          ; preds = %bb4.i.i.i
  %_40.i.i = lshr i32 %ch, 12
  %8 = trunc i32 %_40.i.i to i8
  %9 = or i8 %8, -32
  store i8 %9, i8* %_17.0.sroa_cast5, align 4, !alias.scope !612
  %_44.i.i = lshr i32 %ch, 6
  %10 = trunc i32 %_44.i.i to i8
  %_42.i.i = and i8 %10, 63
  %11 = or i8 %_42.i.i, -128
  %_17.1.sroa_raw_idx8 = getelementptr inbounds i8, i8* %_17.0.sroa_cast5, i64 1
  store i8 %11, i8* %_17.1.sroa_raw_idx8, align 1, !alias.scope !612
  %12 = trunc i32 %ch to i8
  %_46.i.i = and i8 %12, 63
  %13 = or i8 %_46.i.i, -128
  %_17.2.sroa_raw_idx12 = getelementptr inbounds i8, i8* %_17.0.sroa_cast5, i64 2
  store i8 %13, i8* %_17.2.sroa_raw_idx12, align 2, !alias.scope !612
  br label %"_ZN4core4char7methods22_$LT$impl$u20$char$GT$11encode_utf817h03909e731d588b87E.exit"

bb7.i.i:                                          ; preds = %bb4.i.i.i
  %_55.i.i = lshr i32 %ch, 18
  %14 = trunc i32 %_55.i.i to i8
  %15 = or i8 %14, -16
  store i8 %15, i8* %_17.0.sroa_cast5, align 4, !alias.scope !612
  %_59.i.i = lshr i32 %ch, 12
  %16 = trunc i32 %_59.i.i to i8
  %_57.i.i = and i8 %16, 63
  %17 = or i8 %_57.i.i, -128
  %_17.1.sroa_raw_idx = getelementptr inbounds i8, i8* %_17.0.sroa_cast5, i64 1
  store i8 %17, i8* %_17.1.sroa_raw_idx, align 1, !alias.scope !612
  %_63.i.i = lshr i32 %ch, 6
  %18 = trunc i32 %_63.i.i to i8
  %_61.i.i = and i8 %18, 63
  %19 = or i8 %_61.i.i, -128
  %_17.2.sroa_raw_idx = getelementptr inbounds i8, i8* %_17.0.sroa_cast5, i64 2
  store i8 %19, i8* %_17.2.sroa_raw_idx, align 2, !alias.scope !612
  %20 = trunc i32 %ch to i8
  %_65.i.i = and i8 %20, 63
  %21 = or i8 %_65.i.i, -128
  %_17.3.sroa_raw_idx = getelementptr inbounds i8, i8* %_17.0.sroa_cast5, i64 3
  store i8 %21, i8* %_17.3.sroa_raw_idx, align 1, !alias.scope !612
  br label %"_ZN4core4char7methods22_$LT$impl$u20$char$GT$11encode_utf817h03909e731d588b87E.exit"

"_ZN4core4char7methods22_$LT$impl$u20$char$GT$11encode_utf817h03909e731d588b87E.exit": ; preds = %bb5.i.i, %bb6.i.i, %bb7.i.i
  %.0.i5.i.i = phi i64 [ 2, %bb5.i.i ], [ 3, %bb6.i.i ], [ 4, %bb7.i.i ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !617)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !620)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !623)
  %22 = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 1
  %_5.i.i.i.i = load i64, i64* %22, align 8, !alias.scope !626, !noalias !629
  %self.idx.i.i.i.i.i = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 1
  %self.idx.val.i.i.i.i.i = load i64, i64* %self.idx.i.i.i.i.i, align 8, !alias.scope !631, !noalias !629
  %23 = sub i64 %self.idx.val.i.i.i.i.i, %_5.i.i.i.i
  %24 = icmp ult i64 %23, %.0.i5.i.i
  br i1 %24, label %bb2.i.i.i.i.i, label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E.exit"

bb2.i.i.i.i.i:                                    ; preds = %"_ZN4core4char7methods22_$LT$impl$u20$char$GT$11encode_utf817h03909e731d588b87E.exit"
  %_4.i.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0
; call alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  tail call fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i, i64 %_5.i.i.i.i, i64 %.0.i5.i.i), !noalias !629
  %self.idx.val.pre.i.i.i = load i64, i64* %22, align 8, !alias.scope !634, !noalias !629
  br label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E.exit"

"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E.exit": ; preds = %"_ZN4core4char7methods22_$LT$impl$u20$char$GT$11encode_utf817h03909e731d588b87E.exit", %bb2.i.i.i.i.i
  %self.idx.val.i.i.i = phi i64 [ %_5.i.i.i.i, %"_ZN4core4char7methods22_$LT$impl$u20$char$GT$11encode_utf817h03909e731d588b87E.exit" ], [ %self.idx.val.pre.i.i.i, %bb2.i.i.i.i.i ]
  %_2.idx.i.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 0
  %_2.idx.val.i.i.i.i = load i8*, i8** %_2.idx.i.i.i.i, align 8, !alias.scope !635, !noalias !629, !nonnull !20
  %25 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i, i64 %self.idx.val.i.i.i
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 1 %25, i8* noundef nonnull align 4 %_17.0.sroa_cast5, i64 %.0.i5.i.i, i1 false) #26, !noalias !634
  %26 = add i64 %self.idx.val.i.i.i, %.0.i5.i.i
  store i64 %26, i64* %22, align 8, !alias.scope !634, !noalias !629
  call void @llvm.lifetime.end.p0i8(i64 4, i8* nonnull %_17.0.sroa_cast5)
  br label %bb8

bb8:                                              ; preds = %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$4push17h009fa766e4e00c1aE.exit", %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E.exit"
  ret void
}

; <alloc::string::String::retain::SetLenOnDrop as core::ops::drop::Drop>::drop
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN85_$LT$alloc..string..String..retain..SetLenOnDrop$u20$as$u20$core..ops..drop..Drop$GT$4drop17hc7a317394c581850E"(%"string::String::retain::SetLenOnDrop"* noalias nocapture readonly align 8 dereferenceable(24) %self) unnamed_addr #9 {
start:
  %0 = getelementptr inbounds %"string::String::retain::SetLenOnDrop", %"string::String::retain::SetLenOnDrop"* %self, i64 0, i32 1
  %_3 = load i64, i64* %0, align 8
  %1 = getelementptr inbounds %"string::String::retain::SetLenOnDrop", %"string::String::retain::SetLenOnDrop"* %self, i64 0, i32 2
  %_4 = load i64, i64* %1, align 8
  %new_len = sub i64 %_3, %_4
  %2 = bitcast %"string::String::retain::SetLenOnDrop"* %self to %"vec::Vec<u8>"**
  %3 = load %"vec::Vec<u8>"*, %"vec::Vec<u8>"** %2, align 8, !nonnull !20
  %4 = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %3, i64 0, i32 1
  store i64 %new_len, i64* %4, align 8, !alias.scope !638
  ret void
}

; alloc::string::String::insert_bytes
; Function Attrs: nonlazybind uwtable
define void @_ZN5alloc6string6String12insert_bytes17hda0abd97248832cfE(%"string::String"* noalias nocapture align 8 dereferenceable(24) %self, i64 %idx, [0 x i8]* noalias nocapture nonnull readonly align 1 %bytes.0, i64 %bytes.1) unnamed_addr #10 {
start:
  %_2.idx.i = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 1
  %_2.idx.val.i = load i64, i64* %_2.idx.i, align 8
  %self.idx.i.i = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 1
  %self.idx.val.i.i = load i64, i64* %self.idx.i.i, align 8, !alias.scope !641
  %0 = sub i64 %self.idx.val.i.i, %_2.idx.val.i
  %1 = icmp ult i64 %0, %bytes.1
  br i1 %1, label %bb2.i.i, label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E.exit"

bb2.i.i:                                          ; preds = %start
  %_4.i = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0
; call alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  tail call fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i, i64 %_2.idx.val.i, i64 %bytes.1)
  br label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E.exit"

"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E.exit": ; preds = %start, %bb2.i.i
  %_3.idx.i = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 0
  %_3.idx.val.i = load i8*, i8** %_3.idx.i, align 8, !alias.scope !646, !nonnull !20
  %2 = getelementptr inbounds i8, i8* %_3.idx.val.i, i64 %idx
  %_19 = add i64 %bytes.1, %idx
  %3 = getelementptr inbounds i8, i8* %_3.idx.val.i, i64 %_19
  %_22 = sub i64 %_2.idx.val.i, %idx
  tail call void @llvm.memmove.p0i8.p0i8.i64(i8* nonnull align 1 %3, i8* nonnull align 1 %2, i64 %_22, i1 false) #26
  %4 = getelementptr [0 x i8], [0 x i8]* %bytes.0, i64 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %2, i8* nonnull align 1 %4, i64 %bytes.1, i1 false) #26
  %_35 = add i64 %_2.idx.val.i, %bytes.1
  store i64 %_35, i64* %_2.idx.i, align 8, !alias.scope !649
  ret void
}

; alloc::string::FromUtf8Error::as_bytes
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define { [0 x i8]*, i64 } @_ZN5alloc6string13FromUtf8Error8as_bytes17hf0b35e010d63ad7fE(%"string::FromUtf8Error"* noalias nocapture readonly align 8 dereferenceable(40) %self) unnamed_addr #14 personality i32 (...)* @rust_eh_personality {
start:
  %0 = bitcast %"string::FromUtf8Error"* %self to [0 x i8]**
  %_3.idx.val2 = load [0 x i8]*, [0 x i8]** %0, align 8, !alias.scope !652
  %_3.idx1 = getelementptr %"string::FromUtf8Error", %"string::FromUtf8Error"* %self, i64 0, i32 0, i32 1
  %_3.idx1.val = load i64, i64* %_3.idx1, align 8
  %1 = insertvalue { [0 x i8]*, i64 } undef, [0 x i8]* %_3.idx.val2, 0
  %2 = insertvalue { [0 x i8]*, i64 } %1, i64 %_3.idx1.val, 1
  ret { [0 x i8]*, i64 } %2
}

; alloc::string::FromUtf8Error::into_bytes
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @_ZN5alloc6string13FromUtf8Error10into_bytes17haf3eff8d8b42cc57E(%"vec::Vec<u8>"* noalias nocapture sret(%"vec::Vec<u8>") dereferenceable(24) %0, %"string::FromUtf8Error"* noalias nocapture readonly dereferenceable(40) %self) unnamed_addr #9 {
start:
  %1 = bitcast %"vec::Vec<u8>"* %0 to i8*
  %2 = bitcast %"string::FromUtf8Error"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %1, i8* noundef nonnull align 8 dereferenceable(24) %2, i64 24, i1 false)
  ret void
}

; alloc::string::FromUtf8Error::utf8_error
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i128 @_ZN5alloc6string13FromUtf8Error10utf8_error17h08ddf5f99a96445eE(%"string::FromUtf8Error"* noalias nocapture readonly align 8 dereferenceable(40) %self) unnamed_addr #14 {
start:
  %.sroa.0.0..sroa_idx = getelementptr inbounds %"string::FromUtf8Error", %"string::FromUtf8Error"* %self, i64 0, i32 1
  %.sroa.0.0..sroa_cast = bitcast %"core::str::error::Utf8Error"* %.sroa.0.0..sroa_idx to i128*
  %.sroa.0.0.copyload = load i128, i128* %.sroa.0.0..sroa_cast, align 8
  ret i128 %.sroa.0.0.copyload
}

; <alloc::string::FromUtf8Error as core::fmt::Display>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN67_$LT$alloc..string..FromUtf8Error$u20$as$u20$core..fmt..Display$GT$3fmt17hd82c89363e4efd98E"(%"string::FromUtf8Error"* noalias readonly align 8 dereferenceable(40) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %_4 = getelementptr inbounds %"string::FromUtf8Error", %"string::FromUtf8Error"* %self, i64 0, i32 1
; call <core::str::error::Utf8Error as core::fmt::Display>::fmt
  %0 = tail call zeroext i1 @"_ZN66_$LT$core..str..error..Utf8Error$u20$as$u20$core..fmt..Display$GT$3fmt17h498c1679916781e1E"(%"core::str::error::Utf8Error"* noalias nonnull readonly align 8 dereferenceable(16) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %0
}

; <alloc::string::FromUtf16Error as core::fmt::Display>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN68_$LT$alloc..string..FromUtf16Error$u20$as$u20$core..fmt..Display$GT$3fmt17h7e1e3d353db6d76cE"(%"string::FromUtf16Error"* noalias nocapture nonnull readonly align 1 %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
; call <str as core::fmt::Display>::fmt
  %0 = tail call zeroext i1 @"_ZN42_$LT$str$u20$as$u20$core..fmt..Display$GT$3fmt17h6e40a2e7e798ff25E"([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [36 x i8] }>* @alloc3046 to [0 x i8]*), i64 36, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %0
}

; <alloc::string::String as core::clone::Clone>::clone
; Function Attrs: nounwind nonlazybind uwtable
define void @"_ZN60_$LT$alloc..string..String$u20$as$u20$core..clone..Clone$GT$5clone17h08b523dcf2bf1390E"(%"string::String"* noalias nocapture sret(%"string::String") dereferenceable(24) %0, %"string::String"* noalias nocapture readonly align 8 dereferenceable(24) %self) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %_3.idx = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 0
  %_3.idx.val = load i8*, i8** %_3.idx, align 8, !alias.scope !652
  %_3.idx1 = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 1
  %_3.idx1.val = load i64, i64* %_3.idx1, align 8
  %1 = icmp eq i64 %_3.idx1.val, 0
  br i1 %1, label %"_ZN67_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..clone..Clone$GT$5clone17h57ee0b5826626015E.exit", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i": ; preds = %start
  %2 = tail call i8* @__rust_alloc(i64 %_3.idx1.val, i64 1) #26, !noalias !655
  %3 = insertvalue { i8*, i64 } undef, i8* %2, 0
  %4 = icmp eq i8* %2, null
  br i1 %4, label %bb20.i.i.i.i.i.i.i, label %"_ZN67_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..clone..Clone$GT$5clone17h57ee0b5826626015E.exit"

bb20.i.i.i.i.i.i.i:                               ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %_3.idx1.val, i64 1) #27, !noalias !655
  unreachable

"_ZN67_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..clone..Clone$GT$5clone17h57ee0b5826626015E.exit": ; preds = %start, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i"
  %.pn.i.i.i.i.i.i.i = phi { i8*, i64 } [ %3, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %start ]
  %_3.0.i.i.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i.i.i, 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %_3.0.i.i.i.i.i, i8* nonnull align 1 %_3.idx.val, i64 %_3.idx1.val, i1 false) #26, !noalias !669
  %_2.sroa.0.0..sroa_idx = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 0, i32 0
  store i8* %_3.0.i.i.i.i.i, i8** %_2.sroa.0.0..sroa_idx, align 8
  %_2.sroa.4.0..sroa_idx3 = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 0, i32 1
  store i64 %_3.idx1.val, i64* %_2.sroa.4.0..sroa_idx3, align 8
  %_2.sroa.5.0..sroa_idx5 = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 1
  store i64 %_3.idx1.val, i64* %_2.sroa.5.0..sroa_idx5, align 8
  ret void
}

; <alloc::string::String as core::clone::Clone>::clone_from
; Function Attrs: nonlazybind uwtable
define void @"_ZN60_$LT$alloc..string..String$u20$as$u20$core..clone..Clone$GT$10clone_from17h57db484173949eccE"(%"string::String"* noalias nocapture align 8 dereferenceable(24) %self, %"string::String"* noalias nocapture readonly align 8 dereferenceable(24) %source) unnamed_addr #10 {
start:
  %_6.idx = getelementptr inbounds %"string::String", %"string::String"* %source, i64 0, i32 0, i32 0, i32 0
  %_6.idx.val = load i8*, i8** %_6.idx, align 8, !alias.scope !652
  %_6.idx1 = getelementptr %"string::String", %"string::String"* %source, i64 0, i32 0, i32 1
  %_6.idx1.val = load i64, i64* %_6.idx1, align 8
  tail call void @llvm.experimental.noalias.scope.decl(metadata !670)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !673)
  %0 = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 1
  store i64 0, i64* %0, align 8, !alias.scope !676
  tail call void @llvm.experimental.noalias.scope.decl(metadata !681)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !684)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !687)
  %self.idx.i.i.i.i.i.i.i = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 1
  %self.idx.val.i.i.i.i.i.i.i = load i64, i64* %self.idx.i.i.i.i.i.i.i, align 8, !alias.scope !690, !noalias !695
  %1 = icmp ult i64 %self.idx.val.i.i.i.i.i.i.i, %_6.idx1.val
  br i1 %1, label %bb2.i.i.i.i.i.i.i, label %"_ZN67_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..clone..Clone$GT$10clone_from17hbe68e0b75b488abaE.exit"

bb2.i.i.i.i.i.i.i:                                ; preds = %start
  %_4.i.i.i.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0
; call alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  tail call fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i.i, i64 0, i64 %_6.idx1.val), !noalias !695
  %self.idx.val.pre.i.i.i.i.i = load i64, i64* %0, align 8, !alias.scope !697, !noalias !695
  br label %"_ZN67_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..clone..Clone$GT$10clone_from17hbe68e0b75b488abaE.exit"

"_ZN67_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..clone..Clone$GT$10clone_from17hbe68e0b75b488abaE.exit": ; preds = %start, %bb2.i.i.i.i.i.i.i
  %self.idx.val.i.i.i.i.i = phi i64 [ 0, %start ], [ %self.idx.val.pre.i.i.i.i.i, %bb2.i.i.i.i.i.i.i ]
  %_2.idx.i.i.i.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %self, i64 0, i32 0, i32 0, i32 0
  %_2.idx.val.i.i.i.i.i.i = load i8*, i8** %_2.idx.i.i.i.i.i.i, align 8, !alias.scope !698, !noalias !695, !nonnull !20
  %2 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i.i, i64 %self.idx.val.i.i.i.i.i
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %2, i8* nonnull align 1 %_6.idx.val, i64 %_6.idx1.val, i1 false) #26, !noalias !697
  %3 = add i64 %self.idx.val.i.i.i.i.i, %_6.idx1.val
  store i64 %3, i64* %0, align 8, !alias.scope !697, !noalias !695
  ret void
}

; <&alloc::string::String as core::str::pattern::Pattern>::into_searcher
; Function Attrs: nonlazybind uwtable
define void @"_ZN73_$LT$$RF$alloc..string..String$u20$as$u20$core..str..pattern..Pattern$GT$13into_searcher17he7baaa75e27a7485E"(%"core::str::pattern::StrSearcher"* noalias nocapture sret(%"core::str::pattern::StrSearcher") dereferenceable(104) %0, %"string::String"* noalias nocapture readonly align 8 dereferenceable(24) %self, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #10 {
start:
  %1 = bitcast %"string::String"* %self to [0 x i8]**
  %_6.idx.val2.i = load [0 x i8]*, [0 x i8]** %1, align 8, !alias.scope !701
  %_6.idx1.i = getelementptr %"string::String", %"string::String"* %self, i64 0, i32 0, i32 1
  %_6.idx1.val.i = load i64, i64* %_6.idx1.i, align 8, !alias.scope !706
; call core::str::pattern::StrSearcher::new
  tail call void @_ZN4core3str7pattern11StrSearcher3new17h6ae7735913737082E(%"core::str::pattern::StrSearcher"* noalias nocapture nonnull sret(%"core::str::pattern::StrSearcher") dereferenceable(104) %0, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1, [0 x i8]* noalias nonnull readonly align 1 %_6.idx.val2.i, i64 %_6.idx1.val.i)
  ret void
}

; <alloc::string::String as core::convert::From<alloc::boxed::Box<str>>>::from
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN97_$LT$alloc..string..String$u20$as$u20$core..convert..From$LT$alloc..boxed..Box$LT$str$GT$$GT$$GT$4from17h97b8b9ac16df6f8bE"(%"string::String"* noalias nocapture sret(%"string::String") dereferenceable(24) %0, [0 x i8]* noalias nonnull align 1 %s.0, i64 %s.1) unnamed_addr #9 personality i32 (...)* @rust_eh_personality {
start:
  %1 = getelementptr [0 x i8], [0 x i8]* %s.0, i64 0, i64 0
  %_4.sroa.0.0..sroa_idx.i = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 0, i32 0
  store i8* %1, i8** %_4.sroa.0.0..sroa_idx.i, align 8, !alias.scope !707, !noalias !713
  %_4.sroa.4.0..sroa_idx3.i = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 0, i32 1
  store i64 %s.1, i64* %_4.sroa.4.0..sroa_idx3.i, align 8, !alias.scope !707, !noalias !713
  %_4.sroa.5.0..sroa_idx5.i = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 1
  store i64 %s.1, i64* %_4.sroa.5.0..sroa_idx5.i, align 8, !alias.scope !707, !noalias !713
  ret void
}

; alloc::string::<impl core::convert::From<alloc::string::String> for alloc::boxed::Box<str>>::from
; Function Attrs: nounwind nonlazybind uwtable
define { [0 x i8]*, i64 } @"_ZN5alloc6string107_$LT$impl$u20$core..convert..From$LT$alloc..string..String$GT$$u20$for$u20$alloc..boxed..Box$LT$str$GT$$GT$4from17h9c8892a00e4edf5aE"(%"string::String"* noalias nocapture readonly dereferenceable(24) %s) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %_2.sroa.0.0..sroa_idx = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0, i32 0
  %_2.sroa.0.0.copyload = load i8*, i8** %_2.sroa.0.0..sroa_idx, align 8
  %_2.sroa.4.0..sroa_idx2 = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 0, i32 1
  %_2.sroa.4.0.copyload = load i64, i64* %_2.sroa.4.0..sroa_idx2, align 8
  %_2.sroa.5.0..sroa_idx4 = getelementptr inbounds %"string::String", %"string::String"* %s, i64 0, i32 0, i32 1
  %_2.sroa.5.0.copyload = load i64, i64* %_2.sroa.5.0..sroa_idx4, align 8
  %_2.i.i.i = icmp ule i64 %_2.sroa.4.0.copyload, %_2.sroa.5.0.copyload
  %.not.i.i.i.i.i = icmp eq i8* %_2.sroa.0.0.copyload, null
  %or.cond.i = select i1 %_2.i.i.i, i1 true, i1 %.not.i.i.i.i.i
  br i1 %or.cond.i, label %_ZN5alloc6string6String14into_boxed_str17hb5bc62edbdcb4638E.exit, label %bb5.i.i.i.i.i

bb5.i.i.i.i.i:                                    ; preds = %start
  %0 = icmp eq i64 %_2.sroa.5.0.copyload, 0
  br i1 %0, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i.i", label %bb9.i.i.i.i.i.i

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i.i": ; preds = %bb5.i.i.i.i.i
  tail call void @__rust_dealloc(i8* nonnull %_2.sroa.0.0.copyload, i64 %_2.sroa.4.0.copyload, i64 1) #26, !noalias !715
  br label %_ZN5alloc6string6String14into_boxed_str17hb5bc62edbdcb4638E.exit

bb9.i.i.i.i.i.i:                                  ; preds = %bb5.i.i.i.i.i
  %1 = tail call i8* @__rust_realloc(i8* nonnull %_2.sroa.0.0.copyload, i64 %_2.sroa.4.0.copyload, i64 1, i64 %_2.sroa.5.0.copyload) #26, !noalias !715
  %2 = icmp eq i8* %1, null
  br i1 %2, label %bb6.i.i.i.i.i, label %_ZN5alloc6string6String14into_boxed_str17hb5bc62edbdcb4638E.exit

bb6.i.i.i.i.i:                                    ; preds = %bb9.i.i.i.i.i.i
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %_2.sroa.5.0.copyload, i64 1) #27, !noalias !727
  unreachable

_ZN5alloc6string6String14into_boxed_str17hb5bc62edbdcb4638E.exit: ; preds = %start, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i.i", %bb9.i.i.i.i.i.i
  %_5.sroa.0.0.copyload24.i.in.i = phi i8* [ %_2.sroa.0.0.copyload, %start ], [ inttoptr (i64 1 to i8*), %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i.i.i" ], [ %1, %bb9.i.i.i.i.i.i ]
  %_5.sroa.0.0.copyload24.i.i = bitcast i8* %_5.sroa.0.0.copyload24.i.in.i to [0 x i8]*
  %3 = insertvalue { [0 x i8]*, i64 } undef, [0 x i8]* %_5.sroa.0.0.copyload24.i.i, 0
  %4 = insertvalue { [0 x i8]*, i64 } %3, i64 %_2.sroa.5.0.copyload, 1
  ret { [0 x i8]*, i64 } %4
}

; <alloc::string::String as core::convert::From<alloc::borrow::Cow<str>>>::from
; Function Attrs: nounwind nonlazybind uwtable
define void @"_ZN98_$LT$alloc..string..String$u20$as$u20$core..convert..From$LT$alloc..borrow..Cow$LT$str$GT$$GT$$GT$4from17hee37f6aad740449eE"(%"string::String"* noalias nocapture sret(%"string::String") dereferenceable(24) %0, %"borrow::Cow<str>"* noalias nocapture readonly dereferenceable(32) %s) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %_2.sroa.0.0..sroa_idx = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %s, i64 0, i32 0
  %_2.sroa.0.0.copyload = load i64, i64* %_2.sroa.0.0..sroa_idx, align 8
  %_2.sroa.4.0..sroa_idx2 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %s, i64 0, i32 1
  %_2.sroa.4.0..sroa_cast = bitcast [3 x i64]* %_2.sroa.4.0..sroa_idx2 to [0 x i8]**
  %_2.sroa.4.0.copyload = load [0 x i8]*, [0 x i8]** %_2.sroa.4.0..sroa_cast, align 8
  %_2.sroa.6.0..sroa_idx4 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %s, i64 0, i32 1, i64 1
  %_2.sroa.6.0.copyload = load i64, i64* %_2.sroa.6.0..sroa_idx4, align 8
  %_2.sroa.7.0..sroa_idx7 = getelementptr inbounds %"borrow::Cow<str>", %"borrow::Cow<str>"* %s, i64 0, i32 1, i64 2
  %_2.sroa.7.0.copyload = load i64, i64* %_2.sroa.7.0..sroa_idx7, align 8
  tail call void @llvm.experimental.noalias.scope.decl(metadata !730)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !733)
  %switch.not.i = icmp eq i64 %_2.sroa.0.0.copyload, 1
  br i1 %switch.not.i, label %bb1.i, label %bb3.i

bb3.i:                                            ; preds = %start
  %1 = icmp ne [0 x i8]* %_2.sroa.4.0.copyload, null
  tail call void @llvm.assume(i1 %1)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !735) #26
  %2 = icmp eq i64 %_2.sroa.6.0.copyload, 0
  br i1 %2, label %"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E.exit.i", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i": ; preds = %bb3.i
  %3 = tail call i8* @__rust_alloc(i64 %_2.sroa.6.0.copyload, i64 1) #26, !noalias !738
  %4 = insertvalue { i8*, i64 } undef, i8* %3, 0
  %5 = icmp eq i8* %3, null
  br i1 %5, label %bb20.i.i.i.i.i.i.i.i.i.i, label %"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E.exit.i"

bb20.i.i.i.i.i.i.i.i.i.i:                         ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %_2.sroa.6.0.copyload, i64 1) #27, !noalias !738
  unreachable

"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E.exit.i": ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i", %bb3.i
  %.pn.i.i.i.i.i.i.i.i.i.i = phi { i8*, i64 } [ %4, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %bb3.i ]
  %_3.0.i.i.i.i.i.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i.i.i.i.i.i, 0
  %6 = getelementptr [0 x i8], [0 x i8]* %_2.sroa.4.0.copyload, i64 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %_3.0.i.i.i.i.i.i.i.i, i8* nonnull align 1 %6, i64 %_2.sroa.6.0.copyload, i1 false) #26, !noalias !757
  %_2.sroa.0.0..sroa_idx.i.i = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 0, i32 0
  store i8* %_3.0.i.i.i.i.i.i.i.i, i8** %_2.sroa.0.0..sroa_idx.i.i, align 8, !alias.scope !758, !noalias !762
  br label %"_ZN5alloc6borrow12Cow$LT$B$GT$10into_owned17h48a33bbc96bb20d5E.exit"

bb1.i:                                            ; preds = %start
  %_2.sroa.4.8..sroa_cast = bitcast %"string::String"* %0 to [0 x i8]**
  store [0 x i8]* %_2.sroa.4.0.copyload, [0 x i8]** %_2.sroa.4.8..sroa_cast, align 8, !alias.scope !763
  br label %"_ZN5alloc6borrow12Cow$LT$B$GT$10into_owned17h48a33bbc96bb20d5E.exit"

"_ZN5alloc6borrow12Cow$LT$B$GT$10into_owned17h48a33bbc96bb20d5E.exit": ; preds = %"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E.exit.i", %bb1.i
  %_2.sroa.6.0.copyload.sink = phi i64 [ %_2.sroa.6.0.copyload, %"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E.exit.i" ], [ %_2.sroa.7.0.copyload, %bb1.i ]
  %_2.sroa.4.0..sroa_idx3.i.i = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 0, i32 1
  store i64 %_2.sroa.6.0.copyload, i64* %_2.sroa.4.0..sroa_idx3.i.i, align 8, !alias.scope !763
  %_2.sroa.5.0..sroa_idx5.i.i = getelementptr inbounds %"string::String", %"string::String"* %0, i64 0, i32 0, i32 1
  store i64 %_2.sroa.6.0.copyload.sink, i64* %_2.sroa.5.0..sroa_idx5.i.i, align 8, !alias.scope !763
  ret void
}

; alloc::string::<impl core::convert::From<alloc::string::String> for alloc::vec::Vec<u8>>::from
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN5alloc6string104_$LT$impl$u20$core..convert..From$LT$alloc..string..String$GT$$u20$for$u20$alloc..vec..Vec$LT$u8$GT$$GT$4from17h89eb5d4df2631f52E"(%"vec::Vec<u8>"* noalias nocapture sret(%"vec::Vec<u8>") dereferenceable(24) %0, %"string::String"* noalias nocapture readonly dereferenceable(24) %string) unnamed_addr #9 {
start:
  %_2.sroa.0.0..sroa_cast = bitcast %"string::String"* %string to i8*
  %1 = bitcast %"vec::Vec<u8>"* %0 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %1, i8* noundef nonnull align 8 dereferenceable(24) %_2.sroa.0.0..sroa_cast, i64 24, i1 false)
  ret void
}

; <alloc::string::Drain as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN57_$LT$alloc..string..Drain$u20$as$u20$core..fmt..Debug$GT$3fmt17h21db72c3e4d56adeE"(%"string::Drain"* noalias nocapture readonly align 8 dereferenceable(40) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %_13 = alloca { [0 x i8]*, i64 }, align 8
  %_6 = alloca %"core::fmt::builders::DebugTuple", align 8
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_6 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_6, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc3049 to [0 x i8]*), i64 5)
  %1 = bitcast { [0 x i8]*, i64 }* %_13 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %1)
  %_2.idx.i = getelementptr inbounds %"string::Drain", %"string::Drain"* %self, i64 0, i32 3, i32 0
  %_2.idx.val.i = load i8*, i8** %_2.idx.i, align 8, !alias.scope !764
  %_2.idx1.i = getelementptr %"string::Drain", %"string::Drain"* %self, i64 0, i32 3, i32 1
  %_2.idx1.val.i = load i8*, i8** %_2.idx1.i, align 8, !alias.scope !764
  %_18.i.i.i.i = ptrtoint i8* %_2.idx1.val.i to i64
  %_20.i.i.i.i = ptrtoint i8* %_2.idx.val.i to i64
  %2 = sub nuw i64 %_18.i.i.i.i, %_20.i.i.i.i
  %3 = bitcast { [0 x i8]*, i64 }* %_13 to i8**
  store i8* %_2.idx.val.i, i8** %3, align 8
  %.fca.1.gep = getelementptr inbounds { [0 x i8]*, i64 }, { [0 x i8]*, i64 }* %_13, i64 0, i32 1
  store i64 %2, i64* %.fca.1.gep, align 8
  %_10.0 = bitcast { [0 x i8]*, i64 }* %_13 to {}*
; call core::fmt::builders::DebugTuple::field
  %_4 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_6, {}* nonnull align 1 %_10.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.0 to [3 x i64]*))
; call core::fmt::builders::DebugTuple::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_4)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %1)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret i1 %4
}

; <alloc::string::Drain as core::ops::drop::Drop>::drop
; Function Attrs: nonlazybind uwtable
define void @"_ZN62_$LT$alloc..string..Drain$u20$as$u20$core..ops..drop..Drop$GT$4drop17h64d1d4e47275564fE"(%"string::Drain"* noalias nocapture readonly align 8 dereferenceable(40) %self) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
start:
  %0 = getelementptr inbounds %"string::Drain", %"string::Drain"* %self, i64 0, i32 0
  %_3 = load %"string::String"*, %"string::String"** %0, align 8
  %1 = getelementptr inbounds %"string::Drain", %"string::Drain"* %self, i64 0, i32 1
  %_6 = load i64, i64* %1, align 8
  %2 = getelementptr inbounds %"string::Drain", %"string::Drain"* %self, i64 0, i32 2
  %_7 = load i64, i64* %2, align 8
  %_5.not = icmp ugt i64 %_6, %_7
  br i1 %_5.not, label %bb9, label %bb3

bb3:                                              ; preds = %start
  %self_vec.idx = getelementptr %"string::String", %"string::String"* %_3, i64 0, i32 0, i32 1
  %self_vec.idx.val = load i64, i64* %self_vec.idx, align 8
  %_8.not = icmp ult i64 %self_vec.idx.val, %_7
  br i1 %_8.not, label %bb9, label %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$5drain17hb17f3f772d3c18a6E.exit"

bb9:                                              ; preds = %bb24.sink.split.i.i, %bb19.i.i3, %bb11.i.i, %start, %bb3
  ret void

"_ZN5alloc3vec16Vec$LT$T$C$A$GT$5drain17hb17f3f772d3c18a6E.exit": ; preds = %bb3
  store i64 %_6, i64* %self_vec.idx, align 8, !alias.scope !767, !noalias !772
  %_2.idx.i.i = getelementptr inbounds %"string::String", %"string::String"* %_3, i64 0, i32 0, i32 0, i32 0
  %_2.idx.val.i.i = load i8*, i8** %_2.idx.i.i, align 8, !alias.scope !774, !noalias !772, !nonnull !20
  %3 = getelementptr inbounds i8, i8* %_2.idx.val.i.i, i64 %_6
  %4 = getelementptr inbounds i8, i8* %_2.idx.val.i.i, i64 %_7
  %_23.i = sub i64 %self_vec.idx.val, %_7
  %5 = icmp eq i64 %_7, %_6
  %_2.not.i.i.i.i = icmp eq i64 %_23.i, 0
  br i1 %5, label %bb11.i.i, label %bb19.i.i3

bb11.i.i:                                         ; preds = %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$5drain17hb17f3f772d3c18a6E.exit"
  br i1 %_2.not.i.i.i.i, label %bb9, label %bb24.sink.split.i.i

bb19.i.i3:                                        ; preds = %"_ZN5alloc3vec16Vec$LT$T$C$A$GT$5drain17hb17f3f772d3c18a6E.exit"
  br i1 %_2.not.i.i.i.i, label %bb9, label %bb4.i.i40.i.i

bb4.i.i40.i.i:                                    ; preds = %bb19.i.i3
  tail call void @llvm.memmove.p0i8.p0i8.i64(i8* nonnull align 1 %3, i8* nonnull align 1 %4, i64 %_23.i, i1 false) #26, !noalias !777
  br label %bb24.sink.split.i.i

bb24.sink.split.i.i:                              ; preds = %bb11.i.i, %bb4.i.i40.i.i
  %_26.i.i35.i.i = add i64 %_23.i, %_6
  store i64 %_26.i.i35.i.i, i64* %self_vec.idx, align 8, !noalias !782
  br label %bb9
}

; alloc::string::Drain::as_str
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define { [0 x i8]*, i64 } @_ZN5alloc6string5Drain6as_str17hf05b746e91bda355E(%"string::Drain"* noalias nocapture readonly align 8 dereferenceable(40) %self) unnamed_addr #14 {
start:
  %_2.idx = getelementptr inbounds %"string::Drain", %"string::Drain"* %self, i64 0, i32 3, i32 0
  %_2.idx.val = load i8*, i8** %_2.idx, align 8
  %_2.idx1 = getelementptr %"string::Drain", %"string::Drain"* %self, i64 0, i32 3, i32 1
  %_2.idx1.val = load i8*, i8** %_2.idx1, align 8
  %_18.i.i.i = ptrtoint i8* %_2.idx1.val to i64
  %_20.i.i.i = ptrtoint i8* %_2.idx.val to i64
  %0 = sub nuw i64 %_18.i.i.i, %_20.i.i.i
  %1 = bitcast i8* %_2.idx.val to [0 x i8]*
  %2 = insertvalue { [0 x i8]*, i64 } undef, [0 x i8]* %1, 0
  %3 = insertvalue { [0 x i8]*, i64 } %2, i64 %0, 1
  ret { [0 x i8]*, i64 } %3
}

; <alloc::string::Drain as core::convert::AsRef<str>>::as_ref
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define { [0 x i8]*, i64 } @"_ZN72_$LT$alloc..string..Drain$u20$as$u20$core..convert..AsRef$LT$str$GT$$GT$6as_ref17h3e9de094f2b2621bE"(%"string::Drain"* noalias nocapture readonly align 8 dereferenceable(40) %self) unnamed_addr #9 {
start:
  %_2.idx.i = getelementptr inbounds %"string::Drain", %"string::Drain"* %self, i64 0, i32 3, i32 0
  %_2.idx.val.i = load i8*, i8** %_2.idx.i, align 8, !alias.scope !783
  %_2.idx1.i = getelementptr %"string::Drain", %"string::Drain"* %self, i64 0, i32 3, i32 1
  %_2.idx1.val.i = load i8*, i8** %_2.idx1.i, align 8, !alias.scope !783
  %_18.i.i.i.i = ptrtoint i8* %_2.idx1.val.i to i64
  %_20.i.i.i.i = ptrtoint i8* %_2.idx.val.i to i64
  %0 = sub nuw i64 %_18.i.i.i.i, %_20.i.i.i.i
  %1 = bitcast i8* %_2.idx.val.i to [0 x i8]*
  %2 = insertvalue { [0 x i8]*, i64 } undef, [0 x i8]* %1, 0
  %3 = insertvalue { [0 x i8]*, i64 } %2, i64 %0, 1
  ret { [0 x i8]*, i64 } %3
}

; <alloc::string::Drain as core::iter::traits::iterator::Iterator>::size_hint
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN79_$LT$alloc..string..Drain$u20$as$u20$core..iter..traits..iterator..Iterator$GT$9size_hint17hc713afef0caf1e6eE"({ i64, { i64, i64 } }* noalias nocapture sret({ i64, { i64, i64 } }) dereferenceable(24) %0, %"string::Drain"* noalias nocapture readonly align 8 dereferenceable(40) %self) unnamed_addr #9 {
start:
  %_2.idx = getelementptr inbounds %"string::Drain", %"string::Drain"* %self, i64 0, i32 3, i32 0
  %_2.idx.val = load i8*, i8** %_2.idx, align 8
  %_2.idx1 = getelementptr %"string::Drain", %"string::Drain"* %self, i64 0, i32 3, i32 1
  %_2.idx1.val = load i8*, i8** %_2.idx1, align 8
  %_14.i.i = ptrtoint i8* %_2.idx1.val to i64
  %_16.i.i = ptrtoint i8* %_2.idx.val to i64
  %1 = sub nuw i64 %_14.i.i, %_16.i.i
  %_5.i = add i64 %1, 3
  %_4.i = lshr i64 %_5.i, 2
  %2 = getelementptr inbounds { i64, { i64, i64 } }, { i64, { i64, i64 } }* %0, i64 0, i32 0
  store i64 %_4.i, i64* %2, align 8, !alias.scope !786
  %3 = getelementptr inbounds { i64, { i64, i64 } }, { i64, { i64, i64 } }* %0, i64 0, i32 1, i32 0
  store i64 1, i64* %3, align 8, !alias.scope !786
  %4 = getelementptr inbounds { i64, { i64, i64 } }, { i64, { i64, i64 } }* %0, i64 0, i32 1, i32 1
  store i64 %1, i64* %4, align 8, !alias.scope !786
  ret void
}

; alloc::vec::Vec<T,A>::swap_remove::assert_failed
; Function Attrs: cold noinline noreturn nonlazybind uwtable
define void @"_ZN5alloc3vec16Vec$LT$T$C$A$GT$11swap_remove13assert_failed17h8b8aac26f3f344e2E"(i64 %0, i64 %1) unnamed_addr #15 {
start:
  %_10 = alloca [2 x { i8*, i64* }], align 8
  %_3 = alloca %"core::fmt::Arguments", align 8
  %len = alloca i64, align 8
  %index = alloca i64, align 8
  store i64 %0, i64* %index, align 8
  store i64 %1, i64* %len, align 8
  %2 = bitcast %"core::fmt::Arguments"* %_3 to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %2)
  %3 = bitcast [2 x { i8*, i64* }]* %_10 to i8*
  call void @llvm.lifetime.start.p0i8(i64 32, i8* nonnull %3)
  %4 = bitcast [2 x { i8*, i64* }]* %_10 to i64**
  store i64* %index, i64** %4, align 8
  %5 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 0, i32 1
  store i64* bitcast (i1 (i64*, %"core::fmt::Formatter"*)* @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E" to i64*), i64** %5, align 8
  %6 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 1, i32 0
  %7 = bitcast i8** %6 to i64**
  store i64* %len, i64** %7, align 8
  %8 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 1, i32 1
  store i64* bitcast (i1 (i64*, %"core::fmt::Formatter"*)* @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E" to i64*), i64** %8, align 8
  %9 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 0, i32 0
  store [0 x { [0 x i8]*, i64 }]* bitcast (<{ i8*, [8 x i8], i8*, [8 x i8], i8*, [8 x i8] }>* @alloc2910 to [0 x { [0 x i8]*, i64 }]*), [0 x { [0 x i8]*, i64 }]** %9, align 8, !alias.scope !789, !noalias !792
  %10 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 0, i32 1
  store i64 3, i64* %10, align 8, !alias.scope !789, !noalias !792
  %11 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 1, i32 0
  store i64* null, i64** %11, align 8, !alias.scope !789, !noalias !792
  %12 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 2, i32 0
  %13 = bitcast [0 x { i8*, i64* }]** %12 to [2 x { i8*, i64* }]**
  store [2 x { i8*, i64* }]* %_10, [2 x { i8*, i64* }]** %13, align 8, !alias.scope !789, !noalias !792
  %14 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 2, i32 1
  store i64 2, i64* %14, align 8, !alias.scope !789, !noalias !792
; call core::panicking::panic_fmt
  call void @_ZN4core9panicking9panic_fmt17he21f9dfe87a034c1E(%"core::fmt::Arguments"* noalias nocapture nonnull dereferenceable(48) %_3, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc3054 to %"core::panic::location::Location"*)) #25
  unreachable
}

; alloc::vec::Vec<T,A>::insert::assert_failed
; Function Attrs: cold noinline noreturn nonlazybind uwtable
define void @"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6insert13assert_failed17h3f5caaafd6d86541E"(i64 %0, i64 %1) unnamed_addr #15 {
start:
  %_10 = alloca [2 x { i8*, i64* }], align 8
  %_3 = alloca %"core::fmt::Arguments", align 8
  %len = alloca i64, align 8
  %index = alloca i64, align 8
  store i64 %0, i64* %index, align 8
  store i64 %1, i64* %len, align 8
  %2 = bitcast %"core::fmt::Arguments"* %_3 to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %2)
  %3 = bitcast [2 x { i8*, i64* }]* %_10 to i8*
  call void @llvm.lifetime.start.p0i8(i64 32, i8* nonnull %3)
  %4 = bitcast [2 x { i8*, i64* }]* %_10 to i64**
  store i64* %index, i64** %4, align 8
  %5 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 0, i32 1
  store i64* bitcast (i1 (i64*, %"core::fmt::Formatter"*)* @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E" to i64*), i64** %5, align 8
  %6 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 1, i32 0
  %7 = bitcast i8** %6 to i64**
  store i64* %len, i64** %7, align 8
  %8 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 1, i32 1
  store i64* bitcast (i1 (i64*, %"core::fmt::Formatter"*)* @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E" to i64*), i64** %8, align 8
  %9 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 0, i32 0
  store [0 x { [0 x i8]*, i64 }]* bitcast (<{ i8*, [8 x i8], i8*, [8 x i8], i8*, [8 x i8] }>* @alloc2917 to [0 x { [0 x i8]*, i64 }]*), [0 x { [0 x i8]*, i64 }]** %9, align 8, !alias.scope !795, !noalias !798
  %10 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 0, i32 1
  store i64 3, i64* %10, align 8, !alias.scope !795, !noalias !798
  %11 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 1, i32 0
  store i64* null, i64** %11, align 8, !alias.scope !795, !noalias !798
  %12 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 2, i32 0
  %13 = bitcast [0 x { i8*, i64* }]** %12 to [2 x { i8*, i64* }]**
  store [2 x { i8*, i64* }]* %_10, [2 x { i8*, i64* }]** %13, align 8, !alias.scope !795, !noalias !798
  %14 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 2, i32 1
  store i64 2, i64* %14, align 8, !alias.scope !795, !noalias !798
; call core::panicking::panic_fmt
  call void @_ZN4core9panicking9panic_fmt17he21f9dfe87a034c1E(%"core::fmt::Arguments"* noalias nocapture nonnull dereferenceable(48) %_3, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc3056 to %"core::panic::location::Location"*)) #25
  unreachable
}

; alloc::vec::Vec<T,A>::remove::assert_failed
; Function Attrs: cold noinline noreturn nonlazybind uwtable
define void @"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6remove13assert_failed17h202d8f99a66b4b37E"(i64 %0, i64 %1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) %2) unnamed_addr #15 {
start:
  %_10 = alloca [2 x { i8*, i64* }], align 8
  %_3 = alloca %"core::fmt::Arguments", align 8
  %len = alloca i64, align 8
  %index = alloca i64, align 8
  store i64 %0, i64* %index, align 8
  store i64 %1, i64* %len, align 8
  %3 = bitcast %"core::fmt::Arguments"* %_3 to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %3)
  %4 = bitcast [2 x { i8*, i64* }]* %_10 to i8*
  call void @llvm.lifetime.start.p0i8(i64 32, i8* nonnull %4)
  %5 = bitcast [2 x { i8*, i64* }]* %_10 to i64**
  store i64* %index, i64** %5, align 8
  %6 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 0, i32 1
  store i64* bitcast (i1 (i64*, %"core::fmt::Formatter"*)* @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E" to i64*), i64** %6, align 8
  %7 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 1, i32 0
  %8 = bitcast i8** %7 to i64**
  store i64* %len, i64** %8, align 8
  %9 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 1, i32 1
  store i64* bitcast (i1 (i64*, %"core::fmt::Formatter"*)* @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E" to i64*), i64** %9, align 8
  %10 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 0, i32 0
  store [0 x { [0 x i8]*, i64 }]* bitcast (<{ i8*, [8 x i8], i8*, [8 x i8], i8*, [8 x i8] }>* @alloc2924 to [0 x { [0 x i8]*, i64 }]*), [0 x { [0 x i8]*, i64 }]** %10, align 8, !alias.scope !801, !noalias !804
  %11 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 0, i32 1
  store i64 3, i64* %11, align 8, !alias.scope !801, !noalias !804
  %12 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 1, i32 0
  store i64* null, i64** %12, align 8, !alias.scope !801, !noalias !804
  %13 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 2, i32 0
  %14 = bitcast [0 x { i8*, i64* }]** %13 to [2 x { i8*, i64* }]**
  store [2 x { i8*, i64* }]* %_10, [2 x { i8*, i64* }]** %14, align 8, !alias.scope !801, !noalias !804
  %15 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 2, i32 1
  store i64 2, i64* %15, align 8, !alias.scope !801, !noalias !804
; call core::panicking::panic_fmt
  call void @_ZN4core9panicking9panic_fmt17he21f9dfe87a034c1E(%"core::fmt::Arguments"* noalias nocapture nonnull dereferenceable(48) %_3, %"core::panic::location::Location"* noalias nonnull readonly align 8 dereferenceable(24) %2) #25
  unreachable
}

; alloc::vec::Vec<T,A>::split_off::assert_failed
; Function Attrs: cold noinline noreturn nonlazybind uwtable
define void @"_ZN5alloc3vec16Vec$LT$T$C$A$GT$9split_off13assert_failed17h8a4731bdaee2864bE"(i64 %0, i64 %1) unnamed_addr #15 {
start:
  %_10 = alloca [2 x { i8*, i64* }], align 8
  %_3 = alloca %"core::fmt::Arguments", align 8
  %len = alloca i64, align 8
  %at = alloca i64, align 8
  store i64 %0, i64* %at, align 8
  store i64 %1, i64* %len, align 8
  %2 = bitcast %"core::fmt::Arguments"* %_3 to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %2)
  %3 = bitcast [2 x { i8*, i64* }]* %_10 to i8*
  call void @llvm.lifetime.start.p0i8(i64 32, i8* nonnull %3)
  %4 = bitcast [2 x { i8*, i64* }]* %_10 to i64**
  store i64* %at, i64** %4, align 8
  %5 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 0, i32 1
  store i64* bitcast (i1 (i64*, %"core::fmt::Formatter"*)* @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E" to i64*), i64** %5, align 8
  %6 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 1, i32 0
  %7 = bitcast i8** %6 to i64**
  store i64* %len, i64** %7, align 8
  %8 = getelementptr inbounds [2 x { i8*, i64* }], [2 x { i8*, i64* }]* %_10, i64 0, i64 1, i32 1
  store i64* bitcast (i1 (i64*, %"core::fmt::Formatter"*)* @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E" to i64*), i64** %8, align 8
  %9 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 0, i32 0
  store [0 x { [0 x i8]*, i64 }]* bitcast (<{ i8*, [8 x i8], i8*, [8 x i8], i8*, [8 x i8] }>* @alloc2931 to [0 x { [0 x i8]*, i64 }]*), [0 x { [0 x i8]*, i64 }]** %9, align 8, !alias.scope !807, !noalias !810
  %10 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 0, i32 1
  store i64 3, i64* %10, align 8, !alias.scope !807, !noalias !810
  %11 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 1, i32 0
  store i64* null, i64** %11, align 8, !alias.scope !807, !noalias !810
  %12 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 2, i32 0
  %13 = bitcast [0 x { i8*, i64* }]** %12 to [2 x { i8*, i64* }]**
  store [2 x { i8*, i64* }]* %_10, [2 x { i8*, i64* }]** %13, align 8, !alias.scope !807, !noalias !810
  %14 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_3, i64 0, i32 2, i32 1
  store i64 2, i64* %14, align 8, !alias.scope !807, !noalias !810
; call core::panicking::panic_fmt
  call void @_ZN4core9panicking9panic_fmt17he21f9dfe87a034c1E(%"core::fmt::Arguments"* noalias nocapture nonnull dereferenceable(48) %_3, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc3060 to %"core::panic::location::Location"*)) #25
  unreachable
}

; <alloc::vec::Vec<u8> as core::convert::From<&str>>::from
; Function Attrs: nounwind nonlazybind uwtable
define void @"_ZN80_$LT$alloc..vec..Vec$LT$u8$GT$$u20$as$u20$core..convert..From$LT$$RF$str$GT$$GT$4from17h2d296c0dfb3f53acE"(%"vec::Vec<u8>"* noalias nocapture sret(%"vec::Vec<u8>") dereferenceable(24) %0, [0 x i8]* noalias nonnull readonly align 1 %s.0, i64 %s.1) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  tail call void @llvm.experimental.noalias.scope.decl(metadata !813)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !816) #26
  tail call void @llvm.experimental.noalias.scope.decl(metadata !819) #26
  tail call void @llvm.experimental.noalias.scope.decl(metadata !822) #26
  tail call void @llvm.experimental.noalias.scope.decl(metadata !825) #26
  tail call void @llvm.experimental.noalias.scope.decl(metadata !828) #26
  %1 = icmp eq i64 %s.1, 0
  br i1 %1, label %"_ZN87_$LT$alloc..vec..Vec$LT$T$GT$$u20$as$u20$core..convert..From$LT$$RF$$u5b$T$u5d$$GT$$GT$4from17hc8bb33dd7aa72e01E.exit", label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i": ; preds = %start
  %2 = tail call i8* @__rust_alloc(i64 %s.1, i64 1) #26, !noalias !831
  %3 = insertvalue { i8*, i64 } undef, i8* %2, 0
  %4 = icmp eq i8* %2, null
  br i1 %4, label %bb20.i.i.i.i.i.i.i.i, label %"_ZN87_$LT$alloc..vec..Vec$LT$T$GT$$u20$as$u20$core..convert..From$LT$$RF$$u5b$T$u5d$$GT$$GT$4from17hc8bb33dd7aa72e01E.exit"

bb20.i.i.i.i.i.i.i.i:                             ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %s.1, i64 1) #27, !noalias !831
  unreachable

"_ZN87_$LT$alloc..vec..Vec$LT$T$GT$$u20$as$u20$core..convert..From$LT$$RF$$u5b$T$u5d$$GT$$GT$4from17hc8bb33dd7aa72e01E.exit": ; preds = %start, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i"
  %.pn.i.i.i.i.i.i.i.i = phi { i8*, i64 } [ %3, %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i" ], [ { i8* inttoptr (i64 1 to i8*), i64 undef }, %start ]
  %_3.0.i.i.i.i.i.i = extractvalue { i8*, i64 } %.pn.i.i.i.i.i.i.i.i, 0
  %5 = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %0, i64 0, i32 0, i32 0
  store i8* %_3.0.i.i.i.i.i.i, i8** %5, align 8, !alias.scope !837, !noalias !838
  %6 = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %0, i64 0, i32 0, i32 1
  store i64 %s.1, i64* %6, align 8, !alias.scope !837, !noalias !838
  %7 = getelementptr inbounds %"vec::Vec<u8>", %"vec::Vec<u8>"* %0, i64 0, i32 1
  %8 = getelementptr [0 x i8], [0 x i8]* %s.0, i64 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %_3.0.i.i.i.i.i.i, i8* nonnull align 1 %8, i64 %s.1, i1 false) #26, !noalias !839
  store i64 %s.1, i64* %7, align 8, !alias.scope !840, !noalias !838
  ret void
}

; <alloc::alloc::Global as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN57_$LT$alloc..alloc..Global$u20$as$u20$core..fmt..Debug$GT$3fmt17hca2db96ef30bcbe2E"(%"alloc::Global"* noalias nocapture nonnull readonly align 1 %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
; call core::fmt::Formatter::write_str
  %0 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc3061 to [0 x i8]*), i64 6)
  ret i1 %0
}

; <alloc::collections::TryReserveError as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN72_$LT$alloc..collections..TryReserveError$u20$as$u20$core..fmt..Debug$GT$3fmt17h406351ea493183fbE"({ i64, i64 }* noalias readonly align 8 dereferenceable(16) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %_16 = alloca { i64, i64 }*, align 8
  %_5 = alloca %"core::fmt::builders::DebugStruct", align 8
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [15 x i8] }>* @alloc3062 to [0 x i8]*), i64 15)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_5 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast { i64, i64 }** %_16 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store { i64, i64 }* %self, { i64, i64 }** %_16, align 8
  %_13.0 = bitcast { i64, i64 }** %_16 to {}*
; call core::fmt::builders::DebugStruct::field
  %_9 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc3063 to [0 x i8]*), i64 4, {}* nonnull align 1 %_13.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.1 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
; call core::fmt::builders::DebugStruct::finish
  %3 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %3
}

; <alloc::collections::TryReserveErrorKind as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN76_$LT$alloc..collections..TryReserveErrorKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h0e4fd2ec2b87f863E"({ i64, i64 }* noalias readonly align 8 dereferenceable(16) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %_31 = alloca {}*, align 8
  %_23 = alloca { i64, i64 }*, align 8
  %_12 = alloca %"core::fmt::builders::DebugStruct", align 8
  %0 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %self, i64 0, i32 1
  %1 = load i64, i64* %0, align 8
  %2 = icmp eq i64 %1, 0
  br i1 %2, label %bb3, label %bb1

bb3:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %3 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [16 x i8] }>* @alloc3076 to [0 x i8]*), i64 16)
  br label %bb9

bb1:                                              ; preds = %start
  %4 = bitcast %"core::fmt::builders::DebugStruct"* %_12 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %4)
; call core::fmt::Formatter::debug_struct
  %5 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [10 x i8] }>* @alloc3067 to [0 x i8]*), i64 10)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_12 to i128*
  store i128 %5, i128* %.0..sroa_cast, align 8
  %6 = bitcast { i64, i64 }** %_23 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %6)
  store { i64, i64 }* %self, { i64, i64 }** %_23, align 8
  %_20.0 = bitcast { i64, i64 }** %_23 to {}*
; call core::fmt::builders::DebugStruct::field
  %_16 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_12, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc3068 to [0 x i8]*), i64 6, {}* nonnull align 1 %_20.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %6)
  %7 = bitcast {}** %_31 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %7)
  %8 = bitcast {}** %_31 to { i64, i64 }**
  store { i64, i64 }* %self, { i64, i64 }** %8, align 8
  %_28.0 = bitcast {}** %_31 to {}*
; call core::fmt::builders::DebugStruct::field
  %_24 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_12, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [14 x i8] }>* @alloc3072 to [0 x i8]*), i64 14, {}* nonnull align 1 %_28.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %7)
; call core::fmt::builders::DebugStruct::finish
  %9 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_12)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %4)
  br label %bb9

bb9:                                              ; preds = %bb3, %bb1
  %.0.in = phi i1 [ %3, %bb3 ], [ %9, %bb1 ]
  ret i1 %.0.in
}

; <alloc::string::FromUtf8Error as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN65_$LT$alloc..string..FromUtf8Error$u20$as$u20$core..fmt..Debug$GT$3fmt17hafbf9b0722e7d76fE"(%"string::FromUtf8Error"* noalias readonly align 8 dereferenceable(40) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %_25 = alloca %"core::str::error::Utf8Error"*, align 8
  %_17 = alloca %"vec::Vec<u8>"*, align 8
  %_6 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds %"string::FromUtf8Error", %"string::FromUtf8Error"* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds %"string::FromUtf8Error", %"string::FromUtf8Error"* %self, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [13 x i8] }>* @alloc3077 to [0 x i8]*), i64 13)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast %"vec::Vec<u8>"** %_17 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store %"vec::Vec<u8>"* %__self_0_0, %"vec::Vec<u8>"** %_17, align 8
  %_14.0 = bitcast %"vec::Vec<u8>"** %_17 to {}*
; call core::fmt::builders::DebugStruct::field
  %_10 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc3078 to [0 x i8]*), i64 5, {}* nonnull align 1 %_14.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.4 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast %"core::str::error::Utf8Error"** %_25 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store %"core::str::error::Utf8Error"* %__self_0_1, %"core::str::error::Utf8Error"** %_25, align 8
  %_22.0 = bitcast %"core::str::error::Utf8Error"** %_25 to {}*
; call core::fmt::builders::DebugStruct::field
  %_18 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc3082 to [0 x i8]*), i64 5, {}* nonnull align 1 %_22.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.5 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %4
}

; <alloc::string::FromUtf16Error as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN66_$LT$alloc..string..FromUtf16Error$u20$as$u20$core..fmt..Debug$GT$3fmt17h9c75519979f80fa0E"(%"string::FromUtf16Error"* noalias nonnull readonly align 1 %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %_14 = alloca {}*, align 8
  %_5 = alloca %"core::fmt::builders::DebugTuple", align 8
  %__self_0_0 = getelementptr %"string::FromUtf16Error", %"string::FromUtf16Error"* %self, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [14 x i8] }>* @alloc3086 to [0 x i8]*), i64 14)
  %1 = bitcast {}** %_14 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1)
  store {}* %__self_0_0, {}** %_14, align 8
  %_11.0 = bitcast {}** %_14 to {}*
; call core::fmt::builders::DebugTuple::field
  %_9 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5, {}* nonnull align 1 %_11.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1)
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret i1 %2
}

; <&mut W as core::fmt::Write>::write_str
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN50_$LT$$RF$mut$u20$W$u20$as$u20$core..fmt..Write$GT$9write_str17h36a7fc6f3a923613E"(%"string::String"** noalias nocapture readonly align 8 dereferenceable(8) %self, [0 x i8]* noalias nocapture nonnull readonly align 1 %s.0, i64 %s.1) unnamed_addr #10 {
start:
  %_3 = load %"string::String"*, %"string::String"** %self, align 8, !nonnull !20
  tail call void @llvm.experimental.noalias.scope.decl(metadata !843)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !846)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !849)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !852)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !855)
  %0 = getelementptr %"string::String", %"string::String"* %_3, i64 0, i32 0, i32 1
  %_5.i.i.i.i.i.i = load i64, i64* %0, align 8, !alias.scope !858, !noalias !861
  %self.idx.i.i.i.i.i.i.i = getelementptr %"string::String", %"string::String"* %_3, i64 0, i32 0, i32 0, i32 1
  %self.idx.val.i.i.i.i.i.i.i = load i64, i64* %self.idx.i.i.i.i.i.i.i, align 8, !alias.scope !865, !noalias !861
  %1 = sub i64 %self.idx.val.i.i.i.i.i.i.i, %_5.i.i.i.i.i.i
  %2 = icmp ult i64 %1, %s.1
  br i1 %2, label %bb2.i.i.i.i.i.i.i, label %"_ZN58_$LT$alloc..string..String$u20$as$u20$core..fmt..Write$GT$9write_str17h3d99b06320bc832dE.exit"

bb2.i.i.i.i.i.i.i:                                ; preds = %start
  %_4.i.i.i.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %_3, i64 0, i32 0, i32 0
; call alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  tail call fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17h949fbaf5f54e50b5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i.i, i64 %_5.i.i.i.i.i.i, i64 %s.1), !noalias !861
  %self.idx.val.pre.i.i.i.i.i = load i64, i64* %0, align 8, !alias.scope !868, !noalias !861
  br label %"_ZN58_$LT$alloc..string..String$u20$as$u20$core..fmt..Write$GT$9write_str17h3d99b06320bc832dE.exit"

"_ZN58_$LT$alloc..string..String$u20$as$u20$core..fmt..Write$GT$9write_str17h3d99b06320bc832dE.exit": ; preds = %start, %bb2.i.i.i.i.i.i.i
  %self.idx.val.i.i.i.i.i = phi i64 [ %_5.i.i.i.i.i.i, %start ], [ %self.idx.val.pre.i.i.i.i.i, %bb2.i.i.i.i.i.i.i ]
  %3 = getelementptr [0 x i8], [0 x i8]* %s.0, i64 0, i64 0
  %_2.idx.i.i.i.i.i.i = getelementptr inbounds %"string::String", %"string::String"* %_3, i64 0, i32 0, i32 0, i32 0
  %_2.idx.val.i.i.i.i.i.i = load i8*, i8** %_2.idx.i.i.i.i.i.i, align 8, !alias.scope !869, !noalias !861, !nonnull !20
  %4 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i.i, i64 %self.idx.val.i.i.i.i.i
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %4, i8* nonnull align 1 %3, i64 %s.1, i1 false) #26, !noalias !868
  %5 = add i64 %self.idx.val.i.i.i.i.i, %s.1
  store i64 %5, i64* %0, align 8, !alias.scope !868, !noalias !861
  ret i1 false
}

; <&mut W as core::fmt::Write>::write_char
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN50_$LT$$RF$mut$u20$W$u20$as$u20$core..fmt..Write$GT$10write_char17h4f4dd9a3c5cb9730E"(%"string::String"** noalias nocapture readonly align 8 dereferenceable(8) %self, i32 %c) unnamed_addr #10 {
start:
  %_3 = load %"string::String"*, %"string::String"** %self, align 8, !nonnull !20
; call alloc::string::String::push
  tail call fastcc void @_ZN5alloc6string6String4push17h2732284d4570ed38E(%"string::String"* noalias nonnull align 8 dereferenceable(24) %_3, i32 %c)
  ret i1 false
}

; <&mut W as core::fmt::Write>::write_fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN50_$LT$$RF$mut$u20$W$u20$as$u20$core..fmt..Write$GT$9write_fmt17hd2f7f4bf7299fe41E"(%"string::String"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Arguments"* noalias nocapture readonly dereferenceable(48) %args) unnamed_addr #10 {
start:
  %_6.i = alloca %"core::fmt::Arguments", align 8
  %self.i = alloca %"string::String"*, align 8
  %_3 = load %"string::String"*, %"string::String"** %self, align 8, !nonnull !20
  %0 = bitcast %"core::fmt::Arguments"* %args to i8*
  %1 = bitcast %"string::String"** %self.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1)
  store %"string::String"* %_3, %"string::String"** %self.i, align 8, !noalias !872
  %_3.0.i = bitcast %"string::String"** %self.i to {}*
  %2 = bitcast %"core::fmt::Arguments"* %_6.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %2), !noalias !872
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(48) %2, i8* noundef nonnull align 8 dereferenceable(48) %0, i64 48, i1 false)
; call core::fmt::write
  %3 = call zeroext i1 @_ZN4core3fmt5write17hc4ca0ba042bde21dE({}* nonnull align 1 %_3.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, i8*, i8*, [0 x i8] }>* @vtable.8 to [3 x i64]*), %"core::fmt::Arguments"* noalias nocapture nonnull dereferenceable(48) %_6.i), !noalias !876
  call void @llvm.lifetime.end.p0i8(i64 48, i8* nonnull %2), !noalias !872
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1)
  ret i1 %3
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h1e750e3acfebf1ccE"(%"core::str::error::Utf8Error"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %_4 = load %"core::str::error::Utf8Error"*, %"core::str::error::Utf8Error"** %self, align 8, !nonnull !20
; call <core::str::error::Utf8Error as core::fmt::Debug>::fmt
  %0 = tail call zeroext i1 @"_ZN64_$LT$core..str..error..Utf8Error$u20$as$u20$core..fmt..Debug$GT$3fmt17h29ccbff883b6b6bbE"(%"core::str::error::Utf8Error"* noalias nonnull readonly align 8 dereferenceable(16) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %0
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h2e266a9c28fc8eaeE"(i8** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %_4 = load i8*, i8** %self, align 8, !nonnull !20
; call core::fmt::Formatter::debug_lower_hex
  %_3.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !877
  br i1 %_3.i, label %bb2.i, label %bb4.i

bb4.i:                                            ; preds = %start
; call core::fmt::Formatter::debug_upper_hex
  %_7.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !877
  br i1 %_7.i, label %bb6.i, label %bb8.i

bb2.i:                                            ; preds = %start
; call core::fmt::num::<impl core::fmt::LowerHex for u8>::fmt
  %0 = tail call zeroext i1 @"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$u8$GT$3fmt17h9ad27865f481f5ecE"(i8* noalias nonnull readonly align 1 dereferenceable(1) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E.exit"

bb8.i:                                            ; preds = %bb4.i
; call core::fmt::num::imp::<impl core::fmt::Display for u8>::fmt
  %1 = tail call zeroext i1 @"_ZN4core3fmt3num3imp51_$LT$impl$u20$core..fmt..Display$u20$for$u20$u8$GT$3fmt17h7657a76892c51730E"(i8* noalias nonnull readonly align 1 dereferenceable(1) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E.exit"

bb6.i:                                            ; preds = %bb4.i
; call core::fmt::num::<impl core::fmt::UpperHex for u8>::fmt
  %2 = tail call zeroext i1 @"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$u8$GT$3fmt17h1f7726c1de44a74fE"(i8* noalias nonnull readonly align 1 dereferenceable(1) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E.exit"

"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E.exit": ; preds = %bb2.i, %bb8.i, %bb6.i
  %.0.in.i = phi i1 [ %0, %bb2.i ], [ %2, %bb6.i ], [ %1, %bb8.i ]
  ret i1 %.0.in.i
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h33d66f6602a37396E"({ i64, i64 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %_31.i = alloca {}*, align 8
  %_23.i = alloca { i64, i64 }*, align 8
  %_12.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load { i64, i64 }*, { i64, i64 }** %self, align 8, !nonnull !20
  tail call void @llvm.experimental.noalias.scope.decl(metadata !880)
  %0 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %_4, i64 0, i32 1
  %1 = load i64, i64* %0, align 8, !alias.scope !880, !noalias !883
  %2 = icmp eq i64 %1, 0
  br i1 %2, label %bb3.i, label %bb1.i

bb3.i:                                            ; preds = %start
; call core::fmt::Formatter::write_str
  %3 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [16 x i8] }>* @alloc3076 to [0 x i8]*), i64 16), !noalias !880
  br label %"_ZN76_$LT$alloc..collections..TryReserveErrorKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h0e4fd2ec2b87f863E.exit"

bb1.i:                                            ; preds = %start
  %4 = bitcast %"core::fmt::builders::DebugStruct"* %_12.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %4), !noalias !885
; call core::fmt::Formatter::debug_struct
  %5 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [10 x i8] }>* @alloc3067 to [0 x i8]*), i64 10), !noalias !880
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_12.i to i128*
  store i128 %5, i128* %.0..sroa_cast.i, align 8, !noalias !885
  %6 = bitcast { i64, i64 }** %_23.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %6), !noalias !885
  store { i64, i64 }* %_4, { i64, i64 }** %_23.i, align 8, !noalias !885
  %_20.0.i = bitcast { i64, i64 }** %_23.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_16.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_12.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc3068 to [0 x i8]*), i64 6, {}* nonnull align 1 %_20.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %6), !noalias !885
  %7 = bitcast {}** %_31.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %7), !noalias !885
  %8 = bitcast {}** %_31.i to { i64, i64 }**
  store { i64, i64 }* %_4, { i64, i64 }** %8, align 8, !noalias !885
  %_28.0.i = bitcast {}** %_31.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_24.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_12.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [14 x i8] }>* @alloc3072 to [0 x i8]*), i64 14, {}* nonnull align 1 %_28.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %7), !noalias !885
; call core::fmt::builders::DebugStruct::finish
  %9 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_12.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %4), !noalias !885
  br label %"_ZN76_$LT$alloc..collections..TryReserveErrorKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h0e4fd2ec2b87f863E.exit"

"_ZN76_$LT$alloc..collections..TryReserveErrorKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h0e4fd2ec2b87f863E.exit": ; preds = %bb3.i, %bb1.i
  %.0.in.i = phi i1 [ %3, %bb3.i ], [ %9, %bb1.i ]
  ret i1 %.0.in.i
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h528391e318defd79E"(%"vec::Vec<u8>"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 personality i32 (...)* @rust_eh_personality {
start:
  %entry.i.i.i = alloca i8*, align 8
  %_6.i.i = alloca %"core::fmt::builders::DebugList", align 8
  %_4 = load %"vec::Vec<u8>"*, %"vec::Vec<u8>"** %self, align 8, !nonnull !20
  %_4.idx = getelementptr %"vec::Vec<u8>", %"vec::Vec<u8>"* %_4, i64 0, i32 0, i32 0
  %_4.idx.val = load i8*, i8** %_4.idx, align 8, !alias.scope !652
  %_4.idx1 = getelementptr %"vec::Vec<u8>", %"vec::Vec<u8>"* %_4, i64 0, i32 1
  %_4.idx1.val = load i64, i64* %_4.idx1, align 8
  %0 = bitcast %"core::fmt::builders::DebugList"* %_6.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !886
; call core::fmt::Formatter::debug_list
  %1 = tail call i128 @_ZN4core3fmt9Formatter10debug_list17h804611945d4c2177E(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f), !noalias !892
  %.0..sroa_cast.i.i = bitcast %"core::fmt::builders::DebugList"* %_6.i.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i.i, align 8, !noalias !886
  %2 = getelementptr inbounds i8, i8* %_4.idx.val, i64 %_4.idx1.val
  %3 = bitcast i8** %entry.i.i.i to i8*
  %_14.0.i.i.i = bitcast i8** %entry.i.i.i to {}*
  %_12.i16.i.i.i = icmp eq i64 %_4.idx1.val, 0
  br i1 %_12.i16.i.i.i, label %"_ZN65_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hbfa03955de355f46E.exit", label %bb4.i.i.i

bb4.i.i.i:                                        ; preds = %start, %bb4.i.i.i
  %iter.sroa.0.017.i.i.i = phi i8* [ %4, %bb4.i.i.i ], [ %_4.idx.val, %start ]
  %4 = getelementptr inbounds i8, i8* %iter.sroa.0.017.i.i.i, i64 1
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3), !noalias !893
  store i8* %iter.sroa.0.017.i.i.i, i8** %entry.i.i.i, align 8, !noalias !893
; call core::fmt::builders::DebugList::entry
  %_12.i.i.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugList"* @_ZN4core3fmt8builders9DebugList5entry17h70c8a86ab213bc16E(%"core::fmt::builders::DebugList"* noalias nonnull align 8 dereferenceable(16) %_6.i.i, {}* nonnull align 1 %_14.0.i.i.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.7 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3), !noalias !893
  %_12.i.i.i.i = icmp eq i8* %4, %2
  br i1 %_12.i.i.i.i, label %"_ZN65_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hbfa03955de355f46E.exit", label %bb4.i.i.i

"_ZN65_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hbfa03955de355f46E.exit": ; preds = %bb4.i.i.i, %start
; call core::fmt::builders::DebugList::finish
  %5 = call zeroext i1 @_ZN4core3fmt8builders9DebugList6finish17h02d7d1d08d1d92d5E(%"core::fmt::builders::DebugList"* noalias nonnull align 8 dereferenceable(16) %_6.i.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !886
  ret i1 %5
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h7075983059913f2aE"({}** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
; call core::fmt::Formatter::pad
  %0 = tail call zeroext i1 @_ZN4core3fmt9Formatter3pad17h40fda45ac7db157fE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [2 x i8] }>* @alloc3103 to [0 x i8]*), i64 2)
  ret i1 %0
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h9b27245c2844029aE"({ i64, i64 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %_4 = load { i64, i64 }*, { i64, i64 }** %self, align 8, !nonnull !20
; call <core::alloc::layout::Layout as core::fmt::Debug>::fmt
  %0 = tail call zeroext i1 @"_ZN64_$LT$core..alloc..layout..Layout$u20$as$u20$core..fmt..Debug$GT$3fmt17hea3f85e7ab53336fE"({ i64, i64 }* noalias nonnull readonly align 8 dereferenceable(16) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %0
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hf82658bc94a10742E"({ [0 x i8]*, i64 }* noalias nocapture readonly align 8 dereferenceable(16) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #10 {
start:
  %0 = getelementptr inbounds { [0 x i8]*, i64 }, { [0 x i8]*, i64 }* %self, i64 0, i32 0
  %_4.0 = load [0 x i8]*, [0 x i8]** %0, align 8, !nonnull !20
  %1 = getelementptr inbounds { [0 x i8]*, i64 }, { [0 x i8]*, i64 }* %self, i64 0, i32 1
  %_4.1 = load i64, i64* %1, align 8
; call <str as core::fmt::Debug>::fmt
  %2 = tail call zeroext i1 @"_ZN40_$LT$str$u20$as$u20$core..fmt..Debug$GT$3fmt17h2fc61a29b5ba73d9E"([0 x i8]* noalias nonnull readonly align 1 %_4.0, i64 %_4.1, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %2
}

; Function Attrs: cold noreturn nounwind
declare void @llvm.trap() #16

; Function Attrs: nonlazybind
declare i32 @rust_eh_personality(...) unnamed_addr #17

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.end.p0i8(i64 immarg, i8* nocapture) #18

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.start.p0i8(i64 immarg, i8* nocapture) #18

; Function Attrs: argmemonly mustprogress nofree nounwind willreturn
declare void @llvm.memcpy.p0i8.p0i8.i64(i8* noalias nocapture writeonly, i8* noalias nocapture readonly, i64, i1 immarg) #19

; core::panicking::panic
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #15

; Function Attrs: inaccessiblememonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.assume(i1 noundef) #20

; Function Attrs: nofree nounwind nonlazybind uwtable
declare noalias i8* @__rust_alloc(i64, i64) unnamed_addr #21

; Function Attrs: nounwind nonlazybind uwtable
declare void @__rust_dealloc(i8*, i64, i64) unnamed_addr #2

; Function Attrs: nounwind nonlazybind uwtable
declare i8* @__rust_realloc(i8*, i64, i64, i64) unnamed_addr #2

; Function Attrs: noreturn nounwind nonlazybind uwtable
declare void @__rust_alloc_error_handler(i64, i64) unnamed_addr #8

; core::fmt::num::imp::<impl core::fmt::Display for usize>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E"(i64* noalias readonly align 8 dereferenceable(8), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; core::panicking::panic_fmt
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core9panicking9panic_fmt17he21f9dfe87a034c1E(%"core::fmt::Arguments"* noalias nocapture dereferenceable(48), %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #15

; Function Attrs: noreturn nonlazybind uwtable
declare void @rust_oom(i64, i64) unnamed_addr #6

; Function Attrs: argmemonly mustprogress nofree nounwind willreturn
declare void @llvm.memmove.p0i8.p0i8.i64(i8* nocapture writeonly, i8* nocapture readonly, i64, i1 immarg) #19

; Function Attrs: argmemonly mustprogress nofree nounwind willreturn writeonly
declare void @llvm.memset.p0i8.i64(i8* nocapture writeonly, i8, i64, i1 immarg) #22

; core::fmt::Formatter::write_str
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #10

; core::unicode::unicode_data::conversions::to_lower
; Function Attrs: nonlazybind uwtable
declare i96 @_ZN4core7unicode12unicode_data11conversions8to_lower17h4c5a99401b395956E(i32) unnamed_addr #10

; core::unicode::unicode_data::cased::lookup
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core7unicode12unicode_data5cased6lookup17h4e29d1a807414825E(i32) unnamed_addr #10

; core::unicode::unicode_data::case_ignorable::lookup
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core7unicode12unicode_data14case_ignorable6lookup17h5a75bf47d320559cE(i32) unnamed_addr #10

; core::unicode::unicode_data::conversions::to_upper
; Function Attrs: nonlazybind uwtable
declare i96 @_ZN4core7unicode12unicode_data11conversions8to_upper17he4910922e1cf292aE(i32) unnamed_addr #10

; core::str::lossy::Utf8Lossy::from_bytes
; Function Attrs: nonlazybind uwtable
declare { %"core::str::lossy::Utf8Lossy"*, i64 } @_ZN4core3str5lossy9Utf8Lossy10from_bytes17h05976e8c14cc0348E([0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #10

; core::str::lossy::Utf8Lossy::chunks
; Function Attrs: nonlazybind uwtable
declare { i8*, i64 } @_ZN4core3str5lossy9Utf8Lossy6chunks17h46b994a81ffc2754E(%"core::str::lossy::Utf8Lossy"* noalias nonnull readonly align 1, i64) unnamed_addr #10

; <core::str::lossy::Utf8LossyChunksIter as core::iter::traits::iterator::Iterator>::next
; Function Attrs: nonlazybind uwtable
declare void @"_ZN96_$LT$core..str..lossy..Utf8LossyChunksIter$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17ha7777be30a234397E"(%"core::option::Option<core::str::lossy::Utf8LossyChunk>"* noalias nocapture sret(%"core::option::Option<core::str::lossy::Utf8LossyChunk>") dereferenceable(32), { i8*, i64 }* noalias align 8 dereferenceable(16)) unnamed_addr #10

; <core::str::error::Utf8Error as core::fmt::Display>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN66_$LT$core..str..error..Utf8Error$u20$as$u20$core..fmt..Display$GT$3fmt17h498c1679916781e1E"(%"core::str::error::Utf8Error"* noalias readonly align 8 dereferenceable(16), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; <str as core::fmt::Display>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN42_$LT$str$u20$as$u20$core..fmt..Display$GT$3fmt17h6e40a2e7e798ff25E"([0 x i8]* noalias nonnull readonly align 1, i64, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; core::fmt::Formatter::debug_tuple
; Function Attrs: nonlazybind uwtable
declare void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture sret(%"core::fmt::builders::DebugTuple") dereferenceable(24), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #10

; core::fmt::builders::DebugTuple::field
; Function Attrs: nonlazybind uwtable
declare align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias align 8 dereferenceable(24), {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24)) unnamed_addr #10

; core::fmt::builders::DebugTuple::finish
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias align 8 dereferenceable(24)) unnamed_addr #10

; core::fmt::Formatter::debug_struct
; Function Attrs: nonlazybind uwtable
declare i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #10

; core::fmt::builders::DebugStruct::field
; Function Attrs: nonlazybind uwtable
declare align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias align 8 dereferenceable(16), [0 x i8]* noalias nonnull readonly align 1, i64, {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24)) unnamed_addr #10

; core::fmt::builders::DebugStruct::finish
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias align 8 dereferenceable(16)) unnamed_addr #10

; core::option::expect_failed
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core6option13expect_failed17had3e778ecbdbaeaeE([0 x i8]* noalias nonnull readonly align 1, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #15

; <core::fmt::Error as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN53_$LT$core..fmt..Error$u20$as$u20$core..fmt..Debug$GT$3fmt17haa251479953ac187E"(%"core::fmt::Error"* noalias nonnull readonly align 1, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; core::result::unwrap_failed
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core6result13unwrap_failed17he6aaa65cb062b616E([0 x i8]* noalias nonnull readonly align 1, i64, {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24), %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #15

; core::fmt::builders::DebugList::entry
; Function Attrs: nonlazybind uwtable
declare align 8 dereferenceable(16) %"core::fmt::builders::DebugList"* @_ZN4core3fmt8builders9DebugList5entry17h70c8a86ab213bc16E(%"core::fmt::builders::DebugList"* noalias align 8 dereferenceable(16), {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24)) unnamed_addr #10

; core::fmt::write
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt5write17hc4ca0ba042bde21dE({}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24), %"core::fmt::Arguments"* noalias nocapture dereferenceable(48)) unnamed_addr #10

; core::fmt::Formatter::debug_list
; Function Attrs: nonlazybind uwtable
declare i128 @_ZN4core3fmt9Formatter10debug_list17h804611945d4c2177E(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; core::fmt::builders::DebugList::finish
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt8builders9DebugList6finish17h02d7d1d08d1d92d5E(%"core::fmt::builders::DebugList"* noalias align 8 dereferenceable(16)) unnamed_addr #10

; core::fmt::Formatter::pad
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter3pad17h40fda45ac7db157fE(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #10

; core::str::slice_error_fail
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core3str16slice_error_fail17h0d3b5d2bc1b01573E([0 x i8]* noalias nonnull readonly align 1, i64, i64, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #15

; core::str::pattern::StrSearcher::new
; Function Attrs: nonlazybind uwtable
declare void @_ZN4core3str7pattern11StrSearcher3new17h6ae7735913737082E(%"core::str::pattern::StrSearcher"* noalias nocapture sret(%"core::str::pattern::StrSearcher") dereferenceable(104), [0 x i8]* noalias nonnull readonly align 1, i64, [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #10

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i64, i1 } @llvm.uadd.with.overflow.i64(i64, i64) #23

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i64, i1 } @llvm.umul.with.overflow.i64(i64, i64) #23

; core::fmt::Formatter::debug_lower_hex
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias readonly align 8 dereferenceable(64)) unnamed_addr #10

; core::fmt::num::<impl core::fmt::LowerHex for u8>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$u8$GT$3fmt17h9ad27865f481f5ecE"(i8* noalias readonly align 1 dereferenceable(1), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; core::fmt::Formatter::debug_upper_hex
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias readonly align 8 dereferenceable(64)) unnamed_addr #10

; core::fmt::num::<impl core::fmt::UpperHex for u8>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$u8$GT$3fmt17h1f7726c1de44a74fE"(i8* noalias readonly align 1 dereferenceable(1), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; core::fmt::num::imp::<impl core::fmt::Display for u8>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num3imp51_$LT$impl$u20$core..fmt..Display$u20$for$u20$u8$GT$3fmt17h7657a76892c51730E"(i8* noalias readonly align 1 dereferenceable(1), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; <core::str::error::Utf8Error as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN64_$LT$core..str..error..Utf8Error$u20$as$u20$core..fmt..Debug$GT$3fmt17h29ccbff883b6b6bbE"(%"core::str::error::Utf8Error"* noalias readonly align 8 dereferenceable(16), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; <core::alloc::layout::Layout as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN64_$LT$core..alloc..layout..Layout$u20$as$u20$core..fmt..Debug$GT$3fmt17hea3f85e7ab53336fE"({ i64, i64 }* noalias readonly align 8 dereferenceable(16), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; <str as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN40_$LT$str$u20$as$u20$core..fmt..Debug$GT$3fmt17h2fc61a29b5ba73d9E"([0 x i8]* noalias nonnull readonly align 1, i64, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #10

; Function Attrs: inaccessiblememonly nofree nosync nounwind willreturn
declare void @llvm.experimental.noalias.scope.decl(metadata) #24

attributes #0 = { inlinehint noreturn nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #3 = { cold nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #4 = { noinline nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #5 = { noinline nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #6 = { noreturn nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #7 = { cold noreturn nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #8 = { noreturn nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #9 = { mustprogress nofree nosync nounwind nonlazybind uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #10 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #11 = { mustprogress nofree norecurse nosync nounwind nonlazybind uwtable willreturn writeonly "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #12 = { mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #13 = { inlinehint nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #14 = { mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #15 = { cold noinline noreturn nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #16 = { cold noreturn nounwind }
attributes #17 = { nonlazybind "target-cpu"="x86-64" }
attributes #18 = { argmemonly mustprogress nofree nosync nounwind willreturn }
attributes #19 = { argmemonly mustprogress nofree nounwind willreturn }
attributes #20 = { inaccessiblememonly mustprogress nofree nosync nounwind willreturn }
attributes #21 = { nofree nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #22 = { argmemonly mustprogress nofree nounwind willreturn writeonly }
attributes #23 = { mustprogress nofree nosync nounwind readnone speculatable willreturn }
attributes #24 = { inaccessiblememonly nofree nosync nounwind willreturn }
attributes #25 = { noreturn }
attributes #26 = { nounwind }
attributes #27 = { noreturn nounwind }
attributes #28 = { cold }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
!2 = !{i64 0, i64 2}
!3 = !{!4}
!4 = distinct !{!4, !5, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17hde794f90541644ebE: %self"}
!5 = distinct !{!5, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17hde794f90541644ebE"}
!6 = !{!7}
!7 = distinct !{!7, !5, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17hde794f90541644ebE: argument 0"}
!8 = !{!7, !4}
!9 = !{!10}
!10 = distinct !{!10, !11, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E: argument 0"}
!11 = distinct !{!11, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E"}
!12 = !{!13}
!13 = distinct !{!13, !14, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E: %self"}
!14 = distinct !{!14, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E"}
!15 = !{!16, !7, !4}
!16 = distinct !{!16, !14, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E: argument 0"}
!17 = !{!18}
!18 = distinct !{!18, !19, !"_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE: %result"}
!19 = distinct !{!19, !"_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE"}
!20 = !{}
!21 = !{!22, !4}
!22 = distinct !{!22, !23, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17hca4d48779be255e5E: %self"}
!23 = distinct !{!23, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17hca4d48779be255e5E"}
!24 = !{!25}
!25 = distinct !{!25, !26, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17hde794f90541644ebE: %self"}
!26 = distinct !{!26, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17hde794f90541644ebE"}
!27 = !{!28}
!28 = distinct !{!28, !26, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17hde794f90541644ebE: argument 0"}
!29 = !{!28, !25}
!30 = !{!31}
!31 = distinct !{!31, !32, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E: argument 0"}
!32 = distinct !{!32, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E"}
!33 = !{!34}
!34 = distinct !{!34, !35, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E: %self"}
!35 = distinct !{!35, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E"}
!36 = !{!37, !28, !25}
!37 = distinct !{!37, !35, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E: argument 0"}
!38 = !{!39}
!39 = distinct !{!39, !40, !"_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE: %result"}
!40 = distinct !{!40, !"_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE"}
!41 = !{!42, !25}
!42 = distinct !{!42, !43, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17hca4d48779be255e5E: %self"}
!43 = distinct !{!43, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17hca4d48779be255e5E"}
!44 = !{!45}
!45 = distinct !{!45, !46, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17hd7372e2f478a573aE: argument 0"}
!46 = distinct !{!46, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17hd7372e2f478a573aE"}
!47 = !{i64 1, i64 0}
!48 = !{!49}
!49 = distinct !{!49, !50, !"_ZN4core6result19Result$LT$T$C$E$GT$7map_err17h66342fdc531a85f4E: argument 0"}
!50 = distinct !{!50, !"_ZN4core6result19Result$LT$T$C$E$GT$7map_err17h66342fdc531a85f4E"}
!51 = !{!52}
!52 = distinct !{!52, !50, !"_ZN4core6result19Result$LT$T$C$E$GT$7map_err17h66342fdc531a85f4E: %op"}
!53 = !{!54}
!54 = distinct !{!54, !55, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: argument 0"}
!55 = distinct !{!55, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E"}
!56 = !{!57, !58}
!57 = distinct !{!57, !55, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: %pieces.0"}
!58 = distinct !{!58, !55, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: %args.0"}
!59 = !{!60, !62}
!60 = distinct !{!60, !61, !"_ZN99_$LT$alloc..boxed..Box$LT$$u5b$T$u5d$$GT$$u20$as$u20$core..convert..From$LT$$RF$$u5b$T$u5d$$GT$$GT$4from17h26f7f084608d8b63E: %slice.0"}
!61 = distinct !{!61, !"_ZN99_$LT$alloc..boxed..Box$LT$$u5b$T$u5d$$GT$$u20$as$u20$core..convert..From$LT$$RF$$u5b$T$u5d$$GT$$GT$4from17h26f7f084608d8b63E"}
!62 = distinct !{!62, !63, !"_ZN50_$LT$T$u20$as$u20$core..convert..Into$LT$U$GT$$GT$4into17hb8801d34722af82aE: %self.0"}
!63 = distinct !{!63, !"_ZN50_$LT$T$u20$as$u20$core..convert..Into$LT$U$GT$$GT$4into17hb8801d34722af82aE"}
!64 = !{!65}
!65 = distinct !{!65, !66, !"_ZN71_$LT$alloc..borrow..Cow$LT$B$GT$$u20$as$u20$core..ops..deref..Deref$GT$5deref17hc9e9dc5ab4abf1bcE: %self"}
!66 = distinct !{!66, !"_ZN71_$LT$alloc..borrow..Cow$LT$B$GT$$u20$as$u20$core..ops..deref..Deref$GT$5deref17hc9e9dc5ab4abf1bcE"}
!67 = !{!68}
!68 = distinct !{!68, !69, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E: argument 0"}
!69 = distinct !{!69, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E"}
!70 = !{!71, !73, !68}
!71 = distinct !{!71, !72, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!72 = distinct !{!72, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!73 = distinct !{!73, !74, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E: argument 0"}
!74 = distinct !{!74, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E"}
!75 = !{!76}
!76 = distinct !{!76, !77, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!77 = distinct !{!77, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!78 = !{!79}
!79 = distinct !{!79, !80, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!80 = distinct !{!80, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!81 = !{!82}
!82 = distinct !{!82, !83, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!83 = distinct !{!83, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!84 = !{!85}
!85 = distinct !{!85, !86, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!86 = distinct !{!86, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!87 = !{!85, !82, !79, !76}
!88 = !{!89, !90}
!89 = distinct !{!89, !80, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!90 = distinct !{!90, !77, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!91 = !{!92, !85, !82, !79, !76}
!92 = distinct !{!92, !93, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!93 = distinct !{!93, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!94 = !{!95}
!95 = distinct !{!95, !96, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!96 = distinct !{!96, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!97 = !{!98}
!98 = distinct !{!98, !99, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!99 = distinct !{!99, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!100 = !{!101}
!101 = distinct !{!101, !102, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!102 = distinct !{!102, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!103 = !{!104}
!104 = distinct !{!104, !105, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!105 = distinct !{!105, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!106 = !{!107, !109, !104, !101, !98, !95}
!107 = distinct !{!107, !108, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!108 = distinct !{!108, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!109 = distinct !{!109, !110, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!110 = distinct !{!110, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!111 = !{!112, !113}
!112 = distinct !{!112, !99, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!113 = distinct !{!113, !96, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!114 = !{!104, !101, !98, !95}
!115 = !{!116, !104, !101, !98, !95}
!116 = distinct !{!116, !117, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!117 = distinct !{!117, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!118 = !{!109, !104, !101, !98, !95}
!119 = !{!120}
!120 = distinct !{!120, !121, !"_ZN71_$LT$alloc..borrow..Cow$LT$B$GT$$u20$as$u20$core..ops..deref..Deref$GT$5deref17hc9e9dc5ab4abf1bcE: %self"}
!121 = distinct !{!121, !"_ZN71_$LT$alloc..borrow..Cow$LT$B$GT$$u20$as$u20$core..ops..deref..Deref$GT$5deref17hc9e9dc5ab4abf1bcE"}
!122 = !{!123}
!123 = distinct !{!123, !124, !"_ZN71_$LT$alloc..borrow..Cow$LT$B$GT$$u20$as$u20$core..ops..deref..Deref$GT$5deref17hc9e9dc5ab4abf1bcE: %self"}
!124 = distinct !{!124, !"_ZN71_$LT$alloc..borrow..Cow$LT$B$GT$$u20$as$u20$core..ops..deref..Deref$GT$5deref17hc9e9dc5ab4abf1bcE"}
!125 = !{!126}
!126 = distinct !{!126, !127, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E: argument 0"}
!127 = distinct !{!127, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E"}
!128 = !{!129, !131, !126}
!129 = distinct !{!129, !130, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!130 = distinct !{!130, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!131 = distinct !{!131, !132, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E: argument 0"}
!132 = distinct !{!132, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E"}
!133 = !{!134}
!134 = distinct !{!134, !135, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!135 = distinct !{!135, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!136 = !{!137}
!137 = distinct !{!137, !138, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!138 = distinct !{!138, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!139 = !{!140}
!140 = distinct !{!140, !141, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!141 = distinct !{!141, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!142 = !{!143}
!143 = distinct !{!143, !144, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!144 = distinct !{!144, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!145 = !{!143, !140, !137, !134}
!146 = !{!147, !148}
!147 = distinct !{!147, !138, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!148 = distinct !{!148, !135, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!149 = !{!150, !143, !140, !137, !134}
!150 = distinct !{!150, !151, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!151 = distinct !{!151, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!152 = !{!153, !155, !157, !159, !161}
!153 = distinct !{!153, !154, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!154 = distinct !{!154, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!155 = distinct !{!155, !156, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!156 = distinct !{!156, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!157 = distinct !{!157, !158, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!158 = distinct !{!158, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!159 = distinct !{!159, !160, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!160 = distinct !{!160, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!161 = distinct !{!161, !162, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!162 = distinct !{!162, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!163 = !{!164, !165}
!164 = distinct !{!164, !160, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!165 = distinct !{!165, !162, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!166 = !{!167}
!167 = distinct !{!167, !168, !"_ZN71_$LT$alloc..borrow..Cow$LT$B$GT$$u20$as$u20$core..ops..deref..Deref$GT$5deref17hc9e9dc5ab4abf1bcE: %self"}
!168 = distinct !{!168, !"_ZN71_$LT$alloc..borrow..Cow$LT$B$GT$$u20$as$u20$core..ops..deref..Deref$GT$5deref17hc9e9dc5ab4abf1bcE"}
!169 = !{!161}
!170 = !{!159}
!171 = !{!157}
!172 = !{!155}
!173 = !{!174, !153, !155, !157, !159, !161}
!174 = distinct !{!174, !175, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!175 = distinct !{!175, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!176 = !{!155, !157, !159, !161}
!177 = !{!178, !155, !157, !159, !161}
!178 = distinct !{!178, !179, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!179 = distinct !{!179, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!180 = distinct !{!180, !181}
!181 = !{!"llvm.loop.unroll.disable"}
!182 = !{!183}
!183 = distinct !{!183, !184, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E: argument 0"}
!184 = distinct !{!184, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E"}
!185 = !{!186, !188, !183}
!186 = distinct !{!186, !187, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!187 = distinct !{!187, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!188 = distinct !{!188, !189, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E: argument 0"}
!189 = distinct !{!189, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E"}
!190 = !{!191, !193}
!191 = distinct !{!191, !192, !"_ZN4core3fmt5Write9write_fmt17hf1b0536c1be1d21aE: argument 0"}
!192 = distinct !{!192, !"_ZN4core3fmt5Write9write_fmt17hf1b0536c1be1d21aE"}
!193 = distinct !{!193, !192, !"_ZN4core3fmt5Write9write_fmt17hf1b0536c1be1d21aE: %args"}
!194 = !{!195}
!195 = distinct !{!195, !196, !"_ZN4core3mem4take17h830f86c5c6abda5eE: %dest"}
!196 = distinct !{!196, !"_ZN4core3mem4take17h830f86c5c6abda5eE"}
!197 = !{!198, !200, !202, !204, !195}
!198 = distinct !{!198, !199, !"_ZN5alloc3vec12Vec$LT$T$GT$3new17h10bf52469658b2d9E: argument 0"}
!199 = distinct !{!199, !"_ZN5alloc3vec12Vec$LT$T$GT$3new17h10bf52469658b2d9E"}
!200 = distinct !{!200, !201, !"_ZN5alloc6string6String3new17h369e2b30a63ee47aE: argument 0"}
!201 = distinct !{!201, !"_ZN5alloc6string6String3new17h369e2b30a63ee47aE"}
!202 = distinct !{!202, !203, !"_ZN64_$LT$alloc..string..String$u20$as$u20$core..default..Default$GT$7default17h778eda523d8b0460E: argument 0"}
!203 = distinct !{!203, !"_ZN64_$LT$alloc..string..String$u20$as$u20$core..default..Default$GT$7default17h778eda523d8b0460E"}
!204 = distinct !{!204, !196, !"_ZN4core3mem4take17h830f86c5c6abda5eE: argument 0"}
!205 = !{!206, !208, !195}
!206 = distinct !{!206, !207, !"_ZN4core3mem7replace17hb05f76395c523920E: %dest"}
!207 = distinct !{!207, !"_ZN4core3mem7replace17hb05f76395c523920E"}
!208 = distinct !{!208, !207, !"_ZN4core3mem7replace17hb05f76395c523920E: %src"}
!209 = !{!210, !204}
!210 = distinct !{!210, !207, !"_ZN4core3mem7replace17hb05f76395c523920E: %result"}
!211 = !{!204}
!212 = !{!213}
!213 = distinct !{!213, !214, !"_ZN5alloc5slice64_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$$u5b$T$u5d$$GT$10clone_into17ha8b939ac8f5df157E: %target"}
!214 = distinct !{!214, !"_ZN5alloc5slice64_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$$u5b$T$u5d$$GT$10clone_into17ha8b939ac8f5df157E"}
!215 = !{!216, !213}
!216 = distinct !{!216, !217, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$8truncate17hb7f76b27637c74fbE: %self"}
!217 = distinct !{!217, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$8truncate17hb7f76b27637c74fbE"}
!218 = !{!219}
!219 = distinct !{!219, !214, !"_ZN5alloc5slice64_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$$u5b$T$u5d$$GT$10clone_into17ha8b939ac8f5df157E: %self.0"}
!220 = !{!221, !213}
!221 = distinct !{!221, !222, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!222 = distinct !{!222, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!223 = !{!224, !226, !227, !229, !230, !232}
!224 = distinct !{!224, !225, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$15copy_from_slice17h88e6d9baa7cfe94cE: %self.0"}
!225 = distinct !{!225, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$15copy_from_slice17h88e6d9baa7cfe94cE"}
!226 = distinct !{!226, !225, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$15copy_from_slice17h88e6d9baa7cfe94cE: %src.0"}
!227 = distinct !{!227, !228, !"_ZN67_$LT$$u5b$T$u5d$$u20$as$u20$core..slice..CloneFromSpec$LT$T$GT$$GT$15spec_clone_from17h9e75db67b5423949E: %self.0"}
!228 = distinct !{!228, !"_ZN67_$LT$$u5b$T$u5d$$u20$as$u20$core..slice..CloneFromSpec$LT$T$GT$$GT$15spec_clone_from17h9e75db67b5423949E"}
!229 = distinct !{!229, !228, !"_ZN67_$LT$$u5b$T$u5d$$u20$as$u20$core..slice..CloneFromSpec$LT$T$GT$$GT$15spec_clone_from17h9e75db67b5423949E: %src.0"}
!230 = distinct !{!230, !231, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$16clone_from_slice17h9acbe59e8713fc24E: %self.0"}
!231 = distinct !{!231, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$16clone_from_slice17h9acbe59e8713fc24E"}
!232 = distinct !{!232, !231, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$16clone_from_slice17h9acbe59e8713fc24E: %src.0"}
!233 = !{!234}
!234 = distinct !{!234, !235, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!235 = distinct !{!235, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!236 = !{!237}
!237 = distinct !{!237, !238, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!238 = distinct !{!238, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!239 = !{!240}
!240 = distinct !{!240, !241, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!241 = distinct !{!241, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!242 = !{!243, !245, !240, !237, !234, !213}
!243 = distinct !{!243, !244, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!244 = distinct !{!244, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!245 = distinct !{!245, !246, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!246 = distinct !{!246, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!247 = !{!248, !219}
!248 = distinct !{!248, !235, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!249 = !{!240, !237, !234, !213}
!250 = !{!251, !240, !237, !234, !213}
!251 = distinct !{!251, !252, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!252 = distinct !{!252, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!253 = !{!254}
!254 = distinct !{!254, !255, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E: argument 0"}
!255 = distinct !{!255, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E"}
!256 = !{!257, !259, !254}
!257 = distinct !{!257, !258, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!258 = distinct !{!258, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!259 = distinct !{!259, !260, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E: argument 0"}
!260 = distinct !{!260, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E"}
!261 = !{!262, !264, !266}
!262 = distinct !{!262, !263, !"_ZN4core3str11validations15next_code_point17h6b4a41217f71eda3E: %bytes"}
!263 = distinct !{!263, !"_ZN4core3str11validations15next_code_point17h6b4a41217f71eda3E"}
!264 = distinct !{!264, !265, !"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E: %self"}
!265 = distinct !{!265, !"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E"}
!266 = distinct !{!266, !267, !"_ZN87_$LT$core..str..iter..CharIndices$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hf121448137d23d94E: %self"}
!267 = distinct !{!267, !"_ZN87_$LT$core..str..iter..CharIndices$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hf121448137d23d94E"}
!268 = !{!269}
!269 = distinct !{!269, !270, !"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase19map_uppercase_sigma17ha03f9062f1a561cdE: %from.0"}
!270 = distinct !{!270, !"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase19map_uppercase_sigma17ha03f9062f1a561cdE"}
!271 = !{!272}
!272 = distinct !{!272, !270, !"_ZN5alloc3str21_$LT$impl$u20$str$GT$12to_lowercase19map_uppercase_sigma17ha03f9062f1a561cdE: %to"}
!273 = !{!274, !276, !278, !280, !269}
!274 = distinct !{!274, !275, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE: %self.0"}
!275 = distinct !{!275, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE"}
!276 = distinct !{!276, !277, !"_ZN4core3str6traits110_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..RangeTo$LT$usize$GT$$GT$3get17h194b89378bd840c4E: %slice.0"}
!277 = distinct !{!277, !"_ZN4core3str6traits110_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..RangeTo$LT$usize$GT$$GT$3get17h194b89378bd840c4E"}
!278 = distinct !{!278, !279, !"_ZN4core3str6traits110_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..RangeTo$LT$usize$GT$$GT$5index17h975acc4a0c774a99E: %slice.0"}
!279 = distinct !{!279, !"_ZN4core3str6traits110_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..RangeTo$LT$usize$GT$$GT$5index17h975acc4a0c774a99E"}
!280 = distinct !{!280, !281, !"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h020f9c9bc42b5435E: %self.0"}
!281 = distinct !{!281, !"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h020f9c9bc42b5435E"}
!282 = !{!283, !285, !287, !289, !290, !292, !293, !295, !296, !272}
!283 = distinct !{!283, !284, !"_ZN4core3str11validations23next_code_point_reverse17h4dedcadd96a6c484E: %bytes"}
!284 = distinct !{!284, !"_ZN4core3str11validations23next_code_point_reverse17h4dedcadd96a6c484E"}
!285 = distinct !{!285, !286, !"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E: %self"}
!286 = distinct !{!286, !"_ZN96_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..double_ended..DoubleEndedIterator$GT$9next_back17h71cb4b7a7f8627e4E"}
!287 = distinct !{!287, !288, !"_ZN4core4iter6traits12double_ended19DoubleEndedIterator9try_rfold17h7bed9ef35cd69915E: %self"}
!288 = distinct !{!288, !"_ZN4core4iter6traits12double_ended19DoubleEndedIterator9try_rfold17h7bed9ef35cd69915E"}
!289 = distinct !{!289, !288, !"_ZN4core4iter6traits12double_ended19DoubleEndedIterator9try_rfold17h7bed9ef35cd69915E: argument 1"}
!290 = distinct !{!290, !291, !"_ZN4core4iter6traits12double_ended19DoubleEndedIterator5rfind17hb1c399649983bb06E: %self"}
!291 = distinct !{!291, !"_ZN4core4iter6traits12double_ended19DoubleEndedIterator5rfind17hb1c399649983bb06E"}
!292 = distinct !{!292, !291, !"_ZN4core4iter6traits12double_ended19DoubleEndedIterator5rfind17hb1c399649983bb06E: %predicate.0"}
!293 = distinct !{!293, !294, !"_ZN98_$LT$core..iter..adapters..rev..Rev$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4find17h27dffaa22749b0b4E: %self"}
!294 = distinct !{!294, !"_ZN98_$LT$core..iter..adapters..rev..Rev$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4find17h27dffaa22749b0b4E"}
!295 = distinct !{!295, !294, !"_ZN98_$LT$core..iter..adapters..rev..Rev$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4find17h27dffaa22749b0b4E: %predicate.0"}
!296 = distinct !{!296, !297, !"_ZN115_$LT$core..iter..adapters..skip_while..SkipWhile$LT$I$C$P$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h8a8dcf67b7149373E: %self"}
!297 = distinct !{!297, !"_ZN115_$LT$core..iter..adapters..skip_while..SkipWhile$LT$I$C$P$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h8a8dcf67b7149373E"}
!298 = !{!299, !301, !303, !305, !269}
!299 = distinct !{!299, !300, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE: %self.0"}
!300 = distinct !{!300, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE"}
!301 = distinct !{!301, !302, !"_ZN4core3str6traits112_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..RangeFrom$LT$usize$GT$$GT$3get17he424f42cd5401a0eE: %slice.0"}
!302 = distinct !{!302, !"_ZN4core3str6traits112_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..RangeFrom$LT$usize$GT$$GT$3get17he424f42cd5401a0eE"}
!303 = distinct !{!303, !304, !"_ZN4core3str6traits112_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..RangeFrom$LT$usize$GT$$GT$5index17h2e639f113533a767E: %slice.0"}
!304 = distinct !{!304, !"_ZN4core3str6traits112_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..RangeFrom$LT$usize$GT$$GT$5index17h2e639f113533a767E"}
!305 = distinct !{!305, !306, !"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h0c94ab3e9c025581E: %self.0"}
!306 = distinct !{!306, !"_ZN4core3str6traits66_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$str$GT$5index17h0c94ab3e9c025581E"}
!307 = !{!308, !310, !312, !314, !315, !317, !318, !272}
!308 = distinct !{!308, !309, !"_ZN4core3str11validations15next_code_point17h6b4a41217f71eda3E: %bytes"}
!309 = distinct !{!309, !"_ZN4core3str11validations15next_code_point17h6b4a41217f71eda3E"}
!310 = distinct !{!310, !311, !"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E: %self"}
!311 = distinct !{!311, !"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E"}
!312 = distinct !{!312, !313, !"_ZN4core4iter6traits8iterator8Iterator8try_fold17h954178f50228986cE: %self"}
!313 = distinct !{!313, !"_ZN4core4iter6traits8iterator8Iterator8try_fold17h954178f50228986cE"}
!314 = distinct !{!314, !313, !"_ZN4core4iter6traits8iterator8Iterator8try_fold17h954178f50228986cE: argument 1"}
!315 = distinct !{!315, !316, !"_ZN4core4iter6traits8iterator8Iterator4find17h718c57bd22726a0aE: %self"}
!316 = distinct !{!316, !"_ZN4core4iter6traits8iterator8Iterator4find17h718c57bd22726a0aE"}
!317 = distinct !{!317, !316, !"_ZN4core4iter6traits8iterator8Iterator4find17h718c57bd22726a0aE: %predicate.0"}
!318 = distinct !{!318, !319, !"_ZN115_$LT$core..iter..adapters..skip_while..SkipWhile$LT$I$C$P$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h77e50ffc5c35e488E: %self"}
!319 = distinct !{!319, !"_ZN115_$LT$core..iter..adapters..skip_while..SkipWhile$LT$I$C$P$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h77e50ffc5c35e488E"}
!320 = !{!321, !323, !325, !327, !329, !272}
!321 = distinct !{!321, !322, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!322 = distinct !{!322, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!323 = distinct !{!323, !324, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!324 = distinct !{!324, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!325 = distinct !{!325, !326, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!326 = distinct !{!326, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!327 = distinct !{!327, !328, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!328 = distinct !{!328, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!329 = distinct !{!329, !330, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!330 = distinct !{!330, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!331 = !{!332, !333, !269}
!332 = distinct !{!332, !328, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!333 = distinct !{!333, !330, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!334 = !{!335, !321, !323, !325, !327, !329, !272}
!335 = distinct !{!335, !336, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!336 = distinct !{!336, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!337 = !{!338, !340, !342, !344, !346, !272}
!338 = distinct !{!338, !339, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!339 = distinct !{!339, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!340 = distinct !{!340, !341, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!341 = distinct !{!341, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!342 = distinct !{!342, !343, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!343 = distinct !{!343, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!344 = distinct !{!344, !345, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!345 = distinct !{!345, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!346 = distinct !{!346, !347, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!347 = distinct !{!347, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!348 = !{!349, !350, !269}
!349 = distinct !{!349, !345, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!350 = distinct !{!350, !347, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!351 = !{!352, !338, !340, !342, !344, !346, !272}
!352 = distinct !{!352, !353, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!353 = distinct !{!353, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!354 = !{!355}
!355 = distinct !{!355, !356, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E: argument 0"}
!356 = distinct !{!356, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E"}
!357 = !{!358, !360, !355}
!358 = distinct !{!358, !359, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!359 = distinct !{!359, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!360 = distinct !{!360, !361, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E: argument 0"}
!361 = distinct !{!361, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E"}
!362 = !{!363, !365}
!363 = distinct !{!363, !364, !"_ZN4core3str11validations15next_code_point17h6b4a41217f71eda3E: %bytes"}
!364 = distinct !{!364, !"_ZN4core3str11validations15next_code_point17h6b4a41217f71eda3E"}
!365 = distinct !{!365, !366, !"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E: %self"}
!366 = distinct !{!366, !"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hb034e96f9b490979E"}
!367 = !{!368}
!368 = distinct !{!368, !369, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6repeat17hfaa7390a5d98f425E: argument 0"}
!369 = distinct !{!369, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6repeat17hfaa7390a5d98f425E"}
!370 = !{!371}
!371 = distinct !{!371, !372, !"_ZN5alloc3vec12Vec$LT$T$GT$3new17h10bf52469658b2d9E: argument 0"}
!372 = distinct !{!372, !"_ZN5alloc3vec12Vec$LT$T$GT$3new17h10bf52469658b2d9E"}
!373 = !{!371, !368, !374}
!374 = distinct !{!374, !369, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6repeat17hfaa7390a5d98f425E: %self.0"}
!375 = !{!371, !368}
!376 = !{!374}
!377 = !{!368, !374}
!378 = !{!379}
!379 = distinct !{!379, !380, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E: argument 0"}
!380 = distinct !{!380, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E"}
!381 = !{!382}
!382 = distinct !{!382, !383, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!383 = distinct !{!383, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!384 = !{!382, !379, !368, !374}
!385 = !{!382, !379}
!386 = !{!387}
!387 = distinct !{!387, !388, !"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E: %self"}
!388 = distinct !{!388, !"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E"}
!389 = !{!390}
!390 = distinct !{!390, !391, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!391 = distinct !{!391, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!392 = !{!393}
!393 = distinct !{!393, !394, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!394 = distinct !{!394, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!395 = !{!393, !390, !387}
!396 = !{!397, !368, !374}
!397 = distinct !{!397, !388, !"_ZN97_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..iter..traits..collect..Extend$LT$$RF$T$GT$$GT$6extend17h0300701edcbec5e9E: %iter.0"}
!398 = !{!393, !390, !387, !368}
!399 = !{!400}
!400 = distinct !{!400, !401, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7set_len17h03d334d9c3baa92eE: %self"}
!401 = distinct !{!401, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7set_len17h03d334d9c3baa92eE"}
!402 = !{!403, !405}
!403 = distinct !{!403, !404, !"_ZN5alloc6string6String19from_utf8_unchecked17ha4ad48367349c805E: argument 0"}
!404 = distinct !{!404, !"_ZN5alloc6string6String19from_utf8_unchecked17ha4ad48367349c805E"}
!405 = distinct !{!405, !404, !"_ZN5alloc6string6String19from_utf8_unchecked17ha4ad48367349c805E: %bytes"}
!406 = !{!407}
!407 = distinct !{!407, !408, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E: argument 0"}
!408 = distinct !{!408, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E"}
!409 = !{!410, !412, !407}
!410 = distinct !{!410, !411, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!411 = distinct !{!411, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!412 = distinct !{!412, !413, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E: argument 0"}
!413 = distinct !{!413, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E"}
!414 = !{!415}
!415 = distinct !{!415, !416, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!416 = distinct !{!416, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!417 = !{!418}
!418 = distinct !{!418, !419, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!419 = distinct !{!419, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!420 = !{!421}
!421 = distinct !{!421, !422, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!422 = distinct !{!422, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!423 = !{!424}
!424 = distinct !{!424, !425, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!425 = distinct !{!425, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!426 = !{!424, !421, !418, !415}
!427 = !{!428, !429}
!428 = distinct !{!428, !419, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!429 = distinct !{!429, !416, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!430 = !{!431, !424, !421, !418, !415}
!431 = distinct !{!431, !432, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!432 = distinct !{!432, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!433 = !{!434}
!434 = distinct !{!434, !435, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!435 = distinct !{!435, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!436 = !{!437}
!437 = distinct !{!437, !438, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!438 = distinct !{!438, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!439 = !{!440}
!440 = distinct !{!440, !441, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!441 = distinct !{!441, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!442 = !{!443}
!443 = distinct !{!443, !444, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!444 = distinct !{!444, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!445 = !{!446, !448, !443, !440, !437, !434}
!446 = distinct !{!446, !447, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!447 = distinct !{!447, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!448 = distinct !{!448, !449, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!449 = distinct !{!449, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!450 = !{!451, !452}
!451 = distinct !{!451, !438, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!452 = distinct !{!452, !435, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!453 = !{!443, !440, !437, !434}
!454 = !{!455, !443, !440, !437, !434}
!455 = distinct !{!455, !456, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!456 = distinct !{!456, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!457 = !{!458}
!458 = distinct !{!458, !459, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!459 = distinct !{!459, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!460 = !{!461}
!461 = distinct !{!461, !462, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!462 = distinct !{!462, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!463 = !{!464}
!464 = distinct !{!464, !465, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!465 = distinct !{!465, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!466 = !{!467}
!467 = distinct !{!467, !468, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!468 = distinct !{!468, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!469 = !{!470, !472, !467, !464, !461, !458}
!470 = distinct !{!470, !471, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!471 = distinct !{!471, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!472 = distinct !{!472, !473, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!473 = distinct !{!473, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!474 = !{!475, !476}
!475 = distinct !{!475, !462, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!476 = distinct !{!476, !459, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!477 = !{!467, !464, !461, !458}
!478 = !{!479, !467, !464, !461, !458}
!479 = distinct !{!479, !480, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!480 = distinct !{!480, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!481 = !{!482}
!482 = distinct !{!482, !483, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!483 = distinct !{!483, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!484 = !{!485}
!485 = distinct !{!485, !486, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!486 = distinct !{!486, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!487 = !{!488}
!488 = distinct !{!488, !489, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!489 = distinct !{!489, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!490 = !{!491}
!491 = distinct !{!491, !492, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!492 = distinct !{!492, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!493 = !{!494, !496, !491, !488, !485, !482}
!494 = distinct !{!494, !495, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!495 = distinct !{!495, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!496 = distinct !{!496, !497, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!497 = distinct !{!497, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!498 = !{!499, !500}
!499 = distinct !{!499, !486, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!500 = distinct !{!500, !483, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!501 = !{!491, !488, !485, !482}
!502 = !{!503, !491, !488, !485, !482}
!503 = distinct !{!503, !504, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!504 = distinct !{!504, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!505 = !{!506}
!506 = distinct !{!506, !507, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E: argument 0"}
!507 = distinct !{!507, !"_ZN5alloc6string6String13with_capacity17hcdae0764481270f6E"}
!508 = !{!509, !511, !506}
!509 = distinct !{!509, !510, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!510 = distinct !{!510, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!511 = distinct !{!511, !512, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E: argument 0"}
!512 = distinct !{!512, !"_ZN5alloc3vec12Vec$LT$T$GT$13with_capacity17h2e652f8a95764a42E"}
!513 = !{!514}
!514 = distinct !{!514, !515, !"_ZN4core6option19Option$LT$$RF$T$GT$6cloned17h035dfeacd0b5ece7E: argument 0"}
!515 = distinct !{!515, !"_ZN4core6option19Option$LT$$RF$T$GT$6cloned17h035dfeacd0b5ece7E"}
!516 = !{!517, !519}
!517 = distinct !{!517, !518, !"_ZN104_$LT$core..iter..adapters..cloned..Cloned$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h2e9ba807445181abE: %self"}
!518 = distinct !{!518, !"_ZN104_$LT$core..iter..adapters..cloned..Cloned$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h2e9ba807445181abE"}
!519 = distinct !{!519, !520, !"_ZN99_$LT$core..char..decode..DecodeUtf16$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h733b4478207c7ac0E: %self"}
!520 = distinct !{!520, !"_ZN99_$LT$core..char..decode..DecodeUtf16$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h733b4478207c7ac0E"}
!521 = !{!522}
!522 = distinct !{!522, !523, !"_ZN4core6option19Option$LT$$RF$T$GT$6cloned17h035dfeacd0b5ece7E: argument 0"}
!523 = distinct !{!523, !"_ZN4core6option19Option$LT$$RF$T$GT$6cloned17h035dfeacd0b5ece7E"}
!524 = !{!525, !519}
!525 = distinct !{!525, !526, !"_ZN104_$LT$core..iter..adapters..cloned..Cloned$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h2e9ba807445181abE: %self"}
!526 = distinct !{!526, !"_ZN104_$LT$core..iter..adapters..cloned..Cloned$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h2e9ba807445181abE"}
!527 = !{!528}
!528 = distinct !{!528, !529, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$14into_raw_parts17hda1ec1e5156fbb0bE: argument 0"}
!529 = distinct !{!529, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$14into_raw_parts17hda1ec1e5156fbb0bE"}
!530 = !{!531}
!531 = distinct !{!531, !529, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$14into_raw_parts17hda1ec1e5156fbb0bE: %self"}
!532 = !{!533}
!533 = distinct !{!533, !534, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$11try_reserve17haa990c326ba46a91E: argument 0"}
!534 = distinct !{!534, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$11try_reserve17haa990c326ba46a91E"}
!535 = !{!536}
!536 = distinct !{!536, !534, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$11try_reserve17haa990c326ba46a91E: %self"}
!537 = !{!538}
!538 = distinct !{!538, !539, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$11try_reserve17h9118dc770232230dE: argument 0"}
!539 = distinct !{!539, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$11try_reserve17h9118dc770232230dE"}
!540 = !{!541}
!541 = distinct !{!541, !539, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$11try_reserve17h9118dc770232230dE: %self"}
!542 = !{!541, !536}
!543 = !{!538, !533}
!544 = !{!545}
!545 = distinct !{!545, !546, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17hde794f90541644ebE: argument 0"}
!546 = distinct !{!546, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17hde794f90541644ebE"}
!547 = !{!548}
!548 = distinct !{!548, !546, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17hde794f90541644ebE: %self"}
!549 = !{!545, !548, !538, !541, !533, !536}
!550 = !{!548, !541, !536}
!551 = !{!545, !538, !533}
!552 = !{!553}
!553 = distinct !{!553, !554, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E: argument 0"}
!554 = distinct !{!554, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E"}
!555 = !{!556}
!556 = distinct !{!556, !557, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E: %self"}
!557 = distinct !{!557, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E"}
!558 = !{!559, !545, !548, !538, !541, !533, !536}
!559 = distinct !{!559, !557, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E: argument 0"}
!560 = !{!561, !545, !538, !533}
!561 = distinct !{!561, !562, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17hc59085a866b9baadE: argument 0"}
!562 = distinct !{!562, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17hc59085a866b9baadE"}
!563 = !{!564, !548, !541, !536}
!564 = distinct !{!564, !565, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17hca4d48779be255e5E: %self"}
!565 = distinct !{!565, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17hca4d48779be255e5E"}
!566 = !{!567, !545, !538, !533}
!567 = distinct !{!567, !568, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17hb249c1c3764491f2E: argument 0"}
!568 = distinct !{!568, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17hb249c1c3764491f2E"}
!569 = !{!570}
!570 = distinct !{!570, !571, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17try_reserve_exact17h0654327a5aa1fc1eE: argument 0"}
!571 = distinct !{!571, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17try_reserve_exact17h0654327a5aa1fc1eE"}
!572 = !{!573}
!573 = distinct !{!573, !571, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17try_reserve_exact17h0654327a5aa1fc1eE: %self"}
!574 = !{!575}
!575 = distinct !{!575, !576, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$17try_reserve_exact17h994c6ee5195aa040E: argument 0"}
!576 = distinct !{!576, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$17try_reserve_exact17h994c6ee5195aa040E"}
!577 = !{!578}
!578 = distinct !{!578, !576, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$17try_reserve_exact17h994c6ee5195aa040E: %self"}
!579 = !{!578, !573}
!580 = !{!575, !570}
!581 = !{!582}
!582 = distinct !{!582, !583, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$10grow_exact17h9631642c9632acc6E: argument 0"}
!583 = distinct !{!583, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$10grow_exact17h9631642c9632acc6E"}
!584 = !{!585}
!585 = distinct !{!585, !583, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$10grow_exact17h9631642c9632acc6E: %self"}
!586 = !{!582, !585, !575, !578, !570, !573}
!587 = !{!585, !578, !573}
!588 = !{!582, !575, !570}
!589 = !{!590}
!590 = distinct !{!590, !591, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E: argument 0"}
!591 = distinct !{!591, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hf2345801f731b118E"}
!592 = !{!593}
!593 = distinct !{!593, !594, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E: %self"}
!594 = distinct !{!594, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E"}
!595 = !{!596, !582, !585, !575, !578, !570, !573}
!596 = distinct !{!596, !594, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17hf268dc85f644b0c8E: argument 0"}
!597 = !{!598, !582, !575, !570}
!598 = distinct !{!598, !599, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17hc59085a866b9baadE: argument 0"}
!599 = distinct !{!599, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17hc59085a866b9baadE"}
!600 = !{!601, !585, !578, !573}
!601 = distinct !{!601, !602, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17hca4d48779be255e5E: %self"}
!602 = distinct !{!602, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17hca4d48779be255e5E"}
!603 = !{!604, !582, !575, !570}
!604 = distinct !{!604, !605, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17hb249c1c3764491f2E: argument 0"}
!605 = distinct !{!605, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17hb249c1c3764491f2E"}
!606 = !{!607}
!607 = distinct !{!607, !608, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$4push17h009fa766e4e00c1aE: %self"}
!608 = distinct !{!608, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$4push17h009fa766e4e00c1aE"}
!609 = !{!610, !607}
!610 = distinct !{!610, !611, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!611 = distinct !{!611, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!612 = !{!613, !615}
!613 = distinct !{!613, !614, !"_ZN4core4char7methods15encode_utf8_raw17hcb7e96baf78db07aE: %dst.0"}
!614 = distinct !{!614, !"_ZN4core4char7methods15encode_utf8_raw17hcb7e96baf78db07aE"}
!615 = distinct !{!615, !616, !"_ZN4core4char7methods22_$LT$impl$u20$char$GT$11encode_utf817h03909e731d588b87E: %dst.0"}
!616 = distinct !{!616, !"_ZN4core4char7methods22_$LT$impl$u20$char$GT$11encode_utf817h03909e731d588b87E"}
!617 = !{!618}
!618 = distinct !{!618, !619, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!619 = distinct !{!619, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!620 = !{!621}
!621 = distinct !{!621, !622, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!622 = distinct !{!622, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!623 = !{!624}
!624 = distinct !{!624, !625, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!625 = distinct !{!625, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!626 = !{!627, !624, !621, !618}
!627 = distinct !{!627, !628, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!628 = distinct !{!628, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!629 = !{!630}
!630 = distinct !{!630, !619, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!631 = !{!632, !627, !624, !621, !618}
!632 = distinct !{!632, !633, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!633 = distinct !{!633, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!634 = !{!624, !621, !618}
!635 = !{!636, !624, !621, !618}
!636 = distinct !{!636, !637, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!637 = distinct !{!637, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!638 = !{!639}
!639 = distinct !{!639, !640, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7set_len17h03d334d9c3baa92eE: %self"}
!640 = distinct !{!640, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7set_len17h03d334d9c3baa92eE"}
!641 = !{!642, !644}
!642 = distinct !{!642, !643, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!643 = distinct !{!643, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!644 = distinct !{!644, !645, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!645 = distinct !{!645, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!646 = !{!647}
!647 = distinct !{!647, !648, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6as_ptr17hd3fc990f01a090c2E: %self"}
!648 = distinct !{!648, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6as_ptr17hd3fc990f01a090c2E"}
!649 = !{!650}
!650 = distinct !{!650, !651, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7set_len17h03d334d9c3baa92eE: %self"}
!651 = distinct !{!651, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7set_len17h03d334d9c3baa92eE"}
!652 = !{!653}
!653 = distinct !{!653, !654, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6as_ptr17hd3fc990f01a090c2E: %self"}
!654 = distinct !{!654, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6as_ptr17hd3fc990f01a090c2E"}
!655 = !{!656, !658, !660, !661, !663, !664, !666, !667}
!656 = distinct !{!656, !657, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!657 = distinct !{!657, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!658 = distinct !{!658, !659, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hd747977ca58e236cE: %v"}
!659 = distinct !{!659, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hd747977ca58e236cE"}
!660 = distinct !{!660, !659, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hd747977ca58e236cE: %s.0"}
!661 = distinct !{!661, !662, !"_ZN5alloc5slice4hack6to_vec17h336ede9909eb7f9fE: argument 0"}
!662 = distinct !{!662, !"_ZN5alloc5slice4hack6to_vec17h336ede9909eb7f9fE"}
!663 = distinct !{!663, !662, !"_ZN5alloc5slice4hack6to_vec17h336ede9909eb7f9fE: %s.0"}
!664 = distinct !{!664, !665, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h658aa7b17d48faaaE: argument 0"}
!665 = distinct !{!665, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h658aa7b17d48faaaE"}
!666 = distinct !{!666, !665, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h658aa7b17d48faaaE: %self.0"}
!667 = distinct !{!667, !668, !"_ZN67_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..clone..Clone$GT$5clone17h57ee0b5826626015E: argument 0"}
!668 = distinct !{!668, !"_ZN67_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..clone..Clone$GT$5clone17h57ee0b5826626015E"}
!669 = !{!658, !661, !664, !667}
!670 = !{!671}
!671 = distinct !{!671, !672, !"_ZN67_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..clone..Clone$GT$10clone_from17hbe68e0b75b488abaE: %self"}
!672 = distinct !{!672, !"_ZN67_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..clone..Clone$GT$10clone_from17hbe68e0b75b488abaE"}
!673 = !{!674}
!674 = distinct !{!674, !675, !"_ZN74_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..SpecCloneFrom$GT$10clone_from17h11a9555c5aa34c7dE: %this"}
!675 = distinct !{!675, !"_ZN74_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..SpecCloneFrom$GT$10clone_from17h11a9555c5aa34c7dE"}
!676 = !{!677, !679, !674, !671}
!677 = distinct !{!677, !678, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$8truncate17hb7f76b27637c74fbE: %self"}
!678 = distinct !{!678, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$8truncate17hb7f76b27637c74fbE"}
!679 = distinct !{!679, !680, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$5clear17h542bb6776ea92c5fE: %self"}
!680 = distinct !{!680, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$5clear17h542bb6776ea92c5fE"}
!681 = !{!682}
!682 = distinct !{!682, !683, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!683 = distinct !{!683, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!684 = !{!685}
!685 = distinct !{!685, !686, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!686 = distinct !{!686, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!687 = !{!688}
!688 = distinct !{!688, !689, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!689 = distinct !{!689, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!690 = !{!691, !693, !688, !685, !682, !674, !671}
!691 = distinct !{!691, !692, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!692 = distinct !{!692, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!693 = distinct !{!693, !694, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!694 = distinct !{!694, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!695 = !{!696}
!696 = distinct !{!696, !683, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!697 = !{!688, !685, !682, !674, !671}
!698 = !{!699, !688, !685, !682, !674, !671}
!699 = distinct !{!699, !700, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!700 = distinct !{!700, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!701 = !{!702, !704}
!702 = distinct !{!702, !703, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6as_ptr17hd3fc990f01a090c2E: %self"}
!703 = distinct !{!703, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6as_ptr17hd3fc990f01a090c2E"}
!704 = distinct !{!704, !705, !"_ZN100_$LT$alloc..string..String$u20$as$u20$core..ops..index..Index$LT$core..ops..range..RangeFull$GT$$GT$5index17he86ea55d34504c1dE: %self"}
!705 = distinct !{!705, !"_ZN100_$LT$alloc..string..String$u20$as$u20$core..ops..index..Index$LT$core..ops..range..RangeFull$GT$$GT$5index17he86ea55d34504c1dE"}
!706 = !{!704}
!707 = !{!708, !710, !711}
!708 = distinct !{!708, !709, !"_ZN5alloc6string6String19from_utf8_unchecked17ha4ad48367349c805E: argument 0"}
!709 = distinct !{!709, !"_ZN5alloc6string6String19from_utf8_unchecked17ha4ad48367349c805E"}
!710 = distinct !{!710, !709, !"_ZN5alloc6string6String19from_utf8_unchecked17ha4ad48367349c805E: %bytes"}
!711 = distinct !{!711, !712, !"_ZN5alloc3str21_$LT$impl$u20$str$GT$11into_string17h0e8d4567b5df36b2E: argument 0"}
!712 = distinct !{!712, !"_ZN5alloc3str21_$LT$impl$u20$str$GT$11into_string17h0e8d4567b5df36b2E"}
!713 = !{!714}
!714 = distinct !{!714, !712, !"_ZN5alloc3str21_$LT$impl$u20$str$GT$11into_string17h0e8d4567b5df36b2E: %self.0"}
!715 = !{!716, !718, !719, !721, !723, !725}
!716 = distinct !{!716, !717, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$6shrink17hdd2a6b1e54d6a3ccE: argument 0"}
!717 = distinct !{!717, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$6shrink17hdd2a6b1e54d6a3ccE"}
!718 = distinct !{!718, !717, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$6shrink17hdd2a6b1e54d6a3ccE: %self"}
!719 = distinct !{!719, !720, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$13shrink_to_fit17h72e9e54cf9fc6026E: %self"}
!720 = distinct !{!720, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$13shrink_to_fit17h72e9e54cf9fc6026E"}
!721 = distinct !{!721, !722, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$13shrink_to_fit17hf808438a61c3e7acE: %self"}
!722 = distinct !{!722, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$13shrink_to_fit17hf808438a61c3e7acE"}
!723 = distinct !{!723, !724, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16into_boxed_slice17h0f8d95326d30bfb5E: %self"}
!724 = distinct !{!724, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16into_boxed_slice17h0f8d95326d30bfb5E"}
!725 = distinct !{!725, !726, !"_ZN5alloc6string6String14into_boxed_str17hb5bc62edbdcb4638E: %self"}
!726 = distinct !{!726, !"_ZN5alloc6string6String14into_boxed_str17hb5bc62edbdcb4638E"}
!727 = !{!728, !719, !721, !723, !725}
!728 = distinct !{!728, !729, !"_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE: %result"}
!729 = distinct !{!729, !"_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE"}
!730 = !{!731}
!731 = distinct !{!731, !732, !"_ZN5alloc6borrow12Cow$LT$B$GT$10into_owned17h48a33bbc96bb20d5E: argument 0"}
!732 = distinct !{!732, !"_ZN5alloc6borrow12Cow$LT$B$GT$10into_owned17h48a33bbc96bb20d5E"}
!733 = !{!734}
!734 = distinct !{!734, !732, !"_ZN5alloc6borrow12Cow$LT$B$GT$10into_owned17h48a33bbc96bb20d5E: %self"}
!735 = !{!736}
!736 = distinct !{!736, !737, !"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E: argument 0"}
!737 = distinct !{!737, !"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E"}
!738 = !{!739, !741, !743, !744, !746, !747, !749, !750, !752, !753, !755, !736, !756, !731, !734}
!739 = distinct !{!739, !740, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!740 = distinct !{!740, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!741 = distinct !{!741, !742, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hd747977ca58e236cE: %v"}
!742 = distinct !{!742, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hd747977ca58e236cE"}
!743 = distinct !{!743, !742, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hd747977ca58e236cE: %s.0"}
!744 = distinct !{!744, !745, !"_ZN5alloc5slice4hack6to_vec17h336ede9909eb7f9fE: argument 0"}
!745 = distinct !{!745, !"_ZN5alloc5slice4hack6to_vec17h336ede9909eb7f9fE"}
!746 = distinct !{!746, !745, !"_ZN5alloc5slice4hack6to_vec17h336ede9909eb7f9fE: %s.0"}
!747 = distinct !{!747, !748, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h658aa7b17d48faaaE: argument 0"}
!748 = distinct !{!748, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h658aa7b17d48faaaE"}
!749 = distinct !{!749, !748, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h658aa7b17d48faaaE: %self.0"}
!750 = distinct !{!750, !751, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6to_vec17heb835260bc5dd9f7E: argument 0"}
!751 = distinct !{!751, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6to_vec17heb835260bc5dd9f7E"}
!752 = distinct !{!752, !751, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6to_vec17heb835260bc5dd9f7E: %self.0"}
!753 = distinct !{!753, !754, !"_ZN5alloc5slice64_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$$u5b$T$u5d$$GT$8to_owned17hedbf02934fc04ae1E: argument 0"}
!754 = distinct !{!754, !"_ZN5alloc5slice64_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$$u5b$T$u5d$$GT$8to_owned17hedbf02934fc04ae1E"}
!755 = distinct !{!755, !754, !"_ZN5alloc5slice64_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$$u5b$T$u5d$$GT$8to_owned17hedbf02934fc04ae1E: %self.0"}
!756 = distinct !{!756, !737, !"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E: %self.0"}
!757 = !{!741, !744, !747, !750, !753, !736, !731, !734}
!758 = !{!759, !761, !736, !731}
!759 = distinct !{!759, !760, !"_ZN5alloc6string6String19from_utf8_unchecked17ha4ad48367349c805E: argument 0"}
!760 = distinct !{!760, !"_ZN5alloc6string6String19from_utf8_unchecked17ha4ad48367349c805E"}
!761 = distinct !{!761, !760, !"_ZN5alloc6string6String19from_utf8_unchecked17ha4ad48367349c805E: %bytes"}
!762 = !{!756, !734}
!763 = !{!731, !734}
!764 = !{!765}
!765 = distinct !{!765, !766, !"_ZN5alloc6string5Drain6as_str17hf05b746e91bda355E: %self"}
!766 = distinct !{!766, !"_ZN5alloc6string5Drain6as_str17hf05b746e91bda355E"}
!767 = !{!768, !770}
!768 = distinct !{!768, !769, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7set_len17h03d334d9c3baa92eE: %self"}
!769 = distinct !{!769, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7set_len17h03d334d9c3baa92eE"}
!770 = distinct !{!770, !771, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$5drain17hb17f3f772d3c18a6E: %self"}
!771 = distinct !{!771, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$5drain17hb17f3f772d3c18a6E"}
!772 = !{!773}
!773 = distinct !{!773, !771, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$5drain17hb17f3f772d3c18a6E: argument 0"}
!774 = !{!775, !770}
!775 = distinct !{!775, !776, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!776 = distinct !{!776, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!777 = !{!778, !780}
!778 = distinct !{!778, !779, !"_ZN150_$LT$$LT$alloc..vec..drain..Drain$LT$T$C$A$GT$$u20$as$u20$core..ops..drop..Drop$GT$..drop..DropGuard$LT$T$C$A$GT$$u20$as$u20$core..ops..drop..Drop$GT$4drop17he17301d4d08fe4b7E: %self"}
!779 = distinct !{!779, !"_ZN150_$LT$$LT$alloc..vec..drain..Drain$LT$T$C$A$GT$$u20$as$u20$core..ops..drop..Drop$GT$..drop..DropGuard$LT$T$C$A$GT$$u20$as$u20$core..ops..drop..Drop$GT$4drop17he17301d4d08fe4b7E"}
!780 = distinct !{!780, !781, !"_ZN79_$LT$alloc..vec..drain..Drain$LT$T$C$A$GT$$u20$as$u20$core..ops..drop..Drop$GT$4drop17hf7694abe7a9c3175E: %self"}
!781 = distinct !{!781, !"_ZN79_$LT$alloc..vec..drain..Drain$LT$T$C$A$GT$$u20$as$u20$core..ops..drop..Drop$GT$4drop17hf7694abe7a9c3175E"}
!782 = !{!780}
!783 = !{!784}
!784 = distinct !{!784, !785, !"_ZN5alloc6string5Drain6as_str17hf05b746e91bda355E: %self"}
!785 = distinct !{!785, !"_ZN5alloc6string5Drain6as_str17hf05b746e91bda355E"}
!786 = !{!787}
!787 = distinct !{!787, !788, !"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$9size_hint17he6f5bad8108884edE: argument 0"}
!788 = distinct !{!788, !"_ZN81_$LT$core..str..iter..Chars$u20$as$u20$core..iter..traits..iterator..Iterator$GT$9size_hint17he6f5bad8108884edE"}
!789 = !{!790}
!790 = distinct !{!790, !791, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: argument 0"}
!791 = distinct !{!791, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E"}
!792 = !{!793, !794}
!793 = distinct !{!793, !791, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: %pieces.0"}
!794 = distinct !{!794, !791, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: %args.0"}
!795 = !{!796}
!796 = distinct !{!796, !797, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: argument 0"}
!797 = distinct !{!797, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E"}
!798 = !{!799, !800}
!799 = distinct !{!799, !797, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: %pieces.0"}
!800 = distinct !{!800, !797, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: %args.0"}
!801 = !{!802}
!802 = distinct !{!802, !803, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: argument 0"}
!803 = distinct !{!803, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E"}
!804 = !{!805, !806}
!805 = distinct !{!805, !803, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: %pieces.0"}
!806 = distinct !{!806, !803, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: %args.0"}
!807 = !{!808}
!808 = distinct !{!808, !809, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: argument 0"}
!809 = distinct !{!809, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E"}
!810 = !{!811, !812}
!811 = distinct !{!811, !809, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: %pieces.0"}
!812 = distinct !{!812, !809, !"_ZN4core3fmt9Arguments6new_v117h806f9b7eed59c1b1E: %args.0"}
!813 = !{!814}
!814 = distinct !{!814, !815, !"_ZN87_$LT$alloc..vec..Vec$LT$T$GT$$u20$as$u20$core..convert..From$LT$$RF$$u5b$T$u5d$$GT$$GT$4from17hc8bb33dd7aa72e01E: argument 0"}
!815 = distinct !{!815, !"_ZN87_$LT$alloc..vec..Vec$LT$T$GT$$u20$as$u20$core..convert..From$LT$$RF$$u5b$T$u5d$$GT$$GT$4from17hc8bb33dd7aa72e01E"}
!816 = !{!817}
!817 = distinct !{!817, !818, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6to_vec17heb835260bc5dd9f7E: argument 0"}
!818 = distinct !{!818, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6to_vec17heb835260bc5dd9f7E"}
!819 = !{!820}
!820 = distinct !{!820, !821, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h658aa7b17d48faaaE: argument 0"}
!821 = distinct !{!821, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h658aa7b17d48faaaE"}
!822 = !{!823}
!823 = distinct !{!823, !824, !"_ZN5alloc5slice4hack6to_vec17h336ede9909eb7f9fE: argument 0"}
!824 = distinct !{!824, !"_ZN5alloc5slice4hack6to_vec17h336ede9909eb7f9fE"}
!825 = !{!826}
!826 = distinct !{!826, !827, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hd747977ca58e236cE: %v"}
!827 = distinct !{!827, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hd747977ca58e236cE"}
!828 = !{!829}
!829 = distinct !{!829, !830, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE: argument 0"}
!830 = distinct !{!830, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17hf9ef86b0b27eb6ccE"}
!831 = !{!829, !826, !832, !823, !833, !820, !834, !817, !835, !814, !836}
!832 = distinct !{!832, !827, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hd747977ca58e236cE: %s.0"}
!833 = distinct !{!833, !824, !"_ZN5alloc5slice4hack6to_vec17h336ede9909eb7f9fE: %s.0"}
!834 = distinct !{!834, !821, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h658aa7b17d48faaaE: %self.0"}
!835 = distinct !{!835, !818, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6to_vec17heb835260bc5dd9f7E: %self.0"}
!836 = distinct !{!836, !815, !"_ZN87_$LT$alloc..vec..Vec$LT$T$GT$$u20$as$u20$core..convert..From$LT$$RF$$u5b$T$u5d$$GT$$GT$4from17hc8bb33dd7aa72e01E: %s.0"}
!837 = !{!829, !826, !823, !820, !817, !814}
!838 = !{!832, !833, !834, !835, !836}
!839 = !{!826, !823, !820, !817, !814}
!840 = !{!841, !826, !823, !820, !817, !814}
!841 = distinct !{!841, !842, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7set_len17h03d334d9c3baa92eE: %self"}
!842 = distinct !{!842, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7set_len17h03d334d9c3baa92eE"}
!843 = !{!844}
!844 = distinct !{!844, !845, !"_ZN58_$LT$alloc..string..String$u20$as$u20$core..fmt..Write$GT$9write_str17h3d99b06320bc832dE: %self"}
!845 = distinct !{!845, !"_ZN58_$LT$alloc..string..String$u20$as$u20$core..fmt..Write$GT$9write_str17h3d99b06320bc832dE"}
!846 = !{!847}
!847 = distinct !{!847, !848, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!848 = distinct !{!848, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!849 = !{!850}
!850 = distinct !{!850, !851, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %self"}
!851 = distinct !{!851, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E"}
!852 = !{!853}
!853 = distinct !{!853, !854, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE: %self"}
!854 = distinct !{!854, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h7b8a5e710e43acbeE"}
!855 = !{!856}
!856 = distinct !{!856, !857, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E: %self"}
!857 = distinct !{!857, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h397562ca194b06a6E"}
!858 = !{!859, !856, !853, !850, !847, !844}
!859 = distinct !{!859, !860, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E: %self"}
!860 = distinct !{!860, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17hde5497419433def7E"}
!861 = !{!862, !863, !864}
!862 = distinct !{!862, !851, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17h81130cdbab76f724E: %other.0"}
!863 = distinct !{!863, !848, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!864 = distinct !{!864, !845, !"_ZN58_$LT$alloc..string..String$u20$as$u20$core..fmt..Write$GT$9write_str17h3d99b06320bc832dE: %s.0"}
!865 = !{!866, !859, !856, !853, !850, !847, !844}
!866 = distinct !{!866, !867, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE: %self"}
!867 = distinct !{!867, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17he4b4423201f4136aE"}
!868 = !{!856, !853, !850, !847, !844}
!869 = !{!870, !856, !853, !850, !847, !844}
!870 = distinct !{!870, !871, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E: %self"}
!871 = distinct !{!871, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17h84a1d6019d6451c9E"}
!872 = !{!873, !875}
!873 = distinct !{!873, !874, !"_ZN4core3fmt5Write9write_fmt17hf1b0536c1be1d21aE: argument 0"}
!874 = distinct !{!874, !"_ZN4core3fmt5Write9write_fmt17hf1b0536c1be1d21aE"}
!875 = distinct !{!875, !874, !"_ZN4core3fmt5Write9write_fmt17hf1b0536c1be1d21aE: %args"}
!876 = !{!875}
!877 = !{!878}
!878 = distinct !{!878, !879, !"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E: %self"}
!879 = distinct !{!879, !"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E"}
!880 = !{!881}
!881 = distinct !{!881, !882, !"_ZN76_$LT$alloc..collections..TryReserveErrorKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h0e4fd2ec2b87f863E: %self"}
!882 = distinct !{!882, !"_ZN76_$LT$alloc..collections..TryReserveErrorKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h0e4fd2ec2b87f863E"}
!883 = !{!884}
!884 = distinct !{!884, !882, !"_ZN76_$LT$alloc..collections..TryReserveErrorKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h0e4fd2ec2b87f863E: %f"}
!885 = !{!881, !884}
!886 = !{!887, !889, !890}
!887 = distinct !{!887, !888, !"_ZN48_$LT$$u5b$T$u5d$$u20$as$u20$core..fmt..Debug$GT$3fmt17h01f97332363debfdE: %self.0"}
!888 = distinct !{!888, !"_ZN48_$LT$$u5b$T$u5d$$u20$as$u20$core..fmt..Debug$GT$3fmt17h01f97332363debfdE"}
!889 = distinct !{!889, !888, !"_ZN48_$LT$$u5b$T$u5d$$u20$as$u20$core..fmt..Debug$GT$3fmt17h01f97332363debfdE: %f"}
!890 = distinct !{!890, !891, !"_ZN65_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hbfa03955de355f46E: %f"}
!891 = distinct !{!891, !"_ZN65_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hbfa03955de355f46E"}
!892 = !{!887}
!893 = !{!894, !887, !889, !890}
!894 = distinct !{!894, !895, !"_ZN4core3fmt8builders9DebugList7entries17hc76413071dc331e1E: %self"}
!895 = distinct !{!895, !"_ZN4core3fmt8builders9DebugList7entries17hc76413071dc331e1E"}
