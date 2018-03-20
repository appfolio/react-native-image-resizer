import {
  NativeModules,
  Platform,
} from 'react-native';

const SUPPORTED_FORMATS = Platform.select({
  ios: ['JPEG', 'PNG'],
  android: ['JPEG', 'PNG', 'WEBP'],
});

export default {
  createResizedImage: (path, width, height, format, quality, rotation = 0, outputPath) => {
    if (! SUPPORTED_FORMATS.includes(format)) {
      throw new Error(`Only ${SUPPORTED_FORMATS.join()} formats are supported by createResizedImage`);
    }

    return NativeModules.AEImageUtils.createResizedImage(path, width, height, format, quality, rotation, outputPath);
  },
};
