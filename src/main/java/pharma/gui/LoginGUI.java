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


    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel messageLabel;
    private JLabel capsLockWarningLabel;

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
        loginPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#D1D5DB"), 1, true),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)));
        loginPanel.setBackground(Color.decode("#F4F6F8")); // Background color

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title/Logo
        JLabel titleLabel = new JLabel("Pharma IMS Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.decode("#0F766E")); // Primary Color
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);

        // Message Label (for error/success messages)
        messageLabel = new JLabel("Enter your credentials", SwingConstants.CENTER);
        messageLabel.setForeground(Color.decode("#1F2937")); // Text color
        gbc.gridy = 1;
        loginPanel.add(messageLabel, gbc);

        // Employee ID Field
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        JLabel userLabel = new JLabel("Employee ID:", SwingConstants.RIGHT);
        userLabel.setForeground(Color.decode("#1F2937"));
        loginPanel.add(userLabel, gbc);
        usernameField = new JTextField(15);
        gbc.gridx = 1;
        loginPanel.add(usernameField, gbc);

        // Password Field
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel passLabel = new JLabel("Password:", SwingConstants.RIGHT);
        passLabel.setForeground(Color.decode("#1F2937"));
        loginPanel.add(passLabel, gbc);

        JPanel passwordPanel = new JPanel(new BorderLayout(5, 0));
        passwordPanel.setOpaque(false);
        passwordField = new JPasswordField(15);

        // Show/Hide Password Button
        JToggleButton showPasswordBtn = new JToggleButton("👁");
        showPasswordBtn.setMargin(new Insets(0, 5, 0, 5));
        showPasswordBtn.setToolTipText("Show or Hide Password");
        showPasswordBtn.setFocusPainted(false);
        char defaultEchoChar = passwordField.getEchoChar();
        showPasswordBtn.addActionListener(e -> {
            if (showPasswordBtn.isSelected()) {
                passwordField.setEchoChar((char) 0); // Show
            } else {
                passwordField.setEchoChar(defaultEchoChar); // Hide
            }
        });

        passwordPanel.add(passwordField, BorderLayout.CENTER);
        passwordPanel.add(showPasswordBtn, BorderLayout.EAST);

        gbc.gridx = 1;
        loginPanel.add(passwordPanel, gbc);

        // Caps Lock Warning Label
        capsLockWarningLabel = new JLabel("Caps Lock is ON");
        capsLockWarningLabel.setForeground(Color.RED);
        capsLockWarningLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        capsLockWarningLabel.setVisible(false);
        gbc.gridx = 1;
        gbc.gridy = 4;
        loginPanel.add(capsLockWarningLabel, gbc);

        // Caps Lock Listener
        passwordField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                checkCapsLock();
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                checkCapsLock();
            }

            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                checkCapsLock();
            }
        });

        // Login Button
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setBackground(Color.decode("#0F766E")); // Primary color
        loginButton.setForeground(Color.WHITE);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        // Hover effect for Button
        loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(Color.decode("#115E59")); // Button Hover
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(Color.decode("#0F766E")); // Primary
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        loginPanel.add(loginButton, gbc);

        // --- Event Listener ---
        loginButton.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());

        // Add login panel to the frame center
        getContentPane().setBackground(Color.WHITE); // Surround background
        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 50));
        outerPanel.setBackground(Color.WHITE);
        outerPanel.add(loginPanel);
        add(outerPanel, BorderLayout.CENTER);

        // --- Footer Note ---
        JLabel footerLabel = new JLabel(
                "Authorized access only. All activities are monitored and logged per regulatory requirements (FDA / CDSCO / TGA / EFSA).",
                SwingConstants.CENTER);
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        footerLabel.setForeground(Color.GRAY);
        footerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(footerLabel, BorderLayout.SOUTH);
    }

    private void checkCapsLock() {
        boolean isCapsLockOn = Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);
        capsLockWarningLabel.setVisible(isCapsLockOn);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Employee ID and password are required.");
            messageLabel.setForeground(Color.RED);
            return;
        }

        messageLabel.setText("Authenticating...");
        messageLabel.setForeground(Color.ORANGE.darker());
        User user = authService.authenticate(username, password);

        if (user != null) {
            // Success: role is auto-detected from DB
            messageLabel.setText("Login Successful!");
            messageLabel.setForeground(Color.BLUE.darker());

            SwingUtilities.invokeLater(() -> {
                InventoryGUI gui = new InventoryGUI(dbService, user); // Pass both dbService AND the authenticated user
                gui.setVisible(true);
                dispose(); // Close the login window
            });
        } else {
            // Failure
            messageLabel.setText("Invalid Employee ID or password.");
            messageLabel.setForeground(Color.RED);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }
}
