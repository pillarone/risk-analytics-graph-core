package org.pillarone.riskanalytics.graph.core.palette

class Palette {

    static hasMany = [entries:PaletteEntry]
    long userId
    String name
}
