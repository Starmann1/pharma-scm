package pharma.gui;

import pharma.model.Stock;
import pharma.model.User;
import pharma.service.AuthService;
import pharma.service.DatabaseService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
    private JButton sampleButton;
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

        filterComboBox = new JComboBox<>(
                new String[] { "All", "QI Only", "QUARANTINE Only", "APPROVED Only", "REJECTED Only" });
        filterComboBox.addActionListener(e -> {
            loadBatches(getSelectedStatusFilter());
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

                if ("QI".equals(status)) {
                    c.setBackground(new Color(255, 204, 153)); // Orange/Amber
                    c.setForeground(Color.BLACK);
                } else if ("QUARANTINE".equals(status)) {
                    c.setBackground(new Color(255, 255, 200)); // Yellow
                    c.setForeground(Color.BLACK);
                } else if ("APPROVED".equals(status)) {
                    c.setBackground(new Color(200, 255, 200)); // Green
                    c.setForeground(Color.BLACK);
                } else if ("REJECTED".equals(status)) {
                    c.setBackground(new Color(255, 200, 200)); // Red
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
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

        sampleButton = new JButton("Take Sample");
        sampleButton.setBackground(new Color(255, 235, 156));
        sampleButton.setForeground(Color.BLACK);
        sampleButton.addActionListener(e -> takeSample());
        buttonPanel.add(sampleButton);

        releaseButton = new JButton("Approve Batch");
        releaseButton.setBackground(new Color(144, 238, 144));
        releaseButton.setForeground(Color.BLACK);
        releaseButton.addActionListener(e -> updateQCStatus("APPROVED"));
        buttonPanel.add(releaseButton);

        rejectButton = new JButton("Reject Batch");
        rejectButton.setBackground(new Color(255, 160, 160));
        rejectButton.setForeground(Color.BLACK);
        rejectButton.addActionListener(e -> updateQCStatus("REJECTED"));
        buttonPanel.add(rejectButton);

        genealogyButton = new JButton("View Genealogy");
        genealogyButton.addActionListener(e -> viewGenealogy());
        buttonPanel.add(genealogyButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        updateActionButtons();

        return panel;
    }

    private void loadBatches(String statusFilter) {
        try {
            batchTableModel.setRowCount(0);
            List<Stock> batches = dbService.getQCBatches(statusFilter);

            for (Stock batch : batches) {
                batchTableModel.addRow(new Object[] {
                        batch.getBatchNumber(),
                        batch.getBrandName(),
                        batch.getQuantity(),
                        batch.getQcStatus(),
                        batch.getMfgDate(),
                        batch.getExpDate()
                });
            }
            detailsArea.setText("");
            updateActionButtons();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading batches: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showBatchDetails() {
        int selectedRow = batchTable.getSelectedRow();
        if (selectedRow < 0) {
            detailsArea.setText("");
            updateActionButtons();
            return;
        }

        try {
            String batchNumber = (String) batchTable.getValueAt(selectedRow, 0);
            Stock stock = dbService.getStockByBatchNumber(batchNumber);
            if (stock == null) {
                detailsArea.setText("Batch details not found.");
            } else {
                StringBuilder details = new StringBuilder();
                details.append("=== BATCH INFORMATION ===\n\n");
                details.append("Batch Number:     ").append(stock.getBatchNumber()).append("\n");
                details.append("Material Code:    ").append(stock.getMaterialCode()).append("\n");
                details.append("Brand Name:       ").append(stock.getBrandName()).append("\n");
                details.append("Generic Name:     ").append(stock.getGenericName()).append("\n");
                details.append("Manufacturer:     ").append(stock.getManufacturer()).append("\n\n");

                details.append("=== QUANTITY & LOCATION ===\n\n");
                details.append("Quantity:         ").append(stock.getQuantity()).append("\n");
                details.append("Location:         ").append(stock.getLocationCode()).append("\n");
                details.append("Unit Cost:        $").append(stock.getUnitCost()).append("\n\n");

                details.append("=== DATES ===\n\n");
                details.append("Mfg Date:         ").append(stock.getMfgDate()).append("\n");
                details.append("Exp Date:         ").append(stock.getExpDate()).append("\n\n");

                details.append("=== QC STATUS ===\n\n");
                details.append("Status:           ").append(stock.getQcStatus()).append("\n\n");

                details.append("=== TRACEABILITY ===\n\n");
                String parentBatches = stock.getParentBatchId();
                details.append("Parent Batches:   ").append(parentBatches != null ? parentBatches : "None").append("\n");

                if (stock.getProductionOrderId() > 0) {
                    details.append("Production Order: ").append(stock.getProductionOrderId()).append("\n");
                }

                detailsArea.setText(details.toString());
            }
            updateActionButtons();
        } catch (Exception e) {
            detailsArea.setText("Error loading details: " + e.getMessage());
            updateActionButtons();
        }
    }

    private void takeSample() {
        int selectedRow = batchTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a batch", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String batchNumber = (String) batchTable.getValueAt(selectedRow, 0);
            String currentStatus = (String) batchTable.getValueAt(selectedRow, 3);

            if (!"QUARANTINE".equalsIgnoreCase(currentStatus)) {
                JOptionPane.showMessageDialog(this, "Only quarantined batches can be sampled.", "Invalid Action",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!authService.hasPermission(currentUser, "UPDATE_QC_STATUS")) {
                JOptionPane.showMessageDialog(this,
                        "You don't have permission to take samples for QC.\nRequired Role: Quality Analyst",
                        "Permission Denied",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Take QC sample for batch: " + batchNumber + "\n" +
                            "From: QUARANTINE\n" +
                            "To: QI\n\n" +
                            "This action will be logged in the audit trail.",
                    "Confirm Sample Collection",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                dbService.takeSampleForQC(batchNumber, currentUser.getUserId());
                JOptionPane.showMessageDialog(this, "Batch moved to QI successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshCurrentFilter();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error taking QC sample: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateQCStatus(String newStatus) {
        int selectedRow = batchTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a batch", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isRelease = "APPROVED".equals(newStatus);
        boolean isReject = "REJECTED".equals(newStatus);
        boolean hasPerm = (isRelease && authService.hasPermission(currentUser, "RELEASE_BATCH")) ||
                (isReject && authService.hasPermission(currentUser, "REJECT_BATCH")) ||
                authService.hasPermission(currentUser, "UPDATE_QC_STATUS");

        if (!hasPerm) {
            JOptionPane.showMessageDialog(this,
                    "You don't have permission to perform this QC status update.\nRequired Role: Quality Analyst",
                    "Permission Denied",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String batchNumber = (String) batchTable.getValueAt(selectedRow, 0);
            String currentStatus = (String) batchTable.getValueAt(selectedRow, 3);

            if (!"QI".equalsIgnoreCase(currentStatus)) {
                JOptionPane.showMessageDialog(this, "Only QI batches can be approved or rejected.", "Invalid Action",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

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
                refreshCurrentFilter();
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

    private void refreshCurrentFilter() {
        loadBatches(getSelectedStatusFilter());
    }

    private String getSelectedStatusFilter() {
        String filter = (String) filterComboBox.getSelectedItem();
        if (filter == null) {
            return "All";
        }
        return filter.replace(" Only", "");
    }

    private void updateActionButtons() {
        String filter = getSelectedStatusFilter();
        int selectedRow = batchTable.getSelectedRow();
        String currentStatus = selectedRow >= 0 ? String.valueOf(batchTable.getValueAt(selectedRow, 3)) : null;

        boolean canSample = selectedRow >= 0
                && "QUARANTINE".equalsIgnoreCase(currentStatus)
                && "QUARANTINE".equalsIgnoreCase(filter)
                && authService.hasPermission(currentUser, "UPDATE_QC_STATUS");

        boolean canApprove = selectedRow >= 0
                && "QI".equalsIgnoreCase(currentStatus)
                && "QI".equalsIgnoreCase(filter)
                && authService.hasPermission(currentUser, "RELEASE_BATCH");

        boolean canReject = selectedRow >= 0
                && "QI".equalsIgnoreCase(currentStatus)
                && "QI".equalsIgnoreCase(filter)
                && authService.hasPermission(currentUser, "REJECT_BATCH");

        sampleButton.setVisible("QUARANTINE".equalsIgnoreCase(filter));
        sampleButton.setEnabled(canSample);

        releaseButton.setVisible("QI".equalsIgnoreCase(filter));
        releaseButton.setEnabled(canApprove);

        rejectButton.setVisible("QI".equalsIgnoreCase(filter));
        rejectButton.setEnabled(canReject);

        genealogyButton.setEnabled(selectedRow >= 0);
    }
}
