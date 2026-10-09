package net.jaams.weaponry_ex.loader;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.ItemFilters;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.WeaponSpec;
import net.jaams.weaponry_ex.diag.TierReport;
import net.jaams.weaponry_ex.diag.TierValidator;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.locating.IModFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class TierLoader {

    private static final Logger LOGGER = LogManager.getLogger(JaamsWeaponryExMod.MOD_NAME);

    public static final String TIER_FOLDER = "data/jaams_weaponry_ex/jaams/tiers";

    public static final String ITEM_FOLDER = "data/jaams_weaponry_ex/jaams/items";

    private static final String ANCHOR = TIER_FOLDER + "/.anchor";

    private static final String ITEM_ANCHOR = ITEM_FOLDER + "/.anchor";

    private static final String CONFIG_TIER = "jaams/weaponry_ex/tier";

    private static final String CONFIG_ITEM = "jaams/weaponry_ex/item";

    private static volatile List<TierDefinition> tiers = List.of();
    private static volatile Scan result = new Scan(List.of(), List.of());
    private static volatile boolean loaded = false;

    private TierLoader() {
    }

    public record Scan(List<TierDefinition> tiers, List<TierReport> reports) {
    }

    public static synchronized Scan load() {
        if (loaded) {
            return result;
        }
        loaded = true;
        result = scan();
        tiers = result.tiers();
        return result;
    }

    public static synchronized Scan scan() {
        List<TierDefinition> result = new ArrayList<>();
        List<TierReport> reports = new ArrayList<>();
        readConfigFolder(CONFIG_TIER, false, result, reports);
        readConfigFolder(CONFIG_ITEM, true, result, reports);
        readOwnMod(false, result, reports);
        readOwnMod(true, result, reports);
        int anchorCount = scanClassPath(result, reports);
        if (result.isEmpty() && reports.isEmpty()) {
            scanDevFolders(result, reports);
        }
        LOGGER.debug("Tier scan: {} anchor(s) found, {} definition(s) parsed", anchorCount, result.size());
        return new Scan(List.copyOf(result), List.copyOf(reports));
    }

    private static void readConfigFolder(String subpath, boolean standalone,
            List<TierDefinition> out, List<TierReport> reports) {
        for (Path dir : configCandidates(subpath)) {
            if (Files.isDirectory(dir)) {
                LOGGER.debug("Scanning {} folder (runtime config): {}", standalone ? "item" : "tier", dir);
                readDirContainer(dir, standalone, out, reports);
            }
        }
    }

    static List<Path> configCandidates() {
        return configCandidates(CONFIG_TIER);
    }

    static List<Path> configCandidates(String subpath) {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        try {
            candidates.add(FMLPaths.CONFIGDIR.get().resolve(subpath).normalize());
        } catch (Exception | LinkageError ignored) {
            LOGGER.debug("Forge's config folder is not available yet, falling back to the "
                    + "working directory");
        }
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            Path cwd = Path.of(userDir).toAbsolutePath().normalize();
            candidates.add(cwd.resolve("run/config").resolve(subpath).normalize());
            candidates.add(cwd.resolve("config").resolve(subpath).normalize());
            candidates.add(cwd.resolve("..").resolve("config").resolve(subpath).normalize());
        }
        return List.copyOf(candidates);
    }

    private static void readOwnMod(boolean standalone, List<TierDefinition> out,
            List<TierReport> reports) {
        String folder = standalone ? ITEM_FOLDER : TIER_FOLDER;
        try {
            IModFile modFile = ModList.get().getModFileById(JaamsWeaponryExMod.MODID).getFile();
            Path dir = modFile.findResource(folder.split("/"));
            if (dir != null && Files.isDirectory(dir)) {
                LOGGER.debug("Scanning {} folder in mod file '{}': {}", standalone ? "item" : "tier",
                        modFile.getFileName(), dir);
                readDirContainer(dir, standalone, out, reports);
            } else {
                LOGGER.debug("No folder '{}' inside mod file '{}'", folder, modFile.getFileName());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read {} definitions from own mod file", standalone ? "item" : "tier", e);
        }
    }

    private static int scanClassPath(List<TierDefinition> out, List<TierReport> reports) {
        int anchorCount = 0;
        for (Anchor anchor : List.of(new Anchor(ANCHOR, false), new Anchor(ITEM_ANCHOR, true))) {
            try {
                Enumeration<URL> found =
                        JaamsWeaponryExMod.class.getClassLoader().getResources(anchor.path());
                while (found.hasMoreElements()) {
                    URL url = found.nextElement();
                    anchorCount++;
                    try {
                        readContainer(url, anchor.standalone(), out, reports);
                    } catch (Exception e) {
                        LOGGER.error("Failed to read definitions from {}", url, e);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to scan class path for definitions at '{}'", anchor.path(), e);
            }
        }
        return anchorCount;
    }

    private record Anchor(String path, boolean standalone) {
    }

    private static void scanDevFolders(List<TierDefinition> out, List<TierReport> reports) {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        for (String base : List.of("build/resources/main", "src/main/resources")) {
            for (Path level : List.of(cwd, cwd.resolve(".."), cwd.resolve("../.."))) {
                candidates.add(level.resolve(base).normalize());
            }
        }
        for (Path dir : candidates) {
            if (Files.isDirectory(dir)) {
                LOGGER.debug("Scanning tier folder (dev fallback): {}", dir);
                readDirContainer(dir, false, out, reports);
            }
        }
    }

    private static void readContainer(URL anchor, boolean standalone, List<TierDefinition> out,
            List<TierReport> reports) throws Exception {
        if ("jar".equalsIgnoreCase(anchor.getProtocol())) {
            readJarContainer(anchor.toExternalForm(), standalone, out, reports);
        } else if ("file".equalsIgnoreCase(anchor.getProtocol())) {
            Path parent = Path.of(anchor.toURI()).getParent();
            readDirContainer(parent, standalone, out, reports);
        } else {
            LOGGER.warn("Unsupported definition anchor URL scheme '{}': {}", anchor.getProtocol(),
                    anchor);
        }
    }

    private static void readJarContainer(String externalForm, boolean standalone,
            List<TierDefinition> out, List<TierReport> reports) {

        try {
            int bang = externalForm.indexOf('!');
            String jarPart = bang >= 0 ? externalForm.substring(4, bang) : externalForm.substring(4);
            Path jarFile = Path.of(new URI(jarPart));
            try (ZipFile zip = new ZipFile(jarFile.toFile())) {
                readZipContainer(zip, standalone, out, reports);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read definitions from {}", externalForm, e);
        }
    }

    private static void readZipContainer(ZipFile zip, boolean standalone, List<TierDefinition> out,
            List<TierReport> reports) {
        String folder = standalone ? ITEM_FOLDER : TIER_FOLDER;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!name.startsWith(folder + "/") || !name.endsWith(".json") || entry.isDirectory()) {
                continue;
            }
            String source = jarName(zip) + "!" + name;
            try (InputStream in = zip.getInputStream(entry)) {
                accept(new String(in.readAllBytes(), StandardCharsets.UTF_8), source, standalone, out,
                        reports);
            } catch (Exception e) {
                failedToRead(source, out, reports, e);
            }
        }
    }

    private static void readDirContainer(Path dir, boolean standalone, List<TierDefinition> out,
            List<TierReport> reports) {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(files::add);
        } catch (Exception e) {
            LOGGER.error("Failed to list {} folder {}", standalone ? "item" : "tier", dir, e);
            return;
        }

        files.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
        String folder = dir.getFileName() == null ? dir.toString() : dir.getFileName().toString();
        for (Path file : files) {
            String source = folder + "/" + file.getFileName();
            try {
                accept(Files.readString(file, StandardCharsets.UTF_8), source, standalone, out, reports);
            } catch (Exception e) {
                failedToRead(source, out, reports, e);
            }
        }
    }

    private static void accept(String json, String source, boolean standalone, List<TierDefinition> out,
            List<TierReport> reports) {
        TierReport report = TierValidator.parse(json, source, standalone);
        reports.add(report);
        TierDefinition def = report.definition();
        if (def == null || report.skipped()) {
            return;
        }
        if (standalone && !pinSingleWeapon(def, report)) {
            return;
        }
        if (def.conditions != null && !def.conditions.matches()) {
            report.skip("conditions are not met, so this definition is not loaded");
            return;
        }
        for (TierDefinition loaded : out) {
            if (loaded.id.equals(def.id)) {
                report.skip("another tier or item already uses the id '" + def.id + "', so this file is "
                        + "left out");
                return;
            }
        }

        TierValidator.checkNameCollisions(def, out, report);
        out.add(def);
    }

    private static boolean pinSingleWeapon(TierDefinition def, TierReport report) {
        String name = def.weapon == null ? "" : def.weapon.trim();
        WeaponSpec spec = name.isEmpty() ? null : WeaponSpec.byName(name);
        if (spec == null) {
            report.error("this file is in the 'item' folder, so it describes a single weapon and "
                    + "needs 'weapon' with one of: " + String.join(", ", knownWeapons())
                    + " - the file is skipped");
            report.skip("no usable 'weapon' was given, so the file is left out");
            return false;
        }
        if (!def.tier.only_items.isEmpty() && !def.tier.only_items.contains(spec.name())) {
            report.warn("'tier.only_items' is ignored here, because the 'item' folder already fixes "
                    + "this file to the single weapon '" + spec.name() + "'");
        }
        if (ItemFilters.matchesName(def.tier.exclude_items, spec.name())) {
            report.warn("'tier.exclude_items' would remove '" + spec.name() + "', so it is ignored "
                    + "here: an 'item' file always builds the weapon it names");
        }
        def.tier.exclude_items = new ArrayList<>();
        report.info("'item' definition: only '" + spec.name() + "' is registered, without the two "
                + "material items a tier would also create");
        def.pinWeapon(spec);
        return true;
    }

    private static List<String> knownWeapons() {
        List<String> names = new ArrayList<>();
        for (WeaponSpec spec : WeaponSpec.WEAPONS) {
            names.add(spec.name());
        }
        return names;
    }

    private static void failedToRead(String source, List<TierDefinition> out, List<TierReport> reports,
            Exception e) {
        LOGGER.error("Failed to read tier file {}", source, e);
        TierReport report = new TierReport(source, "");
        report.error("could not be read: " + e.getMessage() + " - the file is skipped");
        reports.add(report);
    }

    public static List<TierDefinition> getTiers() {
        return Collections.unmodifiableList(tiers);
    }

    private static String jarName(ZipFile zip) {
        try {
            return Path.of(zip.getName()).getFileName().toString();
        } catch (Exception e) {
            return zip.getName();
        }
    }
}
