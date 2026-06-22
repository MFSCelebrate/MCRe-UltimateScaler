package me.inf32768.ultimate_scaler.util;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

/**
 * 文本事件工厂，直接使用标准 API 构造事件对象。
 */
public class TextEventFactory {
    /**
     * 创建复制到剪贴板的点击事件。
     */
    public static ClickEvent createCopyEvent(String text) {
        // 1.21.5+ 统一使用 ClickEvent.CopyToClipboard
        return new ClickEvent.CopyToClipboard(text);
    }

    /**
     * 创建显示文本的悬停事件。
     */
    public static HoverEvent createShowTextEvent(Text text) {
        // 1.21.5+ 统一使用 HoverEvent.ShowText
        return new HoverEvent.ShowText(text);
    }
}
