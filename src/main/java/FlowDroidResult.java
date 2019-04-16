import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.collections.Pair;

import org.eclipse.lsp4j.DiagnosticSeverity;

import magpiebridge.core.AnalysisResult;
import magpiebridge.core.Kind;

public class FlowDroidResult implements AnalysisResult {
  private final Kind kind;
  private final Position position;
  private final String message;
  private final Iterable<Pair<Position, String>> related;
  private final DiagnosticSeverity severity;
  private final Pair<Position, String> repair;

  public FlowDroidResult(
      Kind kind,
      Position pos,
      String msg,
      Iterable<Pair<Position, String>> relatedInfo,
      DiagnosticSeverity severity,
      Pair<Position, String> repair) {
    this.kind = kind;
    this.position = pos;
    this.message = msg;
    this.related = relatedInfo;
    this.severity = severity;
    this.repair = repair;
  }

  public Kind kind() {
    return this.kind;
  }

  public Position position() {
    return position;
  }

  public Iterable<Pair<Position, String>> related() {
    return related;
  }

  public DiagnosticSeverity severity() {
    return severity;
  }

  public Pair<Position, String> repair() {
    return repair;
  }

  public String toString(boolean useMarkdown) {
    return message;
  }

  @Override
  public String toString() {
    return "DataFlowResult [kind="
        + kind
        + ", position="
        + position
        + ", message="
        + message
        + ", related="
        + related
        + ", severity="
        + severity
        + ", repair="
        + repair
        + "]";
  }
}
