<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.eduflow.model.LookupOption" %>
<%@ page import="com.eduflow.model.ScheduleRequest" %>
<%
  List<ScheduleRequest> pending = (List<ScheduleRequest>) request.getAttribute("pending");
  List<LookupOption> departments = (List<LookupOption>) request.getAttribute("departments");
  List<LookupOption> batches = (List<LookupOption>) request.getAttribute("batches");
  List<LookupOption> sections = (List<LookupOption>) request.getAttribute("sections");
  List<LookupOption> subjects = (List<LookupOption>) request.getAttribute("subjects");
  List<LookupOption> rooms = (List<LookupOption>) request.getAttribute("rooms");
  List<LookupOption> teachers = (List<LookupOption>) request.getAttribute("teachers");
  List<String> days = (List<String>) request.getAttribute("days");
  Map<String, String> dayTypeMap = (Map<String, String>) request.getAttribute("dayTypeMap");
  List<String> timeSlots = Arrays.asList("08:00","08:30","09:00","09:30","10:00","10:30","11:00","11:30","12:00","12:30","13:00","13:30","14:00","14:30","15:00","15:30","16:00","16:30","17:00","17:30","18:00","18:30");
  String message = (String) request.getAttribute("message");
  String error = (String) request.getAttribute("error");
  String messageJs = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
  String errorJs = error == null ? "" : error.replace("\\", "\\\\").replace("\"", "\\\"");
%>
<!DOCTYPE html>
<html>
<head>
  <title>Admin Panel - EduFlow</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen bg-slate-950 text-slate-100">
  <div id="toastContainer" class="fixed top-4 right-4 z-50 space-y-2"></div>
  <div class="max-w-6xl mx-auto p-4">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 mb-6">
      <div>
        <h1 class="text-2xl font-semibold">Admin Panel</h1>
        <p class="text-slate-400">Approve or reject schedule changes</p>
      </div>
      <div class="flex items-center gap-2">
        <a href="<%= request.getContextPath() %>/dashboard.jsp" class="text-sm px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700">Dashboard</a>
        <a href="<%= request.getContextPath() %>/logout" class="text-sm px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700">Logout</a>
      </div>
    </div>

    <div class="mb-6 rounded-xl border border-slate-800 bg-slate-900/60 p-4">
      <h2 class="font-semibold mb-3">Weekday Policy</h2>
      <form action="<%= request.getContextPath() %>/day-settings" method="post" class="grid grid-cols-1 md:grid-cols-4 gap-2 mb-3">
        <select name="day" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2" required>
          <% if (days != null) { for (String d : days) { %><option value="<%= d %>"><%= d %></option><% } } %>
        </select>
        <select name="type" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2" required>
          <option value="WORKING">Working Day</option>
          <option value="WEEKEND">Weekend</option>
          <option value="HOLIDAY">Holiday</option>
        </select>
        <button class="rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-900 font-semibold py-2">Save Day Policy</button>
      </form>
      <div class="flex flex-wrap gap-2 text-xs">
        <% if (days != null) { for (String d : days) {
             String t = dayTypeMap != null && dayTypeMap.get(d) != null ? dayTypeMap.get(d) : "WORKING";
             String cls = "WORKING".equals(t) ? "bg-emerald-500/20 text-emerald-200" : ("WEEKEND".equals(t) ? "bg-amber-500/20 text-amber-200" : "bg-rose-500/20 text-rose-200");
        %>
          <span class="px-2 py-1 rounded-full <%= cls %>"><%= d %>: <%= t %></span>
        <% } } %>
      </div>
    </div>

    <div class="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
      <h2 class="font-semibold mb-3">Pending Requests</h2>
      <div class="space-y-4">
        <% if (pending != null && !pending.isEmpty()) {
             for (ScheduleRequest r : pending) {
               String proposedEsc = r.getProposedData() == null ? "" : r.getProposedData().replace("\"", "&quot;");
        %>
          <div class="rounded-lg border border-slate-800 p-3">
            <div class="text-sm text-slate-400 mb-2">Request #<%= r.getRequestId() %> · Teacher ID <%= r.getTeacherId() %></div>
            <div class="text-xs text-slate-500 mb-3">Proposed: <span class="proposal-summary"></span></div>
            <form action="<%= request.getContextPath() %>/admin/approval" method="post" class="space-y-2 admin-form" data-proposed="<%= proposedEsc %>">
              <input type="hidden" name="requestId" value="<%= r.getRequestId() %>" />
              <div class="flex flex-wrap gap-2">
                <button name="action" value="approve" class="rounded-lg bg-emerald-500 text-slate-900 font-semibold py-2 px-4">Approve</button>
                <button name="action" value="reject" class="rounded-lg bg-rose-500 text-slate-900 font-semibold py-2 px-4">Reject</button>
                <button type="button" class="rounded-lg bg-slate-800 text-slate-200 py-2 px-4 toggle-edit">Edit Details</button>
              </div>
              <div class="hidden edit-fields grid grid-cols-2 md:grid-cols-4 gap-2">
                <select name="deptId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
                  <option value="">Dept</option>
                  <% if (departments != null) { for (LookupOption o : departments) { %><option value="<%= o.getId() %>"><%= o.getLabel() %></option><% } } %>
                </select>
                <select name="batchId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
                  <option value="">Batch</option>
                  <% if (batches != null) { for (LookupOption o : batches) { %><option value="<%= o.getId() %>"><%= o.getLabel() %></option><% } } %>
                </select>
                <select name="sectionId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
                  <option value="">Section</option>
                  <% if (sections != null) { for (LookupOption o : sections) { %><option value="<%= o.getId() %>"><%= o.getLabel() %></option><% } } %>
                </select>
                <select name="subjectId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
                  <option value="">Subject</option>
                  <% if (subjects != null) { for (LookupOption o : subjects) { %><option value="<%= o.getId() %>"><%= o.getLabel() %></option><% } } %>
                </select>
                <select name="teacherId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
                  <option value="">Teacher</option>
                  <% if (teachers != null) { for (LookupOption o : teachers) { %><option value="<%= o.getId() %>"><%= o.getLabel() %></option><% } } %>
                </select>
                <select name="roomId" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
                  <option value="">Room</option>
                  <% if (rooms != null) { for (LookupOption o : rooms) { %><option value="<%= o.getId() %>"><%= o.getLabel() %></option><% } } %>
                </select>
                <select name="day" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
                  <option value="">Day</option>
                  <% if (days != null) { for (String d : days) { %><option value="<%= d %>"><%= d %></option><% } } %>
                </select>
                <select name="timeStart" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
                  <option value="">Start</option>
                  <% for (String t : timeSlots) { %><option value="<%= t %>"><%= t %></option><% } %>
                </select>
                <select name="timeEnd" class="bg-slate-900/60 border border-slate-700 rounded-lg px-3 py-2">
                  <option value="">End</option>
                  <% for (String t : timeSlots) { %><option value="<%= t %>"><%= t %></option><% } %>
                </select>
              </div>
            </form>
          </div>
        <% } } else { %>
          <div class="text-sm text-slate-400">No pending requests</div>
        <% } %>
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

  document.querySelectorAll('.admin-form').forEach(form => {
    const dataAttr = form.getAttribute('data-proposed');
    let data = {};
    try { data = JSON.parse(dataAttr); } catch (e) {}
    const summary = form.parentElement.querySelector('.proposal-summary');
    if (summary) {
      summary.textContent = `${data.day || ''} ${data.timeStart || ''}-${data.timeEnd || ''} · Dept ${data.deptId || ''} · Batch ${data.batchId || ''} · Sec ${data.sectionId || ''} · Sub ${data.subjectCode || data.subjectId || ''} · Room ${data.roomId || ''}`;
    }
    const edit = form.querySelector('.edit-fields');
    form.querySelectorAll('select[name=deptId]').forEach(i => i.value = data.deptId || '');
    form.querySelectorAll('select[name=batchId]').forEach(i => i.value = data.batchId || '');
    form.querySelectorAll('select[name=sectionId]').forEach(i => i.value = data.sectionId || '');
    form.querySelectorAll('select[name=subjectId]').forEach(i => i.value = data.subjectId || '');
    form.querySelectorAll('select[name=teacherId]').forEach(i => i.value = data.teacherId || '');
    form.querySelectorAll('select[name=roomId]').forEach(i => i.value = data.roomId || '');
    form.querySelectorAll('select[name=day]').forEach(i => i.value = data.day || '');
    form.querySelectorAll('select[name=timeStart]').forEach(i => i.value = data.timeStart || '');
    form.querySelectorAll('select[name=timeEnd]').forEach(i => i.value = data.timeEnd || '');

    form.querySelector('.toggle-edit').addEventListener('click', () => {
      edit.classList.toggle('hidden');
    });
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
</script>
</body>
</html>
