const API_BASE = "";
let currentUser = null;

document.addEventListener("DOMContentLoaded", () => {
    const storedUser = localStorage.getItem("ie_user");
    if (!storedUser) {
        window.location.href = "/html/login.html";
        return;
    }

    currentUser = JSON.parse(storedUser);
    if (currentUser.rol !== "Alumno") {
        window.location.href = "/html/login.html";
        return;
    }

    document.getElementById("user-display-name").innerText = currentUser.nombre;
    document.getElementById("user-role-label").innerText = `Rol: ${currentUser.rol}`;
    document.getElementById("btn-logout").addEventListener("click", handleLogout);

    loadAlumnoDashboard();
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

async function loadAlumnoDashboard() {
    try {
        const res = await fetch(`${API_BASE}/api/dashboard?userId=${currentUser.userId}&rol=Alumno`);
        const data = await res.json();
        if (!data.success) return;

        document.getElementById("al-desempeno").innerText = data.dashboard.desempenoGeneral;
        document.getElementById("al-grado-seccion").innerText = `${data.dashboard.grado} "${data.dashboard.seccion}"`;

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

            const submitFormHtml = `
                <form onsubmit="submitHomework(event, '${t.id}')" class="mt-2 form-group-row">
                    <input type="text" placeholder="Nombre del archivo (ej: solucion.pdf)" required id="file-input-${t.id}">
                    <button type="submit" class="btn btn-secondary btn-block">${t.entrega ? "Reemplazar Entrega" : "Enviar Tarea"}</button>
                </form>
            `;

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
    } catch (error) {
        showToast("Error al cargar la información del alumno", "danger");
    }
}

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
    } catch (error) {
        showToast("Error al realizar la entrega", "danger");
    }
}

function resolveExam(examId, title) {
    const gradeVal = prompt(`EXAMEN EN LÍNEA: ${title}\nResponda las preguntas de la evaluación virtual.\n\nAl terminar, digite su nota auto-evaluada de 0 a 20:`);
    if (gradeVal === null) return;
    const numericGrade = parseFloat(gradeVal);
    if (isNaN(numericGrade) || numericGrade < 0 || numericGrade > 20) {
        alert("Nota no válida.");
        return;
    }

    fetch(`${API_BASE}/api/action`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `action=registrarNota&idAlumno=${currentUser.userId}&idCurso=CUR01&valor=${numericGrade}&periodo=Bimestre 2`
    })
    .then(r => r.json())
    .then(data => {
        if (data.success) {
            alert("Examen enviado con éxito. Obtuviste una calificación en base a la escala correspondiente.");
            loadAlumnoDashboard();
        } else {
            alert(data.message);
        }
    });
}

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
    } catch (error) {
        showToast("Error al procesar el pago", "danger");
    }
}

function downloadFile(name) {
    alert(`Descargando archivo escolar: ${name}\nAlmacenado localmente de forma protegida.`);
}

function getBadgeClass(letra) {
    switch (letra) {
        case "AD": return "badge-success";
        case "A": return "badge-info";
        case "B": return "badge-warning";
        case "C": return "badge-danger";
        default: return "";
    }
}