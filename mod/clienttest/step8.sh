# Step 8's in-game checks: a client without the add-on on a server with it, clients with the add-on on the same
# server with Building Planner left out (Syncmatica still in), and a hello with another protocol version. Sourced by
# inside.sh, which provides drv, val, check, keys, click, use_click, client_said, start_server, stop_server,
# start_client, stop_client and $ADDON.

not_on_server="Building Planner isn't on this server. Your Litematica edits stay on your screen, and Effortless Building's shapes place nothing."
other_version="This server runs a different version of Building Planner, so it's off here. Your Litematica edits stay on your screen, and Effortless Building's shapes place nothing."

# On a server with the add-on: sessions start, with no notice. Planner1 starts the plan Hall (2 -60 2 to 8 -57 8),
# which Syncmatica keeps for the server without the add-on below.
check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"
drv 1 "cmd tp @s 5 -60 -3 0 10" "cmd tp Planner2 6 -60 -3 0 10" "wait 40" "select 2 -60 2 8 -57 8" > /dev/null
keys 1 key n
drv 1 "await screen Start a plan" > /dev/null
keys 1 type Hall
keys 1 key Return
check "Planner1 starts Hall and client 2 sees it" \
	eval 'drv 1 "await placement Hall" && drv 2 "await placement Hall"'
check "neither client gets the notice" eval '! grep -qF "[CHAT] Building Planner isn" /out/client1.log /out/client2.log'

# A client without the add-on (its classes and the driver's left off the classpath) joins the server with it and
# plays: it stays connected and places a real block.
cp /out/client2.log /out/client2-with-addon.log
# The server logs the old connection's end before Planner2 rejoins; the later checks read the lines after it.
stop_client 2
wait_for /out/server.log "Planner2 left the game"
joined=$(wc -l < /out/server.log)
start_client 2 "$ADDON"
check "a client without the add-on joins the server with it" \
	timeout 300 grep -q -m1 -F "Planner2 joined the game" <(tail -n +$((joined + 1)) -F /out/server.log)
check "and loaded without the add-on" eval '! grep -qE "^\s+- buildingplanner " /out/client2.log'
# Planner2 stands north of 20 -60 20, looking down at it, and places a block there with a real right-click.
drv 1 "wait 200" "cmd clear Planner2" "cmd item replace entity Planner2 weapon.mainhand with minecraft:gold_block" \
	"cmd tp Planner2 20.5 -60 18.5 0 45" "wait 60" > /dev/null
use_click 2
check "and places a real block" drv 1 "await real 20 -60 20 minecraft:gold_block"
check "and stays connected" eval \
	'! tail -n +$((joined + 1)) /out/server.log | grep -qE "Planner2 lost connection|Disconnecting Planner2"'

# The same server with Building Planner left out, Syncmatica still in; both clients have the add-on.
stop_client 1
stop_client 2
cp /out/client1.log /out/client1-with-addon.log
cp /out/client2.log /out/client2-without-addon.log
stop_server
check "the server starts without Building Planner" eval 'start_server "$ADDON" server-without-addon &&
	! grep -qF "Building Planner loaded" /out/server-without-addon.log'
start_client 1
start_client 2
# The loading screen swallows key presses and hides chat, so wait for it to close.
check "client 1 gets the notice and starts no session" \
	eval 'client_said 1 "$not_on_server" && drv 1 "await noscreen" "wait 40" "await nosession"'
check "client 2 gets the notice too" eval 'client_said 2 "$not_on_server" && drv 2 "await noscreen"'
drv 1 "notoasts" "wait 5" "shot notice-not-on-server" > /dev/null

# load <client>: loads Hall from Syncmatica's list, with its Load button (or Download, without the file).
load() {
	local x y
	drv "$1" "syncgui" "wait 20" > /dev/null
	read -r x y < <(val "$1" "button Hall Load")
	if [[ $x == none ]]; then
		read -r x y < <(val "$1" "button Hall Download")
	fi
	click "$1" "$x" "$y"
	drv "$1" "await placement Hall" "close"
}
check "client 1 loads Hall from Syncmatica" load 1
check "client 2 loads Hall from Syncmatica" load 2

# Litematica's edits stay on that player's screen.
drv 1 "fill 2 -60 2 8 -60 8 minecraft:stone_bricks" > /dev/null
check "client 1's Litematica fill shows on its own screen" drv 1 "await block 8 -60 8 minecraft:stone_bricks"
check "and not on client 2's" eval 'drv 2 "wait 100" && [[ $(val 2 "get 8 -60 8") == minecraft:air ]]'

# Effortless Building's shapes place nothing: four oak planks from 4 -60 5 eastward, in Hall's stone-brick layer, add
# no ghost blocks and no real ones, and the player stays connected.
drv 1 "cmd clear @s" "cmd item replace entity @s weapon.mainhand with minecraft:oak_planks" "ebmode SINGLE" \
	"ebarray 3 1 0 0" "cmd tp @s 4 -55 5 0 90" "wait 40" > /dev/null
use_click 1
check "a shape adds no ghost blocks" eval 'drv 1 "wait 60" && [[ $(val 1 "get 4 -60 5") == minecraft:stone_bricks ]]'
check "and places no real blocks" \
	eval '[[ $(val 1 "real 4 -60 5") == minecraft:air && $(val 1 "real 7 -60 5") == minecraft:air ]]'
check "and the player stays connected" \
	eval '! grep -qE "Planner1 lost connection|Disconnecting Planner1" /out/server-without-addon.log'
drv 1 "ebarray 0" > /dev/null

# A hello from a server with another protocol version gives its own notice and starts no session.
drv 1 "cmd tp @s 5 -60 -3 0 10" "wait 20" "hello 99" > /dev/null
check "a hello with another protocol version gives its notice and starts no session" \
	eval 'client_said 1 "$other_version" && drv 1 "await nosession"'
# The screenshot is written after the command returns; wait for it before inside.sh copies it.
drv 1 "notoasts" "wait 5" "shot notice-other-version" "wait 20" > /dev/null
