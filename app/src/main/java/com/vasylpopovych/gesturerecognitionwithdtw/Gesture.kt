package com.vasylpopovych.gesturerecognitionwithdtw

class Gesture(
    private var _name: String,
    x: List<Float>,
    y: List<Float>,
    z: List<Float>
) {
    private var _x = x.toMutableList()
    private var _y = y.toMutableList()
    private var _z = z.toMutableList()

    var name: String
        get() = _name
        set(value) {
            _name = value
        }

    var x: MutableList<Float>
        get() = _x
        set(value) {
            _x = value
        }

    var y: MutableList<Float>
        get() = _y
        set(value) {
            _y = value
        }

    var z: MutableList<Float>
        get() = _z
        set(value) {
            _z = value
        }

    fun size(): Int = _x.size

    fun getData(): Triple<List<Float>, List<Float>, List<Float>> {
        return Triple(_x, _y, _z)
    }
}