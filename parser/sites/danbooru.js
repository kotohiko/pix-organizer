// noinspection JSUnusedGlobalSymbols
module.exports = {
    name: "Danbooru",

    parse(input) {
        const match = input.match(
            /^httpsdanbooru\.donmai\.usposts(\d+)$/
        );

        if (!match) {
            return null;
        }

        return `https://danbooru.donmai.us/posts/${match[1]}`;
    }
};