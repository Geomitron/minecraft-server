# Step 6's in-game checks: the layer. Shapes and erasing reach the other player at once with the edit glow, undo and
# redo, columns loading as players move, rejoining, dimensions, and a server restart. Sourced by inside.sh, which
# provides drv, val, check, keys, use_click, attack_click, client_said, start_server, stop_server, start_client and
# stop_client. Shapes are drawn in Effortless Building's Single mode with its array modifier, looking straight down, so
# each block's position is known.

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

# Planner1 draws eight stone bricks from 5 -60 5 eastward; Planner2 stands south of them.
drv 1 "cmd tp Planner2 6 -60 -3 0 10" "cmd clear @s" "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" \
	"ebmode SINGLE" "ebarray 7 1 0 0" "cmd tp @s 5 -55 5 0 90" "wait 40" > /dev/null
use_click 1
check "a shape adds ghost blocks for the player who drew it" \
	drv 1 "await block 5 -60 5 minecraft:stone_bricks" "await block 12 -60 5 minecraft:stone_bricks"
check "it reaches the other player at once, glowing with Planner1's name" drv 2 "await block 5 -60 5 minecraft:stone_bricks" \
	"await block 12 -60 5 minecraft:stone_bricks" "await glow Planner1:8" "notoasts" "wait 5" "shot shape-glowing"
check "the glow fades" drv 2 "await noglow"
check "the shape placed no real blocks" eval '[[ $(val 1 "real 5 -60 5") == minecraft:air && $(val 2 "real 12 -60 5") == minecraft:air ]]'
check "the column's tile spans its one section" eval '[[ $(val 1 "tile 0 0") == -64..-49 && $(val 2 "tile 0 0") == -64..-49 ]]'

# Planner2 erases two of them with the break click, holding a block: 9 -60 5 and 10 -60 5.
drv 1 "cmd clear Planner2" "cmd item replace entity Planner2 weapon.mainhand with minecraft:stone_bricks" \
	"cmd tp Planner2 9 -55 5 0 90" > /dev/null
drv 2 "ebmode SINGLE" "ebarray 1 1 0 0" "wait 40" > /dev/null
attack_click 2
check "the break click erases ghost blocks on both clients" eval 'drv 2 "await block 9 -60 5 minecraft:air" \
	"await block 10 -60 5 minecraft:air" && drv 1 "await block 9 -60 5 minecraft:air" "await block 10 -60 5 minecraft:air"'
check "and leaves the real ground and the other ghost blocks" drv 1 "await real 9 -61 5 minecraft:grass_block" \
	"await block 8 -60 5 minecraft:stone_bricks" "await block 11 -60 5 minecraft:stone_bricks"

# Undo and redo with the real keys: Effortless Building's Ctrl+Z and Ctrl+Y go to Building Planner.
keys 2 keydown ctrl key z sleep 0.3 keyup ctrl
check "Ctrl+Z undoes Planner2's erase on both clients" \
	eval 'drv 2 "await block 9 -60 5 minecraft:stone_bricks" && drv 1 "await block 10 -60 5 minecraft:stone_bricks"'
keys 2 keydown ctrl key y sleep 0.3 keyup ctrl
check "Ctrl+Y redoes it on both clients" \
	eval 'drv 2 "await block 9 -60 5 minecraft:air" && drv 1 "await block 10 -60 5 minecraft:air"'
drv 1 "cmd tp @s 5 -60 -3 0 10" "wait 20" > /dev/null
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "Planner1's Ctrl+Z takes their shape away on both clients" \
	eval 'drv 1 "await block 5 -60 5 minecraft:air" "await block 12 -60 5 minecraft:air" && drv 2 "await block 5 -60 5 minecraft:air"'
check "and says it left the blocks Planner2 changed" \
	client_said 1 "2 blocks someone else has changed since were left as they are."
check "a column with no ghost block left has no tile" eval '[[ $(val 1 "tile 0 0") == none && $(val 2 "tile 0 0") == none ]]'
keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
check "Ctrl+Y puts it back, leaving Planner2's erase" eval 'drv 1 "await block 5 -60 5 minecraft:stone_bricks" \
	"await block 9 -60 5 minecraft:air" && drv 2 "await block 12 -60 5 minecraft:stone_bricks" "await block 10 -60 5 minecraft:air"'

# A shape in a chunk Planner2 hasn't loaded: 400 blocks east, past the server's view distance of 6 chunks.
drv 1 "ebarray 0" "cmd tp @s 400 -55 5 0 90" "wait 60" > /dev/null
use_click 1
check "Planner1 draws far away" drv 1 "await block 400 -60 5 minecraft:stone_bricks"
check "Planner2, far from it, holds no copy of that column" eval '[[ $(val 2 "layer 400 -60 5") == minecraft:air ]]'
drv 1 "cmd tp Planner2 402 -60 0 0 10" > /dev/null
check "and sees it after walking there" drv 2 "await block 400 -60 5 minecraft:stone_bricks"

# Planner2 leaves; Planner1 edits near the start; Planner2 rejoins there.
drv 1 "cmd tp Planner2 6 -60 -3 0 10" > /dev/null
drv 2 "wait 40" > /dev/null
stop_client 2
drv 1 "cmd tp @s 5 -60 -3 0 10" "wait 40" "edit 3 -59 3 3 -58 3 minecraft:oak_planks" \
	"await block 3 -58 3 minecraft:oak_planks" > /dev/null
start_client 2
check "client 2 rejoins with a session" drv 2 "await session" "await noscreen"
check "with the edits made while it was away" drv 2 "await block 3 -58 3 minecraft:oak_planks" \
	"await block 5 -60 5 minecraft:stone_bricks" "await block 10 -60 5 minecraft:air"

# A shape in the Nether doesn't show in the overworld, and the overworld's ghost blocks come back on return.
drv 1 "cmd execute in minecraft:the_nether run tp @s 5 70 -3" "wait 100" "edit 5 70 5 7 70 5 minecraft:gold_block" \
	> /dev/null
check "Planner1 draws in the Nether" drv 1 "await block 5 70 5 minecraft:gold_block" "await block 7 70 5 minecraft:gold_block"
check "which doesn't show in the overworld" \
	eval 'drv 2 "wait 40" "await block 5 70 5 minecraft:air" > /dev/null && [[ $(val 2 "layer 5 70 5") == minecraft:air ]]'
drv 1 "cmd execute in minecraft:overworld run tp @s 5 -60 -3 0 10" > /dev/null
check "back in the overworld, Planner1 sees its ghost blocks again" drv 1 "await block 5 -60 5 minecraft:stone_bricks" \
	"await block 3 -58 3 minecraft:oak_planks"
check "and not the Nether's" eval '[[ $(val 1 "layer 5 70 5") == minecraft:air ]]'
drv 1 "notoasts" "wait 5" "shot back-from-the-nether" > /dev/null

# The server, stopped and started again, reads back the same layer.
stop_client 2
stop_client 1
stop_server
start_server "" server-restart
start_client 1
start_client 2
check "after a restart, both clients start a session" eval 'drv 1 "await session" && drv 2 "await session"'
for n in 1 2; do
	check "client $n sees the same ghost blocks after the restart" drv "$n" "await block 5 -60 5 minecraft:stone_bricks" \
		"await block 12 -60 5 minecraft:stone_bricks" "await block 9 -60 5 minecraft:air" \
		"await block 3 -58 3 minecraft:oak_planks"
done
drv 1 "cmd execute in minecraft:the_nether run tp @s 5 70 -3" "wait 40" > /dev/null
check "and the Nether's" drv 1 "await block 6 70 5 minecraft:gold_block"
