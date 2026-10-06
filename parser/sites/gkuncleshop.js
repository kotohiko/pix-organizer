// noinspection JSUnusedGlobalSymbols
module.exports = {
    name: "gkuncleshop",

    parse(input) {
        if (!input.startsWith('【伯公仔】')) return null;
        let name = input.slice('【伯公仔】'.length);

        name = name.replace(/#\d+$/, '');

        return `https://www.gkuncleshop.com.tw/products/${name}.html`;
    }
};