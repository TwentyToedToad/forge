package forge.screens.home.quest;

import forge.StaticData;
import forge.card.CardEdition;
import forge.gui.CardPicturePanel;
import forge.gui.MouseUtil;
import forge.item.PaperCard;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.LayerUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.MouseEvent;

public class MissingCardsPanel extends JPanel {
    private FScrollPane scrollPane;
    private MyScrollablePanel scrollablePanel;
    private final FSkin.SkinFont textFont = FSkin.getFont();
    private final CardPicturePanel cardPicturePanel;
    private CardEdition edition;
    private final LayerUI<FScrollPane> layerUI = new GameLogPanelLayerUI();
    private JLayer<FScrollPane> layer;
    private boolean isScrollBarVisible = false;

    public MissingCardsPanel(CardPicturePanel picturePanel) {
        setMyLayout();
        createScrollablePanel();
        addNewScrollPane();
        cardPicturePanel = picturePanel;
    }

    public void reset() {
        scrollablePanel.removeAll();
        scrollablePanel.validate();
        edition = null;
    }

    public void setEdition(CardEdition edition) {
        this.edition = edition;
    }

    protected void setVerticalScrollbarVisibility() {
        if (isScrollBarVisible) {
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        } else {
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        }
        forceVerticalScrollbarToMin();
    }

    private void setMyLayout() {
        setLayout(new MigLayout("insets 0"));
        setOpaque(false);
    }

    private void forceVerticalScrollbarToMin() {
        scrollPane.validate();
        SwingUtilities.invokeLater(() -> {
            final JScrollBar scrollbar = scrollPane.getVerticalScrollBar();
            scrollbar.setValue(scrollbar.getMinimum());
            // This is needed to ensure scrollbar is set to max correctly.
            scrollPane.validate();
            scrollbar.setValue(scrollbar.getMinimum());
        });
    }

    /**
     * Creates a transparent scroll pane that handles the scrolling
     * characteristics for the list of {@code JTextArea} log entries.
     */
    private void addNewScrollPane() {
        scrollPane = new FScrollPane(false);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().add(scrollablePanel);
        layer = new JLayer<>(scrollPane, layerUI);
        this.add(layer, "w 10:100%, h 100%");
    }

    /**
     * Creates a {@code Scrollable JPanel} that works better with
     * {@code FScrollPane} than the standard {@code JPanel}.
     * <p>
     * This manages the layout and display of the list of {@code JTextArea} log entries.
     * <p>
     * <b>MigLayout Settings</b><ul>
     * <li>{@code insets 0} = No margins
     * <li>{@code gap 0} = no gap between cells
     * <li>{@code flowy} = vertical flow mode - add each new cell below the previous cell.
     */
    private void createScrollablePanel() {
        scrollablePanel = new MyScrollablePanel();
        scrollablePanel.setLayout(new MigLayout("insets 0, gap 0, flowy"));
        scrollablePanel.setBackground(Color.DARK_GRAY);
    }

    public void addCardEntry(final String text) {
        final boolean useAlternateBackColor = (scrollablePanel.getComponents().length % 2 == 0);
        final JTextArea tar = createNewCardEntryJTextArea(text, useAlternateBackColor);

        // If the minimum is not specified then the JTextArea will
        // not be sized correctly using MigLayout.
        // (http://stackoverflow.com/questions/6023145/line-wrap-in-a-jtextarea-causes-jscrollpane-to-missbehave-with-miglayout)
        scrollablePanel.add(tar);

        // Automatically hide scrollbar (if visible).
        if (isScrollBarVisible) {
            isScrollBarVisible = false;
            setVerticalScrollbarVisibility();
        }

        forceVerticalScrollbarToMin();
    }

    private JTextArea createNewCardEntryJTextArea(final String text, final boolean useAlternateBackColor) {
        final FSkin.SkinnedTextArea tar = new FSkin.SkinnedTextArea(text);
        tar.setFont(textFont);
        tar.setBorder(new EmptyBorder(3, 4, 3, 4));
        tar.setFocusable(true);
        tar.setEditable(false);
        tar.setLineWrap(false);
        tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        FSkin.SkinColor skinColor = FSkin.getColor(FSkin.Colors.CLR_ZEBRA);
        if (useAlternateBackColor) { skinColor = skinColor.darker(); }
        tar.setOpaque(true);
        tar.setBackground(skinColor);

        return tar;
    }

    protected final class MyScrollablePanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return textFont.getSize();
        }

        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            return textFont.getSize();
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        // we don't want to track the height, because we want to scroll vertically.
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    protected final class GameLogPanelLayerUI extends LayerUI<FScrollPane> {

        @SuppressWarnings("unchecked")
        @Override
        public void installUI(final JComponent c) {
            super.installUI(c);
            final JLayer<FScrollPane> l = (JLayer<FScrollPane>)c;
            l.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void uninstallUI(final JComponent c) {
            super.uninstallUI(c);
            final JLayer<FScrollPane> l = (JLayer<FScrollPane>)c;
            l.setLayerEventMask(0);
        }

        @Override
        protected void processMouseEvent(final MouseEvent e, final JLayer<? extends FScrollPane> l) {

            final boolean isScrollBarRequired = scrollPane.getVerticalScrollBar().getMaximum() > getHeight();
            final boolean isHoveringOverLogEntry = e.getSource() instanceof JTextArea;

            switch (e.getID()) {
                case MouseEvent.MOUSE_ENTERED:
                    if (isScrollBarRequired && isHoveringOverLogEntry) {
                        MouseUtil.setCursor(Cursor.HAND_CURSOR);
                    }
                    break;
                case MouseEvent.MOUSE_EXITED:
                    MouseUtil.resetCursor();
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    if (isHoveringOverLogEntry) {
                        String[] cardData = ((JTextArea) e.getSource()).getText().split(" \\| ");
                        String name = cardData[2];
                        String number = cardData[0];
                        PaperCard card = StaticData.instance().fetchCard(name, edition.getCode(), number);
                        cardPicturePanel.setItem(card);
                        int pos = ((JTextArea) e.getSource()).getText().length();
                        try {
                            resetAllColors();
                            ((JTextArea) e.getSource()).getHighlighter().addHighlight(0, pos, new DefaultHighlighter.DefaultHighlightPainter(Color.BLACK));
                        } catch (BadLocationException ignored) {}
                    }
                    break;
            }
        }

        private void resetAllColors() {
            for (Component comp : scrollablePanel.getComponents()) {
                ((JTextArea) comp).getHighlighter().removeAllHighlights();
            }
        }
    }
}
