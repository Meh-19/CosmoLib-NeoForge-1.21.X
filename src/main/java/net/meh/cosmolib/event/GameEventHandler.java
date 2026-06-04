package net.meh.cosmolib.event;

import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.CosmeticManager;
import net.meh.cosmolib.crate.entity.CrateEntity;
import net.meh.cosmolib.furniture.tool.BoundingBoxSelectorSession;
import net.meh.cosmolib.toolskin.ToolSkinData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;

/** Listens on the NeoForge game bus for in-game events. */
@EventBusSubscriber(modid = CosmoLib.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class GameEventHandler {

    private GameEventHandler() {}

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CosmeticManager.onPlayerJoin(sp);
        }
    }

    /**
     * Cancels all incoming attacks against a player who is currently in a crate session.
     *
     * <p>Cancelling {@link LivingIncomingDamageEvent} prevents the damage container from
     * being processed — no HP loss, no armour durability drain. This is a much cleaner
     * guarantee than resistance effects, which only reduce the damage value after the hit
     * has already been registered.
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && CrateEntity.isPlayerInCrateSession(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    /**
     * Returns the token to the player's inventory when they remove a skin at the smithing table.
     *
     * <p>Detection logic: the input container has 3 slots (smithing layout), slot 0
     * (template) is empty, slot 1 (base) carries a {@link ToolSkinData} component,
     * and the crafted result is the clean tool.
     * This uniquely identifies a tool-skin removal operation.
     *
     * <p>The token is added to the player's inventory, or dropped at their feet if
     * the inventory is full. The clean tool appears in the smithing output slot.
     */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Container container = event.getInventory();
        // Smithing table input has exactly 3 slots: template, base, addition
        if (container.getContainerSize() != 3) return;

        ItemStack base = container.getItem(1);
        if (base.isEmpty()) return;

        Optional<ToolSkinData> skinDataOpt = ToolSkinData.get(base);
        if (skinDataOpt.isEmpty()) return;

        // Template must be empty — this is the removal path
        if (!container.getItem(0).isEmpty()) return;

        // Result must be the clean tool (same item type, no skin data)
        ItemStack crafted = event.getCrafting();
        if (crafted.getItem() != base.getItem()) return;
        if (ToolSkinData.get(crafted).isPresent()) return;

        // Return the token to inventory
        ItemStack token = skinDataOpt.get().createToken();
        if (token.isEmpty()) return;

        Player player = event.getEntity();
        if (!player.getInventory().add(token)) {
            player.drop(token, false);
        }
    }

    /**
     * Clears any active {@link BoundingBoxSelectorSession} when a player logs out,
     * so their in-progress work does not linger and block other server operations.
     * The temp crash-recovery file is intentionally left on disk for inspection.
     */
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            BoundingBoxSelectorSession.clear(sp.getStringUUID());
        }
    }
}
