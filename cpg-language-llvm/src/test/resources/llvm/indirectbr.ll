define i32 @foo(i32* %addr) nounwind uwtable readnone optsize ssp {
    indirectbr i32* %addr, [ label %bb1, label %bb2 ]

bb1:
    ret i32 1

bb2:
    ret i32 2
}