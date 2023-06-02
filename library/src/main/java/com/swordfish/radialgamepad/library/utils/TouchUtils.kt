/*
 * Created by Filippo Scognamiglio.
 * Copyright (c) 2020. This file is part of RadialGamePad.
 *
 * RadialGamePad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RadialGamePad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RadialGamePad.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.swordfish.radialgamepad.library.utils

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.util.DisplayMetrics
import android.view.MotionEvent
import androidx.annotation.RequiresApi


object TouchUtils {

    var pointerCount = 0
    var samsungMultitouchWorkaround = false

    data class FingerPosition(val pointerId: Int, val x: Float, val y: Float)

    @RequiresApi(29)
    fun extractRawFingersPositions(
        event: MotionEvent,
        offsetX: Int = 0,
        offsetY: Int = 0
    ): Sequence<FingerPosition> {
        return iteratePointerIndexes(event)
            .map { (id, index) ->
                FingerPosition(id, event.getRawX(index) - offsetX, event.getRawY(index) - offsetY)
            }
    }

    fun extractFingersPositions(event: MotionEvent): Sequence<FingerPosition> {
        return iteratePointerIndexes(event)
            .map { (id, index) -> FingerPosition(id, event.getX(index), event.getY(index)) }
    }

    fun extractSamsungFingersPositions(context: Context,pointerCount :Int,event: MotionEvent): Sequence<FingerPosition> {
        return iteratePointerIndexes(event)
            .map { (id, index) -> getSamsungFingerPosition(context,pointerCount,id, event.getX(index), event.getY(index)) }
    }

    private fun getSamsungFingerPosition(context: Context,pointerCount :Int,pointerId: Int,  x: Float,  y: Float):FingerPosition {
        var xV = x
        var yV = y
        // bug is hard to detect here, instead rely on the detection by the other buttons (see #2915)
        if (pointerCount > 1) {
            val scale: Double = getTouchScale(context)
            xV /= scale.toFloat()
            yV /= scale.toFloat()
        }
        return FingerPosition(pointerId, xV, yV)
    }

    private fun iteratePointerIndexes(event: MotionEvent): Sequence<Pair<Int, Int>> {
        return (0 until event.pointerCount)
            .asSequence()
            .map { event.getPointerId(it) }
            .map { id -> id to event.findPointerIndex(id) }
            .filter { (_, index) -> !isCancelEvent(event, index) }
    }

    fun computeRelativeFingerPosition(
        fingers: List<FingerPosition>,
        rect: RectF
    ): List<FingerPosition> {
        return fingers.map {
            FingerPosition(it.pointerId, (it.x - rect.left) / rect.width(), (it.y - rect.top) / rect.height())
        }
    }

    fun computeRelativePosition(x: Float, y: Float, rect: RectF): PointF {
        return PointF((x - rect.left) / rect.width(), (y - rect.top) / rect.height())
    }

    private fun isCancelEvent(event: MotionEvent, pointerIndex: Int): Boolean {
        val isUpAction = event.actionMasked in setOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_POINTER_1_UP, MotionEvent.ACTION_POINTER_2_UP,MotionEvent.ACTION_POINTER_3_UP)
        val isRelatedToCurrentIndex = event.actionIndex == pointerIndex
        return isUpAction && isRelatedToCurrentIndex
    }

    fun getTouchScale(context: Context): Double {
        // via https://github.com/F0RIS/SamsungMultitouchBugSample/blob/Fix1/app/src/main/java/com/jelly/blob/TouchView.java
        val displayMetrics = DisplayMetrics()
        val outSmallestSize = Point()
        val outLargestSize = Point()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        context.windowManager.defaultDisplay.getCurrentSizeRange(outSmallestSize, outLargestSize)
        return (Math.max(
            displayMetrics.widthPixels,
            displayMetrics.heightPixels
        ) / outLargestSize.x.toFloat()).toDouble()
    }
}