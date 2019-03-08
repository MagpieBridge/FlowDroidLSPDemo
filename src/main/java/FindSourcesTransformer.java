import java.util.Map;

import soot.SceneTransformer;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class FindSourcesTransformer extends SceneTransformer{

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {
		JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG(false);
	}

}
