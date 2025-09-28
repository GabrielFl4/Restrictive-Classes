package net.aqualoco.restrictiveclasses.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.aqualoco.restrictiveclasses.network.RCNetwork;
import net.aqualoco.restrictiveclasses.role.RoleRegistry;
import net.aqualoco.restrictiveclasses.state.RoleHolder;
import net.aqualoco.restrictiveclasses.state.RoleStorage;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;


public final class RoleCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(RoleCommand::register0);
    }

    private static void register0(CommandDispatcher<ServerCommandSource> disp, CommandRegistryAccess acc, CommandManager.RegistrationEnvironment env) {
        disp.register(CommandManager.literal("rc")
                .then(CommandManager.literal("role")
                        .then(CommandManager.literal("list")
                                .executes(ctx -> {
                                    var ids = String.join(", ", RoleRegistry.ids());
                                    ctx.getSource().sendFeedback(() -> Text.literal("Roles: " + ids), false);
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("get")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            var p = EntityArgumentType.getPlayer(ctx, "player");
                                            String id = ((RoleHolder)p).rc$getRoleId();
                                            ctx.getSource().sendFeedback(() -> Text.literal(p.getName().getString() + " → " + id), false);
                                            return 1;
                                        })
                                )
                        )
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("roleId", StringArgumentType.string())
                                                .suggests((c, b) -> {
                                                    for (var id : RoleRegistry.ids()) b.suggest(id);
                                                    return b.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    // dentro do .executes() do "set":
                                                    var p = EntityArgumentType.getPlayer(ctx, "player");
                                                    var roleId = StringArgumentType.getString(ctx, "roleId");
                                                    if (RoleRegistry.get(roleId) == null) {
                                                        ctx.getSource().sendError(Text.literal("Role não encontrada: " + roleId));
                                                        return 0;
                                                    }
                                                    ((RoleHolder)p).rc$setRoleId(roleId);
                                                    RoleStorage.get(ctx.getSource().getServer()).setRole(p.getUuid(), roleId);
                                                    RCNetwork.sendRoleTo(p);
                                                    return 1;
                                                })
                                        )))
                )
        );
    }
}
