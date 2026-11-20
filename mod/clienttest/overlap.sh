# In-game checks for overlapping shapes: a shape drawn across an older one changes only the positions they share, and
# every other ghost block of the older shape stays in the layer and stays drawn, for the player who drew it and for the
# other player, including where the new shape replaces a column's tile. Sourced by inside.sh, which provides drv, val,
# check, use_click, start_client and stop_client. Most shapes are sent with the driver's edit command, the path a drawn
# shape takes; one floor and wall are drawn with Effortless Building's own modes and real clicks. "Drawn" means
# Litematica's schematic world holds the block, which a column's unload would clear. The first floor lies at y -55, in
# the 16-block section from -64 to -49, over the four columns from 0 0 to 1 1.

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

floor="0 -55 0 31 -55 31"
# Both players fly outside the floor's opposite corners, above it, looking down across it. Flying stops on the ground,
# so each is put in the air first.
view() {
	drv 1 "cmd tp @s -10 -40 -10 -45 30" "cmd tp Planner2 42 -40 42 135 30" "wait 5" "fly true" > /dev/null
	drv 2 "fly true" > /dev/null
	drv 1 "cmd tp @s -10 -40 -10 -45 30" "cmd tp Planner2 42 -40 42 135 30" "wait 40" > /dev/null
}

# shots <name>: both clients' views.
shots() {
	drv 1 "notoasts" "shot $1" > /dev/null
	drv 2 "notoasts" "shot $1" > /dev/null
}

# floor_ok <client> <oak planks expected>: the client's copy of the layer and Litematica's schematic world each hold
# that many oak planks in the floor's box.
floor_ok() {
	local counts
	counts=$(val "$1" "countbox $floor minecraft:oak_planks")
	echo "  client$1 floor: $counts (want $2)" >> /out/checks.log
	[[ $counts == "layer $2 drawn $2" ]]
}

# draw_floor: erases everything the last case left and draws the oak floor.
draw_floor() {
	drv 1 "edit 0 -60 0 47 -30 47 minecraft:air" "await block 0 -55 0 minecraft:air" > /dev/null
	drv 2 "await block 0 -55 0 minecraft:air" > /dev/null
	drv 1 "edit $floor minecraft:oak_planks" "await block 31 -55 31 minecraft:oak_planks" "wait 40" > /dev/null
	drv 2 "await block 31 -55 31 minecraft:oak_planks" "wait 20" > /dev/null
	shots "floor-$((++floors))"
}
floors=0

# after <name> <oak planks expected> <last block of the shape>: once both clients show the new shape's last block, the
# floor's other ghost blocks are still there at once, a second later and five seconds later, on both clients.
after() {
	drv 1 "await block $3" > /dev/null
	drv 2 "await block $3" > /dev/null
	shots "$1-at-once"
	check "$1: the floor's other ghost blocks stay, at once" eval 'floor_ok 1 '"$2"' && floor_ok 2 '"$2"
	drv 1 "wait 20" > /dev/null
	check "$1: a second later" eval 'floor_ok 1 '"$2"' && floor_ok 2 '"$2"
	drv 1 "wait 100" > /dev/null
	shots "$1-later"
	check "$1: five seconds later" eval 'floor_ok 1 '"$2"' && floor_ok 2 '"$2"
}

view
check "the floor is drawn on both clients" eval 'draw_floor && floor_ok 1 1024 && floor_ok 2 1024'

# A wall rising from the floor into the section above, across two of the floor's columns, replaces their tiles.
drv 1 "edit 0 -55 8 31 -40 8 minecraft:stone_bricks" > /dev/null
after wall-into-next-section 992 "31 -40 8 minecraft:stone_bricks"
check "the wall's columns now span two sections" eval '[[ $(val 1 "tile 0 0") == -64..-33 && $(val 1 "tile 1 1") == -64..-49 ]]'

# Moving away until the columns unload and back reloads them.
drv 1 "cmd tp @s 400 -46 400" "cmd tp Planner2 400 -46 410" "wait 100" > /dev/null
view
drv 1 "wait 60" > /dev/null
shots back
check "after moving away and back, the floor is all there" eval 'floor_ok 1 992 && floor_ok 2 992'

# Rejoining.
stop_client 1
start_client 1
check "client 1 rejoins" drv 1 "await session" "await noscreen" "wait 100"
view
shots rejoined
check "after rejoining, the floor is all there" eval 'floor_ok 1 992'

# A second floor crossing the first at the same height, into columns of its own.
draw_floor
drv 1 "edit 16 -55 16 47 -55 47 minecraft:glass" > /dev/null
after second-floor 768 "47 -55 47 minecraft:glass"

# A wall that stays in the floor's section.
draw_floor
drv 1 "edit 0 -55 8 31 -50 8 minecraft:stone_bricks" > /dev/null
after wall-same-section 992 "31 -50 8 minecraft:stone_bricks"

# The same with Effortless Building's own Floor and Wall modes and real clicks: an oak floor on the ground at y -60,
# then a stone brick wall started on it at z 8, up into the section above.
drv 1 "edit 0 -60 0 47 -30 47 minecraft:air" "await block 0 -55 0 minecraft:air" "await block 47 -55 47 minecraft:air" \
	> /dev/null
drv 1 "cmd clear @s" "cmd item replace entity @s weapon.mainhand with minecraft:oak_planks" "ebarray 0" \
	"ebmode FLOOR" "cmd tp @s 16 -45 16" "wait 5" "fly true" "cmd tp @s 16 -45 16" "wait 20" \
	"lookat 0.5 -60 0.5" "wait 2" > /dev/null
use_click 1
drv 1 "lookat 31.5 -60 31.5" "wait 5" > /dev/null
use_click 1
drv 1 "wait 40" > /dev/null
eb_floor="0 -60 0 31 -60 31"
check "Effortless Building's Floor mode draws the floor" eval \
	'[[ $(val 1 "countbox $eb_floor minecraft:oak_planks") == "layer 1024 drawn 1024" ]]'
view
shots eb-floor
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "ebmode WALL" "cmd tp @s 16 -50 -8" \
	"wait 20" "lookat 0.5 -59 8.5" "wait 2" > /dev/null
use_click 1
drv 1 "lookat 31.5 -44.5 8.5" "wait 5" > /dev/null
use_click 1
drv 1 "wait 40" > /dev/null
echo "  eb wall: $(val 1 "countbox 0 -60 8 31 -40 8 minecraft:stone_bricks"), tile 0 0: $(val 1 "tile 0 0")" \
	>> /out/checks.log
view
shots eb-wall-at-once
eb_floor_ok() {
	local n counts
	for n in 1 2; do
		counts=$(val $n "countbox $eb_floor minecraft:oak_planks")
		echo "  client$n eb floor: $counts" >> /out/checks.log
		[[ $counts == "layer 992 drawn 992" || $counts == "layer 1024 drawn 1024" ]] || return 1
	done
}
check "Effortless Building's wall leaves the floor's other ghost blocks" eb_floor_ok
drv 1 "wait 100" > /dev/null
shots eb-wall-later
check "five seconds later too" eb_floor_ok

# Many tiles replaced at once: a floor over 64 columns at y -55, then five times a second floor over all of them at
# y -40, in the section above, and its erasing, each of which replaces every column's tile.
big="0 -55 0 127 -55 127"
drv 1 "edit 0 -60 0 47 -30 47 minecraft:air" "await block 0 -59 8 minecraft:air" > /dev/null
drv 1 "cmd tp @s 64 -20 64 0 90" "cmd tp Planner2 64 -20 70 0 90" "wait 5" "fly true" > /dev/null
drv 2 "fly true" > /dev/null
drv 1 "edit $big minecraft:oak_planks" "await block 127 -55 127 minecraft:oak_planks" "wait 40" > /dev/null
drv 2 "await block 127 -55 127 minecraft:oak_planks" "wait 20" > /dev/null
big_ok() {
	local n counts
	for n in 1 2; do
		counts=$(val $n "countbox $big minecraft:oak_planks")
		echo "  client$n big floor ($1): $counts" >> /out/checks.log
		[[ $counts == "layer 16384 drawn 16384" ]] || return 1
	done
}
check "the floor over 64 columns is drawn on both clients" big_ok drawn
for round in 1 2 3 4 5; do
	drv 1 "edit 0 -40 0 127 -40 127 minecraft:glass" "await block 127 -40 127 minecraft:glass" "wait 10" > /dev/null
	drv 2 "await block 127 -40 127 minecraft:glass" "wait 10" > /dev/null
	check "round $round: a second floor in the section above leaves the first" big_ok "round $round raised"
	drv 1 "edit 0 -40 0 127 -40 127 minecraft:air" "await block 127 -40 127 minecraft:air" "wait 10" > /dev/null
	drv 2 "await block 127 -40 127 minecraft:air" "wait 10" > /dev/null
	check "round $round: erasing it leaves the first" big_ok "round $round lowered"
done
shots big-floor
