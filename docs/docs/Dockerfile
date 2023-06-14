FROM ghcr.io/afritzler/mkdocs-material

# Ensure installation of required packages
# - uses updated cache on-the-fly without storing it locally
RUN apk --no-cache add \
    git

# Install additional plugins (cf. `./mkdocs-material-plugins.txt`)
COPY mkdocs-material-plugins.txt /
RUN python -m pip install --no-cache-dir -r /mkdocs-material-plugins.txt

# Trust git directory for git revision plugin
RUN git config --global --add safe.directory /docs
