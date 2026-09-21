package com.aerospace.flightpath.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clean, zero-dependency JSON serialization and deserialization utility.
 * Keeps the project 100% self-contained without requiring external Maven/Gradle libraries.
 */
public class JsonHelper {

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Minimal JSON object parser for simple flat/array JSON payloads received via POST.
     */
    public static Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null) return map;
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return map;

        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        List<String> pairs = splitTopLevel(content, ',');

        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx == -1) continue;

            String rawKey = pair.substring(0, colonIdx).trim();
            String rawVal = pair.substring(colonIdx + 1).trim();

            String key = stripQuotes(rawKey);

            if (rawVal.startsWith("\"") && rawVal.endsWith("\"")) {
                map.put(key, stripQuotes(rawVal));
            } else if (rawVal.equals("true") || rawVal.equals("false")) {
                map.put(key, Boolean.parseBoolean(rawVal));
            } else if (rawVal.startsWith("[") && rawVal.endsWith("]")) {
                List<String> list = new ArrayList<>();
                String inner = rawVal.substring(1, rawVal.length() - 1).trim();
                if (!inner.isEmpty()) {
                    List<String> items = splitTopLevel(inner, ',');
                    for (String it : items) {
                        list.add(stripQuotes(it.trim()));
                    }
                }
                map.put(key, list);
            } else {
                try {
                    if (rawVal.contains(".")) {
                        map.put(key, Double.parseDouble(rawVal));
                    } else {
                        map.put(key, Long.parseLong(rawVal));
                    }
                } catch (NumberFormatException e) {
                    map.put(key, rawVal);
                }
            }
        }
        return map;
    }

    private static String stripQuotes(String s) {
        String str = s.trim();
        if (str.startsWith("\"") && str.endsWith("\"") && str.length() >= 2) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static List<String> splitTopLevel(String str, char delimiter) {
        List<String> result = new ArrayList<>();
        int depthBrackets = 0;
        int depthBraces = 0;
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (c == '"' && (i == 0 || str.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '[') depthBrackets++;
                else if (c == ']') depthBrackets--;
                else if (c == '{') depthBraces++;
                else if (c == '}') depthBraces--;
            }

            if (c == delimiter && depthBrackets == 0 && depthBraces == 0 && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }
}
