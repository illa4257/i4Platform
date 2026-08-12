package illa4257.i4Framework.base.points.layout;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.styling.PropIter;
import illa4257.i4Framework.base.styling.StyleProperty;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class StylePoint extends Point {
    private volatile Point v = null;
    public final Component component;
    public final List<String> propertyNames;

    public StylePoint(final Component component, final List<String> names) {
        this.component = component;
        this.propertyNames = names;
        onChange(component.getProperty(names));
    }

    public static StylePoint lambda(final Component component, final List<String> names,
                                    final Predicate<Object> filter, final Function<PropIter, Point> eval) {
        return new StylePoint(component, names) {
            @Override
            public Point eval(final StyleProperty property) {
                return eval.apply(component.getPI().select(property, filter));
            }
        };
    }

    public static <T extends Enum<T>> StylePoint lambda(final Component component, final List<String> names,
                                    final Class<T> enumClass, final Function<PropIter, Point> eval) {
        return new StylePoint(component, names) {
            @Override
            public Point eval(final StyleProperty property) {
                return eval.apply(component.getPI().select(property, enumClass));
            }
        };
    }

    public static StylePoint lambda(final Component component, final List<String> names,
                                    final Function<StyleProperty, Point> eval) {
        return new StylePoint(component, names) {
            @Override
            public Point eval(final StyleProperty property) {
                return eval.apply(property);
            }
        };
    }

    public abstract Point eval(final StyleProperty property);

    public void onChange(final StyleProperty property) {
        v = eval(property);
        reset();
    }

    @Override
    public float calcFloat() {
        final Point p = v;
        return p != null ? p.calcFloat() : 0;
    }

    @Override
    public int calcInt() {
        final Point p = v;
        return p != null ? p.calcInt() : 0;
    }

    @Override
    public void onConstruct() {
        component.subscribe(propertyNames, this::onChange);
        super.onConstruct();
    }

    @Override
    public void onDestruct() {
        component.unsubscribe(this::onChange);
        super.onDestruct();
    }
}