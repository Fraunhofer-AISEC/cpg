; ModuleID = 'memchr.a0036eed-cgu.0'
source_filename = "memchr.a0036eed-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"memmem::NeedleInfo" = type { { i32, i32 }, { i8, i8 }, [2 x i8] }
%"core::panic::location::Location" = type { { [0 x i8]*, i64 }, i32, i32 }
%"core::fmt::Formatter" = type { { i64, i64 }, { i64, i64 }, { {}*, [3 x i64]* }, i32, i32, i8, [7 x i8] }
%"memmem::FindIter" = type { { [0 x i8]*, i64 }, %"memmem::Finder", i64, { i32, i32 } }
%"memmem::Finder" = type { %"memmem::Searcher" }
%"memmem::Searcher" = type { { i8*, i64 }, i64*, %"memmem::SearcherKind", %"memmem::NeedleInfo", [1 x i32] }
%"memmem::SearcherKind" = type { i8, [39 x i8] }
%"memmem::twoway::Forward" = type { %"memmem::twoway::TwoWay" }
%"memmem::twoway::TwoWay" = type { i64, i64, { i64, i64 } }
%"memmem::FindRevIter" = type { { [0 x i8]*, i64 }, %"memmem::FinderRev", { i64, i64 } }
%"memmem::FinderRev" = type { %"memmem::SearcherRev" }
%"memmem::SearcherRev" = type { { i8*, i64 }, %"memmem::SearcherRevKind", { i32, i32 } }
%"memmem::SearcherRevKind" = type { i8, [39 x i8] }
%"core::option::Option<core::fmt::Arguments>" = type { {}*, [5 x i64] }
%"core::fmt::builders::DebugTuple" = type { %"core::fmt::Formatter"*, i64, i8, i8, [6 x i8] }
%"core::fmt::builders::DebugStruct" = type { %"core::fmt::Formatter"*, i8, i8, [6 x i8] }
%"memmem::twoway::Reverse" = type { %"memmem::twoway::TwoWay" }
%"memmem::x86::avx::nostd::Forward" = type { {} }
%"core::fmt::builders::DebugList" = type { %"core::fmt::builders::DebugInner" }
%"core::fmt::builders::DebugInner" = type { %"core::fmt::Formatter"*, i8, i8, [6 x i8] }

@alloc1050 = private unnamed_addr constant <{ [101 x i8] }> <{ [101 x i8] c"/home/alwagner/.cargo/registry/src/github.com-1ecc6299db9ec823/memchr-2.4.1/src/memmem/genericsimd.rs" }>, align 1
@alloc1049 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [101 x i8] }>, <{ [101 x i8] }>* @alloc1050, i32 0, i32 0, i32 0), [16 x i8] c"e\00\00\00\00\00\00\00\83\00\00\00\1F\00\00\00" }>, align 8
@alloc1051 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [101 x i8] }>, <{ [101 x i8] }>* @alloc1050, i32 0, i32 0, i32 0), [16 x i8] c"e\00\00\00\00\00\00\00\84\00\00\00\1F\00\00\00" }>, align 8
@alloc1054 = private unnamed_addr constant <{ [31 x i8] }> <{ [31 x i8] c"needle must be at least 2 bytes" }>, align 1
@alloc1059 = private unnamed_addr constant <{ [111 x i8] }> <{ [111 x i8] c"/home/alwagner/.cargo/registry/src/github.com-1ecc6299db9ec823/memchr-2.4.1/src/memmem/prefilter/genericsimd.rs" }>, align 1
@alloc1056 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [111 x i8] }>, <{ [111 x i8] }>* @alloc1059, i32 0, i32 0, i32 0), [16 x i8] c"o\00\00\00\00\00\00\003\00\00\00\05\00\00\00" }>, align 8
@alloc1058 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [111 x i8] }>, <{ [111 x i8] }>* @alloc1059, i32 0, i32 0, i32 0), [16 x i8] c"o\00\00\00\00\00\00\00?\00\00\00\1F\00\00\00" }>, align 8
@alloc1060 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [111 x i8] }>, <{ [111 x i8] }>* @alloc1059, i32 0, i32 0, i32 0), [16 x i8] c"o\00\00\00\00\00\00\00@\00\00\00\1F\00\00\00" }>, align 8
@alloc1061 = private unnamed_addr constant <{ [107 x i8] }> <{ [107 x i8] c"/home/alwagner/.cargo/registry/src/github.com-1ecc6299db9ec823/memchr-2.4.1/src/memmem/prefilter/x86/sse.rs" }>, align 1
@alloc1062 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [107 x i8] }>, <{ [107 x i8] }>* @alloc1061, i32 0, i32 0, i32 0), [16 x i8] c"k\00\00\00\00\00\00\00#\00\00\00\17\00\00\00" }>, align 8
@alloc1063 = private unnamed_addr constant <{ [19 x i8] }> <{ [19 x i8] c"<prefilter-fn(...)>" }>, align 1
@alloc1082 = private unnamed_addr constant <{ [99 x i8] }> <{ [99 x i8] c"/home/alwagner/.cargo/registry/src/github.com-1ecc6299db9ec823/memchr-2.4.1/src/memmem/rabinkarp.rs" }>, align 1
@alloc1077 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [99 x i8] }>, <{ [99 x i8] }>* @alloc1082, i32 0, i32 0, i32 0), [16 x i8] c"c\00\00\00\00\00\00\00a\00\00\00\0D\00\00\00" }>, align 8
@alloc1094 = private unnamed_addr constant <{ [99 x i8] }> <{ [99 x i8] c"/home/alwagner/.cargo/registry/src/github.com-1ecc6299db9ec823/memchr-2.4.1/src/memmem/rarebytes.rs" }>, align 1
@alloc1089 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [99 x i8] }>, <{ [99 x i8] }>* @alloc1094, i32 0, i32 0, i32 0), [16 x i8] c"c\00\00\00\00\00\00\00T\00\00\00\09\00\00\00" }>, align 8
@0 = private unnamed_addr constant <{ [256 x i8] }> <{ [256 x i8] c"743210/.-g\F2BC\E5,+*)('&%$#\22!8 \1F\1E\1D\1C\FF\94\A4\95\88\A0\9B\AD\DD\DE\86z\E8\CA\D7\E0\D0\DC\CC\BB\B7\B3\B1\A8\B2\C8\E2\C3\9A\B8\AE~x\BF\9D\C2\AA\BD\A2\A1\96\C1\8E\89\AB\B0\B9\A7\BAp\AF\C0\BC\9C\8C\8F{\85\80\93\8A\92r\DF\97\F9\D8\EE\EC\FD\E3\DA\E6\F7\87\B4\F1\E9\F6\F4\E7\8B\F5\F3\FB\EB\C9\C4\F0\D6\98\B6\CD\B5\7F\1B\D4\D3\D2\D5\E4\C5\A9\9F\83\ACiPb`aQ\CF\91ts\90\82\99yk\84mn|oRlv\8Dq\81w}\A5u\\jSHc]AO\A6\ED\A3\C7\BE\E1\D1\CB\C6\D9\DB\CE\EA\F8\9E\EF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF\FF" }>, align 1
@alloc1166 = private unnamed_addr constant <{ [96 x i8] }> <{ [96 x i8] c"/home/alwagner/.cargo/registry/src/github.com-1ecc6299db9ec823/memchr-2.4.1/src/memmem/twoway.rs" }>, align 1
@alloc1097 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\A6\00\00\00&\00\00\00" }>, align 8
@alloc1099 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\AE\00\00\00)\00\00\00" }>, align 8
@alloc1103 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\B3\00\00\004\00\00\00" }>, align 8
@alloc1105 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\BB\00\00\00$\00\00\00" }>, align 8
@alloc1107 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\BB\00\00\001\00\00\00" }>, align 8
@alloc1109 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\BE\00\00\00\22\00\00\00" }>, align 8
@alloc1111 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\BE\00\00\003\00\00\00" }>, align 8
@alloc1113 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\D5\00\00\00&\00\00\00" }>, align 8
@alloc1115 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\DC\00\00\00)\00\00\00" }>, align 8
@alloc1119 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\E1\00\00\004\00\00\00" }>, align 8
@alloc1121 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\E8\00\00\00\18\00\00\00" }>, align 8
@alloc1123 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\E8\00\00\00%\00\00\00" }>, align 8
@alloc1125 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00A\01\00\00)\00\00\00" }>, align 8
@alloc1127 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00G\01\00\00\1C\00\00\00" }>, align 8
@alloc1129 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00G\01\00\00-\00\00\00" }>, align 8
@alloc1135 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00O\01\00\00$\00\00\00" }>, align 8
@alloc1137 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00O\01\00\001\00\00\00" }>, align 8
@alloc1139 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00f\01\00\00)\00\00\00" }>, align 8
@alloc1141 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00k\01\00\00\1C\00\00\00" }>, align 8
@alloc1143 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00k\01\00\00-\00\00\00" }>, align 8
@alloc1151 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00r\01\00\000\00\00\00" }>, align 8
@alloc1153 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\BE\01\00\00\1D\00\00\00" }>, align 8
@alloc1155 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\BF\01\00\00\1E\00\00\00" }>, align 8
@alloc1157 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\D5\01\00\00\1D\00\00\00" }>, align 8
@alloc1159 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\D6\01\00\00\1E\00\00\00" }>, align 8
@alloc1161 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00\08\02\00\00\1B\00\00\00" }>, align 8
@alloc1165 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00.\02\00\00\1B\00\00\00" }>, align 8
@alloc1167 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [96 x i8] }>, <{ [96 x i8] }>* @alloc1166, i32 0, i32 0, i32 0), [16 x i8] c"`\00\00\00\00\00\00\00/\02\00\00\1D\00\00\00" }>, align 8
@alloc1175 = private unnamed_addr constant <{ [40 x i8] }> <{ [40 x i8] c"internal error: entered unreachable code" }>, align 1
@alloc1176 = private unnamed_addr constant <{ [97 x i8] }> <{ [97 x i8] c"/home/alwagner/.cargo/registry/src/github.com-1ecc6299db9ec823/memchr-2.4.1/src/memmem/x86/avx.rs" }>, align 1
@alloc1174 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [97 x i8] }>, <{ [97 x i8] }>* @alloc1176, i32 0, i32 0, i32 0), [16 x i8] c"a\00\00\00\00\00\00\00Z\00\00\00\0D\00\00\00" }>, align 8
@alloc1184 = private unnamed_addr constant <{ [93 x i8] }> <{ [93 x i8] c"/home/alwagner/.cargo/registry/src/github.com-1ecc6299db9ec823/memchr-2.4.1/src/memmem/mod.rs" }>, align 1
@alloc1181 = private unnamed_addr constant <{ i8*, [16 x i8] }> <{ i8* getelementptr inbounds (<{ [93 x i8] }>, <{ [93 x i8] }>* @alloc1184, i32 0, i32 0, i32 0), [16 x i8] c"]\00\00\00\00\00\00\00\7F\01\00\00)\00\00\00" }>, align 8
@alloc1186 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"CowBytes" }>, align 1
@vtable.0 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i8*, i64 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hed460d61bf2992aaE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1190 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"Imp" }>, align 1
@vtable.1 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ [0 x i8]*, i64 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h3aea02fb6dd27823E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1256 = private unnamed_addr constant <{ [7 x i8] }> <{ [7 x i8] c"Forward" }>, align 1
@alloc1216 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"rare1i" }>, align 1
@vtable.2 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i8**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hfc5006809de8f68cE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1217 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"rare2i" }>, align 1
@alloc1200 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"Auto" }>, align 1
@alloc1368 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"None" }>, align 1
@alloc1202 = private unnamed_addr constant <{ [14 x i8] }> <{ [14 x i8] c"PrefilterState" }>, align 1
@alloc1203 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"skips" }>, align 1
@vtable.3 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i32**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h15d12a7ec8ebf07bE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1207 = private unnamed_addr constant <{ [7 x i8] }> <{ [7 x i8] c"skipped" }>, align 1
@alloc1208 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"NeedleHash" }>, align 1
@alloc1209 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"hash" }>, align 1
@vtable.4 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i32**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h1b91a732156e1f6cE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1213 = private unnamed_addr constant <{ [9 x i8] }> <{ [9 x i8] c"hash_2pow" }>, align 1
@alloc1214 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"Hash" }>, align 1
@alloc1215 = private unnamed_addr constant <{ [15 x i8] }> <{ [15 x i8] c"RareNeedleBytes" }>, align 1
@vtable.5 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::twoway::TwoWay"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h30d83982d8dc029dE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1222 = private unnamed_addr constant <{ [7 x i8] }> <{ [7 x i8] c"Reverse" }>, align 1
@alloc1348 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"TwoWay" }>, align 1
@alloc1224 = private unnamed_addr constant <{ [7 x i8] }> <{ [7 x i8] c"byteset" }>, align 1
@vtable.6 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i64**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hc4a9f9ccbf293413E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1228 = private unnamed_addr constant <{ [12 x i8] }> <{ [12 x i8] c"critical_pos" }>, align 1
@vtable.7 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i64**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hf0ca0eaf8e10788bE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1237 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"shift" }>, align 1
@vtable.8 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i64, i64 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h9eb75065b6b2b2e6E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1236 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"Large" }>, align 1
@alloc1238 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"Small" }>, align 1
@alloc1242 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"period" }>, align 1
@alloc1240 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"Suffix" }>, align 1
@alloc1277 = private unnamed_addr constant <{ [3 x i8] }> <{ [3 x i8] c"pos" }>, align 1
@alloc1243 = private unnamed_addr constant <{ [7 x i8] }> <{ [7 x i8] c"Maximal" }>, align 1
@alloc1244 = private unnamed_addr constant <{ [7 x i8] }> <{ [7 x i8] c"Minimal" }>, align 1
@alloc1245 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"Push" }>, align 1
@alloc1246 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"Skip" }>, align 1
@alloc1247 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"Accept" }>, align 1
@alloc1248 = private unnamed_addr constant <{ [18 x i8] }> <{ [18 x i8] c"ApproximateByteSet" }>, align 1
@vtable.9 = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i64**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h375e3f0ea5f25018E" to i8*), [0 x i8] zeroinitializer }>, align 8
@vtable.a = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({}**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h012f9a793918b767E" to i8*), [0 x i8] zeroinitializer }>, align 8
@vtable.b = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i8, i8 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h203c01692e5cf32cE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1260 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"FindIter" }>, align 1
@alloc1272 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"haystack" }>, align 1
@alloc1262 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"prestate" }>, align 1
@vtable.c = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i32, i32 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h70f1e85449412d59E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1273 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"finder" }>, align 1
@vtable.d = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::Finder"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h6af5665499c3be04E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1271 = private unnamed_addr constant <{ [11 x i8] }> <{ [11 x i8] c"FindRevIter" }>, align 1
@vtable.e = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::FinderRev"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hb757f96e3bd9808dE" to i8*), [0 x i8] zeroinitializer }>, align 8
@vtable.f = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i64, i64 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h02afc80f286cfd18E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1281 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"Finder" }>, align 1
@alloc1287 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"searcher" }>, align 1
@vtable.g = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::Searcher"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h782af9d435b63914E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1286 = private unnamed_addr constant <{ [9 x i8] }> <{ [9 x i8] c"FinderRev" }>, align 1
@vtable.h = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::SearcherRev"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17he61c36eefdf14552E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1291 = private unnamed_addr constant <{ [13 x i8] }> <{ [13 x i8] c"FinderBuilder" }>, align 1
@alloc1292 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"config" }>, align 1
@vtable.i = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i8**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17he241ff79fca5e777E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1296 = private unnamed_addr constant <{ [8 x i8] }> <{ [8 x i8] c"Searcher" }>, align 1
@alloc1342 = private unnamed_addr constant <{ [6 x i8] }> <{ [6 x i8] c"needle" }>, align 1
@vtable.j = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i8*, i64 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hff7095ab22540aa2E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1301 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"ninfo" }>, align 1
@vtable.k = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::NeedleInfo"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h289d590000ab1d43E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1305 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"prefn" }>, align 1
@vtable.l = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i64***, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h6ec26ca7b51ec85cE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1344 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"kind" }>, align 1
@vtable.m = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::SearcherKind"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17he2204767e76a9523E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1313 = private unnamed_addr constant <{ [10 x i8] }> <{ [10 x i8] c"NeedleInfo" }>, align 1
@alloc1314 = private unnamed_addr constant <{ [9 x i8] }> <{ [9 x i8] c"rarebytes" }>, align 1
@vtable.n = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i8, i8 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h8d46de007ceb3d4bE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1343 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"nhash" }>, align 1
@vtable.o = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i32, i32 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h4956e92797be587dE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1322 = private unnamed_addr constant <{ [14 x i8] }> <{ [14 x i8] c"SearcherConfig" }>, align 1
@alloc1323 = private unnamed_addr constant <{ [9 x i8] }> <{ [9 x i8] c"prefilter" }>, align 1
@vtable.p = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i8**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h423c5115cbf28310E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1327 = private unnamed_addr constant <{ [14 x i8] }> <{ [14 x i8] c"GenericSIMD256" }>, align 1
@vtable.q = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::x86::avx::nostd::Forward"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h3658f4059176b5a0E" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1331 = private unnamed_addr constant <{ [14 x i8] }> <{ [14 x i8] c"GenericSIMD128" }>, align 1
@vtable.r = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 ({ i8, i8 }**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h1f25bed89d6307b2E" to i8*), [0 x i8] zeroinitializer }>, align 8
@vtable.s = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::twoway::Forward"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h4fb05e4824d7062dE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1352 = private unnamed_addr constant <{ [7 x i8] }> <{ [7 x i8] c"OneByte" }>, align 1
@alloc1353 = private unnamed_addr constant <{ [5 x i8] }> <{ [5 x i8] c"Empty" }>, align 1
@alloc1341 = private unnamed_addr constant <{ [11 x i8] }> <{ [11 x i8] c"SearcherRev" }>, align 1
@vtable.t = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::SearcherRevKind"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h91a378adcdf2e01eE" to i8*), [0 x i8] zeroinitializer }>, align 8
@vtable.u = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (%"memmem::twoway::Reverse"**, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h4c8240682b9c408bE" to i8*), [0 x i8] zeroinitializer }>, align 8
@alloc1358 = private unnamed_addr constant <{ [2 x i8] }> <{ [2 x i8] c"()" }>, align 1
@alloc1359 = private unnamed_addr constant <{ [35 x i8] }> <{ [35 x i8] c"assertion failed: mid <= self.len()" }>, align 1
@alloc1364 = private unnamed_addr constant <{ [4 x i8] }> <{ [4 x i8] c"Some" }>, align 1
@vtable.v = private unnamed_addr constant <{ i8*, [16 x i8], i8*, [0 x i8] }> <{ i8* bitcast (void (i8**)* @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE" to i8*), [16 x i8] c"\08\00\00\00\00\00\00\00\08\00\00\00\00\00\00\00", i8* bitcast (i1 (i64***, %"core::fmt::Formatter"*)* @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h24add91eb36d19a2E" to i8*), [0 x i8] zeroinitializer }>, align 8

@"_ZN79_$LT$memchr..memmem..prefilter..Prefilter$u20$as$u20$core..default..Default$GT$7default17hf30f560f51bb53adE" = unnamed_addr alias i1 (), i1 ()* @_ZN6memchr6memmem13FinderBuilder3new17h6f660de5680092b7E

; core::ptr::drop_in_place<&u8>
; Function Attrs: inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define internal void @"_ZN4core3ptr27drop_in_place$LT$$RF$u8$GT$17h7881da1182d783ccE"(i8** nocapture readnone %_1) unnamed_addr #0 {
start:
  ret void
}

; memchr::memchr::fallback::memchr
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr8fallback6memchr17h9e5739b193f13e8dE(i8 %0, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #1 personality i32 (...)* @rust_eh_personality {
start:
  %_2.i = zext i8 %0 to i64
  %1 = mul nuw i64 %_2.i, 72340172838076673
  %2 = icmp ugt i64 %haystack.1, 15
  %3 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %4 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_18 = icmp ult i64 %haystack.1, 8
  br i1 %_18, label %bb5, label %bb7

bb7:                                              ; preds = %start
  %_26 = bitcast [0 x i8]* %haystack.0 to i64*
  %_26.val = load i64, i64* %_26, align 1
  %_29 = xor i64 %_26.val, %1
  %5 = add i64 %_29, -72340172838076673
  %_6.i = and i64 %_29, -9187201950435737472
  %_3.i = xor i64 %_6.i, -9187201950435737472
  %_2.i15 = and i64 %_3.i, %5
  %.not = icmp eq i64 %_2.i15, 0
  %6 = trunc i64 %_26.val to i8
  br i1 %.not, label %bb12, label %bb10

bb5:                                              ; preds = %start
  %_56.i.not = icmp eq i64 %haystack.1, 0
  br i1 %_56.i.not, label %bb33, label %bb2.preheader.i

bb2.preheader.i:                                  ; preds = %bb5
  %7 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_11.i = load i8, i8* %3, align 1, !noalias !2
  %8 = icmp eq i8 %_11.i, %0
  br i1 %8, label %bb4.i, label %bb6.i

bb6.i:                                            ; preds = %bb2.preheader.i
  %9 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 1
  %exitcond.not.i = icmp eq i64 %haystack.1, 1
  br i1 %exitcond.not.i, label %bb33, label %bb2.i.1

bb4.i:                                            ; preds = %bb2.i.6, %bb2.i.5, %bb2.i.4, %bb2.i.3, %bb2.i.2, %bb2.i.1, %bb2.preheader.i
  %ptr.07.i.lcssa = phi i8* [ %3, %bb2.preheader.i ], [ %9, %bb2.i.1 ], [ %30, %bb2.i.2 ], [ %32, %bb2.i.3 ], [ %34, %bb2.i.4 ], [ %36, %bb2.i.5 ], [ %38, %bb2.i.6 ]
  %_3.i.i = ptrtoint i8* %ptr.07.i.lcssa to i64
  %10 = sub i64 %_3.i.i, %7
  br label %bb33

bb12:                                             ; preds = %bb7
  %_40 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_39 = and i64 %_40, 7
  %_38 = sub nuw nsw i64 8, %_39
  %11 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_38
  %12 = getelementptr inbounds i8, i8* %4, i64 -16
  %_4661 = icmp ule i8* %11, %12
  %or.cond62 = select i1 %2, i1 %_4661, i1 false
  br i1 %or.cond62, label %bb19, label %bb30

bb10:                                             ; preds = %bb7
  %_56.i17 = icmp sgt i64 %haystack.1, 0
  br i1 %_56.i17, label %bb2.preheader.i19, label %bb33

bb2.preheader.i19:                                ; preds = %bb10
  %13 = ptrtoint [0 x i8]* %haystack.0 to i64
  %14 = icmp eq i8 %6, %0
  br i1 %14, label %bb4.i27, label %bb6.i24

bb6.i24:                                          ; preds = %bb2.preheader.i19, %bb6.i24.bb2.i22_crit_edge
  %ptr.07.i2082 = phi i8* [ %15, %bb6.i24.bb2.i22_crit_edge ], [ %3, %bb2.preheader.i19 ]
  %15 = getelementptr inbounds i8, i8* %ptr.07.i2082, i64 1
  %exitcond.not.i23 = icmp eq i8* %15, %4
  br i1 %exitcond.not.i23, label %bb33, label %bb6.i24.bb2.i22_crit_edge

bb6.i24.bb2.i22_crit_edge:                        ; preds = %bb6.i24
  %_11.i21.pre = load i8, i8* %15, align 1, !noalias !5
  %16 = icmp eq i8 %_11.i21.pre, %0
  br i1 %16, label %bb4.i27, label %bb6.i24

bb4.i27:                                          ; preds = %bb6.i24.bb2.i22_crit_edge, %bb2.preheader.i19
  %ptr.07.i20.lcssa = phi i8* [ %3, %bb2.preheader.i19 ], [ %15, %bb6.i24.bb2.i22_crit_edge ]
  %_3.i.i25 = ptrtoint i8* %ptr.07.i20.lcssa to i64
  %17 = sub i64 %_3.i.i25, %13
  br label %bb33

bb33:                                             ; preds = %bb2.i.6, %bb6.i24, %bb6.i45, %bb6.i, %bb6.i.1, %bb6.i.2, %bb6.i.3, %bb6.i.4, %bb6.i.5, %bb4.i48, %bb30, %bb4.i27, %bb10, %bb4.i, %bb5
  %.sroa.0.0.i50.pn = phi i64 [ 1, %bb4.i ], [ 0, %bb5 ], [ 1, %bb4.i27 ], [ 0, %bb10 ], [ 1, %bb4.i48 ], [ 0, %bb30 ], [ 0, %bb6.i.5 ], [ 0, %bb6.i.4 ], [ 0, %bb6.i.3 ], [ 0, %bb6.i.2 ], [ 0, %bb6.i.1 ], [ 0, %bb6.i ], [ 0, %bb6.i45 ], [ 0, %bb6.i24 ], [ 0, %bb2.i.6 ]
  %.sroa.3.0.i49.pn = phi i64 [ %10, %bb4.i ], [ undef, %bb5 ], [ %17, %bb4.i27 ], [ undef, %bb10 ], [ %28, %bb4.i48 ], [ undef, %bb30 ], [ undef, %bb6.i.5 ], [ undef, %bb6.i.4 ], [ undef, %bb6.i.3 ], [ undef, %bb6.i.2 ], [ undef, %bb6.i.1 ], [ undef, %bb6.i ], [ undef, %bb6.i45 ], [ undef, %bb6.i24 ], [ undef, %bb2.i.6 ]
  %.pn = insertvalue { i64, i64 } undef, i64 %.sroa.0.0.i50.pn, 0
  %.pn.pn = insertvalue { i64, i64 } %.pn, i64 %.sroa.3.0.i49.pn, 1
  ret { i64, i64 } %.pn.pn

bb19:                                             ; preds = %bb12, %bb27
  %ptr.063 = phi i8* [ %23, %bb27 ], [ %11, %bb12 ]
  %_55 = bitcast i8* %ptr.063 to i64*
  %a = load i64, i64* %_55, align 8
  %18 = getelementptr inbounds i8, i8* %ptr.063, i64 8
  %_58 = bitcast i8* %18 to i64*
  %b = load i64, i64* %_58, align 8
  %_62 = xor i64 %a, %1
  %19 = add i64 %_62, -72340172838076673
  %_6.i31 = and i64 %_62, -9187201950435737472
  %_3.i32 = xor i64 %_6.i31, -9187201950435737472
  %_2.i33 = and i64 %_3.i32, %19
  %20 = icmp ne i64 %_2.i33, 0
  %_66 = xor i64 %b, %1
  %21 = add i64 %_66, -72340172838076673
  %_6.i34 = and i64 %_66, -9187201950435737472
  %_3.i35 = xor i64 %_6.i34, -9187201950435737472
  %_2.i36 = and i64 %_3.i35, %21
  %22 = icmp ne i64 %_2.i36, 0
  %.eqb = select i1 %20, i1 true, i1 %22
  br i1 %.eqb, label %bb30, label %bb27

bb27:                                             ; preds = %bb19
  %23 = getelementptr inbounds i8, i8* %ptr.063, i64 16
  %_46.not = icmp ugt i8* %23, %12
  br i1 %_46.not, label %bb30, label %bb19

bb30:                                             ; preds = %bb19, %bb27, %bb12
  %ptr.0.lcssa = phi i8* [ %11, %bb12 ], [ %23, %bb27 ], [ %ptr.063, %bb19 ]
  %_56.i38 = icmp ult i8* %ptr.0.lcssa, %4
  br i1 %_56.i38, label %bb2.preheader.i40, label %bb33

bb2.preheader.i40:                                ; preds = %bb30
  %end_ptr9.i39 = ptrtoint i8* %4 to i64
  %24 = ptrtoint i8* %ptr.0.lcssa to i64
  %25 = sub i64 %end_ptr9.i39, %24
  %scevgep.i = getelementptr i8, i8* %ptr.0.lcssa, i64 %25
  br label %bb2.i43

bb2.i43:                                          ; preds = %bb6.i45, %bb2.preheader.i40
  %ptr.07.i41 = phi i8* [ %27, %bb6.i45 ], [ %ptr.0.lcssa, %bb2.preheader.i40 ]
  %_11.i42 = load i8, i8* %ptr.07.i41, align 1, !noalias !8
  %26 = icmp eq i8 %_11.i42, %0
  br i1 %26, label %bb4.i48, label %bb6.i45

bb6.i45:                                          ; preds = %bb2.i43
  %27 = getelementptr inbounds i8, i8* %ptr.07.i41, i64 1
  %exitcond.not.i44 = icmp eq i8* %27, %scevgep.i
  br i1 %exitcond.not.i44, label %bb33, label %bb2.i43

bb4.i48:                                          ; preds = %bb2.i43
  %_3.i.i46 = ptrtoint i8* %ptr.07.i41 to i64
  %28 = sub i64 %_3.i.i46, %_40
  br label %bb33

bb2.i.1:                                          ; preds = %bb6.i
  %_11.i.1 = load i8, i8* %9, align 1, !noalias !2
  %29 = icmp eq i8 %_11.i.1, %0
  br i1 %29, label %bb4.i, label %bb6.i.1

bb6.i.1:                                          ; preds = %bb2.i.1
  %30 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 2
  %exitcond.not.i.1 = icmp eq i64 %haystack.1, 2
  br i1 %exitcond.not.i.1, label %bb33, label %bb2.i.2

bb2.i.2:                                          ; preds = %bb6.i.1
  %_11.i.2 = load i8, i8* %30, align 1, !noalias !2
  %31 = icmp eq i8 %_11.i.2, %0
  br i1 %31, label %bb4.i, label %bb6.i.2

bb6.i.2:                                          ; preds = %bb2.i.2
  %32 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 3
  %exitcond.not.i.2 = icmp eq i64 %haystack.1, 3
  br i1 %exitcond.not.i.2, label %bb33, label %bb2.i.3

bb2.i.3:                                          ; preds = %bb6.i.2
  %_11.i.3 = load i8, i8* %32, align 1, !noalias !2
  %33 = icmp eq i8 %_11.i.3, %0
  br i1 %33, label %bb4.i, label %bb6.i.3

bb6.i.3:                                          ; preds = %bb2.i.3
  %34 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 4
  %exitcond.not.i.3 = icmp eq i64 %haystack.1, 4
  br i1 %exitcond.not.i.3, label %bb33, label %bb2.i.4

bb2.i.4:                                          ; preds = %bb6.i.3
  %_11.i.4 = load i8, i8* %34, align 1, !noalias !2
  %35 = icmp eq i8 %_11.i.4, %0
  br i1 %35, label %bb4.i, label %bb6.i.4

bb6.i.4:                                          ; preds = %bb2.i.4
  %36 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 5
  %exitcond.not.i.4 = icmp eq i64 %haystack.1, 5
  br i1 %exitcond.not.i.4, label %bb33, label %bb2.i.5

bb2.i.5:                                          ; preds = %bb6.i.4
  %_11.i.5 = load i8, i8* %36, align 1, !noalias !2
  %37 = icmp eq i8 %_11.i.5, %0
  br i1 %37, label %bb4.i, label %bb6.i.5

bb6.i.5:                                          ; preds = %bb2.i.5
  %38 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 6
  %exitcond.not.i.5 = icmp eq i64 %haystack.1, 6
  br i1 %exitcond.not.i.5, label %bb33, label %bb2.i.6

bb2.i.6:                                          ; preds = %bb6.i.5
  %_11.i.6 = load i8, i8* %38, align 1, !noalias !2
  %39 = icmp eq i8 %_11.i.6, %0
  br i1 %39, label %bb4.i, label %bb33
}

; memchr::memchr::fallback::memchr2
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr8fallback7memchr217h2426afb0cdd3d1e3E(i8 %0, i8 %1, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #1 personality i32 (...)* @rust_eh_personality {
start:
  %_2.i = zext i8 %0 to i64
  %2 = mul nuw i64 %_2.i, 72340172838076673
  %_2.i20 = zext i8 %1 to i64
  %3 = mul nuw i64 %_2.i20, 72340172838076673
  %4 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %5 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_19 = icmp ult i64 %haystack.1, 8
  br i1 %_19, label %bb5, label %bb7

bb7:                                              ; preds = %start
  %_27 = bitcast [0 x i8]* %haystack.0 to i64*
  %_27.val = load i64, i64* %_27, align 1
  %_30 = xor i64 %_27.val, %2
  %6 = add i64 %_30, -72340172838076673
  %_6.i = and i64 %_30, -9187201950435737472
  %_3.i = xor i64 %_6.i, -9187201950435737472
  %_2.i21 = and i64 %_3.i, %6
  %7 = icmp ne i64 %_2.i21, 0
  %_34 = xor i64 %_27.val, %3
  %8 = add i64 %_34, -72340172838076673
  %_6.i22 = and i64 %_34, -9187201950435737472
  %_3.i23 = xor i64 %_6.i22, -9187201950435737472
  %_2.i24 = and i64 %_3.i23, %8
  %9 = icmp ne i64 %_2.i24, 0
  %.eq2 = select i1 %7, i1 true, i1 %9
  %10 = trunc i64 %_27.val to i8
  br i1 %.eq2, label %bb14, label %bb16

bb5:                                              ; preds = %start
  %_59.i.not = icmp eq i64 %haystack.1, 0
  br i1 %_59.i.not, label %bb33, label %bb2.preheader.i

bb2.preheader.i:                                  ; preds = %bb5
  %11 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_11.i = load i8, i8* %4, align 1, !noalias !11
  %_3.i.i = icmp eq i8 %_11.i, %0
  %_6.i.i = icmp eq i8 %_11.i, %1
  %or.cond.i = select i1 %_3.i.i, i1 true, i1 %_6.i.i
  br i1 %or.cond.i, label %bb4.i, label %bb6.i

bb6.i:                                            ; preds = %bb2.preheader.i
  %12 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 1
  %exitcond.not.i = icmp eq i64 %haystack.1, 1
  br i1 %exitcond.not.i, label %bb33, label %bb2.i.1

bb4.i:                                            ; preds = %bb2.i.6, %bb2.i.5, %bb2.i.4, %bb2.i.3, %bb2.i.2, %bb2.i.1, %bb2.preheader.i
  %ptr.010.i.lcssa = phi i8* [ %4, %bb2.preheader.i ], [ %12, %bb2.i.1 ], [ %28, %bb2.i.2 ], [ %29, %bb2.i.3 ], [ %30, %bb2.i.4 ], [ %31, %bb2.i.5 ], [ %32, %bb2.i.6 ]
  %_3.i6.i = ptrtoint i8* %ptr.010.i.lcssa to i64
  %13 = sub i64 %_3.i6.i, %11
  br label %bb33

bb16:                                             ; preds = %bb7
  %_48 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_47 = and i64 %_48, 7
  %_46 = sub nuw nsw i64 8, %_47
  %14 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_46
  %15 = getelementptr inbounds i8, i8* %5, i64 -8
  %_51.not80 = icmp ugt i8* %14, %15
  br i1 %_51.not80, label %bb30, label %bb20

bb14:                                             ; preds = %bb7
  %_59.i27 = icmp sgt i64 %haystack.1, 0
  br i1 %_59.i27, label %bb2.preheader.i29, label %bb33

bb2.preheader.i29:                                ; preds = %bb14
  %16 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_3.i.i3299 = icmp eq i8 %10, %0
  %_6.i.i33100 = icmp eq i8 %10, %1
  %or.cond.i34101 = select i1 %_3.i.i3299, i1 true, i1 %_6.i.i33100
  br i1 %or.cond.i34101, label %bb4.i40, label %bb6.i37

bb6.i37:                                          ; preds = %bb2.preheader.i29, %bb6.i37.bb2.i35_crit_edge
  %ptr.010.i30102 = phi i8* [ %17, %bb6.i37.bb2.i35_crit_edge ], [ %4, %bb2.preheader.i29 ]
  %17 = getelementptr inbounds i8, i8* %ptr.010.i30102, i64 1
  %exitcond.not.i36 = icmp eq i8* %17, %5
  br i1 %exitcond.not.i36, label %bb33, label %bb6.i37.bb2.i35_crit_edge

bb6.i37.bb2.i35_crit_edge:                        ; preds = %bb6.i37
  %_11.i31.pre = load i8, i8* %17, align 1, !noalias !15
  %_3.i.i32 = icmp eq i8 %_11.i31.pre, %0
  %_6.i.i33 = icmp eq i8 %_11.i31.pre, %1
  %or.cond.i34 = select i1 %_3.i.i32, i1 true, i1 %_6.i.i33
  br i1 %or.cond.i34, label %bb4.i40, label %bb6.i37

bb4.i40:                                          ; preds = %bb6.i37.bb2.i35_crit_edge, %bb2.preheader.i29
  %ptr.010.i30.lcssa = phi i8* [ %4, %bb2.preheader.i29 ], [ %17, %bb6.i37.bb2.i35_crit_edge ]
  %_3.i6.i38 = ptrtoint i8* %ptr.010.i30.lcssa to i64
  %18 = sub i64 %_3.i6.i38, %16
  br label %bb33

bb33:                                             ; preds = %bb2.i.6, %bb6.i62, %bb6.i37, %bb6.i, %bb6.i.1, %bb6.i.2, %bb6.i.3, %bb6.i.4, %bb6.i.5, %bb4.i65, %bb30, %bb4.i40, %bb14, %bb4.i, %bb5
  %.sroa.0.0.i67.pn = phi i64 [ 1, %bb4.i ], [ 0, %bb5 ], [ 1, %bb4.i40 ], [ 0, %bb14 ], [ 1, %bb4.i65 ], [ 0, %bb30 ], [ 0, %bb6.i.5 ], [ 0, %bb6.i.4 ], [ 0, %bb6.i.3 ], [ 0, %bb6.i.2 ], [ 0, %bb6.i.1 ], [ 0, %bb6.i ], [ 0, %bb6.i37 ], [ 0, %bb6.i62 ], [ 0, %bb2.i.6 ]
  %.sroa.3.0.i66.pn = phi i64 [ %13, %bb4.i ], [ undef, %bb5 ], [ %18, %bb4.i40 ], [ undef, %bb14 ], [ %27, %bb4.i65 ], [ undef, %bb30 ], [ undef, %bb6.i.5 ], [ undef, %bb6.i.4 ], [ undef, %bb6.i.3 ], [ undef, %bb6.i.2 ], [ undef, %bb6.i.1 ], [ undef, %bb6.i ], [ undef, %bb6.i37 ], [ undef, %bb6.i62 ], [ undef, %bb2.i.6 ]
  %.pn = insertvalue { i64, i64 } undef, i64 %.sroa.0.0.i67.pn, 0
  %.pn.pn = insertvalue { i64, i64 } %.pn, i64 %.sroa.3.0.i66.pn, 1
  ret { i64, i64 } %.pn.pn

bb20:                                             ; preds = %bb16, %bb27
  %ptr.081 = phi i8* [ %23, %bb27 ], [ %14, %bb16 ]
  %_59 = bitcast i8* %ptr.081 to i64*
  %chunk1 = load i64, i64* %_59, align 8
  %_62 = xor i64 %chunk1, %2
  %19 = add i64 %_62, -72340172838076673
  %_6.i44 = and i64 %_62, -9187201950435737472
  %_3.i45 = xor i64 %_6.i44, -9187201950435737472
  %_2.i46 = and i64 %_3.i45, %19
  %20 = icmp ne i64 %_2.i46, 0
  %_66 = xor i64 %chunk1, %3
  %21 = add i64 %_66, -72340172838076673
  %_6.i47 = and i64 %_66, -9187201950435737472
  %_3.i48 = xor i64 %_6.i47, -9187201950435737472
  %_2.i49 = and i64 %_3.i48, %21
  %22 = icmp ne i64 %_2.i49, 0
  %.eq23 = select i1 %20, i1 true, i1 %22
  br i1 %.eq23, label %bb30, label %bb27

bb27:                                             ; preds = %bb20
  %23 = getelementptr inbounds i8, i8* %ptr.081, i64 8
  %_51.not = icmp ugt i8* %23, %15
  br i1 %_51.not, label %bb30, label %bb20

bb30:                                             ; preds = %bb27, %bb20, %bb16
  %ptr.0.lcssa = phi i8* [ %14, %bb16 ], [ %ptr.081, %bb20 ], [ %23, %bb27 ]
  %_59.i52 = icmp ult i8* %ptr.0.lcssa, %5
  br i1 %_59.i52, label %bb2.preheader.i54, label %bb33

bb2.preheader.i54:                                ; preds = %bb30
  %end_ptr12.i53 = ptrtoint i8* %5 to i64
  %24 = ptrtoint i8* %ptr.0.lcssa to i64
  %25 = sub i64 %end_ptr12.i53, %24
  %scevgep.i = getelementptr i8, i8* %ptr.0.lcssa, i64 %25
  br label %bb2.i60

bb2.i60:                                          ; preds = %bb6.i62, %bb2.preheader.i54
  %ptr.010.i55 = phi i8* [ %26, %bb6.i62 ], [ %ptr.0.lcssa, %bb2.preheader.i54 ]
  %_11.i56 = load i8, i8* %ptr.010.i55, align 1, !noalias !19
  %_3.i.i57 = icmp eq i8 %_11.i56, %0
  %_6.i.i58 = icmp eq i8 %_11.i56, %1
  %or.cond.i59 = select i1 %_3.i.i57, i1 true, i1 %_6.i.i58
  br i1 %or.cond.i59, label %bb4.i65, label %bb6.i62

bb6.i62:                                          ; preds = %bb2.i60
  %26 = getelementptr inbounds i8, i8* %ptr.010.i55, i64 1
  %exitcond.not.i61 = icmp eq i8* %26, %scevgep.i
  br i1 %exitcond.not.i61, label %bb33, label %bb2.i60

bb4.i65:                                          ; preds = %bb2.i60
  %_3.i6.i63 = ptrtoint i8* %ptr.010.i55 to i64
  %27 = sub i64 %_3.i6.i63, %_48
  br label %bb33

bb2.i.1:                                          ; preds = %bb6.i
  %_11.i.1 = load i8, i8* %12, align 1, !noalias !11
  %_3.i.i.1 = icmp eq i8 %_11.i.1, %0
  %_6.i.i.1 = icmp eq i8 %_11.i.1, %1
  %or.cond.i.1 = select i1 %_3.i.i.1, i1 true, i1 %_6.i.i.1
  br i1 %or.cond.i.1, label %bb4.i, label %bb6.i.1

bb6.i.1:                                          ; preds = %bb2.i.1
  %28 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 2
  %exitcond.not.i.1 = icmp eq i64 %haystack.1, 2
  br i1 %exitcond.not.i.1, label %bb33, label %bb2.i.2

bb2.i.2:                                          ; preds = %bb6.i.1
  %_11.i.2 = load i8, i8* %28, align 1, !noalias !11
  %_3.i.i.2 = icmp eq i8 %_11.i.2, %0
  %_6.i.i.2 = icmp eq i8 %_11.i.2, %1
  %or.cond.i.2 = select i1 %_3.i.i.2, i1 true, i1 %_6.i.i.2
  br i1 %or.cond.i.2, label %bb4.i, label %bb6.i.2

bb6.i.2:                                          ; preds = %bb2.i.2
  %29 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 3
  %exitcond.not.i.2 = icmp eq i64 %haystack.1, 3
  br i1 %exitcond.not.i.2, label %bb33, label %bb2.i.3

bb2.i.3:                                          ; preds = %bb6.i.2
  %_11.i.3 = load i8, i8* %29, align 1, !noalias !11
  %_3.i.i.3 = icmp eq i8 %_11.i.3, %0
  %_6.i.i.3 = icmp eq i8 %_11.i.3, %1
  %or.cond.i.3 = select i1 %_3.i.i.3, i1 true, i1 %_6.i.i.3
  br i1 %or.cond.i.3, label %bb4.i, label %bb6.i.3

bb6.i.3:                                          ; preds = %bb2.i.3
  %30 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 4
  %exitcond.not.i.3 = icmp eq i64 %haystack.1, 4
  br i1 %exitcond.not.i.3, label %bb33, label %bb2.i.4

bb2.i.4:                                          ; preds = %bb6.i.3
  %_11.i.4 = load i8, i8* %30, align 1, !noalias !11
  %_3.i.i.4 = icmp eq i8 %_11.i.4, %0
  %_6.i.i.4 = icmp eq i8 %_11.i.4, %1
  %or.cond.i.4 = select i1 %_3.i.i.4, i1 true, i1 %_6.i.i.4
  br i1 %or.cond.i.4, label %bb4.i, label %bb6.i.4

bb6.i.4:                                          ; preds = %bb2.i.4
  %31 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 5
  %exitcond.not.i.4 = icmp eq i64 %haystack.1, 5
  br i1 %exitcond.not.i.4, label %bb33, label %bb2.i.5

bb2.i.5:                                          ; preds = %bb6.i.4
  %_11.i.5 = load i8, i8* %31, align 1, !noalias !11
  %_3.i.i.5 = icmp eq i8 %_11.i.5, %0
  %_6.i.i.5 = icmp eq i8 %_11.i.5, %1
  %or.cond.i.5 = select i1 %_3.i.i.5, i1 true, i1 %_6.i.i.5
  br i1 %or.cond.i.5, label %bb4.i, label %bb6.i.5

bb6.i.5:                                          ; preds = %bb2.i.5
  %32 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 6
  %exitcond.not.i.5 = icmp eq i64 %haystack.1, 6
  br i1 %exitcond.not.i.5, label %bb33, label %bb2.i.6

bb2.i.6:                                          ; preds = %bb6.i.5
  %_11.i.6 = load i8, i8* %32, align 1, !noalias !11
  %_3.i.i.6 = icmp eq i8 %_11.i.6, %0
  %_6.i.i.6 = icmp eq i8 %_11.i.6, %1
  %or.cond.i.6 = select i1 %_3.i.i.6, i1 true, i1 %_6.i.i.6
  br i1 %or.cond.i.6, label %bb4.i, label %bb33
}

; memchr::memchr::fallback::memchr3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr8fallback7memchr317hb1fbce42d5904accE(i8 %0, i8 %1, i8 %2, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #1 personality i32 (...)* @rust_eh_personality {
start:
  %_2.i = zext i8 %0 to i64
  %3 = mul nuw i64 %_2.i, 72340172838076673
  %_2.i44 = zext i8 %1 to i64
  %4 = mul nuw i64 %_2.i44, 72340172838076673
  %_2.i45 = zext i8 %2 to i64
  %5 = mul nuw i64 %_2.i45, 72340172838076673
  %6 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %7 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_23 = icmp ult i64 %haystack.1, 8
  br i1 %_23, label %bb6, label %bb8

bb8:                                              ; preds = %start
  %_31 = bitcast [0 x i8]* %haystack.0 to i64*
  %_31.val = load i64, i64* %_31, align 1
  %_34 = xor i64 %_31.val, %3
  %8 = add i64 %_34, -72340172838076673
  %_6.i = and i64 %_34, -9187201950435737472
  %_3.i = xor i64 %_6.i, -9187201950435737472
  %_2.i46 = and i64 %_3.i, %8
  %9 = icmp ne i64 %_2.i46, 0
  %_38 = xor i64 %_31.val, %4
  %10 = add i64 %_38, -72340172838076673
  %_6.i47 = and i64 %_38, -9187201950435737472
  %_3.i48 = xor i64 %_6.i47, -9187201950435737472
  %_2.i49 = and i64 %_3.i48, %10
  %11 = icmp ne i64 %_2.i49, 0
  %_42 = xor i64 %_31.val, %5
  %12 = add i64 %_42, -72340172838076673
  %_6.i50 = and i64 %_42, -9187201950435737472
  %_3.i51 = xor i64 %_6.i50, -9187201950435737472
  %_2.i52 = and i64 %_3.i51, %12
  %13 = icmp ne i64 %_2.i52, 0
  %.eq2 = select i1 %9, i1 true, i1 %11
  %brmerge = select i1 %.eq2, i1 true, i1 %13
  %14 = trunc i64 %_31.val to i8
  br i1 %brmerge, label %bb19, label %bb21

bb6:                                              ; preds = %start
  %_53.i.not = icmp eq i64 %haystack.1, 0
  br i1 %_53.i.not, label %bb42, label %bb2.lr.ph.i

bb2.lr.ph.i:                                      ; preds = %bb6
  %15 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_11.i = load i8, i8* %6, align 1
  %_4.i.i = icmp eq i8 %_11.i, %0
  %_7.i.i = icmp eq i8 %_11.i, %1
  %or.cond = select i1 %_4.i.i, i1 true, i1 %_7.i.i
  %_10.i.i = icmp eq i8 %_11.i, %2
  %or.cond123 = select i1 %or.cond, i1 true, i1 %_10.i.i
  br i1 %or.cond123, label %bb4.i, label %bb6.i

bb6.i:                                            ; preds = %bb2.lr.ph.i
  %16 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 1
  %exitcond.not.i = icmp eq i64 %haystack.1, 1
  br i1 %exitcond.not.i, label %bb42, label %bb2.i.1

bb4.i:                                            ; preds = %bb2.i.6, %bb2.i.5, %bb2.i.4, %bb2.i.3, %bb2.i.2, %bb2.i.1, %bb2.lr.ph.i
  %ptr.04.i.lcssa = phi i8* [ %6, %bb2.lr.ph.i ], [ %16, %bb2.i.1 ], [ %34, %bb2.i.2 ], [ %35, %bb2.i.3 ], [ %36, %bb2.i.4 ], [ %37, %bb2.i.5 ], [ %38, %bb2.i.6 ]
  %_3.i.i = ptrtoint i8* %ptr.04.i.lcssa to i64
  %17 = sub i64 %_3.i.i, %15
  br label %bb42

bb21:                                             ; preds = %bb8
  %_58 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_57 = and i64 %_58, 7
  %_56 = sub nuw nsw i64 8, %_57
  %18 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_56
  %19 = getelementptr inbounds i8, i8* %7, i64 -8
  %_61.not134 = icmp ugt i8* %18, %19
  br i1 %_61.not134, label %bb39, label %bb25

bb19:                                             ; preds = %bb8
  %_53.i53 = icmp sgt i64 %haystack.1, 0
  br i1 %_53.i53, label %bb2.lr.ph.i56, label %bb42

bb2.lr.ph.i56:                                    ; preds = %bb19
  %20 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_4.i.i59153 = icmp eq i8 %14, %0
  %_7.i.i62154 = icmp eq i8 %14, %1
  %or.cond124155 = select i1 %_4.i.i59153, i1 true, i1 %_7.i.i62154
  %_10.i.i65156 = icmp eq i8 %14, %2
  %or.cond125157 = select i1 %or.cond124155, i1 true, i1 %_10.i.i65156
  br i1 %or.cond125157, label %bb4.i71, label %bb6.i68

bb6.i68:                                          ; preds = %bb2.lr.ph.i56, %bb6.i68.bb2.i60_crit_edge
  %ptr.04.i57158 = phi i8* [ %21, %bb6.i68.bb2.i60_crit_edge ], [ %6, %bb2.lr.ph.i56 ]
  %21 = getelementptr inbounds i8, i8* %ptr.04.i57158, i64 1
  %exitcond.not.i67 = icmp eq i8* %21, %7
  br i1 %exitcond.not.i67, label %bb42, label %bb6.i68.bb2.i60_crit_edge

bb6.i68.bb2.i60_crit_edge:                        ; preds = %bb6.i68
  %_11.i58.pre = load i8, i8* %21, align 1
  %_4.i.i59 = icmp eq i8 %_11.i58.pre, %0
  %_7.i.i62 = icmp eq i8 %_11.i58.pre, %1
  %or.cond124 = select i1 %_4.i.i59, i1 true, i1 %_7.i.i62
  %_10.i.i65 = icmp eq i8 %_11.i58.pre, %2
  %or.cond125 = select i1 %or.cond124, i1 true, i1 %_10.i.i65
  br i1 %or.cond125, label %bb4.i71, label %bb6.i68

bb4.i71:                                          ; preds = %bb6.i68.bb2.i60_crit_edge, %bb2.lr.ph.i56
  %ptr.04.i57.lcssa = phi i8* [ %6, %bb2.lr.ph.i56 ], [ %21, %bb6.i68.bb2.i60_crit_edge ]
  %_3.i.i69 = ptrtoint i8* %ptr.04.i57.lcssa to i64
  %22 = sub i64 %_3.i.i69, %20
  br label %bb42

bb42:                                             ; preds = %bb2.i.6, %bb6.i99, %bb6.i68, %bb6.i, %bb6.i.1, %bb6.i.2, %bb6.i.3, %bb6.i.4, %bb6.i.5, %bb4.i102, %bb39, %bb4.i71, %bb19, %bb4.i, %bb6
  %.sroa.0.0.i104.pn = phi i64 [ 1, %bb4.i ], [ 0, %bb6 ], [ 1, %bb4.i71 ], [ 0, %bb19 ], [ 1, %bb4.i102 ], [ 0, %bb39 ], [ 0, %bb6.i.5 ], [ 0, %bb6.i.4 ], [ 0, %bb6.i.3 ], [ 0, %bb6.i.2 ], [ 0, %bb6.i.1 ], [ 0, %bb6.i ], [ 0, %bb6.i68 ], [ 0, %bb6.i99 ], [ 0, %bb2.i.6 ]
  %.sroa.3.0.i103.pn = phi i64 [ %17, %bb4.i ], [ undef, %bb6 ], [ %22, %bb4.i71 ], [ undef, %bb19 ], [ %33, %bb4.i102 ], [ undef, %bb39 ], [ undef, %bb6.i.5 ], [ undef, %bb6.i.4 ], [ undef, %bb6.i.3 ], [ undef, %bb6.i.2 ], [ undef, %bb6.i.1 ], [ undef, %bb6.i ], [ undef, %bb6.i68 ], [ undef, %bb6.i99 ], [ undef, %bb2.i.6 ]
  %.pn = insertvalue { i64, i64 } undef, i64 %.sroa.0.0.i104.pn, 0
  %.pn.pn = insertvalue { i64, i64 } %.pn, i64 %.sroa.3.0.i103.pn, 1
  ret { i64, i64 } %.pn.pn

bb25:                                             ; preds = %bb21, %bb36
  %ptr.0135 = phi i8* [ %29, %bb36 ], [ %18, %bb21 ]
  %_69 = bitcast i8* %ptr.0135 to i64*
  %chunk1 = load i64, i64* %_69, align 8
  %_72 = xor i64 %chunk1, %3
  %23 = add i64 %_72, -72340172838076673
  %_6.i75 = and i64 %_72, -9187201950435737472
  %_3.i76 = xor i64 %_6.i75, -9187201950435737472
  %_2.i77 = and i64 %_3.i76, %23
  %24 = icmp ne i64 %_2.i77, 0
  %_76 = xor i64 %chunk1, %4
  %25 = add i64 %_76, -72340172838076673
  %_6.i78 = and i64 %_76, -9187201950435737472
  %_3.i79 = xor i64 %_6.i78, -9187201950435737472
  %_2.i80 = and i64 %_3.i79, %25
  %26 = icmp ne i64 %_2.i80, 0
  %_80 = xor i64 %chunk1, %5
  %27 = add i64 %_80, -72340172838076673
  %_6.i81 = and i64 %_80, -9187201950435737472
  %_3.i82 = xor i64 %_6.i81, -9187201950435737472
  %_2.i83 = and i64 %_3.i82, %27
  %28 = icmp ne i64 %_2.i83, 0
  %.eq23 = select i1 %24, i1 true, i1 %26
  %brmerge37 = select i1 %.eq23, i1 true, i1 %28
  br i1 %brmerge37, label %bb39, label %bb36

bb36:                                             ; preds = %bb25
  %29 = getelementptr inbounds i8, i8* %ptr.0135, i64 8
  %_61.not = icmp ugt i8* %29, %19
  br i1 %_61.not, label %bb39, label %bb25

bb39:                                             ; preds = %bb36, %bb25, %bb21
  %ptr.0.lcssa = phi i8* [ %18, %bb21 ], [ %ptr.0135, %bb25 ], [ %29, %bb36 ]
  %_53.i84 = icmp ult i8* %ptr.0.lcssa, %7
  br i1 %_53.i84, label %bb2.lr.ph.i87, label %bb42

bb2.lr.ph.i87:                                    ; preds = %bb39
  %end_ptr6.i85 = ptrtoint i8* %7 to i64
  %30 = ptrtoint i8* %ptr.0.lcssa to i64
  %31 = sub i64 %end_ptr6.i85, %30
  %scevgep.i = getelementptr i8, i8* %ptr.0.lcssa, i64 %31
  br label %bb2.i91

bb2.i91:                                          ; preds = %bb6.i99, %bb2.lr.ph.i87
  %ptr.04.i88 = phi i8* [ %ptr.0.lcssa, %bb2.lr.ph.i87 ], [ %32, %bb6.i99 ]
  %_11.i89 = load i8, i8* %ptr.04.i88, align 1
  %_4.i.i90 = icmp eq i8 %_11.i89, %0
  %_7.i.i93 = icmp eq i8 %_11.i89, %1
  %or.cond126 = select i1 %_4.i.i90, i1 true, i1 %_7.i.i93
  %_10.i.i96 = icmp eq i8 %_11.i89, %2
  %or.cond127 = select i1 %or.cond126, i1 true, i1 %_10.i.i96
  br i1 %or.cond127, label %bb4.i102, label %bb6.i99

bb6.i99:                                          ; preds = %bb2.i91
  %32 = getelementptr inbounds i8, i8* %ptr.04.i88, i64 1
  %exitcond.not.i98 = icmp eq i8* %32, %scevgep.i
  br i1 %exitcond.not.i98, label %bb42, label %bb2.i91

bb4.i102:                                         ; preds = %bb2.i91
  %_3.i.i100 = ptrtoint i8* %ptr.04.i88 to i64
  %33 = sub i64 %_3.i.i100, %_58
  br label %bb42

bb2.i.1:                                          ; preds = %bb6.i
  %_11.i.1 = load i8, i8* %16, align 1
  %_4.i.i.1 = icmp eq i8 %_11.i.1, %0
  %_7.i.i.1 = icmp eq i8 %_11.i.1, %1
  %or.cond.1 = select i1 %_4.i.i.1, i1 true, i1 %_7.i.i.1
  %_10.i.i.1 = icmp eq i8 %_11.i.1, %2
  %or.cond123.1 = select i1 %or.cond.1, i1 true, i1 %_10.i.i.1
  br i1 %or.cond123.1, label %bb4.i, label %bb6.i.1

bb6.i.1:                                          ; preds = %bb2.i.1
  %34 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 2
  %exitcond.not.i.1 = icmp eq i64 %haystack.1, 2
  br i1 %exitcond.not.i.1, label %bb42, label %bb2.i.2

bb2.i.2:                                          ; preds = %bb6.i.1
  %_11.i.2 = load i8, i8* %34, align 1
  %_4.i.i.2 = icmp eq i8 %_11.i.2, %0
  %_7.i.i.2 = icmp eq i8 %_11.i.2, %1
  %or.cond.2 = select i1 %_4.i.i.2, i1 true, i1 %_7.i.i.2
  %_10.i.i.2 = icmp eq i8 %_11.i.2, %2
  %or.cond123.2 = select i1 %or.cond.2, i1 true, i1 %_10.i.i.2
  br i1 %or.cond123.2, label %bb4.i, label %bb6.i.2

bb6.i.2:                                          ; preds = %bb2.i.2
  %35 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 3
  %exitcond.not.i.2 = icmp eq i64 %haystack.1, 3
  br i1 %exitcond.not.i.2, label %bb42, label %bb2.i.3

bb2.i.3:                                          ; preds = %bb6.i.2
  %_11.i.3 = load i8, i8* %35, align 1
  %_4.i.i.3 = icmp eq i8 %_11.i.3, %0
  %_7.i.i.3 = icmp eq i8 %_11.i.3, %1
  %or.cond.3 = select i1 %_4.i.i.3, i1 true, i1 %_7.i.i.3
  %_10.i.i.3 = icmp eq i8 %_11.i.3, %2
  %or.cond123.3 = select i1 %or.cond.3, i1 true, i1 %_10.i.i.3
  br i1 %or.cond123.3, label %bb4.i, label %bb6.i.3

bb6.i.3:                                          ; preds = %bb2.i.3
  %36 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 4
  %exitcond.not.i.3 = icmp eq i64 %haystack.1, 4
  br i1 %exitcond.not.i.3, label %bb42, label %bb2.i.4

bb2.i.4:                                          ; preds = %bb6.i.3
  %_11.i.4 = load i8, i8* %36, align 1
  %_4.i.i.4 = icmp eq i8 %_11.i.4, %0
  %_7.i.i.4 = icmp eq i8 %_11.i.4, %1
  %or.cond.4 = select i1 %_4.i.i.4, i1 true, i1 %_7.i.i.4
  %_10.i.i.4 = icmp eq i8 %_11.i.4, %2
  %or.cond123.4 = select i1 %or.cond.4, i1 true, i1 %_10.i.i.4
  br i1 %or.cond123.4, label %bb4.i, label %bb6.i.4

bb6.i.4:                                          ; preds = %bb2.i.4
  %37 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 5
  %exitcond.not.i.4 = icmp eq i64 %haystack.1, 5
  br i1 %exitcond.not.i.4, label %bb42, label %bb2.i.5

bb2.i.5:                                          ; preds = %bb6.i.4
  %_11.i.5 = load i8, i8* %37, align 1
  %_4.i.i.5 = icmp eq i8 %_11.i.5, %0
  %_7.i.i.5 = icmp eq i8 %_11.i.5, %1
  %or.cond.5 = select i1 %_4.i.i.5, i1 true, i1 %_7.i.i.5
  %_10.i.i.5 = icmp eq i8 %_11.i.5, %2
  %or.cond123.5 = select i1 %or.cond.5, i1 true, i1 %_10.i.i.5
  br i1 %or.cond123.5, label %bb4.i, label %bb6.i.5

bb6.i.5:                                          ; preds = %bb2.i.5
  %38 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 6
  %exitcond.not.i.5 = icmp eq i64 %haystack.1, 6
  br i1 %exitcond.not.i.5, label %bb42, label %bb2.i.6

bb2.i.6:                                          ; preds = %bb6.i.5
  %_11.i.6 = load i8, i8* %38, align 1
  %_4.i.i.6 = icmp eq i8 %_11.i.6, %0
  %_7.i.i.6 = icmp eq i8 %_11.i.6, %1
  %or.cond.6 = select i1 %_4.i.i.6, i1 true, i1 %_7.i.i.6
  %_10.i.i.6 = icmp eq i8 %_11.i.6, %2
  %or.cond123.6 = select i1 %or.cond.6, i1 true, i1 %_10.i.i.6
  br i1 %or.cond123.6, label %bb4.i, label %bb42
}

; memchr::memchr::fallback::memrchr
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr8fallback7memrchr17h496897d60c974fbbE(i8 %0, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #1 personality i32 (...)* @rust_eh_personality {
start:
  %_2.i = zext i8 %0 to i64
  %1 = mul nuw i64 %_2.i, 72340172838076673
  %2 = icmp ult i64 %haystack.1, 16
  %.0.sroa.speculated.i.i.i = select i1 %2, i64 %haystack.1, i64 16
  %3 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %4 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_18 = icmp ult i64 %haystack.1, 8
  br i1 %_18, label %bb1.i, label %bb7

bb7:                                              ; preds = %start
  %5 = getelementptr inbounds i8, i8* %4, i64 -8
  %_26 = bitcast i8* %5 to i64*
  %_26.val = load i64, i64* %_26, align 1
  %_30 = xor i64 %_26.val, %1
  %6 = add i64 %_30, -72340172838076673
  %_6.i = and i64 %_30, -9187201950435737472
  %_3.i = xor i64 %_6.i, -9187201950435737472
  %_2.i15 = and i64 %_3.i, %6
  %.not = icmp eq i64 %_2.i15, 0
  br i1 %.not, label %bb13, label %bb1.i19

bb1.i:                                            ; preds = %start, %bb2.i
  %ptr.0.i = phi i8* [ %7, %bb2.i ], [ %4, %start ]
  %_5.i = icmp ugt i8* %ptr.0.i, %3
  br i1 %_5.i, label %bb2.i, label %bb34

bb2.i:                                            ; preds = %bb1.i
  %7 = getelementptr inbounds i8, i8* %ptr.0.i, i64 -1
  %_13.i = load i8, i8* %7, align 1, !noalias !23
  %8 = icmp eq i8 %_13.i, %0
  br i1 %8, label %bb5.i, label %bb1.i

bb5.i:                                            ; preds = %bb2.i
  %_3.i.i = ptrtoint i8* %7 to i64
  %_5.i.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %9 = sub i64 %_3.i.i, %_5.i.i
  br label %bb34

bb13:                                             ; preds = %bb7
  %_38 = ptrtoint i8* %4 to i64
  %_37 = and i64 %_38, -8
  %10 = inttoptr i64 %_37 to i8*
  %_43 = icmp ugt i64 %haystack.1, 15
  %11 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 16
  %12 = sub nsw i64 0, %.0.sroa.speculated.i.i.i
  %_4553 = icmp ule i8* %11, %10
  %or.cond54 = select i1 %_43, i1 %_4553, i1 false
  br i1 %or.cond54, label %bb19, label %bb1.i37.preheader

bb1.i19:                                          ; preds = %bb7, %bb2.i21
  %ptr.0.i17 = phi i8* [ %13, %bb2.i21 ], [ %4, %bb7 ]
  %_5.i18 = icmp ugt i8* %ptr.0.i17, %3
  br i1 %_5.i18, label %bb2.i21, label %bb34

bb2.i21:                                          ; preds = %bb1.i19
  %13 = getelementptr inbounds i8, i8* %ptr.0.i17, i64 -1
  %_13.i20 = load i8, i8* %13, align 1, !noalias !26
  %14 = icmp eq i8 %_13.i20, %0
  br i1 %14, label %bb5.i24, label %bb1.i19

bb5.i24:                                          ; preds = %bb2.i21
  %_3.i.i22 = ptrtoint i8* %13 to i64
  %_5.i.i23 = ptrtoint [0 x i8]* %haystack.0 to i64
  %15 = sub i64 %_3.i.i22, %_5.i.i23
  br label %bb34

bb34:                                             ; preds = %bb1.i19, %bb1.i37, %bb1.i, %bb5.i42, %bb5.i24, %bb5.i
  %.sroa.0.0.i44.pn = phi i64 [ 1, %bb5.i ], [ 1, %bb5.i24 ], [ 1, %bb5.i42 ], [ 0, %bb1.i ], [ 0, %bb1.i37 ], [ 0, %bb1.i19 ]
  %.sroa.3.0.i43.pn = phi i64 [ %9, %bb5.i ], [ %15, %bb5.i24 ], [ %25, %bb5.i42 ], [ undef, %bb1.i ], [ undef, %bb1.i37 ], [ undef, %bb1.i19 ]
  %.pn = insertvalue { i64, i64 } undef, i64 %.sroa.0.0.i44.pn, 0
  %.pn.pn = insertvalue { i64, i64 } %.pn, i64 %.sroa.3.0.i43.pn, 1
  ret { i64, i64 } %.pn.pn

bb19:                                             ; preds = %bb13, %bb28
  %ptr.055 = phi i8* [ %22, %bb28 ], [ %10, %bb13 ]
  %16 = getelementptr inbounds i8, i8* %ptr.055, i64 -16
  %_54 = bitcast i8* %16 to i64*
  %a = load i64, i64* %_54, align 8
  %17 = getelementptr inbounds i8, i8* %ptr.055, i64 -8
  %_59 = bitcast i8* %17 to i64*
  %b = load i64, i64* %_59, align 8
  %_64 = xor i64 %a, %1
  %18 = add i64 %_64, -72340172838076673
  %_6.i28 = and i64 %_64, -9187201950435737472
  %_3.i29 = xor i64 %_6.i28, -9187201950435737472
  %_2.i30 = and i64 %_3.i29, %18
  %19 = icmp ne i64 %_2.i30, 0
  %_68 = xor i64 %b, %1
  %20 = add i64 %_68, -72340172838076673
  %_6.i31 = and i64 %_68, -9187201950435737472
  %_3.i32 = xor i64 %_6.i31, -9187201950435737472
  %_2.i33 = and i64 %_3.i32, %20
  %21 = icmp ne i64 %_2.i33, 0
  %.eqb = select i1 %19, i1 true, i1 %21
  br i1 %.eqb, label %bb1.i37.preheader, label %bb28

bb28:                                             ; preds = %bb19
  %22 = getelementptr inbounds i8, i8* %ptr.055, i64 %12
  %_45.not = icmp ult i8* %22, %11
  br i1 %_45.not, label %bb1.i37.preheader, label %bb19

bb1.i37.preheader:                                ; preds = %bb19, %bb28, %bb13
  %ptr.0.i35.ph = phi i8* [ %10, %bb13 ], [ %ptr.055, %bb19 ], [ %22, %bb28 ]
  br label %bb1.i37

bb1.i37:                                          ; preds = %bb1.i37.preheader, %bb2.i39
  %ptr.0.i35 = phi i8* [ %23, %bb2.i39 ], [ %ptr.0.i35.ph, %bb1.i37.preheader ]
  %_5.i36 = icmp ugt i8* %ptr.0.i35, %3
  br i1 %_5.i36, label %bb2.i39, label %bb34

bb2.i39:                                          ; preds = %bb1.i37
  %23 = getelementptr inbounds i8, i8* %ptr.0.i35, i64 -1
  %_13.i38 = load i8, i8* %23, align 1, !noalias !29
  %24 = icmp eq i8 %_13.i38, %0
  br i1 %24, label %bb5.i42, label %bb1.i37

bb5.i42:                                          ; preds = %bb2.i39
  %_3.i.i40 = ptrtoint i8* %23 to i64
  %_5.i.i41 = ptrtoint [0 x i8]* %haystack.0 to i64
  %25 = sub i64 %_3.i.i40, %_5.i.i41
  br label %bb34
}

; memchr::memchr::fallback::memrchr2
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr8fallback8memrchr217h986d7a15df682b91E(i8 %0, i8 %1, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #1 personality i32 (...)* @rust_eh_personality {
start:
  %_2.i = zext i8 %0 to i64
  %2 = mul nuw i64 %_2.i, 72340172838076673
  %_2.i20 = zext i8 %1 to i64
  %3 = mul nuw i64 %_2.i20, 72340172838076673
  %4 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %5 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_19 = icmp ult i64 %haystack.1, 8
  br i1 %_19, label %bb1.i, label %bb7

bb7:                                              ; preds = %start
  %6 = getelementptr inbounds i8, i8* %5, i64 -8
  %_27 = bitcast i8* %6 to i64*
  %_27.val = load i64, i64* %_27, align 1
  %_31 = xor i64 %_27.val, %2
  %7 = add i64 %_31, -72340172838076673
  %_6.i = and i64 %_31, -9187201950435737472
  %_3.i = xor i64 %_6.i, -9187201950435737472
  %_2.i21 = and i64 %_3.i, %7
  %8 = icmp ne i64 %_2.i21, 0
  %_35 = xor i64 %_27.val, %3
  %9 = add i64 %_35, -72340172838076673
  %_6.i22 = and i64 %_35, -9187201950435737472
  %_3.i23 = xor i64 %_6.i22, -9187201950435737472
  %_2.i24 = and i64 %_3.i23, %9
  %10 = icmp ne i64 %_2.i24, 0
  %.eq2 = select i1 %8, i1 true, i1 %10
  br i1 %.eq2, label %bb1.i29, label %bb17

bb1.i:                                            ; preds = %start, %bb2.i
  %ptr.0.i = phi i8* [ %11, %bb2.i ], [ %5, %start ]
  %_5.i = icmp ugt i8* %ptr.0.i, %4
  br i1 %_5.i, label %bb2.i, label %bb34

bb2.i:                                            ; preds = %bb1.i
  %11 = getelementptr inbounds i8, i8* %ptr.0.i, i64 -1
  %_13.i = load i8, i8* %11, align 1, !noalias !32
  %_3.i.i = icmp eq i8 %_13.i, %0
  %_6.i.i = icmp eq i8 %_13.i, %1
  %or.cond.i = select i1 %_3.i.i, i1 true, i1 %_6.i.i
  br i1 %or.cond.i, label %bb5.i, label %bb1.i

bb5.i:                                            ; preds = %bb2.i
  %_3.i2.i = ptrtoint i8* %11 to i64
  %_5.i.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %12 = sub i64 %_3.i2.i, %_5.i.i
  br label %bb34

bb17:                                             ; preds = %bb7
  %_46 = ptrtoint i8* %5 to i64
  %_45 = and i64 %_46, -8
  %13 = inttoptr i64 %_45 to i8*
  %14 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 8
  br label %bb18

bb1.i29:                                          ; preds = %bb7, %bb2.i34
  %ptr.0.i27 = phi i8* [ %15, %bb2.i34 ], [ %5, %bb7 ]
  %_5.i28 = icmp ugt i8* %ptr.0.i27, %4
  br i1 %_5.i28, label %bb2.i34, label %bb34

bb2.i34:                                          ; preds = %bb1.i29
  %15 = getelementptr inbounds i8, i8* %ptr.0.i27, i64 -1
  %_13.i30 = load i8, i8* %15, align 1, !noalias !36
  %_3.i.i31 = icmp eq i8 %_13.i30, %0
  %_6.i.i32 = icmp eq i8 %_13.i30, %1
  %or.cond.i33 = select i1 %_3.i.i31, i1 true, i1 %_6.i.i32
  br i1 %or.cond.i33, label %bb5.i37, label %bb1.i29

bb5.i37:                                          ; preds = %bb2.i34
  %_3.i2.i35 = ptrtoint i8* %15 to i64
  %_5.i.i36 = ptrtoint [0 x i8]* %haystack.0 to i64
  %16 = sub i64 %_3.i2.i35, %_5.i.i36
  br label %bb34

bb34:                                             ; preds = %bb1.i51, %bb1.i29, %bb1.i, %bb5.i59, %bb5.i37, %bb5.i
  %.sroa.0.0.i61.pn = phi i64 [ 1, %bb5.i ], [ 1, %bb5.i37 ], [ 1, %bb5.i59 ], [ 0, %bb1.i ], [ 0, %bb1.i29 ], [ 0, %bb1.i51 ]
  %.sroa.3.0.i60.pn = phi i64 [ %12, %bb5.i ], [ %16, %bb5.i37 ], [ %23, %bb5.i59 ], [ undef, %bb1.i ], [ undef, %bb1.i29 ], [ undef, %bb1.i51 ]
  %.pn = insertvalue { i64, i64 } undef, i64 %.sroa.0.0.i61.pn, 0
  %.pn.pn = insertvalue { i64, i64 } %.pn, i64 %.sroa.3.0.i60.pn, 1
  ret { i64, i64 } %.pn.pn

bb18:                                             ; preds = %bb20, %bb17
  %ptr.0 = phi i8* [ %13, %bb17 ], [ %17, %bb20 ]
  %_50.not = icmp ult i8* %ptr.0, %14
  br i1 %_50.not, label %bb1.i51.preheader, label %bb20

bb20:                                             ; preds = %bb18
  %17 = getelementptr inbounds i8, i8* %ptr.0, i64 -8
  %_58 = bitcast i8* %17 to i64*
  %chunk1 = load i64, i64* %_58, align 8
  %_62 = xor i64 %chunk1, %2
  %18 = add i64 %_62, -72340172838076673
  %_6.i41 = and i64 %_62, -9187201950435737472
  %_3.i42 = xor i64 %_6.i41, -9187201950435737472
  %_2.i43 = and i64 %_3.i42, %18
  %19 = icmp ne i64 %_2.i43, 0
  %_66 = xor i64 %chunk1, %3
  %20 = add i64 %_66, -72340172838076673
  %_6.i44 = and i64 %_66, -9187201950435737472
  %_3.i45 = xor i64 %_6.i44, -9187201950435737472
  %_2.i46 = and i64 %_3.i45, %20
  %21 = icmp ne i64 %_2.i46, 0
  %.eq23 = select i1 %19, i1 true, i1 %21
  br i1 %.eq23, label %bb1.i51.preheader, label %bb18

bb1.i51.preheader:                                ; preds = %bb20, %bb18
  br label %bb1.i51

bb1.i51:                                          ; preds = %bb1.i51.preheader, %bb2.i56
  %ptr.0.i49 = phi i8* [ %22, %bb2.i56 ], [ %ptr.0, %bb1.i51.preheader ]
  %_5.i50 = icmp ugt i8* %ptr.0.i49, %4
  br i1 %_5.i50, label %bb2.i56, label %bb34

bb2.i56:                                          ; preds = %bb1.i51
  %22 = getelementptr inbounds i8, i8* %ptr.0.i49, i64 -1
  %_13.i52 = load i8, i8* %22, align 1, !noalias !40
  %_3.i.i53 = icmp eq i8 %_13.i52, %0
  %_6.i.i54 = icmp eq i8 %_13.i52, %1
  %or.cond.i55 = select i1 %_3.i.i53, i1 true, i1 %_6.i.i54
  br i1 %or.cond.i55, label %bb5.i59, label %bb1.i51

bb5.i59:                                          ; preds = %bb2.i56
  %_3.i2.i57 = ptrtoint i8* %22 to i64
  %_5.i.i58 = ptrtoint [0 x i8]* %haystack.0 to i64
  %23 = sub i64 %_3.i2.i57, %_5.i.i58
  br label %bb34
}

; memchr::memchr::fallback::memrchr3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr8fallback8memrchr317h70c4992730bbc42bE(i8 %0, i8 %1, i8 %2, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #1 personality i32 (...)* @rust_eh_personality {
start:
  %_2.i = zext i8 %0 to i64
  %3 = mul nuw i64 %_2.i, 72340172838076673
  %_2.i44 = zext i8 %1 to i64
  %4 = mul nuw i64 %_2.i44, 72340172838076673
  %_2.i45 = zext i8 %2 to i64
  %5 = mul nuw i64 %_2.i45, 72340172838076673
  %6 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %7 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_23 = icmp ult i64 %haystack.1, 8
  br i1 %_23, label %bb1.i, label %bb8

bb8:                                              ; preds = %start
  %8 = getelementptr inbounds i8, i8* %7, i64 -8
  %_31 = bitcast i8* %8 to i64*
  %_31.val = load i64, i64* %_31, align 1
  %_35 = xor i64 %_31.val, %3
  %9 = add i64 %_35, -72340172838076673
  %_6.i = and i64 %_35, -9187201950435737472
  %_3.i = xor i64 %_6.i, -9187201950435737472
  %_2.i46 = and i64 %_3.i, %9
  %10 = icmp ne i64 %_2.i46, 0
  %_39 = xor i64 %_31.val, %4
  %11 = add i64 %_39, -72340172838076673
  %_6.i47 = and i64 %_39, -9187201950435737472
  %_3.i48 = xor i64 %_6.i47, -9187201950435737472
  %_2.i49 = and i64 %_3.i48, %11
  %12 = icmp ne i64 %_2.i49, 0
  %_43 = xor i64 %_31.val, %5
  %13 = add i64 %_43, -72340172838076673
  %_6.i50 = and i64 %_43, -9187201950435737472
  %_3.i51 = xor i64 %_6.i50, -9187201950435737472
  %_2.i52 = and i64 %_3.i51, %13
  %14 = icmp ne i64 %_2.i52, 0
  %.eq2 = select i1 %10, i1 true, i1 %12
  %brmerge = select i1 %.eq2, i1 true, i1 %14
  br i1 %brmerge, label %bb1.i55, label %bb22

bb1.i:                                            ; preds = %start, %bb2.i
  %ptr.0.i = phi i8* [ %15, %bb2.i ], [ %7, %start ]
  %_5.i = icmp ugt i8* %ptr.0.i, %6
  br i1 %_5.i, label %bb2.i, label %bb43

bb2.i:                                            ; preds = %bb1.i
  %15 = getelementptr inbounds i8, i8* %ptr.0.i, i64 -1
  %_13.i = load i8, i8* %15, align 1
  %_4.i.i = icmp eq i8 %_13.i, %0
  %_7.i.i = icmp eq i8 %_13.i, %1
  %or.cond = select i1 %_4.i.i, i1 true, i1 %_7.i.i
  %_10.i.i = icmp eq i8 %_13.i, %2
  %or.cond117 = select i1 %or.cond, i1 true, i1 %_10.i.i
  br i1 %or.cond117, label %bb5.i, label %bb1.i

bb5.i:                                            ; preds = %bb2.i
  %_3.i.i = ptrtoint i8* %15 to i64
  %_5.i.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %16 = sub i64 %_3.i.i, %_5.i.i
  br label %bb43

bb22:                                             ; preds = %bb8
  %_56 = ptrtoint i8* %7 to i64
  %_55 = and i64 %_56, -8
  %17 = inttoptr i64 %_55 to i8*
  %18 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 8
  br label %bb23

bb1.i55:                                          ; preds = %bb8, %bb2.i59
  %ptr.0.i53 = phi i8* [ %19, %bb2.i59 ], [ %7, %bb8 ]
  %_5.i54 = icmp ugt i8* %ptr.0.i53, %6
  br i1 %_5.i54, label %bb2.i59, label %bb43

bb2.i59:                                          ; preds = %bb1.i55
  %19 = getelementptr inbounds i8, i8* %ptr.0.i53, i64 -1
  %_13.i56 = load i8, i8* %19, align 1
  %_4.i.i58 = icmp eq i8 %_13.i56, %0
  %_7.i.i61 = icmp eq i8 %_13.i56, %1
  %or.cond118 = select i1 %_4.i.i58, i1 true, i1 %_7.i.i61
  %_10.i.i64 = icmp eq i8 %_13.i56, %2
  %or.cond119 = select i1 %or.cond118, i1 true, i1 %_10.i.i64
  br i1 %or.cond119, label %bb5.i68, label %bb1.i55

bb5.i68:                                          ; preds = %bb2.i59
  %_3.i.i66 = ptrtoint i8* %19 to i64
  %_5.i.i67 = ptrtoint [0 x i8]* %haystack.0 to i64
  %20 = sub i64 %_3.i.i66, %_5.i.i67
  br label %bb43

bb43:                                             ; preds = %bb1.i83, %bb1.i55, %bb1.i, %bb5.i96, %bb5.i68, %bb5.i
  %.sroa.0.0.i98.pn = phi i64 [ 1, %bb5.i ], [ 1, %bb5.i68 ], [ 1, %bb5.i96 ], [ 0, %bb1.i ], [ 0, %bb1.i55 ], [ 0, %bb1.i83 ]
  %.sroa.3.0.i97.pn = phi i64 [ %16, %bb5.i ], [ %20, %bb5.i68 ], [ %29, %bb5.i96 ], [ undef, %bb1.i ], [ undef, %bb1.i55 ], [ undef, %bb1.i83 ]
  %.pn = insertvalue { i64, i64 } undef, i64 %.sroa.0.0.i98.pn, 0
  %.pn.pn = insertvalue { i64, i64 } %.pn, i64 %.sroa.3.0.i97.pn, 1
  ret { i64, i64 } %.pn.pn

bb23:                                             ; preds = %bb25, %bb22
  %ptr.0 = phi i8* [ %17, %bb22 ], [ %21, %bb25 ]
  %_60.not = icmp ult i8* %ptr.0, %18
  br i1 %_60.not, label %bb1.i83.preheader, label %bb25

bb25:                                             ; preds = %bb23
  %21 = getelementptr inbounds i8, i8* %ptr.0, i64 -8
  %_68 = bitcast i8* %21 to i64*
  %chunk1 = load i64, i64* %_68, align 8
  %_72 = xor i64 %chunk1, %3
  %22 = add i64 %_72, -72340172838076673
  %_6.i72 = and i64 %_72, -9187201950435737472
  %_3.i73 = xor i64 %_6.i72, -9187201950435737472
  %_2.i74 = and i64 %_3.i73, %22
  %23 = icmp ne i64 %_2.i74, 0
  %_76 = xor i64 %chunk1, %4
  %24 = add i64 %_76, -72340172838076673
  %_6.i75 = and i64 %_76, -9187201950435737472
  %_3.i76 = xor i64 %_6.i75, -9187201950435737472
  %_2.i77 = and i64 %_3.i76, %24
  %25 = icmp ne i64 %_2.i77, 0
  %_80 = xor i64 %chunk1, %5
  %26 = add i64 %_80, -72340172838076673
  %_6.i78 = and i64 %_80, -9187201950435737472
  %_3.i79 = xor i64 %_6.i78, -9187201950435737472
  %_2.i80 = and i64 %_3.i79, %26
  %27 = icmp ne i64 %_2.i80, 0
  %.eq23 = select i1 %23, i1 true, i1 %25
  %brmerge37 = select i1 %.eq23, i1 true, i1 %27
  br i1 %brmerge37, label %bb1.i83.preheader, label %bb23

bb1.i83.preheader:                                ; preds = %bb25, %bb23
  br label %bb1.i83

bb1.i83:                                          ; preds = %bb1.i83.preheader, %bb2.i87
  %ptr.0.i81 = phi i8* [ %28, %bb2.i87 ], [ %ptr.0, %bb1.i83.preheader ]
  %_5.i82 = icmp ugt i8* %ptr.0.i81, %6
  br i1 %_5.i82, label %bb2.i87, label %bb43

bb2.i87:                                          ; preds = %bb1.i83
  %28 = getelementptr inbounds i8, i8* %ptr.0.i81, i64 -1
  %_13.i84 = load i8, i8* %28, align 1
  %_4.i.i86 = icmp eq i8 %_13.i84, %0
  %_7.i.i89 = icmp eq i8 %_13.i84, %1
  %or.cond120 = select i1 %_4.i.i86, i1 true, i1 %_7.i.i89
  %_10.i.i92 = icmp eq i8 %_13.i84, %2
  %or.cond121 = select i1 %or.cond120, i1 true, i1 %_10.i.i92
  br i1 %or.cond121, label %bb5.i96, label %bb1.i83

bb5.i96:                                          ; preds = %bb2.i87
  %_3.i.i94 = ptrtoint i8* %28 to i64
  %_5.i.i95 = ptrtoint [0 x i8]* %haystack.0 to i64
  %29 = sub i64 %_3.i.i94, %_5.i.i95
  br label %bb43
}

; memchr::memchr::x86::sse2::memchr
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr3x864sse26memchr17h8c22de531a37fcc8E(i8 %n1, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %.0.vec.insert.i.i = insertelement <16 x i8> undef, i8 %n1, i32 0
  %.15.vec.insert.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %0 = icmp ult i64 %haystack.1, 64
  %.0.sroa.speculated.i.i.i = select i1 %0, i64 %haystack.1, i64 64
  %1 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_17 = icmp ult i64 %haystack.1, 16
  br i1 %_17, label %bb6.preheader, label %bb13

bb6.preheader:                                    ; preds = %start
  %_20155.not = icmp eq i64 %haystack.1, 0
  br i1 %_20155.not, label %bb76, label %bb7.preheader

bb7.preheader:                                    ; preds = %bb6.preheader
  %2 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  br label %bb7

bb13:                                             ; preds = %start
  %3 = bitcast [0 x i8]* %haystack.0 to <16 x i8>*
  %.0.copyload9.i = load <16 x i8>, <16 x i8>* %3, align 1
  %4 = icmp eq <16 x i8> %.0.copyload9.i, %.15.vec.insert.i.i
  %5 = bitcast <16 x i1> %4 to i16
  %6 = icmp eq i16 %5, 0
  br i1 %6, label %bb16, label %bb15

bb7:                                              ; preds = %bb7.preheader, %bb10
  %ptr.0156 = phi i8* [ %7, %bb10 ], [ %2, %bb7.preheader ]
  %_24 = load i8, i8* %ptr.0156, align 1
  %_23 = icmp eq i8 %_24, %n1
  br i1 %_23, label %bb8, label %bb10

bb10:                                             ; preds = %bb7
  %7 = getelementptr inbounds i8, i8* %ptr.0156, i64 1
  %_20 = icmp ult i8* %7, %1
  br i1 %_20, label %bb7, label %bb76

bb8:                                              ; preds = %bb7
  %_3.i = ptrtoint i8* %ptr.0156 to i64
  %_5.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %8 = sub i64 %_3.i, %_5.i
  br label %bb76

bb15:                                             ; preds = %bb13
  %9 = tail call i16 @llvm.cttz.i16(i16 %5, i1 true) #21, !range !44
  %10 = zext i16 %9 to i64
  br label %bb76

bb16:                                             ; preds = %bb13
  %_43 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_42 = and i64 %_43, 15
  %_41 = sub nuw nsw i64 16, %_42
  %11 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_41
  %_46 = icmp ugt i64 %haystack.1, 63
  %12 = getelementptr inbounds i8, i8* %1, i64 -64
  %_48144 = icmp ule i8* %11, %12
  %or.cond145 = select i1 %_46, i1 %_48144, i1 false
  br i1 %or.cond145, label %bb23, label %bb58.preheader

bb58.preheader:                                   ; preds = %bb55, %bb16
  %ptr.1.lcssa = phi i8* [ %11, %bb16 ], [ %30, %bb55 ]
  %13 = getelementptr inbounds i8, i8* %1, i64 -16
  %_129.not152 = icmp ugt i8* %ptr.1.lcssa, %13
  br i1 %_129.not152, label %bb65, label %bb60

bb23:                                             ; preds = %bb16, %bb55
  %ptr.1146 = phi i8* [ %30, %bb55 ], [ %11, %bb16 ]
  %14 = bitcast i8* %ptr.1146 to <16 x i8>*
  %_57.val133 = load <16 x i8>, <16 x i8>* %14, align 16
  %15 = getelementptr inbounds i8, i8* %ptr.1146, i64 16
  %16 = bitcast i8* %15 to <16 x i8>*
  %_60.val134 = load <16 x i8>, <16 x i8>* %16, align 16
  %17 = getelementptr inbounds i8, i8* %ptr.1146, i64 32
  %18 = bitcast i8* %17 to <16 x i8>*
  %_64.val135 = load <16 x i8>, <16 x i8>* %18, align 16
  %19 = getelementptr inbounds i8, i8* %ptr.1146, i64 48
  %20 = bitcast i8* %19 to <16 x i8>*
  %_69.val136 = load <16 x i8>, <16 x i8>* %20, align 16
  %21 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_57.val133
  %22 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_60.val134
  %23 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_64.val135
  %24 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_69.val136
  %25 = or <16 x i1> %22, %21
  %26 = or <16 x i1> %25, %23
  %27 = or <16 x i1> %26, %24
  %28 = bitcast <16 x i1> %27 to i16
  %29 = icmp eq i16 %28, 0
  br i1 %29, label %bb55, label %bb39

bb55:                                             ; preds = %bb23
  %30 = getelementptr inbounds i8, i8* %ptr.1146, i64 %.0.sroa.speculated.i.i.i
  %_48.not = icmp ugt i8* %30, %12
  br i1 %_48.not, label %bb58.preheader, label %bb23

bb39:                                             ; preds = %bb23
  %_3.i40 = ptrtoint i8* %ptr.1146 to i64
  %31 = sub i64 %_3.i40, %_43
  %32 = bitcast <16 x i1> %21 to i16
  %33 = icmp eq i16 %32, 0
  br i1 %33, label %bb44, label %bb42

bb44:                                             ; preds = %bb39
  %34 = bitcast <16 x i1> %22 to i16
  %35 = icmp eq i16 %34, 0
  br i1 %35, label %bb48, label %bb46

bb42:                                             ; preds = %bb39
  %36 = tail call i16 @llvm.cttz.i16(i16 %32, i1 true), !range !44
  %37 = zext i16 %36 to i64
  %_102 = add i64 %31, %37
  br label %bb76

bb48:                                             ; preds = %bb44
  %38 = bitcast <16 x i1> %23 to i16
  %39 = icmp eq i16 %38, 0
  br i1 %39, label %bb52, label %bb50

bb46:                                             ; preds = %bb44
  %40 = add i64 %31, 16
  %41 = tail call i16 @llvm.cttz.i16(i16 %34, i1 true), !range !44
  %42 = zext i16 %41 to i64
  %_109 = add i64 %40, %42
  br label %bb76

bb52:                                             ; preds = %bb48
  %43 = add i64 %31, 48
  %44 = bitcast <16 x i1> %24 to i16
  %45 = zext i16 %44 to i32
  %46 = tail call i32 @llvm.cttz.i32(i32 %45, i1 false) #21, !range !45
  %47 = zext i32 %46 to i64
  %_122 = add i64 %43, %47
  br label %bb76

bb50:                                             ; preds = %bb48
  %48 = add i64 %31, 32
  %49 = tail call i16 @llvm.cttz.i16(i16 %38, i1 true), !range !44
  %50 = zext i16 %49 to i64
  %_116 = add i64 %48, %50
  br label %bb76

bb65:                                             ; preds = %bb63, %bb58.preheader
  %ptr.2.lcssa = phi i8* [ %ptr.1.lcssa, %bb58.preheader ], [ %58, %bb63 ]
  %_143 = icmp ult i8* %ptr.2.lcssa, %1
  br i1 %_143, label %bb66, label %bb76

bb60:                                             ; preds = %bb58.preheader, %bb63
  %ptr.2153 = phi i8* [ %58, %bb63 ], [ %ptr.1.lcssa, %bb58.preheader ]
  %51 = bitcast i8* %ptr.2153 to <16 x i8>*
  %.0.copyload9.i46 = load <16 x i8>, <16 x i8>* %51, align 1
  %52 = icmp eq <16 x i8> %.0.copyload9.i46, %.15.vec.insert.i.i
  %53 = bitcast <16 x i1> %52 to i16
  %54 = icmp eq i16 %53, 0
  br i1 %54, label %bb63, label %bb62

bb62:                                             ; preds = %bb60
  %_3.i.i47 = ptrtoint i8* %ptr.2153 to i64
  %55 = sub i64 %_3.i.i47, %_43
  %56 = tail call i16 @llvm.cttz.i16(i16 %53, i1 true) #21, !range !44
  %57 = zext i16 %56 to i64
  %_13.i = add i64 %55, %57
  br label %bb76

bb63:                                             ; preds = %bb60
  %58 = getelementptr inbounds i8, i8* %ptr.2153, i64 16
  %_129.not = icmp ugt i8* %58, %13
  br i1 %_129.not, label %bb65, label %bb60

bb66:                                             ; preds = %bb65
  %_3.i53 = ptrtoint i8* %1 to i64
  %_5.i54 = ptrtoint i8* %ptr.2.lcssa to i64
  %.neg.neg = add i64 %_3.i53, -16
  %59 = sub i64 %.neg.neg, %_5.i54
  %60 = getelementptr inbounds i8, i8* %ptr.2.lcssa, i64 %59
  %61 = bitcast i8* %60 to <16 x i8>*
  %.0.copyload9.i55 = load <16 x i8>, <16 x i8>* %61, align 1
  %62 = icmp eq <16 x i8> %.0.copyload9.i55, %.15.vec.insert.i.i
  %63 = bitcast <16 x i1> %62 to i16
  %64 = icmp eq i16 %63, 0
  br i1 %64, label %bb76, label %bb4.i59

bb4.i59:                                          ; preds = %bb66
  %_3.i.i56 = ptrtoint i8* %60 to i64
  %65 = sub i64 %_3.i.i56, %_43
  %66 = tail call i16 @llvm.cttz.i16(i16 %63, i1 true) #21, !range !44
  %67 = zext i16 %66 to i64
  %_13.i58 = add i64 %65, %67
  br label %bb76

bb76:                                             ; preds = %bb10, %bb6.preheader, %bb4.i59, %bb66, %bb65, %bb42, %bb46, %bb52, %bb50, %bb62, %bb15, %bb8
  %.sroa.11.2 = phi i64 [ %8, %bb8 ], [ %10, %bb15 ], [ %_13.i, %bb62 ], [ %_122, %bb52 ], [ %_116, %bb50 ], [ %_109, %bb46 ], [ %_102, %bb42 ], [ undef, %bb65 ], [ %_13.i58, %bb4.i59 ], [ undef, %bb66 ], [ undef, %bb6.preheader ], [ undef, %bb10 ]
  %.sroa.0.2 = phi i64 [ 1, %bb8 ], [ 1, %bb15 ], [ 1, %bb62 ], [ 1, %bb52 ], [ 1, %bb50 ], [ 1, %bb46 ], [ 1, %bb42 ], [ 0, %bb65 ], [ 1, %bb4.i59 ], [ 0, %bb66 ], [ 0, %bb6.preheader ], [ 0, %bb10 ]
  %68 = insertvalue { i64, i64 } undef, i64 %.sroa.0.2, 0
  %69 = insertvalue { i64, i64 } %68, i64 %.sroa.11.2, 1
  ret { i64, i64 } %69
}

; memchr::memchr::x86::sse2::memchr2
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr3x864sse27memchr217h648ef86af91e615fE(i8 %n1, i8 %n2, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %.0.vec.insert.i.i = insertelement <16 x i8> undef, i8 %n1, i32 0
  %.15.vec.insert.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %.0.vec.insert.i.i33 = insertelement <16 x i8> undef, i8 %n2, i32 0
  %.15.vec.insert.i.i34 = shufflevector <16 x i8> %.0.vec.insert.i.i33, <16 x i8> poison, <16 x i32> zeroinitializer
  %0 = icmp ult i64 %haystack.1, 32
  %.0.sroa.speculated.i.i.i = select i1 %0, i64 %haystack.1, i64 32
  %1 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_21 = icmp ult i64 %haystack.1, 16
  br i1 %_21, label %bb7.preheader, label %bb17

bb7.preheader:                                    ; preds = %start
  %_24161.not = icmp eq i64 %haystack.1, 0
  br i1 %_24161.not, label %bb71, label %bb8.preheader

bb8.preheader:                                    ; preds = %bb7.preheader
  %2 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  br label %bb8

bb17:                                             ; preds = %start
  %3 = bitcast [0 x i8]* %haystack.0 to <16 x i8>*
  %.0.copyload22.i = load <16 x i8>, <16 x i8>* %3, align 1
  %4 = icmp eq <16 x i8> %.0.copyload22.i, %.15.vec.insert.i.i
  %5 = icmp eq <16 x i8> %.0.copyload22.i, %.15.vec.insert.i.i34
  %6 = or <16 x i1> %5, %4
  %7 = bitcast <16 x i1> %6 to i16
  %8 = icmp eq i16 %7, 0
  br i1 %8, label %bb20, label %bb19

bb8:                                              ; preds = %bb8.preheader, %bb14
  %ptr.0162 = phi i8* [ %9, %bb14 ], [ %2, %bb8.preheader ]
  %_29 = load i8, i8* %ptr.0162, align 1
  %_28 = icmp eq i8 %_29, %n1
  %_31 = icmp eq i8 %_29, %n2
  %_27.0 = select i1 %_28, i1 true, i1 %_31
  br i1 %_27.0, label %bb12, label %bb14

bb14:                                             ; preds = %bb8
  %9 = getelementptr inbounds i8, i8* %ptr.0162, i64 1
  %_24 = icmp ult i8* %9, %1
  br i1 %_24, label %bb8, label %bb71

bb12:                                             ; preds = %bb8
  %_3.i = ptrtoint i8* %ptr.0162 to i64
  %_5.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %10 = sub i64 %_3.i, %_5.i
  br label %bb71

bb19:                                             ; preds = %bb17
  %11 = tail call i16 @llvm.cttz.i16(i16 %7, i1 true) #21, !range !44
  %12 = zext i16 %11 to i64
  br label %bb71

bb20:                                             ; preds = %bb17
  %_52 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_51 = and i64 %_52, 15
  %_50 = sub nuw nsw i64 16, %_51
  %13 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_50
  %_55 = icmp ugt i64 %haystack.1, 31
  %14 = getelementptr inbounds i8, i8* %1, i64 -32
  %_57150 = icmp ule i8* %13, %14
  %or.cond151 = select i1 %_55, i1 %_57150, i1 false
  br i1 %or.cond151, label %bb27, label %bb55.preheader

bb55.preheader:                                   ; preds = %bb52, %bb20
  %ptr.1.lcssa = phi i8* [ %13, %bb20 ], [ %28, %bb52 ]
  %15 = getelementptr inbounds i8, i8* %1, i64 -16
  %_123.not158 = icmp ugt i8* %ptr.1.lcssa, %15
  br i1 %_123.not158, label %bb62, label %bb57

bb27:                                             ; preds = %bb20, %bb52
  %ptr.1152 = phi i8* [ %28, %bb52 ], [ %13, %bb20 ]
  %16 = bitcast i8* %ptr.1152 to <16 x i8>*
  %_66.val137 = load <16 x i8>, <16 x i8>* %16, align 16
  %17 = getelementptr inbounds i8, i8* %ptr.1152, i64 16
  %18 = bitcast i8* %17 to <16 x i8>*
  %_69.val138 = load <16 x i8>, <16 x i8>* %18, align 16
  %19 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_66.val137
  %20 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_69.val138
  %21 = icmp eq <16 x i8> %.15.vec.insert.i.i34, %_66.val137
  %22 = icmp eq <16 x i8> %.15.vec.insert.i.i34, %_69.val138
  %23 = or <16 x i1> %19, %21
  %24 = or <16 x i1> %23, %22
  %25 = or <16 x i1> %24, %20
  %26 = bitcast <16 x i1> %25 to i16
  %27 = icmp eq i16 %26, 0
  br i1 %27, label %bb52, label %bb39

bb52:                                             ; preds = %bb27
  %28 = getelementptr inbounds i8, i8* %ptr.1152, i64 %.0.sroa.speculated.i.i.i
  %_57.not = icmp ugt i8* %28, %14
  br i1 %_57.not, label %bb55.preheader, label %bb27

bb39:                                             ; preds = %bb27
  %_3.i41 = ptrtoint i8* %ptr.1152 to i64
  %29 = sub i64 %_3.i41, %_52
  %30 = bitcast <16 x i1> %19 to i16
  %31 = bitcast <16 x i1> %21 to i16
  %32 = icmp ne i16 %30, 0
  %_104 = icmp ne i16 %31, 0
  %_102.0 = select i1 %32, i1 true, i1 %_104
  br i1 %_102.0, label %bb46, label %bb48

bb48:                                             ; preds = %bb39
  %33 = add i64 %29, 16
  %_3.i47139140 = or <16 x i1> %22, %20
  %_3.i47139 = bitcast <16 x i1> %_3.i47139140 to i16
  %_3.i47 = zext i16 %_3.i47139 to i32
  %34 = tail call i32 @llvm.cttz.i32(i32 %_3.i47, i1 false) #21, !range !45
  %35 = zext i32 %34 to i64
  %_115 = add i64 %33, %35
  br label %bb71

bb46:                                             ; preds = %bb39
  %_3.i48141 = bitcast <16 x i1> %23 to i16
  %_3.i48 = zext i16 %_3.i48141 to i32
  %36 = tail call i32 @llvm.cttz.i32(i32 %_3.i48, i1 false) #21, !range !45
  %37 = zext i32 %36 to i64
  %_106 = add i64 %29, %37
  br label %bb71

bb62:                                             ; preds = %bb60, %bb55.preheader
  %ptr.2.lcssa = phi i8* [ %ptr.1.lcssa, %bb55.preheader ], [ %47, %bb60 ]
  %_138 = icmp ult i8* %ptr.2.lcssa, %1
  br i1 %_138, label %bb63, label %bb71

bb57:                                             ; preds = %bb55.preheader, %bb60
  %ptr.2159 = phi i8* [ %47, %bb60 ], [ %ptr.1.lcssa, %bb55.preheader ]
  %38 = bitcast i8* %ptr.2159 to <16 x i8>*
  %.0.copyload22.i49 = load <16 x i8>, <16 x i8>* %38, align 1
  %39 = icmp eq <16 x i8> %.0.copyload22.i49, %.15.vec.insert.i.i
  %40 = icmp eq <16 x i8> %.0.copyload22.i49, %.15.vec.insert.i.i34
  %41 = or <16 x i1> %40, %39
  %42 = bitcast <16 x i1> %41 to i16
  %43 = icmp eq i16 %42, 0
  br i1 %43, label %bb60, label %bb59

bb59:                                             ; preds = %bb57
  %_3.i.i50 = ptrtoint i8* %ptr.2159 to i64
  %44 = sub i64 %_3.i.i50, %_52
  %45 = tail call i16 @llvm.cttz.i16(i16 %42, i1 true) #21, !range !44
  %46 = zext i16 %45 to i64
  %_23.i = add i64 %44, %46
  br label %bb71

bb60:                                             ; preds = %bb57
  %47 = getelementptr inbounds i8, i8* %ptr.2159, i64 16
  %_123.not = icmp ugt i8* %47, %15
  br i1 %_123.not, label %bb62, label %bb57

bb63:                                             ; preds = %bb62
  %_3.i56 = ptrtoint i8* %1 to i64
  %_5.i57 = ptrtoint i8* %ptr.2.lcssa to i64
  %.neg.neg = add i64 %_3.i56, -16
  %48 = sub i64 %.neg.neg, %_5.i57
  %49 = getelementptr inbounds i8, i8* %ptr.2.lcssa, i64 %48
  %50 = bitcast i8* %49 to <16 x i8>*
  %.0.copyload22.i58 = load <16 x i8>, <16 x i8>* %50, align 1
  %51 = icmp eq <16 x i8> %.0.copyload22.i58, %.15.vec.insert.i.i
  %52 = icmp eq <16 x i8> %.0.copyload22.i58, %.15.vec.insert.i.i34
  %53 = or <16 x i1> %52, %51
  %54 = bitcast <16 x i1> %53 to i16
  %55 = icmp eq i16 %54, 0
  br i1 %55, label %bb71, label %bb6.i62

bb6.i62:                                          ; preds = %bb63
  %_3.i.i59 = ptrtoint i8* %49 to i64
  %56 = sub i64 %_3.i.i59, %_52
  %57 = tail call i16 @llvm.cttz.i16(i16 %54, i1 true) #21, !range !44
  %58 = zext i16 %57 to i64
  %_23.i61 = add i64 %56, %58
  br label %bb71

bb71:                                             ; preds = %bb14, %bb7.preheader, %bb6.i62, %bb63, %bb62, %bb46, %bb48, %bb59, %bb19, %bb12
  %.sroa.9.2 = phi i64 [ %10, %bb12 ], [ %12, %bb19 ], [ %_23.i, %bb59 ], [ %_106, %bb46 ], [ %_115, %bb48 ], [ undef, %bb62 ], [ %_23.i61, %bb6.i62 ], [ undef, %bb63 ], [ undef, %bb7.preheader ], [ undef, %bb14 ]
  %.sroa.0.2 = phi i64 [ 1, %bb12 ], [ 1, %bb19 ], [ 1, %bb59 ], [ 1, %bb46 ], [ 1, %bb48 ], [ 0, %bb62 ], [ 1, %bb6.i62 ], [ 0, %bb63 ], [ 0, %bb7.preheader ], [ 0, %bb14 ]
  %59 = insertvalue { i64, i64 } undef, i64 %.sroa.0.2, 0
  %60 = insertvalue { i64, i64 } %59, i64 %.sroa.9.2, 1
  ret { i64, i64 } %60
}

; memchr::memchr::x86::sse2::memchr3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr3x864sse27memchr317h3528250da6145231E(i8 %n1, i8 %n2, i8 %n3, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %.0.vec.insert.i.i = insertelement <16 x i8> undef, i8 %n1, i32 0
  %.15.vec.insert.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %.0.vec.insert.i.i42 = insertelement <16 x i8> undef, i8 %n2, i32 0
  %.15.vec.insert.i.i43 = shufflevector <16 x i8> %.0.vec.insert.i.i42, <16 x i8> poison, <16 x i32> zeroinitializer
  %.0.vec.insert.i.i44 = insertelement <16 x i8> undef, i8 %n3, i32 0
  %.15.vec.insert.i.i45 = shufflevector <16 x i8> %.0.vec.insert.i.i44, <16 x i8> poison, <16 x i32> zeroinitializer
  %0 = icmp ult i64 %haystack.1, 32
  %.0.sroa.speculated.i.i.i = select i1 %0, i64 %haystack.1, i64 32
  %1 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_25 = icmp ult i64 %haystack.1, 16
  br i1 %_25, label %bb8.preheader, label %bb21

bb8.preheader:                                    ; preds = %start
  %_28223.not = icmp eq i64 %haystack.1, 0
  br i1 %_28223.not, label %bb84, label %bb9.preheader

bb9.preheader:                                    ; preds = %bb8.preheader
  %2 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  br label %bb9

bb21:                                             ; preds = %start
  %3 = bitcast [0 x i8]* %haystack.0 to <16 x i8>*
  %.0.copyload34.i = load <16 x i8>, <16 x i8>* %3, align 1
  %4 = icmp eq <16 x i8> %.0.copyload34.i, %.15.vec.insert.i.i
  %5 = icmp eq <16 x i8> %.0.copyload34.i, %.15.vec.insert.i.i43
  %6 = icmp eq <16 x i8> %.0.copyload34.i, %.15.vec.insert.i.i45
  %7 = or <16 x i1> %4, %6
  %8 = or <16 x i1> %7, %5
  %9 = bitcast <16 x i1> %8 to i16
  %10 = icmp eq i16 %9, 0
  br i1 %10, label %bb24, label %bb23

bb9:                                              ; preds = %bb9.preheader, %bb18
  %ptr.0224 = phi i8* [ %11, %bb18 ], [ %2, %bb9.preheader ]
  %_34 = load i8, i8* %ptr.0224, align 1
  %_33 = icmp eq i8 %_34, %n1
  %_36 = icmp eq i8 %_34, %n2
  %_32.0 = select i1 %_33, i1 true, i1 %_36
  %_39 = icmp eq i8 %_34, %n3
  %or.cond189 = select i1 %_32.0, i1 true, i1 %_39
  br i1 %or.cond189, label %bb16, label %bb18

bb18:                                             ; preds = %bb9
  %11 = getelementptr inbounds i8, i8* %ptr.0224, i64 1
  %_28 = icmp ult i8* %11, %1
  br i1 %_28, label %bb9, label %bb84

bb16:                                             ; preds = %bb9
  %_3.i = ptrtoint i8* %ptr.0224 to i64
  %_5.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %12 = sub i64 %_3.i, %_5.i
  br label %bb84

bb23:                                             ; preds = %bb21
  %13 = tail call i16 @llvm.cttz.i16(i16 %9, i1 true) #21, !range !44
  %14 = zext i16 %13 to i64
  br label %bb84

bb24:                                             ; preds = %bb21
  %_61 = ptrtoint [0 x i8]* %haystack.0 to i64
  %_60 = and i64 %_61, 15
  %_59 = sub nuw nsw i64 16, %_60
  %15 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_59
  %_64 = icmp ugt i64 %haystack.1, 31
  %16 = getelementptr inbounds i8, i8* %1, i64 -32
  %_66210 = icmp ule i8* %15, %16
  %or.cond190211 = select i1 %_64, i1 %_66210, i1 false
  br i1 %or.cond190211, label %bb31, label %bb68.preheader

bb68.preheader:                                   ; preds = %bb65, %bb24
  %ptr.1.lcssa = phi i8* [ %15, %bb24 ], [ %34, %bb65 ]
  %17 = getelementptr inbounds i8, i8* %1, i64 -16
  %_153.not220 = icmp ugt i8* %ptr.1.lcssa, %17
  br i1 %_153.not220, label %bb75, label %bb70

bb31:                                             ; preds = %bb24, %bb65
  %ptr.1212 = phi i8* [ %34, %bb65 ], [ %15, %bb24 ]
  %18 = bitcast i8* %ptr.1212 to <16 x i8>*
  %_75.val191 = load <16 x i8>, <16 x i8>* %18, align 16
  %19 = getelementptr inbounds i8, i8* %ptr.1212, i64 16
  %20 = bitcast i8* %19 to <16 x i8>*
  %_78.val192 = load <16 x i8>, <16 x i8>* %20, align 16
  %21 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_75.val191
  %22 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_78.val192
  %23 = icmp eq <16 x i8> %.15.vec.insert.i.i43, %_75.val191
  %24 = icmp eq <16 x i8> %.15.vec.insert.i.i43, %_78.val192
  %25 = icmp eq <16 x i8> %.15.vec.insert.i.i45, %_75.val191
  %26 = icmp eq <16 x i8> %.15.vec.insert.i.i45, %_78.val192
  %27 = or <16 x i1> %23, %25
  %28 = or <16 x i1> %27, %21
  %29 = or <16 x i1> %28, %26
  %30 = or <16 x i1> %29, %24
  %31 = or <16 x i1> %30, %22
  %32 = bitcast <16 x i1> %31 to i16
  %33 = icmp eq i16 %32, 0
  br i1 %33, label %bb65, label %bb47

bb65:                                             ; preds = %bb31
  %34 = getelementptr inbounds i8, i8* %ptr.1212, i64 %.0.sroa.speculated.i.i.i
  %_66.not = icmp ugt i8* %34, %16
  br i1 %_66.not, label %bb68.preheader, label %bb31

bb47:                                             ; preds = %bb31
  %_3.i56 = ptrtoint i8* %ptr.1212 to i64
  %35 = sub i64 %_3.i56, %_61
  %36 = bitcast <16 x i1> %21 to i16
  %37 = bitcast <16 x i1> %23 to i16
  %38 = bitcast <16 x i1> %25 to i16
  %39 = icmp ne i16 %36, 0
  %_128 = icmp ne i16 %37, 0
  %_126.0 = select i1 %39, i1 true, i1 %_128
  %_130 = icmp ne i16 %38, 0
  %or.cond = select i1 %_126.0, i1 true, i1 %_130
  br i1 %or.cond, label %bb58, label %bb60

bb60:                                             ; preds = %bb47
  %40 = add i64 %35, 16
  %_5.i64193194 = or <16 x i1> %22, %26
  %_4.i195196 = or <16 x i1> %_5.i64193194, %24
  %_4.i195 = bitcast <16 x i1> %_4.i195196 to i16
  %_4.i = zext i16 %_4.i195 to i32
  %41 = tail call i32 @llvm.cttz.i32(i32 %_4.i, i1 false) #21, !range !45
  %42 = zext i32 %41 to i64
  %_144 = add i64 %40, %42
  br label %bb84

bb58:                                             ; preds = %bb47
  %_5.i65197198 = or <16 x i1> %21, %25
  %_4.i66199200 = or <16 x i1> %_5.i65197198, %23
  %_4.i66199 = bitcast <16 x i1> %_4.i66199200 to i16
  %_4.i66 = zext i16 %_4.i66199 to i32
  %43 = tail call i32 @llvm.cttz.i32(i32 %_4.i66, i1 false) #21, !range !45
  %44 = zext i32 %43 to i64
  %_132 = add i64 %35, %44
  br label %bb84

bb75:                                             ; preds = %bb73, %bb68.preheader
  %ptr.2.lcssa = phi i8* [ %ptr.1.lcssa, %bb68.preheader ], [ %56, %bb73 ]
  %_169 = icmp ult i8* %ptr.2.lcssa, %1
  br i1 %_169, label %bb76, label %bb84

bb70:                                             ; preds = %bb68.preheader, %bb73
  %ptr.2221 = phi i8* [ %56, %bb73 ], [ %ptr.1.lcssa, %bb68.preheader ]
  %45 = bitcast i8* %ptr.2221 to <16 x i8>*
  %.0.copyload34.i67 = load <16 x i8>, <16 x i8>* %45, align 1
  %46 = icmp eq <16 x i8> %.0.copyload34.i67, %.15.vec.insert.i.i
  %47 = icmp eq <16 x i8> %.0.copyload34.i67, %.15.vec.insert.i.i43
  %48 = icmp eq <16 x i8> %.0.copyload34.i67, %.15.vec.insert.i.i45
  %49 = or <16 x i1> %46, %48
  %50 = or <16 x i1> %49, %47
  %51 = bitcast <16 x i1> %50 to i16
  %52 = icmp eq i16 %51, 0
  br i1 %52, label %bb73, label %bb72

bb72:                                             ; preds = %bb70
  %_3.i.i68 = ptrtoint i8* %ptr.2221 to i64
  %53 = sub i64 %_3.i.i68, %_61
  %54 = tail call i16 @llvm.cttz.i16(i16 %51, i1 true) #21, !range !44
  %55 = zext i16 %54 to i64
  %_32.i70 = add i64 %53, %55
  br label %bb84

bb73:                                             ; preds = %bb70
  %56 = getelementptr inbounds i8, i8* %ptr.2221, i64 16
  %_153.not = icmp ugt i8* %56, %17
  br i1 %_153.not, label %bb75, label %bb70

bb76:                                             ; preds = %bb75
  %_3.i75 = ptrtoint i8* %1 to i64
  %_5.i76 = ptrtoint i8* %ptr.2.lcssa to i64
  %.neg.neg = add i64 %_3.i75, -16
  %57 = sub i64 %.neg.neg, %_5.i76
  %58 = getelementptr inbounds i8, i8* %ptr.2.lcssa, i64 %57
  %59 = bitcast i8* %58 to <16 x i8>*
  %.0.copyload34.i77 = load <16 x i8>, <16 x i8>* %59, align 1
  %60 = icmp eq <16 x i8> %.0.copyload34.i77, %.15.vec.insert.i.i
  %61 = icmp eq <16 x i8> %.0.copyload34.i77, %.15.vec.insert.i.i43
  %62 = icmp eq <16 x i8> %.0.copyload34.i77, %.15.vec.insert.i.i45
  %63 = or <16 x i1> %60, %62
  %64 = or <16 x i1> %63, %61
  %65 = bitcast <16 x i1> %64 to i16
  %66 = icmp eq i16 %65, 0
  br i1 %66, label %bb84, label %bb8.i81

bb8.i81:                                          ; preds = %bb76
  %_3.i.i78 = ptrtoint i8* %58 to i64
  %67 = sub i64 %_3.i.i78, %_61
  %68 = tail call i16 @llvm.cttz.i16(i16 %65, i1 true) #21, !range !44
  %69 = zext i16 %68 to i64
  %_32.i80 = add i64 %67, %69
  br label %bb84

bb84:                                             ; preds = %bb18, %bb8.preheader, %bb8.i81, %bb76, %bb75, %bb58, %bb60, %bb72, %bb23, %bb16
  %.sroa.9.2 = phi i64 [ %12, %bb16 ], [ %14, %bb23 ], [ %_32.i70, %bb72 ], [ %_132, %bb58 ], [ %_144, %bb60 ], [ undef, %bb75 ], [ %_32.i80, %bb8.i81 ], [ undef, %bb76 ], [ undef, %bb8.preheader ], [ undef, %bb18 ]
  %.sroa.0.2 = phi i64 [ 1, %bb16 ], [ 1, %bb23 ], [ 1, %bb72 ], [ 1, %bb58 ], [ 1, %bb60 ], [ 0, %bb75 ], [ 1, %bb8.i81 ], [ 0, %bb76 ], [ 0, %bb8.preheader ], [ 0, %bb18 ]
  %70 = insertvalue { i64, i64 } undef, i64 %.sroa.0.2, 0
  %71 = insertvalue { i64, i64 } %70, i64 %.sroa.9.2, 1
  ret { i64, i64 } %71
}

; memchr::memchr::x86::sse2::memrchr
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr3x864sse27memrchr17h6b1a5fd77ea8a7e0E(i8 %n1, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %.0.vec.insert.i.i = insertelement <16 x i8> undef, i8 %n1, i32 0
  %.15.vec.insert.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %0 = icmp ult i64 %haystack.1, 64
  %.0.sroa.speculated.i.i.i = select i1 %0, i64 %haystack.1, i64 64
  %1 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %2 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_17 = icmp ult i64 %haystack.1, 16
  br i1 %_17, label %bb6, label %bb13

bb13:                                             ; preds = %start
  %3 = getelementptr inbounds i8, i8* %2, i64 -16
  %4 = bitcast i8* %3 to <16 x i8>*
  %.0.copyload9.i = load <16 x i8>, <16 x i8>* %4, align 1
  %5 = icmp eq <16 x i8> %.0.copyload9.i, %.15.vec.insert.i.i
  %6 = bitcast <16 x i1> %5 to i16
  %7 = icmp eq i16 %6, 0
  br i1 %7, label %bb17, label %bb16

bb6:                                              ; preds = %start, %bb7
  %ptr.0 = phi i8* [ %8, %bb7 ], [ %2, %start ]
  %_20 = icmp ugt i8* %ptr.0, %1
  br i1 %_20, label %bb7, label %bb75

bb7:                                              ; preds = %bb6
  %8 = getelementptr inbounds i8, i8* %ptr.0, i64 -1
  %_26 = load i8, i8* %8, align 1
  %_25 = icmp eq i8 %_26, %n1
  br i1 %_25, label %bb9, label %bb6

bb9:                                              ; preds = %bb7
  %_3.i = ptrtoint i8* %8 to i64
  %_5.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %9 = sub i64 %_3.i, %_5.i
  br label %bb75

bb16:                                             ; preds = %bb13
  %_3.i.i = ptrtoint i8* %3 to i64
  %_5.i1.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %10 = sub i64 %_3.i.i, %_5.i1.i
  %11 = tail call i16 @llvm.ctlz.i16(i16 %6, i1 true) #21, !range !44
  %12 = xor i16 %11, 15
  %13 = zext i16 %12 to i64
  %_13.i = add i64 %10, %13
  br label %bb75

bb17:                                             ; preds = %bb13
  %_42 = ptrtoint i8* %2 to i64
  %_41 = and i64 %_42, -16
  %14 = inttoptr i64 %_41 to i8*
  %_46 = icmp ugt i64 %haystack.1, 63
  %15 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 64
  %16 = sub nsw i64 0, %.0.sroa.speculated.i.i.i
  br i1 %_46, label %bb18, label %bb59.preheader

bb18:                                             ; preds = %bb17, %bb23
  %ptr.1 = phi i8* [ %18, %bb23 ], [ %14, %bb17 ]
  %_48.not = icmp ult i8* %ptr.1, %15
  br i1 %_48.not, label %bb59.preheader, label %bb23

bb59.preheader:                                   ; preds = %bb18, %bb17
  %.us-phi = phi i8* [ %14, %bb17 ], [ %ptr.1, %bb18 ]
  %17 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 16
  br label %bb59

bb23:                                             ; preds = %bb18
  %18 = getelementptr inbounds i8, i8* %ptr.1, i64 %16
  %19 = bitcast i8* %18 to <16 x i8>*
  %_60.val135 = load <16 x i8>, <16 x i8>* %19, align 16
  %20 = getelementptr inbounds i8, i8* %18, i64 16
  %21 = bitcast i8* %20 to <16 x i8>*
  %_63.val136 = load <16 x i8>, <16 x i8>* %21, align 16
  %22 = getelementptr inbounds i8, i8* %18, i64 32
  %23 = bitcast i8* %22 to <16 x i8>*
  %_67.val137 = load <16 x i8>, <16 x i8>* %23, align 16
  %24 = getelementptr inbounds i8, i8* %18, i64 48
  %25 = bitcast i8* %24 to <16 x i8>*
  %_72.val138 = load <16 x i8>, <16 x i8>* %25, align 16
  %26 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_60.val135
  %27 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_63.val136
  %28 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_67.val137
  %29 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_72.val138
  %30 = or <16 x i1> %27, %26
  %31 = or <16 x i1> %30, %28
  %32 = or <16 x i1> %31, %29
  %33 = bitcast <16 x i1> %32 to i16
  %34 = icmp eq i16 %33, 0
  br i1 %34, label %bb18, label %bb40

bb40:                                             ; preds = %bb23
  %_3.i40 = ptrtoint i8* %24 to i64
  %_5.i41 = ptrtoint [0 x i8]* %haystack.0 to i64
  %35 = sub i64 %_3.i40, %_5.i41
  %36 = bitcast <16 x i1> %29 to i16
  %37 = icmp eq i16 %36, 0
  br i1 %37, label %bb46, label %bb44

bb46:                                             ; preds = %bb40
  %38 = bitcast <16 x i1> %28 to i16
  %39 = icmp eq i16 %38, 0
  br i1 %39, label %bb50, label %bb48

bb44:                                             ; preds = %bb40
  %40 = tail call i16 @llvm.ctlz.i16(i16 %36, i1 true) #21, !range !44
  %41 = xor i16 %40, 15
  %42 = zext i16 %41 to i64
  %_107 = add i64 %35, %42
  br label %bb75

bb50:                                             ; preds = %bb46
  %43 = bitcast <16 x i1> %27 to i16
  %44 = icmp eq i16 %43, 0
  br i1 %44, label %bb54, label %bb52

bb48:                                             ; preds = %bb46
  %45 = add i64 %35, -16
  %46 = tail call i16 @llvm.ctlz.i16(i16 %38, i1 true) #21, !range !44
  %47 = xor i16 %46, 15
  %48 = zext i16 %47 to i64
  %_114 = add i64 %45, %48
  br label %bb75

bb54:                                             ; preds = %bb50
  %49 = bitcast <16 x i1> %26 to i16
  %50 = tail call i16 @llvm.ctlz.i16(i16 %49, i1 false) #21, !range !44
  %_3.i48 = zext i16 %50 to i64
  %51 = add i64 %35, -33
  %_127 = sub i64 %51, %_3.i48
  br label %bb75

bb52:                                             ; preds = %bb50
  %52 = add i64 %35, -32
  %53 = tail call i16 @llvm.ctlz.i16(i16 %43, i1 true) #21, !range !44
  %54 = xor i16 %53, 15
  %55 = zext i16 %54 to i64
  %_121 = add i64 %52, %55
  br label %bb75

bb59:                                             ; preds = %bb59.preheader, %bb61
  %ptr.2 = phi i8* [ %56, %bb61 ], [ %.us-phi, %bb59.preheader ]
  %_131.not = icmp ult i8* %ptr.2, %17
  br i1 %_131.not, label %bb66, label %bb61

bb66:                                             ; preds = %bb59
  %_145 = icmp ugt i8* %ptr.2, %1
  br i1 %_145, label %bb67, label %bb75

bb61:                                             ; preds = %bb59
  %56 = getelementptr inbounds i8, i8* %ptr.2, i64 -16
  %57 = bitcast i8* %56 to <16 x i8>*
  %.0.copyload9.i50 = load <16 x i8>, <16 x i8>* %57, align 1
  %58 = icmp eq <16 x i8> %.0.copyload9.i50, %.15.vec.insert.i.i
  %59 = bitcast <16 x i1> %58 to i16
  %60 = icmp eq i16 %59, 0
  br i1 %60, label %bb59, label %bb64

bb64:                                             ; preds = %bb61
  %_3.i.i51 = ptrtoint i8* %56 to i64
  %_5.i1.i52 = ptrtoint [0 x i8]* %haystack.0 to i64
  %61 = sub i64 %_3.i.i51, %_5.i1.i52
  %62 = tail call i16 @llvm.ctlz.i16(i16 %59, i1 true) #21, !range !44
  %63 = xor i16 %62, 15
  %64 = zext i16 %63 to i64
  %_13.i53 = add i64 %61, %64
  br label %bb75

bb67:                                             ; preds = %bb66
  %65 = bitcast [0 x i8]* %haystack.0 to <16 x i8>*
  %.0.copyload9.i58 = load <16 x i8>, <16 x i8>* %65, align 1
  %66 = icmp eq <16 x i8> %.0.copyload9.i58, %.15.vec.insert.i.i
  %67 = bitcast <16 x i1> %66 to i16
  %68 = icmp eq i16 %67, 0
  br i1 %68, label %bb75, label %bb4.i61

bb4.i61:                                          ; preds = %bb67
  %69 = tail call i16 @llvm.ctlz.i16(i16 %67, i1 true) #21, !range !44
  %70 = xor i16 %69, 15
  %71 = zext i16 %70 to i64
  br label %bb75

bb75:                                             ; preds = %bb6, %bb4.i61, %bb67, %bb66, %bb44, %bb48, %bb54, %bb52, %bb64, %bb16, %bb9
  %.sroa.11.2 = phi i64 [ %9, %bb9 ], [ %_13.i, %bb16 ], [ %_13.i53, %bb64 ], [ %_127, %bb54 ], [ %_121, %bb52 ], [ %_114, %bb48 ], [ %_107, %bb44 ], [ undef, %bb66 ], [ %71, %bb4.i61 ], [ undef, %bb67 ], [ undef, %bb6 ]
  %.sroa.0.2 = phi i64 [ 1, %bb9 ], [ 1, %bb16 ], [ 1, %bb64 ], [ 1, %bb54 ], [ 1, %bb52 ], [ 1, %bb48 ], [ 1, %bb44 ], [ 0, %bb66 ], [ 1, %bb4.i61 ], [ 0, %bb67 ], [ 0, %bb6 ]
  %72 = insertvalue { i64, i64 } undef, i64 %.sroa.0.2, 0
  %73 = insertvalue { i64, i64 } %72, i64 %.sroa.11.2, 1
  ret { i64, i64 } %73
}

; memchr::memchr::x86::sse2::memrchr2
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr3x864sse28memrchr217h69765c7d233ef91bE(i8 %n1, i8 %n2, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %.0.vec.insert.i.i = insertelement <16 x i8> undef, i8 %n1, i32 0
  %.15.vec.insert.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %.0.vec.insert.i.i33 = insertelement <16 x i8> undef, i8 %n2, i32 0
  %.15.vec.insert.i.i34 = shufflevector <16 x i8> %.0.vec.insert.i.i33, <16 x i8> poison, <16 x i32> zeroinitializer
  %0 = icmp ult i64 %haystack.1, 32
  %.0.sroa.speculated.i.i.i = select i1 %0, i64 %haystack.1, i64 32
  %1 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %2 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_21 = icmp ult i64 %haystack.1, 16
  br i1 %_21, label %bb7, label %bb17

bb17:                                             ; preds = %start
  %3 = getelementptr inbounds i8, i8* %2, i64 -16
  %4 = bitcast i8* %3 to <16 x i8>*
  %.0.copyload22.i = load <16 x i8>, <16 x i8>* %4, align 1
  %5 = icmp eq <16 x i8> %.0.copyload22.i, %.15.vec.insert.i.i
  %6 = icmp eq <16 x i8> %.0.copyload22.i, %.15.vec.insert.i.i34
  %7 = or <16 x i1> %6, %5
  %8 = bitcast <16 x i1> %7 to i16
  %9 = icmp eq i16 %8, 0
  br i1 %9, label %bb21, label %bb20

bb7:                                              ; preds = %start, %bb8
  %ptr.0 = phi i8* [ %10, %bb8 ], [ %2, %start ]
  %_24 = icmp ugt i8* %ptr.0, %1
  br i1 %_24, label %bb8, label %bb70

bb8:                                              ; preds = %bb7
  %10 = getelementptr inbounds i8, i8* %ptr.0, i64 -1
  %_31 = load i8, i8* %10, align 1
  %_30 = icmp eq i8 %_31, %n1
  %_33 = icmp eq i8 %_31, %n2
  %_29.0 = select i1 %_30, i1 true, i1 %_33
  br i1 %_29.0, label %bb13, label %bb7

bb13:                                             ; preds = %bb8
  %_3.i = ptrtoint i8* %10 to i64
  %_5.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %11 = sub i64 %_3.i, %_5.i
  br label %bb70

bb20:                                             ; preds = %bb17
  %_3.i.i = ptrtoint i8* %3 to i64
  %_5.i6.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %12 = tail call i16 @llvm.ctlz.i16(i16 %8, i1 true) #21, !range !44
  %_3.i.i.i = zext i16 %12 to i64
  %13 = add i64 %_3.i.i, 15
  %14 = add i64 %_5.i6.i, %_3.i.i.i
  %_23.i = sub i64 %13, %14
  br label %bb70

bb21:                                             ; preds = %bb17
  %_51 = ptrtoint i8* %2 to i64
  %_50 = and i64 %_51, -16
  %15 = inttoptr i64 %_50 to i8*
  %_55 = icmp ugt i64 %haystack.1, 31
  %16 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 32
  %17 = sub nsw i64 0, %.0.sroa.speculated.i.i.i
  br i1 %_55, label %bb22, label %bb56.preheader

bb22:                                             ; preds = %bb21, %bb27
  %ptr.1 = phi i8* [ %19, %bb27 ], [ %15, %bb21 ]
  %_57.not = icmp ult i8* %ptr.1, %16
  br i1 %_57.not, label %bb56.preheader, label %bb27

bb56.preheader:                                   ; preds = %bb22, %bb21
  %.us-phi = phi i8* [ %15, %bb21 ], [ %ptr.1, %bb22 ]
  %18 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 16
  br label %bb56

bb27:                                             ; preds = %bb22
  %19 = getelementptr inbounds i8, i8* %ptr.1, i64 %17
  %20 = bitcast i8* %19 to <16 x i8>*
  %_69.val141 = load <16 x i8>, <16 x i8>* %20, align 16
  %21 = getelementptr inbounds i8, i8* %19, i64 16
  %22 = bitcast i8* %21 to <16 x i8>*
  %_72.val142 = load <16 x i8>, <16 x i8>* %22, align 16
  %23 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_69.val141
  %24 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_72.val142
  %25 = icmp eq <16 x i8> %.15.vec.insert.i.i34, %_69.val141
  %26 = icmp eq <16 x i8> %.15.vec.insert.i.i34, %_72.val142
  %27 = or <16 x i1> %23, %25
  %28 = or <16 x i1> %27, %26
  %29 = or <16 x i1> %28, %24
  %30 = bitcast <16 x i1> %29 to i16
  %31 = icmp eq i16 %30, 0
  br i1 %31, label %bb22, label %bb40

bb40:                                             ; preds = %bb27
  %_3.i41 = ptrtoint i8* %21 to i64
  %_5.i42 = ptrtoint [0 x i8]* %haystack.0 to i64
  %32 = sub i64 %_3.i41, %_5.i42
  %33 = bitcast <16 x i1> %24 to i16
  %34 = bitcast <16 x i1> %26 to i16
  %35 = icmp ne i16 %33, 0
  %_108 = icmp ne i16 %34, 0
  %_106.0 = select i1 %35, i1 true, i1 %_108
  br i1 %_106.0, label %bb48, label %bb50

bb50:                                             ; preds = %bb40
  %_3.i47 = bitcast <16 x i1> %27 to i16
  %36 = tail call i16 @llvm.ctlz.i16(i16 %_3.i47, i1 false) #21, !range !44
  %_3.i.i48 = zext i16 %36 to i64
  %37 = xor i64 %_3.i.i48, -1
  %_119 = add i64 %32, %37
  br label %bb70

bb48:                                             ; preds = %bb40
  %_3.i49144 = or <16 x i1> %26, %24
  %_3.i49 = bitcast <16 x i1> %_3.i49144 to i16
  %38 = tail call i16 @llvm.ctlz.i16(i16 %_3.i49, i1 false) #21, !range !44
  %_3.i.i51 = zext i16 %38 to i64
  %39 = add i64 %32, 15
  %_110 = sub i64 %39, %_3.i.i51
  br label %bb70

bb56:                                             ; preds = %bb56.preheader, %bb58
  %ptr.2 = phi i8* [ %40, %bb58 ], [ %.us-phi, %bb56.preheader ]
  %_124.not = icmp ult i8* %ptr.2, %18
  br i1 %_124.not, label %bb63, label %bb58

bb63:                                             ; preds = %bb56
  %_139 = icmp ugt i8* %ptr.2, %1
  br i1 %_139, label %bb64, label %bb70

bb58:                                             ; preds = %bb56
  %40 = getelementptr inbounds i8, i8* %ptr.2, i64 -16
  %41 = bitcast i8* %40 to <16 x i8>*
  %.0.copyload22.i52 = load <16 x i8>, <16 x i8>* %41, align 1
  %42 = icmp eq <16 x i8> %.0.copyload22.i52, %.15.vec.insert.i.i
  %43 = icmp eq <16 x i8> %.0.copyload22.i52, %.15.vec.insert.i.i34
  %44 = or <16 x i1> %43, %42
  %45 = bitcast <16 x i1> %44 to i16
  %46 = icmp eq i16 %45, 0
  br i1 %46, label %bb56, label %bb61

bb61:                                             ; preds = %bb58
  %_3.i.i53 = ptrtoint i8* %40 to i64
  %_5.i6.i54 = ptrtoint [0 x i8]* %haystack.0 to i64
  %47 = tail call i16 @llvm.ctlz.i16(i16 %45, i1 true) #21, !range !44
  %_3.i.i.i55 = zext i16 %47 to i64
  %48 = add i64 %_3.i.i53, 15
  %49 = add i64 %_5.i6.i54, %_3.i.i.i55
  %_23.i56 = sub i64 %48, %49
  br label %bb70

bb64:                                             ; preds = %bb63
  %50 = bitcast [0 x i8]* %haystack.0 to <16 x i8>*
  %.0.copyload22.i61 = load <16 x i8>, <16 x i8>* %50, align 1
  %51 = icmp eq <16 x i8> %.0.copyload22.i61, %.15.vec.insert.i.i
  %52 = icmp eq <16 x i8> %.0.copyload22.i61, %.15.vec.insert.i.i34
  %53 = or <16 x i1> %52, %51
  %54 = bitcast <16 x i1> %53 to i16
  %55 = icmp eq i16 %54, 0
  br i1 %55, label %bb70, label %bb6.i66

bb6.i66:                                          ; preds = %bb64
  %56 = tail call i16 @llvm.ctlz.i16(i16 %54, i1 true) #21, !range !44
  %57 = xor i16 %56, 15
  %_23.i65 = zext i16 %57 to i64
  br label %bb70

bb70:                                             ; preds = %bb7, %bb6.i66, %bb64, %bb63, %bb48, %bb50, %bb61, %bb20, %bb13
  %.sroa.9.2 = phi i64 [ %11, %bb13 ], [ %_23.i, %bb20 ], [ %_23.i56, %bb61 ], [ %_110, %bb48 ], [ %_119, %bb50 ], [ undef, %bb63 ], [ %_23.i65, %bb6.i66 ], [ undef, %bb64 ], [ undef, %bb7 ]
  %.sroa.0.2 = phi i64 [ 1, %bb13 ], [ 1, %bb20 ], [ 1, %bb61 ], [ 1, %bb48 ], [ 1, %bb50 ], [ 0, %bb63 ], [ 1, %bb6.i66 ], [ 0, %bb64 ], [ 0, %bb7 ]
  %58 = insertvalue { i64, i64 } undef, i64 %.sroa.0.2, 0
  %59 = insertvalue { i64, i64 } %58, i64 %.sroa.9.2, 1
  ret { i64, i64 } %59
}

; memchr::memchr::x86::sse2::memrchr3
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memchr3x864sse28memrchr317h81e93cc04a7b04b5E(i8 %n1, i8 %n2, i8 %n3, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #2 personality i32 (...)* @rust_eh_personality {
start:
  %.0.vec.insert.i.i = insertelement <16 x i8> undef, i8 %n1, i32 0
  %.15.vec.insert.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %.0.vec.insert.i.i42 = insertelement <16 x i8> undef, i8 %n2, i32 0
  %.15.vec.insert.i.i43 = shufflevector <16 x i8> %.0.vec.insert.i.i42, <16 x i8> poison, <16 x i32> zeroinitializer
  %.0.vec.insert.i.i44 = insertelement <16 x i8> undef, i8 %n3, i32 0
  %.15.vec.insert.i.i45 = shufflevector <16 x i8> %.0.vec.insert.i.i44, <16 x i8> poison, <16 x i32> zeroinitializer
  %0 = icmp ult i64 %haystack.1, 32
  %.0.sroa.speculated.i.i.i = select i1 %0, i64 %haystack.1, i64 32
  %1 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %2 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_25 = icmp ult i64 %haystack.1, 16
  br i1 %_25, label %bb8, label %bb21

bb21:                                             ; preds = %start
  %3 = getelementptr inbounds i8, i8* %2, i64 -16
  %4 = bitcast i8* %3 to <16 x i8>*
  %.0.copyload34.i = load <16 x i8>, <16 x i8>* %4, align 1
  %5 = icmp eq <16 x i8> %.0.copyload34.i, %.15.vec.insert.i.i
  %6 = icmp eq <16 x i8> %.0.copyload34.i, %.15.vec.insert.i.i43
  %7 = icmp eq <16 x i8> %.0.copyload34.i, %.15.vec.insert.i.i45
  %8 = or <16 x i1> %5, %7
  %9 = or <16 x i1> %8, %6
  %10 = bitcast <16 x i1> %9 to i16
  %11 = icmp eq i16 %10, 0
  br i1 %11, label %bb25, label %bb24

bb8:                                              ; preds = %start, %bb9
  %ptr.0 = phi i8* [ %12, %bb9 ], [ %2, %start ]
  %_28 = icmp ugt i8* %ptr.0, %1
  br i1 %_28, label %bb9, label %bb83

bb9:                                              ; preds = %bb8
  %12 = getelementptr inbounds i8, i8* %ptr.0, i64 -1
  %_36 = load i8, i8* %12, align 1
  %_35 = icmp eq i8 %_36, %n1
  %_38 = icmp eq i8 %_36, %n2
  %_34.0 = select i1 %_35, i1 true, i1 %_38
  %_41 = icmp eq i8 %_36, %n3
  %or.cond193 = select i1 %_34.0, i1 true, i1 %_41
  br i1 %or.cond193, label %bb17, label %bb8

bb17:                                             ; preds = %bb9
  %_3.i = ptrtoint i8* %12 to i64
  %_5.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %13 = sub i64 %_3.i, %_5.i
  br label %bb83

bb24:                                             ; preds = %bb21
  %_3.i.i = ptrtoint i8* %3 to i64
  %_5.i11.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %14 = tail call i16 @llvm.ctlz.i16(i16 %10, i1 true) #21, !range !44
  %_3.i.i.i = zext i16 %14 to i64
  %15 = add i64 %_3.i.i, 15
  %16 = add i64 %_5.i11.i, %_3.i.i.i
  %_32.i = sub i64 %15, %16
  br label %bb83

bb25:                                             ; preds = %bb21
  %_60 = ptrtoint i8* %2 to i64
  %_59 = and i64 %_60, -16
  %17 = inttoptr i64 %_59 to i8*
  %_64 = icmp ugt i64 %haystack.1, 31
  %18 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 32
  %19 = sub nsw i64 0, %.0.sroa.speculated.i.i.i
  br i1 %_64, label %bb26, label %bb69.preheader

bb26:                                             ; preds = %bb25, %bb31
  %ptr.1 = phi i8* [ %21, %bb31 ], [ %17, %bb25 ]
  %_66.not = icmp ult i8* %ptr.1, %18
  br i1 %_66.not, label %bb69.preheader, label %bb31

bb69.preheader:                                   ; preds = %bb26, %bb25
  %.us-phi = phi i8* [ %17, %bb25 ], [ %ptr.1, %bb26 ]
  %20 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 16
  br label %bb69

bb31:                                             ; preds = %bb26
  %21 = getelementptr inbounds i8, i8* %ptr.1, i64 %19
  %22 = bitcast i8* %21 to <16 x i8>*
  %_78.val195 = load <16 x i8>, <16 x i8>* %22, align 16
  %23 = getelementptr inbounds i8, i8* %21, i64 16
  %24 = bitcast i8* %23 to <16 x i8>*
  %_81.val196 = load <16 x i8>, <16 x i8>* %24, align 16
  %25 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_78.val195
  %26 = icmp eq <16 x i8> %.15.vec.insert.i.i, %_81.val196
  %27 = icmp eq <16 x i8> %.15.vec.insert.i.i43, %_78.val195
  %28 = icmp eq <16 x i8> %.15.vec.insert.i.i43, %_81.val196
  %29 = icmp eq <16 x i8> %.15.vec.insert.i.i45, %_78.val195
  %30 = icmp eq <16 x i8> %.15.vec.insert.i.i45, %_81.val196
  %31 = or <16 x i1> %27, %29
  %32 = or <16 x i1> %31, %25
  %33 = or <16 x i1> %32, %30
  %34 = or <16 x i1> %33, %28
  %35 = or <16 x i1> %34, %26
  %36 = bitcast <16 x i1> %35 to i16
  %37 = icmp eq i16 %36, 0
  br i1 %37, label %bb26, label %bb48

bb48:                                             ; preds = %bb31
  %_3.i57 = ptrtoint i8* %23 to i64
  %_5.i58 = ptrtoint [0 x i8]* %haystack.0 to i64
  %38 = sub i64 %_3.i57, %_5.i58
  %39 = bitcast <16 x i1> %26 to i16
  %40 = bitcast <16 x i1> %28 to i16
  %41 = bitcast <16 x i1> %30 to i16
  %42 = icmp ne i16 %39, 0
  %_132 = icmp ne i16 %40, 0
  %_130.0 = select i1 %42, i1 true, i1 %_132
  %_134 = icmp ne i16 %41, 0
  %or.cond = select i1 %_130.0, i1 true, i1 %_134
  br i1 %or.cond, label %bb60, label %bb62

bb62:                                             ; preds = %bb48
  %_5.i65197 = or <16 x i1> %25, %29
  %_4.i198 = or <16 x i1> %_5.i65197, %27
  %_4.i = bitcast <16 x i1> %_4.i198 to i16
  %43 = tail call i16 @llvm.ctlz.i16(i16 %_4.i, i1 false) #21, !range !44
  %_3.i.i66 = zext i16 %43 to i64
  %44 = xor i64 %_3.i.i66, -1
  %_148 = add i64 %38, %44
  br label %bb83

bb60:                                             ; preds = %bb48
  %_5.i67199 = or <16 x i1> %26, %30
  %_4.i68200 = or <16 x i1> %_5.i67199, %28
  %_4.i68 = bitcast <16 x i1> %_4.i68200 to i16
  %45 = tail call i16 @llvm.ctlz.i16(i16 %_4.i68, i1 false) #21, !range !44
  %_3.i.i70 = zext i16 %45 to i64
  %46 = add i64 %38, 15
  %_136 = sub i64 %46, %_3.i.i70
  br label %bb83

bb69:                                             ; preds = %bb69.preheader, %bb71
  %ptr.2 = phi i8* [ %47, %bb71 ], [ %.us-phi, %bb69.preheader ]
  %_154.not = icmp ult i8* %ptr.2, %20
  br i1 %_154.not, label %bb76, label %bb71

bb76:                                             ; preds = %bb69
  %_170 = icmp ugt i8* %ptr.2, %1
  br i1 %_170, label %bb77, label %bb83

bb71:                                             ; preds = %bb69
  %47 = getelementptr inbounds i8, i8* %ptr.2, i64 -16
  %48 = bitcast i8* %47 to <16 x i8>*
  %.0.copyload34.i71 = load <16 x i8>, <16 x i8>* %48, align 1
  %49 = icmp eq <16 x i8> %.0.copyload34.i71, %.15.vec.insert.i.i
  %50 = icmp eq <16 x i8> %.0.copyload34.i71, %.15.vec.insert.i.i43
  %51 = icmp eq <16 x i8> %.0.copyload34.i71, %.15.vec.insert.i.i45
  %52 = or <16 x i1> %49, %51
  %53 = or <16 x i1> %52, %50
  %54 = bitcast <16 x i1> %53 to i16
  %55 = icmp eq i16 %54, 0
  br i1 %55, label %bb69, label %bb74

bb74:                                             ; preds = %bb71
  %_3.i.i72 = ptrtoint i8* %47 to i64
  %_5.i11.i73 = ptrtoint [0 x i8]* %haystack.0 to i64
  %56 = tail call i16 @llvm.ctlz.i16(i16 %54, i1 true) #21, !range !44
  %_3.i.i.i74 = zext i16 %56 to i64
  %57 = add i64 %_3.i.i72, 15
  %58 = add i64 %_5.i11.i73, %_3.i.i.i74
  %_32.i75 = sub i64 %57, %58
  br label %bb83

bb77:                                             ; preds = %bb76
  %59 = bitcast [0 x i8]* %haystack.0 to <16 x i8>*
  %.0.copyload34.i80 = load <16 x i8>, <16 x i8>* %59, align 1
  %60 = icmp eq <16 x i8> %.0.copyload34.i80, %.15.vec.insert.i.i
  %61 = icmp eq <16 x i8> %.0.copyload34.i80, %.15.vec.insert.i.i43
  %62 = icmp eq <16 x i8> %.0.copyload34.i80, %.15.vec.insert.i.i45
  %63 = or <16 x i1> %60, %62
  %64 = or <16 x i1> %63, %61
  %65 = bitcast <16 x i1> %64 to i16
  %66 = icmp eq i16 %65, 0
  br i1 %66, label %bb83, label %bb8.i85

bb8.i85:                                          ; preds = %bb77
  %67 = tail call i16 @llvm.ctlz.i16(i16 %65, i1 true) #21, !range !44
  %68 = xor i16 %67, 15
  %_32.i84 = zext i16 %68 to i64
  br label %bb83

bb83:                                             ; preds = %bb8, %bb8.i85, %bb77, %bb76, %bb60, %bb62, %bb74, %bb24, %bb17
  %.sroa.9.2 = phi i64 [ %13, %bb17 ], [ %_32.i, %bb24 ], [ %_32.i75, %bb74 ], [ %_136, %bb60 ], [ %_148, %bb62 ], [ undef, %bb76 ], [ %_32.i84, %bb8.i85 ], [ undef, %bb77 ], [ undef, %bb8 ]
  %.sroa.0.2 = phi i64 [ 1, %bb17 ], [ 1, %bb24 ], [ 1, %bb74 ], [ 1, %bb60 ], [ 1, %bb62 ], [ 0, %bb76 ], [ 1, %bb8.i85 ], [ 0, %bb77 ], [ 0, %bb8 ]
  %69 = insertvalue { i64, i64 } undef, i64 %.sroa.0.2, 0
  %70 = insertvalue { i64, i64 } %69, i64 %.sroa.9.2, 1
  ret { i64, i64 } %70
}

; memchr::memmem::genericsimd::matched
; Function Attrs: cold mustprogress nofree noinline norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define internal fastcc i64 @_ZN6memchr6memmem11genericsimd7matched17hf7507d2589d36674E(i8* %start_ptr, i8* %ptr, i64 %chunki) unnamed_addr #3 {
start:
  %_3.i = ptrtoint i8* %ptr to i64
  %_5.i = ptrtoint i8* %start_ptr to i64
  %0 = sub i64 %_3.i, %_5.i
  %1 = add i64 %0, %chunki
  ret i64 %1
}

; memchr::memmem::prefilter::genericsimd::matched
; Function Attrs: cold mustprogress nofree noinline nosync nounwind nonlazybind uwtable willreturn
define internal fastcc i64 @_ZN6memchr6memmem9prefilter11genericsimd7matched17h7145dd3b02952477E({ i32, i32 }* noalias nocapture align 4 dereferenceable(8) %prestate, i8* %start_ptr, i8* %ptr, i64 %chunki) unnamed_addr #4 {
start:
  %_3.i = ptrtoint i8* %ptr to i64
  %_5.i = ptrtoint i8* %start_ptr to i64
  %0 = sub i64 %_3.i, %_5.i
  %found = add i64 %0, %chunki
  %1 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %prestate, i64 0, i32 0
  %_4.i = load i32, i32* %1, align 4, !alias.scope !46
  %2 = tail call i32 @llvm.uadd.sat.i32(i32 %_4.i, i32 1) #21
  store i32 %2, i32* %1, align 4, !alias.scope !46
  %_5.i1 = icmp ugt i64 %found, 4294967295
  %3 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %prestate, i64 0, i32 1
  %_9.i = load i32, i32* %3, align 4, !alias.scope !46
  %_10.i = trunc i64 %found to i32
  %4 = tail call i32 @llvm.uadd.sat.i32(i32 %_9.i, i32 %_10.i) #21
  %.sink.i = select i1 %_5.i1, i32 -1, i32 %4
  store i32 %.sink.i, i32* %3, align 4, !alias.scope !46
  ret i64 %found
}

; memchr::memmem::prefilter::x86::sse::find
; Function Attrs: nonlazybind uwtable
define internal { i64, i64 } @_ZN6memchr6memmem9prefilter3x863sse4find17ha2a5ffc317089b73E({ i32, i32 }* noalias nocapture align 4 dereferenceable(8) %prestate, %"memmem::NeedleInfo"* noalias nocapture readonly align 4 dereferenceable(12) %ninfo, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1, [0 x i8]* noalias nocapture nonnull readonly align 1 %needle.0, i64 %needle.1) unnamed_addr #5 personality i32 (...)* @rust_eh_personality {
start:
  tail call void @llvm.experimental.noalias.scope.decl(metadata !49)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !52)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !54)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !56)
  %_7.i = icmp ult i64 %needle.1, 2
  br i1 %_7.i, label %bb1.i, label %bb2.i

bb2.i:                                            ; preds = %start
  %_13.idx.i = getelementptr inbounds %"memmem::NeedleInfo", %"memmem::NeedleInfo"* %ninfo, i64 0, i32 1, i32 0
  %_13.idx.val.i = load i8, i8* %_13.idx.i, align 4, !alias.scope !52, !noalias !58
  %_13.idx1.i = getelementptr %"memmem::NeedleInfo", %"memmem::NeedleInfo"* %ninfo, i64 0, i32 1, i32 1
  %_13.idx1.val.i = load i8, i8* %_13.idx1.i, align 1, !alias.scope !52, !noalias !58
  %_2.not.i.i.i = icmp ugt i8 %_13.idx.val.i, %_13.idx1.val.i
  %_3._4.i.i.i = select i1 %_2.not.i.i.i, i8 %_13.idx.val.i, i8 %_13.idx1.val.i
  %_4._3.i.i.i = select i1 %_2.not.i.i.i, i8 %_13.idx1.val.i, i8 %_13.idx.val.i
  %_6.i.i = zext i8 %_4._3.i.i.i to i64
  %_8.i.i = zext i8 %_3._4.i.i.i to i64
  %min_haystack_len.i = add nuw nsw i64 %_8.i.i, 16
  %_17.i = icmp ugt i64 %min_haystack_len.i, %haystack.1
  br i1 %_17.i, label %bb5.i, label %bb7.i

bb1.i:                                            ; preds = %start
; call core::panicking::panic
  tail call void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [31 x i8] }>* @alloc1054 to [0 x i8]*), i64 31, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1056 to %"core::panic::location::Location"*)) #22, !noalias !59
  unreachable

bb7.i:                                            ; preds = %bb2.i
  %0 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %1 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %2 = sub nuw nsw i64 -16, %_8.i.i
  %3 = getelementptr inbounds i8, i8* %1, i64 %2
  %_40.i = icmp ult i64 %_6.i.i, %needle.1
  br i1 %_40.i, label %bb11.i, label %panic.i, !prof !60

bb5.i:                                            ; preds = %bb2.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !61)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !64)
  %_12.i.i = icmp ult i64 %_6.i.i, %needle.1
  br i1 %_12.i.i, label %bb2.i.i, label %panic.i.i, !prof !60

bb2.i.i:                                          ; preds = %bb5.i
  %4 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_6.i.i
  %_9.i.i = load i8, i8* %4, align 1, !alias.scope !66, !noalias !67
  %5 = icmp eq i64 %haystack.1, 0
  br i1 %5, label %_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE.exit, label %bb3.i.i.i

bb3.i.i.i:                                        ; preds = %bb2.i.i
  %.0.vec.insert.i.i.i.i.i.i.i.i = insertelement <16 x i8> undef, i8 %_9.i.i, i32 0
  %.15.vec.insert.i.i.i.i.i.i.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i.i.i.i.i.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %6 = icmp ult i64 %haystack.1, 64
  %.0.sroa.speculated.i.i.i.i.i.i.i.i.i = select i1 %6, i64 %haystack.1, i64 64
  %7 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_17.i.i.i.i.i.i = icmp ult i64 %haystack.1, 16
  br i1 %_17.i.i.i.i.i.i, label %bb7.preheader.i.i.i.i.i.i, label %bb13.i.i.i.i.i.i

bb7.preheader.i.i.i.i.i.i:                        ; preds = %bb3.i.i.i
  %8 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  br label %bb7.i.i.i.i.i.i

bb13.i.i.i.i.i.i:                                 ; preds = %bb3.i.i.i
  %9 = bitcast [0 x i8]* %haystack.0 to <16 x i8>*
  %.0.copyload9.i.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %9, align 1, !alias.scope !69, !noalias !78
  %10 = icmp eq <16 x i8> %.0.copyload9.i.i.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i.i
  %11 = bitcast <16 x i1> %10 to i16
  %12 = icmp eq i16 %11, 0
  br i1 %12, label %bb16.i.i.i.i.i.i, label %bb15.i.i.i.i.i.i

bb7.i.i.i.i.i.i:                                  ; preds = %bb10.i.i.i.i.i.i, %bb7.preheader.i.i.i.i.i.i
  %ptr.0156.i.i.i.i.i.i = phi i8* [ %13, %bb10.i.i.i.i.i.i ], [ %8, %bb7.preheader.i.i.i.i.i.i ]
  %_24.i.i.i.i.i.i = load i8, i8* %ptr.0156.i.i.i.i.i.i, align 1, !alias.scope !69, !noalias !78
  %_23.i.i.i.i.i.i = icmp eq i8 %_24.i.i.i.i.i.i, %_9.i.i
  br i1 %_23.i.i.i.i.i.i, label %bb8.i.i.i.i.i.i, label %bb10.i.i.i.i.i.i

bb10.i.i.i.i.i.i:                                 ; preds = %bb7.i.i.i.i.i.i
  %13 = getelementptr inbounds i8, i8* %ptr.0156.i.i.i.i.i.i, i64 1
  %_20.i.i.i.i.i.i = icmp ult i8* %13, %7
  br i1 %_20.i.i.i.i.i.i, label %bb7.i.i.i.i.i.i, label %_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE.exit

bb8.i.i.i.i.i.i:                                  ; preds = %bb7.i.i.i.i.i.i
  %_3.i.i.i.i.i.i.i = ptrtoint i8* %ptr.0156.i.i.i.i.i.i to i64
  %_5.i.i.i.i.i.i.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %14 = sub i64 %_3.i.i.i.i.i.i.i, %_5.i.i.i.i.i.i.i
  br label %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i

bb15.i.i.i.i.i.i:                                 ; preds = %bb13.i.i.i.i.i.i
  %15 = tail call i16 @llvm.cttz.i16(i16 %11, i1 true) #21, !range !44
  %16 = zext i16 %15 to i64
  br label %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i

bb16.i.i.i.i.i.i:                                 ; preds = %bb13.i.i.i.i.i.i
  %_43.i.i.i.i.i.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %_42.i.i.i.i.i.i = and i64 %_43.i.i.i.i.i.i, 15
  %_41.i.i.i.i.i.i = sub nuw nsw i64 16, %_42.i.i.i.i.i.i
  %17 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_41.i.i.i.i.i.i
  %_46.i.i.i.i.i.i = icmp ugt i64 %haystack.1, 63
  %18 = getelementptr inbounds i8, i8* %7, i64 -64
  %_48144.i.i.i.i.i.i = icmp ule i8* %17, %18
  %or.cond145.i.i.i.i.i.i = select i1 %_46.i.i.i.i.i.i, i1 %_48144.i.i.i.i.i.i, i1 false
  br i1 %or.cond145.i.i.i.i.i.i, label %bb23.i.i.i.i.i.i, label %bb58.preheader.i.i.i.i.i.i

bb58.preheader.i.i.i.i.i.i:                       ; preds = %bb55.i.i.i.i.i.i, %bb16.i.i.i.i.i.i
  %ptr.1.lcssa.i.i.i.i.i.i = phi i8* [ %17, %bb16.i.i.i.i.i.i ], [ %36, %bb55.i.i.i.i.i.i ]
  %19 = getelementptr inbounds i8, i8* %7, i64 -16
  %_129.not152.i.i.i.i.i.i = icmp ugt i8* %ptr.1.lcssa.i.i.i.i.i.i, %19
  br i1 %_129.not152.i.i.i.i.i.i, label %bb65.i.i.i.i.i.i, label %bb60.i.i.i.i.i.i

bb23.i.i.i.i.i.i:                                 ; preds = %bb16.i.i.i.i.i.i, %bb55.i.i.i.i.i.i
  %ptr.1146.i.i.i.i.i.i = phi i8* [ %36, %bb55.i.i.i.i.i.i ], [ %17, %bb16.i.i.i.i.i.i ]
  %20 = bitcast i8* %ptr.1146.i.i.i.i.i.i to <16 x i8>*
  %_57.val133.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %20, align 16, !alias.scope !69, !noalias !78
  %21 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i.i, i64 16
  %22 = bitcast i8* %21 to <16 x i8>*
  %_60.val134.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %22, align 16, !alias.scope !69, !noalias !78
  %23 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i.i, i64 32
  %24 = bitcast i8* %23 to <16 x i8>*
  %_64.val135.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %24, align 16, !alias.scope !69, !noalias !78
  %25 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i.i, i64 48
  %26 = bitcast i8* %25 to <16 x i8>*
  %_69.val136.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %26, align 16, !alias.scope !69, !noalias !78
  %27 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i.i.i, %_57.val133.i.i.i.i.i.i
  %28 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i.i.i, %_60.val134.i.i.i.i.i.i
  %29 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i.i.i, %_64.val135.i.i.i.i.i.i
  %30 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i.i.i, %_69.val136.i.i.i.i.i.i
  %31 = or <16 x i1> %28, %27
  %32 = or <16 x i1> %31, %29
  %33 = or <16 x i1> %32, %30
  %34 = bitcast <16 x i1> %33 to i16
  %35 = icmp eq i16 %34, 0
  br i1 %35, label %bb55.i.i.i.i.i.i, label %bb39.i.i.i.i.i.i

bb55.i.i.i.i.i.i:                                 ; preds = %bb23.i.i.i.i.i.i
  %36 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i.i, i64 %.0.sroa.speculated.i.i.i.i.i.i.i.i.i
  %_48.not.i.i.i.i.i.i = icmp ugt i8* %36, %18
  br i1 %_48.not.i.i.i.i.i.i, label %bb58.preheader.i.i.i.i.i.i, label %bb23.i.i.i.i.i.i

bb39.i.i.i.i.i.i:                                 ; preds = %bb23.i.i.i.i.i.i
  %_3.i40.i.i.i.i.i.i = ptrtoint i8* %ptr.1146.i.i.i.i.i.i to i64
  %37 = sub i64 %_3.i40.i.i.i.i.i.i, %_43.i.i.i.i.i.i
  %38 = bitcast <16 x i1> %27 to i16
  %39 = icmp eq i16 %38, 0
  br i1 %39, label %bb44.i.i.i.i.i.i, label %bb42.i.i.i.i.i.i

bb44.i.i.i.i.i.i:                                 ; preds = %bb39.i.i.i.i.i.i
  %40 = bitcast <16 x i1> %28 to i16
  %41 = icmp eq i16 %40, 0
  br i1 %41, label %bb48.i.i.i.i.i.i, label %bb46.i.i.i.i.i.i

bb42.i.i.i.i.i.i:                                 ; preds = %bb39.i.i.i.i.i.i
  %42 = tail call i16 @llvm.cttz.i16(i16 %38, i1 true) #21, !range !44
  %43 = zext i16 %42 to i64
  %_102.i.i.i.i.i.i = add i64 %37, %43
  br label %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i

bb48.i.i.i.i.i.i:                                 ; preds = %bb44.i.i.i.i.i.i
  %44 = bitcast <16 x i1> %29 to i16
  %45 = icmp eq i16 %44, 0
  br i1 %45, label %bb52.i.i.i.i.i.i, label %bb50.i.i.i.i.i.i

bb46.i.i.i.i.i.i:                                 ; preds = %bb44.i.i.i.i.i.i
  %46 = add i64 %37, 16
  %47 = tail call i16 @llvm.cttz.i16(i16 %40, i1 true) #21, !range !44
  %48 = zext i16 %47 to i64
  %_109.i.i.i.i.i.i = add i64 %46, %48
  br label %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i

bb52.i.i.i.i.i.i:                                 ; preds = %bb48.i.i.i.i.i.i
  %49 = add i64 %37, 48
  %50 = bitcast <16 x i1> %30 to i16
  %51 = zext i16 %50 to i32
  %52 = tail call i32 @llvm.cttz.i32(i32 %51, i1 false) #21, !range !45
  %53 = zext i32 %52 to i64
  %_122.i.i.i.i.i.i = add i64 %49, %53
  br label %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i

bb50.i.i.i.i.i.i:                                 ; preds = %bb48.i.i.i.i.i.i
  %54 = add i64 %37, 32
  %55 = tail call i16 @llvm.cttz.i16(i16 %44, i1 true) #21, !range !44
  %56 = zext i16 %55 to i64
  %_116.i.i.i.i.i.i = add i64 %54, %56
  br label %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i

bb65.i.i.i.i.i.i:                                 ; preds = %bb63.i.i.i.i.i.i, %bb58.preheader.i.i.i.i.i.i
  %ptr.2.lcssa.i.i.i.i.i.i = phi i8* [ %ptr.1.lcssa.i.i.i.i.i.i, %bb58.preheader.i.i.i.i.i.i ], [ %64, %bb63.i.i.i.i.i.i ]
  %_143.i.i.i.i.i.i = icmp ult i8* %ptr.2.lcssa.i.i.i.i.i.i, %7
  br i1 %_143.i.i.i.i.i.i, label %bb66.i.i.i.i.i.i, label %_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE.exit

bb60.i.i.i.i.i.i:                                 ; preds = %bb58.preheader.i.i.i.i.i.i, %bb63.i.i.i.i.i.i
  %ptr.2153.i.i.i.i.i.i = phi i8* [ %64, %bb63.i.i.i.i.i.i ], [ %ptr.1.lcssa.i.i.i.i.i.i, %bb58.preheader.i.i.i.i.i.i ]
  %57 = bitcast i8* %ptr.2153.i.i.i.i.i.i to <16 x i8>*
  %.0.copyload9.i46.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %57, align 1, !alias.scope !69, !noalias !78
  %58 = icmp eq <16 x i8> %.0.copyload9.i46.i.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i.i
  %59 = bitcast <16 x i1> %58 to i16
  %60 = icmp eq i16 %59, 0
  br i1 %60, label %bb63.i.i.i.i.i.i, label %bb62.i.i.i.i.i.i

bb62.i.i.i.i.i.i:                                 ; preds = %bb60.i.i.i.i.i.i
  %_3.i.i47.i.i.i.i.i.i = ptrtoint i8* %ptr.2153.i.i.i.i.i.i to i64
  %61 = sub i64 %_3.i.i47.i.i.i.i.i.i, %_43.i.i.i.i.i.i
  %62 = tail call i16 @llvm.cttz.i16(i16 %59, i1 true) #21, !range !44
  %63 = zext i16 %62 to i64
  %_13.i.i.i.i.i.i.i = add i64 %61, %63
  br label %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i

bb63.i.i.i.i.i.i:                                 ; preds = %bb60.i.i.i.i.i.i
  %64 = getelementptr inbounds i8, i8* %ptr.2153.i.i.i.i.i.i, i64 16
  %_129.not.i.i.i.i.i.i = icmp ugt i8* %64, %19
  br i1 %_129.not.i.i.i.i.i.i, label %bb65.i.i.i.i.i.i, label %bb60.i.i.i.i.i.i

bb66.i.i.i.i.i.i:                                 ; preds = %bb65.i.i.i.i.i.i
  %_3.i53.i.i.i.i.i.i = ptrtoint i8* %7 to i64
  %_5.i54.i.i.i.i.i.i = ptrtoint i8* %ptr.2.lcssa.i.i.i.i.i.i to i64
  %.neg.neg.i.i.i.i.i.i = add i64 %_3.i53.i.i.i.i.i.i, -16
  %65 = sub i64 %.neg.neg.i.i.i.i.i.i, %_5.i54.i.i.i.i.i.i
  %66 = getelementptr inbounds i8, i8* %ptr.2.lcssa.i.i.i.i.i.i, i64 %65
  %67 = bitcast i8* %66 to <16 x i8>*
  %.0.copyload9.i55.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %67, align 1, !alias.scope !69, !noalias !78
  %68 = icmp eq <16 x i8> %.0.copyload9.i55.i.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i.i
  %69 = bitcast <16 x i1> %68 to i16
  %70 = icmp eq i16 %69, 0
  br i1 %70, label %_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE.exit, label %bb4.i59.i.i.i.i.i.i

bb4.i59.i.i.i.i.i.i:                              ; preds = %bb66.i.i.i.i.i.i
  %_3.i.i56.i.i.i.i.i.i = ptrtoint i8* %66 to i64
  %71 = sub i64 %_3.i.i56.i.i.i.i.i.i, %_43.i.i.i.i.i.i
  %72 = tail call i16 @llvm.cttz.i16(i16 %69, i1 true) #21, !range !44
  %73 = zext i16 %72 to i64
  %_13.i58.i.i.i.i.i.i = add i64 %71, %73
  br label %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i

_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i: ; preds = %bb4.i59.i.i.i.i.i.i, %bb62.i.i.i.i.i.i, %bb50.i.i.i.i.i.i, %bb52.i.i.i.i.i.i, %bb46.i.i.i.i.i.i, %bb42.i.i.i.i.i.i, %bb15.i.i.i.i.i.i, %bb8.i.i.i.i.i.i
  %.sroa.3.0.i.ph.i.i = phi i64 [ %_13.i58.i.i.i.i.i.i, %bb4.i59.i.i.i.i.i.i ], [ %_102.i.i.i.i.i.i, %bb42.i.i.i.i.i.i ], [ %_109.i.i.i.i.i.i, %bb46.i.i.i.i.i.i ], [ %_116.i.i.i.i.i.i, %bb50.i.i.i.i.i.i ], [ %_122.i.i.i.i.i.i, %bb52.i.i.i.i.i.i ], [ %_13.i.i.i.i.i.i.i, %bb62.i.i.i.i.i.i ], [ %16, %bb15.i.i.i.i.i.i ], [ %14, %bb8.i.i.i.i.i.i ]
  %74 = tail call i64 @llvm.usub.sat.i64(i64 %.sroa.3.0.i.ph.i.i, i64 %_6.i.i) #21
  br label %_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE.exit

panic.i.i:                                        ; preds = %bb5.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_6.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1062 to %"core::panic::location::Location"*)) #22, !noalias !79
  unreachable

bb11.i:                                           ; preds = %bb7.i
  %75 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_6.i.i
  %_37.i = load i8, i8* %75, align 1, !alias.scope !56, !noalias !80
  %.0.vec.insert.i.i.i.i = insertelement <16 x i8> undef, i8 %_37.i, i32 0
  %.15.vec.insert.i.i.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %_45.i = icmp ult i64 %_8.i.i, %needle.1
  br i1 %_45.i, label %bb13.i, label %panic2.i, !prof !60

panic.i:                                          ; preds = %bb7.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_6.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1058 to %"core::panic::location::Location"*)) #22, !noalias !59
  unreachable

bb13.i:                                           ; preds = %bb11.i
  %76 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_8.i.i
  %_42.i = load i8, i8* %76, align 1, !alias.scope !56, !noalias !80
  %.0.vec.insert.i.i.i5.i = insertelement <16 x i8> undef, i8 %_42.i, i32 0
  %.15.vec.insert.i.i.i6.i = shufflevector <16 x i8> %.0.vec.insert.i.i.i5.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %_46.not41.i = icmp ugt i8* %0, %3
  br i1 %_46.not41.i, label %bb23.i, label %bb16.i

panic2.i:                                         ; preds = %bb11.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_8.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1060 to %"core::panic::location::Location"*)) #22, !noalias !59
  unreachable

bb23.i:                                           ; preds = %bb20.i, %bb13.i
  %ptr.0.lcssa.i = phi i8* [ %0, %bb13.i ], [ %86, %bb20.i ]
  %_65.i = icmp ult i8* %ptr.0.lcssa.i, %1
  br i1 %_65.i, label %bb24.i, label %bb29.i

bb16.i:                                           ; preds = %bb13.i, %bb20.i
  %ptr.042.i = phi i8* [ %86, %bb20.i ], [ %0, %bb13.i ]
  %77 = getelementptr inbounds i8, i8* %ptr.042.i, i64 %_6.i.i
  %78 = bitcast i8* %77 to <16 x i8>*
  %.0.copyload25.i.i = load <16 x i8>, <16 x i8>* %78, align 1, !alias.scope !54, !noalias !81
  %79 = getelementptr inbounds i8, i8* %ptr.042.i, i64 %_8.i.i
  %80 = bitcast i8* %79 to <16 x i8>*
  %.0.copyload626.i.i = load <16 x i8>, <16 x i8>* %80, align 1, !alias.scope !54, !noalias !81
  %81 = icmp eq <16 x i8> %.0.copyload25.i.i, %.15.vec.insert.i.i.i.i
  %82 = icmp eq <16 x i8> %.0.copyload626.i.i, %.15.vec.insert.i.i.i6.i
  %83 = and <16 x i1> %82, %81
  %84 = bitcast <16 x i1> %83 to i16
  %.not.i = icmp eq i16 %84, 0
  br i1 %.not.i, label %bb20.i, label %bb18.i

bb18.i:                                           ; preds = %bb16.i
  %85 = tail call i16 @llvm.cttz.i16(i16 %84, i1 true), !range !44
  %_25.i.i = zext i16 %85 to i64
; call memchr::memmem::prefilter::genericsimd::matched
  %_57.i = tail call fastcc i64 @_ZN6memchr6memmem9prefilter11genericsimd7matched17h7145dd3b02952477E({ i32, i32 }* noalias nonnull align 4 dereferenceable(8) %prestate, i8* nonnull %0, i8* nonnull %ptr.042.i, i64 %_25.i.i), !noalias !82
  br label %_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE.exit

bb20.i:                                           ; preds = %bb16.i
  %86 = getelementptr inbounds i8, i8* %ptr.042.i, i64 16
  %_46.not.i = icmp ugt i8* %86, %3
  br i1 %_46.not.i, label %bb23.i, label %bb16.i

bb29.i:                                           ; preds = %bb24.i, %bb23.i
  %87 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %prestate, i64 0, i32 0
  %_4.i.i = load i32, i32* %87, align 4, !alias.scope !83, !noalias !86
  %88 = tail call i32 @llvm.uadd.sat.i32(i32 %_4.i.i, i32 1) #21
  store i32 %88, i32* %87, align 4, !alias.scope !83, !noalias !86
  %_5.i.i = icmp ugt i64 %haystack.1, 4294967295
  %89 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %prestate, i64 0, i32 1
  %_9.i13.i = load i32, i32* %89, align 4, !alias.scope !83, !noalias !86
  %_10.i.i = trunc i64 %haystack.1 to i32
  %90 = tail call i32 @llvm.uadd.sat.i32(i32 %_9.i13.i, i32 %_10.i.i) #21
  %.sink.i.i = select i1 %_5.i.i, i32 -1, i32 %90
  store i32 %.sink.i.i, i32* %89, align 4, !alias.scope !83, !noalias !86
  br label %_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE.exit

bb24.i:                                           ; preds = %bb23.i
  %91 = getelementptr inbounds i8, i8* %3, i64 %_6.i.i
  %92 = bitcast i8* %91 to <16 x i8>*
  %.0.copyload25.i7.i = load <16 x i8>, <16 x i8>* %92, align 1, !alias.scope !54, !noalias !81
  %93 = getelementptr inbounds i8, i8* %1, i64 -16
  %94 = bitcast i8* %93 to <16 x i8>*
  %.0.copyload626.i8.i = load <16 x i8>, <16 x i8>* %94, align 1, !alias.scope !54, !noalias !81
  %95 = icmp eq <16 x i8> %.0.copyload25.i7.i, %.15.vec.insert.i.i.i.i
  %96 = icmp eq <16 x i8> %.0.copyload626.i8.i, %.15.vec.insert.i.i.i6.i
  %97 = and <16 x i1> %96, %95
  %98 = bitcast <16 x i1> %97 to i16
  %.not24.i = icmp eq i16 %98, 0
  br i1 %.not24.i, label %bb29.i, label %bb26.i

bb26.i:                                           ; preds = %bb24.i
  %99 = tail call i16 @llvm.cttz.i16(i16 %98, i1 true), !range !44
  %_25.i9.i = zext i16 %99 to i64
; call memchr::memmem::prefilter::genericsimd::matched
  %_77.i = tail call fastcc i64 @_ZN6memchr6memmem9prefilter11genericsimd7matched17h7145dd3b02952477E({ i32, i32 }* noalias nonnull align 4 dereferenceable(8) %prestate, i8* nonnull %0, i8* nonnull %3, i64 %_25.i9.i), !noalias !82
  br label %_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE.exit

_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE.exit: ; preds = %bb10.i.i.i.i.i.i, %bb2.i.i, %bb65.i.i.i.i.i.i, %bb66.i.i.i.i.i.i, %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i, %bb18.i, %bb29.i, %bb26.i
  %.sroa.5.1.i = phi i64 [ undef, %bb29.i ], [ %_57.i, %bb18.i ], [ %_77.i, %bb26.i ], [ %74, %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i ], [ undef, %bb66.i.i.i.i.i.i ], [ undef, %bb65.i.i.i.i.i.i ], [ undef, %bb2.i.i ], [ undef, %bb10.i.i.i.i.i.i ]
  %.sroa.0.1.i = phi i64 [ 0, %bb29.i ], [ 1, %bb18.i ], [ 1, %bb26.i ], [ 1, %_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE.exit.thread.i.i ], [ 0, %bb66.i.i.i.i.i.i ], [ 0, %bb65.i.i.i.i.i.i ], [ 0, %bb2.i.i ], [ 0, %bb10.i.i.i.i.i.i ]
  %100 = insertvalue { i64, i64 } undef, i64 %.sroa.0.1.i, 0
  %101 = insertvalue { i64, i64 } %100, i64 %.sroa.5.1.i, 1
  ret { i64, i64 } %101
}

; <memchr::memmem::prefilter::PrefilterFn as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN75_$LT$memchr..memmem..prefilter..PrefilterFn$u20$as$u20$core..fmt..Debug$GT$3fmt17hc9e20ef8b425891cE"(i64** noalias nocapture readonly align 8 %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
; call <str as core::fmt::Debug>::fmt
  %0 = tail call zeroext i1 @"_ZN40_$LT$str$u20$as$u20$core..fmt..Debug$GT$3fmt17h2fc61a29b5ba73d9E"([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [19 x i8] }>* @alloc1063 to [0 x i8]*), i64 19, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %0
}

; memchr::memmem::rabinkarp::is_fast
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define zeroext i1 @_ZN6memchr6memmem9rabinkarp7is_fast17h8d008e17f396bd9bE([0 x i8]* noalias nocapture nonnull readonly align 1 %haystack.0, i64 %haystack.1, [0 x i8]* noalias nocapture nonnull readonly align 1 %_needle.0, i64 %_needle.1) unnamed_addr #7 {
start:
  %0 = icmp ult i64 %haystack.1, 16
  ret i1 %0
}

; memchr::memmem::rabinkarp::find
; Function Attrs: nofree nosync nounwind nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memmem9rabinkarp4find17h32441d53ee75e290E([0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1, [0 x i8]* noalias nonnull readonly align 1 %needle.0, i64 %needle.1) unnamed_addr #1 personality i32 (...)* @rust_eh_personality {
start:
  %0 = icmp eq i64 %needle.1, 0
  br i1 %0, label %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit.thread, label %bb5.i

bb5.i:                                            ; preds = %start
  %1 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %_8.i = load i8, i8* %1, align 1, !alias.scope !87
  %_6.i.i = zext i8 %_8.i to i32
  %2 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %needle.1
  %_18.i.i.i = ptrtoint i8* %2 to i64
  br label %bb10.i

bb10.i:                                           ; preds = %bb12.i, %bb5.i
  %iter.sroa.0.0.i = phi i8* [ %1, %bb5.i ], [ %5, %bb12.i ]
  %_3.not.i.i = phi i1 [ false, %bb5.i ], [ true, %bb12.i ]
  %iter.sroa.10.0.i = phi i64 [ 1, %bb5.i ], [ 0, %bb12.i ]
  %nh.sroa.10.0.i = phi i32 [ 1, %bb5.i ], [ %8, %bb12.i ]
  %nh.sroa.0.0.i = phi i32 [ %_6.i.i, %bb5.i ], [ %7, %bb12.i ]
  br i1 %_3.not.i.i, label %bb6.i.i, label %bb2.i.i

bb6.i.i:                                          ; preds = %bb18.i.i.i, %bb10.i
  %_15.i.i.i = phi i8* [ %iter.sroa.0.0.i, %bb10.i ], [ %4, %bb18.i.i.i ]
  %_12.i.i.i = icmp eq i8* %_15.i.i.i, %2
  br i1 %_12.i.i.i, label %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit, label %bb12.i

bb2.i.i:                                          ; preds = %bb10.i
  %_7.i.i = add nsw i64 %iter.sroa.10.0.i, -1
  %_20.i.i.i = ptrtoint i8* %iter.sroa.0.0.i to i64
  %3 = sub nuw i64 %_18.i.i.i, %_20.i.i.i
  %_3.not.i.i.i = icmp ugt i64 %3, %_7.i.i
  br i1 %_3.not.i.i.i, label %bb18.i.i.i, label %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit

bb18.i.i.i:                                       ; preds = %bb2.i.i
  %4 = getelementptr inbounds i8, i8* %iter.sroa.0.0.i, i64 %iter.sroa.10.0.i
  br label %bb6.i.i

bb12.i:                                           ; preds = %bb6.i.i
  %5 = getelementptr inbounds i8, i8* %_15.i.i.i, i64 1
  %b.i = load i8, i8* %_15.i.i.i, align 1, !alias.scope !87
  %6 = shl i32 %nh.sroa.0.0.i, 1
  %_6.i5.i = zext i8 %b.i to i32
  %7 = add i32 %6, %_6.i5.i
  %8 = shl i32 %nh.sroa.10.0.i, 1
  br label %bb10.i

_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit: ; preds = %bb6.i.i, %bb2.i.i
  %_4.i = icmp ult i64 %haystack.1, %needle.1
  br i1 %_4.i, label %_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E.exit, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i"

_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit.thread: ; preds = %start
  %_4.i11 = icmp ult i64 %haystack.1, %needle.1
  br i1 %_4.i11, label %_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E.exit, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i": ; preds = %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit
  br i1 %0, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i, label %bb5.i.preheader.i

bb5.i.preheader.i:                                ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i"
  %9 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %10 = add i64 %needle.1, -1
  %xtraiter = and i64 %needle.1, 7
  %11 = icmp ult i64 %10, 7
  br i1 %11, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa, label %bb5.i.preheader.i.new

bb5.i.preheader.i.new:                            ; preds = %bb5.i.preheader.i
  %unroll_iter = and i64 %needle.1, -8
  br label %bb5.i.i

bb5.i.i:                                          ; preds = %bb5.i.i, %bb5.i.preheader.i.new
  %hash.011.i.i = phi i32 [ 0, %bb5.i.preheader.i.new ], [ %35, %bb5.i.i ]
  %iter.sroa.0.010.i.i = phi i8* [ %9, %bb5.i.preheader.i.new ], [ %33, %bb5.i.i ]
  %niter = phi i64 [ %unroll_iter, %bb5.i.preheader.i.new ], [ %niter.nsub.7, %bb5.i.i ]
  %12 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i, i64 1
  %b.i.i = load i8, i8* %iter.sroa.0.010.i.i, align 1, !alias.scope !90, !noalias !95
  %13 = shl i32 %hash.011.i.i, 1
  %_6.i.i.i = zext i8 %b.i.i to i32
  %14 = add i32 %13, %_6.i.i.i
  %15 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i, i64 2
  %b.i.i.1 = load i8, i8* %12, align 1, !alias.scope !90, !noalias !95
  %16 = shl i32 %14, 1
  %_6.i.i.i.1 = zext i8 %b.i.i.1 to i32
  %17 = add i32 %16, %_6.i.i.i.1
  %18 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i, i64 3
  %b.i.i.2 = load i8, i8* %15, align 1, !alias.scope !90, !noalias !95
  %19 = shl i32 %17, 1
  %_6.i.i.i.2 = zext i8 %b.i.i.2 to i32
  %20 = add i32 %19, %_6.i.i.i.2
  %21 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i, i64 4
  %b.i.i.3 = load i8, i8* %18, align 1, !alias.scope !90, !noalias !95
  %22 = shl i32 %20, 1
  %_6.i.i.i.3 = zext i8 %b.i.i.3 to i32
  %23 = add i32 %22, %_6.i.i.i.3
  %24 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i, i64 5
  %b.i.i.4 = load i8, i8* %21, align 1, !alias.scope !90, !noalias !95
  %25 = shl i32 %23, 1
  %_6.i.i.i.4 = zext i8 %b.i.i.4 to i32
  %26 = add i32 %25, %_6.i.i.i.4
  %27 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i, i64 6
  %b.i.i.5 = load i8, i8* %24, align 1, !alias.scope !90, !noalias !95
  %28 = shl i32 %26, 1
  %_6.i.i.i.5 = zext i8 %b.i.i.5 to i32
  %29 = add i32 %28, %_6.i.i.i.5
  %30 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i, i64 7
  %b.i.i.6 = load i8, i8* %27, align 1, !alias.scope !90, !noalias !95
  %31 = shl i32 %29, 1
  %_6.i.i.i.6 = zext i8 %b.i.i.6 to i32
  %32 = add i32 %31, %_6.i.i.i.6
  %33 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i, i64 8
  %b.i.i.7 = load i8, i8* %30, align 1, !alias.scope !90, !noalias !95
  %34 = shl i32 %32, 1
  %_6.i.i.i.7 = zext i8 %b.i.i.7 to i32
  %35 = add i32 %34, %_6.i.i.i.7
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa, label %bb5.i.i

_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa: ; preds = %bb5.i.i, %bb5.i.preheader.i
  %.lcssa.ph = phi i32 [ undef, %bb5.i.preheader.i ], [ %35, %bb5.i.i ]
  %hash.011.i.i.unr = phi i32 [ 0, %bb5.i.preheader.i ], [ %35, %bb5.i.i ]
  %iter.sroa.0.010.i.i.unr = phi i8* [ %9, %bb5.i.preheader.i ], [ %33, %bb5.i.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i, label %bb5.i.i.epil

bb5.i.i.epil:                                     ; preds = %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa, %bb5.i.i.epil
  %hash.011.i.i.epil = phi i32 [ %38, %bb5.i.i.epil ], [ %hash.011.i.i.unr, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa ]
  %iter.sroa.0.010.i.i.epil = phi i8* [ %36, %bb5.i.i.epil ], [ %iter.sroa.0.010.i.i.unr, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb5.i.i.epil ], [ %xtraiter, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa ]
  %36 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.epil, i64 1
  %b.i.i.epil = load i8, i8* %iter.sroa.0.010.i.i.epil, align 1, !alias.scope !90, !noalias !95
  %37 = shl i32 %hash.011.i.i.epil, 1
  %_6.i.i.i.epil = zext i8 %b.i.i.epil to i32
  %38 = add i32 %37, %_6.i.i.i.epil
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i, label %bb5.i.i.epil, !llvm.loop !97

_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i: ; preds = %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa, %bb5.i.i.epil, %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit.thread, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i"
  %.sroa.3.0.i1218 = phi i32 [ %nh.sroa.10.0.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i" ], [ 1, %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit.thread ], [ %nh.sroa.10.0.i, %bb5.i.i.epil ], [ %nh.sroa.10.0.i, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa ]
  %.sroa.0.0.i1317 = phi i32 [ %nh.sroa.0.0.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i" ], [ 0, %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit.thread ], [ %nh.sroa.0.0.i, %bb5.i.i.epil ], [ %nh.sroa.0.0.i, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa ]
  %hash.0.lcssa.i.i = phi i32 [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i" ], [ 0, %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit.thread ], [ %.lcssa.ph, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i.loopexit.unr-lcssa ], [ %38, %bb5.i.i.epil ]
  %start1.i19 = ptrtoint [0 x i8]* %haystack.0 to i64
  br label %bb6.i

bb6.i:                                            ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i", %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i
  %hash.0.i = phi i32 [ %hash.0.lcssa.i.i, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i ], [ %45, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i" ]
  %haystack.sroa.13.0.i = phi i64 [ %haystack.1, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i ], [ %_7.i.i.i.i.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i" ]
  %haystack.sroa.0.0.i = phi [0 x i8]* [ %haystack.0, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E.exit.i ], [ %47, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i" ]
  %39 = icmp eq i32 %hash.0.i, %.sroa.0.0.i1317
  br i1 %39, label %bb8.i, label %bb14.i

bb8.i:                                            ; preds = %bb6.i
; call memchr::memmem::rabinkarp::is_prefix
  %_24.i = tail call fastcc zeroext i1 @_ZN6memchr6memmem9rabinkarp9is_prefix17h9e377cf0e3b7406bE([0 x i8]* noalias nonnull readonly align 1 %haystack.sroa.0.0.i, i64 %haystack.sroa.13.0.i, [0 x i8]* noalias nonnull readonly align 1 %needle.0, i64 %needle.1)
  br i1 %_24.i, label %bb12.i4, label %bb14.i

bb14.i:                                           ; preds = %bb8.i, %bb6.i
  %_32.not.i = icmp ugt i64 %haystack.sroa.13.0.i, %needle.1
  br i1 %_32.not.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i", label %_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E.exit

bb12.i4:                                          ; preds = %bb8.i
  %_28.i = ptrtoint [0 x i8]* %haystack.sroa.0.0.i to i64
  %_27.i = sub i64 %_28.i, %start1.i19
  br label %_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E.exit

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i": ; preds = %bb14.i
  %40 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i, i64 0, i64 0
  %_41.i = load i8, i8* %40, align 1, !alias.scope !99, !noalias !95
  %41 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i, i64 0, i64 %needle.1
  %_45.i = load i8, i8* %41, align 1, !alias.scope !99, !noalias !95
  %_8.i.i.i = zext i8 %_41.i to i32
  %42 = mul i32 %.sroa.3.0.i1218, %_8.i.i.i
  %43 = sub i32 %hash.0.i, %42
  %44 = shl i32 %43, 1
  %_6.i1.i.i = zext i8 %_45.i to i32
  %45 = add i32 %44, %_6.i1.i.i
  %46 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i, i64 0, i64 1
  %_7.i.i.i.i.i = add i64 %haystack.sroa.13.0.i, -1
  %47 = bitcast i8* %46 to [0 x i8]*
  br label %bb6.i

_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E.exit: ; preds = %bb14.i, %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit.thread, %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit, %bb12.i4
  %.sroa.4.0.i = phi i64 [ undef, %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit ], [ %_27.i, %bb12.i4 ], [ undef, %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit.thread ], [ undef, %bb14.i ]
  %.sroa.0.0.i3 = phi i64 [ 0, %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit ], [ 1, %bb12.i4 ], [ 0, %_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE.exit.thread ], [ 0, %bb14.i ]
  %48 = insertvalue { i64, i64 } undef, i64 %.sroa.0.0.i3, 0
  %49 = insertvalue { i64, i64 } %48, i64 %.sroa.4.0.i, 1
  ret { i64, i64 } %49
}

; memchr::memmem::rabinkarp::rfind
; Function Attrs: nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memmem9rabinkarp5rfind17hf6cec5ba7dcd1e70E([0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1, [0 x i8]* noalias nonnull readonly align 1 %needle.0, i64 %needle.1) unnamed_addr #6 personality i32 (...)* @rust_eh_personality {
start:
  %0 = icmp eq i64 %needle.1, 0
  br i1 %0, label %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit.thread, label %bb5.i

bb5.i:                                            ; preds = %start
  %_9.i = add i64 %needle.1, -1
  %1 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_9.i
  %_8.i = load i8, i8* %1, align 1, !alias.scope !100
  %_6.i.i = zext i8 %_8.i to i32
  %2 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %3 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %needle.1
  %_20.i.i.i.i = ptrtoint [0 x i8]* %needle.0 to i64
  br label %bb11.i

bb11.i:                                           ; preds = %bb13.i, %bb5.i
  %iter.sroa.5.0.i = phi i8* [ %3, %bb5.i ], [ %6, %bb13.i ]
  %_3.not.i.i = phi i1 [ false, %bb5.i ], [ true, %bb13.i ]
  %iter.sroa.10.0.i = phi i64 [ 1, %bb5.i ], [ 0, %bb13.i ]
  %nh.sroa.10.0.i = phi i32 [ 1, %bb5.i ], [ %9, %bb13.i ]
  %nh.sroa.0.0.i = phi i32 [ %_6.i.i, %bb5.i ], [ %8, %bb13.i ]
  br i1 %_3.not.i.i, label %bb6.i.i, label %bb2.i.i

bb6.i.i:                                          ; preds = %bb2.i.i, %bb11.i
  %_11.i.i.i.i = phi i8* [ %storemerge.i.i.i.i, %bb2.i.i ], [ %iter.sroa.5.0.i, %bb11.i ]
  %_12.i.i.i.i = icmp eq i8* %2, %_11.i.i.i.i
  br i1 %_12.i.i.i.i, label %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit, label %bb13.i

bb2.i.i:                                          ; preds = %bb11.i
  %_7.i.i = add nsw i64 %iter.sroa.10.0.i, -1
  %_18.i.i.i.i = ptrtoint i8* %iter.sroa.5.0.i to i64
  %4 = sub nuw i64 %_18.i.i.i.i, %_20.i.i.i.i
  %_3.not.i.i.i.i = icmp ugt i64 %4, %_7.i.i
  %.idx.i.i.i.i = sub nsw i64 0, %iter.sroa.10.0.i
  %5 = getelementptr inbounds i8, i8* %iter.sroa.5.0.i, i64 %.idx.i.i.i.i
  %storemerge.i.i.i.i = select i1 %_3.not.i.i.i.i, i8* %5, i8* %2
  br label %bb6.i.i

bb13.i:                                           ; preds = %bb6.i.i
  %6 = getelementptr inbounds i8, i8* %_11.i.i.i.i, i64 -1
  %b.i = load i8, i8* %6, align 1, !alias.scope !100
  %7 = shl i32 %nh.sroa.0.0.i, 1
  %_6.i5.i = zext i8 %b.i to i32
  %8 = add i32 %7, %_6.i5.i
  %9 = shl i32 %nh.sroa.10.0.i, 1
  br label %bb11.i

_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit: ; preds = %bb6.i.i
  %_4.i = icmp ult i64 %haystack.1, %needle.1
  br i1 %_4.i, label %_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE.exit, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i"

_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit.thread: ; preds = %start
  %_4.i13 = icmp ult i64 %haystack.1, %needle.1
  br i1 %_4.i13, label %_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE.exit, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i": ; preds = %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit
  br i1 %0, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i, label %bb7.preheader.i.i

bb7.preheader.i.i:                                ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i"
  %10 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %11 = add i64 %needle.1, -1
  %xtraiter = and i64 %needle.1, 7
  %12 = icmp ult i64 %11, 7
  br i1 %12, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa, label %bb7.preheader.i.i.new

bb7.preheader.i.i.new:                            ; preds = %bb7.preheader.i.i
  %unroll_iter = and i64 %needle.1, -8
  br label %bb7.i.i

bb7.i.i:                                          ; preds = %bb7.i.i, %bb7.preheader.i.i.new
  %hash.011.i.i = phi i32 [ 0, %bb7.preheader.i.i.new ], [ %36, %bb7.i.i ]
  %iter.sroa.4.010.i.i = phi i8* [ %10, %bb7.preheader.i.i.new ], [ %34, %bb7.i.i ]
  %niter = phi i64 [ %unroll_iter, %bb7.preheader.i.i.new ], [ %niter.nsub.7, %bb7.i.i ]
  %13 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i, i64 -1
  %b.i.i = load i8, i8* %13, align 1, !alias.scope !103, !noalias !108
  %14 = shl i32 %hash.011.i.i, 1
  %_6.i.i.i = zext i8 %b.i.i to i32
  %15 = add i32 %14, %_6.i.i.i
  %16 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i, i64 -2
  %b.i.i.1 = load i8, i8* %16, align 1, !alias.scope !103, !noalias !108
  %17 = shl i32 %15, 1
  %_6.i.i.i.1 = zext i8 %b.i.i.1 to i32
  %18 = add i32 %17, %_6.i.i.i.1
  %19 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i, i64 -3
  %b.i.i.2 = load i8, i8* %19, align 1, !alias.scope !103, !noalias !108
  %20 = shl i32 %18, 1
  %_6.i.i.i.2 = zext i8 %b.i.i.2 to i32
  %21 = add i32 %20, %_6.i.i.i.2
  %22 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i, i64 -4
  %b.i.i.3 = load i8, i8* %22, align 1, !alias.scope !103, !noalias !108
  %23 = shl i32 %21, 1
  %_6.i.i.i.3 = zext i8 %b.i.i.3 to i32
  %24 = add i32 %23, %_6.i.i.i.3
  %25 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i, i64 -5
  %b.i.i.4 = load i8, i8* %25, align 1, !alias.scope !103, !noalias !108
  %26 = shl i32 %24, 1
  %_6.i.i.i.4 = zext i8 %b.i.i.4 to i32
  %27 = add i32 %26, %_6.i.i.i.4
  %28 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i, i64 -6
  %b.i.i.5 = load i8, i8* %28, align 1, !alias.scope !103, !noalias !108
  %29 = shl i32 %27, 1
  %_6.i.i.i.5 = zext i8 %b.i.i.5 to i32
  %30 = add i32 %29, %_6.i.i.i.5
  %31 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i, i64 -7
  %b.i.i.6 = load i8, i8* %31, align 1, !alias.scope !103, !noalias !108
  %32 = shl i32 %30, 1
  %_6.i.i.i.6 = zext i8 %b.i.i.6 to i32
  %33 = add i32 %32, %_6.i.i.i.6
  %34 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i, i64 -8
  %b.i.i.7 = load i8, i8* %34, align 1, !alias.scope !103, !noalias !108
  %35 = shl i32 %33, 1
  %_6.i.i.i.7 = zext i8 %b.i.i.7 to i32
  %36 = add i32 %35, %_6.i.i.i.7
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa, label %bb7.i.i

_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa: ; preds = %bb7.i.i, %bb7.preheader.i.i
  %.lcssa.ph = phi i32 [ undef, %bb7.preheader.i.i ], [ %36, %bb7.i.i ]
  %hash.011.i.i.unr = phi i32 [ 0, %bb7.preheader.i.i ], [ %36, %bb7.i.i ]
  %iter.sroa.4.010.i.i.unr = phi i8* [ %10, %bb7.preheader.i.i ], [ %34, %bb7.i.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i, label %bb7.i.i.epil

bb7.i.i.epil:                                     ; preds = %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa, %bb7.i.i.epil
  %hash.011.i.i.epil = phi i32 [ %39, %bb7.i.i.epil ], [ %hash.011.i.i.unr, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa ]
  %iter.sroa.4.010.i.i.epil = phi i8* [ %37, %bb7.i.i.epil ], [ %iter.sroa.4.010.i.i.unr, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb7.i.i.epil ], [ %xtraiter, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa ]
  %37 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i.epil, i64 -1
  %b.i.i.epil = load i8, i8* %37, align 1, !alias.scope !103, !noalias !108
  %38 = shl i32 %hash.011.i.i.epil, 1
  %_6.i.i.i.epil = zext i8 %b.i.i.epil to i32
  %39 = add i32 %38, %_6.i.i.i.epil
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i, label %bb7.i.i.epil, !llvm.loop !111

_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i: ; preds = %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa, %bb7.i.i.epil, %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit.thread, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i"
  %.sroa.3.0.i1420 = phi i32 [ %nh.sroa.10.0.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i" ], [ 1, %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit.thread ], [ %nh.sroa.10.0.i, %bb7.i.i.epil ], [ %nh.sroa.10.0.i, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa ]
  %.sroa.0.0.i1519 = phi i32 [ %nh.sroa.0.0.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i" ], [ 0, %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit.thread ], [ %nh.sroa.0.0.i, %bb7.i.i.epil ], [ %nh.sroa.0.0.i, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa ]
  %hash.0.lcssa.i.i = phi i32 [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i" ], [ 0, %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit.thread ], [ %.lcssa.ph, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.loopexit.unr-lcssa ], [ %39, %bb7.i.i.epil ]
  %40 = xor i64 %needle.1, -1
  br label %bb5.i3

bb5.i3:                                           ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i", %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i
  %hash.0.i = phi i32 [ %hash.0.lcssa.i.i, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i ], [ %47, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i" ]
  %haystack.sroa.16.0.i = phi i64 [ %haystack.1, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i ], [ %_42.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i" ]
  %41 = icmp eq i32 %.sroa.0.0.i1519, %hash.0.i
  br i1 %41, label %bb7.i, label %bb12.i

bb7.i:                                            ; preds = %bb5.i3
; call memchr::memmem::rabinkarp::is_suffix
  %_24.i = tail call fastcc zeroext i1 @_ZN6memchr6memmem9rabinkarp9is_suffix17hf2646f43626eea64E([0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.sroa.16.0.i, [0 x i8]* noalias nonnull readonly align 1 %needle.0, i64 %needle.1), !noalias !112
  br i1 %_24.i, label %bb11.i4, label %bb12.i

bb12.i:                                           ; preds = %bb7.i, %bb5.i3
  %_32.not.i = icmp ugt i64 %haystack.sroa.16.0.i, %needle.1
  br i1 %_32.not.i, label %bb14.i, label %_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE.exit

bb11.i4:                                          ; preds = %bb7.i
  %_27.i = sub i64 %haystack.sroa.16.0.i, %needle.1
  br label %_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE.exit

bb14.i:                                           ; preds = %bb12.i
  %_42.i = add i64 %haystack.sroa.16.0.i, -1
  %_48.i = add i64 %haystack.sroa.16.0.i, %40
  %_55.i = icmp ult i64 %_48.i, %haystack.sroa.16.0.i
  br i1 %_55.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i", label %panic1.i, !prof !60

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i": ; preds = %bb14.i
  %42 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_42.i
  %_41.i = load i8, i8* %42, align 1, !alias.scope !113, !noalias !108
  %_8.i.i.i = zext i8 %_41.i to i32
  %43 = mul i32 %.sroa.3.0.i1420, %_8.i.i.i
  %44 = sub i32 %hash.0.i, %43
  %45 = shl i32 %44, 1
  %46 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_48.i
  %_47.i = load i8, i8* %46, align 1, !alias.scope !113, !noalias !108
  %_6.i1.i.i = zext i8 %_47.i to i32
  %47 = add i32 %45, %_6.i1.i.i
  br label %bb5.i3

panic1.i:                                         ; preds = %bb14.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_48.i, i64 %haystack.sroa.16.0.i, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1077 to %"core::panic::location::Location"*)) #22, !noalias !108
  unreachable

_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE.exit: ; preds = %bb12.i, %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit.thread, %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit, %bb11.i4
  %.sroa.4.0.i = phi i64 [ undef, %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit ], [ %_27.i, %bb11.i4 ], [ undef, %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit.thread ], [ undef, %bb12.i ]
  %.sroa.0.0.i2 = phi i64 [ 0, %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit ], [ 1, %bb11.i4 ], [ 0, %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit.thread ], [ 0, %bb12.i ]
  %48 = insertvalue { i64, i64 } undef, i64 %.sroa.0.0.i2, 0
  %49 = insertvalue { i64, i64 } %48, i64 %.sroa.4.0.i, 1
  ret { i64, i64 } %49
}

; memchr::memmem::rabinkarp::rfind_with
; Function Attrs: nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE({ i32, i32 }* noalias nocapture readonly align 4 dereferenceable(8) %0, [0 x i8]* noalias nonnull readonly align 1 %1, i64 %2, [0 x i8]* noalias nocapture nonnull readonly align 1 %needle.0, i64 %needle.1) unnamed_addr #6 {
start:
  %_4 = icmp ult i64 %2, %needle.1
  br i1 %_4, label %bb20, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit"

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit": ; preds = %start
  %_12.i.i9.i = icmp eq i64 %needle.1, 0
  br i1 %_12.i.i9.i, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit, label %bb7.preheader.i

bb7.preheader.i:                                  ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit"
  %3 = getelementptr inbounds [0 x i8], [0 x i8]* %1, i64 0, i64 %2
  %4 = add i64 %needle.1, -1
  %xtraiter = and i64 %needle.1, 7
  %5 = icmp ult i64 %4, 7
  br i1 %5, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.loopexit.unr-lcssa, label %bb7.preheader.i.new

bb7.preheader.i.new:                              ; preds = %bb7.preheader.i
  %unroll_iter = and i64 %needle.1, -8
  br label %bb7.i

bb7.i:                                            ; preds = %bb7.i, %bb7.preheader.i.new
  %hash.011.i = phi i32 [ 0, %bb7.preheader.i.new ], [ %29, %bb7.i ]
  %iter.sroa.4.010.i = phi i8* [ %3, %bb7.preheader.i.new ], [ %27, %bb7.i ]
  %niter = phi i64 [ %unroll_iter, %bb7.preheader.i.new ], [ %niter.nsub.7, %bb7.i ]
  %6 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i, i64 -1
  %b.i = load i8, i8* %6, align 1, !alias.scope !114
  %7 = shl i32 %hash.011.i, 1
  %_6.i.i = zext i8 %b.i to i32
  %8 = add i32 %7, %_6.i.i
  %9 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i, i64 -2
  %b.i.1 = load i8, i8* %9, align 1, !alias.scope !114
  %10 = shl i32 %8, 1
  %_6.i.i.1 = zext i8 %b.i.1 to i32
  %11 = add i32 %10, %_6.i.i.1
  %12 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i, i64 -3
  %b.i.2 = load i8, i8* %12, align 1, !alias.scope !114
  %13 = shl i32 %11, 1
  %_6.i.i.2 = zext i8 %b.i.2 to i32
  %14 = add i32 %13, %_6.i.i.2
  %15 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i, i64 -4
  %b.i.3 = load i8, i8* %15, align 1, !alias.scope !114
  %16 = shl i32 %14, 1
  %_6.i.i.3 = zext i8 %b.i.3 to i32
  %17 = add i32 %16, %_6.i.i.3
  %18 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i, i64 -5
  %b.i.4 = load i8, i8* %18, align 1, !alias.scope !114
  %19 = shl i32 %17, 1
  %_6.i.i.4 = zext i8 %b.i.4 to i32
  %20 = add i32 %19, %_6.i.i.4
  %21 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i, i64 -6
  %b.i.5 = load i8, i8* %21, align 1, !alias.scope !114
  %22 = shl i32 %20, 1
  %_6.i.i.5 = zext i8 %b.i.5 to i32
  %23 = add i32 %22, %_6.i.i.5
  %24 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i, i64 -7
  %b.i.6 = load i8, i8* %24, align 1, !alias.scope !114
  %25 = shl i32 %23, 1
  %_6.i.i.6 = zext i8 %b.i.6 to i32
  %26 = add i32 %25, %_6.i.i.6
  %27 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i, i64 -8
  %b.i.7 = load i8, i8* %27, align 1, !alias.scope !114
  %28 = shl i32 %26, 1
  %_6.i.i.7 = zext i8 %b.i.7 to i32
  %29 = add i32 %28, %_6.i.i.7
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.loopexit.unr-lcssa, label %bb7.i

_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.loopexit.unr-lcssa: ; preds = %bb7.i, %bb7.preheader.i
  %.lcssa.ph = phi i32 [ undef, %bb7.preheader.i ], [ %29, %bb7.i ]
  %hash.011.i.unr = phi i32 [ 0, %bb7.preheader.i ], [ %29, %bb7.i ]
  %iter.sroa.4.010.i.unr = phi i8* [ %3, %bb7.preheader.i ], [ %27, %bb7.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit, label %bb7.i.epil

bb7.i.epil:                                       ; preds = %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.loopexit.unr-lcssa, %bb7.i.epil
  %hash.011.i.epil = phi i32 [ %32, %bb7.i.epil ], [ %hash.011.i.unr, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.loopexit.unr-lcssa ]
  %iter.sroa.4.010.i.epil = phi i8* [ %30, %bb7.i.epil ], [ %iter.sroa.4.010.i.unr, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb7.i.epil ], [ %xtraiter, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.loopexit.unr-lcssa ]
  %30 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.epil, i64 -1
  %b.i.epil = load i8, i8* %30, align 1, !alias.scope !114
  %31 = shl i32 %hash.011.i.epil, 1
  %_6.i.i.epil = zext i8 %b.i.epil to i32
  %32 = add i32 %31, %_6.i.i.epil
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit, label %bb7.i.epil, !llvm.loop !117

_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit: ; preds = %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.loopexit.unr-lcssa, %bb7.i.epil, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit"
  %hash.0.lcssa.i = phi i32 [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit" ], [ %.lcssa.ph, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.loopexit.unr-lcssa ], [ %32, %bb7.i.epil ]
  %.idx = getelementptr { i32, i32 }, { i32, i32 }* %0, i64 0, i32 0
  %.idx.val = load i32, i32* %.idx, align 4
  %33 = xor i64 %needle.1, -1
  %.idx20 = getelementptr { i32, i32 }, { i32, i32 }* %0, i64 0, i32 1
  %.idx20.val = load i32, i32* %.idx20, align 4
  br label %bb5

bb20:                                             ; preds = %bb12, %bb11, %start
  %.sroa.4.0 = phi i64 [ undef, %start ], [ %_27, %bb11 ], [ undef, %bb12 ]
  %.sroa.0.0 = phi i64 [ 0, %start ], [ 1, %bb11 ], [ 0, %bb12 ]
  %34 = insertvalue { i64, i64 } undef, i64 %.sroa.0.0, 0
  %35 = insertvalue { i64, i64 } %34, i64 %.sroa.4.0, 1
  ret { i64, i64 } %35

bb5:                                              ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit", %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit
  %hash.0 = phi i32 [ %hash.0.lcssa.i, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit ], [ %42, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit" ]
  %haystack.sroa.16.0 = phi i64 [ %2, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit ], [ %_42, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit" ]
  %36 = icmp eq i32 %.idx.val, %hash.0
  br i1 %36, label %bb7, label %bb12

bb7:                                              ; preds = %bb5
; call memchr::memmem::rabinkarp::is_suffix
  %_24 = tail call fastcc zeroext i1 @_ZN6memchr6memmem9rabinkarp9is_suffix17hf2646f43626eea64E([0 x i8]* noalias nonnull readonly align 1 %1, i64 %haystack.sroa.16.0, [0 x i8]* noalias nonnull readonly align 1 %needle.0, i64 %needle.1)
  br i1 %_24, label %bb11, label %bb12

bb12:                                             ; preds = %bb5, %bb7
  %_32.not = icmp ugt i64 %haystack.sroa.16.0, %needle.1
  br i1 %_32.not, label %bb14, label %bb20

bb11:                                             ; preds = %bb7
  %_27 = sub i64 %haystack.sroa.16.0, %needle.1
  br label %bb20

bb14:                                             ; preds = %bb12
  %_42 = add i64 %haystack.sroa.16.0, -1
  %_48 = add i64 %haystack.sroa.16.0, %33
  %_55 = icmp ult i64 %_48, %haystack.sroa.16.0
  br i1 %_55, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit", label %panic1, !prof !60

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit": ; preds = %bb14
  %37 = getelementptr inbounds [0 x i8], [0 x i8]* %1, i64 0, i64 %_42
  %_41 = load i8, i8* %37, align 1
  %_8.i.i = zext i8 %_41 to i32
  %38 = mul i32 %.idx20.val, %_8.i.i
  %39 = sub i32 %hash.0, %38
  %40 = shl i32 %39, 1
  %41 = getelementptr inbounds [0 x i8], [0 x i8]* %1, i64 0, i64 %_48
  %_47 = load i8, i8* %41, align 1
  %_6.i1.i = zext i8 %_47 to i32
  %42 = add i32 %40, %_6.i1.i
  br label %bb5

panic1:                                           ; preds = %bb14
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_48, i64 %haystack.sroa.16.0, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1077 to %"core::panic::location::Location"*)) #22
  unreachable
}

; memchr::memmem::rabinkarp::is_prefix
; Function Attrs: cold nofree noinline nosync nounwind nonlazybind uwtable
define internal fastcc zeroext i1 @_ZN6memchr6memmem9rabinkarp9is_prefix17h9e377cf0e3b7406bE([0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1, [0 x i8]* noalias nocapture nonnull readonly align 1 %needle.0, i64 %needle.1) unnamed_addr #8 personality i32 (...)* @rust_eh_personality {
start:
  tail call void @llvm.experimental.noalias.scope.decl(metadata !118)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !121)
  %_3.not.i = icmp ugt i64 %needle.1, %haystack.1
  br i1 %_3.not.i, label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit, label %bb2.i.i

bb2.i.i:                                          ; preds = %start
  tail call void @llvm.experimental.noalias.scope.decl(metadata !123)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !126)
  %_8.i.i = icmp ult i64 %needle.1, 4
  br i1 %_8.i.i, label %bb7.i.i, label %bb14.i.i

bb14.i.i:                                         ; preds = %bb2.i.i
  %_38.i.i = add i64 %needle.1, -4
  %0 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_38.i.i
  %1 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_38.i.i
  %_4671.i.i = icmp sgt i64 %_38.i.i, 0
  br i1 %_4671.i.i, label %bb20.preheader.i.i, label %bb27.i.i

bb20.preheader.i.i:                               ; preds = %bb14.i.i
  %2 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %3 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  br label %bb20.i.i

bb7.i.i:                                          ; preds = %bb2.i.i
  %exitcond.not.i.i = icmp eq i64 %needle.1, 0
  br i1 %exitcond.not.i.i, label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit, label %bb9.i.i

bb9.i.i:                                          ; preds = %bb7.i.i
  %4 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %5 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %b1.i.i = load i8, i8* %5, align 1, !alias.scope !128, !noalias !129
  %b2.i.i = load i8, i8* %4, align 1, !alias.scope !129, !noalias !128
  %_23.not.i.i = icmp eq i8 %b1.i.i, %b2.i.i
  br i1 %_23.not.i.i, label %bb7.i.i.1, label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit

bb27.i.i:                                         ; preds = %bb24.i.i, %bb14.i.i
  %_63.i.i = bitcast i8* %0 to i32*
  %_63.val.i.i = load i32, i32* %_63.i.i, align 1, !alias.scope !128, !noalias !129
  %_66.i.i = bitcast i8* %1 to i32*
  %_66.val.i.i = load i32, i32* %_66.i.i, align 1, !alias.scope !129, !noalias !128
  %6 = icmp eq i32 %_63.val.i.i, %_66.val.i.i
  br label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit

bb20.i.i:                                         ; preds = %bb24.i.i, %bb20.preheader.i.i
  %py.073.i.i = phi i8* [ %8, %bb24.i.i ], [ %2, %bb20.preheader.i.i ]
  %px.072.i.i = phi i8* [ %7, %bb24.i.i ], [ %3, %bb20.preheader.i.i ]
  %_50.i.i = bitcast i8* %px.072.i.i to i32*
  %_50.val.i.i = load i32, i32* %_50.i.i, align 1, !alias.scope !128, !noalias !129
  %_53.i.i = bitcast i8* %py.073.i.i to i32*
  %_53.val.i.i = load i32, i32* %_53.i.i, align 1, !alias.scope !129, !noalias !128
  %_55.not.i.i = icmp eq i32 %_50.val.i.i, %_53.val.i.i
  br i1 %_55.not.i.i, label %bb24.i.i, label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit

bb24.i.i:                                         ; preds = %bb20.i.i
  %7 = getelementptr inbounds i8, i8* %px.072.i.i, i64 4
  %8 = getelementptr inbounds i8, i8* %py.073.i.i, i64 4
  %_46.i.i = icmp ult i8* %7, %0
  br i1 %_46.i.i, label %bb20.i.i, label %bb27.i.i

_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit: ; preds = %bb9.i.i.2, %bb20.i.i, %bb7.i.i, %bb9.i.i, %bb7.i.i.1, %bb9.i.i.1, %bb7.i.i.2, %start, %bb27.i.i
  %.0.i = phi i1 [ false, %start ], [ %6, %bb27.i.i ], [ %exitcond.not.i.i, %bb9.i.i ], [ %exitcond.not.i.i, %bb7.i.i ], [ %exitcond.not.i.i.1, %bb7.i.i.1 ], [ %exitcond.not.i.i.1, %bb9.i.i.1 ], [ %exitcond.not.i.i.2, %bb7.i.i.2 ], [ false, %bb20.i.i ], [ %spec.select, %bb9.i.i.2 ]
  ret i1 %.0.i

bb7.i.i.1:                                        ; preds = %bb9.i.i
  %exitcond.not.i.i.1 = icmp eq i64 %needle.1, 1
  br i1 %exitcond.not.i.i.1, label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit, label %bb9.i.i.1

bb9.i.i.1:                                        ; preds = %bb7.i.i.1
  %9 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 1
  %10 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 1
  %b1.i.i.1 = load i8, i8* %10, align 1, !alias.scope !128, !noalias !129
  %b2.i.i.1 = load i8, i8* %9, align 1, !alias.scope !129, !noalias !128
  %_23.not.i.i.1 = icmp eq i8 %b1.i.i.1, %b2.i.i.1
  br i1 %_23.not.i.i.1, label %bb7.i.i.2, label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit

bb7.i.i.2:                                        ; preds = %bb9.i.i.1
  %exitcond.not.i.i.2 = icmp eq i64 %needle.1, 2
  br i1 %exitcond.not.i.i.2, label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit, label %bb9.i.i.2

bb9.i.i.2:                                        ; preds = %bb7.i.i.2
  %11 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 2
  %12 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 2
  %b1.i.i.2 = load i8, i8* %12, align 1, !alias.scope !128, !noalias !129
  %b2.i.i.2 = load i8, i8* %11, align 1, !alias.scope !129, !noalias !128
  %_23.not.i.i.2 = icmp eq i8 %b1.i.i.2, %b2.i.i.2
  %exitcond.not.i.i.3 = icmp eq i64 %needle.1, 3
  %spec.select = select i1 %_23.not.i.i.2, i1 %exitcond.not.i.i.3, i1 %exitcond.not.i.i.2
  br label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit
}

; memchr::memmem::rabinkarp::is_suffix
; Function Attrs: cold nofree noinline nosync nounwind nonlazybind uwtable
define internal fastcc zeroext i1 @_ZN6memchr6memmem9rabinkarp9is_suffix17hf2646f43626eea64E([0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1, [0 x i8]* noalias nocapture nonnull readonly align 1 %needle.0, i64 %needle.1) unnamed_addr #8 personality i32 (...)* @rust_eh_personality {
start:
  tail call void @llvm.experimental.noalias.scope.decl(metadata !130)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !133)
  %_3.not.i = icmp ult i64 %haystack.1, %needle.1
  br i1 %_3.not.i, label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit, label %bb2.i.i

bb2.i.i:                                          ; preds = %start
  %_14.i = sub i64 %haystack.1, %needle.1
  %0 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_14.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !135)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !138)
  %_8.i.i = icmp ult i64 %needle.1, 4
  br i1 %_8.i.i, label %bb7.i.i, label %bb14.i.i

bb14.i.i:                                         ; preds = %bb2.i.i
  %_38.i.i = add i64 %needle.1, -4
  %1 = getelementptr inbounds i8, i8* %0, i64 %_38.i.i
  %2 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_38.i.i
  %_4671.i.i = icmp sgt i64 %_38.i.i, 0
  br i1 %_4671.i.i, label %bb20.preheader.i.i, label %bb27.i.i

bb20.preheader.i.i:                               ; preds = %bb14.i.i
  %3 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  br label %bb20.i.i

bb7.i.i:                                          ; preds = %bb2.i.i
  %exitcond.not.i.i = icmp eq i64 %needle.1, 0
  br i1 %exitcond.not.i.i, label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit, label %bb9.i.i

bb9.i.i:                                          ; preds = %bb7.i.i
  %4 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %b1.i.i = load i8, i8* %0, align 1, !alias.scope !140, !noalias !141
  %b2.i.i = load i8, i8* %4, align 1, !alias.scope !141, !noalias !140
  %_23.not.i.i = icmp eq i8 %b1.i.i, %b2.i.i
  br i1 %_23.not.i.i, label %bb7.i.i.1, label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit

bb27.i.i:                                         ; preds = %bb24.i.i, %bb14.i.i
  %_63.i.i = bitcast i8* %1 to i32*
  %_63.val.i.i = load i32, i32* %_63.i.i, align 1, !alias.scope !140, !noalias !141
  %_66.i.i = bitcast i8* %2 to i32*
  %_66.val.i.i = load i32, i32* %_66.i.i, align 1, !alias.scope !141, !noalias !140
  %5 = icmp eq i32 %_63.val.i.i, %_66.val.i.i
  br label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit

bb20.i.i:                                         ; preds = %bb24.i.i, %bb20.preheader.i.i
  %py.073.i.i = phi i8* [ %7, %bb24.i.i ], [ %3, %bb20.preheader.i.i ]
  %px.072.i.i = phi i8* [ %6, %bb24.i.i ], [ %0, %bb20.preheader.i.i ]
  %_50.i.i = bitcast i8* %px.072.i.i to i32*
  %_50.val.i.i = load i32, i32* %_50.i.i, align 1, !alias.scope !140, !noalias !141
  %_53.i.i = bitcast i8* %py.073.i.i to i32*
  %_53.val.i.i = load i32, i32* %_53.i.i, align 1, !alias.scope !141, !noalias !140
  %_55.not.i.i = icmp eq i32 %_50.val.i.i, %_53.val.i.i
  br i1 %_55.not.i.i, label %bb24.i.i, label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit

bb24.i.i:                                         ; preds = %bb20.i.i
  %6 = getelementptr inbounds i8, i8* %px.072.i.i, i64 4
  %7 = getelementptr inbounds i8, i8* %py.073.i.i, i64 4
  %_46.i.i = icmp ult i8* %6, %1
  br i1 %_46.i.i, label %bb20.i.i, label %bb27.i.i

_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit: ; preds = %bb9.i.i.2, %bb20.i.i, %bb7.i.i, %bb9.i.i, %bb7.i.i.1, %bb9.i.i.1, %bb7.i.i.2, %start, %bb27.i.i
  %.0.i = phi i1 [ false, %start ], [ %5, %bb27.i.i ], [ %exitcond.not.i.i, %bb9.i.i ], [ %exitcond.not.i.i, %bb7.i.i ], [ %exitcond.not.i.i.1, %bb7.i.i.1 ], [ %exitcond.not.i.i.1, %bb9.i.i.1 ], [ %exitcond.not.i.i.2, %bb7.i.i.2 ], [ false, %bb20.i.i ], [ %spec.select, %bb9.i.i.2 ]
  ret i1 %.0.i

bb7.i.i.1:                                        ; preds = %bb9.i.i
  %exitcond.not.i.i.1 = icmp eq i64 %needle.1, 1
  br i1 %exitcond.not.i.i.1, label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit, label %bb9.i.i.1

bb9.i.i.1:                                        ; preds = %bb7.i.i.1
  %8 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 1
  %9 = getelementptr inbounds i8, i8* %0, i64 1
  %b1.i.i.1 = load i8, i8* %9, align 1, !alias.scope !140, !noalias !141
  %b2.i.i.1 = load i8, i8* %8, align 1, !alias.scope !141, !noalias !140
  %_23.not.i.i.1 = icmp eq i8 %b1.i.i.1, %b2.i.i.1
  br i1 %_23.not.i.i.1, label %bb7.i.i.2, label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit

bb7.i.i.2:                                        ; preds = %bb9.i.i.1
  %exitcond.not.i.i.2 = icmp eq i64 %needle.1, 2
  br i1 %exitcond.not.i.i.2, label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit, label %bb9.i.i.2

bb9.i.i.2:                                        ; preds = %bb7.i.i.2
  %10 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 2
  %11 = getelementptr inbounds i8, i8* %0, i64 2
  %b1.i.i.2 = load i8, i8* %11, align 1, !alias.scope !140, !noalias !141
  %b2.i.i.2 = load i8, i8* %10, align 1, !alias.scope !141, !noalias !140
  %_23.not.i.i.2 = icmp eq i8 %b1.i.i.2, %b2.i.i.2
  %exitcond.not.i.i.3 = icmp eq i64 %needle.1, 3
  %spec.select = select i1 %_23.not.i.i.2, i1 %exitcond.not.i.i.3, i1 %exitcond.not.i.i.2
  br label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit
}

; <memchr::memmem::FindIter as core::iter::traits::iterator::Iterator>::next
; Function Attrs: nonlazybind uwtable
define { i64, i64 } @"_ZN83_$LT$memchr..memmem..FindIter$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h8d72297dd2eaf869E"(%"memmem::FindIter"* noalias align 8 dereferenceable(112) %self) unnamed_addr #6 personality i32 (...)* @rust_eh_personality {
start:
  %0 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 2
  %_3 = load i64, i64* %0, align 8
  %1 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 0, i32 1
  %_5.1 = load i64, i64* %1, align 8
  %_2 = icmp ult i64 %_5.1, %_3
  br i1 %_2, label %bb11, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit"

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit": ; preds = %start
  %2 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 0, i32 0
  %3 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1
  %_7 = getelementptr %"memmem::Finder", %"memmem::Finder"* %3, i64 0, i32 0
  %_9 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 3
  %_13.0 = load [0 x i8]*, [0 x i8]** %2, align 8, !nonnull !142
  %4 = getelementptr inbounds [0 x i8], [0 x i8]* %_13.0, i64 0, i64 %_3
  %_7.i.i.i.i = sub i64 %_5.1, %_3
  %5 = bitcast i8* %4 to [0 x i8]*
  tail call void @llvm.experimental.noalias.scope.decl(metadata !143)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !146)
  %6 = bitcast %"memmem::Finder"* %3 to [0 x i8]**
  %7 = load [0 x i8]*, [0 x i8]** %6, align 8, !alias.scope !148, !noalias !155, !nonnull !142
  %8 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1, i32 0, i32 0, i32 1
  %9 = load i64, i64* %8, align 8, !alias.scope !148, !noalias !155
  %_6.i = icmp ugt i64 %9, %_7.i.i.i.i
  br i1 %_6.i, label %bb11, label %bb3.i

bb3.i:                                            ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit"
  %10 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1, i32 0, i32 2, i32 0
  %11 = load i8, i8* %10, align 8, !range !157, !alias.scope !143, !noalias !155
  %_11.i = zext i8 %11 to i64
  switch i64 %_11.i, label %bb5.i [
    i64 0, label %bb5
    i64 1, label %bb7.i
    i64 2, label %bb9.i
    i64 3, label %bb16.i
    i64 4, label %bb4.i
  ]

bb5.i:                                            ; preds = %bb3.i
  unreachable

bb7.i:                                            ; preds = %bb3.i
  %12 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1, i32 0, i32 2, i32 1, i64 0
  %b.i = load i8, i8* %12, align 1, !alias.scope !143, !noalias !155
  %13 = icmp eq i64 %_7.i.i.i.i, 0
  br i1 %13, label %bb11, label %bb3.i.i

bb3.i.i:                                          ; preds = %bb7.i
  %.0.vec.insert.i.i.i.i.i.i30.i = insertelement <16 x i8> undef, i8 %b.i, i32 0
  %.15.vec.insert.i.i.i.i.i.i31.i = shufflevector <16 x i8> %.0.vec.insert.i.i.i.i.i.i30.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %14 = icmp ult i64 %_7.i.i.i.i, 64
  %.0.sroa.speculated.i.i.i.i.i.i.i.i = select i1 %14, i64 %_7.i.i.i.i, i64 64
  %15 = getelementptr inbounds [0 x i8], [0 x i8]* %_13.0, i64 0, i64 %_5.1
  %_17.i.i.i.i.i = icmp ult i64 %_7.i.i.i.i, 16
  br i1 %_17.i.i.i.i.i, label %bb7.i.i.i.i.i, label %bb13.i.i.i.i.i

bb13.i.i.i.i.i:                                   ; preds = %bb3.i.i
  %16 = bitcast i8* %4 to <16 x i8>*
  %.0.copyload9.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %16, align 1, !alias.scope !158, !noalias !167
  %17 = icmp eq <16 x i8> %.0.copyload9.i.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i31.i
  %18 = bitcast <16 x i1> %17 to i16
  %19 = icmp eq i16 %18, 0
  br i1 %19, label %bb16.i.i.i.i.i, label %bb15.i.i.i.i.i

bb7.i.i.i.i.i:                                    ; preds = %bb3.i.i, %bb10.i.i.i.i32.i
  %ptr.0156.i.i.i.i.i = phi i8* [ %20, %bb10.i.i.i.i32.i ], [ %4, %bb3.i.i ]
  %_24.i.i.i.i.i = load i8, i8* %ptr.0156.i.i.i.i.i, align 1, !alias.scope !158, !noalias !167
  %_23.i.i.i.i.i = icmp eq i8 %_24.i.i.i.i.i, %b.i
  br i1 %_23.i.i.i.i.i, label %bb8.i.i.i.i.i, label %bb10.i.i.i.i32.i

bb10.i.i.i.i32.i:                                 ; preds = %bb7.i.i.i.i.i
  %20 = getelementptr inbounds i8, i8* %ptr.0156.i.i.i.i.i, i64 1
  %_20.i.i.i.i.i = icmp ult i8* %20, %15
  br i1 %_20.i.i.i.i.i, label %bb7.i.i.i.i.i, label %bb11

bb8.i.i.i.i.i:                                    ; preds = %bb7.i.i.i.i.i
  %_3.i.i.i.i.i.i = ptrtoint i8* %ptr.0156.i.i.i.i.i to i64
  %_5.i.i.i.i.i.i = ptrtoint i8* %4 to i64
  %21 = sub i64 %_3.i.i.i.i.i.i, %_5.i.i.i.i.i.i
  br label %bb5

bb15.i.i.i.i.i:                                   ; preds = %bb13.i.i.i.i.i
  %22 = tail call i16 @llvm.cttz.i16(i16 %18, i1 true) #21, !range !44
  %23 = zext i16 %22 to i64
  br label %bb5

bb16.i.i.i.i.i:                                   ; preds = %bb13.i.i.i.i.i
  %_43.i.i.i.i.i = ptrtoint i8* %4 to i64
  %_42.i.i.i.i.i = and i64 %_43.i.i.i.i.i, 15
  %_41.i.i.i.i.i = sub nuw nsw i64 16, %_42.i.i.i.i.i
  %24 = getelementptr inbounds i8, i8* %4, i64 %_41.i.i.i.i.i
  %_46.i.i.i.i.i = icmp ugt i64 %_7.i.i.i.i, 63
  %25 = getelementptr inbounds i8, i8* %15, i64 -64
  %_48144.i.i.i.i.i = icmp ule i8* %24, %25
  %or.cond145.i.i.i.i.i = select i1 %_46.i.i.i.i.i, i1 %_48144.i.i.i.i.i, i1 false
  br i1 %or.cond145.i.i.i.i.i, label %bb23.i.i.i.i.i, label %bb58.preheader.i.i.i.i.i

bb58.preheader.i.i.i.i.i:                         ; preds = %bb55.i.i.i.i.i, %bb16.i.i.i.i.i
  %ptr.1.lcssa.i.i.i.i.i = phi i8* [ %24, %bb16.i.i.i.i.i ], [ %43, %bb55.i.i.i.i.i ]
  %26 = getelementptr inbounds i8, i8* %15, i64 -16
  %_129.not152.i.i.i.i.i = icmp ugt i8* %ptr.1.lcssa.i.i.i.i.i, %26
  br i1 %_129.not152.i.i.i.i.i, label %bb65.i.i.i.i.i, label %bb60.i.i.i.i.i

bb23.i.i.i.i.i:                                   ; preds = %bb16.i.i.i.i.i, %bb55.i.i.i.i.i
  %ptr.1146.i.i.i.i.i = phi i8* [ %43, %bb55.i.i.i.i.i ], [ %24, %bb16.i.i.i.i.i ]
  %27 = bitcast i8* %ptr.1146.i.i.i.i.i to <16 x i8>*
  %_57.val133.i.i.i.i.i = load <16 x i8>, <16 x i8>* %27, align 16, !alias.scope !158, !noalias !167
  %28 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i, i64 16
  %29 = bitcast i8* %28 to <16 x i8>*
  %_60.val134.i.i.i.i.i = load <16 x i8>, <16 x i8>* %29, align 16, !alias.scope !158, !noalias !167
  %30 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i, i64 32
  %31 = bitcast i8* %30 to <16 x i8>*
  %_64.val135.i.i.i.i.i = load <16 x i8>, <16 x i8>* %31, align 16, !alias.scope !158, !noalias !167
  %32 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i, i64 48
  %33 = bitcast i8* %32 to <16 x i8>*
  %_69.val136.i.i.i.i.i = load <16 x i8>, <16 x i8>* %33, align 16, !alias.scope !158, !noalias !167
  %34 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i31.i, %_57.val133.i.i.i.i.i
  %35 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i31.i, %_60.val134.i.i.i.i.i
  %36 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i31.i, %_64.val135.i.i.i.i.i
  %37 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i31.i, %_69.val136.i.i.i.i.i
  %38 = or <16 x i1> %35, %34
  %39 = or <16 x i1> %38, %36
  %40 = or <16 x i1> %39, %37
  %41 = bitcast <16 x i1> %40 to i16
  %42 = icmp eq i16 %41, 0
  br i1 %42, label %bb55.i.i.i.i.i, label %bb39.i.i.i.i.i

bb55.i.i.i.i.i:                                   ; preds = %bb23.i.i.i.i.i
  %43 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i, i64 %.0.sroa.speculated.i.i.i.i.i.i.i.i
  %_48.not.i.i.i.i.i = icmp ugt i8* %43, %25
  br i1 %_48.not.i.i.i.i.i, label %bb58.preheader.i.i.i.i.i, label %bb23.i.i.i.i.i

bb39.i.i.i.i.i:                                   ; preds = %bb23.i.i.i.i.i
  %_3.i40.i.i.i.i.i = ptrtoint i8* %ptr.1146.i.i.i.i.i to i64
  %44 = sub i64 %_3.i40.i.i.i.i.i, %_43.i.i.i.i.i
  %45 = bitcast <16 x i1> %34 to i16
  %46 = icmp eq i16 %45, 0
  br i1 %46, label %bb44.i.i.i.i.i, label %bb42.i.i.i.i.i

bb44.i.i.i.i.i:                                   ; preds = %bb39.i.i.i.i.i
  %47 = bitcast <16 x i1> %35 to i16
  %48 = icmp eq i16 %47, 0
  br i1 %48, label %bb48.i.i.i.i.i, label %bb46.i.i.i.i.i

bb42.i.i.i.i.i:                                   ; preds = %bb39.i.i.i.i.i
  %49 = tail call i16 @llvm.cttz.i16(i16 %45, i1 true) #21, !range !44
  %50 = zext i16 %49 to i64
  %_102.i.i.i.i.i = add i64 %44, %50
  br label %bb5

bb48.i.i.i.i.i:                                   ; preds = %bb44.i.i.i.i.i
  %51 = bitcast <16 x i1> %36 to i16
  %52 = icmp eq i16 %51, 0
  br i1 %52, label %bb52.i.i.i.i.i, label %bb50.i.i.i.i.i

bb46.i.i.i.i.i:                                   ; preds = %bb44.i.i.i.i.i
  %53 = add i64 %44, 16
  %54 = tail call i16 @llvm.cttz.i16(i16 %47, i1 true) #21, !range !44
  %55 = zext i16 %54 to i64
  %_109.i.i.i.i.i = add i64 %53, %55
  br label %bb5

bb52.i.i.i.i.i:                                   ; preds = %bb48.i.i.i.i.i
  %56 = add i64 %44, 48
  %57 = bitcast <16 x i1> %37 to i16
  %58 = zext i16 %57 to i32
  %59 = tail call i32 @llvm.cttz.i32(i32 %58, i1 false) #21, !range !45
  %60 = zext i32 %59 to i64
  %_122.i.i.i.i.i = add i64 %56, %60
  br label %bb5

bb50.i.i.i.i.i:                                   ; preds = %bb48.i.i.i.i.i
  %61 = add i64 %44, 32
  %62 = tail call i16 @llvm.cttz.i16(i16 %51, i1 true) #21, !range !44
  %63 = zext i16 %62 to i64
  %_116.i.i.i.i.i = add i64 %61, %63
  br label %bb5

bb65.i.i.i.i.i:                                   ; preds = %bb63.i.i.i.i.i, %bb58.preheader.i.i.i.i.i
  %ptr.2.lcssa.i.i.i.i.i = phi i8* [ %ptr.1.lcssa.i.i.i.i.i, %bb58.preheader.i.i.i.i.i ], [ %71, %bb63.i.i.i.i.i ]
  %_143.i.i.i.i.i = icmp ult i8* %ptr.2.lcssa.i.i.i.i.i, %15
  br i1 %_143.i.i.i.i.i, label %bb66.i.i.i.i.i, label %bb11

bb60.i.i.i.i.i:                                   ; preds = %bb58.preheader.i.i.i.i.i, %bb63.i.i.i.i.i
  %ptr.2153.i.i.i.i.i = phi i8* [ %71, %bb63.i.i.i.i.i ], [ %ptr.1.lcssa.i.i.i.i.i, %bb58.preheader.i.i.i.i.i ]
  %64 = bitcast i8* %ptr.2153.i.i.i.i.i to <16 x i8>*
  %.0.copyload9.i46.i.i.i.i.i = load <16 x i8>, <16 x i8>* %64, align 1, !alias.scope !158, !noalias !167
  %65 = icmp eq <16 x i8> %.0.copyload9.i46.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i31.i
  %66 = bitcast <16 x i1> %65 to i16
  %67 = icmp eq i16 %66, 0
  br i1 %67, label %bb63.i.i.i.i.i, label %bb62.i.i.i.i.i

bb62.i.i.i.i.i:                                   ; preds = %bb60.i.i.i.i.i
  %_3.i.i47.i.i.i.i.i = ptrtoint i8* %ptr.2153.i.i.i.i.i to i64
  %68 = sub i64 %_3.i.i47.i.i.i.i.i, %_43.i.i.i.i.i
  %69 = tail call i16 @llvm.cttz.i16(i16 %66, i1 true) #21, !range !44
  %70 = zext i16 %69 to i64
  %_13.i.i.i.i.i.i = add i64 %68, %70
  br label %bb5

bb63.i.i.i.i.i:                                   ; preds = %bb60.i.i.i.i.i
  %71 = getelementptr inbounds i8, i8* %ptr.2153.i.i.i.i.i, i64 16
  %_129.not.i.i.i.i.i = icmp ugt i8* %71, %26
  br i1 %_129.not.i.i.i.i.i, label %bb65.i.i.i.i.i, label %bb60.i.i.i.i.i

bb66.i.i.i.i.i:                                   ; preds = %bb65.i.i.i.i.i
  %_3.i53.i.i.i.i.i = ptrtoint i8* %15 to i64
  %_5.i54.i.i.i.i.i = ptrtoint i8* %ptr.2.lcssa.i.i.i.i.i to i64
  %.neg.neg.i.i.i.i.i = add i64 %_3.i53.i.i.i.i.i, -16
  %72 = sub i64 %.neg.neg.i.i.i.i.i, %_5.i54.i.i.i.i.i
  %73 = getelementptr inbounds i8, i8* %ptr.2.lcssa.i.i.i.i.i, i64 %72
  %74 = bitcast i8* %73 to <16 x i8>*
  %.0.copyload9.i55.i.i.i.i.i = load <16 x i8>, <16 x i8>* %74, align 1, !alias.scope !158, !noalias !167
  %75 = icmp eq <16 x i8> %.0.copyload9.i55.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i31.i
  %76 = bitcast <16 x i1> %75 to i16
  %77 = icmp eq i16 %76, 0
  br i1 %77, label %bb11, label %bb4.i59.i.i.i.i.i

bb4.i59.i.i.i.i.i:                                ; preds = %bb66.i.i.i.i.i
  %_3.i.i56.i.i.i.i.i = ptrtoint i8* %73 to i64
  %78 = sub i64 %_3.i.i56.i.i.i.i.i, %_43.i.i.i.i.i
  %79 = tail call i16 @llvm.cttz.i16(i16 %76, i1 true) #21, !range !44
  %80 = zext i16 %79 to i64
  %_13.i58.i.i.i.i.i = add i64 %78, %80
  br label %bb5

bb9.i:                                            ; preds = %bb3.i
  %81 = icmp ult i64 %_7.i.i.i.i, 16
  br i1 %81, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i37.i", label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb16.i:                                           ; preds = %bb3.i
  %82 = getelementptr %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1, i32 0, i32 2, i32 1, i64 1
  %.idx.val.i = load i8, i8* %82, align 2, !alias.scope !143, !noalias !155
  %_2.i.i.i = zext i8 %.idx.val.i to i64
  %83 = add nuw nsw i64 %_2.i.i.i, 16
  %_29.i = icmp ugt i64 %83, %_7.i.i.i.i
  br i1 %_29.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i", label %bb5.i.i.i.i

bb4.i:                                            ; preds = %bb3.i
; call core::panicking::panic
  tail call void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [40 x i8] }>* @alloc1175 to [0 x i8]*), i64 40, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1174 to %"core::panic::location::Location"*)) #22, !noalias !167
  unreachable

bb34.sink.split.i.i.i.i:                          ; preds = %bb9.i.us.i47.i.i.i.i.2, %bb2.i.i.i.i.i.i, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i, %bb7.i.us.i.us.i.i.i.i, %bb2.i.i79.i.i.i.i, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i, %bb2.i.us.i40.i.i.i.i, %bb7.i.us.i43.i.i.i.i.1, %bb7.i.us.i43.i.i.i.i.2
  %.lcssa153.sink = phi i32 [ %165, %bb7.i.us.i43.i.i.i.i.2 ], [ %165, %bb7.i.us.i43.i.i.i.i.1 ], [ %165, %bb2.i.us.i40.i.i.i.i ], [ %169, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i ], [ %177, %bb2.i.i79.i.i.i.i ], [ %103, %bb7.i.us.i.us.i.i.i.i ], [ %121, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i ], [ %140, %bb2.i.i.i.i.i.i ], [ %165, %bb9.i.us.i47.i.i.i.i.2 ]
  %ptr.0130.sink.i.i.i.i = phi i8* [ %88, %bb7.i.us.i43.i.i.i.i.2 ], [ %88, %bb7.i.us.i43.i.i.i.i.1 ], [ %88, %bb2.i.us.i40.i.i.i.i ], [ %88, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i ], [ %88, %bb2.i.i79.i.i.i.i ], [ %ptr.0134.us.i.i.i.i, %bb7.i.us.i.us.i.i.i.i ], [ %ptr.0134.us142.i.i.i.i, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i ], [ %ptr.0134.i.i.i.i, %bb2.i.i.i.i.i.i ], [ %88, %bb9.i.us.i47.i.i.i.i.2 ]
  %offset.i.i.i.i.i.le = zext i32 %.lcssa153.sink to i64
; call memchr::memmem::genericsimd::matched
  %_58.i.i.i.i = tail call fastcc i64 @_ZN6memchr6memmem11genericsimd7matched17hf7507d2589d36674E(i8* nonnull %4, i8* nonnull %ptr.0130.sink.i.i.i.i, i64 %offset.i.i.i.i.i.le)
  br label %bb5

bb5.i.i.i.i:                                      ; preds = %bb16.i
  %.idx28.i = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1, i32 0, i32 2, i32 1, i64 0
  %.idx28.val.i = load i8, i8* %.idx28.i, align 1, !alias.scope !168, !noalias !173
  tail call void @llvm.experimental.noalias.scope.decl(metadata !178)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !181)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !183)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !186)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !188)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !191)
  %_21.i.i.i.i = zext i8 %.idx28.val.i to i64
  %_29.i.i.i.i = icmp ugt i64 %9, %_21.i.i.i.i
  br i1 %_29.i.i.i.i, label %bb6.i.i.i.i, label %panic.i.i.i.i, !prof !60

bb6.i.i.i.i:                                      ; preds = %bb5.i.i.i.i
  %84 = getelementptr inbounds [0 x i8], [0 x i8]* %7, i64 0, i64 %_21.i.i.i.i
  %_26.i.i.i.i = load i8, i8* %84, align 1, !alias.scope !193, !noalias !194
  %.0.vec.insert.i.i.i.i.i.i.i = insertelement <16 x i8> undef, i8 %_26.i.i.i.i, i32 0
  %.15.vec.insert.i.i.i.i.i.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i.i.i.i.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %_34.i.i.i.i = icmp ugt i64 %9, %_2.i.i.i
  br i1 %_34.i.i.i.i, label %bb8.i.i.i.i, label %panic2.i.i.i.i, !prof !60

panic.i.i.i.i:                                    ; preds = %bb5.i.i.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_21.i.i.i.i, i64 %9, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1049 to %"core::panic::location::Location"*)) #22, !noalias !197
  unreachable

bb8.i.i.i.i:                                      ; preds = %bb6.i.i.i.i
  %85 = getelementptr inbounds [0 x i8], [0 x i8]* %7, i64 0, i64 %_2.i.i.i
  %_31.i.i.i.i = load i8, i8* %85, align 1, !alias.scope !193, !noalias !194
  %.0.vec.insert.i.i.i25.i.i.i.i = insertelement <16 x i8> undef, i8 %_31.i.i.i.i, i32 0
  %.15.vec.insert.i.i.i26.i.i.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i.i25.i.i.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %86 = getelementptr inbounds [0 x i8], [0 x i8]* %_13.0, i64 0, i64 %_5.1
  %87 = sub nuw nsw i64 -16, %_2.i.i.i
  %88 = getelementptr inbounds i8, i8* %86, i64 %87
  %89 = sub i64 0, %9
  %90 = getelementptr inbounds i8, i8* %86, i64 %89
  %_38.i.i.i.i.i.i = add i64 %9, -4
  %91 = getelementptr inbounds [0 x i8], [0 x i8]* %7, i64 0, i64 %_38.i.i.i.i.i.i
  %_4671.i.i.i.i.i.i = icmp sgt i64 %_38.i.i.i.i.i.i, 0
  %92 = getelementptr [0 x i8], [0 x i8]* %7, i64 0, i64 0
  %_63.i.i.i.i.i.i = bitcast i8* %91 to i32*
  %_45.not133.i.i.i.i = icmp ugt i8* %4, %88
  br i1 %_45.not133.i.i.i.i, label %bb21.i.i.i.i, label %bb14.lr.ph.i.i.i.i

bb14.lr.ph.i.i.i.i:                               ; preds = %bb8.i.i.i.i
  %_8.i.i.i.i.i.i = icmp ult i64 %9, 4
  br i1 %_8.i.i.i.i.i.i, label %bb14.us.i.i.i.i, label %bb14.lr.ph.split.i.i.i.i

bb14.us.i.i.i.i:                                  ; preds = %bb14.lr.ph.i.i.i.i, %bb18.us.i.i.i.i
  %ptr.0134.us.i.i.i.i = phi i8* [ %110, %bb18.us.i.i.i.i ], [ %4, %bb14.lr.ph.i.i.i.i ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !198)
  %93 = getelementptr inbounds i8, i8* %ptr.0134.us.i.i.i.i, i64 %_21.i.i.i.i
  %94 = bitcast i8* %93 to <16 x i8>*
  %.0.copyload28.i.us.i.i.i.i = load <16 x i8>, <16 x i8>* %94, align 1, !alias.scope !201, !noalias !202
  %95 = getelementptr inbounds i8, i8* %ptr.0134.us.i.i.i.i, i64 %_2.i.i.i
  %96 = bitcast i8* %95 to <16 x i8>*
  %.0.copyload629.i.us.i.i.i.i = load <16 x i8>, <16 x i8>* %96, align 1, !alias.scope !201, !noalias !202
  %97 = icmp eq <16 x i8> %.0.copyload28.i.us.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i
  %98 = icmp eq <16 x i8> %.0.copyload629.i.us.i.i.i.i, %.15.vec.insert.i.i.i26.i.i.i.i
  %99 = and <16 x i1> %98, %97
  %100 = bitcast <16 x i1> %99 to i16
  %101 = icmp eq i16 %100, 0
  br i1 %101, label %bb18.us.i.i.i.i, label %bb10.us.i.us.preheader.i.i.i.i

bb10.us.i.us.preheader.i.i.i.i:                   ; preds = %bb14.us.i.i.i.i
  %102 = zext i16 %100 to i32
  br label %bb10.us.i.us.i.i.i.i

bb10.us.i.us.i.i.i.i:                             ; preds = %bb19.loopexit.us.i.us.i.i.i.i, %bb10.us.i.us.preheader.i.i.i.i
  %match_offsets.042.us.i.us.i.i.i.i = phi i32 [ %108, %bb19.loopexit.us.i.us.i.i.i.i ], [ %102, %bb10.us.i.us.preheader.i.i.i.i ]
  %103 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.us.i.us.i.i.i.i, i1 true) #21, !range !45
  %offset.us.i.us.i.i.i.i = zext i32 %103 to i64
  %104 = getelementptr inbounds i8, i8* %ptr.0134.us.i.i.i.i, i64 %offset.us.i.us.i.i.i.i
  %_37.us.i.us.i.i.i.i = icmp ult i8* %90, %104
  br i1 %_37.us.i.us.i.i.i.i, label %bb18.us.i.i.i.i, label %bb2.i.us.i.us.i.i.i.i

bb2.i.us.i.us.i.i.i.i:                            ; preds = %bb10.us.i.us.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !203) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !206) #21
  br label %bb7.i.us.i.us.i.i.i.i

bb7.i.us.i.us.i.i.i.i:                            ; preds = %bb9.i.us.i.us.i.i.i.i, %bb2.i.us.i.us.i.i.i.i
  %iter.sroa.9.0.i.us.i.us.i.i.i.i = phi i64 [ %107, %bb9.i.us.i.us.i.i.i.i ], [ 0, %bb2.i.us.i.us.i.i.i.i ]
  %exitcond.not.i.us.i.us.i.i.i.i = icmp eq i64 %iter.sroa.9.0.i.us.i.us.i.i.i.i, %9
  br i1 %exitcond.not.i.us.i.us.i.i.i.i, label %bb34.sink.split.i.i.i.i, label %bb9.i.us.i.us.i.i.i.i

bb9.i.us.i.us.i.i.i.i:                            ; preds = %bb7.i.us.i.us.i.i.i.i
  %105 = getelementptr inbounds i8, i8* %104, i64 %iter.sroa.9.0.i.us.i.us.i.i.i.i
  %106 = getelementptr inbounds [0 x i8], [0 x i8]* %7, i64 0, i64 %iter.sroa.9.0.i.us.i.us.i.i.i.i
  %107 = add nuw nsw i64 %iter.sroa.9.0.i.us.i.us.i.i.i.i, 1
  %b1.i.us.i.us.i.i.i.i = load i8, i8* %106, align 1, !alias.scope !208, !noalias !209
  %b2.i.us.i.us.i.i.i.i = load i8, i8* %105, align 1, !alias.scope !210, !noalias !211
  %_23.not.i.us.i.us.i.i.i.i = icmp eq i8 %b1.i.us.i.us.i.i.i.i, %b2.i.us.i.us.i.i.i.i
  br i1 %_23.not.i.us.i.us.i.i.i.i, label %bb7.i.us.i.us.i.i.i.i, label %bb19.loopexit.us.i.us.i.i.i.i

bb19.loopexit.us.i.us.i.i.i.i:                    ; preds = %bb9.i.us.i.us.i.i.i.i
  %_51.us.i.us.i.i.i.i = add i32 %match_offsets.042.us.i.us.i.i.i.i, -1
  %108 = and i32 %_51.us.i.us.i.i.i.i, %match_offsets.042.us.i.us.i.i.i.i
  %109 = icmp eq i32 %108, 0
  br i1 %109, label %bb18.us.i.i.i.i, label %bb10.us.i.us.i.i.i.i

bb18.us.i.i.i.i:                                  ; preds = %bb19.loopexit.us.i.us.i.i.i.i, %bb10.us.i.us.i.i.i.i, %bb14.us.i.i.i.i
  %110 = getelementptr inbounds i8, i8* %ptr.0134.us.i.i.i.i, i64 16
  %_45.not.us.i.i.i.i = icmp ugt i8* %110, %88
  br i1 %_45.not.us.i.i.i.i, label %bb21.i.i.i.i, label %bb14.us.i.i.i.i

bb14.lr.ph.split.i.i.i.i:                         ; preds = %bb14.lr.ph.i.i.i.i
  br i1 %_4671.i.i.i.i.i.i, label %bb14.us141.i.i.i.i, label %bb14.i.i.i.i

bb14.us141.i.i.i.i:                               ; preds = %bb14.lr.ph.split.i.i.i.i, %bb18.us146.i.i.i.i
  %ptr.0134.us142.i.i.i.i = phi i8* [ %129, %bb18.us146.i.i.i.i ], [ %4, %bb14.lr.ph.split.i.i.i.i ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !198)
  %111 = getelementptr inbounds i8, i8* %ptr.0134.us142.i.i.i.i, i64 %_21.i.i.i.i
  %112 = bitcast i8* %111 to <16 x i8>*
  %.0.copyload28.i.us143.i.i.i.i = load <16 x i8>, <16 x i8>* %112, align 1, !alias.scope !201, !noalias !202
  %113 = getelementptr inbounds i8, i8* %ptr.0134.us142.i.i.i.i, i64 %_2.i.i.i
  %114 = bitcast i8* %113 to <16 x i8>*
  %.0.copyload629.i.us144.i.i.i.i = load <16 x i8>, <16 x i8>* %114, align 1, !alias.scope !201, !noalias !202
  %115 = icmp eq <16 x i8> %.0.copyload28.i.us143.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i
  %116 = icmp eq <16 x i8> %.0.copyload629.i.us144.i.i.i.i, %.15.vec.insert.i.i.i26.i.i.i.i
  %117 = and <16 x i1> %116, %115
  %118 = bitcast <16 x i1> %117 to i16
  %119 = icmp eq i16 %118, 0
  br i1 %119, label %bb18.us146.i.i.i.i, label %bb10.us48.i.us.preheader.i.i.i.i

bb10.us48.i.us.preheader.i.i.i.i:                 ; preds = %bb14.us141.i.i.i.i
  %120 = zext i16 %118 to i32
  br label %bb10.us48.i.us.i.i.i.i

bb10.us48.i.us.i.i.i.i:                           ; preds = %bb19.us53.i.us.i.i.i.i, %bb10.us48.i.us.preheader.i.i.i.i
  %match_offsets.042.us49.i.us.i.i.i.i = phi i32 [ %127, %bb19.us53.i.us.i.i.i.i ], [ %120, %bb10.us48.i.us.preheader.i.i.i.i ]
  %121 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.us49.i.us.i.i.i.i, i1 true) #21, !range !45
  %offset.us50.i.us.i.i.i.i = zext i32 %121 to i64
  %122 = getelementptr inbounds i8, i8* %ptr.0134.us142.i.i.i.i, i64 %offset.us50.i.us.i.i.i.i
  %_37.us51.i.us.i.i.i.i = icmp ult i8* %90, %122
  br i1 %_37.us51.i.us.i.i.i.i, label %bb18.us146.i.i.i.i, label %bb2.i.us52.i.us.i.i.i.i

bb2.i.us52.i.us.i.i.i.i:                          ; preds = %bb10.us48.i.us.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !203) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !206) #21
  %123 = getelementptr inbounds i8, i8* %122, i64 %_38.i.i.i.i.i.i
  br label %bb20.i.us.i.us.i.i.i.i

bb20.i.us.i.us.i.i.i.i:                           ; preds = %bb24.i.us.i.us.i.i.i.i, %bb2.i.us52.i.us.i.i.i.i
  %py.073.i.us.i.us.i.i.i.i = phi i8* [ %125, %bb24.i.us.i.us.i.i.i.i ], [ %122, %bb2.i.us52.i.us.i.i.i.i ]
  %px.072.i.us.i.us.i.i.i.i = phi i8* [ %124, %bb24.i.us.i.us.i.i.i.i ], [ %92, %bb2.i.us52.i.us.i.i.i.i ]
  %_50.i.us.i.us.i.i.i.i = bitcast i8* %px.072.i.us.i.us.i.i.i.i to i32*
  %_50.val.i.us.i.us.i.i.i.i = load i32, i32* %_50.i.us.i.us.i.i.i.i, align 1, !alias.scope !208, !noalias !209
  %_53.i.us.i.us.i.i.i.i = bitcast i8* %py.073.i.us.i.us.i.i.i.i to i32*
  %_53.val.i.us.i.us.i.i.i.i = load i32, i32* %_53.i.us.i.us.i.i.i.i, align 1, !alias.scope !210, !noalias !211
  %_55.not.i.us.i.us.i.i.i.i = icmp eq i32 %_50.val.i.us.i.us.i.i.i.i, %_53.val.i.us.i.us.i.i.i.i
  br i1 %_55.not.i.us.i.us.i.i.i.i, label %bb24.i.us.i.us.i.i.i.i, label %bb19.us53.i.us.i.i.i.i

bb24.i.us.i.us.i.i.i.i:                           ; preds = %bb20.i.us.i.us.i.i.i.i
  %124 = getelementptr inbounds i8, i8* %px.072.i.us.i.us.i.i.i.i, i64 4
  %125 = getelementptr inbounds i8, i8* %py.073.i.us.i.us.i.i.i.i, i64 4
  %_46.i.us.i.us.i.i.i.i = icmp ult i8* %124, %91
  br i1 %_46.i.us.i.us.i.i.i.i, label %bb20.i.us.i.us.i.i.i.i, label %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i

_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i: ; preds = %bb24.i.us.i.us.i.i.i.i
  %_63.val.i.us.i.us.i.i.i.i = load i32, i32* %_63.i.i.i.i.i.i, align 1, !alias.scope !208, !noalias !209
  %_66.i.us.i.us.i.i.i.i = bitcast i8* %123 to i32*
  %_66.val.i.us.i.us.i.i.i.i = load i32, i32* %_66.i.us.i.us.i.i.i.i, align 1, !alias.scope !210, !noalias !211
  %126 = icmp eq i32 %_63.val.i.us.i.us.i.i.i.i, %_66.val.i.us.i.us.i.i.i.i
  br i1 %126, label %bb34.sink.split.i.i.i.i, label %bb19.us53.i.us.i.i.i.i

bb19.us53.i.us.i.i.i.i:                           ; preds = %bb20.i.us.i.us.i.i.i.i, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i
  %_51.us54.i.us.i.i.i.i = add i32 %match_offsets.042.us49.i.us.i.i.i.i, -1
  %127 = and i32 %_51.us54.i.us.i.i.i.i, %match_offsets.042.us49.i.us.i.i.i.i
  %128 = icmp eq i32 %127, 0
  br i1 %128, label %bb18.us146.i.i.i.i, label %bb10.us48.i.us.i.i.i.i

bb18.us146.i.i.i.i:                               ; preds = %bb19.us53.i.us.i.i.i.i, %bb10.us48.i.us.i.i.i.i, %bb14.us141.i.i.i.i
  %129 = getelementptr inbounds i8, i8* %ptr.0134.us142.i.i.i.i, i64 16
  %_45.not.us147.i.i.i.i = icmp ugt i8* %129, %88
  br i1 %_45.not.us147.i.i.i.i, label %bb21.i.i.i.i, label %bb14.us141.i.i.i.i

panic2.i.i.i.i:                                   ; preds = %bb6.i.i.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_2.i.i.i, i64 %9, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1051 to %"core::panic::location::Location"*)) #22, !noalias !197
  unreachable

bb21.i.i.i.i:                                     ; preds = %bb18.i.i.i.i, %bb18.us146.i.i.i.i, %bb18.us.i.i.i.i, %bb8.i.i.i.i
  %ptr.0.lcssa.i.i.i.i = phi i8* [ %4, %bb8.i.i.i.i ], [ %110, %bb18.us.i.i.i.i ], [ %129, %bb18.us146.i.i.i.i ], [ %146, %bb18.i.i.i.i ]
  %_65.i.i.i.i = icmp ult i8* %ptr.0.lcssa.i.i.i.i, %86
  br i1 %_65.i.i.i.i, label %bb22.i.i.i.i, label %bb11

bb14.i.i.i.i:                                     ; preds = %bb14.lr.ph.split.i.i.i.i, %bb18.i.i.i.i
  %ptr.0134.i.i.i.i = phi i8* [ %146, %bb18.i.i.i.i ], [ %4, %bb14.lr.ph.split.i.i.i.i ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !198)
  %130 = getelementptr inbounds i8, i8* %ptr.0134.i.i.i.i, i64 %_21.i.i.i.i
  %131 = bitcast i8* %130 to <16 x i8>*
  %.0.copyload28.i.i.i.i.i = load <16 x i8>, <16 x i8>* %131, align 1, !alias.scope !201, !noalias !202
  %132 = getelementptr inbounds i8, i8* %ptr.0134.i.i.i.i, i64 %_2.i.i.i
  %133 = bitcast i8* %132 to <16 x i8>*
  %.0.copyload629.i.i.i.i.i = load <16 x i8>, <16 x i8>* %133, align 1, !alias.scope !201, !noalias !202
  %134 = icmp eq <16 x i8> %.0.copyload28.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i
  %135 = icmp eq <16 x i8> %.0.copyload629.i.i.i.i.i, %.15.vec.insert.i.i.i26.i.i.i.i
  %136 = and <16 x i1> %135, %134
  %137 = bitcast <16 x i1> %136 to i16
  %138 = icmp eq i16 %137, 0
  br i1 %138, label %bb18.i.i.i.i, label %bb10.i.preheader.i.i.i.i

bb10.i.preheader.i.i.i.i:                         ; preds = %bb14.i.i.i.i
  %139 = zext i16 %137 to i32
  br label %bb10.i.i.i.i.i

bb10.i.i.i.i.i:                                   ; preds = %bb19.i.i.i.i.i, %bb10.i.preheader.i.i.i.i
  %match_offsets.042.i.i.i.i.i = phi i32 [ %144, %bb19.i.i.i.i.i ], [ %139, %bb10.i.preheader.i.i.i.i ]
  %140 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.i.i.i.i.i, i1 true) #21, !range !45
  %offset.i.i.i.i.i = zext i32 %140 to i64
  %141 = getelementptr inbounds i8, i8* %ptr.0134.i.i.i.i, i64 %offset.i.i.i.i.i
  %_37.i.i.i.i.i = icmp ult i8* %90, %141
  br i1 %_37.i.i.i.i.i, label %bb18.i.i.i.i, label %bb2.i.i.i.i.i.i

bb2.i.i.i.i.i.i:                                  ; preds = %bb10.i.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !203) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !206) #21
  %142 = getelementptr inbounds i8, i8* %141, i64 %_38.i.i.i.i.i.i
  %_63.val.i.i.i.i.i.i = load i32, i32* %_63.i.i.i.i.i.i, align 1, !alias.scope !208, !noalias !209
  %_66.i.i.i.i.i.i = bitcast i8* %142 to i32*
  %_66.val.i.i.i.i.i.i = load i32, i32* %_66.i.i.i.i.i.i, align 1, !alias.scope !210, !noalias !211
  %143 = icmp eq i32 %_63.val.i.i.i.i.i.i, %_66.val.i.i.i.i.i.i
  br i1 %143, label %bb34.sink.split.i.i.i.i, label %bb19.i.i.i.i.i

bb19.i.i.i.i.i:                                   ; preds = %bb2.i.i.i.i.i.i
  %_51.i.i.i.i.i = add i32 %match_offsets.042.i.i.i.i.i, -1
  %144 = and i32 %_51.i.i.i.i.i, %match_offsets.042.i.i.i.i.i
  %145 = icmp eq i32 %144, 0
  br i1 %145, label %bb18.i.i.i.i, label %bb10.i.i.i.i.i

bb18.i.i.i.i:                                     ; preds = %bb19.i.i.i.i.i, %bb10.i.i.i.i.i, %bb14.i.i.i.i
  %146 = getelementptr inbounds i8, i8* %ptr.0134.i.i.i.i, i64 16
  %_45.not.i.i.i.i = icmp ugt i8* %146, %88
  br i1 %_45.not.i.i.i.i, label %bb21.i.i.i.i, label %bb14.i.i.i.i

bb22.i.i.i.i:                                     ; preds = %bb21.i.i.i.i
  %_3.i.i.i.i.i = ptrtoint i8* %86 to i64
  %_5.i.i.i.i.i = ptrtoint i8* %ptr.0.lcssa.i.i.i.i to i64
  %147 = sub i64 %_3.i.i.i.i.i, %_5.i.i.i.i.i
  %_72.i.i.i.i = icmp ult i64 %147, %9
  br i1 %_72.i.i.i.i, label %bb11, label %bb25.i.i.i.i

bb25.i.i.i.i:                                     ; preds = %bb22.i.i.i.i
  %_5.i98.i.i.i.i = ptrtoint i8* %88 to i64
  %148 = sub i64 %_5.i.i.i.i.i, %_5.i98.i.i.i.i
  %149 = trunc i64 %148 to i32
  %150 = and i32 %149, 31
  %notmask.i.i.i.i = shl nsw i32 -1, %150
  tail call void @llvm.experimental.noalias.scope.decl(metadata !212)
  %151 = getelementptr inbounds i8, i8* %88, i64 %_21.i.i.i.i
  %152 = bitcast i8* %151 to <16 x i8>*
  %.0.copyload28.i28.i.i.i.i = load <16 x i8>, <16 x i8>* %152, align 1, !alias.scope !201, !noalias !215
  %153 = getelementptr inbounds i8, i8* %86, i64 -16
  %154 = bitcast i8* %153 to <16 x i8>*
  %.0.copyload629.i30.i.i.i.i = load <16 x i8>, <16 x i8>* %154, align 1, !alias.scope !201, !noalias !215
  %155 = icmp eq <16 x i8> %.0.copyload28.i28.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i
  %156 = icmp eq <16 x i8> %.0.copyload629.i30.i.i.i.i, %.15.vec.insert.i.i.i26.i.i.i.i
  %157 = and <16 x i1> %156, %155
  %158 = bitcast <16 x i1> %157 to i16
  %159 = zext i16 %158 to i32
  %160 = and i32 %notmask.i.i.i.i, %159
  %161 = icmp eq i32 %160, 0
  br i1 %161, label %bb11, label %bb10.lr.ph.i35.i.i.i.i

bb10.lr.ph.i35.i.i.i.i:                           ; preds = %bb25.i.i.i.i
  %_8.i.i34.i.i.i.i = icmp ult i64 %9, 4
  br i1 %_8.i.i34.i.i.i.i, label %bb10.us.i39.i.i.i.i.preheader, label %bb10.lr.ph.split.i50.i.i.i.i

bb10.us.i39.i.i.i.i.preheader:                    ; preds = %bb10.lr.ph.i35.i.i.i.i
  %exitcond.not.i.us.i42.i.i.i.i = icmp eq i64 %9, 0
  %162 = getelementptr inbounds [0 x i8], [0 x i8]* %7, i64 0, i64 0
  %exitcond.not.i.us.i42.i.i.i.i.1 = icmp eq i64 %9, 1
  %163 = getelementptr inbounds [0 x i8], [0 x i8]* %7, i64 0, i64 1
  %exitcond.not.i.us.i42.i.i.i.i.2 = icmp eq i64 %9, 2
  %164 = getelementptr inbounds [0 x i8], [0 x i8]* %7, i64 0, i64 2
  %exitcond.not.i.us.i42.i.i.i.i.3 = icmp eq i64 %9, 3
  br label %bb10.us.i39.i.i.i.i

bb10.us.i39.i.i.i.i:                              ; preds = %bb10.us.i39.i.i.i.i.preheader, %bb19.loopexit.us.i49.i.i.i.i
  %match_offsets.042.us.i36.i.i.i.i = phi i32 [ %167, %bb19.loopexit.us.i49.i.i.i.i ], [ %160, %bb10.us.i39.i.i.i.i.preheader ]
  %165 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.us.i36.i.i.i.i, i1 true) #21, !range !45
  %offset.us.i37.i.i.i.i = zext i32 %165 to i64
  %166 = getelementptr inbounds i8, i8* %88, i64 %offset.us.i37.i.i.i.i
  %_37.us.i38.i.i.i.i = icmp ult i8* %90, %166
  br i1 %_37.us.i38.i.i.i.i, label %bb11, label %bb2.i.us.i40.i.i.i.i

bb2.i.us.i40.i.i.i.i:                             ; preds = %bb10.us.i39.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !216) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !219) #21
  br i1 %exitcond.not.i.us.i42.i.i.i.i, label %bb34.sink.split.i.i.i.i, label %bb9.i.us.i47.i.i.i.i

bb9.i.us.i47.i.i.i.i:                             ; preds = %bb2.i.us.i40.i.i.i.i
  %b1.i.us.i44.i.i.i.i = load i8, i8* %162, align 1, !alias.scope !221, !noalias !222
  %b2.i.us.i45.i.i.i.i = load i8, i8* %166, align 1, !alias.scope !223, !noalias !224
  %_23.not.i.us.i46.i.i.i.i = icmp eq i8 %b1.i.us.i44.i.i.i.i, %b2.i.us.i45.i.i.i.i
  br i1 %_23.not.i.us.i46.i.i.i.i, label %bb7.i.us.i43.i.i.i.i.1, label %bb19.loopexit.us.i49.i.i.i.i

bb19.loopexit.us.i49.i.i.i.i:                     ; preds = %bb9.i.us.i47.i.i.i.i.2, %bb9.i.us.i47.i.i.i.i.1, %bb9.i.us.i47.i.i.i.i
  %_51.us.i48.i.i.i.i = add i32 %match_offsets.042.us.i36.i.i.i.i, -1
  %167 = and i32 %_51.us.i48.i.i.i.i, %match_offsets.042.us.i36.i.i.i.i
  %168 = icmp eq i32 %167, 0
  br i1 %168, label %bb11, label %bb10.us.i39.i.i.i.i

bb10.lr.ph.split.i50.i.i.i.i:                     ; preds = %bb10.lr.ph.i35.i.i.i.i
  br i1 %_4671.i.i.i.i.i.i, label %bb10.us48.i54.i.i.i.i, label %bb10.i75.i.i.i.i

bb10.us48.i54.i.i.i.i:                            ; preds = %bb10.lr.ph.split.i50.i.i.i.i, %bb19.us53.i67.i.i.i.i
  %match_offsets.042.us49.i51.i.i.i.i = phi i32 [ %174, %bb19.us53.i67.i.i.i.i ], [ %160, %bb10.lr.ph.split.i50.i.i.i.i ]
  %169 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.us49.i51.i.i.i.i, i1 true) #21, !range !45
  %offset.us50.i52.i.i.i.i = zext i32 %169 to i64
  %170 = getelementptr inbounds i8, i8* %88, i64 %offset.us50.i52.i.i.i.i
  %_37.us51.i53.i.i.i.i = icmp ult i8* %90, %170
  br i1 %_37.us51.i53.i.i.i.i, label %bb11, label %bb2.i.us52.i55.i.i.i.i

bb2.i.us52.i55.i.i.i.i:                           ; preds = %bb10.us48.i54.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !216) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !219) #21
  %171 = getelementptr inbounds i8, i8* %170, i64 %_38.i.i.i.i.i.i
  br label %bb20.i.us.i63.i.i.i.i

bb20.i.us.i63.i.i.i.i:                            ; preds = %bb24.i.us.i65.i.i.i.i, %bb2.i.us52.i55.i.i.i.i
  %py.073.i.us.i56.i.i.i.i = phi i8* [ %173, %bb24.i.us.i65.i.i.i.i ], [ %170, %bb2.i.us52.i55.i.i.i.i ]
  %px.072.i.us.i57.i.i.i.i = phi i8* [ %172, %bb24.i.us.i65.i.i.i.i ], [ %92, %bb2.i.us52.i55.i.i.i.i ]
  %_50.i.us.i58.i.i.i.i = bitcast i8* %px.072.i.us.i57.i.i.i.i to i32*
  %_50.val.i.us.i59.i.i.i.i = load i32, i32* %_50.i.us.i58.i.i.i.i, align 1, !alias.scope !221, !noalias !222
  %_53.i.us.i60.i.i.i.i = bitcast i8* %py.073.i.us.i56.i.i.i.i to i32*
  %_53.val.i.us.i61.i.i.i.i = load i32, i32* %_53.i.us.i60.i.i.i.i, align 1, !alias.scope !223, !noalias !224
  %_55.not.i.us.i62.i.i.i.i = icmp eq i32 %_50.val.i.us.i59.i.i.i.i, %_53.val.i.us.i61.i.i.i.i
  br i1 %_55.not.i.us.i62.i.i.i.i, label %bb24.i.us.i65.i.i.i.i, label %bb19.us53.i67.i.i.i.i

bb24.i.us.i65.i.i.i.i:                            ; preds = %bb20.i.us.i63.i.i.i.i
  %172 = getelementptr inbounds i8, i8* %px.072.i.us.i57.i.i.i.i, i64 4
  %173 = getelementptr inbounds i8, i8* %py.073.i.us.i56.i.i.i.i, i64 4
  %_46.i.us.i64.i.i.i.i = icmp ult i8* %172, %91
  br i1 %_46.i.us.i64.i.i.i.i, label %bb20.i.us.i63.i.i.i.i, label %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i

bb19.us53.i67.i.i.i.i:                            ; preds = %bb20.i.us.i63.i.i.i.i, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i
  %_51.us54.i66.i.i.i.i = add i32 %match_offsets.042.us49.i51.i.i.i.i, -1
  %174 = and i32 %_51.us54.i66.i.i.i.i, %match_offsets.042.us49.i51.i.i.i.i
  %175 = icmp eq i32 %174, 0
  br i1 %175, label %bb11, label %bb10.us48.i54.i.i.i.i

_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i: ; preds = %bb24.i.us.i65.i.i.i.i
  %_63.val.i.us.i68.i.i.i.i = load i32, i32* %_63.i.i.i.i.i.i, align 1, !alias.scope !221, !noalias !222
  %_66.i.us.i69.i.i.i.i = bitcast i8* %171 to i32*
  %_66.val.i.us.i70.i.i.i.i = load i32, i32* %_66.i.us.i69.i.i.i.i, align 1, !alias.scope !223, !noalias !224
  %176 = icmp eq i32 %_63.val.i.us.i68.i.i.i.i, %_66.val.i.us.i70.i.i.i.i
  br i1 %176, label %bb34.sink.split.i.i.i.i, label %bb19.us53.i67.i.i.i.i

bb10.i75.i.i.i.i:                                 ; preds = %bb10.lr.ph.split.i50.i.i.i.i, %bb19.i81.i.i.i.i
  %match_offsets.042.i72.i.i.i.i = phi i32 [ %181, %bb19.i81.i.i.i.i ], [ %160, %bb10.lr.ph.split.i50.i.i.i.i ]
  %177 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.i72.i.i.i.i, i1 true) #21, !range !45
  %offset.i73.i.i.i.i = zext i32 %177 to i64
  %178 = getelementptr inbounds i8, i8* %88, i64 %offset.i73.i.i.i.i
  %_37.i74.i.i.i.i = icmp ult i8* %90, %178
  br i1 %_37.i74.i.i.i.i, label %bb11, label %bb2.i.i79.i.i.i.i

bb2.i.i79.i.i.i.i:                                ; preds = %bb10.i75.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !216) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !219) #21
  %179 = getelementptr inbounds i8, i8* %178, i64 %_38.i.i.i.i.i.i
  %_63.val.i.i76.i.i.i.i = load i32, i32* %_63.i.i.i.i.i.i, align 1, !alias.scope !221, !noalias !222
  %_66.i.i77.i.i.i.i = bitcast i8* %179 to i32*
  %_66.val.i.i78.i.i.i.i = load i32, i32* %_66.i.i77.i.i.i.i, align 1, !alias.scope !223, !noalias !224
  %180 = icmp eq i32 %_63.val.i.i76.i.i.i.i, %_66.val.i.i78.i.i.i.i
  br i1 %180, label %bb34.sink.split.i.i.i.i, label %bb19.i81.i.i.i.i

bb19.i81.i.i.i.i:                                 ; preds = %bb2.i.i79.i.i.i.i
  %_51.i80.i.i.i.i = add i32 %match_offsets.042.i72.i.i.i.i, -1
  %181 = and i32 %_51.i80.i.i.i.i, %match_offsets.042.i72.i.i.i.i
  %182 = icmp eq i32 %181, 0
  br i1 %182, label %bb11, label %bb10.i75.i.i.i.i

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i": ; preds = %bb16.i
  %_35.idx.i = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1, i32 0, i32 3, i32 0, i32 0
  %_35.idx.val.i = load i32, i32* %_35.idx.i, align 8, !alias.scope !143, !noalias !155
  %_35.idx27.i = getelementptr %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1, i32 0, i32 3, i32 0, i32 1
  %_35.idx27.val.i = load i32, i32* %_35.idx27.i, align 4, !alias.scope !143, !noalias !155
  %start1.i.i = ptrtoint i8* %4 to i64
  %_12.i9.i.i.i = icmp eq i64 %9, 0
  br i1 %_12.i9.i.i.i, label %bb6.i.i.preheader, label %bb5.i.i.i.preheader

bb5.i.i.i.preheader:                              ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i"
  %183 = add i64 %9, -1
  %xtraiter = and i64 %9, 7
  %184 = icmp ult i64 %183, 7
  br i1 %184, label %bb6.i.i.preheader.loopexit.unr-lcssa, label %bb5.i.i.i.preheader.new

bb5.i.i.i.preheader.new:                          ; preds = %bb5.i.i.i.preheader
  %unroll_iter = and i64 %9, -8
  br label %bb5.i.i.i

bb5.i.i.i:                                        ; preds = %bb5.i.i.i, %bb5.i.i.i.preheader.new
  %hash.011.i.i.i = phi i32 [ 0, %bb5.i.i.i.preheader.new ], [ %208, %bb5.i.i.i ]
  %iter.sroa.0.010.i.i.i = phi i8* [ %4, %bb5.i.i.i.preheader.new ], [ %206, %bb5.i.i.i ]
  %niter = phi i64 [ %unroll_iter, %bb5.i.i.i.preheader.new ], [ %niter.nsub.7, %bb5.i.i.i ]
  %185 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 1
  %b.i.i.i = load i8, i8* %iter.sroa.0.010.i.i.i, align 1, !alias.scope !225, !noalias !230
  %186 = shl i32 %hash.011.i.i.i, 1
  %_6.i.i.i.i = zext i8 %b.i.i.i to i32
  %187 = add i32 %186, %_6.i.i.i.i
  %188 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 2
  %b.i.i.i.1 = load i8, i8* %185, align 1, !alias.scope !225, !noalias !230
  %189 = shl i32 %187, 1
  %_6.i.i.i.i.1 = zext i8 %b.i.i.i.1 to i32
  %190 = add i32 %189, %_6.i.i.i.i.1
  %191 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 3
  %b.i.i.i.2 = load i8, i8* %188, align 1, !alias.scope !225, !noalias !230
  %192 = shl i32 %190, 1
  %_6.i.i.i.i.2 = zext i8 %b.i.i.i.2 to i32
  %193 = add i32 %192, %_6.i.i.i.i.2
  %194 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 4
  %b.i.i.i.3 = load i8, i8* %191, align 1, !alias.scope !225, !noalias !230
  %195 = shl i32 %193, 1
  %_6.i.i.i.i.3 = zext i8 %b.i.i.i.3 to i32
  %196 = add i32 %195, %_6.i.i.i.i.3
  %197 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 5
  %b.i.i.i.4 = load i8, i8* %194, align 1, !alias.scope !225, !noalias !230
  %198 = shl i32 %196, 1
  %_6.i.i.i.i.4 = zext i8 %b.i.i.i.4 to i32
  %199 = add i32 %198, %_6.i.i.i.i.4
  %200 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 6
  %b.i.i.i.5 = load i8, i8* %197, align 1, !alias.scope !225, !noalias !230
  %201 = shl i32 %199, 1
  %_6.i.i.i.i.5 = zext i8 %b.i.i.i.5 to i32
  %202 = add i32 %201, %_6.i.i.i.i.5
  %203 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 7
  %b.i.i.i.6 = load i8, i8* %200, align 1, !alias.scope !225, !noalias !230
  %204 = shl i32 %202, 1
  %_6.i.i.i.i.6 = zext i8 %b.i.i.i.6 to i32
  %205 = add i32 %204, %_6.i.i.i.i.6
  %206 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 8
  %b.i.i.i.7 = load i8, i8* %203, align 1, !alias.scope !225, !noalias !230
  %207 = shl i32 %205, 1
  %_6.i.i.i.i.7 = zext i8 %b.i.i.i.7 to i32
  %208 = add i32 %207, %_6.i.i.i.i.7
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %bb6.i.i.preheader.loopexit.unr-lcssa, label %bb5.i.i.i

bb6.i.i.preheader.loopexit.unr-lcssa:             ; preds = %bb5.i.i.i, %bb5.i.i.i.preheader
  %.lcssa226.ph = phi i32 [ undef, %bb5.i.i.i.preheader ], [ %208, %bb5.i.i.i ]
  %hash.011.i.i.i.unr = phi i32 [ 0, %bb5.i.i.i.preheader ], [ %208, %bb5.i.i.i ]
  %iter.sroa.0.010.i.i.i.unr = phi i8* [ %4, %bb5.i.i.i.preheader ], [ %206, %bb5.i.i.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb6.i.i.preheader, label %bb5.i.i.i.epil

bb5.i.i.i.epil:                                   ; preds = %bb6.i.i.preheader.loopexit.unr-lcssa, %bb5.i.i.i.epil
  %hash.011.i.i.i.epil = phi i32 [ %211, %bb5.i.i.i.epil ], [ %hash.011.i.i.i.unr, %bb6.i.i.preheader.loopexit.unr-lcssa ]
  %iter.sroa.0.010.i.i.i.epil = phi i8* [ %209, %bb5.i.i.i.epil ], [ %iter.sroa.0.010.i.i.i.unr, %bb6.i.i.preheader.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb5.i.i.i.epil ], [ %xtraiter, %bb6.i.i.preheader.loopexit.unr-lcssa ]
  %209 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i.epil, i64 1
  %b.i.i.i.epil = load i8, i8* %iter.sroa.0.010.i.i.i.epil, align 1, !alias.scope !225, !noalias !230
  %210 = shl i32 %hash.011.i.i.i.epil, 1
  %_6.i.i.i.i.epil = zext i8 %b.i.i.i.epil to i32
  %211 = add i32 %210, %_6.i.i.i.i.epil
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %bb6.i.i.preheader, label %bb5.i.i.i.epil, !llvm.loop !232

bb6.i.i.preheader:                                ; preds = %bb6.i.i.preheader.loopexit.unr-lcssa, %bb5.i.i.i.epil, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i"
  %hash.0.i.i.ph = phi i32 [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i" ], [ %.lcssa226.ph, %bb6.i.i.preheader.loopexit.unr-lcssa ], [ %211, %bb5.i.i.i.epil ]
  br label %bb6.i.i

bb6.i.i:                                          ; preds = %bb6.i.i.preheader, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i"
  %hash.0.i.i = phi i32 [ %218, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i" ], [ %hash.0.i.i.ph, %bb6.i.i.preheader ]
  %haystack.sroa.13.0.i.i = phi i64 [ %_7.i.i.i.i.i.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i" ], [ %_7.i.i.i.i, %bb6.i.i.preheader ]
  %haystack.sroa.0.0.i.i = phi [0 x i8]* [ %220, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i" ], [ %5, %bb6.i.i.preheader ]
  %212 = icmp eq i32 %hash.0.i.i, %_35.idx.val.i
  br i1 %212, label %bb8.i.i, label %bb14.i.i

bb8.i.i:                                          ; preds = %bb6.i.i
; call memchr::memmem::rabinkarp::is_prefix
  %_24.i.i = tail call fastcc zeroext i1 @_ZN6memchr6memmem9rabinkarp9is_prefix17h9e377cf0e3b7406bE([0 x i8]* noalias nonnull readonly align 1 %haystack.sroa.0.0.i.i, i64 %haystack.sroa.13.0.i.i, [0 x i8]* noalias nonnull readonly align 1 %7, i64 %9), !noalias !167
  br i1 %_24.i.i, label %bb12.i.i, label %bb14.i.i

bb14.i.i:                                         ; preds = %bb8.i.i, %bb6.i.i
  %_32.not.i.i = icmp ugt i64 %haystack.sroa.13.0.i.i, %9
  br i1 %_32.not.i.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i", label %bb11

bb12.i.i:                                         ; preds = %bb8.i.i
  %_28.i.i = ptrtoint [0 x i8]* %haystack.sroa.0.0.i.i to i64
  %_27.i.i = sub i64 %_28.i.i, %start1.i.i
  br label %bb5

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i": ; preds = %bb14.i.i
  %213 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i.i, i64 0, i64 0
  %_41.i.i = load i8, i8* %213, align 1, !alias.scope !233, !noalias !230
  %214 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i.i, i64 0, i64 %9
  %_45.i.i = load i8, i8* %214, align 1, !alias.scope !233, !noalias !230
  %_8.i.i.i.i = zext i8 %_41.i.i to i32
  %215 = mul i32 %_35.idx27.val.i, %_8.i.i.i.i
  %216 = sub i32 %hash.0.i.i, %215
  %217 = shl i32 %216, 1
  %_6.i1.i.i.i = zext i8 %_45.i.i to i32
  %218 = add i32 %217, %_6.i1.i.i.i
  %219 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i.i, i64 0, i64 1
  %_7.i.i.i.i.i.i = add i64 %haystack.sroa.13.0.i.i, -1
  %220 = bitcast i8* %219 to [0 x i8]*
  br label %bb6.i.i

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i37.i": ; preds = %bb9.i
  %_20.idx.i = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1, i32 0, i32 3, i32 0, i32 0
  %_20.idx.val.i = load i32, i32* %_20.idx.i, align 8, !alias.scope !143, !noalias !155
  %_20.idx26.i = getelementptr %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1, i32 0, i32 3, i32 0, i32 1
  %_20.idx26.val.i = load i32, i32* %_20.idx26.i, align 4, !alias.scope !143, !noalias !155
  %start1.i35.i = ptrtoint i8* %4 to i64
  %_12.i9.i.i36.i = icmp eq i64 %9, 0
  br i1 %_12.i9.i.i36.i, label %bb6.i52.i.preheader, label %bb5.i.i44.i.preheader

bb5.i.i44.i.preheader:                            ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i37.i"
  %221 = add i64 %9, -1
  %xtraiter256 = and i64 %9, 7
  %222 = icmp ult i64 %221, 7
  br i1 %222, label %bb6.i52.i.preheader.loopexit.unr-lcssa, label %bb5.i.i44.i.preheader.new

bb5.i.i44.i.preheader.new:                        ; preds = %bb5.i.i44.i.preheader
  %unroll_iter260 = and i64 %9, -8
  br label %bb5.i.i44.i

bb5.i.i44.i:                                      ; preds = %bb5.i.i44.i, %bb5.i.i44.i.preheader.new
  %hash.011.i.i39.i = phi i32 [ 0, %bb5.i.i44.i.preheader.new ], [ %246, %bb5.i.i44.i ]
  %iter.sroa.0.010.i.i40.i = phi i8* [ %4, %bb5.i.i44.i.preheader.new ], [ %244, %bb5.i.i44.i ]
  %niter261 = phi i64 [ %unroll_iter260, %bb5.i.i44.i.preheader.new ], [ %niter261.nsub.7, %bb5.i.i44.i ]
  %223 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 1
  %b.i.i41.i = load i8, i8* %iter.sroa.0.010.i.i40.i, align 1, !alias.scope !234, !noalias !239
  %224 = shl i32 %hash.011.i.i39.i, 1
  %_6.i.i.i42.i = zext i8 %b.i.i41.i to i32
  %225 = add i32 %224, %_6.i.i.i42.i
  %226 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 2
  %b.i.i41.i.1 = load i8, i8* %223, align 1, !alias.scope !234, !noalias !239
  %227 = shl i32 %225, 1
  %_6.i.i.i42.i.1 = zext i8 %b.i.i41.i.1 to i32
  %228 = add i32 %227, %_6.i.i.i42.i.1
  %229 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 3
  %b.i.i41.i.2 = load i8, i8* %226, align 1, !alias.scope !234, !noalias !239
  %230 = shl i32 %228, 1
  %_6.i.i.i42.i.2 = zext i8 %b.i.i41.i.2 to i32
  %231 = add i32 %230, %_6.i.i.i42.i.2
  %232 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 4
  %b.i.i41.i.3 = load i8, i8* %229, align 1, !alias.scope !234, !noalias !239
  %233 = shl i32 %231, 1
  %_6.i.i.i42.i.3 = zext i8 %b.i.i41.i.3 to i32
  %234 = add i32 %233, %_6.i.i.i42.i.3
  %235 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 5
  %b.i.i41.i.4 = load i8, i8* %232, align 1, !alias.scope !234, !noalias !239
  %236 = shl i32 %234, 1
  %_6.i.i.i42.i.4 = zext i8 %b.i.i41.i.4 to i32
  %237 = add i32 %236, %_6.i.i.i42.i.4
  %238 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 6
  %b.i.i41.i.5 = load i8, i8* %235, align 1, !alias.scope !234, !noalias !239
  %239 = shl i32 %237, 1
  %_6.i.i.i42.i.5 = zext i8 %b.i.i41.i.5 to i32
  %240 = add i32 %239, %_6.i.i.i42.i.5
  %241 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 7
  %b.i.i41.i.6 = load i8, i8* %238, align 1, !alias.scope !234, !noalias !239
  %242 = shl i32 %240, 1
  %_6.i.i.i42.i.6 = zext i8 %b.i.i41.i.6 to i32
  %243 = add i32 %242, %_6.i.i.i42.i.6
  %244 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 8
  %b.i.i41.i.7 = load i8, i8* %241, align 1, !alias.scope !234, !noalias !239
  %245 = shl i32 %243, 1
  %_6.i.i.i42.i.7 = zext i8 %b.i.i41.i.7 to i32
  %246 = add i32 %245, %_6.i.i.i42.i.7
  %niter261.nsub.7 = add i64 %niter261, -8
  %niter261.ncmp.7 = icmp eq i64 %niter261.nsub.7, 0
  br i1 %niter261.ncmp.7, label %bb6.i52.i.preheader.loopexit.unr-lcssa, label %bb5.i.i44.i

bb6.i52.i.preheader.loopexit.unr-lcssa:           ; preds = %bb5.i.i44.i, %bb5.i.i44.i.preheader
  %.lcssa223.ph = phi i32 [ undef, %bb5.i.i44.i.preheader ], [ %246, %bb5.i.i44.i ]
  %hash.011.i.i39.i.unr = phi i32 [ 0, %bb5.i.i44.i.preheader ], [ %246, %bb5.i.i44.i ]
  %iter.sroa.0.010.i.i40.i.unr = phi i8* [ %4, %bb5.i.i44.i.preheader ], [ %244, %bb5.i.i44.i ]
  %lcmp.mod258.not = icmp eq i64 %xtraiter256, 0
  br i1 %lcmp.mod258.not, label %bb6.i52.i.preheader, label %bb5.i.i44.i.epil

bb5.i.i44.i.epil:                                 ; preds = %bb6.i52.i.preheader.loopexit.unr-lcssa, %bb5.i.i44.i.epil
  %hash.011.i.i39.i.epil = phi i32 [ %249, %bb5.i.i44.i.epil ], [ %hash.011.i.i39.i.unr, %bb6.i52.i.preheader.loopexit.unr-lcssa ]
  %iter.sroa.0.010.i.i40.i.epil = phi i8* [ %247, %bb5.i.i44.i.epil ], [ %iter.sroa.0.010.i.i40.i.unr, %bb6.i52.i.preheader.loopexit.unr-lcssa ]
  %epil.iter257 = phi i64 [ %epil.iter257.sub, %bb5.i.i44.i.epil ], [ %xtraiter256, %bb6.i52.i.preheader.loopexit.unr-lcssa ]
  %247 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i.epil, i64 1
  %b.i.i41.i.epil = load i8, i8* %iter.sroa.0.010.i.i40.i.epil, align 1, !alias.scope !234, !noalias !239
  %248 = shl i32 %hash.011.i.i39.i.epil, 1
  %_6.i.i.i42.i.epil = zext i8 %b.i.i41.i.epil to i32
  %249 = add i32 %248, %_6.i.i.i42.i.epil
  %epil.iter257.sub = add i64 %epil.iter257, -1
  %epil.iter257.cmp.not = icmp eq i64 %epil.iter257.sub, 0
  br i1 %epil.iter257.cmp.not, label %bb6.i52.i.preheader, label %bb5.i.i44.i.epil, !llvm.loop !241

bb6.i52.i.preheader:                              ; preds = %bb6.i52.i.preheader.loopexit.unr-lcssa, %bb5.i.i44.i.epil, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i37.i"
  %hash.0.i49.i.ph = phi i32 [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i37.i" ], [ %.lcssa223.ph, %bb6.i52.i.preheader.loopexit.unr-lcssa ], [ %249, %bb5.i.i44.i.epil ]
  br label %bb6.i52.i

bb6.i52.i:                                        ; preds = %bb6.i52.i.preheader, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i"
  %hash.0.i49.i = phi i32 [ %256, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i" ], [ %hash.0.i49.i.ph, %bb6.i52.i.preheader ]
  %haystack.sroa.13.0.i50.i = phi i64 [ %_7.i.i.i.i.i64.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i" ], [ %_7.i.i.i.i, %bb6.i52.i.preheader ]
  %haystack.sroa.0.0.i51.i = phi [0 x i8]* [ %258, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i" ], [ %5, %bb6.i52.i.preheader ]
  %250 = icmp eq i32 %hash.0.i49.i, %_20.idx.val.i
  br i1 %250, label %bb8.i54.i, label %bb14.i56.i

bb8.i54.i:                                        ; preds = %bb6.i52.i
; call memchr::memmem::rabinkarp::is_prefix
  %_24.i53.i = tail call fastcc zeroext i1 @_ZN6memchr6memmem9rabinkarp9is_prefix17h9e377cf0e3b7406bE([0 x i8]* noalias nonnull readonly align 1 %haystack.sroa.0.0.i51.i, i64 %haystack.sroa.13.0.i50.i, [0 x i8]* noalias nonnull readonly align 1 %7, i64 %9), !noalias !167
  br i1 %_24.i53.i, label %bb12.i59.i, label %bb14.i56.i

bb14.i56.i:                                       ; preds = %bb8.i54.i, %bb6.i52.i
  %_32.not.i55.i = icmp ugt i64 %haystack.sroa.13.0.i50.i, %9
  br i1 %_32.not.i55.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i", label %bb11

bb12.i59.i:                                       ; preds = %bb8.i54.i
  %_28.i57.i = ptrtoint [0 x i8]* %haystack.sroa.0.0.i51.i to i64
  %_27.i58.i = sub i64 %_28.i57.i, %start1.i35.i
  br label %bb5

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i": ; preds = %bb14.i56.i
  %251 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i51.i, i64 0, i64 0
  %_41.i60.i = load i8, i8* %251, align 1, !alias.scope !242, !noalias !239
  %252 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i51.i, i64 0, i64 %9
  %_45.i61.i = load i8, i8* %252, align 1, !alias.scope !242, !noalias !239
  %_8.i.i.i62.i = zext i8 %_41.i60.i to i32
  %253 = mul i32 %_20.idx26.val.i, %_8.i.i.i62.i
  %254 = sub i32 %hash.0.i49.i, %253
  %255 = shl i32 %254, 1
  %_6.i1.i.i63.i = zext i8 %_45.i61.i to i32
  %256 = add i32 %255, %_6.i1.i.i63.i
  %257 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i51.i, i64 0, i64 1
  %_7.i.i.i.i.i64.i = add i64 %haystack.sroa.13.0.i50.i, -1
  %258 = bitcast i8* %257 to [0 x i8]*
  br label %bb6.i52.i

_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit: ; preds = %bb9.i
  %tw.i = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1, i32 0, i32 2, i32 1, i64 7
  %259 = bitcast i8* %tw.i to %"memmem::twoway::Forward"*
; call memchr::memmem::Searcher::find_tw
  %260 = tail call fastcc { i64, i64 } @_ZN6memchr6memmem8Searcher7find_tw17h619e5351c4063bc3E(%"memmem::Searcher"* noalias nonnull readonly align 8 dereferenceable(80) %_7, %"memmem::twoway::Forward"* noalias nonnull readonly align 8 dereferenceable(32) %259, { i32, i32 }* noalias nonnull align 4 dereferenceable(8) %_9, [0 x i8]* noalias nonnull readonly align 1 %5, i64 %_7.i.i.i.i, [0 x i8]* noalias nonnull readonly align 1 %7, i64 %9)
  %.fca.0.extract18.i = extractvalue { i64, i64 } %260, 0
  %switch = icmp eq i64 %.fca.0.extract18.i, 0
  br i1 %switch, label %bb11, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit.bb5_crit_edge

_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit.bb5_crit_edge: ; preds = %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit
  %.fca.1.extract20.i = extractvalue { i64, i64 } %260, 1
  %_19.pre = load i64, i64* %0, align 8
  %.pre = load i64, i64* %8, align 8, !alias.scope !243
  br label %bb5

bb11:                                             ; preds = %bb19.i81.i.i.i.i, %bb10.i75.i.i.i.i, %bb19.us53.i67.i.i.i.i, %bb10.us48.i54.i.i.i.i, %bb19.loopexit.us.i49.i.i.i.i, %bb10.us.i39.i.i.i.i, %bb14.i.i, %bb14.i56.i, %bb10.i.i.i.i32.i, %bb25.i.i.i.i, %bb22.i.i.i.i, %bb21.i.i.i.i, %bb66.i.i.i.i.i, %bb65.i.i.i.i.i, %bb7.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit", %bb5, %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, %start
  %.sroa.4.0 = phi i64 [ undef, %start ], [ %pos, %bb5 ], [ undef, %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit ], [ undef, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit" ], [ undef, %bb7.i ], [ undef, %bb65.i.i.i.i.i ], [ undef, %bb66.i.i.i.i.i ], [ undef, %bb21.i.i.i.i ], [ undef, %bb22.i.i.i.i ], [ undef, %bb25.i.i.i.i ], [ undef, %bb10.i.i.i.i32.i ], [ undef, %bb14.i56.i ], [ undef, %bb14.i.i ], [ undef, %bb10.us.i39.i.i.i.i ], [ undef, %bb19.loopexit.us.i49.i.i.i.i ], [ undef, %bb10.us48.i54.i.i.i.i ], [ undef, %bb19.us53.i67.i.i.i.i ], [ undef, %bb10.i75.i.i.i.i ], [ undef, %bb19.i81.i.i.i.i ]
  %.sroa.0.0 = phi i64 [ 0, %start ], [ 1, %bb5 ], [ 0, %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit ], [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit" ], [ 0, %bb7.i ], [ 0, %bb65.i.i.i.i.i ], [ 0, %bb66.i.i.i.i.i ], [ 0, %bb21.i.i.i.i ], [ 0, %bb22.i.i.i.i ], [ 0, %bb25.i.i.i.i ], [ 0, %bb10.i.i.i.i32.i ], [ 0, %bb14.i56.i ], [ 0, %bb14.i.i ], [ 0, %bb10.us.i39.i.i.i.i ], [ 0, %bb19.loopexit.us.i49.i.i.i.i ], [ 0, %bb10.us48.i54.i.i.i.i ], [ 0, %bb19.us53.i67.i.i.i.i ], [ 0, %bb10.i75.i.i.i.i ], [ 0, %bb19.i81.i.i.i.i ]
  %261 = insertvalue { i64, i64 } undef, i64 %.sroa.0.0, 0
  %262 = insertvalue { i64, i64 } %261, i64 %.sroa.4.0, 1
  ret { i64, i64 } %262

bb5:                                              ; preds = %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit.bb5_crit_edge, %bb12.i59.i, %bb12.i.i, %bb34.sink.split.i.i.i.i, %bb4.i59.i.i.i.i.i, %bb42.i.i.i.i.i, %bb46.i.i.i.i.i, %bb50.i.i.i.i.i, %bb52.i.i.i.i.i, %bb62.i.i.i.i.i, %bb15.i.i.i.i.i, %bb8.i.i.i.i.i, %bb3.i
  %263 = phi i64 [ %.pre, %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit.bb5_crit_edge ], [ %9, %bb12.i59.i ], [ %9, %bb12.i.i ], [ %9, %bb34.sink.split.i.i.i.i ], [ %9, %bb4.i59.i.i.i.i.i ], [ %9, %bb42.i.i.i.i.i ], [ %9, %bb46.i.i.i.i.i ], [ %9, %bb50.i.i.i.i.i ], [ %9, %bb52.i.i.i.i.i ], [ %9, %bb62.i.i.i.i.i ], [ %9, %bb15.i.i.i.i.i ], [ %9, %bb8.i.i.i.i.i ], [ %9, %bb3.i ]
  %_19 = phi i64 [ %_19.pre, %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit.bb5_crit_edge ], [ %_3, %bb12.i59.i ], [ %_3, %bb12.i.i ], [ %_3, %bb34.sink.split.i.i.i.i ], [ %_3, %bb4.i59.i.i.i.i.i ], [ %_3, %bb42.i.i.i.i.i ], [ %_3, %bb46.i.i.i.i.i ], [ %_3, %bb50.i.i.i.i.i ], [ %_3, %bb52.i.i.i.i.i ], [ %_3, %bb62.i.i.i.i.i ], [ %_3, %bb15.i.i.i.i.i ], [ %_3, %bb8.i.i.i.i.i ], [ %_3, %bb3.i ]
  %.sroa.10.0.i13 = phi i64 [ %.fca.1.extract20.i, %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit.bb5_crit_edge ], [ %_27.i58.i, %bb12.i59.i ], [ %_27.i.i, %bb12.i.i ], [ %_58.i.i.i.i, %bb34.sink.split.i.i.i.i ], [ %_13.i58.i.i.i.i.i, %bb4.i59.i.i.i.i.i ], [ %_102.i.i.i.i.i, %bb42.i.i.i.i.i ], [ %_109.i.i.i.i.i, %bb46.i.i.i.i.i ], [ %_116.i.i.i.i.i, %bb50.i.i.i.i.i ], [ %_122.i.i.i.i.i, %bb52.i.i.i.i.i ], [ %_13.i.i.i.i.i.i, %bb62.i.i.i.i.i ], [ %23, %bb15.i.i.i.i.i ], [ %21, %bb8.i.i.i.i.i ], [ %_11.i, %bb3.i ]
  %pos = add i64 %_19, %.sroa.10.0.i13
  %264 = icmp eq i64 %263, 0
  %.0.sroa.speculated.i.i.i = select i1 %264, i64 1, i64 %263
  %265 = add i64 %.0.sroa.speculated.i.i.i, %pos
  store i64 %265, i64* %0, align 8
  br label %bb11

bb7.i.us.i43.i.i.i.i.1:                           ; preds = %bb9.i.us.i47.i.i.i.i
  br i1 %exitcond.not.i.us.i42.i.i.i.i.1, label %bb34.sink.split.i.i.i.i, label %bb9.i.us.i47.i.i.i.i.1

bb9.i.us.i47.i.i.i.i.1:                           ; preds = %bb7.i.us.i43.i.i.i.i.1
  %266 = getelementptr inbounds i8, i8* %166, i64 1
  %b1.i.us.i44.i.i.i.i.1 = load i8, i8* %163, align 1, !alias.scope !221, !noalias !222
  %b2.i.us.i45.i.i.i.i.1 = load i8, i8* %266, align 1, !alias.scope !223, !noalias !224
  %_23.not.i.us.i46.i.i.i.i.1 = icmp eq i8 %b1.i.us.i44.i.i.i.i.1, %b2.i.us.i45.i.i.i.i.1
  br i1 %_23.not.i.us.i46.i.i.i.i.1, label %bb7.i.us.i43.i.i.i.i.2, label %bb19.loopexit.us.i49.i.i.i.i

bb7.i.us.i43.i.i.i.i.2:                           ; preds = %bb9.i.us.i47.i.i.i.i.1
  br i1 %exitcond.not.i.us.i42.i.i.i.i.2, label %bb34.sink.split.i.i.i.i, label %bb9.i.us.i47.i.i.i.i.2

bb9.i.us.i47.i.i.i.i.2:                           ; preds = %bb7.i.us.i43.i.i.i.i.2
  %267 = getelementptr inbounds i8, i8* %166, i64 2
  %b1.i.us.i44.i.i.i.i.2 = load i8, i8* %164, align 1, !alias.scope !221, !noalias !222
  %b2.i.us.i45.i.i.i.i.2 = load i8, i8* %267, align 1, !alias.scope !223, !noalias !224
  %_23.not.i.us.i46.i.i.i.i.2 = icmp eq i8 %b1.i.us.i44.i.i.i.i.2, %b2.i.us.i45.i.i.i.i.2
  %_23.not.i.us.i46.i.i.i.i.2.not = xor i1 %_23.not.i.us.i46.i.i.i.i.2, true
  %exitcond.not.i.us.i42.i.i.i.i.3.not = xor i1 %exitcond.not.i.us.i42.i.i.i.i.3, true
  %brmerge = select i1 %_23.not.i.us.i46.i.i.i.i.2.not, i1 true, i1 %exitcond.not.i.us.i42.i.i.i.i.3.not
  br i1 %brmerge, label %bb19.loopexit.us.i49.i.i.i.i, label %bb34.sink.split.i.i.i.i
}

; <memchr::memmem::FindRevIter as core::iter::traits::iterator::Iterator>::next
; Function Attrs: nonlazybind uwtable
define { i64, i64 } @"_ZN86_$LT$memchr..memmem..FindRevIter$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h3a195f95dab8c542E"(%"memmem::FindRevIter"* noalias nocapture align 8 dereferenceable(96) %self) unnamed_addr #6 personality i32 (...)* @rust_eh_personality {
start:
  %0 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 2, i32 0
  %_3 = load i64, i64* %0, align 8, !range !252
  %switch.not = icmp eq i64 %_3, 1
  br i1 %switch.not, label %bb1, label %bb14

bb1:                                              ; preds = %start
  %1 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 2, i32 1
  %pos = load i64, i64* %1, align 8
  %2 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 0, i32 0
  %_9.0 = load [0 x i8]*, [0 x i8]** %2, align 8, !nonnull !142
  %3 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 0, i32 1
  %_9.1 = load i64, i64* %3, align 8
  %_8.i.i.i = icmp ugt i64 %pos, %_9.1
  br i1 %_8.i.i.i, label %bb3.i.i.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit"

bb3.i.i.i:                                        ; preds = %bb1
; call core::slice::index::slice_end_index_len_fail
  tail call void @_ZN4core5slice5index24slice_end_index_len_fail17h7d511eec41d6bce9E(i64 %pos, i64 %_9.1, %"core::panic::location::Location"* noalias nonnull readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1181 to %"core::panic::location::Location"*)) #22, !noalias !253
  unreachable

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit": ; preds = %bb1
  %_6 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1
  tail call void @llvm.experimental.noalias.scope.decl(metadata !260)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !263)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !265)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !268)
  %4 = bitcast %"memmem::FinderRev"* %_6 to [0 x i8]**
  %5 = load [0 x i8]*, [0 x i8]** %4, align 8, !alias.scope !270, !noalias !277, !nonnull !142
  %6 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1, i32 0, i32 0, i32 1
  %7 = load i64, i64* %6, align 8, !alias.scope !270, !noalias !277
  %_5.i.i = icmp ult i64 %pos, %7
  br i1 %_5.i.i, label %bb14, label %bb3.i.i

bb3.i.i:                                          ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit"
  %8 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1, i32 0, i32 1, i32 0
  %9 = load i8, i8* %8, align 8, !range !278, !alias.scope !279, !noalias !277
  %_10.i.i = zext i8 %9 to i64
  switch i64 %_10.i.i, label %bb5.i.i [
    i64 0, label %bb9
    i64 1, label %bb7.i.i
    i64 2, label %bb4.i.i
  ]

bb5.i.i:                                          ; preds = %bb3.i.i
  unreachable

bb7.i.i:                                          ; preds = %bb3.i.i
  %10 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1, i32 0, i32 1, i32 1, i64 0
  %b.i.i = load i8, i8* %10, align 1, !alias.scope !279, !noalias !277
  %11 = icmp eq i64 %pos, 0
  br i1 %11, label %bb14, label %bb3.i9.i.i

bb3.i9.i.i:                                       ; preds = %bb7.i.i
  %.0.vec.insert.i.i.i.i.i.i.i.i = insertelement <16 x i8> undef, i8 %b.i.i, i32 0
  %.15.vec.insert.i.i.i.i.i.i.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i.i.i.i.i.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %12 = icmp ult i64 %pos, 64
  %.0.sroa.speculated.i.i.i.i.i.i.i.i.i = select i1 %12, i64 %pos, i64 64
  %13 = getelementptr [0 x i8], [0 x i8]* %_9.0, i64 0, i64 0
  %14 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %pos
  %_17.i.i.i.i.i.i = icmp ult i64 %pos, 16
  br i1 %_17.i.i.i.i.i.i, label %bb6.i.i.i.i.i.i, label %bb13.i.i.i.i.i.i

bb13.i.i.i.i.i.i:                                 ; preds = %bb3.i9.i.i
  %15 = getelementptr inbounds i8, i8* %14, i64 -16
  %16 = bitcast i8* %15 to <16 x i8>*
  %.0.copyload9.i.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %16, align 1, !alias.scope !280, !noalias !279
  %17 = icmp eq <16 x i8> %.0.copyload9.i.i.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i.i
  %18 = bitcast <16 x i1> %17 to i16
  %19 = icmp eq i16 %18, 0
  br i1 %19, label %bb17.i.i.i.i.i.i, label %bb16.i.i.i.i.i.i

bb6.i.i.i.i.i.i:                                  ; preds = %bb3.i9.i.i, %bb7.i.i.i.i.i.i
  %ptr.0.i.i.i.i.i.i = phi i8* [ %20, %bb7.i.i.i.i.i.i ], [ %14, %bb3.i9.i.i ]
  %_20.i.i.i.i.i.i = icmp ugt i8* %ptr.0.i.i.i.i.i.i, %13
  br i1 %_20.i.i.i.i.i.i, label %bb7.i.i.i.i.i.i, label %bb14

bb7.i.i.i.i.i.i:                                  ; preds = %bb6.i.i.i.i.i.i
  %20 = getelementptr inbounds i8, i8* %ptr.0.i.i.i.i.i.i, i64 -1
  %_26.i.i.i.i.i.i = load i8, i8* %20, align 1, !alias.scope !280, !noalias !279
  %_25.i.i.i.i.i.i = icmp eq i8 %_26.i.i.i.i.i.i, %b.i.i
  br i1 %_25.i.i.i.i.i.i, label %bb9.i.i.i.i.i.i, label %bb6.i.i.i.i.i.i

bb9.i.i.i.i.i.i:                                  ; preds = %bb7.i.i.i.i.i.i
  %_3.i.i.i.i.i.i.i = ptrtoint i8* %20 to i64
  %_5.i.i.i.i.i.i.i = ptrtoint [0 x i8]* %_9.0 to i64
  %21 = sub i64 %_3.i.i.i.i.i.i.i, %_5.i.i.i.i.i.i.i
  br label %bb6

bb16.i.i.i.i.i.i:                                 ; preds = %bb13.i.i.i.i.i.i
  %_3.i.i.i.i.i.i.i.i = ptrtoint i8* %15 to i64
  %_5.i1.i.i.i.i.i.i.i = ptrtoint [0 x i8]* %_9.0 to i64
  %22 = sub i64 %_3.i.i.i.i.i.i.i.i, %_5.i1.i.i.i.i.i.i.i
  %23 = tail call i16 @llvm.ctlz.i16(i16 %18, i1 true) #21, !range !44
  %24 = xor i16 %23, 15
  %25 = zext i16 %24 to i64
  %_13.i.i.i.i.i.i.i = add i64 %22, %25
  br label %bb6

bb17.i.i.i.i.i.i:                                 ; preds = %bb13.i.i.i.i.i.i
  %_42.i.i.i.i.i.i = ptrtoint i8* %14 to i64
  %_41.i.i.i.i.i.i = and i64 %_42.i.i.i.i.i.i, -16
  %26 = inttoptr i64 %_41.i.i.i.i.i.i to i8*
  %_46.i.i.i.i.i.i = icmp ugt i64 %pos, 63
  %27 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 64
  %28 = sub nsw i64 0, %.0.sroa.speculated.i.i.i.i.i.i.i.i.i
  br i1 %_46.i.i.i.i.i.i, label %bb18.i.i.i.i.i.i, label %bb59.preheader.i.i.i.i.i.i

bb18.i.i.i.i.i.i:                                 ; preds = %bb17.i.i.i.i.i.i, %bb23.i.i.i.i.i.i
  %ptr.1.i.i.i.i.i.i = phi i8* [ %30, %bb23.i.i.i.i.i.i ], [ %26, %bb17.i.i.i.i.i.i ]
  %_48.not.i.i.i.i.i.i = icmp ult i8* %ptr.1.i.i.i.i.i.i, %27
  br i1 %_48.not.i.i.i.i.i.i, label %bb59.preheader.i.i.i.i.i.i, label %bb23.i.i.i.i.i.i

bb59.preheader.i.i.i.i.i.i:                       ; preds = %bb18.i.i.i.i.i.i, %bb17.i.i.i.i.i.i
  %.us-phi.i.i.i.i.i.i = phi i8* [ %26, %bb17.i.i.i.i.i.i ], [ %ptr.1.i.i.i.i.i.i, %bb18.i.i.i.i.i.i ]
  %29 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 16
  br label %bb59.i.i.i.i.i.i

bb23.i.i.i.i.i.i:                                 ; preds = %bb18.i.i.i.i.i.i
  %30 = getelementptr inbounds i8, i8* %ptr.1.i.i.i.i.i.i, i64 %28
  %31 = bitcast i8* %30 to <16 x i8>*
  %_60.val135.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %31, align 16, !noalias !279
  %32 = getelementptr inbounds i8, i8* %30, i64 16
  %33 = bitcast i8* %32 to <16 x i8>*
  %_63.val136.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %33, align 16, !noalias !279
  %34 = getelementptr inbounds i8, i8* %30, i64 32
  %35 = bitcast i8* %34 to <16 x i8>*
  %_67.val137.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %35, align 16, !noalias !279
  %36 = getelementptr inbounds i8, i8* %30, i64 48
  %37 = bitcast i8* %36 to <16 x i8>*
  %_72.val138.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %37, align 16, !noalias !279
  %38 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i.i.i, %_60.val135.i.i.i.i.i.i
  %39 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i.i.i, %_63.val136.i.i.i.i.i.i
  %40 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i.i.i, %_67.val137.i.i.i.i.i.i
  %41 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i.i.i, %_72.val138.i.i.i.i.i.i
  %42 = or <16 x i1> %39, %38
  %43 = or <16 x i1> %42, %40
  %44 = or <16 x i1> %43, %41
  %45 = bitcast <16 x i1> %44 to i16
  %46 = icmp eq i16 %45, 0
  br i1 %46, label %bb18.i.i.i.i.i.i, label %bb40.i.i.i.i.i.i

bb40.i.i.i.i.i.i:                                 ; preds = %bb23.i.i.i.i.i.i
  %_3.i40.i.i.i.i.i.i = ptrtoint i8* %36 to i64
  %_5.i41.i.i.i.i.i.i = ptrtoint [0 x i8]* %_9.0 to i64
  %47 = sub i64 %_3.i40.i.i.i.i.i.i, %_5.i41.i.i.i.i.i.i
  %48 = bitcast <16 x i1> %41 to i16
  %49 = icmp eq i16 %48, 0
  br i1 %49, label %bb46.i.i.i.i.i.i, label %bb44.i.i.i.i.i.i

bb46.i.i.i.i.i.i:                                 ; preds = %bb40.i.i.i.i.i.i
  %50 = bitcast <16 x i1> %40 to i16
  %51 = icmp eq i16 %50, 0
  br i1 %51, label %bb50.i.i.i.i.i.i, label %bb48.i.i.i.i.i.i

bb44.i.i.i.i.i.i:                                 ; preds = %bb40.i.i.i.i.i.i
  %52 = tail call i16 @llvm.ctlz.i16(i16 %48, i1 true) #21, !range !44
  %53 = xor i16 %52, 15
  %54 = zext i16 %53 to i64
  %_107.i.i.i.i.i.i = add i64 %47, %54
  br label %bb6

bb50.i.i.i.i.i.i:                                 ; preds = %bb46.i.i.i.i.i.i
  %55 = bitcast <16 x i1> %39 to i16
  %56 = icmp eq i16 %55, 0
  br i1 %56, label %bb54.i.i.i.i.i.i, label %bb52.i.i.i.i.i.i

bb48.i.i.i.i.i.i:                                 ; preds = %bb46.i.i.i.i.i.i
  %57 = add i64 %47, -16
  %58 = tail call i16 @llvm.ctlz.i16(i16 %50, i1 true) #21, !range !44
  %59 = xor i16 %58, 15
  %60 = zext i16 %59 to i64
  %_114.i.i.i.i.i.i = add i64 %57, %60
  br label %bb6

bb54.i.i.i.i.i.i:                                 ; preds = %bb50.i.i.i.i.i.i
  %61 = bitcast <16 x i1> %38 to i16
  %62 = tail call i16 @llvm.ctlz.i16(i16 %61, i1 false) #21, !range !44
  %_3.i48.i.i.i.i.i.i = zext i16 %62 to i64
  %63 = add i64 %47, -33
  %_127.i.i.i.i.i.i = sub i64 %63, %_3.i48.i.i.i.i.i.i
  br label %bb6

bb52.i.i.i.i.i.i:                                 ; preds = %bb50.i.i.i.i.i.i
  %64 = add i64 %47, -32
  %65 = tail call i16 @llvm.ctlz.i16(i16 %55, i1 true) #21, !range !44
  %66 = xor i16 %65, 15
  %67 = zext i16 %66 to i64
  %_121.i.i.i.i.i.i = add i64 %64, %67
  br label %bb6

bb59.i.i.i.i.i.i:                                 ; preds = %bb61.i.i.i.i.i.i, %bb59.preheader.i.i.i.i.i.i
  %ptr.2.i.i.i.i.i.i = phi i8* [ %68, %bb61.i.i.i.i.i.i ], [ %.us-phi.i.i.i.i.i.i, %bb59.preheader.i.i.i.i.i.i ]
  %_131.not.i.i.i.i.i.i = icmp ult i8* %ptr.2.i.i.i.i.i.i, %29
  br i1 %_131.not.i.i.i.i.i.i, label %bb66.i.i.i.i.i.i, label %bb61.i.i.i.i.i.i

bb66.i.i.i.i.i.i:                                 ; preds = %bb59.i.i.i.i.i.i
  %_145.i.i.i.i.i.i = icmp ugt i8* %ptr.2.i.i.i.i.i.i, %13
  br i1 %_145.i.i.i.i.i.i, label %bb67.i.i.i.i.i.i, label %bb14

bb61.i.i.i.i.i.i:                                 ; preds = %bb59.i.i.i.i.i.i
  %68 = getelementptr inbounds i8, i8* %ptr.2.i.i.i.i.i.i, i64 -16
  %69 = bitcast i8* %68 to <16 x i8>*
  %.0.copyload9.i50.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %69, align 1, !noalias !279
  %70 = icmp eq <16 x i8> %.0.copyload9.i50.i.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i.i
  %71 = bitcast <16 x i1> %70 to i16
  %72 = icmp eq i16 %71, 0
  br i1 %72, label %bb59.i.i.i.i.i.i, label %bb64.i.i.i.i.i.i

bb64.i.i.i.i.i.i:                                 ; preds = %bb61.i.i.i.i.i.i
  %_3.i.i51.i.i.i.i.i.i = ptrtoint i8* %68 to i64
  %_5.i1.i52.i.i.i.i.i.i = ptrtoint [0 x i8]* %_9.0 to i64
  %73 = sub i64 %_3.i.i51.i.i.i.i.i.i, %_5.i1.i52.i.i.i.i.i.i
  %74 = tail call i16 @llvm.ctlz.i16(i16 %71, i1 true) #21, !range !44
  %75 = xor i16 %74, 15
  %76 = zext i16 %75 to i64
  %_13.i53.i.i.i.i.i.i = add i64 %73, %76
  br label %bb6

bb67.i.i.i.i.i.i:                                 ; preds = %bb66.i.i.i.i.i.i
  %77 = bitcast [0 x i8]* %_9.0 to <16 x i8>*
  %.0.copyload9.i58.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %77, align 1, !alias.scope !280, !noalias !279
  %78 = icmp eq <16 x i8> %.0.copyload9.i58.i.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i.i
  %79 = bitcast <16 x i1> %78 to i16
  %80 = icmp eq i16 %79, 0
  br i1 %80, label %bb14, label %bb4.i61.i.i.i.i.i.i

bb4.i61.i.i.i.i.i.i:                              ; preds = %bb67.i.i.i.i.i.i
  %81 = tail call i16 @llvm.ctlz.i16(i16 %79, i1 true) #21, !range !44
  %82 = xor i16 %81, 15
  %83 = zext i16 %82 to i64
  br label %bb6

bb4.i.i:                                          ; preds = %bb3.i.i
  %84 = icmp ult i64 %pos, 16
  br i1 %84, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i.i", label %bb12.i.i

bb12.i.i:                                         ; preds = %bb4.i.i
  %tw.i.i = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1, i32 0, i32 1, i32 1, i64 7
  tail call void @llvm.experimental.noalias.scope.decl(metadata !289)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !292)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !294)
  %85 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1, i32 0, i32 1, i32 1, i64 23
  %86 = bitcast i8* %85 to i64*
  %_4.i.i.i = load i64, i64* %86, align 8, !range !252, !alias.scope !296, !noalias !297
  %switch.not.i.i.i = icmp eq i64 %_4.i.i.i, 1
  %87 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1, i32 0, i32 1, i32 1, i64 31
  %88 = bitcast i8* %87 to i64*
  %shift.i.i.i = load i64, i64* %88, align 8, !alias.scope !296, !noalias !297
  %self.idx8.i.i.i = bitcast i8* %tw.i.i to i64*
  %self.idx8.val.i.i.i = load i64, i64* %self.idx8.i.i.i, align 8, !alias.scope !296, !noalias !297
  %self.idx9.i.i.i = getelementptr %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1, i32 0, i32 1, i32 1, i64 15
  %89 = bitcast i8* %self.idx9.i.i.i to i64*
  %self.idx9.val.i.i.i = load i64, i64* %89, align 8, !alias.scope !296, !noalias !297
  br i1 %switch.not.i.i.i, label %bb2.lr.ph.i11.i.i.i, label %bb2.lr.ph.i.i.i.i

bb2.lr.ph.i.i.i.i:                                ; preds = %bb12.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !298)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !301)
  %90 = getelementptr inbounds [0 x i8], [0 x i8]* %5, i64 0, i64 0
  %_60.i.i.i.i = add i64 %self.idx9.val.i.i.i, 1
  %_52.not.i.i.i.i = icmp eq i64 %7, 0
  br i1 %_52.not.i.i.i.i, label %panic.i.i.i.i, label %bb2.preheader.i.i.i.i, !prof !303

bb2.preheader.i.i.i.i:                            ; preds = %bb2.lr.ph.i.i.i.i
  %umax.i.i.i.i = tail call i64 @llvm.umax.i64(i64 %7, i64 %self.idx9.val.i.i.i)
  br label %bb2.i.i.i.i

bb2.i.i.i.i:                                      ; preds = %bb1.backedge.i.i.i.i, %bb2.preheader.i.i.i.i
  %pos.024.i.i.i.i = phi i64 [ %pos.0.be.i.i.i.i, %bb1.backedge.i.i.i.i ], [ %pos, %bb2.preheader.i.i.i.i ]
  %shift.023.i.i.i.i = phi i64 [ %shift.0.be.i.i.i.i, %bb1.backedge.i.i.i.i ], [ %7, %bb2.preheader.i.i.i.i ]
  %_17.i.i.i.i = sub i64 %pos.024.i.i.i.i, %7
  %_21.i.i.i.i = icmp ult i64 %_17.i.i.i.i, %pos
  br i1 %_21.i.i.i.i, label %bb3.i.i.i.i, label %panic.i.i.i.i, !prof !60

bb3.i.i.i.i:                                      ; preds = %bb2.i.i.i.i
  %91 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_17.i.i.i.i
  %_16.i.i.i.i = load i8, i8* %91, align 1, !alias.scope !304, !noalias !305
  %92 = and i8 %_16.i.i.i.i, 63
  %93 = zext i8 %92 to i64
  %_5.i.i.i.i.i = shl nuw i64 1, %93
  %_3.i.i.i.i.i = and i64 %_5.i.i.i.i.i, %self.idx8.val.i.i.i
  %.not.i.i.i.i = icmp eq i64 %_3.i.i.i.i.i, 0
  br i1 %.not.i.i.i.i, label %bb1.backedge.i.i.i.i, label %bb6.i.i.i.i

panic.i.i.i.i:                                    ; preds = %bb2.i.i.i.i, %bb2.lr.ph.i.i.i.i
  %.us-phi.i.i.i.i = phi i64 [ %pos, %bb2.lr.ph.i.i.i.i ], [ %_17.i.i.i.i, %bb2.i.i.i.i ]
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %.us-phi.i.i.i.i, i64 %pos, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1125 to %"core::panic::location::Location"*)) #22, !noalias !306
  unreachable

bb6.i.i.i.i:                                      ; preds = %bb3.i.i.i.i
  %94 = icmp ult i64 %shift.023.i.i.i.i, %self.idx9.val.i.i.i
  %.0.sroa.speculated.i.i.i.i.i.i.i = select i1 %94, i64 %shift.023.i.i.i.i, i64 %self.idx9.val.i.i.i
  %95 = add i64 %.0.sroa.speculated.i.i.i.i.i.i.i, -1
  %_3560.i.i.i.i = icmp ult i64 %95, %7
  br i1 %_3560.i.i.i.i, label %bb8.i.us.i.i.i, label %bb6.i.split.i.i.i, !prof !60

bb8.i.us.i.i.i:                                   ; preds = %bb6.i.i.i.i, %bb13.i.us.i.i.i
  %i.0.i.us.i.i.i = phi i64 [ %_32.i.us.i.i.i, %bb13.i.us.i.i.i ], [ %.0.sroa.speculated.i.i.i.i.i.i.i, %bb6.i.i.i.i ]
  %_28.not.i.us.i.i.i = icmp eq i64 %i.0.i.us.i.i.i, 0
  br i1 %_28.not.i.us.i.i.i, label %bb17.i.i.i.i, label %bb10.i.us.i.i.i

bb10.i.us.i.i.i:                                  ; preds = %bb8.i.us.i.i.i
  %_32.i.us.i.i.i = add i64 %i.0.i.us.i.i.i, -1
  %_37.i.us.i.i.i = add i64 %_32.i.us.i.i.i, %_17.i.i.i.i
  %_44.i.us.i.i.i = icmp ult i64 %_37.i.us.i.i.i, %pos
  br i1 %_44.i.us.i.i.i, label %bb13.i.us.i.i.i, label %panic2.i.i.i.i, !prof !60

bb13.i.us.i.i.i:                                  ; preds = %bb10.i.us.i.i.i
  %96 = getelementptr inbounds [0 x i8], [0 x i8]* %5, i64 0, i64 %_32.i.us.i.i.i
  %_31.i.us.i.i.i = load i8, i8* %96, align 1, !alias.scope !307, !noalias !308
  %97 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_37.i.us.i.i.i
  %_36.i.us.i.i.i = load i8, i8* %97, align 1, !alias.scope !304, !noalias !305
  %_30.i.us.i.i.i = icmp eq i8 %_31.i.us.i.i.i, %_36.i.us.i.i.i
  br i1 %_30.i.us.i.i.i, label %bb8.i.us.i.i.i, label %bb21.i.i.i.i

bb6.i.split.i.i.i:                                ; preds = %bb6.i.i.i.i
  %_28.not.i.i.i.i = icmp eq i64 %.0.sroa.speculated.i.i.i.i.i.i.i, 0
  br i1 %_28.not.i.i.i.i, label %bb17.i.i.i.i, label %bb10.i.i.i.i

bb10.i.i.i.i:                                     ; preds = %bb6.i.split.i.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %95, i64 %7, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1127 to %"core::panic::location::Location"*)) #22, !noalias !306
  unreachable

panic2.i.i.i.i:                                   ; preds = %bb10.i.us.i.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_37.i.us.i.i.i, i64 %pos, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1129 to %"core::panic::location::Location"*)) #22, !noalias !306
  unreachable

bb17.i.i.i.i:                                     ; preds = %bb8.i.us.i.i.i, %bb6.i.split.i.i.i
  %_49.i.i.i.i = load i8, i8* %90, align 1, !alias.scope !307, !noalias !308
  %_48.not.i.i.i.i = icmp eq i8 %_49.i.i.i.i, %_16.i.i.i.i
  br i1 %_48.not.i.i.i.i, label %bb23.preheader.i.i.i.i, label %bb21.i.i.i.i

bb23.preheader.i.i.i.i:                           ; preds = %bb17.i.i.i.i
  %_6619.i.i.i.i = icmp ugt i64 %shift.023.i.i.i.i, %self.idx9.val.i.i.i
  br i1 %_6619.i.i.i.i, label %bb25.i.i.i.i, label %bb6

bb21.i.i.i.i:                                     ; preds = %bb13.i.us.i.i.i, %bb17.i.i.i.i
  %i.0.i87.i.i.i = phi i64 [ 0, %bb17.i.i.i.i ], [ %i.0.i.us.i.i.i, %bb13.i.us.i.i.i ]
  %_59.i.i.i.i = sub i64 %_60.i.i.i.i, %i.0.i87.i.i.i
  br label %bb33.i.i.i.i

bb33.i.i.i.i:                                     ; preds = %bb28.i.i.i.i, %bb21.i.i.i.i
  %shift.1.i.i.i.i = phi i64 [ %7, %bb21.i.i.i.i ], [ %shift.i.i.i, %bb28.i.i.i.i ]
  %_59.pn.i.i.i.i = phi i64 [ %_59.i.i.i.i, %bb21.i.i.i.i ], [ %shift.i.i.i, %bb28.i.i.i.i ]
  %pos.1.i.i.i.i = sub i64 %pos.024.i.i.i.i, %_59.pn.i.i.i.i
  br label %bb1.backedge.i.i.i.i

bb1.backedge.i.i.i.i:                             ; preds = %bb33.i.i.i.i, %bb3.i.i.i.i
  %shift.0.be.i.i.i.i = phi i64 [ %shift.1.i.i.i.i, %bb33.i.i.i.i ], [ %7, %bb3.i.i.i.i ]
  %pos.0.be.i.i.i.i = phi i64 [ %pos.1.i.i.i.i, %bb33.i.i.i.i ], [ %_17.i.i.i.i, %bb3.i.i.i.i ]
  %_10.not.i.i.i.i = icmp ult i64 %pos.0.be.i.i.i.i, %7
  br i1 %_10.not.i.i.i.i, label %bb14, label %bb2.i.i.i.i

bb25.i.i.i.i:                                     ; preds = %bb23.preheader.i.i.i.i, %bb29.i.i.i.i
  %j.020.i.i.i.i = phi i64 [ %100, %bb29.i.i.i.i ], [ %self.idx9.val.i.i.i, %bb23.preheader.i.i.i.i ]
  %exitcond.not.i.i.i.i = icmp eq i64 %j.020.i.i.i.i, %umax.i.i.i.i
  br i1 %exitcond.not.i.i.i.i, label %panic5.i.i.i.i, label %bb27.i.i.i.i, !prof !303

bb27.i.i.i.i:                                     ; preds = %bb25.i.i.i.i
  %_75.i.i.i.i = add i64 %j.020.i.i.i.i, %_17.i.i.i.i
  %_81.i.i.i.i = icmp ult i64 %_75.i.i.i.i, %pos
  br i1 %_81.i.i.i.i, label %bb28.i.i.i.i, label %panic6.i.i.i.i, !prof !60

panic5.i.i.i.i:                                   ; preds = %bb25.i.i.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %umax.i.i.i.i, i64 %7, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1135 to %"core::panic::location::Location"*)) #22, !noalias !306
  unreachable

bb28.i.i.i.i:                                     ; preds = %bb27.i.i.i.i
  %98 = getelementptr inbounds [0 x i8], [0 x i8]* %5, i64 0, i64 %j.020.i.i.i.i
  %_70.i.i.i.i = load i8, i8* %98, align 1, !alias.scope !307, !noalias !308
  %99 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_75.i.i.i.i
  %_74.i.i.i.i = load i8, i8* %99, align 1, !alias.scope !304, !noalias !305
  %_69.i.i.i.i = icmp eq i8 %_70.i.i.i.i, %_74.i.i.i.i
  br i1 %_69.i.i.i.i, label %bb29.i.i.i.i, label %bb33.i.i.i.i

panic6.i.i.i.i:                                   ; preds = %bb27.i.i.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_75.i.i.i.i, i64 %pos, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1137 to %"core::panic::location::Location"*)) #22, !noalias !306
  unreachable

bb29.i.i.i.i:                                     ; preds = %bb28.i.i.i.i
  %100 = add i64 %j.020.i.i.i.i, 1
  %exitcond62.not.i.i.i.i = icmp eq i64 %100, %shift.023.i.i.i.i
  br i1 %exitcond62.not.i.i.i.i, label %bb6, label %bb25.i.i.i.i

bb2.lr.ph.i11.i.i.i:                              ; preds = %bb12.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !309)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !312)
  %101 = getelementptr inbounds [0 x i8], [0 x i8]* %5, i64 0, i64 0
  %_56.i.i.i.i = add i64 %self.idx9.val.i.i.i, 1
  %_48.not.i10.i.i.i = icmp eq i64 %7, 0
  br i1 %_48.not.i10.i.i.i, label %panic.i21.i.i.i, label %bb2.lr.ph.split.i.i.i.i, !prof !303

bb2.lr.ph.split.i.i.i.i:                          ; preds = %bb2.lr.ph.i11.i.i.i
  %_6113.i.i.i.i = icmp ult i64 %self.idx9.val.i.i.i, %7
  br i1 %_6113.i.i.i.i, label %bb2.us26.preheader.i.i.i.i, label %bb2.lr.ph.split.split.i.i.i.i

bb2.us26.preheader.i.i.i.i:                       ; preds = %bb2.lr.ph.split.i.i.i.i
  %102 = add i64 %self.idx9.val.i.i.i, -1
  %_31.us40194.i.i.i.i = icmp ult i64 %102, %7
  %_24.not.us37.i.i.i.i = icmp eq i64 %self.idx9.val.i.i.i, 0
  br label %bb2.us26.i.i.i.i

bb2.us26.i.i.i.i:                                 ; preds = %bb1.backedge.us61.i.i.i.i, %bb2.us26.preheader.i.i.i.i
  %pos.017.us27.i.i.i.i = phi i64 [ %pos.0.be.us62.i.i.i.i, %bb1.backedge.us61.i.i.i.i ], [ %pos, %bb2.us26.preheader.i.i.i.i ]
  %_16.us28.i.i.i.i = sub i64 %pos.017.us27.i.i.i.i, %7
  %_20.us29.i.i.i.i = icmp ult i64 %_16.us28.i.i.i.i, %pos
  br i1 %_20.us29.i.i.i.i, label %bb3.us30.i.i.i.i, label %panic.i21.i.i.i, !prof !60

bb3.us30.i.i.i.i:                                 ; preds = %bb2.us26.i.i.i.i
  %103 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_16.us28.i.i.i.i
  %_15.us31.i.i.i.i = load i8, i8* %103, align 1, !alias.scope !314, !noalias !315
  %104 = and i8 %_15.us31.i.i.i.i, 63
  %105 = zext i8 %104 to i64
  %_5.i.us32.i.i.i.i = shl nuw i64 1, %105
  %_3.i.us33.i.i.i.i = and i64 %_5.i.us32.i.i.i.i, %self.idx8.val.i.i.i
  %.not.us34.i.i.i.i = icmp eq i64 %_3.i.us33.i.i.i.i, 0
  br i1 %.not.us34.i.i.i.i, label %bb1.backedge.us61.i.i.i.i, label %bb7.us35.i.preheader.i.i.i

bb7.us35.i.preheader.i.i.i:                       ; preds = %bb3.us30.i.i.i.i
  br i1 %_31.us40194.i.i.i.i, label %bb7.us35.i.us.i.i.i, label %bb7.us35.i.preheader.split.i.i.i, !prof !60

bb7.us35.i.us.i.i.i:                              ; preds = %bb7.us35.i.preheader.i.i.i, %bb12.us45.i.us.i.i.i
  %i.0.us36.i.us.i.i.i = phi i64 [ %_28.us39.i.us.i.i.i, %bb12.us45.i.us.i.i.i ], [ %self.idx9.val.i.i.i, %bb7.us35.i.preheader.i.i.i ]
  %_24.not.us37.i.us.i.i.i = icmp eq i64 %i.0.us36.i.us.i.i.i, 0
  br i1 %_24.not.us37.i.us.i.i.i, label %bb16.us49.i.i.i.i, label %bb9.us38.i.us.i.i.i

bb9.us38.i.us.i.i.i:                              ; preds = %bb7.us35.i.us.i.i.i
  %_28.us39.i.us.i.i.i = add i64 %i.0.us36.i.us.i.i.i, -1
  %_33.us43.i.us.i.i.i = add i64 %_28.us39.i.us.i.i.i, %_16.us28.i.i.i.i
  %_40.us44.i.us.i.i.i = icmp ult i64 %_33.us43.i.us.i.i.i, %pos
  br i1 %_40.us44.i.us.i.i.i, label %bb12.us45.i.us.i.i.i, label %panic2.i26.i.i.i, !prof !60

bb12.us45.i.us.i.i.i:                             ; preds = %bb9.us38.i.us.i.i.i
  %106 = getelementptr inbounds [0 x i8], [0 x i8]* %5, i64 0, i64 %_28.us39.i.us.i.i.i
  %_27.us46.i.us.i.i.i = load i8, i8* %106, align 1, !alias.scope !316, !noalias !317
  %107 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_33.us43.i.us.i.i.i
  %_32.us47.i.us.i.i.i = load i8, i8* %107, align 1, !alias.scope !314, !noalias !315
  %_26.us48.i.us.i.i.i = icmp eq i8 %_27.us46.i.us.i.i.i, %_32.us47.i.us.i.i.i
  br i1 %_26.us48.i.us.i.i.i, label %bb7.us35.i.us.i.i.i, label %bb20.us51.i.i.i.i

bb7.us35.i.preheader.split.i.i.i:                 ; preds = %bb7.us35.i.preheader.i.i.i
  br i1 %_24.not.us37.i.i.i.i, label %bb16.us49.i.i.i.i, label %panic1.i23.i.i.i

bb16.us49.i.i.i.i:                                ; preds = %bb7.us35.i.us.i.i.i, %bb7.us35.i.preheader.split.i.i.i
  %_45.us.i.i.i.i = load i8, i8* %101, align 1, !alias.scope !316, !noalias !317
  %_44.not.us.i.i.i.i = icmp eq i8 %_45.us.i.i.i.i, %_15.us31.i.i.i.i
  br i1 %_44.not.us.i.i.i.i, label %bb26.us.i.i.i.i, label %bb20.us51.i.i.i.i

bb20.us51.i.i.i.i:                                ; preds = %bb12.us45.i.us.i.i.i, %bb16.us49.i.i.i.i
  %i.0.us36.i51.i.i.i = phi i64 [ 0, %bb16.us49.i.i.i.i ], [ %i.0.us36.i.us.i.i.i, %bb12.us45.i.us.i.i.i ]
  %_55.us54.i.i.i.i = sub i64 %_56.i.i.i.i, %i.0.us36.i51.i.i.i
  br label %bb32.us55.i.i.i.i

bb26.us.i.i.i.i:                                  ; preds = %bb16.us49.i.i.i.i, %bb28.us.i.i.i.i
  %j.014.us.i.i.i.i = phi i64 [ %110, %bb28.us.i.i.i.i ], [ %self.idx9.val.i.i.i, %bb16.us49.i.i.i.i ]
  %_70.us.i.i.i.i = add i64 %j.014.us.i.i.i.i, %_16.us28.i.i.i.i
  %_76.us.i.i.i.i = icmp ult i64 %_70.us.i.i.i.i, %pos
  br i1 %_76.us.i.i.i.i, label %bb27.us.i.i.i.i, label %panic6.i30.i.i.i, !prof !60

bb27.us.i.i.i.i:                                  ; preds = %bb26.us.i.i.i.i
  %108 = getelementptr inbounds [0 x i8], [0 x i8]* %5, i64 0, i64 %j.014.us.i.i.i.i
  %_65.us.i.i.i.i = load i8, i8* %108, align 1, !alias.scope !316, !noalias !317
  %109 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_70.us.i.i.i.i
  %_69.us.i.i.i.i = load i8, i8* %109, align 1, !alias.scope !314, !noalias !315
  %_64.us.i.i.i.i = icmp eq i8 %_65.us.i.i.i.i, %_69.us.i.i.i.i
  br i1 %_64.us.i.i.i.i, label %bb28.us.i.i.i.i, label %bb29.us.i.i.i.i

bb29.us.i.i.i.i:                                  ; preds = %bb27.us.i.i.i.i
  %_77.us.i.i.i.i = icmp eq i64 %j.014.us.i.i.i.i, %7
  br i1 %_77.us.i.i.i.i, label %bb6, label %bb32.us55.i.i.i.i

bb32.us55.i.i.i.i:                                ; preds = %bb29.us.i.i.i.i, %bb20.us51.i.i.i.i
  %_55.pn.us56.i.i.i.i = phi i64 [ %_55.us54.i.i.i.i, %bb20.us51.i.i.i.i ], [ %shift.i.i.i, %bb29.us.i.i.i.i ]
  %pos.1.us57.i.i.i.i = sub i64 %pos.017.us27.i.i.i.i, %_55.pn.us56.i.i.i.i
  br label %bb1.backedge.us61.i.i.i.i

bb28.us.i.i.i.i:                                  ; preds = %bb27.us.i.i.i.i
  %110 = add i64 %j.014.us.i.i.i.i, 1
  %exitcond.not.i12.i.i.i = icmp eq i64 %110, %7
  br i1 %exitcond.not.i12.i.i.i, label %bb6, label %bb26.us.i.i.i.i

bb1.backedge.us61.i.i.i.i:                        ; preds = %bb32.us55.i.i.i.i, %bb3.us30.i.i.i.i
  %pos.0.be.us62.i.i.i.i = phi i64 [ %pos.1.us57.i.i.i.i, %bb32.us55.i.i.i.i ], [ %_16.us28.i.i.i.i, %bb3.us30.i.i.i.i ]
  %_9.not.us63.i.i.i.i = icmp ult i64 %pos.0.be.us62.i.i.i.i, %7
  br i1 %_9.not.us63.i.i.i.i, label %bb14, label %bb2.us26.i.i.i.i

bb2.lr.ph.split.split.i.i.i.i:                    ; preds = %bb2.lr.ph.split.i.i.i.i
  %_77.i.i.i.i = icmp eq i64 %self.idx9.val.i.i.i, %7
  br i1 %_77.i.i.i.i, label %bb2.us77.preheader.i.i.i.i, label %bb2.preheader.i13.i.i.i

bb2.preheader.i13.i.i.i:                          ; preds = %bb2.lr.ph.split.split.i.i.i.i
  %111 = add i64 %self.idx9.val.i.i.i, -1
  %_31192.i.i.i.i = icmp ult i64 %111, %7
  br i1 %_31192.i.i.i.i, label %bb2.i15.us.i.i.i, label %bb2.preheader.i13.split.i.i.i, !prof !60

bb2.i15.us.i.i.i:                                 ; preds = %bb2.preheader.i13.i.i.i, %bb1.backedge.i29.us.i.i.i
  %pos.017.i.us.i.i.i = phi i64 [ %pos.0.be.i28.us.i.i.i, %bb1.backedge.i29.us.i.i.i ], [ %pos, %bb2.preheader.i13.i.i.i ]
  %_16.i14.us.i.i.i = sub i64 %pos.017.i.us.i.i.i, %7
  %_20.i.us.i.i.i = icmp ult i64 %_16.i14.us.i.i.i, %pos
  br i1 %_20.i.us.i.i.i, label %bb3.i19.us.i.i.i, label %panic.i21.i.i.i, !prof !60

bb3.i19.us.i.i.i:                                 ; preds = %bb2.i15.us.i.i.i
  %112 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_16.i14.us.i.i.i
  %_15.i.us.i.i.i = load i8, i8* %112, align 1, !alias.scope !314, !noalias !315
  %113 = and i8 %_15.i.us.i.i.i, 63
  %114 = zext i8 %113 to i64
  %_5.i.i16.us.i.i.i = shl nuw i64 1, %114
  %_3.i.i17.us.i.i.i = and i64 %_5.i.i16.us.i.i.i, %self.idx8.val.i.i.i
  %.not.i18.us.i.i.i = icmp eq i64 %_3.i.i17.us.i.i.i, 0
  br i1 %.not.i18.us.i.i.i, label %bb1.backedge.i29.us.i.i.i, label %bb7.i.us.us.i.i.i

bb20.i.us.i.i.i:                                  ; preds = %bb12.i25.us.us.i.i.i, %bb16.i.split.us.us.i.i.i
  %_55.i.us.i.i.i = sub i64 %_56.i.i.i.i, %i.0.i22.us.us.i.i.i
  br label %bb32.i.us.i.i.i

bb32.i.us.i.i.i:                                  ; preds = %bb16.i.split.us.us.i.i.i, %bb20.i.us.i.i.i
  %_55.pn.i.us.i.i.i = phi i64 [ %_55.i.us.i.i.i, %bb20.i.us.i.i.i ], [ %shift.i.i.i, %bb16.i.split.us.us.i.i.i ]
  %pos.1.i27.us.i.i.i = sub i64 %pos.017.i.us.i.i.i, %_55.pn.i.us.i.i.i
  br label %bb1.backedge.i29.us.i.i.i

bb1.backedge.i29.us.i.i.i:                        ; preds = %bb32.i.us.i.i.i, %bb3.i19.us.i.i.i
  %pos.0.be.i28.us.i.i.i = phi i64 [ %pos.1.i27.us.i.i.i, %bb32.i.us.i.i.i ], [ %_16.i14.us.i.i.i, %bb3.i19.us.i.i.i ]
  %_9.not.i.us.i.i.i = icmp ult i64 %pos.0.be.i28.us.i.i.i, %7
  br i1 %_9.not.i.us.i.i.i, label %bb14, label %bb2.i15.us.i.i.i

bb7.i.us.us.i.i.i:                                ; preds = %bb3.i19.us.i.i.i, %bb12.i25.us.us.i.i.i
  %i.0.i22.us.us.i.i.i = phi i64 [ %_28.i.us.us.i.i.i, %bb12.i25.us.us.i.i.i ], [ %self.idx9.val.i.i.i, %bb3.i19.us.i.i.i ]
  %_24.not.i.us.us.i.i.i = icmp eq i64 %i.0.i22.us.us.i.i.i, 0
  br i1 %_24.not.i.us.us.i.i.i, label %bb16.i.split.us.us.i.i.i, label %bb9.i.us.us.i.i.i

bb9.i.us.us.i.i.i:                                ; preds = %bb7.i.us.us.i.i.i
  %_28.i.us.us.i.i.i = add i64 %i.0.i22.us.us.i.i.i, -1
  %_33.i.us.us.i.i.i = add i64 %_28.i.us.us.i.i.i, %_16.i14.us.i.i.i
  %_40.i.us.us.i.i.i = icmp ult i64 %_33.i.us.us.i.i.i, %pos
  br i1 %_40.i.us.us.i.i.i, label %bb12.i25.us.us.i.i.i, label %panic2.i26.i.i.i, !prof !60

bb12.i25.us.us.i.i.i:                             ; preds = %bb9.i.us.us.i.i.i
  %115 = getelementptr inbounds [0 x i8], [0 x i8]* %5, i64 0, i64 %_28.i.us.us.i.i.i
  %_27.i.us.us.i.i.i = load i8, i8* %115, align 1, !alias.scope !316, !noalias !317
  %116 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_33.i.us.us.i.i.i
  %_32.i24.us.us.i.i.i = load i8, i8* %116, align 1, !alias.scope !314, !noalias !315
  %_26.i.us.us.i.i.i = icmp eq i8 %_27.i.us.us.i.i.i, %_32.i24.us.us.i.i.i
  br i1 %_26.i.us.us.i.i.i, label %bb7.i.us.us.i.i.i, label %bb20.i.us.i.i.i

bb16.i.split.us.us.i.i.i:                         ; preds = %bb7.i.us.us.i.i.i
  %_45.i.us.i.i.i = load i8, i8* %101, align 1, !alias.scope !316, !noalias !317
  %_44.not.i.us.i.i.i = icmp eq i8 %_45.i.us.i.i.i, %_15.i.us.i.i.i
  br i1 %_44.not.i.us.i.i.i, label %bb32.i.us.i.i.i, label %bb20.i.us.i.i.i

bb2.preheader.i13.split.i.i.i:                    ; preds = %bb2.preheader.i13.i.i.i
  %_24.not.i.i.i.i = icmp eq i64 %self.idx9.val.i.i.i, 0
  br i1 %_24.not.i.i.i.i, label %bb2.i15.us110.i.i.i, label %bb2.i15.i.i.i

bb2.i15.us110.i.i.i:                              ; preds = %bb2.preheader.i13.split.i.i.i, %bb1.backedge.i29.us125.i.i.i
  %pos.017.i.us111.i.i.i = phi i64 [ %pos.0.be.i28.us126.i.i.i, %bb1.backedge.i29.us125.i.i.i ], [ %pos, %bb2.preheader.i13.split.i.i.i ]
  %_16.i14.us112.i.i.i = sub i64 %pos.017.i.us111.i.i.i, %7
  %_20.i.us113.i.i.i = icmp ult i64 %_16.i14.us112.i.i.i, %pos
  br i1 %_20.i.us113.i.i.i, label %bb3.i19.us114.i.i.i, label %panic.i21.i.i.i, !prof !60

bb3.i19.us114.i.i.i:                              ; preds = %bb2.i15.us110.i.i.i
  %117 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_16.i14.us112.i.i.i
  %_15.i.us115.i.i.i = load i8, i8* %117, align 1, !alias.scope !314, !noalias !315
  %118 = and i8 %_15.i.us115.i.i.i, 63
  %119 = zext i8 %118 to i64
  %_5.i.i16.us116.i.i.i = shl nuw i64 1, %119
  %_3.i.i17.us117.i.i.i = and i64 %_5.i.i16.us116.i.i.i, %self.idx8.val.i.i.i
  %.not.i18.us118.i.i.i = icmp eq i64 %_3.i.i17.us117.i.i.i, 0
  br i1 %.not.i18.us118.i.i.i, label %bb1.backedge.i29.us125.i.i.i, label %bb7.i.preheader.us128.i.i.i

bb1.backedge.i29.us125.i.i.i:                     ; preds = %bb7.i.preheader.us128.i.i.i, %bb3.i19.us114.i.i.i
  %pos.0.be.i28.us126.i.i.i = phi i64 [ %pos.1.i27.us124.i.i.i, %bb7.i.preheader.us128.i.i.i ], [ %_16.i14.us112.i.i.i, %bb3.i19.us114.i.i.i ]
  %_9.not.i.us127.i.i.i = icmp ult i64 %pos.0.be.i28.us126.i.i.i, %7
  br i1 %_9.not.i.us127.i.i.i, label %bb14, label %bb2.i15.us110.i.i.i

bb7.i.preheader.us128.i.i.i:                      ; preds = %bb3.i19.us114.i.i.i
  %_45.i.us119.i.i.i = load i8, i8* %101, align 1, !alias.scope !316, !noalias !317
  %_44.not.i.us120.i.i.i = icmp eq i8 %_45.i.us119.i.i.i, %_15.i.us115.i.i.i
  %_55.pn.i.us123.i.i.i = select i1 %_44.not.i.us120.i.i.i, i64 %shift.i.i.i, i64 1
  %pos.1.i27.us124.i.i.i = sub i64 %pos.017.i.us111.i.i.i, %_55.pn.i.us123.i.i.i
  br label %bb1.backedge.i29.us125.i.i.i

bb2.us77.preheader.i.i.i.i:                       ; preds = %bb2.lr.ph.split.split.i.i.i.i
  %_56.i.neg.i.i.i = xor i64 %7, -1
  br label %bb2.us77.i.i.i.i

bb2.us77.i.i.i.i:                                 ; preds = %bb1.backedge.us115.i.i.i.i, %bb2.us77.preheader.i.i.i.i
  %pos.017.us78.i.i.i.i = phi i64 [ %pos.0.be.us116.i.i.i.i, %bb1.backedge.us115.i.i.i.i ], [ %pos, %bb2.us77.preheader.i.i.i.i ]
  %_16.us79.i.i.i.i = sub i64 %pos.017.us78.i.i.i.i, %7
  %_20.us80.i.i.i.i = icmp ult i64 %_16.us79.i.i.i.i, %pos
  br i1 %_20.us80.i.i.i.i, label %bb3.us81.i.i.i.i, label %panic.i21.i.i.i, !prof !60

bb3.us81.i.i.i.i:                                 ; preds = %bb2.us77.i.i.i.i
  %120 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_16.us79.i.i.i.i
  %_15.us82.i.i.i.i = load i8, i8* %120, align 1, !alias.scope !314, !noalias !315
  %121 = and i8 %_15.us82.i.i.i.i, 63
  %122 = zext i8 %121 to i64
  %_5.i.us83.i.i.i.i = shl nuw i64 1, %122
  %_3.i.us84.i.i.i.i = and i64 %_5.i.us83.i.i.i.i, %self.idx8.val.i.i.i
  %.not.us85.i.i.i.i = icmp eq i64 %_3.i.us84.i.i.i.i, 0
  br i1 %.not.us85.i.i.i.i, label %bb1.backedge.us115.i.i.i.i, label %bb7.us86.i.i.i.i

bb7.us86.i.i.i.i:                                 ; preds = %bb3.us81.i.i.i.i, %bb12.us96.i.i.i.i
  %i.0.us87.i.i.i.i = phi i64 [ %_28.us90.i.i.i.i, %bb12.us96.i.i.i.i ], [ %7, %bb3.us81.i.i.i.i ]
  %_24.not.us88.i.i.i.i = icmp eq i64 %i.0.us87.i.i.i.i, 0
  br i1 %_24.not.us88.i.i.i.i, label %bb16.us100.i.i.i.i, label %bb9.us89.i.i.i.i

bb9.us89.i.i.i.i:                                 ; preds = %bb7.us86.i.i.i.i
  %_28.us90.i.i.i.i = add i64 %i.0.us87.i.i.i.i, -1
  %_33.us94.i.i.i.i = add i64 %_28.us90.i.i.i.i, %_16.us79.i.i.i.i
  %_40.us95.i.i.i.i = icmp ult i64 %_33.us94.i.i.i.i, %pos
  br i1 %_40.us95.i.i.i.i, label %bb12.us96.i.i.i.i, label %panic2.i26.i.i.i, !prof !60

bb12.us96.i.i.i.i:                                ; preds = %bb9.us89.i.i.i.i
  %123 = getelementptr inbounds [0 x i8], [0 x i8]* %5, i64 0, i64 %_28.us90.i.i.i.i
  %_27.us97.i.i.i.i = load i8, i8* %123, align 1, !alias.scope !316, !noalias !317
  %124 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_33.us94.i.i.i.i
  %_32.us98.i.i.i.i = load i8, i8* %124, align 1, !alias.scope !314, !noalias !315
  %_26.us99.i.i.i.i = icmp eq i8 %_27.us97.i.i.i.i, %_32.us98.i.i.i.i
  br i1 %_26.us99.i.i.i.i, label %bb7.us86.i.i.i.i, label %bb20.us104.i.i.i.i

bb16.us100.i.i.i.i:                               ; preds = %bb7.us86.i.i.i.i
  %_45.us102.i.i.i.i = load i8, i8* %101, align 1, !alias.scope !316, !noalias !317
  %_44.not.us103.i.i.i.i = icmp eq i8 %_45.us102.i.i.i.i, %_15.us82.i.i.i.i
  br i1 %_44.not.us103.i.i.i.i, label %bb6, label %bb20.us104.i.i.i.i

bb20.us104.i.i.i.i:                               ; preds = %bb12.us96.i.i.i.i, %bb16.us100.i.i.i.i
  %_55.us107.neg.i.i.i.i = add i64 %pos.017.us78.i.i.i.i, %_56.i.neg.i.i.i
  %pos.1.us110.i.i.i.i = add i64 %_55.us107.neg.i.i.i.i, %i.0.us87.i.i.i.i
  br label %bb1.backedge.us115.i.i.i.i

bb1.backedge.us115.i.i.i.i:                       ; preds = %bb20.us104.i.i.i.i, %bb3.us81.i.i.i.i
  %pos.0.be.us116.i.i.i.i = phi i64 [ %pos.1.us110.i.i.i.i, %bb20.us104.i.i.i.i ], [ %_16.us79.i.i.i.i, %bb3.us81.i.i.i.i ]
  %_9.not.us117.i.i.i.i = icmp ult i64 %pos.0.be.us116.i.i.i.i, %7
  br i1 %_9.not.us117.i.i.i.i, label %bb14, label %bb2.us77.i.i.i.i

bb2.i15.i.i.i:                                    ; preds = %bb2.preheader.i13.split.i.i.i, %bb1.backedge.i29.i.i.i
  %pos.017.i.i.i.i = phi i64 [ %_16.i14.i.i.i, %bb1.backedge.i29.i.i.i ], [ %pos, %bb2.preheader.i13.split.i.i.i ]
  %_16.i14.i.i.i = sub i64 %pos.017.i.i.i.i, %7
  %_20.i.i.i.i = icmp ult i64 %_16.i14.i.i.i, %pos
  br i1 %_20.i.i.i.i, label %bb3.i19.i.i.i, label %panic.i21.i.i.i, !prof !60

bb3.i19.i.i.i:                                    ; preds = %bb2.i15.i.i.i
  %125 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_16.i14.i.i.i
  %_15.i.i.i.i = load i8, i8* %125, align 1, !alias.scope !314, !noalias !315
  %126 = and i8 %_15.i.i.i.i, 63
  %127 = zext i8 %126 to i64
  %_5.i.i16.i.i.i = shl nuw i64 1, %127
  %_3.i.i17.i.i.i = and i64 %_5.i.i16.i.i.i, %self.idx8.val.i.i.i
  %.not.i18.i.i.i = icmp eq i64 %_3.i.i17.i.i.i, 0
  br i1 %.not.i18.i.i.i, label %bb1.backedge.i29.i.i.i, label %panic1.i23.i.i.i

panic.i21.i.i.i:                                  ; preds = %bb2.i15.i.i.i, %bb2.i15.us110.i.i.i, %bb2.i15.us.i.i.i, %bb2.us77.i.i.i.i, %bb2.us26.i.i.i.i, %bb2.lr.ph.i11.i.i.i
  %.us-phi.i20.i.i.i = phi i64 [ %pos, %bb2.lr.ph.i11.i.i.i ], [ %_16.us28.i.i.i.i, %bb2.us26.i.i.i.i ], [ %_16.us79.i.i.i.i, %bb2.us77.i.i.i.i ], [ %_16.i14.us.i.i.i, %bb2.i15.us.i.i.i ], [ %_16.i14.us112.i.i.i, %bb2.i15.us110.i.i.i ], [ %_16.i14.i.i.i, %bb2.i15.i.i.i ]
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %.us-phi.i20.i.i.i, i64 %pos, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1139 to %"core::panic::location::Location"*)) #22, !noalias !318
  unreachable

panic1.i23.i.i.i:                                 ; preds = %bb3.i19.i.i.i, %bb7.us35.i.preheader.split.i.i.i
  %.us-phi22.i.pre-phi.i.i.i = phi i64 [ %102, %bb7.us35.i.preheader.split.i.i.i ], [ %111, %bb3.i19.i.i.i ]
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %.us-phi22.i.pre-phi.i.i.i, i64 %7, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1141 to %"core::panic::location::Location"*)) #22, !noalias !318
  unreachable

panic2.i26.i.i.i:                                 ; preds = %bb9.i.us.us.i.i.i, %bb9.us89.i.i.i.i, %bb9.us38.i.us.i.i.i
  %.us-phi23.i.i.i.i = phi i64 [ %_33.us43.i.us.i.i.i, %bb9.us38.i.us.i.i.i ], [ %_33.us94.i.i.i.i, %bb9.us89.i.i.i.i ], [ %_33.i.us.us.i.i.i, %bb9.i.us.us.i.i.i ]
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %.us-phi23.i.i.i.i, i64 %pos, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1143 to %"core::panic::location::Location"*)) #22, !noalias !318
  unreachable

bb1.backedge.i29.i.i.i:                           ; preds = %bb3.i19.i.i.i
  %_9.not.i.i.i.i = icmp ult i64 %_16.i14.i.i.i, %7
  br i1 %_9.not.i.i.i.i, label %bb14, label %bb2.i15.i.i.i

panic6.i30.i.i.i:                                 ; preds = %bb26.us.i.i.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_70.us.i.i.i.i, i64 %pos, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1151 to %"core::panic::location::Location"*)) #22, !noalias !318
  unreachable

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i.i": ; preds = %bb4.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !319)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !322)
  %_12.i.i9.i.i.i.i = icmp eq i64 %7, 0
  br i1 %_12.i.i9.i.i.i.i, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i, label %bb7.preheader.i.i.i.i

bb7.preheader.i.i.i.i:                            ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i.i"
  %128 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %pos
  %129 = add i64 %7, -1
  %xtraiter = and i64 %7, 7
  %130 = icmp ult i64 %129, 7
  br i1 %130, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i.loopexit.unr-lcssa, label %bb7.preheader.i.i.i.i.new

bb7.preheader.i.i.i.i.new:                        ; preds = %bb7.preheader.i.i.i.i
  %unroll_iter = and i64 %7, -8
  br label %bb7.i.i.i.i

bb7.i.i.i.i:                                      ; preds = %bb7.i.i.i.i, %bb7.preheader.i.i.i.i.new
  %hash.011.i.i.i.i = phi i32 [ 0, %bb7.preheader.i.i.i.i.new ], [ %154, %bb7.i.i.i.i ]
  %iter.sroa.4.010.i.i.i.i = phi i8* [ %128, %bb7.preheader.i.i.i.i.new ], [ %152, %bb7.i.i.i.i ]
  %niter = phi i64 [ %unroll_iter, %bb7.preheader.i.i.i.i.new ], [ %niter.nsub.7, %bb7.i.i.i.i ]
  %131 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i.i.i, i64 -1
  %b.i.i.i.i = load i8, i8* %131, align 1, !alias.scope !324, !noalias !327
  %132 = shl i32 %hash.011.i.i.i.i, 1
  %_6.i.i.i.i.i = zext i8 %b.i.i.i.i to i32
  %133 = add i32 %132, %_6.i.i.i.i.i
  %134 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i.i.i, i64 -2
  %b.i.i.i.i.1 = load i8, i8* %134, align 1, !alias.scope !324, !noalias !327
  %135 = shl i32 %133, 1
  %_6.i.i.i.i.i.1 = zext i8 %b.i.i.i.i.1 to i32
  %136 = add i32 %135, %_6.i.i.i.i.i.1
  %137 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i.i.i, i64 -3
  %b.i.i.i.i.2 = load i8, i8* %137, align 1, !alias.scope !324, !noalias !327
  %138 = shl i32 %136, 1
  %_6.i.i.i.i.i.2 = zext i8 %b.i.i.i.i.2 to i32
  %139 = add i32 %138, %_6.i.i.i.i.i.2
  %140 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i.i.i, i64 -4
  %b.i.i.i.i.3 = load i8, i8* %140, align 1, !alias.scope !324, !noalias !327
  %141 = shl i32 %139, 1
  %_6.i.i.i.i.i.3 = zext i8 %b.i.i.i.i.3 to i32
  %142 = add i32 %141, %_6.i.i.i.i.i.3
  %143 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i.i.i, i64 -5
  %b.i.i.i.i.4 = load i8, i8* %143, align 1, !alias.scope !324, !noalias !327
  %144 = shl i32 %142, 1
  %_6.i.i.i.i.i.4 = zext i8 %b.i.i.i.i.4 to i32
  %145 = add i32 %144, %_6.i.i.i.i.i.4
  %146 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i.i.i, i64 -6
  %b.i.i.i.i.5 = load i8, i8* %146, align 1, !alias.scope !324, !noalias !327
  %147 = shl i32 %145, 1
  %_6.i.i.i.i.i.5 = zext i8 %b.i.i.i.i.5 to i32
  %148 = add i32 %147, %_6.i.i.i.i.i.5
  %149 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i.i.i, i64 -7
  %b.i.i.i.i.6 = load i8, i8* %149, align 1, !alias.scope !324, !noalias !327
  %150 = shl i32 %148, 1
  %_6.i.i.i.i.i.6 = zext i8 %b.i.i.i.i.6 to i32
  %151 = add i32 %150, %_6.i.i.i.i.i.6
  %152 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i.i.i, i64 -8
  %b.i.i.i.i.7 = load i8, i8* %152, align 1, !alias.scope !324, !noalias !327
  %153 = shl i32 %151, 1
  %_6.i.i.i.i.i.7 = zext i8 %b.i.i.i.i.7 to i32
  %154 = add i32 %153, %_6.i.i.i.i.i.7
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i.loopexit.unr-lcssa, label %bb7.i.i.i.i

_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i.loopexit.unr-lcssa: ; preds = %bb7.i.i.i.i, %bb7.preheader.i.i.i.i
  %.lcssa338.ph = phi i32 [ undef, %bb7.preheader.i.i.i.i ], [ %154, %bb7.i.i.i.i ]
  %hash.011.i.i.i.i.unr = phi i32 [ 0, %bb7.preheader.i.i.i.i ], [ %154, %bb7.i.i.i.i ]
  %iter.sroa.4.010.i.i.i.i.unr = phi i8* [ %128, %bb7.preheader.i.i.i.i ], [ %152, %bb7.i.i.i.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i, label %bb7.i.i.i.i.epil

bb7.i.i.i.i.epil:                                 ; preds = %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i.loopexit.unr-lcssa, %bb7.i.i.i.i.epil
  %hash.011.i.i.i.i.epil = phi i32 [ %157, %bb7.i.i.i.i.epil ], [ %hash.011.i.i.i.i.unr, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i.loopexit.unr-lcssa ]
  %iter.sroa.4.010.i.i.i.i.epil = phi i8* [ %155, %bb7.i.i.i.i.epil ], [ %iter.sroa.4.010.i.i.i.i.unr, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb7.i.i.i.i.epil ], [ %xtraiter, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i.loopexit.unr-lcssa ]
  %155 = getelementptr inbounds i8, i8* %iter.sroa.4.010.i.i.i.i.epil, i64 -1
  %b.i.i.i.i.epil = load i8, i8* %155, align 1, !alias.scope !324, !noalias !327
  %156 = shl i32 %hash.011.i.i.i.i.epil, 1
  %_6.i.i.i.i.i.epil = zext i8 %b.i.i.i.i.epil to i32
  %157 = add i32 %156, %_6.i.i.i.i.i.epil
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i, label %bb7.i.i.i.i.epil, !llvm.loop !329

_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i: ; preds = %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i.loopexit.unr-lcssa, %bb7.i.i.i.i.epil, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i.i"
  %hash.0.lcssa.i.i.i.i = phi i32 [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i.i" ], [ %.lcssa338.ph, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i.loopexit.unr-lcssa ], [ %157, %bb7.i.i.i.i.epil ]
  %.idx.i.i.i = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1, i32 0, i32 2, i32 0
  %.idx.val.i.i.i = load i32, i32* %.idx.i.i.i, align 8, !alias.scope !330, !noalias !331
  %158 = xor i64 %7, -1
  %.idx20.i.i.i = getelementptr %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1, i32 0, i32 2, i32 1
  %.idx20.val.i.i.i = load i32, i32* %.idx20.i.i.i, align 4, !alias.scope !330, !noalias !331
  br label %bb5.i.i.i

bb5.i.i.i:                                        ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i.i", %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i
  %hash.0.i.i.i = phi i32 [ %hash.0.lcssa.i.i.i.i, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i ], [ %165, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i.i" ]
  %haystack.sroa.16.0.i.i.i = phi i64 [ %pos, %_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE.exit.i.i.i ], [ %_42.i.i.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i.i" ]
  %159 = icmp eq i32 %.idx.val.i.i.i, %hash.0.i.i.i
  br i1 %159, label %bb7.i.i.i, label %bb12.i.i.i

bb7.i.i.i:                                        ; preds = %bb5.i.i.i
; call memchr::memmem::rabinkarp::is_suffix
  %_24.i.i.i = tail call fastcc zeroext i1 @_ZN6memchr6memmem9rabinkarp9is_suffix17hf2646f43626eea64E([0 x i8]* noalias nonnull readonly align 1 %_9.0, i64 %haystack.sroa.16.0.i.i.i, [0 x i8]* noalias nonnull readonly align 1 %5, i64 %7), !noalias !330
  br i1 %_24.i.i.i, label %bb11.i.i.i, label %bb12.i.i.i

bb12.i.i.i:                                       ; preds = %bb7.i.i.i, %bb5.i.i.i
  %_32.not.i.i.i = icmp ugt i64 %haystack.sroa.16.0.i.i.i, %7
  br i1 %_32.not.i.i.i, label %bb14.i.i.i, label %bb14

bb11.i.i.i:                                       ; preds = %bb7.i.i.i
  %_27.i.i.i = sub i64 %haystack.sroa.16.0.i.i.i, %7
  br label %bb6

bb14.i.i.i:                                       ; preds = %bb12.i.i.i
  %_42.i.i.i = add i64 %haystack.sroa.16.0.i.i.i, -1
  %_48.i.i.i = add i64 %haystack.sroa.16.0.i.i.i, %158
  %_55.i.i.i = icmp ult i64 %_48.i.i.i, %haystack.sroa.16.0.i.i.i
  br i1 %_55.i.i.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i.i", label %panic1.i.i.i, !prof !60

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i.i": ; preds = %bb14.i.i.i
  %160 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_42.i.i.i
  %_41.i.i.i = load i8, i8* %160, align 1, !alias.scope !332, !noalias !327
  %_8.i.i.i.i.i = zext i8 %_41.i.i.i to i32
  %161 = mul i32 %.idx20.val.i.i.i, %_8.i.i.i.i.i
  %162 = sub i32 %hash.0.i.i.i, %161
  %163 = shl i32 %162, 1
  %164 = getelementptr inbounds [0 x i8], [0 x i8]* %_9.0, i64 0, i64 %_48.i.i.i
  %_47.i.i.i = load i8, i8* %164, align 1, !alias.scope !332, !noalias !327
  %_6.i1.i.i.i.i = zext i8 %_47.i.i.i to i32
  %165 = add i32 %163, %_6.i1.i.i.i.i
  br label %bb5.i.i.i

panic1.i.i.i:                                     ; preds = %bb14.i.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_48.i.i.i, i64 %haystack.sroa.16.0.i.i.i, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1077 to %"core::panic::location::Location"*)) #22, !noalias !327
  unreachable

bb6:                                              ; preds = %bb23.preheader.i.i.i.i, %bb29.i.i.i.i, %bb16.us100.i.i.i.i, %bb29.us.i.i.i.i, %bb28.us.i.i.i.i, %bb9.i.i.i.i.i.i, %bb16.i.i.i.i.i.i, %bb64.i.i.i.i.i.i, %bb54.i.i.i.i.i.i, %bb52.i.i.i.i.i.i, %bb48.i.i.i.i.i.i, %bb44.i.i.i.i.i.i, %bb4.i61.i.i.i.i.i.i, %bb11.i.i.i
  %.sroa.6.0.i.i.ph = phi i64 [ %_27.i.i.i, %bb11.i.i.i ], [ %83, %bb4.i61.i.i.i.i.i.i ], [ %_107.i.i.i.i.i.i, %bb44.i.i.i.i.i.i ], [ %_114.i.i.i.i.i.i, %bb48.i.i.i.i.i.i ], [ %_121.i.i.i.i.i.i, %bb52.i.i.i.i.i.i ], [ %_127.i.i.i.i.i.i, %bb54.i.i.i.i.i.i ], [ %_13.i53.i.i.i.i.i.i, %bb64.i.i.i.i.i.i ], [ %_13.i.i.i.i.i.i.i, %bb16.i.i.i.i.i.i ], [ %21, %bb9.i.i.i.i.i.i ], [ %_16.us28.i.i.i.i, %bb28.us.i.i.i.i ], [ %_16.us28.i.i.i.i, %bb29.us.i.i.i.i ], [ %_16.us79.i.i.i.i, %bb16.us100.i.i.i.i ], [ %_17.i.i.i.i, %bb29.i.i.i.i ], [ %_17.i.i.i.i, %bb23.preheader.i.i.i.i ]
  %_14 = icmp eq i64 %pos, %.sroa.6.0.i.i.ph
  br i1 %_14, label %bb9, label %bb14.sink.split

bb9:                                              ; preds = %bb3.i.i, %bb6
  %166 = tail call { i64, i1 } @llvm.usub.with.overflow.i64(i64 %pos, i64 1) #21
  %167 = extractvalue { i64, i1 } %166, 0
  %168 = extractvalue { i64, i1 } %166, 1
  %_5.1.not.i = xor i1 %168, true
  %..i = zext i1 %_5.1.not.i to i64
  br label %bb14.sink.split

bb14.sink.split:                                  ; preds = %bb6, %bb9
  %.sink = phi i64 [ %..i, %bb9 ], [ 1, %bb6 ]
  %.sroa.6.0.i.i.ph.sink = phi i64 [ %167, %bb9 ], [ %.sroa.6.0.i.i.ph, %bb6 ]
  %.sroa.4.1.ph = phi i64 [ %pos, %bb9 ], [ %.sroa.6.0.i.i.ph, %bb6 ]
  store i64 %.sink, i64* %0, align 8
  store i64 %.sroa.6.0.i.i.ph.sink, i64* %1, align 8
  br label %bb14

bb14:                                             ; preds = %bb1.backedge.i.i.i.i, %bb1.backedge.i29.i.i.i, %bb1.backedge.i29.us125.i.i.i, %bb1.backedge.i29.us.i.i.i, %bb1.backedge.us115.i.i.i.i, %bb1.backedge.us61.i.i.i.i, %bb12.i.i.i, %bb6.i.i.i.i.i.i, %bb14.sink.split, %bb67.i.i.i.i.i.i, %bb66.i.i.i.i.i.i, %bb7.i.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit", %start
  %.sroa.4.1 = phi i64 [ undef, %start ], [ undef, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit" ], [ undef, %bb7.i.i ], [ undef, %bb66.i.i.i.i.i.i ], [ undef, %bb67.i.i.i.i.i.i ], [ %.sroa.4.1.ph, %bb14.sink.split ], [ undef, %bb6.i.i.i.i.i.i ], [ undef, %bb12.i.i.i ], [ %_16.us28.i.i.i.i, %bb1.backedge.us61.i.i.i.i ], [ %_16.us79.i.i.i.i, %bb1.backedge.us115.i.i.i.i ], [ undef, %bb1.backedge.i29.us.i.i.i ], [ undef, %bb1.backedge.i29.us125.i.i.i ], [ undef, %bb1.backedge.i29.i.i.i ], [ %_17.i.i.i.i, %bb1.backedge.i.i.i.i ]
  %.sroa.0.1 = phi i64 [ 0, %start ], [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit" ], [ 0, %bb7.i.i ], [ 0, %bb66.i.i.i.i.i.i ], [ 0, %bb67.i.i.i.i.i.i ], [ 1, %bb14.sink.split ], [ 0, %bb6.i.i.i.i.i.i ], [ 0, %bb12.i.i.i ], [ 0, %bb1.backedge.us61.i.i.i.i ], [ 0, %bb1.backedge.us115.i.i.i.i ], [ 0, %bb1.backedge.i29.us.i.i.i ], [ 0, %bb1.backedge.i29.us125.i.i.i ], [ 0, %bb1.backedge.i29.i.i.i ], [ 0, %bb1.backedge.i.i.i.i ]
  %169 = insertvalue { i64, i64 } undef, i64 %.sroa.0.1, 0
  %170 = insertvalue { i64, i64 } %169, i64 %.sroa.4.1, 1
  ret { i64, i64 } %170
}

; memchr::memmem::Finder::find
; Function Attrs: nonlazybind uwtable
define { i64, i64 } @_ZN6memchr6memmem6Finder4find17hd8d1eb1563a46245E(%"memmem::Finder"* noalias readonly align 8 dereferenceable(80) %self, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1) unnamed_addr #6 personality i32 (...)* @rust_eh_personality {
start:
  %_6 = alloca { i32, i32 }, align 4
  %_3 = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0
  %0 = bitcast { i32, i32 }* %_6 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %0)
  %_3.i = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 1
  %1 = bitcast i64** %_3.i to {}**
  %2 = load {}*, {}** %1, align 8, !alias.scope !333
  %.not.i.not.i.i = icmp ne {}* %2, null
  %spec.select.i = zext i1 %.not.i.not.i.i to i32
  %.fca.0.gep = getelementptr inbounds { i32, i32 }, { i32, i32 }* %_6, i64 0, i32 0
  store i32 %spec.select.i, i32* %.fca.0.gep, align 4
  %.fca.1.gep = getelementptr inbounds { i32, i32 }, { i32, i32 }* %_6, i64 0, i32 1
  store i32 0, i32* %.fca.1.gep, align 4
  tail call void @llvm.experimental.noalias.scope.decl(metadata !340)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !343)
  %3 = bitcast %"memmem::Finder"* %self to [0 x i8]**
  %4 = load [0 x i8]*, [0 x i8]** %3, align 8, !alias.scope !345, !noalias !352, !nonnull !142
  %5 = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 0, i32 1
  %6 = load i64, i64* %5, align 8, !alias.scope !345, !noalias !352
  %_6.i = icmp ugt i64 %6, %haystack.1
  br i1 %_6.i, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb3.i

bb3.i:                                            ; preds = %start
  %7 = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 2, i32 0
  %8 = load i8, i8* %7, align 8, !range !157, !alias.scope !340, !noalias !352
  %_11.i = zext i8 %8 to i64
  switch i64 %_11.i, label %bb5.i [
    i64 0, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit
    i64 1, label %bb7.i
    i64 2, label %bb9.i
    i64 3, label %bb16.i
    i64 4, label %bb4.i
  ]

bb5.i:                                            ; preds = %bb3.i
  unreachable

bb7.i:                                            ; preds = %bb3.i
  %9 = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 2, i32 1, i64 0
  %b.i = load i8, i8* %9, align 1, !alias.scope !340, !noalias !352
  %10 = icmp eq i64 %haystack.1, 0
  br i1 %10, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb3.i.i

bb3.i.i:                                          ; preds = %bb7.i
  %.0.vec.insert.i.i.i.i.i.i30.i = insertelement <16 x i8> undef, i8 %b.i, i32 0
  %.15.vec.insert.i.i.i.i.i.i31.i = shufflevector <16 x i8> %.0.vec.insert.i.i.i.i.i.i30.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %11 = icmp ult i64 %haystack.1, 64
  %.0.sroa.speculated.i.i.i.i.i.i.i.i = select i1 %11, i64 %haystack.1, i64 64
  %12 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %_17.i.i.i.i.i = icmp ult i64 %haystack.1, 16
  br i1 %_17.i.i.i.i.i, label %bb7.preheader.i.i.i.i.i, label %bb13.i.i.i.i.i

bb7.preheader.i.i.i.i.i:                          ; preds = %bb3.i.i
  %13 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  br label %bb7.i.i.i.i.i

bb13.i.i.i.i.i:                                   ; preds = %bb3.i.i
  %14 = bitcast [0 x i8]* %haystack.0 to <16 x i8>*
  %.0.copyload9.i.i.i.i.i.i = load <16 x i8>, <16 x i8>* %14, align 1, !alias.scope !354, !noalias !363
  %15 = icmp eq <16 x i8> %.0.copyload9.i.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i31.i
  %16 = bitcast <16 x i1> %15 to i16
  %17 = icmp eq i16 %16, 0
  br i1 %17, label %bb16.i.i.i.i.i, label %bb15.i.i.i.i.i

bb7.i.i.i.i.i:                                    ; preds = %bb10.i.i.i.i32.i, %bb7.preheader.i.i.i.i.i
  %ptr.0156.i.i.i.i.i = phi i8* [ %18, %bb10.i.i.i.i32.i ], [ %13, %bb7.preheader.i.i.i.i.i ]
  %_24.i.i.i.i.i = load i8, i8* %ptr.0156.i.i.i.i.i, align 1, !alias.scope !354, !noalias !363
  %_23.i.i.i.i.i = icmp eq i8 %_24.i.i.i.i.i, %b.i
  br i1 %_23.i.i.i.i.i, label %bb8.i.i.i.i.i, label %bb10.i.i.i.i32.i

bb10.i.i.i.i32.i:                                 ; preds = %bb7.i.i.i.i.i
  %18 = getelementptr inbounds i8, i8* %ptr.0156.i.i.i.i.i, i64 1
  %_20.i.i.i.i.i = icmp ult i8* %18, %12
  br i1 %_20.i.i.i.i.i, label %bb7.i.i.i.i.i, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb8.i.i.i.i.i:                                    ; preds = %bb7.i.i.i.i.i
  %_3.i.i.i.i.i.i = ptrtoint i8* %ptr.0156.i.i.i.i.i to i64
  %_5.i.i.i.i.i.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %19 = sub i64 %_3.i.i.i.i.i.i, %_5.i.i.i.i.i.i
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb15.i.i.i.i.i:                                   ; preds = %bb13.i.i.i.i.i
  %20 = tail call i16 @llvm.cttz.i16(i16 %16, i1 true) #21, !range !44
  %21 = zext i16 %20 to i64
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb16.i.i.i.i.i:                                   ; preds = %bb13.i.i.i.i.i
  %_43.i.i.i.i.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %_42.i.i.i.i.i = and i64 %_43.i.i.i.i.i, 15
  %_41.i.i.i.i.i = sub nuw nsw i64 16, %_42.i.i.i.i.i
  %22 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_41.i.i.i.i.i
  %_46.i.i.i.i.i = icmp ugt i64 %haystack.1, 63
  %23 = getelementptr inbounds i8, i8* %12, i64 -64
  %_48144.i.i.i.i.i = icmp ule i8* %22, %23
  %or.cond145.i.i.i.i.i = select i1 %_46.i.i.i.i.i, i1 %_48144.i.i.i.i.i, i1 false
  br i1 %or.cond145.i.i.i.i.i, label %bb23.i.i.i.i.i, label %bb58.preheader.i.i.i.i.i

bb58.preheader.i.i.i.i.i:                         ; preds = %bb55.i.i.i.i.i, %bb16.i.i.i.i.i
  %ptr.1.lcssa.i.i.i.i.i = phi i8* [ %22, %bb16.i.i.i.i.i ], [ %41, %bb55.i.i.i.i.i ]
  %24 = getelementptr inbounds i8, i8* %12, i64 -16
  %_129.not152.i.i.i.i.i = icmp ugt i8* %ptr.1.lcssa.i.i.i.i.i, %24
  br i1 %_129.not152.i.i.i.i.i, label %bb65.i.i.i.i.i, label %bb60.i.i.i.i.i

bb23.i.i.i.i.i:                                   ; preds = %bb16.i.i.i.i.i, %bb55.i.i.i.i.i
  %ptr.1146.i.i.i.i.i = phi i8* [ %41, %bb55.i.i.i.i.i ], [ %22, %bb16.i.i.i.i.i ]
  %25 = bitcast i8* %ptr.1146.i.i.i.i.i to <16 x i8>*
  %_57.val133.i.i.i.i.i = load <16 x i8>, <16 x i8>* %25, align 16, !alias.scope !354, !noalias !363
  %26 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i, i64 16
  %27 = bitcast i8* %26 to <16 x i8>*
  %_60.val134.i.i.i.i.i = load <16 x i8>, <16 x i8>* %27, align 16, !alias.scope !354, !noalias !363
  %28 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i, i64 32
  %29 = bitcast i8* %28 to <16 x i8>*
  %_64.val135.i.i.i.i.i = load <16 x i8>, <16 x i8>* %29, align 16, !alias.scope !354, !noalias !363
  %30 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i, i64 48
  %31 = bitcast i8* %30 to <16 x i8>*
  %_69.val136.i.i.i.i.i = load <16 x i8>, <16 x i8>* %31, align 16, !alias.scope !354, !noalias !363
  %32 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i31.i, %_57.val133.i.i.i.i.i
  %33 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i31.i, %_60.val134.i.i.i.i.i
  %34 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i31.i, %_64.val135.i.i.i.i.i
  %35 = icmp eq <16 x i8> %.15.vec.insert.i.i.i.i.i.i31.i, %_69.val136.i.i.i.i.i
  %36 = or <16 x i1> %33, %32
  %37 = or <16 x i1> %36, %34
  %38 = or <16 x i1> %37, %35
  %39 = bitcast <16 x i1> %38 to i16
  %40 = icmp eq i16 %39, 0
  br i1 %40, label %bb55.i.i.i.i.i, label %bb39.i.i.i.i.i

bb55.i.i.i.i.i:                                   ; preds = %bb23.i.i.i.i.i
  %41 = getelementptr inbounds i8, i8* %ptr.1146.i.i.i.i.i, i64 %.0.sroa.speculated.i.i.i.i.i.i.i.i
  %_48.not.i.i.i.i.i = icmp ugt i8* %41, %23
  br i1 %_48.not.i.i.i.i.i, label %bb58.preheader.i.i.i.i.i, label %bb23.i.i.i.i.i

bb39.i.i.i.i.i:                                   ; preds = %bb23.i.i.i.i.i
  %_3.i40.i.i.i.i.i = ptrtoint i8* %ptr.1146.i.i.i.i.i to i64
  %42 = sub i64 %_3.i40.i.i.i.i.i, %_43.i.i.i.i.i
  %43 = bitcast <16 x i1> %32 to i16
  %44 = icmp eq i16 %43, 0
  br i1 %44, label %bb44.i.i.i.i.i, label %bb42.i.i.i.i.i

bb44.i.i.i.i.i:                                   ; preds = %bb39.i.i.i.i.i
  %45 = bitcast <16 x i1> %33 to i16
  %46 = icmp eq i16 %45, 0
  br i1 %46, label %bb48.i.i.i.i.i, label %bb46.i.i.i.i.i

bb42.i.i.i.i.i:                                   ; preds = %bb39.i.i.i.i.i
  %47 = tail call i16 @llvm.cttz.i16(i16 %43, i1 true) #21, !range !44
  %48 = zext i16 %47 to i64
  %_102.i.i.i.i.i = add i64 %42, %48
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb48.i.i.i.i.i:                                   ; preds = %bb44.i.i.i.i.i
  %49 = bitcast <16 x i1> %34 to i16
  %50 = icmp eq i16 %49, 0
  br i1 %50, label %bb52.i.i.i.i.i, label %bb50.i.i.i.i.i

bb46.i.i.i.i.i:                                   ; preds = %bb44.i.i.i.i.i
  %51 = add i64 %42, 16
  %52 = tail call i16 @llvm.cttz.i16(i16 %45, i1 true) #21, !range !44
  %53 = zext i16 %52 to i64
  %_109.i.i.i.i.i = add i64 %51, %53
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb52.i.i.i.i.i:                                   ; preds = %bb48.i.i.i.i.i
  %54 = add i64 %42, 48
  %55 = bitcast <16 x i1> %35 to i16
  %56 = zext i16 %55 to i32
  %57 = tail call i32 @llvm.cttz.i32(i32 %56, i1 false) #21, !range !45
  %58 = zext i32 %57 to i64
  %_122.i.i.i.i.i = add i64 %54, %58
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb50.i.i.i.i.i:                                   ; preds = %bb48.i.i.i.i.i
  %59 = add i64 %42, 32
  %60 = tail call i16 @llvm.cttz.i16(i16 %49, i1 true) #21, !range !44
  %61 = zext i16 %60 to i64
  %_116.i.i.i.i.i = add i64 %59, %61
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb65.i.i.i.i.i:                                   ; preds = %bb63.i.i.i.i.i, %bb58.preheader.i.i.i.i.i
  %ptr.2.lcssa.i.i.i.i.i = phi i8* [ %ptr.1.lcssa.i.i.i.i.i, %bb58.preheader.i.i.i.i.i ], [ %69, %bb63.i.i.i.i.i ]
  %_143.i.i.i.i.i = icmp ult i8* %ptr.2.lcssa.i.i.i.i.i, %12
  br i1 %_143.i.i.i.i.i, label %bb66.i.i.i.i.i, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb60.i.i.i.i.i:                                   ; preds = %bb58.preheader.i.i.i.i.i, %bb63.i.i.i.i.i
  %ptr.2153.i.i.i.i.i = phi i8* [ %69, %bb63.i.i.i.i.i ], [ %ptr.1.lcssa.i.i.i.i.i, %bb58.preheader.i.i.i.i.i ]
  %62 = bitcast i8* %ptr.2153.i.i.i.i.i to <16 x i8>*
  %.0.copyload9.i46.i.i.i.i.i = load <16 x i8>, <16 x i8>* %62, align 1, !alias.scope !354, !noalias !363
  %63 = icmp eq <16 x i8> %.0.copyload9.i46.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i31.i
  %64 = bitcast <16 x i1> %63 to i16
  %65 = icmp eq i16 %64, 0
  br i1 %65, label %bb63.i.i.i.i.i, label %bb62.i.i.i.i.i

bb62.i.i.i.i.i:                                   ; preds = %bb60.i.i.i.i.i
  %_3.i.i47.i.i.i.i.i = ptrtoint i8* %ptr.2153.i.i.i.i.i to i64
  %66 = sub i64 %_3.i.i47.i.i.i.i.i, %_43.i.i.i.i.i
  %67 = tail call i16 @llvm.cttz.i16(i16 %64, i1 true) #21, !range !44
  %68 = zext i16 %67 to i64
  %_13.i.i.i.i.i.i = add i64 %66, %68
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb63.i.i.i.i.i:                                   ; preds = %bb60.i.i.i.i.i
  %69 = getelementptr inbounds i8, i8* %ptr.2153.i.i.i.i.i, i64 16
  %_129.not.i.i.i.i.i = icmp ugt i8* %69, %24
  br i1 %_129.not.i.i.i.i.i, label %bb65.i.i.i.i.i, label %bb60.i.i.i.i.i

bb66.i.i.i.i.i:                                   ; preds = %bb65.i.i.i.i.i
  %_3.i53.i.i.i.i.i = ptrtoint i8* %12 to i64
  %_5.i54.i.i.i.i.i = ptrtoint i8* %ptr.2.lcssa.i.i.i.i.i to i64
  %.neg.neg.i.i.i.i.i = add i64 %_3.i53.i.i.i.i.i, -16
  %70 = sub i64 %.neg.neg.i.i.i.i.i, %_5.i54.i.i.i.i.i
  %71 = getelementptr inbounds i8, i8* %ptr.2.lcssa.i.i.i.i.i, i64 %70
  %72 = bitcast i8* %71 to <16 x i8>*
  %.0.copyload9.i55.i.i.i.i.i = load <16 x i8>, <16 x i8>* %72, align 1, !alias.scope !354, !noalias !363
  %73 = icmp eq <16 x i8> %.0.copyload9.i55.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i31.i
  %74 = bitcast <16 x i1> %73 to i16
  %75 = icmp eq i16 %74, 0
  br i1 %75, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb4.i59.i.i.i.i.i

bb4.i59.i.i.i.i.i:                                ; preds = %bb66.i.i.i.i.i
  %_3.i.i56.i.i.i.i.i = ptrtoint i8* %71 to i64
  %76 = sub i64 %_3.i.i56.i.i.i.i.i, %_43.i.i.i.i.i
  %77 = tail call i16 @llvm.cttz.i16(i16 %74, i1 true) #21, !range !44
  %78 = zext i16 %77 to i64
  %_13.i58.i.i.i.i.i = add i64 %76, %78
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb9.i:                                            ; preds = %bb3.i
  %79 = icmp ult i64 %haystack.1, 16
  br i1 %79, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i37.i", label %bb13.i

bb16.i:                                           ; preds = %bb3.i
  %80 = getelementptr %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 2, i32 1, i64 1
  %.idx.val.i = load i8, i8* %80, align 2, !alias.scope !340, !noalias !352
  %_2.i.i.i = zext i8 %.idx.val.i to i64
  %81 = add nuw nsw i64 %_2.i.i.i, 16
  %_29.i = icmp ugt i64 %81, %haystack.1
  br i1 %_29.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i", label %bb5.i.i.i.i

bb4.i:                                            ; preds = %bb3.i
; call core::panicking::panic
  tail call void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [40 x i8] }>* @alloc1175 to [0 x i8]*), i64 40, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1174 to %"core::panic::location::Location"*)) #22, !noalias !363
  unreachable

bb34.sink.split.i.i.i.i:                          ; preds = %bb9.i.us.i47.i.i.i.i.2, %bb2.i.i.i.i.i.i, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i, %bb7.i.us.i.us.i.i.i.i, %bb2.i.i79.i.i.i.i, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i, %bb2.i.us.i40.i.i.i.i, %bb7.i.us.i43.i.i.i.i.1, %bb7.i.us.i43.i.i.i.i.2
  %.lcssa139.sink = phi i32 [ %164, %bb7.i.us.i43.i.i.i.i.2 ], [ %164, %bb7.i.us.i43.i.i.i.i.1 ], [ %164, %bb2.i.us.i40.i.i.i.i ], [ %168, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i ], [ %176, %bb2.i.i79.i.i.i.i ], [ %102, %bb7.i.us.i.us.i.i.i.i ], [ %120, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i ], [ %139, %bb2.i.i.i.i.i.i ], [ %164, %bb9.i.us.i47.i.i.i.i.2 ]
  %ptr.0130.sink.i.i.i.i = phi i8* [ %87, %bb7.i.us.i43.i.i.i.i.2 ], [ %87, %bb7.i.us.i43.i.i.i.i.1 ], [ %87, %bb2.i.us.i40.i.i.i.i ], [ %87, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i ], [ %87, %bb2.i.i79.i.i.i.i ], [ %ptr.0134.us.i.i.i.i, %bb7.i.us.i.us.i.i.i.i ], [ %ptr.0134.us142.i.i.i.i, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i ], [ %ptr.0134.i.i.i.i, %bb2.i.i.i.i.i.i ], [ %87, %bb9.i.us.i47.i.i.i.i.2 ]
  %offset.i.i.i.i.i.le = zext i32 %.lcssa139.sink to i64
; call memchr::memmem::genericsimd::matched
  %_58.i.i.i.i = tail call fastcc i64 @_ZN6memchr6memmem11genericsimd7matched17hf7507d2589d36674E(i8* nonnull %84, i8* nonnull %ptr.0130.sink.i.i.i.i, i64 %offset.i.i.i.i.i.le)
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb5.i.i.i.i:                                      ; preds = %bb16.i
  %.idx28.i = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 2, i32 1, i64 0
  %.idx28.val.i = load i8, i8* %.idx28.i, align 1, !alias.scope !364, !noalias !369
  tail call void @llvm.experimental.noalias.scope.decl(metadata !374)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !377)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !379)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !382)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !384)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !387)
  %_21.i.i.i.i = zext i8 %.idx28.val.i to i64
  %_29.i.i.i.i = icmp ugt i64 %6, %_21.i.i.i.i
  br i1 %_29.i.i.i.i, label %bb6.i.i.i.i, label %panic.i.i.i.i, !prof !60

bb6.i.i.i.i:                                      ; preds = %bb5.i.i.i.i
  %82 = getelementptr inbounds [0 x i8], [0 x i8]* %4, i64 0, i64 %_21.i.i.i.i
  %_26.i.i.i.i = load i8, i8* %82, align 1, !alias.scope !389, !noalias !390
  %.0.vec.insert.i.i.i.i.i.i.i = insertelement <16 x i8> undef, i8 %_26.i.i.i.i, i32 0
  %.15.vec.insert.i.i.i.i.i.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i.i.i.i.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %_34.i.i.i.i = icmp ugt i64 %6, %_2.i.i.i
  br i1 %_34.i.i.i.i, label %bb8.i.i.i.i, label %panic2.i.i.i.i, !prof !60

panic.i.i.i.i:                                    ; preds = %bb5.i.i.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_21.i.i.i.i, i64 %6, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1049 to %"core::panic::location::Location"*)) #22, !noalias !393
  unreachable

bb8.i.i.i.i:                                      ; preds = %bb6.i.i.i.i
  %83 = getelementptr inbounds [0 x i8], [0 x i8]* %4, i64 0, i64 %_2.i.i.i
  %_31.i.i.i.i = load i8, i8* %83, align 1, !alias.scope !389, !noalias !390
  %.0.vec.insert.i.i.i25.i.i.i.i = insertelement <16 x i8> undef, i8 %_31.i.i.i.i, i32 0
  %.15.vec.insert.i.i.i26.i.i.i.i = shufflevector <16 x i8> %.0.vec.insert.i.i.i25.i.i.i.i, <16 x i8> poison, <16 x i32> zeroinitializer
  %84 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %85 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %haystack.1
  %86 = sub nuw nsw i64 -16, %_2.i.i.i
  %87 = getelementptr inbounds i8, i8* %85, i64 %86
  %88 = sub i64 0, %6
  %89 = getelementptr inbounds i8, i8* %85, i64 %88
  %_38.i.i.i.i.i.i = add i64 %6, -4
  %90 = getelementptr inbounds [0 x i8], [0 x i8]* %4, i64 0, i64 %_38.i.i.i.i.i.i
  %_4671.i.i.i.i.i.i = icmp sgt i64 %_38.i.i.i.i.i.i, 0
  %91 = getelementptr [0 x i8], [0 x i8]* %4, i64 0, i64 0
  %_63.i.i.i.i.i.i = bitcast i8* %90 to i32*
  %_45.not133.i.i.i.i = icmp ugt i8* %84, %87
  br i1 %_45.not133.i.i.i.i, label %bb21.i.i.i.i, label %bb14.lr.ph.i.i.i.i

bb14.lr.ph.i.i.i.i:                               ; preds = %bb8.i.i.i.i
  %_8.i.i.i.i.i.i = icmp ult i64 %6, 4
  br i1 %_8.i.i.i.i.i.i, label %bb14.us.i.i.i.i, label %bb14.lr.ph.split.i.i.i.i

bb14.us.i.i.i.i:                                  ; preds = %bb14.lr.ph.i.i.i.i, %bb18.us.i.i.i.i
  %ptr.0134.us.i.i.i.i = phi i8* [ %109, %bb18.us.i.i.i.i ], [ %84, %bb14.lr.ph.i.i.i.i ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !394)
  %92 = getelementptr inbounds i8, i8* %ptr.0134.us.i.i.i.i, i64 %_21.i.i.i.i
  %93 = bitcast i8* %92 to <16 x i8>*
  %.0.copyload28.i.us.i.i.i.i = load <16 x i8>, <16 x i8>* %93, align 1, !alias.scope !397, !noalias !398
  %94 = getelementptr inbounds i8, i8* %ptr.0134.us.i.i.i.i, i64 %_2.i.i.i
  %95 = bitcast i8* %94 to <16 x i8>*
  %.0.copyload629.i.us.i.i.i.i = load <16 x i8>, <16 x i8>* %95, align 1, !alias.scope !397, !noalias !398
  %96 = icmp eq <16 x i8> %.0.copyload28.i.us.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i
  %97 = icmp eq <16 x i8> %.0.copyload629.i.us.i.i.i.i, %.15.vec.insert.i.i.i26.i.i.i.i
  %98 = and <16 x i1> %97, %96
  %99 = bitcast <16 x i1> %98 to i16
  %100 = icmp eq i16 %99, 0
  br i1 %100, label %bb18.us.i.i.i.i, label %bb10.us.i.us.preheader.i.i.i.i

bb10.us.i.us.preheader.i.i.i.i:                   ; preds = %bb14.us.i.i.i.i
  %101 = zext i16 %99 to i32
  br label %bb10.us.i.us.i.i.i.i

bb10.us.i.us.i.i.i.i:                             ; preds = %bb19.loopexit.us.i.us.i.i.i.i, %bb10.us.i.us.preheader.i.i.i.i
  %match_offsets.042.us.i.us.i.i.i.i = phi i32 [ %107, %bb19.loopexit.us.i.us.i.i.i.i ], [ %101, %bb10.us.i.us.preheader.i.i.i.i ]
  %102 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.us.i.us.i.i.i.i, i1 true) #21, !range !45
  %offset.us.i.us.i.i.i.i = zext i32 %102 to i64
  %103 = getelementptr inbounds i8, i8* %ptr.0134.us.i.i.i.i, i64 %offset.us.i.us.i.i.i.i
  %_37.us.i.us.i.i.i.i = icmp ult i8* %89, %103
  br i1 %_37.us.i.us.i.i.i.i, label %bb18.us.i.i.i.i, label %bb2.i.us.i.us.i.i.i.i

bb2.i.us.i.us.i.i.i.i:                            ; preds = %bb10.us.i.us.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !399) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !402) #21
  br label %bb7.i.us.i.us.i.i.i.i

bb7.i.us.i.us.i.i.i.i:                            ; preds = %bb9.i.us.i.us.i.i.i.i, %bb2.i.us.i.us.i.i.i.i
  %iter.sroa.9.0.i.us.i.us.i.i.i.i = phi i64 [ %106, %bb9.i.us.i.us.i.i.i.i ], [ 0, %bb2.i.us.i.us.i.i.i.i ]
  %exitcond.not.i.us.i.us.i.i.i.i = icmp eq i64 %iter.sroa.9.0.i.us.i.us.i.i.i.i, %6
  br i1 %exitcond.not.i.us.i.us.i.i.i.i, label %bb34.sink.split.i.i.i.i, label %bb9.i.us.i.us.i.i.i.i

bb9.i.us.i.us.i.i.i.i:                            ; preds = %bb7.i.us.i.us.i.i.i.i
  %104 = getelementptr inbounds i8, i8* %103, i64 %iter.sroa.9.0.i.us.i.us.i.i.i.i
  %105 = getelementptr inbounds [0 x i8], [0 x i8]* %4, i64 0, i64 %iter.sroa.9.0.i.us.i.us.i.i.i.i
  %106 = add nuw nsw i64 %iter.sroa.9.0.i.us.i.us.i.i.i.i, 1
  %b1.i.us.i.us.i.i.i.i = load i8, i8* %105, align 1, !alias.scope !404, !noalias !405
  %b2.i.us.i.us.i.i.i.i = load i8, i8* %104, align 1, !alias.scope !406, !noalias !407
  %_23.not.i.us.i.us.i.i.i.i = icmp eq i8 %b1.i.us.i.us.i.i.i.i, %b2.i.us.i.us.i.i.i.i
  br i1 %_23.not.i.us.i.us.i.i.i.i, label %bb7.i.us.i.us.i.i.i.i, label %bb19.loopexit.us.i.us.i.i.i.i

bb19.loopexit.us.i.us.i.i.i.i:                    ; preds = %bb9.i.us.i.us.i.i.i.i
  %_51.us.i.us.i.i.i.i = add i32 %match_offsets.042.us.i.us.i.i.i.i, -1
  %107 = and i32 %_51.us.i.us.i.i.i.i, %match_offsets.042.us.i.us.i.i.i.i
  %108 = icmp eq i32 %107, 0
  br i1 %108, label %bb18.us.i.i.i.i, label %bb10.us.i.us.i.i.i.i

bb18.us.i.i.i.i:                                  ; preds = %bb19.loopexit.us.i.us.i.i.i.i, %bb10.us.i.us.i.i.i.i, %bb14.us.i.i.i.i
  %109 = getelementptr inbounds i8, i8* %ptr.0134.us.i.i.i.i, i64 16
  %_45.not.us.i.i.i.i = icmp ugt i8* %109, %87
  br i1 %_45.not.us.i.i.i.i, label %bb21.i.i.i.i, label %bb14.us.i.i.i.i

bb14.lr.ph.split.i.i.i.i:                         ; preds = %bb14.lr.ph.i.i.i.i
  br i1 %_4671.i.i.i.i.i.i, label %bb14.us141.i.i.i.i, label %bb14.i.i.i.i

bb14.us141.i.i.i.i:                               ; preds = %bb14.lr.ph.split.i.i.i.i, %bb18.us146.i.i.i.i
  %ptr.0134.us142.i.i.i.i = phi i8* [ %128, %bb18.us146.i.i.i.i ], [ %84, %bb14.lr.ph.split.i.i.i.i ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !394)
  %110 = getelementptr inbounds i8, i8* %ptr.0134.us142.i.i.i.i, i64 %_21.i.i.i.i
  %111 = bitcast i8* %110 to <16 x i8>*
  %.0.copyload28.i.us143.i.i.i.i = load <16 x i8>, <16 x i8>* %111, align 1, !alias.scope !397, !noalias !398
  %112 = getelementptr inbounds i8, i8* %ptr.0134.us142.i.i.i.i, i64 %_2.i.i.i
  %113 = bitcast i8* %112 to <16 x i8>*
  %.0.copyload629.i.us144.i.i.i.i = load <16 x i8>, <16 x i8>* %113, align 1, !alias.scope !397, !noalias !398
  %114 = icmp eq <16 x i8> %.0.copyload28.i.us143.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i
  %115 = icmp eq <16 x i8> %.0.copyload629.i.us144.i.i.i.i, %.15.vec.insert.i.i.i26.i.i.i.i
  %116 = and <16 x i1> %115, %114
  %117 = bitcast <16 x i1> %116 to i16
  %118 = icmp eq i16 %117, 0
  br i1 %118, label %bb18.us146.i.i.i.i, label %bb10.us48.i.us.preheader.i.i.i.i

bb10.us48.i.us.preheader.i.i.i.i:                 ; preds = %bb14.us141.i.i.i.i
  %119 = zext i16 %117 to i32
  br label %bb10.us48.i.us.i.i.i.i

bb10.us48.i.us.i.i.i.i:                           ; preds = %bb19.us53.i.us.i.i.i.i, %bb10.us48.i.us.preheader.i.i.i.i
  %match_offsets.042.us49.i.us.i.i.i.i = phi i32 [ %126, %bb19.us53.i.us.i.i.i.i ], [ %119, %bb10.us48.i.us.preheader.i.i.i.i ]
  %120 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.us49.i.us.i.i.i.i, i1 true) #21, !range !45
  %offset.us50.i.us.i.i.i.i = zext i32 %120 to i64
  %121 = getelementptr inbounds i8, i8* %ptr.0134.us142.i.i.i.i, i64 %offset.us50.i.us.i.i.i.i
  %_37.us51.i.us.i.i.i.i = icmp ult i8* %89, %121
  br i1 %_37.us51.i.us.i.i.i.i, label %bb18.us146.i.i.i.i, label %bb2.i.us52.i.us.i.i.i.i

bb2.i.us52.i.us.i.i.i.i:                          ; preds = %bb10.us48.i.us.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !399) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !402) #21
  %122 = getelementptr inbounds i8, i8* %121, i64 %_38.i.i.i.i.i.i
  br label %bb20.i.us.i.us.i.i.i.i

bb20.i.us.i.us.i.i.i.i:                           ; preds = %bb24.i.us.i.us.i.i.i.i, %bb2.i.us52.i.us.i.i.i.i
  %py.073.i.us.i.us.i.i.i.i = phi i8* [ %124, %bb24.i.us.i.us.i.i.i.i ], [ %121, %bb2.i.us52.i.us.i.i.i.i ]
  %px.072.i.us.i.us.i.i.i.i = phi i8* [ %123, %bb24.i.us.i.us.i.i.i.i ], [ %91, %bb2.i.us52.i.us.i.i.i.i ]
  %_50.i.us.i.us.i.i.i.i = bitcast i8* %px.072.i.us.i.us.i.i.i.i to i32*
  %_50.val.i.us.i.us.i.i.i.i = load i32, i32* %_50.i.us.i.us.i.i.i.i, align 1, !alias.scope !404, !noalias !405
  %_53.i.us.i.us.i.i.i.i = bitcast i8* %py.073.i.us.i.us.i.i.i.i to i32*
  %_53.val.i.us.i.us.i.i.i.i = load i32, i32* %_53.i.us.i.us.i.i.i.i, align 1, !alias.scope !406, !noalias !407
  %_55.not.i.us.i.us.i.i.i.i = icmp eq i32 %_50.val.i.us.i.us.i.i.i.i, %_53.val.i.us.i.us.i.i.i.i
  br i1 %_55.not.i.us.i.us.i.i.i.i, label %bb24.i.us.i.us.i.i.i.i, label %bb19.us53.i.us.i.i.i.i

bb24.i.us.i.us.i.i.i.i:                           ; preds = %bb20.i.us.i.us.i.i.i.i
  %123 = getelementptr inbounds i8, i8* %px.072.i.us.i.us.i.i.i.i, i64 4
  %124 = getelementptr inbounds i8, i8* %py.073.i.us.i.us.i.i.i.i, i64 4
  %_46.i.us.i.us.i.i.i.i = icmp ult i8* %123, %90
  br i1 %_46.i.us.i.us.i.i.i.i, label %bb20.i.us.i.us.i.i.i.i, label %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i

_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i: ; preds = %bb24.i.us.i.us.i.i.i.i
  %_63.val.i.us.i.us.i.i.i.i = load i32, i32* %_63.i.i.i.i.i.i, align 1, !alias.scope !404, !noalias !405
  %_66.i.us.i.us.i.i.i.i = bitcast i8* %122 to i32*
  %_66.val.i.us.i.us.i.i.i.i = load i32, i32* %_66.i.us.i.us.i.i.i.i, align 1, !alias.scope !406, !noalias !407
  %125 = icmp eq i32 %_63.val.i.us.i.us.i.i.i.i, %_66.val.i.us.i.us.i.i.i.i
  br i1 %125, label %bb34.sink.split.i.i.i.i, label %bb19.us53.i.us.i.i.i.i

bb19.us53.i.us.i.i.i.i:                           ; preds = %bb20.i.us.i.us.i.i.i.i, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i.us.i.i.i.i
  %_51.us54.i.us.i.i.i.i = add i32 %match_offsets.042.us49.i.us.i.i.i.i, -1
  %126 = and i32 %_51.us54.i.us.i.i.i.i, %match_offsets.042.us49.i.us.i.i.i.i
  %127 = icmp eq i32 %126, 0
  br i1 %127, label %bb18.us146.i.i.i.i, label %bb10.us48.i.us.i.i.i.i

bb18.us146.i.i.i.i:                               ; preds = %bb19.us53.i.us.i.i.i.i, %bb10.us48.i.us.i.i.i.i, %bb14.us141.i.i.i.i
  %128 = getelementptr inbounds i8, i8* %ptr.0134.us142.i.i.i.i, i64 16
  %_45.not.us147.i.i.i.i = icmp ugt i8* %128, %87
  br i1 %_45.not.us147.i.i.i.i, label %bb21.i.i.i.i, label %bb14.us141.i.i.i.i

panic2.i.i.i.i:                                   ; preds = %bb6.i.i.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_2.i.i.i, i64 %6, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1051 to %"core::panic::location::Location"*)) #22, !noalias !393
  unreachable

bb21.i.i.i.i:                                     ; preds = %bb18.i.i.i.i, %bb18.us146.i.i.i.i, %bb18.us.i.i.i.i, %bb8.i.i.i.i
  %ptr.0.lcssa.i.i.i.i = phi i8* [ %84, %bb8.i.i.i.i ], [ %109, %bb18.us.i.i.i.i ], [ %128, %bb18.us146.i.i.i.i ], [ %145, %bb18.i.i.i.i ]
  %_65.i.i.i.i = icmp ult i8* %ptr.0.lcssa.i.i.i.i, %85
  br i1 %_65.i.i.i.i, label %bb22.i.i.i.i, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb14.i.i.i.i:                                     ; preds = %bb14.lr.ph.split.i.i.i.i, %bb18.i.i.i.i
  %ptr.0134.i.i.i.i = phi i8* [ %145, %bb18.i.i.i.i ], [ %84, %bb14.lr.ph.split.i.i.i.i ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !394)
  %129 = getelementptr inbounds i8, i8* %ptr.0134.i.i.i.i, i64 %_21.i.i.i.i
  %130 = bitcast i8* %129 to <16 x i8>*
  %.0.copyload28.i.i.i.i.i = load <16 x i8>, <16 x i8>* %130, align 1, !alias.scope !397, !noalias !398
  %131 = getelementptr inbounds i8, i8* %ptr.0134.i.i.i.i, i64 %_2.i.i.i
  %132 = bitcast i8* %131 to <16 x i8>*
  %.0.copyload629.i.i.i.i.i = load <16 x i8>, <16 x i8>* %132, align 1, !alias.scope !397, !noalias !398
  %133 = icmp eq <16 x i8> %.0.copyload28.i.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i
  %134 = icmp eq <16 x i8> %.0.copyload629.i.i.i.i.i, %.15.vec.insert.i.i.i26.i.i.i.i
  %135 = and <16 x i1> %134, %133
  %136 = bitcast <16 x i1> %135 to i16
  %137 = icmp eq i16 %136, 0
  br i1 %137, label %bb18.i.i.i.i, label %bb10.i.preheader.i.i.i.i

bb10.i.preheader.i.i.i.i:                         ; preds = %bb14.i.i.i.i
  %138 = zext i16 %136 to i32
  br label %bb10.i.i.i.i.i

bb10.i.i.i.i.i:                                   ; preds = %bb19.i.i.i.i.i, %bb10.i.preheader.i.i.i.i
  %match_offsets.042.i.i.i.i.i = phi i32 [ %143, %bb19.i.i.i.i.i ], [ %138, %bb10.i.preheader.i.i.i.i ]
  %139 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.i.i.i.i.i, i1 true) #21, !range !45
  %offset.i.i.i.i.i = zext i32 %139 to i64
  %140 = getelementptr inbounds i8, i8* %ptr.0134.i.i.i.i, i64 %offset.i.i.i.i.i
  %_37.i.i.i.i.i = icmp ult i8* %89, %140
  br i1 %_37.i.i.i.i.i, label %bb18.i.i.i.i, label %bb2.i.i.i.i.i.i

bb2.i.i.i.i.i.i:                                  ; preds = %bb10.i.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !399) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !402) #21
  %141 = getelementptr inbounds i8, i8* %140, i64 %_38.i.i.i.i.i.i
  %_63.val.i.i.i.i.i.i = load i32, i32* %_63.i.i.i.i.i.i, align 1, !alias.scope !404, !noalias !405
  %_66.i.i.i.i.i.i = bitcast i8* %141 to i32*
  %_66.val.i.i.i.i.i.i = load i32, i32* %_66.i.i.i.i.i.i, align 1, !alias.scope !406, !noalias !407
  %142 = icmp eq i32 %_63.val.i.i.i.i.i.i, %_66.val.i.i.i.i.i.i
  br i1 %142, label %bb34.sink.split.i.i.i.i, label %bb19.i.i.i.i.i

bb19.i.i.i.i.i:                                   ; preds = %bb2.i.i.i.i.i.i
  %_51.i.i.i.i.i = add i32 %match_offsets.042.i.i.i.i.i, -1
  %143 = and i32 %_51.i.i.i.i.i, %match_offsets.042.i.i.i.i.i
  %144 = icmp eq i32 %143, 0
  br i1 %144, label %bb18.i.i.i.i, label %bb10.i.i.i.i.i

bb18.i.i.i.i:                                     ; preds = %bb19.i.i.i.i.i, %bb10.i.i.i.i.i, %bb14.i.i.i.i
  %145 = getelementptr inbounds i8, i8* %ptr.0134.i.i.i.i, i64 16
  %_45.not.i.i.i.i = icmp ugt i8* %145, %87
  br i1 %_45.not.i.i.i.i, label %bb21.i.i.i.i, label %bb14.i.i.i.i

bb22.i.i.i.i:                                     ; preds = %bb21.i.i.i.i
  %_3.i.i.i.i.i = ptrtoint i8* %85 to i64
  %_5.i.i.i.i.i = ptrtoint i8* %ptr.0.lcssa.i.i.i.i to i64
  %146 = sub i64 %_3.i.i.i.i.i, %_5.i.i.i.i.i
  %_72.i.i.i.i = icmp ult i64 %146, %6
  br i1 %_72.i.i.i.i, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb25.i.i.i.i

bb25.i.i.i.i:                                     ; preds = %bb22.i.i.i.i
  %_5.i98.i.i.i.i = ptrtoint i8* %87 to i64
  %147 = sub i64 %_5.i.i.i.i.i, %_5.i98.i.i.i.i
  %148 = trunc i64 %147 to i32
  %149 = and i32 %148, 31
  %notmask.i.i.i.i = shl nsw i32 -1, %149
  tail call void @llvm.experimental.noalias.scope.decl(metadata !408)
  %150 = getelementptr inbounds i8, i8* %87, i64 %_21.i.i.i.i
  %151 = bitcast i8* %150 to <16 x i8>*
  %.0.copyload28.i28.i.i.i.i = load <16 x i8>, <16 x i8>* %151, align 1, !alias.scope !397, !noalias !411
  %152 = getelementptr inbounds i8, i8* %85, i64 -16
  %153 = bitcast i8* %152 to <16 x i8>*
  %.0.copyload629.i30.i.i.i.i = load <16 x i8>, <16 x i8>* %153, align 1, !alias.scope !397, !noalias !411
  %154 = icmp eq <16 x i8> %.0.copyload28.i28.i.i.i.i, %.15.vec.insert.i.i.i.i.i.i.i
  %155 = icmp eq <16 x i8> %.0.copyload629.i30.i.i.i.i, %.15.vec.insert.i.i.i26.i.i.i.i
  %156 = and <16 x i1> %155, %154
  %157 = bitcast <16 x i1> %156 to i16
  %158 = zext i16 %157 to i32
  %159 = and i32 %notmask.i.i.i.i, %158
  %160 = icmp eq i32 %159, 0
  br i1 %160, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb10.lr.ph.i35.i.i.i.i

bb10.lr.ph.i35.i.i.i.i:                           ; preds = %bb25.i.i.i.i
  %_8.i.i34.i.i.i.i = icmp ult i64 %6, 4
  br i1 %_8.i.i34.i.i.i.i, label %bb10.us.i39.i.i.i.i.preheader, label %bb10.lr.ph.split.i50.i.i.i.i

bb10.us.i39.i.i.i.i.preheader:                    ; preds = %bb10.lr.ph.i35.i.i.i.i
  %exitcond.not.i.us.i42.i.i.i.i = icmp eq i64 %6, 0
  %161 = getelementptr inbounds [0 x i8], [0 x i8]* %4, i64 0, i64 0
  %exitcond.not.i.us.i42.i.i.i.i.1 = icmp eq i64 %6, 1
  %162 = getelementptr inbounds [0 x i8], [0 x i8]* %4, i64 0, i64 1
  %exitcond.not.i.us.i42.i.i.i.i.2 = icmp eq i64 %6, 2
  %163 = getelementptr inbounds [0 x i8], [0 x i8]* %4, i64 0, i64 2
  %exitcond.not.i.us.i42.i.i.i.i.3 = icmp eq i64 %6, 3
  br label %bb10.us.i39.i.i.i.i

bb10.us.i39.i.i.i.i:                              ; preds = %bb10.us.i39.i.i.i.i.preheader, %bb19.loopexit.us.i49.i.i.i.i
  %match_offsets.042.us.i36.i.i.i.i = phi i32 [ %166, %bb19.loopexit.us.i49.i.i.i.i ], [ %159, %bb10.us.i39.i.i.i.i.preheader ]
  %164 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.us.i36.i.i.i.i, i1 true) #21, !range !45
  %offset.us.i37.i.i.i.i = zext i32 %164 to i64
  %165 = getelementptr inbounds i8, i8* %87, i64 %offset.us.i37.i.i.i.i
  %_37.us.i38.i.i.i.i = icmp ult i8* %89, %165
  br i1 %_37.us.i38.i.i.i.i, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb2.i.us.i40.i.i.i.i

bb2.i.us.i40.i.i.i.i:                             ; preds = %bb10.us.i39.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !412) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !415) #21
  br i1 %exitcond.not.i.us.i42.i.i.i.i, label %bb34.sink.split.i.i.i.i, label %bb9.i.us.i47.i.i.i.i

bb9.i.us.i47.i.i.i.i:                             ; preds = %bb2.i.us.i40.i.i.i.i
  %b1.i.us.i44.i.i.i.i = load i8, i8* %161, align 1, !alias.scope !417, !noalias !418
  %b2.i.us.i45.i.i.i.i = load i8, i8* %165, align 1, !alias.scope !419, !noalias !420
  %_23.not.i.us.i46.i.i.i.i = icmp eq i8 %b1.i.us.i44.i.i.i.i, %b2.i.us.i45.i.i.i.i
  br i1 %_23.not.i.us.i46.i.i.i.i, label %bb7.i.us.i43.i.i.i.i.1, label %bb19.loopexit.us.i49.i.i.i.i

bb19.loopexit.us.i49.i.i.i.i:                     ; preds = %bb9.i.us.i47.i.i.i.i.2, %bb9.i.us.i47.i.i.i.i.1, %bb9.i.us.i47.i.i.i.i
  %_51.us.i48.i.i.i.i = add i32 %match_offsets.042.us.i36.i.i.i.i, -1
  %166 = and i32 %_51.us.i48.i.i.i.i, %match_offsets.042.us.i36.i.i.i.i
  %167 = icmp eq i32 %166, 0
  br i1 %167, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb10.us.i39.i.i.i.i

bb10.lr.ph.split.i50.i.i.i.i:                     ; preds = %bb10.lr.ph.i35.i.i.i.i
  br i1 %_4671.i.i.i.i.i.i, label %bb10.us48.i54.i.i.i.i, label %bb10.i75.i.i.i.i

bb10.us48.i54.i.i.i.i:                            ; preds = %bb10.lr.ph.split.i50.i.i.i.i, %bb19.us53.i67.i.i.i.i
  %match_offsets.042.us49.i51.i.i.i.i = phi i32 [ %173, %bb19.us53.i67.i.i.i.i ], [ %159, %bb10.lr.ph.split.i50.i.i.i.i ]
  %168 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.us49.i51.i.i.i.i, i1 true) #21, !range !45
  %offset.us50.i52.i.i.i.i = zext i32 %168 to i64
  %169 = getelementptr inbounds i8, i8* %87, i64 %offset.us50.i52.i.i.i.i
  %_37.us51.i53.i.i.i.i = icmp ult i8* %89, %169
  br i1 %_37.us51.i53.i.i.i.i, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb2.i.us52.i55.i.i.i.i

bb2.i.us52.i55.i.i.i.i:                           ; preds = %bb10.us48.i54.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !412) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !415) #21
  %170 = getelementptr inbounds i8, i8* %169, i64 %_38.i.i.i.i.i.i
  br label %bb20.i.us.i63.i.i.i.i

bb20.i.us.i63.i.i.i.i:                            ; preds = %bb24.i.us.i65.i.i.i.i, %bb2.i.us52.i55.i.i.i.i
  %py.073.i.us.i56.i.i.i.i = phi i8* [ %172, %bb24.i.us.i65.i.i.i.i ], [ %169, %bb2.i.us52.i55.i.i.i.i ]
  %px.072.i.us.i57.i.i.i.i = phi i8* [ %171, %bb24.i.us.i65.i.i.i.i ], [ %91, %bb2.i.us52.i55.i.i.i.i ]
  %_50.i.us.i58.i.i.i.i = bitcast i8* %px.072.i.us.i57.i.i.i.i to i32*
  %_50.val.i.us.i59.i.i.i.i = load i32, i32* %_50.i.us.i58.i.i.i.i, align 1, !alias.scope !417, !noalias !418
  %_53.i.us.i60.i.i.i.i = bitcast i8* %py.073.i.us.i56.i.i.i.i to i32*
  %_53.val.i.us.i61.i.i.i.i = load i32, i32* %_53.i.us.i60.i.i.i.i, align 1, !alias.scope !419, !noalias !420
  %_55.not.i.us.i62.i.i.i.i = icmp eq i32 %_50.val.i.us.i59.i.i.i.i, %_53.val.i.us.i61.i.i.i.i
  br i1 %_55.not.i.us.i62.i.i.i.i, label %bb24.i.us.i65.i.i.i.i, label %bb19.us53.i67.i.i.i.i

bb24.i.us.i65.i.i.i.i:                            ; preds = %bb20.i.us.i63.i.i.i.i
  %171 = getelementptr inbounds i8, i8* %px.072.i.us.i57.i.i.i.i, i64 4
  %172 = getelementptr inbounds i8, i8* %py.073.i.us.i56.i.i.i.i, i64 4
  %_46.i.us.i64.i.i.i.i = icmp ult i8* %171, %90
  br i1 %_46.i.us.i64.i.i.i.i, label %bb20.i.us.i63.i.i.i.i, label %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i

bb19.us53.i67.i.i.i.i:                            ; preds = %bb20.i.us.i63.i.i.i.i, %_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i
  %_51.us54.i66.i.i.i.i = add i32 %match_offsets.042.us49.i51.i.i.i.i, -1
  %173 = and i32 %_51.us54.i66.i.i.i.i, %match_offsets.042.us49.i51.i.i.i.i
  %174 = icmp eq i32 %173, 0
  br i1 %174, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb10.us48.i54.i.i.i.i

_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E.exit.loopexit.us.i71.i.i.i.i: ; preds = %bb24.i.us.i65.i.i.i.i
  %_63.val.i.us.i68.i.i.i.i = load i32, i32* %_63.i.i.i.i.i.i, align 1, !alias.scope !417, !noalias !418
  %_66.i.us.i69.i.i.i.i = bitcast i8* %170 to i32*
  %_66.val.i.us.i70.i.i.i.i = load i32, i32* %_66.i.us.i69.i.i.i.i, align 1, !alias.scope !419, !noalias !420
  %175 = icmp eq i32 %_63.val.i.us.i68.i.i.i.i, %_66.val.i.us.i70.i.i.i.i
  br i1 %175, label %bb34.sink.split.i.i.i.i, label %bb19.us53.i67.i.i.i.i

bb10.i75.i.i.i.i:                                 ; preds = %bb10.lr.ph.split.i50.i.i.i.i, %bb19.i81.i.i.i.i
  %match_offsets.042.i72.i.i.i.i = phi i32 [ %180, %bb19.i81.i.i.i.i ], [ %159, %bb10.lr.ph.split.i50.i.i.i.i ]
  %176 = tail call i32 @llvm.cttz.i32(i32 %match_offsets.042.i72.i.i.i.i, i1 true) #21, !range !45
  %offset.i73.i.i.i.i = zext i32 %176 to i64
  %177 = getelementptr inbounds i8, i8* %87, i64 %offset.i73.i.i.i.i
  %_37.i74.i.i.i.i = icmp ult i8* %89, %177
  br i1 %_37.i74.i.i.i.i, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb2.i.i79.i.i.i.i

bb2.i.i79.i.i.i.i:                                ; preds = %bb10.i75.i.i.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !412) #21
  tail call void @llvm.experimental.noalias.scope.decl(metadata !415) #21
  %178 = getelementptr inbounds i8, i8* %177, i64 %_38.i.i.i.i.i.i
  %_63.val.i.i76.i.i.i.i = load i32, i32* %_63.i.i.i.i.i.i, align 1, !alias.scope !417, !noalias !418
  %_66.i.i77.i.i.i.i = bitcast i8* %178 to i32*
  %_66.val.i.i78.i.i.i.i = load i32, i32* %_66.i.i77.i.i.i.i, align 1, !alias.scope !419, !noalias !420
  %179 = icmp eq i32 %_63.val.i.i76.i.i.i.i, %_66.val.i.i78.i.i.i.i
  br i1 %179, label %bb34.sink.split.i.i.i.i, label %bb19.i81.i.i.i.i

bb19.i81.i.i.i.i:                                 ; preds = %bb2.i.i79.i.i.i.i
  %_51.i80.i.i.i.i = add i32 %match_offsets.042.i72.i.i.i.i, -1
  %180 = and i32 %_51.i80.i.i.i.i, %match_offsets.042.i72.i.i.i.i
  %181 = icmp eq i32 %180, 0
  br i1 %181, label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit, label %bb10.i75.i.i.i.i

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i": ; preds = %bb16.i
  %_35.idx.i = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 3, i32 0, i32 0
  %_35.idx.val.i = load i32, i32* %_35.idx.i, align 8, !alias.scope !340, !noalias !352
  %_35.idx27.i = getelementptr %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 3, i32 0, i32 1
  %_35.idx27.val.i = load i32, i32* %_35.idx27.i, align 4, !alias.scope !340, !noalias !352
  %start1.i.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %_12.i9.i.i.i = icmp eq i64 %6, 0
  br i1 %_12.i9.i.i.i, label %bb6.i.i.preheader, label %bb5.i.preheader.i.i

bb5.i.preheader.i.i:                              ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i"
  %182 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %183 = add i64 %6, -1
  %xtraiter = and i64 %6, 7
  %184 = icmp ult i64 %183, 7
  br i1 %184, label %bb6.i.i.preheader.loopexit.unr-lcssa, label %bb5.i.preheader.i.i.new

bb5.i.preheader.i.i.new:                          ; preds = %bb5.i.preheader.i.i
  %unroll_iter = and i64 %6, -8
  br label %bb5.i.i.i

bb5.i.i.i:                                        ; preds = %bb5.i.i.i, %bb5.i.preheader.i.i.new
  %hash.011.i.i.i = phi i32 [ 0, %bb5.i.preheader.i.i.new ], [ %208, %bb5.i.i.i ]
  %iter.sroa.0.010.i.i.i = phi i8* [ %182, %bb5.i.preheader.i.i.new ], [ %206, %bb5.i.i.i ]
  %niter = phi i64 [ %unroll_iter, %bb5.i.preheader.i.i.new ], [ %niter.nsub.7, %bb5.i.i.i ]
  %185 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 1
  %b.i.i.i = load i8, i8* %iter.sroa.0.010.i.i.i, align 1, !alias.scope !421, !noalias !426
  %186 = shl i32 %hash.011.i.i.i, 1
  %_6.i.i.i.i = zext i8 %b.i.i.i to i32
  %187 = add i32 %186, %_6.i.i.i.i
  %188 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 2
  %b.i.i.i.1 = load i8, i8* %185, align 1, !alias.scope !421, !noalias !426
  %189 = shl i32 %187, 1
  %_6.i.i.i.i.1 = zext i8 %b.i.i.i.1 to i32
  %190 = add i32 %189, %_6.i.i.i.i.1
  %191 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 3
  %b.i.i.i.2 = load i8, i8* %188, align 1, !alias.scope !421, !noalias !426
  %192 = shl i32 %190, 1
  %_6.i.i.i.i.2 = zext i8 %b.i.i.i.2 to i32
  %193 = add i32 %192, %_6.i.i.i.i.2
  %194 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 4
  %b.i.i.i.3 = load i8, i8* %191, align 1, !alias.scope !421, !noalias !426
  %195 = shl i32 %193, 1
  %_6.i.i.i.i.3 = zext i8 %b.i.i.i.3 to i32
  %196 = add i32 %195, %_6.i.i.i.i.3
  %197 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 5
  %b.i.i.i.4 = load i8, i8* %194, align 1, !alias.scope !421, !noalias !426
  %198 = shl i32 %196, 1
  %_6.i.i.i.i.4 = zext i8 %b.i.i.i.4 to i32
  %199 = add i32 %198, %_6.i.i.i.i.4
  %200 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 6
  %b.i.i.i.5 = load i8, i8* %197, align 1, !alias.scope !421, !noalias !426
  %201 = shl i32 %199, 1
  %_6.i.i.i.i.5 = zext i8 %b.i.i.i.5 to i32
  %202 = add i32 %201, %_6.i.i.i.i.5
  %203 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 7
  %b.i.i.i.6 = load i8, i8* %200, align 1, !alias.scope !421, !noalias !426
  %204 = shl i32 %202, 1
  %_6.i.i.i.i.6 = zext i8 %b.i.i.i.6 to i32
  %205 = add i32 %204, %_6.i.i.i.i.6
  %206 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i, i64 8
  %b.i.i.i.7 = load i8, i8* %203, align 1, !alias.scope !421, !noalias !426
  %207 = shl i32 %205, 1
  %_6.i.i.i.i.7 = zext i8 %b.i.i.i.7 to i32
  %208 = add i32 %207, %_6.i.i.i.i.7
  %niter.nsub.7 = add i64 %niter, -8
  %niter.ncmp.7 = icmp eq i64 %niter.nsub.7, 0
  br i1 %niter.ncmp.7, label %bb6.i.i.preheader.loopexit.unr-lcssa, label %bb5.i.i.i

bb6.i.i.preheader.loopexit.unr-lcssa:             ; preds = %bb5.i.i.i, %bb5.i.preheader.i.i
  %.lcssa212.ph = phi i32 [ undef, %bb5.i.preheader.i.i ], [ %208, %bb5.i.i.i ]
  %hash.011.i.i.i.unr = phi i32 [ 0, %bb5.i.preheader.i.i ], [ %208, %bb5.i.i.i ]
  %iter.sroa.0.010.i.i.i.unr = phi i8* [ %182, %bb5.i.preheader.i.i ], [ %206, %bb5.i.i.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %bb6.i.i.preheader, label %bb5.i.i.i.epil

bb5.i.i.i.epil:                                   ; preds = %bb6.i.i.preheader.loopexit.unr-lcssa, %bb5.i.i.i.epil
  %hash.011.i.i.i.epil = phi i32 [ %211, %bb5.i.i.i.epil ], [ %hash.011.i.i.i.unr, %bb6.i.i.preheader.loopexit.unr-lcssa ]
  %iter.sroa.0.010.i.i.i.epil = phi i8* [ %209, %bb5.i.i.i.epil ], [ %iter.sroa.0.010.i.i.i.unr, %bb6.i.i.preheader.loopexit.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb5.i.i.i.epil ], [ %xtraiter, %bb6.i.i.preheader.loopexit.unr-lcssa ]
  %209 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i.i.epil, i64 1
  %b.i.i.i.epil = load i8, i8* %iter.sroa.0.010.i.i.i.epil, align 1, !alias.scope !421, !noalias !426
  %210 = shl i32 %hash.011.i.i.i.epil, 1
  %_6.i.i.i.i.epil = zext i8 %b.i.i.i.epil to i32
  %211 = add i32 %210, %_6.i.i.i.i.epil
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %bb6.i.i.preheader, label %bb5.i.i.i.epil, !llvm.loop !428

bb6.i.i.preheader:                                ; preds = %bb6.i.i.preheader.loopexit.unr-lcssa, %bb5.i.i.i.epil, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i"
  %hash.0.i.i.ph = phi i32 [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i" ], [ %.lcssa212.ph, %bb6.i.i.preheader.loopexit.unr-lcssa ], [ %211, %bb5.i.i.i.epil ]
  br label %bb6.i.i

bb6.i.i:                                          ; preds = %bb6.i.i.preheader, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i"
  %hash.0.i.i = phi i32 [ %218, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i" ], [ %hash.0.i.i.ph, %bb6.i.i.preheader ]
  %haystack.sroa.13.0.i.i = phi i64 [ %_7.i.i.i.i.i.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i" ], [ %haystack.1, %bb6.i.i.preheader ]
  %haystack.sroa.0.0.i.i = phi [0 x i8]* [ %220, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i" ], [ %haystack.0, %bb6.i.i.preheader ]
  %212 = icmp eq i32 %hash.0.i.i, %_35.idx.val.i
  br i1 %212, label %bb8.i.i, label %bb14.i.i

bb8.i.i:                                          ; preds = %bb6.i.i
; call memchr::memmem::rabinkarp::is_prefix
  %_24.i.i = tail call fastcc zeroext i1 @_ZN6memchr6memmem9rabinkarp9is_prefix17h9e377cf0e3b7406bE([0 x i8]* noalias nonnull readonly align 1 %haystack.sroa.0.0.i.i, i64 %haystack.sroa.13.0.i.i, [0 x i8]* noalias nonnull readonly align 1 %4, i64 %6), !noalias !363
  br i1 %_24.i.i, label %bb12.i.i, label %bb14.i.i

bb14.i.i:                                         ; preds = %bb8.i.i, %bb6.i.i
  %_32.not.i.i = icmp ugt i64 %haystack.sroa.13.0.i.i, %6
  br i1 %_32.not.i.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i", label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb12.i.i:                                         ; preds = %bb8.i.i
  %_28.i.i = ptrtoint [0 x i8]* %haystack.sroa.0.0.i.i to i64
  %_27.i.i = sub i64 %_28.i.i, %start1.i.i
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i": ; preds = %bb14.i.i
  %213 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i.i, i64 0, i64 0
  %_41.i.i = load i8, i8* %213, align 1, !alias.scope !429, !noalias !426
  %214 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i.i, i64 0, i64 %6
  %_45.i.i = load i8, i8* %214, align 1, !alias.scope !429, !noalias !426
  %_8.i.i.i.i = zext i8 %_41.i.i to i32
  %215 = mul i32 %_35.idx27.val.i, %_8.i.i.i.i
  %216 = sub i32 %hash.0.i.i, %215
  %217 = shl i32 %216, 1
  %_6.i1.i.i.i = zext i8 %_45.i.i to i32
  %218 = add i32 %217, %_6.i1.i.i.i
  %219 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i.i, i64 0, i64 1
  %_7.i.i.i.i.i.i = add i64 %haystack.sroa.13.0.i.i, -1
  %220 = bitcast i8* %219 to [0 x i8]*
  br label %bb6.i.i

bb13.i:                                           ; preds = %bb9.i
  %tw.i = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 2, i32 1, i64 7
  %221 = bitcast i8* %tw.i to %"memmem::twoway::Forward"*
; call memchr::memmem::Searcher::find_tw
  %222 = call fastcc { i64, i64 } @_ZN6memchr6memmem8Searcher7find_tw17h619e5351c4063bc3E(%"memmem::Searcher"* noalias nonnull readonly align 8 dereferenceable(80) %_3, %"memmem::twoway::Forward"* noalias nonnull readonly align 8 dereferenceable(32) %221, { i32, i32 }* noalias nonnull align 4 dereferenceable(8) %_6, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1, [0 x i8]* noalias nonnull readonly align 1 %4, i64 %6)
  %.fca.0.extract18.i = extractvalue { i64, i64 } %222, 0
  %.fca.1.extract20.i = extractvalue { i64, i64 } %222, 1
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i37.i": ; preds = %bb9.i
  %_20.idx.i = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 3, i32 0, i32 0
  %_20.idx.val.i = load i32, i32* %_20.idx.i, align 8, !alias.scope !340, !noalias !352
  %_20.idx26.i = getelementptr %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0, i32 3, i32 0, i32 1
  %_20.idx26.val.i = load i32, i32* %_20.idx26.i, align 4, !alias.scope !340, !noalias !352
  %start1.i35.i = ptrtoint [0 x i8]* %haystack.0 to i64
  %_12.i9.i.i36.i = icmp eq i64 %6, 0
  br i1 %_12.i9.i.i36.i, label %bb6.i52.i.preheader, label %bb5.i.preheader.i38.i

bb5.i.preheader.i38.i:                            ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i37.i"
  %223 = getelementptr [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 0
  %224 = add i64 %6, -1
  %xtraiter242 = and i64 %6, 7
  %225 = icmp ult i64 %224, 7
  br i1 %225, label %bb6.i52.i.preheader.loopexit.unr-lcssa, label %bb5.i.preheader.i38.i.new

bb5.i.preheader.i38.i.new:                        ; preds = %bb5.i.preheader.i38.i
  %unroll_iter246 = and i64 %6, -8
  br label %bb5.i.i44.i

bb5.i.i44.i:                                      ; preds = %bb5.i.i44.i, %bb5.i.preheader.i38.i.new
  %hash.011.i.i39.i = phi i32 [ 0, %bb5.i.preheader.i38.i.new ], [ %249, %bb5.i.i44.i ]
  %iter.sroa.0.010.i.i40.i = phi i8* [ %223, %bb5.i.preheader.i38.i.new ], [ %247, %bb5.i.i44.i ]
  %niter247 = phi i64 [ %unroll_iter246, %bb5.i.preheader.i38.i.new ], [ %niter247.nsub.7, %bb5.i.i44.i ]
  %226 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 1
  %b.i.i41.i = load i8, i8* %iter.sroa.0.010.i.i40.i, align 1, !alias.scope !430, !noalias !435
  %227 = shl i32 %hash.011.i.i39.i, 1
  %_6.i.i.i42.i = zext i8 %b.i.i41.i to i32
  %228 = add i32 %227, %_6.i.i.i42.i
  %229 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 2
  %b.i.i41.i.1 = load i8, i8* %226, align 1, !alias.scope !430, !noalias !435
  %230 = shl i32 %228, 1
  %_6.i.i.i42.i.1 = zext i8 %b.i.i41.i.1 to i32
  %231 = add i32 %230, %_6.i.i.i42.i.1
  %232 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 3
  %b.i.i41.i.2 = load i8, i8* %229, align 1, !alias.scope !430, !noalias !435
  %233 = shl i32 %231, 1
  %_6.i.i.i42.i.2 = zext i8 %b.i.i41.i.2 to i32
  %234 = add i32 %233, %_6.i.i.i42.i.2
  %235 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 4
  %b.i.i41.i.3 = load i8, i8* %232, align 1, !alias.scope !430, !noalias !435
  %236 = shl i32 %234, 1
  %_6.i.i.i42.i.3 = zext i8 %b.i.i41.i.3 to i32
  %237 = add i32 %236, %_6.i.i.i42.i.3
  %238 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 5
  %b.i.i41.i.4 = load i8, i8* %235, align 1, !alias.scope !430, !noalias !435
  %239 = shl i32 %237, 1
  %_6.i.i.i42.i.4 = zext i8 %b.i.i41.i.4 to i32
  %240 = add i32 %239, %_6.i.i.i42.i.4
  %241 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 6
  %b.i.i41.i.5 = load i8, i8* %238, align 1, !alias.scope !430, !noalias !435
  %242 = shl i32 %240, 1
  %_6.i.i.i42.i.5 = zext i8 %b.i.i41.i.5 to i32
  %243 = add i32 %242, %_6.i.i.i42.i.5
  %244 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 7
  %b.i.i41.i.6 = load i8, i8* %241, align 1, !alias.scope !430, !noalias !435
  %245 = shl i32 %243, 1
  %_6.i.i.i42.i.6 = zext i8 %b.i.i41.i.6 to i32
  %246 = add i32 %245, %_6.i.i.i42.i.6
  %247 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i, i64 8
  %b.i.i41.i.7 = load i8, i8* %244, align 1, !alias.scope !430, !noalias !435
  %248 = shl i32 %246, 1
  %_6.i.i.i42.i.7 = zext i8 %b.i.i41.i.7 to i32
  %249 = add i32 %248, %_6.i.i.i42.i.7
  %niter247.nsub.7 = add i64 %niter247, -8
  %niter247.ncmp.7 = icmp eq i64 %niter247.nsub.7, 0
  br i1 %niter247.ncmp.7, label %bb6.i52.i.preheader.loopexit.unr-lcssa, label %bb5.i.i44.i

bb6.i52.i.preheader.loopexit.unr-lcssa:           ; preds = %bb5.i.i44.i, %bb5.i.preheader.i38.i
  %.lcssa209.ph = phi i32 [ undef, %bb5.i.preheader.i38.i ], [ %249, %bb5.i.i44.i ]
  %hash.011.i.i39.i.unr = phi i32 [ 0, %bb5.i.preheader.i38.i ], [ %249, %bb5.i.i44.i ]
  %iter.sroa.0.010.i.i40.i.unr = phi i8* [ %223, %bb5.i.preheader.i38.i ], [ %247, %bb5.i.i44.i ]
  %lcmp.mod244.not = icmp eq i64 %xtraiter242, 0
  br i1 %lcmp.mod244.not, label %bb6.i52.i.preheader, label %bb5.i.i44.i.epil

bb5.i.i44.i.epil:                                 ; preds = %bb6.i52.i.preheader.loopexit.unr-lcssa, %bb5.i.i44.i.epil
  %hash.011.i.i39.i.epil = phi i32 [ %252, %bb5.i.i44.i.epil ], [ %hash.011.i.i39.i.unr, %bb6.i52.i.preheader.loopexit.unr-lcssa ]
  %iter.sroa.0.010.i.i40.i.epil = phi i8* [ %250, %bb5.i.i44.i.epil ], [ %iter.sroa.0.010.i.i40.i.unr, %bb6.i52.i.preheader.loopexit.unr-lcssa ]
  %epil.iter243 = phi i64 [ %epil.iter243.sub, %bb5.i.i44.i.epil ], [ %xtraiter242, %bb6.i52.i.preheader.loopexit.unr-lcssa ]
  %250 = getelementptr inbounds i8, i8* %iter.sroa.0.010.i.i40.i.epil, i64 1
  %b.i.i41.i.epil = load i8, i8* %iter.sroa.0.010.i.i40.i.epil, align 1, !alias.scope !430, !noalias !435
  %251 = shl i32 %hash.011.i.i39.i.epil, 1
  %_6.i.i.i42.i.epil = zext i8 %b.i.i41.i.epil to i32
  %252 = add i32 %251, %_6.i.i.i42.i.epil
  %epil.iter243.sub = add i64 %epil.iter243, -1
  %epil.iter243.cmp.not = icmp eq i64 %epil.iter243.sub, 0
  br i1 %epil.iter243.cmp.not, label %bb6.i52.i.preheader, label %bb5.i.i44.i.epil, !llvm.loop !437

bb6.i52.i.preheader:                              ; preds = %bb6.i52.i.preheader.loopexit.unr-lcssa, %bb5.i.i44.i.epil, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i37.i"
  %hash.0.i49.i.ph = phi i32 [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i37.i" ], [ %.lcssa209.ph, %bb6.i52.i.preheader.loopexit.unr-lcssa ], [ %252, %bb5.i.i44.i.epil ]
  br label %bb6.i52.i

bb6.i52.i:                                        ; preds = %bb6.i52.i.preheader, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i"
  %hash.0.i49.i = phi i32 [ %259, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i" ], [ %hash.0.i49.i.ph, %bb6.i52.i.preheader ]
  %haystack.sroa.13.0.i50.i = phi i64 [ %_7.i.i.i.i.i64.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i" ], [ %haystack.1, %bb6.i52.i.preheader ]
  %haystack.sroa.0.0.i51.i = phi [0 x i8]* [ %261, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i" ], [ %haystack.0, %bb6.i52.i.preheader ]
  %253 = icmp eq i32 %hash.0.i49.i, %_20.idx.val.i
  br i1 %253, label %bb8.i54.i, label %bb14.i56.i

bb8.i54.i:                                        ; preds = %bb6.i52.i
; call memchr::memmem::rabinkarp::is_prefix
  %_24.i53.i = tail call fastcc zeroext i1 @_ZN6memchr6memmem9rabinkarp9is_prefix17h9e377cf0e3b7406bE([0 x i8]* noalias nonnull readonly align 1 %haystack.sroa.0.0.i51.i, i64 %haystack.sroa.13.0.i50.i, [0 x i8]* noalias nonnull readonly align 1 %4, i64 %6), !noalias !363
  br i1 %_24.i53.i, label %bb12.i59.i, label %bb14.i56.i

bb14.i56.i:                                       ; preds = %bb8.i54.i, %bb6.i52.i
  %_32.not.i55.i = icmp ugt i64 %haystack.sroa.13.0.i50.i, %6
  br i1 %_32.not.i55.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i", label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

bb12.i59.i:                                       ; preds = %bb8.i54.i
  %_28.i57.i = ptrtoint [0 x i8]* %haystack.sroa.0.0.i51.i to i64
  %_27.i58.i = sub i64 %_28.i57.i, %start1.i35.i
  br label %_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i65.i": ; preds = %bb14.i56.i
  %254 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i51.i, i64 0, i64 0
  %_41.i60.i = load i8, i8* %254, align 1, !alias.scope !438, !noalias !435
  %255 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i51.i, i64 0, i64 %6
  %_45.i61.i = load i8, i8* %255, align 1, !alias.scope !438, !noalias !435
  %_8.i.i.i62.i = zext i8 %_41.i60.i to i32
  %256 = mul i32 %_20.idx26.val.i, %_8.i.i.i62.i
  %257 = sub i32 %hash.0.i49.i, %256
  %258 = shl i32 %257, 1
  %_6.i1.i.i63.i = zext i8 %_45.i61.i to i32
  %259 = add i32 %258, %_6.i1.i.i63.i
  %260 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.sroa.0.0.i51.i, i64 0, i64 1
  %_7.i.i.i.i.i64.i = add i64 %haystack.sroa.13.0.i50.i, -1
  %261 = bitcast i8* %260 to [0 x i8]*
  br label %bb6.i52.i

_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE.exit: ; preds = %bb10.i75.i.i.i.i, %bb19.i81.i.i.i.i, %bb10.us48.i54.i.i.i.i, %bb19.us53.i67.i.i.i.i, %bb10.us.i39.i.i.i.i, %bb19.loopexit.us.i49.i.i.i.i, %bb14.i.i, %bb14.i56.i, %bb10.i.i.i.i32.i, %start, %bb3.i, %bb7.i, %bb8.i.i.i.i.i, %bb15.i.i.i.i.i, %bb42.i.i.i.i.i, %bb46.i.i.i.i.i, %bb52.i.i.i.i.i, %bb50.i.i.i.i.i, %bb65.i.i.i.i.i, %bb62.i.i.i.i.i, %bb66.i.i.i.i.i, %bb4.i59.i.i.i.i.i, %bb34.sink.split.i.i.i.i, %bb21.i.i.i.i, %bb22.i.i.i.i, %bb25.i.i.i.i, %bb12.i.i, %bb13.i, %bb12.i59.i
  %.sroa.10.0.i = phi i64 [ %.fca.1.extract20.i, %bb13.i ], [ undef, %start ], [ %_11.i, %bb3.i ], [ undef, %bb7.i ], [ %19, %bb8.i.i.i.i.i ], [ %21, %bb15.i.i.i.i.i ], [ %_13.i.i.i.i.i.i, %bb62.i.i.i.i.i ], [ %_122.i.i.i.i.i, %bb52.i.i.i.i.i ], [ %_116.i.i.i.i.i, %bb50.i.i.i.i.i ], [ %_109.i.i.i.i.i, %bb46.i.i.i.i.i ], [ %_102.i.i.i.i.i, %bb42.i.i.i.i.i ], [ undef, %bb65.i.i.i.i.i ], [ %_13.i58.i.i.i.i.i, %bb4.i59.i.i.i.i.i ], [ undef, %bb66.i.i.i.i.i ], [ undef, %bb21.i.i.i.i ], [ undef, %bb22.i.i.i.i ], [ undef, %bb25.i.i.i.i ], [ %_58.i.i.i.i, %bb34.sink.split.i.i.i.i ], [ %_27.i.i, %bb12.i.i ], [ %_27.i58.i, %bb12.i59.i ], [ undef, %bb10.i.i.i.i32.i ], [ undef, %bb14.i56.i ], [ undef, %bb14.i.i ], [ undef, %bb19.loopexit.us.i49.i.i.i.i ], [ undef, %bb10.us.i39.i.i.i.i ], [ undef, %bb19.us53.i67.i.i.i.i ], [ undef, %bb10.us48.i54.i.i.i.i ], [ undef, %bb19.i81.i.i.i.i ], [ undef, %bb10.i75.i.i.i.i ]
  %.sroa.0.0.i = phi i64 [ %.fca.0.extract18.i, %bb13.i ], [ 0, %start ], [ 1, %bb3.i ], [ 0, %bb7.i ], [ 1, %bb8.i.i.i.i.i ], [ 1, %bb15.i.i.i.i.i ], [ 1, %bb62.i.i.i.i.i ], [ 1, %bb52.i.i.i.i.i ], [ 1, %bb50.i.i.i.i.i ], [ 1, %bb46.i.i.i.i.i ], [ 1, %bb42.i.i.i.i.i ], [ 0, %bb65.i.i.i.i.i ], [ 1, %bb4.i59.i.i.i.i.i ], [ 0, %bb66.i.i.i.i.i ], [ 0, %bb21.i.i.i.i ], [ 0, %bb22.i.i.i.i ], [ 0, %bb25.i.i.i.i ], [ 1, %bb34.sink.split.i.i.i.i ], [ 1, %bb12.i.i ], [ 1, %bb12.i59.i ], [ 0, %bb10.i.i.i.i32.i ], [ 0, %bb14.i56.i ], [ 0, %bb14.i.i ], [ 0, %bb19.loopexit.us.i49.i.i.i.i ], [ 0, %bb10.us.i39.i.i.i.i ], [ 0, %bb19.us53.i67.i.i.i.i ], [ 0, %bb10.us48.i54.i.i.i.i ], [ 0, %bb19.i81.i.i.i.i ], [ 0, %bb10.i75.i.i.i.i ]
  %262 = insertvalue { i64, i64 } undef, i64 %.sroa.0.0.i, 0
  %263 = insertvalue { i64, i64 } %262, i64 %.sroa.10.0.i, 1
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %0)
  ret { i64, i64 } %263

bb7.i.us.i43.i.i.i.i.1:                           ; preds = %bb9.i.us.i47.i.i.i.i
  br i1 %exitcond.not.i.us.i42.i.i.i.i.1, label %bb34.sink.split.i.i.i.i, label %bb9.i.us.i47.i.i.i.i.1

bb9.i.us.i47.i.i.i.i.1:                           ; preds = %bb7.i.us.i43.i.i.i.i.1
  %264 = getelementptr inbounds i8, i8* %165, i64 1
  %b1.i.us.i44.i.i.i.i.1 = load i8, i8* %162, align 1, !alias.scope !417, !noalias !418
  %b2.i.us.i45.i.i.i.i.1 = load i8, i8* %264, align 1, !alias.scope !419, !noalias !420
  %_23.not.i.us.i46.i.i.i.i.1 = icmp eq i8 %b1.i.us.i44.i.i.i.i.1, %b2.i.us.i45.i.i.i.i.1
  br i1 %_23.not.i.us.i46.i.i.i.i.1, label %bb7.i.us.i43.i.i.i.i.2, label %bb19.loopexit.us.i49.i.i.i.i

bb7.i.us.i43.i.i.i.i.2:                           ; preds = %bb9.i.us.i47.i.i.i.i.1
  br i1 %exitcond.not.i.us.i42.i.i.i.i.2, label %bb34.sink.split.i.i.i.i, label %bb9.i.us.i47.i.i.i.i.2

bb9.i.us.i47.i.i.i.i.2:                           ; preds = %bb7.i.us.i43.i.i.i.i.2
  %265 = getelementptr inbounds i8, i8* %165, i64 2
  %b1.i.us.i44.i.i.i.i.2 = load i8, i8* %163, align 1, !alias.scope !417, !noalias !418
  %b2.i.us.i45.i.i.i.i.2 = load i8, i8* %265, align 1, !alias.scope !419, !noalias !420
  %_23.not.i.us.i46.i.i.i.i.2 = icmp eq i8 %b1.i.us.i44.i.i.i.i.2, %b2.i.us.i45.i.i.i.i.2
  %_23.not.i.us.i46.i.i.i.i.2.not = xor i1 %_23.not.i.us.i46.i.i.i.i.2, true
  %exitcond.not.i.us.i42.i.i.i.i.3.not = xor i1 %exitcond.not.i.us.i42.i.i.i.i.3, true
  %brmerge = select i1 %_23.not.i.us.i46.i.i.i.i.2.not, i1 true, i1 %exitcond.not.i.us.i42.i.i.i.i.3.not
  br i1 %brmerge, label %bb19.loopexit.us.i49.i.i.i.i, label %bb34.sink.split.i.i.i.i
}

; memchr::memmem::FinderBuilder::new
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn
define zeroext i1 @_ZN6memchr6memmem13FinderBuilder3new17h6f660de5680092b7E() unnamed_addr #7 {
start:
  ret i1 true
}

; memchr::memmem::FinderBuilder::prefilter
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind uwtable willreturn writeonly
define nonnull align 1 dereferenceable(1) i8* @_ZN6memchr6memmem13FinderBuilder9prefilter17h354912e242dedfbfE(i8* noalias returned align 1 dereferenceable(1) %self, i1 zeroext %prefilter) unnamed_addr #9 {
start:
  %0 = zext i1 %prefilter to i8
  store i8 %0, i8* %self, align 1
  ret i8* %self
}

; memchr::memmem::Searcher::new
; Function Attrs: nonlazybind uwtable
define void @_ZN6memchr6memmem8Searcher3new17h16090b51c0ef424aE(%"memmem::Searcher"* noalias nocapture sret(%"memmem::Searcher") dereferenceable(80) %0, i1 zeroext %1, [0 x i8]* noalias nonnull readonly align 1 %needle.0, i64 %needle.1) unnamed_addr #6 personality i32 (...)* @rust_eh_personality {
start:
  %_85.i.i = alloca %"core::option::Option<core::fmt::Arguments>", align 8
  %rare2i.i.i = alloca i8, align 1
  %rare1i.i.i = alloca i8, align 1
  %kind.sroa.11.sroa.0 = alloca [5 x i8], align 1
  tail call void @llvm.experimental.noalias.scope.decl(metadata !439)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !442)
  %needle.1.off.i.i = add i64 %needle.1, -2
  %2 = icmp ugt i64 %needle.1.off.i.i, 253
  br i1 %2, label %_ZN6memchr6memmem9rarebytes15RareNeedleBytes7forward17h554d0b979d1378b1E.exit.i, label %bb6.i.i

bb6.i.i:                                          ; preds = %start
  %3 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %_13.i.i = load i8, i8* %3, align 1, !alias.scope !445
  call void @llvm.lifetime.start.p0i8(i64 1, i8* nonnull %rare1i.i.i), !noalias !445
  %4 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 1
  %_20.i.i = load i8, i8* %4, align 1, !alias.scope !445
  call void @llvm.lifetime.start.p0i8(i64 1, i8* nonnull %rare2i.i.i), !noalias !445
  %_4.i.i.i = zext i8 %_20.i.i to i64
  %5 = getelementptr inbounds <{ [256 x i8] }>, <{ [256 x i8] }>* @0, i64 0, i32 0, i64 %_4.i.i.i
  %_2.i.i.i = load i8, i8* %5, align 1, !noalias !445
  %_4.i9.i.i = zext i8 %_13.i.i to i64
  %6 = getelementptr inbounds <{ [256 x i8] }>, <{ [256 x i8] }>* @0, i64 0, i32 0, i64 %_4.i9.i.i
  %_2.i10.i.i = load i8, i8* %6, align 1, !noalias !445
  %_24.i.i = icmp ult i8 %_2.i.i.i, %_2.i10.i.i
  br i1 %_24.i.i, label %bb10.i.i, label %bb13.i.i

bb13.i.i:                                         ; preds = %bb10.i.i, %bb6.i.i
  %rare2i.i.promoted.i = phi i8 [ 0, %bb10.i.i ], [ 1, %bb6.i.i ]
  %rare1i.i.promoted.i = phi i8 [ 1, %bb10.i.i ], [ 0, %bb6.i.i ]
  %rare2.0.i.i = phi i8 [ %_13.i.i, %bb10.i.i ], [ %_20.i.i, %bb6.i.i ]
  %rare1.0.i.i = phi i8 [ %_20.i.i, %bb10.i.i ], [ %_13.i.i, %bb6.i.i ]
  %7 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %needle.1
  %_18.i.i.i.i.i = ptrtoint i8* %7 to i64
  br label %bb18.i.i.outer

bb10.i.i:                                         ; preds = %bb6.i.i
  br label %bb13.i.i

bb18.i.i:                                         ; preds = %bb18.i.i.backedge, %bb18.i.i.outer89
  %iter.sroa.0.0.i.i = phi i8* [ %iter.sroa.0.0.i.i.ph93, %bb18.i.i.outer89 ], [ %10, %bb18.i.i.backedge ]
  %iter.sroa.11.0.i.i = phi i64 [ %iter.sroa.11.0.i.i.ph94, %bb18.i.i.outer89 ], [ %_11.0.i.i.i.i, %bb18.i.i.backedge ]
  %_3.not.i.i.i = phi i1 [ %_3.not.i.i.i.ph95, %bb18.i.i.outer89 ], [ true, %bb18.i.i.backedge ]
  %iter.sroa.16.0.i.i = phi i64 [ %iter.sroa.16.0.i.i.ph96, %bb18.i.i.outer89 ], [ 0, %bb18.i.i.backedge ]
  br i1 %_3.not.i.i.i, label %bb6.i.i.i, label %bb2.i.i.i

bb6.i.i.i:                                        ; preds = %bb3.i1.i.i.i, %bb18.i.i
  %iter.sroa.11.1.i.i = phi i64 [ %_17.0.i.i.i.i, %bb3.i1.i.i.i ], [ %iter.sroa.11.0.i.i, %bb18.i.i ]
  %_15.i.i.i.i.i = phi i8* [ %9, %bb3.i1.i.i.i ], [ %iter.sroa.0.0.i.i, %bb18.i.i ]
  %_12.i.i.i.i.i = icmp eq i8* %_15.i.i.i.i.i, %7
  br i1 %_12.i.i.i.i.i, label %bb22.i.i, label %"_ZN100_$LT$core..iter..adapters..skip..Skip$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h317c6309fd4a8fdcE.exit.i.i"

bb2.i.i.i:                                        ; preds = %bb18.i.i
  %_7.i.i.i = add nsw i64 %iter.sroa.16.0.i.i, -1
  %_20.i.i.i.i.i = ptrtoint i8* %iter.sroa.0.0.i.i to i64
  %8 = sub nuw i64 %_18.i.i.i.i.i, %_20.i.i.i.i.i
  %_3.not.i.i.i.i.i = icmp ugt i64 %8, %_7.i.i.i
  br i1 %_3.not.i.i.i.i.i, label %bb3.i1.i.i.i, label %bb22.i.i

bb3.i1.i.i.i:                                     ; preds = %bb2.i.i.i
  %9 = getelementptr inbounds i8, i8* %iter.sroa.0.0.i.i, i64 %iter.sroa.16.0.i.i
  %_17.0.i.i.i.i = add i64 %iter.sroa.16.0.i.i, %iter.sroa.11.0.i.i
  br label %bb6.i.i.i

"_ZN100_$LT$core..iter..adapters..skip..Skip$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h317c6309fd4a8fdcE.exit.i.i": ; preds = %bb6.i.i.i
  %10 = getelementptr inbounds i8, i8* %_15.i.i.i.i.i, i64 1
  %_11.0.i.i.i.i = add i64 %iter.sroa.11.1.i.i, 1
  %b.i.i = load i8, i8* %_15.i.i.i.i.i, align 1, !alias.scope !445
  %_4.i13.i.i = zext i8 %b.i.i to i64
  %11 = getelementptr inbounds <{ [256 x i8] }>, <{ [256 x i8] }>* @0, i64 0, i32 0, i64 %_4.i13.i.i
  %_2.i14.i.i = load i8, i8* %11, align 1
  %_2.i16.i.i = load i8, i8* %13, align 1
  %_51.i.i = icmp ult i8 %_2.i14.i.i, %_2.i16.i.i
  br i1 %_51.i.i, label %bb25.i.i, label %bb26.i.i

bb22.i.i:                                         ; preds = %bb2.i.i.i, %bb6.i.i.i
  store i8 %_78.i.i.ph91, i8* %rare2i.i.i, align 1, !noalias !445
  store i8 %_77.i.i.ph, i8* %rare1i.i.i, align 1, !noalias !445
  %_76.i.i = icmp eq i8 %_77.i.i.ph, %_78.i.i.ph91
  br i1 %_76.i.i, label %bb35.i.i, label %bb36.i.i

bb26.i.i:                                         ; preds = %"_ZN100_$LT$core..iter..adapters..skip..Skip$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h317c6309fd4a8fdcE.exit.i.i"
  %_61.not.i.i = icmp eq i8 %b.i.i, %rare1.1.i.i.ph
  br i1 %_61.not.i.i, label %bb18.i.i.backedge, label %bb28.i.i

bb25.i.i:                                         ; preds = %"_ZN100_$LT$core..iter..adapters..skip..Skip$LT$I$GT$$u20$as$u20$core..iter..traits..iterator..Iterator$GT$4next17h317c6309fd4a8fdcE.exit.i.i"
  %12 = trunc i64 %iter.sroa.11.1.i.i to i8
  br label %bb18.i.i.outer

bb18.i.i.outer:                                   ; preds = %bb25.i.i, %bb13.i.i
  %_78.i.i.ph = phi i8 [ %_77.i.i.ph, %bb25.i.i ], [ %rare2i.i.promoted.i, %bb13.i.i ]
  %_77.i.i.ph = phi i8 [ %12, %bb25.i.i ], [ %rare1i.i.promoted.i, %bb13.i.i ]
  %iter.sroa.0.0.i.i.ph = phi i8* [ %10, %bb25.i.i ], [ %3, %bb13.i.i ]
  %iter.sroa.11.0.i.i.ph = phi i64 [ %_11.0.i.i.i.i, %bb25.i.i ], [ 0, %bb13.i.i ]
  %_3.not.i.i.i.ph = phi i1 [ true, %bb25.i.i ], [ false, %bb13.i.i ]
  %iter.sroa.16.0.i.i.ph = phi i64 [ 0, %bb25.i.i ], [ 2, %bb13.i.i ]
  %rare2.1.i.i.ph = phi i8 [ %rare1.1.i.i.ph, %bb25.i.i ], [ %rare2.0.i.i, %bb13.i.i ]
  %rare1.1.i.i.ph = phi i8 [ %b.i.i, %bb25.i.i ], [ %rare1.0.i.i, %bb13.i.i ]
  %_4.i15.i.i = zext i8 %rare1.1.i.i.ph to i64
  %13 = getelementptr inbounds <{ [256 x i8] }>, <{ [256 x i8] }>* @0, i64 0, i32 0, i64 %_4.i15.i.i
  br label %bb18.i.i.outer89

bb18.i.i.outer89:                                 ; preds = %bb18.i.i.outer, %bb32.i.i
  %_78.i.i.ph91 = phi i8 [ %_78.i.i.ph, %bb18.i.i.outer ], [ %15, %bb32.i.i ]
  %iter.sroa.0.0.i.i.ph93 = phi i8* [ %iter.sroa.0.0.i.i.ph, %bb18.i.i.outer ], [ %10, %bb32.i.i ]
  %iter.sroa.11.0.i.i.ph94 = phi i64 [ %iter.sroa.11.0.i.i.ph, %bb18.i.i.outer ], [ %_11.0.i.i.i.i, %bb32.i.i ]
  %_3.not.i.i.i.ph95 = phi i1 [ %_3.not.i.i.i.ph, %bb18.i.i.outer ], [ true, %bb32.i.i ]
  %iter.sroa.16.0.i.i.ph96 = phi i64 [ %iter.sroa.16.0.i.i.ph, %bb18.i.i.outer ], [ 0, %bb32.i.i ]
  %rare2.1.i.i.ph97 = phi i8 [ %rare2.1.i.i.ph, %bb18.i.i.outer ], [ %b.i.i, %bb32.i.i ]
  %_4.i19.i.i = zext i8 %rare2.1.i.i.ph97 to i64
  %14 = getelementptr inbounds <{ [256 x i8] }>, <{ [256 x i8] }>* @0, i64 0, i32 0, i64 %_4.i19.i.i
  br label %bb18.i.i

bb28.i.i:                                         ; preds = %bb26.i.i
  %_2.i20.i.i = load i8, i8* %14, align 1
  %_64.i.i = icmp ult i8 %_2.i14.i.i, %_2.i20.i.i
  br i1 %_64.i.i, label %bb32.i.i, label %bb18.i.i.backedge

bb18.i.i.backedge:                                ; preds = %bb28.i.i, %bb26.i.i
  br label %bb18.i.i

bb32.i.i:                                         ; preds = %bb28.i.i
  %15 = trunc i64 %iter.sroa.11.1.i.i to i8
  br label %bb18.i.i.outer89

bb36.i.i:                                         ; preds = %bb22.i.i
  call void @llvm.lifetime.end.p0i8(i64 1, i8* nonnull %rare2i.i.i), !noalias !445
  call void @llvm.lifetime.end.p0i8(i64 1, i8* nonnull %rare1i.i.i), !noalias !445
  br label %_ZN6memchr6memmem9rarebytes15RareNeedleBytes7forward17h554d0b979d1378b1E.exit.i

bb35.i.i:                                         ; preds = %bb22.i.i
  %16 = bitcast %"core::option::Option<core::fmt::Arguments>"* %_85.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %16), !noalias !445
  %17 = getelementptr inbounds %"core::option::Option<core::fmt::Arguments>", %"core::option::Option<core::fmt::Arguments>"* %_85.i.i, i64 0, i32 0
  store {}* null, {}** %17, align 8, !noalias !445
; call core::panicking::assert_failed
  call fastcc void @_ZN4core9panicking13assert_failed17h2b1ecd6dd4ba2033E(i8* noalias nonnull readonly align 1 dereferenceable(1) %rare1i.i.i, i8* noalias nonnull readonly align 1 dereferenceable(1) %rare2i.i.i, %"core::option::Option<core::fmt::Arguments>"* noalias nocapture nonnull dereferenceable(48) %_85.i.i) #22
  unreachable

_ZN6memchr6memmem9rarebytes15RareNeedleBytes7forward17h554d0b979d1378b1E.exit.i: ; preds = %bb36.i.i, %start
  %.sroa.3.0.i.i = phi i8 [ %_78.i.i.ph91, %bb36.i.i ], [ 0, %start ]
  %.sroa.0.0.i.i = phi i8 [ %_77.i.i.ph, %bb36.i.i ], [ 0, %start ]
  %18 = icmp eq i64 %needle.1, 0
  br i1 %18, label %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread, label %bb5.i.i

_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread: ; preds = %_ZN6memchr6memmem9rarebytes15RareNeedleBytes7forward17h554d0b979d1378b1E.exit.i
  %kind.sroa.11.sroa.0.0.sroa_idx72 = getelementptr inbounds [5 x i8], [5 x i8]* %kind.sroa.11.sroa.0, i64 0, i64 0
  call void @llvm.lifetime.start.p0i8(i64 5, i8* nonnull %kind.sroa.11.sroa.0.0.sroa_idx72)
  br label %bb18

bb5.i.i:                                          ; preds = %_ZN6memchr6memmem9rarebytes15RareNeedleBytes7forward17h554d0b979d1378b1E.exit.i
  %19 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %_8.i.i = load i8, i8* %19, align 1, !alias.scope !446
  %_6.i.i.i = zext i8 %_8.i.i to i32
  %20 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %needle.1
  %_18.i.i.i.i = ptrtoint i8* %20 to i64
  br label %bb10.i5.i

bb10.i5.i:                                        ; preds = %bb12.i.i, %bb5.i.i
  %iter.sroa.0.0.i3.i = phi i8* [ %19, %bb5.i.i ], [ %23, %bb12.i.i ]
  %_3.not.i.i4.i = phi i1 [ false, %bb5.i.i ], [ true, %bb12.i.i ]
  %iter.sroa.10.0.i.i = phi i64 [ 1, %bb5.i.i ], [ 0, %bb12.i.i ]
  %nh.sroa.10.0.i.i = phi i32 [ 1, %bb5.i.i ], [ %26, %bb12.i.i ]
  %nh.sroa.0.0.i.i = phi i32 [ %_6.i.i.i, %bb5.i.i ], [ %25, %bb12.i.i ]
  br i1 %_3.not.i.i4.i, label %bb6.i.i6.i, label %bb2.i.i8.i

bb6.i.i6.i:                                       ; preds = %bb18.i.i.i.i, %bb10.i5.i
  %_15.i.i.i.i = phi i8* [ %iter.sroa.0.0.i3.i, %bb10.i5.i ], [ %22, %bb18.i.i.i.i ]
  %_12.i.i.i.i = icmp eq i8* %_15.i.i.i.i, %20
  br i1 %_12.i.i.i.i, label %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit, label %bb12.i.i

bb2.i.i8.i:                                       ; preds = %bb10.i5.i
  %_7.i.i7.i = add nsw i64 %iter.sroa.10.0.i.i, -1
  %_20.i.i.i.i = ptrtoint i8* %iter.sroa.0.0.i3.i to i64
  %21 = sub nuw i64 %_18.i.i.i.i, %_20.i.i.i.i
  %_3.not.i.i.i.i = icmp ugt i64 %21, %_7.i.i7.i
  br i1 %_3.not.i.i.i.i, label %bb18.i.i.i.i, label %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit

bb18.i.i.i.i:                                     ; preds = %bb2.i.i8.i
  %22 = getelementptr inbounds i8, i8* %iter.sroa.0.0.i3.i, i64 %iter.sroa.10.0.i.i
  br label %bb6.i.i6.i

bb12.i.i:                                         ; preds = %bb6.i.i6.i
  %23 = getelementptr inbounds i8, i8* %_15.i.i.i.i, i64 1
  %b.i9.i = load i8, i8* %_15.i.i.i.i, align 1, !alias.scope !446
  %24 = shl i32 %nh.sroa.0.0.i.i, 1
  %_6.i5.i.i = zext i8 %b.i9.i to i32
  %25 = add i32 %24, %_6.i5.i.i
  %26 = shl i32 %nh.sroa.10.0.i.i, 1
  br label %bb10.i5.i

_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit: ; preds = %bb6.i.i6.i, %bb2.i.i8.i
  %kind.sroa.11.sroa.0.0.sroa_idx = getelementptr inbounds [5 x i8], [5 x i8]* %kind.sroa.11.sroa.0, i64 0, i64 0
  call void @llvm.lifetime.start.p0i8(i64 5, i8* nonnull %kind.sroa.11.sroa.0.0.sroa_idx)
  switch i64 %needle.1, label %bb7 [
    i64 0, label %bb18
    i64 1, label %bb6
  ]

bb7:                                              ; preds = %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit
  %_2.not.i.i.i = icmp ugt i8 %.sroa.0.0.i.i, %.sroa.3.0.i.i
  %_3._4.i.i.i = select i1 %_2.not.i.i.i, i8 %.sroa.0.0.i.i, i8 %.sroa.3.0.i.i
  %_4._3.i.i.i = select i1 %_2.not.i.i.i, i8 %.sroa.3.0.i.i, i8 %.sroa.0.0.i.i
  %27 = icmp ult i64 %needle.1.off.i.i, 31
  %_15.i.i = icmp ne i8 %_4._3.i.i.i, %_3._4.i.i.i
  %spec.select.i.i = select i1 %27, i1 %_15.i.i, i1 false
  %.sroa.4.0.insert.ext.i.i = zext i8 %_3._4.i.i.i to i24
  %.sroa.4.0.insert.shift.i.i = shl nuw i24 %.sroa.4.0.insert.ext.i.i, 16
  %.sroa.3.0.insert.ext.i.i = zext i8 %_4._3.i.i.i to i24
  %.sroa.3.0.insert.shift.i.i = shl nuw nsw i24 %.sroa.3.0.insert.ext.i.i, 8
  %.sroa.3.0.insert.insert.i.i = or i24 %.sroa.4.0.insert.shift.i.i, %.sroa.3.0.insert.shift.i.i
  %.sroa.3.0.insert.insert.i1.i = select i1 %spec.select.i.i, i24 %.sroa.3.0.insert.insert.i.i, i24 0
  %.sroa.4.0.extract.shift = lshr exact i24 %.sroa.3.0.insert.insert.i1.i, 8
  %.sroa.4.0.extract.trunc = trunc i24 %.sroa.4.0.extract.shift to i8
  %.sroa.5.0.extract.shift = lshr i24 %.sroa.3.0.insert.insert.i1.i, 16
  %.sroa.5.0.extract.trunc = trunc i24 %.sroa.5.0.extract.shift to i8
  br i1 %spec.select.i.i, label %bb18, label %bb4.preheader.i.i

bb4.preheader.i.i:                                ; preds = %bb7
  %28 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %29 = add i64 %needle.1, -1
  %xtraiter = and i64 %needle.1, 3
  %30 = icmp ult i64 %29, 3
  br i1 %30, label %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa, label %bb4.preheader.i.i.new

bb4.preheader.i.i.new:                            ; preds = %bb4.preheader.i.i
  %unroll_iter = and i64 %needle.1, -4
  br label %bb4.i.i

bb4.i.i:                                          ; preds = %bb4.i.i, %bb4.preheader.i.i.new
  %bits.012.i.i = phi i64 [ 0, %bb4.preheader.i.i.new ], [ %46, %bb4.i.i ]
  %iter.sroa.0.011.i.i = phi i8* [ %28, %bb4.preheader.i.i.new ], [ %43, %bb4.i.i ]
  %niter = phi i64 [ %unroll_iter, %bb4.preheader.i.i.new ], [ %niter.nsub.3, %bb4.i.i ]
  %31 = getelementptr inbounds i8, i8* %iter.sroa.0.011.i.i, i64 1
  %b.i.i14 = load i8, i8* %iter.sroa.0.011.i.i, align 1, !alias.scope !449, !noalias !454
  %32 = and i8 %b.i.i14, 63
  %33 = zext i8 %32 to i64
  %_11.i.i = shl nuw i64 1, %33
  %34 = or i64 %_11.i.i, %bits.012.i.i
  %35 = getelementptr inbounds i8, i8* %iter.sroa.0.011.i.i, i64 2
  %b.i.i14.1 = load i8, i8* %31, align 1, !alias.scope !449, !noalias !454
  %36 = and i8 %b.i.i14.1, 63
  %37 = zext i8 %36 to i64
  %_11.i.i.1 = shl nuw i64 1, %37
  %38 = or i64 %_11.i.i.1, %34
  %39 = getelementptr inbounds i8, i8* %iter.sroa.0.011.i.i, i64 3
  %b.i.i14.2 = load i8, i8* %35, align 1, !alias.scope !449, !noalias !454
  %40 = and i8 %b.i.i14.2, 63
  %41 = zext i8 %40 to i64
  %_11.i.i.2 = shl nuw i64 1, %41
  %42 = or i64 %_11.i.i.2, %38
  %43 = getelementptr inbounds i8, i8* %iter.sroa.0.011.i.i, i64 4
  %b.i.i14.3 = load i8, i8* %39, align 1, !alias.scope !449, !noalias !454
  %44 = and i8 %b.i.i14.3, 63
  %45 = zext i8 %44 to i64
  %_11.i.i.3 = shl nuw i64 1, %45
  %46 = or i64 %_11.i.i.3, %42
  %niter.nsub.3 = add i64 %niter, -4
  %niter.ncmp.3 = icmp eq i64 %niter.nsub.3, 0
  br i1 %niter.ncmp.3, label %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa, label %bb4.i.i

_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa: ; preds = %bb4.i.i, %bb4.preheader.i.i
  %.lcssa.ph = phi i64 [ undef, %bb4.preheader.i.i ], [ %46, %bb4.i.i ]
  %bits.012.i.i.unr = phi i64 [ 0, %bb4.preheader.i.i ], [ %46, %bb4.i.i ]
  %iter.sroa.0.011.i.i.unr = phi i8* [ %28, %bb4.preheader.i.i ], [ %43, %bb4.i.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i, label %bb4.i.i.epil

bb4.i.i.epil:                                     ; preds = %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa, %bb4.i.i.epil
  %bits.012.i.i.epil = phi i64 [ %50, %bb4.i.i.epil ], [ %bits.012.i.i.unr, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa ]
  %iter.sroa.0.011.i.i.epil = phi i8* [ %47, %bb4.i.i.epil ], [ %iter.sroa.0.011.i.i.unr, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb4.i.i.epil ], [ %xtraiter, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa ]
  %47 = getelementptr inbounds i8, i8* %iter.sroa.0.011.i.i.epil, i64 1
  %b.i.i14.epil = load i8, i8* %iter.sroa.0.011.i.i.epil, align 1, !alias.scope !449, !noalias !454
  %48 = and i8 %b.i.i14.epil, 63
  %49 = zext i8 %48 to i64
  %_11.i.i.epil = shl nuw i64 1, %49
  %50 = or i64 %_11.i.i.epil, %bits.012.i.i.epil
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i, label %bb4.i.i.epil, !llvm.loop !456

_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i: ; preds = %bb4.i.i.epil, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa
  %.lcssa = phi i64 [ %.lcssa.ph, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa ], [ %50, %bb4.i.i.epil ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !457)
  %_527.i.i = icmp ugt i64 %needle.1, 1
  br i1 %_527.i.i, label %bb2.i.i, label %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i

bb2.i.i:                                          ; preds = %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i, %bb13.i.i17
  %_633.i.i = phi i64 [ %_6.i.i, %bb13.i.i17 ], [ 1, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ]
  %suffix.sroa.0.032.i.i = phi i64 [ %suffix.sroa.0.1.i.i, %bb13.i.i17 ], [ 0, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ]
  %suffix.sroa.5.031.i.i = phi i64 [ %suffix.sroa.5.1.i.i, %bb13.i.i17 ], [ 1, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ]
  %candidate_start.029.i.i = phi i64 [ %candidate_start.1.i.i, %bb13.i.i17 ], [ 1, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ]
  %offset.028.i.i = phi i64 [ %offset.1.i.i, %bb13.i.i17 ], [ 0, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ]
  %_12.i.i = add i64 %offset.028.i.i, %suffix.sroa.0.032.i.i
  %_16.i.i = icmp ult i64 %_12.i.i, %needle.1
  br i1 %_16.i.i, label %bb4.i10.i, label %panic.i.i, !prof !60

panic.i.i:                                        ; preds = %bb2.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_12.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1161 to %"core::panic::location::Location"*)) #22, !noalias !460
  unreachable

bb4.i10.i:                                        ; preds = %bb2.i.i
  %51 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_12.i.i
  %current.i.i = load i8, i8* %51, align 1, !alias.scope !461, !noalias !454
  %52 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_633.i.i
  %candidate.i.i = load i8, i8* %52, align 1, !alias.scope !461, !noalias !454
  %_5.i.i.i = icmp ult i8 %candidate.i.i, %current.i.i
  br i1 %_5.i.i.i, label %bb8.i.i, label %bb4.i.i.i

bb4.i.i.i:                                        ; preds = %bb4.i10.i
  %_8.i.not.i.i = icmp ugt i8 %candidate.i.i, %current.i.i
  %_30.i.i = add i64 %offset.028.i.i, 1
  br i1 %_8.i.not.i.i, label %bb9.i.i, label %bb6.i.i16

bb8.i.i:                                          ; preds = %bb4.i10.i
  %53 = add i64 %candidate_start.029.i.i, 1
  br label %bb13.i.i17

bb9.i.i:                                          ; preds = %bb4.i.i.i
  %54 = add i64 %_30.i.i, %candidate_start.029.i.i
  %55 = sub i64 %54, %suffix.sroa.0.032.i.i
  br label %bb13.i.i17

bb6.i.i16:                                        ; preds = %bb4.i.i.i
  %_34.i.i = icmp eq i64 %_30.i.i, %suffix.sroa.5.031.i.i
  %spec.select.i.i15 = select i1 %_34.i.i, i64 0, i64 %_30.i.i
  %56 = select i1 %_34.i.i, i64 %suffix.sroa.5.031.i.i, i64 0
  %spec.select21.i.i = add i64 %56, %candidate_start.029.i.i
  br label %bb13.i.i17

bb13.i.i17:                                       ; preds = %bb6.i.i16, %bb9.i.i, %bb8.i.i
  %offset.1.i.i = phi i64 [ 0, %bb9.i.i ], [ 0, %bb8.i.i ], [ %spec.select.i.i15, %bb6.i.i16 ]
  %candidate_start.1.i.i = phi i64 [ %54, %bb9.i.i ], [ %53, %bb8.i.i ], [ %spec.select21.i.i, %bb6.i.i16 ]
  %suffix.sroa.5.1.i.i = phi i64 [ %55, %bb9.i.i ], [ 1, %bb8.i.i ], [ %suffix.sroa.5.031.i.i, %bb6.i.i16 ]
  %suffix.sroa.0.1.i.i = phi i64 [ %suffix.sroa.0.032.i.i, %bb9.i.i ], [ %candidate_start.029.i.i, %bb8.i.i ], [ %suffix.sroa.0.032.i.i, %bb6.i.i16 ]
  %_6.i.i = add i64 %candidate_start.1.i.i, %offset.1.i.i
  %_5.i.i = icmp ult i64 %_6.i.i, %needle.1
  br i1 %_5.i.i, label %bb2.i.i, label %bb2.us.i.preheader.i

bb2.us.i.preheader.i:                             ; preds = %bb13.i.i17
  %57 = insertvalue { i64, i64 } undef, i64 %suffix.sroa.0.1.i.i, 0
  %58 = insertvalue { i64, i64 } %57, i64 %suffix.sroa.5.1.i.i, 1
  tail call void @llvm.experimental.noalias.scope.decl(metadata !462)
  br label %bb2.us.i.i

bb2.us.i.i:                                       ; preds = %bb13.us.i.i, %bb2.us.i.preheader.i
  %_633.us.i.i = phi i64 [ %_6.us.i.i, %bb13.us.i.i ], [ 1, %bb2.us.i.preheader.i ]
  %suffix.sroa.0.032.us.i.i = phi i64 [ %suffix.sroa.0.1.us.i.i, %bb13.us.i.i ], [ 0, %bb2.us.i.preheader.i ]
  %suffix.sroa.5.031.us.i.i = phi i64 [ %suffix.sroa.5.1.us.i.i, %bb13.us.i.i ], [ 1, %bb2.us.i.preheader.i ]
  %candidate_start.029.us.i.i = phi i64 [ %candidate_start.1.us.i.i, %bb13.us.i.i ], [ 1, %bb2.us.i.preheader.i ]
  %offset.028.us.i.i = phi i64 [ %offset.1.us.i.i, %bb13.us.i.i ], [ 0, %bb2.us.i.preheader.i ]
  %_12.us.i.i = add i64 %offset.028.us.i.i, %suffix.sroa.0.032.us.i.i
  %_16.us.i.i = icmp ult i64 %_12.us.i.i, %needle.1
  br i1 %_16.us.i.i, label %bb4.us.i.i, label %panic.i15.i, !prof !60

bb4.us.i.i:                                       ; preds = %bb2.us.i.i
  %59 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_12.us.i.i
  %current.us.i.i = load i8, i8* %59, align 1, !alias.scope !465, !noalias !454
  %60 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_633.us.i.i
  %candidate.us.i.i = load i8, i8* %60, align 1, !alias.scope !465, !noalias !454
  %_11.i.us.i.i = icmp ugt i8 %candidate.us.i.i, %current.us.i.i
  br i1 %_11.i.us.i.i, label %bb8.us.i.i, label %bb9.i.us.i.i

bb9.i.us.i.i:                                     ; preds = %bb4.us.i.i
  %_14.i.us.not.i.i = icmp ult i8 %candidate.us.i.i, %current.us.i.i
  %_30.us.i.i = add i64 %offset.028.us.i.i, 1
  br i1 %_14.i.us.not.i.i, label %bb9.us.i.i, label %bb6.us.i.i

bb9.us.i.i:                                       ; preds = %bb9.i.us.i.i
  %61 = add i64 %_30.us.i.i, %candidate_start.029.us.i.i
  %62 = sub i64 %61, %suffix.sroa.0.032.us.i.i
  br label %bb13.us.i.i

bb6.us.i.i:                                       ; preds = %bb9.i.us.i.i
  %_34.us.i.i = icmp eq i64 %_30.us.i.i, %suffix.sroa.5.031.us.i.i
  %spec.select.us.i.i = select i1 %_34.us.i.i, i64 0, i64 %_30.us.i.i
  %63 = select i1 %_34.us.i.i, i64 %suffix.sroa.5.031.us.i.i, i64 0
  %spec.select21.us.i.i = add i64 %63, %candidate_start.029.us.i.i
  br label %bb13.us.i.i

bb8.us.i.i:                                       ; preds = %bb4.us.i.i
  %64 = add i64 %candidate_start.029.us.i.i, 1
  br label %bb13.us.i.i

bb13.us.i.i:                                      ; preds = %bb8.us.i.i, %bb6.us.i.i, %bb9.us.i.i
  %offset.1.us.i.i = phi i64 [ 0, %bb9.us.i.i ], [ 0, %bb8.us.i.i ], [ %spec.select.us.i.i, %bb6.us.i.i ]
  %candidate_start.1.us.i.i = phi i64 [ %61, %bb9.us.i.i ], [ %64, %bb8.us.i.i ], [ %spec.select21.us.i.i, %bb6.us.i.i ]
  %suffix.sroa.5.1.us.i.i = phi i64 [ %62, %bb9.us.i.i ], [ 1, %bb8.us.i.i ], [ %suffix.sroa.5.031.us.i.i, %bb6.us.i.i ]
  %suffix.sroa.0.1.us.i.i = phi i64 [ %suffix.sroa.0.032.us.i.i, %bb9.us.i.i ], [ %candidate_start.029.us.i.i, %bb8.us.i.i ], [ %suffix.sroa.0.032.us.i.i, %bb6.us.i.i ]
  %_6.us.i.i = add i64 %candidate_start.1.us.i.i, %offset.1.us.i.i
  %_5.us.i.i = icmp ult i64 %_6.us.i.i, %needle.1
  br i1 %_5.us.i.i, label %bb2.us.i.i, label %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i

panic.i15.i:                                      ; preds = %bb2.us.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_12.us.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1161 to %"core::panic::location::Location"*)) #22, !noalias !466
  unreachable

_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i: ; preds = %bb13.us.i.i, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i
  %65 = phi { i64, i64 } [ { i64 0, i64 1 }, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ], [ %58, %bb13.us.i.i ]
  %suffix.sroa.0.0.lcssa.i31.i = phi i64 [ 0, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ], [ %suffix.sroa.0.1.i.i, %bb13.us.i.i ]
  %suffix.sroa.5.0.lcssa.i13.i = phi i64 [ 1, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ], [ %suffix.sroa.5.1.us.i.i, %bb13.us.i.i ]
  %suffix.sroa.0.0.lcssa.i14.i = phi i64 [ 0, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ], [ %suffix.sroa.0.1.us.i.i, %bb13.us.i.i ]
  %66 = insertvalue { i64, i64 } undef, i64 %suffix.sroa.0.0.lcssa.i14.i, 0
  %67 = insertvalue { i64, i64 } %66, i64 %suffix.sroa.5.0.lcssa.i13.i, 1
  %_16.i = icmp ugt i64 %suffix.sroa.0.0.lcssa.i31.i, %suffix.sroa.0.0.lcssa.i14.i
  %..i = select i1 %_16.i, { i64, i64 } %65, { i64, i64 } %67
  %min_suffix.0.max_suffix.0.i = select i1 %_16.i, i64 %suffix.sroa.0.0.lcssa.i31.i, i64 %suffix.sroa.0.0.lcssa.i14.i
  %_15.sroa.0.0.i = extractvalue { i64, i64 } %..i, 1
  tail call void @llvm.experimental.noalias.scope.decl(metadata !467)
  %_6.i17.i = sub i64 %needle.1, %min_suffix.0.max_suffix.0.i
  %68 = icmp ult i64 %_6.i17.i, %min_suffix.0.max_suffix.0.i
  %.0.sroa.speculated.i.i.i.i.i = select i1 %68, i64 %min_suffix.0.max_suffix.0.i, i64 %_6.i17.i
  %_11.i18.i = shl i64 %min_suffix.0.max_suffix.0.i, 1
  %_10.not.i.i = icmp ult i64 %_11.i18.i, %needle.1
  br i1 %_10.not.i.i, label %bb3.i.i, label %bb18

bb3.i.i:                                          ; preds = %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i
  %_4.not.i.i.i = icmp ugt i64 %min_suffix.0.max_suffix.0.i, %needle.1
  br i1 %_4.not.i.i.i, label %bb1.i.i.i, label %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E.exit.i.i"

bb1.i.i.i:                                        ; preds = %bb3.i.i
; call core::panicking::panic
  tail call void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [35 x i8] }>* @alloc1359 to [0 x i8]*), i64 35, %"core::panic::location::Location"* noalias nonnull readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1153 to %"core::panic::location::Location"*)) #22, !noalias !470
  unreachable

"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E.exit.i.i": ; preds = %bb3.i.i
  %_8.i.i.i.i.i = icmp ult i64 %_6.i17.i, %_15.sroa.0.0.i
  br i1 %_8.i.i.i.i.i, label %bb3.i.i.i.i.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i"

bb3.i.i.i.i.i:                                    ; preds = %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E.exit.i.i"
; call core::slice::index::slice_end_index_len_fail
  tail call void @_ZN4core5slice5index24slice_end_index_len_fail17h7d511eec41d6bce9E(i64 %_15.sroa.0.0.i, i64 %_6.i17.i, %"core::panic::location::Location"* noalias nonnull readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1155 to %"core::panic::location::Location"*)) #22, !noalias !474
  unreachable

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i": ; preds = %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E.exit.i.i"
  tail call void @llvm.experimental.noalias.scope.decl(metadata !481)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !484)
  %_3.not.i.i.i18 = icmp ult i64 %_15.sroa.0.0.i, %min_suffix.0.max_suffix.0.i
  br i1 %_3.not.i.i.i18, label %.thread.i.i, label %bb2.i.i.i.i

bb2.i.i.i.i:                                      ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i"
  %69 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_15.sroa.0.0.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !486)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !489)
  %_8.i.i.i.i = icmp ult i64 %min_suffix.0.max_suffix.0.i, 4
  br i1 %_8.i.i.i.i, label %bb7.i.i.i.i, label %bb14.i.i.i.i

bb14.i.i.i.i:                                     ; preds = %bb2.i.i.i.i
  %_38.i.i.i.i = add i64 %min_suffix.0.max_suffix.0.i, -4
  %70 = getelementptr inbounds i8, i8* %69, i64 %_38.i.i.i.i
  %71 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_38.i.i.i.i
  %_4671.i.i.i.i = icmp sgt i64 %_38.i.i.i.i, 0
  br i1 %_4671.i.i.i.i, label %bb20.i.i.i.i, label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i

bb7.i.i.i.i:                                      ; preds = %bb2.i.i.i.i, %bb9.i.i.i.i
  %iter.sroa.9.0.i.i.i.i = phi i64 [ %74, %bb9.i.i.i.i ], [ 0, %bb2.i.i.i.i ]
  %exitcond.not.i.i.i.i = icmp eq i64 %iter.sroa.9.0.i.i.i.i, %min_suffix.0.max_suffix.0.i
  br i1 %exitcond.not.i.i.i.i, label %bb18, label %bb9.i.i.i.i

bb9.i.i.i.i:                                      ; preds = %bb7.i.i.i.i
  %72 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %iter.sroa.9.0.i.i.i.i
  %73 = getelementptr inbounds i8, i8* %69, i64 %iter.sroa.9.0.i.i.i.i
  %74 = add nuw nsw i64 %iter.sroa.9.0.i.i.i.i, 1
  %b1.i.i.i.i = load i8, i8* %73, align 1, !alias.scope !491, !noalias !492
  %b2.i.i.i.i = load i8, i8* %72, align 1, !alias.scope !493, !noalias !494
  %_23.not.i.i.i.i = icmp eq i8 %b1.i.i.i.i, %b2.i.i.i.i
  br i1 %_23.not.i.i.i.i, label %bb7.i.i.i.i, label %.thread.i.i

bb20.i.i.i.i:                                     ; preds = %bb14.i.i.i.i, %bb24.i.i.i.i
  %py.073.i.i.i.i = phi i8* [ %76, %bb24.i.i.i.i ], [ %28, %bb14.i.i.i.i ]
  %px.072.i.i.i.i = phi i8* [ %75, %bb24.i.i.i.i ], [ %69, %bb14.i.i.i.i ]
  %_50.i.i.i.i = bitcast i8* %px.072.i.i.i.i to i32*
  %_50.val.i.i.i.i = load i32, i32* %_50.i.i.i.i, align 1, !alias.scope !491, !noalias !492
  %_53.i.i.i.i = bitcast i8* %py.073.i.i.i.i to i32*
  %_53.val.i.i.i.i = load i32, i32* %_53.i.i.i.i, align 1, !alias.scope !493, !noalias !494
  %_55.not.i.i.i.i = icmp eq i32 %_50.val.i.i.i.i, %_53.val.i.i.i.i
  br i1 %_55.not.i.i.i.i, label %bb24.i.i.i.i, label %.thread.i.i

bb24.i.i.i.i:                                     ; preds = %bb20.i.i.i.i
  %75 = getelementptr inbounds i8, i8* %px.072.i.i.i.i, i64 4
  %76 = getelementptr inbounds i8, i8* %py.073.i.i.i.i, i64 4
  %_46.i.i.i.i = icmp ult i8* %75, %70
  br i1 %_46.i.i.i.i, label %bb20.i.i.i.i, label %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i

_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i: ; preds = %bb24.i.i.i.i, %bb14.i.i.i.i
  %_63.i.i.i.i = bitcast i8* %70 to i32*
  %_63.val.i.i.i.i = load i32, i32* %_63.i.i.i.i, align 1, !alias.scope !491, !noalias !492
  %_66.i.i.i.i = bitcast i8* %71 to i32*
  %_66.val.i.i.i.i = load i32, i32* %_66.i.i.i.i, align 1, !alias.scope !493, !noalias !494
  %77 = icmp eq i32 %_63.val.i.i.i.i, %_66.val.i.i.i.i
  br i1 %77, label %bb18, label %.thread.i.i

.thread.i.i:                                      ; preds = %bb20.i.i.i.i, %bb9.i.i.i.i, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE.exit.i.i"
  br label %bb18

bb6:                                              ; preds = %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit
  %78 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %_16 = load i8, i8* %78, align 1
  br label %bb18

bb18:                                             ; preds = %bb7.i.i.i.i, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread, %.thread.i.i, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i, %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i, %bb7, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit, %bb6
  %.sroa.0.0.i2.i74 = phi i32 [ %nh.sroa.0.0.i.i, %bb7 ], [ %nh.sroa.0.0.i.i, %bb6 ], [ %nh.sroa.0.0.i.i, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit ], [ %nh.sroa.0.0.i.i, %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i ], [ %nh.sroa.0.0.i.i, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i ], [ %nh.sroa.0.0.i.i, %.thread.i.i ], [ 0, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread ], [ %nh.sroa.0.0.i.i, %bb7.i.i.i.i ]
  %.sroa.3.0.i1.i73 = phi i32 [ %nh.sroa.10.0.i.i, %bb7 ], [ %nh.sroa.10.0.i.i, %bb6 ], [ %nh.sroa.10.0.i.i, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit ], [ %nh.sroa.10.0.i.i, %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i ], [ %nh.sroa.10.0.i.i, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i ], [ %nh.sroa.10.0.i.i, %.thread.i.i ], [ 1, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread ], [ %nh.sroa.10.0.i.i, %bb7.i.i.i.i ]
  %kind.sroa.11.sroa.3.0 = phi i64 [ undef, %bb7 ], [ undef, %bb6 ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit ], [ %.lcssa, %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i ], [ %.lcssa, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i ], [ %.lcssa, %.thread.i.i ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread ], [ %.lcssa, %bb7.i.i.i.i ]
  %kind.sroa.11.sroa.4.0 = phi i64 [ undef, %bb7 ], [ undef, %bb6 ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit ], [ %min_suffix.0.max_suffix.0.i, %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i ], [ %min_suffix.0.max_suffix.0.i, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i ], [ %min_suffix.0.max_suffix.0.i, %.thread.i.i ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread ], [ %min_suffix.0.max_suffix.0.i, %bb7.i.i.i.i ]
  %kind.sroa.11.sroa.5.0 = phi i64 [ undef, %bb7 ], [ undef, %bb6 ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit ], [ 1, %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i ], [ 0, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i ], [ 1, %.thread.i.i ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread ], [ 0, %bb7.i.i.i.i ]
  %kind.sroa.11.sroa.6.0 = phi i64 [ undef, %bb7 ], [ undef, %bb6 ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit ], [ %.0.sroa.speculated.i.i.i.i.i, %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i ], [ %_15.sroa.0.0.i, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i ], [ %.0.sroa.speculated.i.i.i.i.i, %.thread.i.i ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread ], [ %_15.sroa.0.0.i, %bb7.i.i.i.i ]
  %kind.sroa.10.2 = phi i8 [ %.sroa.5.0.extract.trunc, %bb7 ], [ undef, %bb6 ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit ], [ %.sroa.5.0.extract.trunc, %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i ], [ %.sroa.5.0.extract.trunc, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i ], [ %.sroa.5.0.extract.trunc, %.thread.i.i ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread ], [ %.sroa.5.0.extract.trunc, %bb7.i.i.i.i ]
  %kind.sroa.8.2 = phi i8 [ %.sroa.4.0.extract.trunc, %bb7 ], [ %_16, %bb6 ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit ], [ %.sroa.4.0.extract.trunc, %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i ], [ %.sroa.4.0.extract.trunc, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i ], [ %.sroa.4.0.extract.trunc, %.thread.i.i ], [ undef, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread ], [ %.sroa.4.0.extract.trunc, %bb7.i.i.i.i ]
  %kind.sroa.0.2 = phi i8 [ 3, %bb7 ], [ 1, %bb6 ], [ 0, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit ], [ 2, %_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E.exit16.i ], [ 2, %_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E.exit.i.i ], [ 2, %.thread.i.i ], [ 0, %_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E.exit.thread ], [ 2, %bb7.i.i.i.i ]
  %kind.sroa.11.sroa.0.0.sroa_idx75 = getelementptr inbounds [5 x i8], [5 x i8]* %kind.sroa.11.sroa.0, i64 0, i64 0
  %79 = xor i1 %1, true
  %_7.i = icmp ult i64 %needle.1, 2
  %_4.0.i = select i1 %79, i1 true, i1 %_7.i
  %spec.select.i = select i1 %_4.0.i, i64* null, i64* bitcast ({ i64, i64 } ({ i32, i32 }*, %"memmem::NeedleInfo"*, [0 x i8]*, i64, [0 x i8]*, i64)* @_ZN6memchr6memmem9prefilter3x863sse4find17ha2a5ffc317089b73E to i64*)
  %.sroa.2.0.insert.ext.i = zext i32 %.sroa.3.0.i1.i73 to i64
  %.sroa.2.0.insert.shift.i = shl nuw i64 %.sroa.2.0.insert.ext.i, 32
  %.sroa.0.0.insert.ext.i = zext i32 %.sroa.0.0.i2.i74 to i64
  %.sroa.0.0.insert.insert.i = or i64 %.sroa.2.0.insert.shift.i, %.sroa.0.0.insert.ext.i
  %80 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %_39.sroa.6.sroa.0.0._39.sroa.6.0..sroa_idx.sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 1, i64 2
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 1 dereferenceable(5) %_39.sroa.6.sroa.0.0._39.sroa.6.0..sroa_idx.sroa_idx, i8* noundef nonnull align 1 dereferenceable(5) %kind.sroa.11.sroa.0.0.sroa_idx75, i64 5, i1 false)
  %81 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 0, i32 0
  store i8* %80, i8** %81, align 8
  %82 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 0, i32 1
  store i64 %needle.1, i64* %82, align 8
  %_37.sroa.0.0..sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 3
  %_37.sroa.0.0..sroa_cast = bitcast %"memmem::NeedleInfo"* %_37.sroa.0.0..sroa_idx to i64*
  store i64 %.sroa.0.0.insert.insert.i, i64* %_37.sroa.0.0..sroa_cast, align 8
  %_37.sroa.4.0..sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 3, i32 1, i32 0
  store i8 %.sroa.0.0.i.i, i8* %_37.sroa.4.0..sroa_idx, align 8
  %_37.sroa.5.0..sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 3, i32 1, i32 1
  store i8 %.sroa.3.0.i.i, i8* %_37.sroa.5.0..sroa_idx, align 1
  %_37.sroa.6.0..sroa_idx47 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 3, i32 2
  %_37.sroa.6.0..sroa_cast = bitcast [2 x i8]* %_37.sroa.6.0..sroa_idx47 to i16*
  store i16 0, i16* %_37.sroa.6.0..sroa_cast, align 2
  %83 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 1
  store i64* %spec.select.i, i64** %83, align 8
  %_39.sroa.0.0..sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 0
  store i8 %kind.sroa.0.2, i8* %_39.sroa.0.0..sroa_idx, align 8
  %_39.sroa.4.0..sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 1, i64 0
  store i8 %kind.sroa.8.2, i8* %_39.sroa.4.0..sroa_idx, align 1
  %_39.sroa.5.0..sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 1, i64 1
  store i8 %kind.sroa.10.2, i8* %_39.sroa.5.0..sroa_idx, align 2
  %_39.sroa.6.sroa.4.0._39.sroa.6.0..sroa_idx.sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 1, i64 7
  %_39.sroa.6.sroa.4.0._39.sroa.6.0..sroa_idx.sroa_cast = bitcast i8* %_39.sroa.6.sroa.4.0._39.sroa.6.0..sroa_idx.sroa_idx to i64*
  store i64 %kind.sroa.11.sroa.3.0, i64* %_39.sroa.6.sroa.4.0._39.sroa.6.0..sroa_idx.sroa_cast, align 8
  %_39.sroa.6.sroa.5.0._39.sroa.6.0..sroa_idx.sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 1, i64 15
  %_39.sroa.6.sroa.5.0._39.sroa.6.0..sroa_idx.sroa_cast = bitcast i8* %_39.sroa.6.sroa.5.0._39.sroa.6.0..sroa_idx.sroa_idx to i64*
  store i64 %kind.sroa.11.sroa.4.0, i64* %_39.sroa.6.sroa.5.0._39.sroa.6.0..sroa_idx.sroa_cast, align 8
  %_39.sroa.6.sroa.6.0._39.sroa.6.0..sroa_idx.sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 1, i64 23
  %_39.sroa.6.sroa.6.0._39.sroa.6.0..sroa_idx.sroa_cast = bitcast i8* %_39.sroa.6.sroa.6.0._39.sroa.6.0..sroa_idx.sroa_idx to i64*
  store i64 %kind.sroa.11.sroa.5.0, i64* %_39.sroa.6.sroa.6.0._39.sroa.6.0..sroa_idx.sroa_cast, align 8
  %_39.sroa.6.sroa.7.0._39.sroa.6.0..sroa_idx.sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 1, i64 31
  %_39.sroa.6.sroa.7.0._39.sroa.6.0..sroa_idx.sroa_cast = bitcast i8* %_39.sroa.6.sroa.7.0._39.sroa.6.0..sroa_idx.sroa_idx to i64*
  store i64 %kind.sroa.11.sroa.6.0, i64* %_39.sroa.6.sroa.7.0._39.sroa.6.0..sroa_idx.sroa_cast, align 8
  call void @llvm.lifetime.end.p0i8(i64 5, i8* nonnull %kind.sroa.11.sroa.0.0.sroa_idx75)
  ret void
}

; memchr::memmem::Searcher::prefilter_state
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define { i32, i32 } @_ZN6memchr6memmem8Searcher15prefilter_state17h550081e1923b1222E(%"memmem::Searcher"* noalias nocapture readonly align 8 dereferenceable(80) %self) unnamed_addr #10 {
start:
  %_3 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 1
  %0 = bitcast i64** %_3 to {}**
  %1 = load {}*, {}** %0, align 8, !alias.scope !495
  %.not.i.not.i = icmp ne {}* %1, null
  %spec.select = zext i1 %.not.i.not.i to i32
  %2 = insertvalue { i32, i32 } undef, i32 %spec.select, 0
  %3 = insertvalue { i32, i32 } %2, i32 0, 1
  ret { i32, i32 } %3
}

; memchr::memmem::Searcher::needle
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define { [0 x i8]*, i64 } @_ZN6memchr6memmem8Searcher6needle17hf8fce88406e6e39aE(%"memmem::Searcher"* noalias nocapture readonly align 8 dereferenceable(80) %self) unnamed_addr #10 {
start:
  %0 = bitcast %"memmem::Searcher"* %self to [0 x i8]**
  %1 = load [0 x i8]*, [0 x i8]** %0, align 8, !alias.scope !500, !nonnull !142
  %2 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 0, i32 1
  %3 = load i64, i64* %2, align 8, !alias.scope !500
  %4 = insertvalue { [0 x i8]*, i64 } undef, [0 x i8]* %1, 0
  %5 = insertvalue { [0 x i8]*, i64 } %4, i64 %3, 1
  ret { [0 x i8]*, i64 } %5
}

; memchr::memmem::Searcher::as_ref
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @_ZN6memchr6memmem8Searcher6as_ref17h97cd116bc2630aabE(%"memmem::Searcher"* noalias nocapture sret(%"memmem::Searcher") dereferenceable(80) %0, %"memmem::Searcher"* noalias nocapture readonly align 8 dereferenceable(80) %self) unnamed_addr #10 {
start:
  %kind.sroa.11 = alloca [37 x i8], align 1
  %kind.sroa.11.0.sroa_idx23 = getelementptr inbounds [37 x i8], [37 x i8]* %kind.sroa.11, i64 0, i64 0
  call void @llvm.lifetime.start.p0i8(i64 37, i8* nonnull %kind.sroa.11.0.sroa_idx23)
  %1 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 2, i32 0
  %2 = load i8, i8* %1, align 8, !range !157
  %_3 = zext i8 %2 to i64
  switch i64 %_3, label %bb2 [
    i64 0, label %bb7
    i64 1, label %bb4
    i64 2, label %bb5
    i64 3, label %bb6
    i64 4, label %bb1
  ]

bb2:                                              ; preds = %start
  unreachable

bb4:                                              ; preds = %start
  %3 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 2, i32 1, i64 0
  %b = load i8, i8* %3, align 1
  br label %bb7

bb5:                                              ; preds = %start
  %tw.sroa.0.0..sroa_idx5 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 2, i32 1, i64 7
  %_7.sroa.0.0.kind.sroa.11.8.sroa_idx.sroa_idx = getelementptr inbounds [37 x i8], [37 x i8]* %kind.sroa.11, i64 0, i64 5
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 1 dereferenceable(32) %_7.sroa.0.0.kind.sroa.11.8.sroa_idx.sroa_idx, i8* noundef nonnull align 8 dereferenceable(32) %tw.sroa.0.0..sroa_idx5, i64 32, i1 false)
  br label %bb7

bb6:                                              ; preds = %start
  %4 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 2, i32 1, i64 0
  %gs.0 = load i8, i8* %4, align 1
  %5 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 2, i32 1, i64 1
  %gs.1 = load i8, i8* %5, align 2
  br label %bb7

bb1:                                              ; preds = %start
  br label %bb7

bb7:                                              ; preds = %start, %bb4, %bb5, %bb6, %bb1
  %kind.sroa.10.0 = phi i8 [ undef, %bb1 ], [ %gs.1, %bb6 ], [ undef, %bb5 ], [ undef, %bb4 ], [ undef, %start ]
  %kind.sroa.8.0 = phi i8 [ undef, %bb1 ], [ %gs.0, %bb6 ], [ undef, %bb5 ], [ %b, %bb4 ], [ undef, %start ]
  %kind.sroa.0.0 = phi i8 [ 4, %bb1 ], [ 3, %bb6 ], [ 2, %bb5 ], [ 1, %bb4 ], [ 0, %start ]
  %6 = bitcast %"memmem::Searcher"* %self to [0 x i8]**
  %7 = load [0 x i8]*, [0 x i8]** %6, align 8, !alias.scope !505, !nonnull !142
  %8 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 0, i32 1
  %9 = load i64, i64* %8, align 8, !alias.scope !505
  %10 = getelementptr [0 x i8], [0 x i8]* %7, i64 0, i64 0
  %11 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 3
  %12 = bitcast %"memmem::NeedleInfo"* %11 to i8*
  %13 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 3
  %14 = bitcast %"memmem::NeedleInfo"* %13 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(12) %14, i8* noundef nonnull align 8 dereferenceable(12) %12, i64 12, i1 false)
  %15 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 1
  %_16 = load i64*, i64** %15, align 8
  %_17.sroa.6.0..sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 1, i64 2
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 1 dereferenceable(37) %_17.sroa.6.0..sroa_idx, i8* noundef nonnull align 1 dereferenceable(37) %kind.sroa.11.0.sroa_idx23, i64 37, i1 false)
  %16 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 0, i32 0
  store i8* %10, i8** %16, align 8
  %17 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 0, i32 1
  store i64 %9, i64* %17, align 8
  %18 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 1
  store i64* %_16, i64** %18, align 8
  %_17.sroa.0.0..sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 0
  store i8 %kind.sroa.0.0, i8* %_17.sroa.0.0..sroa_idx, align 8
  %_17.sroa.4.0..sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 1, i64 0
  store i8 %kind.sroa.8.0, i8* %_17.sroa.4.0..sroa_idx, align 1
  %_17.sroa.5.0..sroa_idx = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %0, i64 0, i32 2, i32 1, i64 1
  store i8 %kind.sroa.10.0, i8* %_17.sroa.5.0..sroa_idx, align 2
  call void @llvm.lifetime.end.p0i8(i64 37, i8* nonnull %kind.sroa.11.0.sroa_idx23)
  ret void
}

; memchr::memmem::Searcher::find_tw
; Function Attrs: noinline nonlazybind uwtable
define internal fastcc { i64, i64 } @_ZN6memchr6memmem8Searcher7find_tw17h619e5351c4063bc3E(%"memmem::Searcher"* noalias readonly align 8 dereferenceable(80) %self, %"memmem::twoway::Forward"* noalias nocapture readonly align 8 dereferenceable(32) %tw, { i32, i32 }* noalias align 4 dereferenceable(8) %state, [0 x i8]* noalias nonnull readonly align 1 %haystack.0, i64 %haystack.1, [0 x i8]* noalias nonnull readonly align 1 %needle.0, i64 %needle.1) unnamed_addr #11 personality i32 (...)* @rust_eh_personality {
start:
  %0 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 1
  %1 = bitcast i64** %0 to {}**
  %2 = load {}*, {}** %1, align 8
  %.not = icmp eq {}* %2, null
  br i1 %.not, label %bb6, label %bb1

bb1:                                              ; preds = %start
  %self.idx.i = getelementptr { i32, i32 }, { i32, i32 }* %state, i64 0, i32 0
  %self.idx.val.i = load i32, i32* %self.idx.i, align 4, !alias.scope !512
  %3 = icmp eq i32 %self.idx.val.i, 0
  br i1 %3, label %bb6, label %bb3.i99

bb3.i99:                                          ; preds = %bb1
  %4 = add i32 %self.idx.val.i, -1
  %_4.i = icmp ult i32 %4, 50
  br i1 %_4.i, label %bb3, label %bb6.i

bb6.i:                                            ; preds = %bb3.i99
  %5 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %state, i64 0, i32 1
  %_8.i = load i32, i32* %5, align 4, !alias.scope !512
  %_9.i = shl i32 %4, 3
  %_7.not.i = icmp ult i32 %_8.i, %_9.i
  br i1 %_7.not.i, label %bb9.i, label %bb3

bb9.i:                                            ; preds = %bb6.i
  store i32 0, i32* %self.idx.i, align 4, !alias.scope !512
  br label %bb6

bb6:                                              ; preds = %bb9.i, %bb1, %start
  tail call void @llvm.experimental.noalias.scope.decl(metadata !515)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !518)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !520)
  %6 = getelementptr inbounds %"memmem::twoway::Forward", %"memmem::twoway::Forward"* %tw, i64 0, i32 0, i32 2, i32 0
  %_5.i = load i64, i64* %6, align 8, !range !252, !alias.scope !515, !noalias !522
  %switch.not.i = icmp eq i64 %_5.i, 1
  %7 = getelementptr inbounds %"memmem::twoway::Forward", %"memmem::twoway::Forward"* %tw, i64 0, i32 0, i32 2, i32 1
  %shift.i = load i64, i64* %7, align 8, !alias.scope !515, !noalias !522
  %self.idx8.i = getelementptr %"memmem::twoway::Forward", %"memmem::twoway::Forward"* %tw, i64 0, i32 0, i32 0
  %self.idx8.val.i = load i64, i64* %self.idx8.i, align 8, !alias.scope !515, !noalias !522
  %self.idx9.i = getelementptr %"memmem::twoway::Forward", %"memmem::twoway::Forward"* %tw, i64 0, i32 0, i32 1
  %self.idx9.val.i = load i64, i64* %self.idx9.i, align 8, !alias.scope !515, !noalias !522
  br i1 %switch.not.i, label %bb1.i, label %bb3.i

bb3.i:                                            ; preds = %bb6
  tail call void @llvm.experimental.noalias.scope.decl(metadata !524)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !527)
  %_82.i.i = sub i64 1, %self.idx9.val.i
  %8 = sub i64 %needle.1, %shift.i
  %_11.not31.i.i = icmp ugt i64 %needle.1, %haystack.1
  br i1 %_11.not31.i.i, label %bb8, label %bb2.i.i

bb2.i.i:                                          ; preds = %bb3.i, %bb1.backedge.i.i
  %pos.033.i.i = phi i64 [ %pos.0.be.i.i, %bb1.backedge.i.i ], [ 0, %bb3.i ]
  %shift.032.i.i = phi i64 [ %shift.0.be.i.i, %bb1.backedge.i.i ], [ 0, %bb3.i ]
  %9 = icmp ult i64 %shift.032.i.i, %self.idx9.val.i
  %.0.sroa.speculated.i.i.i.i.i = select i1 %9, i64 %self.idx9.val.i, i64 %shift.032.i.i
  %last_byte.i.i = add i64 %pos.033.i.i, %needle.1
  %_54.i.i = add i64 %last_byte.i.i, -1
  %_58.i.i = icmp ult i64 %_54.i.i, %haystack.1
  br i1 %_58.i.i, label %bb19.i.i, label %panic.i.i, !prof !60

bb19.i.i:                                         ; preds = %bb2.i.i
  %10 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_54.i.i
  %_53.i.i = load i8, i8* %10, align 1, !alias.scope !529, !noalias !530
  %11 = and i8 %_53.i.i, 63
  %12 = zext i8 %11 to i64
  %_5.i.i.i = shl nuw i64 1, %12
  %_3.i.i.i = and i64 %_5.i.i.i, %self.idx8.val.i
  %.not.i.i = icmp eq i64 %_3.i.i.i, 0
  br i1 %.not.i.i, label %bb1.backedge.i.i, label %bb23.preheader.i.i

bb23.preheader.i.i:                               ; preds = %bb19.i.i
  %_6227.i.i = icmp ult i64 %.0.sroa.speculated.i.i.i.i.i, %needle.1
  br i1 %_6227.i.i, label %bb27.i.i, label %bb33.preheader.i.i

panic.i.i:                                        ; preds = %bb2.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_54.i.i, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1099 to %"core::panic::location::Location"*)) #22, !noalias !532
  unreachable

bb1.backedge.i.i:                                 ; preds = %bb48.i.i, %bb19.i.i
  %shift.0.be.i.i = phi i64 [ %shift.2.i.i, %bb48.i.i ], [ 0, %bb19.i.i ]
  %pos.0.be.i.i = phi i64 [ %pos.2.i.i, %bb48.i.i ], [ %last_byte.i.i, %bb19.i.i ]
  %_12.i.i = add i64 %pos.0.be.i.i, %needle.1
  %_11.not.i.i = icmp ugt i64 %_12.i.i, %haystack.1
  br i1 %_11.not.i.i, label %bb8, label %bb2.i.i

bb33.preheader.i.i:                               ; preds = %bb29.i.i, %bb23.preheader.i.i
  br i1 %9, label %bb35.i.i, label %bb42.i.i

bb27.i.i:                                         ; preds = %bb23.preheader.i.i, %bb29.i.i
  %i.128.i.i = phi i64 [ %16, %bb29.i.i ], [ %.0.sroa.speculated.i.i.i.i.i, %bb23.preheader.i.i ]
  %_72.i.i = add i64 %i.128.i.i, %pos.033.i.i
  %_76.i.i = icmp ult i64 %_72.i.i, %haystack.1
  br i1 %_76.i.i, label %bb28.i.i, label %panic3.i.i, !prof !60

bb28.i.i:                                         ; preds = %bb27.i.i
  %13 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %i.128.i.i
  %_67.i.i = load i8, i8* %13, align 1, !alias.scope !533, !noalias !534
  %14 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_72.i.i
  %_71.i.i = load i8, i8* %14, align 1, !alias.scope !529, !noalias !530
  %_66.i.i = icmp eq i8 %_67.i.i, %_71.i.i
  br i1 %_66.i.i, label %bb29.i.i, label %bb31.i.i

panic3.i.i:                                       ; preds = %bb27.i.i
  %15 = add i64 %.0.sroa.speculated.i.i.i.i.i, %pos.033.i.i
  %umax.i.i = tail call i64 @llvm.umax.i64(i64 %haystack.1, i64 %15)
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %umax.i.i, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1103 to %"core::panic::location::Location"*)) #22, !noalias !532
  unreachable

bb29.i.i:                                         ; preds = %bb28.i.i
  %16 = add i64 %i.128.i.i, 1
  %exitcond.not.i.i = icmp eq i64 %16, %needle.1
  br i1 %exitcond.not.i.i, label %bb33.preheader.i.i, label %bb27.i.i

bb31.i.i:                                         ; preds = %bb28.i.i
  %_81.i.i = add i64 %_82.i.i, %i.128.i.i
  br label %bb48.i.i

bb48.i.i:                                         ; preds = %bb38.i.i, %bb45.i.i, %bb31.i.i
  %shift.2.i.i = phi i64 [ 0, %bb31.i.i ], [ %8, %bb45.i.i ], [ %8, %bb38.i.i ]
  %_81.pn.i.i = phi i64 [ %_81.i.i, %bb31.i.i ], [ %shift.i, %bb45.i.i ], [ %shift.i, %bb38.i.i ]
  %pos.2.i.i = add i64 %_81.pn.i.i, %pos.033.i.i
  br label %bb1.backedge.i.i

bb35.i.i:                                         ; preds = %bb33.preheader.i.i, %bb39.i.i
  %j.030.i.i = phi i64 [ %19, %bb39.i.i ], [ %self.idx9.val.i, %bb33.preheader.i.i ]
  %_94.i.i = icmp ult i64 %j.030.i.i, %needle.1
  br i1 %_94.i.i, label %bb37.i.i, label %panic4.i.i, !prof !60

bb37.i.i:                                         ; preds = %bb35.i.i
  %_96.i.i = add i64 %j.030.i.i, %pos.033.i.i
  %_100.i.i = icmp ult i64 %_96.i.i, %haystack.1
  br i1 %_100.i.i, label %bb38.i.i, label %panic5.i.i, !prof !60

panic4.i.i:                                       ; preds = %bb35.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %j.030.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1105 to %"core::panic::location::Location"*)) #22, !noalias !532
  unreachable

bb38.i.i:                                         ; preds = %bb37.i.i
  %17 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %j.030.i.i
  %_91.i.i = load i8, i8* %17, align 1, !alias.scope !533, !noalias !534
  %18 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_96.i.i
  %_95.i.i = load i8, i8* %18, align 1, !alias.scope !529, !noalias !530
  %_90.i.i = icmp eq i8 %_91.i.i, %_95.i.i
  br i1 %_90.i.i, label %bb39.i.i, label %bb48.i.i

panic5.i.i:                                       ; preds = %bb37.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_96.i.i, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1107 to %"core::panic::location::Location"*)) #22, !noalias !532
  unreachable

bb39.i.i:                                         ; preds = %bb38.i.i
  %19 = add i64 %j.030.i.i, -1
  %_87.i.i = icmp ugt i64 %19, %shift.032.i.i
  br i1 %_87.i.i, label %bb35.i.i, label %bb42.i.i

bb42.i.i:                                         ; preds = %bb39.i.i, %bb33.preheader.i.i
  %_109.i.i = icmp ult i64 %shift.032.i.i, %needle.1
  br i1 %_109.i.i, label %bb44.i.i, label %panic6.i.i, !prof !60

bb44.i.i:                                         ; preds = %bb42.i.i
  %_111.i.i = add i64 %shift.032.i.i, %pos.033.i.i
  %_115.i.i = icmp ult i64 %_111.i.i, %haystack.1
  br i1 %_115.i.i, label %bb45.i.i, label %panic7.i.i, !prof !60

panic6.i.i:                                       ; preds = %bb42.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %shift.032.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1109 to %"core::panic::location::Location"*)) #22, !noalias !532
  unreachable

bb45.i.i:                                         ; preds = %bb44.i.i
  %20 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %shift.032.i.i
  %_106.i.i = load i8, i8* %20, align 1, !alias.scope !533, !noalias !534
  %21 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_111.i.i
  %_110.i.i = load i8, i8* %21, align 1, !alias.scope !529, !noalias !530
  %_105.i.i = icmp eq i8 %_106.i.i, %_110.i.i
  br i1 %_105.i.i, label %bb8, label %bb48.i.i

panic7.i.i:                                       ; preds = %bb44.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_111.i.i, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1111 to %"core::panic::location::Location"*)) #22, !noalias !532
  unreachable

bb1.i:                                            ; preds = %bb6
  tail call void @llvm.experimental.noalias.scope.decl(metadata !535)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !538)
  %last_byte.i10.i = add i64 %needle.1, -1
  %_5833.i.i = icmp ult i64 %self.idx9.val.i, %needle.1
  %_78.i.i = sub i64 1, %self.idx9.val.i
  %_10.not35.i.i = icmp ugt i64 %needle.1, %haystack.1
  br i1 %_10.not35.i.i, label %bb8, label %bb2.lr.ph.i.i

bb2.lr.ph.i.i:                                    ; preds = %bb1.i
  %22 = add i64 %self.idx9.val.i, -1
  %_95.us198.i.i = icmp ult i64 %22, %needle.1
  br label %bb2.us.i.i

bb2.us.i.i:                                       ; preds = %bb1.backedge.us.i.i, %bb2.lr.ph.i.i
  %pos.036.us.i.i = phi i64 [ %pos.0.be.us.i.i, %bb1.backedge.us.i.i ], [ 0, %bb2.lr.ph.i.i ]
  %_49.us.i.i = add i64 %last_byte.i10.i, %pos.036.us.i.i
  %_53.us.i.i = icmp ult i64 %_49.us.i.i, %haystack.1
  br i1 %_53.us.i.i, label %bb18.us.i.i, label %panic.i32.i, !prof !60

bb18.us.i.i:                                      ; preds = %bb2.us.i.i
  %23 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_49.us.i.i
  %_48.us.i.i = load i8, i8* %23, align 1, !alias.scope !540, !noalias !541
  %24 = and i8 %_48.us.i.i, 63
  %25 = zext i8 %24 to i64
  %_5.i.us.i.i = shl nuw i64 1, %25
  %_3.i.us.i.i = and i64 %_5.i.us.i.i, %self.idx8.val.i
  %.not.us.i.i = icmp eq i64 %_3.i.us.i.i, 0
  br i1 %.not.us.i.i, label %bb44.us.i.i, label %bb22.preheader.us.i.i

bb26.us.i.i:                                      ; preds = %bb22.preheader.us.i.i, %bb28.us.i.i
  %i.034.us.i.i = phi i64 [ %29, %bb28.us.i.i ], [ %self.idx9.val.i, %bb22.preheader.us.i.i ]
  %_68.us.i.i = add i64 %i.034.us.i.i, %pos.036.us.i.i
  %_72.us.i.i = icmp ult i64 %_68.us.i.i, %haystack.1
  br i1 %_72.us.i.i, label %bb27.us.i.i, label %panic3.i38.i, !prof !60

bb27.us.i.i:                                      ; preds = %bb26.us.i.i
  %26 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %i.034.us.i.i
  %_63.us.i.i = load i8, i8* %26, align 1, !alias.scope !543, !noalias !544
  %27 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_68.us.i.i
  %_67.us.i.i = load i8, i8* %27, align 1, !alias.scope !540, !noalias !541
  %_62.us.i.i = icmp eq i8 %_63.us.i.i, %_67.us.i.i
  br i1 %_62.us.i.i, label %bb28.us.i.i, label %bb30.us.i.i

bb30.us.i.i:                                      ; preds = %bb27.us.i.i
  %28 = add i64 %_78.i.i, %_68.us.i.i
  br label %bb1.backedge.us.i.i

bb28.us.i.i:                                      ; preds = %bb27.us.i.i
  %29 = add i64 %i.034.us.i.i, 1
  %exitcond197.not.i.i = icmp eq i64 %29, %needle.1
  br i1 %exitcond197.not.i.i, label %bb34.preheader.us.i.i, label %bb26.us.i.i

bb44.us.i.i:                                      ; preds = %bb40.us.i.us.i, %bb18.us.i.i
  %needle.1.pn.us.i.i = phi i64 [ %needle.1, %bb18.us.i.i ], [ %shift.i, %bb40.us.i.us.i ]
  %pos.2.us.i.i = add i64 %needle.1.pn.us.i.i, %pos.036.us.i.i
  br label %bb1.backedge.us.i.i

bb34.preheader.us.i.i:                            ; preds = %bb28.us.i.i, %bb22.preheader.us.i.i
  br i1 %_95.us198.i.i, label %bb34.us.i.us.i, label %bb34.preheader.us.i.split.i, !prof !60

bb34.us.i.us.i:                                   ; preds = %bb34.preheader.us.i.i, %bb40.us.i.us.i
  %iter.sroa.5.0.us.i.us.i = phi i64 [ %30, %bb40.us.i.us.i ], [ %self.idx9.val.i, %bb34.preheader.us.i.i ]
  %.not11.us.i.us.i = icmp eq i64 %iter.sroa.5.0.us.i.us.i, 0
  br i1 %.not11.us.i.us.i, label %bb8, label %bb36.us.i.us.i

bb36.us.i.us.i:                                   ; preds = %bb34.us.i.us.i
  %30 = add i64 %iter.sroa.5.0.us.i.us.i, -1
  %_97.us.i.us.i = add i64 %30, %pos.036.us.i.i
  %_101.us.i.us.i = icmp ult i64 %_97.us.i.us.i, %haystack.1
  br i1 %_101.us.i.us.i, label %bb40.us.i.us.i, label %panic5.i46.i, !prof !60

bb40.us.i.us.i:                                   ; preds = %bb36.us.i.us.i
  %31 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %30
  %_92.us.i.us.i = load i8, i8* %31, align 1, !alias.scope !543, !noalias !544
  %32 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_97.us.i.us.i
  %_96.us.i.us.i = load i8, i8* %32, align 1, !alias.scope !540, !noalias !541
  %_91.not.us.i.us.i = icmp eq i8 %_92.us.i.us.i, %_96.us.i.us.i
  br i1 %_91.not.us.i.us.i, label %bb34.us.i.us.i, label %bb44.us.i.i

bb34.preheader.us.i.split.i:                      ; preds = %bb34.preheader.us.i.i
  %.not11.us.i.i = icmp eq i64 %self.idx9.val.i, 0
  br i1 %.not11.us.i.i, label %bb8, label %panic4.i44.i

bb22.preheader.us.i.i:                            ; preds = %bb18.us.i.i
  br i1 %_5833.i.i, label %bb26.us.i.i, label %bb34.preheader.us.i.i

bb1.backedge.us.i.i:                              ; preds = %bb44.us.i.i, %bb30.us.i.i
  %pos.0.be.us.i.i = phi i64 [ %pos.2.us.i.i, %bb44.us.i.i ], [ %28, %bb30.us.i.i ]
  %_11.us.i.i = add i64 %pos.0.be.us.i.i, %needle.1
  %_10.not.us.i.i = icmp ugt i64 %_11.us.i.i, %haystack.1
  br i1 %_10.not.us.i.i, label %bb8, label %bb2.us.i.i

panic.i32.i:                                      ; preds = %bb2.us.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_49.us.i.i, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1115 to %"core::panic::location::Location"*)) #22, !noalias !545
  unreachable

panic3.i38.i:                                     ; preds = %bb26.us.i.i
  %33 = add i64 %pos.036.us.i.i, %self.idx9.val.i
  %umax.le.i.i = tail call i64 @llvm.umax.i64(i64 %haystack.1, i64 %33)
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %umax.le.i.i, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1119 to %"core::panic::location::Location"*)) #22, !noalias !545
  unreachable

panic4.i44.i:                                     ; preds = %bb34.preheader.us.i.split.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %22, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1121 to %"core::panic::location::Location"*)) #22, !noalias !545
  unreachable

panic5.i46.i:                                     ; preds = %bb36.us.i.us.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_97.us.i.us.i, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1123 to %"core::panic::location::Location"*)) #22, !noalias !545
  unreachable

bb3:                                              ; preds = %bb3.i99, %bb6.i
  %_14 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 3
  tail call void @llvm.experimental.noalias.scope.decl(metadata !546)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !549)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !551)
  %34 = getelementptr inbounds %"memmem::twoway::Forward", %"memmem::twoway::Forward"* %tw, i64 0, i32 0, i32 2, i32 0
  %_5.i8 = load i64, i64* %34, align 8, !range !252, !alias.scope !546, !noalias !553
  %switch.not.i9 = icmp eq i64 %_5.i8, 1
  %35 = getelementptr inbounds %"memmem::twoway::Forward", %"memmem::twoway::Forward"* %tw, i64 0, i32 0, i32 2, i32 1
  %shift.i10 = load i64, i64* %35, align 8, !alias.scope !546, !noalias !553
  %self.idx8.i11 = getelementptr %"memmem::twoway::Forward", %"memmem::twoway::Forward"* %tw, i64 0, i32 0, i32 0
  %self.idx8.val.i12 = load i64, i64* %self.idx8.i11, align 8, !alias.scope !546, !noalias !553
  %self.idx9.i13 = getelementptr %"memmem::twoway::Forward", %"memmem::twoway::Forward"* %tw, i64 0, i32 0, i32 1
  %self.idx9.val.i14 = load i64, i64* %self.idx9.i13, align 8, !alias.scope !546, !noalias !553
  br i1 %switch.not.i9, label %bb1.i87, label %bb3.i17

bb3.i17:                                          ; preds = %bb3
  tail call void @llvm.experimental.noalias.scope.decl(metadata !555)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !558)
  %_82.i.i15 = sub i64 1, %self.idx9.val.i14
  %36 = sub i64 %needle.1, %shift.i10
  %_11.not31.i.i16 = icmp ugt i64 %needle.1, %haystack.1
  br i1 %_11.not31.i.i16, label %bb8, label %bb2.i.i21.preheader

bb2.i.i21.preheader:                              ; preds = %bb3.i17
  %37 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %state, i64 0, i32 1
  %38 = bitcast {}* %2 to { i64, i64 } ({ i32, i32 }*, %"memmem::NeedleInfo"*, [0 x i8]*, i64, [0 x i8]*, i64)*
  br label %bb2.i.i21

bb2.i.i21:                                        ; preds = %bb1.backedge.i.i37.bb2.i.i21_crit_edge, %bb2.i.i21.preheader
  %self.idx.val.i.i.i.i = phi i32 [ %self.idx.val.i.i.i.i.pre, %bb1.backedge.i.i37.bb2.i.i21_crit_edge ], [ %self.idx.val.i, %bb2.i.i21.preheader ]
  %pos.033.i.i18 = phi i64 [ %pos.0.be.i.i34, %bb1.backedge.i.i37.bb2.i.i21_crit_edge ], [ 0, %bb2.i.i21.preheader ]
  %shift.032.i.i19 = phi i64 [ %shift.0.be.i.i33, %bb1.backedge.i.i37.bb2.i.i21_crit_edge ], [ 0, %bb2.i.i21.preheader ]
  %39 = icmp ult i64 %shift.032.i.i19, %self.idx9.val.i14
  %.0.sroa.speculated.i.i.i.i.i20 = select i1 %39, i64 %self.idx9.val.i14, i64 %shift.032.i.i19
  %40 = icmp eq i32 %self.idx.val.i.i.i.i, 0
  br i1 %40, label %bb18.i.i, label %bb3.i.i.i.i

bb3.i.i.i.i:                                      ; preds = %bb2.i.i21
  %41 = add i32 %self.idx.val.i.i.i.i, -1
  %_4.i.i.i.i = icmp ult i32 %41, 50
  br i1 %_4.i.i.i.i, label %bb7.i.i, label %bb6.i.i.i.i

bb6.i.i.i.i:                                      ; preds = %bb3.i.i.i.i
  %_8.i.i.i.i = load i32, i32* %37, align 4, !alias.scope !560, !noalias !563
  %_9.i.i.i.i = shl i32 %41, 3
  %_7.not.i.i.i.i = icmp ult i32 %_8.i.i.i.i, %_9.i.i.i.i
  br i1 %_7.not.i.i.i.i, label %bb9.i.i.i.i, label %bb7.i.i

bb9.i.i.i.i:                                      ; preds = %bb6.i.i.i.i
  store i32 0, i32* %self.idx.i, align 4, !alias.scope !560, !noalias !563
  br label %bb18.i.i

bb18.i.i:                                         ; preds = %bb11.i.i, %bb9.i.i.i.i, %bb2.i.i21
  %i.0.i.i = phi i64 [ %self.idx9.val.i14, %bb11.i.i ], [ %.0.sroa.speculated.i.i.i.i.i20, %bb2.i.i21 ], [ %.0.sroa.speculated.i.i.i.i.i20, %bb9.i.i.i.i ]
  %shift.1.i.i = phi i64 [ 0, %bb11.i.i ], [ %shift.032.i.i19, %bb2.i.i21 ], [ %shift.032.i.i19, %bb9.i.i.i.i ]
  %pos.1.i.i = phi i64 [ %45, %bb11.i.i ], [ %pos.033.i.i18, %bb2.i.i21 ], [ %pos.033.i.i18, %bb9.i.i.i.i ]
  %last_byte.i.i22 = add i64 %pos.1.i.i, %needle.1
  %_54.i.i23 = add i64 %last_byte.i.i22, -1
  %_58.i.i24 = icmp ult i64 %_54.i.i23, %haystack.1
  br i1 %_58.i.i24, label %bb19.i.i29, label %panic.i.i32, !prof !60

bb7.i.i:                                          ; preds = %bb6.i.i.i.i, %bb3.i.i.i.i
  %_3.i.i.i.i = icmp ugt i64 %pos.033.i.i18, %haystack.1
  br i1 %_3.i.i.i.i, label %bb1.i.i.i.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i"

bb1.i.i.i.i:                                      ; preds = %bb7.i.i
; call core::slice::index::slice_start_index_len_fail
  tail call void @_ZN4core5slice5index26slice_start_index_len_fail17h3f1b9df81972beedE(i64 %pos.033.i.i18, i64 %haystack.1, %"core::panic::location::Location"* noalias nonnull readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1097 to %"core::panic::location::Location"*)) #22, !noalias !565
  unreachable

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i": ; preds = %bb7.i.i
  %42 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %pos.033.i.i18
  %_7.i.i.i.i.i.i = sub i64 %haystack.1, %pos.033.i.i18
  %43 = bitcast i8* %42 to [0 x i8]*
  %44 = tail call { i64, i64 } %38({ i32, i32 }* noalias nonnull align 4 dereferenceable(8) %state, %"memmem::NeedleInfo"* noalias nonnull readonly align 4 dereferenceable(12) %_14, [0 x i8]* noalias nonnull readonly align 1 %43, i64 %_7.i.i.i.i.i.i, [0 x i8]* noalias nonnull readonly align 1 %needle.0, i64 %needle.1), !noalias !563
  %_29.0.i.i = extractvalue { i64, i64 } %44, 0
  %switch.i.not.i.i = icmp eq i64 %_29.0.i.i, 0
  br i1 %switch.i.not.i.i, label %bb8, label %bb11.i.i

bb11.i.i:                                         ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i"
  %_29.1.i.i = extractvalue { i64, i64 } %44, 1
  %45 = add i64 %_29.1.i.i, %pos.033.i.i18
  %_44.i.i = add i64 %45, %needle.1
  %_43.i.i = icmp ugt i64 %_44.i.i, %haystack.1
  br i1 %_43.i.i, label %bb8, label %bb18.i.i

bb19.i.i29:                                       ; preds = %bb18.i.i
  %46 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_54.i.i23
  %_53.i.i25 = load i8, i8* %46, align 1, !alias.scope !570, !noalias !571
  %47 = and i8 %_53.i.i25, 63
  %48 = zext i8 %47 to i64
  %_5.i.i.i26 = shl nuw i64 1, %48
  %_3.i.i.i27 = and i64 %_5.i.i.i26, %self.idx8.val.i12
  %.not.i.i28 = icmp eq i64 %_3.i.i.i27, 0
  br i1 %.not.i.i28, label %bb1.backedge.i.i37, label %bb23.preheader.i.i31

bb23.preheader.i.i31:                             ; preds = %bb19.i.i29
  %_6227.i.i30 = icmp ult i64 %i.0.i.i, %needle.1
  br i1 %_6227.i.i30, label %bb27.i.i43, label %bb33.preheader.i.i39

panic.i.i32:                                      ; preds = %bb18.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_54.i.i23, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1099 to %"core::panic::location::Location"*)) #22, !noalias !563
  unreachable

bb1.backedge.i.i37:                               ; preds = %bb48.i.i57, %bb19.i.i29
  %shift.0.be.i.i33 = phi i64 [ %shift.2.i.i54, %bb48.i.i57 ], [ 0, %bb19.i.i29 ]
  %pos.0.be.i.i34 = phi i64 [ %pos.2.i.i56, %bb48.i.i57 ], [ %last_byte.i.i22, %bb19.i.i29 ]
  %_12.i.i35 = add i64 %pos.0.be.i.i34, %needle.1
  %_11.not.i.i36 = icmp ugt i64 %_12.i.i35, %haystack.1
  br i1 %_11.not.i.i36, label %bb8, label %bb1.backedge.i.i37.bb2.i.i21_crit_edge

bb1.backedge.i.i37.bb2.i.i21_crit_edge:           ; preds = %bb1.backedge.i.i37
  %self.idx.val.i.i.i.i.pre = load i32, i32* %self.idx.i, align 4, !alias.scope !560, !noalias !563
  br label %bb2.i.i21

bb33.preheader.i.i39:                             ; preds = %bb29.i.i51, %bb23.preheader.i.i31
  %_8729.i.i38 = icmp ult i64 %shift.1.i.i, %self.idx9.val.i14
  br i1 %_8729.i.i38, label %bb35.i.i60, label %bb42.i.i73

bb27.i.i43:                                       ; preds = %bb23.preheader.i.i31, %bb29.i.i51
  %i.128.i.i40 = phi i64 [ %52, %bb29.i.i51 ], [ %i.0.i.i, %bb23.preheader.i.i31 ]
  %_72.i.i41 = add i64 %i.128.i.i40, %pos.1.i.i
  %_76.i.i42 = icmp ult i64 %_72.i.i41, %haystack.1
  br i1 %_76.i.i42, label %bb28.i.i47, label %panic3.i.i49, !prof !60

bb28.i.i47:                                       ; preds = %bb27.i.i43
  %49 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %i.128.i.i40
  %_67.i.i44 = load i8, i8* %49, align 1, !alias.scope !572, !noalias !573
  %50 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_72.i.i41
  %_71.i.i45 = load i8, i8* %50, align 1, !alias.scope !570, !noalias !571
  %_66.i.i46 = icmp eq i8 %_67.i.i44, %_71.i.i45
  br i1 %_66.i.i46, label %bb29.i.i51, label %bb31.i.i53

panic3.i.i49:                                     ; preds = %bb27.i.i43
  %51 = add i64 %pos.1.i.i, %i.0.i.i
  %umax.i.i48 = tail call i64 @llvm.umax.i64(i64 %haystack.1, i64 %51)
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %umax.i.i48, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1103 to %"core::panic::location::Location"*)) #22, !noalias !563
  unreachable

bb29.i.i51:                                       ; preds = %bb28.i.i47
  %52 = add i64 %i.128.i.i40, 1
  %exitcond.not.i.i50 = icmp eq i64 %52, %needle.1
  br i1 %exitcond.not.i.i50, label %bb33.preheader.i.i39, label %bb27.i.i43

bb31.i.i53:                                       ; preds = %bb28.i.i47
  %_81.i.i52 = add i64 %_82.i.i15, %i.128.i.i40
  br label %bb48.i.i57

bb48.i.i57:                                       ; preds = %bb38.i.i68, %bb45.i.i81, %bb31.i.i53
  %shift.2.i.i54 = phi i64 [ 0, %bb31.i.i53 ], [ %36, %bb45.i.i81 ], [ %36, %bb38.i.i68 ]
  %_81.pn.i.i55 = phi i64 [ %_81.i.i52, %bb31.i.i53 ], [ %shift.i10, %bb45.i.i81 ], [ %shift.i10, %bb38.i.i68 ]
  %pos.2.i.i56 = add i64 %_81.pn.i.i55, %pos.1.i.i
  br label %bb1.backedge.i.i37

bb35.i.i60:                                       ; preds = %bb33.preheader.i.i39, %bb39.i.i71
  %j.030.i.i58 = phi i64 [ %55, %bb39.i.i71 ], [ %self.idx9.val.i14, %bb33.preheader.i.i39 ]
  %_94.i.i59 = icmp ult i64 %j.030.i.i58, %needle.1
  br i1 %_94.i.i59, label %bb37.i.i63, label %panic4.i.i64, !prof !60

bb37.i.i63:                                       ; preds = %bb35.i.i60
  %_96.i.i61 = add i64 %j.030.i.i58, %pos.1.i.i
  %_100.i.i62 = icmp ult i64 %_96.i.i61, %haystack.1
  br i1 %_100.i.i62, label %bb38.i.i68, label %panic5.i.i69, !prof !60

panic4.i.i64:                                     ; preds = %bb35.i.i60
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %j.030.i.i58, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1105 to %"core::panic::location::Location"*)) #22, !noalias !563
  unreachable

bb38.i.i68:                                       ; preds = %bb37.i.i63
  %53 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %j.030.i.i58
  %_91.i.i65 = load i8, i8* %53, align 1, !alias.scope !572, !noalias !573
  %54 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_96.i.i61
  %_95.i.i66 = load i8, i8* %54, align 1, !alias.scope !570, !noalias !571
  %_90.i.i67 = icmp eq i8 %_91.i.i65, %_95.i.i66
  br i1 %_90.i.i67, label %bb39.i.i71, label %bb48.i.i57

panic5.i.i69:                                     ; preds = %bb37.i.i63
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_96.i.i61, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1107 to %"core::panic::location::Location"*)) #22, !noalias !563
  unreachable

bb39.i.i71:                                       ; preds = %bb38.i.i68
  %55 = add i64 %j.030.i.i58, -1
  %_87.i.i70 = icmp ugt i64 %55, %shift.1.i.i
  br i1 %_87.i.i70, label %bb35.i.i60, label %bb42.i.i73

bb42.i.i73:                                       ; preds = %bb39.i.i71, %bb33.preheader.i.i39
  %_109.i.i72 = icmp ult i64 %shift.1.i.i, %needle.1
  br i1 %_109.i.i72, label %bb44.i.i76, label %panic6.i.i77, !prof !60

bb44.i.i76:                                       ; preds = %bb42.i.i73
  %_111.i.i74 = add i64 %pos.1.i.i, %shift.1.i.i
  %_115.i.i75 = icmp ult i64 %_111.i.i74, %haystack.1
  br i1 %_115.i.i75, label %bb45.i.i81, label %panic7.i.i82, !prof !60

panic6.i.i77:                                     ; preds = %bb42.i.i73
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %shift.1.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1109 to %"core::panic::location::Location"*)) #22, !noalias !563
  unreachable

bb45.i.i81:                                       ; preds = %bb44.i.i76
  %56 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %shift.1.i.i
  %_106.i.i78 = load i8, i8* %56, align 1, !alias.scope !572, !noalias !573
  %57 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_111.i.i74
  %_110.i.i79 = load i8, i8* %57, align 1, !alias.scope !570, !noalias !571
  %_105.i.i80 = icmp eq i8 %_106.i.i78, %_110.i.i79
  br i1 %_105.i.i80, label %bb8, label %bb48.i.i57

panic7.i.i82:                                     ; preds = %bb44.i.i76
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_111.i.i74, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1111 to %"core::panic::location::Location"*)) #22, !noalias !563
  unreachable

bb1.i87:                                          ; preds = %bb3
  tail call void @llvm.experimental.noalias.scope.decl(metadata !574)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !577)
  %last_byte.i10.i83 = add i64 %needle.1, -1
  %_5833.i.i84 = icmp ult i64 %self.idx9.val.i14, %needle.1
  %_78.i.i85 = sub i64 1, %self.idx9.val.i14
  %_10.not35.i.i86 = icmp ugt i64 %needle.1, %haystack.1
  br i1 %_10.not35.i.i86, label %bb8, label %bb2.preheader.i.i

bb2.preheader.i.i:                                ; preds = %bb1.i87
  %58 = add i64 %self.idx9.val.i14, -1
  %59 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %state, i64 0, i32 1
  %60 = bitcast {}* %2 to { i64, i64 } ({ i32, i32 }*, %"memmem::NeedleInfo"*, [0 x i8]*, i64, [0 x i8]*, i64)*
  %_95191.i.i = icmp ult i64 %58, %needle.1
  br label %bb2.i13.i

bb2.i13.i:                                        ; preds = %bb1.backedge.bb2_crit_edge.i.i, %bb2.preheader.i.i
  %self.idx.val.i.i.i12.i = phi i32 [ %self.idx.val.i.i.pre.i.i, %bb1.backedge.bb2_crit_edge.i.i ], [ %self.idx.val.i, %bb2.preheader.i.i ]
  %pos.036.i.i = phi i64 [ %pos.0.be.i41.i, %bb1.backedge.bb2_crit_edge.i.i ], [ 0, %bb2.preheader.i.i ]
  %61 = icmp eq i32 %self.idx.val.i.i.i12.i, 0
  br i1 %61, label %bb17.i.i, label %bb3.i.i.i15.i

bb3.i.i.i15.i:                                    ; preds = %bb2.i13.i
  %62 = add i32 %self.idx.val.i.i.i12.i, -1
  %_4.i.i.i14.i = icmp ult i32 %62, 50
  br i1 %_4.i.i.i14.i, label %bb6.i.i, label %bb6.i.i.i19.i

bb6.i.i.i19.i:                                    ; preds = %bb3.i.i.i15.i
  %_8.i.i.i16.i = load i32, i32* %59, align 4, !alias.scope !579, !noalias !582
  %_9.i.i.i17.i = shl i32 %62, 3
  %_7.not.i.i.i18.i = icmp ult i32 %_8.i.i.i16.i, %_9.i.i.i17.i
  br i1 %_7.not.i.i.i18.i, label %bb9.i.i.i20.i, label %bb6.i.i

bb9.i.i.i20.i:                                    ; preds = %bb6.i.i.i19.i
  store i32 0, i32* %self.idx.i, align 4, !alias.scope !579, !noalias !582
  br label %bb17.i.i

bb17.i.i:                                         ; preds = %bb10.i.i, %bb9.i.i.i20.i, %bb2.i13.i
  %pos.1.i21.i = phi i64 [ %66, %bb10.i.i ], [ %pos.036.i.i, %bb2.i13.i ], [ %pos.036.i.i, %bb9.i.i.i20.i ]
  %_49.i.i = add i64 %pos.1.i21.i, %last_byte.i10.i83
  %_53.i22.i = icmp ult i64 %_49.i.i, %haystack.1
  br i1 %_53.i22.i, label %bb18.i31.i, label %panic.i32.i89, !prof !60

bb6.i.i:                                          ; preds = %bb6.i.i.i19.i, %bb3.i.i.i15.i
  %_3.i.i.i23.i = icmp ugt i64 %pos.036.i.i, %haystack.1
  br i1 %_3.i.i.i23.i, label %bb1.i.i.i24.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i27.i"

bb1.i.i.i24.i:                                    ; preds = %bb6.i.i
; call core::slice::index::slice_start_index_len_fail
  tail call void @_ZN4core5slice5index26slice_start_index_len_fail17h3f1b9df81972beedE(i64 %pos.036.i.i, i64 %haystack.1, %"core::panic::location::Location"* noalias nonnull readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1113 to %"core::panic::location::Location"*)) #22, !noalias !584
  unreachable

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i27.i": ; preds = %bb6.i.i
  %63 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %pos.036.i.i
  %_7.i.i.i.i.i25.i = sub i64 %haystack.1, %pos.036.i.i
  %64 = bitcast i8* %63 to [0 x i8]*
  %65 = tail call { i64, i64 } %60({ i32, i32 }* noalias nonnull align 4 dereferenceable(8) %state, %"memmem::NeedleInfo"* noalias nonnull readonly align 4 dereferenceable(12) %_14, [0 x i8]* noalias nonnull readonly align 1 %64, i64 %_7.i.i.i.i.i25.i, [0 x i8]* noalias nonnull readonly align 1 %needle.0, i64 %needle.1), !noalias !582
  %_25.0.i.i = extractvalue { i64, i64 } %65, 0
  %switch.i.not.i26.i = icmp eq i64 %_25.0.i.i, 0
  br i1 %switch.i.not.i26.i, label %bb8, label %bb10.i.i

bb10.i.i:                                         ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i27.i"
  %_25.1.i.i = extractvalue { i64, i64 } %65, 1
  %66 = add i64 %_25.1.i.i, %pos.036.i.i
  %_39.i.i = add i64 %66, %needle.1
  %_38.i.i = icmp ugt i64 %_39.i.i, %haystack.1
  br i1 %_38.i.i, label %bb8, label %bb17.i.i

bb18.i31.i:                                       ; preds = %bb17.i.i
  %67 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_49.i.i
  %_48.i.i = load i8, i8* %67, align 1, !alias.scope !589, !noalias !590
  %68 = and i8 %_48.i.i, 63
  %69 = zext i8 %68 to i64
  %_5.i.i28.i = shl nuw i64 1, %69
  %_3.i.i29.i = and i64 %_5.i.i28.i, %self.idx8.val.i12
  %.not.i30.i = icmp eq i64 %_3.i.i29.i, 0
  br i1 %.not.i30.i, label %bb44.i34.i, label %bb22.preheader.i.i

bb22.preheader.i.i:                               ; preds = %bb18.i31.i
  br i1 %_5833.i.i84, label %bb26.i.i, label %bb34.preheader.i.i

panic.i32.i89:                                    ; preds = %bb17.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_49.i.i, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1115 to %"core::panic::location::Location"*)) #22, !noalias !582
  unreachable

bb44.i34.i:                                       ; preds = %bb40.i.us.i, %bb18.i31.i
  %needle.1.pn.i.i = phi i64 [ %needle.1, %bb18.i31.i ], [ %shift.i10, %bb40.i.us.i ]
  %pos.2.i33.i = add i64 %needle.1.pn.i.i, %pos.1.i21.i
  br label %bb1.backedge.i42.i

bb34.preheader.i.i:                               ; preds = %bb28.i40.i, %bb22.preheader.i.i
  br i1 %_95191.i.i, label %bb34.i.us.i, label %bb34.i.i, !prof !60

bb34.i.us.i:                                      ; preds = %bb34.preheader.i.i, %bb40.i.us.i
  %iter.sroa.5.0.i.us.i = phi i64 [ %70, %bb40.i.us.i ], [ %self.idx9.val.i14, %bb34.preheader.i.i ]
  %.not11.i.us.i = icmp eq i64 %iter.sroa.5.0.i.us.i, 0
  br i1 %.not11.i.us.i, label %bb8, label %bb36.i.us.i

bb36.i.us.i:                                      ; preds = %bb34.i.us.i
  %70 = add i64 %iter.sroa.5.0.i.us.i, -1
  %_97.i.us.i = add i64 %70, %pos.1.i21.i
  %_101.i.us.i = icmp ult i64 %_97.i.us.i, %haystack.1
  br i1 %_101.i.us.i, label %bb40.i.us.i, label %panic5.i46.i93, !prof !60

bb40.i.us.i:                                      ; preds = %bb36.i.us.i
  %71 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %70
  %_92.i.us.i = load i8, i8* %71, align 1, !alias.scope !591, !noalias !592
  %72 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_97.i.us.i
  %_96.i45.us.i = load i8, i8* %72, align 1, !alias.scope !589, !noalias !590
  %_91.not.i.us.i = icmp eq i8 %_92.i.us.i, %_96.i45.us.i
  br i1 %_91.not.i.us.i, label %bb34.i.us.i, label %bb44.i34.i

bb26.i.i:                                         ; preds = %bb22.preheader.i.i, %bb28.i40.i
  %i.034.i.i = phi i64 [ %76, %bb28.i40.i ], [ %self.idx9.val.i14, %bb22.preheader.i.i ]
  %_68.i.i = add i64 %i.034.i.i, %pos.1.i21.i
  %_72.i35.i = icmp ult i64 %_68.i.i, %haystack.1
  br i1 %_72.i35.i, label %bb27.i37.i, label %panic3.i38.i91, !prof !60

bb27.i37.i:                                       ; preds = %bb26.i.i
  %73 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %i.034.i.i
  %_63.i.i = load i8, i8* %73, align 1, !alias.scope !591, !noalias !592
  %74 = getelementptr inbounds [0 x i8], [0 x i8]* %haystack.0, i64 0, i64 %_68.i.i
  %_67.i36.i = load i8, i8* %74, align 1, !alias.scope !589, !noalias !590
  %_62.i.i = icmp eq i8 %_63.i.i, %_67.i36.i
  br i1 %_62.i.i, label %bb28.i40.i, label %bb30.i.i

panic3.i38.i91:                                   ; preds = %bb26.i.i
  %75 = add i64 %pos.1.i21.i, %self.idx9.val.i14
  %umax.le.i.i90 = tail call i64 @llvm.umax.i64(i64 %haystack.1, i64 %75)
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %umax.le.i.i90, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1119 to %"core::panic::location::Location"*)) #22, !noalias !582
  unreachable

bb28.i40.i:                                       ; preds = %bb27.i37.i
  %76 = add i64 %i.034.i.i, 1
  %exitcond.not.i39.i = icmp eq i64 %76, %needle.1
  br i1 %exitcond.not.i39.i, label %bb34.preheader.i.i, label %bb26.i.i

bb30.i.i:                                         ; preds = %bb27.i37.i
  %77 = add i64 %_68.i.i, %_78.i.i85
  br label %bb1.backedge.i42.i

bb1.backedge.i42.i:                               ; preds = %bb30.i.i, %bb44.i34.i
  %pos.0.be.i41.i = phi i64 [ %pos.2.i33.i, %bb44.i34.i ], [ %77, %bb30.i.i ]
  %_11.i.i = add i64 %pos.0.be.i41.i, %needle.1
  %_10.not.i.i = icmp ugt i64 %_11.i.i, %haystack.1
  br i1 %_10.not.i.i, label %bb8, label %bb1.backedge.bb2_crit_edge.i.i, !llvm.loop !593

bb1.backedge.bb2_crit_edge.i.i:                   ; preds = %bb1.backedge.i42.i
  %self.idx.val.i.i.pre.i.i = load i32, i32* %self.idx.i, align 4, !alias.scope !579, !noalias !582
  br label %bb2.i13.i

bb34.i.i:                                         ; preds = %bb34.preheader.i.i
  %.not11.i.i = icmp eq i64 %self.idx9.val.i14, 0
  br i1 %.not11.i.i, label %bb8, label %panic4.i44.i92

panic4.i44.i92:                                   ; preds = %bb34.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %58, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1121 to %"core::panic::location::Location"*)) #22, !noalias !582
  unreachable

panic5.i46.i93:                                   ; preds = %bb36.i.us.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_97.i.us.i, i64 %haystack.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1123 to %"core::panic::location::Location"*)) #22, !noalias !582
  unreachable

bb8:                                              ; preds = %bb45.i.i81, %bb1.backedge.i.i37, %bb11.i.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i", %bb1.backedge.i42.i, %bb10.i.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i27.i", %bb34.i.us.i, %bb45.i.i, %bb1.backedge.i.i, %bb1.backedge.us.i.i, %bb34.us.i.us.i, %bb34.i.i, %bb1.i87, %bb3.i17, %bb34.preheader.us.i.split.i, %bb1.i, %bb3.i
  %.sroa.0.2.i.pn.i94.pn = phi i64 [ 0, %bb3.i ], [ 0, %bb1.i ], [ 1, %bb34.preheader.us.i.split.i ], [ 0, %bb3.i17 ], [ 0, %bb1.i87 ], [ 1, %bb34.i.i ], [ 1, %bb34.us.i.us.i ], [ 0, %bb1.backedge.us.i.i ], [ 1, %bb45.i.i ], [ 0, %bb1.backedge.i.i ], [ 1, %bb34.i.us.i ], [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i27.i" ], [ 0, %bb10.i.i ], [ 0, %bb1.backedge.i42.i ], [ 0, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i" ], [ 1, %bb45.i.i81 ], [ 0, %bb11.i.i ], [ 0, %bb1.backedge.i.i37 ]
  %.sroa.5.2.i.pn.i95.pn = phi i64 [ undef, %bb3.i ], [ undef, %bb1.i ], [ %pos.036.us.i.i, %bb34.preheader.us.i.split.i ], [ undef, %bb3.i17 ], [ undef, %bb1.i87 ], [ %pos.1.i21.i, %bb34.i.i ], [ %pos.036.us.i.i, %bb34.us.i.us.i ], [ undef, %bb1.backedge.us.i.i ], [ %pos.033.i.i, %bb1.backedge.i.i ], [ %pos.033.i.i, %bb45.i.i ], [ %pos.1.i21.i, %bb34.i.us.i ], [ undef, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i27.i" ], [ undef, %bb10.i.i ], [ undef, %bb1.backedge.i42.i ], [ undef, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i" ], [ %pos.1.i.i, %bb45.i.i81 ], [ undef, %bb11.i.i ], [ undef, %bb1.backedge.i.i37 ]
  %.pn49.i96.pn = insertvalue { i64, i64 } undef, i64 %.sroa.0.2.i.pn.i94.pn, 0
  %.pn = insertvalue { i64, i64 } %.pn49.i96.pn, i64 %.sroa.5.2.i.pn.i95.pn, 1
  ret { i64, i64 } %.pn
}

; memchr::memmem::SearcherRev::new
; Function Attrs: nonlazybind uwtable
define void @_ZN6memchr6memmem11SearcherRev3new17hbd35c934f7444261E(%"memmem::SearcherRev"* noalias nocapture sret(%"memmem::SearcherRev") dereferenceable(64) %0, [0 x i8]* noalias nonnull readonly align 1 %needle.0, i64 %needle.1) unnamed_addr #6 personality i32 (...)* @rust_eh_personality {
start:
  switch i64 %needle.1, label %bb4.preheader.i.i [
    i64 0, label %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit
    i64 1, label %bb4
  ]

bb4.preheader.i.i:                                ; preds = %start
  %1 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %2 = add i64 %needle.1, -1
  %xtraiter = and i64 %needle.1, 3
  %3 = icmp ult i64 %2, 3
  br i1 %3, label %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa, label %bb4.preheader.i.i.new

bb4.preheader.i.i.new:                            ; preds = %bb4.preheader.i.i
  %unroll_iter = and i64 %needle.1, -4
  br label %bb4.i.i

bb4.i.i:                                          ; preds = %bb4.i.i, %bb4.preheader.i.i.new
  %bits.012.i.i = phi i64 [ 0, %bb4.preheader.i.i.new ], [ %19, %bb4.i.i ]
  %iter.sroa.0.011.i.i = phi i8* [ %1, %bb4.preheader.i.i.new ], [ %16, %bb4.i.i ]
  %niter = phi i64 [ %unroll_iter, %bb4.preheader.i.i.new ], [ %niter.nsub.3, %bb4.i.i ]
  %4 = getelementptr inbounds i8, i8* %iter.sroa.0.011.i.i, i64 1
  %b.i.i = load i8, i8* %iter.sroa.0.011.i.i, align 1, !alias.scope !595, !noalias !600
  %5 = and i8 %b.i.i, 63
  %6 = zext i8 %5 to i64
  %_11.i.i = shl nuw i64 1, %6
  %7 = or i64 %_11.i.i, %bits.012.i.i
  %8 = getelementptr inbounds i8, i8* %iter.sroa.0.011.i.i, i64 2
  %b.i.i.1 = load i8, i8* %4, align 1, !alias.scope !595, !noalias !600
  %9 = and i8 %b.i.i.1, 63
  %10 = zext i8 %9 to i64
  %_11.i.i.1 = shl nuw i64 1, %10
  %11 = or i64 %_11.i.i.1, %7
  %12 = getelementptr inbounds i8, i8* %iter.sroa.0.011.i.i, i64 3
  %b.i.i.2 = load i8, i8* %8, align 1, !alias.scope !595, !noalias !600
  %13 = and i8 %b.i.i.2, 63
  %14 = zext i8 %13 to i64
  %_11.i.i.2 = shl nuw i64 1, %14
  %15 = or i64 %_11.i.i.2, %11
  %16 = getelementptr inbounds i8, i8* %iter.sroa.0.011.i.i, i64 4
  %b.i.i.3 = load i8, i8* %12, align 1, !alias.scope !595, !noalias !600
  %17 = and i8 %b.i.i.3, 63
  %18 = zext i8 %17 to i64
  %_11.i.i.3 = shl nuw i64 1, %18
  %19 = or i64 %_11.i.i.3, %15
  %niter.nsub.3 = add i64 %niter, -4
  %niter.ncmp.3 = icmp eq i64 %niter.nsub.3, 0
  br i1 %niter.ncmp.3, label %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa, label %bb4.i.i

_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa: ; preds = %bb4.i.i, %bb4.preheader.i.i
  %.lcssa.ph = phi i64 [ undef, %bb4.preheader.i.i ], [ %19, %bb4.i.i ]
  %bits.012.i.i.unr = phi i64 [ 0, %bb4.preheader.i.i ], [ %19, %bb4.i.i ]
  %iter.sroa.0.011.i.i.unr = phi i8* [ %1, %bb4.preheader.i.i ], [ %16, %bb4.i.i ]
  %lcmp.mod.not = icmp eq i64 %xtraiter, 0
  br i1 %lcmp.mod.not, label %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i, label %bb4.i.i.epil

bb4.i.i.epil:                                     ; preds = %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa, %bb4.i.i.epil
  %bits.012.i.i.epil = phi i64 [ %23, %bb4.i.i.epil ], [ %bits.012.i.i.unr, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa ]
  %iter.sroa.0.011.i.i.epil = phi i8* [ %20, %bb4.i.i.epil ], [ %iter.sroa.0.011.i.i.unr, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa ]
  %epil.iter = phi i64 [ %epil.iter.sub, %bb4.i.i.epil ], [ %xtraiter, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa ]
  %20 = getelementptr inbounds i8, i8* %iter.sroa.0.011.i.i.epil, i64 1
  %b.i.i.epil = load i8, i8* %iter.sroa.0.011.i.i.epil, align 1, !alias.scope !595, !noalias !600
  %21 = and i8 %b.i.i.epil, 63
  %22 = zext i8 %21 to i64
  %_11.i.i.epil = shl nuw i64 1, %22
  %23 = or i64 %_11.i.i.epil, %bits.012.i.i.epil
  %epil.iter.sub = add i64 %epil.iter, -1
  %epil.iter.cmp.not = icmp eq i64 %epil.iter.sub, 0
  br i1 %epil.iter.cmp.not, label %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i, label %bb4.i.i.epil, !llvm.loop !602

_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i: ; preds = %bb4.i.i.epil, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa
  %.lcssa = phi i64 [ %.lcssa.ph, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i.unr-lcssa ], [ %23, %bb4.i.i.epil ]
  tail call void @llvm.experimental.noalias.scope.decl(metadata !603)
  %24 = icmp eq i64 %needle.1, 1
  br i1 %24, label %_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i, label %bb4.lr.ph.i.i

bb4.lr.ph.i.i:                                    ; preds = %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i
  %25 = add i64 %needle.1, -1
  br label %bb4.i10.i

bb4.i10.i:                                        ; preds = %bb15.i.i, %bb4.lr.ph.i.i
  %suffix.sroa.0.039.i.i = phi i64 [ %suffix.sroa.0.1.i.i, %bb15.i.i ], [ %needle.1, %bb4.lr.ph.i.i ]
  %suffix.sroa.9.038.i.i = phi i64 [ %suffix.sroa.9.1.i.i, %bb15.i.i ], [ 1, %bb4.lr.ph.i.i ]
  %candidate_start.036.i.i = phi i64 [ %candidate_start.1.i.i, %bb15.i.i ], [ %25, %bb4.lr.ph.i.i ]
  %offset.035.i.i = phi i64 [ %offset.1.i.i, %bb15.i.i ], [ 0, %bb4.lr.ph.i.i ]
  %26 = xor i64 %offset.035.i.i, -1
  %_16.i.i = add i64 %suffix.sroa.0.039.i.i, %26
  %_21.i.i = icmp ult i64 %_16.i.i, %needle.1
  br i1 %_21.i.i, label %bb5.i.i, label %panic.i.i, !prof !60

bb5.i.i:                                          ; preds = %bb4.i10.i
  %_23.i.i = add i64 %candidate_start.036.i.i, %26
  %_28.i.i = icmp ult i64 %_23.i.i, %needle.1
  br i1 %_28.i.i, label %bb6.i.i, label %panic1.i.i, !prof !60

panic.i.i:                                        ; preds = %bb4.i10.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_16.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1165 to %"core::panic::location::Location"*)) #22, !noalias !606
  unreachable

bb6.i.i:                                          ; preds = %bb5.i.i
  %27 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_16.i.i
  %current.i.i = load i8, i8* %27, align 1, !alias.scope !607, !noalias !600
  %28 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_23.i.i
  %candidate.i.i = load i8, i8* %28, align 1, !alias.scope !607, !noalias !600
  %_5.i.i.i = icmp ult i8 %candidate.i.i, %current.i.i
  br i1 %_5.i.i.i, label %bb10.i.i, label %bb4.i.i.i

bb4.i.i.i:                                        ; preds = %bb6.i.i
  %_8.i.not.i.i = icmp ugt i8 %candidate.i.i, %current.i.i
  br i1 %_8.i.not.i.i, label %bb11.i.i, label %bb8.i.i

panic1.i.i:                                       ; preds = %bb5.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_23.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1167 to %"core::panic::location::Location"*)) #22, !noalias !606
  unreachable

bb10.i.i:                                         ; preds = %bb6.i.i
  %29 = add i64 %candidate_start.036.i.i, -1
  br label %bb15.i.i

bb11.i.i:                                         ; preds = %bb4.i.i.i
  %30 = sub i64 %suffix.sroa.0.039.i.i, %_23.i.i
  br label %bb15.i.i

bb8.i.i:                                          ; preds = %bb4.i.i.i
  %_41.i.i = add nuw i64 %offset.035.i.i, 1
  %_40.i.i = icmp eq i64 %_41.i.i, %suffix.sroa.9.038.i.i
  %spec.select.i.i = select i1 %_40.i.i, i64 0, i64 %_41.i.i
  %31 = select i1 %_40.i.i, i64 %suffix.sroa.9.038.i.i, i64 0
  %spec.select25.i.i = sub i64 %candidate_start.036.i.i, %31
  br label %bb15.i.i

bb15.i.i:                                         ; preds = %bb8.i.i, %bb11.i.i, %bb10.i.i
  %offset.1.i.i = phi i64 [ 0, %bb11.i.i ], [ 0, %bb10.i.i ], [ %spec.select.i.i, %bb8.i.i ]
  %candidate_start.1.i.i = phi i64 [ %_23.i.i, %bb11.i.i ], [ %29, %bb10.i.i ], [ %spec.select25.i.i, %bb8.i.i ]
  %suffix.sroa.9.1.i.i = phi i64 [ %30, %bb11.i.i ], [ 1, %bb10.i.i ], [ %suffix.sroa.9.038.i.i, %bb8.i.i ]
  %suffix.sroa.0.1.i.i = phi i64 [ %suffix.sroa.0.039.i.i, %bb11.i.i ], [ %candidate_start.036.i.i, %bb10.i.i ], [ %suffix.sroa.0.039.i.i, %bb8.i.i ]
  %_12.i.i = icmp ugt i64 %candidate_start.1.i.i, %offset.1.i.i
  br i1 %_12.i.i, label %bb4.i10.i, label %bb4.lr.ph.i11.i

bb4.lr.ph.i11.i:                                  ; preds = %bb15.i.i
  %32 = insertvalue { i64, i64 } undef, i64 %suffix.sroa.0.1.i.i, 0
  %33 = insertvalue { i64, i64 } %32, i64 %suffix.sroa.9.1.i.i, 1
  tail call void @llvm.experimental.noalias.scope.decl(metadata !608)
  br label %bb4.us.i.i

bb4.us.i.i:                                       ; preds = %bb15.us.i.i, %bb4.lr.ph.i11.i
  %suffix.sroa.0.039.us.i.i = phi i64 [ %suffix.sroa.0.1.us.i.i, %bb15.us.i.i ], [ %needle.1, %bb4.lr.ph.i11.i ]
  %suffix.sroa.9.038.us.i.i = phi i64 [ %suffix.sroa.9.1.us.i.i, %bb15.us.i.i ], [ 1, %bb4.lr.ph.i11.i ]
  %candidate_start.036.us.i.i = phi i64 [ %candidate_start.1.us.i.i, %bb15.us.i.i ], [ %25, %bb4.lr.ph.i11.i ]
  %offset.035.us.i.i = phi i64 [ %offset.1.us.i.i, %bb15.us.i.i ], [ 0, %bb4.lr.ph.i11.i ]
  %34 = xor i64 %offset.035.us.i.i, -1
  %_16.us.i.i = add i64 %suffix.sroa.0.039.us.i.i, %34
  %_21.us.i.i = icmp ult i64 %_16.us.i.i, %needle.1
  br i1 %_21.us.i.i, label %bb5.us.i.i, label %panic.i12.i, !prof !60

bb5.us.i.i:                                       ; preds = %bb4.us.i.i
  %_23.us.i.i = add i64 %candidate_start.036.us.i.i, %34
  %_28.us.i.i = icmp ult i64 %_23.us.i.i, %needle.1
  br i1 %_28.us.i.i, label %bb6.us.i.i, label %panic1.i13.i, !prof !60

bb6.us.i.i:                                       ; preds = %bb5.us.i.i
  %35 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_16.us.i.i
  %current.us.i.i = load i8, i8* %35, align 1, !alias.scope !611, !noalias !600
  %36 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_23.us.i.i
  %candidate.us.i.i = load i8, i8* %36, align 1, !alias.scope !611, !noalias !600
  %_11.i.us.i.i = icmp ugt i8 %candidate.us.i.i, %current.us.i.i
  br i1 %_11.i.us.i.i, label %bb10.us.i.i, label %bb9.i.us.i.i

bb9.i.us.i.i:                                     ; preds = %bb6.us.i.i
  %_14.i.us.not.i.i = icmp ult i8 %candidate.us.i.i, %current.us.i.i
  br i1 %_14.i.us.not.i.i, label %bb11.us.i.i, label %bb8.us.i.i

bb11.us.i.i:                                      ; preds = %bb9.i.us.i.i
  %37 = sub i64 %suffix.sroa.0.039.us.i.i, %_23.us.i.i
  br label %bb15.us.i.i

bb8.us.i.i:                                       ; preds = %bb9.i.us.i.i
  %_41.us.i.i = add nuw i64 %offset.035.us.i.i, 1
  %_40.us.i.i = icmp eq i64 %_41.us.i.i, %suffix.sroa.9.038.us.i.i
  %spec.select.us.i.i = select i1 %_40.us.i.i, i64 0, i64 %_41.us.i.i
  %38 = select i1 %_40.us.i.i, i64 %suffix.sroa.9.038.us.i.i, i64 0
  %spec.select25.us.i.i = sub i64 %candidate_start.036.us.i.i, %38
  br label %bb15.us.i.i

bb10.us.i.i:                                      ; preds = %bb6.us.i.i
  %39 = add i64 %candidate_start.036.us.i.i, -1
  br label %bb15.us.i.i

bb15.us.i.i:                                      ; preds = %bb10.us.i.i, %bb8.us.i.i, %bb11.us.i.i
  %offset.1.us.i.i = phi i64 [ 0, %bb11.us.i.i ], [ 0, %bb10.us.i.i ], [ %spec.select.us.i.i, %bb8.us.i.i ]
  %candidate_start.1.us.i.i = phi i64 [ %_23.us.i.i, %bb11.us.i.i ], [ %39, %bb10.us.i.i ], [ %spec.select25.us.i.i, %bb8.us.i.i ]
  %suffix.sroa.9.1.us.i.i = phi i64 [ %37, %bb11.us.i.i ], [ 1, %bb10.us.i.i ], [ %suffix.sroa.9.038.us.i.i, %bb8.us.i.i ]
  %suffix.sroa.0.1.us.i.i = phi i64 [ %suffix.sroa.0.039.us.i.i, %bb11.us.i.i ], [ %candidate_start.036.us.i.i, %bb10.us.i.i ], [ %suffix.sroa.0.039.us.i.i, %bb8.us.i.i ]
  %_12.us.i.i = icmp ugt i64 %candidate_start.1.us.i.i, %offset.1.us.i.i
  br i1 %_12.us.i.i, label %bb4.us.i.i, label %_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i

panic.i12.i:                                      ; preds = %bb4.us.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_16.us.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1165 to %"core::panic::location::Location"*)) #22, !noalias !612
  unreachable

panic1.i13.i:                                     ; preds = %bb5.us.i.i
; call core::panicking::panic_bounds_check
  tail call void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64 %_23.us.i.i, i64 %needle.1, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1167 to %"core::panic::location::Location"*)) #22, !noalias !612
  unreachable

_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i: ; preds = %bb15.us.i.i, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i
  %40 = phi { i64, i64 } [ { i64 1, i64 1 }, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ], [ %33, %bb15.us.i.i ]
  %.sroa.0.0.i32.i = phi i64 [ 1, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ], [ %suffix.sroa.0.1.i.i, %bb15.us.i.i ]
  %.sroa.3.0.i14.i = phi i64 [ 1, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ], [ %suffix.sroa.9.1.us.i.i, %bb15.us.i.i ]
  %.sroa.0.0.i15.i = phi i64 [ 1, %_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E.exit.i ], [ %suffix.sroa.0.1.us.i.i, %bb15.us.i.i ]
  %41 = insertvalue { i64, i64 } undef, i64 %.sroa.0.0.i15.i, 0
  %42 = insertvalue { i64, i64 } %41, i64 %.sroa.3.0.i14.i, 1
  %_16.i = icmp ult i64 %.sroa.0.0.i32.i, %.sroa.0.0.i15.i
  %..i = select i1 %_16.i, { i64, i64 } %40, { i64, i64 } %42
  %min_suffix.0.max_suffix.0.i = select i1 %_16.i, i64 %.sroa.0.0.i32.i, i64 %.sroa.0.0.i15.i
  %_15.sroa.0.0.i = extractvalue { i64, i64 } %..i, 1
  tail call void @llvm.experimental.noalias.scope.decl(metadata !613)
  %_6.i.i = sub i64 %needle.1, %min_suffix.0.max_suffix.0.i
  %43 = icmp ult i64 %_6.i.i, %min_suffix.0.max_suffix.0.i
  %.0.sroa.speculated.i.i.i.i.i = select i1 %43, i64 %min_suffix.0.max_suffix.0.i, i64 %_6.i.i
  %_11.i17.i = shl i64 %_6.i.i, 1
  %_10.not.i.i = icmp ult i64 %_11.i17.i, %needle.1
  br i1 %_10.not.i.i, label %bb3.i.i, label %bb5.i

bb3.i.i:                                          ; preds = %_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i
  %_4.not.i.i.i = icmp ugt i64 %min_suffix.0.max_suffix.0.i, %needle.1
  br i1 %_4.not.i.i.i, label %bb1.i.i.i, label %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E.exit.i.i"

bb1.i.i.i:                                        ; preds = %bb3.i.i
; call core::panicking::panic
  tail call void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [35 x i8] }>* @alloc1359 to [0 x i8]*), i64 35, %"core::panic::location::Location"* noalias nonnull readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1157 to %"core::panic::location::Location"*)) #22, !noalias !616
  unreachable

"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E.exit.i.i": ; preds = %bb3.i.i
  %44 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %min_suffix.0.max_suffix.0.i
  %_31.i.i = sub i64 %min_suffix.0.max_suffix.0.i, %_15.sroa.0.0.i
  %_3.i.i.i.i = icmp ult i64 %min_suffix.0.max_suffix.0.i, %_15.sroa.0.0.i
  br i1 %_3.i.i.i.i, label %bb1.i.i.i.i, label %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i"

bb1.i.i.i.i:                                      ; preds = %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E.exit.i.i"
; call core::slice::index::slice_start_index_len_fail
  tail call void @_ZN4core5slice5index26slice_start_index_len_fail17h3f1b9df81972beedE(i64 %_31.i.i, i64 %min_suffix.0.max_suffix.0.i, %"core::panic::location::Location"* noalias nonnull readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1159 to %"core::panic::location::Location"*)) #22, !noalias !620
  unreachable

"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i": ; preds = %"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E.exit.i.i"
  %45 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_31.i.i
  tail call void @llvm.experimental.noalias.scope.decl(metadata !625)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !628)
  %_3.not.i.i.i = icmp ugt i64 %_6.i.i, %_15.sroa.0.0.i
  br i1 %_3.not.i.i.i, label %.thread.i.i, label %bb2.i.i.i.i

bb2.i.i.i.i:                                      ; preds = %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i"
  tail call void @llvm.experimental.noalias.scope.decl(metadata !630)
  tail call void @llvm.experimental.noalias.scope.decl(metadata !633)
  %_8.i.i.i.i = icmp ult i64 %_6.i.i, 4
  br i1 %_8.i.i.i.i, label %bb7.i.i.i.i, label %bb14.i.i.i.i

bb14.i.i.i.i:                                     ; preds = %bb2.i.i.i.i
  %_38.i.i.i.i = add i64 %_6.i.i, -4
  %46 = getelementptr inbounds i8, i8* %45, i64 %_38.i.i.i.i
  %47 = getelementptr inbounds i8, i8* %44, i64 %_38.i.i.i.i
  %_4671.i.i.i.i = icmp sgt i64 %_38.i.i.i.i, 0
  br i1 %_4671.i.i.i.i, label %bb20.i.i.i.i, label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i

bb7.i.i.i.i:                                      ; preds = %bb2.i.i.i.i, %bb9.i.i.i.i
  %iter.sroa.9.0.i.i.i.i = phi i64 [ %50, %bb9.i.i.i.i ], [ 0, %bb2.i.i.i.i ]
  %exitcond.not.i.i.i.i = icmp eq i64 %iter.sroa.9.0.i.i.i.i, %_6.i.i
  br i1 %exitcond.not.i.i.i.i, label %bb5.i, label %bb9.i.i.i.i

bb9.i.i.i.i:                                      ; preds = %bb7.i.i.i.i
  %48 = getelementptr inbounds i8, i8* %44, i64 %iter.sroa.9.0.i.i.i.i
  %49 = getelementptr inbounds i8, i8* %45, i64 %iter.sroa.9.0.i.i.i.i
  %50 = add nuw nsw i64 %iter.sroa.9.0.i.i.i.i, 1
  %b1.i.i.i.i = load i8, i8* %49, align 1, !alias.scope !635, !noalias !636
  %b2.i.i.i.i = load i8, i8* %48, align 1, !alias.scope !637, !noalias !638
  %_23.not.i.i.i.i = icmp eq i8 %b1.i.i.i.i, %b2.i.i.i.i
  br i1 %_23.not.i.i.i.i, label %bb7.i.i.i.i, label %.thread.i.i

bb20.i.i.i.i:                                     ; preds = %bb14.i.i.i.i, %bb24.i.i.i.i
  %py.073.i.i.i.i = phi i8* [ %52, %bb24.i.i.i.i ], [ %44, %bb14.i.i.i.i ]
  %px.072.i.i.i.i = phi i8* [ %51, %bb24.i.i.i.i ], [ %45, %bb14.i.i.i.i ]
  %_50.i.i.i.i = bitcast i8* %px.072.i.i.i.i to i32*
  %_50.val.i.i.i.i = load i32, i32* %_50.i.i.i.i, align 1, !alias.scope !635, !noalias !636
  %_53.i.i.i.i = bitcast i8* %py.073.i.i.i.i to i32*
  %_53.val.i.i.i.i = load i32, i32* %_53.i.i.i.i, align 1, !alias.scope !637, !noalias !638
  %_55.not.i.i.i.i = icmp eq i32 %_50.val.i.i.i.i, %_53.val.i.i.i.i
  br i1 %_55.not.i.i.i.i, label %bb24.i.i.i.i, label %.thread.i.i

bb24.i.i.i.i:                                     ; preds = %bb20.i.i.i.i
  %51 = getelementptr inbounds i8, i8* %px.072.i.i.i.i, i64 4
  %52 = getelementptr inbounds i8, i8* %py.073.i.i.i.i, i64 4
  %_46.i.i.i.i = icmp ult i8* %51, %46
  br i1 %_46.i.i.i.i, label %bb20.i.i.i.i, label %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i

_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i: ; preds = %bb24.i.i.i.i, %bb14.i.i.i.i
  %_63.i.i.i.i = bitcast i8* %46 to i32*
  %_63.val.i.i.i.i = load i32, i32* %_63.i.i.i.i, align 1, !alias.scope !635, !noalias !636
  %_66.i.i.i.i = bitcast i8* %47 to i32*
  %_66.val.i.i.i.i = load i32, i32* %_66.i.i.i.i, align 1, !alias.scope !637, !noalias !638
  %53 = icmp eq i32 %_63.val.i.i.i.i, %_66.val.i.i.i.i
  br i1 %53, label %bb5.i, label %.thread.i.i

.thread.i.i:                                      ; preds = %bb20.i.i.i.i, %bb9.i.i.i.i, %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i, %"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE.exit.i.i"
  br label %bb5.i

bb4:                                              ; preds = %start
  %54 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %_7 = load i8, i8* %54, align 1
  br label %bb5.i

bb5.i:                                            ; preds = %bb7.i.i.i.i, %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i, %.thread.i.i, %bb4, %_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i
  %kind.sroa.0.075 = phi i8 [ 2, %_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i ], [ 1, %bb4 ], [ 2, %.thread.i.i ], [ 2, %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i ], [ 2, %bb7.i.i.i.i ]
  %kind.sroa.6.073 = phi i8 [ undef, %_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i ], [ %_7, %bb4 ], [ undef, %.thread.i.i ], [ undef, %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i ], [ undef, %bb7.i.i.i.i ]
  %kind.sroa.7.sroa.3.071 = phi i64 [ %.lcssa, %_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i ], [ undef, %bb4 ], [ %.lcssa, %.thread.i.i ], [ %.lcssa, %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i ], [ %.lcssa, %bb7.i.i.i.i ]
  %kind.sroa.7.sroa.4.069 = phi i64 [ %min_suffix.0.max_suffix.0.i, %_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i ], [ undef, %bb4 ], [ %min_suffix.0.max_suffix.0.i, %.thread.i.i ], [ %min_suffix.0.max_suffix.0.i, %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i ], [ %min_suffix.0.max_suffix.0.i, %bb7.i.i.i.i ]
  %kind.sroa.7.sroa.5.067 = phi i64 [ 1, %_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i ], [ 1, %bb4 ], [ 1, %.thread.i.i ], [ 0, %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i ], [ 0, %bb7.i.i.i.i ]
  %kind.sroa.7.sroa.6.065 = phi i64 [ %.0.sroa.speculated.i.i.i.i.i, %_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E.exit16.i ], [ undef, %bb4 ], [ %.0.sroa.speculated.i.i.i.i.i, %.thread.i.i ], [ %_15.sroa.0.0.i, %_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE.exit.i.i ], [ %_15.sroa.0.0.i, %bb7.i.i.i.i ]
  %55 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %_9.i = add i64 %needle.1, -1
  %56 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %_9.i
  %_8.i = load i8, i8* %56, align 1, !alias.scope !639
  %_6.i.i10 = zext i8 %_8.i to i32
  %57 = getelementptr inbounds [0 x i8], [0 x i8]* %needle.0, i64 0, i64 %needle.1
  %_20.i.i.i.i = ptrtoint [0 x i8]* %needle.0 to i64
  br label %bb11.i

bb11.i:                                           ; preds = %bb13.i, %bb5.i
  %iter.sroa.5.0.i = phi i8* [ %57, %bb5.i ], [ %60, %bb13.i ]
  %_3.not.i.i = phi i1 [ false, %bb5.i ], [ true, %bb13.i ]
  %iter.sroa.10.0.i = phi i64 [ 1, %bb5.i ], [ 0, %bb13.i ]
  %nh.sroa.10.0.i = phi i32 [ 1, %bb5.i ], [ %63, %bb13.i ]
  %nh.sroa.0.0.i = phi i32 [ %_6.i.i10, %bb5.i ], [ %62, %bb13.i ]
  br i1 %_3.not.i.i, label %bb6.i.i11, label %bb2.i.i

bb6.i.i11:                                        ; preds = %bb2.i.i, %bb11.i
  %_11.i.i.i.i = phi i8* [ %storemerge.i.i.i.i, %bb2.i.i ], [ %iter.sroa.5.0.i, %bb11.i ]
  %_12.i.i.i.i = icmp eq i8* %55, %_11.i.i.i.i
  br i1 %_12.i.i.i.i, label %_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit, label %bb13.i

bb2.i.i:                                          ; preds = %bb11.i
  %_7.i.i = add nsw i64 %iter.sroa.10.0.i, -1
  %_18.i.i.i.i = ptrtoint i8* %iter.sroa.5.0.i to i64
  %58 = sub nuw i64 %_18.i.i.i.i, %_20.i.i.i.i
  %_3.not.i.i.i.i = icmp ugt i64 %58, %_7.i.i
  %.idx.i.i.i.i = sub nsw i64 0, %iter.sroa.10.0.i
  %59 = getelementptr inbounds i8, i8* %iter.sroa.5.0.i, i64 %.idx.i.i.i.i
  %storemerge.i.i.i.i = select i1 %_3.not.i.i.i.i, i8* %59, i8* %55
  br label %bb6.i.i11

bb13.i:                                           ; preds = %bb6.i.i11
  %60 = getelementptr inbounds i8, i8* %_11.i.i.i.i, i64 -1
  %b.i = load i8, i8* %60, align 1, !alias.scope !639
  %61 = shl i32 %nh.sroa.0.0.i, 1
  %_6.i5.i = zext i8 %b.i to i32
  %62 = add i32 %61, %_6.i5.i
  %63 = shl i32 %nh.sroa.10.0.i, 1
  br label %bb11.i

_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E.exit: ; preds = %bb6.i.i11, %start
  %kind.sroa.0.076 = phi i8 [ 0, %start ], [ %kind.sroa.0.075, %bb6.i.i11 ]
  %kind.sroa.6.074 = phi i8 [ undef, %start ], [ %kind.sroa.6.073, %bb6.i.i11 ]
  %kind.sroa.7.sroa.3.072 = phi i64 [ undef, %start ], [ %kind.sroa.7.sroa.3.071, %bb6.i.i11 ]
  %kind.sroa.7.sroa.4.070 = phi i64 [ undef, %start ], [ %kind.sroa.7.sroa.4.069, %bb6.i.i11 ]
  %kind.sroa.7.sroa.5.068 = phi i64 [ undef, %start ], [ %kind.sroa.7.sroa.5.067, %bb6.i.i11 ]
  %kind.sroa.7.sroa.6.066 = phi i64 [ undef, %start ], [ %kind.sroa.7.sroa.6.065, %bb6.i.i11 ]
  %.sroa.3.0.i = phi i32 [ 1, %start ], [ %nh.sroa.10.0.i, %bb6.i.i11 ]
  %.sroa.0.0.i = phi i32 [ 0, %start ], [ %nh.sroa.0.0.i, %bb6.i.i11 ]
  %64 = getelementptr [0 x i8], [0 x i8]* %needle.0, i64 0, i64 0
  %65 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 0, i32 0
  store i8* %64, i8** %65, align 8
  %66 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 0, i32 1
  store i64 %needle.1, i64* %66, align 8
  %67 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 2, i32 0
  store i32 %.sroa.0.0.i, i32* %67, align 8
  %68 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 2, i32 1
  store i32 %.sroa.3.0.i, i32* %68, align 4
  %_17.sroa.0.0..sroa_idx = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 1, i32 0
  store i8 %kind.sroa.0.076, i8* %_17.sroa.0.0..sroa_idx, align 8
  %_17.sroa.4.0..sroa_idx = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 1, i32 1, i64 0
  store i8 %kind.sroa.6.074, i8* %_17.sroa.4.0..sroa_idx, align 1
  %_17.sroa.5.sroa.4.0._17.sroa.5.0..sroa_idx.sroa_idx = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 1, i32 1, i64 7
  %_17.sroa.5.sroa.4.0._17.sroa.5.0..sroa_idx.sroa_cast = bitcast i8* %_17.sroa.5.sroa.4.0._17.sroa.5.0..sroa_idx.sroa_idx to i64*
  store i64 %kind.sroa.7.sroa.3.072, i64* %_17.sroa.5.sroa.4.0._17.sroa.5.0..sroa_idx.sroa_cast, align 8
  %_17.sroa.5.sroa.5.0._17.sroa.5.0..sroa_idx.sroa_idx = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 1, i32 1, i64 15
  %_17.sroa.5.sroa.5.0._17.sroa.5.0..sroa_idx.sroa_cast = bitcast i8* %_17.sroa.5.sroa.5.0._17.sroa.5.0..sroa_idx.sroa_idx to i64*
  store i64 %kind.sroa.7.sroa.4.070, i64* %_17.sroa.5.sroa.5.0._17.sroa.5.0..sroa_idx.sroa_cast, align 8
  %_17.sroa.5.sroa.6.0._17.sroa.5.0..sroa_idx.sroa_idx = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 1, i32 1, i64 23
  %_17.sroa.5.sroa.6.0._17.sroa.5.0..sroa_idx.sroa_cast = bitcast i8* %_17.sroa.5.sroa.6.0._17.sroa.5.0..sroa_idx.sroa_idx to i64*
  store i64 %kind.sroa.7.sroa.5.068, i64* %_17.sroa.5.sroa.6.0._17.sroa.5.0..sroa_idx.sroa_cast, align 8
  %_17.sroa.5.sroa.7.0._17.sroa.5.0..sroa_idx.sroa_idx = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 1, i32 1, i64 31
  %_17.sroa.5.sroa.7.0._17.sroa.5.0..sroa_idx.sroa_cast = bitcast i8* %_17.sroa.5.sroa.7.0._17.sroa.5.0..sroa_idx.sroa_idx to i64*
  store i64 %kind.sroa.7.sroa.6.066, i64* %_17.sroa.5.sroa.7.0._17.sroa.5.0..sroa_idx.sroa_cast, align 8
  ret void
}

; memchr::memmem::SearcherRev::needle
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define { [0 x i8]*, i64 } @_ZN6memchr6memmem11SearcherRev6needle17hdba161fcdb320d48E(%"memmem::SearcherRev"* noalias nocapture readonly align 8 dereferenceable(64) %self) unnamed_addr #10 {
start:
  %0 = bitcast %"memmem::SearcherRev"* %self to [0 x i8]**
  %1 = load [0 x i8]*, [0 x i8]** %0, align 8, !alias.scope !642, !nonnull !142
  %2 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %self, i64 0, i32 0, i32 1
  %3 = load i64, i64* %2, align 8, !alias.scope !642
  %4 = insertvalue { [0 x i8]*, i64 } undef, [0 x i8]* %1, 0
  %5 = insertvalue { [0 x i8]*, i64 } %4, i64 %3, 1
  ret { [0 x i8]*, i64 } %5
}

; memchr::memmem::SearcherRev::as_ref
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @_ZN6memchr6memmem11SearcherRev6as_ref17hc10356cfcfbc1104E(%"memmem::SearcherRev"* noalias nocapture sret(%"memmem::SearcherRev") dereferenceable(64) %0, %"memmem::SearcherRev"* noalias nocapture readonly align 8 dereferenceable(64) %self) unnamed_addr #10 {
start:
  %kind.sroa.7 = alloca [38 x i8], align 2
  %kind.sroa.7.0.sroa_idx22 = getelementptr inbounds [38 x i8], [38 x i8]* %kind.sroa.7, i64 0, i64 0
  call void @llvm.lifetime.start.p0i8(i64 38, i8* nonnull %kind.sroa.7.0.sroa_idx22)
  %1 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %self, i64 0, i32 1, i32 0
  %2 = load i8, i8* %1, align 8, !range !278
  %_3 = zext i8 %2 to i64
  switch i64 %_3, label %bb2 [
    i64 0, label %bb5
    i64 1, label %bb4
    i64 2, label %bb1
  ]

bb2:                                              ; preds = %start
  unreachable

bb4:                                              ; preds = %start
  %3 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %self, i64 0, i32 1, i32 1, i64 0
  %b = load i8, i8* %3, align 1
  br label %bb5

bb1:                                              ; preds = %start
  %tw.sroa.0.0..sroa_idx5 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %self, i64 0, i32 1, i32 1, i64 7
  %_7.sroa.0.0.kind.sroa.7.8.sroa_idx.sroa_idx = getelementptr inbounds [38 x i8], [38 x i8]* %kind.sroa.7, i64 0, i64 6
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 2 dereferenceable(32) %_7.sroa.0.0.kind.sroa.7.8.sroa_idx.sroa_idx, i8* noundef nonnull align 8 dereferenceable(32) %tw.sroa.0.0..sroa_idx5, i64 32, i1 false)
  br label %bb5

bb5:                                              ; preds = %start, %bb4, %bb1
  %kind.sroa.6.0 = phi i8 [ undef, %bb1 ], [ %b, %bb4 ], [ undef, %start ]
  %kind.sroa.0.0 = phi i8 [ 2, %bb1 ], [ 1, %bb4 ], [ 0, %start ]
  %4 = bitcast %"memmem::SearcherRev"* %self to [0 x i8]**
  %5 = load [0 x i8]*, [0 x i8]** %4, align 8, !alias.scope !647, !nonnull !142
  %6 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %self, i64 0, i32 0, i32 1
  %7 = load i64, i64* %6, align 8, !alias.scope !647
  %8 = getelementptr [0 x i8], [0 x i8]* %5, i64 0, i64 0
  %9 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %self, i64 0, i32 2, i32 0
  %_12.0 = load i32, i32* %9, align 8
  %10 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %self, i64 0, i32 2, i32 1
  %_12.1 = load i32, i32* %10, align 4
  %_13.sroa.5.0..sroa_idx = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 1, i32 1, i64 1
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 2 dereferenceable(38) %_13.sroa.5.0..sroa_idx, i8* noundef nonnull align 2 dereferenceable(38) %kind.sroa.7.0.sroa_idx22, i64 38, i1 false)
  %11 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 0, i32 0
  store i8* %8, i8** %11, align 8
  %12 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 0, i32 1
  store i64 %7, i64* %12, align 8
  %13 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 2, i32 0
  store i32 %_12.0, i32* %13, align 8
  %14 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 2, i32 1
  store i32 %_12.1, i32* %14, align 4
  %_13.sroa.0.0..sroa_idx = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 1, i32 0
  store i8 %kind.sroa.0.0, i8* %_13.sroa.0.0..sroa_idx, align 8
  %_13.sroa.4.0..sroa_idx = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %0, i64 0, i32 1, i32 1, i64 0
  store i8 %kind.sroa.6.0, i8* %_13.sroa.4.0..sroa_idx, align 1
  call void @llvm.lifetime.end.p0i8(i64 38, i8* nonnull %kind.sroa.7.0.sroa_idx22)
  ret void
}

; <memchr::cow::CowBytes as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN58_$LT$memchr..cow..CowBytes$u20$as$u20$core..fmt..Debug$GT$3fmt17hb91ccca4ba3204d1E"({ i8*, i64 }* noalias readonly align 8 dereferenceable(16) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14 = alloca { i8*, i64 }*, align 8
  %_5 = alloca %"core::fmt::builders::DebugTuple", align 8
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1186 to [0 x i8]*), i64 8)
  %1 = bitcast { i8*, i64 }** %_14 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1)
  store { i8*, i64 }* %self, { i8*, i64 }** %_14, align 8
  %_11.0 = bitcast { i8*, i64 }** %_14 to {}*
; call core::fmt::builders::DebugTuple::field
  %_9 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5, {}* nonnull align 1 %_11.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.0 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1)
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret i1 %2
}

; <memchr::cow::Imp as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN53_$LT$memchr..cow..Imp$u20$as$u20$core..fmt..Debug$GT$3fmt17h6a7ac8c510544f61E"({ i8*, i64 }* noalias readonly align 8 dereferenceable(16) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14 = alloca { [0 x i8]*, i64 }*, align 8
  %_5 = alloca %"core::fmt::builders::DebugTuple", align 8
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [3 x i8] }>* @alloc1190 to [0 x i8]*), i64 3)
  %1 = bitcast { [0 x i8]*, i64 }** %_14 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1)
  %2 = bitcast { [0 x i8]*, i64 }** %_14 to { i8*, i64 }**
  store { i8*, i64 }* %self, { i8*, i64 }** %2, align 8
  %_11.0 = bitcast { [0 x i8]*, i64 }** %_14 to {}*
; call core::fmt::builders::DebugTuple::field
  %_9 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5, {}* nonnull align 1 %_11.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.1 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1)
; call core::fmt::builders::DebugTuple::finish
  %3 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret i1 %3
}

; <memchr::memmem::genericsimd::Forward as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN73_$LT$memchr..memmem..genericsimd..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17heb362d32e8c2db5eE"({ i8, i8 }* noalias readonly align 1 dereferenceable(2) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25 = alloca i8*, align 8
  %_17 = alloca i8*, align 8
  %_6 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds { i8, i8 }, { i8, i8 }* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds { i8, i8 }, { i8, i8 }* %self, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1256 to [0 x i8]*), i64 7)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast i8** %_17 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store i8* %__self_0_0, i8** %_17, align 8
  %_14.0 = bitcast i8** %_17 to {}*
; call core::fmt::builders::DebugStruct::field
  %_10 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1216 to [0 x i8]*), i64 6, {}* nonnull align 1 %_14.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast i8** %_25 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store i8* %__self_0_1, i8** %_25, align 8
  %_22.0 = bitcast i8** %_25 to {}*
; call core::fmt::builders::DebugStruct::field
  %_18 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1217 to [0 x i8]*), i64 6, {}* nonnull align 1 %_22.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %4
}

; <memchr::memmem::prefilter::Prefilter as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN73_$LT$memchr..memmem..prefilter..Prefilter$u20$as$u20$core..fmt..Debug$GT$3fmt17h17201383e0067fa1E"(i8* noalias nocapture readonly align 1 dereferenceable(1) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %0 = load i8, i8* %self, align 1, !range !654
  %switch.not = icmp eq i8 %0, 1
  %. = select i1 %switch.not, [0 x i8]* bitcast (<{ [4 x i8] }>* @alloc1200 to [0 x i8]*), [0 x i8]* bitcast (<{ [4 x i8] }>* @alloc1368 to [0 x i8]*)
; call core::fmt::Formatter::write_str
  %1 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 %., i64 4)
  ret i1 %1
}

; <memchr::memmem::prefilter::PrefilterState as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN78_$LT$memchr..memmem..prefilter..PrefilterState$u20$as$u20$core..fmt..Debug$GT$3fmt17hc5db09aace33ebabE"({ i32, i32 }* noalias readonly align 4 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25 = alloca i32*, align 8
  %_17 = alloca i32*, align 8
  %_6 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %self, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [14 x i8] }>* @alloc1202 to [0 x i8]*), i64 14)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast i32** %_17 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store i32* %__self_0_0, i32** %_17, align 8
  %_14.0 = bitcast i32** %_17 to {}*
; call core::fmt::builders::DebugStruct::field
  %_10 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1203 to [0 x i8]*), i64 5, {}* nonnull align 1 %_14.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast i32** %_25 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store i32* %__self_0_1, i32** %_25, align 8
  %_22.0 = bitcast i32** %_25 to {}*
; call core::fmt::builders::DebugStruct::field
  %_18 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1207 to [0 x i8]*), i64 7, {}* nonnull align 1 %_22.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %4
}

; <memchr::memmem::rabinkarp::NeedleHash as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN74_$LT$memchr..memmem..rabinkarp..NeedleHash$u20$as$u20$core..fmt..Debug$GT$3fmt17h1fd51ca258f7a7d4E"({ i32, i32 }* noalias readonly align 4 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25 = alloca i32*, align 8
  %_17 = alloca i32*, align 8
  %_6 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %self, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [10 x i8] }>* @alloc1208 to [0 x i8]*), i64 10)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast i32** %_17 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store i32* %__self_0_0, i32** %_17, align 8
  %_14.0 = bitcast i32** %_17 to {}*
; call core::fmt::builders::DebugStruct::field
  %_10 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1209 to [0 x i8]*), i64 4, {}* nonnull align 1 %_14.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.4 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast i32** %_25 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store i32* %__self_0_1, i32** %_25, align 8
  %_22.0 = bitcast i32** %_25 to {}*
; call core::fmt::builders::DebugStruct::field
  %_18 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [9 x i8] }>* @alloc1213 to [0 x i8]*), i64 9, {}* nonnull align 1 %_22.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %4
}

; <memchr::memmem::rabinkarp::Hash as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN68_$LT$memchr..memmem..rabinkarp..Hash$u20$as$u20$core..fmt..Debug$GT$3fmt17h8ddc4f31b9cb2177E"(i32* noalias readonly align 4 dereferenceable(4) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14 = alloca i32*, align 8
  %_5 = alloca %"core::fmt::builders::DebugTuple", align 8
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1214 to [0 x i8]*), i64 4)
  %1 = bitcast i32** %_14 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1)
  store i32* %self, i32** %_14, align 8
  %_11.0 = bitcast i32** %_14 to {}*
; call core::fmt::builders::DebugTuple::field
  %_9 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5, {}* nonnull align 1 %_11.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1)
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret i1 %2
}

; <memchr::memmem::rarebytes::RareNeedleBytes as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN79_$LT$memchr..memmem..rarebytes..RareNeedleBytes$u20$as$u20$core..fmt..Debug$GT$3fmt17h7fb6c00f79c486bcE"({ i8, i8 }* noalias readonly align 1 dereferenceable(2) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25 = alloca i8*, align 8
  %_17 = alloca i8*, align 8
  %_6 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds { i8, i8 }, { i8, i8 }* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds { i8, i8 }, { i8, i8 }* %self, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [15 x i8] }>* @alloc1215 to [0 x i8]*), i64 15)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast i8** %_17 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store i8* %__self_0_0, i8** %_17, align 8
  %_14.0 = bitcast i8** %_17 to {}*
; call core::fmt::builders::DebugStruct::field
  %_10 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1216 to [0 x i8]*), i64 6, {}* nonnull align 1 %_14.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast i8** %_25 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store i8* %__self_0_1, i8** %_25, align 8
  %_22.0 = bitcast i8** %_25 to {}*
; call core::fmt::builders::DebugStruct::field
  %_18 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1217 to [0 x i8]*), i64 6, {}* nonnull align 1 %_22.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %4
}

; <memchr::memmem::twoway::Forward as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN68_$LT$memchr..memmem..twoway..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17h58ed7f655d1667fdE"(%"memmem::twoway::Forward"* noalias readonly align 8 dereferenceable(32) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14 = alloca %"memmem::twoway::TwoWay"*, align 8
  %_5 = alloca %"core::fmt::builders::DebugTuple", align 8
  %__self_0_0 = getelementptr inbounds %"memmem::twoway::Forward", %"memmem::twoway::Forward"* %self, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1256 to [0 x i8]*), i64 7)
  %1 = bitcast %"memmem::twoway::TwoWay"** %_14 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1)
  store %"memmem::twoway::TwoWay"* %__self_0_0, %"memmem::twoway::TwoWay"** %_14, align 8
  %_11.0 = bitcast %"memmem::twoway::TwoWay"** %_14 to {}*
; call core::fmt::builders::DebugTuple::field
  %_9 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5, {}* nonnull align 1 %_11.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.5 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1)
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret i1 %2
}

; <memchr::memmem::twoway::Reverse as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN68_$LT$memchr..memmem..twoway..Reverse$u20$as$u20$core..fmt..Debug$GT$3fmt17hf32879c849379d46E"(%"memmem::twoway::Reverse"* noalias readonly align 8 dereferenceable(32) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14 = alloca %"memmem::twoway::TwoWay"*, align 8
  %_5 = alloca %"core::fmt::builders::DebugTuple", align 8
  %__self_0_0 = getelementptr inbounds %"memmem::twoway::Reverse", %"memmem::twoway::Reverse"* %self, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1222 to [0 x i8]*), i64 7)
  %1 = bitcast %"memmem::twoway::TwoWay"** %_14 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1)
  store %"memmem::twoway::TwoWay"* %__self_0_0, %"memmem::twoway::TwoWay"** %_14, align 8
  %_11.0 = bitcast %"memmem::twoway::TwoWay"** %_14 to {}*
; call core::fmt::builders::DebugTuple::field
  %_9 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5, {}* nonnull align 1 %_11.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.5 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1)
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret i1 %2
}

; <memchr::memmem::twoway::TwoWay as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN67_$LT$memchr..memmem..twoway..TwoWay$u20$as$u20$core..fmt..Debug$GT$3fmt17h8a05b576142eef61E"(%"memmem::twoway::TwoWay"* noalias readonly align 8 dereferenceable(32) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_34 = alloca { i64, i64 }*, align 8
  %_26 = alloca i64*, align 8
  %_18 = alloca i64*, align 8
  %_7 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds %"memmem::twoway::TwoWay", %"memmem::twoway::TwoWay"* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds %"memmem::twoway::TwoWay", %"memmem::twoway::TwoWay"* %self, i64 0, i32 1
  %__self_0_2 = getelementptr inbounds %"memmem::twoway::TwoWay", %"memmem::twoway::TwoWay"* %self, i64 0, i32 2
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_7 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1348 to [0 x i8]*), i64 6)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_7 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast i64** %_18 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store i64* %__self_0_0, i64** %_18, align 8
  %_15.0 = bitcast i64** %_18 to {}*
; call core::fmt::builders::DebugStruct::field
  %_11 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1224 to [0 x i8]*), i64 7, {}* nonnull align 1 %_15.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.6 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast i64** %_26 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store i64* %__self_0_1, i64** %_26, align 8
  %_23.0 = bitcast i64** %_26 to {}*
; call core::fmt::builders::DebugStruct::field
  %_19 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [12 x i8] }>* @alloc1228 to [0 x i8]*), i64 12, {}* nonnull align 1 %_23.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.7 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
  %4 = bitcast { i64, i64 }** %_34 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4)
  store { i64, i64 }* %__self_0_2, { i64, i64 }** %_34, align 8
  %_31.0 = bitcast { i64, i64 }** %_34 to {}*
; call core::fmt::builders::DebugStruct::field
  %_27 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1237 to [0 x i8]*), i64 5, {}* nonnull align 1 %_31.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.8 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4)
; call core::fmt::builders::DebugStruct::finish
  %5 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %5
}

; <memchr::memmem::twoway::Shift as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN66_$LT$memchr..memmem..twoway..Shift$u20$as$u20$core..fmt..Debug$GT$3fmt17hfd4b4a2dd5d7413fE"({ i64, i64 }* noalias readonly align 8 dereferenceable(16) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_34 = alloca i64*, align 8
  %_23 = alloca %"core::fmt::builders::DebugStruct", align 8
  %_19 = alloca i64*, align 8
  %_8 = alloca %"core::fmt::builders::DebugStruct", align 8
  %0 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %self, i64 0, i32 0
  %_5 = load i64, i64* %0, align 8, !range !252
  %switch.not = icmp eq i64 %_5, 1
  %1 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %self, i64 0, i32 1
  br i1 %switch.not, label %bb1, label %bb3

bb3:                                              ; preds = %start
  %2 = bitcast %"core::fmt::builders::DebugStruct"* %_8 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %2)
; call core::fmt::Formatter::debug_struct
  %3 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1238 to [0 x i8]*), i64 5)
  %.0..sroa_cast8 = bitcast %"core::fmt::builders::DebugStruct"* %_8 to i128*
  store i128 %3, i128* %.0..sroa_cast8, align 8
  %4 = bitcast i64** %_19 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4)
  store i64* %1, i64** %_19, align 8
  %_16.0 = bitcast i64** %_19 to {}*
; call core::fmt::builders::DebugStruct::field
  %_12 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1242 to [0 x i8]*), i64 6, {}* nonnull align 1 %_16.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.7 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4)
; call core::fmt::builders::DebugStruct::finish
  %5 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %2)
  br label %bb10

bb1:                                              ; preds = %start
  %6 = bitcast %"core::fmt::builders::DebugStruct"* %_23 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %6)
; call core::fmt::Formatter::debug_struct
  %7 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1236 to [0 x i8]*), i64 5)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_23 to i128*
  store i128 %7, i128* %.0..sroa_cast, align 8
  %8 = bitcast i64** %_34 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %8)
  store i64* %1, i64** %_34, align 8
  %_31.0 = bitcast i64** %_34 to {}*
; call core::fmt::builders::DebugStruct::field
  %_27 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_23, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1237 to [0 x i8]*), i64 5, {}* nonnull align 1 %_31.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.7 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %8)
; call core::fmt::builders::DebugStruct::finish
  %9 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_23)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %6)
  br label %bb10

bb10:                                             ; preds = %bb3, %bb1
  %.0.in = phi i1 [ %5, %bb3 ], [ %9, %bb1 ]
  ret i1 %.0.in
}

; <memchr::memmem::twoway::Suffix as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN67_$LT$memchr..memmem..twoway..Suffix$u20$as$u20$core..fmt..Debug$GT$3fmt17he955caec0cfd1dceE"({ i64, i64 }* noalias readonly align 8 dereferenceable(16) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25 = alloca i64*, align 8
  %_17 = alloca i64*, align 8
  %_6 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %self, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1240 to [0 x i8]*), i64 6)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast i64** %_17 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store i64* %__self_0_0, i64** %_17, align 8
  %_14.0 = bitcast i64** %_17 to {}*
; call core::fmt::builders::DebugStruct::field
  %_10 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [3 x i8] }>* @alloc1277 to [0 x i8]*), i64 3, {}* nonnull align 1 %_14.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.7 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast i64** %_25 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store i64* %__self_0_1, i64** %_25, align 8
  %_22.0 = bitcast i64** %_25 to {}*
; call core::fmt::builders::DebugStruct::field
  %_18 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1242 to [0 x i8]*), i64 6, {}* nonnull align 1 %_22.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.7 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %4
}

; <memchr::memmem::twoway::SuffixKind as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN71_$LT$memchr..memmem..twoway..SuffixKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h6d35596b5c658690E"(i8* noalias nocapture readonly align 1 dereferenceable(1) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %0 = load i8, i8* %self, align 1, !range !654
  %switch.not = icmp eq i8 %0, 1
  %. = select i1 %switch.not, [0 x i8]* bitcast (<{ [7 x i8] }>* @alloc1243 to [0 x i8]*), [0 x i8]* bitcast (<{ [7 x i8] }>* @alloc1244 to [0 x i8]*)
; call core::fmt::Formatter::write_str
  %1 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 %., i64 7)
  ret i1 %1
}

; <memchr::memmem::twoway::SuffixOrdering as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN75_$LT$memchr..memmem..twoway..SuffixOrdering$u20$as$u20$core..fmt..Debug$GT$3fmt17h1935b95e3a5841ffE"(i8* noalias nocapture readonly align 1 dereferenceable(1) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %0 = load i8, i8* %self, align 1, !range !278
  %_5 = zext i8 %0 to i64
  switch i64 %_5, label %bb2 [
    i64 0, label %bb3
    i64 1, label %bb5
    i64 2, label %bb1
  ]

bb2:                                              ; preds = %start
  unreachable

bb3:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %1 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1247 to [0 x i8]*), i64 6)
  br label %bb8

bb5:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %2 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1246 to [0 x i8]*), i64 4)
  br label %bb8

bb1:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %3 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1245 to [0 x i8]*), i64 4)
  br label %bb8

bb8:                                              ; preds = %bb3, %bb5, %bb1
  %.0.in = phi i1 [ %3, %bb1 ], [ %2, %bb5 ], [ %1, %bb3 ]
  ret i1 %.0.in
}

; <memchr::memmem::twoway::ApproximateByteSet as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN79_$LT$memchr..memmem..twoway..ApproximateByteSet$u20$as$u20$core..fmt..Debug$GT$3fmt17hc87ca094ca876d46E"(i64* noalias readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14 = alloca i64*, align 8
  %_5 = alloca %"core::fmt::builders::DebugTuple", align 8
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [18 x i8] }>* @alloc1248 to [0 x i8]*), i64 18)
  %1 = bitcast i64** %_14 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1)
  store i64* %self, i64** %_14, align 8
  %_11.0 = bitcast i64** %_14 to {}*
; call core::fmt::builders::DebugTuple::field
  %_9 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5, {}* nonnull align 1 %_11.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.9 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1)
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret i1 %2
}

; <memchr::memmem::x86::avx::nostd::Forward as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN77_$LT$memchr..memmem..x86..avx..nostd..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17hd9b746a528f9898fE"(%"memmem::x86::avx::nostd::Forward"* noalias nonnull readonly align 1 %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14 = alloca {}*, align 8
  %_5 = alloca %"core::fmt::builders::DebugTuple", align 8
  %__self_0_0 = getelementptr %"memmem::x86::avx::nostd::Forward", %"memmem::x86::avx::nostd::Forward"* %self, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1256 to [0 x i8]*), i64 7)
  %1 = bitcast {}** %_14 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1)
  store {}* %__self_0_0, {}** %_14, align 8
  %_11.0 = bitcast {}** %_14 to {}*
; call core::fmt::builders::DebugTuple::field
  %_9 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5, {}* nonnull align 1 %_11.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.a to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1)
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret i1 %2
}

; <memchr::memmem::x86::sse::Forward as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN70_$LT$memchr..memmem..x86..sse..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17ha16b0a29596cc67cE"({ i8, i8 }* noalias readonly align 1 dereferenceable(2) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14 = alloca { i8, i8 }*, align 8
  %_5 = alloca %"core::fmt::builders::DebugTuple", align 8
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1256 to [0 x i8]*), i64 7)
  %1 = bitcast { i8, i8 }** %_14 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1)
  store { i8, i8 }* %self, { i8, i8 }** %_14, align 8
  %_11.0 = bitcast { i8, i8 }** %_14 to {}*
; call core::fmt::builders::DebugTuple::field
  %_9 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5, {}* nonnull align 1 %_11.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.b to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1)
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0)
  ret i1 %2
}

; <memchr::memmem::FindIter as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN61_$LT$memchr..memmem..FindIter$u20$as$u20$core..fmt..Debug$GT$3fmt17h089cad05599c5516E"(%"memmem::FindIter"* noalias readonly align 8 dereferenceable(112) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_43 = alloca i64*, align 8
  %_35 = alloca %"memmem::Finder"*, align 8
  %_27 = alloca { i32, i32 }*, align 8
  %_19 = alloca { [0 x i8]*, i64 }*, align 8
  %_8 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 3
  %__self_0_2 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 1
  %__self_0_3 = getelementptr inbounds %"memmem::FindIter", %"memmem::FindIter"* %self, i64 0, i32 2
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_8 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1260 to [0 x i8]*), i64 8)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_8 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast { [0 x i8]*, i64 }** %_19 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store { [0 x i8]*, i64 }* %__self_0_0, { [0 x i8]*, i64 }** %_19, align 8
  %_16.0 = bitcast { [0 x i8]*, i64 }** %_19 to {}*
; call core::fmt::builders::DebugStruct::field
  %_12 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1272 to [0 x i8]*), i64 8, {}* nonnull align 1 %_16.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.1 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast { i32, i32 }** %_27 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store { i32, i32 }* %__self_0_1, { i32, i32 }** %_27, align 8
  %_24.0 = bitcast { i32, i32 }** %_27 to {}*
; call core::fmt::builders::DebugStruct::field
  %_20 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1262 to [0 x i8]*), i64 8, {}* nonnull align 1 %_24.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.c to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
  %4 = bitcast %"memmem::Finder"** %_35 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4)
  store %"memmem::Finder"* %__self_0_2, %"memmem::Finder"** %_35, align 8
  %_32.0 = bitcast %"memmem::Finder"** %_35 to {}*
; call core::fmt::builders::DebugStruct::field
  %_28 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1273 to [0 x i8]*), i64 6, {}* nonnull align 1 %_32.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.d to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4)
  %5 = bitcast i64** %_43 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %5)
  store i64* %__self_0_3, i64** %_43, align 8
  %_40.0 = bitcast i64** %_43 to {}*
; call core::fmt::builders::DebugStruct::field
  %_36 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [3 x i8] }>* @alloc1277 to [0 x i8]*), i64 3, {}* nonnull align 1 %_40.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.7 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %5)
; call core::fmt::builders::DebugStruct::finish
  %6 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %6
}

; <memchr::memmem::FindRevIter as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN64_$LT$memchr..memmem..FindRevIter$u20$as$u20$core..fmt..Debug$GT$3fmt17h59cedde8b2d45f64E"(%"memmem::FindRevIter"* noalias readonly align 8 dereferenceable(96) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_34 = alloca { i64, i64 }*, align 8
  %_26 = alloca %"memmem::FinderRev"*, align 8
  %_18 = alloca { [0 x i8]*, i64 }*, align 8
  %_7 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 1
  %__self_0_2 = getelementptr inbounds %"memmem::FindRevIter", %"memmem::FindRevIter"* %self, i64 0, i32 2
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_7 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [11 x i8] }>* @alloc1271 to [0 x i8]*), i64 11)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_7 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast { [0 x i8]*, i64 }** %_18 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store { [0 x i8]*, i64 }* %__self_0_0, { [0 x i8]*, i64 }** %_18, align 8
  %_15.0 = bitcast { [0 x i8]*, i64 }** %_18 to {}*
; call core::fmt::builders::DebugStruct::field
  %_11 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1272 to [0 x i8]*), i64 8, {}* nonnull align 1 %_15.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.1 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast %"memmem::FinderRev"** %_26 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store %"memmem::FinderRev"* %__self_0_1, %"memmem::FinderRev"** %_26, align 8
  %_23.0 = bitcast %"memmem::FinderRev"** %_26 to {}*
; call core::fmt::builders::DebugStruct::field
  %_19 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1273 to [0 x i8]*), i64 6, {}* nonnull align 1 %_23.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.e to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
  %4 = bitcast { i64, i64 }** %_34 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4)
  store { i64, i64 }* %__self_0_2, { i64, i64 }** %_34, align 8
  %_31.0 = bitcast { i64, i64 }** %_34 to {}*
; call core::fmt::builders::DebugStruct::field
  %_27 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [3 x i8] }>* @alloc1277 to [0 x i8]*), i64 3, {}* nonnull align 1 %_31.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.f to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4)
; call core::fmt::builders::DebugStruct::finish
  %5 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %5
}

; <memchr::memmem::Finder as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN59_$LT$memchr..memmem..Finder$u20$as$u20$core..fmt..Debug$GT$3fmt17h5a00105a2ee5d6bcE"(%"memmem::Finder"* noalias readonly align 8 dereferenceable(80) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_16 = alloca %"memmem::Searcher"*, align 8
  %_5 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %self, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1281 to [0 x i8]*), i64 6)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_5 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast %"memmem::Searcher"** %_16 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store %"memmem::Searcher"* %__self_0_0, %"memmem::Searcher"** %_16, align 8
  %_13.0 = bitcast %"memmem::Searcher"** %_16 to {}*
; call core::fmt::builders::DebugStruct::field
  %_9 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1287 to [0 x i8]*), i64 8, {}* nonnull align 1 %_13.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.g to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
; call core::fmt::builders::DebugStruct::finish
  %3 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %3
}

; <memchr::memmem::FinderRev as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN62_$LT$memchr..memmem..FinderRev$u20$as$u20$core..fmt..Debug$GT$3fmt17h18ca465f2bbe8b28E"(%"memmem::FinderRev"* noalias readonly align 8 dereferenceable(64) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_16 = alloca %"memmem::SearcherRev"*, align 8
  %_5 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds %"memmem::FinderRev", %"memmem::FinderRev"* %self, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [9 x i8] }>* @alloc1286 to [0 x i8]*), i64 9)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_5 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast %"memmem::SearcherRev"** %_16 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store %"memmem::SearcherRev"* %__self_0_0, %"memmem::SearcherRev"** %_16, align 8
  %_13.0 = bitcast %"memmem::SearcherRev"** %_16 to {}*
; call core::fmt::builders::DebugStruct::field
  %_9 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1287 to [0 x i8]*), i64 8, {}* nonnull align 1 %_13.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.h to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
; call core::fmt::builders::DebugStruct::finish
  %3 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %3
}

; <memchr::memmem::FinderBuilder as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN66_$LT$memchr..memmem..FinderBuilder$u20$as$u20$core..fmt..Debug$GT$3fmt17h11eab2ad94b61099E"(i8* noalias readonly align 1 dereferenceable(1) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_16 = alloca i8*, align 8
  %_5 = alloca %"core::fmt::builders::DebugStruct", align 8
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [13 x i8] }>* @alloc1291 to [0 x i8]*), i64 13)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_5 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast i8** %_16 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store i8* %self, i8** %_16, align 8
  %_13.0 = bitcast i8** %_16 to {}*
; call core::fmt::builders::DebugStruct::field
  %_9 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1292 to [0 x i8]*), i64 6, {}* nonnull align 1 %_13.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.i to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
; call core::fmt::builders::DebugStruct::finish
  %3 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %3
}

; <memchr::memmem::Searcher as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN61_$LT$memchr..memmem..Searcher$u20$as$u20$core..fmt..Debug$GT$3fmt17h77d29106b5bc0a9dE"(%"memmem::Searcher"* noalias readonly align 8 dereferenceable(80) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_43 = alloca %"memmem::SearcherKind"*, align 8
  %_35 = alloca i64**, align 8
  %_27 = alloca %"memmem::NeedleInfo"*, align 8
  %_19 = alloca { i8*, i64 }*, align 8
  %_8 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 3
  %__self_0_2 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 1
  %__self_0_3 = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %self, i64 0, i32 2
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_8 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1296 to [0 x i8]*), i64 8)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_8 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast { i8*, i64 }** %_19 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store { i8*, i64 }* %__self_0_0, { i8*, i64 }** %_19, align 8
  %_16.0 = bitcast { i8*, i64 }** %_19 to {}*
; call core::fmt::builders::DebugStruct::field
  %_12 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1342 to [0 x i8]*), i64 6, {}* nonnull align 1 %_16.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.j to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast %"memmem::NeedleInfo"** %_27 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store %"memmem::NeedleInfo"* %__self_0_1, %"memmem::NeedleInfo"** %_27, align 8
  %_24.0 = bitcast %"memmem::NeedleInfo"** %_27 to {}*
; call core::fmt::builders::DebugStruct::field
  %_20 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1301 to [0 x i8]*), i64 5, {}* nonnull align 1 %_24.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.k to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
  %4 = bitcast i64*** %_35 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4)
  store i64** %__self_0_2, i64*** %_35, align 8
  %_32.0 = bitcast i64*** %_35 to {}*
; call core::fmt::builders::DebugStruct::field
  %_28 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1305 to [0 x i8]*), i64 5, {}* nonnull align 1 %_32.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.l to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4)
  %5 = bitcast %"memmem::SearcherKind"** %_43 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %5)
  store %"memmem::SearcherKind"* %__self_0_3, %"memmem::SearcherKind"** %_43, align 8
  %_40.0 = bitcast %"memmem::SearcherKind"** %_43 to {}*
; call core::fmt::builders::DebugStruct::field
  %_36 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1344 to [0 x i8]*), i64 4, {}* nonnull align 1 %_40.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.m to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %5)
; call core::fmt::builders::DebugStruct::finish
  %6 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %6
}

; <memchr::memmem::NeedleInfo as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN63_$LT$memchr..memmem..NeedleInfo$u20$as$u20$core..fmt..Debug$GT$3fmt17hf8ecbadd15609784E"(%"memmem::NeedleInfo"* noalias readonly align 4 dereferenceable(12) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25 = alloca { i32, i32 }*, align 8
  %_17 = alloca { i8, i8 }*, align 8
  %_6 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds %"memmem::NeedleInfo", %"memmem::NeedleInfo"* %self, i64 0, i32 1
  %__self_0_1 = getelementptr inbounds %"memmem::NeedleInfo", %"memmem::NeedleInfo"* %self, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [10 x i8] }>* @alloc1313 to [0 x i8]*), i64 10)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_6 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast { i8, i8 }** %_17 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store { i8, i8 }* %__self_0_0, { i8, i8 }** %_17, align 8
  %_14.0 = bitcast { i8, i8 }** %_17 to {}*
; call core::fmt::builders::DebugStruct::field
  %_10 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [9 x i8] }>* @alloc1314 to [0 x i8]*), i64 9, {}* nonnull align 1 %_14.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.n to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast { i32, i32 }** %_25 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store { i32, i32 }* %__self_0_1, { i32, i32 }** %_25, align 8
  %_22.0 = bitcast { i32, i32 }** %_25 to {}*
; call core::fmt::builders::DebugStruct::field
  %_18 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1343 to [0 x i8]*), i64 5, {}* nonnull align 1 %_22.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.o to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %4
}

; <memchr::memmem::SearcherConfig as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN67_$LT$memchr..memmem..SearcherConfig$u20$as$u20$core..fmt..Debug$GT$3fmt17h95222fe3e975a71aE"(i8* noalias readonly align 1 dereferenceable(1) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_16 = alloca i8*, align 8
  %_5 = alloca %"core::fmt::builders::DebugStruct", align 8
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_5 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [14 x i8] }>* @alloc1322 to [0 x i8]*), i64 14)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_5 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast i8** %_16 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store i8* %self, i8** %_16, align 8
  %_13.0 = bitcast i8** %_16 to {}*
; call core::fmt::builders::DebugStruct::field
  %_9 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [9 x i8] }>* @alloc1323 to [0 x i8]*), i64 9, {}* nonnull align 1 %_13.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.p to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
; call core::fmt::builders::DebugStruct::finish
  %3 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %3
}

; <memchr::memmem::SearcherKind as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN65_$LT$memchr..memmem..SearcherKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h83b8d41a46a884ccE"(%"memmem::SearcherKind"* noalias readonly align 8 dereferenceable(40) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_59 = alloca %"memmem::x86::avx::nostd::Forward"*, align 8
  %_50 = alloca %"core::fmt::builders::DebugTuple", align 8
  %_46 = alloca { i8, i8 }*, align 8
  %_37 = alloca %"core::fmt::builders::DebugTuple", align 8
  %_33 = alloca %"memmem::twoway::Forward"*, align 8
  %_24 = alloca %"core::fmt::builders::DebugTuple", align 8
  %_20 = alloca i8*, align 8
  %_11 = alloca %"core::fmt::builders::DebugTuple", align 8
  %0 = getelementptr inbounds %"memmem::SearcherKind", %"memmem::SearcherKind"* %self, i64 0, i32 0
  %1 = load i8, i8* %0, align 8, !range !157
  %_5 = zext i8 %1 to i64
  switch i64 %_5, label %bb2 [
    i64 0, label %bb3
    i64 1, label %bb5
    i64 2, label %bb9
    i64 3, label %bb13
    i64 4, label %bb1
  ]

bb2:                                              ; preds = %start
  unreachable

bb3:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %2 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1353 to [0 x i8]*), i64 5)
  br label %bb20

bb5:                                              ; preds = %start
  %3 = getelementptr inbounds %"memmem::SearcherKind", %"memmem::SearcherKind"* %self, i64 0, i32 1, i64 0
  %4 = bitcast %"core::fmt::builders::DebugTuple"* %_11 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %4)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_11, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1352 to [0 x i8]*), i64 7)
  %5 = bitcast i8** %_20 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %5)
  store i8* %3, i8** %_20, align 8
  %_17.0 = bitcast i8** %_20 to {}*
; call core::fmt::builders::DebugTuple::field
  %_15 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_11, {}* nonnull align 1 %_17.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %5)
; call core::fmt::builders::DebugTuple::finish
  %6 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_11)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %4)
  br label %bb20

bb9:                                              ; preds = %start
  %__self_02 = getelementptr inbounds %"memmem::SearcherKind", %"memmem::SearcherKind"* %self, i64 0, i32 1, i64 7
  %7 = bitcast %"core::fmt::builders::DebugTuple"* %_24 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %7)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_24, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1348 to [0 x i8]*), i64 6)
  %8 = bitcast %"memmem::twoway::Forward"** %_33 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %8)
  %9 = bitcast %"memmem::twoway::Forward"** %_33 to i8**
  store i8* %__self_02, i8** %9, align 8
  %_30.0 = bitcast %"memmem::twoway::Forward"** %_33 to {}*
; call core::fmt::builders::DebugTuple::field
  %_28 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_24, {}* nonnull align 1 %_30.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.s to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %8)
; call core::fmt::builders::DebugTuple::finish
  %10 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_24)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %7)
  br label %bb20

bb13:                                             ; preds = %start
  %__self_01 = getelementptr inbounds %"memmem::SearcherKind", %"memmem::SearcherKind"* %self, i64 0, i32 1
  %11 = bitcast %"core::fmt::builders::DebugTuple"* %_37 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %11)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_37, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [14 x i8] }>* @alloc1331 to [0 x i8]*), i64 14)
  %12 = bitcast { i8, i8 }** %_46 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %12)
  %13 = bitcast { i8, i8 }** %_46 to [39 x i8]**
  store [39 x i8]* %__self_01, [39 x i8]** %13, align 8
  %_43.0 = bitcast { i8, i8 }** %_46 to {}*
; call core::fmt::builders::DebugTuple::field
  %_41 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_37, {}* nonnull align 1 %_43.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.r to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %12)
; call core::fmt::builders::DebugTuple::finish
  %14 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_37)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %11)
  br label %bb20

bb1:                                              ; preds = %start
  %__self_0 = getelementptr inbounds %"memmem::SearcherKind", %"memmem::SearcherKind"* %self, i64 0, i32 1
  %15 = bitcast %"core::fmt::builders::DebugTuple"* %_50 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %15)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_50, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [14 x i8] }>* @alloc1327 to [0 x i8]*), i64 14)
  %16 = bitcast %"memmem::x86::avx::nostd::Forward"** %_59 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %16)
  %17 = bitcast %"memmem::x86::avx::nostd::Forward"** %_59 to [39 x i8]**
  store [39 x i8]* %__self_0, [39 x i8]** %17, align 8
  %_56.0 = bitcast %"memmem::x86::avx::nostd::Forward"** %_59 to {}*
; call core::fmt::builders::DebugTuple::field
  %_54 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_50, {}* nonnull align 1 %_56.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.q to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %16)
; call core::fmt::builders::DebugTuple::finish
  %18 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_50)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %15)
  br label %bb20

bb20:                                             ; preds = %bb3, %bb5, %bb9, %bb13, %bb1
  %.0.in = phi i1 [ %18, %bb1 ], [ %14, %bb13 ], [ %10, %bb9 ], [ %6, %bb5 ], [ %2, %bb3 ]
  ret i1 %.0.in
}

; <memchr::memmem::SearcherRev as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN64_$LT$memchr..memmem..SearcherRev$u20$as$u20$core..fmt..Debug$GT$3fmt17h9c6507df7bf765f6E"(%"memmem::SearcherRev"* noalias readonly align 8 dereferenceable(64) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_34 = alloca %"memmem::SearcherRevKind"*, align 8
  %_26 = alloca { i32, i32 }*, align 8
  %_18 = alloca { i8*, i64 }*, align 8
  %_7 = alloca %"core::fmt::builders::DebugStruct", align 8
  %__self_0_0 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %self, i64 0, i32 0
  %__self_0_1 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %self, i64 0, i32 2
  %__self_0_2 = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %self, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_7 to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0)
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [11 x i8] }>* @alloc1341 to [0 x i8]*), i64 11)
  %.0..sroa_cast = bitcast %"core::fmt::builders::DebugStruct"* %_7 to i128*
  store i128 %1, i128* %.0..sroa_cast, align 8
  %2 = bitcast { i8*, i64 }** %_18 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2)
  store { i8*, i64 }* %__self_0_0, { i8*, i64 }** %_18, align 8
  %_15.0 = bitcast { i8*, i64 }** %_18 to {}*
; call core::fmt::builders::DebugStruct::field
  %_11 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1342 to [0 x i8]*), i64 6, {}* nonnull align 1 %_15.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.j to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2)
  %3 = bitcast { i32, i32 }** %_26 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3)
  store { i32, i32 }* %__self_0_1, { i32, i32 }** %_26, align 8
  %_23.0 = bitcast { i32, i32 }** %_26 to {}*
; call core::fmt::builders::DebugStruct::field
  %_19 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1343 to [0 x i8]*), i64 5, {}* nonnull align 1 %_23.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.o to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3)
  %4 = bitcast %"memmem::SearcherRevKind"** %_34 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4)
  store %"memmem::SearcherRevKind"* %__self_0_2, %"memmem::SearcherRevKind"** %_34, align 8
  %_31.0 = bitcast %"memmem::SearcherRevKind"** %_34 to {}*
; call core::fmt::builders::DebugStruct::field
  %_27 = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1344 to [0 x i8]*), i64 4, {}* nonnull align 1 %_31.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.t to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4)
; call core::fmt::builders::DebugStruct::finish
  %5 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0)
  ret i1 %5
}

; <memchr::memmem::SearcherRevKind as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define zeroext i1 @"_ZN68_$LT$memchr..memmem..SearcherRevKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h5bcf687e301f39f3E"(%"memmem::SearcherRevKind"* noalias readonly align 8 dereferenceable(40) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_33 = alloca %"memmem::twoway::Reverse"*, align 8
  %_24 = alloca %"core::fmt::builders::DebugTuple", align 8
  %_20 = alloca i8*, align 8
  %_11 = alloca %"core::fmt::builders::DebugTuple", align 8
  %0 = getelementptr inbounds %"memmem::SearcherRevKind", %"memmem::SearcherRevKind"* %self, i64 0, i32 0
  %1 = load i8, i8* %0, align 8, !range !278
  %_5 = zext i8 %1 to i64
  switch i64 %_5, label %bb2 [
    i64 0, label %bb3
    i64 1, label %bb5
    i64 2, label %bb1
  ]

bb2:                                              ; preds = %start
  unreachable

bb3:                                              ; preds = %start
; call core::fmt::Formatter::write_str
  %2 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1353 to [0 x i8]*), i64 5)
  br label %bb12

bb5:                                              ; preds = %start
  %3 = getelementptr inbounds %"memmem::SearcherRevKind", %"memmem::SearcherRevKind"* %self, i64 0, i32 1, i64 0
  %4 = bitcast %"core::fmt::builders::DebugTuple"* %_11 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %4)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_11, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1352 to [0 x i8]*), i64 7)
  %5 = bitcast i8** %_20 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %5)
  store i8* %3, i8** %_20, align 8
  %_17.0 = bitcast i8** %_20 to {}*
; call core::fmt::builders::DebugTuple::field
  %_15 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_11, {}* nonnull align 1 %_17.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %5)
; call core::fmt::builders::DebugTuple::finish
  %6 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_11)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %4)
  br label %bb12

bb1:                                              ; preds = %start
  %__self_0 = getelementptr inbounds %"memmem::SearcherRevKind", %"memmem::SearcherRevKind"* %self, i64 0, i32 1, i64 7
  %7 = bitcast %"core::fmt::builders::DebugTuple"* %_24 to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %7)
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_24, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1348 to [0 x i8]*), i64 6)
  %8 = bitcast %"memmem::twoway::Reverse"** %_33 to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %8)
  %9 = bitcast %"memmem::twoway::Reverse"** %_33 to i8**
  store i8* %__self_0, i8** %9, align 8
  %_30.0 = bitcast %"memmem::twoway::Reverse"** %_33 to {}*
; call core::fmt::builders::DebugTuple::field
  %_28 = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_24, {}* nonnull align 1 %_30.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.u to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %8)
; call core::fmt::builders::DebugTuple::finish
  %10 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_24)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %7)
  br label %bb12

bb12:                                             ; preds = %bb3, %bb5, %bb1
  %.0.in = phi i1 [ %10, %bb1 ], [ %6, %bb5 ], [ %2, %bb3 ]
  ret i1 %.0.in
}

; core::panicking::assert_failed
; Function Attrs: cold noreturn nonlazybind uwtable
define internal fastcc void @_ZN4core9panicking13assert_failed17h2b1ecd6dd4ba2033E(i8* noalias readonly align 1 dereferenceable(1) %0, i8* noalias readonly align 1 dereferenceable(1) %1, %"core::option::Option<core::fmt::Arguments>"* noalias nocapture readonly dereferenceable(48) %args) unnamed_addr #12 {
start:
  %_12 = alloca %"core::option::Option<core::fmt::Arguments>", align 8
  %right = alloca i8*, align 8
  %left = alloca i8*, align 8
  store i8* %0, i8** %left, align 8
  store i8* %1, i8** %right, align 8
  %_6.0 = bitcast i8** %left to {}*
  %_9.0 = bitcast i8** %right to {}*
  %2 = bitcast %"core::option::Option<core::fmt::Arguments>"* %_12 to i8*
  call void @llvm.lifetime.start.p0i8(i64 48, i8* nonnull %2)
  %3 = bitcast %"core::option::Option<core::fmt::Arguments>"* %args to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(48) %2, i8* noundef nonnull align 8 dereferenceable(48) %3, i64 48, i1 false)
; call core::panicking::assert_failed_inner
  call void @_ZN4core9panicking19assert_failed_inner17hd256bbdf19083f81E(i8 1, {}* nonnull align 1 %_6.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*), {}* nonnull align 1 %_9.0, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*), %"core::option::Option<core::fmt::Arguments>"* noalias nocapture nonnull dereferenceable(48) %_12, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8] }>* @alloc1089 to %"core::panic::location::Location"*)) #22
  unreachable
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h012f9a793918b767E"({}** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
; call core::fmt::Formatter::pad
  %0 = tail call zeroext i1 @_ZN4core3fmt9Formatter3pad17h40fda45ac7db157fE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [2 x i8] }>* @alloc1358 to [0 x i8]*), i64 2)
  ret i1 %0
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h02afc80f286cfd18E"({ i64, i64 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_20.i = alloca i64*, align 8
  %_11.i = alloca %"core::fmt::builders::DebugTuple", align 8
  %_4 = load { i64, i64 }*, { i64, i64 }** %self, align 8, !nonnull !142
  tail call void @llvm.experimental.noalias.scope.decl(metadata !655)
  %0 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %_4, i64 0, i32 0
  %_5.i = load i64, i64* %0, align 8, !range !252, !alias.scope !655, !noalias !658
  %switch.not.i = icmp eq i64 %_5.i, 1
  br i1 %switch.not.i, label %bb1.i, label %bb3.i

bb3.i:                                            ; preds = %start
; call core::fmt::Formatter::write_str
  %1 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1368 to [0 x i8]*), i64 4), !noalias !655
  br label %"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17h5d46b5326924fe2fE.exit"

bb1.i:                                            ; preds = %start
  %2 = getelementptr inbounds { i64, i64 }, { i64, i64 }* %_4, i64 0, i32 1
  %3 = bitcast %"core::fmt::builders::DebugTuple"* %_11.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %3), !noalias !660
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_11.i, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1364 to [0 x i8]*), i64 4), !noalias !655
  %4 = bitcast i64** %_20.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4), !noalias !660
  store i64* %2, i64** %_20.i, align 8, !noalias !660
  %_17.0.i = bitcast i64** %_20.i to {}*
; call core::fmt::builders::DebugTuple::field
  %_15.i = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_11.i, {}* nonnull align 1 %_17.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.7 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4), !noalias !660
; call core::fmt::builders::DebugTuple::finish
  %5 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_11.i)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %3), !noalias !660
  br label %"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17h5d46b5326924fe2fE.exit"

"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17h5d46b5326924fe2fE.exit": ; preds = %bb3.i, %bb1.i
  %.0.in.i = phi i1 [ %1, %bb3.i ], [ %5, %bb1.i ]
  ret i1 %.0.in.i
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h15d12a7ec8ebf07bE"(i32** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_4 = load i32*, i32** %self, align 8, !nonnull !142
; call core::fmt::Formatter::debug_lower_hex
  %_3.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !661
  br i1 %_3.i, label %bb2.i, label %bb4.i

bb4.i:                                            ; preds = %start
; call core::fmt::Formatter::debug_upper_hex
  %_7.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !661
  br i1 %_7.i, label %bb6.i, label %bb8.i

bb2.i:                                            ; preds = %start
; call core::fmt::num::<impl core::fmt::LowerHex for u32>::fmt
  %0 = tail call zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$u32$GT$3fmt17hecbcc5a775cb29a6E"(i32* noalias nonnull readonly align 4 dereferenceable(4) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u32$GT$3fmt17ha7f34dbae66c4ee1E.exit"

bb8.i:                                            ; preds = %bb4.i
; call core::fmt::num::imp::<impl core::fmt::Display for u32>::fmt
  %1 = tail call zeroext i1 @"_ZN4core3fmt3num3imp52_$LT$impl$u20$core..fmt..Display$u20$for$u20$u32$GT$3fmt17h80e28ec8edb27b1fE"(i32* noalias nonnull readonly align 4 dereferenceable(4) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u32$GT$3fmt17ha7f34dbae66c4ee1E.exit"

bb6.i:                                            ; preds = %bb4.i
; call core::fmt::num::<impl core::fmt::UpperHex for u32>::fmt
  %2 = tail call zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$u32$GT$3fmt17h846f73226b9e806fE"(i32* noalias nonnull readonly align 4 dereferenceable(4) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u32$GT$3fmt17ha7f34dbae66c4ee1E.exit"

"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u32$GT$3fmt17ha7f34dbae66c4ee1E.exit": ; preds = %bb2.i, %bb8.i, %bb6.i
  %.0.in.i = phi i1 [ %0, %bb2.i ], [ %2, %bb6.i ], [ %1, %bb8.i ]
  ret i1 %.0.in.i
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h1b91a732156e1f6cE"(i32** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14.i = alloca i32*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugTuple", align 8
  %_4 = load i32*, i32** %self, align 8, !nonnull !142
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0), !noalias !664
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5.i, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1214 to [0 x i8]*), i64 4), !noalias !668
  %1 = bitcast i32** %_14.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1), !noalias !664
  store i32* %_4, i32** %_14.i, align 8, !noalias !664
  %_11.0.i = bitcast i32** %_14.i to {}*
; call core::fmt::builders::DebugTuple::field
  %_9.i = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i, {}* nonnull align 1 %_11.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1), !noalias !664
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0), !noalias !664
  ret i1 %2
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h1f25bed89d6307b2E"({ i8, i8 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14.i = alloca { i8, i8 }*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugTuple", align 8
  %_4 = load { i8, i8 }*, { i8, i8 }** %self, align 8, !nonnull !142
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0), !noalias !669
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5.i, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1256 to [0 x i8]*), i64 7), !noalias !673
  %1 = bitcast { i8, i8 }** %_14.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1), !noalias !669
  store { i8, i8 }* %_4, { i8, i8 }** %_14.i, align 8, !noalias !669
  %_11.0.i = bitcast { i8, i8 }** %_14.i to {}*
; call core::fmt::builders::DebugTuple::field
  %_9.i = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i, {}* nonnull align 1 %_11.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.b to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1), !noalias !669
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0), !noalias !669
  ret i1 %2
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h203c01692e5cf32cE"({ i8, i8 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25.i = alloca i8*, align 8
  %_17.i = alloca i8*, align 8
  %_6.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load { i8, i8 }*, { i8, i8 }** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds { i8, i8 }, { i8, i8 }* %_4, i64 0, i32 0
  %__self_0_1.i = getelementptr inbounds { i8, i8 }, { i8, i8 }* %_4, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !674
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1256 to [0 x i8]*), i64 7), !noalias !678
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_6.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !674
  %2 = bitcast i8** %_17.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !674
  store i8* %__self_0_0.i, i8** %_17.i, align 8, !noalias !674
  %_14.0.i = bitcast i8** %_17.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_10.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1216 to [0 x i8]*), i64 6, {}* nonnull align 1 %_14.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !674
  %3 = bitcast i8** %_25.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3), !noalias !674
  store i8* %__self_0_1.i, i8** %_25.i, align 8, !noalias !674
  %_22.0.i = bitcast i8** %_25.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_18.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1217 to [0 x i8]*), i64 6, {}* nonnull align 1 %_22.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3), !noalias !674
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !674
  ret i1 %4
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h24add91eb36d19a2E"(i64*** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
; call <str as core::fmt::Debug>::fmt
  %0 = tail call zeroext i1 @"_ZN40_$LT$str$u20$as$u20$core..fmt..Debug$GT$3fmt17h2fc61a29b5ba73d9E"([0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [19 x i8] }>* @alloc1063 to [0 x i8]*), i64 19, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %0
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h289d590000ab1d43E"(%"memmem::NeedleInfo"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25.i = alloca { i32, i32 }*, align 8
  %_17.i = alloca { i8, i8 }*, align 8
  %_6.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load %"memmem::NeedleInfo"*, %"memmem::NeedleInfo"** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds %"memmem::NeedleInfo", %"memmem::NeedleInfo"* %_4, i64 0, i32 1
  %__self_0_1.i = getelementptr inbounds %"memmem::NeedleInfo", %"memmem::NeedleInfo"* %_4, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !679
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [10 x i8] }>* @alloc1313 to [0 x i8]*), i64 10), !noalias !683
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_6.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !679
  %2 = bitcast { i8, i8 }** %_17.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !679
  store { i8, i8 }* %__self_0_0.i, { i8, i8 }** %_17.i, align 8, !noalias !679
  %_14.0.i = bitcast { i8, i8 }** %_17.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_10.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [9 x i8] }>* @alloc1314 to [0 x i8]*), i64 9, {}* nonnull align 1 %_14.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.n to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !679
  %3 = bitcast { i32, i32 }** %_25.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3), !noalias !679
  store { i32, i32 }* %__self_0_1.i, { i32, i32 }** %_25.i, align 8, !noalias !679
  %_22.0.i = bitcast { i32, i32 }** %_25.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_18.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1343 to [0 x i8]*), i64 5, {}* nonnull align 1 %_22.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.o to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3), !noalias !679
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !679
  ret i1 %4
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h30d83982d8dc029dE"(%"memmem::twoway::TwoWay"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_34.i = alloca { i64, i64 }*, align 8
  %_26.i = alloca i64*, align 8
  %_18.i = alloca i64*, align 8
  %_7.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load %"memmem::twoway::TwoWay"*, %"memmem::twoway::TwoWay"** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds %"memmem::twoway::TwoWay", %"memmem::twoway::TwoWay"* %_4, i64 0, i32 0
  %__self_0_1.i = getelementptr inbounds %"memmem::twoway::TwoWay", %"memmem::twoway::TwoWay"* %_4, i64 0, i32 1
  %__self_0_2.i = getelementptr inbounds %"memmem::twoway::TwoWay", %"memmem::twoway::TwoWay"* %_4, i64 0, i32 2
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_7.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !684
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1348 to [0 x i8]*), i64 6), !noalias !688
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_7.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !684
  %2 = bitcast i64** %_18.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !684
  store i64* %__self_0_0.i, i64** %_18.i, align 8, !noalias !684
  %_15.0.i = bitcast i64** %_18.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_11.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1224 to [0 x i8]*), i64 7, {}* nonnull align 1 %_15.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.6 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !684
  %3 = bitcast i64** %_26.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3), !noalias !684
  store i64* %__self_0_1.i, i64** %_26.i, align 8, !noalias !684
  %_23.0.i = bitcast i64** %_26.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_19.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [12 x i8] }>* @alloc1228 to [0 x i8]*), i64 12, {}* nonnull align 1 %_23.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.7 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3), !noalias !684
  %4 = bitcast { i64, i64 }** %_34.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4), !noalias !684
  store { i64, i64 }* %__self_0_2.i, { i64, i64 }** %_34.i, align 8, !noalias !684
  %_31.0.i = bitcast { i64, i64 }** %_34.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_27.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1237 to [0 x i8]*), i64 5, {}* nonnull align 1 %_31.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.8 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4), !noalias !684
; call core::fmt::builders::DebugStruct::finish
  %5 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !684
  ret i1 %5
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h3658f4059176b5a0E"(%"memmem::x86::avx::nostd::Forward"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14.i = alloca {}*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugTuple", align 8
  %_4 = load %"memmem::x86::avx::nostd::Forward"*, %"memmem::x86::avx::nostd::Forward"** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr %"memmem::x86::avx::nostd::Forward", %"memmem::x86::avx::nostd::Forward"* %_4, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0), !noalias !689
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5.i, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1256 to [0 x i8]*), i64 7), !noalias !693
  %1 = bitcast {}** %_14.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1), !noalias !689
  store {}* %__self_0_0.i, {}** %_14.i, align 8, !noalias !689
  %_11.0.i = bitcast {}** %_14.i to {}*
; call core::fmt::builders::DebugTuple::field
  %_9.i = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i, {}* nonnull align 1 %_11.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.a to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1), !noalias !689
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0), !noalias !689
  ret i1 %2
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h375e3f0ea5f25018E"(i64** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_4 = load i64*, i64** %self, align 8, !nonnull !142
; call core::fmt::Formatter::debug_lower_hex
  %_3.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !694
  br i1 %_3.i, label %bb2.i, label %bb4.i

bb4.i:                                            ; preds = %start
; call core::fmt::Formatter::debug_upper_hex
  %_7.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !694
  br i1 %_7.i, label %bb6.i, label %bb8.i

bb2.i:                                            ; preds = %start
; call core::fmt::num::<impl core::fmt::LowerHex for u64>::fmt
  %0 = tail call zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$u64$GT$3fmt17ha1880b12d13db428E"(i64* noalias nonnull readonly align 8 dereferenceable(8) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u64$GT$3fmt17h772c2a78b67c4edcE.exit"

bb8.i:                                            ; preds = %bb4.i
; call core::fmt::num::imp::<impl core::fmt::Display for u64>::fmt
  %1 = tail call zeroext i1 @"_ZN4core3fmt3num3imp52_$LT$impl$u20$core..fmt..Display$u20$for$u20$u64$GT$3fmt17h2c6c89560bbd7c4fE"(i64* noalias nonnull readonly align 8 dereferenceable(8) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u64$GT$3fmt17h772c2a78b67c4edcE.exit"

bb6.i:                                            ; preds = %bb4.i
; call core::fmt::num::<impl core::fmt::UpperHex for u64>::fmt
  %2 = tail call zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$u64$GT$3fmt17h10594f3bd2afbabbE"(i64* noalias nonnull readonly align 8 dereferenceable(8) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u64$GT$3fmt17h772c2a78b67c4edcE.exit"

"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u64$GT$3fmt17h772c2a78b67c4edcE.exit": ; preds = %bb2.i, %bb8.i, %bb6.i
  %.0.in.i = phi i1 [ %0, %bb2.i ], [ %2, %bb6.i ], [ %1, %bb8.i ]
  ret i1 %.0.in.i
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h3aea02fb6dd27823E"({ [0 x i8]*, i64 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 personality i32 (...)* @rust_eh_personality {
start:
  %entry.i.i.i = alloca i8*, align 8
  %_6.i.i = alloca %"core::fmt::builders::DebugList", align 8
  %_4 = load { [0 x i8]*, i64 }*, { [0 x i8]*, i64 }** %self, align 8, !nonnull !142
  %_4.idx = getelementptr { [0 x i8]*, i64 }, { [0 x i8]*, i64 }* %_4, i64 0, i32 0
  %_4.idx.val = load [0 x i8]*, [0 x i8]** %_4.idx, align 8
  %_4.idx1 = getelementptr { [0 x i8]*, i64 }, { [0 x i8]*, i64 }* %_4, i64 0, i32 1
  %_4.idx1.val = load i64, i64* %_4.idx1, align 8
  %0 = bitcast %"core::fmt::builders::DebugList"* %_6.i.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !697
; call core::fmt::Formatter::debug_list
  %1 = tail call i128 @_ZN4core3fmt9Formatter10debug_list17h804611945d4c2177E(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f), !noalias !703
  %.0..sroa_cast.i.i = bitcast %"core::fmt::builders::DebugList"* %_6.i.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i.i, align 8, !noalias !697
  %2 = getelementptr inbounds [0 x i8], [0 x i8]* %_4.idx.val, i64 0, i64 %_4.idx1.val
  %3 = bitcast i8** %entry.i.i.i to i8*
  %_14.0.i.i.i = bitcast i8** %entry.i.i.i to {}*
  %_12.i16.i.i.i = icmp eq i64 %_4.idx1.val, 0
  br i1 %_12.i16.i.i.i, label %"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hb05d48d2b33ecd4aE.exit", label %bb4.i.preheader.i.i

bb4.i.preheader.i.i:                              ; preds = %start
  %4 = getelementptr [0 x i8], [0 x i8]* %_4.idx.val, i64 0, i64 0
  br label %bb4.i.i.i

bb4.i.i.i:                                        ; preds = %bb4.i.i.i, %bb4.i.preheader.i.i
  %iter.sroa.0.017.i.i.i = phi i8* [ %5, %bb4.i.i.i ], [ %4, %bb4.i.preheader.i.i ]
  %5 = getelementptr inbounds i8, i8* %iter.sroa.0.017.i.i.i, i64 1
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3), !noalias !704
  store i8* %iter.sroa.0.017.i.i.i, i8** %entry.i.i.i, align 8, !noalias !704
; call core::fmt::builders::DebugList::entry
  %_12.i.i.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugList"* @_ZN4core3fmt8builders9DebugList5entry17h70c8a86ab213bc16E(%"core::fmt::builders::DebugList"* noalias nonnull align 8 dereferenceable(16) %_6.i.i, {}* nonnull align 1 %_14.0.i.i.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3), !noalias !704
  %_12.i.i.i.i = icmp eq i8* %5, %2
  br i1 %_12.i.i.i.i, label %"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hb05d48d2b33ecd4aE.exit", label %bb4.i.i.i

"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hb05d48d2b33ecd4aE.exit": ; preds = %bb4.i.i.i, %start
; call core::fmt::builders::DebugList::finish
  %6 = call zeroext i1 @_ZN4core3fmt8builders9DebugList6finish17h02d7d1d08d1d92d5E(%"core::fmt::builders::DebugList"* noalias nonnull align 8 dereferenceable(16) %_6.i.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !697
  ret i1 %6
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h423c5115cbf28310E"(i8** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_4 = load i8*, i8** %self, align 8, !nonnull !142
  tail call void @llvm.experimental.noalias.scope.decl(metadata !707)
  %0 = load i8, i8* %_4, align 1, !range !654, !alias.scope !707, !noalias !710
  %switch.not.i = icmp eq i8 %0, 1
  %..i = select i1 %switch.not.i, [0 x i8]* bitcast (<{ [4 x i8] }>* @alloc1200 to [0 x i8]*), [0 x i8]* bitcast (<{ [4 x i8] }>* @alloc1368 to [0 x i8]*)
; call core::fmt::Formatter::write_str
  %1 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 %..i, i64 4), !noalias !707
  ret i1 %1
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h4956e92797be587dE"({ i32, i32 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25.i = alloca i32*, align 8
  %_17.i = alloca i32*, align 8
  %_6.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load { i32, i32 }*, { i32, i32 }** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds { i32, i32 }, { i32, i32 }* %_4, i64 0, i32 0
  %__self_0_1.i = getelementptr inbounds { i32, i32 }, { i32, i32 }* %_4, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !712
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [10 x i8] }>* @alloc1208 to [0 x i8]*), i64 10), !noalias !716
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_6.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !712
  %2 = bitcast i32** %_17.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !712
  store i32* %__self_0_0.i, i32** %_17.i, align 8, !noalias !712
  %_14.0.i = bitcast i32** %_17.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_10.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1209 to [0 x i8]*), i64 4, {}* nonnull align 1 %_14.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.4 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !712
  %3 = bitcast i32** %_25.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3), !noalias !712
  store i32* %__self_0_1.i, i32** %_25.i, align 8, !noalias !712
  %_22.0.i = bitcast i32** %_25.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_18.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [9 x i8] }>* @alloc1213 to [0 x i8]*), i64 9, {}* nonnull align 1 %_22.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3), !noalias !712
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !712
  ret i1 %4
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h4c8240682b9c408bE"(%"memmem::twoway::Reverse"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14.i = alloca %"memmem::twoway::TwoWay"*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugTuple", align 8
  %_4 = load %"memmem::twoway::Reverse"*, %"memmem::twoway::Reverse"** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds %"memmem::twoway::Reverse", %"memmem::twoway::Reverse"* %_4, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0), !noalias !717
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5.i, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1222 to [0 x i8]*), i64 7), !noalias !721
  %1 = bitcast %"memmem::twoway::TwoWay"** %_14.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1), !noalias !717
  store %"memmem::twoway::TwoWay"* %__self_0_0.i, %"memmem::twoway::TwoWay"** %_14.i, align 8, !noalias !717
  %_11.0.i = bitcast %"memmem::twoway::TwoWay"** %_14.i to {}*
; call core::fmt::builders::DebugTuple::field
  %_9.i = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i, {}* nonnull align 1 %_11.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.5 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1), !noalias !717
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0), !noalias !717
  ret i1 %2
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h4fb05e4824d7062dE"(%"memmem::twoway::Forward"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14.i = alloca %"memmem::twoway::TwoWay"*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugTuple", align 8
  %_4 = load %"memmem::twoway::Forward"*, %"memmem::twoway::Forward"** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds %"memmem::twoway::Forward", %"memmem::twoway::Forward"* %_4, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0), !noalias !722
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5.i, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1256 to [0 x i8]*), i64 7), !noalias !726
  %1 = bitcast %"memmem::twoway::TwoWay"** %_14.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1), !noalias !722
  store %"memmem::twoway::TwoWay"* %__self_0_0.i, %"memmem::twoway::TwoWay"** %_14.i, align 8, !noalias !722
  %_11.0.i = bitcast %"memmem::twoway::TwoWay"** %_14.i to {}*
; call core::fmt::builders::DebugTuple::field
  %_9.i = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i, {}* nonnull align 1 %_11.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.5 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1), !noalias !722
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0), !noalias !722
  ret i1 %2
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h6af5665499c3be04E"(%"memmem::Finder"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_16.i = alloca %"memmem::Searcher"*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load %"memmem::Finder"*, %"memmem::Finder"** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds %"memmem::Finder", %"memmem::Finder"* %_4, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !727
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1281 to [0 x i8]*), i64 6), !noalias !731
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_5.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !727
  %2 = bitcast %"memmem::Searcher"** %_16.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !727
  store %"memmem::Searcher"* %__self_0_0.i, %"memmem::Searcher"** %_16.i, align 8, !noalias !727
  %_13.0.i = bitcast %"memmem::Searcher"** %_16.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_9.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1287 to [0 x i8]*), i64 8, {}* nonnull align 1 %_13.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.g to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !727
; call core::fmt::builders::DebugStruct::finish
  %3 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !727
  ret i1 %3
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h6ec26ca7b51ec85cE"(i64*** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_20.i = alloca i64**, align 8
  %_11.i = alloca %"core::fmt::builders::DebugTuple", align 8
  %_4 = load i64**, i64*** %self, align 8, !nonnull !142
  tail call void @llvm.experimental.noalias.scope.decl(metadata !732)
  %0 = bitcast i64** %_4 to {}**
  %1 = load {}*, {}** %0, align 8, !alias.scope !732, !noalias !735
  %2 = icmp eq {}* %1, null
  br i1 %2, label %bb3.i, label %bb1.i

bb3.i:                                            ; preds = %start
; call core::fmt::Formatter::write_str
  %3 = tail call zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1368 to [0 x i8]*), i64 4), !noalias !732
  br label %"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hd3e67c4facafb664E.exit"

bb1.i:                                            ; preds = %start
  %4 = bitcast %"core::fmt::builders::DebugTuple"* %_11.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %4), !noalias !737
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_11.i, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1364 to [0 x i8]*), i64 4), !noalias !732
  %5 = bitcast i64*** %_20.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %5), !noalias !737
  store i64** %_4, i64*** %_20.i, align 8, !noalias !737
  %_17.0.i = bitcast i64*** %_20.i to {}*
; call core::fmt::builders::DebugTuple::field
  %_15.i = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_11.i, {}* nonnull align 1 %_17.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.v to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %5), !noalias !737
; call core::fmt::builders::DebugTuple::finish
  %6 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_11.i)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %4), !noalias !737
  br label %"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hd3e67c4facafb664E.exit"

"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hd3e67c4facafb664E.exit": ; preds = %bb3.i, %bb1.i
  %.0.in.i = phi i1 [ %3, %bb3.i ], [ %6, %bb1.i ]
  ret i1 %.0.in.i
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h70f1e85449412d59E"({ i32, i32 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25.i = alloca i32*, align 8
  %_17.i = alloca i32*, align 8
  %_6.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load { i32, i32 }*, { i32, i32 }** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds { i32, i32 }, { i32, i32 }* %_4, i64 0, i32 0
  %__self_0_1.i = getelementptr inbounds { i32, i32 }, { i32, i32 }* %_4, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !738
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [14 x i8] }>* @alloc1202 to [0 x i8]*), i64 14), !noalias !742
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_6.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !738
  %2 = bitcast i32** %_17.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !738
  store i32* %__self_0_0.i, i32** %_17.i, align 8, !noalias !738
  %_14.0.i = bitcast i32** %_17.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_10.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1203 to [0 x i8]*), i64 5, {}* nonnull align 1 %_14.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !738
  %3 = bitcast i32** %_25.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3), !noalias !738
  store i32* %__self_0_1.i, i32** %_25.i, align 8, !noalias !738
  %_22.0.i = bitcast i32** %_25.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_18.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [7 x i8] }>* @alloc1207 to [0 x i8]*), i64 7, {}* nonnull align 1 %_22.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.3 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3), !noalias !738
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !738
  ret i1 %4
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h782af9d435b63914E"(%"memmem::Searcher"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_43.i = alloca %"memmem::SearcherKind"*, align 8
  %_35.i = alloca i64**, align 8
  %_27.i = alloca %"memmem::NeedleInfo"*, align 8
  %_19.i = alloca { i8*, i64 }*, align 8
  %_8.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load %"memmem::Searcher"*, %"memmem::Searcher"** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %_4, i64 0, i32 0
  %__self_0_1.i = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %_4, i64 0, i32 3
  %__self_0_2.i = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %_4, i64 0, i32 1
  %__self_0_3.i = getelementptr inbounds %"memmem::Searcher", %"memmem::Searcher"* %_4, i64 0, i32 2
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_8.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !743
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1296 to [0 x i8]*), i64 8), !noalias !747
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_8.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !743
  %2 = bitcast { i8*, i64 }** %_19.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !743
  store { i8*, i64 }* %__self_0_0.i, { i8*, i64 }** %_19.i, align 8, !noalias !743
  %_16.0.i = bitcast { i8*, i64 }** %_19.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_12.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1342 to [0 x i8]*), i64 6, {}* nonnull align 1 %_16.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.j to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !743
  %3 = bitcast %"memmem::NeedleInfo"** %_27.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3), !noalias !743
  store %"memmem::NeedleInfo"* %__self_0_1.i, %"memmem::NeedleInfo"** %_27.i, align 8, !noalias !743
  %_24.0.i = bitcast %"memmem::NeedleInfo"** %_27.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_20.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1301 to [0 x i8]*), i64 5, {}* nonnull align 1 %_24.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.k to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3), !noalias !743
  %4 = bitcast i64*** %_35.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4), !noalias !743
  store i64** %__self_0_2.i, i64*** %_35.i, align 8, !noalias !743
  %_32.0.i = bitcast i64*** %_35.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_28.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1305 to [0 x i8]*), i64 5, {}* nonnull align 1 %_32.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.l to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4), !noalias !743
  %5 = bitcast %"memmem::SearcherKind"** %_43.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %5), !noalias !743
  store %"memmem::SearcherKind"* %__self_0_3.i, %"memmem::SearcherKind"** %_43.i, align 8, !noalias !743
  %_40.0.i = bitcast %"memmem::SearcherKind"** %_43.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_36.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1344 to [0 x i8]*), i64 4, {}* nonnull align 1 %_40.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.m to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %5), !noalias !743
; call core::fmt::builders::DebugStruct::finish
  %6 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_8.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !743
  ret i1 %6
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h8d46de007ceb3d4bE"({ i8, i8 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_25.i = alloca i8*, align 8
  %_17.i = alloca i8*, align 8
  %_6.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load { i8, i8 }*, { i8, i8 }** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds { i8, i8 }, { i8, i8 }* %_4, i64 0, i32 0
  %__self_0_1.i = getelementptr inbounds { i8, i8 }, { i8, i8 }* %_4, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_6.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !748
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [15 x i8] }>* @alloc1215 to [0 x i8]*), i64 15), !noalias !752
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_6.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !748
  %2 = bitcast i8** %_17.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !748
  store i8* %__self_0_0.i, i8** %_17.i, align 8, !noalias !748
  %_14.0.i = bitcast i8** %_17.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_10.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1216 to [0 x i8]*), i64 6, {}* nonnull align 1 %_14.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !748
  %3 = bitcast i8** %_25.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3), !noalias !748
  store i8* %__self_0_1.i, i8** %_25.i, align 8, !noalias !748
  %_22.0.i = bitcast i8** %_25.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_18.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1217 to [0 x i8]*), i64 6, {}* nonnull align 1 %_22.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.2 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3), !noalias !748
; call core::fmt::builders::DebugStruct::finish
  %4 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_6.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !748
  ret i1 %4
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h91a378adcdf2e01eE"(%"memmem::SearcherRevKind"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_4 = load %"memmem::SearcherRevKind"*, %"memmem::SearcherRevKind"** %self, align 8, !nonnull !142
; call <memchr::memmem::SearcherRevKind as core::fmt::Debug>::fmt
  %0 = tail call zeroext i1 @"_ZN68_$LT$memchr..memmem..SearcherRevKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h5bcf687e301f39f3E"(%"memmem::SearcherRevKind"* noalias nonnull readonly align 8 dereferenceable(40) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %0
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17h9eb75065b6b2b2e6E"({ i64, i64 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_4 = load { i64, i64 }*, { i64, i64 }** %self, align 8, !nonnull !142
; call <memchr::memmem::twoway::Shift as core::fmt::Debug>::fmt
  %0 = tail call zeroext i1 @"_ZN66_$LT$memchr..memmem..twoway..Shift$u20$as$u20$core..fmt..Debug$GT$3fmt17hfd4b4a2dd5d7413fE"({ i64, i64 }* noalias nonnull readonly align 8 dereferenceable(16) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %0
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hb757f96e3bd9808dE"(%"memmem::FinderRev"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_16.i = alloca %"memmem::SearcherRev"*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load %"memmem::FinderRev"*, %"memmem::FinderRev"** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds %"memmem::FinderRev", %"memmem::FinderRev"* %_4, i64 0, i32 0
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !753
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [9 x i8] }>* @alloc1286 to [0 x i8]*), i64 9), !noalias !757
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_5.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !753
  %2 = bitcast %"memmem::SearcherRev"** %_16.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !753
  store %"memmem::SearcherRev"* %__self_0_0.i, %"memmem::SearcherRev"** %_16.i, align 8, !noalias !753
  %_13.0.i = bitcast %"memmem::SearcherRev"** %_16.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_9.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1287 to [0 x i8]*), i64 8, {}* nonnull align 1 %_13.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.h to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !753
; call core::fmt::builders::DebugStruct::finish
  %3 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !753
  ret i1 %3
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hc4a9f9ccbf293413E"(i64** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14.i = alloca i64*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugTuple", align 8
  %_4 = load i64*, i64** %self, align 8, !nonnull !142
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0), !noalias !758
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5.i, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [18 x i8] }>* @alloc1248 to [0 x i8]*), i64 18), !noalias !762
  %1 = bitcast i64** %_14.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1), !noalias !758
  store i64* %_4, i64** %_14.i, align 8, !noalias !758
  %_11.0.i = bitcast i64** %_14.i to {}*
; call core::fmt::builders::DebugTuple::field
  %_9.i = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i, {}* nonnull align 1 %_11.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.9 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1), !noalias !758
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0), !noalias !758
  ret i1 %2
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17he2204767e76a9523E"(%"memmem::SearcherKind"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_4 = load %"memmem::SearcherKind"*, %"memmem::SearcherKind"** %self, align 8, !nonnull !142
; call <memchr::memmem::SearcherKind as core::fmt::Debug>::fmt
  %0 = tail call zeroext i1 @"_ZN65_$LT$memchr..memmem..SearcherKind$u20$as$u20$core..fmt..Debug$GT$3fmt17h83b8d41a46a884ccE"(%"memmem::SearcherKind"* noalias nonnull readonly align 8 dereferenceable(40) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  ret i1 %0
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17he241ff79fca5e777E"(i8** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_16.i = alloca i8*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load i8*, i8** %self, align 8, !nonnull !142
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !763
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [14 x i8] }>* @alloc1322 to [0 x i8]*), i64 14), !noalias !767
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_5.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !763
  %2 = bitcast i8** %_16.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !763
  store i8* %_4, i8** %_16.i, align 8, !noalias !763
  %_13.0.i = bitcast i8** %_16.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_9.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [9 x i8] }>* @alloc1323 to [0 x i8]*), i64 9, {}* nonnull align 1 %_13.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.p to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !763
; call core::fmt::builders::DebugStruct::finish
  %3 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !763
  ret i1 %3
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17he61c36eefdf14552E"(%"memmem::SearcherRev"** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_34.i = alloca %"memmem::SearcherRevKind"*, align 8
  %_26.i = alloca { i32, i32 }*, align 8
  %_18.i = alloca { i8*, i64 }*, align 8
  %_7.i = alloca %"core::fmt::builders::DebugStruct", align 8
  %_4 = load %"memmem::SearcherRev"*, %"memmem::SearcherRev"** %self, align 8, !nonnull !142
  %__self_0_0.i = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %_4, i64 0, i32 0
  %__self_0_1.i = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %_4, i64 0, i32 2
  %__self_0_2.i = getelementptr inbounds %"memmem::SearcherRev", %"memmem::SearcherRev"* %_4, i64 0, i32 1
  %0 = bitcast %"core::fmt::builders::DebugStruct"* %_7.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 16, i8* nonnull %0), !noalias !768
; call core::fmt::Formatter::debug_struct
  %1 = tail call i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [11 x i8] }>* @alloc1341 to [0 x i8]*), i64 11), !noalias !772
  %.0..sroa_cast.i = bitcast %"core::fmt::builders::DebugStruct"* %_7.i to i128*
  store i128 %1, i128* %.0..sroa_cast.i, align 8, !noalias !768
  %2 = bitcast { i8*, i64 }** %_18.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %2), !noalias !768
  store { i8*, i64 }* %__self_0_0.i, { i8*, i64 }** %_18.i, align 8, !noalias !768
  %_15.0.i = bitcast { i8*, i64 }** %_18.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_11.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [6 x i8] }>* @alloc1342 to [0 x i8]*), i64 6, {}* nonnull align 1 %_15.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.j to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %2), !noalias !768
  %3 = bitcast { i32, i32 }** %_26.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %3), !noalias !768
  store { i32, i32 }* %__self_0_1.i, { i32, i32 }** %_26.i, align 8, !noalias !768
  %_23.0.i = bitcast { i32, i32 }** %_26.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_19.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [5 x i8] }>* @alloc1343 to [0 x i8]*), i64 5, {}* nonnull align 1 %_23.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.o to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %3), !noalias !768
  %4 = bitcast %"memmem::SearcherRevKind"** %_34.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %4), !noalias !768
  store %"memmem::SearcherRevKind"* %__self_0_2.i, %"memmem::SearcherRevKind"** %_34.i, align 8, !noalias !768
  %_31.0.i = bitcast %"memmem::SearcherRevKind"** %_34.i to {}*
; call core::fmt::builders::DebugStruct::field
  %_27.i = call align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7.i, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [4 x i8] }>* @alloc1344 to [0 x i8]*), i64 4, {}* nonnull align 1 %_31.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.t to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %4), !noalias !768
; call core::fmt::builders::DebugStruct::finish
  %5 = call zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias nonnull align 8 dereferenceable(16) %_7.i)
  call void @llvm.lifetime.end.p0i8(i64 16, i8* nonnull %0), !noalias !768
  ret i1 %5
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hed460d61bf2992aaE"({ i8*, i64 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14.i = alloca { [0 x i8]*, i64 }*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugTuple", align 8
  %_4 = load { i8*, i64 }*, { i8*, i64 }** %self, align 8, !nonnull !142
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0), !noalias !773
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5.i, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [3 x i8] }>* @alloc1190 to [0 x i8]*), i64 3), !noalias !777
  %1 = bitcast { [0 x i8]*, i64 }** %_14.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1), !noalias !773
  %2 = bitcast { [0 x i8]*, i64 }** %_14.i to { i8*, i64 }**
  store { i8*, i64 }* %_4, { i8*, i64 }** %2, align 8, !noalias !773
  %_11.0.i = bitcast { [0 x i8]*, i64 }** %_14.i to {}*
; call core::fmt::builders::DebugTuple::field
  %_9.i = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i, {}* nonnull align 1 %_11.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.1 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1), !noalias !773
; call core::fmt::builders::DebugTuple::finish
  %3 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0), !noalias !773
  ret i1 %3
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hf0ca0eaf8e10788bE"(i64** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_4 = load i64*, i64** %self, align 8, !nonnull !142
; call core::fmt::Formatter::debug_lower_hex
  %_3.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !778
  br i1 %_3.i, label %bb2.i, label %bb4.i

bb4.i:                                            ; preds = %start
; call core::fmt::Formatter::debug_upper_hex
  %_7.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !778
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
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hfc5006809de8f68cE"(i8** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_4 = load i8*, i8** %self, align 8, !nonnull !142
; call core::fmt::Formatter::debug_lower_hex
  %_3.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !781
  br i1 %_3.i, label %bb2.i, label %bb4.i

bb4.i:                                            ; preds = %start
; call core::fmt::Formatter::debug_upper_hex
  %_7.i = tail call zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias nonnull readonly align 8 dereferenceable(64) %f), !noalias !781
  br i1 %_7.i, label %bb6.i, label %bb8.i

bb2.i:                                            ; preds = %start
; call core::fmt::num::<impl core::fmt::LowerHex for u8>::fmt
  %0 = tail call zeroext i1 @"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$u8$GT$3fmt17h9ad27865f481f5ecE"(i8* noalias nonnull readonly align 1 dereferenceable(1) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E.exit"

bb8.i:                                            ; preds = %bb4.i
; call core::fmt::num::imp::<impl core::fmt::Display for u8>::fmt
  %1 = tail call zeroext i1 @"_ZN4core3fmt3num3imp51_$LT$impl$u20$core..fmt..Display$u20$for$u20$u8$GT$3fmt17h7657a76892c51730E"(i8* noalias nonnull readonly align 1 dereferenceable(1) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E.exit"

bb6.i:                                            ; preds = %bb4.i
; call core::fmt::num::<impl core::fmt::UpperHex for u8>::fmt
  %2 = tail call zeroext i1 @"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$u8$GT$3fmt17h1f7726c1de44a74fE"(i8* noalias nonnull readonly align 1 dereferenceable(1) %_4, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f)
  br label %"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E.exit"

"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E.exit": ; preds = %bb2.i, %bb8.i, %bb6.i
  %.0.in.i = phi i1 [ %0, %bb2.i ], [ %2, %bb6.i ], [ %1, %bb8.i ]
  ret i1 %.0.in.i
}

; <&T as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
define internal zeroext i1 @"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hff7095ab22540aa2E"({ i8*, i64 }** noalias nocapture readonly align 8 dereferenceable(8) %self, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64) %f) unnamed_addr #6 {
start:
  %_14.i = alloca { i8*, i64 }*, align 8
  %_5.i = alloca %"core::fmt::builders::DebugTuple", align 8
  %_4 = load { i8*, i64 }*, { i8*, i64 }** %self, align 8, !nonnull !142
  %0 = bitcast %"core::fmt::builders::DebugTuple"* %_5.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 24, i8* nonnull %0), !noalias !784
; call core::fmt::Formatter::debug_tuple
  call void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture nonnull sret(%"core::fmt::builders::DebugTuple") dereferenceable(24) %_5.i, %"core::fmt::Formatter"* noalias nonnull align 8 dereferenceable(64) %f, [0 x i8]* noalias nonnull readonly align 1 bitcast (<{ [8 x i8] }>* @alloc1186 to [0 x i8]*), i64 8), !noalias !788
  %1 = bitcast { i8*, i64 }** %_14.i to i8*
  call void @llvm.lifetime.start.p0i8(i64 8, i8* nonnull %1), !noalias !784
  store { i8*, i64 }* %_4, { i8*, i64 }** %_14.i, align 8, !noalias !784
  %_11.0.i = bitcast { i8*, i64 }** %_14.i to {}*
; call core::fmt::builders::DebugTuple::field
  %_9.i = call align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i, {}* nonnull align 1 %_11.0.i, [3 x i64]* noalias readonly align 8 dereferenceable(24) bitcast (<{ i8*, [16 x i8], i8*, [0 x i8] }>* @vtable.0 to [3 x i64]*))
  call void @llvm.lifetime.end.p0i8(i64 8, i8* nonnull %1), !noalias !784
; call core::fmt::builders::DebugTuple::finish
  %2 = call zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias nonnull align 8 dereferenceable(24) %_5.i)
  call void @llvm.lifetime.end.p0i8(i64 24, i8* nonnull %0), !noalias !784
  ret i1 %2
}

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.start.p0i8(i64 immarg, i8* nocapture) #13

; Function Attrs: argmemonly mustprogress nofree nosync nounwind willreturn
declare void @llvm.lifetime.end.p0i8(i64 immarg, i8* nocapture) #13

; Function Attrs: argmemonly mustprogress nofree nounwind willreturn
declare void @llvm.memcpy.p0i8.p0i8.i64(i8* noalias nocapture writeonly, i8* noalias nocapture readonly, i64, i1 immarg) #14

; Function Attrs: nonlazybind
declare i32 @rust_eh_personality(...) unnamed_addr #15

; core::panicking::panic
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core9panicking5panic17he84354dce55c9beeE([0 x i8]* noalias nonnull readonly align 1, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #16

; core::panicking::panic_bounds_check
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core9panicking18panic_bounds_check17h12b597476ba48ffaE(i64, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #16

; <str as core::fmt::Debug>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN40_$LT$str$u20$as$u20$core..fmt..Debug$GT$3fmt17h2fc61a29b5ba73d9E"([0 x i8]* noalias nonnull readonly align 1, i64, %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::Formatter::debug_tuple
; Function Attrs: nonlazybind uwtable
declare void @_ZN4core3fmt9Formatter11debug_tuple17hd222c0d4f737f527E(%"core::fmt::builders::DebugTuple"* noalias nocapture sret(%"core::fmt::builders::DebugTuple") dereferenceable(24), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #6

; core::fmt::builders::DebugTuple::field
; Function Attrs: nonlazybind uwtable
declare align 8 dereferenceable(24) %"core::fmt::builders::DebugTuple"* @_ZN4core3fmt8builders10DebugTuple5field17h43140f9a23db6ef6E(%"core::fmt::builders::DebugTuple"* noalias align 8 dereferenceable(24), {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24)) unnamed_addr #6

; core::fmt::builders::DebugTuple::finish
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt8builders10DebugTuple6finish17h3ba7d6c565474be2E(%"core::fmt::builders::DebugTuple"* noalias align 8 dereferenceable(24)) unnamed_addr #6

; core::fmt::Formatter::debug_struct
; Function Attrs: nonlazybind uwtable
declare i128 @_ZN4core3fmt9Formatter12debug_struct17h323878815cd69fbbE(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #6

; core::fmt::builders::DebugStruct::field
; Function Attrs: nonlazybind uwtable
declare align 8 dereferenceable(16) %"core::fmt::builders::DebugStruct"* @_ZN4core3fmt8builders11DebugStruct5field17hbae452953895937aE(%"core::fmt::builders::DebugStruct"* noalias align 8 dereferenceable(16), [0 x i8]* noalias nonnull readonly align 1, i64, {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24)) unnamed_addr #6

; core::fmt::builders::DebugStruct::finish
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt8builders11DebugStruct6finish17hea7da80ca448a6b1E(%"core::fmt::builders::DebugStruct"* noalias align 8 dereferenceable(16)) unnamed_addr #6

; core::fmt::Formatter::write_str
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter9write_str17h89723935f155226aE(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #6

; core::panicking::assert_failed_inner
; Function Attrs: noreturn nonlazybind uwtable
declare void @_ZN4core9panicking19assert_failed_inner17hd256bbdf19083f81E(i8, {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24), {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24), %"core::option::Option<core::fmt::Arguments>"* noalias nocapture dereferenceable(48), %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #17

; core::fmt::builders::DebugList::entry
; Function Attrs: nonlazybind uwtable
declare align 8 dereferenceable(16) %"core::fmt::builders::DebugList"* @_ZN4core3fmt8builders9DebugList5entry17h70c8a86ab213bc16E(%"core::fmt::builders::DebugList"* noalias align 8 dereferenceable(16), {}* nonnull align 1, [3 x i64]* noalias readonly align 8 dereferenceable(24)) unnamed_addr #6

; core::fmt::Formatter::debug_list
; Function Attrs: nonlazybind uwtable
declare i128 @_ZN4core3fmt9Formatter10debug_list17h804611945d4c2177E(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::builders::DebugList::finish
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt8builders9DebugList6finish17h02d7d1d08d1d92d5E(%"core::fmt::builders::DebugList"* noalias align 8 dereferenceable(16)) unnamed_addr #6

; core::fmt::Formatter::pad
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter3pad17h40fda45ac7db157fE(%"core::fmt::Formatter"* noalias align 8 dereferenceable(64), [0 x i8]* noalias nonnull readonly align 1, i64) unnamed_addr #6

; core::slice::index::slice_end_index_len_fail
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core5slice5index24slice_end_index_len_fail17h7d511eec41d6bce9E(i64, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #16

; core::slice::index::slice_start_index_len_fail
; Function Attrs: cold noinline noreturn nonlazybind uwtable
declare void @_ZN4core5slice5index26slice_start_index_len_fail17h3f1b9df81972beedE(i64, i64, %"core::panic::location::Location"* noalias readonly align 8 dereferenceable(24)) unnamed_addr #16

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i16 @llvm.ctlz.i16(i16, i1 immarg) #18

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i32 @llvm.cttz.i32(i32, i1 immarg) #18

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i32 @llvm.uadd.sat.i32(i32, i32) #18

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare i64 @llvm.usub.sat.i64(i64, i64) #18

; Function Attrs: mustprogress nofree nosync nounwind readnone speculatable willreturn
declare { i64, i1 } @llvm.usub.with.overflow.i64(i64, i64) #18

; core::fmt::Formatter::debug_lower_hex
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter15debug_lower_hex17hcc93c01961cf1c37E(%"core::fmt::Formatter"* noalias readonly align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::<impl core::fmt::LowerHex for u8>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$u8$GT$3fmt17h9ad27865f481f5ecE"(i8* noalias readonly align 1 dereferenceable(1), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::Formatter::debug_upper_hex
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @_ZN4core3fmt9Formatter15debug_upper_hex17h7a432397bf28b124E(%"core::fmt::Formatter"* noalias readonly align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::<impl core::fmt::UpperHex for u8>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$u8$GT$3fmt17h1f7726c1de44a74fE"(i8* noalias readonly align 1 dereferenceable(1), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::imp::<impl core::fmt::Display for u8>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num3imp51_$LT$impl$u20$core..fmt..Display$u20$for$u20$u8$GT$3fmt17h7657a76892c51730E"(i8* noalias readonly align 1 dereferenceable(1), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::<impl core::fmt::LowerHex for u32>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$u32$GT$3fmt17hecbcc5a775cb29a6E"(i32* noalias readonly align 4 dereferenceable(4), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::<impl core::fmt::UpperHex for u32>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$u32$GT$3fmt17h846f73226b9e806fE"(i32* noalias readonly align 4 dereferenceable(4), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::imp::<impl core::fmt::Display for u32>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num3imp52_$LT$impl$u20$core..fmt..Display$u20$for$u20$u32$GT$3fmt17h80e28ec8edb27b1fE"(i32* noalias readonly align 4 dereferenceable(4), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::<impl core::fmt::LowerHex for u64>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$u64$GT$3fmt17ha1880b12d13db428E"(i64* noalias readonly align 8 dereferenceable(8), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::<impl core::fmt::UpperHex for u64>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num53_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$u64$GT$3fmt17h10594f3bd2afbabbE"(i64* noalias readonly align 8 dereferenceable(8), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::imp::<impl core::fmt::Display for u64>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num3imp52_$LT$impl$u20$core..fmt..Display$u20$for$u20$u64$GT$3fmt17h2c6c89560bbd7c4fE"(i64* noalias readonly align 8 dereferenceable(8), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::<impl core::fmt::LowerHex for usize>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num55_$LT$impl$u20$core..fmt..LowerHex$u20$for$u20$usize$GT$3fmt17h7a0715c481903e22E"(i64* noalias readonly align 8 dereferenceable(8), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::<impl core::fmt::UpperHex for usize>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num55_$LT$impl$u20$core..fmt..UpperHex$u20$for$u20$usize$GT$3fmt17h8135d2e0b80633e3E"(i64* noalias readonly align 8 dereferenceable(8), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; core::fmt::num::imp::<impl core::fmt::Display for usize>::fmt
; Function Attrs: nonlazybind uwtable
declare zeroext i1 @"_ZN4core3fmt3num3imp54_$LT$impl$u20$core..fmt..Display$u20$for$u20$usize$GT$3fmt17hf09e7a3079136607E"(i64* noalias readonly align 8 dereferenceable(8), %"core::fmt::Formatter"* noalias align 8 dereferenceable(64)) unnamed_addr #6

; Function Attrs: inaccessiblememonly nofree nosync nounwind willreturn
declare void @llvm.experimental.noalias.scope.decl(metadata) #19

; Function Attrs: nofree nosync nounwind readnone speculatable willreturn
declare i16 @llvm.cttz.i16(i16, i1 immarg) #20

; Function Attrs: nofree nosync nounwind readnone speculatable willreturn
declare i64 @llvm.umax.i64(i64, i64) #20

attributes #0 = { inlinehint mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { nofree nosync nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { nofree nosync nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" "target-features"="+sse2" }
attributes #3 = { cold mustprogress nofree noinline norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #4 = { cold mustprogress nofree noinline nosync nounwind nonlazybind uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #5 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" "target-features"="+sse2" }
attributes #6 = { nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #7 = { mustprogress nofree norecurse nosync nounwind nonlazybind readnone uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #8 = { cold nofree noinline nosync nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #9 = { mustprogress nofree norecurse nosync nounwind nonlazybind uwtable willreturn writeonly "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #10 = { mustprogress nofree nosync nounwind nonlazybind uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #11 = { noinline nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #12 = { cold noreturn nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #13 = { argmemonly mustprogress nofree nosync nounwind willreturn }
attributes #14 = { argmemonly mustprogress nofree nounwind willreturn }
attributes #15 = { nonlazybind "target-cpu"="x86-64" }
attributes #16 = { cold noinline noreturn nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #17 = { noreturn nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #18 = { mustprogress nofree nosync nounwind readnone speculatable willreturn }
attributes #19 = { inaccessiblememonly nofree nosync nounwind willreturn }
attributes #20 = { nofree nosync nounwind readnone speculatable willreturn }
attributes #21 = { nounwind }
attributes #22 = { noreturn }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
!2 = !{!3}
!3 = distinct !{!3, !4, !"_ZN6memchr6memchr8fallback14forward_search17h69069f1208e9b0b5E: argument 0"}
!4 = distinct !{!4, !"_ZN6memchr6memchr8fallback14forward_search17h69069f1208e9b0b5E"}
!5 = !{!6}
!6 = distinct !{!6, !7, !"_ZN6memchr6memchr8fallback14forward_search17h69069f1208e9b0b5E: argument 0"}
!7 = distinct !{!7, !"_ZN6memchr6memchr8fallback14forward_search17h69069f1208e9b0b5E"}
!8 = !{!9}
!9 = distinct !{!9, !10, !"_ZN6memchr6memchr8fallback14forward_search17h69069f1208e9b0b5E: argument 0"}
!10 = distinct !{!10, !"_ZN6memchr6memchr8fallback14forward_search17h69069f1208e9b0b5E"}
!11 = !{!12, !14}
!12 = distinct !{!12, !13, !"_ZN6memchr6memchr8fallback14forward_search17h7d3505cecbc7171aE: argument 0"}
!13 = distinct !{!13, !"_ZN6memchr6memchr8fallback14forward_search17h7d3505cecbc7171aE"}
!14 = distinct !{!14, !13, !"_ZN6memchr6memchr8fallback14forward_search17h7d3505cecbc7171aE: argument 1"}
!15 = !{!16, !18}
!16 = distinct !{!16, !17, !"_ZN6memchr6memchr8fallback14forward_search17h7d3505cecbc7171aE: argument 0"}
!17 = distinct !{!17, !"_ZN6memchr6memchr8fallback14forward_search17h7d3505cecbc7171aE"}
!18 = distinct !{!18, !17, !"_ZN6memchr6memchr8fallback14forward_search17h7d3505cecbc7171aE: argument 1"}
!19 = !{!20, !22}
!20 = distinct !{!20, !21, !"_ZN6memchr6memchr8fallback14forward_search17h7d3505cecbc7171aE: argument 0"}
!21 = distinct !{!21, !"_ZN6memchr6memchr8fallback14forward_search17h7d3505cecbc7171aE"}
!22 = distinct !{!22, !21, !"_ZN6memchr6memchr8fallback14forward_search17h7d3505cecbc7171aE: argument 1"}
!23 = !{!24}
!24 = distinct !{!24, !25, !"_ZN6memchr6memchr8fallback14reverse_search17h3c199f95de44b403E: argument 0"}
!25 = distinct !{!25, !"_ZN6memchr6memchr8fallback14reverse_search17h3c199f95de44b403E"}
!26 = !{!27}
!27 = distinct !{!27, !28, !"_ZN6memchr6memchr8fallback14reverse_search17h3c199f95de44b403E: argument 0"}
!28 = distinct !{!28, !"_ZN6memchr6memchr8fallback14reverse_search17h3c199f95de44b403E"}
!29 = !{!30}
!30 = distinct !{!30, !31, !"_ZN6memchr6memchr8fallback14reverse_search17h3c199f95de44b403E: argument 0"}
!31 = distinct !{!31, !"_ZN6memchr6memchr8fallback14reverse_search17h3c199f95de44b403E"}
!32 = !{!33, !35}
!33 = distinct !{!33, !34, !"_ZN6memchr6memchr8fallback14reverse_search17h237d49dcd62d3925E: argument 0"}
!34 = distinct !{!34, !"_ZN6memchr6memchr8fallback14reverse_search17h237d49dcd62d3925E"}
!35 = distinct !{!35, !34, !"_ZN6memchr6memchr8fallback14reverse_search17h237d49dcd62d3925E: argument 1"}
!36 = !{!37, !39}
!37 = distinct !{!37, !38, !"_ZN6memchr6memchr8fallback14reverse_search17h237d49dcd62d3925E: argument 0"}
!38 = distinct !{!38, !"_ZN6memchr6memchr8fallback14reverse_search17h237d49dcd62d3925E"}
!39 = distinct !{!39, !38, !"_ZN6memchr6memchr8fallback14reverse_search17h237d49dcd62d3925E: argument 1"}
!40 = !{!41, !43}
!41 = distinct !{!41, !42, !"_ZN6memchr6memchr8fallback14reverse_search17h237d49dcd62d3925E: argument 0"}
!42 = distinct !{!42, !"_ZN6memchr6memchr8fallback14reverse_search17h237d49dcd62d3925E"}
!43 = distinct !{!43, !42, !"_ZN6memchr6memchr8fallback14reverse_search17h237d49dcd62d3925E: argument 1"}
!44 = !{i16 0, i16 17}
!45 = !{i32 0, i32 33}
!46 = !{!47}
!47 = distinct !{!47, !48, !"_ZN6memchr6memmem9prefilter14PrefilterState6update17h8fc59544a3b42f6bE: %self"}
!48 = distinct !{!48, !"_ZN6memchr6memmem9prefilter14PrefilterState6update17h8fc59544a3b42f6bE"}
!49 = !{!50}
!50 = distinct !{!50, !51, !"_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE: %prestate"}
!51 = distinct !{!51, !"_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE"}
!52 = !{!53}
!53 = distinct !{!53, !51, !"_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE: %ninfo"}
!54 = !{!55}
!55 = distinct !{!55, !51, !"_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE: %haystack.0"}
!56 = !{!57}
!57 = distinct !{!57, !51, !"_ZN6memchr6memmem9prefilter11genericsimd4find17h9f1e59497e0be3dcE: %needle.0"}
!58 = !{!50, !55, !57}
!59 = !{!50, !53, !55, !57}
!60 = !{!"branch_weights", i32 2000, i32 1}
!61 = !{!62}
!62 = distinct !{!62, !63, !"_ZN6memchr6memmem9prefilter3x863sse4find22simple_memchr_fallback17h813d021b8f6b02feE: %haystack.0"}
!63 = distinct !{!63, !"_ZN6memchr6memmem9prefilter3x863sse4find22simple_memchr_fallback17h813d021b8f6b02feE"}
!64 = !{!65}
!65 = distinct !{!65, !63, !"_ZN6memchr6memmem9prefilter3x863sse4find22simple_memchr_fallback17h813d021b8f6b02feE: %needle.0"}
!66 = !{!65, !57}
!67 = !{!68, !62, !50, !53, !55}
!68 = distinct !{!68, !63, !"_ZN6memchr6memmem9prefilter3x863sse4find22simple_memchr_fallback17h813d021b8f6b02feE: %ninfo"}
!69 = !{!70, !72, !74, !76, !62, !55}
!70 = distinct !{!70, !71, !"_ZN6memchr6memchr3x864sse26memchr17h8c22de531a37fcc8E: %haystack.0"}
!71 = distinct !{!71, !"_ZN6memchr6memchr3x864sse26memchr17h8c22de531a37fcc8E"}
!72 = distinct !{!72, !73, !"_ZN6memchr6memchr3x866memchr17h10c4f2e2b688bc94E: %haystack.0"}
!73 = distinct !{!73, !"_ZN6memchr6memchr3x866memchr17h10c4f2e2b688bc94E"}
!74 = distinct !{!74, !75, !"_ZN6memchr6memchr6memchr3imp17heab8178553c66ae4E: %haystack.0"}
!75 = distinct !{!75, !"_ZN6memchr6memchr6memchr3imp17heab8178553c66ae4E"}
!76 = distinct !{!76, !77, !"_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE: %haystack.0"}
!77 = distinct !{!77, !"_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE"}
!78 = !{!68, !65, !50, !53, !57}
!79 = !{!68, !62, !65, !50, !53, !55, !57}
!80 = !{!50, !53, !55}
!81 = !{!50, !53, !57}
!82 = !{!53, !57}
!83 = !{!84, !50}
!84 = distinct !{!84, !85, !"_ZN6memchr6memmem9prefilter14PrefilterState6update17h8fc59544a3b42f6bE: %self"}
!85 = distinct !{!85, !"_ZN6memchr6memmem9prefilter14PrefilterState6update17h8fc59544a3b42f6bE"}
!86 = !{!53, !55, !57}
!87 = !{!88}
!88 = distinct !{!88, !89, !"_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE: %needle.0"}
!89 = distinct !{!89, !"_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE"}
!90 = !{!91, !93}
!91 = distinct !{!91, !92, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E: %bytes.0"}
!92 = distinct !{!92, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E"}
!93 = distinct !{!93, !94, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E: argument 0"}
!94 = distinct !{!94, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E"}
!95 = !{!96}
!96 = distinct !{!96, !94, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E: %needle.0"}
!97 = distinct !{!97, !98}
!98 = !{!"llvm.loop.unroll.disable"}
!99 = !{!93}
!100 = !{!101}
!101 = distinct !{!101, !102, !"_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E: %needle.0"}
!102 = distinct !{!102, !"_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E"}
!103 = !{!104, !106}
!104 = distinct !{!104, !105, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE: %bytes.0"}
!105 = distinct !{!105, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE"}
!106 = distinct !{!106, !107, !"_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE: argument 1"}
!107 = distinct !{!107, !"_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE"}
!108 = !{!109, !110}
!109 = distinct !{!109, !107, !"_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE: argument 0"}
!110 = distinct !{!110, !107, !"_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE: %needle.0"}
!111 = distinct !{!111, !98}
!112 = !{!109}
!113 = !{!106}
!114 = !{!115}
!115 = distinct !{!115, !116, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE: %bytes.0"}
!116 = distinct !{!116, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE"}
!117 = distinct !{!117, !98}
!118 = !{!119}
!119 = distinct !{!119, !120, !"_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE: %haystack.0"}
!120 = distinct !{!120, !"_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE"}
!121 = !{!122}
!122 = distinct !{!122, !120, !"_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE: %needle.0"}
!123 = !{!124}
!124 = distinct !{!124, !125, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %x.0"}
!125 = distinct !{!125, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E"}
!126 = !{!127}
!127 = distinct !{!127, !125, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %y.0"}
!128 = !{!124, !119}
!129 = !{!127, !122}
!130 = !{!131}
!131 = distinct !{!131, !132, !"_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E: %haystack.0"}
!132 = distinct !{!132, !"_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E"}
!133 = !{!134}
!134 = distinct !{!134, !132, !"_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E: %needle.0"}
!135 = !{!136}
!136 = distinct !{!136, !137, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %x.0"}
!137 = distinct !{!137, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E"}
!138 = !{!139}
!139 = distinct !{!139, !137, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %y.0"}
!140 = !{!136, !131}
!141 = !{!139, !134}
!142 = !{}
!143 = !{!144}
!144 = distinct !{!144, !145, !"_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE: %self"}
!145 = distinct !{!145, !"_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE"}
!146 = !{!147}
!147 = distinct !{!147, !145, !"_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE: %haystack.0"}
!148 = !{!149, !151, !153, !144}
!149 = distinct !{!149, !150, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE: %self"}
!150 = distinct !{!150, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE"}
!151 = distinct !{!151, !152, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE: %self"}
!152 = distinct !{!152, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE"}
!153 = distinct !{!153, !154, !"_ZN6memchr6memmem8Searcher6needle17hf8fce88406e6e39aE: %self"}
!154 = distinct !{!154, !"_ZN6memchr6memmem8Searcher6needle17hf8fce88406e6e39aE"}
!155 = !{!156, !147}
!156 = distinct !{!156, !145, !"_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE: %state"}
!157 = !{i8 0, i8 5}
!158 = !{!159, !161, !163, !165, !147}
!159 = distinct !{!159, !160, !"_ZN6memchr6memchr3x864sse26memchr17h8c22de531a37fcc8E: %haystack.0"}
!160 = distinct !{!160, !"_ZN6memchr6memchr3x864sse26memchr17h8c22de531a37fcc8E"}
!161 = distinct !{!161, !162, !"_ZN6memchr6memchr3x866memchr17h10c4f2e2b688bc94E: %haystack.0"}
!162 = distinct !{!162, !"_ZN6memchr6memchr3x866memchr17h10c4f2e2b688bc94E"}
!163 = distinct !{!163, !164, !"_ZN6memchr6memchr6memchr3imp17heab8178553c66ae4E: %haystack.0"}
!164 = distinct !{!164, !"_ZN6memchr6memchr6memchr3imp17heab8178553c66ae4E"}
!165 = distinct !{!165, !166, !"_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE: %haystack.0"}
!166 = distinct !{!166, !"_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE"}
!167 = !{!144, !156}
!168 = !{!169, !171, !144}
!169 = distinct !{!169, !170, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %fwd"}
!170 = distinct !{!170, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E"}
!171 = distinct !{!171, !172, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %self"}
!172 = distinct !{!172, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E"}
!173 = !{!174, !175, !176, !177, !156, !147}
!174 = distinct !{!174, !170, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %haystack.0"}
!175 = distinct !{!175, !170, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %needle.0"}
!176 = distinct !{!176, !172, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %haystack.0"}
!177 = distinct !{!177, !172, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %needle.0"}
!178 = !{!179}
!179 = distinct !{!179, !180, !"_ZN6memchr6memmem3x863sse7Forward4find17h59c5c470db40e2beE: %haystack.0"}
!180 = distinct !{!180, !"_ZN6memchr6memmem3x863sse7Forward4find17h59c5c470db40e2beE"}
!181 = !{!182}
!182 = distinct !{!182, !180, !"_ZN6memchr6memmem3x863sse7Forward4find17h59c5c470db40e2beE: %needle.0"}
!183 = !{!184}
!184 = distinct !{!184, !185, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %haystack.0"}
!185 = distinct !{!185, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E"}
!186 = !{!187}
!187 = distinct !{!187, !185, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %needle.0"}
!188 = !{!189}
!189 = distinct !{!189, !190, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %haystack.0"}
!190 = distinct !{!190, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E"}
!191 = !{!192}
!192 = distinct !{!192, !190, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %needle.0"}
!193 = !{!192, !187, !182}
!194 = !{!195, !189, !196, !184, !179, !144, !156}
!195 = distinct !{!195, !190, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %fwd"}
!196 = distinct !{!196, !185, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %self"}
!197 = !{!195, !192, !196, !187, !182, !144, !156}
!198 = !{!199}
!199 = distinct !{!199, !200, !"_ZN6memchr6memmem11genericsimd17fwd_find_in_chunk17h977e1fcff57f72e2E: %needle.0"}
!200 = distinct !{!200, !"_ZN6memchr6memmem11genericsimd17fwd_find_in_chunk17h977e1fcff57f72e2E"}
!201 = !{!189, !184, !179, !147}
!202 = !{!199, !195, !192, !196, !187, !182, !144, !156}
!203 = !{!204}
!204 = distinct !{!204, !205, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %x.0"}
!205 = distinct !{!205, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E"}
!206 = !{!207}
!207 = distinct !{!207, !205, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %y.0"}
!208 = !{!204, !199, !192, !187, !182}
!209 = !{!207, !195, !189, !196, !184, !179, !144, !156}
!210 = !{!207, !189, !184, !179, !147}
!211 = !{!204, !199, !195, !192, !196, !187, !182, !144, !156}
!212 = !{!213}
!213 = distinct !{!213, !214, !"_ZN6memchr6memmem11genericsimd17fwd_find_in_chunk17h977e1fcff57f72e2E: %needle.0"}
!214 = distinct !{!214, !"_ZN6memchr6memmem11genericsimd17fwd_find_in_chunk17h977e1fcff57f72e2E"}
!215 = !{!213, !195, !192, !196, !187, !182, !144, !156}
!216 = !{!217}
!217 = distinct !{!217, !218, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %x.0"}
!218 = distinct !{!218, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E"}
!219 = !{!220}
!220 = distinct !{!220, !218, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %y.0"}
!221 = !{!217, !213, !192, !187, !182}
!222 = !{!220, !195, !189, !196, !184, !179, !144, !156}
!223 = !{!220, !189, !184, !179, !147}
!224 = !{!217, !213, !195, !192, !196, !187, !182, !144, !156}
!225 = !{!226, !228, !147}
!226 = distinct !{!226, !227, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E: %bytes.0"}
!227 = distinct !{!227, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E"}
!228 = distinct !{!228, !229, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E: argument 0"}
!229 = distinct !{!229, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E"}
!230 = !{!231, !144, !156}
!231 = distinct !{!231, !229, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E: %needle.0"}
!232 = distinct !{!232, !98}
!233 = !{!228, !147}
!234 = !{!235, !237, !147}
!235 = distinct !{!235, !236, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E: %bytes.0"}
!236 = distinct !{!236, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E"}
!237 = distinct !{!237, !238, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E: argument 0"}
!238 = distinct !{!238, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E"}
!239 = !{!240, !144, !156}
!240 = distinct !{!240, !238, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E: %needle.0"}
!241 = distinct !{!241, !98}
!242 = !{!237, !147}
!243 = !{!244, !246, !248, !250}
!244 = distinct !{!244, !245, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE: %self"}
!245 = distinct !{!245, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE"}
!246 = distinct !{!246, !247, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE: %self"}
!247 = distinct !{!247, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE"}
!248 = distinct !{!248, !249, !"_ZN6memchr6memmem8Searcher6needle17hf8fce88406e6e39aE: %self"}
!249 = distinct !{!249, !"_ZN6memchr6memmem8Searcher6needle17hf8fce88406e6e39aE"}
!250 = distinct !{!250, !251, !"_ZN6memchr6memmem6Finder6needle17h16141abdde3d7a3eE: %self"}
!251 = distinct !{!251, !"_ZN6memchr6memmem6Finder6needle17h16141abdde3d7a3eE"}
!252 = !{i64 0, i64 2}
!253 = !{!254, !256, !258}
!254 = distinct !{!254, !255, !"_ZN106_$LT$core..ops..range..Range$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17h0a7a5ee63b824680E: %slice.0"}
!255 = distinct !{!255, !"_ZN106_$LT$core..ops..range..Range$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17h0a7a5ee63b824680E"}
!256 = distinct !{!256, !257, !"_ZN108_$LT$core..ops..range..RangeTo$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17h02d8c8385a557d52E: %slice.0"}
!257 = distinct !{!257, !"_ZN108_$LT$core..ops..range..RangeTo$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17h02d8c8385a557d52E"}
!258 = distinct !{!258, !259, !"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE: %self.0"}
!259 = distinct !{!259, !"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE"}
!260 = !{!261}
!261 = distinct !{!261, !262, !"_ZN6memchr6memmem9FinderRev5rfind17h123c410f0328d29aE: %self"}
!262 = distinct !{!262, !"_ZN6memchr6memmem9FinderRev5rfind17h123c410f0328d29aE"}
!263 = !{!264}
!264 = distinct !{!264, !262, !"_ZN6memchr6memmem9FinderRev5rfind17h123c410f0328d29aE: argument 1"}
!265 = !{!266}
!266 = distinct !{!266, !267, !"_ZN6memchr6memmem11SearcherRev5rfind17hab7e642e102f371bE: %self"}
!267 = distinct !{!267, !"_ZN6memchr6memmem11SearcherRev5rfind17hab7e642e102f371bE"}
!268 = !{!269}
!269 = distinct !{!269, !267, !"_ZN6memchr6memmem11SearcherRev5rfind17hab7e642e102f371bE: %haystack.0"}
!270 = !{!271, !273, !275, !266, !261}
!271 = distinct !{!271, !272, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE: %self"}
!272 = distinct !{!272, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE"}
!273 = distinct !{!273, !274, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE: %self"}
!274 = distinct !{!274, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE"}
!275 = distinct !{!275, !276, !"_ZN6memchr6memmem11SearcherRev6needle17hdba161fcdb320d48E: %self"}
!276 = distinct !{!276, !"_ZN6memchr6memmem11SearcherRev6needle17hdba161fcdb320d48E"}
!277 = !{!269, !264}
!278 = !{i8 0, i8 3}
!279 = !{!266, !261}
!280 = !{!281, !283, !285, !287, !269, !264}
!281 = distinct !{!281, !282, !"_ZN6memchr6memchr3x864sse27memrchr17h6b1a5fd77ea8a7e0E: %haystack.0"}
!282 = distinct !{!282, !"_ZN6memchr6memchr3x864sse27memrchr17h6b1a5fd77ea8a7e0E"}
!283 = distinct !{!283, !284, !"_ZN6memchr6memchr3x867memrchr17hee84f3be3722a97eE: %haystack.0"}
!284 = distinct !{!284, !"_ZN6memchr6memchr3x867memrchr17hee84f3be3722a97eE"}
!285 = distinct !{!285, !286, !"_ZN6memchr6memchr7memrchr3imp17h3e17d0ee5af4fa3fE: %haystack.0"}
!286 = distinct !{!286, !"_ZN6memchr6memchr7memrchr3imp17h3e17d0ee5af4fa3fE"}
!287 = distinct !{!287, !288, !"_ZN6memchr6memchr7memrchr17h44e0305fb50d1a13E: %haystack.0"}
!288 = distinct !{!288, !"_ZN6memchr6memchr7memrchr17h44e0305fb50d1a13E"}
!289 = !{!290}
!290 = distinct !{!290, !291, !"_ZN6memchr6memmem6twoway7Reverse5rfind17h269f7cd3ca266726E: %self"}
!291 = distinct !{!291, !"_ZN6memchr6memmem6twoway7Reverse5rfind17h269f7cd3ca266726E"}
!292 = !{!293}
!293 = distinct !{!293, !291, !"_ZN6memchr6memmem6twoway7Reverse5rfind17h269f7cd3ca266726E: %haystack.0"}
!294 = !{!295}
!295 = distinct !{!295, !291, !"_ZN6memchr6memmem6twoway7Reverse5rfind17h269f7cd3ca266726E: %needle.0"}
!296 = !{!290, !266, !261}
!297 = !{!293, !295, !269, !264}
!298 = !{!299}
!299 = distinct !{!299, !300, !"_ZN6memchr6memmem6twoway7Reverse15rfind_small_imp17h3217f32ef45ba4e4E: %haystack.0"}
!300 = distinct !{!300, !"_ZN6memchr6memmem6twoway7Reverse15rfind_small_imp17h3217f32ef45ba4e4E"}
!301 = !{!302}
!302 = distinct !{!302, !300, !"_ZN6memchr6memmem6twoway7Reverse15rfind_small_imp17h3217f32ef45ba4e4E: %needle.0"}
!303 = !{!"branch_weights", i32 1, i32 2000}
!304 = !{!299, !293, !269, !264}
!305 = !{!302, !290, !295, !266, !261}
!306 = !{!299, !302, !290, !293, !295, !266, !261}
!307 = !{!302, !295}
!308 = !{!299, !290, !293, !266, !261}
!309 = !{!310}
!310 = distinct !{!310, !311, !"_ZN6memchr6memmem6twoway7Reverse15rfind_large_imp17h65648b9e4a5789d2E: %haystack.0"}
!311 = distinct !{!311, !"_ZN6memchr6memmem6twoway7Reverse15rfind_large_imp17h65648b9e4a5789d2E"}
!312 = !{!313}
!313 = distinct !{!313, !311, !"_ZN6memchr6memmem6twoway7Reverse15rfind_large_imp17h65648b9e4a5789d2E: %needle.0"}
!314 = !{!310, !293, !269, !264}
!315 = !{!313, !290, !295, !266, !261}
!316 = !{!313, !295}
!317 = !{!310, !290, !293, !266, !261}
!318 = !{!310, !313, !290, !293, !295, !266, !261}
!319 = !{!320}
!320 = distinct !{!320, !321, !"_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE: argument 0"}
!321 = distinct !{!321, !"_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE"}
!322 = !{!323}
!323 = distinct !{!323, !321, !"_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE: argument 1"}
!324 = !{!325, !323, !269, !264}
!325 = distinct !{!325, !326, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE: %bytes.0"}
!326 = distinct !{!326, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_rev17h6b054ec2652e250bE"}
!327 = !{!320, !328, !266, !261}
!328 = distinct !{!328, !321, !"_ZN6memchr6memmem9rabinkarp10rfind_with17h942bd5e0d52fd60dE: %needle.0"}
!329 = distinct !{!329, !98}
!330 = !{!320, !266, !261}
!331 = !{!323, !328, !269, !264}
!332 = !{!323, !269, !264}
!333 = !{!334, !336, !338}
!334 = distinct !{!334, !335, !"_ZN4core6option15Option$LT$T$GT$7is_some17hff66e2e4f2d461a6E: %self"}
!335 = distinct !{!335, !"_ZN4core6option15Option$LT$T$GT$7is_some17hff66e2e4f2d461a6E"}
!336 = distinct !{!336, !337, !"_ZN4core6option15Option$LT$T$GT$7is_none17h9e79d6edc127467aE: %self"}
!337 = distinct !{!337, !"_ZN4core6option15Option$LT$T$GT$7is_none17h9e79d6edc127467aE"}
!338 = distinct !{!338, !339, !"_ZN6memchr6memmem8Searcher15prefilter_state17h550081e1923b1222E: %self"}
!339 = distinct !{!339, !"_ZN6memchr6memmem8Searcher15prefilter_state17h550081e1923b1222E"}
!340 = !{!341}
!341 = distinct !{!341, !342, !"_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE: %self"}
!342 = distinct !{!342, !"_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE"}
!343 = !{!344}
!344 = distinct !{!344, !342, !"_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE: %haystack.0"}
!345 = !{!346, !348, !350, !341}
!346 = distinct !{!346, !347, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE: %self"}
!347 = distinct !{!347, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE"}
!348 = distinct !{!348, !349, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE: %self"}
!349 = distinct !{!349, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE"}
!350 = distinct !{!350, !351, !"_ZN6memchr6memmem8Searcher6needle17hf8fce88406e6e39aE: %self"}
!351 = distinct !{!351, !"_ZN6memchr6memmem8Searcher6needle17hf8fce88406e6e39aE"}
!352 = !{!353, !344}
!353 = distinct !{!353, !342, !"_ZN6memchr6memmem8Searcher4find17h092f6099760cb5baE: %state"}
!354 = !{!355, !357, !359, !361, !344}
!355 = distinct !{!355, !356, !"_ZN6memchr6memchr3x864sse26memchr17h8c22de531a37fcc8E: %haystack.0"}
!356 = distinct !{!356, !"_ZN6memchr6memchr3x864sse26memchr17h8c22de531a37fcc8E"}
!357 = distinct !{!357, !358, !"_ZN6memchr6memchr3x866memchr17h10c4f2e2b688bc94E: %haystack.0"}
!358 = distinct !{!358, !"_ZN6memchr6memchr3x866memchr17h10c4f2e2b688bc94E"}
!359 = distinct !{!359, !360, !"_ZN6memchr6memchr6memchr3imp17heab8178553c66ae4E: %haystack.0"}
!360 = distinct !{!360, !"_ZN6memchr6memchr6memchr3imp17heab8178553c66ae4E"}
!361 = distinct !{!361, !362, !"_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE: %haystack.0"}
!362 = distinct !{!362, !"_ZN6memchr6memchr6memchr17hb5cbb5940758b1ddE"}
!363 = !{!341, !353}
!364 = !{!365, !367, !341}
!365 = distinct !{!365, !366, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %fwd"}
!366 = distinct !{!366, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E"}
!367 = distinct !{!367, !368, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %self"}
!368 = distinct !{!368, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E"}
!369 = !{!370, !371, !372, !373, !353, !344}
!370 = distinct !{!370, !366, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %haystack.0"}
!371 = distinct !{!371, !366, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %needle.0"}
!372 = distinct !{!372, !368, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %haystack.0"}
!373 = distinct !{!373, !368, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %needle.0"}
!374 = !{!375}
!375 = distinct !{!375, !376, !"_ZN6memchr6memmem3x863sse7Forward4find17h59c5c470db40e2beE: %haystack.0"}
!376 = distinct !{!376, !"_ZN6memchr6memmem3x863sse7Forward4find17h59c5c470db40e2beE"}
!377 = !{!378}
!378 = distinct !{!378, !376, !"_ZN6memchr6memmem3x863sse7Forward4find17h59c5c470db40e2beE: %needle.0"}
!379 = !{!380}
!380 = distinct !{!380, !381, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %haystack.0"}
!381 = distinct !{!381, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E"}
!382 = !{!383}
!383 = distinct !{!383, !381, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %needle.0"}
!384 = !{!385}
!385 = distinct !{!385, !386, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %haystack.0"}
!386 = distinct !{!386, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E"}
!387 = !{!388}
!388 = distinct !{!388, !386, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %needle.0"}
!389 = !{!388, !383, !378}
!390 = !{!391, !385, !392, !380, !375, !341, !353}
!391 = distinct !{!391, !386, !"_ZN6memchr6memmem11genericsimd8fwd_find17hb4b5982a139f0605E: %fwd"}
!392 = distinct !{!392, !381, !"_ZN6memchr6memmem3x863sse7Forward9find_impl17h984c13dab3a13699E: %self"}
!393 = !{!391, !388, !392, !383, !378, !341, !353}
!394 = !{!395}
!395 = distinct !{!395, !396, !"_ZN6memchr6memmem11genericsimd17fwd_find_in_chunk17h977e1fcff57f72e2E: %needle.0"}
!396 = distinct !{!396, !"_ZN6memchr6memmem11genericsimd17fwd_find_in_chunk17h977e1fcff57f72e2E"}
!397 = !{!385, !380, !375, !344}
!398 = !{!395, !391, !388, !392, !383, !378, !341, !353}
!399 = !{!400}
!400 = distinct !{!400, !401, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %x.0"}
!401 = distinct !{!401, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E"}
!402 = !{!403}
!403 = distinct !{!403, !401, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %y.0"}
!404 = !{!400, !395, !388, !383, !378}
!405 = !{!403, !391, !385, !392, !380, !375, !341, !353}
!406 = !{!403, !385, !380, !375, !344}
!407 = !{!400, !395, !391, !388, !392, !383, !378, !341, !353}
!408 = !{!409}
!409 = distinct !{!409, !410, !"_ZN6memchr6memmem11genericsimd17fwd_find_in_chunk17h977e1fcff57f72e2E: %needle.0"}
!410 = distinct !{!410, !"_ZN6memchr6memmem11genericsimd17fwd_find_in_chunk17h977e1fcff57f72e2E"}
!411 = !{!409, !391, !388, !392, !383, !378, !341, !353}
!412 = !{!413}
!413 = distinct !{!413, !414, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %x.0"}
!414 = distinct !{!414, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E"}
!415 = !{!416}
!416 = distinct !{!416, !414, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %y.0"}
!417 = !{!413, !409, !388, !383, !378}
!418 = !{!416, !391, !385, !392, !380, !375, !341, !353}
!419 = !{!416, !385, !380, !375, !344}
!420 = !{!413, !409, !391, !388, !392, !383, !378, !341, !353}
!421 = !{!422, !424, !344}
!422 = distinct !{!422, !423, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E: %bytes.0"}
!423 = distinct !{!423, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E"}
!424 = distinct !{!424, !425, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E: argument 0"}
!425 = distinct !{!425, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E"}
!426 = !{!427, !341, !353}
!427 = distinct !{!427, !425, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E: %needle.0"}
!428 = distinct !{!428, !98}
!429 = !{!424, !344}
!430 = !{!431, !433, !344}
!431 = distinct !{!431, !432, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E: %bytes.0"}
!432 = distinct !{!432, !"_ZN6memchr6memmem9rabinkarp4Hash14from_bytes_fwd17hfb8b31b5e4d5cd45E"}
!433 = distinct !{!433, !434, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E: argument 0"}
!434 = distinct !{!434, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E"}
!435 = !{!436, !341, !353}
!436 = distinct !{!436, !434, !"_ZN6memchr6memmem9rabinkarp9find_with17hef1e79e3bbe26ad8E: %needle.0"}
!437 = distinct !{!437, !98}
!438 = !{!433, !344}
!439 = !{!440}
!440 = distinct !{!440, !441, !"_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E: %needle.0"}
!441 = distinct !{!441, !"_ZN6memchr6memmem10NeedleInfo3new17h51dd1f0d69ce2719E"}
!442 = !{!443}
!443 = distinct !{!443, !444, !"_ZN6memchr6memmem9rarebytes15RareNeedleBytes7forward17h554d0b979d1378b1E: %needle.0"}
!444 = distinct !{!444, !"_ZN6memchr6memmem9rarebytes15RareNeedleBytes7forward17h554d0b979d1378b1E"}
!445 = !{!443, !440}
!446 = !{!447, !440}
!447 = distinct !{!447, !448, !"_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE: %needle.0"}
!448 = distinct !{!448, !"_ZN6memchr6memmem9rabinkarp10NeedleHash7forward17h79c39d2e8dce590bE"}
!449 = !{!450, !452}
!450 = distinct !{!450, !451, !"_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E: %needle.0"}
!451 = distinct !{!451, !"_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E"}
!452 = distinct !{!452, !453, !"_ZN6memchr6memmem6twoway7Forward3new17hac57d8d75b47ae34E: %needle.0"}
!453 = distinct !{!453, !"_ZN6memchr6memmem6twoway7Forward3new17hac57d8d75b47ae34E"}
!454 = !{!455}
!455 = distinct !{!455, !453, !"_ZN6memchr6memmem6twoway7Forward3new17hac57d8d75b47ae34E: argument 0"}
!456 = distinct !{!456, !98}
!457 = !{!458}
!458 = distinct !{!458, !459, !"_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E: %needle.0"}
!459 = distinct !{!459, !"_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E"}
!460 = !{!458, !455}
!461 = !{!458, !452}
!462 = !{!463}
!463 = distinct !{!463, !464, !"_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E: %needle.0"}
!464 = distinct !{!464, !"_ZN6memchr6memmem6twoway6Suffix7forward17h5fc375f39230cbb8E"}
!465 = !{!463, !452}
!466 = !{!463, !455}
!467 = !{!468}
!468 = distinct !{!468, !469, !"_ZN6memchr6memmem6twoway5Shift7forward17h984e46fafa92339dE: %needle.0"}
!469 = distinct !{!469, !"_ZN6memchr6memmem6twoway5Shift7forward17h984e46fafa92339dE"}
!470 = !{!471, !473, !468, !455}
!471 = distinct !{!471, !472, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E: argument 0"}
!472 = distinct !{!472, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E"}
!473 = distinct !{!473, !472, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E: %self.0"}
!474 = !{!475, !477, !479, !468, !455}
!475 = distinct !{!475, !476, !"_ZN106_$LT$core..ops..range..Range$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17h0a7a5ee63b824680E: %slice.0"}
!476 = distinct !{!476, !"_ZN106_$LT$core..ops..range..Range$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17h0a7a5ee63b824680E"}
!477 = distinct !{!477, !478, !"_ZN108_$LT$core..ops..range..RangeTo$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17h02d8c8385a557d52E: %slice.0"}
!478 = distinct !{!478, !"_ZN108_$LT$core..ops..range..RangeTo$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17h02d8c8385a557d52E"}
!479 = distinct !{!479, !480, !"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE: %self.0"}
!480 = distinct !{!480, !"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h7a33252506d3c20cE"}
!481 = !{!482}
!482 = distinct !{!482, !483, !"_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E: %haystack.0"}
!483 = distinct !{!483, !"_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E"}
!484 = !{!485}
!485 = distinct !{!485, !483, !"_ZN6memchr6memmem4util9is_suffix17h4029e7e038422ce0E: %needle.0"}
!486 = !{!487}
!487 = distinct !{!487, !488, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %x.0"}
!488 = distinct !{!488, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E"}
!489 = !{!490}
!490 = distinct !{!490, !488, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %y.0"}
!491 = !{!487, !482, !468, !452}
!492 = !{!490, !485, !455}
!493 = !{!490, !485, !468, !452}
!494 = !{!487, !482, !455}
!495 = !{!496, !498}
!496 = distinct !{!496, !497, !"_ZN4core6option15Option$LT$T$GT$7is_some17hff66e2e4f2d461a6E: %self"}
!497 = distinct !{!497, !"_ZN4core6option15Option$LT$T$GT$7is_some17hff66e2e4f2d461a6E"}
!498 = distinct !{!498, !499, !"_ZN4core6option15Option$LT$T$GT$7is_none17h9e79d6edc127467aE: %self"}
!499 = distinct !{!499, !"_ZN4core6option15Option$LT$T$GT$7is_none17h9e79d6edc127467aE"}
!500 = !{!501, !503}
!501 = distinct !{!501, !502, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE: %self"}
!502 = distinct !{!502, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE"}
!503 = distinct !{!503, !504, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE: %self"}
!504 = distinct !{!504, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE"}
!505 = !{!506, !508, !510}
!506 = distinct !{!506, !507, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE: %self"}
!507 = distinct !{!507, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE"}
!508 = distinct !{!508, !509, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE: %self"}
!509 = distinct !{!509, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE"}
!510 = distinct !{!510, !511, !"_ZN6memchr6memmem8Searcher6needle17hf8fce88406e6e39aE: %self"}
!511 = distinct !{!511, !"_ZN6memchr6memmem8Searcher6needle17hf8fce88406e6e39aE"}
!512 = !{!513}
!513 = distinct !{!513, !514, !"_ZN6memchr6memmem9prefilter14PrefilterState12is_effective17h5bd9a514c7cb5407E: %self"}
!514 = distinct !{!514, !"_ZN6memchr6memmem9prefilter14PrefilterState12is_effective17h5bd9a514c7cb5407E"}
!515 = !{!516}
!516 = distinct !{!516, !517, !"_ZN6memchr6memmem6twoway7Forward4find17hb43424f52d69ceb9E: %self"}
!517 = distinct !{!517, !"_ZN6memchr6memmem6twoway7Forward4find17hb43424f52d69ceb9E"}
!518 = !{!519}
!519 = distinct !{!519, !517, !"_ZN6memchr6memmem6twoway7Forward4find17hb43424f52d69ceb9E: %haystack.0"}
!520 = !{!521}
!521 = distinct !{!521, !517, !"_ZN6memchr6memmem6twoway7Forward4find17hb43424f52d69ceb9E: %needle.0"}
!522 = !{!523, !519, !521}
!523 = distinct !{!523, !517, !"_ZN6memchr6memmem6twoway7Forward4find17hb43424f52d69ceb9E: %pre"}
!524 = !{!525}
!525 = distinct !{!525, !526, !"_ZN6memchr6memmem6twoway7Forward14find_small_imp17h64b88bbfe7e1ab47E: %haystack.0"}
!526 = distinct !{!526, !"_ZN6memchr6memmem6twoway7Forward14find_small_imp17h64b88bbfe7e1ab47E"}
!527 = !{!528}
!528 = distinct !{!528, !526, !"_ZN6memchr6memmem6twoway7Forward14find_small_imp17h64b88bbfe7e1ab47E: %needle.0"}
!529 = !{!525, !519}
!530 = !{!531, !528, !516, !523, !521}
!531 = distinct !{!531, !526, !"_ZN6memchr6memmem6twoway7Forward14find_small_imp17h64b88bbfe7e1ab47E: argument 0"}
!532 = !{!531, !516, !523}
!533 = !{!528, !521}
!534 = !{!531, !525, !516, !523, !519}
!535 = !{!536}
!536 = distinct !{!536, !537, !"_ZN6memchr6memmem6twoway7Forward14find_large_imp17h51d3c5038c89a070E: %haystack.0"}
!537 = distinct !{!537, !"_ZN6memchr6memmem6twoway7Forward14find_large_imp17h51d3c5038c89a070E"}
!538 = !{!539}
!539 = distinct !{!539, !537, !"_ZN6memchr6memmem6twoway7Forward14find_large_imp17h51d3c5038c89a070E: %needle.0"}
!540 = !{!536, !519}
!541 = !{!542, !539, !516, !523, !521}
!542 = distinct !{!542, !537, !"_ZN6memchr6memmem6twoway7Forward14find_large_imp17h51d3c5038c89a070E: argument 0"}
!543 = !{!539, !521}
!544 = !{!542, !536, !516, !523, !519}
!545 = !{!542, !516, !523}
!546 = !{!547}
!547 = distinct !{!547, !548, !"_ZN6memchr6memmem6twoway7Forward4find17hb43424f52d69ceb9E: %self"}
!548 = distinct !{!548, !"_ZN6memchr6memmem6twoway7Forward4find17hb43424f52d69ceb9E"}
!549 = !{!550}
!550 = distinct !{!550, !548, !"_ZN6memchr6memmem6twoway7Forward4find17hb43424f52d69ceb9E: %haystack.0"}
!551 = !{!552}
!552 = distinct !{!552, !548, !"_ZN6memchr6memmem6twoway7Forward4find17hb43424f52d69ceb9E: %needle.0"}
!553 = !{!554, !550, !552}
!554 = distinct !{!554, !548, !"_ZN6memchr6memmem6twoway7Forward4find17hb43424f52d69ceb9E: %pre"}
!555 = !{!556}
!556 = distinct !{!556, !557, !"_ZN6memchr6memmem6twoway7Forward14find_small_imp17h64b88bbfe7e1ab47E: %haystack.0"}
!557 = distinct !{!557, !"_ZN6memchr6memmem6twoway7Forward14find_small_imp17h64b88bbfe7e1ab47E"}
!558 = !{!559}
!559 = distinct !{!559, !557, !"_ZN6memchr6memmem6twoway7Forward14find_small_imp17h64b88bbfe7e1ab47E: %needle.0"}
!560 = !{!561}
!561 = distinct !{!561, !562, !"_ZN6memchr6memmem9prefilter14PrefilterState12is_effective17h5bd9a514c7cb5407E: %self"}
!562 = distinct !{!562, !"_ZN6memchr6memmem9prefilter14PrefilterState12is_effective17h5bd9a514c7cb5407E"}
!563 = !{!564, !547, !554}
!564 = distinct !{!564, !557, !"_ZN6memchr6memmem6twoway7Forward14find_small_imp17h64b88bbfe7e1ab47E: argument 0"}
!565 = !{!566, !568, !564, !547, !554}
!566 = distinct !{!566, !567, !"_ZN110_$LT$core..ops..range..RangeFrom$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17hba98ca6c65580fafE: %slice.0"}
!567 = distinct !{!567, !"_ZN110_$LT$core..ops..range..RangeFrom$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17hba98ca6c65580fafE"}
!568 = distinct !{!568, !569, !"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE: %self.0"}
!569 = distinct !{!569, !"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE"}
!570 = !{!556, !550}
!571 = !{!564, !559, !547, !554, !552}
!572 = !{!559, !552}
!573 = !{!564, !556, !547, !554, !550}
!574 = !{!575}
!575 = distinct !{!575, !576, !"_ZN6memchr6memmem6twoway7Forward14find_large_imp17h51d3c5038c89a070E: %haystack.0"}
!576 = distinct !{!576, !"_ZN6memchr6memmem6twoway7Forward14find_large_imp17h51d3c5038c89a070E"}
!577 = !{!578}
!578 = distinct !{!578, !576, !"_ZN6memchr6memmem6twoway7Forward14find_large_imp17h51d3c5038c89a070E: %needle.0"}
!579 = !{!580}
!580 = distinct !{!580, !581, !"_ZN6memchr6memmem9prefilter14PrefilterState12is_effective17h5bd9a514c7cb5407E: %self"}
!581 = distinct !{!581, !"_ZN6memchr6memmem9prefilter14PrefilterState12is_effective17h5bd9a514c7cb5407E"}
!582 = !{!583, !547, !554}
!583 = distinct !{!583, !576, !"_ZN6memchr6memmem6twoway7Forward14find_large_imp17h51d3c5038c89a070E: argument 0"}
!584 = !{!585, !587, !583, !547, !554}
!585 = distinct !{!585, !586, !"_ZN110_$LT$core..ops..range..RangeFrom$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17hba98ca6c65580fafE: %slice.0"}
!586 = distinct !{!586, !"_ZN110_$LT$core..ops..range..RangeFrom$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17hba98ca6c65580fafE"}
!587 = distinct !{!587, !588, !"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE: %self.0"}
!588 = distinct !{!588, !"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE"}
!589 = !{!575, !550}
!590 = !{!583, !578, !547, !554, !552}
!591 = !{!578, !552}
!592 = !{!583, !575, !547, !554, !550}
!593 = distinct !{!593, !594}
!594 = !{!"llvm.loop.unswitch.partial.disable"}
!595 = !{!596, !598}
!596 = distinct !{!596, !597, !"_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E: %needle.0"}
!597 = distinct !{!597, !"_ZN6memchr6memmem6twoway18ApproximateByteSet3new17h31ce0d380f4b7a89E"}
!598 = distinct !{!598, !599, !"_ZN6memchr6memmem6twoway7Reverse3new17hd563358c23206888E: %needle.0"}
!599 = distinct !{!599, !"_ZN6memchr6memmem6twoway7Reverse3new17hd563358c23206888E"}
!600 = !{!601}
!601 = distinct !{!601, !599, !"_ZN6memchr6memmem6twoway7Reverse3new17hd563358c23206888E: argument 0"}
!602 = distinct !{!602, !98}
!603 = !{!604}
!604 = distinct !{!604, !605, !"_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E: %needle.0"}
!605 = distinct !{!605, !"_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E"}
!606 = !{!604, !601}
!607 = !{!604, !598}
!608 = !{!609}
!609 = distinct !{!609, !610, !"_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E: %needle.0"}
!610 = distinct !{!610, !"_ZN6memchr6memmem6twoway6Suffix7reverse17h9c1c6a28ec5361e6E"}
!611 = !{!609, !598}
!612 = !{!609, !601}
!613 = !{!614}
!614 = distinct !{!614, !615, !"_ZN6memchr6memmem6twoway5Shift7reverse17h56cad5fc860292a2E: %needle.0"}
!615 = distinct !{!615, !"_ZN6memchr6memmem6twoway5Shift7reverse17h56cad5fc860292a2E"}
!616 = !{!617, !619, !614, !601}
!617 = distinct !{!617, !618, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E: argument 0"}
!618 = distinct !{!618, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E"}
!619 = distinct !{!619, !618, !"_ZN4core5slice29_$LT$impl$u20$$u5b$T$u5d$$GT$8split_at17h496cd17636cebc03E: %self.0"}
!620 = !{!621, !623, !614, !601}
!621 = distinct !{!621, !622, !"_ZN110_$LT$core..ops..range..RangeFrom$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17hba98ca6c65580fafE: %slice.0"}
!622 = distinct !{!622, !"_ZN110_$LT$core..ops..range..RangeFrom$LT$usize$GT$$u20$as$u20$core..slice..index..SliceIndex$LT$$u5b$T$u5d$$GT$$GT$5index17hba98ca6c65580fafE"}
!623 = distinct !{!623, !624, !"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE: %self.0"}
!624 = distinct !{!624, !"_ZN4core5slice5index74_$LT$impl$u20$core..ops..index..Index$LT$I$GT$$u20$for$u20$$u5b$T$u5d$$GT$5index17h0e07b64f0105e7faE"}
!625 = !{!626}
!626 = distinct !{!626, !627, !"_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE: %haystack.0"}
!627 = distinct !{!627, !"_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE"}
!628 = !{!629}
!629 = distinct !{!629, !627, !"_ZN6memchr6memmem4util9is_prefix17h828bde7971b5188eE: %needle.0"}
!630 = !{!631}
!631 = distinct !{!631, !632, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %x.0"}
!632 = distinct !{!632, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E"}
!633 = !{!634}
!634 = distinct !{!634, !632, !"_ZN6memchr6memmem4util6memcmp17h219f2d59ca832512E: %y.0"}
!635 = !{!631, !626, !614, !598}
!636 = !{!634, !629, !601}
!637 = !{!634, !629, !614, !598}
!638 = !{!631, !626, !601}
!639 = !{!640}
!640 = distinct !{!640, !641, !"_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E: %needle.0"}
!641 = distinct !{!641, !"_ZN6memchr6memmem9rabinkarp10NeedleHash7reverse17h674d934cae15d5c5E"}
!642 = !{!643, !645}
!643 = distinct !{!643, !644, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE: %self"}
!644 = distinct !{!644, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE"}
!645 = distinct !{!645, !646, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE: %self"}
!646 = distinct !{!646, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE"}
!647 = !{!648, !650, !652}
!648 = distinct !{!648, !649, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE: %self"}
!649 = distinct !{!649, !"_ZN6memchr3cow3Imp8as_slice17h23121079bb889e3aE"}
!650 = distinct !{!650, !651, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE: %self"}
!651 = distinct !{!651, !"_ZN6memchr3cow8CowBytes8as_slice17hf67765e2f827b2edE"}
!652 = distinct !{!652, !653, !"_ZN6memchr6memmem11SearcherRev6needle17hdba161fcdb320d48E: %self"}
!653 = distinct !{!653, !"_ZN6memchr6memmem11SearcherRev6needle17hdba161fcdb320d48E"}
!654 = !{i8 0, i8 2}
!655 = !{!656}
!656 = distinct !{!656, !657, !"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17h5d46b5326924fe2fE: %self"}
!657 = distinct !{!657, !"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17h5d46b5326924fe2fE"}
!658 = !{!659}
!659 = distinct !{!659, !657, !"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17h5d46b5326924fe2fE: %f"}
!660 = !{!656, !659}
!661 = !{!662}
!662 = distinct !{!662, !663, !"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u32$GT$3fmt17ha7f34dbae66c4ee1E: %self"}
!663 = distinct !{!663, !"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u32$GT$3fmt17ha7f34dbae66c4ee1E"}
!664 = !{!665, !667}
!665 = distinct !{!665, !666, !"_ZN68_$LT$memchr..memmem..rabinkarp..Hash$u20$as$u20$core..fmt..Debug$GT$3fmt17h8ddc4f31b9cb2177E: %self"}
!666 = distinct !{!666, !"_ZN68_$LT$memchr..memmem..rabinkarp..Hash$u20$as$u20$core..fmt..Debug$GT$3fmt17h8ddc4f31b9cb2177E"}
!667 = distinct !{!667, !666, !"_ZN68_$LT$memchr..memmem..rabinkarp..Hash$u20$as$u20$core..fmt..Debug$GT$3fmt17h8ddc4f31b9cb2177E: %f"}
!668 = !{!665}
!669 = !{!670, !672}
!670 = distinct !{!670, !671, !"_ZN70_$LT$memchr..memmem..x86..sse..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17ha16b0a29596cc67cE: %self"}
!671 = distinct !{!671, !"_ZN70_$LT$memchr..memmem..x86..sse..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17ha16b0a29596cc67cE"}
!672 = distinct !{!672, !671, !"_ZN70_$LT$memchr..memmem..x86..sse..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17ha16b0a29596cc67cE: %f"}
!673 = !{!670}
!674 = !{!675, !677}
!675 = distinct !{!675, !676, !"_ZN73_$LT$memchr..memmem..genericsimd..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17heb362d32e8c2db5eE: %self"}
!676 = distinct !{!676, !"_ZN73_$LT$memchr..memmem..genericsimd..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17heb362d32e8c2db5eE"}
!677 = distinct !{!677, !676, !"_ZN73_$LT$memchr..memmem..genericsimd..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17heb362d32e8c2db5eE: %f"}
!678 = !{!675}
!679 = !{!680, !682}
!680 = distinct !{!680, !681, !"_ZN63_$LT$memchr..memmem..NeedleInfo$u20$as$u20$core..fmt..Debug$GT$3fmt17hf8ecbadd15609784E: %self"}
!681 = distinct !{!681, !"_ZN63_$LT$memchr..memmem..NeedleInfo$u20$as$u20$core..fmt..Debug$GT$3fmt17hf8ecbadd15609784E"}
!682 = distinct !{!682, !681, !"_ZN63_$LT$memchr..memmem..NeedleInfo$u20$as$u20$core..fmt..Debug$GT$3fmt17hf8ecbadd15609784E: %f"}
!683 = !{!680}
!684 = !{!685, !687}
!685 = distinct !{!685, !686, !"_ZN67_$LT$memchr..memmem..twoway..TwoWay$u20$as$u20$core..fmt..Debug$GT$3fmt17h8a05b576142eef61E: %self"}
!686 = distinct !{!686, !"_ZN67_$LT$memchr..memmem..twoway..TwoWay$u20$as$u20$core..fmt..Debug$GT$3fmt17h8a05b576142eef61E"}
!687 = distinct !{!687, !686, !"_ZN67_$LT$memchr..memmem..twoway..TwoWay$u20$as$u20$core..fmt..Debug$GT$3fmt17h8a05b576142eef61E: %f"}
!688 = !{!685}
!689 = !{!690, !692}
!690 = distinct !{!690, !691, !"_ZN77_$LT$memchr..memmem..x86..avx..nostd..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17hd9b746a528f9898fE: %self"}
!691 = distinct !{!691, !"_ZN77_$LT$memchr..memmem..x86..avx..nostd..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17hd9b746a528f9898fE"}
!692 = distinct !{!692, !691, !"_ZN77_$LT$memchr..memmem..x86..avx..nostd..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17hd9b746a528f9898fE: %f"}
!693 = !{!690}
!694 = !{!695}
!695 = distinct !{!695, !696, !"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u64$GT$3fmt17h772c2a78b67c4edcE: %self"}
!696 = distinct !{!696, !"_ZN4core3fmt3num50_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u64$GT$3fmt17h772c2a78b67c4edcE"}
!697 = !{!698, !700, !701}
!698 = distinct !{!698, !699, !"_ZN48_$LT$$u5b$T$u5d$$u20$as$u20$core..fmt..Debug$GT$3fmt17h9694dbd8d561cfe5E: %self.0"}
!699 = distinct !{!699, !"_ZN48_$LT$$u5b$T$u5d$$u20$as$u20$core..fmt..Debug$GT$3fmt17h9694dbd8d561cfe5E"}
!700 = distinct !{!700, !699, !"_ZN48_$LT$$u5b$T$u5d$$u20$as$u20$core..fmt..Debug$GT$3fmt17h9694dbd8d561cfe5E: %f"}
!701 = distinct !{!701, !702, !"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hb05d48d2b33ecd4aE: %f"}
!702 = distinct !{!702, !"_ZN42_$LT$$RF$T$u20$as$u20$core..fmt..Debug$GT$3fmt17hb05d48d2b33ecd4aE"}
!703 = !{!698}
!704 = !{!705, !698, !700, !701}
!705 = distinct !{!705, !706, !"_ZN4core3fmt8builders9DebugList7entries17hd20c6ca75c107e02E: %self"}
!706 = distinct !{!706, !"_ZN4core3fmt8builders9DebugList7entries17hd20c6ca75c107e02E"}
!707 = !{!708}
!708 = distinct !{!708, !709, !"_ZN73_$LT$memchr..memmem..prefilter..Prefilter$u20$as$u20$core..fmt..Debug$GT$3fmt17h17201383e0067fa1E: %self"}
!709 = distinct !{!709, !"_ZN73_$LT$memchr..memmem..prefilter..Prefilter$u20$as$u20$core..fmt..Debug$GT$3fmt17h17201383e0067fa1E"}
!710 = !{!711}
!711 = distinct !{!711, !709, !"_ZN73_$LT$memchr..memmem..prefilter..Prefilter$u20$as$u20$core..fmt..Debug$GT$3fmt17h17201383e0067fa1E: %f"}
!712 = !{!713, !715}
!713 = distinct !{!713, !714, !"_ZN74_$LT$memchr..memmem..rabinkarp..NeedleHash$u20$as$u20$core..fmt..Debug$GT$3fmt17h1fd51ca258f7a7d4E: %self"}
!714 = distinct !{!714, !"_ZN74_$LT$memchr..memmem..rabinkarp..NeedleHash$u20$as$u20$core..fmt..Debug$GT$3fmt17h1fd51ca258f7a7d4E"}
!715 = distinct !{!715, !714, !"_ZN74_$LT$memchr..memmem..rabinkarp..NeedleHash$u20$as$u20$core..fmt..Debug$GT$3fmt17h1fd51ca258f7a7d4E: %f"}
!716 = !{!713}
!717 = !{!718, !720}
!718 = distinct !{!718, !719, !"_ZN68_$LT$memchr..memmem..twoway..Reverse$u20$as$u20$core..fmt..Debug$GT$3fmt17hf32879c849379d46E: %self"}
!719 = distinct !{!719, !"_ZN68_$LT$memchr..memmem..twoway..Reverse$u20$as$u20$core..fmt..Debug$GT$3fmt17hf32879c849379d46E"}
!720 = distinct !{!720, !719, !"_ZN68_$LT$memchr..memmem..twoway..Reverse$u20$as$u20$core..fmt..Debug$GT$3fmt17hf32879c849379d46E: %f"}
!721 = !{!718}
!722 = !{!723, !725}
!723 = distinct !{!723, !724, !"_ZN68_$LT$memchr..memmem..twoway..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17h58ed7f655d1667fdE: %self"}
!724 = distinct !{!724, !"_ZN68_$LT$memchr..memmem..twoway..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17h58ed7f655d1667fdE"}
!725 = distinct !{!725, !724, !"_ZN68_$LT$memchr..memmem..twoway..Forward$u20$as$u20$core..fmt..Debug$GT$3fmt17h58ed7f655d1667fdE: %f"}
!726 = !{!723}
!727 = !{!728, !730}
!728 = distinct !{!728, !729, !"_ZN59_$LT$memchr..memmem..Finder$u20$as$u20$core..fmt..Debug$GT$3fmt17h5a00105a2ee5d6bcE: %self"}
!729 = distinct !{!729, !"_ZN59_$LT$memchr..memmem..Finder$u20$as$u20$core..fmt..Debug$GT$3fmt17h5a00105a2ee5d6bcE"}
!730 = distinct !{!730, !729, !"_ZN59_$LT$memchr..memmem..Finder$u20$as$u20$core..fmt..Debug$GT$3fmt17h5a00105a2ee5d6bcE: %f"}
!731 = !{!728}
!732 = !{!733}
!733 = distinct !{!733, !734, !"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hd3e67c4facafb664E: %self"}
!734 = distinct !{!734, !"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hd3e67c4facafb664E"}
!735 = !{!736}
!736 = distinct !{!736, !734, !"_ZN66_$LT$core..option..Option$LT$T$GT$$u20$as$u20$core..fmt..Debug$GT$3fmt17hd3e67c4facafb664E: %f"}
!737 = !{!733, !736}
!738 = !{!739, !741}
!739 = distinct !{!739, !740, !"_ZN78_$LT$memchr..memmem..prefilter..PrefilterState$u20$as$u20$core..fmt..Debug$GT$3fmt17hc5db09aace33ebabE: %self"}
!740 = distinct !{!740, !"_ZN78_$LT$memchr..memmem..prefilter..PrefilterState$u20$as$u20$core..fmt..Debug$GT$3fmt17hc5db09aace33ebabE"}
!741 = distinct !{!741, !740, !"_ZN78_$LT$memchr..memmem..prefilter..PrefilterState$u20$as$u20$core..fmt..Debug$GT$3fmt17hc5db09aace33ebabE: %f"}
!742 = !{!739}
!743 = !{!744, !746}
!744 = distinct !{!744, !745, !"_ZN61_$LT$memchr..memmem..Searcher$u20$as$u20$core..fmt..Debug$GT$3fmt17h77d29106b5bc0a9dE: %self"}
!745 = distinct !{!745, !"_ZN61_$LT$memchr..memmem..Searcher$u20$as$u20$core..fmt..Debug$GT$3fmt17h77d29106b5bc0a9dE"}
!746 = distinct !{!746, !745, !"_ZN61_$LT$memchr..memmem..Searcher$u20$as$u20$core..fmt..Debug$GT$3fmt17h77d29106b5bc0a9dE: %f"}
!747 = !{!744}
!748 = !{!749, !751}
!749 = distinct !{!749, !750, !"_ZN79_$LT$memchr..memmem..rarebytes..RareNeedleBytes$u20$as$u20$core..fmt..Debug$GT$3fmt17h7fb6c00f79c486bcE: %self"}
!750 = distinct !{!750, !"_ZN79_$LT$memchr..memmem..rarebytes..RareNeedleBytes$u20$as$u20$core..fmt..Debug$GT$3fmt17h7fb6c00f79c486bcE"}
!751 = distinct !{!751, !750, !"_ZN79_$LT$memchr..memmem..rarebytes..RareNeedleBytes$u20$as$u20$core..fmt..Debug$GT$3fmt17h7fb6c00f79c486bcE: %f"}
!752 = !{!749}
!753 = !{!754, !756}
!754 = distinct !{!754, !755, !"_ZN62_$LT$memchr..memmem..FinderRev$u20$as$u20$core..fmt..Debug$GT$3fmt17h18ca465f2bbe8b28E: %self"}
!755 = distinct !{!755, !"_ZN62_$LT$memchr..memmem..FinderRev$u20$as$u20$core..fmt..Debug$GT$3fmt17h18ca465f2bbe8b28E"}
!756 = distinct !{!756, !755, !"_ZN62_$LT$memchr..memmem..FinderRev$u20$as$u20$core..fmt..Debug$GT$3fmt17h18ca465f2bbe8b28E: %f"}
!757 = !{!754}
!758 = !{!759, !761}
!759 = distinct !{!759, !760, !"_ZN79_$LT$memchr..memmem..twoway..ApproximateByteSet$u20$as$u20$core..fmt..Debug$GT$3fmt17hc87ca094ca876d46E: %self"}
!760 = distinct !{!760, !"_ZN79_$LT$memchr..memmem..twoway..ApproximateByteSet$u20$as$u20$core..fmt..Debug$GT$3fmt17hc87ca094ca876d46E"}
!761 = distinct !{!761, !760, !"_ZN79_$LT$memchr..memmem..twoway..ApproximateByteSet$u20$as$u20$core..fmt..Debug$GT$3fmt17hc87ca094ca876d46E: %f"}
!762 = !{!759}
!763 = !{!764, !766}
!764 = distinct !{!764, !765, !"_ZN67_$LT$memchr..memmem..SearcherConfig$u20$as$u20$core..fmt..Debug$GT$3fmt17h95222fe3e975a71aE: %self"}
!765 = distinct !{!765, !"_ZN67_$LT$memchr..memmem..SearcherConfig$u20$as$u20$core..fmt..Debug$GT$3fmt17h95222fe3e975a71aE"}
!766 = distinct !{!766, !765, !"_ZN67_$LT$memchr..memmem..SearcherConfig$u20$as$u20$core..fmt..Debug$GT$3fmt17h95222fe3e975a71aE: %f"}
!767 = !{!764}
!768 = !{!769, !771}
!769 = distinct !{!769, !770, !"_ZN64_$LT$memchr..memmem..SearcherRev$u20$as$u20$core..fmt..Debug$GT$3fmt17h9c6507df7bf765f6E: %self"}
!770 = distinct !{!770, !"_ZN64_$LT$memchr..memmem..SearcherRev$u20$as$u20$core..fmt..Debug$GT$3fmt17h9c6507df7bf765f6E"}
!771 = distinct !{!771, !770, !"_ZN64_$LT$memchr..memmem..SearcherRev$u20$as$u20$core..fmt..Debug$GT$3fmt17h9c6507df7bf765f6E: %f"}
!772 = !{!769}
!773 = !{!774, !776}
!774 = distinct !{!774, !775, !"_ZN53_$LT$memchr..cow..Imp$u20$as$u20$core..fmt..Debug$GT$3fmt17h6a7ac8c510544f61E: %self"}
!775 = distinct !{!775, !"_ZN53_$LT$memchr..cow..Imp$u20$as$u20$core..fmt..Debug$GT$3fmt17h6a7ac8c510544f61E"}
!776 = distinct !{!776, !775, !"_ZN53_$LT$memchr..cow..Imp$u20$as$u20$core..fmt..Debug$GT$3fmt17h6a7ac8c510544f61E: %f"}
!777 = !{!774}
!778 = !{!779}
!779 = distinct !{!779, !780, !"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..Debug$u20$for$u20$usize$GT$3fmt17h06e7e77e2b0b2d99E: %self"}
!780 = distinct !{!780, !"_ZN4core3fmt3num52_$LT$impl$u20$core..fmt..Debug$u20$for$u20$usize$GT$3fmt17h06e7e77e2b0b2d99E"}
!781 = !{!782}
!782 = distinct !{!782, !783, !"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E: %self"}
!783 = distinct !{!783, !"_ZN4core3fmt3num49_$LT$impl$u20$core..fmt..Debug$u20$for$u20$u8$GT$3fmt17h6c0c914d23f30da0E"}
!784 = !{!785, !787}
!785 = distinct !{!785, !786, !"_ZN58_$LT$memchr..cow..CowBytes$u20$as$u20$core..fmt..Debug$GT$3fmt17hb91ccca4ba3204d1E: %self"}
!786 = distinct !{!786, !"_ZN58_$LT$memchr..cow..CowBytes$u20$as$u20$core..fmt..Debug$GT$3fmt17hb91ccca4ba3204d1E"}
!787 = distinct !{!787, !786, !"_ZN58_$LT$memchr..cow..CowBytes$u20$as$u20$core..fmt..Debug$GT$3fmt17hb91ccca4ba3204d1E: %f"}
!788 = !{!785}
