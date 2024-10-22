package org.the3deer.android_3d_model_engine.animation;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Animation;
import org.the3deer.android_3d_model_engine.renderer.RenderListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnimationController implements RenderListener {

    // state
    private boolean enabled = true;

    // vars
    private final List<Animation<?>> animations = new ArrayList<>();
    private final List<Animation<?>> animations_new = new ArrayList<>();

    public void add(Animation<?> animation) {
        synchronized (animations_new) {
            Log.i("AnimationController", "New animation...." + animation);
            this.animations_new.add(animation);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onPrepareFrame() {
        animate();
    }

    private void animate() {

        // copy
        if (!animations_new.isEmpty()){
            animations.addAll(animations_new);
            synchronized (animations_new){
                animations_new.clear();
            }
        }

        // check
        if (animations.isEmpty()) return;

        // perform
        for (Iterator<Animation<?>> iter = animations.iterator(); iter.hasNext(); ) {
            Animation<?> a = iter.next();
            a.animate();
            if (a.isFinished())
                iter.remove();
        }
    }
}
