package org.pillarone.riskanalytics.graph.core.layout

import org.pillarone.riskanalytics.core.user.Person
import org.pillarone.riskanalytics.graph.core.graph.persistence.GraphModel
import grails.util.Environment

class GraphLayout {

    Person person

    static belongsTo = [graphModel : GraphModel]

    static hasMany = [components: ComponentLayout, edges: EdgeLayout]

    static constraints = {
        if(Environment.current == Environment.TEST) {
            person(nullable: true)
        }
    }


}
