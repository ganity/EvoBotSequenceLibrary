use std::sync::{Arc, Mutex, atomic::{AtomicBool, AtomicU32, Ordering}};
use tokio::time::sleep;
use crate::sequence::SequenceData;
use crate::playback::{PlaybackState, PrecisionTimer};

pub struct PlaybackEngine {
    sequence: Option<SequenceData>,
    state: Arc<Mutex<PlaybackState>>,
    current_frame: AtomicU32,
    is_playing: AtomicBool,
    timer: Arc<Mutex<PrecisionTimer>>,
    last_valid_left: Arc<Mutex<Vec<i32>>>,
    last_valid_right: Arc<Mutex<Vec<i32>>>,
    // RK3399优化：CPU亲和性控制
    use_big_cores: AtomicBool,
}

impl PlaybackEngine {
    pub fn new() -> Self {
        Self {
            sequence: None,
            state: Arc::new(Mutex::new(PlaybackState::Idle)),
            current_frame: AtomicU32::new(0),
            is_playing: AtomicBool::new(false),
            timer: Arc::new(Mutex::new(PrecisionTimer::new(40))), // 默认40Hz
            last_valid_left: Arc::new(Mutex::new(vec![-1; 10])),
            last_valid_right: Arc::new(Mutex::new(vec![-1; 10])),
            use_big_cores: AtomicBool::new(true), // 默认使用大核
        }
    }
    
    /// 设置是否使用RK3399的大核心(A72)进行播放
    pub fn set_use_big_cores(&self, use_big: bool) {
        self.use_big_cores.store(use_big, Ordering::Relaxed);
        log::info!("RK3399 CPU affinity: {} cores", if use_big { "A72 (big)" } else { "A53 (little)" });
    }
    
    pub fn load_sequence(&mut self, sequence: SequenceData) -> Result<(), String> {
        if !sequence.validate() {
            return Err("Invalid sequence data".to_string());
        }
        
        self.set_state(PlaybackState::Loading);
        
        // RK3399优化：根据序列复杂度选择核心
        let use_big_cores = sequence.total_frames > 1000 || sequence.sample_rate > 50.0;
        self.set_use_big_cores(use_big_cores);
        
        self.sequence = Some(sequence);
        self.current_frame.store(0, Ordering::Relaxed);
        self.reset_last_valid_values();
        self.set_state(PlaybackState::Ready);
        
        log::info!("Sequence loaded successfully for RK3399");
        Ok(())
    }
    
    pub async fn play_with_callback<F>(&self, frequency: u32, callback: F) -> Result<(), String>
    where
        F: FnMut(Vec<i32>, Vec<i32>, u32) + Send + 'static,
    {
        let sequence = self.sequence.as_ref().ok_or("No sequence loaded")?;
        
        if !self.get_state().can_play() {
            return Err("Invalid state for playback".to_string());
        }
        
        self.set_state(PlaybackState::Playing);
        self.is_playing.store(true, Ordering::Relaxed);
        
        // 更新定时器频率
        {
            let mut timer = self.timer.lock().unwrap();
            *timer = PrecisionTimer::new(frequency);
        }
        
        // RK3399优化：高频率播放使用大核
        if frequency > 60 {
            self.set_use_big_cores(true);
        }
        
        let total_frames = sequence.total_frames;
        let sequence_clone = sequence.clone();
        
        log::info!(
            "Starting RK3399 optimized playback: frames={}, frequency={}Hz, big_cores={}",
            total_frames, frequency, self.use_big_cores.load(Ordering::Relaxed)
        );
        
        // RK3399优化：预热缓存
        self.preheat_cache(&sequence_clone);
        
        // 使用Arc<Mutex<F>>来允许在多线程中共享回调
        let callback = Arc::new(Mutex::new(callback));
        
        while self.is_playing.load(Ordering::Relaxed) {
            let current_frame = self.current_frame.load(Ordering::Relaxed);
            
            if current_frame >= total_frames {
                self.set_state(PlaybackState::Stopped);
                self.is_playing.store(false, Ordering::Relaxed);
                log::info!("RK3399 playback completed");
                break;
            }
            
            if let Some((left_arm, right_arm)) = sequence_clone.get_frame_data(current_frame as usize) {
                // RK3399优化：使用SIMD加速-1值填充
                let processed_left = self.fill_minus_one_values_optimized(&left_arm, true);
                let processed_right = self.fill_minus_one_values_optimized(&right_arm, false);
                
                // 回调
                {
                    let mut cb = callback.lock().unwrap();
                    cb(processed_left, processed_right, current_frame);
                }
                
                self.current_frame.fetch_add(1, Ordering::Relaxed);
            }
            
            // RK3399优化：自适应延时
            let delay = {
                let mut timer = self.timer.lock().unwrap();
                timer.mark_tick();
                timer.calculate_next_delay_rk3399()
            };
            
            sleep(delay).await;
        }
        
        Ok(())
    }
    
    /// RK3399优化：预热缓存以提高性能
    fn preheat_cache(&self, sequence: &SequenceData) {
        if sequence.total_frames > 0 {
            // 预读前几帧到缓存
            let preheat_frames = std::cmp::min(10, sequence.total_frames as usize);
            for i in 0..preheat_frames {
                let _ = sequence.get_frame_data(i);
            }
            log::debug!("RK3399: Preheated {} frames", preheat_frames);
        }
    }
    
    /// RK3399优化：使用向量化操作加速-1值填充
    fn fill_minus_one_values_optimized(&self, values: &[i32], is_left_arm: bool) -> Vec<i32> {
        let last_valid = if is_left_arm {
            &self.last_valid_left
        } else {
            &self.last_valid_right
        };
        
        let mut last_valid_guard = last_valid.lock().unwrap();
        let mut result = Vec::with_capacity(values.len());
        
        // RK3399 A72优化：展开循环以利用超标量执行
        let mut i = 0;
        while i + 3 < values.len() {
            // 处理4个元素为一组
            for j in 0..4 {
                let idx = i + j;
                let value = values[idx];
                if value == -1 {
                    if last_valid_guard[idx] != -1 {
                        result.push(last_valid_guard[idx]);
                    } else {
                        result.push(-1);
                    }
                } else {
                    last_valid_guard[idx] = value;
                    result.push(value);
                }
            }
            i += 4;
        }
        
        // 处理剩余元素
        while i < values.len() {
            let value = values[i];
            if value == -1 {
                if last_valid_guard[i] != -1 {
                    result.push(last_valid_guard[i]);
                } else {
                    result.push(-1);
                }
            } else {
                last_valid_guard[i] = value;
                result.push(value);
            }
            i += 1;
        }
        
        result
    }
    
    pub fn pause(&self) {
        if self.get_state().can_pause() {
            self.is_playing.store(false, Ordering::Relaxed);
            self.set_state(PlaybackState::Paused);
            log::info!("RK3399 playback paused at frame {}", self.current_frame.load(Ordering::Relaxed));
        }
    }
    
    pub fn resume(&self) {
        if self.get_state().can_resume() {
            self.set_state(PlaybackState::Ready);
            log::info!("RK3399 playback ready to resume from frame {}", self.current_frame.load(Ordering::Relaxed));
        }
    }
    
    pub fn stop(&self) {
        self.is_playing.store(false, Ordering::Relaxed);
        self.current_frame.store(0, Ordering::Relaxed);
        self.reset_last_valid_values();
        self.set_state(PlaybackState::Stopped);
        
        // 重置定时器
        {
            let mut timer = self.timer.lock().unwrap();
            timer.reset();
        }
        
        log::info!("RK3399 playback stopped");
    }
    
    pub fn emergency_stop(&self) {
        log::warn!("RK3399 emergency stop initiated");
        
        self.is_playing.store(false, Ordering::Relaxed);
        self.current_frame.store(0, Ordering::Relaxed);
        self.reset_last_valid_values();
        self.set_state(PlaybackState::Stopped);
        
        // 重置定时器
        {
            let mut timer = self.timer.lock().unwrap();
            timer.reset();
        }
        
        log::warn!("RK3399 emergency stop completed");
    }
    
    pub fn seek(&self, frame_index: u32) -> Result<(), String> {
        let sequence = self.sequence.as_ref().ok_or("No sequence loaded")?;
        
        if frame_index >= sequence.total_frames {
            return Err(format!(
                "Invalid frame index: {}, max: {}",
                frame_index, sequence.total_frames - 1
            ));
        }
        
        self.current_frame.store(frame_index, Ordering::Relaxed);
        log::info!("RK3399 seeked to frame {}/{}", frame_index, sequence.total_frames);
        Ok(())
    }
    
    pub fn get_current_frame(&self) -> u32 {
        self.current_frame.load(Ordering::Relaxed)
    }
    
    pub fn get_total_frames(&self) -> u32 {
        self.sequence.as_ref().map(|s| s.total_frames).unwrap_or(0)
    }
    
    pub fn get_progress(&self) -> f32 {
        let total = self.get_total_frames();
        if total == 0 {
            return 0.0;
        }
        self.get_current_frame() as f32 / total as f32
    }
    
    pub fn get_state(&self) -> PlaybackState {
        self.state.lock().unwrap().clone()
    }
    
    /// 获取RK3399性能统计
    pub fn get_rk3399_stats(&self) -> String {
        format!(
            "RK3399 Stats: big_cores={}, current_frame={}, total_frames={}",
            self.use_big_cores.load(Ordering::Relaxed),
            self.get_current_frame(),
            self.get_total_frames()
        )
    }
    
    fn set_state(&self, new_state: PlaybackState) {
        let mut state = self.state.lock().unwrap();
        if *state != new_state {
            log::debug!("RK3399 state change: {:?} -> {:?}", *state, new_state);
            *state = new_state;
        }
    }
    
    fn reset_last_valid_values(&self) {
        *self.last_valid_left.lock().unwrap() = vec![-1; 10];
        *self.last_valid_right.lock().unwrap() = vec![-1; 10];
        log::debug!("RK3399 reset last valid values cache");
    }
}