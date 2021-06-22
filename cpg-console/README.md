# CPG Console

```bash
../gradlew installDist
build/install/cpg-console/bin/cpg-console
```

The following example snippet can be used:

```kotlin
:a src/test/resources/array.cpp
var tu = result.translationUnits.first()
var main = tu.byName<FunctionDeclaration>("main")
:code main?
var decl = main?.body<DeclarationStatement>(0)
var v = decl?.singleDeclaration as? VariableDeclaration
```
