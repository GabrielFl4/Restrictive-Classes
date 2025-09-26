package net.aqualoco.restrictiveclasses.role;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

import java.util.List;
import java.util.Set;

// Estruturas prontas para checagem rápida em runtime.
public final class RolePermissions {
    // Quebrar
    public final List<TagKey<Block>> allowBlockTags;
    public final Set<Block> allowBlocks;
    public final List<TagKey<Item>> allowItemTags; // para usar item
    public final Set<Item> allowItems;

    // Interagir com bloco (abrir/usar)
    public final List<TagKey<Block>> allowUseBlockTags;
    public final Set<Block> allowUseBlocks;

    // (opcional) deny explícito — aplicamos depois do allow (prioridade ao deny)
    public final List<TagKey<Block>> denyBlockTags;
    public final Set<Block> denyBlocks;
    public final List<TagKey<Item>> denyItemTags;
    public final Set<Item> denyItems;
    public final List<TagKey<Block>> denyUseBlockTags;
    public final Set<Block> denyUseBlocks;

    public final Role.Controls controls;

    public RolePermissions(
            List<TagKey<Block>> allowBlockTags, Set<Block> allowBlocks,
            List<TagKey<Item>> allowItemTags, Set<Item> allowItems,
            List<TagKey<Block>> allowUseBlockTags, Set<Block> allowUseBlocks,
            List<TagKey<Block>> denyBlockTags, Set<Block> denyBlocks,
            List<TagKey<Item>> denyItemTags, Set<Item> denyItems,
            List<TagKey<Block>> denyUseBlockTags, Set<Block> denyUseBlocks,
            Role.Controls controls) {
        this.allowBlockTags = allowBlockTags;
        this.allowBlocks = allowBlocks;
        this.allowItemTags = allowItemTags;
        this.allowItems = allowItems;
        this.allowUseBlockTags = allowUseBlockTags;
        this.allowUseBlocks = allowUseBlocks;
        this.denyBlockTags = denyBlockTags;
        this.denyBlocks = denyBlocks;
        this.denyItemTags = denyItemTags;
        this.denyItems = denyItems;
        this.denyUseBlockTags = denyUseBlockTags;
        this.denyUseBlocks = denyUseBlocks;
        this.controls = controls;
    }

    public Role.Controls controls() { return controls; }

    public static TagKey<Block> blockTag(String id) {
        return TagKey.of(RegistryKeys.BLOCK, net.minecraft.util.Identifier.of(id));
    }
    public static TagKey<Item> itemTag(String id) {
        return TagKey.of(RegistryKeys.ITEM, net.minecraft.util.Identifier.of(id));
    }

    public static <T> Set<T> newSet() { return new ObjectOpenHashSet<>(); }
}
