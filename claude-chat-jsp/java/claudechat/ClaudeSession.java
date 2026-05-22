package claudechat;

/**
 * Immutable data class representing a chat session.
 */
public class ClaudeSession {

    public final String id;
    public final String cwd;
    public final String label;
    public final String createdAt;

    public ClaudeSession(String id, String cwd, String label, String createdAt) {
        this.id = id;
        this.cwd = cwd;
        this.label = label;
        this.createdAt = createdAt;
    }

    public String toJson() {
        return "{\"id\":\"" + esc(id) + "\",\"cwd\":\"" + esc(cwd)
                + "\",\"label\":\"" + esc(label) + "\",\"createdAt\":\"" + esc(createdAt) + "\"}";
    }

    /** Minimal JSON string escape — no external libraries required. */
    static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
