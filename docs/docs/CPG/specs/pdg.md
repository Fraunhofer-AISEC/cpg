# Specification: Program Dependence Graph

The [Program Dependence Graph (PDG)](https://dl.acm.org/doi/10.1145/24039.24041)
is a graph which spans both, the data dependencies and the control dependencies
inside the program. This is interesting since it allows to determine which nodes
have some kind of effect on another node, let it be due to a (direct) data flow
or because they have an impact on the execution of an edge or the potential
value. It thus presents a good way to perform program slicing and has
traditionally been used in program optimization, among others.

## The PDG and implicit dataflows

In particular, the PDG is also suitable to identify potential implicit data
flows. Consider the following example:

```java
import javax.crypto.*;
class Main {
    public static void main(String[] args) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256); // for example
            SecretKey secretKey = keyGen.generateKey();
            boolean b;
            if(secretKey.getEncoded()[0] == 3) {
                b = true;
            } else {
                b = false;
            }
            System.out.println(b);
        } catch(Exception e) {
            // We don't care
        }
    }
}
```

If you want to know if the key is printed by the program, then you will follow
the DFG and you won't find a direct dataflow between the call to `generateKey`
and the call to `println`.

However, if you're wondering if there's some kind of leakage of information
about the key, then you will see that the value of the first byte has an effect
on the value of the variable `b` which is printed. This can be interpreted as a
data breach and requires you to follow both, the CDG and the DFG or, more
conveniently, the PDG.

This feature can easily be used through the
[Query API](../../GettingStarted/query.md) and
[Shortcuts](../../GettingStarted/shortcuts.md) by using adding the sensitivity
`Implicit`.

As an example, we receive an empty list for when running the following query
traversing only the DFG:

```kotlin
val dfgOnly =
    key.followDFGEdgesUntilHit(
        findAllPossiblePaths = true,
        direction = Forward(GraphToFollow.DFG),
        sensitivities = FieldSensitive + ContextSensitive
    ) {
        (it as? Call)?.name?.localName == "println"
    }
println(dfgOnly.fulfilled)
```

In contrast, we do find the paths between the two nodes when paths when
running the same query with `Imlicit` specifying

```kotlin
val pdg =
    key.followDFGEdgesUntilHit(
        findAllPossiblePaths = true,
        direction = Forward(GraphToFollow.DFG),
        sensitivities = FieldSensitive + ContextSensitive + Implicit
    ) {
        (it as? Call)?.name?.localName == "println"
    }
println(pdg.fulfilled)
```

!!! warning "Configuration"

    Retrieving the PDG requires to register the two passes
    `ControlDependenceGraphPass` and `ProgramDependenceGraphPass`
    which are currently not in the list of default passes in the
    `TranslationConfiguration`.
