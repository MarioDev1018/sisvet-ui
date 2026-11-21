const Clinical = (() => {

    const RECEPTION_WEIGHT_PREFIX = "sisvet:reception-weight:";

    const STATE_BADGES = {
        EN_ESPERA: ["En espera", "wait"],
        ATENDIENDOSE: ["En atención", "active"],
        EN_BANO: ["En baño", "groom"],
        EN_PELUQUERIA: ["En peluquería", "groom"],
        LISTO_RECOJO: ["Listo para recojo", "ready"],
        ATENDIDO: ["Atendido", "done"]
    };

    const TYPE_LABELS = {
        MEDICAL: "Consulta médica",
        URGENCY: "Urgencia",
        VACCINATION: "Vacunación"
    };

    function el(id) {
        return document.getElementById(id);
    }

    function stateBadge(state) {
        const [label, variant] = STATE_BADGES[state] || [state || "-", "muted"];
        return Sisvet.badge(label, variant);
    }

    function typeBadge(attention) {
        const type = attention.type || "MEDICAL";
        const label = TYPE_LABELS[type] || type;
        if (type === "URGENCY") {
            const p = attention.priority ? ` · P${attention.priority}` : " · Sin triage";
            return Sisvet.badge(label + p, "urgent");
        }
        return Sisvet.badge(label, "muted");
    }

    function petDisplayLabel(pet) {
        const sp = pet.speciesLabel || pet.species || "";
        return `${pet.name} (${sp}) — ${pet.ownerName || "Propietario"} (${pet.ownerDocument || "S/D"})`;
    }

    function populateFilterableSelect(selectEl, items, labelFn, selectedValue = "", emptyOption = null) {
        if (!selectEl) return;
        const current = selectedValue ? String(selectedValue) : selectEl.value;
        selectEl.replaceChildren();
        if (emptyOption) {
            selectEl.add(new Option(emptyOption, ""));
        }
        for (const item of items) {
            selectEl.add(new Option(labelFn(item), String(item.id)));
        }
        if (current && Array.from(selectEl.options).some(o => o.value === current)) {
            selectEl.value = current;
        }
    }

    function setupSelectFilter(inputId, selectEl, allItems, matchTextFn, labelFn, emptyOption = null) {
        const filterInput = el(inputId);
        if (!filterInput || !selectEl) return;
        const apply = () => {
            const term = filterInput.value.trim().toLowerCase();
            const filtered = term
                ? allItems.filter(item => matchTextFn(item).toLowerCase().includes(term))
                : allItems;
            populateFilterableSelect(selectEl, filtered, labelFn, selectEl.value, emptyOption);
        };
        filterInput.addEventListener("input", apply);
        return apply;
    }

    function showWeightEvaluation(alertId, result) {
        const box = el(alertId);
        if (!box || !result) return;
        box.hidden = false;
        if (!result.configured) {
            box.className = "alert alert-warn";
        } else if (result.warning) {
            box.className = "alert alert-danger";
        } else {
            box.className = "alert alert-success";
        }
        box.textContent = result.message || "Peso evaluado.";
    }

    function saveReceptionWeight(petId, attentionId, weight) {
        if (!weight || Number(weight) <= 0) return;
        if (petId) sessionStorage.setItem(`${RECEPTION_WEIGHT_PREFIX}pet:${petId}`, String(weight));
        if (attentionId) sessionStorage.setItem(`${RECEPTION_WEIGHT_PREFIX}att:${attentionId}`, String(weight));
    }

    function readReceptionWeight(petId, attentionId) {
        return (attentionId && sessionStorage.getItem(`${RECEPTION_WEIGHT_PREFIX}att:${attentionId}`))
            || (petId && sessionStorage.getItem(`${RECEPTION_WEIGHT_PREFIX}pet:${petId}`))
            || "";
    }

    function isDoctor() {
        return Sisvet.hasAnyRole("VETERINARIO", "ESPECIALISTA");
    }

    function isReceptionOrAdmin() {
        return Sisvet.hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA");
    }

    function canManageConfig() {
        return Sisvet.hasAnyRole("ADMINISTRADOR", "VETERINARIO", "ESPECIALISTA");
    }

    function queryParams() {
        return new URLSearchParams(window.location.search);
    }

    function toLocalInputValue(date) {
        const pad = n => String(n).padStart(2, "0");
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }

    function dateKey(value) {
        if (!value) return "";
        const d = value instanceof Date ? value : new Date(value);
        if (Number.isNaN(d.getTime())) return String(value).slice(0, 10);
        const pad = n => String(n).padStart(2, "0");
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    }

    // ---------------------------------------------------------
    // 1. CENTRO Y CONFIGURACIÓN CLÍNICA (clinica.html)
    // ---------------------------------------------------------
    async function initHub(view) {
        const canConfig = canManageConfig();
        const notice = el("configRoleNotice");
        if (!canConfig && notice) {
            notice.hidden = false;
            notice.textContent = "Tu perfil actual puede consultar los parámetros, pero solo Administrador, Veterinario o Especialista pueden modificarlos.";
            el("savePolicyBtn").disabled = true;
            el("saveSpecialtyBtn").disabled = true;
        }

        async function loadHubData() {
            const [attentions, followUps, policies, professionals, speciesCatalog] = await Promise.all([
                Sisvet.api("/api/clinical/attentions/active"),
                Sisvet.api("/api/clinical/appointments/pending-follow-ups/details"),
                Sisvet.api("/api/clinical/weight-policies"),
                Sisvet.api("/api/clinical/appointments/professionals"),
                Sisvet.api("/api/clinical/weight-policies/species")
            ]);
            if (!view.isConnected) return;

            const policySpeciesSelect = el("policySpecies");
            if (policySpeciesSelect && Array.isArray(speciesCatalog)) {
                const previousSpecies = policySpeciesSelect.value;
                policySpeciesSelect.replaceChildren();
                for (const sp of speciesCatalog) {
                    policySpeciesSelect.add(new Option(sp.label, sp.code));
                }
                if (previousSpecies && Array.from(policySpeciesSelect.options).some(o => o.value === previousSpecies)) {
                    policySpeciesSelect.value = previousSpecies;
                }
            }

            const waiting = attentions.filter(a => a.state === "EN_ESPERA").length;
            const inProgress = attentions.filter(a => ["ATENDIENDOSE", "EN_BANO", "EN_PELUQUERIA"].includes(a.state)).length;
            const ready = attentions.filter(a => a.state === "LISTO_RECOJO").length;

            el("kpiWaiting").textContent = String(waiting);
            el("kpiInProgress").textContent = String(inProgress);
            el("kpiReady").textContent = String(ready);
            el("kpiFollowUps").textContent = String(followUps.length);

            Sisvet.renderTable("hubAttentionsTable", attentions.slice(0, 6), [
                { label: "Paciente", value: a => `${a.petName} (${a.speciesLabel})` },
                { label: "Propietario", value: a => a.ownerName },
                { label: "Tipo", value: a => typeBadge(a) },
                { label: "Estado", value: a => stateBadge(a.state) }
            ], {
                emptyText: "No hay pacientes activos en sala en este momento.",
                onRowClick: a => Sisvet.navigateTo(`/atenciones.html?attentionId=${a.id}`)
            });

            Sisvet.renderTable("hubFollowUpsTable", followUps.slice(0, 6), [
                { label: "Paciente", value: f => `${f.petName} (${f.speciesLabel})` },
                { label: "Propietario", value: f => f.ownerName },
                { label: "Fecha", value: f => Sisvet.formatDateTime(f.dueAt) },
                { label: "Motivo", value: f => f.reason }
            ], {
                emptyText: "No hay seguimientos pendientes.",
                onRowClick: f => Sisvet.navigateTo(`/pacientes.html?petId=${f.petId}`)
            });

            Sisvet.renderTable("policiesTable", policies, [
                { label: "Especie", value: p => `${p.speciesLabel} (${p.species})` },
                { label: "Mínimo orientativo", value: p => `${p.minKg} kg` },
                { label: "Máximo orientativo", value: p => `${p.maxKg} kg` }
            ], { emptyText: "Aún no hay rangos configurados. Configure al menos Perro y Gato." });

            populateFilterableSelect(
                el("specialtyProfessionalId"),
                professionals,
                p => `${p.fullName} (${p.roleName})`
            );

            const specRows = [];
            for (const prof of professionals) {
                if (!prof.specialties || prof.specialties.length === 0) {
                    specRows.push({ professionalId: prof.id, fullName: prof.fullName, roleName: prof.roleName, specialty: null });
                } else {
                    for (const sp of prof.specialties) {
                        specRows.push({ professionalId: prof.id, fullName: prof.fullName, roleName: prof.roleName, specialty: sp });
                    }
                }
            }

            Sisvet.renderTable("specialtiesTable", specRows, [
                { label: "Profesional", value: r => `${r.fullName} (${r.roleName})` },
                { label: "Especialidad", value: r => r.specialty ? Sisvet.badge(r.specialty, "active") : Sisvet.badge("Sin especialidad", "muted") },
                {
                    label: "Acción",
                    value: r => {
                        if (!r.specialty || !canConfig) return "-";
                        return Sisvet.actionButton("Quitar", async () => {
                            if (!await Sisvet.confirmAction(`¿Quitar la especialidad "${r.specialty}" a ${r.fullName}?`, "Quitar especialidad", true)) return;
                            try {
                                await Sisvet.api(`/api/clinical/appointments/professionals/${r.professionalId}/specialties?specialty=${encodeURIComponent(r.specialty)}`, {
                                    method: "DELETE"
                                });
                                await loadHubData();
                            } catch (error) {
                                Sisvet.showError("specialtyMessage", error);
                            }
                        }, "danger");
                    }
                }
            ], { emptyText: "No hay profesionales activos registrados en Usuarios." });
        }

        el("weightPolicyForm")?.addEventListener("submit", async event => {
            event.preventDefault();
            const f = event.target;
            try {
                await Sisvet.api("/api/clinical/weight-policies", {
                    method: "PUT",
                    body: JSON.stringify({
                        species: f.species.value,
                        minimum: Number(f.minimum.value),
                        maximum: Number(f.maximum.value)
                    })
                });
                Sisvet.message("policyMessage", "Criterio de peso actualizado.");
                await loadHubData();
            } catch (error) {
                Sisvet.showError("policyMessage", error);
            }
        });

        el("specialtyForm")?.addEventListener("submit", async event => {
            event.preventDefault();
            const f = event.target;
            const profId = Number(f.professionalId.value);
            const specialty = f.specialty.value.trim();
            if (!profId || !specialty) return;
            try {
                await Sisvet.api(`/api/clinical/appointments/professionals/${profId}/specialties`, {
                    method: "POST",
                    body: JSON.stringify({ specialty })
                });
                f.specialty.value = "";
                Sisvet.message("specialtyMessage", "Especialidad asignada.");
                await loadHubData();
            } catch (error) {
                Sisvet.showError("specialtyMessage", error);
            }
        });

        await loadHubData();
    }

    // ---------------------------------------------------------
    // 2. PACIENTES Y FICHA CLÍNICA (pacientes.html)
    // ---------------------------------------------------------
    async function initPatients(view) {
        const params = queryParams();
        let allCustomers = [];
        let allPets = [];
        let selectedPetId = Number(params.get("petId") || 0);

        const canRegisterPet = isReceptionOrAdmin();
        if (!canRegisterPet) {
            el("petFormRoleNotice").hidden = false;
            el("petFormRoleNotice").textContent = "El registro y edición de pacientes corresponde a Recepción o Administrador.";
            el("savePetBtn").disabled = true;
            el("newPetToggleBtn").textContent = "Ver formulario de registro";
        }

        function updateCustomSpeciesVisibility() {
            const isOther = el("petSpeciesSelect").value === "OTHER";
            el("customSpeciesWrap").hidden = !isOther;
            el("customSpeciesInput").required = isOther;
        }
        el("petSpeciesSelect").addEventListener("change", updateCustomSpeciesVisibility);

        function openPetFormForCreate(preselectOwnerId = "") {
            el("petForm").reset();
            el("petIdInput").value = "";
            el("petFormTitle").textContent = "Registrar nueva mascota";
            updateCustomSpeciesVisibility();
            if (preselectOwnerId) {
                el("petOwnerId").value = String(preselectOwnerId);
            }
            el("petFormCard").hidden = false;
            el("petFormCard").scrollIntoView({ behavior: "smooth", block: "nearest" });
        }

        function openPetFormForEdit(pet) {
            el("petForm").reset();
            el("petIdInput").value = pet.id;
            el("petFormTitle").textContent = `Editar ficha de ${pet.name}`;
            el("ownerFilterInput").value = "";
            populateFilterableSelect(el("petOwnerId"), allCustomers, c => `${c.fullName} — ${c.documentType} ${c.documentNumber}`, pet.ownerId);
            el("petNameInput").value = pet.name || "";
            if (pet.species === "CANINE" || pet.species === "FELINE") {
                el("petSpeciesSelect").value = pet.species;
            } else {
                el("petSpeciesSelect").value = "OTHER";
                el("customSpeciesInput").value = pet.speciesLabel || pet.species || "";
            }
            updateCustomSpeciesVisibility();
            el("petBreedInput").value = pet.breed || "";
            el("petSexSelect").value = pet.sex || "";
            el("petBirthDateInput").value = pet.birthDate || "";
            el("petAgeMonthsInput").value = pet.estimatedAgeMonths ?? "";
            el("petFormCard").hidden = false;
            el("petFormCard").scrollIntoView({ behavior: "smooth", block: "nearest" });
        }

        el("newPetToggleBtn").addEventListener("click", () => {
            if (el("petFormCard").hidden) openPetFormForCreate();
            else el("petFormCard").hidden = true;
        });
        el("closePetFormBtn").addEventListener("click", () => { el("petFormCard").hidden = true; });
        el("resetPetFormBtn").addEventListener("click", () => openPetFormForCreate());

        el("toggleQuickOwnerBtn").addEventListener("click", () => {
            el("quickOwnerCard").hidden = !el("quickOwnerCard").hidden;
        });
        el("cancelQuickOwnerBtn").addEventListener("click", () => {
            el("quickOwnerCard").hidden = true;
        });

        async function loadCustomers(selectOwnerId = null) {
            allCustomers = await Sisvet.api("/api/customers");
            if (!view.isConnected) return;
            populateFilterableSelect(
                el("petOwnerId"),
                allCustomers,
                c => `${c.fullName} — ${c.documentType} ${c.documentNumber}`,
                selectOwnerId
            );
        }

        setupSelectFilter(
            "ownerFilterInput",
            el("petOwnerId"),
            [],
            () => "",
            () => ""
        );
        // Usa filtro reactivo sobre allCustomers actualizado
        el("ownerFilterInput").addEventListener("input", () => {
            const term = el("ownerFilterInput").value.trim().toLowerCase();
            const filtered = term
                ? allCustomers.filter(c => `${c.fullName} ${c.documentNumber}`.toLowerCase().includes(term))
                : allCustomers;
            populateFilterableSelect(el("petOwnerId"), filtered, c => `${c.fullName} — ${c.documentType} ${c.documentNumber}`);
        });

        async function searchPets(criteria = "") {
            allPets = await Sisvet.api(`/api/clinical/patients/summaries?search=${encodeURIComponent(criteria)}`);
            if (!view.isConnected) return;

            Sisvet.renderTable("patientsTable", allPets, [
                {
                    label: "Paciente",
                    value: p => {
                        const wrap = document.createElement("div");
                        const strong = document.createElement("strong");
                        strong.textContent = p.name;
                        const sub = document.createElement("div");
                        sub.className = "muted";
                        sub.style.fontSize = "12px";
                        sub.textContent = `${p.speciesLabel}${p.breed ? " · " + p.breed : ""} · ID #${p.id}`;
                        wrap.append(strong, sub);
                        return wrap;
                    }
                },
                {
                    label: "Propietario",
                    value: p => `${p.ownerName} (${p.ownerDocument || "S/D"})`
                },
                {
                    label: "Último peso",
                    value: p => p.lastWeightKg != null ? `${p.lastWeightKg} kg` : "Sin pesaje"
                },
                {
                    label: "Estado actual",
                    value: p => p.activeAttentionState ? stateBadge(p.activeAttentionState) : Sisvet.badge("Sin atención activa", "muted")
                },
                {
                    label: "Acciones",
                    value: p => {
                        const btns = [
                            Sisvet.actionButton("Ver ficha", () => openDossier(p.id), "")
                        ];
                        if (canRegisterPet) {
                            btns.push(Sisvet.actionButton("Editar", () => openPetFormForEdit(p), "outline"));
                            if (!p.activeAttentionId) {
                                btns.push(Sisvet.actionButton("Llegada", () => Sisvet.navigateTo(`/atenciones.html?petId=${p.id}`), "secondary"));
                            }
                        }
                        if (p.activeAttentionId) {
                            btns.push(Sisvet.actionButton("Ver en sala", () => Sisvet.navigateTo(`/atenciones.html?attentionId=${p.activeAttentionId}`), "outline"));
                        }
                        return btns;
                    }
                }
            ], {
                emptyText: "No se encontraron mascotas con ese criterio.",
                onRowClick: p => openDossier(p.id)
            });

            Sisvet.message("patientListMessage", `${allPets.length} paciente(s) encontrados.`);
        }

        function setDossierTab(tabName) {
            for (const btn of el("dossierTabs").querySelectorAll(".tab-btn")) {
                btn.classList.toggle("active", btn.dataset.tab === tabName);
            }
            el("dossierPanelWeights").hidden = tabName !== "weights";
            el("dossierPanelConsultations").hidden = tabName !== "consultations";
            el("dossierPanelTreatments").hidden = tabName !== "treatments";
            el("dossierPanelVaccinations").hidden = tabName !== "vaccinations";
            el("dossierPanelAgenda").hidden = tabName !== "agenda";
        }

        for (const btn of el("dossierTabs").querySelectorAll(".tab-btn")) {
            btn.addEventListener("click", () => setDossierTab(btn.dataset.tab));
        }

        async function openDossier(petId) {
            selectedPetId = Number(petId);
            el("dossierRoleNote").hidden = true;

            try {
                let summary;
                let historyView = null;
                if (Sisvet.hasAnyRole("ADMINISTRADOR", "VETERINARIO", "ESPECIALISTA")) {
                    historyView = await Sisvet.api(`/api/clinical/patients/${selectedPetId}/history-view`);
                    summary = historyView.pet;
                } else {
                    summary = await Sisvet.api(`/api/clinical/patients/${selectedPetId}/summary`);
                    el("dossierRoleNote").hidden = false;
                    el("dossierRoleNote").textContent = "Modo recepción: puedes consultar datos de identificación, último peso y estado. La historia clínica detallada es visible para Veterinario/Especialista/Administrador.";
                }
                if (!view.isConnected) return;

                el("petDossierSection").hidden = false;
                el("dossierPetName").textContent = `Ficha clínica de ${summary.name} (#${summary.id})`;

                const badges = el("dossierBadges");
                badges.replaceChildren();
                badges.appendChild(Sisvet.badge(summary.speciesLabel || summary.species, "muted"));
                badges.appendChild(summary.activeAttentionState ? stateBadge(summary.activeAttentionState) : Sisvet.badge("Sin atención en curso", "done"));

                el("dossierOwner").textContent = `${summary.ownerName} (${summary.ownerDocument || "S/D"})`;
                el("dossierContact").textContent = summary.ownerPhone || "Sin teléfono registrado";
                const ageText = summary.birthDate
                    ? `Nac. ${Sisvet.formatDate(summary.birthDate)}`
                    : (summary.estimatedAgeMonths != null ? `${summary.estimatedAgeMonths} meses aprox.` : "Edad no indicada");
                el("dossierBio").textContent = `${summary.speciesLabel} · ${summary.breed || "Mestizo"} · ${summary.sex || "-"} · ${ageText}`;
                el("dossierWeight").textContent = summary.lastWeightKg != null
                    ? `${summary.lastWeightKg} kg (${Sisvet.formatDate(summary.lastWeightAt)})`
                    : "Sin pesajes registrados";

                el("dossierEditBtn").disabled = !canRegisterPet;
                el("dossierEditBtn").onclick = () => openPetFormForEdit(summary);
                el("dossierArrivalLink").href = `/atenciones.html?petId=${summary.id}`;
                el("dossierCareLink").href = summary.activeAttentionId
                    ? `/consultas.html?attentionId=${summary.activeAttentionId}`
                    : `/consultas.html?petId=${summary.id}`;
                el("dossierAgendaLink").href = `/citas.html?petId=${summary.id}`;

                if (historyView) {
                    Sisvet.renderTable("dossierWeightTable", historyView.weightHistory, [
                        { label: "Fecha", value: w => Sisvet.formatDateTime(w.recordedAt) },
                        { label: "Peso medido", value: w => `${w.weightKg} kg` },
                        { label: "Motivo de consulta", value: w => w.reason }
                    ], { emptyText: "Sin mediciones de peso registradas." });

                    Sisvet.renderTable("dossierConsultationsTable", historyView.consultations, [
                        { label: "Fecha", value: c => Sisvet.formatDateTime(c.createdAt) },
                        { label: "Motivo", value: c => c.reason },
                        { label: "Peso", value: c => `${c.weight} kg` },
                        { label: "Síntomas", value: c => c.symptoms || "-" },
                        { label: "Estado", value: c => Sisvet.badge(c.status === "COMPLETED" ? "Cerrada" : "Abierta", c.status === "COMPLETED" ? "done" : "active") }
                    ], { emptyText: "Sin consultas registradas." });

                    Sisvet.renderTable("dossierDiagnosesTable", historyView.diagnoses, [
                        { label: "Fecha", value: d => Sisvet.formatDateTime(d.createdAt) },
                        { label: "Diagnóstico", value: d => d.description },
                        { label: "Indicaciones", value: d => d.indications }
                    ], { emptyText: "Sin diagnósticos registrados." });

                    Sisvet.renderTable("dossierTreatmentsTable", historyView.treatments, [
                        { label: "Fecha", value: t => Sisvet.formatDateTime(t.prescribedAt) },
                        { label: "Tratamiento", value: t => t.description },
                        { label: "Indicaciones", value: t => t.instructions }
                    ], { emptyText: "Sin tratamientos registrados." });

                    Sisvet.renderTable("dossierProceduresTable", historyView.procedures, [
                        { label: "Fecha", value: p => Sisvet.formatDateTime(p.performedAt) },
                        { label: "Procedimiento", value: p => p.name },
                        { label: "Observaciones", value: p => p.observations || "-" }
                    ], { emptyText: "Sin procedimientos registrados." });

                    Sisvet.renderTable("dossierReferralsTable", historyView.referrals, [
                        { label: "Fecha", value: r => Sisvet.formatDateTime(r.createdAt) },
                        { label: "Motivo", value: r => r.reason },
                        { label: "Destino / Especialidad", value: r => r.destination || "-" }
                    ], { emptyText: "Sin derivaciones registradas." });

                    Sisvet.renderTable("dossierVaccinationsTable", historyView.vaccinations, [
                        { label: "Aplicación", value: v => Sisvet.formatDateTime(v.appliedAt) },
                        { label: "Vacuna", value: v => v.vaccine },
                        { label: "Dosis", value: v => v.dose },
                        { label: "Próxima dosis", value: v => Sisvet.formatDate(v.nextDoseDate) }
                    ], { emptyText: "Sin vacunaciones registradas." });

                    Sisvet.renderTable("dossierAppointmentsTable", historyView.appointments, [
                        { label: "Fecha y hora", value: a => `${Sisvet.formatDate(a.startsAt)} ${Sisvet.formatTime(a.startsAt)}` },
                        { label: "Profesional", value: a => a.professionalName },
                        { label: "Motivo", value: a => a.reason },
                        { label: "Estado", value: a => Sisvet.badge(a.status === "CONFIRMED" ? "Confirmada" : "Cancelada", a.status === "CONFIRMED" ? "active" : "done") }
                    ], { emptyText: "Sin citas registradas." });

                    Sisvet.renderTable("dossierFollowUpsTable", historyView.followUps, [
                        { label: "Programado", value: f => Sisvet.formatDateTime(f.dueAt) },
                        { label: "Motivo", value: f => f.reason },
                        { label: "Estado", value: f => Sisvet.badge(f.status, f.status === "COMPLETED" ? "done" : "warn") },
                        {
                            label: "Acción",
                            value: f => f.status !== "COMPLETED"
                                ? Sisvet.actionButton("Completar", async () => {
                                    await Sisvet.api(`/api/clinical/appointments/pending-follow-ups/${f.id}/complete`, { method: "PUT" });
                                    await openDossier(selectedPetId);
                                }, "small")
                                : "-"
                        }
                    ], { emptyText: "Sin seguimientos registrados." });
                } else {
                    for (const tId of ["dossierWeightTable", "dossierConsultationsTable", "dossierDiagnosesTable", "dossierTreatmentsTable", "dossierProceduresTable", "dossierReferralsTable", "dossierVaccinationsTable", "dossierAppointmentsTable", "dossierFollowUpsTable"]) {
                        Sisvet.renderTable(tId, [], [{ label: "Detalle", value: () => "" }], { emptyText: "Solo visible para personal médico / administrador." });
                    }
                }

                el("petDossierSection").scrollIntoView({ behavior: "smooth", block: "nearest" });
            } catch (error) {
                Sisvet.showError("patientListMessage", error);
            }
        }

        el("petSearchForm").addEventListener("submit", async event => {
            event.preventDefault();
            try {
                await searchPets(el("petSearchInput").value.trim());
            } catch (error) {
                Sisvet.showError("patientListMessage", error);
            }
        });

        el("showAllPetsBtn").addEventListener("click", async () => {
            el("petSearchInput").value = "";
            await searchPets("");
        });

        const quickOwnerFormEl = el("quickOwnerForm");
        Sisvet.bindDocumentRules(quickOwnerFormEl?.documentType, quickOwnerFormEl?.documentNumber);

        el("quickOwnerForm").addEventListener("submit", async event => {
            event.preventDefault();
            const f = event.target;
            try {
                const created = await Sisvet.api("/api/customers", {
                    method: "POST",
                    body: JSON.stringify({
                        documentType: f.documentType.value,
                        documentNumber: f.documentNumber.value.trim(),
                        fullName: f.fullName.value.trim(),
                        phone: f.phone.value.trim(),
                        email: f.email.value.trim(),
                        address: ""
                    })
                });
                f.reset();
                el("quickOwnerCard").hidden = true;
                await loadCustomers(created.id);
                el("petOwnerId").value = String(created.id);
                Sisvet.message("petMessage", `Propietario "${created.fullName}" registrado y seleccionado.`);
            } catch (error) {
                Sisvet.showError("quickOwnerMessage", error);
            }
        });

        el("petForm").addEventListener("submit", async event => {
            event.preventDefault();
            const petId = el("petIdInput").value;
            const speciesVal = el("petSpeciesSelect").value === "OTHER"
                ? el("customSpeciesInput").value.trim()
                : el("petSpeciesSelect").value;
            const ageRaw = el("petAgeMonthsInput").value;

            const payload = {
                ownerId: Number(el("petOwnerId").value),
                name: el("petNameInput").value.trim(),
                species: speciesVal,
                breed: el("petBreedInput").value.trim() || null,
                sex: el("petSexSelect").value || null,
                birthDate: el("petBirthDateInput").value || null,
                estimatedAgeMonths: ageRaw === "" ? null : Number(ageRaw)
            };

            try {
                const saved = await Sisvet.api(petId ? `/api/clinical/patients/${petId}` : "/api/clinical/patients", {
                    method: petId ? "PUT" : "POST",
                    body: JSON.stringify(payload)
                });
                Sisvet.message("petMessage", petId ? `Ficha de ${saved.name} actualizada.` : `Mascota ${saved.name} registrada.`);
                await searchPets(el("petSearchInput").value.trim());
                await openDossier(saved.id);
                el("petFormCard").hidden = true;
            } catch (error) {
                Sisvet.showError("petMessage", error);
            }
        });

        await loadCustomers(params.get("ownerId"));
        await searchPets("");
        if (params.get("ownerId") && canRegisterPet) {
            openPetFormForCreate(params.get("ownerId"));
        }
        if (selectedPetId > 0) {
            await openDossier(selectedPetId);
        }
    }

    // ---------------------------------------------------------
    // 3. RECEPCIÓN Y TABLERO DE ESTADOS (atenciones.html)
    // ---------------------------------------------------------
    async function initAttentions(view) {
        const params = queryParams();
        let allPets = [];
        let activeAttentions = [];
        let currentStateFilter = "ALL";
        const canRegisterArrival = isReceptionOrAdmin();
        const canRegisterUrgency = Sisvet.hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA", "VETERINARIO", "ESPECIALISTA");

        if (!canRegisterArrival && el("arrivalRoleNotice")) {
            el("arrivalRoleNotice").hidden = false;
            el("arrivalRoleNotice").textContent = "Como Veterinario/Especialista puedes registrar urgencias inmediatas o iniciar atenciones del tablero. Las llegadas regulares las registra Recepción/Administrador.";
        }

        function syncArrivalTypeUI() {
            const t = el("arrivalType").value;
            el("urgencyDescField").hidden = t !== "URGENCY";
            el("urgencyDescription").required = t === "URGENCY";
            el("groomingField").hidden = t === "VACCINATION" || t === "URGENCY";
            if (t !== "MEDICAL") el("requiresGrooming").checked = false;

            const allowedForChoice = t === "URGENCY" ? canRegisterUrgency : canRegisterArrival;
            el("submitArrivalBtn").disabled = !allowedForChoice;
        }
        el("arrivalType").addEventListener("change", syncArrivalTypeUI);
        syncArrivalTypeUI();

        el("checkReceptionWeightBtn").addEventListener("click", async () => {
            const petId = Number(el("arrivalPetId").value);
            const weight = Number(el("receptionWeight").value);
            if (!petId || !weight || weight <= 0) {
                Sisvet.message("arrivalMessage", "Selecciona mascota e ingresa un peso positivo para evaluar.", true);
                return;
            }
            try {
                const evaluation = await Sisvet.api(`/api/clinical/consultations/weight-evaluation?petId=${petId}&weight=${weight}`);
                if (view.isConnected) showWeightEvaluation("receptionWeightAlert", evaluation);
            } catch (error) {
                Sisvet.showError("arrivalMessage", error);
            }
        });

        async function loadPetsForArrival() {
            allPets = await Sisvet.api("/api/clinical/patients/summaries");
            if (!view.isConnected) return;
            populateFilterableSelect(el("arrivalPetId"), allPets, petDisplayLabel, params.get("petId"));
        }

        el("arrivalPetFilter").addEventListener("input", () => {
            const term = el("arrivalPetFilter").value.trim().toLowerCase();
            const filtered = term
                ? allPets.filter(p => `${p.name} ${p.ownerName} ${p.ownerDocument || ""}`.toLowerCase().includes(term))
                : allPets;
            populateFilterableSelect(el("arrivalPetId"), filtered, petDisplayLabel);
        });

        function filteredAttentions() {
            if (currentStateFilter === "ALL") return activeAttentions;
            if (currentStateFilter === "GROOMING") {
                return activeAttentions.filter(a => a.state === "EN_BANO" || a.state === "EN_PELUQUERIA");
            }
            return activeAttentions.filter(a => a.state === currentStateFilter);
        }

        function buildRowActions(a) {
            const actions = [];
            const vet = isDoctor();
            const rec = isReceptionOrAdmin();

            if (a.state === "EN_ESPERA") {
                if (a.type === "URGENCY" && !a.priority && vet) {
                    actions.push(Sisvet.actionButton("Triage (1-5)", async () => {
                        const raw = await Sisvet.promptValue("Ingresa nivel de prioridad clínica (1 = máxima urgencia, 5 = menor):", `Triage para ${a.petName}`, "number", "1");
                        if (!raw) return;
                        try {
                            await Sisvet.api(`/api/clinical/attentions/${a.id}/priority`, {
                                method: "PUT",
                                body: JSON.stringify({ priority: Number(raw) })
                            });
                            Sisvet.message("boardMessage", `Prioridad P${raw} asignada a ${a.petName}.`);
                            await loadBoard();
                        } catch (error) {
                            Sisvet.showError("boardMessage", error);
                        }
                    }, "danger"));
                }
                if (vet) {
                    actions.push(Sisvet.actionButton("Iniciar y atender", async () => {
                        try {
                            await Sisvet.api(`/api/clinical/attentions/${a.id}/start`, { method: "PUT" });
                            await Sisvet.navigateTo(`/consultas.html?attentionId=${a.id}`);
                        } catch (error) {
                            Sisvet.showError("boardMessage", error);
                        }
                    }));
                } else {
                    const b = Sisvet.actionButton("Espera médico", () => {}, "outline");
                    b.disabled = true;
                    b.title = "Solo Veterinario o Especialista puede iniciar atención clínica";
                    actions.push(b);
                }
            } else if (a.state === "ATENDIENDOSE") {
                if (vet) {
                    actions.push(Sisvet.actionButton("Abrir atención", () => Sisvet.navigateTo(`/consultas.html?attentionId=${a.id}`)));
                    if (a.clinicalClosed || a.vaccinationCompleted) {
                        actions.push(Sisvet.actionButton(a.requiresGrooming ? "Pasar a baño" : "Listo recojo", async () => {
                            try {
                                await Sisvet.api(`/api/clinical/attentions/${a.id}/advance`, { method: "PUT" });
                                await loadBoard();
                            } catch (error) {
                                Sisvet.showError("boardMessage", error);
                            }
                        }, "secondary"));
                    }
                }
            } else if (a.state === "EN_BANO" || a.state === "EN_PELUQUERIA") {
                const nextLabel = a.state === "EN_BANO" ? "Pasar a peluquería" : "Listo para recojo";
                actions.push(Sisvet.actionButton(nextLabel, async () => {
                    try {
                        await Sisvet.api(`/api/clinical/attentions/${a.id}/advance`, { method: "PUT" });
                        await loadBoard();
                    } catch (error) {
                        Sisvet.showError("boardMessage", error);
                    }
                }, "secondary"));
            } else if (a.state === "LISTO_RECOJO") {
                if (rec) {
                    actions.push(Sisvet.actionButton("Registrar retiro", async () => {
                        el("pickupBillingShortcut").hidden = true;
                        try {
                            await Sisvet.api(`/api/clinical/attentions/${a.id}/pickup`, { method: "PUT" });
                            Sisvet.message("boardMessage", `Retiro confirmado para ${a.petName}.`);
                            await loadBoard();
                        } catch (error) {
                            Sisvet.showError("boardMessage", error);
                            if (String(error.message || "").toLowerCase().includes("cobro")) {
                                el("pickupBillingShortcut").hidden = false;
                                if (a.consultationId) {
                                    el("pickupBillingText").textContent = `${a.petName} tiene cobros clínicos pendientes en la consulta #${a.consultationId}.`;
                                    el("pickupBillingLink").href = `/cobros-clinicos.html?consultationId=${a.consultationId}`;
                                } else {
                                    el("pickupBillingText").textContent = `${a.petName} tiene cobros pendientes en la atención #${a.id}.`;
                                    el("pickupBillingLink").href = `/cobros-clinicos.html?attentionId=${a.id}`;
                                }
                            }
                        }
                    }));
                }
                if (a.consultationId) {
                    actions.push(Sisvet.actionButton("Ver cobros", () => Sisvet.navigateTo(`/cobros-clinicos.html?consultationId=${a.consultationId}`), "outline"));
                } else if (a.type === "VACCINATION") {
                    actions.push(Sisvet.actionButton("Ver cobros", () => Sisvet.navigateTo(`/cobros-clinicos.html?attentionId=${a.id}`), "outline"));
                }
            }

            actions.push(Sisvet.actionButton("Ficha", () => Sisvet.navigateTo(`/pacientes.html?petId=${a.petId}`), "outline"));
            return actions;
        }

        async function loadBoard() {
            activeAttentions = await Sisvet.api("/api/clinical/attentions/active");
            if (!view.isConnected) return;

            const rows = filteredAttentions();
            const highlightId = Number(params.get("attentionId") || 0);

            Sisvet.renderTable("activeAttentionsTable", rows, [
                {
                    label: "Paciente",
                    value: a => `${a.petName} (${a.speciesLabel})`
                },
                { label: "Propietario", value: a => `${a.ownerName} (${a.ownerDocument || "S/D"})` },
                { label: "Tipo", value: a => typeBadge(a) },
                {
                    label: "Estado",
                    value: a => {
                        const s = stateBadge(a.state);
                        if (a.requiresGrooming && a.state === "EN_ESPERA") {
                            return [s, Sisvet.badge("+ Baño/Peluq.", "groom")];
                        }
                        return s;
                    }
                },
                { label: "Llegada", value: a => Sisvet.formatTime(a.arrivedAt) },
                { label: "Último peso", value: a => a.lastWeightKg != null ? `${a.lastWeightKg} kg` : "-" },
                { label: "Acciones", value: buildRowActions }
            ], {
                emptyText: "No hay atenciones activas en esta vista.",
                rowClass: a => highlightId && a.id === highlightId ? "row-highlight" : ""
            });
        }

        for (const btn of el("attentionStateFilters").querySelectorAll(".tab-btn")) {
            btn.addEventListener("click", () => {
                currentStateFilter = btn.dataset.state;
                for (const b of el("attentionStateFilters").querySelectorAll(".tab-btn")) {
                    b.classList.toggle("active", b === btn);
                }
                loadBoard();
            });
        }

        el("refreshAttentionsBtn").addEventListener("click", loadBoard);

        el("arrivalForm").addEventListener("submit", async event => {
            event.preventDefault();
            const petId = Number(el("arrivalPetId").value);
            const type = el("arrivalType").value;
            const grooming = el("requiresGrooming").checked;
            const recWeight = el("receptionWeight").value;

            try {
                let created = null;
                if (type === "URGENCY") {
                    const desc = el("urgencyDescription").value.trim();
                    await Sisvet.api("/api/clinical/attentions/urgencies", {
                        method: "POST",
                        body: JSON.stringify({ petId, description: desc })
                    });
                    Sisvet.message("arrivalMessage", "Urgencia registrada en sala de espera.");
                } else {
                    created = await Sisvet.api("/api/clinical/attentions/arrivals", {
                        method: "POST",
                        body: JSON.stringify({ petId, type, requiresGrooming: grooming })
                    });
                    Sisvet.message("arrivalMessage", `Llegada registrada para atención #${created.id}.`);
                }

                saveReceptionWeight(petId, created?.id, recWeight);
                el("urgencyDescription").value = "";
                el("receptionWeight").value = "";
                el("receptionWeightAlert").hidden = true;
                await loadBoard();
            } catch (error) {
                Sisvet.showError("arrivalMessage", error);
            }
        });

        await loadPetsForArrival();
        await loadBoard();
    }

    // ---------------------------------------------------------
    // 4. ATENCIÓN CLÍNICA UNIFICADA (consultas.html)
    // ---------------------------------------------------------
    async function initClinicalCare(view) {
        const params = queryParams();
        let activeAttentions = [];
        let selectedAttention = null;
        let currentConsultation = null;
        let allMedications = [];
        let allVaccines = [];
        const vet = isDoctor();

        if (!vet) {
            el("careRoleBanner").hidden = false;
            el("careRoleBanner").textContent = "Las acciones médicas (abrir consulta, diagnóstico, prescripciones, vacunación y cierre) requieren perfil de Veterinario o Especialista.";
        }

        function setCareTab(tabName) {
            for (const btn of el("careTabs").querySelectorAll(".tab-btn")) {
                btn.classList.toggle("active", btn.dataset.tab === tabName);
            }
            el("careTabDiagnosis").hidden = tabName !== "diagnosis";
            el("careTabTreatment").hidden = tabName !== "treatment";
            el("careTabProcedure").hidden = tabName !== "procedure";
            el("careTabVaccination").hidden = tabName !== "vaccination";
            el("careTabFollowup").hidden = tabName !== "followup";
        }

        for (const btn of el("careTabs").querySelectorAll(".tab-btn")) {
            btn.addEventListener("click", () => setCareTab(btn.dataset.tab));
        }

        const requestedTab = params.get("tab");
        if (requestedTab && ["diagnosis", "treatment", "procedure", "vaccination", "followup"].includes(requestedTab)) {
            setCareTab(requestedTab);
        }

        async function loadMedications(search = "") {
            allMedications = await Sisvet.api(`/api/clinical/treatments/medications?search=${encodeURIComponent(search)}`);
            if (!view.isConnected) return;
            Sisvet.renderTable("medicationsPickerTable", allMedications, [
                { label: "Medicamento", value: m => m.tradeName || m.commercialName || "-" },
                { label: "Detalle / Stock", value: m => m.activeIngredient || "-" },
                { label: "Presentación / Conc.", value: m => m.concentration || m.presentation || "-" },
                {
                    label: "Acción",
                    value: m => Sisvet.actionButton("Seleccionar", () => {
                        const medName = m.tradeName || m.commercialName || "Medicamento";
                        el("selectedProductId").value = m.productId;
                        const detail = [m.activeIngredient, m.concentration].filter(Boolean).join(" · ");
                        el("selectedMedicationDisplay").value = detail
                            ? `${medName} (${detail})`
                            : medName;
                        const descInput = el("treatmentForm")?.querySelector("[name='description']");
                        if (descInput && !descInput.value.trim()) {
                            descInput.focus();
                        }
                    })
                }
            ], { emptyText: "No se encontraron medicamentos disponibles en inventario." });
        }

        let medicationSearchTimer = null;
        el("searchMedicationsBtn").addEventListener("click", () => loadMedications(el("medicationFilterInput").value.trim()));
        el("medicationFilterInput").addEventListener("input", () => {
            clearTimeout(medicationSearchTimer);
            const term = el("medicationFilterInput").value.trim();
            medicationSearchTimer = setTimeout(() => {
                if (view.isConnected) loadMedications(term);
            }, 180);
        });
        el("medicationFilterInput").addEventListener("keydown", event => {
            if (event.key === "Enter") {
                event.preventDefault();
                clearTimeout(medicationSearchTimer);
                loadMedications(el("medicationFilterInput").value.trim());
            }
        });
        el("clearMedicationBtn").addEventListener("click", () => {
            el("selectedProductId").value = "";
            el("selectedMedicationDisplay").value = "";
        });

        function formatVaccineOption(v) {
            return `${v.name} [${v.categoryName}] — Stock: ${v.availableStock} ${v.baseUnit} · ${Sisvet.money(v.unitPricePerBase)}/${v.baseUnit}`;
        }

        function bindVaccinePicker(prefix) {
            const filterEl = el(`${prefix}VaccineFilter`);
            const selectEl = el(`${prefix}VaccineProductId`);
            const nameEl = el(`${prefix}VaccineName`);
            const doseEl = el(`${prefix}VaccineDose`);
            const qtyField = el(`${prefix}VaccineQtyField`);
            const qtyEl = el(`${prefix}VaccineQty`);
            const feeEl = el(`${prefix}VaccineFee`);
            const estEl = el(`${prefix}VaccineEstimate`);
            const refreshBtn = el(`refresh${prefix.charAt(0).toUpperCase() + prefix.slice(1)}VaccinesBtn`);

            const renderSelect = (keepSelected = true) => {
                if (!selectEl) return;
                const prev = keepSelected ? selectEl.value : "";
                const term = (filterEl?.value || "").trim().toLowerCase();
                const filtered = term
                    ? allVaccines.filter(v => `${v.name} ${v.code} ${v.categoryName}`.toLowerCase().includes(term))
                    : allVaccines;
                selectEl.replaceChildren();
                selectEl.add(new Option("Sin insumo de inventario (vacuna externa / solo aplicación)", ""));
                for (const v of filtered) {
                    selectEl.add(new Option(formatVaccineOption(v), String(v.productId)));
                }
                if (prev && Array.from(selectEl.options).some(o => o.value === prev)) {
                    selectEl.value = prev;
                }
                updateEstimate();
            };

            const updateEstimate = () => {
                if (!selectEl || !estEl) return;
                const prodId = Number(selectEl.value || 0);
                const selected = allVaccines.find(v => v.productId === prodId) || null;
                if (qtyField) qtyField.hidden = !selected;
                const fee = Number(feeEl?.value || 0);

                if (selected) {
                    const qty = Math.max(1, Number(qtyEl?.value || 1));
                    if (nameEl) {
                        nameEl.value = selected.name;
                        nameEl.readOnly = true;
                    }
                    if (doseEl) {
                        doseEl.value = `${qty} ${selected.baseUnit}`;
                        doseEl.readOnly = true;
                    }
                    const supply = Number(selected.unitPricePerBase || 0) * qty;
                    estEl.textContent = `Descontará ${qty} ${selected.baseUnit} de inventario (${Sisvet.money(supply)}) + aplicación (${Sisvet.money(fee)}) = Total a cobrar: ${Sisvet.money(supply + fee)}.`;
                } else {
                    if (nameEl) nameEl.readOnly = false;
                    if (doseEl) doseEl.readOnly = false;
                    estEl.textContent = `Sin descuento de inventario (vacuna vacía/externa). Cargo por aplicación a cobrar: ${Sisvet.money(fee)}.`;
                }
            };

            selectEl?.addEventListener("change", () => {
                const prodId = Number(selectEl.value || 0);
                const selected = allVaccines.find(v => v.productId === prodId) || null;
                if (selected) {
                    if (feeEl && (feeEl.value === "35.00" || !feeEl.value)) feeEl.value = "15.00";
                } else {
                    if (nameEl?.readOnly) nameEl.value = "";
                    if (doseEl?.readOnly) doseEl.value = "";
                    if (feeEl && feeEl.value === "15.00") feeEl.value = "35.00";
                }
                updateEstimate();
            });

            selectEl?.closest("form")?.addEventListener("reset", () => setTimeout(updateEstimate, 0));
            filterEl?.addEventListener("input", () => renderSelect(true));
            qtyEl?.addEventListener("input", updateEstimate);
            feeEl?.addEventListener("input", updateEstimate);
            refreshBtn?.addEventListener("click", () => loadVaccines());

            return { renderSelect, updateEstimate };
        }

        const inConsultVaxPicker = bindVaccinePicker("inConsult");
        const standaloneVaxPicker = bindVaccinePicker("standalone");

        async function loadVaccines() {
            allVaccines = await Sisvet.api("/api/clinical/treatments/vaccines");
            if (!view.isConnected) return;
            inConsultVaxPicker.renderSelect(true);
            standaloneVaxPicker.renderSelect(true);
        }

        el("evaluateConsultWeightBtn").addEventListener("click", async () => {
            if (!selectedAttention) return;
            const weight = Number(el("consultWeight").value);
            if (!weight || weight <= 0) {
                Sisvet.message("consultOpenMessage", "Indica un peso positivo para evaluar.", true);
                return;
            }
            try {
                const res = await Sisvet.api(`/api/clinical/consultations/weight-evaluation?petId=${selectedAttention.petId}&weight=${weight}`);
                if (view.isConnected) showWeightEvaluation("consultWeightAlert", res);
            } catch (error) {
                Sisvet.showError("consultOpenMessage", error);
            }
        });

        async function renderConsultationDetail(consultationId) {
            currentConsultation = await Sisvet.api(`/api/clinical/consultations/${consultationId}`);
            if (!view.isConnected) return;

            el("openConsultationCard").hidden = true;
            el("activeConsultationCard").hidden = false;
            el("activeConsultHeading").textContent = `Consulta #${currentConsultation.id} · ${currentConsultation.petName}`;
            el("activeConsultMeta").textContent = `Motivo: ${currentConsultation.reason} · Peso: ${currentConsultation.weight} kg · Veterinario: ${currentConsultation.veterinarianName} · Estado: ${currentConsultation.status === "COMPLETED" ? "Finalizada" : "Abierta"}`;
            el("goToClinicalBillingLink").href = `/cobros-clinicos.html?consultationId=${currentConsultation.id}`;
            el("inConsultAgendaLink").href = `/citas.html?petId=${currentConsultation.petId}&consultationId=${currentConsultation.id}`;
            el("completeConsultationBtn").disabled = !vet || currentConsultation.status === "COMPLETED";

            Sisvet.renderTable("consultationDiagnosesTable", currentConsultation.diagnoses, [
                { label: "Fecha", value: d => Sisvet.formatDateTime(d.createdAt) },
                { label: "Diagnóstico", value: d => d.description },
                { label: "Indicaciones", value: d => d.indications }
            ], { emptyText: "Aún no se registra diagnóstico (obligatorio antes de finalizar la consulta)." });

            Sisvet.renderTable("consultationTreatmentsTable", currentConsultation.treatments, [
                { label: "Fecha", value: t => Sisvet.formatDateTime(t.prescribedAt) },
                { label: "Tratamiento", value: t => t.description },
                { label: "Indicaciones", value: t => t.instructions }
            ], { emptyText: "Sin tratamientos prescritos." });

            Sisvet.renderTable("consultationProceduresTable", currentConsultation.procedures, [
                { label: "Fecha", value: p => Sisvet.formatDateTime(p.performedAt) },
                { label: "Procedimiento", value: p => p.name },
                { label: "Observaciones", value: p => p.observations || "-" }
            ], { emptyText: "Sin procedimientos registrados." });

            Sisvet.renderTable("consultationReferralsTable", currentConsultation.referrals, [
                { label: "Fecha", value: r => Sisvet.formatDateTime(r.createdAt) },
                { label: "Motivo", value: r => r.reason },
                { label: "Destino", value: r => r.destination || "-" }
            ], { emptyText: "Sin derivaciones registradas." });

            Sisvet.renderTable("consultationVaccinationsTable", currentConsultation.vaccinations, [
                { label: "Fecha", value: v => Sisvet.formatDateTime(v.appliedAt) },
                { label: "Vacuna", value: v => v.vaccine },
                { label: "Dosis", value: v => v.dose },
                { label: "Próxima dosis", value: v => Sisvet.formatDate(v.nextDoseDate) }
            ], { emptyText: "Sin vacunas aplicadas en esta consulta." });
        }

        async function renderSelectedAttention() {
            const id = Number(el("careAttentionSelect").value);
            selectedAttention = activeAttentions.find(a => a.id === id) || null;
            currentConsultation = null;

            el("careContextBox").hidden = !selectedAttention;
            el("careStartBar").hidden = true;
            el("medicalCarePanel").hidden = true;
            el("vaccinationCarePanel").hidden = true;
            el("carePatientDossierLink").hidden = !selectedAttention;

            if (!selectedAttention) return;

            el("carePatientDossierLink").href = `/pacientes.html?petId=${selectedAttention.petId}`;
            el("carePetInfo").textContent = `${selectedAttention.petName} (${selectedAttention.speciesLabel})`;
            el("careOwnerInfo").textContent = `${selectedAttention.ownerName} (${selectedAttention.ownerDocument || "S/D"})`;
            el("careStateInfo").textContent = `${TYPE_LABELS[selectedAttention.type] || selectedAttention.type} · ${(STATE_BADGES[selectedAttention.state] || [selectedAttention.state])[0]}`;
            el("careWeightInfo").textContent = selectedAttention.lastWeightKg != null ? `${selectedAttention.lastWeightKg} kg` : "Sin registros";

            if (selectedAttention.state === "EN_ESPERA") {
                el("careStartBar").hidden = false;
                el("startSelectedAttentionBtn").disabled = !vet;
                return;
            }

            if (selectedAttention.state !== "ATENDIENDOSE") {
                Sisvet.message("careSelectMessage", `Esta atención se encuentra en estado ${(STATE_BADGES[selectedAttention.state] || [selectedAttention.state])[0]}. Gestiónala desde Recepción.`);
                return;
            }

            Sisvet.message("careSelectMessage", "");

            if (selectedAttention.type === "VACCINATION") {
                el("vaccinationCarePanel").hidden = false;
                el("advanceVaccinationBtn").disabled = !selectedAttention.vaccinationCompleted || !vet;
                el("standaloneBillingLink").href = `/cobros-clinicos.html?attentionId=${selectedAttention.id}`;
                return;
            }

            el("medicalCarePanel").hidden = false;
            if (!selectedAttention.consultationId) {
                el("openConsultationCard").hidden = false;
                el("activeConsultationCard").hidden = true;
                el("openConsultationBtn").disabled = !vet;
                const recWeight = readReceptionWeight(selectedAttention.petId, selectedAttention.id);
                if (recWeight) {
                    el("consultWeight").value = recWeight;
                    el("evaluateConsultWeightBtn").click();
                } else {
                    el("consultWeight").value = "";
                    el("consultWeightAlert").hidden = true;
                }
            } else {
                await renderConsultationDetail(selectedAttention.consultationId);
            }
        }

        async function loadAttentionsPicker(preferredAttentionId = null) {
            activeAttentions = await Sisvet.api("/api/clinical/attentions/active");
            if (!view.isConnected) return;

            const targetId = preferredAttentionId
                || Number(params.get("attentionId") || 0)
                || (params.get("petId") ? activeAttentions.find(a => a.petId === Number(params.get("petId")))?.id : null);

            populateFilterableSelect(
                el("careAttentionSelect"),
                activeAttentions,
                a => `${a.petName} (${a.ownerName}) — ${(TYPE_LABELS[a.type] || a.type)} · ${(STATE_BADGES[a.state] || [a.state])[0]}`,
                targetId || "",
                activeAttentions.length === 0 ? "No hay atenciones activas en clínica" : null
            );

            await renderSelectedAttention();
        }

        el("careAttentionSelect").addEventListener("change", renderSelectedAttention);
        el("refreshCareBtn").addEventListener("click", () => loadAttentionsPicker(el("careAttentionSelect").value));

        el("startSelectedAttentionBtn").addEventListener("click", async () => {
            if (!selectedAttention) return;
            try {
                await Sisvet.api(`/api/clinical/attentions/${selectedAttention.id}/start`, { method: "PUT" });
                Sisvet.message("careSelectMessage", "Atención iniciada.");
                await loadAttentionsPicker(selectedAttention.id);
            } catch (error) {
                Sisvet.showError("careSelectMessage", error);
            }
        });

        el("consultationOpenForm").addEventListener("submit", async event => {
            event.preventDefault();
            if (!selectedAttention) return;
            try {
                const created = await Sisvet.api("/api/clinical/consultations", {
                    method: "POST",
                    body: JSON.stringify({
                        attentionId: selectedAttention.id,
                        reason: el("consultReason").value.trim(),
                        weight: Number(el("consultWeight").value),
                        symptoms: el("consultSymptoms").value.trim(),
                        observations: el("consultObservations").value.trim()
                    })
                });
                Sisvet.message("consultOpenMessage", `Consulta #${created.id} abierta correctamente.`);
                await loadAttentionsPicker(selectedAttention.id);
            } catch (error) {
                Sisvet.showError("consultOpenMessage", error);
            }
        });

        el("diagnosisForm").addEventListener("submit", async event => {
            event.preventDefault();
            if (!currentConsultation) return;
            const f = event.target;
            try {
                await Sisvet.api("/api/clinical/consultations/diagnoses", {
                    method: "POST",
                    body: JSON.stringify({
                        consultationId: currentConsultation.id,
                        description: f.description.value.trim(),
                        indications: f.indications.value.trim()
                    })
                });
                f.reset();
                Sisvet.message("diagnosisMessage", "Diagnóstico registrado.");
                await renderConsultationDetail(currentConsultation.id);
            } catch (error) {
                Sisvet.showError("diagnosisMessage", error);
            }
        });

        el("treatmentForm").addEventListener("submit", async event => {
            event.preventDefault();
            if (!currentConsultation) return;
            const f = event.target;
            const prodRaw = el("selectedProductId").value;
            try {
                await Sisvet.api("/api/clinical/treatments", {
                    method: "POST",
                    body: JSON.stringify({
                        consultationId: currentConsultation.id,
                        description: f.description.value.trim(),
                        instructions: f.instructions.value.trim(),
                        productId: prodRaw ? Number(prodRaw) : null
                    })
                });
                f.reset();
                el("selectedProductId").value = "";
                el("selectedMedicationDisplay").value = "";
                Sisvet.message("treatmentMessage", "Tratamiento prescrito.");
                await renderConsultationDetail(currentConsultation.id);
            } catch (error) {
                Sisvet.showError("treatmentMessage", error);
            }
        });

        el("procedureForm").addEventListener("submit", async event => {
            event.preventDefault();
            if (!currentConsultation) return;
            const f = event.target;
            try {
                await Sisvet.api("/api/clinical/treatments/procedures", {
                    method: "POST",
                    body: JSON.stringify({
                        consultationId: currentConsultation.id,
                        name: f.name.value.trim(),
                        observations: f.observations.value.trim()
                    })
                });
                f.reset();
                Sisvet.message("procedureMessage", "Procedimiento registrado (cargo generado automáticamente).");
                await renderConsultationDetail(currentConsultation.id);
            } catch (error) {
                Sisvet.showError("procedureMessage", error);
            }
        });

        el("referralForm").addEventListener("submit", async event => {
            event.preventDefault();
            if (!currentConsultation) return;
            const f = event.target;
            try {
                await Sisvet.api("/api/clinical/treatments/referrals", {
                    method: "POST",
                    body: JSON.stringify({
                        consultationId: currentConsultation.id,
                        reason: f.reason.value.trim(),
                        destination: f.destination.value.trim() || null
                    })
                });
                f.reset();
                Sisvet.message("referralMessage", "Derivación registrada.");
                await renderConsultationDetail(currentConsultation.id);
            } catch (error) {
                Sisvet.showError("referralMessage", error);
            }
        });

        el("extraChargeForm").addEventListener("submit", async event => {
            event.preventDefault();
            if (!currentConsultation) return;
            const f = event.target;
            try {
                await Sisvet.api(`/api/clinical-billing/consultations/${currentConsultation.id}/charges`, {
                    method: "POST",
                    body: JSON.stringify({
                        description: f.description.value.trim(),
                        amount: Number(f.amount.value)
                    })
                });
                f.reset();
                Sisvet.message("extraChargeMessage", "Cargo pendiente agregado a caja clínica.");
            } catch (error) {
                Sisvet.showError("extraChargeMessage", error);
            }
        });

        el("inConsultVaccinationForm").addEventListener("submit", async event => {
            event.preventDefault();
            if (!currentConsultation) return;
            const f = event.target;
            const prodId = f.productId.value ? Number(f.productId.value) : null;
            const qtyUsed = prodId ? Number(f.quantityUsed.value) : null;
            const appFee = f.applicationFee.value === "" ? 0 : Number(f.applicationFee.value);

            try {
                await Sisvet.api("/api/clinical/treatments/vaccinations", {
                    method: "POST",
                    body: JSON.stringify({
                        petId: currentConsultation.petId,
                        consultationId: currentConsultation.id,
                        vaccine: f.vaccine.value.trim(),
                        dose: f.dose.value.trim(),
                        nextDoseDate: f.nextDoseDate.value || null,
                        productId: prodId,
                        quantityUsed: qtyUsed,
                        applicationFee: appFee
                    })
                });
                f.reset();
                el("inConsultVaccineFee").value = "15.00";
                inConsultVaxPicker.updateEstimate();
                Sisvet.message("inConsultVaxMessage", prodId
                    ? "Vacuna registrada, stock descontado y cargo generado para caja."
                    : "Vacuna registrada y cargo de aplicación generado para caja.");
                await Promise.all([renderConsultationDetail(currentConsultation.id), loadVaccines()]);
            } catch (error) {
                Sisvet.showError("inConsultVaxMessage", error);
            }
        });

        el("inConsultFollowUpForm").addEventListener("submit", async event => {
            event.preventDefault();
            if (!currentConsultation) return;
            const f = event.target;
            try {
                await Sisvet.api("/api/clinical/appointments/pending-follow-ups", {
                    method: "POST",
                    body: JSON.stringify({
                        petId: currentConsultation.petId,
                        consultationId: currentConsultation.id,
                        dueAt: f.dueAt.value,
                        reason: f.reason.value.trim(),
                        appointmentId: null
                    })
                });
                f.reset();
                Sisvet.message("inConsultFollowUpMessage", "Seguimiento postconsulta programado.");
            } catch (error) {
                Sisvet.showError("inConsultFollowUpMessage", error);
            }
        });

        el("completeConsultationBtn").addEventListener("click", async () => {
            if (!currentConsultation) return;
            if (!await Sisvet.confirmAction(`¿Finalizar la consulta #${currentConsultation.id}?`, "Finalizar consulta")) return;
            try {
                await Sisvet.api(`/api/clinical/consultations/${currentConsultation.id}/complete`, { method: "PUT" });
                Sisvet.message("consultLifecycleMessage", "Consulta finalizada. Ya puedes avanzar la atención a baño o listo para recojo.");
                await loadAttentionsPicker(selectedAttention.id);
            } catch (error) {
                Sisvet.showError("consultLifecycleMessage", error);
            }
        });

        el("advanceAttentionBtn").addEventListener("click", async () => {
            if (!selectedAttention) return;
            try {
                await Sisvet.api(`/api/clinical/attentions/${selectedAttention.id}/advance`, { method: "PUT" });
                Sisvet.message("consultLifecycleMessage", "Estado de atención avanzado.");
                await loadAttentionsPicker(selectedAttention.id);
            } catch (error) {
                Sisvet.showError("consultLifecycleMessage", error);
            }
        });

        el("standaloneVaccinationForm").addEventListener("submit", async event => {
            event.preventDefault();
            if (!selectedAttention) return;
            const f = event.target;
            const nextDate = f.nextDoseDate.value || null;
            const vaccine = f.vaccine.value.trim();
            const prodId = f.productId.value ? Number(f.productId.value) : null;
            const qtyUsed = prodId ? Number(f.quantityUsed.value) : null;
            const appFee = f.applicationFee.value === "" ? 0 : Number(f.applicationFee.value);

            try {
                await Sisvet.api("/api/clinical/treatments/vaccinations", {
                    method: "POST",
                    body: JSON.stringify({
                        petId: selectedAttention.petId,
                        consultationId: null,
                        vaccine,
                        dose: f.dose.value.trim(),
                        nextDoseDate: nextDate,
                        productId: prodId,
                        quantityUsed: qtyUsed,
                        applicationFee: appFee
                    })
                });

                if (nextDate && el("autoCreateVaxFollowUp").checked) {
                    await Sisvet.api("/api/clinical/appointments/pending-follow-ups", {
                        method: "POST",
                        body: JSON.stringify({
                            petId: selectedAttention.petId,
                            consultationId: null,
                            dueAt: `${nextDate}T09:00`,
                            reason: `Refuerzo / próxima dosis de vacuna: ${vaccine}`,
                            appointmentId: null
                        })
                    });
                }

                f.reset();
                el("standaloneVaccineFee").value = "15.00";
                standaloneVaxPicker.updateEstimate();
                Sisvet.message("standaloneVaxMessage", "Vacuna aplicada, stock/cargos actualizados. Ya puedes pasar a listo para recojo y cobrar en caja.");
                await Promise.all([loadAttentionsPicker(selectedAttention.id), loadVaccines()]);
            } catch (error) {
                Sisvet.showError("standaloneVaxMessage", error);
            }
        });

        el("advanceVaccinationBtn").addEventListener("click", async () => {
            if (!selectedAttention) return;
            try {
                await Sisvet.api(`/api/clinical/attentions/${selectedAttention.id}/advance`, { method: "PUT" });
                Sisvet.message("standaloneVaxMessage", "Atención de vacunación pasada a Listo para recojo.");
                await loadAttentionsPicker(selectedAttention.id);
            } catch (error) {
                Sisvet.showError("standaloneVaxMessage", error);
            }
        });

        await Promise.all([loadMedications(""), loadVaccines(), loadAttentionsPicker()]);
    }

    // ---------------------------------------------------------
    // 5. AGENDA GRÁFICA DE CITAS (citas.html)
    // ---------------------------------------------------------
    async function initAppointments(view) {
        const params = queryParams();
        let professionals = [];
        let allPets = [];
        let monthAppointments = [];

        const today = new Date();
        let currentYear = today.getFullYear();
        let currentMonth = today.getMonth(); // 0-11
        let selectedDateKey = dateKey(today);

        async function loadCatalogs() {
            const [profs, pets] = await Promise.all([
                Sisvet.api("/api/clinical/appointments/professionals"),
                Sisvet.api("/api/clinical/patients/summaries")
            ]);
            if (!view.isConnected) return;

            professionals = profs;
            allPets = pets;

            populateFilterableSelect(
                el("agendaProfessionalId"),
                professionals,
                p => `${p.fullName} · ${p.roleName}${p.specialties?.length ? " (" + p.specialties.join(", ") + ")" : ""}`
            );

            populateFilterableSelect(
                el("appointmentPetId"),
                allPets,
                petDisplayLabel,
                params.get("petId")
            );
            syncProfessionalSpecialties();
            await loadPetConsultations(params.get("consultationId"));
        }

        function syncProfessionalSpecialties() {
            const profId = Number(el("agendaProfessionalId").value);
            const prof = professionals.find(p => p.id === profId);
            const info = el("professionalSpecsInfo");
            if (prof) {
                info.textContent = prof.specialties?.length
                    ? `Especialidades habilitadas: ${prof.specialties.join(", ")}`
                    : "Profesional de atención clínica general (sin especialidades asignadas).";
            } else {
                info.textContent = "No hay profesionales activos.";
            }

            const specSel = el("appointmentSpecialty");
            specSel.replaceChildren();
            if (prof?.specialties?.length) {
                for (const sp of prof.specialties) {
                    specSel.add(new Option(sp, sp));
                }
            } else {
                specSel.add(new Option("Sin especialidad configurada", ""));
            }
        }

        function syncKindUI() {
            const isSpec = el("appointmentKind").value === "SPECIALIST";
            el("appointmentSpecialtyField").hidden = !isSpec;
            el("appointmentSpecialty").required = isSpec;
        }
        el("appointmentKind").addEventListener("change", syncKindUI);
        syncKindUI();

        async function loadPetConsultations(preselectConsultId = "") {
            const petId = Number(el("appointmentPetId").value);
            const sel = el("appointmentConsultationId");
            sel.replaceChildren();
            sel.add(new Option("Sin consulta asociada", ""));
            if (!petId || !Sisvet.hasAnyRole("ADMINISTRADOR", "VETERINARIO", "ESPECIALISTA")) {
                if (preselectConsultId) {
                    sel.add(new Option(`Consulta #${preselectConsultId}`, String(preselectConsultId)));
                    sel.value = String(preselectConsultId);
                }
                return;
            }
            try {
                const consults = await Sisvet.api(`/api/clinical/patients/${petId}/consultations`);
                if (!view.isConnected) return;
                for (const c of consults) {
                    sel.add(new Option(`#${c.id} · ${Sisvet.formatDate(c.createdAt)} · ${c.reason}`, String(c.id)));
                }
                if (preselectConsultId) sel.value = String(preselectConsultId);
            } catch (_) {
                // Silencioso si el rol no tiene acceso a listado de consultas
            }
        }

        el("appointmentPetId").addEventListener("change", () => loadPetConsultations());
        el("agendaPetFilter").addEventListener("input", () => {
            const term = el("agendaPetFilter").value.trim().toLowerCase();
            const filtered = term
                ? allPets.filter(p => `${p.name} ${p.ownerName} ${p.ownerDocument || ""}`.toLowerCase().includes(term))
                : allPets;
            populateFilterableSelect(el("appointmentPetId"), filtered, petDisplayLabel);
            loadPetConsultations();
        });

        function renderCalendarAndSlots() {
            const monthStart = new Date(currentYear, currentMonth, 1);
            const monthLabel = new Intl.DateTimeFormat("es-PE", { month: "long", year: "numeric" }).format(monthStart);
            el("calendarMonthLabel").textContent = monthLabel;

            const byDay = new Map();
            for (const app of monthAppointments) {
                const k = dateKey(app.startsAt);
                if (!byDay.has(k)) byDay.set(k, []);
                byDay.get(k).push(app);
            }

            const grid = el("calendarGrid");
            grid.replaceChildren();

            // Lunes = 0 .. Domingo = 6
            const startOffset = (monthStart.getDay() + 6) % 7;
            const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
            const todayStr = dateKey(today);

            for (let i = 0; i < startOffset; i++) {
                const empty = document.createElement("div");
                empty.className = "calendar-day empty";
                grid.appendChild(empty);
            }

            for (let d = 1; d <= daysInMonth; d++) {
                const pad = n => String(n).padStart(2, "0");
                const k = `${currentYear}-${pad(currentMonth + 1)}-${pad(d)}`;
                const dayApps = byDay.get(k) || [];

                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = "calendar-day";
                if (k === todayStr) btn.classList.add("today");
                if (k === selectedDateKey) btn.classList.add("selected");

                const num = document.createElement("span");
                num.className = "day-num";
                num.textContent = String(d);
                btn.appendChild(num);

                if (dayApps.length > 0) {
                    const count = document.createElement("span");
                    count.className = "day-count";
                    count.textContent = `${dayApps.length} ${dayApps.length === 1 ? "cita" : "citas"}`;
                    btn.appendChild(count);
                }

                btn.addEventListener("click", () => {
                    selectedDateKey = k;
                    renderCalendarAndSlots();
                });

                grid.appendChild(btn);
            }

            renderDaySlots(byDay.get(selectedDateKey) || []);
        }

        function renderDaySlots(dayApps) {
            el("daySlotsTitle").textContent = `Horarios del ${Sisvet.formatDate(selectedDateKey)}`;
            const slotsContainer = el("daySlotsGrid");
            slotsContainer.replaceChildren();

            for (let hour = 8; hour < 20; hour++) {
                for (const min of [0, 30]) {
                    const pad = n => String(n).padStart(2, "0");
                    const hhmm = `${pad(hour)}:${pad(min)}`;
                    const slotStart = new Date(`${selectedDateKey}T${hhmm}:00`);
                    const slotEnd = new Date(slotStart.getTime() + 30 * 60000);

                    const conflict = dayApps.find(a => {
                        const s = new Date(a.startsAt);
                        const e = new Date(a.endsAt);
                        return s < slotEnd && e > slotStart;
                    });

                    const btn = document.createElement("button");
                    btn.type = "button";
                    if (conflict) {
                        btn.className = "slot-btn slot-busy";
                        btn.disabled = true;
                        btn.textContent = `${hhmm} · Ocupado (${conflict.petName})`;
                    } else {
                        btn.className = "slot-btn slot-free";
                        btn.textContent = `${hhmm} · Disponible`;
                        btn.addEventListener("click", () => {
                            el("appointmentDateInput").value = `${selectedDateKey}T${hhmm}`;
                            el("appointmentReasonInput").focus();
                        });
                    }
                    slotsContainer.appendChild(btn);
                }
            }
        }

        async function loadMonthAgenda() {
            const profId = Number(el("agendaProfessionalId").value);
            if (!profId) {
                monthAppointments = [];
                renderCalendarAndSlots();
                return;
            }

            const pad = n => String(n).padStart(2, "0");
            const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
            const start = `${currentYear}-${pad(currentMonth + 1)}-01T00:00:00`;
            const end = `${currentYear}-${pad(currentMonth + 1)}-${pad(daysInMonth)}T23:59:59`;

            monthAppointments = await Sisvet.api("/api/clinical/appointments/availability-views", {
                method: "POST",
                body: JSON.stringify({ professionalId: profId, start, end })
            });
            if (!view.isConnected) return;

            renderCalendarAndSlots();

            const canReschedule = isReceptionOrAdmin();
            Sisvet.renderTable("appointmentsTable", monthAppointments, [
                { label: "Inicio", value: a => `${Sisvet.formatDate(a.startsAt)} ${Sisvet.formatTime(a.startsAt)}` },
                { label: "Fin", value: a => Sisvet.formatTime(a.endsAt) },
                { label: "Mascota", value: a => `${a.petName} (${a.speciesLabel})` },
                { label: "Propietario", value: a => a.ownerName },
                { label: "Motivo", value: a => a.reason },
                { label: "Especialidad", value: a => a.specialty || "General" },
                {
                    label: "Acciones",
                    value: a => {
                        const btns = [
                            Sisvet.actionButton("Ficha", () => Sisvet.navigateTo(`/pacientes.html?petId=${a.petId}`), "outline")
                        ];
                        if (canReschedule) {
                            btns.push(Sisvet.actionButton("Reprogramar", async () => {
                                const newDate = await Sisvet.promptValue("Selecciona la nueva fecha y hora:", `Reprogramar cita de ${a.petName}`, "datetime-local", toLocalInputValue(new Date(a.startsAt)));
                                if (!newDate) return;
                                try {
                                    await Sisvet.api(`/api/clinical/appointments/${a.id}/reschedule`, {
                                        method: "PUT",
                                        body: JSON.stringify({ date: newDate })
                                    });
                                    Sisvet.message("appointmentsTableMessage", "Cita reprogramada.");
                                    await loadMonthAgenda();
                                } catch (error) {
                                    Sisvet.showError("appointmentsTableMessage", error);
                                }
                            }, "secondary"));
                            btns.push(Sisvet.actionButton("Cancelar", async () => {
                                if (!await Sisvet.confirmAction(`¿Cancelar la cita de ${a.petName} del ${Sisvet.formatDate(a.startsAt)}?`, "Cancelar cita", true)) return;
                                try {
                                    await Sisvet.api(`/api/clinical/appointments/${a.id}/cancel`, { method: "PUT" });
                                    Sisvet.message("appointmentsTableMessage", "Cita cancelada.");
                                    await loadMonthAgenda();
                                } catch (error) {
                                    Sisvet.showError("appointmentsTableMessage", error);
                                }
                            }, "danger"));
                        }
                        return btns;
                    }
                }
            ], { emptyText: "No hay citas confirmadas para este profesional en el mes seleccionado." });
        }

        el("agendaProfessionalId").addEventListener("change", () => {
            syncProfessionalSpecialties();
            loadMonthAgenda();
        });
        el("agendaRefreshBtn").addEventListener("click", loadMonthAgenda);

        el("calendarPrevBtn").addEventListener("click", () => {
            currentMonth--;
            if (currentMonth < 0) { currentMonth = 11; currentYear--; }
            const pad = n => String(n).padStart(2, "0");
            selectedDateKey = `${currentYear}-${pad(currentMonth + 1)}-01`;
            loadMonthAgenda();
        });

        el("calendarNextBtn").addEventListener("click", () => {
            currentMonth++;
            if (currentMonth > 11) { currentMonth = 0; currentYear++; }
            const pad = n => String(n).padStart(2, "0");
            selectedDateKey = `${currentYear}-${pad(currentMonth + 1)}-01`;
            loadMonthAgenda();
        });

        el("appointmentForm").addEventListener("submit", async event => {
            event.preventDefault();
            const f = event.target;
            const kind = el("appointmentKind").value;
            const profId = Number(el("agendaProfessionalId").value);
            if (!profId) {
                Sisvet.message("appointmentMessage", "Selecciona un profesional.", true);
                return;
            }

            const endpoint = kind === "FOLLOW_UP"
                ? "/api/clinical/appointments/follow-up"
                : kind === "SPECIALIST"
                    ? "/api/clinical/appointments/specialist"
                    : "/api/clinical/appointments";

            try {
                await Sisvet.api(endpoint, {
                    method: "POST",
                    body: JSON.stringify({
                        petId: Number(el("appointmentPetId").value),
                        professionalId: profId,
                        date: el("appointmentDateInput").value,
                        durationMinutes: Number(f.durationMinutes.value || 30),
                        reason: el("appointmentReasonInput").value.trim(),
                        specialty: kind === "SPECIALIST" ? el("appointmentSpecialty").value : null,
                        consultationId: el("appointmentConsultationId").value ? Number(el("appointmentConsultationId").value) : null
                    })
                });
                el("appointmentReasonInput").value = "";
                Sisvet.message("appointmentMessage", "Cita confirmada en agenda.");
                await loadMonthAgenda();
            } catch (error) {
                Sisvet.showError("appointmentMessage", error);
            }
        });

        await loadCatalogs();
        await loadMonthAgenda();
    }

    // ---------------------------------------------------------
    // 6. PENDIENTES Y SEGUIMIENTOS (seguimientos.html)
    // ---------------------------------------------------------
    async function initFollowUps(view) {
        let allPets = [];
        let pendingRows = [];
        let currentFilter = "ALL";

        async function loadPetsAndConsultations() {
            allPets = await Sisvet.api("/api/clinical/patients/summaries");
            if (!view.isConnected) return;
            populateFilterableSelect(el("followUpPetId"), allPets, petDisplayLabel);
            await refreshConsults();
        }

        async function refreshConsults() {
            const petId = Number(el("followUpPetId").value);
            const sel = el("followUpConsultationId");
            sel.replaceChildren();
            sel.add(new Option("Sin consulta asociada", ""));
            if (!petId || !Sisvet.hasAnyRole("ADMINISTRADOR", "VETERINARIO", "ESPECIALISTA")) return;
            try {
                const list = await Sisvet.api(`/api/clinical/patients/${petId}/consultations`);
                if (!view.isConnected) return;
                for (const c of list) {
                    sel.add(new Option(`#${c.id} · ${Sisvet.formatDate(c.createdAt)} · ${c.reason}`, String(c.id)));
                }
            } catch (_) {}
        }

        el("followUpPetId").addEventListener("change", refreshConsults);
        el("followUpPetFilter").addEventListener("input", () => {
            const term = el("followUpPetFilter").value.trim().toLowerCase();
            const filtered = term
                ? allPets.filter(p => `${p.name} ${p.ownerName} ${p.ownerDocument || ""}`.toLowerCase().includes(term))
                : allPets;
            populateFilterableSelect(el("followUpPetId"), filtered, petDisplayLabel);
            refreshConsults();
        });

        function applyFilter() {
            const endOfToday = new Date();
            endOfToday.setHours(23, 59, 59, 999);
            const rows = pendingRows.filter(f => {
                if (currentFilter === "DUE") return new Date(f.dueAt) <= endOfToday;
                if (currentFilter === "UPCOMING") return new Date(f.dueAt) > endOfToday;
                return true;
            });

            Sisvet.renderTable("followUpsTable", rows, [
                { label: "Paciente", value: f => `${f.petName} (${f.speciesLabel})` },
                { label: "Propietario y teléfono", value: f => `${f.ownerName}${f.ownerPhone ? " · " + f.ownerPhone : ""}` },
                { label: "Fecha programada", value: f => Sisvet.formatDateTime(f.dueAt) },
                {
                    label: "Estado",
                    value: f => {
                        const overdue = new Date(f.dueAt) < new Date();
                        if (f.status === "REVIEWED") return Sisvet.badge("Notificado / Revisado", "active");
                        return Sisvet.badge(overdue ? "Vencido" : "Pendiente", overdue ? "urgent" : "warn");
                    }
                },
                { label: "Motivo", value: f => f.reason },
                {
                    label: "Acciones",
                    value: f => [
                        Sisvet.actionButton("Enviar correo", async () => {
                            try {
                                const res = await Sisvet.api(`/api/clinical/appointments/pending-follow-ups/${f.id}/notify`, { method: "POST" });
                                Sisvet.message("followUpsListMessage", res?.message || "Recordatorio procesado.");
                                await loadFollowUps();
                            } catch (error) {
                                Sisvet.showError("followUpsListMessage", error);
                            }
                        }, "outline"),
                        Sisvet.actionButton("Completar", async () => {
                            try {
                                await Sisvet.api(`/api/clinical/appointments/pending-follow-ups/${f.id}/complete`, { method: "PUT" });
                                Sisvet.message("followUpsListMessage", "Seguimiento marcado como completado.");
                                await loadFollowUps();
                            } catch (error) {
                                Sisvet.showError("followUpsListMessage", error);
                            }
                        }),
                        Sisvet.actionButton("Agendar cita", () => Sisvet.navigateTo(`/citas.html?petId=${f.petId}`), "secondary"),
                        Sisvet.actionButton("Ficha", () => Sisvet.navigateTo(`/pacientes.html?petId=${f.petId}`), "outline")
                    ]
                }
            ], { emptyText: "No hay seguimientos pendientes en esta categoría." });
        }

        async function loadFollowUps() {
            pendingRows = await Sisvet.api("/api/clinical/appointments/pending-follow-ups/details");
            if (!view.isConnected) return;
            applyFilter();
        }

        for (const btn of el("followUpFilters").querySelectorAll(".tab-btn")) {
            btn.addEventListener("click", () => {
                currentFilter = btn.dataset.filter;
                for (const b of el("followUpFilters").querySelectorAll(".tab-btn")) {
                    b.classList.toggle("active", b === btn);
                }
                applyFilter();
            });
        }

        el("refreshFollowUpsBtn").addEventListener("click", loadFollowUps);

        el("followUpForm").addEventListener("submit", async event => {
            event.preventDefault();
            const f = event.target;
            try {
                await Sisvet.api("/api/clinical/appointments/pending-follow-ups", {
                    method: "POST",
                    body: JSON.stringify({
                        petId: Number(el("followUpPetId").value),
                        consultationId: el("followUpConsultationId").value ? Number(el("followUpConsultationId").value) : null,
                        dueAt: f.dueAt.value,
                        reason: f.reason.value.trim(),
                        appointmentId: null
                    })
                });
                f.reset();
                Sisvet.message("followUpMessage", "Seguimiento programado.");
                await loadFollowUps();
            } catch (error) {
                Sisvet.showError("followUpMessage", error);
            }
        });

        await Promise.all([loadPetsAndConsultations(), loadFollowUps()]);
    }

    const pages = {
        hub: ["Centro y configuración clínica", initHub],
        patients: ["Pacientes y ficha clínica", initPatients],
        attentions: ["Recepción y sala de espera", initAttentions],
        clinicalCare: ["Atención clínica", initClinicalCare],
        appointments: ["Agenda de citas", initAppointments],
        followUps: ["Pendientes y seguimientos", initFollowUps]
    };

    async function start(pageKey) {
        const view = document.querySelector(".layout .content");
        const entry = pages[pageKey];
        if (!entry) return;
        const [title, handler] = entry;
        if (!await Sisvet.init(title) || !view?.isConnected) return;
        await handler(view);
    }

    return { start };

})();
