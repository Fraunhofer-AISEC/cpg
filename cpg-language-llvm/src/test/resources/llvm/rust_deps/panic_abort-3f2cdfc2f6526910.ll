; ModuleID = 'panic_abort.ccbfe680-cgu.0'
source_filename = "panic_abort.ccbfe680-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"core::panic::location::Location" = type { { [0 x i8]*, i64 }, i32, i32 }

@alloc4 = private unnamed_addr constant <{ [40 x i8] }> <{ [40 x i8] c"internal error: entered unreachable code" }>, align 1
@alloc5 = private unnamed_addr constant <{ [118 x i8] }> <{ [118 x i8] c"/home/alwagner/.rustup/toolchains/nightly-x86_64-unknown-linux-gnu/lib/rustlib/src/rust/library/panic_abort/src/lib.rs" }>, align 1
@alloc6 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [118 x i8] }>, <{ [118 x i8] }>* @alloc5, i32 0, i32 0, i32 0), [16 x i8] c"v\00\00\00\00\00\00\00\1C\00\00\00\05\00\00\00" }>, align 8

; Function Attrs: noreturn nounwind nonlazybind uwtable
define { i64, i64 } @__rust_panic_cleanup(i8* nocapture readnone %_1) unnamed_addr #0 personality void ()* @rust_eh_personality {
start:
; invoke core::panicking::panic
  invoke void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [40 x i8] }>* @alloc4 to [0 x i8]*), i64 40, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc6 to %"core::panic::location::Location"*)) #4
          to label %unreachable unwind label %cleanup

unreachable:                                      ; preds = %start
  unreachable

cleanup:                                          ; preds = %start
  %0 = landingpad { i8*, i32 }
          cleanup
  tail call void @llvm.trap()
  unreachable
}

; Function Attrs: noreturn nounwind nonlazybind uwtable
define i32 @__rust_start_panic({ {}*, [3 x i64]* }* nocapture readnone %_payload) unnamed_addr #0 {
start:
; call panic_abort::__rust_start_panic::abort
  tail call fastcc void @_ZN11panic_abort18__rust_start_panic5abort17h94d5cb754fe7d6b0E() #4
  unreachable
}

; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define void @rust_eh_personality() unnamed_addr #1 {
start:
  ret void
}

; panic_abort::__rust_start_panic::abort
; Function Attrs: noreturn nounwind nonlazybind uwtable
define internal fastcc void @_ZN11panic_abort18__rust_start_panic5abort17h94d5cb754fe7d6b0E() unnamed_addr #0 {
start:
  tail call void @abort() #5
  unreachable
}

; core::panicking::panic
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #2

; Function Attrs: cold noreturn nounwind
declare void @llvm.trap() #3

; Function Attrs: noreturn nounwind nonlazybind uwtable
declare void @abort() unnamed_addr #0

attributes #0 = { noreturn nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { cold noinline noreturn nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #3 = { cold noreturn nounwind }
attributes #4 = { noreturn }
attributes #5 = { noreturn nounwind }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
