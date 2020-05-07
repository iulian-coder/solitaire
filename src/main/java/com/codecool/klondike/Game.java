package com.codecool.klondike;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;

    MenuItem menuItem1 = new MenuItem("Undo Move - Beta");
    MenuItem menuItem2 = new MenuItem("Change Theme");
    MenuItem menuItem3 = new MenuItem("Restart");
    MenuItem menuItem4 = new MenuItem("Exit");

    MenuButton menuButton = new MenuButton("Menu", null, menuItem1, menuItem2, menuItem3, menuItem4);

    private List<Card> lastMoveList = new ArrayList<>();

    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card == stockPile.getTopCard()) {
            if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
                card.moveToPile(discardPile);
                card.flip();
                card.setMouseTransparent(false);
                lastMoveList.add(card);
                System.out.println("Placed " + card + " to the waste.");
            }
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        if (!card.isFaceDown()) {
            Pile activePile = card.getContainingPile();
            ObservableList<Card> draggedPile = card.getContainingPile().getCards();
            if (activePile.getPileType() == Pile.PileType.STOCK)
                return;
            if (activePile.getPileType() == Pile.PileType.DISCARD)
                if (card != discardPile.getTopCard())
                    return;

            double offsetX = e.getSceneX() - dragStartX;
            double offsetY = e.getSceneY() - dragStartY;

            draggedCards.clear();
            draggedCards.add(card);

            try {
                for (int i = draggedPile.indexOf(card) + 1; i < draggedPile.size(); i++) {
                    draggedCards.add(activePile.getCards().get(i));
                }
            } catch (IndexOutOfBoundsException ex) {
                System.out.println("Out of index");
            }

            for (Card draggedCard : draggedCards) {
                draggedCard.getDropShadow().setRadius(20);
                draggedCard.getDropShadow().setOffsetX(10);
                draggedCard.getDropShadow().setOffsetY(10);

                draggedCard.toFront();
                draggedCard.setTranslateX(offsetX);
                draggedCard.setTranslateY(offsetY);
            }
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, tableauPiles);
        Pile pileFoundation = getValidIntersectingPile(card, foundationPiles);

        if (pile != null) {
            isMoveValid(card, pile);
            handleValidMove(card, pile);
            isGameWon();
        } else if (pileFoundation != null) {
            isMoveValid(card, pileFoundation);
            handleValidMove(card, pileFoundation);
            isGameWon();
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
    };


    public void isGameWon() {
        if (stockPile.isEmpty()) {
//                discardPile.isEmpty()) {
//                tableauPiles.isEmpty()) {
            System.out.println("Congratulations! You Won!");
            Popup.display("Congratulations! You Won!");
        }
    }

    public Game() {
        deck = Card.createNewDeck();
        Collections.shuffle(deck);
        getChildren().add(menuButton);
        initPiles();
        dealCards();
        addMenuEventHandlers();
    }

    public void restartGame(){
        Klondike game = new Klondike();
        game.start(Klondike.stage);
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void addMenuEventHandlers(){
        // e (Event) option selected via Lambda
        menuItem1.setOnAction((e -> undoMove()));
        menuItem2.setOnAction((e -> changeTheme()));
        menuItem3.setOnAction((e -> restartGame()));
        menuItem4.setOnAction((e -> Platform.exit()));

    }

    public void undoMove(){
        try {
            int lastIndex = lastMoveList.size()-1;
                lastMoveList.get(lastIndex).flip();
                lastMoveList.get(lastIndex).moveToPile(stockPile);
                lastMoveList.clear();
        }catch (Exception e){
            Popup.display("Out of last move !");
        }
    }

    public void changeTheme(){
        setTableBackground(new Image("/table/multicolor.png"));
        for (Card card: deck) {
            card.changeThemeCards(new Image("card_images/card_back_cool.png"));
        }
    }

    public void refillStockFromDiscard() {
        for (Card card : deck){
            if (card.getContainingPile().getPileType() == Pile.PileType.DISCARD){
                card.moveToPile(stockPile);
                card.flip();
            }
        }
        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        if (destPile.isEmpty() && destPile.getPileType().equals(Pile.PileType.TABLEAU))
            if (card.getRank().equals(Rank.KING)) {
                return true;
            }
        if (!destPile.isEmpty() && destPile.getPileType().equals(Pile.PileType.TABLEAU))
            if (card.isOppositeColor(card, destPile.getTopCard()) && destPile.getTopCard().getRank().getRankNum() == card.getRank().getRankNum()+1) {
                return true;
            }
        if (destPile.isEmpty() && destPile.getPileType().equals(Pile.PileType.FOUNDATION))
            if (card.getRank().equals(Rank.ACE)) {
                return true;
            }
        if (!destPile.isEmpty() && destPile.getPileType().equals(Pile.PileType.FOUNDATION))
            if (Card.isSameSuit(card, destPile.getTopCard()) && destPile.getTopCard().getRank().getRankNum() == card.getRank().getRankNum()-1) {
                return true;
            }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        Iterator<Card> deckIterator = deck.iterator();

        for (int i=0; i<tableauPiles.size(); i++) {
            Pile pile = tableauPiles.get(i);
            if (i>0) {
                for (int j=0; j<i+1; j++) {
                    Card cardBeingPlaced = deckIterator.next();
                    pile.addCard(cardBeingPlaced);
                    addMouseEventHandlers(cardBeingPlaced);
                    cardBeingPlaced.setContainingPile(pile);
                    if (j == i) {
                        cardBeingPlaced.flip();
                    }
                    getChildren().add(cardBeingPlaced);
                }
            } else {
                Card cardBeingPlaced = deckIterator.next();
                pile.addCard(cardBeingPlaced);
                addMouseEventHandlers(cardBeingPlaced);
                cardBeingPlaced.setContainingPile(pile);
                cardBeingPlaced.flip();
                getChildren().add(cardBeingPlaced);
            }
        }

        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });

    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    public static void autoFlip(Pile pile) {
        Card topCard = pile.getTopCard();
        if (topCard != null && topCard.isFaceDown()) {
            topCard.flip();
        }
    }

}
