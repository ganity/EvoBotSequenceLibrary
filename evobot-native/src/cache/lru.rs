use std::collections::HashMap;
use std::hash::Hash;

pub struct LRUCache<K, V> {
    capacity: usize,
    map: HashMap<K, (V, usize)>,
    access_order: Vec<K>,
    current_time: usize,
}

impl<K, V> LRUCache<K, V>
where
    K: Clone + Eq + Hash,
{
    pub fn new(capacity: usize) -> Self {
        Self {
            capacity,
            map: HashMap::new(),
            access_order: Vec::new(),
            current_time: 0,
        }
    }
    
    pub fn get(&mut self, key: &K) -> Option<&V> {
        if let Some((value, access_time)) = self.map.get_mut(key) {
            self.current_time += 1;
            *access_time = self.current_time;
            Some(value)
        } else {
            None
        }
    }
    
    pub fn put(&mut self, key: K, value: V) {
        self.current_time += 1;
        
        if self.map.contains_key(&key) {
            // 更新现有键
            self.map.insert(key, (value, self.current_time));
        } else {
            // 新键
            if self.map.len() >= self.capacity {
                self.evict_lru();
            }
            self.map.insert(key, (value, self.current_time));
        }
    }
    
    pub fn remove(&mut self, key: &K) -> Option<V> {
        self.map.remove(key).map(|(value, _)| value)
    }
    
    pub fn clear(&mut self) {
        self.map.clear();
        self.access_order.clear();
        self.current_time = 0;
    }
    
    pub fn len(&self) -> usize {
        self.map.len()
    }
    
    pub fn is_empty(&self) -> bool {
        self.map.is_empty()
    }
    
    pub fn capacity(&self) -> usize {
        self.capacity
    }
    
    fn evict_lru(&mut self) {
        if self.map.is_empty() {
            return;
        }
        
        // 找到最久未使用的键
        let mut lru_key = None;
        let mut min_access_time = usize::MAX;
        
        for (key, (_, access_time)) in &self.map {
            if *access_time < min_access_time {
                min_access_time = *access_time;
                lru_key = Some(key.clone());
            }
        }
        
        if let Some(key) = lru_key {
            self.map.remove(&key);
            log::debug!("Evicted LRU key from cache");
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_lru_basic_operations() {
        let mut cache = LRUCache::new(2);
        
        cache.put("a", 1);
        cache.put("b", 2);
        
        assert_eq!(cache.get(&"a"), Some(&1));
        assert_eq!(cache.get(&"b"), Some(&2));
        assert_eq!(cache.len(), 2);
    }
    
    #[test]
    fn test_lru_eviction() {
        let mut cache = LRUCache::new(2);
        
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3); // Should evict "a"
        
        assert_eq!(cache.get(&"a"), None);
        assert_eq!(cache.get(&"b"), Some(&2));
        assert_eq!(cache.get(&"c"), Some(&3));
    }
}