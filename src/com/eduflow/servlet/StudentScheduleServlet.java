package com.eduflow.servlet;

import com.eduflow.dao.ScheduleDAO;
import com.eduflow.model.ScheduleView;
import com.eduflow.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class StudentScheduleServlet extends BaseServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!requireRole(req, resp, "student")) {
      return;
    }

    String ajax = req.getParameter("ajax");
    int deptId = parseIntOrDefault(req.getParameter("deptId"), 1);
    int batchId = parseIntOrDefault(req.getParameter("batchId"), 1);
    int sectionId = parseIntOrDefault(req.getParameter("sectionId"), 1);

    ScheduleDAO dao = new ScheduleDAO();
    List<ScheduleView> list = dao.getApprovedScheduleForStudent(deptId, batchId, sectionId);

    if ("1".equals(ajax)) {
      resp.setContentType("application/json");
      resp.setCharacterEncoding("UTF-8");
      StringBuilder sb = new StringBuilder();
      sb.append("{\"items\":[");
      for (int i = 0; i < list.size(); i++) {
        ScheduleView v = list.get(i);
        if (i > 0)
          sb.append(",");
        sb.append("{")
            .append("\"day\":\"").append(JsonUtil.escape(v.getDay())).append("\",")
            .append("\"timeStart\":\"").append(JsonUtil.escape(v.getTimeStart())).append("\",")
            .append("\"timeEnd\":\"").append(JsonUtil.escape(v.getTimeEnd())).append("\",")
            .append("\"subject\":\"").append(JsonUtil.escape(v.getSubjectName())).append("\",")
            .append("\"teacher\":\"").append(JsonUtil.escape(v.getTeacherName())).append("\",")
            .append("\"room\":\"").append(JsonUtil.escape(v.getRoomName())).append("\",")
            .append("\"status\":\"").append(JsonUtil.escape(v.getStatus())).append("\"")
            .append("}");
      }
      sb.append("]}");
      resp.getWriter().write(sb.toString());
      return;
    }

    req.getRequestDispatcher("/student_schedule.jsp").forward(req, resp);
  }

  private int parseIntOrDefault(String value, int defaultVal) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return defaultVal;
    }
  }
}
