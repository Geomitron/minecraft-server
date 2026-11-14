# Player Setup

The server uses these mods for planning builds together:

- Litematica shows a build plan as see-through "ghost" blocks.
- Syncmatica shares those plans with everyone on the server.
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
3. Minecraft starts. Click "Multiplayer", then "Add Server", and enter the server address Geo gave you.

Always start Minecraft from the "Server Pack" instance. Starting it anywhere else skips the mod updates.
