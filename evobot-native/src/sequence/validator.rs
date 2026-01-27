use crate::sequence::SequenceData;

pub struct SequenceValidator;

impl SequenceValidator {
    pub fn validate_sequence(data: &SequenceData) -> Result<(), String> {
        // 基本字段验证
        if data.total_frames == 0 {
            return Err("Total frames cannot be zero".to_string());
        }
        
        if data.sample_rate <= 0.0 || data.sample_rate > 1000.0 {
            return Err(format!("Invalid sample rate: {}", data.sample_rate));
        }
        
        if data.total_duration <= 0.0 {
            return Err(format!("Invalid duration: {}", data.total_duration));
        }
        
        // 序列长度验证
        if data.left_arm_sequence.len() != data.total_frames as usize {
            return Err(format!(
                "Left arm sequence length mismatch: expected {}, got {}",
                data.total_frames, data.left_arm_sequence.len()
            ));
        }
        
        if data.right_arm_sequence.len() != data.total_frames as usize {
            return Err(format!(
                "Right arm sequence length mismatch: expected {}, got {}",
                data.total_frames, data.right_arm_sequence.len()
            ));
        }
        
        // 关节数量验证
        for (frame_idx, frame) in data.left_arm_sequence.iter().enumerate() {
            if frame.len() != SequenceData::JOINTS_PER_ARM {
                return Err(format!(
                    "Left arm frame {} has {} joints, expected {}",
                    frame_idx, frame.len(), SequenceData::JOINTS_PER_ARM
                ));
            }
        }
        
        for (frame_idx, frame) in data.right_arm_sequence.iter().enumerate() {
            if frame.len() != SequenceData::JOINTS_PER_ARM {
                return Err(format!(
                    "Right arm frame {} has {} joints, expected {}",
                    frame_idx, frame.len(), SequenceData::JOINTS_PER_ARM
                ));
            }
        }
        
        // 关节值范围验证
        Self::validate_joint_values(&data.left_arm_sequence, "left arm")?;
        Self::validate_joint_values(&data.right_arm_sequence, "right arm")?;
        
        log::debug!("Sequence validation passed: {}", data.get_info());
        Ok(())
    }
    
    fn validate_joint_values(sequence: &[Vec<i32>], arm_name: &str) -> Result<(), String> {
        for (frame_idx, frame) in sequence.iter().enumerate() {
            for (joint_idx, &value) in frame.iter().enumerate() {
                if value != -1 && (value < 0 || value > 4095) {
                    return Err(format!(
                        "{} frame {} joint {} has invalid value: {} (must be -1 or 0-4095)",
                        arm_name, frame_idx, joint_idx, value
                    ));
                }
            }
        }
        Ok(())
    }
}