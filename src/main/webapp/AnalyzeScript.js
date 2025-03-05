lucide.createIcons();

    function showLoading(message) {
		console.log("hiiii")
      document.getElementById('loadingContainer').classList.remove('hidden');
      document.getElementById('mainContent').classList.add('blurred'); // Apply blur
      document.getElementById('loadingMessage').textContent = message;
    }
    
    function scrollToResults() {
      const resultsSection = document.getElementById("resultsSection");
      resultsSection.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function hideLoading() {
      document.getElementById('loadingContainer').classList.add('hidden');
      document.getElementById('mainContent').classList.remove('blurred'); // Remove blur
    }

    function handleQueryAnalysis() {
      const query = document.getElementById('queryInput').value.trim().toLowerCase();
      const resultsSection = document.getElementById('resultsSection');
      const queryResults = document.getElementById('queryResults');
      const projectResults = document.getElementById('projectResults');

      projectResults.classList.add("hidden"); // Hide project results
      console.log("Query Input:", query);

      let requestData = { "content": query };
      console.log("Sending request:", requestData);

      showLoading("Analyzing your query...");

      fetch('queryAnalyzer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestData)
      })
      .then(response => {
        if (!response.ok) throw new Error("HTTP error! Status: " + response.status);
        return response.json();
      })
      .then(data => {
        hideLoading();
        console.log("Response received:", data);
        
        

        if (data && data.result) {
          
          let modifiedResult = data.result.replace(
	        /^Top Database Overall: (.*)$/m,
	        '<span class="top-database">Top Database Overall: $1</span>'
	      );
	      
	      modifiedResult = modifiedResult.replace("Recommended Database:",'<span class="text-2xl font-bold text-green-200 mb-2 mr-1">Top Database Overall:</span>');
	      modifiedResult = modifiedResult.replace("PostgreSQL",`<span class="text-2xl font-bold text-green-200 mb-2">PostgreSQL</span>`)
	      modifiedResult = modifiedResult.replace("MongoDB",`<span class="text-2xl font-bold text-green-200 mb-2">MongoDB</span>`)
	      modifiedResult = modifiedResult.replace("Cassandra",`<span class="text-2xl font-bold text-green-200 mb-2">Apache Cassandra</span>`)
	      modifiedResult = modifiedResult.replace("MySQL",`<span class="text-2xl font-bold text-green-200 mb-2">MySQL</span>`)
	      let formattedAnswer = highlightMarkdown(modifiedResult);
	      console.log(formattedAnswer);
          document.getElementById('queryPerformance').innerHTML = formattedAnswer;
        } else {
          document.getElementById('queryPerformance').textContent = "No response received.";
        }

        resultsSection.classList.remove('hidden');
        queryResults.classList.remove('hidden');
        
        // Automatically scroll to the results after loading
        scrollToResults();
      })
      .catch(error => {
        hideLoading();
        console.error("Fetch Error:", error);
        alert("Error submitting data. Please try again.");
      });
    }

	function handleProjectAnalysis() {
	  document.getElementById("queryResults").classList.add("hidden");
	  const resultBox = document.getElementById("projectResults");
	  resultBox.classList.remove("hidden");
	  const query = document.getElementById('projectDescription').value.trim().toLowerCase();
	  const resultsSection = document.getElementById('resultsSection');
	  console.log("Query Input:", query);
	
	  let requestData = { "content": query };
	  console.log("Sending request:", requestData);
	
	  showLoading("Analyzing your project...");
	
	  fetch('projectAnalyzer', {
	    method: 'POST',
	    headers: { 'Content-Type': 'application/json' },
	    body: JSON.stringify(requestData)
	  })
	  .then(response => {
	    if (!response.ok) throw new Error("HTTP error! Status: " + response.status);
	    return response.json();
	  })
	  .then(data => {
	    hideLoading();
	    console.log("Response received:", data);
	
	    if (data && data.result) {
	      // Preprocess the response to wrap "Top Database Overall" in a styled HTML tag
	      console.log(data.result);
	      let modifiedResult = data.result.replace(
	        /^Top Database Overall: (.*)$/m,
	        '<span class="top-database">Top Database Overall: $1</span>'
	      );
	      
	      modifiedResult = modifiedResult.replace("1. Top Database Overall:",'<span class="text-2xl font-bold text-green-200 mb-2 mr-1">Top Database Overall:</span>');
	      modifiedResult = modifiedResult.replace("PostgreSQL",`<span class="text-2xl font-bold text-green-200 mb-2">PostgreSQL</span>`)
	      modifiedResult = modifiedResult.replace("MongoDB",`<span class="text-2xl font-bold text-green-200 mb-2">MongoDB</span>`)
	      modifiedResult = modifiedResult.replace("Apache Cassandra",`<span class="text-2xl font-bold text-green-200 mb-2">Apache Cassandra</span>`)
	      modifiedResult = modifiedResult.replace("MySQL",`<span class="text-2xl font-bold text-green-200 mb-2">MySQL</span>`)
	      
	      let formattedResponse = highlightMarkdown(modifiedResult);
	      document.getElementById('projectResults').innerHTML = formattedResponse;
	    } else {
	      document.getElementById('projectResults').textContent = "No response received.";
	    }
	
	    resultsSection.classList.remove('hidden');
	    scrollToResults();
	  })
	  .catch(error => {
	    hideLoading();
	    console.error("Fetch Error:", error);
	    alert("Error submitting data. Please try again.");
	  });
	}

    function highlightMarkdown(text) {
      return marked.parse(text);
    }