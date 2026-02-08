package com.eduflow.model;

import java.sql.Timestamp;

public class ScheduleRequest {
  private int requestId;
  private int teacherId;
  private String proposedData;
  private String status;
  private Timestamp submittedAt;
  private Timestamp reviewedAt;
  private Integer adminId;

  public int getRequestId() {
    return requestId;
  }

  public void setRequestId(int requestId) {
    this.requestId = requestId;
  }

  public int getTeacherId() {
    return teacherId;
  }

  public void setTeacherId(int teacherId) {
    this.teacherId = teacherId;
  }

  public String getProposedData() {
    return proposedData;
  }

  public void setProposedData(String proposedData) {
    this.proposedData = proposedData;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Timestamp getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(Timestamp submittedAt) {
    this.submittedAt = submittedAt;
  }

  public Timestamp getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Timestamp reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  public Integer getAdminId() {
    return adminId;
  }

  public void setAdminId(Integer adminId) {
    this.adminId = adminId;
  }
}
