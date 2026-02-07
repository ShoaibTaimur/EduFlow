package com.eduflow.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {
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
}
