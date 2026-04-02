package com.iot;

/**
 * 变化率检测器
 * 边缘计算功能：检测数据变化速率，识别异常波动
 */
public class ChangeRateDetector {
    
    private final double threshold;
    private Double lastValue;
    private long lastTime;
    
    public ChangeRateDetector(double threshold) {
        this.threshold = threshold;
        this.lastTime = System.currentTimeMillis();
    }
    
    /**
     * 检测变化率
     * @param currentValue 当前值
     * @return 变化率检测结果
     */
    public RateResult detect(double currentValue) {
        long currentTime = System.currentTimeMillis();
        
        if (lastValue == null) {
            lastValue = currentValue;
            lastTime = currentTime;
            return new RateResult(0.0, false, "首次采样");
        }
        
        double valueDiff = currentValue - lastValue;
        double timeDiff = (currentTime - lastTime) / 1000.0; // 转换为秒
        
        if (timeDiff <= 0) {
            timeDiff = 0.001; // 防止除零
        }
        
        double rate = valueDiff / timeDiff; // 变化率：值/秒
        boolean isAbnormal = Math.abs(rate) > threshold;
        String trend = rate > 0 ? "上升" : (rate < 0 ? "下降" : "平稳");
        
        // 更新状态
        lastValue = currentValue;
        lastTime = currentTime;
        
        return new RateResult(rate, isAbnormal, trend);
    }
    
    /**
     * 重置检测器
     */
    public void reset() {
        lastValue = null;
        lastTime = System.currentTimeMillis();
    }
    
    /**
     * 变化率结果
     */
    public static class RateResult {
        private final double rate;
        private final boolean abnormal;
        private final String trend;
        
        public RateResult(double rate, boolean abnormal, String trend) {
            this.rate = rate;
            this.abnormal = abnormal;
            this.trend = trend;
        }
        
        public double getRate() {
            return rate;
        }
        
        public boolean isAbnormal() {
            return abnormal;
        }
        
        public String getTrend() {
            return trend;
        }
        
        @Override
        public String toString() {
            return String.format("RateResult{rate=%.2f/s, abnormal=%b, trend=%s}", rate, abnormal, trend);
        }
    }
}
