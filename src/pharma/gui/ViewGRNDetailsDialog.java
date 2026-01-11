package pharma.gui;

import pharma.model.GRN;

import javax.swing.*;
import java.awt.*;

public class ViewGRNDetailsDialog extends JDialog {
    public ViewGRNDetailsDialog(JFrame owner, GRN grn) {
        super(owner, "GRN Details", true);

        setLayout(new BorderLayout(10, 10));
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("GRN Details"));

        panel.add(new JLabel("GRN ID:"));
        panel.add(new JLabel(String.valueOf(grn.getId())));
        panel.add(new JLabel("PO ID:"));
        panel.add(new JLabel(String.valueOf(grn.getPurchaseOrderId())));
        panel.add(new JLabel("Supplier:"));
        panel.add(new JLabel(grn.getSupplierName()));
        panel.add(new JLabel("Received Date:"));
        panel.add(new JLabel(grn.getReceivedDate().toString()));
        panel.add(new JLabel("Status:"));
        panel.add(new JLabel(grn.getStatus()));

        add(panel, BorderLayout.CENTER);
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> dispose());
        add(okBtn, BorderLayout.SOUTH);

        setSize(350, 230);
        setLocationRelativeTo(owner);
    }
}
