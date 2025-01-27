/**
 * 
 */
package com.xklakoux.freecell;

import java.lang.reflect.Type;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.view.MotionEventCompat;

import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.xklakoux.freecell.enums.Rank;
import com.xklakoux.freecell.enums.Suit;

/**
 * @author artur
 * 
 */
public class Card extends ImageView {

	public final String TAG = Card.class.getSimpleName();

	private Suit suit;
	private Rank number;
	private boolean faceup = false;
	private final int reverseResourceId = (Utils.getResId(
			"reverse_" + Game.getSettings().getString(SettingsConstant.REVERSE, SettingsConstant.DEFAULT_REVERSE),
			R.drawable.class));

	public Card(Context context, Suit suit, Rank number) {
		super(context);
		this.suit = suit;
		this.number = number;
		setAdjustViewBounds(true);
		setImageResource(reverseResourceId);
		setOnTouchListener(new CardTouchListener());
		setId(Game.getUniqueId());
	}

	public Card(Context context, Suit suit, Rank number, boolean faceUp) {
		super(context);
		this.suit = suit;
		this.number = number;
		faceup = faceUp;
		setAdjustViewBounds(true);
		setOnTouchListener(new CardTouchListener());
		// setId(App.getUniqueId());
	}

	public Card(Context context, Card card) {
		super(context);
		suit = card.getSuit();
		number = card.getNumber();
		faceup = true;

	}

	public Suit getSuit() {
		return suit;
	}

	public void setSuit(Suit suit) {
		this.suit = suit;
	}

	public Rank getNumber() {
		return number;
	}

	public void setNumber(Rank number) {
		this.number = number;
	}

	public boolean isFaceup() {
		return faceup;
	}

	public void setFaceup(boolean faceup) {
		this.faceup = faceup;
		if (faceup) {
			String index = Game.getSettings().getString(SettingsConstant.CARD_SET, SettingsConstant.DEFAULT_CARD_SET);
			setImageResource(Utils.getResId(suit.getName() + "_" + number.getId() + "_" + index, R.drawable.class));
		} else {
			String index = Game.getSettings().getString(SettingsConstant.REVERSE, SettingsConstant.DEFAULT_REVERSE);
			setImageResource(Utils.getResId("reverse_" + index, R.drawable.class));
		}
	}

	class CardTouchListener implements OnTouchListener {

		public final String TAG = CardTouchListener.class.getSimpleName();

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			final int action = MotionEventCompat.getActionMasked(event);

			switch (action) {
			case MotionEvent.ACTION_DOWN:

				BasePile owner = (BasePile) v.getParent();
				int index = owner.indexOfChild(v);
				Card card = (Card) v;

				if (isValidMove((Card) v)) {
					ClipData data = ClipData.newPlainText("", "");
					for (int i = 0; i < index; i++) {
						owner.getChildAt(i).setVisibility(View.INVISIBLE);
					}
					DragShadowBuilder shadowBuilder = new MyDragShadowBuilder(owner, card, event);
					if (v.startDrag(data, shadowBuilder, v, 0)) {
						for (int i = index; i < owner.getChildCount(); i++) {
							owner.getChildAt(i).setVisibility(View.INVISIBLE);
						}
					} else {
						for (int i = index; i < owner.getChildCount(); i++) {
							owner.getChildAt(i).setVisibility(View.VISIBLE);
						}
					}
					for (int i = 0; i < index; i++) {
						owner.getChildAt(i).setVisibility(View.VISIBLE);
					}
					return true;
				}
			}
			return true;
		}

		boolean isValidMove(Card selectedCard) {
			ViewGroup owner = (ViewGroup) selectedCard.getParent();
			int index = owner.indexOfChild(selectedCard);

			if (!selectedCard.isFaceup()) {
				return false;
			}

			Card referenceCard = selectedCard;
			for (int i = index + 1; i < owner.getChildCount(); i++) {
				Card card = (Card) owner.getChildAt(i);

				if (((referenceCard.getSuit() == Suit.CLUBS || referenceCard.getSuit() == Suit.SPADES) && (card.getSuit() == Suit.HEARTS || card.getSuit() == Suit.DIAMONDS))
						|| ((referenceCard.getSuit() == Suit.HEARTS || referenceCard.getSuit() == Suit.DIAMONDS) && (card.getSuit() == Suit.SPADES || card.getSuit() == Suit.CLUBS))) {
					if (referenceCard.getNumber().getId() == card.getNumber().getId()+1) {
						referenceCard = card;
						continue;
					}
				}
				return false;
			}
			return true;


		}

	}

	private class MyDragShadowBuilder extends View.DragShadowBuilder {

		BasePile pile;
		Card card;
		MotionEvent event;

		private MyDragShadowBuilder(BasePile pile, Card card, MotionEvent event) {
			super(pile);
			this.pile = pile;
			this.card = card;
			this.event = event;
		}

		@Override
		public void onProvideShadowMetrics(Point size, Point touch) {

			int width;
			int height;

			width = getView().getWidth();
			height = getView().getHeight();
			size.set(width, height);
			touch.set((int) event.getX(), calculateMarginTop(pile, card) + (int) event.getY());

		}

		private int calculateMarginTop(BasePile pile, Card cardStart) {
			int marginTop = 0;
			if (pile instanceof Pile) {
				for (int i = 0; i < pile.indexOfCard(cardStart); i++) {
					Card card = pile.getCardAt(i);
					float tempMargin = card.isFaceup() ? getResources().getDimension(R.dimen.card_stack_margin_up)
							: getResources().getDimension(R.dimen.card_stack_margin_down);
					marginTop += tempMargin;
				}
			}
			return marginTop;
		}
	}
}

class CardInstanceCreator implements InstanceCreator<Card> {

	@Override
	public Card createInstance(Type arg0) {
		return new Card(Game.getAppContext(), Suit.SPADES, Rank.ACE);
	}

}

class CardSerializer implements JsonSerializer<Card> {

	@Override
	public JsonElement serialize(Card arg0, Type arg1, JsonSerializationContext arg2) {
		final JsonObject json = new JsonObject();
		json.addProperty("suit", arg0.getSuit().ordinal());
		json.addProperty("number", arg0.getNumber().ordinal());
		json.addProperty("faceup", arg0.isFaceup());
		return json;
	}

}

class CardDeserializer implements JsonDeserializer<Card> {

	@Override
	public Card deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
		JsonObject object = arg0.getAsJsonObject();
		Suit suit = Suit.values()[object.get("suit").getAsInt()];
		Rank number = Rank.values()[object.get("number").getAsInt()];
		Boolean faceUp = object.get("faceup").getAsBoolean();
		Card card = new Card(Game.getAppContext(), suit, number, faceUp);
		return card;
	}
}