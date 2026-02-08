<%@ page contentType="text/html;charset=UTF-8" language="java" %> <% String role
= (String) session.getAttribute("role"); %>
<!DOCTYPE html>
<html>
  <head>
    <title>EduFlow Dashboard</title>
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <script src="https://cdn.tailwindcss.com"></script>
  </head>
  <body class="min-h-screen bg-slate-950 text-slate-100">
    <div class="max-w-5xl mx-auto p-4">
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-semibold">Dashboard</h1>
          <p class="text-slate-400">Role: <%= role %></p>
        </div>
        <a
          href="<%= request.getContextPath() %>/logout"
          class="text-sm px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700"
          >Logout</a
        >
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <a
          class="rounded-xl border border-slate-800 bg-slate-900/60 p-5 hover:border-cyan-500"
          href="<%= request.getContextPath() %>/student/schedule"
        >
          <h2 class="font-semibold mb-1">Student View</h2>
          <p class="text-sm text-slate-400">Live weekly routine</p>
        </a>
        <a
          class="rounded-xl border border-slate-800 bg-slate-900/60 p-5 hover:border-cyan-500"
          href="<%= request.getContextPath() %>/teacher/request"
        >
          <h2 class="font-semibold mb-1">Teacher Panel</h2>
          <p class="text-sm text-slate-400">Submit schedule changes</p>
        </a>
        <a
          class="rounded-xl border border-slate-800 bg-slate-900/60 p-5 hover:border-cyan-500"
          href="<%= request.getContextPath() %>/admin/approval"
        >
          <h2 class="font-semibold mb-1">Admin Panel</h2>
          <p class="text-sm text-slate-400">Approve or reject requests</p>
        </a>
      </div>
    </div>
  </body>
</html>
