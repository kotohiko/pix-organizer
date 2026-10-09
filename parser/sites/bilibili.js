// noinspection JSUnusedGlobalSymbols
module.exports = {
    name: "Bilibili",

    parse(input) {
        const match = input.match(
            /^httpswww\.bilibili\.comopus(\d+)#\d+$/
        );

        if (!match) {
            return null;
        }

        return `https://www.bilibili.com/opus/${match[1]}`;
    }
};