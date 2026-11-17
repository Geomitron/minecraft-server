# Step 7's in-game checks: Effortless Building's shapes drawn as ghost blocks, in Single mode with the array modifier
# and as a Line, starting on ghost blocks and erasing, the block-mix picker and its inventory toggle, the mix in
# shapes, the mix panel and the picker's "Turn off", what clicks do with a pickaxe, an apple, nothing, a water bucket
# or redstone dust in hand, and a client without Effortless Building. Sourced by inside.sh, which provides drv, val,
# check, keys, click, use_click, attack_click, client_said, start_client and stop_client. Shapes are drawn with
# Planner1 looking straight down, so each block's position is known.

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

# Single mode with the array modifier: eight stone bricks from 5 -60 5 eastward, with Planner2 south of them.
drv 1 "cmd tp Planner2 6 -60 -3 0 10" "cmd clear @s" "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" \
	"ebmode SINGLE" "ebarray 7 1 0 0" "cmd tp @s 5 -55 5 0 90" "wait 40" > /dev/null
use_click 1
check "a shape adds ghost blocks" drv 1 "await block 5 -60 5 minecraft:stone_bricks" "await block 12 -60 5 minecraft:stone_bricks"
check "client 2 sees the shape at once, glowing with Planner1's name" drv 2 "await block 12 -60 5 minecraft:stone_bricks" \
	"await glow Planner1:8" "notoasts" "wait 5" "shot shape-seen"
check "the shape placed no real blocks" eval '[[ $(val 1 "real 5 -60 5") == minecraft:air ]]'
drv 1 "cmd tp @s 2 -58 -5 0 5" "wait 40" "notoasts" "wait 5" "shot shape-drawn" > /dev/null

# A shape can start on a ghost block: a click on the ghost block at 8 -60 5 puts the block on top of it, and the break
# click erases it again, leaving the real ground.
drv 1 "ebarray 0" "cmd tp @s 8 -55 5 0 90" "wait 20" > /dev/null
use_click 1
check "a shape started on a ghost block goes on top of it" \
	eval 'drv 1 "await block 8 -59 5 minecraft:stone_bricks" && drv 2 "await block 8 -59 5 minecraft:stone_bricks"'
attack_click 1
check "the break click erases the ghost block" \
	eval 'drv 1 "await block 8 -59 5 minecraft:air" && drv 2 "await block 8 -59 5 minecraft:air"'
check "and leaves the real blocks" eval 'drv 1 "await real 8 -61 5 minecraft:grass_block" "await block 8 -60 5 minecraft:stone_bricks"'

# A Line: the first click on the ground at 5 -61 9, the second with Planner1 above 10 -60 9.
drv 1 "ebmode LINE" "cmd tp @s 5 -55 9 0 90" "wait 20" > /dev/null
use_click 1
drv 1 "cmd tp @s 10 -55 9 0 90" "wait 20" > /dev/null
use_click 1
check "a Line adds its ghost blocks on both clients" eval 'drv 1 "await block 5 -60 9 minecraft:stone_bricks" \
	"await block 10 -60 9 minecraft:stone_bricks" && drv 2 "await block 5 -60 9 minecraft:stone_bricks" "await block 10 -60 9 minecraft:stone_bricks"'
check "and no real blocks" eval '[[ $(val 1 "real 7 -60 9") == minecraft:air ]]'
drv 1 "ebmode SINGLE" > /dev/null

# The block-mix picker: B opens it with every block; the toggle limits it to the player's inventory, cobblestone and
# mossy cobblestone; Planner1 picks both, at 80 to 20, and turns the mix on.
drv 1 "cmd clear @s" "cmd give @s minecraft:cobblestone 5" "cmd give @s minecraft:mossy_cobblestone 5" \
	"cmd tp @s 2 -58 -5 0 5" "wait 20" > /dev/null
keys 1 key b
check "B opens the block-mix picker" drv 1 "await screen Block mix"
check "the picker lists every block in the game" eval '(( $(val 1 pickershown) > 1000 ))'
check "with the mix off, the picker has no Turn off button" eval '[[ $(val 1 texts) != *"Turn off"* ]]'
drv 1 "notoasts" "wait 5" "shot picker-all" > /dev/null
read -r x y < <(val 1 "widget 0 Only my inventory")
click 1 "$x" "$y"
check "its toggle limits the list to the blocks in the inventory" eval '[[ $(val 1 pickershown) == 2 ]]'
for cell in 0 1; do
	read -r x y < <(val 1 "gridcell $cell")
	click 1 "$x" "$y"
done
for share in 0:80 1:20; do
	read -r x y < <(val 1 "widget ${share%%:*} Percentage of the mix")
	click 1 "$x" "$y"
	keys 1 key ctrl+a type "${share##*:}"
done
drv 1 "wait 10" "notoasts" "wait 5" "shot picker-inventory" > /dev/null
read -r x y < <(val 1 "widget 0 Use mix")
click 1 "$x" "$y"
check "Use mix turns the mix on, 80 cobblestone to 20 mossy cobblestone" \
	eval '[[ $(val 1 mix) == "on minecraft:cobblestone:80,minecraft:mossy_cobblestone:20" ]]'
check "with a message" client_said 1 "Block mix on. Press B to change it or turn it off."
drv 1 "notoasts" "wait 5" "shot mix-on-message" > /dev/null

# The mix panel shows while the mix applies: with cobblestone held in Single mode, and no screen open.
panel="Block mix | 80% | 20%"
check "the panel shows the mix's shares beside their blocks' icons" \
	eval '[[ $(val 1 panel) == "$panel" ]]'
keys 1 key F1
check "the panel hides while the HUD is hidden (F1)" eval 'drv 1 "wait 5" > /dev/null; [[ $(val 1 panel) == hidden ]]'
keys 1 key F1
check "and shows again with the HUD" eval 'drv 1 "wait 5" > /dev/null; [[ $(val 1 panel) == "$panel" ]]'
# For the panel's screenshot, Planner2 steps out of view and the chat fades over 10 seconds.
drv 1 "cmd tp Planner2 30 -60 -10" "cmd tp @s 6 -58 -4 0 30" "wait 220" "notoasts" "wait 5" "shot mix-panel" > /dev/null
# And at GUI scale 4, in a window of 1920 by 1080, where the panel draws at 75% of the game's size.
drv 1 "window 1920 1080 4" "wait 20" "notoasts" "wait 5" "shot mix-panel-1920x1080" "window 854 480 2" "wait 20" \
	> /dev/null

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

# The mix applies only while the main hand holds a block. With the mix on, a pickaxe or an apple in hand draws no ghost
# block with the place click, and an apple's break click breaks the real block as without mods. Planner1 stands within
# vanilla's reach (5 blocks) of it and looks down 45 degrees south, at the real ground at 4 -61 6.
drv 1 "ebarray 0" "cmd clear @s" "cmd give @s minecraft:cobblestone 5" "cmd give @s minecraft:mossy_cobblestone 5" \
	"cmd tp @s 4.5 -60 4.5 0 45" "wait 20" > /dev/null
for item in iron_pickaxe apple; do
	drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:$item" "wait 10" > /dev/null
	check "with the mix on and the $item held, the panel hides" eval '[[ $(val 1 panel) == hidden ]]'
	if [[ $item == iron_pickaxe ]]; then
		drv 1 "notoasts" "wait 5" "shot mix-panel-pickaxe" > /dev/null
	fi
	use_click 1
	check "with the mix on and the $item held, the place click draws no ghost block, on both clients" \
		eval 'drv 1 "wait 20" "await block 4 -60 6 minecraft:air" && drv 2 "await block 4 -60 6 minecraft:air"'
	check "and places no real block" eval '[[ $(val 1 "real 4 -60 6") == minecraft:air ]]'
done
drv 1 "cmd setblock 4 -60 6 minecraft:stone" "wait 10" "await real 4 -60 6 minecraft:stone" > /dev/null
attack_click 1
check "with the mix on and an apple held, the break click breaks the real block" drv 1 "await real 4 -60 6 minecraft:air"
drv 1 "notoasts" "wait 5" "shot mix-with-apple" > /dev/null

# In Disable, with the mix on and a block held, every click is vanilla's: the place click puts a real cobblestone at
# 4 -60 10 and draws no ghost block, and the use click on a door opens it.
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:cobblestone" "ebmode DISABLED" \
	"cmd tp @s 4.5 -60 8.5 0 45" "wait 20" > /dev/null
check "in Disable the mix is still on" eval '[[ $(val 1 mix) == on* ]]'
check "and the panel hides" eval '[[ $(val 1 panel) == hidden ]]'
use_click 1
check "in Disable with the mix on, the place click places a real block" drv 1 "await real 4 -60 10 minecraft:cobblestone"
check "and draws no ghost block, on both clients" \
	eval 'drv 1 "await block 4 -60 10 minecraft:air" && drv 2 "await block 4 -60 10 minecraft:air"'
drv 1 "cmd setblock 30 -60 0 minecraft:oak_door[half=lower,facing=north,open=false]" \
	"cmd setblock 30 -59 0 minecraft:oak_door[half=upper,facing=north,open=false]" \
	"cmd tp @s 30.5 -60 -2 0 30" "wait 20" "await real 30 -60 0 minecraft:oak_door" > /dev/null
use_click 1
check "in Disable with the mix on, the use click opens a door" \
	eval 'drv 1 "wait 10" > /dev/null; [[ $(val 1 "realstate 30 -60 0") == *open=true* ]]'
drv 1 "notoasts" "wait 5" "shot disable-with-mix" "ebmode SINGLE" > /dev/null

# B with the mix on opens the picker on the mix, with a Turn off button, which turns the mix off. The panel, back with
# cobblestone in hand, hides while the picker is open.
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:cobblestone" "cmd tp @s 2 -58 -5 0 5" "wait 20" \
	> /dev/null
check "back in Single mode with cobblestone held, the panel shows" eval '[[ $(val 1 panel) == "$panel" ]]'
keys 1 key b
check "B with the mix on opens the picker" drv 1 "await screen Block mix"
check "on the mix, 80 cobblestone to 20 mossy cobblestone" \
	eval '[[ $(val 1 pickermix) == "minecraft:cobblestone:80,minecraft:mossy_cobblestone:20" ]]'
check "with a Turn off button" eval '[[ $(val 1 texts) == *"Turn off"* ]]'
check "the panel hides while the picker is open" eval '[[ $(val 1 panel) == hidden ]]'
check "and the mix stays on until a button is pressed" eval '[[ $(val 1 mix) == on* ]]'
drv 1 "notoasts" "wait 5" "shot mix-picker-turn-off" > /dev/null
read -r x y < <(val 1 "widget 0 Turn off")
click 1 "$x" "$y"
check "Turn off turns the mix off and closes the picker" \
	eval 'drv 1 "await noscreen" > /dev/null && [[ $(val 1 mix) == off* ]]'
check "with a message" client_said 1 "Block mix off."
check "and the panel hides" eval '[[ $(val 1 panel) == hidden ]]'
drv 1 "notoasts" "wait 5" "shot mix-off-message" > /dev/null

# What clicks do in Single mode, with the mix off, while the main hand holds a pickaxe, an apple or nothing: each
# click on a real stone, or on a ghost stone brick, at 16 -60 4, on the real grass at 16 -61 4. Planner1 stands at
# 16.5 -60 2.5 and looks down 45 degrees south, at that block. None of the three places a block, so every click acts as
# without mods: the use click does nothing to a stone, and the attack click breaks the real block it aims at, which
# under a ghost block is the grass. No click sends an edit.
# click_case <item> <target> <click> <ghost after> <real after> <grass after> <what it does>
click_case() {
	drv 1 "edit 16 -60 4 16 -58 4 minecraft:air" "cmd fill 16 -60 4 16 -58 4 minecraft:air" \
		"cmd setblock 16 -61 4 minecraft:grass_block" "await block 16 -60 4 minecraft:air" \
		"await real 16 -61 4 minecraft:grass_block" > /dev/null
	if [[ $2 == real ]]; then
		drv 1 "cmd setblock 16 -60 4 minecraft:stone" "await real 16 -60 4 minecraft:stone" > /dev/null
	else
		drv 1 "edit 16 -60 4 16 -60 4 minecraft:stone_bricks" "await block 16 -60 4 minecraft:stone_bricks" > /dev/null
	fi
	drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:$1" "cmd tp @s 16.5 -60 2.5 0 45" "wait 20" \
		"holdreset" > /dev/null
	"${3}_click" 1
	drv 1 "wait 30" > /dev/null
	local seen want held
	case $1 in
		iron_pickaxe) held="a pickaxe" ;;
		apple) held="an apple" ;;
		*) held=nothing ;;
	esac
	seen="ghost=$(val 1 "get 16 -60 4") above=$(val 1 "get 16 -59 4") real=$(val 1 "real 16 -60 4")"
	seen+=" grass=$(val 1 "real 16 -61 4") $(val 1 holdlog)"
	want="ghost=minecraft:$4 above=minecraft:air real=minecraft:$5 grass=minecraft:$6"
	echo "  $held $2 $3: $seen" >> /out/checks.log
	check "with $held in hand, the $3 click on a $2 block $7" \
		eval '[[ $seen == "$want "* && $seen == *" edits=0 "* ]]'
}
drv 1 "ebmode SINGLE" > /dev/null
for item in iron_pickaxe apple air; do
	click_case "$item" real use air stone grass_block "does nothing"
	click_case "$item" ghost use stone_bricks air grass_block "does nothing"
	click_case "$item" real attack air air grass_block "breaks the real block"
	click_case "$item" ghost attack stone_bricks air air "breaks the real block under the ghost block"
done

# In Single mode and in Line mode, which takes two clicks: a pickaxe breaks a real block, an empty hand opens a door
# and a water bucket places water, each as without mods and with no edit, and redstone dust, an item that places a
# block, draws a ghost block. Planner1 looks at the real grass at 16 -61 4, or at a stone or a door on it, as above.
# ready <item> [block]: clears the column at 16 4 down to the grass, puts the block (a stone, or a closed door) on the
# grass if given, and puts the item in Planner1's main hand.
ready() {
	drv 1 "edit 16 -60 4 16 -58 4 minecraft:air" "cmd fill 16 -60 4 16 -58 4 minecraft:air" \
		"cmd setblock 16 -61 4 minecraft:grass_block" "await block 16 -60 4 minecraft:air" \
		"await real 16 -61 4 minecraft:grass_block" "await real 16 -60 4 minecraft:air" > /dev/null
	case ${2:-} in
		stone) drv 1 "cmd setblock 16 -60 4 minecraft:stone" "await real 16 -60 4 minecraft:stone" > /dev/null ;;
		# Facing south, a closed door stands at its block's north side, where Planner1's aim meets it.
		door) drv 1 "cmd setblock 16 -60 4 minecraft:oak_door[half=lower,facing=south,open=false]" \
			"cmd setblock 16 -59 4 minecraft:oak_door[half=upper,facing=south,open=false]" \
			"await real 16 -60 4 minecraft:oak_door" > /dev/null ;;
	esac
	drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:$1" "cmd tp @s 16.5 -60 2.5 0 45" "wait 20" \
		"holdreset" > /dev/null
}
no_edit() {
	[[ $(val 1 holdlog) == *" edits=0 "* ]]
}
for mode in SINGLE LINE; do
	drv 1 "ebmode $mode" > /dev/null
	ready iron_pickaxe stone
	attack_click 1
	check "in $mode mode, a pickaxe breaks a real block" drv 1 "await real 16 -60 4 minecraft:air"
	check "and sends no edit" no_edit
	ready air door
	use_click 1
	check "in $mode mode, an empty hand opens a door" \
		eval 'drv 1 "wait 10" > /dev/null; [[ $(val 1 "realstate 16 -60 4") == *open=true* ]]'
	check "and sends no edit" no_edit
	ready water_bucket
	use_click 1
	check "in $mode mode, a water bucket places water" drv 1 "await real 16 -60 4 minecraft:water"
	check "and sends no edit" no_edit
	# The water's flow drains once its source is gone.
	drv 1 "cmd setblock 16 -60 4 minecraft:air" "wait 60" > /dev/null
	ready redstone
	use_click 1
	if [[ $mode == LINE ]]; then
		drv 1 "wait 10" > /dev/null
		use_click 1
	fi
	check "in $mode mode, redstone dust draws a ghost block, on both clients" \
		eval 'drv 1 "await block 16 -60 4 minecraft:redstone_wire" && drv 2 "await block 16 -60 4 minecraft:redstone_wire"'
	check "and places no real block" eval '[[ $(val 1 "real 16 -60 4") == minecraft:air ]]'
	drv 1 "notoasts" "wait 5" "shot redstone-ghost-${mode,,}" > /dev/null
done
drv 1 "ebmode SINGLE" > /dev/null

# A client without Effortless Building loads, skips the hooks into it, and keeps everything else.
cp /out/client2.log /out/client2-with-effortless.log
stop_client 2
start_client 2 effortless-building
# The session starts while the world is still loading, and its loading screen swallows key presses.
check "client 2 without Effortless Building starts a session" drv 2 "await session" "await noscreen"
check "and loaded without Effortless Building or mixin errors" eval \
	'! grep -qE "^\s+- effortlessbuilding " /out/client2.log && ! grep -qiE "InvalidInjection|Mixin apply.*failed|MixinApplyError" /out/client2.log'
drv 1 "edit 6 -58 6 6 -58 6 minecraft:glowstone" > /dev/null
check "and keeps receiving client 1's edits" drv 2 "await block 6 -58 6 minecraft:glowstone"
keys 2 key b
check "its block-mix picker opens" drv 2 "await screen Block mix"
drv 2 "close" > /dev/null
