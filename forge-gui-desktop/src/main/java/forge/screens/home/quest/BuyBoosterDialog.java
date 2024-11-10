package forge.screens.home.quest;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.CardEdition;
import forge.gamemodes.quest.QuestSpellShop;
import forge.gui.CardPicturePanel;
import forge.item.*;
import forge.model.FModel;
import forge.toolbox.*;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.ItemPool;
import forge.util.Localizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;

public class BuyBoosterDialog {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private FComboBoxPanel<CardEdition> boosterSet;
    private final FCheckBox boosterBox = new FCheckBox("Booster Box");
    private final FCheckBox boosterPack = new FCheckBox("Booster Pack");
    private final FLabel ownedTextLabel = new FLabel.Builder().text("Owned: ")
            .fontSize(11)
            .build();
    private final FLabel ownedPercentageLabel = new FLabel.Builder().text("0")
            .fontSize(11)
            .build();
    private final FLabel buyAmountLabel = new FLabel.Builder().text("Amount: ")
            .fontSize(11)
            .build();
    private final FLabel unownedLabel = new FLabel.Builder().text("Unowned: ")
            .fontSize(11)
            .build();
    private final FTextField amountToBuy = new FTextField.Builder().text("1").build();
    private JFormattedTextField amountToBuy2;
    private final FLabel costLabel = new FLabel.Builder().text("")
            .fontSize(11)
            .build();
    private final MainPanel panel = new MainPanel();
    private boolean boosterPackSelected;
    private CardEdition edition;
    private CardPicturePanel cardPicturePanel;
    private MissingCardsPanel missingCardsPanel;

    public BuyBoosterDialog(CardEdition edition) {
        this.edition = edition;
        setup();
    }

    public BuyBoosterDialog() {
        setup();
    }

    private void setup() {
        cardPicturePanel = new CardPicturePanel();
        cardPicturePanel.setBackground(Color.DARK_GRAY);
        missingCardsPanel = new MissingCardsPanel(cardPicturePanel);
        Iterable<CardEdition> sets = Iterables.filter(StaticData.instance().getEditions(), CardEdition.Predicates.CAN_MAKE_BOOSTER);
        ArrayList<CardEdition> cardEditions = Lists.newArrayList(sets);
        cardEditions.sort(Comparator.comparing(CardEdition::getName));
        boosterSet = new FComboBoxPanel<>("Edition:", FlowLayout.CENTER, cardEditions);
        if (edition == null) edition = boosterSet.getSelectedItem();
        boosterSet.setSelectedItem(edition);
        boosterPack.setSelected(true);
        boosterPackSelected = true;
        boosterBox.setSelected(false);
        panel.add(boosterSet);
        panel.add(boosterPack);
        panel.add(boosterBox);
        panel.add(ownedPercentageLabel);
        panel.add(ownedTextLabel);
        panel.add(buyAmountLabel);
        panel.add(amountToBuy);
        panel.add(missingCardsPanel);
        panel.add(unownedLabel);
        panel.add(costLabel);
        panel.add(cardPicturePanel);
        panel.setMinimumSize(new Dimension(WIDTH, HEIGHT));
        costLabel.setText(String.valueOf(QuestSpellShop.getCardValue(BoosterPack.fromSet(edition)) * Integer.parseInt(amountToBuy.getText())));
        ownedPercentageLabel.setText(String.format("%d%%", FModel.getQuest().getCards().getCommanderCompletionPercentage(edition.getCode())));
        setScrollContent();
        updateCostAndPicture();
        boosterSet.addActionListener((arg0 -> {
            edition = boosterSet.getSelectedItem();
            ownedPercentageLabel.setText(String.format("%d%%", FModel.getQuest().getCards().getCommanderCompletionPercentage(edition.getCode())));
            if (amountToBuy.getText().isBlank()) {
                amountToBuy.setText("1");
            }
            updateCostAndPicture();
            setScrollContent();
        }));

        amountToBuy.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if ((ke.getKeyChar() >= '0' && ke.getKeyChar() <= '9') || ke.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    amountToBuy.setEditable(true);
                    amountToBuy.setBackground(Color.BLACK);
                } else {
                    amountToBuy.setEditable(false);
                    amountToBuy.setBackground(Color.RED);
                }
            }
        });

        amountToBuy.addChangeListener(new FTextField.ChangeListener() {
            @Override
            public void textChanged() {
                if (!amountToBuy.getText().isBlank()) {
                    costLabel.setText(String.valueOf(QuestSpellShop.getCardValue(BoosterPack.fromSet(edition)) * Integer.parseInt(amountToBuy.getText())));
                }
            }
        });
        boosterPack.addActionListener((arg0 -> {
            if (boosterPack.isSelected()) {
                boosterPackSelected = true;
                boosterBox.setSelected(false);
                updateCostAndPicture();
            } else {
                boosterBox.setSelected(true);
                boosterPackSelected = false;
                updateCostAndPicture();
            }
        }));
        boosterBox.addActionListener((arg0 -> {
            if (boosterBox.isSelected()) {
                boosterPackSelected = false;
                boosterPack.setSelected(false);
                updateCostAndPicture();
            } else {
                boosterPack.setSelected(true);
                boosterPackSelected = true;
                updateCostAndPicture();
            }
        }));
    }

    private void updateCostAndPicture() {
        if (boosterPackSelected) {
            costLabel.setText(String.valueOf(QuestSpellShop.getCardValue(BoosterPack.fromSet(edition)) * Integer.parseInt(amountToBuy.getText())));
            cardPicturePanel.setItem(BoosterPack.fromSet(edition));
        } else {
            costLabel.setText(String.valueOf(QuestSpellShop.getCardValue(BoosterBox.fromSet(edition)) * Integer.parseInt(amountToBuy.getText())));
            cardPicturePanel.setItem(BoosterBox.fromSet(edition));
        }
    }

    private void setScrollContent() {
        missingCardsPanel.reset();
        missingCardsPanel.setEdition(edition);
        Predicate<PaperCard> filter = IPaperCard.Predicates.printedInSet(edition.getCode());
        Iterable<PaperCard> editionCards = Iterables.filter(FModel.getMagicDb().getCommonCards().getAllCards(), filter);

        ItemPool<PaperCard> ownedCards = FModel.getQuest().getAssets().getCardPool();
        ItemPool<PaperCard> unownedCards = new ItemPool<>(PaperCard.class);
        for (PaperCard card : editionCards) {
            if (!ownedCards.contains(card)) {
                unownedCards.add(card, 1);
            }
        }

        java.util.List<PaperCard> unownedList = unownedCards.toFlatList();
        try {
            unownedList.sort(Comparator.comparingInt(a -> Integer.parseInt(a.getCollectorNumber().replaceAll("\\D", ""))));
        } catch (Exception ignored) {}
        for (PaperCard card : unownedList) {
            missingCardsPanel.addCardEntry(card.getCollectorNumber() + " | " + card.getRarity() + " | " + card.getCardName());
        }
    }

    public BoosterPurchaseData show() {
        FOptionPane optionPane = new FOptionPane(null, "Buy Booster Pack", null, panel, ImmutableList.of(Localizer.getInstance().getMessage("lblOK"), Localizer.getInstance().getMessage("lblCancel")), 0);
        panel.revalidate();
        panel.repaint();
        optionPane.setVisible(true);
        int result = optionPane.getResult();

        optionPane.dispose();

        if (result == 0) {
            ItemPool<InventoryItem> items = new ItemPool<>(InventoryItem.class);

            if (boosterPackSelected) {
                items.add(BoosterPack.fromSet(edition), amountToBuy.getText().isEmpty() ? 1 : Integer.parseInt(amountToBuy.getText()));
            } else {
                items.add(BoosterBox.fromSet(edition), amountToBuy.getText().isEmpty() ? 1 : Integer.parseInt(amountToBuy.getText()));
            }
            BoosterPurchaseData data = new BoosterPurchaseData();
            data.edition = edition;
            data.items = items;
            return data;
        }
        return null;
    }

    private class MainPanel extends SkinnedPanel {
        private MainPanel() {
            super(null);
            setOpaque(false);
        }

        @Override
        public void doLayout() {
            int w = 200;
            int h = FTextField.HEIGHT;
            LayoutHelper helper = new LayoutHelper(this);
            helper.include(boosterPack, w, h);
            helper.offset(100, 0);
            helper.include(boosterBox, w, h);
            helper.newLine();
            helper.fillLine(boosterSet, h);
            helper.newLine();
            helper.include(ownedTextLabel, 100, h);
            helper.offset(10, 0);
            helper.include(ownedPercentageLabel, 50, h);
            helper.offset(10, 0);
            helper.include(buyAmountLabel, 100, h);
            helper.offset(10, 0);
            helper.include(amountToBuy, 100, h);
            helper.offset(10, 0);
            helper.include(costLabel, 100, h);
            helper.newLine();
            helper.include(unownedLabel, 100, h);
            helper.newLine();
            helper.include(missingCardsPanel, getWidth() / 2 - 10, 500);
            helper.offset(10, 0);
            helper.include(cardPicturePanel, getWidth() / 2 - 10, 500);
        }
    }
}
