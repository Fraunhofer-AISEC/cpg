source_filename = "test"
target datalayout = "e-m:e-i64:64-i128:128-n32:64-S128"

@test = constant [11 x i32] [ i32 42, i32 11, i32 74, i32 42, i32 11, i32 74, i32 11, i32 74, i32 1, i32 74, i32 1 ]
@global_var_100003f94 = constant [11 x i8] c"it was 1!\0A\00"
@global_var_100003f9f = constant [22 x i8] c"it was something else\00"
@global_var_100008000 = local_unnamed_addr global i64 4294983548
@global_var_100003f7c = local_unnamed_addr constant i64 1729382227248152656
@global_var_100008008 = local_unnamed_addr global i64 4294983560
@global_var_100003f88 = local_unnamed_addr constant i64 1729382214363250768
@global_var_100008010 = local_unnamed_addr global i64 0

define i64 @main(i64 %argc, i8** %argv) local_unnamed_addr {
dec_label_pc_100003eec:
  %storemerge.reg2mem = alloca i64, !insn.addr !0
  %0 = call i32 @_rand(), !insn.addr !1
  %1 = icmp eq i32 %0, 1, !insn.addr !2
  br i1 %1, label %dec_label_pc_100003f10, label %dec_label_pc_100003f28, !insn.addr !2

dec_label_pc_100003f10:                           ; preds = %dec_label_pc_100003eec
  %2 = call i32 (i8*, ...) @_printf(i8* getelementptr inbounds ([11 x i8], [11 x i8]* @global_var_100003f94, i64 0, i64 0)), !insn.addr !3
  store i64 10, i64* %storemerge.reg2mem, !insn.addr !4
  br label %dec_label_pc_100003f3c, !insn.addr !4

dec_label_pc_100003f28:                           ; preds = %dec_label_pc_100003eec
  %3 = call i32 (i8*, ...) @_printf(i8* getelementptr inbounds ([22 x i8], [22 x i8]* @global_var_100003f9f, i64 0, i64 0)), !insn.addr !5
  store i64 20, i64* %storemerge.reg2mem, !insn.addr !6
  br label %dec_label_pc_100003f3c, !insn.addr !6

dec_label_pc_100003f3c:                           ; preds = %dec_label_pc_100003f28, %dec_label_pc_100003f10
  %storemerge.reload = load i64, i64* %storemerge.reg2mem
  ret i64 %storemerge.reload, !insn.addr !7
}

declare i32 @_printf(i8*, ...) local_unnamed_addr

declare i32 @_rand() local_unnamed_addr

!0 = !{i64 4294983404}
!1 = !{i64 4294983420}
!2 = !{i64 4294983436}
!3 = !{i64 4294983448}
!4 = !{i64 4294983460}
!5 = !{i64 4294983472}
!6 = !{i64 4294983480}
!7 = !{i64 4294983496}
