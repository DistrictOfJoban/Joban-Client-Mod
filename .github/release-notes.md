# JCM v2.3.0-beta.1-yanyang.5

修复 yanyang.4 中 C++ 模块已加载、线路数据正常，但 WR2-A03 的 LCD 和车号牌仍不显示的问题。

游戏完整日志显示，创建这些模型时抛出了 `GlStateTracker: Not protected`：原生模块在列车每帧渲染入口直接上传模型，而 MTR 的 GPU 缓冲区上传要求先捕获 OpenGL 状态。JavaScript 在资源加载阶段使用的外层保护，在这里并不存在。

- `ModelJS.uploadRawModel()` 现在在需要时捕获 GL 状态，并在 `finally` 中恢复调用前的 VAO、VBO、EBO 等状态。已有外层保护时保留外层的所有权，兼容资源重载和动态模型上传。
- 原生宿主记录实际创建成功的纹理/模型数量，并提供逐车厢回放日志。
- 仅上传了像素的帧不再被误报为含有模型绘制；不再为城市里所有非原生脚本列车打印无关诊断。
- 新增真实 MTR GPU 上传回归验证，覆盖正常上传、嵌套保护、异常恢复、顶点/索引缓冲区读回以及绘制后的像素回读。

本机使用用户实际安装的 `YYMTR-fab.4.0.5+1.20.1_2026.10.06.2989.jar` 和 NVIDIA GPU 复现了旧版异常，修复后的 42 项 OpenGL 检查通过。发布流程另用原版 MTR 4.0.5 和 Mesa 软件 OpenGL 验证，并完成四平台 CMake/CTest 和 JNI 联调、12 个 Fabric/Forge jar 构建。

Minecraft 支持版本：1.17.1、1.18.2、1.19.2、1.19.4、1.20.1、1.20.4，各提供 Fabric 与 Forge jar。

本次未改动 C++ 脚本或 JNI ABI，可继续使用 yanyang.4 对应的四平台资源包。安装时替换旧 JCM jar，并完全重启游戏。实际游戏中的 LCD 和车号视觉效果仍需游戏内复测确认。
