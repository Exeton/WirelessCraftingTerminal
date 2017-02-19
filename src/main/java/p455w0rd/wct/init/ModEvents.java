/*
 * This file is part of Wireless Crafting Terminal. Copyright (c) 2016, p455w0rd
 * (aka TheRealp455w0rd), All rights reserved unless otherwise stated.
 *
 * Wireless Crafting Terminal is free software: you can redistribute it and/or
 * modify it under the terms of the MIT License.
 *
 * Wireless Crafting Terminal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the MIT License for
 * more details.
 *
 * You should have received a copy of the MIT License along with Wireless
 * Crafting Terminal. If not, see <https://opensource.org/licenses/MIT>.
 */
package p455w0rd.wct.init;

import java.util.Random;

import org.lwjgl.input.Keyboard;

import appeng.tile.networking.TileController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import p455w0rd.wct.Globals;
import p455w0rd.wct.WCT;
import p455w0rd.wct.api.IWirelessCraftingTerminalItem;
import p455w0rd.wct.client.gui.GuiWCT;
import p455w0rd.wct.handlers.GuiHandler;
import p455w0rd.wct.items.ItemMagnet;
import p455w0rd.wct.sync.WCTPacket;
import p455w0rd.wct.sync.network.NetworkHandler;
import p455w0rd.wct.sync.packets.PacketConfigSync;
import p455w0rd.wct.sync.packets.PacketOpenGui;
import p455w0rd.wct.sync.packets.PacketSetMagnet;
import p455w0rd.wct.util.WCTUtils;
import p455w0rdslib.capabilities.CapabilityChunkLoader;
import p455w0rdslib.capabilities.CapabilityChunkLoader.ProviderTE;
import p455w0rdslib.util.ChunkUtils;

/**
 * @author p455w0rd
 *
 */
public class ModEvents {

	public static void init() {
		MinecraftForge.EVENT_BUS.register(new ModEvents());
		ChunkUtils.register(WCT.INSTANCE);
	}

	@SubscribeEvent
	public void attachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
		if (event.getObject() instanceof TileController && ModConfig.WCT_ENABLE_CONTROLLER_CHUNKLOADER) {
			TileController controller = (TileController) event.getObject();
			event.addCapability(new ResourceLocation(Globals.MODID, "chunkloader"), new ProviderTE(controller));
		}
	}

	@SubscribeEvent
	public void onPlace(BlockEvent.PlaceEvent e) {
		World world = e.getWorld();
		BlockPos pos = e.getPos();
		if (world != null && pos != null && world.getTileEntity(pos) != null && !world.isRemote && ModConfig.WCT_ENABLE_CONTROLLER_CHUNKLOADER) {
			if (world.getTileEntity(pos) instanceof TileController) {
				TileEntity tile = world.getTileEntity(pos);
				if (tile.hasCapability(CapabilityChunkLoader.CAPABILITY_CHUNKLOADER_TE, null)) {
					tile.getCapability(CapabilityChunkLoader.CAPABILITY_CHUNKLOADER_TE, null).attachChunkLoader(WCT.INSTANCE);
				}
			}
		}
	}

	@SubscribeEvent
	public void onBreak(BlockEvent.BreakEvent e) {
		World world = e.getWorld();
		BlockPos pos = e.getPos();
		if (world != null && pos != null && world.getTileEntity(pos) != null && !world.isRemote && ModConfig.WCT_ENABLE_CONTROLLER_CHUNKLOADER) {
			if (world.getTileEntity(pos) instanceof TileController) {
				TileEntity tile = world.getTileEntity(pos);
				if (tile.hasCapability(CapabilityChunkLoader.CAPABILITY_CHUNKLOADER_TE, null)) {
					tile.getCapability(CapabilityChunkLoader.CAPABILITY_CHUNKLOADER_TE, null).detachChunkLoader(WCT.INSTANCE);
				}
			}
		}
	}

	@SubscribeEvent
	public void tickEvent(TickEvent.PlayerTickEvent e) {
		ItemStack wirelessTerm = null;
		EntityPlayer player = e.player;
		IInventory playerInv = player.inventory;

		int invSize = playerInv.getSizeInventory();
		if (invSize <= 0) {
			return;
		}
		for (int i = 0; i < invSize; ++i) {
			ItemStack item = playerInv.getStackInSlot(i);
			if (item == null) {
				continue;
			}
			if (item.getItem() instanceof IWirelessCraftingTerminalItem) {
				wirelessTerm = item;
			}
			if (wirelessTerm == null) {
				continue;
			}
			if (wirelessTerm.hasTagCompound()) {
				NBTTagCompound nbtTC = wirelessTerm.getTagCompound();
				NBTTagList tagList = nbtTC.getTagList("MagnetSlot", 10);
				if (tagList != null) {
					NBTTagCompound magCompound = tagList.getCompoundTagAt(0);
					if (magCompound != null) {
						ItemStack magnetItem = ItemStack.loadItemStackFromNBT(magCompound);
						if (magnetItem != null) {
							((ItemMagnet) magnetItem.getItem()).setItemStack(magnetItem);
							if (magnetItem.getItem() instanceof ItemMagnet) {
								((ItemMagnet) magnetItem.getItem()).doMagnet(magnetItem, e.player.worldObj, e.player, wirelessTerm);
								continue;
							}
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onKeyInput(KeyInputEvent event) {
		EntityPlayer p = Minecraft.getMinecraft().thePlayer;
		if (p.openContainer == null) {
			return;
		}
		if (p.openContainer != null && p.openContainer instanceof ContainerPlayer) {
			if (ModKeybindings.openTerminal.getKeyCode() != Keyboard.CHAR_NONE && ModKeybindings.openTerminal.isPressed()) {
				ItemStack is = WCTUtils.getWirelessTerm(p.inventory);
				if (is == null) {
					return;
				}
				IWirelessCraftingTerminalItem wirelessTerm = (IWirelessCraftingTerminalItem) is.getItem();
				if (wirelessTerm != null && wirelessTerm.isWirelessCraftingEnabled(is)) {
					if (!FMLClientHandler.instance().isGUIOpen(GuiWCT.class)) {
						NetworkHandler.instance().sendToServer(new PacketOpenGui(GuiHandler.GUI_WCT));
					}
				}
			}
			else if (ModKeybindings.openMagnetFilter.getKeyCode() != Keyboard.CHAR_NONE && ModKeybindings.openMagnetFilter.isPressed()) {
				ItemStack magnetItem = WCTUtils.getMagnet(p.inventory);
				//ensure player has a Wireless Crafting Terminal (with Magnet Card Installed) or Magnet Card in their inventory
				//and that they have manually right=clicked it to initialize it
				if (WCTUtils.isMagnetInitialized(magnetItem)) {
					NetworkHandler.instance().sendToServer(new PacketOpenGui(GuiHandler.GUI_MAGNET));
				}
				else { // TODO fix this shit
					p.addChatMessage(new TextComponentString(I18n.format("chatmessages.magnet_init.desc")));
				}

			}

			else if (ModKeybindings.changeMagnetMode.getKeyCode() != Keyboard.CHAR_NONE && ModKeybindings.changeMagnetMode.isPressed()) {
				ItemStack magnetItem = WCTUtils.getMagnet(p.inventory);
				if (magnetItem != null && WCTUtils.isMagnetInitialized(magnetItem)) {
					NetworkHandler.instance().sendToServer(new PacketSetMagnet(magnetItem.getItemDamage()));
					switch (magnetItem.getItemDamage()) {
					case 2:
						p.addChatMessage(new TextComponentString(I18n.format("chatmessages.magnet_deactivated.desc")));

						break;
					case 1:
						p.addChatMessage(new TextComponentString(I18n.format("chatmessages.magnet_activated.desc") + " - " + I18n.format("tooltip.magnet_active_2.desc")));
						break;
					case 0:
						p.addChatMessage(new TextComponentString(I18n.format("chatmessages.magnet_activated.desc") + " - " + I18n.format("tooltip.magnet_active_1.desc")));
						break;
					}
				}
			}
			else {
				return;
			}
		}
	}

	@SubscribeEvent
	public void onMobDrop(LivingDropsEvent event) {
		ItemStack stack = new ItemStack(ModItems.BOOSTER_CARD);
		EntityItem drop = new EntityItem(event.getEntityLiving().getEntityWorld(), event.getEntity().posX, event.getEntity().posY, event.getEntity().posZ, stack);
		if (event.getEntity() instanceof EntityDragon) {
			event.getDrops().add(drop);
		}
		if (event.getEntity() instanceof EntityWither && ModConfig.WCT_BOOSTER_ENABLED) {
			Random rand = event.getEntityLiving().getEntityWorld().rand;
			int n = rand.nextInt(100);
			if (n < ModConfig.WCT_BOOSTER_DROPCHANCE) {
				event.getDrops().add(drop);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerLoggedInEvent e) {
		if (e.player instanceof EntityPlayerMP) {
			final PacketConfigSync p = new PacketConfigSync(ModConfig.WCT_MAX_POWER, ModConfig.WCT_BOOSTER_ENABLED, ModConfig.WCT_BOOSTER_DROPCHANCE, ModConfig.WCT_MINETWEAKER_OVERRIDE, ModConfig.WCT_ENABLE_CONTROLLER_CHUNKLOADER);
			NetworkHandler.instance().sendTo((WCTPacket) p, (EntityPlayerMP) e.player);
		}
	}

}
