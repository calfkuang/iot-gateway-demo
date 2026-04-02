package com.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Spark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

/**
 * Web服务器
 * 提供REST API和静态页面服务
 */
public class WebServer {

    private final DataStore dataStore;
    private final ObjectMapper mapper;

    public WebServer(DataStore dataStore) {
        this.dataStore = dataStore;
        this.mapper = new ObjectMapper();
    }

    /**
     * 启动Web服务器
     */
    public void start(int port) {
        port(port);
        staticFiles.location("/public");

        // 启用CORS
        enableCORS();

        // API路由
        setupRoutes();

        System.out.println("Web服务器启动: http://localhost:" + port);
    }

    /**
     * 设置API路由
     */
    private void setupRoutes() {
        // 获取最新数据
        get("/api/data/latest", (req, res) -> {
            res.type("application/json");
            DataStore.DataPoint point = dataStore.getLatestPoint();
            if (point == null) {
                return "{}";
            }
            return mapper.writeValueAsString(convertToMap(point));
        });

        // 获取历史数据
        get("/api/data/history", (req, res) -> {
            res.type("application/json");
            int limit = Integer.parseInt(req.queryParamOrDefault("limit", "50"));
            List<DataStore.DataPoint> points = dataStore.getRecentDataPoints(limit);
            return mapper.writeValueAsString(points.stream().map(this::convertToMap).toArray());
        });

        // 获取统计数据
        get("/api/stats", (req, res) -> {
            res.type("application/json");
            List<DataStore.DataPoint> points = dataStore.getAllDataPoints();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCount", points.size());

            if (!points.isEmpty()) {
                double avgRaw = points.stream().mapToDouble(DataStore.DataPoint::getRawValue).average().orElse(0);
                double avgFiltered = points.stream().mapToDouble(DataStore.DataPoint::getFilteredValue).average().orElse(0);
                long alarmCount = points.stream().filter(p -> p.isThresholdAlarm() || p.isRateAlarm()).count();

                stats.put("avgRawValue", String.format("%.2f", avgRaw));
                stats.put("avgFilteredValue", String.format("%.2f", avgFiltered));
                stats.put("alarmCount", alarmCount);
            }

            return mapper.writeValueAsString(stats);
        });

        // 主页
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });
    }

    /**
     * 数据点转换为Map
     */
    private Map<String, Object> convertToMap(DataStore.DataPoint point) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", point.getTimestamp());
        map.put("rawValue", point.getRawValue());
        map.put("filteredValue", point.getFilteredValue());
        map.put("thresholdAlarm", point.isThresholdAlarm());
        map.put("changeRate", point.getChangeRate());
        map.put("rateAlarm", point.isRateAlarm());
        map.put("trend", point.getTrend());
        return map;
    }

    /**
     * 启用CORS
     */
    private void enableCORS() {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
            response.type("application/json");
        });
    }

    /**
     * 停止服务器
     */
    public void stop() {
        Spark.stop();
    }
}
