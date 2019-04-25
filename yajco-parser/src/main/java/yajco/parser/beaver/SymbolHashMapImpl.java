package yajco.parser.beaver;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import beaver.Symbol;

public class SymbolHashMapImpl<K, V> extends Symbol implements Map<K, V> {
    private final HashMap<K, V> map;

    public SymbolHashMapImpl(int i, float v) {
        map = new HashMap<K, V>(i, v);
    }

    public SymbolHashMapImpl(int i) {
        map = new HashMap<K, V>(i);
    }

    public SymbolHashMapImpl() {
        map = new HashMap<K, V>();
    }

    public SymbolHashMapImpl(Map<? extends K,? extends V> map) {
        this.map = new HashMap<K, V>(map);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K,? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }
}
