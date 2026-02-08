package com.eduflow.servlet;

import com.eduflow.util.DBUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataBrowserServlet extends BaseServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!requireRole(req, resp, "admin")) {
      return;
    }

    Map<String, String> queries = new LinkedHashMap<>();
    queries.put("USERS", "SELECT user_id, name, email, role_id FROM USERS ORDER BY user_id");
    queries.put("ROLES", "SELECT role_id, role_name FROM ROLES ORDER BY role_id");
    queries.put("DEPARTMENTS", "SELECT dept_id, dept_name FROM DEPARTMENTS ORDER BY dept_id");
    queries.put("BATCHES", "SELECT batch_id, dept_id, year FROM BATCHES ORDER BY batch_id");
    queries.put("SECTIONS", "SELECT section_id, batch_id, section_name FROM SECTIONS ORDER BY section_id");
    queries.put("TEACHERS", "SELECT teacher_id, user_id FROM TEACHERS ORDER BY teacher_id");
    queries.put("SUBJECTS", "SELECT subject_id, subject_code, name, dept_id FROM SUBJECTS ORDER BY subject_id");
    queries.put("CLASSROOMS", "SELECT room_id, room_name, capacity FROM CLASSROOMS ORDER BY room_id");
    queries.put("SCHEDULE", "SELECT schedule_id, dept_id, batch_id, section_id, subject_id, teacher_id, room_id, day, TO_CHAR(time_start,'HH24:MI') AS time_start, TO_CHAR(time_end,'HH24:MI') AS time_end, status FROM SCHEDULE ORDER BY schedule_id");
    queries.put("SCHEDULE_REQUESTS", "SELECT request_id, teacher_id, status, submitted_at, reviewed_at, admin_id FROM SCHEDULE_REQUESTS ORDER BY request_id DESC");
    queries.put("ANNOUNCEMENTS", "SELECT announcement_id, message, created_at FROM ANNOUNCEMENTS ORDER BY announcement_id DESC");
    queries.put("DAY_SETTINGS", "SELECT day_name, day_type, updated_by, updated_at FROM DAY_SETTINGS ORDER BY day_name");

    Map<String, List<Map<String, String>>> data = new LinkedHashMap<>();
    Map<String, List<String>> columns = new LinkedHashMap<>();

    try (Connection conn = DBUtil.getConnection()) {
      for (Map.Entry<String, String> entry : queries.entrySet()) {
        String table = entry.getKey();
        String sql = entry.getValue();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
          List<String> cols = new ArrayList<>();
          int count = rs.getMetaData().getColumnCount();
          for (int i = 1; i <= count; i++) {
            cols.add(rs.getMetaData().getColumnLabel(i));
          }
          columns.put(table, cols);

          List<Map<String, String>> rows = new ArrayList<>();
          while (rs.next()) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 1; i <= count; i++) {
              String key = cols.get(i - 1);
              String val = rs.getString(i);
              row.put(key, val == null ? "" : val);
            }
            rows.add(row);
          }
          data.put(table, rows);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Data browser failed", e);
    }

    req.setAttribute("data", data);
    req.setAttribute("columns", columns);
    req.getRequestDispatcher("/admin_data.jsp").forward(req, resp);
  }
}
