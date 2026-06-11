package pharma.gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pharma.App;
import pharma.agent.ontology.AgentActions;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;
import pharma.dto.RiskReportDTO;

/**
 * Professional dark-themed Swing dashboard for supply chain risk analysis.
 *
 * <p>Features:
 * <ul>
 *   <li>JTable displaying risk reports with columns: Entity Type, Entity ID,
 *       Risk Category, Score, Severity, Recommended Action, Generated At.</li>
 *   <li>Color-coded risk score cells: RED (&gt; 0.7), ORANGE (0.4–0.7),
 *       GREEN (&lt; 0.4).</li>
 *   <li>Refresh button that invokes the {@code RISK_ANALYSIS} agent action
 *       via the {@link pharma.agent.platform.AgentGateway}.</li>
 *   <li>Double-click on any row shows a detail dialog with contributing factors.</li>
 *   <li>Default sort by risk score descending.</li>
 *   <li>Status bar showing total risk count and high-risk count.</li>
 * </ul>
 */
public class RiskDashboardPanel extends JPanel {

    // =========================================================================
    // Theme constants
    // =========================================================================
    private static final Color BG_DARK          = new Color(0x1A, 0x1A, 0x2E);
    private static final Color BG_HEADER        = new Color(0x16, 0x21, 0x3E);
    private static final Color BG_TABLE_ROW     = new Color(0x0F, 0x3D, 0x6D);
    private static final Color BG_TABLE_ALT     = new Color(0x12, 0x2A, 0x4E);
    private static final Color BG_SELECTION     = new Color(0x1A, 0x6D, 0xD4);
    private static final Color TEXT_PRIMARY     = new Color(0xE0, 0xE0, 0xE0);
    private static final Color TEXT_SECONDARY   = new Color(0xA0, 0xA0, 0xB0);
    private static final Color ACCENT_BLUE      = new Color(0x4F, 0xC3, 0xF7);
    private static final Color ACCENT_RED       = new Color(0xEF, 0x53, 0x50);
    private static final Color ACCENT_ORANGE    = new Color(0xFF, 0xA7, 0x26);
    private static final Color ACCENT_GREEN     = new Color(0x66, 0xBB, 0x6A);
    private static final Color BORDER_COLOR     = new Color(0x2A, 0x2A, 0x4E);
    private static final Color BTN_REFRESH_BG   = new Color(0x1E, 0x88, 0xE5);
    private static final Color BTN_REFRESH_HOVER = new Color(0x42, 0xA5, 0xF5);

    private static final Font FONT_TITLE        = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SUBTITLE     = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_TABLE         = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_TABLE_HEADER  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_BUTTON        = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_STATUS        = new Font("Segoe UI", Font.PLAIN, 12);

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // =========================================================================
    // Table columns
    // =========================================================================
    private static final String[] COLUMN_NAMES = {
            "Entity Type", "Entity ID", "Risk Category", "Score",
            "Severity", "Recommended Action", "Generated At"
    };
    private static final int COL_ENTITY_TYPE = 0;
    private static final int COL_ENTITY_ID   = 1;
    private static final int COL_RISK_CAT    = 2;
    private static final int COL_SCORE       = 3;
    private static final int COL_SEVERITY    = 4;
    private static final int COL_ACTION      = 5;
    private static final int COL_GENERATED   = 6;

    // =========================================================================
    // Components
    // =========================================================================
    private final DefaultTableModel tableModel;
    private final JTable riskTable;
    private final TableRowSorter<DefaultTableModel> rowSorter;
    private final JLabel statusLabel;
    private final JButton refreshButton;

    /** Cached risk reports — used for double-click detail dialogs. */
    private List<RiskReportDTO> cachedReports = new ArrayList<>();

    /**
     * Constructs the Risk Dashboard panel with all UI components.
     */
    public RiskDashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // ----- Header panel -----
        add(createHeaderPanel(), BorderLayout.NORTH);

        // ----- Table panel -----
        tableModel = createTableModel();
        riskTable = createRiskTable();
        rowSorter = createRowSorter();
        riskTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(riskTable);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBackground(BG_DARK);
        add(scrollPane, BorderLayout.CENTER);

        // ----- Status bar -----
        statusLabel = new JLabel("  No data loaded. Click Refresh to fetch risk reports.");
        statusLabel.setFont(FONT_STATUS);
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setBackground(BG_HEADER);
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(6, 12, 6, 12)));
        statusLabel.setPreferredSize(new Dimension(0, 32));
        add(statusLabel, BorderLayout.SOUTH);

        // ----- Refresh button reference (created in header) -----
        refreshButton = findRefreshButton();
    }

    // =========================================================================
    // UI Construction
    // =========================================================================

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(BG_DARK);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));

        // Left: title + subtitle
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(BG_DARK);

        JLabel titleLabel = new JLabel("\u26A0  Risk Analysis Dashboard");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(ACCENT_BLUE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titlePanel.add(titleLabel);

        JLabel subtitleLabel = new JLabel("Real-time supply chain risk monitoring and alerting");
        subtitleLabel.setFont(FONT_SUBTITLE);
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setBorder(new EmptyBorder(2, 2, 0, 0));
        titlePanel.add(subtitleLabel);

        header.add(titlePanel, BorderLayout.WEST);

        // Right: Refresh button
        JButton btn = createRefreshButton();
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        btnPanel.setBackground(BG_DARK);
        btnPanel.add(btn);
        header.add(btnPanel, BorderLayout.EAST);

        return header;
    }

    private JButton createRefreshButton() {
        JButton btn = new JButton("\u27F3  Refresh Risk Data");
        btn.setName("refreshButton");
        btn.setFont(FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(BTN_REFRESH_BG);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 36));

        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(BTN_REFRESH_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(BTN_REFRESH_BG);
            }
        });

        btn.addActionListener(e -> refreshRiskData());

        return btn;
    }

    private DefaultTableModel createTableModel() {
        return new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == COL_SCORE) {
                    return Double.class;
                }
                return String.class;
            }
        };
    }

    private JTable createRiskTable() {
        JTable table = new JTable(tableModel);
        table.setFont(FONT_TABLE);
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_DARK);
        table.setSelectionBackground(BG_SELECTION);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(32);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_TABLE_HEADER);
        header.setForeground(ACCENT_BLUE);
        header.setBackground(BG_HEADER);
        header.setBorder(new LineBorder(BORDER_COLOR, 1));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 36));

        // Column widths
        table.getColumnModel().getColumn(COL_ENTITY_TYPE).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_ENTITY_ID).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_RISK_CAT).setPreferredWidth(130);
        table.getColumnModel().getColumn(COL_SCORE).setPreferredWidth(70);
        table.getColumnModel().getColumn(COL_SEVERITY).setPreferredWidth(80);
        table.getColumnModel().getColumn(COL_ACTION).setPreferredWidth(250);
        table.getColumnModel().getColumn(COL_GENERATED).setPreferredWidth(130);

        // Custom renderer for score column colour coding
        table.setDefaultRenderer(Object.class, new RiskTableCellRenderer());
        table.setDefaultRenderer(Double.class, new RiskTableCellRenderer());

        // Double-click handler for detail dialog
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                    showRiskDetailDialog(modelRow);
                }
            }
        });

        return table;
    }

    private TableRowSorter<DefaultTableModel> createRowSorter() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        // Default sort: score descending
        sorter.setSortKeys(List.of(
                new RowSorter.SortKey(COL_SCORE, SortOrder.DESCENDING)));
        return sorter;
    }

    // =========================================================================
    // Data refresh
    // =========================================================================

    /**
     * Invokes the RISK_ANALYSIS agent action via the gateway and populates
     * the table with the returned risk reports.
     */
    public void refreshRiskData() {
        var gateway = App.getAgentGateway();
        if (gateway == null) {
            statusLabel.setText("  \u26A0  Agent platform not available — cannot refresh.");
            statusLabel.setForeground(ACCENT_RED);
            return;
        }

        refreshButton.setEnabled(false);
        refreshButton.setText("\u27F3  Loading...");
        statusLabel.setText("  Fetching risk data from RiskAnalysisAgent...");
        statusLabel.setForeground(TEXT_SECONDARY);

        AgentRequestEnvelope<Void> request = new AgentRequestEnvelope<>(
                AgentActions.RISK_ANALYSIS, 0, 30_000L, null);

        gateway.submit(request).thenAccept(response -> {
            SwingUtilities.invokeLater(() -> populateFromResponse(response));
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("  \u26A0  Error: " + ex.getMessage());
                statusLabel.setForeground(ACCENT_RED);
                refreshButton.setEnabled(true);
                refreshButton.setText("\u27F3  Refresh Risk Data");
            });
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private void populateFromResponse(AgentResponseEnvelope<?> response) {
        try {
            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                statusLabel.setText("  \u26A0  Agent error: " + response.getErrors().get(0));
                statusLabel.setForeground(ACCENT_RED);
                refreshButton.setEnabled(true);
                refreshButton.setText("\u27F3  Refresh Risk Data");
                return;
            }

            // Convert payload to List<RiskReportDTO>
            List<RiskReportDTO> reports = MAPPER.convertValue(
                    response.getPayload(), new TypeReference<List<RiskReportDTO>>() {});

            cachedReports = reports != null ? reports : new ArrayList<>();

            // Populate table
            tableModel.setRowCount(0);
            for (RiskReportDTO report : cachedReports) {
                tableModel.addRow(new Object[]{
                        nvl(report.getEntityType()),
                        nvl(report.getEntityId()),
                        nvl(report.getRiskCategory()),
                        report.getRiskScore(),
                        nvl(report.getSeverity()),
                        nvl(report.getRecommendedAction()),
                        report.getGeneratedAt() != null
                                ? report.getGeneratedAt().format(DT_FORMAT)
                                : "—"
                });
            }

            // Re-apply sort
            rowSorter.sort();

            // Update status bar
            long totalCount = cachedReports.size();
            long highRiskCount = cachedReports.stream()
                    .filter(r -> r.getRiskScore() > 0.7)
                    .count();

            statusLabel.setText(String.format(
                    "  \u2714  %d risk report(s) loaded  |  \u26A0 %d high-risk  |  Last refreshed: %s",
                    totalCount, highRiskCount, LocalDateTime.now().format(DT_FORMAT)));
            statusLabel.setForeground(highRiskCount > 0 ? ACCENT_ORANGE : ACCENT_GREEN);

        } catch (Exception e) {
            statusLabel.setText("  \u26A0  Failed to parse response: " + e.getMessage());
            statusLabel.setForeground(ACCENT_RED);
        } finally {
            refreshButton.setEnabled(true);
            refreshButton.setText("\u27F3  Refresh Risk Data");
        }
    }

    // =========================================================================
    // Detail dialog (double-click)
    // =========================================================================

    private void showRiskDetailDialog(int modelRow) {
        if (modelRow < 0 || modelRow >= cachedReports.size()) {
            return;
        }

        RiskReportDTO report = cachedReports.get(modelRow);

        // Build dialog
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Risk Report Detail", true);
        dialog.setSize(560, 480);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(BG_DARK);
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        // -- Header --
        JLabel headerLabel = new JLabel(
                "\u26A0  " + nvl(report.getRiskCategory()) + " — " + nvl(report.getEntityId()));
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerLabel.setForeground(getScoreColor(report.getRiskScore()));
        content.add(headerLabel, BorderLayout.NORTH);

        // -- Detail grid --
        JPanel detailGrid = new JPanel(new GridBagLayout());
        detailGrid.setBackground(BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        addDetailRow(detailGrid, gbc, row++, "Entity Type", nvl(report.getEntityType()));
        addDetailRow(detailGrid, gbc, row++, "Entity ID", nvl(report.getEntityId()));
        addDetailRow(detailGrid, gbc, row++, "Risk Type", nvl(report.getRiskType()));
        addDetailRow(detailGrid, gbc, row++, "Risk Category", nvl(report.getRiskCategory()));
        addDetailRow(detailGrid, gbc, row++, "Risk Score",
                String.format("%.3f", report.getRiskScore()));
        addDetailRow(detailGrid, gbc, row++, "Severity", nvl(report.getSeverity()));
        addDetailRow(detailGrid, gbc, row++, "Recommended Action",
                nvl(report.getRecommendedAction()));
        addDetailRow(detailGrid, gbc, row++, "Generated At",
                report.getGeneratedAt() != null
                        ? report.getGeneratedAt().format(DT_FORMAT) : "—");

        // -- Contributing factors --
        JPanel factorsPanel = new JPanel(new BorderLayout(0, 6));
        factorsPanel.setBackground(BG_DARK);
        factorsPanel.setBorder(new EmptyBorder(12, 0, 0, 0));

        JLabel factorsTitle = new JLabel("Contributing Factors:");
        factorsTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        factorsTitle.setForeground(ACCENT_BLUE);
        factorsPanel.add(factorsTitle, BorderLayout.NORTH);

        JTextArea factorsArea = new JTextArea();
        factorsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        factorsArea.setBackground(BG_TABLE_ALT);
        factorsArea.setForeground(TEXT_PRIMARY);
        factorsArea.setEditable(false);
        factorsArea.setLineWrap(true);
        factorsArea.setWrapStyleWord(true);
        factorsArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        List<String> factors = report.getContributingFactors();
        List<String> drivers = report.getDrivers();
        StringBuilder sb = new StringBuilder();

        if (factors != null && !factors.isEmpty()) {
            for (int i = 0; i < factors.size(); i++) {
                sb.append(i + 1).append(". ").append(factors.get(i)).append("\n");
            }
        } else {
            sb.append("No contributing factors recorded.\n");
        }

        if (drivers != null && !drivers.isEmpty()) {
            sb.append("\nRisk Drivers:\n");
            for (int i = 0; i < drivers.size(); i++) {
                sb.append("  \u2022 ").append(drivers.get(i)).append("\n");
            }
        }

        factorsArea.setText(sb.toString().trim());
        JScrollPane factorsScroll = new JScrollPane(factorsArea);
        factorsScroll.setBorder(new LineBorder(BORDER_COLOR, 1));
        factorsScroll.setPreferredSize(new Dimension(0, 160));
        factorsPanel.add(factorsScroll, BorderLayout.CENTER);

        // Combine detail grid + factors
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BG_DARK);
        centerPanel.add(detailGrid, BorderLayout.NORTH);
        centerPanel.add(factorsPanel, BorderLayout.CENTER);
        content.add(centerPanel, BorderLayout.CENTER);

        // -- Close button --
        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(FONT_BUTTON);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(BG_HEADER);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(BG_DARK);
        btnPanel.add(closeBtn);
        content.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void addDetailRow(JPanel panel, GridBagConstraints gbc,
                               int row, String label, String value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_SECONDARY);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        val.setForeground(TEXT_PRIMARY);
        panel.add(val, gbc);
        gbc.fill = GridBagConstraints.NONE;
    }

    // =========================================================================
    // Cell Renderer — colour-coded risk scores
    // =========================================================================

    /**
     * Custom cell renderer that applies colour coding to the Score column
     * and alternating row backgrounds for the dark theme.
     */
    private class RiskTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            int modelCol = table.convertColumnIndexToModel(column);
            int modelRow = table.convertRowIndexToModel(row);

            // Default styling
            setFont(FONT_TABLE);
            setBorder(new EmptyBorder(0, 8, 0, 8));

            if (isSelected) {
                c.setBackground(BG_SELECTION);
                c.setForeground(Color.WHITE);
            } else {
                // Alternating row backgrounds
                c.setBackground(row % 2 == 0 ? BG_DARK : BG_TABLE_ALT);
                c.setForeground(TEXT_PRIMARY);
            }

            // Score column — colour coding
            if (modelCol == COL_SCORE) {
                setHorizontalAlignment(SwingConstants.CENTER);

                // Get score value
                Object scoreVal = table.getModel().getValueAt(modelRow, COL_SCORE);
                double score = 0;
                if (scoreVal instanceof Double d) {
                    score = d;
                } else if (scoreVal instanceof Number n) {
                    score = n.doubleValue();
                }

                // Format display
                setText(String.format("%.3f", score));

                if (!isSelected) {
                    Color scoreColor = getScoreColor(score);
                    c.setBackground(new Color(
                            scoreColor.getRed(), scoreColor.getGreen(),
                            scoreColor.getBlue(), 45)); // Semi-transparent tint
                    c.setForeground(scoreColor);
                }
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            // Severity column — coloured text
            if (modelCol == COL_SEVERITY && !isSelected) {
                String severity = value != null ? value.toString() : "";
                switch (severity.toUpperCase()) {
                    case "CRITICAL", "HIGH" -> c.setForeground(ACCENT_RED);
                    case "MEDIUM" -> c.setForeground(ACCENT_ORANGE);
                    case "LOW" -> c.setForeground(ACCENT_GREEN);
                    default -> c.setForeground(TEXT_PRIMARY);
                }
            }

            return c;
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Color getScoreColor(double score) {
        if (score > 0.7) {
            return ACCENT_RED;
        } else if (score >= 0.4) {
            return ACCENT_ORANGE;
        } else {
            return ACCENT_GREEN;
        }
    }

    private static String nvl(String s) {
        return s != null ? s : "—";
    }

    /**
     * Searches for the refresh button in this panel's hierarchy.
     */
    private JButton findRefreshButton() {
        return findComponentByName(this, "refreshButton", JButton.class);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T findComponentByName(
            Container container, String name, Class<T> type) {
        for (Component comp : container.getComponents()) {
            if (type.isInstance(comp) && name.equals(comp.getName())) {
                return (T) comp;
            }
            if (comp instanceof Container sub) {
                T found = findComponentByName(sub, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
