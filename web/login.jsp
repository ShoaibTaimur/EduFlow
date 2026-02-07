<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
  <title>EduFlow Login</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white">
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="w-full max-w-md bg-slate-800/70 backdrop-blur rounded-2xl shadow-xl p-8">
      <div class="mb-6">
        <h1 class="text-2xl font-semibold">EduFlow</h1>
        <p class="text-slate-300">Dynamic Academic Routine Management</p>
      </div>

      <% String error = (String) request.getAttribute("error"); %>
      <% if (error != null) { %>
        <div class="mb-4 rounded-lg border border-red-400/40 bg-red-500/10 p-3 text-sm text-red-200">
          <%= error %>
        </div>
      <% } %>

      <form action="login" method="post" class="space-y-4">
        <div>
          <label class="block text-sm text-slate-300 mb-1">Email</label>
          <input type="email" name="email" required
                 class="w-full rounded-lg bg-slate-900/60 border border-slate-700 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-cyan-400" />
        </div>
        <div>
          <label class="block text-sm text-slate-300 mb-1">Password</label>
          <input type="password" name="password" required
                 class="w-full rounded-lg bg-slate-900/60 border border-slate-700 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-cyan-400" />
        </div>
        <button type="submit" class="w-full rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-900 font-semibold py-2">
          Sign In
        </button>
      </form>

      <div class="mt-6 text-xs text-slate-400">
        Demo accounts: admin@demo.com / admin123 · teacher@demo.com / teacher123 · student@demo.com / student123
      </div>
    </div>
  </div>
</body>
</html>
