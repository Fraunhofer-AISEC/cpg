; ModuleID = 'std_detect.279db575-cgu.0'
source_filename = "std_detect.279db575-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"core::panic::location::Location" = type { { [0 x i8]*, i64 }, i32, i32 }

@_ZN10std_detect6detect5cache5CACHE17h265889881b49d5d4E = local_unnamed_addr global <{ [16 x i8] }> zeroinitializer, align 8
@alloc425 = private unnamed_addr constant <{ [40 x i8] }> <{ [40 x i8] c"internal error: entered unreachable code" }>, align 1
@alloc426 = private unnamed_addr constant <{ [144 x i8] }> <{ [144 x i8] c"/home/alwagner/.rustup/toolchains/nightly-x86_64-unknown-linux-gnu/lib/rustlib/src/rust/library/stdarch/crates/std_detect/src/detect/arch/x86.rs" }>, align 1
@alloc427 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [144 x i8] }>, <{ [144 x i8] }>* @alloc426, i32 0, i32 0, i32 0), [16 x i8] c"\90\00\00\00\00\00\00\00\12\00\00\00\01\00\00\00" }>, align 8
@alloc428 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"rtm" }>, align 1
@alloc429 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"adx" }>, align 1
@alloc430 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"cmpxchg16b" }>, align 1
@alloc431 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"xsavec" }>, align 1
@alloc432 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"xsaves" }>, align 1
@alloc433 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"xsaveopt" }>, align 1
@alloc434 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"xsave" }>, align 1
@alloc435 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"fxsr" }>, align 1
@alloc436 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"popcnt" }>, align 1
@alloc437 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"tbm" }>, align 1
@alloc438 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"lzcnt" }>, align 1
@alloc439 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"bmi2" }>, align 1
@alloc440 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"bmi1" }>, align 1
@alloc441 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"fma" }>, align 1
@alloc442 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"f16c" }>, align 1
@alloc443 = private unnamed_addr constant <{ [18 x i8] }> <{ [18 x i8] c"avx512vp2intersect" }>, align 1
@alloc444 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"avx512bf16" }>, align 1
@alloc445 = private unnamed_addr constant <{ [12 x i8] }> <{ [12 x i8] c"avx512bitalg" }>, align 1
@alloc446 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"avx512vnni" }>, align 1
@alloc447 = private unnamed_addr constant <{ [16 x i8] }> <{ [16 x i8] c"avx512vpclmulqdq" }>, align 1
@alloc448 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"avx512vaes" }>, align 1
@alloc449 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"avx512gfni" }>, align 1
@alloc450 = private unnamed_addr constant <{ [11 x i8] }> <{ [11 x i8] c"avx512vbmi2" }>, align 1
@alloc451 = private unnamed_addr constant <{ [15 x i8] }> <{ [15 x i8] c"avx512vpopcntdq" }>, align 1
@alloc452 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"avx512vbmi" }>, align 1
@alloc453 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"avx512ifma" }>, align 1
@alloc454 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"avx512vl" }>, align 1
@alloc455 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"avx512dq" }>, align 1
@alloc456 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"avx512bw" }>, align 1
@alloc457 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"avx512pf" }>, align 1
@alloc458 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"avx512er" }>, align 1
@alloc459 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"avx512cd" }>, align 1
@alloc460 = private unnamed_addr constant <{ [7 x i8] }> <{ [7 x i8] c"avx512f" }>, align 1
@alloc461 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"avx2" }>, align 1
@alloc462 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"avx" }>, align 1
@alloc463 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"sha" }>, align 1
@alloc464 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"sse4a" }>, align 1
@alloc465 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"sse4.2" }>, align 1
@alloc466 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"sse4.1" }>, align 1
@alloc467 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"ssse3" }>, align 1
@alloc468 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"sse3" }>, align 1
@alloc469 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"sse2" }>, align 1
@alloc470 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"sse" }>, align 1
@alloc471 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"mmx" }>, align 1
@alloc472 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"tsc" }>, align 1
@alloc473 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"rdseed" }>, align 1
@alloc474 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"rdrand" }>, align 1
@alloc475 = private unnamed_addr constant <{ [9 x i8] }> <{ [9 x i8] c"pclmulqdq" }>, align 1
@alloc476 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"aes" }>, align 1

; <std_detect::detect::cache::Initializer as core::default::Default>::default
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define i64 @"_ZN81_$LT$std_detect..detect..cache..Initializer$u20$as$u20$core..default..Default$GT$7default17h26a1fd977ab54822E"() unnamed_addr #0 {
start:
  ret i64 0
}

; std_detect::detect::cache::detect_and_initialize
; Function Attrs: cold nonlazybind uwtable
define i64 @_ZN10std_detect6detect5cache21detect_and_initialize17h5dd376b1bbe6dceaE() unnamed_addr #1 {
start:
  %0 = tail call { i32, i32, i32, i32 } asm sideeffect "movq %rbx, ${0:q}\0Acpuid\0Axchgq %rbx, ${0:q}", "=r,={ax},={cx},={dx},1,2,~{memory}"(i32 0, i32 0) #5, !srcloc !2
  %1 = extractvalue { i32, i32, i32, i32 } %0, 0
  %2 = extractvalue { i32, i32, i32, i32 } %0, 1
  %3 = extractvalue { i32, i32, i32, i32 } %0, 2
  %4 = extractvalue { i32, i32, i32, i32 } %0, 3
  %_23.i = icmp eq i32 %2, 0
  br i1 %_23.i, label %_ZN10std_detect6detect2os15detect_features17h9bd9f469aefffc93E.exit, label %bb11.i

bb11.i:                                           ; preds = %start
  %5 = tail call { i32, i32, i32, i32 } asm sideeffect "movq %rbx, ${0:q}\0Acpuid\0Axchgq %rbx, ${0:q}", "=r,={ax},={cx},={dx},1,2,~{memory}"(i32 1, i32 0) #5, !srcloc !2
  %6 = extractvalue { i32, i32, i32, i32 } %5, 2
  %7 = extractvalue { i32, i32, i32, i32 } %5, 3
  %.sroa.4227.0.extract.trunc.i = zext i32 %6 to i64
  %_31.i = icmp ugt i32 %2, 6
  br i1 %_31.i, label %bb13.i, label %bb16.i

bb13.i:                                           ; preds = %bb11.i
  %8 = tail call { i32, i32, i32, i32 } asm sideeffect "movq %rbx, ${0:q}\0Acpuid\0Axchgq %rbx, ${0:q}", "=r,={ax},={cx},={dx},1,2,~{memory}"(i32 7, i32 0) #5, !srcloc !2
  %9 = extractvalue { i32, i32, i32, i32 } %8, 0
  %10 = extractvalue { i32, i32, i32, i32 } %8, 2
  %.sroa.4234.0.extract.trunc.i = zext i32 %9 to i64
  %.sroa.5236.0.extract.trunc.i = zext i32 %10 to i64
  br label %bb16.i

bb16.i:                                           ; preds = %bb13.i, %bb11.i
  %_30.sroa.0.0.i = phi i64 [ %.sroa.4234.0.extract.trunc.i, %bb13.i ], [ 0, %bb11.i ]
  %_30.sroa.5.0.i = phi i64 [ %.sroa.5236.0.extract.trunc.i, %bb13.i ], [ 0, %bb11.i ]
  %11 = tail call { i32, i32, i32, i32 } asm sideeffect "movq %rbx, ${0:q}\0Acpuid\0Axchgq %rbx, ${0:q}", "=r,={ax},={cx},={dx},1,2,~{memory}"(i32 -2147483648, i32 0) #5, !srcloc !2
  %12 = extractvalue { i32, i32, i32, i32 } %11, 1
  %_41.not.i = icmp eq i32 %12, 0
  br i1 %_41.not.i, label %bb21.i, label %bb18.i

bb18.i:                                           ; preds = %bb16.i
  %13 = tail call { i32, i32, i32, i32 } asm sideeffect "movq %rbx, ${0:q}\0Acpuid\0Axchgq %rbx, ${0:q}", "=r,={ax},={cx},={dx},1,2,~{memory}"(i32 -2147483647, i32 0) #5, !srcloc !2
  %14 = extractvalue { i32, i32, i32, i32 } %13, 2
  %phi.cast.i = zext i32 %14 to i64
  br label %bb21.i

bb21.i:                                           ; preds = %bb18.i, %bb16.i
  %extended_proc_info_ecx.0.i = phi i64 [ %phi.cast.i, %bb18.i ], [ 0, %bb16.i ]
  %_3.i.i.i = shl nuw nsw i64 %.sroa.4227.0.extract.trunc.i, 8
  %15 = and i64 %_3.i.i.i, 256
  %_3.i.i360.i = shl i64 %.sroa.4227.0.extract.trunc.i, 33
  %16 = and i64 %_3.i.i360.i, 70368744177664
  %_3.i.i366.i = lshr i64 %.sroa.4227.0.extract.trunc.i, 9
  %17 = and i64 %_3.i.i366.i, 1024
  %18 = and i64 %_3.i.i366.i, 2048
  %_3.i.i378.i = shl nuw nsw i64 %.sroa.4227.0.extract.trunc.i, 17
  %19 = and i64 %_3.i.i378.i, 1099511627776
  %_3.i.i384.i = lshr i64 %.sroa.4227.0.extract.trunc.i, 25
  %20 = and i64 %_3.i.i384.i, 1
  %_3.i.i390.i = shl nuw nsw i64 %.sroa.4227.0.extract.trunc.i, 5
  %21 = and i64 %_3.i.i390.i, 17179869184
  %_3.i.i396.i = lshr i64 %.sroa.4227.0.extract.trunc.i, 28
  %22 = and i64 %_3.i.i396.i, 4
  %_3.i.i402.i = lshr i64 %_30.sroa.0.0.i, 15
  %23 = and i64 %_3.i.i402.i, 8
  %_3.i.i408.i = shl nuw nsw i64 %_30.sroa.0.0.i, 28
  %24 = and i64 %_3.i.i408.i, 140737488355328
  %_3.i.i414.i = shl i64 %_30.sroa.0.0.i, 37
  %25 = and i64 %_3.i.i414.i, 281474976710656
  %_6.i419.i = zext i32 %7 to i64
  %_3.i.i420.i = and i64 %_6.i419.i, 16
  %_3.i.i426.i = lshr i64 %_6.i419.i, 18
  %26 = and i64 %_3.i.i426.i, 32
  %_3.i.i432.i = shl nuw nsw i64 %_6.i419.i, 17
  %27 = and i64 %_3.i.i432.i, 2199023255552
  %_3.i.i438.i = lshr i64 %_6.i419.i, 19
  %28 = and i64 %_3.i.i438.i, 64
  %29 = and i64 %_3.i.i438.i, 128
  %_3.i.i450.i = lshr i64 %_30.sroa.0.0.i, 16
  %30 = and i64 %_3.i.i450.i, 8192
  %_3.i.i456.i = shl i64 %_30.sroa.0.0.i, 33
  %31 = and i64 %_3.i.i456.i, 68719476736
  %_3.i.i462.i = shl nuw nsw i64 %_30.sroa.0.0.i, 29
  %32 = and i64 %_3.i.i462.i, 137438953472
  %33 = and i64 %.sroa.4227.0.extract.trunc.i, 514
  %34 = or i64 %33, %_3.i.i420.i
  %35 = or i64 %34, %15
  %36 = or i64 %35, %16
  %37 = or i64 %36, %17
  %38 = or i64 %37, %18
  %39 = or i64 %38, %19
  %40 = or i64 %39, %20
  %41 = or i64 %40, %21
  %42 = or i64 %41, %22
  %43 = or i64 %42, %26
  %44 = or i64 %43, %27
  %45 = or i64 %44, %28
  %46 = or i64 %45, %29
  %47 = or i64 %46, %23
  %48 = or i64 %47, %24
  %49 = or i64 %48, %25
  %50 = or i64 %49, %30
  %51 = or i64 %50, %31
  %52 = or i64 %51, %32
  %53 = and i32 %6, 201326592
  %54 = icmp eq i32 %53, 201326592
  br i1 %54, label %bb46.i, label %bb81.i

bb81.i:                                           ; preds = %bb59.i, %bb55.i, %bb46.i, %bb21.i
  %value.21.i = phi i64 [ %64, %bb55.i ], [ %52, %bb46.i ], [ %52, %bb21.i ], [ %98, %bb59.i ]
  %_3.i.i468.i = shl i64 %extended_proc_info_ecx.0.i, 33
  %55 = and i64 %_3.i.i468.i, 274877906944
  %56 = or i64 %value.21.i, %55
  %vendor_id.sroa.8.0.insert.ext.i = zext i32 %3 to i96
  %vendor_id.sroa.8.0.insert.shift.i = shl nuw i96 %vendor_id.sroa.8.0.insert.ext.i, 64
  %vendor_id.sroa.7.0.insert.ext.i = zext i32 %4 to i96
  %vendor_id.sroa.7.0.insert.shift.i = shl nuw nsw i96 %vendor_id.sroa.7.0.insert.ext.i, 32
  %vendor_id.sroa.7.0.insert.insert.i = or i96 %vendor_id.sroa.8.0.insert.shift.i, %vendor_id.sroa.7.0.insert.shift.i
  %vendor_id.sroa.0.0.insert.ext.i = zext i32 %1 to i96
  %vendor_id.sroa.0.0.insert.insert.i = or i96 %vendor_id.sroa.7.0.insert.insert.i, %vendor_id.sroa.0.0.insert.ext.i
  switch i96 %vendor_id.sroa.0.0.insert.insert.i, label %bb91.i [
    i96 21138376743609660797026071873, label %bb88.i
    i96 31391465846818061943646419272, label %bb88.i
  ]

bb46.i:                                           ; preds = %bb21.i
; call core::core_arch::x86::xsave::_xgetbv
  %xcr0.i = tail call fastcc i64 @_ZN4core9core_arch3x865xsave7_xgetbv17h56f8691b4c7eb01fE()
  %_141.i = and i64 %xcr0.i, 6
  %os_avx_support.i = icmp eq i64 %_141.i, 6
  %_144.i = and i64 %xcr0.i, 224
  %os_avx512_support.i = icmp eq i64 %_144.i, 224
  br i1 %os_avx_support.i, label %bb48.i, label %bb81.i

bb48.i:                                           ; preds = %bb46.i
  %_3.i.i475.i = shl nuw nsw i64 %.sroa.4227.0.extract.trunc.i, 16
  %57 = and i64 %_3.i.i475.i, 4398046511104
  %58 = or i64 %52, %57
  %_151.i = icmp ugt i32 %2, 12
  br i1 %_151.i, label %bb50.i, label %bb55.i

bb55.i:                                           ; preds = %bb50.i, %bb48.i
  %value.24.i = phi i64 [ %58, %bb48.i ], [ %72, %bb50.i ]
  %_3.i.i481.i = shl nuw nsw i64 %.sroa.4227.0.extract.trunc.i, 23
  %59 = and i64 %_3.i.i481.i, 34359738368
  %_3.i.i487.i = lshr i64 %.sroa.4227.0.extract.trunc.i, 14
  %60 = and i64 %_3.i.i487.i, 16384
  %_3.i.i493.i = shl nuw nsw i64 %_30.sroa.0.0.i, 10
  %61 = and i64 %_3.i.i493.i, 32768
  %62 = or i64 %60, %59
  %63 = or i64 %62, %61
  %64 = or i64 %63, %value.24.i
  br i1 %os_avx512_support.i, label %bb59.i, label %bb81.i

bb50.i:                                           ; preds = %bb48.i
  %65 = tail call { i32, i32, i32, i32 } asm sideeffect "movq %rbx, ${0:q}\0Acpuid\0Axchgq %rbx, ${0:q}", "=r,={ax},={cx},={dx},1,2,~{memory}"(i32 13, i32 1) #5, !srcloc !2
  %66 = extractvalue { i32, i32, i32, i32 } %65, 1
  %_6.i498.i = zext i32 %66 to i64
  %_3.i.i499.i = shl i64 %_6.i498.i, 43
  %67 = and i64 %_3.i.i499.i, 8796093022208
  %68 = or i64 %67, %58
  %_3.i.i505.i = shl i64 %_6.i498.i, 44
  %69 = and i64 %_3.i.i505.i, 35184372088832
  %70 = or i64 %68, %69
  %_3.i.i511.i = shl i64 %_6.i498.i, 41
  %71 = and i64 %_3.i.i511.i, 17592186044416
  %72 = or i64 %70, %71
  br label %bb55.i

bb59.i:                                           ; preds = %bb55.i
  %_3.i.i517.i = and i64 %_30.sroa.0.0.i, 65536
  %_3.i.i523.i = shl nuw nsw i64 %_30.sroa.0.0.i, 4
  %73 = and i64 %_3.i.i523.i, 2097152
  %_3.i.i529.i = shl nuw nsw i64 %_30.sroa.0.0.i, 2
  %74 = and i64 %_3.i.i529.i, 8388608
  %_3.i.i535.i = lshr i64 %_30.sroa.0.0.i, 7
  %75 = and i64 %_3.i.i535.i, 524288
  %_3.i.i541.i = lshr i64 %_30.sroa.0.0.i, 9
  %76 = and i64 %_3.i.i541.i, 262144
  %_3.i.i547.i = lshr i64 %_30.sroa.0.0.i, 11
  %77 = and i64 %_3.i.i547.i, 131072
  %_3.i.i553.i = lshr i64 %_30.sroa.0.0.i, 10
  %78 = and i64 %_3.i.i553.i, 1048576
  %79 = and i64 %_3.i.i541.i, 4194304
  %_3.i.i565.i = shl nuw nsw i64 %_30.sroa.5.0.i, 23
  %80 = and i64 %_3.i.i565.i, 16777216
  %_3.i.i571.i = shl nuw nsw i64 %_30.sroa.5.0.i, 27
  %81 = and i64 %_3.i.i571.i, 4294967296
  %_3.i.i577.i = shl nuw nsw i64 %_30.sroa.5.0.i, 20
  %82 = and i64 %_3.i.i577.i, 67108864
  %83 = or i64 %73, %_3.i.i517.i
  %84 = or i64 %83, %74
  %85 = or i64 %84, %75
  %86 = or i64 %85, %76
  %87 = or i64 %86, %77
  %88 = or i64 %87, %78
  %89 = or i64 %88, %79
  %90 = or i64 %89, %80
  %91 = or i64 %90, %81
  %92 = or i64 %91, %82
  %93 = or i64 %92, %64
  %_3.i.i583.i = and i64 %_30.sroa.5.0.i, 256
  %.not.i584.i = icmp eq i64 %_3.i.i583.i, 0
  %94 = or i64 %93, 8724152320
  %value.43.i = select i1 %.not.i584.i, i64 %93, i64 %94
  %_3.i.i595.i = shl nuw nsw i64 %_30.sroa.5.0.i, 19
  %_3.i.i619.i = shl nuw nsw i64 %_30.sroa.5.0.i, 11
  %95 = and i64 %_3.i.i619.i, 33554432
  %96 = and i64 %_3.i.i595.i, 4026531840
  %97 = or i64 %96, %95
  %98 = or i64 %97, %value.43.i
  br label %bb81.i

bb91.i:                                           ; preds = %bb88.i, %bb81.i
  %value.49.i = phi i64 [ %104, %bb88.i ], [ %56, %bb81.i ]
  %99 = icmp eq i96 %vendor_id.sroa.0.0.insert.insert.i, 33547032397428960692454450503
  %_3.i.i636.i = and i64 %value.49.i, 16384
  %.not.i = icmp eq i64 %_3.i.i636.i, 0
  %or.cond.i = select i1 %99, i1 %.not.i, i1 false
  %100 = and i64 %value.49.i, -206158430209
  %spec.select.i = select i1 %or.cond.i, i64 %100, i64 %value.49.i
  br label %_ZN10std_detect6detect2os15detect_features17h9bd9f469aefffc93E.exit

bb88.i:                                           ; preds = %bb81.i, %bb81.i
  %_3.i.i625.i = shl nuw nsw i64 %extended_proc_info_ecx.0.i, 6
  %101 = and i64 %_3.i.i625.i, 4096
  %_3.i.i631.i = shl nuw nsw i64 %extended_proc_info_ecx.0.i, 18
  %102 = and i64 %_3.i.i631.i, 549755813888
  %103 = or i64 %102, %101
  %104 = or i64 %103, %56
  br label %bb91.i

_ZN10std_detect6detect2os15detect_features17h9bd9f469aefffc93E.exit: ; preds = %start, %bb91.i
  %.1.i = phi i64 [ 0, %start ], [ %spec.select.i, %bb91.i ]
  %_8.i.i.i = or i64 %.1.i, -9223372036854775808
  store atomic i64 %_8.i.i.i, i64* bitcast (<{ [16 x i8] }>* @_ZN10std_detect6detect5cache5CACHE17h265889881b49d5d4E to i64*) monotonic, align 8
  %_15.i.i = lshr i64 %.1.i, 63
  %_8.i1.i.i = or i64 %_15.i.i, -9223372036854775808
  store atomic i64 %_8.i1.i.i, i64* bitcast (i8* getelementptr inbounds (<{ [16 x i8] }>, <{ [16 x i8] }>* @_ZN10std_detect6detect5cache5CACHE17h265889881b49d5d4E, i64 0, i32 0, i64 8) to i64*) monotonic, align 8
  ret i64 %.1.i
}

; std_detect::detect::features
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define { i8, i8 } @_ZN10std_detect6detect8features17hc174137c6a1fafe8E() unnamed_addr #0 {
start:
  ret { i8, i8 } { i8 0, i8 49 }
}

; std_detect::detect::arch::Feature::to_str
; Function Attrs: nonlazybind uwtable
define { [0 x i8]*, i64 } @_ZN10std_detect6detect4arch7Feature6to_str17h01b5114e0e7c1168E(i8 %0) unnamed_addr #2 {
start:
  switch i8 %0, label %bb2 [
    i8 0, label %bb52
    i8 1, label %bb4
    i8 2, label %bb5
    i8 3, label %bb6
    i8 4, label %bb7
    i8 5, label %bb8
    i8 6, label %bb9
    i8 7, label %bb10
    i8 8, label %bb11
    i8 9, label %bb12
    i8 10, label %bb13
    i8 11, label %bb14
    i8 12, label %bb15
    i8 13, label %bb16
    i8 14, label %bb17
    i8 15, label %bb18
    i8 16, label %bb19
    i8 17, label %bb20
    i8 18, label %bb21
    i8 19, label %bb22
    i8 20, label %bb23
    i8 21, label %bb24
    i8 22, label %bb25
    i8 23, label %bb26
    i8 24, label %bb27
    i8 25, label %bb28
    i8 26, label %bb29
    i8 27, label %bb30
    i8 28, label %bb31
    i8 29, label %bb32
    i8 30, label %bb33
    i8 31, label %bb34
    i8 32, label %bb35
    i8 33, label %bb36
    i8 34, label %bb37
    i8 35, label %bb38
    i8 36, label %bb39
    i8 37, label %bb40
    i8 38, label %bb41
    i8 39, label %bb42
    i8 40, label %bb43
    i8 41, label %bb44
    i8 42, label %bb45
    i8 43, label %bb46
    i8 44, label %bb47
    i8 45, label %bb48
    i8 46, label %bb49
    i8 47, label %bb50
    i8 48, label %bb51
    i8 49, label %bb1
  ]

bb2:                                              ; preds = %start
  unreachable

bb4:                                              ; preds = %start
  br label %bb52

bb5:                                              ; preds = %start
  br label %bb52

bb6:                                              ; preds = %start
  br label %bb52

bb7:                                              ; preds = %start
  br label %bb52

bb8:                                              ; preds = %start
  br label %bb52

bb9:                                              ; preds = %start
  br label %bb52

bb10:                                             ; preds = %start
  br label %bb52

bb11:                                             ; preds = %start
  br label %bb52

bb12:                                             ; preds = %start
  br label %bb52

bb13:                                             ; preds = %start
  br label %bb52

bb14:                                             ; preds = %start
  br label %bb52

bb15:                                             ; preds = %start
  br label %bb52

bb16:                                             ; preds = %start
  br label %bb52

bb17:                                             ; preds = %start
  br label %bb52

bb18:                                             ; preds = %start
  br label %bb52

bb19:                                             ; preds = %start
  br label %bb52

bb20:                                             ; preds = %start
  br label %bb52

bb21:                                             ; preds = %start
  br label %bb52

bb22:                                             ; preds = %start
  br label %bb52

bb23:                                             ; preds = %start
  br label %bb52

bb24:                                             ; preds = %start
  br label %bb52

bb25:                                             ; preds = %start
  br label %bb52

bb26:                                             ; preds = %start
  br label %bb52

bb27:                                             ; preds = %start
  br label %bb52

bb28:                                             ; preds = %start
  br label %bb52

bb29:                                             ; preds = %start
  br label %bb52

bb30:                                             ; preds = %start
  br label %bb52

bb31:                                             ; preds = %start
  br label %bb52

bb32:                                             ; preds = %start
  br label %bb52

bb33:                                             ; preds = %start
  br label %bb52

bb34:                                             ; preds = %start
  br label %bb52

bb35:                                             ; preds = %start
  br label %bb52

bb36:                                             ; preds = %start
  br label %bb52

bb37:                                             ; preds = %start
  br label %bb52

bb38:                                             ; preds = %start
  br label %bb52

bb39:                                             ; preds = %start
  br label %bb52

bb40:                                             ; preds = %start
  br label %bb52

bb41:                                             ; preds = %start
  br label %bb52

bb42:                                             ; preds = %start
  br label %bb52

bb43:                                             ; preds = %start
  br label %bb52

bb44:                                             ; preds = %start
  br label %bb52

bb45:                                             ; preds = %start
  br label %bb52

bb46:                                             ; preds = %start
  br label %bb52

bb47:                                             ; preds = %start
  br label %bb52

bb48:                                             ; preds = %start
  br label %bb52

bb49:                                             ; preds = %start
  br label %bb52

bb50:                                             ; preds = %start
  br label %bb52

bb51:                                             ; preds = %start
  br label %bb52

bb1:                                              ; preds = %start
; call core::panicking::panic
  tail call void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [40 x i8] }>* @alloc425 to [0 x i8]*), i64 40, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc427 to %"core::panic::location::Location"*)) #6
  unreachable

bb52:                                             ; preds = %start, %bb4, %bb5, %bb6, %bb7, %bb8, %bb9, %bb10, %bb11, %bb12, %bb13, %bb14, %bb15, %bb16, %bb17, %bb18, %bb19, %bb20, %bb21, %bb22, %bb23, %bb24, %bb25, %bb26, %bb27, %bb28, %bb29, %bb30, %bb31, %bb32, %bb33, %bb34, %bb35, %bb36, %bb37, %bb38, %bb39, %bb40, %bb41, %bb42, %bb43, %bb44, %bb45, %bb46, %bb47, %bb48, %bb49, %bb50, %bb51
  %.sroa.0.0 = phi [0 x i8]* [ bitcast (<{ [3 x i8] }>* @alloc428 to [0 x i8]*), %bb51 ], [ bitcast (<{ [3 x i8] }>* @alloc429 to [0 x i8]*), %bb50 ], [ bitcast (<{ [10 x i8] }>* @alloc430 to [0 x i8]*), %bb49 ], [ bitcast (<{ [6 x i8] }>* @alloc431 to [0 x i8]*), %bb48 ], [ bitcast (<{ [6 x i8] }>* @alloc432 to [0 x i8]*), %bb47 ], [ bitcast (<{ [8 x i8] }>* @alloc433 to [0 x i8]*), %bb46 ], [ bitcast (<{ [5 x i8] }>* @alloc434 to [0 x i8]*), %bb45 ], [ bitcast (<{ [4 x i8] }>* @alloc435 to [0 x i8]*), %bb44 ], [ bitcast (<{ [6 x i8] }>* @alloc436 to [0 x i8]*), %bb43 ], [ bitcast (<{ [3 x i8] }>* @alloc437 to [0 x i8]*), %bb42 ], [ bitcast (<{ [5 x i8] }>* @alloc438 to [0 x i8]*), %bb41 ], [ bitcast (<{ [4 x i8] }>* @alloc439 to [0 x i8]*), %bb40 ], [ bitcast (<{ [4 x i8] }>* @alloc440 to [0 x i8]*), %bb39 ], [ bitcast (<{ [3 x i8] }>* @alloc441 to [0 x i8]*), %bb38 ], [ bitcast (<{ [4 x i8] }>* @alloc442 to [0 x i8]*), %bb37 ], [ bitcast (<{ [18 x i8] }>* @alloc443 to [0 x i8]*), %bb36 ], [ bitcast (<{ [10 x i8] }>* @alloc444 to [0 x i8]*), %bb35 ], [ bitcast (<{ [12 x i8] }>* @alloc445 to [0 x i8]*), %bb34 ], [ bitcast (<{ [10 x i8] }>* @alloc446 to [0 x i8]*), %bb33 ], [ bitcast (<{ [16 x i8] }>* @alloc447 to [0 x i8]*), %bb32 ], [ bitcast (<{ [10 x i8] }>* @alloc448 to [0 x i8]*), %bb31 ], [ bitcast (<{ [10 x i8] }>* @alloc449 to [0 x i8]*), %bb30 ], [ bitcast (<{ [11 x i8] }>* @alloc450 to [0 x i8]*), %bb29 ], [ bitcast (<{ [15 x i8] }>* @alloc451 to [0 x i8]*), %bb28 ], [ bitcast (<{ [10 x i8] }>* @alloc452 to [0 x i8]*), %bb27 ], [ bitcast (<{ [10 x i8] }>* @alloc453 to [0 x i8]*), %bb26 ], [ bitcast (<{ [8 x i8] }>* @alloc454 to [0 x i8]*), %bb25 ], [ bitcast (<{ [8 x i8] }>* @alloc455 to [0 x i8]*), %bb24 ], [ bitcast (<{ [8 x i8] }>* @alloc456 to [0 x i8]*), %bb23 ], [ bitcast (<{ [8 x i8] }>* @alloc457 to [0 x i8]*), %bb22 ], [ bitcast (<{ [8 x i8] }>* @alloc458 to [0 x i8]*), %bb21 ], [ bitcast (<{ [8 x i8] }>* @alloc459 to [0 x i8]*), %bb20 ], [ bitcast (<{ [7 x i8] }>* @alloc460 to [0 x i8]*), %bb19 ], [ bitcast (<{ [4 x i8] }>* @alloc461 to [0 x i8]*), %bb18 ], [ bitcast (<{ [3 x i8] }>* @alloc462 to [0 x i8]*), %bb17 ], [ bitcast (<{ [3 x i8] }>* @alloc463 to [0 x i8]*), %bb16 ], [ bitcast (<{ [5 x i8] }>* @alloc464 to [0 x i8]*), %bb15 ], [ bitcast (<{ [6 x i8] }>* @alloc465 to [0 x i8]*), %bb14 ], [ bitcast (<{ [6 x i8] }>* @alloc466 to [0 x i8]*), %bb13 ], [ bitcast (<{ [5 x i8] }>* @alloc467 to [0 x i8]*), %bb12 ], [ bitcast (<{ [4 x i8] }>* @alloc468 to [0 x i8]*), %bb11 ], [ bitcast (<{ [4 x i8] }>* @alloc469 to [0 x i8]*), %bb10 ], [ bitcast (<{ [3 x i8] }>* @alloc470 to [0 x i8]*), %bb9 ], [ bitcast (<{ [3 x i8] }>* @alloc471 to [0 x i8]*), %bb8 ], [ bitcast (<{ [3 x i8] }>* @alloc472 to [0 x i8]*), %bb7 ], [ bitcast (<{ [6 x i8] }>* @alloc473 to [0 x i8]*), %bb6 ], [ bitcast (<{ [6 x i8] }>* @alloc474 to [0 x i8]*), %bb5 ], [ bitcast (<{ [9 x i8] }>* @alloc475 to [0 x i8]*), %bb4 ], [ bitcast (<{ [3 x i8] }>* @alloc476 to [0 x i8]*), %start ]
  %.sroa.50.0 = phi i64 [ 3, %bb51 ], [ 3, %bb50 ], [ 10, %bb49 ], [ 6, %bb48 ], [ 6, %bb47 ], [ 8, %bb46 ], [ 5, %bb45 ], [ 4, %bb44 ], [ 6, %bb43 ], [ 3, %bb42 ], [ 5, %bb41 ], [ 4, %bb40 ], [ 4, %bb39 ], [ 3, %bb38 ], [ 4, %bb37 ], [ 18, %bb36 ], [ 10, %bb35 ], [ 12, %bb34 ], [ 10, %bb33 ], [ 16, %bb32 ], [ 10, %bb31 ], [ 10, %bb30 ], [ 11, %bb29 ], [ 15, %bb28 ], [ 10, %bb27 ], [ 10, %bb26 ], [ 8, %bb25 ], [ 8, %bb24 ], [ 8, %bb23 ], [ 8, %bb22 ], [ 8, %bb21 ], [ 8, %bb20 ], [ 7, %bb19 ], [ 4, %bb18 ], [ 3, %bb17 ], [ 3, %bb16 ], [ 5, %bb15 ], [ 6, %bb14 ], [ 6, %bb13 ], [ 5, %bb12 ], [ 4, %bb11 ], [ 4, %bb10 ], [ 3, %bb9 ], [ 3, %bb8 ], [ 3, %bb7 ], [ 6, %bb6 ], [ 6, %bb5 ], [ 9, %bb4 ], [ 3, %start ]
  %1 = insertvalue { [0 x i8]*, i64 } undef, [0 x i8]* %.sroa.0.0, 0
  %2 = insertvalue { [0 x i8]*, i64 } %1, i64 %.sroa.50.0, 1
  ret { [0 x i8]*, i64 } %2
}

; core::core_arch::x86::xsave::_xgetbv
; Function Attrs: inlinehint nounwind nonlazybind uwtable
define internal fastcc i64 @_ZN4core9core_arch3x865xsave7_xgetbv17h56f8691b4c7eb01fE() unnamed_addr #3 {
start:
  %_2 = tail call i64 @llvm.x86.xgetbv(i32 0) #5
  ret i64 %_2
}

; core::panicking::panic
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #4

; Function Attrs: nounwind
declare i64 @llvm.x86.xgetbv(i32) unnamed_addr #5

attributes #0 = { mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { cold nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #3 = { inlinehint nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" "target-features"="+xsave" }
attributes #4 = { cold noinline noreturn nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #5 = { nounwind }
attributes #6 = { noreturn }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
!2 = !{i32 3661390, i32 3661422, i32 3661443}
