// CSS Hot Reload Configuration
const path = require('path');

// Ensure CSS modules are handled properly for hot reload
config.module = config.module || {};
config.module.rules = config.module.rules || [];

// Add CSS hot reload rule
config.module.rules.push({
    test: /\.css$/,
    use: [
        'style-loader', // Injects CSS into DOM and supports HMR
        'css-loader'    // Interprets @import and url() like import/require()
    ]
});

// Enable CSS source maps for better debugging
config.module.rules.forEach(rule => {
    if (rule.use && rule.use.find(use => use.loader === 'css-loader')) {
        const cssLoader = rule.use.find(use => use.loader === 'css-loader');
        cssLoader.options = cssLoader.options || {};
        cssLoader.options.sourceMap = true;
    }
});