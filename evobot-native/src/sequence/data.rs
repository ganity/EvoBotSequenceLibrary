use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SequenceData {
    pub name: String,
    pub sample_rate: f32,
    pub total_duration: f32,
    pub total_frames: u32,
    pub compiled_at: u32,
    pub left_arm_sequence: Vec<Vec<i32>>,  // [frame][joint]
    pub right_arm_sequence: Vec<Vec<i32>>, // [frame][joint]
}

impl SequenceData {
    pub const JOINTS_PER_ARM: usize = 10;
    pub const HOLD_SENTINEL: u16 = 0xFFFF;
    
    pub fn new() -> Self {
        Self {
            name: String::new(),
            sample_rate: 0.0,
            total_duration: 0.0,
            total_frames: 0,
            compiled_at: 0,
            left_arm_sequence: Vec::new(),
            right_arm_sequence: Vec::new(),
        }
    }
    
    pub fn validate(&self) -> bool {
        self.total_frames > 0 
            && self.sample_rate > 0.0 
            && self.left_arm_sequence.len() == self.total_frames as usize
            && self.right_arm_sequence.len() == self.total_frames as usize
            && self.left_arm_sequence.iter().all(|frame| frame.len() == Self::JOINTS_PER_ARM)
            && self.right_arm_sequence.iter().all(|frame| frame.len() == Self::JOINTS_PER_ARM)
    }
    
    pub fn get_frame_data(&self, frame_index: usize) -> Option<(Vec<i32>, Vec<i32>)> {
        if frame_index >= self.total_frames as usize {
            return None;
        }
        
        Some((
            self.left_arm_sequence[frame_index].clone(),
            self.right_arm_sequence[frame_index].clone(),
        ))
    }
    
    pub fn get_info(&self) -> String {
        format!(
            "Sequence: {} | Frames: {} | Rate: {:.1}Hz | Duration: {:.3}s",
            self.name, self.total_frames, self.sample_rate, self.total_duration
        )
    }
}