package cc.jambox;

import lombok.Getter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.util.QuantityFormatter;

import java.awt.image.BufferedImage;

public class ItemCounter extends Counter {
    @Getter
    private final int itemID;
    private final String name;
    ItemCounter(BufferedImage image, int itemID, String name, int count, Plugin plugin) {
        super(image, plugin, count);
        this.itemID = itemID;
        this.name = name;
    }

    @Override
    public String getText()
    {
        return QuantityFormatter.quantityToRSDecimalStack(getCount());
    }

    @Override
    public String getTooltip()
    {
        return name;
    }
}
