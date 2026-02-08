package com.eduflow.dao;

import com.eduflow.model.Announcement;
import com.eduflow.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDAO {
  public boolean addAnnouncement(String message) {
    String sql = "INSERT INTO ANNOUNCEMENTS (message) VALUES (?)";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, message);
      return ps.executeUpdate() == 1;
    } catch (Exception e) {
      throw new RuntimeException("Insert announcement failed", e);
    }
  }

  public List<Announcement> getLatestAnnouncements() {
    String sql = "SELECT announcement_id, message, created_at FROM ANNOUNCEMENTS ORDER BY created_at DESC";
    List<Announcement> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        Announcement a = new Announcement();
        a.setAnnouncementId(rs.getInt("announcement_id"));
        a.setMessage(rs.getString("message"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        list.add(a);
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch announcements failed", e);
    }
    return list;
  }
}
