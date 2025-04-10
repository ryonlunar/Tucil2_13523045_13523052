package tucil2_13523045_13523052;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

public class Boundary2DQuadTree<T> {
	protected final int alignX;
	protected final int alignY;
	protected final MutableIntList indices;
	protected final MutableIntList boundaries;
	protected final List<T> metadata;
	protected final MutableIntList skipped;

	public Boundary2DQuadTree(int x, int y, int width, int height, int alignX, int alignY) {
		if(x % alignX != 0 || y % alignY != 0)
			throw new Error("Origin is not aligned");
		this.alignX = alignX;
		this.alignY = alignY;
		this.indices = IntLists.mutable.empty();
		this.boundaries = IntLists.mutable.empty();
		this.metadata = new ArrayList<>();
		this.skipped = IntLists.mutable.empty();
		newTree(x, y, width, height);
	}

	public int getIndexTL(int index) {
		return indices.get(index * 4 + 0);
	}
	public int getIndexTR(int index) {
		return indices.get(index * 4 + 1);
	}
	public int getIndexBL(int index) {
		return indices.get(index * 4 + 2);
	}
	public int getIndexBR(int index) {
		return indices.get(index * 4 + 3);
	}
	protected void setIndexTL(int index, int value) {
		indices.set(index * 4 + 0, value);
	}
	protected void setIndexTR(int index, int value) {
		indices.set(index * 4 + 1, value);
	}
	protected void setIndexBL(int index, int value) {
		indices.set(index * 4 + 2, value);
	}
	protected void setIndexBR(int index, int value) {
		indices.set(index * 4 + 3, value);
	}
	public int getBoundaryX(int index) {
		return boundaries.get(index * 4 + 0);
	}
	public int getBoundaryY(int index) {
		return boundaries.get(index * 4 + 1);
	}
	public int getBoundaryW(int index) {
		return boundaries.get(index * 4 + 2);
	}
	public int getBoundaryH(int index) {
		return boundaries.get(index * 4 + 3);
	}
	protected void setBoundaryX(int index, int value) {
		boundaries.set(index * 4 + 0, value);
	}
	protected void setBoundaryY(int index, int value) {
		boundaries.set(index * 4 + 1, value);
	}
	protected void setBoundaryW(int index, int value) {
		boundaries.set(index * 4 + 2, value);
	}
	protected void setBoundaryH(int index, int value) {
		boundaries.set(index * 4 + 3, value);
	}
	public T getMetadata(int index) {
		if(skipped.contains(index) || index < 0 || index >= indices.size() / 4)
			throw new IndexOutOfBoundsException(index);
		return metadata.get(index);
	}
	public void setMetadata(int index, T value) {
		if(skipped.contains(index) || index < 0 || index >= indices.size() / 4)
			throw new IndexOutOfBoundsException(index);
		metadata.set(index, value);
	}
	private static final int[] INDICES_INITIAL = { -1, -1, -1, -1 };
	private static final int[] BOUNDARIES_INITIAL = { -1, -1, -1, -1 };
	protected int newTree(int x, int y, int w, int h) {
		int index = indices.size() / 4;
		if(skipped.size() > 0) {
			index = skipped.getLast();
			skipped.removeAtIndex(skipped.size() - 1);
		} else {
			indices.addAllAtIndex(indices.size(), INDICES_INITIAL);
			boundaries.addAllAtIndex(boundaries.size(), BOUNDARIES_INITIAL);
			metadata.add(null);
		}
		setIndexTL(index, -1);
		setIndexTR(index, -1);
		setIndexBL(index, -1);
		setIndexBR(index, -1);
		setBoundaryX(index, x);
		setBoundaryY(index, y);
		setBoundaryW(index, w);
		setBoundaryH(index, h);
		metadata.set(index, null);
		return index;
	}
	protected void deleteTree(int index) {
		skipped.add(index);
		setIndexTL(index, -1);
		setIndexTR(index, -1);
		setIndexBL(index, -1);
		setIndexBR(index, -1);
		setBoundaryX(index, -1);
		setBoundaryY(index, -1);
		setBoundaryW(index, -1);
		setBoundaryH(index, -1);
		metadata.set(index, null);
	}
	public void split(int index) {
		int x = getBoundaryX(index);
		int y = getBoundaryY(index);
		int w = getBoundaryW(index);
		int h = getBoundaryH(index);
		int midX = ((x + w / 2 + alignX - 1) / alignX) * alignX;
		int midY = ((y + h / 2 + alignY - 1) / alignY) * alignY;
		if(midX <= x || midX >= x + w) midX = x + w / 2;
		if(midY <= y || midY >= y + h) midY = y + h / 2;
		int w1 = midX - x;
		int w2 = x + w - midX;
		int h1 = midY - y;
		int h2 = y + h - midY;
		if(getIndexTL(index) == -1)
			setIndexTL(index, newTree(x, y, w1, h1));
		if(getIndexTR(index) == -1)
			setIndexTR(index, newTree(midX, y, w2, h1));
		if(getIndexBL(index) == -1)
			setIndexBL(index, newTree(x, midY, w1, h2));
		if(getIndexBR(index) == -1)
			setIndexBR(index, newTree(midX, midY, w2, h2));
	}
	public void merge(int index) {
		int indexTL = getIndexTL(index);
		int indexTR = getIndexTR(index);
		int indexBL = getIndexBL(index);
		int indexBR = getIndexBR(index);
		if(indexTL != -1) {
			merge(indexTL);
			deleteTree(indexTL);
			setIndexTL(index, -1);
		}
		if(indexTR != -1) {
			merge(indexTR);
			deleteTree(indexTR);
			setIndexTR(index, -1);
		}
		if(indexBL != -1) {
			merge(indexBL);
			deleteTree(indexBL);
			setIndexBL(index, -1);
		}
		if(indexBR != -1) {
			merge(indexBR);
			deleteTree(indexBR);
			setIndexBR(index, -1);
		}
	}
}
