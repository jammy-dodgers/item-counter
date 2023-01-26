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
		name = "Regex match list",
		description = "Separate with ,"
	)
	default String itemList()
	{
		return "Mole (?:skin|claw),(Grimy\\s)?torstol";
	}

	@ConfigItem(
			keyName = "formatAsOsrsNumber",
			name="Format as OSRS Number",
			description = "Format like an osrs number (ie 18.4k instead of 18403)"
	)
	default boolean formatAsOsrsNumber() {return true;}

	@ConfigItem(
			keyName = "countQuantity",
			name="Count quantity of stackables",
			description = "Should stackable items count as the quantity? (If false, stackables count as 1 when matched)"
	)
	default boolean countQuantity() {return true;}
}
