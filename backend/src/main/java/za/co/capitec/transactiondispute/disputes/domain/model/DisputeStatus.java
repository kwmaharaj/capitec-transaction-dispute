package za.co.capitec.transactiondispute.disputes.domain.model;

public enum DisputeStatus {
    OPEN,        // step 1 customer created it. Can only transition to inprogress
    IN_PROGRESS, // step 2 support actively investigating. Can only transition to RESOLVED or REJECTED and OPEN if accidentally set to IN_PROGRESS.
    RESOLVED,    // step 3 resolved in customer's favour
    REJECTED     // step 3 alt rejected (not valid),not in customers favour but support completed it
}