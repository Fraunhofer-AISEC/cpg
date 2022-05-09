; ModuleID = 'panic_unwind.032a371e-cgu.0'
source_filename = "panic_unwind.032a371e-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"unwind::libunwind::_Unwind_Context" = type { [0 x i8] }
%"real_imp::Exception" = type { %"unwind::libunwind::_Unwind_Exception", { {}*, [3 x i64]* } }
%"unwind::libunwind::_Unwind_Exception" = type { i64, void (i32, %"unwind::libunwind::_Unwind_Exception"*)*, [6 x i64] }
%"dwarf::eh::EHContext" = type { i64, i64, { {}*, [3 x i64]* }, { {}*, [3 x i64]* } }

@vtable.0 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, i8*, i8*, [0 x i8] }> <{ i8* bitcast (void (i64**)* @"_ZN4core3ptr88drop_in_place$LT$panic_unwind..real_imp..find_eh_action..$u7b$$u7b$closure$u7d$$u7d$$GT$17h1ab81f62d198e282E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i64 (i64**)* @"_ZN4core3ops8function6FnOnce40call_once$u7b$$u7b$vtable.shim$u7d$$u7d$17h54c15041f8d1471aE" to i8*), i8* bitcast (i64 (i64**)* @"_ZN12panic_unwind8real_imp14find_eh_action28_$u7b$$u7b$closure$u7d$$u7d$17h3f99be3bad287315E" to i8*), i8* bitcast (i64 (i64**)* @"_ZN12panic_unwind8real_imp14find_eh_action28_$u7b$$u7b$closure$u7d$$u7d$17h3f99be3bad287315E" to i8*), [0 x i8] zeroinitializer }>, align 8
@vtable.1 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, i8*, i8*, [0 x i8] }> <{ i8* bitcast (void (i64**)* @"_ZN4core3ptr88drop_in_place$LT$panic_unwind..real_imp..find_eh_action..$u7b$$u7b$closure$u7d$$u7d$$GT$17h1ab81f62d198e282E" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i64 (i64**)* @"_ZN4core3ops8function6FnOnce40call_once$u7b$$u7b$vtable.shim$u7d$$u7d$17h6b9176d2110a7b70E" to i8*), i8* bitcast (i64 (i64**)* @"_ZN12panic_unwind8real_imp14find_eh_action28_$u7b$$u7b$closure$u7d$$u7d$17hb1eb2c952be3528cE" to i8*), i8* bitcast (i64 (i64**)* @"_ZN12panic_unwind8real_imp14find_eh_action28_$u7b$$u7b$closure$u7d$$u7d$17hb1eb2c952be3528cE" to i8*), [0 x i8] zeroinitializer }>, align 8
@switch.table.rust_eh_personality = private unnamed_addr constant [4 x i32] [i32 8, i32 8, i32 6, i32 3], align 4

; core::ops::function::FnOnce::call_once{{vtable.shim}}
; Function Attrs: inlinehint nounwind nonlazybind uwtable
define internal i64 @"_ZN4core3ops8function6FnOnce40call_once$u7b$$u7b$vtable.shim$u7d$$u7d$17h54c15041f8d1471aE"(i64** nocapture readonly %_1) unnamed_addr #0 personality i32 (i32, i32, i64, %"unwind::libunwind::_Unwind_Exception"*, %"unwind::libunwind::_Unwind_Context"*)* @rust_eh_personality {
start:
  %0 = bitcast i64** %_1 to %"unwind::libunwind::_Unwind_Context"***
  %1 = load %"unwind::libunwind::_Unwind_Context"**, %"unwind::libunwind::_Unwind_Context"*** %0, align 8, !nonnull !2
  tail call void @llvm.experimental.noalias.scope.decl(metadata !3)
  %_2.i.i = load %"unwind::libunwind::_Unwind_Context"*, %"unwind::libunwind::_Unwind_Context"** %1, align 8, !alias.scope !3, !noalias !6
  %2 = tail call i64 @_Unwind_GetTextRelBase(%"unwind::libunwind::_Unwind_Context"* %_2.i.i) #12, !noalias !9
  ret i64 %2
}

; core::ops::function::FnOnce::call_once{{vtable.shim}}
; Function Attrs: inlinehint nounwind nonlazybind uwtable
define internal i64 @"_ZN4core3ops8function6FnOnce40call_once$u7b$$u7b$vtable.shim$u7d$$u7d$17h6b9176d2110a7b70E"(i64** nocapture readonly %_1) unnamed_addr #0 personality i32 (i32, i32, i64, %"unwind::libunwind::_Unwind_Exception"*, %"unwind::libunwind::_Unwind_Context"*)* @rust_eh_personality {
start:
  %0 = bitcast i64** %_1 to %"unwind::libunwind::_Unwind_Context"***
  %1 = load %"unwind::libunwind::_Unwind_Context"**, %"unwind::libunwind::_Unwind_Context"*** %0, align 8, !nonnull !2
  tail call void @llvm.experimental.noalias.scope.decl(metadata !10)
  %_2.i.i = load %"unwind::libunwind::_Unwind_Context"*, %"unwind::libunwind::_Unwind_Context"** %1, align 8, !alias.scope !10, !noalias !13
  %2 = tail call i64 @_Unwind_GetDataRelBase(%"unwind::libunwind::_Unwind_Context"* %_2.i.i) #12, !noalias !16
  ret i64 %2
}

; core::ptr::drop_in_place<alloc::boxed::Box<panic_unwind::real_imp::Exception>>
; Function Attrs: nonlazybind uwtable
define internal fastcc void @"_ZN4core3ptr79drop_in_place$LT$alloc..boxed..Box$LT$panic_unwind..real_imp..Exception$GT$$GT$17hec105c82440a3d91E"(%"real_imp::Exception"** nocapture readonly %_1) unnamed_addr #1 personality i32 (i32, i32, i64, %"unwind::libunwind::_Unwind_Exception"*, %"unwind::libunwind::_Unwind_Context"*)* @rust_eh_personality {
start:
  %0 = load %"real_imp::Exception"*, %"real_imp::Exception"** %_1, align 8, !nonnull !2
  %1 = getelementptr inbounds %"real_imp::Exception", %"real_imp::Exception"* %0, i64 0, i32 1
  %2 = getelementptr inbounds { {}*, [3 x i64]* }, { {}*, [3 x i64]* }* %1, i64 0, i32 0
  %3 = load {}*, {}** %2, align 8, !nonnull !2
  %4 = getelementptr inbounds %"real_imp::Exception", %"real_imp::Exception"* %0, i64 0, i32 1, i32 1
  %5 = bitcast [3 x i64]** %4 to void ({}*)***
  %6 = load void ({}*)**, void ({}*)*** %5, align 8, !nonnull !2
  %7 = load void ({}*)*, void ({}*)** %6, align 8, !invariant.load !2, !nonnull !2
  invoke void %7({}* nonnull %3)
          to label %bb3.i.i unwind label %cleanup.i.i

bb3.i.i:                                          ; preds = %start
  %8 = bitcast { {}*, [3 x i64]* }* %1 to i8**
  %9 = load i8*, i8** %8, align 8, !nonnull !2
  %10 = bitcast [3 x i64]** %4 to i64**
  %11 = load i64*, i64** %10, align 8, !nonnull !2
  tail call void @llvm.experimental.noalias.scope.decl(metadata !17)
  %12 = getelementptr inbounds i64, i64* %11, i64 1
  %13 = load i64, i64* %12, align 8, !invariant.load !2, !alias.scope !17
  %14 = icmp eq i64 %13, 0
  br i1 %14, label %bb3, label %bb2.i.i.i.i

bb2.i.i.i.i:                                      ; preds = %bb3.i.i
  %15 = getelementptr inbounds i64, i64* %11, i64 2
  %16 = load i64, i64* %15, align 8, !range !20, !invariant.load !2, !alias.scope !17
  tail call void @__rust_dealloc(i8* nonnull %9, i64 %13, i64 %16) #12, !noalias !17
  br label %bb3

cleanup.i.i:                                      ; preds = %start
  %17 = landingpad { i8*, i32 }
          cleanup
  %18 = bitcast { {}*, [3 x i64]* }* %1 to i8**
  %19 = load i8*, i8** %18, align 8, !nonnull !2
  %20 = bitcast [3 x i64]** %4 to i64**
  %21 = load i64*, i64** %20, align 8, !nonnull !2
  tail call void @llvm.experimental.noalias.scope.decl(metadata !21)
  %22 = getelementptr inbounds i64, i64* %21, i64 1
  %23 = load i64, i64* %22, align 8, !invariant.load !2, !alias.scope !21
  %24 = icmp eq i64 %23, 0
  br i1 %24, label %cleanup.body, label %bb2.i.i4.i.i

bb2.i.i4.i.i:                                     ; preds = %cleanup.i.i
  %25 = getelementptr inbounds i64, i64* %21, i64 2
  %26 = load i64, i64* %25, align 8, !range !20, !invariant.load !2, !alias.scope !21
  tail call void @__rust_dealloc(i8* nonnull %19, i64 %23, i64 %26) #12, !noalias !21
  br label %cleanup.body

bb3:                                              ; preds = %bb2.i.i.i.i, %bb3.i.i
  %27 = bitcast %"real_imp::Exception"** %_1 to i8**
  %28 = load i8*, i8** %27, align 8, !nonnull !2
  tail call void @__rust_dealloc(i8* nonnull %28, i64 80, i64 8) #12
  ret void

cleanup.body:                                     ; preds = %cleanup.i.i, %bb2.i.i4.i.i
  %29 = bitcast %"real_imp::Exception"** %_1 to i8**
  %30 = load i8*, i8** %29, align 8, !nonnull !2
  tail call void @__rust_dealloc(i8* nonnull %30, i64 80, i64 8) #12
  resume { i8*, i32 } %17
}

; core::ptr::drop_in_place<panic_unwind::real_imp::find_eh_action::{{closure}}>
; Function Attrs: inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define internal void @"_ZN4core3ptr88drop_in_place$LT$panic_unwind..real_imp..find_eh_action..$u7b$$u7b$closure$u7d$$u7d$$GT$17h1ab81f62d198e282E"(i64** nocapture readnone %_1) unnamed_addr #2 {
start:
  ret void
}

; panic_unwind::dwarf::eh::read_encoded_pointer
; Function Attrs: nonlazybind uwtable
define internal fastcc { i64, i64 } @_ZN12panic_unwind5dwarf2eh20read_encoded_pointer17hfc4ac8313c3f402dE(i8** noalias align 8 dereferenceable(8) %reader, %"dwarf::eh::EHContext"* noalias nocapture readonly align 8 dereferenceable(48) %context, i8 %encoding) unnamed_addr #1 {
start:
  switch i8 %encoding, label %bb4 [
    i8 -1, label %bb50
    i8 80, label %bb8
  ]

bb50:                                             ; preds = %bb44, %bb43, %bb36, %bb32, %bb4, %start, %bb8
  %.sroa.8.0 = phi i64 [ %result.i, %bb8 ], [ undef, %start ], [ undef, %bb4 ], [ undef, %bb32 ], [ undef, %bb36 ], [ %55, %bb43 ], [ %_51, %bb44 ]
  %.sroa.0.0 = phi i64 [ 0, %bb8 ], [ 1, %start ], [ 1, %bb4 ], [ 1, %bb32 ], [ 1, %bb36 ], [ 0, %bb43 ], [ 0, %bb44 ]
  %0 = insertvalue { i64, i64 } undef, i64 %.sroa.0.0, 0
  %1 = insertvalue { i64, i64 } %0, i64 %.sroa.8.0, 1
  ret { i64, i64 } %1

bb4:                                              ; preds = %start
  %_21 = and i8 %encoding, 15
  switch i8 %_21, label %bb50 [
    i8 0, label %bb14
    i8 1, label %bb16
    i8 2, label %bb18
    i8 3, label %bb20
    i8 4, label %bb22
    i8 9, label %bb24
    i8 10, label %bb26
    i8 11, label %bb28
    i8 12, label %bb30
  ]

bb8:                                              ; preds = %start
  %_12 = load i8*, i8** %reader, align 8
  %_11 = ptrtoint i8* %_12 to i64
  %_6.i = add i64 %_11, 7
  %_5.i = and i64 %_6.i, -8
  %2 = inttoptr i64 %_5.i to i8*
  store i8* %2, i8** %reader, align 8
  tail call void @llvm.experimental.noalias.scope.decl(metadata !24)
  %3 = inttoptr i64 %_5.i to i64*
  %result.i = load i64, i64* %3, align 8, !noalias !24
  %4 = getelementptr inbounds i8, i8* %2, i64 8
  store i8* %4, i8** %reader, align 8, !alias.scope !24
  br label %bb50

bb14:                                             ; preds = %bb4
  tail call void @llvm.experimental.noalias.scope.decl(metadata !27)
  %_3.i16 = load i8*, i8** %reader, align 8, !alias.scope !27
  %5 = bitcast i8* %_3.i16 to i64*
  %result.i17 = load i64, i64* %5, align 1, !noalias !27
  %6 = getelementptr inbounds i8, i8* %_3.i16, i64 8
  store i8* %6, i8** %reader, align 8, !alias.scope !27
  br label %bb32

bb16:                                             ; preds = %bb4
  tail call void @llvm.experimental.noalias.scope.decl(metadata !30)
  %self.promoted.i = load i8*, i8** %reader, align 8, !alias.scope !33
  br label %bb1.i

bb1.i:                                            ; preds = %bb1.i, %bb16
  %7 = phi i8* [ %self.promoted.i, %bb16 ], [ %8, %bb1.i ]
  %shift.0.i = phi i64 [ 0, %bb16 ], [ %11, %bb1.i ]
  %result.0.i = phi i64 [ 0, %bb16 ], [ %10, %bb1.i ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !36) #12
  %result.i.i = load i8, i8* %7, align 1, !noalias !33
  %8 = getelementptr inbounds i8, i8* %7, i64 1
  %_8.i = and i8 %result.i.i, 127
  %_7.i = zext i8 %_8.i to i64
  %9 = and i64 %shift.0.i, 63
  %_6.i18 = shl i64 %_7.i, %9
  %10 = or i64 %_6.i18, %result.0.i
  %11 = add i64 %shift.0.i, 7
  %12 = icmp sgt i8 %result.i.i, -1
  br i1 %12, label %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit, label %bb1.i

_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit: ; preds = %bb1.i
  store i8* %8, i8** %reader, align 8, !alias.scope !33
  br label %bb32

bb18:                                             ; preds = %bb4
  tail call void @llvm.experimental.noalias.scope.decl(metadata !37)
  %_3.i19 = load i8*, i8** %reader, align 8, !alias.scope !37
  %13 = bitcast i8* %_3.i19 to i16*
  %result.i20 = load i16, i16* %13, align 1, !noalias !37
  %14 = getelementptr inbounds i8, i8* %_3.i19, i64 2
  store i8* %14, i8** %reader, align 8, !alias.scope !37
  %15 = zext i16 %result.i20 to i64
  br label %bb32

bb20:                                             ; preds = %bb4
  tail call void @llvm.experimental.noalias.scope.decl(metadata !40)
  %_3.i21 = load i8*, i8** %reader, align 8, !alias.scope !40
  %16 = bitcast i8* %_3.i21 to i32*
  %result.i22 = load i32, i32* %16, align 1, !noalias !40
  %17 = getelementptr inbounds i8, i8* %_3.i21, i64 4
  store i8* %17, i8** %reader, align 8, !alias.scope !40
  %18 = zext i32 %result.i22 to i64
  br label %bb32

bb22:                                             ; preds = %bb4
  tail call void @llvm.experimental.noalias.scope.decl(metadata !43)
  %_3.i23 = load i8*, i8** %reader, align 8, !alias.scope !43
  %19 = bitcast i8* %_3.i23 to i64*
  %result.i24 = load i64, i64* %19, align 1, !noalias !43
  %20 = getelementptr inbounds i8, i8* %_3.i23, i64 8
  store i8* %20, i8** %reader, align 8, !alias.scope !43
  br label %bb32

bb24:                                             ; preds = %bb4
  tail call void @llvm.experimental.noalias.scope.decl(metadata !46)
  %self.promoted.i25 = load i8*, i8** %reader, align 8, !alias.scope !49
  br label %bb1.i31

bb1.i31:                                          ; preds = %bb1.i31, %bb24
  %21 = phi i8* [ %self.promoted.i25, %bb24 ], [ %22, %bb1.i31 ]
  %result.0.i26 = phi i64 [ 0, %bb24 ], [ %25, %bb1.i31 ]
  %shift.0.i27 = phi i32 [ 0, %bb24 ], [ %26, %bb1.i31 ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !52) #12
  %result.i.i28 = load i8, i8* %21, align 1, !noalias !49
  %22 = getelementptr inbounds i8, i8* %21, i64 1
  %_9.i = and i8 %result.i.i28, 127
  %_8.i29 = zext i8 %_9.i to i64
  %23 = and i32 %shift.0.i27, 63
  %24 = zext i32 %23 to i64
  %_7.i30 = shl i64 %_8.i29, %24
  %25 = or i64 %_7.i30, %result.0.i26
  %26 = add i32 %shift.0.i27, 7
  %27 = icmp sgt i8 %result.i.i28, -1
  br i1 %27, label %_ZN12panic_unwind5dwarf11DwarfReader12read_sleb12817h3b43dcffc535e765E.exit, label %bb1.i31

_ZN12panic_unwind5dwarf11DwarfReader12read_sleb12817h3b43dcffc535e765E.exit: ; preds = %bb1.i31
  store i8* %22, i8** %reader, align 8, !alias.scope !49
  %_15.i = icmp ult i32 %26, 64
  %_18.i = and i8 %result.i.i28, 64
  %_17.i = icmp ne i8 %_18.i, 0
  %_14.0.i = select i1 %_15.i, i1 %_17.i, i1 false
  %28 = and i32 %26, 63
  %29 = zext i32 %28 to i64
  %_20.i = shl nsw i64 -1, %29
  %30 = select i1 %_14.0.i, i64 %_20.i, i64 0
  %result.1.i = or i64 %25, %30
  br label %bb32

bb26:                                             ; preds = %bb4
  tail call void @llvm.experimental.noalias.scope.decl(metadata !53)
  %_3.i32 = load i8*, i8** %reader, align 8, !alias.scope !53
  %31 = bitcast i8* %_3.i32 to i16*
  %result.i33 = load i16, i16* %31, align 1, !noalias !53
  %32 = getelementptr inbounds i8, i8* %_3.i32, i64 2
  store i8* %32, i8** %reader, align 8, !alias.scope !53
  %33 = sext i16 %result.i33 to i64
  br label %bb32

bb28:                                             ; preds = %bb4
  tail call void @llvm.experimental.noalias.scope.decl(metadata !56)
  %_3.i34 = load i8*, i8** %reader, align 8, !alias.scope !56
  %34 = bitcast i8* %_3.i34 to i32*
  %result.i35 = load i32, i32* %34, align 1, !noalias !56
  %35 = getelementptr inbounds i8, i8* %_3.i34, i64 4
  store i8* %35, i8** %reader, align 8, !alias.scope !56
  %36 = sext i32 %result.i35 to i64
  br label %bb32

bb30:                                             ; preds = %bb4
  tail call void @llvm.experimental.noalias.scope.decl(metadata !59)
  %_3.i36 = load i8*, i8** %reader, align 8, !alias.scope !59
  %37 = bitcast i8* %_3.i36 to i64*
  %result.i37 = load i64, i64* %37, align 1, !noalias !59
  %38 = getelementptr inbounds i8, i8* %_3.i36, i64 8
  store i8* %38, i8** %reader, align 8, !alias.scope !59
  br label %bb32

bb32:                                             ; preds = %bb14, %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit, %bb18, %bb20, %bb22, %_ZN12panic_unwind5dwarf11DwarfReader12read_sleb12817h3b43dcffc535e765E.exit, %bb26, %bb28, %bb30
  %_43 = phi i8* [ %38, %bb30 ], [ %35, %bb28 ], [ %32, %bb26 ], [ %22, %_ZN12panic_unwind5dwarf11DwarfReader12read_sleb12817h3b43dcffc535e765E.exit ], [ %20, %bb22 ], [ %17, %bb20 ], [ %14, %bb18 ], [ %8, %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit ], [ %6, %bb14 ]
  %result.0 = phi i64 [ %result.i37, %bb30 ], [ %36, %bb28 ], [ %33, %bb26 ], [ %result.1.i, %_ZN12panic_unwind5dwarf11DwarfReader12read_sleb12817h3b43dcffc535e765E.exit ], [ %result.i24, %bb22 ], [ %18, %bb20 ], [ %15, %bb18 ], [ %10, %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit ], [ %result.i17, %bb14 ]
  %_41 = lshr i8 %encoding, 4
  %39 = and i8 %_41, 7
  switch i8 %39, label %bb50 [
    i8 0, label %bb43
    i8 1, label %bb35
    i8 4, label %bb36
    i8 2, label %bb39
    i8 3, label %bb41
  ]

bb35:                                             ; preds = %bb32
  %40 = ptrtoint i8* %_43 to i64
  br label %bb43

bb36:                                             ; preds = %bb32
  %41 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %context, i64 0, i32 1
  %_44 = load i64, i64* %41, align 8
  %42 = icmp eq i64 %_44, 0
  br i1 %42, label %bb50, label %bb43

bb39:                                             ; preds = %bb32
  %43 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %context, i64 0, i32 2, i32 0
  %_45.0 = load {}*, {}** %43, align 8, !nonnull !2
  %44 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %context, i64 0, i32 2, i32 1
  %45 = bitcast [3 x i64]** %44 to i64 ({}*)***
  %_45.115 = load i64 ({}*)**, i64 ({}*)*** %45, align 8, !nonnull !2
  %46 = getelementptr inbounds i64 ({}*)*, i64 ({}*)** %_45.115, i64 5
  %47 = load i64 ({}*)*, i64 ({}*)** %46, align 8, !invariant.load !2, !nonnull !2
  %48 = tail call i64 %47({}* nonnull align 1 %_45.0)
  br label %bb43

bb41:                                             ; preds = %bb32
  %49 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %context, i64 0, i32 3, i32 0
  %_47.0 = load {}*, {}** %49, align 8, !nonnull !2
  %50 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %context, i64 0, i32 3, i32 1
  %51 = bitcast [3 x i64]** %50 to i64 ({}*)***
  %_47.114 = load i64 ({}*)**, i64 ({}*)*** %51, align 8, !nonnull !2
  %52 = getelementptr inbounds i64 ({}*)*, i64 ({}*)** %_47.114, i64 5
  %53 = load i64 ({}*)*, i64 ({}*)** %52, align 8, !invariant.load !2, !nonnull !2
  %54 = tail call i64 %53({}* nonnull align 1 %_47.0)
  br label %bb43

bb43:                                             ; preds = %bb36, %bb32, %bb35, %bb39, %bb41
  %_40.0 = phi i64 [ %54, %bb41 ], [ %48, %bb39 ], [ %40, %bb35 ], [ 0, %bb32 ], [ %_44, %bb36 ]
  %55 = add i64 %_40.0, %result.0
  %56 = icmp sgt i8 %encoding, -1
  br i1 %56, label %bb50, label %bb44

bb44:                                             ; preds = %bb43
  %_52 = inttoptr i64 %55 to i64*
  %_51 = load i64, i64* %_52, align 8
  br label %bb50
}

; Function Attrs: nounwind nonlazybind uwtable
define { i64, i64 } @__rust_panic_cleanup(i8* %payload) unnamed_addr #3 personality i32 (i32, i32, i64, %"unwind::libunwind::_Unwind_Exception"*, %"unwind::libunwind::_Unwind_Context"*)* @rust_eh_personality {
start:
  %0 = bitcast i8* %payload to i64*
  %_7.i = load i64, i64* %0, align 8
  %_6.not.i = icmp eq i64 %_7.i, 5570770221508416340
  br i1 %_6.not.i, label %bb2, label %bb2.i

bb2.i:                                            ; preds = %start
  %exception.i = bitcast i8* %payload to %"unwind::libunwind::_Unwind_Exception"*
  tail call void @_Unwind_DeleteException(%"unwind::libunwind::_Unwind_Exception"* %exception.i) #12
  tail call void @__rust_foreign_exception() #13
  unreachable

bb2:                                              ; preds = %start
  %1 = getelementptr inbounds i8, i8* %payload, i64 64
  %2 = bitcast i8* %1 to {}**
  %_14.0.i = load {}*, {}** %2, align 8, !nonnull !2
  %3 = getelementptr inbounds i8, i8* %payload, i64 72
  %4 = bitcast i8* %3 to [3 x i64]**
  %_14.1.i = load [3 x i64]*, [3 x i64]** %4, align 8, !nonnull !2
  tail call void @__rust_dealloc(i8* nonnull %payload, i64 80, i64 8) #12
  %5 = ptrtoint {}* %_14.0.i to i64
  %.fca.0.insert = insertvalue { i64, i64 } undef, i64 %5, 0
  %6 = ptrtoint [3 x i64]* %_14.1.i to i64
  %.fca.1.insert = insertvalue { i64, i64 } %.fca.0.insert, i64 %6, 1
  ret { i64, i64 } %.fca.1.insert
}

; Function Attrs: nonlazybind uwtable
define i32 @__rust_start_panic({ {}*, [3 x i64]* }* nocapture readonly %payload) unnamed_addr #1 personality i32 (i32, i32, i64, %"unwind::libunwind::_Unwind_Exception"*, %"unwind::libunwind::_Unwind_Context"*)* @rust_eh_personality {
start:
  %0 = getelementptr inbounds { {}*, [3 x i64]* }, { {}*, [3 x i64]* }* %payload, i64 0, i32 0
  %_4.0 = load {}*, {}** %0, align 8, !nonnull !2
  %1 = getelementptr inbounds { {}*, [3 x i64]* }, { {}*, [3 x i64]* }* %payload, i64 0, i32 1
  %2 = bitcast [3 x i64]** %1 to { {}*, [3 x i64]* } ({}*)***
  %_4.11 = load { {}*, [3 x i64]* } ({}*)**, { {}*, [3 x i64]* } ({}*)*** %2, align 8, !nonnull !2
  %3 = getelementptr inbounds { {}*, [3 x i64]* } ({}*)*, { {}*, [3 x i64]* } ({}*)** %_4.11, i64 3
  %4 = load { {}*, [3 x i64]* } ({}*)*, { {}*, [3 x i64]* } ({}*)** %3, align 8, !invariant.load !2, !nonnull !2
  %5 = tail call { {}*, [3 x i64]* } %4({}* nonnull align 1 %_4.0)
  %_3.0 = extractvalue { {}*, [3 x i64]* } %5, 0
  %6 = icmp ne {}* %_3.0, null
  tail call void @llvm.assume(i1 %6) #12
  %7 = tail call dereferenceable_or_null(80) i8* @__rust_alloc(i64 80, i64 8) #12, !noalias !62
  %8 = icmp eq i8* %7, null
  br i1 %8, label %bb3.i.i.i, label %_ZN12panic_unwind8real_imp5panic17h9a3ac7f584fbd0bcE.exit

bb3.i.i.i:                                        ; preds = %start
; call alloc::alloc::handle_alloc_error
  tail call void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64 80, i64 8) #13, !noalias !62
  unreachable

_ZN12panic_unwind8real_imp5panic17h9a3ac7f584fbd0bcE.exit: ; preds = %start
  %payload.1 = extractvalue { {}*, [3 x i64]* } %5, 1
  %9 = bitcast i8* %7 to %"real_imp::Exception"*
  %_3.sroa.0.0..sroa_cast.i = bitcast i8* %7 to i64*
  store i64 5570770221508416340, i64* %_3.sroa.0.0..sroa_cast.i, align 8, !noalias !68
  %_3.sroa.4.0..sroa_idx.i = getelementptr inbounds i8, i8* %7, i64 8
  %_3.sroa.4.0..sroa_cast.i = bitcast i8* %_3.sroa.4.0..sroa_idx.i to void (i32, %"unwind::libunwind::_Unwind_Exception"*)**
  store void (i32, %"unwind::libunwind::_Unwind_Exception"*)* @_ZN12panic_unwind8real_imp5panic17exception_cleanup17hdff845272fcf3072E, void (i32, %"unwind::libunwind::_Unwind_Exception"*)** %_3.sroa.4.0..sroa_cast.i, align 8, !noalias !68
  %_3.sroa.5.0..sroa_raw_idx.i = getelementptr inbounds i8, i8* %7, i64 16
  tail call void @llvm.memset.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(48) %_3.sroa.5.0..sroa_raw_idx.i, i8 0, i64 48, i1 false), !noalias !68
  %_3.sroa.6.0..sroa_idx.i = getelementptr inbounds i8, i8* %7, i64 64
  %_3.sroa.6.0..sroa_cast.i = bitcast i8* %_3.sroa.6.0..sroa_idx.i to {}**
  store {}* %_3.0, {}** %_3.sroa.6.0..sroa_cast.i, align 8, !noalias !68
  %_3.sroa.7.0..sroa_idx.i = getelementptr inbounds i8, i8* %7, i64 72
  %_3.sroa.7.0..sroa_cast.i = bitcast i8* %_3.sroa.7.0..sroa_idx.i to [3 x i64]**
  store [3 x i64]* %payload.1, [3 x i64]** %_3.sroa.7.0..sroa_cast.i, align 8, !noalias !68
  %exception_param.i = getelementptr %"real_imp::Exception", %"real_imp::Exception"* %9, i64 0, i32 0
  %_13.i = tail call i32 @_Unwind_RaiseException(%"unwind::libunwind::_Unwind_Exception"* %exception_param.i), !range !69
  ret i32 %_13.i
}

; panic_unwind::real_imp::panic::exception_cleanup
; Function Attrs: noreturn nounwind nonlazybind uwtable
define internal void @_ZN12panic_unwind8real_imp5panic17exception_cleanup17hdff845272fcf3072E(i32 %_unwind_code, %"unwind::libunwind::_Unwind_Exception"* %exception) unnamed_addr #4 personality i32 (i32, i32, i64, %"unwind::libunwind::_Unwind_Exception"*, %"unwind::libunwind::_Unwind_Context"*)* @rust_eh_personality {
bb1:
  %_3 = alloca %"real_imp::Exception"*, align 8
  %0 = bitcast %"real_imp::Exception"** %_3 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %0)
  %1 = icmp ne %"unwind::libunwind::_Unwind_Exception"* %exception, null
  tail call void @llvm.assume(i1 %1) #12
  %2 = bitcast %"real_imp::Exception"** %_3 to %"unwind::libunwind::_Unwind_Exception"**
  store %"unwind::libunwind::_Unwind_Exception"* %exception, %"unwind::libunwind::_Unwind_Exception"** %2, align 8
; invoke core::ptr::drop_in_place<alloc::boxed::Box<panic_unwind::real_imp::Exception>>
  invoke fastcc void @"_ZN4core3ptr79drop_in_place$LT$alloc..boxed..Box$LT$panic_unwind..real_imp..Exception$GT$$GT$17hec105c82440a3d91E"(%"real_imp::Exception"** nonnull %_3)
          to label %bb2 unwind label %cleanup

cleanup:                                          ; preds = %bb1
  %3 = landingpad { i8*, i32 }
          cleanup
  tail call void @llvm.trap()
  unreachable

bb2:                                              ; preds = %bb1
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %0)
  tail call void @__rust_drop_panic() #13
  unreachable
}

; panic_unwind::real_imp::find_eh_action::{{closure}}
; Function Attrs: inlinehint nounwind nonlazybind uwtable
define internal i64 @"_ZN12panic_unwind8real_imp14find_eh_action28_$u7b$$u7b$closure$u7d$$u7d$17h3f99be3bad287315E"(i64** noalias nocapture readonly align 8 dereferenceable(8) %_1) unnamed_addr #0 {
start:
  %0 = bitcast i64** %_1 to %"unwind::libunwind::_Unwind_Context"***
  %1 = load %"unwind::libunwind::_Unwind_Context"**, %"unwind::libunwind::_Unwind_Context"*** %0, align 8, !nonnull !2
  %_2 = load %"unwind::libunwind::_Unwind_Context"*, %"unwind::libunwind::_Unwind_Context"** %1, align 8
  %2 = tail call i64 @_Unwind_GetTextRelBase(%"unwind::libunwind::_Unwind_Context"* %_2) #12
  ret i64 %2
}

; panic_unwind::real_imp::find_eh_action::{{closure}}
; Function Attrs: inlinehint nounwind nonlazybind uwtable
define internal i64 @"_ZN12panic_unwind8real_imp14find_eh_action28_$u7b$$u7b$closure$u7d$$u7d$17hb1eb2c952be3528cE"(i64** noalias nocapture readonly align 8 dereferenceable(8) %_1) unnamed_addr #0 {
start:
  %0 = bitcast i64** %_1 to %"unwind::libunwind::_Unwind_Context"***
  %1 = load %"unwind::libunwind::_Unwind_Context"**, %"unwind::libunwind::_Unwind_Context"*** %0, align 8, !nonnull !2
  %_2 = load %"unwind::libunwind::_Unwind_Context"*, %"unwind::libunwind::_Unwind_Context"** %1, align 8
  %2 = tail call i64 @_Unwind_GetDataRelBase(%"unwind::libunwind::_Unwind_Context"* %_2) #12
  ret i64 %2
}

; Function Attrs: nounwind nonlazybind uwtable
define i32 @rust_eh_personality(i32 %version, i32 %actions, i64 %exception_class, %"unwind::libunwind::_Unwind_Exception"* %exception_object, %"unwind::libunwind::_Unwind_Context"* %context) unnamed_addr #3 personality i32 (i32, i32, i64, %"unwind::libunwind::_Unwind_Exception"*, %"unwind::libunwind::_Unwind_Context"*)* @rust_eh_personality {
start:
  %reader.i.i.i = alloca i8*, align 8
  %_24.i.i = alloca i64*, align 8
  %_19.i.i = alloca i64*, align 8
  %eh_context.i.i = alloca %"dwarf::eh::EHContext", align 8
  %ip_before_instr.i.i = alloca i32, align 4
  %context.i.i = alloca %"unwind::libunwind::_Unwind_Context"*, align 8
  %0 = icmp eq i32 %version, 1
  br i1 %0, label %bb2.i, label %_ZN12panic_unwind8real_imp24rust_eh_personality_impl17h31fcc2a1f7dc907bE.exit

bb2.i:                                            ; preds = %start
  %1 = bitcast %"unwind::libunwind::_Unwind_Context"** %context.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1) #12
  store %"unwind::libunwind::_Unwind_Context"* %context, %"unwind::libunwind::_Unwind_Context"** %context.i.i, align 8
  %_3.i.i = tail call i8* @_Unwind_GetLanguageSpecificData(%"unwind::libunwind::_Unwind_Context"* %context) #12
  %2 = bitcast i32* %ip_before_instr.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 4, i8* nonnull %2) #12
  store i32 0, i32* %ip_before_instr.i.i, align 4
  %ip.i.i = call i64 @_Unwind_GetIPInfo(%"unwind::libunwind::_Unwind_Context"* %context, i32* nonnull %ip_before_instr.i.i) #12
  %3 = bitcast %"dwarf::eh::EHContext"* %eh_context.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %3) #12
  %_12.i.i = load i32, i32* %ip_before_instr.i.i, align 4
  %4 = icmp eq i32 %_12.i.i, 0
  %5 = sext i1 %4 to i64
  %_11.0.i.i = add i64 %ip.i.i, %5
  %_14.i.i = call i64 @_Unwind_GetRegionStart(%"unwind::libunwind::_Unwind_Context"* %context) #12
  %6 = bitcast i64** %_19.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %6) #12
  %7 = bitcast i64** %_19.i.i to %"unwind::libunwind::_Unwind_Context"***
  store %"unwind::libunwind::_Unwind_Context"** %context.i.i, %"unwind::libunwind::_Unwind_Context"*** %7, align 8
  %8 = bitcast i64** %_24.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %8) #12
  %9 = bitcast i64** %_24.i.i to %"unwind::libunwind::_Unwind_Context"***
  store %"unwind::libunwind::_Unwind_Context"** %context.i.i, %"unwind::libunwind::_Unwind_Context"*** %9, align 8
  %10 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %eh_context.i.i, i64 0, i32 0
  store i64 %_11.0.i.i, i64* %10, align 8
  %11 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %eh_context.i.i, i64 0, i32 1
  store i64 %_14.i.i, i64* %11, align 8
  %12 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %eh_context.i.i, i64 0, i32 2
  %13 = bitcast { {}*, [3 x i64]* }* %12 to i64***
  store i64** %_19.i.i, i64*** %13, align 8
  %14 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %eh_context.i.i, i64 0, i32 2, i32 1
  store [3 x i64]* bitcast (<{ i8*, [16 x i8], i8*, i8*, i8*, [0 x i8] }>* @vtable.0 to [3 x i64]*), [3 x i64]** %14, align 8
  %15 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %eh_context.i.i, i64 0, i32 3
  %16 = bitcast { {}*, [3 x i64]* }* %15 to i64***
  store i64** %_24.i.i, i64*** %16, align 8
  %17 = getelementptr inbounds %"dwarf::eh::EHContext", %"dwarf::eh::EHContext"* %eh_context.i.i, i64 0, i32 3, i32 1
  store [3 x i64]* bitcast (<{ i8*, [16 x i8], i8*, i8*, i8*, [0 x i8] }>* @vtable.1 to [3 x i64]*), [3 x i64]** %17, align 8
  %18 = icmp eq i8* %_3.i.i, null
  br i1 %18, label %bb3.i, label %bb3.i.i.i

bb3.i.i.i:                                        ; preds = %bb2.i
  %19 = bitcast i8** %reader.i.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %19) #12, !noalias !70
  call void @llvm.experimental.noalias.scope.decl(metadata !73) #12
  %result.i.i.i.i = load i8, i8* %_3.i.i, align 1, !noalias !76
  %20 = getelementptr inbounds i8, i8* %_3.i.i, i64 1
  store i8* %20, i8** %reader.i.i.i, align 8, !alias.scope !73, !noalias !70
  %_12.not.i.i.i = icmp eq i8 %result.i.i.i.i, -1
  br i1 %_12.not.i.i.i, label %bb14.i.i.i, label %bb6.i.i.i

bb6.i.i.i:                                        ; preds = %bb3.i.i.i
; invoke panic_unwind::dwarf::eh::read_encoded_pointer
  %21 = invoke fastcc { i64, i64 } @_ZN12panic_unwind5dwarf2eh20read_encoded_pointer17hfc4ac8313c3f402dE(i8** noalias nonnull align 8 dereferenceable(8) %reader.i.i.i, %"dwarf::eh::EHContext"* noalias nonnull readonly align 8 dereferenceable(48) %eh_context.i.i, i8 %result.i.i.i.i)
          to label %.noexc.i unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.i.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp

.noexc.i:                                         ; preds = %bb6.i.i.i
  %_15.0.i.i.i = extractvalue { i64, i64 } %21, 0
  %switch.i.not.i.i.i = icmp eq i64 %_15.0.i.i.i, 0
  br i1 %switch.i.not.i.i.i, label %bb6.bb14_crit_edge.i.i.i, label %bb56.i.i.i

bb6.bb14_crit_edge.i.i.i:                         ; preds = %.noexc.i
  %_15.1.i.i.i = extractvalue { i64, i64 } %21, 1
  %_3.i55.pre.i.i.i = load i8*, i8** %reader.i.i.i, align 8, !alias.scope !77, !noalias !70
  br label %bb14.i.i.i

bb56.i.i.i:                                       ; preds = %.noexc3.i, %.noexc2.i, %.noexc1.i, %bb48.i.i.i, %bb45.i.i.i, %.noexc.i
  %.sroa.9.1.i.i.i = phi i64 [ %lpad.i.i.i, %bb48.i.i.i ], [ undef, %bb45.i.i.i ], [ undef, %.noexc.i ], [ undef, %.noexc1.i ], [ undef, %.noexc2.i ], [ undef, %.noexc3.i ]
  %.sroa.0.1.i.i.i = phi i64 [ %..i83.i.i.i, %bb48.i.i.i ], [ 0, %bb45.i.i.i ], [ 4, %.noexc.i ], [ 4, %.noexc1.i ], [ 4, %.noexc2.i ], [ 4, %.noexc3.i ]
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %19) #12, !noalias !70
  br label %bb3.i

bb14.i.i.i:                                       ; preds = %bb6.bb14_crit_edge.i.i.i, %bb3.i.i.i
  %_3.i55.i.i.i = phi i8* [ %20, %bb3.i.i.i ], [ %_3.i55.pre.i.i.i, %bb6.bb14_crit_edge.i.i.i ]
  %lpad_base.0.i.i.i = phi i64 [ %_14.i.i, %bb3.i.i.i ], [ %_15.1.i.i.i, %bb6.bb14_crit_edge.i.i.i ]
  call void @llvm.experimental.noalias.scope.decl(metadata !77) #12
  %result.i56.i.i.i = load i8, i8* %_3.i55.i.i.i, align 1, !noalias !80
  %22 = getelementptr inbounds i8, i8* %_3.i55.i.i.i, i64 1
  store i8* %22, i8** %reader.i.i.i, align 8, !alias.scope !77, !noalias !70
  %_26.not.i.i.i = icmp eq i8 %result.i56.i.i.i, -1
  br i1 %_26.not.i.i.i, label %bb18.i.i.i, label %bb16.i.i.i

bb18.i.i.i:                                       ; preds = %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit67.i.i.i, %bb14.i.i.i
  %_3.i57.i.i.i = phi i8* [ %32, %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit67.i.i.i ], [ %22, %bb14.i.i.i ]
  call void @llvm.experimental.noalias.scope.decl(metadata !81) #12
  %result.i58.i.i.i = load i8, i8* %_3.i57.i.i.i, align 1, !noalias !84
  %23 = getelementptr inbounds i8, i8* %_3.i57.i.i.i, i64 1
  store i8* %23, i8** %reader.i.i.i, align 8, !alias.scope !81, !noalias !70
  call void @llvm.experimental.noalias.scope.decl(metadata !85) #12
  br label %bb1.i.i.i.i

bb1.i.i.i.i:                                      ; preds = %bb1.i.i.i.i, %bb18.i.i.i
  %24 = phi i8* [ %23, %bb18.i.i.i ], [ %25, %bb1.i.i.i.i ]
  %shift.0.i.i.i.i = phi i64 [ 0, %bb18.i.i.i ], [ %28, %bb1.i.i.i.i ]
  %result.0.i.i.i.i = phi i64 [ 0, %bb18.i.i.i ], [ %27, %bb1.i.i.i.i ]
  call void @llvm.experimental.noalias.scope.decl(metadata !88) #12
  %result.i.i.i.i.i = load i8, i8* %24, align 1, !noalias !91
  %25 = getelementptr inbounds i8, i8* %24, i64 1
  %_8.i.i.i.i = and i8 %result.i.i.i.i.i, 127
  %_7.i.i.i.i = zext i8 %_8.i.i.i.i to i64
  %26 = and i64 %shift.0.i.i.i.i, 63
  %_6.i.i.i.i = shl i64 %_7.i.i.i.i, %26
  %27 = or i64 %_6.i.i.i.i, %result.0.i.i.i.i
  %28 = add i64 %shift.0.i.i.i.i, 7
  %29 = icmp sgt i8 %result.i.i.i.i.i, -1
  br i1 %29, label %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit.i.i.i, label %bb1.i.i.i.i

_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit.i.i.i: ; preds = %bb1.i.i.i.i
  store i8* %25, i8** %reader.i.i.i, align 8, !alias.scope !92, !noalias !70
  %30 = getelementptr inbounds i8, i8* %25, i64 %27
  br label %bb22.i.i.i

bb16.i.i.i:                                       ; preds = %bb14.i.i.i
  call void @llvm.experimental.noalias.scope.decl(metadata !93) #12
  br label %bb1.i66.i.i.i

bb1.i66.i.i.i:                                    ; preds = %bb1.i66.i.i.i, %bb16.i.i.i
  %31 = phi i8* [ %22, %bb16.i.i.i ], [ %32, %bb1.i66.i.i.i ]
  call void @llvm.experimental.noalias.scope.decl(metadata !96) #12
  %result.i.i62.i.i.i = load i8, i8* %31, align 1, !noalias !99
  %32 = getelementptr inbounds i8, i8* %31, i64 1
  %33 = icmp sgt i8 %result.i.i62.i.i.i, -1
  br i1 %33, label %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit67.i.i.i, label %bb1.i66.i.i.i

_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit67.i.i.i: ; preds = %bb1.i66.i.i.i
  store i8* %32, i8** %reader.i.i.i, align 8, !alias.scope !100, !noalias !70
  br label %bb18.i.i.i

bb22.i.i.i:                                       ; preds = %bb44.i.i.i, %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit.i.i.i
  %_40.i.i.i = phi i8* [ %38, %bb44.i.i.i ], [ %25, %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit.i.i.i ]
  %_39.i.i.i = icmp ult i8* %_40.i.i.i, %30
  br i1 %_39.i.i.i, label %bb23.i.i.i, label %bb51.i.i.i

bb23.i.i.i:                                       ; preds = %bb22.i.i.i
; invoke panic_unwind::dwarf::eh::read_encoded_pointer
  %34 = invoke fastcc { i64, i64 } @_ZN12panic_unwind5dwarf2eh20read_encoded_pointer17hfc4ac8313c3f402dE(i8** noalias nonnull align 8 dereferenceable(8) %reader.i.i.i, %"dwarf::eh::EHContext"* noalias nonnull readonly align 8 dereferenceable(48) %eh_context.i.i, i8 %result.i58.i.i.i)
          to label %.noexc1.i unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.i.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp

.noexc1.i:                                        ; preds = %bb23.i.i.i
  %_44.0.i.i.i = extractvalue { i64, i64 } %34, 0
  %_44.1.i.i.i = extractvalue { i64, i64 } %34, 1
  %switch.i68.not.i.i.i = icmp eq i64 %_44.0.i.i.i, 0
  br i1 %switch.i68.not.i.i.i, label %bb26.i.i.i, label %bb56.i.i.i

bb26.i.i.i:                                       ; preds = %.noexc1.i
; invoke panic_unwind::dwarf::eh::read_encoded_pointer
  %35 = invoke fastcc { i64, i64 } @_ZN12panic_unwind5dwarf2eh20read_encoded_pointer17hfc4ac8313c3f402dE(i8** noalias nonnull align 8 dereferenceable(8) %reader.i.i.i, %"dwarf::eh::EHContext"* noalias nonnull readonly align 8 dereferenceable(48) %eh_context.i.i, i8 %result.i58.i.i.i)
          to label %.noexc2.i unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.i.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp

.noexc2.i:                                        ; preds = %bb26.i.i.i
  %_55.0.i.i.i = extractvalue { i64, i64 } %35, 0
  %_55.1.i.i.i = extractvalue { i64, i64 } %35, 1
  %switch.i70.not.i.i.i = icmp eq i64 %_55.0.i.i.i, 0
  br i1 %switch.i70.not.i.i.i, label %bb32.i.i.i, label %bb56.i.i.i

bb32.i.i.i:                                       ; preds = %.noexc2.i
; invoke panic_unwind::dwarf::eh::read_encoded_pointer
  %36 = invoke fastcc { i64, i64 } @_ZN12panic_unwind5dwarf2eh20read_encoded_pointer17hfc4ac8313c3f402dE(i8** noalias nonnull align 8 dereferenceable(8) %reader.i.i.i, %"dwarf::eh::EHContext"* noalias nonnull readonly align 8 dereferenceable(48) %eh_context.i.i, i8 %result.i58.i.i.i)
          to label %.noexc3.i unwind label %cleanup.loopexit.split-lp.loopexit.split-lp.i.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp

.noexc3.i:                                        ; preds = %bb32.i.i.i
  %_66.0.i.i.i = extractvalue { i64, i64 } %36, 0
  %switch.i72.not.i.i.i = icmp eq i64 %_66.0.i.i.i, 0
  br i1 %switch.i72.not.i.i.i, label %bb38.i.i.i, label %bb56.i.i.i

bb38.i.i.i:                                       ; preds = %.noexc3.i
  call void @llvm.experimental.noalias.scope.decl(metadata !101) #12
  %self.promoted.i74.i.i.i = load i8*, i8** %reader.i.i.i, align 8, !alias.scope !104, !noalias !70
  br label %bb1.i81.i.i.i

bb1.i81.i.i.i:                                    ; preds = %bb1.i81.i.i.i, %bb38.i.i.i
  %37 = phi i8* [ %self.promoted.i74.i.i.i, %bb38.i.i.i ], [ %38, %bb1.i81.i.i.i ]
  %shift.0.i75.i.i.i = phi i64 [ 0, %bb38.i.i.i ], [ %41, %bb1.i81.i.i.i ]
  %result.0.i76.i.i.i = phi i64 [ 0, %bb38.i.i.i ], [ %40, %bb1.i81.i.i.i ]
  call void @llvm.experimental.noalias.scope.decl(metadata !107) #12
  %result.i.i77.i.i.i = load i8, i8* %37, align 1, !noalias !108
  %38 = getelementptr inbounds i8, i8* %37, i64 1
  %_8.i78.i.i.i = and i8 %result.i.i77.i.i.i, 127
  %_7.i79.i.i.i = zext i8 %_8.i78.i.i.i to i64
  %39 = and i64 %shift.0.i75.i.i.i, 63
  %_6.i80.i.i.i = shl i64 %_7.i79.i.i.i, %39
  %40 = or i64 %_6.i80.i.i.i, %result.0.i76.i.i.i
  %41 = add i64 %shift.0.i75.i.i.i, 7
  %42 = icmp sgt i8 %result.i.i77.i.i.i, -1
  br i1 %42, label %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit82.i.i.i, label %bb1.i81.i.i.i

_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit82.i.i.i: ; preds = %bb1.i81.i.i.i
  store i8* %38, i8** %reader.i.i.i, align 8, !alias.scope !104, !noalias !70
  %_79.i.i.i = add i64 %_44.1.i.i.i, %_14.i.i
  %_77.i.i.i = icmp ult i64 %_11.0.i.i, %_79.i.i.i
  br i1 %_77.i.i.i, label %bb51.i.i.i, label %bb44.i.i.i

bb44.i.i.i:                                       ; preds = %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit82.i.i.i
  %_84.i.i.i = add i64 %_55.1.i.i.i, %_79.i.i.i
  %_82.i.i.i = icmp ult i64 %_11.0.i.i, %_84.i.i.i
  br i1 %_82.i.i.i, label %bb45.i.i.i, label %bb22.i.i.i

bb51.i.i.i:                                       ; preds = %_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE.exit82.i.i.i, %bb22.i.i.i
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %19) #12, !noalias !70
  br label %bb3.i

bb45.i.i.i:                                       ; preds = %bb44.i.i.i
  %_66.1.le.i.i.i = extractvalue { i64, i64 } %36, 1
  %43 = icmp eq i64 %_66.1.le.i.i.i, 0
  br i1 %43, label %bb56.i.i.i, label %bb48.i.i.i

bb48.i.i.i:                                       ; preds = %bb45.i.i.i
  %lpad.i.i.i = add i64 %_66.1.le.i.i.i, %lpad_base.0.i.i.i
  %44 = icmp eq i64 %40, 0
  %..i83.i.i.i = select i1 %44, i64 1, i64 2
  br label %bb56.i.i.i

bb3.i:                                            ; preds = %bb51.i.i.i, %bb56.i.i.i, %bb2.i
  %.sroa.9.0.i.i.i = phi i64 [ undef, %bb51.i.i.i ], [ %.sroa.9.1.i.i.i, %bb56.i.i.i ], [ undef, %bb2.i ]
  %.sroa.0.0.i.i.i = phi i64 [ 0, %bb51.i.i.i ], [ %.sroa.0.1.i.i.i, %bb56.i.i.i ], [ 0, %bb2.i ]
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %8) #12
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %6) #12
  call void @llvm.lifetime.end.p0i8(i64 48, i8* nonnull %3) #12
  call void @llvm.lifetime.end.p0i8(i64 4, i8* nonnull %2) #12
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1) #12
  %45 = icmp eq i64 %.sroa.0.0.i.i.i, 4
  br i1 %45, label %_ZN12panic_unwind8real_imp24rust_eh_personality_impl17h31fcc2a1f7dc907bE.exit, label %bb6.i

cleanup.loopexit.split-lp.loopexit.split-lp.i.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp.loopexit.split-lp: ; preds = %bb32.i.i.i, %bb26.i.i.i, %bb23.i.i.i, %bb6.i.i.i
  %lpad.loopexit.split-lp22 = landingpad { i8*, i32 }
          cleanup
  call void @llvm.trap() #12
  unreachable

bb6.i:                                            ; preds = %bb3.i
  %46 = icmp ne i32 %actions, 0
  call void @llvm.assume(i1 %46) #12
  %47 = icmp ult i32 %actions, 17
  call void @llvm.assume(i1 %47) #12
  %_12.i = and i32 %actions, 1
  %48 = icmp eq i32 %_12.i, 0
  br i1 %48, label %bb8.i, label %switch.lookup

bb8.i:                                            ; preds = %bb6.i
  switch i64 %.sroa.0.0.i.i.i, label %bb14.i [
    i64 0, label %_ZN12panic_unwind8real_imp24rust_eh_personality_impl17h31fcc2a1f7dc907bE.exit
    i64 1, label %bb16.i
    i64 2, label %bb16.i
    i64 3, label %bb13.i
  ]

bb14.i:                                           ; preds = %bb8.i
  unreachable

bb13.i:                                           ; preds = %bb8.i
  br label %_ZN12panic_unwind8real_imp24rust_eh_personality_impl17h31fcc2a1f7dc907bE.exit

bb16.i:                                           ; preds = %bb8.i, %bb8.i
  %_22.i = ptrtoint %"unwind::libunwind::_Unwind_Exception"* %exception_object to i64
  call void @_Unwind_SetGR(%"unwind::libunwind::_Unwind_Context"* %context, i32 0, i64 %_22.i) #12
  call void @_Unwind_SetGR(%"unwind::libunwind::_Unwind_Context"* %context, i32 1, i64 0) #12
  call void @_Unwind_SetIP(%"unwind::libunwind::_Unwind_Context"* %context, i64 %.sroa.9.0.i.i.i) #12
  br label %_ZN12panic_unwind8real_imp24rust_eh_personality_impl17h31fcc2a1f7dc907bE.exit

switch.lookup:                                    ; preds = %bb6.i
  %switch.gep = getelementptr inbounds [4 x i32], [4 x i32]* @switch.table.rust_eh_personality, i64 0, i64 %.sroa.0.0.i.i.i
  %switch.load = load i32, i32* %switch.gep, align 4
  br label %_ZN12panic_unwind8real_imp24rust_eh_personality_impl17h31fcc2a1f7dc907bE.exit

_ZN12panic_unwind8real_imp24rust_eh_personality_impl17h31fcc2a1f7dc907bE.exit: ; preds = %switch.lookup, %start, %bb3.i, %bb8.i, %bb13.i, %bb16.i
  %.0.i = phi i32 [ 3, %start ], [ 3, %bb3.i ], [ 2, %bb13.i ], [ 7, %bb16.i ], [ 8, %bb8.i ], [ %switch.load, %switch.lookup ]
  ret i32 %.0.i
}

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.start.p0i8(i64 immarg, i8* nocapture) #5

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.end.p0i8(i64 immarg, i8* nocapture) #5

; Function Attrs: cold noreturn nounwind
declare void @llvm.trap() #6

; Function Attrs: argmemonly mustprogress nofree nounwind willreturn writeonly
declare void @llvm.memset.p0i8.i64(i8* nocapture writeonly, i8, i64, i1 immarg) #7

; Function Attrs: nonlazybind uwtable
declare i32 @_Unwind_RaiseException(%"unwind::libunwind::_Unwind_Exception"*) unnamed_addr #1

; Function Attrs: inaccessiblememonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.assume(i1 noundef) #8

; Function Attrs: noreturn nounwind nonlazybind uwtable
declare void @__rust_drop_panic() unnamed_addr #4

; Function Attrs: nounwind nonlazybind uwtable
declare void @_Unwind_DeleteException(%"unwind::libunwind::_Unwind_Exception"*) unnamed_addr #3

; Function Attrs: noreturn nounwind nonlazybind uwtable
declare void @__rust_foreign_exception() unnamed_addr #4

; Function Attrs: nounwind nonlazybind uwtable
declare i8* @_Unwind_GetLanguageSpecificData(%"unwind::libunwind::_Unwind_Context"*) unnamed_addr #3

; Function Attrs: nounwind nonlazybind uwtable
declare i64 @_Unwind_GetIPInfo(%"unwind::libunwind::_Unwind_Context"*, i32*) unnamed_addr #3

; Function Attrs: nounwind nonlazybind uwtable
declare i64 @_Unwind_GetRegionStart(%"unwind::libunwind::_Unwind_Context"*) unnamed_addr #3

; Function Attrs: nounwind nonlazybind uwtable
declare i64 @_Unwind_GetTextRelBase(%"unwind::libunwind::_Unwind_Context"*) unnamed_addr #3

; Function Attrs: nounwind nonlazybind uwtable
declare i64 @_Unwind_GetDataRelBase(%"unwind::libunwind::_Unwind_Context"*) unnamed_addr #3

; Function Attrs: nounwind nonlazybind uwtable
declare void @_Unwind_SetGR(%"unwind::libunwind::_Unwind_Context"*, i32, i64) unnamed_addr #3

; Function Attrs: nounwind nonlazybind uwtable
declare void @_Unwind_SetIP(%"unwind::libunwind::_Unwind_Context"*, i64) unnamed_addr #3

; Function Attrs: nofree nounwind nonlazybind uwtable
declare noalias i8* @__rust_alloc(i64, i64) unnamed_addr #9

; Function Attrs: nounwind nonlazybind uwtable
declare void @__rust_dealloc(i8*, i64, i64) unnamed_addr #3

; alloc::alloc::handle_alloc_error
; Function Attrs: cold noreturn nounwind nonlazybind uwtable
declare void @_ZN5alloc5alloc18handle_alloc_error17h2d5c084c39e97fa4E(i64, i64) unnamed_addr #10

; Function Attrs: inaccessiblememonly nofree nosync nounwind willreturn
declare void @llvm.experimental.noalias.scope.decl(metadata) #11

attributes #0 = { inlinehint nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #3 = { nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #4 = { noreturn nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #5 = { argmemonly mustprogress nofree nosync nounwind willreturn }
attributes #6 = { cold noreturn nounwind }
attributes #7 = { argmemonly mustprogress nofree nounwind willreturn writeonly }
attributes #8 = { inaccessiblememonly mustprogress nofree nosync nounwind willreturn }
attributes #9 = { nofree nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #10 = { cold noreturn nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #11 = { inaccessiblememonly nofree nosync nounwind willreturn }
attributes #12 = { nounwind }
attributes #13 = { noreturn nounwind }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
!2 = !{}
!3 = !{!4}
!4 = distinct !{!4, !5, !"_ZN4core3ops8function6FnOnce9call_once17h7be4525fff0afcf6E: argument 0"}
!5 = distinct !{!5, !"_ZN4core3ops8function6FnOnce9call_once17h7be4525fff0afcf6E"}
!6 = !{!7}
!7 = distinct !{!7, !8, !"_ZN12panic_unwind8real_imp14find_eh_action28_$u7b$$u7b$closure$u7d$$u7d$17h3f99be3bad287315E: %_1"}
!8 = distinct !{!8, !"_ZN12panic_unwind8real_imp14find_eh_action28_$u7b$$u7b$closure$u7d$$u7d$17h3f99be3bad287315E"}
!9 = !{!7, !4}
!10 = !{!11}
!11 = distinct !{!11, !12, !"_ZN4core3ops8function6FnOnce9call_once17hc22ba92b03037ad9E: argument 0"}
!12 = distinct !{!12, !"_ZN4core3ops8function6FnOnce9call_once17hc22ba92b03037ad9E"}
!13 = !{!14}
!14 = distinct !{!14, !15, !"_ZN12panic_unwind8real_imp14find_eh_action28_$u7b$$u7b$closure$u7d$$u7d$17hb1eb2c952be3528cE: %_1"}
!15 = distinct !{!15, !"_ZN12panic_unwind8real_imp14find_eh_action28_$u7b$$u7b$closure$u7d$$u7d$17hb1eb2c952be3528cE"}
!16 = !{!14, !11}
!17 = !{!18}
!18 = distinct !{!18, !19, !"_ZN5alloc5alloc8box_free17h950fba601da12174E: argument 0"}
!19 = distinct !{!19, !"_ZN5alloc5alloc8box_free17h950fba601da12174E"}
!20 = !{i64 1, i64 0}
!21 = !{!22}
!22 = distinct !{!22, !23, !"_ZN5alloc5alloc8box_free17h950fba601da12174E: argument 0"}
!23 = distinct !{!23, !"_ZN5alloc5alloc8box_free17h950fba601da12174E"}
!24 = !{!25}
!25 = distinct !{!25, !26, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h767e9dc419797c29E: %self"}
!26 = distinct !{!26, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h767e9dc419797c29E"}
!27 = !{!28}
!28 = distinct !{!28, !29, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h767e9dc419797c29E: %self"}
!29 = distinct !{!29, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h767e9dc419797c29E"}
!30 = !{!31}
!31 = distinct !{!31, !32, !"_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE: %self"}
!32 = distinct !{!32, !"_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE"}
!33 = !{!34, !31}
!34 = distinct !{!34, !35, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE: %self"}
!35 = distinct !{!35, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE"}
!36 = !{!34}
!37 = !{!38}
!38 = distinct !{!38, !39, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h0e60ab9843ba8aebE: %self"}
!39 = distinct !{!39, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h0e60ab9843ba8aebE"}
!40 = !{!41}
!41 = distinct !{!41, !42, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hcd57b8748267fd1fE: %self"}
!42 = distinct !{!42, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hcd57b8748267fd1fE"}
!43 = !{!44}
!44 = distinct !{!44, !45, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h1e5abaa0c679a2b0E: %self"}
!45 = distinct !{!45, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h1e5abaa0c679a2b0E"}
!46 = !{!47}
!47 = distinct !{!47, !48, !"_ZN12panic_unwind5dwarf11DwarfReader12read_sleb12817h3b43dcffc535e765E: %self"}
!48 = distinct !{!48, !"_ZN12panic_unwind5dwarf11DwarfReader12read_sleb12817h3b43dcffc535e765E"}
!49 = !{!50, !47}
!50 = distinct !{!50, !51, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE: %self"}
!51 = distinct !{!51, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE"}
!52 = !{!50}
!53 = !{!54}
!54 = distinct !{!54, !55, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h7bca432d197eda61E: %self"}
!55 = distinct !{!55, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h7bca432d197eda61E"}
!56 = !{!57}
!57 = distinct !{!57, !58, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hbba1289c6dbb6407E: %self"}
!58 = distinct !{!58, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hbba1289c6dbb6407E"}
!59 = !{!60}
!60 = distinct !{!60, !61, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h72b1fcbced2cff82E: %self"}
!61 = distinct !{!61, !"_ZN12panic_unwind5dwarf11DwarfReader4read17h72b1fcbced2cff82E"}
!62 = !{!63, !65, !67}
!63 = distinct !{!63, !64, !"_ZN5alloc5boxed12Box$LT$T$GT$3new17h33ae0e2d02e55a81E: %x"}
!64 = distinct !{!64, !"_ZN5alloc5boxed12Box$LT$T$GT$3new17h33ae0e2d02e55a81E"}
!65 = distinct !{!65, !66, !"_ZN12panic_unwind8real_imp5panic17h9a3ac7f584fbd0bcE: argument 0"}
!66 = distinct !{!66, !"_ZN12panic_unwind8real_imp5panic17h9a3ac7f584fbd0bcE"}
!67 = distinct !{!67, !66, !"_ZN12panic_unwind8real_imp5panic17h9a3ac7f584fbd0bcE: argument 1"}
!68 = !{!65, !67}
!69 = !{i32 0, i32 10}
!70 = !{!71}
!71 = distinct !{!71, !72, !"_ZN12panic_unwind5dwarf2eh14find_eh_action17hfa0edd1dfbdc31bcE: %context"}
!72 = distinct !{!72, !"_ZN12panic_unwind5dwarf2eh14find_eh_action17hfa0edd1dfbdc31bcE"}
!73 = !{!74}
!74 = distinct !{!74, !75, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE: %self"}
!75 = distinct !{!75, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE"}
!76 = !{!74, !71}
!77 = !{!78}
!78 = distinct !{!78, !79, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE: %self"}
!79 = distinct !{!79, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE"}
!80 = !{!78, !71}
!81 = !{!82}
!82 = distinct !{!82, !83, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE: %self"}
!83 = distinct !{!83, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE"}
!84 = !{!82, !71}
!85 = !{!86}
!86 = distinct !{!86, !87, !"_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE: %self"}
!87 = distinct !{!87, !"_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE"}
!88 = !{!89}
!89 = distinct !{!89, !90, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE: %self"}
!90 = distinct !{!90, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE"}
!91 = !{!89, !86, !71}
!92 = !{!89, !86}
!93 = !{!94}
!94 = distinct !{!94, !95, !"_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE: %self"}
!95 = distinct !{!95, !"_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE"}
!96 = !{!97}
!97 = distinct !{!97, !98, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE: %self"}
!98 = distinct !{!98, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE"}
!99 = !{!97, !94, !71}
!100 = !{!97, !94}
!101 = !{!102}
!102 = distinct !{!102, !103, !"_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE: %self"}
!103 = distinct !{!103, !"_ZN12panic_unwind5dwarf11DwarfReader12read_uleb12817hd4bead231c3061ddE"}
!104 = !{!105, !102}
!105 = distinct !{!105, !106, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE: %self"}
!106 = distinct !{!106, !"_ZN12panic_unwind5dwarf11DwarfReader4read17hd7c3a384be6b231aE"}
!107 = !{!105}
!108 = !{!105, !102, !71}
