// Frontend Logic - I.E. "Mi Perú"

const API_BASE = ""; // Relative to server
let currentUser = null;

document.addEventListener("DOMContentLoaded", () => {
    // Check if user is already logged in (persistence)
    const storedUser = localStorage.getItem("ie_user");
    if (storedUser) {
        currentUser = JSON.parse(storedUser);
        showApp();
    }

    // Set default date in forms to today
    const today = new Date().toISOString().split('T')[0];
    const dateInputs = document.querySelectorAll('input[type="date"]');
    dateInputs.forEach(input => input.value = today);

    // Bind Forms
    document.getElementById("login-form").addEventListener("submit", handleLogin);
    document.getElementById("form-prof-notas").addEventListener("submit", handleProfRegisterNota);
    document.getElementById("form-prof-asistencia").addEventListener("submit", handleProfRegisterAsistencia);
    document.getElementById("form-prof-tareas").addEventListener("submit", handleProfCreateTarea);
    document.getElementById("form-prof-anuncio").addEventListener("submit", handleProfCreateAnuncio);
    document.getElementById("form-grade-submission").addEventListener("submit", handleProfGradeSubmission);
    document.getElementById("form-dir-matricula").addEventListener("submit", handleDirMatricula);
    document.getElementById("form-dir-reg-alumno").addEventListener("submit", handleDirRegAlumno);
    document.getElementById("form-dir-reg-docente").addEventListener("submit", handleDirRegDocente);
    
    document.getElementById("btn-logout").addEventListener("click", handleLogout);
});

// Toast system
function showToast(message, type = "success") {
    const toast = document.getElementById("toast");
    toast.innerText = message;
    toast.className = `toast ${type === 'danger' ? 'bg-danger' : 'bg-primary'}`;
    toast.classList.remove("hidden");
    
    setTimeout(() => {
        toast.classList.add("hidden");
    }, 3500);
}

// 1. Auth Flow
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
            currentUser = data;
            localStorage.setItem("ie_user", JSON.stringify(currentUser));
            showToast(`¡Bienvenido(a), ${currentUser.nombre}!`);
            showApp();
        } else {
            err.innerText = data.message || "Credenciales incorrectas.";
            err.classList.remove("hidden");
        }
    } catch (e) {
        err.innerText = "Error de conexión con el servidor.";
        err.classList.remove("hidden");
    } finally {
        btn.disabled = false;
    }
}

function handleLogout() {
    currentUser = null;
    localStorage.removeItem("ie_user");
    document.getElementById("app-container").classList.add("hidden");
    document.getElementById("login-container").classList.remove("hidden");
    document.getElementById("login-form").reset();
}

function showApp() {
    document.getElementById("login-container").classList.add("hidden");
    document.getElementById("app-container").classList.remove("hidden");
    
    // Set user headers
    document.getElementById("user-display-name").innerText = currentUser.nombre;
    document.getElementById("user-role-label").innerText = `Rol: ${currentUser.rol}`;

    // Hide all views first
    document.querySelectorAll(".role-view").forEach(v => v.classList.add("hidden"));

    // Render corresponding view
    if (currentUser.rol === "Director") {
        document.getElementById("view-director").classList.remove("hidden");
        loadDirectorDashboard();
    } else if (currentUser.rol === "Profesor") {
        document.getElementById("view-profesor").classList.remove("hidden");
        loadProfesorDashboard();
    } else if (currentUser.rol === "Alumno") {
        document.getElementById("view-alumno").classList.remove("hidden");
        loadAlumnoDashboard();
    }
}

// 2. Alumno Dashboard Logic (RF09 - RF15)
async function loadAlumnoDashboard() {
    try {
        const res = await fetch(`${API_BASE}/api/dashboard?userId=${currentUser.userId}&rol=Alumno`);
        const data = await res.json();
        if (!data.success) return;

        // Render Stats
        document.getElementById("al-desempeno").innerText = data.dashboard.desempenoGeneral;
        document.getElementById("al-grado-seccion").innerText = `${data.dashboard.grado} "${data.dashboard.seccion}"`;

        // Render Cursos y Notas (RF13)
        const coursesTbody = document.getElementById("al-cursos-table-body");
        coursesTbody.innerHTML = "";
        data.cursos.forEach(c => {
            let notesStr = c.notas.map(n => `<span class="badge ${getBadgeClass(n.letra)}">${n.periodo}: ${n.letra}</span>`).join(" ");
            if (!notesStr) notesStr = '<span class="text-muted">Sin notas calificadas</span>';
            
            coursesTbody.innerHTML += `
                <tr>
                    <td><strong>${c.nombre}</strong></td>
                    <td>${notesStr}</td>
                </tr>
            `;
        });

        // Render Tareas (RF10, RF11)
        const tareasList = document.getElementById("al-tareas-list");
        tareasList.innerHTML = "";
        if (data.tareas.length === 0) {
            tareasList.innerHTML = '<div class="list-item text-muted">No tienes tareas asignadas.</div>';
        }
        data.tareas.forEach(t => {
            let submissionHtml = "";
            if (t.entrega) {
                submissionHtml = `
                    <div class="alert alert-success mb-0 mt-2">
                        <strong>Entregado:</strong> ${t.entrega.archivo} (${t.entrega.fecha})<br>
                        <strong>Estado:</strong> ${t.entrega.estado}<br>
                        <strong>Nota:</strong> ${t.entrega.calificacion || "Pendiente de revisar"}<br>
                        ${t.entrega.comentarios ? `<strong>Comentarios:</strong> ${t.entrega.comentarios}` : ""}
                    </div>
                `;
            }

            // Allow submission/replace before deadline
            const submitFormHtml = `
                <form onsubmit="submitHomework(event, '${t.id}')" class="mt-2 form-group-row">
                    <input type="text" placeholder="Nombre del archivo (ej: solucion.pdf)" required id="file-input-${t.id}">
                    <button type="submit" class="btn btn-secondary btn-block">${t.entrega ? "Reemplazar Entrega" : "Enviar Tarea"}</button>
                </form>
            `;

            // Exams online RF12 (mocking online resolver)
            let examActionHtml = "";
            if (t.titulo.toLowerCase().includes("examen") || t.titulo.toLowerCase().includes("evaluacion")) {
                examActionHtml = `<button class="btn btn-secondary btn-block mt-2" onclick="resolveExam('${t.id}', '${t.titulo}')">Resolver Examen en Línea</button>`;
            }

            tareasList.innerHTML += `
                <div class="list-item">
                    <div class="list-item-header">
                        <span>${t.titulo}</span>
                        <span class="badge badge-danger">F. Entrega: ${t.fechaEntrega}</span>
                    </div>
                    <div class="list-item-desc">${t.descripcion}</div>
                    ${t.archivoAdjunto ? `<div class="mt-2"><a href="#" onclick="downloadFile('${t.archivoAdjunto}')">📎 Descargar Material: ${t.archivoAdjunto}</a></div>` : ""}
                    ${submissionHtml}
                    ${examActionHtml || submitFormHtml}
                </div>
            `;
        });

        // Render Anuncios (RF15)
        const anunciosList = document.getElementById("al-anuncios-list");
        anunciosList.innerHTML = "";
        data.anuncios.forEach(a => {
            anunciosList.innerHTML += `
                <div class="list-item">
                    <div class="list-item-header">
                        <span>${a.titulo}</span>
                        <small>${a.fecha}</small>
                    </div>
                    <div class="list-item-desc">${a.contenido}</div>
                    <div class="list-item-footer"><small>Publicado por: ${a.autor}</small></div>
                </div>
            `;
        });

        // Render Pagos (RF13, RF20)
        const pagosList = document.getElementById("al-pagos-list");
        pagosList.innerHTML = "";
        data.pagos.forEach(p => {
            const btnPagar = p.pagado ? "" : `<button class="btn btn-secondary mt-2 btn-block" onclick="payInvoice('${p.id}')">Pagar Pension S/. ${p.monto}</button>`;
            pagosList.innerHTML += `
                <div class="list-item">
                    <div class="list-item-header">
                        <span>${p.concepto}</span>
                        <span class="badge ${p.pagado ? 'badge-success' : 'badge-danger'}">${p.pagado ? 'Pagado' : 'Pendiente'}</span>
                    </div>
                    <div class="list-item-desc">Monto: S/. ${p.monto.toFixed(2)} - Vence: ${p.vencimiento}</div>
                    ${p.pagado ? `<div class="list-item-footer"><small>Pagado el: ${p.fechaPago}</small></div>` : ""}
                    ${btnPagar}
                </div>
            `;
        });

    } catch (e) {
        showToast("Error al cargar la información del alumno", "danger");
    }
}

// Student action: Submit homework
async function submitHomework(e, tareaId) {
    e.preventDefault();
    const filenameInput = document.getElementById(`file-input-${tareaId}`);
    const filename = filenameInput.value.trim();
    if (!filename) return;

    try {
        const todayStr = new Date().toISOString().split('T')[0];
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=enviarTarea&idTarea=${tareaId}&idAlumno=${currentUser.userId}&archivo=${encodeURIComponent(filename)}&fecha=${todayStr}`
        });
        const data = await res.json();
        if (data.success) {
            showToast("¡Tarea entregada con éxito!");
            loadAlumnoDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (e) {
        showToast("Error al realizar la entrega", "danger");
    }
}

// Student mock resolve exam (RF12)
function resolveExam(examId, title) {
    const gradeVal = prompt(`EXAMEN EN LÍNEA: ${title}\nResponda las preguntas de la evaluación virtual.\n\nAl terminar, digite su nota auto-evaluada de 0 a 20:`);
    if (gradeVal === null) return;
    const numericGrade = parseFloat(gradeVal);
    if (isNaN(numericGrade) || numericGrade < 0 || numericGrade > 20) {
        alert("Nota no válida.");
        return;
    }
    
    // Simulate resolving and sending
    const todayStr = new Date().toISOString().split('T')[0];
    fetch(`${API_BASE}/api/action`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `action=registrarNota&idAlumno=${currentUser.userId}&idCurso=CUR01&valor=${numericGrade}&periodo=Bimestre 2`
    })
    .then(r => r.json())
    .then(data => {
        if (data.success) {
            alert(`Examen enviado con éxito. Obtuviste una calificación en base a la escala correspondiente.`);
            loadAlumnoDashboard();
        } else {
            alert(data.message);
        }
    });
}

// Student action: pay bill
async function payInvoice(pagoId) {
    try {
        const todayStr = new Date().toISOString().split('T')[0];
        const res = await fetch(`${API_BASE}/api/action`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `action=pagar&idPago=${pagoId}&fecha=${todayStr}`
        });
        const data = await res.json();
        if (data.success) {
            showToast("¡Pago procesado de forma segura y acreditado!");
            loadAlumnoDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (e) {
        showToast("Error al procesar el pago", "danger");
    }
}

// Helper mock file download
function downloadFile(name) {
    alert(`Descargando archivo escolar: ${name}\nAlmacenado localmente de forma protegida.`);
}

// Helper badge mapper
function getBadgeClass(letra) {
    switch (letra) {
        case "AD": return "badge-success";
        case "A": return "badge-info";
        case "B": return "badge-warning";
        case "C": return "badge-danger";
        default: return "";
    }
}

// 3. Profesor Dashboard Logic (RF02 - RF08)
async function loadProfesorDashboard() {
    try {
        const res = await fetch(`${API_BASE}/api/dashboard?userId=${currentUser.userId}&rol=Profesor`);
        const data = await res.json();
        if (!data.success) return;

        // Render Cursos en Dashboard
        const cursosCont = document.getElementById("prof-cursos-container");
        cursosCont.innerHTML = "";
        
        // Load course options into selects
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

        // Event for loading students of selected course
        notaCursoSelect.onchange = () => loadCourseStudents(notaCursoSelect.value, "prof-nota-alumno");
        asisCursoSelect.onchange = () => loadCourseStudents(asisCursoSelect.value, "prof-asis-alumno");

        // Render Horario (RF02)
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

        // Render Anuncios (RF08)
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

        // Render Envíos Recibidos de alumnos (RF06)
        const entregasList = document.getElementById("prof-entregas-list");
        entregasList.innerHTML = "";
        let hasSubmissions = false;

        data.tareas.forEach(t => {
            Object.keys(t.entregas).forEach(alumnoId => {
                const ent = t.entregas[alumnoId];
                hasSubmissions = true;
                
                const btnGrade = ent.estado === "ENVIADO" ? 
                    `<button class="btn btn-secondary mt-2" onclick="openGradeModal('${t.id}', '${alumnoId}', '${ent.archivoEnviado}')">Calificar y Revisar</button>` : 
                    `<span class="badge badge-success mt-2">Calificado: ${ent.calificacion}</span>`;

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

    } catch (e) {
        showToast("Error al cargar la información del docente", "danger");
    }
}

// Fetch and load students in select dropdown
async function loadCourseStudents(cursoId, selectElementId) {
    const select = document.getElementById(selectElementId);
    select.innerHTML = "<option value=''>Cargando alumnos...</option>";
    if (!cursoId) {
        select.innerHTML = "<option value=''>Seleccione un curso primero</option>";
        return;
    }

    try {
        // Find students of this course from director dashboard or direct API
        const res = await fetch(`${API_BASE}/api/dashboard?userId=DIR01&rol=Director`); // we can get users here
        const data = await res.json();
        
        // filter students for selected course
        // To simplify, let's filter based on the grade/section of the selected course
        const coursesRes = await fetch(`${API_BASE}/api/dashboard?userId=${currentUser.userId}&rol=Profesor`);
        const coursesData = await coursesRes.json();
        const activeCourse = coursesData.cursos.find(c => c.id === cursoId);

        select.innerHTML = "<option value=''>Seleccione Alumno</option>";
        data.alumnos.forEach(a => {
            if (a.grado === activeCourse.grado && a.seccion === activeCourse.seccion) {
                select.innerHTML += `<option value="${a.id}">${a.nombre} (${a.id})</option>`;
            }
        });
    } catch (e) {
        select.innerHTML = "<option value=''>Error al cargar alumnos</option>";
    }
}

// Action: register grade (RF04)
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
    } catch (e) {
        showToast("Error al registrar la calificación.", "danger");
    }
}

// Action: register attendance (RF05)
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
            // restore today's date
            document.getElementById("prof-asis-fecha").value = new Date().toISOString().split('T')[0];
            loadProfesorDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (e) {
        showToast("Error al registrar la asistencia.", "danger");
    }
}

// Action: Create task (RF06)
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
    } catch (e) {
        showToast("Error al crear la tarea", "danger");
    }
}

// Action: Create announcement (RF08)
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
    } catch (e) {
        showToast("Error al publicar el anuncio", "danger");
    }
}

// Grading modal actions (RF06)
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
    } catch (e) {
        showToast("Error al calificar el envío", "danger");
    }
}

// Switch inner tabs in Docente
function switchProfTab(tabId) {
    document.querySelectorAll("#view-profesor .tab-pane").forEach(pane => pane.classList.add("hidden"));
    document.querySelectorAll("#view-profesor .tab-btn").forEach(btn => btn.classList.remove("active"));
    
    document.getElementById(tabId).classList.remove("hidden");
    event.currentTarget.classList.add("active");
}

// 4. Director Dashboard Logic (RF16 - RF21)
async function loadDirectorDashboard() {
    try {
        const res = await fetch(`${API_BASE}/api/dashboard?userId=${currentUser.userId}&rol=Director`);
        const data = await res.json();
        if (!data.success) return;

        // Stats cards (RF16)
        document.getElementById("dir-stat-alumnos").innerText = data.dashboard.totalAlumnos;
        document.getElementById("dir-stat-docentes").innerText = data.dashboard.totalDocentes;
        document.getElementById("dir-stat-cursos").innerText = data.dashboard.totalCursos;
        document.getElementById("dir-stat-recaudacion").innerText = `S/. ${data.dashboard.totalRecaudado.toFixed(2)}`;

        // Load dropdown selectors
        const matAlumnoSelect = document.getElementById("dir-mat-alumno");
        matAlumnoSelect.innerHTML = "<option value=''>Seleccione Alumno</option>";

        // Alumnos Table (RF18)
        const alumnosTbody = document.getElementById("dir-alumnos-table-body");
        alumnosTbody.innerHTML = "";
        data.alumnos.forEach(a => {
            matAlumnoSelect.innerHTML += `<option value="${a.id}">${a.nombre} (${a.id})</option>`;
            
            const gradoStr = a.grado ? `${a.grado} "${a.seccion}"` : '<span class="badge badge-warning">Sin Matricular</span>';
            alumnosTbody.innerHTML += `
                <tr>
                    <td>${a.id}</td>
                    <td><strong>${a.nombre}</strong></td>
                    <td>${gradoStr}</td>
                    <td><span class="badge badge-info">${a.desempeno}</span></td>
                    <td>
                        <button class="btn btn-danger" style="padding: 0.3rem 0.6rem; font-size: 0.75rem;" onclick="deleteUser('${a.id}')">Eliminar</button>
                    </td>
                </tr>
            `;
        });

        // Docentes List (RF17)
        const docentesList = document.getElementById("dir-docentes-list");
        docentesList.innerHTML = "";
        data.docentes.forEach(d => {
            // mock attendance check
            docentesList.innerHTML += `
                <div class="list-item">
                    <div class="list-item-header">
                        <span>${d.nombre} (${d.id})</span>
                        <button class="btn btn-danger" style="padding: 0.2rem 0.5rem; font-size: 0.7rem;" onclick="deleteUser('${d.id}')">Eliminar</button>
                    </div>
                    <div class="list-item-desc">Especialidad: ${d.especialidad}</div>
                    <div class="list-item-footer">
                        <span class="badge badge-success">Asistencia Docente: Conectado</span>
                    </div>
                </div>
            `;
        });

        // Pensiones Table (RF20)
        const pagosTbody = document.getElementById("dir-pagos-table-body");
        pagosTbody.innerHTML = "";
        data.pagos.forEach(p => {
            const actionBtn = p.pagado ? 
                `<span class="badge badge-success">Saldado</span>` : 
                `<button class="btn btn-secondary" style="padding: 0.3rem 0.6rem; font-size: 0.75rem;" onclick="realizePaymentDir('${p.id}')">Cobrar</button>`;

            pagosTbody.innerHTML += `
                <tr>
                    <td>${p.alumno}</td>
                    <td>S/. ${p.monto.toFixed(2)}</td>
                    <td>${p.concepto}</td>
                    <td><span class="badge ${p.pagado ? 'badge-success' : 'badge-danger'}">${p.pagado ? 'Pagado' : 'Pendiente'}</span></td>
                    <td>${actionBtn}</td>
                </tr>
            `;
        });

    } catch (e) {
        showToast("Error al cargar la información del director", "danger");
    }
}

// Director action: Register Enrollment (RF19 - validates deudas)
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
            showToast("¡Matrícula realizada con éxito! Grado y sección asignados.");
            document.getElementById("form-dir-matricula").reset();
            loadDirectorDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (e) {
        showToast("Error al procesar la matrícula", "danger");
    }
}

// Director action: Register Alumno (RF18)
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
            document.getElementById("form-dir-reg-alumno").reset();
            loadDirectorDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (e) {
        showToast("Error al registrar estudiante", "danger");
    }
}

// Director action: Register Docente (RF17)
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
    } catch (e) {
        showToast("Error al registrar docente", "danger");
    }
}

// Director action: cobro de pension
async function realizePaymentDir(pagoId) {
    try {
        const todayStr = new Date().toISOString().split('T')[0];
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
    } catch (e) {
        showToast("Error al registrar cobro", "danger");
    }
}

// Director / Teacher action: delete user
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
            if (currentUser.rol === "Director") loadDirectorDashboard();
        } else {
            showToast(data.message, "danger");
        }
    } catch (e) {
        showToast("Error al eliminar usuario", "danger");
    }
}

// Switch inner tabs in Director
function switchDirTab(tabId) {
    document.querySelectorAll("#view-director .tab-pane").forEach(pane => pane.classList.add("hidden"));
    document.querySelectorAll("#view-director .tab-btn").forEach(btn => btn.classList.remove("active"));
    
    document.getElementById(tabId).classList.remove("hidden");
    event.currentTarget.classList.add("active");
}
