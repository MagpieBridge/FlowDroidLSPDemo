import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.util.collections.Pair;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
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
import java.util.logging.Logger;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.FlowAnalysisResult;
import magpiebridge.core.FlowCodePosition;
import magpiebridge.core.IProjectService;
import magpiebridge.core.Kind;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.projectservice.java.JavaProjectService;
import magpiebridge.util.SourceCodeInfo;
import magpiebridge.util.SourceCodePositionFinder;
import magpiebridge.util.SourceCodeReader;
import org.eclipse.lsp4j.DiagnosticSeverity;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;
import soot.util.MultiMap;

public class FlowDroidServerAnalysis implements ServerAnalysis {

  private static final Logger LOG = Logger.getLogger("main");
  private List<String> sources;
  private List<String> sinks;
  private List<String> entryPoints;
  private IEntryPointCreator entryPointCreator;
  private String appClassPath = "";
  private String libPath = "";
  private String configPath;
  private EasyTaintWrapper easyWrapper;
  private ExecutorService exeService;
  private Future<?> last;
  private String appSourcePath = "";

  private boolean showRelated = true;
  private boolean debug = false;

  public FlowDroidServerAnalysis(String configPath) {
    this.configPath = configPath;
    exeService = Executors.newSingleThreadExecutor();
    loadEntryPoints();
    try {
      easyWrapper =
          new EasyTaintWrapper(
              new File(configPath + File.separator + "EasyTaintWrapperSource.txt"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("resource")
  private void loadEntryPoints() {
    entryPoints = new ArrayList<>();
    String entryPointsFile = configPath + File.separator + "EntryPoints.txt";
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
    String sourceSinkFile = configPath + File.separator + "SourcesAndSinks.txt";
    ISourceSinkDefinitionProvider parser;
    try {
      parser = PermissionMethodParser.fromFile(sourceSinkFile);
      for (ISourceSinkDefinition source : parser.getSources()) {
        sources.add(source.toString());
      }
      for (ISourceSinkDefinition sink : parser.getSinks()) {
        sinks.add(sink.toString());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String source() {
    return "FlowDroid";
  }

  /**
   * set up application class path and library path with the project service provided by the server.
   *
   * @param server
   */
  public void setClassPath(MagpieServer server) {
    if (appClassPath.isEmpty()) {
      Optional<IProjectService> opt = server.getProjectService("java");
      if (opt.isPresent()) {
        JavaProjectService ps = (JavaProjectService) server.getProjectService("java").get();
        for (Path p : ps.getClassPath()) {
          appClassPath += p.toAbsolutePath().toString() + File.pathSeparator;
        }
        for (Path p : ps.getLibraryPath()) {
          libPath += p.toAbsolutePath().toString() + File.pathSeparator;
        }

        if (libPath.isEmpty()) {
          for (Path p : ps.getLibraryPath()) {
            libPath += p.toAbsolutePath().toString() + File.pathSeparator;
          }
        }

        libPath +=
            System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar";
      }
    }
  }
  /**
   * Set source path with the project service provided by the server.
   *
   * @param server
   */
  public void setSourcePath(MagpieServer server) {
    MagpieServer s = (MagpieServer) server;
    JavaProjectService ps = (JavaProjectService) s.getProjectService("java").get();
    Set<Path> sourcePath = ps.getSourcePath();
    if (!sourcePath.isEmpty()) {
      appSourcePath = sourcePath.iterator().next().toString();
    }
  }

  public Collection<AnalysisResult> analyze(String appClassPath, String libPath) {
    loadSourceAndSinks();
    Infoflow infoflow = new Infoflow();
    infoflow.getConfig().setInspectSources(false);
    infoflow.getConfig().setInspectSinks(false);
    infoflow.getConfig().setLogSourcesAndSinks(debug);
    infoflow.getConfig().setWriteOutputFiles(debug);
    infoflow.setSootConfig(
        new IInfoflowConfig() {
          public void setSootOptions(Options options, InfoflowConfiguration config) {
            Options.v().set_keep_line_number(true);
          }
        });
    if (showRelated) {
      infoflow
          .getConfig()
          .getPathConfiguration()
          .setPathReconstructionMode(PathReconstructionMode.Fast);
    }
    infoflow.setTaintWrapper(easyWrapper);

    Collection<AnalysisResult> proResults = new HashSet<>();
    infoflow.addResultsAvailableHandler(
        new ResultsAvailableHandler() {

          protected String getFileUrl(String className) throws MalformedURLException {
            return appSourcePath + "/" + className + ".java";
          }

          protected String removeParameterPath(String parmetersOfMethod) {
            String parameters[] = parmetersOfMethod.split(",");
            String pathRemovedParameters = "";

            for (int i = 0; i < parameters.length; i++) {
              if (i != 0) {
                pathRemovedParameters += ", ";
              }

              String parameterSplit[] = parameters[i].split("\\.");
              pathRemovedParameters +=
                  parameterSplit.length > 0
                      ? parameterSplit[parameterSplit.length - 1]
                      : parameters[i];
            }

            return pathRemovedParameters;
          }

          protected String getMethodName(String method) {
            String firstParse[] = method.split("\\(");

            if (firstParse.length > 0) {
              String classRemovedMethod = removeParameterPath(firstParse[0]) + "(";
              classRemovedMethod += removeParameterPath(firstParse[1]);
              return classRemovedMethod;
            }

            return method;
          }

          protected FlowCodePosition makePostion(IInfoflowCFG cfg, Stmt info) {
            SootMethod fileMethod = cfg.getMethodOf(info);
            String fileUrl = null;
            try {
              fileUrl = getFileUrl(fileMethod.getDeclaringClass().toString());
            } catch (MalformedURLException e) {
              e.printStackTrace();
            }

            if (fileUrl == null) {
              return null;
            }

            File javaFile = new File(fileUrl);

            SourceCodeInfo codeInfo =
                SourceCodePositionFinder.findCode(javaFile, info.getJavaSourceStartLineNumber());

            if (codeInfo == null) {
              return null;
            }
            Position codePostion = codeInfo.toPosition();
            return new FlowCodePosition(
                codePostion.getFirstLine(),
                codePostion.getFirstCol(),
                codePostion.getLastLine(),
                codePostion.getLastCol(),
                codePostion.getURL(),
                getMethodName(fileMethod.getSubSignature()));
          }

          protected boolean equalPosition(Position first, Position second) {
            if (first == null || second == null) {
              if (first == second) return true;
              return false;
            }

            if (first.getFirstCol() == second.getFirstCol()
                && first.getFirstLine() == second.getFirstLine()
                && first.getFirstCol() == second.getFirstCol()
                && first.getLastCol() == second.getLastCol()) {
              return true;
            }

            return false;
          }

          @Override
          public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
            MultiMap<ResultSinkInfo, ResultSourceInfo> res = infoflow.getResults().getResults();
            if (res != null) {
              for (ResultSinkInfo sink : res.keySet()) {
                List<Pair<Position, String>> relatedInfo = new ArrayList<>();
                String sourceCode = null;
                FlowCodePosition sourcePos = null;
                ResultSourceInfo mainSource = null;
                for (ResultSourceInfo source : res.get(sink)) {
                  if (source.getPath() != null) {
                    mainSource = source;
                    Position previousPosition = null;
                    for (Stmt pathElement : source.getPath()) {
                      sourcePos = makePostion(cfg, pathElement);

                      if (!equalPosition(sourcePos, previousPosition)) {
                        /*System.out.println("Prv: " + previousPosition);
                        System.out.println("Now: " + sourcePos);*/
                        sourceCode = null;
                        try {
                          sourceCode = SourceCodeReader.getLinesInString(sourcePos);
                        } catch (IOException e) {
                          e.printStackTrace();
                        }
                        Pair<Position, String> pair = Pair.make(sourcePos, sourceCode);
                        relatedInfo.add(pair);
                        previousPosition = sourcePos;
                      }
                    }
                  }
                }

                FlowCodePosition sinkPos = makePostion(cfg, sink.getStmt());
                String sinkCode = null;
                try {
                  sinkCode = SourceCodeReader.getLinesInString(sinkPos);
                } catch (IOException e) {
                  e.printStackTrace();
                }

                FlowCodePosition mainSourcePos = makePostion(cfg, mainSource.getStmt());
                String mainSourceCode = null;
                try {
                  mainSourceCode = SourceCodeReader.getLinesInString(mainSourcePos);
                } catch (IOException e) {
                  e.printStackTrace();
                }

                SootMethod fileMethod = cfg.getMethodOf(mainSource.getStmt());
                String className = fileMethod.getDeclaringClass().toString();

                String str =
                    String.format(
                        "Found a sensitive flow to sink [%s] from the source [%s] at line %d in %s",
                        sinkCode.trim(),
                        mainSourceCode.trim(),
                        mainSourcePos.getFirstLine(),
                        className);

                FlowAnalysisResult result =
                    new FlowAnalysisResult(
                        Kind.Diagnostic,
                        sinkPos,
                        str,
                        relatedInfo,
                        DiagnosticSeverity.Error,
                        null,
                        sinkCode);

                proResults.add(result);
              }
            }
          }
        });

    infoflow.computeInfoflow(appClassPath, libPath, entryPointCreator, sources, sinks);
    infoflow.getResults().printResults();
    // System.err.println("Pro Result :" + proResults);
    return proResults;
  }

  @Override
  public void analyze(Collection<? extends Module> files, AnalysisConsumer server, boolean rerun) {
    if (last != null && !last.isDone()) {
      last.cancel(false);
      if (last.isCancelled()) LOG.info("Susscessfully cancelled last analysis and start new");
    }
    Future<?> future =
        exeService.submit(
            new Runnable() {
              @Override
              public void run() {
                setClassPath((MagpieServer) server);
                setSourcePath((MagpieServer) server);
                Collection<AnalysisResult> results = Collections.emptyList();
                results = analyze(appClassPath, libPath);
                server.consume(results, source());
              }
            });
    last = future;
  }
}
