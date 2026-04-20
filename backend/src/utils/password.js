const crypto = require("crypto");
const bcrypt = require("bcryptjs");

const SALT_ROUNDS = 10;

/**
 * Hash password
 * @param {string} password - Plain password
 * @returns {Promise<string>} Hashed password
 */
async function hashPassword(password) {
  return bcrypt.hash(password, SALT_ROUNDS);
}

function isBcryptHash(hash) {
  return typeof hash === "string" && /^\$2[aby]\$\d{2}\$/.test(hash);
}

function isLegacySha256Hash(hash) {
  return typeof hash === "string" && /^[a-f0-9]{64}$/i.test(hash);
}

function hashLegacyPassword(password) {
  return crypto.createHash("sha256").update(password).digest("hex");
}

/**
 * Compare password with hash
 * @param {string} password - Plain password
 * @param {string} hash - Password hash
 * @returns {Promise<boolean>} True if match
 */
async function comparePassword(password, hash) {
  if (!password || !hash) {
    return false;
  }

  if (isBcryptHash(hash)) {
    return bcrypt.compare(password, hash);
  }

  if (isLegacySha256Hash(hash)) {
    return hashLegacyPassword(password) === hash;
  }

  return false;
}

function needsPasswordRehash(hash) {
  return isLegacySha256Hash(hash);
}

module.exports = {
  hashPassword,
  comparePassword,
  needsPasswordRehash
};
