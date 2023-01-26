package cc.jambox;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.text.html.Option;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
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
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@PluginDescriptor(
	name = "Item Counter"
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
		update();

		log.info("ItemCounterPlugin started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		itemMap.values().stream().forEach(rem -> infoBoxManager.removeInfoBox(rem));
		log.info("ItemCounterPlugin stopped!");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged) {
		if (!configChanged.getGroup().equals("itemcounter")) {
			return;
		}
		log.info("itemcounter config changed");
		itemMap.values().stream().forEach(rem -> infoBoxManager.removeInfoBox(rem));
		itemMap.clear();
		regexes = Text.fromCSV(config.itemList()).stream()
				.map(n -> Pattern.compile(n, Pattern.CASE_INSENSITIVE)).toArray(Pattern[]::new);
		update();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		ItemContainer eqp = client.getItemContainer(InventoryID.EQUIPMENT);
		if ((event.getItemContainer() == inv) || (event.getItemContainer() == eqp))
		{
			checkInventory(multicat_p(Item[].class,inv.getItems(), eqp.getItems()));
		}
	}

	public void update() {

		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		ItemContainer eqp = client.getItemContainer(InventoryID.EQUIPMENT);
		Item[] invItems = inv == null ? new Item[0] : inv.getItems();
		Item[] eqpItems = eqp == null ? new Item[0] : eqp.getItems();
		if (inv != null && eqp != null) {
			clientThread.invokeLater(() -> {
				checkInventory(multicat_p(Item[].class, invItems, eqpItems));
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

	public static <T> T[] multicat_p(Class<T[]> clss, T[]... stuff) {
		return multicat_a(clss, stuff);
	}
	public static <T> T[] multicat_a(Class<T[]> clss, T[][] stuff) {
		int total_len = 0;
		for (T[] a : stuff) {
			if (a != null) {
				total_len += a.length;
			}
		}
					// i seriously despise type erasure
		T[] result = clss.cast(Array.newInstance(clss.getComponentType(), total_len));
		int overall_idx = 0;
		for (T[] thingy : stuff) {
			if (thingy != null) {
				for (int i = 0; i < thingy.length; i++) {
					result[overall_idx++] = thingy[i];
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
