const API_BASE = "";
let currentUser = null;

document.addEventListener("DOMContentLoaded", () => {
    const storedUser = localStorage.getItem("ie_user");

    const btnOpenDocente = document.getElementById("btn-open-docente");
    const btnCloseDocente = document.getElementById("btn-close-docente");
    const btnCancelDocente = document.getElementById("btn-cancel-docente");
    const modalDocente = document.getElementById("modal-docente");

    btnOpenDocente?.addEventListener("click", () => {
        modalDocente.classList.remove("hidden");
    });

    btnCloseDocente?.addEventListener("click", () => {
        modalDocente.classList.add("hidden");
    });

    btnCancelDocente?.addEventListener("click", () => {
        modalDocente.classList.add("hidden");
    });

    if (!storedUser) {
        window.location.href = "login.html";
        return;
    }

    currentUser = JSON.parse(storedUser);

    if (currentUser.rol !== "Director") {
        window.location.href = "login.html";
        return;
    }

    modalDocente?.addEventListener("click", (e) => {
        if (e.target === modalDocente) {
            modalDocente.classList.add("hidden");
        }
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            modalDocente?.classList.add("hidden");
        }
    });

    document.getElementById("user-display-name").innerText = currentUser.nombre;
    document.getElementById("user-role-label").innerText = `Rol: ${currentUser.rol}`;

    const sidebarUserName = document.getElementById("sidebar-user-name");
    if (sidebarUserName) sidebarUserName.innerText = currentUser.nombre;

    document.getElementById("btn-logout").addEventListener("click", handleLogout);

    document.getElementById("form-dir-matricula").addEventListener("submit", handleDirMatricula);
    document.getElementById("form-dir-reg-alumno").addEventListener("submit", handleDirRegAlumno);
    document.getElementById("form-dir-reg-docente").addEventListener("submit", handleDirRegDocente);

    setupDirectorNavigation();
    setupMatriculaModal();
    setupAlumnoModal();
    setupSearchAlumno();

    loadDirectorDashboard();
});

function setupAlumnoModal() {
    const modal = document.getElementById("modal-alumno");
    const openBtn = document.getElementById("btn-open-alumno");
    const closeBtn = document.getElementById("btn-close-alumno");
    const cancelBtn = document.getElementById("btn-cancel-alumno");

    if (!modal || !openBtn) return;

    openBtn.addEventListener("click", openAlumnoModal);
    closeBtn.addEventListener("click", closeAlumnoModal);
    cancelBtn.addEventListener("click", closeAlumnoModal);

    modal.addEventListener("click", (e) => {
        if (e.target === modal) closeAlumnoModal();
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && !modal.classList.contains("hidden")) {
            closeAlumnoModal();
        }
    });
}

function openAlumnoModal() {
    document.getElementById("modal-alumno").classList.remove("hidden");
}

function closeAlumnoModal() {
    const modal = document.getElementById("modal-alumno");
    const form = document.getElementById("form-dir-reg-alumno");

    modal.classList.add("hidden");
    form.reset();
}

/* =========================
   NAVEGACIÓN SIDEBAR
========================= */

function setupDirectorNavigation() {
    const navButtons = document.querySelectorAll(".sidebar-link");
    const quickButtons = document.querySelectorAll(".quick-card");
    const sections = document.querySelectorAll(".director-section");

    function showSection(sectionId) {
        sections.forEach(section => {
            section.classList.add("hidden");
            section.classList.remove("active");
        });

        const target = document.getElementById(sectionId);
        if (target) {
            target.classList.remove("hidden");
            target.classList.add("active");
        }

        navButtons.forEach(btn => {
            btn.classList.toggle("active", btn.dataset.section === sectionId);
        });
    }

    navButtons.forEach(button => {
        button.addEventListener("click", () => showSection(button.dataset.section));
    });

    quickButtons.forEach(button => {
        button.addEventListener("click", () => showSection(button.dataset.section));
    });
}

/* =========================
   MODAL MATRÍCULA
========================= */

function setupMatriculaModal() {
    const modal = document.getElementById("modal-matricula");
    const openBtn = document.getElementById("btn-open-matricula");
    const closeBtn = document.getElementById("btn-close-matricula");
    const cancelBtn = document.getElementById("btn-cancel-matricula");

    if (!modal || !openBtn) return;

    openBtn.addEventListener("click", openMatriculaModal);
    closeBtn.addEventListener("click", closeMatriculaModal);
    cancelBtn.addEventListener("click", closeMatriculaModal);

    modal.addEventListener("click", (e) => {
        if (e.target === modal) closeMatriculaModal();
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && !modal.classList.contains("hidden")) {
            closeMatriculaModal();
        }
    });
}

function openMatriculaModal() {
    document.getElementById("modal-matricula").classList.remove("hidden");
}

function closeMatriculaModal() {
    const modal = document.getElementById("modal-matricula");
    const form = document.getElementById("form-dir-matricula");

    modal.classList.add("hidden");
    form.reset();
}

/* =========================
   BUSCADOR ALUMNOS
========================= */

function setupSearchAlumno() {
    const searchInput = document.querySelector(".search-input");

    if (!searchInput) return;

    searchInput.addEventListener("input", () => {
        const value = searchInput.value.toLowerCase();
        const rows = document.querySelectorAll("#dir-alumnos-table-body tr");

        rows.forEach(row => {
            const text = row.innerText.toLowerCase();
            row.style.display = text.includes(value) ? "" : "none";
        });
    });
}

/* =========================
   SESIÓN
========================= */

function handleLogout() {
    localStorage.removeItem("ie_user");
    window.location.href = "login.html";
}

/* =========================
   TOAST
========================= */

function showToast(message, type = "success") {
    const toast = document.getElementById("toast");
    toast.innerText = message;
    toast.className = `toast ${type === "danger" ? "bg-danger" : "bg-primary"}`;
    toast.classList.remove("hidden");

    setTimeout(() => {
        toast.classList.add("hidden");
    }, 3500);
}

/* =========================
   DATA
========================= */

function parseDashboardPayload(rawText) {
    const normalized = rawText.replace(/:(\s*-?\d+),(\d+)(?=[,}])/g, ":$1.$2");
    return JSON.parse(normalized);
}

async function loadDirectorDashboard() {
    try {
        const res = await fetch(`${API_BASE}/api/dashboard?userId=${currentUser.userId}&rol=Director`);
        const rawText = await res.text();
        const data = parseDashboardPayload(rawText);

        if (!data.success) return;

        document.getElementById("dir-stat-alumnos").innerText = data.dashboard.totalAlumnos;
        document.getElementById("dir-stat-docentes").innerText = data.dashboard.totalDocentes;
        document.getElementById("dir-stat-cursos").innerText = data.dashboard.totalCursos;
        document.getElementById("dir-stat-recaudacion").innerText = `S/. ${data.dashboard.totalRecaudado.toFixed(2)}`;

        cargarAlumnos(data.alumnos);
        cargarDocentes(data.docentes);
        cargarPagos(data.pagos);

    } catch (error) {
        showToast("Error al cargar la información del director", "danger");
    }
}

function cargarAlumnos(alumnos) {
    const matAlumnoSelect = document.getElementById("dir-mat-alumno");
    const alumnosTbody = document.getElementById("dir-alumnos-table-body");

    matAlumnoSelect.innerHTML = "<option value=''>Seleccione Alumno</option>";
    alumnosTbody.innerHTML = "";

    const total = alumnos.length;
    const matriculados = alumnos.filter(a => a.grado && a.seccion).length;
    const sinMatricula = total - matriculados;

    document.getElementById("mini-total-alumnos").innerText = total;
    document.getElementById("mini-matriculados").innerText = matriculados;
    document.getElementById("mini-sin-matricula").innerText = sinMatricula;
    document.getElementById("alumnos-table-count").innerText = `Mostrando ${total} alumno(s) registrados`;

    alumnos.forEach(a => {
        matAlumnoSelect.innerHTML += `<option value="${a.id}">${a.nombre} (${a.id})</option>`;

        const gradoStr = a.grado
            ? `${a.grado} "${a.seccion}"`
            : `<span class="badge badge-warning">Sin Matricular</span>`;

        alumnosTbody.innerHTML += `
            <tr>
                <td>${a.id}</td>
                <td>
                    <strong>${a.nombre}</strong>
                    <br>
                    <span class="text-muted">Código: ${a.id}</span>
                </td>
                <td>${gradoStr}</td>
                <td><span class="badge badge-info">${a.desempeno}</span></td>
                <td>
                    <button class="btn btn-danger btn-small" onclick="deleteUser('${a.id}')">
                        Eliminar
                    </button>
                </td>
            </tr>
        `;
    });
}

function cargarDocentes(docentes) {
    const docentesList = document.getElementById("dir-docentes-list");
    docentesList.innerHTML = "";

    docentes.forEach(d => {
        docentesList.innerHTML += `
            <div class="list-item">
                <div class="list-item-header">
                    <span>${d.nombre} (${d.id})</span>
                    <button class="btn btn-danger btn-small" onclick="deleteUser('${d.id}')">
                        Eliminar
                    </button>
                </div>

                <div class="list-item-desc">
                    Especialidad: ${d.especialidad}
                </div>

                <div class="list-item-footer">
                    <span class="badge badge-success">
                        Asistencia Docente: Conectado
                    </span>
                </div>
            </div>
        `;
    });
}

function cargarPagos(pagos) {
    const pagosTbody = document.getElementById("dir-pagos-table-body");
    pagosTbody.innerHTML = "";

    pagos.forEach(p => {
        const actionBtn = p.pagado
            ? `<span class="badge badge-success">Saldado</span>`
            : `<button class="btn btn-secondary btn-small" onclick="realizePaymentDir('${p.id}')">Cobrar</button>`;

        pagosTbody.innerHTML += `
            <tr>
                <td>${p.alumno}</td>
                <td>S/. ${p.monto.toFixed(2)}</td>
                <td>${p.concepto}</td>
                <td>
                    <span class="badge ${p.pagado ? "badge-success" : "badge-danger"}">
                        ${p.pagado ? "Pagado" : "Pendiente"}
                    </span>
                </td>
                <td>${actionBtn}</td>
            </tr>
        `;
    });
}

/* =========================
   ACCIONES
========================= */

async function handleDirMatricula(e) {
    e.preventDefault();

    const alumnoId = document.getElementById("dir-mat-alumno").value;
    const grado = document.getElementById("dir-mat-grado").value;
    const seccion = document.getElementById("dir-mat-seccion").value;

    try {
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=registrarMatricula&idAlumno=${alumnoId}&grado=${grado}&seccion=${seccion}`
        });

        const data = await res.json();

        if (data.success) {
            showToast("¡Matrícula realizada con éxito!");
            closeMatriculaModal();
            loadDirectorDashboard();
        } else {
            showToast(data.message, "danger");
        }

    } catch (error) {
        showToast("Error al procesar la matrícula", "danger");
    }
}

async function handleDirRegAlumno(e) {
    e.preventDefault();

    const id = document.getElementById("reg-al-id").value.trim();
    const nombre = document.getElementById("reg-al-nombre").value.trim();
    const username = document.getElementById("reg-al-user").value.trim();
    const pass = document.getElementById("reg-al-pass").value;

    try {
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=registrarAlumno&id=${id}&nombre=${encodeURIComponent(nombre)}&username=${encodeURIComponent(username)}&password=${encodeURIComponent(pass)}&grado=&seccion=`
        });

        const data = await res.json();

        if (data.success) {
            showToast("¡Estudiante registrado de forma segura!");
            closeAlumnoModal();
            loadDirectorDashboard();
        } else {
            showToast(data.message, "danger");
        }

    } catch (error) {
        showToast("Error al registrar estudiante", "danger");
    }
}

async function handleDirRegDocente(e) {
    e.preventDefault();

    const id = document.getElementById("reg-doc-id").value.trim();
    const nombre = document.getElementById("reg-doc-nombre").value.trim();
    const username = document.getElementById("reg-doc-user").value.trim();
    const pass = document.getElementById("reg-doc-pass").value;
    const esp = document.getElementById("reg-doc-esp").value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=registrarDocente&id=${id}&nombre=${encodeURIComponent(nombre)}&username=${encodeURIComponent(username)}&password=${encodeURIComponent(pass)}&especialidad=${encodeURIComponent(esp)}`
        });

        const data = await res.json();

        if (data.success) {
            showToast("¡Docente registrado con éxito!");
            document.getElementById("form-dir-reg-docente").reset();
            loadDirectorDashboard();
        } else {
            showToast(data.message, "danger");
        }

    } catch (error) {
        showToast("Error al registrar docente", "danger");
    }
}

async function realizePaymentDir(pagoId) {
    try {
        const todayStr = new Date().toISOString().split("T")[0];

        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=pagar&idPago=${pagoId}&fecha=${todayStr}`
        });

        const data = await res.json();

        if (data.success) {
            showToast("Pago registrado en el sistema escolar.");
            loadDirectorDashboard();
        } else {
            showToast(data.message, "danger");
        }

    } catch (error) {
        showToast("Error al registrar cobro", "danger");
    }
}

async function deleteUser(id) {
    if (!confirm(`¿Está seguro de eliminar al usuario con ID ${id} permanentemente?`)) return;

    try {
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=eliminarUsuario&id=${id}`
        });

        const data = await res.json();

        if (data.success) {
            showToast("Usuario removido del sistema.");
            loadDirectorDashboard();
        } else {
            showToast(data.message, "danger");
        }

    } catch (error) {
        showToast("Error al eliminar usuario", "danger");
    }
}

/* =========================
   TABS USUARIOS
========================= */

function switchDirTab(tabId) {
    document.querySelectorAll("#section-usuarios .tab-pane").forEach(pane => {
        pane.classList.add("hidden");
    });

    document.querySelectorAll("#section-usuarios .tab-btn").forEach(btn => {
        btn.classList.remove("active");
    });

    document.getElementById(tabId).classList.remove("hidden");

    if (window.event && window.event.currentTarget) {
        window.event.currentTarget.classList.add("active");
    }
}