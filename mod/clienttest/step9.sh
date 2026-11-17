# Step 9's in-game checks: showing and hiding ghost blocks with N and the radial menu's button, drawing while they're
# hidden, the choice kept across a restart, and where the radial menu's buttons sit, with screenshots. Below them,
# build steps 10 and 11's checks (copy and paste, and the material list), which don't run until those steps build
# them, and the keys on a server without Building Planner. Sourced by inside.sh, which provides drv, val, check, keys,
# xdo, click, use_click, attack_click, client_said, start_server, stop_server, start_client, stop_client,
# no_planner_message and $ADDON. Players stand on the flat world's grass, whose top is at y -61.

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

# press <client> <key>: a real key press with no screen open.
press() {
	drv "$1" "close" "wait 5" > /dev/null
	keys "$1" key "$2"
}

# state_has <client> <x y z> <text>: the state Litematica shows there contains the text.
state_has() {
	[[ $(val "$1" "state $2") == *"$3"* ]]
}

# shot <client> <name>: a screenshot with the game's pop-ups cleared, written before the call returns.
shot() {
	drv "$1" "notoasts" "wait 5" "shot $2" "wait 20" > /dev/null
}

# pending <step> <what>: the checks it guards belong to build step <step>, which isn't built yet. They don't run, and
# are logged as failing until that step builds them and takes this out.
pending() {
	echo "FAIL (until build step $1) $2: not run" | tee -a /out/checks.log
	pending_sections=$((pending_sections + 1))
	return 1
}

# chat_count <client> <text>: how many lines of that client's chat have been exactly the text.
chat_count() {
	tr -d '\r' < "/out/client$1.log" |
		awk -v t="[CHAT] $2" 'length($0) >= length(t) && substr($0, length($0) - length(t) + 1) == t' | wc -l
}

# said_once_more <client> <text> <count>: that client's chat has shown the text once more than <count> times.
said_once_more() {
	(($(chat_count "$1" "$2") == $3 + 1))
}

config=run/client1/config/buildingplanner-client.properties

# Ghost blocks in the shared layer: a floor of stone bricks with oak stairs and glowstone on it, and oak planks.
drv 1 "cmd tp @s 5 -60 -3 0 10" "cmd tp Planner2 6 -60 -3 0 10" "wait 40" > /dev/null
drv 1 "edit 2 -60 2 8 -60 8 minecraft:stone_bricks" "edit 3 -59 3 3 -59 3 minecraft:oak_stairs[facing=north]" \
	"edit 4 -59 3 4 -59 3 minecraft:glowstone" "edit 12 -60 2 16 -60 6 minecraft:oak_planks" > /dev/null
check "both clients show the ghost blocks" eval 'drv 1 "await block 4 -59 3 minecraft:glowstone" \
	"await block 16 -60 6 minecraft:oak_planks" > /dev/null && drv 2 "await block 4 -59 3 minecraft:glowstone" \
	"await block 16 -60 6 minecraft:oak_planks" > /dev/null'

# N hides every ghost block for that player alone, by taking every tile out of Litematica; the choice is kept.
drv 1 "cmd tp @s 9.5 -58 -3.5 20 25" "wait 40" > /dev/null
check "ghost blocks show at first" eval '[[ $(val 1 ghosts) == "shown, "* ]]'
shot 1 ghosts-shown
hidden_lines=$(chat_count 1 "Ghost blocks hidden.")
press 1 n
check "N hides them: Litematica has no tile, though the game keeps them" \
	eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) =~ ^hidden,\ [1-9][0-9]*\ tiles,\ 0\ placements$ ]]'
check "and says so in chat" said_once_more 1 "Ghost blocks hidden." "$hidden_lines"
check "and shows none of them" eval '[[ $(val 1 "get 4 -59 3") == minecraft:air ]] &&
	[[ $(val 1 "get 12 -60 2") == minecraft:air ]] && [[ $(val 1 "layer 4 -59 3") == minecraft:glowstone ]]'
check "for that player alone" eval '[[ $(val 2 "get 4 -59 3") == minecraft:glowstone ]]'
check "and Building Planner's client config keeps the choice" grep -q "^ghost-blocks-shown=false" "$config"
shot 1 ghosts-hidden

# While they're hidden, another player's edits reach the game's copy of the layer, without a glow: one in a column
# with ghost blocks, and one in a column without any, which makes a new tile.
drv 2 "edit 6 -59 6 6 -59 6 minecraft:gold_block" "edit 40 -60 5 40 -60 5 minecraft:gold_block" \
	"await block 6 -59 6 minecraft:gold_block" "await block 40 -60 5 minecraft:gold_block" > /dev/null
check "Planner2's edits reach Planner1's copy of the layer while hidden" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 "layer 6 -59 6") == minecraft:gold_block ]] && [[ $(val 1 "get 6 -59 6") == minecraft:air ]] &&
	[[ $(val 1 "layer 40 -60 5") == minecraft:gold_block ]] && [[ $(val 1 "get 40 -60 5") == minecraft:air ]]'
check "and don't glow" eval '[[ -z $(val 1 glows) ]]'
shown_lines=$(chat_count 1 "Ghost blocks shown.")
press 1 n
check "N shows them again, Planner2's edits with them" drv 1 "await block 6 -59 6 minecraft:gold_block" \
	"await block 40 -60 5 minecraft:gold_block" "await block 4 -59 3 minecraft:glowstone"
check "and says so in chat" said_once_more 1 "Ghost blocks shown." "$shown_lines"
check "and the config keeps that" grep -q "^ghost-blocks-shown=true" "$config"

# The radial menu's button does what N does. As with Effortless Building's own buttons, it acts when the menu's key
# (left Alt) is let go with the pointer over it, and on a click, after which letting the key go doesn't act again.
# radial_open: opens the radial menu by holding its key down. radial_point <name>: puts the pointer on the menu's
# button of that name. radial_let_go: lets the key go, which closes the menu.
radial_open() {
	xdo 1 keydown Alt_L
	drv 1 "await screen Build Modes" "wait 10" > /dev/null
}
radial_point() {
	local x y
	read -r x y < <(val 1 "widget 0 $1")
	xdo 1 mousemove --window %w "$x" "$y"
	drv 1 "wait 5" > /dev/null
}
radial_let_go() {
	xdo 1 keyup Alt_L
	drv 1 "await noscreen" "wait 5" > /dev/null
}
drv 1 "ebmode SINGLE" "notoasts" > /dev/null
hidden_lines=$(chat_count 1 "Ghost blocks hidden.")
radial_open
radial_point "Show or hide ghost blocks"
check "with the pointer on the radial menu's button, ghost blocks still show" eval \
	'[[ $(val 1 ghosts) == "shown, "* ]]'
radial_let_go
check "letting the menu's key go over the button hides ghost blocks" eval '[[ $(val 1 ghosts) == "hidden, "* ]] &&
	[[ $(val 1 "get 4 -59 3") == minecraft:air ]]'
check "and says so in chat" said_once_more 1 "Ghost blocks hidden." "$hidden_lines"
check "and leaves Effortless Building's mode as it was" eval '[[ $(val 1 ebmode) == SINGLE ]]'
shown_lines=$(chat_count 1 "Ghost blocks shown.")
radial_open
radial_point "Show or hide ghost blocks"
radial_let_go
check "and again shows them" eval '[[ $(val 1 ghosts) == "shown, "* ]] &&
	drv 1 "await block 4 -59 3 minecraft:glowstone" > /dev/null'
check "and says so in chat" said_once_more 1 "Ghost blocks shown." "$shown_lines"
hidden_lines=$(chat_count 1 "Ghost blocks hidden.")
radial_open
radial_point "Show or hide ghost blocks"
xdo 1 click 1
drv 1 "wait 5" > /dev/null
check "a click on the button hides them, the menu staying open" eval '[[ $(val 1 ghosts) == "hidden, "* ]] &&
	drv 1 "await screen Build Modes" > /dev/null'
radial_let_go
check "and letting the key go over it then doesn't act again" eval '[[ $(val 1 ghosts) == "hidden, "* ]] &&
	said_once_more 1 "Ghost blocks hidden." "$hidden_lines"'
press 1 n
check "N shows them" drv 1 "await block 4 -59 3 minecraft:glowstone"

# Drawing a shape while ghost blocks are hidden shows them again, then draws, with a message the first time only.
drv 1 "cmd clear @s" "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "ebarray 0" > /dev/null
press 1 n
drv 1 "cmd tp @s 20.5 -55 5.5 0 90" "wait 30" > /dev/null
check "ghost blocks are hidden before the shape" eval '[[ $(val 1 ghosts) == "hidden, "* ]]'
shown_lines=$(chat_count 1 "Ghost blocks shown.")
use_click 1
check "with ghost blocks hidden, a shape shows them again, then draws" drv 1 \
	"await block 4 -59 3 minecraft:glowstone" "await block 20 -60 5 minecraft:stone_bricks"
check "and says so" client_said 1 "Ghost blocks shown again. Press N to hide them."
check "without the switch's own line" eval '(($(chat_count 1 "Ghost blocks shown.") == shown_lines))'
check "and the config keeps them shown" grep -q "^ghost-blocks-shown=true" "$config"
drv 1 "cmd tp @s 20.5 -58 -2.5 0 40" "wait 20" > /dev/null
shot 1 shown-again
press 1 n
drv 1 "cmd tp @s 22.5 -55 5.5 0 90" "wait 30" > /dev/null
use_click 1
check "a second shape shows them again too" drv 1 "await block 4 -59 3 minecraft:glowstone" \
	"await block 22 -60 5 minecraft:stone_bricks"
check "without a second message" eval '(($(grep -cF "[CHAT] Ghost blocks shown again." /out/client1.log) == 1))'

# Hidden ghost blocks stay hidden after a restart, until N shows them.
press 1 n
check "hidden again" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) == "hidden, "* ]]'
stop_client 1
cp /out/client1.log /out/client1-before-restart.log
start_client 1
check "after a restart, client 1 starts a session with ghost blocks still hidden" eval \
	'drv 1 "await session" "await noscreen" "wait 60" > /dev/null &&
	 [[ $(val 1 ghosts) =~ ^hidden,\ [1-9][0-9]*\ tiles,\ 0\ placements$ ]] &&
	 [[ $(val 1 "layer 4 -59 3") == minecraft:glowstone ]] && [[ $(val 1 "get 4 -59 3") == minecraft:air ]]'
press 1 n
check "and N shows them" drv 1 "await block 4 -59 3 minecraft:glowstone"

# Where the radial menu's buttons sit, with build steps 10 and 11's buttons shown too, in Circle mode, whose options
# fill two rows right of the ring, set before each opening since letting the key go over "Single" chooses it.
# radial_shot <width> <height> <gui scale> <name> opens the menu in a window of that size at that GUI scale, with the
# pointer over "Single" (about 33 right of the menu's middle and 35 above it) so its name and description show, and
# shoots it as <name>. The buttons' boxes go to the log, for outlining. It fails unless the buttons meet nothing the
# menu draws, the sliders, each other or the screen's edges.
radial_shot() {
	local result
	drv 1 "ebmode CIRCLE" "window $1 $2 $3" "wait 20" > /dev/null
	radial_open
	xdo 1 mousemove --window %w $(($1 / 2 + 33 * $3)) $(($2 / 2 - 35 * $3))
	drv 1 "wait 10" "notoasts" "wait 5" "notoasts" "shot $4" > /dev/null
	echo "  $4 buttons: $(val 1 buttons)" >> /out/checks.log
	result=$(val 1 radialclear)
	echo "  $4: $result" >> /out/checks.log
	radial_let_go
	[[ $result == clear ]]
}
# With ghost blocks hidden, so these shots and the hover shots below show every icon between them.
drv 1 "draftbuttons true" "cmd tp @s 20.5 -58 -2.5 0 40" "wait 20" > /dev/null
press 1 n
check "the buttons meet nothing at 854 by 480, GUI scale 2" radial_shot 854 480 2 radial-draft-854x480-gui2
check "nor at 1920 by 1080, GUI scale 2" radial_shot 1920 1080 2 radial-draft-1920x1080-gui2
check "nor at GUI scale 4" radial_shot 1920 1080 4 radial-draft-1920x1080-gui4
press 1 n
# The show or hide button's hover text, with only the buttons built so far. The pointer leaves the button before the
# key is let go, so closing the menu leaves ghost blocks shown.
drv 1 "draftbuttons false" "ebmode SINGLE" > /dev/null
for scale in 2 4; do
	drv 1 "window 1920 1080 $scale" "wait 20" > /dev/null
	radial_open
	radial_point "Show or hide ghost blocks"
	drv 1 "wait 10" "notoasts" "shot radial-hover-1920x1080-gui$scale" > /dev/null
	echo "  radial-hover-1920x1080-gui$scale buttons: $(val 1 buttons)" >> /out/checks.log
	xdo 1 mousemove --window %w 5 5
	radial_let_go
done
check "ghost blocks still show after the hover shots" eval '[[ $(val 1 ghosts) == "shown, "* ]]'
drv 1 "window 854 480 2" "wait 20" > /dev/null

# Copy and paste, as they worked on separate plans: build step 10 rewrites these for the shared layer, with Copy's box
# drawn with the "Cube" shape and the copy following the aim, and its radial menu buttons.
if pending 10 "copy and paste"; then
	# Copy from one plan: the marked area's 8 plan blocks become a copy placed on the block Planner1 looks down at,
	# selected, with Litematica's tool in its placement mode, and no real block is placed.
	drv 1 "select 3 -60 3 5 -59 4" "cmd tp @s 5.5 -60 20.5 0 90" "wait 40" > /dev/null
	press 1 j
	check "J copies the marked area's plan blocks, with a message" client_said 1 "Copied 8 blocks. Press K to paste them."
	shot 1 copy-message
	check "the copy sits on the aimed block, selected" drv 1 "await copy origin 5,-60,20 NONE NONE selected Building Planner copy"
	check "with Litematica's tool in its placement mode" eval '[[ $(val 1 toolmode) == SCHEMATIC_PLACEMENT ]]'
	check "the copy shows the plan's blocks as they were" eval 'state_has 1 "5 -59 20" "oak_stairs" &&
		state_has 1 "5 -59 20" "facing=north" && [[ $(val 1 "get 6 -59 20") == minecraft:glowstone ]] &&
		[[ $(val 1 "get 7 -60 21") == minecraft:stone_bricks ]] && [[ $(val 1 "get 7 -59 21") == minecraft:air ]]'
	check "and places no real block" eval '[[ $(val 1 "real 5 -59 20") == minecraft:air ]]'
	check "and isn't shared" eval 'drv 2 "wait 20" > /dev/null && [[ $(val 2 "get 5 -59 20") == minecraft:air ]]'
	drv 1 "cmd tp @s 6.5 -59 26.5 180 30" "wait 40" > /dev/null
	shot 1 copy-placed

	# Moving it with a real click of Litematica's tool on the grass at 30 -61 20.
	drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:stick" "cmd tp @s 30.5 -60 20.5 0 90" "wait 40" \
		> /dev/null
	attack_click 1
	check "a click with Litematica's tool moves the copy" \
		eval 'drv 1 "wait 20" > /dev/null && [[ $(val 1 copy) =~ ^origin\ 30,-6[01],20\ NONE\ NONE\ selected ]]'
	echo "  the tool click put the copy's origin at: $(val 1 copy)" >> /out/checks.log
	drv 1 "movecopy 30 -60 20" "cmd tp @s 30.5 -58 26.5 180 30" "wait 40" > /dev/null

	# U gives it a quarter turn clockwise around its origin: the stairs now face east.
	press 1 u
	check "U turns the copy a quarter turn clockwise" drv 1 "await copy origin 30,-60,20 NONE CLOCKWISE_90 selected"
	check "its blocks turn with it" eval 'state_has 1 "30 -59 20" "facing=east" &&
		[[ $(val 1 "get 30 -59 21") == minecraft:glowstone ]] && [[ $(val 1 "get 29 -60 22") == minecraft:stone_bricks ]]'
	shot 1 copy-turned

	# H flips it left to right for Planner1, who faces south: the stairs face west, and the copy's floor moves from
	# 29..30 to 30..31 east to west.
	drv 1 "cmd tp @s 30.5 -58 14.5 0 30" "wait 40" > /dev/null
	press 1 h
	check "H mirrors the copy left to right" drv 1 "await copy origin 30,-60,20 FRONT_BACK COUNTERCLOCKWISE_90 selected"
	check "its blocks flip with it" eval 'state_has 1 "30 -59 20" "facing=west" &&
		[[ $(val 1 "get 31 -60 22") == minecraft:stone_bricks ]] && [[ $(val 1 "get 29 -60 22") == minecraft:air ]]'
	shot 1 copy-mirrored
	# Without the tool item in hand, Litematica's tool display leaves the chat clear for the screenshots below.
	drv 1 "cmd clear @s" > /dev/null

	# Barn gets glass where the copy has empty space, and a gold block where the copy has stone bricks; the copy moves
	# over them.
	drv 1 "fill 15 -58 4 15 -58 6 minecraft:glass" "place 14 -58 6 minecraft:glass" "place 15 -59 5 minecraft:gold_block" \
		> /dev/null
	check "client 2 sees Barn's glass and gold block" drv 2 "await block 14 -58 6 minecraft:glass" \
		"await block 15 -59 5 minecraft:gold_block"
	drv 1 "movecopy 14 -59 4" "cmd tp @s 14.5 -57 11.5 180 30" "wait 40" > /dev/null
	check "the copy moves over Barn" drv 1 "await copy origin 14,-59,4 FRONT_BACK COUNTERCLOCKWISE_90 selected"
	# Where the copy and a plan overlap, Litematica shows one block; the log records which.
	echo "  where the copy overlaps Barn: 15 -59 5 shows $(val 1 "get 15 -59 5") (copy stone_bricks, Barn gold_block);" \
		"15 -58 4 shows $(val 1 "get 15 -58 4") (copy empty, Barn glass)" >> /out/checks.log
	check "the copy's empty space shows Barn's glass" eval '[[ $(val 1 "get 15 -58 4") == minecraft:glass ]] &&
		[[ $(val 1 "get 15 -59 5") == minecraft:stone_bricks ]]'
	shot 1 copy-over-plan

	# K pastes it into Barn, where its corner is: its 8 blocks, the stairs still facing west, and its empty space leaves
	# Barn's glass.
	drv 1 "cmd tp Planner2 18.5 -57 11.5 150 30" "wait 40" > /dev/null
	press 1 k
	check "K pastes the copy into the plan at its corner, for the other player" drv 2 \
		"await block 14 -58 4 minecraft:oak_stairs" "await block 14 -58 5 minecraft:glowstone" \
		"await block 15 -59 5 minecraft:stone_bricks" "await block 14 -59 6 minecraft:stone_bricks"
	check "glowing with Planner1's name" drv 2 "await glow Planner1:8"
	shot 2 paste-seen
	check "turned and mirrored as the copy showed it" state_has 2 "14 -58 4" "facing=west"
	check "the copy's empty space leaves Barn's blocks" eval '[[ $(val 2 "get 15 -58 4") == minecraft:glass ]] &&
		[[ $(val 2 "get 15 -58 6") == minecraft:glass ]] && [[ $(val 2 "get 14 -58 6") == minecraft:glass ]]'
	check "Barn keeps its size" drv 2 "await area Barn 0,0,0..4,2,4"
	check "and the copy stays for another paste" drv 1 "await copy origin 14,-59,4 FRONT_BACK COUNTERCLOCKWISE_90 selected"

	# One Ctrl+Z undoes the whole paste; Ctrl+Y redoes it.
	drv 1 "close" > /dev/null
	keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
	check "Ctrl+Z undoes the whole paste" drv 2 "await block 14 -58 4 minecraft:air" "await block 14 -58 5 minecraft:air" \
		"await block 15 -59 5 minecraft:gold_block" "await block 14 -59 6 minecraft:air" "await block 15 -58 4 minecraft:glass"
	keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
	check "Ctrl+Y redoes it" drv 2 "await block 14 -58 4 minecraft:oak_stairs" "await block 15 -59 5 minecraft:stone_bricks" \
		"await block 14 -59 6 minecraft:stone_bricks"

	# Repeated pastes into Hall: the first's stairs rise above Hall, which grows by a layer.
	drv 1 "movecopy 4 -57 6" > /dev/null
	press 1 k
	check "a second paste goes into Hall, which grows to fit it" drv 2 "await block 4 -56 6 minecraft:oak_stairs" \
		"await area Hall 0,0,0..6,4,6"
	drv 1 "movecopy 6 -57 2" > /dev/null
	press 1 k
	check "and a third" drv 2 "await block 6 -56 2 minecraft:oak_stairs" "await block 7 -57 4 minecraft:stone_bricks" \
		"await area Hall 0,0,0..6,4,6"

	# Copy from two plans: 6 of Hall's stone bricks and 4 of Barn's oak planks. Pasted outside every plan, the copy goes
	# into Hall, which most of its blocks came from, and Hall grows to fit it.
	drv 1 "select 6 -60 4 13 -60 5" > /dev/null
	press 1 j
	check "an area over two plans copies both" client_said 1 "Copied 10 blocks. Press K to paste them."
	drv 1 "movecopy 2 -60 20" > /dev/null
	check "the new copy replaces the old" eval '[[ $(val 1 copy) == "origin 2,-60,20 NONE NONE selected Building Planner copy" ]] &&
		[[ $(val 1 placements) != *"Building Planner copy"*"Building Planner copy"* ]]'
	press 1 k
	check "outside every plan, the paste goes into the plan it came from" drv 2 "await area Hall 0,0,0..7,4,19" \
		"await block 4 -60 21 minecraft:stone_bricks" "await block 9 -60 21 minecraft:oak_planks"

	# The copy's messages: an area with no plan blocks, and a single block. A refused copy leaves the earlier one.
	drv 1 "select 40 -60 40 42 -59 42" > /dev/null
	press 1 j
	check "J over no plan blocks gives a message" client_said 1 "There are no plan blocks in the marked area to copy."
	shot 1 copy-nothing-message
	check "and keeps the earlier copy" eval '[[ $(val 1 copy) == "origin 2,-60,20 "* ]]'
	drv 1 "select 4 -59 3 4 -59 3" > /dev/null
	press 1 j
	check "a single block has its own message" client_said 1 "Copied 1 block. Press K to paste it."
	shot 1 copy-one-message

	# Delete clears the copy; so does removing its placement in Litematica.
	press 1 Delete
	check "Delete clears the copy" eval 'drv 1 "await copy none" > /dev/null &&
		[[ $(val 1 placements) != *"Building Planner copy"* ]]'
	press 1 k
	check "K with no copy gives a message" \
		client_said 1 "Nothing to paste. Mark an area over a plan and press J to copy it."
	shot 1 paste-nothing-message
	press 1 j
	check "J copies again" drv 1 "await copy origin"
	drv 1 "removecopy" > /dev/null
	check "removing the copy's placement in Litematica clears the copy" drv 1 "await copy none"

	# With the copy outside every plan and the plan it came from unloaded, K gives a message.
	drv 1 "select 12 -60 2 12 -60 2" > /dev/null
	press 1 j
	drv 1 "await copy origin" "movecopy 40 -60 40" > /dev/null
	drv 1 "syncgui" "wait 20" > /dev/null
	read -r x y < <(val 1 "button Barn Unload")
	click 1 "$x" "$y"
	check "Planner1 unloads Barn" drv 1 "await noplacement Barn" "close"
	press 1 k
	check "K with no plan for the paste gives a message" \
		client_said 1 "Move the copy's corner into a plan first. The plan it came from isn't loaded."
	shot 1 paste-no-plan-message

	# The copy limit: Big's 51,200 plan blocks, filled in two halves, are more than the server's 50,000 for one edit.
	drv 1 "cmd tp @s 80 -60 -6 0 10" "cmd tp Planner2 81 -60 -6 0 10" "wait 40" > /dev/null
	check "Planner1 starts Big" start_plan Big "60 -60 2" "99 -29 41"
	drv 1 "fill 60 -60 2 99 -45 41 minecraft:stone" "wait 100" "fill 60 -44 2 99 -29 41 minecraft:stone" > /dev/null
	check "client 2 sees all of Big's blocks" drv 2 "await block 60 -60 2 minecraft:stone" "await block 99 -29 41 minecraft:stone"
	drv 1 "select 60 -60 2 99 -29 41" > /dev/null
	press 1 j
	check "J over more blocks than the server's limit gives a message" \
		client_said 1 "That area has 51200 plan blocks, more than the server's limit of 50000."
	shot 1 copy-too-many-message
	check "and keeps the earlier copy" eval '[[ $(val 1 copy) == "origin 40,-60,40 "* ]]'
fi

# The material list for a box drawn with the "Cube" shape, and its radial menu button: build step 11 writes these, as
# the spec's list of step9.sh's checks describes them.
if pending 11 "the material list"; then
	:
fi

# On the same server with Building Planner left out, the six keys do nothing.
stop_client 1
stop_client 2
cp /out/client1.log /out/client1-with-addon.log
cp /out/client2.log /out/client2-with-addon.log
stop_server
check "the server starts without Building Planner" eval 'start_server "$ADDON" server-without-addon &&
	! grep -qF "Building Planner loaded" /out/server-without-addon.log'
start_client 1
# The loading screen swallows key presses, so wait for it to close.
check "client 1 starts no session" drv 1 "await noscreen" "wait 40" "await nosession"
drv 1 "cmd tp @s 5 -60 -3 0 10" "select 2 -60 2 8 -57 8" "wait 20" > /dev/null
for key in n j k u h Delete; do
	press 1 "$key"
done
check "N, J, K, U, H and Delete make no copy, open nothing and leave ghost blocks shown" eval 'drv 1 "wait 20" \
	"await noscreen" > /dev/null && [[ $(val 1 placements) != *"Building Planner copy"* ]] &&
	grep -q "^ghost-blocks-shown=true" "$config"'
check "and show no Building Planner message" no_planner_message 1
shot 1 copy-keys-not-on-server
