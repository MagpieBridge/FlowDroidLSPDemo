import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import magpiebridge.core.AnalysisResult;
import org.junit.Test;

public class DemoFlowDroidTest {

  @Test
  public void test() {
    Set<String> srcPath = new HashSet<>();
    Set<String> libPath = new HashSet<>();
    srcPath.add(
        "E:\\Git\\Github\\magpie\\flowdroid-lsp-demo\\src\\test\\resources\\DemoFlowDroid\\src\\main\\java");
    libPath.add(
        "C:\\Users\\linghui\\.m2\\repository\\javax\\servlet\\servlet-api\\2.5\\servlet-api-2.5.jar");
    libPath.add("C:\\PROGRA~1\\Java\\jdk1.8.0_202\\jre\\lib\\rt.jar");
    libPath.add("C:\\Users\\linghui\\.m2\\repository\\junit\\junit\\3.8.1\\junit-3.8.1.jar");
    String configPath = new File("config").getAbsolutePath();
    FlowDroidServerAnalysis analysis = new FlowDroidServerAnalysis(configPath);
    Collection<AnalysisResult> results = analysis.analyze(srcPath, libPath);
    for (AnalysisResult result : results) {
      System.out.println(result.toString());
    }
  }
}
