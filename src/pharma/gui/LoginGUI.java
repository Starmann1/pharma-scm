package pharma.gui;

import pharma.model.User;
import pharma.service.AuthService;
import pharma.service.DatabaseService;
//import pharma.gui.InventoryGUI;

import javax.swing.*;
import java.awt.*;

public class LoginGUI extends JFrame {

    private final AuthService authService;
    private final DatabaseService dbService;

    private JComboBox<String> roleDropdown;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel messageLabel;

    public LoginGUI(DatabaseService dbService) {
        this.dbService = dbService;
        this.authService = new AuthService(dbService);

        setTitle("Pharma IMS - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        initializeComponents();

        pack();
        setLocationRelativeTo(null); // Center the window
        // setVisible(true);
    }

    private void initializeComponents() {
        // --- Main Login Panel (Aesthetics: Card-like panel) ---
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        loginPanel.setBackground(new Color(240, 248, 255)); // AliceBlue background

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title/Logo
        JLabel titleLabel = new JLabel("Pharma IMS Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(0, 102, 102));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);

        // Message Label (for error/success messages)
        messageLabel = new JLabel("Enter your credentials", SwingConstants.CENTER);
        messageLabel.setForeground(Color.BLACK);
        gbc.gridy = 1;
        loginPanel.add(messageLabel, gbc);

        // Role Field
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        loginPanel.add(new JLabel("Role:", SwingConstants.RIGHT), gbc);
        roleDropdown = new JComboBox<>();
        try {
            pharma.service.RoleService rs = new pharma.service.RoleService(dbService);
            for (pharma.model.Role r : rs.getAllRoles()) {
                roleDropdown.addItem(r.getRoleName());
            }
        } catch (Exception e) {
        }
        gbc.gridx = 1;
        loginPanel.add(roleDropdown, gbc);

        // Username Field
        gbc.gridx = 0;
        gbc.gridy = 3;
        loginPanel.add(new JLabel("Username:", SwingConstants.RIGHT), gbc);
        usernameField = new JTextField(15);
        gbc.gridx = 1;
        loginPanel.add(usernameField, gbc);

        // Password Field
        gbc.gridx = 0;
        gbc.gridy = 4;
        loginPanel.add(new JLabel("Password:", SwingConstants.RIGHT), gbc);
        passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        loginPanel.add(passwordField, gbc);

        // Login Button
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setBackground(new Color(0, 153, 153));
        loginButton.setForeground(Color.WHITE);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        loginPanel.add(loginButton, gbc);

        // --- Event Listener ---
        loginButton.addActionListener(_ -> attemptLogin());
        passwordField.addActionListener(_ -> attemptLogin());

        // Add login panel to the frame center
        add(loginPanel, BorderLayout.CENTER);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String selectedRole = (String) roleDropdown.getSelectedItem();

        if (username.isEmpty() || password.isEmpty() || selectedRole == null) {
            messageLabel.setText("Role, username, and password are required.");
            messageLabel.setForeground(Color.RED);
            return;
        }

        messageLabel.setText("Authenticating...");
        messageLabel.setForeground(Color.ORANGE.darker());
        User user = authService.authenticate(username, password);

        if (user != null) {
            if (!user.getRole().getRoleName().equalsIgnoreCase(selectedRole)) {
                messageLabel.setText("Invalid role selected for this user.");
                messageLabel.setForeground(Color.RED);
                return;
            }

            // Success: User authenticated
            messageLabel.setText("Login Successful!");
            messageLabel.setForeground(Color.BLUE.darker());

            // FIX: Launch main application GUI by calling the correct TWO-argument
            // constructor!
            SwingUtilities.invokeLater(() -> {
                InventoryGUI gui = new InventoryGUI(dbService, user); // Pass both dbService AND the authenticated user
                gui.setVisible(true);
                dispose(); // Close the login window
            });
        } else {
            // Failure
            messageLabel.setText("Invalid username or password.");
            messageLabel.setForeground(Color.RED);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }
}
