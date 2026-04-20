function errorHandler(error, _req, res, _next) {
  const status = Number(error.status || 500);
  const message = error.message || "Internal server error";

  console.error("[backend]", message);
  if (error.details) {
    console.error("[backend] details:", JSON.stringify(error.details));
  }

  res.status(status).json({ message });
}

module.exports = errorHandler;
