module.exports = {
    parse(input) {
        const m = input.match(/^httpsdanbooru\.donmai\.usposts(\d+)$/);

        if (!m) return null;

        return `https://danbooru.donmai.us/posts/${m[1]}`;
    }
};