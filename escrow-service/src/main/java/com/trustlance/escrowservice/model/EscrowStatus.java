package com.trustlance.escrowservice.model;

public enum EscrowStatus {
    PENDING_FUNDING,   // Awaiting payment confirmation
    FUNDED,            // Funds are securely held
    RELEASE_PENDING,   // Release authorized, awaiting payout
    RELEASED,          // Paid to seller
    FROZEN,            // Held during a dispute
    REFUNDED           // Returned to buyer
}
