package com.example.pi_dev.Controllers.Events;

public class CatalogueRefreshManager {
    private static CatalogueRefreshManager instance;
    private boolean refreshRequested = false;

    private CatalogueRefreshManager() {}

    public static CatalogueRefreshManager getInstance() {
        if (instance == null) {
            instance = new CatalogueRefreshManager();
        }
        return instance;
    }

    public synchronized void requestRefresh() {
        refreshRequested = true;
    }

    public synchronized boolean isRefreshRequested() {
        return refreshRequested;
    }

    public synchronized void resetRefresh() {
        refreshRequested = false;
    }
}