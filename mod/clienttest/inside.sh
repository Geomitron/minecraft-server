#!/bin/bash
# Runs inside the container (see run.sh): builds the add-on, starts the dev server and two dev clients, each client on
# its own virtual screen, then runs the checks script it's given, which uses the helpers below. Client 1 plays as
# Planner1, an operator; client 2 as Planner2, who isn't. Results go to /out.
set -euo pipefail
checks=$1

mkdir -p /work/mod
rsync -a --delete --exclude /build --exclude /.gradle --exclude /run --exclude /logs /src/ /work/mod/
cd /work/mod
rm -rf run /tmp/args
mkdir -p /tmp/args
: > /out/checks.log

echo "Building the add-on..."
if ! BP_ARGS=/tmp/args ./gradlew --no-daemon -q -I clienttest/capture.gradle runServer runClient runClient2 \
	> /out/gradle.log 2>&1; then
	tail -40 /out/gradle.log
	exit 1
fi

# A flat creative world in peaceful, made fresh for each run.
cat >> run/server/server.properties << 'EOF'
level-type=minecraft\:flat
gamemode=creative
difficulty=peaceful
spawn-monsters=false
generate-structures=false
view-distance=6
simulation-distance=4
EOF
cat > run/server/ops.json << 'EOF'
[{"uuid": "527e7f9d-ae90-3a44-aa88-30a47a9cdf34", "name": "Planner1", "level": 4, "bypassesPlayerLimit": false}]
EOF

# start <run> <folder> <log> [program args...]: starts a captured dev run in the background; its pid goes to
# /tmp/<log>.pid. With LEAVE_OUT set, the classpath leaves out each entry whose path contains it.
start() {
	local run=$1 dir=$2 log=$3
	shift 3
	local a i entry entries kept
	mapfile -t a < "/tmp/args/$run.args"
	if [[ -n ${LEAVE_OUT:-} ]]; then
		for i in "${!a[@]}"; do
			[[ ${a[i]} == -cp ]] || continue
			kept=()
			IFS=: read -ra entries <<< "${a[i + 1]}"
			for entry in "${entries[@]}"; do
				[[ $entry == *"$LEAVE_OUT"* ]] || kept+=("$entry")
			done
			a[i + 1]=$(IFS=:; echo "${kept[*]}")
		done
	fi
	# Each run unpacks LWJGL's native libraries into its own folder; two clients sharing one crash as they unpack.
	(cd "run/$dir" && exec "${a[0]}" -Xmx3G "-Dorg.lwjgl.system.SharedLibraryExtractPath=/tmp/natives-$log" \
		"${a[@]:1}" "$@" > "/out/$log.log" 2>&1) &
	echo $! > "/tmp/$log.pid"
}

# wait_for <file> <text> [seconds]: returns when the file has a line containing the text.
wait_for() {
	timeout "${3:-300}" grep -q -m1 -F -- "$2" <(tail -n +1 -F "$1" 2> /dev/null)
}

# start_client <n> [left-out]: starts dev client n, playing as Planner<n>; the optional second argument leaves the
# mod jars whose paths contain it off the classpath (effortless-building, say).
start_client() {
	local n=$1
	mkdir -p "run/client$n"
	: > "run/client$n/driver-in.txt"
	: > "run/client$n/driver-out.txt"
	LEAVE_OUT=${2:-} DISPLAY=:$n start "runClient$([[ $n == 1 ]] || echo "$n")" "client$n" "client$n" \
		--username "Planner$n" --quickPlayMultiplayer localhost:25565
}

stop_client() {
	local pid
	pid=$(cat "/tmp/client$1.pid")
	kill "$pid"
	wait "$pid" || true
}

marks=0
# drv <client> <command>...: runs the commands on that client's driver and prints each with its result. It fails
# when a command times out, errors, or returns false. Each call ends with a mark the driver echoes; the mark names the
# shell process too, because val runs drv in a subshell, whose count never reaches the caller.
drv() {
	local n=$1
	shift
	local out_file=run/client$n/driver-out.txt before out mark
	before=$(wc -l < "$out_file")
	marks=$((marks + 1))
	mark="mark $BASHPID.$marks"
	printf '%s\n' "$@" "$mark" >> "run/client$n/driver-in.txt"
	if ! wait_for "$out_file" "$mark =>" 180; then
		echo "  client$n: no answer to: $*" | tee -a /out/checks.log
		return 1
	fi
	out=$(tail -n +$((before + 1)) "$out_file" | grep -vF "$mark =>" || true)
	sed "s/^/  client$n: /" <<< "$out" >> /out/checks.log
	printf '%s\n' "$out"
	! grep -qE '=> (TIMEOUT|ERROR|unknown command|false$)' <<< "$out"
}

# val <client> <command>: the command's result alone.
val() {
	drv "$1" "$2" | tail -1 | sed 's/^.* => //'
}

failed=0
# check <description> <command>...: records PASS or FAIL for the command.
check() {
	if "${@:2}" > /dev/null; then
		echo "PASS $1" | tee -a /out/checks.log
	else
		echo "FAIL $1" | tee -a /out/checks.log
		failed=1
	fi
}

# xdo <client> <xdotool command>...: runs xdotool on that client's screen with its window focused; %w in the
# command is the window. Each client is alone on its screen, so its window is the one its process made.
xdo() {
	local n=$1 w
	shift
	w=$(DISPLAY=:$n xdotool search --onlyvisible --pid "$(cat "/tmp/client$n.pid")" 2>> /out/checks.log | head -1)
	if [[ -z $w ]]; then
		echo "  client$n: no window found" | tee -a /out/checks.log
		return 1
	fi
	DISPLAY=:$n xdotool windowfocus "$w" "${@//%w/$w}" 2>&1 | tee -a /out/checks.log
	return "${PIPESTATUS[0]}"
}

# keys <client> <xdotool command>...: real key presses in that client's window, e.g. keys 2 key n.
keys() {
	xdo "$@"
}

# click <client> <x> <y>: a real left click at that window position.
click() {
	xdo "$1" mousemove --window %w "$2" "$3" click 1
}

# use_click <client> and attack_click <client>: the right (use) and left (attack) mouse buttons, held long enough for
# a client tick to see them down, as Effortless Building checks.
use_click() {
	xdo "$1" mousedown 3 sleep 0.3 mouseup 3
}
attack_click() {
	xdo "$1" mousedown 1 sleep 0.3 mouseup 1
}

# client_said <client> <text>: the client's chat showed the text.
client_said() {
	wait_for "/out/client$1.log" "[CHAT] $2" 60
}

finish() {
	for f in run/client*/screenshots/*.png; do
		[[ -e $f ]] && cp "$f" "/out/$(basename "$(dirname "$(dirname "$f")")")-$(basename "$f")"
	done
	return 0
}
trap finish EXIT

Xvfb :1 -screen 0 1024x768x24 -nolisten tcp > /out/xvfb1.log 2>&1 &
Xvfb :2 -screen 0 1024x768x24 -nolisten tcp > /out/xvfb2.log 2>&1 &

echo "Starting the dev server..."
start runServer server server nogui
wait_for /out/server.log 'Done (' 300 || { tail -30 /out/server.log; exit 1; }

echo "Starting both dev clients..."
start_client 1
start_client 2

echo "Running $checks..."
# shellcheck source=/dev/null
source "clienttest/$checks"

if ((failed)); then
	echo "Some checks failed; see checks.log."
	exit 1
fi
echo "All checks passed."
