# PhotoZen 图禅 📷

<p align="center">
  <img src="https://img.shields.io/badge/Version-1.6.0.001-blue" alt="Version">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Min%20SDK-26-brightgreen" alt="Min SDK">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License">
</p>

> 🧘 让整理照片变成一种享受

**PhotoZen**（图禅）是一款专为摄影爱好者设计的照片整理神器。告别繁琐的相册管理，用最自然的方式筛选你的照片。

---

## ✨ 核心亮点

### 🎴 滑动整理 - 像刷 Tinder 一样筛选照片
- **左右滑 → 保留** 喜欢的照片
- **上滑 → 删除** 不要的照片  
- **下滑 → 待定** 纠结的先放一边
- 丝滑的 Spring 动画 + 触感反馈，整理变成解压游戏
- 支持滑动时直接分类到相册

### 🔍 对比抉择 - 同时对比多张照片
- Light Table 模式，同屏对比 2-6 张
- **同步缩放**：放大一张，其他同步放大到相同位置
- 轻松做出取舍

### 📅 时间线浏览 - 按时间维度整理照片
- 照片自动按时间聚合成事件分组
- 智能分组 / 按天 / 按月 / 按年 四种模式
- 每个分组显示整理进度，一键进入整理
- **全屏预览**：双指缩放、循环切换、底部操作栏

### 🚀 一站式整理 - 沉浸式多阶段工作流
- **动态流程**：筛选 → 对比待定 → 分类到相册 → 清理回收站 → 胜利
- 智能跳过无照片的阶段
- 连击系统让你越整理越上瘾
- 胜利动画庆祝你的成果

### 📁 相册分类 - 边整理边归档
- 我的相册管理（气泡图 / 列表视图）
- 滑动时底部显示相册标签，点击即分类
- 支持复制到相册或移动到相册
- 多选后批量添加到相册

### 🏆 成就系统 - 50+ 成就等你解锁
- 从整理新手到传说大师
- 普通 → 稀有 → 史诗 → 传说 → 神话
- 每一步都有惊喜

### ✂️ 无损编辑 - 裁切不伤原图
- 只保存裁切参数，随时恢复
- 虚拟副本：一张照片多种构图
- 导出时才真正处理

---

## 📱 更多功能

- **智能筛选**：按相册、日期范围筛选待整理照片
- **批量管理**：多选后一键操作
- **拖动多选**：长按后拖动批量选择，体验与 Google Photos 一致
- **列数切换**：所有瀑布流支持 1-4 列切换
- **照片状态角标**：左上角显示保留/待定/回收站状态
- **从此张开始筛选**：长按照片可从当前位置开始整理
- **系统分享**：从其他 App 分享照片到 PhotoZen 进行复制或对比
- **回收站**：删除可恢复，彻底删除需确认
- **外部删除同步**：自动检测系统相册中已删除的照片
- **状态栏进度通知**：实时显示每日整理进度
- **桌面小组件**：随时查看今日任务进度

---

## 🛠️ 技术架构

| 技术 | 选型 |
|:---|:---|
| 语言 | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture |
| 依赖注入 | Hilt |
| 异步 | Coroutines + Flow |
| 图片加载 | Coil 3 |
| 本地存储 | Room + DataStore |
| 导航 | Compose Navigation (Type-Safe) |

---

## 📚 项目文档

| 文档 | 说明 |
|:-----|:-----|
| [PRD.md](PRD.md) | 产品功能说明文档 |
| [TECH_DESIGN.md](TECH_DESIGN.md) | 技术设计文档 |
| [CHANGELOG.md](CHANGELOG.md) | 版本更新记录 |

---

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1)+
- JDK 17+
- Android SDK 36

### 构建运行
```bash
git clone https://github.com/westcounty/photoZen.git
cd photoZen
./gradlew assembleDebug
./gradlew installDebug
```

### 权限说明
- `READ_MEDIA_IMAGES` (Android 13+) - 读取照片
- `READ_EXTERNAL_STORAGE` (Android 12-) - 读取照片
- `POST_NOTIFICATIONS` (Android 13+) - 状态栏进度通知
- `RECEIVE_BOOT_COMPLETED` - 开机自启动服务

---

## 📋 版本历史

查看完整更新日志：[CHANGELOG.md](CHANGELOG.md)

### v1.6.0.001 (2026-01-18)
🎯 **功能重构与一站式整理升级！**

- **功能精简**：删除标签模块，简化通知设置和首页布局
- **相册分类增强**：添加到相册弹窗、多处入口、拖动批量选择
- **系统分享**：支持从其他 App 分享照片进行复制或对比
- **时间线增强**：全屏预览、照片状态角标
- **一站式整理升级**：动态多阶段工作流（3阶段/4阶段模式）
- **保留照片优化**：快速整理不在相册中的照片
- **相册列表优化**：刷新修复、全局状态筛选、从此张开始筛选

### v1.5.0.007 (2026-01-18)
🔔 **前台服务保活**

- 状态栏实时显示整理进度
- 开机自启动，进度通知可选开关

### v1.4.1.001 (2026-01-18)
📁 **相册分类模式**

- 滑动整理支持相册分类
- 我的相册管理，对比模式优化

### v1.0.0.001 (2026-01-16)
🎉 **第一个正式版本！**

包含所有核心功能：滑动整理、照片对比、无损编辑、一站式整理、成就系统、智能筛选、批量管理等。

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License - 查看 [LICENSE](LICENSE) 了解详情

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/westcounty">westcounty</a>
</p>
