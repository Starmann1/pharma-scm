/*
package pharma.gui;

import pharma.model.Location; // Assuming this model class exists
import pharma.service.DatabaseService; // Assuming this service exists

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class LocationsPanel extends JPanel {

    private JTable locationsTable;
    private DefaultTableModel tableModel;
    private DatabaseService dbService = new DatabaseService();

    public LocationsPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Location Management (Shelves/Aisles)"));

        // --- Top Control Panel ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        JButton addBtn = new JButton("Add Location");
        JButton updateBtn = new JButton("Update Location");
        JButton deleteBtn = new JButton("Delete Location");
        JButton refreshBtn = new JButton("Refresh");
        
        controlPanel.add(addBtn);
        controlPanel.add(updateBtn);
        controlPanel.add(deleteBtn);
        controlPanel.add(refreshBtn);

        add(controlPanel, BorderLayout.NORTH);

        // --- Center Table View ---
        String[] columnNames = {"ID", "Name (e.g., Aisle A)", "Description"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        locationsTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(locationsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Load data on initialization
        loadLocationData();

        // --- Event Handling (CRUD Logic) ---
        refreshBtn.addActionListener(_ -> loadLocationData());
        addBtn.addActionListener(_ -> openLocationDialog(null));
        updateBtn.addActionListener(_ -> handleUpdateLocation());
        deleteBtn.addActionListener(_ -> handleDeleteLocation());
    }

    private void loadLocationData() {
        tableModel.setRowCount(0); 
        try {
            // Assuming getLocations() returns List<Location>
            List<Location> locations = dbService.getLocations();
            for (Location location : locations) {
                tableModel.addRow(new Object[]{
                    location.getLocationCode(),
                    location.getLocationName(),
                    location.getArea() // Assuming Area field stores description
                });
            }
        } catch (SQLException ex) {
             // Fallback for missing DBService or for simulation
            tableModel.addRow(new Object[]{1, "A1", "Main Shelf - Near Counter"});
            tableModel.addRow(new Object[]{2, "B2", "Refrigerated Goods"});
        }
    }
    
    private void openLocationDialog(Location locationToEdit) {
        // Simplified dialog logic...
        String action = (locationToEdit == null) ? "Add" : "Update";
        JOptionPane.showMessageDialog(this, "Opening form to " + action + " Location.", action + " Location", JOptionPane.INFORMATION_MESSAGE);
        // On successful operation, call loadLocationData();
    }
    
    private void handleUpdateLocation() {
        int selectedRow = locationsTable.getSelectedRow();
        if (selectedRow >= 0) {
            int locationId = (int) tableModel.getValueAt(selectedRow, 0);
            try {
                Location selectedLocation = dbService.getLocationById(locationId); // Assumes getLocationById exists
                openLocationDialog(selectedLocation);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error fetching location details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a location to update.", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void handleDeleteLocation() {
        int selectedRow = locationsTable.getSelectedRow();
        if (selectedRow >= 0) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this location? All linked materials will lose their location data.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    int locationId = (int) tableModel.getValueAt(selectedRow, 0);
                    dbService.deleteLocation(locationId); // Assumes deleteLocation(int) exists
                    JOptionPane.showMessageDialog(this, "Location deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadLocationData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error deleting location: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a location to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }
}
*/
package pharma.gui;

import pharma.model.Location; 
import pharma.service.DatabaseService; 

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class LocationsPanel extends JPanel {

    private JTable locationsTable;
    private DefaultTableModel tableModel;
    private DatabaseService dbService; 
    private JFrame mainFrame;

    // FIX 1: Using consistent constructor signature (JFrame, DatabaseService) 
    // to match how GUI panels are typically initialized by the main frame.
    public LocationsPanel(JFrame mainFrame, DatabaseService dbService) { 
        this.mainFrame = mainFrame;
        this.dbService = dbService;
        
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Location Management (Shelves/Aisles)"));

        // --- Top Control Panel ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        JButton addBtn = new JButton("Add Location");
        JButton updateBtn = new JButton("Update Location");
        JButton deleteBtn = new JButton("Delete Location");
        JButton refreshBtn = new JButton("Refresh");
        
        controlPanel.add(addBtn);
        controlPanel.add(updateBtn);
        controlPanel.add(deleteBtn);
        controlPanel.add(refreshBtn);

        add(controlPanel, BorderLayout.NORTH);

        // --- Center Table View ---
        String[] columnNames = {"Location Code", "Name", "Description", "Capacity"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3) return Integer.class; 
                return columnIndex == 0 ? String.class : super.getColumnClass(columnIndex);
            }
        };
        locationsTable = new JTable(tableModel);
        locationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Enable selection for CRUD
        JScrollPane scrollPane = new JScrollPane(locationsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Load data on initialization
        loadLocationData();

        // --- Event Handling (CRUD Logic) ---
        refreshBtn.addActionListener(_ -> loadLocationData());
        
        // FIX 2: Correctly calling the common dialog entry point for ADDITION.
        addBtn.addActionListener(_ -> openLocationDialog(null)); 
        
        updateBtn.addActionListener(_ -> {
            try {
                handleUpdateLocation();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }); 
        deleteBtn.addActionListener(_ -> handleDeleteLocation()); 
    }

    /**
     * Entry point for showing the Add/Edit dialog. Solves the compilation error.
     * @param locationToEdit The Location object to edit, or null for Add mode.
     */
    private void openLocationDialog(Location locationToEdit) {
        // FIX 3: Call the constructor of LocationDialog with the three correct arguments:
        // (JFrame owner, LocationsPanel parent, Location locationToEdit)
        LocationDialog dialog = new LocationDialog(this.mainFrame, this, locationToEdit);
        dialog.setVisible(true);
    }

    /**
     * Loads all location data from the database and populates the table.
     */
    private void loadLocationData() {
        tableModel.setRowCount(0); 
        try {
            List<Location> locations = dbService.getLocations(); 
            for (Location location : locations) {
                tableModel.addRow(new Object[]{
                    location.getLocationCode(), 
                    location.getLocationName(),
                    location.getDescription(),
                    location.getCapacity() 
                });
            }
        } catch (Exception ex) { 
            JOptionPane.showMessageDialog(this, "Error loading location data: " + ex.getMessage() + ". Check DatabaseService connection.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Public method to refresh the location list, typically called by the dialog after save/update/delete.
     */
    public void refreshLocationList() {
        loadLocationData();
    }
    
    private void handleUpdateLocation() throws SQLException {
         int selectedRow = locationsTable.getSelectedRow();
         if (selectedRow >= 0) {
             String locationCode = (String) tableModel.getValueAt(selectedRow, 0);
             Location selectedLocation = dbService.getLocationById(locationCode); 
             
             if (selectedLocation != null) {
                 openLocationDialog(selectedLocation); // Use the common method for edit
             } else {
                 JOptionPane.showMessageDialog(this, "Error: Location not found in database.", "Database Error", JOptionPane.ERROR_MESSAGE);
             }
         } else {
             JOptionPane.showMessageDialog(this, "Please select a location to update.", "No Selection", JOptionPane.WARNING_MESSAGE);
         }
    }
    
    private void handleDeleteLocation() {
        int selectedRow = locationsTable.getSelectedRow();
        if (selectedRow >= 0) {
            String locationCode = (String) tableModel.getValueAt(selectedRow, 0);
            
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete location " + locationCode + "? This will affect linked inventory!", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                if (dbService.deleteLocation(locationCode)) { 
                    JOptionPane.showMessageDialog(this, "Location deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadLocationData();
                } else {
                    JOptionPane.showMessageDialog(this, "Error deleting location. It may be linked to existing inventory records.", "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a location to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }
}
