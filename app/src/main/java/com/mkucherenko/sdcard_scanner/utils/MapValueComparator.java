package com.mkucherenko.sdcard_scanner.utils;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by Zim on 4/24/2016.
 */
public class MapValueComparator<K, V extends Comparable<? super V>>  implements Comparator<Map.Entry<K, V>>{
    @Override
    public int compare(Map.Entry<K, V> entry1, Map.Entry<K, V> entry2) {
        return entry1.getValue().compareTo(entry2.getValue());
    }
}
