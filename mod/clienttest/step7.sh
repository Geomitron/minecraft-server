# Step 7's in-game checks: Effortless Building's shapes drawn into plans, starting on ghost blocks and erasing, the
# plan a shape goes into, the block-mix picker and its inventory toggle, the mix in shapes and in Litematica's
# edits, and a client without Effortless Building. Sourced by inside.sh, which provides drv, val, check, keys, click,
# use_click, attack_click, client_said, start_client and stop_client. Shapes are drawn in Effortless Building's Single
# mode with its array modifier, with Planner1 looking straight down, so each block's position is known.

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

# Planner1 starts the plan Hall (2 -60 2 to 8 -57 8); both players stand south of it.
drv 1 "cmd tp @s 5 -60 -3 0 10" "cmd tp Planner2 6 -60 -3 0 10" "wait 40" "select 2 -60 2 8 -57 8" > /dev/null
keys 1 key n
drv 1 "await screen Start a plan" > /dev/null
keys 1 type Hall
keys 1 key Return
check "Planner1 starts Hall and client 2 sees it" \
	eval 'drv 1 "await placement Hall" && drv 2 "await placement Hall" "await area Hall 0,0,0..6,3,6"'

# A shape goes into the plan it starts in, as ghost blocks for both players, and grows the plan where it crosses the
# edge: eight stone bricks from 5 -60 5 eastward, past Hall's edge at x 8.
drv 1 "cmd clear @s" "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "ebmode SINGLE" \
	"ebarray 7 1 0 0" "cmd tp @s 5 -55 5 0 90" "wait 20" > /dev/null
use_click 1
check "a shape goes into the plan it starts in, which grows to fit it" drv 1 "await area Hall 0,0,0..10,3,6" \
	"await block 5 -60 5 minecraft:stone_bricks" "await block 12 -60 5 minecraft:stone_bricks"
check "client 2 sees the shape at once, glowing with Planner1's name" drv 2 "await area Hall 0,0,0..10,3,6" \
	"await block 12 -60 5 minecraft:stone_bricks" "await glow Planner1:8" "notoasts" "wait 5" "shot shape-seen"
check "the shape placed no real blocks" eval '[[ $(val 1 "real 5 -60 5") == minecraft:air ]]'
drv 1 "cmd tp @s 2 -58 -5 0 5" "wait 40" "notoasts" "wait 5" "shot shape-drawn" > /dev/null

# A shape can start on a plan's ghost block: a click on the ghost block at 8 -60 5 puts the block on top of it, and
# the break click erases it again, leaving the real ground.
drv 1 "ebarray 0" "cmd tp @s 8 -55 5 0 90" "wait 20" > /dev/null
use_click 1
check "a shape started on a ghost block goes on top of it" \
	eval 'drv 1 "await block 8 -59 5 minecraft:stone_bricks" && drv 2 "await block 8 -59 5 minecraft:stone_bricks"'
attack_click 1
check "the break click erases the ghost block from the plan" \
	eval 'drv 1 "await block 8 -59 5 minecraft:air" && drv 2 "await block 8 -59 5 minecraft:air"'
check "and leaves the real blocks" eval 'drv 1 "await real 8 -61 5 minecraft:grass_block" "await block 8 -60 5 minecraft:stone_bricks"'

# A shape that starts outside every plan goes into the plan selected in Litematica; with none selected, the player
# gets a message.
drv 1 "selectplacement" "cmd tp @s 20 -55 5 0 90" "wait 20" > /dev/null
use_click 1
check "outside every plan, with none selected, the shape gives a message" \
	client_said 1 "Start the shape inside a plan, or select a plan in Litematica first."
drv 1 "notoasts" "wait 5" "shot no-plan-message" "selectplacement Hall" > /dev/null
use_click 1
check "with Hall selected, the shape goes into Hall, which grows to fit it" \
	eval 'drv 1 "await area Hall 0,0,0..18,3,6" "await block 20 -60 5 minecraft:stone_bricks" && drv 2 "await block 20 -60 5 minecraft:stone_bricks"'

# The block-mix picker: B opens it with every block; the toggle limits it to the player's inventory, cobblestone and
# mossy cobblestone; Planner1 picks both, at 80 to 20, and turns the mix on.
drv 1 "cmd clear @s" "cmd give @s minecraft:cobblestone 5" "cmd give @s minecraft:mossy_cobblestone 5" \
	"cmd tp @s 2 -58 -5 0 5" "wait 20" > /dev/null
keys 1 key b
check "B opens the block-mix picker" drv 1 "await screen Block mix"
check "the picker lists every block in the game" eval '(( $(val 1 pickershown) > 1000 ))'
drv 1 "notoasts" "wait 5" "shot picker-all" > /dev/null
read -r x y < <(val 1 "widget 0 Only my inventory")
click 1 "$x" "$y"
check "its toggle limits the list to the blocks in the inventory" eval '[[ $(val 1 pickershown) == 2 ]]'
for cell in 0 1; do
	read -r x y < <(val 1 "gridcell $cell")
	click 1 "$x" "$y"
done
for share in 0:80 1:20; do
	read -r x y < <(val 1 "widget ${share%%:*} Share")
	click 1 "$x" "$y"
	keys 1 key ctrl+a type "${share##*:}"
done
drv 1 "wait 10" "notoasts" "wait 5" "shot picker-inventory" > /dev/null
read -r x y < <(val 1 "widget 0 Use mix")
click 1 "$x" "$y"
check "Use mix turns the mix on, 80 cobblestone to 20 mossy cobblestone" \
	eval '[[ $(val 1 mix) == "on minecraft:cobblestone:80,minecraft:mossy_cobblestone:20" ]]'
check "with a message" client_said 1 "Block mix on. Press B to turn it off."
drv 1 "notoasts" "wait 5" "shot mix-on-message" > /dev/null

# With the mix on, each block of a shape comes from the mix: a row of 20 from 3 -60 7 eastward.
declare -A mix_at
kinds=()
for ((x = 3; x <= 22; x++)); do
	mix_at[$x]=$(val 1 "mixat $x -60 7")
	kinds+=("${mix_at[$x]}")
done
echo "  the mix's blocks along the row: ${kinds[*]}" >> /out/checks.log
row_is_mix() {
	local x
	for ((x = 3; x <= 22; x++)); do
		drv "$1" "await block $x -60 7 ${mix_at[$x]}" > /dev/null || return 1
	done
}
check "the mix gives the row both of its blocks" eval '(( $(printf "%s\n" "${kinds[@]}" | sort -u | wc -l) == 2 ))'
drv 1 "ebarray 19 1 0 0" "cmd tp @s 3 -55 7 0 90" "wait 20" > /dev/null
use_click 1
check "a shape takes each block from the mix, on client 1" row_is_mix 1
check "and on client 2" row_is_mix 2
drv 1 "cmd tp Planner2 12 -56 -6 0 20" > /dev/null
drv 2 "wait 40" "notoasts" "wait 5" "shot mix-shape-seen" > /dev/null

# Litematica's place uses the mix too: the block placed at 4 -59 4 (plan position 2 1 2) is the mix's block there.
expected=$(val 1 "mixat 2 1 2")
drv 1 "place 4 -59 4 minecraft:stone" > /dev/null
check "Litematica's place takes the mix's block, on both clients" \
	eval 'drv 1 "await block 4 -59 4 $expected" && drv 2 "await block 4 -59 4 $expected"'

# B again turns the mix off.
drv 1 "cmd tp @s 2 -58 -5 0 5" "wait 20" > /dev/null
keys 1 key b
check "B turns the mix off, with a message" eval 'client_said 1 "Block mix off." && [[ $(val 1 mix) == off* ]]'
drv 1 "notoasts" "wait 5" "shot mix-off-message" > /dev/null

# A client without Effortless Building loads, skips the hooks into it, and keeps everything else.
cp /out/client2.log /out/client2-with-effortless.log
stop_client 2
start_client 2 effortless-building
# The session starts while the world is still loading, and its loading screen swallows key presses.
check "client 2 without Effortless Building starts a session" drv 2 "await session" "await noscreen"
check "and loaded without Effortless Building or mixin errors" eval \
	'! grep -qE "^\s+- effortlessbuilding " /out/client2.log && ! grep -qiE "InvalidInjection|Mixin apply.*failed|MixinApplyError" /out/client2.log'
drv 2 "await placement Hall" > /dev/null
drv 2 "place 6 -58 6 minecraft:glowstone" > /dev/null
check "its Litematica edits reach client 1" drv 1 "await block 6 -58 6 minecraft:glowstone"
drv 1 "fill 3 -58 3 3 -57 3 minecraft:oak_planks" > /dev/null
check "and client 1's edits reach it" drv 2 "await block 3 -57 3 minecraft:oak_planks"
keys 2 key b
check "its block-mix picker opens" drv 2 "await screen Block mix"
drv 2 "close" > /dev/null
