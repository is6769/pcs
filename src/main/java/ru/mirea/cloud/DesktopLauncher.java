package ru.mirea.cloud;

import ru.mirea.cloud.fx.CloudApplication;

/** Separate launcher permits JavaFX in the executable jar's classpath. */
public final class DesktopLauncher {
    private DesktopLauncher() { }
    public static void main(String[] args) { CloudApplication.main(args); }
}
