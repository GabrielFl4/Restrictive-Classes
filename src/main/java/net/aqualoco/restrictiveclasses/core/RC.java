package net.aqualoco.restrictiveclasses.core;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RC {
    public static final String MOD_ID = "restrictive-classes";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    private RC() {}
}
