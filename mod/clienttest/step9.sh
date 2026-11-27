# Step 9's in-game checks: showing and hiding ghost blocks with N and the radial menu's button, drawing while they're
# hidden, the choice kept across a restart, where the radial menu's buttons sit, with screenshots, the menu's corner
# text, the key headings in Controls, the box with its list under the crosshair, copy and paste with the copy at the
# aim worked by the mouse and its keys, its Line, Wall and Floor, the copy's colours, Delete's marking by sweeping and
# with a box, with what its marks cost, its erasing and its lines, the material list, the counted list kept on the server
# across restarts, the list screen's buttons, saving, opening and deleting lists, the lists
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
# With no mouse mode on, Single's line shows for the stone bricks, which neither turn nor flip.
single_line="[Right-click: place][Left-click: erase][H: Lock (Off)][Esc: exit mode]"
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
# The buttons' boxes go to the log, for outlining. It fails unless the buttons meet nothing the menu draws, each other
# or the screen's edges; the speed sliders show only in Single, where step10.sh checks them with the buttons.
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

# Without a copy, the menu shows seven buttons, each with its hover text, at its middle from the menu's middle, in the
# columns of the wrench, undo and redo: show or hide 13 below it, under the wrench, Copy, Paste and Delete in the row 39
# above it, and Block mix, Material list and Material lists in the row 65 above it.
# Shot with the pointer on the menu's middle, over no button, then with the pointer on Block mix, for its hover text.
slots="Show or hide ghost blocks [N] @ -157,13 | Copy @ -157,-39 | Paste @ -131,-39 | Delete @ -105,-39 | \
Block mix [B] @ -157,-65 | Material list @ -131,-65 | Material lists @ -105,-65"
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
# In Single, the speed sliders' headings and values, exactly as Geo approved them (1391).
check "in Single, the headings \"Build speed\" and \"Break speed\" over \"5 blocks/s\" and \"3.33 blocks/s\"" eval \
	'drawn=" | $(val 1 drawntexts) | " && [[ $drawn == *" | Build speed | "* && $drawn == *" | Break speed | "* &&
	$(val 1 texts) == "5 blocks/s | 3.33 blocks/s | "* ]]'
radial_point "Delete"
drv 1 "wait 10" "notoasts" "shot radial-delete-hover-854x480-gui2" > /dev/null
echo "  radial-delete-hover-854x480-gui2 buttons: $(val 1 buttons)" >> /out/checks.log
radial_point "Block mix"
drv 1 "wait 10" "notoasts" "shot radial-block-mix-hover" > /dev/null
xdo 1 mousemove --window %w 5 5
drv 1 "wait 5" > /dev/null
check "letting the key go with the pointer moved off Block mix closes the menu and opens nothing" radial_let_go

# Hook E34: under the wrench, where Effortless Building shows operators its "Functional Settings" button, the menu
# shows the show or hide button alone, for Planner1 an operator and, made one no longer, not. With the pointer there,
# the frame sets the show or hide button's hover text as its only tooltip, where the menu would set its own button's
# after it. Planner2, made an operator for the moment, makes Planner1 one again.
under_wrench() {
	local tips
	radial_open
	radial_point "Show or hide ghost blocks"
	drv 1 "wait 5" > /dev/null
	tips=$(val 1 tooltips)
	echo "  under the wrench, $(val 1 operator): tooltips $tips" >> /out/checks.log
	xdo 1 mousemove --window %w 5 5
	drv 1 "wait 5" > /dev/null
	radial_let_go
	[[ $tips == "Show or hide ghost blocks [N]" ]]
}
check "Planner1 is an operator" eval '[[ $(val 1 operator) == operator ]]'
check "and the menu shows no \"Functional Settings\" under the wrench, only the show or hide button" under_wrench
drv 1 "cmd op Planner2" "wait 10" "cmd deop Planner1" "wait 20" > /dev/null
check "once Planner1 is no operator" eval '[[ $(val 1 operator) == "not an operator" ]]'
check "the menu shows the show or hide button alone there too" under_wrench
drv 2 "cmd op Planner1" "wait 10" "cmd deop Planner2" "wait 20" > /dev/null
check "and Planner2 makes Planner1 an operator again" eval '[[ $(val 1 operator) == operator ]]'

# Controls: Building Planner's keys, the eight of the copy and the one ghost block among them, and Effortless Building's
# four show under the "Building Planner" heading, in the order Controls sorts them, and no raw key name. The driver's
# own Page Up key, under a heading of its own, is unbound first, so Controls shows no clash with "Nudge up". The list is
# scrolled to the heading and shot at 854 by 480 and 1920 by 1080, GUI scale 2, and at 1920 by 1080, GUI scale 4, with
# the heading's, the keys' and each key row's window boxes (left top right bottom) in the log.
drv 1 "unbindprobe" "keybinds" "wait 10" > /dev/null
echo "  controls: $(val 1 keybindlist)" >> /out/checks.log
planner_keys="Block mix, Change copy mode, Lock or unlock, Nudge back, Nudge down, Nudge forward, Nudge left, \
Nudge right, Nudge up, Open Modifier Settings, Open Radial Menu, Redo, Show or hide ghost blocks, Undo"
check "Controls shows the eight keys of the copy and the one ghost block, and Effortless Building's four under \"Building Planner\", with its own" \
	eval '[[ $(val 1 keybindlist) == *"Building Planner: $planner_keys ; "* ]]'
check "under one heading" eval '(($(val 1 keybindlist | grep -o "Building Planner: " | wc -l) == 1))'
check "with no raw key name" eval '[[ $(val 1 keybindlist) != *"key."* ]]'
drv 1 "close" "wait 5" > /dev/null
# controls_shot <width> <height> <gui scale> <name>: Controls scrolled to the heading, in a window of that size, shot.
controls_shot() {
	drv 1 "window $1 $2 $3" "wait 20" "keybinds" "wait 10" > /dev/null
	echo "  controls boxes $4: $(val 1 "keybindscroll Building Planner")" >> /out/checks.log
	echo "  controls rows $4: $(val 1 "keybindrows Building Planner")" >> /out/checks.log
	drv 1 "wait 5" "notoasts" > /dev/null
	shot 1 "$4"
	drv 1 "close" "wait 5" > /dev/null
}
controls_shot 854 480 2 controls-854x480
controls_shot 1920 1080 2 controls-1920x1080
controls_shot 1920 1080 4 controls-1920x1080-gui4
drv 1 "window 854 480 2" "wait 20" > /dev/null

# The Paste button before any copy gives a message.
nothing_lines=$(chat_count 1 "Nothing to paste. Use Copy first.")
radial_open
radial_click "Paste"
radial_let_go
check "the radial menu's Paste button with no copy gives a message" \
	said_once_more 1 "Nothing to paste. Use Copy first." "$nothing_lines"
check "and shows no copy, Single's line showing" eval '[[ $(val 1 copy) == none ]] && [[ $(val 1 modehud) == "$single_line" ]]'
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

# The copy at the aim's controls lines, with the copy's keys as bound by default, and a box's controls for each click.
# Before the player's first paste, the copy's line in Off; after it, with the sprint key, which lines the copy up
# while the copy follows the aim; a locked copy and a set don't line up, so their lines leave the sprint key out. H
# names the lock's state and R the copy's mode, in every line.
turn_mirror="[Scroll: turn][Middle-click: mirror]"
lock_off="[H: Lock (Off)]"
lock_on="[H: Lock (On)]"
controls="[Right-click: paste]$turn_mirror$lock_off[R: Mode (Off)][Esc/Left-click: close]"
line_up="[Hold Left Control: line up]"
controls_ctrl="[Right-click: paste]$turn_mirror$line_up$lock_off[R: Mode (Off)][Esc/Left-click: close]"
nudges="[Arrow keys + Page Up/Down: nudge]"
locked_line="[Right-click: paste]$nudges$turn_mirror$lock_on[R: Mode (Off)][Esc/Left-click: close]"
outside="[Can't paste: part of the copy is outside the world]"
locked_red="$outside$nudges$turn_mirror$lock_on[R: Mode (Off)][Esc/Left-click: close]"
# start_line <Mode> [locked] [<reason>]: the copy's line in Line, Wall or Floor before the first click, locked or not,
# with the reason before setting the start while the copy is red.
start_line() {
	if [[ ${2:-} == locked ]]; then
		echo "${3:-}[Right-click: set the start]$nudges$turn_mirror$lock_on[R: Mode ($1)][Esc/Left-click: close]"
	else
		echo "[Right-click: set the start]$turn_mirror$line_up$lock_off[R: Mode ($1)][Esc/Left-click: close]"
	fi
}
# set_line <Mode> <paste> [locked]: the copy's line after the first click, with the paste (or the reason) given.
set_line() {
	if [[ ${3:-} == locked ]]; then
		echo "$2$nudges$turn_mirror$lock_on[R: Mode ($1)][Esc/Left-click: cancel]"
	else
		echo "$2$turn_mirror$lock_off[R: Mode ($1)][Esc/Left-click: cancel]"
	fi
}
first_line="[Left-click: set the first corner][Esc/Right-click: cancel]"
second_line="[Left-click: set the opposite corner][Esc/Right-click: cancel]"
height_line="[Left-click: set the height][Esc/Right-click: cancel]"
# Delete's lines with no marks, with marks, and during a hold of Left Ctrl that unmarks.
delete_none="[Hold Left Control: toggle mark][Hold Left Control + Left-click: mark a box][Esc/Right-click: close]"
delete_marked="[Hold Left-click: erase][Hold Left Control: toggle mark][Hold Left Control + Left-click: mark a box][Esc/Right-click: clear]"
delete_unmarking="[Hold Left-click: erase][Hold Left Control: unmark][Hold Left Control + Left-click: unmark a box][Esc/Right-click: clear]"
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
# rects_ok <window width> <gui scale> [<driver command>]: Planner1's controls as drawn now (the driver's moderects, or
# the command given) are rows of rectangles, each one line (9) high, 4 apart in its row, the rows 2 apart; each row is
# centred and inside the screen less 4 at each side, unless it's one rectangle wider than that.
rects_ok() {
	val 1 "${3:-moderects}" | awk -v W="$1" -v S="$2" '
		BEGIN { RS = " / "; bad = 0; last = "" }
		{
			sub(/\n$/, "")
			n = split($0, r, / \| /)
			for (i = 1; i <= n; i++) {
				split(r[i], f, " ")
				if (f[4] - f[2] != 9 * S) bad = 1
				if (i == 1) { top = f[2]; left = f[1]; if (last != "" && top != last + 11 * S) bad = 1 }
				else if (f[2] != top || f[1] != right + 4 * S) bad = 1
				right = f[3]
			}
			if (n > 1 && (left < 4 * S || right > W - 4 * S)) bad = 1
			if (left + right < W - 2 * S || left + right > W + 2 * S) bad = 1
			last = top
		}
		END { exit bad || NR == 0 }'
}
# The name beside the hotbar: the build mode's, or the tool's while a box for Copy or the material list is drawn or
# Delete is on. At 854 by 480, GUI scale 2, the hotbar runs from 122 to 304 GUI pixels across and from 218 to 240 down,
# and the off hand's slot from 93 to 122 across: the name's text ends 2 inside its rectangle, which ends 4 left of
# either, and runs from 224 to 233 down, centred on the hotbar.
# mode_name_at <name> <right>: the name shows once, its text ending at <right> in the window's pixels.
mode_name_at() {
	local box
	box=$(val 1 "drawnbox $1")
	echo "  mode name $1: $box" >> /out/checks.log
	[[ $box != *"|"* ]] && [[ $(cut -d" " -f2- <<< "$box") == "448 $2 466" ]]
}
# line_shot <name> [<width> <height> <gui scale> [<part>]]: Planner1's view at noon at that size (1920 by 1080, GUI
# scale 4, unless given) as <name>, once the chat lines that draw over the controls line have faded, with the
# controls' rows and their window box (left top right bottom) in the log, and that of the rectangle holding <part> if
# given, for the shot's outline, and a check that the rectangles are laid out in rows (rects_ok); then back to 854 by
# 480, GUI scale 2.
line_shot() {
	drv 1 "cmd time set noon" "wait 210" "window ${2:-1920} ${3:-1080} ${4:-4}" "wait 20" "notoasts" "wait 5" \
		"shot $1" "wait 20" > /dev/null
	echo "  $1: $(val 1 modewrap) | box $(val 1 modebox)${5:+ | part $(val 1 "modepart $5")} | name $(val 1 "drawnbox Paste")" \
		>> /out/checks.log
	check "$1: each control is its own rectangle, in centred rows" rects_ok "${2:-1920}" "${4:-4}"
	drv 1 "window 854 480 2" "wait 20" > /dev/null
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
check "with its controls line" eval '[[ $(val 1 modehud) == "$first_line" ]]'
check "and the name Copy shows left of the hotbar, in Single's place" mode_name_at Copy 232
check "and no message in chat" eval '(($(chat_lines 1) == lines))'
use_click 1
check "a right click ends the box, and Single's line shows again" eval 'drv 1 "wait 10" > /dev/null &&
	box_is "none SINGLE" && [[ $(val 1 modehud) == "$single_line" ]]'
check "placing no block, real or ghost, and saying nothing" eval '[[ $(val 1 holdlog) == *" edits=0 "* ]] &&
	[[ $(val 1 "real 8 -60 19") == minecraft:air ]] && [[ $(val 1 "layer 8 -60 19") == minecraft:air ]] &&
	(($(chat_lines 1) == lines))'
# Esc ends a box as its right click does ("Esc"), with no screen opening, and Single's mode stays.
copy_button
drv 1 "wait 5" > /dev/null
press 1 Escape
check "Esc ends the box, as its right click does, pausing nothing" eval 'box_is "none SINGLE" &&
	[[ $(val 1 screenclass) == none ]] && [[ $(val 1 modehud) == "$single_line" ]] && (($(chat_lines 1) == lines))'

# The first corner: the space above the block aimed at for its top face, and the block itself for a side. Until the
# first click, the top face aimed at shows shaded black, and a side shows nothing. The first click, with the aim held,
# leaves the same box, so the shots before and after it differ only by the shade: with the animals gone, the view held
# still and the box's outline and sides light, the shot with the shade has many pixels darker than the one without, and
# that one hardly any darker than the first (shotdarker).
drv 1 "cmd kill @e[type=!minecraft:player]" "wait 5" > /dev/null
copy_button
drv 1 "lookat 5.5 -59 4.5" "wait 5" > /dev/null
check "aiming at a block's top face, the box's first corner is the space above it" \
	box_is "COPY 0 5,-59,4..5,-59,4 white SINGLE"
check "and the block's top face shows shaded" eval '[[ $(val 1 boxshade) == 5,-60,4 ]]'
shot 1 box-shade-top
drv 1 "window 1920 1080 2" "wait 20" > /dev/null
shot 1 box-shade-top-1920x1080
drv 1 "window 854 480 2" "wait 20" > /dev/null
drv 1 "lookat 4.5 -58.5 3.9" "wait 5" > /dev/null
check "aiming at its side, the block itself" box_is "COPY 0 4,-59,3..4,-59,3 white SINGLE"
check "with no face shaded" eval '[[ $(val 1 boxshade) == none ]]'
line_shot line-copy-first

# A box from the space above the stone brick at 5 -60 4 to 3 -60 3, around 6 stone bricks, the stairs and the
# glowstone. Between clicks it follows the aim, with the corners set so far fixed; it's shot after the first and the
# second click, once the chat lines that draw over its line have faded.
drv 1 "lookat 5.5 -59 4.5" "wait 5" > /dev/null
shot 1 box-shade-before
attack_click 1
check "the first click, with the aim held, leaves the same box" box_is "COPY 1 5,-59,4..5,-59,4 white SINGLE"
check "and no face shaded" eval '[[ $(val 1 boxshade) == none ]]'
shot 1 box-shade-gone
shade_darker=$(val 1 "shotdarker box-shade-before box-shade-gone")
shade_lighter=$(val 1 "shotdarker box-shade-gone box-shade-before")
echo "  pixels the shade darkens: $shade_darker; pixels darker without it: $shade_lighter" >> /out/checks.log
check "the shade darkens the view where it's drawn" eval '((shade_darker > 500 && shade_lighter < 200))'
drv 1 "lookat 6.5 -59 6.5" "wait 5" > /dev/null
check "the first click sets the first corner, and the box follows the aim on its level" eval \
	'box_is "COPY 1 5,-59,4..6,-59,6 white SINGLE" && [[ $(val 1 modehud) == "$second_line[2 × 1 × 3]" ]]'
drv 1 "lookat 3.5 -59 3.5" "wait 5" > /dev/null
check "to the opposite corner" eval 'box_is "COPY 1 3,-59,3..5,-59,4 white SINGLE" &&
	[[ $(val 1 modehud) == "$second_line[3 × 1 × 2]" ]]'
check "the list under the crosshair counts the box's blocks: the stairs and the glowstone" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 boxlist) == *" | minecraft:glowstone 1, minecraft:oak_stairs 1" ]]'
# Its row of two cells, from 223 to 261 across and 112 to 130 down at GUI scale 2, sits on a dark background 2 wider at
# each side and 1 taller at each end, in the window's pixels.
backgrounds=$(val 1 boxlistbackgrounds)
echo "  the list's backgrounds: $backgrounds" >> /out/checks.log
check "and sits on the controls' dark background" eval '[[ $backgrounds == "442 222 526 262" ]]'
line_shot line-copy-second
drv 1 "wait 200" > /dev/null
shot 1 box-copy-first-click
drv 1 "window 1920 1080 2" "wait 20" > /dev/null
shot 1 box-copy-first-click-1920x1080
drv 1 "window 854 480 2" "wait 20" > /dev/null
attack_click 1
drv 1 "lookat 3.5 -57.5 3.5" "wait 5" > /dev/null
check "the second click sets the opposite corner, and the height follows the aim" eval \
	'box_is "COPY 2 3,-59,3..5,-58,4 white SINGLE" && [[ $(val 1 modehud) == "$height_line[3 × 2 × 2]" ]]'
drv 1 "lookat 3.5 -59.5 3.5" "wait 5" > /dev/null
check "down to the stone bricks" eval 'box_is "COPY 2 3,-60,3..5,-59,4 white SINGLE" &&
	[[ $(val 1 modehud) == "$height_line[3 × 2 × 2]" ]]'
check "and the list counts the stone bricks too, most numerous first" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 boxlist) == *" | minecraft:stone_bricks 6, minecraft:glowstone 1, minecraft:oak_stairs 1" ]]'
line_shot line-copy-height
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
check "on open ground, Building Planner draws the copy's box blue" drv 1 "await copycolour BLUE"
check "and Litematica draws no box for it (hook L6)" eval '[[ $(val 1 boxes) == *"others: none" ]]'
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
line_shot line-single
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
check "over ghost blocks its stone bricks would replace, the copy is yellow" drv 1 "await copycolour YELLOW"
shots copy-yellow

# A right click pastes it where it shows: its 8 blocks, the stairs still facing west, and its empty space leaves the
# glass. The aim runs on to the real grass at 15 -61 5, which the click leaves.
drv 1 "cmd tp Planner2 18.5 -57 11.5 150 30" "wait 40" > /dev/null
use_click 1
check "a right click pastes the copy where it shows, for the other player" drv 2 \
	"await block 14 -58 3 minecraft:oak_stairs" "await block 14 -58 4 minecraft:glowstone" \
	"await block 15 -59 5 minecraft:stone_bricks" "await block 14 -59 3 minecraft:stone_bricks"
check "glowing with Planner1's name" drv 2 "await glow Planner1:8"
shot 2 paste-seen
check "turned and mirrored as the copy showed it" state_has 2 "14 -58 3" "facing=west"
check "the copy's empty space leaves the ghost blocks under it" eval '[[ $(val 2 "get 15 -58 4") == minecraft:glass ]] &&
	[[ $(val 2 "get 15 -58 5") == minecraft:glass ]] && [[ $(val 2 "get 14 -58 6") == minecraft:glass ]]'
check "the click leaves the real grass" eval '[[ $(val 1 "real 15 -61 5") == minecraft:grass_block ]]'
check "and the copy stays at the aim for another paste, with its controls line, which now names the sprint key" eval \
	'copy_is 1 "^at [-0-9,.]+ FRONT_BACK COUNTERCLOCKWISE_90 unselected 8 blocks$" &&
	[[ $(val 1 modehud) == "$controls_ctrl" ]]'
line_shot line-single-after-paste
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

# Locking and nudging, on the grass west of the rest. Aimed from the south at -30 -61 30's top, the copy, 2 by 2 by 3
# blocks as it's turned, rests with the middle of its bottom on -30 -60 30: it fills -30..-29 east to west and 29..31
# north to south.
drv 1 "cmd tp @s -29.5 -55 36.5 180 30" "wait 40" > /dev/null
aim 1 -30 -60 30
check "the copy rests on the grass, unlocked" eval 'drv 1 \
	"await copy at -30,-60,29..-29,-59,31 FRONT_BACK COUNTERCLOCKWISE_90" > /dev/null && [[ $(val 1 copylock) == unlocked ]]'
press 1 h
check "H locks the copy" eval '[[ $(val 1 copylock) == locked ]]'
check "and its controls line says it's locked, with the nudge keys" eval '[[ $(val 1 modehud) == "$locked_line" ]]'
drv 1 "cmd tp @s -22.5 -56 39.5 150 25" "wait 40" > /dev/null
aim 1 -24 -60 33
check "walking away and aiming elsewhere leaves the locked copy where it was" eval 'drv 1 "wait 10" > /dev/null &&
	copy_is 1 "^at -30,-60,29\.\.-29,-59,31 FRONT_BACK COUNTERCLOCKWISE_90 "'
# Shot with the crosshair on the grass to its right, where an unlocked copy would be.
drv 1 "lookat -24.5 -60.5 31.5" "wait 5" > /dev/null
check "and stays there for the shot" copy_is 1 "^at -30,-60,29\.\.-29,-59,31 "
check "the name Paste shows left of the hotbar, in Single's place, with the copy locked" mode_name_at Paste 232
shots copy-locked
line_shot line-locked
line_shot line-locked-854x480-gui2 854 480 2
# Facing north, each key moves the locked copy one block: Up north, away from the view, Down south, Left west, Right
# east, Page Up up and Page Down down.
drv 1 "cmd tp @s -29.5 -55 40.5 180 30" "wait 40" > /dev/null
# nudge <xdotool key> <what it does> <the copy's box after>: presses the key and checks the copy moved there.
nudge() {
	press 1 "$1"
	check "$2" drv 1 "await copy at $3 FRONT_BACK COUNTERCLOCKWISE_90"
}
nudge Up "facing north, Up moves the locked copy one block north, away from the view" -30,-60,28..-29,-59,30
nudge Down "Down one block south, toward the view" -30,-60,29..-29,-59,31
nudge Left "Left one block west" -31,-60,29..-30,-59,31
nudge Right "Right one block east" -30,-60,29..-29,-59,31
nudge Prior "Page Up one block up" -30,-59,29..-29,-58,31
nudge Next "Page Down one block down" -30,-60,29..-29,-59,31
# Held down, Up moves it once at the press, again after 0.2 seconds, then every 4 ticks: 1 + (ticks down - 1) / 4
# blocks, 5 a second.
# copy_min_z: the north edge of the copy at the aim.
copy_min_z() {
	val 1 copy | sed -E 's/^at [^,]+,[^,]+,([-0-9]+)\.\..*/\1/'
}
start_z=$(copy_min_z)
drv 1 "close" "wait 5" "keyticks FORWARD reset" > /dev/null
xdo 1 keydown Up
drv 1 "wait 40" > /dev/null
xdo 1 keyup Up
drv 1 "wait 5" > /dev/null
held_ticks=$(val 1 "keyticks FORWARD")
moved=$((start_z - $(copy_min_z)))
echo "  Up held for $held_ticks ticks moved the copy $moved blocks north" >> /out/checks.log
check "a held key moves it again after 0.2 seconds, then every 4 ticks, about 5 blocks a second" eval \
	'((held_ticks >= 20 && moved == 1 + (held_ticks - 1) / 4))'
# H unlocks it, and it rests on the aimed face again: aimed at -26 -61 34's top, it fills -26..-25 and 33..35.
aim 1 -26 -60 34
press 1 h
check "H unlocks it" eval '[[ $(val 1 copylock) == unlocked ]]'
check "and it follows the aim again" drv 1 "await copy at -26,-60,33..-25,-59,35 FRONT_BACK COUNTERCLOCKWISE_90"
# Locked there, a right click pastes it, and the copy unlocks: aimed at -30 -61 30's top, away from the paste, it
# follows the aim, filling -30..-29 and 29..31. With the paste erased, it's locked again where it was pasted.
press 1 h
use_click 1
check "a right click pastes the locked copy" drv 2 "await block -26 -59 33 minecraft:oak_stairs" \
	"await block -25 -60 35 minecraft:stone_bricks"
check "and the copy unlocks, its line naming the sprint key again" eval \
	'[[ $(val 1 copylock) == unlocked ]] && [[ $(val 1 modehud) == "$controls_ctrl" ]]'
aim 1 -30 -60 30
check "and follows the aim" drv 1 "await copy at -30,-60,29..-29,-59,31 FRONT_BACK COUNTERCLOCKWISE_90"
shots copy-unlocked-after-paste
drv 1 "edit -26 -60 33 -25 -59 35 minecraft:air" "await block -26 -59 33 minecraft:air" \
	"await block -25 -60 35 minecraft:air" > /dev/null
aim 1 -26 -60 34
drv 1 "await copy at -26,-60,33..-25,-59,35 FRONT_BACK COUNTERCLOCKWISE_90" > /dev/null
press 1 h
check "H locks it again where it was pasted" eval '[[ $(val 1 copylock) == locked ]] &&
	copy_is 1 "^at -26,-60,33\.\.-25,-59,35 FRONT_BACK COUNTERCLOCKWISE_90 "'
# With Freecam's camera on, facing east while Planner1 faces north, Up moves the copy east, away from the camera.
check "Freecam's camera comes on, facing east" eval '[[ $(val 1 "freecam true") == true ]] &&
	drv 1 "wait 10" > /dev/null && [[ $(val 1 "camtp -34.5 -56.5 34.5 -90 30") == "FreeCamera -34.5,-56.5,34.5 east" ]]'
press 1 Up
check "with Freecam's camera on, Up moves the locked copy away from the camera" \
	drv 1 "await copy at -25,-60,33..-24,-59,35 FRONT_BACK COUNTERCLOCKWISE_90"
check "Freecam's camera goes off" eval '[[ $(val 1 "freecam false") == false ]] && drv 1 "wait 10" > /dev/null'
# The copy's colours as it's locked and nudged: blue on the grass; red with part of it below the build height (the
# world's bottom is -64), where a right click pastes nothing and sends no edit; red again with part of it outside the
# world border, shot there; blue once the border is back.
check "on the grass, the locked copy is blue" drv 1 "await copycolour BLUE"
press 1 Next
press 1 Next
press 1 Next
press 1 Next
check "nudged down to the world's bottom, it's still blue" eval \
	'drv 1 "await copy at -25,-64,33..-24,-63,35 FRONT_BACK COUNTERCLOCKWISE_90" > /dev/null &&
	[[ $(val 1 copycolour) == BLUE ]]'
press 1 Next
check "one block lower, part of it is below the build height, and it's red" eval \
	'drv 1 "await copy at -25,-65,33..-24,-64,35 FRONT_BACK COUNTERCLOCKWISE_90" > /dev/null &&
	drv 1 "await copycolour RED BUILD_HEIGHT" > /dev/null'
check "its controls line says why in place of the paste" eval '[[ $(val 1 modehud) == "$locked_red" ]]'
lines=$(chat_lines 1)
use_click 1
check "a right click on the red copy pastes nothing and sends no edit" eval 'drv 1 "wait 20" > /dev/null &&
	[[ $(val 1 holdlog) == *" edits=0 "* ]] && (($(chat_lines 1) == lines))'
check "and the copy stays locked where it was" eval '[[ $(val 1 copylock) == locked ]] &&
	copy_is 1 "^at -25,-65,33\.\.-24,-64,35 "'
for _ in 1 2 3 4 5; do press 1 Prior; done
check "nudged back up onto the grass, it's blue again" eval \
	'drv 1 "await copy at -25,-60,33..-24,-59,35 FRONT_BACK COUNTERCLOCKWISE_90" > /dev/null &&
	drv 1 "await copycolour BLUE" > /dev/null'
check "and its controls line offers the paste again" eval '[[ $(val 1 modehud) == "$locked_line" ]]'
# A border 200 blocks across whose north edge is at z 34 (its centre has decimals, so the game doesn't move it to a
# block's middle): the copy's north row, at z 33, is outside, and Planner1, at z 40.5, is inside.
drv 1 "cmd worldborder center -25.0 134.0" "cmd worldborder set 200" > /dev/null
check "with part of it outside the world border, it's red" drv 1 "await copycolour RED WORLD_BORDER"
check "and its controls line says why" eval '[[ $(val 1 modehud) == "$locked_red" ]]'
lines=$(chat_lines 1)
use_click 1
check "and its right click pastes nothing and sends no edit" eval 'drv 1 "wait 20" > /dev/null &&
	[[ $(val 1 holdlog) == *" edits=0 "* ]] && (($(chat_lines 1) == lines))'
check "Litematica draws no box for the copy" eval '[[ $(val 1 boxes) == *"others: none" ]]'
drv 1 "lookat -24.5 -60 33.5" > /dev/null
shots copy-red-border
line_shot line-locked-red
check "the reason's rectangle is red, and the controls' white" eval \
	'[[ $(val 1 modecolours) == "red white white white white white white" ]]'
line_shot line-locked-red-854x480-gui2 854 480 2
line_shot line-locked-red-1920x1080-gui2 1920 1080 2
check "at 854 by 480, GUI scale 2, the locked line goes on in a row below, moving whole rectangles" eval \
	'[[ $(val 1 modewrap) == "$outside"*"] / ["*"[Esc/Left-click: close]" ]] && rects_ok 854 2'
# R changes a locked copy's mode, and a red copy in Line, Wall or Floor before its first click says why before
# "Right-click: set the start", since that click still fixes the first copy; R at Floor is back to Off.
for mode in Line Wall Floor; do
	press 1 r
	check "R changes the locked red copy's mode to $mode, its line giving the reason before setting the start" eval \
		'[[ $(val 1 copymode) == ${mode^^} ]] && [[ $(val 1 copylock) == locked ]] &&
		[[ $(val 1 modehud) == "$(start_line $mode locked "$outside")" ]]'
	line_shot "line-${mode,,}-red"
done
press 1 r
check "R at Floor is back to Off, the locked red line as before" eval \
	'[[ $(val 1 copymode) == OFF ]] && [[ $(val 1 modehud) == "$locked_red" ]]'
drv 1 "cmd worldborder center 0.0 0.0" "cmd worldborder set 59999968" > /dev/null
check "with the border back, it's blue" drv 1 "await copycolour BLUE"
press 1 h
check "and H unlocks the copy again" eval '[[ $(val 1 copylock) == unlocked ]] &&
	[[ $(val 1 modehud) == "$controls_ctrl" ]]'
# The keys as the player rebinds them, as Controls does: the lock key to K, the mode key to G, the nudge forward to I
# and the sprint key to Right Control. The line names each as bound, and the four nudges one by one once they're off
# the arrow keys; then each key goes back.
rebind() {
	drv 1 "bindkey key.buildingplanner.lock_copy $1" "bindkey key.buildingplanner.copy_mode $2" \
		"bindkey key.buildingplanner.nudge_forward $3" "bindkey key.sprint $4" "wait 2" > /dev/null
}
rebind key.keyboard.k key.keyboard.g key.keyboard.i key.keyboard.right.control
check "with the keys rebound, the controls line names them as bound" eval '[[ $(val 1 modehud) == \
	"[Right-click: paste]$turn_mirror[Hold Right Control: line up][K: Lock (Off)][G: Mode (Off)][Esc/Left-click: close]" ]]'
line_shot line-rebound
press 1 k
check "K then locks the copy, and the locked line names the nudge keys one by one" eval \
	'[[ $(val 1 copylock) == locked ]] && [[ $(val 1 modehud) == "[Right-click: paste][I/Down Arrow/Left Arrow/Right Arrow + Page Up/Down: nudge]$turn_mirror[K: Lock (On)][G: Mode (Off)][Esc/Left-click: close]" ]]'
press 1 k
rebind key.keyboard.h key.keyboard.r key.keyboard.up key.keyboard.left.control
check "and with the keys back, K has unlocked it and the line names H and R again" eval \
	'[[ $(val 1 copylock) == unlocked ]] && [[ $(val 1 modehud) == "$controls_ctrl" ]]'

# Line, Wall and Floor, on the grass west of the locking checks. Aimed from the south at -45 -61 34's top, the copy, 2
# by 2 by 3 blocks as it's turned, fills -45..-44 east to west and 33..35 north to south; its middle block, from which
# Effortless Building's Line, Wall and Floor find the set's far end, is -45 -60 34. R cycles the copy's mode, and H
# locks the copy in each.
drv 1 "cmd tp @s -44.5 -55 42.5 180 30" "wait 40" > /dev/null
aim 1 -45 -60 34
check "the copy's mode starts as Off" eval '[[ $(val 1 copymode) == OFF ]] &&
	drv 1 "await copy at -45,-60,33..-44,-59,35 FRONT_BACK COUNTERCLOCKWISE_90" > /dev/null'
for mode in Line Wall Floor; do
	press 1 r
	check "R switches the copy to $mode, with its controls line" eval '[[ $(val 1 copymode) == ${mode^^} ]] &&
		[[ $(val 1 modehud) == "$(start_line $mode)" ]]'
	press 1 h
	check "locked in $mode, its line says both" eval '[[ $(val 1 copylock) == locked ]] &&
		[[ $(val 1 modehud) == "$(start_line $mode locked)" ]]'
	line_shot "line-${mode,,}-locked"
	press 1 h
	check "and H unlocks it again" eval '[[ $(val 1 copylock) == unlocked ]] &&
		[[ $(val 1 modehud) == "$(start_line $mode)" ]]'
done
press 1 r
check "then R is back to Off" eval '[[ $(val 1 copymode) == OFF ]] && [[ $(val 1 modehud) == "$controls_ctrl" ]]'
press 1 r
check "and to Line again, the copy still following the aim" eval '[[ $(val 1 copymode) == LINE ]] &&
	copy_is 1 "^at -45,-60,33\.\.-44,-59,35 FRONT_BACK COUNTERCLOCKWISE_90 " && [[ $(val 1 copyset) == none ]]'
# first_click: aimed at -45 -61 34's top, the right click that fixes the copy there as the set's first copy.
first_click() {
	aim 1 -45 -60 34
	drv 1 "await copy at -45,-60,33..-44,-59,35" > /dev/null
	use_click 1
}
# undo_paste <block>...: Ctrl+Z, and client 2 sees air at each of the blocks (x y z).
undo_paste() {
	drv 1 "close" > /dev/null
	keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
	local args=() pos
	for pos in "$@"; do args+=("await block $pos minecraft:air"); done
	drv 2 "${args[@]}"
}
# The first click fixes the copy as the set's first copy and sends no edit. Aimed at the copy's own top, Line's block
# is in the first copy, so the set is that copy alone.
first_click
check "in Line the first click fixes the copy as the set's first copy, sending no edit" eval \
	'[[ $(val 1 holdlog) == *" edits=0 "* ]] &&
	drv 1 "await set 1 copies, 1 placements FRONT_BACK COUNTERCLOCKWISE_90: -45,-60,33..-44,-59,35" > /dev/null'
echo "  the set after the first click: $(val 1 copyset)" >> /out/checks.log
check "and its controls line offers to paste the set, with its count" eval \
	'[[ $(val 1 modehud) == "$(set_line Line "[Right-click: paste 1 copy]")" ]]'
# Looking up past the first copy, the view crosses the upright line through its middle at -53.5: Line's block is at
# -54, in the fourth copy, each copy 2 blocks (its height) above the one before.
up4="-45,-60,33..-44,-59,35 ; -45,-58,33..-44,-57,35 ; -45,-56,33..-44,-55,35 ; -45,-54,33..-44,-53,35"
drv 1 "lookat -44.5 -53.5 34.5" "wait 5" > /dev/null
check "looking up, the Line goes up: 4 copies, each the copy's height above the one before" \
	drv 1 "await set 4 copies, 4 placements FRONT_BACK COUNTERCLOCKWISE_90: $up4"
echo "  the Line looking up: $(val 1 copyset)" >> /out/checks.log
check "the first copy stays where the first click fixed it" copy_is 1 "^at -45,-60,33\.\.-44,-59,35 "
check "Litematica draws each copy, the fourth's stairs at its north west corner" eval \
	'[[ $(val 1 "get -45 -53 33") == minecraft:oak_stairs ]] && [[ $(val 1 "get -45 -59 33") == minecraft:oak_stairs ]]'
check "Litematica draws no box for the set's copies, and Building Planner draws them blue" eval \
	'[[ $(val 1 boxes) == *"others: none" ]] && [[ $(val 1 copycolour) == BLUE ]]'
check "the controls line counts the set's 4 copies" eval \
	'[[ $(val 1 modehud) == "$(set_line Line "[Right-click: paste 4 copies]")" ]]'
check "the name Paste shows left of the hotbar, in a Line of copies" mode_name_at Paste 232
shots copy-line-up
line_shot line-line-set
# Looking farther up, Line's block is at -40, 11 copies up; the set stops at 10, the tenth 18 blocks above the first.
drv 1 "lookat -44.5 -40 34.5" "wait 5" > /dev/null
check "looking farther up, the set stops at 10 copies" \
	drv 1 "await set 10 copies, 10 placements FRONT_BACK COUNTERCLOCKWISE_90: -45,-60,33..-44,-59,35 ; "
check "the tenth 18 blocks above the first" eval '[[ $(val 1 copyset) == *" ; -45,-42,33..-44,-41,35" ]] &&
	[[ $(val 1 modehud) == "$(set_line Line "[Right-click: paste 10 copies]")" ]]'
drv 1 "lookat -44.5 -53.5 34.5" "wait 5" > /dev/null
check "looking back, the set shrinks to 4 copies" \
	drv 1 "await set 4 copies, 4 placements FRONT_BACK COUNTERCLOCKWISE_90: $up4"
# A notch up turns every copy a quarter turn clockwise: the first in place, around its middle -45 -60 34, now 3 blocks
# east to west and 2 north to south; the others are laid again above it, still 2 blocks apart.
xdo 1 click 4
check "a notch of the wheel up turns every copy in the set, laid again at the turned size" \
	drv 1 "await set 4 copies, 4 placements FRONT_BACK NONE: -46,-60,34..-44,-59,35 ; -46,-58,34..-44,-57,35 ; -46,-56,34..-44,-55,35 ; -46,-54,34..-44,-53,35"
xdo 1 click 5
check "and a notch down turns them back" drv 1 "await set 4 copies, 4 placements FRONT_BACK COUNTERCLOCKWISE_90: $up4"
# H locks the first copy, and a nudge forward moves it a block north, the set with it; the far end still follows the
# aim, which crosses the line through the moved middle, -45 -60 33, at -53.5 again.
press 1 h
check "H locks the set's first copy, its line naming the nudge keys" eval '[[ $(val 1 copylock) == locked ]] &&
	[[ $(val 1 modehud) == "$(set_line Line "[Right-click: paste 4 copies]" locked)" ]]'
press 1 Up
check "a nudge moves the first copy and the set with it" \
	drv 1 "await set 4 copies, 4 placements FRONT_BACK COUNTERCLOCKWISE_90: -45,-60,32..-44,-59,34 ; -45,-58,32..-44,-57,34 ; -45,-56,32..-44,-55,34 ; -45,-54,32..-44,-53,34"
press 1 Down
check "and the nudge back returns them" drv 1 "await set 4 copies, 4 placements FRONT_BACK COUNTERCLOCKWISE_90: $up4"
press 1 h
check "H unlocks it again" eval '[[ $(val 1 copylock) == unlocked ]] &&
	[[ $(val 1 modehud) == "$(set_line Line "[Right-click: paste 4 copies]")" ]]'
# A left click after the first click drops the set and leaves the copy at the aim, following it again.
attack_click 1
check "a left click after the first click drops the set" eval \
	'drv 1 "wait 10" > /dev/null && [[ $(val 1 copyset) == none ]] && [[ $(val 1 holdlog) == *" edits=0 "* ]]'
check "and leaves the copy at the aim, following it again, still in Line" eval \
	'copy_is 1 "^at " && [[ $(val 1 copymode) == LINE ]] && [[ $(val 1 modehud) == "$(start_line Line)" ]]'
# The Line of 4 up pastes as one edit, which the other player sees, and one Ctrl+Z takes back.
first_click
drv 1 "lookat -44.5 -53.5 34.5" "wait 5" "await set 4 copies, 4 placements FRONT_BACK COUNTERCLOCKWISE_90: $up4" \
	> /dev/null
use_click 1
check "the second click pastes the Line of 4 up as one edit" eval '[[ $(val 1 holdlog) == *" edits=1 "* ]]'
check "which the other player sees, every copy's blocks" drv 2 "await block -45 -59 33 minecraft:oak_stairs" \
	"await block -45 -57 33 minecraft:oak_stairs" "await block -45 -55 33 minecraft:oak_stairs" \
	"await block -45 -53 33 minecraft:oak_stairs" "await block -44 -54 35 minecraft:stone_bricks"
check "and the copy is at the aim again with no set, for a new set, still in Line" eval \
	'[[ $(val 1 copyset) == none ]] && copy_is 1 "^at " && [[ $(val 1 copymode) == LINE ]]'
check "one Ctrl+Z takes back the whole Line" undo_paste "-45 -59 33" "-45 -57 33" "-45 -55 33" "-45 -53 33" \
	"-44 -54 35"
# A Line east: aimed at -38.5 -59.5 34.5, the view crosses the east-west line through the middle at -38.1, in the
# fourth copy, each copy 2 blocks (its length east to west) from the one before.
east4="-45,-60,33..-44,-59,35 ; -43,-60,33..-42,-59,35 ; -41,-60,33..-40,-59,35 ; -39,-60,33..-38,-59,35"
first_click
drv 1 "lookat -38.5 -59.5 34.5" "wait 5" > /dev/null
check "aimed east, the Line goes east: 4 copies, each the copy's length from the one before" \
	drv 1 "await set 4 copies, 4 placements FRONT_BACK COUNTERCLOCKWISE_90: $east4"
echo "  the Line aimed east: $(val 1 copyset)" >> /out/checks.log
shots copy-line-east
use_click 1
check "the second click pastes the Line of 4 east as one edit" eval '[[ $(val 1 holdlog) == *" edits=1 "* ]]'
check "which the other player sees" drv 2 "await block -45 -59 33 minecraft:oak_stairs" \
	"await block -43 -59 33 minecraft:oak_stairs" "await block -41 -59 33 minecraft:oak_stairs" \
	"await block -39 -59 33 minecraft:oak_stairs" "await block -38 -60 35 minecraft:stone_bricks"
check "one Ctrl+Z takes it back" undo_paste "-45 -59 33" "-43 -59 33" "-41 -59 33" "-39 -59 33" "-38 -60 35"
# R during a set switches the mode and keeps the first copy: the Line east becomes a Wall, which from the same aim is
# the same 4 copies, in the upright plane through the middle north to south. Aimed at -40.5 -57.5 34.5, the view crosses
# that plane at -40.3 -57.8: 3 copies east and 2 up.
first_click
drv 1 "lookat -38.5 -59.5 34.5" "wait 5" "await set 4 copies, 4 placements FRONT_BACK COUNTERCLOCKWISE_90: $east4" \
	> /dev/null
press 1 r
check "R during a Line switches to Wall and keeps the first copy, the same 4 copies from this aim" eval \
	'[[ $(val 1 copymode) == WALL ]] && copy_is 1 "^at -45,-60,33\.\.-44,-59,35 " &&
	drv 1 "await set 4 copies, 4 placements FRONT_BACK COUNTERCLOCKWISE_90: $east4" > /dev/null &&
	[[ $(val 1 modehud) == "$(set_line Wall "[Right-click: paste 4 copies]")" ]]'
wall6="-45,-60,33..-44,-59,35 ; -43,-60,33..-42,-59,35 ; -41,-60,33..-40,-59,35 ; -45,-58,33..-44,-57,35 ; -43,-58,33..-42,-57,35 ; -41,-58,33..-40,-57,35"
drv 1 "lookat -40.5 -57.5 34.5" "wait 5" > /dev/null
check "aimed up and east, the Wall is 3 copies by 2, row by row from the first" \
	drv 1 "await set 6 copies, 6 placements FRONT_BACK COUNTERCLOCKWISE_90: $wall6"
echo "  the Wall: $(val 1 copyset)" >> /out/checks.log
check "with its controls line" eval '[[ $(val 1 modehud) == "$(set_line Wall "[Right-click: paste 6 copies]")" ]]'
shots copy-wall
line_shot line-wall-set
use_click 1
check "the second click pastes the Wall of 3 by 2 as one edit" eval '[[ $(val 1 holdlog) == *" edits=1 "* ]]'
check "which the other player sees" drv 2 "await block -45 -59 33 minecraft:oak_stairs" \
	"await block -41 -59 33 minecraft:oak_stairs" "await block -45 -57 33 minecraft:oak_stairs" \
	"await block -41 -57 33 minecraft:oak_stairs" "await block -40 -58 35 minecraft:stone_bricks"
check "still in Wall" eval '[[ $(val 1 copymode) == WALL ]] && [[ $(val 1 copyset) == none ]]'
check "one Ctrl+Z takes back the whole Wall" undo_paste "-45 -59 33" "-41 -59 33" "-45 -57 33" "-41 -57 33" \
	"-40 -58 35"
# A Floor, on the first copy's level: aimed at the copy's own top, Floor's block is the first copy's middle, so the
# set is the first copy alone; aimed at the grass's top at -38.5 -60 25.5, Floor's block is -39 -60 25, in the fourth
# copy east and the fourth north, 2 blocks east to west and 3 north to south apart.
first_click
echo "  the Wall after the first click: $(val 1 copyset)" >> /out/checks.log
press 1 r
check "R during a Wall switches to Floor and keeps the first copy" eval '[[ $(val 1 copymode) == FLOOR ]] &&
	drv 1 "await set 1 copies, 1 placements FRONT_BACK COUNTERCLOCKWISE_90: -45,-60,33..-44,-59,35" > /dev/null'
floor16=$(for z in 33 30 27 24; do for x in -45 -43 -41 -39; do printf '%s' "$x,-60,$z..$((x + 1)),-59,$((z + 2)) ; "; done; done)
floor16=${floor16% ; }
drv 1 "lookat -38.5 -60 25.5" "wait 5" > /dev/null
check "aimed north east on the grass, the Floor is 4 copies by 4, row by row from the first" \
	drv 1 "await set 16 copies, 16 placements FRONT_BACK COUNTERCLOCKWISE_90: $floor16"
echo "  the Floor: $(val 1 copyset)" >> /out/checks.log
check "with its controls line" eval '[[ $(val 1 modehud) == "$(set_line Floor "[Right-click: paste 16 copies]")" ]]'
shots copy-floor
line_shot line-floor-set
use_click 1
check "the second click pastes the Floor of 4 by 4 as one edit" eval '[[ $(val 1 holdlog) == *" edits=1 "* ]]'
check "which the other player sees" drv 2 "await block -45 -59 33 minecraft:oak_stairs" \
	"await block -39 -59 33 minecraft:oak_stairs" "await block -45 -59 24 minecraft:oak_stairs" \
	"await block -39 -59 24 minecraft:oak_stairs" "await block -38 -60 26 minecraft:stone_bricks"
# Each copy of the set is a paste of its own. Aimed at the east face of the stone bricks at -38 -60 29, in the east
# copy of the third row (z 27..29), the copy sits flush against that copy's east face, -37..-36, its edges in line with
# the copy's at the corner nearest the aim, 27..29 north to south; against the whole Floor's box (24..35) it would
# stand at 24..26.
drv 1 "cmd tp @s -32.5 -58 29.5 90 0" "wait 40" "lookat -37.0 -59.5 29.5" "wait 5" > /dev/null
check "aimed at a copy of the Floor, the copy sits flush beside that copy, not beside the Floor's edge" \
	drv 1 "await copy at -37,-60,27..-36,-59,29 FRONT_BACK COUNTERCLOCKWISE_90"
drv 1 "cmd tp @s -44.5 -55 42.5 180 30" "wait 40" > /dev/null
check "one Ctrl+Z takes back the whole Floor" undo_paste "-45 -59 33" "-39 -59 33" "-45 -59 24" "-39 -59 24" \
	"-38 -60 26"
# R at Off drops the set and leaves the copy following the aim.
first_click
drv 1 "lookat -38.5 -60 25.5" "wait 5" "await set 16 copies" > /dev/null
press 1 r
check "R during a Floor switches to Off, which drops the set" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 copymode) == OFF ]] && [[ $(val 1 copyset) == none ]] && [[ $(val 1 holdlog) == *" edits=0 "* ]]'
check "and leaves the copy following the aim" eval 'drv 1 "await copy at -39,-60,24..-38,-59,26" > /dev/null &&
	[[ $(val 1 modehud) == "$controls_ctrl" ]]'

# Esc steps out of one mode a press at a time ("Esc"): during a copy's Line, with Single chosen in Effortless Building,
# it drops the set, then puts the copy away, then returns the build mode to Disable, then pauses. With the inventory
# open, Esc closes it and takes no step.
press 1 r
first_click
drv 1 "lookat -44.5 -53.5 34.5" "wait 5" "await set 4 copies, 4 placements FRONT_BACK COUNTERCLOCKWISE_90: $up4" \
	> /dev/null
keys 1 key e
check "with the inventory open" eval 'drv 1 "wait 10" > /dev/null && [[ $(val 1 screenclass) == *Inventory* ]]'
keys 1 key Escape
check "Esc closes it, and the set stays" eval 'drv 1 "await noscreen" "wait 5" > /dev/null &&
	drv 1 "await set 4 copies, 4 placements" > /dev/null && [[ $(val 1 copymode) == LINE ]]'
press 1 Escape
check "Esc during a copy's Line drops the set, sending no edit, as the line's [Esc/Left-click: cancel] says" eval \
	'[[ $(val 1 copyset) == none ]] && [[ $(val 1 holdlog) == *" edits=0 "* ]] && [[ $(val 1 screenclass) == none ]]'
check "and leaves the copy at the aim, following it again, still in Line" eval \
	'copy_is 1 "^at " && [[ $(val 1 copymode) == LINE ]] && [[ $(val 1 modehud) == "$(start_line Line)" ]]'
check "with the name Paste showing left of the hotbar, in Single's place" mode_name_at Paste 232
press 1 Escape
check "Esc again puts the copy away, as the line's [Esc/Left-click: close] says, and Single's line shows" eval \
	'copy_is 1 "^away " && [[ $(val 1 modehud) == "$single_line" ]] && [[ $(val 1 screenclass) == none ]]'
check "and the name Single shows left of the hotbar again" mode_name_at Single 232
press 1 Escape
check "Esc in Single, as its line's [Esc: exit mode] says, returns the build mode to Disable" eval \
	'[[ $(val 1 ebmode) == DISABLED ]] && [[ $(val 1 screenclass) == none ]]'
press 1 Escape
check "with no step left, Esc pauses the game" drv 1 "await screenclass net.minecraft.client.gui.screens.PauseScreen"
# Back to Single, with the copy at the aim in Off, following the aim.
drv 1 "close" "ebmode SINGLE" "wait 5" > /dev/null
paste_button
press 1 r
press 1 r
press 1 r
check "the copy is back at the aim, in Off" eval '[[ $(val 1 copymode) == OFF ]] && copy_is 1 "^at " &&
	[[ $(val 1 modehud) == "$controls_ctrl" ]]'

# The controls line over light ghost blocks: a floor of ghost white wool, birch planks and glowstone in stripes 2
# blocks wide, from -79 -60 30 to -62 -60 60, under Planner1, who looks down at it with the copy on it, so the line
# crosses each of them. Shot at 1920 by 1080, at GUI scale 2 and 4; then the floor is erased.
drv 1 "cmd tp @s -69.5 -55 56.5 180 45" "wait 40" > /dev/null
light=(minecraft:white_wool minecraft:birch_planks minecraft:glowstone)
for i in 0 1 2 3 4 5 6 7 8; do
	drv 1 "edit $((-79 + 2 * i)) -60 30 $((-78 + 2 * i)) -60 60 ${light[i % 3]}" > /dev/null
done
check "Planner1 sees the light floor, with the copy on it" eval 'drv 1 "await block -79 -60 30 minecraft:white_wool" \
	"await block -62 -60 60 minecraft:glowstone" > /dev/null && drv 1 "await copy at -7" > /dev/null &&
	[[ $(val 1 modehud) == "$controls_ctrl" ]]'
line_shot line-over-light-ghost-blocks-1920x1080-gui2 1920 1080 2
line_shot line-over-light-ghost-blocks-1920x1080-gui4
drv 1 "edit -79 -60 30 -62 -60 60 minecraft:air" > /dev/null
check "and the floor is erased" drv 1 "await block -79 -60 30 minecraft:air" "await block -62 -60 60 minecraft:air"

# A left click puts the copy away, keeping it, and its controls line goes, Single's line showing in its place; the
# click erases nothing. The scroll wheel then changes the hotbar slot again.
attack_click 1
check "a left click puts the copy away" eval \
	'drv 1 "wait 10" > /dev/null && copy_is 1 "^away FRONT_BACK COUNTERCLOCKWISE_90 unselected 8 blocks$"'
check "and its controls line goes, Single's line showing in its place" eval \
	'[[ $(val 1 modehud) == "$single_line" ]]'
check "the click erases nothing" eval '[[ $(val 1 holdlog) == *" edits=0 "* ]] &&
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
check "with its controls line" eval '[[ $(val 1 modehud) == "$controls_ctrl" ]]'

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
# The copy's mode, set to Line here, stays for the next copy.
press 1 r
check "with the copy at the aim, R sets Line" eval '[[ $(val 1 copymode) == LINE ]]'
radial_open
radial_click "Copy"
check "the radial menu's Copy button puts the copy away and starts a box" eval \
	'[[ $(val 1 drawbox) == "COPY 0 "* ]] && copy_is 1 "^away "'
radial_let_go
house_box copy-real-box
check "a box around only real blocks copies them: the house's 26 blocks" client_said 1 "Copied 26 blocks."
check "the new copy keeps the mode, Line" eval '[[ $(val 1 copymode) == LINE ]]'
use_click 1
check "and its first click fixes a set's first copy, sending no edit" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 holdlog) == *" edits=0 "* ]] && [[ $(val 1 copyset) =~ ^[0-9]+\ copies,\ [0-9]+\ placements\ NONE\ NONE: ]]'
attack_click 1
check "a left click drops the set" eval 'drv 1 "wait 10" > /dev/null && [[ $(val 1 copyset) == none ]] && copy_is 1 "^at "'
press 1 r
press 1 r
press 1 r
check "R three times sets Off again" eval '[[ $(val 1 copymode) == OFF ]]'

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
attack_click 1
check "a left click puts it away" eval 'drv 1 "wait 5" > /dev/null && copy_is 1 "^away "'
radial_open
radial_point "Paste"
radial_let_go
aim 1 50 -60 20
check "letting the menu's key go over its Paste button brings it back" \
	drv 1 "await copy at 49,-60,19..51,-58,21 NONE NONE unselected 27 blocks"
use_click 1
check "and a right click pastes them as ghost blocks" drv 2 \
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
check "the Delete button ends the box and starts Delete, and Delete shows selected" eval \
	'[[ $(val 1 mousemode) == DELETE ]] && [[ $(val 1 drawbox) == none* ]] && [[ $(val 1 selectedbuttons) == Delete ]]'
check "and no mode segment does" eval '[[ $(val 1 "ringselected radial-delete-selected") == none ]]'
radial_click "Delete"
selected_shot radial-line-selected
check "with Delete ended, no button shows selected, and Line's segment does" eval '[[ $(val 1 drawbox) == none* ]] &&
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
# Erasing Big is another edit of 200,000 blocks, and leaves the layer as the later checks expect it.
drv 1 "edit 60 -60 2 139 -11 51 minecraft:air" "edit 139 -10 51 139 -10 51 minecraft:air" > /dev/null
check "erasing all of Big reaches both games" eval 'drv 2 "await block 60 -60 2 minecraft:air" \
	"await block 139 -11 51 minecraft:air" "await block 139 -10 51 minecraft:air" &&
	drv 1 "await block 139 -11 51 minecraft:air"'

# A set over the block cap draws red, and its click pastes nothing. Ghost stone from 60 -60 2 to 123 -36 65, 64 by 25
# by 64 blocks, is 102,400 blocks: one copy is under the cap and a set of two is over it. A box from the space above
# its top corner copies it, and the copy rests on its top, from 60 -35 2 to 123 -11 65, and draws blue; its middle
# block is 91 -23 33. In Line, looking down past the copy's side, the view crosses the upright line through the middle
# at about -42: the set goes down 2 copies, 204,800 blocks.
drv 1 "edit 60 -60 2 123 -36 65 minecraft:stone" > /dev/null
check "client 1 sees the 102,400 ghost stone" drv 1 "await block 60 -60 2 minecraft:stone" \
	"await block 123 -36 65 minecraft:stone"
copy_button
box 1 123.5 -35 65.5 -35 60 2 -60
check "a box around it copies its 102,400 blocks" client_said 1 "Copied 102400 blocks."
drv 1 "lookat 91.5 -35 33.5" "wait 5" > /dev/null
check "the copy rests on the stone's top, under the block cap, blue" eval \
	'drv 1 "await copy at 60,-35,2..123,-11,65 NONE NONE unselected 102400 blocks" > /dev/null &&
	drv 1 "await copycolour BLUE" > /dev/null'
press 1 r
use_click 1
check "in Line, the first click fixes the set's first copy, still blue" eval '[[ $(val 1 copymode) == LINE ]] &&
	drv 1 "await set 1 copies, 1 placements NONE NONE: 60,-35,2..123,-11,65" > /dev/null &&
	[[ $(val 1 copycolour) == BLUE ]]'
drv 1 "lookat 91.5 -45 33.5" "wait 5" > /dev/null
check "a set of two copies, 204,800 blocks, is over the block cap and draws red" eval \
	'drv 1 "await set 2 copies, 2 placements NONE NONE: 60,-35,2..123,-11,65 ; 60,-60,2..123,-36,65" > /dev/null &&
	drv 1 "await copycolour RED TOO_MANY_BLOCKS" > /dev/null'
echo "  the Line over the cap: $(val 1 copyset)" >> /out/checks.log
check "its controls line says why in place of the paste" eval \
	'[[ $(val 1 modehud) == "$(set_line Line "[Can'"'"'t paste: too many blocks]")" ]]'
check "Litematica draws no box for the copy or its set" eval '[[ $(val 1 boxes) == *"others: none" ]]'
drv 1 "wait 200" > /dev/null
shots copy-red-set-over-cap
line_shot line-line-red-set
lines=$(chat_lines 1)
use_click 1
check "its click pastes nothing and sends no edit" eval 'drv 1 "wait 20" > /dev/null &&
	[[ $(val 1 holdlog) == *" edits=0 "* ]] && [[ $(val 2 "get 60 -35 2") == minecraft:air ]] &&
	(($(chat_lines 1) == lines))'
check "and leaves the set fixed" drv 1 "await set 2 copies"
drv 1 "lookat 91.5 -20 33.5" "wait 5" > /dev/null
check "aimed back into the first copy, the set is one copy again, and blue" eval \
	'drv 1 "await set 1 copies" > /dev/null && drv 1 "await copycolour BLUE" > /dev/null'
attack_click 1
press 1 r
press 1 r
press 1 r
check "a left click drops the set, and R three times sets Off again" eval '[[ $(val 1 copyset) == none ]] &&
	[[ $(val 1 copymode) == OFF ]]'
attack_click 1
drv 1 "edit 60 -60 2 123 -36 65 minecraft:air" > /dev/null
check "a left click puts the copy away, and the stone is erased" eval 'drv 1 "wait 10" > /dev/null &&
	copy_is 1 "^away " && drv 1 "await block 60 -60 2 minecraft:air" "await block 123 -36 65 minecraft:air" > /dev/null'

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
	'box_is "MATERIAL_LIST 1 3,-59,3..5,-59,4 blue SINGLE" &&
	[[ $(val 1 modehud) == "$second_line[3 × 1 × 2]" ]]'
# At 854 by 480, GUI scale 2, the box's size is a rectangle of its own at the line's end.
wrap=$(val 1 modewrap)
echo "  the material list's controls as they wrap: $wrap" >> /out/checks.log
check "the box's size is the line's last rectangle, and the rectangles are laid out in rows" eval \
	'[[ $wrap == *"[3 × 1 × 2]" ]] && rects_ok 854 2'
line_shot line-material-list-second
drv 1 "wait 200" > /dev/null
shot 1 box-material-list-first-click
drv 1 "window 1920 1080 2" "wait 20" > /dev/null
shot 1 box-material-list-first-click-1920x1080
drv 1 "window 854 480 2" "wait 20" > /dev/null
attack_click 1
drv 1 "lookat 3.5 -59.5 3.5" "wait 5" > /dev/null
check "its second click sets the opposite corner" eval 'box_is "MATERIAL_LIST 2 3,-60,3..5,-59,4 blue SINGLE" &&
	[[ $(val 1 modehud) == "$height_line[3 × 2 × 2]" ]]'
shot 1 box-material-list-second-click
lines=$(chat_lines 1)
use_click 1
check "a right click before the third click ends the box, with no message" eval \
	'drv 1 "wait 10" > /dev/null && box_is "none SINGLE" && (($(chat_lines 1) == lines))'

# Copy and Material list each switch a box from the other, keeping its first corner; the Delete button during a box
# ends it and starts Delete. switch_to <button>: a click on that button of the radial menu, then the aim on the
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
switch_to "Material list"
check "Material list switches a box for Copy, keeping its corner" \
	box_is "MATERIAL_LIST 1 3,-59,3..5,-59,4 blue SINGLE"
switch_to Copy
check "and Copy switches it back" box_is "COPY 1 3,-59,3..5,-59,4 white SINGLE"
switch_to Delete
check "the Delete button during a box ends it and starts Delete" eval '[[ $(val 1 mousemode) == DELETE ]] &&
	box_is "none SINGLE"'
use_click 1
check "a right click with no marks ends Delete, leaving the player's mode" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 mousemode) == none ]] && box_is "none SINGLE"'
check "and the name Single shows left of the hotbar again" mode_name_at Single 232

drv 1 "cmd tp Planner2 101.5 0 26.5 0 30" "wait 40" > /dev/null

# Delete, marking by sweeping. A ghost wall of 10 by 5 stone bricks at x 100 to 109, y -55 to -51, z 40, a ghost oak
# door beside it at x 111, and a real stone right in front of the middle row's fourth block, at 103 -53 39. Planner1
# faces the wall from 8 blocks away, straight in front of that fourth block, with their eyes at the middle row's height.
drv 1 "cmd tp @s 103.5 -54.12 32 0 0" "wait 40" > /dev/null
drv 1 "edit 100 -55 40 109 -51 40 minecraft:stone_bricks" \
	"edit 111 -55 40 111 -55 40 minecraft:oak_door[half=lower,facing=north]" \
	"edit 111 -54 40 111 -54 40 minecraft:oak_door[half=upper,facing=north]" \
	"cmd setblock 103 -53 39 minecraft:stone" > /dev/null
check "both clients show the ghost wall and its door" eval 'drv 2 "await block 109 -51 40 minecraft:stone_bricks" \
	"await block 111 -54 40 minecraft:oak_door" > /dev/null && drv 1 "await block 109 -51 40 minecraft:stone_bricks" \
	"await block 111 -54 40 minecraft:oak_door" "await real 103 -53 39 minecraft:stone" > /dev/null'

# The Delete button starts Delete, whose line with no marks names marking, marking a box and the right click that
# closes it; R does nothing in it.
delete_button
check "the Delete button starts Delete" eval '[[ $(val 1 mousemode) == DELETE ]] && box_is "none SINGLE"'
check "whose line with no marks names marking, marking a box and closing" eval '[[ $(val 1 modehud) == "$delete_none" ]]'
check "and the name Delete shows left of the hotbar, in Single's place" mode_name_at Delete 232
line_shot line-delete
press 1 r
check "R does nothing in Delete" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 mousemode) == DELETE ]] &&
	[[ $(val 1 modehud) == "$delete_none" ]]'

# hold_ctrl and let_go_ctrl: Left Ctrl, Minecraft's sprint key, held down and let go. sweep <x1 y1 z1> <x2 y2 z2>: the
# view turns from looking at the first point to the second in 3 frames, as a quick mouse sweep would, and stays there
# 5 ticks.
hold_ctrl() {
	xdo 1 keydown Control_L
	drv 1 "await sprint true" "wait 5" > /dev/null
}
let_go_ctrl() {
	xdo 1 keyup Control_L
	drv 1 "await sprint false" "wait 5" > /dev/null
}
sweep() {
	drv 1 "turn $1 $2 3" "await turned" "wait 5" > /dev/null
}
# Holding Left Ctrl while the view turns across the middle row from end to end in 3 frames, the aim 4 and a half blocks
# from one frame's to the next, marks the 9 ghost blocks the aim meets, with no gap, and neither the real stone nor the
# ghost block behind it.
row="100,-53,40 101,-53,40 102,-53,40 104,-53,40 105,-53,40 106,-53,40 107,-53,40 108,-53,40 109,-53,40"
drv 1 "lookat 100.5 -52.5 40" "wait 5" > /dev/null
hold_ctrl
sweep "100.5 -52.5 40" "109.5 -52.5 40"
let_go_ctrl
check "a quick sweep with Left Ctrl held marks the middle row's 9 ghost blocks the aim meets, with no gap" eval \
	'[[ $(val 1 marks) == "9 marks, 9 to erase: $row" ]]'
check "and leaves the real stone in front of the fourth" eval '[[ $(val 1 "real 103 -53 39") == minecraft:stone ]]'
check "the list under the crosshair shows the 9 stone bricks" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 boxlist) == *" | minecraft:stone_bricks 9" ]]'
# A second hold that starts on the third mark and sweeps west unmarks the first three.
drv 1 "lookat 102.5 -52.5 40" "wait 5" > /dev/null
hold_ctrl
sweep "102.5 -52.5 40" "100.5 -52.5 40"
let_go_ctrl
row="104,-53,40 105,-53,40 106,-53,40 107,-53,40 108,-53,40 109,-53,40"
check "a hold that starts on a mark unmarks what it sweeps" eval '[[ $(val 1 marks) == "6 marks, 6 to erase: $row" ]]'
# A ghost door marked by its lower half is one mark, and erasing it would take its upper half too.
drv 1 "lookat 111.5 -54.5 40.5" "wait 5" > /dev/null
hold_ctrl
let_go_ctrl
check "a ghost door marked by its lower half has one mark, and two positions to erase" eval \
	'[[ $(val 1 marks) == "7 marks, 8 to erase: $row 111,-55,40" ]]'
# Planner2 erasing a marked ghost block drops its mark.
drv 2 "edit 109 -53 40 109 -53 40 minecraft:air" > /dev/null
check "Planner2 erasing a marked ghost block drops its mark" eval 'drv 1 "await block 109 -53 40 minecraft:air" \
	"wait 5" > /dev/null && [[ $(val 1 marks) == "6 marks, 7 to erase: ${row% *} 111,-55,40" ]]'
drv 1 "edit 109 -53 40 109 -53 40 minecraft:stone_bricks" "await block 109 -53 40 minecraft:stone_bricks" > /dev/null
# A right click clears the marks; a second, with none, ends Delete.
use_click 1
check "a right click clears the marks, leaving Delete on" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 marks) == "0 marks, 0 to erase" ]] && [[ $(val 1 mousemode) == DELETE ]]'
use_click 1
check "a second ends Delete" eval 'drv 1 "wait 10" > /dev/null && [[ $(val 1 mousemode) == none ]]'
# Esc does what Delete's lines say ("Esc"): with marks, it clears them, as [Esc/Right-click: clear] says, and with none
# it ends Delete, as [Esc/Right-click: close] says, pausing nothing.
delete_button
drv 1 "lookat 100.5 -52.5 40" "wait 5" > /dev/null
hold_ctrl
sweep "100.5 -52.5 40" "109.5 -52.5 40"
let_go_ctrl
check "in Delete with marks" eval '[[ $(val 1 mousemode) == DELETE ]] && [[ $(val 1 marks) != "0 marks"* ]]'
press 1 Escape
check "Esc clears the marks, leaving Delete on" eval '[[ $(val 1 marks) == "0 marks, 0 to erase" ]] &&
	[[ $(val 1 mousemode) == DELETE ]] && [[ $(val 1 screenclass) == none ]]'
press 1 Escape
check "and Esc again ends Delete" eval '[[ $(val 1 mousemode) == none ]] && [[ $(val 1 screenclass) == none ]]'

# Delete's lines with marks, and during a hold that unmarks. The middle row's 9 ghost blocks the aim meets and the
# ghost door, marked by its lower half: 10 marks, and 11 positions to erase. Each line is shot at 1920 by 1080, GUI
# scale 4.
delete_button
row="100,-53,40 101,-53,40 102,-53,40 104,-53,40 105,-53,40 106,-53,40 107,-53,40 108,-53,40 109,-53,40"
drv 1 "lookat 100.5 -52.5 40" "wait 5" > /dev/null
hold_ctrl
sweep "100.5 -52.5 40" "109.5 -52.5 40"
let_go_ctrl
drv 1 "lookat 111.5 -54.5 40.5" "wait 5" > /dev/null
hold_ctrl
let_go_ctrl
marked="10 marks, 11 to erase: $row 111,-55,40"
check "the middle row's 9 ghost blocks and the ghost door are marked" eval \
	'[[ $(val 1 mousemode) == DELETE ]] && [[ $(val 1 marks) == "$marked" ]]'
check "Delete's line with marks names the hold that erases them and the right click that clears them" eval \
	'[[ $(val 1 modehud) == "$delete_marked" ]]'
line_shot line-delete-marked
line_shot line-delete-marked-854x480-gui2 854 480 2
# A hold that starts on a mark says "unmark" while Left Ctrl stays down; a hold that starts off the marks marks it
# again.
drv 1 "lookat 104.5 -52.5 40" "wait 5" > /dev/null
hold_ctrl
check "during a hold that unmarks, the line says unmark" eval '[[ $(val 1 modehud) == "$delete_unmarking" ]]'
line_shot line-delete-unmarking
let_go_ctrl
hold_ctrl
let_go_ctrl
check "and a hold that starts on the unmarked block marks it again" eval '[[ $(val 1 marks) == "$marked" ]]'

# A left click held 10 ticks erases nothing. While it's held, a bar fills above the controls: the hotbar's width, 182,
# and 5 high, centred, its bottom 4 above the controls' top, filled a twentieth for each tick, so a quarter at the 5th
# tick and half at the 10th. At 1920 by 1080, GUI scale 4, the screen is 480 wide: the bar runs from 149 to 331, filled
# to 194 and to 240, 4 window pixels to each. The half-full bar is shot, and the driver lets go of the button in the game
# at the next tick.
drv 1 "wait 210" "window 1920 1080 4" "wait 20" "notoasts" "wait 5" "holdreset" > /dev/null
check "with the button up, no bar shows" eval '[[ $(val 1 erasebar) == hidden ]]'
xdo 1 mousedown 1
held=$(drv 1 "await erasing 5" "erasebar" "await erasing 10" "erasebar" "modehud" "modebox" "shot delete-erase-bar-half" \
	"release attack" "wait 20") || true
xdo 1 mouseup 1
# result <command> [<nth>]: the driver's reply to the nth such command (the first by default) in $held.
result() {
	awk -v c="$1" -v n="${2:-1}" 'index($0, c) == 1 { if (++seen == n) { sub(/^[^=]* => /, ""); print; exit } }' \
		<<< "$held"
}
echo "  delete-erase-bar-half: bar $(result erasebar 2) | box $(result modebox)" >> /out/checks.log
read -r _ box_top _ _ <<< "$(result modebox)"
check "at the hold's 5th tick the bar is a quarter full" eval '[[ $(result erasebar 1) == "596 "*" 1324 "*" fill 776" ]]'
check "at its 10th, half full, 5 high and 4 above the controls" eval \
	'[[ $(result erasebar 2) == "596 $((box_top - 36)) 1324 $((box_top - 16)) fill 960" ]]'
check "and the line stays as it was" eval '[[ $(result modehud) == "$delete_marked" ]]'
check "let go then, it erases nothing, and the bar goes" eval '[[ $(val 1 holdlog) == *" edits=0 "* ]] &&
	[[ $(val 1 marks) == "$marked" ]] && [[ $(val 1 erasing) == -1 ]] && [[ $(val 1 erasebar) == hidden ]]'
# Held 20 ticks it sends one edit, which takes the door's upper half too, which Planner2 sees glow with Planner1's
# name, and says how many it erased; the message is shot. Holding on erases nothing more.
erased_before=$(chat_count 1 "Erased 11 ghost blocks.")
drv 1 "holdreset" > /dev/null
xdo 1 mousedown 1
check "a left click held 20 ticks erases the marks, saying how many" eval 'drv 1 "await erasing 10" > /dev/null &&
	client_said 1 "Erased 11 ghost blocks."'
check "which Planner2 sees glow with Planner1's name, the door's upper half with it" eval \
	'drv 2 "await glow Planner1:11" "await block 100 -53 40 minecraft:air" "await block 111 -55 40 minecraft:air" \
	"await block 111 -54 40 minecraft:air" > /dev/null'
drv 1 "shot delete-erased-message" "wait 20" > /dev/null
echo "  delete-erased-message: $(val 1 modewrap) | box $(val 1 modebox)" >> /out/checks.log
check "as one edit" eval '[[ $(val 1 holdlog) == *" edits=1 "* ]]'
check "Delete stays on, with no marks" eval '[[ $(val 1 mousemode) == DELETE ]] &&
	[[ $(val 1 marks) == "0 marks, 0 to erase" ]] && [[ $(val 1 modehud) == "$delete_none" ]]'
drv 1 "wait 30" > /dev/null
check "holding on erases nothing more" eval '[[ $(val 1 holdlog) == *" edits=1 "* ]] &&
	said_once_more 1 "Erased 11 ghost blocks." "$erased_before"'
xdo 1 mouseup 1
drv 1 "window 854 480 2" "wait 20" > /dev/null
# One Ctrl+Z, aimed at the grass so that holding Ctrl marks nothing, brings the blocks back unmarked.
drv 1 "lookat 104.5 -61 35" "wait 5" "close" > /dev/null
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "one Ctrl+Z brings the erased blocks back" drv 1 "await block 100 -53 40 minecraft:stone_bricks" \
	"await block 109 -53 40 minecraft:stone_bricks" "await block 111 -55 40 minecraft:oak_door" \
	"await block 111 -54 40 minecraft:oak_door"
check "unmarked, with Delete still on" eval 'drv 1 "wait 10" > /dev/null && [[ $(val 1 marks) == "0 marks, 0 to erase" ]] &&
	[[ $(val 1 mousemode) == DELETE ]]'

# With the sprint key on Right Control, Delete's line names it; then it goes back.
drv 1 "bindkey key.sprint key.keyboard.right.control" "wait 2" > /dev/null
check "with the sprint key rebound, Delete's line names Right Control" eval \
	'[[ $(val 1 modehud) == "[Hold Right Control: toggle mark][Hold Right Control + Left-click: mark a box][Esc/Right-click: close]" ]]'
line_shot line-delete-rebound
drv 1 "bindkey key.sprint key.keyboard.left.control" "wait 2" > /dev/null
check "and with the key back, it names Left Control" eval '[[ $(val 1 modehud) == "$delete_none" ]]'

# The marked wall, shot at 854 by 480 and 1920 by 1080, GUI scale 2, and with GUI scale 4 for the list under the
# crosshair; first the same view unmarked, for where the marks show. One hold sweeps each row in turn, marking all but
# the block behind the real stone.
drv 1 "lookat 104.5 -52.5 40" "wait 5" > /dev/null
shots delete-unmarked
hold_ctrl
for y in -50.5 -51.5 -52.5 -53.5 -54.5; do
	sweep "100.5 $y 40" "109.5 $y 40"
done
let_go_ctrl
drv 1 "lookat 104.5 -52.5 40" "wait 10" > /dev/null
check "one hold over each row marks the wall's 49 ghost blocks the aim meets" eval \
	'[[ $(val 1 marks) == "49 marks, 49 to erase" ]] && [[ $(val 1 boxlist) == *" | minecraft:stone_bricks 49" ]]'
check "with Delete's line for marks" eval '[[ $(val 1 modehud) == "$delete_marked" ]]'
echo "  delete-marked-854x480 list: $(val 1 boxlist) | box $(val 1 modebox)" >> /out/checks.log
shots delete-marked
drv 1 "window 1920 1080 4" "wait 20" "notoasts" "wait 5" "shot delete-marked-gui4" "wait 20" > /dev/null
echo "  delete-marked-gui4 list: $(val 1 boxlist)" >> /out/checks.log
drv 1 "window 854 480 2" "wait 20" > /dev/null
use_click 1

# Delete, marking with a box. Ghost stone bricks of 10 by 4 by 10 at x 100 to 109, y -60 to -57, z 20 to 29, in front
# of the wall, seen from above their south side. With Left Ctrl held, the left button pressed on the front face of the
# bottom block at 100 -60 20 starts the box there; aimed at the top face of the block at 109 -57 29, the box is 10 by 4
# by 10, its line names the release and its size, and the list under the crosshair counts its 400 ghost blocks. It's
# shot at 854 by 480 and 1920 by 1080, GUI scale 2, and its line at GUI scale 4. While it's drawn, the crosshair passing
# over the wall's middle row, outside the box, marks none of it; aimed back, letting go marks the 400 ghost blocks in the
# box and nothing else.
drv 1 "cmd tp @s 104.5 -50 15 0 30" "wait 20" "edit 100 -60 20 109 -57 29 minecraft:stone_bricks" > /dev/null
check "both clients show the ghost bricks" eval 'drv 2 "await block 109 -57 29 minecraft:stone_bricks" > /dev/null &&
	drv 1 "await block 109 -57 29 minecraft:stone_bricks" "await block 100 -60 20 minecraft:stone_bricks" > /dev/null'
check "with no marks" eval '[[ $(val 1 marks) == "0 marks, 0 to erase" ]]'
drv 1 "lookat 100.5 -59.5 20" "wait 5" > /dev/null
hold_ctrl
xdo 1 mousedown 1
check "Left Ctrl and the left button start the box at the block aimed at" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 markbox) == "mark 100,-60,20..100,-60,20 red" ]] &&
	[[ $(val 1 modehud) == "[Release Left-click: mark the box][Esc: cancel][1 × 1 × 1]" ]]'
drv 1 "lookat 109.5 -56 29.5" "wait 10" > /dev/null
check "aimed at the far top block, the box is 10 by 4 by 10" eval \
	'[[ $(val 1 markbox) == "mark 100,-60,20..109,-57,29 red" ]] &&
	[[ $(val 1 modehud) == "[Release Left-click: mark the box][Esc: cancel][10 × 4 × 10]" ]]'
check "and the list under the crosshair counts its 400 ghost bricks" eval \
	'[[ $(val 1 boxlist) == *" | minecraft:stone_bricks 400" ]]'
shots delete-box
line_shot line-delete-box
before=$(val 1 marks)
sweep "100.5 -52.5 40" "109.5 -52.5 40"
check "the crosshair passing over the wall outside the box marks none of it" eval '[[ $(val 1 marks) == "$before" ]]'
drv 1 "lookat 109.5 -56 29.5" "wait 10" > /dev/null
xdo 1 mouseup 1
drv 1 "wait 5" > /dev/null
check "letting go marks the box's 400 ghost blocks and nothing else" eval \
	'[[ $(val 1 marks) == "400 marks, 400 to erase" ]] && [[ $(val 1 markbox) == none ]] &&
	[[ $(val 1 modehud) == "$delete_marked" ]]'
let_go_ctrl
drv 1 "lookat 104.5 -60 25" "wait 10" > /dev/null
check "the list under the crosshair counts the 400 marks" eval '[[ $(val 1 boxlist) == *" | minecraft:stone_bricks 400" ]]'
shots delete-box-marked
# A box started on a marked block unmarks: from the front bottom block to the top of the block at 104 -57 24, a box of
# 5 by 4 by 5 unmarks its 100 ghost blocks, and the other 300 stay marked.
drv 1 "lookat 100.5 -59.5 20" "wait 5" > /dev/null
hold_ctrl
xdo 1 mousedown 1
drv 1 "lookat 104.5 -56 24.5" "wait 10" > /dev/null
check "a box started on a marked block unmarks" eval '[[ $(val 1 markbox) == "unmark 100,-60,20..104,-57,24 red" ]] &&
	[[ $(val 1 modehud) == "[Release Left-click: unmark the box][Esc: cancel][5 × 4 × 5]" ]]'
line_shot line-delete-box-unmark
xdo 1 mouseup 1
drv 1 "wait 5" > /dev/null
check "letting go unmarks its 100 ghost blocks, leaving 300 marked" eval \
	'[[ $(val 1 marks) == "300 marks, 300 to erase" ]] && [[ $(val 1 markbox) == none ]]'
let_go_ctrl
# A right click while the box is drawn ends it, marking nothing, and leaves Delete on. This box starts on the grass in
# front of the bricks, a real block, so that holding Left Ctrl first marks nothing.
drv 1 "lookat 104.5 -61 18.5" "wait 5" > /dev/null
hold_ctrl
xdo 1 mousedown 1
drv 1 "lookat 109.5 -56 29.5" "wait 10" > /dev/null
check "a box can start on a real block" eval '[[ $(val 1 markbox) == "mark 104,-61,18..109,-57,29 red" ]]'
use_click 1
drv 1 "wait 5" > /dev/null
check "a right click during the box ends it, marking nothing" eval '[[ $(val 1 markbox) == none ]] &&
	[[ $(val 1 marks) == "300 marks, 300 to erase" ]] && [[ $(val 1 mousemode) == DELETE ]]'
xdo 1 mouseup 1
let_go_ctrl
# Esc while the box is drawn ends it too, marking nothing, as its line's [Esc: cancel] says, and pauses nothing.
drv 1 "lookat 104.5 -61 18.5" "wait 5" > /dev/null
hold_ctrl
xdo 1 mousedown 1
drv 1 "lookat 109.5 -56 29.5" "wait 10" > /dev/null
check "with a box drawn again" eval '[[ $(val 1 markbox) == "mark 104,-61,18..109,-57,29 red" ]]'
press 1 Escape
check "Esc during the box ends it, marking nothing" eval '[[ $(val 1 markbox) == none ]] &&
	[[ $(val 1 marks) == "300 marks, 300 to erase" ]] && [[ $(val 1 mousemode) == DELETE ]] &&
	[[ $(val 1 screenclass) == none ]]'
xdo 1 mouseup 1
let_go_ctrl
check "and letting go of the button marks nothing" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 marks) == "300 marks, 300 to erase" ]]'
# Held a second, the left button erases the 300 marked bricks as one edit, which one Ctrl+Z takes back.
drv 1 "holdreset" > /dev/null
xdo 1 mousedown 1
check "a left click held a second erases the box's 300 marks as one edit" eval 'drv 2 "await block 109 -57 29 minecraft:air" \
	> /dev/null && client_said 1 "Erased 300 ghost blocks." && [[ $(val 1 holdlog) == *" edits=1 "* ]]'
xdo 1 mouseup 1
check "leaving the unmarked ones" eval '[[ $(val 2 "countbox 100 -60 20 109 -57 29 minecraft:stone_bricks") == "layer 100 "* ]]'
drv 1 "lookat 104.5 -40 40" "wait 5" "close" > /dev/null
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "one Ctrl+Z brings them back" eval 'drv 2 "await block 109 -57 29 minecraft:stone_bricks" > /dev/null &&
	[[ $(val 2 "countbox 100 -60 20 109 -57 29 minecraft:stone_bricks") == "layer 400 "* ]]'
drv 1 "edit 100 -60 20 109 -57 29 minecraft:air" "cmd tp @s 103.5 -54.12 32 0 0" "wait 20" > /dev/null
check "erasing the bricks reaches both games" eval 'drv 2 "await block 109 -57 29 minecraft:air" > /dev/null &&
	drv 1 "await block 109 -57 29 minecraft:air" > /dev/null'

# The Delete button with ghost blocks hidden shows them and starts Delete; the Copy button in Delete starts a Copy box.
use_click 1
check "with no marks, a right click ends Delete" eval 'drv 1 "wait 10" > /dev/null && [[ $(val 1 mousemode) == none ]]'
press 1 n
check "with ghost blocks hidden" eval '[[ $(val 1 ghosts) == "hidden, "* ]]'
delete_button
check "the Delete button shows them and starts Delete" eval '[[ $(val 1 ghosts) == "shown, "* ]] &&
	[[ $(val 1 mousemode) == DELETE ]]'
copy_button
check "the Copy button in Delete starts a Copy box" eval '[[ $(val 1 drawbox) == "COPY 0 "* ]] &&
	[[ $(val 1 mousemode) == BOX ]]'
use_click 1
drv 1 "wait 10" "edit 100 -55 40 111 -51 40 minecraft:air" "cmd setblock 103 -53 39 minecraft:air" > /dev/null
check "erasing the wall and its door reaches both games" eval 'drv 2 "await block 111 -54 40 minecraft:air" \
	> /dev/null && drv 1 "await block 111 -54 40 minecraft:air" "await block 109 -51 40 minecraft:air" > /dev/null'

# What marks cost, logged: 200,000 ghost stone bricks of 80 by 50 by 50, the most one edit holds, under the players;
# frames timed with none marked and with all marked, building their shape and the shape of 200,000 marks that touch no
# other, and a frame's sweep of 65 aims, half over the bricks and half to the horizon, and all to the horizon. Two more
# ghost bricks beside the top corner, at 140 and 141 -11 51, stay unmarked.
drv 1 "cmd tp @s 100.5 0 26.5 0 30" "wait 60" > /dev/null
drv 1 "edit 60 -60 2 139 -11 51 minecraft:stone_bricks" "edit 140 -11 51 141 -11 51 minecraft:stone_bricks" > /dev/null
check "both games show the 200,002 ghost bricks" eval 'drv 2 "await block 139 -11 51 minecraft:stone_bricks" \
	"await block 141 -11 51 minecraft:stone_bricks" > /dev/null && drv 1 "await block 60 -60 2 minecraft:stone_bricks" \
	"await block 139 -11 51 minecraft:stone_bricks" "await block 141 -11 51 minecraft:stone_bricks" > /dev/null'
delete_button
drv 1 "wait 200" "frametimes 300" "await framestimed" > /dev/null
echo "  frames with no marks: $(val 1 frametimes)" >> /out/checks.log
check "all 200,000 ghost bricks are marked" eval '[[ $(val 1 "markall 60 -60 2 139 -11 51") == 200000 ]] &&
	[[ $(val 1 marks) == "200000 marks, 200000 to erase" ]]'
drv 1 "wait 20" "frametimes 300" "await framestimed" > /dev/null
echo "  frames with 200,000 marks: $(val 1 frametimes)" >> /out/checks.log
shot 1 delete-200000-marks
echo "  building the shape of the 200,000 marks: $(val 1 timeoutline)" >> /out/checks.log
echo "  building the shape of 200,000 marks that touch no other: $(val 1 "timeoutline scattered 200000")" \
	>> /out/checks.log
echo "  a sweep half over the bricks and half to the horizon: $(val 1 "timeaims 100.5 -11 30 100.5 0 280 100")" \
	>> /out/checks.log
echo "  a sweep all to the horizon: $(val 1 "timeaims 350 0 26.5 100.5 0 280 100")" >> /out/checks.log
check "with the marks at the cap, Delete's line warns above its controls" eval \
	'[[ $(val 1 modehud) == "Too many blocks | $delete_marked" ]]'
line_shot line-delete-cap
# With the marks at the cap, a box over the two unmarked bricks would pass it: it draws red, with the warning above its
# line, and letting go marks nothing.
drv 1 "lookat 140.5 -10 51.5" "wait 5" > /dev/null
hold_ctrl
xdo 1 mousedown 1
drv 1 "lookat 141.5 -10 51.5" "wait 10" > /dev/null
check "a box that would take the marks past the cap draws red" eval \
	'[[ $(val 1 markbox) == "mark 140,-11,51..141,-11,51 limit" ]] &&
	[[ $(val 1 modehud) == "Too many blocks | [Release Left-click: mark the box][Esc: cancel][2 × 1 × 1]" ]]'
line_shot line-delete-box-too-many
xdo 1 mouseup 1
drv 1 "wait 5" > /dev/null
check "and letting go marks nothing" eval '[[ $(val 1 marks) == "200000 marks, 200000 to erase" ]] &&
	[[ $(val 1 markbox) == none ]]'
let_go_ctrl
use_click 1
use_click 1
check "the marks clear and Delete ends" eval 'drv 1 "wait 10" > /dev/null && [[ $(val 1 mousemode) == none ]] &&
	[[ $(val 1 marks) == "0 marks, 0 to erase" ]]'
drv 1 "edit 60 -60 2 139 -11 51 minecraft:air" "edit 140 -11 51 141 -11 51 minecraft:air" > /dev/null
check "erasing the bricks reaches both games" eval 'drv 2 "await block 60 -60 2 minecraft:air" \
	"await block 141 -11 51 minecraft:air" > /dev/null && drv 1 "await block 141 -11 51 minecraft:air" > /dev/null'
drv 1 "cmd tp @s 8.5 -56 9.5 0 30" "wait 40" > /dev/null

# Locking the first click in Single, on the grass north-west of the rest, with stone bricks. Facing north from -59.5
# -58 -36.5 and aimed at -60 -61 -40's top, a right click would place at -60 -60 -40. Planner2 flies beside it, since
# a game gets the layer's changes only where it has the world loaded.
main_hand=$(val 1 slot | cut -d' ' -f2)
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "ebmode SINGLE" "ebarray 0" \
	"cmd tp @s -59.5 -58 -36.5 180 30" "cmd tp Planner2 -66.5 -55 -34.5 180 30" "wait 60" > /dev/null
aim 1 -60 -60 -40
check "in Single mode with stone bricks, the lock's controls are active, unlocked" eval \
	'[[ $(val 1 lock) == "unlocked active" ]]'
press 1 h
check "H locks the position beside the aimed face" eval '[[ $(val 1 lock) == "locked -60,-60,-40 active" ]]'
drv 1 "cmd tp @s -54.5 -58 -36.5 180 30" "wait 40" > /dev/null
aim 1 -52 -60 -44
check "flown 5 blocks away and aiming elsewhere, Effortless Building's preview draws it there" eval \
	'drv 1 "wait 10" "await preview -60,-60,-40" > /dev/null && [[ $(val 1 lock) == "locked -60,-60,-40 active" ]]'
check "with Effortless Building's array of 2 copies one block apart upward, the preview draws all three" \
	drv 1 "ebarray 2 0 1 0" "wait 10" "await preview -60,-60,-40 -60,-59,-40 -60,-58,-40" "ebarray 0"
drv 1 "cmd tp @s -59.5 -58 -35.5 180 30" "wait 40" > /dev/null
# lock_nudge <xdotool key> <what it does> <the position after>: presses the key and checks the lock moved there.
lock_nudge() {
	press 1 "$1"
	check "$2" eval "[[ \$(val 1 lock) == 'locked $3 active' ]]"
}
lock_nudge Up "facing north, Up moves it one block north" -60,-60,-41
lock_nudge Prior "Page Up one block up" -60,-59,-41
lock_nudge Next "and Page Down one down" -60,-60,-41
use_click 1
check "a right click places one ghost block there, on both clients" eval \
	'drv 1 "await block -60 -60 -41 minecraft:stone_bricks" > /dev/null &&
	drv 2 "await block -60 -60 -41 minecraft:stone_bricks" > /dev/null && [[ $(val 1 holdlog) == *" edits=1 "* ]]'
check "and the lock ends" eval '[[ $(val 1 lock) == "unlocked active" ]]'
# Locked and nudged west, then held for a second while looking at the sky, the right button places once, at the locked
# position: the lock ends with that block, and the repeats aim at nothing.
aim 1 -60 -60 -40
press 1 h
lock_nudge Left "Left moves it one block west" -61,-60,-40
drv 1 "lookat -59.5 -20 -60" "wait 5" "holdreset" > /dev/null
xdo 1 mousedown 3
drv 1 "wait 20" > /dev/null
xdo 1 mouseup 3
check "held for a second, the right button places once at the locked position and the lock ends" eval \
	'drv 1 "wait 10" > /dev/null && [[ $(val 1 holdlog) == *" edits=1 "* ]] &&
	drv 2 "await block -61 -60 -40 minecraft:stone_bricks" > /dev/null && [[ $(val 1 lock) == "unlocked active" ]]'
# Locked on the ghost block just placed and aimed at the first one's south face, a left click erases at the locked
# position.
aim 1 -60 -60 -40
press 1 h
lock_nudge Left "locked again and nudged west, onto the ghost block placed there" -61,-60,-40
drv 1 "lookat -59.5 -59.5 -40.5" "wait 5" > /dev/null
attack_click 1
check "aiming at another ghost block, a left click erases the ghost block at the locked position" eval \
	'drv 2 "await block -61 -60 -40 minecraft:air" > /dev/null && drv 1 "await block -61 -60 -40 minecraft:air" > /dev/null'
check "leaves the aimed one, and the lock ends" eval '[[ $(val 1 "layer -60 -60 -41") == minecraft:stone_bricks ]] &&
	[[ $(val 2 "layer -60 -60 -41") == minecraft:stone_bricks ]] && [[ $(val 1 lock) == "unlocked active" ]]'
# With Effortless Building's array of 2 copies one block apart upward, a locked right click places three ghost blocks,
# and a left click locked at the first erases all three.
drv 1 "ebarray 2 0 1 0" "wait 5" > /dev/null
aim 1 -60 -60 -40
press 1 h
use_click 1
check "with an array of 2, a right click on the locked position places three ghost blocks" eval \
	'drv 1 "await block -60 -60 -40 minecraft:stone_bricks" "await block -60 -59 -40 minecraft:stone_bricks" \
	"await block -60 -58 -40 minecraft:stone_bricks" > /dev/null && [[ $(val 1 lock) == "unlocked active" ]]'
aim 1 -59 -60 -40
press 1 h
lock_nudge Left "locked east of them and nudged west, onto the first" -60,-60,-40
attack_click 1
check "and a left click there erases all three" eval 'drv 2 "await block -60 -60 -40 minecraft:air" \
	"await block -60 -59 -40 minecraft:air" "await block -60 -58 -40 minecraft:air" > /dev/null &&
	drv 1 "await block -60 -58 -40 minecraft:air" > /dev/null &&
	[[ $(val 1 "layer -60 -60 -41") == minecraft:stone_bricks ]] && [[ $(val 1 lock) == "unlocked active" ]]'
drv 1 "ebarray 0" > /dev/null
# Held for a second while looking at the sky, the left button locked on a ghost block erases it once.
drv 1 "edit -61 -60 -40 -61 -60 -40 minecraft:stone_bricks" "await block -61 -60 -40 minecraft:stone_bricks" > /dev/null
aim 1 -60 -60 -40
press 1 h
lock_nudge Left "locked and nudged onto another ghost block" -61,-60,-40
drv 1 "lookat -59.5 -20 -60" "wait 5" "holdreset" > /dev/null
xdo 1 mousedown 1
drv 1 "wait 20" > /dev/null
xdo 1 mouseup 1
check "held for a second, the left button erases it once and the lock ends" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 holdlog) == *" edits=1 "* ]] && drv 1 "await block -61 -60 -40 minecraft:air" > /dev/null &&
	[[ $(val 1 lock) == "unlocked active" ]]'
# A locked oak stair, nudged up into open air, places there facing east, as Planner1 faces, and the lock ends.
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:oak_stairs" \
	"cmd tp @s -64.5 -58 -40.5 -90 30" "wait 40" > /dev/null
aim 1 -62 -60 -41
press 1 h
lock_nudge Prior "a locked oak stair nudged up" -62,-59,-41
use_click 1
check "places there facing east, and the lock ends" eval \
	'drv 1 "await block -62 -59 -41 minecraft:oak_stairs" > /dev/null &&
	[[ $(val 1 "layerstate -62 -59 -41") == *"facing=east"* ]] && [[ $(val 1 lock) == "unlocked active" ]]'
# With a stick in hand, the controls aren't active: H and the nudge keys do nothing, and the clicks are Minecraft's: a
# right click sends nothing, and a left click breaks a real stone and leaves the ghost blocks.
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" \
	"cmd tp @s -59.5 -58 -35.5 180 30" "wait 40" > /dev/null
aim 1 -60 -60 -40
press 1 h
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:stick" "cmd setblock -58 -60 -39 minecraft:stone" \
	"wait 10" > /dev/null
check "with a stick in hand, the lock's controls aren't active" eval \
	'[[ $(val 1 lock) == "locked -60,-60,-40 inactive" ]]'
press 1 h
press 1 Up
check "and H and the nudge keys do nothing" eval '[[ $(val 1 lock) == "locked -60,-60,-40 inactive" ]]'
use_click 1
check "a right click with a stick sends nothing" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 holdlog) == *" edits=0 "* ]] && [[ $(val 1 "layer -60 -60 -40") == minecraft:air ]]'
drv 1 "lookat -57.5 -59.5 -38.5" "wait 5" > /dev/null
attack_click 1
check "and a left click breaks the real stone, as without the add-on" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 "real -58 -60 -39") == minecraft:air ]] && [[ $(val 1 holdlog) == *" edits=0 "* ]] &&
	[[ $(val 1 "layer -60 -60 -41") == minecraft:stone_bricks ]]'
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "wait 5" > /dev/null
check "with stone bricks back in hand, the lock's controls are active again, still locked" eval \
	'[[ $(val 1 lock) == "locked -60,-60,-40 active" ]]'
drv 1 "ebmode LINE" "wait 5" > /dev/null
check "choosing Line in the radial menu ends the lock" eval '[[ $(val 1 lock) == "unlocked active" ]]'
drv 1 "ebmode SINGLE" "wait 5" > /dev/null
# With the copy at the aim, H locks the copy and not the first click.
aim 1 -60 -60 -40
press 1 h
paste_button
press 1 h
check "with the copy at the aim, H locks the copy and not the first click" eval '[[ $(val 1 copylock) == locked ]] &&
	[[ $(val 1 lock) == "unlocked inactive" ]]'
press 1 h
attack_click 1
check "a left click puts the copy away" eval 'drv 1 "wait 10" > /dev/null && copy_is 1 "^away "'
# With Freecam's camera on, facing east while Planner1 faces north, Up moves the locked position east, away from the
# camera.
aim 1 -60 -60 -40
press 1 h
check "Freecam's camera comes on, facing east, with the position locked" eval \
	'[[ $(val 1 lock) == "locked -60,-60,-40 active" ]] && [[ $(val 1 "freecam true") == true ]] &&
	drv 1 "wait 10" > /dev/null && [[ $(val 1 "camtp -66.5 -57.5 -39.5 -90 30") == "FreeCamera -66.5,-57.5,-39.5 east" ]]'
press 1 Up
check "with Freecam's camera on, Up moves the locked position away from the camera, east" eval \
	'[[ $(val 1 lock) == "locked -59,-60,-40 active" ]]'
check "Freecam's camera goes off" eval '[[ $(val 1 "freecam false") == false ]] && drv 1 "wait 10" > /dev/null'
# The locked position in open air, 3 blocks up, with the crosshair on the grass, shot locked and then unlocked in the
# same view, for the shots' outline.
press 1 Prior
press 1 Prior
press 1 Prior
drv 1 "lookat -50.5 -61 -50.5" "wait 5" > /dev/null
check "the first click's block floats in open air, locked" eval '[[ $(val 1 lock) == "locked -59,-57,-40 active" ]] &&
	drv 1 "await preview -59,-57,-40" > /dev/null'
shots single-locked
press 1 h
check "H unlocks it" eval '[[ $(val 1 lock) == "unlocked active" ]]'
shots single-unlocked
drv 1 "edit -70 -60 -50 -50 -55 -30 minecraft:air" > /dev/null
check "erasing the ghost blocks reaches both games" eval 'drv 2 "await block -60 -60 -41 minecraft:air" > /dev/null &&
	drv 1 "await block -60 -60 -41 minecraft:air" > /dev/null'

# Locking each step in Wall, Cube and Floor, on the same grass with stone bricks, from Planner1 at -59.5 -58 -35.5
# facing north.
# preview_has <count> <positions>...: Effortless Building's preview drew that many blocks, among them each position.
preview_has() {
	local drawn pos
	drv 1 "wait 10" > /dev/null
	drawn=" $(val 1 preview) "
	echo "  preview: $(($(wc -w <<< "$drawn"))) blocks" >> /out/checks.log
	(($(wc -w <<< "$drawn") == $1)) || return 1
	shift
	for pos in "$@"; do
		[[ $drawn == *" $pos "* ]] || return 1
	done
}
# all_in_layer <block> <positions>...: Planner1's copy of the layer has that block at each position (x,y,z).
all_in_layer() {
	local block=$1 pos
	shift
	for pos in "$@"; do
		[[ $(val 1 "layer ${pos//,/ }") == "minecraft:$block" ]] || return 1
	done
}
drv 1 "ebmode WALL" "ebarray 0" "cmd tp @s -59.5 -58 -35.5 180 30" "wait 40" > /dev/null
aim 1 -64 -60 -44
check "in Wall, the lock's controls are active, unlocked" eval '[[ $(val 1 lock) == "unlocked active" ]]'
press 1 h
lock_nudge Up "H before the first click locks the first corner, and Up nudges it north" -64,-60,-45
aim 1 -56 -60 -40
check "aimed elsewhere, the preview draws the first corner alone, where it's locked" \
	drv 1 "wait 10" "await preview -64,-60,-45"
use_click 1
check "the first click starts the wall at the locked corner, and the far corner's step starts unlocked" eval \
	'drv 1 "wait 10" > /dev/null && [[ $(val 1 lock) == "unlocked active" ]] &&
	[[ $(val 1 holdlog) == *" edits=0 "* ]]'
drv 1 "lookat -61.5 -58.5 -44.5" "wait 5" > /dev/null
check "the far corner follows the aim: aimed at -62 -59 -45, the preview draws a wall of 6" \
	preview_has 6 -64,-60,-45 -62,-59,-45
drv 1 "lookat -59.5 -57.5 -44.5" "wait 5" > /dev/null
check "and aimed at -60 -58 -45, a wall of 15" preview_has 15 -64,-60,-45 -60,-58,-45
press 1 h
check "H locks the far corner" eval '[[ $(val 1 lock) == "locked -60,-58,-45 active" ]]'
drv 1 "lookat -62.5 -59.5 -43.5" "wait 5" > /dev/null
check "which stays as Planner1 turns: aimed at -63 -60 -44, the preview still draws the wall of 15" eval \
	'preview_has 15 -64,-60,-45 -60,-58,-45 && [[ $(val 1 lock) == "locked -60,-58,-45 active" ]]'
shots wall-step-locked
lock_nudge Prior "Page Up moves the far corner a block up" -60,-57,-45
check "and makes the wall a block taller: 20 blocks" preview_has 20 -64,-60,-45 -60,-57,-45
lock_nudge Up "Up, across the wall, moves the far corner a block north" -60,-57,-46
check "and the whole wall with it" eval 'preview_has 20 -64,-60,-46 -60,-57,-46 &&
	[[ " $(val 1 preview) " != *" -64,-60,-45 "* ]]'
shots wall-step-nudged
shown=$(val 1 preview)
use_click 1
check "the second click places the wall shown, on both clients, and the lock ends" eval \
	'drv 1 "await block -60 -57 -46 minecraft:stone_bricks" > /dev/null &&
	drv 2 "await block -60 -57 -46 minecraft:stone_bricks" > /dev/null &&
	[[ $(val 1 "boxstates -64 -60 -46 -60 -57 -46 stone_bricks") == "20 20" ]] &&
	all_in_layer stone_bricks $shown &&
	[[ $(val 1 "layer -64 -60 -45") == minecraft:air ]] && [[ $(val 1 lock) == "unlocked active" ]]'
drv 1 "edit -64 -60 -46 -60 -57 -46 minecraft:air" "await block -60 -57 -46 minecraft:air" > /dev/null
# In Cube, after the second click, H locks the height; Page Up raises it and Up, a level nudge, moves the whole cube
# north, with the preview; the third click, aimed higher, places the cube shown.
drv 1 "ebmode CUBE" "wait 5" > /dev/null
aim 1 -66 -60 -46
use_click 1
aim 1 -63 -60 -48
use_click 1
# Aimed at the middle of the second position's block, the height is that block's alone.
drv 1 "lookat -62.5 -59.5 -47.5" "wait 5" > /dev/null
check "in Cube, after the second click the preview draws the floor from -66 -60 -46 to -63 -60 -48" \
	preview_has 12 -66,-60,-46 -63,-60,-48
press 1 h
check "H locks the height, at the second position's block" eval '[[ $(val 1 lock) == "locked -63,-60,-48 active" ]]'
lock_nudge Prior "Page Up raises it a block" -63,-59,-48
check "and the preview draws the cube 2 high" preview_has 24 -66,-60,-46 -63,-59,-48
shots cube-height-locked
lock_nudge Up "Up, a level nudge, moves the height a block north" -63,-59,-49
check "and the whole cube with it" eval 'preview_has 24 -66,-60,-47 -63,-59,-49 &&
	[[ " $(val 1 preview) " != *" -66,-60,-46 "* ]]'
drv 1 "lookat -62.5 -57.5 -48.5" "wait 5" > /dev/null
use_click 1
check "the third click, aimed higher, places the cube shown, 2 high, and the lock ends" eval \
	'drv 1 "await block -63 -59 -49 minecraft:stone_bricks" > /dev/null &&
	drv 2 "await block -66 -60 -47 minecraft:stone_bricks" > /dev/null &&
	[[ $(val 1 "boxstates -66 -60 -47 -63 -59 -49 stone_bricks") == "24 24" ]] &&
	[[ $(val 1 "layer -63 -58 -49") == minecraft:air ]] &&
	[[ $(val 1 "layer -66 -60 -46") == minecraft:air ]] && [[ $(val 1 "layer -63 -60 -50") == minecraft:air ]] &&
	[[ $(val 1 lock) == "unlocked active" ]]'
drv 1 "edit -66 -60 -49 -63 -58 -47 minecraft:air" "await block -63 -59 -49 minecraft:air" > /dev/null
# In Floor, after the first click, H locks the far corner, and Page Up moves the whole floor a block up.
drv 1 "ebmode FLOOR" "wait 5" > /dev/null
aim 1 -66 -60 -46
use_click 1
aim 1 -63 -60 -48
check "in Floor, after the first click the preview draws the floor from -66 -60 -46 to -63 -60 -48" \
	preview_has 12 -66,-60,-46 -63,-60,-48
press 1 h
check "H locks the far corner" eval '[[ $(val 1 lock) == "locked -63,-60,-48 active" ]]'
lock_nudge Prior "Page Up moves it a block up" -63,-59,-48
check "and the whole floor with it" eval 'preview_has 12 -66,-59,-46 -63,-59,-48 &&
	[[ " $(val 1 preview) " != *" -66,-60,-46 "* ]]'
use_click 1
check "the second click places the floor a block up, and the lock ends" eval \
	'drv 1 "await block -63 -59 -48 minecraft:stone_bricks" > /dev/null &&
	[[ $(val 1 "boxstates -66 -59 -46 -63 -59 -48 stone_bricks") == "12 12" ]] &&
	[[ $(val 1 "layer -66 -60 -46") == minecraft:air ]] && [[ $(val 1 lock) == "unlocked active" ]]'
drv 1 "edit -66 -59 -48 -63 -59 -46 minecraft:air" "await block -63 -59 -48 minecraft:air" "ebmode SINGLE" \
	> /dev/null

# R in Single, and a Line of stairs from a locked position, on the same grass, from Planner1 at -58.5 -58 -34.5 facing
# north. Single has no row modes: R leaves its line and the copy's mode as they are, and a right click places one block.
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "ebmode SINGLE" "ebarray 0" \
	"cmd tp @s -58.5 -58 -34.5 180 30" "wait 40" > /dev/null
aim 1 -64 -60 -40
check "in Single with stone bricks, Single's line shows" eval '[[ $(val 1 modehud) == "$single_line" ]]'
press 1 r
check "R does nothing in Single: the line, the lock and the copy's mode stay" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 modehud) == "$single_line" ]] && [[ $(val 1 lock) == "unlocked active" ]] &&
	[[ $(val 1 copymode) == OFF ]]'
use_click 1
check "and a right click places one block, as one edit" eval \
	'drv 1 "await block -64 -60 -40 minecraft:stone_bricks" > /dev/null && [[ $(val 1 holdlog) == *" edits=1 "* ]] &&
	(($(val 1 "countblocks -70 -45 -60 -40 minecraft:stone_bricks") == 1))'
drv 1 "edit -64 -60 -40 -64 -60 -40 minecraft:air" "await block -64 -60 -40 minecraft:air" > /dev/null
# In Line with oak stairs, from farther off: locked on -62 -61 -38's top, the first click aimed elsewhere starts the
# Line there; looking up into open air at -61.5 -49 -37.5, on the vertical line through the start, the Line rises
# 12 blocks, to -49, and the second click places it as one edit.
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:oak_stairs" "ebmode LINE" \
	"cmd tp @s -54.5 -58 -30.5 135 20" "wait 40" > /dev/null
aim 1 -62 -60 -38
press 1 h
check "in Line with oak stairs, H locks the first click's position" eval \
	'[[ $(val 1 lock) == "locked -62,-60,-38 active" ]]'
aim 1 -58 -60 -36
use_click 1
check "the first click, aimed elsewhere, starts the Line at the locked position and sends no edit" eval \
	'drv 1 "wait 10" > /dev/null && [[ $(val 1 holdlog) == *" edits=0 "* ]] &&
	[[ $(val 1 lock) == "unlocked active" ]] && [[ $(val 1 "layer -62 -60 -38") == minecraft:air ]]'
drv 1 "lookat -61.5 -49 -37.5" "wait 5" > /dev/null
check "looking up into open air, the preview draws a Line of 12 upward" preview_has 12 -62,-60,-38 -62,-49,-38
shots line-locked-upward
use_click 1
check "the second click places the 12 stairs as one edit, on both clients, and the lock ends" eval \
	'drv 1 "await block -62 -49 -38 minecraft:oak_stairs" "await block -62 -60 -38 minecraft:oak_stairs" > /dev/null &&
	drv 2 "await block -62 -49 -38 minecraft:oak_stairs" > /dev/null && [[ $(val 1 holdlog) == *" edits=1 "* ]] &&
	[[ $(val 1 "boxstates -62 -60 -38 -62 -49 -38 oak_stairs") == "12 12" ]] &&
	[[ $(val 1 "layer -62 -48 -38") == minecraft:air ]] && [[ $(val 1 lock) == "unlocked active" ]]'
drv 1 "edit -62 -60 -38 -62 -49 -38 minecraft:air" "ebmode SINGLE" > /dev/null
check "erasing the Line reaches both games" eval 'drv 2 "await block -62 -49 -38 minecraft:air" > /dev/null &&
	drv 1 "await block -62 -60 -38 minecraft:air" > /dev/null'
# In Line, a nudge across a line of 5 moves the whole line: from Planner1 at -59.5 -58 -32.5 facing north, the first
# click on -62 -60 -38 and the far end aimed at -58 -60 -38 and locked, Up moves both ends a block north.
drv 1 "ebmode LINE" "cmd tp @s -59.5 -58 -32.5 180 30" "wait 40" > /dev/null
aim 1 -62 -60 -38
use_click 1
aim 1 -58 -60 -38
check "in Line, after the first click the preview draws a line of 5 east" preview_has 5 -62,-60,-38 -58,-60,-38
press 1 h
check "H locks the line's far end" eval '[[ $(val 1 lock) == "locked -58,-60,-38 active" ]]'
lock_nudge Up "Up, across the line, moves the far end a block north" -58,-60,-39
check "and the whole line with it" eval 'preview_has 5 -62,-60,-39 -58,-60,-39 &&
	[[ " $(val 1 preview) " != *" -62,-60,-38 "* ]]'
use_click 1
check "the second click places the moved line of 5, and the lock ends" eval \
	'drv 1 "await block -58 -60 -39 minecraft:oak_stairs" > /dev/null &&
	[[ $(val 1 "boxstates -62 -60 -39 -58 -60 -39 oak_stairs") == "5 5" ]] &&
	[[ $(val 1 "layer -62 -60 -38") == minecraft:air ]] && [[ $(val 1 lock) == "unlocked active" ]]'
drv 1 "edit -62 -60 -39 -58 -60 -39 minecraft:air" "await block -58 -60 -39 minecraft:air" "ebmode SINGLE" > /dev/null

# The red frame for the left click (hook E38), on the same grass with oak stairs, from Planner1 at -59.5 -58 -35.5
# facing north: before the first click, the block a left click would start erasing from shows framed in red, beside
# the stair a right click would place. In Single only on a ghost block, since a left click there erases nothing else;
# in the other modes on any aimed block, with the mirror's copy. A ghost stone brick stands at -62 -60 -40, a real
# stone at -60 -60 -40 for a while, and Effortless Building's mirror across x -66 puts a copy of -62 at -71 and of -60
# at -73. On ghost blocks the frame draws again after the world, more strongly (the driver's `ghostframe`), and on real
# blocks only in Effortless Building's look.
# frame_is [<positions>...]: the red frame covered exactly these positions (x,y,z) in the last whole frame, in any
# order; none without any.
frame_is() {
	local want=${*:-none}
	drv 1 "wait 5" > /dev/null
	[[ $(val 1 redframe | tr ' ' '\n' | sort | xargs) == "$(tr ' ' '\n' <<< "$want" | sort | xargs)" ]]
}
# ghost_frame_is [<positions>...]: the last frame drew the stronger frame on exactly these ghost blocks, in any order.
ghost_frame_is() {
	local want=${*:-none}
	[[ $(val 1 ghostframe | tr ' ' '\n' | sort | xargs) == "$(tr ' ' '\n' <<< "$want" | sort | xargs)" ]]
}
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:oak_stairs" "ebmode SINGLE" "ebarray 0" \
	"cmd tp @s -59.5 -58 -35.5 180 30" "edit -62 -60 -40 -62 -60 -40 minecraft:stone_bricks" \
	"await block -62 -60 -40 minecraft:stone_bricks" "wait 40" > /dev/null
aim 1 -62 -59 -40
check "in Single, aimed at a ghost block's top, the red frame covers the ghost block" frame_is -62,-60,-40
check "and draws on it again, more strongly, after the world" ghost_frame_is -62,-60,-40
check "beside the stair a right click would place on it" eval '[[ $(val 1 preview) == -62,-59,-40 ]]'
shots frame-single-ghost
aim 1 -60 -60 -40
check "in Single, aimed at the grass, a real block, no red frame shows, and the preview draws the stair" eval \
	'frame_is && ghost_frame_is && [[ $(val 1 preview) == -60,-60,-40 ]]'
drv 1 "ebmirror -66" "wait 5" > /dev/null
aim 1 -62 -59 -40
check "with the mirror on, the red frame covers the ghost block and not its copy over the grass" frame_is -62,-60,-40
drv 1 "edit -71 -60 -40 -71 -60 -40 minecraft:stone_bricks" "await block -71 -60 -40 minecraft:stone_bricks" \
	> /dev/null
check "and with a ghost block at the copy too, both mirror copies" frame_is -62,-60,-40 -71,-60,-40
check "both drawn more strongly too" ghost_frame_is -62,-60,-40 -71,-60,-40
drv 1 "ebmode WALL" "ebarray 0" "cmd setblock -60 -60 -40 minecraft:stone" \
	"await real -60 -60 -40 minecraft:stone" "wait 5" > /dev/null
aim 1 -60 -59 -40
check "in Wall, aimed at a real stone's top, the red frame covers the aimed real block" frame_is -60,-60,-40
check "in Effortless Building's look alone" ghost_frame_is
check "beside the stair a right click would start the wall with" eval '[[ $(val 1 preview) == -60,-59,-40 ]]'
shots frame-wall-real
drv 1 "cmd setblock -60 -60 -40 minecraft:air" "await real -60 -60 -40 minecraft:air" > /dev/null
aim 1 -60 -60 -40
check "aimed at the grass, the grass block" frame_is -60,-61,-40
aim 1 -62 -59 -40
check "and aimed at a ghost block, the ghost block" frame_is -62,-60,-40
check "drawn more strongly, as in Single" ghost_frame_is -62,-60,-40
drv 1 "ebmirror -66" "wait 5" > /dev/null
aim 1 -60 -60 -40
check "with the mirror on, both mirror copies: the aimed grass and its copy's" frame_is -60,-61,-40 -73,-61,-40
press 1 h
check "H locks the first corner, and no red frame shows" eval \
	'[[ $(val 1 lock) == "locked -60,-60,-40 active" ]] && frame_is && ghost_frame_is'
press 1 h
check "H unlocks it, and the red frame shows again" frame_is -60,-61,-40 -73,-61,-40
use_click 1
check "during the wall, after the first click, no red frame shows" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 holdlog) == *" edits=0 "* ]] && frame_is'
drv 1 "ebmode DISABLED" "wait 5" > /dev/null
check "in Disable, no red frame shows" eval 'frame_is && ghost_frame_is'
drv 1 "ebmode SINGLE" "ebarray 0" "edit -71 -60 -40 -62 -60 -40 minecraft:air" \
	"await block -62 -60 -40 minecraft:air" "await block -71 -60 -40 minecraft:air" > /dev/null

# One ghost block, turning and flipping, on the same grass in Single mode, from Planner1 at -58.5 -58 -34.5 facing
# north. Hotbar slots 5 to 8 hold oak stairs, oak stairs again, stone and an oak log; a stair placed facing north
# faces north. A ghost glowstone at -66 -60 -40 is the ghost block the middle click aims at.
# turn_is <turn>: the turn of the block in hand, as the driver's heldturn gives it, after a few ticks.
turn_is() {
	drv 1 "wait 5" > /dev/null && [[ $(val 1 heldturn) == "$1" ]]
}
# stairs_face <x1> <x2> <y> <z> <turn>: every ghost block along that row is a stair with that turn's values.
stairs_face() {
	local x
	for ((x = $1; x <= $2; x++)); do
		[[ $(val 1 "layerstate $x $3 $4") == *"_stairs["*"$5,"* ]] || return 1
	done
}
first_slot=$(val 1 slot | cut -d' ' -f1)
drv 1 "cmd item replace entity @s hotbar.4 with minecraft:oak_stairs" \
	"cmd item replace entity @s hotbar.5 with minecraft:oak_stairs" "cmd item replace entity @s hotbar.6 with minecraft:stone" \
	"cmd item replace entity @s hotbar.7 with minecraft:oak_log" "ebmode SINGLE" "ebarray 0" \
	"edit -66 -60 -40 -66 -60 -40 minecraft:glowstone" "cmd tp @s -58.5 -58 -34.5 180 30" "wait 40" > /dev/null
press 1 5
aim 1 -60 -60 -40
check "with oak stairs in hand, the block has no turn" eval '[[ $(val 1 heldturn) == none ]] &&
	[[ $(val 1 slot) == "4 minecraft:oak_stairs" ]]'
hold_ctrl
xdo 1 click 4
check "facing north, a notch up with Left Ctrl held turns the stair a quarter turn clockwise, to face east" \
	turn_is "facing=east,half=bottom"
check "and the hotbar slot stays" eval '[[ $(val 1 slot) == "4 minecraft:oak_stairs" ]]'
let_go_ctrl
# Unlocked, following the aim in Single, hook E30 makes Effortless Building's preview draw the one block at the aim, once,
# turned as it will be placed; with its array of 2 copies one block apart upward, the stair and its copies, once each.
check "unlocked, the preview draws the stair at the aim alone, turned to face east" eval 'drv 1 "wait 10" > /dev/null &&
	echo "  unlocked preview: $(val 1 preview)" >> /out/checks.log && [[ $(val 1 preview) == "-60,-60,-40" ]] &&
	[[ $(val 1 "previewstate -60 -60 -40") == "minecraft:oak_stairs["*"facing=east,half=bottom,"* ]]'
check "with the array of 2 copies, it draws the stair and its 2 copies once each" eval \
	'drv 1 "ebarray 2 0 1 0" "wait 10" "await preview -60,-60,-40 -60,-59,-40 -60,-58,-40" "wait 5" > /dev/null &&
	[[ $(val 1 preview) == "-60,-60,-40 -60,-59,-40 -60,-58,-40" ]] &&
	[[ $(val 1 "previewstate -60 -58 -40") == "minecraft:oak_stairs["*"facing=east,half=bottom,"* ]]'
drv 1 "ebarray 0" "wait 10" > /dev/null
shots stair-at-aim
press 1 h
check "locked, the preview draws the stair turned to face east" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 "previewstate -60 -60 -40") == "minecraft:oak_stairs["*"facing=east,half=bottom,"* ]]'
press 1 h
use_click 1
check "a stair placed then faces east" eval 'drv 1 "await block -60 -60 -40 minecraft:oak_stairs" > /dev/null &&
	[[ $(val 1 "layerstate -60 -60 -40") == *"facing=east,half=bottom,"* ]]'
drv 1 "cmd tp @s -61.5 -58 -47.5 0 30" "wait 40" > /dev/null
aim 1 -62 -60 -44
use_click 1
check "and so does one placed after Planner1 turns to face south" eval \
	'drv 1 "await block -62 -60 -44 minecraft:oak_stairs" > /dev/null &&
	[[ $(val 1 "layerstate -62 -60 -44") == *"facing=east,half=bottom,"* ]]'
drv 1 "edit -62 -60 -44 -62 -60 -44 minecraft:air" "cmd tp @s -58.5 -58 -34.5 180 30" "wait 40" \
	"lookat -65.5 -59.5 -39.5" "wait 5" > /dev/null
hold_ctrl
xdo 1 click 2
check "the middle click with Left Ctrl held, aimed at a ghost block, makes the stair's top half" \
	turn_is "facing=east,half=top"
check "with no block picked, from the game or Litematica" eval '[[ $(val 1 slot) == "4 minecraft:oak_stairs" ]]'
let_go_ctrl
# Turned and flipped stairs before placing, locked at -56 -60 -40 so the preview draws them, and after.
aim 1 -56 -60 -40
press 1 h
drv 1 "lookat -50.5 -61 -50.5" "wait 5" > /dev/null
check "locked, the preview draws the turned and flipped stair" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 "previewstate -56 -60 -40") == "minecraft:oak_stairs["*"facing=east,half=top,"* ]]'
shots stairs-turned-before
use_click 1
check "placed, the stair faces east with its top half, on both clients" eval \
	'drv 1 "await block -56 -60 -40 minecraft:oak_stairs" > /dev/null &&
	[[ $(val 1 "layerstate -56 -60 -40") == *"facing=east,half=top,"* ]] &&
	drv 2 "await block -56 -60 -40 minecraft:oak_stairs" > /dev/null'
check "the lock ends with it, and the preview draws the block at the aim, and not the locked position's as well" eval \
	'drv 1 "wait 10" > /dev/null && echo "  unlocked preview over the placed stair: $(val 1 preview)" >> /out/checks.log &&
	[[ $(val 1 lock) == "unlocked active" ]] && (($(val 1 preview | wc -w) == 1))'
shots stairs-turned-after
# The same stairs in another slot keep the turn; stone drops it, and Left Ctrl with the wheel changes the hotbar slot
# with stone in hand, back to the second stairs, whose turn is gone.
press 1 6
check "the same stairs in a second slot keep the turn" turn_is "facing=east,half=top"
press 1 7
hold_ctrl
xdo 1 click 4
check "with stone in hand, Left Ctrl and the wheel change the hotbar slot" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 slot) == "5 minecraft:oak_stairs" ]]'
let_go_ctrl
check "switching to another block and back drops the turn" turn_is none
# An oak log aimed at the ghost glowstone's south face lies north to south; it flips to standing and back.
press 1 8
drv 1 "lookat -65.5 -59.5 -39.0" "wait 5" > /dev/null
hold_ctrl
xdo 1 click 2
check "an oak log flips to standing" turn_is "axis=y"
xdo 1 click 2
check "and back" turn_is "axis=z"
let_go_ctrl
# Without Left Ctrl, the wheel changes the hotbar slot and the middle click picks the aimed block.
xdo 1 click 5
check "without Left Ctrl, the wheel changes the hotbar slot" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 slot) == "8 "* ]]'
press 1 5
aim 1 -54 -60 -36
xdo 1 click 2
check "and the middle click picks the aimed block" eval 'drv 1 "wait 20" > /dev/null &&
	[[ $(val 1 slot) == *" minecraft:grass_block" ]]'
# What an unlocked left click in Single erases with Effortless Building's array and mirror on, logged for Geo: a right
# click aimed at -52 -61 -42's top places stone there with its copies, and a left click aims at the block placed.
press 1 7
drv 1 "ebarray 2 0 1 0" "wait 5" > /dev/null
aim 1 -52 -60 -42
use_click 1
drv 1 "await block -52 -58 -42 minecraft:stone" "lookat -51.5 -59.5 -41.5" "wait 5" > /dev/null
attack_click 1
drv 1 "wait 20" > /dev/null
echo "  an unlocked left click with the array of 2 upward: $(val 1 "layer -52 -60 -42") $(val 1 "layer -52 -59 -42")" \
	"$(val 1 "layer -52 -58 -42") at -52 -60..-58 -42" >> /out/checks.log
drv 1 "ebmirror -66" "wait 5" > /dev/null
aim 1 -60 -60 -37
use_click 1
drv 1 "await block -73 -60 -37 minecraft:stone" "lookat -59.5 -59.5 -36.5" "wait 5" > /dev/null
attack_click 1
drv 1 "wait 20" > /dev/null
echo "  an unlocked left click with the mirror across x -66: $(val 1 "layer -60 -60 -37") at -60 -60 -37," \
	"$(val 1 "layer -73 -60 -37") at -73 -60 -37" >> /out/checks.log
drv 1 "ebarray 0" "edit -85 -60 -50 -20 -50 -30 minecraft:air" > /dev/null
check "erasing the turned stairs reaches both games" eval 'drv 2 "await block -56 -60 -40 minecraft:air" \
	"await block -64 -60 -46 minecraft:air" > /dev/null && drv 1 "await block -66 -60 -40 minecraft:air" \
	"await block -53 -60 -46 minecraft:air" > /dev/null'
drv 1 "cmd item replace entity @s hotbar.4 with minecraft:air" "cmd item replace entity @s hotbar.5 with minecraft:air" \
	"cmd item replace entity @s hotbar.6 with minecraft:air" "cmd item replace entity @s hotbar.7 with minecraft:air" \
	"cmd item replace entity @s hotbar.8 with minecraft:air" > /dev/null
press 1 $((first_slot + 1))
drv 1 "cmd item replace entity @s weapon.mainhand with $main_hand" "cmd tp @s 8.5 -56 9.5 0 30" \
	"cmd tp Planner2 101.5 0 26.5 0 30" "wait 40" > /dev/null

# Turning in every mode (step 17), on the same grass, from Planner1 at -58.5 -58 -34.5 facing north, with Planner2
# beside it. Hotbar slots 5 to 9 hold oak stairs, oak stairs again, stone, a chest and an oak slab; a stair placed
# facing north faces north.
# preview_faces <y> <z> <x1> <x2> <turn>: the preview drew an oak stair with that turn's values at each x along that row.
preview_faces() {
	local x
	for ((x = $3; x <= $4; x++)); do
		[[ $(val 1 "previewstate $x $1 $2") == "minecraft:oak_stairs["*"$5,"* ]] || return 1
	done
}
# box_all <client> <x1 y1 z1 x2 y2 z2> <text> <count>: that box holds <count> ghost blocks or more on that client, and
# every one has the text in its state.
box_all() {
	local counts
	counts=$(val "$1" "boxstates $2 $3 $4 $5 $6 $7 $8")
	echo "  $2 $3 $4 to $5 $6 $7 on client $1: $counts with $8" >> /out/checks.log
	((${counts% *} >= $9)) && [[ ${counts% *} == "${counts#* }" ]]
}
# turned_shape <mode> <stand x z> <x1 y1 z1 x2 y2 z2> <text> <aims>...: from 5 blocks above the grass at x z, facing
# north, one shape in that Effortless Building mode with a right click at each aim (block positions, aimed at the
# middle of the top of the block below for the first two and 2.5 above the block for a third, which sets the height);
# then, within 5 seconds, the box holds 2 ghost blocks or more, each with the text in its state.
turned_shape() {
	local mode=$1 stand="$2 -55 $3" box="$4 $5 $6 $7 $8 $9" text=${10} x y z tries counts
	shift 10
	drv 1 "ebmode $mode" "cmd tp @s $stand 180 50" "wait 20" > /dev/null
	aim 1 $1
	use_click 1
	aim 1 $2
	use_click 1
	if [[ -n ${3:-} ]]; then
		read -r x y z <<< "$3"
		drv 1 "lookat $(half "$x") $(half $((y + 2))) $(half "$z")" "wait 5" > /dev/null
		use_click 1
	fi
	for ((tries = 0; tries < 20; tries++)); do
		counts=$(val 1 "boxstates $box $text")
		((${counts% *} >= 2)) && break
		drv 1 "wait 5" > /dev/null
	done
	box_all 1 $box "$text" 2
}
drv 1 "cmd item replace entity @s hotbar.4 with minecraft:oak_stairs" \
	"cmd item replace entity @s hotbar.5 with minecraft:oak_stairs" "cmd item replace entity @s hotbar.6 with minecraft:stone" \
	"cmd item replace entity @s hotbar.7 with minecraft:chest" "cmd item replace entity @s hotbar.8 with minecraft:oak_slab" \
	"ebmode SINGLE" "ebarray 0" "cmd tp @s -58.5 -58 -34.5 180 30" "cmd tp Planner2 -66.5 -55 -34.5 180 30" "wait 40" \
	> /dev/null
press 1 5
aim 1 -60 -60 -40
hold_ctrl
xdo 1 click 4
let_go_ctrl
check "in Single, a notch up with Left Ctrl held turns the oak stairs to face east" turn_is "facing=east,half=bottom"
drv 1 "ebmode LINE" "wait 5" > /dev/null
check "the turn stays after choosing Line in the radial menu" turn_is "facing=east,half=bottom"
# A Line of 5 at z -40, from -62 to -58.
aim 1 -62 -60 -40
use_click 1
aim 1 -58 -60 -40
check "a Line of 5 turned stairs: its preview draws each stair facing east" eval \
	'drv 1 "wait 10" > /dev/null && preview_faces -60 -40 -62 -58 "facing=east,half=bottom"'
use_click 1
check "placed, the Line's 5 stairs face east, on both clients" eval \
	'drv 1 "await block -58 -60 -40 minecraft:oak_stairs" > /dev/null && stairs_face -62 -58 -60 -40 "facing=east,half=bottom" &&
	box_all 1 -63 -60 -40 -57 -60 -40 "facing=east," 5 && drv 2 "await block -58 -60 -40 minecraft:oak_stairs" > /dev/null &&
	box_all 2 -63 -60 -40 -57 -60 -40 "facing=east," 5'
shots line-turned-stairs
# In Line, a notch up before the first click turns the stairs to face south. Before the first click, in every mode but
# Disable, the preview draws the first click's block at the aim, beside the aimed face, as it will be placed (hook
# E30), and nothing while aiming at nothing; the array's copies draw as before. Another notch between the clicks turns
# the preview at once, and the Line places with it.
hold_ctrl
xdo 1 click 4
let_go_ctrl
check "in Line, a notch up with Left Ctrl held before the first click turns the stairs to face south" \
	turn_is "facing=south,half=bottom"
aim 1 -62 -60 -42
# first_block_is <mode>: in that mode, the preview draws only the stair at -62 -60 -42, facing south.
first_block_is() {
	drv 1 "ebmode $1" "wait 10" > /dev/null &&
		echo "  first click's block in $1: $(val 1 preview)" >> /out/checks.log &&
		[[ $(val 1 preview) == "-62,-60,-42" ]] &&
		[[ $(val 1 "previewstate -62 -60 -42") == "minecraft:oak_stairs["*"facing=south,half=bottom,"* ]]
}
for mode in WALL CUBE SPHERE LINE; do
	check "in $mode, before the first click, the preview draws one stair at the aim, facing south" first_block_is $mode
	if [[ $mode == WALL || $mode == CUBE ]]; then
		name=first-click-${mode,,}
		drv 1 "wait 210" "notoasts" "wait 5" "shot $name-854x480" > /dev/null
		echo "  $name-854x480: stair $(val 1 "screenbox -62 -60 -42 -62 -60 -42")" >> /out/checks.log
		drv 1 "window 1920 1080 2" "wait 20" "notoasts" "wait 5" "shot $name-1920x1080" > /dev/null
		echo "  $name-1920x1080: stair $(val 1 "screenbox -62 -60 -42 -62 -60 -42")" >> /out/checks.log
		drv 1 "window 854 480 2" "wait 20" > /dev/null
	fi
done
drv 1 "lookat -58.5 100 -34.6" "wait 10" > /dev/null
check "aiming at the sky, the preview draws nothing" eval '[[ $(val 1 preview) == none ]]'
aim 1 -62 -60 -42
check "with the array of 2 copies one block apart upward, it draws the stair and its 2 copies once each" eval \
	'drv 1 "ebarray 2 0 1 0" "wait 10" "await preview -62,-60,-42 -62,-59,-42 -62,-58,-42" > /dev/null'
drv 1 "ebarray 0" "wait 10" > /dev/null
use_click 1
aim 1 -58 -60 -42
check "the first click's preview draws the stairs facing south" eval \
	'drv 1 "wait 10" > /dev/null && preview_faces -60 -42 -62 -58 "facing=south,half=bottom"'
hold_ctrl
xdo 1 click 4
check "another notch between the two clicks turns the preview at once, to face west" eval \
	'drv 1 "wait 3" > /dev/null && preview_faces -60 -42 -62 -58 "facing=west,half=bottom"'
check "with Left Ctrl held during the shape, the build modes' line names the wheel, the middle click and the lock" \
	eval '[[ $(val 1 modehud) == "[Scroll: turn][Middle-click: flip][H: Lock (Off)]" ]]'
let_go_ctrl
check "and without it, the sprint key and the lock" eval \
	'[[ $(val 1 modehud) == "[Hold Left Control: turn or flip][H: Lock (Off)]" ]]'
# During the shape, the build modes' line ends 2 GUI pixels above the action bar's text (72 to 63 above the screen's bottom),
# which shows the shape's block count above Effortless Building's own line (58 above the bottom). Each shot comes once
# the chat has faded, with the line's end moved between -57 and -58 first, so Effortless Building shows the count again.
drv 1 "wait 210" > /dev/null
end=-57
for size in "854 480 2" "1920 1080 2" "1920 1080 4"; do
	read -r w h s <<< "$size"
	drv 1 "window $w $h $s" "wait 20" "notoasts" > /dev/null
	aim 1 $end -60 -42
	drv 1 "wait 5" "shot build-line-shape-${w}x$h-gui$s" "wait 5" > /dev/null
	echo "  build-line-shape-${w}x$h-gui$s: $(val 1 modewrap) | box $(val 1 modebox) | action bar $(val 1 actionbar)" \
		>> /out/checks.log
	check "during a Line at $w by $h, GUI scale $s, the build modes' line ends 2 above the action bar's block count" eval \
		'[[ $(val 1 modebox | cut -d" " -f2,4) == "$((h - 83 * s)) $((h - 74 * s))" ]] && [[ $(val 1 actionbar) != none ]]'
	end=$((end == -57 ? -58 : -57))
done
drv 1 "window 854 480 2" "wait 20" > /dev/null
aim 1 -58 -60 -42
use_click 1
check "the Line places with the second turn, facing west" eval \
	'drv 1 "await block -58 -60 -42 minecraft:oak_stairs" > /dev/null && box_all 1 -63 -60 -42 -57 -60 -42 "facing=west," 5'
# One shape of the west-facing stairs in each other mode, each in a box of its own, from 5 blocks above the grass south
# of it.
check "a Wall of turned stairs faces west" turned_shape WALL -80.5 -42.5 -83 -61 -50 -78 -54 -46 "facing=west," \
	"-82 -60 -48" "-79 -60 -48"
check "so does a Floor" turned_shape FLOOR -73.5 -42.5 -76 -61 -50 -72 -59 -46 "facing=west," "-75 -60 -47" "-73 -60 -49"
check "a Cube" turned_shape CUBE -67.5 -42.5 -70 -61 -50 -66 -54 -46 "facing=west," "-69 -60 -47" "-67 -60 -49" \
	"-67 -60 -49"
check "a Diagonal Line" turned_shape DIAGONAL_LINE -61.5 -42.5 -64 -61 -50 -59 -54 -46 "facing=west," "-63 -60 -48" \
	"-60 -60 -48" "-60 -60 -48"
check "a Diagonal Wall" turned_shape DIAGONAL_WALL -54.5 -42.5 -57 -61 -50 -52 -54 -46 "facing=west," "-56 -60 -48" \
	"-53 -60 -48" "-53 -60 -48"
check "a Slope Floor" turned_shape SLOPE_FLOOR -80.5 -50.5 -83 -61 -58 -79 -54 -54 "facing=west," "-82 -60 -55" \
	"-80 -60 -57" "-80 -60 -57"
# Circle, Cylinder and Sphere start at a corner by default (Effortless Building's circleStart), so their two clicks
# are opposite corners.
check "a Circle" turned_shape CIRCLE -73.5 -50.5 -77 -61 -59 -71 -59 -53 "facing=west," "-76 -60 -54" "-72 -60 -58"
check "a Cylinder" turned_shape CYLINDER -65.5 -50.5 -69 -61 -59 -63 -54 -53 "facing=west," "-68 -60 -54" "-64 -60 -58" \
	"-64 -60 -58"
check "and a Sphere" turned_shape SPHERE -56.5 -50.5 -60 -63 -59 -54 -54 -53 "facing=west," "-59 -60 -54" "-55 -60 -58" \
	"-55 -60 -58"
# With Effortless Building's mirror across x -52, a Line from -58 to -55 at z -38 has its mirror image from -50 to -47,
# whose stairs face east, in the preview and once placed.
drv 1 "ebmode LINE" "cmd tp @s -52.5 -58 -34.5 180 30" "wait 20" "ebmirror -52" "wait 5" > /dev/null
aim 1 -58 -60 -38
use_click 1
aim 1 -55 -60 -38
check "with the mirror on, the preview draws the Line's stairs facing west and its mirror image's facing east" eval \
	'drv 1 "wait 10" > /dev/null && preview_faces -60 -38 -58 -55 "facing=west,half=bottom" &&
	preview_faces -60 -38 -50 -47 "facing=east,half=bottom"'
use_click 1
check "and placed, they face so" eval 'drv 1 "await block -47 -60 -38 minecraft:oak_stairs" > /dev/null &&
	box_all 1 -58 -60 -38 -55 -60 -38 "facing=west," 4 && box_all 1 -50 -60 -38 -47 -60 -38 "facing=east," 4'
drv 1 "ebarray 0" > /dev/null
# With the mix of oak and spruce stairs, a Wall's stairs all take the turn.
drv 1 "setmix minecraft:oak_stairs:50,minecraft:spruce_stairs:50" > /dev/null
check "with the mix of oak and spruce stairs, a Wall's stairs all face west" turned_shape WALL -46.5 -42.5 \
	-49 -61 -50 -44 -54 -46 "facing=west," "-48 -60 -48" "-45 -60 -48"
check "of both kinds" eval '(($(val 1 "boxstates -49 -61 -50 -44 -54 -46 spruce_stairs[" | cut -d" " -f2) > 0)) &&
	(($(val 1 "boxstates -49 -61 -50 -44 -54 -46 oak_stairs[" | cut -d" " -f2) > 0))'
drv 1 "setmix" > /dev/null
# In Disable, Left Ctrl with a notch down turns the stairs a quarter counterclockwise, to face south, and the hotbar
# slot stays; back in Line, they keep the turn. Switching to stone and back to the stairs drops it.
drv 1 "ebmode DISABLED" "cmd tp @s -58.5 -58 -34.5 180 30" "wait 20" > /dev/null
hold_ctrl
xdo 1 click 5
check "in Disable, Left Ctrl with a notch down turns the stairs to face south, and the hotbar slot stays" eval \
	'turn_is "facing=south,half=bottom" && [[ $(val 1 slot) == "4 minecraft:oak_stairs" ]]'
check "and Disable's line shows there in place of the build modes' line" eval \
	'[[ $(val 1 modehud) == "[Right-click: place][Scroll: turn][Middle-click: flip]" ]]'
let_go_ctrl
drv 1 "ebmode LINE" "wait 5" > /dev/null
check "back in Line, the stairs keep the turn made in Disable" turn_is "facing=south,half=bottom"
press 1 7
press 1 6
check "switching to stone and back to the stairs drops it" turn_is none
aim 1 -54 -60 -36
# The build modes' line in Line, word for word (each mode but Single starts placing and erasing), before the first
# click, locked, during a shape drawn with the place click, and during one drawn with the break click, locked or not.
# Locked before the first click, the position stays locked through cancelling either shape.
line_start="[Right-click: start placing][Left-click: start erasing]"
line_end="[H: Lock (Off)][Esc: exit mode]"
line_nudge="[Arrow keys + Page Up/Down: nudge]"
check "in Line with stairs in hand, the line starts placing and erasing, turns or flips, locks and exits the mode" eval \
	'[[ $(val 1 modehud) == "$line_start[Hold Left Control: turn or flip]$line_end" ]]'
line_shot build-line-line
press 1 h
check "locked, it names the nudges and the lock on" eval '[[ $(val 1 lock) == "locked "+([-0-9]),+([-0-9]),+([-0-9])" active" ]] &&
	[[ $(val 1 modehud) == "[Right-click: start placing]$line_nudge[Left-click: start erasing][Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]" ]]'
line_shot build-line-line-locked
use_click 1
aim 1 -51 -60 -36
check "during a Line drawn with the place click, the far end's step starts unlocked: the turning control and the lock" \
	eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 modehud) == "[Hold Left Control: turn or flip][H: Lock (Off)]" ]]'
line_shot build-line-shape
press 1 h
check "locked during it, only the nudges, the turning control and the lock" eval \
	'[[ $(val 1 modehud) == "$line_nudge[Hold Left Control: turn or flip][H: Lock (On)]" ]]'
line_shot build-line-shape-locked
attack_click 1
check "a left click cancels the shape, and the locked line shows again" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 lock) == "locked "+([-0-9]),+([-0-9]),+([-0-9])" active" ]] && [[ $(val 1 modehud) == "[Right-click: start placing]"*"[H: Lock (On)][Esc: exit mode]" ]]'
attack_click 1
aim 1 -51 -60 -36
check "during a Line drawn with the break click, the lock alone" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 modehud) == "[H: Lock (Off)]" ]]'
line_shot build-line-break
press 1 h
check "locked during it, only the nudges and the lock" eval '[[ $(val 1 modehud) == "$line_nudge[H: Lock (On)]" ]]'
line_shot build-line-break-locked
use_click 1
check "a right click cancels it, erasing nothing, and the locked line before the first click shows again" eval \
	'drv 1 "wait 10" > /dev/null && [[ $(val 1 lock) == "locked "+([-0-9]),+([-0-9]),+([-0-9])" active" ]] &&
	[[ $(val 1 modehud) == "[Right-click: start placing]"*"[H: Lock (On)][Esc: exit mode]" ]]'
press 1 h
check "and H unlocks it: the line before the first click" eval '[[ $(val 1 lock) == "unlocked active" ]] &&
	[[ $(val 1 modehud) == "$line_start[Hold Left Control: turn or flip]$line_end" ]]'
aim 1 -54 -60 -36
# The build mode's name beside the hotbar and the held item's name.
# item_name_above <scale>: the held item's name, Oak Stairs, ends 2 GUI pixels above the top of the controls' highest
# row.
item_name_above() {
	local name lines
	name=$(val 1 "drawnbox Oak Stairs")
	lines=$(val 1 modebox)
	echo "  item name: $name | controls $lines | rows $(val 1 modewrap)" >> /out/checks.log
	[[ $name != none ]] && (($(cut -d" " -f4 <<< "$name") == $(cut -d" " -f2 <<< "$lines") - 2 * $1))
}
# switch_back: from the stone and back to the stairs, so the game shows the stairs' name again for 2 seconds.
switch_back() {
	press 1 7
	press 1 6
}
# Choosing a mode in the radial menu leaves its name out of the action bar (hook E35); the action bar's last text has
# gone after 3 seconds.
drv 1 "wait 70" > /dev/null
radial_open
xdo 1 mousemove --window %w $(val 1 "ringat WALL")
drv 1 "wait 5" > /dev/null
radial_let_go
check "choosing Wall in the radial menu puts no name in the action bar" eval \
	'[[ $(val 1 ebmode) == WALL ]] && drv 1 "wait 5" > /dev/null && [[ $(val 1 actionbar) == none ]]'
check "and the name Wall shows left of the hotbar" mode_name_at Wall 232
for mode in "SINGLE Single" "FLOOR Floor" "CUBE Cube" "DIAGONAL_LINE Diagonal Line" "DIAGONAL_WALL Diagonal Wall" \
	"SLOPE_FLOOR Slope Floor" "CIRCLE Circle" "CYLINDER Cylinder" "SPHERE Sphere" "LINE Line"; do
	drv 1 "ebmode ${mode%% *}" "wait 5" > /dev/null
	check "in ${mode#* }, the name ${mode#* } shows there" mode_name_at "${mode#* }" 232
done
drv 1 "ebmode DISABLED" "wait 5" > /dev/null
check "in Disable, no name shows" eval '[[ $(val 1 "drawnbox Disable") == none ]]'
# The copy at the aim in Disable shows its name too, "Paste", which a left click puts away, leaving no name.
paste_button
check "in Disable, the copy at the aim shows the name Paste left of the hotbar" eval \
	'drv 1 "wait 5" > /dev/null && copy_is 1 "^at " && mode_name_at Paste 232'
line_shot paste-disable
attack_click 1
check "and a left click puts the copy away, leaving no name in Disable" eval \
	'drv 1 "wait 10" > /dev/null && copy_is 1 "^away " && [[ $(val 1 "drawnbox Paste") == none ]]'
drv 1 "ebmode LINE" "cmd item replace entity @s weapon.offhand with minecraft:shield" "wait 5" > /dev/null
check "with a shield in the off hand, the name ends 4 left of its slot" mode_name_at Line 174
drv 1 "wait 210" "notoasts" "wait 5" "shot mode-name-shield-854x480" "window 1920 1080 2" "wait 20" "notoasts" \
	"shot mode-name-shield-1920x1080" "window 854 480 2" "wait 20" "cmd item replace entity @s weapon.offhand with air" \
	"wait 5" > /dev/null
echo "  mode-name-shield: name $(val 1 "drawnbox Line")" >> /out/checks.log
check "without it, the name ends 4 left of the hotbar again" mode_name_at Line 232
# The held item's name (hook M8) in survival mode, where the game draws it 59 above the screen's bottom, over the
# build modes' line (Geo's image 1); then in creative mode, 45 above it, under Single's line.
drv 1 "cmd gamemode survival" "wait 20" > /dev/null
switch_back
check "in survival mode, the stairs' name ends 2 above Line's line" item_name_above 2
drv 1 "wait 210" > /dev/null
switch_back
drv 1 "notoasts" "wait 2" "shot item-name-line-854x480" > /dev/null
echo "  item-name-line-854x480: name $(val 1 "drawnbox Oak Stairs") | controls $(val 1 modebox)" >> /out/checks.log
drv 1 "window 1920 1080 2" "wait 20" > /dev/null
switch_back
drv 1 "notoasts" "wait 2" "shot item-name-line-1920x1080" > /dev/null
echo "  item-name-line-1920x1080: name $(val 1 "drawnbox Oak Stairs") | controls $(val 1 modebox)" >> /out/checks.log
drv 1 "window 854 480 2" "cmd gamemode creative" "ebmode SINGLE" "wait 20" > /dev/null
switch_back
check "in creative mode, it ends 2 above Single's line" item_name_above 2
drv 1 "ebmode LINE" "cmd tp @s -58.5 -58 -34.5 180 30" "wait 20" > /dev/null
aim 1 -54 -60 -36
hold_ctrl
check "with Left Ctrl held, to scroll to turn and middle-click to flip" eval \
	'[[ $(val 1 modehud) == "$line_start[Scroll: turn][Middle-click: flip]$line_end" ]]'
line_shot build-line-line-held
xdo 1 click 4
check "turned, it stays the same, with no label" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 modehud) == "$line_start[Scroll: turn][Middle-click: flip]$line_end" ]]'
let_go_ctrl
check "and so it does without Left Ctrl" eval \
	'[[ $(val 1 modehud) == "$line_start[Hold Left Control: turn or flip]$line_end" ]]'
line_shot build-line-line-turned-1920x1080-gui4
line_shot build-line-line-turned-854x480-gui2 854 480 2
press 1 8
check "for a chest, which turns but doesn't flip, it names only the turn" eval \
	'[[ $(val 1 modehud) == "$line_start[Hold Left Control: turn]$line_end" ]]'
hold_ctrl
check "with Left Ctrl held too" eval '[[ $(val 1 modehud) == "$line_start[Scroll: turn]$line_end" ]]'
let_go_ctrl
press 1 9
check "for an oak slab, which flips but doesn't turn, only the flip" eval \
	'[[ $(val 1 modehud) == "$line_start[Hold Left Control: flip]$line_end" ]]'
hold_ctrl
check "with Left Ctrl held too" eval '[[ $(val 1 modehud) == "$line_start[Middle-click: flip]$line_end" ]]'
xdo 1 click 2
check "the middle click with Left Ctrl held flips the slab to its top half" turn_is "type=top"
let_go_ctrl
check "a Floor of the flipped slabs has every slab's top half" turned_shape FLOOR -46.5 -50.5 -49 -61 -58 -45 -59 -54 \
	"oak_slab[type=top," "-48 -60 -55" "-46 -60 -57"
drv 1 "ebmode LINE" "cmd tp @s -58.5 -58 -34.5 180 30" "wait 20" > /dev/null
aim 1 -54 -60 -36
press 1 7
check "for stone, which neither turns nor flips, no turning control" eval \
	'[[ $(val 1 modehud) == "$line_start$line_end" ]]'
line_shot build-line-line-stone
drv 1 "cmd item replace entity @s hotbar.6 with minecraft:stick" "wait 5" > /dev/null
check "with a stick in hand, no line shows" eval '[[ $(val 1 modehud) == hidden ]]'
drv 1 "cmd item replace entity @s hotbar.6 with minecraft:stone" > /dev/null
press 1 5
paste_button
check "with the copy at the aim, the copy's line shows in its place" eval \
	'[[ $(val 1 mousemode) == COPY_AT_AIM ]] && [[ $(val 1 modehud) == "[Right-click: paste]"* ]]'
drv 1 "ebmode LINE" "wait 5" > /dev/null
check "choosing Line puts the copy away, and Line's line shows again" eval '[[ $(val 1 mousemode) == none ]] &&
	[[ $(val 1 modehud) == "$line_start[Hold Left Control: turn or flip]$line_end" ]]'
drv 1 "bindkey key.sprint key.keyboard.right.control" "wait 2" > /dev/null
check "with the sprint key on Right Control, Line's line names Right Control" eval \
	'[[ $(val 1 modehud) == "$line_start[Hold Right Control: turn or flip]$line_end" ]]'
drv 1 "bindkey key.sprint key.keyboard.left.control" "wait 2" > /dev/null
press 1 F1
check "F1 hides Line's line" eval '[[ $(val 1 modehud) == hidden ]]'
press 1 F1
check "and F1 again shows it" eval '[[ $(val 1 modehud) == "$line_start[Hold Left Control: turn or flip]$line_end" ]]'
# During a Line drawn with the break click, from the first Line's west end, the turning controls aren't active: the
# line shows only the lock, and Left Ctrl with the wheel changes the hotbar slot; a right click cancels the shape.
drv 1 "lookat -61.5 -59.5 -39.5" "wait 5" > /dev/null
attack_click 1
check "during a Line drawn with the break click, the line shows only the lock" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 modehud) == "[H: Lock (Off)]" ]]'
hold_ctrl
xdo 1 click 4
check "and Left Ctrl with the wheel changes the hotbar slot" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 slot) == "3 "* ]]'
let_go_ctrl
press 1 5
use_click 1
check "a right click cancels the shape, erasing nothing, and Line's line shows again" eval \
	'drv 1 "wait 10" > /dev/null && [[ $(val 1 "layer -62 -60 -40") == minecraft:oak_stairs ]] &&
	[[ $(val 1 modehud) == "$line_start[Hold Left Control: turn or flip]$line_end" ]]'
# Effortless Building's own line during a shape (hook E31) is drawn in Building Planner's rectangles where Effortless
# Building draws it, 58 above the screen's bottom (the driver's ebline reads what the last frame drew there), in its
# words and colours, with Esc before the click that cancels: during a Line drawn with the place click
# "[Esc/Left-click: cancel][Right-click: place]", each action in dark aqua; with a pickaxe, a tool, in hand during that
# shape, "interact" in place of "place"; during a Line drawn with the break click "[Left-click: break][Esc/Right-click:
# cancel]", each action in red. Each is shot at noon, once the chat has faded.
# ebline_shots <name>: Planner1's view as <name>-1920x1080-gui4 and <name>-854x480-gui2, each with the line's
# rectangles' window boxes in the log for the shots' outlines, and a check that they're laid out as the controls' are,
# in a centred row starting 58 above the screen's bottom.
ebline_shots() {
	local size
	for size in "1920 1080 4" "854 480 2"; do
		read -r ew eh es <<< "$size"
		drv 1 "window $ew $eh $es" "wait 20" "notoasts" "wait 5" "shot $1-${ew}x$eh-gui$es" "wait 5" > /dev/null
		echo "  $1-${ew}x$eh-gui$es: $(val 1 eblinerects)" >> /out/checks.log
		check "$1 at $ew by $eh, GUI scale $es: a rectangle a control, in a centred row starting 58 above the bottom" \
			eval 'rects_ok $ew $es eblinerects && [[ $(val 1 eblinerects | cut -d" " -f2) == $((eh - 58 * es)) ]]'
	done
	drv 1 "window 854 480 2" "wait 20" > /dev/null
}
drv 1 "cmd time set noon" "cmd item replace entity @s hotbar.6 with minecraft:iron_pickaxe" > /dev/null
aim 1 -54 -60 -36
use_click 1
aim 1 -51 -60 -36
drv 1 "wait 210" > /dev/null
check "during a Line drawn with the place click, Effortless Building's line reads [Esc/Left-click: cancel][Right-click: place]" \
	eval '[[ $(val 1 ebline) == "[Esc/Left-click: cancel][Right-click: place]" ]]'
check "each rectangle white, with its action in dark aqua" eval \
	'[[ $(val 1 eblinecolours) == "white+dark_aqua white+dark_aqua" ]]'
ebline_shots effortless-line-place
press 1 7
check "with a pickaxe in hand during that shape, it reads [Esc/Left-click: cancel][Right-click: interact]" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 ebline) == "[Esc/Left-click: cancel][Right-click: interact]" ]] &&
	[[ $(val 1 eblinecolours) == "white+dark_aqua white+dark_aqua" ]]'
ebline_shots effortless-line-interact
press 1 5
attack_click 1
check "a left click cancels the shape, and Line's line shows there again" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 ebline) != *cancel* ]] &&
	[[ $(val 1 modehud) == "$line_start[Hold Left Control: turn or flip]$line_end" ]]'
aim 1 -54 -60 -36
attack_click 1
aim 1 -51 -60 -36
check "during a Line drawn with the break click, it reads [Left-click: break][Esc/Right-click: cancel]" eval \
	'[[ $(val 1 ebline) == "[Left-click: break][Esc/Right-click: cancel]" ]]'
check "each rectangle white, with its action in red" eval '[[ $(val 1 eblinecolours) == "white+red white+red" ]]'
ebline_shots effortless-line-break
use_click 1
check "a right click cancels it, and Line's line shows there again" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 ebline) != *cancel* ]] &&
	[[ $(val 1 modehud) == "$line_start[Hold Left Control: turn or flip]$line_end" ]]'
# Esc cancels a shape under way too ("Esc"), as the line's [Esc/Left-click: cancel] says, placing nothing, pausing
# nothing and leaving Line chosen; and so it does a shape drawn with the break click, erasing nothing.
for click in use attack; do
	aim 1 -54 -60 -36
	"${click}_click" 1
	aim 1 -51 -60 -36
	check "during a Line drawn with the $click click" eval 'drv 1 "wait 5" > /dev/null && [[ $(val 1 ebline) == *cancel* ]]'
	press 1 Escape
	check "Esc cancels it, editing nothing, and Line stays chosen with its line" eval \
		'[[ $(val 1 ebline) != *cancel* ]] && [[ $(val 1 holdlog) == *" edits=0 "* ]] && [[ $(val 1 ebmode) == LINE ]] &&
		[[ $(val 1 screenclass) == none ]] && [[ $(val 1 modehud) == "$line_start[Hold Left Control: turn or flip]$line_end" ]]'
done
drv 1 "cmd item replace entity @s hotbar.6 with minecraft:stone" > /dev/null
# In Disable, the middle click with Left Ctrl held flips the stairs, from the way Minecraft would place them on the
# grass below Planner1, within the game's own reach (Effortless Building's longer reach is left out in Disable), and
# picks no block; with stone, which neither turns nor flips, it picks the aimed block. The grass is due north, so the
# stairs would face north.
drv 1 "ebmode DISABLED" "wait 5" > /dev/null
aim 1 -59 -60 -36
hold_ctrl
xdo 1 click 2
check "in Disable, the middle click with Left Ctrl held flips the stairs to their top half and picks no block" eval \
	'turn_is "facing=north,half=top" && [[ $(val 1 slot) == "4 minecraft:oak_stairs" ]]'
let_go_ctrl
press 1 7
hold_ctrl
xdo 1 click 2
check "with stone in hand, it picks the aimed block" eval 'drv 1 "wait 20" > /dev/null &&
	[[ $(val 1 slot) == *" minecraft:grass_block" ]]'
let_go_ctrl
drv 1 "ebmode SINGLE" "edit -85 -63 -60 -40 -50 -36 minecraft:air" > /dev/null
check "erasing the shapes reaches both games" eval 'drv 2 "await block -58 -60 -40 minecraft:air" \
	"await block -47 -60 -38 minecraft:air" > /dev/null && drv 1 "await block -82 -60 -48 minecraft:air" > /dev/null'

# Turning real blocks in Disable (step 18): a right click with Left Ctrl held places the real block with the turn of
# the block in hand, and the server's world, Planner2 and Planner1's own guess of the block show it turned. Each block
# goes on the grass's top, at y -60, with Planner1 standing 3 blocks south of it, facing north, so Minecraft would place
# a stair facing north, within survival's reach too. Hotbar slots 5 to 9 hold oak stairs, oak logs, torches, oak doors
# and a red bed.
# stand <x> <z> [blocks]: Planner1 on the grass 3 blocks south of x z, or that many, facing north, looking at the
# middle of the grass's top there.
stand() {
	drv 1 "cmd tp @s $(half "$1") -60 $(half $(($2 + ${3:-3}))) 180 30" "wait 10" > /dev/null
	aim 1 "$1" -60 "$2"
}
# real_has <client> <x y z> <block> <text>: that client's world has that block there, with the text in its state.
real_has() {
	drv "$1" "await real $2 $3" > /dev/null && [[ $(val "$1" "realstate $2") == *"$4"* ]]
}
# server_has <x y z> <block state> <tag> [seconds]: the server's own world has that block there, as /execute if block
# finds it; run by Planner1, who then says the tag, which the server's log shows within the seconds (20 unless given).
server_has() {
	drv 1 "cmd execute if block $1 $2 run say $3" > /dev/null && wait_for /out/server.log "$3" "${4:-20}"
}
# no_flicker <block> <text>: Planner1's own block, read at every tick from just before the click (the driver's
# watchblock), was air until the click and from then on that block with the text in its state, with nothing between.
no_flicker() {
	local runs re="^([0-9]+x Block\{minecraft:air\} \| )?[0-9]+x Block\{$1\}\[[^|]*$2[^|]*\$"
	runs=$(val 1 watched)
	echo "  watched: $runs" >> /out/checks.log
	[[ $runs =~ $re ]]
}
# ctrl_click: a right click with Left Ctrl held, which hold_ctrl put down, and Left Ctrl let go after it.
ctrl_click() {
	use_click 1
	let_go_ctrl
}
# disable_round <game mode> <z>: the checks along the row at z, in that game mode.
disable_round() {
	local mode=$1 z=$2 count
	drv 1 "cmd gamemode $mode" "cmd item replace entity @s hotbar.4 with minecraft:oak_stairs 64" \
		"cmd item replace entity @s hotbar.5 with minecraft:oak_log 64" \
		"cmd item replace entity @s hotbar.6 with minecraft:torch 64" \
		"cmd item replace entity @s hotbar.7 with minecraft:oak_door 64" \
		"cmd item replace entity @s hotbar.8 with minecraft:red_bed" "cmd setblock -42 -60 $((z - 1)) minecraft:stone" \
		"cmd setblock -38 -60 $z minecraft:stone" "ebmode DISABLED" "wait 20" > /dev/null
	press 1 7
	press 1 5
	stand -54 "$z"
	hold_ctrl
	xdo 1 click 4
	check "in $mode, in Disable, Left Ctrl and a notch up turn the oak stairs to face east" \
		turn_is "facing=east,half=bottom"
	drv 1 "watchblock -54 -60 $z" > /dev/null
	ctrl_click
	check "a right click with Left Ctrl held places a real stair facing east" \
		real_has 1 "-54 -60 $z" minecraft:oak_stairs "facing=east,"
	check "the server's world has it facing east" server_has "-54 -60 $z" "minecraft:oak_stairs[facing=east]" \
		"turned-stair-$mode"
	check "Planner2 sees it facing east" real_has 2 "-54 -60 $z" minecraft:oak_stairs "facing=east,"
	check "Planner1's own block faces east at every tick from the click, with no flicker" no_flicker \
		minecraft:oak_stairs "facing=east,"
	stand -52 "$z"
	use_click 1
	check "without Left Ctrl, the stair faces north, as Minecraft places it, and the stairs keep the turn" eval \
		'real_has 1 "-52 -60 $z" minecraft:oak_stairs "facing=north," && turn_is "facing=east,half=bottom" &&
		server_has "-52 -60 $z" "minecraft:oak_stairs[facing=north]" "plain-stair-$mode"'
	press 1 6
	stand -50 "$z"
	hold_ctrl
	xdo 1 click 2
	xdo 1 click 4
	check "an oak log flipped onto its side and turned a quarter lies east to west" turn_is "axis=x"
	ctrl_click
	check "placed on the grass's top with Left Ctrl held, it lies east to west" eval \
		'real_has 1 "-50 -60 $z" minecraft:oak_log "axis=x" &&
		server_has "-50 -60 $z" "minecraft:oak_log[axis=x]" "turned-log-$mode"'
	stand -48 "$z"
	use_click 1
	check "where Minecraft, without Left Ctrl, stands it up" real_has 1 "-48 -60 $z" minecraft:oak_log "axis=y"
	press 1 5
	# 4 blocks south, so the stairs the held button adds toward Planner1 have room.
	stand -46 "$z" 4
	hold_ctrl
	xdo 1 click 4
	drv 1 "wait 5" "holdreset" > /dev/null
	xdo 1 mousedown 3
	drv 1 "wait 20" > /dev/null
	xdo 1 mouseup 3
	let_go_ctrl
	check "the right button held with Left Ctrl for a second places 4 turned stairs or more, all facing east" eval \
		'drv 1 "wait 10" > /dev/null && count=$(val 1 "realstates -47 -60 $((z - 2)) -45 -56 $((z + 3)) facing=east,") &&
		echo "  held right button in $mode: $count" >> /out/checks.log && ((${count% *} >= 4)) &&
		[[ ${count% *} == "${count#* }" ]]'
	press 1 7
	drv 1 "cmd tp @s -41.5 -60 $(half $((z + 2))) 180 30" "wait 10" "lookat -41.5 -59.5 $(half $((z - 1)))" \
		"wait 5" > /dev/null
	count=$(val 1 handcount)
	hold_ctrl
	xdo 1 click 4
	check "a torch aimed at a wall turns as the wall torch it places, away from the wall" turn_is "facing=west"
	ctrl_click
	check "it places nothing, on both sides, and the player keeps the torch" eval 'drv 1 "wait 20" > /dev/null &&
		[[ $(val 1 "real -42 -60 $z") == minecraft:air && $(val 1 "real -42 -60 $((z - 1))") == minecraft:stone ]] &&
		! server_has "-42 -60 $z" minecraft:wall_torch "torch-placed-$mode" 5 &&
		[[ $(val 1 handcount) == "$count" && $(val 1 slot) == *" minecraft:torch" ]]'
	press 1 9
	stand -39 "$z"
	hold_ctrl
	xdo 1 click 4
	check "a red bed turns to face east" turn_is "facing=east"
	ctrl_click
	check "turned toward a stone wall, it places nothing and leaves the wall, and the player keeps the bed" eval \
		'drv 1 "wait 20" > /dev/null && [[ $(val 1 "real -39 -60 $z") == minecraft:air ]] &&
		[[ $(val 1 "real -39 -60 $((z - 1))") == minecraft:air && $(val 1 "real -38 -60 $z") == minecraft:stone ]] &&
		! server_has "-39 -60 $z" minecraft:red_bed "bed-placed-$mode" 5 && [[ $(val 1 slot) == *" minecraft:red_bed" ]]'
}
disable_round creative -31
# The turned stair at -54 and the log at -50, shot from 4 blocks south of the point between them, with each one's box in
# the window logged for its outline.
drv 1 "cmd weather clear" "cmd tp @s -51.5 -60 -26.5 180 25" "wait 210" > /dev/null
for size in "854 480" "1920 1080"; do
	read -r w h <<< "$size"
	drv 1 "window $w $h 2" "wait 20" "notoasts" "wait 5" "shot disable-turned-${w}x$h" "wait 20" > /dev/null
	echo "  disable-turned-${w}x$h: stair $(val 1 "screenbox -54 -60 -31 -54 -60 -31") |" \
		"log $(val 1 "screenbox -50 -60 -31 -50 -60 -31")" >> /out/checks.log
done
drv 1 "window 854 480 2" "wait 20" > /dev/null
# Along z -37: a turned stair placed in front of a stair facing north joins it as Minecraft's own does: the same shapes
# as a stair Minecraft places facing east, with Planner1 facing east, in front of another stair facing north.
drv 1 "cmd setblock -54 -60 -37 minecraft:oak_stairs[facing=north]" \
	"cmd setblock -57 -60 -37 minecraft:oak_stairs[facing=north]" > /dev/null
press 1 5
stand -55 -37
hold_ctrl
xdo 1 click 4
drv 1 "wait 5" > /dev/null
ctrl_click
drv 1 "await real -55 -60 -37 minecraft:oak_stairs" "cmd tp @s -60.5 -60 -36.5 -90 30" "wait 10" > /dev/null
aim 1 -58 -60 -37
use_click 1
check "a turned stair placed in front of a stair facing north takes the shapes Minecraft's own stair facing east does" eval \
	'drv 1 "await real -58 -60 -37 minecraft:oak_stairs" "wait 10" > /dev/null &&
	turned="$(val 1 "realstate -55 -60 -37") $(val 1 "realstate -54 -60 -37")" &&
	own="$(val 1 "realstate -58 -60 -37") $(val 1 "realstate -57 -60 -37")" &&
	echo "  turned: $turned; own: $own" >> /out/checks.log &&
	[[ $turned == "$own" && $turned == *facing=east,* && $turned != *facing=east,half=bottom,shape=straight* ]]'
# An oak door turned to face east has both halves facing east.
press 1 8
stand -51 -37
hold_ctrl
xdo 1 click 4
check "an oak door turns to face east" turn_is "facing=east"
ctrl_click
check "placed, both its halves face east, on both sides" eval \
	'real_has 1 "-51 -60 -37" minecraft:oak_door "facing=east," && real_has 1 "-51 -59 -37" minecraft:oak_door "facing=east," &&
	server_has "-51 -59 -37" "minecraft:oak_door[facing=east,half=upper]" "turned-door"'
# A ghost stair facing east is filled by the turned real stair.
drv 1 "edit -48 -60 -37 -48 -60 -37 minecraft:oak_stairs[facing=east]" "await block -48 -60 -37 minecraft:oak_stairs" \
	> /dev/null
press 1 5
stand -48 -37
hold_ctrl
xdo 1 click 4
drv 1 "wait 5" > /dev/null
ctrl_click
check "a ghost stair facing east is filled by the turned real stair, on both clients" eval \
	'real_has 1 "-48 -60 -37" minecraft:oak_stairs "facing=east," && drv 1 "await block -48 -60 -37 minecraft:air" > /dev/null &&
	drv 2 "await block -48 -60 -37 minecraft:air" > /dev/null'
# A turn made in Line: Disable without Left Ctrl places as Minecraft does, and with it places turned. Switching to stone
# and back to the stairs drops the turn, and Left Ctrl then places as Minecraft does.
press 1 7
press 1 5
drv 1 "ebmode LINE" "wait 5" > /dev/null
stand -45 -37
hold_ctrl
xdo 1 click 4
check "with a turn made in Line" turn_is "facing=east,half=bottom"
let_go_ctrl
drv 1 "ebmode DISABLED" "wait 5" > /dev/null
use_click 1
check "Disable without Left Ctrl places as Minecraft does" real_has 1 "-45 -60 -37" minecraft:oak_stairs "facing=north,"
stand -43 -37
hold_ctrl
ctrl_click
check "and with it places turned" real_has 1 "-43 -60 -37" minecraft:oak_stairs "facing=east,"
press 1 7
press 1 5
check "switching to stone and back drops the turn" turn_is none
stand -41 -37
hold_ctrl
ctrl_click
check "and Left Ctrl then places the stair as Minecraft does" eval \
	'real_has 1 "-41 -60 -37" minecraft:oak_stairs "facing=north," &&
	server_has "-41 -60 -37" "minecraft:oak_stairs[facing=north]" "unturned-stair"'
# Along z -43: with Freecam's camera on, "Allow interaction" on and the camera 3 blocks east of Planner1, the turned
# stair goes where the camera aims and faces east.
press 1 5
stand -50 -43
hold_ctrl
xdo 1 click 4
check "with the stairs turned to face east" turn_is "facing=east,half=bottom"
let_go_ctrl
drv 1 "allowinteraction true" "freecam true" "wait 10" "camtp -46.5 -58.4 -39.5 90 30" "camlookat -49.5 -60 -42.5" \
	"wait 5" > /dev/null
hold_ctrl
ctrl_click
check "with Freecam's camera on and \"Allow interaction\" on, the turned stair goes where the camera aims, facing east" \
	eval 'real_has 1 "-50 -60 -43" minecraft:oak_stairs "facing=east," &&
	server_has "-50 -60 -43" "minecraft:oak_stairs[facing=east]" "freecam-stair"'
check "Freecam's camera and \"Allow interaction\" go off again" eval \
	'[[ $(val 1 "freecam false") == false && $(val 1 "allowinteraction false") == false ]] && drv 1 "wait 10" > /dev/null'
disable_round survival -24
# Survival stopped Planner1's flight, which the checks after these rely on; it starts again off the ground, as the game
# stops a flight that touches the ground.
drv 1 "cmd gamemode creative" "cmd fill -61 -60 -45 -36 -56 -19 minecraft:air" "ebmode SINGLE" "wait 10" \
	"cmd tp @s ~ ~10 ~" "wait 5" "fly true" "wait 5" > /dev/null
check "the blocks placed in Disable are cleared" eval '[[ $(val 1 "realstates -61 -60 -45 -36 -56 -19 x") == "0 0" ]]'

# Disable's preview and its line (step 19), along z -31 again, in creative: with Left Ctrl held and a block that turns
# or flips, Effortless Building's preview draws the block a right click would place, in the state it would place, red
# where the turned block can't stay; Disable's line names the controls. Hotbar slots 4 to 9 hold a stick, oak stairs,
# torches, a chest, oak slabs and stone; stone stands at -42 -60 -32 for the torch, and short grass at -50 -60 -31.
# disable_line_is <text>: Disable's line, word for word, once the preview has followed the last change.
disable_line_is() {
	drv 1 "wait 10" > /dev/null && [[ $(val 1 modehud) == "$1" ]]
}
# preview_is <positions> [<state>]: the preview draws those positions, or none, and the first in a state with the text.
preview_is() {
	drv 1 "wait 10" > /dev/null && [[ $(val 1 preview) == "$1" ]] &&
		{ [[ -z ${2:-} ]] || [[ $(val 1 "previewstate ${1//,/ }") == *"$2"* ]]; }
}
# disable_shots <name> <x y z>: Planner1's view with the preview at 854 by 480 and 1920 by 1080, GUI scale 2, with the
# box of the block at x y z in the window logged, for outlining it.
disable_shots() {
	local size w h
	drv 1 "wait 210" > /dev/null
	for size in "854 480" "1920 1080"; do
		read -r w h <<< "$size"
		drv 1 "window $w $h 2" "wait 20" "notoasts" "wait 5" "shot $1-${w}x$h" > /dev/null
		echo "  $1-${w}x$h: block $(val 1 "screenbox $2 $2")" >> /out/checks.log
	done
	drv 1 "window 854 480 2" "wait 20" > /dev/null
}
drv 1 "cmd item replace entity @s hotbar.3 with minecraft:stick" \
	"cmd item replace entity @s hotbar.4 with minecraft:oak_stairs 64" \
	"cmd item replace entity @s hotbar.5 with minecraft:torch 64" "cmd item replace entity @s hotbar.6 with minecraft:chest 64" \
	"cmd item replace entity @s hotbar.7 with minecraft:oak_slab 64" "cmd item replace entity @s hotbar.8 with minecraft:stone 64" \
	"cmd setblock -42 -60 -32 minecraft:stone" "cmd setblock -50 -60 -31 minecraft:short_grass" "cmd weather clear" \
	"ebmode DISABLED" "wait 20" > /dev/null
press 1 9
press 1 5
stand -54 -31
check "in Disable with oak stairs, Disable's line says to hold Left Control to turn or flip" \
	disable_line_is "[Hold Left Control: turn or flip]"
check "and without Left Ctrl, no preview shows" preview_is none
line_shot disable-line-hold
hold_ctrl
check "with Left Ctrl held, the line names the right click that places, the wheel and the middle click" \
	disable_line_is "[Right-click: place][Scroll: turn][Middle-click: flip]"
check "and the preview draws the stair where a right click would place it, facing north, as Minecraft would" \
	preview_is "-54,-60,-31" "minecraft:oak_stairs[facing=north,half=bottom,"
line_shot disable-line-held
xdo 1 click 4
check "a notch up turns it, and the line stays the same, with no label" \
	disable_line_is "[Right-click: place][Scroll: turn][Middle-click: flip]"
check "and the preview draws the stair facing east, not red" eval \
	'preview_is "-54,-60,-31" "minecraft:oak_stairs[facing=east,half=bottom," && [[ $(val 1 previewred) == none ]]'
line_shot disable-line-turned
disable_shots disable-preview-stair "-54 -60 -31"
drawn=$(val 1 "previewstate -54 -60 -31")
ctrl_click
check "the stair placed matches the preview" eval 'drv 1 "await real -54 -60 -31 minecraft:oak_stairs" > /dev/null &&
	placed=$(val 1 "realstate -54 -60 -31") && placed=${placed#Block\{} && placed=${placed/\}/} &&
	echo "  drawn: $drawn; placed: $placed" >> /out/checks.log && [[ $placed == "$drawn" ]]'
check "without Left Ctrl, the line names Left Ctrl, since a right click places as Minecraft does" \
	disable_line_is "[Hold Left Control: turn or flip]"
# Short grass, which a block replaces, aimed at on its south side: the preview takes the grass's place, not the space
# south of it.
drv 1 "cmd tp @s -49.5 -60 -27.5 180 30" "wait 10" "lookat -49.5 -59.6 -30.5" "wait 5" > /dev/null
hold_ctrl
check "aimed at short grass, which a block replaces, the preview takes the grass's place" preview_is "-50,-60,-31"
let_go_ctrl
# A torch aimed at the stone's south side places a wall torch: Disable's line shows only with Left Ctrl held, since
# the wall torch hangs on the stone; turned away from the stone, it can't stay, and the preview and the line show red.
press 1 6
drv 1 "cmd tp @s -41.5 -60 -28.5 180 30" "wait 10" "lookat -41.5 -59.5 -31.5" "wait 5" > /dev/null
check "with a torch aimed at a wall, no line shows without Left Ctrl" disable_line_is hidden
hold_ctrl
check "with Left Ctrl held, Disable's line names the right click and the wheel" \
	disable_line_is "[Right-click: place][Scroll: turn]"
check "and the preview draws the wall torch on the stone, not red" eval \
	'preview_is "-42,-60,-31" "minecraft:wall_torch[facing=south]" && [[ $(val 1 previewred) == none ]]'
xdo 1 click 4
check "turned away from the stone, the preview draws the torch red" eval \
	'preview_is "-42,-60,-31" "minecraft:wall_torch[facing=west]" && [[ $(val 1 previewred) == "-42,-60,-31" ]]'
check "and the line says in red that it can't be placed" disable_line_is "[Can't place here][Scroll: turn]"
line_shot disable-line-red 1920 1080 4 "Can't place here"
check "the reason's rectangle is red, the other white" eval '[[ $(val 1 modecolours) == "red white" ]]'
disable_shots disable-preview-torch-red "-42 -60 -31"
let_go_ctrl
check "without Left Ctrl, no line shows, though the torch is turned" disable_line_is hidden
# Beyond Minecraft's reach (5 blocks in creative), and aimed at the sky, no preview shows.
press 1 5
stand -46 -31 8
hold_ctrl
check "beyond Minecraft's reach, no preview shows" preview_is none
drv 1 "cmd tp @s -45.5 -60 -27.5 180 -60" "wait 10" > /dev/null
check "aimed at the sky, no preview shows" preview_is none
let_go_ctrl
# With Freecam's camera on, 3 blocks east of Planner1, aimed at the grass at -46 -61 -31: with "Allow interaction" off,
# no preview shows; with it on, it shows while Planner1 is within reach of the grass, and not once Planner1 is 16 blocks
# south of it, the camera staying where it is.
stand -46 -31
drv 1 "freecam true" "wait 10" "camtp -42.5 -58.4 -27.5 90 30" "camlookat -45.5 -60 -30.5" \
	"wait 5" > /dev/null
hold_ctrl
check "with Freecam's camera on and \"Allow interaction\" off, no preview shows" preview_is none
drv 1 "allowinteraction true" > /dev/null
check "with \"Allow interaction\" on, the preview shows where the camera aims" preview_is "-46,-60,-31"
drv 1 "cmd tp @s -45.5 -60 -15.5 180 30" "wait 10" > /dev/null
check "and not once Planner1 is beyond reach of the aimed block" preview_is none
let_go_ctrl
check "Freecam's camera and \"Allow interaction\" go off again" eval \
	'[[ $(val 1 "freecam false") == false && $(val 1 "allowinteraction false") == false ]] && drv 1 "wait 10" > /dev/null'
# With Effortless Building's mirror on, Disable's preview draws the one block a right click places.
stand -46 -31
drv 1 "ebmirror -50" "wait 5" > /dev/null
hold_ctrl
check "with Effortless Building's mirror on, the preview draws one block" preview_is "-46,-60,-31"
let_go_ctrl
drv 1 "ebarray 0" "wait 5" > /dev/null
# Each block's line, word for word: a chest only turns, a slab only flips, and stone and a stick have none.
press 1 7
check "with a chest, which only turns, the line says to hold Left Control to turn" \
	disable_line_is "[Hold Left Control: turn]"
hold_ctrl
check "and with it held, to place and scroll to turn" disable_line_is "[Right-click: place][Scroll: turn]"
let_go_ctrl
press 1 8
check "with an oak slab, which only flips, the line says to hold Left Control to flip" \
	disable_line_is "[Hold Left Control: flip]"
hold_ctrl
check "and with it held, to place and middle-click to flip" disable_line_is "[Right-click: place][Middle-click: flip]"
let_go_ctrl
press 1 9
check "with stone, no line shows" disable_line_is hidden
hold_ctrl
check "nor with Left Ctrl held, and no preview" eval 'disable_line_is hidden && preview_is none'
let_go_ctrl
press 1 4
check "with a stick, no line shows" disable_line_is hidden
press 1 5
drv 1 "bindkey key.sprint key.keyboard.right.control" "wait 2" > /dev/null
check "with the sprint key on Right Control, Disable's line names Right Control" \
	disable_line_is "[Hold Right Control: turn or flip]"
drv 1 "bindkey key.sprint key.keyboard.left.control" "wait 2" > /dev/null
press 1 F1
check "F1 hides Disable's line" disable_line_is hidden
press 1 F1
check "and F1 again shows it" disable_line_is "[Hold Left Control: turn or flip]"
drv 1 "cmd fill -61 -60 -45 -36 -56 -19 minecraft:air" "ebmode SINGLE" "wait 10" \
	"cmd item replace entity @s hotbar.3 with minecraft:air" > /dev/null
check "the blocks placed for Disable's preview are cleared" eval \
	'[[ $(val 1 "realstates -61 -60 -45 -36 -56 -19 x") == "0 0" ]]'

# The locked block's colours and Single's line (step 20), along z -31 again, in Single mode: the locked block draws
# Building Planner's box, blue, yellow over a ghost block it would replace, and red outside the world, where a click
# places nothing; Single's line names the controls. Hotbar slots 4 to 8 hold stone, oak stairs, a chest, oak slabs and
# a stick. single_line_is <text>: Single's line, word for word.
single_line_is() {
	drv 1 "wait 5" > /dev/null && [[ $(val 1 modehud) == "$1" ]]
}
single_end="[H: Lock (Off)][Esc: exit mode]"
# single_shot <name> <width> <height> <x1 y1 z1> <x2 y2 z2>: Planner1's view at that size, GUI scale 2, as
# <name>-<width>x<height>, with the window boxes of the blocks from x1 y1 z1 to x2 y2 z2 and of the line logged, for
# outlining them.
single_shot() {
	drv 1 "wait 210" "window $2 $3 2" "wait 20" "notoasts" "wait 5" "shot $1-$2x$3" "wait 20" > /dev/null
	echo "  $1-$2x$3: blocks $(val 1 "screenbox $4 $5") | line $(val 1 modebox)" >> /out/checks.log
	drv 1 "window 854 480 2" "wait 20" > /dev/null
}
# The flat world's animals would push Planner1 off the aim during the shots' waits, so they're killed first.
drv 1 "$area_clear" "edit -62 -60 -33 -48 -59 -29 minecraft:air" \
	"cmd item replace entity @s hotbar.3 with minecraft:stone 64" \
	"cmd item replace entity @s hotbar.4 with minecraft:oak_stairs 64" \
	"cmd item replace entity @s hotbar.5 with minecraft:chest 64" \
	"cmd item replace entity @s hotbar.6 with minecraft:oak_slab 64" \
	"cmd item replace entity @s hotbar.7 with minecraft:stick" "ebarray 0" "wait 10" > /dev/null
press 1 4
press 1 5
stand -50 -31
check "in Single mode with oak stairs, the block follows the aim, unlocked and not turned" eval \
	'[[ $(val 1 lock) == "unlocked active" ]] && [[ $(val 1 heldturn) == none ]]'
check "following the aim, no box shows" eval '[[ $(val 1 copycolour) == none ]]'
check "Single's line names the clicks, holding Left Control to turn or flip, the lock key and Esc" \
	single_line_is "[Right-click: place][Left-click: erase][Hold Left Control: turn or flip]$single_end"
line_shot single-line-following
hold_ctrl
check "with Left Ctrl held, the wheel and the middle click take its place" \
	single_line_is "[Right-click: place][Left-click: erase][Scroll: turn][Middle-click: flip]$single_end"
line_shot single-line-held
xdo 1 click 4
check "a notch up turns the stairs, and the line stays the same, with no label" single_line_is \
	"[Right-click: place][Left-click: erase][Scroll: turn][Middle-click: flip]$single_end"
let_go_ctrl
check "and without Left Ctrl, it names Left Ctrl, with no label" single_line_is \
	"[Right-click: place][Left-click: erase][Hold Left Control: turn or flip]$single_end"
line_shot single-line-turned
press 1 6
check "for a chest, which turns but doesn't flip, it names only the turn" \
	single_line_is "[Right-click: place][Left-click: erase][Hold Left Control: turn]$single_end"
line_shot single-line-chest
hold_ctrl
check "with Left Ctrl held too" \
	single_line_is "[Right-click: place][Left-click: erase][Scroll: turn]$single_end"
let_go_ctrl
press 1 7
check "for an oak slab, which flips but doesn't turn, only the flip" \
	single_line_is "[Right-click: place][Left-click: erase][Hold Left Control: flip]$single_end"
line_shot single-line-slab
hold_ctrl
check "with Left Ctrl held too" \
	single_line_is "[Right-click: place][Left-click: erase][Middle-click: flip]$single_end"
let_go_ctrl
press 1 4
check "for stone, which neither turns nor flips, no turning part" \
	single_line_is "[Right-click: place][Left-click: erase]$single_end"
line_shot single-line-stone
# Locked over the grass, the block is blue; nudged into a ghost gold block it's yellow and its click places, replacing
# the gold, after which the same stair there leaves it blue; nudged below the world's bottom (-64) it's red, and its
# click sends no edit.
press 1 5
check "back on the stairs, the turn is gone" eval '[[ $(val 1 heldturn) == none ]]'
drv 1 "edit -50 -60 -32 -50 -60 -32 minecraft:gold_block" > /dev/null
press 1 h
check "H locks the block, and Building Planner draws its box blue over the grass" eval \
	'[[ $(val 1 lock) == "locked -50,-60,-31 active" ]] && drv 1 "await copycolour BLUE" > /dev/null'
locked_line="[Arrow keys + Page Up/Down: nudge][Left-click: erase][Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]"
check "the locked line names the nudges and the lock on" single_line_is "[Right-click: place]$locked_line"
line_shot single-line-locked
line_shot single-line-locked-854x480-gui2 854 480 2
single_shot single-locked 854 480 "-50 -60 -31" "-50 -60 -31"
single_shot single-locked 1920 1080 "-50 -60 -31" "-50 -60 -31"
press 1 Up
check "nudged into a ghost gold block, it's yellow" eval \
	'[[ $(val 1 lock) == "locked -50,-60,-32 active" ]] && drv 1 "await copycolour YELLOW" > /dev/null'
use_click 1
check "and its click still places, replacing the gold with a stair, and the lock ends with no box" eval \
	'drv 1 "await block -50 -60 -32 minecraft:oak_stairs" > /dev/null && [[ $(val 1 holdlog) == *" edits=1 "* ]] &&
	[[ $(val 1 lock) == "unlocked active" ]] && [[ $(val 1 copycolour) == none ]]'
press 1 h
press 1 Up
check "locked again and nudged onto the same stair, it's blue" eval \
	'[[ $(val 1 lock) == "locked -50,-60,-32 active" ]] && drv 1 "await copycolour BLUE" > /dev/null'
for _ in 1 2 3 4; do press 1 Next; done
check "nudged down to the world's bottom, it's still blue" eval \
	'[[ $(val 1 lock) == "locked -50,-64,-32 active" ]] && drv 1 "await copycolour BLUE" > /dev/null'
press 1 Next
check "one block lower, below the build height, it's red" eval \
	'[[ $(val 1 lock) == "locked -50,-65,-32 active" ]] && drv 1 "await copycolour RED BUILD_HEIGHT" > /dev/null'
check "and the line says why in red, in place of the right click" \
	single_line_is "[Can't place: outside the world]$locked_line"
line_shot single-line-red 1920 1080 4 "Can't place: outside the world"
use_click 1
check "its click places nothing and sends no edit, and, finishing the shape, ends the lock with no box" eval \
	'drv 1 "wait 20" > /dev/null && [[ $(val 1 holdlog) == *" edits=0 "* ]] && [[ $(val 1 lock) == "unlocked active" ]] &&
	[[ $(val 1 copycolour) == none ]]'
# The keys as the player rebinds them: the lock key to K, the mode key to G, the nudge forward to I and the sprint key
# to Right Control.
rebind key.keyboard.k key.keyboard.g key.keyboard.i key.keyboard.right.control
check "with the keys rebound, Single's line names them as bound" \
	single_line_is "[Right-click: place][Left-click: erase][Hold Right Control: turn or flip][K: Lock (Off)][Esc: exit mode]"
press 1 k
check "and locked, the nudges one by one" single_line_is "[Right-click: place][I/Down Arrow/Left Arrow/Right Arrow + Page Up/Down: nudge][Left-click: erase][Hold Right Control: turn or flip][K: Lock (On)][Esc: exit mode]"
press 1 k
rebind key.keyboard.h key.keyboard.r key.keyboard.up key.keyboard.left.control
check "with the keys back, K has unlocked it" eval '[[ $(val 1 lock) == "unlocked active" ]]'
press 1 8
check "with a stick in hand, no line shows" single_line_is hidden
press 1 5
paste_button
check "with the copy at the aim, the copy's line shows" eval \
	'[[ $(val 1 mousemode) == COPY_AT_AIM ]] && [[ $(val 1 modehud) == "[Right-click: paste]"* ]]'
drv 1 "ebmode LINE" "wait 5" > /dev/null
check "in Line mode, Line's line shows" eval '[[ $(val 1 mousemode) == none ]] &&
	single_line_is "$line_start[Hold Left Control: turn or flip]$line_end"'
drv 1 "ebmode SINGLE" "wait 5" > /dev/null
press 1 F1
check "F1 hides Single's line" single_line_is hidden
press 1 F1
check "and F1 again shows it" \
	single_line_is "[Right-click: place][Left-click: erase][Hold Left Control: turn or flip]$single_end"
drv 1 "edit -62 -60 -33 -48 -59 -29 minecraft:air" "cmd item replace entity @s hotbar.3 with minecraft:air" > /dev/null
check "the ghost blocks placed for the colours are erased" drv 1 "await block -50 -60 -32 minecraft:air" \
	"await block -56 -60 -31 minecraft:air"

drv 1 "cmd item replace entity @s hotbar.4 with minecraft:air" "cmd item replace entity @s hotbar.5 with minecraft:air" \
	"cmd item replace entity @s hotbar.6 with minecraft:air" "cmd item replace entity @s hotbar.7 with minecraft:air" \
	"cmd item replace entity @s hotbar.8 with minecraft:air" > /dev/null
press 1 $((first_slot + 1))
drv 1 "cmd item replace entity @s weapon.mainhand with $main_hand" "cmd tp @s 8.5 -56 9.5 0 30" \
	"cmd tp Planner2 101.5 0 26.5 0 30" "wait 40" > /dev/null
check "Planner1 flies again" eval '[[ $(val 1 flying) == true ]]'

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
# Each of the three rows sits on its own dark background, the rows' backgrounds meeting.
backgrounds=$(val 1 boxlistbackgrounds)
echo "  the wrapped list's backgrounds: $backgrounds" >> /out/checks.log
check "each row of the list sits on the dark background" eval \
	'[[ $backgrounds == "442 222 846 262 | 442 262 846 302 | 442 302 846 342" ]]'
drv 1 "wait 200" > /dev/null
shot 1 box-list-wrapped
drv 1 "window 1920 1080 2" "wait 20" > /dev/null
shot 1 box-list-wrapped-1920x1080
echo "  box-list-wrapped-1920x1080 backgrounds: $(val 1 boxlistbackgrounds)" >> /out/checks.log
drv 1 "window 854 480 2" "wait 20" > /dev/null
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
check "a box of 261 blocks a side draws red" box_is "COPY 1 120,-60,64..380,-60,64 limit SINGLE"
check "with its line saying which limit" eval \
	'[[ $(val 1 modehud) == "Too many blocks | $second_line[261 × 1 × 1]" ]]'
line_shot line-copy-too-large
check "the warning's rectangle is red, the line's white" eval \
	'[[ $(val 1 modecolours) == "red white white white" ]]'
shot 1 box-too-large
attack_click 1
check "and its click sets no corner" eval 'drv 1 "wait 5" > /dev/null &&
	box_is "COPY 1 120,-60,64..380,-60,64 limit SINGLE"'
drv 1 "lookat 300.5 -60 64.5" "wait 5" > /dev/null
check "a box over chunks the game hasn't loaded draws red" box_is "COPY 1 120,-60,64..300,-60,64 limit SINGLE"
check "with its line saying which limit" eval '[[ $(val 1 modehud) == \
	"Part of the box is too far away to read. Move closer. | $second_line[181 × 1 × 1]" ]]'
shot 1 box-not-loaded
attack_click 1
check "and its click sets no corner" eval 'drv 1 "wait 5" > /dev/null &&
	box_is "COPY 1 120,-60,64..300,-60,64 limit SINGLE"'
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
check "with its controls line" eval '[[ $(val 1 modehud) == "$first_line" ]]'
check "and the name Material list shows left of the hotbar" mode_name_at "Material list" 232
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
# The copy's mode, set to Line before the restart, starts as Off again after it.
paste_button
press 1 r
check "before the restart, R sets the copy's mode to Line" eval '[[ $(val 1 copymode) == LINE ]]'
cp /out/client1.log /out/client1-before-lists-restart.log
cp /out/server.log /out/server-before-lists-restart.log
stop_client 1
stop_server
start_server "" server-lists
start_client 1
check "after the server and Planner1's game restart, the game has the list it counted as its current list" eval \
	'drv 1 "await session" "await noscreen" "wait 20" > /dev/null && [[ $(val 1 currentlist) == "$counted_list" ]]'
check "and the copy's mode is Off again" eval '[[ $(val 1 copymode) == OFF ]]'
delete_button
check "and the Delete button starts Delete" eval '[[ $(val 1 mousemode) == DELETE ]]'
use_click 1
drv 1 "wait 10" > /dev/null
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
# place of "Export" and "Write to file", and "Close" the place of "Litematica menu", at the bottom right, 10 from the
# screen's right edge as Litematica places that button; the rest of Litematica's buttons and the multiplier stay. Shot
# at 854 by 480 and 1920 by 1080, GUI scale 2.
drv 1 "cmd tp @s 24.5 -51 -4.5 180 30" "wait 40" "fly true" > /dev/null
radial_open
radial_point "Material list"
radial_let_go
material_box
buttons_re='^Refresh,Hide available: (ON|OFF),Info HUD: (ON|OFF),Clear ignored,Clear cache,Save list,Saved lists,Close$'
# close_at <buttons or widgets> <window width> <window height>: Close's box in the list ends 20 from the window's right
# edge (10 at GUI scale 2) and in its bottom 60 pixels.
close_at() {
	local x y bw bh
	read -r x y bw bh <<< "$(sed -E 's/.*Close: ([0-9]+ [0-9]+ [0-9]+ [0-9]+).*/\1/' <<< "$1")"
	((x + bw == $2 - 20 && y + bh <= $3 && y + bh > $3 - 60))
}
for size in "854 480" "1920 1080"; do
	read -r w h <<< "$size"
	drv 1 "window $w $h 2" "wait 20" "notoasts" "wait 5" "shot material-list-buttons-${w}x$h-gui2" > /dev/null
	echo "  material-list-buttons-${w}x$h-gui2 buttons: $(val 1 malilibbuttons)" >> /out/checks.log
	check "at $w by $h, the list's screen shows Save list, Saved lists and Close, and no Export, Write to file or Litematica menu" \
		eval '[[ $(mat_labels) =~ $buttons_re ]] && [[ $(val 1 drawntexts) == *"Multiplier:"* ]]'
	check "and Close sits at the bottom right" eval 'close_at "$(val 1 malilibbuttons)" $w $h'
done
drv 1 "window 854 480 2" "wait 20" > /dev/null

# The (i) icon beside "Multiplier" shows Building Planner's text, one line a point, in place of Litematica's. Pointed
# at, it's shot at 854 by 480 and 1920 by 1080, GUI scale 2.
info_lines=("Counts the ghost blocks and real blocks in the box you drew."
	"Total: how many of each block are in the box."
	"Missing: how many are still to place, where there's air or a different block."
	"Available: how many you carry."
	"Info HUD: pins this list to the screen while you build."
	"Save list: keeps this list on the server."
	"Saved lists: opens the lists you kept."
	"The radial menu's \"Material lists\" button opens the current list again.")
# info_shown: the last frame drew each of the (i) text's lines, and none of Litematica's.
info_shown() {
	local texts line
	texts=$(val 1 drawntexts)
	for line in "${info_lines[@]}"; do
		[[ $texts == *"$line"* ]] || return 1
	done
	[[ $texts != *"Material List hotkey"* ]]
}
# The window, as large as the virtual screen at 1920 by 1080, moves to the screen's corner for that size, so the pointer
# reaches the icon at its top right; it goes back where it was after.
window_at=$(xdo 1 getwindowgeometry --shell %w | sed -nE 's/^[XY]=//p' | tr '\n' ' ')
for size in "854 480" "1920 1080"; do
	read -r w h <<< "$size"
	drv 1 "window $w $h 2" "wait 20" "notoasts" > /dev/null
	[[ $w == 1920 ]] && xdo 1 windowmove %w 0 0 > /dev/null
	xdo 1 mousemove --window %w $(val 1 infoicon) > /dev/null
	drv 1 "wait 10" "shot material-list-info-${w}x$h-gui2" > /dev/null
	echo "  material-list-info-${w}x$h-gui2 texts: $(val 1 drawntexts)" >> /out/checks.log
	check "at $w by $h, pointing at the (i) icon shows Building Planner's text, one line a point" info_shown
	xdo 1 mousemove --window %w 5 5 > /dev/null
done
if [[ -n $window_at ]]; then xdo 1 windowmove %w $window_at > /dev/null; fi
drv 1 "window 854 480 2" "wait 20" > /dev/null

# "Close" closes the list's screen as Escape does: opened from the game, back to the game.
mat_click Close
check "Close closes the list's screen, back to the game" drv 1 "await noscreen"
drv 1 "openlist" "await screen Material list for the blocks in the box" "wait 10" > /dev/null
keys 1 key Escape
check "as Escape does" drv 1 "await noscreen"
drv 1 "openlist" "await screen Material list for the blocks in the box" "wait 10" > /dev/null

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
# rows_above_close <widgets>: every row's Open button ends above Close's top.
rows_above_close() {
	local close_top lowest
	close_top=$(sed -E 's/.*Close: [0-9]+ ([0-9]+) .*/\1/' <<< "$1")
	lowest=$(grep -oE "Open: [0-9]+ [0-9]+ [0-9]+ [0-9]+" <<< "$1" | awk '{ print $3 + $5 }' | sort -n | tail -1)
	((lowest < close_top))
}
check "Saved lists has Close at the bottom right, below its rows" eval \
	'widgets=$(val 1 widgets) && close_at "$widgets" 854 480 && rows_above_close "$widgets"'
# "Close" goes back to the list's screen it opened from, as Escape does.
widget_click 0 Close
check "Close on Saved lists goes back to the list's screen" \
	drv 1 "await screen Material list for the blocks in the box"
mat_click "Saved lists"
drv 1 "await screen Saved lists" > /dev/null
keys 1 key Escape
check "as Escape does" drv 1 "await screen Material list for the blocks in the box"
mat_click "Saved lists"
drv 1 "await screen Saved lists" "wait 5" > /dev/null

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
