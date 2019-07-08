import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import magpiebridge.core.AnalysisResult;
import magpiebridge.projectservice.java.JavaProjectService;
import org.junit.Test;

public class DemoProjectYTest {
  @Test
  public void test() {
    JavaProjectService ps = new JavaProjectService();
    Path root = Paths.get("E:\\Git\\Github\\magpie\\UserStudy\\DemoProjectY").toAbsolutePath();
    ps.setRootPath(root);
    Set<String> srcPath = new HashSet<>();
    Set<String> libPath = new HashSet<>();
    ps.getLibraryPath().stream().forEach(path -> libPath.add(path.toString()));
    ps.getSourcePath().stream().forEach(path -> srcPath.add(path.toString()));
    libPath.add(
        System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar");
    String configPath =
        new File("E:\\Git\\Github\\magpie\\flowdroid-lsp-demo\\config").getAbsolutePath();
    FlowDroidServerAnalysis analysis = new FlowDroidServerAnalysis(configPath);
    Collection<AnalysisResult> results = analysis.analyze(srcPath, libPath);
    for (AnalysisResult result : results) {
      System.out.println(result.toString());
    }
  }
}
