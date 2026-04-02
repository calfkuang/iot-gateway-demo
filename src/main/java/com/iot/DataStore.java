package com.iot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 数据存储
 * 线程安全的数据缓存，用于Web展示
 */
public class DataStore {
    
    private static final int MAX_SIZE = 100;
    
    private final List<DataPoint> dataPoints;
    private DataPoint latestPoint;
    
    public DataStore() {
        this.dataPoints = new CopyOnWriteArrayList<>();
    }
    
    /**
     * 添加数据点
     */
    public void addDataPoint(DataPoint point) {
        dataPoints.add(point);
        latestPoint = point;
        
        // 限制历史数据大小
        if (dataPoints.size() > MAX_SIZE) {
            dataPoints.remove(0);
        }
    }
    
    /**
     * 获取所有数据点
     */
    public List<DataPoint> getAllDataPoints() {
        return new ArrayList<>(dataPoints);
    }
    
    /**
     * 获取最新数据点
     */
    public DataPoint getLatestPoint() {
        return latestPoint;
    }
    
    /**
     * 获取最近N个数据点
     */
    public List<DataPoint> getRecentDataPoints(int count) {
        int size = dataPoints.size();
        if (count >= size) {
            return new ArrayList<>(dataPoints);
        }
        return new ArrayList<>(dataPoints.subList(size - count, size));
    }
    
    /**
     * 数据点模型
     */
    public static class DataPoint {
        private final long timestamp;
        private final double rawValue;
        private final double filteredValue;
        private final boolean thresholdAlarm;
        private final double changeRate;
        private final boolean rateAlarm;
        private final String trend;
        
        public DataPoint(long timestamp, double rawValue, double filteredValue, 
                        boolean thresholdAlarm, double changeRate, boolean rateAlarm, String trend) {
            this.timestamp = timestamp;
            this.rawValue = rawValue;
            this.filteredValue = filteredValue;
            this.thresholdAlarm = thresholdAlarm;
            this.changeRate = changeRate;
            this.rateAlarm = rateAlarm;
            this.trend = trend;
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public double getRawValue() { return rawValue; }
        public double getFilteredValue() { return filteredValue; }
        public boolean isThresholdAlarm() { return thresholdAlarm; }
        public double getChangeRate() { return changeRate; }
        public boolean isRateAlarm() { return rateAlarm; }
        public String getTrend() { return trend; }
    }
}
