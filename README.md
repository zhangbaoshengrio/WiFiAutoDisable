# WiFi Auto Disable

**English** | [简体中文](#wifi-弱信号自动关闭)

---

An Xposed module that automatically disables WiFi when the signal strength drops below a configurable threshold.

## Requirements

- Android 8.1+
- [LSPosed](https://github.com/LSPosed/LSPosed) or EdXposed framework

## Installation

1. Download the APK from [Releases](https://github.com/zhangbaoshengrio/WiFiAutoDisable/releases)
2. Install the APK
3. Open LSPosed → Modules → Enable **WiFi Auto Disable**
4. Set scope to **System UI** (`com.android.systemui`)
5. Reboot your device
6. Open the app to configure settings

## Features

- Automatically disables WiFi when RSSI drops below the threshold
- Configurable threshold from -90 dBm to -50 dBm (default: -75 dBm)
- 5-minute cooldown between disables to prevent rapid toggling
- Displays current WiFi signal strength
- Chinese / English language support

## Signal Strength Reference

| RSSI | Signal Quality |
|------|----------------|
| > -50 dBm | Excellent |
| -50 ~ -60 dBm | Good |
| -60 ~ -75 dBm | Fair |
| -75 ~ -85 dBm | Weak |
| < -85 dBm | Very weak |

## Screenshots

> Module status, threshold slider, and language selector in one screen.

## License

[MIT](LICENSE)

---

# WiFi 弱信号自动关闭

[English](#wifi-auto-disable) | **简体中文**

---

一个 Xposed 模块，当 WiFi 信号强度低于设定阈值时自动关闭 WiFi。

## 环境要求

- Android 8.1+
- [LSPosed](https://github.com/LSPosed/LSPosed) 或 EdXposed 框架

## 安装步骤

1. 从 [Releases](https://github.com/zhangbaoshengrio/WiFiAutoDisable/releases) 下载 APK
2. 安装 APK
3. 打开 LSPosed → 模块 → 启用 **WiFi 弱信号自动关闭**
4. 作用域选择 **系统界面** (`com.android.systemui`)
5. 重启设备
6. 打开 APP 进行设置

## 功能

- 当 WiFi 信号（RSSI）低于阈值时自动关闭 WiFi
- 阈值可在 -90 dBm 到 -50 dBm 之间调节（默认 -75 dBm）
- 每次关闭后有 5 分钟冷却时间，避免频繁切换
- 实时显示当前 WiFi 信号强度
- 支持中英文界面切换

## 信号强度参考

| RSSI | 信号质量 |
|------|----------|
| > -50 dBm | 极强 |
| -50 ~ -60 dBm | 强 |
| -60 ~ -75 dBm | 一般 |
| -75 ~ -85 dBm | 弱 |
| < -85 dBm | 极弱 |

## 开源协议

[MIT](LICENSE)
