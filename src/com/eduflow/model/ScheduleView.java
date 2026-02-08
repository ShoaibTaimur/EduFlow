package com.eduflow.model;

public class ScheduleView {
  private int scheduleId;
  private int deptId;
  private int batchId;
  private int sectionId;
  private int subjectId;
  private int teacherId;
  private int roomId;
  private String sectionName;
  private String subjectCode;
  private String day;
  private String timeStart;
  private String timeEnd;
  private String subjectName;
  private String teacherName;
  private String roomName;
  private String status;
  private String deptName;

  public int getScheduleId() { return scheduleId; }
  public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }

  public int getDeptId() { return deptId; }
  public void setDeptId(int deptId) { this.deptId = deptId; }

  public int getBatchId() { return batchId; }
  public void setBatchId(int batchId) { this.batchId = batchId; }

  public int getSectionId() { return sectionId; }
  public void setSectionId(int sectionId) { this.sectionId = sectionId; }

  public int getSubjectId() { return subjectId; }
  public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

  public int getTeacherId() { return teacherId; }
  public void setTeacherId(int teacherId) { this.teacherId = teacherId; }

  public int getRoomId() { return roomId; }
  public void setRoomId(int roomId) { this.roomId = roomId; }

  public String getSectionName() { return sectionName; }
  public void setSectionName(String sectionName) { this.sectionName = sectionName; }

  public String getSubjectCode() { return subjectCode; }
  public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

  public String getDay() { return day; }
  public void setDay(String day) { this.day = day; }

  public String getTimeStart() { return timeStart; }
  public void setTimeStart(String timeStart) { this.timeStart = timeStart; }

  public String getTimeEnd() { return timeEnd; }
  public void setTimeEnd(String timeEnd) { this.timeEnd = timeEnd; }

  public String getSubjectName() { return subjectName; }
  public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

  public String getTeacherName() { return teacherName; }
  public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

  public String getRoomName() { return roomName; }
  public void setRoomName(String roomName) { this.roomName = roomName; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public String getDeptName() { return deptName; }
  public void setDeptName(String deptName) { this.deptName = deptName; }
}
