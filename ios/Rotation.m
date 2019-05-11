// Inspired by https://github.com/pwmckenna/react-native-motion-manager

#import "Rotation.h"
#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>

@implementation Rotation

@synthesize bridge = _bridge;
RCT_EXPORT_MODULE();

- (id) init {
    self = [super init];
    NSLog(@"Rotation");

    if (self) {
        self->_motionManager = [[CMMotionManager alloc] init];
    }
    return self;
}

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

RCT_REMAP_METHOD(isAvailable,
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
    return [self isAvailableWithResolver:resolve
                                rejecter:reject];
}

- (void) isAvailableWithResolver:(RCTPromiseResolveBlock) resolve
                        rejecter:(RCTPromiseRejectBlock) reject {
    if([self->_motionManager isDeviceMotionAvailable])
    {
        /* Start the accelerometer if it is not active already */
        if([self->_motionManager isDeviceMotionActive] == NO)
        {
            resolve(@YES);
        } else {
            reject(@"-1", @"Rotation is not active", nil);
        }
    }
    else
    {
        reject(@"-1", @"Rotation is not available", nil);
    }
}

RCT_EXPORT_METHOD(setUpdateInterval:(double) interval) {
    NSLog(@"setDeviceMotionUpdateInterval: %f", interval);
    double intervalInSeconds = interval / 1000;

    [self->_motionManager setDeviceMotionUpdateInterval:intervalInSeconds];
}

RCT_EXPORT_METHOD(getUpdateInterval:(RCTResponseSenderBlock) cb) {
    double interval = self->_motionManager.deviceMotionUpdateInterval;
    NSLog(@"getUpdateInterval: %f", interval);
    cb(@[[NSNull null], [NSNumber numberWithDouble:interval]]);
}

RCT_EXPORT_METHOD(getData:(RCTResponseSenderBlock) cb) {
    CMRotationMatrix rotation = self->_motionManager.deviceMotion.attitude.rotationMatrix;

    NSArray* rotationMatrix4x4 = @[
        @(rotation.m11), @(rotation.m12), @(rotation.m13), @0.0,
        @(rotation.m21), @(rotation.m22), @(rotation.m23), @0.0,
        @(rotation.m31), @(rotation.m32), @(rotation.m33), @0.0,
                   @0.0,            @0.0,            @0.0, @1.0
    ];

    NSLog(@"getData: %@", rotationMatrix4x4);

    cb(@[[NSNull null], rotationMatrix4x4]);
}

RCT_EXPORT_METHOD(startUpdates) {
    NSLog(@"startUpdates");
    [self->_motionManager startDeviceMotionUpdates];

    /* Receive the Rotation data on this block */
    [self->_motionManager startDeviceMotionUpdatesUsingReferenceFrame:CMAttitudeReferenceFrameXTrueNorthZVertical
                                      toQueue:[NSOperationQueue mainQueue]
                                      withHandler:^(CMDeviceMotion *deviceMotion, NSError *error)
     {
         CMRotationMatrix rotation = deviceMotion.attitude.rotationMatrix;
         NSArray* rotationMatrix4x4 = @[
             @(rotation.m11), @(rotation.m12), @(rotation.m13), @0.0,
             @(rotation.m21), @(rotation.m22), @(rotation.m23), @0.0,
             @(rotation.m31), @(rotation.m32), @(rotation.m33), @0.0,
                        @0.0,            @0.0,            @0.0, @1.0
         ];
         NSLog(@"startUpdates: %@", rotationMatrix4x4);

         [self.bridge.eventDispatcher sendDeviceEventWithName:@"Rotation" body:rotationMatrix4x4];
     }];

}

RCT_EXPORT_METHOD(stopUpdates) {
    NSLog(@"stopUpdates");
    [self->_motionManager stopDeviceMotionUpdates];
}

@end
