package net.meh.cosmolib.item;

import net.meh.cosmolib.entity.CosmeticMannequinEntity;
import net.meh.cosmolib.registry.CosmoLibEntityTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Placement item for the {@link CosmeticMannequinEntity}.
 *
 * <p>Right-clicking while looking at the top face of a block places the mannequin
 * with its rotation snapped to the player's facing direction in 45° increments.
 *
 * <p>The entity is spawned at the centre of the block above the clicked surface,
 * facing away from the player (matching the convention used by ArmorStands and
 * furniture placement in this mod).
 */
public class CosmeticMannequinItem extends Item {

    public CosmeticMannequinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Ray-cast to find a block surface
        HitResult hit = player.pick(5.0, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        // Only allow placement on the top face of a block
        if (blockHit.getDirection() != net.minecraft.core.Direction.UP) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        net.minecraft.core.BlockPos pos = blockHit.getBlockPos();

        // Compute placement position: centre of the block above the clicked surface
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;

        // Snap player's yaw to the nearest 45° step, then add 4 steps (180°) so the mannequin
        // faces *toward* the player (opposite of the player's look direction).
        // GeckoLib applies (180 − entityYaw) as the render rotation; with entityYaw = playerYaw + 180,
        // that becomes −playerYaw which correctly orients the eye-face toward the player.
        float rawYaw = player.getYRot();
        int rotIndex = Math.floorMod(Math.round(rawYaw / 45.0f) + 4, 8);

        CosmeticMannequinEntity mannequin =
                new CosmeticMannequinEntity(CosmoLibEntityTypes.COSMETIC_MANNEQUIN.get(), level);
        mannequin.setPos(x, y, z);
        mannequin.setRotationIndex(rotIndex);

        level.addFreshEntity(mannequin);

        // Play the same placement sound as an armor stand
        level.playSound((Player) null, x, y, z,
                SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75f, 0.8f);

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResultHolder.success(stack);
    }
}
