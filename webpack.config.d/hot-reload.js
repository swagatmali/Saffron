// Enhanced hot reload configuration
config.devServer = config.devServer || {};

// Enable hot module replacement
config.devServer.hot = true;

// Enable live reload as fallback
config.devServer.liveReload = true;

// Static content serving (configuration cache compatible)
config.devServer.static = config.devServer.static || [];
config.devServer.static.push({
    directory: "./src/wasmJsMain/resources",
    publicPath: "/"
});

// Watch options for better file detection
config.devServer.watchFiles = [
    "src/**/*.kt",
    "src/**/*.css",
    "src/**/*.html"
];

// Overlay errors in browser
config.devServer.client = {
    overlay: {
        errors: true,
        warnings: false
    }
};

// Enable compression for faster loading
config.devServer.compress = true;

// Headers for better development experience
config.devServer.headers = {
    "Access-Control-Allow-Origin": "*"
};