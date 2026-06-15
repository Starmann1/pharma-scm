package pharma.gui;

import java.awt.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import pharma.dto.AIReasoningResultDTO;
import pharma.dto.CitationDTO;
import pharma.gui.components.AgentTracePanelComponent;
import pharma.service.AIDecisionService;

/**
 * Modal dialog that displays the full detail of a single AI decision record.
 *
 * <p>Uses a tabbed pane with four sections:
 * <ol>
 *   <li><b>Summary</b> — prompt summary and visual confidence bar.</li>
 *   <li><b>LLM Output</b> — raw model output in a monospaced text area.</li>
 *   <li><b>RAG Citations</b> — table of retrieved document excerpts.</li>
 *   <li><b>Agent Trace</b> — numbered execution trace via
 *       {@link AgentTracePanelComponent}.</li>
 * </ol>
 *
 * <p>Approve / Reject buttons are only enabled when the decision status is
 * {@code PENDING}.  On approval or rejection the parent dashboard table is
 * automatically refreshed.
 */
public class AIDecisionDetailDialog extends JDialog {

    // ── Dark-theme palette ──────────────────────────────────────────────────
    private static final Color BG_PRIMARY   = new Color(0x1a, 0x1a, 0x2e);
    private static final Color BG_HEADER    = new Color(0x16, 0x21, 0x3e);
    private static final Color BG_FIELD     = new Color(0x0f, 0x34, 0x60);
    private static final Color TEXT_COLOR   = new Color(0xe0, 0xe0, 0xe0);
    private static final Color GREEN        = new Color(0x4c, 0xaf, 0x50);
    private static final Color RED          = new Color(0xf4, 0x43, 0x36);
    private static final Color ORANGE       = new Color(0xff, 0x98, 0x00);

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font BODY_FONT  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font MONO_FONT  = new Font("Consolas", Font.PLAIN, 12);

    private final AIReasoningResultDTO decision;
    private final AIDecisionService aiDecisionService;
    private final Runnable onRefresh;

    /**
     * Opens a detail dialog for the given AI decision.
     *
     * @param owner             parent frame
     * @param decision          the decision record to display
     * @param aiDecisionService service used to approve/reject
     * @param onRefresh         callback invoked after a status change so the
     *                          parent table can reload
     */
    public AIDecisionDetailDialog(Frame owner,
                                   AIReasoningResultDTO decision,
                                   AIDecisionService aiDecisionService,
                                   Runnable onRefresh) {
        super(owner, "AI Decision Detail — #"
                + (decision.getTransactionId() != null ? decision.getTransactionId() : "?"),
                true);
        this.decision = decision;
        this.aiDecisionService = aiDecisionService;
        this.onRefresh = onRefresh;

        initUI();
        setSize(700, 500);
        setLocationRelativeTo(owner);
    }

    // ── Layout ──────────────────────────────────────────────────────────────

    private void initUI() {
        getContentPane().setBackground(BG_PRIMARY);
        setLayout(new BorderLayout(0, 8));

        add(createTitleBar(), BorderLayout.NORTH);
        add(createTabbedPane(), BorderLayout.CENTER);
        add(createButtonBar(), BorderLayout.SOUTH);
    }

    private JPanel createTitleBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bar.setBackground(BG_HEADER);

        JLabel title = new JLabel("AI Decision Detail");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT_COLOR);
        bar.add(title);

        String status = decision.getStatus() != null ? decision.getStatus() : "UNKNOWN";
        JLabel badge = new JLabel("  [" + status + "]");
        badge.setFont(BODY_FONT.deriveFont(Font.BOLD));
        badge.setForeground(statusColor(status));
        bar.add(badge);

        return bar;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(BODY_FONT);
        tabs.setBackground(BG_PRIMARY);
        tabs.setForeground(TEXT_COLOR);

        tabs.addTab("Summary", createSummaryTab());
        tabs.addTab("LLM Output", createLlmOutputTab());
        tabs.addTab("RAG Citations", createCitationsTab());
        tabs.addTab("Agent Trace", createTraceTab());

        return tabs;
    }

    // ── Tab 1: Summary ──────────────────────────────────────────────────────

    private JPanel createSummaryTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextArea summaryArea = new JTextArea(
                decision.getPromptSummary() != null ? decision.getPromptSummary() : "(no summary)");
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(BODY_FONT);
        summaryArea.setBackground(BG_FIELD);
        summaryArea.setForeground(TEXT_COLOR);
        summaryArea.setCaretColor(TEXT_COLOR);
        summaryArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BG_HEADER), "Prompt Summary",
                        0, 0, BODY_FONT, TEXT_COLOR),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        panel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);

        // Confidence bar
        JPanel confidencePanel = new JPanel(new BorderLayout(8, 0));
        confidencePanel.setBackground(BG_PRIMARY);
        confidencePanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        double score = decision.getConfidenceScore();
        JLabel confLabel = new JLabel(String.format("Confidence: %.1f%%", score * 100));
        confLabel.setFont(BODY_FONT.deriveFont(Font.BOLD));
        confLabel.setForeground(confidenceColor(score));
        confidencePanel.add(confLabel, BorderLayout.WEST);

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) (score * 100));
        bar.setStringPainted(true);
        bar.setString(String.format("%.0f%%", score * 100));
        bar.setFont(BODY_FONT);
        bar.setForeground(confidenceColor(score));
        bar.setBackground(BG_FIELD);
        confidencePanel.add(bar, BorderLayout.CENTER);

        panel.add(confidencePanel, BorderLayout.SOUTH);

        return panel;
    }

    // ── Tab 2: LLM Output ───────────────────────────────────────────────────

    private JPanel createLlmOutputTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String rawOutput = decision.getExtractedData() != null
                ? String.valueOf(decision.getExtractedData())
                : "(no output)";

        JTextArea area = new JTextArea(rawOutput);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(MONO_FONT);
        area.setBackground(BG_FIELD);
        area.setForeground(TEXT_COLOR);
        area.setCaretColor(TEXT_COLOR);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ── Tab 3: RAG Citations ────────────────────────────────────────────────

    private JPanel createCitationsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] columns = {"Document", "Page", "Snippet", "Relevance"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        List<CitationDTO> citations = decision.getCitations();
        if (citations != null) {
            for (CitationDTO c : citations) {
                model.addRow(new Object[]{
                        c.getDocumentName(),
                        c.getPageNumber(),
                        c.getChunkText(),
                        String.format("%.2f", c.getRelevanceScore())
                });
            }
        }

        JTable table = new JTable(model);
        styleTable(table);

        // Give the snippet column more space
        table.getColumnModel().getColumn(2).setPreferredWidth(350);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ── Tab 4: Agent Trace ──────────────────────────────────────────────────

    private JPanel createTraceTab() {
        List<String> trace = decision.getAgentTrace();
        return new AgentTracePanelComponent(trace);
    }

    // ── Button bar ──────────────────────────────────────────────────────────

    private JPanel createButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bar.setBackground(BG_HEADER);

        boolean isPending = "PENDING".equalsIgnoreCase(decision.getStatus());

        JButton approveBtn = new JButton("✅  Approve");
        approveBtn.setFont(BODY_FONT.deriveFont(Font.BOLD));
        approveBtn.setBackground(GREEN);
        approveBtn.setForeground(Color.WHITE);
        approveBtn.setFocusPainted(false);
        approveBtn.setEnabled(isPending);
        approveBtn.addActionListener(e -> onApprove());

        JButton rejectBtn = new JButton("❌  Reject");
        rejectBtn.setFont(BODY_FONT.deriveFont(Font.BOLD));
        rejectBtn.setBackground(RED);
        rejectBtn.setForeground(Color.WHITE);
        rejectBtn.setFocusPainted(false);
        rejectBtn.setEnabled(isPending);
        rejectBtn.addActionListener(e -> onReject());

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(BODY_FONT);
        closeBtn.addActionListener(e -> dispose());

        bar.add(approveBtn);
        bar.add(rejectBtn);
        bar.add(closeBtn);

        return bar;
    }

    // ── Actions ─────────────────────────────────────────────────────────────

    private void onApprove() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Approve this AI decision?", "Confirm Approval",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            aiDecisionService.approve(decision.getTransactionId(), 0);
            JOptionPane.showMessageDialog(this,
                    "Decision approved successfully.", "Approved",
                    JOptionPane.INFORMATION_MESSAGE);
            if (onRefresh != null) {
                onRefresh.run();
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error approving decision: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onReject() {
        String reason = JOptionPane.showInputDialog(this,
                "Enter rejection reason:", "Reject AI Decision",
                JOptionPane.WARNING_MESSAGE);
        if (reason == null || reason.isBlank()) {
            return; // cancelled or empty
        }
        try {
            aiDecisionService.reject(decision.getTransactionId(), 0, reason);
            JOptionPane.showMessageDialog(this,
                    "Decision rejected.", "Rejected",
                    JOptionPane.INFORMATION_MESSAGE);
            if (onRefresh != null) {
                onRefresh.run();
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error rejecting decision: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Styling helpers ─────────────────────────────────────────────────────

    private void styleTable(JTable table) {
        table.setFont(BODY_FONT);
        table.setBackground(BG_FIELD);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(BG_HEADER);
        table.setSelectionBackground(BG_HEADER);
        table.setSelectionForeground(TEXT_COLOR);
        table.setRowHeight(26);
        table.getTableHeader().setFont(BODY_FONT.deriveFont(Font.BOLD));
        table.getTableHeader().setBackground(BG_HEADER);
        table.getTableHeader().setForeground(TEXT_COLOR);
    }

    private Color confidenceColor(double score) {
        if (score >= 0.75) return GREEN;
        if (score >= 0.50) return ORANGE;
        return RED;
    }

    private Color statusColor(String status) {
        return switch (status.toUpperCase()) {
            case "APPROVED" -> GREEN;
            case "REJECTED" -> RED;
            case "PENDING"  -> ORANGE;
            default         -> TEXT_COLOR;
        };
    }
}
