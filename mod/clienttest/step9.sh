# Step 9's in-game checks: showing and hiding ghost blocks with N and the radial menu's button, drawing while they're
# hidden, the choice kept across a restart, where the radial menu's buttons sit, with screenshots, copy and paste, the
# material list, and the keys on a server without Building Planner. Sourced by inside.sh, which provides drv, val,
# check, keys, xdo, click, use_click, attack_click, client_said, start_server, stop_server, start_client, stop_client,
# no_planner_message and $ADDON. Players stand on the flat world's grass, whose top is at y -61.

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

# press <client> <key>: a real key press with no screen open. The game handles key events once a frame, which can come
# after several ticks, so it returns only once the game has handled the press and two ticks have passed, by which
# Building Planner's tick has acted on it.
press() {
	local before
	drv "$1" "close" "wait 5" > /dev/null
	before=$(val "$1" presses)
	keys "$1" key "$2"
	drv "$1" "await pressedafter $before" "wait 2" > /dev/null
}

# state_has <client> <x y z> <text>: the state Litematica shows there contains the text.
state_has() {
	[[ $(val "$1" "state $2") == *"$3"* ]]
}

# shot <client> <name>: a screenshot with the game's pop-ups cleared, written before the call returns.
shot() {
	drv "$1" "notoasts" "wait 5" "shot $2" "wait 20" > /dev/null
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

# Where the radial menu's buttons sit, with Turn, Mirror and Clear shown too, without a copy, in Circle mode, whose
# options fill two rows right of the ring, set before each opening since letting the key go over "Single" chooses it.
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
drv 1 "allbuttons true" "cmd tp @s 20.5 -58 -2.5 0 40" "wait 20" > /dev/null
press 1 n
check "the buttons meet nothing at 854 by 480, GUI scale 2" radial_shot 854 480 2 radial-all-854x480-gui2
check "nor at 1920 by 1080, GUI scale 2" radial_shot 1920 1080 2 radial-all-1920x1080-gui2
check "nor at GUI scale 4" radial_shot 1920 1080 4 radial-all-1920x1080-gui4
press 1 n
# The show or hide button's hover text, without a copy's buttons. The pointer leaves the button before the
# key is let go, so closing the menu leaves ghost blocks shown.
drv 1 "allbuttons false" "ebmode SINGLE" > /dev/null
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

# Without a copy, the menu shows four buttons, each with its hover text, at its middle from the menu's middle: show or
# hide, Copy and Paste in the row 39 above it, and Material list alone in the row 65 above it, at the left. Shot with
# the pointer on the menu's middle, over no button.
radial_open
xdo 1 mousemove --window %w 427 240
drv 1 "wait 10" "notoasts" "wait 5" "notoasts" "shot radial-no-copy" > /dev/null
echo "  radial-no-copy buttons: $(val 1 buttons)" >> /out/checks.log
check "without a copy, the menu shows its four buttons in their slots, with their hover texts" eval '[[ $(val 1 slots) == \
	"Show or hide ghost blocks [N] @ -157,-39 | Copy [J] @ -131,-39 | Paste [K] @ -105,-39 | Material list @ -157,-65" ]]'
xdo 1 mousemove --window %w 5 5
radial_let_go

# Copy and paste. Boxes are drawn with three real clicks of Effortless Building's "Cube" shape, aimed with the driver's
# lookat, which turns the player as a mouse move would.
# half <n>: n + 0.5, for negative n too.
half() {
	awk -v n="$1" 'BEGIN { print n + 0.5 }'
}
# aim <client> <x y z>: the player looks at the middle of the top of the block below x y z, so the copy's corner goes
# there.
aim() {
	drv "$1" "lookat $(half "$2") $3 $(half "$4")" "wait 5" > /dev/null
}
# cube <client> <x y z> <x2 z2> <y3> [<shot>]: draws a cube: the first click on the top of the block at x y z, which
# becomes a corner (hook E17); the second on the floor at x2 z2 (Effortless Building's level through that corner); the
# third at height y3 on the line above x2 z2. With a shot name, shoots the box before the third click.
cube() {
	drv "$1" "lookat $(half "$2") $(($3 + 1)) $(half "$4")" "wait 5" > /dev/null
	use_click "$1"
	drv "$1" "lookat $(half "$5") $3 $(half "$6")" "wait 5" > /dev/null
	use_click "$1"
	drv "$1" "lookat $5 $(half "$7") $6" "wait 10" > /dev/null
	[[ -z ${8:-} ]] || shot "$1" "$8"
	use_click "$1"
	drv "$1" "wait 5" > /dev/null
}
# copy_is <client> <pattern>: the copy, as the driver's copy describes it, matches the extended regular expression.
copy_is() {
	[[ $(val "$1" copy) =~ $2 ]]
}

# Both players fly, so each stays where a teleport puts them. Flying stops on the ground, so each is lifted first.
drv 1 "cmd tp @s ~ ~10 ~" "wait 5" "fly true" > /dev/null
drv 1 "cmd execute as Planner2 at @s run tp @s ~ ~10 ~" > /dev/null
drv 2 "wait 5" "fly true" > /dev/null

# J, then a cube from the stone brick at 5 -60 4 to 3 -59 3, around 6 stone bricks, the stairs and the glowstone.
drv 1 "ebmode SINGLE" "cmd tp @s 8.5 -56 9.5 0 30" "wait 40" > /dev/null
press 1 j
check "J waits for a box in Effortless Building's Cube mode" eval '[[ $(val 1 boxwait) == "true CUBE" ]]'
check "with a message" client_said 1 "Draw a box around the blocks to copy."
shot 1 copy-draw-box-message
cube 1 5 -60 4 3 3 -59 copy-box
check "the cube's last click copies the 8 ghost blocks in the box, with a message" \
	client_said 1 "Copied 8 blocks. Press K to paste them."
check "and puts back the player's mode" eval '[[ $(val 1 boxwait) == "false SINGLE" ]]'
check "and places no real block, leaving the ghost blocks" eval '[[ $(val 1 "real 4 -59 3") == minecraft:air ]] &&
	[[ $(val 1 "real 5 -60 4") == minecraft:air ]] && [[ $(val 1 "layer 4 -59 3") == minecraft:glowstone ]] &&
	[[ $(val 2 "get 5 -60 4") == minecraft:stone_bricks ]]'

# The copy follows the aim: its corner goes on the block beside the face aimed at.
drv 1 "cmd tp @s 5.5 -57 25.5 180 30" "wait 40" > /dev/null
aim 1 5 -60 20
check "the copy's corner sits on the aimed block" drv 1 "await copy origin 5,-60,20 NONE NONE unselected 8 blocks"
check "the copy shows the ghost blocks as they were" eval 'state_has 1 "5 -59 20" "oak_stairs" &&
	state_has 1 "5 -59 20" "facing=north" && [[ $(val 1 "get 6 -59 20") == minecraft:glowstone ]] &&
	[[ $(val 1 "get 7 -60 21") == minecraft:stone_bricks ]] && [[ $(val 1 "get 7 -59 21") == minecraft:air ]]'
check "and places no real block" eval '[[ $(val 1 "real 5 -59 20") == minecraft:air ]]'
check "and isn't shared" eval 'drv 2 "wait 20" > /dev/null && [[ $(val 2 "get 5 -59 20") == minecraft:air ]]'
check "Litematica neither selects the copy nor lists it" eval '[[ $(val 1 selected) != "the copy" ]] &&
	[[ $(val 1 placementlist) == *"others: none" ]]'
drv 1 "close" "cmd tp @s 30.5 -57 26.5 180 30" "wait 40" > /dev/null
aim 1 30 -60 20
check "the copy moves with the aim" drv 1 "await copy origin 30,-60,20 NONE NONE unselected"
shot 1 copy-follows-aim

# U gives it a quarter turn clockwise around its origin: the stairs now face east.
press 1 u
check "U turns the copy a quarter turn clockwise" drv 1 "await copy origin 30,-60,20 NONE CLOCKWISE_90"
check "its blocks turn with it" eval 'state_has 1 "30 -59 20" "facing=east" &&
	[[ $(val 1 "get 30 -59 21") == minecraft:glowstone ]] && [[ $(val 1 "get 29 -60 22") == minecraft:stone_bricks ]]'
shot 1 copy-turned

# H flips it left to right for Planner1, who faces south: the stairs face west, and the copy's floor moves from
# 29..30 to 30..31 east to west.
drv 1 "cmd tp @s 30.5 -57 14.5 0 30" "wait 40" > /dev/null
aim 1 30 -60 20
press 1 h
check "H mirrors the copy left to right" drv 1 "await copy origin 30,-60,20 FRONT_BACK COUNTERCLOCKWISE_90"
check "its blocks flip with it" eval 'state_has 1 "30 -59 20" "facing=west" &&
	[[ $(val 1 "get 31 -60 22") == minecraft:stone_bricks ]] && [[ $(val 1 "get 29 -60 22") == minecraft:air ]]'
shot 1 copy-mirrored

# Ghost glass where the copy will have empty space, and a ghost gold block where it will have stone bricks, on the
# oak planks; the copy moves over them.
drv 1 "edit 15 -58 4 15 -58 6 minecraft:glass" "edit 14 -58 6 14 -58 6 minecraft:glass" \
	"edit 15 -59 5 15 -59 5 minecraft:gold_block" > /dev/null
check "client 2 sees the glass and the gold block" drv 2 "await block 14 -58 6 minecraft:glass" \
	"await block 15 -59 5 minecraft:gold_block"
drv 1 "cmd tp @s 11.5 -55 1.5 0 30" "wait 40" > /dev/null
aim 1 14 -59 4
check "the copy moves over them" drv 1 "await copy origin 14,-59,4 FRONT_BACK COUNTERCLOCKWISE_90"
# Where the copy and the layer overlap, Litematica shows one block; the log records which.
echo "  where the copy overlaps ghost blocks: 15 -59 5 shows $(val 1 "get 15 -59 5") (copy stone_bricks, layer" \
	"gold_block); 15 -58 4 shows $(val 1 "get 15 -58 4") (copy empty, layer glass)" >> /out/checks.log
check "the copy's blocks show where they overlap, and the glass in its empty space" eval '
	[[ $(val 1 "get 15 -58 4") == minecraft:glass ]] && [[ $(val 1 "get 15 -59 5") == minecraft:stone_bricks ]]'
shot 1 copy-over-ghosts

# K pastes it where it shows: its 8 blocks, the stairs still facing west, and its empty space leaves the glass.
drv 1 "cmd tp Planner2 18.5 -57 11.5 150 30" "wait 40" > /dev/null
press 1 k
check "K pastes the copy where it shows, for the other player" drv 2 \
	"await block 14 -58 4 minecraft:oak_stairs" "await block 14 -58 5 minecraft:glowstone" \
	"await block 15 -59 5 minecraft:stone_bricks" "await block 14 -59 6 minecraft:stone_bricks"
check "glowing with Planner1's name" drv 2 "await glow Planner1:8"
shot 2 paste-seen
check "turned and mirrored as the copy showed it" state_has 2 "14 -58 4" "facing=west"
check "the copy's empty space leaves the ghost blocks under it" eval '[[ $(val 2 "get 15 -58 4") == minecraft:glass ]] &&
	[[ $(val 2 "get 15 -58 6") == minecraft:glass ]] && [[ $(val 2 "get 14 -58 6") == minecraft:glass ]]'
check "and the copy stays for another paste" copy_is 1 " FRONT_BACK COUNTERCLOCKWISE_90 unselected 8 blocks$"
echo "  after the paste, the copy, which now meets the pasted blocks in the aim: $(val 1 copy)" >> /out/checks.log

# One Ctrl+Z undoes the whole paste; Ctrl+Y redoes it.
drv 1 "close" > /dev/null
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "Ctrl+Z undoes the whole paste" drv 2 "await block 14 -58 4 minecraft:air" "await block 14 -58 5 minecraft:air" \
	"await block 15 -59 5 minecraft:gold_block" "await block 14 -59 6 minecraft:air" "await block 15 -58 4 minecraft:glass"
keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
check "Ctrl+Y redoes it" drv 2 "await block 14 -58 4 minecraft:oak_stairs" "await block 15 -59 5 minecraft:stone_bricks" \
	"await block 14 -59 6 minecraft:stone_bricks"

# The radial menu's copy buttons, with the helpers above. radial_click <name>: a real click on the open menu's button of
# that name, after which letting the key go acts on nothing.
radial_click() {
	radial_point "$1"
	xdo 1 click 1
	drv 1 "wait 5" > /dev/null
}

# The radial menu's Copy button around a real house of oak planks, hollow, with a glass window: 26 real blocks, and no
# ghost blocks.
drv 1 "cmd fill 50 -60 10 52 -58 12 minecraft:oak_planks hollow" "cmd setblock 51 -59 10 minecraft:glass" \
	"cmd tp @s 46.5 -56 6.5 0 30" "wait 40" > /dev/null
check "the house is real" eval '[[ $(val 1 "real 51 -59 10") == minecraft:glass ]] &&
	[[ $(val 1 "real 51 -59 11") == minecraft:air ]] && [[ $(val 1 "layer 50 -60 10") == minecraft:air ]]'
draw_lines=$(chat_count 1 "Draw a box around the blocks to copy.")
radial_open
radial_click "Copy"
check "the radial menu's Copy button waits for a box" eval '[[ $(val 1 boxwait) == "true CUBE" ]]'
radial_let_go
check "with a message" said_once_more 1 "Draw a box around the blocks to copy." "$draw_lines"
cube 1 52 -58 12 50 10 -60 copy-real-box
check "a box around only real blocks copies them: the house's 26 blocks" \
	client_said 1 "Copied 26 blocks. Press K to paste them."

# Two ghost blocks on the house: a gold block inside, where the world has air, and stone at its top corner, over the
# real oak planks there. J and the same box copy the ghost blocks and the real blocks together: 27 blocks, taking the
# ghost stone where a position has both. Client 2 shows them; client 1's copy of the house sits there now.
drv 1 "edit 51 -59 11 51 -59 11 minecraft:gold_block" "edit 52 -58 12 52 -58 12 minecraft:stone" > /dev/null
check "client 2 sees the two ghost blocks" drv 2 "await block 51 -59 11 minecraft:gold_block" \
	"await block 52 -58 12 minecraft:stone"
check "and on client 1 the ghost stone sits over a real oak plank" eval '
	[[ $(val 1 "real 52 -58 12") == minecraft:oak_planks ]] && [[ $(val 1 "layer 52 -58 12") == minecraft:stone ]] &&
	[[ $(val 1 "layer 51 -59 11") == minecraft:gold_block ]]'
press 1 j
cube 1 52 -58 12 50 10 -60 copy-both-box
check "a box around ghost blocks and real blocks copies both: 27 blocks" \
	client_said 1 "Copied 27 blocks. Press K to paste them."
drv 1 "cmd tp @s 51.5 -56 27.5 180 30" "wait 40" > /dev/null
aim 1 50 -60 20
check "the copy shows the house with the gold block inside" eval '
	drv 1 "await copy origin 50,-60,20 NONE NONE unselected 27 blocks" > /dev/null &&
	[[ $(val 1 "get 51 -59 20") == minecraft:glass ]] && [[ $(val 1 "get 51 -59 21") == minecraft:gold_block ]] &&
	[[ $(val 1 "get 50 -58 22") == minecraft:oak_planks ]]'
check "and the ghost stone, not the real oak plank, at its corner" eval '[[ $(val 1 "get 52 -58 22") == minecraft:stone ]]'
radial_open
radial_point "Paste"
radial_let_go
check "letting the menu's key go over its Paste button pastes them as ghost blocks" drv 2 \
	"await block 50 -60 20 minecraft:oak_planks" "await block 50 -58 22 minecraft:oak_planks" \
	"await block 51 -59 20 minecraft:glass" "await block 51 -59 21 minecraft:gold_block"
check "the ghost stone among them, where the copy had a ghost block over a real one" drv 2 \
	"await block 52 -58 22 minecraft:stone"
check "and no real blocks" eval '[[ $(val 1 "real 51 -59 20") == minecraft:air ]] &&
	[[ $(val 1 "real 52 -58 22") == minecraft:air ]]'
shot 1 copy-both-pasted

# With ghost blocks hidden, the same box copies the house's real blocks only: 26, with the real oak plank where the ghost
# stone stands and nothing where the ghost gold block is; ghost blocks stay hidden. Then they show again and the box
# copies the 27 blocks once more, which the checks below use.
press 1 n
check "ghost blocks are hidden before the copy" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) == "hidden, "* ]]'
real_lines=$(chat_count 1 "Copied 26 blocks. Press K to paste them.")
drv 1 "cmd tp @s 46.5 -56 6.5 0 30" "wait 40" > /dev/null
press 1 j
cube 1 52 -58 12 50 10 -60 copy-hidden-box
check "with ghost blocks hidden, the same box copies the house's 26 real blocks" \
	said_once_more 1 "Copied 26 blocks. Press K to paste them." "$real_lines"
check "and ghost blocks stay hidden" eval '[[ $(val 1 ghosts) == "hidden, "* ]]'
drv 1 "cmd tp @s 51.5 -56 27.5 180 30" "wait 40" > /dev/null
aim 1 50 -60 20
check "the copy has the real oak plank where the ghost stone stands, and no gold block" eval '
	drv 1 "await copy origin 50,-60,20 NONE NONE unselected 26 blocks" > /dev/null &&
	[[ $(val 1 "get 52 -58 22") == minecraft:oak_planks ]] && [[ $(val 1 "get 51 -59 21") != minecraft:gold_block ]] &&
	[[ $(val 1 "get 51 -59 20") == minecraft:glass ]]'
shot 1 copy-hidden
press 1 n
check "ghost blocks show again" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) == "shown, "* ]]'
drv 1 "cmd tp @s 46.5 -56 6.5 0 30" "wait 40" > /dev/null
press 1 j
cube 1 52 -58 12 50 10 -60
check "the copy is the house with the gold block and the ghost stone again: 27 blocks" \
	eval 'drv 1 "wait 5" > /dev/null && copy_is 1 " NONE NONE unselected 27 blocks$"'

# With a copy, the menu shows Turn, Mirror and Clear, in the row above the others; shot at 1920 by 1080, GUI scales 2
# and 4, with the pointer off the buttons, and failing unless the buttons meet nothing.
radial_copy_shot() {
	local result
	drv 1 "window 1920 1080 $1" "wait 20" > /dev/null
	radial_open
	xdo 1 mousemove --window %w 5 5
	drv 1 "wait 10" "notoasts" "wait 5" "notoasts" "shot radial-copy-1920x1080-gui$1" > /dev/null
	echo "  radial-copy-1920x1080-gui$1 buttons: $(val 1 buttons)" >> /out/checks.log
	result=$(val 1 radialclear)
	echo "  radial-copy-1920x1080-gui$1: $result" >> /out/checks.log
	radial_let_go
	[[ $result == clear ]]
}
check "with a copy, the menu shows Turn, Mirror and Clear, meeting nothing at 1920 by 1080, GUI scale 2" \
	eval 'radial_copy_shot 2 && grep -q "radial-copy-1920x1080-gui2 buttons: .*Turn the copy.*Mirror the copy.*Clear the copy" /out/checks.log'
check "nor at GUI scale 4" radial_copy_shot 4
drv 1 "window 854 480 2" "wait 20" > /dev/null
radial_open
xdo 1 mousemove --window %w 427 240
drv 1 "wait 10" "notoasts" "wait 5" "notoasts" "shot radial-with-copy" > /dev/null
echo "  radial-with-copy buttons: $(val 1 buttons)" >> /out/checks.log
check "with a copy, the menu's seven buttons sit in their slots, with their hover texts" eval '[[ $(val 1 slots) == \
	"Show or hide ghost blocks [N] @ -157,-39 | Copy [J] @ -131,-39 | Paste [K] @ -105,-39 | Material list @ -157,-65 | Turn the copy [U] @ -157,-91 | Mirror the copy [H] @ -131,-91 | Clear the copy [Delete] @ -105,-91" ]]'
xdo 1 mousemove --window %w 5 5
radial_let_go

# The radial menu's Turn, Mirror and Copy buttons, each by a click.
radial_open
radial_click "Turn the copy"
check "the radial menu's Turn button turns the copy" copy_is 1 " NONE CLOCKWISE_90 unselected 27 blocks$"
radial_click "Mirror the copy"
check "its Mirror button mirrors it" copy_is 1 " (FRONT_BACK|LEFT_RIGHT) [A-Z_0-9]+ unselected 27 blocks$"
radial_click "Copy"
check "its Copy button waits for a box" eval '[[ $(val 1 boxwait) == "true CUBE" ]]'
radial_click "Copy"
check "and pressed again, stops waiting and puts back the mode" eval '[[ $(val 1 boxwait) == "false SINGLE" ]]'
radial_let_go
earlier=$(val 1 copy | cut -d' ' -f3-)

# A box with no blocks in it. The cube's first corner is the aimed block, so the first click lands on a real stone at
# 42 -59 42, which goes before the other two finish the box, from there to 40 -58 40, in the air.
drv 1 "cmd tp @s 45.5 -57 45.5 0 30" "cmd setblock 42 -59 42 minecraft:stone" "wait 40" > /dev/null
press 1 j
drv 1 "lookat 42.5 -58 42.5" "wait 5" > /dev/null
use_click 1
drv 1 "cmd setblock 42 -59 42 minecraft:air" "await real 42 -59 42 minecraft:air" "lookat 40.5 -59 40.5" "wait 5" \
	> /dev/null
use_click 1
drv 1 "lookat 40 -57.5 40" "wait 10" > /dev/null
use_click 1
drv 1 "wait 5" > /dev/null
check "a box with no blocks gives a message" client_said 1 "There are no blocks in that box to copy."
shot 1 copy-nothing-message
check "keeps the earlier copy and puts back the mode" eval '[[ $(val 1 copy | cut -d" " -f3-) == "$earlier" ]] &&
	[[ $(val 1 boxwait) == "false SINGLE" ]]'

# The block cap: Big's 51,200 ghost blocks, added in two halves, are more than the server's 50,000 for one edit.
drv 1 "cmd tp @s 55.5 -20 -3.5 0 30" "cmd tp Planner2 56.5 -20 -3.5 0 30" "wait 40" > /dev/null
drv 1 "edit 60 -60 2 99 -45 41 minecraft:stone" "wait 100" "edit 60 -44 2 99 -29 41 minecraft:stone" > /dev/null
check "client 2 sees all of Big's blocks" drv 2 "await block 60 -60 2 minecraft:stone" "await block 99 -29 41 minecraft:stone"
press 1 j
cube 1 99 -29 41 60 2 -60
check "a cube over more blocks than the server's limit gives a message" \
	client_said 1 "That box has 51200 blocks, more than the server's limit of 50000."
shot 1 copy-too-many-message
check "and keeps the earlier copy" eval '[[ $(val 1 copy | cut -d" " -f3-) == "$earlier" ]]'

# Delete clears the copy; K with no copy gives a message.
press 1 Delete
check "Delete clears the copy" drv 1 "await copy none"
press 1 k
check "K with no copy gives a message" client_said 1 "Nothing to paste. Press J to copy blocks first."
shot 1 paste-nothing-message

# A box of one block: the cube's three clicks on the glowstone at 4 -59 3.
drv 1 "cmd tp @s 8.5 -56 9.5 0 30" "wait 40" > /dev/null
press 1 j
cube 1 4 -59 3 4 3 -59
check "a single block has its own message" client_said 1 "Copied 1 block. Press K to paste it."
shot 1 copy-one-message

# The radial menu's Clear button, then its Paste button with no copy; the copy's own buttons go with the copy.
radial_open
radial_click "Clear the copy"
check "the radial menu's Clear button clears the copy" drv 1 "await copy none"
check "and the menu hides Turn, Mirror and Clear" eval '[[ $(val 1 "widget 0 Turn the copy") == none ]] &&
	[[ $(val 1 "widget 0 Mirror the copy") == none ]] && [[ $(val 1 "widget 0 Clear the copy") == none ]] &&
	[[ $(val 1 "widget 0 Paste") != none ]]'
radial_click "Paste"
check "its Paste button with no copy gives the message" eval \
	'(($(grep -cF "[CHAT] Nothing to paste. Press J to copy blocks first." /out/client1.log) >= 2))'
radial_let_go

# Stopping a wait: J again, Delete, or another mode, which stays.
press 1 j
press 1 j
check "J pressed again stops waiting and puts back the mode" eval '[[ $(val 1 boxwait) == "false SINGLE" ]]'
press 1 j
press 1 Delete
check "Delete stops waiting too" eval '[[ $(val 1 boxwait) == "false SINGLE" ]]'
press 1 j
drv 1 "ebmode LINE" "wait 5" > /dev/null
check "and choosing another mode ends the wait, keeping that mode" eval '[[ $(val 1 boxwait) == "false LINE" ]]'
drv 1 "ebmode SINGLE" "fly false" > /dev/null
drv 2 "fly false" > /dev/null

# The material list. Its radial menu button's hover text, shot at 1920 by 1080, GUI scales 2 and 4; the pointer leaves
# the button before the key is let go, so closing the menu starts no wait.
for scale in 2 4; do
	drv 1 "window 1920 1080 $scale" "wait 20" > /dev/null
	radial_open
	radial_point "Material list"
	drv 1 "wait 10" "notoasts" "shot radial-material-list-hover-1920x1080-gui$scale" > /dev/null
	echo "  radial-material-list-hover-1920x1080-gui$scale buttons: $(val 1 buttons)" >> /out/checks.log
	xdo 1 mousemove --window %w 5 5
	radial_let_go
done
check "the hover shots start no wait" eval '[[ $(val 1 boxwait) == "false SINGLE" ]]'
drv 1 "window 854 480 2" "wait 20" > /dev/null

# A row of 10 ghost stone bricks, 4 of them built for real. Both players fly again, lifted first.
drv 1 "cmd tp @s 24.5 -51 -4.5 180 30" "cmd tp Planner2 24.5 -51 -3.5 180 30" "wait 40" "fly true" > /dev/null
drv 2 "fly true" > /dev/null
drv 1 "cmd tp @s 24.5 -56 -4.5 180 30" "edit 20 -60 -10 29 -60 -10 minecraft:stone_bricks" \
	"cmd fill 20 -60 -10 23 -60 -10 minecraft:stone_bricks" "wait 40" > /dev/null
check "the row shows, 4 of its bricks real" eval 'drv 1 "await block 29 -60 -10 minecraft:stone_bricks" > /dev/null &&
	[[ $(val 1 "real 23 -60 -10") == minecraft:stone_bricks ]] && [[ $(val 1 "real 24 -60 -10") == minecraft:air ]]'

# A copy of the row, which follows the aim over the box below, and ghost blocks hidden.
press 1 j
cube 1 20 -60 -10 29 -10 -60
check "a copy of the row" client_said 1 "Copied 10 blocks. Press K to paste them."
press 1 n
check "ghost blocks are hidden" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) == "hidden, "* ]]'

# Letting the menu's key go over its Material list button waits for a box, with a message; a cube two blocks high
# around the row, starting on its first real brick, ends it. The list's screen takes the button from the game as it
# opens, so the game never sees the third click held: it's a plain press, held for 10 ticks.
# material_box <press>: the three clicks of that box, the third held for 10 ticks when <press> is "held" and a plain
# click otherwise. With no list to open, a held click would go on to act in the player's mode.
material_box() {
	drv 1 "lookat 20.5 -59 -9.5" "wait 5" > /dev/null
	use_click 1
	drv 1 "lookat 29.5 -60 -9.5" "wait 5" > /dev/null
	use_click 1
	drv 1 "lookat 29 -58.5 -10" "wait 10" > /dev/null
	if [[ $1 == held ]]; then
		xdo 1 mousedown 3
		drv 1 "wait 10" > /dev/null
		xdo 1 mouseup 3
	else
		use_click 1
	fi
	drv 1 "wait 10" > /dev/null
}

# With ghost blocks hidden, the box counts as if it held none, so nothing opens, with the message for a box without
# ghost blocks, and ghost blocks stay hidden.
wait_lines=$(chat_count 1 "Draw a box around the ghost blocks to list their materials.")
none_lines=$(chat_count 1 "There are no ghost blocks in that box to list.")
radial_open
radial_point "Material list"
radial_let_go
check "letting the menu's key go over its Material list button waits for a box in Cube mode" eval \
	'[[ $(val 1 boxwait) == "true CUBE" ]]'
check "with a message" said_once_more 1 "Draw a box around the ghost blocks to list their materials." "$wait_lines"
material_box plain
check "with ghost blocks hidden, the cube lists none of the row's ghost blocks, with the message" \
	said_once_more 1 "There are no ghost blocks in that box to list." "$none_lines"
check "opens no list" eval '[[ $(val 1 matlist) == closed* ]]'
check "and ghost blocks stay hidden" eval '[[ $(val 1 ghosts) == "hidden, "* ]]'
check "the cube puts back the player's mode" eval '[[ $(val 1 boxwait) == "false SINGLE" ]]'

# With ghost blocks shown, the same box opens Litematica's material list.
press 1 n
check "ghost blocks show again" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) == "shown, "* ]]'
radial_open
radial_point "Material list"
radial_let_go
material_box held
echo "  the material list: $(val 1 matlist); the copy: $(val 1 copy)" >> /out/checks.log
check "the cube opens Litematica's material list: 10 stone bricks in all, 6 missing" eval '[[ $(val 1 matlist) == \
	"open Material list for the ghost blocks in the box: total 10 missing 6 wrong 0 | minecraft:stone_bricks 10/6" ]]'
check "ghost blocks still show" eval '[[ $(val 1 ghosts) == "shown, "* ]]'
check "the cube places and erases nothing, and puts back the player's mode" eval '
	[[ $(val 1 "real 24 -60 -10") == minecraft:air ]] && [[ $(val 1 "real 20 -59 -10") == minecraft:air ]] &&
	[[ $(val 1 "layer 20 -60 -10") == minecraft:stone_bricks ]] && [[ $(val 1 boxwait) == "false SINGLE" ]]'
check "and leaves the copy" copy_is 1 " 10 blocks$"
for scale in 2 4; do
	drv 1 "window 1920 1080 $scale" "wait 20" "notoasts" "shot material-list-1920x1080-gui$scale" > /dev/null
done
drv 1 "window 854 480 2" "wait 20" "close" "wait 5" > /dev/null

# A box with no ghost blocks opens nothing, with a message; the Material list button during a Copy wait stops it.
# The box, from 42 -61 42 to 40 -60 40, holds the grass and no ghost blocks.
press 1 Delete
drv 1 "cmd tp @s 45.5 -57 45.5 0 30" "wait 40" > /dev/null
none_lines=$(chat_count 1 "There are no ghost blocks in that box to list.")
radial_open
radial_click "Material list"
radial_let_go
cube 1 42 -61 42 40 40 -60
check "a box with no ghost blocks gives a message" \
	said_once_more 1 "There are no ghost blocks in that box to list." "$none_lines"
check "and opens nothing" eval 'drv 1 "wait 10" > /dev/null && [[ $(val 1 matlist) == closed:* ]]'
press 1 j
radial_open
radial_click "Material list"
check "the Material list button during a Copy wait stops it, starting no other" eval \
	'[[ $(val 1 boxwait) == "false SINGLE" ]]'
radial_let_go
drv 1 "fly false" > /dev/null
drv 2 "fly false" > /dev/null

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
drv 1 "cmd tp @s 5 -60 -3 0 10" "wait 20" > /dev/null
for key in n j k u h Delete; do
	press 1 "$key"
done
check "N, J, K, U, H and Delete make no copy, open nothing and leave ghost blocks shown" eval 'drv 1 "wait 20" \
	"await noscreen" > /dev/null && [[ $(val 1 placements) != *"Building Planner copy"* ]] &&
	grep -q "^ghost-blocks-shown=true" "$config"'
check "and show no Building Planner message" no_planner_message 1
shot 1 copy-keys-not-on-server
