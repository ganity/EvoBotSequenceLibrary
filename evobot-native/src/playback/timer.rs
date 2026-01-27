use std::time::{Duration, Instant};
use tokio::time::{interval, MissedTickBehavior};

pub struct PrecisionTimer {
    target_frequency: u32,
    interval_ms: u64,
    last_tick_time: Option<Instant>,
    drift_compensation: i64,
    // RK3399优化字段
    rk3399_mode: bool,
    performance_samples: Vec<u64>,
    adaptive_threshold: u64,
}

impl PrecisionTimer {
    pub fn new(frequency: u32) -> Self {
        let interval_ms = 1000 / frequency as u64;
        Self {
            target_frequency: frequency,
            interval_ms,
            last_tick_time: None,
            drift_compensation: 0,
            rk3399_mode: true, // 默认启用RK3399优化
            performance_samples: Vec::with_capacity(100),
            adaptive_threshold: interval_ms / 4, // 25%阈值
        }
    }
    
    pub fn create_interval(&self) -> tokio::time::Interval {
        let mut interval = interval(Duration::from_millis(self.interval_ms));
        interval.set_missed_tick_behavior(MissedTickBehavior::Skip);
        interval
    }
    
    /// RK3399优化版本的延时计算
    pub fn calculate_next_delay_rk3399(&mut self) -> Duration {
        let current_time = Instant::now();
        
        if let Some(last_time) = self.last_tick_time {
            let actual_interval = current_time.duration_since(last_time);
            let expected_interval = Duration::from_millis(self.interval_ms);
            
            // 记录性能样本
            self.record_performance_sample(actual_interval.as_millis() as u64);
            
            // 计算时间漂移
            let drift = actual_interval.as_millis() as i64 - expected_interval.as_millis() as i64;
            
            // RK3399优化：自适应漂移补偿
            if self.rk3399_mode {
                self.drift_compensation = self.calculate_adaptive_compensation(drift);
            } else {
                self.drift_compensation += drift;
            }
            
            // 应用补偿，RK3399允许更大的调整范围
            let compensation_range = if self.rk3399_mode { 
                self.interval_ms as i64 // 100%范围
            } else { 
                self.interval_ms as i64 / 2 // 50%范围
            };
            
            let compensated_interval = self.interval_ms as i64 - self.drift_compensation;
            let min_interval = self.interval_ms as i64 / 4; // 最小25%
            let max_interval = self.interval_ms as i64 + compensation_range;
            
            let adjusted_interval = compensated_interval.clamp(min_interval, max_interval) as u64;
            
            if drift.abs() > 2 { // RK3399更敏感的阈值
                log::debug!(
                    "RK3399 timer compensation: actual={}ms, expected={}ms, adjusted={}ms, drift={}ms",
                    actual_interval.as_millis(),
                    self.interval_ms,
                    adjusted_interval,
                    drift
                );
            }
            
            // RK3399优化：周期性重置补偿以避免累积误差
            if self.performance_samples.len() >= 50 {
                self.reset_compensation_if_needed();
            }
            
            Duration::from_millis(adjusted_interval)
        } else {
            Duration::from_millis(self.interval_ms)
        }
    }
    
    pub fn calculate_next_delay(&mut self) -> Duration {
        if self.rk3399_mode {
            self.calculate_next_delay_rk3399()
        } else {
            self.calculate_next_delay_standard()
        }
    }
    
    fn calculate_next_delay_standard(&mut self) -> Duration {
        let current_time = Instant::now();
        
        if let Some(last_time) = self.last_tick_time {
            let actual_interval = current_time.duration_since(last_time);
            let expected_interval = Duration::from_millis(self.interval_ms);
            
            // 计算时间漂移
            let drift = actual_interval.as_millis() as i64 - expected_interval.as_millis() as i64;
            self.drift_compensation += drift;
            
            // 应用补偿，但限制在±50%范围内
            let compensated_interval = self.interval_ms as i64 - self.drift_compensation;
            let min_interval = self.interval_ms as i64 / 2;
            let max_interval = self.interval_ms as i64 * 3 / 2;
            
            let adjusted_interval = compensated_interval.clamp(min_interval, max_interval) as u64;
            
            if drift.abs() > 5 {
                log::debug!(
                    "Timer compensation: actual={}ms, expected={}ms, adjusted={}ms, drift={}ms",
                    actual_interval.as_millis(),
                    self.interval_ms,
                    adjusted_interval,
                    drift
                );
            }
            
            // 重置补偿以避免累积
            if self.drift_compensation.abs() > self.interval_ms as i64 {
                self.drift_compensation = 0;
            }
            
            Duration::from_millis(adjusted_interval)
        } else {
            Duration::from_millis(self.interval_ms)
        }
    }
    
    /// RK3399优化：记录性能样本用于自适应调整
    fn record_performance_sample(&mut self, sample: u64) {
        self.performance_samples.push(sample);
        
        // 保持样本数量在合理范围内
        if self.performance_samples.len() > 100 {
            self.performance_samples.remove(0);
        }
    }
    
    /// RK3399优化：计算自适应补偿
    fn calculate_adaptive_compensation(&self, current_drift: i64) -> i64 {
        if self.performance_samples.len() < 10 {
            return current_drift; // 样本不足，使用标准补偿
        }
        
        // 计算最近样本的平均值和标准差
        let recent_samples = &self.performance_samples[self.performance_samples.len().saturating_sub(20)..];
        let avg: f64 = recent_samples.iter().map(|&x| x as f64).sum::<f64>() / recent_samples.len() as f64;
        let variance: f64 = recent_samples.iter()
            .map(|&x| (x as f64 - avg).powi(2))
            .sum::<f64>() / recent_samples.len() as f64;
        let std_dev = variance.sqrt();
        
        // 如果当前漂移在正常范围内，使用渐进式补偿
        let _expected = self.interval_ms as f64;
        if (current_drift as f64).abs() < std_dev * 2.0 {
            // 渐进式补偿：减少过度调整
            (current_drift as f64 * 0.7) as i64
        } else {
            // 大幅漂移：使用完整补偿
            current_drift
        }
    }
    
    /// RK3399优化：智能重置补偿
    fn reset_compensation_if_needed(&mut self) {
        if self.performance_samples.len() < 20 {
            return;
        }
        
        // 检查最近的性能是否稳定
        let recent_samples = &self.performance_samples[self.performance_samples.len() - 20..];
        let avg: f64 = recent_samples.iter().map(|&x| x as f64).sum::<f64>() / recent_samples.len() as f64;
        let expected = self.interval_ms as f64;
        
        // 如果平均性能接近期望值，重置补偿
        if (avg - expected).abs() < self.adaptive_threshold as f64 {
            self.drift_compensation = 0;
            log::debug!("RK3399: Reset drift compensation due to stable performance");
        }
    }
    
    pub fn mark_tick(&mut self) {
        self.last_tick_time = Some(Instant::now());
    }
    
    pub fn reset(&mut self) {
        self.last_tick_time = None;
        self.drift_compensation = 0;
        self.performance_samples.clear();
    }
    
    pub fn get_frequency(&self) -> u32 {
        self.target_frequency
    }
    
    /// 启用或禁用RK3399优化模式
    pub fn set_rk3399_mode(&mut self, enabled: bool) {
        self.rk3399_mode = enabled;
        if enabled {
            log::info!("RK3399 timer optimization enabled");
        } else {
            log::info!("RK3399 timer optimization disabled");
        }
    }
    
    /// 获取RK3399性能统计
    pub fn get_rk3399_performance_stats(&self) -> String {
        if self.performance_samples.is_empty() {
            return "No performance data available".to_string();
        }
        
        let avg: f64 = self.performance_samples.iter().map(|&x| x as f64).sum::<f64>() / self.performance_samples.len() as f64;
        let min = *self.performance_samples.iter().min().unwrap();
        let max = *self.performance_samples.iter().max().unwrap();
        
        format!(
            "RK3399 Timer Stats: avg={:.2}ms, min={}ms, max={}ms, samples={}, drift={}ms",
            avg, min, max, self.performance_samples.len(), self.drift_compensation
        )
    }
}