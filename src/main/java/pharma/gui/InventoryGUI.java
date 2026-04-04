package pharma.gui;

import pharma.service.DatabaseService;
import pharma.service.AuthService;
import pharma.model.User; // Required to accept the authenticated user
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
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
    private ProductionPanel productionPanel;
    private QualityDashboard qualityDashboard;

    // REMOVED DASHBOARD :private final String DASHBOARD = "Dashboard";
    private final String MATERIALS = "Materials";
    private final String SUPPLIERS = "Suppliers";
    private final String PURCHASE_ORDERS = "PurchaseOrders";
    private final String GRN = "GRN";
    private final String INVENTORY = "Inventory";
    private final String LOCATIONS = "Locations";
    private final String REPORTS = "Reports";
    private final String PRODUCTION = "Production";
    private final String QUALITY = "Quality";
    private final String ADMIN = "Admin (RBAC)";

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
        cardLayout.show(mainContentPanel, MATERIALS);
        pack();
        setLocationRelativeTo(null);
    }

    // EDITED: Overloaded constructor for backwards compatibility (optional, but
    // safer)
    public InventoryGUI(DatabaseService dbService) {
        // Fallback or initialization for systems without explicit user login setup
        this(dbService,
                new User(0, "Guest", "Guest User", new pharma.model.Role(0, "Guest", "Guest User"),
                        new java.util.HashSet<>()));
    }

    private void createSideNavigation() {
        sideNavPanel = new JPanel();
        sideNavPanel.setLayout(new BoxLayout(sideNavPanel, BoxLayout.Y_AXIS));
        // Use a fixed width but dynamic height (getHeight() needs to be called after
        // frame is visible)
        sideNavPanel.setPreferredSize(new Dimension(200, 700));
        sideNavPanel.setBackground(new Color(30, 30, 30));

        // EDITED: Displaying user information in the navigation header
        String displayUser = (activeUser.getFullName() != null && !activeUser.getFullName().isEmpty())
                ? activeUser.getFullName()
                : activeUser.getUsername();
        String userInfoHtml = String.format(
                "<html><div style='text-align: center;'><b>Pharma IMS</b><br><span style='font-size:10px;'>User: %s</span><br><span style='font-size:10px;'>Role: %s</span></div></html>",
                displayUser, activeUser.getRole().getRoleName());

        JLabel logo = new JLabel(userInfoHtml, SwingConstants.CENTER);
        logo.setForeground(new Color(153, 204, 255));
        logo.setFont(new Font("Arial", Font.BOLD, 18));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(20, 10, 30, 10));
        sideNavPanel.add(logo);

        if (authService.hasPermission(activeUser, "VIEW_DRUG")) {
            sideNavPanel.add(createNavButton(MATERIALS));
        }
        if (authService.hasPermission(activeUser, "VIEW_INVENTORY")) {
            sideNavPanel.add(createNavButton(INVENTORY));
        }
        if (authService.hasPermission(activeUser, "MANAGE_LOCATIONS")) {
            sideNavPanel.add(createNavButton(LOCATIONS));
        }
        if (authService.hasPermission(activeUser, "VIEW_SUPPLIERS")) {
            sideNavPanel.add(createNavButton(SUPPLIERS));
        }
        if (authService.hasPermission(activeUser, "VIEW_PO")) {
            sideNavPanel.add(createNavButton(PURCHASE_ORDERS));
        }
        if (authService.hasPermission(activeUser, "RECEIVE_PO")) {
            sideNavPanel.add(createNavButton(GRN));
        }
        if (authService.hasPermission(activeUser, "CREATE_PRODUCTION_ORDER")
                || authService.hasPermission(activeUser, "VIEW_BOM")) {
            sideNavPanel.add(createNavButton(PRODUCTION));
        }
        if (authService.hasPermission(activeUser, "UPDATE_QC_STATUS")
                || authService.hasPermission(activeUser, "VIEW_BATCH_TRACEABILITY")) {
            sideNavPanel.add(createNavButton(QUALITY));
        }
        if (authService.hasPermission(activeUser, "VIEW_REPORTS")) {
            sideNavPanel.add(createNavButton(REPORTS));
        }
        if (authService.hasPermission(activeUser, "MANAGE_USERS")) {
            sideNavPanel.add(createNavButton(ADMIN));
        }

        sideNavPanel.add(Box.createVerticalGlue());

        JToggleButton darkModeToggle = new JToggleButton("🌓 Dark Mode");
        darkModeToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
        darkModeToggle.setMaximumSize(new Dimension(180, 40));
        darkModeToggle.setForeground(Color.WHITE);
        darkModeToggle.setBackground(new Color(50, 50, 50));
        darkModeToggle.setFocusPainted(false);
        darkModeToggle.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));
        darkModeToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        darkModeToggle.addActionListener(e -> toggleDarkMode(darkModeToggle.isSelected()));
        sideNavPanel.add(darkModeToggle);

        sideNavPanel.add(Box.createVerticalStrut(20));
    }

    private void toggleDarkMode(boolean isDark) {
        if (isDark) {
            UIManager.put("Panel.background", new ColorUIResource(45, 45, 45));
            UIManager.put("Label.foreground", new ColorUIResource(Color.WHITE));
            UIManager.put("Table.background", new ColorUIResource(60, 60, 60));
            UIManager.put("Table.foreground", new ColorUIResource(Color.WHITE));
            UIManager.put("TableHeader.background", new ColorUIResource(45, 45, 45));
            UIManager.put("TableHeader.foreground", new ColorUIResource(Color.WHITE));
            UIManager.put("ScrollPane.background", new ColorUIResource(45, 45, 45));
            UIManager.put("Viewport.background", new ColorUIResource(45, 45, 45));
            UIManager.put("TextField.background", new ColorUIResource(60, 60, 60));
            UIManager.put("TextField.foreground", new ColorUIResource(Color.WHITE));
            UIManager.put("ComboBox.background", new ColorUIResource(45, 45, 45));
            UIManager.put("ComboBox.foreground", new ColorUIResource(Color.WHITE));
            UIManager.put("CheckBox.background", new ColorUIResource(45, 45, 45));
            UIManager.put("CheckBox.foreground", new ColorUIResource(Color.WHITE));
            UIManager.put("TitledBorder.titleColor", new ColorUIResource(Color.WHITE));
            UIManager.put("Button.background", new ColorUIResource(60, 60, 60));
            UIManager.put("Button.foreground", new ColorUIResource(Color.BLACK));
            UIManager.put("OptionPane.background", new ColorUIResource(45, 45, 45));
            UIManager.put("OptionPane.messageForeground", new ColorUIResource(Color.WHITE));
        } else {
            UIManager.put("Panel.background", null);
            UIManager.put("Label.foreground", null);
            UIManager.put("Table.background", null);
            UIManager.put("Table.foreground", null);
            UIManager.put("TableHeader.background", null);
            UIManager.put("TableHeader.foreground", null);
            UIManager.put("ScrollPane.background", null);
            UIManager.put("Viewport.background", null);
            UIManager.put("TextField.background", null);
            UIManager.put("TextField.foreground", null);
            UIManager.put("ComboBox.background", null);
            UIManager.put("ComboBox.foreground", null);
            UIManager.put("CheckBox.background", null);
            UIManager.put("CheckBox.foreground", null);
            UIManager.put("TitledBorder.titleColor", null);
            UIManager.put("Button.background", null);
            UIManager.put("Button.foreground", null);
            UIManager.put("OptionPane.background", null);
            UIManager.put("OptionPane.messageForeground", null);
        }

        try {
            // Re-apply the current look and feel defaults to clear out the UIResources
            // properly
            // if switching to light mode, and to update components if switching to dark
            // mode
            String currentLaf = UIManager.getLookAndFeel().getClass().getName();
            UIManager.setLookAndFeel(currentLaf);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.updateComponentTreeUI(this);
        sideNavPanel.setBackground(new Color(30, 30, 30));
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

        button.addActionListener(e -> {
            cardLayout.show(mainContentPanel, text);
            refreshPanelForNavigation(text);
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

        // MaterialsPanel: Assumed signature: MaterialsPanel(JFrame, DatabaseService)
        // This fix also addresses the form opening for Problem 4.
        mainContentPanel.add(new MaterialsPanel(null, dbService), MATERIALS);

        // InventoryPanel: Assumed signature: InventoryPanel(DatabaseService)
        mainContentPanel.add(new InventoryPanel(dbService), INVENTORY);

        // FIX for LocationsPanel (Problem 2): Corrected signature is (DatabaseService,
        // JFrame)
        mainContentPanel.add(new LocationsPanel(this, dbService), LOCATIONS);

        // SuppliersPanel: now receives the active user for supplier approval audit logging.
        mainContentPanel.add(new SuppliersPanel(dbService, activeUser), SUPPLIERS);

        // FIX for PurchaseOrderPanel (Problem 1): Corrected signature is (JFrame,
        // DatabaseService)
        mainContentPanel.add(new PurchaseOrderPanel(this, dbService, authService, activeUser), PURCHASE_ORDERS);

        // FIX for GRNPanel (Problem 3): Corrected signature is (JFrame,
        // DatabaseService)
        mainContentPanel.add(new GRNPanel(this, dbService), GRN);

        // Manufacturing panels - Now using actual implementations
        productionPanel = new ProductionPanel(dbService, authService, activeUser);
        qualityDashboard = new QualityDashboard(dbService, authService, activeUser);
        mainContentPanel.add(productionPanel, PRODUCTION);
        mainContentPanel.add(qualityDashboard, QUALITY);

        // ReportsPanel: Assumed signature: ReportsPanel(DatabaseService)
        mainContentPanel.add(new ReportsPanel(dbService), REPORTS);

        if (authService.hasPermission(activeUser, "MANAGE_USERS")) {
            mainContentPanel.add(new AdminRBACPanel(new pharma.service.RoleService(dbService), activeUser), ADMIN);
        }
    }

    private void refreshPanelForNavigation(String panelName) {
        if (PRODUCTION.equals(panelName) && productionPanel != null) {
            productionPanel.refreshData();
        } else if (QUALITY.equals(panelName) && qualityDashboard != null) {
            qualityDashboard.refreshData();
        }
    }

}
