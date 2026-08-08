// Nawala Platform - Modern UI Scripts
(function() {
    'use strict';

    // Theme Management
    const ThemeManager = {
        STORAGE_KEY: 'nawala-theme',
        init() {
            this.loadTheme();
            this.bindEvents();
        },
        loadTheme() {
            const saved = localStorage.getItem(this.STORAGE_KEY) || 'system';
            this.setTheme(saved);
        },
        setTheme(theme) {
            const root = document.documentElement;
            if (theme === 'system') {
                root.removeAttribute('data-theme');
            } else {
                root.setAttribute('data-theme', theme);
            }
            localStorage.setItem(this.STORAGE_KEY, theme);
            this.updateButtons();
        },
        updateButtons() {
            const current = localStorage.getItem(this.STORAGE_KEY) || 'system';
            document.querySelectorAll('.theme-btn').forEach(btn => {
                btn.classList.toggle('active', btn.dataset.theme === current);
            });
        },
        bindEvents() {
            document.querySelectorAll('.theme-btn').forEach(btn => {
                btn.addEventListener('click', () => this.setTheme(btn.dataset.theme));
            });
        }
    };

    // Sidebar
    const Sidebar = {
        init() {
            this.el = document.getElementById('sidebar');
            const menuBtn = document.querySelector('.mobile-menu-btn');
            const overlay = document.querySelector('.sidebar-overlay');
            if (menuBtn) menuBtn.addEventListener('click', () => this.toggle());
            if (overlay) overlay.addEventListener('click', () => this.close());
            document.addEventListener('keydown', e => { if (e.key === 'Escape') this.close(); });
        },
        toggle() { if (this.el) this.el.classList.toggle('open'); },
        close() { if (this.el) this.el.classList.remove('open'); }
    };

    // Alerts auto-dismiss
    const Alerts = {
        init() {
            document.querySelectorAll('.alert-success, .alert-info').forEach(alert => {
                setTimeout(() => {
                    alert.style.opacity = '0';
                    alert.style.transform = 'translateY(-8px)';
                    setTimeout(() => alert.remove(), 300);
                }, 5000);
            });
        }
    };

    // Time display
    const TimeDisplay = {
        init() {
            this.el = document.getElementById('currentTime');
            if (this.el) { this.update(); setInterval(() => this.update(), 30000); }
        },
        update() {
            this.el.textContent = new Date().toLocaleDateString('en-US', {
                weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
            });
        }
    };

    // Tabs
    const Tabs = {
        init() {
            document.querySelectorAll('.tabs').forEach(tabs => {
                tabs.querySelectorAll('.tab-btn').forEach(btn => {
                    btn.addEventListener('click', () => {
                        tabs.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
                        btn.classList.add('active');
                        const parent = tabs.parentElement;
                        parent.querySelectorAll('.tab-content').forEach(c => {
                            c.classList.toggle('active', c.id === btn.dataset.tab);
                        });
                    });
                });
            });
        }
    };

    // Modal
    const Modal = {
        open(id) { document.getElementById(id)?.classList.add('active'); document.body.style.overflow = 'hidden'; },
        close(id) { document.getElementById(id)?.classList.remove('active'); document.body.style.overflow = ''; },
        init() {
            document.querySelectorAll('.modal-backdrop').forEach(m => {
                m.addEventListener('click', e => { if (e.target === m) { m.classList.remove('active'); document.body.style.overflow = ''; } });
            });
            document.querySelectorAll('.modal-close').forEach(btn => {
                btn.addEventListener('click', () => { btn.closest('.modal-backdrop')?.classList.remove('active'); document.body.style.overflow = ''; });
            });
        }
    };

    // Password toggle
    function initPasswordToggle() {
        document.querySelectorAll('.toggle-password').forEach(btn => {
            btn.addEventListener('click', () => {
                const input = btn.parentElement.querySelector('input');
                if (input) { input.type = input.type === 'password' ? 'text' : 'password'; }
            });
        });
    }

    // Load Balancer UI
    const LoadBalancer = {
        init() {
            document.querySelectorAll('.lb-strategy-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    document.querySelectorAll('.lb-strategy-btn').forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');
                });
            });
            document.querySelectorAll('.lb-target-weight input[type="range"]').forEach(s => {
                s.addEventListener('input', () => { if (s.nextElementSibling) s.nextElementSibling.textContent = s.value + '%'; });
            });
        }
    };

    // Init all
    document.addEventListener('DOMContentLoaded', () => {
        ThemeManager.init();
        Sidebar.init();
        Alerts.init();
        TimeDisplay.init();
        Tabs.init();
        Modal.init();
        initPasswordToggle();
        LoadBalancer.init();
    });

    window.Nawala = { Theme: ThemeManager, Sidebar, Modal };
})();
