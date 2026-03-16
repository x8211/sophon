# CPU监控功能说明

本项目提供了两种不同的CPU监控功能，分别使用不同的ADB命令获取数据。

## 包结构

CPU监控功能分为三个子包：

### 1. common - 公共功能
存放两种监控方式共享的代码和数据模型。

**包路径**: `sophon.desktop.feature.systemmonitor.feature.cpu.common`

**主要内容**:
- `domain/model/ThreadCpuInfo.kt` - 线程CPU使用信息数据模型（被dumpsys和realtime共享）

### 2. dumpsys - Dumpsys监控功能
基于 `adb shell dumpsys cpuinfo` 命令的CPU监控功能。

**包路径**: `sophon.desktop.feature.systemmonitor.feature.cpu.dumpsys`

**主要文件**:
- `domain/model/CpuMonitorData.kt` - 数据模型
- `domain/repository/CpuRepository.kt` - Repository接口
- `domain/usecase/GetCpuDataUseCase.kt` - UseCase
- `data/repository/CpuRepositoryImpl.kt` - Repository实现
- `ui/CpuScreen.kt` - UI界面
- `ui/CpuViewModel.kt` - ViewModel

### 3. realtime - 实时监控功能
基于 `adb shell top` 命令的实时CPU监控功能。

**包路径**: `sophon.desktop.feature.systemmonitor.feature.cpu.realtime`

**主要文件**:
- `domain/model/RealtimeCpuData.kt` - 数据模型
- `domain/repository/RealtimeCpuRepository.kt` - Repository接口
- `domain/usecase/GetRealtimeCpuDataUseCase.kt` - UseCase
- `data/repository/RealtimeCpuRepositoryImpl.kt` - Repository实现
- `ui/RealtimeCpuScreen.kt` - UI界面
- `ui/RealtimeCpuViewModel.kt` - ViewModel

## 功能对比

## 1. CPU监测 (Dumpsys) - 基于 `dumpsys cpuinfo`

### 特点
- **数据来源**: `adb shell dumpsys cpuinfo`
- **数据类型**: 统计数据（一段时间内的平均值）
- **更新频率**: 较慢（系统统计周期）
- **数据内容**: 
  - CPU负载信息（1分钟、5分钟、15分钟平均负载）
  - 统计时间范围
  - 进程CPU使用详情（包含用户态、内核态、页错误等）
  - 系统整体CPU使用率

### 适用场景
- 需要查看一段时间内的CPU使用趋势
- 需要详细的进程CPU统计信息
- 需要页错误（page faults）等高级指标

## 2. 实时CPU监测 (Top) - 基于 `top` 命令

### 特点
- **数据来源**: `adb shell top -n 1 -b -m 10`
- **数据类型**: 实时快照数据（当前瞬时状态）
- **更新频率**: 快速（每次调用获取最新数据）
- **数据内容**:
  - 任务统计（总数、运行中、睡眠中、停止、僵尸进程）
  - 内存和Swap信息
  - 系统实时CPU使用率（包含用户态、系统态、空闲、IO等待等）
  - Top 10进程的实时CPU使用情况

### 适用场景
- 需要查看当前瞬时的CPU使用情况
- 需要快速刷新的实时监控
- 需要查看进程状态（运行/睡眠/停止/僵尸）
- 需要内存和Swap信息

## 共享功能

### 线程监控
两种CPU监控功能都支持点击进程查看该进程的所有线程CPU使用情况。

- **实现方式**: 使用 `adb shell top -H -p <pid> -n 1 -b` 命令
- **功能特性**:
  - 持续监测（每2秒自动刷新）
  - 按CPU使用率降序排序
  - 显示线程ID、线程名称、CPU使用率等信息
- **代码复用**: 
  - 公共数据模型: `sophon.desktop.feature.systemmonitor.feature.cpu.common.domain.model.ThreadCpuInfo`
  - dumpsys和realtime的Repository接口都包含 `getProcessThreads(pid: Int)` 方法

## 使用方式

在系统监控主界面（`SystemMonitorScreen`）中，通过Tab切换：
- **"CPU监测(Dumpsys)"** - 使用dumpsys cpuinfo的统计数据
- **"实时CPU监测(Top)"** - 使用top命令的实时数据

## 技术架构

### 包结构设计
```
cpu/
├── common/          # 公共功能
│   └── domain/
│       └── model/   # 共享数据模型（如ThreadCpuInfo）
├── dumpsys/         # Dumpsys监控功能
│   ├── data/
│   ├── domain/
│   └── ui/
└── realtime/        # 实时监控功能
    ├── data/
    ├── domain/
    └── ui/
```

### MVVM架构
dumpsys和realtime两个功能都遵循MVVM架构：
```
ui (Screen + ViewModel)
  ↓
domain (UseCase + Repository接口 + Model)
  ↓
data (Repository实现)
  ↓
common (共享的数据模型和工具)
```

## 命令对比

| 特性 | Dumpsys | Top |
|------|---------|-----|
| 命令 | `adb shell dumpsys cpuinfo` | `adb shell top -n 1 -b -m 10` |
| 数据类型 | 统计数据 | 实时快照 |
| 时间范围 | 一段时间的平均值 | 当前瞬时值 |
| 进程数量 | 所有进程 | Top 10进程 |
| 详细程度 | 高（包含页错误等） | 中（基本CPU和内存信息） |
| 更新速度 | 慢 | 快 |
| 内存信息 | 无 | 有 |
| 任务统计 | 无 | 有 |

## 注意事项

1. **权限要求**: 两种功能都需要ADB调试权限
2. **性能影响**: 实时监控由于频繁调用top命令，可能对设备性能有轻微影响
3. **数据准确性**: 
   - Dumpsys提供的是统计数据，更适合分析趋势
   - Top提供的是瞬时数据，更适合实时监控
