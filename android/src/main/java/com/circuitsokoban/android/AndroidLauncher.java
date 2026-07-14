package com.circuitsokoban.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.circuitsokoban.CircuitSokobanGame;

/** Android entry point: hands the shared {@link CircuitSokobanGame} to the libGDX Android backend. */
public class AndroidLauncher extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true; // hide the system bars for a full-screen board
        initialize(new CircuitSokobanGame(), config);
    }
}
