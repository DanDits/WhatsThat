package com.plattysoft.leonids;

import java.util.List;

/**
 * Created by daniel on 28.12.15.
 */
public interface ParticleFieldController {
    int getPositionInParentX();
    int getPositionInParentY();
    void prepareEmitting(List<Particle> particles);
    void onUpdate();
    void onCleanup(ParticleSystem toClean);
}
