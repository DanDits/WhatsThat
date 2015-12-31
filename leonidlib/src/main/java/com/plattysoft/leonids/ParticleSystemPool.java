package com.plattysoft.leonids;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 27.12.15.
 */
public class ParticleSystemPool implements ParticleSystem.OnAnimationEndedListener {
    // lightweight holding of particle systems: lazy creation of new systems up to max capacity;
    // not removing from list and only marking as locked if animation did not end yet
    private final List<ParticleSystem> mSystems;
    private final ParticleSystemMaker mMaker;
    private boolean[] mAvailable;
    private final int mCapacity;
    public interface ParticleSystemMaker {
        ParticleSystem makeParticleSystem();
    }

    /**
     * Creates a new pool with fixed capacity giving it a valid maker that produces valid new
     * particle systems.
     * @param maker Something that produces new ParticleSystems.
     * @param capacity The capacity of the pool, must be >0.
     */
    public ParticleSystemPool(ParticleSystemMaker maker, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Zero or negative capacity given: " + capacity);
        }
        mMaker = maker;
        mSystems = new ArrayList<>(capacity);
        mCapacity = capacity;
        mAvailable = new boolean[mCapacity];
    }

    /**
     * Obtains a new ParticleSystem if there is one available. The capacity limits how many
     * ParticleSystems can be available at total. When the particle emitting and animation ends,
     * the system is automatically freed for future use.
     * @return A particle system that can be used for starting emitting or null if none is
     * available due to capacity limitations.
     */
    public synchronized ParticleSystem obtain() {
        for (int i = 0; i < mAvailable.length; i++) {
            if (mAvailable[i]) {
                return lockSystem(i, mSystems.get(i));
            }
        }
        // no old system available: if capacity not yet reached create new one
        if (mSystems.size() < mCapacity) {
            ParticleSystem system = mMaker.makeParticleSystem();
            mSystems.add(system);
            return lockSystem(mSystems.size() - 1, system);
        }
        return null;
    }

    private ParticleSystem lockSystem(int index, ParticleSystem system) {
        system.setOnAnimationEndedListener(this);
        mAvailable[index] = false;
        return system;
    }

    @Override
    public synchronized void onParticleAnimationEnded(ParticleSystem system, boolean cancelled) {
        for (int i = 0; i < mSystems.size(); i++) {
            if (mSystems.get(i) == system) {
                system.setOnAnimationEndedListener(null);
                mAvailable[i] = true;
                break;
            }
        }
    }
}
