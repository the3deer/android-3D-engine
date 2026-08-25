package org.the3deer.android.engine.text;

import org.the3deer.android.engine.model.Screen;

/**
 * Interface for font generation in the 3D Engine.
 */
public interface FontFactory {

    void setScreen(Screen screen);

    float getCharWidth();

    float getCharHeight();

    float[] getChar(char c);

    float[] getSymbol(String s);
}
