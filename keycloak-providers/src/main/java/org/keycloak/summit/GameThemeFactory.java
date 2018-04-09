package org.keycloak.summit;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.ClassLoaderTheme;
import org.keycloak.theme.JarThemeProvider;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeProvider;
import org.keycloak.theme.ThemeProviderFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameThemeFactory implements ThemeProviderFactory {

    protected Map<Theme.Type, Map<String, ClassLoaderTheme>> themes = new HashMap<>();

    @Override
    public ThemeProvider create(KeycloakSession session) {
        return new JarThemeProvider(themes);
    }

    @Override
    public void init(Config.Scope config) {
        try {
            themes = new HashMap<>();

            Map<String, ClassLoaderTheme> loginThemes = new HashMap<>();
            loginThemes.put("game", new ClassLoaderTheme("game", Theme.Type.LOGIN, GameThemeFactory.class.getClassLoader()));

            themes.put(Theme.Type.LOGIN, loginThemes);

            Map<String, ClassLoaderTheme> accountThemes = new HashMap<>();
            accountThemes.put("game", new ClassLoaderTheme("game", Theme.Type.ACCOUNT, GameThemeFactory.class.getClassLoader()));

            themes.put(Theme.Type.ACCOUNT, accountThemes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load themes", e);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "game-theme-provider";
    }

}
