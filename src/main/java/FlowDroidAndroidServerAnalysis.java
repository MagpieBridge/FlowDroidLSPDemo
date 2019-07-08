import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.util.collections.Pair;
import de.upb.soot.core.SootClass;
import de.upb.soot.frontends.java.WalaClassLoader;
import de.upb.soot.jimple.basic.PositionInfo;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import magpiebridge.converter.JimpleConverter;
import magpiebridge.converter.PositionInfoTag;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.IProjectService;
import magpiebridge.core.Kind;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.projectservice.java.AndroidProjectService;
import magpiebridge.util.SourceCodeReader;
import org.eclipse.lsp4j.DiagnosticSeverity;
import soot.Scene;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.config.SootConfigForAndroid;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;
import soot.util.MultiMap;

public class FlowDroidAndroidServerAnalysis implements ServerAnalysis {
  private static final Logger LOG = Logger.getLogger("main");
  private String configPath;
  private Set<String> srcPath;
  private Set<String> libPath;
  private String apkFile;
  private String androidPlatforms;
  private boolean showRelated = true;
  private boolean firstTimeOpen = true;
  private ExecutorService exeService;
  private Future<?> last;

  public FlowDroidAndroidServerAnalysis(String configPath, String androidPlatforms) {
    this.configPath = configPath;
    this.androidPlatforms = androidPlatforms;
    exeService = Executors.newSingleThreadExecutor();
  }

  public void setClassPath(MagpieServer server) {
    if (srcPath == null) {
      srcPath = new HashSet<>();
      libPath = new HashSet<>();
      Optional<IProjectService> service = server.getProjectService("java");
      if (service.isPresent()) {
        AndroidProjectService ps = (AndroidProjectService) server.getProjectService("java").get();
        Set<Path> sourcePath = ps.getSourcePath();
        for (Path path : sourcePath) {
          if (path.toString().endsWith("\\main\\java")) {
            srcPath.add(path.toString());
          }
        }

        if (apkFile == null) {
          Optional<Path> p = ps.getApkPath();
          if (p.isPresent()) apkFile = p.get().toString();
          String rootPath = ps.getRootPath().get().toString();
          File gendir = Paths.get(rootPath, "generatedlib").toFile();
          if (!gendir.exists()) {
            gendir.mkdirs();
          }
          Path libJar = Paths.get(gendir.toPath().toString(), "out.jar");
          File jar = libJar.toFile();
          if (firstTimeOpen || !jar.exists()) {
            // generate a big out.jar contains all dependencies from the apk file
            Utils.generateJar(
                apkFile,
                androidPlatforms,
                gendir.toPath().toString(),
                ps.getSourceClassFullQualifiedNames());
            if (jar.exists()) {
              LOG.log(Level.INFO, "Created a out.jar with soot");
              libPath.add(jar.getAbsolutePath().toString());
              LOG.log(Level.INFO, libPath.toString());
            }
            firstTimeOpen = false;
          }
        }
      }
    }
  }

  @Override
  public String source() {
    return "FlowDroid";
  }

  @Override
  public void analyze(Collection<Module> files, MagpieServer server) {
    if (last != null && !last.isDone()) {
      last.cancel(false);
      if (last.isCancelled()) LOG.info("Susscessfully cancelled last analysis and start new");
    }
    Future<?> future =
        exeService.submit(
            new Runnable() {
              @Override
              public void run() {
                setClassPath(server);
                Collection<AnalysisResult> results = Collections.emptyList();
                if (srcPath != null) {
                  results = analyze(apkFile, srcPath, libPath, androidPlatforms);
                }
                server.consume(results, source());
              }
            });
    last = future;
  }

  private List<Pair<Position, String>> getRelated(Stmt[] path) throws Exception {
    List<Pair<Position, String>> related = new ArrayList<>();
    if (path == null) {
      return related;
    }
    for (Stmt s : path) {
      PositionInfoTag tag = (PositionInfoTag) s.getTag("PositionInfoTag");
      if (tag != null) {
        // just add stmt positions on the data flow path to related for now
        Position stmtPos = tag.getPositionInfo().getStmtPosition();
        String code = SourceCodeReader.getLinesInString(stmtPos).split(";")[0] + ";";
        related.add(Pair.make(stmtPos, code));
      }
    }
    return related;
  }

  public Collection<AnalysisResult> analyze(
      String apkFile, Set<String> srcPath, Set<String> libPath, String androidPlatforms) {
    LOG.info("androidPlatform:" + androidPlatforms);
    LOG.info("apkFile:" + apkFile);
    LOG.info("srcPath:" + srcPath);
    LOG.info("libPath:" + libPath);
    try {
      String platformPath = new File(this.androidPlatforms).getAbsolutePath();
      // setup flowDroid
      InfoflowAndroidConfiguration c = new InfoflowAndroidConfiguration();
      // c.setWriteOutputFiles(true);
      c.getPathConfiguration()
          .setPathReconstructionMode(
              PathReconstructionMode.Fast); // turn on to compute data flow path
      c.getAnalysisFileConfig().setAndroidPlatformDir(platformPath);
      c.getAnalysisFileConfig().setTargetAPKFile(apkFile);
      SetupApplication flowDroid = new SetupApplication(c);
      flowDroid.setSourceCodePath(srcPath);
      flowDroid.setLibPath(libPath);
      String androidJar = Scene.v().getAndroidJarPath(platformPath, apkFile);
      flowDroid.setAndroidJar(androidJar);
      Consumer<Set<String>> sourceCodeConsumer =
          sourceCodePath -> {
            HashSet<String> libs = new HashSet<>(libPath);
            libs.add(androidJar);
            WalaClassLoader loader = new WalaClassLoader(sourceCodePath, libs, null);
            List<SootClass> sootClasses = loader.getSootClasses();
            JimpleConverter jimpleConverter = new JimpleConverter(sootClasses);
            jimpleConverter.convertAllClasses();
          };
      flowDroid.setSourceCodeConsumer(sourceCodeConsumer);
      flowDroid.setCallbackFile(configPath + File.separator + "AndroidCallbacks.txt");
      flowDroid.setTaintWrapper(
          new EasyTaintWrapper(configPath + File.separator + "EasyTaintWrapperSource.txt"));
      SootConfigForAndroid sootConfigForAndroid =
          new SootConfigForAndroid() {
            @Override
            public void setSootOptions(Options options, InfoflowConfiguration config) {
              options.set_exclude(Collections.emptyList());
            }
          };
      flowDroid.setSootConfig(sootConfigForAndroid);
      Collection<AnalysisResult> results = new HashSet<>();
      MultiMap<ResultSinkInfo, ResultSourceInfo> res =
          flowDroid.runInfoflow(configPath + File.separator + "SourcesAndSinks.txt").getResults();
      if (res != null) {
        // infoflow.getResults().printResults();
        for (ResultSinkInfo sink : res.keySet()) {
          PositionInfo sinkPos =
              ((PositionInfoTag) sink.getStmt().getTag("PositionInfoTag")).getPositionInfo();
          for (ResultSourceInfo source : res.get(sink)) {

            PositionInfo sourcePos =
                ((PositionInfoTag) source.getStmt().getTag("PositionInfoTag")).getPositionInfo();

            String sinkCode = SourceCodeReader.getLinesInString(sinkPos.getStmtPosition());
            String sourceCode = SourceCodeReader.getLinesInString(sourcePos.getStmtPosition());
            if (sinkCode.isEmpty()) {
              sinkCode = sink.getDefinition().toString();
            }
            if (sourceCode.isEmpty()) {
              sourceCode = source.getDefinition().toString();
            }
            String[] strs = sourcePos.getStmtPosition().getURL().toString().split("/");
            String className = "";
            if (strs.length > 0) className = strs[strs.length - 1];
            String msg =
                String.format(
                    "Found a sensitive flow to sink [%s] from the source [%s] at line %d in %s",
                    sinkCode, sourceCode, sourcePos.getStmtPosition().getFirstLine(), className);
            List<Pair<Position, String>> relatedInfo = new ArrayList<>();
            if (showRelated) relatedInfo = getRelated(source.getPath());
            StringBuilder code = new StringBuilder();
            code.append(SourceCodeReader.getLinesInString(sourcePos.getStmtPosition()));
            code.append(SourceCodeReader.getLinesInString(sinkPos.getStmtPosition()));
            FlowDroidResult r =
                new FlowDroidResult(
                    Kind.Diagnostic,
                    sinkPos.getStmtPosition(),
                    msg,
                    relatedInfo,
                    DiagnosticSeverity.Error,
                    null,
                    code.toString());
            results.add(r);
            return results;
          }
        }
      }
      return results;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
