#import <React/RCTImageLoader.h>

#import "AEImageUtils.h"
#import "ImageHelpers.h"

@implementation AEImageUtils

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE()

static dispatch_queue_t RCTGetMethodQueue()
{
  static dispatch_queue_t queue;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    queue = dispatch_queue_create("com.appfolio.react.ImageResizerQueue", DISPATCH_QUEUE_SERIAL);
  });
  return queue;
}

- (dispatch_queue_t)methodQueue
{
  return RCTGetMethodQueue();
}

bool saveImage(NSString *fullPath, UIImage *image, NSString *format, float quality)
{
  NSData *data = nil;
  if ([format isEqualToString:@"JPEG"]) {
    data = UIImageJPEGRepresentation(image, quality / 100.0);
  } else if ([format isEqualToString:@"PNG"]) {
    data = UIImagePNGRepresentation(image);
  }

  if (data == nil) {
    return NO;
  }

  NSFileManager *fileManager = [NSFileManager defaultManager];
  return [fileManager createFileAtPath:fullPath contents:data attributes:nil];
}

NSString *generateFilePath(NSString *ext, NSString *outputPath, NSError *error)
{
  NSString *directory;

  if ([outputPath length] == 0) {
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    directory = [paths firstObject];
  } else {
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    directory = [documentsDirectory stringByAppendingPathComponent:outputPath];
    if (![[NSFileManager defaultManager] createDirectoryAtPath:directory withIntermediateDirectories:YES attributes:nil error:&error]) {
      return nil;
    }
  }

  NSString *name = [[NSUUID UUID] UUIDString];
  NSString *fullName = [NSString stringWithFormat:@"%@.%@", name, ext];
  NSString *fullPath = [directory stringByAppendingPathComponent:fullName];

  return fullPath;
}

UIImage *rotateImage(UIImage *inputImage, float rotationDegrees)
{
  // We want only fixed 0, 90, 180, 270 degree rotations.
  const int rotDiv90 = (int)round(rotationDegrees / 90);
  const int rotQuadrant = rotDiv90 % 4;
  const int rotQuadrantAbs = (rotQuadrant < 0) ? rotQuadrant + 4 : rotQuadrant;

  // Return the input image if no rotation specified.
  if (0 == rotQuadrantAbs) {
    return inputImage;
  } else {
    // Rotate the image by 80, 180, 270.
    UIImageOrientation orientation = UIImageOrientationUp;

    switch(rotQuadrantAbs) {
      case 1:
        orientation = UIImageOrientationRight; // 90 deg CW
        break;
      case 2:
        orientation = UIImageOrientationDown; // 180 deg rotation
        break;
      default:
        orientation = UIImageOrientationLeft; // 90 deg CCW
        break;
    }

    return [[UIImage alloc] initWithCGImage: inputImage.CGImage
                                      scale: 1.0
                                orientation: orientation];
  }
}

RCT_EXPORT_METHOD(createResizedImage:(NSString *)path
                  width:(float)width
                  height:(float)height
                  format:(NSString *)format
                  quality:(float)quality
                  rotation:(float)rotation
                  outputPath:(NSString *)outputPath
                  callback:(RCTResponseSenderBlock)callback)
{
  CGSize newSize = CGSizeMake(width, height);

  // Set image extension
  NSString *extension = @"jpg";
  if ([format isEqualToString:@"PNG"]) {
    extension = @"png";
  }

  NSString *fullPath = nil;
  NSError *filePathError = nil;
  if (!(fullPath = generateFilePath(extension, outputPath, filePathError))) {
    callback(@[@"Invalid output path.", @""]);
    return;
  }

  [_bridge.imageLoader loadImageWithURLRequest:[RCTConvert NSURLRequest:path] callback:^(NSError *error, UIImage *blockImage) {
    dispatch_async(RCTGetMethodQueue(), ^{
      UIImage *image = blockImage;
      if (error || image == nil) {
        if ([path hasPrefix:@"data:"] || [path hasPrefix:@"file:"]) {
          NSURL *imageUrl = [[NSURL alloc] initWithString:path];
          image = [UIImage imageWithData:[NSData dataWithContentsOfURL:imageUrl]];
        } else {
          image = [[UIImage alloc] initWithContentsOfFile:path];
        }
        if (image == nil) {
          callback(@[@"Can't retrieve the file from the path.", @""]);
          return;
        }
      }

      // Rotate image if rotation is specified.
      if (0 != (int)rotation) {
        image = rotateImage(image, rotation);
        if (image == nil) {
          callback(@[@"Can't rotate the image.", @""]);
          return;
        }
      }

      // Do the resizing
      UIImage *scaledImage = [image scaleToSize:newSize];
      if (scaledImage == nil) {
        callback(@[@"Can't resize the image.", @""]);
        return;
      }

      // Compress and save the image
      if (!saveImage(fullPath, scaledImage, format, quality)) {
        callback(@[@"Can't save the image. Check your compression format and your output path", @""]);
        return;
      }
      NSURL *fileUrl = [[NSURL alloc] initFileURLWithPath:fullPath];
      NSString *fileName = fileUrl.lastPathComponent;
      NSError *attributesError = nil;
      NSDictionary *fileAttributes = [[NSFileManager defaultManager] attributesOfItemAtPath:fullPath error:&attributesError];
      NSNumber *fileSize = fileAttributes == nil ? 0 : [fileAttributes objectForKey:NSFileSize];
      NSDictionary *response = @{@"path": fullPath,
                                 @"uri": fileUrl.absoluteString,
                                 @"name": fileName,
                                 @"size": fileSize == nil ? @(0) : fileSize
                                 };

      callback(@[[NSNull null], response]);
    });
  }];
}

@end
