package tucil2_13523045_13523052;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

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

	protected static final Field FIELD_IntArrayList_size;
	static {
		try {
			FIELD_IntArrayList_size = IntArrayList.class.getDeclaredField("size");
			FIELD_IntArrayList_size.setAccessible(true);
		} catch(Exception e) {
			throw new Error(e);
		}
	}
	public static void growMutableIntList(MutableIntList list, int size) {
		IntArrayList arrayList = (IntArrayList) list;
		size = Math.max(size, arrayList.size());
		arrayList.ensureCapacity(size);
		try {
			FIELD_IntArrayList_size.setInt(arrayList, size);
		} catch(IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	public static void saveCompressed(BufferedImage image, File outputFile, String format, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if(!writers.hasNext()) throw new IllegalStateException("No " + format + " writers found");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if(param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        try(ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        }
        writer.dispose();
    }
}
