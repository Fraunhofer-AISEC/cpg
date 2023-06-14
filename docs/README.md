# CPG Documentation

The documentation for CPG is built with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) and hosted with GitHub Pages.

Simply use the provided [Dockerfiles](./Dockerfile) in this directory. 
It includes all the necessary plugins. 

To build the Docker image use:
```shell
docker build -t mkdocs-material .
```
Afterwards, you can start a local development server:
```shell
docker run --rm -it -p 8000:8000 -v ${PWD}:/docs mkdocs-material
```

Please note, that the `git-revision-date-localized` plugin does not work with git worktrees.