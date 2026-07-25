# Codyze Console Webapp

This frontend uses SvelteKit and **pnpm**.

## Install dependencies

From this directory, run:

```bash
pnpm install
```

Build scripts are explicitly allowed via `pnpm-workspace.yaml` so CI and local installs can run required postinstall hooks (e.g., `esbuild`).

## Developing

```bash
pnpm run dev

# or open the app automatically
pnpm run dev -- --open
```

## Building

```bash
pnpm run build
```

## Preview

```bash
pnpm run preview
```
