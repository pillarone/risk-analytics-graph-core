package org.pillarone.riskanalytics.graph.core.palette.service


class SingletonPaletteServiceAccessor implements IPaletteServiceAccessor {

    private static PaletteService service

    PaletteService obtainService() {
        synchronized (SingletonPaletteServiceAccessor.class) {
            if (service == null) {
                service = new PaletteService()
            }
        }
        return service
    }


}
