# Step 10's in-game checks: hold to repeat in Effortless Building's Single mode, with the radial menu's speed sliders,
# and the look Building Planner sets for Litematica's ghost blocks once. Sourced by inside.sh, which provides drv, val,
# check, keys, xdo, start_client, stop_client and stop_server. Each client starts with a fresh game folder, so its first start is the first start.

# The look, on a first start: both of Litematica's options are set, saved to its config, and recorded. Each client
# also saved a first start before it joined the server.
marker=config/buildingplanner-client.properties
for n in 1 2; do
	check "client $n starts a session on hello" drv $n "await session" "await noscreen"
	check "client $n's first start sets translucent blocks at 0.8" \
		eval '[[ $(val '$n' look) == "translucent=true alpha=0.8" ]]'
	check "client $n saved both to Litematica's config" eval \
		'tr -d " \n\r" < run/client'$n'/config/litematica.json | grep -q "\"ghostBlockAlpha\":0.8" &&
		 tr -d " \n\r" < run/client'$n'/config/litematica.json | grep -Eq "\"renderBlocksAsTranslucent\":(true|\{\"enabled\":true)"'
	check "client $n recorded it in Building Planner's own config" eval \
		'grep -q "^look-defaults-applied=true" run/client'$n'/'$marker
done

# walk <from> <to>: the commands that move Planner1 one block east every two ticks, 2.5 -55 5.5 and on, looking
# straight down, so each held click lands on a new block of the row z 5 at y -60 (-61 is the grass below).
walk() {
	local x
	for ((x = $1; x <= $2; x++)); do
		printf '%s\n' "cmd tp @s $x.5 -55 5.5 0 90" "wait 2"
	done
}

# hold_while_walking <button>: holds that mouse button for 2.5 seconds, as a real press, while Planner1 walks on past
# the end of the hold, so the button never stays down over a block it has already acted on.
hold_while_walking() {
	local steps
	mapfile -t steps < <(walk 3 40)
	xdo 1 mousedown "$1" sleep 2.5 mouseup "$1" &
	local holder=$!
	drv 1 "${steps[@]}" > /dev/null
	wait "$holder"
}

# hold_ok <log> <button> <period>: the hold log shows every gap between edits as <period> ticks, and the edits a held
# button gives: one when it goes down, then one every <period> ticks.
hold_ok() {
	local log=$1 down edits gaps
	down=${log#*"$2="}
	down=${down%% *}
	edits=${log#*edits=}
	edits=${edits%% *}
	gaps=${log#*gaps=}
	((edits == 1 + (down - 1) / $3)) || return 1
	[[ -z $gaps ]] || [[ $(tr ',' '\n' <<< "$gaps" | sort -u) == "$3" ]]
}

# Both players stand south of the row.
drv 1 "cmd tp @s 5 -60 -3 0 10" "cmd tp Planner2 6 -60 -3 0 10" "wait 40" > /dev/null

# Holding the use button in Single mode keeps placing: stone bricks in a row, one new block each repeat.
drv 1 "cmd clear @s" "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "ebmode SINGLE" \
	"ebarray 0" "cmd tp @s 2.5 -55 5.5 0 90" "wait 30" "holdreset" > /dev/null
hold_while_walking 3
drv 1 "wait 20" > /dev/null
placed_log=$(val 1 holdlog)
echo "  hold use: $placed_log" >> /out/checks.log
check "holding use sends an edit when it goes down and every 4 ticks after" hold_ok "$placed_log" use 4
placed=${placed_log#*edits=}
placed=${placed%% *}
check "each repeat put a ghost block in the row, on client 1 and on client 2" eval \
	'drv 1 "await block 2 -60 5 minecraft:stone_bricks" > /dev/null &&
	 [[ $(val 1 "countblocks 2 40 -60 5 minecraft:stone_bricks") == '"$placed"' ]] &&
	 [[ $(val 2 "countblocks 2 40 -60 5 minecraft:stone_bricks") == '"$placed"' ]]'
check "and placed no real block" eval '[[ $(val 1 "real 2 -60 5") == minecraft:air ]]'
drv 1 "cmd tp @s 20 -55 -5 0 40" "wait 20" "notoasts" "wait 5" "shot hold-placed" > /dev/null

# Holding the attack button keeps erasing: the whole row is filled first, so every repeat has a block to erase.
drv 1 "edit 2 -60 5 40 -60 5 minecraft:stone_bricks" "await block 40 -60 5 minecraft:stone_bricks" \
	"cmd tp @s 2.5 -55 5.5 0 90" "wait 30" "holdreset" > /dev/null
hold_while_walking 1
drv 1 "wait 20" > /dev/null
erased_log=$(val 1 holdlog)
echo "  hold attack: $erased_log" >> /out/checks.log
check "holding attack sends an edit when it goes down and every 6 ticks after" hold_ok "$erased_log" attack 6
erased=${erased_log#*edits=}
erased=${erased%% *}
check "each repeat erased one ghost block, on client 1 and on client 2" eval \
	'[[ $(val 1 "countblocks 2 40 -60 5 minecraft:stone_bricks") == '"$((39 - erased))"' ]] &&
	 [[ $(val 2 "countblocks 2 40 -60 5 minecraft:stone_bricks") == '"$((39 - erased))"' ]]'
check "and left the real blocks" eval '[[ $(val 1 "real 2 -61 5") == minecraft:grass_block ]]'

# The other modes keep Effortless Building's own behaviour: a held click in Line mode, which needs two clicks, sends
# nothing, and with the mode disabled the click isn't Building Planner's at all.
for mode in LINE DISABLED; do
	drv 1 "ebmode $mode" "cmd tp @s 30.5 -55 5.5 0 90" "wait 30" "holdreset" > /dev/null
	xdo 1 mousedown 3
	drv 1 "wait 30" > /dev/null
	xdo 1 mouseup 3
	drv 1 "wait 10" > /dev/null
	check "holding use in $mode mode sends no edit" eval '[[ $(val 1 holdlog) == *"edits=0 "* ]]'
done

# The radial menu's sliders, opened with the menu's key (left Alt) held: a click on the placing slider sets 10 blocks
# a second (2 ticks), and a drag of the erasing slider's handle sets 2 a second (10 ticks).
drv 1 "ebmode SINGLE" "cmd tp @s 20 -58 -5 0 30" "wait 20" "notoasts" > /dev/null
xdo 1 keydown Alt_L
check "the radial menu shows both sliders at their first settings" eval 'drv 1 "await screen Build Modes" > /dev/null &&
	[[ $(val 1 texts) == "Placing speed: 5 blocks/s | Erasing speed: 3.33 blocks/s" ]]'
drv 1 "wait 20" "shot radial-first" > /dev/null
# The pointer over "Single", whose description shows under the menu.
xdo 1 mousemove --window %w 493 170
drv 1 "wait 10" "shot radial-single-description" > /dev/null
read -r x y < <(val 1 "sliderat 2 Placing speed")
xdo 1 mousemove --window %w "$x" "$y" click 1
read -r x y < <(val 1 "sliderat 6 Erasing speed")
read -r x2 y2 < <(val 1 "sliderat 10 Erasing speed")
xdo 1 mousemove --window %w "$x" "$y" mousedown 1 mousemove --window %w $(((x + x2) / 2)) "$y" sleep 0.2 \
	mousemove --window %w "$x2" "$y2" sleep 0.2 mouseup 1
check "the click and the drag set them to 10 and 2 blocks a second" \
	eval '[[ $(val 1 texts) == "Placing speed: 10 blocks/s | Erasing speed: 2 blocks/s" ]]'
xdo 1 mousemove --window %w 600 300
drv 1 "wait 10" "shot radial-moved" > /dev/null
xdo 1 keyup Alt_L
check "the menu closes when its key is let go" drv 1 "await noscreen"
check "hold to repeat takes both at once" eval '[[ $(val 1 repeat) == "place=2 erase=10" ]]'
check "and they're kept in Building Planner's client config" eval \
	'grep -q "^place-repeat-ticks=2" run/client1/'$marker' && grep -q "^erase-repeat-ticks=10" run/client1/'$marker

# hold_at <button> <name> <period>: holds the button over the row z 5 from 2 -60 5, as above, and checks the edits
# came every <period> ticks. For erasing, the row is filled first.
hold_at() {
	if [[ $1 == 1 ]]; then
		drv 1 "fill 2 -60 5 40 -60 5 minecraft:stone_bricks" "await block 40 -60 5 minecraft:stone_bricks" > /dev/null
	else
		drv 1 "fill 2 -60 5 40 -57 5 minecraft:air" "await block 40 -60 5 minecraft:air" > /dev/null
	fi
	drv 1 "cmd tp @s 2.5 -55 5.5 0 90" "wait 30" "holdreset" > /dev/null
	hold_while_walking "$1"
	drv 1 "wait 20" > /dev/null
	local log
	log=$(val 1 holdlog)
	echo "  hold $2: $log" >> /out/checks.log
	hold_ok "$log" "$2" "$3"
}
check "holding use now sends an edit every 2 ticks" hold_at 3 use 2
check "and holding attack one every 10" hold_at 1 attack 10

# Both settings survive a restart: the sliders show them, and hold to repeat uses them.
stop_client 1
start_client 1
check "client 1 starts a session again" drv 1 "await session" "await noscreen" "await placement Yard"
check "after a restart, hold to repeat keeps both settings" eval '[[ $(val 1 repeat) == "place=2 erase=10" ]]'
drv 1 "cmd tp @s 20 -58 -5 0 30" "wait 20" > /dev/null
xdo 1 keydown Alt_L
check "and so do the sliders" eval 'drv 1 "await screen Build Modes" > /dev/null &&
	[[ $(val 1 texts) == "Placing speed: 10 blocks/s | Erasing speed: 2 blocks/s" ]]'
xdo 1 keyup Alt_L
drv 1 "await noscreen" "ebmode SINGLE" > /dev/null
check "holding use after the restart sends an edit every 2 ticks" hold_at 3 use 2
drv 1 "ebmode DISABLED" > /dev/null

# The look, after the player's own change: translucent off and alpha 0.3, saved; a restart keeps both.
drv 2 "setlook false 0.3" > /dev/null
stop_client 2
start_client 2
check "client 2 starts a session again" drv 2 "await session" "await noscreen"
check "a restart keeps the player's own look, not Building Planner's" \
	eval '[[ $(val 2 look) == "translucent=false alpha=0.3" ]]'

# With the record gone but the alpha still the player's own, only the option left at Litematica's default is set, and
# on a client that never joins a server with Building Planner: the server is stopped first.
stop_client 2
stop_server
rm "run/client2/$marker"
start_client 2
check "a client that joins no server sets the option left at its default, and keeps the player's alpha" \
	eval 'drv 2 "wait 60" > /dev/null && [[ $(val 2 look) == "translucent=true alpha=0.3" ]]'
check "and records it" eval 'grep -q "^look-defaults-applied=true" run/client2/'$marker
check "and saves it" eval \
	'tr -d " \n\r" < run/client2/config/litematica.json | grep -q "\"ghostBlockAlpha\":0.3"'
