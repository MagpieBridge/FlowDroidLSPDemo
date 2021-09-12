import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.util.collections.Pair;
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
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.IProjectService;
import magpiebridge.core.Kind;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.projectservice.java.JavaProjectService;
import magpiebridge.util.SourceCodePositionFinder;
import magpiebridge.util.SourceCodeReader;

import org.eclipse.jgit.internal.storage.file.PackFile;
import org.eclipse.lsp4j.DiagnosticSeverity;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.StatementSourceSinkDefinition;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.util.MultiMap;

public class FlowDroidServerAnalysis implements ServerAnalysis {

  private static final Logger LOG = Logger.getLogger("main");
  private List<String> sources;
  private List<String> sinks;
  private List<String> entryPoints;
  private IEntryPointCreator entryPointCreator;
  private String appClassPath="";
  private String libPath="";
  private String configPath;
  private EasyTaintWrapper easyWrapper;
  private ExecutorService exeService;
  private Future<?> last;

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
        appClassPath= ps.getClassPath().toString();
        for(Path p: ps.getLibraryPath()) {
          libPath+=p.toAbsolutePath().toString()+ File.pathSeparator;
        }
        if (libPath.isEmpty()) {
       
          for(Path p: ps.getLibraryPath()) {
            libPath+=p.toAbsolutePath().toString()+ File.pathSeparator;
          }
          libPath+= System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar";
        }
      }
    }
  }


  public Collection<AnalysisResult> analyze(String appClassPath, String libPath) {
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
    infoflow.computeInfoflow(appClassPath, libPath, entryPointCreator, sources, sinks);
    Collection<AnalysisResult> results = new HashSet<>();
    MultiMap<ResultSinkInfo, ResultSourceInfo> res = infoflow.getResults().getResults();
    if (res != null) {
      int leaks = 0; 
      for (ResultSinkInfo sink : res.keySet()) {
        int sinkLineNo =  sink.getStmt().getJavaSourceStartLineNumber();
        for (ResultSourceInfo source : res.get(sink)) {
          int sourceLineNo = source.getStmt().getJavaSourceStartLineNumber();
          leaks++;
          // TODO write code convert to FlowDroidResult. 
        }
      }
      System.err.println("Found "+leaks +" leaks.");
      infoflow.getResults().printResults();
    }

    return results;
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
                setClassPath((MagpieServer)server);
                Collection<AnalysisResult> results = Collections.emptyList();
                results = analyze(appClassPath, libPath);
                server.consume(results, source());
              }
            });
    last = future;
    
  }
}
