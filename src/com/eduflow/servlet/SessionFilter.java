package com.eduflow.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public class SessionFilter implements Filter {
  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    String path = req.getServletPath();
    if (path.equals("/login.jsp") || path.equals("/login") || path.startsWith("/css") || path.startsWith("/js")) {
      chain.doFilter(request, response);
      return;
    }

    HttpSession session = req.getSession(false);
    if (session == null || session.getAttribute("user_id") == null) {
      resp.sendRedirect(req.getContextPath() + "/login.jsp");
      return;
    }

    String role = (String) session.getAttribute("role");
    if (path.equals("/admin_panel.jsp") && !"admin".equalsIgnoreCase(role)) {
      resp.sendRedirect(req.getContextPath() + "/login.jsp");
      return;
    }
    if (path.equals("/teacher_panel.jsp") && !"teacher".equalsIgnoreCase(role)) {
      resp.sendRedirect(req.getContextPath() + "/login.jsp");
      return;
    }
    if (path.equals("/student_schedule.jsp") && !"student".equalsIgnoreCase(role)) {
      resp.sendRedirect(req.getContextPath() + "/login.jsp");
      return;
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
