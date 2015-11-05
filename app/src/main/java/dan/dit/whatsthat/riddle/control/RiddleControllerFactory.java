/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.riddle.control;

import dan.dit.whatsthat.riddle.Riddle;

/**
 * Created by daniel on 08.05.15.
 */
public class RiddleControllerFactory {
    public static final RiddleControllerFactory INSTANCE = new RiddleControllerFactory();

    private RiddleControllerFactory() {}

    RiddleController makeController(RiddleGame game, Riddle riddle) {
        return new RiddleController(game, riddle);
    }

    protected static class Silent extends RiddleControllerFactory {
        private Silent() {}

        protected RiddleController makeController(RiddleGame game, Riddle riddle) {
            return new SilentRiddleController(game, riddle);
        }
    }
}
