package pharma.gui;

import pharma.model.User;
import pharma.service.AuthService;
import pharma.service.DatabaseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.List;

/**
 * Quality Dashboard for managing QC status of batches.
 * Features: View batches in quarantine, release/reject batches, view genealogy.
 */
public class QualityDashboard extends JPanel {
    private DatabaseService dbService;
    private AuthService authService;
    private User currentUser;

    private JTable batchTable;
    private DefaultTableModel batchTableModel;
    private JTextArea detailsArea;
    private JButton releaseButton;
    private JButton rejectButton;
    private JButton genealogyButton;
    private JComboBox<String> filterComboBox;

    public QualityDashboard(DatabaseService dbService, AuthService authService, User currentUser) {
        this.dbService = dbService;
        this.authService = authService;
        this.currentUser = currentUser;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        loadBatches("All");
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("Quality Dashboard");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterPanel.add(new JLabel("Filter:"));

        filterComboBox = new JComboBox<>(new String[] { "All", "QUARANTINE Only", "RELEASED Only", "REJECTED Only" });
        filterComboBox.addActionListener(e -> {
            String filter = (String) filterComboBox.getSelectedItem();
            String status = filter.replace(" Only", "");
            loadBatches(status);
        });
        filterPanel.add(filterComboBox);

        panel.add(filterPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));

        panel.add(createBatchTablePanel());
        panel.add(createDetailsPanel());

        return panel;
    }

    private JPanel createBatchTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Batches Requiring QC"));

        String[] columns = { "Batch Number", "Material", "Quantity", "QC Status", "Mfg Date", "Exp Date" };
        batchTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        batchTable = new JTable(batchTableModel);
        batchTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showBatchDetails();
            }
        });

        // Color code rows by QC status
        batchTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) table.getValueAt(row, 3);

                if ("QUARANTINE".equals(status)) {
                    c.setBackground(new Color(255, 255, 200)); // Yellow
                } else if ("RELEASED".equals(status)) {
                    c.setBackground(new Color(200, 255, 200)); // Green
                } else if ("REJECTED".equals(status)) {
                    c.setBackground(new Color(255, 200, 200)); // Red
                }

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(batchTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Batch Details"));

        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(detailsArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        releaseButton = new JButton("Release Batch");
        releaseButton.setBackground(new Color(144, 238, 144));
        releaseButton.addActionListener(e -> updateQCStatus("RELEASED"));
        releaseButton.setEnabled(authService.hasPermission(currentUser, "UPDATE_QC_STATUS"));
        buttonPanel.add(releaseButton);

        rejectButton = new JButton("Reject Batch");
        rejectButton.setBackground(new Color(255, 160, 160));
        rejectButton.addActionListener(e -> updateQCStatus("REJECTED"));
        rejectButton.setEnabled(authService.hasPermission(currentUser, "UPDATE_QC_STATUS"));
        buttonPanel.add(rejectButton);

        genealogyButton = new JButton("View Genealogy");
        genealogyButton.addActionListener(e -> viewGenealogy());
        buttonPanel.add(genealogyButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadBatches(String statusFilter) {
        try {
            batchTableModel.setRowCount(0);

            // Use a custom query method
            java.util.List<BatchInfo> batches = getBatchesForQC(statusFilter);

            for (BatchInfo batch : batches) {
                batchTableModel.addRow(new Object[] {
                        batch.batchNumber,
                        batch.brandName,
                        batch.quantity,
                        batch.qcStatus,
                        batch.mfgDate,
                        batch.expDate
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading batches: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private java.util.List<BatchInfo> getBatchesForQC(String statusFilter) throws Exception {
        java.util.List<BatchInfo> batches = new java.util.ArrayList<>();

        String sql = "SELECT si.batch_number, dm.brand_name, si.quantity, si.qc_status, si.mfg_date, si.exp_date " +
                "FROM Stock_Inventory si " +
                "JOIN Drug_Master dm ON si.material_code = dm.material_code ";

        if (!"All".equals(statusFilter)) {
            sql += "WHERE si.qc_status = ? ";
        }

        sql += "ORDER BY si.mfg_date DESC";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/pharma_ims?allowPublicKeyRetrieval=true&useSSL=false",
                    "root",
                    "SiriusBlack@369");

            pstmt = conn.prepareStatement(sql);

            if (!"All".equals(statusFilter)) {
                pstmt.setString(1, statusFilter);
            }

            rs = pstmt.executeQuery();
            while (rs.next()) {
                BatchInfo batch = new BatchInfo();
                batch.batchNumber = rs.getString("batch_number");
                batch.brandName = rs.getString("brand_name");
                batch.quantity = rs.getDouble("quantity");
                batch.qcStatus = rs.getString("qc_status");
                batch.mfgDate = rs.getDate("mfg_date");
                batch.expDate = rs.getDate("exp_date");
                batches.add(batch);
            }
        } finally {
            if (rs != null)
                rs.close();
            if (pstmt != null)
                pstmt.close();
            if (conn != null)
                conn.close();
        }

        return batches;
    }

    private static class BatchInfo {
        String batchNumber;
        String brandName;
        double quantity;
        String qcStatus;
        java.sql.Date mfgDate;
        java.sql.Date expDate;
    }

    private void showBatchDetails() {
        int selectedRow = batchTable.getSelectedRow();
        if (selectedRow < 0)
            return;

        try {
            String batchNumber = (String) batchTable.getValueAt(selectedRow, 0);

            String sql = "SELECT si.*, dm.brand_name, dm.generic_name, dm.manufacturer " +
                    "FROM Stock_Inventory si " +
                    "JOIN Drug_Master dm ON si.material_code = dm.material_code " +
                    "WHERE si.batch_number = ?";

            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/pharma_ims?allowPublicKeyRetrieval=true&useSSL=false",
                        "root",
                        "SiriusBlack@369");

                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, batchNumber);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    StringBuilder details = new StringBuilder();
                    details.append("=== BATCH INFORMATION ===\n\n");
                    details.append("Batch Number:     ").append(rs.getString("batch_number")).append("\n");
                    details.append("Material Code:    ").append(rs.getString("material_code")).append("\n");
                    details.append("Brand Name:       ").append(rs.getString("brand_name")).append("\n");
                    details.append("Generic Name:     ").append(rs.getString("generic_name")).append("\n");
                    details.append("Manufacturer:     ").append(rs.getString("manufacturer")).append("\n\n");

                    details.append("=== QUANTITY & LOCATION ===\n\n");
                    details.append("Quantity:         ").append(rs.getDouble("quantity")).append("\n");
                    details.append("Location:         ").append(rs.getString("location_code")).append("\n");
                    details.append("Unit Cost:        $").append(rs.getDouble("unit_cost")).append("\n\n");

                    details.append("=== DATES ===\n\n");
                    details.append("Mfg Date:         ").append(rs.getDate("mfg_date")).append("\n");
                    details.append("Exp Date:         ").append(rs.getDate("exp_date")).append("\n\n");

                    details.append("=== QC STATUS ===\n\n");
                    details.append("Status:           ").append(rs.getString("qc_status")).append("\n\n");

                    details.append("=== TRACEABILITY ===\n\n");
                    String parentBatches = rs.getString("parent_batch_id");
                    details.append("Parent Batches:   ").append(parentBatches != null ? parentBatches : "None")
                            .append("\n");

                    int productionOrderId = rs.getInt("production_order_id");
                    if (!rs.wasNull()) {
                        details.append("Production Order: ").append(productionOrderId).append("\n");
                    }

                    detailsArea.setText(details.toString());
                }
            } finally {
                if (rs != null)
                    rs.close();
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            }
        } catch (Exception e) {
            detailsArea.setText("Error loading details: " + e.getMessage());
        }
    }

    private void updateQCStatus(String newStatus) {
        int selectedRow = batchTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a batch", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!authService.hasPermission(currentUser, "UPDATE_QC_STATUS")) {
            JOptionPane.showMessageDialog(this,
                    "You don't have permission to update QC status.\nRequired role: QA Analyst", "Permission Denied",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String batchNumber = (String) batchTable.getValueAt(selectedRow, 0);
            String currentStatus = (String) batchTable.getValueAt(selectedRow, 3);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Update QC status for batch: " + batchNumber + "\n" +
                            "From: " + currentStatus + "\n" +
                            "To: " + newStatus + "\n\n" +
                            "This action will be logged in the audit trail.",
                    "Confirm QC Status Update",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                dbService.updateQCStatus(batchNumber, newStatus, currentUser.getUserId());
                JOptionPane.showMessageDialog(this, "QC status updated successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                String currentFilter = (String) filterComboBox.getSelectedItem();
                String status = currentFilter.replace(" Only", "");
                loadBatches(status);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating QC status: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewGenealogy() {
        int selectedRow = batchTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a batch", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String batchNumber = (String) batchTable.getValueAt(selectedRow, 0);
            List<String> parentBatches = dbService.getBatchGenealogy(batchNumber);

            if (parentBatches.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No parent batches found for this batch.\nThis might be a raw material batch.", "Genealogy",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                StringBuilder message = new StringBuilder();
                message.append("Parent Batches for: ").append(batchNumber).append("\n\n");
                message.append("This finished good was produced from the following raw material batches:\n\n");

                for (int i = 0; i < parentBatches.size(); i++) {
                    message.append((i + 1)).append(". ").append(parentBatches.get(i)).append("\n");
                }

                message.append("\n(These batches were consumed during production using FEFO logic)");

                JTextArea textArea = new JTextArea(message.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(500, 300));

                JOptionPane.showMessageDialog(this, scrollPane, "Batch Genealogy", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error viewing genealogy: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
