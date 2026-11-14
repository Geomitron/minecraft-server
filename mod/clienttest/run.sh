#!/bin/bash
# Runs one set of in-game checks in a Linux container: the dev server and two dev clients on a virtual screen.
#
#   clienttest/run.sh <checks> [out-dir]
#
# <checks> is a script in clienttest/ (step6.sh, say). The container's results (checks.log, the game logs and the
# screenshots) go to out-dir, a new temporary directory unless given; keep it out of the repository. The image
# (bp-test-client) and the volumes for Gradle's cache and the build (bp-test-gradle, bp-test-work) stay for the next
# run; the container is removed when the run ends.
set -euo pipefail

checks=${1:?usage: clienttest/run.sh <checks> [out-dir]}
here=$(cd "$(dirname "$0")" && pwd)
out=${2:-$(mktemp -d)}
mkdir -p "$out"

host_path() { if command -v cygpath > /dev/null; then cygpath -w "$1"; else echo "$1"; fi; }

echo "Building the image bp-test-client (cached after the first run)..."
docker build -q -t bp-test-client "$(host_path "$here")" > /dev/null

name="bp-test-$(date +%Y%m%d-%H%M%S)"
echo "Starting container $name; results go to $out"
id=$(MSYS_NO_PATHCONV=1 docker run -d --name "$name" --cpus=6 --memory=12g \
	--mount "type=bind,source=$(host_path "$here/.."),target=/src,readonly" \
	--mount "type=bind,source=$(host_path "$out"),target=/out" \
	-v bp-test-gradle:/root/.gradle -v bp-test-work:/work \
	bp-test-client bash /src/clienttest/inside.sh "$checks")
trap 'docker rm -f "$id" > /dev/null' EXIT

docker logs -f "$id" &
status=$(docker wait "$id")
wait
echo "Container exited with $status; results in $out"
exit "$status"
