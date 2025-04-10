package tucil2_13523045_13523052;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.Color;
import java.awt.image.BufferedImage;

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
				return Math.random() > 0.1;
			}
		}
		public static class MeanAbsoluteDeviation extends Controller {
			public MeanAbsoluteDeviation(double threshold) {
				super(threshold);
			}
			@Override
			public boolean shouldSplit(ImageBuffer image, int x, int y, int w, int h) {
				return false;
			}
		}
		public static class MaxPixelDifference extends Controller {
			public MaxPixelDifference(double threshold) {
				super(threshold);
			}
			@Override
			public boolean shouldSplit(ImageBuffer image, int x, int y, int w, int h) {
				return false;
			}
		}
		public static class Entropy extends Controller {
			public Entropy(double threshold) {
				super(threshold);
			}
			@Override
			public boolean shouldSplit(ImageBuffer image, int x, int y, int w, int h) {
				return false;
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
