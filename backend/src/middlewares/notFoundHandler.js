function notFoundHandler(req, _res, next) {
  const error = new Error(`Cannot ${req.method} ${req.originalUrl}`);
  error.status = 404;
  next(error);
}

module.exports = notFoundHandler;
