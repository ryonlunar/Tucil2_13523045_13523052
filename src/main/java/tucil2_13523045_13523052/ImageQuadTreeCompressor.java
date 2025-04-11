package tucil2_13523045_13523052;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.list.primitive.MutableIntList;

public class ImageQuadTreeCompressor {
	public static abstract class Controller {
		public final double threshold;
		public Controller(double threshold) {
			this.threshold = threshold;
		}
		public abstract boolean shouldSplit(ImageBuffer image, ImageStatistics imageStatistics, int x, int y, int w, int h);

		public static class Variance extends Controller {
			public Variance(double threshold) {
				super(threshold);
			}
			@Override
			public boolean shouldSplit(ImageBuffer image, ImageStatistics imageStatistics, int x, int y, int w, int h) {
				int pixelCount = w * h;
				double meanR = imageStatistics.getAverageR(x, y, w, h);
				double meanG = imageStatistics.getAverageG(x, y, w, h);
				double meanB = imageStatistics.getAverageB(x, y, w, h);

				// Calculate variance
				double varianceR = 0;
				double varianceG = 0;
				double varianceB = 0;
				for(int j = y; j < y + h; j++) {
					for(int i = x; i < x + w; i++) {
						varianceR += Math.pow(image.getRed(i, j) - meanR, 2);
						varianceG += Math.pow(image.getGreen(i, j) - meanG, 2);
						varianceB += Math.pow(image.getBlue(i, j) - meanB, 2);
					}
				}
				varianceR /= pixelCount;
				varianceG /= pixelCount;
				varianceB /= pixelCount;
				double varRGB = (varianceR + varianceG + varianceB) / 3.0;

				// Check if variance is above threshold
				return varRGB > threshold;
			}
		}
		public static class MeanAbsoluteDeviation extends Controller {
			public MeanAbsoluteDeviation(double threshold) {
				super(threshold);
			}
			@Override
			public boolean shouldSplit(ImageBuffer image, ImageStatistics imageStatistics, int x, int y, int w, int h) {
				int pixelCount = w * h;
				double meanR = imageStatistics.getAverageR(x, y, w, h);
				double meanG = imageStatistics.getAverageG(x, y, w, h);
				double meanB = imageStatistics.getAverageB(x, y, w, h);

				// Calculate Mean Absolute Deviation
				double madR = 0;
				double madG = 0;
				double madB = 0;
				for(int j = y; j < y + h; j++) {
					for(int i = x; i < x + w; i++) {
						madR += Math.abs(image.getRed(i, j) - meanR);
						madG += Math.abs(image.getGreen(i, j) - meanG);
						madB += Math.abs(image.getBlue(i, j) - meanB);
					}
				}
				madR /= pixelCount;
				madG /= pixelCount;
				madB /= pixelCount;
				double madRGB = (madR + madG + madB) / 3.0;

				// Check if MAD is above threshold
				return madRGB > threshold;
			}
		}
		public static class MaxPixelDifference extends Controller {
			public MaxPixelDifference(double threshold) {
				super(threshold);
			}
			@Override
			public boolean shouldSplit(ImageBuffer image, ImageStatistics imageStatistics, int x, int y, int w, int h) {
				int maxR = 0;
				int maxG = 0;
				int maxB = 0;
				int minR = 255;
				int minG = 255;
				int minB = 255;

				// Calculate max and min values
				for(int j = y; j < y + h; j++) {
					for(int i = x; i < x + w; i++) {
						int r = image.getRed(i, j);
						int g = image.getGreen(i, j);
						int b = image.getBlue(i, j);
						maxR = Math.max(maxR, r);
						maxG = Math.max(maxG, g);
						maxB = Math.max(maxB, b);
						minR = Math.min(minR, r);
						minG = Math.min(minG, g);
						minB = Math.min(minB, b);
					}
				}
				double diffR = maxR - minR;
				double diffG = maxG - minG;
				double diffB = maxB - minB;
				double diffRGB = (diffR + diffG + diffB) / 3.0;

				// Calculate the maximum pixel difference
				return diffRGB > threshold;
			}
		}
		public static class Entropy extends Controller {
			public Entropy(double threshold) {
				super(threshold);
			}
			@Override
			public boolean shouldSplit(ImageBuffer image, ImageStatistics imageStatistics, int x, int y, int w, int h) {
				int pixelCount = w * h;
				int[] histR = new int[256];
				int[] histG = new int[256];
				int[] histB = new int[256];

				// Compute histogram
				for(int j = y; j < y + h; j++) {
					for(int i = x; i < x + w; i++) {
						int r = image.getRed(i, j);
						int g = image.getGreen(i, j);
						int b = image.getBlue(i, j);
						histR[r]++;
						histG[g]++;
						histB[b]++;
					}
				}

				// Compute Entropy per channel
				double entropyR = 0;
				double entropyG = 0;
				double entropyB = 0;
				for(int i = 0; i < 256; i++) {
					if(histR[i] > 0) {
						double p = (double) histR[i] / pixelCount;
						entropyR -= p * Math.log(p) / Math.log(2);
					}
					if(histG[i] > 0) {
						double p = (double) histG[i] / pixelCount;
						entropyG -= p * Math.log(p) / Math.log(2);
					}
					if(histB[i] > 0) {
						double p = (double) histB[i] / pixelCount;
						entropyB -= p * Math.log(p) / Math.log(2);
					}
				}

				double entropy = (entropyR + entropyG + entropyB) / 3.0;
				return entropy > threshold;
			}
		}
		public static class StructuralSimilarityIndex extends Controller {
			private static final double C1 = 6.5025;
			private static final double C2 = 58.5225;

			public StructuralSimilarityIndex(double threshold) {
				super(threshold);
			}

			private double computeSSIM(double muX, double sigmaX2, double muY, double sigmaXY) {
				double numerator = (2 * muX * muY + C1) * (2 * sigmaXY + C2);
				double denominator = (muX * muX + muY * muY + C1) * (sigmaX2 + sigmaXY + C2);
				return denominator == 0 ? 1 : numerator / denominator;
			}
			@Override
			public boolean shouldSplit(ImageBuffer image, ImageStatistics imageStatistics, int x, int y, int w, int h){
				int pixelCount = w * h;
				double meanR = imageStatistics.getAverageR(x, y, w, h);
				double meanG = imageStatistics.getAverageG(x, y, w, h);
				double meanB = imageStatistics.getAverageB(x, y, w, h);

				// Calculate variance and covariance
				double varR = 0;
				double varG = 0;
				double varB = 0;
				for(int j = y; j < y + h; j++) {
					for(int i = x; i < x + w; i++) {
						varR += Math.pow(image.getRed(i, j) - meanR, 2);
						varG += Math.pow(image.getGreen(i, j) - meanG, 2);
						varB += Math.pow(image.getBlue(i, j) - meanB, 2);
					}
				}
				varR /= pixelCount;
				varG /= pixelCount;
				varB /= pixelCount;

				// Karena membandingkan satu blok gambar dengan rata-ratanya sendiri maka, kovarians = 0
				double ssimR = computeSSIM(meanR, varR, meanR, 0);
				double ssimG = computeSSIM(meanG, varG, meanG, 0);
				double ssimB = computeSSIM(meanB, varB, meanB, 0);
				double ssim = (ssimR + ssimG + ssimB) / 3.0;

				// Check if SSIM too different from its average
				return ssim < threshold;
			}
		}
	}
	protected static class QuadTreeMetadata {
		protected final int color;
		public QuadTreeMetadata(int color) {
			this.color = color;
		}
	}

	protected final ImageBuffer image;
	protected final ImageStatistics imageStatistics;
	protected final Controller controller;
	protected final int minBlockSize;
	protected final Boundary2DQuadTree<QuadTreeMetadata> quadTree;
	protected final MutableIntList quadTreeIndexQueue;

	protected final BufferedImage compressionImage;
	protected final Graphics2D compressionGraphics;
	protected final MutableIntList drawCompressionQueue;

	public ImageQuadTreeCompressor(ImageBuffer image, Controller controller, int minBlockSize) {
		this.image = image;
		this.imageStatistics = new ImageStatistics(image, minBlockSize, minBlockSize);
		this.controller = controller;
		this.minBlockSize = minBlockSize;
		this.quadTree = new Boundary2DQuadTree<>(0, 0, image.getWidth(), image.getHeight(), minBlockSize, minBlockSize);
		this.quadTreeIndexQueue = IntLists.mutable.empty();
		this.quadTreeIndexQueue.add(0);

		this.compressionImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		this.compressionGraphics = this.compressionImage.createGraphics();
		this.drawCompressionQueue = IntLists.mutable.empty();
	}

	public boolean step() {
		MutableIntList queueCopy = quadTreeIndexQueue.toList();
		quadTreeIndexQueue.clear();
		boolean[] shouldSplitResults = new boolean[queueCopy.size()];
		int[] averageColorResults = new int[queueCopy.size()];
		int futureChunk = 64;
		int futureGroup = (queueCopy.size() + futureChunk - 1) / futureChunk;
		CompletableFuture<?>[] futures = new CompletableFuture[futureGroup];
		for(int i = 0; i < futureGroup; i++) {
			int startQueue = i * futureChunk;
			int endQueue = Math.min((i + 1) * futureChunk, queueCopy.size());
			futures[i] = CompletableFuture.runAsync(() -> {
				for(int j = startQueue; j < endQueue; j++) {
					int quadTreeIndex = queueCopy.get(j);
					int x = quadTree.getBoundaryX(quadTreeIndex);
					int y = quadTree.getBoundaryY(quadTreeIndex);
					int w = quadTree.getBoundaryW(quadTreeIndex);
					int h = quadTree.getBoundaryH(quadTreeIndex);
					shouldSplitResults[j] = w <= 1 || h <= 1 || w <= minBlockSize || h <= minBlockSize ? 
						false : controller.shouldSplit(image, imageStatistics, x, y, w, h);
					averageColorResults[j] = imageStatistics.getAverageColor(x, y, w, h);
				}
			});
		}
		for(int i = 0; i < futureGroup; i++) {
			int startQueue = i * futureChunk;
			int endQueue = Math.min((i + 1) * futureChunk, queueCopy.size());
			futures[i].join();
			for(int j = startQueue; j < endQueue; j++) {
				int quadTreeIndex = queueCopy.get(j);
				boolean shouldSplit = shouldSplitResults[j];
				int averageColor = averageColorResults[j];
				if(shouldSplit) {
					quadTree.split(quadTreeIndex);
					quadTreeIndexQueue.add(quadTree.getIndexTL(quadTreeIndex));
					quadTreeIndexQueue.add(quadTree.getIndexTR(quadTreeIndex));
					quadTreeIndexQueue.add(quadTree.getIndexBL(quadTreeIndex));
					quadTreeIndexQueue.add(quadTree.getIndexBR(quadTreeIndex));
				}
				quadTree.setMetadata(quadTreeIndex, new QuadTreeMetadata(averageColor));
				drawCompressionQueue.add(quadTreeIndex);
			}
		}
		return quadTreeIndexQueue.size() > 0;
	}
	public BufferedImage getCompressedImage() {
		drawCompressionQueue.reverseThis();
		while(drawCompressionQueue.size() > 0) {
			int quadTreeIndex = drawCompressionQueue.getLast();
			drawCompressionQueue.removeAtIndex(drawCompressionQueue.size() - 1);
			int x = quadTree.getBoundaryX(quadTreeIndex);
			int y = quadTree.getBoundaryY(quadTreeIndex);
			int w = quadTree.getBoundaryW(quadTreeIndex);
			int h = quadTree.getBoundaryH(quadTreeIndex);
			QuadTreeMetadata metadata = quadTree.getMetadata(quadTreeIndex);
			compressionGraphics.setColor(new Color(metadata.color, true));
			compressionGraphics.fillRect(x, y, w, h);
		}
		return compressionImage;
	}

	public int getNodeCount() {
		return quadTree.getNodeCount();
	}
	public int getTreeDepth() {
		return quadTree.getMaxDepth();
	}
	public int getTotalLeafArea() {
		return quadTree.getTotalLeafArea();
	}
}
