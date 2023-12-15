package util;
import org.example.QuadLeaf;
public class AbstractLeaf<T> {
    public QuadLeaf<T> value;

    public AbstractLeaf(QuadLeaf<T> value) {
        this.value = value;
    }
}
