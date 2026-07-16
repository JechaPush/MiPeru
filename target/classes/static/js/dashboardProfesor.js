const API_BASE = "";
let currentUser = null;

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

    document.getElementById("user-display-name").innerText = currentUser.nombre;
    document.getElementById("user-role-label").innerText = `Rol: ${currentUser.rol}`;
    document.getElementById("btn-logout").addEventListener("click", handleLogout);

    document.querySelectorAll('input[type="date"]').forEach(input => {
        input.value = new Date().toISOString().split('T')[0];
    });

    document.getElementById("form-prof-notas").addEventListener("submit", handleProfRegisterNota);
    document.getElementById("form-prof-asistencia").addEventListener("submit", handleProfRegisterAsistencia);
    document.getElementById("form-prof-tareas").addEventListener("submit", handleProfCreateTarea);
    document.getElementById("form-prof-anuncio").addEventListener("submit", handleProfCreateAnuncio);
    document.getElementById("form-grade-submission").addEventListener("submit", handleProfGradeSubmission);

    loadProfesorDashboard();
});

function handleLogout() {
    localStorage.removeItem("ie_user");
    window.location.href = "/html/login.html";
}

function showToast(message, type = "success") {
    const toast = document.getElementById("toast");
    toast.innerText = message;
    toast.className = `toast ${type === 'danger' ? 'bg-danger' : 'bg-primary'}`;
    toast.classList.remove("hidden");

    setTimeout(() => {
        toast.classList.add("hidden");
    }, 3500);
}

async function loadProfesorDashboard() {
    try {
        const res = await fetch(`${API_BASE}/api/dashboard?userId=${currentUser.userId}&rol=Profesor`);
        const data = await res.json();
        if (!data.success) return;

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

        const horarioList = document.getElementById("prof-horario-list");
        horarioList.innerHTML = "";
        data.horarios.forEach(h => {
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

        const anunciosList = document.getElementById("prof-anuncios-list");
        anunciosList.innerHTML = "";
        data.anuncios.forEach(a => {
            anunciosList.innerHTML += `
                <div class="list-item">
                    <div class="list-item-header">
                        <span>${a.titulo}</span>
                        <small>${a.fecha}</small>
                    </div>
                    <div class="list-item-desc">${a.contenido}</div>
                    <div class="list-item-footer">
                        <small>Publicado por: ${a.autor}</small>
                    </div>
                </div>
            `;
        });

        const entregasList = document.getElementById("prof-entregas-list");
        entregasList.innerHTML = "";
        let hasSubmissions = false;

        data.tareas.forEach(t => {
            Object.keys(t.entregas).forEach(alumnoId => {
                const ent = t.entregas[alumnoId];
                hasSubmissions = true;

                const btnGrade = ent.estado === "ENVIADO"
                    ? `<button class="btn btn-secondary mt-2" onclick="openGradeModal('${t.id}', '${alumnoId}', '${ent.archivoEnviado}')">Calificar y Revisar</button>`
                    : `<span class="badge badge-success mt-2">Calificado: ${ent.calificacion}</span>`;

                entregasList.innerHTML += `
                    <div class="list-item">
                        <div class="list-item-header">
                            <span>Tarea: ${t.titulo}</span>
                            <small class="badge badge-info">${ent.estado}</small>
                        </div>
                        <div class="list-item-desc">
                            Alumno ID: <strong>${alumnoId}</strong><br>
                            Archivo entregado: <a href="#">${ent.archivoEnviado}</a> (Entregado el ${ent.fechaEnvio})
                        </div>
                        ${btnGrade}
                    </div>
                `;
            });
        });

        if (!hasSubmissions) {
            entregasList.innerHTML = '<div class="list-item text-muted">No se registran entregas de alumnos aún.</div>';
        }
    } catch (error) {
        showToast("Error al cargar la información del docente", "danger");
    }
}

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

function switchProfTab(tabId) {
    document.querySelectorAll("#view-profesor .tab-pane").forEach(pane => pane.classList.add("hidden"));
    document.querySelectorAll("#view-profesor .tab-btn").forEach(btn => btn.classList.remove("active"));

    document.getElementById(tabId).classList.remove("hidden");
    event.currentTarget.classList.add("active");
}