#import "CDVInjectView.h"

@implementation CDVInjectView

- (void)pluginInitialize {
    NSLog(@"loader initializing");
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(pageDidLoad:) name:CDVPageDidLoadNotification object:self.webView];
}


- (void)pageDidLoad:(NSNotification *)event
{
    NSLog(@"loading cordova sources");
    [self injectJavascriptFile:@"www/cordova"];
    [self injectJavascriptFile:@"www/cordova_plugins"];
    for (NSDictionary* pluginParameters in [self parseCordovaPlugins]) {
        NSString* file = pluginParameters[@"file"];
        NSString* path = [NSString stringWithFormat:@"www/%@", file];
        path = [path stringByDeletingPathExtension];
        [self injectJavascriptFile:path];
    }
    [self injectJavascriptFile:@"www/js/index"];
}

- (void)injectJavascriptFile:(NSString*)resource {
    NSLog(@"Injecting %@.js", resource);
    NSString *jsPath = [[NSBundle mainBundle] pathForResource:resource ofType:@"js"];
    NSString *js = [NSString stringWithContentsOfFile:jsPath encoding:NSUTF8StringEncoding error:NULL];
    [self.webViewEngine evaluateJavaScript:js completionHandler:^(id result, NSError *err) { }];
}

- (NSArray*)parseCordovaPlugins{
    NSString *jsPath = [[NSBundle mainBundle] pathForResource:@"www/cordova_plugins" ofType:@"js"];
    NSString *js = [NSString stringWithContentsOfFile:jsPath encoding:NSUTF8StringEncoding error:NULL];
    NSScanner *scanner = [NSScanner scannerWithString:js];
    [scanner scanUpToString:@"[" intoString:nil];
    NSString *substring = nil;
    [scanner scanUpToString:@"];" intoString:&substring];
    substring = [NSString stringWithFormat:@"%@]", substring];
    NSError* localError;
    NSData* data = [substring dataUsingEncoding:NSUTF8StringEncoding];
    NSArray* pluginObjects = [NSJSONSerialization JSONObjectWithData:data options:0 error:&localError];
    return pluginObjects;
}
@end
