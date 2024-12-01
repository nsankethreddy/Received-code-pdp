package ime.model;

import java.util.List;

public class ImageDither extends AbstractFilters {

  private final ModelInterface model;
  private final ImageTransformation transformation;

  public ImageDither(ModelInterface model) {
    this.model = model;
    this.transformation = new ImageTransformation();
  }

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