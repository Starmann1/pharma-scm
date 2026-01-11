/*package pharma.gui;

import pharma.model.Drug;
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
        String[] columnNames = {"Drug Name", "Batch No.", "Quantity", "Unit Price", "Expiry Date", "Location Code", "Supplier"};
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
            List<Drug> inventoryList = dbService.getFullInventoryReport();
            
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
import pharma.model.Drug;
import pharma.service.DatabaseService;
import pharma.gui.components.DrugSearchField;
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
    private DrugSearchField searchField;
    private TableRowSorter<InventoryTableModel> sorter;

    // The constructor sets up the panel and immediately tries to load the data.
    public InventoryPanel(DatabaseService dbService) {
        this.dbService = dbService;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Inventory Management: Drug Stock & Locations"));

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
        searchField = new DrugSearchField(dbService.getFullInventoryReport());
        searchField.setPreferredSize(new Dimension(300, 25));
        searchField.setSelectionListener(drug -> {
            // When a drug is selected from dropdown, filter the table
            filterTableByDrug(drug);
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
     * FIX: Loads the inventory data (List<Drug>) from the DatabaseService
     * and updates the table model.
     */
    public void loadInventoryData() {
        try {
            // The method is now correctly implemented in DatabaseService to return a
            // List<Drug>
            List<Drug> drugs = dbService.getFullInventoryReport();

            // Update the table model with the fetched list
            tableModel.setDrugs(drugs);

            // Update search field with latest drug list
            searchField.setDrugList(drugs);

            System.out.println("Inventory data successfully loaded: " + drugs.size() + " records.");

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
     * Filter table to show only the selected drug
     */
    private void filterTableByDrug(Drug drug) {
        if (drug == null) {
            sorter.setRowFilter(null);
            return;
        }

        // Filter to show only rows matching the selected drug's material code
        RowFilter<InventoryTableModel, Object> filter = RowFilter.regexFilter(
                "^" + drug.getMaterialCode() + "$", 0); // Column 0 is Material Code
        sorter.setRowFilter(filter);
    }

    // ======================================================
    // Private Inner Class for the Table Model
    // ======================================================
    private class InventoryTableModel extends AbstractTableModel {
        private List<Drug> drugs;
        private final String[] COLUMN_NAMES = { "Code", "Brand Name", "Generic Name", "Manufacturer", "Reorder Level" };

        public InventoryTableModel() {
            this.drugs = new ArrayList<>();
        }

        // Method used to update the table data after fetching from the DB
        public void setDrugs(List<Drug> newDrugs) {
            this.drugs = newDrugs;
            fireTableDataChanged(); // Notifies the JTable to redraw
        }

        @Override
        public int getRowCount() {
            return drugs.size();
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
            Drug drug = drugs.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return drug.getMaterialCode();
                case 1:
                    return drug.getBrandName();
                case 2:
                    return drug.getGenericName();
                case 3:
                    return drug.getManufacturer();
                case 4:
                    return drug.getReorderLevel();
                default:
                    return null;
            }
        }
    }
}
