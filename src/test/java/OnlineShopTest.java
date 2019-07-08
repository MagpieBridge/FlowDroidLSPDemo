import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import magpiebridge.core.AnalysisResult;
import magpiebridge.projectservice.java.JavaProjectService;
import org.junit.Test;

public class OnlineShopTest {
  @Test
  public void test() {
    JavaProjectService ps = new JavaProjectService();
    Path root = Paths.get("E:\\Git\\Github\\magpie\\UserStudy\\onlineshop").toAbsolutePath();
    ps.setRootPath(root);
    Set<String> srcPath = new HashSet<>();
    Set<String> libPath = new HashSet<>();
    ps.getLibraryPath().stream().forEach(path -> libPath.add(path.toString()));
    ps.getSourcePath().stream().forEach(path -> srcPath.add(path.toString()));
    libPath.add(
        System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar");
    String configPath = new File("config").getAbsolutePath();
    FlowDroidServerAnalysis analysis = new FlowDroidServerAnalysis(configPath);
    Collection<AnalysisResult> results = analysis.analyze(srcPath, libPath);
    for (AnalysisResult result : results) {
      System.out.println(result.toString());
    }
  }

  @Test
  public void testTeleForum() {
    JavaProjectService ps = new JavaProjectService();
    Path root = Paths.get("E:\\Git\\Github\\magpie\\UserStudy\\teleForum").toAbsolutePath();
    ps.setRootPath(root);
    Set<String> srcPath = new HashSet<>();
    Set<String> libPath = new HashSet<>();
    ps.getLibraryPath().stream().forEach(path -> libPath.add(path.toString()));
    ps.getSourcePath().stream().forEach(path -> srcPath.add(path.toString()));
    libPath.add(
        System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar");
    String configPath = new File("config").getAbsolutePath();
    FlowDroidServerAnalysis analysis = new FlowDroidServerAnalysis(configPath);
    Collection<AnalysisResult> results = analysis.analyze(srcPath, libPath);
    for (AnalysisResult result : results) {
      System.out.println(result.toString());
    }
  }

  @Test
  public void testOnlineChat() {
    JavaProjectService ps = new JavaProjectService();
    Path root = Paths.get("E:\\Git\\Github\\magpie\\UserStudy\\onlinechat").toAbsolutePath();
    ps.setRootPath(root);
    Set<String> srcPath = new HashSet<>();
    Set<String> libPath = new HashSet<>();
    ps.getLibraryPath().stream().forEach(path -> libPath.add(path.toString()));
    ps.getSourcePath().stream().forEach(path -> srcPath.add(path.toString()));
    libPath.add(
        System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar");
    String configPath = new File("config").getAbsolutePath();
    FlowDroidServerAnalysis analysis = new FlowDroidServerAnalysis(configPath);
    Collection<AnalysisResult> results = analysis.analyze(srcPath, libPath);
    for (AnalysisResult result : results) {
      System.out.println(result.toString());
    }
  }
}
