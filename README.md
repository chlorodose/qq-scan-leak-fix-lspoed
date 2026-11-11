# QQ 扫码闪退修复 1.1

> Made with gpt-sol-6.1

针对特定 QQ 扫码后闪退的 legacy Xposed 兼容模块，包名 `local.lilith.qqscanfix`，当前版本 1.1。
它通过现有的 Zygisk → Vector 加载，产物是 APK，不是独立的原生 Zygisk ZIP。

## 问题背景与验证

在 Android 16 / Evolution X 环境中，QQ 9.3.30 扫码后闪退，升级到 9.3.85 后仍复现。
捕获到的故障链路是：`QQScanActivity.doOnDestroy()` 注销广播接收器时异常，留下尚未清理的 receiver；
页面销毁时系统报告 `IntentReceiverLeakedViolation`，随后出现 StrictMode `POLICY_DEATH` 日志，QQ 进程收到 `SIGKILL`。
因此这里绕过的是特定接收器泄漏触发的致命处理，而不是扫码识别本身。

1.1 已在上述问题设备上由实际扫码测试确认生效；这不代表所有 ROM、QQ 版本或扫码场景均已验证。
仅凭 `POLICY_DEATH` 日志不能断言 QQ 自行启用了 `penaltyDeath`，也没有据此确定当前 Evolution X 构建的具体 framework 源码原因。

## 行为与限制

- 只在 `com.tencent.mobileqq` 主进程安装 hook；Vector 作用域仅 QQ、用户 0。
- hook `android.os.StrictMode.onVmPolicyViolation` 的两个重载。
- 仅当异常类型为 `IntentReceiverLeakedViolation`，且原始注册堆栈包含
  `com.tencent.mobileqq.olympic.activity.QQScanActivity.doOnCreate`，才略过其 StrictMode 处罚并记录原始堆栈。
- 其他应用、其他异常、其他页面泄漏、VmPolicy 配置及系统信号处理不变。
- 不修改 QQ APK、QQ 数据或系统 framework。
- 这是兼容性绕过，不修复 QQ 自身的接收器清理 bug。
- 不申请 Android 权限，不含网络、存储读写、后台服务或启动广播。
- 无需新增原生 Zygisk 引擎，使用手机现有 Zygisk → Vector 加载。
- 1.0 的 `onIntentReceiverLeaked` 入口 hook 已实测未命中；内联是可能解释，但未证实。1.1 改为最终 VM 处罚处理入口，已实测命中并生效。

## 构建

Linux x86_64，Python 3.12+、网络。所有构建依赖下载到当前目录，无需系统安装 Java/Android SDK。

```sh
python3 setup.py && bash build.sh
```

输出 `build/qq-scan-fix.apk`；执行目标/非目标堆栈过滤测试、v2/v3 签名验证。
依赖 URL 与 SHA256 在 `dependencies.json`。Android API jar 为 Sable/android-platforms 镜像，Xposed API 为阿里云 Maven 镜像。
首次构建自动生成仅用于本模块的自签密钥 `signing.p12`。实际私钥不在源码包中；独立重建生成的签名与已安装版本不同。
构建脚本使用固定的本地开发 keystore 密码，不能将它视为发布密钥保护措施；请妥善保存自己的 `signing.p12`，且不要上传。

## 安装和启用

```sh
adb install build/qq-scan-fix.apk
adb shell "su -c '/data/adb/modules/zygisk_vector/cli scope set local.lilith.qqscanfix com.tencent.mobileqq/0'"
adb shell "su -c '/data/adb/modules/zygisk_vector/cli modules enable local.lilith.qqscanfix'"
adb shell am force-stop com.tencent.mobileqq
adb shell am start -n com.tencent.mobileqq/com.tencent.mobileqq.activity.SplashActivity
adb logcat -d -s QQScanLeakFix:I
```

同签名更新使用 `adb install -r`。不同签名重建需先卸载**本修复模块**，不可卸载 QQ。

## 回退

在 Vector 禁用“QQ 扫码闪退修复”，然后强制停止并重新启动 QQ。或执行：

```sh
adb shell "su -c '/data/adb/modules/zygisk_vector/cli modules disable local.lilith.qqscanfix'"
adb uninstall local.lilith.qqscanfix
```

无需恢复 QQ 数据、系统文件、HMA-OSS 或存储空间隔离设置。

## 相关 issue

[crDroid issue #1150](https://github.com/crdroidandroid/issue_tracker/issues/1150)

该报告与这里的故障链路高度吻合：相同泄漏被 ROM 的 `forceDeath` 强制退出，且进程内绕过成功。
报告中的 ROM 路径即使未设置一般的 `PENALTY_DEATH` 标志也会打印 `POLICY_DEATH`。
没有据此认定 Evolution X 一定使用相同改动，也没有认定 HMA-OSS / 存储隔离为原因。


## 源码布局

- `src/`：Xposed 入口与注册堆栈过滤器。
- `assets/xposed_init`、`res/`、`AndroidManifest.xml`：模块入口、作用域提示及 APK 元数据。
- `FilterTest.java`：目标与非目标堆栈过滤验证。
- `setup.py`、`dependencies.json`：校验 SHA-256 后准备本地构建工具链。
- `build.sh`、`Sign.java`：编译、打包、自签名及签名验证。

仓库不包含构建产物、依赖缓存、设备日志或实际私钥。作用域需在 Vector 中显式设为 QQ / 用户 0；主进程限制另由代码检查。
