# HomesGUI

A configurable homes GUI plugin for Minecraft 1.12.2 servers.

## Planned features

- `/home` and `/homes` open a 27-slot chest menu.
- Seven player homes, displayed as beds.
- Click an empty home slot to save your current location.
- Left-click a saved home to begin a configurable teleport countdown.
- Countdown cancels if the player moves or takes damage.
- Right-click twice to overwrite a home.
- Shift + right-click twice to delete a home.
- Press Q to rename a home using chat.
- Unsafe destination warnings and confirmation.
- Configurable teleport cooldown, delay, and confirmation windows.
- Teleport particles and sounds.
- Owner-only hidden utility commands.

## Development

- Minecraft: 1.12.2
- Java: 8
- Build system: Maven
- API: Spigot 1.12.2

The project is currently being scaffolded. Features will be implemented incrementally and documented here.

## Building

Once the source is implemented, build with:

```bash
mvn clean package
```

The plugin JAR will be generated in `target/`.

## Installation

1. Build or download the plugin JAR.
2. Place it in your server's `plugins` folder.
3. Restart the server.
4. Configure `plugins/HomesGUI/config.yml`.

## License

License not yet selected.
