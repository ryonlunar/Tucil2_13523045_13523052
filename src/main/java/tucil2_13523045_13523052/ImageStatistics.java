package tucil2_13523045_13523052;

import java.util.concurrent.CompletableFuture;

public class ImageStatistics {
	protected final ImageBuffer image;
	protected final int integralBlockX;
	protected final int integralBlockY;
	protected final Integral integralR;
	protected final Integral integralG;
	protected final Integral integralB;
	protected final Integral integralA;

	public ImageStatistics(ImageBuffer image, int integralBlockX, int integralBlockY) {
		this.image = image;
		this.integralBlockX = integralBlockX;
		this.integralBlockY = integralBlockY;
		CompletableFuture<Integral> futureIntegralR = CompletableFuture.supplyAsync(() -> constructIntegral(image, 0, integralBlockX, integralBlockY));
		CompletableFuture<Integral> futureIntegralG = CompletableFuture.supplyAsync(() -> constructIntegral(image, 1, integralBlockX, integralBlockY));
		CompletableFuture<Integral> futureIntegralB = CompletableFuture.supplyAsync(() -> constructIntegral(image, 2, integralBlockX, integralBlockY));
		CompletableFuture<Integral> futureIntegralA = CompletableFuture.supplyAsync(() -> constructIntegral(image, 3, integralBlockX, integralBlockY));
		this.integralR = futureIntegralR.join();
		this.integralG = futureIntegralG.join();
		this.integralB = futureIntegralB.join();
		this.integralA = futureIntegralA.join();
	}

	public float getAverageR(int x, int y, int w, int h) {
		return Math.min(255, (float) integralR.getIntegral(x, y, w, h) / (w * h));
	}
	public float getAverageG(int x, int y, int w, int h) {
		return Math.min(255, (float) integralG.getIntegral(x, y, w, h) / (w * h));
	}
	public float getAverageB(int x, int y, int w, int h) {
		return Math.min(255, (float) integralB.getIntegral(x, y, w, h) / (w * h));
	}
	public float getAverageA(int x, int y, int w, int h) {
		return Math.min(255, (float) integralA.getIntegral(x, y, w, h) / (w * h));
	}
	public int getAverageColor(int x, int y, int w, int h) {
		return Utils.packARGB(
			(int) getAverageR(x, y, w, h),
			(int) getAverageG(x, y, w, h),
			(int) getAverageB(x, y, w, h),
			(int) getAverageA(x, y, w, h)
		);
	}

	public float getVarianceR(int x, int y, int w, int h) {
		float mean = getAverageR(x, y, w, h);
		float result = 0;
		for(int j = y; j < y + h; j++) {
			for(int i = x; i < x + w; i++) {
				result += Math.pow(image.getRed(i, j) - mean, 2);
			}
		}
		result /= (w * h);
		return result;
	}

	public float getVarianceG(int x, int y, int w, int h) {
		float mean = getAverageG(x, y, w, h);
		float result = 0;
		for(int j = y; j < y + h; j++) {
			for(int i = x; i < x + w; i++) {
				result += Math.pow(image.getGreen(i, j) - mean, 2);
			}
		}
		result /= (w * h);
		return result;
	}

	public float getVarianceB(int x, int y, int w, int h) {
		float mean = getAverageB(x, y, w, h);
		float result = 0;
		for(int j = y; j < y + h; j++) {
			for(int i = x; i < x + w; i++) {
				result += Math.pow(image.getBlue(i, j) - mean, 2);
			}
		}
		result /= (w * h);
		return result;
	}

	public float getVarianceA(int x, int y, int w, int h) {
		float mean = getAverageA(x, y, w, h);
		float result = 0;
		for(int j = y; j < y + h; j++) {
			for(int i = x; i < x + w; i++) {
				result += Math.pow(image.getAlpha(i, j) - mean, 2);
			}
		}
		result /= (w * h);
		return result;
	}

	protected static Integral constructIntegral(ImageBuffer image, int band, int blockX, int blockY) {
		long pixels = (long) image.getWidth() * image.getHeight();
		if(pixels <= ShortIntegral.MAX_PIXELS)
			return new ShortIntegral(image, band, blockX, blockY);
		if(pixels <= IntIntegral.MAX_PIXELS)
			return new IntIntegral(image, band, blockX, blockY);
		return new FloatIntegral(image, band, blockX, blockY);
	}
	protected static interface Integral {
		public Object getValues();
		public int getBand();
		public int getBlockX();
		public int getBlockY();
		public long getIntegral(int x, int y, int w, int h);
	}
	protected static class ShortIntegral implements Integral {
		public static final long MAX_PIXELS = Short.MAX_VALUE / 255;
		protected final int width;
		protected final int height;
		protected final int blockX;
		protected final int blockY;
		protected final int gridWidth;
		protected final int gridHeight;
		protected final int band;
		protected final short[] values;

		public ShortIntegral(ImageBuffer image, int band, int blockX, int blockY) {
			this.width = image.getWidth();
			this.height = image.getHeight();
			this.blockX = blockX;
			this.blockY = blockY;
			this.gridWidth = (width + blockX - 1) / blockX;
			this.gridHeight = (height + blockY - 1) / blockY;
			this.band = band;
			this.values = new short[gridHeight * gridWidth];
			for(int gy = 0; gy < gridHeight; gy++) {
				for(int gx = 0; gx < gridWidth; gx++) {
					int xStart = gx * blockX;
					int yStart = gy * blockY;
					int xEnd = Math.min(xStart + blockX, width);
					int yEnd = Math.min(yStart + blockY, height);
					short blockValue = 0;
					for(int y = yStart; y < yEnd; y++) {
						for(int x = xStart; x < xEnd; x++) {
							blockValue += band == 0 ? image.getRed(x, y) : 
								band == 1 ? image.getGreen(x, y) : 
								band == 2 ? image.getBlue(x, y) : 
								band == 3 ? image.getAlpha(x, y) : 
								0;
						}
					}
					short above = gy > 0 ? values[(gy - 1) * gridWidth + gx] : 0;
					short left = gx > 0 ? values[gy * gridWidth + gx - 1] : 0;
					short diag = gy > 0 && gx > 0 ? values[(gy - 1) * gridWidth + gx - 1] : 0;
					values[gy * gridWidth + gx] = (short) (blockValue + above + left - diag);
				}
			}
		}

		@Override
		public short[] getValues() {
			return values;
		}
		@Override
		public int getBand() {
			return band;
		}
		@Override
		public int getBlockX() {
			return blockX;
		}
		@Override
		public int getBlockY() {
			return blockY;
		}
		@Override
		public long getIntegral(int x, int y, int w, int h) {
			if(x < 0 || x + w > width || y < 0 || y + h > height || w <= 0 || h <= 0)
				throw new IndexOutOfBoundsException();
			if(x + w == width) w = gridWidth * blockX - x;
			if(y + h == height) h = gridHeight * blockY - y;
			if(x % blockX != 0 || y % blockY != 0 || (x + w) % blockX != 0 || (y + h) % blockY != 0)
				throw new IllegalArgumentException("Coordinates must align with stride boundaries");
			x /= blockX;
			y /= blockY;
			w /= blockX;
			h /= blockY;
			short A = values[(y + h - 1) * gridWidth + x + w - 1];
			short B = x > 0 ? values[(y + h - 1) * gridWidth + x - 1] : 0;
			short C = y > 0 ? values[(y - 1) * gridWidth + x + w - 1] : 0;
			short D = x > 0 && y > 0 ? values[(y - 1) * gridWidth + x - 1] : 0;
			return A - B - C + D;
		}
	}
	protected static class IntIntegral implements Integral {
		public static final long MAX_PIXELS = Integer.MAX_VALUE / 255;
		protected final int width;
		protected final int height;
		protected final int blockX;
		protected final int blockY;
		protected final int gridWidth;
		protected final int gridHeight;
		protected final int band;
		protected final int[] values;

		public IntIntegral(ImageBuffer image, int band, int blockX, int blockY) {
			this.width = image.getWidth();
			this.height = image.getHeight();
			this.blockX = blockX;
			this.blockY = blockY;
			this.gridWidth = (width + blockX - 1) / blockX;
			this.gridHeight = (height + blockY - 1) / blockY;
			this.band = band;
			this.values = new int[gridHeight * gridWidth];
			for(int gy = 0; gy < gridHeight; gy++) {
				for(int gx = 0; gx < gridWidth; gx++) {
					int xStart = gx * blockX;
					int yStart = gy * blockY;
					int xEnd = Math.min(xStart + blockX, width);
					int yEnd = Math.min(yStart + blockY, height);
					int blockValue = 0;
					for(int y = yStart; y < yEnd; y++) {
						for(int x = xStart; x < xEnd; x++) {
							blockValue += band == 0 ? image.getRed(x, y) : 
								band == 1 ? image.getGreen(x, y) : 
								band == 2 ? image.getBlue(x, y) : 
								band == 3 ? image.getAlpha(x, y) : 
								0;
						}
					}
					int above = gy > 0 ? values[(gy - 1) * gridWidth + gx] : 0;
					int left = gx > 0 ? values[gy * gridWidth + gx - 1] : 0;
					int diag = gy > 0 && gx > 0 ? values[(gy - 1) * gridWidth + gx - 1] : 0;
					values[gy * gridWidth + gx] = blockValue + above + left - diag;
				}
			}
		}

		@Override
		public int[] getValues() {
			return values;
		}
		@Override
		public int getBand() {
			return band;
		}
		@Override
		public int getBlockX() {
			return blockX;
		}
		@Override
		public int getBlockY() {
			return blockY;
		}
		@Override
		public long getIntegral(int x, int y, int w, int h) {
			if(x < 0 || x + w > width || y < 0 || y + h > height || w <= 0 || h <= 0)
				throw new IndexOutOfBoundsException();
			if(x + w == width) w = gridWidth * blockX - x;
			if(y + h == height) h = gridHeight * blockY - y;
			if(x % blockX != 0 || y % blockY != 0 || (x + w) % blockX != 0 || (y + h) % blockY != 0)
				throw new IllegalArgumentException("Coordinates must align with stride boundaries");
			x /= blockX;
			y /= blockY;
			w /= blockX;
			h /= blockY;
			int A = values[(y + h - 1) * gridWidth + x + w - 1];
			int B = x > 0 ? values[(y + h - 1) * gridWidth + x - 1] : 0;
			int C = y > 0 ? values[(y - 1) * gridWidth + x + w - 1] : 0;
			int D = x > 0 && y > 0 ? values[(y - 1) * gridWidth + x - 1] : 0;
			return (long) A - B - C + D;
		}
	}
	protected static class FloatIntegral implements Integral {
		public static final long MAX_PIXELS = (long) (Float.MAX_VALUE / 255);
		protected final int width;
		protected final int height;
		protected final int blockX;
		protected final int blockY;
		protected final int gridWidth;
		protected final int gridHeight;
		protected final int band;
		protected final float[] values;

		public FloatIntegral(ImageBuffer image, int band, int blockX, int blockY) {
			this.width = image.getWidth();
			this.height = image.getHeight();
			this.blockX = blockX;
			this.blockY = blockY;
			this.gridWidth = (width + blockX - 1) / blockX;
			this.gridHeight = (height + blockY - 1) / blockY;
			this.band = band;
			this.values = new float[gridHeight * gridWidth];
			for(int gy = 0; gy < gridHeight; gy++) {
				for(int gx = 0; gx < gridWidth; gx++) {
					int xStart = gx * blockX;
					int yStart = gy * blockY;
					int xEnd = Math.min(xStart + blockX, width);
					int yEnd = Math.min(yStart + blockY, height);
					float blockValue = 0;
					for(int y = yStart; y < yEnd; y++) {
						for(int x = xStart; x < xEnd; x++) {
							blockValue += band == 0 ? image.getRed(x, y) : 
								band == 1 ? image.getGreen(x, y) : 
								band == 2 ? image.getBlue(x, y) : 
								band == 3 ? image.getAlpha(x, y) : 
								0;
						}
					}
					float above = gy > 0 ? values[(gy - 1) * gridWidth + gx] : 0;
					float left = gx > 0 ? values[gy * gridWidth + gx - 1] : 0;
					float diag = gy > 0 && gx > 0 ? values[(gy - 1) * gridWidth + gx - 1] : 0;
					values[gy * gridWidth + gx] = blockValue + above + left - diag;
				}
			}
		}

		@Override
		public float[] getValues() {
			return values;
		}
		@Override
		public int getBand() {
			return band;
		}
		@Override
		public int getBlockX() {
			return blockX;
		}
		@Override
		public int getBlockY() {
			return blockY;
		}
		@Override
		public long getIntegral(int x, int y, int w, int h) {
			if(x < 0 || x + w > width || y < 0 || y + h > height || w <= 0 || h <= 0)
				throw new IndexOutOfBoundsException();
			if(x + w == width) w = gridWidth * blockX - x;
			if(y + h == height) h = gridHeight * blockY - y;
			if(x % blockX != 0 || y % blockY != 0 || (x + w) % blockX != 0 || (y + h) % blockY != 0)
				throw new IllegalArgumentException("Coordinates must align with stride boundaries");
			x /= blockX;
			y /= blockY;
			w /= blockX;
			h /= blockY;
			float A = values[(y + h - 1) * gridWidth + x + w - 1];
			float B = x > 0 ? values[(y + h - 1) * gridWidth + x - 1] : 0;
			float C = y > 0 ? values[(y - 1) * gridWidth + x + w - 1] : 0;
			float D = x > 0 && y > 0 ? values[(y - 1) * gridWidth + x - 1] : 0;
			return (long) (A - B - C + D);
		}
	}
}
