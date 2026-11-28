# Saved copies (0.11.0): each copy saved on Planner1's computer as Copy makes it, in its own file with the index beside
# it; the copy's controls line naming "Saved copies" (V) as bound; the "Saved copies" screen V opens, its "Paste",
# "Rename" and "Delete" clicked as Planner1 clicks them, with "Rename copy" and the delete question; a rename kept
# outside the 10 most recent, and the 11th copy dropping the oldest with its file; a pick bringing a copy to the aim as
# the radial menu's Paste button brings one; a delete; a copy's file and the index made unreadable; the copies after
# Planner1's game restarts, and on the server restarted without the add-on. The driver copies boxes as Copy's third
# click does, and reads the index and the folder from disk. Sourced by inside.sh, which provides drv, val, check, keys,
# xdo, click, use_click, attack_click, client_said, start_server, stop_server, start_client, stop_client and $ADDON.
# Players stand on the flat world's grass, whose top is at y -61.

planner1=527e7f9d-ae90-3a44-aa88-30a47a9cdf34
folder1=run/client1/building-planner/copies/$planner1
# A copy's name as the game gives it, its time, as "Nov 19, 6:21 PM".
time_name='[A-Z][a-z]{2} [0-9]{1,2}, [0-9]{1,2}:[0-9]{2} [AP]M'
screen=dev.buildingplanner.client.SavedCopiesScreen

check "client 1 starts a session on hello" drv 1 "await session" "await noscreen"
check "client 2 starts a session on hello" drv 2 "await session" "await noscreen"

# press <client> <key>: a real key press with no screen open, returning once the game has handled it.
press() {
	local before
	drv "$1" "close" "wait 5" > /dev/null
	before=$(val "$1" presses)
	keys "$1" key "$2"
	drv "$1" "await pressedafter $before" "wait 2" > /dev/null
}

# chat_count <client> <text>: how many lines of that client's chat have been exactly the text.
chat_count() {
	tr -d '\r' < "/out/client$1.log" |
		awk -v t="[CHAT] $2" 'length($0) >= length(t) && substr($0, length($0) - length(t) + 1) == t' | wc -l
}

# said_once_more <client> <text> <count>: that client's chat shows the text, within a minute, once more than <count>
# times.
said_once_more() {
	local i
	for i in $(seq 60); do
		(($(chat_count "$1" "$2") == $3 + 1)) && return 0
		drv "$1" "wait 20" > /dev/null
	done
	return 1
}

half() {
	awk -v n="$1" 'BEGIN { print n + 0.5 }'
}

# aim <client> <x y z>: the player looks at the middle of the top of the block below x y z, so the copy rests on that
# top with the middle of its bottom at x y z.
aim() {
	drv "$1" "lookat $(half "$2") $3 $(half "$4")" "wait 5" > /dev/null
}

# box <client> <ax ay az> <y1> <x2 z2> <y3>: draws a box for Copy with three left clicks: the first aimed at the point
# ax ay az on a block's side, which becomes the first corner, on level y1; the second at the middle of the block x2 y1
# z2; the third at the middle of the block at height y3 above x2 z2.
box() {
	drv "$1" "lookat $2 $3 $4" "wait 5" > /dev/null
	attack_click "$1"
	drv "$1" "lookat $(half "$6") $5 $(half "$7")" "wait 5" > /dev/null
	attack_click "$1"
	drv "$1" "lookat $(half "$6") $(half "$8") $(half "$7")" "wait 10" > /dev/null
	attack_click "$1"
	drv "$1" "wait 5" > /dev/null
}

# The radial menu, held open with left Alt, and a real click on its button of a name.
radial_open() {
	xdo 1 keydown Alt_L
	drv 1 "await screen Build Modes" "wait 10" > /dev/null
}
radial_click() {
	local x y
	read -r x y < <(val 1 "widget 0 $1")
	xdo 1 mousemove --window %w "$x" "$y"
	drv 1 "wait 5" > /dev/null
	xdo 1 click 1
	drv 1 "wait 5" > /dev/null
}
radial_let_go() {
	xdo 1 keyup Alt_L
	drv 1 "await noscreen" "wait 5" > /dev/null
}
# copy_button and paste_button: a click on the radial menu's Copy or Paste button.
copy_button() {
	radial_open
	radial_click "Copy"
	radial_let_go
}
paste_button() {
	radial_open
	radial_click "Paste"
	radial_let_go
}

# copy_is <client> <pattern>: the copy, as the driver's copy describes it, matches the extended regular expression.
copy_is() {
	[[ $(val "$1" copy) =~ $2 ]]
}

# settled: Planner1's saved copies have done every change; its list is then as on disk.
settled() {
	drv 1 "await copiessettled" > /dev/null
}

# rows: Planner1's saved copies as the game keeps them; index_rows: the index on disk, without the times.
rows() {
	val 1 savedcopies
}
index_rows() {
	val 1 copyindex | sed -E 's/@[0-9]+//g'
}
# no_row <file>: Planner1's saved copies have no row for that file.
no_row() {
	! grep -qE "(^| ; )$1:" <<< "$(rows)"
}
# row_of <file>: the row (from 0, newest first) of the saved copy with that file.
row_of() {
	rows | awk -v f="$1:" -F ' ; ' '{ for (i = 1; i <= NF; i++) if (index($i, f) == 1) { print i - 1; exit } }'
}

# copy_min: the lowest corner of Planner1's copy at the aim, as x y z.
copy_min() {
	val 1 copy | sed -E 's/^at (-?[0-9]+),(-?[0-9]+),(-?[0-9]+)\.\..*$/\1 \2 \3/'
}

# saved_open: V, pressed by Planner1, opens "Saved copies".
saved_open() {
	press 1 v
	drv 1 "await screenclass $screen" "wait 5" > /dev/null
}
# copy_rows: the open "Saved copies" screen's rows as "name / size · blocks", joined by " ; "; shown: the first row in
# view and how many are.
copy_rows() {
	val 1 copyrows | sed -E 's/ \| shown [0-9]+ [0-9]+$//'
}
shown() {
	val 1 copyrows | sed -E 's/^.*\| shown ([0-9]+ [0-9]+)$/\1/'
}
# widget_click <n> <text>: a real click on the nth shown widget (from 0) of the open screen whose text starts with the
# text.
widget_click() {
	local pos
	pos=$(val 1 "widget $1 $2")
	[[ $pos != none ]] && click 1 $pos && drv 1 "wait 5" > /dev/null
}
# wheel <button> <times>: the mouse wheel turned on the open screen, up (4) or down (5), the pointer in the window's
# corner, clear of every widget.
wheel() {
	xdo 1 mousemove --window %w 5 5 click --repeat "$2" --delay 30 "$1" > /dev/null
	drv 1 "wait 5" > /dev/null
}
scroll_top() {
	wheel 4 12
}
scroll_end() {
	wheel 5 12
}
# row_button <label> <row>: a real click on that button of the row (from 0, newest first) of the open "Saved copies",
# the mouse wheel first scrolling the row into view.
row_button() {
	local first count i
	for i in $(seq 20); do
		read -r first count <<< "$(shown)"
		if (($2 < first)); then
			wheel 4 1
		elif (($2 >= first + count)); then
			wheel 5 1
		else
			widget_click $(($2 - first)) "$1"
			return
		fi
	done
	return 1
}
# type_name <text>: empties the focused text field and types the text with real key presses.
type_name() {
	keys 1 key ctrl+a BackSpace
	[[ -z $1 ]] || keys 1 type --delay 20 -- "$1"
	drv 1 "wait 5" > /dev/null
}

sizes=("854 480 2" "1920 1080 2" "1920 1080 4")

# chat_shots <name> <text> <command>: Planner1's screen at 854 by 480 and 1920 by 1080 at GUI scale 2, and 1920 by
# 1080 at GUI scale 4, before and after the command (a function of this script) shows the text in chat, as
# <name>-<size>-gui<scale>, each .boxes with a "change" line outlining the message where the chat differs; the window
# is 854 by 480 again after.
chat_shots() {
	local size w h s shot box shots=()
	drv 1 "wait 210" > /dev/null
	for size in "${sizes[@]}"; do
		read -r w h s <<< "$size"
		shot=$1-${w}x$h-gui$s
		shots+=("$shot")
		drv 1 "window $w $h $s" "wait 20" "notoasts" "wait 5" "shot before-$shot" "wait 20" > /dev/null || return 1
	done
	"$3" || return 1
	for size in "${sizes[@]}"; do
		read -r w h s <<< "$size"
		shot=$1-${w}x$h-gui$s
		drv 1 "window $w $h $s" "wait 20" "notoasts" "wait 5" "shot $shot" "wait 20" > /dev/null || return 1
	done
	drv 1 "window 854 480 2" "wait 20" > /dev/null
	for shot in "${shots[@]}"; do
		box=$(val 1 "chatchange before-$shot $shot")
		[[ $box =~ ^[0-9]+\ [0-9]+\ [0-9]+\ [0-9]+$ ]] || return 1
		echo "change $box $2" >> "run/client1/screenshots/$shot.boxes"
		echo "  $shot: change $box $2" >> /out/checks.log
		rm "run/client1/screenshots/before-$shot.png" "run/client1/screenshots/before-$shot.boxes"
	done
}

# screen_box <.boxes file> <text pattern>: the window box (left top right bottom) around the open screen's widgets and
# the texts the shot drew that match the pattern (an awk regular expression), which leaves out the HUD's.
screen_box() {
	awk -v re="$2" '
		BEGIN { L = T = 1e9; R = B = -1 }
		function add(l, t, r, b) { if (l < L) L = l; if (t < T) T = t; if (r > R) R = r; if (b > B) B = b }
		/^widgets: / {
			line = $0
			sub(/^widgets: /, "", line)
			k = split(line, parts, / \| /)
			for (i = 1; i <= k; i++) {
				m = split(parts[i], f, " ")
				if (m >= 4) add(f[m - 3] + 0, f[m - 2] + 0, f[m - 3] + f[m - 1], f[m - 2] + f[m])
			}
		}
		/^(text|component) / {
			t = $0
			sub(/^[a-z]+ -?[0-9]+ -?[0-9]+ -?[0-9]+ -?[0-9]+ /, "", t)
			if (t ~ re) add($2 + 0, $3 + 0, $4 + 0, $5 + 0)
		}
		END { if (R >= 0) print L, T, R, B }' "$1"
}
# line_box <.boxes file>: the window box of the controls line's "[V: saved copies]".
line_box() {
	sed -n 's/^moderects: //p' "$1" | sed 's# / # | #g' | tr '|' '\n' |
		sed -nE 's/^ *([0-9]+ [0-9]+ [0-9]+ [0-9]+) \[V: saved copies\] *$/\1/p'
}
# shots <name> <label> <box command> [<prepare>]: Planner1's screen at each of the three sizes as
# <name>-<size>-gui<scale>, after the function <prepare>, if any, once the window has its size, with the pointer in
# the window's corner; each .boxes with a "change" line outlining the label's change, the box command's box for that
# .boxes file. The window is 854 by 480 again after.
shots() {
	local size w h s shot file box
	for size in "${sizes[@]}"; do
		read -r w h s <<< "$size"
		shot=$1-${w}x$h-gui$s
		file=run/client1/screenshots/$shot.boxes
		drv 1 "window $w $h $s" "wait 20" > /dev/null || return 1
		[[ -z ${4:-} ]] || "$4" || return 1
		xdo 1 mousemove --window %w 5 5 > /dev/null
		drv 1 "wait 5" "notoasts" "wait 5" "shot $shot" "wait 20" > /dev/null || return 1
		box=$($3 "$file")
		[[ $box =~ ^[0-9]+\ [0-9]+\ [0-9]+\ [0-9]+$ ]] || return 1
		echo "change $box $2" >> "$file"
		echo "  $shot: change $box $2 | rows $(val 1 copyrows)" >> /out/checks.log
	done
	drv 1 "window 854 480 2" "wait 20" > /dev/null
}
copies_texts='^(Saved copies|Your 10 most recent copies stay here[.] Rename a copy to keep it[.]|No saved copies yet[.]|Hut|[A-Z][a-z][a-z] [0-9]+, [0-9]+:[0-9][0-9] [AP]M|[0-9]+ × [0-9]+ × [0-9]+ · [0-9]+ blocks?)$'
copies_box() {
	screen_box "$1" "$copies_texts"
}
rename_box() {
	screen_box "$1" '^(Rename copy|Name)$'
}
question_box() {
	screen_box "$1" '^Delete the copy "Hut"[?]$'
}

# A hut's corner of ghost blocks in the shared layer: a floor of oak planks, a door facing south on it, and a wall
# torch facing west, 15 blocks in a box of 4 by 3 by 3. North of it, a row of 11 ghost blocks, each of another block.
blocks=(stone granite diorite andesite cobblestone bricks glass gold_block iron_block diamond_block emerald_block)
drv 1 "window 854 480 2" "ebmode SINGLE" "cmd tp @s 41.5 -56 36.5 0 30" "cmd tp Planner2 44.5 -56 36.5 0 30" \
	"wait 40" "fly true" > /dev/null
drv 2 "wait 5" "fly true" > /dev/null
drv 1 "edit 40 -60 40 43 -60 42 minecraft:oak_planks" "edit 41 -59 42 41 -59 42 minecraft:oak_door[facing=south,half=lower]" \
	"edit 41 -58 42 41 -58 42 minecraft:oak_door[facing=south,half=upper]" \
	"edit 43 -58 41 43 -58 41 minecraft:wall_torch[facing=west]" > /dev/null
for i in "${!blocks[@]}"; do
	drv 1 "edit $((40 + i)) -60 50 $((40 + i)) -60 50 minecraft:${blocks[i]}" > /dev/null
done
check "both clients show the hut and the row" eval 'drv 1 "await block 43 -58 41 minecraft:wall_torch" \
	"await block 50 -60 50 minecraft:emerald_block" > /dev/null && drv 2 "await block 43 -58 41 minecraft:wall_torch" > /dev/null'
check "no copy is saved yet" eval '[[ $(rows) == none ]] && [[ $(val 1 copyfiles) == nofolder ]]'

# The hut copied with the radial menu's Copy button and a box of three real left clicks.
copy_button
box 1 40.5 -59.5 40.1 -60 43 42 -58
check "Copy copies the hut's 15 blocks" eval 'client_said 1 "Copied 15 blocks." && copy_is 1 "^at .* 15 blocks$"'
settled
check "and saves it at once: 1.litematic and index.dat in building-planner/copies/<Planner1's UUID>" eval \
	'[[ $(val 1 copyfolder) == "building-planner/copies/$planner1" ]] && [[ $(val 1 copyfiles) == "1.litematic index.dat" ]] &&
	[[ -s $folder1/1.litematic && -s $folder1/index.dat ]]'
index=$(val 1 copyindex)
time=$(sed -E 's/^.*@([0-9]+)$/\1/' <<< "$index")
name=$(date -d "@$((time / 1000))" '+%b %-d, %-I:%M %p')
check "the index names it by its time, not kept, with the hut's size and block count" eval \
	'[[ $index == "version 1 next 2: 1:$name:recent:4x3x3:15@$time" ]] && (($(date +%s) - time / 1000 < 600))'
check "and the game lists it so" eval '[[ $(rows) == "1:$name:recent:4x3x3:15" ]]'
check "Litematica's own reader reads its file: the region Copy, 4 by 3 by 3, with 15 blocks" eval \
	'[[ $(val 1 "copyschematic 1") == "$name: Copy 4x3x3, 15 blocks" ]]'
check "Planner2's game has no saved copy" eval '[[ $(val 2 savedcopies) == none ]] && [[ $(val 2 copyfiles) == nofolder ]]'

# The copy at the aim's controls line names "Saved copies" before the close, as the key is bound; unbound, the control
# is gone and V opens nothing; rebound to K, the line names K, which opens the screen.
check "the copy's controls line ends with [V: saved copies][Esc/Left-click: close]" eval '[[ $(val 1 modehud) == \
	"[Right-click: paste][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Off)][V: saved copies][Esc/Left-click: close]" ]]'
check "shots of the controls line with the key" shots copy-line-saved-copies "[V: saved copies]" line_box
drv 1 "bindkey key.buildingplanner.saved_copies key.keyboard.unknown" "wait 2" > /dev/null
check "with the key unbound, the line leaves the control out" eval '[[ $(val 1 modehud) == \
	"[Right-click: paste][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Off)][Esc/Left-click: close]" ]]'
press 1 v
check "and V opens nothing" eval 'drv 1 "wait 10" > /dev/null && [[ $(val 1 screenclass) == none ]]'
drv 1 "bindkey key.buildingplanner.saved_copies key.keyboard.k" "wait 2" > /dev/null
check "rebound to K, the line names K" eval '[[ $(val 1 modehud) == *"[R: Mode (Off)][K: saved copies][Esc/Left-click: close]" ]]'
press 1 k
check "and K opens Saved copies" drv 1 "await screenclass $screen"
drv 1 "close" "wait 5" "bindkey key.buildingplanner.saved_copies key.keyboard.v" "wait 2" > /dev/null
check "bound to V again, the line names V" eval '[[ $(val 1 modehud) == *"[V: saved copies][Esc/Left-click: close]" ]]'

# V opens "Saved copies" with the hut's row; Escape goes back to the game with the copy still at the aim. V does
# nothing with the inventory open.
saved_open
check "V opens Saved copies with the hut's row: its time as its name, its size and block count" eval \
	'[[ $(val 1 copyrows) == "$name / 4 × 3 × 3 · 15 blocks | shown 0 1" ]]'
keys 1 key Escape
check "Escape goes back to the game, the copy still at the aim" eval 'drv 1 "await noscreen" "wait 5" > /dev/null &&
	copy_is 1 "^at .* 15 blocks$"'
press 1 e
drv 1 "wait 10" > /dev/null
inventory=$(val 1 screenclass)
keys 1 key v
check "V does nothing with the inventory open" eval 'drv 1 "wait 10" > /dev/null && [[ $inventory != none ]] &&
	[[ $(val 1 screenclass) == "$inventory" ]]'
drv 1 "close" "wait 5" > /dev/null

# A box Copy refuses saves nothing.
drv 1 "copybox 60 -50 60 61 -50 61" > /dev/null
settled
check "a box with nothing to copy saves nothing" eval '[[ $(rows) == "1:$name:recent:4x3x3:15" ]] &&
	[[ $(val 1 copyfiles) == "1.litematic index.dat" ]]'

# "Rename" on the hut's row opens "Rename copy" with its name in the field. An empty name or only spaces greys "Save"
# out, and the field takes 64 characters, not 65. "Cancel" goes back, renaming nothing; "Hut " saves as "Hut", which
# keeps the copy outside the 10 most recent.
saved_open
row_button Rename 0
check "Rename on the hut's row opens Rename copy with the copy's name in the field" eval \
	'drv 1 "await screen Rename copy" "wait 5" > /dev/null &&
	[[ $(val 1 widgets) == "field=$name: "*" | Save: "*" | Cancel: "* ]]'
check "shots of Rename copy" shots rename-copy "Rename copy" rename_box
type_name ""
check "with the name empty, Save is greyed out" eval '[[ $(val 1 widgets) == "field=: "*" | Save (greyed out): "* ]]'
type_name "   "
check "and with only spaces" eval '[[ $(val 1 widgets) == "field=   : "*" | Save (greyed out): "* ]]'
type_name "$(printf 'N%.0s' {1..65})"
check "a name of 65 characters can't be typed: the field holds 64" \
	eval '[[ $(val 1 widgets) == "field=$(printf "N%.0s" {1..64}): "* ]]'
widget_click 0 Cancel
check "Cancel goes back to Saved copies, renaming nothing" eval 'drv 1 "await screenclass $screen" "wait 5" > /dev/null &&
	settled && [[ $(copy_rows) == "$name / 4 × 3 × 3 · 15 blocks" ]] && [[ $(rows) == "1:$name:recent:4x3x3:15" ]]'
row_button Rename 0
drv 1 "await screen Rename copy" "wait 5" > /dev/null
type_name "Hut "
widget_click 0 Save
check "Save with the name \"Hut \" goes back to Saved copies, the row named Hut" eval \
	'drv 1 "await screenclass $screen" "wait 5" > /dev/null && settled &&
	drv 1 "wait 5" > /dev/null && [[ $(copy_rows) == "Hut / 4 × 3 × 3 · 15 blocks" ]]'
check "and the rename keeps the copy, in the index too" eval '[[ $(rows) == "1:Hut:kept:4x3x3:15" ]] &&
	[[ $(index_rows) == "version 1 next 2: 1:Hut:kept:4x3x3:15" ]]'
widget_click 0 Close
check "Close goes back to the game" drv 1 "await noscreen"

# Eleven more copies, of one block each, each of another block: the first of them drops with its file, and the Hut
# stays.
for i in "${!blocks[@]}"; do
	drv 1 "copybox $((40 + i)) -60 50 $((40 + i)) -60 50" > /dev/null
done
settled
recent=()
for i in $(seq 12 -1 3); do
	recent+=("$i:$time_name:recent:1x1x1:1")
done
expected="^$(IFS=';'; echo "${recent[*]}" | sed 's/;/ ; /g') ; 1:Hut:kept:4x3x3:15$"
check "eleven more copies leave 11 rows: the 10 newest and the Hut" eval '[[ $(rows) =~ $expected ]]'
check "and the first of them is gone, with its file" eval '[[ $(val 1 copyfiles) == "1.litematic 10.litematic \
11.litematic 12.litematic 3.litematic 4.litematic 5.litematic 6.litematic 7.litematic 8.litematic 9.litematic index.dat" ]]'
check "and the index on disk lists the same, its next number 13" eval \
	'[[ $(index_rows) == "version 1 next 13: $(rows)" ]]'
check "and Litematica reads each one's file" eval '[[ $(val 1 "copyschematic 12") == *": Copy 1x1x1, 1 blocks" ]] &&
	[[ $(val 1 "copyschematic 3") == *": Copy 1x1x1, 1 blocks" ]]'

# The screen's 11 rows, newest first; at 854 by 480, GUI scale 2, six show, and the mouse wheel scrolls to the rest.
screen_rows=()
for i in $(seq 10); do
	screen_rows+=("$time_name / 1 × 1 × 1 · 1 block")
done
expected="^$(IFS=';'; echo "${screen_rows[*]}" | sed 's/;/ ; /g') ; Hut / 4 × 3 × 3 · 15 blocks$"
saved_open
check "Saved copies shows the 11 rows, newest first, each one block's \"1 × 1 × 1 · 1 block\"" eval \
	'[[ $(copy_rows) =~ $expected ]]'
check "at 854 by 480, the first six in view" eval '[[ $(shown) == "0 6" ]]'
scroll_end
check "and the mouse wheel scrolls to the last six" eval '[[ $(shown) == "5 6" ]]'
check "shots of Saved copies scrolled to its top" shots saved-copies-top "Saved copies" copies_box scroll_top
check "and to its end" shots saved-copies-end "Saved copies" copies_box scroll_end
drv 1 "close" "wait 5" > /dev/null

# "Paste" on the Hut's row brings the Hut to the aim unlocked and unturned, in the copy's mode, in place of a turned
# and locked copy, the screen closed.
drv 1 "cmd tp @s 61.5 -56 36.5 0 30" "wait 20" > /dev/null
aim 1 60 -60 40
xdo 1 click 4
drv 1 "wait 5" > /dev/null
press 1 h
press 1 r
check "the copy at the aim is turned, locked and in Line" eval \
	'copy_is 1 "^at .* NONE CLOCKWISE_90 " && [[ $(val 1 copylock) == locked ]] && [[ $(val 1 copymode) == LINE ]]'
before_pick=$(val 1 copyindex)
saved_open
row_button Paste 10
check "Paste on the Hut's row closes the screen" drv 1 "await noscreen"
settled
check "and brings the Hut to the aim, unlocked and unturned" eval 'drv 1 "await copy at" "wait 5" > /dev/null &&
	copy_is 1 "^at .* NONE NONE unselected 15 blocks$" && [[ $(val 1 copylock) == unlocked ]]'
check "in the copy mode last set, Line" eval '[[ $(val 1 copymode) == LINE ]]'
# During a Line set, the line ends with the key before the cancel; Escape drops the set.
aim 1 60 -60 40
use_click 1
check "after Line's first click, the line ends with [V: saved copies][Esc/Left-click: cancel]" eval \
	'drv 1 "wait 5" > /dev/null && [[ $(val 1 copyset) != none ]] &&
	[[ $(val 1 modehud) == *"[R: Mode (Line)][V: saved copies][Esc/Left-click: cancel]" ]]'
press 1 Escape
check "Escape drops the set, the copy still at the aim" eval '[[ $(val 1 copyset) == none ]] && copy_is 1 "^at "'
press 1 r
press 1 r
press 1 r
aim 1 60 -60 40
read -r px py pz < <(copy_min)
use_click 1
check "a right click pastes it" drv 1 "await block $px $py $pz minecraft:oak_planks" \
	"await block $((px + 1)) $((py + 1)) $((pz + 2)) minecraft:oak_door" \
	"await block $((px + 3)) $((py + 2)) $((pz + 1)) minecraft:wall_torch"
check "and Planner2 sees its blocks" drv 2 "await block $px $py $pz minecraft:oak_planks" \
	"await block $((px + 3)) $((py + 2)) $((pz + 1)) minecraft:wall_torch"
check "with the door facing south and the torch west, as copied" eval \
	'[[ $(val 1 "state $((px + 1)) $((py + 2)) $((pz + 2))") == *"facing=south"*"half=upper"* ]] &&
	[[ $(val 1 "state $((px + 3)) $((py + 2)) $((pz + 1))") == *"facing=west"* ]]'
press 1 Escape
check "Escape puts the copy away" copy_is 1 "^away "
paste_button
check "and the radial menu's Paste button brings the Hut back" copy_is 1 "^at .* NONE NONE unselected 15 blocks$"
settled
check "a pick, a turn and a paste change no saved copy" eval '[[ $(val 1 copyindex) == "$before_pick" ]]'

# "Delete" on the Hut's row asks again; "Cancel" leaves it, and "Delete" removes the row and the file. The copy at the
# aim stays.
saved_open
row_button Delete 10
check "Delete on the Hut's row asks: Delete the copy \"Hut\"?" eval \
	'drv 1 "await screen Delete the copy \"Hut\"?" "wait 5" > /dev/null &&
	[[ $(val 1 widgets) == *"Delete: "*" | Cancel: "* ]]'
check "shots of the delete question" shots delete-copy-question "Delete the copy \"Hut\"?" question_box
widget_click 0 Cancel
check "Cancel goes back to Saved copies, the Hut still there" eval \
	'drv 1 "await screenclass $screen" "wait 5" > /dev/null && settled && [[ $(copy_rows) == *" ; Hut / 4 × 3 × 3 · 15 blocks" ]] &&
	[[ $(rows) == *" ; 1:Hut:kept:4x3x3:15" ]]'
row_button Delete 10
drv 1 "await screen Delete the copy \"Hut\"?" "wait 5" > /dev/null
# The question is itself the question screen's first widget whose text starts with "Delete"; the button is the second.
widget_click 1 Delete
check "Delete goes back to Saved copies, without the Hut's row" eval \
	'drv 1 "await screenclass $screen" "wait 5" > /dev/null && settled && drv 1 "wait 5" > /dev/null &&
	[[ $(copy_rows) != *Hut* ]] && [[ $(shown) == "4 6" ]]'
drv 1 "close" "wait 5" > /dev/null
check "a delete removes the Hut's row and its file" eval 'no_row 1 && [[ $(rows) == 12:* ]] &&
	[[ $(val 1 copyfiles) == "10.litematic 11.litematic 12.litematic 3.litematic 4.litematic 5.litematic \
6.litematic 7.litematic 8.litematic 9.litematic index.dat" ]]'
check "and the index without it" eval '[[ $(index_rows) == "version 1 next 13: $(rows)" ]]'
check "and the copy at the aim stays" copy_is 1 "^at .* 15 blocks$"

# A copy's file cut short: "Paste" on its row drops it from the list, with a message, and renames it.
head -c 40 "$folder1/7.litematic" > "$folder1/7.cut" && mv "$folder1/7.cut" "$folder1/7.litematic"
unreadable_lines=$(chat_count 1 "That copy couldn't be read.")
# The pointer goes back to the window's middle as the screen closes, which turns the view, so the player looks at the
# same point before and after, for the shots to differ only in the chat.
aim 1 60 -60 40
paste_7() {
	saved_open && row_button Paste "$(row_of 7)" && drv 1 "await noscreen" > /dev/null && aim 1 60 -60 40
}
check "Paste on a copy whose file is cut short shows a message" eval \
	'chat_shots copy-unreadable "That copy couldn'"'"'t be read." paste_7 &&
	said_once_more 1 "That copy couldn'"'"'t be read." "$unreadable_lines"'
settled
check "and drops it from the list and the index" eval 'no_row 7 &&
	[[ $(index_rows) == "version 1 next 13: $(rows)" ]]'
check "and renames its file with .unreadable" eval '[[ -s $folder1/7.litematic.unreadable && ! -e $folder1/7.litematic ]]'
check "and logs an error" grep -qF "couldn't read the saved copy" /out/client1.log
check "and the copy at the aim stays" copy_is 1 "^at .* 15 blocks$"

# V during a box for Copy opens the screen; "Close" goes back with the box still drawn, and "Paste" on a row ends the
# box and brings that copy, the newest, to the aim.
copy_button
drv 1 "lookat 57.5 -61 45.5" "wait 5" > /dev/null
attack_click 1
check "a box for Copy with its first corner" eval '[[ $(val 1 drawbox) == "COPY 1 "* ]]'
saved_open
widget_click 0 Close
check "V during the box opens Saved copies, and Close goes back with the box still drawn" eval \
	'drv 1 "await noscreen" "wait 5" > /dev/null && [[ $(val 1 mousemode) == BOX ]] && [[ $(val 1 drawbox) == "COPY 1 "* ]]'
saved_open
row_button Paste 0
check "Paste on the newest row then ends the box and brings that copy to the aim" eval \
	'drv 1 "await noscreen" > /dev/null && settled && drv 1 "await copy at" "wait 5" > /dev/null &&
	[[ $(val 1 mousemode) == COPY_AT_AIM ]] && copy_is 1 "^at .* 1 blocks$"'
aim 1 58 -60 44
read -r px py pz < <(copy_min)
use_click 1
check "the newest, the emerald block" drv 1 "await block $px $py $pz minecraft:emerald_block"

# After the game restarts, the rows come back, and the Paste button brings the newest copy.
saved_rows=$(rows)
stop_client 1
start_client 1
check "after Planner1's game restarts, the rows come back" eval 'drv 1 "await noscreen" "await session" \
	"await copiessettled" > /dev/null && [[ $(rows) == "$saved_rows" ]]'
drv 1 "window 854 480 2" "ebmode SINGLE" "cmd tp @s 61.5 -56 36.5 0 30" "wait 40" "fly true" > /dev/null
check "with no copy since the game started" copy_is 1 "^none$"
paste_button
settled
check "and the radial menu's Paste button brings the newest to the aim" eval 'drv 1 "await copy at" "wait 5" > /dev/null &&
	copy_is 1 "^at .* NONE NONE unselected 1 blocks$"'
aim 1 64 -60 44
read -r px py pz < <(copy_min)
use_click 1
check "the newest, the emerald block" drv 1 "await block $px $py $pz minecraft:emerald_block"

# The server restarted without Building Planner: the local session shows the same rows, and a saved copy pastes there
# as ghost blocks.
settled
stop_client 1
stop_client 2
stop_server
check "the server starts without Building Planner" eval 'start_server "$ADDON" server-without-addon &&
	! grep -qF "Building Planner loaded" /out/server-without-addon.log'
start_client 1
check "client 1 starts a local session" eval 'drv 1 "await noscreen" "await session" > /dev/null &&
	[[ $(val 1 sessionkind) == local ]]'
check "which shows the same saved copies" eval 'settled && [[ $(rows) == "$saved_rows" ]]'
drv 1 "window 854 480 2" "ebmode SINGLE" "cmd tp @s 81.5 -56 76.5 0 30" "wait 40" "fly true" \
	"await loaded minecraft:overworld 80 80" > /dev/null
saved_open
row_button Paste "$(row_of 3)"
check "Paste on a saved copy's row there brings it to the aim" eval 'drv 1 "await noscreen" > /dev/null && settled &&
	drv 1 "await copy at" "wait 5" > /dev/null && copy_is 1 "^at .* 1 blocks$"'
aim 1 80 -60 80
read -r px py pz < <(copy_min)
use_click 1
check "and pastes as a ghost block, the granite of the second copy, and no real block" eval \
	'drv 1 "await block $px $py $pz minecraft:granite" "wait 20" > /dev/null &&
	[[ $(val 1 "real $px $py $pz") == minecraft:air ]]'

# An unreadable index: the game starts with no saved copies, which the screen says, and renames it, leaving the
# copies' files; the next copy, of a real block, takes a number past theirs.
settled
stop_client 1
printf 'not an index' > "$folder1/index.dat"
start_client 1
check "an unreadable index.dat starts the game with no saved copies" eval 'drv 1 "await noscreen" "await session" \
	> /dev/null && settled && [[ $(rows) == none ]]'
check "and is renamed with .unreadable, the copies' files staying" eval \
	'[[ $(cat "$folder1/index.dat.unreadable") == "not an index" && ! -e $folder1/index.dat ]] &&
	[[ -s $folder1/12.litematic && -s $folder1/3.litematic ]]'
check "and logs an error" grep -qF "it's renamed to index.dat.unreadable" /out/client1.log
drv 1 "window 854 480 2" "cmd tp @s 81.5 -56 76.5 0 30" "wait 40" "fly true" \
	"await loaded minecraft:overworld 80 80" > /dev/null
saved_open
check "Saved copies then has no rows, and says No saved copies yet." eval \
	'[[ $(val 1 copyrows) == "none | shown 0 0" ]] && [[ $(val 1 drawntexts) == *"No saved copies yet."* ]]'
check "shots of the empty Saved copies" shots saved-copies-empty "Saved copies, empty" copies_box
drv 1 "close" "wait 5" "cmd setblock 85 -60 80 minecraft:stone" "await real 85 -60 80 minecraft:stone" \
	"copybox 85 -60 80 85 -60 80" > /dev/null
settled
check "the next copy takes the number past the files in the folder, 13" eval \
	'[[ $(index_rows) =~ ^version\ 1\ next\ 14:\ 13:$time_name:recent:1x1x1:1$ ]] && [[ -s $folder1/13.litematic ]]'

# With the folder emptied, the Paste button says there's nothing to paste.
stop_client 1
rm -r "${folder1:?}"
start_client 1
nothing_lines=$(chat_count 1 "Nothing to paste. Use Copy first.")
drv 1 "await noscreen" "await session" "window 854 480 2" "ebmode SINGLE" > /dev/null
settled
paste_button
check "with the folder emptied, the Paste button says there's nothing to paste" eval \
	'said_once_more 1 "Nothing to paste. Use Copy first." "$nothing_lines" && copy_is 1 "^none$"'
