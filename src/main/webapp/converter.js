// Initialize Lucide icons
lucide.createIcons();

console.log("HI");

// Function to copy text to clipboard
function copyToClipboard(elementId) {
  const element = document.getElementById(elementId);
  element.select();
  document.execCommand('copy');
}

const queries = {
  mysql: "SELECT * FROM users LIMIT 10 OFFSET 20;",
  postgresql: "SELECT * FROM users OFFSET 20 LIMIT 10;",
  cassandra: "SELECT * FROM users WHERE TOKEN(id) > TOKEN(?) LIMIT 10;",
  mongodb: "db.users.find().skip(20).limit(10);"
};

const fromQuery = document.getElementById("sourceDb");
const toQuery = document.getElementById("targetDb");
const queryCodeElement = document.getElementById("fromQuery");

function updateQueryExample() {
  const sourceDbValue = fromQuery.value;
  const targetDbValue = toQuery.value;
  const fromdb = document.getElementById("fromdb");
  const todb = document.getElementById("todb");

  console.log("element");

  fromdb.innerText = `${sourceDbValue.charAt(0).toUpperCase() + sourceDbValue.slice(1)}:`;
  queryCodeElement.innerText = `${queries[sourceDbValue]}`;

  const targetQuery = document.getElementById("toQuery");
  todb.innerText = `${targetDbValue.charAt(0).toUpperCase() + targetDbValue.slice(1)}:`;
  targetQuery.innerText = `${queries[targetDbValue]}`;
}

// Event listeners for when the source or target database changes
fromQuery.addEventListener("change", updateQueryExample);
toQuery.addEventListener("change", updateQueryExample);

updateQueryExample();

// Method to clean and format any query
function cleanQuery(query) {
  // Step 1: Remove newlines and normalize whitespace
  let cleaned = query.replace(/\n/g, ' ').replace(/\s+/g, ' ').trim();
  
  cleaned = cleaned.replace("[","");
  cleaned = cleaned.replace("]","");
  cleaned = cleaned.replace("```","");
  cleaned = cleaned.replace("```","");
  cleaned = cleaned.replace("sql","");
  cleaned = cleaned.replace("postgresql","");
  cleaned = cleaned.replace("postgre","");
  cleaned = cleaned.replace('"',"");
  cleaned = cleaned.replace('"',"");
  cleaned = cleaned.replace('json',"");
  cleaned = cleaned.replace('mongo',"");
  cleaned = cleaned.replace('cql',"");
  cleaned = cleaned.replace("cassandra","");
  cleaned = cleaned.replace("/","");
  cleaned = cleaned.slice(2, -2);
  cleaned = cleaned.replaceAll('\n', '').replaceAll('/', '').replaceAll("--","").replace(/\\/g, "");;
/*  console.log(cleaned);*/
  let newText = cleaned.replace(/[`"\n\[\]]/g, '') // Removes backticks, double quotes, newlines, and square brackets
    .trim(); 
console.log(newText);
  

  return newText;
}

function convertQuery() {
  console.log("hi from convert query");
  const sourceDb = document.getElementById('sourceDb').value;
  const targetDb = document.getElementById('targetDb').value;
  const sourceQuery = document.getElementById('sourceQuery').value;
  const convertedQuery = document.getElementById('convertedQuery');

  const requestBody = {
    sourceDb: sourceDb,
    targetDb: targetDb,
    sourceQuery: sourceQuery
  };

  console.log("Request Body:", requestBody);

  fetch('convert', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(requestBody)
  })
  .then(response => {
    if (!response.ok) {
      throw new Error("Failed to convert query");
    }
    return response.json();
  })
  .then(data => {
    if (data.recommendation) {
      // Clean the query dynamically using the cleanQuery method
     /* console.log(data.recommendation)*/
      const cleanedQuery = cleanQuery(data.recommendation);
      console.log(cleanedQuery)
      convertedQuery.value = cleanedQuery;
    } else {
      convertedQuery.value = "No response received.";
    }
  })
  .catch(error => {
    console.error("Error:", error);
    convertedQuery.value = "An error occurred while converting the query.";
  });
}