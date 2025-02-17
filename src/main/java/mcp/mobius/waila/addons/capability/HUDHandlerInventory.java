package mcp.mobius.waila.addons.capability;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import mcp.mobius.waila.api.*;
import mcp.mobius.waila.utils.InventoryUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public class HUDHandlerInventory implements IWailaDataProvider {

    static final IWailaDataProvider INSTANCE = new HUDHandlerInventory();
    // A set of tile names that need to be ignored in order to avoid network overload
    // Yay hardcoding, but it's better than nothing for now
    private static final Set<String> INVENTORY_IGNORE = Sets.newHashSet(
            "refinedstorage:disk_drive"
    );

    @Nonnull
    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        if (!config.getConfig("capability.inventoryinfo") || accessor.getTileEntity() == null || accessor.getTileEntity().getClass() == TileEntityFurnace.class)
            return currenttip;

        if (accessor.getNBTData().hasKey("handler")) {
            int handlerSize = accessor.getNBTData().getInteger("handlerSize");
            ItemStackHandler itemHandler = new ItemStackHandler();
            itemHandler.setSize(handlerSize);
            InventoryUtils.populateInv(itemHandler, accessor.getNBTData().getTagList("handler", 10));

            String renderString = "";
            int drawnCount = 0;
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (stack.isEmpty())
                    continue;
                String name = stack.getItem().getRegistryName().toString();
                if (drawnCount >= 5) {
                    currenttip.add(renderString);
                    renderString = "";
                    drawnCount = 0;
                }

                String nbt = "";
                if (stack.hasTagCompound())
                    nbt = stack.getTagCompound().toString();
                renderString += SpecialChars.getRenderString("waila.stack", "1", name, String.valueOf(stack.getCount()), String.valueOf(stack.getItemDamage()), nbt);
                drawnCount += 1;
            }

            if (!Strings.isNullOrEmpty(renderString))
                ((ITaggedList<String, String>) currenttip).add(renderString, "IItemHandler");
        }
        return currenttip;
    }

    @Nonnull
    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, BlockPos pos) {
        if (te != null && !INVENTORY_IGNORE.contains(tag.getString("id"))) {
            tag.removeTag("Items"); // Should catch all inventories that do things the standard way. Keeps from duplicating the item list and doubling the packet size
            if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                IItemHandler itemHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                tag.setTag("handler", InventoryUtils.invToNBT(itemHandler, player.isSneaking() ? itemHandler.getSlots() : 5));
                tag.setInteger("handlerSize", player.isSneaking() ? itemHandler.getSlots() : 5);
            } else if (te instanceof IInventory) {
                IItemHandler itemHandler = new InvWrapper((IInventory) te);
                tag.setTag("handler", InventoryUtils.invToNBT(itemHandler, player.isSneaking() ? itemHandler.getSlots() : 5));
                tag.setInteger("handlerSize", player.isSneaking() ? itemHandler.getSlots() : 5);
            } else if (te instanceof TileEntityEnderChest) {
                IItemHandler itemHandler = new InvWrapper(player.getInventoryEnderChest());
                tag.setTag("handler", InventoryUtils.invToNBT(itemHandler, player.isSneaking() ? itemHandler.getSlots() : 5));
                tag.setInteger("handlerSize", player.isSneaking() ? itemHandler.getSlots() : 5);
            }
        }

        return tag;
    }
}
