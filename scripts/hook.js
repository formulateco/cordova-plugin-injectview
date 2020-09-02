const fs = require('fs');
const path = require('path');

function fatal(message) {
    throw new Error(`cordova-plugin-injectview: ${message}`);
}

function createManifest(rootPath, platformName, platformPath, manifestFilename) {
    if (!fs.existsSync(platformPath)) {
        fatal(`platform path does not exist: ${platformPath}`);
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
    fs.writeFileSync(manifestFilename, JSON.stringify(scriptFilenames));
    if (!fs.existsSync(manifestFilename)) {
        fatal(`manifest file does not exist: ${manifestFilename}`);
    }
}

function removeManifest(manifestFilename) {
    if (!fs.existsSync(manifestFilename)) {
        return;
    }

    fs.unlinkSync(manifestFilename);
}

module.exports = function(context) {
    let rootPath = context.opts.projectRoot;
    if (!fs.existsSync(rootPath)) {
        fatal(`invalid project root: ${rootPath}`);
    }

    let platforms = (context.opts.cordova && context.opts.cordova.platforms) || [];
    for (let platformName of platforms) {
        var platformPath;
        if (platformName == 'android') {
            platformPath = path.join(rootPath, 'platforms', platformName, 'app', 'src', 'main', 'assets', 'www');
        } else if (platformName == 'ios') {
            platformPath = path.join(rootPath, 'platforms', platformName, 'www')
        } else {
            // Unsupported platform.
            continue;
        }

        let manifestFilename = path.join(platformPath, 'cordova-plugin-injectview.json');

        // Create or remove manifest file based on hook.
        if (context.hook == 'before_plugin_uninstall') {
            removeManifest(manifestFilename);
        } else {
            createManifest(rootPath, platformName, platformPath, manifestFilename);
        }
    }
};
