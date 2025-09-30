package net.aqualoco.restrictiveclasses.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.aqualoco.restrictiveclasses.core.RC;
import net.aqualoco.restrictiveclasses.role.Role;
import net.aqualoco.restrictiveclasses.role.RolePermissions;
import net.aqualoco.restrictiveclasses.role.RoleRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public final class ClientRules {
    private static final Gson GSON = new GsonBuilder().create();
    private static final ClientRules INSTANCE = new ClientRules();

    public static ClientRules get() { return INSTANCE; }

    private long rulesVersion = -1L;
    private final Map<String, Role> roles = new HashMap<>();
    private final Map<String, RolePermissions> perms = new HashMap<>();
    private String currentRoleId = "unassigned";
    private long lastMsgAt = 0L;
    private static final long MSG_COOLDOWN_MS = 300;

    private ClientRules() {}

    public boolean isReady() {
        return rulesVersion >= 0 && !roles.isEmpty();
    }

    // ---------- sync ----------
    public void applyRulesSnapshot(long version, List<String> roleJsons) {
        if (version == this.rulesVersion && !roles.isEmpty()) return;
        roles.clear(); perms.clear();
        for (String json : roleJsons) {
            Role r = GSON.fromJson(json, Role.class);
            roles.put(r.id(), r);
            perms.put(r.id(), buildPerms(r));
        }
        this.rulesVersion = version;
        RC.LOG.info("[RC/Client] snapshot aplicado: roles={}", roles.size());
    }

    public void applyRole(String roleId) {
        this.currentRoleId = roleId;
    }

    // ---------- prechecks ----------
    public Result canBreak(BlockState state) {
        if (!isReady()) return Result.PENDING;
        var p = perms.get(currentRoleId);
        if (p == null) return Result.PENDING;
        if (!p.controls().break_block()) return Result.ALLOW;

        boolean ok = isAllowedBlock(state, p.allowBlocks, p.allowBlockTags)
                && !isDeniedBlock(state, p.denyBlocks, p.denyBlockTags);
        if (ok) return Result.ALLOW;
        return rolesThatAllowBreak(state).isEmpty() ? Result.DISABLED : Result.REQUIRE;
    }

    public Result canUseItem(ItemStack stack) {
        if (!isReady()) return Result.PENDING;
        var p = perms.get(currentRoleId);
        if (p == null) return Result.PENDING;
        if (!p.controls().use_item()) return Result.ALLOW;

        // 1) regras de uso do ITEM
        boolean deniedByItem = p.denyItems.contains(stack.getItem()) || inAny(stack, p.denyItemTags);
        boolean allowedByItem = p.allowItems.contains(stack.getItem()) || inAny(stack, p.allowItemTags);

        // 2) se for BlockItem, também avaliamos "use_block" (abrir/colocar/interagir)
        boolean deniedByBlock = false;
        boolean allowedByBlock = false;
        if (stack.getItem() instanceof net.minecraft.item.BlockItem bi) {
            Block b = bi.getBlock();
            deniedByBlock = p.denyUseBlocks.contains(b) || inAny(b, p.denyUseBlockTags);
            allowedByBlock = p.allowUseBlocks.contains(b) || inAny(b, p.allowUseBlockTags);
        }

        // deny tem prioridade
        if (deniedByItem || deniedByBlock) return Result.REQUIRE;

        if (allowedByItem || allowedByBlock) return Result.ALLOW;

        // não permitido por esta role → ver se outra permitiria
        return rolesThatAllowUseItem(stack).isEmpty() ? Result.DISABLED : Result.REQUIRE;
    }


    public Result canUseBlock(BlockState state) {
        if (!isReady()) return Result.PENDING;
        var p = perms.get(currentRoleId);
        if (p == null) return Result.PENDING;
        if (!p.controls().use_block()) return Result.ALLOW;

        boolean denied = p.denyUseBlocks.contains(state.getBlock()) || inAny(state, p.denyUseBlockTags);
        boolean ok = p.allowUseBlocks.contains(state.getBlock()) || inAny(state, p.allowUseBlockTags);
        if (denied) return Result.REQUIRE;
        if (ok) return Result.ALLOW;
        return rolesThatAllowUseBlock(state).isEmpty() ? Result.DISABLED : Result.REQUIRE;
    }

    // ---------- mensagens ----------
    public void showDeniedMessageForBreak(BlockState state) { showMessage(rolesThatAllowBreak(state)); }
    public void showDeniedMessageForUseItem(ItemStack stack) { showMessage(rolesThatAllowUseItem(stack)); }
    public void showDeniedMessageForUseBlock(BlockState state) { showMessage(rolesThatAllowUseBlock(state)); }
    public void showDisabledMessage() { showRaw(Text.literal("§cDesabilitado nesse mundo")); }
    public void showSyncingMessage() { showRaw(Text.literal("§eSincronizando regras…")); }

    private void showMessage(List<String> rolesThatAllow) {
        if (rolesThatAllow.isEmpty()) { showDisabledMessage(); return; }
        String names = String.join(" / ", rolesThatAllow);
        showRaw(Text.literal("§cRequer: " + names));
    }

    private void showRaw(Text text) {
        long now = Util.getMeasuringTimeMs();
        if (now - lastMsgAt < MSG_COOLDOWN_MS) return;
        lastMsgAt = now;
        var client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null) client.inGameHud.setOverlayMessage(text, false);
    }

    // ---------- helpers ----------
    private static boolean isAllowedBlock(BlockState state, Set<Block> allowBlocks, List<TagKey<Block>> allowTags) {
        if (allowBlocks.contains(state.getBlock())) return true;
        for (var t : allowTags) if (state.isIn(t)) return true;
        return false;
    }

    private static boolean isDeniedBlock(BlockState state, Set<Block> denyBlocks, List<TagKey<Block>> denyTags) {
        if (denyBlocks.contains(state.getBlock())) return true;
        for (var t : denyTags) if (state.isIn(t)) return true;
        return false;
    }

    private static boolean inAny(ItemStack stack, List<TagKey<Item>> tags) {
        for (var t : tags) if (stack.isIn(t)) return true;
        return false;
    }
    private static boolean inAny(BlockState state, List<TagKey<Block>> tags) {
        for (var t : tags) if (state.isIn(t)) return true;
        return false;
    }

    private static RolePermissions buildPerms(Role r) {
        // Resolve ids e tags no lado do cliente (igual ao servidor)
        var allowBlocks = RolePermissions.<Block>newSet();
        for (String bId : r.allow().blocks()) {
            var id = Identifier.of(bId);
            if (Registries.BLOCK.containsId(id)) allowBlocks.add(Registries.BLOCK.get(id));
        }
        var allowItems = RolePermissions.<Item>newSet();
        for (String iId : r.allow().items()) {
            var id = Identifier.of(iId);
            if (Registries.ITEM.containsId(id)) allowItems.add(Registries.ITEM.get(id));
        }
        var allowUseBlocks = RolePermissions.<Block>newSet();
        for (String bId : r.allow().use_blocks()) {
            var id = Identifier.of(bId);
            if (Registries.BLOCK.containsId(id)) allowUseBlocks.add(Registries.BLOCK.get(id));
        }
        var denyBlocks = RolePermissions.<Block>newSet();
        for (String bId : r.deny().blocks()) {
            var id = Identifier.of(bId);
            if (Registries.BLOCK.containsId(id)) denyBlocks.add(Registries.BLOCK.get(id));
        }
        var denyItems = RolePermissions.<Item>newSet();
        for (String iId : r.deny().items()) {
            var id = Identifier.of(iId);
            if (Registries.ITEM.containsId(id)) denyItems.add(Registries.ITEM.get(id));
        }
        var denyUseBlocks = RolePermissions.<Block>newSet();
        for (String bId : r.deny().use_blocks()) {
            var id = Identifier.of(bId);
            if (Registries.BLOCK.containsId(id)) denyUseBlocks.add(Registries.BLOCK.get(id));
        }

        return new RolePermissions(
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
    }

    private List<String> rolesThatAllowBreak(BlockState state) {
        return roles.values().stream()
                .filter(r -> {
                    var p = perms.get(r.id());
                    if (p == null || !p.controls().break_block()) return false;
                    return !isDeniedBlock(state, p.denyBlocks, p.denyBlockTags)
                            && isAllowedBlock(state, p.allowBlocks, p.allowBlockTags);
                })
                .map(Role::display_name)
                .limit(4) // não poluir overlay
                .collect(Collectors.toList());
    }

    private List<String> rolesThatAllowUseItem(ItemStack stack) {
        boolean isBlockItem = stack.getItem() instanceof net.minecraft.item.BlockItem;
        Block block = isBlockItem ? ((net.minecraft.item.BlockItem) stack.getItem()).getBlock() : null;

        return roles.values().stream().filter(r -> {
            var p = perms.get(r.id());
            if (p == null) return false;

            // para item normal: exige governar use_item;
            // para BlockItem: aceita governar use_item OU use_block
            boolean governs = isBlockItem ? (p.controls().use_item() || p.controls().use_block())
                    : p.controls().use_item();
            if (!governs) return false;

            boolean deniedByItem = p.denyItems.contains(stack.getItem()) || inAny(stack, p.denyItemTags);
            boolean allowedByItem = p.allowItems.contains(stack.getItem()) || inAny(stack, p.allowItemTags);

            boolean deniedByBlock = false, allowedByBlock = false;
            if (isBlockItem && block != null) {
                deniedByBlock = p.denyUseBlocks.contains(block) || inAny(block, p.denyUseBlockTags);
                allowedByBlock = p.allowUseBlocks.contains(block) || inAny(block, p.allowUseBlockTags);
            }

            if (deniedByItem || deniedByBlock) return false; // deny tem prioridade
            return allowedByItem || allowedByBlock;
        }).map(Role::display_name).limit(4).collect(java.util.stream.Collectors.toList());
    }

    private List<String> rolesThatAllowUseBlock(BlockState state) {
        return roles.values().stream()
                .filter(r -> {
                    var p = perms.get(r.id());
                    if (p == null || !p.controls().use_block()) return false;
                    boolean denied = p.denyUseBlocks.contains(state.getBlock()) || inAny(state, p.denyUseBlockTags);
                    boolean ok = p.allowUseBlocks.contains(state.getBlock()) || inAny(state, p.allowUseBlockTags);
                    return !denied && ok;
                })
                .map(Role::display_name)
                .limit(4)
                .collect(Collectors.toList());
    }

    public enum Result { ALLOW, REQUIRE, DISABLED, PENDING }  // <= novo PENDING

    private static boolean inAny(Block block, List<TagKey<Block>> tags) {
        BlockState state = block.getDefaultState();
        for (var t : tags) if (state.isIn(t)) return true;
        return false;
    }

    public java.util.Collection<Role> listRoles() {
        return java.util.Collections.unmodifiableCollection(roles.values());
    }
    public String getCurrentRoleId() { return currentRoleId; }
    public boolean isUnassigned() {
        return RoleRegistry.DEFAULT_ROLE.equals(currentRoleId);
    }
}
