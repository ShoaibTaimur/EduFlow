package com.eduflow.servlet;

import com.eduflow.dao.DaySettingDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

public class DaySettingServlet extends BaseServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!requireLogin(req, resp)) {
      return;
    }
    DaySettingDAO dao = new DaySettingDAO();
    Map<String, String> map = dao.getDayTypeMap();

    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");
    String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    StringBuilder sb = new StringBuilder();
    sb.append("{\"items\":[");
    for (int i = 0; i < days.length; i++) {
      if (i > 0) sb.append(",");
      String day = days[i];
      String type = map.get(day);
      if (type == null) type = "WORKING";
      sb.append("{\"day\":\"").append(day).append("\",\"type\":\"").append(type).append("\"}");
    }
    sb.append("]}");
    resp.getWriter().write(sb.toString());
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!requireRole(req, resp, "admin")) {
      return;
    }
    String day = req.getParameter("day");
    String type = req.getParameter("type");
    if (day == null || type == null || day.isBlank() || type.isBlank()) {
      resp.sendRedirect(req.getContextPath() + "/admin/approval?err=Missing+day+setting+input");
      return;
    }

    HttpSession session = req.getSession(false);
    int adminId = (int) session.getAttribute("user_id");
    DaySettingDAO dao = new DaySettingDAO();
    dao.upsertDaySetting(day, type, adminId);
    if ("WEEKEND".equalsIgnoreCase(type) || "HOLIDAY".equalsIgnoreCase(type)) {
      int deleted = dao.deleteApprovedSchedulesByDay(day);
      resp.sendRedirect(req.getContextPath() + "/admin/approval?msg=Day+setting+saved.+Deleted+" + deleted + "+approved+class(es)+for+" + day);
      return;
    }
    resp.sendRedirect(req.getContextPath() + "/admin/approval?msg=Day+setting+saved");
  }
}
