/**
 * Validation Schemas Index
 * All Zod schemas exported from this file
 */

const authSchemas = require("./authSchemas");
const driverRequestSchemas = require("./driverRequestSchemas");
const driverSchemas = require("./driverSchemas");
const messageSchemas = require("./messageSchemas");
const otpSchemas = require("./otpSchemas");
const tripSchemas = require("./tripSchemas");

module.exports = {
  ...authSchemas,
  ...driverRequestSchemas,
  ...driverSchemas,
  ...messageSchemas,
  ...otpSchemas,
  ...tripSchemas
};
