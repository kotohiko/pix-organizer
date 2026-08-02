const { default: open } = require("open");

module.exports = async function (url) {
    await open(url);
};