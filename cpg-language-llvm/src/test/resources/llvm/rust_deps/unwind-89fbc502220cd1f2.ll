; ModuleID = 'unwind.e482d75d-cgu.0'
source_filename = "unwind.e482d75d-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"core::fmt::Formatter" = type { { i64, i64 }, { i64, i64 }, { {}*, [3 x i64]* }, i32, i32, i8, [7 x i8] }

@alloc48 = private unnamed_addr constant <{ [12 x i8] }> <{ [12 x i8] c"_URC_FAILURE" }>, align 1
@alloc49 = private unnamed_addr constant <{ [20 x i8] }> <{ [20 x i8] c"_URC_CONTINUE_UNWIND" }>, align 1
@alloc50 = private unnamed_addr constant <{ [20 x i8] }> <{ [20 x i8] c"_URC_INSTALL_CONTEXT" }>, align 1
@alloc51 = private unnamed_addr constant <{ [18 x i8] }> <{ [18 x i8] c"_URC_HANDLER_FOUND" }>, align 1
@alloc52 = private unnamed_addr constant <{ [17 x i8] }> <{ [17 x i8] c"_URC_END_OF_STACK" }>, align 1
@alloc53 = private unnamed_addr constant <{ [16 x i8] }> <{ [16 x i8] c"_URC_NORMAL_STOP" }>, align 1
@alloc54 = private unnamed_addr constant <{ [23 x i8] }> <{ [23 x i8] c"_URC_FATAL_PHASE1_ERROR" }>, align 1
@alloc55 = private unnamed_addr constant <{ [23 x i8] }> <{ [23 x i8] c"_URC_FATAL_PHASE2_ERROR" }>, align 1
@alloc56 = private unnamed_addr constant <{ [29 x i8] }> <{ [29 x i8] c"_URC_FOREIGN_EXCEPTION_CAUGHT" }>, align 1
@alloc57 = private unnamed_addr constant <{ [14 x i8] }> <{ [14 x i8] c"_URC_NO_REASON" }>, align 1

; <unwind::libunwind::_Unwind_Reason_Code as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN75_$LT$unwind..libunwind.._Unwind_Reason_Code$u20$as$u20$core..fmt..Debug$GT$3fmt17h6f5026e6881e895eE"(i32* noalias nocapture readonly align 4 dereferenceable(4) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #0 {
start:
  %0 = load i32, i32* %self, align 4, !range !2
  %_5 = zext i32 %0 to i64
  switch i64 %_5, label %bb2 [
    i64 0, label %bb3
    i64 1, label %bb5
    i64 2, label %bb7
    i64 3, label %bb9
    i64 4, label %bb11
    i64 5, label %bb13
    i64 6, label %bb15
    i64 7, label %bb17
    i64 8, label %bb19
    i64 9, label %bb1
  ]

bb2:                                              ; preds = %start
  unreachable

bb3:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %1 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [14 x i8] }>* @alloc57 to [0 x i8]*), i64 14)
  br label %bb22

bb5:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %2 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [29 x i8] }>* @alloc56 to [0 x i8]*), i64 29)
  br label %bb22

bb7:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %3 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [23 x i8] }>* @alloc55 to [0 x i8]*), i64 23)
  br label %bb22

bb9:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %4 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [23 x i8] }>* @alloc54 to [0 x i8]*), i64 23)
  br label %bb22

bb11:                                             ; preds = %start
; call core::fmt::Formatter::write_str
  %5 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [16 x i8] }>* @alloc53 to [0 x i8]*), i64 16)
  br label %bb22

bb13:                                             ; preds = %start
; call core::fmt::Formatter::write_str
  %6 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [17 x i8] }>* @alloc52 to [0 x i8]*), i64 17)
  br label %bb22

bb15:                                             ; preds = %start
; call core::fmt::Formatter::write_str
  %7 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [18 x i8] }>* @alloc51 to [0 x i8]*), i64 18)
  br label %bb22

bb17:                                             ; preds = %start
; call core::fmt::Formatter::write_str
  %8 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [20 x i8] }>* @alloc50 to [0 x i8]*), i64 20)
  br label %bb22

bb19:                                             ; preds = %start
; call core::fmt::Formatter::write_str
  %9 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [20 x i8] }>* @alloc49 to [0 x i8]*), i64 20)
  br label %bb22

bb1:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %10 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [12 x i8] }>* @alloc48 to [0 x i8]*), i64 12)
  br label %bb22

bb22:                                             ; preds = %bb3, %bb5, %bb7, %bb9, %bb11, %bb13, %bb15, %bb17, %bb19, %bb1
  %.0.in = phi i1 [ %10, %bb1 ], [ %9, %bb19 ], [ %8, %bb17 ], [ %7, %bb15 ], [ %6, %bb13 ], [ %5, %bb11 ], [ %4, %bb9 ], [ %3, %bb7 ], [ %2, %bb5 ], [ %1, %bb3 ]
  ret i1 %.0.in
}

; core::fmt::Formatter::write_str
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #0

attributes #0 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
!2 = !{i32 0, i32 10}
