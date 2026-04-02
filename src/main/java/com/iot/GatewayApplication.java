package com.iot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网关启动类
 * 协调Modbus采集、边缘计算、MQTT上报、Web可视化
 */
public class GatewayApplication {

    private static final Logger logger = LoggerFactory.getLogger(GatewayApplication.class);

    // 配置参数
    private static final String MODBUS_HOST = "127.0.0.1";
    private static final int MODBUS_PORT = 502;
    private static final int SLAVE_ID = 1;
    private static final int REGISTER_ADDRESS = 0;

    private static final String MQTT_BROKER = "tcp://broker.emqx.io:1883";
    private static final String MQTT_CLIENT_ID = "gateway-demo";
    private static final String MQTT_TOPIC = "sensor/data";

    private static final long COLLECT_INTERVAL_MS = 1000;
    private static final int WEB_PORT = 8080;

    public static void main(String[] args) {
        logger.info("=== IoT Gateway 启动 ===");

        // 初始化数据存储
        DataStore dataStore = new DataStore();

        // 初始化边缘计算组件
        EdgeProcessor edgeProcessor = new EdgeProcessor(5, 0.0, 100.0);
        ChangeRateDetector rateDetector = new ChangeRateDetector(50.0); // 变化率阈值50/秒

        // 初始化通信组件
        ModbusCollector modbusCollector = new ModbusCollector(MODBUS_HOST, MODBUS_PORT, SLAVE_ID);
        MqttPublisher mqttPublisher = new MqttPublisher(MQTT_BROKER, MQTT_CLIENT_ID, MQTT_TOPIC);

        // 启动Web服务器
        WebServer webServer = new WebServer(dataStore);
        webServer.start(WEB_PORT);
        logger.info("Web监控界面: http://localhost:{}", WEB_PORT);

        // 连接Modbus
        if (!modbusCollector.connect()) {
            logger.error("Modbus连接失败，退出");
            return;
        }

        // 连接MQTT
        if (!mqttPublisher.connect()) {
            logger.error("MQTT连接失败，退出");
            modbusCollector.disconnect();
            return;
        }

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("正在关闭网关...");
            modbusCollector.disconnect();
            mqttPublisher.disconnect();
            webServer.stop();
        }));

        // 主循环：采集 -> 处理 -> 上报
        logger.info("开始数据采集循环，间隔: {}ms", COLLECT_INTERVAL_MS);

        while (true) {
            try {
                // 1. 采集原始数据
                double rawValue = modbusCollector.readRegister(REGISTER_ADDRESS);
                logger.debug("采集原始值: {}", rawValue);

                // 2. 边缘计算 - 滑动平均滤波
                EdgeProcessor.ProcessResult filterResult = edgeProcessor.process(rawValue);

                // 3. 边缘计算 - 变化率检测
                ChangeRateDetector.RateResult rateResult = rateDetector.detect(filterResult.getFilteredValue());

                // 4. 存储数据点（用于Web展示）
                DataStore.DataPoint dataPoint = new DataStore.DataPoint(
                    System.currentTimeMillis(),
                    rawValue,
                    filterResult.getFilteredValue(),
                    filterResult.hasAlarm(),
                    rateResult.getRate(),
                    rateResult.isAbnormal(),
                    rateResult.getTrend()
                );
                dataStore.addDataPoint(dataPoint);

                // 5. 告警判断
                boolean hasAnyAlarm = filterResult.hasAlarm() || rateResult.isAbnormal();
                if (filterResult.hasAlarm()) {
                    logger.warn("阈值告警触发! 值: {}", filterResult.getFilteredValue());
                }
                if (rateResult.isAbnormal()) {
                    logger.warn("变化率异常! 速率: {}/s, 趋势: {}", rateResult.getRate(), rateResult.getTrend());
                }

                // 6. MQTT上报
                String payload = buildPayload(rawValue, filterResult, rateResult);
                mqttPublisher.publish(payload);

                // 控制台输出
                System.out.printf("[数据] 原始: %.2f | 滤波: %.2f | 变化率: %.2f/s(%s) | 告警: %s%n",
                    rawValue,
                    filterResult.getFilteredValue(),
                    rateResult.getRate(),
                    rateResult.getTrend(),
                    hasAnyAlarm ? "是" : "否"
                );

                // 等待下一次采集
                Thread.sleep(COLLECT_INTERVAL_MS);

            } catch (InterruptedException e) {
                logger.info("采集线程被中断");
                break;
            } catch (Exception e) {
                logger.error("采集异常: {}", e.getMessage());
            }
        }

        // 清理资源
        modbusCollector.disconnect();
        mqttPublisher.disconnect();
        webServer.stop();
        logger.info("=== IoT Gateway 停止 ===");
    }

    /**
     * 构建MQTT消息体
     */
    private static String buildPayload(double rawValue, EdgeProcessor.ProcessResult filterResult,
                                       ChangeRateDetector.RateResult rateResult) {
        return String.format(
            "{\"timestamp\":%d,\"rawValue\":%.2f,\"filteredValue\":%.2f,\"thresholdAlarm\":%b,\"changeRate\":%.2f,\"rateAlarm\":%b,\"trend\":\"%s\"}",
            System.currentTimeMillis(),
            rawValue,
            filterResult.getFilteredValue(),
            filterResult.hasAlarm(),
            rateResult.getRate(),
            rateResult.isAbnormal(),
            rateResult.getTrend()
        );
    }
}
