#!/bin/bash
# Runs inside the container (see run.sh): builds the add-on, starts the dev server and two dev clients, each client on
# its own virtual screen, then runs the checks script it's given, which uses the helpers below. Client 1 plays as
# Planner1, an operator; client 2 as Planner2, who isn't. It builds in BP_WORK, the checkout's own folder on the
# bp-test-work volume (see run.sh). Results go to /out.
set -euo pipefail
checks=$1
work=${BP_WORK:?BP_WORK names the build folder on the volume; run.sh sets it}

mkdir -p "$work"
rsync -a --delete --exclude /build --exclude /.gradle --exclude /run --exclude /logs /src/ "$work/"
cd "$work"
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
# /tmp/<log>.pid. With LEAVE_OUT set to one or more globs separated by spaces, the classpath leaves out each entry whose
# path matches one of them, anywhere in the path.
start() {
	local run=$1 dir=$2 log=$3
	shift 3
	local a i entry entries kept pattern patterns
	mapfile -t a < "/tmp/args/$run.args"
	if [[ -n ${LEAVE_OUT:-} ]]; then
		read -ra patterns <<< "$LEAVE_OUT"
		for i in "${!a[@]}"; do
			[[ ${a[i]} == -cp ]] || continue
			kept=()
			IFS=: read -ra entries <<< "${a[i + 1]}"
			for entry in "${entries[@]}"; do
				for pattern in "${patterns[@]}"; do
					# shellcheck disable=SC2053
					[[ $entry == *$pattern* ]] && continue 2
				done
				kept+=("$entry")
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

# The add-on's own classes and resources on the classpath, and the driver's, for start_server and start_client to
# leave out; and the driver's alone.
ADDON="$work/build/*/"
DRIVER="$work/build/*/clienttest"

# start_server [left-out] [log]: starts the dev server, logging to /out/<log>.log (server.log unless given), and
# waits until it's ready; the optional first argument leaves the classpath entries matching it out ($ADDON, say).
start_server() {
	local log=${2:-server}
	LEAVE_OUT=${1:-} start runServer server "$log" nogui
	wait_for "/out/$log.log" 'Done (' 300 || { tail -30 "/out/$log.log"; return 1; }
}

# stop_server [log]: stops the server start_server started with that log.
stop_server() {
	local pid
	pid=$(cat "/tmp/${1:-server}.pid")
	kill "$pid"
	wait "$pid" || true
}

# start_client <n> [left-out]: starts dev client n, playing as Planner<n>; the optional second argument leaves the
# classpath entries matching it out (effortless-building, say, or several globs separated by spaces).
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
# Sections of checks a script leaves for a later build step (step9.sh's pending), which don't run.
pending_sections=0
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

# keys <client> <xdotool command>...: real key presses in that client's window, e.g. keys 2 key b.
keys() {
	xdo "$@"
}

# click <client> <x> <y>: a real left click at that window position.
click() {
	xdo "$1" mousemove --window %w "$2" "$3" click 1
}

# use_click <client> and attack_click <client>: the right (use) and left (attack) mouse buttons, held until the
# driver has seen them down in a client tick, as Effortless Building checks, and let go at once, well before the 4
# ticks after which a held button repeats. A held time would make a click repeat when the game runs fast.
use_click() {
	held_click "$1" 3 use
}
attack_click() {
	held_click "$1" 1 attack
}
held_click() {
	local status=0
	drv "$1" "holdreset" > /dev/null
	xdo "$1" mousedown "$2"
	drv "$1" "await held $3" > /dev/null || status=$?
	xdo "$1" mouseup "$2"
	return "$status"
}

# client_said <client> <text>: the client's chat showed the text.
client_said() {
	wait_for "/out/client$1.log" "[CHAT] $2" 60
}

# no_planner_message <client>: that client's chat has shown none of Building Planner's messages, the texts in its
# language file other than its screens' and key bindings', each matched by its text up to its first %s (or, for one
# that starts with %s, by its text from there to the next).
no_planner_message() {
	! grep -qFf <(tr -d '\r' < src/main/resources/assets/buildingplanner/lang/en_us.json | sed -n \
		-e '/"buildingplanner\.ui\./d' \
		-e 's/^\s*"buildingplanner\.[^"]*": "%s\([^"%]*\).*$/\1/p' \
		-e 's/^\s*"buildingplanner\.[^"]*": "\([^"%]*\).*$/[CHAT] \1/p') "/out/client$1.log"
}

finish() {
	for f in run/client*/screenshots/*.png; do
		[[ -e $f ]] && cp "$f" "/out/$(basename "$(dirname "$(dirname "$f")")")-$(basename "$f")"
	done
	return 0
}
trap finish EXIT

# Screens of 1920 by 1080, so a check can grow a window to that size (the driver's window command).
Xvfb :1 -screen 0 1920x1080x24 -nolisten tcp > /out/xvfb1.log 2>&1 &
Xvfb :2 -screen 0 1920x1080x24 -nolisten tcp > /out/xvfb2.log 2>&1 &

echo "Starting the dev server..."
start_server || exit 1

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
if ((pending_sections)); then
	echo "All checks that ran passed; $pending_sections sections wait on later build steps."
else
	echo "All checks passed."
fi
