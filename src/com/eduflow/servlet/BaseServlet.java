package com.eduflow.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Time;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class BaseServlet extends HttpServlet {
  protected static final List<String> WEEK_DAYS = Collections.unmodifiableList(
      Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));

  protected boolean requireLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    HttpSession session = req.getSession(false);
    if (session == null || session.getAttribute("user_id") == null) {
      resp.sendRedirect(req.getContextPath() + "/login.jsp");
      return false;
    }
    return true;
  }

  protected boolean requireRole(HttpServletRequest req, HttpServletResponse resp, String role) throws IOException {
    if (!requireLogin(req, resp)) {
      return false;
    }
    HttpSession session = req.getSession(false);
    String userRole = (String) session.getAttribute("role");
    if (userRole == null || !userRole.equalsIgnoreCase(role)) {
      resp.sendRedirect(req.getContextPath() + "/login.jsp");
      return false;
    }
    return true;
  }

  protected int parseIntOrZero(String value) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return 0;
    }
  }

  protected int parseIntOrDefault(String value, int defaultVal) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return defaultVal;
    }
  }

  protected Integer parseNullableInt(String value) {
    try {
      if (value == null || value.trim().isEmpty()) {
        return null;
      }
      return Integer.parseInt(value.trim());
    } catch (Exception e) {
      return null;
    }
  }

  protected int pickInt(String preferred, String fallback) {
    Integer val = parseNullableInt(preferred);
    if (val != null) {
      return val;
    }
    val = parseNullableInt(fallback);
    return val == null ? 0 : val;
  }

  protected String pickString(String preferred, String fallback) {
    if (preferred != null && !preferred.trim().isEmpty()) {
      return preferred;
    }
    return fallback;
  }

  protected Time toTime(String hhmm) {
    if (hhmm == null || hhmm.isEmpty()) {
      return null;
    }
    return Time.valueOf(hhmm + ":00");
  }

  protected void applyFeedbackAttributes(HttpServletRequest req) {
    if (req.getAttribute("message") == null && req.getParameter("msg") != null) {
      req.setAttribute("message", req.getParameter("msg"));
    }
    if (req.getAttribute("error") == null && req.getParameter("err") != null) {
      req.setAttribute("error", req.getParameter("err"));
    }
  }
}
