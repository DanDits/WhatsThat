package com.plattysoft.leonids;

import java.util.List;

/**
 * Created by daniel on 28.12.15.
 */
public interface ParticleField {
    ParticleFieldController getParticleController();
    void setParticles(List<Particle> particles);
}
