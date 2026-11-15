# In-game checks for hook S3: Planner1 shares plans, which Planner2 loads, and removes them while Planner2's game draws
# their boxes; Planner2's game keeps running, and Syncmatica handles every packet on its main thread. Without the hook,
# Syncmatica handles some packets on the network thread, always its handshake, and on some connections the plans'
# loads and unloads too; then the game can crash with a ConcurrentModificationException in Litematica's
# OverlayRenderer.renderBoxes. The race is rare per plan, so the checks load and remove 30 plans, 10 at a time. To see
# the crash, run this on a copy of mod/ with "syncmatica.ClientPlayHandlerMixin" taken out of
# buildingplanner.mixins.json; a crash may take more than one run. Sourced by inside.sh, which provides drv, val,
# check, keys and click.

# client2_running: client 2's game process hasn't ended, as it does when the game crashes.
client2_running() {
	kill -0 "$(cat /tmp/client2.pid)" 2> /dev/null
}

# loaded_on_2 <placement|noplacement> <plan>...: client 2 has each plan loaded, or none of them.
loaded_on_2() {
	local state=$1 plan
	shift
	client2_running || return 1
	for plan in "$@"; do
		drv 2 "await $state $plan" > /dev/null || return 1
	done
}

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"
# Planner2 stands south of the plans, looking at them.
drv 1 "cmd tp @s 25 -60 -6 0 10" "cmd tp Planner2 26 -60 -6 0 10" "wait 40" > /dev/null

removed=()
for round in 1 2 3; do
	# Ten plans in a row, 3 by 2 by 3 blocks each, 5 blocks apart, which both players load.
	plans=()
	for i in {1..10}; do
		x=$((5 * i - 3))
		plans+=("R${round}P$i")
		drv 1 "select $x -60 2 $((x + 2)) -59 4" > /dev/null
		keys 1 key n
		drv 1 "await screen Start a plan" > /dev/null
		keys 1 type "R${round}P$i"
		keys 1 key Return
		drv 1 "await noscreen" "await placement R${round}P$i" > /dev/null
	done
	check "client 2 loads round $round's ten plans" loaded_on_2 placement "${plans[@]}"
	# Planner1 removes them one by one with Syncmatica's Remove button, each time on a row the list shows: it shows a
	# few rows at a time.
	left=("${plans[@]}")
	while ((${#left[@]})); do
		drv 1 "syncgui" "wait 10" > /dev/null
		for plan in "${left[@]}"; do
			read -r x y < <(val 1 "button $plan Remove")
			[[ $x == none ]] || break
		done
		click 1 "$x" "$y"
		drv 1 "await notshared $plan" "close" > /dev/null
		removed+=("$plan")
		mapfile -t left < <(printf '%s\n' "${left[@]}" | grep -vxF "$plan")
	done
	client2_running || break
done

check "Planner1 removed all 30 plans" eval '(( ${#removed[@]} == 30 ))'
check "client 2's game is still running" client2_running
check "and logged no ConcurrentModificationException" eval '! grep -q ConcurrentModificationException /out/client2.log'
check "and unloaded every removed plan" loaded_on_2 noplacement "${removed[@]}"
check "Syncmatica and Litematica logged nothing from client 2's network thread" \
	eval '! grep -qE "\[Netty [^]]*\] \((syncmatica|litematica)\)" /out/client2.log'
if client2_running; then
	drv 2 "notoasts" "wait 5" "shot after-removals" "wait 20" > /dev/null
fi
