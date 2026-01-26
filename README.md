# PhotoZen 图禅

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Compose">
  <img src="https://img.shields.io/badge/Min%20SDK-26-brightgreen" alt="Min SDK">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License">
</p>

> 让整理照片变成一种享受

**PhotoZen**（图禅）帮你轻松整理手机里堆积如山的照片。把「筛选-分类-删除」变成一件有趣的事，不管你是摄影爱好者还是拍照记录生活的普通人。

---

## 核心功能

### 滑动整理
像刷短视频一样筛选照片：
- **左右滑** → 保留喜欢的照片
- **上滑** → 删除不要的照片
- **下滑** → 待定纠结的先放一边
- 丝滑动效：弹性回弹、倾斜透视、动态阴影
- 方向震动：保留轻震、删除强震、待定双击
- 滑动时可直接分类到相册

### 照片对比 (Light Table)
同屏对比多张照片，轻松做出取舍：
- 支持 2-6 张同时对比
- **同步缩放**：放大一张，其他同步放大到相同位置
- 快速标记保留或删除

### 时间线浏览
按时间维度整理照片：
- 照片自动按时间聚合成事件分组
- 智能分组 / 按天 / 按月 / 按年 四种模式
- 每个分组显示整理进度，一键进入整理

### 一站式整理
沉浸式多阶段工作流：
- 动态流程：筛选 → 对比待定 → 分类到相册 → 清理回收站
- 智能跳过无照片的阶段
- 连击系统让你越整理越上瘾
- 胜利动画庆祝你的成果

### 相册分类
边整理边归档：
- 我的相册管理（气泡图 / 列表视图）
- 滑动时底部显示相册快捷标签
- 支持复制到相册或移动到相册
- 多选后批量添加到相册

### 成就系统
50+ 成就等你解锁：
- 从整理新手到传说大师
- 普通 → 稀有 → 史诗 → 传说 → 神话

### 全屏预览
沉浸式查看照片：
- 双指缩放、循环切换
- 左上角显示拍摄信息和地理位置
- 底部快捷操作：复制、分享、编辑、删除
- 上下滑动退出

---

## 更多特性

- **智能筛选**：按相册、日期范围筛选待整理照片，支持保存筛选预设
- **批量管理**：长按进入选择模式，拖动批量选择
- **统一手势**：所有列表点击查看大图、长按多选，体验一致
- **列数切换**：瀑布流支持 2-5 列切换
- **照片状态角标**：左上角显示保留/待定/回收站状态
- **从此张开始筛选**：选择模式下可从当前位置开始整理
- **系统分享**：从其他 App 分享照片到 PhotoZen 进行复制或对比
- **回收站**：删除可恢复，彻底删除需确认
- **外部删除同步**：自动检测系统相册中已删除的照片
- **状态栏进度通知**：实时显示每日整理进度
- **桌面小组件**：随时查看今日任务进度
- **整理统计**：日历热力图追踪整理习惯
- **新手引导**：首次使用显示操作引导

---

## 技术架构

| 层级 | 技术选型 |
|:---|:---|
| 语言 | Kotlin 2.0 |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构模式 | MVVM + Clean Architecture |
| 依赖注入 | Hilt |
| 异步处理 | Coroutines + Flow |
| 图片加载 | Coil 3 |
| 本地存储 | Room (SQLite) + DataStore (Preferences) |
| 导航 | Compose Navigation (Type-Safe Routes) |

### 项目结构

```
app/src/main/java/com/example/photozen/
├── data/                   # 数据层
│   ├── local/              # Room 数据库、DAO、Entity
│   ├── repository/         # 数据仓库
│   └── source/             # 数据源 (MediaStore)
├── domain/                 # 领域层
│   ├── model/              # 领域模型
│   └── usecase/            # 用例
├── navigation/             # 导航配置
├── service/                # 后台服务
├── ui/                     # UI 层
│   ├── components/         # 可复用组件
│   ├── screens/            # 页面
│   ├── state/              # 状态管理
│   └── theme/              # 主题配置
└── util/                   # 工具类
```

---

## 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
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
| 权限 | 用途 | Android 版本 |
|:---|:---|:---|
| `READ_MEDIA_IMAGES` | 读取照片 | 13+ |
| `READ_EXTERNAL_STORAGE` | 读取照片 | 12 及以下 |
| `MANAGE_EXTERNAL_STORAGE` | 复制/移动照片到相册 | 11+ |
| `POST_NOTIFICATIONS` | 状态栏进度通知 | 13+ |
| `RECEIVE_BOOT_COMPLETED` | 开机自启动服务 | 全版本 |

---

## 项目文档

| 文档 | 说明 |
|:-----|:-----|
| [CHANGELOG.md](CHANGELOG.md) | 版本更新记录 |
| [PRD.md](PRD.md) | 产品需求文档 |
| [TECH_DESIGN.md](TECH_DESIGN.md) | 技术设计文档 |

---

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License - 查看 [LICENSE](LICENSE) 了解详情

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/westcounty">westcounty</a>
</p>
