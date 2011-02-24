package org.pillarone.riskanalytics.graph.core.graph.persistence


class GraphPersistenceException extends RuntimeException {

    GraphPersistenceException() {
    }

    GraphPersistenceException(Throwable cause) {
        super(cause)
    }

    GraphPersistenceException(String message) {
        super(message)
    }

    GraphPersistenceException(String message, Throwable cause) {
        super(message, cause)
    }
}
