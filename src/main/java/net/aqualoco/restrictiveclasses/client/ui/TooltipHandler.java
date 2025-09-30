package net.aqualoco.restrictiveclasses.client.ui;

import net.aqualoco.restrictiveclasses.client.ClientRules;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class TooltipHandler {
    private TooltipHandler() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register(TooltipHandler::onTooltip);
    }

    private static void onTooltip(ItemStack stack, Item.TooltipContext ctx, TooltipType type, List<Text> lines) {
        // Se ainda não sincronizou, não polui tooltip
        if (!ClientRules.get().isReady()) return;

        var res = ClientRules.get().canUseItem(stack);
        if (res == ClientRules.Result.ALLOW) return;

        if (res == ClientRules.Result.DISABLED) {
            lines.add(Text.literal("Desabilitado nesse mundo").formatted(Formatting.RED));
            return;
        }
        if (res == ClientRules.Result.REQUIRE) {
            var names = getNames(stack);
            if (names.isEmpty()) {
                lines.add(Text.literal("Desabilitado nesse mundo").formatted(Formatting.RED));
            } else {
                //lines.add(Text.empty()); // Linha vazia
                lines.add(Text.literal("Requer: " + String.join(" / ", names)).formatted(Formatting.RED));
            }
        }
        // PENDING não mostra nada no tooltip (overlay já cobre)
    }

    private static java.util.List<String> getNames(ItemStack stack) {
        try {
            // reusa a lista do ClientRules (já limita a 4)
            var method = ClientRules.class.getDeclaredMethod("rolesThatAllowUseItem", ItemStack.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<String> names = (java.util.List<String>) method.invoke(ClientRules.get(), stack);
            return names;
        } catch (Throwable ignored) {
            return java.util.List.of();
        }
    }
}
