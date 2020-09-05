# cordova-plugin-injectview

Automatically inject Cordova platform and plugin scripts into your site.

## Setup

This plugin supports Android and iOS.

```
cordova plugin add cordova-plugin-injectview
```

## Usage

Adding the plugin to your project is sufficient.

At runtime, when a page loads in the Cordova-hosted web view, this plugin injects
Cordova's platform scripts as well as all of your plugin scripts into the web view.
By doing this, you can integrate Cordova with a remotely hosted site without needing
to explicitly include Cordova script tags or do request rewrites to load your Cordova
plugin scripts.

## How It Works

This plugin registers a number of [Cordova hooks](https://cordova.apache.org/docs/en/latest/guide/appdev/hooks/)
in [plugin.xml](plugin.xml) that execute [update-manifest.js](scripts/update-manifest.js).
This script parses each of your project configurations located at `platforms/{platform}/{platform}.json`
and generates a manifest file, `cordova-plugin-injectview.json`, that contains relative paths to Cordova's
platform scripts as well as your plugin scripts. This manifest file gets compiled into your app as a resource.

At runtime, this plugin hooks into the web view's page load event: `onPageFinished` for Android,
`CDVPageDidLoadNotification` for iOS. Once the event fires, this plugin parses the manifest to determine
all script paths, reads each script, concatenates everything into one large script, then evaluates the
whole thing using the web view's JavaScript engine.

## License

Copyright &copy; 2015 Fabian Strachanski  \
Copyright &copy; 2016 Sam Beran  \
Copyright &copy; 2020 Chris Schmich  \
MIT License. See [LICENSE](LICENSE) for details.
