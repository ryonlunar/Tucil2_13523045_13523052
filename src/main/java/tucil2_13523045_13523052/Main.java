package tucil2_13523045_13523052;

import java.io.File;

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
			var quadTreeMethod = QuadTreeMethod.valueOf(cmd.getOptionValue("method"));
			var quadTreeThreshold = Double.parseDouble(cmd.getOptionValue("threshold"));
			var minBlockSize = Integer.parseInt(cmd.getOptionValue("block"));
			entryCLI(inputFile, quadTreeMethod, quadTreeThreshold, minBlockSize);
		} catch(ParseException e) {
			System.out.println("Error: " + e.getMessage());
			new HelpFormatter().printHelp("java -jar " + Utils.getJarName().orElse("Tucil2_13523045_13523052.jar"), mainOptions);
			System.exit(1);
		}
	}
	public static void entryCLI(String inputFile, QuadTreeMethod quadTreeMethod, double quadTreeThreshold, int minBlockSize) throws Exception {
		long startTime = System.nanoTime();
		File file = new File(inputFile);
		long originalFileSize = file.length();
		if (!file.exists()) {
			throw new IllegalArgumentException("Input file not found: " + inputFile);
		}

		String format = inputFile.substring(inputFile.lastIndexOf('.') + 1).toLowerCase();
		if (!format.matches("png|jpg|jpeg|bmp")) {
			throw new IllegalArgumentException("Unsupported image format: " + format);
		}

		var imageBuffer = Utils.readImageBuffer(file);
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

		var compressor = new ImageQuadTreeCompressor(imageBuffer, controller, minBlockSize);

		// Somehow JPG gabisa jir watdehel
		// System.out.println("File name: " + "out/step-" + step + "." + format); debugging purpose
		int step = 0;
		while(compressor.step()) {
			System.out.println("Step " + step);
			ImageIO.write(compressor.getCompressedImage(), format, new File("out/step-" + step + "." + format));
			step++;
		}
		System.out.println("Step " + step);
		ImageIO.write(compressor.getCompressedImage(), format, new File("out/step-" + step + "." + format));
		String finalImagePath = "out/step-" + step + "." + format;

		long endTime = System.nanoTime();
		double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;

		int nodeCount = compressor.getNodeCount();
		int depth = compressor.getTreeDepth();
		long compressedSize = new File(finalImagePath).length();
		double compressionRatio = ((1.0 - (double) compressedSize / originalFileSize) * 100);

		System.out.println("========== OUTPUT ==========");
		System.out.printf("Waktu eksekusi: %.3f detik\n", elapsedSeconds);
		System.out.println("Ukuran gambar sebelum: " + originalFileSize + " byte");
		System.out.println("Ukuran hasil kompresi: " + compressedSize + " byte");
		System.out.printf("Persentase kompresi: %.2f%%\n", compressionRatio);
		System.out.println("Kedalaman pohon: " + depth);
		System.out.println("Jumlah simpul pohon: " + nodeCount);
		System.out.println("Gambar hasil kompresi terakhir: " + finalImagePath);
		System.exit(0);
	}
}
