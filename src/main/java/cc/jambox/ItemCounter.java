package cc.jambox;

import lombok.Getter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.util.QuantityFormatter;

import java.awt.image.BufferedImage;
import java.text.NumberFormat;

public class ItemCounter extends Counter {
    @Getter
    private final int itemID;
    private final String name;

    private final boolean formatStackAsOsrs;
    ItemCounter(BufferedImage image, int itemID, String name, int count, Plugin plugin, boolean formatStackAsOsrs) {
        super(image, plugin, count);
        this.itemID = itemID;
        this.name = name;
        this.formatStackAsOsrs = formatStackAsOsrs;
    }

    @Override
    public String getText()
    {
        return this.formatStackAsOsrs
                ? QuantityFormatter.quantityToRSDecimalStack(getCount())
                : NumberFormat.getIntegerInstance().format(getCount());
    }

    @Override
    public String getTooltip()
    {
        return name + ": " + this.getCount();
    }
}
