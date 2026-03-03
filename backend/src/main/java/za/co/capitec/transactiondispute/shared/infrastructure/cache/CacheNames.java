package za.co.capitec.transactiondispute.shared.infrastructure.cache;

/**
 * Stable identifiers for endpoint-level cache entries.
 *
 * These names are used for:
 * - TTL lookup (app.cache.ttl.*)
 * - key namespaces/versioning
 */
public final class CacheNames {

    private CacheNames() {}

    // transactions (GET)
    public static final String TX_BY_ID = "tx-by-id";
    public static final String TX_SEARCH_USER = "tx-search-user";
    public static final String TX_SEARCH_SUPPORT = "tx-search-support";

    // disputes (GET)
    public static final String DISPUTES_BY_USER = "disputes-by-user";
    public static final String DISPUTES_SUPPORT = "disputes-support";
    public static final String DISPUTE_VIEW_USER = "dispute-view-user";
    public static final String DISPUTE_VIEW_SUPPORT = "dispute-view-support";
    public static final String DISPUTE_HISTORY_USER = "dispute-history-user";
    public static final String DISPUTE_HISTORY_SUPPORT = "dispute-history-support";
}
