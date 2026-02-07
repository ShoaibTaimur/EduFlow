package com.eduflow.dao;

import com.eduflow.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DaySettingDAO {
  public DaySettingDAO() {
    ensureTable();
  }

  public void upsertDaySetting(String dayName, String dayType, int adminId) {
    String mergeSql = "MERGE INTO DAY_SETTINGS d " +
                      "USING (SELECT ? AS day_name, ? AS day_type, ? AS updated_by FROM dual) src " +
                      "ON (d.day_name = src.day_name) " +
                      "WHEN MATCHED THEN UPDATE SET d.day_type = src.day_type, d.updated_by = src.updated_by, d.updated_at = SYSTIMESTAMP " +
                      "WHEN NOT MATCHED THEN INSERT (day_name, day_type, updated_by, updated_at) VALUES (src.day_name, src.day_type, src.updated_by, SYSTIMESTAMP)";
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(mergeSql)) {
      ps.setString(1, dayName);
      ps.setString(2, dayType);
      ps.setInt(3, adminId);
      ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("Upsert day setting failed", e);
    }
  }

  public Map<String, String> getDayTypeMap() {
    Map<String, String> map = new HashMap<>();
    String sql = "SELECT day_name, day_type FROM DAY_SETTINGS";
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        map.put(rs.getString("day_name"), rs.getString("day_type"));
      }
    } catch (Exception e) {
      throw new RuntimeException("Fetch day settings failed", e);
    }
    return map;
  }

  public boolean isNonWorkingDay(String dayName) {
    String sql = "SELECT day_type FROM DAY_SETTINGS WHERE day_name=?";
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, dayName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String type = rs.getString(1);
          return "WEEKEND".equalsIgnoreCase(type) || "HOLIDAY".equalsIgnoreCase(type);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Day setting lookup failed", e);
    }
    return false;
  }

  private void ensureTable() {
    String sql = "CREATE TABLE DAY_SETTINGS (" +
                 "day_name VARCHAR2(10) PRIMARY KEY, " +
                 "day_type VARCHAR2(20) NOT NULL, " +
                 "updated_by NUMBER, " +
                 "updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL, " +
                 "CONSTRAINT ck_day_type CHECK (day_type IN ('WORKING','WEEKEND','HOLIDAY'))" +
                 ")";
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.executeUpdate();
    } catch (SQLException e) {
      // ORA-00955: name is already used by an existing object
      if (e.getErrorCode() != 955) {
        throw new RuntimeException("Failed creating DAY_SETTINGS table", e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed initializing day settings", e);
    }
  }
}
