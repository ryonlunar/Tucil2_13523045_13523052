package tucil2_13523045_13523052;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.list.primitive.MutableIntList;

public class ImageQuadTreeCompressor {
	public static abstract class Controller {
		public final double threshold;
		public Controller(double threshold) {
			this.threshold = threshold;
		}
		public abstract boolean shouldSplit(ImageBuffer image, int x, int y, int w, int h);

		public static class Variance extends Controller {
			public Variance(double threshold) {
				super(threshold);
			}
			@Override
			public boolean shouldSplit(ImageBuffer image, int x, int y, int w, int h) {
				if(w < 1 || h < 1) return false; // Ganti sama minBlockSize ??
				int sumR = 0, sumG = 0, sumB = 0;
				int pixelCount = w * h;

				// Calculate mean
				for(int j = y; j < y + h; j++) {
					for(int i = x; i < x + w; i++) {
						sumR += image.getRed(i, j);
						sumG += image.getGreen(i, j);
						sumB += image.getBlue(i, j);
					}
				}

				double meanR = sumR / (double) pixelCount;
				double meanG = sumG / (double) pixelCount;
				double meanB = sumB / (double) pixelCount;

				// Calculate variance
				double varianceR = 0, varianceG = 0, varianceB = 0;
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
			public boolean shouldSplit(ImageBuffer image, int x, int y, int w, int h) {
				if(w < 1 || h < 1) return false;
				int sumR = 0, sumG = 0, sumB = 0;
				int pixelCount = w * h;

				// Calculate Mean
				for(int j = y; j < y + h; j++) {
					for(int i = x; i < x + w; i++) {
						sumR += image.getRed(i, j);
						sumG += image.getGreen(i, j);
						sumB += image.getBlue(i, j);
					}
				}

				double meanR = sumR / (double) pixelCount;
				double meanG = sumG / (double) pixelCount;	
				double meanB = sumB / (double) pixelCount;

				// Calculate Mean Absolute Deviation
				double madR = 0, madG = 0, madB = 0;
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
			public boolean shouldSplit(ImageBuffer image, int x, int y, int w, int h) {
				if(w < 1 || h < 1) return false;
				int maxR = 0, maxG = 0, maxB = 0;
				int minR = 255, minG = 255, minB = 255;

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
			public boolean shouldSplit(ImageBuffer image, int x, int y, int w, int h) {
				if(w < 1 || h < 1) return false;
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
				double entropyR = 0, entropyG = 0, entropyB = 0;
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
			public boolean shouldSplit(ImageBuffer image, int x, int y, int w, int h){
				if(w < 1 || h < 1) return false;
				int pixelCount = w * h;
				double meanR = 0, meanG = 0, meanB = 0;

				// Calculate mean
				for(int j = y; j < y + h; j++) {
					for(int i = x; i < x + w; i++) {
						meanR += image.getRed(i, j);
						meanG += image.getGreen(i, j);
						meanB += image.getBlue(i, j);
					}
				}
				meanR /= pixelCount;
				meanG /= pixelCount;
				meanB /= pixelCount;

				// Calculate variance and covariance
				double varR = 0, varG = 0, varB = 0;
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
	protected static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	protected final ImageBuffer image;
	protected final long[] imageIntegralR;
	protected final long[] imageIntegralG;
	protected final long[] imageIntegralB;
	protected final long[] imageIntegralA;
	protected final Controller controller;
	protected final int minBlockSize;
	protected final Boundary2DQuadTree<QuadTreeMetadata> quadTree;
	protected final MutableIntList quadTreeIndexQueue;

	public ImageQuadTreeCompressor(ImageBuffer image, Controller controller, int minBlockSize) {
		this.image = image;
		this.imageIntegralR = Utils.getPrecomputeImageIntegral(image, 0);
		this.imageIntegralG = Utils.getPrecomputeImageIntegral(image, 1);
		this.imageIntegralB = Utils.getPrecomputeImageIntegral(image, 2);
		this.imageIntegralA = Utils.getPrecomputeImageIntegral(image, 3);
		this.controller = controller;
		this.minBlockSize = minBlockSize;
		this.quadTree = new Boundary2DQuadTree<>(0, 0, image.getWidth(), image.getHeight());
		this.quadTreeIndexQueue = IntLists.mutable.empty();
		this.quadTreeIndexQueue.add(0);
	}

	public boolean step() {
		MutableIntList queueCopy = quadTreeIndexQueue.toList();
		quadTreeIndexQueue.clear();
		CompletableFuture<Boolean>[] shouldSplitFutures = new CompletableFuture[queueCopy.size()];
		CompletableFuture<Integer>[] averageColorFutures = new CompletableFuture[queueCopy.size()];
		for(int i = 0; i < queueCopy.size(); i++) {
			int quadTreeIndex = queueCopy.get(i);
			int x = quadTree.getBoundaryX(quadTreeIndex);
			int y = quadTree.getBoundaryY(quadTreeIndex);
			int w = quadTree.getBoundaryW(quadTreeIndex);
			int h = quadTree.getBoundaryH(quadTreeIndex);

			if (w <= 1 || h <= 1 || w <= minBlockSize || h <= minBlockSize) {
				averageColorFutures[i] = CompletableFuture.supplyAsync(() ->
					getImageAverageColor(x, y, w, h), executorService);
				shouldSplitFutures[i] = CompletableFuture.completedFuture(false);
				continue;
			}

			shouldSplitFutures[i] = CompletableFuture.supplyAsync(() -> controller.shouldSplit(image, x, y, w, h), executorService);
			averageColorFutures[i] = CompletableFuture.supplyAsync(() -> getImageAverageColor(x, y, w, h), executorService);
		}
		for(int i = 0; i < queueCopy.size(); i++) {
			int quadTreeIndex = queueCopy.get(i);
			boolean shouldSplit = shouldSplitFutures[i].join();
			// TODO minBlockSize
			if(!shouldSplit) continue;
			quadTree.split(quadTreeIndex);
			quadTreeIndexQueue.add(quadTree.getIndexTL(quadTreeIndex));
			quadTreeIndexQueue.add(quadTree.getIndexTR(quadTreeIndex));
			quadTreeIndexQueue.add(quadTree.getIndexBL(quadTreeIndex));
			quadTreeIndexQueue.add(quadTree.getIndexBR(quadTreeIndex));
		}
		for(int i = 0; i < queueCopy.size(); i++) {
			int quadTreeIndex = queueCopy.get(i);
			int color = averageColorFutures[i].join();
			quadTree.setMetadata(quadTreeIndex, new QuadTreeMetadata(color));
		}
		return quadTreeIndexQueue.size() > 0;
	}
	protected int getImageAverageColor(int x, int y, int w, int h) {
		int r = Utils.getImageIntegralAverageColor(imageIntegralR, image.getWidth(), image.getHeight(), x, y, w, h);
		int g = Utils.getImageIntegralAverageColor(imageIntegralG, image.getWidth(), image.getHeight(), x, y, w, h);
		int b = Utils.getImageIntegralAverageColor(imageIntegralB, image.getWidth(), image.getHeight(), x, y, w, h);
		int a = Utils.getImageIntegralAverageColor(imageIntegralA, image.getWidth(), image.getHeight(), x, y, w, h);
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}
	public BufferedImage getCompressedImage() {
		var compressedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		var graphics = compressedImage.getGraphics();
		var queue = IntLists.mutable.empty();
		queue.add(0);
		while(queue.size() > 0) {
			int quadTreeIndex = queue.get(0);
			queue.removeAtIndex(0);
			int x = quadTree.getBoundaryX(quadTreeIndex);
			int y = quadTree.getBoundaryY(quadTreeIndex);
			int w = quadTree.getBoundaryW(quadTreeIndex);
			int h = quadTree.getBoundaryH(quadTreeIndex);
			QuadTreeMetadata metadata = quadTree.getMetadata(quadTreeIndex);
			if(metadata == null) continue;
			graphics.setColor(new Color(metadata.color, true));
			graphics.fillRect(x, y, w, h);
			int indexTL = quadTree.getIndexTL(quadTreeIndex);
			int indexTR = quadTree.getIndexTR(quadTreeIndex);
			int indexBL = quadTree.getIndexBL(quadTreeIndex);
			int indexBR = quadTree.getIndexBR(quadTreeIndex);
			if(indexTL != -1) queue.add(indexTL);
			if(indexTR != -1) queue.add(indexTR);
			if(indexBL != -1) queue.add(indexBL);
			if(indexBR != -1) queue.add(indexBR);
		}
		return compressedImage;
	}
}
