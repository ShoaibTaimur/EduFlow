<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="com.eduflow.model.ScheduleView" %>
<%@ page import="com.eduflow.model.ScheduleRequest" %>
<%@ page import="com.eduflow.model.LookupOption" %>
<%
  List<ScheduleView> assignments = (List<ScheduleView>) request.getAttribute("assignments");
  List<ScheduleRequest> requests = (List<ScheduleRequest>) request.getAttribute("requests");
  List<LookupOption> departments = (List<LookupOption>) request.getAttribute("departments");
  List<LookupOption> batches = (List<LookupOption>) request.getAttribute("batches");
  List<LookupOption> sections = (List<LookupOption>) request.getAttribute("sections");
  List<LookupOption> subjects = (List<LookupOption>) request.getAttribute("subjects");
  List<LookupOption> rooms = (List<LookupOption>) request.getAttribute("rooms");
  List<String> days = (List<String>) request.getAttribute("days");
  Integer selectedDeptId = (Integer) request.getAttribute("selectedDeptId");
  Integer selectedBatchId = (Integer) request.getAttribute("selectedBatchId");
  String message = (String) request.getAttribute("message");
  String error = (String) request.getAttribute("error");
  String messageJs = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
  String errorJs = error == null ? "" : error.replace("\\", "\\\\").replace("\"", "\\\"");
  List<String> timeSlots = Arrays.asList("08:00","08:30","09:00","09:30","10:00","10:30","11:00","11:30","12:00","12:30","13:00","13:30","14:00","14:30","15:00","15:30","16:00","16:30","17:00","17:30","18:00","18:30");
%>
<!DOCTYPE html>
<html>
<head>
  <title>Teacher Panel - EduFlow</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen bg-slate-950 text-slate-100">
  <div id="toastContainer" class="fixed top-4 right-4 z-50 space-y-2"></div>
  <div class="max-w-6xl mx-auto p-4">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 mb-6">
      <div>
        <h1 class="text-2xl font-semibold">Teacher Panel</h1>
        <p class="text-slate-400">Submit reschedule requests</p>
      </div>
      <div class="flex items-center gap-2">
        <a href="<%= request.getContextPath() %>/dashboard.jsp" class="text-sm px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700">Dashboard</a>
        <a href="<%= request.getContextPath() %>/logout" class="text-sm px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700">Logout</a>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <div class="lg:col-span-2 rounded-xl border border-slate-800 bg-slate-900/60 p-4">
        <h2 class="font-semibold mb-3">Assigned Classes</h2>
        <div class="overflow-auto">
          <table class="min-w-full text-sm">
            <thead class="text-slate-400">
              <tr>
                <th class="text-left py-2">Class ID</th>
                <th class="text-left py-2">Day</th>
                <th class="text-left py-2">Time</th>
                <th class="text-left py-2">Subject</th>
                <th class="text-left py-2">Room</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800">
              <% if (assignments != null) {
                   for (ScheduleView a : assignments) { %>
                <tr>
                  <td class="py-2"><%= a.getScheduleId() %></td>
                  <td class="py-2"><%= a.getDay() %></td>
                  <td class="py-2"><%= a.getTimeStart() %> - <%= a.getTimeEnd() %></td>
                  <td class="py-2"><%= a.getSubjectName() %></td>
                  <td class="py-2"><%= a.getRoomName() %></td>
                </tr>
              <% } } else { %>
                <tr><td class="py-3 text-slate-400" colspan="5">No assignments found</td></tr>
              <% } %>
            </tbody>
          </table>
        </div>
      </div>

      <div class="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
        <h2 class="font-semibold mb-3">Submit Reschedule</h2>
        <form action="<%= request.getContextPath() %>/teacher/request" method="post" class="space-y-3">
          <select name="scheduleId" class="w-full bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
            <option value="">Create New Class (no existing class selected)</option>
            <% if (assignments != null) { for (ScheduleView a : assignments) { %>
              <option value="<%= a.getScheduleId() %>"
                      data-dept-id="<%= a.getDeptId() %>"
                      data-batch-id="<%= a.getBatchId() %>"
                      data-section-id="<%= a.getSectionId() %>"
                      data-subject-id="<%= a.getSubjectId() %>"
                      data-room-id="<%= a.getRoomId() %>"
                      data-day="<%= a.getDay() %>"
                      data-time-start="<%= a.getTimeStart() %>"
                      data-time-end="<%= a.getTimeEnd() %>">
                Update #<%= a.getScheduleId() %> - <%= a.getDay() %> <%= a.getTimeStart() %>-<%= a.getTimeEnd() %> (<%= a.getSubjectCode() != null ? a.getSubjectCode() : a.getSubjectName() %>)
              </option>
            <% } } %>
          </select>
          <div class="grid grid-cols-2 gap-2">
            <select name="deptId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2" required onchange="location.href='<%= request.getContextPath() %>/teacher/request?deptId=' + this.value;">
              <% if (departments != null) { for (LookupOption o : departments) { %>
                <option value="<%= o.getId() %>" <%= (selectedDeptId != null && selectedDeptId == o.getId()) ? "selected" : "" %>><%= o.getLabel() %></option>
              <% } } %>
            </select>
            <select name="batchId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2" required onchange="location.href='<%= request.getContextPath() %>/teacher/request?deptId=' + document.querySelector('[name=deptId]').value + '&batchId=' + this.value;">
              <% if (batches != null) { for (LookupOption o : batches) { %>
                <option value="<%= o.getId() %>" <%= (selectedBatchId != null && selectedBatchId == o.getId()) ? "selected" : "" %>><%= o.getLabel() %></option>
              <% } } %>
            </select>
            <select name="sectionId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2" required>
              <% if (sections != null) { for (LookupOption o : sections) { %>
                <option value="<%= o.getId() %>"><%= o.getLabel() %></option>
              <% } } %>
            </select>
            <select name="subjectId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2" required>
              <% if (subjects != null) { for (LookupOption o : subjects) { %>
                <option value="<%= o.getId() %>"><%= o.getLabel() %></option>
              <% } } %>
            </select>
            <select name="roomId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2" required>
              <% if (rooms != null) { for (LookupOption o : rooms) { %>
                <option value="<%= o.getId() %>"><%= o.getLabel() %></option>
              <% } } %>
            </select>
            <select name="day" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2" required>
              <% if (days != null) { for (String d : days) { %>
                <option value="<%= d %>"><%= d %></option>
              <% } } %>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-2">
            <select name="timeStart" required class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
              <option value="">Start Time</option>
              <% for (String t : timeSlots) { %><option value="<%= t %>"><%= t %></option><% } %>
            </select>
            <select name="timeEnd" required class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
              <option value="">End Time</option>
              <% for (String t : timeSlots) { %><option value="<%= t %>"><%= t %></option><% } %>
            </select>
          </div>
          <button type="submit" class="w-full rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-900 font-semibold py-2">Submit Request</button>
        </form>
      </div>
    </div>

    <div class="mt-6 rounded-xl border border-slate-800 bg-slate-900/60 p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-semibold">My Requests</h2>
        <form action="<%= request.getContextPath() %>/teacher/request" method="post">
          <input type="hidden" name="action" value="clear" />
          <button type="submit" class="text-xs px-3 py-2 rounded-lg bg-rose-500/20 text-rose-200 hover:bg-rose-500/30">Clear All</button>
        </form>
      </div>
      <div class="overflow-auto">
        <table class="min-w-full text-sm">
          <thead class="text-slate-400">
            <tr>
              <th class="text-left py-2">Request ID</th>
              <th class="text-left py-2">Status</th>
              <th class="text-left py-2">Type</th>
              <th class="text-left py-2">Class ID</th>
              <th class="text-left py-2">Dept</th>
              <th class="text-left py-2">Batch</th>
              <th class="text-left py-2">Section</th>
              <th class="text-left py-2">Subject</th>
              <th class="text-left py-2">Room</th>
              <th class="text-left py-2">Day</th>
              <th class="text-left py-2">Time</th>
              <th class="text-left py-2">Submitted</th>
            </tr>
          </thead>
          <tbody id="requestRows" class="divide-y divide-slate-800">
            <% if (requests != null) {
                 for (ScheduleRequest r : requests) {
                   String proposedEsc = r.getProposedData() == null ? "" : r.getProposedData().replace("\"", "&quot;");
            %>
              <tr data-proposed="<%= proposedEsc %>">
                <td class="py-2"><%= r.getRequestId() %></td>
                <td class="py-2"><%= r.getStatus() %></td>
                <td class="py-2 cell-type"></td>
                <td class="py-2 cell-schedule-id"></td>
                <td class="py-2 cell-dept"></td>
                <td class="py-2 cell-batch"></td>
                <td class="py-2 cell-section"></td>
                <td class="py-2 cell-subject"></td>
                <td class="py-2 cell-room"></td>
                <td class="py-2 cell-day"></td>
                <td class="py-2 cell-time"></td>
                <td class="py-2"><%= r.getSubmittedAt() %></td>
              </tr>
            <% } } else { %>
              <tr><td class="py-3 text-slate-400" colspan="12">No requests found</td></tr>
            <% } %>
          </tbody>
        </table>
      </div>
    </div>

    <div class="mt-6 grid grid-cols-1 lg:grid-cols-3 gap-4">
      <div class="lg:col-span-2 rounded-xl border border-slate-800 bg-slate-900/60 p-4">
        <h2 class="font-semibold mb-3">Announcements</h2>
        <ul id="announcementList" class="space-y-2 text-sm text-slate-300"></ul>
      </div>
      <div class="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
        <h2 class="font-semibold mb-3">Post Announcement</h2>
        <form action="<%= request.getContextPath() %>/announcements" method="post" class="space-y-3">
          <textarea name="message" rows="4" required class="w-full bg-slate-900/60 border border-slate-700 rounded-lg p-3"></textarea>
          <button class="w-full rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-900 font-semibold py-2">Publish</button>
        </form>
      </div>
    </div>
  </div>

<script>
  function showToast(type, text) {
    if (!text) return;
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    const tone = type === 'error'
      ? 'border-rose-500/50 bg-rose-500/15 text-rose-100'
      : 'border-emerald-500/50 bg-emerald-500/15 text-emerald-100';
    toast.className = 'min-w-[260px] max-w-sm rounded-lg border px-4 py-3 text-sm shadow-lg backdrop-blur ' + tone;
    toast.textContent = text;
    container.appendChild(toast);
    setTimeout(() => {
      toast.classList.add('opacity-0', 'transition', 'duration-300');
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  }

  const serverMessage = "<%= messageJs %>";
  const serverError = "<%= errorJs %>";
  if (serverMessage) showToast('success', serverMessage);
  if (serverError) showToast('error', serverError);

  const scheduleSelect = document.querySelector('select[name="scheduleId"]');
  if (scheduleSelect) {
    scheduleSelect.addEventListener('change', () => {
      const opt = scheduleSelect.options[scheduleSelect.selectedIndex];
      if (!opt || !opt.value) return;
      const set = (name, key) => {
        const el = document.querySelector(`[name="${name}"]`);
        if (el && opt.dataset[key]) el.value = opt.dataset[key];
      };
      set('deptId', 'deptId');
      set('batchId', 'batchId');
      set('sectionId', 'sectionId');
      set('subjectId', 'subjectId');
      set('roomId', 'roomId');
      set('day', 'day');
      set('timeStart', 'timeStart');
      set('timeEnd', 'timeEnd');
    });
  }

  document.querySelectorAll('#requestRows tr[data-proposed]').forEach(row => {
    try {
      const data = JSON.parse(row.getAttribute('data-proposed'));
      row.querySelector('.cell-type').textContent = data.requestType || 'CREATE';
      row.querySelector('.cell-schedule-id').textContent = data.scheduleId || '-';
      row.querySelector('.cell-dept').textContent = data.deptId || '';
      row.querySelector('.cell-batch').textContent = data.batchId || '';
      row.querySelector('.cell-section').textContent = data.sectionId || '';
      row.querySelector('.cell-subject').textContent = data.subjectCode || data.subjectId || '';
      row.querySelector('.cell-room').textContent = data.roomId || '';
      row.querySelector('.cell-day').textContent = data.day || '';
      row.querySelector('.cell-time').textContent = (data.timeStart || '') + ' - ' + (data.timeEnd || '');
    } catch (e) {
      // Ignore parse errors
    }
  });

  const ctx = '<%= request.getContextPath() %>';
  async function loadAnnouncements() {
    try {
      const res = await fetch(ctx + '/announcements');
      const data = await res.json();
      const list = document.getElementById('announcementList');
      list.innerHTML = '';
      data.items.forEach(a => {
        const li = document.createElement('li');
        li.className = 'rounded-lg border border-slate-800 bg-slate-950/40 p-2';
        li.textContent = a.message;
        list.appendChild(li);
      });
      if (data.items.length === 0) {
        list.innerHTML = '<li class="text-slate-500">No announcements yet</li>';
      }
    } catch (e) {
      document.getElementById('announcementList').innerHTML = '<li class="text-slate-500">Failed to load announcements</li>';
    }
  }
  loadAnnouncements();
  setInterval(loadAnnouncements, 10000);
</script>
</body>
</html>
