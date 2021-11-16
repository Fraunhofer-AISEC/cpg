declare void @_CxxThrowException(i8*, %eh.ThrowInfo*)
declare i32 @__CxxFrameHandler3(...)
declare void @throwingFoo(i32)
declare void @cleanO(i32*)
%eh.ThrowInfo = type { i32, i32, i32, i32 }


define void @f() #0 personality i8* bitcast (i32 (...)* @__CxxFrameHandler3 to i8*) {
entry:
  invoke void @_CxxThrowException(i8* null, %eh.ThrowInfo* null) #1
          to label %unreachable unwind label %catch.dispatch

catch.dispatch:                                   ; preds = %entry
  %0 = catchswitch within none [label %catch] unwind to caller

catch:                                            ; preds = %catch.dispatch
  %1 = catchpad within %0 [i8* null, i32 64, i8* null]
  invoke void @_CxxThrowException(i8* null, %eh.ThrowInfo* null) #1
          to label %unreachable unwind label %catch.dispatch2

catch.dispatch2:                                  ; preds = %catch
  %2 = catchswitch within %1 [label %catch3] unwind to caller

catch3:                                           ; preds = %catch.dispatch2
  %3 = catchpad within %2 [i8* null, i32 64, i8* null]
  catchret from %3 to label %try.cont

try.cont:                                         ; preds = %catch3
  catchret from %1 to label %try.cont6

try.cont6:                                        ; preds = %try.cont
  ret void

unreachable:                                      ; preds = %catch, %entry
  unreachable
}

define i32 @g() nounwind personality i32 (...)* @__CxxFrameHandler3 {
entry:
  %o = alloca i32, align 8
  invoke void @throwingFoo(i32 10)
          to label %cont unwind label %clean

cont:                                         ; preds = %entry
  call void @cleanO(i32* %o)
  ret i32 5

clean:                                        ; preds = %entry
  %0 = cleanuppad within none []
  call void @cleanO(i32* %o)
  cleanupret from %0 unwind to caller
}