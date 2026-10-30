; A getelementptr constant expression (as opposed to a getelementptr instruction) used in a
; global initializer. Checks that LLVMGetGEPSourceElementType also works on a ConstantExpr, not
; just on an instruction.

%struct.ST = type { i32, i32 }

@g = global %struct.ST { i32 1, i32 2 }
@gp = global ptr getelementptr (%struct.ST, ptr @g, i64 0, i32 1)
