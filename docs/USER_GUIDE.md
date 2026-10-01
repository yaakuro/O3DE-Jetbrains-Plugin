# O3DE plugin for CLion — User guide

This guide is for people **using** the plugin inside CLion. Developers looking for build
instructions should read [`README.md`](../README.md) instead.

---

## 1. What the plugin does

The plugin wraps the Open 3D Engine command line tool (`o3de.sh` on Linux/macOS,
`o3de.bat` on Windows) so that the most common O3DE chores can be performed without leaving
CLion:

* create gems, projects, components and template instances,
* register engines, projects, gems, templates and external folders,
* enable a gem inside a project,
* run any other `o3de` sub command,
* inspect what is currently registered with O3DE.

Every command is executed exactly the way you would type it in a terminal, its full text is
echoed to a console, and its output and exit code are reported back to you.

---

## 2. Requirements

| Requirement | Notes |
|---|---|
| CLion **2026.1** (build 261) or newer | The plugin declares `since-build="261"` with no upper bound. |
| An O3DE engine checkout or installer | Needed only to run commands; the plugin itself has no O3DE dependency. |
| `~/.o3de/o3de_manifest.json` | Used for auto-detection and for the registry view. Not required to exist. |

---

## 3. Installation

1. Build or obtain `o3de-plugin-1.0.0.zip` (`build/distributions/o3de-plugin-1.0.0.zip`
   when building from source).
2. In CLion: **Settings | Plugins | ⚙ (Manage Plugin Repositories / gear icon) |
   Install Plugin from Disk...** and pick the zip.
3. Restart the IDE when prompted.

To try it first in a throw-away IDE instance, run `./gradlew --offline runIde` from the
project directory — that launches a sandbox CLion with only this plugin installed.

---

## 4. First-time setup

Open **Settings | Tools | O3DE**.

| Field | What it does |
|---|---|
| **o3de script** | Absolute path to `o3de.sh` / `o3de.bat`. Leave empty to auto-detect. |
| **Engine root** | Absolute path to the O3DE engine folder. Used when the script field is empty. |
| **Extra arguments** | Text inserted **after** the sub command on every run (e.g. `-v`). |
| **Show the O3DE tool window when a command starts** | Activates the bottom-stripe tool window automatically. |
| **Refresh the project after a command succeeds** | Triggers a VFS refresh when a command exits with code 0. |
| **Ask before running commands that change files** | Confirmation dialog for create/register/enable operations. |

Press **Detect** to search for the script automatically. Detection order:

1. the value of **o3de script**,
2. `scripts/o3de.sh` (or `o3de.bat`) inside the **Engine root**,
3. every engine listed under `"engines"` in `~/.o3de/o3de_manifest.json`,
4. well-known locations: `~/o3de`, `~/O3DE`, `~/.o3de`, `/usr/local/o3de`,
   `/opt/o3de`, `C:\o3de`.

> **Tip** — if detection fails, run `o3de register --this-engine` in a terminal once. That adds
> your engine to the manifest, after which **Detect** succeeds.

Both path fields also accept an *engine root* or a *scripts folder*; the plugin normalises them
to the script automatically. A one-line summary of what was found is shown below the **Detect**
button.

---

## 5. Where everything is

| Location | Contents |
|---|---|
| **Tools \| O3DE** | The complete menu (also shown as a popup at the bottom of the *Tools* menu). |
| **Project view context menu \| O3DE** | The most-used subset, scoped to the folder you right-clicked. |
| **O3DE tool window** (bottom stripe) | **Console** tab with the command output, **Registry** tab with the manifest contents and a **Lua API** tab with the Lua references read from a running target. |
| **Settings \| Tools \| O3DE** | Configuration (see above). |
| **Event log / balloon notifications** | Success, failure and "script not found" messages, tagged `O3DE`. |

### Action IDs (for keymaps)

No shortcuts are pre-assigned. Open **Settings | Keymap** and search for `O3DE`, or use these
IDs directly:

| Action | ID |
|---|---|
| Create Gem… | `O3de.CreateGem` |
| Create Project… | `O3de.CreateProject` |
| Create Component… | `O3de.CreateComponent` |
| Instantiate Template… | `O3de.InstantiateTemplate` |
| Register ▸ This Engine | `O3de.RegisterEngine` |
| Register ▸ Project… | `O3de.RegisterProject` |
| Register ▸ Gem… | `O3de.RegisterGem` |
| Register ▸ Template… | `O3de.RegisterTemplate` |
| Register ▸ External Folder… | `O3de.RegisterFolder` |
| Enable Gem in Project… | `O3de.EnableGem` |
| Show Registration | `O3de.ShowRegistration` |
| Run O3DE Command… | `O3de.RunCommand` |
| O3DE Settings… | `O3de.OpenSettings` |

---

## 6. Workflows

### 6.1 Create a gem

**Tools | O3DE | Create Gem…**

| Field | Meaning |
|---|---|
| **Name** | Gem name. Letters, digits, `_` and `-`. |
| **Path** | Destination folder. Auto-fills to `<default_gems_folder>/<Name>` as you type, until you edit it by hand. |
| **Template** | Any registered template; defaults to `DefaultGem`. |
| **Display name** | Optional, written to `gem.json`. |
| **Summary** | Optional, written to `gem.json`. |
| **Do not register the gem afterwards** | Adds `--no-register`. |

Runs `o3de create-gem`.

### 6.2 Create a project

**Tools | O3DE | Create Project…**

| Field | Meaning |
|---|---|
| **Name** | Project name. |
| **Path** | Destination folder, auto-filled to `<default_projects_folder>/<Name>`. |
| **Template** | Defaults to `DefaultProject`. |
| **Project id** | Leave empty to derive it from the name. |
| **Do not register the project afterwards** | Skips registration. |

Runs `o3de create-project`.

### 6.3 Create a component

**Tools | O3DE | Create Component…** (also in the Project view menu)

| Field | Meaning |
|---|---|
| **Name** | Component name — the templates append the `Component` suffix themselves. |
| **Destination** | Where the files are written. Pre-filled by walking up from the selected folder looking for `gem.json`; falls back to `<project>/Code` or the project root. |
| **Gem name** | Replaces `${GemName}` in the generated file names **and** contents. Auto-filled from the enclosing `gem.json`. |
| **Template** | Only templates whose name contains "component" are listed (`DefaultComponent`, `LevelComponent`, `SystemComponent`, `LyShineComponent`, …). |

Runs `o3de create-from-template -tn <template> -dn <Name> -r '${GemName}' <template>`.

> Select a folder in the project view before invoking the action — that folder is used as the
> starting point for detecting the gem and the destination.

### 6.4 Instantiate any template

**Tools | O3DE | Instantiate Template…**

| Field | Meaning |
|---|---|
| **Template** | Every template registered in the O3DE manifest. |
| **Name** | Destination name (`-dn`). |
| **Destination** | Folder to create the instance in. |
| **Replacements** | Optional, one `token = value` per line, passed as `-r` pairs. |

Example:

```
${GemName} = MyGem
${ProjectName} = MyProject
```

Runs `o3de create-from-template -dp <dest> -tn <template> -dn <name> -r '${GemName}' MyGem …`.

### 6.5 Register objects

**Tools | O3DE | Register ▸ …**

| Menu entry | Command |
|---|---|
| **This Engine** | `o3de register --this-engine` (no folder chooser) |
| **Project…** | `o3de register -pp <folder>` |
| **Gem…** | `o3de register -gp <folder>` |
| **Template…** | `o3de register -tp <folder>` |
| **External Folder…** | `o3de register -es <folder>` |

Each opens a folder chooser pre-filled with the folder selected in the project view (or the
project root). Cancelling the chooser aborts the command.

> Registering a **project** is what makes it appear under **Open Project** in the launcher and
> in the plugin's Registry tab.

### 6.6 Enable a gem in a project

**Tools | O3DE | Enable Gem in Project…**

| Field | Meaning |
|---|---|
| **Project** | The O3DE project folder — auto-detected from `project.json`. |
| **Gem** | The gem folder — auto-detected from `gem.json`. |
| **Skip the version compatibility check** | Adds `-f` (`--force`). |

Runs `o3de enable-gem -pp <project> -gp <gem>` (plus `-f` when the checkbox is on).

The counterpart `o3de disable-gem` is available through **Run O3DE Command…**.

### 6.7 Show registration

**Tools | O3DE | Show Registration** runs `o3de register-show` and prints the result into the
console — a useful cross-check when the Registry tab and the real manifest disagree.

### 6.8 Run any other command

**Tools | O3DE | Run O3DE Command…** is the escape hatch for everything not covered above
(`create-template`, `export-project`, `android-configure`, `disable-gem`,
`edit-gem-properties`, …).

| Field | Meaning |
|---|---|
| **Sub command** | Combo box with every known sub command; also editable, so you can type one that is not listed. |
| **Arguments** | Raw argument text, split on whitespace with support for quoted tokens. |
| **Working dir** | Directory the process runs in; defaults to the project root. |
| **preview** | The exact command line that will be executed, updated as you type. |

Only letters, digits, `_` and `-` are accepted for the sub command, so arbitrary options cannot
be smuggled in through that field.

---

## 7. The O3DE tool window

Open it via **View | Tool Windows | O3DE**, or let a command open it automatically
(**Show the O3DE tool window when a command starts**).

### Console tab

* Each run prints the full rendered command line prefixed with `$`.
* Standard output is shown as normal text, standard error as error text.
* When the process ends you get `[o3de] finished in N ms (exit code 0)` or
  `[o3de] failed with exit code N`, plus a balloon notification with the same information.

Toolbar buttons (top right of the tool window):

| Button | Effect |
|---|---|
| **Clear** | Clears the console buffer. |
| **Stop** | Terminates the currently running command (enabled only while one runs). |
| **O3DE Settings** | Opens Settings \| Tools \| O3DE. |

### Registry tab

A tree over `~/.o3de/o3de_manifest.json` with the sections **Engines**, **Projects**, **Gems**,
**Templates**, **External folders**, **Restricted**, **Repos** — each showing the number of
entries.

| Control | Effect |
|---|---|
| **Reload** | Re-reads the manifest from disk. |
| **Verify with o3de** | Runs `o3de register-show`, prints it to the Console tab, then refreshes the tree. |
| Hover | Shows the full path of the entry as a tooltip. |
| Double click | Selects the folder in the project view (or opens the file if the path is inside the project). |

### Lua API tab

Lists the Lua API surface of a **running** O3DE Editor or Game: reflected **Classes**, **EBuses**
and global functions/properties.

| Control | Effect |
|---|---|
| **Refresh** | Starts listening on port 6777 (if needed) and pulls the references from the connected target. |
| **Filter** | Narrows the tree to entries whose name contains the text. |
| Status label | Shows the connection/refresh state, e.g. *Waiting for an O3DE Editor or Game on port 6777...*. |

The data is used by Lua code completion in `.lua` files:

* Typing `MyEbus.` offers `Broadcast`, `Event` and `Queue`.
* Typing `MyEbus.Event.` offers the EBus event names with their parameter hints.
* Typing `MyClass.` offers the class' methods and properties.
* With no receiver typed, class names, EBus names and global function names are offered.

Completion needs no third-party Lua plugin and also fires while a `.lua` file is open as plain
text.

---

## 8. Behaviour worth knowing

* **One command at a time.** Starting a second command while one is running shows
  *"Another O3DE command is still running."* and does nothing else.
* **Confirmation before running.** Every command except **Show Registration** asks for
  confirmation first (unless **Ask before running commands that change files** is off). The
  dialog shows the exact command line that will be executed.
* **"O3DE script not found"** appears as a warning balloon with an **Open Settings** action
  whenever no script could be located.
* **Working directory.** Most actions run with the project root as the working directory
  unless the action has an explicit *Working dir* field.
* **Lua references come from a live target.** They are only available while an O3DE Editor or
  Game built with RemoteTools (`!RELEASE`) is running and connects back on port 6777. Nothing
  is read from disk.

---

## 9. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| *"O3DE script not found"* balloon | Set **o3de script** in settings, or run `o3de register --this-engine`, then press **Detect**. |
| Template dropdown is empty | No templates are registered. Check the **Registry** tab; register the engine's `Templates` folder or run `o3de register -tp <folder>`. |
| *Create Component* lists no component template | The list is filtered to template names containing "component". Register `DefaultComponent` from `<engine>/Templates/DefaultComponent`. |
| Component lands in the wrong folder | The destination is detected by walking up from the folder selected in the project view looking for `gem.json`. Select the right folder first, then edit **Destination** manually. |
| `${GemName}` not replaced in the output | Fill in **Gem name** — replacements are only generated for non-empty values. |
| Command fails with a non-zero exit code | The console contains the CLI's own message; the exact command is printed above it. Re-run it by hand with `Run O3DE Command…` to reproduce. |
| Nothing appears in the Registry tab | `~/.o3de/o3de_manifest.json` is missing or has no array entries. Press **Reload** after fixing it. |
| A registered path cannot be revealed by double click | The path does not exist on this machine (or is not a local directory). |
| Extra arguments break a sub command | **Extra arguments** are inserted for *every* run. Clear the field if a particular command rejects them. |
| Lua API tab stays on *Waiting for an O3DE Editor or Game on port 6777...* | No RemoteTools target is running, or it is a release build (RemoteTools is compiled out of `_RELEASE`). Start a debug/profile Editor or Game. |
| *Address already in use* / listener error in the Lua API tab | Another RemoteTools host owns port 6777 — most likely the standalone O3DE Lua Editor. Close it and press **Refresh**. |
| Lua completion shows nothing | The tab has no data yet (press **Refresh** with a target running), or **Offer Lua code completion** is off in settings. |

---

## 10. Command reference

| Menu entry | `o3de` sub command | Notable flags |
|---|---|---|
| Create Gem… | `create-gem` | `-gp -gn -tn -dn -s --no-register` |
| Create Project… | `create-project` | `-pp -pn -tn --project-id --no-register` |
| Create Component… / Instantiate Template… | `create-from-template` | `-dp -tn -dn -r` |
| Register ▸ This Engine | `register` | `--this-engine` |
| Register ▸ Project/Gem/Template/Folder | `register` | `-pp / -gp / -tp / -es` |
| Enable Gem in Project… | `enable-gem` | `-pp -gp -f` |
| Show Registration | `register-show` | — |
| Run O3DE Command… | any | free form |

`o3de --help` and `o3de <sub command> --help` document the authoritative flag list; the plugin
does not restrict what you can pass through **Run O3DE Command…**.

---

## 11. Known limitations

* There is **no** `o3de create-component` sub command — component creation goes through
  `create-from-template`, which is what the plugin does.
* `create-from-template` does not evaluate `condition` blocks in `template.json` and does not
  append newly generated files to an existing `*_files.cmake`. The plugin reproduces the CLI's
  behaviour instead of silently patching your files; add such entries by hand if you need them.
* Only one O3DE command can run at a time.
* Lua references require a running RemoteTools build of the engine and exclusive use of TCP port
  6777 — the plugin cannot share the port with the standalone Lua Editor.
* The plugin does not watch the manifest for external changes — press **Reload** in the
  Registry tab after editing `o3de_manifest.json` by hand.
