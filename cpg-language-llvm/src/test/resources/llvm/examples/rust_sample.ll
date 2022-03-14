; ModuleID = 'cpg_rs_test.dbbe0041-cgu.0'
source_filename = "cpg_rs_test.dbbe0041-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"core::fmt::Arguments" = type { { [0 x { [0 x i8]*, i64 }]*, i64 }, { i64*, i64 }, { [0 x { i8*, i64* }]*, i64 } }
%"unwind::libunwind::_Unwind_Exception" = type { i64, void (i32, %"unwind::libunwind::_Unwind_Exception"*)*, [6 x i64] }
%"unwind::libunwind::_Unwind_Context" = type { [0 x i8] }

@vtable.0 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, i8*, i8*, [0 x i8] }> <{ i8* bitcast (void (i64**)* @"_ZN4core3ptr85drop_in_place$LT$std..rt..lang_start$LT$$LP$$RP$$GT$..$u7b$$u7b$closure$u7d$$u7d$$GT$17h2182095d83600a8fE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i32 (i64**)* @"_ZN4core3ops8function6FnOnce40call_once$u7b$$u7b$vtable.shim$u7d$$u7d$17h5efe6b9a0939581cE" to i8*), i8* bitcast (i32 (i64**)* @"_ZN3std2rt10lang_start28_$u7b$$u7b$closure$u7d$$u7d$17h40bc64437f27ebd0E" to i8*), i8* bitcast (i32 (i64**)* @"_ZN3std2rt10lang_start28_$u7b$$u7b$closure$u7d$$u7d$17h40bc64437f27ebd0E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc2 = private unnamed_addr constant <{ [14 x i8] }> <{ [14 x i8] c"Hello, world!\0A" }>, align 1
@alloc3 = private unnamed_addr constant <{ i8*, [8 x i8] }> <{ i8* getelementptr inbounds (<{ [14 x i8] }>, <{ [14 x i8] }>* @alloc2, i32 0, i32 0, i32 0), [8 x i8] c"\0E\00\00\00\00\00\00\00" }>, align 8
@alloc5 = private unnamed_addr constant <{ [0 x i8] }> zeroinitializer, align 8

; std::sys_common::backtrace::__rust_begin_short_backtrace
; Function Attrs: noinline nonlazybind uwtable
define internal fastcc void @_ZN3std10sys_common9backtrace28__rust_begin_short_backtrace17h748911433efa822cE(void ()* nocapture nonnull %f) unnamed_addr #0 personality i32 (i32, i32, i64, %"unwind::libunwind::_Unwind_Exception"*, %"unwind::libunwind::_Unwind_Context"*)* @rust_eh_personality {
start:
  tail call void %f()
  tail call void asm sideeffect "", "r,~{memory}"({}* undef) #6, !srcloc !3
  ret void
}

; std::rt::lang_start
; Function Attrs: nonlazybind uwtable
define hidden i64 @_ZN3std2rt10lang_start17h4ce2d0d2984ce443E(void ()* nonnull %main, i64 %argc, i8** %argv) unnamed_addr #1 {
start:
  %_8 = alloca i64*, align 8
  %0 = bitcast i64** %_8 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %0)
  %1 = bitcast i64** %_8 to void ()**
  store void ()* %main, void ()** %1, align 8
  %_5.0 = bitcast i64** %_8 to {}*
; call std::rt::lang_start_internal
  %2 = call i64 @_ZN3std2rt19lang_start_internal17hd15a47be08101c28E({}* nonnull align 1 %_5.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, i8*, i8*, [0 x i8] }>* @vtable.0 to [3 x i64]*), i64 %argc, i8** %argv)
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %0)
  ret i64 %2
}

; std::rt::lang_start::{{closure}}
; Function Attrs: inlinehint nonlazybind uwtable
define internal i32 @"_ZN3std2rt10lang_start28_$u7b$$u7b$closure$u7d$$u7d$17h40bc64437f27ebd0E"(i64** noalias nocapture readonly align 8 dereferenceable(8) %_1) unnamed_addr #2 {
start:
  %0 = bitcast i64** %_1 to void ()**
  %_3 = load void ()*, void ()** %0, align 8, !nonnull !4
; call std::sys_common::backtrace::__rust_begin_short_backtrace
  tail call fastcc void @_ZN3std10sys_common9backtrace28__rust_begin_short_backtrace17h748911433efa822cE(void ()* nonnull %_3)
  ret i32 0
}

; core::ops::function::FnOnce::call_once{{vtable.shim}}
; Function Attrs: inlinehint nonlazybind uwtable
define internal i32 @"_ZN4core3ops8function6FnOnce40call_once$u7b$$u7b$vtable.shim$u7d$$u7d$17h5efe6b9a0939581cE"(i64** nocapture readonly %_1) unnamed_addr #2 personality i32 (i32, i32, i64, %"unwind::libunwind::_Unwind_Exception"*, %"unwind::libunwind::_Unwind_Context"*)* @rust_eh_personality {
start:
  %0 = bitcast i64** %_1 to void ()**
  %1 = load void ()*, void ()** %0, align 8, !nonnull !4
; call std::sys_common::backtrace::__rust_begin_short_backtrace
  tail call fastcc void @_ZN3std10sys_common9backtrace28__rust_begin_short_backtrace17h748911433efa822cE(void ()* nonnull %1), !noalias !5
  ret i32 0
}

; core::ptr::drop_in_place<std::rt::lang_start<()>::{{closure}}>
; Function Attrs: inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define internal void @"_ZN4core3ptr85drop_in_place$LT$std..rt..lang_start$LT$$LP$$RP$$GT$..$u7b$$u7b$closure$u7d$$u7d$$GT$17h2182095d83600a8fE"(i64** nocapture %_1) unnamed_addr #3 {
start:
  ret void
}

; cpg_rs_test::main
; Function Attrs: nonlazybind uwtable
define internal void @_ZN11cpg_rs_test4main17h9e4ddb287fe877f0E() unnamed_addr #1 {
start:
  %_2 = alloca %"core::fmt::Arguments", align 8
  %0 = bitcast %"core::fmt::Arguments"* %_2 to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %0)
  %1 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_2, i64 0, i32 0, i32 0
  store [0 x { [0 x i8]*, i64 }]* bitcast (<{ i8*, [8 x i8] }>* @alloc3 to [0 x { [0 x i8]*, i64 }]*), [0 x { [0 x i8]*, i64 }]** %1, align 8, !alias.scope !8
  %2 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_2, i64 0, i32 0, i32 1
  store i64 1, i64* %2, align 8, !alias.scope !8
  %3 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_2, i64 0, i32 1, i32 0
  store i64* null, i64** %3, align 8, !alias.scope !8
  %4 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_2, i64 0, i32 2, i32 0
  store [0 x { i8*, i64* }]* bitcast (<{ [0 x i8] }>* @alloc5 to [0 x { i8*, i64* }]*), [0 x { i8*, i64* }]** %4, align 8, !alias.scope !8
  %5 = getelementptr inbounds %"core::fmt::Arguments", %"core::fmt::Arguments"* %_2, i64 0, i32 2, i32 1
  store i64 0, i64* %5, align 8, !alias.scope !8
; call std::io::stdio::_print
  call void @_ZN3std2io5stdio6_print17hd9c9dbd31aa97d70E(%"core::fmt::Arguments"* noalias nocapture nonnull dereferenceable(48) %_2)
  call void @llvm.lifetime.end.p0i8(i64 48, i8* nonnull %0)
  ret void
}

; Function Attrs: nonlazybind uwtable
declare i32 @rust_eh_personality(i32, i32, i64, %"unwind::libunwind::_Unwind_Exception"*, %"unwind::libunwind::_Unwind_Context"*) unnamed_addr #1

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.start.p0i8(i64 immarg, i8* nocapture) #4

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.end.p0i8(i64 immarg, i8* nocapture) #4

; std::rt::lang_start_internal
; Function Attrs: nonlazybind uwtable
declare i64 @_ZN3std2rt19lang_start_internal17hd15a47be08101c28E({}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24), i64, i8**) unnamed_addr #1

; std::io::stdio::_print
; Function Attrs: nonlazybind uwtable
declare void @_ZN3std2io5stdio6_print17hd9c9dbd31aa97d70E(%"core::fmt::Arguments"* noalias nocapture dereferenceable(48)) unnamed_addr #1

; Function Attrs: nonlazybind
define i32 @main(i32 %0, i8** %1) unnamed_addr #5 {
top:
  %_8.i = alloca i64*, align 8
  %2 = sext i32 %0 to i64
  %3 = bitcast i64** %_8.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  %4 = bitcast i64** %_8.i to void ()**
  store void ()* @_ZN11cpg_rs_test4main17h9e4ddb287fe877f0E, void ()** %4, align 8
  %_5.0.i = bitcast i64** %_8.i to {}*
; call std::rt::lang_start_internal
  %5 = call i64 @_ZN3std2rt19lang_start_internal17hd15a47be08101c28E({}* nonnull align 1 %_5.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, i8*, i8*, [0 x i8] }>* @vtable.0 to [3 x i64]*), i64 %2, i8** %1)
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
  %6 = trunc i64 %5 to i32
  ret i32 %6
}

attributes #0 = { noinline nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { inlinehint nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #3 = { inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #4 = { argmemonly mustprogress nofree nosync nounwind willreturn }
attributes #5 = { nonlazybind "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #6 = { nounwind }

!llvm.module.flags = !{!0, !1, !2}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 7, !"PIE Level", i32 2}
!2 = !{i32 2, !"RtLibUseGOT", i32 1}
!3 = !{i32 3165991}
!4 = !{}
!5 = !{!6}
!6 = distinct !{!6, !7, !"_ZN3std2rt10lang_start28_$u7b$$u7b$closure$u7d$$u7d$17h40bc64437f27ebd0E: %_1"}
!7 = distinct !{!7, !"_ZN3std2rt10lang_start28_$u7b$$u7b$closure$u7d$$u7d$17h40bc64437f27ebd0E"}
!8 = !{!9}
!9 = distinct !{!9, !10, !"_ZN4core3fmt9Arguments6new_v117h28f78a49eb4e2f12E: argument 0"}
!10 = distinct !{!10, !"_ZN4core3fmt9Arguments6new_v117h28f78a49eb4e2f12E"}
