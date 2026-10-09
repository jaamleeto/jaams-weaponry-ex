package net.jaams.weaponry_ex.data;

import java.util.ArrayList;
import java.util.List;

public class TierTagSection {

    public boolean enabled = true;

    public String name;

    public List<String> exclude_items = new ArrayList<>();

    public List<String> include_items = new ArrayList<>();

    public boolean isDefault() {
        return enabled && (name == null || name.isBlank())
                && exclude_items.isEmpty() && include_items.isEmpty();
    }
}
