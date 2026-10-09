package net.jaams.weaponry_ex.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TagSection {

    public List<String> tier = new ArrayList<>();

    public List<String> materials = new ArrayList<>();

    public Map<String, List<String>> items = new LinkedHashMap<>();

    public TierTagSection tier_tag = new TierTagSection();

    public boolean isEmpty() {
        return tier.isEmpty() && materials.isEmpty() && items.isEmpty() && tier_tag.isDefault();
    }

    public List<String> tagsFor(WeaponSpec spec) {
        if (spec == null) {
            return List.of();
        }
        List<String> tags = new ArrayList<>(tier);
        for (Map.Entry<String, List<String>> entry : items.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            if (entry.getValue() == null || key.isEmpty()) {
                continue;
            }
            if (key.equals(spec.name()) || key.equals(spec.registryName())) {
                tags.addAll(entry.getValue());
            }
        }
        return tags;
    }
}
