# 物联网多协议网关演示项目

基于Java的轻量级物联网网关，支持Modbus TCP采集、边缘计算、MQTT云端上报、Web可视化。

## 技术栈
- Java 11
- Modbus TCP（jlibmodbus）
- MQTT（Eclipse Paho）
- Web框架（Spark Java）
- 可视化（Chart.js）
- Maven

## 项目结构

```
iot-gateway-demo/
├── src/
│   ├── main/java/com/iot/
│   │   ├── GatewayApplication.java    # 启动类
│   │   ├── ModbusCollector.java       # Modbus采集
│   │   ├── EdgeProcessor.java         # 边缘计算：滑动平均滤波
│   │   ├── ChangeRateDetector.java    # 边缘计算：变化率检测
│   │   ├── MqttPublisher.java         # MQTT上报
│   │   ├── DataStore.java             # 数据存储
│   │   └── WebServer.java             # Web服务器
│   └── resources/
│       └── public/
│           └── index.html             # 可视化面板
├── docs/
│   ├── architecture.png               # 手绘架构图
│   └── demo-video.md                  # 视频链接
├── README.md
└── pom.xml
```

## 功能特性
- [x] Modbus设备数据采集
- [x] 滑动平均滤波（边缘计算）
- [x] **变化率检测**（边缘计算）- 识别异常波动
- [x] 阈值告警
- [x] MQTT云端上报
- [x] **Web实时可视化** - 数据趋势图表

## 边缘计算功能

### 1. 滑动平均滤波
- 窗口大小：5个采样点
- 平滑噪声数据

### 2. 变化率检测
- 计算数据变化速率（值/秒）
- 阈值：50/秒
- 识别趋势：上升/下降/平稳

## 快速开始

### 1. 启动Modbus模拟器
```bash
# 使用diagslave模拟器
diagslave -m tcp -p 502
```

### 2. 编译运行
```bash
# 编译
mvn clean package

# 运行
java -jar target/iot-gateway-demo-1.0.0.jar
```

### 3. 访问Web界面
打开浏览器访问：http://localhost:8080

### 4. 查看MQTT数据
使用EMQX在线客户端订阅 `sensor/data` 主题：
https://www.emqx.io/zh/mqtt-client

## 核心流程

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Modbus TCP  │───▶│ 边缘计算    │───▶│  MQTT上报   │───▶│  Web展示    │
│  采集数据   │    │ 滤波+变化率 │    │ 到EMQX云端  │    │ 实时可视化  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

## API接口

| 接口 | 说明 |
|------|------|
| GET /api/data/latest | 获取最新数据 |
| GET /api/data/history?limit=50 | 获取历史数据 |
| GET /api/stats | 获取统计数据 |

## 配置说明

默认配置在 `GatewayApplication.java` 中：

```java
// Modbus配置
String MODBUS_HOST = "127.0.0.1";
int MODBUS_PORT = 502;
int SLAVE_ID = 1;

// MQTT配置（EMQX公共Broker）
String MQTT_BROKER = "tcp://broker.emqx.io:1883";
String MQTT_TOPIC = "sensor/data";

// Web服务
int WEB_PORT = 8080;

// 采集间隔
long COLLECT_INTERVAL_MS = 1000;
```

## Web界面预览

- **实时数值卡片**：原始值、滤波值、变化率、告警状态
- **数据趋势图**：原始值 vs 滤波值对比
- **变化率图**：实时监控数据波动

## 适用场景
- 工厂设备数字化改造
- 环境监测系统
- 教学演示

## 联系方式
寻求物联网开发岗位，欢迎技术交流。
