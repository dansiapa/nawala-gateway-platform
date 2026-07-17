// Nawala Platform - UI Scripts

// Toggle sidebar on mobile
function toggleSidebar() {
    const sidebar = document.getElementById(''sidebar'');
    if (sidebar) sidebar.classList.toggle(''open'');
}

// Toggle password visibility
function togglePassword(btn) {
    const input = btn.parentElement.querySelector(''input'');
    if (input.type === ''password'') {
        input.type = ''text'';
        btn.textContent = ''\u{1F576}'';
    } else {
        input.type = ''password'';
        btn.textContent = ''\u{1F441}'';
    }
}

// Auto-dismiss alerts
document.addEventListener(''DOMContentLoaded'', function() {
    const alerts = document.querySelectorAll(''.alert-success, .alert-warning'');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            alert.style.opacity = ''0'';
            alert.style.transform = ''translateY(-8px)'';
            setTimeout(function() { alert.remove(); }, 300);
        }, 5000);
    });

    // Update current time
    const timeEl = document.getElementById(''currentTime'');
    if (timeEl) {
        function updateTime() {
            const now = new Date();
            timeEl.textContent = now.toLocaleDateString(''en-US'', {
                weekday: ''short'', day: ''numeric'', month: ''short'', hour: ''2-digit'', minute: ''2-digit''
            });
        }
        updateTime();
        setInterval(updateTime, 30000);
    }

    // Close sidebar when clicking outside on mobile
    document.addEventListener(''click'', function(e) {
        const sidebar = document.getElementById(''sidebar'');
        const menuBtn = document.querySelector(''.mobile-menu-btn'');
        if (sidebar && sidebar.classList.contains(''open'') && !sidebar.contains(e.target) && !menuBtn.contains(e.target)) {
            sidebar.classList.remove(''open'');
        }
    });
});
