<div align="center">

![PonyRace Logo](https://pic.eqad.fun/EQAD_Web/pr_FULL15x_T.png)

## PonyRace - 小马种族

</div>

## 简介

这个插件可以让玩家在游戏内扮演MLP种族，每个种族都有单独的技能、数值和饮食习惯

注：史山，请捏住鼻子查看源代码

## 其他

### 命令
- `/pr set <种族> [玩家]` 设置种族
- `/pr info [玩家]` 查看种族信息
- `/pr reload` 重载配置文件
- `/pr gui [玩家]` 打开种族选择菜单

### 权限节点
- `ponyrace.admin` 管理员命令

### PAPI变量
- `%ponyrace_race%` 玩家种族
- `%ponyrace_stamina%` 玩家体力值
- `%ponyrace_mana%` 玩家魔力值
- `%ponyrace_enrage%` 玩家发怒值

### 配置文件

```yaml
# PonyRace配置文件
# 若无特殊标注，配置文件内所有数值皆需填入正整数

messages:
  # 插件消息前缀
  prefix: "&8[&bPonyRace&8]&r "
  # 关于插件页面的使用文档链接
  about-url: "https://www.eqad.fun/wiki/ponyrace"

settings:
  selection:
    # 是否在无种族玩家加入时显示种族选择菜单
    show: true
    # 是否使用效率更低的方式来兼容登录插件
    # 如果种族选择窗口被登录插件卡没，请启用此选项
    # 该选项启用后会覆盖“强制无种族玩家选择种族”选项
    login-plugin-support: false
    # 是否强制无种族玩家选择种族
    force: true
  consume:
    # 左键技能魔力消耗量
    mana-left: 20
    # 右键技能魔力消耗量
    mana-right: 50
    # 飞行时每秒体力消耗量
    stamina: 1
    # 冲刺技能体力消耗量
    stamina-boost: 20
    # 玩家飞行速度超过阈值后每增加1速度所额外消耗的体力（可小数）
    stamina-move: 0.5
    # 最大额外消耗体力
    stamina-move-max: 3
    # 飞行速度阈值（方块/秒）
    stamina-move-threshold: 8
    # 发怒时每0.25秒怒气消耗量
    enrage: 3
  regen:
    # 每秒魔力恢复量
    mana: 1
    # 每秒体力恢复量
    stamina: 2
    # 每0.25秒怒气恢复量
    enrage: 1
  cooldown:
    # 魔法技能冷却（秒）
    mana: 1
    # 冲刺技能冷却（秒）
    boost: 1
    # 吃食物/宝石冷却，不影响原版食物食用速度（秒）
    eat: 1
    # 龙族火球技能冷却（秒）
    dragon-fire: 30
    # 在体力耗尽后需要恢复到多少体力才能继续飞行
    fly: 20
    # 发怒所需要的最低怒气值
    enrage: 20
  skill:
    # 独角兽最大传送距离（方块）
    max-teleport-length: 100
    # 独角兽激光射程（方块）
    max-laser-length: 60
    # 独角兽激光伤害
    laser-damage: 12
```

