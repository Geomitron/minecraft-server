# In-game checks for overlapping shapes: a shape drawn across an older one changes only the positions they share, and
# every other ghost block of the older shape stays in the layer and stays drawn, for the player who drew it and for the
# other player, including where the new shape replaces a column's tile. Sourced by inside.sh, which provides drv, val,
# check, keys, use_click, attack_click, start_client and stop_client. Most shapes are sent with the driver's edit
# command, the path a drawn shape takes; floors, lines and walls of about 20 blocks, as Geo drew, are drawn with
# Effortless Building's own modes and real clicks, then undone or erased with the break click. "Drawn" means
# Litematica's schematic world holds the block, which a column's unload would clear; the last cases also check that
# what Litematica's renderer draws of each column is what a fresh render draws. The first floor lies at y -55, in the
# 16-block section from -64 to -49, over the four columns from 0 0 to 1 1. Through all of it, the game's self-check
# writes no warning to either client's log; at the end, a block taken out of Litematica's copy alone makes it warn.

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

# Rejoining. Client 1's log starts again when it restarts, so it's checked first.
check "before client 1 restarts, its log has no self-check warning" no_self_check_warning 1
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

# Floors about 20 blocks long, as Geo drew them, with Effortless Building's modes and real clicks. Floor A, of oak
# planks, lies on the ground at y -60 over the columns from 0 0 to 1 1. Client 1 draws from outside its corner;
# client 2 looks down on it from above.
A="0 -60 0 19 -60 19"
cols="-1 -1 1 1"
drv 1 "edit $big minecraft:air" "await block 127 -55 127 minecraft:air" > /dev/null
drv 1 "cmd tp Planner2 10 -35 10 0 90" "wait 5" > /dev/null
drv 2 "fly true" > /dev/null
drv 1 "cmd tp Planner2 10 -35 10 0 90" > /dev/null

# a_ok <oak planks expected> [<block> <expected>]: on both clients, floor A's box holds that many oak planks, and as
# many of the block given, in the copy of the layer and in what Litematica draws.
a_ok() {
	local n counts want
	for n in 1 2; do
		counts=$(val $n "countbox $A minecraft:oak_planks")
		want="layer $1 drawn $1"
		if [[ -n ${2:-} ]]; then
			counts+=", $(val $n "countbox $A $2")"
			want+=", layer $3 drawn $3"
		fi
		echo "  client$n A: $counts (want $want)" >> /out/checks.log
		[[ $counts == "$want" ]] || return 1
	done
}

# drawn_as_fresh: on both clients, Litematica draws the same columns near floor A as once it has thrown away its builds
# and built every column again, and each column's last build has at least the indices of the fresh one. A build can
# have a few more: a column built before its neighbour's edit keeps the faces between them that the edit hides.
drawn_as_fresh() {
	local n before after i
	drv 1 "wait 40" > /dev/null
	for n in 1 2; do
		read -ra before <<< "$(val $n "rendered $cols")"
		drv $n "rerender" "wait 60" > /dev/null
		read -ra after <<< "$(val $n "rendered $cols")"
		echo "  client$n drawn ${before[*]}, after a fresh render ${after[*]}" >> /out/checks.log
		((${#before[@]} == ${#after[@]})) || return 1
		for i in "${!after[@]}"; do
			if [[ ${after[i]} == - || ${before[i]} == - ]]; then
				[[ ${before[i]} == "${after[i]}" ]] || return 1
			else
				((${before[i]%\*} >= ${after[i]%\*})) || return 1
			fi
		done
	done
}

hold() {
	drv 1 "cmd item replace entity @s weapon.mainhand with $1" "ebmode $2" > /dev/null
}

# clicks <use_click | attack_click> <aim> <aim>: a shape's two clicks, aimed at the two points.
clicks() {
	drv 1 "lookat $2" "wait 3" > /dev/null
	$1 1
	drv 1 "lookat $3" "wait 5" > /dev/null
	$1 1
	drv 1 "wait 20" > /dev/null
}

# floor_a <name>: erases what the last case left and draws floor A with Floor mode, from its corner on the ground to
# the opposite one.
floor_a() {
	drv 1 "edit -16 -60 -16 40 -40 40 minecraft:air" "await block 0 -60 0 minecraft:air" > /dev/null
	drv 2 "await block 0 -60 0 minecraft:air" > /dev/null
	drv 1 "cmd clear @s" "ebarray 0" "cmd tp @s 10 -50 -12 0 50" "wait 5" "fly true" "cmd tp @s 10 -50 -12 0 50" \
		"wait 20" > /dev/null
	hold minecraft:oak_planks FLOOR
	clicks use_click "0.5 -60 0.5" "19.5 -60 19.5"
	drv 2 "await block 19 -60 19 minecraft:oak_planks" > /dev/null
	check "$1: floor A is drawn" eval 'a_ok 400'
}

# crossing <name> <block> <mode> <aim> <aim> <last block> <A's blocks it covers>: floor A, then a shape of the block
# drawn across it, then undone with Ctrl+Z. A's blocks the shape doesn't cover stay, at once and five seconds later,
# and all of A's are back after the undo.
crossing() {
	floor_a "$1"
	hold "$2" "$3"
	drv 1 "lookat $4" "wait 3" > /dev/null
	use_click 1
	drv 1 "lookat $5" "wait 5" > /dev/null
	check "$1: while the shape is under way, floor A stays" eval 'a_ok 400'
	use_click 1
	drv 1 "await block $6 $2" > /dev/null
	drv 2 "await block $6 $2" > /dev/null
	shots "$1-drawn"
	check "$1: A's other blocks stay, at once" eval "a_ok $((400 - $7)) $2 $7"
	drv 1 "wait 100" > /dev/null
	check "$1: five seconds later" eval "a_ok $((400 - $7)) $2 $7"
	check "$1: Litematica draws each column as a fresh render does" drawn_as_fresh
	keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
	drv 1 "await block $6 minecraft:air" > /dev/null
	drv 2 "await block $6 minecraft:air" "wait 20" > /dev/null
	shots "$1-undone"
	check "$1: undone, all of floor A is back" eval 'a_ok 400'
	check "$1: undone, Litematica draws each column as a fresh render does" drawn_as_fresh
}

# A floor at A's height, started on the grass beside it and ended past its other side, covering 10 of its rows.
crossing floor-across minecraft:stone FLOOR "25.5 -60 5.5" "-5.5 -60 14.5" "-5 -60 14" 200
# A floor started on A, which goes on top of it, over a quarter of it.
crossing floor-on-top minecraft:stone FLOOR "10.5 -59 10.5" "29.5 -59 29.5" "29 -59 29" 0
# A line through A at its height.
crossing line-across minecraft:stone LINE "25.5 -60 10.5" "-5.5 -60 10.5" "-5 -60 10" 20
# A wall across A, from the grass beside it up into the next section.
crossing wall-across minecraft:stone_bricks WALL "-4.5 -60 10.5" "24.5 -44.5 10.5" "24 -45 10" 20

# A floor across A erased again with the break click, drawn with Floor mode on the stone floor's top.
floor_a erase
hold minecraft:stone FLOOR
clicks use_click "25.5 -60 5.5" "-5.5 -60 14.5"
drv 2 "await block -5 -60 14 minecraft:stone" > /dev/null
clicks attack_click "25.5 -59 5.5" "-5.5 -60 14.5"
drv 1 "await block -5 -60 14 minecraft:air" > /dev/null
drv 2 "await block -5 -60 14 minecraft:air" "wait 20" > /dev/null
shots erased
check "erase: A's other blocks stay" eval 'a_ok 200'
check "erase: Litematica draws each column as a fresh render does" drawn_as_fresh

# Lines of 18 more kinds of block across A, each written into the tiles in place, which makes each tile's palette grow
# past 4 and 16 states, where Litematica's container changes how it stores them.
floor_a palette
z=2
for block in stone glass bricks cobblestone andesite diorite granite sandstone red_wool blue_wool lime_wool \
	white_wool black_wool yellow_wool orange_wool pink_wool gray_wool cyan_wool; do
	drv 1 "edit -2 -60 $z 21 -60 $z minecraft:$block" "await block 21 -60 $z minecraft:$block" > /dev/null
	drv 2 "await block 21 -60 $z minecraft:$block" > /dev/null
	z=$((z + 1))
done
drv 1 "wait 40" > /dev/null
check "palette: after 18 kinds of block in place, A's other blocks stay" eval 'a_ok 40'
check "palette: Litematica draws each column as a fresh render does" drawn_as_fresh
shots palette

check "neither client's log has a self-check warning" no_self_check_warning 1 2

# The game's self-check warns when Litematica's copy alone loses a block: one of floor A's oak planks taken out of
# client 1's tile, then a block of stone put elsewhere in the column, in place. Every position of column 0 0 at y -60
# holds a ghost block.
drv 1 "unhold 0 -60 0" "wait 20" > /dev/null
drv 1 "edit 5 -60 1 5 -60 1 minecraft:stone" "await block 5 -60 1 minecraft:stone" > /dev/null
drv 2 "await block 5 -60 1 minecraft:stone" "wait 40" > /dev/null
warning="Self-check: chunk column 0 0 (x 0 to 15, z 0 to 15) in minecraft:overworld, after an edit of 1 block in it:"
warning+=" the game's copy of the layer holds 256 ghost blocks (the column held 256 before), Litematica holds 255,"
warning+=" and they differ at 1 position"
check "a block taken out of Litematica's copy alone makes client 1's self-check warn, with the column, the change and \
the counts" wait_for /out/client1.log "$warning" 60
grep -F "Self-check:" /out/client1.log >> /out/checks.log || true
check "client 2, whose Litematica still holds the block, doesn't warn" no_self_check_warning 2
