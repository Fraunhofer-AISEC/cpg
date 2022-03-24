declare i32 @rand() nounwind

define i32 @loopPhi() {
LoopHeader:
  %a = call i32 @rand()
  br label %Loop

Loop:       ; Loop that counts from 0 <= i < 10
  %indvar = phi i32 [ 0, %LoopHeader ], [ %nextindvar, %Loop ]
  %nextindvar = add i32 %indvar, 1
  %cond = icmp eq i32 %nextindvar, 10
  br i1 %cond, label %End, label %Loop

End:
  ret i32 %a
}