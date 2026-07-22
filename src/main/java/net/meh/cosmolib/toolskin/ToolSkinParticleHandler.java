package net.meh.cosmolib.toolskin;

import net.meh.cosmolib.CosmoLib;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Handles particle effects for tool skins:
 * - Sword/Axe: spawns particles when hitting entities
 * - Bow: trails particles behind arrows shot from skinned bows
 * - Pickaxe/Shovel/Axe: spawns particles when breaking blocks
 */
@EventBusSubscriber(modid = CosmoLib.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ToolSkinParticleHandler {

    /**
     * Handles sword/axe hit particles.
     * When a player hits an entity with a skinned tool, spawn particles at the hit location.
     */
    @SubscribeEvent
    public static void onEntityHit(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (!(attacker.level() instanceof ServerLevel serverLevel)) return;

        ItemStack weapon = attacker.getMainHandItem();
        ToolSkinData.get(weapon).ifPresent(skinData -> {
            if (skinData.particle() == null) return;

            // Only spawn for melee weapons (sword/axe)
            ToolSkinType type = skinData.skinType();
            if (type != ToolSkinType.SWORD && type != ToolSkinType.AXE) return;

            // Spawn particles at the target's position
            Vec3 pos = event.getEntity().position().add(0, event.getEntity().getBbHeight() / 2, 0);
            for (int i = 0; i < 5; i++) {
                double offsetX = (serverLevel.random.nextDouble() - 0.5) * 0.5;
                double offsetY = (serverLevel.random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 0.5;
                serverLevel.sendParticles(skinData.particle(),
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        1, 0, 0, 0, 0);
            }
        });
    }

    /**
     * Handles pickaxe/shovel/axe block break particles.
     * When a player breaks a block with a skinned tool, spawn particles at the block location.
     */
    @SubscribeEvent
    public static void onBlockBreak(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) return;

        ItemStack tool = event.getEntity().getMainHandItem();
        ToolSkinData.get(tool).ifPresent(skinData -> {
            if (skinData.particle() == null) return;

            // Only spawn for harvesting tools
            ToolSkinType type = skinData.skinType();
            if (type != ToolSkinType.PICKAXE && type != ToolSkinType.SHOVEL && type != ToolSkinType.AXE) return;

            // Spawn particles at block center
            Vec3 pos = event.getPosition().get().getCenter();
            for (int i = 0; i < 3; i++) {
                double offsetX = (serverLevel.random.nextDouble() - 0.5) * 0.3;
                double offsetY = (serverLevel.random.nextDouble() - 0.5) * 0.3;
                double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 0.3;
                serverLevel.sendParticles(skinData.particle(),
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        1, 0, 0, 0, 0);
            }
        });
    }

    /**
     * Handles bow/crossbow arrow trail particles.
     * Checks arrows in the world - if they were shot from a skinned bow/crossbow, trail particles.
     */
    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.level() instanceof ServerLevel serverLevel)) return;
        if (arrow.tickCount % 2 != 0) return; // Only every other tick

        // Check if the arrow's owner has a skinned bow/crossbow
        if (!(arrow.getOwner() instanceof LivingEntity owner)) return;

        ItemStack weapon = owner.getMainHandItem();
        ToolSkinData.get(weapon).ifPresent(skinData -> {
            if (skinData.particle() == null) return;
            if (skinData.skinType() != ToolSkinType.BOW && skinData.skinType() != ToolSkinType.CROSSBOW) return;

            // Spawn particle at arrow position
            Vec3 pos = arrow.position();
            serverLevel.sendParticles(skinData.particle(),
                    pos.x, pos.y, pos.z,
                    1, 0, 0, 0, 0);
        });
    }
}
