/**
 * Validation Schemas Index
 * All Zod schemas exported from this file
 */

const authSchemas = require("./authSchemas");
const driverRequestSchemas = require("./driverRequestSchemas");
const driverSchemas = require("./driverSchemas");
const otpSchemas = require("./otpSchemas");
const tripSchemas = require("./tripSchemas");

module.exports = {
  ...authSchemas,
  ...driverRequestSchemas,
  ...driverSchemas,
  ...otpSchemas,
  ...tripSchemas
};
