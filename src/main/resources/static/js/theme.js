const root = document.documentElement;

document.addEventListener("DOMContentLoaded", () => {
    const themeSelect = document.getElementById("theme-select");

    // Load saved theme or default
    const savedTheme = localStorage.getItem("theme") || "default";
    applyTheme(savedTheme);

    if (themeSelect) {
        themeSelect.value = savedTheme;

        themeSelect.addEventListener("change", () => {
            const theme = themeSelect.value;
            localStorage.setItem("theme", theme);
            applyTheme(theme);
        });
    }
});

function applyTheme(theme) {
    root.classList.remove("theme-pink", "theme-blue", "theme-purple", "theme-orange", "theme-yellow", "theme-teal", "theme-cyan", "theme-magenta", "theme-lime");

    if (theme === "pink") {
        root.classList.add("theme-pink");
    } else if (theme === "blue") {
        root.classList.add("theme-blue");
    }else if (theme === "purple") {
        root.classList.add("theme-purple");
    }else if (theme === "orange") {
        root.classList.add("theme-orange");
    }else if (theme === "yellow") {
        root.classList.add("theme-yellow");
    }else if (theme === "teal") {
        root.classList.add("theme-teal");
    }else if (theme === "cyan") {
        root.classList.add("theme-cyan");
    }else if (theme === "magenta") {
        root.classList.add("theme-magenta");
    }else if (theme === "lime") {
        root.classList.add("theme-lime");
    }
}
