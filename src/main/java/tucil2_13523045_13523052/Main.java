package tucil2_13523045_13523052;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

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
		mainOptions.addOption(Option.builder("g").longOpt("gif").desc("Generate gif animation").build());
		mainOptions.addOption(Option.builder("v").longOpt("verbose").desc("Output verbose").build());
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
			var outputFile = cmd.getOptionValue("output");
			var quadTreeMethod = QuadTreeMethod.valueOf(cmd.getOptionValue("method"));
			var quadTreeThreshold = Double.parseDouble(cmd.getOptionValue("threshold"));
			var minBlockSize = Integer.parseInt(cmd.getOptionValue("block"));
			var generateGif = cmd.hasOption("gif");
			var verbose = cmd.hasOption("verbose");
			entryCLI(inputFile, outputFile, quadTreeMethod, quadTreeThreshold, minBlockSize, generateGif, verbose);
		} catch(ParseException e) {
			System.out.println("Error: " + e.getMessage());
			new HelpFormatter().printHelp("java -jar " + Utils.getJarName().orElse("Tucil2_13523045_13523052.jar"), mainOptions);
			System.exit(1);
		}
	}
	public static void entryCLI(String inputFile, String outputFile, QuadTreeMethod quadTreeMethod, double quadTreeThreshold, int minBlockSize, boolean generateGif, boolean verbose) throws Exception {
		File file = new File(inputFile).getAbsoluteFile();
		if(!file.exists())
			throw new IllegalArgumentException("Input file not found: " + inputFile);
		String format = inputFile.substring(inputFile.lastIndexOf('.') + 1).toLowerCase();
		if(!format.matches("png|jpg|jpeg|bmp"))
			throw new IllegalArgumentException("Unsupported image format: " + format);
		File outputDirectory = new File(outputFile).getAbsoluteFile();
		if(!outputDirectory.exists())
			outputDirectory.mkdirs();
		if(!outputDirectory.exists() || !outputDirectory.isDirectory())
			throw new IllegalArgumentException("Cannot create output directory");

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
		System.out.printf("Precompute done in %.2fms\n", (float) (System.nanoTime() - constructStart) / 1000000);
		var saveFutures = new ArrayList<CompletableFuture<?>>();
		var gifFile = generateGif ? new File(outputDirectory, "output.gif") : null;
		var gifWriter = generateGif ? new GifSequenceWriter(ImageIO.createImageOutputStream(gifFile), BufferedImage.TYPE_INT_RGB, 150, true) : null;
		var gifFutures = generateGif ? Collections.synchronizedList(new ArrayList<CompletableFuture<?>>()) : null;
		var gifSequenceFuture = generateGif ? CompletableFuture.completedFuture(null) : null;
		var bufferImageQueue = new LinkedBlockingQueue<Pair<BufferedImage, Graphics2D>>();
		var getBufferImage = (Supplier<Pair<BufferedImage, Graphics2D>>) () -> {
			var result = bufferImageQueue.poll();
			if(result == null) {
				var workingImage = new BufferedImage(image.getWidth(), image.getHeight(), format == "png" ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
				var workingGraphics = workingImage.createGraphics();
				result = Tuples.pair(workingImage, workingGraphics);
			}
			return result;
		};

		int step = 1;
		long loopTotalDuration = 0;
		AtomicLong saveTotalDuration = new AtomicLong(0);
		AtomicLong gifTotalDuration = generateGif ? new AtomicLong(0) : null;
		while(true) {
			int currentStep = step;
			long loopStart = System.nanoTime();
			boolean loop = compressor.step();
			long loopDuration = System.nanoTime() - loopStart;
			loopTotalDuration += loopDuration;
			System.out.printf("Step %d done in %.2fms\n", currentStep, (float) loopDuration / 1000000);
			if(generateGif || verbose) {
				var compressedImage = compressor.getCompressedImage();
				var pair = getBufferImage.get();
				var lastGifSequenceFuture = generateGif ? gifSequenceFuture : null;
				var myGifSequenceFuture = generateGif ? new CompletableFuture<>() : null;
				gifSequenceFuture = myGifSequenceFuture;
				pair.getTwo().drawImage(compressedImage, 0, 0, compressedImage.getWidth(), compressedImage.getHeight(), null);
				saveFutures.add(CompletableFuture.supplyAsync(() -> {
					var returnAllowed = new AtomicInteger(0);
					try {
						if(generateGif) {
							gifFutures.add(lastGifSequenceFuture.thenApply(__ -> {
								try {
									long gifStart = System.nanoTime();
									gifWriter.writeToSequence(pair.getOne());
									long gifDuration = System.nanoTime() - gifStart;
									gifTotalDuration.addAndGet(gifDuration);
								} catch(IOException e) {
									myGifSequenceFuture.completeExceptionally(e);
									throw new RuntimeException(e);
								} finally {
									if(returnAllowed.incrementAndGet() == 2) {
										try {
											bufferImageQueue.put(pair);
										} catch(InterruptedException e) {
											myGifSequenceFuture.completeExceptionally(e);
											throw new RuntimeException(e);
										}
									}
								}
								myGifSequenceFuture.complete(null);
								return null;
							}));
						}
						long saveStart = System.nanoTime();
						ImageIO.write(pair.getOne(), format, new File(outputDirectory, "step-" + currentStep + "." + format));
						long saveDuration = System.nanoTime() - saveStart;
						saveTotalDuration.addAndGet(saveDuration);
					} catch(IOException e) {
						throw new RuntimeException(e);
					} finally {
						if(returnAllowed.incrementAndGet() == (generateGif ? 2 : 1)) {
							try {
								bufferImageQueue.put(pair);
							} catch(InterruptedException e) {
								throw new RuntimeException(e);
							}
						}
					}
					return null;
				}, Utils.executorService));
			}
			if(!loop) break;
			step++;
		}
		File finalImageFile = new File(outputDirectory, "output." + format);
		if(generateGif || verbose) {
			File stepLastFile = new File(outputDirectory, "step-" + step + "." + format);
			for(var saveFuture : saveFutures)
				saveFuture.join();
			saveFutures.add(CompletableFuture.runAsync(() -> {
				try {
					long saveStart = System.nanoTime();
					Files.copy(stepLastFile.toPath(), finalImageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					long saveDuration = System.nanoTime() - saveStart;
					saveTotalDuration.addAndGet(saveDuration);
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}, Utils.executorService));
			if(generateGif) {
				for(var gifFuture : gifFutures)
					gifFuture.join();
				saveFutures.add(CompletableFuture.runAsync(() -> {
					try {
						long gifStart = System.nanoTime();
						gifWriter.close();
						long gifDuration = System.nanoTime() - gifStart;
						gifTotalDuration.addAndGet(gifDuration);
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				}, Utils.executorService));
			}
		} else {
			var compressedImage = compressor.getCompressedImage();
			var pair = getBufferImage.get();
			pair.getTwo().drawImage(compressedImage, 0, 0, compressedImage.getWidth(), compressedImage.getHeight(), null);
			saveFutures.add(CompletableFuture.supplyAsync(() -> {
				try {
					long saveStart = System.nanoTime();
					ImageIO.write(pair.getOne(), format, finalImageFile);
					long saveDuration = System.nanoTime() - saveStart;
					saveTotalDuration.addAndGet(saveDuration);
				} catch(IOException e) {
					throw new RuntimeException(e);
				} finally {
					try {
						bufferImageQueue.put(pair);
					} catch(InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
				return null;
			}, Utils.executorService));
		}
		for(var saveFuture : saveFutures)
			saveFuture.join();

		long originalFileSize = file.length();
		int nodeCount = compressor.getNodeCount();
		int depth = compressor.getTreeDepth();
		long compressedSize = finalImageFile.length();
		double compressionRatio = ((1.0 - (double) compressedSize / originalFileSize) * 100);
		System.out.printf("========== OUTPUT ==========\n");
		if(generateGif)
			System.out.printf("Waktu eksekusi: %.2fms (+waktu I/O %.2fms) (+waktu generate GIF %.2fms)\n", (float) loopTotalDuration / 1000000, (float) saveTotalDuration.get() / 1000000, (float) gifTotalDuration.get() / 1000000);
		else
			System.out.printf("Waktu eksekusi: %.2fms (+waktu I/O %.2fms)\n", (float) loopTotalDuration / 1000000, (float) saveTotalDuration.get() / 1000000);
		System.out.printf("Ukuran gambar sebelum: %d bytes\n", originalFileSize);
		System.out.printf("Ukuran hasil kompresi: %d bytes\n", compressedSize);
		System.out.printf("Persentase kompresi: %.2f%%\n", compressionRatio);
		System.out.printf("Kedalaman pohon: %d\n", depth);
		System.out.printf("Jumlah simpul pohon: %d\n", nodeCount);
		var cwdUri = new File("").getAbsoluteFile().toURI();
		System.out.printf("Gambar hasil kompresi terakhir: %s\n", finalImageFile.toURI().relativize(cwdUri).getPath());
		if(generateGif)
			System.out.printf("Gambar hasil GIF: %s\n", gifFile.toURI().relativize(cwdUri).getPath());
		System.exit(0);
	}
}
