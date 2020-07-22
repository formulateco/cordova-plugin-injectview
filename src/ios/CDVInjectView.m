#import "CDVInjectView.h"

@implementation CDVInjectView

- (void)pluginInitialize {
    NSLog(@"Plugin cordova-plugin-injectview loaded.");
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(pageDidLoad:) name:CDVPageDidLoadNotification object:self.webView];
}

- (void)pageDidLoad:(NSNotification *)event {
    // Read cordova.js and cordova_plugins.js.
    NSMutableArray *scripts = [NSMutableArray array];
    [scripts addObject:[self readJavaScriptFile:@"www/cordova"]];
    [scripts addObject:[self readJavaScriptFile:@"www/cordova_plugins"]];

    // Read source for each plugin.
    for (NSDictionary *pluginParameters in [self parseCordovaPlugins]) {
        NSString *file = pluginParameters[@"file"];
        NSString *path = [[NSString stringWithFormat:@"www/%@", file] stringByDeletingPathExtension];
        [scripts addObject:[self readJavaScriptFile:path]];
    }

    // Evaluate all concatenated sources at once.
    NSString *js = [scripts componentsJoinedByString:@"\n;\n"];
    [self.webViewEngine evaluateJavaScript:js completionHandler:^(id result, NSError *err) { }];
}

- (NSString*)readJavaScriptFile:(NSString*)resource {
    NSString *path = [[NSBundle mainBundle] pathForResource:resource ofType:@"js"];
    NSString *js = [NSString stringWithContentsOfFile:path encoding:NSUTF8StringEncoding error:NULL];
    return js;
}

- (NSArray*)parseCordovaPlugins {
    NSString *js = [self readJavaScriptFile:@"www/cordova_plugins"];
    NSScanner *scanner = [NSScanner scannerWithString:js];
    [scanner scanUpToString:@"[" intoString:nil];
    NSString *substring = nil;
    [scanner scanUpToString:@"];" intoString:&substring];
    substring = [NSString stringWithFormat:@"%@]", substring];
    NSError *localError;
    NSData *data = [substring dataUsingEncoding:NSUTF8StringEncoding];
    NSArray *pluginObjects = [NSJSONSerialization JSONObjectWithData:data options:0 error:&localError];
    return pluginObjects;
}

@end