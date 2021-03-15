from java.lang import System
from de.fraunhofer.aisec.cpg.graph.declarations import TranslationUnitDeclaration
from de.fraunhofer.aisec.cpg.graph.declarations import Declaration

import ast


d = Declaration()
d.setFile("123")
x = TranslationUnitDeclaration()
x.addDeclaration(d)
res = x

System.out.println(codeToParse)
x = ast.dump(ast.parse(codeToParse), indent=4)
System.out.println(x)
