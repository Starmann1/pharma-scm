package pharma.gui;

import pharma.model.PurchaseOrder;
import pharma.service.DatabaseService;
import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class CreateGRNDialog extends JDialog {

    private GRNPanel parentPanel;
    private PurchaseOrder purchaseOrder;
    // UI components for GRN details, e.g., GRN Date, Received Items Table, etc.
    // private JTable itemsTable;

    public CreateGRNDialog(JFrame owner, GRNPanel parent, PurchaseOrder po) {
        super(owner, "Create GRN for PO: " + po.getPoNumber(), true);
        this.parentPanel = parent;
        this.purchaseOrder = po;
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

        // --- Center Panel (GRN Line Items - Placeholder for complex logic) ---
        JTextArea itemsArea = new JTextArea(
                "GRN line item grid will go here. Must reflect PO items and allow actual quantities/batch/expiry data entry.");
        itemsArea.setEditable(false);
        add(new JScrollPane(itemsArea), BorderLayout.CENTER);

        // --- Button Panel (OK and CANCEL buttons, related to Problem 4) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save GRN");
        JButton cancelButton = new JButton("CANCEL");

        saveButton.addActionListener(_ -> saveGRN());
        cancelButton.addActionListener(_ -> dispose());

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

        // Placeholder save logic:
        boolean success = DatabaseService.getInstance().createGRNFromPO(purchaseOrder);

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
