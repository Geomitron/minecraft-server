# Step 15's comparison shots: the ghost blocks through a wall, red and orange, from Planner2's side as filled.sh sets
# them up; and "Match ghost blocks" with ghost blocks hidden: Planner1, with ghost blocks hidden and the setting on,
# places oak stairs into a hidden ghost stair facing east, which face as Planner1 does, shot from Planner2's side.
# Each shot's .boxes file and the placed stair's window box go to /out. Sourced by inside.sh.

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

# The ghost blocks through the wall, red and orange, as in filled.sh.
drv 1 "cmd tp @s 120 -60 156 -90 0" "cmd tp Planner2 154.5 -60 166.5 180 10" "wait 40" \
	"cmd kill @e[type=!minecraft:player]" "cmd time set noon" "cmd fill 139 -60 160 169 -49 160 minecraft:stone" \
	"edit 154 -60 156 154 -60 156 minecraft:stone_bricks" "edit 152 -60 156 152 -60 156 minecraft:oak_stairs[facing=east]" \
	> /dev/null
check "Planner2 sees the two ghost blocks behind the wall" drv 2 "await block 154 -60 156 minecraft:stone_bricks" \
	"await block 152 -60 156 minecraft:oak_stairs" "await real 154 -59 160 minecraft:stone" "await noglow"
drv 1 "cmd setblock 154 -60 156 minecraft:cobblestone" "cmd setblock 152 -60 156 minecraft:oak_stairs[facing=north]" \
	> /dev/null
check "through the wall, the cobblestone's ghost block draws red and the stair's orange" eval \
	'drv 2 "await real 154 -60 156 minecraft:cobblestone" "await real 152 -60 156 minecraft:oak_stairs" "wait 25" \
	> /dev/null && [[ $(val 2 throughwalls) == "lightblue none red 154,-60,156 orange 152,-60,156" ]]'
drv 2 "window 1920 1080 2" "wait 40" "notoasts" "shot compare-wrong-blocks-through-wall-1920x1080-gui2" "wait 10" \
	> /dev/null

# "Match ghost blocks" on, saved, as filled.sh turns it on.
settings_screen() {
	drv "$1" "close" "settings" "await screen Settings" "wait 10" > /dev/null
}
match_switch() {
	local text x y
	read -r text x y < <(val "$1" "settingrow Match ghost blocks")
	click "$1" "$x" "$y"
	drv "$1" "wait 5" > /dev/null
}
save_settings() {
	local x y
	read -r x y < <(val "$1" "widget 0 Save")
	click "$1" "$x" "$y"
	drv "$1" "await noscreen" "wait 5" > /dev/null
}
drv 1 "window 854 480 2" "wait 20" > /dev/null
settings_screen 1
match_switch 1
save_settings 1
check "\"Match ghost blocks\" is saved on" grep -q "^match-ghost-turn=true" run/client1/config/buildingplanner-client.properties

# A ghost oak stair facing east at 220 -60 220; Planner1 3 blocks north of it, facing south, hides ghost blocks and
# places oak stairs there in Disable. Planner2 stands south-east of it, facing it, with ghost blocks shown.
drv 1 "cmd tp @s 220.5 -60 217.5 0 30" "cmd tp Planner2 221.5 -60 225.5 180 25" \
	"cmd item replace entity @s weapon.mainhand with minecraft:oak_stairs 64" "ebmode DISABLED" "wait 40" \
	"edit 220 -60 220 220 -60 220 minecraft:oak_stairs[facing=east]" > /dev/null
check "both clients see the ghost stair facing east" eval \
	'drv 1 "await block 220 -60 220 minecraft:oak_stairs" > /dev/null &&
	drv 2 "await block 220 -60 220 minecraft:oak_stairs" "await noglow" > /dev/null'
keys 1 key n
check "Planner1's ghost blocks are hidden" eval 'drv 1 "wait 20" > /dev/null && [[ $(val 1 ghosts) == hidden* ]]'
drv 1 "lookat 220.5 -60 220.5" "wait 5" > /dev/null
use_click 1
drv 1 "await real 220 -60 220 minecraft:oak_stairs" > /dev/null
echo "  Planner1's placed stair: $(val 1 "realstate 220 -60 220")" >> /out/checks.log
drv 2 "await real 220 -60 220 minecraft:oak_stairs" "wait 20" > /dev/null
echo "  Planner2 sees: $(val 2 "realstate 220 -60 220")" >> /out/checks.log
drv 2 "window 1920 1080 2" "wait 40" "notoasts" "shot compare-match-while-hidden-1920x1080-gui2" "wait 10" > /dev/null
echo "  placed stair box: $(val 2 "screenbox 220 -60 220 220 -60 220")" >> /out/checks.log
