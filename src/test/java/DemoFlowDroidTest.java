import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import magpiebridge.core.AnalysisResult;
import org.junit.Test;

public class DemoFlowDroidTest {

  @Test
  public void test() {
    String appClassPath
        = "E:\\Git\\Github\\magpie\\flowdroid-lsp-demo\\src\\test\\resources\\DemoFlowDroid\\target\\classes";
    String libPath = "C:\\Users\\linghui\\.m2\\repository\\javax\\servlet\\servlet-api\\2.5\\servlet-api-2.5.jar"
        + File.pathSeparator + "C:\\Program Files\\Java\\jdk1.8.0_202\\jre\\lib\\rt.jar" + File.pathSeparator
        + "C:\\Users\\linghui\\.m2\\repository\\junit\\junit\\3.8.1\\junit-3.8.1.jar";
    String configPath = new File("config").getAbsolutePath();
    FlowDroidServerAnalysis analysis = new FlowDroidServerAnalysis(configPath);
    analysis.analyze(appClassPath, libPath);
  }

}
