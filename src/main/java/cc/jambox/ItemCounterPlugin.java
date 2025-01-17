package cc.jambox;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

import java.util.HashMap;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Regex Item Counter"
)
public class ItemCounterPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ItemCounterConfig config;

	@Inject
	private ClientThread clientThread;

	private HashMap<String, ItemCounter> itemMap;

	private Pattern[] regexes;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ItemManager itemManager;

	@Override
	protected void startUp() throws Exception
	{
		regexes = Text.fromCSV(config.itemList()).stream()
				.map(n -> Pattern.compile(n, Pattern.CASE_INSENSITIVE)).toArray(Pattern[]::new);
		itemMap = new HashMap<>();
		clientThread.invoke(this::update);
	}

	@Override
	protected void shutDown() throws Exception
	{
		itemMap.values().stream().forEach(rem -> infoBoxManager.removeInfoBox(rem));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged) {
		if (!configChanged.getGroup().equals("regexitemcounter")) {
			return;
		}
		itemMap.values().stream().forEach(rem -> infoBoxManager.removeInfoBox(rem));
		itemMap.clear();
		regexes = Text.fromCSV(config.itemList()).stream()
				.map(n -> Pattern.compile(n, Pattern.CASE_INSENSITIVE)).toArray(Pattern[]::new);
		clientThread.invoke(this::update);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		ItemContainer eqp = client.getItemContainer(InventoryID.EQUIPMENT);
		if (inv == null || eqp == null || (event.getItemContainer() != inv && event.getItemContainer() != eqp)) return;

		checkInventory(flattenItemArrayParams(inv.getItems(), eqp.getItems()));
	}

	public void update() {

		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		ItemContainer eqp = client.getItemContainer(InventoryID.EQUIPMENT);
		Item[] invItems = inv == null ? new Item[0] : inv.getItems();
		Item[] eqpItems = eqp == null ? new Item[0] : eqp.getItems();
		if (inv != null && eqp != null) {
			clientThread.invokeLater(() -> {
				checkInventory(flattenItemArrayParams(invItems, eqpItems));
			});
		}
	}

	private void checkInventory(final Item[] invItems) {
		for (Pattern regex: regexes) {
			int running_total = 0;
			for (Item item: invItems) {
				int itemId = item.getId();
				String itemName = itemManager.getItemComposition(itemId).getName();
				if (regex.matcher(itemName).matches()) {
					running_total += config.countQuantity() ? item.getQuantity() : 1;
					ItemCounter counter = itemMap.getOrDefault(regex.pattern(), null);
					if (counter == null) {
						counter = new ItemCounter(itemManager.getImage(itemId), itemId, regex.pattern(),
								running_total, this, config.formatAsOsrsNumber());
						infoBoxManager.addInfoBox(counter);
						itemMap.put(regex.pattern(), counter);
					}
				}
			}
			ItemCounter counter = itemMap.getOrDefault(regex.pattern(), null);
			if (counter != null) {
				counter.setCount(running_total);
			}
		}
	}
	public static Item[] flattenItemArrayParams(Item[]... stuff) {
		return flattenItemArrays(stuff);
	}
	public static Item[] flattenItemArrays(Item[][] itemSets) {
		int total_len = 0;
		for (Item[] itemSet : itemSets) {
			if (itemSet != null) {
				total_len += itemSet.length;
			}
		}
		Item[] result = new Item[total_len];
		int overall_idx = 0;
		for (Item[] itemSet : itemSets) {
			if (itemSet != null) {
				for (int i = 0; i < itemSet.length; i++) {
					result[overall_idx++] = itemSet[i];
				}
			}
		}
		return result;
	}

	@Provides
    ItemCounterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ItemCounterConfig.class);
	}
}
