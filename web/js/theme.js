(function () {
  const key = 'eduflow_theme';
  function applyTheme(theme) {
    if (theme === 'light') {
      document.body.classList.add('light');
    } else {
      document.body.classList.remove('light');
    }
  }
  const saved = localStorage.getItem(key) || 'dark';
  applyTheme(saved);

  window.EduFlowTheme = {
    toggle: function () {
      const isLight = document.body.classList.toggle('light');
      localStorage.setItem(key, isLight ? 'light' : 'dark');
      return isLight ? 'light' : 'dark';
    },
    current: function () {
      return document.body.classList.contains('light') ? 'light' : 'dark';
    },
    apply: applyTheme
  };
})();
