#import "CDVInjectView.h"

@implementation CDVInjectView

- (void)pluginInitialize {
    NSLog(@"Plugin cordova-plugin-injectview loaded.");
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(pageDidLoad:) name:CDVPageDidLoadNotification object:self.webView];
}

- (void)pageDidLoad:(NSNotification *)event {
    NSArray *filenames = [self getCordovaFiles];
    if (filenames == nil) {
        return;
    }

    NSMutableArray *scripts = [NSMutableArray array];
    for (NSString *filename in filenames) {
        NSString *content = [self readResourceFile:filename];
        [scripts addObject:content];
    }

    // Evaluate all concatenated sources at once.
    NSString *expression = [scripts componentsJoinedByString:@"\n;\n"];
    [self.webViewEngine evaluateJavaScript:expression completionHandler:^(id result, NSError *err) { }];
}

- (NSString*)readResourceFile:(NSString*)filename {
    NSString *path = [[NSBundle mainBundle] pathForResource:filename ofType:nil];
    return [NSString stringWithContentsOfFile:path encoding:NSUTF8StringEncoding error:NULL];
}

- (NSArray*)getCordovaFiles {
    NSString *manifest = [self readResourceFile:@"www/cordova-plugin-injectview.json"];
    NSData *data = [manifest dataUsingEncoding:NSUTF8StringEncoding];
    if (data == nil) {
        NSLog(@"Could not find or load Cordova script manifest.");
        return nil;
    }

    NSError *error;
    NSArray *filenames = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
    if (filenames == nil) {
        NSLog(@"Failed to parse Cordova script manifest.");
        return nil;
    }

    return filenames;
}

@end
