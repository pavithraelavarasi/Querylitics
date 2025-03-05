const machineIPs = [
  { ip: "10.51.25.165", url: "http://localhost:8080/TradeShow/cassandraproxy" },
  { ip: "10.51.24.73", url: "http://10.51.24.73:8080/GenerateDataUI/cassandraproxy" },
  { ip: "10.51.24.247", url: "http://10.51.24.247:8080/Loader/cassandraproxy" }
];

document.addEventListener("DOMContentLoaded", function () {
  lucide.createIcons();

  const form = document.getElementById("cassandraForm");
  const columnContainer = document.getElementById("columnContainer");
  const addColumnBtn = document.getElementById("addColumn");
  const fetchDataBtn = document.getElementById("fetchData");
  const machineContainer = document.getElementById("machineContainer");
  const serverToggles = document.querySelectorAll('.server-toggle');
  const serverStatuses = document.querySelectorAll('.server-status');

  // Check for missing elements
  if (!form) console.error("cassandraForm not found!");
  if (!columnContainer) console.error("columnContainer not found!");
  if (!addColumnBtn) console.error("addColumn not found!");
  if (!fetchDataBtn) console.error("fetchData not found!");
  if (!machineContainer) console.error("machineContainer not found!");

  // Add new column
  addColumnBtn.addEventListener("click", function () {
    if (columnContainer.children.length === 0) return;
    
    const template = columnContainer.children[0];
    const newColumn = template.cloneNode(true);

    newColumn.querySelector(".column-name").value = "";
    newColumn.querySelector(".column-type").selectedIndex = 0;
    
    const deleteBtn = newColumn.querySelector(".delete-column");
    deleteBtn.classList.remove("hidden");
    deleteBtn.addEventListener("click", function () {
      newColumn.remove();
    });

    columnContainer.appendChild(newColumn);
    lucide.createIcons();
  });
  
  // Server toggle functionality
  serverToggles.forEach((toggle, index) => {
    toggle.addEventListener('change', function() {
      const serverStatus = serverStatuses[index];
      const machineUrl = machineIPs[index].url;

      if (!this.checked) {
        serverStatus.innerHTML = `
          <div class="server-off-gif"></div>
          <p class="text-center font-bold text-red-500">Server Offline</p>
        `;
      } else {
        serverStatus.innerHTML = `
          <p class="mb-2 font-bold">Status: Ready</p>
          <p class="font-bold">Records: 0</p>
        `;
      }

      fetch(machineUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: this.checked ? "On" : "Off" })
      }).catch(error => console.error("Error toggling server:", error));
    });
  });

  form.addEventListener("submit", async function (e) {
    e.preventDefault();

    const formData = {
      database: document.getElementById("database").value,
      table: document.getElementById("tableName").value,
      replicationFactor: document.getElementById("number").value,
      numRecords: 10,
      columns: Array.from(columnContainer.children).map((column) => ({
        name: column.querySelector(".column-name").value,
        type: column.querySelector(".column-type").value,
      })),
    };

    console.log("Form submitted with data:", formData);

    try {
      const response = await fetch("createTable", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        throw new Error(`Server responded with status: ${response.status}`);
      }

      const data = await response.json();
      console.log("Response from server:", data);

      if (data.status === "success") {
        showSuccessPopup();
        // Removed machineContainer.classList.add("show")
      } else {
        showErrorPopup(data.message || "Unknown error occurred");
      }
    } catch (error) {
      console.error("Error fetching data:", error);
      showErrorPopup(`Error fetching data: ${error.message}`);
    }
  });
  
  machineIPs.forEach((obj) => {
    fetch(obj.url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "on" })
    }).catch(error => console.error("Error toggling server:", error));
  });
  
  // Retry fetch function
  async function fetchWithRetry(endpoint, body, statusElement, ip, maxRetries = 10, delayMs = 2000) {
    let attempts = 0;

    while (attempts < maxRetries) {
      try {
        statusElement.classList.add('loading');
        statusElement.innerHTML = `
          <p class="mb-2 font-bold">Status: Fetching data... (Attempt ${attempts + 1}/${maxRetries})</p>
          <p class="font-bold">Records: 0</p>
        `;

        const response = await fetch(endpoint, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });

        if (response.status === 200) {
          const data = await response.json();
          statusElement.classList.remove('loading');
          if (data.error) {
            statusElement.innerHTML = `
              <p class='mb-2 text-red-500 font-bold'>Error: ${data.error}</p>
              <p class='font-bold'>Records: 0</p>
            `;
          } else {
            statusElement.innerHTML = renderTable(data, ip);
          }
          return; // Success, exit the loop
        } else if (response.status === 500) {
          statusElement.classList.remove('loading');
          statusElement.innerHTML = `
            <p class='mb-2 text-red-500 font-bold'>Internal Server Error (500)</p>
            <p class='font-bold'>Records: 0</p>
          `;
        } else {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
      } catch (error) {
        console.error(`Fetch attempt ${attempts + 1} failed:`, error);
        statusElement.classList.remove('loading');
        statusElement.innerHTML = `
          <p class='mb-2 text-red-500 font-bold'>Fetch error: ${error.message}</p>
          <p class='font-bold'>Records: 0</p>
        `;
      }

      attempts++;
      if (attempts < maxRetries) {
        await new Promise(resolve => setTimeout(resolve, delayMs)); // Wait before retrying
      }
    }

    statusElement.innerHTML = `
      <p class='mb-2 text-red-500 font-bold'>Failed to fetch data after ${maxRetries} attempts</p>
      <p class='font-bold'>Records: 0</p>
    `;
  }

  // Fetch Data Button Click Event with Retry
  fetchDataBtn.addEventListener("click", function () {
    const databaseName = document.getElementById("database").value;
    const tableName = document.getElementById("tableName").value;

    const endpoints = [
      "fetchData",
      "http://10.51.24.73:8080/GenerateDataUI/fetchData",
      "http://10.51.24.247:8080/Loader/fetchData",
    ];

    const body = {
      database: databaseName,
      table: tableName,
    };

    serverStatuses.forEach((status, index) => {
      if (!serverToggles[index].checked) return;
      fetchWithRetry(endpoints[index], body, status, machineIPs[index].ip);
    });
  });
  
  function showSuccessPopup() {
    const popup = document.createElement("div");
    popup.classList.add(
      "popup",
      "bg-green-600",
      "text-white",
      "p-4",
      "rounded",
      "fixed",
      "top-4",
      "left-1/2",
      "transform",
      "-translate-x-1/2",
      "flex",
      "items-center",
      "gap-2",
      "z-50",
      "font-bold"
    );
    popup.innerHTML = `<i class="w-6 h-6 text-white">👍</i> <span>Success! Table created.</span>`;

    document.body.appendChild(popup);
    setTimeout(() => popup.remove(), 2000);
  }
  
  function showErrorPopup(message) {
    const popup = document.createElement("div");
    popup.classList.add(
      "popup",
      "bg-red-600",
      "text-white",
      "p-4",
      "rounded",
      "fixed",
      "top-4",
      "left-1/2",
      "transform",
      "-translate-x-1/2",
      "flex",
      "items-center",
      "gap-2",
      "z-50",
      "font-bold"
    );
    popup.innerHTML = `<i class="w-6 h-6 text-white">⚠️</i> <span>Error: ${message}</span>`;

    document.body.appendChild(popup);
    setTimeout(() => popup.remove(), 2000);
  }

  function renderTable(data, ip) {
    if (!Array.isArray(data) || data.length === 0) {
      return "<p class='mb-2 font-bold'>No data available.</p><p class='font-bold'>Records: 0</p>";
    }

    return `
      <div class='overflow-auto max-h-[300px] border border-gray-600 rounded-md hide-scrollbar'>
        <table class='w-full border-collapse border border-gray-600'>
          <thead class='bg-gray-700'>
            <tr>
              ${Object.keys(data[0]).map((key) => `<th class='border border-gray-600 p-3 font-bold text-center'>${key}</th>`).join("")}
            </tr>
          </thead>
          <tbody>
            ${data.map((row) => `<tr>${Object.values(row).map((value) => `<td class='border border-gray-600 p-3 text-center font-bold'>${value}</td>`).join("")}</tr>`).join("")}
          </tbody>
        </table>
      </div>
      <p class='mt-2 text-sm text-gray-400 text-right font-bold'>Total Records: ${data.length}</p>
      <p class='text-sm text-gray-400 text-right font-bold'>Server IP: ${ip}</p>
    `;
  }
});