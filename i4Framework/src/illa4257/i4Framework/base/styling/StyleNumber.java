package illa4257.i4Framework.base.styling;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.math.Unit;
import illa4257.i4Framework.base.points.ParentPoint;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.numbers.PointMultiplierN;
import illa4257.i4Framework.base.points.numbers.PointNumber;

import java.util.Arrays;
import java.util.List;

public class StyleNumber {
    public final float number;
    public final Unit unit;

    public StyleNumber(final float number, final Unit unit) {
        this.number = number;
        this.unit = unit;
    }

    public float calc(final Component component, final Orientation orientation) {
        if (unit == Unit.PERCENT)
            if (component != null) {
                final Container p = component.getParent();
                return p != null ? (orientation != Orientation.VERTICAL ? p.width : p.height).calcFloat() * number : 0;
            } else
                return 0;
        return number;
    }

    public Point getPoint(final Component component, final Orientation orientation) {
        if (unit == Unit.PERCENT)
            return new PointMultiplierN(new ParentPoint(component, orientation), number);
        return new PointNumber(number);
    }

    private static final List<Character>
            spaces = Arrays.asList(' ', '\t', '\r', '\n'),
            numbers = Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.');

    public static StyleNumber stringParser(final String s) {
        final int l = s.length();
        int i = 0;
        for (; i < l; i++)
            if (!spaces.contains(s.charAt(i)))
                break;
        final StringBuilder b = new StringBuilder();
        for (; i < l; i++) {
            if (!numbers.contains(s.charAt(i)))
                break;
            b.append(s.charAt(i));
        }
        if (b.length() == 0)
            return null;
        final float n = Float.parseFloat(b.toString());
        final char u = i < l ? s.charAt(i++) : ' ';
        if (u == '%')
            return new StyleNumber(n / 100, Unit.PERCENT);
        if ((u == 'p' && i < l && s.charAt(i) == 'x') || u == ' ')
            return new StyleNumber(n, Unit.PX);
        return null;
    }

    public static StyleNumber styleSettingParser(final StyleSetting setting) {
        final String s = setting.get(String.class);
        if (s != null)
            return stringParser(s);
        return null;
    }
}