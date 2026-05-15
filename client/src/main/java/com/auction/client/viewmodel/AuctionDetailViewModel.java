package com.auction.client.viewmodel;

import java.util.ArrayList;
import java.util.List;

import com.auction.client.model.AuctionItem;

/**
 * ViewModel for AuctionDetail screen.
 * Holds the currently displayed item and prepares chart/meta data for the controller.
 */
public class AuctionDetailViewModel {

    private AuctionItem currentItem;

    // ── Bind item ─────────────────────────────────────────────
    public void setItem(AuctionItem item) {
        this.currentItem = item;
    }

    public AuctionItem getItem() {
        return currentItem;
    }

    // ── Derived display values ────────────────────────────────
    public String getDisplayPrice() {
        if (currentItem == null) return "$0";
        return String.format("$%,.0f", currentItem.getCurrentPrice());
    }

    public String getDisplayTimeRemaining() {
        if (currentItem == null) return "00:00:00";
        return formatTime(currentItem.secondsLeft());
    }

    public String getDisplayTitle() {
        return currentItem != null ? currentItem.getItemName() : "";
    }

    public String getDisplaySubtitle() {
        return currentItem != null && currentItem.getDescription() != null
                ? currentItem.getDescription() : "";
    }

    public boolean isEndingSoon() {
        return currentItem != null && currentItem.isEndingSoon();
    }

    // ── Chart data ────────────────────────────────────────────
    /**
     * Returns demo price history points (label → price).
     * Replace with real server data when available.
     */
    public List<ChartPoint> getPriceHistory() {
        List<ChartPoint> points = new ArrayList<>();
        if (currentItem == null) return points;

        double base = currentItem.getCurrentPrice();
        points.add(new ChartPoint("10:00", base * 0.985));
        points.add(new ChartPoint("11:00", base * 0.990));
        points.add(new ChartPoint("12:00", base * 0.995));
        points.add(new ChartPoint("13:00", base * 0.998));
        points.add(new ChartPoint("14:00", base));
        return points;
    }

    // ── Helper ────────────────────────────────────────────────
    private String formatTime(int seconds) {
        if (seconds <= 0) return "00:00:00";
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /** Simple data holder for chart points. */
    public static class ChartPoint {
        public final String label;
        public final double price;
        public ChartPoint(String label, double price) {
            this.label = label;
            this.price = price;
        }
    }
}
