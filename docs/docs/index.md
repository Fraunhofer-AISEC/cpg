---
title: "Home"
linkTitle: "Home"
weight: 20
no_list: false
menu:
  main:
    weight: 20
hide:
  - navigation
description: >
    The CPG library is a language-agnostic graph representation of source code.
---

## What does the CPG library offer?

<img src="assets/img/graph.svg" style="float:right;width:40%;" />

A Code Property Graph (CPG) is a graph-based representation of code which unites
several concepts such as an Abstract Syntax Tree (AST), Control Flow Graph
(CFG) or Evaluation Order Graph (EOG), Data Flow Graph (DFG) or Control Dependence
Graph (CDG), among others, in a single supergraph. This is beneficial because
the CPG contains the most relevant information to conduct static program
analysis and yet, the graph provides a certain abstraction of the respective
programming language.


<p class="empty-small" />

<div style="display:grid;grid-template-columns: auto auto auto;" markdown>

 <div class="box 1" markdown>
  <h3>Supported Languages</h3>

  ---

  The library supports the following programming languages out of the box:

  * Java (Source code, JVM bytecode and Jimple)
  * C/C++
  * Go
  * Python
  * TypeScript
  * LLVM-IR
  * Ruby

  Nothing suitable found? [Write your own language frontend](./CPG/impl/language.md)
  for the respective language.
  </div>

  <div class="box 2" markdown>
  <h3>Built-in Analyses</h3>

  ---

  The library currently provides different analyses:
  
  * Dataflow Analysis
  * Reachability Analysis
  * Constant Propagation
  * Intraprocedural Order Evaluation of Statements
  </div>

  <div class="box 3" markdown>
  <h3>Accessing the Graph</h3>
  
  ---
  
  The library can be used by analysts or tools in different ways:

  * The graph can be exported to the graph database [neo4j](https://neo4j.com)
  * The CPG can be included into every project as a library
  * [Codyze](./GettingStarted/codyze.md), a tool checking for compliance of your software artifacs against certain requirements and visualizing the graph and the results in a webconsole
  * We provide an API for querying the graph for interesting properties
  </div>

  <div class="box 4" markdown>
  <h3>Highly Extensible</h3>
  
  ---
  
  The library is easily extensible. You can add new...

  * language frontends <font size="2">[Tell me more about it!](./CPG/impl/language.md)</font>,
  * passes <font size="2">[Tell me more about it!](./CPG/impl/passes.md)</font> or
  * analyses.
  </div>

  <div class="box 5" markdown>
  <h3>Handling Incomplete Code</h3>
  
  ---
  
  The code you have to analyze is missing dependencies, is under active development and might
  miss some code fragments?
  <br>
  No problem! Our tooling provides a certain resilience against such problems.
   <!--This allows our toolchain to analyze programs even if the current code base is incomplete and incorrect to a certain extent.-->
  </div>

  <div class="box 6" markdown>
  </div>

</div>

<p class="empty"/>

<div class="float-container" markdown>
<div class="left-picture">
<img src="assets/img/Institut-AISEC-Gebaeude-Nacht.jpg" />
</div>
<div class="left-picture-text" markdown>

## About Us

We're a team of researchers at <a href="https://www.aisec.fraunhofer.de/">Fraunhofer AISEC</a>.
We're interested in different topics in the area of static program analysis. If
you're interested in our work, feel free to reach out to us - we're happy to
collaborate and push the boundaries of static code analysis.
</div>
</div>

<p class="empty" style="border-top: 1px solid #c7cacc;" />


## Publications

### 2024

<div class="papers">

<div class="admonition paper">
  <p class="admonition-title">Analyzing the Impact of Copying-and-Pasting Vulnerable Solidity Code Snippets from Question-and-Answer Websites</p>
  <div class="left">
    <p class="authors">Konrad Weiss, Christof Ferreira Torres, Florian Wendland</p>
    <p class="conference">In: ACM Internet Measurement Conference (IMC). Madrid, Spain.</p>
    <details>
      <summary>bibtex</summary>
      <pre><code>@inproceedings{weiss2024solidity,
  author={Weiss, Konrad and Ferreira Torres, Christof and Wendland, Florian},
  title={Analyzing the Impact of Copying-and-Pasting Vulnerable Solidity Code Snippets from Question-and-Answer Websites},
  year={2024},
  booktitle={Proceedings of the 2024 ACM on Internet Measurement Conference},
  series={IMC '24},
  doi = {10.1145/3646547.3688437},
  location = {Madrid, Spain},
  publisher={ACM}
}</code></pre>
    </details>
  </div>
  <div class="right">
    <a class="green-button" href="https://doi.org/10.1145/3646547.3688437">paper</a>
  </div>
</div>

</div>

### 2023

<div class="papers">

<div class="admonition paper">
  <p class="admonition-title">A Uniform Representation of Classical and Quantum Source Code for Static Code Analysis</p>
  <div class="left">
    <p class="authors">Maximilian Kaul, Alexander Küchler, Christian Banse</p>
    <p class="conference">In: IEEE International Conference on Quantum Computing and Engineering (QCE). Bellevue, WA, USA.</p>
    <details>
      <summary>bibtex</summary>
      <pre><code>@inproceedings{kaul2023qcpg,
  author={Maximilian Kaul and Alexander K\"uchler and Christian Banse},
  title={A Uniform Representation of Classical and Quantum Source Code for Static Code Analysis},
  year={2023},
  booktitle={2023 IEEE International Conference on Quantum Computing and Engineering},
  series={QCE '23},
  doi={10.1109/QCE57702.2023.00115},
  location={Bellevue, WA, USA},
  publisher={IEEE}
}</code></pre>
    </details>
  </div>
  <div class="right">
    <a class="green-button" href="https://arxiv.org/pdf/2308.06113.pdf">preprint</a><br />
    <a class="green-button" href="https://doi.org/10.1109/QCE57702.2023.00115">paper</a>
  </div>
</div>

<div class="admonition paper">
  <p class="admonition-title">AbsIntIO: Towards Showing the Absence of Integer Overflows in Binaries using Abstract Interpretation</p>
  <div class="left">
    <p class="authors">Alexander Küchler, Leon Wenning, Florian Wendland</p>
    <p class="conference">In: ACM ASIA Conference on Computer and Communications Security (Asia CCS). Melbourne, VIC, Australia.</p>
    <details>
      <summary>bibtex</summary>
      <pre><code>@inproceedings{kuechler2023absintio,
  author={Alexander K\"uchler and Leon Wenning and Florian Wendland},
  title={AbsIntIO: Towards Showing the Absence of Integer Overflows in Binaries using Abstract Interpretation},
  year={2023},
  booktitle={ACM ASIA Conference on Computer and Communications Security},
  series={Asia CCS '23},
  doi={10.1145/3579856.3582814},
  location={Melbourne, VIC, Australia},
  publisher={ACM}
}</code></pre>
    </details>
  </div>
  <div class="right">
    <a class="green-button" href="https://doi.org/10.1145/3579856.3582814">paper</a>
  </div>
</div>

</div>

### 2022

<div class="papers">

<div class="admonition paper">
  <p class="admonition-title">Representing LLVM-IR in a Code Property Graph</p>
  <div class="left">
    <p class="authors">Alexander Küchler, Christian Banse</p>
    <p class="conference">In: 25th Information Security Conference (ISC). Bali, Indonesia.</p>
    <details>
      <summary>bibtex</summary>
      <pre><code>@inproceedings{kuechler2022representing,
  author={Alexander K\"uchler and Christian Banse},
  title={Representing LLVM-IR in a Code Property Graph},
  year={2022},
  booktitle={25th Information Security Conference},
  series={ISC},
  doi={10.1007/978-3-031-22390-7\_21},
  location={Bali, Indonesia},
  publisher={Springer}
}</code></pre>
    </details>
  </div>
  <div class="right">
    <a class="green-button" href="https://arxiv.org/pdf/2211.05627.pdf">preprint</a><br />
    <a class="green-button" href="https://link.springer.com/chapter/10.1007/978-3-031-22390-7_21">paper</a>
  </div>
</div>

<div class="admonition paper">
  <p class="admonition-title">A Language-Independent Analysis Platform for Source Code</p>
  <div class="left">
    <p class="authors">Konrad Weiss, Christian Banse</p>
    <details>
      <summary>bibtex</summary>
      <pre><code>@misc{weiss2022a,
  doi = {10.48550/ARXIV.2203.08424},
  url = {https://arxiv.org/abs/2203.08424},
  author = {Weiss, Konrad and Banse, Christian},
  title = {A Language-Independent Analysis Platform for Source Code},
  publisher = {arXiv},
  year = {2022},
}</code></pre>
    </details>
  </div>
  <div class="right">
    <a class="green-button" href="https://arxiv.org/pdf/2203.08424.pdf">paper</a>
  </div>
</div>

</div>

### 2021

<div class="papers" style=" border-bottom: 1px solid #c7cacc;">

<div class="admonition paper">
  <p class="admonition-title">Cloud Property Graph: Connecting Cloud Security Assessments with Static Code Analysis</p>
  <div class="left">
    <p class="authors">Christian Banse, Immanuel Kunz, Angelika Schneider, Konrad Weiss</p>
    <p class="conference">In: 2021 IEEE 14th International Conference on Cloud Computing (CLOUD). Los Alamitos, CA, USA</p>
    <details>
      <summary>bibtex</summary>
      <pre><code>@inproceedings{banse2021cloudpg,
  author = {Christian Banse and Immanuel Kunz and Angelika Schneider and Konrad Weiss},
  booktitle = {2021 IEEE 14th International Conference on Cloud Computing (CLOUD)},
  title = {Cloud Property Graph: Connecting Cloud Security Assessments with Static Code Analysis},
  year = {2021},
  pages = {13-19},
  doi = {10.1109/CLOUD53861.2021.00014},
  url = {https://doi.ieeecomputersociety.org/10.1109/CLOUD53861.2021.00014},
  publisher = {IEEE Computer Society},
  address = {Los Alamitos, CA, USA},
  month = {sep}
}</code></pre>
    </details>
  </div>
  <div class="right">
    <a class="green-button" href="https://arxiv.org/pdf/2206.06938.pdf">preprint</a><br />
    <a class="green-button" href="https://www.computer.org/csdl/proceedings-article/cloud/2021/006000a013/1ymJ7POIlxe">paper</a>
  </div>
</div>

</div>

