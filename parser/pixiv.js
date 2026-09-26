module.exports = {
    name: "Pixiv",

    parse(input) {
        const match = input.match(/^(\d{8,9})_p\d+$/);

        if (!match) {
            return null;
        }

        return `https://www.pixiv.net/artworks/${match[1]}`;
    }
};