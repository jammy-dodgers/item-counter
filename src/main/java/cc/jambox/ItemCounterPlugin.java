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
		regexes = Arrays.stream(config.itemList().split(",")).map(n -> Pattern.compile(n, Pattern.CASE_INSENSITIVE)).toArray(Pattern[]::new);
		itemMap = new HashMap<>();
		clientThread.invokeLater(() ->
		{
			ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
			ItemContainer eqp = client.getItemContainer(InventoryID.EQUIPMENT);
			Item[] invItems = inv == null ? new Item[0] : inv.getItems();
			Item[] eqpItems = eqp == null ? new Item[0] : eqp.getItems();
			if (inv != null && eqp != null) {
				checkInventory(Stream.concat(Arrays.stream(invItems), Arrays.stream(eqpItems)).toArray(Item[]::new));
			}
		});

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
		if (configChanged.getGroup() != "itemcounter") {
			return;
		}
		itemMap.values().stream().forEach(rem -> infoBoxManager.removeInfoBox(rem));
		regexes = Arrays.stream(config.itemList().split(",")).map(n -> Pattern.compile(n, Pattern.CASE_INSENSITIVE)).toArray(Pattern[]::new);
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		ItemContainer eqp = client.getItemContainer(InventoryID.EQUIPMENT);
		Item[] invItems = inv == null ? new Item[0] : inv.getItems();
		Item[] eqpItems = eqp == null ? new Item[0] : eqp.getItems();
		if (inv != null && eqp != null) {
			clientThread.invokeLater(() -> {
				checkInventory(Stream.concat(Arrays.stream(invItems), Arrays.stream(eqpItems)).toArray(Item[]::new));
			});
		}
	}

//	@Subscribe
//	public void onGameStateChanged(GameStateChanged gameStateChanged)
//	{
//		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
//		{
//			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
//		}
//	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		ItemContainer eqp = client.getItemContainer(InventoryID.EQUIPMENT);
		if ((event.getItemContainer() == inv) || (event.getItemContainer() == eqp))
		{
			checkInventory(Stream.concat(Arrays.stream(inv.getItems()), Arrays.stream(eqp.getItems())).toArray(Item[]::new));
		}
	}

	private void checkInventory(final Item[] invItems) {
		for (Pattern regex: regexes) {
			boolean matchedAny = false;
			int running_total = 0;
			for (Item item: invItems) {
				int itemId = item.getId();
				String itemName = itemManager.getItemComposition(itemId).getName();
				if (regex.matcher(itemName).matches()) {
					running_total+=item.getQuantity();
					matchedAny = true;
					ItemCounter counter = itemMap.getOrDefault(regex.pattern(), null);
					if (counter == null) {
						counter = new ItemCounter(itemManager.getImage(itemId), itemId, regex.pattern(), running_total, this);
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

	@Provides
    ItemCounterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ItemCounterConfig.class);
	}
}
