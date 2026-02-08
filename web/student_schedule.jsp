<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
  <head>
    <title>Student Schedule - EduFlow</title>
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <script src="https://cdn.tailwindcss.com"></script>
  </head>
  <body
    class="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 text-slate-100"
  >
    <div class="max-w-6xl mx-auto p-4">
      <div
        class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 mb-6"
      >
        <div>
          <h1 class="text-2xl font-semibold">Student Weekly Routine</h1>
          <p class="text-slate-400">Auto-refresh every 10 seconds</p>
        </div>
        <div class="flex items-center gap-2">
          <a
            href="<%= request.getContextPath() %>/dashboard.jsp"
            class="text-sm px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700"
            >Dashboard</a
          >
          <a
            href="<%= request.getContextPath() %>/logout"
            class="text-sm px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700"
            >Logout</a
          >
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div
          class="lg:col-span-1 rounded-xl border border-slate-800 bg-slate-900/60 p-4 h-fit"
        >
          <div class="flex items-center justify-between mb-3">
            <h2 class="font-semibold">Announcements</h2>
            <button
              id="clearAnnouncementsBtn"
              class="text-xs px-2 py-1 rounded bg-slate-800 hover:bg-slate-700"
            >
              Clear
            </button>
          </div>
          <ul
            id="announcementList"
            class="space-y-2 text-sm text-slate-300"
          ></ul>
        </div>

        <div class="lg:col-span-2">
          <div id="status" class="text-sm text-slate-400 mb-3">Loading...</div>
          <div class="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
            <div class="flex items-center justify-between gap-3 mb-4">
              <button
                id="prevDay"
                class="rounded-lg bg-slate-800 hover:bg-slate-700 px-3 py-2 text-sm"
              >
                Previous
              </button>
              <div id="currentDayLabel" class="text-lg font-semibold"></div>
              <div class="flex items-center gap-2">
                <button
                  id="todayBtn"
                  class="rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-900 px-3 py-2 text-sm font-semibold"
                >
                  Today
                </button>
                <button
                  id="nextDay"
                  class="rounded-lg bg-slate-800 hover:bg-slate-700 px-3 py-2 text-sm"
                >
                  Next
                </button>
              </div>
            </div>

            <div
              id="dayStrip"
              class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-2 mb-4"
            ></div>

            <div id="dayNotice" class="mb-3"></div>
            <div id="daySchedule" class="space-y-3"></div>
          </div>
        </div>
      </div>
    </div>

    <script>
      const ctx = "<%= request.getContextPath() %>";
      const statusEl = document.getElementById("status");
      const dayStrip = document.getElementById("dayStrip");
      const daySchedule = document.getElementById("daySchedule");
      const dayNotice = document.getElementById("dayNotice");
      const announcementList = document.getElementById("announcementList");
      const clearAnnouncementsBtn = document.getElementById(
        "clearAnnouncementsBtn",
      );
      const currentDayLabel = document.getElementById("currentDayLabel");
      const prevDayBtn = document.getElementById("prevDay");
      const nextDayBtn = document.getElementById("nextDay");
      const todayBtn = document.getElementById("todayBtn");
      const orderedDays = [
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday",
      ];
      let uiDays = [];
      let selectedIndex = 0;
      let grouped = {};
      let dayTypeMap = {};
      let clearedAnnouncementUntil = Number(
        localStorage.getItem("eduflow_announcements_cleared_until") || "0",
      );

      function getTodayName() {
        const jsDay = new Date().getDay();
        return [
          "Sunday",
          "Monday",
          "Tuesday",
          "Wednesday",
          "Thursday",
          "Friday",
          "Saturday",
        ][jsDay];
      }

      function rotateDaysFromToday() {
        const today = getTodayName();
        const start = orderedDays.indexOf(today);
        if (start === -1) return orderedDays.slice();
        return orderedDays.slice(start).concat(orderedDays.slice(0, start));
      }

      function statusClass(status) {
        if (status === "APPROVED") return "bg-emerald-500/20 text-emerald-200";
        if (status === "PENDING") return "bg-amber-500/20 text-amber-200";
        return "bg-slate-700 text-slate-200";
      }

      function normalizeGrouped(items) {
        const map = {};
        orderedDays.forEach((d) => (map[d] = []));
        items.forEach((item) => {
          if (!map[item.day]) map[item.day] = [];
          map[item.day].push(item);
        });
        return map;
      }

      function renderDayStrip() {
        dayStrip.innerHTML = "";
        const today = getTodayName();
        uiDays.forEach((day, idx) => {
          const btn = document.createElement("button");
          const active = idx === selectedIndex;
          const type = dayTypeMap[day] || "WORKING";
          const isToday = day === today;
          btn.className =
            "rounded-lg px-3 py-2 text-sm border text-left transition " +
            (active
              ? "bg-cyan-500/20 text-cyan-100 border-cyan-400"
              : "bg-slate-900/70 text-slate-200 border-slate-700 hover:bg-slate-800");
          let meta = "";
          if (type === "WEEKEND")
            meta = '<div class="text-xs text-amber-300 mt-0.5">Weekend</div>';
          if (type === "HOLIDAY")
            meta = '<div class="text-xs text-rose-300 mt-0.5">Holiday</div>';
          if (type === "WORKING" && isToday)
            meta = '<div class="text-xs text-cyan-300 mt-0.5">Today</div>';
          btn.innerHTML = '<div class="font-medium">' + day + "</div>" + meta;
          btn.addEventListener("click", () => {
            selectedIndex = idx;
            renderDayStrip();
            renderDaySchedule();
          });
          dayStrip.appendChild(btn);
        });
      }

      function renderDaySchedule() {
        const day = uiDays[selectedIndex];
        currentDayLabel.textContent = day;
        const type = dayTypeMap[day] || "WORKING";
        if (type === "WEEKEND" || type === "HOLIDAY") {
          const cls =
            type === "WEEKEND"
              ? "border-amber-500/40 bg-amber-500/10 text-amber-200"
              : "border-rose-500/40 bg-rose-500/10 text-rose-200";
          dayNotice.innerHTML =
            '<div class=\"rounded-lg border p-3 text-sm ' +
            cls +
            '\">' +
            day +
            " is marked as " +
            type +
            " by admin.</div>";
        } else {
          dayNotice.innerHTML = "";
        }
        const items = grouped[day] || [];
        daySchedule.innerHTML = "";
        if (items.length === 0) {
          daySchedule.innerHTML =
            '<div class="rounded-lg border border-slate-800 bg-slate-950/40 p-4 text-slate-400">No classes scheduled for ' +
            day +
            ".</div>";
          return;
        }
        items.forEach((item) => {
          const card = document.createElement("div");
          card.className =
            "rounded-lg border border-slate-800 bg-slate-950/50 p-4";
          card.innerHTML =
            '<div class="flex flex-col md:flex-row md:items-center md:justify-between gap-2">' +
            "<div>" +
            '<div class="text-base font-semibold">' +
            item.subject +
            "</div>" +
            '<div class="text-sm text-slate-400">Teacher: ' +
            item.teacher +
            "</div>" +
            "</div>" +
            '<span class="px-2 py-1 rounded-full text-xs ' +
            statusClass(item.status) +
            '">' +
            item.status +
            "</span>" +
            "</div>" +
            '<div class="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm">' +
            '<div class="rounded-md bg-slate-900/70 px-3 py-2">Time: ' +
            item.timeStart +
            " - " +
            item.timeEnd +
            "</div>" +
            '<div class="rounded-md bg-slate-900/70 px-3 py-2">Room: ' +
            item.room +
            "</div>" +
            "</div>";
          daySchedule.appendChild(card);
        });
      }

      function slideDay(step) {
        if (uiDays.length === 0) return;
        selectedIndex = (selectedIndex + step + uiDays.length) % uiDays.length;
        renderDayStrip();
        renderDaySchedule();
      }

      function jumpToToday() {
        if (uiDays.length === 0) return;
        const today = getTodayName();
        const idx = uiDays.indexOf(today);
        selectedIndex = idx >= 0 ? idx : 0;
        renderDayStrip();
        renderDaySchedule();
      }

      async function loadSchedule() {
        try {
          statusEl.textContent = "Refreshing...";
          const selectedDayBefore = uiDays[selectedIndex] || getTodayName();
          const res = await fetch(ctx + "/student/schedule?ajax=1");
          const data = await res.json();
          grouped = normalizeGrouped(data.items || []);
          uiDays = rotateDaysFromToday();
          const restored = uiDays.indexOf(selectedDayBefore);
          selectedIndex = restored >= 0 ? restored : 0;
          renderDayStrip();
          renderDaySchedule();
          statusEl.textContent =
            "Last updated: " + new Date().toLocaleTimeString();
        } catch (e) {
          statusEl.textContent = "Failed to load schedule";
        }
      }

      async function loadDaySettings() {
        try {
          const res = await fetch(ctx + "/day-settings");
          const data = await res.json();
          dayTypeMap = {};
          (data.items || []).forEach((item) => {
            dayTypeMap[item.day] = item.type;
          });
          if (uiDays.length > 0) {
            renderDayStrip();
            renderDaySchedule();
          }
        } catch (e) {
          // keep defaults
        }
      }

      async function loadAnnouncements() {
        try {
          const res = await fetch(ctx + "/announcements");
          const data = await res.json();
          announcementList.innerHTML = "";
          let visibleCount = 0;
          data.items.forEach((a) => {
            const idNum = parseInt(a.id || "0", 10);
            if (idNum <= clearedAnnouncementUntil) return;
            const li = document.createElement("li");
            li.className =
              "rounded-lg border border-slate-800 bg-slate-950/40 p-2";
            li.textContent = a.message;
            announcementList.appendChild(li);
            visibleCount++;
          });
          if (visibleCount === 0) {
            announcementList.innerHTML =
              '<li class=\"text-slate-500\">No announcements to show</li>';
          }
        } catch (e) {
          announcementList.innerHTML =
            '<li class=\"text-slate-500\">Failed to load announcements</li>';
        }
      }

      prevDayBtn.addEventListener("click", () => slideDay(-1));
      nextDayBtn.addEventListener("click", () => slideDay(1));
      todayBtn.addEventListener("click", jumpToToday);
      clearAnnouncementsBtn.addEventListener("click", () => {
        // Use latest server ID as a local dismissal watermark.
        fetch(ctx + "/announcements")
          .then((res) => res.json())
          .then((data) => {
            const maxId = Math.max(
              0,
              ...(data.items || []).map((a) => parseInt(a.id || "0", 10)),
            );
            clearedAnnouncementUntil = maxId;
            localStorage.setItem(
              "eduflow_announcements_cleared_until",
              String(clearedAnnouncementUntil),
            );
            announcementList.innerHTML =
              '<li class=\"text-slate-500\">No announcements to show</li>';
          })
          .catch(() => {
            announcementList.innerHTML =
              '<li class=\"text-slate-500\">No announcements to show</li>';
          });
      });

      loadDaySettings();
      loadSchedule();
      loadAnnouncements();
      setInterval(loadDaySettings, 10000);
      setInterval(loadSchedule, 10000);
      setInterval(loadAnnouncements, 10000);
    </script>
  </body>
</html>
