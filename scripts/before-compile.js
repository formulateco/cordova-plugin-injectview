const fs = require('fs');
const path = require('path');

function fatal(message) {
    throw new Error(`cordova-plugin-injectview: ${message}`);
}

function createCordovaManifest(rootPath, platformName, platformPath) {
    if (!platformName) {
        fatal(`invalid platform: ${platformName}`);
    }

    if (!platformPath) {
        fatal(`invalid platform path for ${platformName}`);
    }

    if (!fs.existsSync(platformPath)) {
        fatal(`platform output path does not exist: ${platformPath}`);
    }

    let cordovaFilename = path.join(platformPath, 'cordova.js');
    if (!fs.existsSync(cordovaFilename)) {
        fatal(`Cordova platform file does not exist: ${cordovaFilename}`);
    }

    let cordovaPluginsFilename = path.join(platformPath, 'cordova_plugins.js');
    if (!fs.existsSync(cordovaPluginsFilename)) {
        fatal(`Cordova plugins file does not exist: ${cordovaPluginsFilename}`);
    }

    // Load plugin info from platform configuration.
    let configFilename = path.join(rootPath, 'platforms', platformName, `${platformName}.json`);
    if (!fs.existsSync(configFilename)) {
        fatal(`platform configuration file does not exist for ${platformName}: ${configFilename}`);
    }

    let config = JSON.parse(fs.readFileSync(configFilename));
    let modules = (config && config.modules) || [];

    // Always include cordova.js and cordova_plugins.js
    // as part of the Cordova script manifest.
    let scriptFilenames = [
        path.posix.join('www', 'cordova.js'),
        path.posix.join('www', 'cordova_plugins.js')
    ];

    // Include each plugin as part of the manifest.
    for (let module of modules) {
        let filename = module.file;
        if (!filename) {
            continue;
        }

        let localPluginFilename = path.join(platformPath, filename);
        if (!fs.existsSync(localPluginFilename)) {
            fatal(`plugin file does not exist at expected location: ${localPluginFilename}`);
        }

        scriptFilenames.push(path.posix.join('www', filename));
    }

    // Write script manifest to be included as an app resource.
    let outputFilename = path.join(platformPath, 'cordova-plugin-injectview.json');
    fs.writeFileSync(outputFilename, JSON.stringify(scriptFilenames));

    if (!fs.existsSync(outputFilename)) {
        fatal(`output file does not exist: ${outputFilename}`);
    }
}

module.exports = function(context) {
    let rootPath = context.opts.projectRoot;
    if (!fs.existsSync(rootPath)) {
        fatal(`invalid project root: ${rootPath}`);
    }

    // Create manifest for each specified platform.
    for (let i = 0; i < context.opts.platforms.length; i++) {
        let platformName = context.opts.platforms[i];
        let platformPath = context.opts.paths[i];
        createCordovaManifest(rootPath, platformName, platformPath);
    }
};