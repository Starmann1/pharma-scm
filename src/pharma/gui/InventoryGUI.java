/*
package pharma.gui;

import pharma.service.DatabaseService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class InventoryGUI extends JFrame {
    private JPanel mainContentPanel;
    private CardLayout cardLayout;
    private JPanel sideNavPanel;
    private DatabaseService dbService;

    private final String DASHBOARD = "Dashboard";
    private final String DRUGS = "Drugs";
    private final String SUPPLIERS = "Suppliers";
    private final String PURCHASE_ORDERS = "PurchaseOrders";
    private final String GRN = "GRN";
    private final String INVENTORY = "Inventory";
    private final String LOCATIONS = "Locations";
    private final String REPORTS = "Reports";

    public InventoryGUI(DatabaseService dbService) {
        this.dbService = dbService;
        setTitle("Pharma IMS - Inventory Management System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dbService.disconnect();
                dispose();
            }
        });
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);

        createSideNavigation();
        add(sideNavPanel, BorderLayout.WEST);

        addContentPanels();
        add(mainContentPanel, BorderLayout.CENTER);

        cardLayout.show(mainContentPanel, DASHBOARD);
        pack();
        setLocationRelativeTo(null);
    }

    private void createSideNavigation() {
        sideNavPanel = new JPanel();
        sideNavPanel.setLayout(new BoxLayout(sideNavPanel, BoxLayout.Y_AXIS));
        sideNavPanel.setPreferredSize(new Dimension(200, getHeight()));
        sideNavPanel.setBackground(new Color(30, 30, 30));

        JLabel logo = new JLabel("Pharma IMS", SwingConstants.CENTER);
        logo.setForeground(new Color(153, 204, 255));
        logo.setFont(new Font("Arial", Font.BOLD, 24));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(20, 10, 30, 10));
        sideNavPanel.add(logo);

        String[] menuItems = {DASHBOARD, DRUGS, SUPPLIERS, PURCHASE_ORDERS, GRN, INVENTORY, LOCATIONS, REPORTS};
        for (String item : menuItems) {
            JButton button = createNavButton(item);
            sideNavPanel.add(button);
        }

        sideNavPanel.add(Box.createVerticalGlue());
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(180, 40));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(30, 30, 30)); 
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));

        button.addActionListener(_ -> {
            cardLayout.show(mainContentPanel, text);
            System.out.println("Switched to: " + text); 
        });

        return button;
    }

    private void addContentPanels() {
        mainContentPanel.add(new SuppliersPanel(dbService), SUPPLIERS);
        mainContentPanel.add(createPlaceholderPanel(DASHBOARD), DASHBOARD);
        mainContentPanel.add(createPlaceholderPanel(DRUGS + " - Drug Master"), DRUGS);
        mainContentPanel.add(createPlaceholderPanel(PURCHASE_ORDERS + " - PO Management"), PURCHASE_ORDERS);
        mainContentPanel.add(createPlaceholderPanel(GRN + " - Goods Receipt Notes"), GRN);
        mainContentPanel.add(createPlaceholderPanel(INVENTORY + " - Real-time Stock"), INVENTORY);
        mainContentPanel.add(createPlaceholderPanel(LOCATIONS + " - Location Master"), LOCATIONS);
        mainContentPanel.add(createPlaceholderPanel(REPORTS + " - Analytics"), REPORTS);
    }

    private JPanel createPlaceholderPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel label = new JLabel("Module: " + title, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 36));
        label.setForeground(new Color(50, 50, 50));
        panel.add(label, BorderLayout.CENTER);
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel headerTitle = new JLabel(title);
        headerTitle.setFont(new Font("Arial", Font.BOLD, 28));
        header.add(headerTitle);
        header.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(header, BorderLayout.NORTH);

        return panel;
    }
}
*/
package pharma.gui;

import pharma.service.DatabaseService;
import pharma.service.AuthService;
import pharma.model.User; // Required to accept the authenticated user
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class InventoryGUI extends JFrame {
    private JPanel mainContentPanel;
    private CardLayout cardLayout;
    private JPanel sideNavPanel;
    private DatabaseService dbService;
    private AuthService authService;
    private User activeUser; // Field to hold the logged-in user

    // REMOVED DASHBOARD :private final String DASHBOARD = "Dashboard";
    private final String DRUGS = "Drugs";
    private final String SUPPLIERS = "Suppliers";
    private final String PURCHASE_ORDERS = "PurchaseOrders";
    private final String GRN = "GRN";
    private final String INVENTORY = "Inventory";
    private final String LOCATIONS = "Locations";
    private final String REPORTS = "Reports";
    private final String PRODUCTION = "Production";
    private final String QUALITY = "Quality";

    // EDITED: New constructor signature now accepts the DatabaseService AND the
    // authenticated User object.
    public InventoryGUI(DatabaseService dbService, User user) {
        this.dbService = dbService;
        this.authService = new AuthService(dbService);
        this.activeUser = user; // Store the authenticated user

        // EDITED: Title updated to display the logged-in user
        setTitle(
                "Pharma IMS - Inventory Management System | User: " + user.getUsername() + " (" + user.getRole() + ")");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Custom Window Listener to handle database disconnection on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Since connections are per-operation, this primarily cleans up the main app
                // instance
                if (dbService != null) {
                    dbService.disconnect();
                }
                dispose();
            }
        });

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);

        createSideNavigation();
        add(sideNavPanel, BorderLayout.WEST);

        // EDITED: Replaced placeholder calls with actual panel instances, passing
        // dbService
        addContentPanels();

        add(mainContentPanel, BorderLayout.CENTER);

        // Show the Dashboard upon startup
        cardLayout.show(mainContentPanel, DRUGS);
        pack();
        setLocationRelativeTo(null);
    }

    // EDITED: Overloaded constructor for backwards compatibility (optional, but
    // safer)
    public InventoryGUI(DatabaseService dbService) {
        // Fallback or initialization for systems without explicit user login setup
        this(dbService, new User(0, "Guest", "N/A"));
    }

    private void createSideNavigation() {
        sideNavPanel = new JPanel();
        sideNavPanel.setLayout(new BoxLayout(sideNavPanel, BoxLayout.Y_AXIS));
        // Use a fixed width but dynamic height (getHeight() needs to be called after
        // frame is visible)
        sideNavPanel.setPreferredSize(new Dimension(200, 700));
        sideNavPanel.setBackground(new Color(30, 30, 30));

        // EDITED: Displaying user information in the navigation header
        String userInfoHtml = String.format(
                "<html><div style='text-align: center;'><b>Pharma IMS</b><br><span style='font-size:10px;'>User: %s</span><br><span style='font-size:10px;'>Role: %s</span></div></html>",
                activeUser.getUsername(), activeUser.getRole());

        JLabel logo = new JLabel(userInfoHtml, SwingConstants.CENTER);
        logo.setForeground(new Color(153, 204, 255));
        logo.setFont(new Font("Arial", Font.BOLD, 18));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(20, 10, 30, 10));
        sideNavPanel.add(logo);

        String[] menuItems = { DRUGS, INVENTORY, LOCATIONS, SUPPLIERS, PURCHASE_ORDERS, GRN, PRODUCTION, QUALITY,
                REPORTS };
        for (String item : menuItems) {
            JButton button = createNavButton(item);
            sideNavPanel.add(button);
        }

        sideNavPanel.add(Box.createVerticalGlue());
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(180, 40));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(30, 30, 30));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));

        button.addActionListener(_ -> {
            cardLayout.show(mainContentPanel, text);
            System.out.println("Switched to: " + text);
        });

        return button;
    }

    /**
     * Instantiates all the content panels and adds them to the CardLayout.
     * EDITED: Now uses the actual panel classes instead of placeholders.
     */
    private void addContentPanels() {
        // Note: The constructor parameter order must exactly match the definition in
        // each Panel.

        // DrugsPanel: Assumed signature: DrugsPanel(JFrame, DatabaseService)
        // This fix also addresses the form opening for Problem 4.
        mainContentPanel.add(new DrugsPanel(null, dbService), DRUGS);

        // InventoryPanel: Assumed signature: InventoryPanel(DatabaseService)
        mainContentPanel.add(new InventoryPanel(dbService), INVENTORY);

        // FIX for LocationsPanel (Problem 2): Corrected signature is (DatabaseService,
        // JFrame)
        mainContentPanel.add(new LocationsPanel(this, dbService), LOCATIONS);

        // SuppliersPanel: Assumed signature: SuppliersPanel(DatabaseService)
        mainContentPanel.add(new SuppliersPanel(dbService), SUPPLIERS);

        // FIX for PurchaseOrderPanel (Problem 1): Corrected signature is (JFrame,
        // DatabaseService)
        mainContentPanel.add(new PurchaseOrderPanel(this, dbService), PURCHASE_ORDERS);

        // FIX for GRNPanel (Problem 3): Corrected signature is (JFrame,
        // DatabaseService)
        mainContentPanel.add(new GRNPanel(this, dbService), GRN);

        // Manufacturing panels - Now using actual implementations
        mainContentPanel.add(new ProductionPanel(dbService, authService, activeUser), PRODUCTION);
        mainContentPanel.add(new QualityDashboard(dbService, authService, activeUser), QUALITY);

        // ReportsPanel: Assumed signature: ReportsPanel(DatabaseService)
        mainContentPanel.add(new ReportsPanel(dbService), REPORTS);
    }

    private JPanel createPlaceholderPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("Module: " + title, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 36));
        label.setForeground(new Color(50, 50, 50));
        panel.add(label, BorderLayout.CENTER);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel headerTitle = new JLabel(title);
        headerTitle.setFont(new Font("Arial", Font.BOLD, 28));
        header.add(headerTitle);
        header.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(header, BorderLayout.NORTH);

        // Add informational message
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        JLabel infoLabel = new JLabel("<html><div style='text-align: center;'>" +
                "<p style='font-size: 14px; color: #666;'>This module is part of the Manufacturing ERP features.</p>" +
                "<p style='font-size: 12px; color: #999;'>Complete DatabaseService integration to enable this panel.</p>"
                +
                "<p style='font-size: 12px; color: #999;'>See QUICK_START_GUIDE.md for instructions.</p>" +
                "</div></html>");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoPanel.add(infoLabel);
        panel.add(infoPanel, BorderLayout.SOUTH);

        return panel;
    }
}
