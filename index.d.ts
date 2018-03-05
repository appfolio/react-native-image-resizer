declare module "react-native-image-utils" {
    export interface Response {
        path: string;
        uri: string;
        size?: number;
        name?: string;
    }

    export default class ImageUtils {
        static createResizedImage(
            uri: string, width: number, height: number,
            format: "PNG" | "JPEG" | "WEBP", quality: number,
            rotation?: number, outputPath?: string
        ): Promise<Response>;
    }
}
