package pharma.gui;

import javax.swing.*;

import pharma.service.DatabaseService;

import java.awt.*;

public class ReportsPanel extends JPanel {

    public ReportsPanel(DatabaseService dbService) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("Reporting & Analytics"));

        // --- Center Panel for Report Options ---
        JPanel centerPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel selectLabel = new JLabel("Select a Report to Generate:");
        selectLabel.setFont(new Font("Arial", Font.BOLD, 16));
        centerPanel.add(selectLabel);
        
        // --- Report Buttons ---
        centerPanel.add(createReportButton("Generate Stock Value Report (Current Inventory Value)", "STOCK_VALUE"));
        centerPanel.add(createReportButton("Generate Low Stock / Reorder Report", "LOW_STOCK"));
        centerPanel.add(createReportButton("Generate Expiration Date Report (Next 6 Months)", "EXPIRATION"));
        centerPanel.add(createReportButton("Generate Supplier Performance Report", "SUPPLIER_PERF"));
        centerPanel.add(createReportButton("Generate Goods Received History", "GRN_HISTORY"));

        JPanel wrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapperPanel.add(centerPanel);

        add(wrapperPanel, BorderLayout.CENTER);
    }
    
    private JButton createReportButton(String text, String command) {
        JButton button = new JButton(text);
        button.setActionCommand(command);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setPreferredSize(new Dimension(350, 40));
        button.addActionListener(e -> generateReport(e.getActionCommand()));
        return button;
    }

    private void generateReport(String reportType) {
        String message;
        switch (reportType) {
            case "STOCK_VALUE":
                message = "Generating detailed stock value report including purchase price and FIFO/LIFO valuation...";
                break;
            case "LOW_STOCK":
                message = "Generating report of all items below their defined reorder point...";
                break;
            case "EXPIRATION":
                message = "Generating report highlighting items expiring within 180 days...";
                break;
            case "SUPPLIER_PERF":
                message = "Generating report on supplier fulfillment rates and average delivery times...";
                break;
            case "GRN_HISTORY":
                message = "Compiling a history of all received shipments.";
                break;
            default:
                message = "Report generation logic triggered for " + reportType;
                break;
        }
        JOptionPane.showMessageDialog(this, message, "Report Generation: " + reportType, JOptionPane.INFORMATION_MESSAGE);
        // In a real application, this would open a new window or save a file (e.g., PDF/Excel)
    }
}
