package ru.berdinskiybear.armorhud;

import net.minecraft.text.Text;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.berdinskiybear.armorhud.config.ArmorHudConfig;

public final class ArmorHudMod {
    public static final String MOD_ID = "slotted_armor_hud";
    public static final String MOD_NAME = "Slotted Armor HUD";
    public static final String FABRIC_RESOURCE_LOADER_ID = "fabric-resource-loader-v0";
    public static final String CLOTH_CONFIG_ID = "cloth-config2";
    public static final Text CONFIG_SCREEN_NAME = Text.translatable("armorHud.configScreen.title");
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public static ArmorHudConfig.MutableConfig temporaryConfig;
    public static ArmorHudConfig.MutableConfig previewConfig = new ArmorHudConfig.MutableConfig();
    private static ArmorHudConfig config;

    public static ArmorHudConfig getConfig() {
        return config;
    }

    public static void setConfig(ArmorHudConfig config) {
        ArmorHudMod.config = config;
    }

    public static void writeConfig() {
        ArmorHudConfig.writeConfigFile(ArmorHudMod.getConfig());
    }

    public static void readConfig() {
        ArmorHudMod.setConfig(ArmorHudConfig.readConfigFile());
    }

    public static void log(String message) {
        log(Level.INFO, message);
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, "[" + MOD_NAME + "] " + message);
    }
}
