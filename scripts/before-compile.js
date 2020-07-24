const fs = require('fs');
const path = require('path');

function fatal(message) {
    throw new Error(`cordova-plugin-injectview: ${message}`);
}

module.exports = function(context) {
    let root = context.opts.projectRoot;
    if (!fs.existsSync(root)) {
        fatal(`invalid project root: ${root}`);
    }

    let platform = context.opts.platforms[0];
    if (!platform) {
        fatal(`invalid platform: ${platform}`);
    }

    let platformPath = context.opts.paths[0];
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

    let configFilename = path.join(root, 'platforms', platform, `${platform}.json`);
    if (!fs.existsSync(configFilename)) {
        fatal(`platform configuration file does not exist for ${platform}: ${configFilename}`);
    }

    let config = JSON.parse(fs.readFileSync(configFilename));
    let modules = (config && config.modules) || [];

    let scriptFilenames = [
        path.posix.join('www', 'cordova.js'),
        path.posix.join('www', 'cordova_plugins.js')
    ];

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

    let outputFilename = path.join(platformPath, 'cordova-plugin-injectview.json');
    fs.writeFileSync(outputFilename, JSON.stringify(scriptFilenames));

    if (!fs.existsSync(outputFilename)) {
        fatal(`output file does not exist: ${outputFilename}`);
    }
};