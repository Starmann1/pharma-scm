package pharma.gui;

import pharma.service.DatabaseService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Date;
import java.util.List; // ⬅️ ADDED IMPORT
import pharma.model.PurchaseOrder;
import pharma.model.GRN; // ⬅️ ADDED IMPORT (Required for data loading)

public class GRNPanel extends JPanel {
    private JFrame mainFrame;
    private JTable grnTable;
    private DefaultTableModel tableModel;
    private DatabaseService dbService;

    // FIX 1: Constructor now requires the JFrame reference
    public GRNPanel(JFrame mainFrame, DatabaseService dbService) {
        this.mainFrame = mainFrame;
        this.dbService = dbService;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Goods Received Notes (GRN) Management"));

        // --- Top Control Panel ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton createGrnBtn = new JButton("Create GRN from PO");
        JButton viewDetailsBtn = new JButton("View Details");
        JButton refreshBtn = new JButton("Refresh");

        controlPanel.add(createGrnBtn);
        controlPanel.add(viewDetailsBtn);
        controlPanel.add(refreshBtn);

        add(controlPanel, BorderLayout.NORTH);

        // --- Center Table View ---
        String[] columnNames = { "GRN ID", "PO ID", "Supplier", "Received Date", "Status" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        grnTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(grnTable);
        add(scrollPane, BorderLayout.CENTER);

        // Load initial data (Calls the public method below)
        loadGRNData();

        // --- Event Handling (Fixes Problem 3) ---
        createGrnBtn.addActionListener(e -> handleCreateGRN());
        viewDetailsBtn.addActionListener(e -> handleViewDetails()); // FIX: Added View Details handler
        refreshBtn.addActionListener(e -> loadGRNData()); // Calls the implemented public loader
    }

    /**
     * FIX 2: Loads GRN data from the database and populates the table (Problem 5
     * implementation).
     * This is the correct implementation for the method that was previously an
     * unimplemented stub.
     */
    public void loadGRNData() {
        System.out.println("DEBUG: loadGRNData() called - Refreshing GRN list...");
        tableModel.setRowCount(0);

        try {
            // NOTE: dbService.getGRNs() must be implemented to return List<GRN>
            List<GRN> grns = dbService.getGRNs();

            System.out.println("DEBUG: Retrieved " + (grns != null ? grns.size() : 0) + " GRNs from database");

            if (grns != null) {
                for (GRN grn : grns) {
                    System.out.println("DEBUG: Adding GRN - ID: " + grn.getId() + ", PO: " + grn.getPurchaseOrderId()
                            + ", Supplier: " + grn.getSupplierName());
                    tableModel.addRow(new Object[] {
                            grn.getId(),
                            grn.getPurchaseOrderId(),
                            grn.getSupplierName(),
                            grn.getReceivedDate(),
                            grn.getStatus()
                    });
                }
            }
            System.out.println("DEBUG: GRN table now has " + tableModel.getRowCount() + " rows");
        } catch (Exception ex) {
            System.err.println("Error running loadGRNData: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading GRN data: " + ex.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);

            // Fallback simulation (Only add simulation data if the table is empty)
            if (tableModel.getRowCount() == 0) {
                tableModel.addRow(new Object[] { 5001, 1001, "Acme Pharma Inc.", new Date(), "Completed" });
                tableModel.addRow(new Object[] { 5002, 1003, "MediSuppliers Co.", new Date(), "Verified" });
            }
        }
    }

    /**
     * FIX 3: Implements the logic to handle button click and open the form.
     */
    private void handleCreateGRN() {
        try {
            // Get all purchase orders that can have GRNs created (typically "Pending" or
            // "Shipped" status)
            List<PurchaseOrder> availablePOs = dbService.getPurchaseOrders();

            if (availablePOs == null || availablePOs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No Purchase Orders available to create GRN.", "No Data",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Create a list of PO descriptions for selection
            String[] poOptions = new String[availablePOs.size()];
            for (int i = 0; i < availablePOs.size(); i++) {
                PurchaseOrder po = availablePOs.get(i);
                poOptions[i] = "PO #" + po.getId() + " - " + po.getSupplierName() + " (" + po.getStatus() + ")";
            }

            // Show selection dialog
            String selected = (String) JOptionPane.showInputDialog(
                    this,
                    "Select a Purchase Order to create GRN:",
                    "Create GRN from PO",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    poOptions,
                    poOptions[0]);

            if (selected != null) {
                // Find the selected PO
                int selectedIndex = -1;
                for (int i = 0; i < poOptions.length; i++) {
                    if (poOptions[i].equals(selected)) {
                        selectedIndex = i;
                        break;
                    }
                }

                if (selectedIndex >= 0) {
                    PurchaseOrder selectedPO = availablePOs.get(selectedIndex);
                    // Open the CreateGRNDialog with the selected PO
                    CreateGRNDialog dialog = new CreateGRNDialog(this.mainFrame, this, selectedPO);
                    dialog.setVisible(true);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error processing GRN creation: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * FIX 4: Public callback method for the dialog to refresh the list after a
     * successful save.
     */
    public void refreshGRNList() {
        loadGRNData();
    }

    /**
     * Handles viewing details of a selected GRN
     */
    private void handleViewDetails() {
        int row = grnTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a GRN row.", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int grnId = (int) tableModel.getValueAt(row, 0);
        GRN grn = dbService.getGRNById(grnId);
        if (grn == null) {
            JOptionPane.showMessageDialog(this, "GRN not found.", "Data Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ViewGRNDetailsDialog dlg = new ViewGRNDetailsDialog(mainFrame, grn);
        dlg.setVisible(true);
    }
}
