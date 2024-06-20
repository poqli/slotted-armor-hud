package ru.berdinskiybear.armorhud.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.berdinskiybear.armorhud.ArmorHudMod;
import ru.berdinskiybear.armorhud.config.ArmorHudConfig;
import java.util.ArrayList;
import java.util.List;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin extends DrawableHelper {
    @Shadow @Final
    private MinecraftClient client;
    @Shadow
    private int scaledWidth;
    @Shadow
    private int scaledHeight;
    @Unique
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");
    @Unique
    private static final Identifier EMPTY_HELMET_SLOT_TEXTURE = new Identifier("item/empty_armor_slot_helmet");
    @Unique
    private static final Identifier EMPTY_CHESTPLATE_SLOT_TEXTURE = new Identifier("item/empty_armor_slot_chestplate");
    @Unique
    private static final Identifier EMPTY_LEGGINGS_SLOT_TEXTURE = new Identifier("item/empty_armor_slot_leggings");
    @Unique
    private static final Identifier EMPTY_BOOTS_SLOT_TEXTURE = new Identifier("item/empty_armor_slot_boots");
    @Unique
    private static final Identifier BLOCK_ATLAS_TEXTURE = new Identifier("textures/atlas/blocks.png");

    @Unique
    private static final int slot_length = 20;
    @Unique
    private static final int slot_borderedLength = 22;
    @Unique
    private static final int hotbar_offset = 98;
    @Unique
    private static final int offhandSlot_offset = 29;
    @Unique
    private static final int attackIndicator_offset = 23;
    @Unique
    private static final int[] slotU = {1, 21, 41, 61, 81, 101, 121, 141, 161};

    @Unique
    private final List<ItemStack> armorHudItems = new ArrayList<>(4);

    @Shadow
    protected abstract void renderHotbarItem(MatrixStack matrixStack, int x, int y, float tickDelta, PlayerEntity player, ItemStack stack, int seed);

    @Shadow
    protected abstract PlayerEntity getCameraPlayer();

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbar(FLnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.AFTER))
    public void armorHud_renderArmorHud(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        // add this to profiler
        this.client.getProfiler().push("armorHud");
        ArmorHudConfig config = this.getArmorHudConfig();

        // switch to enable the mod
        if (config.isEnabled()) {
            PlayerEntity playerEntity = this.getCameraPlayer();
            if (playerEntity != null) {

                // count the items and save the ones that need to be drawn
                armorHudItems.addAll(playerEntity.getInventory().armor);
                boolean hasArmor = false;
                for (int i = 0; i < armorHudItems.size(); i++) {
                    ItemStack armorSlot = armorHudItems.get(i);
                    if (!armorSlot.isEmpty())
                        hasArmor = true;
                    else if (config.getSlotsShown() == ArmorHudConfig.SlotsShown.Show_Equipped)
                        armorHudItems.remove(i--);
                }
                if (!hasArmor && config.getSlotsShown() == ArmorHudConfig.SlotsShown.Show_All)
                    armorHudItems.clear();

                // if true, then prepare and draw
                if (!armorHudItems.isEmpty() || config.getSlotsShown() == ArmorHudConfig.SlotsShown.Always_Show) {
                    final int armorHudLength = slot_borderedLength + ((armorHudItems.size() - 1) * slot_length);
                    final int verticalMultiplier;
                    final int sideMultiplier;
                    final int sideOffsetMultiplier;
                    final int addedHotbarOffset;
                    final int y;
                    final int x;

                    // calculate the position of the armor HUD based on the config
                    switch (config.getAnchor()) {
                        case Hotbar, Bottom ->
                                verticalMultiplier = -1;
                        case Top, Top_Center ->
                                verticalMultiplier = 1;
                        default -> throw new IllegalStateException("Unexpected value: " + config.getAnchor());
                    }
                    if ((config.getAnchor() == ArmorHudConfig.Anchor.Hotbar && config.getSide() == ArmorHudConfig.Side.Left) || (config.getAnchor() != ArmorHudConfig.Anchor.Hotbar && config.getSide() == ArmorHudConfig.Side.Right)) {
                        sideMultiplier = -1;
                        sideOffsetMultiplier = -1;
                    } else {
                        sideMultiplier = 1;
                        sideOffsetMultiplier = 0;
                    }
                    switch (config.getOffhandSlotBehavior()) {
                        case Leave_Space -> addedHotbarOffset = Math.max(offhandSlot_offset, attackIndicator_offset);
                        case Adhere -> {
                            if ((playerEntity.getMainArm() == Arm.RIGHT && config.getSide() == ArmorHudConfig.Side.Left || playerEntity.getMainArm() == Arm.LEFT && config.getSide() == ArmorHudConfig.Side.Right) && !playerEntity.getOffHandStack().isEmpty())
                                addedHotbarOffset = offhandSlot_offset;
                            else if ((playerEntity.getMainArm() == Arm.RIGHT && config.getSide() == ArmorHudConfig.Side.Right || playerEntity.getMainArm() == Arm.LEFT && config.getSide() == ArmorHudConfig.Side.Left) && this.client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR)
                                addedHotbarOffset = attackIndicator_offset;
                            else
                                addedHotbarOffset = 0;
                        }
                        case Ignore -> addedHotbarOffset = 0;
                        default -> throw new IllegalStateException("Unexpected value: " + config.getOffhandSlotBehavior());
                    }
                    int x_temp;
                    int y_temp;
                    switch (config.getOrientation()) {
                        case Horizontal -> {
                            x_temp = switch (config.getAnchor()) {
                                case Top_Center -> scaledWidth / 2 - (armorHudLength / 2);
                                case Top, Bottom -> (armorHudLength - scaledWidth) * sideOffsetMultiplier;
                                case Hotbar -> scaledWidth / 2 + ((hotbar_offset + addedHotbarOffset) * sideMultiplier) + (armorHudLength * sideOffsetMultiplier);
                            };
                            y_temp = switch (config.getAnchor()) {
                                case Bottom, Hotbar -> scaledHeight - slot_borderedLength;
                                case Top, Top_Center -> 0;
                            };
                        }
                        case Vertical -> {
                            x_temp = switch (config.getAnchor()) {
                                case Top_Center -> scaledWidth / 2 - (slot_borderedLength / 2);
                                case Top, Bottom -> (slot_borderedLength - scaledWidth) * sideOffsetMultiplier;
                                case Hotbar -> scaledWidth / 2 + ((hotbar_offset + addedHotbarOffset) * sideMultiplier) + (slot_borderedLength * sideOffsetMultiplier);
                            };
                            y_temp = switch (config.getAnchor()) {
                                case Bottom, Hotbar -> scaledHeight - armorHudLength;
                                case Top, Top_Center -> 0;
                            };
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + config.getOrientation());
                    }
                    if (config.getAnchor() != ArmorHudConfig.Anchor.Top_Center)
                        x_temp += config.getOffsetX() * sideMultiplier;
                    y_temp += config.getOffsetY() * verticalMultiplier;
                    x = x_temp;
                    y = y_temp;

                    // prepare the texture
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();

                    // draw the slots
                    int[] slotTextures = new int[armorHudItems.size()];
                    for (int i = 0; i < armorHudItems.size(); i++)
                        slotTextures[i] = config.getSlotTextures()[i] - 1;
                    matrices.push();
                    matrices.translate(0, 0, -92);
                    this.drawSlots(config, matrices, x, y, slotTextures);
                    matrices.pop();

                    // blend in the empty slot icons
                    if (config.isEmptyIconsShown() && config.getSlotsShown() != ArmorHudConfig.SlotsShown.Show_Equipped && (!armorHudItems.isEmpty() || config.getSlotsShown() == ArmorHudConfig.SlotsShown.Always_Show)) {
                        matrices.push();
                        matrices.translate(0, 0, -91);
                        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_COLOR, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
                        this.drawEmptySlotIcons(config, matrices, x, y);
                        RenderSystem.defaultBlendFunc();
                        matrices.pop();
                    }

                    // draw the armour items
                    matrices.push();
                    matrices.translate(0, 0, -241);
                    this.drawArmorItems(config, matrices, x, y, tickDelta, playerEntity);
                    matrices.pop();
                    RenderSystem.disableBlend();
                }
                armorHudItems.clear();
            }
        }
        this.client.getProfiler().pop();
    }

    @Unique
    private void drawSlots(ArmorHudConfig config, MatrixStack matrices, int x, int y, int[] slotTextures) {
        final ArmorHudConfig.Orientation orientation = config.getOrientation();
        final ArmorHudConfig.Style style = config.getStyle();
        final int borderLength = config.getBorderLength();
        final boolean matchBorderAndSlotTextures = config.isMatchBorderAndSlotTextures();
        final int slotAmount = slotTextures.length;
        final int offhandU = 24; // location on widgets.png
        final int offhandV = 23; // location on widgets.png

        // calculate slot textures
        // hotbar width = 182
        // hotbar height = 22
        int slotOffsetUV = 0;
        int slotLength = slot_length;
        int edgeSlotLength = slot_length;
        if (borderLength > 0) {
            slotOffsetUV += borderLength - 1;
            slotLength -= 2 * (borderLength - 1);
            edgeSlotLength -= borderLength - 1;
        }
        // draw slot texture
        if (slotAmount == 1)
            drawTexture(matrices, x + 1 + slotOffsetUV, y + 1 + slotOffsetUV, slotU[slotTextures[0]] + slotOffsetUV, 1 + slotOffsetUV, slotLength, slotLength);
        else {
            if (orientation == ArmorHudConfig.Orientation.Vertical) {
                for (int i = 0; i < slotAmount; i++)
                    if (i == 0)
                        drawTexture(matrices, x + 1 + slotOffsetUV, y + 1 + slotOffsetUV, slotU[slotTextures[0]] + slotOffsetUV, 1 + slotOffsetUV, slotLength, edgeSlotLength);
                    else if (i == slotAmount - 1)
                        drawTexture(matrices, x + 1 + slotOffsetUV, y + 1 + i * slot_length, slotU[slotTextures[i]] + slotOffsetUV, 1, slotLength, edgeSlotLength);
                    else
                        drawTexture(matrices, x + 1 + slotOffsetUV, y + 1 + i * slot_length, slotU[slotTextures[i]] + slotOffsetUV, 1, slotLength, slot_length);
            } else {
                for (int i = 0; i < slotAmount; i++)
                    if (i == 0)
                        drawTexture(matrices, x + 1 + slotOffsetUV, y + 1 + slotOffsetUV, slotU[slotTextures[0]] + slotOffsetUV, 1 + slotOffsetUV, edgeSlotLength, slotLength);
                    else if (i == slotAmount - 1)
                        drawTexture(matrices, x + 1 + i * slot_length, y + 1 + slotOffsetUV, slotU[slotTextures[i]], 1 + slotOffsetUV, edgeSlotLength, slotLength);
                    else
                        drawTexture(matrices, x + 1 + i * slot_length, y + 1 + slotOffsetUV, slotU[slotTextures[i]], 1 + slotOffsetUV, slot_length, slotLength);
            }
        }

        // calculate border textures
        int borderTextureX1 = slotU[0] + borderLength - 1;
        int borderTextureX2 = slotU[8] + slotLength + borderLength - 1;
        int borderTextureY1 = borderLength;
        int borderTextureY2 = slotLength + borderLength;
        int endPieceOffset = slotLength + borderLength;
        int edgePieceLength = 1 + slot_length - borderLength;
        int endBorderOffset = 2 + slot_length * slotAmount - borderLength;
        // draw border texture
        if (borderLength > 0) {
            if (orientation == ArmorHudConfig.Orientation.Vertical) {
                if (matchBorderAndSlotTextures)
                    borderTextureX1 = slotU[slotTextures[0]] + borderLength - 1;
                if (slotAmount == 1) {
                    // side borders
                    drawTexture(matrices, x, y + borderLength, 0, borderLength, borderLength, slotLength);
                    drawTexture(matrices, x + endPieceOffset, y + borderLength, borderTextureX2, borderLength, borderLength, slotLength);
                } else {
                    for (int i = 0; i < slotAmount; i++) {
                        // side borders
                        if (i == 0) {
                            drawTexture(matrices, x, y + borderLength, 0, borderLength, borderLength, edgePieceLength);
                            drawTexture(matrices, x + endPieceOffset, y + borderLength, borderTextureX2, borderLength, borderLength, edgePieceLength);
                        } else if (i == slotAmount - 1) {
                            drawTexture(matrices, x, y + 1 + i * slot_length, 0, 1, borderLength, edgePieceLength);
                            drawTexture(matrices, x + endPieceOffset, y + 1 + i * slot_length, borderTextureX2, 1, borderLength, edgePieceLength);
                        } else {
                            drawTexture(matrices, x, y + 1 + i * slot_length, 0, 1, borderLength, slot_length);
                            drawTexture(matrices, x + endPieceOffset, y + 1 + i * slot_length, borderTextureX2, 1, borderLength, slot_length);
                        }
                    }
                }
                if (style == ArmorHudConfig.Style.Rounded) {
                    // top-bottom borders
                    drawTexture(matrices, x, y, offhandU, offhandV, slot_borderedLength, borderLength);
                    drawTexture(matrices, x, y + endBorderOffset, offhandU, offhandV + endPieceOffset, slot_borderedLength, borderLength);
                } else {
                    // top border
                    drawTexture(matrices, x, y, 0, 0, borderLength, borderLength);
                    drawTexture(matrices, x + borderLength, y, borderTextureX1, 0, slotLength, borderLength);
                    drawTexture(matrices, x + endPieceOffset, y, borderTextureX2, 0, borderLength, borderLength);
                    // bottom border
                    drawTexture(matrices, x, y + endBorderOffset, 0, borderTextureY2, borderLength, borderLength);
                    drawTexture(matrices, x + borderLength, y + endBorderOffset, borderTextureX1, borderTextureY2, slotLength, borderLength);
                    drawTexture(matrices, x + endPieceOffset, y + endBorderOffset, borderTextureX2, borderTextureY2, borderLength, borderLength);
                }
            } else {
                if (slotAmount == 1) {
                    // top-bottom borders
                    drawTexture(matrices, x + borderLength, y, borderTextureX1, 0, slotLength, borderLength);
                    drawTexture(matrices, x + borderLength, y + endPieceOffset, borderTextureX1, borderTextureY2, slotLength, borderLength);
                } else {
                    int borderTextureX = slotU[0];
                    for (int i = 0; i < slotAmount; i++) {
                        // top-bottom borders
                        if (i > 0 && matchBorderAndSlotTextures)
                            borderTextureX = slotU[slotTextures[i]];
                        if (i == 0) {
                            drawTexture(matrices, x + borderLength, y, borderTextureX1, 0, edgePieceLength, borderLength);
                            drawTexture(matrices, x + borderLength, y + endPieceOffset, borderTextureX1, borderTextureY2, edgePieceLength, borderLength);
                        } else if (i == slotAmount - 1) {
                            drawTexture(matrices, x + 1 + i * slot_length, y, borderTextureX, 0, edgePieceLength, borderLength);
                            drawTexture(matrices, x + 1 + i * slot_length, y + endPieceOffset, borderTextureX, borderTextureY2, edgePieceLength, borderLength);
                        } else {
                            drawTexture(matrices, x + 1 + i * slot_length, y, borderTextureX, 0, slot_length, borderLength);
                            drawTexture(matrices, x + 1 + i * slot_length, y + endPieceOffset, borderTextureX, borderTextureY2, slot_length, borderLength);
                        }
                    }
                }
                if (style == ArmorHudConfig.Style.Rounded) {
                    // left-right borders
                    drawTexture(matrices, x, y, offhandU, offhandV, borderLength, slot_borderedLength);
                    drawTexture(matrices, x + endBorderOffset, y, offhandU + endPieceOffset, offhandV, borderLength, slot_borderedLength);
                } else {
                    // left border
                    drawTexture(matrices, x, y, 0, 0, borderLength, borderLength);
                    drawTexture(matrices, x, y + borderLength, 0, borderTextureY1, borderLength, slotLength);
                    drawTexture(matrices, x, y + endPieceOffset, 0, borderTextureY2, borderLength, borderLength);
                    // right border
                    drawTexture(matrices, x + endBorderOffset, y, borderTextureX2, 0, borderLength, borderLength);
                    drawTexture(matrices, x + endBorderOffset, y + borderLength, borderTextureX2, borderTextureY1, borderLength, slotLength);
                    drawTexture(matrices, x + endBorderOffset, y + endPieceOffset, borderTextureX2, borderTextureY2, borderLength, borderLength);
                }
            }
        }
    }

    @Unique
    private void drawEmptySlotIcons(ArmorHudConfig config, MatrixStack matrices, int x, int y) {
        for (int i = 0; i < armorHudItems.size(); i++) {
            if (armorHudItems.get(i).isEmpty()) {
                Identifier spriteId = switch (i) {
                    case 0 -> EMPTY_BOOTS_SLOT_TEXTURE;
                    case 1 -> EMPTY_LEGGINGS_SLOT_TEXTURE;
                    case 2 -> EMPTY_CHESTPLATE_SLOT_TEXTURE;
                    case 3 -> EMPTY_HELMET_SLOT_TEXTURE;
                    default -> throw new IllegalStateException("Unexpected value: " + i);
                };
                Sprite sprite = this.client.getSpriteAtlas(BLOCK_ATLAS_TEXTURE).apply(spriteId);
                RenderSystem.setShaderTexture(0, sprite.getAtlasId());

                int slotOffset = config.isReversed() ? i * slot_length : (armorHudItems.size() - i - 1) * slot_length;
                switch (config.getOrientation()) {
                    case Horizontal -> drawSprite(matrices, x + 3 + slotOffset, y + 3, 0, 16, 16, sprite);
                    case Vertical -> drawSprite(matrices, x + 3, y + 3 + slotOffset, 0, 16, 16, sprite);
                }
            }
        }
    }

    @Unique
    private void drawArmorItems(ArmorHudConfig config, MatrixStack matrices, int x, int y, float tickDelta, PlayerEntity playerEntity) {
        for (int i = 0; i < armorHudItems.size(); i++) {
            int slotOffset = config.isReversed() ? i * slot_length : (armorHudItems.size() - i - 1) * slot_length;
            switch (config.getOrientation()) {
                case Horizontal -> this.renderHotbarItem(matrices, x + 3 + slotOffset, y + 3, tickDelta, playerEntity, armorHudItems.get(i), i + 1);
                case Vertical -> this.renderHotbarItem(matrices, x + 3, y + 3 + slotOffset, tickDelta, playerEntity, armorHudItems.get(i), i + 1);
            }
        }
    }

    /**
     * Determines which config to use.
     * If the config screen is open, the preview config is returned. Otherwise, the loaded config is returned.
     *
     * @return config
     */
    @Unique
    private ArmorHudConfig getArmorHudConfig() {
        return this.client.currentScreen != null && this.client.currentScreen.getTitle() == ArmorHudMod.CONFIG_SCREEN_NAME ? ArmorHudMod.previewConfig : ArmorHudMod.getConfig();
    }
}
