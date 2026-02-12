# 🚀 EasyCommands

EasyCommands (EasyCommandsPlugin) makes running repetitive or commonly-used commands effortless by letting players bind commands to quick shortcuts and view them in a compact HUD.

An intuitive in-game UI lets players assign commands to numbered slots, preview active shortcuts, and trigger them with simple interactions. Perfect for players and admins who want faster workflows.

---

**📦 Project**: EasyCommandsPlugin
- **Group:** Tblack
- **Name:** EasyCommandsPlugin
- **Version:** 0.1.0
- **Main class:** br.tblack.plugin.CommandsScreenPlugin
- **Description:** Run any command you want with simple keyboard shortcuts.
- **Author:** Ewerton "Tblack" Mendes

---

**✨ Features**
- ✅ Bind any server command to numbered slots for quick execution.
- 🎛️ In-game configuration UI (`/cmd`) to add, edit, or remove shortcuts.
- 🖥️ Optional Quick Command HUD that displays your active bindings.
- 💾 Per-player JSON storage (persisted to `plugins/Shortcuts/shortcuts.json`).

**Why use this plugin?**
- ⏱️ Save time by turning repetitive commands into one-click actions.
- 🎮 Improve usability for admins and players with frequently-run commands.

---

**📥 Installation**
1. Build or download the plugin JAR.
2. Place the JAR into your server `mods` or `plugins` folder.
3. Restart the server.

The plugin will create its data folder and the shortcuts storage file automatically on first run.

---

**🎯 Usage**
- Run `/cmd` in-game to open the EasyCommands setup UI.
- Assign commands to numbered slots (example: slot 1 → `spawn`).
- The Quick Command HUD shows `Slot N -> /yourcommand` so you always know what's bound.
- Trigger shortcuts by performing the configured interaction (the plugin maps player interactions to slots).

Tip: Keep sensitive or admin-only commands protected via your server permission system.

---

**⚙️ Configuration & Data**
- Shortcuts are stored per-player in `plugins/Shortcuts/shortcuts.json`.

Example structure:

```json
{
	"<player-uuid>": {
		"1": "spawn",
		"2": "kit starter",
		"3": "message Hello world"
	}
}
```

- The `ShortcutConfig` class in source handles loading and saving using Gson (pretty-printed).
- To bulk-install shortcuts, edit `plugins/Shortcuts/shortcuts.json` while the server is stopped, then start the server.

---

**🔐 Permissions & Security**
- The `/cmd` UI is provided by the plugin. Use your server permission system to restrict access to privileged commands.
- Avoid allowing regular players to bind commands that perform server-critical actions (teleports, item grants, world changes).

---

**🛠️ Building from source**
This project uses Gradle. On Windows run:

```
.\gradlew.bat build
```

On Unix/macOS run:

```
./gradlew build
```

After a successful build, the plugin JAR is located in `build/libs/`. Drop it into your server `mods`/`plugins` folder and restart.

---

**🐞 Troubleshooting**
- If shortcuts don't persist: ensure the server process can write to `plugins/Shortcuts/`.
- If the HUD/UI doesn't appear: check server logs for plugin startup lines and confirm the plugin loaded successfully.
- If interaction triggers misfire: the plugin uses interaction timing heuristics; verify client/server latency isn't interfering.

---

**🔄 Compatibility & Notes**
- Built as a Hytale server plugin — see `manifest.json` in `src/main/resources/` for metadata.
- Confirm server API compatibility on a staging server before production use.

---

**🤝 Contributing**
- Bug reports, feature requests, and PRs are welcome. Please open issues or pull requests.
- Follow project style and include tests where appropriate.

---

**📜 Changelog**
- 0.1.0 — Initial release: UI for configuring per-player shortcuts; HUD display; per-player JSON storage.

---

**✉️ Credits & Contact**
- Author: Ewerton "Tblack" Mendes
- Project manifest: `src/main/resources/manifest.json`

License: MIT — included as `LICENSE` in the project root.