use jni::{JNIEnv, JavaVM, objects::{JObject, JValue, GlobalRef}};
use std::sync::{Arc, Mutex};
use std::collections::HashMap;

// 全局回调管理器
lazy_static::lazy_static! {
    static ref CALLBACK_MANAGER: Arc<Mutex<CallbackManager>> = 
        Arc::new(Mutex::new(CallbackManager::new()));
}

/// 回调管理器，负责管理Java回调对象和方法
pub struct CallbackManager {
    java_vm: Option<JavaVM>,
    listeners: HashMap<i64, CallbackInfo>,
}

/// 回调信息结构 - 使用String存储方法签名而不是原始指针
struct CallbackInfo {
    listener_ref: GlobalRef,
}

impl CallbackManager {
    pub fn new() -> Self {
        Self {
            java_vm: None,
            listeners: HashMap::new(),
        }
    }
    
    /// 初始化JavaVM引用
    pub fn init_java_vm(&mut self, java_vm: JavaVM) {
        self.java_vm = Some(java_vm);
        log::info!("CallbackManager initialized with JavaVM");
    }
    
    /// 注册回调监听器
    pub fn register_listener(
        &mut self,
        player_id: i64,
        env: &mut JNIEnv,
        listener: &JObject,
    ) -> Result<(), String> {
        // 创建全局引用
        let listener_ref = env.new_global_ref(listener)
            .map_err(|e| format!("Failed to create global ref: {}", e))?;
        
        // 存储回调信息
        let callback_info = CallbackInfo {
            listener_ref,
        };
        
        self.listeners.insert(player_id, callback_info);
        log::info!("Registered callback listener for player {}", player_id);
        Ok(())
    }
    
    /// 注销回调监听器
    pub fn unregister_listener(&mut self, player_id: i64) {
        if self.listeners.remove(&player_id).is_some() {
            log::info!("Unregistered callback listener for player {}", player_id);
        }
    }
    
    /// 调用帧数据回调
    pub fn call_frame_callback(
        &self,
        player_id: i64,
        left_arm: &[i32],
        right_arm: &[i32],
        frame_index: u32,
    ) -> Result<(), String> {
        let java_vm = self.java_vm.as_ref()
            .ok_or("JavaVM not initialized")?;
        
        let callback_info = self.listeners.get(&player_id)
            .ok_or("Listener not found")?;
        
        // 获取JNI环境
        let mut env = java_vm.attach_current_thread()
            .map_err(|e| format!("Failed to attach thread: {}", e))?;
        
        // 创建Java数组
        let left_array = env.new_int_array(left_arm.len() as i32)
            .map_err(|e| format!("Failed to create left array: {}", e))?;
        let right_array = env.new_int_array(right_arm.len() as i32)
            .map_err(|e| format!("Failed to create right array: {}", e))?;
        
        // 填充数组数据
        env.set_int_array_region(&left_array, 0, left_arm)
            .map_err(|e| format!("Failed to set left array: {}", e))?;
        env.set_int_array_region(&right_array, 0, right_arm)
            .map_err(|e| format!("Failed to set right array: {}", e))?;
        
        // 调用Java方法
        env.call_method(
            &callback_info.listener_ref,
            "onFrameData",
            "([I[II)V",
            &[
                JValue::Object(&left_array.into()),
                JValue::Object(&right_array.into()),
                JValue::Int(frame_index as i32),
            ],
        ).map_err(|e| format!("Failed to call onFrameData: {}", e))?;
        
        // 检查Java异常
        if env.exception_check().unwrap_or(false) {
            env.exception_describe().ok();
            env.exception_clear().ok();
            return Err("Java exception in onFrameData callback".to_string());
        }
        
        Ok(())
    }
    
    /// 调用完成回调
    pub fn call_complete_callback(&self, player_id: i64) -> Result<(), String> {
        let java_vm = self.java_vm.as_ref()
            .ok_or("JavaVM not initialized")?;
        
        let callback_info = self.listeners.get(&player_id)
            .ok_or("Listener not found")?;
        
        let mut env = java_vm.attach_current_thread()
            .map_err(|e| format!("Failed to attach thread: {}", e))?;
        
        env.call_method(
            &callback_info.listener_ref,
            "onComplete",
            "()V",
            &[],
        ).map_err(|e| format!("Failed to call onComplete: {}", e))?;
        
        if env.exception_check().unwrap_or(false) {
            env.exception_describe().ok();
            env.exception_clear().ok();
            return Err("Java exception in onComplete callback".to_string());
        }
        
        log::info!("Called onComplete callback for player {}", player_id);
        Ok(())
    }
    
    /// 调用错误回调
    pub fn call_error_callback(&self, player_id: i64, error: &str) -> Result<(), String> {
        let java_vm = self.java_vm.as_ref()
            .ok_or("JavaVM not initialized")?;
        
        let callback_info = self.listeners.get(&player_id)
            .ok_or("Listener not found")?;
        
        let mut env = java_vm.attach_current_thread()
            .map_err(|e| format!("Failed to attach thread: {}", e))?;
        
        let error_string = env.new_string(error)
            .map_err(|e| format!("Failed to create error string: {}", e))?;
        
        env.call_method(
            &callback_info.listener_ref,
            "onError",
            "(Ljava/lang/String;)V",
            &[JValue::Object(&error_string.into())],
        ).map_err(|e| format!("Failed to call onError: {}", e))?;
        
        if env.exception_check().unwrap_or(false) {
            env.exception_describe().ok();
            env.exception_clear().ok();
            return Err("Java exception in onError callback".to_string());
        }
        
        log::error!("Called onError callback for player {}: {}", player_id, error);
        Ok(())
    }
    
    /// 调用急停回调
    pub fn call_emergency_stop_callback(&self, player_id: i64) -> Result<(), String> {
        let java_vm = self.java_vm.as_ref()
            .ok_or("JavaVM not initialized")?;
        
        let callback_info = self.listeners.get(&player_id)
            .ok_or("Listener not found")?;
        
        let mut env = java_vm.attach_current_thread()
            .map_err(|e| format!("Failed to attach thread: {}", e))?;
        
        env.call_method(
            &callback_info.listener_ref,
            "onEmergencyStop",
            "()V",
            &[],
        ).map_err(|e| format!("Failed to call onEmergencyStop: {}", e))?;
        
        if env.exception_check().unwrap_or(false) {
            env.exception_describe().ok();
            env.exception_clear().ok();
            return Err("Java exception in onEmergencyStop callback".to_string());
        }
        
        log::warn!("Called onEmergencyStop callback for player {}", player_id);
        Ok(())
    }
}

// 公共接口函数

/// 初始化回调系统
pub fn init_callback_system(java_vm: JavaVM) {
    let mut manager = CALLBACK_MANAGER.lock().unwrap();
    manager.init_java_vm(java_vm);
}

/// 注册播放器回调监听器
pub fn register_player_listener(
    player_id: i64,
    env: &mut JNIEnv,
    listener: &JObject,
) -> Result<(), String> {
    let mut manager = CALLBACK_MANAGER.lock().unwrap();
    manager.register_listener(player_id, env, listener)
}

/// 注销播放器回调监听器
pub fn unregister_player_listener(player_id: i64) {
    let mut manager = CALLBACK_MANAGER.lock().unwrap();
    manager.unregister_listener(player_id);
}

/// 调用Java帧数据回调
pub fn call_java_frame_callback(
    player_id: i64,
    left_arm: &[i32],
    right_arm: &[i32],
    frame_index: u32,
) {
    let manager = CALLBACK_MANAGER.lock().unwrap();
    if let Err(e) = manager.call_frame_callback(player_id, left_arm, right_arm, frame_index) {
        log::error!("Frame callback failed for player {}: {}", player_id, e);
    }
}

/// 调用Java完成回调
pub fn call_java_complete_callback(player_id: i64) {
    let manager = CALLBACK_MANAGER.lock().unwrap();
    if let Err(e) = manager.call_complete_callback(player_id) {
        log::error!("Complete callback failed for player {}: {}", player_id, e);
    }
}

/// 调用Java错误回调
pub fn call_java_error_callback(player_id: i64, error: &str) {
    let manager = CALLBACK_MANAGER.lock().unwrap();
    if let Err(e) = manager.call_error_callback(player_id, error) {
        log::error!("Error callback failed for player {}: {}", player_id, e);
    }
}

/// 调用Java急停回调
pub fn call_java_emergency_stop_callback(player_id: i64) {
    let manager = CALLBACK_MANAGER.lock().unwrap();
    if let Err(e) = manager.call_emergency_stop_callback(player_id) {
        log::error!("Emergency stop callback failed for player {}: {}", player_id, e);
    }
}