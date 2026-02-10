package com.eduflow.servlet;

import com.eduflow.dao.DayPolicyDAO;
import com.eduflow.dao.LookupDAO;
import com.eduflow.dao.ScheduleRequestDAO;
import com.eduflow.dao.ScheduleDAO;
import com.eduflow.dao.AnnouncementDAO;
import com.eduflow.model.LookupOption;
import com.eduflow.model.ScheduleRequest;
import com.eduflow.model.ScheduleView;
import com.eduflow.util.WebDataUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Time;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AdminScheduleApprovalServlet extends BaseServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!requireRole(req, resp, "admin")) {
      return;
    }

    ScheduleRequestDAO requestDAO = new ScheduleRequestDAO();
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

    req.setAttribute("pending", pending);
    req.setAttribute("approvedSchedules", approvedSchedules);
    req.setAttribute("departments", departments);
    req.setAttribute("batches", batches);
    req.setAttribute("sections", sections);
    req.setAttribute("subjects", subjects);
    req.setAttribute("rooms", lookupDAO.getRooms());
    req.setAttribute("teachers", lookupDAO.getTeachers());
    req.setAttribute("days", WEEK_DAYS);

    DayPolicyDAO dayPolicyDAO = new DayPolicyDAO();
    req.setAttribute("dayTypeMap", dayPolicyDAO.getDayTypeMap());
    applyFeedbackAttributes(req);

    req.getRequestDispatcher("/admin_panel.jsp").forward(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!requireRole(req, resp, "admin")) {
      return;
    }

    HttpSession session = req.getSession(false);
    int adminId = (int) session.getAttribute("user_id");
    int requestId = parseIntOrZero(req.getParameter("requestId"));
    String action = req.getParameter("action");

    ScheduleRequestDAO requestDAO = new ScheduleRequestDAO();
    ScheduleDAO scheduleDAO = new ScheduleDAO();
    DayPolicyDAO dayPolicyDAO = new DayPolicyDAO();

    if ("approve".equalsIgnoreCase(action)) {
      handleApprove(req, resp, requestDAO, scheduleDAO, dayPolicyDAO, requestId, adminId);
      return;
    }
    if ("clearDemoData".equalsIgnoreCase(action)) {
      handleClearDemoData(req, resp, scheduleDAO, requestDAO, dayPolicyDAO);
      return;
    }
    if ("updateExisting".equalsIgnoreCase(action)) {
      handleUpdateExisting(req, resp, scheduleDAO, dayPolicyDAO);
      return;
    }
    if ("reject".equalsIgnoreCase(action)) {
      requestDAO.updateStatus(requestId, "REJECTED", adminId);
      req.setAttribute("message", "Request rejected");
    }

    doGet(req, resp);
  }

  private void handleApprove(HttpServletRequest req, HttpServletResponse resp, ScheduleRequestDAO requestDAO,
      ScheduleDAO scheduleDAO, DayPolicyDAO dayPolicyDAO, int requestId, int adminId)
      throws ServletException, IOException {
    ScheduleRequest request = requestDAO.getRequestById(requestId);
    if (request == null) {
      req.setAttribute("error", "Request not found");
      doGet(req, resp);
      return;
    }

    Map<String, String> data = WebDataUtil.parseSimpleJson(request.getProposedData());
    LookupDAO lookupDAO = new LookupDAO();

    int deptId = pickInt(req.getParameter("deptId"), data.get("deptId"));
    int batchId = pickInt(req.getParameter("batchId"), data.get("batchId"));
    int sectionId = pickInt(req.getParameter("sectionId"), data.get("sectionId"));
    int teacherId = pickInt(req.getParameter("teacherId"), data.get("teacherId"));
    int roomId = pickInt(req.getParameter("roomId"), data.get("roomId"));
    int scheduleId = pickInt(req.getParameter("scheduleId"), data.get("scheduleId"));
    String requestType = pickString(req.getParameter("requestType"), data.get("requestType"));
    boolean isUpdateRequest = "UPDATE".equalsIgnoreCase(requestType) && scheduleId > 0;

    Integer subjectId = parseNullableInt(req.getParameter("subjectId"));
    if (subjectId == null) {
      subjectId = parseNullableInt(data.get("subjectId"));
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
    if (dayPolicyDAO.isNonWorkingDay(day)) {
      req.setAttribute("error", day + " is marked as weekend/holiday. Cannot approve this slot.");
      doGet(req, resp);
      return;
    }

    ScheduleDAO.ConflictState conflict = scheduleDAO.evaluateConflicts(isUpdateRequest ? scheduleId : 0,
        deptId, batchId, sectionId, teacherId, roomId, day, start, end);
    if (conflict.hasStudentConflict()) {
      req.setAttribute("error", "Student schedule conflict detected for selected day/time.");
      doGet(req, resp);
      return;
    }
    if (conflict.hasRoomClash()) {
      req.setAttribute("error", "Room clash detected. The selected room already has a class in that slot.");
      doGet(req, resp);
      return;
    }
    if (!conflict.isTeacherAvailable()) {
      req.setAttribute("error", "Teacher is already assigned in that slot.");
      doGet(req, resp);
      return;
    }

    try {
      boolean ok = isUpdateRequest
          ? scheduleDAO.updateApprovedSchedule(scheduleId, deptId, batchId, sectionId, subjectId, teacherId, roomId,
              day, start, end)
          : scheduleDAO.insertApprovedSchedule(deptId, batchId, sectionId, subjectId, teacherId, roomId, day, start,
              end);
      if (ok) {
        requestDAO.updateStatus(requestId, "APPROVED", adminId);
        req.setAttribute("message", isUpdateRequest
            ? "Update request approved and existing schedule changed"
            : "Request approved and schedule created");
      } else {
        req.setAttribute("error", "Failed to update schedule");
      }
    } catch (RuntimeException e) {
      req.setAttribute("error",
          "Approval failed due to database constraints. Verify room/teacher/subject references.");
    }

    doGet(req, resp);
  }

  private void handleUpdateExisting(HttpServletRequest req, HttpServletResponse resp, ScheduleDAO scheduleDAO,
      DayPolicyDAO dayPolicyDAO) throws ServletException, IOException {
    int scheduleId = parseIntOrZero(req.getParameter("scheduleId"));
    int deptId = parseIntOrZero(req.getParameter("deptId"));
    int batchId = parseIntOrZero(req.getParameter("batchId"));
    int sectionId = parseIntOrZero(req.getParameter("sectionId"));
    int subjectId = parseIntOrZero(req.getParameter("subjectId"));
    int teacherId = parseIntOrZero(req.getParameter("teacherId"));
    int roomId = parseIntOrZero(req.getParameter("roomId"));
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
    if (dayPolicyDAO.isNonWorkingDay(day)) {
      req.setAttribute("error", day + " is marked as weekend/holiday. Cannot set class on this day.");
      doGet(req, resp);
      return;
    }

    ScheduleDAO.ConflictState conflict = scheduleDAO.evaluateConflicts(scheduleId, deptId, batchId,
        sectionId, teacherId, roomId, day, start, end);
    if (conflict.hasStudentConflict()) {
      req.setAttribute("error", "Student schedule conflict detected.");
      doGet(req, resp);
      return;
    }
    if (conflict.hasRoomClash()) {
      req.setAttribute("error", "Room clash detected.");
      doGet(req, resp);
      return;
    }
    if (!conflict.isTeacherAvailable()) {
      req.setAttribute("error", "Teacher is already assigned in that slot.");
      doGet(req, resp);
      return;
    }

    scheduleDAO.updateApprovedSchedule(scheduleId, deptId, batchId, sectionId, subjectId, teacherId, roomId, day,
        start, end);
    req.setAttribute("message", "Existing schedule updated by admin.");
    doGet(req, resp);
  }

  private void handleClearDemoData(HttpServletRequest req, HttpServletResponse resp, ScheduleDAO scheduleDAO,
      ScheduleRequestDAO requestDAO, DayPolicyDAO dayPolicyDAO) throws ServletException, IOException {
    AnnouncementDAO announcementDAO = new AnnouncementDAO();
    int deletedRequests = requestDAO.deleteAllRequests();
    int deletedSchedules = scheduleDAO.deleteAllSchedules();
    int deletedAnnouncements = announcementDAO.deleteAllAnnouncements();
    int deletedDayPolicies = dayPolicyDAO.clearAllDayPolicies();

    req.setAttribute("message",
        "Demo data cleared. Requests: " + deletedRequests
            + ", Schedules: " + deletedSchedules
            + ", Announcements: " + deletedAnnouncements
            + ", Day Policies: " + deletedDayPolicies
            + ". Auth data kept unchanged.");
    doGet(req, resp);
  }
}
