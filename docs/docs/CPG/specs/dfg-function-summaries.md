# Specification: Data Flow Graph - Function Summaries

For functions and methods which are part of the analyzed codebase, the CPG can track data flows interprocedurally to some extent.
However, for all functions and methods which cannot be analyzed, we have no information available.
For this case, we provide the user a way to specify custom summaries of the data flows through the function.
To do so, you need to fill a JSON file as follows:

* The outer element is a list/array
* In this list, you add elements, each of which summarizes the flows for one function/method
* The element consists of two objects: The `functionDeclaration` and the `dataFlows`
* The `functionDeclaration` consists of:
  * `language`: The FQN of the `Language` element which this function is relevant for.
  * `methodName`: The FQN of the function or method. We use this one to identify the relevant function/method. Do not forget to add the class name and use the separators as specified by the `Language`.
  * `signature` (*optional*): This optional element allows us to differentiate between overloaded functions (i.e., two functions have the same FQN but accept different arguments). If no `signature` is specified, it matches to any function/method with the name you specified. The `signature` is a list of FQNs of the types (as strings)
* The `dataFlows` element is a list of objects with the following elements:
  * `from`: A description of the start-node of a DFG-edge. Valid options:
    * `paramX`: where `X` is the offset (we start counting with 0)
    * `base`: the receiver of the method (i.e., the object the method is called on)
  * `to`: A description of the end-node of the DFG-edge. Valid options:
    * `paramX` where `X` is the offset (we start counting with 0)
    * `base` the receiver of the method (i.e., the object the method is called on)
    * `return` the return value of the function
    * `returnX` where `X` is a number and specifies the index of the return value (if multiple values are returned).
  * `dfgType`: Here, you can give more information. Currently, this is unused but should later allow us to add the properties to the edge.

An example of a file could look as follows:
```json
[
  {
    "functionDeclaration": {
      "language": "de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage",
      "methodName": "java.util.List.addAll",
      "signature": ["int", "java.util.Object"]
    },
    "dataFlows": [
      {
        "from": "param1",
        "to": "base",
        "dfgType": "full"
      }
    ]
  },
  {
    "functionDeclaration": {
      "language": "de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage",
      "methodName": "java.util.List.addAll",
      "signature": ["java.util.Object"]
    },
    "dataFlows": [
      {
        "from": "param0",
        "to": "base",
        "dfgType": "full"
      }
    ]
  },
  {
    "functionDeclaration": {
      "language": "de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage",
      "methodName": "memcpy"
    },
    "dataFlows": [
      {
        "from": "param1",
        "to": "param0",
        "dfgType": "full"
      }
    ]
  }
]
```
This file configures the following edges:
* For a method declaration in Java `java.util.List.addAll(int, java.util.Object)`, the parameter 1 flows to the base (i.e., the list object)
* For a method declaration in Java `java.util.List.addAll(java.util.Object)`, the parameter 0 flows to the base (i.e., the list object)
* For a function declaration in C `memcpy` (and thus also CXX `std::memcpy`), the parameter 1 flows to parameter 0.