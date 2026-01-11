package pharma.gui.components;

import pharma.model.Drug;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Autocomplete search field for drugs with dropdown suggestions.
 * Filters drugs as user types and displays matching results in a popup.
 */
public class DrugSearchField extends JTextField {

    private List<Drug> allDrugs;
    private JPopupMenu suggestionPopup;
    private JList<String> suggestionList;
    private DefaultListModel<String> listModel;
    private DrugSelectionListener selectionListener;
    private int maxSuggestions = 10;

    /**
     * Interface for handling drug selection events
     */
    public interface DrugSelectionListener {
        void onDrugSelected(Drug drug);
    }

    public DrugSearchField(List<Drug> drugs) {
        this.allDrugs = drugs != null ? drugs : new ArrayList<>();
        initializeComponents();
        setupListeners();
    }

    private void initializeComponents() {
        // Create popup menu for suggestions
        suggestionPopup = new JPopupMenu();
        suggestionPopup.setFocusable(false);

        // Create list model and list for suggestions
        listModel = new DefaultListModel<>();
        suggestionList = new JList<>(listModel);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(Math.min(maxSuggestions, 10));

        // Style the suggestion list
        suggestionList.setFont(new Font("Arial", Font.PLAIN, 12));
        suggestionList.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Add list to scroll pane
        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(null);
        suggestionPopup.add(scrollPane);

        // Set placeholder text
        setToolTipText("Type drug name, code, or generic name to search...");
    }

    private void setupListeners() {
        // Document listener to filter as user types
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterAndShowSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterAndShowSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterAndShowSuggestions();
            }
        });

        // Mouse listener for suggestion selection
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    selectSuggestion();
                }
            }
        });

        // Key listener for keyboard navigation
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (suggestionPopup.isVisible()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            // Move selection down
                            int currentIndex = suggestionList.getSelectedIndex();
                            if (currentIndex < listModel.getSize() - 1) {
                                suggestionList.setSelectedIndex(currentIndex + 1);
                                suggestionList.ensureIndexIsVisible(currentIndex + 1);
                            }
                            e.consume();
                            break;

                        case KeyEvent.VK_UP:
                            // Move selection up
                            int index = suggestionList.getSelectedIndex();
                            if (index > 0) {
                                suggestionList.setSelectedIndex(index - 1);
                                suggestionList.ensureIndexIsVisible(index - 1);
                            }
                            e.consume();
                            break;

                        case KeyEvent.VK_ENTER:
                            // Select current suggestion
                            if (suggestionList.getSelectedIndex() != -1) {
                                selectSuggestion();
                                e.consume();
                            }
                            break;

                        case KeyEvent.VK_ESCAPE:
                            // Close popup
                            suggestionPopup.setVisible(false);
                            e.consume();
                            break;
                    }
                }
            }
        });

        // Focus listener to hide popup when focus is lost
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Delay hiding to allow click on suggestion
                SwingUtilities.invokeLater(() -> {
                    if (!suggestionList.hasFocus()) {
                        suggestionPopup.setVisible(false);
                    }
                });
            }
        });
    }

    private void filterAndShowSuggestions() {
        String searchText = getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            suggestionPopup.setVisible(false);
            return;
        }

        // Filter drugs based on search text
        listModel.clear();
        List<Drug> matchedDrugs = new ArrayList<>();

        for (Drug drug : allDrugs) {
            if (matchesDrug(drug, searchText)) {
                matchedDrugs.add(drug);
                String displayText = formatDrugForDisplay(drug);
                listModel.addElement(displayText);

                if (matchedDrugs.size() >= maxSuggestions) {
                    break;
                }
            }
        }

        // Show popup if there are suggestions
        if (!listModel.isEmpty()) {
            // Adjust popup size
            int width = getWidth();
            int height = suggestionList.getPreferredSize().height;
            suggestionPopup.setPopupSize(width, Math.min(height + 5, 250));

            // Show popup below the text field
            if (!suggestionPopup.isVisible()) {
                suggestionPopup.show(this, 0, getHeight());
            }

            // Auto-select first item
            suggestionList.setSelectedIndex(0);
        } else {
            suggestionPopup.setVisible(false);
        }
    }

    private boolean matchesDrug(Drug drug, String searchText) {
        // Check if search text matches any drug property
        return (drug.getBrandName() != null && drug.getBrandName().toLowerCase().contains(searchText)) ||
                (drug.getGenericName() != null && drug.getGenericName().toLowerCase().contains(searchText)) ||
                (drug.getMaterialCode() != null && drug.getMaterialCode().toLowerCase().contains(searchText)) ||
                (drug.getManufacturer() != null && drug.getManufacturer().toLowerCase().contains(searchText));
    }

    private String formatDrugForDisplay(Drug drug) {
        // Format: "Brand Name (Generic Name) - Code"
        StringBuilder sb = new StringBuilder();
        sb.append(drug.getBrandName());

        if (drug.getGenericName() != null && !drug.getGenericName().isEmpty()) {
            sb.append(" (").append(drug.getGenericName()).append(")");
        }

        sb.append(" - ").append(drug.getMaterialCode());

        return sb.toString();
    }

    private void selectSuggestion() {
        int selectedIndex = suggestionList.getSelectedIndex();
        if (selectedIndex != -1) {
            String selectedText = listModel.getElementAt(selectedIndex);

            // Extract material code from the formatted string
            String materialCode = extractMaterialCode(selectedText);

            // Find the drug object
            Drug selectedDrug = findDrugByCode(materialCode);

            if (selectedDrug != null) {
                // Set text field to brand name
                setText(selectedDrug.getBrandName());

                // Notify listener
                if (selectionListener != null) {
                    selectionListener.onDrugSelected(selectedDrug);
                }
            }

            suggestionPopup.setVisible(false);
        }
    }

    private String extractMaterialCode(String displayText) {
        // Extract code from format: "Brand Name (Generic Name) - CODE"
        int dashIndex = displayText.lastIndexOf(" - ");
        if (dashIndex != -1) {
            return displayText.substring(dashIndex + 3).trim();
        }
        return "";
    }

    private Drug findDrugByCode(String materialCode) {
        for (Drug drug : allDrugs) {
            if (drug.getMaterialCode().equals(materialCode)) {
                return drug;
            }
        }
        return null;
    }

    // Public methods

    public void setDrugList(List<Drug> drugs) {
        this.allDrugs = drugs != null ? drugs : new ArrayList<>();
    }

    public void setSelectionListener(DrugSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setMaxSuggestions(int max) {
        this.maxSuggestions = max;
    }

    public void clearSelection() {
        setText("");
        suggestionPopup.setVisible(false);
    }
}
