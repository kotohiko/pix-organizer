// noinspection JSUnusedGlobalSymbols
module.exports = {
    name: "Twitter",

    parse(input) {
        const match = input.match(
            /^httpsx\.com(.+?)status(\d+)(?:photo\d+)?$/
        );

        if (!match) {
            return null;
        }

        return `https://x.com/${match[1]}/status/${match[2]}`;
    }
};