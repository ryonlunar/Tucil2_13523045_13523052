package tucil2_13523045_13523052;

public class ImageBuffer {
	protected final int width;
	protected final int height;
	protected final int[] buffer;

	public ImageBuffer(int width, int height, int[] buffer) {
		this.width = width;
		this.height = height;
		this.buffer = buffer;
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public int[] getBuffer() {
		return buffer;
	}
	public int getRed(int x, int y) {
		return (buffer[y * width + x] >> 16) & 0xFF;
	}
	public int getGreen(int x, int y) {
		return (buffer[y * width + x] >> 8)  & 0xFF;
	}
	public int getBlue(int x, int y) {
		return buffer[y * width + x] & 0xFF;
	}
	public int getAlpha(int x, int y) {
		return (buffer[y * width + x] >> 24) & 0xFF;
	}
}
