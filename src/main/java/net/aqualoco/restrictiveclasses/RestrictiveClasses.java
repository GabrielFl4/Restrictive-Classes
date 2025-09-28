package net.aqualoco.restrictiveclasses;

import net.aqualoco.restrictiveclasses.command.RoleCommand;
import net.aqualoco.restrictiveclasses.core.RC;
import net.aqualoco.restrictiveclasses.events.Gates;
import net.aqualoco.restrictiveclasses.network.RCNetwork;
import net.aqualoco.restrictiveclasses.network.payload.RoleSyncS2CPayload;
import net.aqualoco.restrictiveclasses.network.payload.RulesSyncS2CPayload;
import net.aqualoco.restrictiveclasses.role.RoleLoader;
import net.aqualoco.restrictiveclasses.role.RoleRegistry;
import net.aqualoco.restrictiveclasses.state.RoleHolder;
import net.aqualoco.restrictiveclasses.state.RoleStorage;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestrictiveClasses implements ModInitializer {
	public static final String MOD_ID = "restrictive-classes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 1) Carregador de roles
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new RoleLoader());
		PayloadTypeRegistry.playS2C().register(RulesSyncS2CPayload.ID, RulesSyncS2CPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RoleSyncS2CPayload.ID, RoleSyncS2CPayload.CODEC);

		// 2) Eventos ("gates")
		Gates.register();

		// 3) Comandos
		RoleCommand.register();

		// 4) Ao entrar, aplicar a role persistida
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			var player = handler.player;
			var storage = RoleStorage.get(server);
			String roleId = storage.getRole(player.getUuid()).orElse(RoleRegistry.DEFAULT_ROLE);
			((RoleHolder) player).rc$setRoleId(roleId);

			RCNetwork.sendRulesTo(player);
			RCNetwork.sendRoleTo(player);
			RC.LOG.info("[RC] {} entrou com role '{}'", player.getGameProfile().getName(), roleId);
		});

		// Após /reload (data packs): reenvia snapshot para todos
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
			RC.LOG.info("[RC] datapack reload → reenviando snapshot de regras");
			RCNetwork.broadcastRules(server);
		});

		// Aplico depois do jogador respawnar uma sincronia da Role do jogador
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			RCNetwork.sendRoleTo(newPlayer);
		});
	}
}
