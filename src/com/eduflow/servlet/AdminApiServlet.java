package com.eduflow.servlet;

import com.eduflow.dao.AnnouncementDAO;
import com.eduflow.dao.DayPolicyDAO;
import com.eduflow.model.Announcement;
import com.eduflow.util.WebDataUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AdminApiServlet extends BaseServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String path = req.getServletPath();
    if ("/announcements".equals(path)) {
      handleAnnouncementsGet(req, resp);
      return;
    }
    if ("/day-settings".equals(path)) {
      handleDaySettingsGet(req, resp);
      return;
    }
    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String path = req.getServletPath();
    if ("/announcements".equals(path)) {
      handleAnnouncementsPost(req, resp);
      return;
    }
    if ("/day-settings".equals(path)) {
      handleDaySettingsPost(req, resp);
      return;
    }
    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  private void handleAnnouncementsGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    if (!requireLogin(req, resp)) {
      return;
    }

    AnnouncementDAO dao = new AnnouncementDAO();
    List<Announcement> list = dao.getLatestAnnouncements();

    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");
    StringBuilder sb = new StringBuilder();
    sb.append("{\"items\":[");
    for (int i = 0; i < list.size(); i++) {
      Announcement a = list.get(i);
      if (i > 0) {
        sb.append(",");
      }
      sb.append("{\"id\":").append(a.getAnnouncementId())
          .append(",\"message\":\"").append(WebDataUtil.escapeJson(a.getMessage())).append("\"")
          .append(",\"announcerName\":\"").append(WebDataUtil.escapeJson(a.getAnnouncerName())).append("\"")
          .append(",\"announcerRole\":\"").append(WebDataUtil.escapeJson(a.getAnnouncerRole())).append("\"")
          .append("}");
    }
    sb.append("]}");
    resp.getWriter().write(sb.toString());
  }

  private void handleAnnouncementsPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    if (!requireLogin(req, resp)) {
      return;
    }

    HttpSession session = req.getSession(false);
    String role = (String) session.getAttribute("role");
    if (!"admin".equalsIgnoreCase(role) && !"teacher".equalsIgnoreCase(role)) {
      resp.sendRedirect(req.getContextPath() + "/login.jsp");
      return;
    }

    String message = req.getParameter("message");
    if (message == null || message.trim().isEmpty()) {
      if ("teacher".equalsIgnoreCase(role)) {
        resp.sendRedirect(req.getContextPath() + "/teacher/request?err=Announcement+message+is+required");
      } else {
        resp.sendRedirect(req.getContextPath() + "/admin/approval?err=Announcement+message+is+required");
      }
      return;
    }

    int userId = (int) session.getAttribute("user_id");
    AnnouncementDAO dao = new AnnouncementDAO();
    dao.addAnnouncement(message.trim(), userId);
    if ("teacher".equalsIgnoreCase(role)) {
      resp.sendRedirect(req.getContextPath() + "/teacher/request?msg=Announcement+published");
    } else {
      resp.sendRedirect(req.getContextPath() + "/admin/approval?msg=Announcement+published");
    }
  }

  private void handleDaySettingsGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    if (!requireLogin(req, resp)) {
      return;
    }

    DayPolicyDAO dao = new DayPolicyDAO();
    Map<String, String> map = dao.getDayTypeMap();

    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");
    StringBuilder sb = new StringBuilder();
    sb.append("{\"items\":[");
    for (int i = 0; i < WEEK_DAYS.size(); i++) {
      if (i > 0) {
        sb.append(",");
      }
      String day = WEEK_DAYS.get(i);
      String type = map.get(day);
      if (type == null) {
        type = "WORKING";
      }
      sb.append("{\"day\":\"").append(day).append("\",\"type\":\"").append(type).append("\"}");
    }
    sb.append("]}");
    resp.getWriter().write(sb.toString());
  }

  private void handleDaySettingsPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
    DayPolicyDAO dao = new DayPolicyDAO();
    dao.upsertDaySetting(day, type, adminId);
    if ("WEEKEND".equalsIgnoreCase(type) || "HOLIDAY".equalsIgnoreCase(type)) {
      int deleted = dao.deleteApprovedSchedulesByDay(day);
      resp.sendRedirect(req.getContextPath() + "/admin/approval?msg=Day+setting+saved.+Deleted+" + deleted
          + "+approved+class(es)+for+" + day);
      return;
    }

    resp.sendRedirect(req.getContextPath() + "/admin/approval?msg=Day+setting+saved");
  }
}
