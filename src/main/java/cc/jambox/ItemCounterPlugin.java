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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import static net.runelite.api.InventoryID.EQUIPMENT;
import static net.runelite.api.InventoryID.INVENTORY;

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
		clientThread.invokeLater(this::checkInventory);
	}

	@Override
	protected void shutDown() throws Exception
	{
		itemMap.values().forEach(rem -> infoBoxManager.removeInfoBox(rem));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged) {
		if (!configChanged.getGroup().equals("regexitemcounter")) {
			return;
		}
		itemMap.values().forEach(rem -> infoBoxManager.removeInfoBox(rem));
		itemMap.clear();
		regexes = Text.fromCSV(config.itemList()).stream()
				.map(n -> Pattern.compile(n, Pattern.CASE_INSENSITIVE)).toArray(Pattern[]::new);
		clientThread.invokeLater(this::checkInventory);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		ItemContainer itemContainer = event.getItemContainer();
		if (itemContainer.getId() != INVENTORY.getId() && itemContainer.getId() != EQUIPMENT.getId()) return;

		checkInventory();
	}

	private void checkInventory() {
		ItemContainer inv = client.getItemContainer(INVENTORY);
		ItemContainer eqp = client.getItemContainer(EQUIPMENT);
		if (inv == null || eqp == null) return;
		final Item[] invItems = flattenItemArrays(inv.getItems(), eqp.getItems());

		for (Pattern regex: regexes) {
			int running_total = 0;
			for (Item item: invItems) {
				int itemId = item.getId();
				String itemName = itemManager.getItemComposition(itemId).getName();
                if (!regex.matcher(itemName).matches()) continue;

                running_total += config.countQuantity() ? item.getQuantity() : 1;
				itemMap.computeIfAbsent(regex.pattern(), pattern -> registerCounter(itemId, pattern));
            }
			if (itemMap.containsKey(regex.pattern())) {
				itemMap.get(regex.pattern()).setCount(running_total);
			}
		}
	}

	private ItemCounter registerCounter(int itemId, String pattern) {
		ItemCounter counter = new ItemCounter(itemManager.getImage(itemId), itemId, pattern, 0, this,
				config.formatAsOsrsNumber());
		infoBoxManager.addInfoBox(counter);
		return counter;
	}

	private static Item[] flattenItemArrays(Item[]... itemSets) {
		int totalLen = Arrays.stream(itemSets).mapToInt(Array::getLength).reduce(0, Integer::sum);
		Item[] result = new Item[totalLen];
		int overall_idx = 0;
		for (Item[] itemSet : itemSets) {
            if (itemSet == null) continue;

            for (Item item : itemSet) {
                result[overall_idx++] = item;
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
