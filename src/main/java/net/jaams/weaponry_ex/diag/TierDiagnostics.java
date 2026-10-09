package net.jaams.weaponry_ex.diag;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.registry.TieredItems;
import net.jaams.weaponry_ex.registry.TieredItems.TierEntry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class TierDiagnostics {

    private static final Logger LOGGER = LogManager.getLogger(JaamsWeaponryExMod.MOD_NAME);

    private static volatile List<TierReport> reports = List.of();
    private static volatile boolean finished = false;

    private TierDiagnostics() {
    }

    public record Totals(int files, int loaded, int skipped, int errors, int warnings) {
    }

    public static void set(List<TierReport> found) {
        reports = List.copyOf(found);
    }

    public static synchronized void finish(boolean checkTags) {
        if (finished) {
            return;
        }
        finished = true;
        markLoaded();
        for (TierReport report : reports) {
            if (silenced(report)) {
                continue;
            }
            TierValidator.audit(report.definition(), report, checkTags);
        }
        print();
    }

    private static boolean silenced(TierReport report) {
        return report.isQuiet();
    }

    private static boolean disabled(TierReport report) {
        TierDefinition def = report.definition();
        return def != null && !def.enabled;
    }

    private static void markLoaded() {
        for (TierEntry entry : TieredItems.entries()) {
            boolean registered = allRegistered(entry);
            for (TierReport report : reports) {
                if (report.definition() != null
                        && report.definition().id.equals(entry.definition().id)) {
                    report.loaded(registered);
                }
            }
        }
    }

    private static boolean allRegistered(TierEntry entry) {
        for (RegistryObject<Item> item : entry.allItems()) {
            if (!item.isPresent() || !ForgeRegistries.ITEMS.containsValue(item.get())) {
                return false;
            }
        }
        return true;
    }

    private static Totals totals() {
        int loaded = 0;
        int skipped = 0;
        int errors = 0;
        int warnings = 0;
        for (TierReport report : reports) {
            if (report.definition() != null && !report.skipped()) {
                loaded++;
            } else if (report.skipped()) {
                skipped++;
            }
            errors += report.count(TierIssue.Severity.ERROR);
            warnings += report.count(TierIssue.Severity.WARN);
        }
        return new Totals(reports.size(), loaded, skipped, errors, warnings);
    }

    private static void print() {
        if (reports.isEmpty()) {
            return;
        }
        for (TierReport report : reports) {
            if (silenced(report) || disabled(report)) {
                continue;
            }
            if (report.hasProblems()) {
                LOGGER.error("  {} -> {}", report.source(), state(report));
                for (TierIssue issue : report.issues()) {
                    switch (issue.severity()) {
                        case ERROR -> LOGGER.error("      {}", issue.line());
                        case WARN -> LOGGER.warn("      {}", issue.line());
                        case INFO -> { }
                    }
                }
            } else if (!report.skipped()) {
                LOGGER.info("  {} -> {} ({})", report.source(), described(report), state(report));
            }
        }

        Totals totals = totals();
        if (totals.errors() == 0 && totals.warnings() == 0) {
            LOGGER.info("Tier definitions: {} loaded, {} skipped, no problems found",
                    totals.loaded(), totals.skipped());
        } else {
            LOGGER.info("Tier definitions: {} loaded, {} skipped, {} error(s), {} warning(s)",
                    totals.loaded(), totals.skipped(), totals.errors(), totals.warnings());
        }
    }

    private static String described(TierReport report) {
        String described = report.describe();
        return described.isEmpty() ? "no usable tier" : described;
    }

    private static String state(TierReport report) {
        if (report.skipped()) {
            return "skipped";
        }
        return report.loaded() == null ? "not registered" : report.loaded() ? "loaded" : "not registered";
    }
}
