# Step 9's in-game checks: showing and hiding ghost blocks with N and the radial menu's button, drawing while they're
# hidden, the choice kept across a restart, where the radial menu's buttons sit, with screenshots, the menu's corner
# text, the key headings in Controls, the box with its
# list under the crosshair, copy and paste with the copy at the aim worked by the mouse, the Delete box, the material
# list, the counted
# list kept on the server across restarts, the list screen's buttons, saving, opening and deleting lists, the lists
# across restarts and on another computer, a damaged lists file, the server's limits, Refresh far from the box, and N on
# a server without Building Planner. Sourced by inside.sh, which provides drv, val,
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
# radial_click <name>: a real click on the open menu's button of that name, after which letting the key go acts on
# nothing.
radial_click() {
	radial_point "$1"
	xdo 1 click 1
	drv 1 "wait 5" > /dev/null
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

# Litematica's area selection. A player who left a simple selection on the server before Building Planner turned
# Litematica's tool off has no tool to change or clear it, and it kept drawing as a large see-through box, with its
# origin as a one-block outline. Here client 1 makes a selection of 4 by 3 by 4 blocks on the grass with an origin of
# its own beside it, and turns Litematica's drawing of selections on, as it is by default, and the view is shot with
# the selection and with it moved far away, with ghost blocks hidden so nothing else is in it.
# The selection is then saved, with the option on and with the record that 0.3.0 and 0.4.0 leave (the look group's,
# kept) but not the area selection group's, as for a player who updates; after the restart, the option is off, the
# selection and its origin are still there, and the same view shows no more difference with it and with it moved away
# than the sky and the grass's light make between two shots (some hundreds of pixels). shotdiff counts the pixels
# two shots differ at.
# The animals the flat world starts with wander through the view, so they're killed first.
area_clear="cmd kill @e[type=!minecraft:player]"
area_view="cmd tp @s 31.5 -57 24.5 0 35"
area_box="SIMPLE 30,-61,30..33,-59,33 origin 35,-61,31"
drv 1 "$area_clear" "$area_view" "selbox 30 -61 30 33 -59 33 35 -61 31" "setareaoption true" "wait 40" > /dev/null
check "Litematica holds the selection, which it draws at its default" eval \
	'[[ $(val 1 selinfo) == "$area_box" ]] && [[ $(val 1 areaoption) == true ]]'
shot 1 area-selection-drawn
drv 1 "selaway" "wait 40" > /dev/null
shot 1 area-selection-away
area_drawn=$(val 1 "shotdiff area-selection-drawn area-selection-away")
echo "  pixels the selection changes: $area_drawn" >> /out/checks.log
check "with the option on, the view with the selection differs from the view with it moved away" eval '((area_drawn > 10000))'
drv 1 "selbox 30 -61 30 33 -59 33 35 -61 31" "wait 5" > /dev/null
stop_client 1
check "the first start wrote the look group's record, which 0.3.0 and 0.4.0 write, and the area selection group's" eval \
	'grep -q "^look-defaults-applied=true" "$config" && grep -q "^area-selection-off-applied=true" "$config"'
sed -i '/^area-selection-off-applied=/d' "$config"
cp /out/client1.log /out/client1-before-restart.log
start_client 1
check "after a restart, client 1 starts a session with ghost blocks still hidden" eval \
	'drv 1 "await session" "await noscreen" "wait 60" > /dev/null &&
	 [[ $(val 1 ghosts) =~ ^hidden,\ [1-9][0-9]*\ tiles,\ 0\ placements$ ]] &&
	 [[ $(val 1 "layer 4 -59 3") == minecraft:glowstone ]] && [[ $(val 1 "get 4 -59 3") == minecraft:air ]]'
check "Building Planner turned the drawing of area selections off, once" eval \
	'[[ $(val 1 areaoption) == false ]] && grep -q "^area-selection-off-applied=true" "$config" &&
	 grep -q "^look-defaults-applied=true" "$config"'
check "and the selection from before, with its origin, is still there" eval '[[ $(val 1 selinfo) == "$area_box" ]]'
drv 1 "$area_clear" "$area_view" "wait 40" > /dev/null
shot 1 area-selection-hidden
drv 1 "selaway" "wait 40" > /dev/null
shot 1 area-selection-hidden-away
area_left=$(val 1 "shotdiff area-selection-hidden area-selection-hidden-away")
echo "  pixels the selection changes with the option off: $area_left" >> /out/checks.log
check "the selection left from before is no longer drawn" eval '((area_left < 3000))'
press 1 n
check "and N shows them" drv 1 "await block 4 -59 3 minecraft:glowstone"

# Where the radial menu's buttons sit, without a copy, in Circle mode, whose options fill two rows right of the ring,
# set before each opening since letting the key go over "Single" chooses it.
# radial_shot <width> <height> <gui scale> <name> opens the menu in a window of that size at that GUI scale, with the
# pointer over "Single" (about 33 right of the menu's middle and 35 above it) so its name and description show, and
# shoots it as <name>, and 1 and 2 frames after it opens as <name>-frame1 and <name>-frame2, which it keeps in frames.
# The buttons' boxes go to the log, for outlining. It fails unless the buttons meet nothing the menu draws, the
# sliders, each other or the screen's edges.
radial_shot() {
	local result
	drv 1 "ebmode CIRCLE" "window $1 $2 $3" "wait 20" "notoasts" "menuframes arm $4" > /dev/null
	radial_open
	xdo 1 mousemove --window %w $(($1 / 2 + 33 * $3)) $(($2 / 2 - 35 * $3))
	drv 1 "wait 10" "notoasts" "wait 5" "notoasts" "shot $4" > /dev/null
	echo "  $4 buttons: $(val 1 buttons)" >> /out/checks.log
	frames=$(val 1 menuframes)
	echo "  $4 frames: $frames" >> /out/checks.log
	result=$(val 1 radialclear)
	echo "  $4: $result" >> /out/checks.log
	radial_let_go
	[[ $result == clear ]]
}
# The buttons join the menu's opening animation: 1 and 2 frames after it opens, each is nearer the middle and smaller
# than once it's open, as the menu's own are. The animation runs on the game's ticks, over 2.5 of them, and the virtual
# screen's software drawing takes several ticks for a frame after a shot, where a player's game draws several frames a
# tick; so for these shots the server runs 1 tick a second instead of 20, which the game follows, and each frame takes
# a twentieth as much of the animation.
frames_grow() {
	[[ $frames == *"; nearer and smaller" ]]
}
# With ghost blocks hidden, so these shots and the hover shots below show every icon between them.
drv 1 "cmd tp @s 20.5 -58 -2.5 0 40" "wait 20" > /dev/null
press 1 n
drv 1 "cmd tick rate 1" "wait 2" > /dev/null
check "the buttons meet nothing at 854 by 480, GUI scale 2" radial_shot 854 480 2 radial-all-854x480-gui2
check "and 1 and 2 frames after the menu opens, they're nearer its middle and smaller" frames_grow
check "nor at 1920 by 1080, GUI scale 2" radial_shot 1920 1080 2 radial-all-1920x1080-gui2
check "where they grow with the menu too" frames_grow
check "nor at GUI scale 4" radial_shot 1920 1080 4 radial-all-1920x1080-gui4
check "where they grow with the menu too" frames_grow
drv 1 "cmd tick rate 20" "wait 5" > /dev/null
press 1 n
# The show or hide button's and the Delete button's hover texts. The pointer leaves the button before the key is let
# go, so closing the menu leaves ghost blocks shown and starts no box.
drv 1 "ebmode SINGLE" > /dev/null
for scale in 2 4; do
	drv 1 "window 1920 1080 $scale" "wait 20" > /dev/null
	radial_open
	radial_point "Show or hide ghost blocks"
	drv 1 "wait 10" "notoasts" "shot radial-hover-1920x1080-gui$scale" > /dev/null
	echo "  radial-hover-1920x1080-gui$scale buttons: $(val 1 buttons)" >> /out/checks.log
	radial_point "Delete"
	drv 1 "wait 10" "notoasts" "shot radial-delete-hover-1920x1080-gui$scale" > /dev/null
	echo "  radial-delete-hover-1920x1080-gui$scale buttons: $(val 1 buttons)" >> /out/checks.log
	xdo 1 mousemove --window %w 5 5
	radial_let_go
done
check "ghost blocks still show after the hover shots" eval '[[ $(val 1 ghosts) == "shown, "* ]]'
check "and no box started" eval '[[ $(val 1 drawbox) == none* ]]'
drv 1 "window 854 480 2" "wait 20" > /dev/null

# Without a copy, the menu shows seven buttons, each with its hover text, at its middle from the menu's middle: show or
# hide, Block mix and Material list in the row 39 above it, Copy, Paste and Material lists in the row 65 above it, at
# the left, and Delete 91 above it, over Copy.
# Shot with the pointer on the menu's middle, over no button, then with the pointer on Block mix, for its hover text.
slots="Show or hide ghost blocks [N] @ -157,-39 | Block mix [B] @ -131,-39 | Material list @ -105,-39 | \
Copy @ -157,-65 | Paste @ -131,-65 | Material lists @ -105,-65 | Delete @ -157,-91"
radial_open
xdo 1 mousemove --window %w 427 240
drv 1 "wait 10" "notoasts" "wait 5" "notoasts" "shot radial-no-copy" > /dev/null
echo "  radial-no-copy buttons: $(val 1 buttons)" >> /out/checks.log
echo "  radial-no-copy corner text: $(val 1 cornertext)" >> /out/checks.log
check "without a copy, the menu shows its seven buttons in their slots, with their hover texts" \
	eval '[[ $(val 1 slots) == "$slots" ]]'
# Building Planner's name, where Effortless Building draws its own: the text at the menu's lower right.
check "the menu's corner text reads \"Building Planner\"" eval '[[ $(val 1 cornertext) == "Building Planner | "* ]]'
check "and the menu draws no \"Effortless Building\"" eval '[[ $(val 1 drawntexts) != *"Effortless Building"* ]]'
radial_point "Delete"
drv 1 "wait 10" "notoasts" "shot radial-delete-hover-854x480-gui2" > /dev/null
echo "  radial-delete-hover-854x480-gui2 buttons: $(val 1 buttons)" >> /out/checks.log
radial_point "Block mix"
drv 1 "wait 10" "notoasts" "shot radial-block-mix-hover" > /dev/null
xdo 1 mousemove --window %w 5 5
drv 1 "wait 5" > /dev/null
check "letting the key go with the pointer moved off Block mix closes the menu and opens nothing" radial_let_go

# Controls: Effortless Building's four keys show under the "Building Planner" heading, with Building Planner's own, in
# the order Controls sorts them, and no raw key name. The list is scrolled to the heading and shot at 854 by 480, with
# the heading's and the keys' window boxes (left top right bottom) in the log.
drv 1 "keybinds" "wait 10" > /dev/null
echo "  controls: $(val 1 keybindlist)" >> /out/checks.log
planner_keys="Block mix, Open Modifier Settings, Open Radial Menu, Redo, Show or hide ghost blocks, Undo"
check "Controls shows Effortless Building's four keys under \"Building Planner\", with Building Planner's own" eval \
	'[[ $(val 1 keybindlist) == *"Building Planner: $planner_keys"* ]]'
check "under one heading" eval '(($(val 1 keybindlist | grep -o "Building Planner: " | wc -l) == 1))'
check "with no raw key name" eval '[[ $(val 1 keybindlist) != *"key."* ]]'
echo "  controls boxes: $(val 1 "keybindscroll Building Planner")" >> /out/checks.log
drv 1 "wait 5" "notoasts" > /dev/null
shot 1 controls-854x480
drv 1 "close" "wait 5" > /dev/null

# The Paste button before any copy gives a message.
nothing_lines=$(chat_count 1 "Nothing to paste. Use Copy first.")
radial_open
radial_click "Paste"
radial_let_go
check "the radial menu's Paste button with no copy gives a message" \
	said_once_more 1 "Nothing to paste. Use Copy first." "$nothing_lines"
check "and shows no copy" eval '[[ $(val 1 copy) == none ]] && [[ $(val 1 modehud) == hidden ]]'
shot 1 paste-nothing-message

# Boxes, copy and paste. Boxes are drawn with three real left clicks, aimed with the driver's lookat, which turns the
# player as a mouse move would.
# half <n>: n + 0.5, for negative n too.
half() {
	awk -v n="$1" 'BEGIN { print n + 0.5 }'
}
# aim <client> <x y z>: the player looks at the middle of the top of the block below x y z, so the copy rests on that
# top with the middle of its bottom at x y z.
aim() {
	drv "$1" "lookat $(half "$2") $3 $(half "$4")" "wait 5" > /dev/null
}
# box <client> <ax ay az> <y1> <x2 z2> <y3> [<shot>]: draws a box with three left clicks: the first aimed at the point
# ax ay az, on a block's top face for the space above it or on its side for the block itself, which becomes the first
# corner, on level y1; the second at the middle of the block x2 y1 z2, where the line of sight crosses that level; the
# third at the middle of the block at height y3 above x2 z2. With a shot name, shoots the box before the third click.
box() {
	drv "$1" "lookat $2 $3 $4" "wait 5" > /dev/null
	attack_click "$1"
	drv "$1" "lookat $(half "$6") $5 $(half "$7")" "wait 5" > /dev/null
	attack_click "$1"
	drv "$1" "lookat $(half "$6") $(half "$8") $(half "$7")" "wait 10" > /dev/null
	[[ -z ${9:-} ]] || shot "$1" "$9"
	attack_click "$1"
	drv "$1" "wait 5" > /dev/null
}
# copy_is <client> <pattern>: the copy, as the driver's copy describes it, matches the extended regular expression.
copy_is() {
	[[ $(val "$1" copy) =~ $2 ]]
}
# box_is <text>: Planner1's box, as the driver's drawbox describes it, followed by Effortless Building's mode, is the
# text.
box_is() {
	[[ $(val 1 drawbox) == "$1" ]]
}
# chat_lines <client>: how many chat lines that client has shown.
chat_lines() {
	grep -c '\[CHAT\]' "/out/client$1.log" || true
}

# Both players fly, so each stays where a teleport puts them. Flying stops on the ground, so each is lifted first.
drv 1 "cmd tp @s ~ ~10 ~" "wait 5" "fly true" > /dev/null
drv 1 "cmd execute as Planner2 at @s run tp @s ~ ~10 ~" > /dev/null
drv 2 "wait 5" "fly true" > /dev/null

# The copy at the aim's controls line, and a box's controls for each click.
controls="Left-click to paste, Scroll to turn, Middle-click to mirror, Right-click to close"
first_line="Left-click to set the first corner, Right-click to cancel"
second_line="Left-click to set the opposite corner, Right-click to cancel"
height_line="Left-click to set the height, Right-click to cancel"
# copy_button, delete_button and paste_button: a click on the radial menu's Copy, Delete or Paste button.
copy_button() {
	radial_open
	radial_click "Copy"
	radial_let_go
}
delete_button() {
	radial_open
	radial_click "Delete"
	radial_let_go
}
paste_button() {
	radial_open
	radial_click "Paste"
	radial_let_go
}
# copy_kept: the copy as the driver describes it, without where it is.
copy_kept() {
	val 1 copy | sed -E 's/^(at [^ ]+|away) //'
}
# shots <name>: Planner1's view at 854 by 480 and at 1920 by 1080, both at GUI scale 2, as <name>-854x480 and
# <name>-1920x1080, once the chat lines that draw over the controls line have faded, 200 ticks after they came.
shots() {
	drv 1 "wait 210" "notoasts" "wait 5" "shot $1-854x480" "window 1920 1080 2" "wait 20" "notoasts" \
		"shot $1-1920x1080" "window 854 480 2" "wait 20" > /dev/null
}

# The Copy button starts a box, with its line, leaving Effortless Building's mode as it was; a right click ends it,
# placing no block where Planner1 looks, at 8 -60 19.
drv 1 "ebmode SINGLE" "cmd tp @s 8.5 -56 9.5 0 30" "wait 40" > /dev/null
lines=$(chat_lines 1)
copy_button
check "the radial menu's Copy button starts a box, leaving Effortless Building's mode as it was" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 drawbox) == "COPY 0 "*" white SINGLE" ]]'
check "with its controls line" eval '[[ $(val 1 modehud) == "Copy: $first_line" ]]'
check "and no message in chat" eval '(($(chat_lines 1) == lines))'
use_click 1
check "a right click ends the box" eval 'drv 1 "wait 10" > /dev/null && box_is "none SINGLE" &&
	[[ $(val 1 modehud) == hidden ]]'
check "placing no block, real or ghost, and saying nothing" eval '[[ $(val 1 holdlog) == *" edits=0 "* ]] &&
	[[ $(val 1 "real 8 -60 19") == minecraft:air ]] && [[ $(val 1 "layer 8 -60 19") == minecraft:air ]] &&
	(($(chat_lines 1) == lines))'

# The first corner: the space above the block aimed at for its top face, and the block itself for a side.
copy_button
drv 1 "lookat 5.5 -59 4.5" "wait 5" > /dev/null
check "aiming at a block's top face, the box's first corner is the space above it" \
	box_is "COPY 0 5,-59,4..5,-59,4 white SINGLE"
drv 1 "lookat 4.5 -58.5 3.9" "wait 5" > /dev/null
check "aiming at its side, the block itself" box_is "COPY 0 4,-59,3..4,-59,3 white SINGLE"

# A box from the space above the stone brick at 5 -60 4 to 3 -60 3, around 6 stone bricks, the stairs and the
# glowstone. Between clicks it follows the aim, with the corners set so far fixed; it's shot after the first and the
# second click, once the chat lines that draw over its line have faded.
drv 1 "lookat 5.5 -59 4.5" "wait 5" > /dev/null
attack_click 1
drv 1 "lookat 6.5 -59 6.5" "wait 5" > /dev/null
check "the first click sets the first corner, and the box follows the aim on its level" eval \
	'box_is "COPY 1 5,-59,4..6,-59,6 white SINGLE" && [[ $(val 1 modehud) == "Copy: $second_line (2 × 1 × 3)" ]]'
drv 1 "lookat 3.5 -59 3.5" "wait 5" > /dev/null
check "to the opposite corner" eval 'box_is "COPY 1 3,-59,3..5,-59,4 white SINGLE" &&
	[[ $(val 1 modehud) == "Copy: $second_line (3 × 1 × 2)" ]]'
check "the list under the crosshair counts the box's blocks: the stairs and the glowstone" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 boxlist) == *" | minecraft:glowstone 1, minecraft:oak_stairs 1" ]]'
drv 1 "wait 200" > /dev/null
shot 1 box-copy-first-click
attack_click 1
drv 1 "lookat 3.5 -57.5 3.5" "wait 5" > /dev/null
check "the second click sets the opposite corner, and the height follows the aim" eval \
	'box_is "COPY 2 3,-59,3..5,-58,4 white SINGLE" && [[ $(val 1 modehud) == "Copy: $height_line (3 × 2 × 2)" ]]'
drv 1 "lookat 3.5 -59.5 3.5" "wait 5" > /dev/null
check "down to the stone bricks" eval 'box_is "COPY 2 3,-60,3..5,-59,4 white SINGLE" &&
	[[ $(val 1 modehud) == "Copy: $height_line (3 × 2 × 2)" ]]'
check "and the list counts the stone bricks too, most numerous first" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 boxlist) == *" | minecraft:stone_bricks 6, minecraft:glowstone 1, minecraft:oak_stairs 1" ]]'
shot 1 box-copy-second-click
attack_click 1
drv 1 "wait 5" > /dev/null
check "the third click copies the 8 ghost blocks in the box, with a message" client_said 1 "Copied 8 blocks."
check "and ends the box, leaving the player's mode" box_is "none SINGLE"
check "and places no real block, leaving the ghost blocks" eval '[[ $(val 1 "real 4 -59 3") == minecraft:air ]] &&
	[[ $(val 1 "real 5 -60 4") == minecraft:air ]] && [[ $(val 1 "layer 4 -59 3") == minecraft:glowstone ]] &&
	[[ $(val 2 "get 5 -60 4") == minecraft:stone_bricks ]]'
check "the copy is at the aim, with its controls line" eval 'copy_is 1 "^at " && [[ $(val 1 modehud) == "$controls" ]]'

# The copy, 3 by 2 by 2 blocks, rests against the face aimed at, with the middle of its side that touches the face on
# the block beside it; along a side 2 blocks across, the middle is the block with the lower coordinate. On the grass's
# top at 5 -61 20, its bottom's middle is on 5 -60 20: it fills 4..6 east to west and 20..21 north to south.
drv 1 "cmd tp @s 5.5 -57 25.5 180 30" "wait 40" > /dev/null
aim 1 5 -60 20
check "the copy rests on the aimed top face, its middle under the crosshair" \
	drv 1 "await copy at 4,-60,20..6,-59,21 NONE NONE unselected 8 blocks"
check "the copy shows the ghost blocks as they were" eval 'state_has 1 "4 -59 20" "oak_stairs" &&
	state_has 1 "4 -59 20" "facing=north" && [[ $(val 1 "get 5 -59 20") == minecraft:glowstone ]] &&
	[[ $(val 1 "get 6 -60 21") == minecraft:stone_bricks ]] && [[ $(val 1 "get 6 -59 21") == minecraft:air ]]'
check "and places no real block" eval '[[ $(val 1 "real 4 -59 20") == minecraft:air ]]'
check "and isn't shared" eval 'drv 2 "wait 20" > /dev/null && [[ $(val 2 "get 4 -59 20") == minecraft:air ]]'
check "Litematica neither selects the copy nor lists it" eval '[[ $(val 1 selected) != "the copy" ]] &&
	[[ $(val 1 placementlist) == *"others: none" ]]'
drv 1 "close" "await noscreen" > /dev/null
shots copy-on-floor

# A real stone wall from 40 -60 30 to 44 -56 30, and a real stone block in the air at 46 -55 32. Aimed at the wall's
# south face at 42 -58 30, the copy stands south of it, the middle of its north side on 42 -58 31; aimed at the floating
# block's bottom, it hangs under it, the middle of its top on 46 -56 32.
drv 1 "cmd fill 40 -60 30 44 -56 30 minecraft:stone" "cmd setblock 46 -55 32 minecraft:stone" \
	"cmd tp @s 42.5 -57 36.5 180 10" "wait 40" > /dev/null
drv 1 "lookat 42.5 -57.5 31.0" "wait 5" > /dev/null
check "against the aimed side face, the copy stands beside it" \
	drv 1 "await copy at 41,-58,31..43,-57,32 NONE NONE unselected 8 blocks"
check "with its blocks as they were" eval 'state_has 1 "41 -57 31" "facing=north" &&
	[[ $(val 1 "get 42 -57 31") == minecraft:glowstone ]] && [[ $(val 1 "get 43 -58 32") == minecraft:stone_bricks ]]'
shots copy-against-wall
drv 1 "cmd tp @s 46.5 -59 36.5 180 -30" "wait 40" > /dev/null
drv 1 "lookat 46.5 -55.0 32.5" "wait 5" > /dev/null
check "under the aimed bottom face, the copy hangs below it" \
	drv 1 "await copy at 45,-57,32..47,-56,33 NONE NONE unselected 8 blocks"
drv 1 "cmd fill 40 -60 30 46 -55 32 minecraft:air" > /dev/null

drv 1 "cmd tp @s 30.5 -57 26.5 180 30" "wait 40" > /dev/null
aim 1 30 -60 20
check "the copy moves with the aim" drv 1 "await copy at 29,-60,20..31,-59,21 NONE NONE unselected"
check "with its controls line on screen" eval '[[ $(val 1 modehud) == "$controls" ]]'
# Chat lines draw over the controls line until they fade, 200 ticks after they came.
drv 1 "wait 210" > /dev/null
shot 1 copy-follows-aim

# The scroll wheel turns it a quarter turn around its middle for each notch, clockwise for a notch up, instead of
# changing the hotbar slot. Turned, it's 2 by 3: the middle of its bottom stays on 30 -60 20, so it fills 30..31 east to
# west and 19..21 north to south, and the stairs face east.
slot=$(val 1 slot)
xdo 1 click 4
check "a notch of the scroll wheel up turns the copy a quarter turn clockwise, in place" \
	drv 1 "await copy at 30,-60,19..31,-59,21 NONE CLOCKWISE_90"
check "its blocks turn with it" eval 'state_has 1 "31 -59 19" "facing=east" &&
	[[ $(val 1 "get 31 -59 20") == minecraft:glowstone ]] && [[ $(val 1 "get 30 -60 21") == minecraft:stone_bricks ]]'
check "and the hotbar slot stays" eval '[[ $(val 1 slot) == "$slot" ]]'
shot 1 copy-turned
xdo 1 click 5
check "a notch down turns it a quarter turn counterclockwise, back where it was" \
	drv 1 "await copy at 29,-60,20..31,-59,21 NONE NONE"
xdo 1 click 4
check "and up again, clockwise, the hotbar slot staying" eval \
	'drv 1 "await copy at 30,-60,19..31,-59,21 NONE CLOCKWISE_90" > /dev/null && [[ $(val 1 slot) == "$slot" ]]'

# A middle click flips it left to right for Planner1, who faces south, around its middle: the copy keeps its box, the
# stairs face west, and the stairs and the glowstone move from the box's east column to its west one. The crosshair
# is on the copy's blocks, which Litematica's own pick block on the middle click would otherwise take (hook L11).
drv 1 "cmd tp @s 30.5 -57 14.5 0 30" "wait 40" > /dev/null
aim 1 30 -60 20
xdo 1 click 2
check "a middle click mirrors the copy left to right, in place" \
	drv 1 "await copy at 30,-60,19..31,-59,21 FRONT_BACK COUNTERCLOCKWISE_90"
check "and picks no block, leaving the hotbar as it was" eval '[[ $(val 1 slot) == "$slot" ]]'
check "its blocks flip with it" eval 'state_has 1 "30 -59 19" "facing=west" &&
	[[ $(val 1 "get 30 -59 20") == minecraft:glowstone ]] && [[ $(val 1 "get 31 -59 19") == minecraft:air ]]'
shot 1 copy-mirrored

# Ghost glass where the copy will have empty space, and a ghost gold block where it will have stone bricks, on the
# oak planks; the copy moves over them.
drv 1 "edit 15 -58 4 15 -58 6 minecraft:glass" "edit 14 -58 6 14 -58 6 minecraft:glass" \
	"edit 15 -59 5 15 -59 5 minecraft:gold_block" > /dev/null
check "client 2 sees the glass and the gold block" drv 2 "await block 14 -58 6 minecraft:glass" \
	"await block 15 -59 5 minecraft:gold_block"
drv 1 "cmd tp @s 11.5 -55 1.5 0 30" "wait 40" > /dev/null
aim 1 14 -59 4
check "the copy moves over them" drv 1 "await copy at 14,-59,3..15,-58,5 FRONT_BACK COUNTERCLOCKWISE_90"
# Where the copy and the layer overlap, Litematica shows one block; the log records which.
echo "  where the copy overlaps ghost blocks: 15 -59 5 shows $(val 1 "get 15 -59 5") (copy stone_bricks, layer" \
	"gold_block); 15 -58 4 shows $(val 1 "get 15 -58 4") (copy empty, layer glass)" >> /out/checks.log
check "the copy's blocks show where they overlap, and the glass in its empty space" eval '
	[[ $(val 1 "get 15 -58 4") == minecraft:glass ]] && [[ $(val 1 "get 15 -59 5") == minecraft:stone_bricks ]]'
shot 1 copy-over-ghosts

# A left click pastes it where it shows: its 8 blocks, the stairs still facing west, and its empty space leaves the
# glass. The aim runs on to the real grass at 15 -61 5, which the click leaves.
drv 1 "cmd tp Planner2 18.5 -57 11.5 150 30" "wait 40" > /dev/null
attack_click 1
check "a left click pastes the copy where it shows, for the other player" drv 2 \
	"await block 14 -58 3 minecraft:oak_stairs" "await block 14 -58 4 minecraft:glowstone" \
	"await block 15 -59 5 minecraft:stone_bricks" "await block 14 -59 3 minecraft:stone_bricks"
check "glowing with Planner1's name" drv 2 "await glow Planner1:8"
shot 2 paste-seen
check "turned and mirrored as the copy showed it" state_has 2 "14 -58 3" "facing=west"
check "the copy's empty space leaves the ghost blocks under it" eval '[[ $(val 2 "get 15 -58 4") == minecraft:glass ]] &&
	[[ $(val 2 "get 15 -58 5") == minecraft:glass ]] && [[ $(val 2 "get 14 -58 6") == minecraft:glass ]]'
check "the click breaks no real block" eval '[[ $(val 1 "real 15 -61 5") == minecraft:grass_block ]]'
check "and the copy stays at the aim for another paste, with its controls line" eval \
	'copy_is 1 "^at [-0-9,.]+ FRONT_BACK COUNTERCLOCKWISE_90 unselected 8 blocks$" &&
	[[ $(val 1 modehud) == "$controls" ]]'
echo "  after the paste, the copy, which now meets the pasted blocks in the aim: $(val 1 copy)" >> /out/checks.log

# One Ctrl+Z undoes the whole paste; Ctrl+Y redoes it. Neither puts the copy away.
drv 1 "close" > /dev/null
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "Ctrl+Z undoes the whole paste" drv 2 "await block 14 -58 3 minecraft:air" "await block 14 -58 4 minecraft:air" \
	"await block 15 -59 5 minecraft:gold_block" "await block 14 -59 3 minecraft:air" "await block 15 -58 4 minecraft:glass"
keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
check "Ctrl+Y redoes it" drv 2 "await block 14 -58 3 minecraft:oak_stairs" "await block 15 -59 5 minecraft:stone_bricks" \
	"await block 14 -59 3 minecraft:stone_bricks"
check "and the copy stays at the aim" copy_is 1 "^at "

# Snapping to the paste, whose box is 14 -59 3 to 15 -58 5. Aimed at the east face of the glass at 15 -58 5, inside
# the paste's box, the copy sits flush against the box's east face, its edges in line with the box's at the corner
# nearest the aim, the box's top south corner: 16..17 east to west, -59..-58 up, and 3..5 north to south. Without the
# paste it would stand at 16 -58 4 to 17 -57 6, its middle on the aim.
drv 1 "cmd tp @s 20.5 -58 5.5 90 0" "wait 40" > /dev/null
drv 1 "lookat 16.0 -57.5 5.5" "wait 5" > /dev/null
check "aimed at a recent paste, the copy sits flush beside it" \
	drv 1 "await copy at 16,-59,3..17,-58,5 FRONT_BACK COUNTERCLOCKWISE_90"
shots copy-snapped

# Holding Left Ctrl, Minecraft's sprint key, keeps the copy in line with the latest paste: on its level, -59, and on
# its row, 3..5 north to south, while the aim is nearer that row than the paste's column, moving only along the row as
# the aim moves; nearer the column, on 14..15 east to west. Let go, it rests on the aimed face again.
drv 1 "cmd time set noon" "cmd tp @s 20.5 -54 14.5 180 30" "wait 40" > /dev/null
xdo 1 keydown Control_L
check "Left Ctrl holds down the sprint key" drv 1 "await sprint true"
aim 1 24 -60 6
check "with Left Ctrl held, the copy stays on the latest paste's level and row" \
	drv 1 "await copy at 24,-59,3..25,-58,5 FRONT_BACK COUNTERCLOCKWISE_90"
aim 1 28 -60 6
check "moving along the row with the aim" drv 1 "await copy at 28,-59,3..29,-58,5 FRONT_BACK COUNTERCLOCKWISE_90"
aim 1 20 -60 6
check "and back toward the paste" drv 1 "await copy at 20,-59,3..21,-58,5 FRONT_BACK COUNTERCLOCKWISE_90"
shots copy-in-line
aim 1 15 -60 14
check "and on the paste's column when the aim is nearer it" \
	drv 1 "await copy at 14,-59,13..15,-58,15 FRONT_BACK COUNTERCLOCKWISE_90"
xdo 1 keyup Control_L
check "let go, the copy rests on the aimed face again" eval 'drv 1 "await sprint false" > /dev/null &&
	drv 1 "await copy at 15,-60,13..16,-59,15 FRONT_BACK COUNTERCLOCKWISE_90" > /dev/null'

# A right click puts the copy away, keeping it, and its controls line goes; the click builds nothing. The scroll wheel
# then changes the hotbar slot again.
use_click 1
check "a right click puts the copy away" eval \
	'drv 1 "wait 10" > /dev/null && copy_is 1 "^away FRONT_BACK COUNTERCLOCKWISE_90 unselected 8 blocks$"'
check "and its controls line goes" eval '[[ $(val 1 modehud) == hidden ]]'
check "the click builds nothing" eval '[[ $(val 1 holdlog) == *" edits=0 "* ]] &&
	[[ $(val 1 "real 15 -60 5") == minecraft:air ]]'
slot=$(val 1 slot)
xdo 1 click 5
check "with the copy put away, the scroll wheel changes the hotbar slot again" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 slot) != "$slot" ]] && copy_is 1 "^away "'
xdo 1 click 4
check "and back" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 slot) == "$slot" ]]'

# The radial menu's Paste button brings it back to the aim, turned and mirrored as it was.
paste_button
check "the radial menu's Paste button brings the copy back, turned and mirrored as it was" eval \
	'drv 1 "wait 5" > /dev/null && copy_is 1 "^at [-0-9,.]+ FRONT_BACK COUNTERCLOCKWISE_90 unselected 8 blocks$"'
check "with its controls line" eval '[[ $(val 1 modehud) == "$controls" ]]'

# Choosing a build mode, Block mix, and showing or hiding ghost blocks each put it away.
drv 1 "ebmode LINE" "wait 5" > /dev/null
check "choosing a build mode puts it away" copy_is 1 "^away "
drv 1 "ebmode SINGLE" > /dev/null
paste_button
check "Paste brings it back again" copy_is 1 "^at "
radial_open
radial_point "Block mix"
xdo 1 keyup Alt_L
check "the radial menu's Block mix button puts it away and opens the block mix screen" \
	eval 'drv 1 "await screen Block mix" > /dev/null && copy_is 1 "^away "'
drv 1 "close" "wait 5" > /dev/null
paste_button
press 1 b
check "and so does B" eval 'drv 1 "await screen Block mix" > /dev/null && copy_is 1 "^away "'
drv 1 "close" "wait 5" > /dev/null
paste_button
press 1 n
check "and hiding ghost blocks puts it away" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) == "hidden, "* ]] && copy_is 1 "^away "'
press 1 n
paste_button
check "Paste brings it back once more" copy_is 1 "^at "

# The radial menu's Copy button around a real house of oak planks, hollow, with a glass window: 26 real blocks, and no
# ghost blocks. house_box [<shot>]: the box from the house's top corner nearest Planner1, aimed at its north side, to
# the opposite corner on its floor.
house_box() {
	box 1 50.5 -57.5 10.1 -58 52 12 -60 "$@"
}
drv 1 "cmd fill 50 -60 10 52 -58 12 minecraft:oak_planks hollow" "cmd setblock 51 -59 10 minecraft:glass" \
	"cmd tp @s 46.5 -56 6.5 0 30" "wait 40" > /dev/null
check "the house is real" eval '[[ $(val 1 "real 51 -59 10") == minecraft:glass ]] &&
	[[ $(val 1 "real 51 -59 11") == minecraft:air ]] && [[ $(val 1 "layer 50 -60 10") == minecraft:air ]]'
radial_open
radial_click "Copy"
check "the radial menu's Copy button puts the copy away and starts a box" eval \
	'[[ $(val 1 drawbox) == "COPY 0 "* ]] && copy_is 1 "^away "'
radial_let_go
house_box copy-real-box
check "a box around only real blocks copies them: the house's 26 blocks" client_said 1 "Copied 26 blocks."

# Two ghost blocks on the house: a gold block inside, where the world has air, and stone at its top corner, over the
# real oak planks there. The same box copies the ghost blocks and the real blocks together: 27 blocks, taking the
# ghost stone where a position has both. Client 2 shows them; client 1's copy of the house sits there now. Aimed at the
# grass's top at 50 -61 20, the copy, 3 blocks each way, fills 49..51 east to west and 19..21 north to south.
drv 1 "edit 51 -59 11 51 -59 11 minecraft:gold_block" "edit 52 -58 12 52 -58 12 minecraft:stone" > /dev/null
check "client 2 sees the two ghost blocks" drv 2 "await block 51 -59 11 minecraft:gold_block" \
	"await block 52 -58 12 minecraft:stone"
check "and on client 1 the ghost stone sits over a real oak plank" eval '
	[[ $(val 1 "real 52 -58 12") == minecraft:oak_planks ]] && [[ $(val 1 "layer 52 -58 12") == minecraft:stone ]] &&
	[[ $(val 1 "layer 51 -59 11") == minecraft:gold_block ]]'
copy_button
house_box copy-both-box
check "a box around ghost blocks and real blocks copies both: 27 blocks" client_said 1 "Copied 27 blocks."
drv 1 "cmd tp @s 51.5 -56 27.5 180 30" "wait 40" > /dev/null
aim 1 50 -60 20
check "the copy shows the house with the gold block inside" eval '
	drv 1 "await copy at 49,-60,19..51,-58,21 NONE NONE unselected 27 blocks" > /dev/null &&
	[[ $(val 1 "get 50 -59 19") == minecraft:glass ]] && [[ $(val 1 "get 50 -59 20") == minecraft:gold_block ]] &&
	[[ $(val 1 "get 49 -58 21") == minecraft:oak_planks ]]'
check "and the ghost stone, not the real oak plank, at its corner" eval '[[ $(val 1 "get 51 -58 21") == minecraft:stone ]]'
use_click 1
check "a right click puts it away" eval 'drv 1 "wait 5" > /dev/null && copy_is 1 "^away "'
radial_open
radial_point "Paste"
radial_let_go
aim 1 50 -60 20
check "letting the menu's key go over its Paste button brings it back" \
	drv 1 "await copy at 49,-60,19..51,-58,21 NONE NONE unselected 27 blocks"
attack_click 1
check "and a left click pastes them as ghost blocks" drv 2 \
	"await block 49 -60 19 minecraft:oak_planks" "await block 49 -58 21 minecraft:oak_planks" \
	"await block 50 -59 19 minecraft:glass" "await block 50 -59 20 minecraft:gold_block"
check "the ghost stone among them, where the copy had a ghost block over a real one" drv 2 \
	"await block 51 -58 21 minecraft:stone"
check "and no real blocks" eval '[[ $(val 1 "real 50 -59 19") == minecraft:air ]] &&
	[[ $(val 1 "real 51 -58 21") == minecraft:air ]]'
shot 1 copy-both-pasted

# With ghost blocks hidden, the same box copies the house's real blocks only: 26, with the real oak plank where the ghost
# stone stands and nothing where the ghost gold block is; ghost blocks stay hidden. Then they show again and the box
# copies the 27 blocks once more, which the checks below use.
press 1 n
check "ghost blocks are hidden before the copy" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) == "hidden, "* ]]'
real_lines=$(chat_count 1 "Copied 26 blocks.")
drv 1 "cmd tp @s 46.5 -56 6.5 0 30" "wait 40" > /dev/null
copy_button
house_box copy-hidden-box
check "with ghost blocks hidden, the same box copies the house's 26 real blocks" \
	said_once_more 1 "Copied 26 blocks." "$real_lines"
check "and ghost blocks stay hidden" eval '[[ $(val 1 ghosts) == "hidden, "* ]]'
drv 1 "cmd tp @s 51.5 -56 27.5 180 30" "wait 40" > /dev/null
aim 1 50 -60 20
check "the copy has the real oak plank where the ghost stone stands, and no gold block" eval '
	drv 1 "await copy at 49,-60,19..51,-58,21 NONE NONE unselected 26 blocks" > /dev/null &&
	[[ $(val 1 "get 51 -58 21") == minecraft:oak_planks ]] && [[ $(val 1 "get 50 -59 20") != minecraft:gold_block ]] &&
	[[ $(val 1 "get 50 -59 19") == minecraft:glass ]]'
shot 1 copy-hidden
press 1 n
check "ghost blocks show again" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) == "shown, "* ]]'
drv 1 "cmd tp @s 46.5 -56 6.5 0 30" "wait 40" > /dev/null
copy_button
house_box
check "the copy is the house with the gold block and the ghost stone again: 27 blocks" \
	eval 'drv 1 "wait 5" > /dev/null && copy_is 1 "^at [-0-9,.]+ NONE NONE unselected 27 blocks$"'

# With the copy at the aim, the menu shows the same seven buttons, none of them for turning, mirroring or clearing the
# copy; shot at 1920 by 1080, GUI scales 2 and 4, with the pointer off the buttons, and failing unless the buttons
# meet nothing.
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
check "with the copy at the aim, the menu's buttons meet nothing at 1920 by 1080, GUI scale 2" radial_copy_shot 2
check "nor at GUI scale 4" radial_copy_shot 4
drv 1 "window 854 480 2" "wait 20" > /dev/null
radial_open
xdo 1 mousemove --window %w 427 240
drv 1 "wait 10" "notoasts" "wait 5" "notoasts" "shot radial-with-copy" > /dev/null
echo "  radial-with-copy buttons: $(val 1 buttons)" >> /out/checks.log
check "with the copy at the aim, the menu shows the same seven buttons in their slots, with their hover texts" eval \
	'copy_is 1 "^at " && [[ $(val 1 slots) == "$slots" ]]'
xdo 1 mousemove --window %w 5 5
radial_let_go

# While a mouse mode is on, its button shows selected, in the menu's blue, and no mode segment does: Paste while the
# copy is at the aim, Copy, Material list and Delete while a box is drawn for them. With the mode ended, the player's
# mode, Line, shows selected again. Each is shot at 854 by 480 with the pointer in the screen's corner, off every
# button.
# selected_shot <name>: moves the pointer off the buttons and shoots the open menu as <name>.
selected_shot() {
	xdo 1 mousemove --window %w 5 5
	drv 1 "wait 5" > /dev/null
	shot 1 "$1"
	echo "  $1 buttons: $(val 1 buttons)" >> /out/checks.log
}
drv 1 "ebmode LINE" "wait 5" > /dev/null
paste_button
radial_open
selected_shot radial-paste-selected
check "with the copy at the aim in Line mode, Paste shows selected" eval 'copy_is 1 "^at " &&
	[[ $(val 1 selectedbuttons) == Paste ]] && [[ $(val 1 ebmode) == LINE ]]'
check "and no mode segment does" eval '[[ $(val 1 "ringselected radial-paste-selected") == none ]]'
radial_click "Copy"
selected_shot radial-copy-selected
check "while a box is drawn for Copy, Copy shows selected" eval '[[ $(val 1 drawbox) == "COPY 0 "* ]] &&
	[[ $(val 1 selectedbuttons) == Copy ]]'
check "and no mode segment does" eval '[[ $(val 1 "ringselected radial-copy-selected") == none ]]'
radial_click "Material list"
selected_shot radial-material-list-selected
check "while it's drawn for the material list, Material list shows selected" eval \
	'[[ $(val 1 drawbox) == "MATERIAL_LIST 0 "* ]] && [[ $(val 1 selectedbuttons) == "Material list" ]]'
check "and no mode segment does" eval '[[ $(val 1 "ringselected radial-material-list-selected") == none ]]'
radial_click "Delete"
selected_shot radial-delete-selected
check "while it's drawn for Delete, Delete shows selected" eval \
	'[[ $(val 1 drawbox) == "DELETE 0 "* ]] && [[ $(val 1 selectedbuttons) == Delete ]]'
check "and no mode segment does" eval '[[ $(val 1 "ringselected radial-delete-selected") == none ]]'
radial_click "Delete"
selected_shot radial-line-selected
check "with the box ended, no button shows selected, and Line's segment does" eval '[[ $(val 1 drawbox) == none* ]] &&
	[[ $(val 1 selectedbuttons) == none ]] && [[ $(val 1 "ringselected radial-line-selected") == LINE ]]'
radial_let_go
drv 1 "ebmode SINGLE" > /dev/null

# The Copy button pressed again ends its box.
radial_open
radial_click "Copy"
check "its Copy button starts a box" eval '[[ $(val 1 drawbox) == "COPY 0 "* ]]'
radial_click "Copy"
check "and pressed again, ends it, leaving the player's mode" box_is "none SINGLE"
radial_let_go
earlier=$(copy_kept)

# A box with no blocks in it. Its first click lands on the side of a real stone at 42 -59 42, which becomes the first
# corner and goes before the other two finish the box, from there to 40 -58 40, in the air.
drv 1 "cmd tp @s 45.5 -57 45.5 0 30" "cmd setblock 42 -59 42 minecraft:stone" "wait 40" > /dev/null
copy_button
drv 1 "lookat 42.5 -58.5 42.9" "wait 5" > /dev/null
check "a first corner on the stone's side is the stone" box_is "COPY 0 42,-59,42..42,-59,42 white SINGLE"
attack_click 1
drv 1 "cmd setblock 42 -59 42 minecraft:air" "await real 42 -59 42 minecraft:air" "lookat 40.5 -59 40.5" "wait 5" \
	> /dev/null
attack_click 1
drv 1 "lookat 40.5 -57.5 40.5" "wait 10" > /dev/null
attack_click 1
drv 1 "wait 5" > /dev/null
check "a box with no blocks gives a message" client_said 1 "There are no blocks in that box to copy."
shot 1 copy-nothing-message
check "keeps the earlier copy and ends the box, leaving the player's mode" eval '[[ $(copy_kept) == "$earlier" ]] &&
	box_is "none SINGLE"'

# The block cap, 200,000 blocks for one edit (the server's default): an edit of 204,000 blocks, one more layer than the
# cap holds, is refused by the game, and Big, an edit of exactly 200,000 ghost blocks (80 by 50 by 50), goes through
# the server to both games. A box over Big and the grass under it holds 204,000 blocks. The box starts in the space
# above Big's top corner, 48 blocks from the players, who are above its middle.
drv 1 "cmd tp @s 100.5 0 26.5 0 30" "cmd tp Planner2 101.5 0 26.5 0 30" "wait 60" > /dev/null
drv 1 "edit 60 -60 2 139 -11 52 minecraft:stone" > /dev/null
check "an edit of one block layer over the cap gives a message" \
	client_said 1 "That edit changes 204000 blocks, more than the server's limit of 200000."
drv 1 "edit 60 -60 2 139 -11 51 minecraft:stone" > /dev/null
check "client 2 sees all of Big's 200,000 blocks" drv 2 "await block 60 -60 2 minecraft:stone" \
	"await block 139 -11 51 minecraft:stone"
check "and client 1 does too" drv 1 "await block 60 -60 2 minecraft:stone" "await block 139 -11 51 minecraft:stone"
check "the edit over the cap changed nothing" eval '[[ $(val 2 "get 60 -60 52") != *stone* ]]'
copy_button
box 1 139.5 -10 51.5 -10 60 2 -61
check "a box over more blocks than the server's limit gives a message" \
	client_said 1 "That box has 204000 blocks, more than the server's limit of 200000."
shot 1 copy-too-many-message
check "and keeps the earlier copy" eval '[[ $(copy_kept) == "$earlier" ]]'
# A Delete box over Big and one more ghost block on its top corner holds 200,001 ghost blocks, more than one erase may
# take: it draws red with its line, and its click sets no corner and erases nothing. The box starts in the space above
# that block.
drv 1 "edit 139 -10 51 139 -10 51 minecraft:stone" "await block 139 -10 51 minecraft:stone" > /dev/null
delete_button
drv 1 "lookat 139.5 -9 51.5" "wait 5" > /dev/null
attack_click 1
drv 1 "lookat 60.5 -9 2.5" "wait 5" > /dev/null
attack_click 1
drv 1 "lookat 60.5 -60.5 2.5" "wait 10" > /dev/null
check "a Delete box over more ghost blocks than the server's limit draws red" \
	box_is "DELETE 2 60,-61,2..139,-9,51 red SINGLE"
check "with its line saying so" eval '[[ $(val 1 modehud) == "Too many blocks | Delete: $height_line (80 × 53 × 50)" ]]'
drv 1 "wait 200" > /dev/null
shot 1 box-delete-too-many
attack_click 1
check "and its click sets no corner and erases nothing" eval 'drv 1 "wait 5" > /dev/null &&
	box_is "DELETE 2 60,-61,2..139,-9,51 red SINGLE" && [[ $(val 1 holdlog) == *" edits=0 "* ]] &&
	[[ $(val 2 "get 139 -10 51") == minecraft:stone ]]'
drv 1 "lookat 60.5 -9.5 2.5" "wait 5" > /dev/null
check "a box over fewer ghost blocks draws white again" box_is "DELETE 2 60,-10,2..139,-9,51 white SINGLE"
use_click 1
check "a right click ends it" eval 'drv 1 "wait 10" > /dev/null && box_is "none SINGLE"'
# Erasing Big is another edit of 200,000 blocks, and leaves the layer as the later checks expect it.
drv 1 "edit 60 -60 2 139 -11 51 minecraft:air" "edit 139 -10 51 139 -10 51 minecraft:air" > /dev/null
check "erasing all of Big reaches both games" eval 'drv 2 "await block 60 -60 2 minecraft:air" \
	"await block 139 -11 51 minecraft:air" "await block 139 -10 51 minecraft:air" &&
	drv 1 "await block 139 -11 51 minecraft:air"'

# A box of one block: three clicks on the glowstone at 4 -59 3, the first on its side.
drv 1 "cmd tp @s 8.5 -56 9.5 0 30" "wait 40" > /dev/null
copy_button
box 1 4.5 -58.5 3.9 -59 4 3 -59
check "a single block has its own message" client_said 1 "Copied 1 block."
shot 1 copy-one-message

# Ending a box: the Copy button again, or choosing a build mode, which stays.
copy_button
copy_button
check "the Copy button pressed again ends its box, leaving the player's mode" box_is "none SINGLE"
copy_button
drv 1 "ebmode LINE" "wait 5" > /dev/null
check "and choosing a build mode ends the box, keeping that mode" box_is "none LINE"
drv 1 "ebmode SINGLE" > /dev/null

# The Material list button during a box for Copy switches it to the material list, keeping its first corner; it's shot
# after the first and the second click, once the chat lines have faded. A right click then ends it, with no message.
copy_button
drv 1 "lookat 5.5 -59 4.5" "wait 5" > /dev/null
attack_click 1
radial_open
radial_click "Material list"
radial_let_go
drv 1 "lookat 3.5 -59 3.5" "wait 5" > /dev/null
check "the Material list button during a box for Copy switches it to the material list, keeping its corner" eval \
	'box_is "MATERIAL_LIST 1 3,-59,3..5,-59,4 white SINGLE" &&
	[[ $(val 1 modehud) == "Material list: $second_line (3 × 1 × 2)" ]]'
# At 854 by 480, GUI scale 2, this line wraps onto two lines, with the box's size whole.
wrap=$(val 1 modewrap)
echo "  the material list's controls as they wrap: $wrap" >> /out/checks.log
check "the controls line wraps without breaking the box's size" eval \
	'[[ $wrap == *" / "* && $wrap == *"(3 × 1 × 2)" && ${wrap%% / *} != *"("* ]]'
drv 1 "wait 200" > /dev/null
shot 1 box-material-list-first-click
attack_click 1
drv 1 "lookat 3.5 -59.5 3.5" "wait 5" > /dev/null
check "its second click sets the opposite corner" eval 'box_is "MATERIAL_LIST 2 3,-60,3..5,-59,4 white SINGLE" &&
	[[ $(val 1 modehud) == "Material list: $height_line (3 × 2 × 2)" ]]'
shot 1 box-material-list-second-click
lines=$(chat_lines 1)
use_click 1
check "a right click before the third click ends the box, with no message" eval \
	'drv 1 "wait 10" > /dev/null && box_is "none SINGLE" && (($(chat_lines 1) == lines))'

# The Delete button during a box for Copy switches it to Delete, keeping its first corner, and Copy, Delete and
# Material list each switch it from the others. A Delete box's list counts only the ghost blocks in it. The Delete
# button pressed again ends its box. switch_to <button>: a click on that button of the radial menu, then the aim on the
# opposite corner.
switch_to() {
	radial_open
	radial_click "$1"
	radial_let_go
	drv 1 "lookat 3.5 -59 3.5" "wait 5" > /dev/null
}
copy_button
drv 1 "lookat 5.5 -59 4.5" "wait 5" > /dev/null
attack_click 1
switch_to Delete
check "the Delete button during a box for Copy switches it to Delete, keeping its corner" eval \
	'box_is "DELETE 1 3,-59,3..5,-59,4 white SINGLE" && [[ $(val 1 modehud) == "Delete: $second_line (3 × 1 × 2)" ]]'
check "its list counts the ghost blocks in it" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 boxlist) == *" | minecraft:glowstone 1, minecraft:oak_stairs 1" ]]'
switch_to "Material list"
check "Material list switches a box for Delete, keeping its corner" \
	box_is "MATERIAL_LIST 1 3,-59,3..5,-59,4 white SINGLE"
switch_to Delete
check "Delete switches it back" box_is "DELETE 1 3,-59,3..5,-59,4 white SINGLE"
switch_to Copy
check "and Copy switches a box for Delete, keeping its corner" box_is "COPY 1 3,-59,3..5,-59,4 white SINGLE"
switch_to Delete
switch_to Delete
check "the Delete button pressed again ends its box, leaving the player's mode" box_is "none SINGLE"

# Starting a Delete box while ghost blocks are hidden shows them first, as starting a shape does.
press 1 n
check "with ghost blocks hidden" eval '[[ $(val 1 ghosts) == "hidden, "* ]]'
delete_button
check "the Delete button shows them and starts its box" eval '[[ $(val 1 ghosts) == "shown, "* ]] &&
	[[ $(val 1 drawbox) == "DELETE 0 "* ]] && [[ $(val 1 modehud) == "Delete: $first_line" ]]'
use_click 1
check "a right click ends it" eval 'drv 1 "wait 10" > /dev/null && box_is "none SINGLE"'

# A Delete box of 40 by 40 by 10 over 16,000 ghost oak planks, a ghost oak door among them in the box's top layer whose
# upper half stands above the box, and a real stone among the planks, which a stone doesn't fill. The box's first
# corner is the plank at its south-west bottom corner, aimed at on its side; it's shot before the third click at 854 by
# 480 and at 1920 by 1080. The third click erases the 16,001 ghost blocks, the door's upper half among them, as one
# edit, which Planner2 sees glow with Planner1's name, and leaves the real stone; one Ctrl+Z brings them all back.
drv 1 "cmd tp @s 219.5 -30 60.5 180 30" "cmd tp Planner2 223.5 -30 62.5 180 30" "wait 100" > /dev/null
drv 1 "edit 200 -60 0 239 -51 39 minecraft:oak_planks" \
	"edit 239 -51 39 239 -51 39 minecraft:oak_door[half=lower,facing=north]" \
	"edit 239 -50 39 239 -50 39 minecraft:oak_door[half=upper,facing=north]" \
	"cmd setblock 205 -60 5 minecraft:stone" > /dev/null
check "both clients show the ghost blocks" eval 'drv 2 "await block 200 -60 0 minecraft:oak_planks" \
	"await block 238 -51 39 minecraft:oak_planks" "await block 239 -50 39 minecraft:oak_door" > /dev/null &&
	drv 1 "await block 239 -50 39 minecraft:oak_door" "await real 205 -60 5 minecraft:stone" > /dev/null &&
	[[ $(val 1 "layer 205 -60 5") == minecraft:oak_planks ]]'
delete_button
drv 1 "lookat 200.5 -59.5 39.9" "wait 5" > /dev/null
check "a Delete box's first corner on a ghost block's side is that block" \
	box_is "DELETE 0 200,-60,39..200,-60,39 white SINGLE"
attack_click 1
drv 1 "lookat 239.5 -60 0.5" "wait 5" > /dev/null
attack_click 1
drv 1 "lookat 239.5 -50.5 0.5" "wait 10" > /dev/null
check "its three clicks draw a box of 40 by 40 by 10" eval 'box_is "DELETE 2 200,-60,0..239,-51,39 white SINGLE" &&
	[[ $(val 1 modehud) == "Delete: $height_line (40 × 10 × 40)" ]]'
list=$(val 1 boxlist)
echo "  the Delete box's list: $list" >> /out/checks.log
check "and its list counts only the ghost blocks in it, not the real stone" eval \
	'[[ $list == *" | minecraft:oak_planks 15999, minecraft:oak_door 1" ]]'
drv 1 "wait 200" > /dev/null
shot 1 box-delete-854x480
drv 1 "window 1920 1080 2" "wait 20" "notoasts" > /dev/null
shot 1 box-delete-1920x1080
echo "  box-delete-1920x1080 box: $(val 1 drawbox)" >> /out/checks.log
drv 1 "window 854 480 2" "wait 20" > /dev/null
attack_click 1
check "the third click erases the ghost blocks, with a message" client_said 1 "Erased 16001 ghost blocks."
check "as one edit" eval '[[ $(val 1 holdlog) == *" edits=1 "* ]]'
check "and ends the box, leaving the player's mode" box_is "none SINGLE"
check "Planner2 sees them go, the door's upper half above the box with them" drv 2 \
	"await block 200 -60 0 minecraft:air" "await block 239 -51 39 minecraft:air" "await block 239 -50 39 minecraft:air" \
	"await block 238 -51 39 minecraft:air"
# An edit over 512 blocks glows as its area's outline, which keeps no list of blocks.
check "glowing with Planner1's name" drv 2 "await glow Planner1:0"
shot 2 delete-erased-seen
check "no ghost plank is left in the box for either player" eval \
	'[[ $(val 1 "countbox 200 -60 0 239 -51 39 minecraft:oak_planks") == "layer 0 "* ]] &&
	[[ $(val 2 "countbox 200 -60 0 239 -51 39 minecraft:oak_planks") == "layer 0 "* ]]'
check "and the real stone stays" eval '[[ $(val 1 "real 205 -60 5") == minecraft:stone ]]'
drv 1 "close" > /dev/null
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "one Ctrl+Z brings them all back" drv 2 "await block 200 -60 0 minecraft:oak_planks" \
	"await block 239 -51 39 minecraft:oak_door" "await block 239 -50 39 minecraft:oak_door" \
	"await block 238 -51 39 minecraft:oak_planks"
check "every one of them" eval '[[ $(val 2 "countbox 200 -60 0 239 -51 39 minecraft:oak_planks") == "layer 15999 "* ]]'

# A Delete box without ghost blocks, over the grass south of the planks, gives its message and changes nothing.
delete_button
drv 1 "lookat 210.5 -60 45.5" "wait 5" > /dev/null
attack_click 1
drv 1 "lookat 214.5 -60 48.5" "wait 5" > /dev/null
attack_click 1
drv 1 "lookat 214.5 -58.5 48.5" "wait 10" > /dev/null
check "a Delete box over no ghost blocks" box_is "DELETE 2 210,-60,45..214,-59,48 white SINGLE"
attack_click 1
check "gives its message" client_said 1 "There are no ghost blocks in that box to erase."
shot 1 delete-nothing-message
check "and changes nothing" eval '[[ $(val 1 holdlog) == *" edits=0 "* ]] && box_is "none SINGLE" &&
	[[ $(val 1 "real 210 -61 45") == minecraft:grass_block ]] &&
	[[ $(val 2 "countbox 200 -60 0 239 -51 39 minecraft:oak_planks") == "layer 15999 "* ]]'
drv 1 "edit 200 -60 0 239 -50 39 minecraft:air" > /dev/null
check "erasing the planks reaches both games" eval 'drv 2 "await block 200 -60 0 minecraft:air" \
	"await block 239 -50 39 minecraft:air" > /dev/null && drv 1 "await block 239 -50 39 minecraft:air"'
drv 1 "cmd tp @s 8.5 -56 9.5 0 30" "cmd tp Planner2 101.5 0 26.5 0 30" "wait 40" > /dev/null

# The list under the crosshair for a box of 144 by 3 by 144 over a real floor of 40 kinds of block, at 854 by 480,
# GUI scale 2: strips 3 wide from the box's west side, the first 144 long and each next one 1 shorter, so the kinds
# are in strip order, most numerous first. The box is as wide as the chunks the game loads around the player on the
# dev server (its view distance is 6) allow, and goes from the grass east of the strips to the far corner.
kinds=(stone granite polished_granite diorite polished_diorite andesite polished_andesite cobblestone oak_planks
	spruce_planks birch_planks jungle_planks acacia_planks dark_oak_planks mangrove_planks cherry_planks bamboo_planks
	crimson_planks warped_planks bricks stone_bricks mossy_stone_bricks sandstone red_sandstone white_wool orange_wool
	magenta_wool light_blue_wool yellow_wool lime_wool pink_wool gray_wool light_gray_wool cyan_wool purple_wool
	blue_wool brown_wool green_wool red_wool black_wool)
drv 1 "cmd tp @s -151.5 -40 8.5 0 30" "wait 100" > /dev/null
fills=()
for i in "${!kinds[@]}"; do
	fills+=("cmd fill $((-224 + 3 * i)) -60 -64 $((-222 + 3 * i)) -60 $((79 - i)) minecraft:${kinds[i]}")
done
drv 1 "${fills[@]}" "await real -105 -60 40 minecraft:black_wool" > /dev/null
expected=""
for i in $(seq 0 28); do
	if ((i == 0)); then
		sep=""
	elif ((i % 10 == 0)); then
		sep=" / "
	else
		sep=", "
	fi
	expected+="${sep}minecraft:${kinds[i]} $((3 * (144 - i)))"
done
expected+=", +11"
copy_button
drv 1 "lookat -80.5 -60 79.5" "wait 5" > /dev/null
attack_click 1
drv 1 "lookat -223.5 -60 -63.5" "wait 5" > /dev/null
attack_click 1
drv 1 "lookat -223.5 -57.5 -63.5" "wait 10" > /dev/null
list=$(val 1 boxlist)
echo "  the list for 40 kinds: $list" >> /out/checks.log
check "a box of 144 by 3 by 144 over 40 kinds of block" box_is "COPY 2 -224,-60,-64..-81,-58,79 white SINGLE"
read -r right width bottom top _ <<< "$list"
check "lists them in rows of 10 that end inside the screen and above the controls line, with a +N cell" eval \
	'((right <= width - 4 && bottom <= top)) && [[ ${list#* | } == "$expected" ]]'
drv 1 "wait 200" > /dev/null
shot 1 box-list-wrapped
use_click 1
check "a right click ends the box and its list" eval 'drv 1 "wait 10" > /dev/null && box_is "none SINGLE" &&
	[[ $(val 1 boxlist) == hidden ]]'

# A shape drawn with the break click over real blocks shows no list of them (hook E8): it erases ghost blocks and
# leaves the real ones.
drv 1 "cmd tp @s -92.5 -57 4.5 0 30" "wait 20" "ebmode LINE" "lookat -90.5 -60 0.5" "wait 5" > /dev/null
attack_click 1
drv 1 "lookat -95.5 -60 0.5" "wait 10" > /dev/null
check "a break shape over real grass lists none of it" eval '[[ $(val 1 breaklist) == "BREAKING 0" ]]'
attack_click 1
drv 1 "wait 5" "ebmode SINGLE" > /dev/null
check "and the shape ends" eval '[[ $(val 1 breaklist) == "none 0" ]]'
drv 1 "cmd tp @s 8.5 -56 9.5 0 30" "wait 100" > /dev/null

# The limits. A box from a corner on the grass at 120 -60 64; Planner1 then flies 210 blocks east, as a player may
# while drawing, and the corner's chunks unload. An opposite corner 261 blocks from it is past the limit on a side,
# which comes first, and one 181 blocks from it reaches chunks Planner1's game hasn't loaded. Each draws red, with its
# line, and its click sets no corner. The shots wait for the chat lines to fade.
drv 1 "cmd tp @s 120.5 -56 70.5 180 30" "wait 40" > /dev/null
copy_button
drv 1 "lookat 120.5 -60 64.5" "wait 5" > /dev/null
check "a first corner on the grass" box_is "COPY 0 120,-60,64..120,-60,64 white SINGLE"
attack_click 1
drv 1 "cmd tp @s 330.5 -50 64.5 90 30" "wait 210" "lookat 380.5 -60 64.5" "wait 5" > /dev/null
check "a box of 261 blocks a side draws red" box_is "COPY 1 120,-60,64..380,-60,64 red SINGLE"
check "with its line saying which limit" eval \
	'[[ $(val 1 modehud) == "Too many blocks | Copy: $second_line (261 × 1 × 1)" ]]'
shot 1 box-too-large
attack_click 1
check "and its click sets no corner" eval 'drv 1 "wait 5" > /dev/null &&
	box_is "COPY 1 120,-60,64..380,-60,64 red SINGLE"'
drv 1 "lookat 300.5 -60 64.5" "wait 5" > /dev/null
check "a box over chunks the game hasn't loaded draws red" box_is "COPY 1 120,-60,64..300,-60,64 red SINGLE"
check "with its line saying which limit" eval '[[ $(val 1 modehud) == \
	"Part of the box is too far away to read. Move closer. | Copy: $second_line (181 × 1 × 1)" ]]'
shot 1 box-not-loaded
attack_click 1
check "and its click sets no corner" eval 'drv 1 "wait 5" > /dev/null &&
	box_is "COPY 1 120,-60,64..300,-60,64 red SINGLE"'
use_click 1
check "a right click ends it" eval 'drv 1 "wait 10" > /dev/null && box_is "none SINGLE"'
drv 1 "cmd tp @s 8.5 -56 9.5 0 30" "wait 40" "fly false" > /dev/null
drv 2 "fly false" > /dev/null

# The material list. Its radial menu button's hover text, shot at 1920 by 1080, GUI scales 2 and 4; the pointer leaves
# the button before the key is let go, so closing the menu starts no box.
for scale in 2 4; do
	drv 1 "window 1920 1080 $scale" "wait 20" > /dev/null
	radial_open
	radial_point "Material list"
	drv 1 "wait 10" "notoasts" "shot radial-material-list-hover-1920x1080-gui$scale" > /dev/null
	echo "  radial-material-list-hover-1920x1080-gui$scale buttons: $(val 1 buttons)" >> /out/checks.log
	xdo 1 mousemove --window %w 5 5
	radial_let_go
done
check "the hover shots start no box" box_is "none SINGLE"
drv 1 "window 854 480 2" "wait 20" > /dev/null

# A row of 10 ghost oak planks, 6 of them built for real, which removes their ghost blocks, with a row of 10 real stone
# blocks on each side of it. Both players fly again, lifted first.
drv 1 "cmd tp @s 24.5 -51 -4.5 180 30" "cmd tp Planner2 24.5 -51 -3.5 180 30" "wait 40" "fly true" > /dev/null
drv 2 "fly true" > /dev/null
drv 1 "cmd tp @s 24.5 -56 -4.5 180 30" "edit 20 -60 -10 29 -60 -10 minecraft:oak_planks" \
	"cmd fill 20 -60 -10 25 -60 -10 minecraft:oak_planks" "cmd fill 20 -60 -11 29 -60 -11 minecraft:stone" \
	"cmd fill 20 -60 -9 29 -60 -9 minecraft:stone" "wait 40" > /dev/null
check "the row shows, 6 of its planks real, with the stone beside it" eval \
	'drv 1 "await block 29 -60 -10 minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 "real 25 -60 -10") == minecraft:oak_planks ]] && [[ $(val 1 "real 26 -60 -10") == minecraft:air ]] &&
	[[ $(val 1 "real 29 -60 -11") == minecraft:stone ]] && [[ $(val 1 "real 20 -60 -9") == minecraft:stone ]]'

# A copy of the row, and ghost blocks hidden, which puts it away.
copy_button
box 1 20.5 -59 -9.5 -59 29 -10 -60
check "a copy of the row" client_said 1 "Copied 10 blocks."
press 1 n
check "ghost blocks are hidden" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) == "hidden, "* ]]'

# Letting the menu's key go over its Material list button starts a box; a box two blocks high around the row and the
# stone beside it, from the space above the first stone block, ends it. The list's screen takes the button from the game
# as it opens, so the game never sees the third click held: it's a press held for 10 ticks.
# list_box <lookat x y z of the first corner> <y of the second corner> <x and z of the second corner> <lowest y>: the
# three clicks of a box as `box` makes them, with the third held for 10 ticks.
list_box() {
	drv 1 "lookat $1 $2 $3" "wait 5" > /dev/null
	attack_click 1
	drv 1 "lookat $(half "$5") $4 $(half "$6")" "wait 5" > /dev/null
	attack_click 1
	drv 1 "lookat $(half "$5") $(half "$7") $(half "$6")" "wait 10" > /dev/null
	xdo 1 mousedown 1
	drv 1 "wait 10" > /dev/null
	xdo 1 mouseup 1
	drv 1 "wait 10" > /dev/null
}
# material_box: the box around the row and the stone beside it.
material_box() {
	list_box 20.5 -59 -10.5 -59 29 -9 -60
}

# With ghost blocks hidden, the box lists the real blocks only: the 6 built planks and the 20 stone blocks, with nothing
# missing, and ghost blocks stay hidden.
radial_open
radial_point "Material list"
radial_let_go
check "letting the menu's key go over its Material list button starts a box for it" eval \
	'[[ $(val 1 drawbox) == "MATERIAL_LIST 0 "* ]]'
check "with its controls line" eval '[[ $(val 1 modehud) == "Material list: $first_line" ]]'
material_box
echo "  the material list with ghost blocks hidden: $(val 1 matlist)" >> /out/checks.log
check "with ghost blocks hidden, the box lists the real blocks: 6 planks and 20 stone blocks, none missing" eval \
	'[[ $(val 1 matlist) == "open Material list for the blocks in the box: total 26 missing 0 wrong 0 | "* ]] &&
	[[ $(val 1 matlist) == *" | minecraft:oak_planks 6/0"* ]] && [[ $(val 1 matlist) == *" | minecraft:stone 20/0"* ]]'
check "and ghost blocks stay hidden" eval '[[ $(val 1 ghosts) == "hidden, "* ]]'
for scale in 2 4; do
	drv 1 "window 1920 1080 $scale" "wait 20" "notoasts" "shot material-list-hidden-1920x1080-gui$scale" > /dev/null
done
drv 1 "window 854 480 2" "wait 20" "close" "wait 5" > /dev/null
check "the box ends, leaving the player's mode" box_is "none SINGLE"

# With ghost blocks shown, the same box opens Litematica's material list. The copy is at the aim before, and the
# Material list button puts it away.
press 1 n
check "ghost blocks show again" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ghosts) == "shown, "* ]]'
paste_button
check "the copy is back at the aim" copy_is 1 "^at "
radial_open
radial_point "Material list"
radial_let_go
check "the Material list button puts it away" copy_is 1 "^away "
material_box
echo "  the material list: $(val 1 matlist); the copy: $(val 1 copy)" >> /out/checks.log
check "the box opens Litematica's material list: 30 blocks in all, 4 planks missing, the stone built" eval \
	'[[ $(val 1 matlist) == "open Material list for the blocks in the box: total 30 missing 4 wrong 0 | "* ]] &&
	[[ $(val 1 matlist) == *" | minecraft:oak_planks 10/4"* ]] && [[ $(val 1 matlist) == *" | minecraft:stone 20/0"* ]]'
check "ghost blocks still show" eval '[[ $(val 1 ghosts) == "shown, "* ]]'
check "the box places and erases nothing, and ends, leaving the player's mode" eval '
	[[ $(val 1 "real 26 -60 -10") == minecraft:air ]] && [[ $(val 1 "real 20 -59 -10") == minecraft:air ]] &&
	[[ $(val 1 "layer 26 -60 -10") == minecraft:oak_planks ]] && box_is "none SINGLE"'
check "and leaves the copy put away" copy_is 1 "^away .* 10 blocks$"
for scale in 2 4; do
	drv 1 "window 1920 1080 $scale" "wait 20" "notoasts" "shot material-list-1920x1080-gui$scale" > /dev/null
done
drv 1 "window 854 480 2" "wait 20" "close" "wait 5" > /dev/null

# A box with no ghost blocks lists its real blocks: the 9 grass blocks from 42 -61 42 to 40 -61 40, from the space above
# the grass; a box of the air above them opens an empty list. The Copy button during a box for the material list
# switches it to Copy, and pressed again ends it.
drv 1 "cmd tp @s 45.5 -57 45.5 0 30" "wait 40" > /dev/null
radial_open
radial_click "Material list"
radial_let_go
list_box 42.5 -60 42.5 -60 40 40 -61
echo "  the material list of a box with real blocks only: $(val 1 matlist)" >> /out/checks.log
check "a box with no ghost blocks lists its real blocks" eval \
	'[[ $(val 1 matlist) == "open Material list for the blocks in the box: total 9 missing 0 wrong 0 | minecraft:grass_block 9/0" ]]'
drv 1 "close" "wait 5" > /dev/null
radial_open
radial_click "Material list"
radial_let_go
list_box 42.5 -60 42.5 -60 40 40 -60
echo "  the material list of a box with no blocks: $(val 1 matlist)" >> /out/checks.log
check "a box with no blocks opens an empty list" eval \
	'[[ $(val 1 matlist) == "open Material list for the blocks in the box: total 0 missing 0 wrong 0" ]]'
for scale in 2 4; do
	drv 1 "window 1920 1080 $scale" "wait 20" "notoasts" "shot material-list-empty-1920x1080-gui$scale" > /dev/null
done
drv 1 "window 854 480 2" "wait 20" "close" "wait 5" > /dev/null
radial_open
radial_click "Material list"
radial_click "Copy"
check "the Copy button during a box for the material list switches it to Copy" eval \
	'[[ $(val 1 drawbox) == "COPY 0 "* ]]'
radial_click "Copy"
check "and pressed again, ends it" box_is "none SINGLE"
radial_let_go

# The lists on the server. The material list box around the row, counted again, is Planner1's "Last counted" in their
# file on the dev server within 30 seconds of the count (the driver reads the file, which shares the container), and
# after the server and Planner1's game restart, the game has it as its current list.
lists_file=../server/world/building-planner/material-lists/$(val 1 "uuid Planner1").dat
drv 1 "cmd tp @s 24.5 -51 -4.5 180 30" "wait 40" "fly true" > /dev/null
radial_open
radial_point "Material list"
radial_let_go
material_box
list_counted=$SECONDS
counted_list=$(val 1 currentlist)
echo "  the counted list: $counted_list" >> /out/checks.log
drv 1 "close" "wait 5" > /dev/null
check "a box Planner1 counts is Last counted in their file on the server within 30 seconds" eval \
	'[[ $counted_list == "minecraft:overworld "*" | minecraft:oak_planks 10/4/0"* ]] &&
	drv 1 "await listfile $lists_file" > /dev/null &&
	((SECONDS - list_counted <= 31))'
echo "  seconds from the count to the file: $((SECONDS - list_counted)); the file's Last counted: $(val 1 "listfile $lists_file")" \
	>> /out/checks.log
cp /out/client1.log /out/client1-before-lists-restart.log
cp /out/server.log /out/server-before-lists-restart.log
stop_client 1
stop_server
start_server "" server-lists
start_client 1
check "after the server and Planner1's game restart, the game has the list it counted as its current list" eval \
	'drv 1 "await session" "await noscreen" "wait 20" > /dev/null && [[ $(val 1 currentlist) == "$counted_list" ]]'
echo "  the current list after the restart: $(val 1 currentlist)" >> /out/checks.log
drv 1 "fly false" > /dev/null

# The material list's screen and the saved lists. Client 2 lost the server at the restart above, so it stops until it's
# needed.
stop_client 2
lists_log=server-lists
uuid=$(val 1 "uuid Planner1")
server_lists_file=run/server/world/building-planner/material-lists/$uuid.dat

# mat_labels: the labels of the buttons on Litematica's open material list screen, separated by ",".
mat_labels() {
	val 1 malilibbuttons | sed -E 's/: [0-9]+ [0-9]+ [0-9]+ [0-9]+//g; s/ \| /,/g'
}
# mat_click <label>: a real click on the button of the open material list screen whose label starts with the text.
mat_click() {
	local pos
	pos=$(val 1 "malilibbutton $1")
	[[ $pos != none ]] && click 1 $pos
}
# widget_click <n> <text>: a real click on the nth shown widget (from 0) of the open screen whose text starts with the
# text.
widget_click() {
	local pos
	pos=$(val 1 "widget $1 $2")
	[[ $pos != none ]] && click 1 $pos
}
# type_name <text>: empties the focused text field and types the text with real key presses.
type_name() {
	keys 1 key ctrl+a BackSpace
	[[ -z $1 ]] || keys 1 type --delay 20 -- "$1"
	drv 1 "wait 5" > /dev/null
}
# save_as <name>: "Save list" on the open material list screen, the name typed over the date, and "Save", which
# returns to the list's screen.
save_as() {
	mat_click "Save list"
	drv 1 "await screen Save list" > /dev/null
	type_name "$1"
	widget_click 0 Save
	drv 1 "await screen Material list for the blocks in the box" "wait 20" > /dev/null
}
# wait_saved <names>: waits, up to 35 seconds, until the server's file of Planner1's lists holds exactly those saved
# lists, newest first; returns whether it did.
wait_saved() {
	local i
	for i in $(seq 35); do
		[[ $(val 1 "listfilesaved $lists_file") == "$1" ]] && return 0
		drv 1 "wait 20" > /dev/null
	done
	return 1
}
# both_sizes_shot <name>: shoots the open screen at 854 by 480 and at 1920 by 1080, GUI scale 2, as <name>-854x480-gui2
# and <name>-1920x1080-gui2, logging the boxes of its widgets, or of a MaLiLib screen's buttons, for outlining; it
# ends at 854 by 480.
both_sizes_shot() {
	local size w h
	for size in "854 480" "1920 1080"; do
		read -r w h <<< "$size"
		drv 1 "window $w $h 2" "wait 20" "notoasts" "wait 5" "shot $1-${w}x$h-gui2" > /dev/null
		echo "  $1-${w}x$h-gui2 widgets: $(val 1 widgets); buttons: $(val 1 malilibbuttons)" >> /out/checks.log
	done
	drv 1 "window 854 480 2" "wait 20" > /dev/null
}

# The box around the row and the stone beside it opens the list's screen again: "Save list" and "Saved lists" take the
# place of "Export" and "Write to file", and "Litematica menu" is gone; the rest of Litematica's buttons and the
# multiplier stay. Shot at 854 by 480 and 1920 by 1080, GUI scale 2.
drv 1 "cmd tp @s 24.5 -51 -4.5 180 30" "wait 40" "fly true" > /dev/null
radial_open
radial_point "Material list"
radial_let_go
material_box
buttons_re='^Refresh,Hide available: (ON|OFF),Info HUD: (ON|OFF),Clear ignored,Clear cache,Save list,Saved lists$'
for size in "854 480" "1920 1080"; do
	read -r w h <<< "$size"
	drv 1 "window $w $h 2" "wait 20" "notoasts" "wait 5" "shot material-list-buttons-${w}x$h-gui2" > /dev/null
	echo "  material-list-buttons-${w}x$h-gui2 buttons: $(val 1 malilibbuttons)" >> /out/checks.log
	check "at $w by $h, the list's screen shows Save list and Saved lists, and no Export, Write to file or Litematica menu" \
		eval '[[ $(mat_labels) =~ $buttons_re ]] && [[ $(val 1 drawntexts) == *"Multiplier:"* ]]'
done
drv 1 "window 854 480 2" "wait 20" > /dev/null

# The material list panel. "Info HUD" on the list's screen turns it on, and it shows once the screen closes, in the
# top-left corner, with the list's rows in the screen's order. The list here is the row and the stone beside it: 20 stone
# blocks, all built, and 10 oak planks, 4 of them missing; Planner1 carries none of either.
# panel_is <client> <text> [ticks]: the panel reads the text, within the ticks (60 unless given).
panel_is() {
	local i
	for i in $(seq $((${3:-60} / 5))); do
		[[ $(val "$1" matpanel) == "$2" ]] && return 0
		drv "$1" "wait 5" > /dev/null
	done
	[[ $(val "$1" matpanel) == "$2" ]]
}
# multiplier <n>: types n into the open material list screen's "Multiplier" field, with real clicks and key presses.
multiplier() {
	click 1 $(val 1 matmultiplier)
	keys 1 key ctrl+a BackSpace
	keys 1 type -- "$1"
	drv 1 "wait 10" > /dev/null
}
# reopen: the current list's screen, opened again.
reopen() {
	drv 1 "openlist" "await screen Material list for the blocks in the box" "wait 10" > /dev/null
}
panel_head="Material list | All Hold Need"
drv 1 "cmd clear @s minecraft:oak_planks" "wait 10" > /dev/null
check "the panel is off at first, and the button reads Info HUD: OFF" eval \
	'[[ $(val 1 matpanel) == hidden && $(mat_labels) == *"Info HUD: OFF"* ]]'
mat_click "Info HUD"
check "Info HUD turns the panel on, and the button reads Info HUD: ON" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(mat_labels) == *"Info HUD: ON"* && $(val 1 infohud) == true ]]'
check "the panel hides while the screen is open" eval '[[ $(val 1 matpanel) == hidden ]]'
drv 1 "close" > /dev/null
check "once the screen closes, the panel shows the list's rows in the screen's order, with their counts" \
	panel_is 1 "$panel_head | minecraft:stone 20 0 0 green | minecraft:oak_planks 10 0 4 red"
echo "  the panel's box: $(val 1 matpanelbox)" >> /out/checks.log
check "it sits 4 from the screen's top and left edges" eval '[[ $(val 1 matpanelbox) == "8 8 "* ]]'
shot 1 material-panel-row
# The multiplier at 4: "Total" times 4, and "Missing" the same, on the screen and the panel.
reopen
multiplier 4
check "with the multiplier at 4, the screen's counts change" eval \
	'[[ $(val 1 drawntexts) == *"80"* && $(val 1 drawntexts) == *"40"* ]]'
drv 1 "close" > /dev/null
check "and the panel's heading and every count change as the screen's do" panel_is 1 \
	"Material list ×4 | All Hold Need | minecraft:stone 80 0 80 red | minecraft:oak_planks 40 0 40 red"
reopen
multiplier 1
# A click on the screen's "Missing" heading sorts by it, most first.
click 1 $(val 1 "matheader 2")
drv 1 "wait 10" "close" > /dev/null
check "sorting by Missing on the screen reorders the panel" \
	panel_is 1 "$panel_head | minecraft:oak_planks 10 0 4 red | minecraft:stone 20 0 0 green"
reopen
click 1 $(val 1 "matheader 1")
mat_click "Hide available"
drv 1 "wait 10" "close" > /dev/null
check "Hide available leaves out the rows it hides on the screen: the stone, all built" \
	panel_is 1 "$panel_head | minecraft:oak_planks 10 0 4 red"
reopen
mat_click "Hide available"
drv 1 "wait 10" "close" > /dev/null
check "and with it off, they're back" panel_is 1 "$panel_head | minecraft:stone 20 0 0 green | minecraft:oak_planks 10 0 4 red"
drv 1 "cmd give @s minecraft:oak_planks 3" > /dev/null
check "picking up 3 oak planks raises Hold within 2 seconds" \
	panel_is 1 "$panel_head | minecraft:stone 20 0 0 green | minecraft:oak_planks 10 3 4 red" 45
# A second box, the row alone, keeps the panel on with the new list.
radial_open
radial_point "Material list"
radial_let_go
list_box 20.5 -59 -9.5 -59 29 -10 -60
drv 1 "close" > /dev/null
check "a second box keeps the panel on, with the new list" panel_is 1 "$panel_head | minecraft:oak_planks 10 3 4 red"
press 1 F1
check "F1 hides the panel" eval '[[ $(val 1 matpanel) == hidden ]]'
press 1 F1
check "and shows it again" panel_is 1 "$panel_head | minecraft:oak_planks 10 3 4 red"
# The recount: 2 of the missing planks built for real lower "Missing" by 2 within 2 seconds, on the panel and the
# screen; taking them away again lowers "Total" by 2, since building them removed their ghost blocks, and drawing the
# ghost blocks again brings both back.
drv 1 "cmd fill 26 -60 -10 27 -60 -10 minecraft:oak_planks" "await real 27 -60 -10 minecraft:oak_planks" > /dev/null
check "with its box loaded, 2 of the missing planks built lower Missing by 2 within 2 seconds" \
	panel_is 1 "$panel_head | minecraft:oak_planks 10 3 2 gold" 45
reopen
check "on the screen too" eval '[[ $(val 1 matlist) == *" | minecraft:oak_planks 10/2" ]]'
drv 1 "close" "cmd fill 26 -60 -10 27 -60 -10 minecraft:air" "await real 27 -60 -10 minecraft:air" > /dev/null
check "and taken away, they leave the list, their ghost blocks gone" \
	panel_is 1 "$panel_head | minecraft:oak_planks 8 3 2 gold" 45
drv 1 "edit 26 -60 -10 27 -60 -10 minecraft:oak_planks" > /dev/null
check "drawn again as ghost blocks, they're missing again" panel_is 1 "$panel_head | minecraft:oak_planks 10 3 4 red" 45
drv 1 "cmd clear @s minecraft:oak_planks" > /dev/null

# Shots of the panel, with the mix panel on too (a mix of stone bricks, held), for a list of 13 kinds of block: 10 rows, then "+3 more". Each kind
# is a row of ghost blocks one shorter than the last, from 13 stone blocks to 1 diorite: the stone is all built for real
# ("Missing" green), the cobblestone covered by the 64 Planner1 carries (gold), the rest missing (red).
kinds=(stone cobblestone oak_planks spruce_planks birch_planks bricks sandstone glass white_wool red_wool terracotta
	granite diorite)
for k in "${!kinds[@]}"; do
	drv 1 "edit 0 -60 $((-20 - k)) $((12 - k)) -60 $((-20 - k)) minecraft:${kinds[k]}" > /dev/null
done
# Stone bricks in the main hand and Effortless Building's mode Single, which the mix panel needs to show; the stone
# bricks go before the cobblestone, which goes elsewhere.
mode_before_panel=$(val 1 ebmode)
drv 1 "ebmode SINGLE" "cmd fill 0 -60 -20 12 -60 -20 minecraft:stone" \
	"cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "cmd give @s minecraft:cobblestone 64" \
	"cmd tp @s 6.5 -52 -10.5 180 50" "await block 0 -60 -32 minecraft:diorite" "wait 40" > /dev/null
radial_open
radial_point "Material list"
radial_let_go
list_box 0.5 -59 -31.5 -59 12 -20 -60
# The commands' chat lines fade after 10 seconds, before the shots.
drv 1 "close" "setmix minecraft:stone_bricks:80,minecraft:mossy_stone_bricks:20" "wait 220" > /dev/null
long_rows="minecraft:stone 13 0 0 green | minecraft:cobblestone 12 64 12 gold | minecraft:oak_planks 11 0 11 red | \
minecraft:spruce_planks 10 0 10 red | minecraft:birch_planks 9 0 9 red | minecraft:bricks 8 0 8 red | \
minecraft:sandstone 7 0 7 red | minecraft:glass 6 0 6 red | minecraft:white_wool 5 0 5 red | minecraft:red_wool 4 0 4 red"
# At 854 by 480 the 10th row and the "+N more" line after it would cover the hotbar, so the panel shows 9 rows and "+4
# more", the 10th row and the 3 past it; at 1920 by 1080 it shows all 10, then "+3 more".
long_rows9=${long_rows% | minecraft:red_wool 4 0 4 red}
check "the mix panel shows too" eval '[[ $(val 1 panel) == "Block mix | 80% | 20% | B: Edit mix" ]]'
# panel_above_hotbar: the panel's box ends above the hotbar's top. The game draws the hotbar 22 GUI pixels above the
# screen's bottom, and its selection frame 23, so the box's bottom (the box's y and height, in the window's pixels) is
# compared with the window's height less 23 GUI pixels. $win_h and $gui are the window's height and GUI scale.
panel_above_hotbar() {
	local x y w h
	read -r x y w h <<< "$(val 1 matpanelbox)"
	echo "  the panel's bottom: $((y + h)); the hotbar's top: $((win_h - 23 * gui))" >> /out/checks.log
	((y + h <= win_h - 23 * gui))
}
for size in "854 480 2" "1920 1080 2" "1920 1080 4"; do
	read -r w h s <<< "$size"
	dims=$(val 1 "window $w $h $s")
	drv 1 "wait 20" > /dev/null
	win_h=${dims#*x}
	win_h=${win_h%% *}
	gui=${dims##* }
	if [[ $size == "854 480 2" ]]; then
		check "at $w by $h, GUI scale $s, a list of 13 kinds shows the 9 rows that fit above the hotbar, then +4 more" \
			panel_is 1 "$panel_head | $long_rows9 | +4 more"
	else
		check "at $w by $h, GUI scale $s, a list of 13 kinds shows all 10 rows, then +3 more" \
			panel_is 1 "$panel_head | $long_rows | +3 more"
	fi
	check "at $w by $h, GUI scale $s, the panel's bottom is above the hotbar's top" panel_above_hotbar
	shot 1 "material-panel-${w}x$h-gui$s"
	echo "  material-panel-${w}x$h-gui$s panel box: $(val 1 matpanelbox)" >> /out/checks.log
done
check "at GUI scale 4 the panel sits 4 from the top and left edges too" eval '[[ $(val 1 matpanelbox) == "16 16 "* ]]'
drv 1 "window 854 480 2" "wait 20" > /dev/null
# With the multiplier at 2, the heading shows it; shot at 854 by 480.
reopen
multiplier 2
drv 1 "close" > /dev/null
long_rows_2="minecraft:stone 26 0 26 red | minecraft:cobblestone 24 64 24 gold | minecraft:oak_planks 22 0 22 red | \
minecraft:spruce_planks 20 0 20 red | minecraft:birch_planks 18 0 18 red | minecraft:bricks 16 0 16 red | \
minecraft:sandstone 14 0 14 red | minecraft:glass 12 0 12 red | minecraft:white_wool 10 0 10 red | \
minecraft:red_wool 8 0 8 red"
check "with the multiplier at 2, the heading reads Material list ×2" \
	panel_is 1 "Material list ×2 | All Hold Need | ${long_rows_2% | minecraft:red_wool 8 0 8 red} | +4 more"
shot 1 material-panel-multiplier-854x480-gui2
echo "  material-panel-multiplier-854x480-gui2 panel box: $(val 1 matpanelbox)" >> /out/checks.log
# A box with no blocks: "No blocks to list." under the heading.
drv 1 "cmd tp @s 45.5 -57 45.5 0 30" "wait 40" > /dev/null
radial_open
radial_point "Material list"
radial_let_go
list_box 42.5 -60 42.5 -60 40 40 -60
drv 1 "close" > /dev/null
check "a list with no rows shows No blocks to list. under the heading" panel_is 1 "Material list | No blocks to list."
shot 1 material-panel-empty-854x480-gui2
echo "  material-panel-empty-854x480-gui2 panel box: $(val 1 matpanelbox)" >> /out/checks.log
drv 1 "setmix" "cmd clear @s minecraft:cobblestone" "cmd clear @s minecraft:stone_bricks" "ebmode $mode_before_panel" \
	> /dev/null

# Back to the box around the row and the stone beside it, with the panel off, for the saved lists' checks.
drv 1 "cmd tp @s 24.5 -51 -4.5 180 30" "wait 40" > /dev/null
radial_open
radial_point "Material list"
radial_let_go
material_box
mat_click "Info HUD"
check "Info HUD turns the panel off" eval 'drv 1 "wait 5" > /dev/null && [[ $(mat_labels) == *"Info HUD: OFF"* ]] &&
	drv 1 "close" "wait 5" > /dev/null && [[ $(val 1 matpanel) == hidden ]]'
reopen

# "Save list": its name starts as the date and time; the field takes 64 characters, not 65; "Save" is greyed out while
# the name is empty or only spaces. Shot with the date, and with the name empty.
mat_click "Save list"
check "Save list opens its screen, the name filled in with the date and time" eval \
	'drv 1 "await screen Save list" > /dev/null &&
	[[ $(val 1 widgets) =~ ^field=[A-Z][a-z]{2}\ [0-9]{1,2},\ [0-9]{1,2}:[0-9]{2}\ (AM|PM):\ [0-9\ ]+\|\ Save:\ [0-9\ ]+\|\ Cancel: ]]'
both_sizes_shot save-list
type_name ""
check "with the name empty, Save is greyed out" eval '[[ $(val 1 widgets) == "field=: "*" | Save (greyed out): "* ]]'
type_name "   "
check "and with only spaces" eval '[[ $(val 1 widgets) == "field=   : "*" | Save (greyed out): "* ]]'
both_sizes_shot save-list-empty
type_name "$(printf 'N%.0s' {1..65})"
check "a name of 65 characters can't be typed: the field holds 64" \
	eval '[[ $(val 1 widgets) == "field=$(printf "N%.0s" {1..64}): "* ]]'
widget_click 0 Cancel
check "Cancel goes back to the list's screen and saves nothing" eval \
	'drv 1 "await screen Material list for the blocks in the box" "wait 20" > /dev/null &&
	[[ $(val 1 listindex) == "last counted 10x2x3" ]]'

# "Planks", with spaces around the name, which are dropped.
save_as "  Planks "
check "Save list with the name Planks keeps the list" eval '[[ $(val 1 listindex) == "last counted 10x2x3 | Planks 10x2x3" ]]'

# A box around the row alone (10 by 2 by 1) replaces "Last counted" and leaves "Planks"; it's kept as "Row".
drv 1 "close" "wait 5" > /dev/null
radial_open
radial_point "Material list"
radial_let_go
list_box 20.5 -59 -9.5 -59 29 -10 -60
check "a new box replaces Last counted and leaves Planks" eval \
	'[[ $(val 1 listindex) == "last counted 10x2x1 | Planks 10x2x3" ]]'
save_as Row
row_saved=$SECONDS
check "a second save, Row, comes first, as the newest" eval \
	'[[ $(val 1 listindex) == "last counted 10x2x1 | Row 10x2x1 | Planks 10x2x3" ]]'
check "Planner1's lists are in their file on the server within 30 seconds of the save" eval \
	'wait_saved "Row, Planks" && ((SECONDS - row_saved <= 31))'
echo "  seconds from the save to the file: $((SECONDS - row_saved))" >> /out/checks.log
check "and no file in Planner1's game folder holds them" eval '! grep -rlF -e "Planks" -e "Row" \
	--exclude=driver-in.txt --exclude=driver-out.txt --exclude-dir=logs --exclude-dir=screenshots run/client1'

# "Saved lists": "Last counted" first, then the saved lists, newest first, each with when it was saved and its size.
save_as "Old list"
mat_click "Saved lists"
rows_re='^Last counted / .+ · 10 × 2 × 1 \| Old list / .+ · 10 × 2 × 1 \| Row / .+ · 10 × 2 × 1 \| Planks / .+ · 10 × 2 × 3$'
check "Saved lists shows Last counted first, then the saved lists, newest first, with their sizes" eval \
	'drv 1 "await screen Saved lists" > /dev/null && [[ $(val 1 savedlists) =~ $rows_re ]]'
echo "  saved lists: $(val 1 savedlists)" >> /out/checks.log
check "every row has Open, and every row but Last counted has Delete" eval \
	'[[ $(val 1 widgets | grep -o "Open: " | wc -l) == 4 && $(val 1 widgets | grep -o "Delete: " | wc -l) == 3 ]]'
both_sizes_shot saved-lists

# "Delete" asks again, then deletes; the screen follows the index the server sends back.
widget_click 0 Delete
check "Delete asks again" drv 1 "await screen Delete the list \"Old list\"?"
both_sizes_shot saved-lists-delete
check "and Old list is still kept while it asks" eval '[[ $(val 1 listindex) == *"| Old list "* ]]'
# The question's own text starts with "Delete" too, so the button is the second widget that does.
widget_click 1 Delete
check "confirmed, it deletes the list and comes back to Saved lists without it" eval \
	'drv 1 "await screen Saved lists" "wait 20" > /dev/null &&
	[[ $(val 1 listindex) == "last counted 10x2x1 | Row 10x2x1 | Planks 10x2x3" ]] &&
	[[ $(val 1 savedlists) != *"Old list"* ]]'

# "Open" on Planks waits for the server's list, makes it current with its counts as kept and opens its screen;
# "Last counted" stays the row's box.
widget_click 2 Open
check "Open on Planks opens its list" eval \
	'drv 1 "await screen Material list for the blocks in the box" "wait 20" > /dev/null &&
	planks_open=$(val 1 currentlist) && [[ $planks_open == "minecraft:overworld 20,-60,-11..29,-59,-9 "* &&
	$planks_open == *" | minecraft:oak_planks 10/4/0"* && $planks_open == *" | minecraft:stone 20/0/0"* ]]'
check "and leaves Last counted as it was" eval '[[ $(val 1 listindex) == "last counted 10x2x1 | Row 10x2x1 | Planks 10x2x3" ]]'
check "and the server's file keeps the row's box as Last counted" eval \
	'drv 1 "wait 100" > /dev/null && wait_saved "Row, Planks" &&
	[[ $(val 1 "listfile $lists_file") == "minecraft:overworld 20,-60,-10..29,-59,-10 "* ]]'
drv 1 "close" "wait 5" > /dev/null
planks_list=$(val 1 currentlist)
# The radial menu's "Material lists" button opens the current list's screen, Planks, where the panel is turned on for
# the restarts below: Planner1 carries 3 oak planks then. The screen opens once the menu has closed.
radial_open
radial_point "Material lists"
xdo 1 keyup Alt_L
check "the radial menu's Material lists button opens the current list's screen" eval \
	'drv 1 "await screen Material list for the blocks in the box" "wait 10" > /dev/null &&
	[[ $(val 1 currentlist) == "$planks_list" ]]'
mat_click "Info HUD"
drv 1 "close" "wait 60" > /dev/null
planks_panel="$panel_head | minecraft:stone 20 0 0 green | minecraft:oak_planks 10 3 4 red"

# With the panel on, its recount every 2 seconds updates only the panel and the screen: it never makes the list "Last
# counted", which only drawing a box and Refresh do. Here the panel is on with Planks current, and "Last counted" is the
# row's box (10 by 2 by 1), in the game's index and in the server's file.
# panel_matches <regex> [ticks]: the panel's text matches the regular expression, within the ticks (60 unless given).
panel_matches() {
	local i
	for i in $(seq $((${2:-60} / 5))); do
		[[ $(val 1 matpanel) =~ $1 ]] && return 0
		drv 1 "wait 5" > /dev/null
	done
	[[ $(val 1 matpanel) =~ $1 ]]
}
# open_planks: from the open list's screen, "Saved lists" and "Open" on Planks, the third row after "Last counted" and
# Row, which waits for the server's list and opens its screen.
open_planks() {
	mat_click "Saved lists"
	drv 1 "await screen Saved lists" "wait 10" > /dev/null
	widget_click 2 Open
	drv 1 "await screen Material list for the blocks in the box" "wait 20" > /dev/null
}
index_row_box="last counted 10x2x1 | Row 10x2x1 | Planks 10x2x3"
last_before=$(val 1 "listfile $lists_file")
echo "  Last counted in the server's file before: $last_before" >> /out/checks.log
check "with the panel on, Last counted is the row's box" eval \
	'[[ $last_before == "minecraft:overworld 20,-60,-10..29,-59,-10 "* && $(val 1 listindex) == "$index_row_box" ]]'
reopen
open_planks
drv 1 "close" "wait 130" > /dev/null
check "opening a saved list and waiting 6 seconds, three recounts, leaves Last counted as it was in the game" eval \
	'[[ $(val 1 listindex) == "$index_row_box" && $(val 1 currentlist) == "$planks_list" && $(val 1 matpanel) != hidden ]]'
check "and in the server's file, after the 30 seconds it takes to write" eval \
	'drv 1 "wait 560" > /dev/null && [[ $(val 1 "listfile $lists_file") == "$last_before" ]]'
# The recount follows the world: 2 of the missing planks built show as "Need 2" within 2 seconds, and "Last counted"
# is still as it was; Refresh then replaces it.
drv 1 "cmd fill 26 -60 -10 27 -60 -10 minecraft:oak_planks" "await real 27 -60 -10 minecraft:oak_planks" > /dev/null
check "2 of the missing planks built show on the panel as Need 2 within 2 seconds" \
	panel_matches '^Material list \| All Hold Need \| minecraft:stone 20 0 0 green \| minecraft:oak_planks 10 [0-9]+ 2 (red|gold)$' 45
check "while Last counted stays as it was" eval '[[ $(val 1 listindex) == "$index_row_box" ]]'
reopen
mat_click "Refresh"
check "Refresh replaces Last counted, with the panel on: the game's index and the server's file have this list" eval \
	'drv 1 "wait 10" "await listfile $lists_file" > /dev/null &&
	[[ $(val 1 listindex) == "last counted 10x2x3 | Row 10x2x1 | Planks 10x2x3" ]]'
drv 1 "close" "cmd fill 26 -60 -10 27 -60 -10 minecraft:air" "await real 27 -60 -10 minecraft:air" \
	"edit 26 -60 -10 27 -60 -10 minecraft:oak_planks" "await block 27 -60 -10 minecraft:oak_planks" "wait 5" > /dev/null
# The 2 planks are taken away and drawn again as ghost blocks. A new box, around the row alone, replaces it again, and
# Planks is opened again for the restarts below.
radial_open
radial_point "Material list"
radial_let_go
list_box 20.5 -59 -9.5 -59 29 -10 -60
check "drawing a box replaces Last counted, with the panel on: the row's box again" eval \
	'drv 1 "await listfile $lists_file" > /dev/null && [[ $(val 1 listindex) == "$index_row_box" ]]'
open_planks
check "Open on Planks makes it the current list again and keeps the panel on" eval \
	'[[ $(val 1 currentlist) == "$planks_list" && $(val 1 listindex) == "$index_row_box" ]]'
drv 1 "close" "wait 60" > /dev/null

# saved_lists_again <client> <shot>: on that client, the panel shows Planks, with "Hold" counted again (3 oak planks
# given first), and its recount leaves "Last counted" the row's box; the current list's screen, then "Saved lists" over
# it, show the same lists, and "Open" on Planks shows its counts as kept; the panel and each screen are shot, without
# toasts and with the pointer off the rows.
saved_lists_again() {
	local n=$1
	drv "$n" "await session" "await noscreen" "wait 20" "cmd clear @s" "cmd give @s minecraft:oak_planks 3" "wait 20" \
		> /dev/null || return 1
	# The panel shows Planks, and once its box's chunks load, its recount every 2 seconds brings its counts up to date.
	panel_is "$n" "$planks_panel" 200 || return 1
	drv "$n" "notoasts" "wait 5" "notoasts" "shot $2-panel" > /dev/null || return 1
	echo "  $2 panel box: $(val "$n" matpanelbox)" >> /out/checks.log
	drv "$n" "wait 40" > /dev/null
	[[ $(val "$n" listindex) == "last counted 10x2x1 | "* ]] || return 1
	drv "$n" "openlist" "await screen Material list for the blocks in the box" "wait 10" > /dev/null || return 1
	[[ $(val "$n" currentlist) == "$planks_list" ]] || return 1
	click "$n" $(val "$n" "malilibbutton Saved lists")
	drv "$n" "await screen Saved lists" "wait 10" "notoasts" "wait 5" "notoasts" "shot $2-saved-lists" > /dev/null ||
		return 1
	[[ $(val "$n" savedlists) =~ ^Last\ counted\ /\ .+\ ·\ 10\ ×\ 2\ ×\ 1\ \|\ Row\ /\ .+\ \|\ Planks\ /\ .+\ ·\ 10\ ×\ 2\ ×\ 3$ ]] ||
		return 1
	click "$n" $(val "$n" "widget 2 Open")
	# The pointer leaves the rows, whose hover box would cover the counts.
	drv "$n" "await screen Material list for the blocks in the box" > /dev/null || return 1
	xdo "$n" mousemove --window %w 5 5
	drv "$n" "wait 20" "notoasts" "wait 5" "notoasts" "shot $2-planks" > /dev/null || return 1
	echo "  $2: $(val "$n" currentlist); available: $(val "$n" available)" >> /out/checks.log
	[[ $(val "$n" currentlist) == "$planks_list" && $(val "$n" available) == *"minecraft:oak_planks 3"* ]]
}
stop_client 1
start_client 1
check "after Planner1's game restarts, the panel shows the same list, Saved lists the same lists, and Open shows Planks as kept" \
	saved_lists_again 1 lists-after-game-restart
drv 1 "close" "wait 5" > /dev/null
radial_open
radial_point "Material lists"
xdo 1 keyup Alt_L
check "and the radial menu's Material lists button opens Planks' screen" eval \
	'drv 1 "await screen Material list for the blocks in the box" "wait 10" > /dev/null &&
	[[ $(val 1 currentlist) == "$planks_list" ]]'
drv 1 "close" "wait 5" > /dev/null
stop_client 1
stop_server "$lists_log"
lists_log=server-lists-again
start_server "" "$lists_log"
start_client 1
check "and again after the server restarts" saved_lists_again 1 lists-after-server-restart
drv 1 "close" "wait 5" > /dev/null

# Planner1 playing from client 2's game folder sees the same lists; Planner2 sees none of them.
stop_client 1
start_client 2 "" Planner1
check "Planner1 playing from client 2's game folder sees the same lists" saved_lists_again 2 lists-other-computer
drv 2 "close" "wait 5" > /dev/null
stop_client 2
start_client 2
start_client 1
check "Planner2 sees none of Planner1's lists, and no panel" eval 'drv 2 "await session" "await noscreen" "wait 20" > /dev/null &&
	[[ $(val 2 listindex) == "last counted none" && $(val 2 currentlist) == none && $(val 2 matpanel) == hidden ]] &&
	drv 2 "savedlistscreen" "await screen Saved lists" "wait 10" > /dev/null && [[ -z $(val 2 savedlists) ]]'
# The game's chat toast comes once Planner1 joins after it; let it come, then clear it.
drv 2 "wait 60" "notoasts" "wait 5" "notoasts" "shot saved-lists-none" "wait 10" "close" > /dev/null
drv 1 "await session" "await noscreen" > /dev/null
check "for Planner2, with no current list, the radial menu's Material lists button opens Saved lists" eval \
	'xdo 2 keydown Alt_L > /dev/null && drv 2 "await screen Build Modes" "wait 10" > /dev/null &&
	xdo 2 mousemove --window %w $(val 2 "widget 0 Material lists") > /dev/null && drv 2 "wait 5" > /dev/null &&
	xdo 2 keyup Alt_L > /dev/null && drv 2 "await screen Saved lists" "wait 10" > /dev/null && [[ -z $(val 2 savedlists) ]]'
drv 2 "close" "wait 5" > /dev/null

# A lists file cut short: the server, restarted, logs an error at Planner1's join, renames the file with .unreadable,
# and Planner1 has no lists; a new save writes a new file.
stop_client 1
stop_server "$lists_log"
truncate -s 20 "$server_lists_file"
lists_log=server-lists-damaged
start_server "" "$lists_log"
start_client 1
check "with Planner1's lists file cut short, Planner1's join logs an error" eval \
	'drv 1 "await session" "await noscreen" "wait 20" > /dev/null && grep -qE "/ERROR\].*$uuid" "/out/$lists_log.log"'
check "the file is renamed with .unreadable" eval '[[ -f $server_lists_file.unreadable && ! -f $server_lists_file ]]'
check "and Planner1 has no lists" eval '[[ $(val 1 listindex) == "last counted none" && $(val 1 currentlist) == none ]]'
drv 1 "cmd tp @s 24.5 -51 -4.5 180 30" "wait 40" "fly true" > /dev/null
radial_open
radial_point "Material list"
radial_let_go
material_box
save_as "After damage"
check "a new save writes a new file" wait_saved "After damage"

# Messages past the server's limits, sent by the driver as the game never sends them, change nothing, each with a
# warning, and Planner1 stays connected.
before=$(wc -l < "/out/$lists_log.log")
drv 1 "pastlimits longname" "wait 10" "pastlimits overtotal Over" "wait 10" > /dev/null
check "a save_list with a name of 65 characters is dropped with a warning" eval \
	'grep -qF "Building Planner dropped Planner1'"'"'s save_list: a name of 65 characters" "/out/$lists_log.log"'
check "and one with a row whose Missing is over its Total" eval \
	'grep -qF "Building Planner dropped Planner1'"'"'s save_list: a row for " "/out/$lists_log.log"'
drv 1 "pastlimits burst Burst" "wait 20" > /dev/null
check "and the 21st list message in 10 seconds" eval \
	'grep -qF "Building Planner dropped Planner1'"'"'s save_list: more than 20 list messages in 10 seconds" "/out/$lists_log.log"'
drv 1 "wait 220" > /dev/null
check "none of them changed Planner1's lists" eval '[[ $(val 1 listindex) == "last counted 10x2x3 | After damage 10x2x3" ]] &&
	wait_saved "After damage"'
check "and Planner1 stayed connected" eval 'drv 1 "await session" > /dev/null &&
	! tail -n +$((before + 1)) "/out/$lists_log.log" | grep -qE "Planner1 lost connection|Disconnecting Planner1"'

# Refresh on a list whose box is in chunks the game doesn't hold keeps the counts, with its message; back at the box,
# with 2 more planks built, it counts again and replaces Last counted.
counts_before=$(val 1 currentlist)
drv 1 "close" "cmd tp @s 324.5 -51 -4.5 180 30" "wait 200" "openlist" \
	"await screen Material list for the blocks in the box" "wait 10" > /dev/null
mat_click "Refresh"
drv 1 "wait 10" > /dev/null
check "Refresh 300 blocks from the box keeps the counts and says why" eval \
	'[[ $(val 1 currentlist) == "$counts_before" && $(val 1 drawntexts) == *"Move closer to the box to count it again."* ]]'
both_sizes_shot material-list-refresh-far
# The server loads the box's chunk again after the teleport, before the fill can build in it; the box lies in that one
# chunk, so the game holds it again once it shows the new planks.
check "back at the box, the game shows 2 more planks built" drv 1 "close" "cmd tp @s 24.5 -51 -4.5 180 30" "wait 100" \
	"cmd fill 26 -60 -10 27 -60 -10 minecraft:oak_planks" "await real 26 -60 -10 minecraft:oak_planks" \
	"await real 27 -60 -10 minecraft:oak_planks" "wait 20"
drv 1 "openlist" "await screen Material list for the blocks in the box" "wait 10" > /dev/null
mat_click "Refresh"
check "back at the box, Refresh counts again" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 currentlist) == *" | minecraft:oak_planks 10/2/0"* ]]'
echo "  the screen after Refresh at the box: $(val 1 drawntexts)" >> /out/checks.log
check "and replaces Last counted" eval 'drv 1 "await listfile $lists_file" > /dev/null'
drv 1 "close" "wait 5" "fly false" > /dev/null

# The panel on, with that list, for the server without Building Planner.
drv 1 "openlist" "await screen Material list for the blocks in the box" "wait 10" > /dev/null
mat_click "Info HUD"
drv 1 "close" "wait 100" > /dev/null
before_panel=$(val 1 matpanel)
echo "  the panel before the server without Building Planner: $before_panel" >> /out/checks.log

# On the same server with Building Planner left out, N does nothing, and the material list panel doesn't show; with
# Building Planner back, the panel shows again with the same list.
stop_client 1
stop_client 2
cp /out/client1.log /out/client1-with-addon.log
cp /out/client2.log /out/client2-with-addon.log
stop_server "$lists_log"
check "the server starts without Building Planner" eval 'start_server "$ADDON" server-without-addon &&
	! grep -qF "Building Planner loaded" /out/server-without-addon.log'
start_client 1
# The loading screen swallows key presses, so wait for it to close.
check "client 1 starts no session" drv 1 "await noscreen" "wait 40" "await nosession"
drv 1 "cmd tp @s 5 -60 -3 0 10" "wait 20" > /dev/null
press 1 n
check "N opens nothing and leaves ghost blocks shown" eval 'drv 1 "wait 20" "await noscreen" > /dev/null &&
	grep -q "^ghost-blocks-shown=true" "$config"'
check "and shows no Building Planner message" no_planner_message 1
shot 1 keys-not-on-server
check "and the material list panel doesn't show" eval '[[ $(val 1 matpanel) == hidden ]]'
stop_client 1
stop_server server-without-addon
start_server "" server-addon-back
start_client 1
check "with Building Planner back, the panel shows again with the same list" eval \
	'[[ $before_panel == "Material list | All Hold Need | "* ]] && drv 1 "await session" "await noscreen" > /dev/null &&
	panel_is 1 "$before_panel" 200'
shot 1 material-panel-addon-back
