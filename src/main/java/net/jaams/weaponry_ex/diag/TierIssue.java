package net.jaams.weaponry_ex.diag;

public record TierIssue(Severity severity, String message) {

    public enum Severity {

        ERROR,

        WARN,

        INFO
    }

    public static TierIssue error(String message) {
        return new TierIssue(Severity.ERROR, message);
    }

    public static TierIssue warn(String message) {
        return new TierIssue(Severity.WARN, message);
    }

    public static TierIssue info(String message) {
        return new TierIssue(Severity.INFO, message);
    }

    public String line() {
        return "[" + severity.name() + "] " + message;
    }
}
