import magpiebridge.core.IProjectService;
import magpiebridge.core.JavaProjectService;
import magpiebridge.core.MagpieServer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
    MagpieServer server = new MagpieServer();
    String language = "java";
    IProjectService javaProjectService = new JavaProjectService();
    server.addProjectService(language, javaProjectService);
    server.addAnalysis(language, new FlowDroidServerAnalysis(config));
    server.launchOnStdio();
  }
}
