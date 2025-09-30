package net.aqualoco.restrictiveclasses;

import net.aqualoco.restrictiveclasses.core.RC;
import net.aqualoco.restrictiveclasses.events.Gates;
import net.aqualoco.restrictiveclasses.network.RCNetwork;
import net.aqualoco.restrictiveclasses.network.payload.RoleSyncS2CPayload;
import net.aqualoco.restrictiveclasses.network.payload.RulesSyncS2CPayload;
import net.aqualoco.restrictiveclasses.network.payload.SelectRoleC2SPayload;
import net.aqualoco.restrictiveclasses.role.RoleRegistry;
import net.aqualoco.restrictiveclasses.state.RoleHolder;
import net.aqualoco.restrictiveclasses.state.RoleStorage;
import net.aqualoco.restrictiveclasses.command.RoleCommand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class RestrictiveClasses implements ModInitializer {
	@Override
	public void onInitialize() {
		// Datapack roles
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new net.aqualoco.restrictiveclasses.role.RoleLoader());

		// === NETWORK TYPES ===
		// (registre aqui; NÃO registre no client initializer para evitar duplicado)
		PayloadTypeRegistry.playS2C().register(RulesSyncS2CPayload.ID, RulesSyncS2CPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RoleSyncS2CPayload.ID,  RoleSyncS2CPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SelectRoleC2SPayload.ID, SelectRoleC2SPayload.CODEC);

		// Gates/Comandos
		Gates.register();
		RoleCommand.register();

		// JOIN: envia snapshot e a role
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;

			var storage = RoleStorage.get(server);
			String roleId = storage.getRole(player.getUuid()).orElse(RoleRegistry.DEFAULT_ROLE);
			((RoleHolder) player).rc$setRoleId(roleId);

			RC.LOG.info("[RC] Enviando rules_sync para {}…", player.getGameProfile().getName());
			RCNetwork.sendRulesTo(player);            // 1º regras
			RCNetwork.sendRoleTo(player);             // 2º role

			RC.LOG.info("[RC] {} entrou com role '{}'", player.getGameProfile().getName(), roleId);
		});

		// /reload → reenvia snapshot
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
			RC.LOG.info("[RC] datapack reload → reenviando snapshot de regras");
			RCNetwork.broadcastRules(server);
		});

		// Respawn → reenviar role (já copiamos no mixin copyFrom)
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
			RCNetwork.sendRoleTo(newP);
		});

		// === C2S: seleção de role ===
		ServerPlayNetworking.registerGlobalReceiver(SelectRoleC2SPayload.ID, (payload, ctx) -> {
			ServerPlayerEntity player = ctx.player();
			player.getServer().execute(() -> {
				String roleId = payload.roleId();
				if (RoleRegistry.get(roleId) == null) {
					player.sendMessage(Text.literal("§cRole inválida: " + roleId), false);
					return;
				}
				((RoleHolder) player).rc$setRoleId(roleId);
				RoleStorage.get(player.getServer()).setRole(player.getUuid(), roleId);
				RCNetwork.sendRoleTo(player);
			});
		});
	}
}
