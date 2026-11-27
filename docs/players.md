# Player Setup

The server uses these mods for planning builds together:

- Litematica shows a build plan as see-through "ghost" blocks.
- Building Planner lets everyone on the server edit the same ghost blocks at once, and shows each change to everyone as it's made.
- Effortless Building draws lines, walls, floors, and other shapes in ghost blocks.
- Freecam lets you fly the camera around a build without moving your character.

You need a Minecraft: Java Edition account. The steps below install the right version of Minecraft and the mods for you.

## Prism Launcher

Prism Launcher is a free Minecraft launcher. It stores each copy of the game, called an "instance", in its own folder with its own mods. The server's instance updates its mods each time you start it, so you never install updates yourself.

### Installation

1. Go to https://prismlauncher.org/download and download the version for your computer.
2. Run the installer and open Prism Launcher.
3. If Prism Launcher asks about Java, let it download Java for you.

### Microsoft Account

1. In Prism Launcher, click "Accounts" at the top right, then "Manage Accounts".
2. Click "Add Microsoft" and sign in with the Microsoft account that owns Minecraft.
3. Close the accounts window.

### Server Instance

1. Click "Add Instance" at the top left.
2. Choose "Import" on the left.
3. Paste this link into the text box: https://github.com/Geomitron/minecraft-server/releases/latest/download/prism-instance.zip
4. In the "Name" box at the top, change the name to "Server Pack".
5. Click "OK". An instance named "Server Pack" appears.

### Your Old Settings

If you've played Minecraft on this computer before, you can bring your controls, video, sound, and other settings into the new instance. Minecraft saves them in a file named `options.txt`.

1. Find `options.txt` where you played before, and copy it:
   - **Prism Launcher:** right-click the instance you played before, click "Folder", and open the "minecraft" folder.
   - **The Minecraft Launcher:** on Windows, press Windows+R, type `%APPDATA%\.minecraft`, and press Enter. On a Mac, in Finder, click "Go", then "Go to Folder", type `~/Library/Application Support/minecraft`, and press Return. On Linux, open the hidden `.minecraft` folder in your home folder.
2. In Prism Launcher, right-click "Server Pack" and click "Folder".
3. Open the "minecraft" folder and paste `options.txt` there. If your computer asks, replace the file that's already there.

### First Launch

1. Double-click "Server Pack".
2. A small window shows the mods downloading. Wait for it to finish. It's slower the first time, when it downloads every mod.
3. Minecraft starts. Click "Multiplayer", then "Add Server", and enter the server address.

Always start Minecraft from the "Server Pack" instance. Starting it anywhere else skips the mod updates.

## Build Plans

You plan a build in ghost blocks: see-through blocks that show where real blocks will go. Everyone on the server sees the same ghost blocks, anyone can add or erase them, and each change shows for everyone as it's made. Ghost blocks never turn into real blocks and don't use up your items. Once a plan looks right, you build it for real by hand, or turn on "Place real blocks" to build it with shapes and pastes.

### Keys

These are the keys for planning. You can change any of them in Controls, under the "Building Planner", "Effortless Building", and "Freecam" headings.

| Key | Name in Controls | Use |
| --- | --- | --- |
| Left Alt (hold) | "Open Radial Menu" | Open the radial menu, where you choose a build mode or a planning tool. |
| N | "Show or hide ghost blocks" | Hide all ghost blocks on your screen, or show them again. |
| B | "Block mix" | Open the block mix. |
| H | "Lock or unlock" | Lock where your next click goes, or lock the copy in place. |
| Arrow keys, Page Up, Page Down | "Nudge forward", "Nudge back", "Nudge left", "Nudge right", "Nudge up", "Nudge down" | Move what's locked by one block. |
| R | "Change copy mode" | Switch the copy between Off, Line, Wall, and Floor. |
| Ctrl+Z | "Undo" | Undo your newest change to the ghost blocks. |
| Ctrl+Y | "Redo" | Redo the change you undid. |
| F4 | "Toggle Freecam" | Fly the camera away from your character. |
| M | "Config GUI" | Open Freecam's settings. |

Controls shows "Undo" and "Redo" as Z and Y alone, and you hold Ctrl with them. Left Control, Minecraft's "Sprint" key, also turns blocks, lines up the copy, and marks ghost blocks in Delete. It still makes you sprint if you walk forward while you hold it.

### Ghost Blocks

Each ghost block is tinted to show what's built there:

- **Light blue:** nothing is built there yet.
- **Red:** a different block is there.
- **Orange:** the right block is there, but facing another way, or with a different number of a block that stacks in one space, such as candles.

Ghost blocks within 32 blocks of you also show through walls, so you can find mistakes and what's left to build.

When you place the same block as a ghost block, facing the same way, the ghost block disappears for everyone. Breaking the real block later doesn't bring the ghost block back.

When someone else changes ghost blocks, the blocks they changed glow for a few seconds, with their name beside them.

Press N to hide all ghost blocks on your own screen, and press it again to show them. Chat says "Ghost blocks hidden." or "Ghost blocks shown.", and the game remembers your choice. If you draw a shape while they're hidden, they show again first.

### Radial Menu

Hold Left Alt to open the radial menu. Point at a build mode or a button, and click it or let go of Left Alt to choose it. Pointing at a button shows its name.

- **Build modes:** the ring around the middle shows "Disable", "Single", "Line", "Wall", "Floor", "Cube", "Diagonal Line", "Diagonal Wall", "Slope Floor", "Circle", "Cylinder", and "Sphere".
- **Effortless Building's buttons:** undo, redo, "Settings", and the wrench, which opens "Modifier Settings", where you can turn on mirror, radial mirror, and array to repeat each shape.
- **Building Planner's buttons:** "Copy", "Paste", and "Delete" in the row above the wrench, and "Block mix", "Material list", and "Material lists" in the row above that. The button under the wrench shows or hides ghost blocks, as N does.
- **Sliders:** "Build speed" and "Break speed", on the right side of the menu while "Single" is the build mode.

### Shapes

1. Hold Left Alt and choose a build mode, such as "Wall". Its name shows to the left of your hotbar while it's on.
2. Hold a block, or another item that places a block, such as redstone dust, a fence, a door, or seeds.
3. Aim where the shape starts. The block a right-click would place shows there, and a red frame shows on the block a left-click would start from. Right-click.
4. Aim where the shape ends, and right-click again. "Cube", "Cylinder", "Sphere", "Slope Floor", "Diagonal Line", and "Diagonal Wall" need a third right-click for the height.

To erase ghost blocks, draw a shape with left-clicks instead. In "Single", each right-click places one ghost block and each left-click erases one, so the red frame shows only on ghost blocks. Hold the button to keep placing or erasing, at the speed the "Build speed" and "Break speed" sliders set.

A shape can start on a ghost block or on a real block. A line above the hotbar shows the controls you can use at each moment. With a tool, food, or an empty hand, clicks break and use real blocks as they do without mods. To place real blocks with a block in hand, choose "Disable", or turn on "Place real blocks".

### Locking and Nudging

Press H to lock where your next shape starts. The locked block stays where it is while you walk or fly around it. The arrow keys move it one block forward, back, left, or right of the way you face, and Page Up and Page Down move it up or down. Your next click starts the shape at the locked block, wherever you aim.

During a shape, H locks the point your next click sets, such as a wall's far corner or a cube's height. The nudge keys then move that point, which changes the shape's size. A nudge that wouldn't change the size (such as across a wall) moves the whole shape. After each click, the next point follows your aim until you lock it. The lock ends once the shape is placed or erased, or when you press H again.

While a block is locked, a box shows around it: blue where the block will go in, yellow where it would replace a different ghost block, and red where it's outside the world, where a click places nothing.

### Turning and Flipping

With a block in hand, hold Left Control and scroll to give the block a quarter turn, or middle-click to flip it, such as a stair upside down or a log on its side. The block stays turned, whichever way you face, until you switch to a different block. Shapes, and the blocks shown before you click, use the turn.

This works for real blocks in "Disable" too. Hold Left Control to see the block where a right-click will place it, turn or flip it, and right-click while you still hold Left Control. Without Left Control, a right-click places the block as usual. A turned block that can't stay where it would go, such as a torch turned away from its wall, shows red, and the right-click doesn't place it.

To place real blocks facing the way their ghost blocks face:

1. Hold Left Alt and click "Settings".
2. Turn on "Match ghost blocks", and click "Save".

With it on, while ghost blocks show, a block you place into a ghost block of the same block faces the way the ghost block does, whichever way you stand.

### Block Mix

A block mix draws shapes with several blocks at once, each in its own share, such as 80 cobblestone to 20 mossy cobblestone. A mix of one block lets you plan with a block you don't have.

1. Press B, or choose "Block mix" in the radial menu. The "Block mix" screen lists the blocks in your inventory. Turn off "Only my inventory" to list every block in the game, and type in "Search blocks" to find one.
2. Click up to nine blocks to add them to the mix. Click a block again to take it out.
3. Under each block in the mix, type its share. The screen shows each block's percentage of the mix.
4. Click "Use mix". Chat shows "Block mix on. Press B to edit or clear it."

While you hold a block in any build mode but "Disable", shapes use the mix instead of the block you hold, and a panel at the right edge of the screen shows the mix. Press B to change the mix, or click "Clear Mix" to empty it and turn it off. The mix lasts until you close the game.

### Undo

Press Ctrl+Z, or click the undo button in the radial menu, to undo your newest change: a whole shape, paste, or erase at a time. Press Ctrl+Y, or click the redo button, to redo it, until you make a new change.

Undo takes back only your own changes. If someone else has changed one of those blocks since, the block stays as they left it, and chat tells you how many blocks were left.

### Delete

Delete erases many ghost blocks at once: you mark them, then erase every marked one.

1. Hold Left Alt and choose "Delete".
2. Hold Left Control and sweep your aim across ghost blocks. Each ghost block the crosshair passes over is marked: it shows red, and the marked blocks are listed beside the crosshair.
3. To mark everything in a box, hold Left Control, press the left mouse button on one block, and let go on another. Every ghost block in the box between them is marked.
4. Hold the left mouse button, without Left Control, for one second. A bar in the middle of the screen fills while you hold it, and then the marked ghost blocks are erased. Chat says how many, such as "Erased 120 ghost blocks."

A sweep or a box that starts on a marked ghost block unmarks instead. Right-click or press Esc to clear the marks, and do it again to leave Delete. One undo brings back everything one erase took.

### Copy and Paste

Copy takes the blocks in a box, ghost blocks and real blocks alike, and pastes them as ghost blocks wherever you aim, or as real blocks while "Place real blocks" is on. Only you see the copy until you paste it.

1. Hold Left Alt and choose "Copy".
2. Left-click to set one corner of the box, left-click again to set the opposite corner on the same level, and left-click a third time to set the height. The box shows white, with its size and the blocks in it. If you aim at the top of a block, the first corner is the space above it, so a box started on the ground includes what stands on it.
3. Chat says how many blocks were copied, and the copy follows your aim.

A box can be up to 256 blocks along each side. Right-click or press Esc to cancel it. While ghost blocks are hidden, Copy copies only real blocks.

With the copy at your aim:

- **Paste:** right-click. The copy stays at your aim for more pastes, and one undo takes back one paste. The empty space in a copy leaves the ghost blocks under it as they are.
- **Turn and mirror:** scroll to turn the copy, and middle-click to mirror it.
- **Lock:** press H to lock the copy in place, so you can walk or fly around it. The arrow keys, Page Up, and Page Down then nudge it one block. It unlocks once you paste it.
- **Rows of copies:** press R to switch between Off, Line, Wall, and Floor. In Line, Wall, or Floor, right-click to set the first copy, aim to add copies beside it (up to 10 along each side), and right-click again to paste them all.
- **Close:** left-click or press Esc to put the copy away. In Line, Wall, or Floor after the first right-click, this cancels the row and leaves the copy at your aim.

The copy snaps flush to the sides of your recent pastes. Hold Left Control to keep it in line with your latest paste. Its box shows blue where it will paste, yellow where it would replace ghost blocks (it still pastes), and red where it can't paste, with the reason above the hotbar.

Choosing a build mode or another planning tool, or showing or hiding ghost blocks, puts the copy away. Choose "Paste" in the radial menu to bring your latest copy back. Leaving the server or changing dimension forgets it.

### Real Blocks

With "Place real blocks" on, shapes and pastes place real blocks from your inventory instead of ghost blocks.

1. Hold Left Alt and click "Settings".
2. Turn on "Place real blocks", and click "Save".

With it on, each right-click shape in a build mode other than "Disable" places real blocks, along with its mirror and array copies, using the block mix if it's on. Each block faces the way the preview shows it. Left-click shapes still erase ghost blocks.

Each block uses its item from your inventory, as when you place it by hand: a double slab uses two slabs. If your inventory doesn't have every item the whole shape needs, with its mirror and array copies, nothing is placed. In creative mode, shapes use no items.

A block goes in only within 64 blocks of you, and only into a space a right-click could place it in, such as air, water, or grass, never in place of a real block or where a mob or player stands. You can place up to 2,000 real blocks at once, and up to 2,000 a second. If the server keeps any blocks out, chat says how many weren't placed.

Before you click, each block that won't go in shows red in the preview, such as one too far away, one where a real block or a mob is, or a torch with nothing to stand on. If you're short of any item, the whole shape shows red.

In a shape, torches, signs, banners, and coral fans stand upright even where you aim at the side of a wall, so beside a wall they go in only where a block under them holds them up. To put one on a wall, place it by hand in "Disable".

A paste follows the same rules, and each block faces the way the copy shows it. The copy's box shows blue when all of it will go in, yellow when part of it won't, and red when nothing will. A pasted chest is empty, and a pasted sign is blank.

Ctrl+Z doesn't take real blocks back: it undoes your newest change to ghost blocks. Real blocks come out only when you break them.

### Material Lists

A material list counts the blocks a plan needs.

1. Hold Left Alt and choose "Material list".
2. Draw a box around the plan with three left-clicks, as for Copy. The box shows blue.
3. The material list opens. "Total" counts each block in the box, ghost blocks and real blocks alike. "Missing" is how many of each block the ghost blocks still need: green once it's done, gold when you carry enough, and red otherwise. "Available" is how many you carry.

On the list's screen:

- Click a column's heading to sort the list by it.
- "Multiplier" multiplies the counts, for building the plan more than once.
- "Refresh" counts the box again, while you're near it.
- "Info HUD" pins the list to the top-left corner of your screen, with each block's "All", "Hold", and "Need". While you're near the box, the panel counts it again every 2 seconds, so "Need" drops as the group builds. The panel stays on after you restart the game, until you turn it off.
- "Save list" saves the list under a name, and "Saved lists" opens a list you saved. Your latest list counted from a box is always saved too, as "Last counted". The server stores your lists, so they're the same from any computer.
- "Close" closes the screen.

Choose "Material lists" in the radial menu to open the current list again.

### Esc

Each press of Esc steps out of one thing, in this order: a row of copies, the copy, a box, Delete's marks, Delete, a shape you've started, then the build mode, back to "Disable". Once nothing is left, Esc pauses the game. With a screen open, Esc closes it as usual.

### Freecam

Freecam moves the camera away from your character, so you can look at a build from any side.

1. Press F4. Your character stays where it is.
2. Move the camera with your usual walking keys. Jump moves it up, and sneak moves it down.
3. Press F4 again to go back to your character.

While the camera is away, shapes, boxes, the copy, and Delete aim from the camera, as if you stood there, and the nudge keys go by the way the camera faces. With Freecam's settings as they start, clicks on real blocks don't reach the world from the camera, so go back to your character to build for real. Ghost blocks show and change only near your character, where your game has loaded the world.
