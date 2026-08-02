module.exports = {
    parse(input) {
        const m = input.match(/^(\d{8,9})_p\d+$/);

        if (!m) return null;

        return `https://www.pixiv.net/artworks/${m[1]}`;
    }
};