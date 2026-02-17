package br.tblack.plugin.enums;

public enum HudPositionPreset {
    TOP_LEFT("anchor-width: 250; anchor-height: 270; anchor-left: 10; anchor-top: 20;"),
    TOP_RIGHT("anchor-width: 250; anchor-height: 270; anchor-right: 10; anchor-top: 20;"),
    CENTER_LEFT("anchor-width: 250; anchor-height: 270; anchor-left: 10; anchor-top: 50%;"),
    CENTER_RIGHT("anchor-width: 250; anchor-height: 270; anchor-right: 10; anchor-top: 50%;"),
    BOTTOM_LEFT("anchor-width: 250; anchor-height: 270; anchor-left: 10; anchor-bottom: 20;"),
    BOTTOM_RIGHT("anchor-width: 250; anchor-height: 270; anchor-right: 10; anchor-bottom: 20;");

    private final String style;

    HudPositionPreset(String style) {
        this.style = style;
    }

    public String getStyle() {
        return style;
    }
}
