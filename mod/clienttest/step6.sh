# Step 6's in-game checks: the layer. Shapes and erasing reach the other player at once with the edit glow, tiles stay
# out of Litematica's screens and edits, undo and redo, columns loading as players move, rejoining, dimensions, a server
# restart, and old plans' placements leaving Litematica at a session's start. Sourced by inside.sh, which
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

# Tiles stay out of Litematica's screens (hooks L4 to L7): the column's tile is Litematica's only placement, and
# Litematica selects a new placement whenever none is selected.
check "each client gives Litematica the tile" \
	eval '[[ $(val 1 "tiles") == "1 tiles, 1 placements" && $(val 2 "tiles") == "1 tiles, 1 placements" ]]'
check "Litematica selected no tile" eval '[[ $(val 1 "selected") == none && $(val 2 "selected") == none ]]'
check "and selecting a tile leaves nothing selected" eval 'drv 1 "selecttile" > /dev/null && [[ $(val 1 "selected") == none ]]'
check "Litematica's placement list shows no tile" \
	eval '[[ $(val 1 "placementlist") == "0 tiles; others: none" ]] && drv 1 "close" > /dev/null'
check "Litematica draws no tile's box" eval '[[ $(val 1 "boxes") == "0 tiles; others: none" ]]'
# The tile's box covers y -64 to -49, where the flat world's real ground lies, with nothing in the layer there.
check "no real block in the tile's box is marked as an extra block" \
	eval '[[ $(val 2 "overlay 6 -61 5") == NONE && $(val 2 "overlay 6 -64 12") == NONE ]]'
check "and a ghost block with no real block there is still marked as missing" eval '[[ $(val 2 "overlay 5 -60 5") == MISSING ]]'
drv 2 "notoasts" "wait 5" "shot no-extra-marks" > /dev/null
# Litematica's Edit Schematic actions (hook L1): placing a block into a tile and filling over one change nothing.
drv 1 "place 6 -60 5 minecraft:gold_block" "fill 5 -59 5 7 -59 5 minecraft:gold_block" "wait 40" > /dev/null || true
check "Litematica's own edits leave the layer and the tile as they were" eval '[[ $(val 1 "layer 6 -60 5") == minecraft:stone_bricks &&
	$(val 1 "get 6 -60 5") == minecraft:stone_bricks && $(val 1 "get 6 -59 5") == minecraft:air &&
	$(val 2 "get 6 -60 5") == minecraft:stone_bricks ]]'

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

# Plans players shared before the layer: Planner1's Litematica holds and saves placements of plan files in Syncmatica's
# client folder and in "Start a plan"'s, and restores them at the next start.
drv 1 "oldplans 20 -60 20" > /dev/null
check "Litematica saved the old plans' placements" \
	eval 'grep -rqF "Old shared plan" run/client1/config && grep -rqF "Old started plan" run/client1/config'

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
check "the session removed the old plans' placements Litematica restored" \
	eval 'grep -qF "Removed 2 placements of old plans from Litematica" /out/client1.log && [[ $(val 1 "placements") != *Old* ]]'
check "and left their files" eval '[[ -f "run/client1/schematics/sync/Old shared plan.litematic" &&
	-f "run/client1/schematics/Building Planner/Old started plan.litematic" ]]'
drv 1 "cmd execute in minecraft:the_nether run tp @s 5 70 -3" "wait 40" > /dev/null
check "and the Nether's" drv 1 "await block 6 70 5 minecraft:gold_block"
