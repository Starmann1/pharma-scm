package pharma.gui;

import pharma.model.Supplier;
import pharma.service.DatabaseService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SuppliersPanel extends JPanel {
    private DatabaseService dbService;
    private JTable supplierTable;
    private DefaultTableModel tableModel;

    private final String[] COLUMN_NAMES = {
            "ID", "Name", "Contact Person", "Address", "Email", "Phone", "GSTIN", "License No.", "Payment Terms"
    };

    public SuppliersPanel(DatabaseService dbService) {
        this.dbService = dbService;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Supplier Master: Manage Vendor Information", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        add(titleLabel, BorderLayout.NORTH);

        // Table Setup
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0;
            }
        };
        supplierTable = new JTable(tableModel);
        supplierTable.setRowHeight(25);
        supplierTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        JScrollPane scrollPane = new JScrollPane(supplierTable);
        add(scrollPane, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton refreshButton = new JButton("Refresh Data");
        JButton addButton = new JButton("Add Supplier");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");

        controlPanel.add(refreshButton);
        controlPanel.add(addButton);
        controlPanel.add(editButton);
        controlPanel.add(deleteButton);

        add(controlPanel, BorderLayout.SOUTH);

        // Action Listeners
        refreshButton.addActionListener(e -> loadSuppliers());
        addButton.addActionListener(e -> showAddEditDialog(null));
        editButton.addActionListener(e -> {
            int selectedRow = supplierTable.getSelectedRow();
            if (selectedRow != -1) {
                Supplier selectedSupplier = getSupplierFromSelectedRow(selectedRow);
                showAddEditDialog(selectedSupplier);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a supplier to edit.",
                        "Selection Error", JOptionPane.WARNING_MESSAGE);
            }
        });
        deleteButton.addActionListener(e -> deleteSelectedSupplier());

        loadSuppliers();
    }

    private void loadSuppliers() {
        tableModel.setRowCount(0);
        try {
            List<Supplier> suppliers = dbService.getAllSuppliers();
            for (Supplier s : suppliers) {
                Object[] rowData = {
                        s.getSupplierId(),
                        s.getSupplierName(),
                        s.getContactPerson(),
                        s.getAddress(),
                        s.getEmail(),
                        s.getPhoneNumber(),
                        s.getGstin(),
                        s.getDrugLicenseNumber(),
                        s.getPaymentTerms()
                };
                tableModel.addRow(rowData);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load supplier data: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void showAddEditDialog(Supplier supplier) {
        String title = (supplier == null) ? "Add New Supplier" : "Edit Supplier: " + supplier.getSupplierName();

        JTextField nameField = new JTextField(supplier != null ? supplier.getSupplierName() : "", 20);
        JTextField contactField = new JTextField(supplier != null ? supplier.getContactPerson() : "", 20);
        JTextField emailField = new JTextField(supplier != null ? supplier.getEmail() : "", 20);
        JTextField phoneField = new JTextField(supplier != null ? supplier.getPhoneNumber() : "", 20);
        JTextField gstinField = new JTextField(supplier != null ? supplier.getGstin() : "", 20);
        JTextField licenseField = new JTextField(supplier != null ? supplier.getDrugLicenseNumber() : "", 20);
        JTextField termsField = new JTextField(supplier != null ? supplier.getPaymentTerms() : "", 20);
        JTextArea addressArea = new JTextArea(supplier != null ? supplier.getAddress() : "", 3, 20);
        addressArea.setLineWrap(true);
        JScrollPane addressScroll = new JScrollPane(addressArea);

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.add(new JLabel("Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Contact Person:"));
        formPanel.add(contactField);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(emailField);
        formPanel.add(new JLabel("Phone:"));
        formPanel.add(phoneField);
        formPanel.add(new JLabel("Address:"));
        formPanel.add(addressScroll);
        formPanel.add(new JLabel("GSTIN:"));
        formPanel.add(gstinField);
        formPanel.add(new JLabel("License No.:"));
        formPanel.add(licenseField);
        formPanel.add(new JLabel("Payment Terms:"));
        formPanel.add(termsField);

        int result = JOptionPane.showConfirmDialog(this, formPanel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Supplier newOrUpdatedSupplier = supplier != null ? supplier : new Supplier();

            newOrUpdatedSupplier.setSupplierName(nameField.getText());
            newOrUpdatedSupplier.setContactPerson(contactField.getText());
            newOrUpdatedSupplier.setEmail(emailField.getText());
            newOrUpdatedSupplier.setPhoneNumber(phoneField.getText());
            newOrUpdatedSupplier.setAddress(addressArea.getText());
            newOrUpdatedSupplier.setGstin(gstinField.getText());
            newOrUpdatedSupplier.setDrugLicenseNumber(licenseField.getText());
            newOrUpdatedSupplier.setPaymentTerms(termsField.getText());

            if (supplier == null) {
                dbService.addSupplier(newOrUpdatedSupplier);
            } else {
                dbService.updateSupplier(newOrUpdatedSupplier);
            }
            loadSuppliers();
        }
    }

    private Supplier getSupplierFromSelectedRow(int row) {
        Supplier s = new Supplier();
        s.setSupplierId((int) tableModel.getValueAt(row, 0));
        s.setSupplierName((String) tableModel.getValueAt(row, 1));
        s.setContactPerson((String) tableModel.getValueAt(row, 2));
        s.setAddress((String) tableModel.getValueAt(row, 3));
        s.setEmail((String) tableModel.getValueAt(row, 4));
        s.setPhoneNumber((String) tableModel.getValueAt(row, 5));
        s.setGstin((String) tableModel.getValueAt(row, 6));
        s.setDrugLicenseNumber((String) tableModel.getValueAt(row, 7));
        s.setPaymentTerms((String) tableModel.getValueAt(row, 8));
        return s;
    }

    private void deleteSelectedSupplier() {
        int selectedRow = supplierTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier to delete.",
                    "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int supplierId = (int) tableModel.getValueAt(selectedRow, 0);
        String supplierName = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete supplier: " + supplierName + "?\nThis action cannot be undone.",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (dbService.deleteSupplier(supplierId)) {
                JOptionPane.showMessageDialog(this, "Supplier deleted successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                loadSuppliers();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to delete supplier. Check database constraints.",
                        "Deletion Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
