package ru.mirea.cloud.model;

public enum RequestStatus {
    NEW, APPROVED, PROVISIONED, REJECTED, CANCELLED;

    public boolean canTransitionTo(RequestStatus next) {
        return switch (this) {
            case NEW -> next == APPROVED || next == REJECTED || next == CANCELLED;
            case APPROVED -> next == PROVISIONED || next == CANCELLED;
            case PROVISIONED, REJECTED, CANCELLED -> false;
        };
    }
}
