package pharma.gui;

import pharma.model.Permission;
import pharma.model.Role;
import pharma.model.User;
import pharma.service.RoleService;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdminRBACPanel extends JPanel {
    private RoleService roleService;
    private User activeUser;
    private JComboBox<Role> roleComboBox;
    private JPanel permissionsPanel;
    private Map<Integer, JCheckBox> permissionCheckboxes;

    public AdminRBACPanel(RoleService roleService, User activeUser) {
        this.roleService = roleService;
        this.activeUser = activeUser;
        this.permissionCheckboxes = new HashMap<>();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Role-Based Access Control (RBAC) Management");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(headerLabel, BorderLayout.NORTH);

        // Top Selection Panel
        JPanel topSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topSelectionPanel.add(new JLabel("Select Role: "));
        roleComboBox = new JComboBox<>();
        roleComboBox.addActionListener(e -> {
            Role selected = (Role) roleComboBox.getSelectedItem();
            if (selected != null) {
                loadPermissionsForRole(selected.getRoleId());
            }
        });
        topSelectionPanel.add(roleComboBox);

        // Add permissions panel (using GridBagLayout for better checkbox wrapping, but
        // GridLayout is easier)
        permissionsPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        JScrollPane scrollPane = new JScrollPane(permissionsPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Role Permissions"));

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.add(topSelectionPanel, BorderLayout.NORTH);
        centerWrapper.add(scrollPane, BorderLayout.CENTER);

        add(centerWrapper, BorderLayout.CENTER);

        // Bottom - Save Button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save Permissions");
        saveButton.setBackground(new Color(40, 167, 69));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFont(new Font("Arial", Font.BOLD, 14));
        saveButton.addActionListener(e -> savePermissions());
        bottomPanel.add(saveButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // Load Initial Data
        loadInitialData();
    }

    private void loadInitialData() {
        // Load Permissions Checkboxes
        List<Permission> allPermissions = roleService.getAllPermissions();
        permissionsPanel.removeAll();
        permissionCheckboxes.clear();

        for (Permission p : allPermissions) {
            JCheckBox checkBox = new JCheckBox(p.getModule() + " - " + p.getPermissionName());
            checkBox.setToolTipText(p.getDescription());
            permissionCheckboxes.put(p.getPermissionId(), checkBox);
            permissionsPanel.add(checkBox);
        }

        // Load Roles
        List<Role> roles = roleService.getAllRoles();
        roleComboBox.removeAllItems();
        for (Role role : roles) {
            roleComboBox.addItem(role);
        }

        if (!roles.isEmpty()) {
            roleComboBox.setSelectedIndex(0);
        }
    }

    private void loadPermissionsForRole(int roleId) {
        Set<Integer> assignedIds = roleService.getPermissionIdsForRole(roleId);

        for (Map.Entry<Integer, JCheckBox> entry : permissionCheckboxes.entrySet()) {
            entry.getValue().setSelected(assignedIds.contains(entry.getKey()));
        }
    }

    private void savePermissions() {
        Role selected = (Role) roleComboBox.getSelectedItem();
        if (selected == null)
            return;

        // Security check: Don't allow non-admins to save changes
        if (!activeUser.hasRole("Admin") && !activeUser.hasPermission("MANAGE_USERS")) {
            JOptionPane.showMessageDialog(this, "You don't have permission to modify roles.", "Access Denied",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Set<Integer> newPermissions = new HashSet<>();
        for (Map.Entry<Integer, JCheckBox> entry : permissionCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                newPermissions.add(entry.getKey());
            }
        }

        boolean success = roleService.updateRolePermissions(selected.getRoleId(), newPermissions,
                activeUser.getUserId());

        if (success) {
            JOptionPane.showMessageDialog(this, "Permissions updated successfully for " + selected.getRoleName()
                    + "\nChanges will take effect on next login.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update permissions.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
