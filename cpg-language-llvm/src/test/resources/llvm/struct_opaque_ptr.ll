; Same as struct.ll, but written using LLVM's opaque pointer syntax (`ptr`) instead of the
; typed-pointer syntax (`i32*`, `%struct.ST*`, ...). This makes sure the frontend can resolve
; getelementptr chains purely from the instruction's explicit source element type, since opaque
; pointers no longer carry any pointee type information themselves.
;
; struct RT {
;   char A;
;   int B[10][20];
;   char C;
; };
; struct ST {
;   int X;
;   double Y;
;   struct RT Z;
; };
;
; int *foo(struct ST *s) {
;   return &s[1].Z.B[5][13];
; }

%struct.RT = type { i8, [10 x [20 x i32]], i8 }
%struct.ST = type { i32, double, %struct.RT }

define ptr @foo(ptr %s) nounwind uwtable readnone optsize ssp {
  %arrayidx = getelementptr inbounds %struct.ST, ptr %s, i64 1, i32 2, i32 1, i64 5, i64 13
  ret ptr %arrayidx
}
