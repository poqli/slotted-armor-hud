package ru.berdinskiybear.armorhud.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
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

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Shadow @Final
    private MinecraftClient client;
    @Unique
    private int offset = 0;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;getMatrices()Lnet/minecraft/client/util/math/MatrixStack;", ordinal = 0, shift = At.Shift.AFTER))
    public void calculateOffset(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        ArmorHudConfig config = this.getArmorHudConfig();
        if (config.isEnabled() && config.isPushChatBox() && config.getAnchor() == ArmorHudConfig.Anchor.Bottom && config.getSide() == ArmorHudConfig.Side.Left && config.getOrientation() == ArmorHudConfig.Orientation.Vertical) {
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
                if (amount != 0 && config.getOffsetY() > config.getMinOffsetBeforePushingChatBox())
                    add += config.getOffsetY() - config.getMinOffsetBeforePushingChatBox();
                if (config.getOrientation() == ArmorHudConfig.Orientation.Vertical)
                    add += 20 * (amount - 1);
            }
            this.offset = Math.max(add, 0);
        } else
            this.offset = 0;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", shift = At.Shift.AFTER, ordinal = 0))
    public void offset(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        context.getMatrices().translate(0.0F, -((float) this.offset), 0.0F);
    }

    @ModifyVariable(method = "toChatLineY", at = @At(value = "STORE", ordinal = 0), ordinal = 1)
    public double offsetYConversion(double d) {
        return d - this.offset;
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
