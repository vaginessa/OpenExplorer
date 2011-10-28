/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.brandroid.carousel;

import android.content.Context;
import android.util.AttributeSet;

import org.brandroid.carousel.CarouselController;
import org.brandroid.carousel.CarouselView;
import org.brandroid.openmanager.R;

public class MyCarouselView extends CarouselView {

    public MyCarouselView(Context context, CarouselController controller) {
        this(context, null, controller);
    }

    public MyCarouselView(Context context, AttributeSet attrs) {
        this(context, attrs, new CarouselController());
    }

    public MyCarouselView(Context context, AttributeSet attrs, CarouselController controller) {
        super(context, attrs, controller);
    }

    public Info getRenderScriptInfo() {
        return new Info(R.raw.carousel);
    }

    @Override
    public boolean interpretLongPressEvents() {
        return true;
    }

}
