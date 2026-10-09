package net.jaams.weaponry_ex.data;

import net.minecraftforge.fml.ModList;

public class ModCondition {

    public String modid;

    public boolean required = true;

    public String type = "mod_loaded";

    public String mod;

    public String id;

    public boolean test() {
        String conditionId = modid;
        if (conditionId == null || conditionId.trim().isEmpty()) {
            conditionId = mod;
        }
        if (conditionId == null || conditionId.trim().isEmpty()) {
            conditionId = id;
        }
        if (conditionId == null || conditionId.trim().isEmpty()) {
            return true;
        }
        String cId = conditionId.trim();
        boolean loaded = ModList.get() != null && ModList.get().isLoaded(cId);
        if (required) {
            return loaded;
        }
        return !loaded;
    }
}
