package com.eduflow.servlet;

import com.eduflow.dao.UserDAO;
import com.eduflow.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public class LoginServlet extends BaseServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    req.getRequestDispatcher("/login.jsp").forward(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String email = req.getParameter("email");
    String password = req.getParameter("password");

    UserDAO userDAO = new UserDAO();
    User user = userDAO.authenticate(email, password);
    if (user == null) {
      req.setAttribute("error", "Invalid email or password");
      req.getRequestDispatcher("/login.jsp").forward(req, resp);
      return;
    }

    HttpSession session = req.getSession(true);
    session.setAttribute("user_id", user.getUserId());
    session.setAttribute("role", user.getRoleName());

    String role = user.getRoleName().toLowerCase();
    if ("admin".equals(role)) {
      resp.sendRedirect(req.getContextPath() + "/admin/approval");
    } else if ("teacher".equals(role)) {
      resp.sendRedirect(req.getContextPath() + "/teacher/request");
    } else {
      resp.sendRedirect(req.getContextPath() + "/student/schedule");
    }
  }
}
