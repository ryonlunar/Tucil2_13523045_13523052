package tucil2_13523045_13523052;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.imageio.ImageIO;

public class Utils {
	private Utils() {
	}

	public static Optional<String> getJarName() {
		try {
			String path = Utils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			return Optional.of(new File(path).getName());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}
	public static ImageBuffer readImageBuffer(File file) throws IOException {
		var image = ImageIO.read(file);
		var width = image.getWidth();
		var height = image.getHeight();
		var buffer = new int[width * height];
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				var pixel = image.getRGB(x, y);
				buffer[y * width + x] = pixel;
			}
		}
		return new ImageBuffer(width, height, buffer);
	}
	public static long[] getPrecomputeImageIntegral(ImageBuffer image, int band) {
		long[] sum = new long[image.getWidth() * image.getHeight()];
		for(int y = 0; y < image.getHeight(); y++) {
			long rowSum = 0;
			for(int x = 0; x < image.getWidth(); x++) {
				rowSum += band == 0 ? image.getRed(x, y) : 
					band == 1 ? image.getGreen(x, y) : 
					band == 2 ? image.getBlue(x, y) : 
					band == 3 ? image.getAlpha(x, y) : 
					0;
				sum[y * image.getWidth() + x] = rowSum + (y > 0 ? sum[(y - 1) * image.getWidth() + x] : 0);
			}
		}
		return sum;
	}
	public static int getImageIntegralAverageColor(long[] imageIntegral, int iw, int ih, int x, int y, int w, int h) {
		if(x < 0 || x + w > iw || y < 0 || y + h > ih)
			throw new IndexOutOfBoundsException();
		long total = imageIntegral[(y + h - 1) * iw + (x + w - 1)];
		if(x > 0) total -= imageIntegral[(y + h - 1) * iw + x - 1];
		if(y > 0) total -= imageIntegral[(y - 1) * iw + (x + w - 1)];
		if(x > 0 && y > 0) total += imageIntegral[(y - 1) * iw + x - 1];
		return (int) (total / (w * h));
	}
}
