package net.aqualoco.restrictiveclasses.client.ui;

import net.aqualoco.restrictiveclasses.client.ClientRules;
import net.aqualoco.restrictiveclasses.network.payload.SelectRoleC2SPayload;
import net.aqualoco.restrictiveclasses.role.Role;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class RoleSelectScreen extends Screen {
    private final List<Role> roles = new ArrayList<>();
    private int selected = -1;

    public RoleSelectScreen() {
        super(Text.literal("Escolha sua Classe"));
    }

    @Override
    protected void init() {
        roles.clear();
        // lista todas menos "unassigned"
        for (var r : ClientRules.get().listRoles()) {
            if (!net.aqualoco.restrictiveclasses.role.RoleRegistry.DEFAULT_ROLE.equals(r.id())) {
                roles.add(r);
            }
        }
        int y = this.height / 2 - Math.min(roles.size(), 6) * 12;
        int x = this.width / 2 - 120;

        // botões de role (simples, coluna única)
        for (int i = 0; i < roles.size(); i++) {
            final int idx = i;
            var r = roles.get(i);
            ButtonWidget btn = ButtonWidget.builder(Text.literal(r.display_name()), b -> {
                selected = idx;
            }).dimensions(x, y + i * 24, 240, 20).build();
            this.addDrawableChild(btn);
        }

        // botão confirmar
        ButtonWidget choose = ButtonWidget.builder(Text.literal("Escolher"), b -> {
            if (selected >= 0 && selected < roles.size()) {
                String id = roles.get(selected).id();
                ClientPlayNetworking.send(new SelectRoleC2SPayload(id));
                close();
            }
        }).dimensions(this.width / 2 - 60, y + roles.size() * 24 + 10, 120, 20).build();
        choose.active = true;
        this.addDrawableChild(choose);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        if (selected >= 0 && selected < roles.size()) {
            var r = roles.get(selected);
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Selecionada: " + r.display_name()), this.width / 2, 40, 0xFFFFAA);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
