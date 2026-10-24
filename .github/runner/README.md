# Self-hosted CI runner

The `build` workflow runs on `[self-hosted, Linux, ARM64]`. This describes how to (re-)create such
a runner on Azure. Nothing here contains credentials; the runner registration token has to be
created by hand each time.

## Sizing

Measured on this build: 8 vCPUs are used well, and one build needs about 10 GiB of RAM (Gradle
daemon, Kotlin daemon, parallel test JVMs, the neo4j container), so 16 GiB is too tight.

| Size | vCPU / RAM | Notes | ~€/month (24/7, Germany West Central) |
|---|---|---|---|
| `Standard_D8ps_v6` | 8 / 32 GiB | Cobalt 100, managed OS disk. Recommended | 215 + disk |
| `Standard_D8plds_v6` | 8 / 16 GiB | Local NVMe, memory is the limit | 240 |

Avoid the Ampere `Dps_v5` sizes (slower and more expensive than the `v6`) and burstable `B` sizes
(CPU throttling). Prices change, check the Azure pricing calculator.

## 1. Create the VM

Reuses the existing resource group, subnet and network security group. Adjust the variables if they
change. Azure only accepts RSA and Ed25519 SSH keys, not ECDSA.

```bash
RG=SAS-CI
NAME=sas-cpg-ci-v2
SUB=/subscriptions/<subscription-id>/resourceGroups/sas-ci/providers/Microsoft.Network

az vm create -g "$RG" -n "$NAME" -l germanywestcentral \
  --size Standard_D8ps_v6 \
  --image canonical:ubuntu-24_04-lts:server-arm64:latest \
  --security-type TrustedLaunch --enable-secure-boot true --enable-vtpm true \
  --os-disk-size-gb 128 --storage-sku Premium_LRS \
  --admin-username azureuser --authentication-type ssh --ssh-key-values ~/.ssh/id_ed25519.pub \
  --subnet "$SUB/virtualNetworks/vnet-germanywestcentral-1/subnets/snet-germanywestcentral-1" \
  --nsg "$SUB/networkSecurityGroups/sas-cpg-ci-nsg" \
  --public-ip-sku Standard --public-ip-address "$NAME-ip" \
  --accelerated-networking true --assign-identity '[system]' \
  --tags purpose=github-runner

# Allows `az ssh vm` with Entra ID
az vm extension set -g "$RG" --vm-name "$NAME" -n AADSSHLoginForLinux \
  --publisher Microsoft.Azure.ActiveDirectory
```

## 2. Provision it

```bash
IP=$(az vm show -d -g "$RG" -n "$NAME" --query publicIps -o tsv)
az ssh vm --ip "$IP" --local-user azureuser --private-key-file ~/.ssh/id_ed25519 \
  -- -o StrictHostKeyChecking=accept-new 'bash -s' < .github/runner/setup.sh
```

This installs the build dependencies and Docker, adds swap, creates the tool cache at
`/opt/hostedtoolcache` (see below) and downloads the runner.

## 3. Register the runner

The runners are registered at **organization** level. Create a registration token in the
organization settings under *Actions > Runners > New self-hosted runner* (this needs org admin),
then on the VM:

```bash
cd ~/actions-runner1
./config.sh --url https://github.com/Fraunhofer-AISEC --token <TOKEN> \
  --name sas-cpg-ci-1 --labels self-hosted,Linux,ARM64 --unattended
sudo ./svc.sh install && sudo ./svc.sh start
```

To replace an existing runner of the same name, remove it first (`./config.sh remove --token
<REMOVE_TOKEN>` on the old VM, or in the runner's settings page). The token and the runner's
`.credentials` files are secrets and must not be copied between machines.

Run **one** runner per VM. Two concurrent builds on 8 vCPUs compete for CPU and memory and are
slower than running them one after the other.

## Moving an existing runner to a new VM

Instead of registering a new runner, you can move one, as long as the old one is **stopped for
good** (never run both with the same identity):

1. On the old VM: `sudo ./svc.sh stop` (and wait for running jobs to finish).
2. Copy `.runner`, `.credentials` and `.credentials_rsaparams` (secrets!) from the old to the new
   runner directory, e.g. `tar c ... | tar x` through `az ssh vm`. Keep the new `.env`.
3. `config.sh` never ran on the new VM, so `svc.sh` does not exist. Create the service by hand:
   `cp bin/runsvc.sh runsvc.sh`, write a systemd unit running `runsvc.sh` as the runner user (see
   `/etc/systemd/system/actions.runner.*.service` on any runner for a template), then
   `sudo systemctl enable --now <unit>`.
4. Deallocate or delete the old VM.

## Why `AGENT_TOOLSDIRECTORY`

`actions/setup-python` builds are relocated and still reference `/opt/hostedtoolcache`. If the tool
cache lives elsewhere (the default is `_work/_tool` in the runner directory), compiling `jep`
fails with `cannot find -lpython3.14`. `setup.sh` therefore creates `/opt/hostedtoolcache` and
writes `AGENT_TOOLSDIRECTORY` into the runner's `.env`.

## Debugging a slow build

```bash
free -h; vmstat 1 5      # swap use (si/so) and iowait (wa)
iostat -x 2              # disk saturation
```
