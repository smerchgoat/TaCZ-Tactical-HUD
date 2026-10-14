package com.tacticalhud.client.hud;

import com.tacticalhud.TacticalHud;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunHeatData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class HudSnapshotFactory {
    /** Items counted as grenades / medkits. Data-driven so modpacks can add their own throwables and heals. */
    static final TagKey<Item> GRENADES = TagKey.create(Registries.ITEM, new ResourceLocation(TacticalHud.MOD_ID, "grenades"));
    static final TagKey<Item> MEDKITS = TagKey.create(Registries.ITEM, new ResourceLocation(TacticalHud.MOD_ID, "medkits"));

    private static final int MAX_DISPLAYED_AMMO = AmmoCache.MAX_AMMO;
    private static final String INFINITE = "∞";
    private static final String NO_VALUE = "--";

    private HudSnapshotFactory() {
    }

    static HudSnapshot capture(LocalPlayer player, ItemStack gun, IGun iGun, GunData gunData,
                               GunDisplayInstance display, AmmoCache ammoCache, float partialTick) {
        ammoCache.refresh(player, gun, iGun, gunData);
        boolean inventoryFed = iGun.useInventoryAmmo(gun);
        int reserve = ammoCache.reserveAmmo();
        int barrelRound = iGun.hasBulletInBarrel(gun) && gunData.getBolt() != Bolt.OPEN_BOLT ? 1 : 0;
        int magAmmo = Math.min((inventoryFed ? reserve : iGun.getCurrentAmmoCount(gun)) + barrelRound, MAX_DISPLAYED_AMMO);
        int magCapacity = Math.max(ammoCache.magCapacity(), 1);

        boolean infiniteReserve = gunData.getReloadData().isInfinite() || reserve >= MAX_DISPLAYED_AMMO;
        String reserveText;
        String spareMagsText;
        if (inventoryFed) {
            reserveText = "";
            spareMagsText = NO_VALUE;
        } else if (infiniteReserve) {
            reserveText = INFINITE;
            spareMagsText = INFINITE;
        } else {
            reserveText = Integer.toString(reserve);
            spareMagsText = twoDigits(Mth.ceil((float) reserve / magCapacity));
        }

        GunHeatData heatData = gunData.getHeatData();
        boolean hasHeat = heatData != null && iGun.hasHeatData(gun);
        float heat = hasHeat ? Mth.clamp(iGun.getHeatAmount(gun) / heatData.getHeatMax(), 0f, 1f) : HudSnapshot.NO_HEAT;
        boolean overheatLocked = hasHeat && iGun.isOverheatLocked(gun);

        ReloadState reloadState = IGunOperator.fromLivingEntity(player).getSynReloadState();
        Inventory inventory = player.getInventory();
        float health = player.getMaxHealth() > 0 ? Mth.clamp(player.getHealth() / player.getMaxHealth(), 0f, 1f) : 0f;

        return new HudSnapshot(
                iGun.getGunId(gun),
                magAmmo,
                magCapacity,
                reserveText,
                spareMagsText,
                overheatLocked,
                heat,
                iGun.getFireMode(gun),
                display.getHUDTexture(),
                reloadState.getStateType(),
                reloadState.getCountDown(),
                otherHotbarGunAmmo(inventory),
                countTagged(inventory, GRENADES),
                countTagged(inventory, MEDKITS),
                health,
                player.getArmorValue(),
                IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(partialTick)
        );
    }

    private static String[] otherHotbarGunAmmo(Inventory inventory) {
        String[] result = new String[HudSnapshot.OTHER_GUN_SLOTS];
        int found = 0;
        for (int slot = 0; slot < Inventory.getSelectionSize() && found < result.length; slot++) {
            if (slot == inventory.selected) {
                continue;
            }
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof IGun otherGun) {
                int ammo = otherGun.getCurrentAmmoCount(stack) + (otherGun.hasBulletInBarrel(stack) ? 1 : 0);
                result[found++] = twoDigits(Math.min(ammo, MAX_DISPLAYED_AMMO));
            }
        }
        return result;
    }

    private static int countTagged(Inventory inventory, TagKey<Item> tag) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(tag)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }
}
