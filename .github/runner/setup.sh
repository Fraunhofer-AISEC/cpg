#!/usr/bin/env bash
# Provisions a fresh Ubuntu 24.04 arm64 VM as a self-hosted GitHub Actions runner for this
# repository. Run it as the admin user on the VM (see README.md). It does NOT register the runner,
# because that needs a short-lived token which must not be stored anywhere; see README.md.
set -euo pipefail

RUNNER_DIR="${RUNNER_DIR:-$HOME/actions-runner1}"
TOOLCACHE="${TOOLCACHE:-/opt/hostedtoolcache}"
SWAP_SIZE="${SWAP_SIZE:-8G}"

export DEBIAN_FRONTEND=noninteractive
sudo apt-get update -qq
# build-essential, python3-dev and libpython3-dev are needed to compile jep; docker is used by the
# neo4j tests; sysstat gives us iostat/sar for debugging slow builds.
sudo apt-get install -y -qq build-essential git curl jq zip unzip docker.io sysstat \
  python3-venv python3-dev libpython3-dev
sudo usermod -aG docker "$USER"

# Swap as a safety net against the OOM killer during parallel Gradle/Kotlin/test JVMs
if ! swapon --show | grep -q swapfile; then
  sudo fallocate -l "$SWAP_SIZE" /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

# The setup-* actions install into the tool cache, and e.g. Python bakes its location into the
# build. Putting it where it would be on a GitHub-hosted runner avoids broken library paths when
# building jep.
sudo mkdir -p "$TOOLCACHE"
sudo chown "$USER" "$TOOLCACHE"

# Download (but do not configure) the latest runner
version="$(curl -fsSL https://api.github.com/repos/actions/runner/releases/latest | jq -r .tag_name | sed 's/^v//')"
mkdir -p "$RUNNER_DIR"
cd "$RUNNER_DIR"
curl -fsSL "https://github.com/actions/runner/releases/download/v${version}/actions-runner-linux-arm64-${version}.tar.gz" | tar xz
echo "AGENT_TOOLSDIRECTORY=$TOOLCACHE" > .env

echo "Runner $version downloaded to $RUNNER_DIR. Now register it, see .github/runner/README.md"
