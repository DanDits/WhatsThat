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

package dan.dit.whatsthat.solution;

/**
 * Created by daniel on 13.04.15.
 */
public interface SolutionInputListener {

    /**
     * Notifies that the solution got completed.
     * @param userWord The word the user entered to complete the solution.
     * @return If the solution input is allowed to show that the solution is valid.
     */
    boolean onSolutionComplete(String userWord);

    /**
     * Indicates that the solution previously was complete and then got changed again.
     */
    void onSolutionIncomplete();
}
