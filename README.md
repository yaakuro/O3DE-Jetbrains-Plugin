# O3DE plugin for CLion

A JetBrains CLion plugin that drives the [Open 3D Engine](https://www.o3de.org/) command line
tool (`o3de.sh` / `o3de.bat`) from inside the IDE.

Everything is implemented from scratch: the plugin does not reuse code from any other plugin in
this repository.

> **Using the plugin?** Read the [user guide](docs/USER_GUIDE.md) — installation, settings,
> every dialog explained field by field, tool window reference and troubleshooting.
> This README covers building and developing the plugin itself.

## Features

| Menu entry | `o3de` command |
|---|---|
| **Create Gem...** | `create-gem` |
| **Create Project...** | `create-project` |
| **Create Component...** | `create-from-template` (component templates) |
| **Instantiate Template...** | `create-from-template` |
| **Register > This Engine** | `register --this-engine` |
| **Register > Project / Gem / Template / External Folder** | `register -pp / -gp / -tp / -es` |
| **Enable Gem in Project...** | `enable-gem` |
| **Show Registration** | `register-show` |
| **Run O3DE Command...** | any sub command, free form |

All actions live under **Tools | O3DE** and are duplicated in the **Project view** context menu
when a directory is selected.

### Output and registry

Every invocation is echoed to the **O3DE** tool window (bottom stripe) with its full rendered
command line, its output and its exit code. The **Registry** tab parses
`~/.o3de/o3de_manifest.json` and lists the registered engines, projects, gems, templates,
external folders, restricted folders and repos; double clicking an entry reveals it in the
project view when it belongs to the open project.

### Lua API references and completion

The **Lua API** tab of the O3DE tool window lists the Classes, EBuses and global functions a
running O3DE target exposes to Lua. Press **Refresh** to (re)read them; the same data drives Lua
code completion for `.lua` files.

The plugin speaks the engine's **RemoteTools** protocol directly (pure Java, no C++ and no
dependency on the Lua Editor). It listens on TCP port **6777** — the same port the standalone
Lua Editor uses — and the running Editor/Game dials out to it. The data is pulled from the
in-engine `ScriptDebugAgent`, so it reflects the exact API of the build that is running
(including every enabled gem). Because the port is exclusive, close the standalone Lua Editor
while the plugin has the listener open.

Completion works without any third-party Lua plugin: the contributor is registered for
`language="ANY"` and filters on the `.lua` extension, so it also works while a `.lua` file is
open as plain text. After `MyEbus.` it offers `Broadcast`, `Event` and `Queue`; after
`MyEbus.Event.` it offers the EBus event names. Class names, EBus names and globals are offered
when no receiver is typed.

### Settings

**Settings | Tools | O3DE**

* **O3DE script** – path to `o3de.sh` / `o3de.bat`. When empty the plugin walks the engines
  registered in the O3DE manifest and picks the one that owns the newest engine.
* **Show tool window on run** – activate the O3DE tool window when a command starts.
* **Refresh VFS after success** – trigger a virtual file system refresh when a command exits 0.
* **Confirm write commands** – ask before running commands that create, register or enable
  objects.
* **Read Lua API references** – listen for a running O3DE target and read the Lua API surface
  from it.
* **Offer Lua code completion** – use those references for `.lua` completion.

## Building

The build is deliberately set up to work **fully offline**, because this environment has no
usable network connection.

```bash
cd o3de-plugin
./gradlew --offline buildPlugin
```

The distribution is written to `build/distributions/o3de-plugin-<version>.zip`.

### Build prerequisites

* **Gradle 8.10** – used through the wrapper (`gradle-8.10-bin` is already present in
  `~/.gradle/wrapper/dists`). If the wrapper distribution is missing, run the cached binary
  directly: `~/.gradle/wrapper/dists/gradle-8.10-bin/*/gradle-8.10/bin/gradle`.
* **`org.jetbrains.intellij` Gradle plugin 1.17.4** – 1.x is the only version fully cached in
  this environment. 2.x cannot be resolved offline, hence the "does not support 242+"
  warning printed by every build; it is a warning, not an error.
* **A local CLion installation** – resolved by `build.gradle` in this order:
  1. `-PclionPath=<path>` on the command line
  2. `O3DE_CLION_PATH` environment variable
  3. `clionPath` in `gradle.properties`
  4. auto detection of `~/.local/share/JetBrains/Toolbox/apps/clion*/` (newest first)

  Only CLion installations with `productCode == "CL"` are considered.
* **A JDK with `javac` 25** – the CLion platform is class file major 69, so compilation must be
  forked onto a Java 25 compiler. The script picks the newest known JDK home that contains a
  working `bin/javac` (CLion's own `jbr` qualifies) and passes `--release 17` so the produced
  classes still target Java 17 bytecode.

### Build settings that must not be reverted

`build.gradle` disables the following tasks/options; re-enabling them breaks the offline build:

* `patchPluginXml.instrumentCode = false`, `instrumentTestCode = false` – instrumentation
  downloads `java-compiler-ant-tasks` from the network.
* `buildSearchableOptions` – also requires a network/IDE round trip.
* `intellij.localPath` instead of `intellij.version` – no artifacts are downloaded.

Run `./gradlew --offline printEnvironment` to see which CLion and `javac` were selected.

## Installing

```bash
./gradlew --offline runIde        # start a sandbox CLion with the plugin
```

or copy `build/distributions/o3de-plugin-1.0.0.zip` into CLion via
**Settings | Plugins | ⚙ | Install Plugin from Disk...**.

## Project layout

```
src/main/java/com/o3de/clion/
├── O3deIcons.java              SVG icon registry
├── actions/                    Every Tools | O3DE action
├── core/
│   ├── O3deCliLocator.java     Finds o3de.sh / o3de.bat
│   ├── O3deCommandLine.java    Builds the argv for every supported sub command
│   ├── O3deManifest.java       Reads ~/.o3de/o3de_manifest.json
│   ├── O3deProjectContext.java Detects whether the open project is a gem or a project
│   └── O3deService.java        Project service: console + process runner
├── remote/                     RemoteTools wire protocol (pure Java)
│   ├── AzCrc32.java            AZ::Crc32 (lower-cased variant used by AZ_CRC_CE)
│   ├── NetworkBuffer.java      Big-endian / bounded-value / AZStd::string codec
│   ├── TcpPacketCodec.java     AzNetworking 5 byte framing + LZ4 decompression
│   ├── Lz4BlockDecompressor.java
│   ├── ObjectStreamReader.java / ObjectStreamWriter.java
│   ├── RemoteToolsHost.java    Listens on 6777, reassembles and routes messages
│   ├── objectstream/           AZ::ObjectStream element tree
│   └── script/                 ScriptDebugAgent client, protocol and result parser
├── lua/                        Lua API reference service, tool window tab and completion
├── settings/                   Settings | Tools | O3DE page
├── toolwindow/                 O3DE tool window (console + registry)
└── ui/                         Dialogs shared by the actions
```

The process runner uses plain `ProcessBuilder` on purpose: the platform execution API
(`CommandLineState`, `ProcessHandler`, ...) changes far more often between platform releases
than `ProcessBuilder` does, and the plugin only needs "run this argv and stream its output".

## Verified `o3de` behaviour

Flags below were verified against `o3de.sh` (O3DE 1.13.2) before being hard coded:

* `-v` is a *sub command* flag – it must follow the sub command, not precede it.
* `create-gem -gp <path> -gn <name> -tn <template> --no-register`
* `create-project -pp <path> -n <name> -tn <template>` (no `--no-register` equivalent)
* `create-from-template -dp <path> -tn <template> -dn <name> -r '${GemName}' <SourceName>`
  – the replacement token is applied to file names *and* file contents. `-r` pairs must come
  last on the command line.
* `enable-gem -pp <project> -gp <gem> [-f]`
* `register --this-engine | -pp <path> | -gp <path> | -tp <path> | -es <path>`
* `register-show`, `get-registered`, `disable-gem`, `edit-gem-properties`, ...

There is **no** `create-component` sub command: component creation is
`create-from-template -tn DefaultComponent|LevelComponent|SystemComponent|...`.

`create-from-template` does not evaluate `condition` blocks in `template.json` and does not
append the generated files to an existing `*_files.cmake`. The plugin deliberately reproduces
that behaviour instead of patching the generated output.

## Known warnings during build

```
Gradle IntelliJ Plugin 1.x does not support building plugins against the IntelliJ Platform 2024.2+
The 'since-build' property is lower than the target IntelliJ Platform major version: 261 < 263.
```

Both are informational. The plugin is compiled against CLion 2026.3 (build 263) while declaring
`since-build="261"` so that it also installs into CLion 2026.1; only platform APIs that exist in
both releases are used.

## License

Licensed under either of

* Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE) or
  http://www.apache.org/licenses/LICENSE-2.0)
* MIT license ([LICENSE-MIT](LICENSE-MIT) or http://opensource.org/licenses/MIT)

at your option.

Copyright (c) 2026 Cengiz Terzibas.
