# 熊氏补丁包：墓碑 （Tidy UP: Grave Stone）
Tidy 的 墓碑 补丁模组，主要联动其它常见模组，如 饰品（Curios）、Inventorio，等。

## 关于：墓碑 （Grave Stone）

墓碑，由 [Max Henkel](https://github.com/henkelmax) 制作，玩家每次死亡时都会生成相应的墓碑，保留身上的全部物品。

[github](https://github.com/henkelmax/gravestone)
| [curseforge](https://www.curseforge.com/minecraft/mc-mods/gravestone-mod)
| [modrinth](https://modrinth.com/mod/gravestone-mod)

感谢 [Max Henkel](https://github.com/henkelmax) 以及所有贡献者共同开发并维护这个模组~

## 功能特性
## 新功能：可扩展死亡物品栏 API （Extensible Death Inventory API）

**仅服务端**

允许其它人便捷地为 墓碑 mod 做死亡掉落相关的兼容，特别是捡尸后可以将物品恢复到原来的槽位。

目前内置了 Curios 与 Inventorio 的联动。

## 改善：死亡笔记（讣告） tooltip

**仅客户端**

为死亡笔记（讣告）增加了 tooltip 描述，包括玩家名称、死亡时间、掉落物数量等，并提示玩家可以通过死亡笔记直接恢复物品（需要权限）。
