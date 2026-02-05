use jni::JNIEnv;
use jni::objects::{JClass, JObject, JByteArray};
use jni::sys::{jlong, jint, jboolean, jstring};
use std::sync::{Arc, Mutex};
use std::collections::HashMap;
use crate::playback::PlaybackEngine;
use crate::sequence::SequenceParser;
use crate::cache::CacheManager;
use crate::jni_bridge::callbacks::{
    register_player_listener, unregister_player_listener,
    call_java_frame_callback, call_java_complete_callback, call_java_error_callback,
    call_java_emergency_stop_callback
};

// 全局播放器实例管理
lazy_static::lazy_static! {
    static ref PLAYERS: Arc<Mutex<HashMap<jlong, Arc<Mutex<PlaybackEngine>>>>> = 
        Arc::new(Mutex::new(HashMap::new()));
    static ref CACHE_MANAGER: Arc<CacheManager> = Arc::new(CacheManager::new(10));
    static ref NEXT_ID: Arc<Mutex<jlong>> = Arc::new(Mutex::new(1));
    static ref JAVA_VM_INITIALIZED: Arc<Mutex<bool>> = Arc::new(Mutex::new(false));
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeCreate(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let player = Arc::new(Mutex::new(PlaybackEngine::new()));
    let id = {
        let mut next_id = NEXT_ID.lock().unwrap();
        let id = *next_id;
        *next_id += 1;
        id
    };
    
    PLAYERS.lock().unwrap().insert(id, player);
    log::info!("Created native player with ID: {}", id);
    id
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
) {
    // 注销回调监听器
    unregister_player_listener(player_id);
    
    // 移除播放器实例
    PLAYERS.lock().unwrap().remove(&player_id);
    log::info!("Destroyed native player with ID: {}", player_id);
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeRegisterListener(
    mut env: JNIEnv,
    _class: JClass,
    player_id: jlong,
    listener: JObject,
) -> jboolean {
    match register_player_listener(player_id, &mut env, &listener) {
        Ok(_) => {
            log::info!("Registered listener for player {}", player_id);
            true as jboolean
        }
        Err(e) => {
            log::error!("Failed to register listener for player {}: {}", player_id, e);
            false as jboolean
        }
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeUnregisterListener(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
) {
    unregister_player_listener(player_id);
    log::info!("Unregistered listener for player {}", player_id);
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeLoadSequenceFromBytes(
    mut env: JNIEnv,
    _class: JClass,
    player_id: jlong,
    data: JByteArray,
) -> jboolean {
    let players = PLAYERS.lock().unwrap();
    let player = match players.get(&player_id) {
        Some(p) => p.clone(),
        None => {
            log::error!("Player not found: {}", player_id);
            return false as jboolean;
        }
    };
    
    // 转换Java字节数组到Rust
    let data_len = env.get_array_length(&data).unwrap() as usize;
    let mut buffer = vec![0i8; data_len];
    env.get_byte_array_region(data, 0, &mut buffer).unwrap();
    
    // 转换为u8
    let byte_data: Vec<u8> = buffer.iter().map(|&x| x as u8).collect();
    
    // 解析序列
    match SequenceParser::parse_from_bytes(&byte_data) {
        Ok(sequence) => {
            // 缓存序列
            let cache_key = format!("sequence_{}", sequence.name);
            CACHE_MANAGER.put(cache_key, sequence.clone());
            
            match player.lock().unwrap().load_sequence(sequence) {
                Ok(_) => {
                    log::info!("Sequence loaded successfully");
                    true as jboolean
                }
                Err(e) => {
                    log::error!("Failed to load sequence: {}", e);
                    call_java_error_callback(player_id, &format!("{}", e));
                    false as jboolean
                }
            }
        }
        Err(e) => {
            log::error!("Failed to parse sequence: {}", e);
            call_java_error_callback(player_id, &format!("{}", e));
            false as jboolean
        }
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativePlayAsync(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
    frequency: jint,
) -> jboolean {
    let players = PLAYERS.lock().unwrap();
    let player = match players.get(&player_id) {
        Some(p) => p.clone(),
        None => {
            log::error!("Player not found: {}", player_id);
            return false as jboolean;
        }
    };
    drop(players); // 释放锁
    
    // 在新线程中启动异步播放
    let freq = frequency as u32;
    
    std::thread::spawn(move || {
        let rt = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .unwrap();
        rt.block_on(async {
            let result = {
                let engine = player.lock().unwrap();
                engine.play_with_callback(freq, move |left, right, frame| {
                    call_java_frame_callback(player_id, &left, &right, frame);
                }).await
            };
            
            match result {
                Ok(_) => {
                    call_java_complete_callback(player_id);
                    log::info!("Async playback completed for player {}", player_id);
                }
                Err(e) => {
                    call_java_error_callback(player_id, &e);
                    log::error!("Async playback failed for player {}: {}", player_id, e);
                }
            }
        });
    });
    
    log::info!("Started async playback for player {} at {}Hz", player_id, frequency);
    true as jboolean
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativePause(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
) {
    if let Some(player) = PLAYERS.lock().unwrap().get(&player_id) {
        player.lock().unwrap().pause();
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeResume(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
) {
    if let Some(player) = PLAYERS.lock().unwrap().get(&player_id) {
        player.lock().unwrap().resume();
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeStop(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
) {
    if let Some(player) = PLAYERS.lock().unwrap().get(&player_id) {
        player.lock().unwrap().stop();
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeEmergencyStop(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
) {
    if let Some(player) = PLAYERS.lock().unwrap().get(&player_id) {
        player.lock().unwrap().emergency_stop();
        call_java_emergency_stop_callback(player_id);
        log::warn!("Emergency stop executed for player {}", player_id);
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeSeek(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
    frame_index: jint,
) -> jboolean {
    if let Some(player) = PLAYERS.lock().unwrap().get(&player_id) {
        match player.lock().unwrap().seek(frame_index as u32) {
            Ok(_) => true as jboolean,
            Err(e) => {
                log::error!("Seek failed: {}", e);
                call_java_error_callback(player_id, &format!("{}", e));
                false as jboolean
            }
        }
    } else {
        false as jboolean
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeGetCurrentFrame(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
) -> jint {
    if let Some(player) = PLAYERS.lock().unwrap().get(&player_id) {
        player.lock().unwrap().get_current_frame() as jint
    } else {
        -1
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeGetTotalFrames(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
) -> jint {
    if let Some(player) = PLAYERS.lock().unwrap().get(&player_id) {
        player.lock().unwrap().get_total_frames() as jint
    } else {
        0
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeClearCache(
    _env: JNIEnv,
    _class: JClass,
) {
    CACHE_MANAGER.clear();
    log::info!("Native cache cleared");
}

// RK3399专用优化接口
#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeSetRK3399BigCores(
    _env: JNIEnv,
    _class: JClass,
    player_id: jlong,
    use_big_cores: jboolean,
) -> jboolean {
    if let Some(player) = PLAYERS.lock().unwrap().get(&player_id) {
        player.lock().unwrap().set_use_big_cores(use_big_cores != 0);
        log::info!("RK3399 big cores setting: {}", use_big_cores != 0);
        true as jboolean
    } else {
        false as jboolean
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeGetRK3399Stats(
    env: JNIEnv,
    _class: JClass,
    player_id: jlong,
) -> jstring {
    if let Some(player) = PLAYERS.lock().unwrap().get(&player_id) {
        let stats = player.lock().unwrap().get_rk3399_stats();
        match env.new_string(&stats) {
            Ok(jstr) => jstr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        }
    } else {
        std::ptr::null_mut()
    }
}

#[no_mangle]
pub extern "C" fn Java_com_evobot_sequence_EvoBotSequencePlayer_nativeGetPerformanceStats(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    // 返回全局性能统计
    let stats = format!(
        "RK3399 Global Stats: players={}, cache_size={}",
        PLAYERS.lock().unwrap().len(),
        CACHE_MANAGER.size()
    );
    
    match env.new_string(&stats) {
        Ok(jstr) => jstr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}