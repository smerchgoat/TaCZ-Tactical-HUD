package com.tacticalhud.client.hud;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Magazine capacity and reserve ammo of the held gun, refreshed at most once per tick.
 * <p>
 * Mirrors the cache of TaCZ's own {@code GunHudOverlay} (GPL-3.0), including one side effect that must be kept:
 * for guns fed straight from the inventory, the counted ammo is written back into the gun. TaCZ does this from
 * its HUD, so while this HUD replaces it, this cache has to do it instead.
 */
final class AmmoCache {
    /** Same cap as TaCZ's HUD; creative ammo boxes and players who need no ammo report this value. */
    static final int MAX_AMMO = 9999;
    private static final long REFRESH_INTERVAL_MS = 50L;

    private long lastRefreshMs = -1L;
    private int magCapacity;
    private int reserveAmmo;

    void refresh(LocalPlayer player, ItemStack gun, IGun iGun, GunData gunData) {
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs <= REFRESH_INTERVAL_MS) {
            return;
        }
        lastRefreshMs = now;
        magCapacity = AttachmentDataUtils.getAmmoCountWithAttachment(gun, gunData);
        if (!IGunOperator.fromLivingEntity(player).needCheckAmmo()) {
            reserveAmmo = MAX_AMMO;
        } else if (iGun.useDummyAmmo(gun)) {
            reserveAmmo = iGun.getDummyAmmoAmount(gun);
        } else {
            reserveAmmo = countInventoryAmmo(gun, player.getInventory());
        }
        if (iGun.useInventoryAmmo(gun)) {
            iGun.setCurrentAmmoCount(gun, reserveAmmo);
        }
    }

    int magCapacity() {
        return magCapacity;
    }

    int reserveAmmo() {
        return reserveAmmo;
    }

    private static int countInventoryAmmo(ItemStack gun, Inventory inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gun, stack)) {
                count += stack.getCount();
            }
            if (stack.getItem() instanceof IAmmoBox box && box.isAmmoBoxOfGun(gun, stack)) {
                if (box.isAllTypeCreative(stack) || box.isCreative(stack)) {
                    return MAX_AMMO;
                }
                count += box.getAmmoCount(stack);
            }
        }
        return count;
    }
}
