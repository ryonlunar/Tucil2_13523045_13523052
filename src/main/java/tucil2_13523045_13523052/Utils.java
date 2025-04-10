package tucil2_13523045_13523052;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

public class Utils {
	private Utils() {
	}

	public static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
	public static int packARGB(int r, int g, int b, int a) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}
}
