rm -rf build/
mkdir build/
npm run-script build
cp -r src/assets* build/
cp -r src/css* build/
cp -r dist/OpenAudioMc.bundle.js build/
cp -r src/index.html build/