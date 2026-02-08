package com.eduflow.dao;

import com.eduflow.model.ScheduleRequest;
import com.eduflow.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {
  public boolean insertRequest(int teacherId, String proposedData) {
    String sql = "INSERT INTO SCHEDULE_REQUESTS (teacher_id, proposed_data, status) VALUES (?, ?, 'PENDING')";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, teacherId);
      ps.setString(2, proposedData);
      return ps.executeUpdate() == 1;
    } catch (Exception e) {
      throw new RuntimeException("Insert request failed", e);
    }
  }

  public boolean updateStatus(int requestId, String status, int adminId) {
    String sql = "UPDATE SCHEDULE_REQUESTS SET status=?, reviewed_at=SYSTIMESTAMP, admin_id=? WHERE request_id=?";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, status);
      ps.setInt(2, adminId);
      ps.setInt(3, requestId);
      return ps.executeUpdate() == 1;
    } catch (Exception e) {
      throw new RuntimeException("Update request status failed", e);
    }
  }

  public List<ScheduleRequest> getPendingRequests() {
    String sql = "SELECT request_id, teacher_id, proposed_data, status, submitted_at FROM SCHEDULE_REQUESTS WHERE status='PENDING' ORDER BY submitted_at DESC";
    List<ScheduleRequest> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        ScheduleRequest r = new ScheduleRequest();
        r.setRequestId(rs.getInt("request_id"));
        r.setTeacherId(rs.getInt("teacher_id"));
        r.setProposedData(rs.getString("proposed_data"));
        r.setStatus(rs.getString("status"));
        r.setSubmittedAt(rs.getTimestamp("submitted_at"));
        list.add(r);
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch requests failed", e);
    }
    return list;
  }

  public List<ScheduleRequest> getRequestsByTeacher(int teacherId) {
    String sql = "SELECT request_id, teacher_id, proposed_data, status, submitted_at, reviewed_at " +
        "FROM SCHEDULE_REQUESTS WHERE teacher_id=? ORDER BY submitted_at DESC";
    List<ScheduleRequest> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, teacherId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ScheduleRequest r = new ScheduleRequest();
          r.setRequestId(rs.getInt("request_id"));
          r.setTeacherId(rs.getInt("teacher_id"));
          r.setProposedData(rs.getString("proposed_data"));
          r.setStatus(rs.getString("status"));
          r.setSubmittedAt(rs.getTimestamp("submitted_at"));
          r.setReviewedAt(rs.getTimestamp("reviewed_at"));
          list.add(r);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch teacher requests failed", e);
    }
    return list;
  }

  public ScheduleRequest getRequestById(int requestId) {
    String sql = "SELECT request_id, teacher_id, proposed_data, status, submitted_at, reviewed_at " +
        "FROM SCHEDULE_REQUESTS WHERE request_id=?";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, requestId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          ScheduleRequest r = new ScheduleRequest();
          r.setRequestId(rs.getInt("request_id"));
          r.setTeacherId(rs.getInt("teacher_id"));
          r.setProposedData(rs.getString("proposed_data"));
          r.setStatus(rs.getString("status"));
          r.setSubmittedAt(rs.getTimestamp("submitted_at"));
          r.setReviewedAt(rs.getTimestamp("reviewed_at"));
          return r;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch request failed", e);
    }
    return null;
  }

  public int deleteRequestsByTeacher(int teacherId) {
    String sql = "DELETE FROM SCHEDULE_REQUESTS WHERE teacher_id=?";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, teacherId);
      return ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("Delete requests failed", e);
    }
  }
}
