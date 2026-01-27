use jni::sys::jint;
use android_logger::Config;
use log::LevelFilter;

pub mod sequence;
pub mod playback;
pub mod cache;
pub mod jni_bridge;

// 重新导出主要类型以便测试使用
pub use sequence::{SequenceParser, SequenceData};
pub use playback::PlaybackEngine;
pub use cache::CacheManager;

#[no_mangle]
pub extern "C" fn JNI_OnLoad(vm: jni::JavaVM, _reserved: *mut std::ffi::c_void) -> jint {
    android_logger::init_once(
        Config::default()
            .with_max_level(LevelFilter::Info)
            .with_tag("EvoBotNative")
    );
    
    log::info!("EvoBot Native Library loaded - RK3399 optimized version");
    
    // 初始化回调系统
    jni_bridge::callbacks::init_callback_system(vm);
    
    jni::JNIVersion::V6.into()
}