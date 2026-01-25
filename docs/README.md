# PhotoZen 文档中心

> 📅 版本: v2.0.0 | 更新日期: 2026-01-25

欢迎来到 PhotoZen 图禅的文档中心。本目录包含项目的所有技术文档、设计规范和参考资料。

---

## 快速导航

### 核心文档

| 文档 | 说明 | 适合读者 |
|:-----|:-----|:--------|
| [PRD.md](PRD.md) | 产品需求文档 | 产品、设计、开发 |
| [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) | 设计系统规范 | 设计、前端开发 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 技术架构设计 | 后端开发、架构师 |
| [TECH_DESIGN.md](TECH_DESIGN.md) | 技术设计详情 | 全栈开发 |

### 交互与设计

| 文档 | 说明 |
|:-----|:-----|
| [INTERACTION_SPEC.md](INTERACTION_SPEC.md) | 交互设计规范 |
| [GESTURE_SPEC.md](GESTURE_SPEC.md) | 手势交互规范 |
| [LOGO_DESIGN.md](LOGO_DESIGN.md) | Logo 设计规范 |

### 技术指南

| 文档 | 说明 |
|:-----|:-----|
| [STATE_MANAGEMENT.md](STATE_MANAGEMENT.md) | 状态管理指南 |
| [COMPONENT_USAGE.md](COMPONENT_USAGE.md) | 组件使用示例 |

### 开发流程

| 文档 | 说明 |
|:-----|:-----|
| [WORKFLOW_GUIDE.md](WORKFLOW_GUIDE.md) | 复杂任务多层拆解工作流程（AI 协作开发指南） |

### 中文参考文档

| 文档 | 说明 |
|:-----|:-----|
| [评估报告.md](评估报告.md) | 项目评估报告 |
| [设计优化建议.md](设计优化建议.md) | 设计优化建议（已实现） |

---

## 文档结构

```
docs/
├── README.md              # 文档索引（本文件）
├── PRD.md                 # 产品需求文档
├── DESIGN_SYSTEM.md       # 设计系统规范
├── ARCHITECTURE.md        # 技术架构设计
├── TECH_DESIGN.md         # 技术设计详情
├── INTERACTION_SPEC.md    # 交互设计规范
├── GESTURE_SPEC.md        # 手势交互规范
├── STATE_MANAGEMENT.md    # 状态管理指南
├── COMPONENT_USAGE.md     # 组件使用示例
├── WORKFLOW_GUIDE.md      # 复杂任务工作流程指南
├── LOGO_DESIGN.md         # Logo 设计规范
├── 评估报告.md             # 项目评估报告
├── 设计优化建议.md          # 设计优化建议
│
└── archive/               # 归档文档
    ├── planning/          # 规划文档
    │   ├── PLAN_L1_OVERVIEW.md
    │   ├── PLAN_L2_MODULE_*.md
    │   └── PLAN_MASTER_INDEX.md
    └── drafts/            # 草稿文档
        ├── listing_prd.md
        ├── REQUIREMENTS_LISTING.md
        └── REQUIREMENTS_DESIGN_OPTIMIZATION.md
```

---

## 新手入门

### 了解项目

1. **产品功能**: 阅读 [PRD.md](PRD.md) 了解 PhotoZen 的功能和用户价值
2. **设计规范**: 阅读 [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) 了解视觉和动效规范
3. **技术架构**: 阅读 [ARCHITECTURE.md](ARCHITECTURE.md) 了解整体架构

### 开始开发

1. **技术栈**: 查看 [TECH_DESIGN.md](TECH_DESIGN.md) 第 1 节了解技术选型
2. **状态管理**: 阅读 [STATE_MANAGEMENT.md](STATE_MANAGEMENT.md) 了解数据流
3. **组件使用**: 参考 [COMPONENT_USAGE.md](COMPONENT_USAGE.md) 了解 UI 组件

### 设计开发

1. **设计 Token**: 查看 [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) 中的 Token 定义
2. **交互规范**: 阅读 [INTERACTION_SPEC.md](INTERACTION_SPEC.md) 了解交互细节
3. **手势规范**: 阅读 [GESTURE_SPEC.md](GESTURE_SPEC.md) 了解手势实现

---

## 版本历史

### v2.0.0 (2026-01-25) - Flow Release

**功能完成**:
- 完整实现 67 项功能需求 (REQ-001 ~ REQ-067)
- 实现 33 项设计优化 (DES-001 ~ DES-033)

**设计系统**:
- 统一设计 Token 系统 (PicZenTokens)
- 统一动效系统 (PicZenMotion)
- 操作色彩系统 (PicZenActionColors)
- 6 级表面层次 (PicZenDarkSurfaces)

**动画优化**:
- 列表错落入场动画
- 页面过渡动画系统
- 按压微交互效果
- 滑动卡片倾斜透视
- 动态阴影效果
- 弹性回弹动画

**震动反馈**:
- 方向感知震动反馈
- 临界点震动提示
- 操作完成震动确认

### v1.6.0 (2026-01-18)

- 基础功能完成
- 滑动整理
- 相册管理
- 成就系统

---

## 代码仓库

- **主分支**: `main` - 所有非 AI 功能开发
- **实验分支**: `explore/smart-gallery` - AI 相关功能

---

## 文档维护指南

### 更新原则

1. **及时更新**: 代码变更后及时更新相关文档
2. **版本同步**: 文档版本号与代码版本保持一致
3. **交叉引用**: 相关文档之间保持正确的链接

### 文档规范

1. 使用 Markdown 格式
2. 中英文混排时添加空格
3. 代码块指定语言类型
4. 表格对齐使用 `:` 语法

### 归档规则

- 规划文档完成后移至 `archive/planning/`
- 草稿文档完成后移至 `archive/drafts/`
- 过时文档添加归档标注

---

## 联系方式

如有问题或建议，请通过以下方式联系：

- GitHub Issues: 提交 Bug 或功能建议
- Pull Request: 提交文档改进

---

*本文档由 Claude Code 自动生成和维护*
