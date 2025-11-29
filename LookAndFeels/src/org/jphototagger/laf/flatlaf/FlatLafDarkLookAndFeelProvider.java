package org.jphototagger.laf.flatlaf;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import org.jphototagger.api.windows.LookAndFeelProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 * FlatLaf Dark look and feel provider.
 */
@ServiceProvider(service = LookAndFeelProvider.class)
public final class FlatLafDarkLookAndFeelProvider implements LookAndFeelProvider {

    private static final Logger LOGGER = Logger.getLogger(FlatLafDarkLookAndFeelProvider.class.getName());

    @Override
    public String getDisplayname() {
        return "FlatLaf Dark";
    }

    @Override
    public String getDescription() {
        return "Modern flat dark theme";
    }

    @Override
    public Component getPreferencesComponent() {
        return null;
    }

    @Override
    public String getPreferencesKey() {
        return "FlatLafDark";
    }

    @Override
    public boolean canInstall() {
        return true;
    }

    @Override
    public void setLookAndFeel() {
        LOGGER.info("Setting FlatLaf Dark Look and Feel");
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
    }

    @Override
    public int getPosition() {
        return 101; // Position in the list of available LaFs
    }
}
