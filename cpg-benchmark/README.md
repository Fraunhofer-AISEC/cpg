# Performance Evaluation for Incremental CPG Construction

This tool compares the performance of creating a CPG from scratch with incremental construction based on Git commits.
The repository to be used for evaluation can either be remote (e.g. `--remote https://github.com/Fraunhofer-AISEC/cpg.git`)
or local (e.g. `--local /tmp/cpg`). To analyze the 50 newest commits, use `-n 50` or `--commits 50`.
If file output is desired, e.g. for automated result parsing, use the `--output` option.
In order to ensure the equivalence of the conventional and incremental graph at each commit,
use the `--verify-equivalence` flag.