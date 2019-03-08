import magpiebridge.core.MagpieServer;

public class FlowDroidDemoMain {
	public static void main(String... args) {
		MagpieServer server = new MagpieServer();
		server.addAnalysis("java", new FlowdroidServerAnalysis());
		server.launchOnStdio();
	}

}
