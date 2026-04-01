package io.github.sinri.stark.logging.format;

import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LogEventFormattingSupport {

    private static final Comparator<String> STRING_COMPARATOR =
            Comparator.nullsFirst(String::compareTo);

    private LogEventFormattingSupport() {
    }

    static List<String> sortedMarkerNames(@Nullable List<Marker> markers) {
        if (markers == null || markers.isEmpty()) {
            return List.of();
        }
        return markers.stream()
                      .map(Marker::getName)
                      .distinct()
                      .sorted(STRING_COMPARATOR)
                      .toList();
    }

    static Map<String, String> sortedMdc(@Nullable Map<String, String> mdc) {
        if (mdc == null || mdc.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        mdc.entrySet().stream()
           .filter(entry -> entry.getValue() != null)
           .sorted(Map.Entry.comparingByKey(STRING_COMPARATOR))
           .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return ordered;
    }

    static List<KeyValuePair> sortedKeyValuePairs(@Nullable List<KeyValuePair> keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.isEmpty()) {
            return List.of();
        }
        ArrayList<KeyValuePair> ordered = new ArrayList<>(keyValuePairs);
        ordered.sort(Comparator.comparing(kv -> kv.key, STRING_COMPARATOR));
        return ordered;
    }

    static String renderEntries(Map<String, String> entries) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (index++ > 0) {
                sb.append(' ');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    static String renderKeyValuePairs(List<KeyValuePair> keyValuePairs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyValuePairs.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            KeyValuePair kv = keyValuePairs.get(i);
            sb.append(kv.key).append('=').append(kv.value);
        }
        return sb.toString();
    }
}
