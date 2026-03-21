package pharma.gui;

import pharma.model.Location;
import pharma.service.DatabaseService; 

import javax.swing.*;
import java.awt.*;

/**
 * Reusable dialog for creating and editing Location records.
 */
public class LocationDialog extends JDialog {

    private LocationsPanel parentPanel;
    private Location locationToEdit; // Holds null for Add, or the object for Update
    
    private JTextField codeField;
    private JTextField nameField;
    private JTextField descriptionField;
    private JTextField capacityField;
    private JButton saveButton;

    /**
     * @param owner The main application frame (for proper dialog centering).
     * @param parent The LocationsPanel instance (to call refresh after save).
     * @param location The Location object to populate/edit, or null to create a new one.
     */
    public LocationDialog(JFrame owner, LocationsPanel parent, Location location) { // <-- This is the CORRECT signature
        super(owner, (location == null ? "Add New Location" : "Update Location: " + location.getLocationCode()), true);
        this.parentPanel = parent;
        this.locationToEdit = location;
        
        setLayout(new BorderLayout(10, 10));
        
        // --- Input Panel ---
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 1. Location Code
        inputPanel.add(new JLabel("Location Code:"));
        codeField = new JTextField(15);
        inputPanel.add(codeField);
        
        // 2. Location Name
        inputPanel.add(new JLabel("Location Name:"));
        nameField = new JTextField(15);
        inputPanel.add(nameField);
        
        // 3. Description
        inputPanel.add(new JLabel("Description:"));
        descriptionField = new JTextField(15);
        inputPanel.add(descriptionField);

        // 4. Capacity
        inputPanel.add(new JLabel("Capacity (Numeric):"));
        capacityField = new JTextField(15);
        inputPanel.add(capacityField);

        // Populate fields if in Edit mode
        if (locationToEdit != null) {
            codeField.setText(locationToEdit.getLocationCode());
            codeField.setEditable(false); // Cannot change PK on update
            codeField.setBackground(Color.LIGHT_GRAY);
            nameField.setText(locationToEdit.getLocationName());
            descriptionField.setText(locationToEdit.getDescription());
            capacityField.setText(String.valueOf(locationToEdit.getCapacity()));
        }
        
        add(inputPanel, BorderLayout.CENTER);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton(locationToEdit == null ? "ADD" : "SAVE");
        JButton cancelButton = new JButton("CANCEL");

        saveButton.addActionListener(e -> saveLocation());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }
    
    private void saveLocation() {
        try {
            // 1. Input Validation and Parsing
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String description = descriptionField.getText().trim();
            String capacityStr = capacityField.getText().trim();

            if (code.isEmpty() || name.isEmpty() || capacityStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Location Code, Name, and Capacity are mandatory.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int capacity;
            try {
                capacity = Integer.parseInt(capacityStr);
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Capacity must be a valid whole number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 2. Database Operation
            DatabaseService dbService = DatabaseService.getInstance();
            boolean success;
            
            // Check if we are in UPDATE or ADD mode
            if (locationToEdit != null) {
                // UPDATE Mode
                success = dbService.updateLocation(code, name, description, capacity); 
            } else {
                // ADD Mode
                success = dbService.addLocation(code, name, description, capacity); 
            }
            
            // 3. Post-Operation and Refresh
            if (success) {
                String action = (locationToEdit != null ? "updated" : "added");
                JOptionPane.showMessageDialog(this, "Location " + action + " successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                parentPanel.refreshLocationList(); 
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Database operation failed. Check for duplicate code or other database errors.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            // Catches any exceptions from the DatabaseService layer (e.g., SQLException)
            JOptionPane.showMessageDialog(this, "A critical database error occurred: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
