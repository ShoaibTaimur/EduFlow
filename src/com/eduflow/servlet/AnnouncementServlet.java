package com.eduflow.servlet;

import com.eduflow.dao.AnnouncementDAO;
import com.eduflow.model.Announcement;
import com.eduflow.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class AnnouncementServlet extends BaseServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
      if (i > 0)
        sb.append(",");
      sb.append("{\"id\":").append(a.getAnnouncementId())
          .append(",\"message\":\"").append(JsonUtil.escape(a.getMessage())).append("\"")
          .append("}");
    }
    sb.append("]}");
    resp.getWriter().write(sb.toString());
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
    AnnouncementDAO dao = new AnnouncementDAO();
    dao.addAnnouncement(message.trim());
    if ("teacher".equalsIgnoreCase(role)) {
      resp.sendRedirect(req.getContextPath() + "/teacher/request?msg=Announcement+published");
    } else {
      resp.sendRedirect(req.getContextPath() + "/admin/approval?msg=Announcement+published");
    }
  }
}
