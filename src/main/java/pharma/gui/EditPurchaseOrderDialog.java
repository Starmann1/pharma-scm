/*package pharma.gui;

import pharma.model.PurchaseOrder;
import pharma.model.PurchaseOrder.PurchaseOrderItem;
import pharma.model.Supplier;
import pharma.service.DatabaseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EditPurchaseOrderDialog extends JDialog {
    private final DatabaseService dbService = DatabaseService.getInstance();
    private final PurchaseOrderPanel parentPanel;
    private final int orderId;

    // Header fields and table model
    private JTextField poNumberField, expectedDateField, totalAmountField;
    private JComboBox<String> supplierComboBox;
    private DefaultTableModel itemTableModel;
    private JTable itemTable;
    private JTextField itemDrugIdField, itemQuantityField, itemPriceField;
    private Map<String, Integer> supplierNameToIdMap;

    public EditPurchaseOrderDialog(JFrame owner, PurchaseOrderPanel parentPanel, int orderId) {
        super(owner, "Edit Purchase Order", true);
        this.parentPanel = parentPanel;
        this.orderId = orderId;

        try {
            // Load all suppliers for dropdown
            List<Supplier> suppliers = dbService.getAllSuppliers();
            supplierNameToIdMap = suppliers.stream()
                .collect(Collectors.toMap(Supplier::getSupplierName, Supplier::getSupplierId));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading suppliers: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            supplierNameToIdMap = Map.of();
        }

        initComponents();
        // Load order and prefill fields
        try {
            PurchaseOrder po = dbService.getPurchaseOrderById(String.valueOf(orderId));
            if (po != null) prefillOrderData(po);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading order: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header panel
        JPanel headerPanel = new JPanel(new GridLayout(4, 2, 10, 5));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Order Details"));
        headerPanel.add(new JLabel("PO Number"));
        poNumberField = new JTextField();
        poNumberField.setEditable(false);
        headerPanel.add(poNumberField);
        headerPanel.add(new JLabel("Supplier"));
        supplierComboBox = new JComboBox<>(supplierNameToIdMap.keySet().toArray(new String[0]));
        headerPanel.add(supplierComboBox);
        headerPanel.add(new JLabel("Expected Date (YYYY-MM-DD)"));
        expectedDateField = new JTextField();
        headerPanel.add(expectedDateField);
        headerPanel.add(new JLabel("Total Amount"));
        totalAmountField = new JTextField();
        totalAmountField.setEditable(false);
        headerPanel.add(totalAmountField);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Line items
        JPanel itemPanel = buildLineItemPanel();
        mainPanel.add(itemPanel, BorderLayout.CENTER);

        // Buttons
        JButton saveButton = new JButton("SAVE");
        saveButton.addActionListener(this::savePoAction);
        JButton cancelButton = new JButton("CANCEL");
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel buildLineItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Line Items"));
        String[] columnNames = {"Material ID", "Quantity", "Unit Price"};
        itemTableModel = new DefaultTableModel(columnNames, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
            public Class<?> getColumnClass(int col) {
                return col == 1 ? Integer.class : (col == 2 ? Double.class : String.class);
            }
        };
        itemTable = new JTable(itemTableModel);

        // Input fields for editing line items (add new)
        JPanel inputFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        inputFields.add(new JLabel("Material ID"));
        itemDrugIdField = new JTextField(8);
        inputFields.add(itemDrugIdField);
        inputFields.add(new JLabel("Qty"));
        itemQuantityField = new JTextField(5);
        inputFields.add(itemQuantityField);
        inputFields.add(new JLabel("Price"));
        itemPriceField = new JTextField(7);
        inputFields.add(itemPriceField);

        JButton addItemButton = new JButton("Add/Replace Item");
        addItemButton.addActionListener(this::addItemAction);
        JButton removeItemButton = new JButton("Remove Selected");
        removeItemButton.addActionListener(e -> {
            int row = itemTable.getSelectedRow();
            if(row >= 0) {
                itemTableModel.removeRow(row);
                recalculateTotal();
            }
        });

        inputFields.add(addItemButton);
        inputFields.add(removeItemButton);

        panel.add(new JScrollPane(itemTable), BorderLayout.CENTER);
        panel.add(inputFields, BorderLayout.SOUTH);

        return panel;
    }

    private void prefillOrderData(PurchaseOrder po) {
        poNumberField.setText(String.valueOf(po.getId()));
        supplierComboBox.setSelectedItem(po.getSupplierName());
        expectedDateField.setText(String.valueOf(po.getExpectedDate()));
        totalAmountField.setText(String.format("%.2f", po.getTotalAmount()));
        // Load line items
        itemTableModel.setRowCount(0);
        for (PurchaseOrderItem item : po.getItems()) {
            itemTableModel.addRow(new Object[]{item.getMaterialCode(), item.getQuantity(), item.getUnitPrice()});
        }
    }

    private void addItemAction(ActionEvent e) {
        try {
            String drugId = itemDrugIdField.getText().trim();
            int quantity = Integer.parseInt(itemQuantityField.getText().trim());
            double unitPrice = Double.parseDouble(itemPriceField.getText().trim());
            if (drugId.isEmpty() || quantity <= 0 || unitPrice <= 0)
                throw new IllegalArgumentException("All item fields must be valid and positive.");
            // if you want to replace DrugID if exists:
            for (int i = 0; i < itemTableModel.getRowCount(); i++) {
                String existingId = (String)itemTableModel.getValueAt(i, 0);
                if(existingId.equals(drugId)) {
                    itemTableModel.setValueAt(quantity, i, 1);
                    itemTableModel.setValueAt(unitPrice, i, 2);
                    recalculateTotal();
                    return;
                }
            }
            itemTableModel.addRow(new Object[] { drugId, quantity, unitPrice });
            recalculateTotal();
            itemDrugIdField.setText("");
            itemQuantityField.setText("");
            itemPriceField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity and Price must be numbers.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalculateTotal() {
        double total = 0.0;
        for (int i = 0; i < itemTableModel.getRowCount(); i++) {
            int quantity = (int) itemTableModel.getValueAt(i, 1);
            double price = (double) itemTableModel.getValueAt(i, 2);
            total += quantity * price;
        }
        totalAmountField.setText(String.format("%.2f", total));
    }

    private void savePoAction(ActionEvent e) {
        String selectedSupplierName = (String) supplierComboBox.getSelectedItem();
        String expectedDateStr = expectedDateField.getText().trim();
        double finalTotalAmount;

        if (selectedSupplierName == null || selectedSupplierName.isEmpty() || itemTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Supplier and at least one line item required.",
                    "Validation", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            LocalDate expectedDate = LocalDate.parse(expectedDateStr);
            finalTotalAmount = Double.parseDouble(totalAmountField.getText().replace(",", ""));
            if (finalTotalAmount <= 0) {
                JOptionPane.showMessageDialog(this, "Total must be greater than zero.",
                        "Validation", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Integer supplierId = supplierNameToIdMap.get(selectedSupplierName);
            if (supplierId == null) return;
            List<PurchaseOrderItem> poItems = new ArrayList<>();
            for (int i = 0; i < itemTableModel.getRowCount(); i++) {
                String materialCode = (String) itemTableModel.getValueAt(i, 0);
                int quantity = (int) itemTableModel.getValueAt(i, 1);
                double unitPrice = (double) itemTableModel.getValueAt(i, 2);
                poItems.add(new PurchaseOrderItem(materialCode, quantity, unitPrice));
            }
            PurchaseOrder updatedPo = new PurchaseOrder(
                orderId, supplierId, selectedSupplierName,
                LocalDate.now(), expectedDate, finalTotalAmount, "Pending", poItems);

            boolean success = dbService.updatePurchaseOrder(updatedPo);
            if(success){
                JOptionPane.showMessageDialog(this, "Purchase Order updated successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                if (parentPanel != null) parentPanel.loadOrderData();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update Purchase Order in database.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Total calculation error.",
                    "Validation", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating order: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        throw new UnsupportedOperationException("Unimplemented method 'isSaved'");
    }
}*/

package pharma.gui;

import pharma.model.PurchaseOrder;
import pharma.model.PurchaseOrder.PurchaseOrderItem;
import pharma.model.Supplier;
import pharma.service.DatabaseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EditPurchaseOrderDialog extends JDialog {
    private final DatabaseService dbService = DatabaseService.getInstance();
    private final PurchaseOrderPanel parentPanel;
    private final int orderId;

    // UI fields
    private JTextField poNumberField, expectedDateField, totalAmountField;
    private JComboBox<String> supplierComboBox;
    private DefaultTableModel itemTableModel;
    private JTable itemTable;
    private JTextField itemDrugIdField, itemQuantityField, itemPriceField;
    private Map<String, Integer> supplierNameToIdMap;
    private boolean saved = false; // <-- Key addition

    public EditPurchaseOrderDialog(JFrame owner, PurchaseOrderPanel parentPanel, int orderId) {
        super(owner, "Edit Purchase Order", true);
        this.parentPanel = parentPanel;
        this.orderId = orderId;

        try {
            List<Supplier> suppliers = dbService.getAllSuppliers();
            supplierNameToIdMap = suppliers.stream()
                    .collect(Collectors.toMap(Supplier::getSupplierName, Supplier::getSupplierId));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading suppliers: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            supplierNameToIdMap = Map.of();
        }

        initComponents();
        try {
            PurchaseOrder po = dbService.getPurchaseOrderById(String.valueOf(orderId));
            if (po != null)
                prefillOrderData(po);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading order: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel headerPanel = new JPanel(new GridLayout(4, 2, 10, 5));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Order Details"));
        headerPanel.add(new JLabel("PO Number"));
        poNumberField = new JTextField();
        poNumberField.setEditable(false);
        poNumberField.setBackground(new Color(45, 45, 45));
        poNumberField.setForeground(Color.WHITE);
        headerPanel.add(poNumberField);
        headerPanel.add(new JLabel("Supplier"));
        supplierComboBox = new JComboBox<>(supplierNameToIdMap.keySet().toArray(new String[0]));
        headerPanel.add(supplierComboBox);
        headerPanel.add(new JLabel("Expected Date (YYYY-MM-DD)"));
        expectedDateField = new JTextField();
        headerPanel.add(expectedDateField);
        headerPanel.add(new JLabel("Total Amount"));
        totalAmountField = new JTextField();
        totalAmountField.setEditable(false);
        totalAmountField.setBackground(new Color(45, 45, 45));
        totalAmountField.setForeground(Color.WHITE);
        headerPanel.add(totalAmountField);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Line items
        JPanel itemPanel = buildLineItemPanel();
        mainPanel.add(itemPanel, BorderLayout.CENTER);

        // Buttons
        JButton saveButton = new JButton("SAVE");
        saveButton.addActionListener(this::savePoAction);
        JButton cancelButton = new JButton("CANCEL");
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel buildLineItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Line Items"));
        String[] columnNames = { "Material ID", "Quantity", "Unit Price" };
        itemTableModel = new DefaultTableModel(columnNames, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            public Class<?> getColumnClass(int col) {
                return col == 1 ? Integer.class : (col == 2 ? Double.class : String.class);
            }
        };
        itemTable = new JTable(itemTableModel);
        JPanel inputFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        inputFields.add(new JLabel("Material ID"));
        itemDrugIdField = new JTextField(8);
        inputFields.add(itemDrugIdField);
        inputFields.add(new JLabel("Qty"));
        itemQuantityField = new JTextField(5);
        inputFields.add(itemQuantityField);
        inputFields.add(new JLabel("Price"));
        itemPriceField = new JTextField(7);
        inputFields.add(itemPriceField);
        JButton addItemButton = new JButton("Add/Replace Item");
        addItemButton.addActionListener(this::addItemAction);
        JButton removeItemButton = new JButton("Remove Selected");
        removeItemButton.addActionListener(e -> {
            int row = itemTable.getSelectedRow();
            if (row >= 0) {
                itemTableModel.removeRow(row);
                recalculateTotal();
            }
        });
        inputFields.add(addItemButton);
        inputFields.add(removeItemButton);
        panel.add(new JScrollPane(itemTable), BorderLayout.CENTER);
        panel.add(inputFields, BorderLayout.SOUTH);
        return panel;
    }

    private void prefillOrderData(PurchaseOrder po) {
        System.out.println("DEBUG: Prefilling order data for PO ID: " + po.getId());
        poNumberField.setText(String.valueOf(po.getId()));
        supplierComboBox.setSelectedItem(po.getSupplierName());
        expectedDateField.setText(String.valueOf(po.getExpectedDate()));
        totalAmountField.setText(String.format("%.2f", po.getTotalAmount()));
        // Load line items
        itemTableModel.setRowCount(0);
        System.out.println("DEBUG: Number of items in PO: " + (po.getItems() != null ? po.getItems().size() : 0));
        for (PurchaseOrderItem item : po.getItems()) {
            System.out.println("DEBUG: Adding item to table - Material: " + item.getMaterialCode() + ", Qty: "
                    + item.getQuantity() + ", Price: " + item.getUnitPrice());
            itemTableModel.addRow(new Object[] { item.getMaterialCode(), item.getQuantity(), item.getUnitPrice() });
        }
        System.out.println("DEBUG: Table now has " + itemTableModel.getRowCount() + " rows");
    }

    private void addItemAction(ActionEvent e) {
        try {
            String drugId = itemDrugIdField.getText().trim();
            int quantity = Integer.parseInt(itemQuantityField.getText().trim());
            double unitPrice = Double.parseDouble(itemPriceField.getText().trim());
            if (drugId.isEmpty() || quantity <= 0 || unitPrice <= 0)
                throw new IllegalArgumentException("All item fields must be valid and positive.");
            for (int i = 0; i < itemTableModel.getRowCount(); i++) {
                String existingId = (String) itemTableModel.getValueAt(i, 0);
                if (existingId.equals(drugId)) {
                    itemTableModel.setValueAt(quantity, i, 1);
                    itemTableModel.setValueAt(unitPrice, i, 2);
                    recalculateTotal();
                    return;
                }
            }
            itemTableModel.addRow(new Object[] { drugId, quantity, unitPrice });
            recalculateTotal();
            itemDrugIdField.setText("");
            itemQuantityField.setText("");
            itemPriceField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity and Price must be numbers.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalculateTotal() {
        double total = 0.0;
        for (int i = 0; i < itemTableModel.getRowCount(); i++) {
            int quantity = (int) itemTableModel.getValueAt(i, 1);
            double price = (double) itemTableModel.getValueAt(i, 2);
            total += quantity * price;
        }
        totalAmountField.setText(String.format("%.2f", total));
    }

    private void savePoAction(ActionEvent e) {
        String selectedSupplierName = (String) supplierComboBox.getSelectedItem();
        String expectedDateStr = expectedDateField.getText().trim();
        double finalTotalAmount;
        if (selectedSupplierName == null || selectedSupplierName.isEmpty() || itemTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Supplier and at least one line item required.",
                    "Validation", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            LocalDate expectedDate = LocalDate.parse(expectedDateStr);
            finalTotalAmount = Double.parseDouble(totalAmountField.getText().replace(",", ""));
            if (finalTotalAmount <= 0) {
                JOptionPane.showMessageDialog(this, "Total must be greater than zero.",
                        "Validation", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Integer supplierId = supplierNameToIdMap.get(selectedSupplierName);
            if (supplierId == null)
                return;
            List<PurchaseOrderItem> poItems = new ArrayList<>();
            for (int i = 0; i < itemTableModel.getRowCount(); i++) {
                String materialCode = (String) itemTableModel.getValueAt(i, 0);
                int quantity = (int) itemTableModel.getValueAt(i, 1);
                double unitPrice = (double) itemTableModel.getValueAt(i, 2);
                poItems.add(new PurchaseOrderItem(materialCode, quantity, unitPrice));
            }
            PurchaseOrder updatedPo = new PurchaseOrder(
                    orderId, supplierId, selectedSupplierName,
                    LocalDate.now(), expectedDate, finalTotalAmount, "Pending", poItems);

            boolean success = dbService.updatePurchaseOrder(updatedPo);
            if (success) {
                saved = true; // <-- Mark as saved for parent panel
                JOptionPane.showMessageDialog(this, "Purchase Order updated successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                if (parentPanel != null)
                    parentPanel.loadOrderData();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update Purchase Order in database.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Total calculation error.",
                    "Validation", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating order: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
