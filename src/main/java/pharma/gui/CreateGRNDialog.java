package pharma.gui;

import pharma.model.PurchaseOrder;
import pharma.model.Supplier;
import pharma.service.DatabaseService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Date;

public class CreateGRNDialog extends JDialog {

    private GRNPanel parentPanel;
    private PurchaseOrder purchaseOrder;
    private DatabaseService dbService;
    private JTable itemsTable;

    public CreateGRNDialog(JFrame owner, GRNPanel parent, PurchaseOrder po, DatabaseService dbService) {
        super(owner, "Create GRN for PO: " + po.getPoNumber(), true);
        this.parentPanel = parent;
        this.purchaseOrder = po;
        this.dbService = dbService;
        setLayout(new BorderLayout(10, 10));

        // --- Header Panel (Displaying PO Info) ---
        JPanel headerPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Purchase Order Details"));

        headerPanel.add(new JLabel("PO Number:"));
        headerPanel.add(new JLabel(po.getPoNumber()));

        headerPanel.add(new JLabel("Supplier:"));
        headerPanel.add(new JLabel(po.getSupplierName())); // Assuming this getter exists

        headerPanel.add(new JLabel("GRN Date:"));
        headerPanel.add(new JTextField(new Date().toString()));

        add(headerPanel, BorderLayout.NORTH);

        // --- Center Panel (GRN Line Items) ---
        String[] columnNames = {"Material Code", "PO Qty", "Receive Qty", "Batch", "Expiry Date"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 2; // Allow editing only Receive Qty, Batch, Expiry Date
            }
        };

        try {
            List<PurchaseOrder.PurchaseOrderItem> items = dbService.getPurchaseOrderItems(po.getId());
            for (PurchaseOrder.PurchaseOrderItem item : items) {
                String materialCode = item.getMaterialCode();
                int quantity = item.getQuantity();
                String defaultBatch = "BATCH-" + System.currentTimeMillis() % 100000 + "-" + materialCode;
                String defaultExpiry = LocalDate.now().plusYears(2).toString();
                tableModel.addRow(new Object[] { materialCode, quantity, quantity, defaultBatch, defaultExpiry });
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error fetching PO items for grid: " + e.getMessage());
        }

        itemsTable = new JTable(tableModel);
        add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        // --- Button Panel (OK and CANCEL buttons, related to Problem 4) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save GRN");
        JButton cancelButton = new JButton("CANCEL");

        saveButton.addActionListener(e -> saveGRN());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(800, 600);
        setLocationRelativeTo(owner);
    }

    private void saveGRN() {
        // In a real application, logic here would validate items received and
        // call DatabaseService.createGRN(purchaseOrder, grnDetails);

        System.out.println("DEBUG: Attempting to save GRN for PO ID: " + purchaseOrder.getId());

        try {
            Supplier supplier = dbService.getSupplierById(purchaseOrder.getSupplierId());
            if (supplier != null && !Supplier.STATUS_APPROVED.equalsIgnoreCase(supplier.getSupplierStatus())) {
                JOptionPane.showMessageDialog(this, "Cannot create GRN for unapproved supplier.", "Approval Required",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error validating supplier approval: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Placeholder save logic:
        boolean success = dbService.createGRNFromPO(purchaseOrder);

        System.out.println("DEBUG: GRN creation " + (success ? "SUCCESSFUL" : "FAILED"));

        if (success) {
            JOptionPane.showMessageDialog(this, "GRN successfully created and inventory updated.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            // Refresh the GRN list (Problem 5 fix)
            System.out.println("DEBUG: Calling parentPanel.loadGRNData() to refresh list...");
            parentPanel.loadGRNData();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "GRN creation failed. Check database logs.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
