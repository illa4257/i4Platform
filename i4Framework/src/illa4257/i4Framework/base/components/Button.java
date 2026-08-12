package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.IMoveableInputEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchUpEvent;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.graphics.Sprite;
import illa4257.i4Framework.base.styling.PropIter;
import illa4257.i4Framework.base.styling.AdvancedSpriteAlign;
import illa4257.i4Framework.base.styling.StyleProperty;
import illa4257.i4Utils.math.Vector2;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.Context;
import illa4257.i4Framework.base.styling.HorizontalAlign;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;

import java.util.*;

import static illa4257.i4Framework.base.styling.HorizontalAlign.*;

public class Button extends Component {
    private final Object textLocker = new Object();
    private volatile Object text;
    public volatile Object font = null;

    public Button() { this(null); }
    public Button(final Object text) {
        this.text = text;
        setFocusable(true);
        final EventListener<IMoveableInputEvent> ml = e -> {
            if (
                    !isEnabled() ||
                    e.x() < 0 || e.x() > width.calcFloat() ||
                            e.y() < 0 || e.y() > height.calcFloat()
            )
                return;
            fire(new ActionEvent(this, e.isSystem()));
        };
        addEventListener(MouseUpEvent.class, ml::run);
        addEventListener(TouchUpEvent.class, ml::run);
    }

    public void setText(final Object text) {
        synchronized (textLocker) {
            if (Objects.equals(this.text, text))
                return;
            final Object old = this.text;
            this.text = text;
            fire(new ChangeTextEvent(this, old, text));
        }
    }

    public static final List<String> spriteProperties = Arrays.asList("sprite", "image");

    private static class S {
        public final Sprite sprite;
        public final AdvancedSpriteAlign align;
        public float w = 0;

        public S(final Sprite sprite, final AdvancedSpriteAlign align) {
            this.sprite = sprite;
            this.align = align;
        }
    }

    @Override
    public void paint(final Context ctx) {
        super.paint(ctx);
        final float h = height.calcFloat();
        final Object te = text;
        final PropIter ss = getPI(), ss2 = getPI2();

        ss.select("color", StyleProperty.paintFilter).nextLayer().nextSet();
        final Paint c = ss.paint(Color.TRANSPARENT);
        final String t = te != null ? String.valueOf(te) : "";
        ctx.setPaint(c);
        final Object f = font;
        if (f != null)
            ctx.setFont(f);

        ss.select("text-align", HorizontalAlign.class).nextLayer().nextSet();
        final HorizontalAlign textAlign = ss.e(HorizontalAlign.class, LEFT);

        final Vector2 s = ctx.bounds(t);

        ss.select(spriteProperties, StyleProperty.spriteFilter).nextLayer().nextSet();
        ss2.select("sprite-align", AdvancedSpriteAlign.class).nextLayer().nextSet();
        final Stack<S> horizontalSprites = new Stack<>(), textSprites = new Stack<>(), topSprites = new Stack<>();
        while (ss.hasNext()) {
            final Sprite n = ss.sprite(null);
            final AdvancedSpriteAlign spriteAlign = ss2.e(AdvancedSpriteAlign.class, AdvancedSpriteAlign.LEFT);
            if (n == null)
                continue;
            if (spriteAlign == AdvancedSpriteAlign.TOP)
                topSprites.push(new S(n, null));
            else if (spriteAlign == AdvancedSpriteAlign.LEFT || spriteAlign == AdvancedSpriteAlign.RIGHT)
                horizontalSprites.push(new S(n, spriteAlign));
            else if (spriteAlign == AdvancedSpriteAlign.LEFT_OF_TEXT || spriteAlign == AdvancedSpriteAlign.RIGHT_OF_TEXT)
                textSprites.push(new S(n, spriteAlign));
        }

        final float gap = 8 * dp.calcFloat();
        float sx = gap, ex = width.calcFloat() - gap, topOffset = (h - s.y) / 2, mh = s.y;
        if (!topSprites.isEmpty()) {
            final float topHeight;
            if (t.isEmpty() && horizontalSprites.isEmpty() && textSprites.isEmpty())
                topHeight = h - gap * 2;
            else
                topHeight = h - mh - gap * 3;
            topOffset = gap * 2 + topHeight;
            float tw = -gap;
            for (final S sprite : topSprites) {
                final float iw = sprite.sprite.getWidth() / sprite.sprite.getHeight() * topHeight;
                sprite.w = iw;
                tw += iw + gap;
            }
            tw = (width.calcFloat() - tw) / 2;
            for (final S sprite : topSprites) {
                ctx.drawSprite(sprite.sprite, tw, gap, sprite.w, topHeight);
                tw += sprite.w + gap;
            }
            topSprites.clear();
        }

        for (final S sprite : horizontalSprites) {
            final float iw = sprite.sprite.getWidth() / sprite.sprite.getHeight() * mh;
            if (sprite.align == AdvancedSpriteAlign.LEFT) {
                ctx.drawSprite(sprite.sprite, sx, topOffset, iw, mh);
                sx += iw + gap;
            } else {
                ex -= iw;
                ctx.drawSprite(sprite.sprite, ex, topOffset, iw, mh);
                ex -= gap;
            }
        }
        horizontalSprites.clear();

        if (textAlign == LEFT) {
            Iterator<S> iter = textSprites.iterator();
            while (iter.hasNext()) {
                final S sprite = iter.next();
                if (sprite.align != AdvancedSpriteAlign.LEFT_OF_TEXT)
                    continue;
                final float iw = sprite.sprite.getWidth() / sprite.sprite.getHeight() * mh;
                ctx.drawSprite(sprite.sprite, sx, topOffset, iw, mh);
                sx += iw + gap;
                iter.remove();
            }

            ctx.setPaint(c);
            if (f != null)
                ctx.setFont(f);
            ctx.drawString(t, sx, topOffset);
            sx += s.x + gap;

            iter = textSprites.iterator();
            while (iter.hasNext()) {
                final S sprite = iter.next();
                final float iw = sprite.sprite.getWidth() / sprite.sprite.getHeight() * mh;
                ctx.drawSprite(sprite.sprite, sx, topOffset, iw, mh);
                sx += iw + gap;
                iter.remove();
            }
        } else if (textAlign == RIGHT) {
            Iterator<S> iter = textSprites.iterator();
            while (iter.hasNext()) {
                final S sprite = iter.next();
                if (sprite.align != AdvancedSpriteAlign.RIGHT_OF_TEXT)
                    continue;
                final float iw = sprite.sprite.getWidth() / sprite.sprite.getHeight() * mh;
                ex -= iw;
                ctx.drawSprite(sprite.sprite, ex, topOffset, iw, mh);
                ex -= gap;
                iter.remove();
            }

            ctx.setPaint(c);
            if (f != null)
                ctx.setFont(f);
            ex -= s.x;
            ctx.drawString(t, ex, topOffset);
            ex -= gap;

            iter = textSprites.iterator();
            while (iter.hasNext()) {
                final S sprite = iter.next();
                final float iw = sprite.sprite.getWidth() / sprite.sprite.getHeight() * mh;
                ex -= iw;
                ctx.drawSprite(sprite.sprite, ex, topOffset, iw, mh);
                ex -= gap;
                iter.remove();
            }
        } else if (textAlign == CENTER) {
            float contentWidth = s.x;
            for (final S sprite : textSprites) {
                final float iw = sprite.sprite.getWidth() / sprite.sprite.getHeight() * mh;
                sprite.w = iw;
                contentWidth += iw + gap;
            }

            float x = (width.calcFloat() - contentWidth) / 2;

            Iterator<S> iter = textSprites.iterator();
            while (iter.hasNext()) {
                final S sprite = iter.next();
                if (sprite.align != AdvancedSpriteAlign.LEFT_OF_TEXT)
                    continue;
                final float iw = sprite.sprite.getWidth() / sprite.sprite.getHeight() * mh;
                ctx.drawSprite(sprite.sprite, x, topOffset, iw, mh);
                x += iw + gap;
                iter.remove();
            }

            ctx.drawString(t, x, topOffset);
            x += s.x + gap;

            iter = textSprites.iterator();
            while (iter.hasNext()) {
                final S sprite = iter.next();
                final float iw = sprite.sprite.getWidth() / sprite.sprite.getHeight() * mh;
                ctx.drawSprite(sprite.sprite, x, topOffset, iw, mh);
                x += iw + gap;
                iter.remove();
            }
        }
    }
}