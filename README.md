# PicZen 图禅 📷

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose" alt="Compose">
  <img src="https://img.shields.io/badge/Min%20SDK-24-brightgreen" alt="Min SDK">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License">
</p>

> 🧘 高效的照片整理工具，让照片管理更轻松

PicZen（图禅）是一款专为摄影爱好者设计的 Android 照片整理应用。采用 Tinder 风格的滑动交互，让照片分类变得有趣且高效。

## ✨ 功能特色

### 🎴 Flow Sorter - 滑动整理
- **左滑/右滑 → 保留** 标记为喜欢的照片
- **上滑 → 删除** 移入回收站
- **下滑 → 待定** 滑入待定池，稍后在 Light Table 中对比
- 点击照片进入**全屏查看**，支持**双指缩放**
- 丝滑的 Spring 动画和触觉反馈
- **纯手势操作** - 无底部按钮干扰，沉浸式体验

### 🔍 Light Table - 照片对比
- 同时对比 2-4 张待定照片
- **同步缩放** - 缩放一张照片时，其他照片同步缩放到相同位置
- 快速决定保留或删除

### ✂️ 非破坏性编辑
- **智能裁切** - 只保存裁切参数，不修改原始文件
- **虚拟副本** - 为同一张照片创建多个版本，不占用额外存储空间
- **副本预览** - 全屏预览虚拟副本，左右滑动切换，裁切范围可视化
- **图片导出** - 将虚拟副本导出为新图片，保存到相册 PicZen 文件夹
- 随时可以恢复原始状态

### 🗂️ 照片管理
- 按状态查看照片（保留/待定/回收站）
- 长按照片快速切换状态或编辑
- 回收站支持多选和系统级彻底删除

### 🚀 Flow 工作流
- **沉浸式整理** - 一键进入 Flow 隧道，顺序完成：滑动 → 对比 → 胜利
- **连击系统** - 快速滑动累积 Combo，视觉反馈和触感随等级变化
- **胜利庆祝** - 完成整理后展示统计数据和动画效果

### 🏷️ 标签气泡
- **物理模拟** - 可拖拽的气泡图，标签大小反映照片数量
- **层级浏览** - 支持父子标签结构，点击进入下一层
- **快速创建** - 可视化添加和管理标签

### 🏆 成就系统
- 累计整理数成就徽章
- 整理进度永久记录，不会因删除照片而减少

## 🛠️ 技术架构

### Tech Stack
- **语言**: Kotlin 2.0
- **UI**: Jetpack Compose + Material 3
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt (Dagger)
- **异步**: Kotlin Coroutines + Flow
- **图片加载**: Coil 3
- **本地存储**: Room Database + DataStore
- **导航**: Compose Navigation (Type-Safe)

### 项目结构
```
app/src/main/java/com/example/photozen/
├── data/                    # 数据层
│   ├── local/              # Room 数据库
│   │   ├── dao/           # DAO 接口
│   │   ├── entity/        # 实体类
│   │   └── converter/     # 类型转换器
│   ├── model/              # 数据模型
│   ├── repository/         # 仓库实现
│   ├── source/             # 数据源 (MediaStore)
│   └── util/               # 工具类 (ImageSaver)
├── di/                      # Hilt 依赖注入模块
├── domain/                  # 领域层
│   └── usecase/            # 用例
├── navigation/              # 导航配置
└── ui/                      # UI 层
    ├── components/         # 通用组件
    │   └── bubble/        # 气泡图组件
    ├── screens/            # 各屏幕
    │   ├── flowsorter/    # 滑动整理
    │   ├── lighttable/    # 照片对比
    │   ├── editor/        # 照片编辑
    │   ├── photolist/     # 照片列表
    │   ├── trash/         # 回收站
    │   ├── home/          # 首页
    │   ├── settings/      # 设置
    │   ├── workflow/      # Flow 工作流
    │   └── tags/          # 标签气泡
    ├── util/               # UI 工具
    └── theme/              # 主题配置
```

## 📱 截图

| 首页 | 滑动整理 | 照片对比 | 照片编辑 |
|:---:|:---:|:---:|:---:|
| 整理进度统计 | Tinder 风格滑动 | 同步缩放对比 | 非破坏性裁切 |

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17+
- Android SDK 34
- Gradle 8.13

### 构建运行
```bash
# 克隆仓库
git clone https://github.com/westcounty/photoZen.git
cd photoZen

# 构建 Debug APK
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

### 权限说明
- `READ_MEDIA_IMAGES` (Android 13+) - 读取设备照片
- `READ_EXTERNAL_STORAGE` (Android 12 及以下) - 读取设备照片

## 📋 更新日志

### V1.1.1 (2025-01-14)
- 🎯 **手势优化** - 全新手势映射：左右滑=保留，上滑=删除，下滑=待定（带下沉动效）
- 🚫 **移除底部按钮** - 纯手势操作，沉浸式体验
- ⚡ **滑动流畅度提升** - Combo 动画优化，不阻塞下一张照片操作
- 🔧 **修复状态栏重叠** - Flow 模式顶栏正确处理系统状态栏
- 🗑️ **移除足迹地图** - 暂时移除 GPS 扫描功能，后续重新实现

### V1.1
- Flow 工作流隧道（沉浸式整理体验）
- 连击系统（Combo 视觉与触感反馈）
- 胜利庆祝屏幕
- 标签气泡图（物理模拟）
- 标签层级结构

### V1.0
- Flow Sorter 滑动整理
- Light Table 照片对比
- 非破坏性裁切
- 虚拟副本管理
- 回收站管理
- 成就系统

## 📋 开发计划

- [x] Flow Sorter 滑动整理
- [x] Light Table 照片对比
- [x] 非破坏性裁切
- [x] 虚拟副本管理
- [x] 回收站管理
- [x] 成就系统
- [x] Flow 工作流
- [x] 标签气泡系统
- [ ] 足迹地图（重新实现）
- [ ] 照片标签批量管理
- [ ] 云端同步

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/westcounty">westcounty</a>
</p>
