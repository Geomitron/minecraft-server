# Ghost blocks the real world fills: real blocks placed by hand, by /fill, by a piston and by flowing water remove the
# ghost blocks they fill, on both clients within a tick and with no glow; a block placed another way leaves its ghost
# block; breaking a filled block brings none back; a paste onto built blocks changes nothing; and ghost blocks saved over
# matching real blocks while the server ran without the add-on go once their chunk loads. Shots of a wall half built,
# before and after, at 854 by 480 and 1920 by 1080, GUI scale 2. Sourced by inside.sh, which provides drv, val, check,
# use_click, start_server, stop_server, start_client, stop_client and $ADDON. Everything happens more than 16 blocks
# from the world's spawn, where Planner2, who isn't an operator, may build.

# no_self_check_warning <client>...: the clients' logs have no warning from the game's self-check; any found goes to
# checks.log.
no_self_check_warning() {
	local n found=0
	for n in "$@"; do
		if grep -F "Self-check:" "/out/client$n.log" >> /out/checks.log; then
			found=1
		fi
	done
	((found == 0))
}

# layer_a_tick_later <client> <x y z> <block>: once that client shows the real block there, its copy of the layer
# there a tick later.
layer_a_tick_later() {
	drv "$1" "await real $2 $3 $4 $5" "wait 1" "layer $2 $3 $4" | tail -1 | sed 's/^.* => //'
}

# filled_within_a_tick <first client> <x y z> <block>: on the first client, which learns of the real block from the
# server, and then on the other, the ghost block there is gone a tick after the real block shows, and neither client
# shows a glow.
filled_within_a_tick() {
	local other=$((3 - $1))
	[[ $(layer_a_tick_later "$1" "$2" "$3" "$4" "$5") == minecraft:air ]] &&
		[[ $(layer_a_tick_later "$other" "$2" "$3" "$4" "$5") == minecraft:air ]] &&
		[[ -z $(val 1 glows) && -z $(val 2 glows) ]]
}

# both_layer <x y z> <block>: both clients' copies of the layer hold that block there.
both_layer() {
	[[ $(val 1 "layer $1 $2 $3") == "$4" && $(val 2 "layer $1 $2 $3") == "$4" ]]
}

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

# A ghost wall of stone bricks from 40 -60 40 to 47 -58 40, ghost water at 55 -60 40, and two ghost oak stairs facing
# north at 70 -60 40 and 71 -60 40, where water from the waterlogged one stays off the wall. Both players stand south of
# the wall, facing it, holding stone bricks, in Effortless Building's Disable mode, where their clicks are vanilla's.
drv 1 "cmd tp @s 40.5 -60 37.5 0 30" "cmd tp Planner2 47.5 -60 37.5 0 30" \
	"cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" \
	"cmd item replace entity Planner2 weapon.mainhand with minecraft:stone_bricks" "ebmode DISABLED" "wait 40" \
	"edit 40 -60 40 47 -58 40 minecraft:stone_bricks" "edit 55 -60 40 55 -60 40 minecraft:water" \
	"edit 70 -60 40 71 -60 40 minecraft:oak_stairs[facing=north]" > /dev/null
drv 2 "ebmode DISABLED" > /dev/null
for n in 1 2; do
	check "client $n sees the ghost wall, water and stairs" drv "$n" "await block 40 -60 40 minecraft:stone_bricks" \
		"await block 47 -58 40 minecraft:stone_bricks" "await block 55 -60 40 minecraft:water" \
		"await block 71 -60 40 minecraft:oak_stairs"
done
check "and Planner1's edit glow fades on client 2" drv 2 "await noglow"

# By hand: Planner1 places a stone brick on the grass at 40 -60 40, and Planner2 one at 47 -60 40.
drv 1 "lookat 40.5 -60 40.5" "wait 5" > /dev/null
use_click 1
check "a stone brick Planner1 places by hand removes its ghost block within a tick, for both, with no glow" \
	filled_within_a_tick 2 40 -60 40 minecraft:stone_bricks
drv 2 "lookat 47.5 -60 40.5" "wait 5" > /dev/null
use_click 2
check "a stone brick Planner2 places by hand removes its ghost block within a tick, for both, with no glow" \
	filled_within_a_tick 1 47 -60 40 minecraft:stone_bricks
check "the ghost blocks beside them stay" eval 'both_layer 41 -60 40 minecraft:stone_bricks &&
	both_layer 40 -59 40 minecraft:stone_bricks'

# By /fill: 41 -60 40 and 42 -60 40.
drv 1 "cmd fill 41 -60 40 42 -60 40 minecraft:stone_bricks" > /dev/null
check "stone bricks /fill places remove their ghost blocks within a tick, for both, with no glow" eval \
	'filled_within_a_tick 1 41 -60 40 minecraft:stone_bricks && filled_within_a_tick 2 42 -60 40 minecraft:stone_bricks'

# By a piston: a piston at 43 -60 42 facing north pushes a stone brick from 43 -60 41 to 43 -60 40 once a redstone
# block powers it.
drv 1 "cmd setblock 43 -60 42 minecraft:piston[facing=north]" "cmd setblock 43 -60 41 minecraft:stone_bricks" \
	"wait 10" "cmd setblock 43 -60 43 minecraft:redstone_block" > /dev/null
check "a stone brick a piston pushes into place removes its ghost block within a tick, for both, with no glow" \
	filled_within_a_tick 2 43 -60 40 minecraft:stone_bricks

# By water: a water source at 55 -60 41 flows into the ghost water at 55 -60 40.
drv 1 "cmd setblock 55 -60 41 minecraft:water" > /dev/null
check "water flowing into ghost water removes it within a tick, for both, with no glow" \
	filled_within_a_tick 2 55 -60 40 minecraft:water

# A stair facing south leaves the ghost stair facing north; one facing north, waterlogged, fills its ghost stair.
drv 1 "cmd setblock 70 -60 40 minecraft:oak_stairs[facing=south]" \
	"cmd setblock 71 -60 40 minecraft:oak_stairs[facing=north,waterlogged=true]" "wait 20" > /dev/null
check "a stair placed facing the other way leaves its ghost block" both_layer 70 -60 40 minecraft:oak_stairs
check "a waterlogged stair facing the same way fills its ghost block" both_layer 71 -60 40 minecraft:air

# Breaking filled blocks brings no ghost block back.
drv 1 "cmd setblock 40 -60 40 minecraft:air" "cmd fill 41 -60 40 42 -60 40 minecraft:air" "wait 20" > /dev/null
check "breaking a filled block brings no ghost block back" eval 'drv 2 "await real 41 -60 40 minecraft:air" > /dev/null &&
	both_layer 40 -60 40 minecraft:air && both_layer 41 -60 40 minecraft:air && both_layer 42 -60 40 minecraft:air'

# A paste onto built blocks: real stone bricks from 40 -60 48 to 43 -59 48, copied and pasted where they stand.
drv 1 "cmd fill 40 -60 48 43 -59 48 minecraft:stone_bricks" "wait 10" > /dev/null
check "Planner1 copies the 8 built blocks and pastes them where they stand" eval \
	'[[ $(val 1 "pastehere 40 -60 48 43 -59 48") == "at 40,-60,48..43,-59,48 "*" 8 blocks" ]]'
drv 1 "wait 20" > /dev/null
check "the paste onto built blocks changes nothing, with no glow" eval 'both_layer 40 -60 48 minecraft:air &&
	both_layer 43 -59 48 minecraft:air && [[ -z $(val 2 glows) ]]'
use_click 1
check "the rest of the ghost wall stays" eval 'both_layer 44 -60 40 minecraft:stone_bricks &&
	both_layer 46 -58 40 minecraft:stone_bricks'

# Shots for Geo: a ghost wall of stone bricks from 60 -60 60 to 67 -57 60, seen by Planner2 from the south, then half
# built with real stone bricks: the two lower rows and half of the third.
drv 1 "cmd tp @s 50 -60 30" "edit 60 -60 60 67 -57 60 minecraft:stone_bricks" \
	"cmd tp Planner2 63.5 -60 68.5 180 5" > /dev/null
check "Planner2 sees the ghost wall for the shots" drv 2 "await block 60 -60 60 minecraft:stone_bricks" \
	"await block 67 -57 60 minecraft:stone_bricks" "await noglow"
drv 2 "window 854 480 2" "notoasts" "wait 40" "shot wall-before-854x480" "wait 10" "window 1920 1080 2" "wait 40" \
	"shot wall-before-1920x1080" "wait 10" > /dev/null
drv 1 "cmd fill 60 -60 60 67 -59 60 minecraft:stone_bricks" "cmd fill 60 -58 60 63 -58 60 minecraft:stone_bricks" \
	> /dev/null
check "the wall's real blocks remove their ghost blocks, and the rest stay" eval \
	'filled_within_a_tick 2 63 -58 60 minecraft:stone_bricks && both_layer 60 -60 60 minecraft:air &&
	both_layer 64 -58 60 minecraft:stone_bricks && both_layer 67 -57 60 minecraft:stone_bricks'
drv 2 "notoasts" "wait 40" "shot wall-after-1920x1080" "wait 10" "window 854 480 2" "wait 40" \
	"shot wall-after-854x480" "wait 10" > /dev/null

# Ghost blocks saved over matching real blocks while the server ran without the add-on: ghost gold blocks at 300 -60 300
# and 301 -60 300 and a ghost oak stair facing north at 302 -60 300, far from where the players stand.
drv 1 "cmd tp @s 300 -60 295 0 30" "wait 60" "edit 300 -60 300 301 -60 300 minecraft:gold_block" \
	"edit 302 -60 300 302 -60 300 minecraft:oak_stairs[facing=north]" > /dev/null
check "Planner1 draws ghost blocks far away" drv 1 "await block 300 -60 300 minecraft:gold_block" \
	"await block 302 -60 300 minecraft:oak_stairs"
drv 1 "cmd tp @s 50 -60 30" "wait 40" > /dev/null
check "so far, neither client's log has a self-check warning" no_self_check_warning 1 2
stop_client 2
stop_client 1
stop_server
start_server "$ADDON" server-without
start_client 1
check "client 1 joins the server without the add-on, with no session" drv 1 "await noscreen" "wait 40" "await nosession"
drv 1 "cmd tp @s 300 -60 295 0 30" "wait 60" "cmd setblock 300 -60 300 minecraft:gold_block" \
	"cmd setblock 301 -60 300 minecraft:gold_block" "cmd setblock 302 -60 300 minecraft:oak_stairs[facing=south]" \
	"wait 20" > /dev/null
check "on the server without the add-on, Planner1 builds over them" eval \
	'[[ $(val 1 "real 301 -60 300") == minecraft:gold_block && $(val 1 "real 302 -60 300") == minecraft:oak_stairs ]]'
drv 1 "cmd tp @s 50 -60 30" "wait 40" > /dev/null
stop_client 1
stop_server server-without
start_server "" server-again
start_client 1
start_client 2
check "with the add-on back, both clients start a session" eval 'drv 1 "await session" && drv 2 "await session"'
check "far from the built blocks, client 1 holds no copy of their column" \
	eval '[[ $(val 1 "tile 18 18") == none && $(val 1 "layer 302 -60 300") == minecraft:air ]]'
drv 1 "cmd tp @s 300 -60 295 0 30" > /dev/null
check "once their chunk loads, the ghost blocks the real ones fill are gone, and the stair facing the other way stays" \
	eval 'drv 1 "await block 302 -60 300 minecraft:oak_stairs" > /dev/null &&
	[[ $(val 1 "layer 300 -60 300") == minecraft:air && $(val 1 "layer 301 -60 300") == minecraft:air ]]'
check "and nothing glows" eval '[[ -z $(val 1 glows) ]]'
check "neither client's log has a self-check warning" no_self_check_warning 1 2
