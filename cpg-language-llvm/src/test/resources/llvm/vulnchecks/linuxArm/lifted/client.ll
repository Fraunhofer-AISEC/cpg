source_filename = "test"
target datalayout = "e-p:32:32:32-f80:32:32"

%sockaddr = type { i32, [14 x i8] }

@global_var_2200c = local_unnamed_addr global i32 68836
@global_var_22010 = local_unnamed_addr global i32 68836
@global_var_22014 = local_unnamed_addr global i32 68836
@global_var_22018 = local_unnamed_addr global i32 68836
@global_var_2201c = local_unnamed_addr global i32 68836
@global_var_22024 = local_unnamed_addr global i32 68836
@global_var_22028 = local_unnamed_addr global i32 68836
@global_var_2202c = local_unnamed_addr global i32 68836
@global_var_22030 = local_unnamed_addr global i32 68836
@global_var_22034 = local_unnamed_addr global i32 68836
@global_var_22038 = local_unnamed_addr global i32 68836
@global_var_2203c = local_unnamed_addr global i32 68836
@global_var_22040 = local_unnamed_addr global i32 68836
@global_var_22044 = local_unnamed_addr global i32 68836
@global_var_22048 = local_unnamed_addr global i32 68836
@global_var_2204c = local_unnamed_addr global i32 68836
@global_var_22050 = local_unnamed_addr global i32 68836
@global_var_22054 = local_unnamed_addr global i32 68836
@global_var_22058 = local_unnamed_addr global i32 68836
@global_var_2205c = local_unnamed_addr global i32 68836
@global_var_22060 = local_unnamed_addr global i32 68836
@global_var_22064 = local_unnamed_addr global i32 68836
@global_var_22068 = local_unnamed_addr global i32 68836
@global_var_2206c = local_unnamed_addr global i32 68836
@global_var_22070 = local_unnamed_addr global i32 68836
@global_var_22074 = local_unnamed_addr global i32 68836
@global_var_22078 = local_unnamed_addr global i32 68836
@global_var_2207c = local_unnamed_addr global i32 68836
@global_var_22084 = local_unnamed_addr global i32 68836
@global_var_22088 = local_unnamed_addr global i32 68836
@global_var_2208c = local_unnamed_addr global i32 68836
@global_var_22090 = local_unnamed_addr global i32 68836
@global_var_22094 = local_unnamed_addr global i32 68836
@global_var_22098 = local_unnamed_addr global i32 68836
@global_var_10ee4 = local_unnamed_addr constant i32 71532
@global_var_10ee8 = local_unnamed_addr constant i32 70552
@global_var_10f0c = local_unnamed_addr constant i32 69888
@global_var_11100 = local_unnamed_addr constant i32 -390232064
@global_var_10f10 = local_unnamed_addr constant i32 160
@global_var_10f00 = local_unnamed_addr constant i32 -481165312
@global_var_10f34 = local_unnamed_addr constant i32 139440
@global_var_10f38 = local_unnamed_addr constant i32 139440
@global_var_10f3c = local_unnamed_addr constant i32 0
@global_var_10f6c = local_unnamed_addr constant i32 139440
@global_var_10f70 = local_unnamed_addr constant i32 139440
@global_var_10f74 = local_unnamed_addr constant i32 0
@global_var_10f9c = local_unnamed_addr constant i32 139788
@global_var_11104 = local_unnamed_addr constant i32 71624
@global_var_21eec = global i32 0
@global_var_117c8 = local_unnamed_addr constant i32* @global_var_21eec
@global_var_11108 = local_unnamed_addr constant i32 71552
@global_var_11780 = constant [23 x i8] c"Error creating socket.\00"
@global_var_1110c = local_unnamed_addr constant i32 71576
@global_var_11798 = constant [15 x i8] c"Connecting to \00"
@global_var_11110 = local_unnamed_addr constant i32 139648
@global_var_22180 = global i32 0
@global_var_11114 = local_unnamed_addr constant i32 71592
@global_var_117a8 = constant [4 x i8] c"...\00"
@global_var_11118 = local_unnamed_addr constant i32 68916
@global_var_11120 = local_unnamed_addr constant i32 71596
@global_var_117ac = constant [28 x i8] c"Error connecting to server.\00"
@global_var_11150 = local_unnamed_addr constant i32 71268
@global_var_11664 = constant i32 -449990652
@global_var_111c0 = local_unnamed_addr constant i32 71640
@global_var_111c4 = local_unnamed_addr constant i32 71628
@global_var_117cc = constant [9 x i8] c"ALL:!ADH\00"
@global_var_117d0 = local_unnamed_addr constant [5 x i8] c"!ADH\00"
@global_var_111f0 = local_unnamed_addr constant i32 71628
@global_var_112b4 = local_unnamed_addr constant i32 71644
@global_var_117dc = local_unnamed_addr constant i32* @global_var_21eec
@global_var_112b8 = local_unnamed_addr constant i32 71628
@global_var_112e4 = local_unnamed_addr constant i32 139436
@global_var_220ac = constant [4 x i8] c"MD5\00"
@global_var_11394 = local_unnamed_addr constant i32 2097154
@global_var_1163c = local_unnamed_addr constant i32 71804
@global_var_1187c = local_unnamed_addr constant i32* @global_var_21eec
@global_var_11640 = local_unnamed_addr constant i32 71648
@global_var_117e0 = constant [14 x i8] c"172.217.18.99\00"
@global_var_11644 = local_unnamed_addr constant i32 139792
@global_var_22210 = global i32 0
@global_var_11648 = local_unnamed_addr constant i32 71664
@global_var_117f0 = constant [20 x i8] c"Error creating SSL.\00"
@global_var_1164c = local_unnamed_addr constant i32 139648
@global_var_11650 = local_unnamed_addr constant i32 68916
@global_var_11654 = local_unnamed_addr constant i32 71684
@global_var_11804 = constant [44 x i8] c"Error creating SSL connection. Error Code: \00"
@global_var_11658 = local_unnamed_addr constant i32 71728
@global_var_11830 = constant [36 x i8] c"Call to SSL_get_verify_result is ok\00"
@global_var_1165c = local_unnamed_addr constant i32 139456
@global_var_220c0 = global i32 0
@global_var_11660 = local_unnamed_addr constant i32 71764
@global_var_11854 = constant [37 x i8] c"SSL communication established using \00"
@global_var_116e0 = local_unnamed_addr constant i32 65535
@global_var_116e4 = local_unnamed_addr constant i32 139796
@global_var_22214 = local_unnamed_addr global i32 0
@global_var_116e8 = local_unnamed_addr constant i32 139432
@global_var_220a8 = local_unnamed_addr global i32 0
@global_var_11708 = local_unnamed_addr constant i32 65535
@global_var_11764 = local_unnamed_addr constant i32 67528
@global_var_107c8 = local_unnamed_addr constant [16 x i8] c"treamIT_T0_ES6_\00"
@global_var_11768 = local_unnamed_addr constant i32 67516
@global_var_107bc = local_unnamed_addr constant [28 x i8] c"St13basic_ostreamIT_T0_ES6_\00"
@global_var_11724 = local_unnamed_addr constant i32 -509579264
@0 = external global i32
@global_var_1176c = local_unnamed_addr constant void ()* inttoptr (i32 -516948194 to void ()*)
@global_var_10eec = local_unnamed_addr constant void ()* inttoptr (i32 71436 to void ()*)
@global_var_1111c = local_unnamed_addr constant i16 443
@global_var_117d4 = local_unnamed_addr constant i8 0
@global_var_116ec = local_unnamed_addr constant void (i32*)* inttoptr (i32 69288 to void (i32*)*)

define i32 @function_10d04(i32 %arg1, i32 %arg2, i32* %arg3, i32 %arg4) local_unnamed_addr {
dec_label_pc_10d04:
  %0 = call i32 @SSL_set_fd(), !insn.addr !0
  ret i32 %0, !insn.addr !0
}

define i32 @function_10d1c(i32 %arg1) local_unnamed_addr {
dec_label_pc_10d1c:
  %0 = call i32 @SSL_new(), !insn.addr !1
  ret i32 %0, !insn.addr !1
}

define i32 @function_10d40(i32 %arg1, i32 %arg2, i32 %arg3) local_unnamed_addr {
dec_label_pc_10d40:
  %0 = call i32 @OPENSSL_init_ssl(), !insn.addr !2
  ret i32 %0, !insn.addr !2
}

define i32 @function_10d58() local_unnamed_addr {
dec_label_pc_10d58:
  %0 = call i32 @ERR_get_error(), !insn.addr !3
  ret i32 %0, !insn.addr !3
}

define i32 @function_10d7c(i32 %arg1) local_unnamed_addr {
dec_label_pc_10d7c:
  %0 = call i32 @SSL_CIPHER_get_name(), !insn.addr !4
  ret i32 %0, !insn.addr !4
}

define i32 @function_10d88(i32 %arg1) local_unnamed_addr {
dec_label_pc_10d88:
  %0 = call i32 @TLSv1_2_client_method(), !insn.addr !5
  ret i32 %0, !insn.addr !5
}

define i32 @function_10ddc(i32 %arg1) local_unnamed_addr {
dec_label_pc_10ddc:
  %0 = call i32 @SSL_CTX_new(), !insn.addr !6
  ret i32 %0, !insn.addr !6
}

define i32 @function_10de8(i32 %arg1) local_unnamed_addr {
dec_label_pc_10de8:
  %0 = call i32 @__cxa_end_cleanup(), !insn.addr !7
  ret i32 %0, !insn.addr !7
}

define i32 @function_10e00(i32 %arg1) local_unnamed_addr {
dec_label_pc_10e00:
  %0 = call i32 @SSL_connect(), !insn.addr !8
  ret i32 %0, !insn.addr !8
}

define i32 @function_10e0c(i32 %arg1, i32* %arg2, i32 %arg3, i32* %arg4) local_unnamed_addr {
dec_label_pc_10e0c:
  %0 = call i32 @SSL_CTX_set_cipher_list(), !insn.addr !9
  ret i32 %0, !insn.addr !9
}

define i32 @function_10e48(i32 %arg1) local_unnamed_addr {
dec_label_pc_10e48:
  %0 = call i32 @SSL_get_current_cipher(), !insn.addr !10
  ret i32 %0, !insn.addr !10
}

define i32 @function_10e60(i32 %arg1) local_unnamed_addr {
dec_label_pc_10e60:
  %0 = call i32 @SSL_get_verify_result(), !insn.addr !11
  ret i32 %0, !insn.addr !11
}

define i32 @function_10e78(i32 %arg1, i32 %arg2, i32* %arg3) local_unnamed_addr {
dec_label_pc_10e78:
  %0 = call i32 @SSL_CTX_set_verify(), !insn.addr !12
  ret i32 %0, !insn.addr !12
}

define i32 @function_10e84(i32 %arg1, i32 %arg2) local_unnamed_addr {
dec_label_pc_10e84:
  %0 = call i32 @ERR_error_string(), !insn.addr !13
  ret i32 %0, !insn.addr !13
}

define i32 @_Z9connectToNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEi(i32 %arg1, i32 %arg2) local_unnamed_addr {
dec_label_pc_10fa4:
  %r3.2.reg2mem = alloca i32, !insn.addr !14
  %stack_var_-28 = alloca i32, align 4
  %0 = call i32 @socket(i32 2, i32 1, i32 0), !insn.addr !15
  %1 = icmp eq i32 %0, 0, !insn.addr !16
  br i1 %1, label %dec_label_pc_10fe8, label %dec_label_pc_10ff8, !insn.addr !17

dec_label_pc_10fe8:                               ; preds = %dec_label_pc_10fa4
  %2 = call i32 @puts(i8* getelementptr inbounds ([23 x i8], [23 x i8]* @global_var_11780, i32 0, i32 0)), !insn.addr !18
  store i32 -1, i32* %r3.2.reg2mem, !insn.addr !19
  br label %dec_label_pc_110f8, !insn.addr !19

dec_label_pc_10ff8:                               ; preds = %dec_label_pc_10fa4
  %3 = call i32 @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(i32* nonnull @global_var_22180, i8* getelementptr inbounds ([15 x i8], [15 x i8]* @global_var_11798, i32 0, i32 0)), !insn.addr !20
  %4 = inttoptr i32 %3 to i32*, !insn.addr !21
  %5 = inttoptr i32 %arg1 to i32*, !insn.addr !21
  %6 = call i32 @_ZStlsIcSt11char_traitsIcESaIcEERSt13basic_ostreamIT_T0_ES7_RKNSt7__cxx1112basic_stringIS4_S5_T1_EE(i32* %4, i32* %5), !insn.addr !21
  %7 = inttoptr i32 %6 to i32*, !insn.addr !22
  %8 = call i32 @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(i32* %7, i8* getelementptr inbounds ([4 x i8], [4 x i8]* @global_var_117a8, i32 0, i32 0)), !insn.addr !22
  %9 = inttoptr i32 %8 to i32* (i32*)*, !insn.addr !23
  %10 = call i32 @_ZNSolsEPFRSoS_E(i32* (i32*)* %9), !insn.addr !23
  %11 = call i32* @memset(i32* nonnull %stack_var_-28, i32 0, i32 16), !insn.addr !24
  store i32 2, i32* %stack_var_-28, align 4, !insn.addr !25
  %12 = call i32 @_ZNKSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE5c_strEv(), !insn.addr !26
  %13 = inttoptr i32 %12 to i8*, !insn.addr !27
  %14 = call i32 @inet_addr(i8* %13), !insn.addr !27
  %15 = call i16 @htons(i16 443), !insn.addr !28
  %16 = bitcast i32* %stack_var_-28 to %sockaddr*, !insn.addr !29
  %17 = call i32 @connect(i32 %0, %sockaddr* nonnull %16, i32 16), !insn.addr !29
  %18 = icmp ne i32 %17, 0, !insn.addr !30
  %19 = icmp eq i1 %18, false, !insn.addr !31
  store i32 %0, i32* %r3.2.reg2mem, !insn.addr !32
  br i1 %19, label %dec_label_pc_110f8, label %dec_label_pc_110b4, !insn.addr !32

dec_label_pc_110b4:                               ; preds = %dec_label_pc_10ff8
  %20 = call i32 @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(i32* nonnull @global_var_22180, i8* getelementptr inbounds ([28 x i8], [28 x i8]* @global_var_117ac, i32 0, i32 0)), !insn.addr !33
  %21 = inttoptr i32 %20 to i32* (i32*)*, !insn.addr !34
  %22 = call i32 @_ZNSolsEPFRSoS_E(i32* (i32*)* %21), !insn.addr !34
  store i32 -1, i32* %r3.2.reg2mem, !insn.addr !35
  br label %dec_label_pc_110f8, !insn.addr !35

dec_label_pc_110f8:                               ; preds = %dec_label_pc_10fe8, %dec_label_pc_110b4, %dec_label_pc_10ff8
  %r3.2.reload = load i32, i32* %r3.2.reg2mem
  ret i32 %r3.2.reload, !insn.addr !36

; uselistorder directives
  uselistorder i32* %stack_var_-28, { 0, 2, 1 }
  uselistorder i32* %r3.2.reg2mem, { 0, 2, 1, 3 }
  uselistorder label %dec_label_pc_110f8, { 1, 2, 0 }
}

define i32 @_Z23failDisableVerificationP10ssl_ctx_st(i32* %arg1) local_unnamed_addr {
dec_label_pc_11124:
  %0 = ptrtoint i32* %arg1 to i32
  %1 = call i32 @function_10e78(i32 %0, i32 1, i32* nonnull @global_var_11664), !insn.addr !37
  ret i32 %1, !insn.addr !38
}

define i32 @_Z22failSetInsecureCiphersP10ssl_ctx_st(i32* %arg1) local_unnamed_addr {
dec_label_pc_11154:
  %0 = ptrtoint i32* %arg1 to i32
  %stack_var_-24 = alloca i32, align 4
  store i32 978078785, i32* %stack_var_-24, align 4, !insn.addr !39
  %1 = call i32 @function_10e0c(i32 %0, i32* nonnull %stack_var_-24, i32 0, i32* nonnull %stack_var_-24), !insn.addr !40
  ret i32 %1, !insn.addr !41
}

define i32 @_Z29failSetInsecureCiphersLiteralP10ssl_ctx_st(i32* %arg1) local_unnamed_addr {
dec_label_pc_111c8:
  %0 = alloca i32
  %1 = load i32, i32* %0
  %2 = load i32, i32* %0
  %3 = ptrtoint i32* %arg1 to i32
  %4 = inttoptr i32 %1 to i32*, !insn.addr !42
  %5 = call i32 @function_10e0c(i32 %3, i32* bitcast ([9 x i8]* @global_var_117cc to i32*), i32 %2, i32* %4), !insn.addr !42
  ret i32 %5, !insn.addr !43

; uselistorder directives
  uselistorder i32* %0, { 1, 0 }
}

define i32 @_Z25failSetInsecureCiphersSTLP10ssl_ctx_st(i32* %arg1) local_unnamed_addr {
dec_label_pc_111f4:
  %r0.0.reg2mem = alloca i32, !insn.addr !44
  %0 = ptrtoint i32* %arg1 to i32
  %stack_var_-36 = alloca i32, align 4
  %stack_var_-40 = alloca i32, align 4
  %1 = call i32 @_ZNSaIcEC1Ev(), !insn.addr !45
  %2 = ptrtoint i32* %stack_var_-40 to i32, !insn.addr !46
  %3 = bitcast i32* %stack_var_-36 to i8*, !insn.addr !47
  %4 = call i32 @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEC1EPKcRKS3_(i8* nonnull %3, i32* bitcast ([9 x i8]* @global_var_117cc to i32*)), !insn.addr !47
  %5 = call i32 @_ZNSaIcED1Ev(), !insn.addr !48
  %6 = call i32 @_ZNKSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE5c_strEv(), !insn.addr !49
  %7 = inttoptr i32 %6 to i32*, !insn.addr !50
  %8 = call i32 @function_10e0c(i32 %0, i32* %7, i32 %2, i32* %7), !insn.addr !50
  %9 = call i32 @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev(), !insn.addr !51
  store i32 %9, i32* %r0.0.reg2mem, !insn.addr !52
  br i1 icmp eq (i32 xor (i32 ptrtoint (i32* @global_var_21eec to i32), i32 ptrtoint (i32* @global_var_21eec to i32)), i32 0), label %dec_label_pc_112ac, label %dec_label_pc_112a8, !insn.addr !52

dec_label_pc_112a8:                               ; preds = %dec_label_pc_111f4
  call void @__stack_chk_fail(), !insn.addr !53
  store i32 ptrtoint (i32* @0 to i32), i32* %r0.0.reg2mem, !insn.addr !53
  br label %dec_label_pc_112ac, !insn.addr !53

dec_label_pc_112ac:                               ; preds = %dec_label_pc_112a8, %dec_label_pc_111f4
  %r0.0.reload = load i32, i32* %r0.0.reg2mem
  ret i32 %r0.0.reload, !insn.addr !54

; uselistorder directives
  uselistorder i32 ()* @_ZNKSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE5c_strEv, { 1, 0 }
}

define i32 @_Z28failSetInsecureCiphersGlobalP10ssl_ctx_st(i32* %arg1) local_unnamed_addr {
dec_label_pc_112bc:
  %0 = alloca i32
  %1 = load i32, i32* %0
  %2 = load i32, i32* %0
  %3 = ptrtoint i32* %arg1 to i32
  %4 = inttoptr i32 %1 to i32*, !insn.addr !55
  %5 = call i32 @function_10e0c(i32 %3, i32* bitcast ([4 x i8]* @global_var_220ac to i32*), i32 %2, i32* %4), !insn.addr !55
  ret i32 %5, !insn.addr !56

; uselistorder directives
  uselistorder i32* %0, { 1, 0 }
  uselistorder i32 (i32, i32*, i32, i32*)* @function_10e0c, { 3, 2, 1, 0 }
}

define i32 @_Z14initTLSContextv() local_unnamed_addr {
dec_label_pc_11308:
  %0 = call i32 @function_10d40(i32 0, i32 0, i32 0), !insn.addr !57
  %1 = call i32 @function_10d40(i32 2097154, i32 0, i32 0), !insn.addr !58
  %2 = call i32 @function_10d88(i32 %1), !insn.addr !59
  %3 = call i32 @function_10ddc(i32 %2), !insn.addr !60
  %4 = inttoptr i32 %3 to i32*, !insn.addr !61
  %5 = call i32 @_Z22failSetInsecureCiphersP10ssl_ctx_st(i32* %4), !insn.addr !61
  %6 = call i32 @_Z29failSetInsecureCiphersLiteralP10ssl_ctx_st(i32* %4), !insn.addr !62
  %7 = call i32 @_Z25failSetInsecureCiphersSTLP10ssl_ctx_st(i32* %4), !insn.addr !63
  %8 = call i32 @_Z28failSetInsecureCiphersGlobalP10ssl_ctx_st(i32* %4), !insn.addr !64
  %9 = call i32 @function_10e78(i32 %3, i32 1, i32* null), !insn.addr !65
  %10 = call i32 @_Z23failDisableVerificationP10ssl_ctx_st(i32* %4), !insn.addr !66
  ret i32 %3, !insn.addr !67

; uselistorder directives
  uselistorder i32 (i32, i32, i32*)* @function_10e78, { 1, 0 }
  uselistorder i32 (i32, i32, i32)* @function_10d40, { 1, 0 }
}

define i32 @main(i32 %argc, i8** %argv) local_unnamed_addr {
dec_label_pc_11398:
  %r3.2.reg2mem = alloca i32, !insn.addr !68
  %stack_var_-44 = alloca i32, align 4
  %0 = call i32 @_ZNSaIcEC1Ev(), !insn.addr !69
  %1 = bitcast i32* %stack_var_-44 to i8*, !insn.addr !70
  %2 = call i32 @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEC1EPKcRKS3_(i8* nonnull %1, i32* bitcast ([14 x i8]* @global_var_117e0 to i32*)), !insn.addr !70
  %3 = ptrtoint i32* %stack_var_-44 to i32, !insn.addr !71
  %4 = call i32 @_Z9connectToNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEi(i32 %3, i32 2), !insn.addr !72
  %5 = call i32 @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev(), !insn.addr !73
  %6 = call i32 @_ZNSaIcED1Ev(), !insn.addr !74
  %7 = icmp slt i32 %4, 0, !insn.addr !75
  store i32 -1, i32* %r3.2.reg2mem, !insn.addr !76
  br i1 %7, label %dec_label_pc_11630, label %dec_label_pc_11420, !insn.addr !76

dec_label_pc_11420:                               ; preds = %dec_label_pc_11398
  %8 = call i32 @_Z14initTLSContextv(), !insn.addr !77
  %9 = call i32 @function_10d1c(i32 %8), !insn.addr !78
  store i32 %9, i32* @global_var_22210, align 4, !insn.addr !79
  %10 = icmp eq i32 %9, 0, !insn.addr !80
  br i1 %10, label %dec_label_pc_11450, label %dec_label_pc_11474, !insn.addr !81

dec_label_pc_11450:                               ; preds = %dec_label_pc_11420
  %11 = call i32 @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(i32* nonnull @global_var_22180, i8* getelementptr inbounds ([20 x i8], [20 x i8]* @global_var_117f0, i32 0, i32 0)), !insn.addr !82
  %12 = inttoptr i32 %11 to i32* (i32*)*, !insn.addr !83
  %13 = call i32 @_ZNSolsEPFRSoS_E(i32* (i32*)* %12), !insn.addr !83
  store i32 -1, i32* %r3.2.reg2mem, !insn.addr !84
  br label %dec_label_pc_11630, !insn.addr !84

dec_label_pc_11474:                               ; preds = %dec_label_pc_11420
  %14 = call i32 @function_10d04(i32 %9, i32 %4, i32* nonnull @global_var_22210, i32 %9), !insn.addr !85
  %15 = load i32, i32* @global_var_22210, align 4, !insn.addr !86
  %16 = call i32 @function_10e00(i32 %15), !insn.addr !87
  %17 = icmp sgt i32 %16, 0, !insn.addr !88
  br i1 %17, label %dec_label_pc_11558, label %dec_label_pc_114ac, !insn.addr !88

dec_label_pc_114ac:                               ; preds = %dec_label_pc_11474
  %18 = call i32 @function_10d58(), !insn.addr !89
  %19 = call i32 @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(i32* nonnull @global_var_22180, i8* getelementptr inbounds ([44 x i8], [44 x i8]* @global_var_11804, i32 0, i32 0)), !insn.addr !90
  %20 = call i32 @function_10e84(i32 %18, i32 0), !insn.addr !91
  %21 = inttoptr i32 %19 to i32*, !insn.addr !92
  %22 = inttoptr i32 %20 to i8*, !insn.addr !92
  %23 = call i32 @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(i32* %21, i8* %22), !insn.addr !92
  %24 = inttoptr i32 %23 to i32* (i32*)*, !insn.addr !93
  %25 = call i32 @_ZNSolsEPFRSoS_E(i32* (i32*)* %24), !insn.addr !93
  store i32 -1, i32* %r3.2.reg2mem, !insn.addr !94
  br label %dec_label_pc_11630, !insn.addr !94

dec_label_pc_11558:                               ; preds = %dec_label_pc_11474
  %26 = load i32, i32* @global_var_22210, align 4, !insn.addr !95
  %27 = call i32 @function_10e60(i32 %26), !insn.addr !96
  %28 = icmp eq i32 %27, 0, !insn.addr !97
  %29 = icmp eq i1 %28, false, !insn.addr !98
  br i1 %29, label %dec_label_pc_115a0, label %dec_label_pc_11584, !insn.addr !99

dec_label_pc_11584:                               ; preds = %dec_label_pc_11558
  %30 = call i32 @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(i32* nonnull @global_var_220c0, i8* getelementptr inbounds ([36 x i8], [36 x i8]* @global_var_11830, i32 0, i32 0)), !insn.addr !100
  %31 = inttoptr i32 %30 to i32* (i32*)*, !insn.addr !101
  %32 = call i32 @_ZNSolsEPFRSoS_E(i32* (i32*)* %31), !insn.addr !101
  br label %dec_label_pc_115a0, !insn.addr !101

dec_label_pc_115a0:                               ; preds = %dec_label_pc_11584, %dec_label_pc_11558
  %33 = call i32 @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(i32* nonnull @global_var_220c0, i8* getelementptr inbounds ([37 x i8], [37 x i8]* @global_var_11854, i32 0, i32 0)), !insn.addr !102
  %34 = load i32, i32* @global_var_22210, align 4, !insn.addr !103
  %35 = call i32 @function_10e48(i32 %34), !insn.addr !104
  %36 = call i32 @function_10d7c(i32 %35), !insn.addr !105
  %37 = inttoptr i32 %33 to i32*, !insn.addr !106
  %38 = inttoptr i32 %36 to i8*, !insn.addr !106
  %39 = call i32 @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(i32* %37, i8* %38), !insn.addr !106
  %40 = inttoptr i32 %39 to i32* (i32*)*, !insn.addr !107
  %41 = call i32 @_ZNSolsEPFRSoS_E(i32* (i32*)* %40), !insn.addr !107
  store i32 0, i32* %r3.2.reg2mem, !insn.addr !108
  br label %dec_label_pc_11630, !insn.addr !108

dec_label_pc_11630:                               ; preds = %dec_label_pc_11450, %dec_label_pc_114ac, %dec_label_pc_115a0, %dec_label_pc_11398
  %r3.2.reload = load i32, i32* %r3.2.reg2mem
  ret i32 %r3.2.reload, !insn.addr !109

; uselistorder directives
  uselistorder i32* %stack_var_-44, { 1, 0 }
  uselistorder i32* %r3.2.reg2mem, { 0, 3, 2, 4, 1 }
  uselistorder i32 (i32* (i32*)*)* @_ZNSolsEPFRSoS_E, { 5, 4, 3, 2, 1, 0 }
  uselistorder i32 (i32*, i8*)* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc, { 8, 7, 6, 5, 4, 3, 2, 1, 0 }
  uselistorder i32* @global_var_22210, { 2, 3, 4, 0, 1 }
  uselistorder i32 -1, { 1, 2, 0, 3, 4 }
  uselistorder i32 0, { 0, 1, 2, 3, 4, 36, 19, 5, 6, 20, 7, 8, 37, 38, 22, 21, 25, 23, 24, 9, 10, 11, 12, 39, 26, 13, 14, 15, 16, 17, 18, 40, 27, 28, 29, 30, 31, 32, 33, 34, 35 }
  uselistorder i32 ()* @_ZNSaIcED1Ev, { 1, 0 }
  uselistorder i32 ()* @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev, { 1, 0 }
  uselistorder i32 2, { 1, 0, 2 }
  uselistorder i32 (i8*, i32*)* @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEC1EPKcRKS3_, { 1, 0 }
  uselistorder i32 ()* @_ZNSaIcEC1Ev, { 1, 0 }
  uselistorder i32 1, { 8, 2, 5, 1, 10, 9, 3, 0, 11, 6, 7, 12, 4 }
  uselistorder label %dec_label_pc_11630, { 2, 1, 0, 3 }
}

declare i32 @SSL_set_fd() local_unnamed_addr

declare i32 @inet_addr(i8*) local_unnamed_addr

declare i32 @SSL_new() local_unnamed_addr

declare i32 @_ZNKSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE5c_strEv() local_unnamed_addr

declare i32 @OPENSSL_init_ssl() local_unnamed_addr

declare i32 @puts(i8*) local_unnamed_addr

declare i32 @ERR_get_error() local_unnamed_addr

declare i32 @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev() local_unnamed_addr

declare i32* @memset(i32*, i32, i32) local_unnamed_addr

declare i32 @SSL_CIPHER_get_name() local_unnamed_addr

declare i32 @TLSv1_2_client_method() local_unnamed_addr

declare i32 @_ZStlsIcSt11char_traitsIcESaIcEERSt13basic_ostreamIT_T0_ES7_RKNSt7__cxx1112basic_stringIS4_S5_T1_EE(i32*, i32*) local_unnamed_addr

declare i32 @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(i32*, i8*) local_unnamed_addr

declare i32 @_ZNSolsEPFRSoS_E(i32* (i32*)*) local_unnamed_addr

declare i32 @_ZNSaIcED1Ev() local_unnamed_addr

declare void @__stack_chk_fail() local_unnamed_addr

declare i32 @SSL_CTX_new() local_unnamed_addr

declare i32 @__cxa_end_cleanup() local_unnamed_addr

declare i32 @socket(i32, i32, i32) local_unnamed_addr

declare i32 @SSL_connect() local_unnamed_addr

declare i32 @SSL_CTX_set_cipher_list() local_unnamed_addr

declare i32 @connect(i32, %sockaddr*, i32) local_unnamed_addr

declare i32 @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEC1EPKcRKS3_(i8*, i32*) local_unnamed_addr

declare i16 @htons(i16) local_unnamed_addr

declare i32 @SSL_get_current_cipher() local_unnamed_addr

declare i32 @SSL_get_verify_result() local_unnamed_addr

declare i32 @_ZNSaIcEC1Ev() local_unnamed_addr

declare i32 @SSL_CTX_set_verify() local_unnamed_addr

declare i32 @ERR_error_string() local_unnamed_addr

!0 = !{i64 68876}
!1 = !{i64 68900}
!2 = !{i64 68936}
!3 = !{i64 68960}
!4 = !{i64 68996}
!5 = !{i64 69008}
!6 = !{i64 69092}
!7 = !{i64 69104}
!8 = !{i64 69128}
!9 = !{i64 69140}
!10 = !{i64 69200}
!11 = !{i64 69224}
!12 = !{i64 69248}
!13 = !{i64 69260}
!14 = !{i64 69540}
!15 = !{i64 69588}
!16 = !{i64 69600}
!17 = !{i64 69604}
!18 = !{i64 69612}
!19 = !{i64 69620}
!20 = !{i64 69632}
!21 = !{i64 69648}
!22 = !{i64 69664}
!23 = !{i64 69680}
!24 = !{i64 69700}
!25 = !{i64 69708}
!26 = !{i64 69716}
!27 = !{i64 69728}
!28 = !{i64 69744}
!29 = !{i64 69780}
!30 = !{i64 69788}
!31 = !{i64 69804}
!32 = !{i64 69808}
!33 = !{i64 69820}
!34 = !{i64 69836}
!35 = !{i64 69844}
!36 = !{i64 69888}
!37 = !{i64 69952}
!38 = !{i64 69964}
!39 = !{i64 70016}
!40 = !{i64 70036}
!41 = !{i64 70076}
!42 = !{i64 70112}
!43 = !{i64 70124}
!44 = !{i64 70132}
!45 = !{i64 70172}
!46 = !{i64 70176}
!47 = !{i64 70192}
!48 = !{i64 70204}
!49 = !{i64 70216}
!50 = !{i64 70232}
!51 = !{i64 70244}
!52 = !{i64 70272}
!53 = !{i64 70312}
!54 = !{i64 70320}
!55 = !{i64 70356}
!56 = !{i64 70368}
!57 = !{i64 70432}
!58 = !{i64 70448}
!59 = !{i64 70452}
!60 = !{i64 70464}
!61 = !{i64 70480}
!62 = !{i64 70488}
!63 = !{i64 70496}
!64 = !{i64 70504}
!65 = !{i64 70520}
!66 = !{i64 70528}
!67 = !{i64 70544}
!68 = !{i64 70552}
!69 = !{i64 70596}
!70 = !{i64 70616}
!71 = !{i64 70628}
!72 = !{i64 70632}
!73 = !{i64 70652}
!74 = !{i64 70664}
!75 = !{i64 70672}
!76 = !{i64 70676}
!77 = !{i64 70688}
!78 = !{i64 70704}
!79 = !{i64 70716}
!80 = !{i64 70728}
!81 = !{i64 70732}
!82 = !{i64 70744}
!83 = !{i64 70760}
!84 = !{i64 70768}
!85 = !{i64 70788}
!86 = !{i64 70796}
!87 = !{i64 70804}
!88 = !{i64 70824}
!89 = !{i64 70828}
!90 = !{i64 70848}
!91 = !{i64 70868}
!92 = !{i64 70884}
!93 = !{i64 70900}
!94 = !{i64 70908}
!95 = !{i64 71004}
!96 = !{i64 71012}
!97 = !{i64 71020}
!98 = !{i64 71036}
!99 = !{i64 71040}
!100 = !{i64 71052}
!101 = !{i64 71068}
!102 = !{i64 71080}
!103 = !{i64 71092}
!104 = !{i64 71100}
!105 = !{i64 71112}
!106 = !{i64 71128}
!107 = !{i64 71144}
!108 = !{i64 71148}
!109 = !{i64 71224}
