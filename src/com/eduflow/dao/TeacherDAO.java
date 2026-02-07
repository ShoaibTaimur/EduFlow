package com.eduflow.dao;

import com.eduflow.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TeacherDAO {
  public Integer getTeacherIdByUserId(int userId) {
    String sql = "SELECT teacher_id FROM TEACHERS WHERE user_id=?";
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("teacher_id");
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch teacher id failed", e);
    }
    return null;
  }
}
