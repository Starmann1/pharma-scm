package pharma.gui;

import pharma.model.*;
import pharma.service.AuthService;
import pharma.service.DatabaseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Production Panel for managing manufacturing orders and executing production
 * runs.
 * Features: BOM selection, material shortage analysis, production order
 * creation, and execution.
 */
public class ProductionPanel extends JPanel {
    private DatabaseService dbService;
    private AuthService authService;
    private User currentUser;

    private JComboBox<String> productComboBox;
    private JComboBox<String> bomComboBox;
    private JTextField qtyField;
    private JButton analyzeButton;
    private JButton createOrderButton;
    private JButton executeButton;

    private JTable shortageTable;
    private DefaultTableModel shortageTableModel;

    private JTable ordersTable;
    private DefaultTableModel ordersTableModel;

    private JTextArea orderDetailsArea;

    public ProductionPanel(DatabaseService dbService, AuthService authService, User currentUser) {
        this.dbService = dbService;
        this.authService = authService;
        this.currentUser = currentUser;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        loadProductionOrders();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("Production Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.WEST);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));

        panel.add(createLeftPanel());
        panel.add(createRightPanel());

        return panel;
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Create Production Order"));

        panel.add(createOrderFormPanel(), BorderLayout.NORTH);
        panel.add(createShortagePanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createOrderFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Product selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Finished Product:"), gbc);

        gbc.gridx = 1;
        productComboBox = new JComboBox<>();
        productComboBox.addActionListener(e -> loadBOMsForProduct());
        panel.add(productComboBox, gbc);

        // BOM selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("BOM Version:"), gbc);

        gbc.gridx = 1;
        bomComboBox = new JComboBox<>();
        panel.add(bomComboBox, gbc);

        // Quantity
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Quantity:"), gbc);

        gbc.gridx = 1;
        qtyField = new JTextField("1000");
        panel.add(qtyField, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        analyzeButton = new JButton("Analyze Shortages");
        analyzeButton.addActionListener(e -> analyzeShortages());
        buttonPanel.add(analyzeButton);

        createOrderButton = new JButton("Create Order");
        createOrderButton.addActionListener(e -> createProductionOrder());
        createOrderButton.setEnabled(authService.hasPermission(currentUser, "CREATE_PRODUCTION_ORDER"));
        buttonPanel.add(createOrderButton);

        panel.add(buttonPanel, gbc);

        loadFinishedProducts();

        return panel;
    }

    private JPanel createShortagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Material Availability"));

        String[] columns = { "Material", "Required", "Available", "Shortage", "Status" };
        shortageTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        shortageTable = new JTable(shortageTableModel);
        JScrollPane scrollPane = new JScrollPane(shortageTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Production Orders"));

        panel.add(createOrdersTablePanel(), BorderLayout.CENTER);
        panel.add(createOrderDetailsPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createOrdersTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = { "Order ID", "Batch Number", "Product", "Qty", "Status", "Date" };
        ordersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ordersTable = new JTable(ordersTableModel);
        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showOrderDetails();
            }
        });

        JScrollPane scrollPane = new JScrollPane(ordersTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        executeButton = new JButton("Execute Production Run");
        executeButton.addActionListener(e -> executeProductionRun());
        executeButton.setEnabled(authService.hasPermission(currentUser, "EXECUTE_PRODUCTION_RUN"));
        buttonPanel.add(executeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createOrderDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Order Details"));

        orderDetailsArea = new JTextArea(5, 40);
        orderDetailsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(orderDetailsArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void loadFinishedProducts() {
        try {
            List<Material> materials = dbService.getMaterialsByType(Material.MaterialType.FINISHED_GOOD);
            productComboBox.removeAllItems();
            for (Material material : materials) {
                productComboBox.addItem(material.getMaterialCode() + " - " + material.getBrandName());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBOMsForProduct() {
        try {
            String selected = (String) productComboBox.getSelectedItem();
            if (selected == null)
                return;

            String materialCode = selected.split(" - ")[0];
            List<BOMHeader> boms = dbService.getActiveBOMsForMaterial(materialCode);

            bomComboBox.removeAllItems();
            for (BOMHeader bom : boms) {
                bomComboBox.addItem("Version " + bom.getVersionNumber() + " (ID: " + bom.getBomId() + ")");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading BOMs: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void analyzeShortages() {
        try {
            String bomSelection = (String) bomComboBox.getSelectedItem();
            if (bomSelection == null) {
                JOptionPane.showMessageDialog(this, "Please select a BOM", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int bomId = Integer.parseInt(bomSelection.split("ID: ")[1].replace(")", ""));
            double qty = Double.parseDouble(qtyField.getText());

            Map<String, Double> shortages = dbService.validateBOMAvailability(bomId, qty);
            List<BOMDetail> ingredients = dbService.getBOMIngredients(bomId);

            shortageTableModel.setRowCount(0);

            for (BOMDetail ingredient : ingredients) {
                double required = ingredient.getRequiredQty() * qty;
                double shortage = shortages.get(ingredient.getIngredientMaterialCode());
                double available = required - shortage;
                String status = shortage > 0 ? "SHORTAGE" : "Sufficient";

                shortageTableModel.addRow(new Object[] {
                        ingredient.getIngredientMaterialCode(),
                        String.format("%.2f %s", required, ingredient.getUom()),
                        String.format("%.2f", available),
                        String.format("%.2f", shortage),
                        status
                });
            }

            // Color code rows
            shortageTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    String status = (String) table.getValueAt(row, 4);
                    if ("SHORTAGE".equals(status)) {
                        c.setBackground(new Color(255, 200, 200));
                    } else {
                        c.setBackground(new Color(200, 255, 200));
                    }
                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                    }
                    return c;
                }
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error analyzing shortages: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createProductionOrder() {
        try {
            String bomSelection = (String) bomComboBox.getSelectedItem();
            if (bomSelection == null) {
                JOptionPane.showMessageDialog(this, "Please select a BOM", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int bomId = Integer.parseInt(bomSelection.split("ID: ")[1].replace(")", ""));
            double qty = Double.parseDouble(qtyField.getText());

            String productSelection = (String) productComboBox.getSelectedItem();
            String materialCode = productSelection.split(" - ")[0];
            String batchNumber = "BATCH-" + materialCode + "-" + System.currentTimeMillis();

            ProductionOrder order = new ProductionOrder();
            order.setBatchNumber(batchNumber);
            order.setBomId(bomId);
            order.setPlannedQty(qty);
            order.setStatus(ProductionOrder.ProductionStatus.PLANNED);
            order.setProductionDate(LocalDate.now());
            order.setCreatedBy(currentUser.getUserId());
            order.setNotes("Created via Production Panel");

            int orderId = dbService.createProductionOrder(order);

            if (orderId > 0) {
                JOptionPane.showMessageDialog(this,
                        "Production Order created successfully!\nOrder ID: " + orderId + "\nBatch: " + batchNumber,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                loadProductionOrders();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error creating order: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadProductionOrders() {
        try {
            List<ProductionOrder> orders = dbService.getAllProductionOrders();
            ordersTableModel.setRowCount(0);

            for (ProductionOrder order : orders) {
                BOMHeader bom = dbService.getBOMById(order.getBomId());
                String productName = bom != null ? bom.getMaterialCode() : "Unknown";

                ordersTableModel.addRow(new Object[] {
                        order.getOrderId(),
                        order.getBatchNumber(),
                        productName,
                        order.getPlannedQty(),
                        order.getStatus().getDisplayName(),
                        order.getProductionDate()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading orders: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showOrderDetails() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow < 0)
            return;

        try {
            int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
            ProductionOrder order = dbService.getProductionOrderById(orderId);

            if (order != null) {
                StringBuilder details = new StringBuilder();
                details.append("Order ID: ").append(order.getOrderId()).append("\n");
                details.append("Batch Number: ").append(order.getBatchNumber()).append("\n");
                details.append("BOM ID: ").append(order.getBomId()).append("\n");
                details.append("Planned Qty: ").append(order.getPlannedQty()).append("\n");
                details.append("Actual Qty: ").append(order.getActualQty() != null ? order.getActualQty() : "N/A")
                        .append("\n");
                details.append("Status: ").append(order.getStatus().getDisplayName()).append("\n");
                details.append("Production Date: ").append(order.getProductionDate()).append("\n");
                details.append("Completed Date: ")
                        .append(order.getCompletedDate() != null ? order.getCompletedDate() : "N/A").append("\n");
                details.append("Notes: ").append(order.getNotes() != null ? order.getNotes() : "").append("\n");

                List<MaterialConsumption> consumptions = dbService.getMaterialConsumptionsForOrder(orderId);
                if (!consumptions.isEmpty()) {
                    details.append("\n--- Material Consumption ---\n");
                    for (MaterialConsumption mc : consumptions) {
                        details.append(String.format("- %s (Batch: %s): %s %s\n",
                                mc.getMaterialCode(), mc.getBatchNumber(), mc.getConsumedQty(), mc.getUom()));
                    }
                }

                orderDetailsArea.setText(details.toString());
            }
        } catch (Exception e) {
            orderDetailsArea.setText("Error loading details: " + e.getMessage());
        }
    }

    private void executeProductionRun() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a production order", "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
            String status = (String) ordersTable.getValueAt(selectedRow, 4);

            if (!"Planned".equals(status)) {
                JOptionPane.showMessageDialog(this, "Only orders with 'Planned' status can be executed", "Warning",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Execute production run for Order ID: " + orderId + "?\n\nThis will:\n" +
                            "- Consume raw materials (FEFO)\n" +
                            "- Create finished goods in QUARANTINE\n" +
                            "- Update order status to Quality-Testing\n" +
                            "- Log audit trail\n\nThis action cannot be undone!",
                    "Confirm Production Run",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                dbService.executeProductionRun(orderId, currentUser.getUserId());
                JOptionPane.showMessageDialog(this, "Production run executed successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                loadProductionOrders();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error executing production run: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
