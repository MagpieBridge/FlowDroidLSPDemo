import magpiebridge.core.AnalysisResult;
import magpiebridge.core.IProjectService;
import magpiebridge.core.MagpieServer;
import magpiebridge.projectservice.java.JavaProjectService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Triple;

public class FlowDroidDemoMain {
  public static void main(String... args) throws ParseException {
    Options options = new Options();
    options.addOption("h", "help", false, "Print this mesage");
    options.addOption("c", "config", true, "The location of configuration files");
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);
    HelpFormatter helper = new HelpFormatter();
    String cmdLineSyntax = "\nJava Project:-c configuration files\n";
    if (cmd.hasOption('h')) {
      helper.printHelp(cmdLineSyntax, options);
      return;
    }
    String config = null;
    if (!cmd.hasOption("c")) {
      helper.printHelp(cmdLineSyntax, options);
    } else {
      config = cmd.getOptionValue("c");
    }
    MagpieServer server =
        new MagpieServer() {

          @Override
          protected boolean isFalsePositive(AnalysisResult result) {
            String serverUri = result.position().getURL().toString();
            String clientUri = this.getClientUri(serverUri);
            for (String uri : falsePositives.keySet()) {
              if (uri.equals(clientUri)) {
                for (Triple<Integer, String, String> fp : falsePositives.get(clientUri)) {
                  int diff = Math.abs((result.position().getFirstLine() + 1) - fp.getLeft());
                  int threshold = 5;
                  if (diff < threshold && result.code().equals(fp.getMiddle())) {
                    return true;
                  }
                }
              }
            }
            return false;
          }
        };

    String language = "java";
    IProjectService javaProjectService = new JavaProjectService();
    server.addProjectService(language, javaProjectService);
    server.addAnalysis(language, new FlowDroidServerAnalysis(config));
    server.launchOnStdio();
  }
}
