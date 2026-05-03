const env = require("../config/env");
const AppError = require("../utils/httpError");

async function fetchJson(pathname, params) {
  ensureApiKey();

  const url = new URL(`https://api.geoapify.com${pathname}`);
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      url.searchParams.set(key, String(value));
    }
  }
  url.searchParams.set("apiKey", env.geoapifyApiKey);

  const response = await fetch(url, {
    headers: {
      Accept: "application/json"
    }
  });

  const bodyText = await response.text();
  let payload;
  try {
    payload = bodyText ? JSON.parse(bodyText) : {};
  } catch (_error) {
    throw new AppError("Geoapify returned invalid JSON", 502);
  }

  if (!response.ok) {
    throw new AppError(
      payload.message || `Geoapify request failed with HTTP ${response.status}`,
      response.status,
      payload
    );
  }

  return payload;
}

function ensureApiKey() {
  if (!env.geoapifyApiKey) {
    throw new AppError("Server is missing GEOAPIFY_API_KEY", 500);
  }
}

module.exports = {
  fetchJson
};
