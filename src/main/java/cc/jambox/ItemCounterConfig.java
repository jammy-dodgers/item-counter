package cc.jambox;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.util.Arrays;

@ConfigGroup("itemcounter")
public interface ItemCounterConfig extends Config
{
	@ConfigItem(
		keyName = "itemList",
		name = "Item name list",
		description = "Separate with ,"
	)
	default String itemList()
	{
		return "watermelon,logs,big bones";
	}
}
