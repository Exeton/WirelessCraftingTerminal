package p455w0rd.wct.init;

import baubles.common.Baubles;
import net.minecraftforge.fml.common.Loader;

public class ModGlobals {
	public static final String MODID = "wct";
	public static final String VERSION = "0.2.39";
	public static final String NAME = "Wireless Crafting Terminal";
	public static final String SERVER_PROXY = "p455w0rd.wct.proxy.CommonProxy";
	public static final String CLIENT_PROXY = "p455w0rd.wct.proxy.ClientProxy";
	public static final String GUI_FACTORY = "p455w0rd.wct.client.gui.GuiFactory";
	public static final String DEP_LIST = "required-after:appliedenergistics2@[rv5-alpha-5,);required-after:p455w0rdslib;after:" + Baubles.MODID + ";after:mousetweaks;";
	public static final String CONFIG_FILE = "config/WirelessCraftingTerminal.cfg";

	public static enum Mods {
			BAUBLES("baubles", "Baubles"),
			BAUBLESAPI("Baubles|API", "Baubles API"),
			JEI("jei", "Just Enough Items");

		private String modid, name;

		Mods(String modidIn, String nameIn) {
			modid = modidIn;
			name = nameIn;
		}

		public String getId() {
			return modid;
		}

		public String getName() {
			return name;
		}

		public boolean isLoaded() {
			return Loader.isModLoaded(getId());
		}
	}
}
