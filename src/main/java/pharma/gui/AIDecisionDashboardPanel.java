package pharma.gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pharma.config.ApplicationServices;
import pharma.dto.AIReasoningResultDTO;
import pharma.service.AIDecisionService;

/**
 * Professional dark-themed Swing panel that displays all AI decisions in a
 * filterable table with drill-down capability.
 *
 * <p>Features:
 * <ul>
 *   <li>Top bar with title, Refresh button, and status filter combo.</li>
 *   <li>Sortable table with colour-coded confidence and status cells.</li>
 *   <li>Double-click to open {@link AIDecisionDetailDialog}.</li>
 *   <li>Status bar showing total / pending counts.</li>
 * </ul>
 *
 * <p>Colour palette:
 * <table>
 *   <tr><td>Background</td><td>{@code #1a1a2e}</td></tr>
 *   <tr><td>Header</td><td>{@code #16213e}</td></tr>
 *   <tr><td>Row even</td><td>{@code #0f3460}</td></tr>
 *   <tr><td>Row odd</td><td>{@code #1a1a2e}</td></tr>
 *   <tr><td>Text</td><td>{@code #e0e0e0}</td></tr>
 * </table>
 */
public class AIDecisionDashboardPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(AIDecisionDashboardPanel.class);

    // ── Dark-theme palette ──────────────────────────────────────────────────
    private static final Color BG_PRIMARY   = new Color(0x1a, 0x1a, 0x2e);
    private static final Color BG_HEADER    = new Color(0x16, 0x21, 0x3e);
    private static final Color ROW_EVEN     = new Color(0x0f, 0x34, 0x60);
    private static final Color ROW_ODD      = new Color(0x1a, 0x1a, 0x2e);
    private static final Color TEXT_COLOR   = new Color(0xe0, 0xe0, 0xe0);
    private static final Color GREEN        = new Color(0x4c, 0xaf, 0x50);
    private static final Color RED          = new Color(0xf4, 0x43, 0x36);
    private static final Color ORANGE       = new Color(0xff, 0x98, 0x00);
    private static final Color YELLOW       = new Color(0xff, 0xeb, 0x3b);
    private static final Color ACCENT_BLUE  = new Color(0x53, 0xa8, 0xf4);

    private static final Font TITLE_FONT  = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font BODY_FONT   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font STATUS_FONT = new Font("Segoe UI", Font.ITALIC, 12);

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── Table columns ───────────────────────────────────────────────────────
    private static final String[] COLUMNS = {
            "ID", "Task Type", "Confidence", "Status", "Requires Review", "Created At"
    };
    private static final int COL_ID           = 0;
    private static final int COL_TASK_TYPE    = 1;
    private static final int COL_CONFIDENCE   = 2;
    private static final int COL_STATUS       = 3;
    private static final int COL_REVIEW       = 4;
    private static final int COL_CREATED_AT   = 5;

    // ── State ───────────────────────────────────────────────────────────────
    private final ApplicationServices services;
    private final AIDecisionService aiDecisionService;

    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> filterCombo;
    private JLabel statusBar;
    private List<AIReasoningResultDTO> currentData = new ArrayList<>();

    /**
     * Creates a new AI Decision Dashboard panel.
     *
     * @param services application service façade providing access to
     *                 {@link AIDecisionService}
     */
    public AIDecisionDashboardPanel(ApplicationServices services) {
        this.services = services;
        this.aiDecisionService = services.getAiDecisionService();

        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PRIMARY);

        add(createTopBar(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        refreshData();
    }

    /**
     * Reloads the table data from the database, honouring the active filter.
     */
    public void refreshData() {
        try {
            currentData = aiDecisionService.findAll();
            applyFilter();
        } catch (Exception e) {
            log.error("Failed to load AI decisions: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this,
                    "Error loading AI decisions: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Top bar ─────────────────────────────────────────────────────────────

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(BG_HEADER);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Title
        JLabel title = new JLabel("AI Decision Dashboard");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT_COLOR);
        bar.add(title, BorderLayout.WEST);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);

        filterCombo = new JComboBox<>(new String[]{"ALL", "PENDING", "APPROVED", "REJECTED"});
        filterCombo.setFont(BODY_FONT);
        filterCombo.addActionListener(e -> applyFilter());
        controls.add(new StyledLabel("Filter:"));
        controls.add(filterCombo);

        JButton refreshBtn = new JButton("⟳  Refresh");
        refreshBtn.setFont(BODY_FONT.deriveFont(Font.BOLD));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setBackground(ACCENT_BLUE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> refreshData());
        controls.add(refreshBtn);

        bar.add(controls, BorderLayout.EAST);
        return bar;
    }

    // ── Table ───────────────────────────────────────────────────────────────

    private JScrollPane createTablePanel() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(BODY_FONT);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(BG_PRIMARY);
        table.setForeground(TEXT_COLOR);
        table.setSelectionBackground(ACCENT_BLUE.darker());
        table.setSelectionForeground(Color.WHITE);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(BODY_FONT.deriveFont(Font.BOLD));
        header.setBackground(BG_HEADER);
        header.setForeground(TEXT_COLOR);
        header.setReorderingAllowed(false);

        // Column widths
        table.getColumnModel().getColumn(COL_ID).setPreferredWidth(180);
        table.getColumnModel().getColumn(COL_TASK_TYPE).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_CONFIDENCE).setPreferredWidth(110);
        table.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_REVIEW).setPreferredWidth(110);
        table.getColumnModel().getColumn(COL_CREATED_AT).setPreferredWidth(140);

        // Renderers
        table.getColumnModel().getColumn(COL_CONFIDENCE)
                .setCellRenderer(new ConfidenceCellRenderer());
        table.getColumnModel().getColumn(COL_STATUS)
                .setCellRenderer(new StatusCellRenderer());

        // Default renderer for alternating rows
        DefaultTableCellRenderer altRowRenderer = new AlternatingRowRenderer();
        for (int i = 0; i < COLUMNS.length; i++) {
            if (i != COL_CONFIDENCE && i != COL_STATUS) {
                table.getColumnModel().getColumn(i).setCellRenderer(altRowRenderer);
            }
        }

        // Double-click → detail dialog
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDetailDialog();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_PRIMARY);
        return scroll;
    }

    // ── Status bar ──────────────────────────────────────────────────────────

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        bar.setBackground(BG_HEADER);

        statusBar = new JLabel();
        statusBar.setFont(STATUS_FONT);
        statusBar.setForeground(TEXT_COLOR);
        bar.add(statusBar);

        return bar;
    }

    // ── Filter & populate ───────────────────────────────────────────────────

    private void applyFilter() {
        String filter = (String) filterCombo.getSelectedItem();
        if (filter == null) {
            filter = "ALL";
        }

        tableModel.setRowCount(0);

        int totalShown = 0;
        int pendingCount = 0;

        for (AIReasoningResultDTO d : currentData) {
            String status = d.getStatus() != null ? d.getStatus() : "PENDING";

            if ("PENDING".equalsIgnoreCase(status)) {
                pendingCount++;
            }

            if (!"ALL".equals(filter) && !filter.equalsIgnoreCase(status)) {
                continue;
            }

            String createdAt = d.getCreatedAt() != null
                    ? d.getCreatedAt().format(DT_FMT)
                    : "—";

            tableModel.addRow(new Object[]{
                    d.getTransactionId(),
                    d.getTaskType(),
                    d.getConfidenceScore(),
                    status,
                    d.isRequiresHumanReview() ? "Yes" : "No",
                    createdAt
            });
            totalShown++;
        }

        statusBar.setText(totalShown + " total decision(s) | " + pendingCount + " pending review");
    }

    // ── Detail dialog ───────────────────────────────────────────────────────

    private void openDetailDialog() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }

        String txId = (String) tableModel.getValueAt(viewRow, COL_ID);

        AIReasoningResultDTO detail = currentData.stream()
                .filter(d -> txId != null && txId.equals(d.getTransactionId()))
                .findFirst()
                .orElse(null);

        if (detail == null) {
            JOptionPane.showMessageDialog(this,
                    "Could not find decision record for ID: " + txId,
                    "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        new AIDecisionDetailDialog(owner, detail, aiDecisionService, this::refreshData)
                .setVisible(true);
    }

    // =====================================================================
    // Custom cell renderers
    // =====================================================================

    /**
     * Alternating-row renderer: even rows {@link #ROW_EVEN}, odd rows
     * {@link #ROW_ODD}.
     */
    private static class AlternatingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            if (!sel) {
                c.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                c.setForeground(TEXT_COLOR);
            }
            return c;
        }
    }

    /**
     * Renders the <b>Confidence</b> column with a colour-coded value and icon.
     *
     * <ul>
     *   <li>≥ 0.75 → green + ✅</li>
     *   <li>0.50–0.74 → orange + ⚠️</li>
     *   <li>&lt; 0.50 → red + ❌</li>
     * </ul>
     */
    private static class ConfidenceCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);

            double score = 0;
            if (value instanceof Number n) {
                score = n.doubleValue();
            }

            String icon;
            Color fg;
            if (score >= 0.75) {
                icon = "✅ ";
                fg = GREEN;
            } else if (score >= 0.50) {
                icon = "⚠️ ";
                fg = ORANGE;
            } else {
                icon = "❌ ";
                fg = RED;
            }

            setText(icon + String.format("%.1f%%", score * 100));
            setForeground(sel ? Color.WHITE : fg);
            setBackground(sel ? ACCENT_BLUE.darker() : (row % 2 == 0 ? ROW_EVEN : ROW_ODD));
            setFont(BODY_FONT.deriveFont(Font.BOLD));
            return this;
        }
    }

    /**
     * Renders the <b>Status</b> column with colour coding:
     * PENDING → yellow, APPROVED → green, REJECTED → red.
     */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);

            String status = value != null ? value.toString() : "";
            Color fg = switch (status.toUpperCase()) {
                case "PENDING"  -> YELLOW;
                case "APPROVED" -> GREEN;
                case "REJECTED" -> RED;
                default         -> TEXT_COLOR;
            };

            setForeground(sel ? Color.WHITE : fg);
            setBackground(sel ? ACCENT_BLUE.darker() : (row % 2 == 0 ? ROW_EVEN : ROW_ODD));
            setFont(BODY_FONT.deriveFont(Font.BOLD));
            setHorizontalAlignment(CENTER);
            return this;
        }
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    /** Small helper label with the dashboard text colour. */
    private static class StyledLabel extends JLabel {
        StyledLabel(String text) {
            super(text);
            setFont(BODY_FONT);
            setForeground(TEXT_COLOR);
        }
    }
}
