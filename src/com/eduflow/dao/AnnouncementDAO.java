package com.eduflow.dao;

import com.eduflow.model.Announcement;
import com.eduflow.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDAO {
  public AnnouncementDAO() {
    ensureAuthorColumn();
  }

  public boolean addAnnouncement(String message, int postedByUserId) {
    String sql = "INSERT INTO ANNOUNCEMENTS (message, posted_by_user_id) VALUES (?, ?)";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, message);
      ps.setInt(2, postedByUserId);
      return ps.executeUpdate() == 1;
    } catch (Exception e) {
      throw new RuntimeException("Insert announcement failed", e);
    }
  }

  public List<Announcement> getLatestAnnouncements() {
    String sql = "SELECT a.announcement_id, a.message, a.created_at, " +
        "u.name AS announcer_name, r.role_name AS announcer_role " +
        "FROM ANNOUNCEMENTS a " +
        "LEFT JOIN USERS u ON a.posted_by_user_id = u.user_id " +
        "LEFT JOIN ROLES r ON u.role_id = r.role_id " +
        "ORDER BY a.created_at DESC";
    List<Announcement> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        list.add(mapAnnouncement(rs));
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch announcements failed", e);
    }
    return list;
  }

  public int deleteAllAnnouncements() {
    String sql = "DELETE FROM ANNOUNCEMENTS";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      return ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("Delete announcements failed", e);
    }
  }

  private Announcement mapAnnouncement(ResultSet rs) throws Exception {
    Announcement a = new Announcement();
    a.setAnnouncementId(rs.getInt("announcement_id"));
    a.setMessage(rs.getString("message"));
    a.setCreatedAt(rs.getTimestamp("created_at"));
    a.setAnnouncerName(rs.getString("announcer_name"));
    a.setAnnouncerRole(rs.getString("announcer_role"));
    return a;
  }

  private void ensureAuthorColumn() {
    String sql = "ALTER TABLE ANNOUNCEMENTS ADD (posted_by_user_id NUMBER)";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.executeUpdate();
    } catch (SQLException e) {
      // ORA-01430: column being added already exists in table
      if (e.getErrorCode() != 1430) {
        throw new RuntimeException("Ensure announcement author column failed", e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Ensure announcement author column failed", e);
    }
  }
}
