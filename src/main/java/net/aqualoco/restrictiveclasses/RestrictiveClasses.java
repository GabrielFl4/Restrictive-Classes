package net.aqualoco.restrictiveclasses;

import net.aqualoco.restrictiveclasses.command.RoleCommand;
import net.aqualoco.restrictiveclasses.core.RC;
import net.aqualoco.restrictiveclasses.events.Gates;
import net.aqualoco.restrictiveclasses.role.RoleLoader;
import net.aqualoco.restrictiveclasses.state.RoleHolder;
import net.aqualoco.restrictiveclasses.state.RoleStorage;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestrictiveClasses implements ModInitializer {
	public static final String MOD_ID = "restrictive-classes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 1) Carregador de roles
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new RoleLoader());

		// 2) Eventos ("gates")
		Gates.register();

		// 3) Comando /rc role ...
		RoleCommand.register();

		// 4) Ao entrar, aplicar a role persistida
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			var player = handler.player;
			var storage = RoleStorage.get(server);
			String roleId = String.valueOf(storage.getRole(player.getUuid()));
			((RoleHolder) player).rc$setRoleId(roleId);
			RC.LOG.info("[RC] {} entrou com role '{}'", player.getGameProfile().getName(), roleId);
		});

		RC.LOG.info("[RC] Restrictive Classes inicializado.");
	}
}
