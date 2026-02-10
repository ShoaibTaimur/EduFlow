package com.eduflow.util;

import java.util.HashMap;
import java.util.Map;

public final class WebDataUtil {
  private WebDataUtil() {
  }

  public static String escapeJson(String s) {
    if (s == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"':
          sb.append("\\\"");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '\b':
          sb.append("\\b");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        default:
          if (c < 32) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
      }
    }
    return sb.toString();
  }

  public static Map<String, String> parseSimpleJson(String json) {
    Map<String, String> map = new HashMap<>();
    if (json == null) {
      return map;
    }

    String s = json.trim();
    if (s.startsWith("{") && s.endsWith("}")) {
      s = s.substring(1, s.length() - 1);
    }
    if (s.trim().isEmpty()) {
      return map;
    }

    String[] parts = s.split(",");
    for (String part : parts) {
      String[] kv = part.split(":", 2);
      if (kv.length != 2) {
        continue;
      }
      map.put(strip(kv[0]), strip(kv[1]));
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
