/**
 * Validation Schemas Index
 * All Zod schemas exported from this file
 */

const authSchemas = require("./authSchemas");
const driverRequestSchemas = require("./driverRequestSchemas");
const otpSchemas = require("./otpSchemas");

module.exports = {
  ...authSchemas,
  ...driverRequestSchemas,
  ...otpSchemas
};
