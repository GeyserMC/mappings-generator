package org.geysermc.resources;

import lombok.Value;

@Value
public class Pair<K, V> {

    K key;
    V value;
}
