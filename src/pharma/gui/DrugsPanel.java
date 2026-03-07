/*package pharma.gui;

import pharma.model.Drug;
import pharma.service.DatabaseService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class DrugsPanel extends JPanel {
    
    private JTable drugsTable;
    private DefaultTableModel tableModel;
    private DatabaseService dbService;
    private JFrame mainFrame; // ⬅️ FIX 1: ADDED Field to hold main JFrame reference

    // FIX 2: Updated Constructor Signature (must match call in InventoryGUI)
    public DrugsPanel(JFrame mainFrame, DatabaseService dbService) { 
        this.mainFrame = mainFrame; // ⬅️ STORE the JFrame reference
        this.dbService = dbService; // Use the injected dbService
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Drug Management"));

        // Table Setup
        tableModel = new DefaultTableModel(new Object[]{"Material Code", "Brand Name", "Generic Name", "Manufacturer", "Formulation", "Strength", "Reorder Level", "Preferred Supplier ID"}, 0);
        drugsTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(drugsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel();
        JButton addButton = new JButton("Add Drug");
        JButton editButton = new JButton("Edit Drug");
        JButton deleteButton = new JButton("Delete Drug");

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Calls the fixed dialog method
                showAddDrugDialog();
            }
        });

        editButton.addActionListener(e -> {
            int selectedRow = drugsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(DrugsPanel.this, "Please select a drug to edit.", "Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Material Code is at column 0
            String materialCode = (String) drugsTable.getValueAt(selectedRow, 0);
            
            // Fetch the full Drug object for pre-populating the form
            Drug drugToEdit = dbService.getDrugByMaterialCode(materialCode);
            
            if (drugToEdit != null) {
                showDrugFormDialog(drugToEdit); // Pass the existing drug object for EDIT mode
            } else {
                 JOptionPane.showMessageDialog(DrugsPanel.this, "Could not fetch drug data from the database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // To be implemented (Delete functionality)
                JOptionPane.showMessageDialog(DrugsPanel.this, "Delete functionality not implemented yet.", "Not Implemented", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        controlPanel.add(addButton);
        controlPanel.add(editButton);
        controlPanel.add(deleteButton);
        add(controlPanel, BorderLayout.NORTH); // Changed to NORTH for better layout flow

        loadDrugs(); // Load initial data
    }


    private void loadDrugs() {
        tableModel.setRowCount(0); // Clear existing data
        
        try {
            // FIX for Problem 5: Ensure dbService.getAllDrugs() is implemented and returns data
            List<Drug> drugs = dbService.getAllDrugs(); 
            if (drugs != null) {
                for (Drug drug : drugs) {
                    tableModel.addRow(new Object[]{
                            drug.getMaterialCode(),
                            drug.getBrandName(),
                            drug.getGenericName(),
                            drug.getManufacturer(),
                            drug.getFormulation(),
                            drug.getStrength(),
                            drug.getReorderLevel(),
                            drug.getPreferredSupplierId()
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading drugs: " + e.getMessage());
            // Add fallback data here if needed, or just let the table stay empty on DB failure.
        }
    }

    private void showAddDrugDialog() {
        // Use the stored mainFrame and pass THIS panel instance for refresh callback
        JDialog dialog = new AddDrugDialog(this.mainFrame, this); 
        dialog.setVisible(true);
        // NOTE: The dialog itself will call refreshDrugList() on success.
    }
    
    
    public void refreshDrugList() {
        loadDrugs();
    }


    private class AddDrugDialog extends JDialog {
        
        private DrugsPanel parentPanel; // ⬅️ Added reference to parent panel
        
        private JTextField materialCodeField;
        private JTextField brandNameField;
        private JTextField genericNameField;
        private JTextField manufacturerField;
        private JTextField formulationField;
        private JTextField strengthField;
        private JTextField scheduleCategoryField;
        private JTextField storageConditionsField;
        private JTextField reorderLevelField;
        private JCheckBox isActiveCheckBox;
        private JTextField preferredSupplierIdField;

        // Updated constructor signature to take the parent DrugsPanel
        public AddDrugDialog(JFrame owner, DrugsPanel parent) {
            super(owner, "Add New Drug", true);
            this.parentPanel = parent; // ⬅️ Store parent reference
            
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int y = 0;
            // ... (All field definitions and adds remain the same) ...

            // 1. Material Code
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Material Code:"), gbc);
            gbc.gridx = 1;
            materialCodeField = new JTextField(20);
            add(materialCodeField, gbc);

            // 2. Brand Name
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Brand Name:"), gbc);
            gbc.gridx = 1;
            brandNameField = new JTextField(20);
            add(brandNameField, gbc);

            // 3. Generic Name
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Generic Name:"), gbc);
            gbc.gridx = 1;
            genericNameField = new JTextField(20);
            add(genericNameField, gbc);

            // 4. Manufacturer
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Manufacturer:"), gbc);
            gbc.gridx = 1;
            manufacturerField = new JTextField(20);
            add(manufacturerField, gbc);

            // 5. Formulation
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Formulation:"), gbc);
            gbc.gridx = 1;
            formulationField = new JTextField(20);
            add(formulationField, gbc);

            // 6. Strength
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Strength:"), gbc);
            gbc.gridx = 1;
            strengthField = new JTextField(20);
            add(strengthField, gbc);

            // 7. Schedule Category
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Schedule Category:"), gbc);
            gbc.gridx = 1;
            scheduleCategoryField = new JTextField(20);
            add(scheduleCategoryField, gbc);

            // 8. Storage Conditions
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Storage Conditions:"), gbc);
            gbc.gridx = 1;
            storageConditionsField = new JTextField(20);
            add(storageConditionsField, gbc);

            // 9. Reorder Level
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Reorder Level:"), gbc);
            gbc.gridx = 1;
            reorderLevelField = new JTextField(20);
            add(reorderLevelField, gbc);

            // 10. Is Active
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Is Active:"), gbc);
            gbc.gridx = 1;
            isActiveCheckBox = new JCheckBox();
            isActiveCheckBox.setSelected(true);
            add(isActiveCheckBox, gbc);

            // 11. Preferred Supplier ID
            gbc.gridx = 0; gbc.gridy = y++;
            add(new JLabel("Preferred Supplier ID:"), gbc);
            gbc.gridx = 1;
            preferredSupplierIdField = new JTextField(20);
            add(preferredSupplierIdField, gbc);
            
            // --- Buttons (Problem 4 fix is confirmed to be present) ---
            JPanel buttonPanel = new JPanel();
            JButton saveButton = new JButton("OK"); // Renamed to "OK" to match request style
            JButton cancelButton = new JButton("CANCEL"); // Renamed to "CANCEL"

            saveButton.addActionListener(_ -> {
                // Delegation to save logic
                saveDrug(); 
            });

            cancelButton.addActionListener(_ -> dispose()); // Cancel button logic

            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            gbc.gridx = 0;
            gbc.gridy = y++;
            gbc.gridwidth = 2;
            add(buttonPanel, gbc);

            pack();
            setLocationRelativeTo(owner);
        }

        private void saveDrug() {
             // Logic moved from anonymous class to method for cleaner structure
            try {
                String materialCode = materialCodeField.getText().trim();
                String brandName = brandNameField.getText().trim();
                String genericName = genericNameField.getText().trim();
                String manufacturer = manufacturerField.getText().trim();
                String formulation = formulationField.getText().trim();
                String strength = strengthField.getText().trim();
                String scheduleCategory = scheduleCategoryField.getText().trim();
                String storageConditions = storageConditionsField.getText().trim();
                String reorderLevelStr = reorderLevelField.getText().trim();
                boolean isActive = isActiveCheckBox.isSelected();
                String preferredSupplierIdStr = preferredSupplierIdField.getText().trim();

                // Basic validation
                if (materialCode.isEmpty() || brandName.isEmpty() || genericName.isEmpty() || reorderLevelStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill in all mandatory fields (Material Code, Brand Name, Generic Name, Reorder Level).", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int reorderLevel;
                try {
                    reorderLevel = Integer.parseInt(reorderLevelStr);
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Reorder Level must be a valid integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Integer preferredSupplierId = null;
                if (!preferredSupplierIdStr.isEmpty()) {
                    try {
                        preferredSupplierId = Integer.parseInt(preferredSupplierIdStr);
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(this, "Preferred Supplier ID must be a valid integer or left blank.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                Drug newDrug = new Drug();
                newDrug.setMaterialCode(materialCode);
                newDrug.setBrandName(brandName);
                newDrug.setGenericName(genericName);
                newDrug.setManufacturer(manufacturer);
                newDrug.setFormulation(formulation);
                newDrug.setStrength(strength);
                newDrug.setScheduleCategory(scheduleCategory);
                newDrug.setStorageConditions(storageConditions);
                newDrug.setReorderLevel(reorderLevel);
                newDrug.setActive(isActive);
                newDrug.setPreferredSupplierId(preferredSupplierId);

                // Assuming dbService is accessed via the parentPanel field now
                boolean success = parentPanel.dbService.addDrug(newDrug); 

                if (success) {
                    JOptionPane.showMessageDialog(this, "Drug added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    // FIX 6: Call the parent panel's refresh method (Problem 5)
                    parentPanel.refreshDrugList(); 
                    dispose();
                } else {
                     JOptionPane.showMessageDialog(this, "Failed to add drug. Database reported failure.", "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding drug: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}*/

package pharma.gui;

import pharma.model.Drug;
import pharma.service.DatabaseService;
import pharma.gui.components.DrugSearchField;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.util.List;

public class DrugsPanel extends JPanel {

    private JTable drugsTable;
    private DefaultTableModel tableModel;
    private DatabaseService dbService;
    private JFrame mainFrame;
    private DrugSearchField searchField;
    private TableRowSorter<DefaultTableModel> sorter;

    public DrugsPanel(JFrame mainFrame, DatabaseService dbService) {
        this.mainFrame = mainFrame;
        this.dbService = dbService;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Drug Management"));

        // Table Setup
        // NOTE: Material Code is the primary key (PK)
        tableModel = new DefaultTableModel(new Object[] { "Material Code", "Brand Name", "Generic Name", "Manufacturer",
                "Formulation", "Strength", "Reorder Level", "Preferred Supplier ID", "Material Type", "UOM" }, 0) {
            // Override isCellEditable to make the table non-editable
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        drugsTable = new JTable(tableModel);
        drugsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Important for edit/delete

        // Add table sorter for filtering
        sorter = new TableRowSorter<>(tableModel);
        drugsTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(drugsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Control Panel with Search
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Search Field with Autocomplete
        JLabel searchLabel = new JLabel("Search:");
        searchField = new DrugSearchField(dbService.getAllDrugs());
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

        JButton addButton = new JButton("Add Drug");
        JButton editButton = new JButton("Edit Drug");
        JButton deleteButton = new JButton("Delete Drug");
        JButton refreshButton = new JButton("Refresh Data");

        addButton.addActionListener(_ -> showDrugFormDialog(null)); // Pass null for ADD mode

        // --- EDIT BUTTON IMPLEMENTATION ---
        editButton.addActionListener(_ -> {
            int selectedRow = drugsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(DrugsPanel.this, "Please select a drug to edit.", "Selection Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Convert view row to model row (important when using sorter/filter)
            int modelRow = drugsTable.convertRowIndexToModel(selectedRow);
            String materialCode = (String) tableModel.getValueAt(modelRow, 0);

            // Fetch the full Drug object for pre-populating the form
            Drug drugToEdit = dbService.getDrugByMaterialCode(materialCode);

            if (drugToEdit != null) {
                showDrugFormDialog(drugToEdit); // Pass the existing drug object for EDIT mode
            } else {
                JOptionPane.showMessageDialog(DrugsPanel.this, "Could not fetch drug data from the database.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // --- DELETE BUTTON IMPLEMENTATION ---
        deleteButton.addActionListener(_ -> {
            int selectedRow = drugsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(DrugsPanel.this, "Please select a drug to delete.", "Selection Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int modelRow = drugsTable.convertRowIndexToModel(selectedRow);
            String materialCode = (String) tableModel.getValueAt(modelRow, 0);
            String brandName = (String) tableModel.getValueAt(modelRow, 1);

            int confirm = JOptionPane.showConfirmDialog(
                    DrugsPanel.this,
                    "Are you sure you want to delete the drug: " + brandName + " (" + materialCode + ")?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (dbService.deleteDrug(materialCode)) {
                    JOptionPane.showMessageDialog(DrugsPanel.this, "Drug deleted successfully.", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    refreshDrugList(); // Refresh the table
                } else {
                    JOptionPane.showMessageDialog(DrugsPanel.this,
                            "Failed to delete drug. It might be referenced by other records.", "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // --- REFRESH BUTTON IMPLEMENTATION ---
        refreshButton.addActionListener(_ -> {
            refreshDrugList();
            JOptionPane.showMessageDialog(DrugsPanel.this, "Drug list refreshed.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        // Add components to Control Panel
        controlPanel.add(searchLabel);
        controlPanel.add(searchField);
        controlPanel.add(clearSearchBtn);
        controlPanel.add(new JSeparator(SwingConstants.VERTICAL));
        controlPanel.add(addButton);
        controlPanel.add(editButton);
        controlPanel.add(deleteButton);
        controlPanel.add(refreshButton);

        // Add the Control Panel to the DrugsPanel
        add(controlPanel, BorderLayout.NORTH);

        loadDrugs(); // Load initial data
    } // <--- Constructor correctly ends here.

    private void loadDrugs() {
        tableModel.setRowCount(0); // Clear existing data

        try {
            List<Drug> drugs = dbService.getAllDrugs();
            if (drugs != null) {
                for (Drug drug : drugs) {
                    tableModel.addRow(new Object[] {
                            drug.getMaterialCode(),
                            drug.getBrandName(),
                            drug.getGenericName(),
                            drug.getManufacturer(),
                            drug.getFormulation(),
                            drug.getStrength(),
                            drug.getReorderLevel(),
                            drug.getPreferredSupplierId(),
                            drug.getMaterialType(),
                            drug.getUnitOfMeasure()
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading drugs: " + e.getMessage());
        }
    }

    /**
     * Handles opening the dialog in either Add or Edit mode.
     */
    private void showDrugFormDialog(Drug drugToEdit) {
        // Use the stored mainFrame and pass THIS panel instance for refresh callback
        JDialog dialog = new DrugFormDialog(this.mainFrame, this, drugToEdit);
        dialog.setVisible(true);
    }

    /**
     * Public callback for the dialog to refresh the table on success.
     */
    public void refreshDrugList() {
        loadDrugs();
        // Update search field with latest drug list
        searchField.setDrugList(dbService.getAllDrugs());
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
        RowFilter<DefaultTableModel, Object> filter = RowFilter.regexFilter(
                "^" + drug.getMaterialCode() + "$", 0); // Column 0 is Material Code
        sorter.setRowFilter(filter);
    }

    /**
     * Refactored and renamed from AddDrugDialog to serve as a generic form for Add
     * and Edit.
     */
    private class DrugFormDialog extends JDialog {

        private DrugsPanel parentPanel;
        private Drug drugToEdit; // Holds the drug object if in EDIT mode (or null for ADD mode)

        private JTextField materialCodeField;
        private JTextField brandNameField;
        private JTextField genericNameField;
        private JTextField manufacturerField;
        private JTextField formulationField;
        private JTextField strengthField;
        private JTextField scheduleCategoryField;
        private JTextField storageConditionsField;
        private JTextField reorderLevelField;
        private JCheckBox isActiveCheckBox;
        private JTextField preferredSupplierIdField;
        private JComboBox<String> materialTypeComboBox;
        private JTextField unitOfMeasureField;

        // Updated constructor signature to handle both modes
        public DrugFormDialog(JFrame owner, DrugsPanel parent, Drug drug) {
            super(owner, drug == null ? "Add New Drug" : "Edit Drug: " + drug.getMaterialCode(), true);
            this.parentPanel = parent;
            this.drugToEdit = drug;

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int y = 0;

            // 1. Material Code (Editable only for Add, Disabled/Read-only for Edit)
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Material Code:"), gbc);
            gbc.gridx = 1;
            materialCodeField = new JTextField(20);
            if (drugToEdit != null) {
                materialCodeField.setText(drugToEdit.getMaterialCode());
                materialCodeField.setEditable(false); // PK cannot be changed in edit mode
                materialCodeField.setBackground(Color.LIGHT_GRAY);
            }
            add(materialCodeField, gbc);

            // 2. Brand Name
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Brand Name:"), gbc);
            gbc.gridx = 1;
            brandNameField = new JTextField(20);
            add(brandNameField, gbc);

            // 3. Generic Name
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Generic Name:"), gbc);
            gbc.gridx = 1;
            genericNameField = new JTextField(20);
            add(genericNameField, gbc);

            // 4. Manufacturer
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Manufacturer:"), gbc);
            gbc.gridx = 1;
            manufacturerField = new JTextField(20);
            add(manufacturerField, gbc);

            // 5. Formulation
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Formulation:"), gbc);
            gbc.gridx = 1;
            formulationField = new JTextField(20);
            add(formulationField, gbc);

            // 6. Strength
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Strength:"), gbc);
            gbc.gridx = 1;
            strengthField = new JTextField(20);
            add(strengthField, gbc);

            // 7. Schedule Category
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Schedule Category:"), gbc);
            gbc.gridx = 1;
            scheduleCategoryField = new JTextField(20);
            add(scheduleCategoryField, gbc);

            // 8. Storage Conditions
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Storage Conditions:"), gbc);
            gbc.gridx = 1;
            storageConditionsField = new JTextField(20);
            add(storageConditionsField, gbc);

            // 9. Reorder Level
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Reorder Level:"), gbc);
            gbc.gridx = 1;
            reorderLevelField = new JTextField(20);
            add(reorderLevelField, gbc);

            // 10. Is Active
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Is Active:"), gbc);
            gbc.gridx = 1;
            isActiveCheckBox = new JCheckBox();
            isActiveCheckBox.setSelected(true);
            add(isActiveCheckBox, gbc);

            // 11. Preferred Supplier ID
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Preferred Supplier ID:"), gbc);
            gbc.gridx = 1;
            preferredSupplierIdField = new JTextField(20);
            add(preferredSupplierIdField, gbc);

            // 12. Material Type
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Material Type:"), gbc);
            gbc.gridx = 1;
            materialTypeComboBox = new JComboBox<>(
                    new String[] { "RAW_MATERIAL", "PACKAGING", "INTERMEDIATE", "FINISHED_GOOD" });
            add(materialTypeComboBox, gbc);

            // 13. Unit of Measure
            gbc.gridx = 0;
            gbc.gridy = y++;
            add(new JLabel("Unit of Measure:"), gbc);
            gbc.gridx = 1;
            unitOfMeasureField = new JTextField(20);
            unitOfMeasureField.setText("NOS"); // Default
            add(unitOfMeasureField, gbc);

            // --- Load existing data if in Edit Mode ---
            if (drugToEdit != null) {
                loadDrugData(drugToEdit);
            }

            // --- Buttons ---
            JPanel buttonPanel = new JPanel();
            JButton saveButton = new JButton(drugToEdit == null ? "ADD" : "SAVE");
            JButton cancelButton = new JButton("CANCEL");

            saveButton.addActionListener(_ -> {
                saveDrug();
            });

            cancelButton.addActionListener(_ -> dispose()); // Close dialog

            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            gbc.gridx = 0;
            gbc.gridy = y++;
            gbc.gridwidth = 2;
            add(buttonPanel, gbc);

            pack();
            setLocationRelativeTo(owner);
        }

        private void loadDrugData(Drug drug) {
            brandNameField.setText(drug.getBrandName());
            genericNameField.setText(drug.getGenericName());
            manufacturerField.setText(drug.getManufacturer());
            formulationField.setText(drug.getFormulation());
            strengthField.setText(drug.getStrength());
            scheduleCategoryField.setText(drug.getScheduleCategory());
            storageConditionsField.setText(drug.getStorageConditions());
            reorderLevelField.setText(String.valueOf(drug.getReorderLevel()));
            isActiveCheckBox.setSelected(drug.isActive());
            if (drug.getPreferredSupplierId() != null) {
                preferredSupplierIdField.setText(String.valueOf(drug.getPreferredSupplierId()));
            } else {
                preferredSupplierIdField.setText("");
            }
            if (drug.getMaterialType() != null) {
                materialTypeComboBox.setSelectedItem(drug.getMaterialType());
            }
            if (drug.getUnitOfMeasure() != null) {
                unitOfMeasureField.setText(drug.getUnitOfMeasure());
            }
        }

        private void saveDrug() {
            // Logic moved from anonymous class to method for cleaner structure
            try {
                // 1. Get and Validate Input
                String materialCode = materialCodeField.getText().trim();
                String brandName = brandNameField.getText().trim();
                String genericName = genericNameField.getText().trim();
                String manufacturer = manufacturerField.getText().trim();
                String formulation = formulationField.getText().trim();
                String strength = strengthField.getText().trim();
                String scheduleCategory = scheduleCategoryField.getText().trim();
                String storageConditions = storageConditionsField.getText().trim();
                String reorderLevelStr = reorderLevelField.getText().trim();
                boolean isActive = isActiveCheckBox.isSelected();
                String preferredSupplierIdStr = preferredSupplierIdField.getText().trim();
                String materialType = (String) materialTypeComboBox.getSelectedItem();
                String unitOfMeasure = unitOfMeasureField.getText().trim();

                // Basic validation
                if (materialCode.isEmpty() || brandName.isEmpty() || genericName.isEmpty()
                        || reorderLevelStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please fill in all mandatory fields (Material Code, Brand Name, Generic Name, Reorder Level).",
                            "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int reorderLevel;
                try {
                    reorderLevel = Integer.parseInt(reorderLevelStr);
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Reorder Level must be a valid integer.", "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Integer preferredSupplierId = null;
                if (!preferredSupplierIdStr.isEmpty()) {
                    try {
                        preferredSupplierId = Integer.parseInt(preferredSupplierIdStr);
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(this,
                                "Preferred Supplier ID must be a valid integer or left blank.", "Input Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // 2. Create/Update Drug Object
                // Use the existing object for update if available, otherwise create a new one
                Drug drugToSave = (drugToEdit != null) ? drugToEdit : new Drug();

                drugToSave.setMaterialCode(materialCode);
                drugToSave.setBrandName(brandName);
                drugToSave.setGenericName(genericName);
                drugToSave.setManufacturer(manufacturer);
                drugToSave.setFormulation(formulation);
                drugToSave.setStrength(strength);
                drugToSave.setScheduleCategory(scheduleCategory);
                drugToSave.setStorageConditions(storageConditions);
                drugToSave.setReorderLevel(reorderLevel);
                drugToSave.setActive(isActive);
                drugToSave.setPreferredSupplierId(preferredSupplierId);
                drugToSave.setMaterialType(materialType);
                drugToSave.setUnitOfMeasure(unitOfMeasure.isEmpty() ? "NOS" : unitOfMeasure);

                // 3. Call Database Service
                boolean success;
                String message;

                if (drugToEdit == null) {
                    // ADD Mode
                    success = parentPanel.dbService.addDrug(drugToSave);
                    message = "Drug added successfully!";
                } else {
                    // EDIT Mode
                    success = parentPanel.dbService.updateDrug(drugToSave);
                    message = "Drug updated successfully!";
                }

                // 4. Handle Result
                if (success) {
                    JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                    parentPanel.refreshDrugList();
                    dispose(); // Close the dialog
                } else {
                    JOptionPane.showMessageDialog(this,
                            (drugToEdit == null ? "Failed to add drug. It may already exist (duplicate Material Code)."
                                    : "Failed to update drug."),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error processing drug record: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}
