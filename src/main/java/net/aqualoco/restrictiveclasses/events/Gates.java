package net.aqualoco.restrictiveclasses.events;

import net.aqualoco.restrictiveclasses.core.RC;
import net.aqualoco.restrictiveclasses.role.Role;
import net.aqualoco.restrictiveclasses.role.RolePermissions;
import net.aqualoco.restrictiveclasses.role.RoleRegistry;
import net.aqualoco.restrictiveclasses.state.RoleHolder;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class Gates {

    public static void register() {
        // Quebrar bloco
        PlayerBlockBreakEvents.BEFORE.register(Gates::onBeforeBreak);

        // Usar item
        UseItemCallback.EVENT.register(Gates::onUseItem);

        // Usar/abrir bloco
        UseBlockCallback.EVENT.register(Gates::onUseBlock);
    }

    // === BREAK ===
    private static boolean onBeforeBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity be) {
        if (world.isClient) return true; // só server decide

        var roleId = ((RoleHolder) player).rc$getRoleId();
        var perms = RoleRegistry.perms(roleId);
        if (perms == null) perms = RoleRegistry.perms(RoleRegistry.DEFAULT_ROLE);

        if (!perms.controls().break_block()) return true; // esta role não governa "break"

        boolean allowed = isBlockAllowedForBreak(state, perms);
        if (!allowed) {
            deny(player, "Você não pode quebrar isso com a sua classe (" + roleId + ")");
            return false; // cancela a quebra
        }
        return true;
    }

    private static boolean isBlockAllowedForBreak(BlockState state, RolePermissions p) {
        // deny tem prioridade
        for (var t : p.denyBlockTags) if (state.isIn(t)) return false;
        if (p.denyBlocks.contains(state.getBlock())) return false;

        boolean ok = p.allowBlocks.contains(state.getBlock());
        if (!ok) for (var t : p.allowBlockTags) if (state.isIn(t)) { ok = true; break; }
        return ok;
    }

    // === USE ITEM ===
    private static ActionResult onUseItem(PlayerEntity player, World world, Hand hand) {
        if (world.isClient) return ActionResult.PASS;

        var perms = RoleRegistry.perms(((RoleHolder) player).rc$getRoleId());
        if (perms == null) perms = RoleRegistry.perms(RoleRegistry.DEFAULT_ROLE);

        if (!perms.controls().use_item()) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);

        // deny > allow
        if (perms.denyItems.contains(stack.getItem())) {
            deny(player, "Sua classe não permite usar este item");
            return ActionResult.FAIL;
        }
        for (var t : perms.denyItemTags) {
            if (stack.isIn(t)) { deny(player, "Sua classe não permite usar este item"); return ActionResult.FAIL; }
        }

        boolean ok = perms.allowItems.contains(stack.getItem());
        if (!ok) {
            for (var t : perms.allowItemTags) if (stack.isIn(t)) { ok = true; break; }
        }

        if (!ok) {
            deny(player, "Sua classe não permite usar este item");
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // === USE BLOCK ===
    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.PASS;

        var perms = RoleRegistry.perms(((RoleHolder) player).rc$getRoleId());
        if (perms == null) perms = RoleRegistry.perms(RoleRegistry.DEFAULT_ROLE);

        if (!perms.controls().use_block()) return ActionResult.PASS;

        var state = world.getBlockState(hit.getBlockPos());

        // deny > allow
        if (perms.denyUseBlocks.contains(state.getBlock())) {
            deny(player, "Sua classe não pode interagir com este bloco");
            return ActionResult.FAIL;
        }
        for (var t : perms.denyUseBlockTags) {
            if (state.isIn(t)) { deny(player, "Sua classe não pode interagir com este bloco"); return ActionResult.FAIL; }
        }

        boolean ok = perms.allowUseBlocks.contains(state.getBlock());
        if (!ok) for (var t : perms.allowUseBlockTags) if (state.isIn(t)) { ok = true; break; }

        if (!ok) {
            deny(player, "Sua classe não pode interagir com este bloco");
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private static void deny(PlayerEntity player, String msg) {
        player.sendMessage(Text.literal("§c" + msg),true); // simples e visível; depois trocamos por action bar/sons
    }
}
