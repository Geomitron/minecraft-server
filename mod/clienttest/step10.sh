# Step 10's in-game checks: hold to repeat in Effortless Building's Single mode, with the radial menu's speed sliders,
# editing from Freecam's camera, and the look Building Planner sets for Litematica's ghost blocks once. Sourced by
# inside.sh, which provides drv, val, check, keys, xdo, use_click, attack_click, client_said, start_client, stop_client
# and stop_server. Each client starts with a fresh game folder, so its first start is the first start.

# The look, on a first start: both of Litematica's look options are set and its tool item and its drawing of area
# selections turned off, saved to its config, and recorded. Each client also saved a first start before it joined the
# server.
marker=config/buildingplanner-client.properties

# layer_json <client> <hotkey>: that hotkey's entry in the client's Litematica config, compacted, up to the next entry
# of the file's Hotkeys section (each entry starts with a quoted name and a colon). It goes to the log too.
layer_json() {
	tr -d ' \n\r\t' < "run/client$1/config/litematica.json" | grep -o "\"$2\":.*" |
		sed -E 's/^("[^"]*":(\{[^{}]*(\{[^{}]*\})*[^{}]*\}|"[^"]*"|[^,{}]*)).*/\1/' | tee -a /out/checks.log
}
for n in 1 2; do
	check "client $n starts a session on hello" drv $n "await session" "await noscreen"
	check "client $n's first start sets translucent blocks at 0.8" \
		eval '[[ $(val '$n' look) == "translucent=true alpha=0.8" ]]'
	check "client $n saved both to Litematica's config" eval \
		'tr -d " \n\r" < run/client'$n'/config/litematica.json | grep -q "\"ghostBlockAlpha\":0.8" &&
		 tr -d " \n\r" < run/client'$n'/config/litematica.json | grep -Eq "\"renderBlocksAsTranslucent\":(true|\{\"enabled\":true)"'
	check "client $n recorded the look group in Building Planner's own config" eval \
		'grep -q "^look-defaults-applied=true" run/client'$n'/'$marker
	check "client $n's first start turns Litematica's drawing of area selections off" eval \
		'[[ $(val '$n' areaoption) == false ]]'
	check "and saves that to Litematica's config" eval \
		'tr -d " \n\r" < run/client'$n'/config/litematica.json |
		 grep -Eq "\"enableAreaSelectionBoxesRendering\":(false|\{\"enabled\":false)"'
	check "client $n recorded the area selection group in Building Planner's own config" eval \
		'grep -q "^area-selection-off-applied=true" run/client'$n'/'$marker
	check "client $n's first start turns Litematica's tool item off" eval '[[ $(val '$n' toolitem) == false ]]'
	check "and saves that to Litematica's config" eval \
		'tr -d " \n\r" < run/client'$n'/config/litematica.json | grep -Eq "\"toolItemEnabled\":(false|\{\"enabled\":false)"'
	check "client $n's first start gives Freecam's menu key to M and Litematica's menu hotkey none" eval \
		'[[ $(val '$n' menukeys) == "freecam=key.keyboard.m litematica=none" ]]'
	check "and saves Freecam's key to Minecraft's options" eval \
		'grep -q "^key_key.freecam.config.open:key.keyboard.m$" run/client'$n'/options.txt'
	check "and Litematica's hotkey to its config, where it is no longer M" eval \
		'tr -d " \n\r" < run/client'$n'/config/litematica.json | grep -o "\"openGuiMainMenu\":[^}]*}\?" |
		 tee -a /out/checks.log | grep -vq "\"M\""'
	check "client $n recorded the menu key group in Building Planner's own config" eval \
		'grep -q "^menu-key-applied=true" run/client'$n'/'$marker
	check "client $n's first start frees Page Up and Page Down from Litematica's layer hotkeys" eval \
		'[[ $(val '$n' layerkeys) == "next=none previous=none" ]]'
	check "and saves both to its config, where neither holds Page Up or Page Down" eval \
		'layer_json '$n' layerNext | grep -q . && layer_json '$n' layerPrevious | grep -q . &&
		 ! layer_json '$n' layerNext | grep -q PAGE_ && ! layer_json '$n' layerPrevious | grep -q PAGE_'
	check "client $n recorded the layer keys group in Building Planner's own config" eval \
		'grep -q "^layer-keys-applied=true" run/client'$n'/'$marker
done

# M opens Freecam's screen, and no screen of Litematica's. Litematica's M,L hotkey for its material list (M held, then
# L) can't be typed either: Freecam's screen is open before L goes down. Escape closes the screen.
keys 1 key m
check "M opens Freecam's screen" eval 'drv 1 "wait 20" > /dev/null && cls=$(val 1 screenclass) &&
	echo "  Freecam'"'"'s screen: $cls" >> /out/checks.log && [[ $cls == *freecam* || $cls == *shedaniel* ]]'
drv 1 "notoasts" "wait 5" "shot freecam-menu" "wait 5" > /dev/null
keys 1 key Escape
check "Escape closes it" drv 1 "await noscreen"
keys 1 keydown m sleep 0.3 keydown l sleep 0.3 keyup l keyup m
check "M then L opens none of Litematica's screens, only Freecam's" eval 'drv 1 "wait 20" > /dev/null &&
	cls=$(val 1 screenclass) && [[ $cls == *freecam* || $cls == *shedaniel* ]] && [[ $cls != fi.dy.masa.* ]]'
drv 1 "notoasts" "wait 5" "shot freecam-menu-after-m-l" "wait 5" > /dev/null
keys 1 key Escape
check "and Escape closes it" drv 1 "await noscreen"

# A Page Up press reaches a key mapping bound to it (the driver's own), once Litematica's layer hotkeys have no key. As
# controls, the same press does not while the player has Litematica's hotkey on Page Up (the driver's setlayerkeys),
# and does once it has no key again. A press is a real key press (X's name for Page Up is Prior), in the game with no
# screen open, where Litematica's hotkeys act.
# page_up_presses: presses Page Up and prints how many reached the key mapping.
page_up_presses() {
	drv 1 "pagepresses" > /dev/null
	keys 1 key Prior
	drv 1 "wait 10" > /dev/null
	val 1 pagepresses
}
check "a Page Up press reaches a key mapping bound to it" eval '[[ $(page_up_presses) == 1 ]]'
drv 1 "setlayerkeys PAGE_UP PAGE_DOWN" > /dev/null
check "with Litematica's layer hotkey on Page Up again, the press doesn't (the check can tell)" eval \
	'[[ $(page_up_presses) == 0 ]]'
drv 1 "setlayerkeys none none" > /dev/null
check "and with both hotkeys cleared again, it does" eval '[[ $(page_up_presses) == 1 ]]'

# walk <from> <to>: the commands that move Planner1 one block east every two ticks, 2.5 -55 5.5 and on, looking
# straight down, so each held click lands on a new block of the row z 5 at y -60 (-61 is the grass below).
walk() {
	local x
	for ((x = $1; x <= $2; x++)); do
		printf '%s\n' "cmd tp @s $x.5 -55 5.5 0 90" "wait 2"
	done
}

# hold_while_walking <button>: holds that mouse button for 2.5 seconds, as a real press, while Planner1 walks on past
# the end of the hold, so the button never stays down over a block it has already acted on.
hold_while_walking() {
	local steps
	mapfile -t steps < <(walk 3 40)
	xdo 1 mousedown "$1" sleep 2.5 mouseup "$1" &
	local holder=$!
	drv 1 "${steps[@]}" > /dev/null
	wait "$holder"
}

# hold_ok <log> <button> <period>: the hold log shows the edits a held button gives: one when it goes down, the first
# repeat 4 ticks later (the game's own wait) or <period> ticks later if that's longer, then one every <period> ticks.
hold_ok() {
	local log=$1 down edits gaps first=$(($3 > 4 ? $3 : 4)) repeats=0
	down=${log#*"$2="}
	down=${down%% *}
	edits=${log#*edits=}
	edits=${edits%% *}
	gaps=${log#*gaps=}
	((down - 1 < first)) || repeats=$(((down - 1 - first) / $3 + 1))
	((edits == 1 + repeats)) || return 1
	[[ -z $gaps ]] || [[ ${gaps%%,*} == "$first" ]] || return 1
	[[ $gaps != *,* ]] || [[ $(tr ',' '\n' <<< "${gaps#*,}" | sort -u) == "$3" ]]
}

# Both players stand south of the row.
drv 1 "cmd tp @s 5 -60 -3 0 10" "cmd tp Planner2 6 -60 -3 0 10" "wait 40" > /dev/null

# Holding the use button in Single mode keeps placing: stone bricks in a row, one new block each repeat.
drv 1 "cmd clear @s" "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "ebmode SINGLE" \
	"ebarray 0" "cmd tp @s 2.5 -55 5.5 0 90" "wait 30" "holdreset" > /dev/null
hold_while_walking 3
drv 1 "wait 20" > /dev/null
placed_log=$(val 1 holdlog)
echo "  hold use: $placed_log" >> /out/checks.log
check "holding use sends an edit when it goes down and every 4 ticks after" hold_ok "$placed_log" use 4
placed=${placed_log#*edits=}
placed=${placed%% *}
check "each repeat put a ghost block in the row, on client 1 and on client 2" eval \
	'drv 1 "await block 2 -60 5 minecraft:stone_bricks" > /dev/null &&
	 [[ $(val 1 "countblocks 2 40 -60 5 minecraft:stone_bricks") == '"$placed"' ]] &&
	 [[ $(val 2 "countblocks 2 40 -60 5 minecraft:stone_bricks") == '"$placed"' ]]'
check "and placed no real block" eval '[[ $(val 1 "real 2 -60 5") == minecraft:air ]]'
drv 1 "cmd tp @s 20 -55 -5 0 40" "wait 20" "notoasts" "wait 5" "shot hold-placed" > /dev/null

# Holding the attack button keeps erasing: the whole row is filled first, so every repeat has a block to erase.
drv 1 "edit 2 -60 5 40 -60 5 minecraft:stone_bricks" "await block 40 -60 5 minecraft:stone_bricks" \
	"cmd tp @s 2.5 -55 5.5 0 90" "wait 30" "holdreset" > /dev/null
hold_while_walking 1
drv 1 "wait 20" > /dev/null
erased_log=$(val 1 holdlog)
echo "  hold attack: $erased_log" >> /out/checks.log
check "holding attack sends an edit when it goes down and every 6 ticks after" hold_ok "$erased_log" attack 6
erased=${erased_log#*edits=}
erased=${erased%% *}
check "each repeat erased one ghost block, on client 1 and on client 2" eval \
	'[[ $(val 1 "countblocks 2 40 -60 5 minecraft:stone_bricks") == '"$((39 - erased))"' ]] &&
	 [[ $(val 2 "countblocks 2 40 -60 5 minecraft:stone_bricks") == '"$((39 - erased))"' ]]'
check "and left the real blocks" eval '[[ $(val 1 "real 2 -61 5") == minecraft:grass_block ]]'

# The other modes keep Effortless Building's own behaviour: a held click in Line mode, which needs two clicks, sends
# nothing, and with the mode disabled the click isn't Building Planner's at all.
for mode in LINE DISABLED; do
	drv 1 "ebmode $mode" "cmd tp @s 30.5 -55 5.5 0 90" "wait 30" "holdreset" > /dev/null
	xdo 1 mousedown 3
	drv 1 "wait 30" > /dev/null
	xdo 1 mouseup 3
	drv 1 "wait 10" > /dev/null
	check "holding use in $mode mode sends no edit" eval '[[ $(val 1 holdlog) == *"edits=0 "* ]]'
done

# The radial menu's sliders, opened with the menu's key (left Alt) held: a click on the placing slider sets 10 blocks
# a second (2 ticks), and a drag of the erasing slider's handle sets 2 a second (10 ticks).
drv 1 "ebmode SINGLE" "cmd tp @s 20 -58 -5 0 30" "wait 20" "notoasts" > /dev/null

# radial_at <width> <height> <gui scale>: opens the radial menu in a window of that size at that GUI scale, with the
# pointer over "Single" (about 33 right of the menu's middle and 35 above it) so its description shows 100 below the
# middle, and shoots it as radial-<width>x<height>. The sliders' boxes go to the log. It fails unless both sliders end
# above the description and start on the screen.
radial_at() {
	local bx by bw bh kx ky kw kh
	drv 1 "window $1 $2 $3" "wait 20" > /dev/null
	xdo 1 keydown Alt_L
	drv 1 "await screen Build Modes" "wait 10" > /dev/null
	xdo 1 mousemove --window %w $(($1 / 2 + 33 * $3)) $(($2 / 2 - 35 * $3))
	drv 1 "wait 10" "notoasts" "shot radial-$1x$2" > /dev/null
	read -r bx by bw bh < <(val 1 "box Build speed")
	read -r kx ky kw kh < <(val 1 "box Break speed")
	echo "  at $1x$2, GUI scale $3: the sliders' boxes $bx $by $bw $bh and $kx $ky $kw $kh" >> /out/checks.log
	xdo 1 keyup Alt_L
	drv 1 "await noscreen" > /dev/null
	((ky + kh <= $2 / 2 + 100 * $3 && bx >= 0 && kx >= 0))
}
check "the sliders end above a mode's description and stay on the screen" radial_at 854 480 2
check "and at GUI scale 4" radial_at 1920 1080 4
drv 1 "window 854 480 2" "wait 20" > /dev/null

# The menu's buttons' names, which texts lists after the sliders' labels.
buttons="Show or hide ghost blocks | Block mix | Material list | Copy | Paste | Material lists | Delete"
xdo 1 keydown Alt_L
check "the radial menu shows both sliders at their first settings" eval 'drv 1 "await screen Build Modes" > /dev/null &&
	[[ $(val 1 texts) == "Build speed: 5 blocks/s | Break speed: 3.33 blocks/s | $buttons" ]]'
read -r x y < <(val 1 "sliderat 2 Build speed")
xdo 1 mousemove --window %w "$x" "$y" click 1
read -r x y < <(val 1 "sliderat 6 Break speed")
read -r x2 y2 < <(val 1 "sliderat 10 Break speed")
xdo 1 mousemove --window %w "$x" "$y" mousedown 1 mousemove --window %w $(((x + x2) / 2)) "$y" sleep 0.2 \
	mousemove --window %w "$x2" "$y2" sleep 0.2 mouseup 1
check "the click and the drag set them to 10 and 2 blocks a second" \
	eval '[[ $(val 1 texts) == "Build speed: 10 blocks/s | Break speed: 2 blocks/s | $buttons" ]]'
xdo 1 mousemove --window %w 600 300
drv 1 "wait 10" "shot radial-moved" > /dev/null
xdo 1 keyup Alt_L
check "the menu closes when its key is let go" drv 1 "await noscreen"
check "hold to repeat takes both at once" eval '[[ $(val 1 repeat) == "place=2 erase=10" ]]'
check "and they're kept in Building Planner's client config" eval \
	'grep -q "^place-repeat-ticks=2" run/client1/'$marker' && grep -q "^erase-repeat-ticks=10" run/client1/'$marker

# hold_at <button> <name> <period>: holds the button over the row z 5 from 2 -60 5, as above, and checks the edits
# came every <period> ticks. For erasing, the row is filled first, four blocks high: a break that meets no ghost block
# sends no edit, and at a repeat every tick the player, one block on every 2 ticks, breaks some places twice or more.
hold_at() {
	if [[ $1 == 1 ]]; then
		drv 1 "edit 2 -60 5 40 -57 5 minecraft:stone_bricks" "await block 40 -57 5 minecraft:stone_bricks" > /dev/null
	else
		drv 1 "edit 2 -60 5 40 -57 5 minecraft:air" "await block 40 -60 5 minecraft:air" > /dev/null
	fi
	drv 1 "cmd tp @s 2.5 -55 5.5 0 90" "wait 30" "holdreset" > /dev/null
	hold_while_walking "$1"
	drv 1 "wait 20" > /dev/null
	local log
	log=$(val 1 holdlog)
	echo "  hold $2: $log" >> /out/checks.log
	hold_ok "$log" "$2" "$3"
}
check "holding use now sends an edit 4 ticks after it goes down, then every 2 ticks" hold_at 3 use 2
check "and holding attack one every 10" hold_at 1 attack 10

# Both settings survive a restart: the sliders show them, and hold to repeat uses them.
stop_client 1
start_client 1
check "client 1 starts a session again" drv 1 "await session" "await noscreen"
check "after a restart, hold to repeat keeps both settings" eval '[[ $(val 1 repeat) == "place=2 erase=10" ]]'
drv 1 "cmd tp @s 20 -58 -5 0 30" "wait 20" > /dev/null
xdo 1 keydown Alt_L
check "and so do the sliders" eval 'drv 1 "await screen Build Modes" > /dev/null &&
	[[ $(val 1 texts) == "Build speed: 10 blocks/s | Break speed: 2 blocks/s | $buttons" ]]'
xdo 1 keyup Alt_L
drv 1 "await noscreen" "ebmode SINGLE" > /dev/null
check "holding use after the restart sends an edit 4 ticks after it goes down, then every 2 ticks" hold_at 3 use 2

# At the fastest speed, 20 blocks a second (a repeat every tick), a quick click still acts once, and a held one first
# repeats after the game's own wait of 4 ticks. Both sliders are dragged to their right end.
drv 1 "cmd tp @s 20 -58 -5 0 30" "wait 20" > /dev/null
xdo 1 keydown Alt_L
drv 1 "await screen Build Modes" > /dev/null
for slider in "Build speed" "Break speed"; do
	read -r x y < <(val 1 "sliderat 6 $slider")
	read -r x2 y2 < <(val 1 "sliderat 1 $slider")
	xdo 1 mousemove --window %w "$x" "$y" mousedown 1 mousemove --window %w $(((x + x2) / 2)) "$y" sleep 0.2 \
		mousemove --window %w $((x2 + 4)) "$y2" sleep 0.2 mouseup 1
done
check "both sliders now read 20 blocks a second" \
	eval '[[ $(val 1 texts) == "Build speed: 20 blocks/s | Break speed: 20 blocks/s | $buttons" ]]'
xdo 1 keyup Alt_L
drv 1 "await noscreen" "ebmode SINGLE" > /dev/null
check "hold to repeat takes a repeat every tick" eval '[[ $(val 1 repeat) == "place=1 erase=1" ]]'
# quick_click <use|attack>: one quick click on the row z 5 at 2 -60 5, with the row filled first for attack; the hold
# log then shows one edit.
quick_click() {
	if [[ $1 == attack ]]; then
		drv 1 "edit 2 -60 5 40 -60 5 minecraft:stone_bricks" "await block 40 -60 5 minecraft:stone_bricks" > /dev/null
	else
		drv 1 "edit 2 -60 5 40 -57 5 minecraft:air" "await block 40 -60 5 minecraft:air" > /dev/null
	fi
	drv 1 "cmd tp @s 2.5 -55 5.5 0 90" "wait 30" "holdreset" > /dev/null
	"${1}_click" 1
	drv 1 "wait 20" > /dev/null
	local log
	log=$(val 1 holdlog)
	echo "  quick $1 click: $log" >> /out/checks.log
	[[ $log == *" edits=1 "* ]]
}
check "at 20 blocks a second, a quick use click places one block" quick_click use
check "and a quick attack click erases one" quick_click attack
check "holding use sends an edit 4 ticks after it goes down, then every tick" hold_at 3 use 1
check "and holding attack likewise" hold_at 1 attack 1

# The screen the radial menu's wrench button opens, for Geo's review: its pointer goes to the wrench (157 left of the
# menu's middle and 13 above it, at GUI scale 2), and a click opens Effortless Building's Modifiers screen.
drv 1 "cmd tp @s 20 -58 -5 0 30" "wait 20" > /dev/null
xdo 1 keydown Alt_L
drv 1 "await screen Build Modes" "wait 20" > /dev/null
xdo 1 mousemove --window %w $((854 / 2 - 157 * 2)) $((480 / 2 - 13 * 2))
drv 1 "wait 10" > /dev/null
xdo 1 click 1
check "the wrench button opens the Modifiers screen" drv 1 "await screen Modifiers"
xdo 1 keyup Alt_L
# The chat, full of the checks' teleports, fades over 10 seconds first.
drv 1 "wait 220" "notoasts" "wait 5" "shot modifiers" "close" "await noscreen" > /dev/null
drv 1 "ebmode DISABLED" > /dev/null

# Editing from Freecam: with Freecam's camera on, 10 blocks north of Planner1 and with its "Allow interaction" off, as
# by default, shapes, boxes and the copy aim from the camera. Planner1 stands at 70.5 -60 40.5 facing east and looking
# up at the sky, so their own aim meets no block; the camera's eyes are at 70.5 -56.5 30.5, and it looks north and down
# at the grass, whose top is at y -60 here.
# cam <x y z>: the camera looks at that point, as a mouse move would. cam_shot <name>: shoots the camera's view at
# 854 by 480 and at 1920 by 1080, both at GUI scale 2, as <name>-854x480 and <name>-1920x1080, once the chat has faded.
# Each point the camera aims at is chosen so its line of sight passes no ghost block on the way.
cam() {
	drv 1 "camlookat $1 $2 $3" "wait 5" > /dev/null
}
# state_has <client> <x y z> <text>: the state Litematica shows there contains the text.
state_has() {
	[[ $(val "$1" "state $2") == *"$3"* ]]
}
cam_shot() {
	drv 1 "wait 210" "notoasts" "wait 5" "shot $1-854x480" "window 1920 1080 2" "wait 20" "notoasts" \
		"shot $1-1920x1080" "window 854 480 2" "wait 20" > /dev/null
}
drv 1 "cmd tp @s 70.5 -60 40.5 -90 -60" "cmd tp Planner2 60.5 -60 45.5 180 10" "cmd clear @s" \
	"cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "ebmode SINGLE" "ebarray 0" "wait 40" \
	> /dev/null
check "Freecam's camera comes on" eval '[[ $(val 1 "freecam true") == true ]] && drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 "camtp 70.5 -56.5 30.5 180 30") == "FreeCamera 70.5,-56.5,30.5 north" ]]'
check "with its \"Allow interaction\" off, as by default" eval '[[ $(val 1 allowinteraction) == false ]]'

# A Single shape goes where the camera aims, on client 1 and client 2, as a ghost block alone.
cam 70.5 -60 25.5
drv 1 "holdreset" > /dev/null
use_click 1
check "from Freecam's camera, a Single shape goes where the camera aims" drv 1 \
	"await block 70 -60 25 minecraft:stone_bricks"
check "for the other player too" drv 2 "await block 70 -60 25 minecraft:stone_bricks"
check "placing no real block" eval '[[ $(val 1 "real 70 -60 25") == minecraft:air ]]'

# A Line shape: the first click at 66 -60 24, the second at 74 -60 24, its line of sight from the camera. The preview
# is shot between the clicks.
drv 1 "ebmode LINE" > /dev/null
cam 66.5 -60 24.5
use_click 1
cam 74.5 -60 24.5
cam_shot freecam-preview
use_click 1
check "a Line shape goes where the camera aims, from its first click to its second" eval \
	'drv 1 "await block 74 -60 24 minecraft:stone_bricks" > /dev/null &&
	 [[ $(val 1 "countblocks 60 80 -60 24 minecraft:stone_bricks") == 9 ]] &&
	 [[ $(val 1 "get 66 -60 24") == minecraft:stone_bricks ]]'

# A stair drawn with the camera facing north faces north, though Planner1 faces east.
drv 1 "ebmode SINGLE" "cmd item replace entity @s weapon.mainhand with minecraft:oak_stairs" "wait 5" > /dev/null
cam 70.5 -60 27.5
use_click 1
check "a stair drawn with the camera facing north faces north" eval \
	'drv 1 "await block 70 -60 27 minecraft:oak_stairs" > /dev/null && state_has 1 "70 -60 27" "facing=north"'

# The break click erases the ghost block the camera aims at, leaving the real grass under it.
drv 1 "cmd item replace entity @s weapon.mainhand with minecraft:stone_bricks" "wait 5" > /dev/null
cam 70.5 -59 25.5
attack_click 1
check "the break click erases the ghost block where the camera aims" drv 1 "await block 70 -60 25 minecraft:air"
check "leaving the real block under it" eval '[[ $(val 1 "real 70 -61 25") =~ ^minecraft:(grass_block|dirt)$ ]]'

# A box for Copy around the line, drawn with the camera's aim and three left clicks, which Freecam would otherwise
# cancel, and the copy at the camera's aim. copy_button: a click on the radial menu's Copy button.
copy_button() {
	xdo 1 keydown Alt_L
	drv 1 "await screen Build Modes" "wait 10" > /dev/null
	radial_button "Copy"
	xdo 1 click 1
	drv 1 "wait 5" > /dev/null
	xdo 1 keyup Alt_L
	drv 1 "await noscreen" "wait 5" > /dev/null
}
# radial_button <name>: puts the pointer on the open radial menu's button of that name.
radial_button() {
	local x y
	read -r x y < <(val 1 "widget 0 $1")
	xdo 1 mousemove --window %w "$x" "$y"
	drv 1 "wait 5" > /dev/null
}
copy_button
cam 64.5 -60 23.5
check "with Freecam's camera on, a box for Copy starts at the camera's aim" eval \
	'[[ $(val 1 drawbox) == "COPY 0 64,-60,23..64,-60,23 white SINGLE" ]]'
attack_click 1
cam 68.5 -60 24.5
check "the first left click sets its first corner, and the box follows the camera's aim" eval \
	'[[ $(val 1 drawbox) == "COPY 1 64,-60,23..68,-60,24 white SINGLE" ]]'
cam 75.5 -60 24.5
cam_shot freecam-box
attack_click 1
cam 75.5 -59.5 24.5
check "the second sets the opposite corner, and the height follows the camera's aim" eval \
	'[[ $(val 1 drawbox) == "COPY 2 64,-60,23..75,-60,24 white SINGLE" ]]'
attack_click 1
check "the third copies the line's 9 ghost blocks" client_said 1 "Copied 9 blocks."
# The copy, 9 blocks along x, rests on the grass the camera aims at with its middle on the block above.
cam 60.5 -60 22.5
check "the copy goes where the camera aims" drv 1 "await copy at 56,-60,22..64,-60,22 NONE NONE unselected 9 blocks"
cam 63.5 -60 21.5
check "and follows the camera's aim" drv 1 "await copy at 59,-60,21..67,-60,21 NONE NONE"
cam_shot freecam-copy
# The camera faces north and Planner1 east: a middle click mirrors it left to right for the camera's facing.
xdo 1 click 2
check "a middle click mirrors the copy by the camera's facing" drv 1 "await copy at 59,-60,21..67,-60,21 FRONT_BACK NONE"
use_click 1
check "a right click puts the copy away" eval 'drv 1 "wait 10" > /dev/null && [[ $(val 1 copy) == "away "* ]]'

# The radial menu and N work as without Freecam: letting the menu's key go over its show or hide button hides ghost
# blocks, and N shows them again.
xdo 1 keydown Alt_L
check "the radial menu opens with Freecam's camera on" drv 1 "await screen Build Modes"
drv 1 "wait 10" > /dev/null
radial_button "Show or hide ghost blocks"
xdo 1 keyup Alt_L
check "and its show or hide button hides ghost blocks" eval 'drv 1 "await noscreen" "wait 5" > /dev/null &&
	[[ $(val 1 ghosts) == "hidden, "* ]]'
drv 1 "close" "wait 5" > /dev/null
before=$(val 1 presses)
keys 1 key n
check "N shows them again" eval 'drv 1 "await pressedafter '"$before"'" "wait 2" > /dev/null &&
	[[ $(val 1 ghosts) == "shown, "* ]]'

# The block mix: a Line shape of 12 blocks along z 20, from the camera, takes its blocks from the mix. Ctrl+Z undoes it,
# and Ctrl+Y redoes it.
mixed() {
	echo $(($(val 1 "countblocks 60 80 -60 20 minecraft:stone_bricks") +
		$(val 1 "countblocks 60 80 -60 20 minecraft:mossy_stone_bricks")))
}
drv 1 "setmix minecraft:stone_bricks:50,minecraft:mossy_stone_bricks:50" "ebmode LINE" > /dev/null
cam 66.5 -60 20.5
use_click 1
cam 77.5 -60 20.5
use_click 1
check "with the block mix on, a Line shape from the camera takes the mix's blocks" eval \
	'drv 1 "await block 77 -60 20 minecraft:stone_bricks|minecraft:mossy_stone_bricks" > /dev/null &&
	 [[ $(mixed) == 12 ]] && (($(val 1 "countblocks 60 80 -60 20 minecraft:mossy_stone_bricks") > 0)) &&
	 (($(val 1 "countblocks 60 80 -60 20 minecraft:stone_bricks") > 0))'
keys 1 keydown ctrl key z sleep 0.3 keyup ctrl
check "Ctrl+Z undoes it" eval 'drv 1 "await block 77 -60 20 minecraft:air" > /dev/null && [[ $(mixed) == 0 ]]'
keys 1 keydown ctrl key y sleep 0.3 keyup ctrl
check "Ctrl+Y redoes it" eval \
	'drv 1 "await block 77 -60 20 minecraft:stone_bricks|minecraft:mossy_stone_bricks" > /dev/null && [[ $(mixed) == 12 ]]'
drv 1 "setmix" "ebmode SINGLE" > /dev/null

# A left click with a stick, which places no block, aimed by the camera at a real stone, stays Freecam's: with
# "Allow interaction" off, it breaks nothing.
drv 1 "cmd setblock 72 -60 28 minecraft:stone" "cmd item replace entity @s weapon.mainhand with minecraft:stick" \
	"wait 10" > /dev/null
cam 72.5 -59 28.5
attack_click 1
check "a left click with a stick from Freecam's camera breaks no real block" eval \
	'drv 1 "wait 20" > /dev/null && [[ $(val 1 "real 72 -60 28") == minecraft:stone ]]'
check "Freecam's camera goes off" eval '[[ $(val 1 "freecam false") == false ]] && drv 1 "wait 10" > /dev/null &&
	[[ $(val 1 camera) == LocalPlayer* ]]'
drv 1 "cmd clear @s" "ebmode DISABLED" > /dev/null

# The look, after the player's own change: translucent off and alpha 0.3, and the drawing of area selections turned
# back on, saved; a restart keeps all three.
drv 2 "setlook false 0.3" "setareaoption true" > /dev/null
stop_client 2
start_client 2
check "client 2 starts a session again" drv 2 "await session" "await noscreen"
check "a restart keeps the player's own look, not Building Planner's" \
	eval '[[ $(val 2 look) == "translucent=false alpha=0.3" ]]'
check "and the drawing of area selections the player turned back on" eval '[[ $(val 2 areaoption) == true ]]'

# With the record gone but the alpha still the player's own, only the option left at Litematica's default is set, and
# on a client that never joins a server with Building Planner: the server is stopped first.
stop_client 2
stop_server
rm "run/client2/$marker"
start_client 2
check "a client that joins no server sets the option left at its default, and keeps the player's alpha" \
	eval 'drv 2 "wait 60" > /dev/null && [[ $(val 2 look) == "translucent=true alpha=0.3" ]]'
check "and turns the drawing of area selections, which was at its default, off" eval '[[ $(val 2 areaoption) == false ]]'
check "and records each group" eval \
	'grep -q "^look-defaults-applied=true" run/client2/'$marker' && grep -q "^area-selection-off-applied=true" run/client2/'$marker
check "and saves it" eval \
	'tr -d " \n\r" < run/client2/config/litematica.json | grep -q "\"ghostBlockAlpha\":0.3"'

# Each group has its record of its own. With only the look group's record left, as 0.3.0 and 0.4.0 leave it, the look
# stays as the player has it (Litematica's own defaults, which the look group would change) and only the drawing of area
# selections, turned back on, is set.
drv 2 "setlook false 0.5" "setareaoption true" > /dev/null
stop_client 2
sed -i '/^area-selection-off-applied=/d' "run/client2/$marker"
start_client 2
check "with only the look group's record left, the look is left as it is" \
	eval 'drv 2 "wait 60" > /dev/null && [[ $(val 2 look) == "translucent=false alpha=0.5" ]]'
check "the drawing of area selections is turned off" eval '[[ $(val 2 areaoption) == false ]]'
check "and its record is written beside the look group's" eval \
	'grep -q "^look-defaults-applied=true" run/client2/'$marker' && grep -q "^area-selection-off-applied=true" run/client2/'$marker

# With only the area selection group's record left, the drawing the player turned back on stays on and the look is set.
drv 2 "setareaoption true" > /dev/null
stop_client 2
sed -i '/^look-defaults-applied=/d' "run/client2/$marker"
start_client 2
check "with only the area selection group's record left, the look is set" \
	eval 'drv 2 "wait 60" > /dev/null && [[ $(val 2 look) == "translucent=true alpha=0.8" ]]'
check "and the drawing of area selections the player turned on stays on" eval '[[ $(val 2 areaoption) == true ]]'
check "and the look group's record is written" eval 'grep -q "^look-defaults-applied=true" run/client2/'$marker

# With the records of the other groups left and the menu key group's gone, a Freecam key the player bound to K stays,
# and Litematica's hotkey, which is "M" again, still goes.
drv 2 "setlook false 0.5" "setmenukeys K" > /dev/null
check "the player's own Freecam key, with Litematica's menu hotkey M, as set" eval \
	'[[ $(val 2 menukeys) == "freecam=key.keyboard.k litematica=M" ]]'
stop_client 2
sed -i '/^menu-key-applied=/d' "run/client2/$marker"
start_client 2
check "with only the menu key group's record gone, the player's Freecam key stays at K, and Litematica's hotkey goes" \
	eval 'drv 2 "wait 60" > /dev/null && [[ $(val 2 menukeys) == "freecam=key.keyboard.k litematica=none" ]]'
check "and the group's record is written" eval 'grep -q "^menu-key-applied=true" run/client2/'$marker
check "and the other groups left the player's look alone" \
	eval '[[ $(val 2 look) == "translucent=false alpha=0.5" ]] && [[ $(val 2 areaoption) == true ]]'

# With all records left, nothing is set again: Freecam's key goes back to M and Litematica's hotkey to M, as a player
# might set them, and a restart leaves both.
drv 2 "setmenukeys M" > /dev/null
stop_client 2
start_client 2
check "with every record left, the player's own keys stay" \
	eval 'drv 2 "wait 60" > /dev/null && [[ $(val 2 menukeys) == "freecam=key.keyboard.m litematica=M" ]]'

# With only the layer keys group's record gone, a hotkey the player moved to K stays, and the other, which is Page Down
# again, goes.
drv 2 "setlayerkeys K PAGE_DOWN" > /dev/null
check "the player's own layer hotkeys, next on K and previous on Page Down, as set" eval \
	'[[ $(val 2 layerkeys) == "next=K previous=PAGE_DOWN" ]]'
stop_client 2
sed -i '/^layer-keys-applied=/d' "run/client2/$marker"
start_client 2
check "with only the layer keys group's record gone, the hotkey the player moved stays at K, and Page Down goes" \
	eval 'drv 2 "wait 60" > /dev/null && [[ $(val 2 layerkeys) == "next=K previous=none" ]]'
check "and the group's record is written" eval 'grep -q "^layer-keys-applied=true" run/client2/'$marker
check "and saves the moved hotkey to Litematica's config" eval 'layer_json 2 layerNext | grep -q K'
check "and the menu keys group left the player's own keys alone" \
	eval '[[ $(val 2 menukeys) == "freecam=key.keyboard.m litematica=M" ]]'

# With all records left, nothing is set again: both hotkeys back on Page Up and Page Down, as a player might set them,
# and a restart leaves both.
drv 2 "setlayerkeys PAGE_UP PAGE_DOWN" > /dev/null
stop_client 2
start_client 2
check "with every record left, the player's own layer hotkeys stay" \
	eval 'drv 2 "wait 60" > /dev/null && [[ $(val 2 layerkeys) == "next=PAGE_UP previous=PAGE_DOWN" ]]'
