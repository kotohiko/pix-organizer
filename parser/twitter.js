module.exports = {
    parse(input) {
        const m = input.match(/^httpsx\.com(.+?)status(\d+)photo\d+$/);

        if (!m) return null;

        const username = m[1];
        const tweetId = m[2];

        return `https://x.com/${username}/status/${tweetId}`;
    }
};