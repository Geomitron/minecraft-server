# Servers without Building Planner (0.10.0): the dev server started with the add-on left off its classpath and with
# spawn protection, both clients having the add-on. Both clients start a local session at joining and send none of
# Building Planner's payloads; a Line of 4 oak planks draws ghost blocks on Planner1's client alone and no real block,
# and Ctrl+Z and Ctrl+Y take it back and forward; real oak planks placed by hand, by /setblock and while Planner1 is
# away fill ghost oak planks, and one the server refuses Planner2 leaves its ghost block; after Planner1 leaves, the
# area's file is in the game folder, and the ghost blocks show again after a rejoin and after the game restarts; a file
# made unreadable stays as it was, and its ghost blocks don't show. What needs a server that runs Building Planner
# doesn't act: in Disable, Left Ctrl with the wheel changes the hotbar slot, a stair placed into a ghost stair with
# "Match ghost blocks" on faces as Minecraft places it, and with "Place real blocks" on a Line draws ghost blocks; and a
# Litematica placement of Planner1's own marks extra blocks, while the tiles mark none; a box counted for a material
# list, saved, with the panel on, comes back after a rejoin. With the server restarted with Building Planner, Planner1
# sees the shared ghost blocks without its local ones and no line above "Building Planner" in the radial menu, and a
# copy there of ghost and real blocks stays through a trip to the Nether and, with the server restarted without it
# again, pastes as ghost blocks where the local ghost blocks show again. There, the radial menu's line "(client only)"
# sits a row above "Building Planner" at 1920 by 1080, GUI scales 2 and 4, and a row above a hovered mode's
# description at 854 by 480. Real blocks are checked on the server with server_has.
# Sourced by inside.sh, which provides drv, val, check, xdo, click, keys, use_click, attack_click, client_said,
# wait_for, start_server, stop_server, start_client, stop_client and $ADDON.

server_log=/out/server-without-addon.log
planner1=527e7f9d-ae90-3a44-aa88-30a47a9cdf34
# The folder Planner1's game keeps its ghost blocks in for the dev server, and the area file of the blocks near 0 0.
folder1=run/client1/building-planner/servers/localhost_25565/$planner1
area1=$folder1/layer/minecraft/overworld/0.0.dat

# half <n>: n + 0.5.
half() {
	awk -v n="$1" 'BEGIN { print n + 0.5 }'
}

# aim <client> <x y z>: the player looks at the middle of the top of the block below x y z, so a click there starts a
# shape at x y z.
aim() {
	drv "$1" "lookat $(half "$2") $3 $(half "$4")" "wait 5" > /dev/null
}

# draw <client> <x1 y1 z1> <x2 y2 z2>: a Line's first click aimed at the top of the block under x1 y1 z1, then the aim
# at the middle of the side of x2 y2 z2 facing the player, who stands north of it, and the click that places it.
draw() {
	aim "$1" "$2" "$3" "$4"
	use_click "$1"
	drv "$1" "lookat $(half "$5") $(half "$6") $7" "wait 5" > /dev/null
	use_click "$1"
	drv "$1" "wait 10" > /dev/null
}

# shots <client> <name> [command]: that client's screen at 854 by 480 and at 1920 by 1080, GUI scale 2, once the chat
# has faded, as <name>-<size>-gui2; the window is 854 by 480 again after. The driver command, if given, runs after each
# shot, so checks.log has its answer at both sizes (the screenbox of the change, say, for outlining it).
shots() {
	local after=${3:-mark}
	drv "$1" "wait 210" "notoasts" "wait 5" "shot $2-854x480-gui2" "$after" "window 1920 1080 2" "wait 20" "notoasts" \
		"shot $2-1920x1080-gui2" "$after" "window 854 480 2" "wait 20" > /dev/null
}

tags=0
# server_has <x y z> <block>: the server's own world has that block there, as /execute if block finds it; the server
# says a word no line of its log has yet when it has, and another once it has checked.
server_has() {
	tags=$((tags + 1))
	local tag=local$tags
	drv 1 "cmd execute if block $1 $2 $3 $4 run say $tag-hit" "cmd say $tag-end" > /dev/null &&
		wait_for "$server_log" "$tag-end" 20 && grep -qF -- "$tag-hit" "$server_log"
}

# switch_setting <client> <row>: opens "Settings", as the radial menu's button does, clicks the row's switch and then
# the screen's "Save" button, which closes it, with the client's own mouse.
client_config=run/client1/config/buildingplanner-client.properties
switch_setting() {
	local text x y
	drv "$1" "close" "settings" "await screen Settings" "wait 10" > /dev/null
	read -r text x y < <(val "$1" "settingrow $2")
	click "$1" "$x" "$y"
	drv "$1" "wait 5" > /dev/null
	read -r x y < <(val "$1" "widget 0 Save")
	click "$1" "$x" "$y"
	drv "$1" "await noscreen" "wait 5" > /dev/null
}

# no_planner_payload <client>: the driver's counter of the custom payloads leaving that client counts none of
# Building Planner's.
no_planner_payload() {
	[[ $(val "$1" bpsent) == none ]]
}

# A ghost oak plank in the shared layer of the server with Building Planner, at 80 -60 80, for the checks of the copy
# between servers at the end.
drv 1 "await noscreen" "await session" "cmd tp @s 80.5 -60 77.5 0 30" "await loaded minecraft:overworld 80 80" \
	"wait 20" "edit 80 -60 80 80 -60 80 minecraft:oak_planks" "await block 80 -60 80 minecraft:oak_planks" "wait 20" \
	> /dev/null

# The dev server again, with the add-on left off its classpath, and with spawn protection, which the dev server is
# otherwise without, so it refuses a block Planner2, who isn't an operator, places near the world's spawn.
stop_client 1
stop_client 2
stop_server
sed -i 's/^spawn-protection=.*/spawn-protection=16/' run/server/server.properties
check "the server starts without Building Planner" eval 'start_server "$ADDON" server-without-addon &&
	! grep -qF "Building Planner loaded" "$server_log"'
start_client 1
start_client 2

# The loading screen swallows key presses, so each check waits for it to close.
check "client 1 starts a local session at joining" eval 'drv 1 "await noscreen" "await session" > /dev/null &&
	[[ $(val 1 sessionkind) == local ]]'
check "client 2 starts a local session at joining" eval 'drv 2 "await noscreen" "await session" > /dev/null &&
	[[ $(val 2 sessionkind) == local ]]'
check "and each says in its log where it keeps the ghost blocks" eval \
	'grep -qF "kept on this computer, in building-planner/servers/localhost_25565/$planner1" /out/client1.log'

# A Line of 4 oak planks from 20 -60 20 eastward, drawn by Planner1 standing north of it; Planner2 stands beside it.
drv 1 "window 854 480 2" "cmd setworldspawn 0 -60 0" "cmd clear @s" \
	"cmd item replace entity @s weapon.mainhand with minecraft:oak_planks 64" "cmd tp @s 21.5 -60 16.5 0 30" \
	"cmd tp Planner2 24.5 -60 16.5 0 30" "ebmode LINE" "wait 40" "await loaded minecraft:overworld 20 20" > /dev/null
drv 2 "await loaded minecraft:overworld 20 20" > /dev/null
draw 1 20 -60 20 23 -60 20
check "a Line of 4 oak planks draws 4 ghost blocks on Planner1's client" eval \
	'drv 1 "await block 23 -60 20 minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 "countblocks 20 23 -60 20 minecraft:oak_planks") == 4 ]]'
check "and places no real block on the server" eval '! server_has 20 -60 20 minecraft:oak_planks &&
	! server_has 23 -60 20 minecraft:oak_planks && [[ $(val 1 "real 20 -60 20") == minecraft:air ]]'
check "and draws none on Planner2's client" eval \
	'drv 2 "wait 20" > /dev/null && [[ $(val 2 "countblocks 20 23 -60 20 minecraft:oak_planks") == 0 ]]'
# The shots from a few blocks farther back, in Disable, where no preview and no line of controls shows.
drv 1 "ebmode DISABLED" "cmd tp @s 21.5 -60 12.5 0 25" "wait 20" > /dev/null
shots 1 local-ghost-blocks "screenbox 20 -60 20 23 -60 20"
drv 1 "ebmode LINE" "cmd tp @s 21.5 -60 16.5 0 30" "wait 20" > /dev/null
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "Ctrl+Z takes the Line back" eval 'drv 1 "await block 20 -60 20 minecraft:air" > /dev/null &&
	[[ $(val 1 "countblocks 20 23 -60 20 minecraft:oak_planks") == 0 ]]'
keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
check "and Ctrl+Y draws it again" eval 'drv 1 "await block 23 -60 20 minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 "countblocks 20 23 -60 20 minecraft:oak_planks") == 4 ]]'
check "and no Effortless Building packet that changes real blocks leaves the client" \
	eval '! val 1 "ebsent" | grep -qE "place_build_mode|break_build_mode|undo|redo"'
check "neither client sent any of Building Planner's payloads" eval 'no_planner_payload 1 && no_planner_payload 2'

# The files: leaving writes the area's file, which the next session reads, after a rejoin and after a restart.
check "after Planner1 leaves, the area's file is in its game folder for the server" eval \
	'drv 1 "leave" "wait 40" > /dev/null && [[ -s $area1 ]]'
drv 1 "join localhost:25565" > /dev/null
check "and after a rejoin, the Line shows again" eval 'drv 1 "await session" "await noscreen" \
	"await block 23 -60 20 minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 "countblocks 20 23 -60 20 minecraft:oak_planks") == 4 ]]'
check "and Ctrl+Z still takes it back, then Ctrl+Y draws it again" eval \
	'keys 1 keydown ctrl key z sleep 0.3 keyup ctrl && drv 1 "await block 20 -60 20 minecraft:air" > /dev/null &&
	keys 1 keydown ctrl key y sleep 0.3 keyup ctrl && drv 1 "await block 20 -60 20 minecraft:oak_planks" > /dev/null'
drv 1 "leave" "wait 40" > /dev/null
stop_client 1
start_client 1
check "and after the game restarts, it shows again" eval 'drv 1 "await noscreen" "await session" \
	"await block 23 -60 20 minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 "countblocks 20 23 -60 20 minecraft:oak_planks") == 4 ]]'
check "and the restarted game sends none of Building Planner's payloads" no_planner_payload 1

# An unreadable file: the game leaves its ghost blocks out and never writes over it, even after an edit in its area.
drv 1 "leave" "wait 40" > /dev/null
stop_client 1
cp "$area1" /tmp/area1.dat
printf 'not a layer file' > "$area1"
cp "$area1" /tmp/area1-unreadable.dat
start_client 1
check "a file made unreadable leaves its ghost blocks out" eval 'drv 1 "await noscreen" "await session" \
	"await loaded minecraft:overworld 20 20" "wait 40" > /dev/null &&
	[[ $(val 1 "countblocks 20 23 -60 20 minecraft:oak_planks") == 0 ]]'
check "and the game says so in its log" grep -qF "couldn't read" /out/client1.log
drv 1 "edit 21 -60 22 21 -60 22 minecraft:stone" "await block 21 -60 22 minecraft:stone" "leave" "wait 40" > /dev/null
check "and stays as it was after an edit in its area and a leave" cmp -s "$area1" /tmp/area1-unreadable.dat
cp /tmp/area1.dat "$area1"
drv 1 "join localhost:25565" > /dev/null
check "and with the file put back, the Line shows again" eval 'drv 1 "await session" "await noscreen" \
	"await block 23 -60 20 minecraft:oak_planks" > /dev/null'

# Ghost blocks a real block fills, in Disable, where right clicks are Minecraft's: a real oak plank Planner1 places
# by hand into a ghost oak plank at 26 -60 20, standing within reach, once the server has it, and one /setblock puts
# at 28 -60 20.
drv 1 "ebmode DISABLED" "cmd tp @s 26.5 -60 17.5 0 30" "await loaded minecraft:overworld 26 20" "wait 20" \
	"edit 26 -60 20 26 -60 20 minecraft:oak_planks" "edit 28 -60 20 28 -60 20 minecraft:oak_planks" \
	"await inlayer 26 -60 20 minecraft:oak_planks" "await inlayer 28 -60 20 minecraft:oak_planks" \
	"lookat 26.5 -60 20.5" "wait 5" > /dev/null
use_click 1
check "a real oak plank Planner1 places into a ghost oak plank removes it" eval \
	'drv 1 "await real 26 -60 20 minecraft:oak_planks" "await inlayer 26 -60 20 minecraft:air" > /dev/null &&
	server_has 26 -60 20 minecraft:oak_planks'
check "and so does one /setblock puts in a chunk Planner1 has loaded" drv 1 \
	"cmd setblock 28 -60 20 minecraft:oak_planks" "await real 28 -60 20 minecraft:oak_planks" \
	"await inlayer 28 -60 20 minecraft:air"
check "and the ghost blocks beside them stay" eval '[[ $(val 1 "layer 23 -60 20") == minecraft:oak_planks ]]'

# One set while Planner1 is farther away than the view distance: a ghost oak plank at 300 -60 300, and a ghost stone
# beside it that no real block fills, which shows when the column is back. The server keeps the chunk loaded.
drv 1 "cmd tp @s 300.5 -60 296.5 0 30" "await loaded minecraft:overworld 300 300" "wait 40" \
	"edit 300 -60 300 300 -60 300 minecraft:oak_planks" "edit 302 -60 300 302 -60 300 minecraft:stone" \
	"await inlayer 300 -60 300 minecraft:oak_planks" "cmd forceload add 300 300" "cmd tp @s 21.5 -60 16.5 0 30" \
	"wait 60" "cmd setblock 300 -60 300 minecraft:oak_planks" "wait 20" > /dev/null
check "a real oak plank set while Planner1 is away reaches the server but not Planner1's game" eval \
	'server_has 300 -60 300 minecraft:oak_planks && [[ $(val 1 "real 300 -60 300") != minecraft:oak_planks ]]'
drv 1 "cmd forceload remove 300 300" "cmd tp @s 300.5 -60 296.5 0 30" "await loaded minecraft:overworld 300 300" \
	"await inlayer 302 -60 300 minecraft:stone" > /dev/null
check "and removes its ghost block once Planner1 returns and the chunk loads" eval \
	'drv 1 "await inlayer 300 -60 300 minecraft:air" > /dev/null &&
	[[ $(val 1 "real 300 -60 300") == minecraft:oak_planks ]]'
drv 1 "cmd tp @s 21.5 -60 16.5 0 30" "wait 20" > /dev/null

# Planner2, who isn't an operator, places a real oak plank into a ghost oak plank within 16 blocks of the world's
# spawn: the game shows it until the server refuses it, and the ghost block stays.
drv 1 "cmd item replace entity Planner2 weapon.mainhand with minecraft:oak_planks 64" \
	"cmd tp Planner2 5.5 -60 2.5 0 30" > /dev/null
drv 2 "ebmode DISABLED" "await loaded minecraft:overworld 5 5" "wait 40" "edit 5 -60 5 5 -60 5 minecraft:oak_planks" \
	"await inlayer 5 -60 5 minecraft:oak_planks" "lookat 5.5 -60 5.5" "wait 5" "watchblock 5 -60 5" > /dev/null
use_click 2
check "an oak plank the server refuses Planner2 shows until the server's answer" eval \
	'drv 2 "wait 40" > /dev/null && [[ $(val 2 watched) == *"minecraft:oak_planks"*"| "*"minecraft:air"* ]] &&
	! server_has 5 -60 5 minecraft:oak_planks'
check "and leaves its ghost block" eval '[[ $(val 2 "layer 5 -60 5") == minecraft:oak_planks &&
	$(val 2 "real 5 -60 5") == minecraft:air ]]'

# What needs a server that runs Building Planner doesn't act during a local session. In Disable, with oak stairs in
# hand, Left Ctrl with a notch up changes the hotbar slot, as without a session (step8.sh).
drv 1 "ebmode DISABLED" "cmd item replace entity @s hotbar.4 with minecraft:oak_stairs 64" \
	"cmd tp @s 40.5 -60 37.5 0 30" "await loaded minecraft:overworld 40 40" "wait 20" > /dev/null
keys 1 key 5
drv 1 "lookat 40.5 -60 40.5" "wait 5" > /dev/null
xdo 1 keydown Control_L
drv 1 "await sprint true" "wait 5" > /dev/null
xdo 1 click 4
check "in Disable, Left Ctrl with the wheel changes the hotbar slot" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 slot) == "3 "* ]]'
xdo 1 keyup Control_L
keys 1 key 5
drv 1 "await sprint false" "wait 5" > /dev/null
# With "Match ghost blocks" on, Planner1, 3 blocks north of a ghost oak stair facing east and facing south, places an
# oak stair there: it faces south, as Minecraft places it, and leaves the ghost stair.
switch_setting 1 "Match ghost blocks"
check "\"Match ghost blocks\" is on, as \"Settings\" saved it" grep -q "^match-ghost-turn=true" "$client_config"
drv 1 "edit 40 -60 40 40 -60 40 minecraft:oak_stairs[facing=east]" "await inlayer 40 -60 40 minecraft:oak_stairs" \
	"lookat 40.5 -60 40.5" "wait 5" > /dev/null
use_click 1
check "with it on, a stair placed into a ghost stair facing east faces south, as Minecraft places it" eval \
	'drv 1 "await real 40 -60 40 minecraft:oak_stairs" > /dev/null &&
	[[ $(val 1 "realstate 40 -60 40") == *"facing=south,"* ]] &&
	server_has 40 -60 40 "minecraft:oak_stairs[facing=south]"'
check "and leaves the ghost stair" eval 'drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 "layer 40 -60 40") == minecraft:oak_stairs ]]'
# With "Place real blocks" on, a Line of 4 oak planks from 40 -60 46 eastward draws ghost blocks.
switch_setting 1 "Place real blocks"
check "\"Place real blocks\" is on, as \"Settings\" saved it" grep -q "^place-real-blocks=true" "$client_config"
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:oak_planks 64" "ebmode LINE" \
	"cmd tp @s 41.5 -60 42.5 0 30" "wait 20" > /dev/null
draw 1 40 -60 46 43 -60 46
check "with it on, a Line of 4 oak planks draws 4 ghost blocks" eval \
	'drv 1 "await block 43 -60 46 minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 "countblocks 40 43 -60 46 minecraft:oak_planks") == 4 ]]'
check "and places no real block on the server" eval '! server_has 40 -60 46 minecraft:oak_planks &&
	! server_has 43 -60 46 minecraft:oak_planks && [[ $(val 1 "real 40 -60 46") == minecraft:air ]]'
check "and still sends none of Building Planner's payloads" no_planner_payload 1
switch_setting 1 "Place real blocks"
switch_setting 1 "Match ghost blocks"
check "both settings are off again, as \"Settings\" saved them" eval \
	'grep -q "^place-real-blocks=false" "$client_config" && grep -q "^match-ghost-turn=false" "$client_config"'

# Litematica's extra blocks (hook L7): placements of Planner1's own, two empty plans of 3 by 3 laid on the ground at
# 60 -61 60 and 64 -61 60 (the driver's oldplans, whose placements stay during a local session), mark the real grass
# in them as extra, as Litematica does; the ground under the Lines' ghost blocks, in a tile's box alone, isn't marked.
drv 1 "ebmode DISABLED" "cmd tp @s 63.5 -60 56.5 0 25" "await loaded minecraft:overworld 60 60" "wait 20" \
	"oldplans 60 -61 60" "wait 40" > /dev/null
check "a Litematica placement of Planner1's own marks a real block where it holds air as extra" eval \
	'[[ $(val 1 "overlay 61 -61 61") == EXTRA && $(val 1 "overlay 65 -61 61") == EXTRA ]]'
check "while no real block in a tile's box alone is marked" eval \
	'[[ $(val 1 "overlay 20 -61 20") == NONE && $(val 1 "overlay 41 -61 46") == NONE ]]'
shots 1 local-placement-extra-blocks "screenbox 60 -61 60 66 -61 62"

# The material lists on the computer, in Planner1's folder for the server: a box around the first Line's 4 ghost oak
# planks, started with the radial menu's "Material list" button and drawn with three clicks, opens its list, which is
# saved as "Line" on its screen, and its Info HUD button turns the panel on. After a rejoin, the game has the list as
# its current one, the panel shows it, and "Line" and "Last counted" are among its lists.
lists_file=$folder1/material-lists/$planner1.dat
# radial_button <name>: a real click on the radial menu's button of that name, the menu's key (left Alt) held.
radial_button() {
	local x y
	xdo 1 keydown Alt_L
	drv 1 "await screen Build Modes" "wait 10" > /dev/null
	read -r x y < <(val 1 "widget 0 $1")
	xdo 1 mousemove --window %w "$x" "$y"
	drv 1 "wait 5" > /dev/null
	xdo 1 click 1
	drv 1 "wait 5" > /dev/null
	xdo 1 keyup Alt_L
	drv 1 "await noscreen" "wait 5" > /dev/null
}
# radial_line <width> <height> <gui scale> <single or -> [shot]: in a window of that size at that GUI scale, opens the
# radial menu (left Alt held), with the pointer over "Single" (about 33 right of the menu's middle and 35 above it) if
# asked, so its name and description show, and shoots it as the shot, if given, once the chat has faded. The driver's
# radialline goes to $line, and its cornertext to $corner, both to the log too.
radial_line() {
	drv 1 "window $1 $2 $3" "wait 20" > /dev/null
	if [[ -n ${5:-} ]]; then
		drv 1 "wait 210" > /dev/null
	fi
	xdo 1 keydown Alt_L
	drv 1 "await screen Build Modes" "wait 10" > /dev/null
	if [[ $4 == single ]]; then
		xdo 1 mousemove --window %w $(($1 / 2 + 33 * $3)) $(($2 / 2 - 35 * $3))
		drv 1 "wait 10" > /dev/null
	fi
	if [[ -n ${5:-} ]]; then
		drv 1 "notoasts" "wait 5" "shot $5" > /dev/null
	fi
	line=$(val 1 radialline)
	corner=$(val 1 cornertext)
	echo "  the radial menu at $1x$2, GUI scale $3: the line $line; the corner $corner" >> /out/checks.log
	xdo 1 keyup Alt_L
	drv 1 "await noscreen" "wait 5" > /dev/null
}
# list_box <ax ay az> <y1> <x2 z2> <y3>: box's clicks for a material list, the last one a press and a release that the
# list's screen, which it opens, takes from the game.
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
# mat_click <label>: a real click on the button of the open material list screen whose label starts with the text.
mat_click() {
	local pos
	pos=$(val 1 "malilibbutton $1")
	[[ $pos != none ]] && click 1 $pos
}
# save_as <name>: "Save list" on the open material list screen, the name typed over the date with real key presses,
# and "Save", which returns to the list's screen.
save_as() {
	local x y
	mat_click "Save list"
	drv 1 "await screen Save list" > /dev/null
	keys 1 key ctrl+a BackSpace
	keys 1 type --delay 20 -- "$1"
	drv 1 "wait 5" > /dev/null
	read -r x y < <(val 1 "widget 0 Save")
	click 1 "$x" "$y"
	drv 1 "await screen Material list for the blocks in the box" "wait 20" > /dev/null
}
drv 1 "ebmode SINGLE" "cmd tp @s 21.5 -60 16.5 0 30" "wait 20" > /dev/null
radial_button "Material list"
list_box 20.5 -59 20.5 -59 23 20 -60
check "a box around the first Line's ghost blocks opens its material list: 4 oak planks, all missing" eval \
	'drv 1 "await screen Material list for the blocks in the box" "wait 10" > /dev/null &&
	[[ $(val 1 currentlist) == *" | minecraft:oak_planks 4/4/0"* ]]'
counted_list=$(val 1 currentlist)
echo "  the counted list: $counted_list" >> /out/checks.log
save_as Line
mat_click "Info HUD"
drv 1 "close" "wait 10" > /dev/null
panel=$(val 1 matpanel)
echo "  the panel: $panel" >> /out/checks.log
check "saved as \"Line\", with its Info HUD on, the panel shows it" eval \
	'[[ $panel == "Material list | All Hold Need | "* && $(val 1 listindex) == *"Line "* ]]'
check "after Planner1 leaves, the lists' file is in Planner1's folder for the server" eval \
	'drv 1 "leave" "wait 40" > /dev/null && [[ -s $lists_file ]]'
drv 1 "join localhost:25565" > /dev/null
check "and after a rejoin, the game has the counted list as its current one" eval \
	'drv 1 "await session" "await noscreen" "wait 20" > /dev/null && [[ $(val 1 currentlist) == "$counted_list" ]]'
check "the panel shows it again" eval '[[ $(val 1 matpanel) == "$panel" ]]'
check "and \"Line\" and \"Last counted\" are among the lists" eval \
	'[[ $(val 1 listindex) == "last counted 4x2x1 | Line 4x2x1" ]]'
echo "  the lists after the rejoin: $(val 1 listindex)" >> /out/checks.log

# The copy between servers, kept while the game runs, which goes on throughout, the server stopping under it each time.
# With the server restarted with Building Planner, Planner1 sees the shared ghost oak plank drawn at the start, without
# its local ghost blocks; a copy there of that plank and a real stone beside it stays, put away, through a trip to the
# Nether, and, with the server restarted without Building Planner again, where the local ghost blocks show again and
# the shared plank doesn't, the radial menu's Paste button brings it back and a right click pastes it as ghost blocks.
# copy_box <ax ay az> <x2 z2> <y3>: the radial menu's Copy button, then the box's three clicks: the first corner where
# the player aims, the opposite one on its level, and the height.
copy_box() {
	radial_button Copy
	drv 1 "lookat $1 $2 $3" "wait 5" > /dev/null
	attack_click 1
	drv 1 "lookat $(half "$4") $2 $(half "$5")" "wait 5" > /dev/null
	attack_click 1
	drv 1 "lookat $(half "$4") $(half "$6") $(half "$5")" "wait 10" > /dev/null
	attack_click 1
	drv 1 "wait 10" > /dev/null
}
# near_both: Planner1 stands where its game has the columns of 20 -60 20 and 80 -60 80.
near_both() {
	drv 1 "cmd tp @s 50.5 -60 46.5 0 30" "await loaded minecraft:overworld 20 20" \
		"await loaded minecraft:overworld 80 80" "wait 40" > /dev/null
}
stop_server server-without-addon
check "with the server restarted with Building Planner, Planner1's game starts its session at a rejoin" eval \
	'start_server "" server-with-addon && grep -qF "Building Planner loaded" /out/server-with-addon.log &&
	drv 1 "join localhost:25565" "await session" "await noscreen" > /dev/null && [[ $(val 1 sessionkind) == server ]]'
near_both
check "and Planner1 sees the shared ghost oak plank, without its local ghost blocks" eval \
	'drv 1 "await block 80 -60 80 minecraft:oak_planks" > /dev/null && [[ $(val 1 "layer 20 -60 20") == minecraft:air ]]'
radial_line 854 480 2 -
check "and its radial menu doesn't show the line above \"Building Planner\"" eval \
	'[[ $line == none && $corner == "Building Planner | "* ]]'
drv 1 "cmd setblock 81 -60 80 minecraft:stone" "cmd tp @s 80.5 -60 77.5 0 30" "await real 81 -60 80 minecraft:stone" \
	"wait 20" > /dev/null
copy_box 80.5 -59 80.5 81 80 -60
check "a copy of the shared ghost oak plank and a real stone beside it holds both" eval \
	'[[ $(val 1 copy) == "at "*" NONE NONE unselected 2 blocks" ]]'
check "and stays through a trip to the Nether, put away" eval \
	'drv 1 "cmd execute in minecraft:the_nether run tp @s 80 70 80" "await loaded minecraft:the_nether 80 80" "wait 20" \
	> /dev/null && [[ $(val 1 copy) == "away NONE NONE unselected 2 blocks" ]]'
drv 1 "cmd execute in minecraft:overworld run tp @s 80.5 -60 77.5 0 30" "await loaded minecraft:overworld 80 80" \
	"wait 20" > /dev/null
stop_server server-with-addon
server_log=/out/server-without-addon-again.log
check "with the server restarted without Building Planner again, Planner1's game starts a local session at a rejoin" \
	eval 'start_server "$ADDON" server-without-addon-again && ! grep -qF "Building Planner loaded" "$server_log" &&
	drv 1 "join localhost:25565" "await session" "await noscreen" > /dev/null && [[ $(val 1 sessionkind) == local ]]'
near_both
check "where its local ghost blocks show again, and the shared plank doesn't" eval \
	'drv 1 "await block 20 -60 20 minecraft:oak_planks" > /dev/null && [[ $(val 1 "layer 80 -60 80") == minecraft:air ]]'
check "and the copy is still there, put away" eval '[[ $(val 1 copy) == "away NONE NONE unselected 2 blocks" ]]'
drv 1 "cmd tp @s 90.5 -60 77.5 0 30" "await loaded minecraft:overworld 90 80" "wait 20" > /dev/null
radial_button Paste
aim 1 90 -60 80
drv 1 "wait 10" > /dev/null
spot=$(val 1 copy)
echo "  the copy the Paste button brought to the aim: $spot" >> /out/checks.log
read -r px py pz < <(sed -E 's/^at ([-0-9]+),([-0-9]+),([-0-9]+)[.][.].*/\1 \2 \3/' <<< "$spot")
use_click 1
check "the Paste button brings it to the aim, and a right click pastes it as ghost blocks: the oak plank and the stone" \
	eval '[[ $spot == "at "* ]] && drv 1 "await block $px $py $pz minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 "layer $px $py $pz") == minecraft:oak_planks && $(val 1 "layer $((px + 1)) $py $pz") == minecraft:stone ]]'
check "and places no real block on the server" eval \
	'! server_has "$px" "$py" "$pz" minecraft:oak_planks && ! server_has "$((px + 1))" "$py" "$pz" minecraft:stone'

# The radial menu's line above "Building Planner" during a local session, at the row the driver's radialline gives,
# with the strings drawn one row below it: the menu's name in the corner, or, on a screen up to 256 high in the GUI's
# units, a hovered mode's description; the hotbar's counts can share the name's row. The line in each window size goes
# to a shot, radial-line-<size>-gui<scale>.
radial_line 1920 1080 2 - radial-line-1920x1080-gui2
check "with the radial menu open, the line above \"Building Planner\" reads \"(client only)\", 4 from the right edge, a row above the name, at 1920 by 1080, GUI scale 2" \
	eval '[[ $line == "(client only) | 520 4 | "* && " / ${line##* | } / " == *" / Building Planner / "* ]]'
radial_line 1920 1080 4 - radial-line-1920x1080-gui4
check "and at GUI scale 4" eval \
	'[[ $line == "(client only) | 250 4 | "* && " / ${line##* | } / " == *" / Building Planner / "* ]]'
radial_line 854 480 2 single radial-line-854x480-gui2
check "and at 854 by 480, GUI scale 2, a row above the hovered mode's description" eval \
	'[[ $line == "(client only) | 210 4 | "* && ${line##* | } != none && ${line##* | } != "Building Planner" ]]'
