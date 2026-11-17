# Minecraft Server

A survival Minecraft server, and Building Planner, a mod being built for it. With Building Planner, players draft builds together in ghost blocks that everyone on the server sees and edits live, then build them by hand.

- [Player setup](docs/players.md): the mods players use, and how to install them with Prism Launcher.
- [Server plan](docs/plan.md): the current server, the decisions so far, and the work still open.
- [Building Planner design](docs/building-planner.md): how the mod works.
- `pack/`: the mod pack that each player's instance downloads its mods from.
- `instance/`: the Prism Launcher instance players import.
- `mod/`: the code for Building Planner.
- `ops/error-events/`: a service that watches the server for errors and sends alerts.
