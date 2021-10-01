import java.io.File;
import org.junit.Test;

public class DemoFlowDroidTest {

  @Test
  public void test() {
    String appClassPath =
        "D:\\eclipse_project\\FlowDroidLSPDemo\\src\\test\\resources\\DemoFlowDroid\\target\\classes";
    String libPath =
        "C:\\Users\\samic\\.m2\\repository\\javax\\servlet\\servlet-api\\2.5\\servlet-api-2.5.jar"
            + File.pathSeparator
            + "C:\\Program Files\\Java\\jdk1.8.0_211\\jre\\lib\\rt.jar"
            + File.pathSeparator
            + "C:\\Users\\samic\\.m2\\repository\\junit\\junit\\3.8.1\\junit-3.8.1.jar";
    String configPath = new File("config").getAbsolutePath();
    FlowDroidServerAnalysis analysis = new FlowDroidServerAnalysis(configPath);
    analysis.analyze(appClassPath, libPath);
  }
}
