; ModuleID = 'dct module #0'
source_filename = "dct module #0"

%regset = type { i64, i64, i32, i64, i64, i64, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, <16 x i8>, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64 }

define void @fn_100003F9C(%regset* noalias nocapture) {
entry_fn_100003F9C:
  %PC_ptr = getelementptr inbounds %regset, %regset* %0, i32 0, i32 3
  %PC_init = load i64, i64* %PC_ptr
  %PC = alloca i64
  store i64 %PC_init, i64* %PC
  %SP_ptr = getelementptr inbounds %regset, %regset* %0, i32 0, i32 4
  %SP_init = load i64, i64* %SP_ptr
  %SP = alloca i64
  store i64 %SP_init, i64* %SP
  %WSP_init = trunc i64 %SP_init to i32
  %WSP = alloca i32
  store i32 %WSP_init, i32* %WSP
  %X8_ptr = getelementptr inbounds %regset, %regset* %0, i32 0, i32 46
  %X8_init = load i64, i64* %X8_ptr
  %X8 = alloca i64
  store i64 %X8_init, i64* %X8
  %W8_init = trunc i64 %X8_init to i32
  %W8 = alloca i32
  store i32 %W8_init, i32* %W8
  %X0_ptr = getelementptr inbounds %regset, %regset* %0, i32 0, i32 38
  %X0_init = load i64, i64* %X0_ptr
  %X0 = alloca i64
  store i64 %X0_init, i64* %X0
  %W0_init = trunc i64 %X0_init to i32
  %W0 = alloca i32
  store i32 %W0_init, i32* %W0
  %LR_ptr = getelementptr inbounds %regset, %regset* %0, i32 0, i32 1
  %LR_init = load i64, i64* %LR_ptr
  %LR = alloca i64
  store i64 %LR_init, i64* %LR
  br label %bb_100003F9C

exit_fn_100003F9C:                                ; preds = %bb_100003F9C
  %1 = load i64, i64* %LR
  store i64 %1, i64* %LR_ptr
  %2 = load i64, i64* %PC
  store i64 %2, i64* %PC_ptr
  %3 = load i64, i64* %SP
  store i64 %3, i64* %SP_ptr
  %4 = load i64, i64* %X0
  store i64 %4, i64* %X0_ptr
  %5 = load i64, i64* %X8
  store i64 %5, i64* %X8_ptr
  ret void

bb_100003F9C:                                     ; preds = %entry_fn_100003F9C
  %PC_1 = add i64 4294983580, 4
  %SP_0 = load i64, i64* %SP
  %SP_1 = sub i64 %SP_0, 16
  %WSP_0 = trunc i64 %SP_1 to i32
  %PC_2 = add i64 %PC_1, 4
  %6 = add i64 %SP_1, 12
  %7 = inttoptr i64 %6 to i32*
  store i32 0, i32* %7, align 1
  %PC_3 = add i64 %PC_2, 4
  %W8_0 = shl i32 1, 0
  %X8_0 = load i64, i64* %X8
  %X8_1 = zext i32 %W8_0 to i64
  %PC_4 = add i64 %PC_3, 4
  %8 = add i64 %SP_1, 8
  %9 = inttoptr i64 %8 to i32*
  store i32 %W8_0, i32* %9, align 1
  %PC_5 = add i64 %PC_4, 4
  %10 = add i64 %SP_1, 8
  %11 = inttoptr i64 %10 to i32*
  %W0_0 = load i32, i32* %11, align 1
  %X0_0 = load i64, i64* %X0
  %X0_1 = zext i32 %W0_0 to i64
  %PC_6 = add i64 %PC_5, 4
  %SP_2 = add i64 %SP_1, 16
  %WSP_1 = trunc i64 %SP_2 to i32
  %PC_7 = add i64 %PC_6, 4
  %LR_0 = load i64, i64* %LR
  store i64 %LR_0, i64* %LR
  store i64 %LR_0, i64* %PC
  store i64 %SP_2, i64* %SP
  store i32 %WSP_1, i32* %WSP
  store i32 %W0_0, i32* %W0
  store i32 %W8_0, i32* %W8
  store i64 %X0_1, i64* %X0
  store i64 %X8_1, i64* %X8
  br label %exit_fn_100003F9C
}

; Function Attrs: noreturn nounwind
declare void @llvm.trap() #0

define i32 @main(i32, i8**) {
  %3 = alloca %regset, align 64
  %4 = alloca [1024 x i8], align 64
  %5 = getelementptr inbounds [1024 x i8], [1024 x i8]* %4, i32 0, i32 0
  call void @main_init_regset(%regset* %3, i8* %5, i32 1024, i32 %0, i8** %1)
  call void @fn_100003F9C(%regset* %3)
  %6 = call i32 @main_fini_regset(%regset* %3)
  ret i32 %6
}

define void @main_init_regset(%regset*, i8*, i32, i32, i8**) {
  %6 = ptrtoint i8* %1 to i64
  %7 = zext i32 %2 to i64
  %8 = add i64 %6, %7
  %9 = sub i64 %8, 8
  %10 = inttoptr i64 %9 to i64*
  store i64 -1, i64* %10
  %11 = getelementptr inbounds %regset, %regset* %0, i32 0, i32 4
  store i64 %9, i64* %11
  %12 = zext i32 %3 to i64
  %13 = getelementptr inbounds %regset, %regset* %0, i32 0, i32 38
  store i64 %12, i64* %13
  %14 = ptrtoint i8** %4 to i64
  %15 = getelementptr inbounds %regset, %regset* %0, i32 0, i32 39
  store i64 %14, i64* %15
  ret void
}

define i32 @main_fini_regset(%regset*) {
  %2 = getelementptr inbounds %regset, %regset* %0, i32 0, i32 38
  %3 = load i64, i64* %2
  %4 = trunc i64 %3 to i32
  ret i32 %4
}

attributes #0 = { noreturn nounwind }