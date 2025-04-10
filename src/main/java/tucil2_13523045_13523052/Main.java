package tucil2_13523045_13523052;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {
	public enum QuadTreeMethod {
		Variance,
		MeanAbsoluteDeviation,
		MaxPixelDifference,
		Entropy;
	}
	public static void main(String... args) throws Exception {
		var basicOptions = new Options();
		basicOptions.addOption(Option.builder("h").longOpt("help").desc("Shows help message").build());
		var mainOptions = new Options();
		mainOptions.addOption(Option.builder("i").longOpt("input").hasArg().required().desc("Input image file").build());
		mainOptions.addOption(Option.builder("o").longOpt("output").hasArg().required().desc("Output image file").build());
		mainOptions.addOption(Option.builder("m").longOpt("method").hasArg().required().desc("QuadTree compression method").build());
		mainOptions.addOption(Option.builder("t").longOpt("threshold").hasArg().required().desc("QuadTree compression threshold").build());
		mainOptions.addOption(Option.builder("b").longOpt("block").hasArg().required().desc("Minimum block size").build());
		for(var basicOption : basicOptions.getOptions())
			mainOptions.addOption(basicOption);
		try {
			var basicOptionsParser = new DefaultParser().parse(basicOptions, args, true);
			if(basicOptionsParser.hasOption("help")) {
				new HelpFormatter().printHelp("java -jar " + Utils.getJarName().orElse("Tucil2_13523045_13523052.jar"), mainOptions);
				System.exit(0);
			}
		} catch(ParseException e) {
			System.out.println("Error: " + e.getMessage());
			new HelpFormatter().printHelp("java -jar " + Utils.getJarName().orElse("Tucil2_13523045_13523052.jar"), mainOptions);
			System.exit(1);
		}
		try {
			var cmd = new DefaultParser().parse(mainOptions, args, false);
			var inputFile = cmd.getOptionValue("input");
			var outputFile = cmd.getOptionValue("input");
			var quadTreeMethod = QuadTreeMethod.valueOf(cmd.getOptionValue("method"));
			var quadTreeThreshold = Double.parseDouble(cmd.getOptionValue("threshold"));
			var minBlockSize = Integer.parseInt(cmd.getOptionValue("block"));
			entryCLI(inputFile, outputFile, quadTreeMethod, quadTreeThreshold, minBlockSize);
		} catch(ParseException e) {
			System.out.println("Error: " + e.getMessage());
			new HelpFormatter().printHelp("java -jar " + Utils.getJarName().orElse("Tucil2_13523045_13523052.jar"), mainOptions);
			System.exit(1);
		}
	}
	public static void entryCLI(String inputFile, String outputFile, QuadTreeMethod quadTreeMethod, double quadTreeThreshold, int minBlockSize) throws Exception {
		File file = new File(inputFile);
		if(!file.exists())
			throw new IllegalArgumentException("Input file not found: " + inputFile);
		String format = inputFile.substring(inputFile.lastIndexOf('.') + 1).toLowerCase();
		if(!format.matches("png|jpg|jpeg|bmp"))
			throw new IllegalArgumentException("Unsupported image format: " + format);

		var image = Utils.readImageBuffer(file);
		ImageQuadTreeCompressor.Controller controller = null;
		switch(quadTreeMethod) {
			case Variance:
				controller = new ImageQuadTreeCompressor.Controller.Variance(quadTreeThreshold);
				break;
			case MeanAbsoluteDeviation:
				controller = new ImageQuadTreeCompressor.Controller.MeanAbsoluteDeviation(quadTreeThreshold);
				break;
			case MaxPixelDifference:
				controller = new ImageQuadTreeCompressor.Controller.MaxPixelDifference(quadTreeThreshold);
				break;
			case Entropy:
				controller = new ImageQuadTreeCompressor.Controller.Entropy(quadTreeThreshold);
				break;
		}

		long constructStart = System.nanoTime();
		var compressor = new ImageQuadTreeCompressor(image, controller, minBlockSize);
		var workingImage = new BufferedImage(image.getWidth(), image.getHeight(), format == "png" ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		var workingGraphics = workingImage.createGraphics();
		System.out.printf("Precompute done in %.2fms\n", (float) (System.nanoTime() - constructStart) / 1000000);
		CompletableFuture<?> lastSave = CompletableFuture.completedFuture(null);

		int step = 1;
		long loopTotalDuration = 0;
		long[] saveTotalDuration = { 0 };
		while(true) {
			int currentStep = step;
			long loopStart = System.nanoTime();
			boolean loop = compressor.step();
			long loopDuration = System.nanoTime() - loopStart;
			loopTotalDuration += loopDuration;
			System.out.printf("Step %d done in %.2fms\n", currentStep, (float) loopDuration / 1000000);
			// Do not call `getCompressedImage` unless the last save is done. Whenever `getCompressedImage` gets called
			// it has a side effect of rendering the queued draw.
			lastSave.join();
			var compressedImage = compressor.getCompressedImage();
			lastSave = CompletableFuture.supplyAsync(() -> {
				workingGraphics.drawImage(compressedImage, 0, 0, workingImage.getWidth(), workingImage.getHeight(), 0, 0, compressedImage.getWidth(), compressedImage.getHeight(), null);
				try {
					long saveStart = System.nanoTime();
					ImageIO.write(workingImage, format, new File("out/step-" + currentStep + "." + format));
					long saveDuration = System.nanoTime() - saveStart;
					saveTotalDuration[0] += saveDuration;
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
				return null;
			}, Utils.executorService);
			if(!loop) break;
			step++;
		}
		lastSave.join();

		String finalImagePath = "out/step-" + (step - 1) + "." + format;
		long originalFileSize = file.length();
		int nodeCount = compressor.getNodeCount();
		int depth = compressor.getTreeDepth();
		long compressedSize = new File(finalImagePath).length();
		double compressionRatio = ((1.0 - (double) compressedSize / originalFileSize) * 100);
		System.out.printf("========== OUTPUT ==========");
		System.out.printf("Waktu eksekusi: %.2fms (+waktu I/O %.2fms)\n", (float) loopTotalDuration / 1000000, (float) saveTotalDuration[0] / 1000000);
		System.out.printf("Ukuran gambar sebelum: %d bytes\n", originalFileSize);
		System.out.printf("Ukuran hasil kompresi: %d bytes\n", compressedSize);
		System.out.printf("Persentase kompresi: %.2f%%\n", compressionRatio);
		System.out.printf("Kedalaman pohon: %d\n", depth);
		System.out.printf("Jumlah simpul pohon: %d\n", nodeCount);
		System.out.printf("Gambar hasil kompresi terakhir: %d\n", finalImagePath);
		System.exit(0);
	}
}
