package org.geysermc.generator;

import lombok.Value;

@Value
public class Pair<K, V> {

    K key;
    V value;
}
