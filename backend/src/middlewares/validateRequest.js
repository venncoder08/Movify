/**
 * Validation middleware for Zod schemas
 */

/**
 * Validate request data against Zod schema
 * @param {ZodSchema} schema - Zod schema to validate against
 * @returns {Function} Express middleware
 */
function validateRequest(schema) {
  return (req, res, next) => {
    try {
      // Parse request data
      const dataToValidate = {
        body: req.body,
        query: req.query,
        params: req.params
      };

      // Validate against schema
      const result = schema.safeParse(dataToValidate);

      if (!result.success) {
        const errors = result.error.errors.map((error) => ({
          field: error.path.join("."),
          message: error.message
        }));

        return res.status(400).json({
          error: "Validation Error",
          message: "Invalid request data",
          details: errors
        });
      }

      // Validation passed, continue
      next();
    } catch (error) {
      res.status(500).json({
        error: "Internal Server Error",
        message: error.message
      });
    }
  };
}

module.exports = {
  validateRequest
};
