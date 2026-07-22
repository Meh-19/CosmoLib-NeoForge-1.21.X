package net.meh.cosmolib.toolskin;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Common-side {@link TooltipComponent} injected into a skinned tool's tooltip by
 * {@link net.meh.cosmolib.mixin.ItemStackTooltipMixin}.
 *
 * <p>Carries the full {@link ToolSkinData} so the client-side renderer
 * ({@link net.meh.cosmolib.toolskin.client.ClientOriginalItemTooltipComponent})
 * can draw the original item icon, set name, and optional exclusive tag as a
 * single compact line — without any font glyph dependency.
 */
public record OriginalItemTooltipComponent(ToolSkinData data) implements TooltipComponent {}
