package pharma.gui;

import pharma.model.Material;
import pharma.service.DatabaseService;
import pharma.gui.components.MaterialSearchField;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.util.List;

public class MaterialsPanel extends JPanel {

    private JTable drugsTable;
    private DefaultTableModel tableModel;
    private DatabaseService dbService;
    private JFrame mainFrame;
    private MaterialSearchField searchField;
    private TableRowSorter<DefaultTableModel> sorter;

    public MaterialsPanel(JFrame mainFrame, DatabaseService dbService) {
        this.mainFrame = mainFrame;
        this.dbService = dbService;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Material Management"));

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
        searchField = new MaterialSearchField(dbService.getAllDrugs());
        searchField.setPreferredSize(new Dimension(300, 25));
        searchField.setSelectionListener(material -> {
            // When a material is selected from dropdown, filter the table
            filterTableByDrug(material);
        });

        JButton clearSearchBtn = new JButton("Clear Search");
        clearSearchBtn.addActionListener(e -> {
            searchField.clearSelection();
            sorter.setRowFilter(null); // Show all rows
        });

        JButton addButton = new JButton("Add Material");
        JButton editButton = new JButton("Edit Material");
        JButton deleteButton = new JButton("Delete Material");
        JButton refreshButton = new JButton("Refresh Data");

        addButton.addActionListener(e -> showDrugFormDialog(null)); // Pass null for ADD mode

        // --- EDIT BUTTON IMPLEMENTATION ---
        editButton.addActionListener(e -> {
            int selectedRow = drugsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(MaterialsPanel.this, "Please select a material to edit.", "Selection Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Convert view row to model row (important when using sorter/filter)
            int modelRow = drugsTable.convertRowIndexToModel(selectedRow);
            String materialCode = (String) tableModel.getValueAt(modelRow, 0);

            // Fetch the full Material object for pre-populating the form
            Material drugToEdit = dbService.getDrugByMaterialCode(materialCode);

            if (drugToEdit != null) {
                showDrugFormDialog(drugToEdit); // Pass the existing material object for EDIT mode
            } else {
                JOptionPane.showMessageDialog(MaterialsPanel.this, "Could not fetch material data from the database.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // --- DELETE BUTTON IMPLEMENTATION ---
        deleteButton.addActionListener(e -> {
            int selectedRow = drugsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(MaterialsPanel.this, "Please select a material to delete.", "Selection Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int modelRow = drugsTable.convertRowIndexToModel(selectedRow);
            String materialCode = (String) tableModel.getValueAt(modelRow, 0);
            String brandName = (String) tableModel.getValueAt(modelRow, 1);

            int confirm = JOptionPane.showConfirmDialog(
                    MaterialsPanel.this,
                    "Are you sure you want to delete the material: " + brandName + " (" + materialCode + ")?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (dbService.deleteDrug(materialCode)) {
                    JOptionPane.showMessageDialog(MaterialsPanel.this, "Material deleted successfully.", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    refreshDrugList(); // Refresh the table
                } else {
                    JOptionPane.showMessageDialog(MaterialsPanel.this,
                            "Failed to delete material. It might be referenced by other records.", "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // --- REFRESH BUTTON IMPLEMENTATION ---
        refreshButton.addActionListener(e -> {
            refreshDrugList();
            JOptionPane.showMessageDialog(MaterialsPanel.this, "Material list refreshed.", "Info",
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

        // Add the Control Panel to the MaterialsPanel
        add(controlPanel, BorderLayout.NORTH);

        loadDrugs(); // Load initial data
    } // <--- Constructor correctly ends here.

    private void loadDrugs() {
        tableModel.setRowCount(0); // Clear existing data

        try {
            List<Material> materials = dbService.getAllDrugs();
            if (materials != null) {
                for (Material material : materials) {
                    tableModel.addRow(new Object[] {
                            material.getMaterialCode(),
                            material.getBrandName(),
                            material.getGenericName(),
                            material.getManufacturer(),
                            material.getFormulation(),
                            material.getStrength(),
                            material.getReorderLevel(),
                            material.getPreferredSupplierId(),
                            material.getMaterialType(),
                            material.getUnitOfMeasure()
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading materials: " + e.getMessage());
        }
    }

    /**
     * Handles opening the dialog in either Add or Edit mode.
     */
    private void showDrugFormDialog(Material drugToEdit) {
        // Use the stored mainFrame and pass THIS panel instance for refresh callback
        JDialog dialog = new DrugFormDialog(this.mainFrame, this, drugToEdit);
        dialog.setVisible(true);
    }

    /**
     * Public callback for the dialog to refresh the table on success.
     */
    public void refreshDrugList() {
        loadDrugs();
        // Update search field with latest material list
        searchField.setDrugList(dbService.getAllDrugs());
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
        RowFilter<DefaultTableModel, Object> filter = RowFilter.regexFilter(
                "^" + material.getMaterialCode() + "$", 0); // Column 0 is Material Code
        sorter.setRowFilter(filter);
    }

    /**
     * Refactored and renamed from AddDrugDialog to serve as a generic form for Add
     * and Edit.
     */
    private class DrugFormDialog extends JDialog {

        private MaterialsPanel parentPanel;
        private Material drugToEdit; // Holds the material object if in EDIT mode (or null for ADD mode)

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
        public DrugFormDialog(JFrame owner, MaterialsPanel parent, Material material) {
            super(owner, material == null ? "Add New Material" : "Edit Material: " + material.getMaterialCode(), true);
            this.parentPanel = parent;
            this.drugToEdit = material;

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
                materialCodeField.setForeground(Color.BLACK);
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
                    new String[] { "RAW_MATERIAL", "EXCIPIENT", "PACKAGING", "INTERMEDIATE", "FINISHED_GOOD" });

            materialTypeComboBox.addActionListener(e -> {
                if (reorderLevelField.getText().trim().isEmpty() || reorderLevelField.getText().trim().equals("0")) {
                    String type = (String) materialTypeComboBox.getSelectedItem();
                    if ("RAW_MATERIAL".equals(type)) {
                        reorderLevelField.setText("500");
                    } else if ("EXCIPIENT".equals(type)) {
                        reorderLevelField.setText("250");
                    } else if ("PACKAGING".equals(type)) {
                        reorderLevelField.setText("1000");
                    } else if ("FINISHED_GOOD".equals(type)) {
                        reorderLevelField.setText("100");
                    } else {
                        reorderLevelField.setText("50");
                    }
                }
            });
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

            saveButton.addActionListener(e -> {
                saveDrug();
            });

            cancelButton.addActionListener(e -> dispose()); // Close dialog

            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            gbc.gridx = 0;
            gbc.gridy = y++;
            gbc.gridwidth = 2;
            add(buttonPanel, gbc);

            pack();
            setLocationRelativeTo(owner);
        }

        private void loadDrugData(Material material) {
            brandNameField.setText(material.getBrandName());
            genericNameField.setText(material.getGenericName());
            manufacturerField.setText(material.getManufacturer());
            formulationField.setText(material.getFormulation());
            strengthField.setText(material.getStrength());
            scheduleCategoryField.setText(material.getScheduleCategory());
            storageConditionsField.setText(material.getStorageConditions());
            reorderLevelField.setText(String.valueOf(material.getReorderLevel()));
            isActiveCheckBox.setSelected(material.isActive());
            if (material.getPreferredSupplierId() != null) {
                preferredSupplierIdField.setText(String.valueOf(material.getPreferredSupplierId()));
            } else {
                preferredSupplierIdField.setText("");
            }
            if (material.getMaterialType() != null) {
                materialTypeComboBox.setSelectedItem(material.getMaterialType().name());
            }
            if (material.getUnitOfMeasure() != null) {
                unitOfMeasureField.setText(material.getUnitOfMeasure().name());
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

                // 2. Create/Update Material Object
                // Use the existing object for update if available, otherwise create a new one
                Material drugToSave = (drugToEdit != null) ? drugToEdit : new Material();

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
                drugToSave.setMaterialType(Material.MaterialType.fromString(materialType));
                drugToSave.setUnitOfMeasure(Material.UnitOfMeasure.fromString(unitOfMeasure.isEmpty() ? "NOS" : unitOfMeasure));

                // 3. Call Database Service
                boolean success;
                String message;

                if (drugToEdit == null) {
                    // ADD Mode
                    success = parentPanel.dbService.addDrug(drugToSave);
                    message = "Material added successfully!";
                } else {
                    // EDIT Mode
                    success = parentPanel.dbService.updateDrug(drugToSave);
                    message = "Material updated successfully!";
                }

                // 4. Handle Result
                if (success) {
                    JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                    parentPanel.refreshDrugList();
                    dispose(); // Close the dialog
                } else {
                    JOptionPane.showMessageDialog(this,
                            (drugToEdit == null ? "Failed to add material. It may already exist (duplicate Material Code)."
                                    : "Failed to update material."),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error processing material record: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}
