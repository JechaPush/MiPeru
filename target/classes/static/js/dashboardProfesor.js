const API_BASE = "";
let currentUser = null;

// ──────────────────────────────────────────────
//  INICIALIZACIÓN
// ──────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
    const storedUser = localStorage.getItem("ie_user");
    if (!storedUser) {
        window.location.href = "/html/login.html";
        return;
    }

    currentUser = JSON.parse(storedUser);
    if (currentUser.rol !== "Profesor") {
        window.location.href = "/html/login.html";
        return;
    }

    // Datos del usuario en header y sidebar
    document.getElementById("user-display-name").innerText = currentUser.nombre;
    document.getElementById("user-role-label").innerText = `Rol: ${currentUser.rol}`;

    // Avatar con iniciales
    const initials = currentUser.nombre
        ? currentUser.nombre.trim().split(" ").map(w => w[0]).join("").substring(0, 2).toUpperCase()
        : "P";
    document.getElementById("prof-avatar").innerText = initials;
    document.getElementById("prof-perfil-avatar").innerText = initials;

    // Logout
    document.getElementById("btn-logout").addEventListener("click", handleLogout);

    // Form inputs de fecha
    document.querySelectorAll('input[type="date"]').forEach(input => {
        input.value = new Date().toISOString().split('T')[0];
    });

    // Listeners de Formularios (Preservados)
    document.getElementById("form-prof-notas").addEventListener("submit", handleProfRegisterNota);
    document.getElementById("form-prof-asistencia").addEventListener("submit", handleProfRegisterAsistencia);
    document.getElementById("form-prof-tareas").addEventListener("submit", handleProfCreateTarea);
    document.getElementById("form-prof-anuncio").addEventListener("submit", handleProfCreateAnuncio);
    document.getElementById("form-grade-submission").addEventListener("submit", handleProfGradeSubmission);

    // ── Sidebar: navegación modular ──
    document.getElementById("prof-sidebar-nav").addEventListener("click", (e) => {
        const btn = e.target.closest(".prof-nav-item");
        if (!btn) return;
        switchTeacherModule(btn.dataset.module, btn.dataset.title);
        closeSidebarOnMobile();
    });

    // ── Accesos rápidos de Inicio ──
    document.querySelectorAll(".prof-quick-card").forEach(card => {
        card.addEventListener("click", () => {
            switchTeacherModule(card.dataset.module, card.dataset.title);
        });
    });

    // ── Toggle sidebar en móvil ──
    document.getElementById("prof-toggle-sidebar").addEventListener("click", toggleSidebar);
    document.getElementById("prof-overlay").addEventListener("click", closeSidebarOnMobile);

    // Cargar datos
    loadProfesorDashboard();
});

// ──────────────────────────────────────────────
//  NAVEGACIÓN MODULAR
// ──────────────────────────────────────────────
function switchTeacherModule(moduleName, moduleTitle) {
    // 1. Ocultar todos los módulos
    document.querySelectorAll(".prof-module").forEach(m => {
        m.classList.add("hidden");
        m.classList.remove("active");
    });

    // 2. Mostrar el módulo solicitado
    const target = document.getElementById(`module-${moduleName}`);
    if (target) {
        target.classList.remove("hidden");
        target.classList.add("active", "fade-in");
    }

    // 3. Actualizar ítem activo del sidebar
    document.querySelectorAll(".prof-nav-item").forEach(btn => {
        btn.classList.remove("active");
        if (btn.dataset.module === moduleName) {
            btn.classList.add("active");
        }
    });

    // 4. Actualizar título del header
    if (moduleTitle) {
        document.getElementById("prof-module-title").innerText = moduleTitle;
    }
}

// ──────────────────────────────────────────────
//  SIDEBAR MÓVIL
// ──────────────────────────────────────────────
function toggleSidebar() {
    const sidebar = document.getElementById("prof-sidebar");
    const overlay = document.getElementById("prof-overlay");
    const isOpen = sidebar.classList.toggle("open");
    overlay.classList.toggle("hidden", !isOpen);
}

function closeSidebarOnMobile() {
    const sidebar = document.getElementById("prof-sidebar");
    const overlay = document.getElementById("prof-overlay");
    if (window.innerWidth <= 768) {
        sidebar.classList.remove("open");
        overlay.classList.add("hidden");
    }
}

// ──────────────────────────────────────────────
//  LOGOUT (Preservada)
// ──────────────────────────────────────────────
function handleLogout() {
    localStorage.removeItem("ie_user");
    window.location.href = "/html/login.html";
}

// ──────────────────────────────────────────────
//  TOAST (Preservada)
// ──────────────────────────────────────────────
function showToast(message, type = "success") {
    const toast = document.getElementById("toast");
    toast.innerText = message;
    toast.className = `toast ${type === 'danger' ? 'bg-danger' : 'bg-primary'}`;
    toast.classList.remove("hidden");

    setTimeout(() => {
        toast.classList.add("hidden");
    }, 3500);
}

// ──────────────────────────────────────────────
//  CARGA PRINCIPAL (Adaptada para Poblar Módulos)
// ──────────────────────────────────────────────
async function loadProfesorDashboard() {
    try {
        const res = await fetch(`${API_BASE}/api/dashboard?userId=${currentUser.userId}&rol=Profesor`);
        const data = await res.json();
        if (!data.success) return;

        // 1. Calcular contadores para Inicio
        const totalCursos = data.cursos.length;
        const totalAlumnos = data.cursos.reduce((sum, c) => sum + (c.alumnosCount || 0), 0);
        const totalTareas = data.tareas.length;
        
        let totalEntregasPendientes = 0;
        data.tareas.forEach(t => {
            Object.keys(t.entregas).forEach(alumnoId => {
                const ent = t.entregas[alumnoId];
                if (ent && ent.estado === "ENVIADO") {
                    totalEntregasPendientes++;
                }
            });
        });

        // Poblar indicadores en Inicio
        document.getElementById("prof-total-cursos").innerText = totalCursos;
        document.getElementById("prof-total-alumnos").innerText = totalAlumnos;
        document.getElementById("prof-total-tareas").innerText = totalTareas;
        document.getElementById("prof-total-entregas").innerText = totalEntregasPendientes;

        // 2. Mis Cursos (Módulo 2)
        const cursosCont = document.getElementById("prof-cursos-container");
        cursosCont.innerHTML = "";

        const notaCursoSelect = document.getElementById("prof-nota-curso");
        const asisCursoSelect = document.getElementById("prof-asis-curso");
        const tarCursoSelect = document.getElementById("prof-tar-curso");

        notaCursoSelect.innerHTML = "<option value=''>Seleccione Curso</option>";
        asisCursoSelect.innerHTML = "<option value=''>Seleccione Curso</option>";
        tarCursoSelect.innerHTML = "<option value=''>Seleccione Curso</option>";

        data.cursos.forEach(c => {
            cursosCont.innerHTML += `
                <div class="course-box">
                    <h4>${c.nombre}</h4>
                    <p>Grado: ${c.grado} - Sección: ${c.seccion}</p>
                    <p>Total Alumnos: <strong>${c.alumnosCount}</strong></p>
                </div>
            `;

            const opt = `<option value="${c.id}">${c.nombre} (${c.grado} ${c.seccion})</option>`;
            notaCursoSelect.innerHTML += opt;
            asisCursoSelect.innerHTML += opt;
            tarCursoSelect.innerHTML += opt;
        });

        notaCursoSelect.onchange = () => loadCourseStudents(notaCursoSelect.value, "prof-nota-alumno");
        asisCursoSelect.onchange = () => loadCourseStudents(asisCursoSelect.value, "prof-asis-alumno");

        // 3. Mi Horario Cronológico (Módulo 7)
        const orderDays = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
        const horariosOrdenados = [...data.horarios].sort((a, b) => {
            return orderDays.indexOf(a.dia) - orderDays.indexOf(b.dia);
        });

        const horarioList = document.getElementById("prof-horario-list");
        horarioList.innerHTML = "";
        horariosOrdenados.forEach(h => {
            horarioList.innerHTML += `
                <div class="list-item">
                    <div class="list-item-header">
                        <span>${h.cursoNombre}</span>
                        <span class="badge badge-info">${h.dia}</span>
                    </div>
                    <div class="list-item-desc">Horario: ${h.inicio} - ${h.fin} en <strong>${h.aula}</strong></div>
                </div>
            `;
        });

        // 4. Anuncios (Módulo 8)
        const anunciosList = document.getElementById("prof-anuncios-list");
        anunciosList.innerHTML = "";
        data.anuncios.forEach(a => {
            const fechaHtml = (a.fecha && a.fecha !== "undefined")
                ? `<small>${a.fecha}</small>`
                : "";

            anunciosList.innerHTML += `
                <div class="list-item">
                    <div class="list-item-header">
                        <span>${a.titulo}</span>
                        ${fechaHtml}
                    </div>
                    <div class="list-item-desc">${a.contenido}</div>
                    <div class="list-item-footer">
                        <small>Publicado por: ${a.autor}</small>
                    </div>
                </div>
            `;
        });

        // 5. Entregas de Alumnos (Módulo 6)
        const entregasList = document.getElementById("prof-entregas-list");
        entregasList.innerHTML = "";
        let hasSubmissions = false;

        data.tareas.forEach(t => {
            Object.keys(t.entregas).forEach(alumnoId => {
                const ent = t.entregas[alumnoId];
                if (!ent) return;
                hasSubmissions = true;

                const btnGrade = ent.estado === "ENVIADO"
                    ? `<button class="btn btn-secondary mt-2" onclick="openGradeModal('${t.id}', '${alumnoId}', '${ent.archivoEnviado}')">Calificar y Revisar</button>`
                    : `<span class="badge badge-success mt-2">Calificado: ${ent.calificacion}</span>`;

                const fechaEnvioHtml = (ent.fechaEnvio && ent.fechaEnvio !== "undefined")
                    ? ` (Entregado el ${ent.fechaEnvio})`
                    : "";

                entregasList.innerHTML += `
                    <div class="list-item">
                        <div class="list-item-header">
                            <span>Tarea: ${t.titulo}</span>
                            <small class="badge badge-info">${ent.estado}</small>
                        </div>
                        <div class="list-item-desc">
                            Alumno ID: <strong>${alumnoId}</strong><br>
                            Archivo entregado: <a href="#" onclick="alert('Descargando archivo enviado...')">${ent.archivoEnviado}</a>${fechaEnvioHtml}
                        </div>
                        ${btnGrade}
                    </div>
                `;
            });
        });

        if (!hasSubmissions) {
            entregasList.innerHTML = '<div class="list-item text-muted">No se registran entregas de alumnos aún.</div>';
        }

        // 6. Previews de Inicio (Fila 4), Perfil (Módulo 9) y Tareas Publicadas (Módulo 5)
        renderInicioPreviews(horariosOrdenados, data.anuncios);
        renderPerfil(totalCursos, totalAlumnos);
        renderTareasPublicadas(data.tareas, data.cursos);

    } catch (error) {
        showToast("Error al cargar la información del docente", "danger");
    }
}

// ──────────────────────────────────────────────
//  PREVIEWS DE INICIO Y PERFIL
// ──────────────────────────────────────────────
function renderInicioPreviews(horarios, anuncios) {
    const previewsContainer = document.getElementById("prof-inicio-previews");
    const clasePreview = document.getElementById("prof-inicio-clase-preview");
    const anuncioPreview = document.getElementById("prof-inicio-anuncio-preview");

    let hasVisiblePreview = false;

    // Próxima clase
    const proximaClase = (horarios && horarios.length > 0) ? horarios[0] : null;
    if (proximaClase) {
        document.getElementById("prof-inicio-clase-titulo").innerText = proximaClase.cursoNombre;
        document.getElementById("prof-inicio-clase-horario").innerText = `${proximaClase.dia} (${proximaClase.inicio} - ${proximaClase.fin})`;
        clasePreview.classList.remove("hidden");
        hasVisiblePreview = true;
    } else {
        clasePreview.classList.add("hidden");
    }

    // Último anuncio
    const ultimoAnuncio = (anuncios && anuncios.length > 0) ? anuncios[0] : null;
    if (ultimoAnuncio) {
        document.getElementById("prof-inicio-anuncio-titulo").innerText = ultimoAnuncio.titulo;
        
        const fechaText = (ultimoAnuncio.fecha && ultimoAnuncio.fecha !== "undefined")
            ? `Fecha: ${ultimoAnuncio.fecha}`
            : `Por: ${ultimoAnuncio.autor}`;
            
        document.getElementById("prof-inicio-anuncio-fecha").innerText = fechaText;
        anuncioPreview.classList.remove("hidden");
        hasVisiblePreview = true;
    } else {
        anuncioPreview.classList.add("hidden");
    }

    if (hasVisiblePreview) {
        previewsContainer.classList.remove("hidden");
    } else {
        previewsContainer.classList.add("hidden");
    }
}

function renderPerfil(totalCursos, totalAlumnos) {
    // Identidad
    document.getElementById("prof-perfil-nombre").innerText = currentUser.nombre || "Profesor";
    document.getElementById("prof-perfil-rol").innerText = currentUser.rol || "Profesor";
    document.getElementById("prof-perfil-codigo").innerText = currentUser.userId ? `Código: ${currentUser.userId}` : "";

    // Información Profesional (Solo datos reales)
    const infoEl = document.getElementById("prof-perfil-info");
    const items = [
        { label: "Cursos asignados", value: totalCursos },
        { label: "Alumnos a cargo", value: totalAlumnos }
    ];

    infoEl.innerHTML = items.map(item => `
        <div class="prof-perfil-row">
            <span class="prof-perfil-label">${item.label}</span>
            <span class="prof-perfil-value">${item.value}</span>
        </div>
    `).join("");
}

function renderTareasPublicadas(tareas, cursos) {
    const container = document.getElementById("prof-published-tasks-container");
    if (!container) return;
    container.innerHTML = "";

    if (!tareas || tareas.length === 0) {
        container.innerHTML = `<div class="prof-empty-state">Aún no hay tareas publicadas disponibles para mostrar.</div>`;
        return;
    }

    const todayStr = new Date().toISOString().split('T')[0];

    // Ordenar: activas con fecha más próxima primero, luego vencidas
    const sortedTareas = [...tareas].sort((a, b) => {
        const hasA = a.fechaEntrega && a.fechaEntrega !== "undefined";
        const hasB = b.fechaEntrega && b.fechaEntrega !== "undefined";

        const isAExpired = hasA && a.fechaEntrega < todayStr;
        const isBExpired = hasB && b.fechaEntrega < todayStr;

        // Activas antes de vencidas
        if (!isAExpired && isBExpired) return -1;
        if (isAExpired && !isBExpired) return 1;

        // Si ambas son activas o ambas vencidas, ordenar cronológicamente por fecha
        if (hasA && hasB) {
            return a.fechaEntrega.localeCompare(b.fechaEntrega);
        }
        if (hasA) return -1;
        if (hasB) return 1;
        return 0;
    });

    let tableHtml = `
        <div class="table-container">
            <table class="table prof-tasks-tabla">
                <thead>
                    <tr>
                        <th>Título</th>
                        <th>Curso y sección</th>
                        <th>Fecha de entrega</th>
                        <th>Vigencia</th>
                        <th>Entregas recibidas</th>
                        <th>Acción</th>
                    </tr>
                </thead>
                <tbody>
    `;

    sortedTareas.forEach(t => {
        const hasDate = t.fechaEntrega && t.fechaEntrega !== "undefined";
        const isExpired = hasDate && t.fechaEntrega < todayStr;

        let vigenciaLabel = "Activa";
        let vigenciaClass = "badge-success";
        let fechaEntregaLabel = t.fechaEntrega;

        if (!hasDate) {
            fechaEntregaLabel = "Sin fecha definida";
            vigenciaLabel = "Activa";
            vigenciaClass = "badge-success";
        } else if (isExpired) {
            vigenciaLabel = "Vencida";
            vigenciaClass = "badge-danger";
        }

        const cursoObj = cursos ? cursos.find(c => c.id === t.idCurso) : null;
        const cursoNombre = cursoObj ? `${cursoObj.nombre} (${cursoObj.grado} ${cursoObj.seccion})` : t.idCurso;

        const entregasCount = t.entregas ? Object.keys(t.entregas).length : 0;
        const entregasLabel = entregasCount === 1 ? "1 entrega" : `${entregasCount} entregas`;

        tableHtml += `
            <tr>
                <td><strong>${t.titulo}</strong></td>
                <td>${cursoNombre}</td>
                <td>${fechaEntregaLabel}</td>
                <td><span class="badge ${vigenciaClass}">${vigenciaLabel}</span></td>
                <td>${entregasLabel}</td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="switchTeacherModule('entregas', 'Entregas de Alumnos')">
                        Ver entregas
                    </button>
                </td>
            </tr>
        `;
    });

    tableHtml += `
                </tbody>
            </table>
        </div>
    `;

    container.innerHTML = tableHtml;
}

// ──────────────────────────────────────────────
//  ALUMNOS POR CURSO (Preservada)
// ──────────────────────────────────────────────
async function loadCourseStudents(cursoId, selectElementId) {
    const select = document.getElementById(selectElementId);
    select.innerHTML = "<option value=''>Cargando alumnos...</option>";

    if (!cursoId) {
        select.innerHTML = "<option value=''>Seleccione un curso primero</option>";
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/dashboard?userId=DIR01&rol=Director`);
        const data = await res.json();

        const coursesRes = await fetch(`${API_BASE}/api/dashboard?userId=${currentUser.userId}&rol=Profesor`);
        const coursesData = await coursesRes.json();
        const activeCourse = coursesData.cursos.find(c => c.id === cursoId);

        select.innerHTML = "<option value=''>Seleccione Alumno</option>";
        data.alumnos.forEach(a => {
            if (activeCourse && a.grado === activeCourse.grado && a.seccion === activeCourse.seccion) {
                select.innerHTML += `<option value="${a.id}">${a.nombre} (${a.id})</option>`;
            }
        });
    } catch (error) {
        select.innerHTML = "<option value=''>Error al cargar alumnos</option>";
    }
}

// ──────────────────────────────────────────────
//  SUBMITS Y FORMULARIOS (Preservadas)
// ──────────────────────────────────────────────
async function handleProfRegisterNota(e) {
    e.preventDefault();
    const cursoId = document.getElementById("prof-nota-curso").value;
    const alumnoId = document.getElementById("prof-nota-alumno").value;
    const notaValor = document.getElementById("prof-nota-valor").value;
    const periodo = document.getElementById("prof-nota-periodo").value;

    try {
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=registrarNota&idAlumno=${alumnoId}&idCurso=${cursoId}&valor=${notaValor}&periodo=${encodeURIComponent(periodo)}`
        });
        const data = await res.json();
        if (data.success) {
            showToast("¡Nota registrada con conversión automática exitosa!");
            document.getElementById("form-prof-notas").reset();
            loadProfesorDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (error) {
        showToast("Error al registrar la calificación.", "danger");
    }
}

async function handleProfRegisterAsistencia(e) {
    e.preventDefault();
    const alumnoId = document.getElementById("prof-asis-alumno").value;
    const estado = document.getElementById("prof-asis-estado").value;
    const fecha = document.getElementById("prof-asis-fecha").value;
    const obs = document.getElementById("prof-asis-obs").value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=registrarAsistencia&idUsuario=${alumnoId}&fecha=${fecha}&estado=${estado}&observacion=${encodeURIComponent(obs)}&esDocente=false`
        });
        const data = await res.json();
        if (data.success) {
            showToast("¡Asistencia guardada correctamente!");
            document.getElementById("form-prof-asistencia").reset();
            document.getElementById("prof-asis-fecha").value = new Date().toISOString().split('T')[0];
            loadProfesorDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (error) {
        showToast("Error al registrar la asistencia.", "danger");
    }
}

async function handleProfCreateTarea(e) {
    e.preventDefault();
    const cursoId = document.getElementById("prof-tar-curso").value;
    const titulo = document.getElementById("prof-tar-titulo").value.trim();
    const fecha = document.getElementById("prof-tar-fecha").value;
    const desc = document.getElementById("prof-tar-desc").value.trim();
    const archivo = document.getElementById("prof-tar-archivo").value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=crearTarea&idCurso=${cursoId}&titulo=${encodeURIComponent(titulo)}&descripcion=${encodeURIComponent(desc)}&fechaEntrega=${fecha}&archivo=${encodeURIComponent(archivo)}`
        });
        const data = await res.json();
        if (data.success) {
            showToast("¡Tarea creada y publicada en el portal!");
            document.getElementById("form-prof-tareas").reset();
            document.getElementById("prof-tar-fecha").value = new Date().toISOString().split('T')[0];
            loadProfesorDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (error) {
        showToast("Error al crear la tarea", "danger");
    }
}

async function handleProfCreateAnuncio(e) {
    e.preventDefault();
    const titulo = document.getElementById("prof-anuncio-titulo").value.trim();
    const cont = document.getElementById("prof-anuncio-cont").value.trim();
    const today = new Date().toISOString().split('T')[0];

    try {
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=publicarAnuncio&titulo=${encodeURIComponent(titulo)}&contenido=${encodeURIComponent(cont)}&fecha=${today}&autor=${encodeURIComponent(currentUser.nombre)}`
        });
        const data = await res.json();
        if (data.success) {
            showToast("¡Anuncio escolar publicado!");
            document.getElementById("form-prof-anuncio").reset();
            loadProfesorDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (error) {
        showToast("Error al publicar el anuncio", "danger");
    }
}

// ──────────────────────────────────────────────
//  MODAL CALIFICACIONES (Preservadas)
// ──────────────────────────────────────────────
function openGradeModal(tareaId, alumnoId, filename) {
    document.getElementById("modal-task-id").value = tareaId;
    document.getElementById("modal-student-id").value = alumnoId;
    document.getElementById("modal-student-name").innerText = alumnoId;
    document.getElementById("modal-filename").innerText = filename;
    document.getElementById("grade-modal").classList.remove("hidden");
}

function closeGradeModal() {
    document.getElementById("grade-modal").classList.add("hidden");
    document.getElementById("form-grade-submission").reset();
}

async function handleProfGradeSubmission(e) {
    e.preventDefault();
    const tareaId = document.getElementById("modal-task-id").value;
    const alumnoId = document.getElementById("modal-student-id").value;
    const grade = document.getElementById("modal-grade").value;
    const comment = document.getElementById("modal-comment").value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=calificarTarea&idTarea=${tareaId}&idAlumno=${alumnoId}&calificacion=${grade}&comentarios=${encodeURIComponent(comment)}`
        });
        const data = await res.json();
        if (data.success) {
            showToast("¡Calificación registrada correctamente!");
            closeGradeModal();
            loadProfesorDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (error) {
        showToast("Error al calificar el envío", "danger");
    }
}