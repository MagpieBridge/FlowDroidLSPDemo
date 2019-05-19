import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.util.collections.Pair;
import de.upb.soot.core.SootClass;
import de.upb.soot.frontends.java.WalaClassLoader;
import de.upb.soot.jimple.basic.PositionInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
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
import java.util.logging.Logger;
import magpiebridge.converter.JimpleConverter;
import magpiebridge.converter.PositionInfoTag;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.IProjectService;
import magpiebridge.core.Kind;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.projectservice.java.JavaProjectService;
import magpiebridge.util.SourceCodeReader;
import org.eclipse.lsp4j.DiagnosticSeverity;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.util.MultiMap;

public class FlowDroidServerAnalysis implements ServerAnalysis {

  private static final Logger LOG = Logger.getLogger("main");

  private List<String> sources;
  private List<String> sinks;
  private List<String> entryPoints;
  private IEntryPointCreator entryPointCreator;
  private Set<String> srcPath;
  private Set<String> libPath;
  private String configPath;
  private EasyTaintWrapper easyWrapper;
  private ExecutorService exeService;
  private Future<?> last;

  private boolean showRelated = false;
  private boolean debug = false;

  public FlowDroidServerAnalysis(String configPath) {
    this.configPath = configPath;
    exeService = Executors.newSingleThreadExecutor();
    loadEntryPoints();
    try {
      easyWrapper =
          new EasyTaintWrapper(
              new File(configPath + File.separator + "EasyTaintWrapperSource.cfg"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("resource")
  private void loadEntryPoints() {
    entryPoints = new ArrayList<>();
    String entryPointsFile = configPath + File.separator + "EntryPoints.cfg";
    String regex = "^<(.+):\\s*(.+)\\s+(.+)\\s*\\((.*)\\)>";
    FileReader fr = null;
    BufferedReader br = null;
    try {
      fr = new FileReader(entryPointsFile);
      String line;
      br = new BufferedReader(fr);
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.matches(regex)) {
          entryPoints.add(line);
        }
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    entryPointCreator = new DefaultEntryPointCreator(entryPoints);
  }

  private void loadSourceAndSinks() {
    sources = new ArrayList<>();
    sinks = new ArrayList<>();
    String sourceSinkFile = configPath + File.separator + "SourcesAndSinks.cfg";
    ISourceSinkDefinitionProvider parser;
    try {
      parser = PermissionMethodParser.fromFile(sourceSinkFile);
      for (SourceSinkDefinition source : parser.getSources()) {
        sources.add(source.toString());
      }
      for (SourceSinkDefinition sink : parser.getSinks()) {
        sinks.add(sink.toString());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String source() {
    return "Analyzer Y";
  }

  /**
   * set up source code path and library path with the project service provided by the server.
   *
   * @param server
   */
  public void setClassPath(MagpieServer server) {
    if (srcPath == null) {
      srcPath = new HashSet<>();
      Optional<IProjectService> opt = server.getProjectService("java");
      if (opt.isPresent()) {
        JavaProjectService ps = (JavaProjectService) server.getProjectService("java").get();
        Set<Path> sourcePath = ps.getSourcePath();
        if (libPath == null) {
          libPath = new HashSet<>();
          ps.getLibraryPath().stream().forEach(path -> libPath.add(path.toString()));
          libPath.add(
              System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar");
        }
        if (!sourcePath.isEmpty()) {
          Set<String> temp = new HashSet<>();
          sourcePath.stream().forEach(path -> temp.add(path.toString()));
          srcPath = temp;
        }
      }
    }
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
                  results = analyze(srcPath, libPath);
                }
                server.consume(results, source());
              }
            });
    last = future;
  }

  public Collection<AnalysisResult> analyze(Set<String> srcPath, Set<String> libPath) {
    // LOG.info("entryPoints: " + entryPoints);
    // LOG.info("srcPath: " + srcPath);
    // LOG.info("libPath: " + libPath);
    loadSourceAndSinks();
    Infoflow infoflow = new Infoflow();
    infoflow.getConfig().setInspectSources(false);
    infoflow.getConfig().setInspectSinks(false);
    infoflow.getConfig().setLogSourcesAndSinks(debug);
    infoflow.getConfig().setWriteOutputFiles(debug);
    if (showRelated) {
      infoflow
          .getConfig()
          .getPathConfiguration()
          .setPathReconstructionMode(PathReconstructionMode.Fast);
    }
    infoflow.setTaintWrapper(easyWrapper);
    infoflow.setSourceCodePath(srcPath);
    Consumer<Set<String>> sourceCodeConsumer =
        sp -> {
          WalaClassLoader loader = new WalaClassLoader(sp, libPath, null);
          List<SootClass> sootClasses = loader.getSootClasses();
          JimpleConverter jimpleConverter = new JimpleConverter(sootClasses);
          jimpleConverter.convertAllClasses();
        };
    infoflow.setSourceCodeConsumer(sourceCodeConsumer);
    infoflow.computeInfoflow(null, getLongLibPath(libPath), entryPointCreator, sources, sinks);
    Collection<AnalysisResult> results = new HashSet<>();
    MultiMap<ResultSinkInfo, ResultSourceInfo> res = infoflow.getResults().getResults();
    if (res != null) {
      // infoflow.getResults().printResults();
      for (ResultSinkInfo sink : res.keySet()) {
        PositionInfo sinkPos =
            ((PositionInfoTag) sink.getStmt().getTag("PositionInfoTag")).getPositionInfo();
        for (ResultSourceInfo source : res.get(sink)) {

          PositionInfo sourcePos =
              ((PositionInfoTag) source.getStmt().getTag("PositionInfoTag")).getPositionInfo();
          try {
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
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    if (debug) {
      for (Stmt source : infoflow.getCollectedSources()) LOG.info("++Detected SOURCE: " + source);
      for (Stmt sink : infoflow.getCollectedSinks()) LOG.info("--Detected SINK: " + sink);
    }
    return results;
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

  private String getLongLibPath(Set<String> libPath) {
    StringBuilder strBuilder = new StringBuilder();
    for (String lib : libPath) {
      strBuilder.append(lib);
      strBuilder.append(File.pathSeparator);
    }
    return strBuilder.toString();
  }
}
