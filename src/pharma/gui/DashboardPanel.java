/*package pharma.gui;

import javax.swing.*;

import pharma.service.DatabaseService;

import java.awt.*;

public class DashboardPanel extends JPanel {
    private DatabaseService dbService;
    public DashboardPanel(DatabaseService dbService) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // --- Header ---
        JLabel headerLabel = new JLabel("Pharmacy Dashboard", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(headerLabel, BorderLayout.NORTH);

        // --- Main Content (Summary Cards) ---
        JPanel summaryPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        
        // Card 1: Total Drugs/Stock
        summaryPanel.add(createSummaryCard("Total Inventory Items", "1540", "items in stock", Color.CYAN.darker()));
        
        // Card 2: Low Stock Alerts
        summaryPanel.add(createSummaryCard("Low Stock Alerts", "12", "drugs need reorder", Color.ORANGE));
        
        // Card 3: Pending Purchase Orders
        summaryPanel.add(createSummaryCard("Pending Orders", "5", "pending shipment", Color.MAGENTA.darker()));
        
        // Card 4: Expired Items (Simulated)
        summaryPanel.add(createSummaryCard("Expired Items", "2", "items expired (last week)", Color.RED.darker()));

        add(summaryPanel, BorderLayout.CENTER);
    }

    private JPanel createSummaryCard(String title, String count, String subtitle, Color color) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(color.darker());
        card.add(titleLabel, BorderLayout.NORTH);

        JLabel countLabel = new JLabel(count, SwingConstants.CENTER);
        countLabel.setFont(new Font("Arial", Font.BOLD, 48));
        countLabel.setForeground(color);
        card.add(countLabel, BorderLayout.CENTER);

        JLabel subtitleLabel = new JLabel(subtitle, SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        card.add(subtitleLabel, BorderLayout.SOUTH);

        return card;
    }
}*/

package pharma.gui;

import pharma.service.DatabaseService;
import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {

    // FIX: Constructor now accepts DatabaseService
    public DashboardPanel(DatabaseService dbService) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // --- Header ---
        JLabel headerLabel = new JLabel("Pharmacy Dashboard", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(headerLabel, BorderLayout.NORTH);

        // --- Main Content (Summary Cards - Placeholder values) ---
        JPanel summaryPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        
        // These calls would eventually use dbService.getMetrics()
        summaryPanel.add(createSummaryCard("Total Inventory Items", "1540", "items in stock", Color.CYAN.darker()));
        summaryPanel.add(createSummaryCard("Low Stock Alerts", "12", "drugs need reorder", Color.ORANGE));
        summaryPanel.add(createSummaryCard("Pending Orders", "5", "pending shipment", Color.MAGENTA.darker()));
        summaryPanel.add(createSummaryCard("Expired Items", "2", "items expired (last week)", Color.RED.darker()));

        add(summaryPanel, BorderLayout.CENTER);
    }

    private JPanel createSummaryCard(String title, String count, String subtitle, Color color) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(color.darker());
        card.add(titleLabel, BorderLayout.NORTH);

        JLabel countLabel = new JLabel(count, SwingConstants.CENTER);
        countLabel.setFont(new Font("Arial", Font.BOLD, 48));
        countLabel.setForeground(color);
        card.add(countLabel, BorderLayout.CENTER);

        JLabel subtitleLabel = new JLabel(subtitle, SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        card.add(subtitleLabel, BorderLayout.SOUTH);

        return card;
    }
}
