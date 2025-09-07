#!/bin/bash

echo "🔥 Building production WASM build..."
./gradlew --no-daemon wasmJsBrowserProductionWebpack

echo "🧹 Cleaning docs folder..."
rm -rf docs/*

echo "📁 Copying fresh build to docs folder..."
cp -r composeApp/build/dist/wasmJs/productionExecutable/* docs/

echo "📊 Files in docs folder:"
ls -la docs/

echo "✅ Deployment ready! Commit and push the docs folder to deploy to GitHub Pages."
echo ""
echo "Next steps:"
echo "1. git add docs/"
echo "2. git commit -m 'Deploy latest build to GitHub Pages'"
echo "3. git push origin main"