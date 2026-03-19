/*package pharma.gui;

import pharma.model.Material;
import pharma.service.DatabaseService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*; 

public class InventoryPanel extends JPanel {

    private JTable inventoryTable;
    private DefaultTableModel tableModel;
    private DatabaseService dbService;

    // FIX: Constructor now accepts DatabaseService
    public InventoryPanel(DatabaseService dbService) {
        this.dbService = dbService; 
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Current Inventory Stock Levels"));

        // --- Top Control Panel (Search and Filters) ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search by Name/Batch");
        JButton lowStockBtn = new JButton("View Low Stock");
        JButton expiredBtn = new JButton("View Expiring Soon");
        JButton refreshBtn = new JButton("Refresh");
        
        controlPanel.add(new JLabel("Search:"));
        controlPanel.add(searchField);
        controlPanel.add(searchBtn);
        controlPanel.add(lowStockBtn);
        controlPanel.add(expiredBtn);
        controlPanel.add(refreshBtn);

        add(controlPanel, BorderLayout.NORTH);

        // --- Center Table View ---
        String[] columnNames = {"Material Name", "Batch No.", "Quantity", "Unit Price", "Expiry Date", "Location Code", "Supplier"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        inventoryTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        add(scrollPane, BorderLayout.CENTER);

        // Load initial data 
        loadInventoryData();

        // --- Event Handling (Placeholders) ---
        refreshBtn.addActionListener(_ -> loadInventoryData());
        searchBtn.addActionListener(_ -> filterInventory(searchField.getText()));
        lowStockBtn.addActionListener(_ -> filterLowStock());
        expiredBtn.addActionListener(_ -> filterExpired());
    }

    private void loadInventoryData() {
        tableModel.setRowCount(0); 
        
        try {
            // FIX: Calls the injected dbService
            List<Material> inventoryList = dbService.getFullInventoryReport();
            
            // Simulated data for display structure confirmation:
            tableModel.addRow(new Object[]{"Paracetamol 500mg", "PT500-A22", 450, 1.50, "2025-11-30", "A1", "Acme Pharma Inc."});
            tableModel.addRow(new Object[]{"Amoxicillin 250mg", "AX250-B01", 50, 5.25, "2024-12-15", "B3", "Global Meds Ltd."}); 
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading inventory data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private void filterInventory(String query) {
        JOptionPane.showMessageDialog(this, "Filtering Inventory by query: " + query, "Search", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void filterLowStock() {
        JOptionPane.showMessageDialog(this, "Displaying items below minimum stock level.", "Low Stock", JOptionPane.INFORMATION_MESSAGE);
    }

    private void filterExpired() {
        JOptionPane.showMessageDialog(this, "Displaying items expiring soon.", "Expiring Soon", JOptionPane.INFORMATION_MESSAGE);
    }
}*/

package pharma.gui;

import javax.swing.*;
import pharma.model.Material;
import pharma.model.Stock;
import pharma.service.DatabaseService;
import pharma.gui.components.MaterialSearchField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryPanel extends JPanel {

    private DatabaseService dbService;
    private JTable inventoryTable;
    private InventoryTableModel tableModel;
    private MaterialSearchField searchField;
    private TableRowSorter<InventoryTableModel> sorter;

    // The constructor sets up the panel and immediately tries to load the data.
    public InventoryPanel(DatabaseService dbService) {
        this.dbService = dbService;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Inventory Management: Material Stock & Locations"));

        // 1. Initialize the Table Model
        this.tableModel = new InventoryTableModel();
        this.inventoryTable = new JTable(this.tableModel);

        // Add table sorter for filtering
        sorter = new TableRowSorter<>(tableModel);
        inventoryTable.setRowSorter(sorter);

        // 2. Add table to a scroll pane and to the panel
        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Control Panel with Search
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Search Field with Autocomplete
        JLabel searchLabel = new JLabel("Search:");
        searchField = new MaterialSearchField(dbService.getFullInventoryReport());
        searchField.setPreferredSize(new Dimension(300, 25));
        searchField.setSelectionListener(material -> {
            // When a material is selected from dropdown, filter the table
            filterTableByDrug(material);
        });

        JButton clearSearchBtn = new JButton("Clear Search");
        clearSearchBtn.addActionListener(_ -> {
            searchField.clearSelection();
            sorter.setRowFilter(null); // Show all rows
        });

        JButton refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(_ -> loadInventoryData());

        buttonPanel.add(searchLabel);
        buttonPanel.add(searchField);
        buttonPanel.add(clearSearchBtn);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.NORTH);

        // 4. Load data immediately after setup
        loadInventoryData();
    }

    /**
     * FIX: Loads the inventory data (List<Material>) from the DatabaseService
     * and updates the table model.
     */
    public void loadInventoryData() {
        try {
            List<Stock> stocks = dbService.getDetailedInventoryReport();
            tableModel.setStocks(stocks);
            searchField.setDrugList(dbService.getFullInventoryReport());

            System.out.println("Inventory data successfully loaded: " + stocks.size() + " records.");

        } catch (Exception e) {
            // This catches any unexpected errors (though DatabaseService handles SQL
            // exceptions)
            JOptionPane.showMessageDialog(this,
                    "Error loading inventory data: " + e.getMessage(),
                    "Loading Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Filter table to show only the selected material
     */
    private void filterTableByDrug(Material material) {
        if (material == null) {
            sorter.setRowFilter(null);
            return;
        }

        // Filter to show only rows matching the selected material's material code
        RowFilter<InventoryTableModel, Object> filter = RowFilter.regexFilter(
                "^" + material.getMaterialCode() + "$", 0); // Column 0 is Material Code
        sorter.setRowFilter(filter);
    }

    // ======================================================
    // Private Inner Class for the Table Model
    // ======================================================
    private class InventoryTableModel extends AbstractTableModel {
        private List<Stock> stocks;
        private final String[] COLUMN_NAMES = { "Material Code", "Brand Name", "Batch No.", "Qty", "Reserved Qty",
                "Available Qty", "Exp Date", "QC Status", "Batch Source", "Location" };

        public InventoryTableModel() {
            this.stocks = new ArrayList<>();
        }

        public void setStocks(List<Stock> newStocks) {
            this.stocks = newStocks;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return stocks.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Stock stock = stocks.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return stock.getMaterialCode();
                case 1:
                    return stock.getBrandName();
                case 2:
                    return stock.getBatchNumber();
                case 3:
                    return stock.getQuantity();
                case 4:
                    return stock.getReservedQuantity();
                case 5:
                    return stock.getAvailableQuantity();
                case 6:
                    return stock.getExpDate();
                case 7:
                    return stock.getQcStatus();
                case 8:
                    return stock.getParentBatchId();
                case 9:
                    return stock.getLocationCode();
                default:
                    return null;
            }
        }
    }
}
