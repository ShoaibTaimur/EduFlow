package com.eduflow.dao;

import com.eduflow.model.User;
import com.eduflow.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {
  public User authenticate(String email, String password) {
    String sql = "SELECT u.user_id, u.name, u.email, u.role_id, r.role_name " +
        "FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
        "WHERE u.email = ? AND u.password = ?";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, email);
      ps.setString(2, password);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          User user = new User();
          user.setUserId(rs.getInt("user_id"));
          user.setName(rs.getString("name"));
          user.setEmail(rs.getString("email"));
          user.setRoleId(rs.getInt("role_id"));
          user.setRoleName(rs.getString("role_name"));
          return user;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Authentication failed", e);
    }
    return null;
  }
}
