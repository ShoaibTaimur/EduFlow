package com.eduflow.dao;

import com.eduflow.util.DBUtil;
import com.eduflow.model.LookupOption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class LookupDAO {
  public void ensureDemoAcademicData() {
    try (Connection conn = DBUtil.getConnection()) {
      conn.setAutoCommit(false);

      int deptId = ensureDepartment(conn, "Computer Science and Engineering");
      int batchId = ensureBatch(conn, deptId, 2025);

      ensureSection(conn, batchId, "A");
      ensureSection(conn, batchId, "B");
      ensureSection(conn, batchId, "C");
      ensureSection(conn, batchId, "D");

      ensureSubject(conn, deptId, "CSE-201", "Data Structures");
      ensureSubject(conn, deptId, "CSE-203", "Discrete Mathematics");
      ensureSubject(conn, deptId, "CSE-205", "Object Oriented Programming");
      ensureSubject(conn, deptId, "CSE-207", "Digital Logic Design");
      ensureSubject(conn, deptId, "CSE-209", "Database Systems");
      ensureSubject(conn, deptId, "CSE-211", "Algorithms");

      ensureRoom(conn, "CSE Lab 1", 45);
      ensureRoom(conn, "CSE Lab 2", 45);
      ensureRoom(conn, "CSE Room 201", 60);
      ensureRoom(conn, "CSE Room 202", 60);
      ensureRoom(conn, "CSE Seminar Hall", 100);

      conn.commit();
      conn.setAutoCommit(true);
    } catch (Exception e) {
      throw new RuntimeException("Ensure demo lookup data failed", e);
    }
  }

  public List<LookupOption> getDepartments() {
    return queryOptions("SELECT dept_id, dept_name FROM DEPARTMENTS ORDER BY dept_name");
  }

  public List<LookupOption> getBatchesByDept(int deptId) {
    String sql = "SELECT batch_id, TO_CHAR(year) FROM BATCHES WHERE dept_id=? ORDER BY year DESC";
    List<LookupOption> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, deptId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(new LookupOption(rs.getInt(1), rs.getString(2)));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Lookup batches failed", e);
    }
    return list;
  }

  public List<LookupOption> getSectionsByBatch(int batchId) {
    String sql = "SELECT section_id, section_name FROM SECTIONS " +
        "WHERE batch_id=? AND UPPER(section_name) IN ('A','B','C','D') ORDER BY section_name";
    List<LookupOption> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, batchId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(new LookupOption(rs.getInt(1), rs.getString(2)));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Lookup sections failed", e);
    }
    return list;
  }

  public List<LookupOption> getSubjectsByDept(int deptId) {
    String sql = "SELECT subject_id, subject_code || ' - ' || name FROM SUBJECTS WHERE dept_id=? ORDER BY subject_code";
    List<LookupOption> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, deptId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(new LookupOption(rs.getInt(1), rs.getString(2)));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Lookup subjects failed", e);
    }
    return list;
  }

  public List<LookupOption> getRooms() {
    return queryOptions("SELECT room_id, room_name FROM CLASSROOMS ORDER BY room_id");
  }

  public List<LookupOption> getTeachers() {
    return queryOptions(
        "SELECT t.teacher_id, u.name FROM TEACHERS t JOIN USERS u ON t.user_id=u.user_id ORDER BY u.name");
  }

  public String getSubjectCodeById(int subjectId) {
    String sql = "SELECT subject_code FROM SUBJECTS WHERE subject_id=?";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, subjectId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return rs.getString(1);
      }
    } catch (Exception e) {
      throw new RuntimeException("Lookup subject code failed", e);
    }
    return null;
  }

  public Integer resolveDeptId(String deptValue) {
    Integer num = tryParseInt(deptValue);
    if (num != null)
      return num;
    String sql = "SELECT dept_id FROM DEPARTMENTS WHERE LOWER(dept_name)=LOWER(?)";
    return queryForId(sql, deptValue);
  }

  public Integer resolveBatchId(int deptId, String batchValue) {
    Integer num = tryParseInt(batchValue);
    if (num != null)
      return num;
    String sql = "SELECT batch_id FROM BATCHES WHERE dept_id=? AND year=?";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, deptId);
      ps.setInt(2, Integer.parseInt(batchValue));
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return rs.getInt(1);
      }
    } catch (Exception e) {
      throw new RuntimeException("Resolve batch failed", e);
    }
    return null;
  }

  public Integer resolveSectionId(int batchId, String sectionValue) {
    String normalized = normalizeSection(sectionValue);
    Integer num = tryParseInt(normalized);
    if (num != null)
      return num;
    String sql = "SELECT section_id FROM SECTIONS WHERE batch_id=? AND LOWER(section_name)=LOWER(?)";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, batchId);
      ps.setString(2, normalized);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return rs.getInt(1);
      }
    } catch (Exception e) {
      throw new RuntimeException("Resolve section failed", e);
    }
    String fallbackSql = "SELECT section_id FROM SECTIONS WHERE LOWER(section_name)=LOWER(?) FETCH FIRST 2 ROWS ONLY";
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
      ps.setString(1, normalized);
      try (ResultSet rs = ps.executeQuery()) {
        int count = 0;
        int id = 0;
        while (rs.next()) {
          id = rs.getInt(1);
          count++;
        }
        if (count == 1)
          return id;
      }
    } catch (Exception e) {
      throw new RuntimeException("Resolve section failed", e);
    }
    return null;
  }

  public Integer resolveSubjectIdByCode(String subjectCode) {
    Integer num = tryParseInt(subjectCode);
    if (num != null)
      return num;
    String sql = "SELECT subject_id FROM SUBJECTS WHERE LOWER(subject_code)=LOWER(?)";
    return queryForId(sql, subjectCode);
  }

  private Integer queryForId(String sql, String value) {
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, value);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return rs.getInt(1);
      }
    } catch (Exception e) {
      throw new RuntimeException("Lookup failed", e);
    }
    return null;
  }

  private List<LookupOption> queryOptions(String sql) {
    List<LookupOption> list = new ArrayList<>();
    try (Connection conn = DBUtil.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        list.add(new LookupOption(rs.getInt(1), rs.getString(2)));
      }
    } catch (Exception e) {
      throw new RuntimeException("Lookup list failed", e);
    }
    return list;
  }

  private int ensureDepartment(Connection conn, String deptName) throws Exception {
    String find = "SELECT dept_id FROM DEPARTMENTS WHERE LOWER(dept_name)=LOWER(?)";
    try (PreparedStatement ps = conn.prepareStatement(find)) {
      ps.setString(1, deptName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return rs.getInt(1);
      }
    }
    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO DEPARTMENTS (dept_name) VALUES (?)")) {
      ps.setString(1, deptName);
      ps.executeUpdate();
    }
    try (PreparedStatement ps = conn.prepareStatement(find)) {
      ps.setString(1, deptName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return rs.getInt(1);
      }
    }
    throw new IllegalStateException("Department insert verification failed");
  }

  private int ensureBatch(Connection conn, int deptId, int year) throws Exception {
    String find = "SELECT batch_id FROM BATCHES WHERE dept_id=? AND year=?";
    try (PreparedStatement ps = conn.prepareStatement(find)) {
      ps.setInt(1, deptId);
      ps.setInt(2, year);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return rs.getInt(1);
      }
    }
    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO BATCHES (dept_id, year) VALUES (?, ?)")) {
      ps.setInt(1, deptId);
      ps.setInt(2, year);
      ps.executeUpdate();
    }
    try (PreparedStatement ps = conn.prepareStatement(find)) {
      ps.setInt(1, deptId);
      ps.setInt(2, year);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return rs.getInt(1);
      }
    }
    throw new IllegalStateException("Batch insert verification failed");
  }

  private void ensureSection(Connection conn, int batchId, String section) throws Exception {
    String find = "SELECT 1 FROM SECTIONS WHERE batch_id=? AND UPPER(section_name)=UPPER(?)";
    try (PreparedStatement ps = conn.prepareStatement(find)) {
      ps.setInt(1, batchId);
      ps.setString(2, section);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return;
      }
    }
    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO SECTIONS (batch_id, section_name) VALUES (?, ?)")) {
      ps.setInt(1, batchId);
      ps.setString(2, section);
      ps.executeUpdate();
    }
  }

  private void ensureSubject(Connection conn, int deptId, String code, String name) throws Exception {
    String find = "SELECT 1 FROM SUBJECTS WHERE LOWER(subject_code)=LOWER(?)";
    try (PreparedStatement ps = conn.prepareStatement(find)) {
      ps.setString(1, code);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return;
      }
    }
    try (PreparedStatement ps = conn
        .prepareStatement("INSERT INTO SUBJECTS (subject_code, name, dept_id) VALUES (?, ?, ?)")) {
      ps.setString(1, code);
      ps.setString(2, name);
      ps.setInt(3, deptId);
      ps.executeUpdate();
    }
  }

  private void ensureRoom(Connection conn, String roomName, int capacity) throws Exception {
    String find = "SELECT 1 FROM CLASSROOMS WHERE LOWER(room_name)=LOWER(?)";
    try (PreparedStatement ps = conn.prepareStatement(find)) {
      ps.setString(1, roomName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return;
      }
    }
    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO CLASSROOMS (room_name, capacity) VALUES (?, ?)")) {
      ps.setString(1, roomName);
      ps.setInt(2, capacity);
      ps.executeUpdate();
    }
  }

  private Integer tryParseInt(String value) {
    if (value == null)
      return null;
    try {
      return Integer.parseInt(value.trim());
    } catch (Exception e) {
      return null;
    }
  }

  private String normalizeSection(String value) {
    if (value == null)
      return null;
    String v = value.trim();
    if (v.isEmpty())
      return v;
    String lower = v.toLowerCase();
    if (lower.startsWith("section")) {
      v = v.substring(7).trim();
    } else if (lower.startsWith("sec")) {
      v = v.substring(3).trim();
    }
    return v;
  }
}
