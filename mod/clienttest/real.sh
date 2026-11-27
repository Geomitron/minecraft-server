# "Place real blocks" (proposed for 0.9.0): the setting's row in "Settings", saved by its "Save" button, and what lands
# on the server. Each shape is drawn and placed, and each copy pasted, with Planner1's own clicks while the setting
# acts; a few checks send boxes of blocks with the driver's sendplace instead. With Planner1 in survival, its inventory
# set with /clear and /item: the controls lines, Ctrl+Z and Ctrl+Y after a ghost shape and a Line of real blocks, the
# break click erasing ghost blocks only, a whole Line from the main hand and the inventory and its one place sound,
# nothing, the whole Line red and no sound for a Line 1 oak plank short, nothing for a Line of torches along a wall's
# side, a door, a bed, the mix, the mirror's copies counted, stairs in a Wall turned with the wheel and mirrored, a real
# block and a cow in the way and red, ghost blocks filled and left red, a copy of a hut with a door, a wall torch, a
# hanging lantern, a double slab, candles and water, yellow, red 1 cobblestone short, pasted with one place sound for
# each player and around a real block in its way, a Floor of its copies red past 2,000 blocks, and a Line of 3 copies;
# in creative, the reach and the copy past it red, the cap of 2,000 blocks a place and a second, and the time a place
# of 2,000 takes; for Planner2, who isn't an operator, adventure mode and, once the server has started again with spawn
# protection, the spawn; then the setting off again, for a shape and a paste. Everything else happens more than 16
# blocks from the world's spawn, which the script sets at 0 -60 0. Each shape is drawn by a player standing south of
# it, with nothing real between their eyes and its clicks. "On the server" means the dev server's own world, as
# /execute if block finds it, and every real block checked there is checked on both clients too. Sourced by inside.sh,
# which provides drv, val, check, xdo, click, keys, use_click, attack_click, client_said, wait_for and the server's and
# clients' start and stop.

# half <n>: n + 0.5.
half() {
	awk -v n="$1" 'BEGIN { print n + 0.5 }'
}

# aim <client> <x y z>: the player looks at the middle of the top of the block below x y z, so a click there starts a
# shape at x y z.
aim() {
	drv "$1" "lookat $(half "$2") $3 $(half "$4")" "wait 5" > /dev/null
}

hold_ctrl() {
	xdo 1 keydown Control_L
	drv 1 "await sprint true" "wait 5" > /dev/null
}

let_go_ctrl() {
	xdo 1 keyup Control_L
	drv 1 "await sprint false" "wait 5" > /dev/null
}

# drawn <client>: how many blocks that client's preview drew in the last whole frame, after 10 ticks.
drawn() {
	local positions
	positions=$(drv "$1" "wait 10" "preview" | tail -1 | sed 's/^.* => //')
	if [[ $positions == none ]]; then
		echo 0
	else
		wc -w <<< "$positions"
	fi
}

# preview_is <client> <blocks>: the preview draws that many blocks; the count goes to checks.log.
preview_is() {
	local n
	n=$(drawn "$1")
	echo "  client$1's preview: $n blocks" >> /out/checks.log
	((n == $2))
}

# red <client>: the positions that client's preview drew red in the last whole frame, after 10 ticks, as x,y,z sorted
# and joined with spaces, or none; they go to checks.log too.
red() {
	local positions
	positions=$(drv "$1" "wait 10" "previewred" | tail -1 | sed 's/^.* => //' | tr ' ' '\n' | sort | paste -sd ' ')
	echo "  client$1's preview drew red: $positions" >> /out/checks.log
	echo "$positions"
}

# icons: the item icons beside Planner1's crosshair in the last whole frame, after 10 ticks; they go to checks.log too.
icons() {
	local drawn
	drawn=$(drv 1 "wait 10" "stacks" | tail -1 | sed 's/^.* => //')
	echo "  the crosshair's icons: $drawn" >> /out/checks.log
	echo "$drawn"
}

# draw <client> <x1 y1 z1> <x2 y2 z2>: a Line's first click aimed at the top of the block under x1 y1 z1, then the aim
# at the middle of the side of x2 y2 z2 facing the player, who stands south of it. Effortless Building ends the Line
# where the look crosses the plane of the first block's side, so it ends at x2 y2 z2 exactly.
draw() {
	aim "$1" "$2" "$3" "$4"
	use_click "$1"
	drv "$1" "lookat $(half "$5") $(half "$6") $7" "wait 5" > /dev/null
}

# erase <x1 y1 z1> <x2 y2 z2>: Planner1's shape drawn with the break click from the block x1 y1 z1, aimed at its top,
# to x2 y2 z2, as draw aims it.
erase() {
	aim 1 "$1" $(($2 + 1)) "$3"
	attack_click 1
	drv 1 "lookat $(half "$4") $(half "$5") $6" "wait 5" > /dev/null
	attack_click 1
	drv 1 "wait 10" > /dev/null
}

# place_click <client>: the right click that finishes the shape under way, or places in Single.
place_click() {
	use_click "$1"
	drv "$1" "wait 10" > /dev/null
}

# stock <item> <count> [<item> <count>]...: Planner1's inventory holds only these, the first in the main hand and each
# other in a slot of its own.
stock() {
	local commands=("cmd clear @s" "cmd item replace entity @s weapon.mainhand with $1 $2") slot=0
	shift 2
	while (($#)); do
		commands+=("cmd item replace entity @s inventory.$slot with $1 $2")
		slot=$((slot + 1))
		shift 2
	done
	drv 1 "${commands[@]}" "wait 10" > /dev/null
}

# items <item>: how many of the item Planner1's inventory holds, once the server's last changes have reached its game.
items() {
	drv 1 "wait 10" > /dev/null
	val 1 "itemcount $1"
}

# count_of <text> <lines>: how many of the lines are exactly the text.
count_of() {
	grep -cxF -- "$1" <<< "$2" || true
}

# shots <client> <name>: that client's screen at 854 by 480 and at 1920 by 1080, GUI scale 2, once the chat has faded,
# as <name>-<size>-gui2; the window is 854 by 480 again after.
shots() {
	drv "$1" "wait 210" "notoasts" "wait 5" "shot $2-854x480-gui2" "window 1920 1080 2" "wait 20" "notoasts" \
		"shot $2-1920x1080-gui2" "window 854 480 2" "wait 20" > /dev/null
}

# The setting's row in "Settings" and its switch, clicked with the client's own mouse.
# settings_screen <client>: opens the client's "Settings" screen, as its radial menu's button does.
settings_screen() {
	drv "$1" "close" "settings" "await screen Settings" "wait 10" > /dev/null
}
# row <client> <label>: the switch of the open screen's row with that label: its text, then its centre.
row() {
	val "$1" "settingrow $2"
}
# real_switch <client>: a real click on the switch of "Place real blocks".
real_switch() {
	local text x y
	read -r text x y < <(row "$1" "Place real blocks")
	click "$1" "$x" "$y"
	drv "$1" "wait 5" > /dev/null
}
# save_settings <client>: a real click on the screen's "Save" button, which closes it.
save_settings() {
	local x y
	read -r x y < <(val "$1" "widget 0 Save")
	click "$1" "$x" "$y"
	drv "$1" "await noscreen" "wait 5" > /dev/null
}
# setting_saved <client> <value>: the client's settings file saves the setting with that value.
setting_saved() {
	grep -qx "place-real-blocks=$2" "run/client$1/config/buildingplanner-client.properties"
}

# The log of the server the clients play on.
server_log=/out/server.log
# new_tag: a tag no other check says, for the server's log; each check may run in a subshell of its own.
new_tag() {
	echo "real-check-$(date +%s%N)"
}

# server_said <tag> <command>...: Planner1 runs the commands, each of which says the tag's hit when its block is as
# asked, then says the tag's end; gives how many hits the server's log has.
server_said() {
	local tag=$1
	shift
	drv 1 "$@" "cmd say $tag-end" > /dev/null && wait_for "$server_log" "$tag-end" 20 &&
		{ grep -c -- "$tag-hit" "$server_log" || true; }
}

# server_count <state> <x1 y1 z1> <x2 y2 z2>: how many blocks of that box the server's own world has in that state, as
# /execute if block finds them.
server_count() {
	local tag commands=() x y z
	tag=$(new_tag)
	for ((x = $2; x <= $5; x++)); do
		for ((y = $3; y <= $6; y++)); do
			for ((z = $4; z <= $7; z++)); do
				commands+=("cmd execute if block $x $y $z $1 run say $tag-hit")
			done
		done
	done
	server_said "$tag" "${commands[@]}"
}

# server_has <x y z> <state>: the server's own world has a block in that state there.
server_has() {
	[[ $(server_count "$4" "$1" "$2" "$3" "$1" "$2" "$3") == 1 ]]
}

# clients_count <x1 y1 z1 x2 y2 z2> <text>: each client's count of the box's real blocks other than air and of those whose
# state has the text, as "N M" when the clients agree.
clients_count() {
	local one two
	one=$(val 1 "realstates $1 $2 $3 $4 $5 $6 $7")
	two=$(val 2 "realstates $1 $2 $3 $4 $5 $6 $7")
	echo "  realstates $*: client1 $one, client2 $two" >> /out/checks.log
	[[ $one == "$two" ]] && echo "$one"
}

# lands <state> <text> <x1 y1 z1 x2 y2 z2> <count>: the server's world has that many blocks of the box in that state,
# and both clients show that many real blocks there, each with the text in its state, and no other real block.
lands() {
	[[ $(server_count "$1" "$3" "$4" "$5" "$6" "$7" "$8") == "$9" && $(clients_count "$3" "$4" "$5" "$6" "$7" "$8" "$2") == "$9 $9" ]]
}

# arrive <x y z> <block>: both clients show that real block there.
arrive() {
	drv 1 "await real $1 $2 $3 $4" > /dev/null && drv 2 "await real $1 $2 $3 $4" > /dev/null
}

# ghosts_are <client> <x1> <x2> <y> <z> <block> <count>: that client's copy of the layer holds that many ghost blocks
# of that block in the row from x1 to x2 at y z.
ghosts_are() {
	[[ $(val "$1" "countblocks $2 $3 $4 $5 $6") == "$7" ]]
}

# sent <client> <command>: the driver's sendplace, which gives "N blocks".
sent() {
	val "$1" "$2" | tee -a /out/checks.log
}

# chat_lines: how many chat lines Planner1's game has logged.
chat_lines() {
	grep -c -F "[CHAT]" /out/client1.log || true
}
# no_notice_since <lines>: Planner1's chat has shown nothing since it had that many lines but a say of a new tag, which
# the server sends after anything Planner1's click before it brought.
no_notice_since() {
	local tag
	tag=$(new_tag)
	drv 1 "cmd say $tag" > /dev/null && wait_for /out/client1.log "$tag" 20 && (($(chat_lines) == $1 + 1))
}

# The copy, as step9.sh drives it. press <client> <key>: a real key press with no screen open, returning once the game
# has handled it and two ticks have passed.
press() {
	local before
	drv "$1" "close" "wait 5" > /dev/null
	before=$(val "$1" presses)
	keys "$1" key "$2"
	drv "$1" "await pressedafter $before" "wait 2" > /dev/null
}
# copy_button: a real click on the radial menu's Copy button, with the menu's key (left Alt) held, which starts a box.
copy_button() {
	local x y
	xdo 1 keydown Alt_L
	drv 1 "await screen Build Modes" "wait 10" > /dev/null
	read -r x y < <(val 1 "widget 0 Copy")
	xdo 1 mousemove --window %w "$x" "$y"
	drv 1 "wait 5" > /dev/null
	xdo 1 click 1
	drv 1 "wait 5" > /dev/null
	xdo 1 keyup Alt_L
	drv 1 "await noscreen" "wait 5" > /dev/null
}
# box <ax ay az> <y1> <x2 z2> <y3>: Planner1's box with three left clicks: the first aimed at the point ax ay az, on a
# block's top face for the space above it or on its side for the block itself, which becomes the first corner, on
# level y1; the second at the middle of the block x2 y1 z2, where the line of sight crosses that level; the third at
# the middle of the block at height y3 above x2 z2.
box() {
	drv 1 "lookat $1 $2 $3" "wait 5" > /dev/null
	attack_click 1
	drv 1 "lookat $(half "$5") $4 $(half "$6")" "wait 5" > /dev/null
	attack_click 1
	drv 1 "lookat $(half "$5") $(half "$7") $(half "$6")" "wait 10" > /dev/null
	attack_click 1
	drv 1 "wait 5" > /dev/null
}

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

# Planner1 in survival, facing north 3.5 blocks south of each Line, with Planner2 nearby; the Lines run eastward, 5
# blocks apart. The animals the world made with its land are killed, so none is in a shape's way. Both windows are 854
# by 480 at GUI scale 2.
drv 1 "window 854 480 2" "cmd setworldspawn 0 -60 0" "cmd gamemode survival @s" "cmd tp @s 102.5 -60 36.5 180 30" \
	"cmd tp Planner2 103.5 -60 35.5 180 30" "ebmode LINE" "wait 40" "cmd kill @e[type=!minecraft:player]" \
	"wait 10" > /dev/null
drv 2 "window 854 480 2" "ebmode DISABLED" > /dev/null

# A ghost shape for undo, drawn with the setting off as it starts: a Line of ghost stone bricks from 100 -60 44 to
# 104 -60 44.
stock minecraft:stone_bricks 64
draw 1 100 -60 44 104 -60 44
place_click 1
check "with the setting off, a Line draws ghost stone bricks on both clients" eval \
	'drv 1 "await block 104 -60 44 minecraft:stone_bricks" > /dev/null &&
	drv 2 "await block 104 -60 44 minecraft:stone_bricks" > /dev/null && ghosts_are 1 100 104 -60 44 minecraft:stone_bricks 5'
check "and no real block" eval '[[ $(clients_count 100 -60 44 104 -60 44 stone_bricks) == "0 0" ]]'

# "Settings": the row "Place real blocks" after "Match ghost blocks", off at first, turned on with a real click and
# saved by "Save"; Planner2 turns it on the same way, for the Line near the spawn below.
settings_screen 1
read -r _ _ match_y < <(row 1 "Match ghost blocks")
read -r real_text _ real_y < <(row 1 "Place real blocks")
echo "  the rows' switches: \"Match ghost blocks\" at y $match_y, \"Place real blocks\" $real_text at y $real_y" \
	>> /out/checks.log
check "Planner1's \"Settings\" has the row \"Place real blocks\" right after \"Match ghost blocks\", off at first" eval \
	'[[ $real_text == OFF ]] && ((real_y - match_y == 48))'
real_switch 1
check "a click on its switch turns it on, and saves nothing yet" eval \
	'[[ $(row 1 "Place real blocks") == "ON "* ]] && ! grep -q "^place-real-blocks=" run/client1/config/buildingplanner-client.properties'
save_settings 1
check "\"Save\" closes the screen and saves place-real-blocks=true" setting_saved 1 true
# The screen reads its settings again whenever it lays itself out, as for a new window size, so its shots are taken
# once it shows the saved setting.
settings_screen 1
check "opened again, the screen shows it on" eval '[[ $(row 1 "Place real blocks") == "ON "* ]]'
xdo 1 mousemove --window %w 2 2
shots 1 real-settings
drv 1 "close" > /dev/null
settings_screen 2
real_switch 2
save_settings 2
check "Planner2 turns it on too" setting_saved 2 true

# The controls lines with the setting on, oak planks in hand: Line's before its first click, and Single's.
stock minecraft:oak_planks 5
aim 1 100 -60 40
check "Line's controls line reads \"[Right-click: start placing real blocks]\"" eval \
	'[[ $(val 1 modehud) == "[Right-click: start placing real blocks][Left-click: start erasing][H: Lock (Off)][Esc: exit mode]" ]]'
shots 1 real-line-controls
drv 1 "ebmode SINGLE" "wait 5" > /dev/null
check "Single's reads \"[Right-click: place real blocks]\"" eval \
	'[[ $(val 1 modehud) == "[Right-click: place real blocks][Left-click: erase][H: Lock (Off)][Esc: exit mode]" ]]'
shots 1 real-single-controls
drv 1 "ebmode LINE" "wait 5" > /dev/null

# Undo: a Line of 5 real oak planks from 100 -60 40 to 104 -60 40, after the ghost Line, makes no step of its own.
draw 1 100 -60 40 104 -60 40
place_click 1
check "a Line of real blocks places 5 real oak planks, as both clients show" eval \
	'arrive 104 -60 40 minecraft:oak_planks && lands minecraft:oak_planks oak_planks 100 -60 40 104 -60 40 5'
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "Ctrl+Z after it undoes the ghost Line before it, on both clients" eval \
	'drv 1 "await block 104 -60 44 minecraft:air" > /dev/null && drv 2 "await block 100 -60 44 minecraft:air" > /dev/null'
check "and leaves the Line's real blocks" lands minecraft:oak_planks oak_planks 100 -60 40 104 -60 40 5
keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
check "Ctrl+Y redoes the ghost Line on both clients" eval \
	'drv 1 "await block 104 -60 44 minecraft:stone_bricks" > /dev/null &&
	drv 2 "await block 100 -60 44 minecraft:stone_bricks" > /dev/null'
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
drv 1 "await block 104 -60 44 minecraft:air" > /dev/null
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "with no change to ghost blocks left to undo, Ctrl+Z says there's nothing to undo" client_said 1 "Nothing to undo."
check "and the real blocks stay" lands minecraft:oak_planks oak_planks 100 -60 40 104 -60 40 5

# The break click with the setting on: over the real Line, then over ghost stone bricks from 100 -60 42 to 104 -60 42.
stock minecraft:oak_planks 5
drv 1 "edit 100 -60 42 104 -60 42 minecraft:stone_bricks" > /dev/null
check "both clients see the ghost stone bricks" eval 'drv 1 "await block 104 -60 42 minecraft:stone_bricks" > /dev/null &&
	drv 2 "await block 104 -60 42 minecraft:stone_bricks" > /dev/null'
erase 100 -60 40 104 -60 40
check "a shape drawn with the break click over the real Line breaks none of it" eval \
	'drv 1 "wait 20" > /dev/null && lands minecraft:oak_planks oak_planks 100 -60 40 104 -60 40 5'
erase 100 -60 42 104 -60 42
check "and over the ghost stone bricks erases them, on both clients" eval \
	'drv 1 "await block 104 -60 42 minecraft:air" > /dev/null && drv 2 "await block 100 -60 42 minecraft:air" > /dev/null &&
	ghosts_are 1 100 104 -60 42 minecraft:stone_bricks 0 && ghosts_are 2 100 104 -60 42 minecraft:stone_bricks 0'

# A Line of 20 oak planks from 40 -60 40 to 59 -60 40, with 10 oak planks in hand and 10 elsewhere.
drv 1 "cmd tp @s 49.5 -60 36.5 180 30" "cmd tp Planner2 50.5 -60 35.5 180 30" "wait 40" > /dev/null
stock minecraft:oak_planks 10 minecraft:oak_planks 10
draw 1 40 -60 40 59 -60 40
check "a Line from 40 -60 40 to 59 -60 40 draws 20 blocks" preview_is 1 20
check "none of them red" eval '[[ $(red 1) == none ]]'
check "and the icons beside the crosshair count the 20 oak planks it uses" eval \
	'[[ $(icons) == "minecraft:oak_planks 20" ]]'
drv 1 "soundreset" > /dev/null
drv 2 "soundreset" > /dev/null
place_click 1
check "the click places all 20 as real oak planks on the server, as both clients show" eval \
	'arrive 59 -60 40 minecraft:oak_planks && lands minecraft:oak_planks oak_planks 40 -60 40 59 -60 40 20'
check "no ghost block appears" eval '[[ $(val 1 "countblocks 40 59 -60 40 minecraft:oak_planks") == 0 &&
	$(val 2 "countblocks 40 59 -60 40 minecraft:oak_planks") == 0 ]]'
check "Planner1's inventory holds no oak planks" eval '[[ $(items minecraft:oak_planks) == 0 ]]'
check "the server sends Planner2's game one place sound for the 20 blocks" eval \
	'[[ $(val 2 "serversounds block.wood.place") == 1 ]]'
check "and Planner1's game none" eval '[[ $(val 1 "serversounds block.wood.place") == 0 ]]'
check "Planner1's own game plays the click's place sound once" eval '[[ $(val 1 "sounds block.wood.place") == 1 ]]'

# The same Line at z 45 with 19 oak planks.
drv 1 "cmd tp @s 49.5 -60 41.5 180 30" "wait 20" > /dev/null
stock minecraft:oak_planks 10 minecraft:oak_planks 9
draw 1 40 -60 45 59 -60 45
check "with 19 oak planks, a Line of 20 draws 20 blocks" preview_is 1 20
check "all 20 red" eval '[[ $(red 1 | wc -w) == 20 ]]'
check "and the icons beside the crosshair show 19 oak planks and a red 1" eval \
	'[[ $(icons) == "minecraft:oak_planks 19, minecraft:oak_planks 1 red" ]]'
shots 1 real-line-short
drv 1 "soundreset" > /dev/null
place_click 1
check "the click gets the notice naming 1 oak plank short" client_said 1 \
	"Not enough items, so nothing was placed. You're short of 1 Oak Planks."
check "nothing went in on the server or either client" lands minecraft:oak_planks oak_planks 40 -60 45 59 -60 45 0
check "the inventory still has its 19" eval '[[ $(items minecraft:oak_planks) == 19 ]]'
check "and Planner1's game played no place sound" eval '[[ $(val 1 "sounds block.wood.place") == 0 ]]'

# A Line of torches along the south side of a real stone wall from 70 -60 30 to 79 -58 30, aimed at the wall's side:
# each torch keeps Effortless Building's standing form, which can't stay there, so nothing goes in and no notice
# comes (1408 Q1 C).
drv 1 "cmd fill 70 -60 30 79 -58 30 minecraft:stone" "cmd tp @s 74.5 -60 35.5 180 30" \
	"cmd tp Planner2 75.5 -60 36.5 180 30" "wait 20" > /dev/null
stock minecraft:torch 8
drv 1 "lookat 70.5 -58.5 31" "wait 5" > /dev/null
use_click 1
drv 1 "lookat 77.5 -58.5 31" "wait 5" > /dev/null
check "a Line of torches along the wall's side draws 8 blocks" preview_is 1 8
echo "  the preview's first torch: $(val 1 "previewstate 70 -59 31")" >> /out/checks.log
check "all 8 red, as torches that can't stay there" eval '[[ $(red 1 | wc -w) == 8 ]]'
lines=$(chat_lines)
place_click 1
check "no notice comes for the click: Planner1's chat shows only a say sent after it" no_notice_since "$lines"
check "it places no torch on the server or either client" eval \
	'lands minecraft:torch torch 70 -59 31 77 -59 31 0 &&
	[[ $(server_count minecraft:wall_torch 70 -59 31 77 -59 31) == 0 ]]'
check "and Planner1 keeps all 8 torches" eval '[[ $(items minecraft:torch) == 8 ]]'

# In Single, a door at 62 -60 32 and a bed at 65 -60 32, Planner1 facing north, each placed with one click.
drv 1 "ebmode SINGLE" "cmd tp @s 63.5 -60 35.5 180 30" "cmd tp Planner2 64.5 -60 36.5 180 30" "wait 20" > /dev/null
stock minecraft:oak_door 1
aim 1 62 -60 32
place_click 1
check "on the server, the door has both halves, as both clients show" eval \
	'arrive 62 -59 32 minecraft:oak_door && server_has 62 -60 32 "minecraft:oak_door[half=lower]" &&
	server_has 62 -59 32 "minecraft:oak_door[half=upper]" &&
	[[ $(clients_count 62 -60 32 62 -59 32 "half=lower") == "2 1" ]]'
check "and it used the one door" eval '[[ $(items minecraft:oak_door) == 0 ]]'
stock minecraft:red_bed 1
aim 1 65 -60 32
place_click 1
check "on the server, the bed has both parts, its head north of its foot, as both clients show" eval \
	'arrive 65 -60 31 minecraft:red_bed && server_has 65 -60 32 "minecraft:red_bed[part=foot,facing=north]" &&
	server_has 65 -60 31 "minecraft:red_bed[part=head,facing=north]" &&
	[[ $(clients_count 65 -60 31 65 -60 32 "part=foot") == "2 1" ]]'
check "and it used the one bed" eval '[[ $(items minecraft:red_bed) == 0 ]]'

# The mix of 80 cobblestone and 20 mossy cobblestone, in Lines at z 50 and 55.
drv 1 "ebmode LINE" "setmix minecraft:cobblestone:80,minecraft:mossy_cobblestone:20" \
	"cmd tp @s 49.5 -60 46.5 180 30" "cmd tp Planner2 50.5 -60 45.5 180 30" "wait 20" > /dev/null
# mix_states <z>: the block the preview drew at each x from 40 to 59 at -60 z, one per line.
mix_states() {
	local x commands=()
	for ((x = 40; x <= 59; x++)); do
		commands+=("previewstate $x -60 $1")
	done
	drv 1 "${commands[@]}" | sed -e 's/^.* => //' -e 's/\[.*$//'
}
# mix_matches <client> <z>: each block of the Line at z on that client is the one the preview showed.
mix_matches() {
	local x commands=()
	for ((x = 40; x <= 59; x++)); do
		commands+=("real $x -60 $2")
	done
	[[ $(drv "$1" "${commands[@]}" | sed 's/^.* => //') == "$shown" ]]
}
# mix_on_server <z>: each block of the Line at z on the server is the one the preview showed.
mix_on_server() {
	local tag commands=() i=0 block
	tag=$(new_tag)
	while read -r block; do
		commands+=("cmd execute if block $((40 + i)) -60 $1 $block run say $tag-hit")
		i=$((i + 1))
	done <<< "$shown"
	[[ $(server_said "$tag" "${commands[@]}") == 20 ]]
}
stock minecraft:cobblestone 64
draw 1 40 -60 50 59 -60 50
check "a Line of the mix draws 20 blocks" preview_is 1 20
shown=$(mix_states 50)
cobble=$(count_of minecraft:cobblestone "$shown")
mossy=$(count_of minecraft:mossy_cobblestone "$shown")
echo "  the preview uses $cobble cobblestone and $mossy mossy cobblestone" >> /out/checks.log
check "the preview's blocks are the mix's" eval '((cobble + mossy == 20 && cobble > 0))'
if ((mossy > 0)); then
	stock minecraft:cobblestone "$cobble" minecraft:mossy_cobblestone "$mossy"
else
	stock minecraft:cobblestone "$cobble"
fi
check "with as many of each as the preview uses, the preview draws the same blocks" eval \
	'[[ $(mix_states 50) == "$shown" ]]'
place_click 1
check "on the server, each block of the Line is the one the preview showed there, as both clients show" eval \
	'drv 1 "await real 59 -60 50 $(tail -1 <<< "$shown")" > /dev/null &&
	drv 2 "await real 59 -60 50 $(tail -1 <<< "$shown")" > /dev/null && mix_on_server 50 && mix_matches 1 50 &&
	mix_matches 2 50'
check "the inventory has none of either left" eval \
	'[[ $(items minecraft:cobblestone) == 0 && $(items minecraft:mossy_cobblestone) == 0 ]]'
drv 1 "cmd tp @s 49.5 -60 51.5 180 30" "wait 20" > /dev/null
stock minecraft:cobblestone 64
draw 1 40 -60 55 59 -60 55
check "a second Line of the mix draws 20 blocks" preview_is 1 20
shown=$(mix_states 55)
cobble=$(count_of minecraft:cobblestone "$shown")
mossy=$(count_of minecraft:mossy_cobblestone "$shown")
echo "  the preview uses $cobble cobblestone and $mossy mossy cobblestone" >> /out/checks.log
if ((mossy > 1)); then
	stock minecraft:cobblestone "$cobble" minecraft:mossy_cobblestone $((mossy - 1))
else
	stock minecraft:cobblestone "$cobble"
fi
check "the second Line uses some mossy cobblestone" eval '((mossy > 0))'
place_click 1
check "with one mossy cobblestone fewer than it uses, the notice names 1 mossy cobblestone short" client_said 1 \
	"Not enough items, so nothing was placed. You're short of 1 Mossy Cobblestone."
check "and nothing went in" eval '[[ $(server_count minecraft:cobblestone 40 -60 55 59 -60 55) == 0 &&
	$(server_count minecraft:mossy_cobblestone 40 -60 55 59 -60 55) == 0 &&
	$(clients_count 40 -60 55 59 -60 55 cobblestone) == "0 0" ]]'
drv 1 "setmix" > /dev/null

# With the mirror across x 50, a Line of 5 from 40 to 44 at z 60 and its copy from 55 to 59 need 10 oak planks.
drv 1 "cmd tp @s 49.5 -60 56.5 180 30" "wait 20" "ebmirror 50" "wait 5" > /dev/null
stock minecraft:oak_planks 5
draw 1 40 -60 60 44 -60 60
check "with the mirror on, a Line of 5 draws 10 blocks" preview_is 1 10
place_click 1
check "with oak planks for only one of them, the notice names 5 oak planks short" client_said 1 \
	"Not enough items, so nothing was placed. You're short of 5 Oak Planks."
check "and nothing went in" lands minecraft:oak_planks oak_planks 40 -60 60 59 -60 60 0
check "the inventory still has its 5" eval '[[ $(items minecraft:oak_planks) == 5 ]]'

# Oak stairs in a Wall of 4 by 2 from 40 -60 65 to 43 -59 65, turned with the wheel, and mirrored across x 45 to 46 -60
# 65 to 49 -59 65. A real stair stands behind one end of each wall's bottom row, at a right angle, so the stair there
# joins it as by hand: Minecraft makes a stair inner_left when the stair behind it faces its facing turned
# anticlockwise, and no stair of its own facing stands that way beside it.
drv 1 "ebmode WALL" "cmd tp @s 44.5 -60 60.5 180 30" "cmd tp Planner2 45.5 -60 59.5 180 30" "wait 20" \
	"ebmirror 45" "wait 5" > /dev/null
stock minecraft:oak_stairs 16
aim 1 40 -60 65
hold_ctrl
xdo 1 click 4
drv 1 "wait 5" > /dev/null
let_go_ctrl
echo "  held turn: $(val 1 heldturn)" >> /out/checks.log
check "a wheel turn turns the stair in hand" eval '[[ $(val 1 heldturn) == *facing=* ]]'
use_click 1
drv 1 "lookat 43.5 -58.5 65" "wait 5" > /dev/null
check "the Wall and its mirrored copy draw 16 stairs" preview_is 1 16
# stair_at <x y z>: the facing and half of the stair the preview draws there, as "facing half".
stair_at() {
	val 1 "previewstate $1 $2 $3" | sed -n 's/.*facing=\([a-z]*\),half=\([a-z]*\).*/\1 \2/p'
}
ccw() {
	case $1 in east) echo north ;; north) echo west ;; west) echo south ;; south) echo east ;; esac
}
# behind <facing>: the step in x and in z from a stair facing that way to the space behind it.
behind() {
	case $1 in east) echo "-1 0" ;; west) echo "1 0" ;; north) echo "0 1" ;; south) echo "0 -1" ;; esac
}
# join_end <facing> <west end> <east end>: the end of a wall along x whose stair can join a stair behind it: the west
# end for stairs facing north or east, the east end for south or west.
join_end() {
	case $1 in north | east) echo "$2" ;; *) echo "$3" ;; esac
}
read -r facing half <<< "$(stair_at 40 -60 65)"
read -r copy_facing copy_half <<< "$(stair_at 49 -60 65)"
join=$(join_end "$facing" 40 43)
copy_join=$(join_end "$copy_facing" 46 49)
read -r dx dz <<< "$(behind "$facing")"
read -r copy_dx copy_dz <<< "$(behind "$copy_facing")"
echo "  the walls' stairs face $facing and $copy_facing; they join at x $join and $copy_join" >> /out/checks.log
drv 1 "cmd setblock $((join + dx)) -60 $((65 + dz)) minecraft:oak_stairs[facing=$(ccw "$facing"),half=$half]" \
	"cmd setblock $((copy_join + copy_dx)) -60 $((65 + copy_dz)) minecraft:oak_stairs[facing=$(ccw "$copy_facing"),half=$copy_half]" \
	"wait 10" > /dev/null
# stair_states: each stair the preview draws, as "x y z facing half", one per line.
stair_states() {
	local x y
	for x in 40 41 42 43 46 47 48 49; do
		for y in -60 -59; do
			echo "$x $y 65 $(stair_at "$x" "$y" 65)"
		done
	done
}
stairs=$(stair_states)
echo "$stairs" | sed 's/^/  preview: /' >> /out/checks.log
place_click 1
# stairs_as_shown <client>: each stair on that client faces as the preview showed it, in the same half.
stairs_as_shown() {
	local x y z f h state
	while read -r x y z f h; do
		state=$(val "$1" "realstate $x $y $z")
		[[ $state == *oak_stairs* && $state == *"facing=$f"* && $state == *"half=$h"* ]] || return 1
	done <<< "$stairs"
}
# stairs_on_server: each stair on the server faces as the preview showed it, in the same half.
stairs_on_server() {
	local x y z f h tag commands=()
	tag=$(new_tag)
	while read -r x y z f h; do
		commands+=("cmd execute if block $x $y $z minecraft:oak_stairs[facing=$f,half=$h] run say $tag-hit")
	done <<< "$stairs"
	[[ $(server_said "$tag" "${commands[@]}") == 16 ]]
}
check "on the server, each stair faces as the preview showed it, as both clients show" eval \
	'arrive 49 -59 65 minecraft:oak_stairs && arrive 43 -59 65 minecraft:oak_stairs && stairs_on_server &&
	stairs_as_shown 1 && stairs_as_shown 2'
check "the stair beside the real one behind each wall joins it, as by hand" eval \
	'server_has $join -60 65 "minecraft:oak_stairs[facing=$facing,shape=inner_left]" &&
	server_has $copy_join -60 65 "minecraft:oak_stairs[facing=$copy_facing,shape=inner_left]" &&
	[[ $(val 1 "realstate $join -60 65") == *shape=inner_left* && $(val 2 "realstate $copy_join -60 65") == *shape=inner_left* ]]'
check "the inventory has no stairs left" eval '[[ $(items minecraft:oak_stairs) == 0 ]]'
# Planner1 steps out of Planner2's view of the Wall for its shots.
drv 1 "cmd tp @s 38.5 -60 58.5" "wait 5" > /dev/null
drv 2 "lookat 45 -59 65" "wait 5" > /dev/null
shots 2 real-wall-placed
drv 1 "ebarray 0" > /dev/null

# A Line of 10 at z 70 through a real stone at 43 -60 70 and a cow at 46 -60 70, with 8 oak planks.
drv 1 "ebmode LINE" "cmd tp @s 44.5 -60 66.5 180 30" "cmd tp Planner2 45.5 -60 64.5 180 30" \
	"cmd setblock 43 -60 70 minecraft:stone" "cmd summon minecraft:cow 46.5 -60 70.5 {NoAI:1b}" "wait 20" > /dev/null
stock minecraft:oak_planks 8
draw 1 40 -60 70 49 -60 70
echo "  the Line through the stone and the cow draws $(drawn 1) blocks" >> /out/checks.log
check "the preview draws the stone's and the cow's blocks red" eval '[[ $(red 1) == "43,-60,70 46,-60,70" ]]'
check "and the icons beside the crosshair count the 8 oak planks the others use" eval \
	'[[ $(icons) == "minecraft:oak_planks 8" ]]'
shots 1 real-line-red-icons
place_click 1
check "on the server, the other 8 are oak planks, the stone stays and the cow's space is empty, as both clients show" \
	eval 'arrive 49 -60 70 minecraft:oak_planks && arrive 45 -60 70 minecraft:oak_planks &&
	[[ $(server_count minecraft:oak_planks 40 -60 70 49 -60 70) == 8 ]] && server_has 43 -60 70 minecraft:stone &&
	server_has 46 -60 70 minecraft:air && [[ $(clients_count 40 -60 70 49 -60 70 oak_planks) == "9 8" ]]'
check "the inventory has no oak planks left" eval '[[ $(items minecraft:oak_planks) == 0 ]]'
drv 1 "cmd kill @e[type=minecraft:cow]" > /dev/null

# The reach, in creative: Planner1's eyes at 100 -58.38 105.5, a Single block at 97 -60 105 with the array's 2 copies 66
# apart, at 163 (63 blocks from the eyes) and 229 (129), whose chunk the server keeps loaded for the check.
drv 1 "cmd gamemode creative @s" "ebmode SINGLE" "ebarray 2 66 0 0" "cmd forceload add 229 105" \
	"cmd tp @s 100.0 -60 105.5 90 30" "cmd tp Planner2 130.5 -60 110.5 90 30" "wait 60" > /dev/null
stock minecraft:oak_planks 1
aim 1 97 -60 105
echo "  the Single block and the array's copies draw $(drawn 1) blocks" >> /out/checks.log
check "the preview draws the copy past 64 blocks red" eval '[[ $(red 1) == "229,-60,105" ]]'
place_click 1
check "on the server, the block and the copy at 63 blocks are placed, and the one at 129 isn't, as both clients show" \
	eval 'arrive 163 -60 105 minecraft:oak_planks && server_has 97 -60 105 minecraft:oak_planks &&
	server_has 163 -60 105 minecraft:oak_planks && server_has 229 -60 105 minecraft:air &&
	[[ $(val 1 "real 97 -60 105") == minecraft:oak_planks ]]'
# Sent as boxes: two blocks level with the eyes, whose nearest faces are 64 and 65 blocks from them.
drv 1 "ebarray 0" > /dev/null
check "a place of blocks 64 and 65 blocks from the eyes is sent" eval \
	'[[ $(sent 1 "sendplace west 164.0 -58.5 105.5 shape 164 -59 105 165 -59 105 minecraft:oak_planks") == "2 blocks" ]]'
check "on the server, the one at 64 is placed and the one at 65 isn't, as both clients show" eval \
	'arrive 164 -59 105 minecraft:oak_planks && server_has 164 -59 105 minecraft:oak_planks &&
	server_has 165 -59 105 minecraft:air && [[ $(val 2 "real 165 -59 105") == minecraft:air ]]'
drv 1 "cmd forceload remove 229 105" "cmd gamemode survival @s" > /dev/null

# A Floor of oak planks from 40 -60 80 to 44 -60 84 over ghost oak planks, and one from 50 -60 80 to 54 -60 84 over
# ghost stone bricks, sent as boxes with the face and hit of a Floor's first click.
drv 1 "cmd tp @s 47.5 -60 77.5 180 30" "cmd tp Planner2 47.5 -60 76.5 180 30" "wait 40" \
	"edit 40 -60 80 44 -60 84 minecraft:oak_planks" "edit 50 -60 80 54 -60 84 minecraft:stone_bricks" > /dev/null
for n in 1 2; do
	check "client $n sees the ghost floors" drv "$n" "await block 44 -60 84 minecraft:oak_planks" \
		"await block 54 -60 84 minecraft:stone_bricks"
done
stock minecraft:oak_planks 50
check "a place of both floors is sent" eval '[[ $(sent 1 "sendplace up 40.5 -60 80.5 shape 40 -60 80 44 -60 84 minecraft:oak_planks 50 -60 80 54 -60 84 minecraft:oak_planks") == "50 blocks" ]]'
check "on the server, both floors are real oak planks, as both clients show" eval \
	'arrive 54 -60 84 minecraft:oak_planks && arrive 44 -60 84 minecraft:oak_planks &&
	lands minecraft:oak_planks oak_planks 40 -60 80 44 -60 84 25 && lands minecraft:oak_planks oak_planks 50 -60 80 54 -60 84 25'
# ghosts_left <client> <x1> <x2> <block>: how many ghost blocks of that block the rows from z 80 to 84 hold from x1 to x2.
ghosts_left() {
	local z total=0
	for z in 80 81 82 83 84; do
		total=$((total + $(val "$1" "countblocks $2 $3 -60 $z $4")))
	done
	echo "$total"
}
check "the oak planks take the ghost oak planks out of the layer, on both clients" eval \
	'drv 1 "wait 10" > /dev/null && [[ $(ghosts_left 1 40 44 minecraft:oak_planks) == 0 && $(ghosts_left 2 40 44 minecraft:oak_planks) == 0 ]]'
check "and leave the ghost stone bricks" eval \
	'[[ $(ghosts_left 1 50 54 minecraft:stone_bricks) == 25 && $(ghosts_left 2 50 54 minecraft:stone_bricks) == 25 ]]'
check "which Litematica marks red for Planner1, as wrong blocks" eval '[[ $(val 1 "overlay 52 -60 82") == WRONG_BLOCK ]]'

# A paste of real blocks, with Planner1's own clicks: a hut of cobblestone from 110 -60 60 to 114 -57 64 (floor, walls
# and roof), with an oak door in its south wall and, inside, a wall torch on the north wall, a lantern hanging from the
# roof, a double slab, 3 candles and a water source in the floor: 86 blocks, 79 of them cobblestone. Planner1 copies
# it with the radial menu's Copy button, standing on a stone 2 blocks above the roof south of it, as step9.sh copies
# its house, the box's first corner the roof's corner nearest Planner1, aimed at its south side.
drv 1 "cmd fill 110 -60 60 114 -57 64 minecraft:cobblestone hollow" "cmd setblock 111 -60 63 minecraft:water" \
	"cmd setblock 112 -59 64 minecraft:oak_door[half=lower]" "cmd setblock 112 -58 64 minecraft:oak_door[half=upper]" \
	"cmd setblock 112 -58 61 minecraft:wall_torch[facing=south]" \
	"cmd setblock 112 -58 62 minecraft:lantern[hanging=true]" "cmd setblock 111 -59 61 minecraft:stone_slab[type=double]" \
	"cmd setblock 113 -59 61 minecraft:candle[candles=3]" "cmd setblock 112 -56 68 minecraft:stone" \
	"cmd tp @s 112.5 -55 68.5 180 30" "cmd tp Planner2 126.5 -60 70.5 180 30" "wait 40" \
	"cmd kill @e[type=!minecraft:player]" "wait 10" > /dev/null
check "both clients show the hut's 86 blocks, 79 of them cobblestone" eval \
	'arrive 112 -58 64 minecraft:oak_door && arrive 112 -58 62 minecraft:lantern &&
	[[ $(clients_count 110 -60 60 114 -57 64 cobblestone) == "86 79" ]]'
# hut_stock [cobblestone]: Planner1's inventory holds the items a paste of the hut uses, cobblestone in hand: 79
# cobblestone unless given, a door, a torch, a lantern, 2 stone slabs and 3 candles.
hut_stock() {
	local cobblestone=${1:-79}
	stock minecraft:cobblestone 64 minecraft:cobblestone $((cobblestone - 64)) minecraft:oak_door 1 minecraft:torch 1 \
		minecraft:lantern 1 minecraft:stone_slab 2 minecraft:candle 3
}
# hut_used: Planner1's inventory holds none of the items a paste of the hut uses.
hut_used() {
	local item
	for item in cobblestone oak_door torch lantern stone_slab candle; do
		[[ $(items "minecraft:$item") == 0 ]] || return 1
	done
}
hut_stock
copy_button
box 110.5 -56.5 64.9 -57 114 60 -60
check "a box around the hut copies its 86 blocks" client_said 1 "Copied 86 blocks."
# On the ground east of the hut, aimed at the grass, the copy rests from 120 -60 60 to 124 -57 64.
drv 1 "cmd tp @s 122.5 -60 70.5 180 30" "cmd setblock 112 -56 68 minecraft:air" "wait 20" "lookat 122.5 -60 62.5" \
	"wait 5" > /dev/null
check "the copy rests on the ground east of the hut, from 120 -60 60 to 124 -57 64" \
	drv 1 "await copy at 120,-60,60..124,-57,64"
copy_end="[Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Off)][Esc/Left-click: close]"
check "with every item the paste uses, the copy is yellow, for the water no item places" \
	drv 1 "await copycolour YELLOW"
check "and the controls line reads \"[Right-click: paste real blocks]\"" eval \
	'[[ $(val 1 modehud) == "[Right-click: paste real blocks]$copy_end" ]]'
shots 1 real-copy-yellow
hut_stock 78
short_line="[Can't paste: not enough blocks]$copy_end"
check "with 1 cobblestone short, the copy is red" drv 1 "await copycolour RED NOT_ENOUGH_BLOCKS"
check "and the controls line reads \"[Can't paste: not enough blocks]\"" eval '[[ $(val 1 modehud) == "$short_line" ]]'
shots 1 real-copy-red
place_click 1
check "its click sends nothing" eval '[[ $(val 1 placesent) == none && $(val 1 holdlog) == *" edits=0 "* ]]'
check "and nothing goes in" eval '[[ $(clients_count 120 -60 60 124 -57 64 cobblestone) == "0 0" ]] &&
	server_has 120 -60 60 minecraft:air && [[ $(items minecraft:cobblestone) == 78 ]]'

hut_stock
drv 1 "await copycolour YELLOW" "soundreset" > /dev/null
drv 2 "soundreset" > /dev/null
lines=$(chat_lines)
place_click 1
check "with every item, the click sends one place of the copy's 86 blocks and no edit" eval \
	'[[ $(val 1 placesent) == 86 && $(val 1 holdlog) == *" edits=0 "* ]]'
check "no notice comes for the water: Planner1's chat shows only a say sent after the click" no_notice_since "$lines"
# hut_matches: the server's blocks from 120 -60 60 to 124 -57 64 are the hut's, block for block in the same states, as
# /execute if blocks compares them, once the hut's water is taken out.
hut_matches() {
	local tag
	tag=$(new_tag)
	[[ $(server_said "$tag" "cmd setblock 111 -60 63 minecraft:air" \
		"cmd execute if blocks 110 -60 60 114 -57 64 120 -60 60 all run say $tag-hit") == 1 ]]
}
check "on the server, the paste is the hut but for its water, as both clients show" eval \
	'arrive 124 -57 64 minecraft:cobblestone && arrive 122 -58 62 minecraft:lantern && hut_matches &&
	[[ $(clients_count 120 -60 60 124 -57 64 cobblestone) == "85 79" ]]'
echo "  the pasted door: $(val 1 "realstate 122 -59 64")" >> /out/checks.log
check "with the door's two halves, the torch on the wall, the lantern hanging, the double slab and the 3 candles" eval \
	'server_has 122 -59 64 "minecraft:oak_door[half=lower]" && server_has 122 -58 64 "minecraft:oak_door[half=upper]" &&
	server_has 122 -58 61 "minecraft:wall_torch[facing=south]" && server_has 122 -58 62 "minecraft:lantern[hanging=true]" &&
	server_has 121 -59 61 "minecraft:stone_slab[type=double]" && server_has 123 -59 61 "minecraft:candle[candles=3]" &&
	server_has 121 -60 63 minecraft:air'
check "using every item the paste needs" hut_used
check "the server sends Planner1's game and Planner2's each one place sound" eval \
	'[[ $(val 1 "serversounds .place") == 1 && $(val 2 "serversounds .place") == 1 ]]'
drv 2 "lookat 122.5 -58.5 65" "wait 5" > /dev/null
shots 2 real-paste-placed

# The same paste at 130 -60 60, over a real stone at 130 -59 62 in its west wall: 78 cobblestone go in.
drv 1 "cmd setblock 130 -59 62 minecraft:stone" "cmd tp @s 132.5 -60 70.5 180 30" "wait 20" \
	"lookat 132.5 -60 62.5" "wait 5" > /dev/null
hut_stock 78
check "the copy rests from 130 -60 60 to 134 -57 64" drv 1 "await copy at 130,-60,60..134,-57,64"
check "yellow, for the water and the stone in its way" drv 1 "await copycolour YELLOW"
check "and after a paste, its controls line has \"[Hold Left Control: line up]\"" eval \
	'[[ $(val 1 modehud) == "[Right-click: paste real blocks][Scroll: turn][Middle-click: mirror][Hold Left Control: line up][H: Lock (Off)][R: Mode (Off)][Esc/Left-click: close]" ]]'
place_click 1
check "the click places all but the stone's block and the water, and the stone stays, as both clients show" eval \
	'arrive 134 -57 64 minecraft:cobblestone && arrive 132 -58 62 minecraft:lantern &&
	server_has 130 -59 62 minecraft:stone && [[ $(server_count minecraft:cobblestone 130 -60 60 134 -57 64) == 78 ]] &&
	[[ $(clients_count 130 -60 60 134 -57 64 cobblestone) == "85 78" ]]'
check "using the 78 cobblestone and the other items" hut_used

# A Floor of the hut's copies, 5 by 5 from 150 -60 60 to 174 -57 40: 25 copies of 86 blocks, 2,150 in all. The Floor
# ends where the line of sight crosses the level of the first copy's middle block, -59 (Floor.findFloor), so the aim
# after the first click is at that level.
drv 1 "cmd tp @s 162.5 -60 66.5 180 30" "wait 20" > /dev/null
hut_stock
press 1 r
press 1 r
press 1 r
check "R three times sets Floor" eval '[[ $(val 1 copymode) == FLOOR ]]'
drv 1 "lookat 152.5 -60 62.5" "wait 5" "await copy at 150,-60,60..154,-57,64" > /dev/null
use_click 1
drv 1 "lookat 172.5 -59 42.5" "wait 5" > /dev/null
echo "  the Floor: $(val 1 copyset | cut -c 1-80)" >> /out/checks.log
check "a Floor of 25 copies" drv 1 "await set 25 copies"
too_many="[Can't paste: too many blocks][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Floor)][Esc/Left-click: cancel]"
check "over 2,000 blocks, it's red" drv 1 "await copycolour RED TOO_MANY_BLOCKS"
check "and the controls line reads \"[Can't paste: too many blocks]\"" eval '[[ $(val 1 modehud) == "$too_many" ]]'
place_click 1
check "its second click sends nothing, and nothing goes in" eval \
	'[[ $(val 1 placesent) == none && $(val 1 holdlog) == *" edits=0 "* ]] && server_has 150 -60 60 minecraft:air &&
	[[ $(clients_count 150 -60 60 154 -57 64 cobblestone) == "0 0" ]]'
attack_click 1
press 1 r
check "a left click drops the set, and R sets Off again" eval \
	'drv 1 "wait 10" > /dev/null && [[ $(val 1 copyset) == none && $(val 1 copymode) == OFF ]]'

# A Line of 3 copies of a row of 4 real oak planks from 140 -60 60 to 143 -60 60, pasted from 150 -60 60 to 161 -60 60.
drv 1 "cmd fill 140 -60 60 143 -60 60 minecraft:oak_planks" "cmd tp @s 141.5 -60 64.5 180 30" "wait 20" > /dev/null
stock minecraft:oak_planks 12
copy_button
box 140.5 -59 60.5 -59 143 60 -60
check "a box around the row copies its 4 blocks" client_said 1 "Copied 4 blocks."
drv 1 "cmd tp @s 155.5 -60 64.5 180 30" "wait 20" > /dev/null
press 1 r
check "R sets Line" eval '[[ $(val 1 copymode) == LINE ]]'
drv 1 "lookat 151.5 -60 60.5" "wait 5" > /dev/null
check "the copy rests from 150 -60 60 to 153 -60 60" drv 1 "await copy at 150,-60,60..153,-60,60"
use_click 1
drv 1 "lookat 159.5 -60 60.5" "wait 5" > /dev/null
check "a Line of 3 copies" drv 1 "await set 3 copies"
check "blue, with the controls line reading \"[Right-click: paste 3 copies as real blocks]\"" eval \
	'drv 1 "await copycolour BLUE" > /dev/null &&
	[[ $(val 1 modehud) == "[Right-click: paste 3 copies as real blocks][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Line)][Esc/Left-click: cancel]" ]]'
place_click 1
check "the second click sends one place of the 3 copies' 12 blocks" eval '[[ $(val 1 placesent) == 12 ]]'
check "on the server, the 12 are real oak planks, as both clients show" eval \
	'arrive 161 -60 60 minecraft:oak_planks && lands minecraft:oak_planks oak_planks 150 -60 60 161 -60 60 12'
check "using the 12 oak planks" eval '[[ $(items minecraft:oak_planks) == 0 ]]'
# Back to Off, with the copy put away.
press 1 r
press 1 r
press 1 r
attack_click 1
check "R three times sets Off again, and a left click puts the copy away" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 copymode) == OFF && $(val 1 copy) == away* ]]'

# In adventure mode, 5 blocks from 68 -60 52 to 72 -60 52.
drv 1 "cmd gamemode adventure Planner2" "cmd tp Planner2 70.5 -60 47.5 180 30" "cmd tp @s 69.5 -60 47.5 180 30" \
	"wait 40" > /dev/null
check "in adventure mode, Planner2's place of 5 blocks is sent" eval \
	'[[ $(sent 2 "sendplace up 68.5 -60 52.5 shape 68 -60 52 72 -60 52 minecraft:oak_planks") == "5 blocks" ]]'
check "Planner2 gets the notice" client_said 2 "5 blocks weren't placed: the server doesn't allow them there."
check "and nothing went in" lands minecraft:oak_planks oak_planks 68 -60 52 72 -60 52 0
drv 1 "cmd gamemode creative Planner2" > /dev/null

# The cap, in creative: 2,001 blocks from 40 -60 100 to 59 -60 199 and at 40 -60 200, then the 2,000 alone and right
# after them 1 more, with both players east of the blocks, out of their way and within 64 blocks of every one, and
# the animals the world made as it made the land killed, so nothing is in the blocks' way.
drv 1 "cmd gamemode creative @s" "cmd tp @s 61.5 -60 150.5 90 30" "cmd tp Planner2 62.5 -60 140.5 90 30" \
	"wait 60" "cmd kill @e[type=!minecraft:player]" "wait 10" > /dev/null
stock minecraft:oak_planks 1
check "a place of 2,001 blocks is sent" eval '[[ $(sent 1 "sendplace up 40.5 -60 100.5 shape 40 -60 100 59 -60 199 minecraft:oak_planks 40 -60 200 40 -60 200 minecraft:oak_planks") == "2001 blocks" ]]'
check "it gets the cap's notice" client_said 1 \
	"That's 2,001 blocks, more than the 2,000 real blocks you can place at once, so nothing was placed."
check "and nothing went in" eval '[[ $(clients_count 40 -60 100 59 -60 200 oak_planks) == "0 0" ]] &&
	server_has 40 -60 100 minecraft:air && server_has 59 -60 199 minecraft:air && server_has 40 -60 200 minecraft:air'
debug_log=run/server/logs/debug.log
before=$(wc -l < "$debug_log")
check "a place of 2,000 blocks and, right after it, one of 1 more are sent" eval \
	'[[ $(drv 1 "sendplace up 40.5 -60 100.5 shape 40 -60 100 59 -60 199 minecraft:oak_planks" "sendplace up 40.5 -60 200.5 shape 40 -60 200 40 -60 200 minecraft:oak_planks" | sed "s/^.* => //" | tr "\n" " ") == "2000 blocks 1 blocks " ]]'
check "the second gets the notice of the cap a second" client_said 1 \
	"That would place more than 2,000 real blocks in a second, so nothing was placed."
# The server's 2,000 blocks match a box of oak planks /fill makes at y -40 for the check.
cap_tag=$(new_tag)
check "on the server, all 2,000 are real oak planks and the 1 more isn't placed, as both clients show" eval \
	'arrive 59 -60 199 minecraft:oak_planks && arrive 40 -60 100 minecraft:oak_planks &&
	[[ $(server_said "$cap_tag" "cmd fill 40 -40 100 59 -40 199 minecraft:oak_planks" \
		"cmd execute if blocks 40 -60 100 59 -60 199 40 -40 100 all run say $cap_tag-hit" \
		"cmd fill 40 -40 100 59 -40 199 minecraft:air") == 1 ]] &&
	server_has 40 -60 200 minecraft:air && [[ $(clients_count 40 -60 100 59 -60 200 oak_planks) == "2000 2000" ]]'
check "and they used no item" eval '[[ $(items minecraft:oak_planks) == 1 ]]'

check "neither client's log has a self-check warning" eval \
	'! grep -F "Self-check:" /out/client1.log /out/client2.log >> /out/checks.log'

# Spawn protection: the dev server starts with none (build.gradle's prepareDevServer), and Minecraft reads it only as
# the server starts, so the server starts again with Minecraft's usual 16 blocks, and both games join it again.
stop_client 1
stop_client 2
stop_server
# The server writes its debug log out in full as it stops, and moves it aside as it starts again.
took=$(tail -n +$((before + 1)) "$debug_log" | grep -E "Building Planner placed [0-9]+ of .*'s 2000 real blocks" | head -1 |
	sed -nE 's/.* in ([0-9]+) ms.*/\1/p' || true)
echo "  the server placed the 2,000 blocks in ${took:-?} ms" | tee -a /out/checks.log
check "the server logs the time it took to place the 2,000" eval '[[ -n $took ]]'
sed -i 's/^spawn-protection=.*/spawn-protection=16/' run/server/server.properties
server_log=/out/server-protected.log
start_server "" server-protected || exit 1
start_client 1
start_client 2
check "with spawn protection, both clients start a session again" eval \
	'drv 1 "await session" "await noscreen" > /dev/null && drv 2 "await session" "await noscreen" > /dev/null'
settings_screen 1
check "after the restart, Planner1's \"Place real blocks\" shows on, as saved" eval \
	'[[ $(row 1 "Place real blocks") == "ON "* ]]'
drv 1 "close" > /dev/null
# Planner2, who isn't an operator, near the spawn: a Line of 7 from 2 -60 4 to 8 -60 4, within the spawn protection's
# 16 blocks, drawn and placed with Planner2's own clicks.
drv 1 "cmd tp Planner2 4.5 -60 0.5 0 30" "cmd item replace entity Planner2 weapon.mainhand with minecraft:oak_planks 7" \
	"cmd tp @s 5.5 -60 -2.5 0 30" "wait 40" > /dev/null
drv 2 "ebmode LINE" "wait 5" > /dev/null
draw 2 2 -60 4 8 -60 4
check "Planner2's Line within the spawn protection draws 7 blocks" preview_is 2 7
place_click 2
check "Planner2's click gets the notice" client_said 2 "7 blocks weren't placed: the server doesn't allow them there."
check "and nothing went in" lands minecraft:oak_planks oak_planks 2 -60 4 8 -60 4 0

# Turned off, a shape draws ghost blocks again: a Line of 5 from 100 -60 46 to 104 -60 46.
settings_screen 1
real_switch 1
save_settings 1
check "a click on the switch and \"Save\" turn the setting off" setting_saved 1 false
drv 1 "window 854 480 2" "cmd gamemode survival @s" "ebmode LINE" "cmd tp @s 102.5 -60 41.5 180 30" \
	"cmd tp Planner2 103.5 -60 38.5 180 30" "wait 40" > /dev/null
stock minecraft:oak_planks 5
aim 1 100 -60 46
check "Line's controls line reads \"[Right-click: start placing]\" again" eval \
	'[[ $(val 1 modehud) == "[Right-click: start placing][Left-click: start erasing][H: Lock (Off)][Esc: exit mode]" ]]'
draw 1 100 -60 46 104 -60 46
place_click 1
check "the Line draws ghost oak planks on both clients" eval \
	'drv 1 "await block 104 -60 46 minecraft:oak_planks" > /dev/null &&
	drv 2 "await block 104 -60 46 minecraft:oak_planks" > /dev/null && ghosts_are 1 100 104 -60 46 minecraft:oak_planks 5'
check "places no real block and uses no item" eval \
	'lands minecraft:oak_planks oak_planks 100 -60 46 104 -60 46 0 && [[ $(items minecraft:oak_planks) == 5 ]]'
# And a paste draws ghost blocks: a copy of the real Line from 100 -60 40 to 104 -60 40, pasted at 100 -60 48.
drv 1 "cmd tp @s 102.5 -60 52.5 180 30" "wait 20" > /dev/null
copy_button
box 100.5 -59 40.5 -59 104 40 -60
check "a box around the real Line copies its 5 blocks" client_said 1 "Copied 5 blocks."
drv 1 "lookat 102.5 -60 48.5" "wait 5" > /dev/null
check "the copy rests from 100 -60 48 to 104 -60 48" drv 1 "await copy at 100,-60,48..104,-60,48"
check "with its controls line reading \"[Right-click: paste]\" again" eval \
	'[[ $(val 1 modehud) == "[Right-click: paste][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Off)][Esc/Left-click: close]" ]]'
place_click 1
check "the paste writes ghost oak planks on both clients, sending an edit and no place" eval \
	'drv 1 "await block 104 -60 48 minecraft:oak_planks" > /dev/null &&
	drv 2 "await block 100 -60 48 minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 placesent) == none && $(val 1 holdlog) == *" edits=1 "* ]]'
check "and places no real block and uses no item" eval \
	'lands minecraft:oak_planks oak_planks 100 -60 48 104 -60 48 0 && [[ $(items minecraft:oak_planks) == 5 ]]'
attack_click 1
check "nor does either client's log have a self-check warning since they started again" eval \
	'! grep -F "Self-check:" /out/client1.log /out/client2.log >> /out/checks.log'
