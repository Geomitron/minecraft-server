# Step 6's in-game checks: the client's Litematica bridge, starting a plan, the edit glow, undo and redo keys, joining
# mid-edit, and Syncmatica's Unload and Remove buttons. Sourced by inside.sh, which provides drv, val, check, keys,
# click, client_said, start_client and stop_client.

check "client 1 starts a session on hello" drv 1 "await session"
check "client 2 starts a session on hello" drv 2 "await session"

# Both players stand south of the plan, looking at it.
drv 1 "cmd tp @s 5 -60 -3 0 10" "cmd tp Planner2 6 -60 -3 0 10" "wait 40" > /dev/null

# A fresh Litematica is in Simple mode with no area marked, so N gives a message instead of the start screen.
check "a fresh Litematica is in Simple selection mode" eval '[[ $(val 1 "selmode") == SIMPLE ]]'
keys 1 key n
no_area="Mark an area with Litematica's area tool first, then press this key again."
check "N with no area marked gives the message, not the start screen" \
	eval 'client_said 1 "$no_area" && drv 1 "wait 20" "await noscreen"'

# Starting a plan: mark an area, press N, type a name, press Enter.
drv 1 "select 2 -60 2 8 -57 8" > /dev/null
keys 1 key n
check "N opens the start screen" drv 1 "await screen Start a plan"
keys 1 type Hall
keys 1 key Return
check "Enter creates the plan and closes the screen" drv 1 "await noscreen" "await placement Hall"
check "the new plan appears for the other player at once" drv 2 "await placement Hall" "await area Hall 0,0,0..6,3,6"

# Edit Schematic writes go to the other player, glowing with the editor's name.
drv 1 "fill 2 -60 2 8 -60 8 minecraft:stone_bricks" > /dev/null
check "client 1's Litematica fill reaches client 2" drv 2 "await block 8 -60 8 minecraft:stone_bricks"
check "client 2 shows it glowing with Planner1's name" drv 2 "await glow Planner1:49" "shot glow-from-planner1"
drv 2 "place 5 -59 5 minecraft:glowstone" > /dev/null
check "client 2's Litematica block reaches client 1" drv 1 "await block 5 -59 5 minecraft:glowstone"
check "client 1 shows it glowing with Planner2's name" drv 1 "await glow Planner2:1" "shot glow-from-planner2"
check "the glow fades out" drv 1 "await noglow" "shot after-glow"

# Growth in place: an edit past the plan's box grows it on both clients without reloading.
drv 1 "shape Hall 10 0 0 minecraft:stone" > /dev/null
check "the plan grows in place on client 1" drv 1 "await area Hall 0,0,0..10,3,6" "await block 12 -60 2 minecraft:stone"
check "the plan grows in place on client 2" drv 2 "await area Hall 0,0,0..10,3,6" "await block 12 -60 2 minecraft:stone" \
	"await block 8 -60 8 minecraft:stone_bricks" "shot grown"

# Undo and redo with the real keys: Effortless Building's Ctrl+Z and Ctrl+Y go to Building Planner.
drv 2 "close" > /dev/null
keys 2 keydown ctrl key z sleep 0.3 keyup ctrl
check "Ctrl+Z undoes client 2's last edit on both clients" \
	eval 'drv 2 "await block 5 -59 5 minecraft:air" && drv 1 "await block 5 -59 5 minecraft:air"'
keys 2 keydown ctrl key y sleep 0.3 keyup ctrl
check "Ctrl+Y redoes it on both clients" \
	eval 'drv 2 "await block 5 -59 5 minecraft:glowstone" && drv 1 "await block 5 -59 5 minecraft:glowstone"'

# Syncmatica's Unload button hides the plan for that player until they next join.
drv 2 "syncgui" "wait 20" "shot syncmatica-list" > /dev/null
read -r x y < <(val 2 "button Hall Unload")
click 2 "$x" "$y"
check "Unload hides the plan on client 2" drv 2 "await noplacement Hall"
check "and leaves it on client 1" drv 1 "wait 20" "await placement Hall"
drv 2 "close" > /dev/null

# Joining mid-edit: client 2 leaves, client 1 edits and grows the plan, client 2 rejoins.
stop_client 2
drv 1 "fill 2 -59 2 2 -58 2 minecraft:oak_planks" "shape Hall 0 0 9 minecraft:stone" \
	"await area Hall 0,0,0..10,3,9" > /dev/null
start_client 2
check "client 2 rejoins with a session" drv 2 "await session"
check "the unloaded plan loads again on the next join" drv 2 "await placement Hall"
check "with the edits and growth made while client 2 was away" drv 2 "await area Hall 0,0,0..10,3,9" \
	"await block 2 -58 2 minecraft:oak_planks" "await block 2 -60 11 minecraft:stone" \
	"await block 5 -59 5 minecraft:glowstone" "await block 8 -60 8 minecraft:stone_bricks"
drv 1 "cmd tp Planner2 6 -60 -3 0 10" > /dev/null

# Syncmatica's Remove button: a player who isn't an operator can't remove someone else's plan.
drv 2 "syncgui" "wait 20" > /dev/null
read -r x y < <(val 2 "button Hall Remove")
click 2 "$x" "$y"
check "Planner2 removing Planner1's plan is refused with a message" \
	client_said 2 "Only Planner1, who shared this plan, can delete it."
check "and the plan stays for both" eval 'drv 1 "wait 20" "await shared Hall" && drv 2 "await shared Hall"'
drv 2 "close" > /dev/null

# Planner2 starts two plans of their own.
for plan in Shed:20 Barn:30; do
	name=${plan%%:*} x0=${plan##*:}
	drv 2 "select $x0 -60 2 $((x0 + 4)) -58 6" > /dev/null
	keys 2 key n
	drv 2 "await screen Start a plan" > /dev/null
	keys 2 type "$name"
	keys 2 key Return
	check "Planner2 starts $name and client 1 sees it" eval "drv 2 'await placement $name' && drv 1 'await placement $name'"
done

# An operator can remove anyone's plan.
drv 1 "syncgui" "wait 20" > /dev/null
read -r x y < <(val 1 "button Shed Remove")
click 1 "$x" "$y"
check "Planner1, an operator, removes Planner2's plan for both" \
	eval 'drv 1 "await notshared Shed" "await noplacement Shed" && drv 2 "await notshared Shed" "await noplacement Shed"'
drv 1 "close" > /dev/null

# The owner can remove their own plan.
drv 2 "syncgui" "wait 20" > /dev/null
read -r x y < <(val 2 "button Barn Remove")
click 2 "$x" "$y"
check "Planner2 removes their own plan for both" \
	eval 'drv 2 "await notshared Barn" && drv 1 "await notshared Barn" "await noplacement Barn"'
drv 2 "close" > /dev/null
