const API_BASE = "";

document.addEventListener("DOMContentLoaded", () => {
    const storedUser = localStorage.getItem("ie_user");
    if (storedUser) {
        const currentUser = JSON.parse(storedUser);
        if (currentUser.rol === "Alumno") {
            window.location.href = "/html/dashboardAlumno.html";
        } else if (currentUser.rol === "Profesor") {
            window.location.href = "/html/dashboardProfesor.html";
        } else if (currentUser.rol === "Director") {
            window.location.href = "/html/dashboardDirector.html";
        } else {
            window.location.href = "/html/login.html";
        }
        return;
    }

    const loginForm = document.getElementById("login-form");
    if (loginForm) {
        loginForm.addEventListener("submit", handleLogin);
    }
});

async function handleLogin(e) {
    e.preventDefault();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();
    const btn = document.getElementById("btn-login");
    const err = document.getElementById("login-error");

    btn.disabled = true;
    err.classList.add("hidden");

    try {
        const res = await fetch(`${API_BASE}/api/login`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`
        });

        const data = await res.json();

        if (data.success) {
            localStorage.setItem("ie_user", JSON.stringify(data));
            if (data.rol === "Alumno") {
                window.location.href = "/html/dashboardAlumno.html";
            } else if (data.rol === "Profesor") {
                window.location.href = "/html/dashboardProfesor.html";
            } else if (data.rol === "Director") {
                window.location.href = "/html/dashboardDirector.html";
            } else {
                window.location.href = "/html/login.html";
            }
            return;
        }

        err.innerText = data.message || "Credenciales incorrectas.";
        err.classList.remove("hidden");
    } catch (error) {
        err.innerText = "Error de conexión con el servidor.";
        err.classList.remove("hidden");
    } finally {
        btn.disabled = false;
    }
}
/* =============================
   Mostrar / ocultar contraseña
============================= */

const passwordInput = document.getElementById("password");
const togglePassword = document.getElementById("toggle-password");

if (togglePassword) {

    togglePassword.addEventListener("click", () => {

        const isPassword = passwordInput.type === "password";

        passwordInput.type = isPassword ? "text" : "password";

        document.getElementById("eye-icon").innerHTML = isPassword
            ? `
                <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6z"/>
                <circle cx="12" cy="12" r="3"/>
              `
            : `
                <path d="M3 3l18 18"/>
                <path d="M10.5 6.3A9.5 9.5 0 0 1 12 6c6.5 0 10 6 10 6a18.8 18.8 0 0 1-3.3 3.9"/>
                <path d="M6.3 6.3A18.4 18.4 0 0 0 2 12s3.5 6 10 6a9.8 9.8 0 0 0 4.1-.9"/>
              `;

    });

}