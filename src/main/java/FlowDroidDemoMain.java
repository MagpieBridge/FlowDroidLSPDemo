import java.text.MessageFormat;
import java.util.function.Supplier;
import magpiebridge.core.IProjectService;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.projectservice.java.JavaProjectService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

public class FlowDroidDemoMain {
  private static final String DEFAULT_PORT = "5007";

  public static void main(String... args) throws ParseException {
    Options options = new Options();
    options.addOption("h", "help", false, "Print this mesage");
    options.addOption("c", "config", true, "The location of configuration files");
    options.addOption(
        "s",
        "socket",
        false,
        MessageFormat.format("run in socket mode, standard port is {0}", DEFAULT_PORT));
    options.addOption(
        "p",
        "port",
        true,
        MessageFormat.format("sets the port for socket mode, standard port is {0}", DEFAULT_PORT));
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);
    HelpFormatter helper = new HelpFormatter();
    String cmdLineSyntax =
        "\nJava Project:-c configuration files\nAndroid Project:-a -p Android platforms -c configuration files ";
    if (cmd.hasOption('h')) {
      helper.printHelp(cmdLineSyntax, options);
      return;
    }

    Supplier<MagpieServer> createServer =
        () -> {
          String config = null;
          if (!cmd.hasOption("c")) {
            helper.printHelp(cmdLineSyntax, options);
          } else {
            config = cmd.getOptionValue("c");
          }
          ServerConfiguration c = new ServerConfiguration();
          MagpieServer server = new MagpieServer(c);
          String language = "java";

          // analyze java project
          IProjectService javaProjectService = new JavaProjectService();

          server.addProjectService(language, javaProjectService);
          server.addAnalysis(Either.forLeft(new FlowDroidServerAnalysis(config)), language);
          return server;
        };

    if (cmd.hasOption("socket")) {
      int port = Integer.parseInt(cmd.getOptionValue("port", DEFAULT_PORT));
      MagpieServer.launchOnSocketPort(port, createServer);
    } else {
      createServer.get().launchOnStdio();
    }
  }
}
