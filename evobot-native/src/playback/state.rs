#[derive(Debug, Clone, PartialEq)]
pub enum PlaybackState {
    Idle,
    Loading,
    Ready,
    Playing,
    Paused,
    Stopped,
    Error(String),
}

impl PlaybackState {
    pub fn can_play(&self) -> bool {
        matches!(self, PlaybackState::Ready | PlaybackState::Paused)
    }
    
    pub fn can_pause(&self) -> bool {
        matches!(self, PlaybackState::Playing)
    }
    
    pub fn can_resume(&self) -> bool {
        matches!(self, PlaybackState::Paused)
    }
    
    pub fn can_stop(&self) -> bool {
        matches!(self, PlaybackState::Playing | PlaybackState::Paused)
    }
    
    pub fn get_description(&self) -> &'static str {
        match self {
            PlaybackState::Idle => "Idle",
            PlaybackState::Loading => "Loading",
            PlaybackState::Ready => "Ready",
            PlaybackState::Playing => "Playing",
            PlaybackState::Paused => "Paused",
            PlaybackState::Stopped => "Stopped",
            PlaybackState::Error(_) => "Error",
        }
    }
}