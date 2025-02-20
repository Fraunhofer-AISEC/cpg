# Design Principles

## The CPG represents the code's ...

* Structure/Syntax
* Data Flows
* Execution Order/Control Flow
* Variable Usage
* Calls
* The Type System

## The CPG should parse ...

* Incomplete code
* Code with missing toolchains
* With resilience to incorrect code
* Language heterogeneous projects


## CPG-Library users should be able to ...

* Load projects and single files
* Visualize and analyze code
* Implement and register new Language Frontends
* Extends and modify existing components, e.g., passes
* Parse code incrementally

## The CPG-Transformation should be ...
* Language independent: Allow for language independent and cross-language queries
* Information-rich: contain language-specific information in generalized structures
* Fast (enough).
    * Small Projects/Development projects should be analyzable in real-time, at most some seconds.
    * Large libraries should take no longer than a few hours.
    * About 5 to 10 times as long as the compilation process.
