; ModuleID = 'addr2line.93d04925-cgu.0'
source_filename = "addr2line.93d04925-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>" = type { {}*, [2 x i64] }
%"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>" = type { i64, [2 x i64] }
%"core::option::Option<(u64, u64, Location)>" = type { [8 x i32], i32, [3 x i32] }
%LocationRangeUnitIter = type { %Lines*, { [0 x %LineSequence]*, i64 }, i64, i64, i64 }
%Lines = type { { [0 x %"alloc::string::String"]*, i64 }, { [0 x %LineSequence]*, i64 } }
%"alloc::string::String" = type { %"alloc::vec::Vec<u8>" }
%"alloc::vec::Vec<u8>" = type { { i8*, i64 }, i64 }
%LineSequence = type { i64, i64, { [0 x %LineRow]*, i64 } }
%LineRow = type { i64, i64, i32, i32 }
%"core::option::Option<alloc::string::String>" = type { {}*, [2 x i64] }
%"alloc::borrow::Cow<str>" = type { i64, [3 x i64] }

@alloc356 = private unnamed_addr constant <{ [2 x i8] }> <{ [2 x i8] c":\\" }>, align 1

; alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
; Function Attrs: cold nonlazybind uwtable
define internal fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17hd4a8cd68ada581f5E"({ i8*, i64 }* noalias nocapture align 8 dereferenceable(16) %slf, i64 %len, i64 %additional) unnamed_addr #0 personality i32 (...)* @rust_eh_personality {
start:
  %_30.i = alloca %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", align 8
  %_28.i = alloca %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", align 8
  tail call void @llvm.experimental.noalias.scope.decl(metadata !2)
  %0 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %len, i64 %additional) #17
  %1 = extractvalue { i64, i1 } %0, 0
  %2 = extractvalue { i64, i1 } %0, 1
  br i1 %2, label %bb5.i, label %bb8.i

bb8.i:                                            ; preds = %start
  %3 = getelementptr { i8*, i64 }, { i8*, i64 }* %slf, i64 0, i32 1
  %_20.i = load i64, i64* %3, align 8, !alias.scope !2, !noalias !5
  %_19.i = shl i64 %_20.i, 1
  %4 = icmp ugt i64 %_19.i, %1
  %.0.sroa.speculated.i.i.i.i = select i1 %4, i64 %_19.i, i64 %1
  %5 = icmp ugt i64 %.0.sroa.speculated.i.i.i.i, 8
  %.0.sroa.speculated.i.i.i16.i = select i1 %5, i64 %.0.sroa.speculated.i.i.i.i, i64 8
  %6 = bitcast %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %_28.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %6) #17, !noalias !7
  %7 = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %7) #17, !noalias !7
  %self.idx.i = getelementptr { i8*, i64 }, { i8*, i64 }* %slf, i64 0, i32 0
  %_4.i.i = icmp eq i64 %_20.i, 0
  br i1 %_4.i.i, label %bb5.i.i, label %bb6.i.i

bb6.i.i:                                          ; preds = %bb8.i
  %self.idx.val.i = load i8*, i8** %self.idx.i, align 8, !alias.scope !2, !noalias !5
  %_13.sroa.0.0..sroa_idx.i.i = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i to i8**
  store i8* %self.idx.val.i, i8** %_13.sroa.0.0..sroa_idx.i.i, align 8, !alias.scope !8, !noalias !7
  %8 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i, i64 0, i32 1, i64 0
  store i64 %_20.i, i64* %8, align 8, !alias.scope !8, !noalias !7
  %9 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i, i64 0, i32 1, i64 1
  store i64 1, i64* %9, align 8, !alias.scope !8, !noalias !7
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE.exit.i"

bb5.i.i:                                          ; preds = %bb8.i
  %10 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i, i64 0, i32 0
  store {}* null, {}** %10, align 8, !alias.scope !8, !noalias !7
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE.exit.i"

"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE.exit.i": ; preds = %bb5.i.i, %bb6.i.i
; call alloc::raw_vec::finish_grow
  call fastcc void @_ZN5alloc7raw_vec11finish_grow17h5d0d1cfc1c15f922E(%"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* noalias nocapture nonnull dereferenceable(24) %_28.i, i64 %.0.sroa.speculated.i.i.i16.i, i64 1, %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* noalias nocapture nonnull dereferenceable(24) %_30.i) #17, !noalias !7
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %7) #17, !noalias !7
  %11 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %_28.i, i64 0, i32 0
  %_2.i.i = load i64, i64* %11, align 8, !range !11, !alias.scope !12, !noalias !15
  %switch.not.i17.i = icmp eq i64 %_2.i.i, 1
  br i1 %switch.not.i17.i, label %bb3.i, label %_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE.exit

bb3.i:                                            ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE.exit.i"
  %12 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %_28.i, i64 0, i32 1, i64 0
  %e.0.i.i = load i64, i64* %12, align 8, !alias.scope !12, !noalias !15
  %13 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %_28.i, i64 0, i32 1, i64 1
  %e.1.i.i = load i64, i64* %13, align 8, !alias.scope !12, !noalias !15
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %6) #17, !noalias !7
  %14 = icmp eq i64 %e.1.i.i, 0
  br i1 %14, label %bb5.i, label %bb6.i

bb5.i:                                            ; preds = %start, %bb3.i
; call alloc::raw_vec::capacity_overflow
  tail call void @_ZN5alloc7raw_vec17capacity_overflow17hbdad83560505d524E() #18, !noalias !17
  unreachable

bb6.i:                                            ; preds = %bb3.i
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %e.0.i.i, i64 %e.1.i.i) #19, !noalias !17
  unreachable

_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE.exit: ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE.exit.i"
  %15 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %_28.i, i64 0, i32 1
  %16 = bitcast [2 x i64]* %15 to i8**
  %v.0.i46.i = load i8*, i8** %16, align 8, !alias.scope !12, !noalias !15, !nonnull !20
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %6) #17, !noalias !7
  store i8* %v.0.i46.i, i8** %self.idx.i, align 8, !alias.scope !21, !noalias !5
  store i64 %.0.sroa.speculated.i.i.i16.i, i64* %3, align 8, !alias.scope !21, !noalias !5
  ret void
}

; alloc::raw_vec::RawVec<T,A>::reserve_for_push
; Function Attrs: noinline nonlazybind uwtable
define internal fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$16reserve_for_push17hada22938690d550eE"({ i8*, i64 }* noalias nocapture align 8 dereferenceable(16) %self, i64 %len) unnamed_addr #1 personality i32 (...)* @rust_eh_personality {
start:
  %_30.i = alloca %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", align 8
  %_28.i = alloca %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", align 8
  tail call void @llvm.experimental.noalias.scope.decl(metadata !24)
  %0 = tail call { i64, i1 } @llvm.uadd.with.overflow.i64(i64 %len, i64 1) #17
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
  %6 = bitcast %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %_28.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %6) #17, !noalias !29
  %7 = bitcast %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %7) #17, !noalias !29
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
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE.exit.i"

bb5.i.i:                                          ; preds = %bb8.i
  %10 = getelementptr inbounds %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>", %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* %_30.i, i64 0, i32 0
  store {}* null, {}** %10, align 8, !alias.scope !30, !noalias !29
  br label %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE.exit.i"

"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE.exit.i": ; preds = %bb5.i.i, %bb6.i.i
; call alloc::raw_vec::finish_grow
  call fastcc void @_ZN5alloc7raw_vec11finish_grow17h5d0d1cfc1c15f922E(%"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* noalias nocapture nonnull dereferenceable(24) %_28.i, i64 %.0.sroa.speculated.i.i.i16.i, i64 1, %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* noalias nocapture nonnull dereferenceable(24) %_30.i) #17, !noalias !29
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %7) #17, !noalias !29
  %11 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %_28.i, i64 0, i32 0
  %_2.i.i = load i64, i64* %11, align 8, !range !11, !alias.scope !33, !noalias !36
  %switch.not.i17.i = icmp eq i64 %_2.i.i, 1
  br i1 %switch.not.i17.i, label %bb3.i, label %_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE.exit

bb3.i:                                            ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE.exit.i"
  %12 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %_28.i, i64 0, i32 1, i64 0
  %e.0.i.i = load i64, i64* %12, align 8, !alias.scope !33, !noalias !36
  %13 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %_28.i, i64 0, i32 1, i64 1
  %e.1.i.i = load i64, i64* %13, align 8, !alias.scope !33, !noalias !36
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %6) #17, !noalias !29
  %14 = icmp eq i64 %e.1.i.i, 0
  br i1 %14, label %bb5.i, label %bb6.i

bb5.i:                                            ; preds = %start, %bb3.i
; call alloc::raw_vec::capacity_overflow
  tail call void @_ZN5alloc7raw_vec17capacity_overflow17hbdad83560505d524E() #18, !noalias !38
  unreachable

bb6.i:                                            ; preds = %bb3.i
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %e.0.i.i, i64 %e.1.i.i) #19, !noalias !38
  unreachable

_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE.exit: ; preds = %"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE.exit.i"
  %15 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %_28.i, i64 0, i32 1
  %16 = bitcast [2 x i64]* %15 to i8**
  %v.0.i46.i = load i8*, i8** %16, align 8, !alias.scope !33, !noalias !36, !nonnull !20
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %6) #17, !noalias !29
  store i8* %v.0.i46.i, i8** %self.idx.i, align 8, !alias.scope !41, !noalias !27
  store i64 %.0.sroa.speculated.i.i.i16.i, i64* %3, align 8, !alias.scope !41, !noalias !27
  ret void
}

; alloc::raw_vec::finish_grow
; Function Attrs: noinline nounwind nonlazybind uwtable
define internal fastcc void @_ZN5alloc7raw_vec11finish_grow17h5d0d1cfc1c15f922E(%"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* noalias nocapture dereferenceable(24) %0, i64 %new_layout.0, i64 %new_layout.1, %"core::option::Option<(core::ptr::non_null::NonNull<u8>, core::alloc::layout::Layout)>"* noalias nocapture readonly dereferenceable(24) %current_memory) unnamed_addr #2 {
start:
  %1 = icmp eq i64 %new_layout.1, 0
  br i1 %1, label %bb5, label %bb10

bb5:                                              ; preds = %start
  %2 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %0, i64 0, i32 1, i64 0
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
  %6 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %0, i64 0, i32 1, i64 1
  store i64 %.sink52, i64* %6, align 8
  %7 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %0, i64 0, i32 0
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
  %14 = tail call i8* @__rust_alloc(i64 %new_layout.0, i64 %new_layout.1) #17
  br label %bb21

bb7.i.i:                                          ; preds = %bb15
  %_21.i.i = icmp ule i64 %9, %new_layout.0
  tail call void @llvm.assume(i1 %_21.i.i) #17
  %15 = tail call i8* @__rust_realloc(i8* nonnull %5, i64 %9, i64 %new_layout.1, i64 %new_layout.0) #17
  %16 = icmp eq i8* %15, null
  br i1 %16, label %bb1.i11, label %bb3.i10

bb14:                                             ; preds = %bb10
  %17 = icmp eq i64 %new_layout.0, 0
  br i1 %17, label %bb3.i.i, label %bb8.i.i9

bb3.i.i:                                          ; preds = %bb14
  %_2.i.i.i = inttoptr i64 %new_layout.1 to i8*
  br label %bb21

bb8.i.i9:                                         ; preds = %bb14
  %18 = tail call i8* @__rust_alloc(i64 %new_layout.0, i64 %new_layout.1) #17
  br label %bb21

bb21:                                             ; preds = %bb3.i.i.i, %bb8.i.i.i, %bb8.i.i9, %bb3.i.i
  %.sroa.0.2.i.i.pn = phi i8* [ %_2.i.i.i, %bb3.i.i ], [ %18, %bb8.i.i9 ], [ %_2.i.i.i.i, %bb3.i.i.i ], [ %14, %bb8.i.i.i ]
  %19 = icmp eq i8* %.sroa.0.2.i.i.pn, null
  br i1 %19, label %bb1.i11, label %bb3.i10

bb3.i10:                                          ; preds = %bb7.i.i, %bb21
  %.sroa.0.2.i.i.pn50 = phi i8* [ %.sroa.0.2.i.i.pn, %bb21 ], [ %15, %bb7.i.i ]
  %20 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %0, i64 0, i32 1
  %21 = bitcast [2 x i64]* %20 to i8**
  store i8* %.sroa.0.2.i.i.pn50, i8** %21, align 8, !alias.scope !48, !noalias !51
  br label %bb24

bb1.i11:                                          ; preds = %bb7.i.i, %bb21
  %22 = getelementptr inbounds %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>", %"core::result::Result<core::ptr::non_null::NonNull<[u8]>, alloc::collections::TryReserveError>"* %0, i64 0, i32 1, i64 0
  store i64 %new_layout.0, i64* %22, align 8, !alias.scope !48, !noalias !51
  br label %bb24
}

; <addr2line::LocationRangeUnitIter as core::iter::traits::iterator::Iterator>::next
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define void @"_ZN91_$LT$addr2line..LocationRangeUnitIter$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17hbdbcd2629b19570eE"(%"core::option::Option<(u64, u64, Location)>"* noalias nocapture sret(%"core::option::Option<(u64, u64, Location)>") dereferenceable(48) %0, %LocationRangeUnitIter* noalias nocapture align 8 dereferenceable(48) %self) unnamed_addr #3 {
start:
  %1 = getelementptr inbounds %LocationRangeUnitIter, %LocationRangeUnitIter* %self, i64 0, i32 1, i32 0
  %_4.0 = load [0 x %LineSequence]*, [0 x %LineSequence]** %1, align 8, !nonnull !20
  %2 = getelementptr inbounds %LocationRangeUnitIter, %LocationRangeUnitIter* %self, i64 0, i32 1, i32 1
  %_4.1 = load i64, i64* %2, align 8
  %3 = getelementptr inbounds %LocationRangeUnitIter, %LocationRangeUnitIter* %self, i64 0, i32 2
  %4 = getelementptr inbounds %LocationRangeUnitIter, %LocationRangeUnitIter* %self, i64 0, i32 4
  %_10 = load i64, i64* %4, align 8
  %5 = getelementptr inbounds %LocationRangeUnitIter, %LocationRangeUnitIter* %self, i64 0, i32 3
  %_572 = load i64, i64* %3, align 8
  %_3.i.i73 = icmp ult i64 %_572, %_4.1
  br i1 %_3.i.i73, label %bb5.preheader, label %bb26

bb5.preheader:                                    ; preds = %start
  %6 = getelementptr inbounds [0 x %LineSequence], [0 x %LineSequence]* %_4.0, i64 0, i64 %_572, i32 0
  br label %bb5

bb5:                                              ; preds = %bb5.preheader, %bb9
  %.0.i.i76 = phi i64* [ %.0.i.i, %bb9 ], [ %6, %bb5.preheader ]
  %_575 = phi i64 [ %10, %bb9 ], [ %_572, %bb5.preheader ]
  %_9 = load i64, i64* %.0.i.i76, align 8
  %_8.not = icmp ult i64 %_9, %_10
  br i1 %_8.not, label %bb7, label %bb26

bb7:                                              ; preds = %bb5
  %7 = getelementptr inbounds i64, i64* %.0.i.i76, i64 2
  %8 = bitcast i64* %7 to [0 x %LineRow]**
  %_12.0 = load [0 x %LineRow]*, [0 x %LineRow]** %8, align 8, !nonnull !20
  %9 = getelementptr inbounds i64, i64* %.0.i.i76, i64 3
  %_12.1 = load i64, i64* %9, align 8
  %_13 = load i64, i64* %5, align 8
  %_3.i.i54 = icmp ult i64 %_13, %_12.1
  br i1 %_3.i.i54, label %bb11, label %bb9

bb9:                                              ; preds = %bb7
  %10 = add nuw i64 %_575, 1
  store i64 %10, i64* %3, align 8
  store i64 0, i64* %5, align 8
  %_3.i.i = icmp ult i64 %10, %_4.1
  %11 = getelementptr inbounds [0 x %LineSequence], [0 x %LineSequence]* %_4.0, i64 0, i64 %10, i32 0
  %.0.i.i = select i1 %_3.i.i, i64* %11, i64* null
  %exitcond.not = icmp eq i64 %10, %_4.1
  br i1 %exitcond.not, label %bb26, label %bb5

bb11:                                             ; preds = %bb7
  %12 = getelementptr inbounds [0 x %LineRow], [0 x %LineRow]* %_12.0, i64 0, i64 %_13, i32 0
  %13 = bitcast i64* %12 to %LineRow*
  %_17 = load i64, i64* %12, align 8
  %_16.not = icmp ult i64 %_17, %_10
  br i1 %_16.not, label %bb13, label %bb26

bb13:                                             ; preds = %bb11
  %14 = bitcast %LocationRangeUnitIter* %self to { [0 x %"alloc::string::String"]*, i64 }**
  %15 = load { [0 x %"alloc::string::String"]*, i64 }*, { [0 x %"alloc::string::String"]*, i64 }** %14, align 8, !nonnull !20
  %16 = getelementptr inbounds { [0 x %"alloc::string::String"]*, i64 }, { [0 x %"alloc::string::String"]*, i64 }* %15, i64 0, i32 0
  %_21.0 = load [0 x %"alloc::string::String"]*, [0 x %"alloc::string::String"]** %16, align 8, !nonnull !20
  %17 = getelementptr inbounds { [0 x %"alloc::string::String"]*, i64 }, { [0 x %"alloc::string::String"]*, i64 }* %15, i64 0, i32 1
  %_21.1 = load i64, i64* %17, align 8
  %18 = getelementptr inbounds i64, i64* %12, i64 1
  %_23 = load i64, i64* %18, align 8
  %_3.i.i56 = icmp ult i64 %_23, %_21.1
  %19 = getelementptr inbounds [0 x %"alloc::string::String"], [0 x %"alloc::string::String"]* %_21.0, i64 0, i64 %_23
  %20 = bitcast %"alloc::string::String"* %19 to i64*
  %.0.i.i57 = select i1 %_3.i.i56, i64* %20, i64* null
  br i1 %_3.i.i56, label %bb3.i, label %"_ZN4core6option15Option$LT$T$GT$3map17h4327f575b61a265aE.exit"

bb3.i:                                            ; preds = %bb13
  %21 = bitcast i64* %.0.i.i57 to [0 x i8]**
  %_5.idx.val2.i.i.i.i = load [0 x i8]*, [0 x i8]** %21, align 8, !alias.scope !53
  %22 = getelementptr i64, i64* %.0.i.i57, i64 2
  %_5.idx1.val.i.i.i.i = load i64, i64* %22, align 8, !alias.scope !64
  %23 = getelementptr [0 x i8], [0 x i8]* %_5.idx.val2.i.i.i.i, i64 0, i64 0
  br label %"_ZN4core6option15Option$LT$T$GT$3map17h4327f575b61a265aE.exit"

"_ZN4core6option15Option$LT$T$GT$3map17h4327f575b61a265aE.exit": ; preds = %bb13, %bb3.i
  %.sroa.3.0.i = phi i64 [ %_5.idx1.val.i.i.i.i, %bb3.i ], [ undef, %bb13 ]
  %.sroa.0.0.i = phi i8* [ %23, %bb3.i ], [ null, %bb13 ]
  %_28 = add nuw i64 %_13, 1
  %_3.i.i58 = icmp ult i64 %_28, %_12.1
  %24 = getelementptr inbounds i64, i64* %.0.i.i76, i64 1
  %25 = getelementptr inbounds [0 x %LineRow], [0 x %LineRow]* %_12.0, i64 0, i64 %_28, i32 0
  %.in = select i1 %_3.i.i58, i64* %25, i64* %24
  %26 = load i64, i64* %.in, align 8
  %_34 = sub i64 %26, %_17
  %27 = getelementptr inbounds i64, i64* %12, i64 2
  %28 = bitcast i64* %27 to i32*
  %_40 = load i32, i32* %28, align 8
  %29 = icmp ne i32 %_40, 0
  %. = zext i1 %29 to i32
  %30 = getelementptr inbounds %LineRow, %LineRow* %13, i64 0, i32 3
  %_43 = load i32, i32* %30, align 4
  %31 = icmp ne i32 %_43, 0
  %_42.sroa.0.0 = zext i1 %31 to i32
  store i64 %_28, i64* %5, align 8
  %_45.sroa.0.0..sroa_idx = bitcast %"core::option::Option<(u64, u64, Location)>"* %0 to i64*
  store i64 %_17, i64* %_45.sroa.0.0..sroa_idx, align 8
  %_45.sroa.4.0..sroa_idx40 = getelementptr inbounds %"core::option::Option<(u64, u64, Location)>", %"core::option::Option<(u64, u64, Location)>"* %0, i64 0, i32 0, i64 2
  %32 = bitcast i32* %_45.sroa.4.0..sroa_idx40 to i64*
  store i64 %_34, i64* %32, align 8
  %_45.sroa.5.0..sroa_idx42 = getelementptr inbounds %"core::option::Option<(u64, u64, Location)>", %"core::option::Option<(u64, u64, Location)>"* %0, i64 0, i32 0, i64 4
  %33 = bitcast i32* %_45.sroa.5.0..sroa_idx42 to i8**
  store i8* %.sroa.0.0.i, i8** %33, align 8
  %_45.sroa.6.0..sroa_idx44 = getelementptr inbounds %"core::option::Option<(u64, u64, Location)>", %"core::option::Option<(u64, u64, Location)>"* %0, i64 0, i32 0, i64 6
  %34 = bitcast i32* %_45.sroa.6.0..sroa_idx44 to i64*
  store i64 %.sroa.3.0.i, i64* %34, align 8
  %35 = getelementptr inbounds %"core::option::Option<(u64, u64, Location)>", %"core::option::Option<(u64, u64, Location)>"* %0, i64 0, i32 1
  store i32 %., i32* %35, align 8
  %36 = getelementptr inbounds %"core::option::Option<(u64, u64, Location)>", %"core::option::Option<(u64, u64, Location)>"* %0, i64 0, i32 2, i64 0
  store i32 %_40, i32* %36, align 4
  %37 = getelementptr inbounds %"core::option::Option<(u64, u64, Location)>", %"core::option::Option<(u64, u64, Location)>"* %0, i64 0, i32 2, i64 1
  store i32 %_42.sroa.0.0, i32* %37, align 8
  %38 = getelementptr inbounds %"core::option::Option<(u64, u64, Location)>", %"core::option::Option<(u64, u64, Location)>"* %0, i64 0, i32 2, i64 2
  store i32 %_43, i32* %38, align 4
  br label %bb27

bb26:                                             ; preds = %bb5, %bb9, %start, %bb11
  %39 = getelementptr inbounds %"core::option::Option<(u64, u64, Location)>", %"core::option::Option<(u64, u64, Location)>"* %0, i64 0, i32 1
  store i32 2, i32* %39, align 8
  br label %bb27

bb27:                                             ; preds = %bb26, %"_ZN4core6option15Option$LT$T$GT$3map17h4327f575b61a265aE.exit"
  ret void
}

; addr2line::path_push
; Function Attrs: nonlazybind uwtable
define void @_ZN9addr2line9path_push17hae821fe7351b2ab8E(%"alloc::string::String"* noalias nocapture align 8 dereferenceable(24) %path, [0 x i8]* noalias nocapture nonnull readonly align 1 %p.0, i64 %p.1) unnamed_addr #4 personality i32 (...)* @rust_eh_personality {
start:
  %_5.not.i.i.i.i.i = icmp eq i64 %p.1, 0
  br i1 %_5.not.i.i.i.i.i, label %bb8, label %_ZN9addr2line13has_unix_root17hbe85360c8432b5a1E.exit

_ZN9addr2line13has_unix_root17hbe85360c8432b5a1E.exit: ; preds = %start
  %0 = getelementptr [0 x i8], [0 x i8]* %p.0, i64 0, i64 0
  %rhsc.i = load i8, i8* %0, align 1, !alias.scope !65
  switch i8 %rhsc.i, label %bb2.i [
    i8 47, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i.i"
    i8 92, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i.i"
  ]

bb2.i:                                            ; preds = %_ZN9addr2line13has_unix_root17hbe85360c8432b5a1E.exit
  %_3.i.i.i.i.i.i = icmp ugt i64 %p.1, 1
  br i1 %_3.i.i.i.i.i.i, label %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i", label %bb8

"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i": ; preds = %bb2.i
  %1 = getelementptr inbounds [0 x i8], [0 x i8]* %p.0, i64 0, i64 1
  %b.i.i.i.i = load i8, i8* %1, align 1, !alias.scope !68
  %2 = icmp sgt i8 %b.i.i.i.i, -65
  br i1 %2, label %bb2.i.i.i, label %bb8

bb2.i.i.i:                                        ; preds = %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i"
  %_3.i.i.i1.i.i.i = icmp ugt i64 %p.1, 3
  br i1 %_3.i.i.i1.i.i.i, label %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit6.i.i.i", label %bb7.i2.i.i.i

bb7.i2.i.i.i:                                     ; preds = %bb2.i.i.i
  %3 = icmp eq i64 %p.1, 3
  br i1 %3, label %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit, label %bb8

"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit6.i.i.i": ; preds = %bb2.i.i.i
  %4 = getelementptr inbounds [0 x i8], [0 x i8]* %p.0, i64 0, i64 3
  %b.i3.i.i.i = load i8, i8* %4, align 1, !alias.scope !77
  %5 = icmp sgt i8 %b.i3.i.i.i, -65
  br i1 %5, label %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit, label %bb8

_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit: ; preds = %bb7.i2.i.i.i, %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit6.i.i.i"
  %bcmp.i.i.i.i.i.i.i = tail call i32 @bcmp(i8* noundef nonnull dereferenceable(2) %1, i8* noundef nonnull dereferenceable(2) getelementptr inbounds (<{ [2 x i8] }>, <{ [2 x i8] }>* @alloc356, i64 0, i32 0, i64 0), i64 2) #17, !alias.scope !80, !noalias !87
  %6 = icmp eq i32 %bcmp.i.i.i.i.i.i.i, 0
  br i1 %6, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i.i", label %bb8

bb8:                                              ; preds = %start, %bb2.i, %bb7.i2.i.i.i, %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i", %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit6.i.i.i", %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit
  %7 = bitcast %"alloc::string::String"* %path to [0 x i8]**
  %_5.idx.val2.i.i = load [0 x i8]*, [0 x i8]** %7, align 8, !alias.scope !90
  %_5.idx1.i.i = getelementptr %"alloc::string::String", %"alloc::string::String"* %path, i64 0, i32 0, i32 1
  %_5.idx1.val.i.i = load i64, i64* %_5.idx1.i.i, align 8, !alias.scope !97
  %_5.not.i.i.i.i.i8 = icmp eq i64 %_5.idx1.val.i.i, 0
  %8 = getelementptr [0 x i8], [0 x i8]* %_5.idx.val2.i.i, i64 0, i64 0
  br i1 %_5.not.i.i.i.i.i8, label %bb16, label %"_ZN4core3str21_$LT$impl$u20$str$GT$11starts_with17h320687375214b78fE.exit.i10"

"_ZN4core3str21_$LT$impl$u20$str$GT$11starts_with17h320687375214b78fE.exit.i10": ; preds = %bb8
  %9 = getelementptr [0 x i8], [0 x i8]* %_5.idx.val2.i.i, i64 0, i64 0
  %rhsc.i9 = load i8, i8* %9, align 1, !alias.scope !98
  %10 = icmp eq i8 %rhsc.i9, 92
  br i1 %10, label %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23.thread42, label %bb2.i12

bb2.i12:                                          ; preds = %"_ZN4core3str21_$LT$impl$u20$str$GT$11starts_with17h320687375214b78fE.exit.i10"
  %_3.i.i.i.i.i.i11 = icmp ugt i64 %_5.idx1.val.i.i, 1
  br i1 %_3.i.i.i.i.i.i11, label %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i14", label %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit"

"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i14": ; preds = %bb2.i12
  %11 = getelementptr inbounds [0 x i8], [0 x i8]* %_5.idx.val2.i.i, i64 0, i64 1
  %b.i.i.i.i13 = load i8, i8* %11, align 1, !alias.scope !101
  %12 = icmp sgt i8 %b.i.i.i.i13, -65
  br i1 %12, label %bb2.i.i.i16, label %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit"

bb2.i.i.i16:                                      ; preds = %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i14"
  %_3.i.i.i1.i.i.i15 = icmp ugt i64 %_5.idx1.val.i.i, 3
  br i1 %_3.i.i.i1.i.i.i15, label %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit6.i.i.i19", label %bb7.i2.i.i.i17

bb7.i2.i.i.i17:                                   ; preds = %bb2.i.i.i16
  %13 = icmp eq i64 %_5.idx1.val.i.i, 3
  br i1 %13, label %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23, label %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit"

"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit6.i.i.i19": ; preds = %bb2.i.i.i16
  %14 = getelementptr inbounds [0 x i8], [0 x i8]* %_5.idx.val2.i.i, i64 0, i64 3
  %b.i3.i.i.i18 = load i8, i8* %14, align 1, !alias.scope !108
  %15 = icmp sgt i8 %b.i3.i.i.i18, -65
  br i1 %15, label %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23, label %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit"

_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23: ; preds = %bb7.i2.i.i.i17, %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit6.i.i.i19"
  %bcmp.i.i.i.i.i.i.i20 = tail call i32 @bcmp(i8* noundef nonnull dereferenceable(2) %11, i8* noundef nonnull dereferenceable(2) getelementptr inbounds (<{ [2 x i8] }>, <{ [2 x i8] }>* @alloc356, i64 0, i32 0, i64 0), i64 2) #17, !alias.scope !111, !noalias !118
  %16 = icmp eq i32 %bcmp.i.i.i.i.i.i.i20, 0
  br i1 %16, label %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23.thread42, label %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit"

_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23.thread42: ; preds = %"_ZN4core3str21_$LT$impl$u20$str$GT$11starts_with17h320687375214b78fE.exit.i10", %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23
  br label %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit"

"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit": ; preds = %bb2.i12, %bb7.i2.i.i.i17, %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i14", %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit6.i.i.i19", %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23, %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23.thread42
  %.ph = phi i8 [ 47, %bb2.i12 ], [ 47, %bb7.i2.i.i.i17 ], [ 47, %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit.i.i.i14" ], [ 47, %"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE.exit6.i.i.i19" ], [ 47, %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23 ], [ 92, %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit23.thread42 ]
  %_20.i.i.i.i = add i64 %_5.idx1.val.i.i, -1
  %17 = getelementptr inbounds [0 x i8], [0 x i8]* %_5.idx.val2.i.i, i64 0, i64 %_20.i.i.i.i
  %rhsc = load i8, i8* %17, align 1
  %18 = icmp eq i8 %.ph, %rhsc
  br i1 %18, label %bb18, label %bb16

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i.i": ; preds = %_ZN9addr2line13has_unix_root17hbe85360c8432b5a1E.exit, %_ZN9addr2line13has_unix_root17hbe85360c8432b5a1E.exit, %_ZN9addr2line16has_windows_root17h265a9b679fe97408E.exit
  %19 = tail call i8* @__rust_alloc(i64 %p.1, i64 1) #17, !noalias !121
  %20 = icmp eq i8* %19, null
  br i1 %20, label %bb20.i.i.i.i.i.i.i.i.i.i.i, label %"_ZN47_$LT$str$u20$as$u20$alloc..string..ToString$GT$9to_string17h5384d3684260716eE.exit"

bb20.i.i.i.i.i.i.i.i.i.i.i:                       ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i.i"
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 %p.1, i64 1) #19, !noalias !121
  unreachable

"_ZN47_$LT$str$u20$as$u20$alloc..string..ToString$GT$9to_string17h5384d3684260716eE.exit": ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$8allocate17h6e88fff47ec614c6E.exit.i.i.i.i.i.i.i.i.i.i.i"
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %19, i8* nonnull align 1 %0, i64 %p.1, i1 false) #17, !noalias !148
  %.idx.i.i = getelementptr %"alloc::string::String", %"alloc::string::String"* %path, i64 0, i32 0, i32 0, i32 0
  %.idx.val.i.i = load i8*, i8** %.idx.i.i, align 8
  %.idx5.i.i = getelementptr %"alloc::string::String", %"alloc::string::String"* %path, i64 0, i32 0, i32 0, i32 1
  %.idx5.val.i.i = load i64, i64* %.idx5.i.i, align 8
  %_4.i.i.i.i.i = icmp eq i64 %.idx5.val.i.i, 0
  %.not.i.i.i.i = icmp eq i8* %.idx.val.i.i, null
  %or.cond.i.i.i.i = select i1 %_4.i.i.i.i.i, i1 true, i1 %.not.i.i.i.i
  br i1 %or.cond.i.i.i.i, label %bb22, label %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i"

"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i": ; preds = %"_ZN47_$LT$str$u20$as$u20$alloc..string..ToString$GT$9to_string17h5384d3684260716eE.exit"
  tail call void @__rust_dealloc(i8* nonnull %.idx.val.i.i, i64 %.idx5.val.i.i, i64 1) #17
  br label %bb22

bb22:                                             ; preds = %"_ZN63_$LT$alloc..alloc..Global$u20$as$u20$core..alloc..Allocator$GT$10deallocate17hea57586e89b5dd49E.exit.i.i.i.i", %"_ZN47_$LT$str$u20$as$u20$alloc..string..ToString$GT$9to_string17h5384d3684260716eE.exit"
  store i8* %19, i8** %.idx.i.i, align 8
  store i64 %p.1, i64* %.idx5.i.i, align 8
  %_8.sroa.6.0..sroa_idx33 = getelementptr inbounds %"alloc::string::String", %"alloc::string::String"* %path, i64 0, i32 0, i32 1
  store i64 %p.1, i64* %_8.sroa.6.0..sroa_idx33, align 8
  br label %bb20

bb20:                                             ; preds = %"_ZN84_$LT$alloc..string..String$u20$as$u20$core..ops..arith..AddAssign$LT$$RF$str$GT$$GT$10add_assign17h7978cfa1e7dfc545E.exit", %bb22
  ret void

bb18:                                             ; preds = %_ZN5alloc6string6String4push17h2732284d4570ed38E.exit, %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit"
  %_2.idx.val.i.i.i.i.i.i53 = phi i8* [ %_2.idx.val.i.i.i.i.i.i52, %_ZN5alloc6string6String4push17h2732284d4570ed38E.exit ], [ %8, %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit" ]
  %_5.i.i.i.i.i.i = phi i64 [ %28, %_ZN5alloc6string6String4push17h2732284d4570ed38E.exit ], [ %_5.idx1.val.i.i, %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit" ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !149)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !152)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !155)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !158)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !161)
  %self.idx.i.i.i.i.i.i.i = getelementptr %"alloc::string::String", %"alloc::string::String"* %path, i64 0, i32 0, i32 0, i32 1
  %self.idx.val.i.i.i.i.i.i.i = load i64, i64* %self.idx.i.i.i.i.i.i.i, align 8, !alias.scope !164, !noalias !169
  %21 = sub i64 %self.idx.val.i.i.i.i.i.i.i, %_5.i.i.i.i.i.i
  %22 = icmp ult i64 %21, %p.1
  br i1 %22, label %bb2.i.i.i.i.i.i.i25, label %"_ZN84_$LT$alloc..string..String$u20$as$u20$core..ops..arith..AddAssign$LT$$RF$str$GT$$GT$10add_assign17h7978cfa1e7dfc545E.exit"

bb2.i.i.i.i.i.i.i25:                              ; preds = %bb18
  %_4.i.i.i.i.i.i = getelementptr inbounds %"alloc::string::String", %"alloc::string::String"* %path, i64 0, i32 0, i32 0
; call alloc::raw_vec::RawVec<T,A>::reserve::do_reserve_and_handle
  tail call fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve21do_reserve_and_handle17hd4a8cd68ada581f5E"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_4.i.i.i.i.i.i, i64 %_5.i.i.i.i.i.i, i64 %p.1), !noalias !169
  %self.idx.val.pre.i.i.i.i.i = load i64, i64* %_5.idx1.i.i, align 8, !alias.scope !173, !noalias !169
  %_2.idx.i.i.i.i.i.i.phi.trans.insert = getelementptr inbounds %"alloc::string::String", %"alloc::string::String"* %path, i64 0, i32 0, i32 0, i32 0
  %_2.idx.val.i.i.i.i.i.i.pre = load i8*, i8** %_2.idx.i.i.i.i.i.i.phi.trans.insert, align 8, !alias.scope !174, !noalias !169
  br label %"_ZN84_$LT$alloc..string..String$u20$as$u20$core..ops..arith..AddAssign$LT$$RF$str$GT$$GT$10add_assign17h7978cfa1e7dfc545E.exit"

"_ZN84_$LT$alloc..string..String$u20$as$u20$core..ops..arith..AddAssign$LT$$RF$str$GT$$GT$10add_assign17h7978cfa1e7dfc545E.exit": ; preds = %bb18, %bb2.i.i.i.i.i.i.i25
  %_2.idx.val.i.i.i.i.i.i = phi i8* [ %_2.idx.val.i.i.i.i.i.i53, %bb18 ], [ %_2.idx.val.i.i.i.i.i.i.pre, %bb2.i.i.i.i.i.i.i25 ]
  %self.idx.val.i.i.i.i.i = phi i64 [ %_5.i.i.i.i.i.i, %bb18 ], [ %self.idx.val.pre.i.i.i.i.i, %bb2.i.i.i.i.i.i.i25 ]
  %23 = getelementptr [0 x i8], [0 x i8]* %p.0, i64 0, i64 0
  %24 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i.i, i64 %self.idx.val.i.i.i.i.i
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* nonnull align 1 %24, i8* nonnull align 1 %23, i64 %p.1, i1 false) #17, !noalias !173
  %25 = add i64 %self.idx.val.i.i.i.i.i, %p.1
  store i64 %25, i64* %_5.idx1.i.i, align 8, !alias.scope !173, !noalias !169
  br label %bb20

bb16:                                             ; preds = %bb8, %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit"
  %26 = phi i8 [ %.ph, %"_ZN4core3str21_$LT$impl$u20$str$GT$9ends_with17hd833f436f262d853E.exit" ], [ 47, %bb8 ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !177)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !180)
  %_6.idx.i.i = getelementptr %"alloc::string::String", %"alloc::string::String"* %path, i64 0, i32 0, i32 0, i32 1
  %_6.idx.val.i.i = load i64, i64* %_6.idx.i.i, align 8, !alias.scope !183
  %_3.i.i = icmp eq i64 %_5.idx1.val.i.i, %_6.idx.val.i.i
  br i1 %_3.i.i, label %bb2.i.i, label %_ZN5alloc6string6String4push17h2732284d4570ed38E.exit

bb2.i.i:                                          ; preds = %bb16
  %_6.i.i = getelementptr inbounds %"alloc::string::String", %"alloc::string::String"* %path, i64 0, i32 0, i32 0
; call alloc::raw_vec::RawVec<T,A>::reserve_for_push
  tail call fastcc void @"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$16reserve_for_push17hada22938690d550eE"({ i8*, i64 }* noalias nonnull align 8 dereferenceable(16) %_6.i.i, i64 %_5.idx1.val.i.i)
  %_13.pre.i.i = load i64, i64* %_5.idx1.i.i, align 8, !alias.scope !183
  %_2.idx.i.i.i.phi.trans.insert = getelementptr inbounds %"alloc::string::String", %"alloc::string::String"* %path, i64 0, i32 0, i32 0, i32 0
  %_2.idx.val.i.i.i.pre = load i8*, i8** %_2.idx.i.i.i.phi.trans.insert, align 8, !alias.scope !184
  br label %_ZN5alloc6string6String4push17h2732284d4570ed38E.exit

_ZN5alloc6string6String4push17h2732284d4570ed38E.exit: ; preds = %bb16, %bb2.i.i
  %_2.idx.val.i.i.i.i.i.i52 = phi i8* [ %_2.idx.val.i.i.i.pre, %bb2.i.i ], [ %8, %bb16 ]
  %_13.i.i = phi i64 [ %_13.pre.i.i, %bb2.i.i ], [ %_5.idx1.val.i.i, %bb16 ]
  %27 = getelementptr inbounds i8, i8* %_2.idx.val.i.i.i.i.i.i52, i64 %_13.i.i
  store i8 %26, i8* %27, align 1, !noalias !183
  %28 = add i64 %_13.i.i, 1
  store i64 %28, i64* %_5.idx1.i.i, align 8, !alias.scope !183
  br label %bb18
}

; addr2line::demangle
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind uwtable willreturn writeonly
define void @_ZN9addr2line8demangle17h49bae4bf26a6d93fE(%"core::option::Option<alloc::string::String>"* noalias nocapture sret(%"core::option::Option<alloc::string::String>") dereferenceable(24) %0, [0 x i8]* noalias nocapture nonnull readonly align 1 %name.0, i64 %name.1, i16 %language) unnamed_addr #5 {
start:
  %1 = getelementptr inbounds %"core::option::Option<alloc::string::String>", %"core::option::Option<alloc::string::String>"* %0, i64 0, i32 0
  store {}* null, {}** %1, align 8
  ret void
}

; addr2line::demangle_auto
; Function Attrs: nonlazybind uwtable
define void @_ZN9addr2line13demangle_auto17h65af1ec72603e294E(%"alloc::borrow::Cow<str>"* noalias nocapture sret(%"alloc::borrow::Cow<str>") dereferenceable(32) %0, %"alloc::borrow::Cow<str>"* noalias nocapture readonly dereferenceable(32) %name, i16 %1, i16 %2) unnamed_addr #4 personality i32 (...)* @rust_eh_personality {
start:
  %_17.sroa.0.0..sroa_idx = getelementptr inbounds %"alloc::borrow::Cow<str>", %"alloc::borrow::Cow<str>"* %name, i64 0, i32 0
  %_17.sroa.0.0.copyload = load i64, i64* %_17.sroa.0.0..sroa_idx, align 8
  %_17.sroa.5.0..sroa_idx45 = getelementptr inbounds %"alloc::borrow::Cow<str>", %"alloc::borrow::Cow<str>"* %name, i64 0, i32 1
  %_17.sroa.5.0..sroa_cast = bitcast [3 x i64]* %_17.sroa.5.0..sroa_idx45 to i8**
  %_17.sroa.5.0.copyload = load i8*, i8** %_17.sroa.5.0..sroa_cast, align 8
  %_17.sroa.6.0..sroa_idx50 = getelementptr inbounds %"alloc::borrow::Cow<str>", %"alloc::borrow::Cow<str>"* %name, i64 0, i32 1, i64 1
  %3 = bitcast i64* %_17.sroa.6.0..sroa_idx50 to <2 x i64>*
  %4 = load <2 x i64>, <2 x i64>* %3, align 8
  %_17.sroa.0.0..sroa_idx43 = getelementptr inbounds %"alloc::borrow::Cow<str>", %"alloc::borrow::Cow<str>"* %0, i64 0, i32 0
  store i64 %_17.sroa.0.0.copyload, i64* %_17.sroa.0.0..sroa_idx43, align 8, !alias.scope !187, !noalias !191
  %_17.sroa.5.0..sroa_idx47 = getelementptr inbounds %"alloc::borrow::Cow<str>", %"alloc::borrow::Cow<str>"* %0, i64 0, i32 1
  %_17.sroa.5.0..sroa_cast48 = bitcast [3 x i64]* %_17.sroa.5.0..sroa_idx47 to i8**
  store i8* %_17.sroa.5.0.copyload, i8** %_17.sroa.5.0..sroa_cast48, align 8, !alias.scope !187, !noalias !191
  %_17.sroa.6.0..sroa_idx52 = getelementptr inbounds %"alloc::borrow::Cow<str>", %"alloc::borrow::Cow<str>"* %0, i64 0, i32 1, i64 1
  %5 = bitcast i64* %_17.sroa.6.0..sroa_idx52 to <2 x i64>*
  store <2 x i64> %4, <2 x i64>* %5, align 8, !alias.scope !187, !noalias !191
  ret void
}

; Function Attrs: argmemonly mustprogress nofree nounwind willreturn
declare void @llvm.memcpy.p0i8.p0i8.i64(i8* noalias nocapture writeonly, i8* noalias nocapture readonly, i64, i1 immarg) #6

; Function Attrs: nonlazybind
declare i32 @rust_eh_personality(...) unnamed_addr #7

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.start.p0i8(i64 immarg, i8* nocapture) #8

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.end.p0i8(i64 immarg, i8* nocapture) #8

; alloc::raw_vec::capacity_overflow
; Function Attrs: noreturn nonlazybind uwtable
declare void @_ZN5alloc7raw_vec17capacity_overflow17hbdad83560505d524E() unnamed_addr #9

; alloc::alloc::handle_alloc_error
; Function Attrs: cold noreturn nounwind nonlazybind uwtable
declare void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64, i64) unnamed_addr #10

; Function Attrs: inaccessiblememonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.assume(i1 noundef) #11

; Function Attrs: nofree nounwind nonlazybind uwtable
declare noalias i8* @__rust_alloc(i64, i64) unnamed_addr #12

; Function Attrs: nounwind nonlazybind uwtable
declare void @__rust_dealloc(i8*, i64, i64) unnamed_addr #13

; Function Attrs: nounwind nonlazybind uwtable
declare i8* @__rust_realloc(i8*, i64, i64, i64) unnamed_addr #13

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i64, i1 } @llvm.uadd.with.overflow.i64(i64, i64) #14

; Function Attrs: argmemonly nofree nounwind nonlazybind readonly willreturn
declare i32 @bcmp(i8* nocapture, i8* nocapture, i64) local_unnamed_addr #15

; Function Attrs: inaccessiblememonly nofree nosync nounwind willreturn
declare void @llvm.experimental.noalias.scope.decl(metadata) #16

attributes #0 = { cold nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { noinline nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { noinline nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #3 = { nofree nosync nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #4 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #5 = { mustprogress nofree norecurse nosync nounwind nonlazybind uwtable willreturn writeonly "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #6 = { argmemonly mustprogress nofree nounwind willreturn }
attributes #7 = { nonlazybind "target-cpu"="x86-64" }
attributes #8 = { argmemonly mustprogress nofree nosync nounwind willreturn }
attributes #9 = { noreturn nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #10 = { cold noreturn nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #11 = { inaccessiblememonly mustprogress nofree nosync nounwind willreturn }
attributes #12 = { nofree nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #13 = { nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #14 = { mustprogress nofree nosync nounwind readnone speculatable willreturn }
attributes #15 = { argmemonly nofree nounwind nonlazybind readonly willreturn }
attributes #16 = { inaccessiblememonly nofree nosync nounwind willreturn }
attributes #17 = { nounwind }
attributes #18 = { noreturn }
attributes #19 = { noreturn nounwind }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
!2 = !{!3}
!3 = distinct !{!3, !4, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17h76094885f4f5aeaaE: %self"}
!4 = distinct !{!4, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17h76094885f4f5aeaaE"}
!5 = !{!6}
!6 = distinct !{!6, !4, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17h76094885f4f5aeaaE: argument 0"}
!7 = !{!6, !3}
!8 = !{!9}
!9 = distinct !{!9, !10, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE: argument 0"}
!10 = distinct !{!10, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE"}
!11 = !{i64 0, i64 2}
!12 = !{!13}
!13 = distinct !{!13, !14, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17h157d1aea5f998f24E: %self"}
!14 = distinct !{!14, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17h157d1aea5f998f24E"}
!15 = !{!16, !6, !3}
!16 = distinct !{!16, !14, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17h157d1aea5f998f24E: argument 0"}
!17 = !{!18}
!18 = distinct !{!18, !19, !"_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE: %result"}
!19 = distinct !{!19, !"_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE"}
!20 = !{}
!21 = !{!22, !3}
!22 = distinct !{!22, !23, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17h08709b17744a23b5E: %self"}
!23 = distinct !{!23, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17h08709b17744a23b5E"}
!24 = !{!25}
!25 = distinct !{!25, !26, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17h76094885f4f5aeaaE: %self"}
!26 = distinct !{!26, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17h76094885f4f5aeaaE"}
!27 = !{!28}
!28 = distinct !{!28, !26, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14grow_amortized17h76094885f4f5aeaaE: argument 0"}
!29 = !{!28, !25}
!30 = !{!31}
!31 = distinct !{!31, !32, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE: argument 0"}
!32 = distinct !{!32, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$14current_memory17hef81f77343e889feE"}
!33 = !{!34}
!34 = distinct !{!34, !35, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17h157d1aea5f998f24E: %self"}
!35 = distinct !{!35, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17h157d1aea5f998f24E"}
!36 = !{!37, !28, !25}
!37 = distinct !{!37, !35, !"_ZN79_$LT$core..result..Result$LT$T$C$E$GT$$u20$as$u20$core..ops..try_trait..Try$GT$6branch17h157d1aea5f998f24E: argument 0"}
!38 = !{!39}
!39 = distinct !{!39, !40, !"_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE: %result"}
!40 = distinct !{!40, !"_ZN5alloc7raw_vec14handle_reserve17h6aba3a3a9bc33f5eE"}
!41 = !{!42, !25}
!42 = distinct !{!42, !43, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17h08709b17744a23b5E: %self"}
!43 = distinct !{!43, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$15set_ptr_and_cap17h08709b17744a23b5E"}
!44 = !{!45}
!45 = distinct !{!45, !46, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17h75e28d5459c9ff09E: argument 0"}
!46 = distinct !{!46, !"_ZN153_$LT$core..result..Result$LT$T$C$F$GT$$u20$as$u20$core..ops..try_trait..FromResidual$LT$core..result..Result$LT$core..convert..Infallible$C$E$GT$$GT$$GT$13from_residual17h75e28d5459c9ff09E"}
!47 = !{i64 1, i64 0}
!48 = !{!49}
!49 = distinct !{!49, !50, !"_ZN4core6result19Result$LT$T$C$E$GT$7map_err17h68bc3d77e8e55cc0E: argument 0"}
!50 = distinct !{!50, !"_ZN4core6result19Result$LT$T$C$E$GT$7map_err17h68bc3d77e8e55cc0E"}
!51 = !{!52}
!52 = distinct !{!52, !50, !"_ZN4core6result19Result$LT$T$C$E$GT$7map_err17h68bc3d77e8e55cc0E: %op"}
!53 = !{!54, !56, !58, !60, !62}
!54 = distinct !{!54, !55, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6as_ptr17he5e3c14d4e310f91E: %self"}
!55 = distinct !{!55, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6as_ptr17he5e3c14d4e310f91E"}
!56 = distinct !{!56, !57, !"_ZN65_$LT$alloc..string..String$u20$as$u20$core..ops..deref..Deref$GT$5deref17ha3c940e2c463d27fE: %self"}
!57 = distinct !{!57, !"_ZN65_$LT$alloc..string..String$u20$as$u20$core..ops..deref..Deref$GT$5deref17ha3c940e2c463d27fE"}
!58 = distinct !{!58, !59, !"_ZN5alloc6string6String6as_str17h6172904621f5de02E: %self"}
!59 = distinct !{!59, !"_ZN5alloc6string6String6as_str17h6172904621f5de02E"}
!60 = distinct !{!60, !61, !"_ZN4core3ops8function6FnOnce9call_once17h6bb9712de7a201f3E: argument 0"}
!61 = distinct !{!61, !"_ZN4core3ops8function6FnOnce9call_once17h6bb9712de7a201f3E"}
!62 = distinct !{!62, !63, !"_ZN4core6option15Option$LT$T$GT$3map17h4327f575b61a265aE: argument 0"}
!63 = distinct !{!63, !"_ZN4core6option15Option$LT$T$GT$3map17h4327f575b61a265aE"}
!64 = !{!56, !58, !60, !62}
!65 = !{!66}
!66 = distinct !{!66, !67, !"_ZN9addr2line13has_unix_root17hbe85360c8432b5a1E: %p.0"}
!67 = distinct !{!67, !"_ZN9addr2line13has_unix_root17hbe85360c8432b5a1E"}
!68 = !{!69, !71, !73, !75}
!69 = distinct !{!69, !70, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE: %self.0"}
!70 = distinct !{!70, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE"}
!71 = distinct !{!71, !72, !"_ZN4core3str6traits108_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..Range$LT$usize$GT$$GT$3get17h76c571b034054f8eE: %slice.0"}
!72 = distinct !{!72, !"_ZN4core3str6traits108_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..Range$LT$usize$GT$$GT$3get17h76c571b034054f8eE"}
!73 = distinct !{!73, !74, !"_ZN4core3str21_$LT$impl$u20$str$GT$3get17h956c3754c023ababE: %self.0"}
!74 = distinct !{!74, !"_ZN4core3str21_$LT$impl$u20$str$GT$3get17h956c3754c023ababE"}
!75 = distinct !{!75, !76, !"_ZN9addr2line16has_windows_root17h265a9b679fe97408E: %p.0"}
!76 = distinct !{!76, !"_ZN9addr2line16has_windows_root17h265a9b679fe97408E"}
!77 = !{!78, !71, !73, !75}
!78 = distinct !{!78, !79, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE: %self.0"}
!79 = distinct !{!79, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE"}
!80 = !{!81, !83, !84, !86}
!81 = distinct !{!81, !82, !"_ZN73_$LT$$u5b$A$u5d$$u20$as$u20$core..slice..cmp..SlicePartialEq$LT$B$GT$$GT$5equal17h82a2ebb18f1b71a7E: %self.0"}
!82 = distinct !{!82, !"_ZN73_$LT$$u5b$A$u5d$$u20$as$u20$core..slice..cmp..SlicePartialEq$LT$B$GT$$GT$5equal17h82a2ebb18f1b71a7E"}
!83 = distinct !{!83, !82, !"_ZN73_$LT$$u5b$A$u5d$$u20$as$u20$core..slice..cmp..SlicePartialEq$LT$B$GT$$GT$5equal17h82a2ebb18f1b71a7E: %other.0"}
!84 = distinct !{!84, !85, !"_ZN4core5slice3cmp81_$LT$impl$u20$core..cmp..PartialEq$LT$$u5b$B$u5d$$GT$$u20$for$u20$$u5b$A$u5d$$GT$2eq17h990a64787ff1997bE: %self.0"}
!85 = distinct !{!85, !"_ZN4core5slice3cmp81_$LT$impl$u20$core..cmp..PartialEq$LT$$u5b$B$u5d$$GT$$u20$for$u20$$u5b$A$u5d$$GT$2eq17h990a64787ff1997bE"}
!86 = distinct !{!86, !85, !"_ZN4core5slice3cmp81_$LT$impl$u20$core..cmp..PartialEq$LT$$u5b$B$u5d$$GT$$u20$for$u20$$u5b$A$u5d$$GT$2eq17h990a64787ff1997bE: %other.0"}
!87 = !{!88}
!88 = distinct !{!88, !89, !"_ZN70_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..cmp..PartialEq$GT$2eq17he2c2a31a77c72a50E: %self"}
!89 = distinct !{!89, !"_ZN70_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..cmp..PartialEq$GT$2eq17he2c2a31a77c72a50E"}
!90 = !{!91, !93, !95}
!91 = distinct !{!91, !92, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6as_ptr17he5e3c14d4e310f91E: %self"}
!92 = distinct !{!92, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$6as_ptr17he5e3c14d4e310f91E"}
!93 = distinct !{!93, !94, !"_ZN65_$LT$alloc..string..String$u20$as$u20$core..ops..deref..Deref$GT$5deref17ha3c940e2c463d27fE: %self"}
!94 = distinct !{!94, !"_ZN65_$LT$alloc..string..String$u20$as$u20$core..ops..deref..Deref$GT$5deref17ha3c940e2c463d27fE"}
!95 = distinct !{!95, !96, !"_ZN5alloc6string6String6as_str17h6172904621f5de02E: %self"}
!96 = distinct !{!96, !"_ZN5alloc6string6String6as_str17h6172904621f5de02E"}
!97 = !{!93, !95}
!98 = !{!99}
!99 = distinct !{!99, !100, !"_ZN9addr2line16has_windows_root17h265a9b679fe97408E: %p.0"}
!100 = distinct !{!100, !"_ZN9addr2line16has_windows_root17h265a9b679fe97408E"}
!101 = !{!102, !104, !106, !99}
!102 = distinct !{!102, !103, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE: %self.0"}
!103 = distinct !{!103, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE"}
!104 = distinct !{!104, !105, !"_ZN4core3str6traits108_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..Range$LT$usize$GT$$GT$3get17h76c571b034054f8eE: %slice.0"}
!105 = distinct !{!105, !"_ZN4core3str6traits108_$LT$impl$u20$core..slice..index..SliceIndex$LT$str$GT$$u20$for$u20$core..ops..range..Range$LT$usize$GT$$GT$3get17h76c571b034054f8eE"}
!106 = distinct !{!106, !107, !"_ZN4core3str21_$LT$impl$u20$str$GT$3get17h956c3754c023ababE: %self.0"}
!107 = distinct !{!107, !"_ZN4core3str21_$LT$impl$u20$str$GT$3get17h956c3754c023ababE"}
!108 = !{!109, !104, !106, !99}
!109 = distinct !{!109, !110, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE: %self.0"}
!110 = distinct !{!110, !"_ZN4core3str21_$LT$impl$u20$str$GT$16is_char_boundary17hb758182025112b0cE"}
!111 = !{!112, !114, !115, !117}
!112 = distinct !{!112, !113, !"_ZN73_$LT$$u5b$A$u5d$$u20$as$u20$core..slice..cmp..SlicePartialEq$LT$B$GT$$GT$5equal17h82a2ebb18f1b71a7E: %self.0"}
!113 = distinct !{!113, !"_ZN73_$LT$$u5b$A$u5d$$u20$as$u20$core..slice..cmp..SlicePartialEq$LT$B$GT$$GT$5equal17h82a2ebb18f1b71a7E"}
!114 = distinct !{!114, !113, !"_ZN73_$LT$$u5b$A$u5d$$u20$as$u20$core..slice..cmp..SlicePartialEq$LT$B$GT$$GT$5equal17h82a2ebb18f1b71a7E: %other.0"}
!115 = distinct !{!115, !116, !"_ZN4core5slice3cmp81_$LT$impl$u20$core..cmp..PartialEq$LT$$u5b$B$u5d$$GT$$u20$for$u20$$u5b$A$u5d$$GT$2eq17h990a64787ff1997bE: %self.0"}
!116 = distinct !{!116, !"_ZN4core5slice3cmp81_$LT$impl$u20$core..cmp..PartialEq$LT$$u5b$B$u5d$$GT$$u20$for$u20$$u5b$A$u5d$$GT$2eq17h990a64787ff1997bE"}
!117 = distinct !{!117, !116, !"_ZN4core5slice3cmp81_$LT$impl$u20$core..cmp..PartialEq$LT$$u5b$B$u5d$$GT$$u20$for$u20$$u5b$A$u5d$$GT$2eq17h990a64787ff1997bE: %other.0"}
!118 = !{!119}
!119 = distinct !{!119, !120, !"_ZN70_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..cmp..PartialEq$GT$2eq17he2c2a31a77c72a50E: %self"}
!120 = distinct !{!120, !"_ZN70_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..cmp..PartialEq$GT$2eq17he2c2a31a77c72a50E"}
!121 = !{!122, !124, !126, !127, !129, !130, !132, !133, !135, !136, !138, !139, !141, !142, !144, !145, !147}
!122 = distinct !{!122, !123, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17h85ed853b3561acbeE: argument 0"}
!123 = distinct !{!123, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$16with_capacity_in17h85ed853b3561acbeE"}
!124 = distinct !{!124, !125, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hc0996a8a6b37c65eE: %v"}
!125 = distinct !{!125, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hc0996a8a6b37c65eE"}
!126 = distinct !{!126, !125, !"_ZN52_$LT$T$u20$as$u20$alloc..slice..hack..ConvertVec$GT$6to_vec17hc0996a8a6b37c65eE: %s.0"}
!127 = distinct !{!127, !128, !"_ZN5alloc5slice4hack6to_vec17h728f109fce30448eE: argument 0"}
!128 = distinct !{!128, !"_ZN5alloc5slice4hack6to_vec17h728f109fce30448eE"}
!129 = distinct !{!129, !128, !"_ZN5alloc5slice4hack6to_vec17h728f109fce30448eE: %s.0"}
!130 = distinct !{!130, !131, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h7336fcae3b3803d5E: argument 0"}
!131 = distinct !{!131, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h7336fcae3b3803d5E"}
!132 = distinct !{!132, !131, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$9to_vec_in17h7336fcae3b3803d5E: %self.0"}
!133 = distinct !{!133, !134, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6to_vec17hab97f813902045a4E: argument 0"}
!134 = distinct !{!134, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6to_vec17hab97f813902045a4E"}
!135 = distinct !{!135, !134, !"_ZN5alloc5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$6to_vec17hab97f813902045a4E: %self.0"}
!136 = distinct !{!136, !137, !"_ZN5alloc5slice64_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$$u5b$T$u5d$$GT$8to_owned17h7137f0b945779c5eE: argument 0"}
!137 = distinct !{!137, !"_ZN5alloc5slice64_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$$u5b$T$u5d$$GT$8to_owned17h7137f0b945779c5eE"}
!138 = distinct !{!138, !137, !"_ZN5alloc5slice64_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$$u5b$T$u5d$$GT$8to_owned17h7137f0b945779c5eE: %self.0"}
!139 = distinct !{!139, !140, !"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E: argument 0"}
!140 = distinct !{!140, !"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E"}
!141 = distinct !{!141, !140, !"_ZN5alloc3str56_$LT$impl$u20$alloc..borrow..ToOwned$u20$for$u20$str$GT$8to_owned17h7dcf0c3d20978f29E: %self.0"}
!142 = distinct !{!142, !143, !"_ZN76_$LT$alloc..string..String$u20$as$u20$core..convert..From$LT$$RF$str$GT$$GT$4from17h271f67f609f6389bE: argument 0"}
!143 = distinct !{!143, !"_ZN76_$LT$alloc..string..String$u20$as$u20$core..convert..From$LT$$RF$str$GT$$GT$4from17h271f67f609f6389bE"}
!144 = distinct !{!144, !143, !"_ZN76_$LT$alloc..string..String$u20$as$u20$core..convert..From$LT$$RF$str$GT$$GT$4from17h271f67f609f6389bE: %s.0"}
!145 = distinct !{!145, !146, !"_ZN47_$LT$str$u20$as$u20$alloc..string..ToString$GT$9to_string17h5384d3684260716eE: argument 0"}
!146 = distinct !{!146, !"_ZN47_$LT$str$u20$as$u20$alloc..string..ToString$GT$9to_string17h5384d3684260716eE"}
!147 = distinct !{!147, !146, !"_ZN47_$LT$str$u20$as$u20$alloc..string..ToString$GT$9to_string17h5384d3684260716eE: %self.0"}
!148 = !{!124, !127, !130, !133, !136, !139, !142, !145}
!149 = !{!150}
!150 = distinct !{!150, !151, !"_ZN84_$LT$alloc..string..String$u20$as$u20$core..ops..arith..AddAssign$LT$$RF$str$GT$$GT$10add_assign17h7978cfa1e7dfc545E: %self"}
!151 = distinct !{!151, !"_ZN84_$LT$alloc..string..String$u20$as$u20$core..ops..arith..AddAssign$LT$$RF$str$GT$$GT$10add_assign17h7978cfa1e7dfc545E"}
!152 = !{!153}
!153 = distinct !{!153, !154, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %self"}
!154 = distinct !{!154, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E"}
!155 = !{!156}
!156 = distinct !{!156, !157, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17haa51461604abd7fbE: %self"}
!157 = distinct !{!157, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17haa51461604abd7fbE"}
!158 = !{!159}
!159 = distinct !{!159, !160, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h01ea1ca1bafc893cE: %self"}
!160 = distinct !{!160, !"_ZN132_$LT$alloc..vec..Vec$LT$T$C$A$GT$$u20$as$u20$alloc..vec..spec_extend..SpecExtend$LT$$RF$T$C$core..slice..iter..Iter$LT$T$GT$$GT$$GT$11spec_extend17h01ea1ca1bafc893cE"}
!161 = !{!162}
!162 = distinct !{!162, !163, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h88ea0675e5817815E: %self"}
!163 = distinct !{!163, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$15append_elements17h88ea0675e5817815E"}
!164 = !{!165, !167, !162, !159, !156, !153, !150}
!165 = distinct !{!165, !166, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17h8f1222fbaf598c5fE: %self"}
!166 = distinct !{!166, !"_ZN5alloc7raw_vec19RawVec$LT$T$C$A$GT$7reserve17h8f1222fbaf598c5fE"}
!167 = distinct !{!167, !168, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17he97c30fc37060d53E: %self"}
!168 = distinct !{!168, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$7reserve17he97c30fc37060d53E"}
!169 = !{!170, !171, !172}
!170 = distinct !{!170, !157, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$17extend_from_slice17haa51461604abd7fbE: %other.0"}
!171 = distinct !{!171, !154, !"_ZN5alloc6string6String8push_str17hbf89b66285b7fb55E: %string.0"}
!172 = distinct !{!172, !151, !"_ZN84_$LT$alloc..string..String$u20$as$u20$core..ops..arith..AddAssign$LT$$RF$str$GT$$GT$10add_assign17h7978cfa1e7dfc545E: %other.0"}
!173 = !{!162, !159, !156, !153, !150}
!174 = !{!175, !162, !159, !156, !153, !150}
!175 = distinct !{!175, !176, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17hf3c37d8532ae1480E: %self"}
!176 = distinct !{!176, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17hf3c37d8532ae1480E"}
!177 = !{!178}
!178 = distinct !{!178, !179, !"_ZN5alloc6string6String4push17h2732284d4570ed38E: %self"}
!179 = distinct !{!179, !"_ZN5alloc6string6String4push17h2732284d4570ed38E"}
!180 = !{!181}
!181 = distinct !{!181, !182, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$4push17h61b8be62966eb89bE: %self"}
!182 = distinct !{!182, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$4push17h61b8be62966eb89bE"}
!183 = !{!181, !178}
!184 = !{!185, !181, !178}
!185 = distinct !{!185, !186, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17hf3c37d8532ae1480E: %self"}
!186 = distinct !{!186, !"_ZN5alloc3vec16Vec$LT$T$C$A$GT$10as_mut_ptr17hf3c37d8532ae1480E"}
!187 = !{!188, !190}
!188 = distinct !{!188, !189, !"_ZN4core6option15Option$LT$T$GT$9unwrap_or17h9d78b9a9e477dc95E: argument 0"}
!189 = distinct !{!189, !"_ZN4core6option15Option$LT$T$GT$9unwrap_or17h9d78b9a9e477dc95E"}
!190 = distinct !{!190, !189, !"_ZN4core6option15Option$LT$T$GT$9unwrap_or17h9d78b9a9e477dc95E: %default"}
!191 = !{!192}
!192 = distinct !{!192, !189, !"_ZN4core6option15Option$LT$T$GT$9unwrap_or17h9d78b9a9e477dc95E: %self"}
