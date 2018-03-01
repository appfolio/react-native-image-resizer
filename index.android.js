import React from 'react-native';

const AEImageUtils = React.NativeModules.AEImageUtils;

export default {
  createResizedImage: (imagePath, newWidth, newHeight, compressFormat, quality, rotation = 0, outputPath) => {
    return new Promise((resolve, reject) => {
      AEImageUtils.createResizedImage(imagePath, newWidth, newHeight,
        compressFormat, quality, rotation, outputPath, resolve, reject);
    });
  },
};
