# Contributing / 贡献指南

Thanks for helping! 这个模组刻意保持“低耦合、可回归”，改动前请先看这几条。

## 构建与测试

```bash
./gradlew build test      # 构建 + 纯数学单元测试
./gradlew runServer       # 专用服务端（run/server）
./gradlew runClientQuick  # 客户端并直连 127.0.0.1:25565
```

需要 JDK 21。Gradle wrapper 已提交，不需要本机装 Gradle。

## 自检命令

服务端开启 RCON 后（`run/server/server.properties` 里 `enable-rcon=true`），可以用
`tools/rcon.ps1` 跑回归：

```powershell
tools/rcon.ps1 -Command "cvtransit selftest"       # 放置站点 + 床 + 村民，触发普查
tools/rcon.ps1 -Command "cvtransit status"         # 所有站点的普查快照
tools/rcon.ps1 -Command "cvtransit simulate"       # 到站结算
tools/rcon.ps1 -Command "cvtransit econ"           # 交易站先扣后铸 / 充值 / 欠债
tools/rcon.ps1 -Command "cvtransit lamp"           # 路灯六条绑定分支
tools/rcon.ps1 -Command "cvtransit board"          # 上车填座规则
tools/rcon.ps1 -Command "cvtransit accident"       # 事故扣款
tools/rcon.ps1 -Command "cvtransit chunks"         # 已加载区块数（检查是否误加载）
tools/rcon.ps1 -Command "cvtransit persist write|check"
```

## 验证发布包

`jar-smoke/` 是一个只加载构建产物（`run/mods/*.jar`）的最小工程，用来确认打包正确
（`mods.toml`、mixin 配置、资源、类都在）。用法见 `jar-smoke/README.md`：

```bash
./gradlew build
cd jar-smoke && mkdir -p run/mods && cp ../build/libs/*.jar run/mods/
../gradlew -p . runServer --no-daemon
# 期望日志：Create: Village Transit 0.1.0 ... / Done (Xs)
```

## 代码约定

- **只有一个 mixin**（`mixin/AbstractContraptionEntityMixin`），用于 Seat 规则的唯一注入点；
  新增 mixin 需要在 PR 里说明为什么没有公开 API 可用。
- 所有面向玩家的数值都走 `config/VTConfig`，不要在代码里写死。
- 任何“可能未加载”的位置，读世界前必须先 `hasChunk` / `isLoaded`（参见 `DESIGN.md` §13.4）。
- 纯计算请放到 `economy/RewardMath` 这类无 Minecraft 依赖的类里，方便加单测。

## 美术资源

贴图是程序化占位，欢迎替换。文件名与尺寸保持 `16x16` RGBA，直接覆盖：

```
src/main/resources/assets/create_village_transit/textures/block/*.png
src/main/resources/assets/create_village_transit/textures/item/*.png
```

重新生成占位图：`powershell -File tools/gen_textures.ps1`、`tools/gen_seat_assets.ps1`。

## 提交

- 一个 PR 一件事，附上你跑过的自检命令与输出。
- 不要提交 `build/`、`run/`、日志（已在 `.gitignore`）。
