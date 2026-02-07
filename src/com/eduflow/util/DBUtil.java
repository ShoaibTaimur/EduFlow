package com.eduflow.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBUtil {
  private static final String CONFIG_FILE = "/config.properties";
  private static String url;
  private static String user;
  private static String password;

  static {
    loadConfig();
  }

  private static void loadConfig() {
    try (InputStream in = DBUtil.class.getResourceAsStream(CONFIG_FILE)) {
      if (in == null) {
        throw new IllegalStateException("Missing config.properties in classpath");
      }
      Properties props = new Properties();
      props.load(in);
      url = props.getProperty("db.url");
      user = props.getProperty("db.user");
      password = props.getProperty("db.password");
      Class.forName("oracle.jdbc.driver.OracleDriver");
    } catch (Exception e) {
      throw new RuntimeException("Failed to load DB configuration", e);
    }
  }

  public static Connection getConnection() throws Exception {
    return DriverManager.getConnection(url, user, password);
  }
}
