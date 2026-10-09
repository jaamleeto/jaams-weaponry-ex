package net.jaams.weaponry_ex.data;

import java.util.ArrayList;
import java.util.List;

public class Conditions {

    public List<ModCondition> mods = new ArrayList<>();

    public boolean isEmpty() {
        return mods == null || mods.isEmpty();
    }

    public boolean matches() {
        if (isEmpty()) {
            return true;
        }
        for (ModCondition mod : mods) {
            if (mod == null) {
                continue;
            }
            if (!mod.test()) {
                return false;
            }
        }
        return true;
    }
}
