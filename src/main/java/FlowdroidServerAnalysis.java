import com.ibm.wala.classLoader.Module;

import de.upb.soot.core.SootClass;
import de.upb.soot.frontends.java.JimpleConverter;
import de.upb.soot.frontends.java.PositionTag;
import de.upb.soot.frontends.java.WalaClassLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.xmlpull.v1.XmlPullParserException;

import soot.Scene;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.config.SootConfigForAndroid;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;

import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;

public class FlowdroidServerAnalysis implements ServerAnalysis {

  @Override
  public String source() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void analyze(Collection<Module> files, MagpieServer server) {
    // TODO Auto-generated method stub

  }

  public static void main(String... args) {
    String configPath = "./config";
    // define class path
    String projectDir = new File("src/test/resources/ActivityLifecycle1").getAbsolutePath();
    Set<String> sourcePath = new HashSet<>();
    sourcePath.add(projectDir + File.separator + "src");
    sourcePath.add(projectDir + File.separator + "gen");
    Set<String> libPath = new HashSet<>();
    File libDir = new File(projectDir + File.separator + "libs");
    for (File file : libDir.listFiles()) {
      if (file.getName().endsWith(".jar")) {
        libPath.add(file.getAbsolutePath());
      }
    }
    String androidPlatform = new File("src/test/resources/platforms").getAbsolutePath();

    try {
      // setup flowDroid
      InfoflowAndroidConfiguration c = new InfoflowAndroidConfiguration();
      // c.setWriteOutputFiles(true);
      c.getPathConfiguration().setPathReconstructionMode(PathReconstructionMode.Fast); // turn on to compute data flow path
      c.getAnalysisFileConfig().setAndroidPlatformDir(androidPlatform);
      c.getAnalysisFileConfig().setTargetAPKFile("src/test/resources/ActivityLifecycle1/ActivityLifecycle1.apk");
      SetupApplication flowDroid = new SetupApplication(c);
      flowDroid.setSourceCodePath(sourcePath);
      flowDroid.setLibPath(libPath);
      String androidJar
          = Scene.v().getAndroidJarPath(androidPlatform, "src/test/resources/ActivityLifecycle1/ActivityLifecycle1.apk");
      flowDroid.setAndroidJar(androidJar);
      Consumer<Set<String>> sourceCodeConsumer = sourceCodePath -> {
        HashSet<String> libs = new HashSet<>(libPath);
        libs.add(androidJar);
        WalaClassLoader loader = new WalaClassLoader(sourceCodePath, libs, null);
        List<SootClass> sootClasses = loader.getSootClasses();
        JimpleConverter jimpleConverter = new JimpleConverter(sootClasses);
        jimpleConverter.convertAllClasses();
      };
      flowDroid.setSourceCodeConsumer(sourceCodeConsumer);
      flowDroid.setCallbackFile(configPath + File.separator + "AndroidCallbacks.txt");
      flowDroid.setTaintWrapper(new EasyTaintWrapper(configPath + File.separator + "EasyTaintWrapperSource.txt"));
      SootConfigForAndroid sootConfigForAndroid = new SootConfigForAndroid() {
        @Override
        public void setSootOptions(Options options, InfoflowConfiguration config) {
          options.set_exclude(Collections.emptyList());
        }
      };
      flowDroid.setSootConfig(sootConfigForAndroid);
      InfoflowResults results = flowDroid.runInfoflow(configPath + File.separator + "SourcesAndSinks.txt");
      for (DataFlowResult re : results.getResultSet()) {
        Stmt sink = re.getSink().getStmt();
        Stmt source = re.getSource().getStmt();
        System.out
            .println("sink: " + sink.toString() + "\nposition: " + ((PositionTag) sink.getTag("PositionTag")).getPosition());
        System.out.println(
            "source: " + source.toString() + "\nposition: " + ((PositionTag) source.getTag("PositionTag")).getPosition());
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (XmlPullParserException e) {
      e.printStackTrace();
    }
  }

}
