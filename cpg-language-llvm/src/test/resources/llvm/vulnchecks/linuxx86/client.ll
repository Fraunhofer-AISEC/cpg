; ModuleID = '../client.cpp'
source_filename = "../client.cpp"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

%"class.std::ios_base::Init" = type { i8 }
%struct.ssl_st = type opaque
%"class.std::basic_ostream" = type { i32 (...)**, %"class.std::basic_ios" }
%"class.std::basic_ios" = type { %"class.std::ios_base", %"class.std::basic_ostream"*, i8, i8, %"class.std::basic_streambuf"*, %"class.std::ctype"*, %"class.std::num_put"*, %"class.std::num_get"* }
%"class.std::ios_base" = type { i32 (...)**, i64, i64, i32, i32, i32, %"struct.std::ios_base::_Callback_list"*, %"struct.std::ios_base::_Words", [8 x %"struct.std::ios_base::_Words"], i32, %"struct.std::ios_base::_Words"*, %"class.std::locale" }
%"struct.std::ios_base::_Callback_list" = type { %"struct.std::ios_base::_Callback_list"*, void (i32, %"class.std::ios_base"*, i32)*, i32, i32 }
%"struct.std::ios_base::_Words" = type { i8*, i64 }
%"class.std::locale" = type { %"class.std::locale::_Impl"* }
%"class.std::locale::_Impl" = type { i32, %"class.std::locale::facet"**, i64, %"class.std::locale::facet"**, i8** }
%"class.std::locale::facet" = type <{ i32 (...)**, i32, [4 x i8] }>
%"class.std::basic_streambuf" = type { i32 (...)**, i8*, i8*, i8*, i8*, i8*, i8*, %"class.std::locale" }
%"class.std::ctype" = type <{ %"class.std::locale::facet.base", [4 x i8], %struct.__locale_struct*, i8, [7 x i8], i32*, i32*, i16*, i8, [256 x i8], [256 x i8], i8, [6 x i8] }>
%"class.std::locale::facet.base" = type <{ i32 (...)**, i32 }>
%struct.__locale_struct = type { [13 x %struct.__locale_data*], i16*, i32*, i32*, [13 x i8*] }
%struct.__locale_data = type opaque
%"class.std::num_put" = type { %"class.std::locale::facet.base", [4 x i8] }
%"class.std::num_get" = type { %"class.std::locale::facet.base", [4 x i8] }
%"class.std::__cxx11::basic_string" = type { %"struct.std::__cxx11::basic_string<char, std::char_traits<char>, std::allocator<char> >::_Alloc_hider", i64, %union.anon }
%"struct.std::__cxx11::basic_string<char, std::char_traits<char>, std::allocator<char> >::_Alloc_hider" = type { i8* }
%union.anon = type { i64, [8 x i8] }
%struct.sockaddr_in = type { i16, i16, %struct.in_addr, [8 x i8] }
%struct.in_addr = type { i32 }
%struct.sockaddr = type { i16, [14 x i8] }
%struct.ssl_ctx_st = type opaque
%struct.x509_store_ctx_st = type opaque
%"class.std::allocator" = type { i8 }
%struct.ossl_init_settings_st = type opaque
%struct.ssl_method_st = type opaque
%struct.ssl_cipher_st = type opaque

@_ZStL8__ioinit = internal global %"class.std::ios_base::Init" zeroinitializer, align 1
@__dso_handle = external hidden global i8
@ssl = dso_local global %struct.ssl_st* null, align 8
@bad_ciphers = dso_local global [4 x i8] c"MD5\00", align 1
@.str = private unnamed_addr constant [24 x i8] c"Error creating socket.\0A\00", align 1
@_ZSt4cerr = external dso_local global %"class.std::basic_ostream", align 8
@.str.1 = private unnamed_addr constant [15 x i8] c"Connecting to \00", align 1
@.str.2 = private unnamed_addr constant [4 x i8] c"...\00", align 1
@.str.3 = private unnamed_addr constant [28 x i8] c"Error connecting to server.\00", align 1
@__const._Z22failSetInsecureCiphersP10ssl_ctx_st.ciphers = private unnamed_addr constant [9 x i8] c"ALL:!ADH\00", align 1
@.str.4 = private unnamed_addr constant [9 x i8] c"ALL:!ADH\00", align 1
@.str.5 = private unnamed_addr constant [14 x i8] c"172.217.18.99\00", align 1
@.str.6 = private unnamed_addr constant [20 x i8] c"Error creating SSL.\00", align 1
@.str.7 = private unnamed_addr constant [44 x i8] c"Error creating SSL connection. Error Code: \00", align 1
@_ZSt4cout = external dso_local global %"class.std::basic_ostream", align 8
@.str.8 = private unnamed_addr constant [36 x i8] c"Call to SSL_get_verify_result is ok\00", align 1
@.str.9 = private unnamed_addr constant [37 x i8] c"SSL communication established using \00", align 1
@llvm.global_ctors = appending global [1 x { i32, void ()*, i8* }] [{ i32, void ()*, i8* } { i32 65535, void ()* @_GLOBAL__sub_I_client.cpp, i8* null }]

; Function Attrs: noinline uwtable
define internal void @__cxx_global_var_init() #0 section ".text.startup" {
  call void @_ZNSt8ios_base4InitC1Ev(%"class.std::ios_base::Init"* @_ZStL8__ioinit)
  %1 = call i32 @__cxa_atexit(void (i8*)* bitcast (void (%"class.std::ios_base::Init"*)* @_ZNSt8ios_base4InitD1Ev to void (i8*)*), i8* getelementptr inbounds (%"class.std::ios_base::Init", %"class.std::ios_base::Init"* @_ZStL8__ioinit, i32 0, i32 0), i8* @__dso_handle) #3
  ret void
}

declare dso_local void @_ZNSt8ios_base4InitC1Ev(%"class.std::ios_base::Init"*) unnamed_addr #1

; Function Attrs: nounwind
declare dso_local void @_ZNSt8ios_base4InitD1Ev(%"class.std::ios_base::Init"*) unnamed_addr #2

; Function Attrs: nounwind
declare dso_local i32 @__cxa_atexit(void (i8*)*, i8*, i8*) #3

; Function Attrs: noinline optnone uwtable
define dso_local i32 @_Z9connectToNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEi(%"class.std::__cxx11::basic_string"* %0, i32 %1) #4 {
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca i32, align 4
  %6 = alloca %struct.sockaddr_in, align 4
  %7 = alloca i32, align 4
  store i32 %1, i32* %4, align 4
  %8 = call i32 @socket(i32 2, i32 1, i32 0) #3
  store i32 %8, i32* %5, align 4
  %9 = load i32, i32* %5, align 4
  %10 = icmp ne i32 %9, 0
  br i1 %10, label %13, label %11

11:                                               ; preds = %2
  %12 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([24 x i8], [24 x i8]* @.str, i64 0, i64 0))
  store i32 -1, i32* %3, align 4
  br label %35

13:                                               ; preds = %2
  %14 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) @_ZSt4cerr, i8* getelementptr inbounds ([15 x i8], [15 x i8]* @.str.1, i64 0, i64 0))
  %15 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsIcSt11char_traitsIcESaIcEERSt13basic_ostreamIT_T0_ES7_RKNSt7__cxx1112basic_stringIS4_S5_T1_EE(%"class.std::basic_ostream"* dereferenceable(272) %14, %"class.std::__cxx11::basic_string"* dereferenceable(32) %0)
  %16 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) %15, i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str.2, i64 0, i64 0))
  %17 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZNSolsEPFRSoS_E(%"class.std::basic_ostream"* %16, %"class.std::basic_ostream"* (%"class.std::basic_ostream"*)* @_ZSt4endlIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_)
  %18 = bitcast %struct.sockaddr_in* %6 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 4 %18, i8 0, i64 16, i1 false)
  %19 = getelementptr inbounds %struct.sockaddr_in, %struct.sockaddr_in* %6, i32 0, i32 0
  store i16 2, i16* %19, align 4
  %20 = call i8* @_ZNKSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE5c_strEv(%"class.std::__cxx11::basic_string"* %0) #3
  %21 = call i32 @inet_addr(i8* %20) #3
  %22 = getelementptr inbounds %struct.sockaddr_in, %struct.sockaddr_in* %6, i32 0, i32 2
  %23 = getelementptr inbounds %struct.in_addr, %struct.in_addr* %22, i32 0, i32 0
  store i32 %21, i32* %23, align 4
  %24 = call zeroext i16 @htons(i16 zeroext 443) #9
  %25 = getelementptr inbounds %struct.sockaddr_in, %struct.sockaddr_in* %6, i32 0, i32 1
  store i16 %24, i16* %25, align 2
  store i32 16, i32* %7, align 4
  %26 = load i32, i32* %5, align 4
  %27 = bitcast %struct.sockaddr_in* %6 to %struct.sockaddr*
  %28 = call i32 @connect(i32 %26, %struct.sockaddr* %27, i32 16)
  %29 = icmp ne i32 %28, 0
  br i1 %29, label %30, label %33

30:                                               ; preds = %13
  %31 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) @_ZSt4cerr, i8* getelementptr inbounds ([28 x i8], [28 x i8]* @.str.3, i64 0, i64 0))
  %32 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZNSolsEPFRSoS_E(%"class.std::basic_ostream"* %31, %"class.std::basic_ostream"* (%"class.std::basic_ostream"*)* @_ZSt4endlIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_)
  store i32 -1, i32* %3, align 4
  br label %35

33:                                               ; preds = %13
  %34 = load i32, i32* %5, align 4
  store i32 %34, i32* %3, align 4
  br label %35

35:                                               ; preds = %33, %30, %11
  %36 = load i32, i32* %3, align 4
  ret i32 %36
}

; Function Attrs: nounwind
declare dso_local i32 @socket(i32, i32, i32) #2

declare dso_local i32 @printf(i8*, ...) #1

declare dso_local dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272), i8*) #1

declare dso_local dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsIcSt11char_traitsIcESaIcEERSt13basic_ostreamIT_T0_ES7_RKNSt7__cxx1112basic_stringIS4_S5_T1_EE(%"class.std::basic_ostream"* dereferenceable(272), %"class.std::__cxx11::basic_string"* dereferenceable(32)) #1

declare dso_local dereferenceable(272) %"class.std::basic_ostream"* @_ZNSolsEPFRSoS_E(%"class.std::basic_ostream"*, %"class.std::basic_ostream"* (%"class.std::basic_ostream"*)*) #1

declare dso_local dereferenceable(272) %"class.std::basic_ostream"* @_ZSt4endlIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_(%"class.std::basic_ostream"* dereferenceable(272)) #1

; Function Attrs: argmemonly nounwind willreturn
declare void @llvm.memset.p0i8.i64(i8* nocapture writeonly, i8, i64, i1 immarg) #5

; Function Attrs: nounwind
declare dso_local i32 @inet_addr(i8*) #2

; Function Attrs: nounwind
declare dso_local i8* @_ZNKSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE5c_strEv(%"class.std::__cxx11::basic_string"*) #2

; Function Attrs: nounwind readnone
declare dso_local zeroext i16 @htons(i16 zeroext) #6

declare dso_local i32 @connect(i32, %struct.sockaddr*, i32) #1

; Function Attrs: noinline optnone uwtable
define dso_local void @_Z23failDisableVerificationP10ssl_ctx_st(%struct.ssl_ctx_st* %0) #4 {
  %2 = alloca %struct.ssl_ctx_st*, align 8
  store %struct.ssl_ctx_st* %0, %struct.ssl_ctx_st** %2, align 8
  %3 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %2, align 8
  call void @SSL_CTX_set_verify(%struct.ssl_ctx_st* %3, i32 1, i32 (i32, %struct.x509_store_ctx_st*)* @_Z10callMeBackiP17x509_store_ctx_st)
  ret void
}

declare dso_local void @SSL_CTX_set_verify(%struct.ssl_ctx_st*, i32, i32 (i32, %struct.x509_store_ctx_st*)*) #1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @_Z10callMeBackiP17x509_store_ctx_st(i32 %0, %struct.x509_store_ctx_st* %1) #7 {
  %3 = alloca i32, align 4
  %4 = alloca %struct.x509_store_ctx_st*, align 8
  store i32 %0, i32* %3, align 4
  store %struct.x509_store_ctx_st* %1, %struct.x509_store_ctx_st** %4, align 8
  ret i32 1
}

; Function Attrs: noinline optnone uwtable
define dso_local void @_Z22failSetInsecureCiphersP10ssl_ctx_st(%struct.ssl_ctx_st* %0) #4 {
  %2 = alloca %struct.ssl_ctx_st*, align 8
  %3 = alloca [9 x i8], align 1
  store %struct.ssl_ctx_st* %0, %struct.ssl_ctx_st** %2, align 8
  %4 = bitcast [9 x i8]* %3 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %4, i8* align 1 getelementptr inbounds ([9 x i8], [9 x i8]* @__const._Z22failSetInsecureCiphersP10ssl_ctx_st.ciphers, i32 0, i32 0), i64 9, i1 false)
  %5 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %2, align 8
  %6 = getelementptr inbounds [9 x i8], [9 x i8]* %3, i64 0, i64 0
  %7 = call i32 @SSL_CTX_set_cipher_list(%struct.ssl_ctx_st* %5, i8* %6)
  ret void
}

; Function Attrs: argmemonly nounwind willreturn
declare void @llvm.memcpy.p0i8.p0i8.i64(i8* noalias nocapture writeonly, i8* noalias nocapture readonly, i64, i1 immarg) #5

declare dso_local i32 @SSL_CTX_set_cipher_list(%struct.ssl_ctx_st*, i8*) #1

; Function Attrs: noinline optnone uwtable
define dso_local void @_Z29failSetInsecureCiphersLiteralP10ssl_ctx_st(%struct.ssl_ctx_st* %0) #4 {
  %2 = alloca %struct.ssl_ctx_st*, align 8
  store %struct.ssl_ctx_st* %0, %struct.ssl_ctx_st** %2, align 8
  %3 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %2, align 8
  %4 = call i32 @SSL_CTX_set_cipher_list(%struct.ssl_ctx_st* %3, i8* getelementptr inbounds ([9 x i8], [9 x i8]* @.str.4, i64 0, i64 0))
  ret void
}

; Function Attrs: noinline optnone uwtable
define dso_local void @_Z25failSetInsecureCiphersSTLP10ssl_ctx_st(%struct.ssl_ctx_st* %0) #4 personality i8* bitcast (i32 (...)* @__gxx_personality_v0 to i8*) {
  %2 = alloca %struct.ssl_ctx_st*, align 8
  %3 = alloca %"class.std::__cxx11::basic_string", align 8
  %4 = alloca %"class.std::allocator", align 1
  %5 = alloca i8*
  %6 = alloca i32
  store %struct.ssl_ctx_st* %0, %struct.ssl_ctx_st** %2, align 8
  call void @_ZNSaIcEC1Ev(%"class.std::allocator"* %4) #3
  invoke void @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEC1EPKcRKS3_(%"class.std::__cxx11::basic_string"* %3, i8* getelementptr inbounds ([9 x i8], [9 x i8]* @.str.4, i64 0, i64 0), %"class.std::allocator"* dereferenceable(1) %4)
          to label %7 unwind label %12

7:                                                ; preds = %1
  call void @_ZNSaIcED1Ev(%"class.std::allocator"* %4) #3
  %8 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %2, align 8
  %9 = call i8* @_ZNKSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE5c_strEv(%"class.std::__cxx11::basic_string"* %3) #3
  %10 = invoke i32 @SSL_CTX_set_cipher_list(%struct.ssl_ctx_st* %8, i8* %9)
          to label %11 unwind label %16

11:                                               ; preds = %7
  call void @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev(%"class.std::__cxx11::basic_string"* %3) #3
  ret void

12:                                               ; preds = %1
  %13 = landingpad { i8*, i32 }
          cleanup
  %14 = extractvalue { i8*, i32 } %13, 0
  store i8* %14, i8** %5, align 8
  %15 = extractvalue { i8*, i32 } %13, 1
  store i32 %15, i32* %6, align 4
  call void @_ZNSaIcED1Ev(%"class.std::allocator"* %4) #3
  br label %20

16:                                               ; preds = %7
  %17 = landingpad { i8*, i32 }
          cleanup
  %18 = extractvalue { i8*, i32 } %17, 0
  store i8* %18, i8** %5, align 8
  %19 = extractvalue { i8*, i32 } %17, 1
  store i32 %19, i32* %6, align 4
  call void @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev(%"class.std::__cxx11::basic_string"* %3) #3
  br label %20

20:                                               ; preds = %16, %12
  %21 = load i8*, i8** %5, align 8
  %22 = load i32, i32* %6, align 4
  %23 = insertvalue { i8*, i32 } undef, i8* %21, 0
  %24 = insertvalue { i8*, i32 } %23, i32 %22, 1
  resume { i8*, i32 } %24
}

; Function Attrs: nounwind
declare dso_local void @_ZNSaIcEC1Ev(%"class.std::allocator"*) unnamed_addr #2

declare dso_local void @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEC1EPKcRKS3_(%"class.std::__cxx11::basic_string"*, i8*, %"class.std::allocator"* dereferenceable(1)) unnamed_addr #1

declare dso_local i32 @__gxx_personality_v0(...)

; Function Attrs: nounwind
declare dso_local void @_ZNSaIcED1Ev(%"class.std::allocator"*) unnamed_addr #2

; Function Attrs: nounwind
declare dso_local void @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev(%"class.std::__cxx11::basic_string"*) unnamed_addr #2

; Function Attrs: noinline optnone uwtable
define dso_local void @_Z28failSetInsecureCiphersGlobalP10ssl_ctx_st(%struct.ssl_ctx_st* %0) #4 {
  %2 = alloca %struct.ssl_ctx_st*, align 8
  store %struct.ssl_ctx_st* %0, %struct.ssl_ctx_st** %2, align 8
  %3 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %2, align 8
  %4 = call i32 @SSL_CTX_set_cipher_list(%struct.ssl_ctx_st* %3, i8* getelementptr inbounds ([4 x i8], [4 x i8]* @bad_ciphers, i64 0, i64 0))
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @_Z29failDisableVerificationLambdaP10ssl_ctx_st(%struct.ssl_ctx_st* %0) #7 {
  %2 = alloca %struct.ssl_ctx_st*, align 8
  store %struct.ssl_ctx_st* %0, %struct.ssl_ctx_st** %2, align 8
  ret void
}

; Function Attrs: noinline optnone uwtable
define dso_local %struct.ssl_ctx_st* @_Z14initTLSContextv() #4 {
  %1 = alloca %struct.ssl_ctx_st*, align 8
  %2 = call i32 @OPENSSL_init_ssl(i64 0, %struct.ossl_init_settings_st* null)
  %3 = call i32 @OPENSSL_init_ssl(i64 2097154, %struct.ossl_init_settings_st* null)
  %4 = call %struct.ssl_method_st* @TLSv1_2_client_method()
  %5 = call %struct.ssl_ctx_st* @SSL_CTX_new(%struct.ssl_method_st* %4)
  store %struct.ssl_ctx_st* %5, %struct.ssl_ctx_st** %1, align 8
  %6 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %1, align 8
  call void @_Z22failSetInsecureCiphersP10ssl_ctx_st(%struct.ssl_ctx_st* %6)
  %7 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %1, align 8
  call void @_Z29failSetInsecureCiphersLiteralP10ssl_ctx_st(%struct.ssl_ctx_st* %7)
  %8 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %1, align 8
  call void @_Z25failSetInsecureCiphersSTLP10ssl_ctx_st(%struct.ssl_ctx_st* %8)
  %9 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %1, align 8
  call void @_Z28failSetInsecureCiphersGlobalP10ssl_ctx_st(%struct.ssl_ctx_st* %9)
  %10 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %1, align 8
  call void @SSL_CTX_set_verify(%struct.ssl_ctx_st* %10, i32 1, i32 (i32, %struct.x509_store_ctx_st*)* null)
  %11 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %1, align 8
  call void @_Z23failDisableVerificationP10ssl_ctx_st(%struct.ssl_ctx_st* %11)
  %12 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %1, align 8
  ret %struct.ssl_ctx_st* %12
}

declare dso_local i32 @OPENSSL_init_ssl(i64, %struct.ossl_init_settings_st*) #1

declare dso_local %struct.ssl_ctx_st* @SSL_CTX_new(%struct.ssl_method_st*) #1

declare dso_local %struct.ssl_method_st* @TLSv1_2_client_method() #1

; Function Attrs: noinline norecurse optnone uwtable
define dso_local i32 @main(i32 %0, i8** %1) #8 personality i8* bitcast (i32 (...)* @__gxx_personality_v0 to i8*) {
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca i8**, align 8
  %6 = alloca i32, align 4
  %7 = alloca %"class.std::__cxx11::basic_string", align 8
  %8 = alloca %"class.std::allocator", align 1
  %9 = alloca i8*
  %10 = alloca i32
  %11 = alloca %struct.ssl_ctx_st*, align 8
  %12 = alloca i32, align 4
  %13 = alloca i32, align 4
  store i32 0, i32* %3, align 4
  store i32 %0, i32* %4, align 4
  store i8** %1, i8*** %5, align 8
  call void @_ZNSaIcEC1Ev(%"class.std::allocator"* %8) #3
  invoke void @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEC1EPKcRKS3_(%"class.std::__cxx11::basic_string"* %7, i8* getelementptr inbounds ([14 x i8], [14 x i8]* @.str.5, i64 0, i64 0), %"class.std::allocator"* dereferenceable(1) %8)
          to label %14 unwind label %20

14:                                               ; preds = %2
  %15 = invoke i32 @_Z9connectToNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEi(%"class.std::__cxx11::basic_string"* %7, i32 2)
          to label %16 unwind label %24

16:                                               ; preds = %14
  call void @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev(%"class.std::__cxx11::basic_string"* %7) #3
  call void @_ZNSaIcED1Ev(%"class.std::allocator"* %8) #3
  store i32 %15, i32* %6, align 4
  %17 = load i32, i32* %6, align 4
  %18 = icmp slt i32 %17, 0
  br i1 %18, label %19, label %29

19:                                               ; preds = %16
  store i32 -1, i32* %3, align 4
  br label %78

20:                                               ; preds = %2
  %21 = landingpad { i8*, i32 }
          cleanup
  %22 = extractvalue { i8*, i32 } %21, 0
  store i8* %22, i8** %9, align 8
  %23 = extractvalue { i8*, i32 } %21, 1
  store i32 %23, i32* %10, align 4
  br label %28

24:                                               ; preds = %14
  %25 = landingpad { i8*, i32 }
          cleanup
  %26 = extractvalue { i8*, i32 } %25, 0
  store i8* %26, i8** %9, align 8
  %27 = extractvalue { i8*, i32 } %25, 1
  store i32 %27, i32* %10, align 4
  call void @_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev(%"class.std::__cxx11::basic_string"* %7) #3
  br label %28

28:                                               ; preds = %24, %20
  call void @_ZNSaIcED1Ev(%"class.std::allocator"* %8) #3
  br label %80

29:                                               ; preds = %16
  %30 = call %struct.ssl_ctx_st* @_Z14initTLSContextv()
  store %struct.ssl_ctx_st* %30, %struct.ssl_ctx_st** %11, align 8
  %31 = load %struct.ssl_ctx_st*, %struct.ssl_ctx_st** %11, align 8
  %32 = call %struct.ssl_st* @SSL_new(%struct.ssl_ctx_st* %31)
  store %struct.ssl_st* %32, %struct.ssl_st** @ssl, align 8
  %33 = load %struct.ssl_st*, %struct.ssl_st** @ssl, align 8
  %34 = icmp ne %struct.ssl_st* %33, null
  br i1 %34, label %38, label %35

35:                                               ; preds = %29
  %36 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) @_ZSt4cerr, i8* getelementptr inbounds ([20 x i8], [20 x i8]* @.str.6, i64 0, i64 0))
  %37 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZNSolsEPFRSoS_E(%"class.std::basic_ostream"* %36, %"class.std::basic_ostream"* (%"class.std::basic_ostream"*)* @_ZSt4endlIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_)
  store i32 -1, i32* %3, align 4
  br label %78

38:                                               ; preds = %29
  %39 = load %struct.ssl_st*, %struct.ssl_st** @ssl, align 8
  %40 = load i32, i32* %6, align 4
  %41 = call i32 @SSL_set_fd(%struct.ssl_st* %39, i32 %40)
  %42 = load %struct.ssl_st*, %struct.ssl_st** @ssl, align 8
  %43 = call i32 @SSL_connect(%struct.ssl_st* %42)
  store i32 %43, i32* %12, align 4
  %44 = load i32, i32* %12, align 4
  %45 = icmp sle i32 %44, 0
  br i1 %45, label %46, label %55

46:                                               ; preds = %38
  %47 = call i64 @ERR_get_error()
  %48 = trunc i64 %47 to i32
  store i32 %48, i32* %13, align 4
  %49 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) @_ZSt4cerr, i8* getelementptr inbounds ([44 x i8], [44 x i8]* @.str.7, i64 0, i64 0))
  %50 = load i32, i32* %13, align 4
  %51 = sext i32 %50 to i64
  %52 = call i8* @ERR_error_string(i64 %51, i8* null)
  %53 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) %49, i8* %52)
  %54 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZNSolsEPFRSoS_E(%"class.std::basic_ostream"* %53, %"class.std::basic_ostream"* (%"class.std::basic_ostream"*)* @_ZSt4endlIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_)
  store i32 -1, i32* %3, align 4
  br label %78

55:                                               ; preds = %38
  %56 = load i32, i32* %12, align 4
  %57 = icmp sle i32 %56, 0
  br i1 %57, label %58, label %64

58:                                               ; preds = %55
  %59 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) @_ZSt4cerr, i8* getelementptr inbounds ([44 x i8], [44 x i8]* @.str.7, i64 0, i64 0))
  %60 = call i64 @ERR_get_error()
  %61 = call i8* @ERR_error_string(i64 %60, i8* null)
  %62 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) %59, i8* %61)
  %63 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZNSolsEPFRSoS_E(%"class.std::basic_ostream"* %62, %"class.std::basic_ostream"* (%"class.std::basic_ostream"*)* @_ZSt4endlIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_)
  store i32 -1, i32* %3, align 4
  br label %78

64:                                               ; preds = %55
  %65 = load %struct.ssl_st*, %struct.ssl_st** @ssl, align 8
  %66 = call i64 @SSL_get_verify_result(%struct.ssl_st* %65)
  %67 = icmp eq i64 %66, 0
  br i1 %67, label %68, label %71

68:                                               ; preds = %64
  %69 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) @_ZSt4cout, i8* getelementptr inbounds ([36 x i8], [36 x i8]* @.str.8, i64 0, i64 0))
  %70 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZNSolsEPFRSoS_E(%"class.std::basic_ostream"* %69, %"class.std::basic_ostream"* (%"class.std::basic_ostream"*)* @_ZSt4endlIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_)
  br label %71

71:                                               ; preds = %68, %64
  %72 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) @_ZSt4cout, i8* getelementptr inbounds ([37 x i8], [37 x i8]* @.str.9, i64 0, i64 0))
  %73 = load %struct.ssl_st*, %struct.ssl_st** @ssl, align 8
  %74 = call %struct.ssl_cipher_st* @SSL_get_current_cipher(%struct.ssl_st* %73)
  %75 = call i8* @SSL_CIPHER_get_name(%struct.ssl_cipher_st* %74)
  %76 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZStlsISt11char_traitsIcEERSt13basic_ostreamIcT_ES5_PKc(%"class.std::basic_ostream"* dereferenceable(272) %72, i8* %75)
  %77 = call dereferenceable(272) %"class.std::basic_ostream"* @_ZNSolsEPFRSoS_E(%"class.std::basic_ostream"* %76, %"class.std::basic_ostream"* (%"class.std::basic_ostream"*)* @_ZSt4endlIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_)
  store i32 0, i32* %3, align 4
  br label %78

78:                                               ; preds = %71, %58, %46, %35, %19
  %79 = load i32, i32* %3, align 4
  ret i32 %79

80:                                               ; preds = %28
  %81 = load i8*, i8** %9, align 8
  %82 = load i32, i32* %10, align 4
  %83 = insertvalue { i8*, i32 } undef, i8* %81, 0
  %84 = insertvalue { i8*, i32 } %83, i32 %82, 1
  resume { i8*, i32 } %84
}

declare dso_local %struct.ssl_st* @SSL_new(%struct.ssl_ctx_st*) #1

declare dso_local i32 @SSL_set_fd(%struct.ssl_st*, i32) #1

declare dso_local i32 @SSL_connect(%struct.ssl_st*) #1

declare dso_local i64 @ERR_get_error() #1

declare dso_local i8* @ERR_error_string(i64, i8*) #1

declare dso_local i64 @SSL_get_verify_result(%struct.ssl_st*) #1

declare dso_local i8* @SSL_CIPHER_get_name(%struct.ssl_cipher_st*) #1

declare dso_local %struct.ssl_cipher_st* @SSL_get_current_cipher(%struct.ssl_st*) #1

; Function Attrs: noinline uwtable
define internal void @_GLOBAL__sub_I_client.cpp() #0 section ".text.startup" {
  call void @__cxx_global_var_init()
  ret void
}

attributes #0 = { noinline uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "frame-pointer"="all" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "frame-pointer"="all" "less-precise-fpmad"="false" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #2 = { nounwind "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "frame-pointer"="all" "less-precise-fpmad"="false" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #3 = { nounwind }
attributes #4 = { noinline optnone uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "frame-pointer"="all" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #5 = { argmemonly nounwind willreturn }
attributes #6 = { nounwind readnone "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "frame-pointer"="all" "less-precise-fpmad"="false" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #7 = { noinline nounwind optnone uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "frame-pointer"="all" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #8 = { noinline norecurse optnone uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "frame-pointer"="all" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #9 = { nounwind readnone }

!llvm.module.flags = !{!0}
!llvm.ident = !{!1}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{!"clang version 10.0.0-4ubuntu1 "}
