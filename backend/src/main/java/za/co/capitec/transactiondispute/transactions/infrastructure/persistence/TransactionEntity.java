package za.co.capitec.transactiondispute.transactions.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import za.co.capitec.transactiondispute.transactions.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table(schema = "transactions", name = "transactions")
public class TransactionEntity {

    @Id
    @Column("transaction_id")
    private UUID transactionId;

    @Column("user_id")
    private UUID userId;

    @Column("posted_at")
    private Instant postedAt;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("merchant")
    private String merchant;

    @Column("transaction_status")
    private TransactionStatus transactionStatus;

    @Column("bin")
    private String bin;

    @Column("last4digits")
    private String last4digits;

    @Column("pan_hash")
    private String panHash;

    @Column("rrn")
    private String rrn;

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getLast4digits() {
        return last4digits;
    }

    public void setLast4digits(String last4digits) {
        this.last4digits = last4digits;
    }

    public String getPanHash() {
        return panHash;
    }

    public void setPanHash(String panHash) {
        this.panHash = panHash;
    }

    public String getRrn() {
        return rrn;
    }

    public void setRrn(String rrn) {
        this.rrn = rrn;
    }
}
