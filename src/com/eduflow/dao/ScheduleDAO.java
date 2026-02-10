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
  public static final class ConflictState {
    private final boolean studentConflict;
    private final boolean roomClash;
    private final boolean teacherAvailable;

    public ConflictState(boolean studentConflict, boolean roomClash, boolean teacherAvailable) {
      this.studentConflict = studentConflict;
      this.roomClash = roomClash;
      this.teacherAvailable = teacherAvailable;
    }

    public boolean hasStudentConflict() {
      return studentConflict;
    }

    public boolean hasRoomClash() {
      return roomClash;
    }

    public boolean isTeacherAvailable() {
      return teacherAvailable;
    }

    public boolean hasAnyConflict() {
      return studentConflict || roomClash || !teacherAvailable;
    }
  }

  public ConflictState evaluateConflicts(int excludeScheduleId, int deptId, int batchId, int sectionId, int teacherId,
      int roomId, String day, Time start, Time end) {
    boolean isUpdate = excludeScheduleId > 0;
    boolean studentConflict = isUpdate
        ? hasStudentConflictExcluding(excludeScheduleId, deptId, batchId, sectionId, day, start, end)
        : hasStudentConflict(deptId, batchId, sectionId, day, start, end);
    boolean roomClash = isUpdate
        ? hasRoomClashExcluding(excludeScheduleId, roomId, day, start, end)
        : hasRoomClash(roomId, day, start, end);
    boolean teacherAvailable = isUpdate
        ? isTeacherAvailableExcluding(excludeScheduleId, teacherId, day, start, end)
        : isTeacherAvailable(teacherId, day, start, end);
    return new ConflictState(studentConflict, roomClash, teacherAvailable);
  }

  public boolean hasStudentConflict(int deptId, int batchId, int sectionId, String day, Time start, Time end) {
    return hasStudentConflictExcluding(0, deptId, batchId, sectionId, day, start, end);
  }

  public boolean hasStudentConflictExcluding(int excludeScheduleId, int deptId, int batchId, int sectionId, String day,
      Time start, Time end) {
    String sql = "SELECT COUNT(*) FROM SCHEDULE " +
        "WHERE dept_id=? AND batch_id=? AND section_id=? AND day=? AND status='APPROVED' " +
        (excludeScheduleId > 0 ? "AND schedule_id<>? " : "") +
        "AND (? < TO_CHAR(time_end,'HH24:MI') AND ? > TO_CHAR(time_start,'HH24:MI'))";
    if (excludeScheduleId > 0) {
      return countConflicts(sql,
          new Object[] { deptId, batchId, sectionId, day, excludeScheduleId, toHHmm(start), toHHmm(end) });
    }
    return countConflicts(sql, new Object[] { deptId, batchId, sectionId, day, toHHmm(start), toHHmm(end) });
  }

  public boolean hasRoomClash(int roomId, String day, Time start, Time end) {
    return hasRoomClashExcluding(0, roomId, day, start, end);
  }

  public boolean hasRoomClashExcluding(int excludeScheduleId, int roomId, String day, Time start, Time end) {
    String sql = "SELECT COUNT(*) FROM SCHEDULE " +
        "WHERE room_id=? AND day=? AND status='APPROVED' " +
        (excludeScheduleId > 0 ? "AND schedule_id<>? " : "") +
        "AND (? < TO_CHAR(time_end,'HH24:MI') AND ? > TO_CHAR(time_start,'HH24:MI'))";
    if (excludeScheduleId > 0) {
      return countConflicts(sql, new Object[] { roomId, day, excludeScheduleId, toHHmm(start), toHHmm(end) });
    }
    return countConflicts(sql, new Object[] { roomId, day, toHHmm(start), toHHmm(end) });
  }

  public boolean isTeacherAvailable(int teacherId, String day, Time start, Time end) {
    return isTeacherAvailableExcluding(0, teacherId, day, start, end);
  }

  public boolean isTeacherAvailableExcluding(int excludeScheduleId, int teacherId, String day, Time start, Time end) {
    String sql = "SELECT COUNT(*) FROM SCHEDULE " +
        "WHERE teacher_id=? AND day=? AND status='APPROVED' " +
        (excludeScheduleId > 0 ? "AND schedule_id<>? " : "") +
        "AND (? < TO_CHAR(time_end,'HH24:MI') AND ? > TO_CHAR(time_start,'HH24:MI'))";
    if (excludeScheduleId > 0) {
      return !countConflicts(sql, new Object[] { teacherId, day, excludeScheduleId, toHHmm(start), toHHmm(end) });
    }
    return !countConflicts(sql, new Object[] { teacherId, day, toHHmm(start), toHHmm(end) });
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

  public boolean updateApprovedSchedule(int scheduleId, int deptId, int batchId, int sectionId, int subjectId,
      int teacherId, int roomId, String day, Time start, Time end) {
    String sql = "UPDATE SCHEDULE SET dept_id=?, batch_id=?, section_id=?, subject_id=?, teacher_id=?, room_id=?, " +
        "day=?, time_start=TO_DATE(?, 'HH24:MI'), time_end=TO_DATE(?, 'HH24:MI'), " +
        "status='APPROVED', last_updated=SYSTIMESTAMP WHERE schedule_id=?";
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
      ps.setInt(10, scheduleId);
      return ps.executeUpdate() == 1;
    } catch (Exception e) {
      throw new RuntimeException("Update schedule failed", e);
    }
  }

  private String toHHmm(Time time) {
    if (time == null)
      return null;
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

  public boolean existsSchedule(int scheduleId) {
    return existsById("SELECT COUNT(*) FROM SCHEDULE WHERE schedule_id=?", scheduleId);
  }

  public int deleteAllSchedules() {
    String sql = "DELETE FROM SCHEDULE";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      return ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("Delete schedules failed", e);
    }
  }

  public boolean isScheduleOwnedByTeacher(int scheduleId, int teacherId) {
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn
            .prepareStatement("SELECT COUNT(*) FROM SCHEDULE WHERE schedule_id=? AND teacher_id=?")) {
      ps.setInt(1, scheduleId);
      ps.setInt(2, teacherId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (Exception e) {
      throw new RuntimeException("Schedule ownership check failed", e);
    }
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
          list.add(mapStudentScheduleView(rs));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch schedule failed", e);
    }
    return list;
  }

  public List<ScheduleView> getTeacherAssignments(int teacherId) {
    String sql = "SELECT s.schedule_id, s.dept_id, s.batch_id, s.section_id, s.subject_id, s.teacher_id, s.room_id, " +
        "s.day, TO_CHAR(s.time_start, 'HH24:MI') AS time_start, " +
        "TO_CHAR(s.time_end, 'HH24:MI') AS time_end, sub.subject_code, sub.name AS subject_name, " +
        "sec.section_name, c.room_name, s.status, d.dept_name " +
        "FROM SCHEDULE s " +
        "JOIN SUBJECTS sub ON s.subject_id = sub.subject_id " +
        "JOIN SECTIONS sec ON s.section_id = sec.section_id " +
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
          list.add(mapTeacherAssignmentView(rs));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch teacher assignments failed", e);
    }
    return list;
  }

  public List<ScheduleView> getApprovedSchedulesForAdmin() {
    String sql = "SELECT s.schedule_id, s.dept_id, s.batch_id, s.section_id, s.subject_id, s.teacher_id, s.room_id, " +
        "s.day, TO_CHAR(s.time_start, 'HH24:MI') AS time_start, TO_CHAR(s.time_end, 'HH24:MI') AS time_end, " +
        "d.dept_name, b.year AS batch_year, sec.section_name, sub.subject_code, sub.name AS subject_name, " +
        "u.name AS teacher_name, c.room_name " +
        "FROM SCHEDULE s " +
        "JOIN DEPARTMENTS d ON s.dept_id = d.dept_id " +
        "JOIN BATCHES b ON s.batch_id = b.batch_id " +
        "JOIN SECTIONS sec ON s.section_id = sec.section_id " +
        "JOIN SUBJECTS sub ON s.subject_id = sub.subject_id " +
        "JOIN TEACHERS t ON s.teacher_id = t.teacher_id " +
        "JOIN USERS u ON t.user_id = u.user_id " +
        "JOIN CLASSROOMS c ON s.room_id = c.room_id " +
        "WHERE s.status='APPROVED' " +
        "ORDER BY s.day, s.time_start, d.dept_name, b.year, sec.section_name";
    List<ScheduleView> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        list.add(mapAdminScheduleView(rs));
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch admin schedules failed", e);
    }
    return list;
  }

  private ScheduleView mapStudentScheduleView(ResultSet rs) throws Exception {
    ScheduleView v = new ScheduleView();
    v.setDay(rs.getString("day"));
    v.setTimeStart(rs.getString("time_start"));
    v.setTimeEnd(rs.getString("time_end"));
    v.setSubjectName(rs.getString("subject_name"));
    v.setTeacherName(rs.getString("teacher_name"));
    v.setRoomName(rs.getString("room_name"));
    v.setStatus(rs.getString("status"));
    return v;
  }

  private ScheduleView mapTeacherAssignmentView(ResultSet rs) throws Exception {
    ScheduleView v = new ScheduleView();
    v.setScheduleId(rs.getInt("schedule_id"));
    v.setDeptId(rs.getInt("dept_id"));
    v.setBatchId(rs.getInt("batch_id"));
    v.setSectionId(rs.getInt("section_id"));
    v.setSubjectId(rs.getInt("subject_id"));
    v.setTeacherId(rs.getInt("teacher_id"));
    v.setRoomId(rs.getInt("room_id"));
    v.setDay(rs.getString("day"));
    v.setTimeStart(rs.getString("time_start"));
    v.setTimeEnd(rs.getString("time_end"));
    v.setSubjectCode(rs.getString("subject_code"));
    v.setSectionName(rs.getString("section_name"));
    v.setSubjectName(rs.getString("subject_name"));
    v.setRoomName(rs.getString("room_name"));
    v.setStatus(rs.getString("status"));
    v.setDeptName(rs.getString("dept_name"));
    return v;
  }

  private ScheduleView mapAdminScheduleView(ResultSet rs) throws Exception {
    ScheduleView v = new ScheduleView();
    v.setScheduleId(rs.getInt("schedule_id"));
    v.setDeptId(rs.getInt("dept_id"));
    v.setBatchId(rs.getInt("batch_id"));
    v.setSectionId(rs.getInt("section_id"));
    v.setSubjectId(rs.getInt("subject_id"));
    v.setTeacherId(rs.getInt("teacher_id"));
    v.setRoomId(rs.getInt("room_id"));
    v.setDay(rs.getString("day"));
    v.setTimeStart(rs.getString("time_start"));
    v.setTimeEnd(rs.getString("time_end"));
    v.setDeptName(rs.getString("dept_name") + " (" + rs.getString("batch_year") + ")");
    v.setSectionName(rs.getString("section_name"));
    v.setSubjectCode(rs.getString("subject_code"));
    v.setSubjectName(rs.getString("subject_name"));
    v.setTeacherName(rs.getString("teacher_name"));
    v.setRoomName(rs.getString("room_name"));
    return v;
  }
}
