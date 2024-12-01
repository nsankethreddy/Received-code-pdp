package ime.model;

import java.util.List;

/**
 * This class implements image dithering functionality, extending AbstractFilters. It applies the
 * dithering algorithm to create a black and white image effect.
 */
public class ImageDither extends AbstractFilters {

  private final ModelInterface model;

  /**
   * Constructs an ImageDither object.
   *
   * @param model The ModelInterface object used to interact with the image data.
   */
  public ImageDither(ModelInterface model) {
    this.model = model;
  }

  /**
   * Executes the dithering operation on a specified image. This method performs the following
   * steps: 1. Extracts necessary information from the command tokens. 2. Retrieves the original
   * image from the model. 3. Creates an intensity component of the original image. 4. Applies the
   * dithering algorithm. 5. Creates a split image combining the original and dithered versions. 6.
   * Stores the resulting image in the model.
   *
   * @param commandTokens A list of strings containing the command and its parameters. Expected
   *                      format: [command, sourceImageName, destinationImageName, splitPercentage]
   */
  @Override
  public void execute(List<String> commandTokens) {
    String imageName = commandTokens.get(1);
    String destName = commandTokens.get(2);
    int splitPercent = extractSplitPercent(commandTokens);

    ImageInterface originalImage = model.getImage(imageName);
    int height = originalImage.getHeight();
    int width = originalImage.getWidth();

    ImageInterface intensityComponent = new Image(height, width);
    for (int r = 0; r < height; r++) {
      for (int c = 0; c < width; c++) {
        PixelInterface pixel = originalImage.getPixel(r, c);
        int intensity = (pixel.getR() + pixel.getG() + pixel.getB()) / 3;
        intensityComponent.updatePixel(r, c,
            new Pixel(intensity, intensity, intensity, pixel.getA()));
      }
    }

    PixelInterface[][] ditheredPixels = new PixelInterface[height][width];

    int[][] pixelValues = new int[height][width];
    for (int r = 0; r < height; r++) {
      for (int c = 0; c < width; c++) {
        pixelValues[r][c] = intensityComponent.getPixel(r, c).getR();
      }
    }

    for (int r = 0; r < height; r++) {
      for (int c = 0; c < width; c++) {
        int oldColor = pixelValues[r][c];
        int newColor = (oldColor > 127) ? 255 : 0;
        int error = oldColor - newColor;
        ditheredPixels[r][c] = new Pixel(newColor, newColor, newColor, 255);

        if (c + 1 < width) {
          pixelValues[r][c + 1] += error * 7 / 16;
        }
        if (r + 1 < height) {
          if (c - 1 >= 0) {
            pixelValues[r + 1][c - 1] += error * 3 / 16;
          }
          pixelValues[r + 1][c] += error * 5 / 16;
          if (c + 1 < width) {
            pixelValues[r + 1][c + 1] += error * 1 / 16;
          }
        }
      }
    }

    ImageInterface ditheringImage = new Image(height, width);
    ditheringImage.imageFill(ditheredPixels);
    ImageInterface splitDitherImage = getSplitImage(originalImage, ditheringImage, splitPercent);
    model.storeImage(destName, splitDitherImage);
  }

  /**
   * Creates a split image combining the original and dithered versions. The split point is
   * determined by the splitPercent parameter.
   *
   * @param originalImage  The original image.
   * @param ditheringImage The dithered version of the image.
   * @param splitPercent   The percentage of the image width where the split should occur.
   * @return A new ImageInterface object representing the split image.
   */
  private ImageInterface getSplitImage(ImageInterface originalImage, ImageInterface ditheringImage,
      int splitPercent) {
    int height = originalImage.getHeight();
    int width = originalImage.getWidth();
    PixelInterface[][] splitPixels = new PixelInterface[height][width];

    int splitPoint = (width * splitPercent) / 100;

    for (int r = 0; r < height; r++) {
      for (int c = 0; c < width; c++) {
        PixelInterface originalPixel = originalImage.getPixel(r, c);
        PixelInterface ditheringPixel = ditheringImage.getPixel(r, c);

        if (c < splitPoint) {
          splitPixels[r][c] = new Pixel(ditheringPixel.getR(), ditheringPixel.getG(),
              ditheringPixel.getB(), originalPixel.getA());
        } else {
          splitPixels[r][c] = new Pixel(originalPixel.getR(), originalPixel.getG(),
              originalPixel.getB(), originalPixel.getA());
        }
      }
    }

    ImageInterface splitImage = new Image(height, width);
    splitImage.imageFill(splitPixels);
    return splitImage;
  }
}