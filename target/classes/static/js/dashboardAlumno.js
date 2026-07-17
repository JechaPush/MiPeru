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
    if (currentUser.rol !== "Alumno") {
        window.location.href = "/html/login.html";
        return;
    }

    // Datos del usuario en header y sidebar
    document.getElementById("user-display-name").innerText = currentUser.nombre;
    document.getElementById("user-role-label").innerText = `Rol: ${currentUser.rol}`;

    // Avatar con iniciales
    const initials = currentUser.nombre
        ? currentUser.nombre.trim().split(" ").map(w => w[0]).join("").substring(0, 2).toUpperCase()
        : "A";
    document.getElementById("al-avatar").innerText = initials;
    document.getElementById("al-perfil-avatar").innerText = initials;

    // ── Logout — un solo handler, dos botones reutilizan handleLogout() ──
    document.getElementById("btn-logout").addEventListener("click", handleLogout);

    // ── Sidebar: navegación modular ──
    document.getElementById("al-sidebar-nav").addEventListener("click", (e) => {
        const btn = e.target.closest(".al-nav-item");
        if (!btn) return;
        switchModule(btn.dataset.module, btn.dataset.title);
        closeSidebarOnMobile();
    });

    // ── Accesos rápidos de Inicio ──
    document.querySelectorAll(".al-quick-card").forEach(card => {
        card.addEventListener("click", () => {
            switchModule(card.dataset.module, card.dataset.title);
        });
    });

    // ── Toggle sidebar en móvil ──
    document.getElementById("al-toggle-sidebar").addEventListener("click", toggleSidebar);
    document.getElementById("al-overlay").addEventListener("click", closeSidebarOnMobile);

    // ── Cargar datos del dashboard ──
    loadAlumnoDashboard();
});

// ──────────────────────────────────────────────
//  NAVEGACIÓN MODULAR
// ──────────────────────────────────────────────

/**
 * Muestra únicamente el módulo indicado, actualiza el título
 * del header y resalta el ítem activo del sidebar.
 * @param {string} moduleName  - nombre del módulo (ej: "cursos")
 * @param {string} [moduleTitle] - título legible para el header
 */
function switchModule(moduleName, moduleTitle) {
    // 1. Ocultar todos los módulos
    document.querySelectorAll(".al-module").forEach(m => {
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
    document.querySelectorAll(".al-nav-item").forEach(btn => {
        btn.classList.remove("active");
        if (btn.dataset.module === moduleName) {
            btn.classList.add("active");
        }
    });

    // 4. Actualizar título del header
    if (moduleTitle) {
        document.getElementById("al-module-title").innerText = moduleTitle;
    }
}

// ──────────────────────────────────────────────
//  SIDEBAR MÓVIL
// ──────────────────────────────────────────────
function toggleSidebar() {
    const sidebar = document.getElementById("al-sidebar");
    const overlay = document.getElementById("al-overlay");
    const isOpen = sidebar.classList.toggle("open");
    overlay.classList.toggle("hidden", !isOpen);
}

function closeSidebarOnMobile() {
    const sidebar = document.getElementById("al-sidebar");
    const overlay = document.getElementById("al-overlay");
    if (window.innerWidth <= 768) {
        sidebar.classList.remove("open");
        overlay.classList.add("hidden");
    }
}

// ──────────────────────────────────────────────
//  LOGOUT
// ──────────────────────────────────────────────
function handleLogout() {
    localStorage.removeItem("ie_user");
    window.location.href = "/html/login.html";
}

// ──────────────────────────────────────────────
//  TOAST
// ──────────────────────────────────────────────
function showToast(message, type = "success") {
    const toast = document.getElementById("toast");
    toast.innerText = message;
    toast.className = `toast ${type === "danger" ? "bg-danger" : "bg-primary"}`;
    toast.classList.remove("hidden");

    setTimeout(() => {
        toast.classList.add("hidden");
    }, 3500);
}

// ──────────────────────────────────────────────
//  CARGA PRINCIPAL DEL DASHBOARD
// ──────────────────────────────────────────────
async function loadAlumnoDashboard() {
    try {
        const res = await fetch(`${API_BASE}/api/dashboard?userId=${currentUser.userId}&rol=Alumno`);
        const data = await res.json();
        if (!data.success) return;

        // ── Módulo Inicio: tarjetas de resumen ──
        document.getElementById("al-desempeno").innerText = data.dashboard.desempenoGeneral;
        document.getElementById("al-grado-seccion").innerText =
            `${data.dashboard.grado} "${data.dashboard.seccion}"`;

        // ── Módulo Inicio: contadores ──
        document.getElementById("al-total-cursos").innerText = data.cursos.length;
        const tareasPendientes = data.tareas.filter(t => !t.entrega).length;
        document.getElementById("al-total-tareas").innerText = tareasPendientes;

        // ── Módulo Inicio: resumen de pensiones (Fila 2) y previews (Fila 4) ──
        renderInicioPensionSummary(data.pagos);
        renderInicioPreviews(data.tareas, data.anuncios);

        // ── Módulo Perfil ──
        renderPerfil(data.dashboard);

        // ── Módulo Cursos y Notas ──
        renderCursos(data.cursos);

        // ── Módulo Tareas y Envíos ──
        renderTareas(data.tareas);

        // ── Módulo Anuncios ──
        renderAnuncios(data.anuncios);

        // ── Módulo Pensiones y Pagos ──
        renderPagos(data.pagos);

    } catch (error) {
        showToast("Error al cargar la información del alumno", "danger");
    }
}

// ──────────────────────────────────────────────
//  RENDERIZADO DE MÓDULOS
// ──────────────────────────────────────────────

function renderInicioPensionSummary(pagos) {
    const valueEl = document.getElementById("al-inicio-pension");
    const subEl = document.getElementById("al-inicio-pension-sub");
    const cardEl = document.getElementById("al-inicio-pension-card");

    if (!pagos || pagos.length === 0) {
        cardEl.classList.add("hidden");
        return;
    }

    cardEl.classList.remove("hidden");
    const pendientes = pagos.filter(p => !p.pagado);

    if (pendientes.length === 0) {
        valueEl.innerText = "Al día";
        valueEl.style.color = "#10B981"; // Green color
        subEl.innerText = "Sin pagos pendientes";
    } else {
        const totalPendiente = pendientes.reduce((sum, p) => sum + p.monto, 0);
        valueEl.innerText = `S/. ${totalPendiente.toFixed(2)}`;
        valueEl.style.color = "var(--primary-red)";
        subEl.innerText = `${pendientes.length} por pagar`;
    }
}

function renderInicioPreviews(tareas, anuncios) {
    const previewsContainer = document.getElementById("al-inicio-previews");
    const tareaPreview = document.getElementById("al-inicio-tarea-preview");
    const anuncioPreview = document.getElementById("al-inicio-anuncio-preview");

    let hasVisiblePreview = false;

    // 1. Buscar la primera tarea pendiente (sin entregar)
    const proximaTarea = tareas ? tareas.find(t => !t.entrega) : null;
    if (proximaTarea) {
        document.getElementById("al-inicio-proxima-tarea-titulo").innerText = proximaTarea.titulo;
        document.getElementById("al-inicio-proxima-tarea-fecha").innerText = `Entrega: ${proximaTarea.fechaEntrega}`;
        tareaPreview.classList.remove("hidden");
        hasVisiblePreview = true;
    } else {
        tareaPreview.classList.add("hidden");
    }

    // 2. Buscar el anuncio más reciente (el primero en el array)
    const ultimoAnuncio = anuncios && anuncios.length > 0 ? anuncios[0] : null;
    if (ultimoAnuncio) {
        document.getElementById("al-inicio-ultimo-anuncio-titulo").innerText = ultimoAnuncio.titulo;
        document.getElementById("al-inicio-ultimo-anuncio-autor").innerText = `Por: ${ultimoAnuncio.autor}`;
        anuncioPreview.classList.remove("hidden");
        hasVisiblePreview = true;
    } else {
        anuncioPreview.classList.add("hidden");
    }

    // Ocultar toda la fila de previews si no hay nada que mostrar
    if (hasVisiblePreview) {
        previewsContainer.classList.remove("hidden");
    } else {
        previewsContainer.classList.add("hidden");
    }
}

function renderPerfil(dashboard) {
    // Bloque Identidad
    document.getElementById("al-perfil-nombre").innerText = currentUser.nombre || "Alumno";
    document.getElementById("al-perfil-rol").innerText = currentUser.rol || "Alumno";
    document.getElementById("al-perfil-codigo").innerText = currentUser.userId ? `Código: ${currentUser.userId}` : "";

    // Bloque Académico
    const infoEl = document.getElementById("al-perfil-info");
    const items = [
        { label: "Grado", value: dashboard.grado || "—" },
        { label: "Sección", value: dashboard.seccion || "—" },
        { label: "Desempeño", value: dashboard.desempenoGeneral || "—" }
    ];

    infoEl.innerHTML = items.map(item => `
        <div class="al-perfil-row">
            <span class="al-perfil-label">${item.label}</span>
            <span class="al-perfil-value">${item.value}</span>
        </div>
    `).join("");
}

function renderCursos(cursos) {
    const tbody = document.getElementById("al-cursos-table-body");
    tbody.innerHTML = "";

    cursos.forEach(c => {
        let notesStr = c.notas
            .map(n => `<span class="badge ${getBadgeClass(n.letra)}">${n.periodo}: ${n.letra}</span>`)
            .join(" ");
        if (!notesStr) {
            notesStr = '<span class="text-muted">Sin notas calificadas</span>';
        }

        // Condición 3: No agregar punto rojo al nombre del curso, solo usar la franja en td:first-child (manejada por CSS)
        tbody.innerHTML += `
            <tr>
                <td><strong>${c.nombre}</strong></td>
                <td>${notesStr}</td>
            </tr>
        `;
    });
}

function renderTareas(tareas) {
    const list = document.getElementById("al-tareas-list");
    list.innerHTML = "";

    if (tareas.length === 0) {
        list.innerHTML = '<p class="text-muted" style="padding:0.5rem 0">No tienes tareas asignadas.</p>';
        return;
    }

    tareas.forEach(t => {
        // Determinar estado visual de la tarjeta
        let estadoClase = "estado-pendiente";
        if (t.entrega) {
            estadoClase = t.entrega.calificacion ? "estado-calificado" : "estado-entregado";
        }

        // Bloque de entrega actual
        let entregaHtml = "";
        if (t.entrega) {
            entregaHtml = `
                <div class="al-tarea-entrega alert alert-success mb-0">
                    <strong>Entregado:</strong> ${t.entrega.archivo} (${t.entrega.fecha})<br>
                    <strong>Estado:</strong> ${t.entrega.estado}<br>
                    <strong>Nota:</strong> ${t.entrega.calificacion || "Pendiente de revisar"}
                    ${t.entrega.comentarios ? `<br><strong>Comentarios:</strong> ${t.entrega.comentarios}` : ""}
                </div>
            `;
        }

        // Formulario de envío / reemplazo
        const formHtml = `
            <form onsubmit="submitHomework(event, '${t.id}')" class="al-tarea-form">
                <input type="text" placeholder="Nombre del archivo (ej: solucion.pdf)" required id="file-input-${t.id}">
                <button type="submit" class="btn btn-secondary">
                    ${t.entrega ? "Reemplazar" : "Enviar"}
                </button>
            </form>
        `;

        // Botón de examen en línea (si aplica)
        let examHtml = "";
        if (t.titulo.toLowerCase().includes("examen") || t.titulo.toLowerCase().includes("evaluacion")) {
            examHtml = `
                <button class="btn btn-secondary mt-2"
                    onclick="resolveExam('${t.id}', '${t.titulo}')">
                    Resolver Examen en Línea
                </button>
            `;
        }

        // Adjunto
        const adjuntoHtml = t.archivoAdjunto
            ? `<p class="al-tarea-adjunto">
                   <i class="bi bi-paperclip"></i>
                   <a href="#" onclick="downloadFile('${t.archivoAdjunto}')">
                       Descargar material: ${t.archivoAdjunto}
                   </a>
               </p>`
            : "";

        list.innerHTML += `
            <div class="al-tarea-card ${estadoClase}">
                <div class="al-tarea-top">
                    <span class="al-tarea-title">${t.titulo}</span>
                    <span class="badge badge-danger">Entrega: ${t.fechaEntrega}</span>
                </div>
                <p class="al-tarea-desc">${t.descripcion}</p>
                ${adjuntoHtml}
                ${entregaHtml}
                ${examHtml || formHtml}
            </div>
        `;
    });
}

function renderAnuncios(anuncios) {
    const list = document.getElementById("al-anuncios-list");
    list.innerHTML = "";

    if (anuncios.length === 0) {
        list.innerHTML = '<p class="text-muted" style="padding:0.5rem 0">No hay anuncios disponibles.</p>';
        return;
    }

    anuncios.forEach(a => {
        // Corrección: mostrar fecha solo si existe y no es undefined
        const fechaHtml = (a.fecha && a.fecha !== "undefined")
            ? `<span class="al-anuncio-fecha">${a.fecha}</span>`
            : "";

        list.innerHTML += `
            <div class="al-anuncio-card">
                <div class="al-anuncio-top">
                    <span class="al-anuncio-titulo">${a.titulo}</span>
                    ${fechaHtml}
                </div>
                <p class="al-anuncio-contenido">${a.contenido}</p>
                <p class="al-anuncio-autor">Publicado por: ${a.autor}</p>
            </div>
        `;
    });
}

function renderPagos(pagos) {
    const list = document.getElementById("al-pagos-list");
    list.innerHTML = "";

    if (pagos.length === 0) {
        list.innerHTML = '<p class="text-muted" style="padding:0.5rem 0">No hay registros de pensiones.</p>';
        return;
    }

    pagos.forEach(p => {
        // Corrección: ocultar vencimiento o fechaPago si son undefined / null
        const vencimientoHtml = (p.vencimiento && p.vencimiento !== "undefined")
            ? `<span>Vencimiento: ${p.vencimiento}</span><br>`
            : "";

        const fechaPagoHtml = (p.pagado && p.fechaPago && p.fechaPago !== "undefined")
            ? `<span>Pagado el: ${p.fechaPago}</span><br>`
            : "";

        const btnPagar = p.pagado
            ? ""
            : `<button class="btn btn-secondary" onclick="payInvoice('${p.id}')">
                   Pagar S/. ${p.monto.toFixed(2)}
               </button>`;

        list.innerHTML += `
            <div class="al-pago-card">
                <div class="al-pago-info-col">
                    <p class="al-pago-concepto">${p.concepto}</p>
                    <div class="al-pago-detalle">
                        <span>Monto: S/. ${p.monto.toFixed(2)}</span><br>
                        ${vencimientoHtml}
                        ${fechaPagoHtml}
                    </div>
                </div>
                <div class="al-pago-action-col">
                    <span class="badge ${p.pagado ? "badge-success" : "badge-danger"}">
                        ${p.pagado ? "Pagado" : "Pendiente"}
                    </span>
                    ${btnPagar}
                </div>
            </div>
        `;
    });
}

// ──────────────────────────────────────────────
//  ACCIONES (sin cambios respecto al original)
// ──────────────────────────────────────────────

async function submitHomework(e, tareaId) {
    e.preventDefault();
    const filenameInput = document.getElementById(`file-input-${tareaId}`);
    const filename = filenameInput.value.trim();
    if (!filename) return;

    try {
        const todayStr = new Date().toISOString().split("T")[0];
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
        const todayStr = new Date().toISOString().split("T")[0];
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

// ──────────────────────────────────────────────
//  AUXILIARES
// ──────────────────────────────────────────────
function downloadFile(name) {
    alert(`Descargando archivo escolar: ${name}\nAlmacenado localmente de forma protegida.`);
}

function getBadgeClass(letra) {
    switch (letra) {
        case "AD": return "badge-success";
        case "A":  return "badge-info";
        case "B":  return "badge-warning";
        case "C":  return "badge-danger";
        default:   return "";
    }
}