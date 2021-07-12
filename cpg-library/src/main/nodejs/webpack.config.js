const path = require('path');

module.exports = {
    entry: './src/parser.js',
    target: 'node',
    output: {
        path: path.resolve(__dirname, '../../../build/resources/main/nodejs'),
        filename: 'parser.js',
    }
};
