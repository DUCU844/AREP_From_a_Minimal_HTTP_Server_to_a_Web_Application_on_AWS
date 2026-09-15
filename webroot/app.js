// Asynchronous client for the hardcoded services exposed by the Java server.
// Every action here prevents the default form submission, shows a loading
// state, and clearly separates:
//   (a) network failures (fetch() itself throws / rejects), from
//   (b) valid HTTP responses with an error status (4xx/5xx and a JSON body).

function showResult(resultEl, errorEl, message) {
    errorEl.style.display = "none";
    resultEl.textContent = message;
    resultEl.style.display = "block";
}

function showError(resultEl, errorEl, message) {
    resultEl.style.display = "none";
    errorEl.textContent = message;
    errorEl.style.display = "block";
}

function setLoading(button, resultEl, errorEl, isLoading) {
    button.disabled = isLoading;
    if (isLoading) {
        resultEl.style.display = "none";
        errorEl.style.display = "none";
        button.dataset.originalText = button.dataset.originalText || button.textContent;
        button.textContent = "Loading...";
    } else {
        button.textContent = button.dataset.originalText || button.textContent;
    }
}

/**
 * Calls a service endpoint and routes the outcome through onSuccess/onHttpError,
 * with network failures handled separately (per the lab's test matrix).
 */
async function callService(url, { resultEl, errorEl, button }) {
    setLoading(button, resultEl, errorEl, true);
    try {
        const response = await fetch(url, { method: "GET" });
        let body;
        try {
            body = await response.json();
        } catch (parseError) {
            throw new Error("The server returned a response that was not valid JSON.");
        }

        if (!response.ok) {
            // Valid HTTP response, but a controlled client/server error.
            const message = body && body.error ? body.error : `Request failed with status ${response.status}.`;
            showError(resultEl, errorEl, message);
            return;
        }

        return body;
    } catch (networkError) {
        // fetch() itself failed: DNS, connection refused, timeout, CORS, etc.
        showError(resultEl, errorEl, "Network error: could not reach the server. " + networkError.message);
        return undefined;
    } finally {
        setLoading(button, resultEl, errorEl, false);
    }
}

document.getElementById("greeting-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const name = document.getElementById("greeting-name").value;
    const resultEl = document.getElementById("greeting-result");
    const errorEl = document.getElementById("greeting-error");
    const button = event.target.querySelector("button");

    const url = "/api/greeting?name=" + encodeURIComponent(name);
    const body = await callService(url, { resultEl, errorEl, button });
    if (body) {
        showResult(resultEl, errorEl, body.greeting);
    }
});

document.getElementById("square-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const value = document.getElementById("square-value").value;
    const resultEl = document.getElementById("square-result");
    const errorEl = document.getElementById("square-error");
    const button = event.target.querySelector("button");

    const url = "/api/square?value=" + encodeURIComponent(value);
    const body = await callService(url, { resultEl, errorEl, button });
    if (body) {
        showResult(resultEl, errorEl, `${body.input}^2 = ${body.square}`);
    }
});

document.getElementById("time-button").addEventListener("click", async (event) => {
    const resultEl = document.getElementById("time-result");
    const errorEl = document.getElementById("time-error");
    const button = event.target;

    const body = await callService("/api/time", { resultEl, errorEl, button });
    if (body) {
        showResult(resultEl, errorEl, "Server time: " + body.serverTime);
    }
});
