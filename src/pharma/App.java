package pharma;

import pharma.service.DatabaseService;
import pharma.gui.LoginGUI;
//import pharma.gui.InventoryGUI; // 👈 UPDATED: Import InventoryGUI
//import pharma.gui.LoginGUI;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

public class App {
    public static void main(String[] args) {
        // Ensure the GUI starts on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            DatabaseService dbService = new DatabaseService();

            // 1. Test database connection first
            if (dbService.connect()) {
                // Application launches directly into main window
                System.out.println("Application starting up, bypassing login...");

                LoginGUI login = new LoginGUI(dbService);
                login.setVisible(true);

                // InventoryGUI gui = new InventoryGUI(dbService);
                // gui.setVisible(true);
                // The LoginGUI logic is now removed.

            } else {
                JOptionPane.showMessageDialog(null,
                        "Failed to connect to the database. Check your MySQL server status and JDBC details.",
                        "Fatal Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
