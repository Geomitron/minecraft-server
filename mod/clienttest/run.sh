#!/bin/bash
# Runs one set of in-game checks in a Linux container: the dev server and two dev clients on a virtual screen.
#
#   clienttest/run.sh <checks> [out-dir]
#
# <checks> is a script in clienttest/ (step6.sh, say). The container's results (checks.log, the game logs and the
# screenshots) go to out-dir, a new temporary directory unless given; keep it out of the repository. The image
# (bp-test-client) and the volumes for Gradle's cache and the builds (bp-test-gradle, bp-test-work) stay for the next
# run; the container is removed when the run ends. Each checkout builds in a folder of its own on bp-test-work, named
# after the checkout's folder, so runs from two checkouts at once don't overwrite each other's build, and a checkout's
# next run builds only what changed.
set -euo pipefail

checks=${1:?usage: clienttest/run.sh <checks> [out-dir]}
here=$(cd "$(dirname "$0")" && pwd)
out=${2:-$(mktemp -d)}
mkdir -p "$out"

# The checkout's build folder on bp-test-work: the checkout folder's name, with any character unsafe in a path made _.
work=/work/$(printf %s "$(basename "$(cd "$here/../.." && pwd)")" | tr -c 'A-Za-z0-9._-' _)

host_path() { if command -v cygpath > /dev/null; then cygpath -w "$1"; else echo "$1"; fi; }

echo "Building the image bp-test-client (cached after the first run)..."
docker build -q -t bp-test-client "$(host_path "$here")" > /dev/null

name="bp-test-$(date +%Y%m%d-%H%M%S)"
echo "Starting container $name, building in $work on bp-test-work; results go to $out"
id=$(MSYS_NO_PATHCONV=1 docker run -d --name "$name" --cpus=6 --memory=12g \
	--mount "type=bind,source=$(host_path "$here/.."),target=/src,readonly" \
	--mount "type=bind,source=$(host_path "$out"),target=/out" \
	-v bp-test-gradle:/root/.gradle -v bp-test-work:/work -e "BP_WORK=$work" \
	bp-test-client bash /src/clienttest/inside.sh "$checks")
trap 'docker rm -f "$id" > /dev/null' EXIT

docker logs -f "$id" &
status=$(docker wait "$id")
wait
echo "Container exited with $status; results in $out"
exit "$status"
