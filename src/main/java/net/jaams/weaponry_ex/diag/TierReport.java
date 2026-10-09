package net.jaams.weaponry_ex.diag;

import net.jaams.weaponry_ex.data.TierDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class TierReport {

    private final String source;
    private final String rawJson;
    private final List<TierIssue> issues = new ArrayList<>();

    private TierDefinition definition;
    private boolean skipped;
    private boolean audited;
    private boolean quiet;
    private Boolean loaded;

    public TierReport(String source, String rawJson) {
        this.source = source;
        this.rawJson = rawJson == null ? "" : rawJson;
    }

    public String source() {
        return source;
    }

    public String rawJson() {
        return rawJson;
    }

    public TierDefinition definition() {
        return definition;
    }

    public String id() {
        return definition == null ? "" : definition.id;
    }

    public List<TierIssue> issues() {
        return Collections.unmodifiableList(issues);
    }

    public boolean skipped() {
        return skipped;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void quiet() {
        this.quiet = true;
    }

    public boolean audited() {
        return audited;
    }

    public Boolean loaded() {
        return loaded;
    }

    public void define(TierDefinition definition) {
        this.definition = definition;
    }

    public void skip(String reason) {
        this.skipped = true;
        info(reason);
    }

    public void markAudited() {
        this.audited = true;
    }

    public void loaded(boolean loaded) {
        this.loaded = loaded;
    }

    public void error(String message) {
        if (quiet) {
            return;
        }
        issues.add(TierIssue.error(message));
    }

    public void warn(String message) {
        if (quiet) {
            return;
        }
        issues.add(TierIssue.warn(message));
    }

    public void info(String message) {
        if (quiet) {
            return;
        }
        issues.add(TierIssue.info(message));
    }

    public int count(TierIssue.Severity severity) {
        int total = 0;
        for (TierIssue issue : issues) {
            if (issue.severity() == severity) {
                total++;
            }
        }
        return total;
    }

    public boolean hasProblems() {
        return count(TierIssue.Severity.ERROR) > 0 || count(TierIssue.Severity.WARN) > 0;
    }

    public String describe() {
        if (definition == null) {
            return "";
        }
        String color = Integer.toHexString(definition.getColor() & 0xFFFFFF).toUpperCase(Locale.ROOT);
        return definition.id + "  (" + definition.getRecipeType().name().toLowerCase(Locale.ROOT)
                + ", " + definition.properties.durability + " uses, #" + color + ")";
    }
}
