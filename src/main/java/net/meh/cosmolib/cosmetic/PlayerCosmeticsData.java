package net.meh.cosmolib.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Per-player cosmetic loadout. Serialized to NBT via {@link #CODEC} and
 * persisted to disk (survives restarts and death) through the attachment system.
 */
public class PlayerCosmeticsData {

    private record SlotEntry(String slot, ItemStack stack) {
        static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING   .fieldOf("slot") .forGetter(SlotEntry::slot),
                ItemStack.CODEC.fieldOf("stack").forGetter(SlotEntry::stack)
        ).apply(inst, SlotEntry::new));
    }

    public static final Codec<PlayerCosmeticsData> CODEC = SlotEntry.CODEC.listOf().xmap(
            list -> {
                PlayerCosmeticsData data = new PlayerCosmeticsData();
                for (SlotEntry e : list) {
                    try {
                        CosmeticSlot slot = CosmeticSlot.valueOf(e.slot().toUpperCase(Locale.ROOT));
                        if (!e.stack().isEmpty()) data.equipped.put(slot, e.stack().copy());
                    } catch (IllegalArgumentException ignored) {}
                }
                return data;
            },
            data -> {
                List<SlotEntry> list = new ArrayList<>();
                data.equipped.forEach((slot, stack) -> {
                    if (!stack.isEmpty()) list.add(new SlotEntry(slot.getId(), stack.copy()));
                });
                return list;
            }
    );

    private final Map<CosmeticSlot, ItemStack> equipped = new EnumMap<>(CosmeticSlot.class);

    public PlayerCosmeticsData() {}

    public void equip(CosmeticSlot slot, ItemStack stack) {
        if (stack.isEmpty()) equipped.remove(slot);
        else equipped.put(slot, stack.copy());
    }

    public void unequip(CosmeticSlot slot) {
        equipped.remove(slot);
    }

    public ItemStack getEquipped(CosmeticSlot slot) {
        return equipped.getOrDefault(slot, ItemStack.EMPTY);
    }

    public boolean hasEquipped(CosmeticSlot slot) {
        ItemStack s = equipped.get(slot);
        return s != null && !s.isEmpty();
    }

    public Map<CosmeticSlot, ItemStack> getAllEquipped() {
        return Collections.unmodifiableMap(equipped);
    }
}
