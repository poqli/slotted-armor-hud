package ru.berdinskiybear.armorhud.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.berdinskiybear.armorhud.ArmorHudMod;
import ru.berdinskiybear.armorhud.config.ArmorHudConfig;
import java.util.List;

@Mixin(InGameHud.class)
public class StatusEffectHudMixin {
    @Shadow @Final
    private MinecraftClient client;
    @Unique
    private int offset = 0;

    @Inject(method = "renderStatusEffectOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/effect/StatusEffectInstance;shouldShowIcon()Z", shift = At.Shift.AFTER))
    public void calculateOffset(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ArmorHudConfig config = this.getArmorHudConfig();
        if (config.isEnabled() && config.isPushStatusEffectIcons() && config.getAnchor() == ArmorHudConfig.Anchor.Top && config.getSide() == ArmorHudConfig.Side.Right) {
            int add = 0;
            int amount = 0;
            PlayerEntity playerEntity = this.getCameraPlayer();
            if (playerEntity != null) {
                if (config.getSlotsShown() == ArmorHudConfig.SlotsShown.Always_Show)
                    amount = 4;
                else {
                    List<ItemStack> armorList = playerEntity.getInventory().armor;
                    for (ItemStack itemStack : armorList) {
                        if (!itemStack.isEmpty()) {
                            amount++;
                            if (config.getSlotsShown() != ArmorHudConfig.SlotsShown.Show_Equipped) {
                                amount = 4;
                                break;
                            }
                        }
                    }
                }
                if (amount != 0)
                    add += 22 + config.getOffsetY() + config.getStatusEffectIconSpacing();
                if (config.getOrientation() == ArmorHudConfig.Orientation.Vertical)
                    add += 20 * (amount - 1);
            }
            this.offset = Math.max(add, 0);
        } else
            this.offset = 0;
    }

    @ModifyVariable(method = "renderStatusEffectOverlay", at = @At(value = "STORE", ordinal = 0), ordinal = 3)
    public int statusEffectIconsOffset(int y) {
        return y + this.offset;
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

    @Unique
    private PlayerEntity getCameraPlayer() {
        return !(this.client.getCameraEntity() instanceof PlayerEntity) ? null : (PlayerEntity) this.client.getCameraEntity();
    }
}
