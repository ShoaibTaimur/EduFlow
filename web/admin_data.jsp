<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
  Map<String, List<Map<String, String>>> data = (Map<String, List<Map<String, String>>>) request.getAttribute("data");
  Map<String, List<String>> columns = (Map<String, List<String>>) request.getAttribute("columns");
%>
<!DOCTYPE html>
<html>
<head>
  <title>Admin Data Browser - EduFlow</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen bg-slate-950 text-slate-100">
  <div class="max-w-6xl mx-auto p-4">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-semibold">Admin Data Browser</h1>
        <p class="text-slate-400 muted">Read-only view of core tables</p>
      </div>
      <div class="flex items-center gap-2">
        <a href="<%= request.getContextPath() %>/admin/approval" class="text-sm px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 btn">Back</a>
        <a href="<%= request.getContextPath() %>/logout" class="text-sm px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 btn">Logout</a>
      </div>
    </div>

    <% if (data != null) { for (String table : data.keySet()) { %>
      <div class="rounded-xl border border-slate-800 bg-slate-900/60 p-4 mb-6 panel">
        <h2 class="font-semibold mb-3"><%= table %></h2>
        <div class="overflow-auto">
          <table class="min-w-full text-sm">
            <thead class="text-slate-400 table-head">
              <tr>
                <% for (String col : columns.get(table)) { %>
                  <th class="text-left py-2 pr-4"><%= col %></th>
                <% } %>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800 row-border">
              <% List<Map<String, String>> rows = data.get(table);
                 if (rows != null && !rows.isEmpty()) {
                   for (Map<String, String> row : rows) { %>
                    <tr>
                      <% for (String col : columns.get(table)) { %>
                        <td class="py-2 pr-4 text-slate-200"><%= row.get(col) %></td>
                      <% } %>
                    </tr>
              <%   } } else { %>
                <tr><td class="py-3 text-slate-400 muted" colspan="99">No data</td></tr>
              <% } %>
            </tbody>
          </table>
        </div>
      </div>
    <% } } %>
  </div>
</body>
</html>
