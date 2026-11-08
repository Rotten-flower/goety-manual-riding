# Build / 构建

Requires Python 3.11 or later and network access for the first build. Use each platform's included Gradle Wrapper.
需要 Python 3.11 及以上，首次构建需联网。各端使用其目录中的 Gradle Wrapper。

| Target / 目标 | Build JDK / 构建 JDK | Game JDK / 游戏 JDK | Project / 工程 |
| --- | --- | --- | --- |
| Forge 1.20.1 | 17 | 17 | repository root / 仓库根目录 |
| Fabric 1.20.1 | 21 | 17 | `fabric-1.20.1/` |
| NeoForge 1.21.1 | 21 | 21 | `neoforge-1.21.1/` |

Fabric's Loom version requires Java 21 to run Gradle; install Java 17 as well for its compile and game toolchain. Set `JAVA_HOME` to the build JDK before running the corresponding commands.
Fabric 的 Loom 需 Java 21 运行 Gradle，同时安装 Java 17 供编译与游戏工具链使用。运行各端命令前，将 `JAVA_HOME` 设为对应构建 JDK。

## Forge 1.20.1

```sh
python tools/prepare-dependencies.py
./gradlew build
```

Windows uses `gradlew.bat`. The reobfuscated JAR is written to `build/libs/`.
Windows 使用 `gradlew.bat`，重混淆后的 JAR 位于 `build/libs/`。

The setup script fetches Goety 2.5.60.6, Curios 5.14.1+1.20.1 and Patchouli 1.20.1-85-FORGE into `libs/` and checks their pinned SHA-512 hashes.
准备脚本将固定版本的依赖下载到 `libs/` 并校验 SHA-512。

## Fabric 1.20.1

```sh
python tools/prepare-dependencies.py --platform fabric
cd fabric-1.20.1
./gradlew build
```

Uses Sh1roCu's Goety-Fabric `0.7.0-forge2.5.58.2+mc1.20.1`, Trinkets 3.7.1 and Forge Config API Port 8.0.2. Loom resolves Fabric Loader 0.18.5 and Fabric API 0.92.12+1.20.1. The setup script exposes Goety's bundled libraries under `libs/nested/` for development runs.
使用 Sh1roCu 的 Goety-Fabric `0.7.0-forge2.5.58.2+mc1.20.1`、Trinkets 3.7.1 和 Forge Config API Port 8.0.2。Loom 获取 Fabric Loader 0.18.5 与 Fabric API 0.92.12+1.20.1。准备脚本将本体内嵌库提取到 `libs/nested/`，用于开发运行。

## NeoForge 1.21.1

```sh
python tools/prepare-dependencies.py --platform neoforge
cd neoforge-1.21.1
./gradlew build
```

Uses Goety 3.2.2, Curios 9.5.1+1.21.1 and Patchouli 1.21.1-93-NEOFORGE with NeoForge 21.1.265.
使用 Goety 3.2.2、Curios 9.5.1+1.21.1、Patchouli 1.21.1-93-NEOFORGE 与 NeoForge 21.1.265。

Each platform writes its installable JAR to its own `build/libs/`. Dependencies are external build inputs and are not bundled in the addon. Fabric and NeoForge reuse the root project's `assets/` and icon; keep that directory structure when building.
各端安装 JAR 位于各自的 `build/libs/`。前置作为外部构建输入，不打入附属模组。Fabric 与 NeoForge 共用根工程的 `assets/` 和图标，构建时保留仓库目录结构。

## Run / 运行

From the target platform's directory:
在目标加载器的工程目录中运行：

```sh
./gradlew runClient
./gradlew runServer
```

The dedicated server requires accepting Minecraft's EULA in `run/server/eula.txt`.
专用服务器需在 `run/server/eula.txt` 中接受 Minecraft EULA。

## Contributing / 参与开发

Build before submitting changes and verify affected behavior in a game instance. Rendering and input changes need actual game checks; server-side changes also need a dedicated-server check.
提交前构建并在游戏中验证受影响行为，输入与渲染修改需实机检查，服务端行为还需专服检查。
