package pharma.gui;

import pharma.model.GRN;
import pharma.model.PurchaseOrder;
import pharma.service.DatabaseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ViewGRNDetailsDialog extends JDialog {
    public ViewGRNDetailsDialog(JFrame owner, GRN grn, DatabaseService dbService) {
        super(owner, "GRN Details", true);

        setLayout(new BorderLayout(10, 10));
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("GRN Details"));

        panel.add(new JLabel("GRN ID:"));
        panel.add(new JLabel(String.valueOf(grn.getId())));
        panel.add(new JLabel("PO ID:"));
        panel.add(new JLabel(String.valueOf(grn.getPurchaseOrderId())));
        panel.add(new JLabel("Supplier:"));
        panel.add(new JLabel(grn.getSupplierName()));
        panel.add(new JLabel("Received Date:"));
        panel.add(new JLabel(grn.getReceivedDate().toString()));
        panel.add(new JLabel("Status:"));
        panel.add(new JLabel(grn.getStatus()));

        add(panel, BorderLayout.NORTH);

        // --- Center Panel (GRN Line Items) ---
        String[] columnNames = {"Material Code", "PO Qty", "Receive Qty", "Batch", "Expiry Date", "Unit Price", "Total Cost"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        try {
            Map<String, PurchaseOrder.PurchaseOrderItem> poItemsByMaterial = new HashMap<>();
            for (PurchaseOrder.PurchaseOrderItem item : dbService.getPurchaseOrderItems(grn.getPurchaseOrderId())) {
                poItemsByMaterial.put(item.getMaterialCode(), item);
            }

            for (GRN.GRNItem item : grn.getItems()) {
                PurchaseOrder.PurchaseOrderItem poItem = poItemsByMaterial.get(item.getMaterialCode());
                int poQuantity = poItem != null ? poItem.getQuantity() : 0;
                double unitPrice = poItem != null ? poItem.getUnitPrice() : 0.0;
                int receivedQuantity = item.getQuantityReceived();
                double totalCost = receivedQuantity * unitPrice;
                String expiry = item.getExpiryDate() != null ? item.getExpiryDate().toString() : "";

                tableModel.addRow(new Object[] {
                        item.getMaterialCode(),
                        poQuantity,
                        receivedQuantity,
                        item.getBatchNumber(),
                        expiry,
                        String.format("$%.2f", unitPrice),
                        String.format("$%.2f", totalCost)
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error fetching PO/GRN items for grid: " + e.getMessage());
        }

        JTable itemsTable = new JTable(tableModel);
        add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> dispose());
        buttonPanel.add(okBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(700, 450);
        setLocationRelativeTo(owner);
    }
}
