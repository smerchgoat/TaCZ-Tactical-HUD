package com.tacticalhud;

import com.tacticalhud.client.ClientSetup;
import com.tacticalhud.config.HudConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Angled 3D gun HUD for TaCZ. Purely client-side: on a dedicated server the mod loads but does nothing,
 * and servers do not need it installed.
 */
@Mod(TacticalHud.MOD_ID)
public final class TacticalHud {
    public static final String MOD_ID = "tacz_tactical_hud";

    public TacticalHud() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
