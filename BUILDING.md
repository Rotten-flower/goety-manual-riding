# Build / 构建

Requires Java 17, Python 3.11 or later, and network access for the first build.
需要 Java 17、Python 3.11 及以上，首次构建需联网。

```sh
python tools/prepare-dependencies.py
./gradlew build
```

Windows uses `gradlew.bat`. The reobfuscated JAR is written to `build/libs/`.
Windows 使用 `gradlew.bat`，重混淆后的 JAR 位于 `build/libs/`。

The setup script fetches Goety 2.5.60.6, Curios 5.14.1+1.20.1 and Patchouli 1.20.1-85-FORGE into `libs/` and checks their pinned SHA-512 hashes. These dependencies are not bundled in the addon.
准备脚本下载并校验固定版本的依赖，不将它们打入附属模组。

## Run / 运行

```sh
./gradlew runClient
./gradlew runServer
```

The dedicated server requires accepting Minecraft's EULA in `run/server/eula.txt`.
专用服务器需在 `run/server/eula.txt` 中接受 Minecraft EULA。

## Contributing / 参与开发

Build before submitting changes and verify affected behavior in a game instance. Rendering and input changes need actual game checks; server-side changes also need a dedicated-server check.
提交前构建并在游戏中验证受影响行为，输入与渲染修改需实机检查，服务端行为还需专服检查。
