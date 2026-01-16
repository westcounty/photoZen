# PhotoZen 图禅 📷

<p align="center">
  <img src="https://img.shields.io/badge/Version-1.1.0.018-blue" alt="Version">
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

### 🔍 对比抉择 - 同时对比多张照片
- Light Table 模式，同屏对比 2-4 张
- **同步缩放**：放大一张，其他同步放大到相同位置
- 轻松做出取舍

### 🏷️ 标签气泡 - 谁说管理标签不能好玩？
- 可拖拽的物理气泡图
- 标签越大，照片越多
- 弹性碰撞 + 惯性滑动，位置自动记忆

### ✂️ 无损编辑 - 裁切不伤原图
- 只保存裁切参数，随时恢复
- 虚拟副本：一张照片多种构图
- 导出时才真正处理

### 🚀 心流模式 - 沉浸式一站整理
- 滑动 → 对比 → 打标签 → 完成
- 连击系统让你越整理越上瘾
- 胜利动画庆祝你的成果

### 🏆 成就系统 - 50+ 成就等你解锁
- 从整理新手到传说大师
- 普通 → 稀有 → 史诗 → 传说 → 神话
- 每一步都有惊喜

---

## 📱 更多功能

- **智能筛选**：按相册、日期范围筛选待整理照片
- **批量管理**：多选后一键操作
- **列数切换**：所有瀑布流支持 1/2/3 列切换
- **回收站**：删除可恢复，彻底删除需确认
- **外部删除同步**：自动检测系统相册中已删除的照片

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

---

## 📋 版本历史

查看完整更新日志：[CHANGELOG.md](CHANGELOG.md)

### v1.1.0.018 (2026-01-17)
🚀 **体验优化版本！**

- **每日任务升级**：首页突出显示，成为核心入口
- **快速滑动优化**：彻底解决快速滑动崩溃问题
- **进度显示修复**：分母显示真实总数，不再受限
- **动画流畅度**：恢复丝滑的Spring动画效果
- **桌面小组件**：每日任务进度实时更新
- **默认设置优化**：每日任务默认快速整理模式，默认晚上10点提醒

### v1.0.0.001 (2026-01-16)
🎉 **第一个正式版本！**

包含所有核心功能：滑动整理、照片对比、标签气泡、无损编辑、心流模式、成就系统、智能筛选、批量管理等。

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License - 查看 [LICENSE](LICENSE) 了解详情

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/westcounty">westcounty</a>
</p>
