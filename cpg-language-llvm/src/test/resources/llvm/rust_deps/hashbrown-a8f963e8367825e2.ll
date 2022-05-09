; ModuleID = 'hashbrown.6e345ebb-cgu.0'
source_filename = "hashbrown.6e345ebb-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"core::panic::location::Location" = type { { [0 x i8]*, i64 }, i32, i32 }
%"core::fmt::Formatter" = type { { i64, i64 }, { i64, i64 }, { {}*, [3 x i64]* }, i32, i32, i8, [7 x i8] }
%"core::fmt::builders::DebugStruct" = type { %"core::fmt::Formatter"*, i8, i8, [6 x i8] }
%"core::fmt::builders::DebugTuple" = type { %"core::fmt::Formatter"*, i64, i8, i8, [6 x i8] }

@alloc727 = private unnamed_addr constant <{ [28 x i8] }> <{ [28 x i8] c"Hash table capacity overflow" }>, align 1
@alloc728 = private unnamed_addr constant <{ [94 x i8] }> <{ [94 x i8] c"/home/alwagner/.cargo/registry/src/github.com-1ecc6299db9ec823/hashbrown-0.11.0/src/raw/mod.rs" }>, align 1
@alloc729 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [94 x i8] }>, <{ [94 x i8] }>* @alloc728, i32 0, i32 0, i32 0), [16 x i8] c"^\00\00\00\00\00\00\00c\00\00\00(\00\00\00" }>, align 8
@alloc712 = private unnamed_addr constant <{ [16 x i8] }> <{ [16 x i8] c"\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF" }>, align 16
@alloc730 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"AllocError" }>, align 1
@alloc731 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"layout" }>, align 1
@vtable.0 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i64**)* @"_ZN4core3ptr30drop_in_place$LT$$RF$usize$GT$17h5dafcf2185217a02E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i64, i64 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17he58e747b14e5c3beE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc735 = private unnamed_addr constant <{ [16 x i8] }> <{ [16 x i8] c"CapacityOverflow" }>, align 1
@alloc736 = private unnamed_addr constant <{ [9 x i8] }> <{ [9 x i8] c"Duplicate" }>, align 1
@vtable.1 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i64**)* @"_ZN4core3ptr30drop_in_place$LT$$RF$usize$GT$17h5dafcf2185217a02E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i64**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hd2a9974a7383c59fE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc740 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"Absent" }>, align 1

; core::ptr::drop_in_place<&usize>
; Function Attrs: inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define internal void @"_ZN4core3ptr30drop_in_place$LT$$RF$usize$GT$17h5dafcf2185217a02E"(i64** nocapture readnone %_1) unnamed_addr #0 {
start:
  ret void
}

; hashbrown::raw::Fallibility::capacity_overflow
; Function Attrs: nonlazybind uwtable
define { i64, i64 } @_ZN9hashbrown3raw11Fallibility17capacity_overflow17hcc166e88f22df8b2E(i1 zeroext %0) unnamed_addr #1 {
start:
  br i1 %0, label %bb1, label %bb3

bb3:                                              ; preds = %start
  ret { i64, i64 } { i64 undef, i64 0 }

bb1:                                              ; preds = %start
; call core::panicking::panic
  tail call void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [28 x i8] }>* @alloc727 to [0 x i8]*), i64 28, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc729 to %"core::panic::location::Location"*)) #7
  unreachable
}

; hashbrown::raw::Fallibility::alloc_err
; Function Attrs: nounwind nonlazybind uwtable
define { i64, i64 } @_ZN9hashbrown3raw11Fallibility9alloc_err17h962ee636b123f164E(i1 zeroext %0, i64 %layout.0, i64 %layout.1) unnamed_addr #2 {
start:
  br i1 %0, label %bb1, label %bb3

bb3:                                              ; preds = %start
  %1 = insertvalue { i64, i64 } undef, i64 %layout.0, 0
  %2 = insertvalue { i64, i64 } %1, i64 %layout.1, 1
  ret { i64, i64 } %2

bb1:                                              ; preds = %start
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %layout.0, i64 %layout.1) #8
  unreachable
}

; hashbrown::raw::sse2::Group::static_empty
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define align 1 dereferenceable(16) [16 x i8]* @_ZN9hashbrown3raw4sse25Group12static_empty17h999f9c7ae02108b8E() unnamed_addr #3 {
start:
  ret [16 x i8]* getelementptr inbounds (<{ [16 x i8] }>, <{ [16 x i8] }>* @alloc712, i64 0, i32 0)
}

; <hashbrown::TryReserveError as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN63_$LT$hashbrown..TryReserveError$u20$as$u20$core..fmt..Debug$GT$3fmt17hdadf77725f9665cdE"({ i64, i64 }* noalias readonly align 8 dereferenceable(16) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #1 {
start:
  %_22 = alloca { i64, i64 }*, align 8
  %_11 = alloca %"core::fmt::builders::DebugStruct", align 8
  %0 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %self, i64 0, i32 1
  %1 = load i64, i64* %0, align 8
  %2 = icmp eq i64 %1, 0
  br i1 %2, label %bb3, label %bb1

bb3:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %3 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [16 x i8] }>* @alloc735 to [0 x i8]*), i64 16)
  br label %bb8

bb1:                                              ; preds = %start
  %4 = bitcast %"core::fmt::builders::DebugStruct"* %_11 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %4)
; call core::fmt::Formatter::debug_struct
  %5 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [10 x i8] }>* @alloc730 to [0 x i8]*), i64 10)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_11 to i128*
  store i128 %5, i128* %.0..sroa_cast, align 8
  %6 = bitcast { i64, i64 }** %_22 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %6)
  store { i64, i64 }* %self, { i64, i64 }** %_22, align 8
  %_19.0 = bitcast { i64, i64 }** %_22 to {}*
; call core::fmt::builders::DebugStruct::field
  %_15 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_11, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc731 to [0 x i8]*), i64 6, {}* nonnull align 1 %_19.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.0 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %6)
; call core::fmt::builders::DebugStruct::finish
  %7 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_11)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %4)
  br label %bb8

bb8:                                              ; preds = %bb3, %bb1
  %.0.in = phi i1 [ %3, %bb3 ], [ %7, %bb1 ]
  ret i1 %.0.in
}

; <hashbrown::UnavailableMutError as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN67_$LT$hashbrown..UnavailableMutError$u20$as$u20$core..fmt..Debug$GT$3fmt17ha29dd47779c6e421E"({ i64, i64 }* noalias readonly align 8 dereferenceable(16) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #1 {
start:
  %_20 = alloca i64*, align 8
  %_11 = alloca %"core::fmt::builders::DebugTuple", align 8
  %0 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %self, i64 0, i32 0
  %_5 = load i64, i64* %0, align 8, !range !2
  %switch.not = icmp eq i64 %_5, 1
  br i1 %switch.not, label %bb1, label %bb3

bb3:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %1 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc740 to [0 x i8]*), i64 6)
  br label %bb8

bb1:                                              ; preds = %start
  %2 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %self, i64 0, i32 1
  %3 = bitcast %"core::fmt::builders::DebugTuple"* %_11 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %3)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_11, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [9 x i8] }>* @alloc736 to [0 x i8]*), i64 9)
  %4 = bitcast i64** %_20 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4)
  store i64* %2, i64** %_20, align 8
  %_17.0 = bitcast i64** %_20 to {}*
; call core::fmt::builders::DebugTuple::field
  %_15 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_11, {}* nonnull align 1 %_17.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.1 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4)
; call core::fmt::builders::DebugTuple::finish
  %5 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_11)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %3)
  br label %bb8

bb8:                                              ; preds = %bb3, %bb1
  %.0.in = phi i1 [ %1, %bb3 ], [ %5, %bb1 ]
  ret i1 %.0.in
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hd2a9974a7383c59fE"(i64** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #1 {
start:
  %_4 = load i64*, i64** %self, align 8, !nonnull !3
; call core::fmt::Formatter::debug_lower_hex
  %_3.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !4
  br i1 %_3.i, label %bb2.i, label %bb4.i

bb4.i:                                            ; preds = %start
; call core::fmt::Formatter::debug_upper_hex
  %_7.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !4
  br i1 %_7.i, label %bb6.i, label %bb8.i

bb2.i:                                            ; preds = %start
; call core::fmt::num::<impl core::fmt::LowerHex for usize>::fmt
  %0 = tail call zeroext i1 @"_ZN4core3fmt3num55_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$usize$GT$3fmt17h7a0715c481903e22E"(i64* noalias nonnull readonly align 8 dereferenceable(8) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..Debug$u20$for$u20$usize$GT$3fmt17h06e7e77e2b0b2d99E.exit"

bb8.i:                                            ; preds = %bb4.i
; call core::fmt::num::imp::<impl core::fmt::Display for usize>::fmt
  %1 = tail call zeroext i1 @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E"(i64* noalias nonnull readonly align 8 dereferenceable(8) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..Debug$u20$for$u20$usize$GT$3fmt17h06e7e77e2b0b2d99E.exit"

bb6.i:                                            ; preds = %bb4.i
; call core::fmt::num::<impl core::fmt::UpperHex for usize>::fmt
  %2 = tail call zeroext i1 @"_ZN4core3fmt3num55_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$usize$GT$3fmt17h8135d2e0b80633e3E"(i64* noalias nonnull readonly align 8 dereferenceable(8) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..Debug$u20$for$u20$usize$GT$3fmt17h06e7e77e2b0b2d99E.exit"

"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..Debug$u20$for$u20$usize$GT$3fmt17h06e7e77e2b0b2d99E.exit": ; preds = %bb2.i, %bb8.i, %bb6.i
  %.0.in.i = phi i1 [ %0, %bb2.i ], [ %2, %bb6.i ], [ %1, %bb8.i ]
  ret i1 %.0.in.i
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17he58e747b14e5c3beE"({ i64, i64 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #1 {
start:
  %_4 = load { i64, i64 }*, { i64, i64 }** %self, align 8, !nonnull !3
; call <core::alloc::layout::Layout as core::fmt::Debug>::fmt
  %0 = tail call zeroext i1 @"_ZN64_$LT$core..alloc..layout..Layout$u20$as$u20$core..fmt..Debug$GT$3fmt17hea3f85e7ab53336fE"({ i64, i64 }* noalias nonnull readonly align 8 dereferenceable(16) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %0
}

; core::panicking::panic
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #4

; alloc::alloc::handle_alloc_error
; Function Attrs: cold noreturn nounwind nonlazybind uwtable
declare void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64, i64) unnamed_addr #5

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.start.p0i8(i64 immarg, i8* nocapture) #6

; core::fmt::Formatter::debug_struct
; Function Attrs: nonlazybind uwtable
declare i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #1

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.end.p0i8(i64 immarg, i8* nocapture) #6

; core::fmt::builders::DebugStruct::field
; Function Attrs: nonlazybind uwtable
declare align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias align 8 dereferenceable(16), [0 x i8]* noalias nonnull readonly align 1, i64, {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24)) unnamed_addr #1

; core::fmt::builders::DebugStruct::finish
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias align 8 dereferenceable(16)) unnamed_addr #1

; core::fmt::Formatter::write_str
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #1

; core::fmt::Formatter::debug_tuple
; Function Attrs: nonlazybind uwtable
declare void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture sret(%"core::fmt::builders::DebugTuple") dereferenceable(24), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #1

; core::fmt::builders::DebugTuple::field
; Function Attrs: nonlazybind uwtable
declare align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias align 8 dereferenceable(24), {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24)) unnamed_addr #1

; core::fmt::builders::DebugTuple::finish
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias align 8 dereferenceable(24)) unnamed_addr #1

; core::fmt::Formatter::debug_lower_hex
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias readonly align 8 dereferenceable(64)) unnamed_addr #1

; core::fmt::num::<impl core::fmt::LowerHex for usize>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num55_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$usize$GT$3fmt17h7a0715c481903e22E"(i64* noalias readonly align 8 dereferenceable(8), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #1

; core::fmt::Formatter::debug_upper_hex
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias readonly align 8 dereferenceable(64)) unnamed_addr #1

; core::fmt::num::<impl core::fmt::UpperHex for usize>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num55_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$usize$GT$3fmt17h8135d2e0b80633e3E"(i64* noalias readonly align 8 dereferenceable(8), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #1

; core::fmt::num::imp::<impl core::fmt::Display for usize>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E"(i64* noalias readonly align 8 dereferenceable(8), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #1

; <core::alloc::layout::Layout as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN64_$LT$core..alloc..layout..Layout$u20$as$u20$core..fmt..Debug$GT$3fmt17hea3f85e7ab53336fE"({ i64, i64 }* noalias readonly align 8 dereferenceable(16), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #1

attributes #0 = { inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #3 = { mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #4 = { cold noinline noreturn nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #5 = { cold noreturn nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #6 = { argmemonly mustprogress nofree nosync nounwind willreturn }
attributes #7 = { noreturn }
attributes #8 = { noreturn nounwind }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
!2 = !{i64 0, i64 2}
!3 = !{}
!4 = !{!5}
!5 = distinct !{!5, !6, !"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..Debug$u20$for$u20$usize$GT$3fmt17h06e7e77e2b0b2d99E: %self"}
!6 = distinct !{!6, !"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..Debug$u20$for$u20$usize$GT$3fmt17h06e7e77e2b0b2d99E"}
