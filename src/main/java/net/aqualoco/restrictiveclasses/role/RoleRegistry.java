package net.aqualoco.restrictiveclasses.role;

import net.aqualoco.restrictiveclasses.core.RC;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;



import java.util.*;

public final class RoleRegistry {
    private static final Map<String, Role> ROLES = new HashMap<>();
    private static final Map<String, RolePermissions> PERMS = new HashMap<>();

    // role padrão (nega tudo porque não tem allow)
    public static final String DEFAULT_ROLE = "unassigned";

    public static void clear() {
        ROLES.clear();
        PERMS.clear();
    }

    public static void put(Role r) {
        ROLES.put(r.id(), r);

        // ===== ALLOW =====
        Set<Block> allowBlocks = RolePermissions.<Block>newSet();
        for (String bId : r.allow().blocks()) {
            Identifier id = Identifier.of(bId);
            if (Registries.BLOCK.containsId(id)) {
                allowBlocks.add(Registries.BLOCK.get(id));
            }
        }

        Set<Item> allowItems = RolePermissions.<Item>newSet();
        for (String iId : r.allow().items()) {
            Identifier id = Identifier.of(iId);
            if (Registries.ITEM.containsId(id)) {
                allowItems.add(Registries.ITEM.get(id));
            }
        }

        Set<Block> allowUseBlocks = RolePermissions.<Block>newSet();
        for (String bId : r.allow().use_blocks()) {
            Identifier id = Identifier.of(bId);
            if (Registries.BLOCK.containsId(id)) {
                allowUseBlocks.add(Registries.BLOCK.get(id));
            }
        }

        // ===== DENY =====
        Set<Block> denyBlocks = RolePermissions.<Block>newSet();
        for (String bId : r.deny().blocks()) {
            Identifier id = Identifier.of(bId);
            if (Registries.BLOCK.containsId(id)) {
                denyBlocks.add(Registries.BLOCK.get(id));
            }
        }

        Set<Item> denyItems = RolePermissions.<Item>newSet();
        for (String iId : r.deny().items()) {
            Identifier id = Identifier.of(iId);
            if (Registries.ITEM.containsId(id)) {
                denyItems.add(Registries.ITEM.get(id));
            }
        }

        Set<Block> denyUseBlocks = RolePermissions.<Block>newSet();
        for (String bId : r.deny().use_blocks()) {
            Identifier id = Identifier.of(bId);
            if (Registries.BLOCK.containsId(id)) {
                denyUseBlocks.add(Registries.BLOCK.get(id));
            }
        }

        RolePermissions perms = new RolePermissions(
                r.allow().block_tags().stream().map(RolePermissions::blockTag).toList(),
                allowBlocks,
                r.allow().item_tags().stream().map(RolePermissions::itemTag).toList(),
                allowItems,
                r.allow().use_block_tags().stream().map(RolePermissions::blockTag).toList(),
                allowUseBlocks,
                r.deny().block_tags().stream().map(RolePermissions::blockTag).toList(),
                denyBlocks,
                r.deny().item_tags().stream().map(RolePermissions::itemTag).toList(),
                denyItems,
                r.deny().use_block_tags().stream().map(RolePermissions::blockTag).toList(),
                denyUseBlocks,
                r.controls() == null ? Role.Controls.defaults() : r.controls()
        );

        PERMS.put(r.id(), perms);
        RC.LOG.info("[RC] Registrada role '{}'", r.id());
    }

    public static Collection<Role> all() { return Collections.unmodifiableCollection(ROLES.values()); }
    public static Set<String> ids() { return Collections.unmodifiableSet(ROLES.keySet()); }

    public static Role get(String id) { return ROLES.get(id); }
    public static RolePermissions perms(String id) { return PERMS.get(id); }
}
