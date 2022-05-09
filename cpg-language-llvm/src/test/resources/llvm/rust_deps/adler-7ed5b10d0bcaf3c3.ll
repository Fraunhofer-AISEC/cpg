; ModuleID = 'adler.90009da1-cgu.0'
source_filename = "adler.90009da1-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"algo::U32X4" = type { [4 x i32] }
%"core::panic::location::Location" = type { { [0 x i8]*, i64 }, i32, i32 }
%"core::fmt::Formatter" = type { { i64, i64 }, { i64, i64 }, { {}*, [3 x i64]* }, i32, i32, i8, [7 x i8] }
%"core::fmt::builders::DebugStruct" = type { %"core::fmt::Formatter"*, i8, i8, [6 x i8] }

@alloc40 = private unnamed_addr constant <{ [86 x i8] }> <{ [86 x i8] c"/home/alwagner/.cargo/registry/src/github.com-1ecc6299db9ec823/adler-0.2.3/src/algo.rs" }>, align 1
@alloc41 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [86 x i8] }>, <{ [86 x i8] }>* @alloc40, i32 0, i32 0, i32 0), [16 x i8] c"V\00\00\00\00\00\00\00\87\00\00\00\0D\00\00\00" }>, align 8
@str.0 = internal constant [57 x i8] c"attempt to calculate the remainder with a divisor of zero"
@alloc42 = private unnamed_addr constant <{ [7 x i8] }> <{ [7 x i8] c"Adler32" }>, align 1
@alloc43 = private unnamed_addr constant <{ [1 x i8] }> <{ [1 x i8] c"a" }>, align 1
@vtable.1 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i16**)* @"_ZN4core3ptr28drop_in_place$LT$$RF$u16$GT$17ha161ec706f812d55E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i16**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17ha0d5123a914676ecE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc47 = private unnamed_addr constant <{ [1 x i8] }> <{ [1 x i8] c"b" }>, align 1

; core::ptr::drop_in_place<&u16>
; Function Attrs: inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define internal void @"_ZN4core3ptr28drop_in_place$LT$$RF$u16$GT$17ha161ec706f812d55E"(i16** nocapture readnone %_1) unnamed_addr #0 {
start:
  ret void
}

; <adler::algo::U32X4 as core::ops::arith::AddAssign>::add_assign
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define void @"_ZN66_$LT$adler..algo..U32X4$u20$as$u20$core..ops..arith..AddAssign$GT$10add_assign17h1e1202ae4448edb0E"(%"algo::U32X4"* noalias align 4 dereferenceable(16) %self, i128 %0) unnamed_addr #1 personality i32 (...)* @rust_eh_personality {
start:
  %other.sroa.2.0.extract.shift = lshr i128 %0, 32
  %other.sroa.3.0.extract.shift = lshr i128 %0, 64
  %other.sroa.4.0.extract.shift = lshr i128 %0, 96
  %1 = insertelement <4 x i128> poison, i128 %0, i32 0
  %2 = insertelement <4 x i128> %1, i128 %other.sroa.2.0.extract.shift, i32 1
  %3 = insertelement <4 x i128> %2, i128 %other.sroa.3.0.extract.shift, i32 2
  %4 = insertelement <4 x i128> %3, i128 %other.sroa.4.0.extract.shift, i32 3
  %5 = trunc <4 x i128> %4 to <4 x i32>
  %6 = bitcast %"algo::U32X4"* %self to <4 x i32>*
  %7 = load <4 x i32>, <4 x i32>* %6, align 4, !alias.scope !2
  %8 = add <4 x i32> %7, %5
  %9 = bitcast %"algo::U32X4"* %self to <4 x i32>*
  store <4 x i32> %8, <4 x i32>* %9, align 4, !alias.scope !2
  ret void
}

; <adler::algo::U32X4 as core::ops::arith::RemAssign<u32>>::rem_assign
; Function Attrs: nonlazybind uwtable
define void @"_ZN77_$LT$adler..algo..U32X4$u20$as$u20$core..ops..arith..RemAssign$LT$u32$GT$$GT$10rem_assign17h9ce7056e067e3f31E"(%"algo::U32X4"* noalias align 4 dereferenceable(16) %self, i32 %quotient) unnamed_addr #2 {
start:
  %0 = getelementptr inbounds %"algo::U32X4", %"algo::U32X4"* %self, i64 0, i32 0, i64 0
  %_14 = icmp eq i32 %quotient, 0
  br i1 %_14, label %panic, label %"_ZN94_$LT$core..slice..iter..IterMut$LT$T$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hf9dd2e9b55e34852E.exit.preheader", !prof !7

"_ZN94_$LT$core..slice..iter..IterMut$LT$T$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hf9dd2e9b55e34852E.exit.preheader": ; preds = %start
  %1 = getelementptr inbounds %"algo::U32X4", %"algo::U32X4"* %self, i64 0, i32 0, i64 1
  %2 = load i32, i32* %0, align 4
  %3 = urem i32 %2, %quotient
  store i32 %3, i32* %0, align 4
  %4 = getelementptr inbounds %"algo::U32X4", %"algo::U32X4"* %self, i64 0, i32 0, i64 2
  %5 = load i32, i32* %1, align 4
  %6 = urem i32 %5, %quotient
  store i32 %6, i32* %1, align 4
  %7 = getelementptr inbounds %"algo::U32X4", %"algo::U32X4"* %self, i64 0, i32 0, i64 3
  %8 = load i32, i32* %4, align 4
  %9 = urem i32 %8, %quotient
  store i32 %9, i32* %4, align 4
  %10 = load i32, i32* %7, align 4
  %11 = urem i32 %10, %quotient
  store i32 %11, i32* %7, align 4
  ret void

panic:                                            ; preds = %start
; call core::panicking::panic
  tail call void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast ([57 x i8]* @str.0 to [0 x i8]*), i64 57, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc41 to %"core::panic::location::Location"*)) #8
  unreachable
}

; <adler::algo::U32X4 as core::ops::arith::MulAssign<u32>>::mul_assign
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define void @"_ZN77_$LT$adler..algo..U32X4$u20$as$u20$core..ops..arith..MulAssign$LT$u32$GT$$GT$10mul_assign17h855b6e7409f4d897E"(%"algo::U32X4"* noalias align 4 dereferenceable(16) %self, i32 %rhs) unnamed_addr #1 {
start:
  %0 = bitcast %"algo::U32X4"* %self to <4 x i32>*
  %1 = load <4 x i32>, <4 x i32>* %0, align 4
  %2 = insertelement <4 x i32> poison, i32 %rhs, i32 0
  %3 = shufflevector <4 x i32> %2, <4 x i32> poison, <4 x i32> zeroinitializer
  %4 = mul <4 x i32> %1, %3
  %5 = bitcast %"algo::U32X4"* %self to <4 x i32>*
  store <4 x i32> %4, <4 x i32>* %5, align 4
  ret void
}

; adler::Adler32::write_slice
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define void @_ZN5adler7Adler3211write_slice17h7b976c2d5d26149cE({ i16, i16 }* noalias nocapture align 2 dereferenceable(4) %self, [0 x i8]* noalias nonnull readonly align 1 %bytes.0, i64 %bytes.1) unnamed_addr #1 personality i32 (...)* @rust_eh_personality {
start:
  tail call void @llvm.experimental.noalias.scope.decl(metadata !8)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !11)
  %0 = getelementptr inbounds { i16, i16 }, { i16, i16 }* %self, i64 0, i32 0
  %_4.i = load i16, i16* %0, align 2, !alias.scope !8, !noalias !11
  %1 = zext i16 %_4.i to i32
  %2 = getelementptr inbounds { i16, i16 }, { i16, i16 }* %self, i64 0, i32 1
  %_6.i = load i16, i16* %2, align 2, !alias.scope !8, !noalias !11
  %3 = zext i16 %_6.i to i32
  %_14.i = and i64 %bytes.1, -4
  %4 = getelementptr inbounds [0 x i8], [0 x i8]* %bytes.0, i64 0, i64 %_14.i
  %_7.i.i.i.i.i.i = and i64 %bytes.1, 3
  %rem.i.i.i = urem i64 %_14.i, 22208
  %fst_len.i.i.i = sub i64 %_14.i, %rem.i.i.i
  %5 = getelementptr inbounds [0 x i8], [0 x i8]* %bytes.0, i64 0, i64 %fst_len.i.i.i
  %_49.i = mul nuw nsw i32 %1, 22208
  %_2.i291.i = icmp ult i64 %fst_len.i.i.i, 22208
  br i1 %_2.i291.i, label %bb11.i, label %"_ZN98_$LT$core..slice..iter..ChunksExact$LT$T$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h7c676405b838c054E.exit.i"

"_ZN98_$LT$core..slice..iter..ChunksExact$LT$T$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h7c676405b838c054E.exit.i": ; preds = %start, %bb18.i
  %b.0298.i = phi i32 [ %28, %bb18.i ], [ %3, %start ]
  %iter.sroa.5.0297.i = phi i64 [ %_7.i.i.i.i.i.i.i, %bb18.i ], [ %fst_len.i.i.i, %start ]
  %iter.sroa.0.0296.i = phi [0 x i8]* [ %24, %bb18.i ], [ %bytes.0, %start ]
  %6 = phi <4 x i32> [ %26, %bb18.i ], [ zeroinitializer, %start ]
  %7 = phi <4 x i32> [ %27, %bb18.i ], [ zeroinitializer, %start ]
  br label %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit.i

bb11.i:                                           ; preds = %bb18.i, %start
  %b.0.lcssa.i = phi i32 [ %3, %start ], [ %28, %bb18.i ]
  %8 = phi <4 x i32> [ zeroinitializer, %start ], [ %27, %bb18.i ]
  %9 = phi <4 x i32> [ zeroinitializer, %start ], [ %26, %bb18.i ]
  %fst_len.i.i75.i = and i64 %rem.i.i.i, 32764
  %_2.i98311.i = icmp eq i64 %fst_len.i.i75.i, 0
  %10 = shufflevector <4 x i32> %9, <4 x i32> undef, <2 x i32> <i32 2, i32 1>
  %11 = shufflevector <4 x i32> %9, <4 x i32> undef, <2 x i32> <i32 3, i32 0>
  br i1 %_2.i98311.i, label %bb30.i, label %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader

_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader: ; preds = %bb11.i
  %12 = add nsw i64 %fst_len.i.i75.i, -4
  %13 = and i64 %12, 4
  %lcmp.mod.not.not = icmp eq i64 %13, 0
  br i1 %lcmp.mod.not.not, label %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol, label %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit

_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol: ; preds = %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader
  %_7.i.i.i.i.i.i100.i.prol = add nsw i64 %fst_len.i.i75.i, -4
  %14 = getelementptr inbounds i8, i8* %5, i64 4
  %15 = bitcast i8* %5 to <4 x i8>*
  %16 = load <4 x i8>, <4 x i8>* %15, align 1, !alias.scope !13, !noalias !8
  %17 = zext <4 x i8> %16 to <4 x i32>
  %18 = add nuw nsw <4 x i32> %9, %17
  %19 = add nuw nsw <4 x i32> %18, %8
  %20 = shufflevector <4 x i32> %18, <4 x i32> undef, <2 x i32> <i32 2, i32 1>
  %21 = shufflevector <4 x i32> %18, <4 x i32> undef, <2 x i32> <i32 3, i32 0>
  br label %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit

_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit: ; preds = %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader
  %.lcssa46.unr = phi <4 x i32> [ undef, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader ], [ %19, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol ]
  %.lcssa45.unr = phi <2 x i32> [ undef, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader ], [ %20, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol ]
  %.lcssa44.unr = phi <2 x i32> [ undef, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader ], [ %21, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol ]
  %iter2.sroa.5.0317.i.unr = phi i64 [ %fst_len.i.i75.i, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader ], [ %_7.i.i.i.i.i.i100.i.prol, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol ]
  %iter2.sroa.0.0.in316.i.unr = phi i8* [ %5, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader ], [ %14, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol ]
  %.unr = phi <4 x i32> [ %9, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader ], [ %18, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol ]
  %.unr52 = phi <4 x i32> [ %8, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.preheader ], [ %19, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol ]
  %22 = icmp eq i64 %12, 0
  br i1 %22, label %bb30.i, label %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i

bb18.i:                                           ; preds = %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit.i
  %_7.i.i.i.i.i.i.i = add i64 %iter.sroa.5.0297.i, -22208
  %23 = getelementptr inbounds [0 x i8], [0 x i8]* %iter.sroa.0.0296.i, i64 0, i64 22208
  %24 = bitcast i8* %23 to [0 x i8]*
  %25 = add nuw i32 %b.0298.i, %_49.i
  %26 = urem <4 x i32> %54, <i32 65521, i32 65521, i32 65521, i32 65521>
  %27 = urem <4 x i32> %55, <i32 65521, i32 65521, i32 65521, i32 65521>
  %28 = urem i32 %25, 65521
  %_2.i.i = icmp ult i64 %_7.i.i.i.i.i.i.i, 22208
  br i1 %_2.i.i, label %bb11.i, label %"_ZN98_$LT$core..slice..iter..ChunksExact$LT$T$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h7c676405b838c054E.exit.i"

_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit.i: ; preds = %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit.i, %"_ZN98_$LT$core..slice..iter..ChunksExact$LT$T$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h7c676405b838c054E.exit.i"
  %iter1.sroa.5.0283.i = phi i64 [ 22208, %"_ZN98_$LT$core..slice..iter..ChunksExact$LT$T$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h7c676405b838c054E.exit.i" ], [ %_7.i.i.i.i.i.i82.i.3, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit.i ]
  %iter1.sroa.0.0282.i = phi [0 x i8]* [ %iter.sroa.0.0296.i, %"_ZN98_$LT$core..slice..iter..ChunksExact$LT$T$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h7c676405b838c054E.exit.i" ], [ %50, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit.i ]
  %29 = phi <4 x i32> [ %6, %"_ZN98_$LT$core..slice..iter..ChunksExact$LT$T$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h7c676405b838c054E.exit.i" ], [ %54, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit.i ]
  %30 = phi <4 x i32> [ %7, %"_ZN98_$LT$core..slice..iter..ChunksExact$LT$T$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h7c676405b838c054E.exit.i" ], [ %55, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit.i ]
  %31 = getelementptr inbounds [0 x i8], [0 x i8]* %iter1.sroa.0.0282.i, i64 0, i64 4
  %32 = bitcast [0 x i8]* %iter1.sroa.0.0282.i to <4 x i8>*
  %33 = load <4 x i8>, <4 x i8>* %32, align 1, !alias.scope !16, !noalias !8
  %34 = zext <4 x i8> %33 to <4 x i32>
  %35 = add <4 x i32> %29, %34
  %36 = add <4 x i32> %35, %30
  %37 = getelementptr inbounds [0 x i8], [0 x i8]* %iter1.sroa.0.0282.i, i64 0, i64 8
  %38 = bitcast i8* %31 to <4 x i8>*
  %39 = load <4 x i8>, <4 x i8>* %38, align 1, !alias.scope !16, !noalias !8
  %40 = zext <4 x i8> %39 to <4 x i32>
  %41 = add <4 x i32> %35, %40
  %42 = add <4 x i32> %41, %36
  %43 = getelementptr inbounds [0 x i8], [0 x i8]* %iter1.sroa.0.0282.i, i64 0, i64 12
  %44 = bitcast i8* %37 to <4 x i8>*
  %45 = load <4 x i8>, <4 x i8>* %44, align 1, !alias.scope !16, !noalias !8
  %46 = zext <4 x i8> %45 to <4 x i32>
  %47 = add <4 x i32> %41, %46
  %48 = add <4 x i32> %47, %42
  %49 = getelementptr inbounds [0 x i8], [0 x i8]* %iter1.sroa.0.0282.i, i64 0, i64 16
  %50 = bitcast i8* %49 to [0 x i8]*
  %_7.i.i.i.i.i.i82.i.3 = add nsw i64 %iter1.sroa.5.0283.i, -16
  %51 = bitcast i8* %43 to <4 x i8>*
  %52 = load <4 x i8>, <4 x i8>* %51, align 1, !alias.scope !16, !noalias !8
  %53 = zext <4 x i8> %52 to <4 x i32>
  %54 = add <4 x i32> %47, %53
  %55 = add <4 x i32> %54, %48
  %_2.i80.i.3 = icmp eq i64 %_7.i.i.i.i.i.i82.i.3, 0
  br i1 %_2.i80.i.3, label %bb18.i, label %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit.i

bb30.i.loopexit.unr-lcssa:                        ; preds = %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i
  %56 = shufflevector <4 x i32> %92, <4 x i32> undef, <2 x i32> <i32 3, i32 0>
  %57 = shufflevector <4 x i32> %92, <4 x i32> undef, <2 x i32> <i32 2, i32 1>
  br label %bb30.i

bb30.i:                                           ; preds = %bb30.i.loopexit.unr-lcssa, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit, %bb11.i
  %58 = phi <4 x i32> [ %8, %bb11.i ], [ %.lcssa46.unr, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit ], [ %93, %bb30.i.loopexit.unr-lcssa ]
  %59 = phi <2 x i32> [ %10, %bb11.i ], [ %.lcssa45.unr, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit ], [ %57, %bb30.i.loopexit.unr-lcssa ]
  %60 = phi <2 x i32> [ %11, %bb11.i ], [ %.lcssa44.unr, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit ], [ %56, %bb30.i.loopexit.unr-lcssa ]
  %_74.i = trunc i64 %rem.i.i.i to i32
  %_73.i = mul nuw nsw i32 %1, %_74.i
  %61 = extractelement <2 x i32> %60, i32 1
  %62 = urem i32 %61, 65521
  %63 = urem <2 x i32> %59, <i32 65521, i32 65521>
  %64 = extractelement <2 x i32> %60, i32 0
  %65 = urem i32 %64, 65521
  %66 = urem <4 x i32> %58, <i32 65521, i32 65521, i32 65521, i32 65521>
  %67 = extractelement <2 x i32> %63, i32 0
  %_89.neg.i = mul nsw i32 %67, -2
  %_94.i = sub nuw nsw i32 65521, %65
  %_93.i = mul nuw nsw i32 %_94.i, 3
  %68 = add nuw i32 %b.0.lcssa.i, %_73.i
  %69 = urem i32 %68, 65521
  %70 = call i32 @llvm.vector.reduce.add.v4i32(<4 x i32> %66)
  %reass.mul = shl nuw nsw i32 %70, 2
  %_88.i = add nuw nsw i32 %69, 196563
  %71 = add nuw nsw i32 %_88.i, %_93.i
  %72 = add nsw i32 %71, %_89.neg.i
  %73 = extractelement <2 x i32> %63, i32 1
  %74 = sub nuw nsw i32 %72, %73
  %75 = add nuw nsw i32 %74, %reass.mul
  %76 = add nuw nsw i32 %65, %1
  %77 = add nuw nsw i32 %76, %67
  %78 = add nuw nsw i32 %77, %73
  %79 = add nuw nsw i32 %78, %62
  %_12.i159326.i = icmp eq i64 %_7.i.i.i.i.i.i, 0
  br i1 %_12.i159326.i, label %"_ZN5adler4algo32_$LT$impl$u20$adler..Adler32$GT$7compute17had10b73f3f629de8E.exit", label %bb55.i

_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i: ; preds = %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i
  %iter2.sroa.5.0317.i = phi i64 [ %_7.i.i.i.i.i.i100.i.1, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i ], [ %iter2.sroa.5.0317.i.unr, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit ]
  %iter2.sroa.0.0.in316.i = phi i8* [ %88, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i ], [ %iter2.sroa.0.0.in316.i.unr, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit ]
  %80 = phi <4 x i32> [ %92, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i ], [ %.unr, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit ]
  %81 = phi <4 x i32> [ %93, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i ], [ %.unr52, %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i.prol.loopexit ]
  %82 = getelementptr inbounds i8, i8* %iter2.sroa.0.0.in316.i, i64 4
  %83 = bitcast i8* %iter2.sroa.0.0.in316.i to <4 x i8>*
  %84 = load <4 x i8>, <4 x i8>* %83, align 1, !alias.scope !13, !noalias !8
  %85 = zext <4 x i8> %84 to <4 x i32>
  %86 = add <4 x i32> %80, %85
  %87 = add <4 x i32> %86, %81
  %_7.i.i.i.i.i.i100.i.1 = add i64 %iter2.sroa.5.0317.i, -8
  %88 = getelementptr inbounds i8, i8* %iter2.sroa.0.0.in316.i, i64 8
  %89 = bitcast i8* %82 to <4 x i8>*
  %90 = load <4 x i8>, <4 x i8>* %89, align 1, !alias.scope !13, !noalias !8
  %91 = zext <4 x i8> %90 to <4 x i32>
  %92 = add <4 x i32> %86, %91
  %93 = add <4 x i32> %92, %87
  %_2.i98.i.1 = icmp eq i64 %_7.i.i.i.i.i.i100.i.1, 0
  br i1 %_2.i98.i.1, label %bb30.i.loopexit.unr-lcssa, label %_ZN5adler4algo5U32X44from17ha6c97c141b421b05E.exit129.i

bb55.i:                                           ; preds = %bb30.i
  %byte.i = load i8, i8* %4, align 1, !alias.scope !11, !noalias !8
  %94 = zext i8 %byte.i to i32
  %95 = add nuw nsw i32 %79, %94
  %96 = add i32 %95, %75
  %_12.i159.i = icmp eq i64 %_7.i.i.i.i.i.i, 1
  br i1 %_12.i159.i, label %"_ZN5adler4algo32_$LT$impl$u20$adler..Adler32$GT$7compute17had10b73f3f629de8E.exit", label %bb55.i.1

"_ZN5adler4algo32_$LT$impl$u20$adler..Adler32$GT$7compute17had10b73f3f629de8E.exit": ; preds = %bb55.i, %bb55.i.1, %bb55.i.2, %bb30.i
  %b.2.lcssa.i = phi i32 [ %75, %bb30.i ], [ %96, %bb55.i ], [ %102, %bb55.i.1 ], [ %106, %bb55.i.2 ]
  %a.1.lcssa.i = phi i32 [ %79, %bb30.i ], [ %95, %bb55.i ], [ %101, %bb55.i.1 ], [ %105, %bb55.i.2 ]
  %_132.i = urem i32 %a.1.lcssa.i, 65521
  %97 = trunc i32 %_132.i to i16
  store i16 %97, i16* %0, align 2, !alias.scope !8, !noalias !11
  %_134.i = urem i32 %b.2.lcssa.i, 65521
  %98 = trunc i32 %_134.i to i16
  store i16 %98, i16* %2, align 2, !alias.scope !8, !noalias !11
  ret void

bb55.i.1:                                         ; preds = %bb55.i
  %99 = getelementptr inbounds i8, i8* %4, i64 1
  %byte.i.1 = load i8, i8* %99, align 1, !alias.scope !11, !noalias !8
  %100 = zext i8 %byte.i.1 to i32
  %101 = add nuw nsw i32 %95, %100
  %102 = add i32 %101, %96
  %_12.i159.i.1 = icmp eq i64 %_7.i.i.i.i.i.i, 2
  br i1 %_12.i159.i.1, label %"_ZN5adler4algo32_$LT$impl$u20$adler..Adler32$GT$7compute17had10b73f3f629de8E.exit", label %bb55.i.2

bb55.i.2:                                         ; preds = %bb55.i.1
  %103 = getelementptr inbounds i8, i8* %4, i64 2
  %byte.i.2 = load i8, i8* %103, align 1, !alias.scope !11, !noalias !8
  %104 = zext i8 %byte.i.2 to i32
  %105 = add i32 %101, %104
  %106 = add i32 %105, %102
  br label %"_ZN5adler4algo32_$LT$impl$u20$adler..Adler32$GT$7compute17had10b73f3f629de8E.exit"
}

; <adler::Adler32 as core::hash::Hasher>::write
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define void @"_ZN53_$LT$adler..Adler32$u20$as$u20$core..hash..Hasher$GT$5write17h8c4ccb5e73a68568E"({ i16, i16 }* noalias nocapture align 2 dereferenceable(4) %self, [0 x i8]* noalias nonnull readonly align 1 %bytes.0, i64 %bytes.1) unnamed_addr #1 {
start:
; call adler::Adler32::write_slice
  tail call void @_ZN5adler7Adler3211write_slice17h7b976c2d5d26149cE({ i16, i16 }* noalias nonnull align 2 dereferenceable(4) %self, [0 x i8]* noalias nonnull readonly align 1 %bytes.0, i64 %bytes.1)
  ret void
}

; adler::adler32_slice
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define i32 @_ZN5adler13adler32_slice17h3b7592d23a964ac7E([0 x i8]* noalias nonnull readonly align 1 %data.0, i64 %data.1) unnamed_addr #1 {
start:
  %h = alloca { i16, i16 }, align 2
  %0 = bitcast { i16, i16 }* %h to i8*
  call void @llvm.lifetime.start.p0i8(i64 4, i8* nonnull %0)
  %.fca.0.gep = getelementptr inbounds { i16, i16 }, { i16, i16 }* %h, i64 0, i32 0
  store i16 1, i16* %.fca.0.gep, align 2
  %.fca.1.gep = getelementptr inbounds { i16, i16 }, { i16, i16 }* %h, i64 0, i32 1
  store i16 0, i16* %.fca.1.gep, align 2
; call adler::Adler32::write_slice
  call void @_ZN5adler7Adler3211write_slice17h7b976c2d5d26149cE({ i16, i16 }* noalias nonnull align 2 dereferenceable(4) %h, [0 x i8]* noalias nonnull readonly align 1 %data.0, i64 %data.1)
  %h.idx.val = load i16, i16* %.fca.0.gep, align 2
  %h.idx1.val = load i16, i16* %.fca.1.gep, align 2
  %1 = zext i16 %h.idx1.val to i32
  %_2.i = shl nuw i32 %1, 16
  %2 = zext i16 %h.idx.val to i32
  %3 = or i32 %_2.i, %2
  call void @llvm.lifetime.end.p0i8(i64 4, i8* nonnull %0)
  ret i32 %3
}

; <adler::Adler32 as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN51_$LT$adler..Adler32$u20$as$u20$core..fmt..Debug$GT$3fmt17h2032c4d4fcbc8d3dE"({ i16, i16 }* noalias readonly align 2 dereferenceable(4) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #2 {
start:
  %_25 = alloca i16*, align 8
  %_17 = alloca i16*, align 8
  %_6 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds { i16, i16 }, { i16, i16 }* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds { i16, i16 }, { i16, i16 }* %self, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc42 to [0 x i8]*), i64 7)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast i16** %_17 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store i16* %__self_0_0, i16** %_17, align 8
  %_14.0 = bitcast i16** %_17 to {}*
; call core::fmt::builders::DebugStruct::field
  %_10 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [1 x i8] }>* @alloc43 to [0 x i8]*), i64 1, {}* nonnull align 1 %_14.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.1 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast i16** %_25 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store i16* %__self_0_1, i16** %_25, align 8
  %_22.0 = bitcast i16** %_25 to {}*
; call core::fmt::builders::DebugStruct::field
  %_18 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [1 x i8] }>* @alloc47 to [0 x i8]*), i64 1, {}* nonnull align 1 %_22.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.1 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %4
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17ha0d5123a914676ecE"(i16** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #2 {
start:
  %_4 = load i16*, i16** %self, align 8, !nonnull !19
; call core::fmt::Formatter::debug_lower_hex
  %_3.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !20
  br i1 %_3.i, label %bb2.i, label %bb4.i

bb4.i:                                            ; preds = %start
; call core::fmt::Formatter::debug_upper_hex
  %_7.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !20
  br i1 %_7.i, label %bb6.i, label %bb8.i

bb2.i:                                            ; preds = %start
; call core::fmt::num::<impl core::fmt::LowerHex for u16>::fmt
  %0 = tail call zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$u16$GT$3fmt17ha2082e812641fd21E"(i16* noalias nonnull readonly align 2 dereferenceable(2) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u16$GT$3fmt17h019c7e92cf1ab240E.exit"

bb8.i:                                            ; preds = %bb4.i
; call core::fmt::num::imp::<impl core::fmt::Display for u16>::fmt
  %1 = tail call zeroext i1 @"_ZN4core3fmt3num3imp52_$LT$impl$u20$core..fmt..Display$u20$for$u20$u16$GT$3fmt17hb434ec8c69e3a05cE"(i16* noalias nonnull readonly align 2 dereferenceable(2) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u16$GT$3fmt17h019c7e92cf1ab240E.exit"

bb6.i:                                            ; preds = %bb4.i
; call core::fmt::num::<impl core::fmt::UpperHex for u16>::fmt
  %2 = tail call zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$u16$GT$3fmt17h19c0f71ea07cc8f9E"(i16* noalias nonnull readonly align 2 dereferenceable(2) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u16$GT$3fmt17h019c7e92cf1ab240E.exit"

"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u16$GT$3fmt17h019c7e92cf1ab240E.exit": ; preds = %bb2.i, %bb8.i, %bb6.i
  %.0.in.i = phi i1 [ %0, %bb2.i ], [ %2, %bb6.i ], [ %1, %bb8.i ]
  ret i1 %.0.in.i
}

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.start.p0i8(i64 immarg, i8* nocapture) #3

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.end.p0i8(i64 immarg, i8* nocapture) #3

; core::panicking::panic
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #4

; core::fmt::Formatter::debug_struct
; Function Attrs: nonlazybind uwtable
declare i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #2

; core::fmt::builders::DebugStruct::field
; Function Attrs: nonlazybind uwtable
declare align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias align 8 dereferenceable(16), [0 x i8]* noalias nonnull readonly align 1, i64, {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24)) unnamed_addr #2

; core::fmt::builders::DebugStruct::finish
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias align 8 dereferenceable(16)) unnamed_addr #2

; Function Attrs: nonlazybind
declare i32 @rust_eh_personality(...) unnamed_addr #5

; core::fmt::Formatter::debug_lower_hex
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias readonly align 8 dereferenceable(64)) unnamed_addr #2

; core::fmt::num::<impl core::fmt::LowerHex for u16>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$u16$GT$3fmt17ha2082e812641fd21E"(i16* noalias readonly align 2 dereferenceable(2), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #2

; core::fmt::Formatter::debug_upper_hex
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias readonly align 8 dereferenceable(64)) unnamed_addr #2

; core::fmt::num::<impl core::fmt::UpperHex for u16>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$u16$GT$3fmt17h19c0f71ea07cc8f9E"(i16* noalias readonly align 2 dereferenceable(2), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #2

; core::fmt::num::imp::<impl core::fmt::Display for u16>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num3imp52_$LT$impl$u20$core..fmt..Display$u20$for$u20$u16$GT$3fmt17hb434ec8c69e3a05cE"(i16* noalias readonly align 2 dereferenceable(2), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #2

; Function Attrs: inaccessiblememonly nofree nosync nounwind willreturn
declare void @llvm.experimental.noalias.scope.decl(metadata) #6

; Function Attrs: nofree nosync nounwind readnone willreturn
declare i32 @llvm.vector.reduce.add.v4i32(<4 x i32>) #7

attributes #0 = { inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { nofree nosync nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #3 = { argmemonly mustprogress nofree nosync nounwind willreturn }
attributes #4 = { cold noinline noreturn nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #5 = { nonlazybind "target-cpu"="x86-64" }
attributes #6 = { inaccessiblememonly nofree nosync nounwind willreturn }
attributes #7 = { nofree nosync nounwind readnone willreturn }
attributes #8 = { noreturn }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
!2 = !{!3, !5}
!3 = distinct !{!3, !4, !"_ZN51_$LT$u32$u20$as$u20$core..ops..arith..AddAssign$GT$10add_assign17h1d02010959433110E: %self"}
!4 = distinct !{!4, !"_ZN51_$LT$u32$u20$as$u20$core..ops..arith..AddAssign$GT$10add_assign17h1d02010959433110E"}
!5 = distinct !{!5, !6, !"_ZN66_$LT$u32$u20$as$u20$core..ops..arith..AddAssign$LT$$RF$u32$GT$$GT$10add_assign17h51e2ed2c205b8ba8E: %self"}
!6 = distinct !{!6, !"_ZN66_$LT$u32$u20$as$u20$core..ops..arith..AddAssign$LT$$RF$u32$GT$$GT$10add_assign17h51e2ed2c205b8ba8E"}
!7 = !{!"branch_weights", i32 1, i32 2000}
!8 = !{!9}
!9 = distinct !{!9, !10, !"_ZN5adler4algo32_$LT$impl$u20$adler..Adler32$GT$7compute17had10b73f3f629de8E: %self"}
!10 = distinct !{!10, !"_ZN5adler4algo32_$LT$impl$u20$adler..Adler32$GT$7compute17had10b73f3f629de8E"}
!11 = !{!12}
!12 = distinct !{!12, !10, !"_ZN5adler4algo32_$LT$impl$u20$adler..Adler32$GT$7compute17had10b73f3f629de8E: %bytes.0"}
!13 = !{!14, !12}
!14 = distinct !{!14, !15, !"_ZN5adler4algo5U32X44from17ha6c97c141b421b05E: %bytes.0"}
!15 = distinct !{!15, !"_ZN5adler4algo5U32X44from17ha6c97c141b421b05E"}
!16 = !{!17, !12}
!17 = distinct !{!17, !18, !"_ZN5adler4algo5U32X44from17ha6c97c141b421b05E: %bytes.0"}
!18 = distinct !{!18, !"_ZN5adler4algo5U32X44from17ha6c97c141b421b05E"}
!19 = !{}
!20 = !{!21}
!21 = distinct !{!21, !22, !"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u16$GT$3fmt17h019c7e92cf1ab240E: %self"}
!22 = distinct !{!22, !"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u16$GT$3fmt17h019c7e92cf1ab240E"}
