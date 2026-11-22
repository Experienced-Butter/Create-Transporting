# Create: Village Transit / 机械动力：通勤运输

> 状态：**0.1.0 已实现并跑通核心闭环**（构建通过、服务端实测：普查 → 激活 → 旅客生成 → 村民清零失效）。
> 平台：Minecraft 1.21.1 · NeoForge 21.1.x · Create 6.0.10
> 本文档随实现同步维护；所有数值均可在 config 调整。

---

## 1. 项目身份

| 项 | 值 |
|---|---|
| 显示名 | Create: Village Transit / 机械动力：通勤运输 |
| modId | `create_village_transit` |
| 包名 | `com.experiencedbutter.villagetransit` |
| 版本 | 0.1.0 |
| 许可 | MIT |
| 语言 | `en_us` + `zh_cn` 全量（方块、物品、提示、GUI、工具提示）；config 注释英文 |
| Mixin | 无。只用公开 API + 轮询，保证可重建 |

## 2. 依赖

| 依赖 | 坐标 / 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | `[21.1.0,)`，开发用 21.1.267 |
| Create | `com.simibubi.create:create-1.21.1:6.0.10-281:slim`，运行时 `[6.0.10,6.1)` |
| Ponder | `net.createmod.ponder:ponder-neoforge:1.0.92+mc1.21.1` |
| Flywheel | `dev.engine-room.flywheel:flywheel-neoforge-api-1.21.1:1.0.7` |
| 构建 | JDK 21 · Gradle 8.10 · ModDevGradle 2.x（NeoForge 运行时即官方映射，无需 `fg.deobf`） |
| 注册 | NeoForge `DeferredRegister`（不使用 Registrate） |

## 3. 内容清单

| 类型 | id | 说明 |
|---|---|---|
| 方块 | `village_station` | 村庄站点；普查门面、旅客列表、刷新按钮 |
| 方块 | `platform_lamp` | 站台路灯；村庄站点 ↔ 列车站点 双绑定 |
| 方块 ×16 | `passenger_seat_<color>` | 旅客座垫；继承 `SeatBlock`，全部加入 `#create:seats` |
| 方块 | `trade_station` | 交易站；绑定放置者 |
| 物品 | `passenger` | 旅客；不可堆叠；组件存目的/耐心/起点 |
| 物品 ×7 | `copper_coin` … `netherite_coin` | 面额 10 / 10² / … / 10⁷（单位：分） |
| 内部物品 | `seat_occupant_marker` | 玩家占座标记；不可获得 |

### 配方

| 产物 | 类型 | 配料 |
|---|---|---|
| 村庄站点 | 无序 | `create:track_station` + 绿宝石 + 指南针 |
| 站台路灯 | 竖直 3 格 | 灯笼 / 栅栏 / 绿宝石 |
| 旅客座垫 | 无序 ×16 | `create:<color>_seat` + 绿宝石 |
| 交易站 | 无序 | 箱子 + 绿宝石 + 铁锭 |

## 4. 规则

### 4.1 村庄站点

- 放置后进入 `UNBOUND`，首次普查判定村庄有效性。
- 有效性双模（config `checkMode`）：
  - `structure_and_poi`：命中 `#minecraft:village` 的 structure start，且半径内 POI 达标；
  - `structure_only`：命中结构包围盒（外扩 `structureMargin`）即有效。
- 普查口径：**已加载区块内**的成年村民为 V；`profession ∉ {NONE, NITWIT}` 为 E；工作站点 POI 为 W。
- 失败即 `INVALID`，面板显示原因（"不在村庄范围"/"部分区块未加载"）。
- **V = 0 → 站点失效**（`invalidWhenNoVillagers`）。
- 普查间隔：`censusBaseTicks + min(censusMaxTicks, censusPerVillagerTicks × V) + rand(0, censusJitterTicks)`。
- 全块加载守卫：扫描盒内有任一区块未加载 → 放弃本次普查、保留旧数据、退避重试。
- 超过 `staleTimeoutMinutes` 无法普查：`SUSPENDED`；是否停发新旅客由 `stopGeneratingWhenStale` 决定（默认 false，即照旧按旧数据生成）。
- 唯一性：一个村庄只允许一个站点；同维度任意两站间距 ≥ `minStationSeparation`。

### 4.2 站台路灯

- 单方块、3 格高模型。两个绑定槽：A = 村庄站点，B = `create:track_station`。
- 绑定时要求：类型匹配、目标区块已加载、中心距 ≤ `maxLinkDistance`。
- 绑定成功后写入 `LinkTable`（SavedData），世界内显示线框（A 绿 / B 蓝，可关）。
- **村庄站点非 ACTIVE → 灯不工作**（灯灭、GUI 显示原因）；列车站点完全不因绑定改变任何原版行为。

### 4.3 旅客

- 生成：`N = floor(travelChance × V)` + 概率进位，上限 `maxPerUpdate`，容量 `passengerCapacity`，满则丢弃。
- 目的：`u = (V−E)/V`；一次随机分流：`travel` 权重按 `employedWeights/unemployedWeights` 线性混合。
  默认等价于：`P(旅行)=0.30`、`P(搬迁)=0.10+0.50u`、`P(贸易)=0.60−0.50u`。
- 耐心：查表 `patienceWeights`（1..8），默认 `[1,4,7,9,6,3,2,1]`；超耐心 `ultraRareOneIn`，站点已有 `ultraCap` 个则丢弃不重掷；超耐心在站点恒为 10。
- 列表更新：`rand(updateMinTicks, updateMaxTicks)`；顺序：全体耐心−1（超耐心保持 10）→ 清 0 → 生成。
- 物品形态：`passenger` 携带 `PassengerData(purpose, patience, origin, seq, bornTick)`；GUI 悬停显示完整信息。

### 4.4 旅客座垫

- 世界内：100% 原版坐垫行为（坐、摔落、拴绳、染色——在 16 色家族内部互换并保留内容）。
- 动态结构内：虚拟单栏位（只接受旅客 / 玩家标记，不可堆叠）；不注册 `IItemHandler`，避免被 `MountedStorageManager` 并入列车货舱。
- 玩家落座：自定义 `MovingInteractionBehaviour` 拦截；座垫已有旅客时拒绝落座；坐下写入玩家标记，离开时清除（按已落座玩家对账，不扫世界）。
- 列车侧另有 SavedData 台账，保证卸载、事故、拆解时数据完整。

### 4.5 到站流程

`TrainTracker` 每 tick 扫 `Create.RAILWAYS.trains`，检测到站 / 脱轨 / 列车消失：

1. 到站且灯的两个链接有效、村站 ACTIVE；
2. **下车**：全员耐心−1（含超耐心）→ 耐心 0 必下；否则按目的判定：
   - 旅行：`V < travelBelow`
   - 贸易：`V > tradeAbove`
   - 搬迁：`无职业占比 ≤ relocateMaxUnemployedRatio`
   - 其余（含 V 恰好等于阈值）不停，属中转；
3. 结算报酬（见 5.4）；
4. **上车**：村站旅客按 `seq` 顺序填入空座垫，直到满。

### 4.6 事故与拆解

- 触发：`derailed` 跳变、列车从 `RAILWAYS.trains` 消失、拆解。
- 对每个旅客就地结算：距离取"事故点 → 该旅客起点"，全额扣款（`accidentPenaltyFactor`），允许负值。
- 车上旅客清空，台账同步。

### 4.7 经济

- 余额单位 = 分（1 铜币 = 10 分），`SavedData` 存 UUID → long，离线可记。
- 交易站：owner = 放置者；GUI 仅主人可开。
- **先扣款后铸币**：7 个面额槽里的硬币都是已付过钱的；铸币是唯一造币入口。
- 自动取出：默认关，按槽开关；每 `refillIntervalTicks` 最多补 `maxMintPerRefill` 枚/槽。
- 欠债：禁止取出，插入立即抵债。余额上限 `maxBalance`，超额存入退回。

## 5. 公式

### 5.1 普查间隔

```
interval = clamp(base + min(max, perVillager × V) + rand(0, jitter), base, base + max + jitter)
默认：1200 + min(3600, 60×V) + rand(0,600)   // 60s ~ 5min
```

### 5.2 目的权重

```
u = (V - E) / V
P(travel)   = 0.30
P(relocate) = 0.10 + 0.50u
P(trade)    = 0.60 - 0.50u
```
一次随机 + 两次比较；与"逐村民掷骰"期望等价。

### 5.3 距离系数（含死区）

```
distMult = d <= freeDistance ? 1.0 : 1 + cap × (1 - exp(-(d - freeDistance) / distanceK))
默认 freeDistance = 256, distanceK = 256
```

### 5.4 报酬

```
factor   = patienceBase + patiencePerPoint × 耐心        // 0.60 + 0.15×p
reward   = base(目的) × factor × distMult × routeFactor
routeFactor = max(routeFatigueFloor, 1 / (1 + fatigue / routeFatigueK))
每次结算：fatigue = fatigue × 0.5^(Δt / routeFatigueHalfLifeSeconds) + 1   // 无序村庄对
```

### 5.5 事故扣款

```
d      = 事故点 → 旅客起点（跨维度用 crossDimensionDistance）
reward = base(目的) × factor(事故时耐心) × distMult(d)     // 不乘 routeFactor
扣款   = reward × accidentPenaltyFactor
```

## 6. Config（server 端为主，共 63 项，全部可调）

### 6.1 `[village]`

| 键 | 默认 | 说明 |
|---|---|---|
| checkMode | `structure_and_poi` | `structure_and_poi` / `structure_only` |
| censusRadius | 64 | 普查半径（格） |
| structureMargin | 16 | 结构包围盒外扩 |
| minBeds | 3 | 判定有效所需床 POI |
| minWorkstations | 1 | 判定有效所需工作站点 POI |
| requireFullyLoadedCensus | true | 全块加载才普查 |
| censusRetryTicks / censusRetryMaxTicks | 200 / 1200 | 失败退避 |
| staleTimeoutMinutes | 30 | 陈旧超时（分钟） |
| stopGeneratingWhenStale | false | 超时后是否停发旅客 |
| invalidWhenNoVillagers | true | V=0 时站点失效 |
| minStationSeparation | 48 | 站点最小间距 |
| oneStationPerVillage | true | 一村一站 |

### 6.2 `[station]`

| 键 | 默认 | 说明 |
|---|---|---|
| censusBaseTicks | 1200 | 基础检查间隔 |
| censusPerVillagerTicks | 60 | 每村民增量 |
| censusMaxTicks | 4800 | 增量上限 |
| censusJitterTicks | 600 | 随机抖动 |
| passengerCapacity | 64 | 站点旅客容量 |
| refreshButtonCooldownTicks | 20 | 刷新按钮冷却 |
| lampRequiresActiveStation | true | 村站未激活时灯失效 |

### 6.3 `[passenger]`

| 键 | 默认 | 说明 |
|---|---|---|
| travelChance | 0.10 | 每位有效村民的出行概率 |
| maxPerUpdate | 8 | 单次新增上限 |
| updateMinTicks / updateMaxTicks | 1200 / 3600 | 列表更新间隔范围 |
| ultraRareOneIn | 64 | 超耐心出现率 1/N |
| ultraCap | 2 | 站点超耐心上限 |
| patienceWeights | `[1,4,7,9,6,3,2,1]` | 耐心 1..8 的权重 |
| employedWeights | `[0.30,0.10,0.60]` | 有职业：旅行/搬迁/贸易 |
| unemployedWeights | `[0.30,0.60,0.10]` | 无职业：旅行/搬迁/贸易 |

### 6.4 `[preference]`

| 键 | 默认 |
|---|---|
| travelBelow | 15 |
| tradeAbove | 15 |
| relocateMaxUnemployedRatio | 0.25 |

### 6.5 `[reward]`

| 键 | 默认 | 说明 |
|---|---|---|
| baseTravel / baseRelocate / baseTrade | 20 / 10 / 35 | 基准分 |
| patienceBase / patiencePerPoint | 0.60 / 0.15 | 耐心系数 |
| freeDistance / distanceK | 256 / 256 | 距离死区与饱和 |
| capTravel / capRelocate / capTrade | 2.0 / 1.0 / 1.5 | 距离增幅封顶 |
| crossDimensionDistance | 4096 | 跨维度折算距离 |
| accidentPenaltyFactor | 1.0 | 事故扣款系数 |
| maxPayoutPerArrival | 4000 | 单次到站结算上限（0=不限） |
| hourlyCapPerStation | 6000 | 每站每小时兜底（0=关） |
| routeFatigueEnabled | true | 线路疲劳开关 |
| routeFatigueHalfLifeSeconds | 1800 | 疲劳半衰期 |
| routeFatigueK | 4.0 | 疲劳强度 |
| routeFatigueFloor | 0.2 | 疲劳地板 |

### 6.6 `[lamp]` / `[economy]` / `[seat]` / `[debug]` / `[client]`

| 键 | 默认 |
|---|---|
| maxLinkDistance | 30 |
| allowSharedLinks | false |
| maxBalance | 9.0e15 |
| autoWithdrawDefault | false |
| refillIntervalTicks | 20 |
| maxMintPerRefill | 1 |
| maxManualMintPerSlot | 64 |
| ownerOnlyGui | true |
| blockPlayerSeatWhenOccupied | true |
| dropPassengersOnNonTrainDismantle | true |
| logCensus / logTransfers / logEconomy | false |
| showLinkOutlines | true |
| linkOutlineColorVillage / Station | `#2FD27A` / `#4AA3FF` |
| passengerListRows | 6 |
| tooltipVerbose | true |
| particlesOnBoard | true |

## 7. 平衡表（d 为两村直线距离，freeDistance = 256）

单旅客报酬（分；10 分 = 1 铜币）：

| 目的 | 基准 | 耐心 | d=256 | d=512 | d=1024 | d=2048 |
|---|---|---|---|---|---|---|
| 旅行 | 20 | 4 | 24 | 54 | 70 | 72 |
| 搬迁 | 10 | 3 | 11 | 17 | 21 | 21 |
| 贸易 | 35 | 5 | 47 | 92 | 115 | 118 |
| 贸易（超耐心） | 35 | 10 | 74 | 143 | 179 | 184 |

线路收益（8 座车、一村一停、混合目的，d≈512）：约 480 分 ≈ 5 铁币；疲劳地板 0.2 时约 96 分。
32 座大车：约 1900 分/停；疲劳压制后约 380 分。
兜底：每站每小时 6000 分（只防病态，正常节奏碰不到）。

## 8. 美术规格（已定：Create 工业风）

调色板：

| 用途 | 颜色 |
|---|---|
| 黄铜主 / 暗 / 亮 | `#C9A227` / `#8A6B1E` / `#F0D264` |
| 安山岩 亮 / 中 / 暗 | `#8A8A8A` / `#6E6E6E` / `#4F4F4F` |
| 绿宝石 亮 / 中 / 暗 | `#2FD27A` / `#1FBF6A` / `#0E7A42` |
| 黑铁 亮 / 暗 | `#4A4A4A` / `#343434` |
| 灯罩 亮 / 暖 | `#FFD27F` / `#FFB347` |
| 硬币 铜/铁/金/紫晶/钻石/绿宝石/下界合金 | `#C77B4A` / `#C9CDD1` / `#F2C641` / `#9C6BE0` / `#6FE3D8` / `#35D07F` / `#3B3238` |
| 旅客目的色 旅行/搬迁/贸易 | `#4AA3FF` / `#E08A3C` / `#F2C641` |

资产清单（16×16，除 GUI）：

- 方块贴图：`village_station`（顶/侧/底）、`platform_lamp`（灯罩/杆/基座）、`trade_station`
- 座垫：16 色各 1 张（Create 坐垫版式 + 绿宝石包边）
- 物品：`passenger`、7 种硬币
- GUI：3 张面板底图（村庄站点 / 路灯 / 交易站），Create 面板配色
- 模型：`platform_lamp` 3 格高（基座 + 杆 + 灯罩）、座垫继承 Create 坐垫模型

生成方式：程序化 16×16 像素画（确定性调色板 + 形状模板），先出整张预览拼图审风格，再接线；允许用同名 PNG 直接替换。

## 9. 网络包

| 包 | 方向 | 用途 |
|---|---|---|
| `ServerboundStationAction` | C→S | 刷新按钮 / 打开同步 |
| `ClientboundStationData` | S→C | 普查结果 + 旅客列表 + 计时器 |
| `ServerboundLampLink` | C→S | 槽位绑定（槽 + 目标坐标） |
| `ServerboundLampUnlink` | C→S | 解绑 |
| `ClientboundLampData` | S→C | 双槽状态 + 失效原因 |
| `ServerboundTradeAction` | C→S | 取出 / 槽开关 |

## 10. 性能预算

| 动作 | 成本 | 频率 |
|---|---|---|
| 村庄普查 | 0.2–1 ms | 每站 60s–5min，全局每 tick ≤1 站 |
| 旅客更新 | <10 µs | 每站 1–3 min |
| 列车轮询 | O(列车数) | 每 tick |
| 占座对账 | O(在座玩家数) | 每 tick |

## 11. 已知限制

1. 只能统计**已加载区块**内的村民；部分加载时拒绝普查并在 GUI 标注。
2. 依赖 Create 的公开类与字段；6.1 若改签名需小改（用法集中在 3 个适配类）。
3. 钱包与台账是新增存档数据，删档即清零。
4. 不做 Ponder 教学。
5. 交易站管道属于"物理接触"权限，服务器需自行规划放置位置。

## 12. 验收清单

- [x] 合成四条均可制作
- [x] 村庄内放置 → ACTIVE；村庄外 → INVALID 且不生成
- [x] 村民清零 → 失效
- [x] 普查间隔随 V 增长；刷新按钮在完成前置灰（服务端 `refreshInFlight` + GUI 置灰）
- [x] 旅客生成数量/目的/耐心符合公式，超耐心上限 2
- [x] 悬停旅客显示完整信息
- [x] 路灯双槽绑定：超距/未加载/类型错误均拒绝
- [x] 灯绑定未激活村站 → 灯不工作；列车站行为无变化
- [x] 到站先下车后上车，耐心−1 与下车判定正确
- [x] 报酬数值与公式一致；线路疲劳在 GUI 可见（系数未在 GUI 显示，见已知缺口）
- [x] 事故按事故地点结算并允许负余额
- [x] 交易站先扣后铸、欠债禁止取出、自动取出默认关
- [x] 一村一站与最小间距生效
- [x] 卸载后列表继续更新；重载数据一致
- [x] 报酬公式有单元测试（7/7）
- [x] 装有旅客的座垫拒绝玩家落座（mixin 已注入）
- [ ] 真机点击验证（本机没有可驱动该 NeoForge 工程的探针，见 13.2）

## 13. 实现状态（0.1.0）

### 13.1 已完成

| 模块 | 实现 | 实测证据 |
|---|---|---|
| 村庄站点 | 方块、BE、SavedData 记录、节流普查、全块加载守卫、过期重试、刷新按钮 | RCON `cvtransit selftest` → `state=ACTIVE villagers=4 beds=3` |
| 旅客 | `passenger` 物品 + 数据组件、目的/耐心、列表更新、超耐心上限、老化清除 | `travellers=3` / `travellers=5` 随更新增长 |
| 失活 | 村民清零 → INVALID；失败原因写入 `lastReason` 并在面板/`status` 显示 | `/kill @e[type=villager]` + `refresh` → `state=INVALID villagers=0`；村外放置 → `reason=not_in_village_structure` |
| 站台路灯 | 双槽绑定、选择模式、距离/加载/类型/唯一性校验、点灯条件 | 编译 + 服务端加载（交互未真机点击） |
| 旅客座垫 | 16 色、`#create:seats` 标签、染色只在家族内互换 | 资源加载无缺模型 |
| 列车层 | 轮询到站/脱轨/拆解、上下车、耐心衰减、报酬、线路疲劳、每小时兜底、事故倒扣 | 服务端无异常（无列车场景） |
| 经济 | 钱包 SavedData、7 面额硬币、交易站先扣后铸、漏斗能力、主人限定 GUI | 编译 + 服务端加载 |
| 座垫占座拦截 | 单个 mixin 注入 `AbstractContraptionEntity#handlePlayerInteraction`，座位上有旅客时拒绝玩家落座 | 导出转换后的类，确认包含 `handler$…$vt$seatRules` |
| 座垫旅客查看 | 同一注入点：潜行右键有旅客的座垫 → 打开只读旅客面板（`SeatInspectMenu` + `SeatInspectScreen`） | 构建 + 服务端启动无异常；菜单注册无冲突（客户端点击验证仍缺） |
| 玩家占座标记 | 玩家坐下时在台账写入内部"玩家"物品、起身移除；结算时跳过标记、上车时跳过该座 | `/cvtransit markers` → `afterSit=2 afterStand=1 markersNow=1 travellerKept=true` |
| 单元测试 | `RewardMathTest`：距离死区/饱和、耐心、目的基准、疲劳起点与地板、非负、设计表数值 | `gradlew test` → 7/7 通过（0 失败） |
| 到站结算 | `ArrivalPipeline` 串起"下车→封顶→线路疲劳→入账→上车"，`/cvtransit simulate` 用合成台账驱动 | `paid=102 credited=102 balance=102 alighted=2 stayed=1 boarded=2 waiting=0`（2 名候车旅客补上 2 个空座，玩家占用的座位被跳过） |
| 面板信息 | 站点面板新增「本小时已结算 X / Y」与「旅客 3/64」容量显示，随 `StationDataPayload` 下发 | 截图确认渲染与布局，无重叠 |
| 经济不变量 | `/cvtransit econ` 用真实交易站 BE 跑：先扣后铸、存入充值、欠债禁止取出 | `minted=true balanceAfterMint=990 slot=1 balanceAfterDeposit=1010 slotEmpty=true mintWhileInDebt=false` |
| 路灯绑定分支 | `/cvtransit lamp` 跑真实 `LampBinding`：成功 / 重复 / 类型错 / 超距 / 槽位错 / 未加载，并输出未点亮原因码 | `bind=true duplicate=false wrongType=false tooFar=false wrongSlot=false unloaded=false codes=1,2,4`（1=未绑村庄，2=未绑列车站，4=绑定的列车站已消失） |
| 上车规则 | `SeatBoarding` 抽出；`/cvtransit board` 用 4 座（1 座被玩家占）+ 5 名候车旅客驱动 | `boarded=3 onboard=3 waiting=2 seat1Free=true`（按座位序填、跳过玩家座、剩 2 人候车） |
| 事故扣款 | `AccidentPenalty` 抽出；`/cvtransit accident` 用 2 名旅客 + 512 格事故点结算并扣款 | `travellers=2 penalty=150 balanceAfter=-50 negative=true`（允许欠债） |
| 存档持久化 | `/cvtransit persist write` 写入站点/旅客/列车台账/钱包 → `save-all flush` → 重启服务端 → `persist check` | `stationFound=true villagers=21 fixtureTravellers=3 totalTravellers=5 manifest=1 balance=4321`（原有 3 名旅客未丢，重启后又新增 2 名，说明站点仍在运行） |
| 未加载不强加载 | 调度器先 `hasChunk` 再读方块状态；`/cvtransit chunks` 观测 | 远处站点（6000,100,6000）卸载后 `setblock` 探测返回 `Could not set the block`，即该区块保持未加载；区块总数稳定不回升 |
| 边界数值 | 单元测试覆盖 `distanceK=0`、`cap=0`、负耐心、`routeFatigueK` 极小、零基准、百万格距离 | `13/13` 通过，无除零/无溢出 |
| 生成分布 | `TravellerMath` 抽成纯函数并单测：目的权重恒和为 1、旅行恒 30%、有职业/无职业切换、耐心表均值 4.12、头数=floor+概率进位 | `TravellerMathTest` 8/8 通过（含一次测试自身期望写反的修正） |
| 本地化完整性 | `LocalisationTest` 扫描源码里的 `translatable("…")`，与 `en_us.json`/`zh_cn.json` 双向比对 | 2/2 通过；插入一个不存在的 key 后测试立即报 `keys missing from en_us.json: [missing.test.key]`，删除后恢复绿色 |
| 调度器开销 | `/cvtransit load <n>` 造 N 个合成站点记录（未加载区块），`/cvtransit perf` 读最近一 tick 的耗时 | 9 个站点 `12µs`；509 个站点（500 个同 tick 到期）`86µs`，其后稳定 `36µs` |
| 存档体积 | `/cvtransit load 100 64` 造 100 站点 × 64 旅客装满 → `save-all` → 解压量测 | 原始 NBT `12.7KB → 1.55MB`，约 **240 B/旅客**（站点满员 ≈15KB）；清理后回落到 `12.7KB` |
| 网络负载 | `/cvtransit packet <n>` 用真实 `StreamCodec` 编码一次快照并读字节数 | 空站点 46 B；8 名旅客 358 B；64 名 2542 B（≈38 B/旅客）；路灯包 28 B。面板每秒刷新一次，满员时约 2.5 KB/s |
| 发布包自检 | 解压 `build/libs/*.jar` 检查清单与资源 | 69 个 class、37 张 PNG、89 个 JSON；`neoforge.mods.toml` 声明 `create [6.0.10,6.1)` 与 `[[mixins]]`；开发用的 `GuiDemo` 已从 jar 排除（数量 0） |
| 发布包启动 | `jar-smoke/` 独立工程：只把构建出的 jar 放进 `run/mods`（源码集不参与），与用户安装方式一致 | 日志出现 `Create: Village Transit 0.1.0 (create_village_transit)`、mixin 兼容级别 `JAVA_21`、mod 初始化，服务端 `Done (4.545s)` |
| GUI 视觉 | `-Dcvtransit.guiDemo` 让客户端自动轮播四个界面，`tools/capture_window.ps1` 抓窗口（无需合成输入） | 四张截图确认：站点面板/路灯/交易站/座垫旅客面板均正常渲染；据此修掉"左侧信息压住旅客列表"与"座垫面板标题显示原始 lang key"两个问题 |
| 极端配置 | 把 20 项配置推到极值（半径 8、间隔 100 tick、出行率 100%、距离封顶 0、每小时封顶 0 等）后启动并跑全部自检 | 服务端 `Done (1.054s)` 无异常；`simulate=53`（距离系数恒 1 的预期值）、`accident=74`、站点 8 名旅客 |
| 配置 | 63 项，`run/server/config/create_village_transit-server.toml` 生成正常 | 配置文件 225 行 |

**已验证的运行环境**：NeoForge 21.1.219 + Create 6.0.10-281，专用服务端 `Done (1.1s)`，客户端 `runClientQuick` 连接成功、无缺失模型/贴图。

### 13.2 已知缺口（0.1.0 不含）

1. **座垫容器是只读的**：潜行右键可查看座位上的旅客，但不能从座垫里取出/放入旅客（写入路径目前只由到站上下车驱动）。
2. **线路疲劳系数未在 GUI 显示**（数值已参与结算；每小时封顶用量已在面板显示）。
3. **GUI 只验证了渲染，没验证点击**：`-Dcvtransit.guiDemo` + 窗口截图确认四个界面渲染正确，但按钮回调、路灯选方块模式这些交互仍无自动化手段（合成输入在本机会话里被系统拦截）。
4. **列车"接线"未用真实列车跑过**：结算核心（老化、目的判定、报酬、上车顺序）已由 `/cvtransit simulate` 驱动真实代码验证；但 `TrainTracker` 的列车发现、座位扫描、上车填座仍无实机列车证据（Create 没有生成列车的命令，盲搭轨道+转向架不现实）。
5. **美术为程序化占位**：`tools/gen_textures.ps1`、`tools/gen_seat_assets.ps1` 可重生成，欢迎 PR 替换同名 PNG。
6. **mixin 的唯一依赖点**：`AbstractContraptionEntity#handlePlayerInteraction` 若在 Create 6.1 改签名，mixin 会在启动时直接报错（`defaultRequire: 1`），需同步修改。
7. **发布包验证方式**：开发环境无法同时加载源码集与同 id 的 jar，因此用 `jar-smoke/` 子工程把构建产物放进 `run/mods` 单独启动验证；它覆盖加载与注册，但不覆盖游戏内玩法。

### 13.3 开发者命令

```
gradlew build                 # 产出 build/libs/create_village_transit-0.1.0.jar
gradlew runServer             # 专用服务端（run/server）
gradlew runClientQuick        # 客户端并直连 127.0.0.1:25565
tools/rcon.ps1 -Command "cvtransit selftest|status|refresh|simulate|econ|lamp|board|accident|chunks|trains|links|persist write|persist check"
```

`TESTPLAN.md` 是留给真机验收的清单（真实列车、鼠标点击、贴图），每条写了布置步骤、要输入的命令和预期结果。

工程化：`.github/workflows/build.yml` 在 push / PR 时用 JDK 21 跑 `build test` 并上传 jar；
`CONTRIBUTING.md` 写明构建、自检命令、mixin 政策与贴图替换方式。

### 13.4 代码审查记录
按"会不会在未加载时读世界 / 会不会算错钱"两条线过了一遍全部 48 个类，修掉 5 处：

| 位置 | 问题 | 修复 |
|---|---|---|
| `VillageTransitService` | 每 tick 对每个站点调用 `getBlockState`，未加载区块会被同步加载 | 先 `hasChunk`，未加载只跑旅客列表更新 |
| 三个 Menu 的 `stillValid` | 先读方块状态再判距离，玩家远距离传送时可能加载目标区块 | 距离优先，超距直接关闭菜单 |
| `TrainTracker` | 重启后首 tick 把"正停在站里"当成到站，重复结算 | `primed` 标志：首 tick 只记录不触发 |
| `TrainTracker.penaliseById` | 没有缓存位置时用世界原点 (0,0,0) 当事故点，扣款天文数字 | 位置未知按距离 0 计（只收基准价） |
| `TradeStationBlockEntity` | 无主人时硬币被消耗但不入账 | 无主人保留硬币，不消耗 |
| `VillageTransitService` | 普查结果、状态与计时器改动没有调用 `setDirty()`，重启可能丢进度 | 每 tick 有实际改动时统一 `data.setDirty()` |

其余世界访问点（命令、方块交互、菜单构造、路灯校验）都确认在"玩家/命令已在现场"或"先 `isLoaded` 再读"的前提下，没有类似隐患。

