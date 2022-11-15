# CPG Console

From the project root folder
```bash
./gradlew :cpg-console:installDist
cpg-console/build/install/cpg-console/bin/cpg-console
```

From this folder
```bash
../gradlew installDist
build/install/cpg-console/bin/cpg-console
```

The following example snippet can be used:

```kotlin
:tr src/test/resources/array.cpp
var main = tu.byName<FunctionDeclaration>("main")
:code main?
var decl = main?.body<DeclarationStatement>(0)
var v = decl?.singleDeclaration as? VariableDeclaration
```
