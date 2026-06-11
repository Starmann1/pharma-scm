package pharma.gui.components;

import java.awt.*;
import java.util.List;

import javax.swing.*;

/**
 * Reusable Swing panel that renders an ordered list of agent trace entries.
 *
 * <p>Each step is displayed as a numbered row ("Step 1: …") with alternating
 * background colours for readability.  The panel is scrollable when the list
 * exceeds the visible area.
 *
 * <p>Colour palette follows the dark-themed AI Decision Dashboard style.
 */
public class AgentTracePanelComponent extends JPanel {

    // ── Dark-theme palette ──────────────────────────────────────────────────
    private static final Color BG_PRIMARY   = new Color(0x1a, 0x1a, 0x2e);
    private static final Color ROW_EVEN     = new Color(0x0f, 0x34, 0x60);
    private static final Color ROW_ODD      = new Color(0x1a, 0x1a, 0x2e);
    private static final Color TEXT_COLOR   = new Color(0xe0, 0xe0, 0xe0);
    private static final Color STEP_NUMBER  = new Color(0x53, 0xa8, 0xf4);

    private static final Font STEP_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    /**
     * Creates a new agent trace panel populated with the given steps.
     *
     * @param traceEntries ordered list of trace strings; may be {@code null} or empty
     */
    public AgentTracePanelComponent(List<String> traceEntries) {
        setLayout(new BorderLayout());
        setBackground(BG_PRIMARY);

        if (traceEntries == null || traceEntries.isEmpty()) {
            JLabel emptyLabel = new JLabel("  No agent trace entries available.");
            emptyLabel.setFont(STEP_FONT);
            emptyLabel.setForeground(TEXT_COLOR);
            add(emptyLabel, BorderLayout.CENTER);
            return;
        }

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BG_PRIMARY);

        for (int i = 0; i < traceEntries.size(); i++) {
            listPanel.add(createStepRow(i + 1, traceEntries.get(i), i % 2 == 0));
        }

        // Push remaining space below the last step
        listPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BG_PRIMARY);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private JPanel createStepRow(int stepNumber, String text, boolean even) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(even ? ROW_EVEN : ROW_ODD);
        row.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel numberLabel = new JLabel("Step " + stepNumber + ": ");
        numberLabel.setFont(STEP_FONT.deriveFont(Font.BOLD));
        numberLabel.setForeground(STEP_NUMBER);
        row.add(numberLabel, BorderLayout.WEST);

        JLabel textLabel = new JLabel(text);
        textLabel.setFont(STEP_FONT);
        textLabel.setForeground(TEXT_COLOR);
        row.add(textLabel, BorderLayout.CENTER);

        return row;
    }
}
