package com.eduflow.model;

import java.sql.Timestamp;

public class Announcement {
  private int announcementId;
  private String message;
  private Timestamp createdAt;
  private String announcerName;
  private String announcerRole;

  public int getAnnouncementId() {
    return announcementId;
  }

  public void setAnnouncementId(int announcementId) {
    this.announcementId = announcementId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }

  public String getAnnouncerName() {
    return announcerName;
  }

  public void setAnnouncerName(String announcerName) {
    this.announcerName = announcerName;
  }

  public String getAnnouncerRole() {
    return announcerRole;
  }

  public void setAnnouncerRole(String announcerRole) {
    this.announcerRole = announcerRole;
  }
}
