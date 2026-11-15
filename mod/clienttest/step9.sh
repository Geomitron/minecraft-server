# Step 9's in-game checks: copying part of the plans, moving, turning and mirroring the copy, pasting it into a plan,
# undo and redo, the copy's messages, Clear, the copy limit, and the keys on a server without Building Planner.
# Sourced by inside.sh, which provides drv, val, check, keys, click, attack_click, client_said, start_server,
# stop_server, start_client, stop_client, no_planner_message and $ADDON. Players stand on the flat world's grass, whose
# top is at y -61.

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

# start_plan <name> <corner> <corner>: Planner1 marks the area and starts a plan there, which client 2 sees.
start_plan() {
	drv 1 "select $2 $3" > /dev/null
	keys 1 key n
	drv 1 "await screen Start a plan" > /dev/null
	keys 1 type "$1"
	keys 1 key Return
	drv 1 "await noscreen" "await placement $1" > /dev/null && drv 2 "await placement $1" > /dev/null
}

# press <client> <key>: a real key press with no screen open.
press() {
	drv "$1" "close" "wait 5" > /dev/null
	keys "$1" key "$2"
}

# state_has <client> <x y z> <text>: the state Litematica shows there contains the text.
state_has() {
	[[ $(val "$1" "state $2") == *"$3"* ]]
}

# shot <client> <name>: a screenshot with the game's pop-ups cleared, written before the call returns.
shot() {
	drv "$1" "notoasts" "wait 5" "shot $2" "wait 20" > /dev/null
}

drv 1 "cmd tp @s 5 -60 -3 0 10" "cmd tp Planner2 6 -60 -3 0 10" "wait 40" > /dev/null
check "Planner1 starts Hall" start_plan Hall "2 -60 2" "8 -57 8"
drv 1 "fill 2 -60 2 8 -60 8 minecraft:stone_bricks" "place 3 -59 3 minecraft:oak_stairs[facing=north]" \
	"place 4 -59 3 minecraft:glowstone" > /dev/null
check "Planner1 starts Barn" start_plan Barn "12 -60 2" "16 -58 6"
drv 1 "fill 12 -60 2 16 -60 6 minecraft:oak_planks" > /dev/null
check "client 2 sees both plans' blocks" drv 2 "await block 4 -59 3 minecraft:glowstone" \
	"await block 16 -60 6 minecraft:oak_planks"

# Copy from one plan: the marked area's 8 plan blocks become a copy placed on the block Planner1 looks down at,
# selected, with Litematica's tool in its placement mode, and no real block is placed.
drv 1 "select 3 -60 3 5 -59 4" "cmd tp @s 5.5 -60 20.5 0 90" "wait 40" > /dev/null
press 1 j
check "J copies the marked area's plan blocks, with a message" client_said 1 "Copied 8 blocks. Press K to paste them."
shot 1 copy-message
check "the copy sits on the aimed block, selected" drv 1 "await copy origin 5,-60,20 NONE NONE selected Building Planner copy"
check "with Litematica's tool in its placement mode" eval '[[ $(val 1 toolmode) == SCHEMATIC_PLACEMENT ]]'
check "the copy shows the plan's blocks as they were" eval 'state_has 1 "5 -59 20" "oak_stairs" &&
	state_has 1 "5 -59 20" "facing=north" && [[ $(val 1 "get 6 -59 20") == minecraft:glowstone ]] &&
	[[ $(val 1 "get 7 -60 21") == minecraft:stone_bricks ]] && [[ $(val 1 "get 7 -59 21") == minecraft:air ]]'
check "and places no real block" eval '[[ $(val 1 "real 5 -59 20") == minecraft:air ]]'
check "and isn't shared" eval 'drv 2 "wait 20" > /dev/null && [[ $(val 2 "get 5 -59 20") == minecraft:air ]]'
drv 1 "cmd tp @s 6.5 -59 26.5 180 30" "wait 40" > /dev/null
shot 1 copy-placed

# Moving it with a real click of Litematica's tool on the grass at 30 -61 20.
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:stick" "cmd tp @s 30.5 -60 20.5 0 90" "wait 40" \
	> /dev/null
attack_click 1
check "a click with Litematica's tool moves the copy" \
	eval 'drv 1 "wait 20" > /dev/null && [[ $(val 1 copy) =~ ^origin\ 30,-6[01],20\ NONE\ NONE\ selected ]]'
echo "  the tool click put the copy's origin at: $(val 1 copy)" >> /out/checks.log
drv 1 "movecopy 30 -60 20" "cmd tp @s 30.5 -58 26.5 180 30" "wait 40" > /dev/null

# U gives it a quarter turn clockwise around its origin: the stairs now face east.
press 1 u
check "U turns the copy a quarter turn clockwise" drv 1 "await copy origin 30,-60,20 NONE CLOCKWISE_90 selected"
check "its blocks turn with it" eval 'state_has 1 "30 -59 20" "facing=east" &&
	[[ $(val 1 "get 30 -59 21") == minecraft:glowstone ]] && [[ $(val 1 "get 29 -60 22") == minecraft:stone_bricks ]]'
shot 1 copy-turned

# H flips it left to right for Planner1, who faces south: the stairs face west, and the copy's floor moves from
# 29..30 to 30..31 east to west.
drv 1 "cmd tp @s 30.5 -58 14.5 0 30" "wait 40" > /dev/null
press 1 h
check "H mirrors the copy left to right" drv 1 "await copy origin 30,-60,20 FRONT_BACK COUNTERCLOCKWISE_90 selected"
check "its blocks flip with it" eval 'state_has 1 "30 -59 20" "facing=west" &&
	[[ $(val 1 "get 31 -60 22") == minecraft:stone_bricks ]] && [[ $(val 1 "get 29 -60 22") == minecraft:air ]]'
shot 1 copy-mirrored
# Without the tool item in hand, Litematica's tool display leaves the chat clear for the screenshots below.
drv 1 "cmd clear @s" > /dev/null

# Barn gets glass where the copy has empty space, and a gold block where the copy has stone bricks; the copy moves
# over them.
drv 1 "fill 15 -58 4 15 -58 6 minecraft:glass" "place 14 -58 6 minecraft:glass" "place 15 -59 5 minecraft:gold_block" \
	> /dev/null
check "client 2 sees Barn's glass and gold block" drv 2 "await block 14 -58 6 minecraft:glass" \
	"await block 15 -59 5 minecraft:gold_block"
drv 1 "movecopy 14 -59 4" "cmd tp @s 14.5 -57 11.5 180 30" "wait 40" > /dev/null
check "the copy moves over Barn" drv 1 "await copy origin 14,-59,4 FRONT_BACK COUNTERCLOCKWISE_90 selected"
# Where the copy and a plan overlap, Litematica shows one block; the log records which.
echo "  where the copy overlaps Barn: 15 -59 5 shows $(val 1 "get 15 -59 5") (copy stone_bricks, Barn gold_block);" \
	"15 -58 4 shows $(val 1 "get 15 -58 4") (copy empty, Barn glass)" >> /out/checks.log
shot 1 copy-over-plan

# K pastes it into Barn, where its corner is: its 8 blocks, the stairs still facing west, and its empty space leaves
# Barn's glass.
drv 1 "cmd tp Planner2 18.5 -57 11.5 150 30" "wait 40" > /dev/null
press 1 k
check "K pastes the copy into the plan at its corner, for the other player" drv 2 \
	"await block 14 -58 4 minecraft:oak_stairs" "await block 14 -58 5 minecraft:glowstone" \
	"await block 15 -59 5 minecraft:stone_bricks" "await block 14 -59 6 minecraft:stone_bricks"
check "glowing with Planner1's name" drv 2 "await glow Planner1:8"
shot 2 paste-seen
check "turned and mirrored as the copy showed it" state_has 2 "14 -58 4" "facing=west"
check "the copy's empty space leaves Barn's blocks" eval '[[ $(val 2 "get 15 -58 4") == minecraft:glass ]] &&
	[[ $(val 2 "get 15 -58 6") == minecraft:glass ]] && [[ $(val 2 "get 14 -58 6") == minecraft:glass ]]'
check "Barn keeps its size" drv 2 "await area Barn 0,0,0..4,2,4"
check "and the copy stays for another paste" drv 1 "await copy origin 14,-59,4 FRONT_BACK COUNTERCLOCKWISE_90 selected"

# One Ctrl+Z undoes the whole paste; Ctrl+Y redoes it.
drv 1 "close" > /dev/null
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "Ctrl+Z undoes the whole paste" drv 2 "await block 14 -58 4 minecraft:air" "await block 14 -58 5 minecraft:air" \
	"await block 15 -59 5 minecraft:gold_block" "await block 14 -59 6 minecraft:air" "await block 15 -58 4 minecraft:glass"
keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
check "Ctrl+Y redoes it" drv 2 "await block 14 -58 4 minecraft:oak_stairs" "await block 15 -59 5 minecraft:stone_bricks" \
	"await block 14 -59 6 minecraft:stone_bricks"

# Repeated pastes into Hall: the first's stairs rise above Hall, which grows by a layer.
drv 1 "movecopy 4 -57 6" > /dev/null
press 1 k
check "a second paste goes into Hall, which grows to fit it" drv 2 "await block 4 -56 6 minecraft:oak_stairs" \
	"await area Hall 0,0,0..6,4,6"
drv 1 "movecopy 6 -57 2" > /dev/null
press 1 k
check "and a third" drv 2 "await block 6 -56 2 minecraft:oak_stairs" "await block 7 -57 4 minecraft:stone_bricks" \
	"await area Hall 0,0,0..6,4,6"

# Copy from two plans: 6 of Hall's stone bricks and 4 of Barn's oak planks. Pasted outside every plan, the copy goes
# into Hall, which most of its blocks came from, and Hall grows to fit it.
drv 1 "select 6 -60 4 13 -60 5" > /dev/null
press 1 j
check "an area over two plans copies both" client_said 1 "Copied 10 blocks. Press K to paste them."
drv 1 "movecopy 2 -60 20" > /dev/null
check "the new copy replaces the old" eval '[[ $(val 1 copy) == "origin 2,-60,20 NONE NONE selected Building Planner copy" ]] &&
	[[ $(val 1 placements) != *"Building Planner copy"*"Building Planner copy"* ]]'
press 1 k
check "outside every plan, the paste goes into the plan it came from" drv 2 "await area Hall 0,0,0..7,4,19" \
	"await block 4 -60 21 minecraft:stone_bricks" "await block 9 -60 21 minecraft:oak_planks"

# The copy's messages: an area with no plan blocks, and a single block. A refused copy leaves the earlier one.
drv 1 "select 40 -60 40 42 -59 42" > /dev/null
press 1 j
check "J over no plan blocks gives a message" client_said 1 "There are no plan blocks in the marked area to copy."
shot 1 copy-nothing-message
check "and keeps the earlier copy" eval '[[ $(val 1 copy) == "origin 2,-60,20 "* ]]'
drv 1 "select 4 -59 3 4 -59 3" > /dev/null
press 1 j
check "a single block has its own message" client_said 1 "Copied 1 block. Press K to paste it."
shot 1 copy-one-message

# Delete clears the copy; so does removing its placement in Litematica.
press 1 Delete
check "Delete clears the copy" eval 'drv 1 "await copy none" > /dev/null &&
	[[ $(val 1 placements) != *"Building Planner copy"* ]]'
press 1 k
check "K with no copy gives a message" \
	client_said 1 "Nothing to paste. Mark an area over a plan and press J to copy it."
shot 1 paste-nothing-message
press 1 j
check "J copies again" drv 1 "await copy origin"
drv 1 "removecopy" > /dev/null
check "removing the copy's placement in Litematica clears the copy" drv 1 "await copy none"

# With the copy outside every plan and the plan it came from unloaded, K gives a message.
drv 1 "select 12 -60 2 12 -60 2" > /dev/null
press 1 j
drv 1 "await copy origin" "movecopy 40 -60 40" > /dev/null
drv 1 "syncgui" "wait 20" > /dev/null
read -r x y < <(val 1 "button Barn Unload")
click 1 "$x" "$y"
check "Planner1 unloads Barn" drv 1 "await noplacement Barn" "close"
press 1 k
check "K with no plan for the paste gives a message" \
	client_said 1 "Move the copy's corner into a plan first. The plan it came from isn't loaded."
shot 1 paste-no-plan-message

# The copy limit: Big's 51,200 plan blocks, filled in two halves, are more than the server's 50,000 for one edit.
drv 1 "cmd tp @s 80 -60 -6 0 10" "cmd tp Planner2 81 -60 -6 0 10" "wait 40" > /dev/null
check "Planner1 starts Big" start_plan Big "60 -60 2" "99 -29 41"
drv 1 "fill 60 -60 2 99 -45 41 minecraft:stone" "wait 100" "fill 60 -44 2 99 -29 41 minecraft:stone" > /dev/null
check "client 2 sees all of Big's blocks" drv 2 "await block 60 -60 2 minecraft:stone" "await block 99 -29 41 minecraft:stone"
drv 1 "select 60 -60 2 99 -29 41" > /dev/null
press 1 j
check "J over more blocks than the server's limit gives a message" \
	client_said 1 "That area has 51200 plan blocks, more than the server's limit of 50000."
shot 1 copy-too-many-message
check "and keeps the earlier copy" eval '[[ $(val 1 copy) == "origin 40,-60,40 "* ]]'

# On the same server with Building Planner left out, the five keys do nothing.
stop_client 1
stop_client 2
cp /out/client1.log /out/client1-with-addon.log
cp /out/client2.log /out/client2-with-addon.log
stop_server
check "the server starts without Building Planner" eval 'start_server "$ADDON" server-without-addon &&
	! grep -qF "Building Planner loaded" /out/server-without-addon.log'
start_client 1
# The loading screen swallows key presses, so wait for it to close.
check "client 1 starts no session" drv 1 "await noscreen" "wait 40" "await nosession"
drv 1 "cmd tp @s 5 -60 -3 0 10" "select 2 -60 2 8 -57 8" "wait 20" > /dev/null
for key in j k u h Delete; do
	press 1 "$key"
done
check "J, K, U, H and Delete make no copy and open nothing" eval 'drv 1 "wait 20" "await noscreen" > /dev/null &&
	[[ $(val 1 placements) != *"Building Planner copy"* ]]'
check "and show no Building Planner message" no_planner_message 1
shot 1 copy-keys-not-on-server
