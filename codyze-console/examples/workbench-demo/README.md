# Workbench Demo

A small WiFi application in C to try the features of the console on the agent page. Start it with the
run configuration **Codyze Compliance Scan (with Console and Workbench Demo)** (or
`compliance scan --project-dir codyze-console/examples/workbench-demo --components wifi_demo --console=true`)
and open <http://localhost:8080/chat>.

The frontend for C has to be enabled (`enableCXXFrontend=true` in `gradle.properties`).

## A model for the agent

The agent needs an LLM provider. Copy `cpg-ai/src/main/resources/application.conf.example` to
`application.conf` (next to it) and keep one client, e.g. a local model with
[Ollama](https://ollama.com):

```
llm { clients { ollama { baseUrl = "http://localhost:11434" } } }
```

and `ollama pull qwen2.5:7b` (or another model with tool calling). Models of this size follow
simple questions; for the citations of nodes and longer chains of tool calls a larger model is more
reliable. Everything except the agent (code, layers, PDG, ...) works without a model.

## What is in the code

| File | What it is for |
|---|---|
| `main.c` | `app_main`: branches, a loop (`send_with_retries`), a `switch` (`describe`), the key flowing into `encrypt`, `copy_plain`, the log (`report_key`) and the network |
| `config.c/.h` | A struct with a function pointer (`on_connect`), validation with early returns |
| `crypto.c/.h` | `encrypt` and `copy_plain` with loops |
| `log.c/.h` | `log_debug` and `log_error`, which call the undeclared `printf` |
| `net.c/.h` | `net_open` and `net_send`, which call the undeclared `socket` and `send` |
| `tagging.codyze.kts` | Concepts for `get_key` (secret) and `encrypt` (cipher) |
| `project.codyze.kts` | The project, two requirements, and the dependence graph passes |

Calls to functions that are not declared anywhere (`get_key`, `printf`, `socket`, `send`) are
**external**, the call through the function pointer `cfg.on_connect(key)` is **unresolved**.

## What to try

**File tree, tabs, quick open, command palette**
- Open files in the tree, open several (tabs), go back and forward with `Alt+←` / `Alt+→`.
- `Ctrl/Cmd+P` opens a file by name, `Ctrl/Cmd+Shift+P` shows all commands.

**Code, layers and the code lens above `app_main`**
- The layers *Concepts*, *External*, *Uncertain* show the calls of the analysis in the code and in the
  ruler. `get_key`/`encrypt` have concepts, `get_key`, `send`, ... are external, `cfg.on_connect` is
  uncertain.
- The code lens above a function (`calls 8 · 3 external · ⚠ 1 unresolved`) lists only the external or
  unresolved calls in the inspector when clicked.
- Hover over a name for the hover card, click it for the inspector.

**Outline and breadcrumb**
- Outline (second icon of the activity bar) with the functions and their figures; the breadcrumb names
  the function you are in.

**Dataflow**
- Click `key` in `app_main` → inspector → *Where does the value go?* → follow a node to show the path
  `get_key → encrypt / copy_plain → report_key → log_debug` (arcs and steps in the code).

**Dependence graph (PDG)**
- Select `net_send(sock, buf, len)` in `send_with_retries`, then the *PDG* chip or right click →
  *PDG: What affects this?* / *What does this affect?*. Try the hops, `+N` at the border, double click,
  pinning, *Show what this affects →*, and clicking lines in the code.
- `encrypt(key, buf, len)` has a control dependence on `if (cfg.secure)` (true/false edges).
- `send`/`socket` calls show external stubs.

**Right click in the code**
- Menu with the PDG slices (and their sizes), callers/callees and *Ask agent about this…*.

**Ask on selection**
- Select code and use the field next to it (or `Ctrl/Cmd+K`). Needs an LLM provider in
  `application.conf` (see `cpg-ai/src/main/resources/application.conf.example`).

**Agent, evidence and trust**
- Questions to try: *Is the key ever written to the log?* (yes, through `report_key` when `debug` is
  set), *Which calls could not be resolved?*, *Where is the key encrypted?*
- The answer cites nodes as links; the steps mark the code with numbers; red notes under the answer
  name unresolved or external calls the evidence relies on, and nodes cited that no tool returned.

**Requirements**
- *Keys are encrypted* should hold, *All calls are resolved* fails on purpose (open the QueryTree).
