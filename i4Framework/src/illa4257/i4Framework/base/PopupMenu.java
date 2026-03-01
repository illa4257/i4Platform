package illa4257.i4Framework.base;

public interface PopupMenu {
    PopupMenu add(final String text, final Runnable action);
    PopupMenu show();
}