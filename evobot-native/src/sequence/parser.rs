use std::io::{Read, Cursor};
use byteorder::{LittleEndian, ReadBytesExt};
use thiserror::Error;
use crate::sequence::SequenceData;

#[derive(Error, Debug)]
pub enum ParseError {
    #[error("Invalid magic number: {0}")]
    InvalidMagic(String),
    #[error("Invalid frame count: {0}")]
    InvalidFrameCount(u32),
    #[error("Invalid sample rate: {0}")]
    InvalidSampleRate(f32),
    #[error("Invalid duration: {0}")]
    InvalidDuration(f32),
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
}

pub struct SequenceParser;

impl SequenceParser {
    const MAGIC_NUMBER: &'static str = "EBS1";
    const HEADER_SIZE: usize = 96;
    const FRAME_SIZE: usize = 40;  // 20 joints × 2 bytes
    const NAME_SIZE: usize = 64;
    
    pub fn parse_from_bytes(data: &[u8]) -> Result<SequenceData, ParseError> {
        if data.len() < Self::HEADER_SIZE {
            return Err(ParseError::IoError(
                std::io::Error::new(std::io::ErrorKind::UnexpectedEof, "File too small")
            ));
        }
        
        let mut cursor = Cursor::new(data);
        
        // 解析文件头
        let header = Self::parse_header(&mut cursor)?;
        
        // 验证数据区大小
        let expected_data_size = header.frame_count as usize * Self::FRAME_SIZE;
        let remaining_bytes = data.len() - Self::HEADER_SIZE;
        if remaining_bytes < expected_data_size {
            return Err(ParseError::IoError(
                std::io::Error::new(std::io::ErrorKind::UnexpectedEof, "Incomplete data section")
            ));
        }
        
        // 解析帧数据
        let (left_arm_sequence, right_arm_sequence) = Self::parse_frames(&mut cursor, header.frame_count)?;
        
        Ok(SequenceData {
            name: header.name,
            sample_rate: header.sample_rate,
            total_duration: header.total_duration,
            total_frames: header.frame_count,
            compiled_at: header.compiled_at,
            left_arm_sequence,
            right_arm_sequence,
        })
    }
    
    fn parse_header(cursor: &mut Cursor<&[u8]>) -> Result<HeaderData, ParseError> {
        // 魔数验证
        let mut magic = [0u8; 4];
        cursor.read_exact(&mut magic)?;
        let magic_str = String::from_utf8_lossy(&magic);
        if magic_str != Self::MAGIC_NUMBER {
            return Err(ParseError::InvalidMagic(magic_str.to_string()));
        }
        
        // 读取头部字段
        let frame_count = cursor.read_u32::<LittleEndian>()?;
        if frame_count == 0 || frame_count > 100000 {
            return Err(ParseError::InvalidFrameCount(frame_count));
        }
        
        let sample_rate = cursor.read_f32::<LittleEndian>()?;
        if sample_rate <= 0.0 || sample_rate > 1000.0 {
            return Err(ParseError::InvalidSampleRate(sample_rate));
        }
        
        let total_duration = cursor.read_f32::<LittleEndian>()?;
        if total_duration <= 0.0 || total_duration > 3600.0 {
            return Err(ParseError::InvalidDuration(total_duration));
        }
        
        let compiled_at = cursor.read_u32::<LittleEndian>()?;
        
        // 跳过保留字段 (12 bytes)
        cursor.set_position(cursor.position() + 12);
        
        // 读取名称 (64 bytes, UTF-8)
        let mut name_bytes = [0u8; Self::NAME_SIZE];
        cursor.read_exact(&mut name_bytes)?;
        let name = String::from_utf8_lossy(&name_bytes)
            .trim_end_matches('\0')
            .to_string();
        
        log::debug!(
            "Header parsed: name={}, frames={}, rate={:.1}Hz, duration={:.3}s",
            name, frame_count, sample_rate, total_duration
        );
        
        Ok(HeaderData {
            frame_count,
            sample_rate,
            total_duration,
            compiled_at,
            name,
        })
    }
    
    fn parse_frames(cursor: &mut Cursor<&[u8]>, frame_count: u32) -> Result<(Vec<Vec<i32>>, Vec<Vec<i32>>), ParseError> {
        let mut left_arm_sequence = Vec::with_capacity(frame_count as usize);
        let mut right_arm_sequence = Vec::with_capacity(frame_count as usize);
        
        for _ in 0..frame_count {
            let mut left_arm = Vec::with_capacity(SequenceData::JOINTS_PER_ARM);
            let mut right_arm = Vec::with_capacity(SequenceData::JOINTS_PER_ARM);
            
            // 读取20个关节数据
            for joint in 0..20 {
                let pos = cursor.read_u16::<LittleEndian>()?;
                let value = if pos == SequenceData::HOLD_SENTINEL { 
                    -1 
                } else { 
                    pos as i32 
                };
                
                if joint < SequenceData::JOINTS_PER_ARM {
                    // 左臂关节 0-9
                    left_arm.push(value);
                } else {
                    // 右臂关节 0-9
                    right_arm.push(value);
                }
            }
            
            left_arm_sequence.push(left_arm);
            right_arm_sequence.push(right_arm);
        }
        
        log::debug!("Parsed {} frames successfully", frame_count);
        Ok((left_arm_sequence, right_arm_sequence))
    }
}

struct HeaderData {
    frame_count: u32,
    sample_rate: f32,
    total_duration: f32,
    compiled_at: u32,
    name: String,
}