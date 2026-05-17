package net.meh.cosmolib.item;

import net.meh.cosmolib.furniture.block.AbstractFurnitureBlock;
import net.meh.cosmolib.furniture.blockentity.FurnitureBlockEntity;
import net.meh.cosmolib.paint.PaintData;
import net.meh.cosmolib.tag.CosmoLibTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

public class PaintbrushItem extends Item {

    public PaintbrushItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level   = ctx.getLevel();
        BlockPos pos  = ctx.getClickedPos();
        ItemStack stack = ctx.getItemInHand();

        int color = getPaintColor(stack);
        if (color == -1) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);
        boolean canPaint = (state.getBlock() instanceof AbstractFurnitureBlock afb && afb.isPaintable())
                || state.is(CosmoLibTags.Blocks.PAINTABLE);
        if (!canPaint) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FurnitureBlockEntity furniture)) return InteractionResult.PASS;

        if (!level.isClientSide) {
            furniture.setPaintColor(color);
            spawnParticles(level, pos, color);

            if (ctx.getPlayer() instanceof ServerPlayer sp && !sp.getAbilities().instabuild) {
                stack.hurtAndBreak(1, (ServerLevel) level, sp,
                        item -> sp.onEquippedItemBroken(item, EquipmentSlot.MAINHAND));
            }
        }

        level.playSound(null, pos,
                net.minecraft.sounds.SoundEvents.DYE_USE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void spawnParticles(Level level, BlockPos pos, int color) {
        if (!(level instanceof ServerLevel server)) return;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        server.sendParticles(
                new DustParticleOptions(new Vector3f(r, g, b), 1.0f),
                pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                10, 0.3, 0.2, 0.3, 0.05);
    }

    public static int getPaintColor(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return -1;
        CompoundTag tag = data.copyTag();
        return tag.contains(PaintData.KEY) ? tag.getInt(PaintData.KEY) : -1;
    }
}
