package com.ivotasevski.idempotency.domain;

import com.ivotasevski.idempotency.action.Action;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "gtw_idemp")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class IdempotentRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "x_request_id", nullable = false)
    private String xRequestId;

    @Column(name = "trx_id", nullable = false)
    private String trxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private IdempotentRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "idempotent_action")
    private Action idempotentAction;

    @Version
    @Column(name = "version")
    private Long version;

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "lock_deadline")
    private Instant lockDeadline;

    @Column(name = "request_hash")
    private String requestHash;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_body")
    private byte[] responseBody;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_headers", columnDefinition = "jsonb")
    private Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
}
