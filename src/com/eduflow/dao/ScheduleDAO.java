package com.eduflow.dao;

import com.eduflow.model.ScheduleView;
import com.eduflow.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDAO {
  public boolean hasStudentConflict(int deptId, int batchId, int sectionId, String day, Time start, Time end) {
    String sql = "SELECT COUNT(*) FROM SCHEDULE " +
                 "WHERE dept_id=? AND batch_id=? AND section_id=? AND day=? AND status='APPROVED' " +
                 "AND (? < TO_CHAR(time_end,'HH24:MI') AND ? > TO_CHAR(time_start,'HH24:MI'))";
    return countConflicts(sql, new Object[]{deptId, batchId, sectionId, day, toHHmm(start), toHHmm(end)});
  }

  public boolean hasRoomClash(int roomId, String day, Time start, Time end) {
    String sql = "SELECT COUNT(*) FROM SCHEDULE " +
                 "WHERE room_id=? AND day=? AND status='APPROVED' " +
                 "AND (? < TO_CHAR(time_end,'HH24:MI') AND ? > TO_CHAR(time_start,'HH24:MI'))";
    return countConflicts(sql, new Object[]{roomId, day, toHHmm(start), toHHmm(end)});
  }

  public boolean isTeacherAvailable(int teacherId, String day, Time start, Time end) {
    String sql = "SELECT COUNT(*) FROM SCHEDULE " +
                 "WHERE teacher_id=? AND day=? AND status='APPROVED' " +
                 "AND (? < TO_CHAR(time_end,'HH24:MI') AND ? > TO_CHAR(time_start,'HH24:MI'))";
    return !countConflicts(sql, new Object[]{teacherId, day, toHHmm(start), toHHmm(end)});
  }

  private boolean countConflicts(String sql, Object[] params) {
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        ps.setObject(i + 1, params[i]);
      }
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Conflict check failed", e);
    }
    return false;
  }

  public boolean insertApprovedSchedule(int deptId, int batchId, int sectionId, int subjectId,
                                        int teacherId, int roomId, String day, Time start, Time end) {
    String sql = "INSERT INTO SCHEDULE " +
                 "(dept_id, batch_id, section_id, subject_id, teacher_id, room_id, day, time_start, time_end, status) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, TO_DATE(?, 'HH24:MI'), TO_DATE(?, 'HH24:MI'), 'APPROVED')";
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, deptId);
      ps.setInt(2, batchId);
      ps.setInt(3, sectionId);
      ps.setInt(4, subjectId);
      ps.setInt(5, teacherId);
      ps.setInt(6, roomId);
      ps.setString(7, day);
      ps.setString(8, toHHmm(start));
      ps.setString(9, toHHmm(end));
      return ps.executeUpdate() == 1;
    } catch (Exception e) {
      throw new RuntimeException("Insert schedule failed", e);
    }
  }

  private String toHHmm(Time time) {
    if (time == null) return null;
    String val = time.toString(); // HH:MM:SS
    return val.length() >= 5 ? val.substring(0, 5) : val;
  }

  public boolean existsDepartment(int deptId) {
    return existsById("SELECT COUNT(*) FROM DEPARTMENTS WHERE dept_id=?", deptId);
  }

  public boolean existsBatch(int batchId) {
    return existsById("SELECT COUNT(*) FROM BATCHES WHERE batch_id=?", batchId);
  }

  public boolean existsSection(int sectionId) {
    return existsById("SELECT COUNT(*) FROM SECTIONS WHERE section_id=?", sectionId);
  }

  public boolean existsSubject(int subjectId) {
    return existsById("SELECT COUNT(*) FROM SUBJECTS WHERE subject_id=?", subjectId);
  }

  public boolean existsTeacher(int teacherId) {
    return existsById("SELECT COUNT(*) FROM TEACHERS WHERE teacher_id=?", teacherId);
  }

  public boolean existsRoom(int roomId) {
    return existsById("SELECT COUNT(*) FROM CLASSROOMS WHERE room_id=?", roomId);
  }

  private boolean existsById(String sql, int id) {
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (Exception e) {
      throw new RuntimeException("Reference lookup failed", e);
    }
  }

  public List<ScheduleView> getApprovedScheduleForStudent(int deptId, int batchId, int sectionId) {
    String sql = "SELECT s.day, TO_CHAR(s.time_start, 'HH24:MI') AS time_start, " +
                 "TO_CHAR(s.time_end, 'HH24:MI') AS time_end, sub.name AS subject_name, " +
                 "u.name AS teacher_name, c.room_name, s.status " +
                 "FROM SCHEDULE s " +
                 "JOIN SUBJECTS sub ON s.subject_id = sub.subject_id " +
                 "JOIN TEACHERS t ON s.teacher_id = t.teacher_id " +
                 "JOIN USERS u ON t.user_id = u.user_id " +
                 "JOIN CLASSROOMS c ON s.room_id = c.room_id " +
                 "WHERE s.dept_id=? AND s.batch_id=? AND s.section_id=? AND s.status='APPROVED' " +
                 "ORDER BY s.day, s.time_start";
    List<ScheduleView> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, deptId);
      ps.setInt(2, batchId);
      ps.setInt(3, sectionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ScheduleView v = new ScheduleView();
          v.setDay(rs.getString("day"));
          v.setTimeStart(rs.getString("time_start"));
          v.setTimeEnd(rs.getString("time_end"));
          v.setSubjectName(rs.getString("subject_name"));
          v.setTeacherName(rs.getString("teacher_name"));
          v.setRoomName(rs.getString("room_name"));
          v.setStatus(rs.getString("status"));
          list.add(v);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch schedule failed", e);
    }
    return list;
  }

  public List<ScheduleView> getTeacherAssignments(int teacherId) {
    String sql = "SELECT s.day, TO_CHAR(s.time_start, 'HH24:MI') AS time_start, " +
                 "TO_CHAR(s.time_end, 'HH24:MI') AS time_end, sub.name AS subject_name, " +
                 "c.room_name, s.status, d.dept_name " +
                 "FROM SCHEDULE s " +
                 "JOIN SUBJECTS sub ON s.subject_id = sub.subject_id " +
                 "JOIN CLASSROOMS c ON s.room_id = c.room_id " +
                 "JOIN DEPARTMENTS d ON s.dept_id = d.dept_id " +
                 "WHERE s.teacher_id=? AND s.status='APPROVED' " +
                 "ORDER BY s.day, s.time_start";
    List<ScheduleView> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, teacherId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ScheduleView v = new ScheduleView();
          v.setDay(rs.getString("day"));
          v.setTimeStart(rs.getString("time_start"));
          v.setTimeEnd(rs.getString("time_end"));
          v.setSubjectName(rs.getString("subject_name"));
          v.setRoomName(rs.getString("room_name"));
          v.setStatus(rs.getString("status"));
          v.setDeptName(rs.getString("dept_name"));
          list.add(v);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch teacher assignments failed", e);
    }
    return list;
  }
}
