package pharma.gui;

import pharma.model.GRN;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ViewGRNDetailsDialog extends JDialog {
    public ViewGRNDetailsDialog(JFrame owner, GRN grn) {
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

        // Fetch PO items directly from DB and join with GRN details to populate the grid
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/pharma_ims?allowPublicKeyRetrieval=true&useSSL=false", "root", "SiriusBlack@369");
                 PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT pi.drug_id, pi.quantity AS po_qty, pi.unit_price, gi.quantity_received, gi.batch_number, gi.expiry_date " +
                    "FROM PurchaseOrder_Item pi " +
                    "LEFT JOIN GRN_Item gi ON pi.drug_id = gi.drug_id AND gi.grn_id = ? " +
                    "WHERE pi.po_id = ?")) {
                
                pstmt.setInt(1, grn.getId());
                pstmt.setInt(2, grn.getPurchaseOrderId());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String matCode = rs.getString("drug_id");
                        int poQty = rs.getInt("po_qty");
                        int recQty = rs.getInt("quantity_received");
                        double unitPrice = rs.getDouble("unit_price");
                        double totalCost = recQty * unitPrice;
                        String batch = rs.getString("batch_number");
                        String expiry = rs.getString("expiry_date");
                        
                        tableModel.addRow(new Object[]{
                            matCode, poQty, recQty, batch, expiry, 
                            String.format("$%.2f", unitPrice), 
                            String.format("$%.2f", totalCost)
                        });
                    }
                }
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
