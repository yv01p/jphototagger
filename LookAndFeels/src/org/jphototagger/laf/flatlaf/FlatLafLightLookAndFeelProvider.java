package org.jphototagger.laf.flatlaf;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import org.jphototagger.api.windows.LookAndFeelProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 * FlatLaf Light look and feel provider.
 */
@ServiceProvider(service = LookAndFeelProvider.class)
public final class FlatLafLightLookAndFeelProvider implements LookAndFeelProvider {

    private static final Logger LOGGER = Logger.getLogger(FlatLafLightLookAndFeelProvider.class.getName());

    @Override
    public String getDisplayname() {
        return "FlatLaf Light";
    }

    @Override
    public String getDescription() {
        return "Modern flat light theme";
    }

    @Override
    public Component getPreferencesComponent() {
        return null;
    }

    @Override
    public String getPreferencesKey() {
        return "FlatLafLight";
    }

    @Override
    public boolean canInstall() {
        return true;
    }

    @Override
    public void setLookAndFeel() {
        LOGGER.info("Setting FlatLaf Light Look and Feel");
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
    }

    @Override
    public int getPosition() {
        return 100; // Position in the list of available LaFs
    }
}
