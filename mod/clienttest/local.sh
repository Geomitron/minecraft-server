# Servers without Building Planner (0.10.0): the dev server started with the add-on left off its classpath and with
# spawn protection, both clients having the add-on. Both clients start a local session at joining and send none of
# Building Planner's payloads; a Line of 4 oak planks draws ghost blocks on Planner1's client alone and no real block,
# and Ctrl+Z and Ctrl+Y take it back and forward; real oak planks placed by hand, by /setblock and while Planner1 is
# away fill ghost oak planks, and one the server refuses Planner2 leaves its ghost block; after Planner1 leaves, the
# area's file is in the game folder, and the ghost blocks show again after a rejoin and after the game restarts; a file
# made unreadable stays as it was, and its ghost blocks don't show. Real blocks are checked on the server with
# server_has. Sourced by inside.sh, which provides drv, val, check, xdo, keys, use_click, attack_click, client_said,
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

# no_planner_payload <client>: the driver's counter of the custom payloads leaving that client counts none of
# Building Planner's.
no_planner_payload() {
	[[ $(val "$1" bpsent) == none ]]
}

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
