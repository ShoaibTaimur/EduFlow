package com.eduflow.model;

public class ScheduleView {
  private String day;
  private String timeStart;
  private String timeEnd;
  private String subjectName;
  private String teacherName;
  private String roomName;
  private String status;
  private String deptName;

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
