# Create: Village Transit 0.1.1

**Villages become passenger stations for Create trains.**

用 Create 的列车把村民当作旅客运输：村庄站点汇总旅客，站台路灯绑定村庄与车站，旅客座垫载客，
到站按里程与耐心结算报酬，交易站存取货币，事故与拆解从车主账户扣款。

---

## English

### Summary (store listing)

Create addon that turns villages into passenger stations: travellers accumulate at a village
station, platform lamps link a village to a train station, passenger seats carry them, arrivals
pay the train owner, and accidents charge the owner.

### Dependencies

| Mod | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.0 or newer |
| Create | 6.0.10 or newer (6.0.x) |

Nothing else is required. JEI is optional and picks up the crafting recipes automatically.

### Installation

1. Install NeoForge 21.1 for Minecraft 1.21.1.
2. Put `create-1.21.1-6.0.10-*.jar` into `mods/`.
3. Put `create_village_transit-0.1.1.jar` into `mods/`.
4. Launch the game. Everything is configured in `config/create_village_transit-server.toml`
   (63 options) or in the in-game config screen.

### Highlights

- **Village Station** – surveys the village on a throttle, keeps a list of travellers, manual refresh.
- **Platform Lamp** – binds one village station and one train station within 30 blocks, outlines the
  two blocks for 5 seconds after opening its screen, lights up only while the village is active.
- **Passenger Seats (16 colours)** – Create seats in the world; on a train they hold one traveller
  each, block players from sitting, and show a villager whose outfit matches the traveller's purpose.
- **Boarding & payouts** – travellers alight by patience and purpose, the owner is paid per arrival
  with distance curve, route fatigue, per-arrival and hourly caps; trips under 256 blocks pay nothing.
- **Trade Station** – deposit/withdraw mode with the player inventory shown; conversion only happens
  while the block is powered by redstone; seven coin denominations from 10 to 10,000,000 cents.
- **Accidents** – derailment or dismantling clears the travellers and charges the owner (debt allowed).

### Known limitations

- The train integration was exercised with synthetic manifests and a real train driven by hand; a
  full automated train test does not exist.
- Textures are hand-made placeholders, contributions welcome.
- Ponder scenes are not shipped in this version.

### Licence

MIT. Contributions welcome — see `CONTRIBUTING.md`.

---

## 中文

### 简介（商店页用）

把村庄变成列车的客运站：村庄站点持续汇总旅客，站台路灯把村庄与车站绑定，旅客座垫负责载客，
到站后按里程与耐心给车主结算报酬，发生事故则从车主账户扣款。

### 前置依赖

| 模组 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.0 或更高 |
| Create（机械动力） | 6.0.10 或更高的 6.0.x |

不需要其他依赖。JEI 可选，装上后会自动索引本模组的配方。

### 安装

1. 为 1.21.1 安装 NeoForge 21.1；
2. 把 Create 6.0.10 的 jar 放进 `mods/`；
3. 把 `create_village_transit-0.1.1.jar` 放进 `mods/`；
4. 启动游戏。所有配置在 `config/create_village_transit-server.toml`（63 项），也可以在游戏内配置界面调整。

### 主要内容

- **村庄站点**：按间隔普查村庄并维护旅客名单，支持手动刷新；
- **站台路灯**：在 30 格内绑定一个村庄站点和一个列车站点，打开界面后 5 秒内会画出两个绑定方块的轮廓，村庄激活时才会亮；
- **旅客座垫（16 色）**：世界里就是 Create 的座垫；在列车上每个座垫承载一名旅客、玩家不能坐，并会显示一名服饰与出行目的对应的村民；
- **上下车与报酬**：旅客按耐心与目地下车，车主按到站结算，含距离曲线、线路疲劳、单次与每小时封顶；不足 256 格的短途不计收入；
- **交易站**：放入/取出两种模式并显示玩家背包，只有红石充能时才转换；七种硬币（10 到 10,000,000 分）；
- **事故**：脱轨或拆解会清空旅客并从车主账户扣款（允许欠债）。

### 已知限制

- 列车集成用合成台账和手动驾驶的真实列车验证过，没有自动化列车测试；
- 贴图为手工占位，欢迎 PR 替换；
- 本版本不含思索（Ponder）场景。

### 许可

MIT，欢迎贡献，见 `CONTRIBUTING.md`。
