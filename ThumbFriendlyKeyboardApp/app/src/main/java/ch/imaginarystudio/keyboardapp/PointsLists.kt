package ch.imaginarystudio.keyboardapp

import kotlin.random.Random


var hexagonal = mutableListOf<Vec2>()
var hexagonalSupport = mutableListOf<Vec2>()
val spacing = 0.1
val width = 9
val height = 6

fun makeHexagonal() {
    for (i in 0..6) {
        var evenInt = i%2
        var end = 9 + evenInt
        for (j in 0 .. end) {
            var noise = Vec2((Random.nextDouble()-0.5f)/10000, (Random.nextDouble()-0.5f)/10000)
            hexagonalSupport.add(
                Vec2(
                    spacing * j + (spacing - spacing/2*evenInt),
                    spacing * i) + noise)
            if (( i != 0) and (i != 6) and (j!=0) and (j!=end)) {
                hexagonal.add(
                Vec2(
                    spacing * j + (spacing - spacing/2*evenInt),
                    spacing * i) + noise)
            }
        }
    }
}


