// Nawala Platform - UI Scripts

// Toggle sidebar on mobile
function toggleSidebar() {
    var sidebar = document.getElementById("sidebar");
    if (sidebar) sidebar.classList.toggle("open");
}

// Toggle password visibility
function togglePassword(btn) {
    var input = btn.parentElement.querySelector("input");
    if (input.type === "password") {
        input.type = "text";
        btn.textContent = "\u{1F576}";
    } else {
        input.type = "password";
        btn.textContent = "\u{1F441}";
    }
}

// Auto-dismiss alerts
document.addEventListener("DOMContentLoaded", function() {
    var alerts = document.querySelectorAll(".alert-success, .alert-warning");
    alerts.forEach(function(alert) {
        setTimeout(function() {
            alert.style.opacity = "0";
            alert.style.transform = "translateY(-8px)";
            alert.style.transition = "all 0.3s ease";
            setTimeout(function() { alert.remove(); }, 300);
        }, 5000);
    });

    // Update current time in header
    var timeEl = document.getElementById("currentTime");
    if (timeEl) {
        function updateTime() {
            var now = new Date();
            timeEl.textContent = now.toLocaleDateString("en-US", {
                weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit"
            });
        }
        updateTime();
        setInterval(updateTime, 30000);
    }

    // Close sidebar when clicking outside on mobile
    document.addEventListener("click", function(e) {
        var sidebar = document.getElementById("sidebar");
        var menuBtn = document.querySelector(".mobile-menu-btn");
        if (sidebar && sidebar.classList.contains("open") && !sidebar.contains(e.target) && menuBtn && !menuBtn.contains(e.target)) {
            sidebar.classList.remove("open");
        }
    });

    // Mark active nav item based on current path
    var currentPath = window.location.pathname;
    var navItems = document.querySelectorAll(".nav-item");
    navItems.forEach(function(item) {
        var href = item.getAttribute("href");
        if (href && currentPath.startsWith(href) && href !== "/") {
            // Thymeleaf handles active class via fragment param, but this is a fallback
        }
    });
});
