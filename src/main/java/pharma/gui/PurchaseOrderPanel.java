package pharma.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import pharma.model.PurchaseOrder;
import pharma.model.User;
import pharma.service.AuthService;
import pharma.service.DatabaseService;
import java.awt.*;
import java.util.List;

public class PurchaseOrderPanel extends JPanel {
    private JTable ordersTable;
    private DefaultTableModel tableModel;
    private DatabaseService dbService;
    private JFrame mainFrame;

    private JButton createOrderBtn, viewDetailsBtn, receiveShipmentBtn, deleteOrderBtn, refreshBtn;

    public PurchaseOrderPanel(JFrame mainFrame, DatabaseService dbService, AuthService authService, User currentUser) {
        this.mainFrame = mainFrame;
        this.dbService = dbService;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Purchase Order Management"));

        // Top control panel with buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        createOrderBtn = new JButton("Create New Order");
        viewDetailsBtn = new JButton("View/Edit Order");
        receiveShipmentBtn = new JButton("Receive Shipment");
        deleteOrderBtn = new JButton("Delete Order");
        refreshBtn = new JButton("Refresh Orders");

        // Enforce specific action permissions
        createOrderBtn.setEnabled(authService.hasPermission(currentUser, "CREATE_PO"));
        viewDetailsBtn.setEnabled(
                authService.hasPermission(currentUser, "EDIT_PO") || authService.hasPermission(currentUser, "VIEW_PO"));
        receiveShipmentBtn.setEnabled(authService.hasPermission(currentUser, "RECEIVE_PO"));
        // Delete assumes Admin logic, we can tie to DELETE_PO or simply Admin
        // role/MANAGE_USERS since the prompt doesn't list DELETE_PO explicitly!
        // Actually, wait, Prompt didn't list DELETE_PO. We will tie it to Admin!
        deleteOrderBtn.setEnabled(authService.hasPermission(currentUser, "MANAGE_USERS"));

        controlPanel.add(createOrderBtn);
        controlPanel.add(viewDetailsBtn);
        controlPanel.add(receiveShipmentBtn);
        controlPanel.add(deleteOrderBtn);
        controlPanel.add(refreshBtn);

        add(controlPanel, BorderLayout.NORTH);

        // Table setup
        String[] columnNames = { "Order ID", "Supplier", "Order Date", "Expected Date", "Total Cost", "Status" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Table cells not editable directly
            }
        };
        ordersTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        add(scrollPane, BorderLayout.CENTER);

        // Load initial data
        if (authService.hasPermission(currentUser, "VIEW_PO")) {
            loadOrderData();
        }

        // Button event handlers
        refreshBtn.addActionListener(e -> loadOrderData());
        viewDetailsBtn.addActionListener(e -> handleViewEditOrder());
        receiveShipmentBtn.addActionListener(e -> handleReceiveShipment());
        deleteOrderBtn.addActionListener(e -> handleDeleteOrder());
        createOrderBtn.addActionListener(e -> openOrderCreationDialog());
    }

    void loadOrderData() {
        tableModel.setRowCount(0);
        try {
            List<PurchaseOrder> poList = dbService.getPurchaseOrders();
            if (poList != null) {
                for (PurchaseOrder po : poList) {
                    tableModel.addRow(new Object[] {
                            po.getId(),
                            po.getSupplierName(),
                            po.getOrderDate(),
                            po.getExpectedDate(),
                            po.getTotalAmount(),
                            po.getStatus()
                    });
                }
            }
        } catch (Exception ex) {
            System.err.println("Error loading orders: " + ex.getMessage());
        }
    }

    private void handleViewEditOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow >= 0) {
            // Get the actual order ID from the table (first column)
            int orderId = (int) tableModel.getValueAt(selectedRow, 0);
            // Open edit dialog passing orderId and reload after editing
            EditPurchaseOrderDialog editDialog = new EditPurchaseOrderDialog(mainFrame, this, orderId);
            editDialog.setVisible(true);
            if (editDialog.isSaved()) {
                loadOrderData();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select an order to view/edit.", "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleReceiveShipment() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow >= 0) {
            int orderId = (int) tableModel.getValueAt(selectedRow, 0);
            try {
                dbService.receivePurchaseOrderShipment(orderId);
                JOptionPane.showMessageDialog(this, "Shipment received for Order ID " + orderId, "Receive Shipment",
                        JOptionPane.INFORMATION_MESSAGE);
                loadOrderData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error receiving shipment: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select an order to receive shipment.", "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleDeleteOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow >= 0) {
            int orderId = (int) tableModel.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete Order ID " + orderId + "?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    dbService.deletePurchaseOrder(orderId);
                    JOptionPane.showMessageDialog(this, "Order deleted successfully.", "Delete Order",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadOrderData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error deleting order: " + ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select an order to delete.", "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openOrderCreationDialog() {
        // You can refer to your existing CreatePurchaseOrderDialog to implement this
        try {
            CreatePurchaseOrderDialog dialog = new CreatePurchaseOrderDialog(mainFrame, this);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadOrderData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error opening order creation dialog: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshPurchaseOrderList() {
        // Refresh the purchase order list by reloading data
        loadOrderData();
    }
}
