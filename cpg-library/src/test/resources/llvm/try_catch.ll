declare i32 @throwingFoo() nounwind

; Definition of main function
define i32 @main() {   ; i32()*
  %res = invoke i32 @throwingFoo()
          to label %continueTry unwind label %catchBB

continueTry:
  %a = add i32 %res, 6
  ret i32 %a

catchBB:
  %1 = landingpad { i8*, i32 }
          cleanup
  ret i32 0
}