declare i32 @rand() nounwind

define i32 @main() {   ; i32()*
  %x = call i32 @rand()
  %cond = icmp eq i32 %x, 10
  br i1 %cond, label %IfEqual, label %IfUnequal

IfEqual:
  %a = mul i32 %x, 32768
  br label %continue

IfUnequal:
  %b = add i32 %x, 7
  br label %continue

continue:
  %y = phi i32 [ %a, %IfEqual ], [ %b, %IfUnequal ]
  ret i32 %y
}

; This code does the following:
; int main() {
;   i32 x = rand()
;    int y;
;   if(x == 1) {
;     int a = x * 32768
;     y = a
;   } else {
;     int b = x + 7
;     y = b
;   }
;   return y
; }