# Step 8's in-game checks: clients without the add-on, or without Litematica and MaLiLib, on a server with it; clients
# with the add-on on the same server with Building Planner left out, each starting a local session; and a hello with
# protocol version 4, as Building Planner 0.5.0 sends, which ends that session, after which Left Ctrl doesn't turn the
# block in hand. Sourced by inside.sh, which provides drv, val, check, xdo, click, keys, use_click, attack_click,
# wait_for, start_server, stop_server, start_client, stop_client, no_planner_message, $ADDON and $DRIVER.

# said_since <line> <text>: returns when the server log, after that line, has a line containing the text.
said_since() {
	timeout 300 grep -q -m1 -F -- "$2" <(tail -n +$(($1 + 1)) -F /out/server.log)
}

# joined_since <line>: the server log, after that line, says Planner2 joined.
joined_since() {
	said_since "$1" "Planner2 joined the game"
}

# stayed_since <line>: the server log, after that line, doesn't say Planner2's connection ended.
stayed_since() {
	! tail -n +$(($1 + 1)) /out/server.log | grep -qE "Planner2 lost connection|Disconnecting Planner2"
}

# rejoin_2 [left-out]: stops client 2 and starts it again with the classpath entries matching the argument left out;
# sets joined to the server log's line count from before it rejoins, after the server logged this leaving, and
# debug_joined to the debug log's from before it left.
rejoin_2() {
	local before
	before=$(wc -l < /out/server.log)
	debug_joined=$(wc -l < "$debug_log")
	stop_client 2
	said_since "$before" "Planner2 left the game"
	joined=$(wc -l < /out/server.log)
	start_client 2 "${1:-}"
}

# On a server with the add-on, both clients start a session.
check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"
# The server notes each list message it sends in its debug log (the dev server's logs/debug.log), so a list message to
# a game without the add-on would show there; here, both games with the add-on got theirs at join.
# lists_sent_since <line> <player>: the debug log, after that line, notes a list message sent to the player.
debug_log=run/server/logs/debug.log
lists_sent_since() {
	tail -n +$(($1 + 1)) "$debug_log" | grep -qF "Building Planner sent $2 the list message buildingplanner:"
}
check "the server's debug log notes the list messages it sent each game at join" eval \
	'lists_sent_since 0 Planner1 && lists_sent_since 0 Planner2'
cp /out/client2.log /out/client2-with-addon.log

# A client without the add-on (its classes and the driver's left off the classpath) joins the server with it and
# plays: it stays connected and places a real block.
rejoin_2 "$ADDON"
check "a client without the add-on joins the server with it" joined_since "$joined"
check "and loaded without the add-on" eval '! grep -qE "^\s+- buildingplanner " /out/client2.log'
# Planner2 stands north of 20 -60 20, looking down at it, and places a block there with a real right-click, held for a
# time since the client has no driver.
drv 1 "cmd tp @s 5 -60 -3 0 10" "wait 200" "cmd clear Planner2" \
	"cmd item replace entity Planner2 weapon.mainhand with minecraft:gold_block" "cmd tp Planner2 20.5 -60 18.5 0 45" \
	"wait 60" > /dev/null
xdo 2 mousedown 3 sleep 0.3 mouseup 3
check "and places a real block" drv 1 "await real 20 -60 20 minecraft:gold_block"
check "and stays connected" stayed_since "$joined"
check "and the server sends it no list message and logs no error" eval \
	'! lists_sent_since "$debug_joined" Planner2 && ! tail -n +$((joined + 1)) /out/server.log | grep -qE "/ERROR\]|Exception"'
cp /out/client2.log /out/client2-without-addon.log

# A client with the add-on and Effortless Building but without Litematica and MaLiLib (and the driver, which uses
# them) loads, with Building Planner off, and plays on the server with the add-on.
rejoin_2 "litematica malilib $DRIVER"
check "a client without Litematica and MaLiLib joins the server with the add-on" joined_since "$joined"
check "and loaded with the add-on and Effortless Building but not Litematica" eval 'grep -qE "^\s+- buildingplanner " /out/client2.log &&
	grep -qE "^\s+- effortlessbuilding " /out/client2.log && ! grep -qE "^\s+- (litematica|malilib) " /out/client2.log'
check "and Building Planner is off on it" grep -qF "Litematica or MaLiLib is missing, so Building Planner is off" /out/client2.log
drv 1 "cmd tp Planner2 21.5 -60 18.5 0 45" "wait 200" > /dev/null
check "and it stays connected, with no mixin error" eval 'stayed_since "$joined" && kill -0 "$(cat /tmp/client2.pid)" &&
	! grep -qE "Mixin apply|MixinApplyError|InvalidInjectionException|InvalidMixinException|NoClassDefFoundError|Game crashed" \
		/out/client2.log'
cp /out/client2.log /out/client2-without-litematica.log

# The same server with Building Planner left out; both clients have the add-on, and each starts a local session.
stop_client 1
stop_client 2
cp /out/client1.log /out/client1-with-addon.log
stop_server
check "the server starts without Building Planner" eval 'start_server "$ADDON" server-without-addon &&
	! grep -qF "Building Planner loaded" /out/server-without-addon.log'
start_client 1
start_client 2
# The loading screen swallows key presses, so wait for it to close.
check "client 1 starts a local session" eval 'drv 1 "await noscreen" "await session" > /dev/null &&
	[[ $(val 1 sessionkind) == local ]]'
check "client 2 starts a local session" eval 'drv 2 "await noscreen" "await session" > /dev/null &&
	[[ $(val 2 sessionkind) == local ]]'
keys 1 key b
check "the block mix key opens the mix screen" drv 1 "await screen Block mix"
read -r x y < <(val 1 "widget 0 Cancel")
click 1 "$x" "$y"
drv 1 "await noscreen" > /dev/null

# Effortless Building's shapes draw ghost blocks on Planner1's client alone: four oak planks from 4 -60 5 eastward add
# ghost blocks there, none on Planner2's client and no real ones, and the player stays connected.
drv 1 "cmd clear @s" "cmd item replace entity @s weapon.mainhand with minecraft:oak_planks" "ebmode SINGLE" \
	"ebarray 3 1 0 0" "cmd tp @s 4 -55 5 0 90" "wait 40" > /dev/null
use_click 1
check "a shape adds ghost blocks on Planner1's client" eval 'drv 1 "await block 7 -60 5 minecraft:oak_planks" > /dev/null &&
	[[ $(val 1 "countblocks 4 7 -60 5 minecraft:oak_planks") == 4 ]]'
check "and none on Planner2's" eval 'drv 2 "wait 20" > /dev/null &&
	[[ $(val 2 "countblocks 4 7 -60 5 minecraft:oak_planks") == 0 ]]'
check "and places no real blocks" \
	eval '[[ $(val 1 "real 4 -60 5") == minecraft:air && $(val 1 "real 7 -60 5") == minecraft:air ]]'
check "and the player stays connected" \
	eval '! grep -qE "Planner1 lost connection|Disconnecting Planner1" /out/server-without-addon.log'
# The undo and redo keys and the break click would send undo, redo and break packets too. The driver's counter sees
# each packet as it leaves the client; the server without Effortless Building couldn't show that it never got them.
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
attack_click 1
drv 1 "wait 20" > /dev/null
check "no place, break, undo or redo packet of Effortless Building leaves the client" \
	eval '! val 1 "ebsent" | grep -qE "place_build_mode|break_build_mode|undo|redo"'
drv 1 "ebarray 0" > /dev/null

# A hello with protocol version 4, as the server with Building Planner 0.5.0 sends, ends the local session and starts
# none.
drv 1 "cmd tp @s 5 -60 -3 0 10" "wait 20" "hello 4" > /dev/null
check "a hello with protocol version 4 ends the local session and starts none" drv 1 "wait 20" "await nosession"
# The screenshot is written after the command returns; wait for it before inside.sh copies it.
drv 1 "notoasts" "wait 5" "shot off-other-version" "wait 20" > /dev/null
# Without a session, Left Ctrl doesn't turn the block in hand (step 18): in Effortless Building's Disable, with oak
# stairs in hand, Left Ctrl with a notch up changes the hotbar slot, and a right click with Left Ctrl held places the
# stair as Minecraft places it, facing south, the way Planner1 faces.
drv 1 "ebmode DISABLED" "cmd item replace entity @s hotbar.4 with minecraft:oak_stairs" "cmd tp @s 5.5 -60 -2.5 0 30" \
	"wait 20" > /dev/null
keys 1 key 5
drv 1 "lookat 5.5 -60 0.5" "wait 5" > /dev/null
xdo 1 keydown Control_L
drv 1 "await sprint true" "wait 5" > /dev/null
xdo 1 click 4
check "after that hello, Left Ctrl with the wheel changes the hotbar slot" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 slot) == "3 "* ]]'
xdo 1 keyup Control_L
keys 1 key 5
drv 1 "await sprint false" "wait 5" > /dev/null
check "and the stairs are in hand again" eval '[[ $(val 1 slot) == "4 minecraft:oak_stairs" ]]'
xdo 1 keydown Control_L
drv 1 "await sprint true" "wait 5" > /dev/null
use_click 1
xdo 1 keyup Control_L
check "and a right click with Left Ctrl held places the stair as Minecraft places it" eval \
	'drv 1 "await real 5 -60 0 minecraft:oak_stairs" > /dev/null && [[ $(val 1 "realstate 5 -60 0") == *"facing=south,"* ]]'

check "neither client showed a Building Planner message on the server without it" \
	eval 'no_planner_message 1 && no_planner_message 2'
