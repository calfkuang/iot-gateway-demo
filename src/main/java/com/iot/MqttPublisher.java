package com.iot;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT发布器
 * 负责将数据发布到MQTT Broker
 */
public class MqttPublisher {

    private static final Logger logger = LoggerFactory.getLogger(MqttPublisher.class);

    private final String brokerUrl;
    private final String clientId;
    private final String topic;

    private MqttClient client;
    private boolean connected = false;

    public MqttPublisher(String brokerUrl, String clientId, String topic) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.topic = topic;
    }

    /**
     * 建立MQTT连接
     */
    public boolean connect() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl, clientId, persistence);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            options.setAutomaticReconnect(true);

            client.connect(options);
            connected = true;

            logger.info("MQTT连接成功: {}, ClientID: {}", brokerUrl, clientId);
            return true;

        } catch (MqttException e) {
            logger.error("MQTT连接失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 发布消息
     * @param payload 消息内容
     */
    public void publish(String payload) {
        if (!connected || client == null) {
            logger.warn("MQTT未连接，无法发布消息");
            return;
        }

        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            message.setRetained(false);

            client.publish(topic, message);
            logger.debug("消息已发布到 [{}]: {}", topic, payload);

        } catch (MqttException e) {
            logger.error("MQTT发布失败: {}", e.getMessage());
        }
    }

    /**
     * 发布消息（指定Topic）
     * @param topic 主题
     * @param payload 消息内容
     */
    public void publish(String topic, String payload) {
        if (!connected || client == null) {
            logger.warn("MQTT未连接，无法发布消息");
            return;
        }

        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);

            client.publish(topic, message);
            logger.debug("消息已发布到 [{}]: {}", topic, payload);

        } catch (MqttException e) {
            logger.error("MQTT发布失败: {}", e.getMessage());
        }
    }

    /**
     * 断开MQTT连接
     */
    public void disconnect() {
        if (client != null && connected) {
            try {
                client.disconnect();
                logger.info("MQTT连接已关闭");
            } catch (MqttException e) {
                logger.error("关闭MQTT连接失败: {}", e.getMessage());
            }
        }
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getTopic() {
        return topic;
    }
}
