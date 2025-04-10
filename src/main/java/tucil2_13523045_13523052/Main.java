package tucil2_13523045_13523052;

import java.io.File;

import javax.imageio.ImageIO;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
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
		var imageBuffer = Utils.readImageBuffer(new File(inputFile));
		var compressor = new ImageQuadTreeCompressor(imageBuffer, new ImageQuadTreeCompressor.Controller.Variance(quadTreeThreshold), minBlockSize);
		int step = 0;
		while(compressor.step()) {
			System.out.println("Step " + step);
			ImageIO.write(compressor.getCompressedImage(), "png", new File("out/step-" + step + ".png"));
			step++;
		}
		ImageIO.write(compressor.getCompressedImage(), "png", new File("out/step-" + step + ".png"));
		System.out.println("Done");
		System.exit(0);
	}
}
