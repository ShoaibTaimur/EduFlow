package com.eduflow.servlet;

import com.eduflow.dao.LookupDAO;
import com.eduflow.dao.ScheduleRequestDAO;
import com.eduflow.dao.ScheduleDAO;
import com.eduflow.dao.TeacherDAO;
import com.eduflow.dao.DayPolicyDAO;
import com.eduflow.model.LookupOption;
import com.eduflow.model.ScheduleRequest;
import com.eduflow.model.ScheduleView;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Time;
import java.util.Collections;
import java.util.List;

public class TeacherScheduleRequestServlet extends BaseServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!requireRole(req, resp, "teacher")) {
      return;
    }

    HttpSession session = req.getSession(false);
    int userId = (int) session.getAttribute("user_id");
    TeacherDAO teacherDAO = new TeacherDAO();
    Integer teacherId = teacherDAO.getTeacherIdByUserId(userId);

    ScheduleDAO scheduleDAO = new ScheduleDAO();
    ScheduleRequestDAO requestDAO = new ScheduleRequestDAO();
    LookupDAO lookupDAO = new LookupDAO();
    lookupDAO.ensureDemoAcademicData();

    if (teacherId != null) {
      List<ScheduleView> assignments = scheduleDAO.getTeacherAssignments(teacherId);
      List<ScheduleRequest> requests = requestDAO.getRequestsByTeacher(teacherId);
      req.setAttribute("assignments", assignments);
      req.setAttribute("requests", requests);
    }

    List<LookupOption> departments = lookupDAO.getDepartments();
    int deptId = parseIntOrZero(req.getParameter("deptId"));
    if (deptId <= 0 && !departments.isEmpty())
      deptId = departments.get(0).getId();
    List<LookupOption> batches = deptId > 0 ? lookupDAO.getBatchesByDept(deptId) : Collections.emptyList();
    int batchId = parseIntOrZero(req.getParameter("batchId"));
    if (batchId <= 0 && !batches.isEmpty())
      batchId = batches.get(0).getId();
    List<LookupOption> sections = batchId > 0 ? lookupDAO.getSectionsByBatch(batchId) : Collections.emptyList();
    List<LookupOption> subjects = deptId > 0 ? lookupDAO.getSubjectsByDept(deptId) : Collections.emptyList();
    List<LookupOption> rooms = lookupDAO.getRooms();

    req.setAttribute("departments", departments);
    req.setAttribute("batches", batches);
    req.setAttribute("sections", sections);
    req.setAttribute("subjects", subjects);
    req.setAttribute("rooms", rooms);
    req.setAttribute("selectedDeptId", deptId);
    req.setAttribute("selectedBatchId", batchId);
    req.setAttribute("days", WEEK_DAYS);
    applyFeedbackAttributes(req);

    req.getRequestDispatcher("/teacher_panel.jsp").forward(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!requireRole(req, resp, "teacher")) {
      return;
    }

    HttpSession session = req.getSession(false);
    int userId = (int) session.getAttribute("user_id");
    TeacherDAO teacherDAO = new TeacherDAO();
    Integer teacherId = teacherDAO.getTeacherIdByUserId(userId);
    if (teacherId == null) {
      req.setAttribute("error", "Teacher profile not found");
      doGet(req, resp);
      return;
    }

    String action = req.getParameter("action");
    if ("clear".equalsIgnoreCase(action)) {
      ScheduleRequestDAO requestDAO = new ScheduleRequestDAO();
      int deleted = requestDAO.deleteRequestsByTeacher(teacherId);
      req.setAttribute("message", "Cleared " + deleted + " request(s)");
      doGet(req, resp);
      return;
    }

    int deptId = parseIntOrZero(req.getParameter("deptId"));
    int batchId = parseIntOrZero(req.getParameter("batchId"));
    int sectionId = parseIntOrZero(req.getParameter("sectionId"));
    int subjectId = parseIntOrZero(req.getParameter("subjectId"));
    int roomId = parseIntOrZero(req.getParameter("roomId"));
    int scheduleId = parseIntOrZero(req.getParameter("scheduleId"));
    String day = req.getParameter("day");
    Time start = toTime(req.getParameter("timeStart"));
    Time end = toTime(req.getParameter("timeEnd"));
    if (deptId <= 0 || batchId <= 0 || sectionId <= 0 || subjectId <= 0 || roomId <= 0 || start == null
        || end == null) {
      req.setAttribute("error", "Please select all required values.");
      doGet(req, resp);
      return;
    }

    ScheduleDAO scheduleDAO = new ScheduleDAO();
    DayPolicyDAO dayPolicyDAO = new DayPolicyDAO();
    if (dayPolicyDAO.isNonWorkingDay(day)) {
      req.setAttribute("error", day + " is marked as weekend/holiday. Scheduling is blocked.");
      doGet(req, resp);
      return;
    }
    if (!scheduleDAO.existsRoom(roomId)) {
      req.setAttribute("error", "Selected room does not exist.");
      doGet(req, resp);
      return;
    }
    if (scheduleId > 0 && !scheduleDAO.isScheduleOwnedByTeacher(scheduleId, teacherId)) {
      req.setAttribute("error", "You can only request updates for your own assigned classes.");
      doGet(req, resp);
      return;
    }
    ScheduleDAO.ConflictState conflict = scheduleDAO.evaluateConflicts(scheduleId, deptId, batchId,
        sectionId, teacherId, roomId, day, start, end);
    if (conflict.hasAnyConflict()) {
      req.setAttribute("error", "Schedule conflict detected. Please adjust time/room.");
      doGet(req, resp);
      return;
    }

    LookupDAO lookupDAO = new LookupDAO();
    String subjectCode = lookupDAO.getSubjectCodeById(subjectId);
    String requestType = scheduleId > 0 ? "UPDATE" : "CREATE";
    String proposedData = String.format(
        "{\"requestType\":\"%s\",\"scheduleId\":%d,\"deptId\":%d,\"batchId\":%d,\"sectionId\":%d,\"subjectId\":%d,\"subjectCode\":\"%s\",\"teacherId\":%d,\"roomId\":%d,\"day\":\"%s\",\"timeStart\":\"%s\",\"timeEnd\":\"%s\"}",
        requestType, scheduleId, deptId, batchId, sectionId, subjectId, subjectCode, teacherId, roomId, day,
        req.getParameter("timeStart"), req.getParameter("timeEnd"));

    ScheduleRequestDAO requestDAO = new ScheduleRequestDAO();
    boolean ok = requestDAO.insertRequest(teacherId, proposedData);
    req.setAttribute("message", ok
        ? ("UPDATE".equals(requestType) ? "Update request submitted for admin approval"
            : "Request submitted for admin approval")
        : "Request failed");
    doGet(req, resp);
  }
}
