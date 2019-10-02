# React Native Image Utils

A React Native module that can create scaled versions of local images (also supports the assets library on iOS).

## Getting started

### **1. Add `react-native-image-utils` to your dependencies**

```sh
$ yarn add github:appfolio/react-native-image-utils
```

### **2. Linking native dependencies**

#### Mostly automatic installation

```sh
$ react-native link react-native-image-utils
```

#### *iOS*:

If using cocoapods in the `ios/` directory run
```sh
$ pod install
```

#### **Manual installation**

#### *iOS*

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-image-utils` ➜ `ios` and add `AEImageUtils.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libAEImageUtils.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### *Android*

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.appfolio.react.imageutils.AEImageUtilsPackage;` to the imports at the top of the file
  - Add `new AEImageUtilsPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-image-utils'
  	project(':react-native-image-utils').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-image-utils/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-image-utils')
  	```

## Usage example

```javascript
import ImageUtils from 'react-native-image-utils';

ImageUtils.createResizedImage(imageUri, newWidth, newHeight, compressFormat, quality, rotation, outputPath).then((response) => {
  // response.uri is the URI of the new image that can now be displayed, uploaded...
  // response.path is the path of the new image
  // response.name is the name of the new image with the extension
  // response.size is the size of the new image
}).catch((err) => {
  // Oops, something went wrong. Check that the filename is correct and
  // inspect err to get more details.
});
```

### Sample app

A basic, sample app is available in [the `example` folder](https://github.com/bamlab/react-native-image-utils/tree/master/example). It uses the module to resize a photo from the Camera Roll.

## API

### `promise createResizedImage(path, maxWidth, maxHeight, compressFormat, quality, rotation = 0, outputPath)`

The promise resolves with an object containing: `path`, `uri`, `name` and `size` of the new file. The URI can be used directly as the `source` of an [`<Image>`](https://facebook.github.io/react-native/docs/image.html) component.

Option | Description
------ | -----------
path | Path of image file, or a base64 encoded image string prefixed with 'data:image/imagetype' where `imagetype` is jpeg or png.
maxWidth | Image max width (ratio is preserved)
maxHeight | Image max height (ratio is preserved)
compressFormat | Can be either JPEG, PNG or WEBP (android only).
quality | A number between 0 and 100. Used for the JPEG compression.
rotation | Rotation to apply to the image, in degrees, for android. On iOS, rotation is limited (and rounded) to multiples of 90 degrees.
outputPath | The resized image path. If null, resized image will be stored in cache folder. To set outputPath make sure to add option for rotation too (if no rotation is needed, just set it to 0).
