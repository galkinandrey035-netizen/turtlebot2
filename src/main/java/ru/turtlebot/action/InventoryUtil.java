package ru.turtlebot.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class InventoryUtil {
    private InventoryUtil() {}

    public static int countSeagrass(ClientPlayerEntity p) {
        int n = 0;
        for (ItemStack s : p.getInventory().main) if (s.isOf(Items.SEAGRASS)) n += s.getCount();
        return n;
    }

    public static boolean ensureSeagrassInHand(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p.getMainHandStack().isOf(Items.SEAGRASS)) return true;
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.SEAGRASS)) { inv.selectedSlot = i; return true; }
        }
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).isOf(Items.SEAGRASS)) {
                mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, i,
                        inv.selectedSlot, SlotActionType.SWAP, p);
                return true;
            }
        }
        return false;
    }
}
