package org.pillarone.riskanalytics.graph.core.palette

class PaletteEntry {

    static belongsTo = Palette
    static hasMany = [palettes:Palette]
    Class type
}
