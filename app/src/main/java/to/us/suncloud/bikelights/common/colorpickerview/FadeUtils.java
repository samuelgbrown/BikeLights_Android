/*
 * Copyright (C) 2017 skydoves
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package to.us.suncloud.bikelights.common.colorpickerview;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import to.us.suncloud.bikelights.R;

@SuppressWarnings("WeakerAccess")
public class FadeUtils {
    public static void fadeIn(View view) {
        Animation fadeIn = AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_in);
        fadeIn.setFillAfter(true);
        view.startAnimation(fadeIn);
    }

    public static void fadeOut(View view) {
        Animation fadeOut = AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_out);
        fadeOut.setFillAfter(true);
        view.startAnimation(fadeOut);
    }
}