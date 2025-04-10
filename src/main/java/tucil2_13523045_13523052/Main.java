package tucil2_13523045_13523052;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
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
		var options = new Options();
		options.addOption("i", "input", true, "Input image file");
		options.addOption("m", "method", true, "QuadTree compression method");
		options.addOption("t", "threshold", true, "QuadTree compression threshold");
		options.addOption("b", "block", true, "Minimum block size");
		options.addOption("h", "help", false, "Shows help message");
		try {
			var cmd = new DefaultParser().parse(options, args);
			if(cmd.hasOption("help")) {
				new HelpFormatter().printHelp(Utils.getCommandLine().orElse("java -jar Tucil2_13523045_13523052.jar"), options);
				System.exit(0);
			}
			var inputFile = cmd.getOptionValue("input");
			var quadTreeMethod = QuadTreeMethod.valueOf(cmd.getOptionValue("method"));
			var quadTreeThreshold = Double.parseDouble(cmd.getOptionValue("threshold"));
			var minBlockSize = Integer.parseInt(cmd.getOptionValue("block"));
			entryCLI(inputFile, quadTreeMethod, quadTreeThreshold, minBlockSize);
		} catch(ParseException e) {
			System.out.println("Error: " + e.getMessage());
			new HelpFormatter().printHelp(Utils.getCommandLine().orElse("java -jar Tucil2_13523045_13523052.jar"), options);
			System.exit(1);
		}
	}
	public static void entryCLI(String inputFile, QuadTreeMethod quadTreeMethod, double quadTreeThreshold, int minBlockSize) throws Exception {
		var imageBuffer = Utils.readImageBuffer(new File(inputFile));
		var quadTree = new Boundary2DQuadTree(0, 0, imageBuffer.getWidth(), imageBuffer.getHeight());
	}
}
