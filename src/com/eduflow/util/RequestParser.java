package com.eduflow.util;

import java.util.HashMap;
import java.util.Map;

public class RequestParser {
  public static Map<String, String> parseSimpleJson(String json) {
    Map<String, String> map = new HashMap<>();
    if (json == null) return map;
    String s = json.trim();
    if (s.startsWith("{") && s.endsWith("}")) {
      s = s.substring(1, s.length() - 1);
    }
    if (s.trim().isEmpty()) return map;

    String[] parts = s.split(",");
    for (String part : parts) {
      String[] kv = part.split(":", 2);
      if (kv.length != 2) continue;
      String key = strip(kv[0]);
      String val = strip(kv[1]);
      map.put(key, val);
    }
    return map;
  }

  private static String strip(String s) {
    String v = s.trim();
    if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
      return v.substring(1, v.length() - 1);
    }
    return v;
  }
}
