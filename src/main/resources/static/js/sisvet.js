const Sisvet = (() => {

    let csrfToken = null;
    let currentUser = null;
    // Cambio: conserva el estado necesario para navegar entre vistas sin recargar
    let spaNavigationReady = false;
    let navigationVersion = 0;

    const links = [
        ["General", [
            ["Panel principal", "/panel.html", "panel-principal.svg"],
            ["Inventario", "/inventario.html", "inventario.svg"]
        ]],
        // El tercer campo queda vacío hasta que se asigne el SVG de cada apartado.
        ["Clínica", [
            ["Centro clínico", "/clinica.html", ""],
            ["Pacientes y ficha clínica", "/pacientes.html", ""],
            ["Recepción y sala de espera", "/atenciones.html", ""],
            ["Atención clínica integral", "/consultas.html", ""],
            ["Agenda de citas", "/citas.html", ""],
            ["Tratamientos", "/consultas.html?tab=treatment", ""],
            ["Vacunaciones", "/consultas.html?tab=vaccination", ""],
            ["Pendientes y seguimientos", "/seguimientos.html", ""]
        ]],
        ["Comercial", [
            ["Usuarios", "/usuarios.html", "usuarios.svg"],
            ["Productos", "/productos.html", "productos.svg"],
            ["Clientes", "/clientes.html", "Clientes.svg"],
            ["Proveedores", "/proveedores.html", "proveedores.svg"],
            ["Compras", "/compras.html", "Compra.svg"],
            ["Ventas", "/ventas.html", "Ventas.svg"],
            ["Procesamiento de pagos", "/pagos.html", ""],
            ["Facturación", "/facturas.html", "facturacion.svg"],
            ["Cobros clínicos", "/cobros-clinicos.html", "Cobros-Clinicos.svg"]
        ]],
        ["Finanzas", [
            ["Gastos", "/gastos.html", "Gastos.svg"],
            ["Reportes", "/reportes.html", "Reportes.svg"]
        ]]
    ];

    function isActiveNavigationLink(href, currentUrl) {
        const linkUrl = new URL(href, currentUrl);
        if (linkUrl.pathname !== currentUrl.pathname) {
            return false;
        }
        if (linkUrl.search) {
            return linkUrl.search === currentUrl.search;
        }

        const hasSpecificMatch = links.some(([, items]) => items.some(([, target]) => {
            const targetUrl = new URL(target, currentUrl);
            return targetUrl.pathname === currentUrl.pathname
                && targetUrl.search === currentUrl.search
                && Boolean(targetUrl.search);
        }));
        return !hasSpecificMatch;
    }

    async function syncPageStyles(nextDocument, baseUrl, version) {

        const baseStyles = new Set(Array.from(
            document.head.querySelectorAll('link[rel="stylesheet"]:not([data-page-stylesheet])')
        ).map(stylesheet => stylesheet.href));
        const nextStylesheets = Array.from(
            nextDocument.querySelectorAll('link[rel="stylesheet"][href]')
        ).map(stylesheet => ({
            source: stylesheet,
            href: new URL(stylesheet.getAttribute("href"), baseUrl).href
        }));
        const nextStyleUrls = new Set(nextStylesheets.map(stylesheet => stylesheet.href));
        const currentPageStyles = Array.from(
            document.head.querySelectorAll('link[rel="stylesheet"][data-page-stylesheet]')
        );
        const currentStyleUrls = new Set(currentPageStyles.map(stylesheet => stylesheet.href));
        const firstBaseStyle = document.head.querySelector(
            'link[rel="stylesheet"]:not([data-page-stylesheet])'
        );

        for (const { source, href } of nextStylesheets) {
            if (baseStyles.has(href) || currentStyleUrls.has(href)) {
                continue;
            }

            const stylesheet = document.createElement("link");
            for (const attribute of source.attributes) {
                stylesheet.setAttribute(attribute.name, attribute.value);
            }
            stylesheet.href = href;
            stylesheet.setAttribute("data-page-stylesheet", "");

            await new Promise((resolve, reject) => {
                stylesheet.onload = resolve;
                stylesheet.onerror = () => reject(new Error("No se pudo cargar una hoja de estilos."));
                document.head.insertBefore(stylesheet, firstBaseStyle);
            });

            currentStyleUrls.add(href);
            if (version !== navigationVersion) {
                return;
            }
        }

        if (version !== navigationVersion) {
            return;
        }

        for (const stylesheet of currentPageStyles) {
            if (!nextStyleUrls.has(stylesheet.href)) {
                stylesheet.remove();
            }
        }
    }

    // Cambio: sustituye el contenido de la vista y ejecuta sus inicializadores
    async function navigateTo(url, addHistory = true) {

        const version = ++navigationVersion;
        const response = await fetch(url.pathname + url.search, {
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error("No se pudo cargar la página solicitada.");
        }

        const html = await response.text();
        const nextDocument = new DOMParser().parseFromString(html, "text/html");
        const nextContent = nextDocument.querySelector(".layout .content");
        const currentContent = document.querySelector(".layout .content");

        if (!nextContent || !currentContent) {
            throw new Error("La página no contiene el área principal esperada.");
        }

        if (version !== navigationVersion) {
            return;
        }

        await syncPageStyles(nextDocument, url, version);
        if (version !== navigationVersion) {
            return;
        }

        const toolbar = currentContent.querySelector(".panel-toolbar");
        const nextHeading = nextContent.querySelector(".page-header h1, h1");
        const nextNodes = Array.from(nextContent.childNodes)
            .filter(node => !(node.nodeType === Node.ELEMENT_NODE
                && node.matches(".panel-toolbar")))
            .map(node => document.importNode(node, true));

        currentContent.replaceChildren(...(toolbar ? [toolbar] : []), ...nextNodes);

        if (toolbar && nextHeading) {
            toolbar.querySelector(".panel-current-page").textContent = nextHeading.textContent.trim();
        }

        for (const link of document.querySelectorAll("#navigation a[href]")) {
            link.classList.toggle("active", isActiveNavigationLink(link.href, url));
        }

        if (addHistory) {
            window.history.pushState({}, "", url.href);
        }

        document.title = nextDocument.title;

        for (const originalScript of nextDocument.querySelectorAll("script")) {

            if (originalScript.src) {
                const scriptUrl = new URL(originalScript.src, url);

                const scriptAlreadyLoaded = Array.from(document.scripts)
                    .some(script => script.src === scriptUrl.href);

                if (scriptAlreadyLoaded) {
                    continue;
                }

                const script = document.createElement("script");

                for (const attribute of originalScript.attributes) {
                    script.setAttribute(attribute.name, attribute.value);
                }

                await new Promise((resolve, reject) => {
                    script.onload = resolve;
                    script.onerror = () => reject(new Error("No se pudo cargar un script de la página."));
                    document.body.appendChild(script);
                });
            } else if (originalScript.type !== "application/json") {
                const script = document.createElement("script");
                script.textContent = "(function () {\n"
                    + originalScript.textContent
                    + "\n}).call(window);";
                document.body.appendChild(script);
            }

            if (version !== navigationVersion) {
                return;
            }
        }
    }

    // Cambio: habilita enlaces internos y los botones atrás/adelante del navegador
    function setupSpaNavigation() {

        if (spaNavigationReady) {
            return;
        }

        document.addEventListener("click", event => {

            if (event.defaultPrevented || event.button !== 0
                    || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) {
                return;
            }

            const link = event.target.closest("a[href]");

            if (!link || link.target === "_blank" || link.hasAttribute("download")
                    || !link.closest(".layout")) {
                return;
            }

            const url = new URL(link.href, window.location.href);

            if (url.origin !== window.location.origin
                    || !url.pathname.endsWith(".html")
                    || url.pathname === "/index.html") {
                return;
            }

            event.preventDefault();

            navigateTo(url).catch(error => {
                console.error("Error al cambiar de sección:", error);
                window.location.assign(url.href);
            });
        });

        window.addEventListener("popstate", () => {
            navigateTo(new URL(window.location.href), false).catch(error => {
                console.error("Error al restaurar la sección:", error);
                window.location.reload();
            });
        });

        spaNavigationReady = true;
    }

    // Mantiene la barra superior fija y enlazada con las secciones del panel
    function setupToolbar(content, title) {

        let toolbar = content.querySelector(".panel-toolbar");

        if (!toolbar) {
            const template = document.createElement("template");
            template.innerHTML = `
                <nav class="panel-toolbar" aria-label="Barra superior">
                    <h1 class="panel-current-page"></h1>
                    <div class="panel-toolbar-actions">
                        <div class="panel-search">
                            <label class="visually-hidden" for="moduleSearch">Buscar sección</label>
                            <input id="moduleSearch" type="search" placeholder="Buscar secciones..."
                                   role="combobox" aria-autocomplete="list" aria-expanded="false"
                                   aria-controls="moduleSearchResults" autocomplete="off">
                            <div id="moduleSearchResults" class="panel-search-results" role="listbox" hidden></div>
                        </div>
                        <a class="panel-sales-button" href="/ventas.html">
                            <img src="/images/paw-placeholder.svg" alt="">
                            <span>Nueva venta</span>
                        </a>
                        <button class="panel-icon-button" type="button" aria-label="Notificaciones">
                            <img src="/images/notification.svg" alt="">
                        </button>
                        <div class="panel-profile">
                            <button id="profileButton" class="panel-icon-button" type="button"
                                    aria-label="Abrir perfil" aria-haspopup="true" aria-expanded="false"
                                    aria-controls="profileMenu">
                                <img src="/images/user-icon.svg" alt="">
                            </button>
                            <div id="profileMenu" class="panel-profile-menu" role="menu" hidden>
                                <div class="panel-profile-details">
                                    <strong id="profileName"></strong>
                                    <span id="profileRole"></span>
                                    <span class="panel-session-status"><span></span>Activo</span>
                                </div>
                                <button id="logoutButton" type="button" role="menuitem">Cerrar sesión</button>
                            </div>
                        </div>
                    </div>
                </nav>`;
            toolbar = template.content.firstElementChild;
            content.prepend(toolbar);

            const search = toolbar.querySelector("#moduleSearch");
            const results = toolbar.querySelector("#moduleSearchResults");
            const profileButton = toolbar.querySelector("#profileButton");
            const profileMenu = toolbar.querySelector("#profileMenu");

            function updateSearchResults() {
                const query = search.value.trim().toLocaleLowerCase("es");
                const links = Array.from(document.querySelectorAll("#navigation a[href]"))
                    .filter(link => link.textContent.toLocaleLowerCase("es").includes(query));

                results.replaceChildren();

                for (const link of links) {
                    const option = document.createElement("a");
                    option.href = link.href;
                    option.textContent = link.textContent;
                    option.setAttribute("role", "option");
                    results.appendChild(option);
                }

                results.hidden = links.length === 0;
                search.setAttribute("aria-expanded", String(!results.hidden));
            }

            search.addEventListener("focus", updateSearchResults);
            search.addEventListener("input", updateSearchResults);
            search.addEventListener("keydown", event => {
                if (event.key === "Escape") {
                    results.hidden = true;
                    search.setAttribute("aria-expanded", "false");
                } else if (event.key === "ArrowDown" && !results.hidden) {
                    event.preventDefault();
                    results.querySelector("a")?.focus();
                } else if (event.key === "Enter" && !results.hidden) {
                    event.preventDefault();
                    results.querySelector("a")?.click();
                }
            });

            profileButton.addEventListener("click", () => {
                profileMenu.hidden = !profileMenu.hidden;
                profileButton.setAttribute("aria-expanded", String(!profileMenu.hidden));
            });

            toolbar.querySelector("#logoutButton").addEventListener("click", logout);

            document.addEventListener("click", event => {
                if (!event.target.closest(".panel-search")) {
                    results.hidden = true;
                    search.setAttribute("aria-expanded", "false");
                }

                if (!event.target.closest(".panel-profile")) {
                    profileMenu.hidden = true;
                    profileButton.setAttribute("aria-expanded", "false");
                }
            });

            document.addEventListener("keydown", event => {
                if (event.key === "Escape") {
                    profileMenu.hidden = true;
                    profileButton.setAttribute("aria-expanded", "false");
                }
            });
        }

        toolbar.querySelector(".panel-current-page").textContent = title;
        toolbar.querySelector("#profileName").textContent = currentUser.fullName;
        toolbar.querySelector("#profileRole").textContent = "Rol: " + currentUser.role;
    }

    async function csrf() {

        if (csrfToken) {
            return csrfToken;
        }

        const response = await fetch("/api/auth/csrf", {
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error("No se pudo obtener el token CSRF.");
        }

        csrfToken = await response.json();
        return csrfToken;
    }

    async function api(path, options = {}) {

        const method = (options.method || "GET").toUpperCase();
        const headers = { ...(options.headers || {}) };

        if (!["GET", "HEAD", "OPTIONS"].includes(method)) {

            const token = await csrf();
            headers[token.headerName] = token.token;

            if (options.body !== undefined && !headers["Content-Type"]) {
                headers["Content-Type"] = "application/json";
            }
        }

        const response = await fetch(path, {
            ...options,
            method,
            headers,
            credentials: "same-origin"
        });

        if (response.status === 401) {
            window.location.href = "/index.html";
            throw new Error("La sesión ha finalizado.");
        }

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";

        const data = contentType.includes("application/json")
            ? await response.json()
            : await response.text();

        if (!response.ok) {

            const errorMessage = typeof data === "object"
                ? data.message || data.error || "Error en la solicitud."
                : data || "Error en la solicitud.";

            throw new Error(errorMessage);
        }

        return data;
    }

    async function init(title) {

        // Cambio: registra la navegación antes de cargar cada modulo
        setupSpaNavigation();

        const response = await fetch("/api/auth/me", {
            credentials: "same-origin"
        });

        if (!response.ok) {
            window.location.href = "/index.html";
            return false;
        }

        currentUser = await response.json();

        const navigation = document.getElementById("navigation");

        if (navigation) {

            navigation.className = "sidebar";
            navigation.replaceChildren();

            for (const [section, items] of links) {

                const heading = document.createElement("div");
                heading.className = "section";
                heading.textContent = section;
                navigation.appendChild(heading);

                for (const [name, href, iconName] of items) {

                    const link = document.createElement("a");
                    link.href = href;

                    if (iconName) {
                        const icon = document.createElement("img");
                        icon.src = "/images/" + iconName;
                        icon.alt = "";
                        icon.className = "sidebar-link-icon";
                        icon.setAttribute("aria-hidden", "true");
                        link.appendChild(icon);
                    }

                    link.appendChild(document.createTextNode(name));

                    if (isActiveNavigationLink(href, new URL(window.location.href))) {
                        link.className = "active";
                    }

                    navigation.appendChild(link);
                }
            }

            const brand = document.createElement("h2");
            brand.className = "sidebar-brand";
            const brandLogo = document.createElement("img");
            brandLogo.src = "/images/paw-placeholder.svg";
            brandLogo.alt = "";
            brandLogo.className = "sidebar-brand-logo";
            brandLogo.setAttribute("aria-hidden", "true");
            brand.append(brandLogo, document.createTextNode("SISVET-CLINIC"));
            navigation.prepend(brand);

            const logoutButton = document.createElement("button");
            const logoutIcon = document.createElement("img");
            logoutIcon.src = "/images/log-out.svg";
            logoutIcon.alt = "";
            logoutIcon.className = "sidebar-link-icon";
            logoutIcon.setAttribute("aria-hidden", "true");
            logoutButton.append(logoutIcon, document.createTextNode("Cerrar sesión"));

            logoutButton.addEventListener("click", logout);
            navigation.appendChild(logoutButton);
        }

        const content = document.querySelector(".layout .content");
        const pageHeading = content && content.querySelector(".page-header h1, h1");

        if (content) {
            setupToolbar(content, pageHeading ? pageHeading.textContent.trim() : title);
        }

        document.title = title + " | SISVET-CLINIC";

        return true;
    }

    async function logout() {

        try {
            await api("/api/auth/logout", { method: "POST" });
            csrfToken = null;
            window.location.href = "/index.html";
        } catch (error) {
            alert(error.message);
        }
    }

    function message(id, value, error = false) {

        const element = document.getElementById(id);

        if (!element) {
            return;
        }

        element.textContent = value;
        element.className = error ? "message error" : "message";
    }

    function money(value) {
        return new Intl.NumberFormat("es-PE", {
            style: "currency",
            currency: "PEN"
        }).format(Number(value || 0));
    }

    function operationKey() {
        return crypto.randomUUID();
    }

    function renderTable(tableId, rows, columns) {

        const table = document.getElementById(tableId);
        table.replaceChildren();

        const thead = document.createElement("thead");
        const tbody = document.createElement("tbody");
        const headerRow = document.createElement("tr");

        for (const column of columns) {

            const th = document.createElement("th");
            th.textContent = column.label;
            headerRow.appendChild(th);
        }

        thead.appendChild(headerRow);

        for (const row of rows) {

            const tr = document.createElement("tr");

            for (const column of columns) {

                const td = document.createElement("td");
                const value = column.value(row);

                td.textContent = value == null ? "" : String(value);
                tr.appendChild(td);
            }

            tbody.appendChild(tr);
        }

        table.append(thead, tbody);
    }

    function renderObjectTable(tableId, rows) {

        if (!Array.isArray(rows) || rows.length === 0) {
            renderTable(tableId, [], []);
            return;
        }

        const columns = Object.keys(rows[0]).map(key => ({
            label: key.replace(/([A-Z])/g, " $1"),
            value: row => {
                const value = row[key];
                return typeof value === "object" && value !== null
                    ? JSON.stringify(value)
                    : value;
            }
        }));

        renderTable(tableId, rows, columns);
    }

    function showError(messageId, error) {
        message(messageId, error.message || "Error inesperado.", true);
        console.error(error);
    }

    return {
        api,
        init,
        // Expone el cierre de sesión para el menú del perfil del panel
        logout,
        csrf,
        money,
        message,
        operationKey,
        renderTable,
        renderObjectTable,
        showError,
        navigateTo: (target, addHistory = true) =>
            navigateTo(new URL(target, window.location.href), addHistory),
        getUser: () => currentUser
    };

})();