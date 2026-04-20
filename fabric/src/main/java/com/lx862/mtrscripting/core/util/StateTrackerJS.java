/**
 * MIT License
 *
 * Copyright (c) 2022-present Zbx1425
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.lx862.mtrscripting.core.util;

import java.util.Objects;

@SuppressWarnings("unused")
public class StateTrackerJS {
    private Object lastState;
    private Object currentState;
    private double currentStateTime;
    private boolean firstTimeCurrentState;

    public void setState(Object newValue) {
        if (!Objects.equals(newValue, currentState)) {
            lastState = currentState;
            currentState = newValue;
            currentStateTime = TimingJS.globalElapsed();
            firstTimeCurrentState = true;
        } else {
            firstTimeCurrentState = false;
        }
    }

    public Object stateNow() {
        return currentState;
    }

    public Object stateLast() {
        return lastState;
    }

    public double stateNowDuration() {
        return TimingJS.globalElapsed() - currentStateTime;
    }

    public boolean stateNowFirst() {
        return firstTimeCurrentState;
    }

    public boolean changedTo(Object state) {
        return stateNowFirst() && Objects.equals(currentState, state);
    }

    public boolean changedFromTo(Object oldState, Object curState) {
        return stateNowFirst() && Objects.equals(oldState, lastState) && Objects.equals(curState, currentState);
    }
}
