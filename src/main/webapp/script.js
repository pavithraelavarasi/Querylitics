// Global variables
let choosedDB;
let choosedDBTiming;
let tablenames;
let dbChart = null;
let datanamea;

// Initialize Lucide icons and OS data
lucide.createIcons();
const osdata = JSON.parse(getClientInfo());

// Remove loading screen after 2 seconds
setTimeout(() => {
    document.getElementById('loadingScreen').style.display = 'none';
}, 2000);

// Utility function to load external scripts
function loadExternalScript(url, callback) {
    let script = document.createElement("script");
    script.src = url;
    script.async = true;
    script.onload = callback;
    document.head.appendChild(script);
}

// Page content
const pages = {
    home: `
        <div class="space-y-6">
          <!-- Generate Test Data Section -->
          <div class="bg-gray-800 rounded-lg shadow-xl p-6">
            <h2 class="text-2xl font-semibold mb-4 flex items-center text-white">
              <i data-lucide="database" class="mr-2"></i>
              Generate Test Data
            </h2>
            <div class="grid grid-cols-2 gap-4 mb-6" id="dbSelection">
              <button class="w-full p-4 rounded-lg border-2 border-gray-700 hover:border-blue-500 bg-gray-700/30 transition-all flex justify-center items-center space-x-2" onclick="openGenerateModal('mysql')">
                <img src="database.png" alt="MySQL Icon" class="w-8 h-8">
                <div class="font-medium text-white">MySQL</div>
              </button>
              <button class="w-full p-4 rounded-lg border-2 border-gray-700 hover:border-blue-500 bg-gray-700/30 transition-all flex justify-center items-center space-x-2" onclick="openGenerateModal('postgresql')">
                <img src="icons8-postgresql-48.png" alt="PostgreSQL Icon" class="w-12 h-11">
                <div class="font-medium text-white">PostgreSQL</div>
              </button>
              <button class="w-full p-4 rounded-lg border-2 border-gray-700 hover:border-blue-500 bg-gray-700/30 transition-all flex justify-center items-center space-x-2" onclick="openGenerateModal('cassandra')">
                <img src="Eye_Color_page-0001-removebg-preview.png" alt="Cassandra Icon" class="w-12 h-8">
                <div class="font-medium text-white">Cassandra</div>
              </button>
              <button class="w-full p-4 rounded-lg border-2 border-gray-700 hover:border-blue-500 bg-gray-700/30 transition-all flex justify-center items-center space-x-2" onclick="openGenerateModal('mongodb')">
                <img src="icons8-mongo-db-48.png" alt="MongoDB Icon" class="w-10 h-11">
                <div class="font-medium text-white">MongoDB</div>
              </button>
            </div>
          </div>
          <!-- System Information -->
          <div class="bg-gray-800 rounded-lg shadow-xl p-6">
            <h2 class="text-2xl font-semibold mb-4 flex items-center text-white">
              <i data-lucide="cpu" class="mr-2"></i>
              <p id="nodetb">System Information</p>
            </h2>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div class="bg-gray-700/50 p-4 rounded-lg">
                <div class="flex items-center justify-between mb-2">
                  <h3 class="text-lg font-medium text-white">Time</h3>
                  <i data-lucide="clock" class="w-5 h-5 text-blue-400"></i>
                </div>
                <p class="text-gray-300" id="nametb"></p>
                <p class="text-gray-300" id="currentTime">Loading...</p>
              </div>
              <div class="bg-gray-700/50 p-4 rounded-lg">
                <div class="flex items-center justify-between mb-2">
                  <h3 class="text-lg font-medium text-white" id="tbstorage">Storage</h3>
                  <i data-lucide="hard-drive" class="w-5 h-5 text-green-400"></i>
                </div>
                <div class="space-y-2">
                  <div class="flex justify-between text-gray-300">
                    <span id="tablesize">Used:</span>
                    <span id="used">45.2 GB</span>
                  </div>
                  <div class="w-full bg-gray-600 rounded-full h-2">
                    <div id="widbar" class="bg-green-500 h-2 rounded-full" style="width: 45%"></div>
                  </div>
                  <div class="flex justify-between text-gray-300">
                    <span id="rowsize">Total:</span>
                    <span id="rowtb">100 GB</span>
                  </div>
                </div>
              </div>
              <div class="bg-gray-700/50 p-4 rounded-lg">
                <div class="flex items-center justify-between mb-2">
                  <h3 class="text-lg font-medium text-white">System</h3>
                  <i data-lucide="monitor" class="w-5 h-5 text-purple-400"></i>
                </div>
                <div class="space-y-1 text-gray-300">
                  <p>OS: ${osdata.os}</p>
                  <p>Browser: ${osdata.browser}</p>
                  <p>Architecture: ${osdata.architecture}</p>
                </div>
              </div>
            </div>
          </div>
          <!-- Performance Chart -->
          <div class="card">
            <h2 class="card-title">
              <i data-lucide="bar-chart-2"></i>
              Database Performance Comparison
            </h2>
            <div class="chart-container relative">
              <canvas id="dbChart" class="transition-all duration-300"></canvas>
              <div id="loaderContainer" class="absolute inset-0 flex justify-center items-center hidden overflow-hidden">
                <iframe id="chartLoader" src="loader.html" class="w-50 h-50 overflow-hidden"></iframe>
              </div>
            </div>
            <div class="button-group">
              <button onclick="fetchDBTimings()" class="action-button primary" disabled>
                <i data-lucide="bar-chart-2"></i>
                Compare Databases
              </button>
              <button class="action-button secondary" onclick="Execute()">
                <i data-lucide="play"></i>
                Execute Query
              </button>
            </div>
          </div>
        </div>
    `,
    analyze: 'Analyze.html'
};

// Chart initialization
function initializeChart() {
    const ctx = document.getElementById('dbChart');
    if (!ctx) {
        console.error('Canvas element not found for Chart.js');
        return;
    }

    if (dbChart) {
        dbChart.destroy();
    }

    const defaultData = {
        labels: ['MySQL', 'PostgreSQL', 'Cassandra', 'MongoDB'],
        datasets: [{
            label: 'Query Time (ms)',
            data: [50, 45, 10, 35],
            borderColor: '#3b82f6',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            fill: true,
            tension: 0.4,
            pointRadius: 6,
            pointBackgroundColor: '#3b82f6',
            borderWidth: 2
        }]
    };

    dbChart = new Chart(ctx, {
        type: 'line',
        data: defaultData,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        color: '#f3f4f6',
                        font: { size: 14, weight: 'bold' },
                        padding: 20,
                        usePointStyle: true,
                        pointStyle: 'circle'
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(17, 24, 39, 0.9)',
                    titleColor: '#f3f4f6',
                    bodyColor: '#f3f4f6',
                    padding: 12,
                    borderRadius: 8,
                    callbacks: {
                        label: (context) => `${context.dataset.label}: ${context.raw} ms`
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: 'rgba(243, 244, 246, 0.1)' },
                    ticks: {
                        color: '#f3f4f6',
                        font: { size: 12 },
                        callback: (value) => `${value} ms`
                    },
                    title: {
                        display: true,
                        text: 'Response Time (milliseconds)',
                        color: '#f3f4f6',
                        font: { size: 14, weight: 'bold' }
                    }
                },
                x: {
                    grid: { color: 'rgba(243, 244, 246, 0.1)' },
                    ticks: {
                        color: '#f3f4f6',
                        font: { size: 12, weight: 'bold' }
                    }
                }
            }
        }
    });
}

// Chart update functions
function intialechart(database, time) {
    console.log("intialechart params:", database, time);
    if (!database || typeof time !== 'number') {
        console.error("Invalid database or time:", database, time);
        return;
    }
    const data = ["mysql", "postgresql", "cassandra", "mongodb"];
    const dt = [0, 0, 0, 0];
    const index = data.indexOf(database.toLowerCase());
    if (index !== -1) {
        dt[index] = time;
        updateChart(data, dt);
    } else {
        console.error("Database not recognized:", database);
    }
}

function updateChart(labels, data) {
    if (!dbChart) {
        console.error("Chart not initialized, initializing...");
        initializeChart();
    }
    dbChart.data.labels = labels;
    dbChart.data.datasets[0].data = data;
    dbChart.update();
}

// Page navigation
document.querySelectorAll('[data-page]').forEach(link => {
    link.addEventListener('click', async (e) => {
        e.preventDefault();
        const page = e.currentTarget.getAttribute('data-page');
        const contentArea = document.getElementById('contentArea');

        contentArea.classList.add('loading');

        if (typeof pages[page] === 'string' && pages[page].endsWith('.html')) {
            try {
                const response = await fetch(pages[page]);
                const html = await response.text();
                contentArea.innerHTML = html;
            } catch (error) {
                contentArea.innerHTML = `
                    <div class="bg-red-900/20 p-4 rounded-lg text-red-400">
                        Failed to load page. Please try again.
                    </div>
                `;
            }
        } else {
            contentArea.innerHTML = pages[page];
        }

        contentArea.classList.remove('loading');
        lucide.createIcons();

        if (page === 'home') {
            setTimeout(() => {
                console.log("Navigating to home");
                initializeChart();
                updateTime();
                lucide.createIcons();

                let singleData = JSON.parse(sessionStorage.getItem("data"));
                if (singleData) {
                    console.log("Rendering single DB data from sessionStorage:", singleData);
                    responseuiGenerate(singleData);
                    return;
                }

                let allDBData = JSON.parse(sessionStorage.getItem("allDBData"));
                if (allDBData) {
                    console.log("Rendering all DB data from sessionStorage:", allDBData);
                    const simulatedData = {
                        message: "success",
                        results: allDBData.labels.map((label, index) => ({
                            dbName: label,
                            timeTaken: allDBData.times[index]
                        }))
                    };
                    responseuiAllDB(simulatedData);
                }
            }, 100);
        }
    });
});

// Load home page by default
document.getElementById('contentArea').innerHTML = pages.home;
lucide.createIcons();

// Modal handling
const modal = document.getElementById('generateModal');
const closeModal = document.getElementById('closeModal');
const addColumnBtn = document.getElementById('addColumn');
const columnContainer = document.getElementById('columnContainer');
const generateForm = document.getElementById('generateForm');

function openGenerateModal(dbType) {
    modal.classList.add('active');
    const selectElement = modal.querySelector('select');
    selectElement.value = dbType;

    const extraFieldsContainer = document.getElementById('extraFieldsContainer');
    if (dbType === 'cassandra') {
        extraFieldsContainer.innerHTML = `
            <div class="space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-300 mb-2">Database Name</label>
                <input type="text" class="w-full bg-gray-700 text-white rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500" placeholder="Enter database name" id="dbName">
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-300 mb-2">Replication Factor</label>
                <input type="number" class="w-full bg-gray-700 text-white rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500" placeholder="Enter replication factor" id="replicationFactor">
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-300 mb-2">Class</label>
                <select class="w-full bg-gray-700 text-white rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500" id="classType">
                  <option value="SimpleStrategy">SimpleStrategy</option>
                  <option value="NetworkTopologyStrategy">NetworkTopologyStrategy</option>
                </select>
              </div>
            </div>
        `;
    } else {
        extraFieldsContainer.innerHTML = '';
    }

    columnContainer.innerHTML = '';
    const defaultColumn = document.createElement('div');
    defaultColumn.className = 'column-entry grid grid-cols-2 gap-4 relative';
    defaultColumn.innerHTML = `
        <button type="button" class="absolute top-0 right-0 p-2 text-gray-400 hover:text-red-400" onclick="removeColumn(this)">
            <i data-lucide="x-circle" class="w-5 h-5"></i>
        </button>
        <div>
            <label class="block text-sm font-medium text-gray-300 mb-2">Column Name</label>
            <input type="text" class="column-name w-full bg-gray-700 text-white rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500" placeholder="Enter column name" required>
        </div>
        <div>
            <label class="block text-sm font-medium text-gray-300 mb-2">Data Type</label>
            <select class="column-type w-full bg-gray-700 text-white rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500">
                <option value="int">Integer</option>
                <option value="varchar">String</option>
                <option value="date">Date</option>
                <option value="boolean">Boolean</option>
                <option value="float">Float</option>
            </select>
        </div>
    `;
    columnContainer.appendChild(defaultColumn);
    lucide.createIcons();
}

closeModal.addEventListener('click', () => {
    modal.classList.remove('active');
    document.getElementById("tableName").value = "";
});

addColumnBtn.addEventListener('click', () => {
    const newColumn = document.createElement('div');
    newColumn.className = 'column-entry grid grid-cols-2 gap-4 relative';
    newColumn.innerHTML = `
        <button type="button" class="absolute top-0 right-0 p-2 text-gray-400 hover:text-red-400" onclick="removeColumn(this)">
            <i data-lucide="x-circle" class="w-5 h-5"></i>
        </button>
        <div>
            <label class="block text-sm font-medium text-gray-300 mb-2">Column Name</label>
            <input type="text" class="column-name w-full bg-gray-700 text-white rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500" placeholder="Enter column name" required>
        </div>
        <div>
            <label class="block text-sm font-medium text-gray-300 mb-2">Data Type</label>
            <select class="column-type w-full bg-gray-700 text-white rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500">
                <option value="int">Integer</option>
                <option value="varchar">String</option>
                <option value="date">Date</option>
                <option value="boolean">Boolean</option>
                <option value="float">Float</option>
            </select>
        </div>
    `;
    columnContainer.appendChild(newColumn);
    lucide.createIcons();
});

function removeColumn(button) {
    const columnEntry = button.closest('.column-entry');
    columnEntry.remove();
}

// Form submission for GenerateData
generateForm.addEventListener('submit', (e) => {
    e.preventDefault();

    const dbType = document.getElementById('dbTypeSelect').value;
    choosedDB = dbType;
	tablenames = document.getElementById("tableName").value;
	sessionStorage.setItem("tablename",tablenames);
    const tableName = document.getElementById('tableName').value;
    const numRecords = document.getElementById("numRecords").value;

    let cassandraOptions = {};
    if (dbType === "cassandra") {
        cassandraOptions = {
            dbName: document.getElementById('dbName')?.value || "",
            replicationFactor: document.getElementById('replicationFactor')?.value || "",
            classType: document.getElementById('classType')?.value || ""
        };
    }

    const columns = [];
    document.querySelectorAll('.column-entry').forEach(column => {
        const columnName = column.querySelector('.column-name').value;
        const columnType = column.querySelector('.column-type').value;
        if (columnName && columnType) {
            columns.push({ name: columnName, type: columnType });
        }
    });
	document.getElementById("tableName").value = "";

    const requestData = {
        dbType,
        tableName,
        numRecords: parseInt(numRecords),
        ...cassandraOptions,
        columns
    };

    console.log("Sending JSON to GenerateData:", JSON.stringify(requestData, null, 2));

    const loaderContainer = document.getElementById("loaderContainer");
    const chartCanvas = document.getElementById("dbChart");
    loaderContainer.classList.remove("hidden");
    chartCanvas.classList.add("blur-md");
	modal.classList.remove('active')
	scrollToBottom();

    fetch('GenerateData', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestData)
    })
    .then(response => {
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        return response.json();
    })
    .then(data => {
        if (data.error) {
            console.error("GenerateData error:", data.error);
            alert("Failed to generate data: " + data.error);
        } else {
            responseuiGenerate(data);
        }
    })
    .catch(error => {
        console.error("Fetch error:", error);
        alert("Error submitting data: " + error.message);
    })
    .finally(() => {
        loaderContainer.classList.add("hidden");
        chartCanvas.classList.remove("blur-md");
       // modal.classList.remove('active');
    });
});

// Response handler for GenerateData with animations
function responseuiGenerate(data) {
    console.log("responseuiGenerate data:", data);
    let row = Math.abs(data.memoryUsed);
    choosedDBTiming = data.runtime;

    if (!dbChart) {
        console.log("Chart not initialized, initializing now...");
        initializeChart();
    }

    const storageTitle = document.getElementById("tbstorage");
    const usedElement = document.getElementById("used");
    const widbar = document.getElementById("widbar");
    const tableSize = document.getElementById("tablesize");
    const rowSize = document.getElementById("rowsize");
    const rowTb = document.getElementById("rowtb");
    const timeElement = document.getElementById("currentTime");
    const nodeTb = document.getElementById("nodetb");
    const nameTb = document.getElementById("nametb");

    timeElement.textContent = "Updating";
    timeElement.classList.add("loading-text");
    clearInterval(interval1);

    let db = document.getElementById('dbTypeSelect').value;

    setTimeout(() => {
        timeElement.classList.remove("loading-text");

        storageTitle.classList.add("fade-in");
        storageTitle.innerText = "Table Storage";

        usedElement.classList.add("fade-in");
        usedElement.innerText = formatBytes(row);

        widbar.style.transition = "width 0.5s ease-in-out";
        widbar.style.width = widgenerator(data.numberofrecords) + "%";

        tableSize.classList.add("fade-in");
        tableSize.innerText = "Table Storage:";

        rowSize.classList.add("fade-in");
        rowSize.innerText = "Row Size:";

        rowTb.classList.add("fade-in");
        rowTb.innerText = data.numberofrecords;

        timeElement.classList.add("scale-up");
        timeSetter(data.runtime);

        nodeTb.classList.add("fade-in");
        nodeTb.innerText = "Table Information";

        nameTb.classList.add("fade-in");
        setTbname(db);

        intialechart(db, data.runtime);

        setTimeout(() => {
            [storageTitle, usedElement, tableSize, rowSize, rowTb, timeElement, nodeTb, nameTb]
                .forEach(el => el.classList.remove("fade-in", "scale-up"));
            widbar.style.transition = "";
        }, 500);
    }, 1000);

    // Store data with 'tablename' key for execute.js compatibility
    const storageData = { ...data, tablename: tablenames};
    sessionStorage.setItem("data", JSON.stringify(storageData));
 //   tablenames = document.getElementById("tableName").value;
    
    document.querySelectorAll(".primary").forEach(button => {
        button.disabled = false;
    });
    
}

// Fetch all DB timings
async function fetchDBTimings() {
    console.log("Fetching all DB timings...");
    document.querySelectorAll(".primary").forEach(button => {
        button.disabled = true;
    });

    const loaderContainer = document.getElementById("loaderContainer");
    const chartCanvas = document.getElementById("dbChart");
    loaderContainer.classList.remove("hidden");
    chartCanvas.classList.add("blur-md");

    const tableName = document.getElementById('tableName').value;
    const numRecords = document.getElementById('numRecords').value;
    const columns = [];
    document.querySelectorAll('.column-entry').forEach(entry => {
        const columnName = entry.querySelector('.column-name').value;
        const columnType = entry.querySelector('.column-type').value;
        if (columnName && columnType) {
            columns.push({ name: columnName, type: columnType });
        }
    });

    let reqData = {
        chosedDB: choosedDB,
        chosedDBTime: choosedDBTiming,
        tableName: tablenames,
        columns: columns,
        numRecords: numRecords
    };

    try {
        const response = await fetch('allDb', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(reqData)
        });
        const data = await response.json();
        console.log("fetchDBTimings response:", data);

        if (data.message === "success") {
            responseuiAllDB(data);
        } else {
            console.error("Invalid API response:", data.error || "No results");
            updateChart(["Error"], [0]);
        }
    } catch (error) {
        console.error("Request failed:", error);
        updateChart(["Error"], [0]);
    } finally {
        loaderContainer.classList.add("hidden");
        chartCanvas.classList.remove("blur-md");
        document.querySelectorAll(".primary").forEach(button => {
            button.disabled = false;
        });
    }
}

// Response handler for all DB fetch with animations
function responseuiAllDB(data) {
    console.log("responseuiAllDB data:", data);

    if (!dbChart) {
        console.log("Chart not initialized, initializing now...");
        initializeChart();
    }

    const dbLabels = [];
    const dbTimes = [];
    let leastTime = Infinity;
    let fastestDB = "";

    data.results.forEach(db => {
        if (db.dbName && typeof db.timeTaken === 'number') {
            dbLabels.push(db.dbName);
            dbTimes.push(db.timeTaken);
            if (db.timeTaken < leastTime) {
                leastTime = db.timeTaken;
                fastestDB = db.dbName;
            }
        }
    });

    updateChart(dbLabels, dbTimes);

    const timeElement = document.getElementById("currentTime");
    const nodeTb = document.getElementById("nodetb");
    const nameTb = document.getElementById("nametb");

    timeElement.textContent = "Updating";
    timeElement.classList.add("loading-text");
    clearInterval(interval1);

    setTimeout(() => {
        timeElement.classList.remove("loading-text");

        nodeTb.classList.add("fade-in");
        nodeTb.innerText = "Compared To All Databases";

        nameTb.classList.add("fade-in");
        setTbname(fastestDB);

        timeElement.classList.add("scale-up");
        timeSetter(leastTime);

        document.getElementById("tbstorage").classList.add("fade-in");
        document.getElementById("tbstorage").innerText = "Storage (N/A for comparison)";

        document.getElementById("used").classList.add("fade-in");
        document.getElementById("used").innerText = "N/A";

        document.getElementById("widbar").style.transition = "width 0.5s ease-in-out";
        document.getElementById("widbar").style.width = "0%";

        document.getElementById("tablesize").classList.add("fade-in");
        document.getElementById("tablesize").innerText = "Table Storage:";

        document.getElementById("rowsize").classList.add("fade-in");
        document.getElementById("rowsize").innerText = "Row Size:";

        document.getElementById("rowtb").classList.add("fade-in");
        document.getElementById("rowtb").innerText = "N/A";

        setTimeout(() => {
            [nodeTb, nameTb, timeElement, document.getElementById("tbstorage"), document.getElementById("used"), 
             document.getElementById("tablesize"), document.getElementById("rowsize"), document.getElementById("rowtb")]
                .forEach(el => el.classList.remove("fade-in", "scale-up"));
            document.getElementById("widbar").style.transition = "";
        }, 500);
    }, 1000);

   	sessionStorage.removeItem("data");
    sessionStorage.setItem("allDBData", JSON.stringify({
        labels: dbLabels,
        times: dbTimes,
        fastestDB: fastestDB,
        leastTime: leastTime
    }));

    scrollToBottom();
}

// Utility functions
function updateTime() {
    const timeElement = document.getElementById('currentTime');
    if (timeElement) {
        const now = new Date();
        timeElement.textContent = now.toLocaleString();
    }
}

function getClientInfo() {
    let userAgent = navigator.userAgent.toLowerCase();
    let os = "Unknown OS";
    let architecture = "Unknown Architecture";
    let browser = "Unknown Browser";

    let userAgentParts = userAgent.split(" ");
    for (let i = 0; i < userAgentParts.length; i++) {
        if (userAgentParts[i].includes("win")) os = "Windows";
        else if (userAgentParts[i].includes("mac")) os = "macOS";
        else if (userAgentParts[i].includes("ubuntu")) os = "Ubuntu";
        else if (userAgentParts[i].includes("android")) os = "Android";
        else if (userAgentParts[i].includes("iphone") || userAgentParts[i].includes("ipad")) os = "iOS";
        else if (userAgentParts[i].includes("linux")) os = "Linux";

        if (navigator.platform.includes("Win64") || navigator.platform.includes("x86_64")) architecture = "x86_64";
        else if (navigator.platform.includes("Arm") || navigator.platform.includes("arm")) architecture = "ARM";
        else if (navigator.platform.includes("x86")) architecture = "x86";

        if (userAgentParts[i].includes("chrome")) { browser = "Chrome"; break; }
        else if (userAgentParts[i].includes("firefox")) browser = "Firefox";
        else if (userAgentParts[i].includes("safari") && !userAgentParts[i].includes("chrome")) browser = "Safari";
        else if (userAgentParts[i].includes("edge")) browser = "Edge";
        else if (userAgentParts[i].includes("opera")) browser = "Opera";
    }

    return JSON.stringify({ os, architecture, browser }, null, 2);
}

function formatBytes(bytes) {
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + " KB";
    return (bytes / (1024 * 1024)).toFixed(2) + " MB";
}

function msToSeconds(milliseconds) {
    return (milliseconds / 1000).toFixed(2) + " s";
}

function widgenerator(row) {
    return (row / 1000000) * 100;
}

function setTbname(name) {
    datanamea = name;
    document.getElementById("nametb").innerText = "Data Base: " + name;
}

function timeSetter(time) {
    document.getElementById("currentTime").innerText = "Time Taken: " + msToSeconds(time);
}

function scrollToBottom() {
    window.scrollTo({ top: document.body.scrollHeight, behavior: "smooth" });
}

// Additional functions
function Execute() {
    document.getElementById('contentArea').innerHTML = '<iframe src="execute.html" width="100%" height="1550px" scrolling="no" id="iframes"></iframe>';
}

function converter() {
    document.getElementById('contentArea').innerHTML = '<iframe src="converter.html" width="100%" height="1000px" scrolling="no"></iframe>';
}

function cassandra() {
    document.getElementById('contentArea').innerHTML = '<iframe src="CassandraVault.html" width="100%" height="1000px" scrolling="no"></iframe>';
}

function configure() {
    document.getElementById('contentArea').innerHTML = '<iframe src="cassandraplayground.html" width="100%" height="1500px" scrolling="no" id="iframes"></iframe>';
}

function Ring() {
    document.getElementById('contentArea').innerHTML = '<iframe src="RingStructure.html" width="100%" height="1000px" scrolling="no"></iframe>';
}


// Dropdown handling
document.addEventListener('DOMContentLoaded', function() {
    const dropdownButton = document.getElementById('cassandraDropdown');
    const dropdownMenu = document.getElementById('cassandraMenu');
    const dropdownLabel = document.getElementById('dropdownLabel');
    const chevronIcon = dropdownButton.querySelector('[data-lucide="chevron-down"]');
    const menuItems = dropdownMenu.querySelectorAll('[data-option]');

    dropdownButton.addEventListener('click', function(e) {
        e.stopPropagation();
        dropdownMenu.classList.toggle('hidden');
        chevronIcon.classList.toggle('rotate-180');
    });

    menuItems.forEach(item => {
        item.addEventListener('click', function(e) {
            e.stopPropagation();
            const selectedOption = item.getAttribute('data-option');
            dropdownLabel.textContent = selectedOption;
            dropdownMenu.classList.add('hidden');
            chevronIcon.classList.remove('rotate-180');
            menuItems.forEach(i => i.classList.remove('bg-gray-700'));
            item.classList.add('bg-gray-700');
        });
    });

    document.addEventListener('click', function() {
        dropdownMenu.classList.add('hidden');
        chevronIcon.classList.remove('rotate-180');
    });

    dropdownMenu.addEventListener('click', function(e) {
        e.stopPropagation();
    });

    updateTime();
    sessionStorage.clear();
    if (document.getElementById("dbChart")) {
        initializeChart();
    }
});

// Initialize time
let interval1 = setInterval(updateTime, 1000);
updateTime();