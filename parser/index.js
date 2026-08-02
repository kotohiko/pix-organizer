const fs = require("fs");
const path = require("path");

module.exports = fs
    .readdirSync(__dirname)
    .filter(f => f.endsWith(".js") && f !== "index.js")
    .map(f => require(path.join(__dirname, f)));