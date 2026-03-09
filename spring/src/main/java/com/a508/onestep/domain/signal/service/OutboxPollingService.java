package com.a508.onestep.domain.signal.service;

public interface OutboxPollingService {

    int publishPendingOutboxEvents();
}
