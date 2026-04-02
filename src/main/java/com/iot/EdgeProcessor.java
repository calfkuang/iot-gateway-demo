package com.iot;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 边缘处理器
 * 实现滑动平均滤波和阈值告警
 */
public class EdgeProcessor {
    
    private final int windowSize;
    private final double minThreshold;
    private final double maxThreshold;
    
    private final Queue<Double> window;
    
    public EdgeProcessor(int windowSize, double minThreshold, double maxThreshold) {
        this.windowSize = windowSize;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.window = new LinkedList<>();
    }
    
    /**
     * 处理数据：滤波 + 告警检测
     * @param rawValue 原始值
     * @return 处理结果
     */
    public ProcessResult process(double rawValue) {
        // 添加到滑动窗口
        window.offer(rawValue);
        if (window.size() > windowSize) {
            window.poll();
        }
        
        // 计算滑动平均值
        double filteredValue = calculateMovingAverage();
        
        // 阈值检测
        boolean hasAlarm = checkThreshold(filteredValue);
        
        return new ProcessResult(filteredValue, hasAlarm);
    }
    
    /**
     * 计算滑动平均值
     */
    private double calculateMovingAverage() {
        if (window.isEmpty()) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (double value : window) {
            sum += value;
        }
        
        return sum / window.size();
    }
    
    /**
     * 阈值检测
     */
    private boolean checkThreshold(double value) {
        return value < minThreshold || value > maxThreshold;
    }
    
    /**
     * 清空窗口
     */
    public void clear() {
        window.clear();
    }
    
    /**
     * 获取当前窗口大小
     */
    public int getCurrentWindowSize() {
        return window.size();
    }
    
    /**
     * 处理结果内部类
     */
    public static class ProcessResult {
        private final double filteredValue;
        private final boolean hasAlarm;
        
        public ProcessResult(double filteredValue, boolean hasAlarm) {
            this.filteredValue = filteredValue;
            this.hasAlarm = hasAlarm;
        }
        
        public double getFilteredValue() {
            return filteredValue;
        }
        
        public boolean hasAlarm() {
            return hasAlarm;
        }
        
        @Override
        public String toString() {
            return String.format("ProcessResult{value=%.2f, alarm=%b}", filteredValue, hasAlarm);
        }
    }
}
