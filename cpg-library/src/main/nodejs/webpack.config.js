const path = require('path');

module.exports = {
    entry: './parser.js',
    target: 'node',
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: 'parser.js',
    }
};
