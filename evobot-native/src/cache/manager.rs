use std::sync::{Arc, Mutex};
use crate::cache::LRUCache;
use crate::sequence::SequenceData;

#[derive(Debug, Clone)]
pub struct CacheStats {
    pub total_requests: u64,
    pub cache_hits: u64,
    pub cache_misses: u64,
    pub current_size: usize,
    pub max_size: usize,
}

impl CacheStats {
    pub fn hit_rate(&self) -> f64 {
        if self.total_requests == 0 {
            0.0
        } else {
            self.cache_hits as f64 / self.total_requests as f64
        }
    }
}

pub struct CacheManager {
    cache: Arc<Mutex<LRUCache<String, SequenceData>>>,
    stats: Arc<Mutex<CacheStats>>,
}

impl CacheManager {
    pub fn new(capacity: usize) -> Self {
        Self {
            cache: Arc::new(Mutex::new(LRUCache::new(capacity))),
            stats: Arc::new(Mutex::new(CacheStats {
                total_requests: 0,
                cache_hits: 0,
                cache_misses: 0,
                current_size: 0,
                max_size: capacity,
            })),
        }
    }
    
    pub fn get(&self, key: &str) -> Option<SequenceData> {
        let mut cache = self.cache.lock().unwrap();
        let mut stats = self.stats.lock().unwrap();
        
        stats.total_requests += 1;
        
        if let Some(sequence) = cache.get(&key.to_string()) {
            stats.cache_hits += 1;
            log::debug!("Cache hit for key: {}", key);
            Some(sequence.clone())
        } else {
            stats.cache_misses += 1;
            log::debug!("Cache miss for key: {}", key);
            None
        }
    }
    
    pub fn put(&self, key: String, sequence: SequenceData) {
        let mut cache = self.cache.lock().unwrap();
        let mut stats = self.stats.lock().unwrap();
        
        cache.put(key.clone(), sequence);
        stats.current_size = cache.len();
        
        log::debug!("Cached sequence: {} (size: {})", key, stats.current_size);
    }
    
    pub fn remove(&self, key: &str) -> bool {
        let mut cache = self.cache.lock().unwrap();
        let mut stats = self.stats.lock().unwrap();
        
        let removed = cache.remove(&key.to_string()).is_some();
        stats.current_size = cache.len();
        
        if removed {
            log::debug!("Removed from cache: {}", key);
        }
        
        removed
    }
    
    pub fn clear(&self) {
        let mut cache = self.cache.lock().unwrap();
        let mut stats = self.stats.lock().unwrap();
        
        cache.clear();
        stats.current_size = 0;
        stats.total_requests = 0;
        stats.cache_hits = 0;
        stats.cache_misses = 0;
        
        log::info!("Cache cleared");
    }
    
    pub fn get_stats(&self) -> CacheStats {
        let cache = self.cache.lock().unwrap();
        let mut stats = self.stats.lock().unwrap();
        
        stats.current_size = cache.len();
        stats.clone()
    }
    
    pub fn contains_key(&self, key: &str) -> bool {
        let mut cache = self.cache.lock().unwrap();
        cache.get(&key.to_string()).is_some()
    }
    
    pub fn size(&self) -> usize {
        let cache = self.cache.lock().unwrap();
        cache.len()
    }
    
    pub fn capacity(&self) -> usize {
        let cache = self.cache.lock().unwrap();
        cache.capacity()
    }
    
    pub fn is_empty(&self) -> bool {
        let cache = self.cache.lock().unwrap();
        cache.is_empty()
    }
}