declare i32 @throwingFoo() nounwind
; target specific
declare i8* @__cxa_begin_catch(i8*) nounwind
; llvm intrinsics
declare i32 @llvm.eh.typeid.for(i8*)

@_ZTIi = external constant i8* ; represents an integer
@_ZTId = external constant i8* ; represents a double

; Definition of main function
define i32 @main() {   ; i32()*
  %res = invoke i32 @throwingFoo()
          to label %continueTry unwind label %catchBB

continueTry:
  %a = add i32 %res, 6
  ret i32 %a

catchBB:
  %info = landingpad { i8*, i32 }
          catch i8** @_ZTIi
          catch i8** null
  %except = extractvalue { i8*, i32 } %info, 0
  %selector = extractvalue { i8*, i32 } %info, 1
  %inttype = call i32 @llvm.eh.typeid.for(i8* bitcast (i8** @_ZTIi to i8*)) nounwind
  %isint = icmp eq i32 %selector, %inttype
  br i1 %isint, label %int_catch, label %next_catch

int_catch:
  %thrown = call i8* @__cxa_begin_catch(i8* %except)
  %tmp = bitcast i8* %thrown to i32*
  %i = load i32, i32* %tmp
  ret i32 %i

next_catch:
  ret i32 0
}

; This sample does the following:
; try {
;   int res = throwingFoo();
;   int a = res + 6;
;   return a;
; } catch(int i) {
;    return i;
; } catch(...) {
;    return 0;
; }