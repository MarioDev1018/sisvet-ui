const Sisvet = (() => {

    let csrfToken = null;
    let currentUser = null;
    // Cambio: conserva el estado necesario para navegar entre vistas sin recargar.
    let spaNavigationReady = false;
    let navigationVersion = 0;

    const links = [
        ["General", [
            ["Panel principal", "/panel.html"],
            ["Inventario", "/inventario.html"]
        ]],
        ["Comercial", [
            ["Usuarios", "/usuarios.html"],
            ["Productos", "/productos.html"],
            ["Clientes", "/clientes.html"],
            ["Proveedores", "/proveedores.html"],
            ["Compras", "/compras.html"],
            ["Ventas", "/ventas.html"],
            ["Facturación", "/facturas.html"],
            ["Cobros clínicos", "/cobros-clinicos.html"]
        ]],
        ["Finanzas", [
            ["Gastos", "/gastos.html"],
            ["Reportes", "/reportes.html"]
        ]]
    ];

    // Cambio: sustituye el contenido de la vista y ejecuta sus inicializadores.
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

        currentContent.replaceWith(document.importNode(nextContent, true));

        if (addHistory) {
            window.history.pushState({}, "", url.href);
        }

        document.title = nextDocument.title;

        for (const originalScript of nextDocument.querySelectorAll("script")) {

            if (originalScript.src) {
                const scriptUrl = new URL(originalScript.src, url);

                if (scriptUrl.pathname === "/js/sisvet.js") {
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

    // Cambio: habilita enlaces internos y los botones atrás/adelante del navegador.
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

        // Cambio: registra la navegación SPA antes de cargar cada módulo.
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
            navigation.innerHTML = "<h2>SISVET-CLINIC</h2>";

            for (const [section, items] of links) {

                const heading = document.createElement("div");
                heading.className = "section";
                heading.textContent = section;
                navigation.appendChild(heading);

                for (const [name, href] of items) {

                    const link = document.createElement("a");
                    link.href = href;
                    link.textContent = name;

                    if (window.location.pathname === href) {
                        link.className = "active";
                    }

                    navigation.appendChild(link);
                }
            }

            const logoutButton = document.createElement("button");
            logoutButton.textContent = "Cerrar sesión";

            logoutButton.addEventListener("click", logout);
            navigation.appendChild(logoutButton);
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
        csrf,
        money,
        message,
        operationKey,
        renderTable,
        renderObjectTable,
        showError,
        getUser: () => currentUser
    };

})();