# The player's own worlds (0.10.0): with the dev server stopped, Planner1's game opens a copy of the dev server's world
# from its saves folder, where Building Planner's server part runs inside the game. Planner1's game starts a session
# from the world's hello, and its radial menu shows no line above "Building Planner"; a Line of 4 oak planks draws ghost
# blocks and no real block, and Ctrl+Z and Ctrl+Y take it back and forward; with "Place real blocks" on, a Line places
# 4 real oak planks; a real oak plank placed by hand into a ghost one removes it. Opened to LAN, the world takes
# Planner2's game, which starts a session, shows no line in its radial menu and Planner1's ghost blocks, and draws a
# shape Planner1 sees glowing with Planner2's name. After Planner1 leaves, the area's file is in the world's folder, the
# game folder has no config file of the server part's, and when the world opens again its ghost blocks show and
# placements of old plans' files stay. Effortless Building's welcome message at each player's first join and its build
# mode hint at their first choice of a mode in the radial menu reach neither player's chat, the LAN guest's read from
# the connection as from a dedicated server. Sourced by inside.sh, which provides drv, val, check, xdo, click, keys,
# use_click, attack_click, client_said, wait_for, start_server, stop_server, start_client, stop_client and $ADDON.

world=run/client1/saves/own
# The area file of the blocks near 0 0, in the world's folder.
area1=$world/building-planner/layer/minecraft/overworld/0.0.dat
client_config=run/client1/config/buildingplanner-client.properties

# half <n>: n + 0.5.
half() {
	awk -v n="$1" 'BEGIN { print n + 0.5 }'
}

# aim <client> <x y z>: the player looks at the middle of the top of the block below x y z, so a click there starts a
# shape at x y z.
aim() {
	drv "$1" "lookat $(half "$2") $3 $(half "$4")" "wait 5" > /dev/null
}

# draw <client> <x1 y1 z1> <x2 y2 z2>: a Line's first click aimed at the top of the block under x1 y1 z1, then the aim
# at the middle of the side of x2 y2 z2 facing the player, who stands north of it, and the click that places it.
draw() {
	aim "$1" "$2" "$3" "$4"
	use_click "$1"
	drv "$1" "lookat $(half "$5") $(half "$6") $7" "wait 5" > /dev/null
	use_click "$1"
	drv "$1" "wait 10" > /dev/null
}

# shots <client> <name> [command]: that client's screen at 854 by 480 and at 1920 by 1080, GUI scale 2, once the chat
# has faded, as <name>-<size>-gui2; the window is 854 by 480 again after. The driver command, if given, runs after each
# shot, so checks.log has its answer at both sizes (the screenbox of the change, say, for outlining it).
shots() {
	local after=${3:-mark}
	drv "$1" "wait 210" "notoasts" "wait 5" "shot $2-854x480-gui2" "$after" "window 1920 1080 2" "wait 20" "notoasts" \
		"shot $2-1920x1080-gui2" "$after" "window 854 480 2" "wait 20" > /dev/null
}

# radial_texts <client>: the driver's radialline and cornertext with that client's radial menu open (left Alt held),
# separated by " || ".
radial_texts() {
	xdo "$1" keydown Alt_L
	drv "$1" "await screen Build Modes" "wait 10" > /dev/null
	echo "$(val "$1" radialline) || $(val "$1" cornertext)"
	xdo "$1" keyup Alt_L
	drv "$1" "await noscreen" "wait 5" > /dev/null
}

# choose_in_menu <client> <mode>: chooses that build mode as a player does, with a click on its segment of the radial
# menu (left Alt held), then lets the menu go with the pointer in the window's corner. The click asks Effortless
# Building's server part for its build mode hint.
choose_in_menu() {
	local x y
	xdo "$1" keydown Alt_L
	drv "$1" "await screen Build Modes" "wait 10" > /dev/null
	read -r x y < <(val "$1" "ringat $2")
	xdo "$1" mousemove --window %w "$x" "$y"
	drv "$1" "wait 5" > /dev/null
	xdo "$1" click 1
	drv "$1" "wait 5" > /dev/null
	xdo "$1" mousemove --window %w 5 5
	xdo "$1" keyup Alt_L
	drv "$1" "await noscreen" "wait 5" > /dev/null
}

# kept_out <client> <key> <text>: a game message of that translation key reached the client and was kept out of its
# chat (the driver's droppedchat), and the client's log has no chat line starting with the message's text.
kept_out() {
	drv "$1" "await droppedchat $2" > /dev/null && ! grep -qF -- "[CHAT] $3" "/out/client$1.log"
}
welcome=effortlessbuilding.message.welcome_message
welcome_text="Thank you for using Effortless Building!"
hint=effortlessbuilding.message.build_mode_hint
hint_text="You have activated a build mode!"

tags=0
# world_has <x y z> <block>: the world's own copy has that block there, as /execute if block finds it; the game's own
# server, which logs to the game's log, says a word no line of that log has yet when it has, and another once it has
# checked.
world_has() {
	tags=$((tags + 1))
	local tag=own$tags
	drv 1 "cmd execute if block $1 $2 $3 $4 run say $tag-hit" "cmd say $tag-end" > /dev/null &&
		wait_for /out/client1.log "$tag-end" 20 && grep -qF -- "$tag-hit" /out/client1.log
}

# switch_setting <client> <row>: opens "Settings", as the radial menu's button does, clicks the row's switch and then
# the screen's "Save" button, which closes it, with the client's own mouse.
switch_setting() {
	local text x y
	drv "$1" "close" "settings" "await screen Settings" "wait 10" > /dev/null
	read -r text x y < <(val "$1" "settingrow $2")
	click "$1" "$x" "$y"
	drv "$1" "wait 5" > /dev/null
	read -r x y < <(val "$1" "widget 0 Save")
	click "$1" "$x" "$y"
	drv "$1" "await noscreen" "wait 5" > /dev/null
}

# The dev server's world, copied into Planner1's saves folder as "own" once the server has stopped and saved it. The
# world has cheats off, as a dedicated server makes it; the driver's cmd runs on the game's own server, as an operator.
stop_client 1
stop_client 2
stop_server
mkdir -p run/client1/saves
cp -r run/server/world "$world"
start_client 1 "" "" --quickPlaySingleplayer own

# The loading screen swallows key presses, so each check waits for it to close.
check "Planner1's game starts a session from its own world's hello" eval \
	'drv 1 "await noscreen" "await session" > /dev/null && [[ $(val 1 sessionkind) == server ]]'
check "and its radial menu doesn't show the line above \"Building Planner\"" eval \
	'[[ $(radial_texts 1) == "none || Building Planner | "* ]]'
# Effortless Building's server part, which the game runs, sends its welcome message at a player's first join of the
# world, and its build mode hint at their first choice of a mode in the radial menu; neither reaches chat.
check "Effortless Building's welcome message reaches Planner1's game and stays out of its chat" kept_out 1 "$welcome" \
	"$welcome_text"

# A Line of 4 oak planks from 20 -60 20 eastward, drawn by Planner1 standing north of it, in Line chosen in the radial
# menu.
drv 1 "window 854 480 2" "ebmode SINGLE" "wait 10" > /dev/null
choose_in_menu 1 LINE
check "Planner1 chooses Line in its radial menu" eval '[[ $(val 1 ebmode) == LINE ]]'
check "and Effortless Building's build mode hint reaches its game and stays out of its chat" kept_out 1 "$hint" \
	"$hint_text"
drv 1 "cmd clear @s" "cmd item replace entity @s weapon.mainhand with minecraft:oak_planks 64" \
	"cmd tp @s 21.5 -60 16.5 0 30" "wait 40" "await loaded minecraft:overworld 20 20" > /dev/null
draw 1 20 -60 20 23 -60 20
check "a Line of 4 oak planks draws 4 ghost blocks" eval 'drv 1 "await block 23 -60 20 minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 "countblocks 20 23 -60 20 minecraft:oak_planks") == 4 ]]'
check "and places no real block" eval '! world_has 20 -60 20 minecraft:oak_planks &&
	! world_has 23 -60 20 minecraft:oak_planks && [[ $(val 1 "real 20 -60 20") == minecraft:air ]]'
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "Ctrl+Z takes the Line back" eval 'drv 1 "await block 20 -60 20 minecraft:air" > /dev/null &&
	[[ $(val 1 "countblocks 20 23 -60 20 minecraft:oak_planks") == 0 ]]'
keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
check "and Ctrl+Y draws it again" eval 'drv 1 "await block 23 -60 20 minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 "countblocks 20 23 -60 20 minecraft:oak_planks") == 4 ]]'
check "and no Effortless Building packet that changes real blocks leaves the game" \
	eval '! val 1 "ebsent" | grep -qE "place_build_mode|break_build_mode|undo|redo"'
# The shots from a few blocks farther back, in Disable, where no preview and no line of controls shows.
drv 1 "ebmode DISABLED" "cmd tp @s 21.5 -60 12.5 0 25" "wait 20" > /dev/null
shots 1 own-ghost-blocks "screenbox 20 -60 20 23 -60 20"

# With "Place real blocks" on, a Line of 4 oak planks from 40 -60 46 eastward places 4 real ones, through the server
# part's place.
switch_setting 1 "Place real blocks"
check "\"Place real blocks\" is on, as \"Settings\" saved it" grep -q "^place-real-blocks=true" "$client_config"
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:oak_planks 64" "ebmode LINE" \
	"cmd tp @s 41.5 -60 42.5 0 30" "await loaded minecraft:overworld 40 46" "wait 20" > /dev/null
draw 1 40 -60 46 43 -60 46
check "with it on, a Line of 4 oak planks places 4 real ones" eval \
	'drv 1 "await real 40 -60 46 minecraft:oak_planks" "await real 43 -60 46 minecraft:oak_planks" > /dev/null &&
	world_has 40 -60 46 minecraft:oak_planks && world_has 43 -60 46 minecraft:oak_planks'
check "and no ghost block" eval '[[ $(val 1 "layer 40 -60 46") == minecraft:air ]]'
switch_setting 1 "Place real blocks"
check "\"Place real blocks\" is off again" grep -q "^place-real-blocks=false" "$client_config"

# In Disable, where right clicks are Minecraft's, a real oak plank Planner1 places by hand into a ghost oak plank at
# 26 -60 20 removes it, as hook S1 sees the change in the game's own world.
drv 1 "ebmode DISABLED" "cmd tp @s 26.5 -60 17.5 0 30" "await loaded minecraft:overworld 26 20" "wait 20" \
	"edit 26 -60 20 26 -60 20 minecraft:oak_planks" "await inlayer 26 -60 20 minecraft:oak_planks" \
	"lookat 26.5 -60 20.5" "wait 5" > /dev/null
use_click 1
check "a real oak plank Planner1 places into a ghost oak plank removes it" eval \
	'drv 1 "await real 26 -60 20 minecraft:oak_planks" "await inlayer 26 -60 20 minecraft:air" > /dev/null &&
	world_has 26 -60 20 minecraft:oak_planks'

# Opened to LAN, the world takes Planner2's game at the port the driver gives.
port=$(val 1 lan)
echo "  the LAN port: $port" >> /out/checks.log
check "the world opens to LAN" eval '[[ $port =~ ^[0-9]+$ ]]'
start_client 2 "" "" --quickPlayMultiplayer "localhost:$port"
check "Planner2's game starts a session from the world's hello" eval \
	'drv 2 "await noscreen" "await session" > /dev/null && [[ $(val 2 sessionkind) == server ]]'
check "and its radial menu doesn't show the line above \"Building Planner\"" eval \
	'[[ $(radial_texts 2) == "none || Building Planner | "* ]]'
# Planner2's game reads both messages from the LAN connection, encoded as a dedicated server sends them.
check "Effortless Building's welcome message reaches Planner2's game and stays out of its chat" kept_out 2 "$welcome" \
	"$welcome_text"
drv 2 "window 854 480 2" "ebmode SINGLE" "wait 10" > /dev/null
choose_in_menu 2 LINE
check "Planner2 chooses Line in its radial menu" eval '[[ $(val 2 ebmode) == LINE ]]'
check "and Effortless Building's build mode hint reaches its game and stays out of its chat" kept_out 2 "$hint" \
	"$hint_text"
drv 1 "ebmode LINE" "cmd tp @s 21.5 -60 16.5 0 30" "cmd tp Planner2 24.5 -60 16.5 0 30" "wait 40" > /dev/null
check "and shows Planner1's ghost blocks" eval 'drv 2 "await loaded minecraft:overworld 20 20" \
	"await block 23 -60 20 minecraft:oak_planks" > /dev/null &&
	[[ $(val 2 "countblocks 20 23 -60 20 minecraft:oak_planks") == 4 ]]'
# Planner2's Line of stone bricks from 20 -60 18 eastward, between the players and Planner1's Line, since an aim
# through ghost blocks lands on them. A LAN guest may not run commands, so the host gives Planner2 its blocks.
drv 1 "cmd clear Planner2" "cmd item replace entity Planner2 weapon.mainhand with minecraft:stone_bricks 64" "wait 20" \
	> /dev/null
draw 2 20 -60 18 23 -60 18
check "a Line Planner2 draws reaches Planner1, glowing with Planner2's name" drv 1 \
	"await block 23 -60 18 minecraft:stone_bricks" "await glow Planner2:4"
drv 1 "ebmode DISABLED" "cmd tp @s 21.5 -60 12.5 0 25" "wait 20" > /dev/null
shots 1 own-lan-guest "screenbox 20 -60 18 23 -60 20"

# Placements of old plans' files, which a session with another server removes, then a leave: Planner2's game loses the
# world, and the world's stop writes the area's file into the world's folder.
drv 1 "oldplans 60 -61 60" "wait 5" "leave" "wait 40" > /dev/null
check "after Planner1 leaves, the area's file is in the world's folder" eval '[[ -s $area1 ]]'
check "and the game folder has no config file of the server part's" eval \
	'[[ ! -e run/client1/config/buildingplanner.properties ]]'
stop_client 1
stop_client 2
start_client 1 "" "" --quickPlaySingleplayer own
check "when the world opens again, its ghost blocks show" eval 'drv 1 "await noscreen" "await session" \
	"cmd tp @s 21.5 -60 16.5 0 30" "await loaded minecraft:overworld 20 20" \
	"await block 23 -60 18 minecraft:stone_bricks" > /dev/null &&
	[[ $(val 1 "countblocks 20 23 -60 20 minecraft:oak_planks") == 4 ]]'
check "and the placements of old plans' files stay" eval 'drv 1 "wait 20" > /dev/null &&
	[[ $(val 1 placements) == *"Old shared plan"* && $(val 1 placements) == *"Old started plan"* ]]'
