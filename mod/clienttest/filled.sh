# Ghost blocks the real world fills: real blocks placed by hand, by /fill, by a piston and by flowing water remove the
# ghost blocks they fill, on both clients within a tick and with no glow; a block placed another way leaves its ghost
# block; a block placed several to a spot fills its ghost block only at the same count; ghost blocks drawn over real
# blocks that fill them remove the older ghost block there, and undo and redo bring it back and remove it again;
# breaking a filled block brings none back; a paste onto built blocks changes nothing; a stair placed with "Match ghost
# blocks" on faces as its ghost stair does and fills it, but only while ghost blocks show, and the setting stays as saved after the game restarts; and
# ghost blocks saved over matching real blocks while the server ran without the add-on go once their chunk loads. Shots
# of a wall half built, before and after, of "Settings", without and with the row's tooltip, and the stairs
# placed with the setting on and off, at 854 by 480 and 1920 by 1080, GUI scale 2. Sourced by inside.sh, which provides drv, val, check,
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

# A block placed several to a spot fills its ghost block only at the same count: a ghost block of 4 candles at 74 -60 40
# stays with 1, 2 and 3 real candles and goes at the 4th.
both_state() {
	[[ $(val 1 "layerstate $1 $2 $3") == *"$4"* && $(val 2 "layerstate $1 $2 $3") == *"$4"* ]]
}
drv 1 "edit 74 -60 40 74 -60 40 minecraft:candle[candles=4]" > /dev/null
for n in 1 2; do
	check "client $n sees the ghost block of 4 candles" drv "$n" "await block 74 -60 40 minecraft:candle"
done
for count in 1 2 3; do
	drv 1 "cmd setblock 74 -60 40 minecraft:candle[candles=$count]" "wait 10" > /dev/null
	check "$count real candles leave the ghost block of 4 on both clients" both_state 74 -60 40 candles=4
done
drv 1 "cmd setblock 74 -60 40 minecraft:candle[candles=4]" "wait 10" > /dev/null
check "the 4th real candle removes the ghost block on both clients" both_layer 74 -60 40 minecraft:air

# Drawing a ghost block where the real block already fills it: a ghost stone block at 76 -60 40 stays under real oak
# planks; ghost oak planks drawn there remove it as part of the same edit, one Ctrl+Z brings it back and Ctrl+Y removes
# it again.
drv 1 "edit 76 -60 40 76 -60 40 minecraft:stone" "cmd setblock 76 -60 40 minecraft:oak_planks" "wait 10" > /dev/null
check "a ghost stone block stays where real oak planks are built" both_layer 76 -60 40 minecraft:stone
drv 1 "edit 76 -60 40 76 -60 40 minecraft:oak_planks" "wait 10" > /dev/null
check "ghost oak planks drawn over real oak planks remove the ghost stone block on both clients" eval \
	'drv 1 "await block 76 -60 40 minecraft:air" && drv 2 "await block 76 -60 40 minecraft:air" &&
	both_layer 76 -60 40 minecraft:air'
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "one undo brings the ghost stone block back on both clients" eval \
	'drv 1 "await block 76 -60 40 minecraft:stone" && drv 2 "await block 76 -60 40 minecraft:stone" &&
	both_layer 76 -60 40 minecraft:stone'
keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
check "a redo removes it again on both clients" eval \
	'drv 1 "await block 76 -60 40 minecraft:air" && drv 2 "await block 76 -60 40 minecraft:air" &&
	both_layer 76 -60 40 minecraft:air'
check "Planner1's edit, undo and redo glows fade on client 2" drv 2 "await noglow"

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

# Ghost blocks through walls: a ghost stone brick at 156 -60 156 over air, a ghost stone brick at 154 -60 156 with a
# real cobblestone in it, and a ghost oak stair facing east at 152 -60 156 with a real oak stair facing north in it,
# 4 blocks behind a real stone wall from 139 -60 160 to 169 -49 160 that fills Planner2's view from the south. The
# animals the flat world starts with are killed so nothing else moves in the view, and Planner1 stands out of it, west
# of the blocks. through_walls is what the last frame drew with all three.
through_walls="lightblue 156,-60,156 red 154,-60,156 orange 152,-60,156"
drv 1 "cmd tp @s 120 -60 156 -90 0" "cmd tp Planner2 154.5 -60 166.5 180 10" "wait 40" \
	"cmd kill @e[type=!minecraft:player]" "cmd time set noon" "cmd fill 139 -60 160 169 -49 160 minecraft:stone" \
	"edit 156 -60 156 156 -60 156 minecraft:stone_bricks" "edit 154 -60 156 154 -60 156 minecraft:stone_bricks" \
	"edit 152 -60 156 152 -60 156 minecraft:oak_stairs[facing=east]" > /dev/null
check "Planner2 sees the three ghost blocks behind the wall" drv 2 "await block 156 -60 156 minecraft:stone_bricks" \
	"await block 154 -60 156 minecraft:stone_bricks" "await block 152 -60 156 minecraft:oak_stairs" \
	"await real 154 -59 160 minecraft:stone" "await noglow"
drv 1 "cmd setblock 154 -60 156 minecraft:cobblestone" "cmd setblock 152 -60 156 minecraft:oak_stairs[facing=north]" \
	> /dev/null
check "a cobblestone and an oak stair facing north leave the ghost blocks they don't fill" eval \
	'drv 2 "await real 154 -60 156 minecraft:cobblestone" "await real 152 -60 156 minecraft:oak_stairs" "wait 5" \
	> /dev/null && both_layer 154 -60 156 minecraft:stone_bricks && both_layer 152 -60 156 minecraft:oak_stairs'
check "from 10 blocks through the wall, the block over air draws light blue, the cobblestone's red, the stair's orange" \
	eval \
	'drv 2 "wait 20" > /dev/null && [[ $(val 2 throughwalls) == "$through_walls" ]]'
drv 2 "window 854 480 2" "notoasts" "wait 40" "shot through-walls-through-wall-854x480" "wait 10" \
	"window 1920 1080 2" "wait 40" "notoasts" "shot through-walls-through-wall-1920x1080" "wait 10" > /dev/null
keys 2 key n
check "with ghost blocks hidden, nothing draws" eval \
	'drv 2 "wait 20" > /dev/null && [[ $(val 2 ghosts) == hidden* ]] && [[ $(val 2 throughwalls) == none ]]'
drv 2 "notoasts" "wait 40" "shot through-walls-hidden-1920x1080" "wait 10" > /dev/null
through=$(val 2 "shotdiff through-walls-through-wall-1920x1080 through-walls-hidden-1920x1080")
echo "  pixels the ghost blocks change through the wall: $through" >> /out/checks.log
check "the view through the wall differs from the view with ghost blocks hidden, where the three blocks draw" eval \
	'((through > 500))'
keys 2 key n
check "shown again, they draw again" eval \
	'drv 2 "wait 20" > /dev/null && [[ $(val 2 throughwalls) == "$through_walls" ]]'
# The cost of finding them and building their shapes: 20 scans with a ghost block at every position within 32 blocks
# of Planner2's camera, then the shapes built whole and again after each of 20 1-block edits.
cost=$(val 2 "wrongscan 20")
echo "  ghost blocks through walls: $cost" >> /out/checks.log
check "a scan with ghost blocks filling its range, and building its shapes, runs" eval \
	'[[ $cost == "20 scans of 274625 ghost blocks, "*"; shapes built whole in "*"; after a 1-block edit, "* ]]'
# Planner2's camera is in block z 188 here, 32 blocks from the blocks' z, and in z 189 farther back, 33 blocks away.
drv 1 "cmd tp Planner2 154.5 -60 189.5 180 10" > /dev/null
check "from 33 blocks, beyond 32, nothing draws" eval \
	'drv 2 "wait 40" > /dev/null && [[ $(val 2 throughwalls) == none ]]'
drv 1 "cmd tp Planner2 154.5 -60 188.5 180 10" > /dev/null
check "from 32 blocks, all three draw" eval \
	'drv 2 "wait 40" > /dev/null && [[ $(val 2 throughwalls) == "$through_walls" ]]'
# From the north side, in front of the wall, the faint colours draw over Litematica's own overlay.
drv 1 "cmd tp Planner2 154.5 -60 150.5 0 10" > /dev/null
check "from the wall's other side, all three draw" eval \
	'drv 2 "wait 40" > /dev/null && [[ $(val 2 throughwalls) == "$through_walls" ]]'
drv 2 "notoasts" "wait 40" "shot through-walls-in-view-1920x1080" "wait 10" "window 854 480 2" "wait 40" \
	"notoasts" "shot through-walls-in-view-854x480" "wait 10" > /dev/null
# Erasing the ghost block over air takes it out within half a second: the scan that changed the shapes comes within 10
# ticks of Planner2's copy of the layer taking it out.
drv 1 "edit 156 -60 156 156 -60 156 minecraft:air" > /dev/null
erased_at=$(drv 2 "await inlayer 156 -60 156 minecraft:air" "gametime" | tail -1 | sed 's/^.* => //')
check "erasing the ghost block over air takes it out of the light blue within half a second" eval \
	'drv 2 "await throughwalls lightblue none red 154,-60,156 orange 152,-60,156" > /dev/null &&
	 changed_at=$(val 2 throughwallschanged) &&
	 echo "  the layer lost it at $erased_at, the shapes changed at $changed_at" >> /out/checks.log &&
	 ((changed_at - erased_at <= 10))'

# "Match ghost blocks" ("Settings"): the row in Effortless Building's "Settings" screen, off at first, saved by
# the screen's "Save" button. With it on, Planner1, 3 blocks north of a ghost oak stair facing east at 220 -60 220 and
# facing south, places oak stairs there in Disable, and the server's world, Planner2 and Planner1's own block face east
# and fill the ghost block; with it off, a stair placed into a ghost stair facing east at 222 -60 220 faces south, as
# Planner1 does, and leaves its ghost block. The setting is left on for the check after the game restarts, below.
# settings_screen <client>: opens the client's "Settings" screen, as its radial menu's button does.
settings_screen() {
	drv "$1" "close" "settings" "await screen Settings" "wait 10" > /dev/null
}
# match_row <client>: the "Match ghost blocks" row's switch on the open screen: its text, then its centre.
match_row() {
	val "$1" "settingrow Match ghost blocks"
}
# match_switch <client>: a real click on the row's switch.
match_switch() {
	local text x y
	read -r text x y < <(match_row "$1")
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
# real_has <client> <x y z> <block> <text>: that client's world has that block there, with the text in its state.
real_has() {
	drv "$1" "await real $2 $3 $4 $5" > /dev/null && [[ $(val "$1" "realstate $2 $3 $4") == *"$6"* ]]
}
# server_has <x y z> <block state> <tag>: the server's own world has that block there, as /execute if block finds it;
# run by Planner1, who then says the tag, which the server's log shows within 20 seconds.
server_has() {
	drv 1 "cmd execute if block $1 $2 $3 $4 run say $5" > /dev/null && wait_for /out/server.log "$5" 20
}
client_config=run/client1/config/buildingplanner-client.properties
drv 1 "window 854 480 2" "wait 20" > /dev/null
# The radial menu's button under undo, which opens the screen, is named "Settings": with the pointer on it, 131 left
# of the menu's middle and 13 below it, its hover text is "Settings" alone, shot, and a real click on it opens the
# screen, titled "Settings".
xdo 1 keydown Alt_L
drv 1 "await screen Build Modes" "wait 10" > /dev/null
xdo 1 mousemove --window %w $((427 - 131 * 2)) $((240 + 13 * 2))
drv 1 "wait 10" "notoasts" "shot radial-settings-hover-854x480-gui2" > /dev/null
check "the radial menu's button under undo is named \"Settings\" on hover" eval '[[ $(val 1 tooltips) == Settings ]]'
xdo 1 click 1
check "and a click on it opens the screen, titled \"Settings\"" drv 1 "await screen Settings" "wait 10"
xdo 1 keyup Alt_L
check "Planner1's \"Settings\" has the row \"Match ghost blocks\", off at first" eval \
	'[[ $(match_row 1) == "OFF "* ]]'
# label_hover: moves client 1's mouse onto the row's label, which shows its tooltip over the row.
label_hover() {
	local left top right bottom
	read -r left top right bottom < <(val 1 "drawnbox Match ghost blocks")
	xdo 1 mousemove --window %w $(((left + right) / 2)) $(((top + bottom) / 2))
}
# settings_shots <size>: "Settings" with the mouse in a corner, then with the row's tooltip, which covers the row.
settings_shots() {
	xdo 1 mousemove --window %w 2 2
	drv 1 "notoasts" "wait 5" "shot settings-match-ghost-blocks-$1-gui2" > /dev/null
	label_hover
	drv 1 "wait 5" "shot settings-match-ghost-blocks-tooltip-$1-gui2" > /dev/null
}
label_hover
check "the row's label shows its tooltip" eval 'drv 1 "wait 5" > /dev/null &&
	[[ $(val 1 tooltips) == "Automatically flips and turns a block you place to match a ghost block you'"'"'re filling in" ]]'
settings_shots 854x480
drv 1 "window 1920 1080 2" "wait 20" > /dev/null
settings_shots 1920x1080
drv 1 "window 854 480 2" "wait 20" > /dev/null
match_switch 1
check "a click on the row's switch turns it on, and saves nothing yet" eval \
	'[[ $(match_row 1) == "ON "* ]] && ! grep -q "^match-ghost-turn=" "$client_config"'
save_settings 1
check "\"Save\" closes the screen and saves the setting on" grep -q "^match-ghost-turn=true" "$client_config"
drv 1 "cmd tp @s 220.5 -60 217.5 0 30" "cmd tp Planner2 221.5 -60 225.5 180 25" \
	"cmd item replace entity @s weapon.mainhand with minecraft:oak_stairs 64" "ebmode DISABLED" "wait 40" \
	"edit 220 -60 220 220 -60 220 minecraft:oak_stairs[facing=east]" \
	"edit 222 -60 220 222 -60 220 minecraft:oak_stairs[facing=east]" > /dev/null
check "both clients see the two ghost stairs facing east" eval \
	'drv 1 "await block 222 -60 220 minecraft:oak_stairs" > /dev/null &&
	drv 2 "await block 222 -60 220 minecraft:oak_stairs" "await noglow" > /dev/null'
drv 1 "lookat 220.5 -60 220.5" "wait 5" > /dev/null
use_click 1
check "with the setting on, Planner1 facing south places a stair facing east, as the ghost stair faces" \
	real_has 1 220 -60 220 minecraft:oak_stairs "facing=east,"
check "the server's world has it facing east" server_has 220 -60 220 "minecraft:oak_stairs[facing=east]" \
	matched-stair-east
check "Planner2 sees it facing east" real_has 2 220 -60 220 minecraft:oak_stairs "facing=east,"
check "and it fills the ghost stair on both clients" eval 'drv 2 "wait 5" > /dev/null && both_layer 220 -60 220 minecraft:air'
# With ghost blocks hidden, the setting doesn't act: a stair placed into a hidden ghost stair facing east at 224 -60 220
# faces as Planner1 does, south, and leaves the ghost stair.
drv 1 "edit 224 -60 220 224 -60 220 minecraft:oak_stairs[facing=east]" "await block 224 -60 220 minecraft:oak_stairs" \
	"cmd tp @s 224.5 -60 217.5 0 30" "wait 10" > /dev/null
keys 1 key n
check "with Planner1's ghost blocks hidden" eval 'drv 1 "wait 20" > /dev/null && [[ $(val 1 ghosts) == hidden* ]]'
drv 1 "lookat 224.5 -60 220.5" "wait 5" > /dev/null
use_click 1
check "with the setting on and ghost blocks hidden, the stair faces south, as Planner1 does" eval \
	'real_has 1 224 -60 220 minecraft:oak_stairs "facing=south," &&
	server_has 224 -60 220 "minecraft:oak_stairs[facing=south]" hidden-stair-south &&
	real_has 2 224 -60 220 minecraft:oak_stairs "facing=south,"'
check "and leaves its ghost stair on both clients" eval 'drv 2 "wait 5" > /dev/null &&
	both_layer 224 -60 220 minecraft:oak_stairs'
keys 1 key n
check "Planner1's ghost blocks show again" eval 'drv 1 "wait 20" > /dev/null && [[ $(val 1 ghosts) == shown* ]]'
settings_screen 1
match_switch 1
save_settings 1
check "the switch turned off again and saved, the setting is off" grep -q "^match-ghost-turn=false" "$client_config"
drv 1 "cmd tp @s 222.5 -60 217.5 0 30" "wait 10" "lookat 222.5 -60 220.5" "wait 5" > /dev/null
use_click 1
check "with the setting off, the stair faces south, as Planner1 does" eval \
	'real_has 1 222 -60 220 minecraft:oak_stairs "facing=south," &&
	server_has 222 -60 220 "minecraft:oak_stairs[facing=south]" unmatched-stair-south &&
	real_has 2 222 -60 220 minecraft:oak_stairs "facing=south,"'
check "and leaves its ghost stair on both clients" eval 'drv 2 "wait 5" > /dev/null &&
	both_layer 222 -60 220 minecraft:oak_stairs'
drv 2 "window 854 480 2" "notoasts" "wait 40" "shot match-ghost-blocks-placed-854x480-gui2" "wait 10" \
	"window 1920 1080 2" "wait 40" "notoasts" "shot match-ghost-blocks-placed-1920x1080-gui2" "wait 10" > /dev/null
settings_screen 1
match_switch 1
save_settings 1
check "turned on again and saved, for the restart" grep -q "^match-ghost-turn=true" "$client_config"

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
check "client 1 joins the server without the add-on, with a local session" eval \
	'drv 1 "await noscreen" "await session" > /dev/null && [[ $(val 1 sessionkind) == local ]]'
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
settings_screen 1
check "after the game restarts, \"Match ghost blocks\" is on, as saved" eval '[[ $(match_row 1) == "ON "* ]]'
drv 1 "close" > /dev/null
check "far from the built blocks, client 1 holds no copy of their column" \
	eval '[[ $(val 1 "tile 18 18") == none && $(val 1 "layer 302 -60 300") == minecraft:air ]]'
drv 1 "cmd tp @s 300 -60 295 0 30" > /dev/null
check "once their chunk loads, the ghost blocks the real ones fill are gone, and the stair facing the other way stays" \
	eval 'drv 1 "await block 302 -60 300 minecraft:oak_stairs" > /dev/null &&
	[[ $(val 1 "layer 300 -60 300") == minecraft:air && $(val 1 "layer 301 -60 300") == minecraft:air ]]'
check "and nothing glows" eval '[[ -z $(val 1 glows) ]]'
check "neither client's log has a self-check warning" no_self_check_warning 1 2
