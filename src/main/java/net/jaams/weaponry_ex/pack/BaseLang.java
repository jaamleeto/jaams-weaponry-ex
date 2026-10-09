package net.jaams.weaponry_ex.pack;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.WeaponSpec;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;

public final class BaseLang {

    private static final Logger LOGGER = LogManager.getLogger(JaamsWeaponryExMod.MOD_NAME);

    private static final String LANG_ROOT = "assets/jaams_weaponry/lang/";
    private static final String KEY_ROOT = "item.jaams_weaponry.";

    private static final String BASE_NAMESPACE = JaamsWeaponryExMod.BASE_NAMESPACE;

    private static final String TIER_PROBE_WEAPON = "dagger";

    private static final String REFERENCE_TIER = "shinerite";

    private static final String REFERENCE_MATERIAL = "wood";

    private static final String REFERENCE_GEM = "diamond";

    private static final int MIN_STEM_MATCH = 2;

    private static final Map<String, BaseLang> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> ABSENT = ConcurrentHashMap.newKeySet();

    private static final List<String> COMPARISON_WEAPONS =
            List.of("dagger", "kama", "spear", "broadsword", "greathammer", "battle_axe", "scythe");

    private final Map<String, String> values;

    private final boolean leading;

    private final int phraseWords;

    private final String stem;

    private final int[] tierWord;

    private final int ingotMaterialAt;

    private final int compressedMaterialAt;

    private final int gemMaterialAt;

    private BaseLang(Map<String, String> values, boolean leading, int phraseWords, String stem,
            int[] tierWord, int ingotMaterialAt, int compressedMaterialAt, int gemMaterialAt) {
        this.values = values;
        this.leading = leading;
        this.phraseWords = phraseWords;
        this.stem = stem;
        this.tierWord = tierWord;
        this.ingotMaterialAt = ingotMaterialAt;
        this.compressedMaterialAt = compressedMaterialAt;
        this.gemMaterialAt = gemMaterialAt;
    }

    public static String weapon(String code, String weaponName, String tierLabel) {
        BaseLang lang = of(code);
        return lang == null ? null : lang.weaponName(weaponName, tierLabel);
    }

    public static String heavyIngot(String code, String tierLabel) {
        return ingot(code, "heavy", tierLabel);
    }

    public static String doubleIngot(String code, String tierLabel) {
        return ingot(code, "double", tierLabel);
    }

    public static String heavyCompressed(String code, String word) {
        return shaped(code, "heavy", "compressed", word);
    }

    public static String doubleCompressed(String code, String word) {
        return shaped(code, "double", "compressed", word);
    }

    public static String heavyGem(String code, String word) {
        return shaped(code, "heavy", "gem", word);
    }

    public static String doubleGem(String code, String word) {
        return shaped(code, "double", "gem", word);
    }

    private static String shaped(String code, String density, String shape, String word) {
        BaseLang lang = of(code);
        return lang == null ? null : lang.shaped(density, shape, word);
    }

    private String shaped(String density, String shape, String word) {
        String exact = value(KEY_ROOT + itemId(density, shape, word));
        if (exact != null && !exact.isBlank()) {
            return exact;
        }
        int at = "gem".equals(shape) ? gemMaterialAt : compressedMaterialAt;
        String label = materialName(word);
        if (at < 0 || label == null) {
            return null;
        }
        return reword(value(KEY_ROOT + itemId(density, shape, referenceOf(shape))), at, label);
    }

    private static String itemId(String density, String shape, String word) {
        return switch (shape) {
            case "ingot" -> density + "_" + word + "_ingot";
            case "compressed" -> density + "_compressed_" + word;
            default -> density + "_" + word;
        };
    }

    private static String referenceOf(String shape) {
        return "gem".equals(shape) ? REFERENCE_GEM : REFERENCE_MATERIAL;
    }

    private String materialName(String word) {
        for (String shape : new String[] {"ingot", "compressed", "gem"}) {
            int at = switch (shape) {
                case "ingot" -> ingotMaterialAt;
                case "compressed" -> compressedMaterialAt;
                default -> gemMaterialAt;
            };
            if (at < 0) {
                continue;
            }
            String found = reword(value(KEY_ROOT + itemId("double", shape, word)), at, null);
            if (found != null && !found.isBlank()) {
                return found;
            }
        }
        return null;
    }

    private static String reword(String name, int at, String with) {
        if (name == null) {
            return null;
        }
        int[][] spans = words(name);
        if (at < 0 || spans.length <= at) {
            return null;
        }
        return with == null ? word(name, spans[at])
                : name.substring(0, spans[at][0]) + with + name.substring(spans[at][1]);
    }

    private static String ingot(String code, String density, String tierLabel) {
        BaseLang lang = of(code);
        return lang == null ? null
                : lang.atStem(lang.value(KEY_ROOT + density + "_" + REFERENCE_TIER + "_ingot"), tierLabel);
    }

    public static Set<String> shipped() {
        Set<String> codes = new LinkedHashSet<>();
        codes.add("en_us");
        try {
            URL url = JaamsWeaponryExMod.class.getClassLoader().getResource(LANG_ROOT + "en_us.json");
            if (url == null) {
                return codes;
            }
            String location = url.toString();
            if (location.startsWith("jar:")) {

                JarURLConnection connection = (JarURLConnection) url.openConnection();
                for (Enumeration<JarEntry> entries = connection.getJarFile().entries();
                        entries.hasMoreElements();) {
                    String entry = entries.nextElement().getName();
                    if (entry.startsWith(LANG_ROOT) && entry.endsWith(".json")) {
                        String code = entry.substring(LANG_ROOT.length(), entry.length() - ".json".length());
                        if (!code.isEmpty() && !code.contains("/")) {
                            codes.add(code);
                        }
                    }
                }
            } else if (location.startsWith("file:")) {
                int slash = location.lastIndexOf('/');
                if (slash > "file:".length()) {
                    File dir = new File(URI.create(location.substring(0, slash)));
                    File[] files = dir.listFiles((parent, name) -> name.endsWith(".json"));
                    if (files != null) {
                        Arrays.sort(files, Comparator.comparing(File::getName));
                        for (File file : files) {
                            String name = file.getName();
                            codes.add(name.substring(0, name.length() - ".json".length()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to list the base mod's languages, only en_us will be written", e);
        }
        return codes;
    }

    private static BaseLang of(String code) {
        if (code == null || code.isEmpty() || ABSENT.contains(code)) {
            return null;
        }
        BaseLang cached = CACHE.get(code);
        if (cached != null) {
            return cached;
        }
        BaseLang loaded = load(code);
        if (loaded == null) {
            ABSENT.add(code);
        } else {
            CACHE.put(code, loaded);
        }
        return loaded;
    }

    private static BaseLang load(String code) {
        Map<String, String> values = read(code);
        if (values.isEmpty()) {
            LOGGER.debug("Base mod ships no lang file for '{}'", code);
            return null;
        }
        List<String> catalog = new ArrayList<>();
        for (WeaponSpec weapon : WeaponSpec.WEAPONS) {
            String name = values.get(KEY_ROOT + REFERENCE_TIER + "_" + weapon.name());
            if (name != null && !name.isBlank()) {
                catalog.add(name);
            }
        }
        if (catalog.isEmpty()) {
            LOGGER.debug("Base mod lang '{}' has no '{}' catalog to mirror", code, REFERENCE_TIER);
            return null;
        }

        String head = commonPrefix(catalog);
        String tail = commonSuffix(catalog);

        boolean leading = head.length() >= tail.length();

        String affix = leading ? trimTrailingSpace(head) : trimLeadingSpace(tail);
        int phraseWords = affix.isEmpty() ? 0 : affix.split("\\s+").length;
        if (phraseWords == 0) {
            LOGGER.warn("Base mod lang '{}' shares no usable tier phrase across its {} "
                    + "reference weapons; generated names fall back to a generic form",
                    code, catalog.size());
            return new BaseLang(values, leading, 0, "", new int[0], -1, -1, -1);
        }
        return new BaseLang(values, leading, phraseWords, affix,
                tierWordMask(values, phraseWords, leading),
                materialWord(values, "ingot", REFERENCE_TIER),
                materialWord(values, "compressed", REFERENCE_MATERIAL),
                materialWord(values, "gem", REFERENCE_GEM));
    }

    private static final int SHAPE_STEM_MATCH = 2;

    private static int materialWord(Map<String, String> values, String shape, String material) {
        String light = values.get(KEY_ROOT + itemId("double", shape, material));
        String heavy = values.get(KEY_ROOT + itemId("heavy", shape, material));
        if (light == null || heavy == null) {
            return -1;
        }
        int[][] a = words(light);
        int[][] b = words(heavy);
        if (a.length != b.length) {
            return -1;
        }
        List<Integer> shared = new ArrayList<>();
        for (int i = 0; i < a.length; i++) {
            if (word(light, a[i]).equals(word(heavy, b[i]))) {
                shared.add(i);
            }
        }
        if (shared.size() == 1) {
            return shared.get(0);
        }
        if (shared.isEmpty()) {
            return -1;
        }
        for (String sibling : siblingMaterials(values, shape, material)) {
            String other = values.get(KEY_ROOT + itemId("double", shape, sibling));
            if (other == null) {
                continue;
            }
            int[][] c = words(other);
            if (c.length != a.length) {
                continue;
            }
            List<Integer> materialAt = new ArrayList<>();
            for (int i : shared) {
                String mine = word(light, a[i]);
                String theirs = word(other, c[i]);
                if (sharedPrefixLength(mine, theirs) < SHAPE_STEM_MATCH) {
                    materialAt.add(i);
                }
            }
            if (materialAt.size() == 1) {
                return materialAt.get(0);
            }
        }
        return -1;
    }

    private static List<String> siblingMaterials(Map<String, String> values, String shape, String material) {
        String prefix = KEY_ROOT + "double_" + ("compressed".equals(shape) ? "compressed_" : "");
        String suffix = "ingot".equals(shape) ? "_ingot" : "";
        List<String> siblings = new ArrayList<>();
        for (String key : values.keySet()) {
            if (!key.startsWith(prefix) || !key.endsWith(suffix)
                    || key.length() <= prefix.length() + suffix.length()) {
                continue;
            }
            String name = key.substring(prefix.length(), key.length() - suffix.length());
            if (!name.isEmpty() && !name.contains("_") && !name.equals(material)) {
                siblings.add(name);
            }
        }
        return siblings;
    }

    private static int[] tierWordMask(Map<String, String> values, int phraseWords, boolean leading) {
        int[] mask = new int[phraseWords];
        Arrays.fill(mask, 1);
        List<String> tiers = baseTiers(values);
        for (String weapon : COMPARISON_WEAPONS) {
            String reference = values.get(KEY_ROOT + REFERENCE_TIER + "_" + weapon);
            if (reference == null) {
                continue;
            }
            int[][] referenceWords = words(reference);
            for (String tier : tiers) {
                if (REFERENCE_TIER.equals(tier)) {

                    continue;
                }
                String other = values.get(KEY_ROOT + tier + "_" + weapon);
                if (other == null) {
                    continue;
                }
                int[][] otherWords = words(other);
                if (otherWords.length != referenceWords.length) {
                    continue;
                }
                for (int i = 0; i < phraseWords; i++) {
                    int at = leading ? i : referenceWords.length - phraseWords + i;
                    int there = leading ? i : otherWords.length - phraseWords + i;
                    if (word(reference, referenceWords[at]).equals(word(other, otherWords[there]))) {
                        mask[i] = 0;
                    }
                }
            }
        }
        return mask;
    }

    private static List<String> baseTiers(Map<String, String> values) {
        List<String> shared = null;
        for (String weapon : COMPARISON_WEAPONS) {
            String suffix = "_" + weapon;
            Set<String> tiers = new LinkedHashSet<>();
            for (String key : values.keySet()) {
                if (key.startsWith(KEY_ROOT) && key.endsWith(suffix)) {
                    String tier = key.substring(KEY_ROOT.length(), key.length() - suffix.length());
                    if (!tier.isEmpty() && !tier.contains("_")) {
                        tiers.add(tier);
                    }
                }
            }
            if (tiers.isEmpty()) {
                continue;
            }
            if (shared == null) {
                shared = new ArrayList<>(tiers);
            } else {
                shared.retainAll(tiers);
            }
        }
        return shared == null ? List.of() : shared;
    }

    private static Map<String, String> read(String code) {
        String path = LANG_ROOT + code + ".json";
        try (InputStream in = JaamsWeaponryExMod.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return Map.of();
            }
            JsonObject root = JsonParser
                    .parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    values.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            return values;
        } catch (Exception e) {
            LOGGER.warn("Failed to read base mod lang {}", path, e);
            return Map.of();
        }
    }

    private String value(String key) {
        return values.get(key);
    }

    public static Boolean ships(String path) {
        BaseLang lang = of("en_us");
        if (lang == null) {
            return null;
        }
        return lang.values.containsKey(KEY_ROOT + path);
    }

    public static boolean ships(String path, boolean fallbackToLang) {
        Boolean registered = registered(path);
        if (registered != null) {
            return registered;
        }
        return fallbackToLang && Boolean.TRUE.equals(ships(path));
    }

    public static Boolean registered(String path) {
        try {
            ResourceLocation key = ResourceLocation.tryParse(BASE_NAMESPACE + ":" + path);
            if (key == null) {
                return null;
            }
            var items = net.minecraftforge.registries.ForgeRegistries.ITEMS;
            if (items == null || items.isEmpty()) {
                return null;
            }
            return items.containsKey(key);
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    public static List<String> registeredTiers() {
        Set<String> tiers = new LinkedHashSet<>();
        try {
            var items = net.minecraftforge.registries.ForgeRegistries.ITEMS;
            if (items == null) {
                return List.of();
            }
            String marker = "_" + TIER_PROBE_WEAPON;
            for (ResourceLocation id : items.getKeys()) {
                if (!BASE_NAMESPACE.equals(id.getNamespace())) {
                    continue;
                }
                String path = id.getPath();
                int cut = path.indexOf(marker);
                if (cut <= 0) {
                    continue;
                }
                tiers.add(path.substring(0, cut));
            }
        } catch (Exception | LinkageError e) {
            return List.of();
        }
        return List.copyOf(tiers);
    }

    private String weaponName(String weaponName, String tierLabel) {
        List<String> names = namesOf(weaponName);
        if (names.isEmpty()) {
            return null;
        }
        return atEdge(names.get(0), names, tierLabel);
    }

    private List<String> namesOf(String weaponName) {
        List<String> names = new ArrayList<>();
        String atReference = values.get(KEY_ROOT + REFERENCE_TIER + "_" + weaponName);
        if (atReference != null) {
            names.add(atReference);
        }
        for (String tier : tiersOf(weaponName)) {
            String name = values.get(KEY_ROOT + tier + "_" + weaponName);
            if (name != null && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private List<String> tiersOf(String weaponName) {
        String suffix = "_" + weaponName;
        Set<String> tiers = new LinkedHashSet<>();
        for (String key : values.keySet()) {
            if (key.startsWith(KEY_ROOT) && key.endsWith(suffix)) {
                String tier = key.substring(KEY_ROOT.length(), key.length() - suffix.length());
                if (!tier.isEmpty() && !tier.contains("_")) {
                    tiers.add(tier);
                }
            }
        }
        List<String> sorted = new ArrayList<>(tiers);
        Collections.sort(sorted);
        return sorted;
    }

    private String atEdge(String baseName, List<String> everyTier, String tierLabel) {
        if (baseName == null || phraseWords == 0) {
            return null;
        }
        int[][] spans = words(baseName);
        if (spans.length < phraseWords) {
            return null;
        }
        int from = leading ? 0 : spans.length - phraseWords;
        if (!agreesAround(baseName, spans, from, from + phraseWords, everyTier)) {
            return null;
        }
        return swap(baseName, spans, from, from + phraseWords, tierLabel);
    }

    private String atStem(String baseName, String tierLabel) {
        if (baseName == null || phraseWords == 0) {
            return null;
        }
        int[][] spans = words(baseName);
        if (spans.length < phraseWords) {
            return null;
        }
        int best = -1;
        int bestScore = MIN_STEM_MATCH - 1;
        for (int i = 0; i + phraseWords <= spans.length; i++) {
            int score = stemMatch(baseName.substring(spans[i][0], spans[i + phraseWords - 1][1]));
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        if (best < 0) {
            return null;
        }
        return swap(baseName, spans, best, best + phraseWords, tierLabel);
    }

    private boolean agreesAround(String baseName, int[][] spans, int from, int to,
            List<String> everyTier) {
        for (String other : everyTier) {
            if (other.equals(baseName)) {
                continue;
            }
            int[][] otherSpans = words(other);
            if (otherSpans.length != spans.length) {
                continue;
            }
            for (int i = 0; i < spans.length; i++) {
                if (i >= from && i < to) {
                    continue;
                }
                if (!word(baseName, spans[i]).equals(word(other, otherSpans[i]))) {
                    return false;
                }
            }
        }
        return true;
    }

    private String swap(String baseName, int[][] spans, int from, int to, String tierLabel) {
        int start = spans[from][0];
        int end = spans[to - 1][1];
        StringBuilder out = new StringBuilder();
        int cursor = start;
        boolean placed = false;
        for (int i = from; i < to; i++) {
            if (tierWord[i - from] == 0) {
                out.append(baseName, cursor, spans[i][1]);
            } else if (!placed) {

                out.append(baseName, cursor, spans[i][0]).append(label(tierLabel));
                placed = true;
            }
            cursor = spans[i][1];
        }
        out.append(baseName, cursor, end);
        return baseName.substring(0, start) + out + baseName.substring(end);
    }

    private static String word(String s, int[] span) {
        return s.substring(span[0], span[1]);
    }

    private String label(String tierLabel) {
        return tierLabel == null || tierLabel.isBlank() ? REFERENCE_TIER : tierLabel;
    }

    private int stemMatch(String candidate) {
        int limit = Math.min(candidate.length(), stem.length());
        int i = 0;
        while (i < limit
                && Character.toLowerCase(candidate.charAt(i)) == Character.toLowerCase(stem.charAt(i))) {
            i++;
        }
        return i;
    }

    private static int[][] words(String s) {
        List<int[]> spans = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i >= s.length()) {
                break;
            }
            int start = i;
            while (i < s.length() && !Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            spans.add(new int[] { start, i });
        }
        return spans.toArray(new int[0][]);
    }

    private static String commonPrefix(List<String> catalog) {
        String shared = catalog.get(0);
        for (String name : catalog) {
            shared = shared.substring(0, sharedPrefixLength(shared, name));
        }
        return shared;
    }

    private static String commonSuffix(List<String> catalog) {
        String shared = catalog.get(0);
        for (String name : catalog) {
            int length = commonSuffixLength(shared, name);
            shared = shared.substring(shared.length() - length);
        }
        return shared;
    }

    private static int sharedPrefixLength(String a, String b) {
        int limit = Math.min(a.length(), b.length());
        int i = 0;
        while (i < limit && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }

    private static int commonSuffixLength(String a, String b) {
        int limit = Math.min(a.length(), b.length());
        int i = 0;
        while (i < limit && a.charAt(a.length() - 1 - i) == b.charAt(b.length() - 1 - i)) {
            i++;
        }
        return i;
    }

    private static String trimTrailingSpace(String s) {
        int i = s.length();
        while (i > 0 && Character.isWhitespace(s.charAt(i - 1))) {
            i--;
        }
        return s.substring(0, i);
    }

    private static String trimLeadingSpace(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
    }
}
