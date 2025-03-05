console.log("inside execute.js");

let tablename;

// DOMContentLoaded ensures elements are loaded
document.addEventListener("DOMContentLoaded", () => {
    lucide.createIcons();

    const mainContent = document.getElementById("mainContent");
    const databaseGrid = document.getElementById("databaseGrid");
    const tableNameElement = document.getElementById("tableName");
    const queryForm = document.getElementById("queryForm");
    const dbNameSelect = document.getElementById("dbName");
    const queryInput = document.getElementById("query");
    const errorArea = document.getElementById("errorArea");
    const successArea = document.getElementById("successArea");
    const errorMessage = document.getElementById("errorMessage");
    const errorLineContainer = document.getElementById("errorLocation");
    const errorLine = document.getElementById("errorLine");
    const executionTime = document.getElementById("executionTime");
    const resultData = document.getElementById("resultData");
    const rowsAffected = document.getElementById("rowsAffected");

    // Check for missing elements
    if (!mainContent || !databaseGrid || !tableNameElement || !queryForm || !dbNameSelect || !queryInput ||
        !errorArea || !successArea || !errorMessage || !errorLineContainer || !executionTime || !resultData || !rowsAffected) {
        console.error("Required DOM elements not found:", {
            mainContent, databaseGrid, tableNameElement, queryForm, dbNameSelect, queryInput,
            errorArea, successArea, errorMessage, errorLineContainer, executionTime, resultData, rowsAffected
        });
        return;
    }

    // Show loader
    function showLoading() {
        const loaderOverlay = document.getElementById("loaderOverlay");
        if (loaderOverlay) {
            loaderOverlay.classList.add("show");
            document.body.classList.add("loading");
        } else {
            console.warn("Loader overlay not found");
        }
    }

    // Hide loader
    function hideLoading() {
        const loaderOverlay = document.getElementById("loaderOverlay");
        if (loaderOverlay) {
            loaderOverlay.classList.remove("show");
            document.body.classList.remove("loading");
            mainContent.style.filter = "blur(0px)";
        }
    }

    async function fetchDatabaseData(table) {
        showLoading();
        try {
            const requestData = {
                database: "mysql", // Hardcoded for now; could use dbNameSelect.value
                table: table
            };

            const response = await fetch('dbFetcher', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestData)
            });

            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            const dbResponse = await response.json();
            console.log("Fetched Data:", dbResponse);

            if (!dbResponse || typeof dbResponse !== "object") {
                console.error("Error: Invalid database response format", dbResponse);
                alert("Error: Invalid database response format.");
                return;
            }

            databaseGrid.innerHTML = '';
            Object.entries(dbResponse).forEach(([dbName, dbData]) => {
                if (dbData.data && dbData.columns) {
                    renderDatabaseTable(dbName, dbData);
                } else {
                    console.warn(`Skipping database ${dbName}: Invalid format`, dbData);
                }
            });

            console.log("All database data fetched and UI updated.");
        } catch (error) {
            console.error("Error fetching database data:", error);
            alert("Failed to fetch database data: " + error.message);
        } finally {
            hideLoading();
        }
    }

    function renderDatabaseTable(database, dbData) {
        const dbCard = document.createElement('div');
        dbCard.className = 'database-card bg-[#232936] rounded-lg shadow-xl p-6 space-y-4';

        dbCard.innerHTML = `
            <div class="flex items-center space-x-3">
                <div class="p-2 rounded-lg ${getDbColor(database)}">
                    <i data-lucide="database" class="w-5 h-5 text-white"></i>
                </div>
                <h2 class="font-semibold text-white">${database}</h2>
            </div>
            <p class="text-gray-400 text-sm">Execution Time: ${dbData.execution_time_ms.toFixed(2)} ms</p>
            <div class="overflow-x-auto">
                <table class="w-full text-sm text-left text-gray-300">
                    <thead class="text-xs uppercase bg-[#2a3241] text-gray-400">
                        <tr>
                            ${dbData.columns.map(column => `<th class="px-4 py-2">${column}</th>`).join('')}
                        </tr>
                    </thead>
                    <tbody>
                        ${dbData.data.map(row => `
                            <tr class="border-b border-[#2a3241]">
                                ${row.map(value => `<td class="px-4 py-2">${value}</td>`).join('')}
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;

        databaseGrid.appendChild(dbCard);
        lucide.createIcons();
    }

    function getDbColor(dbName) {
        const colorMap = {
            postgresql: 'bg-green-500',
            cassandra: 'bg-purple-500',
            mysql: 'bg-blue-500',
            mongodb: 'bg-orange-500'
        };
        return colorMap[dbName.toLowerCase()] || 'bg-gray-500';
    }

    function methodload() {
        let data = sessionStorage.getItem("tablename");
		console.log(data);
        if (data) {
            tablename = data;
            tableNameElement.innerText = tablename;
            fetchDatabaseData(tablename);
        } else {
            tableNameElement.innerText = "Create A Database To Execute Query";
        }
    }

    function dbselector(db) {
        if (db === "mysql" || db === "postgresql") {
            return "dummy";
        } else {
            return "data";
        }
    }

    function generateTable(columns, data) {
        return `
            <table class="w-full text-sm text-left text-gray-300">
                <thead class="text-xs uppercase bg-[#2a3241] text-gray-400">
                    <tr>
                        ${columns.map(column => `<th class="px-4 py-2">${column}</th>`).join('')}
                    </tr>
                </thead>
                <tbody>
                    ${data.map(row => `
                        <tr class="border-b border-[#2a3241]">
                            ${row.map(value => `<td class="px-4 py-2">${value}</td>`).join('')}
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    }

    methodload();

    queryForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        showLoading();

        const dbType = dbNameSelect.value;
        const databaseName = dbselector(dbType);
        const query = queryInput.value.trim();

        errorArea.classList.add("hidden");
        successArea.classList.add("hidden");
        errorLineContainer.classList.add("hidden");

        if (!query) {
            alert("Please enter a query.");
            hideLoading();
            return;
        }

        const requestData = { dbType, databaseName, query };

        try {
            const response = await fetch("executeQuery", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(requestData),
            });

            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            const result = await response.json();

            if (result.error) {
                errorMessage.textContent = result.error;
                if (result.error_line) {
                    errorLine.textContent = result.error_line;
                    errorLineContainer.classList.remove("hidden");
                }
                errorArea.classList.remove("hidden");
            } else {
                executionTime.innerHTML = `<span class="text-xl font-semibold text-white">${result.execution_time_ms.toFixed(2)} ms</span>`;
                rowsAffected.textContent = result.rowsAffected || 0; // Assuming API provides this; otherwise adjust
                if (result.data && result.columns) {
                    resultData.innerHTML = generateTable(result.columns, result.data);
                } else {
                    resultData.innerHTML = "";
                }
                successArea.classList.remove("hidden");
            }
        } catch (error) {
            alert("Failed to execute query: " + error.message);
            console.error("Error executing query:", error);
        } finally {
            hideLoading();
        }
    });
});