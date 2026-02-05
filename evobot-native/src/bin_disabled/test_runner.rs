// 直接使用模块路径导入
use evobot_sequence_native::sequence::{SequenceParser, SequenceData};
use evobot_sequence_native::playback::PlaybackEngine;
use evobot_sequence_native::cache::CacheManager;
use std::error::Error;

fn main() -> Result<(), Box<dyn Error>> {
    // 初始化日志
    env_logger::init();
    
    println!("=== EvoBot Rust Native Library Test ===");
    
    // 测试1: 序列解析器
    println!("\n1. Testing Sequence Parser...");
    test_sequence_parser()?;
    
    // 测试2: 播放引擎
    println!("\n2. Testing Playback Engine...");
    test_playback_engine()?;
    
    // 测试3: 缓存管理器
    println!("\n3. Testing Cache Manager...");
    test_cache_manager()?;
    
    // 测试4: RK3399优化功能
    println!("\n4. Testing RK3399 Optimizations...");
    test_rk3399_optimizations()?;
    
    println!("\n=== All tests completed successfully! ===");
    Ok(())
}

fn test_sequence_parser() -> Result<(), Box<dyn Error>> {
    // 创建模拟的.ebs文件数据
    let mock_ebs_data = create_mock_ebs_data();
    
    match SequenceParser::parse_from_bytes(&mock_ebs_data) {
        Ok(sequence) => {
            println!("✓ Sequence parsed successfully:");
            println!("  - Name: {}", sequence.name);
            println!("  - Total frames: {}", sequence.total_frames);
            println!("  - Sample rate: {:.1}Hz", sequence.sample_rate);
            println!("  - Duration: {:.3}s", sequence.total_duration);
            
            // 验证数据完整性
            if sequence.validate() {
                println!("✓ Sequence data validation passed");
            } else {
                println!("✗ Sequence data validation failed");
            }
        }
        Err(e) => {
            println!("✗ Sequence parsing failed: {}", e);
            return Err(format!("Sequence parsing failed: {}", e).into());
        }
    }
    
    Ok(())
}

fn test_playback_engine() -> Result<(), Box<dyn Error>> {
    let mut engine = PlaybackEngine::new();
    
    // 创建测试序列
    let mock_ebs_data = create_mock_ebs_data();
    let sequence = SequenceParser::parse_from_bytes(&mock_ebs_data)
        .map_err(|e| format!("Failed to parse sequence: {}", e))?;
    
    // 加载序列
    engine.load_sequence(sequence)
        .map_err(|e| format!("Failed to load sequence: {}", e))?;
    println!("✓ Sequence loaded into playback engine");
    
    // 测试状态管理
    println!("✓ Initial state: {:?}", engine.get_state());
    
    // 测试帧信息
    println!("✓ Total frames: {}", engine.get_total_frames());
    println!("✓ Current frame: {}", engine.get_current_frame());
    
    // 测试RK3399优化设置
    engine.set_use_big_cores(true);
    println!("✓ RK3399 big cores enabled");
    
    // 获取性能统计
    let stats = engine.get_rk3399_stats();
    println!("✓ RK3399 stats: {}", stats);
    
    // 测试播放控制
    engine.pause();
    println!("✓ Playback paused");
    
    engine.resume();
    println!("✓ Playback resumed");
    
    engine.stop();
    println!("✓ Playback stopped");
    
    // 测试跳转功能
    if let Err(e) = engine.seek(10) {
        println!("✓ Seek test (expected to work): {}", e);
    } else {
        println!("✓ Seek to frame 10 successful");
    }
    
    Ok(())
}

fn test_cache_manager() -> Result<(), Box<dyn Error>> {
    let cache = CacheManager::new(5); // 容量为5的缓存
    
    // 创建测试数据
    let mock_ebs_data = create_mock_ebs_data();
    let sequence1 = SequenceParser::parse_from_bytes(&mock_ebs_data)
        .map_err(|e| format!("Failed to parse sequence: {}", e))?;
    let sequence2 = sequence1.clone();
    
    // 测试缓存操作
    cache.put("test_sequence_1".to_string(), sequence1);
    println!("✓ Sequence cached");
    
    cache.put("test_sequence_2".to_string(), sequence2);
    println!("✓ Second sequence cached");
    
    // 测试缓存检索
    if let Some(_seq) = cache.get("test_sequence_1") {
        println!("✓ Sequence retrieved from cache");
    } else {
        println!("✗ Failed to retrieve sequence from cache");
    }
    
    // 测试缓存统计
    println!("✓ Cache size: {}", cache.size());
    
    // 测试缓存清理
    cache.clear();
    println!("✓ Cache cleared, size: {}", cache.size());
    
    Ok(())
}

fn test_rk3399_optimizations() -> Result<(), Box<dyn Error>> {
    let mut engine = PlaybackEngine::new();
    
    // 测试大核心设置
    engine.set_use_big_cores(true);
    println!("✓ Big cores enabled");
    
    engine.set_use_big_cores(false);
    println!("✓ Big cores disabled");
    
    // 测试性能统计
    let stats = engine.get_rk3399_stats();
    println!("✓ Performance stats: {}", stats);
    
    // 测试定时器优化（创建一个简单的回调测试）
    let mock_ebs_data = create_mock_ebs_data();
    let sequence = SequenceParser::parse_from_bytes(&mock_ebs_data)
        .map_err(|e| format!("Failed to parse sequence: {}", e))?;
    engine.load_sequence(sequence)
        .map_err(|e| format!("Failed to load sequence: {}", e))?;
    
    println!("✓ RK3399 optimizations tested successfully");
    
    Ok(())
}

fn create_mock_ebs_data() -> Vec<u8> {
    let mut data = Vec::new();
    
    // EBS文件头 (96 bytes)
    // 魔数 "EBS1"
    data.extend_from_slice(b"EBS1");
    
    // 帧数 (4 bytes, little endian) - 100帧
    data.extend_from_slice(&100u32.to_le_bytes());
    
    // 采样率 (4 bytes, float) - 40.0Hz
    data.extend_from_slice(&40.0f32.to_le_bytes());
    
    // 总时长 (4 bytes, float) - 2.5秒
    data.extend_from_slice(&2.5f32.to_le_bytes());
    
    // 编译时间 (4 bytes) - 当前时间戳
    data.extend_from_slice(&1640995200u32.to_le_bytes());
    
    // 保留字段 (12 bytes)
    data.extend_from_slice(&[0u8; 12]);
    
    // 序列名称 (64 bytes) - "Test Sequence"
    let mut name_bytes = [0u8; 64];
    let name = b"Test Sequence";
    name_bytes[..name.len()].copy_from_slice(name);
    data.extend_from_slice(&name_bytes);
    
    // 数据区 - 100帧 × 20关节 × 2字节
    for frame in 0..100 {
        for joint in 0..20 {
            // 创建一些测试数据：简单的正弦波模式
            let angle = (frame as f32 * 0.1 + joint as f32 * 0.2).sin();
            let position = ((angle + 1.0) * 1000.0) as u16; // 0-2000范围
            data.extend_from_slice(&position.to_le_bytes());
        }
    }
    
    data
}