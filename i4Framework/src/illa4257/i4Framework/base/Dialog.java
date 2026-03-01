package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.Component;

public interface Dialog {
    Dialog setTitle(final String title);
    Dialog setMessage(final String message);
    Dialog setContent(final Component component);
    Dialog setPositiveButton(final String name, final Runnable action);
    Dialog setNegativeButton(final String name, final Runnable action);
    Dialog setOnCancelListener(final Runnable action);
    Dialog show();
}