package pharma.gui;

import pharma.model.PurchaseOrder;
import pharma.model.PurchaseOrder.PurchaseOrderItem;
import pharma.model.Supplier;
import pharma.service.DatabaseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
//import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreatePurchaseOrderDialog extends JDialog {

    private final DatabaseService dbService = DatabaseService.getInstance();
    private final PurchaseOrderPanel parentPanel;

    // Header Fields
    private JTextField poNumberField;
    private JComboBox<String> supplierComboBox;
    private JTextField expectedDateField;
    private JTextField totalAmountField;

    // Line Item Fields
    private DefaultTableModel itemTableModel;
    private JTable itemTable;
    private JComboBox<String> itemDrugComboBox; // Changed from text field to dropdown
    private JTextField itemQuantityField;
    private JTextField itemPriceField;
    private JTextField drugSearchField; // Search field for filtering materials

    // Utility Maps
    private Map<String, Integer> supplierNameToIdMap;
    private Map<String, String> drugDisplayToCodeMap; // Maps display text to material code
    private List<String> allDrugDisplayNames; // Sorted list of all material display names

    public CreatePurchaseOrderDialog(JFrame owner, PurchaseOrderPanel parent) throws ClassNotFoundException {
        super(owner, "Create New Purchase Order", true);
        this.parentPanel = parent;
        loadSupplierData();
        loadDrugData(); // Load materials for dropdown
        initComponents();
        pack();
        setLocationRelativeTo(owner);
    }

    private void loadSupplierData() {
        try {
            List<Supplier> suppliers = dbService.getAllSuppliers();
            supplierNameToIdMap = suppliers.stream()
                    .collect(Collectors.toMap(Supplier::getSupplierName, Supplier::getSupplierId));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading supplier data: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            supplierNameToIdMap = Map.of();
        }
    }

    private void loadDrugData() {
        try {
            var materials = dbService.getDrugs();
            drugDisplayToCodeMap = materials.stream()
                    .collect(Collectors.toMap(
                            material -> material.getMaterialCode() + " - " + material.getBrandName(),
                            material -> material.getMaterialCode()));

            // Create sorted list by Material ID (material code)
            allDrugDisplayNames = drugDisplayToCodeMap.keySet().stream()
                    .sorted() // Sorts alphabetically by material code (DRG001, DRG002, etc.)
                    .collect(Collectors.toList());

            System.out.println(
                    "DEBUG: Loaded " + drugDisplayToCodeMap.size() + " materials for dropdown (sorted by Material ID)");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading material data: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            drugDisplayToCodeMap = Map.of();
            allDrugDisplayNames = new ArrayList<>();
        }
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- 1. Header Panel (PO Details) ---
        JPanel headerPanel = new JPanel(new GridLayout(4, 2, 10, 5));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Order Details"));

        // PO Number
        headerPanel.add(new JLabel("PO Number:"));
        poNumberField = new JTextField(dbService.generateNextPoNumber());
        poNumberField.setEditable(false);
        poNumberField.setBackground(new Color(45, 45, 45));
        poNumberField.setForeground(Color.WHITE);
        headerPanel.add(poNumberField);

        // Supplier Dropdown
        headerPanel.add(new JLabel("Select Supplier:"));
        supplierComboBox = new JComboBox<>(supplierNameToIdMap.keySet().toArray(new String[0]));
        headerPanel.add(supplierComboBox);

        // Expected Date
        headerPanel.add(new JLabel("Expected Date (YYYY-MM-DD):"));
        expectedDateField = new JTextField(LocalDate.now().plusWeeks(1).toString());
        headerPanel.add(expectedDateField);

        // Total Amount
        headerPanel.add(new JLabel("Total Amount:"));
        totalAmountField = new JTextField("0.00");
        totalAmountField.setEditable(false);
        totalAmountField.setBackground(new Color(45, 45, 45));
        totalAmountField.setForeground(Color.WHITE);
        headerPanel.add(totalAmountField);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- 2. Line Item Panel (Table and Add Form) ---
        JPanel itemPanel = buildLineItemPanel();
        mainPanel.add(itemPanel, BorderLayout.CENTER);

        // --- 3. Footer/Action Panel ---
        JButton saveButton = new JButton("CREATE");
        saveButton.addActionListener(this::savePoAction);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("CANCEL");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel buildLineItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Line Items"));

        // Table Setup
        String[] columnNames = { "Material ID", "Quantity", "Unit Price" };
        itemTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 1 ? Integer.class : (columnIndex == 2 ? Double.class : String.class);
            }
        };
        itemTable = new JTable(itemTableModel);

        // Search and Input Fields Panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));

        // Search field at the top
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("🔍 Search Material:"));
        drugSearchField = new JTextField(20);
        drugSearchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                filterDrugDropdown(drugSearchField.getText());
            }
        });
        searchPanel.add(drugSearchField);
        JButton clearSearchBtn = new JButton("Clear");
        clearSearchBtn.addActionListener(e -> {
            drugSearchField.setText("");
            filterDrugDropdown("");
        });
        searchPanel.add(clearSearchBtn);
        inputPanel.add(searchPanel, BorderLayout.NORTH);

        // Input Fields for adding items
        JPanel inputFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        inputFields.add(new JLabel("Select Material:"));
        // Create dropdown with sorted materials
        itemDrugComboBox = new JComboBox<>(allDrugDisplayNames.toArray(new String[0]));
        itemDrugComboBox.setPreferredSize(new Dimension(250, 25));
        inputFields.add(itemDrugComboBox);
        inputFields.add(new JLabel("Qty:"));
        itemQuantityField = new JTextField(5);
        inputFields.add(itemQuantityField);
        inputFields.add(new JLabel("Price:"));
        itemPriceField = new JTextField(7);
        inputFields.add(itemPriceField);
        JButton addItemButton = new JButton("Add Item");
        addItemButton.addActionListener(this::addItemAction);
        inputFields.add(addItemButton);

        inputPanel.add(inputFields, BorderLayout.SOUTH);

        panel.add(new JScrollPane(itemTable), BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void addItemAction(ActionEvent e) {
        try {
            String selectedDisplay = (String) itemDrugComboBox.getSelectedItem();
            if (selectedDisplay == null || selectedDisplay.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a material.", "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get the actual material code from the display text
            String drugId = drugDisplayToCodeMap.get(selectedDisplay);
            int quantity = Integer.parseInt(itemQuantityField.getText().trim());
            double unitPrice = Double.parseDouble(itemPriceField.getText().trim());

            if (quantity <= 0 || unitPrice <= 0) {
                throw new IllegalArgumentException("Quantity and Price must be positive values.");
            }

            // Check if material already exists in the table
            for (int i = 0; i < itemTableModel.getRowCount(); i++) {
                if (itemTableModel.getValueAt(i, 0).equals(drugId)) {
                    JOptionPane.showMessageDialog(this, "Material already added. Remove it first to change quantity/price.",
                            "Duplicate Item", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            itemTableModel.addRow(new Object[] { drugId, quantity, unitPrice });
            recalculateTotal();
            // Reset to first item in dropdown
            itemDrugComboBox.setSelectedIndex(0);
            itemQuantityField.setText("");
            itemPriceField.setText("");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity and Price must be valid numbers.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalculateTotal() {
        double total = 0.0;
        for (int i = 0; i < itemTableModel.getRowCount(); i++) {
            int quantity = (int) itemTableModel.getValueAt(i, 1);
            double price = (double) itemTableModel.getValueAt(i, 2);
            total += (quantity * price);
        }
        totalAmountField.setText(String.format("%.2f", total));
    }

    /**
     * Filters the material dropdown based on search text
     * Searches in both Material ID and Brand Name (case-insensitive)
     */
    private void filterDrugDropdown(String searchText) {
        itemDrugComboBox.removeAllItems();

        if (searchText == null || searchText.trim().isEmpty()) {
            // Show all materials (sorted)
            for (String drugDisplay : allDrugDisplayNames) {
                itemDrugComboBox.addItem(drugDisplay);
            }
        } else {
            // Filter materials that match the search text
            String searchLower = searchText.toLowerCase().trim();
            List<String> filteredDrugs = allDrugDisplayNames.stream()
                    .filter(material -> material.toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());

            for (String drugDisplay : filteredDrugs) {
                itemDrugComboBox.addItem(drugDisplay);
            }

            // Show dropdown if there are matches
            if (!filteredDrugs.isEmpty() && itemDrugComboBox.getItemCount() > 0) {
                itemDrugComboBox.setPopupVisible(true);
            }
        }
    }

    /**
     * Handles the creation of the Purchase Order, passing the corrected model
     * object.
     */
    private void savePoAction(ActionEvent e) {
        String selectedSupplierName = (String) supplierComboBox.getSelectedItem();
        String expectedDateStr = expectedDateField.getText().trim();
        double finalTotalAmount;

        if (selectedSupplierName == null || selectedSupplierName.isEmpty() || itemTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please select a supplier and add at least one line item.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 1. Validate and Parse Header Data
        LocalDate expectedDate;
        try {
            expectedDate = LocalDate.parse(expectedDateStr);
            finalTotalAmount = Double.parseDouble(totalAmountField.getText().replace(',', '.'));
            if (finalTotalAmount <= 0) {
                JOptionPane.showMessageDialog(this, "Total amount must be greater than zero.", "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Total amount calculation error. Recalculate line items.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer supplierId = supplierNameToIdMap.get(selectedSupplierName);
        if (supplierId == null) {
            return;
        }

        // 2. Build the List of PurchaseOrderItems
        List<PurchaseOrderItem> poItems = new ArrayList<>();
        for (int i = 0; i < itemTableModel.getRowCount(); i++) {

            String materialCode = (String) itemTableModel.getValueAt(i, 0);
            int quantity = (int) itemTableModel.getValueAt(i, 1);
            double unitPrice = (double) itemTableModel.getValueAt(i, 2);

            // 💡 CRITICAL FIX: Pass the String materialCode to the updated constructor
            poItems.add(new PurchaseOrderItem(materialCode, quantity, unitPrice));
        }

        // 3. Create the final PurchaseOrder object
        PurchaseOrder newPo = new PurchaseOrder(
                supplierId,
                selectedSupplierName,
                LocalDate.now(), // Order date is today
                expectedDate,
                finalTotalAmount,
                "Pending",
                poItems);

        // 4. Call the fixed DatabaseService method
        try {
            boolean success = dbService.createPurchaseOrder(newPo);

            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Purchase Order (" + newPo.getPoNumber() + ") created successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                if (parentPanel != null) {
                    parentPanel.refreshPurchaseOrderList(); // Assuming PurchaseOrderPanel has this method
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to create Purchase Order in database. Check server logs for constraint violations.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Database Driver Error: " + ex.getMessage(), "Configuration Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private boolean isPoSaved = false;

    public boolean isSaved() {
        return isPoSaved;
    }

}
