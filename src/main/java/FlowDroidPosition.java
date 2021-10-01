import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

public class FlowDroidPosition implements Position {

  private int firstLine;
  private int lastLine;
  private int firstCol;
  private URL url;

  public FlowDroidPosition(int firstLine, int lastLine, int firstCol, URL url) {
    this.firstLine = firstLine;
    this.lastLine = lastLine;
    this.firstCol = firstCol;
    this.url = url;
  }

  @Override
  public int getFirstLine() {
    return firstLine;
  }

  @Override
  public int getFirstCol() {
    return firstCol;
  }

  @Override
  public URL getURL() {
    return url;
  }

  @Override
  public int getLastLine() {
    return lastLine;
  }

  @Override
  public int getLastCol() {
    return -1;
  }

  @Override
  public int getFirstOffset() {
    return -1;
  }

  @Override
  public int getLastOffset() {
    return -1;
  }

  @Override
  public int compareTo(SourcePosition arg0) {
    return 0;
  }

  @Override
  public Reader getReader() throws IOException {
    return null;
  }

  public String toString() {
    return "First Line: "
        + firstLine
        + ", Last Line: "
        + lastLine
        + ", First Col :"
        + firstCol
        + ", url : "
        + url.toString();
  }
}
