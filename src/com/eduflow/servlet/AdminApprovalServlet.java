package com.eduflow.servlet;

import com.eduflow.dao.LookupDAO;
import com.eduflow.dao.RequestDAO;
import com.eduflow.dao.ScheduleDAO;
import com.eduflow.dao.DaySettingDAO;
import com.eduflow.model.LookupOption;
import com.eduflow.model.ScheduleRequest;
import com.eduflow.model.ScheduleView;
import com.eduflow.util.RequestParser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Time;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.List;

public class AdminApprovalServlet extends BaseServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!requireRole(req, resp, "admin")) {
      return;
    }
    RequestDAO requestDAO = new RequestDAO();
    ScheduleDAO scheduleDAO = new ScheduleDAO();
    List<ScheduleRequest> pending = requestDAO.getPendingRequests();
    List<ScheduleView> approvedSchedules = scheduleDAO.getApprovedSchedulesForAdmin();
    LookupDAO lookupDAO = new LookupDAO();
    lookupDAO.ensureDemoAcademicData();
    List<LookupOption> departments = lookupDAO.getDepartments();
    int deptId = !departments.isEmpty() ? departments.get(0).getId() : 0;
    List<LookupOption> batches = deptId > 0 ? lookupDAO.getBatchesByDept(deptId) : Collections.emptyList();
    int batchId = !batches.isEmpty() ? batches.get(0).getId() : 0;
    List<LookupOption> sections = batchId > 0 ? lookupDAO.getSectionsByBatch(batchId) : Collections.emptyList();
    List<LookupOption> subjects = deptId > 0 ? lookupDAO.getSubjectsByDept(deptId) : Collections.emptyList();
    List<LookupOption> rooms = lookupDAO.getRooms();
    List<LookupOption> teachers = lookupDAO.getTeachers();
    req.setAttribute("pending", pending);
    req.setAttribute("approvedSchedules", approvedSchedules);
    req.setAttribute("departments", departments);
    req.setAttribute("batches", batches);
    req.setAttribute("sections", sections);
    req.setAttribute("subjects", subjects);
    req.setAttribute("rooms", rooms);
    req.setAttribute("teachers", teachers);
    req.setAttribute("days", Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
    DaySettingDAO daySettingDAO = new DaySettingDAO();
    req.setAttribute("dayTypeMap", daySettingDAO.getDayTypeMap());
    if (req.getAttribute("message") == null && req.getParameter("msg") != null) {
      req.setAttribute("message", req.getParameter("msg"));
    }
    if (req.getAttribute("error") == null && req.getParameter("err") != null) {
      req.setAttribute("error", req.getParameter("err"));
    }
    req.getRequestDispatcher("/admin_panel.jsp").forward(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!requireRole(req, resp, "admin")) {
      return;
    }

    HttpSession session = req.getSession(false);
    int adminId = (int) session.getAttribute("user_id");

    int requestId = parseInt(req.getParameter("requestId"));
    String action = req.getParameter("action");

    RequestDAO requestDAO = new RequestDAO();
    ScheduleDAO scheduleDAO = new ScheduleDAO();
    DaySettingDAO daySettingDAO = new DaySettingDAO();

    if ("approve".equalsIgnoreCase(action)) {
      ScheduleRequest request = requestDAO.getRequestById(requestId);
      if (request == null) {
        req.setAttribute("error", "Request not found");
        doGet(req, resp);
        return;
      }

      Map<String, String> data = RequestParser.parseSimpleJson(request.getProposedData());
      LookupDAO lookupDAO = new LookupDAO();

      int deptId = pickInt(req.getParameter("deptId"), data.get("deptId"));
      int batchId = pickInt(req.getParameter("batchId"), data.get("batchId"));
      int sectionId = pickInt(req.getParameter("sectionId"), data.get("sectionId"));
      int teacherId = pickInt(req.getParameter("teacherId"), data.get("teacherId"));
      int roomId = pickInt(req.getParameter("roomId"), data.get("roomId"));
      int scheduleId = pickInt(req.getParameter("scheduleId"), data.get("scheduleId"));
      String requestType = pickString(req.getParameter("requestType"), data.get("requestType"));
      boolean isUpdateRequest = "UPDATE".equalsIgnoreCase(requestType) && scheduleId > 0;

      Integer subjectId = tryParseInt(req.getParameter("subjectId"));
      if (subjectId == null) {
        subjectId = tryParseInt(data.get("subjectId"));
      }
      if (subjectId == null) {
        subjectId = lookupDAO.resolveSubjectIdByCode(data.get("subjectCode"));
      }

      String day = pickString(req.getParameter("day"), data.get("day"));
      Time start = toTime(pickString(req.getParameter("timeStart"), data.get("timeStart")));
      Time end = toTime(pickString(req.getParameter("timeEnd"), data.get("timeEnd")));
      if (subjectId == null || deptId <= 0 || batchId <= 0 || sectionId <= 0 || teacherId <= 0 || roomId <= 0
          || day == null || day.trim().isEmpty() || start == null || end == null) {
        req.setAttribute("error", "Request data is incomplete. Use Edit Details to correct it.");
        doGet(req, resp);
        return;
      }
      if (!scheduleDAO.existsDepartment(deptId)) {
        req.setAttribute("error", "Invalid department ID: " + deptId);
        doGet(req, resp);
        return;
      }
      if (!scheduleDAO.existsBatch(batchId)) {
        req.setAttribute("error", "Invalid batch ID: " + batchId);
        doGet(req, resp);
        return;
      }
      if (!scheduleDAO.existsSection(sectionId)) {
        req.setAttribute("error", "Invalid section ID: " + sectionId);
        doGet(req, resp);
        return;
      }
      if (!scheduleDAO.existsSubject(subjectId)) {
        req.setAttribute("error", "Invalid subject ID/code in request.");
        doGet(req, resp);
        return;
      }
      if (!scheduleDAO.existsTeacher(teacherId)) {
        req.setAttribute("error", "Invalid teacher ID: " + teacherId);
        doGet(req, resp);
        return;
      }
      if (!scheduleDAO.existsRoom(roomId)) {
        req.setAttribute("error", "Invalid room ID: " + roomId + ". Create the room first or edit the request.");
        doGet(req, resp);
        return;
      }
      if (isUpdateRequest && !scheduleDAO.existsSchedule(scheduleId)) {
        req.setAttribute("error", "Schedule to update was not found. It may have been deleted.");
        doGet(req, resp);
        return;
      }
      if (daySettingDAO.isNonWorkingDay(day)) {
        req.setAttribute("error", day + " is marked as weekend/holiday. Cannot approve this slot.");
        doGet(req, resp);
        return;
      }
      boolean studentConflict = isUpdateRequest
        ? scheduleDAO.hasStudentConflictExcluding(scheduleId, deptId, batchId, sectionId, day, start, end)
        : scheduleDAO.hasStudentConflict(deptId, batchId, sectionId, day, start, end);
      if (studentConflict) {
        req.setAttribute("error", "Student schedule conflict detected for selected day/time.");
        doGet(req, resp);
        return;
      }
      boolean roomClash = isUpdateRequest
        ? scheduleDAO.hasRoomClashExcluding(scheduleId, roomId, day, start, end)
        : scheduleDAO.hasRoomClash(roomId, day, start, end);
      if (roomClash) {
        req.setAttribute("error", "Room clash detected. The selected room already has a class in that slot.");
        doGet(req, resp);
        return;
      }
      boolean teacherAvailable = isUpdateRequest
        ? scheduleDAO.isTeacherAvailableExcluding(scheduleId, teacherId, day, start, end)
        : scheduleDAO.isTeacherAvailable(teacherId, day, start, end);
      if (!teacherAvailable) {
        req.setAttribute("error", "Teacher is already assigned in that slot.");
        doGet(req, resp);
        return;
      }

      try {
        boolean ok = isUpdateRequest
          ? scheduleDAO.updateApprovedSchedule(scheduleId, deptId, batchId, sectionId, subjectId, teacherId, roomId, day, start, end)
          : scheduleDAO.insertApprovedSchedule(deptId, batchId, sectionId, subjectId, teacherId, roomId, day, start, end);
        if (ok) {
          requestDAO.updateStatus(requestId, "APPROVED", adminId);
          req.setAttribute("message", isUpdateRequest
            ? "Update request approved and existing schedule changed"
            : "Request approved and schedule created");
        } else {
          req.setAttribute("error", "Failed to update schedule");
        }
      } catch (RuntimeException e) {
        req.setAttribute("error", "Approval failed due to database constraints. Verify room/teacher/subject references.");
      }
    } else if ("updateExisting".equalsIgnoreCase(action)) {
      int scheduleId = parseInt(req.getParameter("scheduleId"));
      int deptId = parseInt(req.getParameter("deptId"));
      int batchId = parseInt(req.getParameter("batchId"));
      int sectionId = parseInt(req.getParameter("sectionId"));
      int subjectId = parseInt(req.getParameter("subjectId"));
      int teacherId = parseInt(req.getParameter("teacherId"));
      int roomId = parseInt(req.getParameter("roomId"));
      String day = req.getParameter("day");
      Time start = toTime(req.getParameter("timeStart"));
      Time end = toTime(req.getParameter("timeEnd"));

      if (scheduleId <= 0 || deptId <= 0 || batchId <= 0 || sectionId <= 0 || subjectId <= 0
          || teacherId <= 0 || roomId <= 0 || day == null || day.trim().isEmpty() || start == null || end == null) {
        req.setAttribute("error", "All schedule fields are required for admin update.");
        doGet(req, resp);
        return;
      }
      if (!scheduleDAO.existsSchedule(scheduleId)) {
        req.setAttribute("error", "Schedule not found.");
        doGet(req, resp);
        return;
      }
      if (daySettingDAO.isNonWorkingDay(day)) {
        req.setAttribute("error", day + " is marked as weekend/holiday. Cannot set class on this day.");
        doGet(req, resp);
        return;
      }
      if (scheduleDAO.hasStudentConflictExcluding(scheduleId, deptId, batchId, sectionId, day, start, end)) {
        req.setAttribute("error", "Student schedule conflict detected.");
        doGet(req, resp);
        return;
      }
      if (scheduleDAO.hasRoomClashExcluding(scheduleId, roomId, day, start, end)) {
        req.setAttribute("error", "Room clash detected.");
        doGet(req, resp);
        return;
      }
      if (!scheduleDAO.isTeacherAvailableExcluding(scheduleId, teacherId, day, start, end)) {
        req.setAttribute("error", "Teacher is already assigned in that slot.");
        doGet(req, resp);
        return;
      }
      scheduleDAO.updateApprovedSchedule(scheduleId, deptId, batchId, sectionId, subjectId, teacherId, roomId, day, start, end);
      req.setAttribute("message", "Existing schedule updated by admin.");
    } else if ("reject".equalsIgnoreCase(action)) {
      requestDAO.updateStatus(requestId, "REJECTED", adminId);
      req.setAttribute("message", "Request rejected");
    }

    doGet(req, resp);
  }

  private int parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return 0;
    }
  }

  private Integer tryParseInt(String value) {
    try {
      if (value == null || value.trim().isEmpty()) return null;
      return Integer.parseInt(value.trim());
    } catch (Exception e) {
      return null;
    }
  }

  private int pickInt(String preferred, String fallback) {
    Integer val = tryParseInt(preferred);
    if (val != null) return val;
    val = tryParseInt(fallback);
    return val == null ? 0 : val;
  }

  private String pickString(String preferred, String fallback) {
    if (preferred != null && !preferred.trim().isEmpty()) return preferred;
    return fallback;
  }

  private Time toTime(String hhmm) {
    if (hhmm == null || hhmm.length() == 0) return null;
    return Time.valueOf(hhmm + ":00");
  }
}
